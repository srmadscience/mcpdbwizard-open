package com.mcpdbwizard.pub;

// We're working with JDBC

import java.sql.*;

// We use an Arraylist to store data
import java.util.ArrayList;

// We have to format numbers
import java.text.NumberFormat;

// We have to format dates
import java.text.SimpleDateFormat;

// Oracle always returns numbers as BigDecimal
import java.math.BigDecimal;

// We turn Longs and Clobs into files
import java.io.*;

/**
 * Create a writableRowSet that is based on a ResultSet.
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * <p>
 * Under normal circumstances <a href="https://mcpdbwizard.com" target="_blank" class="manual">MCPDBWizard</a> users
 * will have no reason to use this class directly - the generated code will use it.
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 */
public class WriteableRowSet extends ReadOnlyRowSet {
    /**
     * Create a writableRowSet that is based on a ResultSet.
     *
     * @param theResultSet
     * @param theQuery
     * @param maxRows
     * @param theLog
     * @throws CSException
     */
    public WriteableRowSet(ResultSet theResultSet
            , String theQuery
            , int maxRows
            , LogInterface theLog) throws CSException {
        super(theResultSet, theQuery, maxRows, theLog);
    }

    /**
     * Create a ReadOnlyRowSet that is based on user defined data rather than a
     * ResultSet.
     * <p>
     * This constructor is used to create a rowset based on a set of parameters for
     * a stored procedure call.
     *
     * @param theData An array of Object that contains another array of
     *                     Object. The inner arrays are all of the the same length and map to a row of
     *                     columns.
     * @param columnNames
     * @param columnOracleDatatypeNames
     * @param underlyingOracleDatatypes
     * @param columnJavaDatatypes
     * @param theLog
     * @param downloadedFileDir A directory to keep files containing clobs
     *                     and blobs
     */
    public WriteableRowSet(Object[] theData
            , String[] columnNames
            , String[] columnOracleDatatypeNames
            , int[] underlyingOracleDatatypes
            , int[] columnJavaDatatypes
            , long[] columnLengths
            , int[] columnDecimalPlaces
            , LogInterface theLog
            , File downloadedFileDir
            , boolean keepFiles) {
        super(theData
                , columnNames
                , columnOracleDatatypeNames
                , underlyingOracleDatatypes
                , columnJavaDatatypes
                , columnLengths
                , columnDecimalPlaces
                , theLog
                , downloadedFileDir
                , keepFiles);
    }

    /**
     * Update current row as an array of Object
     *
     * @param currentRow A one dimensional Object array to store as the current row.
     * @throws CSNoDataInRowSetException if the rowset is empty.
     */
    public void setCurrentRow(Object[] currentRow) throws CSNoDataInRowSetException {
        checkRows();
        readOnlyRowSetData.set(currentRowNumber, currentRow);
    }

    /**
     * Delete current row
     *
     * @throws CSNoDataInRowSetException if the rowset is empty.
     */
    public void deleteCurrentRow() throws CSNoDataInRowSetException {
        checkRows();
        readOnlyRowSetData.remove(currentRowNumber);
        rowCount--;
    }

    /**
     * Add a new row.
     * We assume that newRow is an array of Object whose structure exactly matches the existing rows
     *
     * @param newRow A one dimensional Object array to add as a new row.
     */
    public void addNewRow(Object[] newRow) throws CSNoDataInRowSetException {
        int originalRowNumber = getCurrentRowNumber();

        last();
        readOnlyRowSetData.add(newRow);
        readOnlyRowSetData.trimToSize();

        // Increment row counter
        rowCount++;

        setCurrentRowNumber(originalRowNumber);

    }

    /**
     * Set column <code>columnId</code> as a String. Numbers will be formatted. Dates will be formatted according
     * to theTimeStampFormat.
     *
     * @param theColumnName The name of the column
     * @param theValue of column #columnId.
     * @throws CSInvalidColumnIdException       if columnId is not the id of a valid column. Column numbering starts at 0.
     * @throws CSNoDataInRowSetException        if there are no rows in this rowset.
     * @throws CSDBInvalidDatatypeCastException if columnId is not of a data type that can be turned into a String.
     * @throws CSUnsupportedDatatypeException   if columnId is not of a data type that we support.
     */
    public void setString(String theColumnName, String theValue) throws CSInvalidColumnIdException, CSNoDataInRowSetException, CSDBInvalidDatatypeCastException, CSUnsupportedDatatypeException {
        setString(getColumnId(theColumnName), theValue);
    }

