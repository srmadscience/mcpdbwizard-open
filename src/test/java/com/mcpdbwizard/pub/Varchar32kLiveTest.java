package com.mcpdbwizard.pub;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Live-database tests for <b>extended (32k) datatypes</b> — {@code VARCHAR2(32767)}
 * and {@code RAW(32767)} columns, available only when the server runs
 * {@code MAX_STRING_SIZE = EXTENDED} (12c+). The {@code pub} library carries no
 * length limits of its own; these tests prove the claim that extended columns
 * simply ride the existing TEXT / RAW paths, exercising the interesting
 * boundaries: 4000 (the STANDARD VARCHAR2 cap), 4001 (where the driver switches
 * to out-of-line binding), and 32767 (the EXTENDED cap).
 * <p>
 * Self-provisioning and <b>double-gated</b>: skipped when no database is reachable
 * ({@link DbTestSupport}), and skipped — via the ORA-00910 the CREATE TABLE raises —
 * when the server runs {@code MAX_STRING_SIZE = STANDARD} (all the standing test
 * boxes' default PDBs do). Point it at an EXTENDED PDB, e.g. EXTPDB1 on the 26ai
 * box (created by {@code sql/make_extpdb1.sql}):
 * <pre>
 *   ORINDA_TEST_URL='jdbc:oracle:thin:@203.0.113.10:1521/extpdb1' \
 *   ORINDA_TEST_USER=appschema ORINDA_TEST_PASSWORD=appschema \
 *     mvn -Dtest=Varchar32kLiveTest test
 * </pre>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Varchar32kLiveTest {

    private static final String TABLE = "OB_VARCHAR_32K";

    /** ORA-00910: specified length too long for its datatype. */
    private static final int ORA_LENGTH_TOO_LONG = 910;

    private static final int STANDARD_CAP = 4000;
    private static final int EXTENDED_CAP = 32767;

    private final LogInterface log = new ConsoleLog();
    private Connection connection;

    /** Deterministic text of the given length, so equality checks content, not just length. */
    private static String patternedText(int length) {
        StringBuilder b = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            b.append((char) ('A' + (i % 26)));
        }
        return b.toString();
    }

    /** Deterministic bytes of the given length. */
    private static byte[] patternedBytes(int length) {
        byte[] b = new byte[length];
        for (int i = 0; i < length; i++) {
            b[i] = (byte) i;
        }
        return b;
    }

    @BeforeAll
    void setUp() throws SQLException, CSException {
        connection = DbTestSupport.requireConnection();
        DbTestSupport.executeQuietly(connection, "DROP TABLE " + TABLE + " PURGE");
        try {
            DbTestSupport.execute(connection,
                    "CREATE TABLE " + TABLE + " ("
                            + " id NUMBER PRIMARY KEY,"
                            + " big_text VARCHAR2(32767),"
                            + " big_raw RAW(32767))");
        } catch (SQLException e) {
            if (e.getErrorCode() == ORA_LENGTH_TOO_LONG) {
                Assumptions.abort("Server runs MAX_STRING_SIZE=STANDARD "
                        + "(ORA-00910 creating a VARCHAR2(32767) column) - skipping");
            }
            throw e;
        }
        // Boundary rows, inserted through the pub bind path (StatementParameters2),
        // which is exactly the surface the generated code writes through.
        insertRow(1, patternedText(STANDARD_CAP), null);
        insertRow(2, patternedText(STANDARD_CAP + 1), null);
        insertRow(3, patternedText(EXTENDED_CAP), patternedBytes(EXTENDED_CAP));
        insertRow(4, null, null);
        connection.commit();
    }

    @AfterAll
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            DbTestSupport.executeQuietly(connection, "DROP TABLE " + TABLE + " PURGE");
            connection.close();
        }
    }

    private void insertRow(int id, String text, byte[] raw) throws CSException, SQLException {
        DmlStatement insert = new DmlStatement(
                "INSERT INTO " + TABLE + " (id, big_text, big_raw) VALUES (?, ?, ?)",
                log, connection) {
        };
        insert.setParam(1, id);
        insert.setParam(2, text);
        insert.setParam(3, raw);
        PreparedStatement ps = (PreparedStatement) insert.getUnderlyingStatement();
        insert.bindParameters(ps);
        assertEquals(1, ps.executeUpdate());
    }

    /** Fetch the single row with the given id. */
    private ReadOnlyRowSet row(int id) throws CSException {
        ReadOnlyRowSet rows = new QueryStatement(
                "SELECT id, big_text, big_raw FROM " + TABLE + " WHERE id = " + id,
                log, connection).execute();
        assertTrue(rows.first(), "expected a row with id=" + id);
        return rows;
    }

    // ---- classification: extended columns ride the existing paths ---------

    @Test
    void extendedColumnsClassifyAsTextAndBinary() throws CSException {
        ReadOnlyRowSet rows = row(1);
        String[] typeNames = rows.getColumnOracleDatatypeNames();

        assertEquals(SqlUtils.ORACLE_TEXT_DATATYPE,
                SqlUtils.getUnderlyingOracleDatatype(typeNames[1]),
                "VARCHAR2(32767) -> TEXT");
        assertEquals(SqlUtils.ORACLE_BINARY_DATATYPE,
                SqlUtils.getUnderlyingOracleDatatype(typeNames[2]),
                "RAW(32767) -> RAW/binary");
    }

    // ---- VARCHAR2 boundaries ----------------------------------------------

    @Test
    void roundTripsTextAtTheStandardCap() throws CSException {
        assertEquals(patternedText(STANDARD_CAP), row(1).getString("BIG_TEXT"));
    }

    @Test
    void roundTripsTextJustOverTheStandardCap() throws CSException {
        // 4001 chars: only legal at all under EXTENDED, and past the driver's
        // inline-bind threshold on the way in.
        assertEquals(patternedText(STANDARD_CAP + 1), row(2).getString("BIG_TEXT"));
    }

    @Test
    void roundTripsTextAtTheExtendedCap() throws CSException {
        String text = row(3).getString("BIG_TEXT");
        assertEquals(EXTENDED_CAP, text.length());
        assertEquals(patternedText(EXTENDED_CAP), text);
    }

    // ---- RAW at the extended cap -------------------------------------------

    @Test
    void roundTripsRawAtTheExtendedCap() throws CSException {
        assertArrayEquals(patternedBytes(EXTENDED_CAP), row(3).getByteArray("BIG_RAW"));
    }

    // ---- NULLs --------------------------------------------------------------

    @Test
    void nullExtendedColumnsReadBackAsNull() throws CSException {
        ReadOnlyRowSet rows = row(4);
        assertNull(rows.getString("BIG_TEXT"));
        assertTrue(rows.isNull("BIG_TEXT"));
        assertNull(rows.getByteArray("BIG_RAW"));
    }
}
