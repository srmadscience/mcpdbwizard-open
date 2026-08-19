package com.mcpdbwizard.pub;

import java.sql.Connection;

/**
 * Represents a PL/SQL VARRAY or TABLE
 * <p>
 * This interface is implemented by generated classes that represents PL/SQL
 * VARRAYS or TABLES. It is used at the bind/getResults stage.
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 * @since Oracle 10g/4.0.1902
 */
public interface PlsqlArray {
    /**
     * Method to set the ArrayList by passing in an array of Object
     *
     * @param newValues an array of Object
     * @throws CSException when array is not usable or is not composed of Object
     */
    public void setNewValuesAsObject(Object[] newValues) throws CSException;

    /**
     * Return array as Object[]
     *
     * @param theConnection - only used when working with STRUCT.
     * @return Object[]
     */
    public Object[] getCurrentValuesAsObject(Connection theConnection);

    /**
     * Return array name
     *
     * @return String The Array Name.
     */
    public String getArrayName();

}

