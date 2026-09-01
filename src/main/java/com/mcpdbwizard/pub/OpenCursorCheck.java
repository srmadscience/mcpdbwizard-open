package com.mcpdbwizard.pub;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Warns, once at start-up, when a config publishes more tools than the database session can hold
 * open cursors for.
 *
 * <h2>Why this exists</h2>
 *
 * <p>A generated DAO prepares each statement once and <em>keeps the handle</em> —
 * {@link DmlStatement#createPreparedStatement()} only prepares when {@code thePreparedStatement}
 * is null, and closes it in {@code freeConnection()}. That is deliberate reuse and it is the right
 * design: the second call to a tool costs no parse. The consequence is that a session's open-cursor
 * count grows with the number of <em>distinct</em> tools it has been asked for, and never with how
 * many times they are called. Once every tool has been used once, the count stops rising.
 *
 * <p>So a server that exposes more distinct tools than {@code open_cursors} — 300 by default, and
 * 300 on every box this was measured against — will eventually raise {@code ORA-01000: maximum open
 * cursors for session exceeded}, no matter how gently it is used.
 *
 * <h2>Why it is worth a check rather than a note in a manual</h2>
 *
 * <p>Measured behaviour, because it decides how this failure presents. Hitting the cap throws, the
 * throw releases the cursors, and the count climbs again from zero — so the server does not stop.
 * It fails one call in every {@code open_cursors}, on a different tool each time. At 512 tools that
 * was five failures in 1536 calls: <b>0.3%, scattered</b>, which reads as a transient database blip
 * rather than a configuration that has outgrown a parameter. A deployment can run in that state for
 * a long time without anyone connecting the errors to the config's size.
 *
 * <p>It is also invisible to load testing, which is the other reason to say it at start-up. Because
 * the cost is per distinct tool, a million calls through four tools opens four cursors. Only
 * <em>breadth</em> finds this, and load tests are built for depth.
 *
 * <h2>One cursor per tool is the FLOOR, not the rate</h2>
 *
 * <p>Measured per-tool cost against a real schema: most scalar routines cost 1, a routine that
 * returns a REF CURSOR or moves a LOB cost 3, and one that executes no SQL costs 0 — an average of
 * about 1.3 across a 17-tool sample. That is why {@link #WARN_AT_FRACTION} fires well below the
 * limit rather than at it: a server with 250 tools against {@code open_cursors=300} looks safe by
 * counting and is not.
 *
 * <h2>Warn, never refuse</h2>
 *
 * <p>This logs and returns. Refusing to start would be wrong twice over: the estimate is an
 * estimate, and a server whose callers only ever touch a handful of its tools will never reach the
 * limit at all. The failure being prevented is a silent one, and a loud sentence prevents it.
 *
 * <h2>When {@code V$PARAMETER} cannot be read</h2>
 *
 * <p>Reading it needs a privilege — usually {@code SELECT_CATALOG_ROLE} or an explicit grant — and
 * whether a DAO account has it varies by <em>account</em> rather than by database: of four accounts
 * on the development estate, three could read it and one could not. So this is neither the normal
 * path nor a rare one.
 *
 * <p>When it cannot be read the check <b>does not assume a limit</b>. It reports that
 * {@code open_cursors} is unknown and that the tool count may exceed it. Guessing Oracle's shipped
 * default would be worse than saying nothing: a site that has <em>raised</em> {@code open_cursors} —
 * the common adjustment on a busy system — would be warned on every start-up about a config that
 * fits comfortably, and a start-up warning that cannot be silenced and is not true is exactly how a
 * check gets ignored. {@link #ORACLE_DEFAULT_OPEN_CURSORS} is therefore documentation for the reader
 * of the message, not an input to any decision.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class OpenCursorCheck {

    /**
     * Oracle's shipped {@code open_cursors}. Quoted in the unknown-limit message so the reader has a
     * number to compare against; deliberately NOT used as a fallback limit — see the class comment.
     */
    public static final int ORACLE_DEFAULT_OPEN_CURSORS = 300;

    /**
     * Warn once the tool count reaches this share of the limit. Deliberately not 1.0: a tool costs
     * one cursor at best and three at worst, so parity between the counts is already over.
     */
    public static final double WARN_AT_FRACTION = 0.7;

    private static final String LIMIT_QUERY =
            "select value from v$parameter where name = 'open_cursors'";

    private OpenCursorCheck() {
    }

    /**
     * Log a warning if this config publishes enough tools to exhaust the session's cursors.
     *
     * @param theConnection the DAO's own connection — the session that will hold the cursors, which
     *                      is why this cannot be checked from an application that merely knows
     *                      about the config. A null connection is ignored.
     * @param theToolCount  how many tools this server publishes
     * @param theLog        where to say so; a null log is ignored
     */
    public static void warnIfToolsExceedCursors(Connection theConnection, int theToolCount,
                                                LogInterface theLog) {
        if (theConnection == null || theLog == null || theToolCount <= 0) {
            return;
        }

        int theLimit = readOpenCursors(theConnection);

        if (theLimit <= 0) {
            // Unknown, and left unknown. Said once at start-up whatever the tool count, because
            // without the limit there is no threshold to be under: a small config is only safe if
            // open_cursors is bigger than it, and that is the fact we do not have.
            theLog.warning("This server publishes " + theToolCount + " tools, and open_cursors could"
                    + " not be read from V$PARAMETER on this connection (the account needs"
                    + " SELECT_CATALOG_ROLE or a grant on it), so it is UNKNOWN and MAY BE EXCEEDED."
                    + " " + WHY_IT_MATTERS + " Oracle's default open_cursors is "
                    + ORACLE_DEFAULT_OPEN_CURSORS + "; if this server's tool count approaches"
                    + " whatever this database is set to, " + WHAT_TO_DO);
            return;
        }

        if (theToolCount < theLimit * WARN_AT_FRACTION) {
            return;
        }

        String theMessage = "This server publishes " + theToolCount + " tools and open_cursors is "
                + theLimit + ". " + WHY_IT_MATTERS + " " + WHAT_TO_DO;

        if (theToolCount >= theLimit) {
            theLog.error(theMessage);
        } else {
            theLog.warning(theMessage);
        }
    }

    /** Shared so the known and unknown messages cannot drift into describing different mechanisms. */
    private static final String WHY_IT_MATTERS =
            "A generated DAO holds ONE OPEN CURSOR PER DISTINCT TOOL for the life of the session --"
                    + " more for tools returning a REF CURSOR or moving a LOB -- so a session asked"
                    + " for most of this server's tools can raise ORA-01000 (maximum open cursors"
                    + " for session exceeded). It recovers and continues, so it presents as"
                    + " occasional unrelated-looking failures rather than an outage.";

    private static final String WHAT_TO_DO =
            "either raise open_cursors on the database, or split this config into smaller ones.";

    /**
     * The session's {@code open_cursors}, or 0 when it cannot be read.
     *
     * <p>Closed with try-with-resources, which matters more here than usual: a check about leaking
     * cursors that leaked one would be its own best example.
     */
    private static int readOpenCursors(Connection theConnection) {
        try (PreparedStatement theStatement = theConnection.prepareStatement(LIMIT_QUERY);
             ResultSet theResults = theStatement.executeQuery()) {
            if (theResults.next()) {
                return Integer.parseInt(theResults.getString(1).trim());
            }
        } catch (SQLException | NumberFormatException e) {
            // Expected on an ordinary application schema: V$PARAMETER needs SELECT_CATALOG_ROLE or
            // an explicit grant. The caller falls back to the documented default and says so.
            return 0;
        }
        return 0;
    }
}
