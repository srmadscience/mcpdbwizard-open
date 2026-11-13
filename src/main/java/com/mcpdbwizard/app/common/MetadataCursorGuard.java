package com.mcpdbwizard.app.common;

import com.mcpdbwizard.pub.CSException;
import com.mcpdbwizard.pub.LogInterface;
import com.mcpdbwizard.pub.QueryStatement;
import com.mcpdbwizard.pub.ReadOnlyRowSet;

import java.sql.Connection;

/**
 * Runs the generator's per-object dictionary queries, and recovers when the session runs out of
 * cursors (ORA-01000, maximum open cursors exceeded).
 * <p>
 * <b>Why it exists (2.0.37).</b> The MCP passes create a {@code QueryStatement} for every table,
 * lookup and collection they describe and, until this, never closed one: each held an open cursor
 * for the rest of the generator's session. With Oracle's usual {@code open_cursors} of 300, a config
 * of about seventy tables or more reached the limit part-way through, and every table after that was
 * left out of the MCP server with an information-level line while generation reported success.
 * Measured on all six test boxes: {@code generic_test1_mcp} lost 120 tables' tools that way.
 * <p>
 * <b>The policy (David, 2026-11-12):</b> nothing changes until ORA-01000 is seen. The first time it
 * is, this class logs a warning naming what it was reading, sets a flag, has the caller disconnect
 * and reconnect (which releases every cursor the old session held, and is logged), and retries that
 * query once. From then on every statement it runs is closed as soon as its rows are read, so the
 * run cannot reach the limit again. A query that fails again after the reconnect is logged at error
 * level and thrown, never skipped quietly.
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class MetadataCursorGuard {

    /** What the generator does to recover: disconnect, reconnect, and return the new connection. */
    public interface Reconnector {
        Connection reconnect(String why) throws CSException;
    }

    /** Supplies the connection to use now, which changes when a reconnect happens. */
    public interface ConnectionSource {
        Connection current();
    }

    private final LogInterface theLog;
    private final ConnectionSource theConnections;
    private final Reconnector theReconnector;
    private boolean closeEachTime;
    private int recoveries;

    public MetadataCursorGuard(LogInterface theLog, ConnectionSource theConnections, Reconnector theReconnector) {
        this.theLog = theLog;
        this.theConnections = theConnections;
        this.theReconnector = theReconnector;
    }

    /** True once ORA-01000 has been seen in this run: every statement is then closed after use. */
    public boolean closesEachTime() {
        return closeEachTime;
    }

    /** How many times this run reconnected because of ORA-01000. */
    public int recoveries() {
        return recoveries;
    }

    /**
     * True when {@code theProblem}, or anything that caused it, is ORA-01000. The pub library wraps
     * the driver's exception in a {@code CSException} whose message carries the Oracle text, so the
     * messages are read as well as the vendor code.
     */
    public static boolean isCursorExhaustion(Throwable theProblem) {
        // One definition, shared with the generated servers' pool (DaoFactoryPool).
        return com.mcpdbwizard.pub.DaoFactoryPool.isCursorExhaustion(theProblem);
    }

    /**
     * Runs one dictionary query.
     *
     * @param theWhat    what is being read, for the log ("table APP.ORDERS: its primary key")
     * @param theSql     the query
     * @param theParams  its bind values, in order
     * @return the rows, read into memory
     * @throws CSException when the query fails for any other reason, or fails again after the
     *                     recovery; the second case is logged here at error level
     */
    public ReadOnlyRowSet query(String theWhat, String theSql, String... theParams) throws CSException {
        try {
            return runOnce(theSql, theParams);
        } catch (CSException e) {
            if (!isCursorExhaustion(e)) {
                throw e;
            }
            recover(theWhat);
            try {
                return runOnce(theSql, theParams);
            } catch (CSException again) {
                theLog.error("Reading " + theWhat + " failed again after reconnecting for ORA-01000: "
                        + again.getMessage());
                throw again;
            }
        }
    }

    private ReadOnlyRowSet runOnce(String theSql, String[] theParams) throws CSException {
        QueryStatement theQuery = new QueryStatement(theSql, theLog, theConnections.current());
        boolean ran = false;
        try {
            for (int i = 0; i < theParams.length; i++) {
                theQuery.setParam(i + 1, theParams[i]);
            }
            ReadOnlyRowSet theRows = theQuery.execute();
            ran = true;
            return theRows;
        } finally {
            // A failed statement is closed whatever the flag says: it is about to be retried or
            // reported, and a statement that failed with ORA-01000 holds nothing worth keeping.
            if (closeEachTime || !ran) {
                theQuery.releaseResources();
            }
        }
    }

    private void recover(String theWhat) throws CSException {
        recoveries++;
        if (!closeEachTime) {
            closeEachTime = true;
            theLog.warning("ORA-01000 (maximum open cursors exceeded) while reading " + theWhat
                    + ". The session has run out of cursors. From now on each dictionary statement is"
                    + " closed as soon as it has been read, and the session is reconnected to release the"
                    + " cursors it holds. Raising the database's OPEN_CURSORS would also avoid this.");
        } else {
            theLog.warning("ORA-01000 (maximum open cursors exceeded) again while reading " + theWhat
                    + ", although statements are now closed after use; reconnecting again.");
        }
        theReconnector.reconnect("ORA-01000 while reading " + theWhat);
        theLog.warning("Retrying " + theWhat + " on the new connection.");
    }
}
