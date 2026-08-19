package com.mcpdbwizard.pub;

import java.sql.*;

import oracle.jdbc.OracleTypes;

/**
 * A set of useful static methods for working with SQL.
 * <p>
 * This class changes for different versions of Oracle.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 */
public class SqlUtils {

    /**
     * Constant for identifying statements as being queries
     */
    public static final int SELECT = 0;

    /**
     * Constant for identifying statements as being inserts
     */
    public static final int INSERT = 1;

    /**
     * Constant for identifying statements as being updates
     */
    public static final int UPDATE = 2;

    /**
     * Constant for identifying statements as being deletes
     */
    public static final int DELETE = 3;

    /**
     * Constant for identifying statements as being DDL statements
     *
     * @since 2.0.1527 Support for Lock, Merge and DDL statements
     */
    public static final int DDL = 5;

    /**
     * Constant for identifying statements as being Lock statements
     *
     * @since 2.0.1527 Support for Lock, Merge and DDL statements
     */
    public static final int LOCK = 6;

    /**
     * Constant for identifying statements as being Merge statements
     *
     * @since 2.0.1527 Support for Lock, Merge and DDL statements
     */
    public static final int MERGE = 7;

    /**
     * Constant for identifying statements as being Lock statements
     *
     * @since 2.0.1527 Support for Lock, Merge and DDL statements
     */
    public static final int XPLAN = 8;

    /**
     * Constant for identifying statements as being Lock statements
     *
     * @since 2.0.1527 Support for Lock, Merge and DDL statements
     */
    public static final int SAVEPOINT = 9;

    /**
     * Constant for identifying statements as being Lock statements
     *
     * @since 2.0.1527 Support for Lock, Merge and DDL statements
     */
    public static final int SET_CONSTRAINTS = 10;

    /**
     * Constant for identifying statements as being Lock statements
     *
     * @since 2.0.1527 Support for Lock, Merge and DDL statements
     */
    public static final int SET_TRANSACTION = 11;

    /**
     * Constant for identifying statements as being Lock statements
     *
     * @since 2.0.1527 Support for Lock, Merge and DDL statements
     */
    public static final int SET_ROLE = 12;

    /**
     * Constant for identifying statements as being PL/SQL statements
     *
     * @since 2.0.1527 Support for Lock, Merge and DDL statements
     */
    public static final int PLSQL = 14;

    /**
     * Constant for identifying statements as being unidentifiable
     */
    public static final int UNKNOWN = 4;

    /**
     * Array containing words which are not legal identifers...
     */
    public static final String[] RESERVED_WORDS =
            {"ACCESS"
                    , "ADD"
                    , "ALL"
                    , "ALTER"
                    , "AND"
                    , "ANY"
                    , "AS"
                    , "ASC"
                    , "AUDIT"
                    , "BETWEEN"
                    , "BY"
                    , "CHAR"
                    , "CHECK"
                    , "CLUSTER"
                    , "COLUMN"
                    , "COMMENT"
                    , "COMPRESS"
                    , "CONNECT"
                    , "CREATE"
                    , "CURRENT"
                    , "DATE"
                    , "DECIMAL"
                    , "DEFAULT"
                    , "DELETE"
                    , "DESC"
                    , "DISTINCT"
                    , "DROP"
                    , "ELSE"
                    , "EXCLUSIVE"
                    , "EXISTS"
                    , "FILE"
                    , "FLOAT"
                    , "FOR"
                    , "FROM"
                    , "GRANT"
                    , "GROUP"
                    , "HAVING"
                    , "IDENTIFIED"
                    , "IMMEDIATE"
                    , "IN"
                    , "INCREMENT"
                    , "INDEX"
                    , "INITIAL"
                    , "INSERT"
                    , "INTEGER"
                    , "INTERSECT"
                    , "INTO"
                    , "IS"
                    , "LEVEL"
                    , "LIKE"
                    , "LOCK"
                    , "LONG"
                    , "MAXEXTENTS"
                    , "MINUS"
                    , "MLSLABEL"
                    , "MODE"
                    , "MODIFY"
                    , "NOAUDIT"
                    , "NOCOMPRESS"
                    , "NOT"
                    , "NOWAIT"
                    , "NULL"
                    , "NUMBER"
                    , "OF"
                    , "OFFLINE"
                    , "ON"
                    , "ONLINE"
                    , "OPTION"
                    , "OR"
                    , "ORDER"
                    , "PCTFREE"
                    , "PRIOR"
                    , "PRIVILEGES"
                    , "PUBLIC"
                    , "RAW"
                    , "RENAME"
                    , "RESOURCE"
                    , "REVOKE"
                    , "ROW"
                    , "ROWID"
                    , "ROWNUM"
                    , "ROWS"
                    , "SELECT"
                    , "SESSION"
                    , "SET"
                    , "SHARE"
                    , "SIZE"
                    , "SMALLINT"
                    , "START"
                    , "SUCCESSFUL"
                    , "SYNONYM"
                    , "SYSDATE"
                    , "TABLE"
                    , "THEN"
                    , "TO"
                    , "TRIGGER"
                    , "UID"
                    , "UNION"
                    , "UNIQUE"
                    , "UPDATE"
                    , "USER"
                    , "VALIDATE"
                    , "VALUES"
                    , "VARCHAR"
                    , "VARCHAR2"
                    , "VIEW"
                    , "WHENEVER"
                    , "WHERE"
                    , "WITH"};

