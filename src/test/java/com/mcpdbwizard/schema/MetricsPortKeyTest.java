package com.mcpdbwizard.schema;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code METRICS_PORT} round-trips through both config formats.
 *
 * <p>Adding a scalar means touching eight separate places in {@link Schema} — the field, the full
 * constructor and its assignment, the getter/setter, the known-keys list, {@code fromProperties},
 * {@code toPb2}, and the two JSON map halves. Missing one is silent: the value simply disappears on
 * the next save, or worse lands in {@code extraProperties} and round-trips while no code can read
 * it. {@link SchemaRoundTripTest} cannot catch it, because it runs over the committed propfiles and
 * none of them sets this key.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class MetricsPortKeyTest {

    @Test
    void itSurvivesPb2ToSchemaToPb2() {
        Properties theOriginal = new Properties();
        theOriginal.setProperty("METRICS_PORT", "9464");

        Schema theSchema = new Schema(theOriginal);
        assertEquals("9464", theSchema.getMetricsPort());
        assertEquals("9464", theSchema.toPb2().getProperty("METRICS_PORT"));
    }

    @Test
    void itSurvivesTheJsonLeg() {
        Schema theSchema = new Schema();
        theSchema.setMetricsPort("9464");

        Schema theReloaded = new Schema(theSchema.toJson());
        assertEquals("9464", theReloaded.getMetricsPort());
        assertEquals("9464", theReloaded.toPb2().getProperty("METRICS_PORT"));
    }

    @Test
    void itIsARecognisedKeyRatherThanAnUnknownOne() {
        // The real trap. A key missing from the known-keys list still round-trips - through
        // extraProperties - so a test that only checked the value would pass while nothing in the
        // application could read it. extraProperties must stay EMPTY.
        Properties theOriginal = new Properties();
        theOriginal.setProperty("METRICS_PORT", "9464");
        assertTrue(new Schema(theOriginal).getExtraProperties().isEmpty(),
                "METRICS_PORT landed in extraProperties, so it is not in the known-keys list");
    }

    @Test
    void absentStaysAbsentRatherThanBecomingEmpty() {
        // The Schema contract: a null scalar means the key was absent and must stay absent, so
        // every config written before this option existed is byte-identical after a load and save.
        Schema theLegacy = new Schema(new Properties());
        assertNull(theLegacy.getMetricsPort());
        assertFalse(theLegacy.toPb2().containsKey("METRICS_PORT"));
    }
}
