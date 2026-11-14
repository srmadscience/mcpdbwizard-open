package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@link ReadOnlyRowSet#numberToBigDecimal}: what a numeric column's {@code getObject} may return,
 * as a BigDecimal. Oracle returns BigDecimal; pgjdbc returns Integer, Long (an {@code int8}, and
 * {@code count(*)}), Short, Float and Double, each of which was a ClassCastException from
 * {@code getInt} and the other numeric getters before 2026-11-14.
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class ReadOnlyRowSetNumberTest {

    @Test
    void aBigDecimalIsReturnedAsItIs() {
        BigDecimal value = new BigDecimal("12.50");
        assertSame(value, ReadOnlyRowSet.numberToBigDecimal(value));
    }

    @Test
    void everyIntegerTypeConvertsExactly() {
        assertEquals(new BigDecimal(7), ReadOnlyRowSet.numberToBigDecimal(7));
        assertEquals(new BigDecimal(Long.MAX_VALUE), ReadOnlyRowSet.numberToBigDecimal(Long.MAX_VALUE));
        assertEquals(new BigDecimal(-3), ReadOnlyRowSet.numberToBigDecimal((short) -3));
        assertEquals(new BigDecimal(1), ReadOnlyRowSet.numberToBigDecimal((byte) 1));
        assertEquals(new BigDecimal("123456789012345678901234567890"),
                ReadOnlyRowSet.numberToBigDecimal(new BigInteger("123456789012345678901234567890")));
    }

    @Test
    void aFloatKeepsItsOwnDigits() {
        assertEquals(new BigDecimal("0.1"), ReadOnlyRowSet.numberToBigDecimal(0.1f));
        assertEquals(new BigDecimal("4.5"), ReadOnlyRowSet.numberToBigDecimal(4.5d));
    }

    @Test
    void whatNoBigDecimalCanHoldIsRefused() {
        assertNull(ReadOnlyRowSet.numberToBigDecimal(Double.NaN));
        assertNull(ReadOnlyRowSet.numberToBigDecimal(Double.POSITIVE_INFINITY));
        assertNull(ReadOnlyRowSet.numberToBigDecimal(Float.NEGATIVE_INFINITY));
        assertNull(ReadOnlyRowSet.numberToBigDecimal("12"));
        assertNull(ReadOnlyRowSet.numberToBigDecimal(null));
    }
}
