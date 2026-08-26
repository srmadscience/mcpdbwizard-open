package com.mcpdbwizard.loadtest;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the run measured, printed for a person and optionally written for a machine.
 *
 * <p>Two things here are choices rather than formatting.
 *
 * <p><b>Both latencies are always shown.</b> Service time is what the server took; scheduled latency
 * is what a client experienced, queueing included. In an unpaced run they are equal and the report
 * says so in one line rather than repeating the same numbers twice. In a paced run the gap between
 * them <em>is</em> the finding — see {@link RatePacer}.
 *
 * <p><b>Achieved rate is stated against the target.</b> A run that asked for 500 calls/sec and
 * managed 180 has answered the question, but only if it says so; a report giving latency alone lets
 * a saturated server look fast.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class LoadReport {

    private final LoadOptions theOptions;

    private final Map<String, ToolSamples> theResults;

    private final long theWallMillis;

    private final long theDiscarded;

    private final List<String> theWorkerFailures;

    public LoadReport(LoadOptions theOptionValues, Map<String, ToolSamples> theResultValues,
            long theWallValue, long theDiscardedValue, List<String> theFailureValues) {
        this.theOptions = theOptionValues;
        this.theResults = theResultValues;
        this.theWallMillis = theWallValue;
        this.theDiscarded = theDiscardedValue;
        this.theWorkerFailures = theFailureValues;
    }

    public String render() {
        StringBuilder theOut = new StringBuilder();
        long theTotalCalls = 0L;
        long theTotalErrors = 0L;
        long theTotalLate = 0L;

        theOut.append(System.lineSeparator()).append("=== results ===").append(System.lineSeparator());
        for (Map.Entry<String, ToolSamples> theEntry : theResults.entrySet()) {
            ToolSamples theSamples = theEntry.getValue();
            theTotalCalls += theSamples.calls();
            theTotalErrors += theSamples.errors();
            theTotalLate += theSamples.lateStarts();
            renderTool(theOut, theEntry.getKey(), theSamples);
        }

        theOut.append(System.lineSeparator());
        theOut.append(String.format("total       : %,d calls, %,d errors%n",
                Long.valueOf(theTotalCalls), Long.valueOf(theTotalErrors)));
        if (theDiscarded > 0L) {
            theOut.append(String.format("warm-up     : %,d further calls made and discarded (%s)%n",
                    Long.valueOf(theDiscarded), duration(theOptions.warmupNanos() / 1_000_000L)));
        }
        double theAchieved = theTotalCalls * 1000.0 / Math.max(1L, theWallMillis);
        theOut.append(String.format("wall clock  : %s  (%,.1f calls/sec, %d thread(s))%n",
                duration(theWallMillis), Double.valueOf(theAchieved),
                Integer.valueOf(theOptions.threads())));
        if (theOptions.rate() > 0.0) {
            theOut.append(String.format("target rate : %,.1f calls/sec — achieved %,.1f%% of it%n",
                    Double.valueOf(theOptions.rate()),
                    Double.valueOf(theAchieved * 100.0 / theOptions.rate())));
            theOut.append(String.format("behind      : %,d of %,d calls started late%s%n",
                    Long.valueOf(theTotalLate), Long.valueOf(theTotalCalls),
                    theTotalLate > 0L ? " — read the SCHEDULED latency, not the service time" : ""));
        } else {
            theOut.append("target rate : none — flat out, so scheduled latency equals service time")
                    .append(System.lineSeparator());
        }
        for (String theFailure : theWorkerFailures) {
            theOut.append("WORKER FAILED: ").append(theFailure).append(System.lineSeparator());
        }
        return theOut.toString();
    }

    private void renderTool(StringBuilder theOut, String theTool, ToolSamples theSamples) {
        theOut.append(String.format("%-28s calls %,9d   errors %,d%n", theTool,
                Integer.valueOf(theSamples.calls()), Long.valueOf(theSamples.errors())));
        if (theSamples.calls() == 0) {
            return;
        }
        theOut.append(percentileLine("service ms", theSamples.serviceMicros()));
        if (theOptions.rate() > 0.0) {
            theOut.append(percentileLine("sched.  ms", theSamples.scheduledMicros()));
        }
        for (Map.Entry<String, Long> theError : theSamples.errorsByKind().entrySet()) {
            theOut.append(String.format("%-28s   %,9d x %s%n", "", theError.getValue(),
                    theError.getKey()));
        }
        renderUniqueness(theOut, theSamples);
    }

    private static String percentileLine(String theLabel, LatencyDigest theDigest) {
        return String.format("%-28s %s  min %s  p50 %s  p90 %s  p99 %s  p999 %s  max %s%n", "",
                theLabel, millis(theDigest.min()), millis(theDigest.percentile(0.50)),
                millis(theDigest.percentile(0.90)), millis(theDigest.percentile(0.99)),
                millis(theDigest.percentile(0.999)), millis(theDigest.max()));
    }

    /**
     * The {@code "check": "unique"} verdict.
     *
     * <p>This is the part that can tell a fast server from a broken one. A throughput figure alone
     * cannot: a server answering every call from a cache, or retrying one call and counting it
     * twice, posts an excellent number. Distinct values proves each call reached Oracle, and a
     * contiguous span proves none was lost on the way back.
     */
    private static void renderUniqueness(StringBuilder theOut, ToolSamples theSamples) {
        long[] theValues = theSamples.returnedValues().sortedCopy();
        if (theValues.length == 0) {
            if (theSamples.unparsedValues() > 0L) {
                theOut.append(String.format("%-28s unique  no numeric value in any of %,d answers"
                        + " — check not applicable%n", "", Long.valueOf(theSamples.unparsedValues())));
            }
            return;
        }
        long theDuplicates = 0L;
        for (int i = 1; i < theValues.length; i++) {
            if (theValues[i] == theValues[i - 1]) {
                theDuplicates++;
            }
        }
        long theSpan = theValues[theValues.length - 1] - theValues[0] + 1L;
        theOut.append(String.format("%-28s unique  %,d..%,d  distinct %,d  duplicates %,d  %s%n", "",
                Long.valueOf(theValues[0]), Long.valueOf(theValues[theValues.length - 1]),
                Long.valueOf(theValues.length - theDuplicates), Long.valueOf(theDuplicates),
                theDuplicates == 0L ? "(correct)" : "*** WRONG ***"));
        theOut.append(String.format("%-28s span    %,d values over %,d calls  %s%n", "",
                Long.valueOf(theSpan), Integer.valueOf(theValues.length),
                theSpan == theValues.length ? "(contiguous - none lost)" : "(gaps)"));
        if (theSamples.unparsedValues() > 0L) {
            theOut.append(String.format("%-28s        %,d answer(s) carried no number%n", "",
                    Long.valueOf(theSamples.unparsedValues())));
        }
    }

    /** The same figures as JSON, so two runs can be compared without re-reading a table. */
    public void writeJson(Path thePath) throws IOException {
        Map<String, Object> theRoot = new LinkedHashMap<String, Object>();
        theRoot.put("url", theOptions.url());
        theRoot.put("threads", Integer.valueOf(theOptions.threads()));
        theRoot.put("targetRatePerSecond", Double.valueOf(theOptions.rate()));
        theRoot.put("wallMillis", Long.valueOf(theWallMillis));
        theRoot.put("warmupDiscardedCalls", Long.valueOf(theDiscarded));
        theRoot.put("workerFailures", theWorkerFailures);

        long theTotalCalls = 0L;
        List<Map<String, Object>> theTools = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, ToolSamples> theEntry : theResults.entrySet()) {
            ToolSamples theSamples = theEntry.getValue();
            theTotalCalls += theSamples.calls();
            Map<String, Object> theRow = new LinkedHashMap<String, Object>();
            theRow.put("tool", theEntry.getKey());
            theRow.put("calls", Integer.valueOf(theSamples.calls()));
            theRow.put("errors", Long.valueOf(theSamples.errors()));
            theRow.put("lateStarts", Long.valueOf(theSamples.lateStarts()));
            theRow.put("serviceMicros", percentileMap(theSamples.serviceMicros()));
            theRow.put("scheduledMicros", percentileMap(theSamples.scheduledMicros()));
            theRow.put("errorsByKind", theSamples.errorsByKind());
            theTools.add(theRow);
        }
        theRoot.put("totalCalls", Long.valueOf(theTotalCalls));
        theRoot.put("achievedRatePerSecond",
                Double.valueOf(theTotalCalls * 1000.0 / Math.max(1L, theWallMillis)));
        theRoot.put("tools", theTools);

        McpJsonMapper theMapper =
                new JacksonMcpJsonMapper(tools.jackson.databind.json.JsonMapper.builder().build());
        Files.write(thePath, theMapper.writeValueAsString(theRoot).getBytes(StandardCharsets.UTF_8));
    }

    private static Map<String, Object> percentileMap(LatencyDigest theDigest) {
        Map<String, Object> theMap = new LinkedHashMap<String, Object>();
        theMap.put("count", Integer.valueOf(theDigest.size()));
        theMap.put("mean", Double.valueOf(theDigest.mean()));
        theMap.put("min", Long.valueOf(theDigest.min()));
        theMap.put("p50", Long.valueOf(theDigest.percentile(0.50)));
        theMap.put("p90", Long.valueOf(theDigest.percentile(0.90)));
        theMap.put("p99", Long.valueOf(theDigest.percentile(0.99)));
        theMap.put("p999", Long.valueOf(theDigest.percentile(0.999)));
        theMap.put("max", Long.valueOf(theDigest.max()));
        return theMap;
    }

    static String millis(long theMicros) {
        return theMicros < 0L ? "     -" : String.format("%.2f", Double.valueOf(theMicros / 1000.0));
    }

    static String duration(long theMillis) {
        long theSeconds = Math.max(0L, theMillis) / 1000L;
        return String.format("%d:%02d:%02d", Long.valueOf(theSeconds / 3600L),
                Long.valueOf((theSeconds % 3600L) / 60L), Long.valueOf(theSeconds % 60L));
    }
}
