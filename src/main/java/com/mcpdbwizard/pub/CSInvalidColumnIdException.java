package com.mcpdbwizard.pub;

/**
 * Thrown when an attempt is made to retrieve a value from a column that does not exist.
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 * @see ReadOnlyRowSet
 */
public class CSInvalidColumnIdException extends CSException {
    int invalidColumnId = 0;

    /**
     * Default constructor
     */
    public CSInvalidColumnIdException() {
        super();
    }

    public CSInvalidColumnIdException(String theExceptionMessage
            , int theInvalidColumnId) {
        super(theExceptionMessage);
        this.invalidColumnId = theInvalidColumnId;
    }
}



