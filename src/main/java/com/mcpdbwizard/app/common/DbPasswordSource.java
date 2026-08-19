package com.mcpdbwizard.app.common;

import com.mcpdbwizard.pub.CSException;

/**
 * Lets a config say "the database password comes from the environment" instead of carrying the
 * password itself.
 *
 * <p>Writing {@value #SENTINEL} wherever the password would go — as the {@code PASS} value, or
 * inside the {@code DAO_CONNECTION_NAME} JDBC URL — means the real secret is read from the
 * {@value #ENVIRONMENT_VARIABLE} environment variable instead. Nothing else changes, and a config
 * that carries a literal password keeps working exactly as before.
 *
 * <h2>Why the sentinel survives into the generated code</h2>
 *
 * <p>The same value is used twice: the generator connects with it to read the data dictionary, and
 * it is also written into the generated {@code DaoFactory} for that code to connect with later.
 * Resolving the sentinel once, at config load, would fix only the first — the real password would
 * still be substituted into the emitted source, which is the copy that gets deployed.
 *
 * <p>So resolution happens at each point of use, and they differ:
 *
 * <ul>
 *   <li>the <b>generator</b> calls {@link #resolve} when it opens its own connection, and keeps the
 *       configured (sentinel) form everywhere else;</li>
 *   <li>the <b>emitter</b> calls {@link #toJavaExpression}, which produces source that reads the
 *       environment variable <em>at run time</em> rather than a literal.</li>
 * </ul>
 *
 * <p>Both are fail-closed: an unset or empty variable is an error, not an empty password.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class DbPasswordSource {

    /** Write this in a config where the password would go. */
    public static final String SENTINEL = "FROM_ENV_VARIABLE_DB_PASS";

    /** The environment variable the sentinel refers to. */
    public static final String ENVIRONMENT_VARIABLE = "DB_PASS";

    private DbPasswordSource() {
    }

    /**
     * Whether this configured value defers to the environment.
     *
     * <p>A "contains" test, not an equality test, because the value may be a whole JDBC URL with the
     * sentinel embedded in its credentials — which is how the Oracle hard-coded connection type
     * carries a password.
     */
    public static boolean isFromEnvironment(String theConfiguredValue) {
        return theConfiguredValue != null && theConfiguredValue.indexOf(SENTINEL) >= 0;
    }

    /**
     * The value with the real secret substituted, for the generator's own connection.
     *
     * @return the value unchanged when it holds no sentinel
     * @throws CSException if the variable is unset or empty — connecting with a blank password would
     *                     fail further away, with a message about the database rather than the config
     */
    public static String resolve(String theConfiguredValue) throws CSException {
        if (!isFromEnvironment(theConfiguredValue)) {
            return theConfiguredValue;
        }

        String theSecret;
        try {
            theSecret = com.mcpdbwizard.pub.EnvironmentSecret.read(ENVIRONMENT_VARIABLE);
        } catch (IllegalStateException e) {
            // Both variables set, or the file is unreadable or empty.
            throw new CSException(e.getMessage());
        }
        if (theSecret == null || theSecret.length() == 0) {
            throw new CSException("This config takes the database password from the environment (it"
                    + " contains " + SENTINEL + "), but neither " + ENVIRONMENT_VARIABLE + " nor "
                    + ENVIRONMENT_VARIABLE + com.mcpdbwizard.pub.EnvironmentSecret.FILE_SUFFIX
                    + " is set. Set one and try again.");
        }
        return replaceSentinel(theConfiguredValue, theSecret);
    }

    /**
     * A Java source expression yielding this value, for the generated code.
     *
     * <p>Without a sentinel that is just a quoted literal, byte-identical to what the generator
     * emitted before this existed. With one, it is a concatenation calling {@code theHelperCall},
     * so the secret is read from the environment when the generated code runs and never appears in
     * the emitted source.
     *
     * @param theHelperCall source of the no-argument call returning the password, e.g.
     *                      {@code passwordFromEnvironment()}
     */
    public static String toJavaExpression(String theConfiguredValue, String theHelperCall) {
        if (!isFromEnvironment(theConfiguredValue)) {
            return "\"" + theConfiguredValue + "\"";
        }

        StringBuilder theExpression = new StringBuilder();
        int thePosition = 0;
        while (thePosition <= theConfiguredValue.length()) {
            int theNext = theConfiguredValue.indexOf(SENTINEL, thePosition);
            if (theNext < 0) {
                appendLiteral(theExpression, theConfiguredValue.substring(thePosition));
                break;
            }
            appendLiteral(theExpression, theConfiguredValue.substring(thePosition, theNext));
            appendTerm(theExpression, theHelperCall);
            thePosition = theNext + SENTINEL.length();
        }
        // Only possible if the value is nothing but sentinels, which still yields a term above.
        return theExpression.length() == 0 ? theHelperCall : theExpression.toString();
    }

    /** Append a quoted literal, skipping the empty runs either side of a sentinel. */
    private static void appendLiteral(StringBuilder theExpression, String theText) {
        if (theText.length() == 0) {
            return;
        }
        appendTerm(theExpression, "\"" + escapeForJavaSource(theText) + "\"");
    }

    private static void appendTerm(StringBuilder theExpression, String theTerm) {
        if (theExpression.length() > 0) {
            theExpression.append(" + ");
        }
        theExpression.append(theTerm);
    }

    /** A JDBC URL should contain neither, but emitting broken source would be worse than checking. */
    private static String escapeForJavaSource(String theText) {
        return theText.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String replaceSentinel(String theValue, String theSecret) {
        StringBuilder theResult = new StringBuilder();
        int thePosition = 0;
        while (true) {
            int theNext = theValue.indexOf(SENTINEL, thePosition);
            if (theNext < 0) {
                theResult.append(theValue.substring(thePosition));
                return theResult.toString();
            }
            theResult.append(theValue, thePosition, theNext).append(theSecret);
            thePosition = theNext + SENTINEL.length();
        }
    }
}
