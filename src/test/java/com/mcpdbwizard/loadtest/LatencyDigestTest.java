package com.mcpdbwizard.loadtest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Percentile arithmetic, and the two edges that produce a wrong number rather than an exception.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class LatencyDigestTest {

    private static LatencyDigest oneToOneHundred() {
        LatencyDigest theDigest = new LatencyDigest();
        // Added descending, so a test that reads a percentile without sorting first gets it wrong.
        for (int i = 100; i >= 1; i--) {
            theDigest.add(i);
        }
        return theDigest;
    }

    @Test
    void anEmptyDigestReportsMinusOneRatherThanThrowing() {
        LatencyDigest theDigest = new LatencyDigest();
        assertTrue(theDigest.isEmpty());
        assertEquals(-1L, theDigest.percentile(0.5));
        assertEquals(-1L, theDigest.min());
        assertEquals(-1L, theDigest.max());
        assertEquals(0.0, theDigest.mean());
    }

    @Test
    void percentilesAreReadFromSortedSamples() {
        LatencyDigest theDigest = oneToOneHundred();
        assertEquals(100, theDigest.size());
        assertEquals(1L, theDigest.min());
        assertEquals(100L, theDigest.max());
        assertEquals(50L, theDigest.percentile(0.50));
        assertEquals(90L, theDigest.percentile(0.90));
        assertEquals(99L, theDigest.percentile(0.99));
    }

    /**
     * The obvious {@code (int)(count * fraction)} form indexes one past the end at 1.0, which is an
     * AIOOBE in the middle of printing a report that already cost five minutes to collect.
     */
    @Test
    void theTopAndBottomOfTheRangeAreClamped() {
        LatencyDigest theDigest = oneToOneHundred();
        assertEquals(100L, theDigest.percentile(1.0));
        assertEquals(100L, theDigest.percentile(1.5));
        assertEquals(1L, theDigest.percentile(0.0));
        assertEquals(1L, theDigest.percentile(-0.5));
    }

    @Test
    void aSingleSampleIsEveryPercentile() {
        LatencyDigest theDigest = new LatencyDigest();
        theDigest.add(7L);
        assertEquals(7L, theDigest.percentile(0.0));
        assertEquals(7L, theDigest.percentile(0.999));
        assertEquals(7L, theDigest.percentile(1.0));
    }

    @Test
    void addingAfterReadingReSorts() {
        LatencyDigest theDigest = oneToOneHundred();
        assertEquals(100L, theDigest.max());
        theDigest.add(5L);
        assertEquals(101, theDigest.size());
        assertEquals(5L, theDigest.percentile(0.04));
        assertEquals(100L, theDigest.max());
    }

    @Test
    void mergingTwoDigestsKeepsEverySample() {
        LatencyDigest theFirst = new LatencyDigest();
        LatencyDigest theSecond = new LatencyDigest();
        for (int i = 0; i < 2000; i++) {
            theFirst.add(1L);
            theSecond.add(3L);
        }
        theFirst.addAll(theSecond);
        assertEquals(4000, theFirst.size());
        assertEquals(1L, theFirst.min());
        assertEquals(3L, theFirst.max());
        assertEquals(2.0, theFirst.mean(), 0.0001);
    }
}
