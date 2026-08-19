package com.mcpdbwizard.pub;

/**
 * Thrown when a SQLException is generated.
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 */
public class CSDBException extends CSException {

    /**
     * The oracle Error code
     */
    public int theSqlCode = 0;

    /**
     * The Sql statement that caused this exception
     */
    String theSqlStatement = null;

    /**
     * An optional comment
     */
    String theApplicationComment = null;

    /**
     * Default constructor
     */
    public CSDBException() {
        super();
    }

    /**
     * Contructor with parameters
     *
     * @param theSqlCode the oracle error code
     * @param theSqlErrorMessage The message text associated with this exception
     * @param theSqlStatement The Sql statement that caused this exception
     * @param theApplicationComment An optional comment
     */
    public CSDBException(int theSqlCode
            , String theSqlErrorMessage
            , String theSqlStatement
            , String theApplicationComment) {
        super(theSqlStatement + ":" + theSqlErrorMessage + ":" + theApplicationComment);
        this.theSqlCode = theSqlCode;
        this.theSqlStatement = theSqlStatement;
        this.theApplicationComment = theApplicationComment;
    }
}



