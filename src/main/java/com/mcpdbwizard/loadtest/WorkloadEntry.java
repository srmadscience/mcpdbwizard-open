package com.mcpdbwizard.loadtest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One tool in a workload: what to call, with what arguments, how often, and what to check.
 *
 * <h2>Why the arguments are templated</h2>
 *
 * <p>Calling one tool a hundred thousand times with the <em>same</em> bind values does not measure
 * the server. It measures Oracle's cursor cache and its buffer cache, both of which will answer the
 * second call and every call after it from memory. The resulting figure is real, repeatable and
 * describes a workload nobody has. A load tool that makes that easy to do by accident is worse than
 * no load tool, so three substitutions are built in:
 *
 * <ul>
 *   <li>{@code ${seq}} — the call's index in the run, counting from zero</li>
 *   <li>{@code ${thread}} — which worker made the call</li>
 *   <li>{@code ${random:a-b}} — a whole number in {@code [a, b]}, from a seeded generator so a run
 *       repeats</li>
 * </ul>
 *
 * <p>And deliberately no more. This is not a scripting language: anything that needs real data
 * needs a real fixture, and pretending otherwise in a template syntax would only hide that.
 *
 * <p><b>A value that is one token and nothing else keeps its type.</b> {@code "${seq}"} binds the
 * number 42, not the string {@code "42"} — a numeric MCP parameter given a string is a schema
 * violation, and the tool would spend the run measuring the rejection path.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class WorkloadEntry {

    /** {@code check} value asking that every answer differ. */
    public static final String CHECK_UNIQUE = "unique";

    private static final Pattern TOKEN =
            Pattern.compile("\\$\\{(seq|thread|random:(-?\\d+)\\s*-\\s*(-?\\d+))\\}");

    private final String theTool;

    private final Map<String, Object> theArgs;

    private final int theWeight;

    private final String theCheck;

    public WorkloadEntry(String theToolName, Map<String, Object> theArgValues, int theWeightValue,
            String theCheckValue) {
        if (theToolName == null || theToolName.trim().isEmpty()) {
            throw new IllegalArgumentException("a workload entry needs a \"tool\" name");
        }
        if (theWeightValue < 1) {
            throw new IllegalArgumentException("weight for '" + theToolName
                    + "' must be at least 1 — got " + theWeightValue);
        }
        if (theCheckValue != null && !CHECK_UNIQUE.equals(theCheckValue)) {
            throw new IllegalArgumentException("unknown check '" + theCheckValue + "' for '"
                    + theToolName + "'; the only one is \"" + CHECK_UNIQUE + "\"");
        }
        this.theTool = theToolName.trim();
        this.theArgs = theArgValues == null ? Map.of() : Map.copyOf(theArgValues);
        this.theWeight = theWeightValue;
        this.theCheck = theCheckValue;
    }

    /** A tool called with no arguments and no check — the {@code --tools a,b,c} case. */
    public static WorkloadEntry ofName(String theToolName) {
        return new WorkloadEntry(theToolName, Map.of(), 1, null);
    }

    public String tool() {
        return theTool;
    }

    public int weight() {
        return theWeight;
    }

    public boolean wantsUniqueCheck() {
        return CHECK_UNIQUE.equals(theCheck);
    }

    /** The declared arguments, tokens unsubstituted. */
    public Map<String, Object> rawArgs() {
        return theArgs;
    }

    /**
     * The arguments for one call, with the tokens filled in.
     *
     * @param theSeq    this call's index in the run
     * @param theThread the worker making it
     * @param theRandom that worker's generator — one per thread, so no lock is taken on the hot path
     */
    public Map<String, Object> argsFor(long theSeq, int theThread, Random theRandom) {
        if (theArgs.isEmpty()) {
            return Map.of();
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> theRendered =
                (Map<String, Object>) substitute(theArgs, theSeq, theThread, theRandom);
        return theRendered;
    }

    /** Walks maps and lists so a token works wherever it is written, including inside a record. */
    private static Object substitute(Object theValue, long theSeq, int theThread, Random theRandom) {
        if (theValue instanceof String) {
            return substituteString((String) theValue, theSeq, theThread, theRandom);
        }
        if (theValue instanceof Map) {
            Map<?, ?> theSource = (Map<?, ?>) theValue;
            Map<String, Object> theCopy = new LinkedHashMap<String, Object>();
            for (Map.Entry<?, ?> theEntry : theSource.entrySet()) {
                theCopy.put(String.valueOf(theEntry.getKey()),
                        substitute(theEntry.getValue(), theSeq, theThread, theRandom));
            }
            return theCopy;
        }
        if (theValue instanceof List) {
            List<Object> theCopy = new ArrayList<Object>();
            for (Object theItem : (List<?>) theValue) {
                theCopy.add(substitute(theItem, theSeq, theThread, theRandom));
            }
            return theCopy;
        }
        return theValue;
    }

    private static Object substituteString(String theText, long theSeq, int theThread,
            Random theRandom) {
        Matcher theMatcher = TOKEN.matcher(theText);
        if (!theMatcher.find()) {
            return theText;
        }
        // A value that is EXACTLY one token keeps its numeric type; see the class comment.
        if (theMatcher.start() == 0 && theMatcher.end() == theText.length()) {
            return valueOf(theMatcher, theSeq, theThread, theRandom);
        }
        StringBuilder theResult = new StringBuilder();
        theMatcher.reset();
        while (theMatcher.find()) {
            theMatcher.appendReplacement(theResult, Matcher.quoteReplacement(
                    String.valueOf(valueOf(theMatcher, theSeq, theThread, theRandom))));
        }
        theMatcher.appendTail(theResult);
        return theResult.toString();
    }

    private static Object valueOf(Matcher theMatcher, long theSeq, int theThread, Random theRandom) {
        String theToken = theMatcher.group(1);
        if ("seq".equals(theToken)) {
            return Long.valueOf(theSeq);
        }
        if ("thread".equals(theToken)) {
            return Integer.valueOf(theThread);
        }
        long theLow = Long.parseLong(theMatcher.group(2));
        long theHigh = Long.parseLong(theMatcher.group(3));
        if (theHigh < theLow) {
            long theSwap = theLow;
            theLow = theHigh;
            theHigh = theSwap;
        }
        // Inclusive of both ends: "1-1" must be a usable way to say "always 1".
        return Long.valueOf(theLow + (long) (theRandom.nextDouble() * (theHigh - theLow + 1)));
    }
}
