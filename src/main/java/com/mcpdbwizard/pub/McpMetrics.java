package com.mcpdbwizard.pub;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-tool call metrics for a generated MCP server, in Prometheus exposition format.
 *
 * <h2>Why the generator has to tell it which database object a tool touches</h2>
 *
 * <p>At run time the server knows only the tool NAME — a lower-cased, punctuation-stripped form of
 * an Oracle name, with an overload number appended where one was needed. Going back from that to
 * {@code APPSCHEMA.FIXTURE_PKG.GREET} is not possible, and one Oracle object routinely yields several
 * tools (a table yields at least four). The generator is the only place that holds the mapping, so
 * it bakes it in through {@link #describe} at start-up and every series carries the object as a
 * label. That is what makes {@code sum by (db_object)} work in a query.
 *
 * <h2>What is measured, and over what window</h2>
 *
 * <p>Counts and byte totals are cumulative since start-up, which is what a Prometheus counter must
 * be. The <b>quantiles are over the last {@value #WINDOW} calls to that tool</b>, computed exactly
 * from a ring of retained samples rather than estimated — the same windowed spirit as a Prometheus
 * summary, and the reason they are useful at all: a p90 over all of history stops moving after a
 * day and stops answering "is it slow now?".
 *
 * <p><b>The maximum is deliberately NOT windowed.</b> A windowed max silently discards the worst
 * call the server ever served, which is the one an operator is looking for.
 *
 * <h2>Cost</h2>
 *
 * <p>{@link #record} takes one uncontended lock per tool and writes a handful of longs. It sits
 * behind a database round trip, so it is noise. Nothing here is emitted unless the config sets
 * {@code PROMETHEUS_SERVER=YES}, so a server that does not want metrics does not pay for the
 * argument serialisation the byte counters need either.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class McpMetrics {

    /** How many recent latencies are retained per tool for the quantile calculation. */
    public static final int WINDOW = 2048;

    /** Used for a call whose tool name was null or blank, so a bad request is still counted. */
    public static final String UNKNOWN_TOOL = "unknown";

    /** {@link #describe}d object type for a tool nothing described. */
    public static final String UNKNOWN_OBJECT_TYPE = "unknown";

    private static final String PREFIX = "mcpdbwizard_mcp_";

    private final String theServerName;

    /**
     * Where this server is running, on every series.
     *
     * <p>Needed because {@code server} alone does not identify a process: it is the generated
     * server's CLASS name, and every config that keeps the default factory name produces
     * {@code DaoFactoryMcpServer}. Several servers on one host, or the same config on several hosts,
     * therefore collapse into one set of series without this.
     *
     * <p>Resolved ONCE, at construction. A reverse lookup per scrape would put a name service on the
     * path of a monitoring endpoint, which is a poor place for it.
     */
    private final String theHostName;

    /**
     * Which CONFIG this server was generated from.
     *
     * <p>The label that finally makes a series identify one process. {@code server} is the
     * generated class name and {@code host} is the machine; twenty servers launched from twenty
     * configs on one host share both, so without this their series merge and the merge is silent.
     */
    private final String theConfigName;

    /**
     * Keyed by tool name. A {@link ConcurrentHashMap} because {@link #describe} runs on the main
     * thread at start-up while {@link #record} runs on request threads.
     */
    private final ConcurrentHashMap<String, ToolStats> theTools =
            new ConcurrentHashMap<String, ToolStats>();

    /** The pool whose gauges are reported alongside the call metrics, or null when unpooled. */
    private volatile DaoFactoryPool<?> thePool;

    /** The audit sink, so a scrape can say whether the trail is complete. Null when unaudited. */
    private volatile McpAuditSink theAuditSink;

    private McpMetrics(String theServerNameValue, String theConfigNameValue) {
        this.theServerName = theServerNameValue == null ? "" : theServerNameValue;
        this.theHostName = hostName();
        this.theConfigName = configName(theConfigNameValue);
    }

    /**
     * Create the registry for one generated server.
     *
     * @param theServerNameValue the server class name, reported as the {@code server} label so a
     *                           scrape of several servers on one host can tell them apart
     */
    public static McpMetrics forServer(String theServerNameValue) {
        return new McpMetrics(theServerNameValue, "");
    }

    /**
     * As {@link #forServer(String)}, naming the config this server was generated from.
     *
     * @param theConfigNameValue the generation-time identity, typically the emitted package name.
     *                           {@code MCP_METRICS_CONFIG_LABEL} overrides it at run time, which is
     *                           how the web runtime substitutes the config's own name.
     */
    public static McpMetrics forServer(String theServerNameValue, String theConfigNameValue) {
        return new McpMetrics(theServerNameValue, theConfigNameValue);
    }

    /**
     * Record which database object a tool reaches. Called once per tool at start-up, from a static
     * initialiser the generator writes.
     *
     * <p>Describing a tool does not make it appear in a scrape: a tool that has never been called
     * has no series, which is correct — Prometheus counters should not be born at zero for
     * thousands of tools that may never be used.
     *
     * @param theToolNameValue   the MCP tool name, as it appears in {@code tools/list}
     * @param theDbObjectValue   the fully-qualified Oracle object, e.g. {@code APPSCHEMA.FIXTURE_PKG.GREET}
     * @param theObjectTypeValue {@code table}, {@code view}, {@code procedure}, {@code sequence},
     *                           {@code statement} — what kind of thing the object is
     */
    public void describe(String theToolNameValue, String theDbObjectValue, String theObjectTypeValue) {
        ToolStats theStats = statsFor(theToolNameValue);
        theStats.dbObject = theDbObjectValue == null ? "" : theDbObjectValue;
        theStats.objectType = theObjectTypeValue == null || theObjectTypeValue.length() == 0
                ? UNKNOWN_OBJECT_TYPE
                : theObjectTypeValue;
    }

    /**
     * Report the connection pool, so a scrape carries its counters too.
     *
     * <p>These are the numbers the {@code POOL-STATS} log line has always held. They were only ever
     * recoverable by tailing a log file and parsing it, which is a poor channel for something a
     * monitoring system wants every fifteen seconds.
     *
     * @param thePoolValue the pool, or null for an unpooled server
     */
    public void bindPool(DaoFactoryPool<?> thePoolValue) {
        this.thePool = thePoolValue;
    }

    /**
     * Report the audit sink, so a scrape carries the one number that says whether the trail can be
     * relied on: how many records have been LOST.
     *
     * <p>This is the metric worth alerting on. Everything else here describes how the server is
     * performing; a non-zero drop count says the record of what it did is incomplete, and nothing
     * else in the system will mention it — the sink counts and logs, and a log line is not something
     * anyone watches for.
     *
     * @param theSinkValue the sink in use, or null when the server is unaudited
     */
    public void bindAuditSink(McpAuditSink theSinkValue) {
        this.theAuditSink = theSinkValue;
    }

    /**
     * Record one completed tool call. Called from the generated {@code call(...)} funnel's
     * {@code finally}, so failures are measured as well as successes.
     *
     * @param theToolNameValue  the tool that ran; null or blank is counted as {@link #UNKNOWN_TOOL}
     * @param theOutcomeValue   one of the {@link McpCallRecord} {@code OUTCOME_} constants
     * @param theDurationMicros elapsed duration of the call in MICROSECONDS, and it must come from
     *                          {@code System.nanoTime()} rather than a wall clock — at a few
     *                          milliseconds a call, {@code currentTimeMillis} cannot resolve the
     *                          measurement and an NTP step can make it run backwards
     * @param theRequestBytes   size of the call's arguments as JSON, or 0 when there were none
     * @param theResponseBytes  size of the response payload, or 0 when the call produced none
     */
    public void record(String theToolNameValue, String theOutcomeValue, long theDurationMicros,
                       long theRequestBytes, long theResponseBytes) {
        ToolStats theStats = statsFor(theToolNameValue);
        String theOutcome = theOutcomeValue == null || theOutcomeValue.length() == 0
                ? McpCallRecord.OUTCOME_ERROR
                : theOutcomeValue;

        // Everything under one lock so a scrape can never see a count that disagrees with the sum
        // it is divided by. Contention is per tool and each call has just made a database round
        // trip, so this costs nothing measurable.
        synchronized (theStats) {
            Long thePrevious = theStats.callsByOutcome.get(theOutcome);
            theStats.callsByOutcome.put(theOutcome,
                    Long.valueOf(thePrevious == null ? 1L : thePrevious.longValue() + 1L));

            theStats.requestBytes += theRequestBytes < 0 ? 0 : theRequestBytes;
            theStats.responseBytes += theResponseBytes < 0 ? 0 : theResponseBytes;

            long theDuration = theDurationMicros < 0 ? 0 : theDurationMicros;
            theStats.durationCount++;
            theStats.durationSumMicros += theDuration;
            if (theDuration > theStats.maxMicros) {
                theStats.maxMicros = theDuration;
            }
            theStats.window[theStats.windowNext] = theDuration;
            theStats.windowNext = (theStats.windowNext + 1) % WINDOW;
            if (theStats.windowFill < WINDOW) {
                theStats.windowFill++;
            }
        }
    }

    /**
     * The whole registry in Prometheus text exposition format (version 0.0.4).
     *
     * <p>Deterministically ordered — tools by name, outcomes by name — so a diff of two scrapes is
     * readable and the tests can assert on whole blocks rather than fishing for lines.
     */
    public String scrape() {
        StringBuilder theText = new StringBuilder(4096);

        List<String> theToolNames = new ArrayList<String>(theTools.keySet());
        Collections.sort(theToolNames);

        // A snapshot per tool, taken under the tool's own lock, so the text below is built outside
        // every lock and a slow StringBuilder never blocks a call.
        List<Snapshot> theSnapshots = new ArrayList<Snapshot>(theToolNames.size());
        for (int seq = 0; seq < theToolNames.size(); seq++) {
            ToolStats theStats = theTools.get(theToolNames.get(seq));
            if (theStats != null) {
                Snapshot theSnapshot = theStats.snapshot();
                if (theSnapshot != null) {
                    theSnapshots.add(theSnapshot);
                }
            }
        }

        appendCalls(theText, theSnapshots);
        appendDuration(theText, theSnapshots);
        appendBytes(theText, theSnapshots);
        appendPool(theText);
        appendAudit(theText);

        return theText.toString();
    }

    private void appendCalls(StringBuilder theText, List<Snapshot> theSnapshots) {
        theText.append("# HELP ").append(PREFIX).append("calls_total")
                .append(" MCP tool calls completed, by outcome.\n");
        theText.append("# TYPE ").append(PREFIX).append("calls_total counter\n");
        for (int seq = 0; seq < theSnapshots.size(); seq++) {
            Snapshot theSnapshot = theSnapshots.get(seq);
            for (Map.Entry<String, Long> theEntry : theSnapshot.callsByOutcome.entrySet()) {
                theText.append(PREFIX).append("calls_total")
                        .append(labels(theSnapshot, "outcome", theEntry.getKey()))
                        .append(' ').append(theEntry.getValue().longValue()).append('\n');
            }
        }
    }

    private void appendDuration(StringBuilder theText, List<Snapshot> theSnapshots) {
        // No outcome label here, deliberately: splitting the quantiles across seven outcomes would
        // leave each one computed from a handful of samples and make p90 meaningless.
        theText.append("# HELP ").append(PREFIX).append("call_duration_seconds")
                .append(" MCP tool call latency. Quantiles are over the last ")
                .append(WINDOW).append(" calls to the tool.\n");
        theText.append("# TYPE ").append(PREFIX).append("call_duration_seconds summary\n");
        for (int seq = 0; seq < theSnapshots.size(); seq++) {
            Snapshot theSnapshot = theSnapshots.get(seq);
            appendQuantile(theText, theSnapshot, "0.5");
            appendQuantile(theText, theSnapshot, "0.75");
            appendQuantile(theText, theSnapshot, "0.9");
            theText.append(PREFIX).append("call_duration_seconds_sum").append(labels(theSnapshot))
                    .append(' ').append(seconds(theSnapshot.durationSumMicros)).append('\n');
            theText.append(PREFIX).append("call_duration_seconds_count").append(labels(theSnapshot))
                    .append(' ').append(theSnapshot.durationCount).append('\n');
        }

        // The maximum is a separate family because it is not part of the summary's window: it is the
        // worst call since start-up, which is the one worth keeping.
        theText.append("# HELP ").append(PREFIX).append("call_duration_seconds_max")
                .append(" Longest MCP tool call since start-up.\n");
        theText.append("# TYPE ").append(PREFIX).append("call_duration_seconds_max gauge\n");
        for (int seq = 0; seq < theSnapshots.size(); seq++) {
            Snapshot theSnapshot = theSnapshots.get(seq);
            theText.append(PREFIX).append("call_duration_seconds_max").append(labels(theSnapshot))
                    .append(' ').append(seconds(theSnapshot.maxMicros)).append('\n');
        }
    }

    private void appendQuantile(StringBuilder theText, Snapshot theSnapshot, String theQuantile) {
        theText.append(PREFIX).append("call_duration_seconds")
                .append(labels(theSnapshot, "quantile", theQuantile))
                .append(' ')
                .append(seconds(quantile(theSnapshot.window, Double.parseDouble(theQuantile))))
                .append('\n');
    }

    private void appendBytes(StringBuilder theText, List<Snapshot> theSnapshots) {
        theText.append("# HELP ").append(PREFIX).append("request_bytes_total")
                .append(" Bytes of JSON arguments received, by tool.\n");
        theText.append("# TYPE ").append(PREFIX).append("request_bytes_total counter\n");
        for (int seq = 0; seq < theSnapshots.size(); seq++) {
            Snapshot theSnapshot = theSnapshots.get(seq);
            theText.append(PREFIX).append("request_bytes_total").append(labels(theSnapshot))
                    .append(' ').append(theSnapshot.requestBytes).append('\n');
        }
        theText.append("# HELP ").append(PREFIX).append("response_bytes_total")
                .append(" Bytes of JSON payload returned, by tool.\n");
        theText.append("# TYPE ").append(PREFIX).append("response_bytes_total counter\n");
        for (int seq = 0; seq < theSnapshots.size(); seq++) {
            Snapshot theSnapshot = theSnapshots.get(seq);
            theText.append(PREFIX).append("response_bytes_total").append(labels(theSnapshot))
                    .append(' ').append(theSnapshot.responseBytes).append('\n');
        }
    }

    private void appendPool(StringBuilder theText) {
        DaoFactoryPool<?> thePoolNow = thePool;
        if (thePoolNow == null) {
            return;
        }
        String theServerLabel = "{host=\"" + escape(theHostName)
                + "\",config=\"" + escape(theConfigName)
                + "\",server=\"" + escape(theServerName) + "\"}";
        appendPoolGauge(theText, "pool_active", "gauge",
                "DAO factories currently borrowed.", theServerLabel, thePoolNow.getNumActive());
        appendPoolGauge(theText, "pool_idle", "gauge",
                "DAO factories pooled and available.", theServerLabel, thePoolNow.getNumIdle());
        appendPoolGauge(theText, "pool_max", "gauge",
                "Ceiling on pooled DAO factories.", theServerLabel, thePoolNow.getMaxSize());
        appendPoolGauge(theText, "pool_borrowed_total", "counter",
                "DAO factory borrows since start-up.", theServerLabel, thePoolNow.getBorrowedCount());
        appendPoolGauge(theText, "pool_created_total", "counter",
                "DAO factories created since start-up.", theServerLabel, thePoolNow.getCreatedCount());
        appendPoolGauge(theText, "pool_destroyed_total", "counter",
                "DAO factories destroyed since start-up.", theServerLabel, thePoolNow.getDestroyedCount());
        // Rising while pool_created_total is flat means the database is refusing logons. That is a
        // different fault from churn (both rising together) and neither is visible without this.
        appendPoolGauge(theText, "pool_create_failed_total", "counter",
                "DAO factory creations that failed since start-up.", theServerLabel,
                thePoolNow.getCreateFailedCount());
    }

    /**
     * The audit sink's counters.
     *
     * <p>A sink that does not count reports -1 through the SPI, and <b>-1 must not be exported</b>:
     * a gauge reading -1 would be graphed, alerted on and averaged as if it were a measurement.
     * Omitting the series instead makes "this sink cannot tell you" visible as an ABSENT metric,
     * which a monitoring system already knows how to express and a wrong number does not.
     */
    private void appendAudit(StringBuilder theText) {
        McpAuditSink theSinkNow = theAuditSink;
        if (theSinkNow == null) {
            return;
        }
        String theServerLabel = "{host=\"" + escape(theHostName)
                + "\",config=\"" + escape(theConfigName)
                + "\",server=\"" + escape(theServerName) + "\"}";
        long theDropped = theSinkNow.getDroppedCount();
        long theDelivered = theSinkNow.getDeliveredCount();
        long thePending = theSinkNow.getPendingCount();

        if (theDropped >= 0L) {
            appendPoolGauge(theText, "audit_dropped_total", "counter",
                    "Audit records KNOWN LOST since start-up. Non-zero means the trail is incomplete.",
                    theServerLabel, theDropped);
        }
        if (theDelivered >= 0L) {
            appendPoolGauge(theText, "audit_delivered_total", "counter",
                    "Audit records confirmed delivered since start-up.", theServerLabel, theDelivered);
        }
        if (thePending >= 0L) {
            appendPoolGauge(theText, "audit_pending", "gauge",
                    "Audit records written down but not yet delivered. Sustained non-zero means the"
                            + " destination is unreachable.", theServerLabel, thePending);
        }
    }

    private void appendPoolGauge(StringBuilder theText, String theName, String theType,
                                 String theHelp, String theLabels, long theValue) {
        theText.append("# HELP ").append(PREFIX).append(theName).append(' ').append(theHelp).append('\n');
        theText.append("# TYPE ").append(PREFIX).append(theName).append(' ').append(theType).append('\n');
        theText.append(PREFIX).append(theName).append(theLabels).append(' ').append(theValue).append('\n');
    }

    private String labels(Snapshot theSnapshot) {
        return labels(theSnapshot, null, null);
    }

    /**
     * The value for the {@code host} label.
     *
     * <p>{@code MCP_METRICS_HOST_LABEL} wins when it is set, and a deployment should usually set it.
     * The fallback is the JVM's idea of the local host name, which <b>inside a container is the
     * container id</b> — unique, but it changes on every recreate, so a graph keyed on it breaks
     * each time the container is replaced. An address or a logical name is far more useful, and
     * only the operator knows which.
     *
     * <p>Deliberately NOT called {@code instance}: Prometheus sets that label itself from the scrape
     * target and renames any the application supplies to {@code exported_instance}, so using that
     * name would produce a confusingly duplicated pair.
     */
    static String hostName() {
        String theConfigured = System.getenv(HOST_LABEL_VARIABLE);
        if (theConfigured != null && theConfigured.trim().length() > 0) {
            return theConfigured.trim();
        }
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            // A host with no resolvable name still has to serve metrics; an empty label would be
            // worse than a placeholder, because it reads as "this series has no host".
            return "unknown";
        }
    }

    /** Overrides the detected host name on every series. */
    public static final String HOST_LABEL_VARIABLE = "MCP_METRICS_HOST_LABEL";

    /** Overrides the baked-in config name on every series. */
    public static final String CONFIG_LABEL_VARIABLE = "MCP_METRICS_CONFIG_LABEL";

    /**
     * The value for the {@code config} label: the environment first, then whatever the generator
     * baked in, then a placeholder.
     *
     * <p>The environment wins because the web runtime knows the config by the name an operator
     * chose, while the generator only knows the package it emitted into. Never blank — an empty
     * label reads as "this series belongs to no config", which is a different claim from "nobody
     * said which".
     */
    static String configName(String theBakedIn) {
        String theConfigured = System.getenv(CONFIG_LABEL_VARIABLE);
        if (theConfigured != null && theConfigured.trim().length() > 0) {
            return theConfigured.trim();
        }
        if (theBakedIn != null && theBakedIn.trim().length() > 0) {
            return theBakedIn.trim();
        }
        return "unknown";
    }

    private String labels(Snapshot theSnapshot, String theExtraName, String theExtraValue) {
        StringBuilder theLabels = new StringBuilder(96);
        theLabels.append("{host=\"").append(escape(theHostName));
        theLabels.append("\",config=\"").append(escape(theConfigName));
        theLabels.append("\",server=\"").append(escape(theServerName));
        theLabels.append("\",tool=\"").append(escape(theSnapshot.toolName));
        theLabels.append("\",db_object=\"").append(escape(theSnapshot.dbObject));
        theLabels.append("\",object_type=\"").append(escape(theSnapshot.objectType)).append('"');
        if (theExtraName != null) {
            theLabels.append(',').append(theExtraName).append("=\"")
                    .append(escape(theExtraValue)).append('"');
        }
        return theLabels.append('}').toString();
    }

    /**
     * The φ-quantile of a sample set, by nearest rank: the smallest sample at or above the
     * {@code ceil(q * n)}th position.
     *
     * <p>Exact rather than estimated. The samples are already in memory and there are at most
     * {@value #WINDOW} of them, so the approximation a streaming algorithm buys is not worth its
     * error — or the reader having to know it is there.
     *
     * @param theSamples the retained latencies in milliseconds; sorted in place
     * @return the quantile in milliseconds, or 0 when there are no samples
     */
    static long quantile(long[] theSamples, double theQuantile) {
        if (theSamples == null || theSamples.length == 0) {
            return 0L;
        }
        Arrays.sort(theSamples);
        int thePosition = (int) Math.ceil(theQuantile * theSamples.length) - 1;
        if (thePosition < 0) {
            thePosition = 0;
        }
        if (thePosition >= theSamples.length) {
            thePosition = theSamples.length - 1;
        }
        return theSamples[thePosition];
    }

    /** Microseconds in a second — the divisor the exposition converts by. */
    private static final long MICROS_PER_SECOND = 1000000L;

    /**
     * Microseconds as seconds, Prometheus's base unit for a duration.
     *
     * <p><b>The unit is microseconds because milliseconds stopped being enough.</b> Calls were 70 ms
     * when this was written and are now ~3 ms, so a 1 ms quantum went from 1% of a call to a third
     * of one: quantiles snapped to whole milliseconds, and anything under half a millisecond
     * recorded as a zero. The wire format is unchanged — seconds, as Prometheus requires — but it
     * now carries six decimal places instead of three.
     *
     * <p>Built by hand rather than through {@code String.format}, which would honour the default
     * locale and write {@code 0,009} anywhere the decimal separator is a comma — silently
     * unparseable to a scraper.
     */
    static String seconds(long theMicros) {
        StringBuilder theValue = new StringBuilder(16);
        if (theMicros < 0) {
            theValue.append('-');
            theMicros = -theMicros;
        }
        theValue.append(theMicros / MICROS_PER_SECOND).append('.');
        String theFraction = Long.toString(theMicros % MICROS_PER_SECOND);
        // Left-pad to six digits: 1234 micros is .001234 of a second, not .1234.
        for (int i = theFraction.length(); i < 6; i++) {
            theValue.append('0');
        }
        return theValue.append(theFraction).toString();
    }

    /** Prometheus label-value escaping: backslash, double quote and newline. */
    static String escape(String theText) {
        if (theText == null) {
            return "";
        }
        StringBuilder theResult = new StringBuilder(theText.length());
        for (int seq = 0; seq < theText.length(); seq++) {
            char theCharacter = theText.charAt(seq);
            switch (theCharacter) {
                case '\\': theResult.append("\\\\"); break;
                case '"':  theResult.append("\\\""); break;
                case '\n': theResult.append("\\n");  break;
                default:   theResult.append(theCharacter); break;
            }
        }
        return theResult.toString();
    }

    private ToolStats statsFor(String theToolNameValue) {
        String theKey = theToolNameValue == null || theToolNameValue.trim().length() == 0
                ? UNKNOWN_TOOL
                : theToolNameValue.trim();
        ToolStats theExisting = theTools.get(theKey);
        if (theExisting != null) {
            return theExisting;
        }
        ToolStats theCreated = new ToolStats(theKey);
        ToolStats theRaced = theTools.putIfAbsent(theKey, theCreated);
        return theRaced == null ? theCreated : theRaced;
    }

    /** Live state for one tool. Every field is read and written under the instance's own monitor. */
    private static final class ToolStats {

        private final String toolName;
        private String dbObject = "";
        private String objectType = UNKNOWN_OBJECT_TYPE;
        private final Map<String, Long> callsByOutcome = new TreeMap<String, Long>();
        private long requestBytes;
        private long responseBytes;
        private long durationCount;
        private long durationSumMicros;
        private long maxMicros;
        private final long[] window = new long[WINDOW];
        private int windowNext;
        private int windowFill;

        private ToolStats(String theToolNameValue) {
            this.toolName = theToolNameValue;
        }

        /** A consistent copy, or null when this tool has been described but never called. */
        private Snapshot snapshot() {
            synchronized (this) {
                if (durationCount == 0) {
                    return null;
                }
                Snapshot theSnapshot = new Snapshot();
                theSnapshot.toolName = toolName;
                theSnapshot.dbObject = dbObject;
                theSnapshot.objectType = objectType;
                theSnapshot.callsByOutcome = new LinkedHashMap<String, Long>(callsByOutcome);
                theSnapshot.requestBytes = requestBytes;
                theSnapshot.responseBytes = responseBytes;
                theSnapshot.durationCount = durationCount;
                theSnapshot.durationSumMicros = durationSumMicros;
                theSnapshot.maxMicros = maxMicros;
                theSnapshot.window = Arrays.copyOf(window, windowFill);
                return theSnapshot;
            }
        }
    }

    /** An immutable-by-convention copy of one tool's state, built under lock and read outside it. */
    private static final class Snapshot {
        private String toolName;
        private String dbObject;
        private String objectType;
        private Map<String, Long> callsByOutcome;
        private long requestBytes;
        private long responseBytes;
        private long durationCount;
        private long durationSumMicros;
        private long maxMicros;
        private long[] window;
    }
}
