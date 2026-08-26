package com.mcpdbwizard.loadtest;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;

import java.time.Duration;

/**
 * Builds the MCP client each worker drives.
 *
 * <p>One place, because the two things this tool aims at differ only by URL and token:
 *
 * <ul>
 *   <li>a generated server — {@code http://127.0.0.1:8090/mcp}, token from {@code MCP_HTTP_TOKEN}
 *       if it was given one;</li>
 *   <li>the web proxy — {@code http://host:8080/mcp/{config}}, token being a user's API token,
 *       which the proxy swaps for the server's own before forwarding.</li>
 * </ul>
 *
 * <p>Measuring only the first is the easy mistake: the proxy is the path real clients use and the
 * only one carrying the per-caller rate limit, the call quota and the token check, so its cost is
 * part of the answer rather than overhead to be excluded.
 *
 * <p>The token is attached through the SDK's request customizer rather than by pre-building a
 * request, so it is applied to every request the transport makes — the POSTs, the GET that opens the
 * event stream, and the DELETE that ends the session. Setting it on the initialize call alone
 * produces a run that starts and then 401s, which reads as an expired token.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class McpClients {

    private McpClients() {
    }

    /**
     * A connected, initialized client.
     *
     * <p>Each worker gets its own: an MCP session is stateful and single-threaded, so sharing one
     * across threads would serialise the run and measure the client's lock rather than the server.
     */
    public static McpSyncClient connect(LoadOptions theOptions) {
        HttpClientStreamableHttpTransport.Builder theBuilder =
                HttpClientStreamableHttpTransport.builder(theOptions.baseUrl())
                        .endpoint(theOptions.endpointPath())
                        .connectTimeout(Duration.ofSeconds(20));
        String theToken = theOptions.token();
        if (theToken != null && !theToken.isEmpty()) {
            theBuilder.httpRequestCustomizer(
                (theRequest, theMethod, theUri, theBody, theContext) ->
                        theRequest.header("Authorization", "Bearer " + theToken));
        }
        McpSyncClient theClient = McpClient.sync(theBuilder.build())
                .requestTimeout(theOptions.requestTimeout())
                .initializationTimeout(Duration.ofSeconds(60))
                .clientInfo(new McpSchema.Implementation("mcpdbwizard-load", "1.0.0"))
                .build();
        theClient.initialize();
        return theClient;
    }

    /** Close without letting a shutdown problem obscure results that are already collected. */
    public static void closeQuietly(McpSyncClient theClient) {
        if (theClient == null) {
            return;
        }
        try {
            theClient.closeGracefully();
        } catch (RuntimeException e) {
            // Deliberately silent. The numbers are in hand; a failure to say goodbye is not a result.
        }
    }
}
