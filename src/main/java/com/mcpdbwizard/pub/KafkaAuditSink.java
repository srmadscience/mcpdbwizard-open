package com.mcpdbwizard.pub;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Reference {@link McpAuditSink} that publishes audit records to a Kafka topic.
 *
 * <p>Select it with {@code MCP_AUDIT_SINK=com.mcpdbwizard.pub.KafkaAuditSink}.
 *
 * <table>
 *   <caption>Environment</caption>
 *   <tr><td>{@code MCP_AUDIT_KAFKA_BOOTSTRAP}</td><td>Required. Bootstrap servers.</td></tr>
 *   <tr><td>{@code MCP_AUDIT_KAFKA_TOPIC}</td><td>Topic, default {@value #DEFAULT_TOPIC}.</td></tr>
 *   <tr><td>{@code MCP_AUDIT_KAFKA_ON_FULL}</td><td>{@code block} (default) or {@code drop}.</td></tr>
 *   <tr><td>{@code MCP_AUDIT_KAFKA_BLOCK_MS}</td><td>How long {@code block} waits, default
 *       {@value #DEFAULT_BLOCK_MS}.</td></tr>
 * </table>
 *
 * <h2>What this does and does not guarantee — read before relying on it</h2>
 *
 * <p><b>This is a reference implementation, not a durable audit pipeline.</b> Buffering is the Kafka
 * producer's in-memory buffer. If the broker is unreachable for longer than that buffer and the
 * configured block time absorb, records are <b>dropped</b> — counted and reported, but gone. Nothing
 * here spools to disk and replays, and a disk spool is the substantial work that a genuinely durable
 * trail needs (see {@code docs/mcp-audit-sink-plan.md} §2.3).
 *
 * <p>The two policies express the honest trade:
 *
 * <ul>
 *   <li>{@code block} — a tool call waits up to {@code MCP_AUDIT_KAFKA_BLOCK_MS} for buffer space,
 *       so a broker outage slows the database work. Closer to audit semantics, at the cost of
 *       coupling Oracle calls to Kafka's availability.</li>
 *   <li>{@code drop} — the call never waits, and the record is lost on a full buffer. Correct when
 *       the trail is valuable but not load-bearing; wrong when somebody will later rely on it being
 *       complete.</li>
 * </ul>
 *
 * <p>{@code acks=all} is set, so a record acknowledged by the broker is on every in-sync replica.
 * That covers durability <em>after</em> hand-off; it says nothing about records that never got there.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class KafkaAuditSink implements McpAuditSink {

    public static final String BOOTSTRAP_VARIABLE = "MCP_AUDIT_KAFKA_BOOTSTRAP";
    public static final String TOPIC_VARIABLE = "MCP_AUDIT_KAFKA_TOPIC";
    public static final String ON_FULL_VARIABLE = "MCP_AUDIT_KAFKA_ON_FULL";
    public static final String BLOCK_MS_VARIABLE = "MCP_AUDIT_KAFKA_BLOCK_MS";

    public static final String DEFAULT_TOPIC = "mcp-audit";
    public static final String DEFAULT_BLOCK_MS = "5000";

    private final Producer<String, String> theProducer;
    private final String theTopic;
    private final LogInterface theLog = new JulLog("KafkaAuditSink");
    private final AtomicLong theDropped = new AtomicLong();

    /** Built reflectively by {@link McpAuditSinks#fromEnvironment()}. */
    public KafkaAuditSink() {
        this(buildProducer(), topicFrom(McpAuditSinks.setting(TOPIC_VARIABLE)));
    }

    /** For tests: inject a producer rather than reach a broker. */
    KafkaAuditSink(Producer<String, String> theProducerValue, String theTopicValue) {
        this.theProducer = theProducerValue;
        this.theTopic = theTopicValue;
    }

    /**
     * The configured topic, or {@value #DEFAULT_TOPIC} when the setting is absent or blank.
     *
     * <p>Public so a status page can show the topic records will actually go to, rather than
     * restating this default somewhere else and drifting from it.
     */
    public static String topicFrom(String theSetting) {
        return theSetting == null || theSetting.trim().length() == 0
                ? DEFAULT_TOPIC : theSetting.trim();
    }

    /** Producer settings, exposed for testing so the policy mapping can be asserted without a broker. */
    static Properties producerProperties(String theBootstrap, String theOnFull, String theBlockMillis) {
        if (theBootstrap == null || theBootstrap.trim().length() == 0) {
            throw new IllegalArgumentException(BOOTSTRAP_VARIABLE + " must be set to use "
                    + KafkaAuditSink.class.getName());
        }

        Properties theProperties = new Properties();
        theProperties.put("bootstrap.servers", theBootstrap.trim());
        theProperties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        theProperties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        // A record the broker acknowledges is on every in-sync replica. Anything less would make the
        // trail's durability depend on which broker happened to take the write.
        theProperties.put("acks", "all");
        theProperties.put("enable.idempotence", "true");

        // max.block.ms is where the policy lives: it is how long send() waits for buffer space before
        // giving up. "drop" is simply a very short wait.
        boolean theDropFlag = "drop".equalsIgnoreCase(theOnFull == null ? "" : theOnFull.trim());
        String theBlock = theBlockMillis == null || theBlockMillis.trim().length() == 0
                ? DEFAULT_BLOCK_MS : theBlockMillis.trim();
        theProperties.put("max.block.ms", theDropFlag ? "1" : theBlock);

        return theProperties;
    }

    /**
     * Producer settings with operator-supplied properties applied on top.
     *
     * <p>This is what makes TLS need no code: {@code MCP_AUDIT_KAFKA_PROP_SECURITY_PROTOCOL=SSL}
     * plus the truststore properties, and the producer is encrypted in transit. It is deliberately a
     * general passthrough rather than one variable per option, because the alternative is a code
     * change every time somebody needs a Kafka setting we did not anticipate — compression, SASL
     * mechanism, request timeouts.
     *
     * <p><b>Applied AFTER the six properties this class sets</b>, so an operator can override them.
     * That includes {@code acks} and {@code enable.idempotence}, which are the two that quietly
     * weaken the trail's durability — hence {@link #describeOverrides}, so a start-up log line names
     * anything of ours that was replaced. A silently downgraded {@code acks=all} is exactly the
     * change nobody would notice.
     *
     * <p>Names convert by stripping the prefix, lower-casing, and turning underscores into dots:
     * {@code MCP_AUDIT_KAFKA_PROP_SSL_TRUSTSTORE_LOCATION} → {@code ssl.truststore.location}.
     *
     * @param theSettings environment and/or system properties to scan; see {@link #passthrough}
     */
    static Properties producerProperties(String theBootstrap, String theOnFull, String theBlockMillis,
                                         Map<String, String> theSettings) {
        Properties theProperties = producerProperties(theBootstrap, theOnFull, theBlockMillis);
        theProperties.putAll(passthrough(theSettings));
        return theProperties;
    }

    /**
     * The operator-supplied producer properties found in {@code theSettings}.
     *
     * <p>Keys are matched case-insensitively on the {@value #PROPERTY_PREFIX} prefix so the same
     * setting can arrive as an environment variable ({@code MCP_AUDIT_KAFKA_PROP_SECURITY_PROTOCOL})
     * or as a system property ({@code mcp.audit.kafka.prop.security.protocol}) — the second form is
     * what lets the web application turn TLS on from its own UI, since a process cannot change its
     * own environment. This mirrors the {@code DAO_POOL_*} / {@code -Ddao.pool.*} pairing the
     * generated pool already uses.
     */
    static Properties passthrough(Map<String, String> theSettings) {
        Properties theExtra = new Properties();
        if (theSettings == null) {
            return theExtra;
        }
        for (Map.Entry<String, String> theEntry : theSettings.entrySet()) {
            String theKey = theEntry.getKey();
            if (theKey == null || theEntry.getValue() == null) {
                continue;
            }
            String theNormalised = theKey.replace('.', '_').toUpperCase();
            if (!theNormalised.startsWith(PROPERTY_PREFIX)) {
                continue;
            }
            String theName = theNormalised.substring(PROPERTY_PREFIX.length())
                    .toLowerCase().replace('_', '.');
            if (theName.length() > 0) {
                theExtra.put(theName, theEntry.getValue().trim());
            }
        }
        return theExtra;
    }

    /**
     * Which of this class's own settings the operator replaced, for a start-up log line, or null
     * when none were.
     */
    static String describeOverrides(Map<String, String> theSettings) {
        Properties theExtra = passthrough(theSettings);
        StringBuilder theNames = new StringBuilder();
        for (String theOurs : OUR_PROPERTIES) {
            if (theExtra.containsKey(theOurs)) {
                if (theNames.length() > 0) {
                    theNames.append(", ");
                }
                theNames.append(theOurs).append('=').append(theExtra.get(theOurs));
            }
        }
        return theNames.length() == 0 ? null : theNames.toString();
    }

    /** Everything {@link #producerProperties} sets itself, so an override of one can be reported. */
    private static final String[] OUR_PROPERTIES = {
        "bootstrap.servers", "key.serializer", "value.serializer",
        "acks", "enable.idempotence", "max.block.ms"
    };

    /**
     * Both spellings normalise to this: {@code MCP_AUDIT_KAFKA_PROP_} as an environment variable, and
     * {@code mcp.audit.kafka.prop.} as a system property.
     */
    static final String PROPERTY_PREFIX = "MCP_AUDIT_KAFKA_PROP_";

    /** Environment variables and system properties together, the latter winning on a clash. */
    static Map<String, String> settings() {
        Map<String, String> theSettings = new java.util.LinkedHashMap<String, String>(System.getenv());
        for (String theName : System.getProperties().stringPropertyNames()) {
            theSettings.put(theName, System.getProperty(theName));
        }
        return theSettings;
    }

    private static Producer<String, String> buildProducer() {
        Map<String, String> theSettings = settings();
        String theOverridden = describeOverrides(theSettings);
        if (theOverridden != null) {
            // Loud on purpose: acks and enable.idempotence are what make an acknowledged record
            // durable, and replacing them from the outside is invisible everywhere else.
            new JulLog("KafkaAuditSink").warning(
                    "Audit producer settings overridden by " + PROPERTY_PREFIX + "* : " + theOverridden);
        }
        return new KafkaProducer<String, String>(producerProperties(
                McpAuditSinks.setting(BOOTSTRAP_VARIABLE),
                McpAuditSinks.setting(ON_FULL_VARIABLE),
                McpAuditSinks.setting(BLOCK_MS_VARIABLE),
                theSettings));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Never throws: this runs in a {@code finally} on the tool-call path, and an audit failure
     * must not replace the caller's real result.
     */
    public void record(McpAuditEvent theEvent) {
        if (theEvent == null) {
            return;
        }
        try {
            // Keyed by tool so a topic partition preserves per-tool ordering; across tools, ordering
            // is Kafka's per-partition guarantee and nothing stronger.
            theProducer.send(new ProducerRecord<String, String>(
                    theTopic, theEvent.getToolName(), theEvent.toJson()), (theMetadata, theError) -> {
                        if (theError != null) {
                            countDrop(theError.toString());
                        }
                    });
        } catch (Exception e) {
            // Buffer full past max.block.ms, serialization failure, producer already closed.
            countDrop(e.toString());
        }
    }

    private void countDrop(String theReason) {
        long theTotal = theDropped.incrementAndGet();
        // Every drop is a hole in the trail, but logging each one during an outage would itself be a
        // flood. Report the first, then powers of ten.
        if (theTotal == 1L || theTotal % 1000L == 0L) {
            theLog.error("Audit record not delivered to Kafka (" + theTotal + " so far): " + theReason);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Blocks until the producer has completed every outstanding send, then reports whether any of
     * them failed. A spool depends on this answer being honest: saying "delivered" about a batch that
     * did not arrive would let it delete records that were never received.
     */
    @Override
    public synchronized boolean flush() {
        try {
            theProducer.flush();
        } catch (Exception e) {
            countDrop(e.toString());
            // Falls through: the comparison below reports it, and an exception here is not the only
            // way this flush can have lost something.
        }

        // Compared against the count at the END OF THE PREVIOUS FLUSH, not the start of this one.
        //
        // THIS IS THE WHOLE CORRECTNESS OF THE SPOOL, and getting it wrong silently destroyed
        // records. record() can drop synchronously -- a send() that cannot fetch metadata throws
        // inside max.block.ms and is counted there and then, before any flush begins. Sampling the
        // counter at the top of flush() therefore could not see it: before and after matched, flush
        // returned true, and SpoolingAuditSink deleted a segment holding a record the broker had
        // never received. Observed for real against a broker with no such topic: the sink logged
        // "Audit record not delivered", the topic's end offset stayed 0, and the segment was gone.
        long theNow = theDropped.get();
        boolean theCleanFlag = theNow == theDroppedAtLastFlush;
        theDroppedAtLastFlush = theNow;
        return theCleanFlag;
    }

    /**
     * The drop count as at the end of the previous {@link #flush()}.
     *
     * <p>Guarded by {@code flush()} being synchronized. Not an {@code AtomicLong} because it is only
     * ever read and written there, and making it atomic would suggest it can be sampled
     * independently — which is exactly the mistake this field exists to correct.
     */
    private long theDroppedAtLastFlush;

    /** How many records have been lost. Non-zero means the trail has holes. */
    public long getDroppedCount() {
        return theDropped.get();
    }

    public void close() {
        try {
            theProducer.close();
        } catch (Exception e) {
            theLog.warning("Kafka audit producer did not close cleanly: " + e);
        }
        if (theDropped.get() > 0L) {
            theLog.error("Audit trail is incomplete: " + theDropped.get()
                    + " record(s) were never delivered to Kafka.");
        }
    }
}
