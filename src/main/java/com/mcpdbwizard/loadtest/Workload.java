package com.mcpdbwizard.loadtest;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The set of tool calls a run makes, and in what proportion.
 *
 * <p>Read from a JSON file so a mixed workload is data rather than code — the harness this replaces
 * had its two tools' arguments compiled in, which meant every new tool was a source edit and a
 * rebuild. The format is deliberately small:
 *
 * <pre>
 * { "tools": [
 *     { "tool": "job_id_nextval", "weight": 1, "check": "unique" },
 *     { "tool": "get_employee",   "weight": 5, "args": { "p_empno": "${random:7369-7999}" } },
 *     { "tool": "insert_audit",   "weight": 1, "args": { "p_note": "load ${seq}" } }
 * ] }
 * </pre>
 *
 * <p>A bare JSON array is accepted too, since that is what a hand-edited file usually starts as.
 * {@link ToolCatalogue#render} prints a ready-made entry for every published tool, so writing one of
 * these is copy-and-paste rather than authorship.
 *
 * <h2>Selection is by rotation, not by dice</h2>
 *
 * <p>{@link #pick} walks the weighted list in order rather than sampling it randomly. Over a short
 * run the proportions are then exact instead of approximately right, which matters when the point is
 * to compare two runs: a 5:1 mix that came out 5.3:1 costs an argument about whether the difference
 * is the server or the sampling.
 *
 * <p>Parsed with the MCP SDK's own JSON mapper — the same one the generator emits into every server
 * ({@code SAAdminWrangler}) — so this package adds nothing to any classpath it did not already need.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class Workload {

    private final List<WorkloadEntry> theEntries;

    /** Entry index per unit of weight, so selection is one array read. */
    private final int[] theSchedule;

    public Workload(List<WorkloadEntry> theEntryList) {
        if (theEntryList == null || theEntryList.isEmpty()) {
            throw new IllegalArgumentException("a workload needs at least one tool");
        }
        this.theEntries = List.copyOf(theEntryList);
        int theTotal = 0;
        for (WorkloadEntry theEntry : theEntries) {
            theTotal += theEntry.weight();
        }
        this.theSchedule = new int[theTotal];
        int theSlot = 0;
        for (int i = 0; i < theEntries.size(); i++) {
            for (int w = 0; w < theEntries.get(i).weight(); w++) {
                theSchedule[theSlot++] = i;
            }
        }
    }

    /** The {@code --tools a,b,c} case: each named once, in rotation, with no arguments. */
    public static Workload ofToolNames(List<String> theNames) {
        List<WorkloadEntry> theList = new ArrayList<WorkloadEntry>();
        for (String theName : theNames) {
            theList.add(WorkloadEntry.ofName(theName));
        }
        return new Workload(theList);
    }

    public static Workload fromFile(Path thePath) throws IOException {
        return fromJson(new String(Files.readAllBytes(thePath), StandardCharsets.UTF_8));
    }

    public static Workload fromJson(String theJson) throws IOException {
        McpJsonMapper theMapper =
                new JacksonMcpJsonMapper(tools.jackson.databind.json.JsonMapper.builder().build());
        Object theRoot = theMapper.readValue(theJson, Object.class);
        Object theToolList = theRoot;
        if (theRoot instanceof Map) {
            theToolList = ((Map<?, ?>) theRoot).get("tools");
            if (theToolList == null) {
                throw new IllegalArgumentException(
                        "a workload object needs a \"tools\" array; see --list for the shape");
            }
        }
        if (!(theToolList instanceof List)) {
            throw new IllegalArgumentException(
                    "a workload must be a JSON array of entries, or an object with a \"tools\" array");
        }
        List<WorkloadEntry> theEntries = new ArrayList<WorkloadEntry>();
        for (Object theItem : (List<?>) theToolList) {
            if (!(theItem instanceof Map)) {
                throw new IllegalArgumentException(
                        "each workload entry must be an object — got " + theItem);
            }
            Map<?, ?> theRow = (Map<?, ?>) theItem;
            Object theArgs = theRow.get("args");
            if (theArgs != null && !(theArgs instanceof Map)) {
                throw new IllegalArgumentException("\"args\" for '" + theRow.get("tool")
                        + "' must be an object of parameter name to value");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> theArgMap = (Map<String, Object>) theArgs;
            Object theWeight = theRow.get("weight");
            Object theCheck = theRow.get("check");
            theEntries.add(new WorkloadEntry(
                    theRow.get("tool") == null ? null : String.valueOf(theRow.get("tool")),
                    theArgMap,
                    theWeight == null ? 1 : ((Number) theWeight).intValue(),
                    theCheck == null ? null : String.valueOf(theCheck)));
        }
        return new Workload(theEntries);
    }

    /**
     * The entry for one call.
     *
     * @param theSeq the call's index in the run
     */
    public WorkloadEntry pick(long theSeq) {
        return theEntries.get(theSchedule[(int) Math.floorMod(theSeq, theSchedule.length)]);
    }

    public List<WorkloadEntry> entries() {
        return theEntries;
    }

    /** Distinct tool names, in the order first mentioned. */
    public Set<String> toolNames() {
        Set<String> theNames = new LinkedHashSet<String>();
        for (WorkloadEntry theEntry : theEntries) {
            theNames.add(theEntry.tool());
        }
        return theNames;
    }
}
