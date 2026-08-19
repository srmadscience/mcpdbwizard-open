package com.mcpdbwizard.pub;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Sends every record to several sinks, so a deployment can keep a local trail <em>and</em> stream.
 *
 * <p>{@code MCP_AUDIT_SINK} names one class, which was enough while the only question was where
 * records went. It stopped being enough when the local trail became something every tier has: a
 * licensed installation keeps records here for its window and sends them to Kafka or Splunk as well,
 * and those are two sinks, not a choice between them.
 *
 * <h2>Every member is told, whatever the others do</h2>
 *
 * <p>{@link #record} catches per sink. One sink throwing must not stop the next from being told —
 * and it must certainly not reach the caller, which is on the tool-call path with a real result to
 * return.
 *
 * <p><b>{@link #flush()} asks every member before answering, and never short-circuits.</b> A
 * {@code &&} chain would skip flushing the second sink as soon as the first said no, which is the
 * subtle version of not flushing at all: a spool in front of this would then be told "not
 * delivered" by a sink that was never asked, and would keep replaying records the other member had
 * already taken.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class FanOutAuditSink implements McpAuditSink {

    private final List<McpAuditSink> theSinks;
    private final LogInterface theLog = new JulLog("FanOutAuditSink");

    public FanOutAuditSink(McpAuditSink... theSinkValues) {
        this.theSinks = new ArrayList<McpAuditSink>(Arrays.asList(theSinkValues));
        this.theSinks.removeIf(theSink -> theSink == null);
    }

    /** The members, in the order they are told. */
    public List<McpAuditSink> sinks() {
        return java.util.Collections.unmodifiableList(theSinks);
    }

    public void record(McpAuditEvent theEvent) {
        for (McpAuditSink theSink : theSinks) {
            try {
                theSink.record(theEvent);
            } catch (RuntimeException e) {
                // The SPI says record must not throw; this defends against a member that breaks
                // that contract, because the alternative is one bad sink silently disabling the
                // rest of the audit trail.
                theLog.warning("Audit sink " + theSink.describe() + " threw on record: " + e);
            }
        }
    }

    @Override
    public boolean flush() {
        boolean theAllConfirmedFlag = true;
        for (McpAuditSink theSink : theSinks) {
            try {
                if (!theSink.flush()) {
                    theAllConfirmedFlag = false;
                }
            } catch (RuntimeException e) {
                theLog.warning("Audit sink " + theSink.describe() + " threw on flush: " + e);
                theAllConfirmedFlag = false;
            }
        }
        return theAllConfirmedFlag;
    }

    /**
     * The total known to be lost, or -1 when no member counts.
     *
     * <p>Zero and -1 must stay distinguishable: -1 means nobody is counting, while 0 is a positive
     * claim that nothing was lost. Summing a -1 into a total would turn the second into the first.
     */
    @Override
    public long getDroppedCount() {
        return sum(0);
    }

    @Override
    public long getDeliveredCount() {
        return sum(1);
    }

    @Override
    public long getPendingCount() {
        return sum(2);
    }

    private long sum(int theWhich) {
        long theTotal = 0L;
        boolean theAnyReportedFlag = false;
        for (McpAuditSink theSink : theSinks) {
            long theValue = theWhich == 0 ? theSink.getDroppedCount()
                    : theWhich == 1 ? theSink.getDeliveredCount() : theSink.getPendingCount();
            if (theValue >= 0L) {
                theAnyReportedFlag = true;
                theTotal += theValue;
            }
        }
        return theAnyReportedFlag ? theTotal : -1L;
    }

    @Override
    public String describe() {
        StringBuilder theText = new StringBuilder();
        for (McpAuditSink theSink : theSinks) {
            if (theText.length() > 0) {
                theText.append(" + ");
            }
            theText.append(theSink.describe());
        }
        return theText.length() == 0 ? "nothing" : theText.toString();
    }

    /** Close every member, even if one of them fails. */
    public void close() {
        for (McpAuditSink theSink : theSinks) {
            try {
                theSink.close();
            } catch (RuntimeException e) {
                theLog.warning("Audit sink " + theSink.describe() + " threw on close: " + e);
            }
        }
    }
}
