package com.mcpdbwizard.loadtest;

import java.util.Arrays;

/**
 * A growable list of microsecond samples with percentile arithmetic.
 *
 * <p>A run does not know how many samples it will take — a duration budget has no call count — so
 * this grows rather than being sized up front, and it stores primitives rather than boxing every
 * measurement into a {@code List<Long>}. At a few thousand calls a second that difference is the
 * measuring instrument's own overhead, which is exactly the thing a load tool must not spend.
 *
 * <p><b>Every sample is kept.</b> This is deliberately not a streaming estimator: the runs here are
 * minutes long on one machine, so exact percentiles are affordable, and an approximate p999 is the
 * number most likely to be quoted and least likely to be checked.
 *
 * <p>Not thread-safe. Each worker owns its own digests and they are merged once, after the run.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class LatencyDigest {

    private long[] theValues = new long[1024];

    private int theCount;

    /** Cleared by {@link #add}, set by {@link #sortIfNeeded}, so a read after a write re-sorts. */
    private boolean theSortedFlag = true;

    private long theSum;

    /** Add one sample. */
    public void add(long theMicros) {
        if (theCount == theValues.length) {
            theValues = Arrays.copyOf(theValues, theValues.length * 2);
        }
        theValues[theCount++] = theMicros;
        theSum += theMicros;
        theSortedFlag = false;
    }

    /** Absorb another digest's samples. */
    public void addAll(LatencyDigest theOther) {
        for (int i = 0; i < theOther.theCount; i++) {
            add(theOther.theValues[i]);
        }
    }

    public int size() {
        return theCount;
    }

    public boolean isEmpty() {
        return theCount == 0;
    }

    public long sum() {
        return theSum;
    }

    /** Arithmetic mean in microseconds, or 0 when there are no samples. */
    public double mean() {
        return theCount == 0 ? 0.0 : (double) theSum / theCount;
    }

    /**
     * The sample at the given fraction, by nearest rank.
     *
     * @param theFraction 0.0 for the minimum, 1.0 for the maximum
     * @return the sample in microseconds, or -1 when there are none
     */
    public long percentile(double theFraction) {
        if (theCount == 0) {
            return -1L;
        }
        sortIfNeeded();
        // Nearest rank, so p50 of 1..100 is 50 rather than 50.5 or 51: the smallest sample at or
        // above the fraction of the population. Clamped rather than trusted, because 0.0 lands on
        // -1 and 1.0 on the count -- both an ArrayIndexOutOfBoundsException in the middle of
        // printing a report that already cost five minutes to collect.
        int theIndex = (int) Math.ceil(theFraction * theCount) - 1;
        if (theIndex < 0) {
            theIndex = 0;
        } else if (theIndex >= theCount) {
            theIndex = theCount - 1;
        }
        return theValues[theIndex];
    }

    public long min() {
        return percentile(0.0);
    }

    public long max() {
        return percentile(1.0);
    }

    /** A copy of the samples, sorted ascending. */
    public long[] sortedCopy() {
        sortIfNeeded();
        return Arrays.copyOf(theValues, theCount);
    }

    private void sortIfNeeded() {
        if (!theSortedFlag) {
            Arrays.sort(theValues, 0, theCount);
            theSortedFlag = true;
        }
    }
}
