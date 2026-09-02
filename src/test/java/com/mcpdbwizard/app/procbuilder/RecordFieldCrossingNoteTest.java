package com.mcpdbwizard.app.procbuilder;

import com.mcpdbwizard.pub.McpDates;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A RAW or DATE field inside a record has to say how it crosses.
 *
 * <p>Both used to be published as a bare <code>{"type":"string"}</code> — true, and useless. It does
 * not say "send base64", and it does not say which date spellings are taken. A caller could only
 * find out by being rejected.
 *
 * <p><b>The DATE note has since been rewritten a second time, and the reason is worth knowing
 * before editing it.</b> Its first version warned that the {@code T} form was REQUIRED inside a
 * record, unlike a scalar parameter — accurate, because a record's fields went through a
 * {@code SimpleDateFormat} on one pattern rather than through {@code McpDates}. 2.0.17 made the two
 * paths one ({@code McpDateModule}), so that warning became a restriction the code no longer
 * enforces, and {@code theDateNoteNoLongerCLAIMSAnAsymmetryThatIsFIXED} below now asserts those
 * words are ABSENT. A schema naming a rule that is not enforced is worse than one that says
 * nothing: it is believed, and the caller never tries the form that works.
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
    void aDateFieldOffersEverySpellingTheParserActuallyTAKES() {
        String theNote = SAAdminWrangler.recordFieldCrossingNote(SAAdminWrangler.RECORD_FIELD_DATE);

        // Each of these is asserted because each is a form McpDates accepts and a caller would
        // otherwise have no way to discover. The bare date first: it is the one a model reaches for
        // when a column is called BIRTHDATE, and it is the one this note used to say was refused.
        assertTrue(theNote.contains("1980-01-01,"), theNote);
        assertTrue(theNote.contains("1980-01-01T09:30:00"), theNote);
        assertTrue(theNote.contains("1980-01-01T09:30:00.500"), theNote);
        assertTrue(theNote.contains("1980-01-01T09:30:00+05:30"), theNote);

        // And these are what McpDates does with the two that need explaining rather than showing.
        assertTrue(theNote.contains("converted"), theNote);
        assertTrue(theNote.contains("epoch"), theNote);
    }

    @Test
    void theDateNoteNoLongerCLAIMSAnAsymmetryThatIsFIXED() {
        String theNote = SAAdminWrangler.recordFieldCrossingNote(SAAdminWrangler.RECORD_FIELD_DATE);

        // This test is the reverse of the one it replaces, and that is the point of keeping it
        // rather than deleting it. It used to assert the note said the T form was REQUIRED here and
        // not for a scalar parameter -- true until McpDateModule made the two paths one.
        //
        // A schema naming a restriction that is NOT enforced is worse than one that says nothing:
        // the caller believes it and never tries the form that works. So the words that described
        // the old defect must not survive the fix.
        assertFalse(theNote.contains("REQUIRED"), theNote);
        assertFalse(theNote.contains("scalar"), theNote);
        assertFalse(theNote.contains("unlike"), theNote);

        // The pattern goes too. McpDates.ISO_PATTERN is still what a date is RENDERED with, but it
        // has never been the set that is ACCEPTED, and printing it here said otherwise.
        assertFalse(theNote.contains(McpDates.ISO_PATTERN), theNote);
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
