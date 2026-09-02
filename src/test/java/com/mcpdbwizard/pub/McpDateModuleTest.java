package com.mcpdbwizard.pub;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A DATE inside a record must cross exactly as a DATE parameter of its own does.
 *
 * <p><b>Tested through a mapper, not through {@link McpDateModule} directly</b>, and configured the
 * way {@code SAAdminWrangler} configures {@code RECORD_MAPPER} — field visibility ANY, getters
 * NONE, {@code FAIL_ON_UNKNOWN_PROPERTIES} enabled, the date pattern still set. The defect this
 * covers was never in {@link McpDates}, which was already right; it was in what Jackson did instead
 * of calling it. A test that exercised the module in isolation would have passed before the fix.
 *
 * <p><b>Every case asserts the record answer EQUALS the scalar answer</b> rather than asserting a
 * literal. The requirement is parity — one Oracle type, one behaviour — so pinning two independent
 * literals would let both drift together and still pass.
 *
 * @author devteam@mcpdbwizard.com
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 * @version 1
 */
class McpDateModuleTest {

    /** Shaped like a generated record class: public fields, no setters, no annotations. */
    public static class Rec {
        public java.util.Date paramBirthdate;
        public java.sql.Timestamp paramWhen;
    }

    /** The emitted mapper, with the module. Kept in step with SAAdminWrangler by hand. */
    private static final JsonMapper RECORD_MAPPER = JsonMapper.builder()
            .changeDefaultVisibility(vc -> vc
                    .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                    .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withCreatorVisibility(JsonAutoDetect.Visibility.NONE))
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .defaultDateFormat(new SimpleDateFormat(McpDates.ISO_PATTERN))
            .defaultTimeZone(TimeZone.getDefault())
            .addModule(new McpDateModule())
            .build();

    private static java.util.Date inRecord(String theIsoText) {
        return RECORD_MAPPER.readValue("{\"paramBirthdate\":\"" + theIsoText + "\"}", Rec.class)
                .paramBirthdate;
    }

    /** Both paths, one assertion: the record must answer what the scalar parameter answers. */
    private static void assertCrossesLikeAScalar(String theIsoText) {
        assertEquals(McpDates.parse(theIsoText), inRecord(theIsoText),
                "a record field and a scalar parameter must read \"" + theIsoText + "\" the same way");
    }

    // ---- the four defects, one test each ------------------------------------

    @Test
    void aBareDateIsAcceptedInsideARecord() {
        // The only one of the four that ever failed LOUDLY, and so the only one reported.
        assertCrossesLikeAScalar("1980-01-01");
    }

    @Test
    void aZoneOffsetIsHonouredAndNotDropped() {
        // The worst of them. Before this, a record read +05:30 as local time -- five and a half
        // hours out, no error, on data an agent reads as a departure time.
        //
        // NOTE the value: an OFFSET, never a Z. In a zero-offset zone a Z case agrees whether or
        // not the offset is honoured, so it can pass while this whole fix is absent.
        assertCrossesLikeAScalar("1980-01-01T09:30:00+05:30");

        // ... and the proof that the case above can actually tell the difference: the same wall
        // clock without the offset must NOT be the same instant.
        assertNotEquals(McpDates.parse("1980-01-01T09:30:00"),
                inRecord("1980-01-01T09:30:00+05:30"),
                "if these are equal the offset is being ignored, whatever the case above says");
    }

    @Test
    void fractionalSecondsSurvive() {
        assertCrossesLikeAScalar("1980-01-01T09:30:00.500");
        assertEquals(500, inRecord("1980-01-01T09:30:00.500").getTime() % 1000);
    }

    @Test
    void anImpossibleDateIsRefusedRatherThanRolled() {
        // SimpleDateFormat is lenient: this used to arrive as 2004-02-14. Confident nonsense.
        assertThrows(Exception.class, () -> inRecord("2003-13-45T00:00:00"));
    }

    // ---- the rest of the accepted set --------------------------------------

    @Test
    void everyAcceptedScalarSpellingIsAcceptedInARecordToo() {
        for (String theText : new String[]{"1980-01-01", "1980-01-01T09:30", "1980-01-01T09:30:00",
                "1980-01-01T09:30:00Z", "1980-01-01T09:30:00+05:30", "1980-01-01T09:30:00.500"}) {
            assertCrossesLikeAScalar(theText);
        }
    }

    @Test
    void whatTheScalarPathRefusesTheRecordPathRefuses() {
        assertThrows(Exception.class, () -> inRecord("rubbish"));
        assertThrows(Exception.class, () -> inRecord("1980-13-01"));
        assertThrows(Exception.class, () -> inRecord("01/01/1980"));
    }

    @Test
    void theRefusalSaysWhatWasExpected() {
        Exception theException = assertThrows(Exception.class, () -> inRecord("01/01/1980"));

        // A model given "Unparseable date" retries blind, and a retry loop against a failing tool
        // churns pooled connections -- which this product has already been taken down by once.
        assertTrue(theException.getMessage().contains("Expected an ISO-8601 date"),
                theException.getMessage());
        assertTrue(theException.getMessage().contains("paramBirthdate"),
                "Jackson names the field it was reading, which is worth keeping: "
                        + theException.getMessage());
    }

    // ---- forms that already worked, and must go on working -----------------

    @Test
    void anEpochNumberStillBinds() {
        // Deliberately NOT parity: the scalar path refuses this, a record accepts it, and it
        // accepted it before this change. Withdrawing a spelling that works is the same class of
        // failure as the four above. 315532800000 is 1980-01-01T00:00:00Z.
        Rec theRecord = RECORD_MAPPER.readValue("{\"paramBirthdate\":315532800000}", Rec.class);

        assertEquals(315532800000L, theRecord.paramBirthdate.getTime());
    }

    @Test
    void nullAndEmptyStillBindNull() {
        assertNull(RECORD_MAPPER.readValue("{\"paramBirthdate\":null}", Rec.class).paramBirthdate);
        assertNull(RECORD_MAPPER.readValue("{\"paramBirthdate\":\"\"}", Rec.class).paramBirthdate);
    }

    // ---- outbound ----------------------------------------------------------

    @Test
    void aTimestampFieldKeepsItsFractionalSecondsOnTheWayOut() {
        Rec theRecord = new Rec();
        theRecord.paramWhen = new java.sql.Timestamp(0L);
        theRecord.paramWhen.setNanos(500000000);
        theRecord.paramBirthdate = new java.util.Date(0L);

        String theJson = RECORD_MAPPER.writeValueAsString(theRecord);

        // The scalar path has rendered a Timestamp this way since 2.0.0, via formatAny. A record
        // field went through the DATE pattern instead and dropped the only thing the type is for.
        assertTrue(theJson.contains("\"paramWhen\":\"" + McpDates.formatAny(theRecord.paramWhen) + "\""),
                theJson);
        assertTrue(theJson.contains(".500"), theJson);
    }

    @Test
    void aPlainDateFieldRendersExactlyAsItAlwaysHas() {
        Rec theRecord = new Rec();
        theRecord.paramBirthdate = new java.util.Date(0L);

        // The half that must NOT change. Every existing client reads this field.
        assertTrue(RECORD_MAPPER.writeValueAsString(theRecord)
                        .contains("\"paramBirthdate\":\"" + McpDates.format(theRecord.paramBirthdate) + "\""),
                RECORD_MAPPER.writeValueAsString(theRecord));
    }
}
