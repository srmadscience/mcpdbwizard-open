package com.mcpdbwizard.pub;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Writes the audit trail to S3 as rolling JSONL objects.
 *
 * <p>Select it with {@code MCP_AUDIT_SINK=com.mcpdbwizard.pub.S3AuditSink}.
 *
 * <table>
 *   <caption>Environment</caption>
 *   <tr><td>{@code MCP_AUDIT_S3_BUCKET}</td><td>Required.</td></tr>
 *   <tr><td>{@code MCP_AUDIT_S3_PREFIX}</td><td>Key prefix, default {@value #DEFAULT_PREFIX}.</td></tr>
 *   <tr><td>{@code MCP_AUDIT_S3_REGION}</td><td>Region. The SDK's own resolution if unset.</td></tr>
 *   <tr><td>{@code MCP_AUDIT_S3_ENDPOINT}</td><td>Override, for MinIO or a VPC endpoint.</td></tr>
 *   <tr><td>{@code MCP_AUDIT_S3_ROLL_BYTES}</td><td>Roll an object at this size, default
 *       {@value #DEFAULT_ROLL_BYTES}.</td></tr>
 *   <tr><td>{@code MCP_AUDIT_S3_ROLL_SECONDS}</td><td>...or after this long, default
 *       {@value #DEFAULT_ROLL_SECONDS}.</td></tr>
 * </table>
 *
 * <h2>An object store is not a stream, so this batches</h2>
 *
 * <p>A PUT per tool call would be slow and, at S3's per-request pricing, absurd. Records accumulate
 * and become one object when it is big enough or old enough.
 *
 * <p><b>Keys are date-partitioned</b> —
 * {@code <prefix>/<config>/yyyy/MM/dd/<epochMillis>-<uuid>.jsonl} — for two concrete reasons rather
 * than tidiness. A lifecycle rule can expire a prefix, which is how retention is done on the S3
 * side; and Athena or Glue can partition on the date without reading the objects.
 *
 * <h2>The roll timer is not optional</h2>
 *
 * <p>Rolling only when a record arrives would mean a quiet server holds its last few records in
 * memory indefinitely, and loses them if the process stops. A daemon thread rolls on schedule so the
 * window in which a record exists only in memory is bounded by {@code MCP_AUDIT_S3_ROLL_SECONDS}
 * rather than by how busy the server happens to be.
 *
 * <p>Records in that window are <b>in memory only</b>. {@link SpoolingAuditSink} in front is what
 * closes it: it writes to disk before this ever sees a record, and deletes only once
 * {@link #flush()} confirms the PUT.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class S3AuditSink implements McpAuditSink {

    public static final String BUCKET_VARIABLE = "MCP_AUDIT_S3_BUCKET";
    public static final String PREFIX_VARIABLE = "MCP_AUDIT_S3_PREFIX";
    public static final String REGION_VARIABLE = "MCP_AUDIT_S3_REGION";
    public static final String ENDPOINT_VARIABLE = "MCP_AUDIT_S3_ENDPOINT";
    public static final String ROLL_BYTES_VARIABLE = "MCP_AUDIT_S3_ROLL_BYTES";
    public static final String ROLL_SECONDS_VARIABLE = "MCP_AUDIT_S3_ROLL_SECONDS";

    public static final String DEFAULT_PREFIX = "mcp-audit";
    public static final long DEFAULT_ROLL_BYTES = 8L * 1024L * 1024L;
    public static final long DEFAULT_ROLL_SECONDS = 300L;

    private static final DateTimeFormatter DATE_PATH =
            DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneOffset.UTC);

    private final S3Client theClient;
    private final String theBucket;
    private final String thePrefix;
    private final String theConfig;
    private final long theRollBytes;
    private final long theRollMillis;
    private final LogInterface theLog = new JulLog("S3AuditSink");

    private final List<String> thePending = new ArrayList<String>();
    private long thePendingBytes;
    private long theOldestPendingMillis;

    private final AtomicLong theDropped = new AtomicLong();
    private final AtomicLong theDelivered = new AtomicLong();
    private long theDroppedAtLastFlush;

    private Thread theRoller;
    private volatile boolean theRunningFlag = true;

    /** Built reflectively by {@link McpAuditSinks#fromEnvironment()}. */
    public S3AuditSink() {
        this(buildClient(), required(BUCKET_VARIABLE), prefix(McpAuditSinks.setting(PREFIX_VARIABLE)),
                configLabel(),
                positive(ROLL_BYTES_VARIABLE, McpAuditSinks.setting(ROLL_BYTES_VARIABLE), DEFAULT_ROLL_BYTES),
                positive(ROLL_SECONDS_VARIABLE, McpAuditSinks.setting(ROLL_SECONDS_VARIABLE),
                        DEFAULT_ROLL_SECONDS));
        startRolling();
    }

    /** For tests: inject a client rather than reach AWS. The roll thread is not started. */
    public S3AuditSink(S3Client theClientValue, String theBucketValue, String thePrefixValue,
                       String theConfigValue, long theRollBytesValue, long theRollSecondsValue) {
        this.theClient = theClientValue;
        this.theBucket = theBucketValue;
        this.thePrefix = thePrefixValue;
        this.theConfig = theConfigValue;
        this.theRollBytes = theRollBytesValue;
        this.theRollMillis = theRollSecondsValue * 1000L;
    }

    /** Start the scheduled roll. Separate from the constructor so a test can roll deterministically. */
    public void startRolling() {
        theRoller = new Thread(() -> {
            while (theRunningFlag) {
                try {
                    Thread.sleep(1000L);
                    rollIfDue();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception e) {
                    theLog.warning("Scheduled audit roll to S3 failed, will retry: " + e);
                }
            }
        }, "mcp-audit-s3");
        theRoller.setDaemon(true);
        theRoller.start();
    }

    /** {@inheritDoc} <p>Never throws. */
    public void record(McpAuditEvent theEvent) {
        if (theEvent == null) {
            return;
        }
        List<String> theBatch = null;
        synchronized (thePending) {
            if (thePending.isEmpty()) {
                theOldestPendingMillis = System.currentTimeMillis();
            }
            String theLine = theEvent.toJson();
            thePending.add(theLine);
            thePendingBytes += theLine.length() + 1L;
            if (thePendingBytes >= theRollBytes) {
                theBatch = take();
            }
        }
        // Uploaded OUTSIDE the lock: an S3 round trip inside it would put every other tool call in
        // this server behind one record's PUT.
        if (theBatch != null) {
            put(theBatch);
        }
    }

    /** Roll if the pending batch is old enough. Called by the timer, and testable on its own. */
    public void rollIfDue() {
        List<String> theBatch = null;
        synchronized (thePending) {
            if (!thePending.isEmpty()
                    && System.currentTimeMillis() - theOldestPendingMillis >= theRollMillis) {
                theBatch = take();
            }
        }
        if (theBatch != null) {
            put(theBatch);
        }
    }

    /** Caller holds the lock. */
    private List<String> take() {
        List<String> theBatch = new ArrayList<String>(thePending);
        thePending.clear();
        thePendingBytes = 0L;
        return theBatch;
    }

    /**
     * The key for an object rolled now.
     *
     * <p>The millisecond prefix sorts objects within a day, and the UUID makes the key unique when
     * two servers sharing a config roll in the same millisecond — which is not hypothetical, since
     * a key collision in S3 silently OVERWRITES rather than failing, and the record it destroyed
     * would never be missed.
     */
    String keyFor(long theMillis) {
        return thePrefix + "/" + theConfig + "/" + DATE_PATH.format(Instant.ofEpochMilli(theMillis))
                + "/" + theMillis + "-" + UUID.randomUUID() + ".jsonl";
    }

    private void put(List<String> theBatch) {
        if (theBatch.isEmpty()) {
            return;
        }
        StringBuilder theBody = new StringBuilder();
        for (String theLine : theBatch) {
            theBody.append(theLine).append('\n');
        }
        String theKey = keyFor(System.currentTimeMillis());
        try {
            theClient.putObject(PutObjectRequest.builder()
                            .bucket(theBucket)
                            .key(theKey)
                            .contentType("application/x-ndjson")
                            .build(),
                    RequestBody.fromString(theBody.toString(), StandardCharsets.UTF_8));
            theDelivered.addAndGet(theBatch.size());
        } catch (RuntimeException e) {
            countDrops(theBatch.size(), theKey + ": " + e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Forces a roll, then reports whether anything has been lost since the previous flush.
     * Compared against the count at the END OF THE PREVIOUS FLUSH — the rule
     * {@link KafkaAuditSink#flush()} sets out, and it applies here for the same reason: a batch can
     * be rejected inside {@link #record} when it fills, before any flush begins.
     */
    @Override
    public boolean flush() {
        List<String> theBatch;
        synchronized (thePending) {
            theBatch = take();
        }
        put(theBatch);

        synchronized (this) {
            long theNow = theDropped.get();
            boolean theCleanFlag = theNow == theDroppedAtLastFlush;
            theDroppedAtLastFlush = theNow;
            return theCleanFlag;
        }
    }

    private void countDrops(long theCount, String theReason) {
        long theTotal = theDropped.addAndGet(theCount);
        theLog.error("Audit object of " + theCount + " record(s) not written to S3 (" + theTotal
                + " lost so far): " + theReason);
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
        return "S3 s3://" + theBucket + "/" + thePrefix + "/" + theConfig;
    }

    /** Roll what is left, then release the client. */
    public void close() {
        theRunningFlag = false;
        if (theRoller != null) {
            theRoller.interrupt();
        }
        flush();
        if (theDropped.get() > 0L) {
            theLog.error("Audit trail is incomplete: " + theDropped.get()
                    + " record(s) were never written to S3.");
        }
        try {
            theClient.close();
        } catch (RuntimeException e) {
            theLog.warning("Closing the S3 client failed: " + e);
        }
    }

    // ---- settings ----

    private static S3Client buildClient() {
        S3ClientBuilder theBuilder = S3Client.builder();
        String theRegion = trimToNull(McpAuditSinks.setting(REGION_VARIABLE));
        if (theRegion != null) {
            theBuilder = theBuilder.region(Region.of(theRegion));
        }
        String theEndpoint = trimToNull(McpAuditSinks.setting(ENDPOINT_VARIABLE));
        if (theEndpoint != null) {
            theBuilder = theBuilder.endpointOverride(URI.create(theEndpoint));
        }
        // No credentials are configured here on purpose. The SDK's default chain is the point of
        // taking the dependency: in a container it finds the task role or the service account,
        // which is what a deployment in AWS actually uses.
        return theBuilder.build();
    }

    /**
     * Which config this server serves, for the key.
     *
     * <p>Reuses the label the metrics already carry, so one server's objects are separable from
     * another's without a second variable that could disagree with the first.
     */
    static String configLabel() {
        String theValue = trimToNull(McpAuditSinks.setting(McpMetrics.CONFIG_LABEL_VARIABLE));
        return theValue == null ? "unknown" : sanitise(theValue);
    }

    /** Keys are opaque to S3, but a slash makes a folder and whitespace makes a key nobody can type. */
    static String sanitise(String theValue) {
        String theClean = theValue.trim().replaceAll("[^A-Za-z0-9._-]", "_");
        return theClean.isEmpty() ? "unknown" : theClean;
    }

    static String prefix(String theSetting) {
        String theValue = trimToNull(theSetting);
        if (theValue == null) {
            return DEFAULT_PREFIX;
        }
        while (theValue.endsWith("/")) {
            theValue = theValue.substring(0, theValue.length() - 1);
        }
        while (theValue.startsWith("/")) {
            // A leading slash makes an S3 key beginning with an empty path segment, which is legal
            // and renders as an unnamed folder nobody can navigate.
            theValue = theValue.substring(1);
        }
        return theValue.isEmpty() ? DEFAULT_PREFIX : theValue;
    }

    private static String required(String theVariable) {
        String theValue = trimToNull(McpAuditSinks.setting(theVariable));
        if (theValue == null) {
            throw new IllegalArgumentException(theVariable + " must be set to use "
                    + S3AuditSink.class.getName());
        }
        return theValue;
    }

    private static String trimToNull(String theValue) {
        return theValue == null || theValue.trim().isEmpty() ? null : theValue.trim();
    }

    private static long positive(String theName, String theSetting, long theDefault) {
        if (theSetting == null || theSetting.trim().isEmpty()) {
            return theDefault;
        }
        try {
            long theValue = Long.parseLong(theSetting.trim());
            if (theValue <= 0L) {
                throw new IllegalArgumentException(theName + " must be greater than zero, not '"
                        + theSetting.trim() + "'");
            }
            return theValue;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(theName + " must be a whole number, not '"
                    + theSetting.trim() + "'");
        }
    }
}