    /**
     * Constant for identifiying oracle Text datatypes
     */
    public static final int ORACLE_TEXT_DATATYPE = 0;

    /**
     * Constant for identifiying oracle Number datatypes
     */
    public static final int ORACLE_NUMBER_DATATYPE = 1;

    /**
     * Constant for identifiying oracle Date datatypes
     */
    public static final int ORACLE_DATE_DATATYPE = 2;

    /**
     * Constant for identifiying oracle Long Text datatypes
     */
    public static final int ORACLE_LONGTEXT_DATATYPE = 3;

    /**
     * Constant for identifiying oracle Long Binary datatypes
     */
    public static final int ORACLE_LONG_BINARY_DATATYPE = 4;

    /**
     * Constant for identifiying oracle Binary datatypes
     */
    public static final int ORACLE_BINARY_DATATYPE = 11;

    /**
     * Constant for identifiying oracle CLOB datatypes
     */
    public static final int ORACLE_CLOB_DATATYPE = 15;

    /**
     * Constant for identifiying oracle BLOB datatypes
     */
    public static final int ORACLE_BLOB_DATATYPE = 16;

    /**
     * Constant for identifiying oracle BFILE datatypes
     */
    public static final int ORACLE_BFILE_DATATYPE = 17;

    /**
     * Constant for identifiying oracle ref cursors
     */
    public static final int ORACLE_REFCURSOR_DATATYPE = 6;

    /**
     * Constant for identifiying PL/SQL Boolean
     */
    public static final int ORACLE_BOOLEAN_DATATYPE = 7;

    /**
     * Constant for identifiying PL/SQL Boolean
     */
    public static final int MCPDBWIZARD_READONLYROWSET = 8;

    /**
     * Constant for identifiying ROWID
     */
    public static final int ORACLE_ROWID_DATATYPE = 9;

    /**
     * Constant for identifiying UROWID
     */
    public static final int ORACLE_UROWID_DATATYPE = 10;

    /**
     * Constant for identifiying TIMESTAMP
     */
    public static final int ORACLE_TIMESTAMP_DATATYPE = 12;

    /**
     * Constant for identifiying TIMESTAMPTZ
     */
    public static final int ORACLE_TIMESTAMPTZ_DATATYPE = 13;

    /**
     * Constant for identifiying TIMESTAMPLTZ
     */
    public static final int ORACLE_TIMESTAMPLTZ_DATATYPE = 14;

    /**
     * Constant for identifiying oracle Collection's TABLE
     */
    public static final int ORACLE_TABLE_DATATYPE = 18;

    /**
     * Constant for identifiying oracle Collection's VARRAY
     */
    public static final int ORACLE_VARRAY_DATATYPE = 19;

    /**
     * Constant for identifiying Oracle OBJECT Datatype
     */
    public static final int ORACLE_OBJECT_DATATYPE = 20;

    /**
     * Constant for identifiying PL/SQL Rowtype Datatype
     */
    public static final int ORACLE_ROWTYPE_DATATYPE = 21;

    /**
     * Constant for identifiying INTERVAL YEAR TO MONTH Datatype
     */
    public static final int ORACLE_INTERVAL_YEAR_TO_MONTH_DATATYPE = 22;

    /**
     * Constant for identifiying INTERVAL DAY TO SECOND Datatype
     *
     * @since Oracle 10.1.0
     */
    public static final int ORACLE_INTERVAL_DAY_TO_SECOND_DATATYPE = 23;

    /**
     * Constant for identifiying PL/SQL Index By tables
     *
     * @since Oracle 10.1.0
     */
    public static final int ORACLE_PLSQL_INDEXBY_DATATYPE = 24;

    /**
     * Constant for identifiying PL/SQL Index By tables
     *
     * @since Oracle 10.1.0
     */
    public static final int ORACLE_PLSQL_INDEXBY_ROWTYPE_DATATYPE = 25;

    /**
     * Constant for identifiying XMLType
     *
     * @since Oracle 10.2.0
     */
    public static final int ORACLE_XMLTYPE_DATATYPE = 26;

