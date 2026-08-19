package com.mcpdbwizard.test;

import com.mcpdbwizard.pub.ConsoleLog;
import com.mcpdbwizard.pub.LogInterface;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Base class that makes a migrated legacy {@link TestInterface} harness directly runnable
 * under JUnit when it needs <b>no database</b>. It owns the {@code theLog} field the harness
 * bodies expect and exposes a single {@code @Test} that invokes {@code test(true)} and
 * asserts success; failure detail is written to the harness's own log.
 * <p>
 * Each concrete harness {@code extends} this class and provides {@code getTestName()} /
 * {@code test(boolean)}.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
abstract class AbstractLocalHarness implements TestInterface {

    protected LogInterface theLog = new ConsoleLog();

    @Test
    void runHarness() {
        assertTrue(test(true), getTestName());
    }
}
