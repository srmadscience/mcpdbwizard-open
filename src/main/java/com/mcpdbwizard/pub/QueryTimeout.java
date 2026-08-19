package com.mcpdbwizard.pub;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * An optional cap on how long any one generated statement may run.
 *
 * <p>Set {@code DAO_QUERY_TIMEOUT_SECONDS} (or {@code -Ddao.query.timeoutSeconds}). Unset means no
 * cap, which is the behaviour every existing deployment already has.
 *
 * <h2>Why this matters more than a rate limit</h2>
 *
 * <p>A rate limit bounds how <em>often</em> work starts. It does nothing about how long one call runs,
 * and a single expensive query holds its pooled factory — and therefore its Oracle session — for as
 * long as the database takes. A handful of slow calls can starve every other caller at trivial request
 * rates. This is the control that bounds that, and it is why
 * {@code docs/mcp-rate-limiting-plan.md} rates it above the rate limit itself.
 *
 * <p><b>It is opt-in on purpose.</b> A timeout low enough to be useful against a runaway query is also
 * low enough to break a legitimately slow report, and only the deployment knows which of its
 * statements are which. Defaulting this on would trade a rare failure for a routine one.
 *
 * <p>Oracle raises {@code ORA-01013} when the cap is hit, which the generated code surfaces as an
 * ordinary {@code CSException} — the call fails, the factory returns to the pool, and the session is
 * reusable.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class QueryTimeout {

    /** Environment variable holding the cap in seconds. */
    public static final String ENVIRONMENT_VARIABLE = "DAO_QUERY_TIMEOUT_SECONDS";

    /** System property equivalent, which wins over the environment. */
    public static final String SYSTEM_PROPERTY = "dao.query.timeoutSeconds";

    /** Resolved once: this is consulted on every statement, and it cannot change while running. */
    private static final int THE_SECONDS = resolve(System.getProperty(SYSTEM_PROPERTY),
            System.getenv(ENVIRONMENT_VARIABLE));

    private QueryTimeout() {
    }

    /** The configured cap in seconds, or 0 when there is none. */
    public static int getSeconds() {
        return THE_SECONDS;
    }

    public static boolean isEnabled() {
        return THE_SECONDS > 0;
    }

    /**
     * Apply the cap to a statement, if one is configured.
     *
     * <p>Deliberately swallows {@link SQLException}: a driver that will not accept a query timeout is
     * not a reason to fail the call the statement was prepared for. The cap is a safety net, and a
     * safety net that breaks the thing it protects is worse than none.
     *
     * @param theStatement the statement just prepared; null is tolerated
     */
    public static void apply(Statement theStatement) {
        if (theStatement == null || THE_SECONDS <= 0) {
            return;
        }
        try {
            theStatement.setQueryTimeout(THE_SECONDS);
        } catch (SQLException e) {
            // Nothing useful to do, and nowhere sensible to report it from.
        }
    }

    /** Testable resolution: property wins over environment, anything unusable means "no cap". */
    static int resolve(String thePropertyValue, String theEnvironmentValue) {
        String theValue = thePropertyValue != null && thePropertyValue.trim().length() > 0
                ? thePropertyValue : theEnvironmentValue;
        if (theValue == null || theValue.trim().length() == 0) {
            return 0;
        }
        try {
            int theSeconds = Integer.parseInt(theValue.trim());
            // Negative is meaningless to JDBC and zero already means "no cap"; treat both as unset
            // rather than throwing, because this is read from a static initialiser and a throw here
            // would take the whole application down over a typo in an optional setting.
            return theSeconds > 0 ? theSeconds : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
