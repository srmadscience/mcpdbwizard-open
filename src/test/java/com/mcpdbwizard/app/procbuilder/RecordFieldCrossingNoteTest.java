package com.mcpdbwizard.app.procbuilder;

import com.mcpdbwizard.pub.McpDates;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A RAW or DATE field inside a record has to say how it crosses.
 *
 * <p>Both used to be published as a bare <code>{"type":"string"}</code> — true, and useless. It does
 * not say "send base64", and it does not say the {@code T} is required. A caller could only find out
 * by being rejected, and for the DATE that rejection is the documented known issue: a scalar date
 * parameter accepts {@code 1980-01-01} while the same Oracle type inside a record does not, because
 * a record's fields go through {@code RECORD_MAPPER}, pinned to one pattern, rather than the lenient
 * scalar parser.
 *
 * <p>The RAW case is worse than an error, which is why it earns its own assertion below: hex and
 * base64 are both plain strings, so a caller who sends hex is not rejected — the bytes are simply
 * wrong, and the procedure runs.
 *
 * <p>Tested here rather than through a generated tree because the emitted method dispatches on
 * {@code Class} at run time. The emitter interpolates these very strings, so asserting them is
 * asserting what a client will read.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class RecordFieldCrossingNoteTest {

    @Test
    void aRawFieldSaysBase64AndSaysNotHex() {
        String theNote = SAAdminWrangler.recordFieldCrossingNote(SAAdminWrangler.RECORD_FIELD_RAW);

        assertTrue(theNote.contains("base64"), theNote);
        // "NOT hex" is the load-bearing half. Hex is the spelling a human reaches for with RAW, and
        // it is accepted as a string and decoded into the wrong bytes -- a silent wrong answer,
        // where a rejection would at least be a question.
        assertTrue(theNote.contains("NOT hex"), theNote);
        assertTrue(theNote.contains("3q2+7w=="), theNote);
    }

    @Test
    void aDateFieldNamesThePatternTheMapperIsActuallyPinnedTo() {
        String theNote = SAAdminWrangler.recordFieldCrossingNote(SAAdminWrangler.RECORD_FIELD_DATE);

        // Derived from the constant, not retyped. A description that drifts from the format the
        // mapper enforces is worse than no description, because it gets believed.
        assertTrue(theNote.contains(McpDates.ISO_PATTERN), theNote);
        assertEquals("yyyy-MM-dd'T'HH:mm:ss", McpDates.ISO_PATTERN,
                "if this pattern moves, the note moves with it -- and this line is the reminder"
                        + " that the two are the same fact");
    }

    @Test
    void theDateNoteSaysTheTFormIsRequiredHereAndNotEverywhere() {
        String theNote = SAAdminWrangler.recordFieldCrossingNote(SAAdminWrangler.RECORD_FIELD_DATE);

        // The asymmetry IS the defect. Saying "ISO-8601" alone would not help: 1980-01-01 is ISO
        // 8601 and is accepted as a scalar parameter, so a caller has every reason to expect it
        // here too.
        assertTrue(theNote.contains("REQUIRED"), theNote);
        assertTrue(theNote.contains("1980-01-01"), theNote);
        assertTrue(theNote.contains("scalar"), theNote);
    }

    @Test
    void anythingElseGetsNoNote() {
        // A NUMBER or VARCHAR2 field crosses as the JSON type its schema already names. A note there
        // would be noise, and noise is what stops the two above being read.
        assertNull(SAAdminWrangler.recordFieldCrossingNote("number"));
        assertNull(SAAdminWrangler.recordFieldCrossingNote(null));
        assertNull(SAAdminWrangler.recordFieldCrossingNote(""));
    }
}
