package com.mcpdbwizard.pub;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Live-database tests for the <b>Oracle 23ai datatypes</b> that
 * {@link com.mcpdbwizard.pub} gained support for: the native ISO-SQL
 * {@code BOOLEAN} column, the native binary {@code JSON} column (21c+), and the
 * {@code VECTOR} column. Also covers the 12c-era {@code IDENTITY} column and a
 * (conditionally) extended {@code VARCHAR2}.
 * <p>
 * The fixture mirrors {@code sql/datatypes_23ai.sql} and is created/dropped by the
 * test itself, so the suite is self-provisioning. Gated on a reachable Oracle 23ai
 * instance via {@link DbTestSupport} — if none is reachable the whole class is
 * <em>skipped</em>, never failed. Point it at the dev box with, e.g.:
 * <pre>
 *   ORINDA_TEST_URL='jdbc:oracle:thin:@203.0.113.10:1521/FREEPDB1' \
 *   ORINDA_TEST_USER=appschema ORINDA_TEST_PASSWORD=appschema \
 *     mvn -Dtest=Datatypes23aiLiveTest test
 * </pre>
 * Requires Oracle Database 23ai or later; on an older server (the 12c/18c/19c/21c
 * test boxes) the {@code @BeforeAll} skips the class via an Assumption, matching
 * how the other live tests gate on their prerequisites.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Datatypes23aiLiveTest {

    private static final String TABLE = "OB_DATATYPES_23AI";

    private final LogInterface log = new ConsoleLog();
    private Connection connection;

    @BeforeAll
    void setUp() throws SQLException {
        connection = DbTestSupport.requireConnection();
        int major = connection.getMetaData().getDatabaseMajorVersion();
        org.junit.jupiter.api.Assumptions.assumeTrue(major >= 23,
                "Oracle " + major + " lacks the 23ai datatypes (need >= 23) - skipping");
        DbTestSupport.executeQuietly(connection, "DROP TABLE " + TABLE + " PURGE");
        DbTestSupport.execute(connection,
                "CREATE TABLE " + TABLE + " ("
                        + " id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,"
                        + " flag BOOLEAN,"
                        + " doc JSON,"
                        + " embedding VECTOR(3, FLOAT32),"
                        + " any_vec VECTOR,"
                        + " bin_vec VECTOR(16, BINARY),"
                        + " sp_vec VECTOR(25, FLOAT32, SPARSE),"
                        + " big_text VARCHAR2(4000))");
        DbTestSupport.execute(connection,
                "INSERT INTO " + TABLE + " (flag, doc, embedding, any_vec, bin_vec, sp_vec, big_text)"
                        + " VALUES (TRUE, '{\"a\":1,\"b\":[10,20]}', '[1.5, 2.5, 3.5]',"
                        + " '[9, 8, 7, 6]', to_vector('[3, 200]', 16, binary),"
                        + " to_vector('[25, [3, 10], [1.5, -2.0]]', 25, FLOAT32, SPARSE), 'hello extended')");
        DbTestSupport.execute(connection,
                "INSERT INTO " + TABLE + " (flag, doc, embedding, any_vec, bin_vec, sp_vec, big_text)"
                        + " VALUES (FALSE, NULL, '[0, 0, 0]', NULL, NULL, NULL, NULL)");
        // The connection is auto-commit (see DbTestSupport.connect()), so the DML above
        // is already durable; no explicit commit is needed (and would raise ORA-17273).
    }

    @AfterAll
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            DbTestSupport.executeQuietly(connection, "DROP TABLE " + TABLE + " PURGE");
            connection.close();
        }
    }

    /** Run a query and return the populated row set, ordered by the identity id. */
    private ReadOnlyRowSet allRows() throws CSException {
        return new QueryStatement(
                "SELECT id, flag, doc, embedding, any_vec, bin_vec, sp_vec, big_text FROM " + TABLE + " ORDER BY id",
                log, connection).execute();
    }

    // ---- classification: the JDBC type names map to the new constants ----

    @Test
    void columnTypeNamesClassifyToTheNewConstants() throws CSException {
        ReadOnlyRowSet rows = allRows();
        String[] typeNames = rows.getColumnOracleDatatypeNames();

        // id, flag, doc, embedding, any_vec, bin_vec, sp_vec, big_text
        assertEquals(SqlUtils.ORACLE_NUMBER_DATATYPE,
                SqlUtils.getUnderlyingOracleDatatype(typeNames[0]), "id (IDENTITY) -> NUMBER");
        assertEquals(SqlUtils.ORACLE_NATIVE_BOOLEAN_DATATYPE,
                SqlUtils.getUnderlyingOracleDatatype(typeNames[1]), "flag -> native BOOLEAN");
        assertEquals(SqlUtils.ORACLE_JSON_DATATYPE,
                SqlUtils.getUnderlyingOracleDatatype(typeNames[2]), "doc -> JSON");
        assertEquals(SqlUtils.ORACLE_VECTOR_DATATYPE,
                SqlUtils.getUnderlyingOracleDatatype(typeNames[3]), "embedding -> VECTOR");
        assertEquals(SqlUtils.ORACLE_VECTOR_DATATYPE,
                SqlUtils.getUnderlyingOracleDatatype(typeNames[4]), "any_vec -> VECTOR");
        // bin_vec's JDBC type NAME is also plain "VECTOR" (the format is only in getColumnType()),
        // so the name-based classifier returns the generic VECTOR code; ReadOnlyRowSet refines it
        // to ORACLE_VECTOR_BINARY_DATATYPE internally (see readsBinaryVectorColumn).
        assertEquals(SqlUtils.ORACLE_VECTOR_DATATYPE,
                SqlUtils.getUnderlyingOracleDatatype(typeNames[5]), "bin_vec -> VECTOR (by name)");
        // sp_vec (sparse) also NAMEs plain "VECTOR"; ReadOnlyRowSet preserves it as a SparseVector at
        // unload via isSparse() (see readsSparseVectorColumn).
        assertEquals(SqlUtils.ORACLE_VECTOR_DATATYPE,
                SqlUtils.getUnderlyingOracleDatatype(typeNames[6]), "sp_vec -> VECTOR (by name)");
        assertEquals(SqlUtils.ORACLE_TEXT_DATATYPE,
                SqlUtils.getUnderlyingOracleDatatype(typeNames[7]), "big_text -> TEXT");
    }

    // ---- native BOOLEAN --------------------------------------------------

    @Test
    void readsNativeBooleanColumn() throws CSException {
        ReadOnlyRowSet rows = allRows();

        rows.first();
        assertEquals(Boolean.TRUE, rows.getBooleanObj("FLAG"));
        assertTrue(rows.getBoolean("FLAG"));
        assertEquals("true", rows.getString("FLAG"));

        rows.last();
        assertEquals(Boolean.FALSE, rows.getBooleanObj("FLAG"));
        assertFalse(rows.getBoolean("FLAG"));
    }

    // ---- native JSON -----------------------------------------------------

    @Test
    void readsNativeJsonColumn() throws CSException {
        ReadOnlyRowSet rows = allRows();
        rows.first();

        oracle.sql.json.OracleJsonValue json = rows.getJSON("DOC");
        assertNotNull(json);
        // Renders back to JSON text via both the typed value and getString().
        assertEquals("{\"a\":1,\"b\":[10,20]}", json.toString());
        assertEquals("{\"a\":1,\"b\":[10,20]}", rows.getString("DOC"));

        // The document is navigable as an object.
        oracle.sql.json.OracleJsonObject obj = json.asJsonObject();
        assertEquals(1, obj.getInt("a"));

        // NULL JSON reads back as null.
        rows.last();
        assertNull(rows.getJSON("DOC"));
        assertTrue(rows.isNull("DOC"));
    }

    // ---- VECTOR ----------------------------------------------------------

    @Test
    void readsVectorColumns() throws CSException {
        ReadOnlyRowSet rows = allRows();
        rows.first();

        // FLOAT32 storage: 1.5 / 2.5 / 3.5 are exact, so equality is safe.
        assertArrayEquals(new double[]{1.5, 2.5, 3.5}, rows.getVector("EMBEDDING"));
        assertArrayEquals(new double[]{9, 8, 7, 6}, rows.getVector("ANY_VEC"));
        assertEquals("[1.5, 2.5, 3.5]", rows.getString("EMBEDDING"));

        // NULL flexible vector reads back as null.
        rows.last();
        assertNull(rows.getVector("ANY_VEC"));
        // ...but a fixed-dimension column is still populated.
        assertArrayEquals(new double[]{0, 0, 0}, rows.getVector("EMBEDDING"));
    }

    @Test
    void readsBinaryVectorColumn() throws CSException {
        // A binary VECTOR is bit-packed: VECTOR(16, BINARY) = 16 bits = 2 bytes. The driver cannot
        // read it as double[] (ORA-17004), so ReadOnlyRowSet classifies it (via getColumnType() =
        // VECTOR_BINARY) and reads it as byte[]. If the classification were wrong the query would
        // throw ORA-17004 while unloading, so reaching these assertions proves the byte[] path.
        ReadOnlyRowSet rows = allRows();
        rows.first();

        // to_vector('[3, 200]', 16, binary) -> bytes 3 and 200 (200 is -56 as a signed byte).
        assertArrayEquals(new byte[]{3, (byte) 200}, rows.getVectorBinary("BIN_VEC"));
        assertEquals("[3, -56]", rows.getString("BIN_VEC"));

        // NULL binary vector reads back as null.
        rows.last();
        assertNull(rows.getVectorBinary("BIN_VEC"));
    }

    @Test
    void writesBinaryVectorColumnViaBoundParam() throws CSException, java.sql.SQLException {
        // Bind a byte[] as VECTOR_BINARY (setVectorBinaryParam) into an INSERT, read it back on the
        // same connection, then delete the row so the row-order-sensitive tests above are unaffected.
        byte[] packed = new byte[]{(byte) 0xAB, (byte) 0xCD}; // 16 bits = 2 bytes
        DmlStatement insert = new DmlStatement(
                "INSERT INTO " + TABLE + " (bin_vec, big_text) VALUES (?, ?)", log, connection) {
        };
        try {
            insert.setVectorBinaryParam(1, packed);
            insert.setParam(2, "binwrite");
            java.sql.PreparedStatement ps = (java.sql.PreparedStatement) insert.getUnderlyingStatement();
            insert.bindParameters(ps);
            assertEquals(1, ps.executeUpdate());

            ReadOnlyRowSet rows = new QueryStatement(
                    "SELECT bin_vec FROM " + TABLE + " WHERE big_text = 'binwrite'", log, connection).execute();
            rows.first();
            assertArrayEquals(packed, rows.getVectorBinary("BIN_VEC"));
        } finally {
            DbTestSupport.executeQuietly(connection, "DELETE FROM " + TABLE + " WHERE big_text = 'binwrite'");
        }
    }

    @Test
    void readsSparseVectorColumn() throws CSException {
        // A sparse VECTOR carries the same JDBC metadata as a dense one, so ReadOnlyRowSet reads the
        // value as an oracle.sql.VECTOR and preserves the sparse form (getVectorSparse) rather than
        // densifying it. to_vector('[25, [3, 10], [1.5, -2.0]]', ...) = length 25, non-zeros at 3 and 10.
        ReadOnlyRowSet rows = allRows();
        rows.first();

        SparseVector sp = rows.getVectorSparse("SP_VEC");
        assertEquals(25, sp.length);
        assertArrayEquals(new int[]{3, 10}, sp.indices);
        assertArrayEquals(new double[]{1.5, -2.0}, sp.values);
        // getVector() on the same column densifies it (zeros except at 3 and 10).
        double[] dense = rows.getVector("SP_VEC");
        assertEquals(25, dense.length);
        assertEquals(1.5, dense[3]);
        assertEquals(-2.0, dense[10]);
        assertEquals(0.0, dense[0]);

        // NULL sparse vector reads back as null.
        rows.last();
        assertNull(rows.getVectorSparse("SP_VEC"));
    }

    @Test
    void writesSparseVectorColumnViaBoundParam() throws CSException, java.sql.SQLException {
        // Bind a SparseVector (setVectorSparseParam) into an INSERT, read it back, then clean up.
        SparseVector sp = new SparseVector(25, new int[]{2, 20}, new double[]{7.5, -3.25});
        DmlStatement insert = new DmlStatement(
                "INSERT INTO " + TABLE + " (sp_vec, big_text) VALUES (?, ?)", log, connection) {
        };
        try {
            insert.setVectorSparseParam(1, sp);
            insert.setParam(2, "spwrite");
            java.sql.PreparedStatement ps = (java.sql.PreparedStatement) insert.getUnderlyingStatement();
            insert.bindParameters(ps);
            assertEquals(1, ps.executeUpdate());

            ReadOnlyRowSet rows = new QueryStatement(
                    "SELECT sp_vec FROM " + TABLE + " WHERE big_text = 'spwrite'", log, connection).execute();
            rows.first();
            SparseVector back = rows.getVectorSparse("SP_VEC");
            assertEquals(25, back.length);
            assertArrayEquals(new int[]{2, 20}, back.indices);
            assertArrayEquals(new double[]{7.5, -3.25}, back.values);
        } finally {
            DbTestSupport.executeQuietly(connection, "DELETE FROM " + TABLE + " WHERE big_text = 'spwrite'");
        }
    }

    // ---- IDENTITY + extended VARCHAR2 ------------------------------------

    @Test
    void readsIdentityAndTextColumns() throws CSException {
        ReadOnlyRowSet rows = allRows();
        rows.first();

        // IDENTITY column is a generated NUMBER; first row's id is >= 1.
        assertTrue(rows.getLong("ID") >= 1L);
        assertEquals("hello extended", rows.getString("BIG_TEXT"));

        rows.last();
        assertNull(rows.getString("BIG_TEXT"));
    }

    // ---- wrong-type access still throws the documented cast exception ----

    @Test
    void readingJsonAsVectorThrowsCastException() throws CSException {
        ReadOnlyRowSet rows = allRows();
        rows.first();
        assertThrows(CSDBInvalidDatatypeCastException.class, () -> rows.getVector("DOC"));
        assertThrows(CSDBInvalidDatatypeCastException.class, () -> rows.getJSON("EMBEDDING"));
    }
}
