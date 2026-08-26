package com.mcpdbwizard.loadtest;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the report says, which is the whole product of a run.
 *
 * <p>Three of these pin claims rather than formatting: that a paced run shows the scheduled latency
 * beside the service time, that errors are grouped rather than tallied, and that a duplicate answer
 * is called out in a way nobody scrolls past.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class LoadReportTest {

    private static LoadOptions options(String... theExtra) {
        String[] theBase = {"--url", "http://127.0.0.1:8090/mcp", "--tools", "t", "--for", "60s"};
        String[] theArgs = new String[theBase.length + theExtra.length];
        System.arraycopy(theBase, 0, theArgs, 0, theBase.length);
        System.arraycopy(theExtra, 0, theArgs, theBase.length, theExtra.length);
        return LoadOptions.parse(theArgs);
    }

    private static Map<String, ToolSamples> oneTool(ToolSamples theSamples) {
        Map<String, ToolSamples> theResults = new LinkedHashMap<String, ToolSamples>();
        theResults.put("t", theSamples);
        return theResults;
    }

    private static ToolSamples tenGoodCalls() {
        ToolSamples theSamples = new ToolSamples();
        for (int i = 1; i <= 10; i++) {
            theSamples.recordSuccess(i * 1000L, i * 1000L, false);
        }
        return theSamples;
    }

    @Test
    void anUnpacedRunSaysWhyThereIsOnlyOneLatency() {
        String theReport = new LoadReport(options(), oneTool(tenGoodCalls()), 10_000L, 0L,
                List.of()).render();
        assertTrue(theReport.contains("service ms"), theReport);
        assertFalse(theReport.contains("sched.  ms"), theReport);
        assertTrue(theReport.contains("flat out"), theReport);
    }

    /**
     * The coordinated-omission guard. Under a target rate the run cannot hold, service time stays
     * flat while the queue grows; a report showing only the first would call a saturated server fast.
     */
    @Test
    void aPacedRunShowsScheduledLatencyAndHowFarBehindItFell() {
        ToolSamples theSamples = new ToolSamples();
        for (int i = 0; i < 100; i++) {
            theSamples.recordSuccess(5_000L, 5_000L + i * 1_000_000L, true);
        }
        String theReport = new LoadReport(options("--rate", "500"), oneTool(theSamples),
                10_000L, 0L, List.of()).render();
        assertTrue(theReport.contains("service ms"), theReport);
        assertTrue(theReport.contains("sched.  ms"), theReport);
        assertTrue(theReport.contains("target rate"), theReport);
        assertTrue(theReport.contains("100 of 100 calls started late"), theReport);
        assertTrue(theReport.contains("read the SCHEDULED latency"), theReport);
    }

    @Test
    void errorsAreGroupedByKindWithACountEach() {
        ToolSamples theSamples = new ToolSamples();
        for (int i = 0; i < 7; i++) {
            theSamples.recordError("SQLException: ORA-12516 listener could not hand off",
                    1000L, 1000L, false);
        }
        theSamples.recordError("McpError: connection reset", 1000L, 1000L, false);
        String theReport = new LoadReport(options(), oneTool(theSamples), 1000L, 0L, List.of())
                .render();
        assertTrue(theReport.contains("7 x SQLException: ORA-12516"), theReport);
        assertTrue(theReport.contains("1 x McpError: connection reset"), theReport);
        assertTrue(theReport.contains("errors 8"), theReport);
    }

    @Test
    void aCleanUniquenessCheckSaysCorrectAndContiguous() {
        ToolSamples theSamples = tenGoodCalls();
        for (long i = 500L; i < 510L; i++) {
            theSamples.recordReturnedValue(i);
        }
        String theReport = new LoadReport(options(), oneTool(theSamples), 1000L, 0L, List.of())
                .render();
        assertTrue(theReport.contains("(correct)"), theReport);
        assertTrue(theReport.contains("(contiguous - none lost)"), theReport);
    }

    /** A repeated answer means the calls did not all reach Oracle; it must be impossible to miss. */
    @Test
    void aDuplicateAnswerIsCalledOutLoudly() {
        ToolSamples theSamples = tenGoodCalls();
        for (int i = 0; i < 10; i++) {
            theSamples.recordReturnedValue(500L);
        }
        String theReport = new LoadReport(options(), oneTool(theSamples), 1000L, 0L, List.of())
                .render();
        assertTrue(theReport.contains("*** WRONG ***"), theReport);
        assertTrue(theReport.contains("(gaps)"), theReport);
    }

    @Test
    void aCheckThatCouldNotBeAppliedSaysSoRatherThanPassingSilently() {
        ToolSamples theSamples = tenGoodCalls();
        for (int i = 0; i < 10; i++) {
            theSamples.recordReturnedValue(-1L);
        }
        assertTrue(new LoadReport(options(), oneTool(theSamples), 1000L, 0L, List.of()).render()
                .contains("check not applicable"));
    }

    @Test
    void discardedWarmupCallsAreReportedRatherThanForgotten() {
        String theReport = new LoadReport(options("--warmup", "30s"), oneTool(tenGoodCalls()),
                60_000L, 4_321L, List.of()).render();
        assertTrue(theReport.contains("4,321 further calls made and discarded"), theReport);
    }

    @Test
    void aWorkerThatNeverConnectedIsShoutedAboutRatherThanAveragedIn() {
        String theReport = new LoadReport(options(), oneTool(tenGoodCalls()), 1000L, 0L,
                List.of("McpError: 401 Unauthorized")).render();
        assertTrue(theReport.contains("WORKER FAILED: McpError: 401 Unauthorized"), theReport);
    }

    // ---- the helpers the worker uses to classify an answer ---------------------------------

    @Test
    void theFirstNumberIsPulledOutOfWhateverTheToolReturned() {
        assertEquals(1234L, LoadWorker.firstNumberIn("{\"nextval\":1234}"));
        assertEquals(1234L, LoadWorker.firstNumberIn("1234"));
        assertEquals(-1L, LoadWorker.firstNumberIn("no digits here"));
        assertEquals(-1L, LoadWorker.firstNumberIn(null));
    }

    /** A 25-digit run of characters is not a long; it must not blow up the middle of a run. */
    @Test
    void anImplausiblyLongNumberIsTreatedAsUnparsedRatherThanOverflowing() {
        assertEquals(-1L, LoadWorker.firstNumberIn("1234567890123456789012345"));
    }

    /**
     * Grouping only works if the varying tail is cut. Oracle messages carry a line number or a
     * cursor id, so untrimmed text yields twelve thousand groups of one and the summary says nothing.
     */
    @Test
    void errorMessagesAreTrimmedToTheFirstLineAndCapped() {
        assertEquals("ORA-12516: TNS:listener could not find available handler",
                LoadWorker.summarise("ORA-12516: TNS:listener could not find available handler\n"
                        + "\tat oracle.jdbc.driver.T4CTTIoer11.processError(T4CTTIoer11.java:513)"));
        assertEquals(120, LoadWorker.summarise("x".repeat(400)).length());
        assertEquals("(no message)", LoadWorker.summarise(null));
    }
}
