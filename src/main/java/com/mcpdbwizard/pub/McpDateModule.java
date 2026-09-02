package com.mcpdbwizard.pub;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

/**
 * {@link McpDates} as a Jackson module, so a date crosses the same way inside a RECORD as it does
 * as a parameter of its own.
 *
 * <h2>What this fixes</h2>
 *
 * <p>The generated server binds a PL/SQL record parameter through Jackson, and configured it with
 * {@code .defaultDateFormat(new SimpleDateFormat(McpDates.ISO_PATTERN))}. Handing Jackson the
 * PATTERN gets none of the parsing {@link McpDates} wraps around it, and a
 * {@link java.text.SimpleDateFormat} on that pattern has two habits that are exactly the defects
 * {@code McpDates} exists to prevent: {@code parse(String)} stops at the end of the pattern and
 * IGNORES what follows, and it is LENIENT. Measured, before this module, against the mapper the
 * emitter actually builds:
 *
 * <pre>
 *   input                        in a record          as a scalar parameter
 *   1980-01-01                   REFUSED              1980-01-01 00:00:00
 *   1980-01-01T09:30             REFUSED              1980-01-01 09:30:00
 *   1980-01-01T09:30:00+05:30    09:30:00 +0000       04:00:00 +0000    offset DROPPED
 *   1980-01-01T09:30:00.500      09:30:00.000         09:30:00.500      fraction DROPPED
 *   2003-13-45T00:00:00          2004-02-14           REFUSED           rolled, silently
 * </pre>
 *
 * <p><b>Only the first of those five is visible.</b> The bare date fails closed, which is why it is
 * the one that got reported; the three below it produce a plausible wrong answer and tell nobody. A
 * departure time sent as {@code +05:30} was stored five and a half hours out.
 *
 * <p>Outbound had the same shape of loss: a {@link java.sql.Timestamp} FIELD rendered through the
 * DATE pattern and dropped its fractional seconds, because {@code Timestamp extends Date} and only
 * {@link McpDates#formatAny} dispatches on that.
 *
 * <h2>Why a module rather than a format</h2>
 *
 * <p>A {@code DateFormat} can only describe one spelling. The accepted set is a date, optionally a
 * time, optionally an offset — which is a parser, not a pattern, and {@link McpDates} already is
 * that parser. Registering it here means the two paths cannot drift, because there is only one.
 *
 * <h2>Why this is a separate class from {@link McpDates}</h2>
 *
 * <p>So that {@code McpDates} stays free of Jackson types. It is reached by every generated DAO,
 * including those in applications that never touch MCP and have no Jackson on the classpath; this
 * class is loaded only by a generated MCP server, which always does.
 *
 * @author devteam@mcpdbwizard.com
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 * @version 1
 * @since 2.0.17
 */
public final class McpDateModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    /**
     * Register {@link McpDates} for both date types a generated class can declare.
     *
     * <p><b>Those two are the whole surface.</b> The generator maps an Oracle date column or
     * attribute to {@code java.util.Date} or {@code java.sql.Timestamp} and to nothing else — there
     * is no {@code java.sql.Date} and no {@code java.time} field to cover. Registering a type that
     * cannot occur would be dead code that looks like caution.
     *
     * <p>Jackson matches a deserializer on the DECLARED field type, so {@code Timestamp} needs its
     * own entry even though it extends {@code Date}: without one it would fall back to Jackson's
     * default and lose the fractional seconds that are the only reason the type differs.
     */
    public McpDateModule() {
        super("McpDates");

        addDeserializer(java.util.Date.class, new DateDeserializer());
        addDeserializer(java.sql.Timestamp.class, new TimestampDeserializer());
        addSerializer(java.util.Date.class, new DateSerializer());
        addSerializer(java.sql.Timestamp.class, new TimestampSerializer());
    }

    /**
     * An epoch millisecond count, where the caller sent a JSON number rather than a string.
     *
     * <p><b>Kept deliberately, though the scalar path refuses it.</b> A number binds a date in a
     * record today — measured: {@code {"paramBirthdate":315532800000}} arrives as 1980-01-01 — and
     * this change is about removing surprises, not adding one. Silently withdrawing a spelling that
     * works is the same class of failure as the four this module fixes.
     *
     * <p>It is the one place the two paths still differ, and it differs in the caller's favour.
     *
     * @param theParser positioned on the value
     * @return the value, or null when the token is not a number
     */
    private static java.util.Date epochOrNull(JsonParser theParser) {
        if (theParser.currentToken() == JsonToken.VALUE_NUMBER_INT) {
            return new java.util.Date(theParser.getLongValue());
        }

        return null;
    }

    /** Text through {@link McpDates#parse}; a JSON number as epoch milliseconds. */
    private static final class DateDeserializer extends ValueDeserializer<java.util.Date> {

        @Override
        public java.util.Date deserialize(JsonParser theParser, DeserializationContext theContext) {
            java.util.Date theEpoch = epochOrNull(theParser);

            return theEpoch != null ? theEpoch : McpDates.parse(theParser.getString());
        }
    }

    /** The same, as the type that carries fractional seconds. */
    private static final class TimestampDeserializer extends ValueDeserializer<java.sql.Timestamp> {

        @Override
        public java.sql.Timestamp deserialize(JsonParser theParser, DeserializationContext theContext) {
            java.util.Date theEpoch = epochOrNull(theParser);

            return theEpoch != null
                    ? new java.sql.Timestamp(theEpoch.getTime())
                    : McpDates.parseTimestamp(theParser.getString());
        }
    }

    /**
     * Out through {@link McpDates#formatAny}, which is the dispatch that keeps a
     * {@code Timestamp}'s fractional seconds and leaves a plain {@code DATE} rendering
     * byte-for-byte as it always has.
     */
    private static final class DateSerializer extends ValueSerializer<java.util.Date> {

        @Override
        public void serialize(java.util.Date theValue, JsonGenerator theGenerator,
                SerializationContext theContext) {
            theGenerator.writeString(McpDates.formatAny(theValue));
        }
    }

    /** Declared separately so a {@code Timestamp} field matches on its own type. */
    private static final class TimestampSerializer extends ValueSerializer<java.sql.Timestamp> {

        @Override
        public void serialize(java.sql.Timestamp theValue, JsonGenerator theGenerator,
                SerializationContext theContext) {
            theGenerator.writeString(McpDates.formatTimestamp(theValue));
        }
    }
}
