package com.mcpdbwizard.pub;

/**
 * Where a generated MCP server sends its audit records.
 *
 * <p>A service-provider interface rather than a built-in: {@code MCP_AUDIT_SINK} names an
 * implementing class, exactly as {@code DAO_LOG_TYPE} selects a {@link LogInterface} backend. That
 * keeps every client library — Kafka's included — an optional dependency nobody else pays for, and
 * makes a broker, a database table and a file three implementations rather than three forks of the
 * emitted code.
 *
 * <p>Implementations need a public no-argument constructor and must configure themselves from the
 * environment.
 *
 * <p><b>{@link #record} must not throw.</b> It is called from a {@code finally} on the tool-call path,
 * so an exception escaping it would replace the caller's real result — or its real error — with a
 * failure of the audit system. A sink that cannot deliver should count and report, not propagate.
 * Whether it should also <em>block</em> is the deployment's decision and belongs in the
 * implementation; see {@code docs/mcp-audit-sink-plan.md} §2.3.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public interface McpAuditSink {

    /**
     * Record one call. Must not throw.
     *
     * @param theEvent the call to record; never null
     */
    void record(McpAuditEvent theEvent);

    /**
     * Confirm that everything handed to {@link #record} since the last flush is durably accepted.
     *
     * <p>This is what makes a spool possible. A spool writes each record to disk first and may only
     * delete it once the sink has genuinely taken it — and for an asynchronous sink like Kafka,
     * {@code record} returning tells you nothing, because the send has not completed yet.
     *
     * <p>The default returns true, which is correct for a sink that delivers synchronously inside
     * {@code record}. An asynchronous sink must override it, and must return <b>false</b> if anything
     * since the last flush was lost — returning true on a failed batch would let a spool delete
     * records that never arrived, which is the one way a spool can be worse than no spool.
     *
     * @return true if everything since the last flush is safely delivered
     */
    default boolean flush() {
        return true;
    }

    /** Flush and release anything held. Called from the server's shutdown hook. */
    /**
     * How many records this sink is known to have LOST, or -1 when it does not report.
     *
     * <p>The number an operator actually needs, and the one nothing surfaced before: a trail is
     * only worth citing if it is complete, and "complete" is exactly what a drop count denies.
     * Reported through the SPI rather than by casting to a particular sink so a status page keeps
     * working when the sink is swapped.
     *
     * <p>-1 means "this sink does not count", which a caller must show differently from 0. Claiming
     * zero losses on a sink that cannot tell is the one wrong answer here.
     */
    default long getDroppedCount() {
        return -1L;
    }

    /** How many records were confirmed delivered, or -1 when the sink does not report. */
    default long getDeliveredCount() {
        return -1L;
    }

    /** How many records are written down but not yet delivered, or -1 when not reported. */
    default long getPendingCount() {
        return -1L;
    }

    /**
     * A short human description of what this sink is, for an operator reading a status page.
     *
     * <p>Defaults to the class name. A wrapper overrides it to name what it wraps — otherwise a
     * spooled Kafka sink reports only "SpoolingAuditSink", and where the records finally go is the
     * part being asked about.
     */
    default String describe() {
        return getClass().getSimpleName();
    }

    void close();
}
