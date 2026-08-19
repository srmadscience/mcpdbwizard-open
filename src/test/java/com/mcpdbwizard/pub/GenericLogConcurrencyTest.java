package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The log's date formatter must not be shared between threads.
 *
 * <p>Background: a pooled server warms several factories at once and each logs as it starts. With a
 * single shared {@code SimpleDateFormat} — which the constructor also reassigned — that produced an
 * intermittent {@code ArrayIndexOutOfBoundsException} from inside {@code format()}, surfacing as
 * "Could not obtain a DAO factory from the pool" and pointing at the pool rather than at logging.
 *
 * <p><b>The load test below is a smoke test, not the guard.</b> It was written first and it passed
 * against the broken code, because every thread formats roughly the same instant: a corrupted shared
 * calendar then yields a value that is wrong but still plausible, and the exception is rare. The real
 * guard is {@link #theFormatterIsNotSharedBetweenThreads()}, which asserts the structure that made
 * the race possible and fails deterministically on the old field.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class GenericLogConcurrencyTest {

    /** yyyy/MM/dd HH:mm:ss.S z — what a correctly formatted stamp looks like. */
    private static final Pattern WELL_FORMED =
            Pattern.compile("^\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d+ \\S+:.*");

    @Test
    void theFormatterIsNotSharedBetweenThreads() {
        // Deterministic: a static SimpleDateFormat is shared by every log in the JVM, and
        // SimpleDateFormat is not thread-safe. No amount of load testing substitutes for not
        // having one.
        for (Field theField : GenericLog.class.getDeclaredFields()) {
            if (SimpleDateFormat.class.isAssignableFrom(theField.getType())
                    && Modifier.isStatic(theField.getModifiers())) {
                fail("GenericLog." + theField.getName() + " is a shared static "
                        + theField.getType().getSimpleName()
                        + "; SimpleDateFormat is not thread-safe, so every logging thread races on it");
            }
        }
    }

    @Test
    void concurrentFormattingSmokeTest() throws Exception {
        final ConsoleLog theLog = new ConsoleLog();
        final AtomicReference<Throwable> theFailure = new AtomicReference<Throwable>();
        final AtomicReference<String> theGarbled = new AtomicReference<String>();

        int theThreads = 16;
        ExecutorService thePool = Executors.newFixedThreadPool(theThreads);
        final CountDownLatch theStart = new CountDownLatch(1);
        final CountDownLatch theDone = new CountDownLatch(theThreads);

        for (int t = 0; t < theThreads; t++) {
            thePool.submit(() -> {
                try {
                    theStart.await();
                    for (int i = 0; i < 3000; i++) {
                        String theMessage = theLog.formatMessage(LogInterface.INFO, "hello");
                        if (!WELL_FORMED.matcher(theMessage).matches()) {
                            theGarbled.compareAndSet(null, theMessage);
                        }
                    }
                } catch (Throwable e) {
                    theFailure.compareAndSet(null, e);
                } finally {
                    theDone.countDown();
                }
            });
        }

        theStart.countDown();
        assertTrue(theDone.await(60, TimeUnit.SECONDS), "threads did not finish");
        thePool.shutdownNow();

        assertNull(theFailure.get(), () -> "formatting threw: " + theFailure.get());
        assertNull(theGarbled.get(), () -> "corrupted timestamp: " + theGarbled.get());
    }

    @Test
    void theFormatItselfIsUnchanged() {
        // The fix must not alter what a log line looks like.
        assertEquals("yyyy/MM/dd HH:mm:ss.S z", new ConsoleLog().getDateFormat());
        assertTrue(WELL_FORMED.matcher(new ConsoleLog().formatMessage(LogInterface.INFO, "x")).matches());
    }
}
