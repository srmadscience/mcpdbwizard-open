package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The syslog sink, driven against a real socket.
 *
 * <p>Against a real collector rather than a mocked writer, because the two things that break a
 * syslog integration are both on the wire: the framing, and the field order in the header. Neither
 * is visible if the test asserts on a string the sink handed to a stub.
 *
 * <p>{@link #aRecordSurvivesTheCollectorRestarting()} is the one worth the class. A collector
 * restart drops the connection, and the write after it is the one that finds out — without a
 * reconnect that is a hole in the trail every time someone patches their SIEM.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class SyslogAuditSinkTest {

    /** A syslog collector that speaks RFC 6587 octet counting and remembers what it was sent. */
    private static class Collector implements AutoCloseable {
        final ServerSocket server;
        final List<String> received = Collections.synchronizedList(new ArrayList<String>());
        final CountDownLatch latch;
        volatile boolean running = true;
        // Tracked so close() can drop it. Closing the ServerSocket alone leaves an ALREADY
        // ACCEPTED connection open, so the sink's peer has not actually gone away and the restart
        // this test is about never happens -- which is exactly how it first passed for the wrong
        // reason and then failed for the right one.
        volatile Socket accepted;

        Collector(int theExpected) throws Exception {
            this(new ServerSocket(0), theExpected);
        }

        /** On an already-bound socket, so the restart case can reclaim the same port. */
        Collector(ServerSocket theSocket, int theExpected) {
            this.server = theSocket;
            this.latch = new CountDownLatch(theExpected);
            Thread theThread = new Thread(this::accept, "test-syslog");
            theThread.setDaemon(true);
            theThread.start();
        }

        int port() {
            return server.getLocalPort();
        }

        private void accept() {
            while (running) {
                try (Socket theSocket = server.accept();
                     InputStream theStream = theSocket.getInputStream()) {
                    accepted = theSocket;
                    readFrames(theStream);
                } catch (Exception e) {
                    return;   // closed, which is how this thread ends
                }
            }
        }

        private void readFrames(InputStream theStream) throws Exception {
            DataInputStream theData = new DataInputStream(theStream);
            while (running) {
                StringBuilder theLength = new StringBuilder();
                int theByte;
                while ((theByte = theData.read()) != -1 && theByte != ' ') {
                    theLength.append((char) theByte);
                }
                if (theByte == -1) {
                    return;
                }
                byte[] theMessage = new byte[Integer.parseInt(theLength.toString())];
                theData.readFully(theMessage);
                received.add(new String(theMessage, StandardCharsets.UTF_8));
                latch.countDown();
            }
        }

        public void close() throws Exception {
            running = false;
            if (accepted != null) {
                accepted.close();
            }
            server.close();
        }
    }

    private static McpAuditEvent event(String theTool) {
        return McpAuditEvent.of(theTool, null, "ok", 7L, null, McpAuditSinks.Level.NAMES, 0);
    }

    private static SyslogAuditSink tcpSink(int thePort) {
        return new SyslogAuditSink("127.0.0.1", thePort, false,
                SyslogAuditSink.DEFAULT_FACILITY, "mcpdbwizard");
    }

    @Test
    void aRecordArrivesAsOneRfc5424Message() throws Exception {
        try (Collector theCollector = new Collector(1)) {
            SyslogAuditSink theSink = tcpSink(theCollector.port());
            theSink.record(event("get_flights"));
            assertTrue(theCollector.latch.await(5, TimeUnit.SECONDS), "nothing arrived");
            theSink.close();

            String theMessage = theCollector.received.get(0);
            // <facility*8+severity>VERSION, and 13*8+6 is the audit facility at informational.
            assertTrue(theMessage.startsWith("<110>1 "), theMessage);
            assertTrue(theMessage.contains("mcpdbwizard"), theMessage);
            // The record itself is the MSG, so a collector that parses JSON gets the fields.
            assertTrue(theMessage.contains("\"tool\":\"get_flights\""), theMessage);
        }
    }

    @Test
    void framingIsOctetCountedSoARecordCannotSplitInTwo() throws Exception {
        // The reason for length prefixes rather than newlines: a record's JSON can carry an escaped
        // newline, and with newline framing that one record becomes two malformed ones.
        try (Collector theCollector = new Collector(2)) {
            SyslogAuditSink theSink = tcpSink(theCollector.port());
            theSink.record(event("first\ntool"));
            theSink.record(event("second"));
            assertTrue(theCollector.latch.await(5, TimeUnit.SECONDS), "expected two whole messages");
            theSink.close();

            assertEquals(2, theCollector.received.size());
            assertTrue(theCollector.received.get(1).contains("second"));
        }
    }

    @Test
    void aRecordSurvivesTheCollectorRestarting() throws Exception {
        int thePort;
        SyslogAuditSink theSink;
        try (Collector theFirst = new Collector(1)) {
            thePort = theFirst.port();
            theSink = tcpSink(thePort);
            theSink.record(event("before"));
            assertTrue(theFirst.latch.await(5, TimeUnit.SECONDS));
        }
        // The collector has gone. The next write finds a broken pipe and must reconnect.
        try (ServerSocket theSecond = new ServerSocket(thePort)) {
            Collector theRestarted = new Collector(theSecond, 1);
            theSink.record(event("after"));
            assertTrue(theRestarted.latch.await(5, TimeUnit.SECONDS),
                    "the record after a restart was lost");
            assertTrue(theRestarted.received.get(0).contains("after"));
            theSink.close();
            theRestarted.close();
        }
    }

    @Test
    void flushReportsAFailureThatHappenedBeforeItWasCalled() throws Exception {
        // The rule KafkaAuditSink documents at length: record() fails synchronously here, so a
        // flush that sampled its counter at the top would see nothing and a spool would delete a
        // segment whose records never left the machine.
        SyslogAuditSink theSink = new SyslogAuditSink("127.0.0.1", unusedPort(), false,
                SyslogAuditSink.DEFAULT_FACILITY, "mcpdbwizard");

        theSink.record(event("goes_nowhere"));

        assertTrue(theSink.getDroppedCount() > 0L, "a refused connection must count");
        assertFalse(theSink.flush(), "flush must not confirm a batch that failed");
        // And a clean stretch afterwards reports clean, or every later flush would be poisoned.
        assertTrue(theSink.flush());
        theSink.close();
    }

    @Test
    void aBadFacilityStopsStartUpRatherThanBeingGuessed() {
        assertThrows(IllegalArgumentException.class, () -> SyslogAuditSink.facility("99"));
        assertThrows(IllegalArgumentException.class, () -> SyslogAuditSink.facility("audit"));
        assertEquals(SyslogAuditSink.DEFAULT_FACILITY, SyslogAuditSink.facility(null));
    }

    @Test
    void aSpaceInTheAppNameCannotShiftEveryFieldAfterIt() {
        // RFC 5424 is space-delimited: an APP-NAME with a space in it silently moves PROCID into
        // MSGID and so on, and the collector reads the wrong things into the wrong places.
        assertEquals("my_app", SyslogAuditSink.appName("my app"));
        assertEquals(SyslogAuditSink.DEFAULT_APP_NAME, SyslogAuditSink.appName("  "));
    }

    private static int unusedPort() throws Exception {
        try (ServerSocket theSocket = new ServerSocket(0)) {
            return theSocket.getLocalPort();
        }
    }
}
