package com.mcpdbwizard.pub;

import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code --list-tools} arm that {@code SAAdminWrangler.generateMcpServerClass} emits at the top
 * of a generated server's {@code main}.
 *
 * <p><b>What is pinned here is the property the arm depends on:</b> that a tool specification can be
 * built, read and serialized <em>without its call handler ever running</em>. That is what lets the
 * mode answer with no database, no Oracle session and no MCP handshake — and it is not obvious from
 * reading the emitted source, because the handler sitting right there in the builder chain is the
 * one thing that would need a connection.
 *
 * <p>Kept db-free deliberately. The web console runs this mode on every generation to fill the
 * Service Options panel, so it is on a path a contributor without an Oracle box still exercises.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class GeneratedMcpToolListingShapeTest {

    /** Fails the test if anything asks it for a result — the point being that nothing does. */
    private static SyncToolSpecification specificationLike(String theName) {
        return new SyncToolSpecification.Builder()
                .tool(Tool.builder()
                        .name(theName)
                        .description("Fetch one row from table OB_TSTZ_TEST by primary key (id (NUMBER)).")
                        .inputSchema(Map.of(
                                "type", "object",
                                "properties", Map.of("id", Map.of(
                                        "type", "number", "description", "Oracle NUMBER")),
                                "required", List.of("id"),
                                "additionalProperties", Boolean.FALSE))
                        .build())
                .callHandler((exchange, request) -> {
                    throw new AssertionError("--list-tools must never invoke a call handler:"
                            + " that is the half that needs a database.");
                })
                .build();
    }

    /** The emitted loop, verbatim in shape: specifications in, Tools out, one result object. */
    private static ListToolsResult listing(SyncToolSpecification... theSpecifications) {
        List<Tool> theTools = new ArrayList<Tool>();
        for (SyncToolSpecification theSpecification : Arrays.asList(theSpecifications)) {
            theTools.add(theSpecification.tool());
        }
        return new ListToolsResult(theTools, null);
    }

    @Test
    void theListingIsReadableWithoutInvokingAnyHandler() {
        ListToolsResult theResult = listing(
                specificationLike("ob_tstz_test_get_by_pk"), specificationLike("ob_tstz_test_insert"));

        assertEquals(2, theResult.tools().size());
        assertEquals("ob_tstz_test_get_by_pk", theResult.tools().get(0).name());
    }

    @Test
    void itSerializesToTheToolsListPayloadAClientWouldReceive() throws Exception {
        // The same mapper the generated server builds, so this is the payload itself rather than
        // a re-serialization that could differ from what the transport writes.
        String theJson = new JacksonMcpJsonMapper(tools.jackson.databind.json.JsonMapper.builder().build())
                .writeValueAsString(listing(specificationLike("ob_tstz_test_get_by_pk")));

        assertTrue(theJson.contains("\"tools\""), theJson);
        assertTrue(theJson.contains("\"ob_tstz_test_get_by_pk\""), theJson);
        // The schema is the part a caller cannot get any other way without connecting.
        assertTrue(theJson.contains("\"inputSchema\""), theJson);
        assertTrue(theJson.contains("Oracle NUMBER"), theJson);
    }

    @Test
    void aServerWithOneToolStillProducesAWellFormedListing() throws Exception {
        // Guards the degenerate end of the loop: one tool, no separator logic to get wrong.
        String theJson = new JacksonMcpJsonMapper(tools.jackson.databind.json.JsonMapper.builder().build())
                .writeValueAsString(listing(specificationLike("only_tool")));
        assertFalse(theJson.contains("null"), "a null cursor should be omitted, not written: " + theJson);
    }

    @Test
    void theHandlerIsWhatWouldHaveNeededTheDatabase() {
        // States the contract the other way round, so the test fails loudly if somebody ever makes
        // the listing path touch a handler: this is the call --list-tools must not make.
        SyncToolSpecification theSpecification = specificationLike("ob_tstz_test_get_by_pk");
        try {
            CallToolResult theUnreachable = theSpecification.callHandler().apply(null, null);
            throw new AssertionError("expected the handler to be the part that cannot run: "
                    + theUnreachable);
        } catch (AssertionError expected) {
            assertTrue(expected.getMessage().contains("never invoke a call handler"), expected.getMessage());
        }
    }
}
