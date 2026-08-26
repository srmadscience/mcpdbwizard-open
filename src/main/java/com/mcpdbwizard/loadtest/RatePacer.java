package com.mcpdbwizard.loadtest;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * Hands out the instant at which each call should <em>start</em>, so a run holds a target rate.
 *
 * <h2>Open loop, and why that is the point</h2>
 *
 * <p>Slots are scheduled on a fixed cadence from the run's start — call <i>n</i> is due at
 * {@code start + n/rate} — and that schedule never bends. When the server slows down, this does
 * <em>not</em> slow down with it: the run simply falls behind its own schedule, and how far behind
 * it fell is the measurement.
 *
 * <p>The alternative (a worker that sleeps for a fixed gap <em>after</em> each reply) is a closed
 * loop, and it quietly lets the server set the rate. Ask such a tool for 200 calls/sec against a
 * server that can only do 50 and it reports 50 calls/sec with excellent latency, because it only
 * ever had one call outstanding. Nothing in its output says the target was missed by four times.
 *
 * <h2>The consequence for latency</h2>
 *
 * <p>Because the schedule is fixed, a call's <b>scheduled latency</b> — from the instant it was
 * <em>due</em> to the instant the reply arrived — is not the same as its <b>service time</b>, from
 * the instant it was actually sent. When a run falls behind, service time stays flat and looks
 * healthy while scheduled latency grows without bound. Reporting only the first is the classic
 * coordinated-omission error, so {@link LoadWorker} records both and {@link LoadReport} prints both.
 *
 * <p>An unpaced pacer ({@link #unpaced()}) is the flat-out case: every slot is due immediately, so
 * scheduled latency and service time coincide and the report says so.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class RatePacer {

    /**
     * How long before the slot to stop parking and start spinning.
     *
     * <p>{@link LockSupport#parkNanos} wakes on the operating system's timer, and it overshoots in
     * proportion to how long it was asked to wait — measured on one ordinary machine: 0.25 ms after
     * a 1 ms park, 3.3 ms after a 20 ms one. At a 50/sec cadence a single long park therefore put a
     * 4.4 ms floor under every scheduled latency: an artefact of the measuring tool, several times
     * larger than the sub-millisecond figures it existed to measure.
     *
     * <p>So the wait is done in three parts: park in halving steps (each one short enough that its
     * own overshoot stays well inside what is left), then a final park down to this threshold, then
     * spin. The spin costs at most this much CPU per call per thread — a small fraction of a core at
     * any realistic rate — and an unpaced run never waits at all and never pays it.
     */
    private static final long SPIN_THRESHOLD_NANOS = 2_000_000L;

    /**
     * Lateness below this is the clock's, not the run's.
     *
     * <p>Even after spinning, a slot can be missed by a few microseconds. Counting those as "started
     * late" would mark most of a perfectly healthy run behind schedule and make the figure useless
     * for spotting a run that genuinely is.
     */
    private static final long LATE_TOLERANCE_NANOS = 200_000L;

    private final double theRatePerSecond;

    private final long theStartNanos;

    private final AtomicLong theSlotCounter = new AtomicLong();

    private RatePacer(double theRateValue, long theStartValue) {
        this.theRatePerSecond = theRateValue;
        this.theStartNanos = theStartValue;
    }

    /** No pacing: every slot is due at the run's start, so workers never wait. */
    public static RatePacer unpaced(long theStartNanos) {
        return new RatePacer(0.0, theStartNanos);
    }

    /**
     * @param theRateValue  target calls per second across every thread; zero or less means unpaced
     * @param theStartNanos the run's origin, on the {@link System#nanoTime()} scale
     */
    public static RatePacer at(double theRateValue, long theStartNanos) {
        return theRateValue > 0.0 ? new RatePacer(theRateValue, theStartNanos)
                : unpaced(theStartNanos);
    }

    public boolean isPaced() {
        return theRatePerSecond > 0.0;
    }

    public double ratePerSecond() {
        return theRatePerSecond;
    }

    /**
     * When call {@code theIndex} is due, on the {@link System#nanoTime()} scale.
     *
     * <p>Pure arithmetic with no clock read, which is what makes the schedule testable.
     */
    public long slotNanosFor(long theIndex) {
        if (!isPaced()) {
            return theStartNanos;
        }
        return theStartNanos + Math.round(theIndex / theRatePerSecond * 1_000_000_000.0);
    }

    /**
     * Claim the next slot in the schedule.
     *
     * <p>Shared across every worker, so the rate is a property of the run rather than of one
     * thread — eight threads at a target of 100/sec issue 100 calls a second between them, not 800.
     *
     * @return the index of the claimed slot, counting from zero
     */
    public long claimSlot() {
        return theSlotCounter.getAndIncrement();
    }

    /**
     * Wait for this slot, if there is anything to wait for.
     *
     * <p>An unpaced run has no schedule, so it never waits and is never late — without this guard
     * every slot is nominally due at the run's start and every call after the first is reported as
     * having started late, which is true of the arithmetic and meaningless as a finding.
     *
     * @return nanoseconds late, or zero
     */
    public long awaitSlot(long theDueNanos) {
        return isPaced() ? parkUntil(theDueNanos) : 0L;
    }

    /**
     * Wait until the given instant, unless it has already passed.
     *
     * <p>Returns how late the caller already is, which is the number the report needs: a run that
     * never waits is a run that never reached its target rate.
     *
     * @param theDueNanos the slot instant, from {@link #slotNanosFor}
     * @return nanoseconds late, or zero when the slot was met (within the clock's own tolerance)
     */
    public static long parkUntil(long theDueNanos) {
        long theRemaining = theDueNanos - System.nanoTime();
        while (theRemaining > SPIN_THRESHOLD_NANOS) {
            // Half of what is left, never the whole of it: a park's overshoot is a fraction of its
            // own length, so halving keeps every overshoot comfortably inside the remaining wait.
            LockSupport.parkNanos(Math.max(1L, (theRemaining - SPIN_THRESHOLD_NANOS) / 2L));
            theRemaining = theDueNanos - System.nanoTime();
        }
        // Deliberately does NOT try to "catch up" gently when already late: the schedule is fixed,
        // so the right thing is to go now and let the lateness be reported.
        while (System.nanoTime() < theDueNanos) {
            Thread.onSpinWait();
        }
        long theShortfall = System.nanoTime() - theDueNanos;
        return theShortfall > LATE_TOLERANCE_NANOS ? theShortfall : 0L;
    }
}
