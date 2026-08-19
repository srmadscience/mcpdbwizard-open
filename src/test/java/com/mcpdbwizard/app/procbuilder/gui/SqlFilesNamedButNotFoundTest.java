package com.mcpdbwizard.app.procbuilder.gui;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The diff behind the "named by this configuration but is not in …" warning.
 *
 * <p>The statement list is built by scanning the SQL directory and matching what it finds back to
 * the config's keys, so a named file that is absent produces no wrangler and nothing downstream can
 * mention it. This walk is what notices.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class SqlFilesNamedButNotFoundTest {

    private static final int LIMIT = 100;

    private Properties config(String... theNameThenFlagPairs) {
        Properties theConfig = new Properties();
        for (int i = 0; i < theNameThenFlagPairs.length; i = i + 2) {
            int theIndex = i / 2;
            theConfig.setProperty("SQL_FILENAME_" + theIndex, theNameThenFlagPairs[i]);
            if (theNameThenFlagPairs[i + 1] != null) {
                theConfig.setProperty("SQL_CREATE_CLASS_" + theIndex, theNameThenFlagPairs[i + 1]);
            }
        }
        return theConfig;
    }

    @Test
    void aConfigWhoseFilesAreAllPresentReportsNothing() {
        List<String[]> theMisses = ApplicationShell.sqlFilesNamedButNotFound(
                config("a.sql", "YES", "b.sql", "NO"), Set.of("a.sql", "b.sql"), LIMIT);
        assertTrue(theMisses.isEmpty(), "expected no misses, got " + theMisses.size());
    }

    @Test
    void anAbsentFileIsReportedWithTheFlagThatDecidesHowMuchItMatters() {
        // The flag is carried because it separates "you will not get the class you asked for" from
        // an inert leftover, and most dangling entries are the latter.
        List<String[]> theMisses = ApplicationShell.sqlFilesNamedButNotFound(
                config("here.sql", "YES", "gone.sql", "YES"), Set.of("here.sql"), LIMIT);
        assertEquals(1, theMisses.size());
        assertEquals("gone.sql", theMisses.get(0)[0]);
        assertEquals("YES", theMisses.get(0)[1]);
    }

    @Test
    void anAbsentFileThatAskedForNoClassIsStillReported() {
        // Inert, but it is still a description that can be written and never used, and it is still a
        // config naming something that is not there. Reported, and the message says it is harmless.
        List<String[]> theMisses = ApplicationShell.sqlFilesNamedButNotFound(
                config("gone.sql", "NO"), Set.of("other.sql"), LIMIT);
        assertEquals(1, theMisses.size());
        assertEquals("NO", theMisses.get(0)[1]);
    }

    @Test
    void aGapInTheIndicesDoesNotStopTheWalk() {
        // Indices are not guaranteed contiguous -- an entry removed by hand leaves a hole, and
        // stopping at the first one would silently skip everything after it.
        Properties theConfig = new Properties();
        theConfig.setProperty("SQL_FILENAME_0", "gone0.sql");
        theConfig.setProperty("SQL_FILENAME_7", "gone7.sql");
        List<String[]> theMisses = ApplicationShell.sqlFilesNamedButNotFound(
                theConfig, Set.of(), LIMIT);
        assertEquals(2, theMisses.size());
        assertEquals("gone0.sql", theMisses.get(0)[0]);
        assertEquals("gone7.sql", theMisses.get(1)[0]);
    }

    @Test
    void aConfigWithNoSqlStatementsAtAllReportsNothing() {
        assertTrue(ApplicationShell.sqlFilesNamedButNotFound(
                new Properties(), Set.of("a.sql"), LIMIT).isEmpty());
    }

    @Test
    void missesComeBackInConfigOrder() {
        // The warnings read as a list, so the order should match the config rather than a hash.
        List<String[]> theMisses = ApplicationShell.sqlFilesNamedButNotFound(
                config("z.sql", "NO", "a.sql", "NO", "m.sql", "NO"), Set.of(), LIMIT);
        assertEquals(List.of("z.sql", "a.sql", "m.sql"),
                theMisses.stream().map(m -> m[0]).toList());
    }
}
