package com.mcpdbwizard.schema;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The {@code MCP_INSTRUCTIONS} key through both config formats.
 *
 * <p><b>The first test is the one that earns its place, and a round-trip test cannot replace it.</b>
 * A key that is not listed in {@code Schema.SCALAR_KEYS} does not fail — it falls through to
 * {@code extraProperties}, where it round-trips <em>perfectly</em> while nothing in the application
 * can read it. {@link SchemaRoundTripTest} would stay green with the key completely unwired, and so
 * would the two round-trip tests below. Only asking for the value back through its accessor
 * distinguishes "preserved" from "preserved and usable", and that distinction has already caught
 * this class of mistake once, on {@code TABLE_MCP_DESC}.
 *
 * <p>{@code SchemaRoundTripTest} also cannot cover this key at all: it runs over the committed
 * propfiles and none of them sets one, so it proves the key stays <em>absent</em> — the other half
 * of what matters — and never that it survives when present.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class McpInstructionsConfigKeyTest {

    /** Quotes and a backslash on purpose: this text ends up inside a Java string literal. */
    private static final String INSTRUCTIONS =
            "Bookings for the \"north\" region only. Never cancel a flight with passengers on it. "
                    + "Paths look like C:\\schedules.";

    private static Properties withInstructions() {
        Properties p = new Properties();
        p.setProperty("MCP_SERVER", "YES");
        p.setProperty("MCP_INSTRUCTIONS", INSTRUCTIONS);
        return p;
    }

    @Test
    void theKeyIsModelledRatherThanSweptIntoExtras() {
        Schema theSchema = new Schema(withInstructions());

        assertEquals(INSTRUCTIONS, theSchema.getMcpInstructions(),
                "MCP_INSTRUCTIONS did not reach its field - if it is missing from SCALAR_KEYS it"
                        + " still round-trips through extraProperties, so the round-trip tests"
                        + " below would pass while nothing could read the value");
        assertFalse(theSchema.getExtraProperties().containsKey("MCP_INSTRUCTIONS"),
                "MCP_INSTRUCTIONS leaked into extraProperties");
    }

    @Test
    void theKeySurvivesAPb2RoundTrip() {
        Properties theOriginal = withInstructions();
        Properties theRebuilt = new Schema(theOriginal).toPb2();

        assertEquals(theOriginal, theRebuilt);
    }

    @Test
    void theKeySurvivesAJsonRoundTrip() {
        Schema theSchema = new Schema(withInstructions());

        assertEquals(INSTRUCTIONS, new Schema(theSchema.toJson()).getMcpInstructions(),
                "lost crossing JSON");
    }

    /**
     * Absent must stay absent. A config that never set this must not gain an empty key on its first
     * save, because that is what keeps its generated output byte-identical.
     */
    @Test
    void anAbsentKeyIsNotInvented() {
        Properties theOriginal = new Properties();
        theOriginal.setProperty("MCP_SERVER", "YES");

        Schema theSchema = new Schema(theOriginal);

        assertNull(theSchema.getMcpInstructions());
        assertFalse(theSchema.toPb2().containsKey("MCP_INSTRUCTIONS"),
                "an absent MCP_INSTRUCTIONS was written out as a key");
    }
}