    /**
     * Constant for identifiying ORACLE_SDO_GEOMETRY_DATATYPE
     *
     * @since Oracle 10.2.0
     */
    public static final int ORACLE_SDO_GEOMETRY_DATATYPE = 27;

    /**
     * Constant for identifiying the native JSON datatype.
     * <p>
     * The binary (OSON) JSON column type. Read through the JDBC driver as an
     * {@code oracle.sql.json.OracleJsonValue}.
     *
     * @since Oracle 21c
     */
    public static final int ORACLE_JSON_DATATYPE = 28;

    /**
     * Constant for identifiying the native (ISO-SQL) BOOLEAN column datatype.
     * <p>
     * This is the real {@code BOOLEAN} <em>column</em> type, distinct from the
     * older PL/SQL-only boolean ({@link #ORACLE_BOOLEAN_DATATYPE}). It arrives
     * from the driver as a {@link java.lang.Boolean}.
     *
     * @since Oracle 23ai
     */
    public static final int ORACLE_NATIVE_BOOLEAN_DATATYPE = 29;

    /**
     * Constant for identifiying the VECTOR datatype (AI vector embeddings).
     * <p>
     * Covers {@code VECTOR} and its dimensioned/typed forms
     * ({@code VECTOR(3, FLOAT32)}, {@code VECTOR_FLOAT64}, {@code VECTOR_INT8}, ...).
     * Read through the JDBC driver as a {@code double[]} (or {@code float[]} /
     * {@code byte[]} for the storage-typed accessors).
     *
     * @since Oracle 23ai
     */
    public static final int ORACLE_VECTOR_DATATYPE = 30;

    /**
     * Constant for identifiying a native VECTOR whose storage format is
     * <code>BINARY</code> (bit-packed).
     * <p>
     * Distinct from the dense-numeric {@link #ORACLE_VECTOR_DATATYPE} because a
     * binary vector cannot be read/bound as a {@code double[]} (the driver throws
     * {@code ORA-17004}); its natural Java representation is a {@code byte[]}
     * ({@code n} bits = {@code n/8} bytes), bound via
     * {@code oracle.jdbc.OracleType.VECTOR_BINARY}. The format is not visible in
     * {@code DATA_TYPE} (always {@code "VECTOR"}); it is derived from the
     * {@code USER/ALL_TAB_COLUMNS.VECTOR_INFO} descriptor — see
     * {@link #getVectorDatatypeFromInfo(String)}.
     *
     * @since Oracle 23ai
     */
    public static final int ORACLE_VECTOR_BINARY_DATATYPE = 31;

    /**
     * Constant for identifiying a native VECTOR whose storage is
     * <code>SPARSE</code> (index/value pairs rather than a dense array).
     * <p>
     * Recognised so the generator can detect and skip/flag such a column rather
     * than emit the dense {@code double[]} path that fails on it. NOTE: sparse
     * vectors are <em>not readable</em> by the currently bundled Oracle JDBC
     * driver (ojdbc11 23.7 throws {@code ORA-17004} for every Java type and
     * provides no {@code VECTOR_SPARSE} bind type), so no read/write support is
     * wired for this constant yet — see {@code docs/oracle23ai-vector-subformats-plan.md}.
     * Derived from {@code VECTOR_INFO} — see {@link #getVectorDatatypeFromInfo(String)}.
     *
     * @since Oracle 23ai
     */
    public static final int ORACLE_VECTOR_SPARSE_DATATYPE = 32;

    /**
     * Constant for identifiying unrecognized oracle datatypes
     */
    public static final int ORACLE_OTHER_DATATYPE = 99;

    /**
     * Constant for identifiying null datatypes
     */
    public static final int ORACLE_NULL_DATATYPE = 100;

    /**
     * Constant for ORA-4043 - Object does not exist
     */
    public static final int OBJECT_DOES_NOT_EXIST = 4043;

    /**
     * Constant for ORA-17074 message - invalid name pattern
     **/
    public static final int INVALID_NAME_PATTERN = 17074;

    /**
     * Constant for ORA-17059 message - invalid name pattern
     * The most common cause for this is a DB/Driver version mismatch
     **/
    public static final int FAILED_TO_CONVERT_INTERNAL = 17059;

    /**
     * Constant for ORA-2303 message - cannot drop or replace a type with type or table dependents
     **/
    public static final int TYPE_HAS_DEPENDENTS = 2303;

