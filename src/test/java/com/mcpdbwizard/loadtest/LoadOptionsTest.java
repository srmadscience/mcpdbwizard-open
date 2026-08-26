package com.mcpdbwizard.loadtest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Argument handling, including every rejection.
 *
 * <p>The rejections matter more than the acceptances. A load run that starts on a bad flag makes a
 * hundred thousand failing calls and then reports a 100% error rate, which is indistinguishable from
 * a sick server — this repository has watched a missing variable make a generator claim it had
 * failed on all 41 configs in twelve seconds, with a message naming the wrong thing entirely.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class LoadOptionsTest {

    private static LoadOptions parse(String... theArgs) {
        return LoadOptions.parse(theArgs);
    }

    private static String rejectionOf(String... theArgs) {
        return assertThrows(IllegalArgumentException.class, () -> LoadOptions.parse(theArgs))
                .getMessage();
    }

    @Test
    void aMinimalRunParses() {
        LoadOptions theOptions = parse("--url", "http://127.0.0.1:8090/mcp",
                "--tools", "job_id_nextval", "--for", "60s");
        assertEquals("http://127.0.0.1:8090/mcp", theOptions.url());
        assertEquals(List.of("job_id_nextval"), theOptions.toolNames());
        assertEquals(60_000_000_000L, theOptions.durationNanos());
        assertEquals(1, theOptions.threads());
        assertEquals(0.0, theOptions.rate());
        assertNull(theOptions.token());
    }

    /**
     * The one flag that reaches both surfaces. A generated server is a bare {@code /mcp}; the web
     * proxy is {@code /mcp/{config}}, and the SDK's transport wants those split.
     */
    @Test
    void theUrlSplitsIntoBaseAndEndpointForEitherTarget() {
        LoadOptions theDirect = parse("--url", "http://127.0.0.1:8090/mcp", "--list");
        assertEquals("http://127.0.0.1:8090", theDirect.baseUrl());
        assertEquals("/mcp", theDirect.endpointPath());

        LoadOptions theProxy = parse("--url", "https://wizard.example.com/mcp/payroll", "--list");
        assertEquals("https://wizard.example.com", theProxy.baseUrl());
        assertEquals("/mcp/payroll", theProxy.endpointPath());
    }

    @Test
    void aUrlWithNoPathStillTargetsTheDefaultEndpoint() {
        assertEquals("/mcp", parse("--url", "http://localhost:8090", "--list").endpointPath());
    }

    @Test
    void durationsAcceptTheUnitsPeopleType() {
        assertEquals(500_000_000L, LoadOptions.parseDuration("500ms", "--for"));
        assertEquals(30_000_000_000L, LoadOptions.parseDuration("30s", "--for"));
        assertEquals(300_000_000_000L, LoadOptions.parseDuration("5m", "--for"));
        assertEquals(7_200_000_000_000L, LoadOptions.parseDuration("2h", "--for"));
    }

    /**
     * A bare number is SECONDS. Reading it as milliseconds would turn a "300" meant as five minutes
     * into a run that ends before the JVM has finished warming up — and it would report a number.
     */
    @Test
    void aBareNumberIsSeconds() {
        assertEquals(300_000_000_000L, LoadOptions.parseDuration("300", "--for"));
    }

    @Test
    void aDurationThatIsNotOneSaysSoInsteadOfThrowingNumberFormatException() {
        String theMessage = rejectionOf("--url", "http://h:1/mcp", "--tools", "t",
                "--for", "five minutes");
        assertTrue(theMessage.contains("--for"), theMessage);
        assertTrue(theMessage.contains("five minutes"), theMessage);
    }

    @Test
    void aUrlIsRequired() {
        assertTrue(rejectionOf("--tools", "t", "--for", "60s").contains("--url is required"));
    }

    @Test
    void aUrlThatIsNotOneIsRejectedBeforeAnythingConnects() {
        assertTrue(rejectionOf("--url", "127.0.0.1:8090", "--tools", "t", "--for", "60s")
                .contains("not a usable URL"));
    }

    @Test
    void aRunNeedsABudget() {
        String theMessage = rejectionOf("--url", "http://h:1/mcp", "--tools", "t");
        assertTrue(theMessage.contains("--for"), theMessage);
        assertTrue(theMessage.contains("--calls"), theMessage);
    }

    @Test
    void bothBudgetsTogetherAreAllowedAndMeanWhicheverComesFirst() {
        LoadOptions theOptions = parse("--url", "http://h:1/mcp", "--tools", "t",
                "--calls", "100000000", "--for", "30s");
        assertEquals(100_000_000L, theOptions.callBudget());
        assertEquals(30_000_000_000L, theOptions.durationNanos());
    }

    @Test
    void aRunNeedsSomethingToCall() {
        String theMessage = rejectionOf("--url", "http://h:1/mcp", "--for", "60s");
        assertTrue(theMessage.contains("--tools"), theMessage);
        assertTrue(theMessage.contains("--list"), theMessage);
    }

    /** {@code --list} needs a URL and nothing else; it is the first thing anyone runs. */
    @Test
    void listingNeedsNoBudgetAndNoTools() {
        LoadOptions theOptions = parse("--url", "http://h:1/mcp", "--list");
        assertTrue(theOptions.isListOnly());
        assertEquals(0L, theOptions.durationNanos());
    }

    @Test
    void aWarmupThatSwallowsTheWholeRunIsRejected() {
        String theMessage = rejectionOf("--url", "http://h:1/mcp", "--tools", "t",
                "--for", "30s", "--warmup", "30s");
        assertTrue(theMessage.contains("--warmup"), theMessage);
    }

    @Test
    void aRateMustBeAboveZeroAndOmittingItMeansFlatOut() {
        assertEquals(200.0, parse("--url", "http://h:1/mcp", "--tools", "t", "--for", "1s",
                "--rate", "200").rate());
        String theMessage = rejectionOf("--url", "http://h:1/mcp", "--tools", "t",
                "--for", "1s", "--rate", "0");
        assertTrue(theMessage.contains("omit it entirely"), theMessage);
    }

    @Test
    void threadsAndCallsMustBePositiveWholeNumbers() {
        assertTrue(rejectionOf("--url", "http://h:1/mcp", "--tools", "t", "--for", "1s",
                "--threads", "0").contains("at least 1"));
        assertTrue(rejectionOf("--url", "http://h:1/mcp", "--tools", "t",
                "--calls", "lots").contains("whole number"));
    }

    @Test
    void aCallCountMayCarrySeparators() {
        assertEquals(290_000L, parse("--url", "http://h:1/mcp", "--tools", "t",
                "--calls", "290,000").callBudget());
    }

    @Test
    void anUnknownFlagIsNamedRatherThanIgnored() {
        String theMessage = rejectionOf("--url", "http://h:1/mcp", "--threds", "8");
        assertTrue(theMessage.contains("--threds"), theMessage);
        assertTrue(theMessage.contains("--help"), theMessage);
    }

    @Test
    void aFlagMissingItsValueSaysWhichOne() {
        assertTrue(rejectionOf("--url").contains("--url needs a value"));
    }

    @Test
    void toolListsAreSplitAndTrimmed() {
        assertEquals(List.of("a", "b", "c"), parse("--url", "http://h:1/mcp",
                "--tools", " a , b ,, c ", "--for", "1s").toolNames());
    }

    @Test
    void helpNeedsNothingElseToBeValid() {
        LoadOptions theOptions = parse("--help");
        assertTrue(theOptions.isHelp());
        assertFalse(theOptions.isListOnly());
    }

    /** The usage text is what a mistyped command line sends people to, so it must say the basics. */
    @Test
    void theUsageTextNamesEveryFlagThatChangesTheMeasurement() {
        String theUsage = LoadOptions.usage();
        for (String theFlag : List.of("--url", "--token", "--list", "--tools", "--workload",
                "--for", "--calls", "--threads", "--rate", "--warmup", "--timeout", "--out")) {
            assertTrue(theUsage.contains(theFlag), "usage never mentions " + theFlag);
        }
    }
}
