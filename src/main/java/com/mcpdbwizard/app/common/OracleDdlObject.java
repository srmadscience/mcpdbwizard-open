package com.mcpdbwizard.app.common;

//import java.sql.Timestamp;

/**
 * Partial mapping of USER_OBJECTS table.
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 */

public class OracleDdlObject {

    /**
     * The owner of the Object.
     * Not in USER_OBJECTS but needed anyway.
     */
    public String owner;

    /**
     * The name of the database Object.
     * maps to OBJECT_NAME in USER_OBJECTS
     */
    public String objectName;

    /**
     * The type of the database Object.
     * maps to OBJECT_TYPE in USER_OBJECTS
     */
    public String objectType;

    /**
     * The exact time the database Object was created.
     * maps to CREATED in USER_OBJECTS
     */
    public java.sql.Timestamp created;

    /**
     * The last time the database Object was modified.
     * maps to LAST_DDL_TIME in USER_OBJECTS
     */
    public java.sql.Timestamp lastDdlTime = null;

    /**
     * Create an instance of this class using information found in USER_OBJECTS
     */
    public OracleDdlObject(String owner, String objectName, String objectType
            , java.sql.Timestamp created, java.sql.Timestamp lastDdlTime) {
        this.owner = new String(owner);
        this.objectName = new String(objectName);
        this.objectType = new String(objectType);
        if (created != null) {
            this.created = new java.sql.Timestamp(created.getTime());
        }
        if (lastDdlTime != null) {
            this.lastDdlTime = new java.sql.Timestamp(lastDdlTime.getTime());
        }
    }
}


