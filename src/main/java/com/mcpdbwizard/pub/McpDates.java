package com.mcpdbwizard.pub;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;

/**
 * How a DATE or TIMESTAMP crosses the Model Context Protocol, in one place.
 *
 * <p>The generated MCP server used to carry this itself, as
 * {@code new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")} inlined in two places and a comment asking
 * whoever edited one to remember the other. That arrangement had five separate defects and only the
 * first of them was visible:
 *
 * <ol>
 * <li>{@code 1990-01-01} was REFUSED, though the tool schema said "ISO-8601 string" and that is
 *     ISO-8601. It is also the form a model reaches for when a column is called {@code HIRE_DATE},
 *     so it was the one people hit first.</li>
 * <li>A trailing zone was SILENTLY DROPPED. {@code SimpleDateFormat.parse(String)} stops at the end
 *     of its pattern and ignores whatever follows, so {@code ...T22:44:00+05:30} and
 *     {@code ...T22:44:00Z} both parsed as local time — no error, wrong instant, no signal.</li>
 * <li>Fractional seconds went the same way, which costs a DATE nothing and costs a TIMESTAMP real
 *     data.</li>
 * <li>Parsing was LENIENT, so {@code 2003-13-45} came back as {@code 2004-02-14} rather than as an
 *     error: confident nonsense.</li>
 * <li>Nothing carried a zone at all, so an accepted string meant different instants on different
 *     servers, and — because daylight saving moves — on different dates on the same server.</li>
 * </ol>
 *
 * <p><b>Items 2 to 4 were the dangerous ones.</b> Item 1 failed closed; those three produced a
 * plausible wrong answer and told nobody.
 *
 * <h2>What is accepted now</h2>
 *
 * <p>A date, optionally a time, optionally a zone offset:
 *
 * <pre>
 *   1990-01-01                    midnight, server zone
 *   1990-01-01T09:30              seconds optional
 *   1990-01-01T09:30:00
 *   1990-01-01T09:30:00.500       fractional seconds kept
 *   1990-01-01T09:30:00Z          honoured, not ignored
 *   1990-01-01T09:30:00+05:30     honoured, not ignored
 * </pre>
 *
 * <p>Anything else is REFUSED with a message naming the accepted forms rather than merely echoing
 * the input. That matters more than politeness: an agent given "Unparseable date" retries blind,
 * and a retry loop against a failing tool churns pooled connections.
 *
 * <p><b>An offset is converted, not recorded.</b> An Oracle DATE has no zone to store one in, so
 * {@code 09:30Z} becomes whatever 09:30 UTC is on the server's clock. That is the correct reading of
 * the input and it is a real change from the old behaviour, which kept the wall clock and threw the
 * offset away.
 *
 * <p><b>Where there is no offset the server's own zone is used.</b> That is not ideal — it is what
 * an Oracle DATE can represent, and inventing UTC instead would silently shift every value that
 * works today.
 *
 * <p>This lives in {@code pub} rather than in emitted source for two reasons: it can be unit-tested
 * here and cannot be tested there, and the generated Jackson configuration can reference
 * {@link #ISO_PATTERN} instead of repeating the literal — a library constant is not the illegal
 * forward reference that stopped the emitted constant being shared.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @since 2.0.0
 */
public final class McpDates {

    /**
     * How a DATE is rendered on the way out, and what Jackson is configured with.
     *
     * <p>Deliberately unchanged from what generated servers have always emitted: altering it would
     * change every date field every existing client reads. Fractional seconds are added on the way
     * out only for a {@link java.sql.Timestamp}, which is the type that actually carries them —
     * see {@link #formatTimestamp}.
     */
    public static final String ISO_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

    /** With fractional seconds, for the type that has them. */
    public static final String ISO_PATTERN_MILLIS = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    /**
     * Date, optional time, optional offset.
     *
     * <p>{@code ISO_LOCAL_TIME} brings optional seconds and optional fractional seconds with it, so
     * the four time shapes above need no separate handling. {@code appendOffsetId} accepts both
     * {@code Z} and {@code +05:30}.
     *
     * <p>Built once and shared, which is safe here and was not with {@code SimpleDateFormat}:
     * {@link DateTimeFormatter} is immutable and thread-safe. Do not replace this with a
     * {@code SimpleDateFormat} field — that is the shared-formatter bug this project has already
     * fixed once, in {@code GenericLog}.
     */
    private static final DateTimeFormatter PARSER = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .optionalStart()
            .appendLiteral('T')
            .append(DateTimeFormatter.ISO_LOCAL_TIME)
            .optionalEnd()
            .optionalStart()
            .appendOffsetId()
            .optionalEnd()
            .toFormatter();

