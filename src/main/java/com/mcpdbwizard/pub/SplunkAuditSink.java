package com.mcpdbwizard.pub;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sends audit records to Splunk's HTTP Event Collector.
 *
 * <p>Select it with {@code MCP_AUDIT_SINK=com.mcpdbwizard.pub.SplunkAuditSink}. Like the syslog
 * sink it needs no library — HEC is an HTTP POST, and the JDK has had a client since 11.
 *
 * <table>
 *   <caption>Environment</caption>
 *   <tr><td>{@code MCP_AUDIT_SPLUNK_URL}</td><td>Required. The collector's base URL, e.g.
 *       {@code https://splunk.example.com:8088}. A URL already ending in
 *       {@code /services/collector/event} is taken as given.</td></tr>
 *   <tr><td>{@code MCP_AUDIT_SPLUNK_TOKEN_FILE}</td><td>File holding the HEC token. <b>Preferred.</b></td></tr>
 *   <tr><td>{@code MCP_AUDIT_SPLUNK_TOKEN}</td><td>The token itself, if a file is impossible.</td></tr>
 *   <tr><td>{@code MCP_AUDIT_SPLUNK_INDEX}</td><td>Target index. Splunk's default if unset.</td></tr>
 *   <tr><td>{@code MCP_AUDIT_SPLUNK_SOURCETYPE}</td><td>Default {@value #DEFAULT_SOURCETYPE}.</td></tr>
 *   <tr><td>{@code MCP_AUDIT_SPLUNK_BATCH_SIZE}</td><td>Records per POST, default
 *       {@value #DEFAULT_BATCH_SIZE}.</td></tr>
 *   <tr><td>{@code MCP_AUDIT_SPLUNK_TIMEOUT_MS}</td><td>Per-request timeout, default
 *       {@value #DEFAULT_TIMEOUT_MS}.</td></tr>
 * </table>
 *
 * <h2>The token comes from a file by preference</h2>
 *
 * <p>Both spellings work, and the file is the one to use. A token in an environment variable is
 * readable by anything that can see the process, appears in {@code docker inspect} and in whatever
 * orchestrator holds the task definition, and tends to end up in a repository. The file form takes a
 * mounted secret, and the token is read once at start-up rather than held anywhere it can be
 * printed.
 *
 * <h2>Batching, and what {@link #flush()} therefore guarantees</h2>
 *
 * <p>Records accumulate and are POSTed when the batch fills or when {@code flush()} is called — HEC
 * accepts several events in one request, and a POST per tool call would make the audit trail the
 * slowest thing in the system.
 *
 * <p>{@code flush()} sends whatever is pending, waits for the response, and reports honestly.
 * <b>A batch Splunk rejects counts as lost</b>, and the records are not retried here: retrying is
 * what {@link SpoolingAuditSink} is for, and it can only do it if this tells the truth. Saying
 * "delivered" about a rejected batch would let the spool delete records Splunk never took.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class SplunkAuditSink implements McpAuditSink {

    public static final String URL_VARIABLE = "MCP_AUDIT_SPLUNK_URL";
    public static final String TOKEN_VARIABLE = "MCP_AUDIT_SPLUNK_TOKEN";
    public static final String TOKEN_FILE_VARIABLE = "MCP_AUDIT_SPLUNK_TOKEN_FILE";
    public static final String INDEX_VARIABLE = "MCP_AUDIT_SPLUNK_INDEX";
    public static final String SOURCETYPE_VARIABLE = "MCP_AUDIT_SPLUNK_SOURCETYPE";
    public static final String BATCH_SIZE_VARIABLE = "MCP_AUDIT_SPLUNK_BATCH_SIZE";
    public static final String TIMEOUT_MS_VARIABLE = "MCP_AUDIT_SPLUNK_TIMEOUT_MS";

    public static final String DEFAULT_SOURCETYPE = "mcpdbwizard:audit";
    public static final int DEFAULT_BATCH_SIZE = 50;
    public static final int DEFAULT_TIMEOUT_MS = 10000;

    static final String COLLECTOR_PATH = "/services/collector/event";

    private final URI theEndpoint;
    private final String theToken;
    private final String theIndex;
    private final String theSourceType;
    private final int theBatchSize;
    private final HttpClient theClient;
    private final Duration theTimeout;
    private final LogInterface theLog = new JulLog("SplunkAuditSink");

    private final List<String> thePending = new ArrayList<String>();
    private final AtomicLong theDropped = new AtomicLong();
    private final AtomicLong theDelivered = new AtomicLong();
    private long theDroppedAtLastFlush;

    /** Built reflectively by {@link McpAuditSinks#fromEnvironment()}. */
    public SplunkAuditSink() {
        this(endpoint(required(URL_VARIABLE)),
                token(),
                trimToNull(McpAuditSinks.setting(INDEX_VARIABLE)),
                sourceType(McpAuditSinks.setting(SOURCETYPE_VARIABLE)),
                number(BATCH_SIZE_VARIABLE, McpAuditSinks.setting(BATCH_SIZE_VARIABLE), DEFAULT_BATCH_SIZE),
                number(TIMEOUT_MS_VARIABLE, McpAuditSinks.setting(TIMEOUT_MS_VARIABLE), DEFAULT_TIMEOUT_MS),
                null);
    }

    /**
     * @param theClientValue an HTTP client, or null to build one — the seam a test uses so no
     *                       Splunk is needed
     */
    public SplunkAuditSink(URI theEndpointValue, String theTokenValue, String theIndexValue,
                           String theSourceTypeValue, int theBatchSizeValue, int theTimeoutMillis,
                           HttpClient theClientValue) {
        this.theEndpoint = theEndpointValue;
        this.theToken = theTokenValue;
        this.theIndex = theIndexValue;
        this.theSourceType = theSourceTypeValue;
        this.theBatchSize = Math.max(1, theBatchSizeValue);
        this.theTimeout = Duration.ofMillis(theTimeoutMillis);
        this.theClient = theClientValue != null ? theClientValue
                : HttpClient.newBuilder().connectTimeout(theTimeout).build();
    }

    /** {@inheritDoc} <p>Never throws. */
    public void record(McpAuditEvent theEvent) {
        if (theEvent == null) {
            return;
        }
        List<String> theBatch = null;
        synchronized (thePending) {
            thePending.add(envelope(theEvent));
            if (thePending.size() >= theBatchSize) {
                theBatch = new ArrayList<String>(thePending);
                thePending.clear();
            }
        }
        // Posted OUTSIDE the lock: a slow collector would otherwise block every other tool call in
        // the server behind this one's audit record.
        if (theBatch != null) {
            post(theBatch);
        }
    }

    /**
     * One HEC event.
     *
     * <p>{@code time} is the record's own timestamp in epoch seconds, not the moment Splunk
     * received it — a batch delivered after an outage must appear in the timeline where it
     * happened, or the trail's ordering silently becomes a record of when the network recovered.
     */
    String envelope(McpAuditEvent theEvent) {
        StringBuilder theJson = new StringBuilder("{\"time\":");
        theJson.append(theEvent.getTimestampMillis() / 1000L);
        theJson.append(",\"sourcetype\":\"").append(escape(theSourceType)).append('"');
        if (theIndex != null) {
            theJson.append(",\"index\":\"").append(escape(theIndex)).append('"');
        }
        // The record goes in as an OBJECT rather than a string, so Splunk indexes its fields and a
        // search can ask for one tool or one user without a regex over the payload.
        theJson.append(",\"event\":").append(theEvent.toJson()).append('}');
        return theJson.toString();
    }

    /** Send a batch, counting every record in it as lost if it does not land. */
    private void post(List<String> theBatch) {
        if (theBatch.isEmpty()) {
            return;
        }
        StringBuilder theBody = new StringBuilder();
        for (String theEvent : theBatch) {
            // HEC takes concatenated JSON objects, not an array. No separator is needed and a
            // comma between them is an error.
            theBody.append(theEvent);
        }
        try {
            HttpRequest theRequest = HttpRequest.newBuilder(theEndpoint)
                    .timeout(theTimeout)
                    .header("Authorization", "Splunk " + theToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(theBody.toString(), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> theResponse =
                    theClient.send(theRequest, HttpResponse.BodyHandlers.ofString());
            if (theResponse.statusCode() / 100 == 2) {
                theDelivered.addAndGet(theBatch.size());
                return;
            }
            countDrops(theBatch.size(), "HTTP " + theResponse.statusCode() + " " + theResponse.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            countDrops(theBatch.size(), "interrupted");
        } catch (IOException | RuntimeException e) {
            countDrops(theBatch.size(), e.toString());
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Posts what is pending, then reports whether anything has been lost since the previous
     * flush. Compared against the count at the END OF THE PREVIOUS FLUSH, as
     * {@link KafkaAuditSink#flush()} explains: a batch can be rejected inside {@link #record} when
     * it fills, long before any flush begins, and sampling at the top of this method would not see
     * it — which is exactly how a spool comes to delete records that never arrived.
     */
    @Override
    public boolean flush() {
        List<String> theBatch;
        synchronized (thePending) {
            theBatch = new ArrayList<String>(thePending);
            thePending.clear();
        }
        post(theBatch);

        synchronized (this) {
            long theNow = theDropped.get();
            boolean theCleanFlag = theNow == theDroppedAtLastFlush;
            theDroppedAtLastFlush = theNow;
            return theCleanFlag;
        }
    }

    private void countDrops(long theCount, String theReason) {
        long theTotal = theDropped.addAndGet(theCount);
        theLog.error("Audit batch of " + theCount + " record(s) not accepted by Splunk ("
                + theTotal + " lost so far): " + theReason);
    }

    public long getDroppedCount() {
        return theDropped.get();
    }

    public long getDeliveredCount() {
        return theDelivered.get();
    }

    @Override
    public long getPendingCount() {
        synchronized (thePending) {
            return thePending.size();
        }
    }

    @Override
    public String describe() {
        return "Splunk HEC " + theEndpoint + (theIndex == null ? "" : " index=" + theIndex);
    }

    /** Send what is left before the process goes away. */
    public void close() {
        flush();
        if (theDropped.get() > 0L) {
            theLog.error("Audit trail is incomplete: " + theDropped.get()
                    + " record(s) were never accepted by Splunk.");
        }
    }

    // ---- settings ----

    /** The collector endpoint, appending the HEC path unless the URL already carries it. */
    static URI endpoint(String theUrl) {
        String theTrimmed = theUrl.trim();
        while (theTrimmed.endsWith("/")) {
            theTrimmed = theTrimmed.substring(0, theTrimmed.length() - 1);
        }
        if (theTrimmed.endsWith(COLLECTOR_PATH)) {
            return URI.create(theTrimmed);
        }
        return URI.create(theTrimmed + COLLECTOR_PATH);
    }

    /**
     * The HEC token, from a file if one is named.
     *
     * <p>The file wins when both are set. Nobody sets both on purpose, and preferring the safer of
     * the two is the right way to resolve an accident.
     */
    static String token() {
        String theFile = trimToNull(McpAuditSinks.setting(TOKEN_FILE_VARIABLE));
        if (theFile != null) {
            try {
                String theContents = Files.readString(Path.of(theFile), StandardCharsets.UTF_8).trim();
                if (theContents.isEmpty()) {
                    throw new IllegalArgumentException(TOKEN_FILE_VARIABLE + "=" + theFile
                            + " is empty.");
                }
                return theContents;
            } catch (IOException e) {
                throw new IllegalArgumentException("Cannot read " + TOKEN_FILE_VARIABLE + "="
                        + theFile + ": " + e.getMessage());
            }
        }
        String theInline = trimToNull(McpAuditSinks.setting(TOKEN_VARIABLE));
        if (theInline == null) {
            throw new IllegalArgumentException("One of " + TOKEN_FILE_VARIABLE + " (preferred) or "
                    + TOKEN_VARIABLE + " must be set to use " + SplunkAuditSink.class.getName());
        }
        return theInline;
    }

    static String sourceType(String theSetting) {
        String theValue = trimToNull(theSetting);
        return theValue == null ? DEFAULT_SOURCETYPE : theValue;
    }

    private static String required(String theVariable) {
        String theValue = trimToNull(McpAuditSinks.setting(theVariable));
        if (theValue == null) {
            throw new IllegalArgumentException(theVariable + " must be set to use "
                    + SplunkAuditSink.class.getName());
        }
        return theValue;
    }

    private static String trimToNull(String theValue) {
        return theValue == null || theValue.trim().isEmpty() ? null : theValue.trim();
    }

    private static int number(String theName, String theSetting, int theDefault) {
        if (theSetting == null || theSetting.trim().isEmpty()) {
            return theDefault;
        }
        try {
            int theValue = Integer.parseInt(theSetting.trim());
            if (theValue <= 0) {
                throw new IllegalArgumentException(theName + " must be greater than zero, not '"
                        + theSetting.trim() + "'");
            }
            return theValue;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(theName + " must be a whole number, not '"
                    + theSetting.trim() + "'");
        }
    }

    private static String escape(String theValue) {
        return theValue.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
