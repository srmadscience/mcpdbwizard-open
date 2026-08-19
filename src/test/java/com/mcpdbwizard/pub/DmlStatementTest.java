package com.mcpdbwizard.pub;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Live-database tests for {@link DmlStatement}, the connection-aware base of
 * {@link QueryStatement}. It is abstract but declares no abstract methods, so the
 * tests drive it through a trivial anonymous subclass and run real INSERT / UPDATE /
 * DELETE through {@link DmlStatement#getUnderlyingStatement()} together with the
 * inherited parameter binding. Also covers statement classification and the
 * connection / Oracle-resource lifecycle. Gated — see {@link DbTestSupport}.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DmlStatementTest {

    private static final String TABLE = "OB_DML_TGT";

    private final LogInterface log = new ConsoleLog();
    private Connection connection;

    /** Concrete handle on the abstract class (no abstract methods to implement). */
    private DmlStatement dml(String sql) {
        return new DmlStatement(sql, log, connection) {
        };
    }

    @BeforeAll
    void setUp() throws SQLException {
        connection = DbTestSupport.requireConnection();
        DbTestSupport.executeQuietly(connection, "DROP TABLE " + TABLE);
        DbTestSupport.execute(connection,
                "CREATE TABLE " + TABLE + " (id NUMBER(10) PRIMARY KEY, name VARCHAR2(40))");
        connection.commit();
    }

    @AfterAll
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            DbTestSupport.executeQuietly(connection, "DROP TABLE " + TABLE);
            connection.close();
        }
    }

    private int countRows() throws CSException {
        ReadOnlyRowSet rows = new QueryStatement(
                "SELECT COUNT(*) AS c FROM " + TABLE, log, connection).execute();
        rows.first();
        return rows.getInt("C");
    }

    @Test
    void classifiesStatementType() {
        assertEquals(SqlUtils.INSERT, dml("INSERT INTO " + TABLE + " VALUES (?, ?)").statementType);
        assertEquals(SqlUtils.UPDATE, dml("UPDATE " + TABLE + " SET name = ?").statementType);
        assertEquals(SqlUtils.DELETE, dml("DELETE FROM " + TABLE).statementType);
    }

    @Test
    void runsAFullInsertUpdateDeleteCycle() throws CSException, SQLException {
        // INSERT with bound parameters.
        DmlStatement insert = dml("INSERT INTO " + TABLE + " (id, name) VALUES (?, ?)");
        insert.setParam(1, 100);
        insert.setParam(2, "original");
        PreparedStatement insertPs = (PreparedStatement) insert.getUnderlyingStatement();
        insert.bindParameters(insertPs);
        assertEquals(1, insertPs.executeUpdate());
        connection.commit();

        ReadOnlyRowSet afterInsert =
                new QueryStatement("SELECT name FROM " + TABLE + " WHERE id = 100", log, connection).execute();
        afterInsert.first();
        assertEquals("original", afterInsert.getString("NAME"));

        // UPDATE with bound parameters.
        DmlStatement update = dml("UPDATE " + TABLE + " SET name = ? WHERE id = ?");
        update.setParam(1, "changed");
        update.setParam(2, 100);
        PreparedStatement updatePs = (PreparedStatement) update.getUnderlyingStatement();
        update.bindParameters(updatePs);
        assertEquals(1, updatePs.executeUpdate());
        connection.commit();

        ReadOnlyRowSet afterUpdate =
                new QueryStatement("SELECT name FROM " + TABLE + " WHERE id = 100", log, connection).execute();
        afterUpdate.first();
        assertEquals("changed", afterUpdate.getString("NAME"));

        // DELETE with a bound parameter.
        DmlStatement delete = dml("DELETE FROM " + TABLE + " WHERE id = ?");
        delete.setParam(1, 100);
        PreparedStatement deletePs = (PreparedStatement) delete.getUnderlyingStatement();
        delete.bindParameters(deletePs);
        assertEquals(1, deletePs.executeUpdate());
        connection.commit();

        assertEquals(0, countRows());
    }

    @Test
    void tracksConnectionAndResourceState() throws CSException {
        DmlStatement statement = dml("INSERT INTO " + TABLE + " (id, name) VALUES (?, ?)");

        assertTrue(statement.connectionIsUsable());
        assertTrue(statement.hasResources());

        // Creating the prepared statement is reported the first time only.
        assertTrue(statement.createPreparedStatement());
        assertFalse(statement.createPreparedStatement());

        statement.freeConnection();
        assertFalse(statement.connectionIsUsable());
        assertFalse(statement.hasResources());
    }

    @Test
    void aStatementWithNoConnectionIsNotUsable() {
        DmlStatement orphan = new DmlStatement("DELETE FROM " + TABLE, log) {
        };
        assertFalse(orphan.connectionIsUsable());
        assertFalse(orphan.hasResources());
    }
}
