package com.mcpdbwizard.test;

import com.mcpdbwizard.app.mcp.NoDatabaseStub;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shared no-database stub definition, which lives in this module because this is the module the
 * open-source repository publishes.
 *
 * <p>Needs no database, which is the point of the thing being tested.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class NoDatabaseStubTest {

    @Test
    void theToolIsNamedAsSpecified() {
        assertEquals("select_from_dual", NoDatabaseStub.TOOL_NAME);
        assertEquals("DUMMY", NoDatabaseStub.COLUMN_NAME);
        assertEquals("database connection needed", NoDatabaseStub.NO_DATABASE_MESSAGE);
    }

    /**
     * The endpoint is what an outside tool tries. {@code mcp-proxy} serves port 8080 at
     * {@code /mcp} by default, and a container front-end told only the port will try that.
     */
    @Test
    void theHttpEndpointIsWhatAFrontEndExpects() {
        assertEquals("/mcp", NoDatabaseStub.HTTP_ENDPOINT);
        assertEquals(8080, NoDatabaseStub.DEFAULT_HTTP_PORT);
    }

    @Test
    void theToolTakesNoArguments() {
        SyncToolSpecification theSpec = NoDatabaseStub.toolSpecification();
        assertEquals("select_from_dual", theSpec.tool().name());
        Object theProperties = theSpec.tool().inputSchema().get("properties");
        assertTrue(theProperties == null || ((java.util.Map<?, ?>) theProperties).isEmpty(),
                "an argument would imply the value goes somewhere, and there is nowhere for it to go");
    }

    /**
     * The description must say there is no database: it is what an agent actually receives, and a
     * caller that reads only the description should already know not to trust the value.
     */
    @Test
    void theDescriptionSaysThereIsNoDatabase() {
        String theDescription = NoDatabaseStub.toolSpecification().tool().description();
        assertTrue(theDescription.toUpperCase().contains("NO DATABASE CONNECTION"), theDescription);
        assertTrue(theDescription.contains("no SQL is executed"), theDescription);
    }

    /**
     * The value carries the message, not {@code X}.
     *
     * <p>Asserted deliberately: a later "fix" restoring {@code DUAL} fidelity would make the stub
     * indistinguishable from a working server, which is what the design exists to avoid.
     */
    @Test
    void theColumnCarriesTheMessageRatherThanX() {
        String theJson = NoDatabaseStub.resultJson();
        assertEquals("[{\"DUMMY\":\"database connection needed\"}]", theJson);
        assertFalse(theJson.contains("\"DUMMY\":\"X\""),
                "the column must carry the message; restoring X would hide that there is no database");
    }

    /**
     * The instructions must not oversell it. A client reading them should understand this is not a
     * preview of a generated server.
     */
    @Test
    void theInstructionsSayWhatItIsNot() {
        String theInstructions = NoDatabaseStub.instructions();
        assertTrue(theInstructions.contains("no Oracle connection"), theInstructions);
        assertTrue(theInstructions.contains("exposes no real data"), theInstructions);
    }
}