    /**
     * Set column <code>columnId</code> as a String. Numbers will be formatted. Dates will be formatted according
     * to theTimeStampFormat.
     *
     * @param columnId The number of the column.
     * @param theValue value of column #columnId if it can be turned into a string.
     * @throws CSInvalidColumnIdException       if columnId is not the id of a valid column. Column numbering starts at 0.
     * @throws CSNoDataInRowSetException        if there are no rows in this rowset.
     * @throws CSDBInvalidDatatypeCastException if columnId is not of a data type that can be turned into a String.
     * @throws CSUnsupportedDatatypeException   if columnId is not of a data type that we support.
     */
    public void setString(int columnId, String theValue) throws CSInvalidColumnIdException, CSNoDataInRowSetException, CSDBInvalidDatatypeCastException, CSUnsupportedDatatypeException {
        Object[] tempRow = getCurrentRow();

        // Will throw CSNoDataInRowSetException if no rows exist.
        checkRows();

        // Will throw CSInvalidColumnIdException if columnId is out of range.
        checkRange(columnId);

        // will throw CSUnsupportedDatatypeException if currentRow(columnId) can not be turned into a String
        tempRow[columnId] = setString(theValue
                , underlyingOracleDatatypes[columnId]
                , columnOracleDatatypeNames[columnId]
                , columnNames[columnId]
                , theTimestampFormat
                , theNumberFormat);

        setCurrentRow(tempRow);

    }

    /**
     * Set column <code>columnName</code> as a native Oracle <code>JSON</code> value (21c+).
     *
     * @param theColumnName The name of the column.
     * @param theValue The JSON document (may be null).
     * @since Oracle 21c
     */
    public void setJSON(String theColumnName, oracle.sql.json.OracleJsonValue theValue) throws CSInvalidColumnIdException, CSNoDataInRowSetException {
        setJSON(getColumnId(theColumnName), theValue);
    }

    /**
     * Set column <code>columnId</code> as a native Oracle <code>JSON</code> value (21c+).
     *
     * @param columnId The number of the column.
     * @param theValue The JSON document (may be null).
     * @since Oracle 21c
     */
    public void setJSON(int columnId, oracle.sql.json.OracleJsonValue theValue) throws CSInvalidColumnIdException, CSNoDataInRowSetException {
        setNativeObject(columnId, theValue);
    }

    /**
     * Set column <code>columnName</code> as a native Oracle <code>VECTOR</code> (23ai).
     *
     * @param theColumnName The name of the column.
     * @param theValue One element per dimension (may be null).
     * @since Oracle 23ai
     */
    public void setVector(String theColumnName, double[] theValue) throws CSInvalidColumnIdException, CSNoDataInRowSetException {
        setVector(getColumnId(theColumnName), theValue);
    }

    /**
     * Set column <code>columnId</code> as a native Oracle <code>VECTOR</code> (23ai).
     *
     * @param columnId The number of the column.
     * @param theValue One element per dimension (may be null).
     * @since Oracle 23ai
     */
    public void setVector(int columnId, double[] theValue) throws CSInvalidColumnIdException, CSNoDataInRowSetException {
        setNativeObject(columnId, theValue);
    }

    /**
     * Set column <code>columnName</code> as a binary (bit-packed) Oracle <code>VECTOR</code> (23ai).
     * Distinct from {@link #setVector(String, double[])} (dense FLOAT32/FLOAT64/INT8 vectors).
     *
     * @param theColumnName The name of the column.
     * @param theValue The packed vector bytes (n bits = n/8 bytes), may be null.
     * @since Oracle 23ai
     */
    public void setVectorBinary(String theColumnName, byte[] theValue) throws CSInvalidColumnIdException, CSNoDataInRowSetException {
        setVectorBinary(getColumnId(theColumnName), theValue);
    }

    /**
     * Set column <code>columnId</code> as a binary (bit-packed) Oracle <code>VECTOR</code> (23ai).
     *
     * @param columnId The number of the column.
     * @param theValue The packed vector bytes (n bits = n/8 bytes), may be null.
     * @since Oracle 23ai
     */
    public void setVectorBinary(int columnId, byte[] theValue) throws CSInvalidColumnIdException, CSNoDataInRowSetException {
        setNativeObject(columnId, theValue);
    }

    /**
     * Set column <code>columnName</code> as a sparse Oracle <code>VECTOR</code> (23ai). Distinct from
     * {@link #setVector(String, double[])} (dense) and {@link #setVectorBinary(String, byte[])}
     * (binary): a sparse vector carries {@code {length, indices, values}} and is not densified.
     *
     * @param theColumnName The name of the column.
     * @param theValue The sparse vector (may be null).
     * @since Oracle 23ai
     */
    public void setVectorSparse(String theColumnName, SparseVector theValue) throws CSInvalidColumnIdException, CSNoDataInRowSetException {
        setVectorSparse(getColumnId(theColumnName), theValue);
    }

    /**
     * Set column <code>columnId</code> as a sparse Oracle <code>VECTOR</code> (23ai).
     *
     * @param columnId The number of the column.
     * @param theValue The sparse vector (may be null).
     * @since Oracle 23ai
     */
    public void setVectorSparse(int columnId, SparseVector theValue) throws CSInvalidColumnIdException, CSNoDataInRowSetException {
        setNativeObject(columnId, theValue);
    }

