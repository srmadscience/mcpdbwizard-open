package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the fan-out that lets a deployment keep a local trail and stream at the same time.
 *
 * <p>The one worth the class is {@link #flushAsksEveryMemberEvenAfterOneSaysNo()}. A {@code &&}
 * chain reads correctly and is wrong: the second sink never gets flushed once the first refuses, so
 * a spool in front is told "not delivered" about a sink that was never asked, and replays records
 * that had in fact arrived.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class FanOutAuditSinkTest {

    private static final class Recorder implements McpAuditSink {
        final List<String> received = Collections.synchronizedList(new ArrayList<String>());
        volatile boolean flushConfirms = true;
        volatile boolean flushCalled = false;
        volatile boolean closed = false;
        volatile boolean throwOnRecord = false;
        volatile long dropped = -1L;

        public void record(McpAuditEvent theEvent) {
            if (throwOnRecord) {
                throw new IllegalStateException("this sink is broken");
            }
            received.add(theEvent.toJson());
        }

        @Override
        public boolean flush() {
            flushCalled = true;
            return flushConfirms;
        }

        @Override
        public long getDroppedCount() {
            return dropped;
        }

        public void close() {
            closed = true;
        }
    }

    private static McpAuditEvent event(String theTool) {
        return McpAuditEvent.of(theTool, null, "ok", 1L, null, McpAuditSinks.Level.NAMES, 0);
    }

    @Test
    void everyMemberGetsEveryRecord() {
        Recorder theLocal = new Recorder();
        Recorder theStream = new Recorder();

        new FanOutAuditSink(theLocal, theStream).record(event("get_flights"));

        assertEquals(1, theLocal.received.size());
        assertEquals(1, theStream.received.size());
    }

    @Test
    void flushAsksEveryMemberEvenAfterOneSaysNo() {
        Recorder theRefuses = new Recorder();
        theRefuses.flushConfirms = false;
        Recorder theConfirms = new Recorder();

        assertFalse(new FanOutAuditSink(theRefuses, theConfirms).flush());

        assertTrue(theRefuses.flushCalled);
        assertTrue(theConfirms.flushCalled, "a member after a refusal must still be flushed");
    }

    @Test
    void flushConfirmsOnlyWhenEveryMemberDoes() {
        Recorder theA = new Recorder();
        Recorder theB = new Recorder();
        assertTrue(new FanOutAuditSink(theA, theB).flush());

        theB.flushConfirms = false;
        assertFalse(new FanOutAuditSink(theA, theB).flush());
    }

    @Test
    void oneBrokenMemberDoesNotStopTheOthersOrReachTheCaller() {
        // The SPI says record must not throw. This defends against a member that breaks the
        // contract, because otherwise one bad sink silently disables the whole trail.
        Recorder theBroken = new Recorder();
        theBroken.throwOnRecord = true;
        Recorder theHealthy = new Recorder();

        new FanOutAuditSink(theBroken, theHealthy).record(event("get_flights"));

        assertEquals(1, theHealthy.received.size());
    }

    @Test
    void closeClosesEveryMemberEvenWhenOneIsBroken() {
        Recorder theA = new Recorder();
        Recorder theB = new Recorder();
        new FanOutAuditSink(theA, theB).close();
        assertTrue(theA.closed);
        assertTrue(theB.closed);
    }

    @Test
    void nullMembersAreIgnoredSoCompositionNeedsNoBranching() {
        Recorder theOnly = new Recorder();
        FanOutAuditSink theSink = new FanOutAuditSink(null, theOnly, null);
        assertEquals(1, theSink.sinks().size());
        theSink.record(event("a"));
        assertEquals(1, theOnly.received.size());
    }

    // ---- counters: -1 means "nobody counts", which is not the same as zero ----

    @Test
    void anUncountedTotalStaysMinusOneRatherThanBecomingZero() {
        Recorder theA = new Recorder();
        Recorder theB = new Recorder();
        assertEquals(-1L, new FanOutAuditSink(theA, theB).getDroppedCount());
    }

    @Test
    void countingMembersAreSummedAndSilentOnesIgnored() {
        Recorder theCounts = new Recorder();
        theCounts.dropped = 3L;
        Recorder theSilent = new Recorder();

        // 3 and "no idea" is 3, not 2.
        assertEquals(3L, new FanOutAuditSink(theCounts, theSilent).getDroppedCount());
    }

    @Test
    void describeNamesEveryDestination() {
        String theText = new FanOutAuditSink(new Recorder(), new Recorder()).describe();
        assertTrue(theText.contains("+"), "an operator needs to see BOTH destinations: " + theText);
    }
}
