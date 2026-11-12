package com.mcpdbwizard.pub;

import com.mcpdbwizard.app.common.MetadataCursorGuard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * {@link MetadataCursorGuard} against a real session that has run out of cursors (2.0.37).
 * <p>
 * The generator's MCP passes held a cursor per table, so a config of about seventy tables or more
 * hit ORA-01000 part-way through and silently lost every later table's tools. The guard's policy
 * (David, 2026-11-12): change nothing until ORA-01000 is seen; then log it, reconnect (logged),
 * retry, and close every statement from then on. These tests exhaust a session's cursors for real
 * by holding open result sets until Oracle refuses another, which is the condition the generator
 * reached, rather than imitating the error.
 * <p>
 * Gated on a reachable database ({@link DbTestSupport}); skips without one.
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class MetadataCursorGuardLiveTest {

    private static final String QUERY = "SELECT ? AS v FROM dual";

    /** Every message, kept in order with its type, so a test can assert what was said and when. */
    private static final class CapturingLog extends GenericLog {
        final List<String> lines = new ArrayList<>();

        @Override
        protected void writeMessage(String messageType, String messageText, boolean isModal, boolean isLogged) {
            lines.add(messageType + ": " + messageText);
        }

        @Override
        public void flush() {
        }

        @Override
        public String getCurrentLog() {
            return "capture";
        }

        long count(String fragment) {
            return lines.stream().filter(l -> l.contains(fragment)).count();
        }
    }

    private final List<Connection> opened = new ArrayList<>();
    private Connection current;

    @AfterEach
    void closeEverything() {
        for (Connection c : opened) {
            try {
                c.close();
            } catch (SQLException ignored) {
                // closing on the way out
            }
        }
    }

    private Connection open() {
        Connection c = DbTestSupport.requireConnection();
        opened.add(c);
        return c;
    }

    /** Holds open result sets on {@code c} until Oracle refuses another with ORA-01000. */
    private static int exhaustCursors(Connection c) throws SQLException {
        int held = 0;
        while (held < 100_000) {
            try {
                PreparedStatement ps = c.prepareStatement("SELECT " + held + " FROM dual");
                ResultSet rs = ps.executeQuery();
                rs.next();
                held++;
            } catch (SQLException e) {
                if (e.getErrorCode() == 1000) {
                    return held;
                }
                throw e;
            }
        }
        fail("held 100000 cursors without ORA-01000: OPEN_CURSORS is not what this test assumes");
        return held;
    }

    private MetadataCursorGuard guard(CapturingLog log) {
        current = open();
        return new MetadataCursorGuard(log, () -> current, why -> {
            log.warning("TEST-RECONNECT " + why);
            current = open();
            return current;
        });
    }

    @Test
    void withoutCursorExhaustionNothingChanges() throws Exception {
        CapturingLog log = new CapturingLog();
        MetadataCursorGuard g = guard(log);
        for (int i = 0; i < 20; i++) {
            ReadOnlyRowSet rows = g.query("value " + i, QUERY, "v" + i);
            assertEquals("v" + i, rows.getString("V"));
        }
        assertFalse(g.closesEachTime(), "the flag is set only by ORA-01000");
        assertEquals(0, g.recoveries());
        assertEquals(0, log.count("ORA-01000"), log.lines.toString());
        assertEquals(0, log.count("TEST-RECONNECT"), log.lines.toString());
    }

    @Test
    void ora01000IsLoggedThenReconnectedRetriedAndClosedFromThenOn() throws Exception {
        CapturingLog log = new CapturingLog();
        MetadataCursorGuard g = guard(log);
        int held = exhaustCursors(current);
        assertTrue(held > 10, "the session held " + held + " cursors before ORA-01000");

        ReadOnlyRowSet rows = g.query("table APP.ORDERS: its primary key", QUERY, "after");

        assertEquals("after", rows.getString("V"), "the retry on the new connection answers");
        assertTrue(g.closesEachTime(), "ORA-01000 sets the flag");
        assertEquals(1, g.recoveries());
        assertEquals(1, log.count("ORA-01000 (maximum open cursors exceeded) while reading table APP.ORDERS: its primary key"),
                "the warning names what was being read: " + log.lines);
        assertEquals(1, log.count("TEST-RECONNECT ORA-01000 while reading table APP.ORDERS"), log.lines.toString());
        assertEquals(1, log.count("Retrying table APP.ORDERS: its primary key"), log.lines.toString());
        int warning = indexOf(log, "while reading table APP.ORDERS");
        int reconnect = indexOf(log, "TEST-RECONNECT");
        int retry = indexOf(log, "Retrying");
        assertTrue(warning < reconnect && reconnect < retry, "warning, then reconnect, then retry: " + log.lines);
    }

    @Test
    void afterRecoveryMoreQueriesThanTheLimitAllowsAllSucceed() throws Exception {
        CapturingLog log = new CapturingLog();
        MetadataCursorGuard g = guard(log);
        int held = exhaustCursors(current);
        g.query("the first table", QUERY, "first");
        // Before the fix each of these kept its cursor, so twice the limit could never finish.
        for (int i = 0; i < 2 * held; i++) {
            assertEquals("q" + i, g.query("table " + i, QUERY, "q" + i).getString("V"));
        }
        assertEquals(1, g.recoveries(), "closing each statement means no second exhaustion: " + log.lines);
    }

    private static int indexOf(CapturingLog log, String fragment) {
        for (int i = 0; i < log.lines.size(); i++) {
            if (log.lines.get(i).contains(fragment)) {
                return i;
            }
        }
        return -1;
    }
}
