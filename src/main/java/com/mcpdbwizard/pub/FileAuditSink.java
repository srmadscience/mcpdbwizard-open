package com.mcpdbwizard.pub;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Keeps the audit trail on this machine for a bounded window, and deletes it when the window passes.
 *
 * <p>This is the record every tier gets. A licence buys a longer window and the right to send the
 * records somewhere else; it does not buy the existence of a record, because a product sold on
 * knowing what an agent did cannot have a free tier that knows nothing.
 *
 * <h2>The same file mechanics as the spool, the opposite lifecycle</h2>
 *
 * <p>{@link SpoolingAuditSink} also writes rolling JSONL segments, and the resemblance is deliberate.
 * But a spool is a <b>delivery queue</b>: a segment exists until a delegate confirms it, and is then
 * deleted as soon as possible. This is a <b>retention store</b>: a segment exists until it is old
 * enough to delete, and there is no delegate to confirm anything. They disagree about the one thing
 * that decides when a file may be removed, which is why this is a sibling rather than a subclass.
 *
 * <p><b>Do not wrap this in a spool.</b> Spooling a sink whose delivery mechanism is "write it to
 * disk" produces two copies of the same bytes, one of which is deleted the moment the other is
 * written. {@link McpAuditSinks} composes it directly for that reason.
 *
 * <h2>What "retention" costs, and what it does not promise</h2>
 *
 * <p><b>Records that age out are gone.</b> That is the feature, not a limitation: a store that
 * quietly kept everything would be a worse position to be in than one that deletes on a schedule,
 * because the retention window is what makes it defensible under a storage-limitation rule.
 *
 * <p><b>The window is enforced at segment granularity</b>, so a record may outlive it by up to one
 * segment. Segments roll on size, so a quiet server holds its oldest records longer than a busy one.
 * Nothing here should be relied on to delete a specific record at a specific minute.
 *
 * <p><b>{@code MCP_AUDIT_RETENTION_DAYS=0} means no trail at all</b> — this sink is not built.
 * See {@link #fromEnvironment(String)}.
 *
 * <h2>Two ways records are lost, and only one of them counts</h2>
 *
 * <p>Ageing out is the policy working and is <b>not</b> counted as a drop. Being evicted early
 * because {@code MCP_AUDIT_FILE_MAX_BYTES} was reached <b>is</b> a drop: those records were inside
 * the window an operator was promised and are gone anyway, which is exactly what
 * {@link #getDroppedCount()} exists to say.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class FileAuditSink implements McpAuditSink {

    public static final String DIRECTORY_VARIABLE = "MCP_AUDIT_FILE_DIR";
    public static final String RETENTION_DAYS_VARIABLE = "MCP_AUDIT_RETENTION_DAYS";
    public static final String MAX_BYTES_VARIABLE = "MCP_AUDIT_FILE_MAX_BYTES";
    public static final String SEGMENT_BYTES_VARIABLE = "MCP_AUDIT_FILE_SEGMENT_BYTES";

    /** Twenty-four hours: the free tier's window, and the safe default for everyone else. */
    public static final int DEFAULT_RETENTION_DAYS = 1;

    /** Generous enough for a real trail, small enough that it cannot quietly fill a volume. */
    public static final long DEFAULT_MAX_BYTES = 512L * 1024L * 1024L;

    /** Matches the spool, so the two directories behave alike to anyone looking at them. */
    public static final long DEFAULT_SEGMENT_BYTES = 1024L * 1024L;

    // Public because the trail is READ by another module. The console concatenates several
    // writers' directories into one download, so the file naming and the rule for reading a
    // segment's age out of its name are a contract between writer and reader, not internals.
    public static final String ACTIVE_NAME = "active.jsonl";
    public static final String SEGMENT_PREFIX = "trail-";
    public static final String SEGMENT_SUFFIX = ".jsonl";

    private static final long MILLIS_PER_DAY = 24L * 60L * 60L * 1000L;

    /**
     * UTC, compact, and lexicographically sortable — so {@code ls} shows the window in date order
     * and a human can see at a glance how far back the trail goes. That readability is the reason
     * the open time is in the NAME rather than left to the file's modification time, which any
     * backup or copy would rewrite.
     */
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    private final Path theDirectory;
    private final int theRetentionDays;
    private final long theMaxBytes;
    private final long theSegmentBytes;
    private final LogInterface theLog = new JulLog("FileAuditSink");

    private final AtomicLong theWritten = new AtomicLong();
    private final AtomicLong theDropped = new AtomicLong();
    private final Object theWriteLock = new Object();

    public FileAuditSink(Path theDirectoryValue, int theRetentionDaysValue, long theMaxBytesValue,
                         long theSegmentBytesValue) {
        this.theDirectory = theDirectoryValue;
        this.theRetentionDays = theRetentionDaysValue;
        this.theMaxBytes = theMaxBytesValue;
        this.theSegmentBytes = theSegmentBytesValue;

        try {
            Files.createDirectories(theDirectory);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot use " + theDirectory
                    + " as an audit trail directory: " + e.getMessage());
        }

        // Close whatever the last run left open, so its records age from when they were written
        // rather than from whenever this process happens to fill a segment.
        synchronized (theWriteLock) {
            rollActive();
            prune();
        }
    }

    /**
     * Build from the environment, or null when no local trail is wanted.
     *
     * <p>Returns null in two cases, and the difference matters to nobody but is worth stating: no
     * {@link #DIRECTORY_VARIABLE} means the deployment never asked for a trail, and
     * {@code MCP_AUDIT_RETENTION_DAYS=0} means it asked for one and then asked to keep nothing.
     * Zero is a supported, deliberate setting — the deployment that streams every record to a SIEM
     * and wants no production data resting on this box.
     *
     * <p><b>Zero is never inferred.</b> An unset window is one day, and a window that cannot be
     * parsed stops start-up. Nothing here silently decides to keep no records.
     *
     * @param theSubdirectoryValue a per-process directory name, or null; a trail directory tolerates
     *                             exactly one writer, for the same reason a spool does
     */
    public static FileAuditSink fromEnvironment(String theSubdirectoryValue) {
        return fromEnvironment(theSubdirectoryValue, Integer.MAX_VALUE);
    }

    /**
     * As {@link #fromEnvironment(String)}, with a hard ceiling on the window.
     *
     * <p>The ceiling exists because a process cannot always be trusted with its own environment. A
     * generated server is launched by something that sets its variables deliberately; the web
     * application reads the variables an <em>operator</em> gave it, so a window it is not entitled
     * to has to be cut down here rather than believed.
     *
     * <p><b>Deliberately an int, not a licence.</b> This module is the Apache-2.0 half and must
     * carry no licensing logic; the caller works out what the ceiling is and passes a number.
     *
     * @param theCeilingDays the largest window to honour, whatever the environment asks for
     */
    public static FileAuditSink fromEnvironment(String theSubdirectoryValue, int theCeilingDays) {
        String theDirectorySetting = McpAuditSinks.setting(DIRECTORY_VARIABLE);
        if (theDirectorySetting == null || theDirectorySetting.trim().length() == 0) {
            return null;
        }
        int theRetentionDays = Math.min(
                retentionDays(McpAuditSinks.setting(RETENTION_DAYS_VARIABLE)), theCeilingDays);
        if (theRetentionDays <= 0) {
            return null;
        }
        Path theDirectory = Paths.get(theDirectorySetting.trim());
        if (theSubdirectoryValue != null && theSubdirectoryValue.trim().length() > 0) {
            theDirectory = theDirectory.resolve(theSubdirectoryValue.trim());
        }
        return new FileAuditSink(theDirectory, theRetentionDays,
                positive(MAX_BYTES_VARIABLE, McpAuditSinks.setting(MAX_BYTES_VARIABLE), DEFAULT_MAX_BYTES),
                positive(SEGMENT_BYTES_VARIABLE, McpAuditSinks.setting(SEGMENT_BYTES_VARIABLE),
                        DEFAULT_SEGMENT_BYTES));
    }

    /**
     * The configured window in days, defaulting to one.
     *
     * <p>A value that is not a whole number, or is negative, <b>throws</b> rather than falling back.
     * That matches {@link McpAuditSinks#level(String)} and the rest of the audit configuration: a
     * mistyped setting stops start-up instead of leaving an operator with a window they did not
     * choose. Defaulting a typo to one day would be the quiet version of the same bug this whole
     * feature exists to avoid.
     */
    static int retentionDays(String theSetting) {
        if (theSetting == null || theSetting.trim().length() == 0) {
            return DEFAULT_RETENTION_DAYS;
        }
        try {
            int theValue = Integer.parseInt(theSetting.trim());
            if (theValue < 0) {
                throw new IllegalArgumentException(RETENTION_DAYS_VARIABLE
                        + " cannot be negative, got '" + theSetting.trim() + "'."
                        + " Use 0 to keep no local trail at all.");
            }
            return theValue;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(RETENTION_DAYS_VARIABLE
                    + " must be a whole number of days, not '" + theSetting.trim() + "'."
                    + " Use 0 to keep no local trail at all.");
        }
    }

    /** {@inheritDoc} <p>Appends to the trail. Never throws. */
    public void record(McpAuditEvent theEvent) {
        if (theEvent == null) {
            return;
        }
        try {
            synchronized (theWriteLock) {
                append(theEvent.toJson());
                theWritten.incrementAndGet();
            }
        } catch (Exception e) {
            // A record that could not be written is genuinely lost -- there is no queue behind this
            // to retry from -- so it counts.
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
        if (Files.size(theActive) >= theSegmentBytes) {
            rollActive();
            // Pruning on roll rather than on a timer: no background thread to own, and the window
            // is then visible in a directory listing rather than being a promise made by a daemon.
            prune();
        }
    }

    /** Close the active file into a segment named for the moment it is closed. */
    private void rollActive() {
        Path theActive = theDirectory.resolve(ACTIVE_NAME);
        try {
            if (!Files.exists(theActive) || Files.size(theActive) == 0L) {
                return;
            }
            long theMillis = System.currentTimeMillis();
            Path theSegment = segmentPath(theMillis);
            // Two rolls inside one millisecond would collide. Stepping the stamp forward is
            // harmless -- it can only make a record look marginally younger than it is, and the
            // window is approximate at segment granularity anyway.
            while (Files.exists(theSegment)) {
                theMillis++;
                theSegment = segmentPath(theMillis);
            }
            Files.move(theActive, theSegment);
        } catch (IOException e) {
            theLog.warning("Could not roll the audit trail: " + e);
        }
    }

    private Path segmentPath(long theMillis) {
        return theDirectory.resolve(SEGMENT_PREFIX + STAMP.format(Instant.ofEpochMilli(theMillis))
                + "-" + theMillis + SEGMENT_SUFFIX);
    }

    /**
     * Delete what is past the window, then what does not fit.
     *
     * <p><b>The byte cap wins.</b> If both limits would be breached the oldest goes first regardless
     * of age, because the alternative is filling the volume the database's own logs are on. Records
     * lost that way are counted; records lost to age are not.
     */
    void prune() {
        File[] theSegments = segments();
        long theCutoff = System.currentTimeMillis() - (theRetentionDays * MILLIS_PER_DAY);

        for (File theSegment : theSegments) {
            if (openedAt(theSegment) < theCutoff) {
                delete(theSegment, false);
            }
        }

        long theTotal = totalBytes();
        if (theTotal <= theMaxBytes) {
            return;
        }
        for (File theSegment : segments()) {
            if (theTotal <= theMaxBytes) {
                return;
            }
            long theSize = theSegment.length();
            long theLines = countLines(theSegment);
            if (delete(theSegment, true)) {
                theTotal -= theSize;
                countDrops(theLines, "the trail reached " + MAX_BYTES_VARIABLE + " (" + theMaxBytes
                        + " bytes) and the oldest segment was evicted before its window expired");
            }
        }
    }

    private boolean delete(File theSegment, boolean theEarlyFlag) {
        if (theSegment.delete()) {
            return true;
        }
        theLog.warning("Could not delete audit trail segment " + theSegment.getName()
                + (theEarlyFlag ? "; the trail is over its byte cap." : "; it is past its window."));
        return false;
    }

    /**
     * When this segment was closed, from its name, falling back to the file's own timestamp.
     *
     * <p>The fallback matters: a file someone copied, renamed or restored from a backup would
     * otherwise be treated as having been opened at the epoch and deleted on the next prune.
     */
    public static long openedAt(File theSegment) {
        String theName = theSegment.getName();
        int theDash = theName.lastIndexOf('-');
        int theDot = theName.lastIndexOf(SEGMENT_SUFFIX);
        if (theDash > 0 && theDot > theDash) {
            try {
                return Long.parseLong(theName.substring(theDash + 1, theDot));
            } catch (NumberFormatException ignored) {
                // Falls through to the file's own timestamp.
            }
        }
        return theSegment.lastModified();
    }

    /** Closed segments, oldest first. The active file is never pruned; it is still being written. */
    private File[] segments() {
        File[] theFiles = theDirectory.toFile().listFiles((theDir, theName) ->
                theName.startsWith(SEGMENT_PREFIX) && theName.endsWith(SEGMENT_SUFFIX));
        if (theFiles == null) {
            return new File[0];
        }
        Arrays.sort(theFiles, (a, b) -> Long.compare(openedAt(a), openedAt(b)));
        return theFiles;
    }

    private long totalBytes() {
        long theTotal = 0L;
        File[] theFiles = theDirectory.toFile().listFiles();
        if (theFiles != null) {
            for (File theFile : theFiles) {
                theTotal += theFile.length();
            }
        }
        return theTotal;
    }

    private static long countLines(File theFile) {
        try {
            return Files.readAllLines(theFile.toPath(), StandardCharsets.UTF_8).size();
        } catch (IOException e) {
            return 0L;
        }
    }

    /** How far back the trail actually goes, in millis since the epoch, or -1 when it is empty. */
    public long oldestRecordMillis() {
        File[] theSegments = segments();
        if (theSegments.length > 0) {
            return openedAt(theSegments[0]);
        }
        File theActive = theDirectory.resolve(ACTIVE_NAME).toFile();
        return theActive.exists() ? theActive.lastModified() : -1L;
    }

    /** The configured window, in days. */
    public int getRetentionDays() {
        return theRetentionDays;
    }

    /** Where the trail is, so a status page can name it. */
    public Path getDirectory() {
        return theDirectory;
    }

    /**
     * Records evicted before their window expired, or lost to a write failure.
     *
     * <p>Deliberately NOT counting records that simply aged out. Those left on schedule, and
     * reporting them as losses would make a healthy trail permanently look broken.
     */
    public long getDroppedCount() {
        return theDropped.get();
    }

    /** Records written. For this sink, written is delivered. */
    public long getDeliveredCount() {
        return theWritten.get();
    }

    /** Always zero: there is no delivery step to be behind on. */
    public long getPendingCount() {
        return 0L;
    }

    @Override
    public String describe() {
        return "local trail in " + theDirectory + " (" + theRetentionDays + " day retention)";
    }

    private void countDrop(String theReason) {
        countDrops(1L, theReason);
    }

    private void countDrops(long theCount, String theReason) {
        if (theCount <= 0L) {
            return;
        }
        long theTotal = theDropped.addAndGet(theCount);
        theLog.error("Audit trail lost " + theCount + " record(s) (" + theTotal + " so far): "
                + theReason);
    }

    /** Close the active segment so the trail on disk is complete, and say what was lost. */
    public void close() {
        synchronized (theWriteLock) {
            rollActive();
        }
        if (theDropped.get() > 0L) {
            theLog.error("Audit trail is incomplete: " + theDropped.get()
                    + " record(s) were lost before their retention window expired.");
        }
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
