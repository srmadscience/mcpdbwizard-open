package com.mcpdbwizard.app.common;

//import com.mcpdbwizard.pub.SqlUtils;

/**
 * Partial mapping of USER_SEQUENCES table.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 */
public class OracleFunction extends OracleDdlObject {

    /**
     *
     */
    public String javaName = null;

    /**
     *
     */
    public String oracleName = null;

    public String fixedJavaName = null;

    public OracleFunction(String owner, String objectName) {
        super(owner, objectName, "SEQUENCE", null, null);
    }


    /**
     * Return name of sequence
     */
    public String toString() {
        return (objectName);
    }


}



