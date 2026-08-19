package com.mcpdbwizard.schema;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The migration that moves a statement's text out of a file and into the config that names it.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class SqlInlinerTest {

    private static Path pb2With(Path theDir, String... theKeyValues) throws Exception {
        Properties p = new Properties();
        for (int i = 0; i < theKeyValues.length; i += 2) {
            p.setProperty(theKeyValues[i], theKeyValues[i + 1]);
        }
        Path theFile = theDir.resolve("config.pb2");
        try (OutputStream out = Files.newOutputStream(theFile)) {
            p.store(out, "MCPDBWizard test");
        }
        return theFile;
    }

    private static Properties load(Path theFile) throws Exception {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(theFile)) {
            p.load(in);
        }
        return p;
    }

    @Test
    void theStatementTextIsCopiedIntoTheConfig(@TempDir Path theDir) throws Exception {
        Path theSqlDir = Files.createDirectories(theDir.resolve("sql"));
        String theSql = "select ename\n  from emp\n where empno = ?\n";
        Files.write(theSqlDir.resolve("byempno.sql"), theSql.getBytes(StandardCharsets.UTF_8));
        Path theConfig = pb2With(theDir, "SQL_FILENAME_0", "byempno.sql");

        SqlInliner.Result theResult = SqlInliner.migrate(theConfig, List.of(theSqlDir));

        assertEquals(List.of("byempno.sql"), theResult.getInlined());
        assertEquals(theSql, load(theConfig).getProperty("SQL_TEXT_0"),
                "the statement must arrive byte-for-byte, newlines and all");
    }

    /**
     * An overlay in front of a shared library, which is how the propfiles resolve their SQL: the
     * first directory that has the file wins.
     */
    @Test
    void theSearchPathIsOrderedAndFirstMatchWins(@TempDir Path theDir) throws Exception {
        Path theOverlay = Files.createDirectories(theDir.resolve("overlay"));
        Path theLibrary = Files.createDirectories(theDir.resolve("library"));
        Files.write(theOverlay.resolve("q.sql"), "select 'overlay' from dual".getBytes(StandardCharsets.UTF_8));
        Files.write(theLibrary.resolve("q.sql"), "select 'library' from dual".getBytes(StandardCharsets.UTF_8));
        Path theConfig = pb2With(theDir, "SQL_FILENAME_0", "q.sql");

        SqlInliner.migrate(theConfig, List.of(theOverlay, theLibrary));

        assertTrue(load(theConfig).getProperty("SQL_TEXT_0").contains("overlay"),
                "the overlay must win, as it does when the directory is composed for generation");
    }

    /**
     * The property that makes it safe to run over every committed propfile, twice, by accident.
     */
    @Test
    void migratingTwiceChangesNothingTheSecondTime(@TempDir Path theDir) throws Exception {
        Path theSqlDir = Files.createDirectories(theDir.resolve("sql"));
        Files.write(theSqlDir.resolve("q.sql"), "select 1 from dual".getBytes(StandardCharsets.UTF_8));
        Path theConfig = pb2With(theDir, "SQL_FILENAME_0", "q.sql");

        SqlInliner.migrate(theConfig, List.of(theSqlDir));
        String theAfterFirst = Files.readString(theConfig);
        SqlInliner.Result theSecond = SqlInliner.migrate(theConfig, List.of(theSqlDir));

        assertFalse(theSecond.changedAnything());
        assertEquals(List.of("q.sql"), theSecond.getAlreadyInline());
        assertEquals(theAfterFirst, Files.readString(theConfig),
                "the file must be untouched, not merely equivalent");
    }

    /**
     * A statement whose file has gone is the condition this whole change exists to cure. It must be
     * REPORTED and skipped, never fatal — refusing to migrate the other statements over it would
     * leave the config exactly as broken as before, with no partial progress.
     */
    @Test
    void aMissingFileIsReportedAndTheRestStillMigrate(@TempDir Path theDir) throws Exception {
        Path theSqlDir = Files.createDirectories(theDir.resolve("sql"));
        Files.write(theSqlDir.resolve("present.sql"), "select 1 from dual".getBytes(StandardCharsets.UTF_8));
        Path theConfig = pb2With(theDir,
                "SQL_FILENAME_0", "present.sql",
                "SQL_FILENAME_1", "vanished.sql");

        SqlInliner.Result theResult = SqlInliner.migrate(theConfig, List.of(theSqlDir));

        assertEquals(List.of("present.sql"), theResult.getInlined());
        assertEquals(List.of("vanished.sql"), theResult.getNotFound());
        Properties theMigrated = load(theConfig);
        assertTrue(theMigrated.containsKey("SQL_TEXT_0"));
        assertFalse(theMigrated.containsKey("SQL_TEXT_1"),
                "a statement with no text must stay without one rather than gain an empty value,"
                        + " which would later read as 'this statement is empty'");
    }

    /**
     * The existing content must survive untouched. These files are 2011 {@code store()} output in
     * hash order, so a rewrite would reorder every line and bury the migration in noise.
     */
    @Test
    void theOriginalLinesAreLeftAloneAndTheNewOnesAppended(@TempDir Path theDir) throws Exception {
        Path theSqlDir = Files.createDirectories(theDir.resolve("sql"));
        Files.write(theSqlDir.resolve("q.sql"), "select 1 from dual".getBytes(StandardCharsets.UTF_8));
        Path theConfig = pb2With(theDir, "SQL_FILENAME_0", "q.sql", "PACKAGE_NAME", "com.example");
        String theBefore = Files.readString(theConfig);

        SqlInliner.migrate(theConfig, List.of(theSqlDir));

        String theAfter = Files.readString(theConfig);
        assertTrue(theAfter.startsWith(theBefore),
                "the migration must be a pure append - the original bytes, then the new keys");
        assertTrue(theAfter.length() > theBefore.length(), "the file should have grown");
        assertTrue(theAfter.substring(theBefore.length()).startsWith("SQL_TEXT_0="));
    }

    /**
     * Escaping is Java's, not hand-rolled: a statement full of the characters a properties file
     * treats specially must survive.
     */
    @Test
    void aStatementFullOfSpecialCharactersSurvives(@TempDir Path theDir) throws Exception {
        Path theSqlDir = Files.createDirectories(theDir.resolve("sql"));
        String theSql = "select 'a:b', \"C=D\", '\\path\\here'   -- # comment !\n  from dual\n";
        Files.write(theSqlDir.resolve("nasty.sql"), theSql.getBytes(StandardCharsets.UTF_8));
        Path theConfig = pb2With(theDir, "SQL_FILENAME_0", "nasty.sql");

        SqlInliner.migrate(theConfig, List.of(theSqlDir));

        assertEquals(theSql, load(theConfig).getProperty("SQL_TEXT_0"),
                "colons, equals, hashes, backslashes and newlines all have meaning in a .pb2");
    }
}