    /**
     * Reduce an Oracle TYPE specification to the form that two spellings of the same type share.
     *
     * <p>Generated code uses this to tell a type that merely could not be replaced (ORA-2303 is the
     * ordinary outcome once the {@code _A} collection depends on the {@code _T} object type) from one
     * whose definition has actually changed underneath it. Oracle keeps a type's specification text
     * in {@code USER_SOURCE} exactly as submitted, so the comparison is against the DDL itself rather
     * than against {@code USER_TYPE_ATTRS} - which would need the DDL's datatype spellings mapped
     * onto the data dictionary's, and would report a difference wherever that mapping was imperfect.
     *
     * <p>Discarded: everything up to and including the leading {@code TYPE} keyword (so
     * {@code CREATE OR REPLACE TYPE x ...} and the stored {@code TYPE x ...} agree), semicolons,
     * case, and <b>all</b> whitespace - the same statement arrives as one concatenated line from
     * generated Java and as several lines from {@code extraObjects.sql}. Removing whitespace outright
     * is safe because a type specification contains no string literals.
     *
     * @param theSpec a CREATE TYPE statement or a stored specification; null is treated as empty
     * @return the comparable form, never null
     */
    public static String normaliseTypeSpec(String theSpec) {
        if (theSpec == null) {
            return "";
        }
        String theText = theSpec.toUpperCase();
        int theStart = theText.indexOf("TYPE ");
        if (theStart > -1) {
            theText = theText.substring(theStart + 5);
        }
        StringBuffer theResult = new StringBuffer(theText.length());
        for (int i = 0; i < theText.length(); i++) {
            char theChar = theText.charAt(i);
            if (!Character.isWhitespace(theChar) && theChar != ';') {
                theResult.append(theChar);
            }
        }
        return theResult.toString();
    }

    /**
     * A set of useful static methods for working with SQL.
     */
    public SqlUtils() {
    }

    /**
     * The comment every generated statement carries, so a DBA can find this product's SQL in
     * {@code V$SQL} without knowing what the application was called.
     *
     * <p>Deliberately a plain comment rather than anything Oracle interprets: it costs a few bytes
     * per cursor, survives into the shared pool exactly as written, and cannot change a plan. It is
     * emitted by BOTH halves of the generator — the PL/SQL call builder and the SQL statement
     * builder — so it is declared once here rather than typed twice.
     *
     * <p>Anything matching on it should match this constant, not a literal: it read
     * {@code "Created  By"} with two spaces until 2026-08-16, so a hand-written
     * {@code sql_text LIKE} against old and new output has to allow for both.
     */
    public static final String GENERATED_BY_COMMENT =
            "/* Created By " + Namer.param_product_name_long + " */";

    /**
     * Classify a SQL statement as a SELECT, INSERT, UPDATE, etc.
     *
     * <p><b>Leading comments and whitespace are skipped</b> before the first keyword is read. That
     * was once a known bug, and fixing it is what allows {@link #GENERATED_BY_COMMENT} to be put at
     * the START of a generated statement: without it every marked statement classified as
     * {@link #UNKNOWN}, which in {@code DmlStatement} means a statement that no longer knows whether
     * it is a query. Block comments may nest neither in SQL nor here; a comment that is never closed
     * consumes the rest of the string, which then classifies as UNKNOWN — correctly, since such a
     * statement would not parse either.
     *
     * @param theStatement a SQL Statement
     * @return an int that will be a SqlUtils constant such as SqlUtils.SELECT, SqlUtils.UPDATE, etc.
     * @since 2.0.1527 Support for Lock, Merge and DDL statements
     */
    public static int getStatementType(String theStatement) {
        int statementType = SqlUtils.UNKNOWN;
        String tempStatement = skipLeadingComments(theStatement).toUpperCase();

        if (tempStatement.startsWith("SELECT")) {
            statementType = SqlUtils.SELECT;
        } else if (tempStatement.startsWith("INSERT")) {
            statementType = SqlUtils.INSERT;
        } else if (tempStatement.startsWith("UPDATE")) {
            statementType = SqlUtils.UPDATE;
        } else if (tempStatement.startsWith("DELETE")) {
            statementType = SqlUtils.DELETE;
        } else if (tempStatement.startsWith("LOCK")) {
            statementType = SqlUtils.LOCK;
        } else if (tempStatement.startsWith("MERGE")) {
            statementType = SqlUtils.MERGE;
        } else if (tempStatement.startsWith("EXPLAIN PLAN")) {
            statementType = SqlUtils.XPLAN;
        } else if (tempStatement.startsWith("SAVEPOINT")) {
            statementType = SqlUtils.SAVEPOINT;
        } else if (tempStatement.startsWith("SET CONSTRAINTS")) {
            statementType = SqlUtils.SET_CONSTRAINTS;
        } else if (tempStatement.startsWith("SET TRANSACTION")) {
            statementType = SqlUtils.SET_TRANSACTION;
        } else if (tempStatement.startsWith("SET ROLE")) {
            statementType = SqlUtils.SET_ROLE;
        } else if (tempStatement.startsWith("CALL")
                || tempStatement.startsWith("DECLARE")
                || tempStatement.startsWith("BEGIN")
        ) {
            statementType = SqlUtils.PLSQL;
        } else if (tempStatement.startsWith("ALTER")
                || tempStatement.startsWith("ANALYZE")
                || tempStatement.startsWith("ASSOCIATE")
                || tempStatement.startsWith("AUDIT")
                || tempStatement.startsWith("COMMENT")
                || tempStatement.startsWith("COMMIT")
                || tempStatement.startsWith("CREATE")
                || tempStatement.startsWith("DISASSOCIATE")
                || tempStatement.startsWith("DROP")
                || tempStatement.startsWith("GRANT")
                || tempStatement.startsWith("NOAUDIT")
                || tempStatement.startsWith("RENAME")
                || tempStatement.startsWith("REVOKE")
                || tempStatement.startsWith("ROLLBACK")
                || tempStatement.startsWith("TRUNCATE")
        ) {
            statementType = SqlUtils.DDL;
        }

        return (statementType);

    }

