package com.mcpdbwizard.pub;

/**
 * Thrown when an attempt is made to retrieve a value from a column has an unsupported datatype
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 */
public class CSUnsupportedDatatypeException extends CSException {

    /**
     * The name of the datatype that is not supported.
     */
    String theUnsupportedDatatype = "";

    /**
     * Default constructor
     */
    public CSUnsupportedDatatypeException() {
        super();
    }

    /**
     * Constructor with parameters.
     *
     * @param theExceptionMessage An exception message
     * @param theUnsupportedDatatype The name of the unsupported data type.
     */
    public CSUnsupportedDatatypeException(String theExceptionMessage
            , String theUnsupportedDatatype
    ) {
        super(theExceptionMessage);
        this.theUnsupportedDatatype = new String(theUnsupportedDatatype);
    }
}



