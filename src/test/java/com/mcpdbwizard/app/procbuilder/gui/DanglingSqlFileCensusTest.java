package com.mcpdbwizard.app.procbuilder.gui;

import com.mcpdbwizard.schema.Schema;
import com.mcpdbwizard.schema.SqlStatement;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A census of {@code SQL_FILENAME_<i>} entries that name a file `Sqlfiles/` does not hold.
 *
 * <p><b>Why a census and not a prohibition.</b> There are 84 of them today, and fixing them is a
 * content decision — some of the files exist in another directory, some exist nowhere — recorded in
 * {@code docs/missing-sql-file-plan.md} §5. Asserting the exact set does two useful things in the
 * meantime: it stops the number growing unnoticed, and it makes progress visible, because working
 * through §5 turns this red and the fix is to lower the number here with a diff in front of you.
 *
 * <p><b>Why it matters that ten of them asked for a class.</b> A dangling entry with
 * {@code SQL_CREATE_CLASS=YES} is a class the config author asked for and silently did not get —
 * `generic_test1`, which every other count is measured against, is one of them. The rest are inert.
 * The two are asserted separately so a change that converts one into the other cannot pass.
 *
 * <p>This is a fact about the COMMITTED propfiles, so it needs no database and no generation.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class DanglingSqlFileCensusTest {

    /** The shared statement library. */
    private static final File SQL_DIR = new File("Sqlfiles");
    private static final File PROPFILES = new File("Propfiles");

    /**
     * What one propfile can see: the library plus its own overlay, exactly as
     * {@code testrun_current.sh}'s {@code sqlDirFor} composes it. The overlay key drops any
     * {@code _mcp} suffix because a variant shares its original's statements.
     */
    private Set<String> filesVisibleTo(String theConfigName) {
        String[] theLibrary = SQL_DIR.list();
        assertTrue(theLibrary != null && theLibrary.length > 0,
                "no SQL files found in " + SQL_DIR.getAbsolutePath());
        Set<String> theVisible = new LinkedHashSet<>(List.of(theLibrary));
        File theOverlay = new File(SQL_DIR, theConfigName.replace("_mcp", ""));
        if (theOverlay.isDirectory()) {
            String[] theExtra = theOverlay.list();
            if (theExtra != null) {
                theVisible.addAll(List.of(theExtra));
            }
        }
        return theVisible;
    }

    /**
     * A census of the DEVELOPMENT corpus, so it needs that corpus present.
     *
     * <p>The open-source export ships neither {@code Sqlfiles/} nor the configs that introspect
     * the private schema, and the pinned counts below are properties of the configs that stay
     * behind -- all four remaining dangling references live in them. Without this the export's
     * suite would fail on an assertion about files it was never given, which reads as a broken
     * test rather than an absent fixture.
     *
     * <p>Deliberately keyed on {@code Sqlfiles/}, not on a count: the numbers must stay exact
     * wherever the library IS present, because "update this number only downwards" is the whole
     * point of the assertions. Skipping on a low count instead would let the corpus quietly
     * shrink here too.
     */
    private Map<String, List<String>> census(boolean theCreateYesOnly) throws Exception {
        Assumptions.assumeTrue(SQL_DIR.isDirectory(),
                "no Sqlfiles/ -- the statement library is not part of this tree");
        File[] theConfigs = PROPFILES.listFiles((d, n) -> n.endsWith(".pb2"));
        assertTrue(theConfigs != null && theConfigs.length > 0, "no propfiles found");
        Map<String, List<String>> theResult = new TreeMap<>();
        for (File theConfig : theConfigs) {
            Schema theSchema = new Schema(theConfig);
            Set<String> theOnDisk = filesVisibleTo(
                    theConfig.getName().substring(0, theConfig.getName().length() - 4));
            List<String> theDangling = new ArrayList<>();
            for (SqlStatement theStatement : theSchema.getSqlStatements()) {
                String theName = theStatement.getFilename();
                if (theName == null || theOnDisk.contains(theName)) {
                    continue;
                }
                if (theCreateYesOnly && !"YES".equals(theStatement.getCreateClass())) {
                    continue;
                }
                theDangling.add(theName);
            }
            if (!theDangling.isEmpty()) {
                theResult.put(theConfig.getName(), theDangling);
            }
        }
        return theResult;
    }

    private int total(Map<String, List<String>> theCensus) {
        int theTotal = 0;
        for (List<String> theList : theCensus.values()) {
            theTotal = theTotal + theList.size();
        }
        return theTotal;
    }

    @Test
    void theDanglingReferenceCountIsExactlyWhatWasMeasured() throws Exception {
        Map<String, List<String>> theCensus = census(false);
        // UPDATE THIS NUMBER ONLY DOWNWARDS, and only with the diff in front of you -- see
        // docs/missing-sql-file-plan.md §5. Upwards means a propfile just gained a reference to a
        // file nobody added, which is the failure this exists to catch.
        //
        // 84 -> 18 when the regen moved to per-propfile SQL directories (library + overlay): six
        // files that existed elsewhere in the repo became visible.
        //
        // 18 -> 4 on 2026-08-13, closing §5 item 2 for the half that cost something. The seven
        // Demo*.sql references were removed from generic_testd/_mcp rather than written, because
        // they are DML against a customer/booking/flight application and GENERIC_TESTD holds
        // exactly two tables, SAMPLE_TABLE and TEST -- so no statement could have been written
        // that would run. They were never in git either: stale references inherited from a
        // repurposed config, naming an application the schema does not have.
        //
        // The 4 that remain are queryXml.sql in generic_testg/h and their _mcp siblings, and they
        // are DELIBERATELY KEPT: SQL_CREATE_CLASS=NO, so they generate nothing and cost nothing,
        // and the entry is a DESELECTION. An unnamed .sql in the directory defaults to generating,
        // so deleting the reference would silently start generating queryXml.sql the day somebody
        // adds it -- the opposite of what the author asked for.
        assertEquals(4, total(theCensus),
                "dangling SQL_FILENAME references changed; census:\n" + render(theCensus));
        assertEquals(4, theCensus.size(),
                "number of affected propfiles changed; census:\n" + render(theCensus));
    }

    @Test
    void theSubsetThatAskedForAClassIsPinnedSeparately() throws Exception {
        Map<String, List<String>> theCensus = census(true);
        // These are the ones that actually cost something: the author set SQL_CREATE_CLASS=YES and
        // no class is generated. 46 -> 14 with per-propfile directories, and 14 -> ZERO on
        // 2026-08-13 when generic_testd/_mcp's seven Demo*.sql references were removed. (The plan
        // doc first said 26 for the old figure: I summed the per-propfile list by eye and got it
        // wrong, and this assertion is what caught it.)
        //
        // Zero is the state worth defending. Every remaining dangling reference is now one that
        // asked for NOTHING, so a config can no longer promise a class it does not deliver. If this
        // goes above zero, a propfile has started naming a missing file AND asking for a class.
        assertEquals(0, total(theCensus),
                "dangling create=YES references changed; census:\n" + render(theCensus));
        assertEquals(0, theCensus.size(),
                "number of affected propfiles changed; census:\n" + render(theCensus));
        assertTrue(!theCensus.containsKey("generic_test1.pb2"),
                "generic_test1 was the worst case and is now clean -- if it comes back, the"
                        + " per-propfile SQL directory composition has regressed");
    }

    private String render(Map<String, List<String>> theCensus) {
        StringBuilder theText = new StringBuilder();
        for (Map.Entry<String, List<String>> theEntry : new LinkedHashMap<>(theCensus).entrySet()) {
            theText.append("  ").append(theEntry.getKey()).append(" -> ")
                    .append(String.join(", ", theEntry.getValue())).append("\n");
        }
        return theText.toString();
    }
}