    private McpDates() {
    }

    /**
     * Render a date the way every generated server always has.
     *
     * @param theDate the value, or null
     * @return the ISO-8601 text, or null for a null input
     */
    public static String format(java.util.Date theDate) {
        return theDate == null
                ? null
                : new java.text.SimpleDateFormat(ISO_PATTERN).format(theDate);
    }

    /**
     * Render a timestamp, keeping the fractional seconds a {@code DATE} does not have.
     *
     * @param theTimestamp the value, or null
     * @return the ISO-8601 text with milliseconds, or null for a null input
     */
    public static String formatTimestamp(java.sql.Timestamp theTimestamp) {
        return theTimestamp == null
                ? null
                : new java.text.SimpleDateFormat(ISO_PATTERN_MILLIS).format(theTimestamp);
    }

    /**
     * Render whichever of the two this actually is.
     *
     * <p>The generated code reaches every date value through {@code instanceof java.util.Date}, and
     * {@link java.sql.Timestamp} <em>extends</em> {@code java.util.Date} — so without this dispatch
     * a timestamp renders through the DATE pattern and drops the fractional seconds that are the
     * only reason the type differs. Sending it here rather than widening
     * {@link #ISO_PATTERN} keeps a plain DATE rendering byte-for-byte as it always has.
     *
     * @param theDate the value, or null
     * @return the ISO-8601 text, with milliseconds only for a {@code Timestamp}
     */
    public static String formatAny(java.util.Date theDate) {
        if (theDate instanceof java.sql.Timestamp) {
            return formatTimestamp((java.sql.Timestamp) theDate);
        }
        return format(theDate);
    }

    /**
     * ISO text to the form an Oracle format mask matches, for a zoned timestamp crossing MCP.
     *
     * <p><b>Why this exists rather than a reformat.</b> MCP speaks ISO-8601, where a {@code T}
     * separates the date from the time. The generated PL/SQL converts a zoned index-by element with
     * {@code 'yyyy-mm-dd hh24:mi:ss.ff9 TZR'}, which has a SPACE there — measured, an ISO {@code T}
     * against that mask raises ORA-01858. The separator before the ZONE is flexible (with or
     * without a space, offset or region name, all accepted), so this is the only difference that
     * matters.
     *
     * <p><b>It is a TEXTUAL swap, deliberately, and not a parse-and-reformat.</b> Parsing would
     * yield an instant and lose the caller's zone, so rendering it again would substitute the
     * server's — silently turning {@code +05:30} into whatever the server runs on, which is the
     * exact defect this whole area was fixed for.
     *
     * @param theIsoText the caller's value, or null
     * @return the same value with the date/time separator Oracle's mask expects
     */
    public static String toOracleTimestampText(String theIsoText) {
        if (theIsoText == null) {
            return null;
        }

        // Only the FIRST 'T', and only where a date-time separator can be: position 10 in
        // yyyy-mm-dd. Replacing every 'T' would corrupt a region name (Asia/Katmandu has none, but
        // America/Port_of_Spain and US/Eastern do carry letters, and a blanket replace is the kind
        // of shortcut that works on the test value and not on the estate).
        if (theIsoText.length() > 10 && theIsoText.charAt(10) == 'T') {
            return theIsoText.substring(0, 10) + ' ' + theIsoText.substring(11);
        }

        return theIsoText;
    }

    /**
     * The reverse: what Oracle handed back, as the ISO text MCP crosses.
     *
     * @param theOracleText the value from the database, or null
     * @return the same value with an ISO {@code T} separator
     */
    public static String fromOracleTimestampText(String theOracleText) {
        if (theOracleText == null) {
            return null;
        }

        if (theOracleText.length() > 10 && theOracleText.charAt(10) == ' ') {
            return theOracleText.substring(0, 10) + 'T' + theOracleText.substring(11);
        }

        return theOracleText;
    }

