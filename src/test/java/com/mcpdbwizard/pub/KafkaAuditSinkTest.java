package com.mcpdbwizard.pub;

import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the reference Kafka sink, using Kafka's own {@code MockProducer} rather than a broker.
 *
 * <p>The behaviour that matters is not that records reach Kafka — that is the producer's job — but
 * that a failing sink <b>never throws into the caller</b>. {@code record} runs in a {@code finally} on
 * the tool-call path, so an exception escaping it would replace the caller's real result, or its real
 * error, with a failure of the audit system. A broken audit trail must not break the database call it
 * was auditing.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class KafkaAuditSinkTest {

    private static McpAuditEvent event() {
        return McpAuditEvent.of("get_customer", null, "ok", 12, null,
                McpAuditSinks.Level.NAMES, 8192);
    }

    @Test
    void aRecordIsPublishedKeyedByToolName() {
        MockProducer<String, String> theProducer =
                new MockProducer<String, String>(true, new StringSerializer(), new StringSerializer());
        KafkaAuditSink theSink = new KafkaAuditSink(theProducer, "audit");

        theSink.record(event());

        assertEquals(1, theProducer.history().size());
        assertEquals("audit", theProducer.history().get(0).topic());
        assertEquals("get_customer", theProducer.history().get(0).key(),
                "keyed by tool so a partition preserves per-tool ordering");
        assertTrue(theProducer.history().get(0).value().contains("\"outcome\":\"ok\""));
    }

    @Test
    void aFailingProducerIsCountedRatherThanThrown() {
        // The whole point: an audit failure must not become the caller's failure.
        MockProducer<String, String> theProducer =
                new MockProducer<String, String>(false, new StringSerializer(), new StringSerializer());
        KafkaAuditSink theSink = new KafkaAuditSink(theProducer, "audit");

        theSink.record(event());
        theProducer.errorNext(new RuntimeException("broker gone"));

        assertEquals(1, theSink.getDroppedCount(), "a lost record must be counted, not silently ignored");
    }

    @Test
    void aClosedProducerDoesNotThrowIntoTheCaller() {
        MockProducer<String, String> theProducer =
                new MockProducer<String, String>(true, new StringSerializer(), new StringSerializer());
        KafkaAuditSink theSink = new KafkaAuditSink(theProducer, "audit");
        theProducer.close();

        theSink.record(event());

        assertEquals(1, theSink.getDroppedCount());
    }

    @Test
    void aNullEventIsIgnored() {
        MockProducer<String, String> theProducer =
                new MockProducer<String, String>(true, new StringSerializer(), new StringSerializer());
        new KafkaAuditSink(theProducer, "audit").record(null);

        assertEquals(0, theProducer.history().size());
    }

    @Test
    void theOverflowPolicyIsExpressedAsMaxBlockMs() {
        // "drop" is simply a send() that will not wait for buffer space.
        Properties theBlocking = KafkaAuditSink.producerProperties("h:9092", "block", "7000");
        assertEquals("7000", theBlocking.get("max.block.ms"));

        Properties theDropping = KafkaAuditSink.producerProperties("h:9092", "drop", "7000");
        assertEquals("1", theDropping.get("max.block.ms"));

        Properties theDefault = KafkaAuditSink.producerProperties("h:9092", null, null);
        assertEquals(KafkaAuditSink.DEFAULT_BLOCK_MS, theDefault.get("max.block.ms"),
                "blocking is the default: silently dropping audit records is the worse surprise");
    }

    @Test
    void everyAcknowledgedRecordIsOnEveryInSyncReplica() {
        Properties theProperties = KafkaAuditSink.producerProperties("h:9092", null, null);

        assertEquals("all", theProperties.get("acks"));
        assertEquals("true", theProperties.get("enable.idempotence"));
    }

    @Test
    void aMissingBootstrapStopsStartUp() {
        // Rather than starting a server that believes it is auditing and is not.
        assertThrows(IllegalArgumentException.class,
                () -> KafkaAuditSink.producerProperties(null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> KafkaAuditSink.producerProperties("  ", null, null));
    }

    @Test
    void theTopicDefaultsButCanBeSet() {
        assertEquals(KafkaAuditSink.DEFAULT_TOPIC, KafkaAuditSink.topicFrom(null));
        assertEquals(KafkaAuditSink.DEFAULT_TOPIC, KafkaAuditSink.topicFrom("   "));
        assertEquals("my-audit", KafkaAuditSink.topicFrom(" my-audit "));
    }

    // ---- operator-supplied producer properties -----------------------------------------------

    private static java.util.Map<String, String> settings(String... thePairs) {
        java.util.Map<String, String> theMap = new java.util.LinkedHashMap<String, String>();
        for (int i = 0; i < thePairs.length; i += 2) {
            theMap.put(thePairs[i], thePairs[i + 1]);
        }
        return theMap;
    }

    /** The whole point of the passthrough: TLS with no code change. */
    @Test
    void anEnvironmentVariableBecomesAProducerProperty() {
        Properties theProperties = KafkaAuditSink.producerProperties("h:9092", null, null,
                settings("MCP_AUDIT_KAFKA_PROP_SECURITY_PROTOCOL", "SSL",
                         "MCP_AUDIT_KAFKA_PROP_SSL_TRUSTSTORE_LOCATION", "/certs/kafka.p12"));
        assertEquals("SSL", theProperties.get("security.protocol"));
        assertEquals("/certs/kafka.p12", theProperties.get("ssl.truststore.location"));
    }

    /**
     * The system-property spelling exists so the web application can turn TLS on from its own UI:
     * a process cannot change its own environment, and the sink is built inside that process.
     */
    @Test
    void aSystemPropertySpellingWorksToo() {
        Properties theProperties = KafkaAuditSink.producerProperties("h:9092", null, null,
                settings("mcp.audit.kafka.prop.security.protocol", "SSL"));
        assertEquals("SSL", theProperties.get("security.protocol"));
    }

    @Test
    void unrelatedSettingsAreIgnored() {
        Properties theProperties = KafkaAuditSink.producerProperties("h:9092", null, null,
                settings("PATH", "/usr/bin", "MCP_AUDIT_KAFKA_TOPIC", "t", "HOME", "/root"));
        assertNull(theProperties.get("topic"));
        assertFalse(theProperties.containsKey("path"));
        // The six we set ourselves are still there.
        assertEquals("all", theProperties.get("acks"));
    }

    /** Applied last, so an operator can override us — including the two that matter. */
    @Test
    void anOperatorCanOverrideOurOwnSettings() {
        Properties theProperties = KafkaAuditSink.producerProperties("h:9092", null, null,
                settings("MCP_AUDIT_KAFKA_PROP_ACKS", "1"));
        assertEquals("1", theProperties.get("acks"));
    }

    /**
     * ...but never silently. acks and enable.idempotence are what make an acknowledged record
     * durable; downgrading either is invisible everywhere else in the system.
     */
    @Test
    void overridingOneOfOursIsReportable() {
        String theDescription = KafkaAuditSink.describeOverrides(
                settings("MCP_AUDIT_KAFKA_PROP_ACKS", "1"));
        assertNotNull(theDescription);
        assertTrue(theDescription.contains("acks=1"), theDescription);

        assertNull(KafkaAuditSink.describeOverrides(
                settings("MCP_AUDIT_KAFKA_PROP_SECURITY_PROTOCOL", "SSL")),
                "a setting that is not one of ours is not an override");
    }

    @Test
    void nullAndEmptySettingsAreHarmless() {
        assertTrue(KafkaAuditSink.passthrough(null).isEmpty());
        assertTrue(KafkaAuditSink.passthrough(settings()).isEmpty());
        assertNull(KafkaAuditSink.describeOverrides(null));
        // A prefix with nothing after it names no property.
        assertTrue(KafkaAuditSink.passthrough(settings("MCP_AUDIT_KAFKA_PROP_", "x")).isEmpty());
    }

    // ---- flush() and the spool's durability guarantee -----------------------------------------

    /**
     * THE REGRESSION, and it destroyed real records before it was found.
     *
     * <p>{@code send()} can fail SYNCHRONOUSLY — a producer that cannot fetch metadata throws inside
     * {@code max.block.ms}, and {@code record()} counts the drop there and then, before any flush
     * begins. The old {@code flush()} sampled the drop counter at its own start, so that drop was
     * invisible to it: before and after matched, it returned true, and {@link SpoolingAuditSink}
     * deleted a segment holding a record the broker never received.
     *
     * <p>Observed against a live broker with no such topic: the sink logged "Audit record not
     * delivered", the topic's end offset stayed 0, and the segment was gone. A write-ahead spool
     * that deletes undelivered records is worse than no spool, because it is trusted.
     */
    @Test
    void aDropDuringRecordMakesTheNextFlushReportFailure() {
        MockProducer<String, String> theProducer =
                new MockProducer<String, String>(false, new StringSerializer(), new StringSerializer());
        KafkaAuditSink theSink = new KafkaAuditSink(theProducer, "t");

        theSink.record(McpAuditEvent.of("tool", null, McpCallRecord.OUTCOME_OK, 1L, null,
                McpAuditSinks.Level.NAMES, 0));
        // The send fails after record() returned, exactly as a real callback failure does.
        theProducer.errorNext(new RuntimeException("broker said no"));

        assertFalse(theSink.flush(),
                "flush must report failure so the spool KEEPS the segment");
    }

    /** ...and the spool must not then be stuck for ever: a clean window reports success again. */
    @Test
    void aLaterFlushWithNoNewDropsReportsSuccess() {
        MockProducer<String, String> theProducer =
                new MockProducer<String, String>(false, new StringSerializer(), new StringSerializer());
        KafkaAuditSink theSink = new KafkaAuditSink(theProducer, "t");

        theSink.record(McpAuditEvent.of("tool", null, McpCallRecord.OUTCOME_OK, 1L, null,
                McpAuditSinks.Level.NAMES, 0));
        theProducer.errorNext(new RuntimeException("broker said no"));
        assertFalse(theSink.flush());

        // The retry succeeds.
        theSink.record(McpAuditEvent.of("tool", null, McpCallRecord.OUTCOME_OK, 1L, null,
                McpAuditSinks.Level.NAMES, 0));
        theProducer.completeNext();
        assertTrue(theSink.flush(),
                "a window with no new drops is deliverable, or the spool could never drain");
    }

    /** A sink that has never dropped anything reports success, which is the ordinary case. */
    @Test
    void aCleanSinkFlushesTrue() {
        MockProducer<String, String> theProducer =
                new MockProducer<String, String>(true, new StringSerializer(), new StringSerializer());
        KafkaAuditSink theSink = new KafkaAuditSink(theProducer, "t");
        theSink.record(McpAuditEvent.of("tool", null, McpCallRecord.OUTCOME_OK, 1L, null,
                McpAuditSinks.Level.NAMES, 0));
        assertTrue(theSink.flush());
        assertTrue(theSink.flush(), "flushing twice does not invent a failure");
    }
}
