package com.mcpdbwizard.loadtest;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Everything one run measured about one tool.
 *
 * <p>Each worker keeps its own instance per tool and they are merged once the run is over, so
 * nothing on the hot path takes a lock. That is not premature: at a few thousand calls a second a
 * shared, synchronised accumulator becomes a contention point of its own, and the tool would then be
 * measuring itself.
 *
 * <h2>Two latencies, not one</h2>
 *
 * <p>{@link #serviceMicros} is send-to-receive. {@link #scheduledMicros} runs from the instant the
 * call was <em>due</em> under {@link RatePacer}'s fixed schedule. In an unpaced run they are the
 * same number. In a paced run that the server cannot keep up with they diverge without limit, and
 * the second is the honest one — see {@link RatePacer} for why quoting only the first is the
 * coordinated-omission mistake.
 *
 * <h2>Errors are grouped, not just counted</h2>
 *
 * <p>"12,000 errors" and "12,000 errors, every one ORA-12516" are different findings, and only the
 * second one tells you the database ran out of processes. A bare count has sent people looking at
 * the wrong layer before now.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class ToolSamples {

    private final LatencyDigest theServiceMicros = new LatencyDigest();

    private final LatencyDigest theScheduledMicros = new LatencyDigest();

    /**
     * Numbers pulled out of the answers, for {@code "check": "unique"}.
     *
     * <p>A {@link LatencyDigest} because it is exactly the growable, sortable list of longs this
     * needs; nothing here is a latency.
     */
    private final LatencyDigest theReturnedValues = new LatencyDigest();

    private long theErrors;

    private long theUnparsedValues;

    /** Calls that began after their scheduled slot — the run failing to hold its target rate. */
    private long theLateStarts;

    private final Map<String, Long> theErrorsByKind = new LinkedHashMap<String, Long>();

    public void recordSuccess(long theServiceValue, long theScheduledValue, boolean theLateFlag) {
        theServiceMicros.add(theServiceValue);
        theScheduledMicros.add(theScheduledValue);
        if (theLateFlag) {
            theLateStarts++;
        }
    }

    public void recordError(String theKind, long theServiceValue, long theScheduledValue,
            boolean theLateFlag) {
        // The latency of a failure is still a latency, and leaving it out flatters a run whose
        // errors are slow ones — a connection timeout is the slowest call the server ever makes.
        theServiceMicros.add(theServiceValue);
        theScheduledMicros.add(theScheduledValue);
        if (theLateFlag) {
            theLateStarts++;
        }
        theErrors++;
        theErrorsByKind.merge(theKind, Long.valueOf(1L), (a, b) -> Long.valueOf(a.longValue() + 1L));
    }

    public void recordReturnedValue(long theValue) {
        if (theValue < 0L) {
            theUnparsedValues++;
        } else {
            theReturnedValues.add(theValue);
        }
    }

    public void merge(ToolSamples theOther) {
        theServiceMicros.addAll(theOther.theServiceMicros);
        theScheduledMicros.addAll(theOther.theScheduledMicros);
        theReturnedValues.addAll(theOther.theReturnedValues);
        theErrors += theOther.theErrors;
        theUnparsedValues += theOther.theUnparsedValues;
        theLateStarts += theOther.theLateStarts;
        for (Map.Entry<String, Long> theEntry : theOther.theErrorsByKind.entrySet()) {
            theErrorsByKind.merge(theEntry.getKey(), theEntry.getValue(),
                (a, b) -> Long.valueOf(a.longValue() + b.longValue()));
        }
    }

    public LatencyDigest serviceMicros() {
        return theServiceMicros;
    }

    public LatencyDigest scheduledMicros() {
        return theScheduledMicros;
    }

    public LatencyDigest returnedValues() {
        return theReturnedValues;
    }

    public long errors() {
        return theErrors;
    }

    public long unparsedValues() {
        return theUnparsedValues;
    }

    public long lateStarts() {
        return theLateStarts;
    }

    public Map<String, Long> errorsByKind() {
        return theErrorsByKind;
    }

    public int calls() {
        return theServiceMicros.size();
    }
}
