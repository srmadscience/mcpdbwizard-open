package com.mcpdbwizard.pub;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the MCP Java SDK's transport-level error bodies into JSON-RPC errors.
 *
 * <p><b>Why this exists.</b> SDK 2.0.0's Streamable HTTP transport answers its own errors -- no
 * session, an unknown session, an unparseable message, and any method it rejects before
 * {@code initialize} (such as the 2026-07-28 revision's {@code server/discover}) -- by serialising
 * the whole {@code McpError} EXCEPTION OBJECT: its JSON-RPC code and message, and also its
 * {@code stackTrace}, naming our classes, source files, line numbers and the Jetty and JDK versions.
 * Measured 2026-10-02. That body is not a JSON-RPC error at all, and the web proxy carries it to
 * whoever called.
 *
 * <p>The rewrite keeps exactly the code and the message and drops everything else, giving
 * {@code {"jsonrpc":"2.0","id":null,"error":{"code":..,"message":".."}}} -- {@code id} null because
 * the transport answered before reading one, as JSON-RPC prescribes for that case. The status code
 * is not touched: a 2026-07-28 client deciding whether we are a legacy server reads the status and
 * whether the body is one of ITS error types, and a plain JSON-RPC error is still not one, so the
 * documented fallback to {@code initialize} behaves exactly as before.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class McpErrorBodies {

    /** The SDK's shape: {@code "jsonRpcError":{"code":N,"message":"..."}} -- message already JSON-escaped. */
    private static final Pattern SDK_ERROR = Pattern.compile(
            "\"jsonRpcError\"\\s*:\\s*\\{\\s*\"code\"\\s*:\\s*(-?\\d+)\\s*,\\s*\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private McpErrorBodies() {
    }

    /**
     * The JSON-RPC error to send instead of an SDK transport error body, or null when the body is
     * not one (it is then passed on unchanged -- this rewrites the one shape it knows, and nothing
     * else).
     *
     * @param theBody the response body as written
     * @return the replacement body, or null
     */
    public static String jsonRpcErrorFor(String theBody) {
        if (theBody == null || !theBody.contains("\"jsonRpcError\"")) {
            return null;
        }
        Matcher m = SDK_ERROR.matcher(theBody);
        if (!m.find()) {
            return null;
        }
        return "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":" + m.group(1)
                + ",\"message\":\"" + m.group(2) + "\"}}";
    }
}
