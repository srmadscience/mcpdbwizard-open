package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for the local audit trail.
 *
 * <p>Two properties are the feature and the rest is detail.
 *
 * <p>{@link #recordsPastTheWindowAreDeletedAndNotCountedAsLost()} — a retention store that never
 * deletes is not a retention store, and the deletion has to be invisible in the loss counters or a
 * healthy trail reads as a broken one for ever.
 *
 * <p>{@link #evictionForSpaceIsCountedAsLoss()} — the opposite case. Records thrown away early
 * because the volume filled were inside the window an operator was promised, and a trail that lost
 * them while claiming to be complete is worse than one that admits it.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class FileAuditSinkTest {

    private static McpAuditEvent event(String theTool) {
        return McpAuditEvent.of(theTool, null, "ok", 1L, null, McpAuditSinks.Level.NAMES, 0);
    }

    /** One segment per record, so the tests can reason about files rather than bytes. */
    private static FileAuditSink sink(Path theDirectory, int theRetentionDays) {
        return new FileAuditSink(theDirectory, theRetentionDays, 10L * 1024L * 1024L, 1L);
    }

    private static List<String> allLines(Path theDirectory) throws IOException {
        List<String> theLines = new ArrayList<String>();
        File[] theFiles = theDirectory.toFile().listFiles();
        if (theFiles != null) {
            java.util.Arrays.sort(theFiles);
            for (File theFile : theFiles) {
                theLines.addAll(Files.readAllLines(theFile.toPath(), StandardCharsets.UTF_8));
            }
        }
        return theLines;
    }

    private static File[] segments(Path theDirectory) {
        File[] theFiles = theDirectory.toFile().listFiles((d, n) ->
                n.startsWith(FileAuditSink.SEGMENT_PREFIX) && n.endsWith(FileAuditSink.SEGMENT_SUFFIX));
        return theFiles == null ? new File[0] : theFiles;
    }

    /** Backdate a segment by rewriting the millis in its name, which is where age comes from. */
    private static void backdateByDays(File theSegment, int theDays) {
        String theName = theSegment.getName();
        int theDash = theName.lastIndexOf('-');
        int theDot = theName.lastIndexOf(FileAuditSink.SEGMENT_SUFFIX);
        long theOld = Long.parseLong(theName.substring(theDash + 1, theDot));
        long theNew = theOld - (theDays * 24L * 60L * 60L * 1000L);
        File theTarget = new File(theSegment.getParentFile(),
                theName.substring(0, theDash + 1) + theNew + FileAuditSink.SEGMENT_SUFFIX);
        assertTrue(theSegment.renameTo(theTarget), "could not backdate the segment");
        assertTrue(theTarget.setLastModified(theNew), "could not backdate the mtime");
    }

    // ---- the two properties that matter ----

    @Test
    void recordsPastTheWindowAreDeletedAndNotCountedAsLost(@TempDir Path dir) {
        FileAuditSink theSink = sink(dir, 7);
        theSink.record(event("old_call"));
        theSink.record(event("forces_a_roll"));

        File[] theBefore = segments(dir);
        assertTrue(theBefore.length > 0, "expected a closed segment to prune");
        for (File theSegment : theBefore) {
            backdateByDays(theSegment, 30);
        }

        theSink.prune();

        assertEquals(0, segments(dir).length, "a segment 30 days past a 7 day window must be gone");
        // Ageing out is the policy working. Counting it would make every healthy trail look broken.
        assertEquals(0L, theSink.getDroppedCount());
    }

    @Test
    void evictionForSpaceIsCountedAsLoss(@TempDir Path dir) {
        // A byte cap small enough that the second record cannot fit beside the first.
        FileAuditSink theSink = new FileAuditSink(dir, 90, 200L, 1L);
        for (int i = 0; i < 12; i++) {
            theSink.record(event("call_" + i));
        }
        theSink.prune();

        assertTrue(theSink.getDroppedCount() > 0L,
                "records evicted before their window expired must be counted");
        assertTrue(theSink.getDeliveredCount() >= 12L);
    }

    // ---- retention window ----

    @Test
    void aRecordInsideTheWindowIsKept(@TempDir Path dir) throws Exception {
        FileAuditSink theSink = sink(dir, 7);
        theSink.record(event("recent_call"));
        theSink.close();

        theSink.prune();

        assertTrue(String.join("\n", allLines(dir)).contains("recent_call"));
    }

    @Test
    void theWindowIsMeasuredFromTheSegmentNameNotTheFileTimestamp(@TempDir Path dir) {
        FileAuditSink theSink = sink(dir, 1);
        theSink.record(event("a"));
        theSink.record(event("b"));

        File[] theSegments = segments(dir);
        assertTrue(theSegments.length > 0);
        // Backdate ONLY the name, and leave the mtime as "now". If pruning read the mtime this
        // segment would look fresh and survive -- which is the failure a restored backup causes.
        String theName = theSegments[0].getName();
        int theDash = theName.lastIndexOf('-');
        int theDot = theName.lastIndexOf(FileAuditSink.SEGMENT_SUFFIX);
        long theStale = System.currentTimeMillis() - (10L * 24L * 60L * 60L * 1000L);
        File theTarget = new File(dir.toFile(),
                theName.substring(0, theDash + 1) + theStale + FileAuditSink.SEGMENT_SUFFIX);
        assertTrue(theSegments[0].renameTo(theTarget));

        theSink.prune();

        assertTrue(!theTarget.exists(), "a segment named 10 days ago must not survive a 1 day window");
    }

    @Test
    void anUnparseableSegmentNameFallsBackToTheFileTimestamp(@TempDir Path dir) throws Exception {
        Path theOdd = dir.resolve(FileAuditSink.SEGMENT_PREFIX + "hand-renamed" + FileAuditSink.SEGMENT_SUFFIX);
        Files.writeString(theOdd, "{}\n");
        long theRecent = System.currentTimeMillis() - 1000L;
        assertTrue(theOdd.toFile().setLastModified(theRecent));

        // Zero, not the epoch: treating an unreadable name as 1970 would delete a file somebody
        // had just restored by hand.
        assertEquals(theRecent, FileAuditSink.openedAt(theOdd.toFile()));

        FileAuditSink theSink = sink(dir, 7);
        theSink.prune();
        assertTrue(Files.exists(theOdd), "a recently-touched segment must survive the window");
    }

    // ---- the zero window ----

    @Test
    void zeroDaysMeansNoTrailIsBuiltAtAll(@TempDir Path dir) {
        withSettings(dir.toString(), "0", () ->
                assertNull(FileAuditSink.fromEnvironment(null),
                        "a zero window must produce no sink, not an empty one"));
    }

    @Test
    void anAbsentWindowIsOneDayAndNeverZero() {
        assertEquals(1, FileAuditSink.retentionDays(null));
        assertEquals(1, FileAuditSink.retentionDays(""));
        assertEquals(1, FileAuditSink.retentionDays("   "));
        assertNotEquals(0, FileAuditSink.retentionDays(null));
    }

    @Test
    void aMistypedWindowStopsStartUpRatherThanGuessing() {
        // Consistent with MCP_AUDIT_LEVEL: a typo must not silently pick a window for you. In
        // particular it must never land on 0, which would keep nothing.
        assertThrows(IllegalArgumentException.class, () -> FileAuditSink.retentionDays("seven"));
        assertThrows(IllegalArgumentException.class, () -> FileAuditSink.retentionDays("-1"));
    }

    @Test
    void noDirectoryMeansNoTrail() {
        withSettings(null, "30", () -> assertNull(FileAuditSink.fromEnvironment(null)));
    }

    // ---- housekeeping ----

    @Test
    void everyRecordSurvivesARestart(@TempDir Path dir) throws Exception {
        FileAuditSink theFirst = sink(dir, 7);
        theFirst.record(event("before_restart"));
        theFirst.close();

        FileAuditSink theSecond = sink(dir, 7);
        theSecond.record(event("after_restart"));
        theSecond.close();

        String theTrail = String.join("\n", allLines(dir));
        assertTrue(theTrail.contains("before_restart"));
        assertTrue(theTrail.contains("after_restart"));
    }

    @Test
    void nothingIsEverPendingBecauseWrittenIsDelivered(@TempDir Path dir) {
        FileAuditSink theSink = sink(dir, 7);
        theSink.record(event("a"));
        assertEquals(0L, theSink.getPendingCount());
        assertEquals(1L, theSink.getDeliveredCount());
    }

    @Test
    void describeNamesTheDirectoryAndTheWindow(@TempDir Path dir) {
        assertTrue(sink(dir, 42).describe().contains("42"));
        assertTrue(sink(dir, 42).describe().contains(dir.toString()));
    }

    /** Run with the audit system properties set, and always put them back. */
    private static void withSettings(String theDirectory, String theRetention, Runnable theBody) {
        String theDirKey = McpAuditSinks.propertyNameFor(FileAuditSink.DIRECTORY_VARIABLE);
        String theDaysKey = McpAuditSinks.propertyNameFor(FileAuditSink.RETENTION_DAYS_VARIABLE);
        String theOldDir = System.getProperty(theDirKey);
        String theOldDays = System.getProperty(theDaysKey);
        try {
            if (theDirectory == null) {
                System.clearProperty(theDirKey);
            } else {
                System.setProperty(theDirKey, theDirectory);
            }
            System.setProperty(theDaysKey, theRetention);
            theBody.run();
        } finally {
            restore(theDirKey, theOldDir);
            restore(theDaysKey, theOldDays);
        }
    }

    private static void restore(String theKey, String theValue) {
        if (theValue == null) {
            System.clearProperty(theKey);
        } else {
            System.setProperty(theKey, theValue);
        }
    }
}
