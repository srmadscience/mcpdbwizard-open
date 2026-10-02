package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The SDK transport error body, rewritten. The fixture is the start of a body measured from a real
 * generated server on 2026-10-02 (POST server/discover with no session), stack trace included.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class McpErrorBodiesTest {

    private static final String SDK_BODY = "{\"jsonRpcError\":{\"code\":-32601,\"message\":\"Session ID required in"
            + " mcp-session-id header\"},\"cause\":null,\"localizedMessage\":\"Session ID required\",\"message\":"
            + "\"Session ID required\",\"stackTrace\":[{\"classLoaderName\":\"app\",\"className\":"
            + "\"io.modelcontextprotocol.spec.McpError$Builder\",\"fileName\":\"McpError.java\",\"lineNumber\":81}],"
            + "\"suppressed\":[]}";

    @Test
    void theStackTraceIsGoneAndTheCodeAndMessageKept() {
        String theClean = McpErrorBodies.jsonRpcErrorFor(SDK_BODY);
        assertEquals("{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32601,"
                + "\"message\":\"Session ID required in mcp-session-id header\"}}", theClean);
        assertFalse(theClean.contains("stackTrace") || theClean.contains("McpError.java"));
    }

    /** The message can carry what the caller sent (an unknown session id); its escaping must survive. */
    @Test
    void anEscapedQuoteInTheMessageSurvives() {
        String theClean = McpErrorBodies.jsonRpcErrorFor(
                "{\"jsonRpcError\":{\"code\":-32603,\"message\":\"Session not found: a\\\"b\"},\"stackTrace\":[]}");
        assertEquals("{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32603,\"message\":\"Session not found: a\\\"b\"}}",
                theClean);
    }

    /** Only the one shape it knows: anything else -- ours, or a proper JSON-RPC error -- is left alone. */
    @Test
    void otherBodiesAreNotTouched() {
        assertNull(McpErrorBodies.jsonRpcErrorFor(null));
        assertNull(McpErrorBodies.jsonRpcErrorFor(""));
        assertNull(McpErrorBodies.jsonRpcErrorFor("{\"jsonrpc\":\"2.0\",\"id\":1,\"error\":{\"code\":-32601,\"message\":\"x\"}}"));
        assertNull(McpErrorBodies.jsonRpcErrorFor("Forbidden"));
    }
}
