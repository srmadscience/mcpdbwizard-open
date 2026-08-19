package com.mcpdbwizard.pub;

/**
 * An extension of Exception used by this package.
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 */
public class CSException extends Exception {

    /**
     * Default constructor
     */
    public CSException() {
        super();
    }

    /**
     * Default constructor that takes a String
     */
    public CSException(String theExceptionMessage) {
        super(theExceptionMessage);
    }
}



