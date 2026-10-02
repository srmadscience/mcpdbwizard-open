package com.mcpdbwizard.schema;

import com.mcpdbwizard.app.procbuilder.gui.ApplicationShell;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code MCP_CONTEXT_PARAM_<i>} key family through both config formats and the generator's
 * reader.
 *
 * <p>As with {@link McpInstructionsConfigKeyTest}, the first test is the one a round-trip cannot
 * replace: a family missing from {@code Schema}'s key switch falls through to
 * {@code extraProperties} and round-trips perfectly while nothing can read it.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class McpContextParamsConfigKeyTest {

    /** Out of order and with a gap at 2, as a hand-edited file might be. */
    private static Properties withParams() {
        Properties p = new Properties();
        p.setProperty("MCP_SERVER", "YES");
        p.setProperty("MCP_CONTEXT_PARAM_3", "REGION");
        p.setProperty("MCP_CONTEXT_PARAM_1", "CUSTOMER_ID");
        return p;
    }

    @Test
    void theFamilyIsModelledRatherThanSweptIntoExtras() {
        Schema theSchema = new Schema(withParams());

        assertEquals(List.of("CUSTOMER_ID", "REGION"), theSchema.getMcpContextParamNames(),
                "MCP_CONTEXT_PARAM_<i> did not reach the model, or not in index order");
        assertTrue(theSchema.getExtraProperties().isEmpty(),
                "a context parameter leaked into extraProperties: " + theSchema.getExtraProperties());
    }

    @Test
    void thePb2RoundTripKeepsTheGap() {
        Properties theOriginal = withParams();

        assertEquals(theOriginal, new Schema(theOriginal).toPb2());
    }

    @Test
    void theJsonRoundTripKeepsNamesAndIndices() {
        Schema theRebuilt = new Schema(new Schema(withParams()).toJson());

        assertEquals(List.of("CUSTOMER_ID", "REGION"), theRebuilt.getMcpContextParamNames());
        assertEquals(withParams(), theRebuilt.toPb2(), "lost an index crossing JSON");
    }

    /**
     * Absent stays absent in BOTH formats. The JSON half matters most: the committed .json configs
     * are byte-for-byte ConfigConverter output, so an always-written empty list would change every
     * one of them.
     */
    @Test
    void aConfigWithoutThemGainsNothing() {
        Properties theOriginal = new Properties();
        theOriginal.setProperty("MCP_SERVER", "YES");
        Schema theSchema = new Schema(theOriginal);

        assertTrue(theSchema.getMcpContextParamNames().isEmpty());
        assertEquals(theOriginal, theSchema.toPb2());
        assertFalse(theSchema.toJson().contains("mcpContextParams"),
                "an empty list was written into the JSON form");
    }

    @Test
    void settingNamesRenumbersFromOneAndNormalises() {
        Schema theSchema = new Schema(withParams());
        theSchema.setMcpContextParamNames(List.of(" customer_id ", "Region"));

        Properties thePb2 = theSchema.toPb2();
        assertEquals("CUSTOMER_ID", thePb2.getProperty("MCP_CONTEXT_PARAM_1"));
        assertEquals("REGION", thePb2.getProperty("MCP_CONTEXT_PARAM_2"));
        assertFalse(thePb2.containsKey("MCP_CONTEXT_PARAM_3"), "the old index 3 survived a replace");
    }

    /** The generator side reads the same file, and must agree with the model on order. */
    @Test
    void theGeneratorReadsTheSameNamesInTheSameOrder() {
        Properties theProperties = withParams();
        theProperties.setProperty("MCP_CONTEXT_PARAM_7", "  ");
        theProperties.setProperty("MCP_CONTEXT_PARAM_X", "NOT_OURS");

        assertEquals(new Schema(withParams()).getMcpContextParamNames(),
                ApplicationShell.readMcpContextParams(theProperties));
    }
}