    /**
     * Strip whitespace and any leading {@code /*...*}{@code /} or {@code --} comments, so the caller
     * sees the first real keyword.
     *
     * <p>Only LEADING comments are touched. Nothing else in the statement is rewritten — this
     * returns a view for classification, never the text that is sent to Oracle.
     *
     * @param theStatement a SQL statement; null is treated as empty
     * @return the statement from its first non-comment, non-whitespace character
     */
    static String skipLeadingComments(String theStatement) {
        if (theStatement == null) {
            return "";
        }
        int i = 0;
        int len = theStatement.length();
        while (i < len) {
            char c = theStatement.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (c == '/' && i + 1 < len && theStatement.charAt(i + 1) == '*') {
                int end = theStatement.indexOf("*/", i + 2);
                // Unterminated: consume the rest, which classifies as UNKNOWN. That is the right
                // answer -- Oracle would not parse it either.
                i = (end < 0) ? len : end + 2;
            } else if (c == '-' && i + 1 < len && theStatement.charAt(i + 1) == '-') {
                int end = theStatement.indexOf('\n', i + 2);
                i = (end < 0) ? len : end + 1;
            } else {
                break;
            }
        }
        return theStatement.substring(i);
    }

    /**
     * Count the JDBC bind parameters in a SQL statement.
     *
     * <p>Comments are skipped, so a {@code ?} inside one is not counted and -- the case that
     * actually bit -- an apostrophe inside one does not open a phantom string literal. The demo
     * statement {@code CustomerDelete} carries the comment "If you don't have a comment after a
     * parameter"; that lone apostrophe used to swallow the rest of the statement, so its real
     * {@code ?} went uncounted, {@code StatementParameters2} was sized 0, and every call died with
     * "Attempt to set parameter number '1' even though statement doesn't take parameters."
     *
     * <p>A doubled {@code ''} escape inside a literal needs no special case: the toggle turns off
     * then straight back on, which lands in the right state at the closing quote.
     *
     * @param  aSqlStatement a SQL Statement; null is treated as empty
     * @return an int The number of JDBC parameters in this SQL statement
     */
    public static int countParameters(String aSqlStatement) {
        if (aSqlStatement == null) {
            return 0;
        }

        final int len = aSqlStatement.length();
        boolean inQuote = false;
        int paramCounter = 0;

        for (int i = 0; i < len; i++) {
            char c = aSqlStatement.charAt(i);

            // Comments are only comments outside a string literal.
            if (!inQuote && c == '/' && i + 1 < len && aSqlStatement.charAt(i + 1) == '*') {
                int end = aSqlStatement.indexOf("*/", i + 2);
                // Unterminated: consume the rest. Oracle would not parse it either.
                i = (end < 0) ? len : end + 1;
                continue;
            }

            if (!inQuote && c == '-' && i + 1 < len && aSqlStatement.charAt(i + 1) == '-') {
                int end = aSqlStatement.indexOf('\n', i + 2);
                i = (end < 0) ? len : end;
                continue;
            }

            if (c == '\'') {
                inQuote = !inQuote;
                continue;
            }

            if (!inQuote && c == '?') {
                paramCounter++;
            }
        }

        return (paramCounter);
    }

