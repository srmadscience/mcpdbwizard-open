package com.mcpdbwizard.pub;

/**
 * Thrown when a method which returns a native type such as <code>int,long,double</code>
 * is called for a column whose value is <code>null</code>.
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 * @see ReadOnlyRowSet
 */
public class CSAttemptToGetNullException extends CSException {

    /**
     * Which column we were looking at when this happened.
     */
    public int theColumnId = -1;

    /**
     * Default constructor
     */
    public CSAttemptToGetNullException() {
        super();
    }

    /**
     * Constructor which takes an message and a columnId
     */
    public CSAttemptToGetNullException(String theExceptionMessage
            , int theColumnId) {
        super("Column " + theColumnId + ":" + theExceptionMessage);
        this.theColumnId = theColumnId;
    }
}




