package com.mcpdbwizard.loadtest;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the server publishes, and the two things a load run needs from it.
 *
 * <h2>1. Validation, before a single call is made</h2>
 *
 * <p>{@link #missing} names any tool a run intends to call that the server does not publish, and
 * {@link McpLoad} refuses to start when that list is non-empty. A mistyped tool name otherwise
 * produces a run that completes with a 100% error rate — which reads as a broken server, not as a
 * broken command line. This repository has been caught by that shape before: a missing variable made
 * a generator report "GENERATION FAILED" for all 41 propfiles in twelve seconds, and the message
 * named the wrong thing.
 *
 * <h2>2. Making a workload file writable by hand</h2>
 *
 * <p>{@link #render} prints every tool with its parameters and a ready-made workload entry, so
 * authoring a workload is copy-and-paste. Without it, "call any published tool" means reading the
 * generated server's source to find out what a parameter is called.
 *
 * <p><b>The listing is paged.</b> {@code tools/list} returns a cursor when there are more, and a
 * generated server for a real schema publishes hundreds of tools — one per table access method, per
 * PL/SQL routine, per curated statement. Reading only the first page silently under-reports, and the
 * missing-tool check would then reject tools that exist.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class ToolCatalogue {

    /** Guards against a server that returns the same cursor forever. */
    private static final int MAX_PAGES = 500;

    /**
     * Shared leading characters that make one name a plausible correction for another.
     *
     * <p>Four is short enough to catch a typo and long enough that a generated server's shared
     * prefixes ({@code get_}, {@code insert_}) do not suggest every tool it publishes.
     */
    private static final int MIN_SUGGESTION_PREFIX = 5;

    private final List<McpSchema.Tool> theTools;

    public ToolCatalogue(List<McpSchema.Tool> theToolList) {
        this.theTools = List.copyOf(theToolList);
    }

    /** Read every page of {@code tools/list}. */
    public static ToolCatalogue from(McpSyncClient theClient) {
        List<McpSchema.Tool> theAll = new ArrayList<McpSchema.Tool>();
        String theCursor = null;
        for (int thePage = 0; thePage < MAX_PAGES; thePage++) {
            McpSchema.ListToolsResult theResult =
                    theCursor == null ? theClient.listTools() : theClient.listTools(theCursor);
            theAll.addAll(theResult.tools());
            theCursor = theResult.nextCursor();
            if (theCursor == null || theCursor.isEmpty()) {
                break;
            }
        }
        return new ToolCatalogue(theAll);
    }

    public List<McpSchema.Tool> tools() {
        return theTools;
    }

    public int size() {
        return theTools.size();
    }

    /** Those of {@code theWanted} the server does not publish, in the order asked for. */
    public List<String> missing(Collection<String> theWanted) {
        List<String> theMissing = new ArrayList<String>();
        for (String theName : theWanted) {
            boolean theFound = false;
            for (McpSchema.Tool theTool : theTools) {
                if (theTool.name().equals(theName)) {
                    theFound = true;
                    break;
                }
            }
            if (!theFound) {
                theMissing.add(theName);
            }
        }
        return theMissing;
    }

    /**
     * Tool names close enough to {@code theName} to be what was meant.
     *
     * <p>Substring either way catches "employee" for {@code get_employee}; a shared prefix catches
     * the commoner case of a typo near the end, where neither name contains the other —
     * {@code seq_nextvl} for {@code seq_nextval} shares nine characters and no substring at all.
     */
    public List<String> nearestTo(String theName) {
        List<String> theNear = new ArrayList<String>();
        String theLower = theName.toLowerCase();
        for (McpSchema.Tool theTool : theTools) {
            String theCandidate = theTool.name().toLowerCase();
            if (theCandidate.contains(theLower) || theLower.contains(theCandidate)
                    || commonPrefix(theCandidate, theLower) >= MIN_SUGGESTION_PREFIX) {
                theNear.add(theTool.name());
            }
            if (theNear.size() == 5) {
                break;
            }
        }
        return theNear;
    }

    private static int commonPrefix(String theFirst, String theSecond) {
        int theLimit = Math.min(theFirst.length(), theSecond.length());
        int i = 0;
        while (i < theLimit && theFirst.charAt(i) == theSecond.charAt(i)) {
            i++;
        }
        return i;
    }

    /** The {@code --list} output: every tool, its parameters, and a workload entry to copy. */
    public String render() {
        StringBuilder theOut = new StringBuilder();
        theOut.append(theTools.size()).append(" tool(s) published").append(System.lineSeparator());
        for (McpSchema.Tool theTool : theTools) {
            theOut.append(System.lineSeparator());
            theOut.append(theTool.name());
            if (theTool.title() != null && !theTool.title().isEmpty()) {
                theOut.append("  (").append(theTool.title()).append(')');
            }
            theOut.append(System.lineSeparator());
            if (theTool.description() != null && !theTool.description().isEmpty()) {
                theOut.append("    ").append(firstLine(theTool.description()))
                        .append(System.lineSeparator());
            }
            for (String theLine : parameterLines(theTool)) {
                theOut.append("    ").append(theLine).append(System.lineSeparator());
            }
            theOut.append("    workload: ").append(skeletonEntry(theTool))
                    .append(System.lineSeparator());
        }
        return theOut.toString();
    }

    /** One line per parameter: name, type, and whether the schema marks it required. */
    public static List<String> parameterLines(McpSchema.Tool theTool) {
        List<String> theLines = new ArrayList<String>();
        Map<String, Object> theProperties = propertiesOf(theTool);
        if (theProperties.isEmpty()) {
            theLines.add("(no parameters)");
            return theLines;
        }
        List<String> theRequired = requiredOf(theTool);
        for (Map.Entry<String, Object> theEntry : theProperties.entrySet()) {
            StringBuilder theLine = new StringBuilder(theEntry.getKey());
            theLine.append(" : ").append(typeOf(theEntry.getValue()));
            if (theRequired.contains(theEntry.getKey())) {
                theLine.append(" (required)");
            }
            String theDescription = descriptionOf(theEntry.getValue());
            if (theDescription != null) {
                theLine.append("  — ").append(firstLine(theDescription));
            }
            theLines.add(theLine.toString());
        }
        return theLines;
    }

    /**
     * A workload entry for this tool, as a single line of JSON.
     *
     * <p>Placeholders are type-appropriate but meaningless on purpose — {@code CHANGE_ME} rather
     * than a plausible-looking string. A value that looks real is a value nobody edits, and a lookup
     * that misses every row measures the not-found path at full speed while reporting excellent
     * latency.
     */
    public static String skeletonEntry(McpSchema.Tool theTool) {
        StringBuilder theEntry = new StringBuilder("{ \"tool\": \"").append(theTool.name())
                .append("\", \"weight\": 1");
        Map<String, Object> theProperties = propertiesOf(theTool);
        if (!theProperties.isEmpty()) {
            theEntry.append(", \"args\": {");
            boolean theFirstFlag = true;
            for (Map.Entry<String, Object> theEntry2 : theProperties.entrySet()) {
                if (!theFirstFlag) {
                    theEntry.append(',');
                }
                theFirstFlag = false;
                theEntry.append(" \"").append(theEntry2.getKey()).append("\": ")
                        .append(placeholderFor(typeOf(theEntry2.getValue())));
            }
            theEntry.append(" }");
        }
        return theEntry.append(" }").toString();
    }

    private static String placeholderFor(String theType) {
        switch (theType) {
            case "integer":
            case "number":
                return "0";
            case "boolean":
                return "false";
            case "array":
                return "[]";
            case "object":
                return "{}";
            default:
                return "\"CHANGE_ME\"";
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> propertiesOf(McpSchema.Tool theTool) {
        Map<String, Object> theSchema = theTool.inputSchema();
        if (theSchema == null) {
            return Map.of();
        }
        Object theProperties = theSchema.get("properties");
        if (!(theProperties instanceof Map)) {
            return Map.of();
        }
        return new LinkedHashMap<String, Object>((Map<String, Object>) theProperties);
    }

    private static List<String> requiredOf(McpSchema.Tool theTool) {
        Map<String, Object> theSchema = theTool.inputSchema();
        if (theSchema == null) {
            return List.of();
        }
        Object theRequired = theSchema.get("required");
        if (!(theRequired instanceof List)) {
            return List.of();
        }
        List<String> theNames = new ArrayList<String>();
        for (Object theItem : (List<?>) theRequired) {
            theNames.add(String.valueOf(theItem));
        }
        return theNames;
    }

    private static String typeOf(Object thePropertySchema) {
        if (thePropertySchema instanceof Map) {
            Object theType = ((Map<?, ?>) thePropertySchema).get("type");
            if (theType != null) {
                return String.valueOf(theType);
            }
        }
        return "string";
    }

    private static String descriptionOf(Object thePropertySchema) {
        if (thePropertySchema instanceof Map) {
            Object theDescription = ((Map<?, ?>) thePropertySchema).get("description");
            if (theDescription != null) {
                return String.valueOf(theDescription);
            }
        }
        return null;
    }

    private static String firstLine(String theText) {
        int theBreak = theText.indexOf('\n');
        String theLine = theBreak < 0 ? theText : theText.substring(0, theBreak);
        return theLine.length() > 140 ? theLine.substring(0, 137) + "..." : theLine.trim();
    }
}
