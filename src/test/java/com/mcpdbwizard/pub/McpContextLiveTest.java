package com.mcpdbwizard.pub;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code MCP} application context, live: what {@code app/db/mcp-context/install.sql} creates,
 * driven through {@link McpContextParams#applyToSession}, the call the generated server makes.
 *
 * <p><b>It FAILS, rather than skips, when the database is reachable and the context is not
 * installed.</b> Every box in the test estate has it (installed by {@code testdata.sh}), so a skip
 * there would hide a box that lost it. Run {@code install.sql} and {@code grant.sql <test user>}
 * as a DBA to make it pass on your own database.
 *
 * <p>The tests that matter most are the two that prove this is a CONTROL and not a convention: the
 * test account cannot write the namespace except through the package, and the package refuses
 * what the Java checks refuse even when those checks are skipped.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class McpContextLiveTest {

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

    private static Map<String, String> values(String... thePairs) {
        Map<String, String> theValues = new LinkedHashMap<>();
        for (int i = 0; i < thePairs.length; i += 2) {
            theValues.put(thePairs[i], thePairs[i + 1]);
        }
        return theValues;
    }

    @Test
    void aValueSetIsWhatSysContextReturns() throws SQLException {
        McpContextParams.applyToSession(connection, values("CUSTOMER_ID", "42", "REGION", "north"));

        assertEquals("42", mcp(connection, "CUSTOMER_ID"));
        assertEquals("north", mcp(connection, "REGION"), "values are case-preserved");
        assertEquals("42", mcp(connection, "customer_id"), "attribute lookup is case-insensitive");
    }

    /** The pooling guarantee: whatever the session held before, it holds exactly the new set after. */
    @Test
    void everyApplyReplacesTheWholeSet() throws SQLException {
        McpContextParams.applyToSession(connection, values("CUSTOMER_ID", "42", "REGION", "north"));
        McpContextParams.applyToSession(connection, values("CUSTOMER_ID", "43"));

        assertEquals("43", mcp(connection, "CUSTOMER_ID"));
        assertNull(mcp(connection, "REGION"), "a value from the previous apply survived");
    }

    @Test
    void anEmptyApplyStillClears() throws SQLException {
        McpContextParams.applyToSession(connection, values("CUSTOMER_ID", "42"));
        McpContextParams.applyToSession(connection, values());

        assertNull(mcp(connection, "CUSTOMER_ID"));
    }

    @Test
    void theContextBelongsToTheSessionNotTheAccount() throws SQLException {
        try (Connection theOther = DbTestSupport.requireConnection()) {
            McpContextParams.applyToSession(connection, values("CUSTOMER_ID", "42"));
            McpContextParams.applyToSession(theOther, values("CUSTOMER_ID", "77"));

            assertEquals("42", mcp(connection, "CUSTOMER_ID"));
            assertEquals("77", mcp(theOther, "CUSTOMER_ID"));
        }
    }

    @Test
    void theFullValueLengthRoundTrips() throws SQLException {
        String theLongest = "v".repeat(McpContextParams.MAX_VALUE_BYTES);
        McpContextParams.applyToSession(connection, values("CUSTOMER_ID", theLongest));

        assertEquals(theLongest, mcp(connection, "CUSTOMER_ID"));
    }

    /** THE property: the account cannot write the namespace except through our package. */
    @Test
    void theAccountCannotSetTheNamespaceDirectly() {
        SQLException theFailure = assertThrows(SQLException.class, () -> {
            try (CallableStatement theCall = connection.prepareCall(
                    "begin dbms_session.set_context('MCP', 'CUSTOMER_ID', '99'); end;")) {
                theCall.execute();
            }
        });
        assertTrue(theFailure.getMessage().contains("ORA-01031"), theFailure.getMessage());
    }

    /**
     * The package re-checks what the Java side checks, so a caller that skipped those checks still
     * cannot set a NULL -- Oracle stores '' as NULL, so this is also the empty-value case.
     */
    @Test
    void thePackageRefusesAnEmptyValueOnItsOwn() {
        SQLException theFailure = assertThrows(SQLException.class,
                () -> McpContextParams.applyToSession(connection, values("CUSTOMER_ID", "")));
        assertTrue(theFailure.getMessage().contains("ORA-20902"), theFailure.getMessage());
    }

    @Test
    void thePackageRefusesABadNameOnItsOwn() {
        SQLException theFailure = assertThrows(SQLException.class,
                () -> McpContextParams.applyToSession(connection, values("CUSTOMER-ID", "42")));
        assertTrue(theFailure.getMessage().contains("ORA-20901"), theFailure.getMessage());
    }

    /** A refused set must not leave the previous caller's values behind either. */
    @Test
    void aRefusedApplyHasStillClearedFirst() throws SQLException {
        McpContextParams.applyToSession(connection, values("CUSTOMER_ID", "42"));
        assertThrows(SQLException.class,
                () -> McpContextParams.applyToSession(connection, values("REGION", "")));

        assertNull(mcp(connection, "CUSTOMER_ID"));
    }

    private static String mcp(Connection theConnection, String theName) throws SQLException {
        try (Statement theStatement = theConnection.createStatement();
             ResultSet theRows = theStatement.executeQuery(
                     "SELECT SYS_CONTEXT('MCP', '" + theName + "', 4000) FROM DUAL")) {
            theRows.next();
            return theRows.getString(1);
        }
    }
}
