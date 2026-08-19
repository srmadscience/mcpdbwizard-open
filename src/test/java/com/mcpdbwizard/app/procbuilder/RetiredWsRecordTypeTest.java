package com.mcpdbwizard.app.procbuilder;

import com.mcpdbwizard.app.procbuilder.gui.ApplicationShell;
import com.mcpdbwizard.schema.Schema;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code WS_RECORD_TYPE} is retired, and these pin the two halves of that: it no longer has any
 * effect, and a config carrying it still round-trips.
 *
 * <p>The option chose the visibility of a generated record class's own attributes. Its non-default
 * emitted <b>protected</b> fields — the label said "private", which was never accurate — producing
 * a shape only 2 of the 41 propfiles ever generated. That rarely-taken path is where <i>bug F</i>
 * hid: the MCP emitter assigned such a field directly, which compiled for 39 configs and failed
 * for two with "paramData has protected access". The encapsulation it bought on generated DTO
 * classes was not worth a code path most runs never exercise.
 *
 * <p>Note MCP itself was never at risk: the emitted {@code RECORD_MAPPER} uses
 * {@code Visibility.ANY}, so Jackson saw protected fields either way. The hazard was the emitter,
 * not the crossing.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class RetiredWsRecordTypeTest {

    /** The value everything must now resolve to. */
    private static final String PUBLIC_FORM = ApplicationShell.WS_REC_TYPES[0];

    @Test
    void theTwoChoicesAreStillDeclaredSoALegacyValueCanBeRecognised() {
        // Deleting the constants would be a step too far: a legacy config may hold either string,
        // and the normalisation sites compare against them.
        assertEquals(2, ApplicationShell.WS_REC_TYPES.length);
        assertTrue(PUBLIC_FORM.startsWith("public"), PUBLIC_FORM);
    }

    /**
     * The key must survive both config formats. Retiring an option is not licence to lose it: a
     * customer's {@code .pb2} converted to {@code .json} and back must still compare equal, or the
     * converter stops being lossless for every config written before the change.
     */
    @Test
    void aLegacyValueStillRoundTripsThroughBothFormats() {
        Properties theProperties = new Properties();
        theProperties.setProperty("WS_RECORD_TYPE", "private, set & get methods");

        Schema theSchema = new Schema(theProperties);
        assertEquals("private, set & get methods", theSchema.getWsRecordType(),
                "the scalar must still carry it, or .pb2 <-> .json stops being lossless");
        assertEquals("private, set & get methods",
                theSchema.toPb2().getProperty("WS_RECORD_TYPE"));
        assertEquals("private, set & get methods",
                new Schema(theSchema.toJson()).getWsRecordType(), "lost crossing JSON");
    }

    /**
     * The load-bearing assertion. Both GUIs dropped their control and normalise on save, but
     * neither of those makes the setting unreachable — a hand-edited config, or (on the web) a
     * value already in the session schema, would sail past them. Ignoring the key at the single
     * point it is READ is what retires it, so that is what this checks.
     */
    @Test
    void theGeneratorNoLongerReadsTheKey() throws Exception {
        String theSource = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/mcpdbwizard/app/procbuilder/gui/ApplicationShell.java"));

        int theRead = theSource.indexOf("getProperty(\"WS_RECORD_TYPE\"");
        assertEquals(-1, theRead,
                "ApplicationShell reads WS_RECORD_TYPE again at offset " + theRead
                        + " -- the option is live once more, and the protected-field path with it");

        // ...and the field it used to fill is pinned to the public form.
        assertTrue(theSource.contains("wsRecTypeComboBox = WS_REC_TYPES[0]"),
                "the record-type field must be pinned to the public form");
    }

    /**
     * Guards the direction of the pin. Asserting only "does not read the key" would still pass if
     * someone pinned it to the PROTECTED form, which is the exact shape being retired.
     */
    @Test
    void theSurvivingFormIsThePublicOne() {
        assertNotNull(PUBLIC_FORM);
        assertTrue(PUBLIC_FORM.contains("no access methods"),
                "the surviving form must be the public-field one, not the protected variant: "
                        + PUBLIC_FORM);
        assertTrue(ApplicationShell.WS_REC_TYPES[1].startsWith("private"),
                "the retired variant is the one whose LABEL says private while it emitted protected");
    }
}
