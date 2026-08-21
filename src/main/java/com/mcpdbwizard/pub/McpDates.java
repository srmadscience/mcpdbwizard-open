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
