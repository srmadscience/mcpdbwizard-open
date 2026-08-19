package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the optional call-rate limit.
 *
 * <p>The case that matters most is {@link #anUnconfiguredLimiterPermitsEverything()}: this is opt-in,
 * so every existing deployment must behave exactly as before. After that, the interesting ones are
 * the refusals to guess — a mistyped limit has to stop start-up rather than silently leave the server
 * unlimited, which is the failure mode where an operator believes they are protected and is not.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class McpRateLimiterTest {

    @Test
    void anUnconfiguredLimiterPermitsEverything() {
        McpRateLimiter theLimiter = McpRateLimiter.fromSettings(null, null);

        assertFalse(theLimiter.isEnabled());
        for (int i = 0; i < 10000; i++) {
            assertTrue(theLimiter.tryAcquire(), "an unset limit must never refuse");
        }
    }

    @Test
    void anEmptySettingIsTheSameAsUnset() {
        assertFalse(McpRateLimiter.fromSettings("", "   ").isEnabled());
    }

    @Test
    void theBurstIsWhatMayArriveAtOnce() {
        // 5/sec with a burst of 3: three immediately, then refusal until the bucket refills.
        McpRateLimiter theLimiter = McpRateLimiter.fromSettings("5", "3");

        assertTrue(theLimiter.tryAcquire());
        assertTrue(theLimiter.tryAcquire());
        assertTrue(theLimiter.tryAcquire());
        assertFalse(theLimiter.tryAcquire(), "the fourth exceeds the bucket depth");
    }

    @Test
    void theBucketRefillsOverTime() throws Exception {
        McpRateLimiter theLimiter = McpRateLimiter.fromSettings("50", "1");

        assertTrue(theLimiter.tryAcquire());
        assertFalse(theLimiter.tryAcquire());

        // 50/sec is a token every 20ms; 200ms is ample even on a loaded machine.
        Thread.sleep(200);
        assertTrue(theLimiter.tryAcquire(), "the bucket must refill as time passes");
    }

    @Test
    void theBurstDefaultsToOneSecondsWorth() {
        // Not an arbitrary constant: a burst below the rate would make the configured rate
        // unreachable.
        McpRateLimiter theLimiter = McpRateLimiter.fromSettings("7", null);

        assertEquals(7.0, theLimiter.getRatePerSecond(), 0.0001);
        assertEquals(7.0, theLimiter.getBurst(), 0.0001);
    }

    @Test
    void aBurstBelowOneIsRaisedSoAtLeastOneCallCanEverProceed() {
        // A fractional rate with a matching burst would otherwise refuse every call forever.
        McpRateLimiter theLimiter = McpRateLimiter.fromSettings("0.5", null);

        assertTrue(theLimiter.getBurst() >= 1.0, "got " + theLimiter.getBurst());
        assertTrue(theLimiter.tryAcquire());
    }

    @Test
    void aMistypedLimitStopsStartUpRatherThanDisablingItself() {
        // Silently treating "onehundred" as unlimited would leave an operator believing they had a
        // limit. Fail loudly instead.
        assertThrows(IllegalArgumentException.class, () -> McpRateLimiter.fromSettings("onehundred", null));
        assertThrows(IllegalArgumentException.class, () -> McpRateLimiter.fromSettings("0", null));
        assertThrows(IllegalArgumentException.class, () -> McpRateLimiter.fromSettings("-5", null));
        assertThrows(IllegalArgumentException.class, () -> McpRateLimiter.fromSettings("10", "rubbish"));
    }

    @Test
    void aBurstWithoutARateIsRefusedRatherThanIgnored() {
        // Setting only the burst reads as "I configured a limit" but would be unlimited.
        IllegalArgumentException theException = assertThrows(IllegalArgumentException.class,
                () -> McpRateLimiter.fromSettings(null, "20"));

        assertTrue(theException.getMessage().contains(McpRateLimiter.RATE_VARIABLE),
                theException.getMessage());
    }

    @Test
    void concurrentCallersCannotOverdrawTheBucket() throws Exception {
        // The bucket is shared state, so the count admitted must not exceed the depth however many
        // threads race for it.
        final int theBurst = 50;
        final McpRateLimiter theLimiter = McpRateLimiter.fromSettings("0.001", Integer.toString(theBurst));
        final AtomicInteger theAdmitted = new AtomicInteger();

        int theThreads = 16;
        ExecutorService thePool = Executors.newFixedThreadPool(theThreads);
        final CountDownLatch theStart = new CountDownLatch(1);
        final CountDownLatch theDone = new CountDownLatch(theThreads);

        for (int t = 0; t < theThreads; t++) {
            thePool.submit(() -> {
                try {
                    theStart.await();
                    for (int i = 0; i < 200; i++) {
                        if (theLimiter.tryAcquire()) {
                            theAdmitted.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    theDone.countDown();
                }
            });
        }

        theStart.countDown();
        assertTrue(theDone.await(60, TimeUnit.SECONDS));
        thePool.shutdownNow();

        // The refill rate is negligible over the run, so the burst is the whole budget.
        assertTrue(theAdmitted.get() <= theBurst,
                "admitted " + theAdmitted.get() + " with a burst of " + theBurst);
        assertEquals(theBurst, theAdmitted.get(), "and it should hand out the whole budget");
    }

    @Test
    void itDescribesItselfForTheStartUpLog() {
        assertEquals("no rate limit", McpRateLimiter.disabled().toString());
        assertTrue(McpRateLimiter.fromSettings("10", "20").toString().contains("10"));
    }
}
