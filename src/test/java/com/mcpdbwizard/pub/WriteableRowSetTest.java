package com.mcpdbwizard.pub;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Live-database tests for {@link WriteableRowSet}. The row set is sourced from a real
 * query via {@link ReadOnlyRowSet#getWriteableRowSet()}; the mutations themselves
 * (set / add / delete / replace) operate on the in-memory copy and are verified there.
 * Gated — see {@link DbTestSupport}.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WriteableRowSetTest {

    private static final String TABLE = "OB_WRS_DATA";

    private final LogInterface log = new ConsoleLog();
    private Connection connection;

    @BeforeAll
    void setUp() throws SQLException {
        connection = DbTestSupport.requireConnection();
        DbTestSupport.executeQuietly(connection, "DROP TABLE " + TABLE);
        DbTestSupport.execute(connection,
                "CREATE TABLE " + TABLE + " (id NUMBER(10) PRIMARY KEY, name VARCHAR2(40))");
        for (int i = 1; i <= 3; i++) {
            DbTestSupport.execute(connection,
                    "INSERT INTO " + TABLE + " (id, name) VALUES (" + i + ", 'row" + i + "')");
        }
        connection.commit();
    }

    @AfterAll
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            DbTestSupport.executeQuietly(connection, "DROP TABLE " + TABLE);
            connection.close();
        }
    }

    private WriteableRowSet writeableRows() throws CSException {
        ReadOnlyRowSet source = new QueryStatement(
                "SELECT id, name FROM " + TABLE + " ORDER BY id", log, connection).execute();
        return source.getWriteableRowSet();
    }

    @Test
    void isDerivedFromTheReadOnlyRowSetWithSameShape() throws CSException {
        WriteableRowSet rows = writeableRows();
        assertEquals(3, rows.size());
        assertEquals(2, rows.width());
    }

    @Test
    void setStringUpdatesTheCurrentRow() throws CSException {
        WriteableRowSet rows = writeableRows();
        rows.first();
        rows.setString("NAME", "edited");
        assertEquals("edited", rows.getString("NAME"));

        // The change is isolated to the current row.
        rows.nextRow();
        assertEquals("row2", rows.getString("NAME"));
    }

    @Test
    void addNewRowGrowsTheRowSet() throws CSException {
        WriteableRowSet rows = writeableRows();

        rows.addNewRow(new Object[]{new BigDecimal(4), "row4"});

        assertEquals(4, rows.size());
        assertTrue(rows.last());
        assertEquals(4, rows.getInt("ID"));
        assertEquals("row4", rows.getString("NAME"));
    }

    @Test
    void deleteCurrentRowShrinksTheRowSet() throws CSException {
        WriteableRowSet rows = writeableRows();

        rows.first();
        rows.deleteCurrentRow();

        assertEquals(2, rows.size());
        rows.first();
        // The former second row is now first.
        assertEquals(2, rows.getInt("ID"));
    }

    @Test
    void setCurrentRowReplacesAllColumns() throws CSException {
        WriteableRowSet rows = writeableRows();

        rows.first();
        rows.setCurrentRow(new Object[]{new BigDecimal(42), "replaced"});

        assertEquals(42, rows.getInt("ID"));
        assertEquals("replaced", rows.getString("NAME"));
        assertEquals(3, rows.size());
    }
}