    /**
     * The most fractional-second digits the unzoned Oracle TIMESTAMP mask will match.
     *
     * <p>Eight, because {@link com.mcpdbwizard.pub.PlsqlIndexByTable2#ORACLE_TIMESTAMP_TO_CHAR_MASK}
     * ends {@code .ff8}. Measured on 12c: one to eight digits are accepted, nine raises ORA-01830.
     */
    private static final int MAX_UNZONED_FRACTION_DIGITS = 8;

    /**
     * ISO text to the form the Oracle DATE mask matches, for a DATE index-by element crossing MCP.
     *
     * <p>Two differences from {@link #toOracleTimestampText}, both MEASURED against 12c rather than
     * reasoned about — which is the rule this area exists under, every wrong claim in its history
     * having come from reasoning about a format that one {@code TO_DATE} call would have settled:
     *
     * <ul>
     * <li>the {@code T} becomes a space, exactly as it does for a zoned timestamp; and</li>
     * <li><b>any fractional seconds are REMOVED.</b>
     * {@link com.mcpdbwizard.pub.PlsqlIndexByTable2#ORACLE_DATE_TO_CHAR_MASK} is
     * {@code 'yyyy-mm-dd hh24:mi:ss'} with no {@code FF} element, and
     * {@code TO_DATE('2019-03-01 14:25:36.123', ...)} raises <b>ORA-01830</b>.</li>
     * </ul>
     *
     * <p>A bare {@code '2019-03-01'} is accepted by that mask and means midnight, so a date-only
     * value needs no padding and gets none.
     *
     * <p><b>Dropping the fraction loses precision, and that is deliberate.</b> An Oracle DATE has
     * no sub-second component to store it in, so the alternative is not fidelity but ORA-01830 —
     * and the SCALAR date path already drops it silently, by binding a {@link java.util.Date} that
     * Oracle truncates on arrival. Refusing here would make an index-by DATE stricter than a plain
     * DATE parameter beside it, for nothing. The emitted tool description says so out loud, because
     * an accepted spelling that a caller cannot discover is precisely the defect a live MCP session
     * reported against this area once already.
     *
     * <p>Nothing else is touched. A value carrying a zone, or one that is not a timestamp at all,
     * is passed through to be refused by Oracle in Oracle's own words rather than quietly reshaped
     * into something that parses — the same division of labour
     * {@link com.mcpdbwizard.pub.PlsqlIndexByTable2#ensureFractionalSeconds()} keeps.
     *
     * @param theIsoText the caller's value, or null
     * @return the same value in the form the DATE mask accepts
     * @since 2.0.6
     */
    public static String toOracleDateText(String theIsoText) {
        String theText = toOracleTimestampText(theIsoText);

        if (theText == null) {
            return null;
        }

        int theDot = theText.indexOf('.', 10);

        if (theDot < 0) {
            return theText;
        }

        // Whatever follows the digits -- a zone, trailing text -- is KEPT, so that a value this
        // mask cannot take still reaches Oracle recognisably rather than half-repaired.
        return theText.substring(0, theDot) + theText.substring(endOfFraction(theText, theDot));
    }

    /**
     * ISO text to the form the unzoned Oracle TIMESTAMP mask matches.
     *
     * <p>The {@code T} becomes a space, and a fraction longer than
     * {@value #MAX_UNZONED_FRACTION_DIGITS} digits is truncated. Measured on 12c against
     * {@code 'yyyy-mm-dd hh24:mi:ss.ff8'}: a MISSING fraction is accepted, one to eight digits are
     * accepted, and nine raises ORA-01830.
     *
     * <p><b>That first point is why there is no counterpart to
     * {@link com.mcpdbwizard.pub.PlsqlIndexByTable2#ensureFractionalSeconds()} here.</b> That
     * method exists because the ZONED mask stops tolerating a missing fraction once it names a
     * zone; the unzoned mask never stopped, so padding would be work with no effect.
     *
     * <p>Truncating rather than refusing, because a caller sending a ninth digit is already sending
     * more precision than the column will keep: Oracle's TIMESTAMP tops out at 9 and DEFAULTS to 6,
     * and rounds {@code .12345678} to {@code .123457} on arrival whatever we hand it.
     *
     * @param theIsoText the caller's value, or null
     * @return the same value in the form the unzoned TIMESTAMP mask accepts
     * @since 2.0.6
     */
    public static String toOracleUnzonedTimestampText(String theIsoText) {
        String theText = toOracleTimestampText(theIsoText);

        if (theText == null) {
            return null;
        }

        int theDot = theText.indexOf('.', 10);

        if (theDot < 0) {
            return theText;
        }

        int theEnd = endOfFraction(theText, theDot);

        if (theEnd - theDot - 1 <= MAX_UNZONED_FRACTION_DIGITS) {
            return theText;
        }

        return theText.substring(0, theDot + 1 + MAX_UNZONED_FRACTION_DIGITS) + theText.substring(theEnd);
    }

