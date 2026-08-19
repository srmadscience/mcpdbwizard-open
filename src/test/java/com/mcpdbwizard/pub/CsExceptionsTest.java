package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@code CS*Exception} hierarchy. These verify constructor
 * wiring, field population, message composition and the {@code toString()}
 * formatting of {@link CSColumnException}.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class CsExceptionsTest {

    @Test
    void csExceptionCarriesItsMessageAndIsAnException() {
        CSException e = new CSException("boom");
        assertEquals("boom", e.getMessage());
        assertTrue(e instanceof Exception);
        assertNull(new CSException().getMessage());
    }

    @Test
    void allCsExceptionsShareACommonRoot() {
        assertTrue(new CSDBException() instanceof CSException);
        assertTrue(new CSColumnException(null, null, 0, null, false, 0, 0, null) instanceof CSException);
        assertTrue(new CSInvalidColumnIdException() instanceof CSException);
        assertTrue(new CSDBInvalidDatatypeCastException() instanceof CSException);
    }

    @Test
    void csDbExceptionComposesMessageAndStoresSqlCode() {
        CSDBException e = new CSDBException(942, "table or view does not exist",
                "SELECT * FROM missing", "while loading");
        assertEquals(942, e.theSqlCode);
        // super message is "<sql>:<errMsg>:<comment>"
        assertEquals("SELECT * FROM missing:table or view does not exist:while loading",
                e.getMessage());
    }

    @Test
    void csInvalidColumnIdExceptionStoresMessage() {
        CSInvalidColumnIdException e = new CSInvalidColumnIdException("no such column", 5);
        assertEquals("no such column", e.getMessage());
    }

    @Test
    void csNumberFormatExceptionComposesLocationAndValue() {
        CSNumberFormatException e = new CSNumberFormatException("MyClass.parse", "abc");
        assertEquals("MyClass.parse:abc", e.getMessage());
    }

    @Test
    void csDatatypeCastExceptionStoresFromAndToTypes() {
        CSDBInvalidDatatypeCastException e =
                new CSDBInvalidDatatypeCastException("cannot cast", "NUMBER", "DATE");
        assertEquals("cannot cast", e.getMessage());
        assertEquals("NUMBER", e.theCastedDatatype);
        assertEquals("DATE", e.theCasteeDatatype);
    }

    // ---- CSColumnException.toString() formatting -------------------------

    @Test
    void columnExceptionToStringForNullNotAllowed() {
        CSColumnException e = new CSColumnException(
                "EMP", "ENAME", CSColumnException.NULL_NOT_ALLOWED_HERE,
                null, false, 30, 0, null);
        assertEquals("EMP.ENAME (30,0) NOT NULL: NULL_NOT_ALLOWED_HERE", e.toString());
    }

    @Test
    void columnExceptionToStringForLengthExceededWithNullsAndComment() {
        CSColumnException e = new CSColumnException(
                "EMP", "JOB", CSColumnException.COLUMN_LENGTH_EXCEEDED,
                "TOO LONG", true, 9, 0, "check input");
        assertEquals("EMP.JOB (9,0) NULL: COLUMN_LENGTH_EXCEEDED - check input", e.toString());
    }

    @Test
    void columnExceptionToStringForDecimalPlacesExceeded() {
        CSColumnException e = new CSColumnException(
                "ACCT", "BALANCE", CSColumnException.DECIMAL_PLACES_EXCEEDED,
                null, false, 7, 2, null);
        assertEquals("ACCT.BALANCE (7,2) NOT NULL: DECIMAL_PLACES_EXCEEDED", e.toString());
    }

    @Test
    void columnExceptionFieldsArePopulated() {
        CSColumnException e = new CSColumnException(
                "T", "C", CSColumnException.COLUMN_LENGTH_EXCEEDED,
                "val", true, 12, 3, "note");
        assertEquals("T", e.tableName);
        assertEquals("C", e.columnName);
        assertEquals(CSColumnException.COLUMN_LENGTH_EXCEEDED, e.exceptionType);
        assertEquals("val", e.theValue);
        assertTrue(e.allowsNulls);
        assertEquals(12, e.length);
        assertEquals(3, e.decimalPlaces);
        assertEquals("note", e.theApplicationComment);
    }
}