    /**
     * Return an <code>int</code> that represents the underlying oracle data type.
     * This method takes an oracle data type and classifies it as Text, Number, Date
     * Long Text or Other.
     *
     * @param  theColumnDataType
     * @return int A SqlUtils constant that represents the underlying oracle data type.
     */
    public static int getUnderlyingOracleDatatype(String theColumnDataType) {
        int returnCode = ORACLE_OTHER_DATATYPE;
        if (theColumnDataType == null) {
            returnCode = ORACLE_NULL_DATATYPE;
        } else if (theColumnDataType.equals("VARCHAR2")
                || theColumnDataType.equals("VARCHAR")
                || theColumnDataType.equals("CHAR")
                || theColumnDataType.equals("CHARACTER")
                || theColumnDataType.equals("STRING")
                // National-character string types are treated as ordinary text. The
                // national charset's form-of-use is not applied here -- see KLUGE 001 in
                // DatatypeWrangler -- so this is safe when the database main charset is
                // Unicode (e.g. AL32UTF8) or the data is within its repertoire.
                || theColumnDataType.equals("NCHAR")
                || theColumnDataType.equals("NCHARACTER")
                || theColumnDataType.equals("NVARCHAR")
                || theColumnDataType.equals("NVARCHAR2")
        ) {
            returnCode = ORACLE_TEXT_DATATYPE;
        } else if (theColumnDataType.equals("ROWID")) {
            returnCode = ORACLE_ROWID_DATATYPE;
        } else if (theColumnDataType.equals("UROWID")) {
            returnCode = ORACLE_UROWID_DATATYPE;
        } else if (theColumnDataType.equals("DATE")) {
            returnCode = ORACLE_DATE_DATATYPE;
        } else if (theColumnDataType.equals("NUMBER")
                || theColumnDataType.equals("FLOAT")
                || theColumnDataType.equals("NATURAL")
                || theColumnDataType.equals("NATURALN")
                || theColumnDataType.equals("POSITIVE")
                || theColumnDataType.equals("POSITIVEN")
                || theColumnDataType.equals("SIGNTYPE")
                || theColumnDataType.equals("BINARY_INTEGER")
                || theColumnDataType.equals("DEC")
                || theColumnDataType.equals("DECIMAL")
                || theColumnDataType.equals("DOUBLE PRECISION")
                || theColumnDataType.equals("FLOAT")
                || theColumnDataType.equals("INTEGER")
                || theColumnDataType.equals("INT")
                || theColumnDataType.equals("NUMERIC")
                || theColumnDataType.equals("REAL")
                || theColumnDataType.equals("SMALLINT")
                || theColumnDataType.equals("PLS_INTEGER")
                || theColumnDataType.equals("DECFLOAT")//DB2
                // IEEE-754 binary floating-point types (Oracle 10g+). They ride the NUMBER
                // path (java.math.BigDecimal / OracleTypes.NUMERIC); Oracle converts between
                // BINARY_FLOAT/BINARY_DOUBLE and NUMBER. The special values BINARY_FLOAT_INFINITY
                // / _NAN have no BigDecimal representation and are out of scope.
                || theColumnDataType.equals("BINARY_FLOAT")
                || theColumnDataType.equals("BINARY_DOUBLE")
        ) {
            returnCode = ORACLE_NUMBER_DATATYPE;
        } else if (theColumnDataType.equals("LONG")) {
            returnCode = ORACLE_LONGTEXT_DATATYPE;
        } else if (theColumnDataType.equals("CLOB")
                || theColumnDataType.equals("NCLOB")) {
            // NCLOB rides the CLOB path: it is a character LOB and oracle.sql.NCLOB extends
            // oracle.sql.CLOB, so the same locator, loader and getCLOB handling apply. The
            // generator normalizes the "NCLOB" name to "CLOB" where it would otherwise emit a
            // non-existent LongObjectLoader.loadNCLOB call (see CallableStatementParameterEngine).
            returnCode = ORACLE_CLOB_DATATYPE;
        } else if (theColumnDataType.equals("com.mcpdbwizard.pub.ReadOnlyRowSet")) {
            returnCode = MCPDBWIZARD_READONLYROWSET;
        } else if (theColumnDataType.equals("ORACLE COLLECTION")) {
            returnCode = ORACLE_TABLE_DATATYPE;
        } else if (theColumnDataType.equals("TABLE")) {
            returnCode = ORACLE_TABLE_DATATYPE;
        } else if (theColumnDataType.equals("VARRAY")) {
            returnCode = ORACLE_VARRAY_DATATYPE;
        } else if (theColumnDataType.equals("OBJECT")) {
            returnCode = ORACLE_OBJECT_DATATYPE;
        } else if (theColumnDataType.equals("PL/SQL BOOLEAN")) {
            returnCode = ORACLE_BOOLEAN_DATATYPE;
        } else if (theColumnDataType.equals("PL/SQL RECORD")) {
            returnCode = ORACLE_ROWTYPE_DATATYPE;
        } else if (theColumnDataType.equals("LONG RAW")) {
            returnCode = ORACLE_LONG_BINARY_DATATYPE;
        } else if (theColumnDataType.equals("BLOB")) {
            returnCode = ORACLE_BLOB_DATATYPE;
        } else if (theColumnDataType.equals("BFILE")) {
            returnCode = ORACLE_BFILE_DATATYPE;
        } else if (theColumnDataType.equals("RAW")) {
            returnCode = ORACLE_BINARY_DATATYPE;
        } else if (theColumnDataType.equals("REF CURSOR")) {
            returnCode = MCPDBWIZARD_READONLYROWSET;
        } else if (theColumnDataType.equals("INTERVAL YEAR TO MONTH")
                || theColumnDataType.equals("INTERVALYM")
                || (theColumnDataType.startsWith("INTERVAL YEAR")
                && theColumnDataType.endsWith("TO MONTH"))) {
            returnCode = ORACLE_INTERVAL_YEAR_TO_MONTH_DATATYPE;
        } else if (theColumnDataType.equals("INTERVAL DAY TO SECOND")
                || theColumnDataType.equals("INTERVALDS")
                || (theColumnDataType.startsWith("INTERVAL DAY")
                && theColumnDataType.indexOf("TO SECOND") > -1))
        {
            returnCode = ORACLE_INTERVAL_DAY_TO_SECOND_DATATYPE;
        }
        else if (theColumnDataType.equals("TIMESTAMP WITH LOCAL TIME ZONE") // Datatype according to ALL_SOURCE
                || theColumnDataType.equals("TIMESTAMPLTZ") // Datatype according to ResultSet
                || (theColumnDataType.startsWith("TIMESTAMP") // Allow for "TIMESTAMP(6) WITH LOCAL TIME ZONE"
                && theColumnDataType.endsWith("LOCAL TIME ZONE"))) {
            returnCode = ORACLE_TIMESTAMPLTZ_DATATYPE;
        } else if (theColumnDataType.equals("TIMESTAMP WITH TIME ZONE")  // Datatype according to ALL_SOURCE
                || theColumnDataType.equals("TIMESTAMPTZ")   // Datatype according to ResultSet
                || (theColumnDataType.startsWith("TIMESTAMP") // Allow for "TIMESTAMP(6) WITH LOCAL TIME ZONE"
                && theColumnDataType.endsWith("TIME ZONE"))) {
            returnCode = ORACLE_TIMESTAMPTZ_DATATYPE;
        } else if (theColumnDataType.startsWith("TIMESTAMP")) {
            returnCode = ORACLE_TIMESTAMP_DATATYPE;
        }
        else if (theColumnDataType.startsWith("PL/SQL TABLE"))
        {
            returnCode = ORACLE_PLSQL_INDEXBY_DATATYPE;
        }
        else if (theColumnDataType.equals("XMLTYPE"))
        {
            returnCode = ORACLE_XMLTYPE_DATATYPE;
        }
        else if (theColumnDataType.equals("SDO_GEOMETRY"))
        {
            returnCode = ORACLE_SDO_GEOMETRY_DATATYPE;
        }
        else if (theColumnDataType.equals("MDSYS.SDO_GEOMETRY"))
        {
            returnCode = ORACLE_SDO_GEOMETRY_DATATYPE;
        }
        else if (theColumnDataType.equals("JSON"))   // native binary JSON column - Oracle 21c+
        {
            returnCode = ORACLE_JSON_DATATYPE;
        }
        else if (theColumnDataType.equals("BOOLEAN"))   // native ISO-SQL boolean column - Oracle 23ai
        {
            // The PL/SQL-only boolean reports as "PL/SQL BOOLEAN" (handled above), so
            // a bare "BOOLEAN" here is the real SQL column type.
            returnCode = ORACLE_NATIVE_BOOLEAN_DATATYPE;
        }
        else if (theColumnDataType.equals("VECTOR_BINARY"))  // synthetic token from the generator's
        {                                                    // table-column introspection (VECTOR_INFO says BINARY)
            returnCode = ORACLE_VECTOR_BINARY_DATATYPE;
        }
        else if (theColumnDataType.equals("VECTOR_SPARSE"))  // synthetic token (VECTOR_INFO says SPARSE)
        {
            returnCode = ORACLE_VECTOR_SPARSE_DATATYPE;
        }
        else if (theColumnDataType.equals("VECTOR")          // VECTOR / VECTOR(3, FLOAT32) / VECTOR_FLOAT64 ... - Oracle 23ai
                || theColumnDataType.startsWith("VECTOR"))
        {
            returnCode = ORACLE_VECTOR_DATATYPE;
        }

        return (returnCode);
    }

