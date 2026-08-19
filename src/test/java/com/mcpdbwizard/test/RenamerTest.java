package com.mcpdbwizard.test;

import com.mcpdbwizard.app.common.LegalOracleNameWranger;
import com.mcpdbwizard.pub.ConsoleLog;
import com.mcpdbwizard.pub.LogInterface;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the Oracle-name -> legal-Java-name renamer's self-test. {@link LegalOracleNameWranger}
 * lives in the main source tree and carries the legacy self-test contract
 * ({@code getTestName()} / {@code test(boolean)}) but is not a {@code T*} harness in this
 * package, so it gets its own thin JUnit wrapper rather than extending {@link
 * AbstractLocalHarness}.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class RenamerTest {

    private final LogInterface log = new ConsoleLog();

    @Test
    void legalOracleNameRenamer() {
        LegalOracleNameWranger harness = new LegalOracleNameWranger(log);
        assertTrue(harness.test(true), harness.getTestName());
    }
}
