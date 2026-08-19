package com.mcpdbwizard.pub;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Names the session in Oracle's own views, by calling {@code DBMS_APPLICATION_INFO.SET_MODULE}
 * once when a connection is opened.
 *
 * <p>Without this a session shows up in {@code V$SESSION} as a bare username with a null
 * {@code MODULE}, and every connection from every application that shares the account looks
 * identical. That matters here more than it does for an ordinary application, because a generated
 * DAO layer is deliberately a <em>service account</em> shape — one Oracle user for all callers — so
 * the username identifies nobody. {@code MODULE} is the first column a DBA groups by when a session
 * is blocking, burning CPU, or holding a lock, and it is what {@code DBMS_MONITOR} and AWR key their
 * per-module aggregates on.
 *
 * <p>Set at connect time and left alone. {@code SET_MODULE} costs one round trip on a connection
 * that has just paid for a login, which is not worth optimising away.
 *
 * <h2>Failure is never fatal</h2>
 *
 * <p>This throws {@link SQLException} rather than swallowing it, so the caller decides — and both
 * callers log and carry on. Naming a session is a diagnostic courtesy; refusing to connect because
 * the courtesy failed would trade a working application for a tidy view. {@code DBMS_APPLICATION_INFO}
 * is granted to {@code PUBLIC} on a stock install, so a failure here means someone revoked it
 * deliberately, and their application should still run.
 *
 * <h2>Lengths</h2>
 *
 * <p>Oracle truncates {@code module_name} at 48 bytes and {@code action_name} at 32, silently. We
 * truncate first so that what we asked for is what the view shows, rather than discovering the
 * limit through a mysteriously clipped name. The cut is by character, so a multi-byte name can still
 * exceed the byte limit and be clipped further by Oracle — harmless, and not worth a byte-aware
 * substring for values that are ASCII class names in practice.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class SessionInfo {

    /** Oracle truncates {@code module_name} beyond this many bytes. */
    public static final int MODULE_MAX_LENGTH = 48;

    /** Oracle truncates {@code action_name} beyond this many bytes. */
    public static final int ACTION_MAX_LENGTH = 32;

    /**
     * Module recorded by the generator's own connection — the one that reads the data dictionary,
     * not one a generated application opens.
     * <p>
     * <b>Now taken from {@link Namer#param_prod_name}, having been a literal until 2026-08-07.</b>
     * It was a literal for a reason that has since gone away: the Namer constants were unresolved
     * placeholders, so reading one would have put the string {@code "SUBST_PROD_NAME"} into
     * {@code V$SESSION}. They hold real branding now, and this is the value a DBA groups by when a
     * session is blocking — so it should follow the product name rather than be retyped beside it.
     */
    public static final String GENERATOR_MODULE = Namer.param_prod_name;

    /**
     * Action recorded by the generator's connection. Every such connection does the same thing —
     * read the dictionary — whether it was opened by the Swing UI, a batch run, or the web
     * Design page, so one value is honest for all three.
     */
    public static final String GENERATOR_ACTION = "Introspect";

    private static final String SET_MODULE_CALL = "{call DBMS_APPLICATION_INFO.SET_MODULE(?, ?)}";

    private SessionInfo() {
    }

    /**
     * Record this session's module and action in {@code V$SESSION}.
     *
     * @param theConnection the connection to name; a null one is ignored
     * @param theModule     application name, truncated to {@link #MODULE_MAX_LENGTH}; null clears it
     * @param theAction     what it is doing, truncated to {@link #ACTION_MAX_LENGTH}; null clears it
     * @throws SQLException if the call fails — see the class comment on why callers absorb this
     */
    public static void setModule(Connection theConnection, String theModule, String theAction)
            throws SQLException {

        if (theConnection == null) {
            return;
        }

        try (CallableStatement theCall = theConnection.prepareCall(SET_MODULE_CALL)) {
            theCall.setString(1, truncate(theModule, MODULE_MAX_LENGTH));
            theCall.setString(2, truncate(theAction, ACTION_MAX_LENGTH));
            theCall.execute();
        }
    }

    /**
     * Cut a value to what Oracle will store, leaving null and short values untouched.
     */
    public static String truncate(String theValue, int theMaxLength) {

        if (theValue == null || theValue.length() <= theMaxLength) {
            return theValue;
        }

        return theValue.substring(0, theMaxLength);
    }
}
