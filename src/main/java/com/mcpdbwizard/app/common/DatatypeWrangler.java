package com.mcpdbwizard.app.common;

import com.mcpdbwizard.pub.*;

/**
 * @author devteam@mcpdbwizard.com
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 * @version 2
 */
public class DatatypeWrangler {

    /**
     * Used to keep track of the Oracle Version.
     * A future release of Oracle may require that the methods
     * in the class return different values
     */
    String oracleVersion = null;

    /**
     * Log Object
     */
    LogInterface theLog = null;

    /**
     * Class used for mapping Oracle data type to OracleTypes
     */
    public DatatypeWrangler(String oracleVersion, LogInterface theLog) {
        this.oracleVersion = oracleVersion;
        this.theLog = theLog;
    }

    /**
     * Return an <tt>int</tt> that represents the offical oracle data type.
     *
     * @param String An oracle data type
     * @return int A constant that represents the underlying oracle data type.
     * @since 2.0.1503 Support for STRING
     */
    public String getOracletypeCode(String theColumnDataType, LogInterface theLog) throws CSException {
        String returnCode = "SqlUtils.ORACLE_OTHER_DATATYPE";

        if (theColumnDataType.equals("VARCHAR2")
                || theColumnDataType.equals("VARCHAR")
                || theColumnDataType.equals("STRING")) {
            returnCode = "OracleTypes.VARCHAR";
        } else if (theColumnDataType.equals("CHAR")) {
            returnCode = "OracleTypes.CHAR";
        } else if (theColumnDataType.equals("CHARACTER")) {
            returnCode = "OracleTypes.CHAR";
        } else if (theColumnDataType.equals("ROWID")) {
            returnCode = "OracleTypes.ROWID";
        } else if (theColumnDataType.equals("UROWID")) {
            returnCode = "OracleTypes.CHAR";
        }
        // KLUGE 001: the national-character string types are mapped onto their non-national
        // JDBC equivalents (CHAR / VARCHAR). This is correct when the database main charset
        // is Unicode or the data stays within its repertoire; doing it "properly" would
        // register them with the NCHAR form-of-use (OracleTypes.NCHAR/NVARCHAR + setFormOfUse)
        // to preserve the national charset in mixed-charset databases.
        //
        else if (theColumnDataType.equals("NCHAR")
                || theColumnDataType.equals("NCHARACTER")) {
            returnCode = "OracleTypes.CHAR";
        } else if (theColumnDataType.equals("NVARCHAR")
                || theColumnDataType.equals("NVARCHAR2")) {
            returnCode = "OracleTypes.VARCHAR";
        } else if (theColumnDataType.equals("NCLOB")) {
            // NCLOB rides the CLOB path (oracle.sql.NCLOB extends oracle.sql.CLOB).
            returnCode = "OracleTypes.CLOB";
        } else if (theColumnDataType.equals("DATE")) {
            returnCode = "OracleTypes.TIMESTAMP";
        } else if (theColumnDataType.equals("ORACLE COLLECTION")) {
            returnCode = "SqlUtils.ORACLE_TABLE_DATATYPE";
        } else if (theColumnDataType.equals("TABLE")) {
            returnCode = "SqlUtils.ORACLE_TABLE_DATATYPE";
        } else if (theColumnDataType.equals("VARRAY")) {
            returnCode = "SqlUtils.ORACLE_VARRAY_DATATYPE";
        } else if (theColumnDataType.equals("OBJECT")) {
            returnCode = "SqlUtils.ORACLE_OBJECT_DATATYPE";
        } else if (theColumnDataType.equals("READONLYROWSET")) {
            returnCode = "SqlUtils.MCPDBWIZARD_READONLYROWSET";
        } else if (theColumnDataType.equals("NUMBER")
                || theColumnDataType.equals("FLOAT")
                || theColumnDataType.equals("DECFLOAT")
                // IEEE-754 binary floating-point types ride the NUMBER path; Oracle converts
                // BINARY_FLOAT/BINARY_DOUBLE to/from NUMBER (special values Inf/NaN excepted).
                || theColumnDataType.equals("BINARY_FLOAT")
                || theColumnDataType.equals("BINARY_DOUBLE")
                // PL/SQL integer subtypes (NATURAL[N], POSITIVE[N], SIGNTYPE, PLS_INTEGER) and the
                // ANSI numeric aliases (DEC, DECIMAL, INTEGER, INT, NUMERIC, REAL, SMALLINT,
                // DOUBLE PRECISION) are all stored/transferred as NUMBER, so they ride the NUMBER
                // path here -- matching SqlUtils.getUnderlyingOracleDatatype and JavaUtils, which
                // already classify them as ORACLE_NUMBER_DATATYPE / Java type "N".
                || theColumnDataType.equals("NATURAL")
                || theColumnDataType.equals("NATURALN")
                || theColumnDataType.equals("POSITIVE")
                || theColumnDataType.equals("POSITIVEN")
                || theColumnDataType.equals("SIGNTYPE")
                || theColumnDataType.equals("PLS_INTEGER")
                || theColumnDataType.equals("DEC")
                || theColumnDataType.equals("DECIMAL")
                || theColumnDataType.equals("DOUBLE PRECISION")
                || theColumnDataType.equals("INTEGER")
                || theColumnDataType.equals("INT")
                || theColumnDataType.equals("NUMERIC")
                || theColumnDataType.equals("REAL")
                || theColumnDataType.equals("SMALLINT")) {
            returnCode = "OracleTypes.NUMERIC";
        } else if (theColumnDataType.equals("PL/SQL BOOLEAN")) {
            returnCode = "OracleTypes.NUMBER";
        } else if (theColumnDataType.equals("BINARY_INTEGER")) {
            returnCode = "OracleTypes.NUMBER";
        } else if (theColumnDataType.equals("RAW")) {
            returnCode = "OracleTypes.VARBINARY";
        } else if (theColumnDataType.equals("TIMESTAMP WITH LOCAL TIME ZONE") // Datatype according to ALL_SOURCE
                || theColumnDataType.equals("TIMESTAMPLTZ") // Datatype according to ResultSet
                || (theColumnDataType.startsWith("TIMESTAMP") // Allow for "TIMESTAMP(6) WITH LOCAL TIME ZONE"
                && theColumnDataType.endsWith("LOCAL TIME ZONE"))) {
            returnCode = "OracleTypes.TIMESTAMPLTZ";
        } else if (theColumnDataType.equals("TIMESTAMP WITH TIME ZONE")  // Datatype according to ALL_SOURCE
                || theColumnDataType.equals("TIMESTAMPTZ")   // Datatype according to ResultSet
                || (theColumnDataType.startsWith("TIMESTAMP") // Allow for "TIMESTAMP(6) WITH LOCAL TIME ZONE"
                && theColumnDataType.endsWith("TIME ZONE"))) {
            returnCode = "OracleTypes.TIMESTAMPTZ";
        } else if (theColumnDataType.startsWith("TIMESTAMP")) {
            returnCode = "OracleTypes.TIMESTAMP";
        } else if (theColumnDataType.equals("LONG")) {
            returnCode = "OracleTypes.LONGVARCHAR";
        } else if (theColumnDataType.equals("LONG RAW")) {
            returnCode = "OracleTypes.LONGVARBINARY";
        } else if (theColumnDataType.equals("BLOB")) {
            returnCode = "OracleTypes.BLOB";
        } else if (theColumnDataType.equals("CLOB")) {
            returnCode = "OracleTypes.CLOB";
        } else if (theColumnDataType.equals("BFILE")) {
            returnCode = "OracleTypes.BFILE";
        } else if (theColumnDataType.equals("REF CURSOR")) {
            returnCode = "OracleTypes.CURSOR";
        } else if (theColumnDataType.equals("PL/SQL RECORD")) {
            returnCode = "SqlUtils.ORACLE_ROWTYPE_DATATYPE";
        } else if (theColumnDataType.equals("INTERVAL YEAR TO MONTH")
                || theColumnDataType.equals("INTERVALYM")
                || (theColumnDataType.startsWith("INTERVAL YEAR")
                && theColumnDataType.endsWith("TO MONTH"))) {
            returnCode = "OracleTypes.INTERVALYM";
        } else if (theColumnDataType.equals("INTERVAL DAY TO SECOND")
                || theColumnDataType.equals("INTERVALDS")
                || (theColumnDataType.startsWith("INTERVAL DAY")
                && theColumnDataType.indexOf("TO SECOND") > -1)) {
            returnCode = "OracleTypes.INTERVALDS";
        } else if (theColumnDataType.equals("PL/SQL TABLE")) {
            //returnCode = "OracleTypes.PLSQL_INDEX_TABLE";
            returnCode = "SqlUtils.ORACLE_PLSQL_INDEXBY_ROWTYPE_DATATYPE";
        } else if (theColumnDataType.equals("XMLTYPE")
                || theColumnDataType.equals("PUBLIC.XMLTYPE")
                || theColumnDataType.equals("SYS.XMLTYPE")) {
            returnCode = "OracleTypes.OPAQUE";
        } else if (theColumnDataType.equals("SDO_GEOMETRY")) {
            returnCode = "SqlUtils.ORACLE_SDO_GEOMETRY_DATATYPE";
        } else if (theColumnDataType.equals("JSON")) {
            // Native binary (OSON) JSON column (21c+).
            returnCode = "OracleTypes.JSON";
        } else if (theColumnDataType.equals("BOOLEAN")) {
            // Native ISO-SQL BOOLEAN column (23ai). The PL/SQL-only boolean is the
            // separate "PL/SQL BOOLEAN" string handled above.
            returnCode = "OracleTypes.BOOLEAN";
        } else if (theColumnDataType.equals("VECTOR_BINARY")) {
            // Binary (bit-packed) VECTOR column (23ai) — synthetic token from the table-column
            // introspection; bound as a binary vector (byte[]), distinct from dense VECTOR.
            returnCode = "OracleTypes.VECTOR_BINARY";
        } else if (theColumnDataType.equals("VECTOR_SPARSE")) {
            // Sparse VECTOR column (23ai) — synthetic token from the table-column introspection.
            // Binds through the generic VECTOR type carrying a sparse oracle.sql.VECTOR (there is no
            // VECTOR_SPARSE bind type); the SparseVector representation is distinct from dense/binary.
            returnCode = "OracleTypes.VECTOR";
        } else if (theColumnDataType.equals("VECTOR")
                || theColumnDataType.startsWith("VECTOR")) {
            // Native VECTOR column (23ai), incl. VECTOR(n, FLOAT32/FLOAT64/INT8).
            returnCode = "OracleTypes.VECTOR";
        } else {
            throw new CSException("Unknown data type of " + theColumnDataType + " seen");
        }

        return (returnCode);
    }

}


