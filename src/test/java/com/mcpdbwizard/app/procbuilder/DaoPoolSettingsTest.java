package com.mcpdbwizard.app.procbuilder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Database-free tests for {@link DaoPoolSettings}, the object that carries the {@code DAO_POOL_*}
 * config values into the generator.
 *
 * <p>Its whole job is to turn what a config file may or may not say into something the emission code
 * can use without null checks, so the interesting cases are the missing and malformed ones.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class DaoPoolSettingsTest {

    @Test
    void disabledIsWhatAConfigSayingNothingProduces() {
        DaoPoolSettings theSettings = DaoPoolSettings.disabled();

        assertFalse(theSettings.isEnabled(), "no config key means no pooling, and no emission");
    }

    @Test
    void missingValuesFallBackToTheSameDefaultsTheRuntimeUses() {
        // A config can enable pooling without naming any size. What it gets must match
        // com.mcpdbwizard.pub.DaoFactoryPoolConfig, or "said nothing" and "said the default" would
        // behave differently.
        DaoPoolSettings theSettings = new DaoPoolSettings(true, null, null, null, null, null);

        assertEquals("10", theSettings.getMaxSize());
        assertEquals("0", theSettings.getMinIdle());
        assertEquals("30000", theSettings.getMaxWaitMs());
        assertEquals("300000", theSettings.getIdleTimeoutMs());
        assertTrue(theSettings.isCommitOnReturn(), "matches the unpooled COMMIT_CONNECTIONS=YES default");
    }

    @Test
    void blankValuesAreTreatedAsMissingRatherThanEmitted() {
        DaoPoolSettings theSettings = new DaoPoolSettings(true, "  ", "", "   ", "", "");

        assertEquals("10", theSettings.getMaxSize());
        assertEquals("0", theSettings.getMinIdle());
        assertEquals("30000", theSettings.getMaxWaitMs());
        assertEquals("300000", theSettings.getIdleTimeoutMs());
    }

    @Test
    void suppliedValuesAreKeptAndTrimmed() {
        DaoPoolSettings theSettings =
                new DaoPoolSettings(true, " 24 ", "2", "5000", "120000", "ROLLBACK");

        assertTrue(theSettings.isEnabled());
        assertEquals("24", theSettings.getMaxSize());
        assertEquals("2", theSettings.getMinIdle());
        assertEquals("5000", theSettings.getMaxWaitMs());
        assertEquals("120000", theSettings.getIdleTimeoutMs());
        assertFalse(theSettings.isCommitOnReturn());
    }

    @Test
    void onlyRollbackTurnsTheCommitPolicyOff() {
        assertFalse(new DaoPoolSettings(true, null, null, null, null, "ROLLBACK").isCommitOnReturn());
        assertFalse(new DaoPoolSettings(true, null, null, null, null, " rollback ").isCommitOnReturn());

        assertTrue(new DaoPoolSettings(true, null, null, null, null, "COMMIT").isCommitOnReturn());
        // Anything unrecognised keeps the safer-for-existing-behaviour default rather than silently
        // switching a config to rollback.
        assertTrue(new DaoPoolSettings(true, null, null, null, null, "MAYBE").isCommitOnReturn());
    }
}
