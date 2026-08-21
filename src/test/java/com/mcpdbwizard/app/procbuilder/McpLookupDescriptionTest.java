package com.mcpdbwizard.app.procbuilder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A foreign-key child lookup must name the table the rows come FROM.
 *
 * <p>It used to name only the table being looked up BY — the parent — so
 * {@code flights_bkg_flt} described itself as "the child rows referencing table FLIGHTS" and never
 * mentioned that what comes back is BOOKINGS rows. A caller was told what it was searching with and
 * never what it would get.
 *
 * <p><b>Why this is a defect and not a wording preference.</b> On a table with several children the
 * descriptions were identical apart from a constraint name Oracle generated, so nothing
 * distinguished the tools at all — see {@link #severalChildrenAreTellableApart}. A tool count
 * cannot catch that: all the tools exist, and the model simply picks the wrong one.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class McpLookupDescriptionTest {

    @Test
    void aForeignKeyLookupNamesTheChildTable() {
        String theText = SAAdminWrangler.mcpLookupDescription("fk", "FLIGHTS", "DR.BOOKINGS",
                "BKG_FLT", "airline_name (VARCHAR2), flight_number (NUMBER)", false);

        assertTrue(theText.contains("DR.BOOKINGS"),
                "the rows come from BOOKINGS and it must say so: " + theText);
        assertTrue(theText.contains("FLIGHTS"),
                "the parent is still what you look up by: " + theText);
        assertTrue(theText.contains("BKG_FLT"),
                "the constraint name is the override key and must survive: " + theText);
    }

    /**
     * The case that makes this worth doing. Five foreign keys pointed at GROUPS in one real
     * generated tree, all keyed on the same single column; before this the five descriptions
     * differed only by R_15 / R_40 / R_41 / R_47 / R_62, which carry no meaning to anyone.
     */
    @Test
    void severalChildrenAreTellableApart() {
        String theMembers = SAAdminWrangler.mcpLookupDescription("fk", "GROUPS",
                "DR.GROUP_MEMBERS", "R_15", "group_id (NUMBER)", false);
        String theRoles = SAAdminWrangler.mcpLookupDescription("fk", "GROUPS",
                "DR.GROUP_ROLES", "R_40", "group_id (NUMBER)", false);

        assertNotEquals(theMembers, theRoles);
        assertTrue(theMembers.contains("GROUP_MEMBERS"), theMembers);
        assertTrue(theRoles.contains("GROUP_ROLES"), theRoles);

        // Strip the constraint names and they must STILL differ -- that is the whole point. If the
        // only difference were R_15 vs R_40 this test would pass while the defect remained.
        assertNotEquals(theMembers.replace("R_15", "X"), theRoles.replace("R_40", "X"));
    }

    /**
     * A missing child name must not render as the word "null". The name is carried from the FK
     * walk, and a future change to that walk could stop carrying it; this fails safe to the old
     * wording rather than emitting a description with a hole in it.
     */
    @Test
    void anAbsentChildNameFallsBackRatherThanPrintingNull() {
        String theText = SAAdminWrangler.mcpLookupDescription("fk", "FLIGHTS", null,
                "BKG_FLT", "flight_number (NUMBER)", false);

        assertTrue(theText.indexOf("null") < 0, theText);
        assertTrue(theText.contains("FLIGHTS"), theText);
        assertTrue(theText.contains("BKG_FLT"), theText);

        assertEquals(theText, SAAdminWrangler.mcpLookupDescription("fk", "FLIGHTS", "  ",
                "BKG_FLT", "flight_number (NUMBER)", false), "blank must behave as absent");
    }

    // ---- the two kinds that are NOT changing, pinned so the FK edit cannot disturb them ----

    @Test
    void aUniqueKeyLookupIsUnchanged() {
        assertEquals("Look up the row in table GROUPS by unique key GRPS_FK01"
                        + " (group_name (VARCHAR2), group_type_name (VARCHAR2))."
                        + " Returns the matching row as a JSON object, or {\\\"found\\\":false}.",
                SAAdminWrangler.mcpLookupDescription("uk", "GROUPS", null, "GRPS_FK01",
                        "group_name (VARCHAR2), group_type_name (VARCHAR2)", true));
    }

    @Test
    void anIndexLookupIsUnchanged() {
        assertEquals("Look up rows in table FLIGHTS by index FLT_IX3"
                        + " (airline_name (VARCHAR2), flight_number (NUMBER))."
                        + " Returns the matching rows as a JSON array.",
                SAAdminWrangler.mcpLookupDescription("ix", "FLIGHTS", null, "FLT_IX3",
                        "airline_name (VARCHAR2), flight_number (NUMBER)", false));
    }

    /**
     * The escaped quotes are emitted INTO a Java string literal, so they must arrive here as
     * backslash-quote and not as a bare quote -- a bare one does not fail politely, it fails as a
     * compile error across the whole generated tree.
     */
    @Test
    void theSingleRowMarkerKeepsItsSourceLevelEscaping() {
        String theText = SAAdminWrangler.mcpLookupDescription("uk", "T", null, "U1", "c (NUMBER)", true);
        assertTrue(theText.contains("{\\\"found\\\":false}"), theText);
    }

    /** No double quotes: {@code .description(...)} does not escape what it is handed. */
    @Test
    void noDescriptionCarriesABareDoubleQuote() {
        String[] theTexts = {
            SAAdminWrangler.mcpLookupDescription("fk", "FLIGHTS", "DR.BOOKINGS", "BKG_FLT", "c (NUMBER)", false),
            SAAdminWrangler.mcpLookupDescription("uk", "GROUPS", null, "GRPS_FK01", "c (NUMBER)", true),
            SAAdminWrangler.mcpLookupDescription("ix", "FLIGHTS", null, "FLT_IX3", "c (NUMBER)", false)
        };
        for (String theText : theTexts) {
            for (int seq = 0; seq < theText.length(); seq++) {
                if (theText.charAt(seq) == '"') {
                    assertTrue(seq > 0 && theText.charAt(seq - 1) == '\\',
                            "unescaped quote at " + seq + " in: " + theText);
                }
            }
        }
    }
}