    /**
     * One past the last digit of the fractional-seconds run starting at {@code theDot}.
     *
     * <p>Scans DIGITS only, so anything after them -- a space and a zone, or trailing rubbish -- is
     * found rather than consumed, and the callers above can preserve it.
     *
     * @param theText the value being examined
     * @param theDot  the index of the decimal point
     * @return the index one past the fraction, which is {@code theDot + 1} when no digit follows
     */
    private static int endOfFraction(String theText, int theDot) {
        int theEnd = theDot + 1;

        while (theEnd < theText.length()
                && theText.charAt(theEnd) >= '0' && theText.charAt(theEnd) <= '9') {
            theEnd++;
        }

        return theEnd;
    }

    /**
     * Read one of the accepted forms.
     *
     * @param theValue the caller's value; {@code toString} is used, so a JSON string arrives here
     *                 as itself
     * @return the parsed value, or null for a null input
     * @throws IllegalArgumentException naming the accepted forms, when the text is not one of them
     */
    public static java.util.Date parse(Object theValue) {
        if (theValue == null) {
            return null;
        }

        String theText = String.valueOf(theValue).trim();
        if (theText.length() == 0) {
            return null;
        }

        TemporalAccessor theParsed;
        try {
            // parse(CharSequence) requires the WHOLE text to be consumed, which is the property
            // that closes the silently-ignored-trailing-zone hole. parse(String, ParsePosition)
            // would reopen it.
            theParsed = PARSER.parse(theText);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(explain(theText), e);
        }

        try {
            java.time.ZoneOffset theOffset = theParsed.query(TemporalQueries.offset());
            LocalDate theDate = theParsed.query(TemporalQueries.localDate());
            java.time.LocalTime theTime = theParsed.query(TemporalQueries.localTime());

            if (theDate == null) {
                throw new IllegalArgumentException(explain(theText));
            }

            // No time at all means midnight -- the bare-date case, which used to be refused.
            LocalDateTime theMoment = (theTime == null)
                    ? theDate.atStartOfDay()
                    : LocalDateTime.of(theDate, theTime);

            // An offset is CONVERTED to the server's clock, because an Oracle DATE has nowhere to
            // keep one. Absent, the server's own zone is assumed.
            Instant theInstant = (theOffset != null)
                    ? theMoment.toInstant(theOffset)
                    : theMoment.atZone(ZoneId.systemDefault()).toInstant();

            return new java.util.Date(theInstant.toEpochMilli());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(explain(theText), e);
        }
    }

    /**
     * The same as {@link #parse}, as a {@link java.sql.Timestamp}.
     *
     * @param theValue the caller's value, or null
     * @return the parsed value, or null for a null input
     */
    public static java.sql.Timestamp parseTimestamp(Object theValue) {
        java.util.Date theDate = parse(theValue);
        return theDate == null ? null : new java.sql.Timestamp(theDate.getTime());
    }

