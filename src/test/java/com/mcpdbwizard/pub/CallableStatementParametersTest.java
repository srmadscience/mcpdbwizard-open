package com.mcpdbwizard.pub;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Live-database tests for {@link CallableStatementParameters}, which represents the
 * IN / OUT parameters of a PL/SQL call. The class does not run the call itself — the
 * generated DAO code does — so each test mirrors that flow: {@code prepareCall} →
 * {@link CallableStatementParameters#bindParameters(CallableStatement)} →
 * {@code execute} → {@link CallableStatementParameters#unloadParameters(CallableStatement)}
 * → {@link StatementParameters2#getParam(int)}. Exercises a function return value, an
 * OUT parameter and the unset-parameter guard. Gated — see {@link DbTestSupport}.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CallableStatementParametersTest {

    private static final String FUNCTION = "OB_TEST_ADD";
    private static final String PROCEDURE = "OB_TEST_GREET";

    private final LogInterface log = new ConsoleLog();
    private Connection connection;

    @BeforeAll
    void setUp() throws SQLException {
        connection = DbTestSupport.requireConnection();
        DbTestSupport.execute(connection,
                "CREATE OR REPLACE FUNCTION " + FUNCTION + " (a IN NUMBER, b IN NUMBER) "
                        + "RETURN NUMBER AS BEGIN RETURN a + b; END;");
        DbTestSupport.execute(connection,
                "CREATE OR REPLACE PROCEDURE " + PROCEDURE + " (nm IN VARCHAR2, msg OUT VARCHAR2) "
                        + "AS BEGIN msg := 'Hello ' || nm; END;");
    }

    @AfterAll
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            DbTestSupport.executeQuietly(connection, "DROP FUNCTION " + FUNCTION);
            DbTestSupport.executeQuietly(connection, "DROP PROCEDURE " + PROCEDURE);
            connection.close();
        }
    }

    @Test
    void callsAFunctionAndReadsItsReturnValue() throws CSException, SQLException {
        String call = "{ ? = call " + FUNCTION + "(?, ?) }";
        CallableStatementParameters params = new CallableStatementParameters(call, log);
        params.setOutParam(1, Types.NUMERIC); // the function return
        params.setParam(2, 3);
        params.setParam(3, 4);

        try (CallableStatement statement = connection.prepareCall(call)) {
            params.bindParameters(statement);
            statement.execute();
            params.unloadParameters(statement);
        }

        Object result = params.getParam(1);
        assertInstanceOf(BigDecimal.class, result);
        assertEquals(7, ((BigDecimal) result).intValue());
    }

    @Test
    void callsAProcedureAndReadsAnOutParameter() throws CSException, SQLException {
        String call = "{ call " + PROCEDURE + "(?, ?) }";
        CallableStatementParameters params = new CallableStatementParameters(call, log);
        params.setParam(1, "World");
        params.setOutParam(2, Types.VARCHAR);

        try (CallableStatement statement = connection.prepareCall(call)) {
            params.bindParameters(statement);
            statement.execute();
            params.unloadParameters(statement);
        }

        assertEquals("Hello World", params.getParam(2));
    }

    @Test
    void bindingWithAnUnsetParameterIsRejected() throws CSException, SQLException {
        String call = "{ call " + PROCEDURE + "(?, ?) }";
        CallableStatementParameters params = new CallableStatementParameters(call, log);
        params.setParam(1, "World");
        // Parameter 2 deliberately left unset.

        try (CallableStatement statement = connection.prepareCall(call)) {
            assertThrows(CSException.class, () -> params.bindParameters(statement));
        }
    }

    @Test
    void settingAnOutOfRangeParameterIsRejected() {
        CallableStatementParameters params = new CallableStatementParameters(2, log);
        assertThrows(CSException.class, () -> params.setParam(3, "too far"));
    }
}
