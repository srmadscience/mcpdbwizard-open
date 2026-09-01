package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link OpenCursorCheck}, which warns when a config publishes more tools than the
 * session has cursors for.
 *
 * <p>No database. The check makes exactly one query and then does arithmetic, so a proxy that
 * answers that query exercises everything that matters — and lets the privilege failure be tested,
 * which is the case a live test could not produce on demand without revoking a grant.
 *
 * <p>Deliberately NOT Mockito: {@code java.sql} interfaces are wide, the assertions here are about
 * what was logged rather than about interaction order, and this project has already been bitten by
 * Mockito's JDK sensitivity. A twenty-line proxy has no such dependency.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class OpenCursorCheckTest {

    /** Captures what the check said, and at what level. */
    private static final class CapturedLog implements InvocationHandler {
        final List<String> warnings = new ArrayList<>();
        final List<String> errors = new ArrayList<>();

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (args != null && args.length > 0 && args[0] instanceof String) {
                if ("warning".equals(method.getName())) {
                    warnings.add((String) args[0]);
                } else if ("error".equals(method.getName())) {
                    errors.add((String) args[0]);
                }
            }
            return defaultFor(method.getReturnType());
        }

        LogInterface asLog() {
            return (LogInterface) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{LogInterface.class}, this);
        }
    }

    private static Object defaultFor(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return Boolean.FALSE;
        }
        if (type == void.class) {
            return null;
        }
        return 0;
    }

    /**
     * A connection whose one query answers with {@code theOpenCursors}, or throws when it is null —
     * which is what an application schema without a grant on V$PARAMETER actually does.
     */
    private static Connection connectionReporting(String theOpenCursors) {
        InvocationHandler results = (p, m, a) -> switch (m.getName()) {
            case "next" -> Boolean.TRUE;
            case "getString" -> theOpenCursors;
            default -> defaultFor(m.getReturnType());
        };
        InvocationHandler statement = (p, m, a) -> {
            if ("executeQuery".equals(m.getName())) {
                if (theOpenCursors == null) {
                    throw new SQLException("ORA-00942: table or view does not exist");
                }
                return Proxy.newProxyInstance(OpenCursorCheckTest.class.getClassLoader(),
                        new Class<?>[]{ResultSet.class}, results);
            }
            return defaultFor(m.getReturnType());
        };
        InvocationHandler connection = (p, m, a) -> {
            if ("prepareStatement".equals(m.getName())) {
                return Proxy.newProxyInstance(OpenCursorCheckTest.class.getClassLoader(),
                        new Class<?>[]{PreparedStatement.class}, statement);
            }
            return defaultFor(m.getReturnType());
        };
        return (Connection) Proxy.newProxyInstance(OpenCursorCheckTest.class.getClassLoader(),
                new Class<?>[]{Connection.class}, connection);
    }

    @Test
    void anOrdinaryConfigSaysNothing() {
        // The common case, and the one that must stay silent: a warning on every start-up would be
        // trained away long before it applied to anybody.
        CapturedLog theLog = new CapturedLog();
        OpenCursorCheck.warnIfToolsExceedCursors(connectionReporting("300"), 27, theLog.asLog());
        assertEquals(List.of(), theLog.warnings);
        assertEquals(List.of(), theLog.errors);
    }

    @Test
    void itWarnsBeforeTheCountReachesTheLimitRatherThanAtIt() {
        // 237 tools against 300 cursors LOOKS safe by counting and is not: a tool costs one cursor
        // at best and three when it returns a REF CURSOR or moves a LOB. Measured average 1.29, so
        // parity between the two numbers is already over the line.
        CapturedLog theLog = new CapturedLog();
        OpenCursorCheck.warnIfToolsExceedCursors(connectionReporting("300"), 237, theLog.asLog());
        assertEquals(1, theLog.warnings.size(), "237 of 300 must warn, not pass silently");
        assertEquals(List.of(), theLog.errors, "under the limit is a warning, not an error");
        assertTrue(theLog.warnings.get(0).contains("237"));
        assertTrue(theLog.warnings.get(0).contains("open_cursors is 300"));
        assertTrue(theLog.warnings.get(0).contains("ORA-01000"),
                "the message must name the error it predicts, or nobody will connect the two");
    }

    @Test
    void reachingTheLimitIsAnErrorNotAWarning() {
        CapturedLog theLog = new CapturedLog();
        OpenCursorCheck.warnIfToolsExceedCursors(connectionReporting("300"), 539, theLog.asLog());
        assertEquals(List.of(), theLog.warnings);
        assertEquals(1, theLog.errors.size());
        assertTrue(theLog.errors.get(0).contains("539"));
    }

    @Test
    void aRaisedOpenCursorsSettingIsRespected() {
        // The whole point of reading the parameter rather than assuming it: a site that has already
        // raised open_cursors must not be nagged about a config that now fits.
        CapturedLog theLog = new CapturedLog();
        OpenCursorCheck.warnIfToolsExceedCursors(connectionReporting("4000"), 539, theLog.asLog());
        assertEquals(List.of(), theLog.warnings);
        assertEquals(List.of(), theLog.errors);
    }

    @Test
    void whenTheParameterCannotBeReadTheLimitIsReportedUNKNOWN() {
        // It must NOT fall back to Oracle's default and compare against it. A site that raised
        // open_cursors -- the common adjustment -- would then be warned on every start-up about a
        // config that fits, and an untrue warning nobody can silence is how a check gets ignored.
        CapturedLog theLog = new CapturedLog();
        OpenCursorCheck.warnIfToolsExceedCursors(connectionReporting(null), 539, theLog.asLog());
        assertEquals(1, theLog.warnings.size());
        assertEquals(List.of(), theLog.errors, "an unknown limit is not a breach; it is unknown");
        String said = theLog.warnings.get(0);
        assertTrue(said.contains("UNKNOWN"));
        assertTrue(said.contains("MAY BE EXCEEDED"));
        assertTrue(said.contains("539"));
        assertTrue(said.contains("ORA-01000"), "it must name the error it predicts");
        assertTrue(said.contains("SELECT_CATALOG_ROLE"), "and how to make it readable");
    }

    @Test
    void anUnknownLimitIsReportedWhateverTheToolCount() {
        // Deliberate: without the limit there is no threshold to be under. A five-tool config is
        // only safe if open_cursors exceeds five, and that is precisely the fact we do not have.
        CapturedLog theLog = new CapturedLog();
        OpenCursorCheck.warnIfToolsExceedCursors(connectionReporting(null), 5, theLog.asLog());
        assertEquals(1, theLog.warnings.size());
        assertTrue(theLog.warnings.get(0).contains("UNKNOWN"));
    }

    @Test
    void theOracleDefaultIsQuotedForTheReaderButNeverUsedAsALimit() {
        // 300 appears in the unknown message as something to compare against. If it were being used
        // as a fallback LIMIT, 539 tools would have come out as an error rather than a warning --
        // which is the assertion above -- and 210 tools would warn here. It must not.
        CapturedLog theLog = new CapturedLog();
        OpenCursorCheck.warnIfToolsExceedCursors(connectionReporting("300"), 210, theLog.asLog());
        assertEquals(1, theLog.warnings.size(), "210 of a KNOWN 300 is 70%, so this one does warn");

        CapturedLog theSecond = new CapturedLog();
        OpenCursorCheck.warnIfToolsExceedCursors(connectionReporting(null), 210, theSecond.asLog());
        assertTrue(theSecond.warnings.get(0).contains("UNKNOWN"),
                "the same count against an UNREADABLE limit must not be compared with 300 at all");
    }

    @Test
    void nothingToCheckIsNotAFailure() {
        CapturedLog theLog = new CapturedLog();
        OpenCursorCheck.warnIfToolsExceedCursors(null, 539, theLog.asLog());
        OpenCursorCheck.warnIfToolsExceedCursors(connectionReporting("300"), 0, theLog.asLog());
        assertEquals(List.of(), theLog.warnings);
        assertEquals(List.of(), theLog.errors);
    }
}
