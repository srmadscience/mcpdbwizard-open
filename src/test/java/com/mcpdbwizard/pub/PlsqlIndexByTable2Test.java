package com.mcpdbwizard.pub;

import oracle.jdbc.OracleTypes;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PlsqlIndexByTable2} — the in-memory marshalling holder used to pass
 * PL/SQL {@code INDEX BY} table parameters in and out of generated callable-statement
 * wrappers. No database connection is required: every method here exercises the pure
 * Java-side conversion logic ({@code setArray}/{@code getArrayAs*}) that turns typed Java
 * arrays into the internal {@code String}/{@code BigDecimal} representation and back.
 * <p>
 * This is the database-free half of the legacy {@code com.mcpdbwizard.test.tPlsqlIndexBy2}
 * harness (the other, live-DB half is migrated as {@code T10GPlsqlIndexBy}). The legacy
 * harness drove the now-removed {@code PlsqlIndexByTable2.STRING_ARRAY} alias; the current
 * class is constructed with {@code OracleTypes.VARCHAR}/{@code OracleTypes.NUMBER} plus a
 * decimal-precision, which is what these tests use.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class PlsqlIndexByTable2Test {

    @Test
    void freshTableIsEmptyButGivesBackANonNullArray() {
        PlsqlIndexByTable2 t = new PlsqlIndexByTable2(OracleTypes.VARCHAR, 0);
        assertEquals(0, t.getArrayLength());
        assertNotNull(t.getArray());
        assertEquals(0, t.getArray().length);
    }

    @Test
    void elementBoundsAreSettable() {
        PlsqlIndexByTable2 t = new PlsqlIndexByTable2(OracleTypes.VARCHAR, 0);
        t.setElementMaxLength(50);
        t.setElementMaxCount(7);
        assertEquals(50, t.getElementMaxLength());
        assertEquals(7, t.getElementMaxCount());
    }

    @Test
    void setArrayGrowsElementMaxCountToFit() {
        PlsqlIndexByTable2 t = new PlsqlIndexByTable2(OracleTypes.VARCHAR, 0);
        t.setElementMaxCount(2);
        t.setArray(new Object[]{"a", "b", "c", "d"});
        assertEquals(4, t.getElementMaxCount());
    }

    @Test
    void stringArrayRoundTripsAndPreservesNulls() {
        PlsqlIndexByTable2 t = new PlsqlIndexByTable2(OracleTypes.VARCHAR, 10);
        String[] in = {"a", null, "b", null};
        t.setArray(in);

        Object[] out = t.getArray();
        assertEquals(in.length, out.length);
        for (int i = 0; i < in.length; i++) {
            assertEquals(in[i], out[i], "element " + i);
        }
    }

    @Test
    void nullArrayBecomesAnEmptyArrayNotNull() {
        PlsqlIndexByTable2 t = new PlsqlIndexByTable2(OracleTypes.VARCHAR, 10);
        t.setArray((Object[]) null);
        assertNotNull(t.getArray());
        assertEquals(0, t.getArray().length);
    }

    // --- numeric round-trips across both storage modes (VARCHAR vs NUMBER) and -------------
    // --- both precisions (0 and 10 decimal places), mirroring the legacy harness matrix. ---

    @Test
    void intArraysRoundTripInEveryTypeAndPrecisionCombination() throws CSException {
        int[] in = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        for (int type : new int[]{OracleTypes.VARCHAR, OracleTypes.NUMBER}) {
            for (int precision : new int[]{0, 10}) {
                PlsqlIndexByTable2 t = new PlsqlIndexByTable2(type, precision);
                t.setArray(in);
                assertArrayEquals(in, t.getArrayAsInt(-1),
                        "type=" + type + " precision=" + precision);
            }
        }
    }

    @Test
    void longArraysRoundTripInEveryTypeAndPrecisionCombination() throws CSException {
        long[] in = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        for (int type : new int[]{OracleTypes.VARCHAR, OracleTypes.NUMBER}) {
            for (int precision : new int[]{0, 10}) {
                PlsqlIndexByTable2 t = new PlsqlIndexByTable2(type, precision);
                t.setArray(in);
                assertArrayEquals(in, t.getArrayAsLong(-1),
                        "type=" + type + " precision=" + precision);
            }
        }
    }

    @Test
    void floatArraysRoundTrip() throws CSException {
        float[] in = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        for (int type : new int[]{OracleTypes.VARCHAR, OracleTypes.NUMBER}) {
            for (int precision : new int[]{0, 10}) {
                PlsqlIndexByTable2 t = new PlsqlIndexByTable2(type, precision);
                t.setArray(in);
                assertArrayEquals(in, t.getArrayAsFloat(-1), 0.0001f,
                        "type=" + type + " precision=" + precision);
            }
        }
    }

    @Test
    void doubleArraysRoundTrip() throws CSException {
        double[] in = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        for (int type : new int[]{OracleTypes.VARCHAR, OracleTypes.NUMBER}) {
            for (int precision : new int[]{0, 10}) {
                PlsqlIndexByTable2 t = new PlsqlIndexByTable2(type, precision);
                t.setArray(in);
                assertArrayEquals(in, t.getArrayAsDouble(-1), 0.0001d,
                        "type=" + type + " precision=" + precision);
            }
        }
    }

    @Test
    void bigDecimalArraysRoundTrip() throws CSException {
        BigDecimal[] in = {new BigDecimal(1), new BigDecimal(2), new BigDecimal(3),
                new BigDecimal(4), new BigDecimal(5)};
        for (int type : new int[]{OracleTypes.VARCHAR, OracleTypes.NUMBER}) {
            for (int precision : new int[]{0, 10}) {
                PlsqlIndexByTable2 t = new PlsqlIndexByTable2(type, precision);
                t.setArray(in);
                BigDecimal[] out = t.getArrayAsBigDecimal();
                assertEquals(in.length, out.length);
                for (int i = 0; i < in.length; i++) {
                    assertEquals(0, in[i].compareTo(out[i]),
                            "type=" + type + " precision=" + precision + " element " + i);
                }
            }
        }
    }

    @Test
    void nullElementsComeBackAsTheSuppliedNullToken() throws CSException {
        PlsqlIndexByTable2 t = new PlsqlIndexByTable2(OracleTypes.NUMBER, 0);
        t.setArray(new Object[]{new BigDecimal(5), null, new BigDecimal(7)});
        assertArrayEquals(new int[]{5, -999, 7}, t.getArrayAsInt(-999));
    }

    // --- timestamps -------------------------------------------------------------------------

    @Test
    void timestampArrayRoundTripsWhenPrecisionAllowsNoNanos() throws CSException {
        PlsqlIndexByTable2 t = new PlsqlIndexByTable2(OracleTypes.VARCHAR, 0);
        Timestamp[] in = {
                Timestamp.valueOf("2008-06-01 07:31:15"),
                null,
                Timestamp.valueOf("1967-11-06 06:30:10"),
        };
        t.setArray(in);

        Timestamp[] out = t.getArrayAsTimestamp();
        assertEquals(in.length, out.length);
        for (int i = 0; i < in.length; i++) {
            if (in[i] == null) {
                assertNull(out[i], "element " + i);
            } else {
                assertEquals(0, in[i].compareTo(out[i]), "element " + i);
            }
        }
    }

    @Test
    void timestampWithNanosIsRejectedWhenPrecisionIsZero() {
        PlsqlIndexByTable2 t = new PlsqlIndexByTable2(OracleTypes.VARCHAR, 0);
        Timestamp withNanos = Timestamp.valueOf("1967-11-06 06:30:10");
        withNanos.setNanos(1);
        assertThrows(CSNoNanosAllowedException.class,
                () -> t.setArray(new Timestamp[]{withNanos}));
    }

    // --- RAW (byte[][]) -> hex string -> byte[][] -------------------------------------------

    @Test
    void rawByteArraysRoundTripThroughHexEncoding() {
        PlsqlIndexByTable2 t = new PlsqlIndexByTable2(OracleTypes.VARCHAR, 0);

        byte[][] in = new byte[3][];
        in[0] = null;                       // null row stays null
        in[1] = new byte[255];
        in[2] = new byte[255];
        for (int i = 0; i < 255; i++) {
            in[1][i] = (byte) (i % 127);
            in[2][i] = (byte) (i % 5);
        }

        t.setArray(in);
        byte[][] out = t.getArrayAsRaw();

        assertEquals(in.length, out.length);
        assertNull(out[0]);
        for (int row = 1; row < in.length; row++) {
            assertArrayEquals(in[row], out[row], "row " + row);
        }
    }

    @Test
    void getArrayLengthReflectsTheStoredArray() {
        PlsqlIndexByTable2 t = new PlsqlIndexByTable2(OracleTypes.VARCHAR, 0);
        assertEquals(0, t.getArrayLength());
        t.setArray(new Object[]{"x", "y", "z"});
        assertEquals(3, t.getArrayLength());
    }
}
