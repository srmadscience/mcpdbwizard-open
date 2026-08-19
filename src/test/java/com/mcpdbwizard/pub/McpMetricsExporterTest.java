package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Database-free tests for the metrics endpoint.
 *
 * <p>Each one binds an ephemeral port on loopback, so they need no fixture and cannot collide with
 * anything — including each other.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class McpMetricsExporterTest {

    // ---- the port setting -----------------------------------------------

    @Test
    void noPortMeansNoListener() {
        // The default. A server generated with PROMETHEUS_SERVER=YES still binds nothing until an
        // operator names a port, which is what lets twenty of them run on one host.
        assertNull(McpMetricsExporter.port(null));
        assertNull(McpMetricsExporter.port(""));
        assertNull(McpMetricsExporter.port("   "));
    }

    @Test
    void aPortIsReadAndTrimmed() {
        assertEquals(Integer.valueOf(9464), McpMetricsExporter.port("9464"));
        assertEquals(Integer.valueOf(9464), McpMetricsExporter.port("  9464 "));
        assertEquals(Integer.valueOf(0), McpMetricsExporter.port("0"));
    }

    @Test
    void aMistypedPortStopsStartUpRatherThanBeingIgnored() {
        // Silently ignoring it leaves an operator believing they are collecting metrics when they
        // are not - the same reasoning as MCP_AUDIT_LEVEL and MCP_RATE_LIMIT.
        assertThrows(IllegalArgumentException.class, () -> McpMetricsExporter.port("nine"));
        assertThrows(IllegalArgumentException.class, () -> McpMetricsExporter.port("9464x"));
        assertThrows(IllegalArgumentException.class, () -> McpMetricsExporter.port("-1"));
        assertThrows(IllegalArgumentException.class, () -> McpMetricsExporter.port("70000"));
    }

    @Test
    void theBindHostDefaultsToLoopback() {
        // Measuring a server must not be what puts it on the network.
        assertEquals("127.0.0.1", McpMetricsExporter.bindHost(null));
        assertEquals("127.0.0.1", McpMetricsExporter.bindHost(""));
        assertEquals("0.0.0.0", McpMetricsExporter.bindHost(" 0.0.0.0 "));
    }

    // ---- serving --------------------------------------------------------

    @Test
    void aScrapeReturnsTheRegistryInPrometheusFormat() throws Exception {
        McpMetrics theMetrics = McpMetrics.forServer("DaoFactoryMcpServer");
        theMetrics.describe("ob_gen_pkg_greet", "APPSCHEMA.OB_GEN_PKG.GREET", "procedure");
        theMetrics.record("ob_gen_pkg_greet", McpCallRecord.OUTCOME_OK, 12, 40, 900);

        try (McpMetricsExporter theExporter =
                     McpMetricsExporter.start(theMetrics, "127.0.0.1", 0)) {
            Response theResponse = get(theExporter.getPort(), "/metrics");
            assertEquals(200, theResponse.status);
            assertEquals(McpMetricsExporter.CONTENT_TYPE, theResponse.contentType);
            assertTrue(theResponse.body.contains("db_object=\"APPSCHEMA.OB_GEN_PKG.GREET\""),
                    theResponse.body);
        }
    }

    @Test
    void aScrapeIsLiveRatherThanASnapshotTakenAtStartUp() {
        McpMetrics theMetrics = McpMetrics.forServer("DaoFactoryMcpServer");
        try (McpMetricsExporter theExporter =
                     McpMetricsExporter.start(theMetrics, "127.0.0.1", 0)) {
            assertTrue(!get(theExporter.getPort(), "/metrics").body.contains("tool=\"late\""));
            theMetrics.record("late", McpCallRecord.OUTCOME_OK, 1, 0, 0);
            assertTrue(get(theExporter.getPort(), "/metrics").body.contains("tool=\"late\""));
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void anythingButTheMetricsPathIsNotFound() throws Exception {
        // The endpoint exists to be scraped and nothing else. It sits outside the /mcp filters, so
        // it must not become an accidental second surface on the process.
        try (McpMetricsExporter theExporter =
                     McpMetricsExporter.start(McpMetrics.forServer("s"), "127.0.0.1", 0)) {
            assertEquals(404, get(theExporter.getPort(), "/").status);
            assertEquals(404, get(theExporter.getPort(), "/mcp").status);
            assertEquals(404, get(theExporter.getPort(), "/metrics/../secret").status);
        }
    }

    @Test
    void aWriteMethodIsRefused() throws Exception {
        try (McpMetricsExporter theExporter =
                     McpMetricsExporter.start(McpMetrics.forServer("s"), "127.0.0.1", 0)) {
            Response theResponse = request(theExporter.getPort(), "/metrics", "POST");
            assertEquals(405, theResponse.status);
        }
    }

    @Test
    void closingItReleasesThePort() throws Exception {
        McpMetricsExporter theExporter =
                McpMetricsExporter.start(McpMetrics.forServer("s"), "127.0.0.1", 0);
        int thePort = theExporter.getPort();
        theExporter.close();

        // Re-binding the same port is the check: a leaked listener would refuse it.
        try (McpMetricsExporter theSecond =
                     McpMetricsExporter.start(McpMetrics.forServer("s"), "127.0.0.1", thePort)) {
            assertEquals(thePort, theSecond.getPort());
        }
    }

    // ---- tiny HTTP client -----------------------------------------------

    private record Response(int status, String contentType, String body) {
    }

    private static Response get(int thePort, String thePath) {
        try {
            return request(thePort, thePath, "GET");
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static Response request(int thePort, String thePath, String theMethod) throws IOException {
        HttpURLConnection theConnection =
                (HttpURLConnection) new URL("http://127.0.0.1:" + thePort + thePath).openConnection();
        theConnection.setRequestMethod(theMethod);
        if ("POST".equals(theMethod)) {
            theConnection.setDoOutput(true);
            theConnection.getOutputStream().write(new byte[] {1});
        }
        int theStatus = theConnection.getResponseCode();
        InputStream theStream = theStatus < 400
                ? theConnection.getInputStream()
                : theConnection.getErrorStream();
        String theBody = theStream == null
                ? ""
                : new String(theStream.readAllBytes(), StandardCharsets.UTF_8);
        return new Response(theStatus, theConnection.getHeaderField("Content-Type"), theBody);
    }
}
