package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Database-free tests for {@link DaoFactoryPoolConfig}: the baked-in defaults, the deployment
 * overrides, and the range checks.
 *
 * <p>Only the system-property half of the override path is exercised — a test cannot set an
 * environment variable in its own JVM. The two share {@code override()}, so what is untested here is
 * the {@code System.getenv} call itself.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class DaoFactoryPoolConfigTest {

    /** Run something with system properties set, and clear them afterwards whatever happens. */
    private static void withProperties(String[] theNamesAndValues, Runnable theWork) {
        try {
            for (int i = 0; i < theNamesAndValues.length; i += 2) {
                System.setProperty(theNamesAndValues[i], theNamesAndValues[i + 1]);
            }
            theWork.run();
        } finally {
            for (int i = 0; i < theNamesAndValues.length; i += 2) {
                System.clearProperty(theNamesAndValues[i]);
            }
        }
    }

    @Test
    void defaultsAreUsableWithoutAnyConfiguration() {
        DaoFactoryPoolConfig theConfig = new DaoFactoryPoolConfig();

        assertEquals(10, theConfig.getMaxSize());
        assertEquals(0, theConfig.getMinIdle(), "an idle server should be able to give every session back");
        assertEquals(30000L, theConfig.getMaxWaitMillis());
        assertEquals(300000L, theConfig.getIdleTimeoutMillis());
        assertTrue(theConfig.isCommitOnReturn(), "matches the unpooled COMMIT_CONNECTIONS=YES default");
        assertTrue(theConfig.isValidateOnBorrow());
    }

    @Test
    void deploymentCanRetuneWithoutRegenerating() {
        withProperties(new String[]{
                "dao.pool.maxSize", "40",
                "dao.pool.minIdle", "4",
                "dao.pool.maxWaitMillis", "2500",
                "dao.pool.idleTimeoutMillis", "60000",
                "dao.pool.onReturn", "ROLLBACK",
                "dao.pool.validateOnBorrow", "false"}, () -> {

            DaoFactoryPoolConfig theConfig = new DaoFactoryPoolConfig()
                    .setMaxSize(10)
                    .applyOverrides();

            assertEquals(40, theConfig.getMaxSize(), "the override must beat the baked-in value");
            assertEquals(4, theConfig.getMinIdle());
            assertEquals(2500L, theConfig.getMaxWaitMillis());
            assertEquals(60000L, theConfig.getIdleTimeoutMillis());
            assertFalse(theConfig.isCommitOnReturn());
            assertFalse(theConfig.isValidateOnBorrow());
        });
    }

    @Test
    void bakedInValuesSurviveWhenNothingOverridesThem() {
        DaoFactoryPoolConfig theConfig = new DaoFactoryPoolConfig()
                .setMaxSize(7)
                .setMinIdle(2)
                .applyOverrides();

        assertEquals(7, theConfig.getMaxSize());
        assertEquals(2, theConfig.getMinIdle());
    }

    @Test
    void blankOverrideIsTreatedAsAbsent() {
        withProperties(new String[]{"dao.pool.maxSize", "   "}, () -> {
            DaoFactoryPoolConfig theConfig = new DaoFactoryPoolConfig().setMaxSize(6).applyOverrides();
            assertEquals(6, theConfig.getMaxSize());
        });
    }

    @Test
    void onReturnAcceptsEitherPolicyInAnyCase() {
        withProperties(new String[]{"dao.pool.onReturn", "commit"}, () ->
                assertTrue(new DaoFactoryPoolConfig().setCommitOnReturn(false).applyOverrides()
                        .isCommitOnReturn()));

        withProperties(new String[]{"dao.pool.onReturn", "Rollback"}, () ->
                assertFalse(new DaoFactoryPoolConfig().applyOverrides().isCommitOnReturn()));
    }

    @Test
    void validateOnBorrowAlsoTakesTheYesNoSpellingTheConfigFilesUse() {
        withProperties(new String[]{"dao.pool.validateOnBorrow", "NO"}, () ->
                assertFalse(new DaoFactoryPoolConfig().applyOverrides().isValidateOnBorrow()));

        withProperties(new String[]{"dao.pool.validateOnBorrow", "YES"}, () ->
                assertTrue(new DaoFactoryPoolConfig().setValidateOnBorrow(false).applyOverrides()
                        .isValidateOnBorrow()));
    }

    // ---- refusing to start on a bad setting is deliberate --------------------------------------

    @Test
    void anUnparseableOverrideStopsStartupRatherThanBeingIgnored() {
        withProperties(new String[]{"dao.pool.maxSize", "lots"}, () -> {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> new DaoFactoryPoolConfig().applyOverrides());
            assertTrue(e.getMessage().contains("DAO_POOL_MAX_SIZE"),
                    "the message should name the key an operator set: " + e.getMessage());
        });
    }

    @Test
    void anUnrecognisedOnReturnPolicyStopsStartup() {
        withProperties(new String[]{"dao.pool.onReturn", "MAYBE"}, () -> {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> new DaoFactoryPoolConfig().applyOverrides());
            assertTrue(e.getMessage().contains("COMMIT"), e.getMessage());
        });
    }

    @Test
    void nonsensicalSizesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new DaoFactoryPoolConfig().setMaxSize(0).validateSettings());

        assertThrows(IllegalArgumentException.class,
                () -> new DaoFactoryPoolConfig().setMinIdle(-1).validateSettings());

        assertThrows(IllegalArgumentException.class,
                () -> new DaoFactoryPoolConfig().setMaxSize(4).setMinIdle(5).validateSettings(),
                "a floor above the ceiling cannot be honoured");

        assertThrows(IllegalArgumentException.class,
                () -> new DaoFactoryPoolConfig().setIdleTimeoutMillis(0).validateSettings());

        assertThrows(IllegalArgumentException.class,
                () -> new DaoFactoryPoolConfig().setMaxWaitMillis(-1).validateSettings());
    }

    @Test
    void toStringNamesEverySettingSoAServerCanLogIt() {
        String theText = new DaoFactoryPoolConfig().setMaxSize(12).setMinIdle(3).toString();

        assertTrue(theText.contains("maxSize=12"), theText);
        assertTrue(theText.contains("minIdle=3"), theText);
        assertTrue(theText.contains("onReturn=COMMIT"), theText);
    }
}