    /**
     * Read a {@link java.sql.Timestamp} from either the JDBC escape form
     * {@code "yyyy-mm-dd hh:mm:ss[.f...]"} or one of the ISO forms {@link #parse} accepts.
     *
     * <p>This exists because a generated table's TIMESTAMP column crosses MCP as the FIRST of
     * those and a PL/SQL TIMESTAMP parameter crosses as the second, and one surface -- a table's
     * index-lookup tool -- has to take a value the caller most likely read back off the other
     * table tools. A generated row exposes such a column through {@code set<Col>(String)} /
     * {@code get<Col>String()}, both of which speak {@code java.sql.Timestamp}'s own
     * {@code toString}/{@code valueOf} form, so that is what a {@code get_by_pk} result carries
     * and what an {@code insert} accepts. Refusing it in the lookup tool alone would mean a value
     * this server had just emitted was not a value it would take back.
     *
     * <p>The JDBC form is read with {@link java.sql.Timestamp#valueOf}, not through {@link #parse}:
     * {@code parse} returns a {@link java.util.Date} and would truncate to milliseconds, while an
     * Oracle {@code TIMESTAMP(6)} carries microseconds and {@code valueOf} keeps all nine digits
     * of the nanosecond field.
     *
     * @param theValue the caller's value, or null
     * @return the parsed value, or null for a null (or empty) input
     * @throws IllegalArgumentException naming the accepted ISO forms, when the text is neither
     */
    public static java.sql.Timestamp parseSqlTimestamp(Object theValue) {
        if (theValue == null) {
            return null;
        }
        String theText = String.valueOf(theValue).trim();
        if (theText.length() == 0) {
            return null;
        }
        // A space and no 'T' is the JDBC escape form; anything else (a bare ISO date included)
        // goes to the ISO reader, whose message names the forms it accepts.
        if (theText.indexOf(' ') > 0 && theText.indexOf('T') < 0) {
            try {
                return java.sql.Timestamp.valueOf(theText);
            } catch (IllegalArgumentException e) {
                // Not the JDBC form after all -- fall through rather than report a message that
                // names only that one.
            }
        }

        // The ISO reading, and NOT via parseTimestamp: that one goes through java.util.Date and
        // truncates to milliseconds. Harmless while the generated DAO threw the fraction away
        // anyway; not harmless now that a TIMESTAMP binds at full precision, because an equality
        // lookup written in ISO would silently miss the row an identical lookup written in the
        // JDBC form finds. Measured: "...T06:54:23.710755" found 0 rows where
        // "... 06:54:23.710755" found 1.
        java.util.Date theDate = parse(theValue);
        if (theDate == null) {
            return null;
        }
        java.sql.Timestamp theStamp = new java.sql.Timestamp(theDate.getTime());
        int theNanos = isoNanoOfSecond(theText);
        if (theNanos >= 0) {
            theStamp.setNanos(theNanos);
        }
        return theStamp;
    }

    /**
     * The nanosecond-of-second digits of an ISO text, or -1 when it carries none.
     *
     * <p>Read off the TEXT rather than from {@link #parse}'s result, because that result is a
     * {@link java.util.Date} and the digits are already gone by then. {@link #parse} has by this
     * point accepted the whole string, so the fraction here is known to be well formed -- this only
     * has to find it and pad it to nine digits.
     */
    private static int isoNanoOfSecond(String theText) {
        int theDot = theText.indexOf('.');
        if (theDot < 0) {
            return -1;
        }
        int theEnd = theDot + 1;
        while (theEnd < theText.length()
                && theText.charAt(theEnd) >= '0' && theText.charAt(theEnd) <= '9') {
            theEnd++;
        }
        String theDigits = theText.substring(theDot + 1, theEnd);
        if (theDigits.length() == 0) {
            return -1;
        }
        if (theDigits.length() > 9) {
            theDigits = theDigits.substring(0, 9);
        }
        while (theDigits.length() < 9) {
            theDigits = theDigits + "0";
        }
        return Integer.parseInt(theDigits);
    }

    /**
     * Say what was expected, not merely what arrived.
     *
     * <p>The old message was {@code Unparseable date: "1990-01-01"}, which names the input and
     * leaves the caller to guess the rule. A model given that retries with another guess.
     */
    private static String explain(String theText) {
        return "Cannot read \"" + theText + "\" as a date. Expected an ISO-8601 date, optionally"
                + " with a time and a zone offset: 1990-01-01, 1990-01-01T09:30:00,"
                + " 1990-01-01T09:30:00.500, 1990-01-01T09:30:00Z or 1990-01-01T09:30:00+05:30.";
    }
}
