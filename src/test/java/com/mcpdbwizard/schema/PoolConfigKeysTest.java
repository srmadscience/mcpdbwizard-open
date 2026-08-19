package com.mcpdbwizard.schema;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code DAO_POOL_*} keys through both config formats.
 *
 * <p>{@link SchemaRoundTripTest} cannot cover them: it runs over the committed propfiles, and none
 * of those carries a pooling setting — so it proves the keys stay <em>absent</em>, which is the other
 * half of what matters, but never that they survive when present.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class PoolConfigKeysTest {

    private static Properties poolProperties() {
        Properties p = new Properties();
        p.setProperty("DAO_POOL", "YES");
        p.setProperty("DAO_POOL_MAX_SIZE", "24");
        p.setProperty("DAO_POOL_MIN_IDLE", "2");
        p.setProperty("DAO_POOL_MAX_WAIT_MS", "5000");
        p.setProperty("DAO_POOL_IDLE_TIMEOUT_MS", "120000");
        p.setProperty("DAO_POOL_ON_RETURN", "ROLLBACK");
        return p;
    }

    private static void assertPoolValues(Schema theSchema) {
        assertEquals("YES", theSchema.getDaoPool());
        assertEquals("24", theSchema.getDaoPoolMaxSize());
        assertEquals("2", theSchema.getDaoPoolMinIdle());
        assertEquals("5000", theSchema.getDaoPoolMaxWaitMs());
        assertEquals("120000", theSchema.getDaoPoolIdleTimeoutMs());
        assertEquals("ROLLBACK", theSchema.getDaoPoolOnReturn());
    }

    @Test
    void poolKeysAreModelledRatherThanSweptIntoExtras() {
        Schema theSchema = new Schema(poolProperties());

        assertPoolValues(theSchema);
        for (String theKey : theSchema.getExtraProperties().keySet()) {
            assertFalse(theKey.startsWith("DAO_POOL"),
                    "DAO_POOL key leaked into extraProperties: " + theKey);
        }
    }

    @Test
    void poolKeysSurvivePb2RoundTrip() {
        Properties theOriginal = poolProperties();
        Properties theRebuilt = new Schema(theOriginal).toPb2();

        assertEquals(theOriginal, theRebuilt);
    }

    @Test
    void poolKeysSurviveJsonRoundTrip() {
        Schema theSchema = new Schema(poolProperties());
        Schema theReloaded = new Schema(theSchema.toJson());

        assertPoolValues(theReloaded);
        assertEquals(poolProperties(), theReloaded.toPb2());
    }

    @Test
    void aConfigWithNoPoolingSettingsStaysThatWay() {
        // The reason every existing propfile is byte-identical after a load/save: a null scalar
        // means the key was absent, and toPb2() must not invent a default for it.
        Schema theSchema = new Schema(new Properties());

        assertNull(theSchema.getDaoPool());
        assertNull(theSchema.getDaoPoolMaxSize());
        assertNull(theSchema.getDaoPoolOnReturn());

        Properties theRebuilt = theSchema.toPb2();
        for (Object theKey : theRebuilt.keySet()) {
            assertFalse(theKey.toString().startsWith("DAO_POOL"),
                    "toPb2() invented a pooling key that was never set: " + theKey);
        }
    }

    @Test
    void anEmptyPoolValueIsPresentButEmptyNotAbsent() {
        Properties theOriginal = new Properties();
        theOriginal.setProperty("DAO_POOL_MAX_SIZE", "");

        Schema theSchema = new Schema(theOriginal);
        assertEquals("", theSchema.getDaoPoolMaxSize());
        assertTrue(theSchema.toPb2().containsKey("DAO_POOL_MAX_SIZE"));
    }
}
