package com.mcpdbwizard.app.mcp;

import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import java.util.List;
import java.util.Map;

/**
 * An MCP server with one hard-coded tool and no database behind it.
 *
 * <h2>What it is for</h2>
 *
 * <p>A server that cannot start without Oracle cannot be introspected without Oracle, which makes
 * four ordinary things impossible: smoke-testing a client's wiring before introducing the database
 * as a variable, proving in CI that a build starts and speaks MCP, evaluating this project before
 * credentials exist, and demonstrating it without a live instance. This answers all four with one
 * tool and no configuration.
 *
 * <p>It is not a preview of the real thing. A generated server exposes the PL/SQL, tables,
 * statements and sequences <em>you</em> selected, each with a JSON Schema built from its actual
 * signature. This exposes one tool that returns a fixed sentence. The only thing the two have in
 * common is that a client can connect to them.
 *
 * <h2>Why the answer is a sentence and not an {@code X}</h2>
 *
 * <p>A real {@code SELECT * FROM DUAL} returns one column named {@code DUMMY} whose value is
 * {@code X}. This keeps the column name and replaces the value with {@link #NO_DATABASE_MESSAGE},
 * so the result has the shape a caller expects and the only thing that differs is the one thing
 * that should: the value says there is no database.
 *
 * <p><b>Do not "fix" the value back to {@code X} for fidelity.</b> The message is the entire point.
 * A stub answering {@code X} would look exactly like a working server to anyone who ran it without
 * reading anything.
 *
 * <h2>Running it</h2>
 *
 * <pre>
 * java -cp mcpdbwizard-app-&lt;version&gt;-shaded.jar com.mcpdbwizard.app.mcp.NoDatabaseStub
 * java -cp mcpdbwizard-app-&lt;version&gt;-shaded.jar com.mcpdbwizard.app.mcp.NoDatabaseStub http 8080
 * </pre>
 *
 * <p>stdio by default, Streamable HTTP when started with {@code http [port]} — the same convention
 * a generated {@code &lt;Factory&gt;McpServer} follows, so neither has to be learned separately.
 * The HTTP form serves {@link #HTTP_ENDPOINT}, which is what a tool fronting a container tries when
 * it has been told nothing more than the port.
 *
 * <p><b>Nothing here is authenticated</b>, because there is nothing behind it to protect: no
 * database, no data, no query path, one constant string.
 *
 * <h2>One definition, two hosts</h2>
 *
 * <p>{@link #toolSpecification()} is the single source of truth for the tool. The web console's own
 * no-database mode serves this same specification through a servlet rather than defining a second
 * one, so the two cannot drift into describing different things.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class NoDatabaseStub {

    /** The tool's name. */
    public static final String TOOL_NAME = "select_from_dual";

    /** The column name a real {@code DUAL} query returns. */
    public static final String COLUMN_NAME = "DUMMY";

    /** The value that column carries instead of {@code X}. */
    public static final String NO_DATABASE_MESSAGE = "database connection needed";

    /** The name this server reports to a client. */
    public static final String SERVER_NAME = "mcpdbwizard-no-database";

    /**
     * The path the HTTP form serves.
     *
     * <p>{@code /mcp} rather than something more explicit because it is what an outside tool tries:
     * {@code mcp-proxy}, which directory build checks use to front a container, serves port 8080 at
     * {@code /mcp} by default.
     *
     * <p><b>One path only, and that is a property of the SDK rather than a choice.</b> Its servlet
     * transport compares {@code request.getRequestURI()} against its configured endpoint with
     * {@code String.equals}, so a servlet registered at two URLs serves the configured one and 404s
     * the other — measured, after an attempt that mapped two and logged at start-up that both were
     * live.
     */
    public static final String HTTP_ENDPOINT = "/mcp";

    /** The default port for the HTTP form, matching what a container front-end expects. */
    public static final int DEFAULT_HTTP_PORT = 8080;

    private NoDatabaseStub() {
    }

    /**
     * The one tool: no arguments, one row, one column.
     *
     * <p>No input schema properties, deliberately. An argument would imply the value goes somewhere,
     * and there is nowhere for it to go.
     *
     * <p>The result is a JSON array of row objects keyed by column name, which is how a generated
     * server returns query rows — so a client that can read a real result can read this one.
     *
     * <p>The description says there is no database because that is the text an agent actually
     * receives. A caller that reads only the description should already know not to treat the value
     * as a query result.
     */
    public static SyncToolSpecification toolSpecification() {
        Tool theTool = Tool.builder()
                .name(TOOL_NAME)
                .description("Returns a single row with one column, " + COLUMN_NAME + ". This "
                        + "server has NO DATABASE CONNECTION: the value is a fixed message saying "
                        + "so, not a query result, and no SQL is executed. The tool exists to prove "
                        + "that an MCP client can reach this server before Oracle is configured.")
                .inputSchema(new JsonSchema("object", Map.of(), List.of(), Boolean.FALSE, null, null))
                .build();

        return SyncToolSpecification.builder()
                .tool(theTool)
                .callHandler((theExchange, theRequest) -> CallToolResult.builder()
                        .addTextContent(resultJson())
                        .build())
                .build();
    }

    /** The tool's answer, as the JSON text a caller receives. */
    public static String resultJson() {
        return "[{\"" + COLUMN_NAME + "\":\"" + NO_DATABASE_MESSAGE + "\"}]";
    }

    /** What this server tells a client it is, and what it deliberately is not. */
    public static String instructions() {
        return "This MCPDBWizard server has no Oracle connection, so it exposes no real data. The "
                + "single tool here returns a message saying so. It exists to prove that an MCP "
                + "client can reach it. Point MCPDBWizard at an Oracle schema and generate a server "
                + "to get tools built from your own PL/SQL, tables, statements and sequences, each "
                + "with a JSON Schema derived from its real signature.";
    }

    /**
     * stdio by default; {@code http [port]} to serve Streamable HTTP instead.
     *
     * <p>Never writes to stdout except through the transport. A stdio MCP server that prints
     * anything else has corrupted the protocol stream, which is why the HTTP branch's own progress
     * line goes to stderr.
     */
    public static void main(String[] theArgs) throws Exception {
        boolean theHttpFlag = theArgs.length > 0 && "http".equalsIgnoreCase(theArgs[0]);

        if (theHttpFlag) {
            int thePort = theArgs.length > 1 ? Integer.parseInt(theArgs[1]) : DEFAULT_HTTP_PORT;
            serveHttp(thePort);
        } else {
            serveStdio();
        }
    }

    /** Builds the server on a transport, with the one tool. */
    private static void build(Object theProvider) {
        if (theProvider instanceof StdioServerTransportProvider theStdio) {
            McpServer.sync(theStdio)
                    .serverInfo(SERVER_NAME, "1.0.0")
                    .instructions(instructions())
                    .capabilities(ServerCapabilities.builder().tools(false).logging().build())
                    .tools(List.of(toolSpecification()))
                    .build();
        } else {
            McpServer.sync((HttpServletStreamableServerTransportProvider) theProvider)
                    .serverInfo(SERVER_NAME, "1.0.0")
                    .instructions(instructions())
                    .capabilities(ServerCapabilities.builder().tools(false).logging().build())
                    .tools(List.of(toolSpecification()))
                    .build();
        }
    }

    private static void serveStdio() throws Exception {
        build(new StdioServerTransportProvider(
                new JacksonMcpJsonMapper(tools.jackson.databind.json.JsonMapper.builder().build())));
        // The stdio transport holds the process open on its own reader thread; park the main thread
        // rather than returning, which would let the JVM exit before a client said anything.
        Thread.currentThread().join();
    }

    private static void serveHttp(int thePort) throws Exception {
        HttpServletStreamableServerTransportProvider theProvider =
                HttpServletStreamableServerTransportProvider.builder()
                        .jsonMapper(new JacksonMcpJsonMapper(
                                tools.jackson.databind.json.JsonMapper.builder().build()))
                        .mcpEndpoint(HTTP_ENDPOINT)
                        .build();
        build(theProvider);

        org.eclipse.jetty.server.Server theServer = new org.eclipse.jetty.server.Server(thePort);
        org.eclipse.jetty.ee10.servlet.ServletContextHandler theContext =
                new org.eclipse.jetty.ee10.servlet.ServletContextHandler();
        theContext.setContextPath("/");
        theContext.addServlet(new org.eclipse.jetty.ee10.servlet.ServletHolder(theProvider),
                HTTP_ENDPOINT);
        theServer.setHandler(theContext);
        theServer.start();
        // stderr, not stdout: see the note on main().
        System.err.println("MCPDBWizard no-database stub listening on http://0.0.0.0:" + thePort
                + HTTP_ENDPOINT + " - one tool, " + TOOL_NAME + ", no authentication, no database.");
        theServer.join();
    }
}
