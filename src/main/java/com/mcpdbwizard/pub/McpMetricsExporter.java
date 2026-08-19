package com.mcpdbwizard.pub;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Serves a {@link McpMetrics} registry at {@code /metrics} for Prometheus to scrape.
 *
 * <table>
 *   <caption>Environment</caption>
 *   <tr><td>{@code MCP_METRICS_PORT}</td>
 *       <td>Port to listen on. <b>Unset means no listener</b> — see below.</td></tr>
 *   <tr><td>{@code MCP_METRICS_HOST}</td>
 *       <td>Address to bind, default {@value #DEFAULT_BIND_HOST}.</td></tr>
 * </table>
 *
 * <h2>Why there is no default port</h2>
 *
 * <p>Generation-time {@code PROMETHEUS_SERVER=YES} emits this; the environment variable is what
 * starts it. The same split as {@code MCP_HTTP_TOKEN}, and here it is not merely tidy: the web
 * application launches up to twenty generated servers at once and the start-up harness forks
 * twenty-one per box. A default port would give one of them a listener and the other twenty a bind
 * failure to log, every run.
 *
 * <h2>It uses the JDK's own HTTP server, and it is a separate port on purpose</h2>
 *
 * <p>{@code com.sun.net.httpserver} costs no dependency and works identically under both MCP
 * transports — a stdio server has no Jetty to hang a servlet on, and stdio is where a locally-run
 * server lives. A separate port also keeps the scrape off {@code /mcp/*}, whose bearer-token and
 * {@code Origin} filters exist to guard a surface that can write, and which a Prometheus scraper
 * would not satisfy anyway.
 *
 * <h2>Exposure warns rather than refuses</h2>
 *
 * <p>Binding a non-loopback address logs a warning. It does <b>not</b> refuse the way
 * {@link McpHttpPolicy} refuses an unauthenticated MCP port: this endpoint is read-only and carries
 * no data from the database. It does publish the schema's object names and the shape of the
 * traffic, which is worth a line in the log and worth a network policy, but it is not the same
 * risk as an open call surface.
 *
 * <h2>A failure here must not take the server down</h2>
 *
 * <p>An unusable port is reported and the MCP server carries on. Metrics are an observability
 * sideline, and refusing to serve tools because nothing could be told about them would be the
 * wrong trade — unlike the fail-closed security guards, where refusing IS the protection. A port
 * that will not parse is different and does throw: that is an operator typo, and silently ignoring
 * it leaves someone believing they are collecting metrics when they are not.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class McpMetricsExporter implements AutoCloseable {

    public static final String PORT_VARIABLE = "MCP_METRICS_PORT";
    public static final String HOST_VARIABLE = "MCP_METRICS_HOST";

    /** Loopback, so a server is not put on the network by the act of measuring it. */
    public static final String DEFAULT_BIND_HOST = "127.0.0.1";

    /** The scrape path. Anything else is a 404. */
    public static final String METRICS_PATH = "/metrics";

    /** Prometheus text exposition format, version 0.0.4. */
    public static final String CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";

    private final HttpServer theServer;

    private McpMetricsExporter(HttpServer theServerValue) {
        this.theServer = theServerValue;
    }

    /**
     * Start the exporter if {@link #PORT_VARIABLE} names a port, otherwise do nothing.
     *
     * @param theMetrics the registry to serve
     * @param theLog     where a bind failure or an exposure warning is reported; may be null
     * @return the running exporter, or null when no port was configured or the port could not be
     *         bound
     * @throws IllegalArgumentException if the port or host setting cannot be understood
     */
    public static McpMetricsExporter startIfConfigured(McpMetrics theMetrics, LogInterface theLog) {
        Integer thePort = port(System.getenv(PORT_VARIABLE));
        if (thePort == null) {
            return null;
        }
        String theHost = bindHost(System.getenv(HOST_VARIABLE));

        if (!McpHttpPolicy.isLoopbackBindHost(theHost)) {
            warn(theLog, "MCP metrics are being exposed on " + theHost + ":" + thePort
                    + " (" + HOST_VARIABLE + "). The endpoint is unauthenticated and publishes this"
                    + " schema's object names and call volumes; restrict it at the network.");
        }

        try {
            return start(theMetrics, theHost, thePort.intValue());
        } catch (IOException e) {
            // Deliberately not fatal - see the class comment. The server still serves tools.
            error(theLog, "Could not start the MCP metrics exporter on " + theHost + ":" + thePort
                    + " (" + e + "). The server is running WITHOUT metrics.");
            return null;
        }
    }

    /**
     * Start an exporter on a given address, bypassing the environment.
     *
     * @param thePort the port, or 0 to let the operating system choose one
     */
    public static McpMetricsExporter start(McpMetrics theMetrics, String theHost, int thePort)
            throws IOException {
        if (theMetrics == null) {
            throw new IllegalArgumentException("A metrics registry is required");
        }
        HttpServer theHttpServer = HttpServer.create(new InetSocketAddress(theHost, thePort), 0);
        theHttpServer.createContext("/", new ScrapeHandler(theMetrics));
        theHttpServer.setExecutor(daemonExecutor());
        theHttpServer.start();
        return new McpMetricsExporter(theHttpServer);
    }

    /** The port actually bound — the point of the exercise when 0 was requested. */
    public int getPort() {
        return theServer.getAddress().getPort();
    }

    @Override
    public void close() {
        theServer.stop(0);
    }

    /**
     * Testable half of the port setting.
     *
     * @return the port, or null when the variable is unset or blank
     * @throws IllegalArgumentException if it is not a usable port number
     */
    static Integer port(String theSetting) {
        if (theSetting == null || theSetting.trim().length() == 0) {
            return null;
        }
        String theValue = theSetting.trim();
        int thePort;
        try {
            thePort = Integer.parseInt(theValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(PORT_VARIABLE
                    + " must be a port number, not '" + theValue + "'");
        }
        if (thePort < 0 || thePort > 65535) {
            throw new IllegalArgumentException(PORT_VARIABLE
                    + " must be between 0 and 65535, not " + thePort);
        }
        return Integer.valueOf(thePort);
    }

    /** Testable half of the bind-host setting. */
    static String bindHost(String theSetting) {
        if (theSetting == null || theSetting.trim().length() == 0) {
            return DEFAULT_BIND_HOST;
        }
        return theSetting.trim();
    }

    private static Executor daemonExecutor() {
        return Executors.newSingleThreadExecutor(new ThreadFactory() {
            public Thread newThread(Runnable theRunnable) {
                Thread theThread = new Thread(theRunnable, "mcp-metrics");
                // Daemon so a scrape in flight never holds up shutdown of the server it measures.
                theThread.setDaemon(true);
                return theThread;
            }
        });
    }

    private static void warn(LogInterface theLog, String theMessage) {
        if (theLog != null) {
            theLog.warning(theMessage);
        }
    }

    private static void error(LogInterface theLog, String theMessage) {
        if (theLog != null) {
            theLog.error(theMessage);
        }
    }

    /** Answers {@link McpMetricsExporter#METRICS_PATH} and nothing else. */
    private static final class ScrapeHandler implements HttpHandler {

        private final McpMetrics theMetrics;

        private ScrapeHandler(McpMetrics theMetricsValue) {
            this.theMetrics = theMetricsValue;
        }

        public void handle(HttpExchange theExchange) throws IOException {
            try {
                if (!METRICS_PATH.equals(theExchange.getRequestURI().getPath())) {
                    respond(theExchange, 404, "text/plain; charset=utf-8",
                            "Not found. Metrics are at " + METRICS_PATH + "\n");
                    return;
                }
                if (!"GET".equalsIgnoreCase(theExchange.getRequestMethod())
                        && !"HEAD".equalsIgnoreCase(theExchange.getRequestMethod())) {
                    theExchange.getResponseHeaders().set("Allow", "GET, HEAD");
                    respond(theExchange, 405, "text/plain; charset=utf-8", "Method not allowed\n");
                    return;
                }
                respond(theExchange, 200, CONTENT_TYPE, theMetrics.scrape());
            } finally {
                theExchange.close();
            }
        }

        private void respond(HttpExchange theExchange, int theStatus, String theContentType,
                             String theBody) throws IOException {
            byte[] theBytes = theBody.getBytes(StandardCharsets.UTF_8);
            theExchange.getResponseHeaders().set("Content-Type", theContentType);
            if ("HEAD".equalsIgnoreCase(theExchange.getRequestMethod())) {
                theExchange.sendResponseHeaders(theStatus, -1);
                return;
            }
            theExchange.sendResponseHeaders(theStatus, theBytes.length);
            OutputStream theOutput = theExchange.getResponseBody();
            theOutput.write(theBytes);
            theOutput.flush();
        }
    }
}
