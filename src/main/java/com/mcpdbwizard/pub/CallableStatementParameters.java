package com.mcpdbwizard.pub;

// We prepare JDBC statements

import java.sql.*;

// We use BigDecimal because Oracle does.
import java.math.BigDecimal;

// We use File to store Long data types
import java.io.File;

// We use oracle Extensions
import oracle.jdbc.OracleCallableStatement;
import oracle.jdbc.OracleTypes;

/**
 * A set of parameters for a Callable Statement statement
 * call
 * <p>
 * This class represents OUT or IN/OUT parameters for a Stored Procedure
 * or function. It extends StatementParameters2 which represents IN parameters and
 * is used by both SQL statements and Procedures.
 * <p>
 * Under normal circumstances <a href="https://mcpdbwizard.com" target="_blank" class="manual">MCPDBWizard</a> users
 * will have no reason to use this class directly - the created code will use it.
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 */
public class CallableStatementParameters extends StatementParameters2 {
    /**
     * Text of ref cursor invalid message
     * This message is created by Oracle when a NULL Ref Cursor is returned.
     */
    private static final String REF_CURSOR_INVALID = "Ref cursor is invalid";
    /**
     * Text of curaor closed message
     * This message is created by Oracle 10.1.10 when a zero length cursor is returned.
     */
    private static final String REF_CURSOR_CLOSED = "Cursor is closed.";
    /**
     * Array of boolean for keeping track of whether parameters are set or not.
     */
    protected boolean[] outputParameterSetArray = null;

    /**
     * Create a set of statement Parameters of size <code>howMany</code>.
     *
     * @param howMany The number of parameters the statement requires.
     */
    public CallableStatementParameters(int howMany, LogInterface theLog) {
        super(howMany, theLog);
        clearParameters();
    }

    /**
     * Create a set of statement Parameters by counting '?' characters in a SQL statement
     *
     * @param sqlStatement The statement that we will be working with
     */
    public CallableStatementParameters(String sqlStatement, LogInterface theLog) {
        this(SqlUtils.countParameters(sqlStatement), theLog);
    }

    /**
     * Clear our parameter array
     */
    public void clearParameters() {
        // Explicitly initialize this array as null.
        super.clearParameters();

        outputParameterSetArray = new boolean[parameterArray.length];

        for (int i = 0; i < parameterArray.length; i++) {
            outputParameterSetArray[i] = false;
        }
    }

    /**
     *
     * Set an out parameter for a PL/SQL Index By Table
     * In order to retrieve a PL/SQL Index By Table you need to specify the
     * data type you are expecting and how many records can be in the array.
     * parametervalue is used to store this as well as the resulting array.
     *
     * @param parameterId The id of the parameter to set. Id's start at 0.
     * @param parameterValue
     * @throws CSException if <code>parameterId</code> is not a valid parameter.
     * @since Oracle 10g / 4.0.1798
     */
    public void setPlSqlIndexArrayOutParam(int parameterId
            , com.mcpdbwizard.pub.PlsqlIndexByTable2 parameterValue) throws CSException
    {
        checkRange(parameterId);
        parameterArray[parameterId - 1] = parameterValue;
        parameterTypeArray[parameterId - 1] = OracleTypes.PLSQL_INDEX_TABLE;
        outputParameterSetArray[parameterId - 1] = true;
    }

    /**
     * Set an out parameter for a PL/SQL Array
     * In order to retrieve a PL/SQL varray or table you need to specify the
     * oracle collection name.
     * parametervalue is used to store this as well as the resulting array.
     *
     * @param parameterId The id of the parameter to set. Id's start at 0.
     * @param parameterValue
     * @throws CSException if <code>parameterId</code> is not a valid parameter.
     * @since Oracle 9i / 4.0.1885
     */
    public void setPlSqlTableArrayOutParam(int parameterId
            , com.mcpdbwizard.pub.PlsqlArray parameterValue) throws CSException {
        checkRange(parameterId);
        parameterArray[parameterId - 1] = parameterValue;
        parameterTypeArray[parameterId - 1] = OracleTypes.ARRAY;
        outputParameterSetArray[parameterId - 1] = true;
    }

    /**
     * Set an out parameter
     *
     * @param parameterId The id of the parameter to set. Id's start at 0.
     * @param parameterType A parameter type
     * @throws CSException if <code>parameterId</code> is not a valid parameter.
     */
    public void setOutParam(int parameterId
            , int parameterType) throws CSException {
        checkRange(parameterId);
        parameterTypeArray[parameterId - 1] = parameterType;
        outputParameterSetArray[parameterId - 1] = true;
    }

