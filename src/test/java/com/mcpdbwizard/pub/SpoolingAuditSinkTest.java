package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the write-ahead audit spool.
 *
 * <p>The property worth the whole feature is {@link #recordsSurviveARestartOfTheServer()}: a record
 * accepted while the delegate was unavailable must still be delivered after the process has gone away
 * and come back. Everything else is detail.
 *
 * <p>The second is {@link #aSegmentIsNotDeletedUntilTheDelegateConfirms()}. Deleting on an
 * unconfirmed delivery is the one way a spool is <em>worse</em> than no spool: it converts "we lost
 * some records" into "we believe we have every record and do not".
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class SpoolingAuditSinkTest {

    /** A delegate that can be told to fail, and remembers what it was given. */
    private static final class CapturingSink implements McpAuditSink {
        final List<String> received = Collections.synchronizedList(new ArrayList<String>());
        volatile boolean healthy = true;
        volatile boolean closed = false;

        public void record(McpAuditEvent theEvent) {
            received.add(theEvent.toJson());
        }

        @Override
        public boolean flush() {
            if (!healthy) {
                // Model an async sink that accepted the calls and then failed to deliver.
                received.clear();
                return false;
            }
            return true;
        }

        public void close() {
            closed = true;
        }
    }

    private static McpAuditEvent event(String theTool) {
        return McpAuditEvent.of(theTool, null, "ok", 1, null, McpAuditSinks.Level.NAMES, 0);
    }

    private static SpoolingAuditSink sink(CapturingSink theDelegate, Path theDirectory) {
        // Segment size 1 so every record rolls immediately and draining is deterministic.
        return new SpoolingAuditSink(theDelegate, theDirectory, 1_000_000L, 1L, false, false);
    }

    @Test
    void aRecordIsDeliveredAndThenRemovedFromDisk(@TempDir Path theDirectory) {
        CapturingSink theDelegate = new CapturingSink();
        SpoolingAuditSink theSpool = sink(theDelegate, theDirectory);

        theSpool.record(event("get_customer"));
        assertEquals(1, theSpool.getPendingCount(), "written to disk before any delivery attempt");

        assertEquals(1, theSpool.drainOnce());
        assertEquals(1, theDelegate.received.size());
        assertTrue(theDelegate.received.get(0).contains("get_customer"));
        assertEquals(0, theSpool.getPendingCount(), "delivered records are removed");
    }

    @Test
    void aSegmentIsNotDeletedUntilTheDelegateConfirms(@TempDir Path theDirectory) {
        CapturingSink theDelegate = new CapturingSink();
        SpoolingAuditSink theSpool = sink(theDelegate, theDirectory);
        theDelegate.healthy = false;

        theSpool.record(event("t1"));
        theSpool.drainOnce();

        assertEquals(1, theSpool.getPendingCount(),
                "an unconfirmed delivery must leave the record on disk, not delete it");
        assertEquals(0, theSpool.getDeliveredCount());

        theDelegate.healthy = true;
        assertEquals(1, theSpool.drainOnce());
        assertEquals(0, theSpool.getPendingCount());
    }

    @Test
    void recordsSurviveARestartOfTheServer(@TempDir Path theDirectory) {
        // The whole point of the feature. Accept records while the delegate is broken, lose the
        // process entirely, then come back and deliver them.
        CapturingSink theFirstDelegate = new CapturingSink();
        theFirstDelegate.healthy = false;
        SpoolingAuditSink theFirstSpool = sink(theFirstDelegate, theDirectory);
        theFirstSpool.record(event("before_crash_1"));
        theFirstSpool.record(event("before_crash_2"));
        theFirstSpool.drainOnce();
        assertEquals(2, theFirstSpool.getPendingCount());
        // No close(): the process died.

        CapturingSink theSecondDelegate = new CapturingSink();
        SpoolingAuditSink theSecondSpool = sink(theSecondDelegate, theDirectory);

        assertEquals(2, theSecondSpool.drainOnce(), "the new process must pick up the old spool");
        assertEquals(2, theSecondDelegate.received.size());
        assertTrue(theSecondDelegate.received.toString().contains("before_crash_1"));
        assertTrue(theSecondDelegate.received.toString().contains("before_crash_2"));
    }

    @Test
    void aFullSpoolRefusesAndCountsRatherThanDiscardingWhatItHas(@TempDir Path theDirectory) {
        CapturingSink theDelegate = new CapturingSink();
        theDelegate.healthy = false;
        // Tiny cap: the first record fills it.
        SpoolingAuditSink theSpool = new SpoolingAuditSink(theDelegate, theDirectory, 10L, 1L, false, false);

        theSpool.record(event("first"));
        theSpool.record(event("second"));

        assertTrue(theSpool.getDroppedCount() >= 1, "a refused record must be counted");
        assertTrue(theSpool.getPendingCount() >= 1,
                "the records already accepted must NOT be discarded to make room");
    }

    @Test
    void aReplayedRecordIsByteIdenticalToWhatWasWritten(@TempDir Path theDirectory) {
        CapturingSink theDelegate = new CapturingSink();
        SpoolingAuditSink theSpool = sink(theDelegate, theDirectory);
        McpAuditEvent theEvent = McpAuditEvent.of("t", null, "ok", 5, "payload",
                McpAuditSinks.Level.VALUES, 0);
        String theOriginal = theEvent.toJson();

        theSpool.record(theEvent);
        theSpool.drainOnce();

        assertEquals(theOriginal, theDelegate.received.get(0),
                "a re-serialisation that differed would undermine the hash the record carries");
    }

    @Test
    void everyEventCarriesAnIdSoDuplicatesCanBeCollapsed() {
        // Delivery is at-least-once, so a consumer needs to be able to tell a replay from a new call.
        String theFirst = event("t").toJson();
        String theSecond = event("t").toJson();

        assertTrue(theFirst.matches("^\\{\"id\":\"[0-9a-f-]{36}\".*"), theFirst);
        assertFalse(theFirst.equals(theSecond));
    }

    @Test
    void closingDrainsAndClosesTheDelegate(@TempDir Path theDirectory) {
        CapturingSink theDelegate = new CapturingSink();
        SpoolingAuditSink theSpool = sink(theDelegate, theDirectory);

        theSpool.record(event("last"));
        theSpool.close();

        assertEquals(1, theDelegate.received.size(), "shutdown must deliver what is still spooled");
        assertTrue(theDelegate.closed);
    }

    // ---- an undecryptable segment ---------------------------------------------------------------

    /**
     * A segment that cannot be read must be QUARANTINED, not retried for ever and not deleted.
     *
     * <p>This is the state a rotated {@code MCP_AUDIT_SPOOL_KEY} leaves behind, and both obvious
     * behaviours are wrong: leaving it in place blocks every segment behind it for ever, and
     * deleting it destroys audit records. Simulated here by writing a line that claims to be
     * encrypted while no key is configured — the same failure the drainer sees.
     */
    @Test
    void aSegmentThatCannotBeReadIsMovedAsideRatherThanRetriedOrDeleted(@TempDir Path theDirectory)
            throws Exception {
        CapturingSink theDelegate = new CapturingSink();
        SpoolingAuditSink theSpool = sink(theDelegate, theDirectory);

        // One good record, delivered normally, so the spool is known to be working.
        theSpool.record(event("good_one"));
        theSpool.drainOnce();
        assertEquals(1, theDelegate.received.size());

        // A segment the drainer cannot read.
        Path theBad = theDirectory.resolve("segment-0000000000000-000000000000001.jsonl");
        Files.writeString(theBad, com.mcpdbwizard.pub.SpoolCipher.MARKER + "bm90LXJlYWxseQ==\n",
                java.nio.charset.StandardCharsets.UTF_8);

        theSpool.drainOnce();

        assertFalse(Files.exists(theBad), "the unreadable segment must not be left to retry for ever");
        assertTrue(Files.exists(theDirectory.resolve(
                        theBad.getFileName() + SpoolingAuditSink.QUARANTINE_SUFFIX)),
                "...and must not be deleted either - the records are kept for someone with the key");
        assertEquals(1, theDelegate.received.size(), "nothing extra was delivered");
    }

    /** A quarantined segment is not counted as pending: it is lost, not queued. */
    @Test
    void aQuarantinedSegmentIsNotReportedAsPending(@TempDir Path theDirectory) throws Exception {
        CapturingSink theDelegate = new CapturingSink();
        SpoolingAuditSink theSpool = sink(theDelegate, theDirectory);

        Files.writeString(theDirectory.resolve("segment-0000000000000-000000000000002.jsonl"),
                com.mcpdbwizard.pub.SpoolCipher.MARKER + "bm90LXJlYWxseQ==\n",
                java.nio.charset.StandardCharsets.UTF_8);
        theSpool.drainOnce();

        assertEquals(0, theSpool.getPendingCount(),
                "reporting it as pending would suggest it is still going to be delivered");
    }

    /**
     * End to end with a cipher: what lands on disk is unreadable, and what reaches the sink is the
     * original record. Constructed directly rather than through the environment, because the key is
     * read once at construction from a variable a test cannot set.
     */
    @Test
    void withACipherTheDiskIsUnreadableButDeliveryIsUnchanged(@TempDir Path theDirectory)
            throws Exception {
        CapturingSink theDelegate = new CapturingSink();
        SpoolingAuditSink theSpool = new SpoolingAuditSink(theDelegate, theDirectory,
                1_000_000L, 1L, false, false,
                new SpoolCipher(SpoolCipher.keyFrom("a-deployment-secret")));

        theSpool.record(McpAuditEvent.of("get_customer",
                java.util.Collections.<String, Object>singletonMap("p_id", "SECRET-VALUE"),
                "ok", 1, "{\"row\":\"SECRET-VALUE\"}", McpAuditSinks.Level.VALUES, 0));

        // Segment size 1 rolls the active file immediately, so read whatever is on disk.
        StringBuilder theBuilder = new StringBuilder();
        try (java.util.stream.Stream<Path> theFiles = Files.list(theDirectory)) {
            for (Path theFile : theFiles.toList()) {
                theBuilder.append(Files.readString(theFile, java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        String theOnDisk = theBuilder.toString();
        assertFalse(theOnDisk.contains("SECRET-VALUE"),
                "the spooled line must not carry the value in the clear: " + theOnDisk);
        assertTrue(theOnDisk.startsWith(SpoolCipher.MARKER), theOnDisk);

        theSpool.drainOnce();
        assertEquals(1, theDelegate.received.size());
        assertTrue(theDelegate.received.get(0).contains("SECRET-VALUE"),
                "the sink must receive the ORIGINAL record, byte for byte");
    }
}
