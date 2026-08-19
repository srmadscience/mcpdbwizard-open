package com.mcpdbwizard.pub;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Splunk HEC sink, driven against a real HTTP endpoint.
 *
 * <p>{@link #aRejectedBatchIsReportedAsLostRatherThanConfirmed()} is the property the spool depends
 * on. Splunk answering 403 to a bad token is the commonest way this integration is wrong on the day
 * it is set up, and a sink that reported success would have {@link SpoolingAuditSink} delete every
 * record it never took.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class SplunkAuditSinkTest {

    /** A stand-in collector that records the bodies and can be told to refuse. */
    private static final class Hec implements AutoCloseable {
        final HttpServer server;
        final List<String> bodies = Collections.synchronizedList(new ArrayList<String>());
        final List<String> authorizations = Collections.synchronizedList(new ArrayList<String>());
        final AtomicInteger status = new AtomicInteger(200);

        Hec() throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext(SplunkAuditSink.COLLECTOR_PATH, theExchange -> {
                bodies.add(new String(theExchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                String theAuth = theExchange.getRequestHeaders().getFirst("Authorization");
                authorizations.add(theAuth == null ? "" : theAuth);
                byte[] theReply = "{\"text\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
                theExchange.sendResponseHeaders(status.get(), theReply.length);
                theExchange.getResponseBody().write(theReply);
                theExchange.close();
            });
            server.start();
        }

        URI uri() {
            return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        }

        public void close() {
            server.stop(0);
        }
    }

    private Hec hec;

    @AfterEach
    void stop() {
        if (hec != null) {
            hec.close();
        }
    }

    private SplunkAuditSink sink(int theBatchSize) throws IOException {
        hec = new Hec();
        return new SplunkAuditSink(SplunkAuditSink.endpoint(hec.uri().toString()),
                "the-token", "audit_index", SplunkAuditSink.DEFAULT_SOURCETYPE,
                theBatchSize, 5000, null);
    }

    private static McpAuditEvent event(String theTool) {
        return McpAuditEvent.of(theTool, null, "ok", 3L, null, McpAuditSinks.Level.NAMES, 0);
    }

    @Test
    void recordsArriveAsHecEventsWithTheTokenHeader() throws Exception {
        SplunkAuditSink theSink = sink(1);
        theSink.record(event("get_flights"));

        assertEquals(1, hec.bodies.size());
        String theBody = hec.bodies.get(0);
        assertTrue(theBody.contains("\"sourcetype\":\"mcpdbwizard:audit\""), theBody);
        assertTrue(theBody.contains("\"index\":\"audit_index\""), theBody);
        // The record is an OBJECT, not a string, so Splunk indexes its fields.
        assertTrue(theBody.contains("\"event\":{"), theBody);
        assertTrue(theBody.contains("\"tool\":\"get_flights\""), theBody);
        assertEquals("Splunk the-token", hec.authorizations.get(0));
    }

    @Test
    void aBatchIsOneRequestOfConcatenatedObjects() throws Exception {
        // HEC takes objects back to back, NOT a JSON array -- a comma between them is an error.
        SplunkAuditSink theSink = sink(3);
        theSink.record(event("a"));
        theSink.record(event("b"));
        assertEquals(0, hec.bodies.size(), "the batch must not go early");

        theSink.record(event("c"));

        assertEquals(1, hec.bodies.size(), "the whole batch is one request");
        String theBody = hec.bodies.get(0);
        assertTrue(theBody.contains("}{"), "expected concatenated objects: " + theBody);
        assertFalse(theBody.contains("},{"), "an array separator would be rejected: " + theBody);
    }

    @Test
    void flushSendsWhatIsStillPending() throws Exception {
        SplunkAuditSink theSink = sink(100);
        theSink.record(event("waiting"));
        assertEquals(1L, theSink.getPendingCount());

        assertTrue(theSink.flush());

        assertEquals(1, hec.bodies.size());
        assertEquals(0L, theSink.getPendingCount());
        assertEquals(1L, theSink.getDeliveredCount());
    }

    @Test
    void aRejectedBatchIsReportedAsLostRatherThanConfirmed() throws Exception {
        SplunkAuditSink theSink = sink(1);
        hec.status.set(403);

        theSink.record(event("refused"));

        assertEquals(1L, theSink.getDroppedCount());
        assertEquals(0L, theSink.getDeliveredCount());
        assertFalse(theSink.flush(), "a spool must not be told this batch arrived");
    }

    @Test
    void aFailureBeforeFlushIsStillReportedByIt() throws Exception {
        // The rule KafkaAuditSink documents: a full batch posts inside record(), so a flush that
        // sampled its counter at the top would miss a rejection that had already happened.
        SplunkAuditSink theSink = sink(1);
        hec.status.set(500);
        theSink.record(event("lost"));

        assertFalse(theSink.flush(), "the earlier rejection must surface here");
        // ...and a clean stretch afterwards is clean, or every later flush stays poisoned.
        hec.status.set(200);
        theSink.record(event("fine"));
        assertTrue(theSink.flush());
    }

    @Test
    void closeSendsTheLastPartialBatch() throws Exception {
        SplunkAuditSink theSink = sink(100);
        theSink.record(event("last"));
        theSink.close();

        assertEquals(1, hec.bodies.size(), "a shutdown must not discard a part-full batch");
    }

    // ---- settings ----

    @Test
    void theCollectorPathIsAppendedUnlessItIsAlreadyThere() {
        assertEquals("https://splunk:8088/services/collector/event",
                SplunkAuditSink.endpoint("https://splunk:8088").toString());
        assertEquals("https://splunk:8088/services/collector/event",
                SplunkAuditSink.endpoint("https://splunk:8088/").toString());
        assertEquals("https://splunk:8088/services/collector/event",
                SplunkAuditSink.endpoint("https://splunk:8088/services/collector/event").toString());
    }

    @Test
    void aTokenFileIsPreferredToATokenInTheEnvironment() throws Exception {
        // Nobody sets both on purpose, so preferring the safer of the two is the right way to
        // resolve the accident.
        Path theFile = Files.createTempFile("hec", ".token");
        Files.writeString(theFile, "  from-the-file\n");
        String theFileKey = McpAuditSinks.propertyNameFor(SplunkAuditSink.TOKEN_FILE_VARIABLE);
        String theInlineKey = McpAuditSinks.propertyNameFor(SplunkAuditSink.TOKEN_VARIABLE);
        try {
            System.setProperty(theFileKey, theFile.toString());
            System.setProperty(theInlineKey, "from-the-environment");
            assertEquals("from-the-file", SplunkAuditSink.token());
        } finally {
            System.clearProperty(theFileKey);
            System.clearProperty(theInlineKey);
            Files.deleteIfExists(theFile);
        }
    }

    @Test
    void noTokenAtAllStopsStartUpWithBothSpellingsNamed() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                SplunkAuditSink::token);
        assertTrue(e.getMessage().contains(SplunkAuditSink.TOKEN_FILE_VARIABLE), e.getMessage());
        assertTrue(e.getMessage().contains(SplunkAuditSink.TOKEN_VARIABLE), e.getMessage());
    }
}
