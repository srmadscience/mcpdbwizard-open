package com.mcpdbwizard.pub;

import oracle.jdbc.OracleTypes;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PlsqlIndexByTable} — a pure in-memory holder for PL/SQL
 * INDEX BY table data. No database connection is required.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class PlsqlIndexByTableTest {

    @Test
    void freshTableIsEmptyAndDefaultsToNumberType() {
        PlsqlIndexByTable t = new PlsqlIndexByTable();
        assertEquals(0, t.getArrayLength());
        assertEquals(OracleTypes.NUMBER, t.getDataTypeCode());
        // Default element bounds documented on the class.
        assertEquals(1024, t.getElementMaxLength());
        assertEquals(1024, t.getElementMaxCount());
    }

    @Test
    void elementBoundsAreSettable() {
        PlsqlIndexByTable t = new PlsqlIndexByTable();
        t.setElementMaxLength(50);
        t.setElementMaxCount(7);
        assertEquals(50, t.getElementMaxLength());
        assertEquals(7, t.getElementMaxCount());
    }

    @Test
    void settingStringArrayDetectsStringType() {
        PlsqlIndexByTable t = new PlsqlIndexByTable();
        t.setArray(new Object[]{"a", "b", "c"});
        assertEquals(OracleTypes.VARCHAR, t.getDataTypeCode());
        assertEquals(3, t.getArrayLength());
        assertArrayEquals(new String[]{"a", "b", "c"}, t.getArrayAsString());
    }

    @Test
    void settingNumberArrayDetectsNumberType() {
        PlsqlIndexByTable t = new PlsqlIndexByTable();
        t.setArray(new Object[]{new BigDecimal(1), new BigDecimal(2)});
        assertEquals(OracleTypes.NUMBER, t.getDataTypeCode());
        assertEquals(2, t.getArrayLength());
    }

    @Test
    void nullObjectArrayBecomesEmptyArray() {
        PlsqlIndexByTable t = new PlsqlIndexByTable();
        t.setArray((Object[]) null);
        assertEquals(0, t.getArrayLength());
        assertNotNull(t.getArray());
    }

    @Test
    void setArrayGrowsElementMaxCountButNeverShrinksIt() {
        PlsqlIndexByTable t = new PlsqlIndexByTable();
        t.setElementMaxCount(2);
        // Array larger than the current max => max grows to fit.
        t.setArray(new Object[]{"a", "b", "c", "d"});
        assertEquals(4, t.getElementMaxCount());

        // A smaller array does not shrink the max back down.
        t.setElementMaxCount(10);
        t.setArray(new Object[]{"x"});
        assertEquals(10, t.getElementMaxCount());
    }

    @Test
    void intArrayRoundTripsWithNullToken() {
        PlsqlIndexByTable t = new PlsqlIndexByTable();
        t.setArray(new int[]{10, 20, 30});
        assertEquals(OracleTypes.NUMBER, t.getDataTypeCode());
        assertArrayEquals(new int[]{10, 20, 30}, t.getArrayAsInt(Integer.MIN_VALUE));
    }

    @Test
    void longFloatDoubleConveniencePrimitivesRoundTrip() {
        PlsqlIndexByTable tl = new PlsqlIndexByTable();
        tl.setArray(new long[]{1L, 2L});
        assertArrayEquals(new long[]{1L, 2L}, tl.getArrayAsLong(Long.MIN_VALUE));

        PlsqlIndexByTable tf = new PlsqlIndexByTable();
        tf.setArray(new float[]{1.5f, 2.5f});
        assertArrayEquals(new float[]{1.5f, 2.5f}, tf.getArrayAsFloat(Float.MIN_VALUE), 0.0001f);

        PlsqlIndexByTable td = new PlsqlIndexByTable();
        td.setArray(new double[]{1.25d, 2.75d});
        assertArrayEquals(new double[]{1.25d, 2.75d}, td.getArrayAsDouble(Double.MIN_VALUE), 0.0001d);
    }

    @Test
    void nullElementsAreReplacedByTheSuppliedNullToken() {
        PlsqlIndexByTable t = new PlsqlIndexByTable();
        t.setArray(new Object[]{new BigDecimal(5), null, new BigDecimal(7)});
        assertArrayEquals(new int[]{5, -999, 7}, t.getArrayAsInt(-999));
    }

    @Test
    void getArrayAsStringConvertsNumbersAndPreservesNulls() {
        PlsqlIndexByTable t = new PlsqlIndexByTable();
        t.setArray(new Object[]{new BigDecimal(42), null, new BigDecimal("3.14")});
        assertArrayEquals(new String[]{"42", null, "3.14"}, t.getArrayAsString());
    }

    @Test
    void getArrayAsBigDecimalReturnsTheNumbers() throws Exception {
        PlsqlIndexByTable t = new PlsqlIndexByTable();
        t.setArray(new Object[]{new BigDecimal(1), new BigDecimal(2)});
        BigDecimal[] result = t.getArrayAsBigDecimal();
        assertArrayEquals(new BigDecimal[]{new BigDecimal(1), new BigDecimal(2)}, result);
    }

    @Test
    void getArrayAsBigDecimalThrowsForNonNumericContents() {
        PlsqlIndexByTable t = new PlsqlIndexByTable();
        t.setArray(new Object[]{"not a number"});
        assertThrows(CSException.class, t::getArrayAsBigDecimal);
    }

    @Test
    void emptyTableConversionsReturnEmptyArrays() throws Exception {
        PlsqlIndexByTable t = new PlsqlIndexByTable();
        assertEquals(0, t.getArrayAsInt(0).length);
        assertEquals(0, t.getArrayAsLong(0).length);
        assertEquals(0, t.getArrayAsString().length);
        assertEquals(0, t.getArrayAsBigDecimal().length);
    }
}
