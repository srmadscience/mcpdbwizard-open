package com.mcpdbwizard.loadtest;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * One thread, one MCP session, one slice of the run.
 *
 * <p>Each worker holds its own client because an MCP session is stateful and single-threaded;
 * sharing one would serialise every call and measure the client's lock instead of the server.
 *
 * <p><b>The budget is claimed from a shared counter, not divided up front.</b> Splitting
 * {@code --calls} into equal per-thread slices makes the run last as long as its slowest thread and
 * leaves the others idle at the end — so the tail of the run is quietly less concurrent than the
 * head, and the last percentiles are measured against a lighter load than the first. Claiming a
 * slot each time keeps every thread busy until the budget is gone.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class LoadWorker implements Runnable {

    private final int theThreadIndex;

    private final LoadOptions theOptions;

    private final Workload theWorkload;

    private final RatePacer thePacer;

    private final long theDeadlineNanos;

    private final long theWarmupEndNanos;

    private final AtomicLong theProgressDone;

    private final AtomicLong theProgressErrors;

    private final Map<String, ToolSamples> theResults = new LinkedHashMap<String, ToolSamples>();

    private final Random theRandom;

    /** Calls made before the warm-up window closed, and therefore left out of the report. */
    private long theDiscarded;

    private volatile String theFailure;

    public LoadWorker(int theIndex, LoadOptions theOptionValues, Workload theWorkloadValue,
            RatePacer thePacerValue, long theDeadlineValue, long theWarmupEndValue,
            AtomicLong theDoneCounter, AtomicLong theErrorCounter) {
        this.theThreadIndex = theIndex;
        this.theOptions = theOptionValues;
        this.theWorkload = theWorkloadValue;
        this.thePacer = thePacerValue;
        this.theDeadlineNanos = theDeadlineValue;
        this.theWarmupEndNanos = theWarmupEndValue;
        this.theProgressDone = theDoneCounter;
        this.theProgressErrors = theErrorCounter;
        // Seeded per thread from one run seed, so a run is repeatable and two threads do not walk
        // the same random sequence.
        this.theRandom = new Random(theOptionValues.randomSeed() + theIndex);
        for (WorkloadEntry theEntry : theWorkloadValue.entries()) {
            theResults.putIfAbsent(theEntry.tool(), new ToolSamples());
        }
    }

    @Override
    public void run() {
        McpSyncClient theClient = null;
        try {
            theClient = McpClients.connect(theOptions);
            callUntilDone(theClient);
        } catch (RuntimeException e) {
            // Recorded rather than printed, so one thread failing to connect does not interleave a
            // stack trace through another thread's progress lines. McpLoad reports it at the end.
            theFailure = describe(e);
        } finally {
            McpClients.closeQuietly(theClient);
        }
    }

    private void callUntilDone(McpSyncClient theClient) {
        long theBudget = theOptions.callBudget();
        while (true) {
            long theIndex = thePacer.claimSlot();
            if (theBudget > 0L && theIndex >= theBudget) {
                return;
            }
            long theDueNanos = thePacer.slotNanosFor(theIndex);
            if (theDueNanos >= theDeadlineNanos) {
                // The slot falls outside the time limit; waiting for it would only overrun.
                return;
            }
            long theLateBy = thePacer.awaitSlot(theDueNanos);
            if (System.nanoTime() >= theDeadlineNanos) {
                return;
            }
            callOnce(theClient, theIndex, theDueNanos, theLateBy > 0L);
        }
    }

    private void callOnce(McpSyncClient theClient, long theIndex, long theDueNanos,
            boolean theLateFlag) {
        WorkloadEntry theEntry = theWorkload.pick(theIndex);
        ToolSamples theSamples = theResults.get(theEntry.tool());
        Map<String, Object> theArgs = theEntry.argsFor(theIndex, theThreadIndex, theRandom);

        long theSentNanos = System.nanoTime();
        McpSchema.CallToolResult theResult = null;
        String theErrorKind = null;
        try {
            theResult = theClient.callTool(new McpSchema.CallToolRequest(theEntry.tool(), theArgs));
            if (Boolean.TRUE.equals(theResult.isError())) {
                theErrorKind = "tool error: " + summarise(firstText(theResult));
            } else if (theResult.content().isEmpty() && theResult.structuredContent() == null) {
                theErrorKind = "empty result";
            }
        } catch (RuntimeException e) {
            theErrorKind = describe(e);
        }
        long theFinishedNanos = System.nanoTime();

        theProgressDone.incrementAndGet();
        if (theErrorKind != null) {
            theProgressErrors.incrementAndGet();
        }
        if (theFinishedNanos < theWarmupEndNanos) {
            theDiscarded++;
            return;
        }
        long theServiceMicros = (theFinishedNanos - theSentNanos) / 1000L;
        // With no target rate there is no schedule to be late against: every slot is nominally due
        // at the run's start, so measuring from it would report the time since the run began and
        // call every call late. Flat out, the two latencies ARE the same thing, and the report says
        // so -- this is what makes that claim true rather than merely printed.
        long theScheduledMicros = thePacer.isPaced()
                ? (theFinishedNanos - theDueNanos) / 1000L : theServiceMicros;
        if (theErrorKind != null) {
            theSamples.recordError(theErrorKind, theServiceMicros, theScheduledMicros, theLateFlag);
            return;
        }
        theSamples.recordSuccess(theServiceMicros, theScheduledMicros, theLateFlag);
        if (theEntry.wantsUniqueCheck()) {
            theSamples.recordReturnedValue(firstNumberIn(firstText(theResult)));
        }
    }

    public Map<String, ToolSamples> results() {
        return theResults;
    }

    public long discarded() {
        return theDiscarded;
    }

    /** Why this worker stopped early, or null if it did not. */
    public String failure() {
        return theFailure;
    }

    private static String firstText(McpSchema.CallToolResult theResult) {
        if (theResult == null || theResult.content().isEmpty()) {
            return "";
        }
        McpSchema.Content theContent = theResult.content().get(0);
        return theContent instanceof McpSchema.TextContent
                ? ((McpSchema.TextContent) theContent).text() : theContent.type();
    }

    /**
     * The first whole number in a result, for the uniqueness check.
     *
     * @return the number, or -1 when there is not one
     */
    static long firstNumberIn(String theText) {
        if (theText == null) {
            return -1L;
        }
        StringBuilder theDigits = new StringBuilder();
        for (int i = 0; i < theText.length(); i++) {
            char theChar = theText.charAt(i);
            if (Character.isDigit(theChar)) {
                theDigits.append(theChar);
            } else if (theDigits.length() > 0) {
                break;
            }
        }
        if (theDigits.length() == 0 || theDigits.length() > 18) {
            return -1L;
        }
        return Long.parseLong(theDigits.toString());
    }

    /**
     * Shorten a message so errors group.
     *
     * <p>Grouping is the point: Oracle messages carry a line number, a cursor id or a timestamp that
     * differs on every occurrence, so untrimmed text produces twelve thousand groups of one and the
     * summary says nothing.
     */
    static String describe(Throwable theThrowable) {
        // Walk to the deepest cause. The MCP SDK wraps a transport failure in "Client failed to
        // initialize by explicit API call", which names the API rather than the reason -- an HTTP
        // 401 arrives looking like a client bug. The bottom of the chain is where the answer is.
        Throwable theCause = theThrowable;
        StringBuilder theChain = new StringBuilder(theThrowable.getClass().getSimpleName())
                .append(": ").append(summarise(theThrowable.getMessage()));
        while (theCause.getCause() != null && theCause.getCause() != theCause) {
            theCause = theCause.getCause();
            theChain.append(" <- ").append(theCause.getClass().getSimpleName())
                    .append(": ").append(summarise(theCause.getMessage()));
        }
        return theChain.toString();
    }

    static String summarise(String theMessage) {
        if (theMessage == null) {
            return "(no message)";
        }
        String theFirst = theMessage;
        int theBreak = theFirst.indexOf('\n');
        if (theBreak >= 0) {
            theFirst = theFirst.substring(0, theBreak);
        }
        theFirst = theFirst.trim();
        return theFirst.length() > 120 ? theFirst.substring(0, 117) + "..." : theFirst;
    }
}