    /**
     * Set an OPAQUE out parameter
     *
     * @param parameterId The id of the parameter to set. Id's start at 0.
     * @param parameterType A parameter type
     * @param parameterOpaqueTypeName Underlying data type name
     * @throws CSException if <code>parameterId</code> is not a valid parameter.
     * @since 6.0.2746
     */
    public void setOutParam(int parameterId
            , int parameterType
            , String parameterOpaqueTypeName) throws CSException {
        checkRange(parameterId);
        parameterTypeArray[parameterId - 1] = parameterType;
        outputParameterSetArray[parameterId - 1] = true;
        parameterOpaqueTypeNameArray[parameterId - 1] = parameterOpaqueTypeName;
    }

    /**
     * Bind these parameters to a Prepared Statement.
     *
     * @param theCallableStatement The prepared statement you
     *                          want these parameters bound to.
     * @since Oracle 10g / 4.0.1798 Support for PL/SQL INDEX BY tables
     */
    public void bindParameters(CallableStatement theCallableStatement) throws CSException {
        if (parameterArray.length > 0) {
            // First bind all the input params
            super.bindParameters(theCallableStatement);

            // Bind Parameters. A CSDBException will be thrown if something goes wrong.
            for (int i = 0; i < parameterArray.length; i++) {
                try {
                    if (outputParameterSetArray[i]) {
                        if (parameterTypeArray[i] == OracleTypes.ARRAY) {
                            com.mcpdbwizard.pub.PlsqlArray theArray = (com.mcpdbwizard.pub.PlsqlArray) parameterArray[i];
                            ((OracleCallableStatement) theCallableStatement).registerOutParameter(i + 1, OracleTypes.ARRAY, theArray.getArrayName());
                        } else if (parameterTypeArray[i] == OracleTypes.STRUCT)
                        {
                            ((OracleCallableStatement) theCallableStatement).registerOutParameter(i + 1, OracleTypes.STRUCT, parameterOpaqueTypeNameArray[i]);
                        }
                        else if (parameterTypeArray[i] == OracleTypes.PLSQL_INDEX_TABLE)
                        {
                            com.mcpdbwizard.pub.PlsqlIndexByTable2 theTable = (com.mcpdbwizard.pub.PlsqlIndexByTable2) parameterArray[i];
                            ((OracleCallableStatement) theCallableStatement).registerIndexTableOutParameter(i + 1
                                    , theTable.getElementMaxCount(), theTable.getRealDataTypeCode(), theTable.getElementMaxLength());
                        }
                        else if (parameterTypeArray[i] == OracleTypes.OPAQUE)
                        {
                            theCallableStatement.registerOutParameter(i + 1, parameterTypeArray[i], parameterOpaqueTypeNameArray[i]);
                        }
                        else {
                            theCallableStatement.registerOutParameter(i + 1, parameterTypeArray[i]);
                        }
                    }
                } catch (SQLException e) {
                    theCallableStatement = null;
                    if (e.getErrorCode() == SqlUtils.INVALID_NAME_PATTERN) {
                        throw new CSException("CallableStatementParameters: Error while trying to set output parameter "
                                + (i + 1)
                                + ":ORA-" + SqlUtils.INVALID_NAME_PATTERN + ". This can happen if you are using PL/SQL Package arrays and "
                                + "haven't created the extra objects required by " + Namer.param_prod_name + ". "
                                + "Try running the 'extraObjects.sql' script or calling the "
                                + "'createExtraTypeObjects()' method in your service class "
                                + ". Message Detail: "
                                + e.getMessage()
                        );
                    }
                    throw new CSException("CallableStatementParameters: Error while trying to set output parameter " + (i + 1) + ":" + e.getClass().getName() + ":" + e.getMessage());
                } catch (Exception e) {
                    theCallableStatement = null;
                    throw new CSException("CallableStatementParameters: Error while trying to set output parameter " + i + ":" + e.getClass().getName() + ":" + e.getMessage());
                }
            }
        }
    }

