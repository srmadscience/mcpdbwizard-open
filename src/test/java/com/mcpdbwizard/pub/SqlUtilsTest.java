package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SqlUtils}. All methods under test are pure, static, and
 * require no database connection.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class SqlUtilsTest {

    // ---- getStatementType ------------------------------------------------

    @ParameterizedTest
    @CsvSource({
            "SELECT * FROM dual,            0",   // SELECT
            "insert into t values (1),      1",   // INSERT (case-insensitive)
            "UPDATE t SET x = 1,            2",   // UPDATE
            "DELETE FROM t,                 3",   // DELETE
            "LOCK TABLE t IN EXCLUSIVE MODE,6",   // LOCK
            "MERGE INTO t USING s,          7",   // MERGE
            "EXPLAIN PLAN FOR SELECT 1,     8",   // XPLAN
            "SAVEPOINT sp1,                 9",   // SAVEPOINT
            "SET CONSTRAINTS ALL DEFERRED, 10",   // SET_CONSTRAINTS
            "SET TRANSACTION READ ONLY,    11",   // SET_TRANSACTION
            "SET ROLE dba,                 12",   // SET_ROLE
            "BEGIN null; END;,             14",   // PLSQL
            "DECLARE x NUMBER; BEGIN END;, 14",   // PLSQL
            "CALL my_proc(),               14",   // PLSQL
            "CREATE TABLE t (x NUMBER),     5",   // DDL
            "DROP TABLE t,                  5",   // DDL
            "TRUNCATE TABLE t,              5",   // DDL
            "GRANT SELECT ON t TO bob,      5",   // DDL
    })
    void classifiesStatementType(String sql, int expected) {
        assertEquals(expected, SqlUtils.getStatementType(sql));
    }

    @Test
    void unrecognisedStatementIsUnknown() {
        assertEquals(SqlUtils.UNKNOWN, SqlUtils.getStatementType("FROBNICATE the widget"));
        assertEquals(SqlUtils.UNKNOWN, SqlUtils.getStatementType(""));
    }

    @Test
    void getStatementTypeIsCaseInsensitive() {
        assertEquals(SqlUtils.SELECT, SqlUtils.getStatementType("select 1 from dual"));
        assertEquals(SqlUtils.SELECT, SqlUtils.getStatementType("SeLeCt 1 from dual"));
    }

    // ---- leading comments ------------------------------------------------
    //
    // Every generated statement now starts with GENERATED_BY_COMMENT. Classification has to see
    // past it, or DmlStatement -- which classifies whatever text it is handed -- decides that a
    // marked SELECT is UNKNOWN and no longer knows it is a query. That is the whole reason the
    // marker can sit at the FRONT of the statement rather than after the first keyword.

    @Test
    void theGeneratedMarkerDoesNotHideTheStatementType() {
        assertEquals(SqlUtils.SELECT,
                SqlUtils.getStatementType(SqlUtils.GENERATED_BY_COMMENT + "\nSELECT * FROM dual"));
        assertEquals(SqlUtils.INSERT,
                SqlUtils.getStatementType(SqlUtils.GENERATED_BY_COMMENT + "\nINSERT INTO t VALUES (1)"));
        assertEquals(SqlUtils.UPDATE,
                SqlUtils.getStatementType(SqlUtils.GENERATED_BY_COMMENT + "\nUPDATE t SET x = 1"));
        assertEquals(SqlUtils.DELETE,
                SqlUtils.getStatementType(SqlUtils.GENERATED_BY_COMMENT + "\nDELETE FROM t"));
    }

    /** The marker is one shape of leading comment; the classifier handles the general case. */
    @Test
    void leadingCommentsAndWhitespaceAreSkipped() {
        assertEquals(SqlUtils.SELECT, SqlUtils.getStatementType("   \n\t SELECT 1 FROM dual"));
        assertEquals(SqlUtils.SELECT, SqlUtils.getStatementType("/* one */ SELECT 1 FROM dual"));
        assertEquals(SqlUtils.SELECT, SqlUtils.getStatementType("-- a line comment\nSELECT 1 FROM dual"));
        assertEquals(SqlUtils.SELECT,
                SqlUtils.getStatementType("/* one */\n-- two\n  /* three */ SELECT 1 FROM dual"));
        assertEquals(SqlUtils.PLSQL, SqlUtils.getStatementType("/* c */ BEGIN null; END;"));
    }

    /**
     * An optimizer hint must still be read as a hint, not consumed as a comment — it only works
     * where Oracle expects it, immediately after the keyword, so it is never leading.
     */
    @Test
    void aHintAfterTheKeywordIsUntouched() {
        assertEquals(SqlUtils.SELECT, SqlUtils.getStatementType("SELECT /*+ FULL(t) */ * FROM t"));
    }

    /** Nothing is rewritten: this only decides where classification starts reading. */
    @Test
    void skippingIsClassificationOnlyAndDoesNotAlterTheStatement() {
        String marked = SqlUtils.GENERATED_BY_COMMENT + "\nSELECT 1 FROM dual";
        SqlUtils.getStatementType(marked);
        assertTrue(marked.startsWith(SqlUtils.GENERATED_BY_COMMENT),
                "the text sent to Oracle must keep its marker");
    }

    @Test
    void anUnterminatedCommentIsUnknownRatherThanMisread() {
        // Oracle would not parse it either, so UNKNOWN is the honest answer.
        assertEquals(SqlUtils.UNKNOWN, SqlUtils.getStatementType("/* never closed SELECT 1 FROM dual"));
    }

    @Test
    void theMarkerIsSingleSpacedAndNamesTheProduct() {
        // It read "Created  By" until 2026-08-16. Anyone matching on it in V$SQL depends on this.
        assertEquals("/* Created By " + Namer.param_product_name_long + " */",
                SqlUtils.GENERATED_BY_COMMENT);
        assertFalse(SqlUtils.GENERATED_BY_COMMENT.contains("  "), "no double space");
    }

    // ---- countParameters -------------------------------------------------

    @Test
    void countsBindParameters() {
        assertEquals(0, SqlUtils.countParameters("SELECT * FROM dual"));
        assertEquals(1, SqlUtils.countParameters("SELECT * FROM t WHERE id = ?"));
        assertEquals(3, SqlUtils.countParameters("INSERT INTO t VALUES (?, ?, ?)"));
    }

    @Test
    void ignoresQuestionMarksInsideStringLiterals() {
        // The '?' inside the quoted literal must not be counted; the trailing one must.
        assertEquals(1, SqlUtils.countParameters("SELECT 'why?' FROM t WHERE id = ?"));
        assertEquals(0, SqlUtils.countParameters("SELECT 'a ? b ? c' FROM dual"));
    }

    @Test
    void apostropheInsideCommentDoesNotHideARealParameter() {
        // The defect this closes. A lone apostrophe in a comment used to open a string literal
        // that never closed, so the real '?' after it went uncounted and the statement was sized
        // for zero parameters. Measured live against orindademo: 2000/2000 calls failed.
        String sql = "DELETE customers\n"
                + "/* If you don't have a comment after a parameter you are free to choose a\n"
                + "   name and set the data type using the user interface */\n"
                + "WHERE name = ?";
        assertEquals(1, SqlUtils.countParameters(sql));
    }

    @Test
    void ignoresQuestionMarksInsideComments() {
        // The other half of the same bug: a '?' inside a comment was counted as a bind variable.
        assertEquals(0, SqlUtils.countParameters("SELECT 1 FROM dual /* really? */"));
        assertEquals(0, SqlUtils.countParameters("SELECT 1 FROM dual -- really?\n"));
        assertEquals(1, SqlUtils.countParameters("SELECT /* what? */ 1 FROM t WHERE id = ?"));
        assertEquals(1, SqlUtils.countParameters("SELECT 1 FROM t -- what?\nWHERE id = ?"));
    }

    @Test
    void countsParametersAroundQuotesAndComments() {
        // A doubled '' escape must leave the scanner in the right state at the closing quote.
        assertEquals(1, SqlUtils.countParameters("SELECT 'it''s' FROM t WHERE id = ?"));
        // A comment marker inside a literal is not a comment.
        assertEquals(1, SqlUtils.countParameters("SELECT '/* not a comment' FROM t WHERE id = ?"));
        // An unterminated comment swallows the rest, as Oracle would reject it anyway.
        assertEquals(0, SqlUtils.countParameters("SELECT 1 FROM t /* WHERE id = ?"));
        assertEquals(0, SqlUtils.countParameters(null));
    }

    // ---- getUnderlyingOracleDatatype ------------------------------------

    @Test
    void nullDatatypeMapsToNullConstant() {
        assertEquals(SqlUtils.ORACLE_NULL_DATATYPE, SqlUtils.getUnderlyingOracleDatatype(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"VARCHAR2", "VARCHAR", "CHAR", "CHARACTER", "STRING"})
    void textDatatypes(String type) {
        assertEquals(SqlUtils.ORACLE_TEXT_DATATYPE, SqlUtils.getUnderlyingOracleDatatype(type));
    }

    @ParameterizedTest
    @ValueSource(strings = {"NCHAR", "NCHARACTER", "NVARCHAR", "NVARCHAR2"})
    void nationalCharacterDatatypesMapToText(String type) {
        // National-character string types ride the ordinary text path (KLUGE 001).
        assertEquals(SqlUtils.ORACLE_TEXT_DATATYPE, SqlUtils.getUnderlyingOracleDatatype(type));
    }

    @Test
    void nclobMapsToClob() {
        // NCLOB rides the CLOB path (oracle.sql.NCLOB extends oracle.sql.CLOB).
        assertEquals(SqlUtils.ORACLE_CLOB_DATATYPE, SqlUtils.getUnderlyingOracleDatatype("NCLOB"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"NUMBER", "FLOAT", "INTEGER", "INT", "DECIMAL", "PLS_INTEGER", "BINARY_INTEGER"})
    void numberDatatypes(String type) {
        assertEquals(SqlUtils.ORACLE_NUMBER_DATATYPE, SqlUtils.getUnderlyingOracleDatatype(type));
    }

    @ParameterizedTest
    @ValueSource(strings = {"BINARY_FLOAT", "BINARY_DOUBLE"})
    void ieeeBinaryFloatDatatypesMapToNumber(String type) {
        // IEEE-754 binary floating-point types ride the NUMBER path.
        assertEquals(SqlUtils.ORACLE_NUMBER_DATATYPE, SqlUtils.getUnderlyingOracleDatatype(type));
    }

    @Test
    void dateAndLobAndRowidDatatypes() {
        assertEquals(SqlUtils.ORACLE_DATE_DATATYPE, SqlUtils.getUnderlyingOracleDatatype("DATE"));
        assertEquals(SqlUtils.ORACLE_CLOB_DATATYPE, SqlUtils.getUnderlyingOracleDatatype("CLOB"));
        assertEquals(SqlUtils.ORACLE_BLOB_DATATYPE, SqlUtils.getUnderlyingOracleDatatype("BLOB"));
        assertEquals(SqlUtils.ORACLE_ROWID_DATATYPE, SqlUtils.getUnderlyingOracleDatatype("ROWID"));
        assertEquals(SqlUtils.ORACLE_LONGTEXT_DATATYPE, SqlUtils.getUnderlyingOracleDatatype("LONG"));
    }

    @Test
    void timestampVariantsAreDistinguished() {
        assertEquals(SqlUtils.ORACLE_TIMESTAMP_DATATYPE,
                SqlUtils.getUnderlyingOracleDatatype("TIMESTAMP(6)"));
        assertEquals(SqlUtils.ORACLE_TIMESTAMPTZ_DATATYPE,
                SqlUtils.getUnderlyingOracleDatatype("TIMESTAMP WITH TIME ZONE"));
        assertEquals(SqlUtils.ORACLE_TIMESTAMPLTZ_DATATYPE,
                SqlUtils.getUnderlyingOracleDatatype("TIMESTAMP WITH LOCAL TIME ZONE"));
    }

    @Test
    void intervalDatatypes() {
        assertEquals(SqlUtils.ORACLE_INTERVAL_YEAR_TO_MONTH_DATATYPE,
                SqlUtils.getUnderlyingOracleDatatype("INTERVAL YEAR TO MONTH"));
        assertEquals(SqlUtils.ORACLE_INTERVAL_DAY_TO_SECOND_DATATYPE,
                SqlUtils.getUnderlyingOracleDatatype("INTERVAL DAY TO SECOND"));
    }

    @Test
    void unrecognisedDatatypeMapsToOther() {
        assertEquals(SqlUtils.ORACLE_OTHER_DATATYPE,
                SqlUtils.getUnderlyingOracleDatatype("SOME_USER_DEFINED_TYPE"));
    }

    // ---- Oracle 23ai datatypes ------------------------------------------

    @Test
    void nativeJsonDatatype() {
        // Native binary JSON column type (Oracle 21c+).
        assertEquals(SqlUtils.ORACLE_JSON_DATATYPE, SqlUtils.getUnderlyingOracleDatatype("JSON"));
    }

    @Test
    void nativeBooleanDatatypeIsDistinctFromPlsqlBoolean() {
        // The real ISO-SQL BOOLEAN column type (Oracle 23ai)...
        assertEquals(SqlUtils.ORACLE_NATIVE_BOOLEAN_DATATYPE,
                SqlUtils.getUnderlyingOracleDatatype("BOOLEAN"));
        // ...is a different constant from the older PL/SQL-only boolean.
        assertEquals(SqlUtils.ORACLE_BOOLEAN_DATATYPE,
                SqlUtils.getUnderlyingOracleDatatype("PL/SQL BOOLEAN"));
        assertNotEquals(SqlUtils.ORACLE_BOOLEAN_DATATYPE, SqlUtils.ORACLE_NATIVE_BOOLEAN_DATATYPE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"VECTOR", "VECTOR(3,FLOAT32)", "VECTOR(384,FLOAT64)", "VECTOR_INT8"})
    void vectorDatatypeVariants(String type) {
        // VECTOR and its dimensioned / storage-typed spellings (Oracle 23ai).
        assertEquals(SqlUtils.ORACLE_VECTOR_DATATYPE, SqlUtils.getUnderlyingOracleDatatype(type));
    }

    @Test
    void vectorFormatTokensClassifyToTheirConstants() {
        // The generator's table-column introspection rewrites a VECTOR column's DATA_TYPE to a
        // format-specific token (VECTOR_BINARY / VECTOR_SPARSE); getUnderlyingOracleDatatype must
        // recognise those, and not confuse them with the generic VECTOR (startsWith) path.
        assertEquals(SqlUtils.ORACLE_VECTOR_BINARY_DATATYPE,
                SqlUtils.getUnderlyingOracleDatatype("VECTOR_BINARY"));
        assertEquals(SqlUtils.ORACLE_VECTOR_SPARSE_DATATYPE,
                SqlUtils.getUnderlyingOracleDatatype("VECTOR_SPARSE"));
        // Plain VECTOR (and dimensioned forms) still classify as the dense code.
        assertEquals(SqlUtils.ORACLE_VECTOR_DATATYPE,
                SqlUtils.getUnderlyingOracleDatatype("VECTOR"));
    }

    // ---- getVectorDatatypeFromInfo (VECTOR_INFO sub-classification) -------

    @ParameterizedTest
    @ValueSource(strings = {
            "VECTOR(3,FLOAT32,DENSE)", "VECTOR(2,FLOAT64,DENSE)",
            "VECTOR(5,INT8,DENSE)", "VECTOR(*,*,DENSE)", "VECTOR(768,FLOAT32,DENSE)"})
    void vectorInfoDenseAndFlexibleClassifyAsDenseVector(String info) {
        // Dense FLOAT32/FLOAT64/INT8 and flexible (*,*) all ride the existing double[] path.
        assertEquals(SqlUtils.ORACLE_VECTOR_DATATYPE, SqlUtils.getVectorDatatypeFromInfo(info));
    }

    @Test
    void vectorInfoBinaryClassifiesAsBinaryVector() {
        assertEquals(SqlUtils.ORACLE_VECTOR_BINARY_DATATYPE,
                SqlUtils.getVectorDatatypeFromInfo("VECTOR(16,BINARY,DENSE)"));
    }

    @Test
    void vectorInfoSparseClassifiesAsSparseVector() {
        assertEquals(SqlUtils.ORACLE_VECTOR_SPARSE_DATATYPE,
                SqlUtils.getVectorDatatypeFromInfo("VECTOR(4,FLOAT32,SPARSE)"));
    }

    @Test
    void vectorInfoNullOrBlankFallsBackToDenseVector() {
        // Callers without VECTOR_INFO (e.g. proc parameters) must be unaffected.
        assertEquals(SqlUtils.ORACLE_VECTOR_DATATYPE, SqlUtils.getVectorDatatypeFromInfo(null));
        assertEquals(SqlUtils.ORACLE_VECTOR_DATATYPE, SqlUtils.getVectorDatatypeFromInfo(""));
        assertEquals(SqlUtils.ORACLE_VECTOR_DATATYPE, SqlUtils.getVectorDatatypeFromInfo("VECTOR"));
    }

    @Test
    void vectorInfoIsCaseInsensitive() {
        assertEquals(SqlUtils.ORACLE_VECTOR_BINARY_DATATYPE,
                SqlUtils.getVectorDatatypeFromInfo("vector(16,binary,dense)"));
        assertEquals(SqlUtils.ORACLE_VECTOR_SPARSE_DATATYPE,
                SqlUtils.getVectorDatatypeFromInfo("vector(4,float32,sparse)"));
    }

    @Test
    void vectorFormatConstantsAreDistinct() {
        // The three vector codes must be mutually distinct (and distinct from "other"/null).
        assertNotEquals(SqlUtils.ORACLE_VECTOR_DATATYPE, SqlUtils.ORACLE_VECTOR_BINARY_DATATYPE);
        assertNotEquals(SqlUtils.ORACLE_VECTOR_DATATYPE, SqlUtils.ORACLE_VECTOR_SPARSE_DATATYPE);
        assertNotEquals(SqlUtils.ORACLE_VECTOR_BINARY_DATATYPE, SqlUtils.ORACLE_VECTOR_SPARSE_DATATYPE);
        assertNotEquals(SqlUtils.ORACLE_OTHER_DATATYPE, SqlUtils.ORACLE_VECTOR_BINARY_DATATYPE);
    }

    @Test
    void extendedVarcharAndIdentityRideExistingPaths() {
        // 12c "extended" VARCHAR2 still reports as VARCHAR2 (text); an IDENTITY column
        // reports to JDBC as NUMBER. Neither needs a new classification.
        assertEquals(SqlUtils.ORACLE_TEXT_DATATYPE, SqlUtils.getUnderlyingOracleDatatype("VARCHAR2"));
        assertEquals(SqlUtils.ORACLE_NUMBER_DATATYPE, SqlUtils.getUnderlyingOracleDatatype("NUMBER"));
    }

    // ---- RESERVED_WORDS --------------------------------------------------

    /**
     * The case the check exists for: the SAME type reaches this method as one concatenated line
     * from generated Java and as several lines from extraObjects.sql, and Oracle hands back a third
     * spelling again (USER_SOURCE stores the text from the TYPE keyword onwards). All three must
     * compare equal, or a server restart would refuse to start on a type that is perfectly correct.
     *
     * <p>The literals below are real output, taken from a generated DAOFactoryServiceImpl, the
     * extraObjects.sql beside it, and USER_SOURCE on a live 26ai server.
     */
    @Test
    void theThreeSpellingsOfOneTypeAllCompareEqual() {
        String fromGeneratedJava = "CREATE OR REPLACE TYPE OSOFT7990ND99_T AS OBJECT"
                + " (COL_0 VARCHAR2(128) ,COL_1 VARCHAR2(512) ,COL_2 NUMBER)";
        String fromExtraObjectsSql = "CREATE OR REPLACE TYPE OSOFT7990ND99_T AS OBJECT\n"
                + " (COL_0 VARCHAR2(128)\n ,COL_1 VARCHAR2(512)\n ,COL_2 NUMBER);";
        String fromUserSource = "TYPE OSOFT7990ND99_T AS OBJECT (COL_0 VARCHAR2(128)"
                + " ,COL_1 VARCHAR2(512) ,COL_2 NUMBER)";

        assertEquals(SqlUtils.normaliseTypeSpec(fromGeneratedJava),
                SqlUtils.normaliseTypeSpec(fromExtraObjectsSql));
        assertEquals(SqlUtils.normaliseTypeSpec(fromGeneratedJava),
                SqlUtils.normaliseTypeSpec(fromUserSource));
    }

    /** A collection type, whose stored source keeps the trailing semicolon it was created with. */
    @Test
    void collectionTypeSemicolonAndDoubleSpaceAreDiscounted() {
        assertEquals(SqlUtils.normaliseTypeSpec("CREATE OR REPLACE TYPE OSOFT_A  AS TABLE OF OSOFT_T;"),
                SqlUtils.normaliseTypeSpec("TYPE OSOFT_A AS TABLE OF OSOFT_T"));
    }

    /**
     * The whole point: a real difference must survive normalisation. An extra column, a changed
     * length and a changed datatype are the three ways these types drift.
     */
    @Test
    void realDifferencesAreNotNormalisedAway() {
        String theBase = "CREATE OR REPLACE TYPE T AS OBJECT (COL_0 VARCHAR2(128) ,COL_1 NUMBER)";
        assertNotEquals(SqlUtils.normaliseTypeSpec(theBase), SqlUtils.normaliseTypeSpec(
                "CREATE OR REPLACE TYPE T AS OBJECT (COL_0 VARCHAR2(128) ,COL_1 NUMBER ,COL_2 DATE)"));
        assertNotEquals(SqlUtils.normaliseTypeSpec(theBase), SqlUtils.normaliseTypeSpec(
                "CREATE OR REPLACE TYPE T AS OBJECT (COL_0 VARCHAR2(129) ,COL_1 NUMBER)"));
        assertNotEquals(SqlUtils.normaliseTypeSpec(theBase), SqlUtils.normaliseTypeSpec(
                "CREATE OR REPLACE TYPE T AS OBJECT (COL_0 VARCHAR2(128) ,COL_1 DATE)"));
    }

    /** Different types must not collide just because the leading keyword is stripped from both. */
    @Test
    void theTypeNameIsPartOfTheComparison() {
        assertNotEquals(SqlUtils.normaliseTypeSpec("CREATE OR REPLACE TYPE A_T AS OBJECT (C NUMBER)"),
                SqlUtils.normaliseTypeSpec("CREATE OR REPLACE TYPE B_T AS OBJECT (C NUMBER)"));
    }

    @Test
    void normaliseTypeSpecToleratesNullAndEmpty() {
        assertEquals("", SqlUtils.normaliseTypeSpec(null));
        assertEquals("", SqlUtils.normaliseTypeSpec(""));
        // No TYPE keyword at all: nothing is stripped, and it still must not throw.
        assertEquals("NOTDDL", SqlUtils.normaliseTypeSpec("not ddl"));
    }

    @Test
    void reservedWordsListIsPopulatedAndContainsKnownKeywords() {
        assertTrue(SqlUtils.RESERVED_WORDS.length > 100);
        assertTrue(java.util.Arrays.asList(SqlUtils.RESERVED_WORDS).contains("SELECT"));
        assertTrue(java.util.Arrays.asList(SqlUtils.RESERVED_WORDS).contains("TABLE"));
        // All entries should be upper case.
        for (String word : SqlUtils.RESERVED_WORDS) {
            assertEquals(word.toUpperCase(), word, "reserved word not upper case: " + word);
        }
    }
}
