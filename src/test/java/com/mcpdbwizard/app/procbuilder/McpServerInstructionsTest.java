package com.mcpdbwizard.app.procbuilder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The generated server's {@code instructions} string, and above all that the author's own words
 * survive every surface the config exposes.
 *
 * <p><b>Why this class exists.</b> The duality-view clause used to ASSIGN where every other clause
 * appends, so a config that exposed a view silently discarded whatever the author had written on
 * Design → Service Options. It was the worst shape a data-loss bug can take: the console accepted
 * the text, stored it in the config, and read it back onto the tab it was typed into, so every
 * surface a person could see agreed it had been saved. The only place it was missing was the
 * server's own {@code initialize} response, which nobody reads directly.
 *
 * <p><b>It survived because the feature was built on configs without a view.</b> A test that
 * exercises one surface at a time cannot find it — every single-surface case passes either way
 * except the view. So the assertions below deliberately walk EACH surface with author text present,
 * rather than testing the composition once with everything switched on.
 *
 * <p>Tested against the extracted helper rather than a generated tree because the emitter
 * interpolates this string verbatim: what {@link SAAdminWrangler#mcpServerInstructions} returns is
 * what a model reads, and asserting it needs no Oracle instance.
 *
 * @author devteam@mcpdbwizard.com
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 * @version 1
 */
class McpServerInstructionsTest {

    private static final String AUTHOR = "Bookings for the Dublin office. Never cancel anything.";

    /** No surface exposed at all, so every flag below can be turned on one at a time. */
    private static String instructionsWith(String theAuthor, boolean theHaveViews, boolean theHaveTables,
            boolean theHaveFunctions, boolean theHaveSql, boolean theHaveSequences) {
        return SAAdminWrangler.mcpServerInstructions(theAuthor,
                theHaveViews, "OB_ORDERS_DV",
                theHaveTables, "BOOKINGS",
                theHaveFunctions, "OB_PKG.GREET",
                theHaveSql, "recent_orders.sql",
                theHaveSequences, "OB_SEQ");
    }

    // ---- the regression ----------------------------------------------------

    @Test
    void aDualityViewDoesNotDiscardTheAuthorsInstructions() {
        String theInstructions = instructionsWith(AUTHOR, true, false, false, false, false);

        // The whole defect in one assertion. Before the fix this returned the view clause alone.
        assertTrue(theInstructions.startsWith(AUTHOR),
                "the author's words must come first, and a model reads top-down: " + theInstructions);
        assertTrue(theInstructions.contains("OB_ORDERS_DV"), theInstructions);
    }

    @Test
    void everySurfaceKeepsTheAuthorsInstructions() {
        // ONE surface at a time, because that is the shape the bug hid in: only the view case
        // failed, so any test that switched several on at once would have passed with it present.
        assertTrue(instructionsWith(AUTHOR, true, false, false, false, false).startsWith(AUTHOR), "view");
        assertTrue(instructionsWith(AUTHOR, false, true, false, false, false).startsWith(AUTHOR), "table");
        assertTrue(instructionsWith(AUTHOR, false, false, true, false, false).startsWith(AUTHOR), "routine");
        assertTrue(instructionsWith(AUTHOR, false, false, false, true, false).startsWith(AUTHOR), "sql");
        assertTrue(instructionsWith(AUTHOR, false, false, false, false, true).startsWith(AUTHOR), "sequence");
        assertTrue(instructionsWith(AUTHOR, true, true, true, true, true).startsWith(AUTHOR), "all five");
    }

    // ---- what must NOT have changed ----------------------------------------

    @Test
    void aConfigWithNoAuthorTextEmitsTheInventoryExactlyAsBefore() {
        // The generated half is unchanged by the fix, and this pins it: a config that never set
        // MCP_INSTRUCTIONS must emit a byte-identical server, or the fix is not a fix.
        String theInstructions = instructionsWith(null, true, true, false, false, false);

        assertEquals("Document CRUD over JSON-relational duality view(s) OB_ORDERS_DV."
                        + " Documents carry a server-maintained _id and _metadata.etag;"
                        + " an update whose document carries _metadata.etag is rejected when the stored"
                        + " document changed since it was read (re-read and retry). Omitting _metadata"
                        + " forces an unconditional overwrite. "
                        + "Exposes row CRUD on table(s) BOOKINGS (get_by_pk / insert / update / delete),"
                        + " rows crossing as JSON objects keyed by column name. ",
                theInstructions);
    }

    @Test
    void theClausesKeepTheirOrder() {
        String theInstructions = instructionsWith(AUTHOR, true, true, true, true, true);

        int theAuthorAt = theInstructions.indexOf(AUTHOR);
        int theViewAt = theInstructions.indexOf("Document CRUD over");
        int theTableAt = theInstructions.indexOf("Exposes row CRUD on table(s)");
        int theRoutineAt = theInstructions.indexOf("Also exposes PL/SQL routine(s)");
        int theSqlAt = theInstructions.indexOf("Exposes user SQL statement(s)");
        int theSequenceAt = theInstructions.indexOf("Exposes sequence(s)");

        assertTrue(theAuthorAt == 0, theInstructions);
        assertTrue(theViewAt > theAuthorAt && theTableAt > theViewAt && theRoutineAt > theTableAt
                        && theSqlAt > theRoutineAt && theSequenceAt > theSqlAt,
                "clauses must read in the emitter's order: " + theInstructions);
    }

    @Test
    void aConfigExposingNothingSaysNothing() {
        assertEquals("", instructionsWith(null, false, false, false, false, false));
    }

    @Test
    void aDmlStatementClauseKeepsItsEscapedQuotes() {
        // This string is interpolated into GENERATED JAVA SOURCE, so its quotes are escaped for
        // that file rather than for this one. Asserting it here is how a well-meaning tidy of the
        // backslashes gets caught -- it would compile fine and break every generated tree.
        String theInstructions = instructionsWith(null, false, false, false, true, false);

        assertTrue(theInstructions.contains("{\\\"executed\\\":true}"), theInstructions);
    }
}