    /**
     * Refine a native VECTOR column's datatype code from its
     * {@code USER/ALL_TAB_COLUMNS.VECTOR_INFO} descriptor.
     * <p>
     * On Oracle 23ai every VECTOR column reports {@code DATA_TYPE = "VECTOR"}
     * regardless of storage format, so {@link #getUnderlyingOracleDatatype(String)}
     * cannot tell dense from binary from sparse. The format <em>is</em> exposed for
     * a table/view column in the {@code VECTOR_INFO} descriptor, which looks like
     * {@code VECTOR(<dim>,<format>,<storage>)} — e.g. {@code VECTOR(3,FLOAT32,DENSE)},
     * {@code VECTOR(16,BINARY,DENSE)}, {@code VECTOR(4,FLOAT32,SPARSE)},
     * {@code VECTOR(*,*,DENSE)}. This method maps that descriptor to the specific
     * constant:
     * <ul>
     *   <li>{@code SPARSE} storage &rarr; {@link #ORACLE_VECTOR_SPARSE_DATATYPE}</li>
     *   <li>{@code BINARY} format (dense) &rarr; {@link #ORACLE_VECTOR_BINARY_DATATYPE}</li>
     *   <li>anything else, incl. dense FLOAT32/FLOAT64/INT8 and flexible {@code (*,*)}
     *       &rarr; {@link #ORACLE_VECTOR_DATATYPE}</li>
     * </ul>
     * A {@code null}, blank, or unrecognised descriptor falls back to the dense
     * {@link #ORACLE_VECTOR_DATATYPE} — the pre-existing behaviour — so callers that
     * have no {@code VECTOR_INFO} (e.g. proc parameters, which do not expose it) are
     * unaffected. Sparse takes precedence over binary because it is the harder
     * limitation (unreadable by the current driver); the two are not combinable in
     * practice.
     *
     * @param vectorInfo the {@code VECTOR_INFO} descriptor, or {@code null}
     * @return one of {@link #ORACLE_VECTOR_DATATYPE},
     *         {@link #ORACLE_VECTOR_BINARY_DATATYPE},
     *         {@link #ORACLE_VECTOR_SPARSE_DATATYPE}
     * @since Oracle 23ai
     */
    public static int getVectorDatatypeFromInfo(String vectorInfo) {
        if (vectorInfo == null) {
            return ORACLE_VECTOR_DATATYPE;
        }

        String upper = vectorInfo.toUpperCase();

        if (upper.indexOf("SPARSE") >= 0) {
            return ORACLE_VECTOR_SPARSE_DATATYPE;
        }
        if (upper.indexOf("BINARY") >= 0) {
            return ORACLE_VECTOR_BINARY_DATATYPE;
        }
        return ORACLE_VECTOR_DATATYPE;
    }

