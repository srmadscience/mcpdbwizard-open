package com.mcpdbwizard.pub;

/**
 * Thrown when an attempt is made to retrieve a value in a form that it can not be converted to.
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 */
public class CSDBInvalidDatatypeCastException extends CSException {

    /**
     * The data type we were trying to convert from
     */
    public String theCastedDatatype = null;

    /**
     * The data type we were trying to convert to
     */
    public String theCasteeDatatype = null;

    /**
     * Default constructor
     */
    public CSDBInvalidDatatypeCastException() {
        super();
    }

    /**
     * Constructer that takes parameters.
     *
     * @param theExceptionMessage An application generated message
     * @param theCastedDatatype The data type we were trying to convert from
     * @param theCasteeDatatype The data type we were trying to convert to
     */
    public CSDBInvalidDatatypeCastException
    (String theExceptionMessage
            , String theCastedDatatype
            , String theCasteeDatatype) {
        super(theExceptionMessage);
        this.theCastedDatatype = new String(theCastedDatatype);
        this.theCasteeDatatype = new String(theCasteeDatatype);
    }
}


