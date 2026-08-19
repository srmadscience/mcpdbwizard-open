package com.mcpdbwizard.pub;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Writes every audit record to disk first, then delivers it to a delegate sink and deletes it only
 * once the delegate confirms.
 *
 * <p>This is what makes the trail survive an outage. A plain {@link KafkaAuditSink} loses records
 * once the broker has been unreachable longer than its in-memory buffer absorbs; with a spool in
 * front, those records are on disk and are replayed — including across a restart, because the spool
 * is read back when the server starts.
 *
 * <h2>Write-ahead, not fallback</h2>
 *
 * <p>Records are spooled <em>before</em> any delivery attempt, not after one fails. A fallback design
 * cannot work here: {@link McpAuditSink#record} returns {@code void} and an asynchronous sink has not
 * even attempted delivery by the time it returns, so there is nothing to fall back from. Writing
 * first also means a process killed mid-call still has the record.
 *
 * <h2>The guarantee, stated precisely</h2>
 *
 * <p><b>At-least-once, and duplicates are possible.</b> A segment is deleted only after the delegate
 * confirms via {@link McpAuditSink#flush()}, so a crash between delivering and deleting replays that
 * segment. Every event carries {@link McpAuditEvent#getId()} so a consumer can collapse the repeat.
 *
 * <p><b>What survives what:</b> with {@code MCP_AUDIT_SPOOL_FSYNC=never} (the default) records
 * survive the process dying — a crash, a container restart, an OOM kill — because the bytes are in
 * the operating system's cache. They do <b>not</b> survive the machine losing power. Setting
 * {@code always} calls {@code fsync} per record, which survives that too and costs a disk round trip
 * on every tool call.
 *
 * <p><b>Disk is not infinite.</b> Past {@code MCP_AUDIT_SPOOL_MAX_BYTES} the policy decides:
 * {@code drop} refuses new records and counts them, {@code block} makes the tool call wait for the
 * drainer to catch up. Dropping the <em>oldest</em> is deliberately not offered — silently discarding
 * the records already accepted for audit is the worst of the three.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class SpoolingAuditSink implements McpAuditSink {

    public static final String DIRECTORY_VARIABLE = "MCP_AUDIT_SPOOL_DIR";
    public static final String MAX_BYTES_VARIABLE = "MCP_AUDIT_SPOOL_MAX_BYTES";
    public static final String ON_FULL_VARIABLE = "MCP_AUDIT_SPOOL_ON_FULL";
    public static final String FSYNC_VARIABLE = "MCP_AUDIT_SPOOL_FSYNC";
    public static final String SEGMENT_BYTES_VARIABLE = "MCP_AUDIT_SPOOL_SEGMENT_BYTES";

    public static final long DEFAULT_MAX_BYTES = 100L * 1024L * 1024L;
    public static final long DEFAULT_SEGMENT_BYTES = 1024L * 1024L;

    private static final String ACTIVE_NAME = "active.jsonl";
    private static final String SEGMENT_PREFIX = "segment-";
    private static final String SEGMENT_SUFFIX = ".jsonl";

    private final McpAuditSink theDelegate;
    private final Path theDirectory;
    private final long theMaxBytes;
    private final long theSegmentBytes;
    private final boolean theBlockFlag;
    private final boolean theFsyncFlag;
    private final LogInterface theLog = new JulLog("SpoolingAuditSink");

    /**
     * Encrypts each spooled line, or null to write plaintext. See {@link SpoolCipher} for what that
     * protects and why encrypting the volume is usually the better answer.
     */
    private final SpoolCipher theCipher;

    private final AtomicLong theDropped = new AtomicLong();
    private final AtomicLong theDelivered = new AtomicLong();
    private final Object theWriteLock = new Object();

    private Thread theDrainer;
    private volatile boolean theRunningFlag = true;

    public SpoolingAuditSink(McpAuditSink theDelegateValue, Path theDirectoryValue, long theMaxBytesValue,
                             long theSegmentBytesValue, boolean theBlockFlagValue, boolean theFsyncFlagValue) {
        this(theDelegateValue, theDirectoryValue, theMaxBytesValue, theSegmentBytesValue,
                theBlockFlagValue, theFsyncFlagValue, SpoolCipher.fromEnvironment());
    }

    /**
     * As above, with the cipher supplied rather than read from the environment.
     *
     * @param theCipherValue encrypts each spooled line, or null to write plaintext
     */
    public SpoolingAuditSink(McpAuditSink theDelegateValue, Path theDirectoryValue, long theMaxBytesValue,
                             long theSegmentBytesValue, boolean theBlockFlagValue,
                             boolean theFsyncFlagValue, SpoolCipher theCipherValue) {
        this.theCipher = theCipherValue;
        this.theDelegate = theDelegateValue;
        this.theDirectory = theDirectoryValue;
        this.theMaxBytes = theMaxBytesValue;
        this.theSegmentBytes = theSegmentBytesValue;
        this.theBlockFlag = theBlockFlagValue;
        this.theFsyncFlag = theFsyncFlagValue;

        try {
            Files.createDirectories(theDirectory);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot use " + theDirectory
                    + " as an audit spool directory: " + e.getMessage());
        }

        // Anything left from a previous run is picked up here. This is the whole point of spooling:
        // a server that died with undelivered records delivers them when it comes back.
        rollActive();
    }

    /** Build from the environment, wrapping the given delegate. Returns the delegate unchanged when unset. */
    public static McpAuditSink wrap(McpAuditSink theDelegateValue) {
        return wrap(theDelegateValue, null);
    }

    /**
     * As {@link #wrap(McpAuditSink)}, but placing the spool in a named subdirectory.
     *
     * <p>A spool has exactly one writer by construction: one active file, and a drainer that takes
     * every closed segment it finds. Two processes sharing a directory would therefore interleave
     * writes into one file and each deliver the other's segments. A generated server is the only
     * process in its JVM and needs no subdirectory; the web application, which records proxied
     * requests alongside however many generated servers it has launched, passes one so that each
     * spool has a single owner.
     *
     * @param theSubdirectoryValue a directory name to append, or null to use the configured
     *                             directory as it stands
     */
    public static McpAuditSink wrap(McpAuditSink theDelegateValue, String theSubdirectoryValue) {
        String theDirectorySetting = McpAuditSinks.setting(DIRECTORY_VARIABLE);
        if (theDirectorySetting == null || theDirectorySetting.trim().length() == 0) {
            return theDelegateValue;
        }
        Path theDirectory = Paths.get(theDirectorySetting.trim());
        if (theSubdirectoryValue != null && theSubdirectoryValue.trim().length() > 0) {
            theDirectory = theDirectory.resolve(theSubdirectoryValue.trim());
        }
        SpoolingAuditSink theSink = new SpoolingAuditSink(theDelegateValue,
                theDirectory,
                positive(MAX_BYTES_VARIABLE, McpAuditSinks.setting(MAX_BYTES_VARIABLE), DEFAULT_MAX_BYTES),
                positive(SEGMENT_BYTES_VARIABLE, McpAuditSinks.setting(SEGMENT_BYTES_VARIABLE), DEFAULT_SEGMENT_BYTES),
                "block".equalsIgnoreCase(trim(McpAuditSinks.setting(ON_FULL_VARIABLE))),
                "always".equalsIgnoreCase(trim(McpAuditSinks.setting(FSYNC_VARIABLE))));
        theSink.startDraining();
        return theSink;
    }

    /** Start the background drainer. Separate from the constructor so tests can drain deterministically. */
    public void startDraining() {
        theDrainer = new Thread(() -> {
            while (theRunningFlag) {
                try {
                    Thread.sleep(1000L);
                    drainOnce();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception e) {
                    theLog.warning("Audit spool drain failed, will retry: " + e);
                }
            }
        }, "mcp-audit-spool");
        theDrainer.setDaemon(true);
        theDrainer.start();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Appends to the spool. Never throws — a failure to audit must not become the caller's failure.
     */
    public void record(McpAuditEvent theEvent) {
        if (theEvent == null) {
            return;
        }
        try {
            synchronized (theWriteLock) {
                if (spooledBytes() >= theMaxBytes) {
                    if (!theBlockFlag) {
                        countDrop("spool is full (" + theMaxBytes + " bytes)");
                        return;
                    }
                    // Block: let the drainer catch up. Bounded so a permanently dead delegate cannot
                    // wedge the server forever - at which point dropping is the only option left.
                    for (int i = 0; i < 50 && spooledBytes() >= theMaxBytes; i++) {
                        theWriteLock.wait(100L);
                    }
                    if (spooledBytes() >= theMaxBytes) {
                        countDrop("spool still full after waiting");
                        return;
                    }
                }
                // Encrypted here rather than at the caller, so exactly one path writes to the
                // spool and there is no route that bypasses it.
                append(theCipher == null ? theEvent.toJson() : theCipher.encrypt(theEvent.toJson()));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            countDrop("interrupted");
        } catch (Exception e) {
            countDrop(e.toString());
        }
    }

    private void append(String theLine) throws IOException {
        Path theActive = theDirectory.resolve(ACTIVE_NAME);
        try (BufferedWriter theWriter = Files.newBufferedWriter(theActive, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            theWriter.write(theLine);
            theWriter.newLine();
        }
        if (theFsyncFlag) {
            // Survives the machine losing power, not just the process dying. One disk round trip per
            // tool call, which is why it is not the default.
            try (RandomAccessFile theFile = new RandomAccessFile(theActive.toFile(), "rws")) {
                theFile.getFD().sync();
            }
        }
        if (Files.size(theActive) >= theSegmentBytes) {
            rollActive();
        }
    }

    /** Close the active file into a numbered segment so the drainer can take it whole. */
    private void rollActive() {
        Path theActive = theDirectory.resolve(ACTIVE_NAME);
        try {
            if (Files.exists(theActive) && Files.size(theActive) > 0L) {
                Path theSegment = theDirectory.resolve(SEGMENT_PREFIX
                        + System.currentTimeMillis() + "-" + System.nanoTime() + SEGMENT_SUFFIX);
                Files.move(theActive, theSegment);
            }
        } catch (IOException e) {
            theLog.warning("Could not roll the audit spool: " + e);
        }
    }

    /**
     * The record inside one spooled line.
     *
     * <p>A plaintext line passes straight through, which is what lets encryption be switched ON over
     * a spool that already holds records.
     *
     * <p><b>The reverse needs guarding, and did not get it at first.</b> Switching encryption OFF —
     * or simply losing the key variable — leaves encrypted lines behind with no cipher to read them.
     * Passing those through would deliver the base64 ciphertext to the sink AS IF IT WERE A RECORD:
     * no error anywhere, a segment deleted as successfully delivered, and an audit trail containing
     * unreadable strings where the records used to be. Refused instead, which routes the segment to
     * quarantine.
     */
    private String plainTextOf(String theLine) {
        if (theCipher != null) {
            return theCipher.decrypt(theLine);
        }
        if (SpoolCipher.isEncrypted(theLine)) {
            throw new IllegalStateException("This segment is encrypted but no "
                    + SpoolCipher.KEY_VARIABLE + " is set, so it cannot be read.");
        }
        return theLine;
    }

    /** Suffix for a segment that cannot be read, so a later drain does not pick it up again. */
    static final String QUARANTINE_SUFFIX = ".unreadable";

    /**
     * Move a segment aside that cannot be delivered because it cannot be read.
     *
     * <p>The usual cause is {@link SpoolCipher#KEY_VARIABLE} having changed, which strands every
     * record written under the old key. There is no recovery inside this process — the records are
     * kept, renamed, and reported, and someone with the old key can decrypt the file by hand.
     *
     * <p><b>A quarantined file still counts towards {@code MCP_AUDIT_SPOOL_MAX_BYTES}</b>, because it
     * is still occupying disk. It will never drain, so it erodes the budget permanently and an
     * operator has to move or delete it. Excluding it from the accounting was rejected: the cap
     * exists to bound what this directory costs, and pretending a file is not there would let the
     * spool overrun the limit it was given.
     */
    private void quarantine(File theSegment, RuntimeException theCause) {
        File theTarget = new File(theSegment.getParentFile(),
                theSegment.getName() + QUARANTINE_SUFFIX);
        boolean theMovedFlag = theSegment.renameTo(theTarget);
        theLog.error("Audit segment " + theSegment.getName() + " cannot be read and will never be"
                + " delivered: " + theCause.getMessage());
        theLog.error(theMovedFlag
                ? "Moved it to " + theTarget.getName() + ". Its records are NOT in the audit trail."
                : "Could not move it aside, so it will be retried and will keep failing.");
    }

    /**
     * Deliver every closed segment, oldest first, deleting each only once the delegate confirms.
     *
     * @return how many records were delivered
     */
    public long drainOnce() {
        rollActive();
        long theCount = 0L;
        for (File theSegment : closedSegments()) {
            List<String> theLines;
            try {
                theLines = Files.readAllLines(theSegment.toPath(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                theLog.warning("Cannot read audit segment " + theSegment + ": " + e);
                continue;
            }

            try {
                for (String theLine : theLines) {
                    if (theLine.trim().length() > 0) {
                        theDelegate.record(McpAuditEvent.ofRawJson(plainTextOf(theLine)));
                    }
                }
            } catch (RuntimeException e) {
                // An undecryptable segment can NEVER be delivered, so leaving it in place would
                // retry it for ever and block every segment behind it. Deleting it would destroy
                // audit records. Quarantine is the only honest third option: out of the way, still
                // on disk, and loud about it.
                quarantine(theSegment, e);
                continue;
            }

            // Only now, and only if the delegate says everything arrived. Deleting on a false
            // confirmation is the one way a spool is worse than no spool.
            if (!theDelegate.flush()) {
                theLog.warning("Audit delegate did not confirm delivery; keeping " + theSegment.getName()
                        + " to retry. Records in it may be delivered more than once.");
                return theCount;
            }
            theCount += theLines.size();
            theDelivered.addAndGet(theLines.size());
            if (!theSegment.delete()) {
                theLog.warning("Delivered but could not delete " + theSegment
                        + "; it will be replayed, producing duplicates.");
            }
            synchronized (theWriteLock) {
                theWriteLock.notifyAll();
            }
        }
        return theCount;
    }

    private File[] closedSegments() {
        File[] theFiles = theDirectory.toFile().listFiles((theDir, theName) ->
                theName.startsWith(SEGMENT_PREFIX) && theName.endsWith(SEGMENT_SUFFIX));
        if (theFiles == null) {
            return new File[0];
        }
        Arrays.sort(theFiles);
        return theFiles;
    }

    private long spooledBytes() {
        long theTotal = 0L;
        File[] theFiles = theDirectory.toFile().listFiles();
        if (theFiles != null) {
            for (File theFile : theFiles) {
                theTotal += theFile.length();
            }
        }
        return theTotal;
    }

    /** Records refused because the spool was full. Non-zero means the trail has holes. */
    public long getDroppedCount() {
        return theDropped.get();
    }

    /** Records handed to the delegate and confirmed. */
    public long getDeliveredCount() {
        return theDelivered.get();
    }

    /** How many are still on disk awaiting delivery. */
    public long getPendingCount() {
        long thePending = 0L;
        List<File> theFiles = new ArrayList<File>(Arrays.asList(closedSegments()));
        File theActive = theDirectory.resolve(ACTIVE_NAME).toFile();
        if (theActive.exists()) {
            theFiles.add(theActive);
        }
        for (File theFile : theFiles) {
            try {
                thePending += Files.readAllLines(theFile.toPath(), StandardCharsets.UTF_8).size();
            } catch (IOException e) {
                // Counted as zero rather than failing a status call.
            }
        }
        return thePending;
    }

    private void countDrop(String theReason) {
        long theTotal = theDropped.incrementAndGet();
        if (theTotal == 1L || theTotal % 1000L == 0L) {
            theLog.error("Audit record not spooled (" + theTotal + " so far): " + theReason);
        }
    }

    /** Drain what is left, then close the delegate. */
    /**
     * Names what the records finally go to, not just this wrapper.
     *
     * <p>"SpoolingAuditSink" on its own answers the wrong question: the spool is how delivery
     * survives an outage, while the destination is what an operator is checking.
     */
    @Override
    public String describe() {
        return "spooled -> " + theDelegate.describe();
    }

    public void close() {
        theRunningFlag = false;
        if (theDrainer != null) {
            theDrainer.interrupt();
        }
        try {
            drainOnce();
        } catch (Exception e) {
            theLog.warning("Final audit drain failed: " + e);
        }
        long thePending = getPendingCount();
        if (thePending > 0L) {
            theLog.error("Shutting down with " + thePending + " audit record(s) still spooled in "
                    + theDirectory + ". They will be delivered when this server restarts.");
        }
        if (theDropped.get() > 0L) {
            theLog.error("Audit trail is incomplete: " + theDropped.get() + " record(s) were refused.");
        }
        theDelegate.close();
    }

    private static String trim(String theValue) {
        return theValue == null ? "" : theValue.trim();
    }

    private static long positive(String theName, String theSetting, long theDefault) {
        if (theSetting == null || theSetting.trim().length() == 0) {
            return theDefault;
        }
        try {
            long theValue = Long.parseLong(theSetting.trim());
            if (theValue <= 0L) {
                throw new IllegalArgumentException(theName + " must be greater than zero, not '"
                        + theSetting.trim() + "'");
            }
            return theValue;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(theName + " must be a whole number, not '"
                    + theSetting.trim() + "'");
        }
    }

}
