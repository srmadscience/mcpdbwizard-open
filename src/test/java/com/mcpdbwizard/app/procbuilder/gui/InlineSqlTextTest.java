package com.mcpdbwizard.app.procbuilder.gui;

import com.mcpdbwizard.schema.Schema;
import com.mcpdbwizard.schema.SqlStatement;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 1 of storing a user SQL statement IN the config ({@code SQL_TEXT_<i>}) rather than as a
 * filename pointing into {@code SQL_FILE_DIRECTORY}.
 *
 * <p>The old arrangement is why a committed propfile still carries
 * {@code SQL_FILE_DIRECTORY=C:\DR\Work\CodeSpooks\Sqlfiles} — an absolute path to a machine that no
 * longer exists — and why every consumer rewrites that directory before it can generate.
 *
 * <p>Phase 1 adds the key and teaches the loader to prefer it. It changes nothing for a config that
 * does not carry one, which is the property the estate's byte-identity check confirms.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class InlineSqlTextTest {

    private static final int LIMIT = 100;

    private static Properties configWith(int theIndex, String theName, String theSql) {
        Properties p = new Properties();
        p.setProperty("SQL_FILENAME_" + theIndex, theName);
        if (theSql != null) {
            p.setProperty("SQL_TEXT_" + theIndex, theSql);
        }
        return p;
    }

    // ---- the model ----------------------------------------------------------

    @Test
    void theStatementTextRoundTripsThroughBothFormats() {
        String theSql = "select ename\n  from emp\n where empno = ?\n";
        Schema theSchema = new Schema(configWith(0, "byempno.sql", theSql));

        assertEquals(theSql, theSchema.getSqlStatements().get(0).getSql());
        assertEquals(theSql, theSchema.toPb2().getProperty("SQL_TEXT_0"),
                "newlines and all - a statement is not a statement if it comes back reflowed");
        assertEquals(theSql, new Schema(theSchema.toJson()).getSqlStatements().get(0).getSql(),
                "lost crossing JSON");
    }

    /**
     * The key must be CLAIMED, not merely preserved. An unclaimed key lands in extraProperties,
     * where it round-trips perfectly while the model never sees it — so a round-trip assertion
     * alone would pass on a broken implementation.
     */
    @Test
    void theTextIsClaimedByTheModelRatherThanKeptAsAnUnknownKey() {
        Schema theSchema = new Schema(configWith(0, "x.sql", "select 1 from dual"));

        assertTrue(theSchema.getExtraProperties() == null
                        || !theSchema.getExtraProperties().containsKey("SQL_TEXT_0"),
                "SQL_TEXT_0 fell through to extraProperties: the switch in fromPb2 is what claims a"
                        + " key -- a family that is not listed there falls through to"
                        + " extraProperties and this would pass anyway");
    }

    @Test
    void aConfigWithNoInlineTextIsUnchanged() {
        // The Phase 1 neutrality claim, at model level: nothing appears that was not there.
        Properties theOld = configWith(0, "legacy.sql", null);
        Schema theSchema = new Schema(theOld);

        SqlStatement theStatement = theSchema.getSqlStatements().get(0);
        assertEquals("legacy.sql", theStatement.getFilename());
        assertNull(theStatement.getSql(), "absent must stay absent, not become empty");
        assertFalse(theSchema.toPb2().containsKey("SQL_TEXT_0"),
                "an absent key must not be written back as empty - that would change every config"
                        + " the first time it was saved");
    }

    // ---- the loader ---------------------------------------------------------

    @Test
    void aStatementCarriedInTheConfigIsFoundWithNoFileOnDisk() {
        // The whole point: no directory, no file, and the statement is still there.
        Properties theConfig = configWith(0, "inline.sql", "select 1 from dual");

        String[] theNames = ApplicationShell.mergeInlineSqlNames(new String[0], theConfig, LIMIT);

        assertEquals(List.of("inline.sql"), Arrays.asList(theNames));
    }

    @Test
    void aDirectoryListingStillDrivesTheListWhenNothingIsInlined() {
        // Neutrality again, this time at the loader: same input, same output as before the change.
        Properties theConfig = configWith(0, "ondisk.sql", null);

        String[] theNames = ApplicationShell.mergeInlineSqlNames(
                new String[] {"ondisk.sql", "other.sql"}, theConfig, LIMIT);

        assertEquals(List.of("ondisk.sql", "other.sql"), Arrays.asList(theNames),
                "files must come first and in their original order, so an existing deployment sees"
                        + " exactly the list it saw before");
    }

    @Test
    void aNameIsNotListedTwiceWhenItIsBothInlineAndOnDisk() {
        Properties theConfig = configWith(0, "both.sql", "select 1 from dual");

        String[] theNames = ApplicationShell.mergeInlineSqlNames(
                new String[] {"both.sql"}, theConfig, LIMIT);

        assertEquals(1, theNames.length, "a duplicate would generate the statement twice");
    }

    @Test
    void theConfigIndexIsFoundByName() {
        Properties theConfig = new Properties();
        theConfig.setProperty("SQL_FILENAME_0", "first.sql");
        theConfig.setProperty("SQL_FILENAME_7", "seventh.sql");   // deliberately not contiguous

        assertEquals(0, ApplicationShell.findSqlPropertyIndex(theConfig, "first.sql", LIMIT));
        assertEquals(7, ApplicationShell.findSqlPropertyIndex(theConfig, "seventh.sql", LIMIT),
                "the scan must survive a gap in the indexes, which are not guaranteed contiguous");
        assertEquals(-1, ApplicationShell.findSqlPropertyIndex(theConfig, "absent.sql", LIMIT));
    }

    /**
     * The user-visible payoff. A config that names a SQL file not present in the directory produces
     * a warning saying no statement was read for it — the "SQL files go missing" complaint this
     * whole change exists to answer. A statement carrying its own text must not produce it.
     */
    @Test
    void anInlineStatementIsNotReportedAsAMissingFile() {
        Properties theConfig = configWith(0, "inline.sql", "select 1 from dual");
        theConfig.setProperty("SQL_CREATE_CLASS_0", "YES");

        // What the loader now hands the reporter: the merged list, not the bare directory listing.
        Set<String> theFound = new LinkedHashSet<>(Arrays.asList(
                ApplicationShell.mergeInlineSqlNames(new String[0], theConfig, LIMIT)));

        assertTrue(ApplicationShell.sqlFilesNamedButNotFound(theConfig, theFound, LIMIT).isEmpty(),
                "a statement the config carries itself cannot be a missing file");
    }

    @Test
    void aStatementWithNeitherFileNorTextIsStillReportedMissing() {
        // The warning must not be silenced generally - only for statements that really are present.
        Properties theConfig = configWith(0, "gone.sql", null);
        theConfig.setProperty("SQL_CREATE_CLASS_0", "YES");

        Set<String> theFound = new LinkedHashSet<>(Arrays.asList(
                ApplicationShell.mergeInlineSqlNames(new String[0], theConfig, LIMIT)));

        List<String[]> theMisses =
                ApplicationShell.sqlFilesNamedButNotFound(theConfig, theFound, LIMIT);
        assertEquals(1, theMisses.size(), "a genuinely missing file must still be reported");
        assertEquals("gone.sql", theMisses.get(0)[0]);
    }
}
