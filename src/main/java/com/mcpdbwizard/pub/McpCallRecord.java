package com.mcpdbwizard.pub;

import java.util.Map;

/**
 * One line per MCP tool call, for the operator: which tool ran, how it ended, and how long it took.
 *
 * <p>The generated server funnels every tool handler through a single {@code call(...)} helper, which
 * writes one of these afterwards. Before this existed the funnel logged nothing at all — a failure
 * went back to the caller as an error result and left no server-side trace, pool exhaustion included.
 *
 * <p>The format is a fixed prefix followed by a JSON object, so it can be grepped out of a mixed log
 * and parsed without one:
 *
 * <pre>MCP-CALL {"tool":"ob_gen_pkg_greet","outcome":"ok","ms":12,"args":["p_name"]}</pre>
 *
 * <h2>Argument names, never argument values</h2>
 *
 * <p>Only the argument <em>names</em> are recorded. This is the one place where an MCP server differs
 * sharply from an ordinary service: the values were chosen by a model rather than by a programmer, so
 * they are the most interesting thing here and also the most dangerous — they can carry anything the
 * caller put in front of the model, including personal data. Names give the shape of the call without
 * that risk. Recording values should be a separate, explicit opt-in, and is deliberately not built.
 *
 * <p>This is <b>not</b> an audit trail. It is a diagnostic record written to a log that the process
 * itself owns, so it has no durability, ordering or tamper-evidence guarantees. A real audit trail
 * needs a sink that survives the container and cannot be rewritten by the thing being audited.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class McpCallRecord {

    /** Marks the line, so it can be found in a log carrying everything else too. */
    public static final String PREFIX = "MCP-CALL";

    /** The tool ran and returned data. */
    public static final String OUTCOME_OK = "ok";

    /** The tool ran and found nothing — a normal answer, not a failure. */
    public static final String OUTCOME_NOT_FOUND = "not-found";

    /** Optimistic-lock clash: the document changed since it was read. */
    public static final String OUTCOME_DOCUMENT_CHANGED = "document-changed";

    /** Every pooled factory is checked out and the pool is at its ceiling. Load, not a fault. */
    public static final String OUTCOME_POOL_EXHAUSTED = "pool-exhausted";

    /** Refused by the configured rate limit — load shed by policy, not by saturation. */
    public static final String OUTCOME_RATE_LIMITED = "rate-limited";

    /** The database refused the work. */
    public static final String OUTCOME_DATABASE_ERROR = "database-error";

    /** Anything else, including a bug in the generated code. */
    public static final String OUTCOME_ERROR = "error";

    private McpCallRecord() {
    }

    /**
     * Build the record.
     *
     * @param theToolName      the MCP tool that was invoked
     * @param theArguments     the call's arguments; only the keys are used, and null is fine
     * @param theOutcome       one of the {@code OUTCOME_} constants
     * @param theMilliseconds  wall-clock duration of the call
     */
    public static String line(String theToolName, Map<String, Object> theArguments,
                              String theOutcome, long theMilliseconds) {
        StringBuilder theLine = new StringBuilder(PREFIX);
        theLine.append(" {\"tool\":\"").append(escape(theToolName));
        theLine.append("\",\"outcome\":\"").append(escape(theOutcome));
        theLine.append("\",\"ms\":").append(theMilliseconds);
        theLine.append(",\"args\":[");

        if (theArguments != null) {
            boolean theFirstFlag = true;
            for (String theName : theArguments.keySet()) {
                if (!theFirstFlag) {
                    theLine.append(',');
                }
                theLine.append('"').append(escape(theName)).append('"');
                theFirstFlag = false;
            }
        }
        return theLine.append("]}").toString();
    }

    /**
     * The argument NAMES of a call, comma-separated, with the values left out.
     *
     * <p>Shared with {@link McpProtocolLog} so the names-not-values rule above has one implementation
     * rather than two that can drift apart. The rule is the interesting part of both records, and it
     * is the kind of thing that gets relaxed by accident in a copy.
     *
     * @param theArguments the call's arguments; only the keys are read, and null gives ""
     */
    static String argumentNames(Map<String, Object> theArguments) {
        if (theArguments == null) {
            return "";
        }
        StringBuilder theNames = new StringBuilder();
        for (String theName : theArguments.keySet()) {
            if (theNames.length() > 0) {
                theNames.append(", ");
            }
            theNames.append(theName);
        }
        return theNames.toString();
    }

    /**
     * JSON string escaping.
     *
     * <p>Tool names are generated identifiers and safe, but argument names come off the wire and a
     * caller can put anything in them — including a quote or a newline, which would break the line
     * into something unparseable or let a crafted name forge a second record.
     */
    private static String escape(String theText) {
        if (theText == null) {
            return "";
        }

        StringBuilder theResult = new StringBuilder(theText.length());
        for (int i = 0; i < theText.length(); i++) {
            char theCharacter = theText.charAt(i);
            switch (theCharacter) {
                case '"':
                    theResult.append("\\\"");
                    break;
                case '\\':
                    theResult.append("\\\\");
                    break;
                case '\n':
                    theResult.append("\\n");
                    break;
                case '\r':
                    theResult.append("\\r");
                    break;
                case '\t':
                    theResult.append("\\t");
                    break;
                default:
                    if (theCharacter < 0x20) {
                        theResult.append(String.format("\\u%04x", (int) theCharacter));
                    } else {
                        theResult.append(theCharacter);
                    }
                    break;
            }
        }
        return theResult.toString();
    }
}
