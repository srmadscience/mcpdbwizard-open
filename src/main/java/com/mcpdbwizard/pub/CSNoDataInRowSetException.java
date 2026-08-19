package com.mcpdbwizard.pub;

/**
 * Thrown when an attempt is made to retrieve a value from an empty readOnlyRowset.
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 * @see ReadOnlyRowSet
 */
public class CSNoDataInRowSetException extends CSException {

    /**
     * Default constructor
     */
    public CSNoDataInRowSetException() {
        super();
    }

    /**
     * Constructor with parameters.
     *
     * @param theExceptionMessage An exception message
     */
    public CSNoDataInRowSetException(String theExceptionMessage) {
        super(theExceptionMessage);
    }
}


