package com.mcpdbwizard.pub;

import java.sql.SQLException;

/**
 * This interface is used in scenarios where non-Oracle array support is required
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 * @since 6.0.2839 DB2 Compatible version created.
 */
public interface JavaArrayObject {

    /**
     * Return a java.sql.Array which is composed of java.sql.Struct[]
     *
     * @return java.sql.Array
     */
    public java.sql.Array getArray(java.sql.Connection theConnection) throws CSException;

    /**
     * Accept an array of java.sql.Struct
     *
     * @param theArray
     */
    public void setArray(java.sql.Array theArray) throws CSException;

}