    /**
     *
     * Create and return a BFILE locator.
     * This static method creates an oracle.sql.BFILE object
     * by accessing the database. Note that a 'valid' BFILE will be
     * returned even if theOracleDirectory does not exist or theFileName
     * does not refer to an existing file.
     *
     * @param                           theOracleDirectory
     * @param                           theFileName
     * @param                        theConnection
     * @param  theLog a logging mechanism
     * @param                          debugMessages Create debug messages in log
     * @return oracle.sql.BFILE A BFILE which may or may not be usable.
     * @throws CSException If we can't create a BFILE.
     * @since Oracle 8.1.7 / 4.0.2108
     */
    public static oracle.sql.BFILE createBfileLocator(String theOracleDirectory
            , String theFileName
            , java.sql.Connection theConnection
            , LogInterface theLog
            , boolean debugMessages) throws CSException
    {
        oracle.sql.BFILE newBfile = null;
        try
        {
            if (debugMessages)
            {
                theLog.debug("IOUtils.createBfileLocator: Attempting to create BFILE locator for Directory/File "
                        + theOracleDirectory
                        + "/"
                        + theFileName);
            }
            PreparedStatement getBfileStatement = theConnection.prepareStatement("SELECT /* " + Namer.param_prod_name + " */ bfilename(?,?) FROM DUAL");
            QueryTimeout.apply(getBfileStatement);
            getBfileStatement.setString(1, theOracleDirectory);
            getBfileStatement.setString(2, theFileName);
            ResultSet theResult = getBfileStatement.executeQuery();
            theResult.next();
            newBfile = (oracle.sql.BFILE) theResult.getObject(1);
            theResult.close();
            getBfileStatement.close();
            if (debugMessages)
            {
                theLog.debug("IOUtils.createBfileLocator: Created BFILE locator for Directory/File "
                        + theOracleDirectory
                        + "/"
                        + theFileName);
            }
        }
        catch (SQLException e)
        {
            theLog.error("IOUtils.createBfileLocator: " + e.getMessage());
            throw new CSException("Unable to create oracle.sql.BFILE object :" + e.getMessage());
        }
        return (newBfile);
    }
}



