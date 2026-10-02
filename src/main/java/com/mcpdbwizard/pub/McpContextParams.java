package com.mcpdbwizard.pub;

import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The rules for MCP context parameters: names declared in a config that a client supplies on the
 * connection URL ({@code /mcp/<owner>/<config>?CUSTOMER_ID=42}), or under stdio as
 * {@code MCP_CONTEXT_<NAME>} environment variables, and that the generated server places in the
 * Oracle application context {@value #ORACLE_NAMESPACE} for every tool call — so PL/SQL and curated
 * SQL can read {@code SYS_CONTEXT('MCP','CUSTOMER_ID')}.
 *
 * <p><b>What this protects against is the agent, not the person.</b> The URL never enters the
 * model's context and no tool can change it, so an agent cannot choose another customer. Anyone who
 * can edit the client's configuration can — an accepted risk, and why a value here must never be a
 * secret. See {@code app/docs/mcp-per-caller-scoping-plan.md} §0.
 *
 * <p>Lives in the runtime library, not the config model, because both ends need the same answer:
 * the config editor refuses a name the generated server could never set, and the generated server
 * refuses a value Oracle would reject.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class McpContextParams {

    /** The config key family: {@code MCP_CONTEXT_PARAM_<i>=<NAME>}. */
    public static final String PB2_KEY_PREFIX = "MCP_CONTEXT_PARAM_";

    /** The stdio source: {@code MCP_CONTEXT_<NAME>=<value>}. */
    public static final String ENV_VARIABLE_PREFIX = "MCP_CONTEXT_";

    /** The Oracle application context namespace the values are set in. */
    public static final String ORACLE_NAMESPACE = "MCP";

    /**
     * The package {@code app/db/mcp-context/install.sql} creates, and the only code Oracle lets
     * write the {@value #ORACLE_NAMESPACE} namespace.
     */
    public static final String ORACLE_PACKAGE = "MCPDBWIZARD.MCPDBWIZARD_CTX";

    /**
     * The key a generated server's HTTP transport files the parsed query string under, in the
     * per-request {@code McpTransportContext} its tool handlers read back.
     */
    public static final String TRANSPORT_CONTEXT_KEY = "com.mcpdbwizard.mcp-context";

    /**
     * The values one request (or, under stdio, the whole process) supplied, or the reason they
     * cannot be used. Exactly one of the two is meaningful: {@link #getProblem()} non-null means the
     * call must be refused and nothing set.
     */
    public static final class Resolved {
        private final Map<String, String> theValues;
        private final String theProblem;

        private Resolved(Map<String, String> theValuesIn, String theProblemIn) {
            this.theValues = Collections.unmodifiableMap(theValuesIn);
            this.theProblem = theProblemIn;
        }

        static Resolved ok(Map<String, String> theValuesIn) {
            return new Resolved(theValuesIn, null);
        }

        static Resolved refused(String theProblemIn) {
            return new Resolved(new LinkedHashMap<String, String>(), theProblemIn);
        }

        /** Name to value, in declaration order. Empty when refused. */
        public Map<String, String> getValues() {
            return theValues;
        }

        /** Why the call must be refused, or null. Written for the caller, naming the parameter. */
        public String getProblem() {
            return theProblem;
        }
    }

    /**
     * The longest name accepted. 30 is the {@code DBMS_SESSION.SET_CONTEXT} attribute limit on
     * 12.1; later versions allow more, but a config that works on one box and not another is the
     * failure this estate exists to catch, so the portable limit is the only limit.
     */
    public static final int MAX_NAME_LENGTH = 30;

    /** The {@code DBMS_SESSION.SET_CONTEXT} value limit, in bytes. */
    public static final int MAX_VALUE_BYTES = 4000;

    /**
     * Upper case only, because names are stored upper case and the comparison with the URL is
     * case-insensitive. Letters, digits and underscore, starting with a letter: an Oracle simple
     * identifier, and safe as a URL query name and as an environment variable suffix without
     * escaping in any of the three.
     */
    private static final Pattern NAME = Pattern.compile("[A-Z][A-Z0-9_]*");

    private McpContextParams() {
    }

    /**
     * The canonical form of a name: trimmed and upper case. Null stays null.
     *
     * @param theName a name as typed or as it arrived on a URL
     * @return the stored form
     */
    public static String normalise(String theName) {
        return theName == null ? null : theName.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Why this name cannot be declared, or null when it can. Checked after {@link #normalise}, so
     * lower case is accepted and stored upper.
     *
     * @param theName the name as typed
     * @return a sentence naming the problem, or null
     */
    public static String nameProblem(String theName) {
        String theCanonical = normalise(theName);
        if (theCanonical == null || theCanonical.isEmpty()) {
            return "A context parameter name cannot be empty.";
        }
        if (theCanonical.length() > MAX_NAME_LENGTH) {
            return "Context parameter '" + theCanonical + "' is " + theCanonical.length()
                    + " characters; the limit is " + MAX_NAME_LENGTH
                    + " (Oracle's application context attribute limit).";
        }
        if (!NAME.matcher(theCanonical).matches()) {
            return "Context parameter '" + theCanonical + "' must start with a letter and contain"
                    + " only letters, digits and underscores.";
        }
        return null;
    }

    /**
     * Why this list of names cannot be declared together, or null when it can: each name must be
     * valid, and no name may appear twice (after normalising, so {@code customer_id} and
     * {@code CUSTOMER_ID} are the same name).
     *
     * @param theNames the names in declaration order
     * @return a sentence naming the first problem, or null
     */
    public static String listProblem(List<String> theNames) {
        if (theNames == null) {
            return null;
        }
        Set<String> theSeen = new HashSet<>();
        for (String theName : theNames) {
            String theProblem = nameProblem(theName);
            if (theProblem != null) {
                return theProblem;
            }
            if (!theSeen.add(normalise(theName))) {
                return "Context parameter '" + normalise(theName) + "' is declared twice.";
            }
        }
        return null;
    }

    /**
     * The values on one HTTP request's query string, checked against what the config declared.
     *
     * <p>Refused, naming the parameter: a name not declared (a typo must not run with the context
     * unset); a name given twice (which one was meant?); a declared name missing or empty (Oracle
     * stores '' as NULL); a value over {@link #MAX_VALUE_BYTES}. Names match case-insensitively.
     * Read from the raw query string rather than {@code getParameter}, which may parse a request
     * body and does not report a repeated name as a repeat.
     *
     * @param theQuery    {@code HttpServletRequest.getQueryString()}, possibly null
     * @param theDeclared the names the config declares, normalised
     * @return the values, or the reason to refuse
     */
    public static Resolved fromQueryString(String theQuery, List<String> theDeclared) {
        Map<String, String> theSupplied = new LinkedHashMap<>();
        if (theQuery != null && !theQuery.isEmpty()) {
            for (String thePair : theQuery.split("&")) {
                if (thePair.isEmpty()) {
                    continue;
                }
                int theEquals = thePair.indexOf('=');
                String theName;
                String theValue;
                try {
                    theName = URLDecoder.decode(theEquals < 0 ? thePair : thePair.substring(0, theEquals),
                            StandardCharsets.UTF_8);
                    theValue = theEquals < 0 ? "" : URLDecoder.decode(thePair.substring(theEquals + 1),
                            StandardCharsets.UTF_8);
                } catch (IllegalArgumentException e) {
                    return Resolved.refused("The connection URL's query string is malformed: " + e.getMessage());
                }
                String theCanonical = normalise(theName);
                if (!theDeclared.contains(theCanonical)) {
                    return Resolved.refused("The connection URL carries '" + theName + "', which this"
                            + " server does not accept. " + declaredSentence(theDeclared));
                }
                if (theSupplied.containsKey(theCanonical)) {
                    return Resolved.refused("The connection URL gives " + theCanonical + " more than once.");
                }
                theSupplied.put(theCanonical, theValue);
            }
        }
        return complete(theSupplied, theDeclared, "the connection URL");
    }

    /**
     * The values a stdio server takes from its environment: {@code MCP_CONTEXT_<NAME>} for each
     * declared name. Fixed for the life of the process. Only declared names are looked up, so an
     * unrelated variable that happens to share the prefix is not a refusal.
     *
     * @param theEnvironment usually {@code System.getenv()}
     * @param theDeclared    the names the config declares, normalised
     * @return the values, or the reason to refuse
     */
    public static Resolved fromEnvironment(Map<String, String> theEnvironment, List<String> theDeclared) {
        Map<String, String> theSupplied = new LinkedHashMap<>();
        for (String theName : theDeclared) {
            String theValue = theEnvironment.get(ENV_VARIABLE_PREFIX + theName);
            if (theValue != null) {
                theSupplied.put(theName, theValue);
            }
        }
        return complete(theSupplied, theDeclared, "the environment (" + ENV_VARIABLE_PREFIX + "<NAME>)");
    }

    /** Every declared name present and usable, in declaration order; otherwise refused. */
    private static Resolved complete(Map<String, String> theSupplied, List<String> theDeclared,
                                     String theSource) {
        Map<String, String> theOrdered = new LinkedHashMap<>();
        List<String> theMissing = new ArrayList<>();
        for (String theName : theDeclared) {
            String theValue = theSupplied.get(theName);
            if (theValue == null || theValue.isEmpty()) {
                theMissing.add(theName);
                continue;
            }
            String theProblem = valueProblem(theName, theValue);
            if (theProblem != null) {
                return Resolved.refused(theProblem);
            }
            theOrdered.put(theName, theValue);
        }
        if (!theMissing.isEmpty()) {
            return Resolved.refused("This server requires " + String.join(", ", theMissing)
                    + " from " + theSource + ", and " + (theMissing.size() == 1 ? "it is" : "they are")
                    + " missing or empty.");
        }
        return Resolved.ok(theOrdered);
    }

    private static String declaredSentence(List<String> theDeclared) {
        return theDeclared.isEmpty() ? "It accepts no URL parameters."
                : "It accepts: " + String.join(", ", theDeclared) + ".";
    }

    /**
     * Replace this session's {@value #ORACLE_NAMESPACE} context with exactly these values, in ONE
     * round trip: clear everything, then set each pair.
     *
     * <p><b>Clear first, every time, including when the map is empty.</b> Connections are pooled,
     * so a session that served one caller is handed to the next; clearing only on the way out
     * would leak the previous caller's values whenever that path did not run (a timeout, a thread
     * killed mid-call). Clearing at the start cannot be skipped by anything that happened before.
     *
     * <p>Names and values are bound, never concatenated, and the package re-checks both, so a
     * caller that skipped {@link #nameProblem}/{@link #valueProblem} still cannot set an invalid
     * name or a NULL.
     *
     * @param theConnection the session to set them on
     * @param theValues     name to value, in the order to set them; names already normalised
     * @throws SQLException when the package is missing or not granted (PLS-00201 inside an
     *                      ORA-06550), or rejects a name or value (ORA-20901 / ORA-20902). The
     *                      caller must not run the tool call on this session afterwards.
     */
    public static void applyToSession(Connection theConnection, Map<String, String> theValues)
            throws SQLException {
        StringBuilder theBlock = new StringBuilder("begin ").append(ORACLE_PACKAGE).append(".clear_all;");
        for (int i = 0; i < theValues.size(); i++) {
            theBlock.append(' ').append(ORACLE_PACKAGE).append(".set_value(?, ?);");
        }
        theBlock.append(" end;");
        try (CallableStatement theCall = theConnection.prepareCall(theBlock.toString())) {
            int theIndex = 1;
            for (Map.Entry<String, String> theEntry : theValues.entrySet()) {
                theCall.setString(theIndex++, theEntry.getKey());
                theCall.setString(theIndex++, theEntry.getValue());
            }
            theCall.execute();
        }
    }

    /**
     * What to tell an operator when {@link #applyToSession} failed because the context is not
     * there to set: the package missing, or this account not granted it. Null for any other
     * failure, which should be reported as it stands.
     *
     * <p>Takes any throwable and walks its causes, because a pooled server reaches the database
     * through a wrapper that re-throws the driver's exception inside its own.
     *
     * @param theFailure what {@link #applyToSession}, or the borrow around it, threw
     * @return the sentence to log, or null
     */
    public static String notInstalledReason(Throwable theFailure) {
        for (Throwable theCause = theFailure; theCause != null; theCause = theCause.getCause()) {
            String theMessage = String.valueOf(theCause.getMessage());
            if (theMessage.contains("PLS-00201")) {
                return "This config declares URL context parameters, but this database account cannot"
                        + " call " + ORACLE_PACKAGE + ". A DBA must run app/db/mcp-context/install.sql"
                        + " once for this database, then grant.sql for this account. ("
                        + theMessage.trim() + ")";
            }
            if (theCause.getCause() == theCause) {
                break;
            }
        }
        return null;
    }

    /**
     * Why this value cannot be set, or null when it can. Missing and empty are both refused.
     * Length is measured in UTF-8 bytes, because that is
     * what Oracle's limit counts on an AL32UTF8 database; a value is refused rather than truncated,
     * since a truncated customer id is a different customer.
     *
     * @param theName  the parameter, for the message
     * @param theValue the value as supplied
     * @return a sentence naming the problem, or null
     */
    public static String valueProblem(String theName, String theValue) {
        // Empty is refused with null, not accepted as a value: Oracle stores '' as NULL, so
        // ?CUSTOMER_ID= would satisfy "required" and still leave SYS_CONTEXT returning NULL -- the
        // exact state the requirement exists to prevent.
        if (theValue == null || theValue.isEmpty()) {
            return "Context parameter '" + theName + "' has no value.";
        }
        // An UNEXPANDED client-side variable. Measured with Claude Code 2.1.286: a URL of
        // ...?CUSTOMER_ID=${CUST} with CUST unset is sent with the literal text "${CUST}", on every
        // request, and without this the call ran with that string as the customer -- matching no
        // rows rather than failing, so the misconfiguration was silent. No real identifier
        // contains "${", so refusing it costs nothing and names the actual fault.
        if (theValue.contains("${")) {
            return "Context parameter '" + theName + "' is '" + theValue + "', which looks like an"
                    + " environment variable the MCP client did not substitute. Set it in the"
                    + " client's environment, or put the value in the URL directly.";
        }
        int theBytes = theValue.getBytes(StandardCharsets.UTF_8).length;
        if (theBytes > MAX_VALUE_BYTES) {
            return "Context parameter '" + theName + "' is " + theBytes + " bytes; the limit is "
                    + MAX_VALUE_BYTES + ".";
        }
        return null;
    }
}