    /**
     * Set column <code>columnName</code> as a native ISO-SQL <code>BOOLEAN</code> (23ai).
     *
     * @param theColumnName The name of the column.
     * @param theValue The boolean (may be null).
     * @since Oracle 23ai
     */
    public void setBoolean(String theColumnName, Boolean theValue) throws CSInvalidColumnIdException, CSNoDataInRowSetException {
        setBoolean(getColumnId(theColumnName), theValue);
    }

    /**
     * Set column <code>columnId</code> as a native ISO-SQL <code>BOOLEAN</code> (23ai).
     *
     * @param columnId The number of the column.
     * @param theValue The boolean (may be null).
     * @since Oracle 23ai
     */
    public void setBoolean(int columnId, Boolean theValue) throws CSInvalidColumnIdException, CSNoDataInRowSetException {
        setNativeObject(columnId, theValue);
    }

    /**
     * Poke a value that the row set already stores in its native Java form (native
     * JSON / VECTOR / BOOLEAN) straight into the current row's column slot, with the
     * usual range and row guards. No string conversion is performed.
     *
     * @param columnId The number of the column.
     * @param theValue The native value (may be null).
     */
    private void setNativeObject(int columnId, Object theValue) throws CSInvalidColumnIdException, CSNoDataInRowSetException {
        Object[] tempRow = getCurrentRow();

        // Will throw CSNoDataInRowSetException if no rows exist.
        checkRows();

        // Will throw CSInvalidColumnIdException if columnId is out of range.
        checkRange(columnId);

        tempRow[columnId] = theValue;

        setCurrentRow(tempRow);
    }

    /**
     * Set column <code>columnId</code> as a String.
     * Numbers will be formatted. Dates will be formatted according
     * to theTimeStampFormat.
     *
     * @param oracleUnderlyingDatatype Identifies what kind of data we are dealing with.
     * @param oracleDataType The oracle data type of the column
     * @param oracleColumnName The name of the column.
     * @param theTimestampFormat Used if tempObject is a String.
     * @param theNumberFormat Used if tempObject is a Number.
     * @return String the Value of column #columnId if it can be turned into a string.
     * @throws CSUnsupportedDatatypeException   if columnId is not of a data type that we can handle.
     * @throws CSDBInvalidDatatypeCastException if columnId is not of a data type that can be converted to a String
     */
    private Object setString(String theValue
            , int oracleUnderlyingDatatype
            , String oracleDataType
            , String oracleColumnName
            , SimpleDateFormat theTimestampFormat
            , NumberFormat theNumberFormat) throws CSDBInvalidDatatypeCastException, CSUnsupportedDatatypeException {
        String newString = null;

        if (theValue == null) {
            newString = null;
        } else {
            switch (oracleUnderlyingDatatype) {
                case SqlUtils.ORACLE_TEXT_DATATYPE: {
                    newString = theValue.trim();
                    break;
                }
                case SqlUtils.ORACLE_BINARY_DATATYPE: {
                    newString = new String(theValue.getBytes());
                    break;
                }
                case SqlUtils.ORACLE_NATIVE_BOOLEAN_DATATYPE: {
                    // Native ISO-SQL BOOLEAN (23ai): the row stores a java.lang.Boolean,
                    // so parse the string and return the boxed boolean directly.
                    String trimmed = theValue.trim();
                    return Boolean.valueOf(trimmed.equalsIgnoreCase("true")
                            || trimmed.equals("1")
                            || trimmed.equalsIgnoreCase("Y")
                            || trimmed.equalsIgnoreCase("yes"));
                }
                case SqlUtils.ORACLE_LONGTEXT_DATATYPE:
                case SqlUtils.ORACLE_NUMBER_DATATYPE:
                case SqlUtils.ORACLE_DATE_DATATYPE:
                case SqlUtils.ORACLE_LONG_BINARY_DATATYPE:
                case SqlUtils.ORACLE_REFCURSOR_DATATYPE:
                case SqlUtils.MCPDBWIZARD_READONLYROWSET:
                case SqlUtils.ORACLE_BOOLEAN_DATATYPE:
                case SqlUtils.ORACLE_CLOB_DATATYPE:
                case SqlUtils.ORACLE_BLOB_DATATYPE:
                case SqlUtils.ORACLE_BFILE_DATATYPE:
                case SqlUtils.ORACLE_OBJECT_DATATYPE:
                case SqlUtils.ORACLE_TABLE_DATATYPE:
                case SqlUtils.ORACLE_VARRAY_DATATYPE:
                default: {
                    createInvalidDatatypeCastException(oracleDataType, "String", oracleColumnName);
                    break;
                }
            }
        }

        return (newString);
    }
}


