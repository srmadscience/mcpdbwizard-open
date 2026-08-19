package com.mcpdbwizard.pub;

/**
 * Thrown by the rowValidate method when a non-null column is null or a
 * number column's contents don't match the size of the corresponding database column.
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 * @since 4.0.1709
 */
public class CSColumnException extends CSException {

    /**
     * Constant to indicate column should not be null or a zero length string.
     */
    public static final int NULL_NOT_ALLOWED_HERE = 0;

    /**
     * Constant to indicate String is to long or a number has too many digits
     * to the left of the decimal point.
     */
    public static final int COLUMN_LENGTH_EXCEEDED = 1;

    /**
     * Constant to indicate column is a number that has too many digits
     * to the left of the decimal point.
     */
    public static final int DECIMAL_PLACES_EXCEEDED = 2;

    /**
     * Table name
     */
    public String tableName = null;

    /**
     * Column name
     */
    public String columnName = null;

    /**
     * What kind of CSColumnException this is - one of  NULL_NOT_ALLOWED_HERE,
     * COLUMN_LENGTH_EXCEEDED or DECIMAL_PLACES_EXCEEDED
     */
    public int exceptionType = Integer.MIN_VALUE;

    /**
     * The value in question
     */
    public Object theValue = null;

    /**
     * Whether null is allowed
     */
    public boolean allowsNulls = false;

    /**
     * How long a String is or how many digits a number has to the left of the
     * decimal point.
     */
    public int length = Integer.MIN_VALUE;

    /**
     * How many digits a number has to the right of the decimal point.
     * Note that in Oracle this can be a negative number. For example:
     * NUMBER(4,0) is 4 digits, no decimal places e.g. 2004
     * NUMBER(6,2) is 4 digits, a decimal point and then 2 digits e.g. 7665.43
     * NUMBER(7,-3) is 4 digits and then 3 zeros  e.g. 9,456,000
     */
    public int decimalPlaces = Integer.MIN_VALUE;

    /**
     * An optional comment
     */
    public String theApplicationComment = null;

    /**
     * Contructor with parameters
     *
     * @param tableName
     * @param columnName
     * @param exceptionType
     * @param theValue
     * @param allowsNulls
     * @param length
     * @param decimalPlaces
     * @param theApplicationComment
     */
    public CSColumnException(String tableName
            , String columnName
            , int exceptionType
            , Object theValue
            , boolean allowsNulls
            , int length
            , int decimalPlaces
            , String theApplicationComment) {
        super();

        this.tableName = tableName;
        this.columnName = columnName;
        this.exceptionType = exceptionType;
        this.theValue = theValue;
        this.allowsNulls = allowsNulls;
        this.length = length;
        this.decimalPlaces = decimalPlaces;
        this.theApplicationComment = theApplicationComment;
    }

    /**
     * Return a String representation of this exception
     */
    public String toString() {
        String description = this.tableName + "." + this.columnName
                + " (" + this.length + "," + this.decimalPlaces + ") ";

        if (this.allowsNulls) {
            description = description + "NULL: ";
        } else {
            description = description + "NOT NULL: ";
        }

        if (exceptionType == NULL_NOT_ALLOWED_HERE) {
            description = description + "NULL_NOT_ALLOWED_HERE";
        } else if (exceptionType == COLUMN_LENGTH_EXCEEDED) {
            description = description + "COLUMN_LENGTH_EXCEEDED";
        } else if (exceptionType == DECIMAL_PLACES_EXCEEDED) {
            description = description + "DECIMAL_PLACES_EXCEEDED";
        }

        if (this.theApplicationComment != null) {
            description = description + " - " + this.theApplicationComment;
        }

        return (description);
    }
}



