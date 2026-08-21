package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The six cases that came back from a live MCP session, plus the ones they imply.
 *
 * <p>Five of these FAILED against the {@code SimpleDateFormat} this class replaced, and four of the
 * five failed <em>silently</em> — returning a plausible wrong instant rather than an error. That is
 * why they are pinned here rather than left to review: nothing about the old behaviour looked wrong
 * from the outside.
 *
 * <p><b>Every expectation is built from {@link ZonedDateTime}, never from a literal epoch number.</b>
 * A hard-coded millisecond value would encode the machine's zone into the test and pass or fail by
 * geography.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class McpDatesTest {

    /** What the server's own clock makes of a local wall time. */
    private static Date localWall(int y, int mo, int d, int h, int mi, int s, int ms) {
        return Date.from(ZonedDateTime.of(y, mo, d, h, mi, s, ms * 1_000_000,
                ZoneId.systemDefault()).toInstant());
    }

    /** What a wall time in a named zone means as an instant. */
    private static Date wallIn(String theZone, int y, int mo, int d, int h, int mi, int s) {
        return Date.from(ZonedDateTime.of(y, mo, d, h, mi, s, 0, ZoneId.of(theZone)).toInstant());
    }

    // ---- item 1: the one that failed loudly ----

    @Test
    void aBareDateIsAcceptedAsMidnight() {
        // REFUSED before this class existed, with "Unparseable date" -- while the tool schema said
        // "ISO-8601 string" and this is ISO-8601. It is also what a model sends for HIRE_DATE.
        assertEquals(localWall(1990, 1, 1, 0, 0, 0, 0), McpDates.parse("1990-01-01"));
    }

    @Test
    void theOnlyFormatThatEverWorkedStillWorks() {
        assertEquals(localWall(1990, 1, 1, 0, 0, 0, 0), McpDates.parse("1990-01-01T00:00:00"));
    }

    // ---- items 2 and 3: the ones that failed silently ----

    @Test
    void aNumericOffsetIsHonouredRatherThanDiscarded() {
        // Before: parsed as 22:44 LOCAL, the offset ignored because SimpleDateFormat stops at the
        // end of its pattern. No error, wrong instant, nothing to notice.
        assertEquals(wallIn("+05:30", 2003, 6, 9, 22, 44, 0),
                McpDates.parse("2003-06-09T22:44:00+05:30"));
    }

    @Test
    void aZuluSuffixIsHonouredRatherThanDiscarded() {
        // The commonest ISO-8601 form an agent emits, and the one most likely to be wrong quietly.
        assertEquals(wallIn("UTC", 2003, 6, 9, 22, 44, 0),
                McpDates.parse("2003-06-09T22:44:00Z"));
    }

    @Test
    void fractionalSecondsSurvive() {
        assertEquals(localWall(2003, 6, 9, 22, 44, 0, 500),
                McpDates.parse("2003-06-09T22:44:00.500"));
    }

    @Test
    void secondsAreOptional() {
        assertEquals(localWall(2003, 6, 9, 22, 44, 0, 0), McpDates.parse("2003-06-09T22:44"));
    }

    // ---- item 4: confident nonsense ----

    @Test
    void animpossibleDateIsRefusedRatherThanRolledOver() {
        // Before: 2004-02-14. A malformed value came back as a real date nobody asked for.
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> McpDates.parse("2003-13-45T00:00:00"));
        assertTrue(e.getMessage().contains("2003-13-45"), e.getMessage());
    }

    @Test
    void trailingRubbishIsRefusedRatherThanIgnored() {
        // The mechanism behind items 2 and 3, stated directly: anything after the pattern used to
        // be discarded in silence.
        assertThrows(IllegalArgumentException.class,
                () -> McpDates.parse("1990-01-01T00:00:00 and then some"));
    }

    // ---- the message, which is what stops an agent retrying blind ----

    @Test
    void theMessageNamesTheExpectationAndNotJustTheInput() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> McpDates.parse("last Tuesday"));
        String m = e.getMessage();
        assertTrue(m.contains("last Tuesday"), m);
        assertTrue(m.contains("1990-01-01T09:30:00Z"), "should show an accepted form: " + m);
        assertTrue(m.contains("+05:30"), "should show the offset form: " + m);
    }

    // ---- nulls and blanks ----

    @Test
    void nullAndBlankAreNull() {
        assertNull(McpDates.parse(null));
        assertNull(McpDates.parse(""));
        assertNull(McpDates.parse("   "));
        assertNull(McpDates.format(null));
        assertNull(McpDates.formatTimestamp(null));
        assertNull(McpDates.parseTimestamp(null));
    }

    // ---- rendering ----

    @Test
    void aDateRendersExactlyAsItAlwaysHas() {
        // Pinned because changing it would alter every date field every existing client reads.
        TimeZone theOriginal = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            assertEquals("2003-06-09T18:38:00",
                    McpDates.format(new Date(wallIn("UTC", 2003, 6, 9, 18, 38, 0).getTime())));
        } finally {
            TimeZone.setDefault(theOriginal);
        }
    }

    @Test
    void aTimestampKeepsItsFractionalSeconds() {
        TimeZone theOriginal = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            java.sql.Timestamp t =
                    new java.sql.Timestamp(wallIn("UTC", 2003, 6, 9, 18, 38, 0).getTime() + 500);
            assertEquals("2003-06-09T18:38:00.500", McpDates.formatTimestamp(t));
        } finally {
            TimeZone.setDefault(theOriginal);
        }
    }

    // ---- the ISO <-> Oracle-mask separator, for a zoned index-by crossing MCP ----

    @Test
    void theIsoSeparatorBecomesTheOneOraclesMaskExpects() {
        // Measured: an ISO T against 'yyyy-mm-dd hh24:mi:ss.ff9 TZR' raises ORA-01858.
        assertEquals("2019-03-01 14:25:36.0+05:30",
                McpDates.toOracleTimestampText("2019-03-01T14:25:36.0+05:30"));
        assertEquals("2019-03-01 14:25:36.0 Asia/Calcutta",
                McpDates.toOracleTimestampText("2019-03-01T14:25:36.0 Asia/Calcutta"));
    }

    @Test
    void theSeparatorSwapIsReversible() {
        String theIso = "2019-03-01T14:25:36.123 Asia/Calcutta";
        assertEquals(theIso,
                McpDates.fromOracleTimestampText(McpDates.toOracleTimestampText(theIso)));
    }

    /**
     * The zone text must survive untouched. A parse-and-reformat would resolve the offset to an
     * instant and render it in the SERVER's zone -- silently replacing what the caller sent, which
     * is the defect this whole area was fixed for.
     */
    @Test
    void theZoneTextIsCarriedThroughUnchanged() {
        assertTrue(McpDates.toOracleTimestampText("2019-03-01T14:25:36.0+05:30").endsWith("+05:30"));
        assertTrue(McpDates.toOracleTimestampText("2019-03-01T14:25:36.0 US/Eastern")
                .endsWith("US/Eastern"));
    }

    /**
     * Only the date/time separator moves. A blanket replace would corrupt a region name -- the kind
     * of shortcut that passes on the test value and fails on somebody's timezone.
     */
    @Test
    void aTinLaterInTheStringIsLeftAlone() {
        assertEquals("2019-03-01 14:25:36.0 US/Eastern",
                McpDates.toOracleTimestampText("2019-03-01T14:25:36.0 US/Eastern"));
        assertEquals("2019-03-01 14:25:36.0 America/Port_of_Spain",
                McpDates.toOracleTimestampText("2019-03-01T14:25:36.0 America/Port_of_Spain"));
    }

    @Test
    void theSeparatorHelpersTolerateNullAndOddInput() {
        assertNull(McpDates.toOracleTimestampText(null));
        assertNull(McpDates.fromOracleTimestampText(null));
        assertEquals("short", McpDates.toOracleTimestampText("short"));
        assertEquals("2019-03-01", McpDates.toOracleTimestampText("2019-03-01"));
    }

    @Test
    void aTimestampRoundTripsThroughTheStringForm() {
        java.sql.Timestamp t = McpDates.parseTimestamp("2003-06-09T18:38:00.500");
        assertEquals("2003-06-09T18:38:00.500", McpDates.formatTimestamp(t));
    }
}
