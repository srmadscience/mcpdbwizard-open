package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How the local trail and a remote sink are composed.
 *
 * <p>The case that makes this class necessary is
 * {@link #aTrailAndAStreamTogetherProduceBoth()}: {@code MCP_AUDIT_SINK} names one class, and until
 * the local trail existed that was the whole answer. A licensed installation keeps records here for
 * its window <em>and</em> sends them to Kafka, which is two sinks and not a choice between them.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class McpAuditSinksCompositionTest {

    /** A stand-in for a remote sink, so no broker is needed. */
    public static final class DummyStreamSink implements McpAuditSink {
        public void record(McpAuditEvent theEvent) {
        }

        public void close() {
        }
    }

    @Test
    void aTrailAloneIsJustTheTrail(@TempDir Path dir) {
        withSettings(settings(dir.toString(), "7", null), () -> {
            McpAuditSink theSink = McpAuditSinks.fromEnvironment(null);
            assertInstanceOf(FileAuditSink.class, theSink);
            assertTrue(McpAuditSinks.isConfigured());
            assertTrue(McpAuditSinks.isLocalTrailConfigured());
            assertFalse(McpAuditSinks.isStreamConfigured());
        });
    }

    @Test
    void aStreamAloneIsJustTheStream(@TempDir Path dir) {
        withSettings(settings(null, null, DummyStreamSink.class.getName()), () -> {
            McpAuditSink theSink = McpAuditSinks.fromEnvironment(null);
            assertInstanceOf(DummyStreamSink.class, theSink);
            assertTrue(McpAuditSinks.isConfigured());
            assertFalse(McpAuditSinks.isLocalTrailConfigured());
        });
    }

    @Test
    void aTrailAndAStreamTogetherProduceBoth(@TempDir Path dir) {
        withSettings(settings(dir.toString(), "30", DummyStreamSink.class.getName()), () -> {
            McpAuditSink theSink = McpAuditSinks.fromEnvironment(null);
            FanOutAuditSink theFanOut = assertInstanceOf(FanOutAuditSink.class, theSink);

            assertEquals(2, theFanOut.sinks().size());
            assertInstanceOf(FileAuditSink.class, theFanOut.sinks().get(0));
            assertInstanceOf(DummyStreamSink.class, theFanOut.sinks().get(1));
        });
    }

    @Test
    void neitherIsTheNoOpAndNothingThrows(@TempDir Path dir) {
        withSettings(settings(null, null, null), () -> {
            McpAuditSink theSink = McpAuditSinks.fromEnvironment(null);
            theSink.record(McpAuditEvent.of("t", null, "ok", 1L, null, McpAuditSinks.Level.NAMES, 0));
            assertFalse(McpAuditSinks.isConfigured());
        });
    }

    @Test
    void aZeroWindowLeavesOnlyTheStream(@TempDir Path dir) {
        // The data-residency case: stream everything, rest nothing on this box.
        withSettings(settings(dir.toString(), "0", DummyStreamSink.class.getName()), () -> {
            assertInstanceOf(DummyStreamSink.class, McpAuditSinks.fromEnvironment(null));
            assertFalse(McpAuditSinks.isLocalTrailConfigured());
            // Still auditing, so the shutdown hook and the status page must still say so.
            assertTrue(McpAuditSinks.isConfigured());
        });
    }

    @Test
    void isConfiguredCoversATrailWithNoStream(@TempDir Path dir) {
        // It used to ask only about MCP_AUDIT_SINK. Left that way, a deployment with only a local
        // trail would never install the shutdown hook that closes it, and its status page would
        // say it was not auditing while it was.
        withSettings(settings(dir.toString(), "1", null), () ->
                assertTrue(McpAuditSinks.isConfigured()));
    }

    @Test
    void namingTheTrailAsTheStreamIsRefusedWithAnExplanation(@TempDir Path dir) {
        withSettings(settings(null, null, FileAuditSink.class.getName()), () -> {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> McpAuditSinks.fromEnvironment(null));
            // The message has to name the variables that DO configure it, or this is just a wall.
            assertTrue(e.getMessage().contains(FileAuditSink.DIRECTORY_VARIABLE), e.getMessage());
            assertTrue(e.getMessage().contains(FileAuditSink.RETENTION_DAYS_VARIABLE), e.getMessage());
        });
    }

    // ---- harness ----

    private static Map<String, String> settings(String theDir, String theDays, String theSink) {
        Map<String, String> theSettings = new HashMap<String, String>();
        theSettings.put(FileAuditSink.DIRECTORY_VARIABLE, theDir);
        theSettings.put(FileAuditSink.RETENTION_DAYS_VARIABLE, theDays);
        theSettings.put(McpAuditSinks.SINK_VARIABLE, theSink);
        return theSettings;
    }

    /**
     * Set the audit settings as system properties and always put them back.
     *
     * <p>System properties rather than the environment because a JVM cannot change its own
     * environment — which is exactly why {@link McpAuditSinks#setting} reads both.
     */
    private static void withSettings(Map<String, String> theSettings, Runnable theBody) {
        Map<String, String> thePrevious = new HashMap<String, String>();
        for (Map.Entry<String, String> theEntry : theSettings.entrySet()) {
            String theKey = McpAuditSinks.propertyNameFor(theEntry.getKey());
            thePrevious.put(theKey, System.getProperty(theKey));
            if (theEntry.getValue() == null) {
                System.clearProperty(theKey);
            } else {
                System.setProperty(theKey, theEntry.getValue());
            }
        }
        try {
            theBody.run();
        } finally {
            for (Map.Entry<String, String> theEntry : thePrevious.entrySet()) {
                if (theEntry.getValue() == null) {
                    System.clearProperty(theEntry.getKey());
                } else {
                    System.setProperty(theEntry.getKey(), theEntry.getValue());
                }
            }
        }
    }
}
