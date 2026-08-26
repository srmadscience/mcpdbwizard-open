package com.mcpdbwizard.loadtest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The workload file: parsing, proportions, and the templating that stops a run measuring a cache.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class WorkloadTest {

    private static final Random FIXED = new Random(1L);

    @Test
    void anObjectWithAToolsArrayParses() throws IOException {
        Workload theWorkload = Workload.fromJson("{ \"tools\": ["
                + " { \"tool\": \"a\", \"weight\": 2 },"
                + " { \"tool\": \"b\", \"args\": { \"p\": 1 }, \"check\": \"unique\" } ] }");
        assertEquals(2, theWorkload.entries().size());
        assertEquals(Set.of("a", "b"), new HashSet<String>(theWorkload.toolNames()));
        assertTrue(theWorkload.entries().get(1).wantsUniqueCheck());
        assertFalse(theWorkload.entries().get(0).wantsUniqueCheck());
    }

    /** A hand-edited file usually starts life as a bare array, so both shapes are accepted. */
    @Test
    void aBareArrayParsesToo() throws IOException {
        assertEquals(1, Workload.fromJson("[ { \"tool\": \"a\" } ]").entries().size());
    }

    @Test
    void weightDefaultsToOne() throws IOException {
        assertEquals(1, Workload.fromJson("[ { \"tool\": \"a\" } ]").entries().get(0).weight());
    }

    /**
     * Rotation, not dice. Over a short run the proportions are exact instead of approximately
     * right, which is what makes two runs comparable without an argument about sampling noise.
     */
    @Test
    void weightsAreHonouredExactlyOverOneCycle() throws IOException {
        Workload theWorkload = Workload.fromJson("[ { \"tool\": \"heavy\", \"weight\": 5 },"
                + " { \"tool\": \"light\", \"weight\": 1 } ]");
        int theHeavy = 0;
        for (long i = 0; i < 600L; i++) {
            if ("heavy".equals(theWorkload.pick(i).tool())) {
                theHeavy++;
            }
        }
        assertEquals(500, theHeavy);
    }

    @Test
    void aSingleToolIsPickedForEverySlotIncludingLargeIndexes() {
        Workload theWorkload = Workload.ofToolNames(List.of("only"));
        assertEquals("only", theWorkload.pick(0L).tool());
        assertEquals("only", theWorkload.pick(Integer.MAX_VALUE + 1L).tool());
    }

    @Test
    void toolsGivenOnTheCommandLineBecomeAnUnweightedRotation() {
        Workload theWorkload = Workload.ofToolNames(List.of("a", "b"));
        assertEquals("a", theWorkload.pick(0L).tool());
        assertEquals("b", theWorkload.pick(1L).tool());
        assertEquals("a", theWorkload.pick(2L).tool());
    }

    // ---- templating -----------------------------------------------------------------------

    /**
     * A value that is exactly one numeric token keeps its type. Binding the STRING "42" to a
     * numeric MCP parameter is a schema violation, and the run would then spend its whole budget
     * measuring how fast the server can reject it.
     */
    @Test
    void aValueThatIsOneTokenKeepsItsNumericType() {
        WorkloadEntry theEntry = new WorkloadEntry("t", Map.of("p", "${seq}"), 1, null);
        Object theValue = theEntry.argsFor(42L, 0, FIXED).get("p");
        assertInstanceOf(Long.class, theValue);
        assertEquals(42L, theValue);
    }

    @Test
    void aTokenInsideTextIsSubstitutedAsText() {
        WorkloadEntry theEntry =
                new WorkloadEntry("t", Map.of("note", "load ${seq} on ${thread}"), 1, null);
        assertEquals("load 7 on 3", theEntry.argsFor(7L, 3, FIXED).get("note"));
    }

    @Test
    void aStringWithNoTokenIsLeftExactlyAlone() {
        WorkloadEntry theEntry = new WorkloadEntry("t", Map.of("p", "SMITH"), 1, null);
        assertEquals("SMITH", theEntry.argsFor(1L, 0, FIXED).get("p"));
    }

    @Test
    void randomStaysInsideItsInclusiveRange() {
        WorkloadEntry theEntry =
                new WorkloadEntry("t", Map.of("p", "${random:7369-7999}"), 1, null);
        Random theRandom = new Random(99L);
        Set<Object> theSeen = new HashSet<Object>();
        for (int i = 0; i < 20000; i++) {
            long theValue = ((Long) theEntry.argsFor(i, 0, theRandom).get("p")).longValue();
            assertTrue(theValue >= 7369L && theValue <= 7999L, "out of range: " + theValue);
            theSeen.add(Long.valueOf(theValue));
        }
        // It must actually vary; a "random" that returns one value is the cache-measuring bug this
        // whole feature exists to prevent.
        assertTrue(theSeen.size() > 500, "only " + theSeen.size() + " distinct values");
    }

    @Test
    void aSingleValueRangeIsAllowed() {
        WorkloadEntry theEntry = new WorkloadEntry("t", Map.of("p", "${random:5-5}"), 1, null);
        assertEquals(5L, theEntry.argsFor(0L, 0, FIXED).get("p"));
    }

    @Test
    void theSameSeedGivesTheSameRun() {
        WorkloadEntry theEntry = new WorkloadEntry("t", Map.of("p", "${random:1-1000000}"), 1, null);
        List<Object> theFirst = new ArrayList<Object>();
        List<Object> theSecond = new ArrayList<Object>();
        Random theA = new Random(4242L);
        Random theB = new Random(4242L);
        for (int i = 0; i < 50; i++) {
            theFirst.add(theEntry.argsFor(i, 0, theA).get("p"));
            theSecond.add(theEntry.argsFor(i, 0, theB).get("p"));
        }
        assertEquals(theFirst, theSecond);
    }

    /** Records cross as nested objects, so a token has to work at any depth. */
    @Test
    void tokensAreSubstitutedInsideNestedObjectsAndArrays() {
        WorkloadEntry theEntry = new WorkloadEntry("t",
                Map.of("rec", Map.of("id", "${seq}", "name", "row ${seq}"),
                       "list", List.of("${seq}", "fixed")), 1, null);
        Map<String, Object> theArgs = theEntry.argsFor(9L, 0, FIXED);
        @SuppressWarnings("unchecked")
        Map<String, Object> theRecord = (Map<String, Object>) theArgs.get("rec");
        assertEquals(9L, theRecord.get("id"));
        assertEquals("row 9", theRecord.get("name"));
        assertEquals(List.of(9L, "fixed"), theArgs.get("list"));
    }

    // ---- rejections -----------------------------------------------------------------------

    @Test
    void anEntryWithNoToolNameIsRejected() {
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> Workload.fromJson("[ { \"weight\": 2 } ]")).getMessage().contains("\"tool\""));
    }

    @Test
    void anObjectWithoutAToolsArrayIsRejected() {
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> Workload.fromJson("{ \"calls\": 5 }")).getMessage().contains("\"tools\""));
    }

    @Test
    void argsThatAreNotAnObjectAreRejected() {
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> Workload.fromJson("[ { \"tool\": \"a\", \"args\": [1,2] } ]"))
                .getMessage().contains("args"));
    }

    /** A misspelled check must not be accepted and then silently never run. */
    @Test
    void anUnknownCheckIsRejectedRatherThanIgnored() {
        String theMessage = assertThrows(IllegalArgumentException.class,
                () -> Workload.fromJson("[ { \"tool\": \"a\", \"check\": \"uniqe\" } ]"))
                .getMessage();
        assertTrue(theMessage.contains("uniqe"), theMessage);
        assertTrue(theMessage.contains("unique"), theMessage);
    }

    @Test
    void aWeightBelowOneIsRejected() {
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> Workload.fromJson("[ { \"tool\": \"a\", \"weight\": 0 } ]"))
                .getMessage().contains("at least 1"));
    }

    @Test
    void anEmptyWorkloadIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Workload.fromJson("[]"));
    }
}
