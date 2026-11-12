package com.mcpdbwizard.loadtest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The schedule, tested as arithmetic rather than by waiting for it.
 *
 * <p>The whole of {@link RatePacer} that decides <em>when</em> a call happens is a pure function of
 * the slot index, deliberately, so these assertions are exact and instant. Only
 * {@link RatePacer#parkUntil} reads a clock, and it is one branch.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class RatePacerTest {

    private static final long ORIGIN = 1_000_000_000_000L;

    @Test
    void anUnpacedRunHasEverySlotDueImmediately() {
        RatePacer thePacer = RatePacer.unpaced(ORIGIN);
        assertFalse(thePacer.isPaced());
        assertEquals(ORIGIN, thePacer.slotNanosFor(0L));
        assertEquals(ORIGIN, thePacer.slotNanosFor(1_000_000L));
    }

    @Test
    void aRateOfZeroOrLessIsTheUnpacedCase() {
        assertFalse(RatePacer.at(0.0, ORIGIN).isPaced());
        assertFalse(RatePacer.at(-5.0, ORIGIN).isPaced());
    }

    @Test
    void slotsFallOnAFixedCadenceFromTheStart() {
        RatePacer thePacer = RatePacer.at(100.0, ORIGIN);
        assertTrue(thePacer.isPaced());
        assertEquals(ORIGIN, thePacer.slotNanosFor(0L));
        assertEquals(ORIGIN + 10_000_000L, thePacer.slotNanosFor(1L));
        assertEquals(ORIGIN + 1_000_000_000L, thePacer.slotNanosFor(100L));
    }

    /**
     * The property the whole open-loop design rests on: slot n is due at start + n/rate, computed
     * from the origin every time, so an early slot running late cannot push the later ones out.
     * A cadence accumulated by adding a gap to "now" would drift by exactly the lateness.
     */
    @Test
    void theScheduleDoesNotDriftWithTheRunsProgress() {
        RatePacer thePacer = RatePacer.at(250.0, ORIGIN);
        for (long i = 0; i < 100_000L; i += 7L) {
            assertEquals(ORIGIN + Math.round(i / 250.0 * 1_000_000_000.0),
                    thePacer.slotNanosFor(i));
        }
    }

    /**
     * Without this, every call after the first in a flat-out run is "late" against a slot that was
     * nominally due when the run began — arithmetically true and useless as a finding.
     */
    @Test
    void anUnpacedRunNeverWaitsAndIsNeverLate() {
        RatePacer thePacer = RatePacer.unpaced(System.nanoTime() - 60_000_000_000L);
        assertEquals(0L, thePacer.awaitSlot(thePacer.slotNanosFor(1_000L)));
    }

    @Test
    void aPacedRunReportsLatenessThroughTheSameEntryPoint() {
        RatePacer thePacer = RatePacer.at(1.0, System.nanoTime() - 60_000_000_000L);
        assertTrue(thePacer.awaitSlot(thePacer.slotNanosFor(0L)) > 0L);
    }

    @Test
    void slotsAreClaimedOnceEachAcrossEveryCaller() {
        RatePacer thePacer = RatePacer.at(10.0, ORIGIN);
        assertEquals(0L, thePacer.claimSlot());
        assertEquals(1L, thePacer.claimSlot());
        assertEquals(2L, thePacer.claimSlot());
    }

    @Test
    void aSlotAlreadyPastIsTakenImmediatelyAndReportsTheShortfall() {
        long theLongAgo = System.nanoTime() - 500_000_000L;
        long theLate = RatePacer.parkUntil(theLongAgo);
        assertTrue(theLate > 0L, "an overdue slot must report how late it is, got " + theLate);
    }

    /**
     * The wait must not OVERSHOOT either. A plain parkNanos wakes on the OS timer and lands a
     * millisecond or more past the slot, which showed up as a 4.4 ms floor under every scheduled
     * latency in a 50/sec run — the tool measuring itself rather than the server.
     *
     * <p>Judged on the MEDIAN of 21 waits, not on one. A single wait on a shared CI runner can be
     * preempted after its spin and land late through no fault of the pacer: one did, 2.04 ms past
     * a 1 ms bound (2026-11-10, three failed CI runs in two days, each green on a rerun). A
     * preemption disturbs a few samples; a pacer that genuinely overshoots, as plain parkNanos
     * does, overshoots every time and moves the median. So the bound stays 1 ms, and an early
     * wake is still refused on every sample.
     */
    @Test
    void aSlotInTheFutureIsWaitedForAndMetRatherThanOvershot() {
        long[] theOvershoots = new long[21];
        for (int i = 0; i < theOvershoots.length; i++) {
            long theSoon = System.nanoTime() + 5_000_000L;
            // Not asserted per sample: a preempted wait is reported late (2.02 ms in CI, 2026-11-11),
            // correctly. Lateness IS overshoot past the pacer's tolerance, so the median bounds it.
            RatePacer.parkUntil(theSoon);
            theOvershoots[i] = System.nanoTime() - theSoon;
            assertTrue(theOvershoots[i] >= 0L, "returned before the slot, overshoot " + theOvershoots[i]);
        }
        long[] theSorted = theOvershoots.clone();
        java.util.Arrays.sort(theSorted);
        long theMedian = theSorted[theSorted.length / 2];
        assertTrue(theMedian < 1_000_000L, "median wake " + theMedian + "ns past the slot; all: "
                + java.util.Arrays.toString(theOvershoots));
    }
}
