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
 * Live-database tests for {@link ReadOnlyRowSet}, the class that materialises a JDBC
 * {@code ResultSet} into an in-memory, navigable, type-converting row set. Covers the
 * typed getters, row navigation, NULL handling and the {@code CS*Exception} surface
 * that callers rely on. Gated on a reachable Oracle instance — see {@link DbTestSupport}.
 * <p>
 * The fixture has two rows: a fully-populated row and an all-NULL row, queried in a
 * deterministic order so {@code first()} / {@code last()} land predictably.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReadOnlyRowSetTest {

    private static final String TABLE = "OB_ROWS_DATA";

    private final LogInterface log = new ConsoleLog();
    private Connection connection;

    @BeforeAll
    void setUp() throws SQLException {
        connection = DbTestSupport.requireConnection();
        DbTestSupport.executeQuietly(connection, "DROP TABLE " + TABLE);
        DbTestSupport.execute(connection,
                "CREATE TABLE " + TABLE + " ("
                        + " id NUMBER(10),"
                        + " name VARCHAR2(50),"
                        + " price NUMBER(10,2),"
                        + " qty NUMBER(5),"
                        + " created DATE,"
                        + " ts TIMESTAMP,"
                        + " note CLOB)");
        DbTestSupport.execute(connection,
                "INSERT INTO " + TABLE + " VALUES (1, 'Widget', 9.99, 5,"
                        + " TO_DATE('2020-01-15 13:45:30','YYYY-MM-DD HH24:MI:SS'),"
                        + " TO_TIMESTAMP('2021-06-30 08:09:10','YYYY-MM-DD HH24:MI:SS'),"
                        + " 'hello clob')");
        DbTestSupport.execute(connection,
                "INSERT INTO " + TABLE
                        + " VALUES (2, NULL, NULL, NULL, NULL, NULL, NULL)");
        connection.commit();
    }

    @AfterAll
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            DbTestSupport.executeQuietly(connection, "DROP TABLE " + TABLE);
            connection.close();
        }
    }

    /** Run a query and return the populated row set (LOBs left as files). */
    private ReadOnlyRowSet query(String sql) throws CSException {
        return new QueryStatement(sql, log, connection).execute();
    }

    private ReadOnlyRowSet allRows() throws CSException {
        return query("SELECT id, name, price, qty, created, ts, note FROM " + TABLE + " ORDER BY id");
    }

    // ---- shape & column metadata ----------------------------------------

    @Test
    void reportsRowAndColumnCounts() throws CSException {
        ReadOnlyRowSet rows = allRows();
        assertEquals(2, rows.size());
        assertEquals(7, rows.width());
    }

    @Test
    void mapsColumnNamesToIdsAndBack() throws CSException {
        ReadOnlyRowSet rows = allRows();
        int priceId = rows.getColumnId("PRICE");
        assertTrue(priceId >= 0);
        assertEquals("PRICE", rows.getColumnName(priceId));
        assertEquals(-1, rows.getColumnId("NO_SUCH_COLUMN"));
        assertTrue(rows.checkColumnName("ID"));
        assertFalse(rows.checkColumnName("NOPE"));
        assertEquals(7, rows.getColumnNamesAsStringArray().length);
    }

    // ---- navigation ------------------------------------------------------

    @Test
    void navigatesRowsAndClampsAtTheEdges() throws CSException {
        ReadOnlyRowSet rows = allRows();

        assertTrue(rows.first());
        assertEquals(0, rows.getCurrentRowNumber());

        assertTrue(rows.nextRow());
        assertEquals(1, rows.getCurrentRowNumber());

        // Already on the last row: nextRow returns false and the position is clamped.
        assertFalse(rows.nextRow());
        assertEquals(1, rows.getCurrentRowNumber());

        assertTrue(rows.last());
        assertEquals(1, rows.getCurrentRowNumber());

        assertTrue(rows.prevRow());
        assertEquals(0, rows.getCurrentRowNumber());

        assertFalse(rows.setCurrentRowNumber(99));
        assertFalse(rows.setCurrentRowNumber(-1));
    }

    @Test
    void getCurrentRowReturnsAFullWidthArray() throws CSException {
        ReadOnlyRowSet rows = allRows();
        rows.first();
        Object[] row = rows.getCurrentRow();
        assertEquals(7, row.length);
    }

    // ---- typed getters on the populated row ------------------------------

    @Test
    void readsNumericColumnsAsEveryNumericType() throws CSException {
        ReadOnlyRowSet rows = allRows();
        rows.first();

        assertEquals(1, rows.getInt("ID"));
        assertEquals(1L, rows.getLong("ID"));
        assertEquals(0, new BigDecimal("9.99").compareTo(rows.getBigDecimal("PRICE")));
        assertEquals(9.99, rows.getDouble("PRICE"), 0.0001);
        assertEquals(5, rows.getInt("QTY"));
        assertEquals(Integer.valueOf(5), rows.getIntegerObj("QTY"));
    }

    @Test
    void readsTextColumnAsString() throws CSException {
        ReadOnlyRowSet rows = allRows();
        rows.first();
        assertEquals("Widget", rows.getString("NAME"));
    }

    @Test
    void readsDateAndTimestampColumnsAsTimestamps() throws CSException {
        ReadOnlyRowSet rows = allRows();
        rows.first();

        // Both DATE and TIMESTAMP columns are materialised as java.sql.Timestamp and
        // carry the stored date/time components.
        assertTrue(rows.getTimestamp("CREATED").toString().startsWith("2020-01-15 13:45:30"));
        assertTrue(rows.getTimestamp("TS").toString().startsWith("2021-06-30 08:09:10"));
    }

    @Test
    void readsDateColumnAsSqlDate() throws CSException {
        // A DATE column is materialised as a java.sql.Timestamp; getDate() converts it via
        // the common java.util.Date supertype and returns the date portion. (Regression
        // guard: getDate() previously cast straight to java.sql.Date and threw
        // ClassCastException for every DATE column.)
        ReadOnlyRowSet rows = allRows();
        rows.first();
        assertEquals("2020-01-15", rows.getDate("CREATED").toString());
    }

    // ---- NULL handling ---------------------------------------------------

    @Test
    void recognisesNullAndNonNullColumns() throws CSException {
        ReadOnlyRowSet rows = allRows();
        rows.last(); // the all-NULL row

        assertTrue(rows.isNull("NAME"));
        assertNull(rows.getString("NAME"));
        assertNull(rows.getIntegerObj("QTY"));

        rows.first();
        assertFalse(rows.isNull("NAME"));
    }

    @Test
    void primitiveGetterOnNullThrows() throws CSException {
        ReadOnlyRowSet rows = allRows();
        rows.last(); // the all-NULL row
        assertThrows(CSAttemptToGetNullException.class, () -> rows.getInt("QTY"));
    }

    // ---- error surface ---------------------------------------------------

    @Test
    void unknownColumnThrowsInvalidColumnId() throws CSException {
        ReadOnlyRowSet rows = allRows();
        rows.first();
        assertThrows(CSInvalidColumnIdException.class, () -> rows.getInt("NO_SUCH_COLUMN"));
    }

    @Test
    void incompatibleTypeConversionThrowsCastException() throws CSException {
        ReadOnlyRowSet rows = allRows();
        rows.first();
        // A NUMBER column cannot be read as a date.
        assertThrows(CSDBInvalidDatatypeCastException.class, () -> rows.getDate("ID"));
    }

    @Test
    void gettersOnAnEmptyRowSetThrowNoData() throws CSException {
        ReadOnlyRowSet empty = query("SELECT id FROM " + TABLE + " WHERE id = -1");
        assertEquals(0, empty.size());
        assertThrows(CSNoDataInRowSetException.class, () -> empty.getInt("ID"));
    }

    // ---- CLOB retrieval --------------------------------------------------

    @Test
    void readsClobContentWhenMaterialisedAsBytes() throws CSException {
        QueryStatement query = new QueryStatement(
                "SELECT note FROM " + TABLE + " WHERE id = 1", log, connection);
        // Keep the CLOB in memory (as a char[]) instead of spilling it to a temp file.
        query.setUseByteArraysForLongsAndLOBS(true);

        ReadOnlyRowSet rows = query.execute();
        rows.first();

        assertEquals("hello clob", rows.getString("NOTE"));
    }
}
