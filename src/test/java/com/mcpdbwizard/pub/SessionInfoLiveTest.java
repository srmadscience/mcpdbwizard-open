package com.mcpdbwizard.pub;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Live-database tests for {@link SessionInfo}: that the module and action really do arrive on the
 * session. Gated on a reachable Oracle instance — see {@link DbTestSupport} — so it skips rather
 * than fails where there is none. The truncation rules are database-free and live in
 * {@link SessionInfoTest}.
 * <p>
 * These read back through {@code SYS_CONTEXT('USERENV', ...)} rather than {@code V$SESSION}
 * deliberately: it reports the same two values for the current session and needs no {@code SELECT}
 * grant on a {@code V$} view, which an ordinary application account frequently does not have.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SessionInfoLiveTest {

    private Connection connection;

    @BeforeAll
    void setUp() {
        connection = DbTestSupport.requireConnection();
    }

    @AfterAll
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    void recordsModuleAndActionOnTheSession() throws SQLException {
        SessionInfo.setModule(connection, "McpDbWizardTestModule", "McpDbWizardTestAction");

        assertEquals("McpDbWizardTestModule", sysContext("MODULE"));
        assertEquals("McpDbWizardTestAction", sysContext("ACTION"));
    }

    @Test
    void clearsTheActionWhenGivenNull() throws SQLException {
        SessionInfo.setModule(connection, "McpDbWizardTestModule", "McpDbWizardTestAction");
        SessionInfo.setModule(connection, "McpDbWizardTestModule", null);

        // This is the shape the generated factory uses: a module and no action. A stale action left
        // behind by an earlier caller would misdescribe every session that followed it.
        assertEquals("McpDbWizardTestModule", sysContext("MODULE"));
        assertNull(sysContext("ACTION"));
    }

    @Test
    void anOverlongModuleArrivesTruncatedRatherThanRejected() throws SQLException {
        String theLongName = "M".repeat(SessionInfo.MODULE_MAX_LENGTH + 30);

        SessionInfo.setModule(connection, theLongName, null);

        // Oracle would have truncated it anyway; what is pinned here is that our limit IS its limit,
        // so a long name is clipped where we said it would be and not somewhere unexpected.
        assertEquals("M".repeat(SessionInfo.MODULE_MAX_LENGTH), sysContext("MODULE"));
    }

    /** Read one USERENV attribute of the current session. */
    private String sysContext(String theAttribute) throws SQLException {
        try (Statement theStatement = connection.createStatement();
             ResultSet theRows = theStatement.executeQuery(
                     "SELECT SYS_CONTEXT('USERENV', '" + theAttribute + "') FROM DUAL")) {
            theRows.next();
            return theRows.getString(1);
        }
    }
}
