package com.mcpdbwizard.pub;

// We are working wih JDBC

import java.sql.*;

/**
 * A parameterized SQL statement
 * that continues to exist even if the connection it uses is withdrawn.
 * <p>
 * Under normal circumstances <a href="https://mcpdbwizard.com" target="_blank" class="manual">MCPDBWizard</a> users
 * will have no reason to use this class directly - the generated code will use it.
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 */
public abstract class DmlStatement extends StatementParameters2 implements OracleResourceUser {
    /**
     * Text of SQL DML statement
     */
    String statementSqlText = null;

    /**
     * What kind of DML statement this is - SELECT, INSERT, UPDATE or DELETE, etc
     *
     * @see
     */
    int statementType;

    /**
     * Database connection
     */
    Connection theConnection = null;

    /**
     * Prepared form of Sql Statement
     */
    PreparedStatement thePreparedStatement = null;

    /**
     * Log Interface Object
     */
    LogInterface theLog = null;

    /**
     * Create a DML Statement object and give it a connection
     */
    public DmlStatement(String statementSqlText, LogInterface theLog, Connection theConnection) {
        this(statementSqlText, theLog);
        setConnection(theConnection);
    }

    /**
     * Create a DML Statement object.
     */
    public DmlStatement(String statementSqlText, LogInterface theLog) {
        super(statementSqlText, theLog);
        this.theLog = theLog;
        this.statementSqlText = new String(statementSqlText);
        statementType = SqlUtils.getStatementType(statementSqlText);
    }

    /**
     * Provide a connection
     *
     * @param theConnection
     */
    public void setConnection(Connection theConnection) {
        this.theConnection = theConnection;
    }


    /**
     * Complain if the connection does not exist.
     *
     * @throws CSDBException No database connection was available when this method was called
     */
    protected void testConnection() throws CSDBException {
        // Complain if we are without a connection...
        if (theConnection == null) {
            throw new CSDBException(0, "No Connection Provided", statementSqlText, "Execute method called with no Connection present");
        }
    }

    /**
     * Prepare the statement if needed.
     *
     * @return <code>true</code> if we had to prepare the statement
     *         <code>false</code> if the statement was already prepared
     * @throws CSDBException We were unable to prepare the statement
     */
    protected boolean createPreparedStatement() throws CSDBException {
        boolean prepareDoneThisTime = false;

        if (thePreparedStatement == null) {
            prepareDoneThisTime = true;
            try {
                thePreparedStatement = theConnection.prepareStatement(statementSqlText);
                QueryTimeout.apply(thePreparedStatement);
            } catch (java.sql.SQLException e) {
                thePreparedStatement = null;
                throw new CSDBException(e.getErrorCode(), e.toString(), statementSqlText, "Unable to prepare this statement");
            }
        }
        return (prepareDoneThisTime);
    }

    /**
     * Release the current connection
     * <p>
     * The prepared Statement will be closed and nullified.
     */
    public void freeConnection() {
        if (thePreparedStatement != null) {
            try {
                thePreparedStatement.close();
                thePreparedStatement = null;
            } catch (SQLException e) {
                theLog.error("Unable to close " + statementSqlText + " :" + e.getMessage());
            }
        }
        this.theConnection = null;
    }

    /**
     * Check if our connection is usable
     *
     * @return <code>true</code> if our connecion is usable
     *         <code>false</code> if our connecion is not usable
     */
    public boolean connectionIsUsable() {
        if (theConnection == null || theLog == null) {
            return (false);
        }
        return (true);
    }

    /**
     * Used to tell if the object is using Oracle resources.
     *
     * @return <code>true</code> if the object holds a resource.
     *         <code>false</code> if the object does not hold a resource.
     */
    public boolean hasResources() {
        if (theConnection != null) {
            return (true);
        }
        return (false);
    }

    /**
     * Used to tell an object to release its Oracle resources. This method never throws an exception. If
     * releasing the resource will create problems they should be dealt with by the implementing class, not
     * escalated to the calling class.
     *
     * @return <code>true</code> if the objects held an open PreparedStatement, ResultSet or similer resource.
     */
    public boolean releaseResources() {
        freeConnection();
        return (true);
    }

    /**
     * Return underlying java.sql.Statement Object. This method exists so that users
     * can call the various methods such as 'setQueryTimeout' that are defined in
     * the java.sql.Statement interface. Do not use it to replace the Statement
     * object.
     *
     * @return java.sql.Statement
     * @throws CSDBException If we had to try to create the Statement before we could return it and something went wrong.
     * @since 5.0.2267 Retuens Statement Object.
     */
    public java.sql.Statement getUnderlyingStatement() throws CSDBException {
        createPreparedStatement();
        return (thePreparedStatement);
    }

}


