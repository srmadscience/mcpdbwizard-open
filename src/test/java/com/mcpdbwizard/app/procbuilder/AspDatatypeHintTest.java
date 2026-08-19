package com.mcpdbwizard.app.procbuilder;

import com.mcpdbwizard.pub.JulLog;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * The datatype written into a generated statement's comment must resolve to a token the engine
 * knows, because an unrecognised one falls back to {@code STRING} and the generator carries on.
 *
 * <p><b>This exists because that fallback shipped a generator that emitted uncompilable code.</b> A
 * table with a PRIMARY KEY and a {@code TIMESTAMP WITH [LOCAL] TIME ZONE} column produced
 * {@code setParam<Col>(String)} while the row field is a {@code byte[]}, so the manager passed one
 * to the other and javac refused it — for years, because the comment writer emitted
 * {@code oracle.sql.TIMESTAMPTZ} and the hint parser had no entry for the spelling its own other
 * half produced.
 *
 * <p>The live regen cannot catch this on its own: no committed propfile has a PK'd zoned-timestamp
 * table, so nothing ever compiles that path. Rather than provision a new fixture table on six
 * Oracle boxes to guard a pure string mapping, the mapping is pinned here — db-free, so it runs on
 * every build rather than only when someone drives the estate.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class AspDatatypeHintTest {

    /**
     * Resolve the datatype the generator would use for the single parameter in {@code theSql}.
     *
     * <p><b>NOT ConsoleLog.</b> An unrecognised datatype logs a WARNING, and {@code ConsoleLog}
     * blocks on "Press Enter to continue..." when it warns — so a test using it does not fail when
     * the regression returns, it HANGS, which is worse than no test. Found by running this against
     * the pre-fix generator and watching it stop dead rather than go red.
     */
    private static String resolvedTypeOf(String theSql) {
        SqlStatementWrangler theWrangler = new SqlStatementWrangler(
                "probe.sql", theSql, new Properties(), 0, new JulLog("AspDatatypeHintTest"));
        return theWrangler.paramHintJavaDataTypes[0];
    }

    @Test
    void aZonedTimestampResolvesToItsOwnTokenAndNotToString() {
        // The exact spelling SAAdminWrangler's comment writer emits for these columns.
        assertEquals("TIMESTAMPTZ",
                resolvedTypeOf("insert into t (c) values (? /* c oracle.sql.TIMESTAMPTZ */)"),
                "a TIMESTAMP WITH TIME ZONE column fell back to STRING, which emits a"
                        + " setParam(String) the row's byte[] field cannot be passed to");
        assertEquals("TIMESTAMPLTZ",
                resolvedTypeOf("insert into t (c) values (? /* c oracle.sql.TIMESTAMPLTZ */)"),
                "a TIMESTAMP WITH LOCAL TIME ZONE column fell back to STRING");
    }

    /**
     * The bug was a silent fallback, so the assertion that matters most is that it is NOT taken —
     * asserting only the positive would pass just as well if STRING were added as a third alias.
     */
    @Test
    void theSilentStringFallbackIsNotTaken() {
        assertNotEquals("STRING",
                resolvedTypeOf("insert into t (c) values (? /* c oracle.sql.TIMESTAMPTZ */)"));
        assertNotEquals("STRING",
                resolvedTypeOf("insert into t (c) values (? /* c oracle.sql.TIMESTAMPLTZ */)"));
    }

    /**
     * A zoned timestamp's row field is a {@code byte[]}, which is also RAW's Java spelling — the
     * same ambiguity binary VECTOR columns needed their own token to escape. If the zoned types
     * ever reach the {@code byte[]} synonym they will bind as RAW and store the wire bytes as
     * binary, silently. Ordering is what prevents it, and ordering is easy to disturb.
     */
    @Test
    void aZonedTimestampIsNeverMistakenForRaw() {
        assertNotEquals("RAW",
                resolvedTypeOf("insert into t (c) values (? /* c oracle.sql.TIMESTAMPTZ */)"));
        assertNotEquals("RAW",
                resolvedTypeOf("insert into t (c) values (? /* c oracle.sql.TIMESTAMPLTZ */)"));
        // ...while a genuine byte[] still is RAW, which is what makes the check above meaningful
        // rather than merely true.
        assertEquals("RAW", resolvedTypeOf("insert into t (c) values (? /* c byte[] */)"));
    }

    /**
     * Guards the neighbours the new entries sit between, so a future insertion cannot quietly
     * shadow one. A plain TIMESTAMP is the interesting case: its spelling is a prefix of neither
     * zoned name, but the tokens now differ only by a suffix.
     */
    @Test
    void theNeighbouringDatatypesStillResolveAsBefore() {
        assertEquals("DATE", resolvedTypeOf("insert into t (c) values (? /* c java.sql.Timestamp */)"));
        assertEquals("DATE", resolvedTypeOf("insert into t (c) values (? /* c java.util.Date */)"));
        assertEquals("NUMBER", resolvedTypeOf("insert into t (c) values (? /* c java.math.BigDecimal */)"));
        assertEquals("JSON",
                resolvedTypeOf("insert into t (c) values (? /* c oracle.sql.json.OracleJsonValue */)"));
        assertEquals("VECTOR", resolvedTypeOf("insert into t (c) values (? /* c double[] */)"));
    }
}
