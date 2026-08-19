package com.mcpdbwizard.app.procbuilder;

import java.sql.*;
//import oracle.jdbc.driver.*;
//import java.util.Properties;

import com.mcpdbwizard.app.common.*;
import com.mcpdbwizard.pub.*;

/**
 * @author devteam@mcpdbwizard.com
 * @version 7
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 * Portions Copyright (c) 1999 SpookyAction.com
 */
public class SADbWrangler extends ConnectionWrangler {


    String errType = "";
    String errMsg = "";

    String theDriver = "";

    public SADbWrangler
            (String pDriver
                    , String pHostName
                    , int pPort
                    , String pSid
                    , String pUser
                    , String pPassword
                    , LogInterface pLog) throws CSException {
        super(pHostName, pPort, pSid, pUser, pPassword, pLog);
        theDriver = new String(pDriver);

    }

    public String getDriverName() {
        return (theDriver);
    }

    public void setTrace(boolean newValue) {
        try {
            CallableStatement newCallableStatement = mrConnection.prepareCall("ALTER SESSION SET SQL_TRACE = " + newValue);
            newCallableStatement.execute();
            newCallableStatement.close();
        } catch (SQLException e) {
            System.err.println(e.toString());
        }

    }
}