    /**
     * Unload these parameter from a Prepared Statement.
     *
     * @param theCallableStatement The prepared statement you
     *                          want these parameters unloaded from.
     * @since Oracle 10g / 4.0.1798 Support for PL/SQL INDEX BY tables
     * @since Oracle 9.0.1 / 4.0.1847 Support for PL/SQL  tables
     */
    public void unloadParameters(CallableStatement theCallableStatement) throws CSException {
        if (parameterArray.length > 0) {
            // Unload Parameters. A CSDBException will be thrown if something goes wrong.
            for (int i = 0; i < parameterArray.length; i++) {
                try {
                    if (outputParameterSetArray[i]) {
                        if (parameterTypeArray[i] == OracleTypes.LONGVARBINARY) {
                            parameterArray[i] = ((OracleCallableStatement) theCallableStatement).getBinaryStream(i + 1);
                        } else if (parameterTypeArray[i] == OracleTypes.CLOB) {
                            parameterArray[i] = ((OracleCallableStatement) theCallableStatement).getCLOB(i + 1);
                        } else if (parameterTypeArray[i] == OracleTypes.BLOB) {
                            parameterArray[i] = ((OracleCallableStatement) theCallableStatement).getBLOB(i + 1);
                        } else if (parameterTypeArray[i] == OracleTypes.BFILE) {
                            parameterArray[i] = ((OracleCallableStatement) theCallableStatement).getBFILE(i + 1);
                        } else if (parameterTypeArray[i] == OracleTypes.PLSQL_INDEX_TABLE)
                        {
                            // Since 12.1 getPlsqlIndexTable throws a NPE if the result is null
                            com.mcpdbwizard.pub.PlsqlIndexByTable2 theTable = (com.mcpdbwizard.pub.PlsqlIndexByTable2) parameterArray[i];
                            try
                            {
                                Object[] arrayResult = (Object[]) ((OracleCallableStatement) theCallableStatement).getPlsqlIndexTable(i + 1);
                                theTable.setArray(arrayResult);
                            }
                            catch (NullPointerException npe)
                            {
                                theTable.setArray(new Object[0]);
                            }
                        }
                        else if (parameterTypeArray[i] == OracleTypes.ARRAY) {
                            oracle.sql.ARRAY arrayResult = ((OracleCallableStatement) theCallableStatement).getARRAY(i + 1);
                            com.mcpdbwizard.pub.PlsqlArray theArray = (com.mcpdbwizard.pub.PlsqlArray) parameterArray[i];
                            theArray.setNewValuesAsObject(arrayResult.getOracleArray());
                        } else if (parameterTypeArray[i] == OracleTypes.CURSOR) {
                            try {
                                parameterArray[i] = ((OracleCallableStatement) theCallableStatement).getCursor(i + 1);
                            } catch (Exception e) {
                                if (e.getMessage().equalsIgnoreCase(REF_CURSOR_INVALID)) {
                                    // REF_CURSOR_INVALID is generated when an attempt is made
                                    // to access a cursor parameter that is null.
                                    parameterArray[i] = null;
                                } else if (e.getMessage().equalsIgnoreCase(REF_CURSOR_CLOSED)) {
                                    // REF_CURSOR_CLOSED is generated in Oracle 10G when an attempt is made
                                    // to access a cursor that contains zero rows.
                                    parameterArray[i] = null;
                                } else {
                                    throw (e);
                                }
                            }
                        } else if (parameterTypeArray[i] == OracleTypes.JSON) {
                            // Native JSON (21c+): read back as an OracleJsonValue.
                            parameterArray[i] = theCallableStatement.getObject(i + 1, oracle.sql.json.OracleJsonValue.class);
                        } else if (parameterTypeArray[i] == OracleTypes.VECTOR) {
                            // Native VECTOR (23ai): read back as one double per dimension.
                            // An unconstrained VECTOR OUT parameter is format-flexible, so peek at
                            // the raw oracle.sql.VECTOR first: a SPARSE value comes back as a
                            // SparseVector (preserved, not densified); otherwise read as double[]
                            // (dense) and, if a BINARY (bit-packed) value refuses that conversion
                            // (ORA-17004), fall back to byte[]. The generated wrappers dispatch on
                            // the runtime type.
                            oracle.sql.VECTOR vecOut = theCallableStatement.getObject(i + 1, oracle.sql.VECTOR.class);
                            if (vecOut != null && vecOut.isSparse()) {
                                parameterArray[i] = com.mcpdbwizard.pub.SparseVector.fromVector(vecOut);
                            } else {
                                try {
                                    parameterArray[i] = theCallableStatement.getObject(i + 1, double[].class);
                                } catch (java.sql.SQLException e) {
                                    parameterArray[i] = theCallableStatement.getObject(i + 1, byte[].class);
                                }
                            }
                        } else if (parameterTypeArray[i] == OracleTypes.VECTOR_BINARY) {
                            // Binary (bit-packed) VECTOR (23ai): read back as byte[] (double[] fails).
                            parameterArray[i] = theCallableStatement.getObject(i + 1, byte[].class);
                        } else if (parameterTypeArray[i] == OracleTypes.BOOLEAN) {
                            // Native ISO-SQL BOOLEAN (23ai).
                            parameterArray[i] = theCallableStatement.getObject(i + 1, Boolean.class);
                        } else {
                            parameterArray[i] = theCallableStatement.getObject(i + 1);
                        }
                    }
                } catch (Exception e) {
                    theCallableStatement = null;
                    throw new CSException("Error while trying to get parameter " + (i + 1) + ":" + e.getClass().getName() + ":" + e.getMessage());
                }
            }
        }
    }

    /**
     * Complain if not all parameters set...
     *
     * @throws CSException if one or more parameters is not set.
     */
    public void checkSet() throws CSException {
        for (int i = 0; i < parameterArray.length; i++) {
            if ((!inputParameterSetArray[i])
                    && (!outputParameterSetArray[i])
            ) {
                throw new CSException("Parameter " + (i + 1) + " not set");
            }
        }
    }
}



