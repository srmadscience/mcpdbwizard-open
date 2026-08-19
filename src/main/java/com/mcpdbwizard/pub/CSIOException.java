package com.mcpdbwizard.pub;

/**
 * Thrown when we encounter an IOException
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 */
public class CSIOException extends CSException {

    /**
     * Default constructor
     */
    public CSIOException() {
        super();
    }

    /**
     * Constructor with parameters.
     *
     * @param theExceptionMessage An exception message
     */
    public CSIOException(String theExceptionMessage) {
        super(theExceptionMessage);
    }
}



