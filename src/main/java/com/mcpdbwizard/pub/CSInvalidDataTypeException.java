package com.mcpdbwizard.pub;

/**
 * Thrown when an we encounter a value whose data type is unsupported
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 */
public class CSInvalidDataTypeException extends CSException {
    /**
     * Data type we we trying to convert from
     */
    String theCasterDatatype = "";

    /**
     * Data type we we trying to convert to
     */
    String theCasteeDatatype = "";

    /**
     * Default constructor
     */
    public CSInvalidDataTypeException() {
        super();
    }

    /**
     * Thrown when an we encounter a value whose data type is unsupported
     *
     */
    public CSInvalidDataTypeException(String theExceptionMessage
            , String theCasterDatatype
            , String theCasteeDatatype) {
        super(theExceptionMessage);
        this.theCasterDatatype = new String(theCasterDatatype);
        this.theCasteeDatatype = new String(theCasteeDatatype);
    }
}


