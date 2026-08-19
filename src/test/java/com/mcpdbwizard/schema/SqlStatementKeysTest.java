package com.mcpdbwizard.schema;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The SQL-statement half of a config, pinned before its storage changes.
 *
 * <p>A config currently stores a statement as a bare FILENAME plus one global
 * {@code SQL_FILE_DIRECTORY}, and the text lives on disk. That is being changed so the config
 * carries the statement itself. These tests are the safety net for that work: they say what the
 * model must keep doing, so a later phase can be shown to have changed nothing.
 *
 * <p><b>They are deliberately not enough on their own.</b> A model that round-trips perfectly can
 * still hand {@code SqlStatementWrangler} a different string and rename every generated class —
 * that constructor strips a fixed four characters assuming {@code ".sql"}, so {@code update5.sql}
 * becomes {@code Update5} while {@code update5} becomes {@code Upda}. Only a byte-comparison of
 * GENERATED OUTPUT catches that, and it needs a live database. This file is the fast half.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
@EnabledIf("theConfigCorpusIsPresent")
class SqlStatementKeysTest {

    /**
     * The open-source export ships no configs -- a config enumerates the schema it introspects --
     * so this whole class has nothing to examine there. Disabled at class level rather than
     * skipped per test: two of these are {@code @ParameterizedTest}s whose source would be empty,
     * and JUnit 5.10 fails a parameterised test that receives zero arguments rather than skipping
     * it, so an {@code Assumptions} call inside the test body never runs.
     */
    static boolean theConfigCorpusIsPresent() {
        return new java.io.File("Propfiles").isDirectory();
    }

    /** Propfiles that declare at least one SQL statement. */
    private static List<File> configsWithSql() {
        File dir = new File("Propfiles");
        File[] all = dir.listFiles((d, n) -> n.endsWith(".pb2"));
        assertNotNull(all, "Propfiles/ not found - run from the app module directory");
        List<File> withSql = new ArrayList<>();
        for (File f : all) {
            Properties p = load(f);
            for (String key : p.stringPropertyNames()) {
                if (key.startsWith("SQL_FILENAME_")) {
                    withSql.add(f);
                    break;
                }
            }
        }
        return withSql;
    }

    private static Properties load(File theFile) {
        Properties p = new Properties();
        try (InputStream in = new FileInputStream(theFile)) {
            p.load(in);
        } catch (Exception e) {
            throw new IllegalStateException("cannot read " + theFile, e);
        }
        return p;
    }

    /**
     * The corpus must not be empty, and must contain a config that really exercises the feature.
     *
     * <p>Written because the obvious choice of fixture was WRONG: {@code generic_test5} declares 29
     * statements but sets {@code SQL_CREATE_CLASS=YES} on exactly ONE, so a safety net built on it
     * would pass almost any refactor while appearing thorough. Statements declared and statements
     * that generate a class are different counts, and it is the second that matters.
     */
    @Test
    void theCorpusActuallyExercisesSqlStatements() {
        // This asserts a property of the DEVELOPMENT corpus: that it is rich enough for the
        // sibling tests in this class to mean anything. The open-source export withholds the
        // statement library together with the configs that introspect the private schema -- which
        // are the statement-heavy ones -- so there the property is simply false and cannot be made
        // true. Asserting it anyway would fail the export's suite over an absent fixture.
        //
        // Keyed on Sqlfiles/ rather than on a count, for the reason the assertion itself exists:
        // a threshold that lowers itself when the corpus shrinks is not a guard. Wherever the
        // library IS present, both numbers below stay exact.
        Assumptions.assumeTrue(new File("Sqlfiles").isDirectory(),
                "no Sqlfiles/ -- the statement-heavy configs are not part of this tree");

        List<File> theConfigs = configsWithSql();
        assertTrue(theConfigs.size() >= 10,
                "expected many configs with SQL statements, found " + theConfigs.size());

        int theBestClassCount = 0;
        String theBest = null;
        for (File f : theConfigs) {
            Properties p = load(f);
            int created = 0;
            for (String key : p.stringPropertyNames()) {
                if (key.startsWith("SQL_CREATE_CLASS_") && "YES".equals(p.getProperty(key))) {
                    created++;
                }
            }
            if (created > theBestClassCount) {
                theBestClassCount = created;
                theBest = f.getName();
            }
        }
        assertTrue(theBestClassCount >= 20,
                "no config generates enough SQL classes to be a meaningful guard; best was "
                        + theBest + " with " + theBestClassCount);
    }

    /**
     * Every SQL key survives PB2 -> Schema -> PB2, and nothing falls through to extraProperties.
     *
     * <p>extraProperties is the silent-success trap: it preserves an unrecognised key faithfully, so
     * a round-trip test passes while the model never sees the value. When {@code SQL_TEXT_<i>} is
     * added it must be claimed by a {@code case} in {@code fromPb2}'s switch — note it is the switch
     * that claims keys: a family not listed there falls through to extraProperties. (A dead
     * {@code COLLECTION_KEY} pattern used to sit near the top of that file inviting exactly the
     * wrong edit; it was deleted once the SQL work proved it gated nothing.)
     */
    @Test
    void everySqlKeyRoundTripsAndNothingLandsInExtraProperties() {
        for (File theConfig : configsWithSql()) {
            Properties theOriginal = load(theConfig);
            Schema theSchema = new Schema(theOriginal);

            assertTrue(theSchema.getExtraProperties() == null
                            || theSchema.getExtraProperties().isEmpty(),
                    theConfig.getName() + " left keys in extraProperties: "
                            + theSchema.getExtraProperties());

            Properties theRoundTripped = theSchema.toPb2();
            for (String key : theOriginal.stringPropertyNames()) {
                if (!key.startsWith("SQL_")) {
                    continue;
                }
                assertEquals(theOriginal.getProperty(key), theRoundTripped.getProperty(key),
                        theConfig.getName() + " lost or changed " + key);
            }
        }
    }

    /**
     * Proves the trap the test above guards against is real.
     *
     * <p>An unclaimed key does not fail, warn, or vanish — it is quietly kept in
     * {@code extraProperties} and round-trips perfectly, so a test that only checked round-tripping
     * would pass while the model never saw the value. That is why the assertion above is about
     * {@code extraProperties} being EMPTY rather than about the keys surviving.
     *
     * <p>Uses a key that will never be claimed, so this stays true whatever is added later.
     */
    @Test
    void anUnclaimedKeyFallsIntoExtraPropertiesRatherThanFailing() {
        Properties theProperties = new Properties();
        theProperties.setProperty("SQL_NOT_A_REAL_FAMILY_0", "some value");

        Schema theSchema = new Schema(theProperties);

        assertNotNull(theSchema.getExtraProperties());
        assertEquals("some value", theSchema.getExtraProperties().get("SQL_NOT_A_REAL_FAMILY_0"),
                "an unrecognised key is kept verbatim - which is why a round-trip alone proves"
                        + " nothing about the model having understood it");
        assertEquals("some value", theSchema.toPb2().getProperty("SQL_NOT_A_REAL_FAMILY_0"),
                "and it round-trips, silently");
    }

    /** The same, through the JSON format. */
    @Test
    void everySqlKeySurvivesTheJsonFormatToo() {
        for (File theConfig : configsWithSql()) {
            Properties theOriginal = load(theConfig);
            Properties theRoundTripped = new Schema(new Schema(theOriginal).toJson()).toPb2();
            for (String key : theOriginal.stringPropertyNames()) {
                if (!key.startsWith("SQL_")) {
                    continue;
                }
                assertEquals(theOriginal.getProperty(key), theRoundTripped.getProperty(key),
                        theConfig.getName() + " lost " + key + " crossing JSON");
            }
        }
    }

    /**
     * The property the whole design rests on: a {@code .pb2} is a {@link Properties} file, so
     * inlining a statement means putting a MULTI-LINE value in one. Worth proving before relying
     * on it — {@code store} escapes the newlines and {@code load} restores them, so the text
     * survives exactly.
     */
    @Test
    void aMultiLineValueSurvivesAPropertiesRoundTrip() throws Exception {
        String theSql = "select empno,\n       ename\n  from emp\n where deptno = ?\n";

        Properties theWritten = new Properties();
        theWritten.setProperty("SQL_TEXT_0", theSql);

        Path theFile = Files.createTempFile("sqltext", ".pb2");
        try (OutputStream out = Files.newOutputStream(theFile)) {
            theWritten.store(out, "test");
        }

        // The stored form must be single-line-escaped, or a .pb2 would stop being loadable.
        String theRaw = Files.readString(theFile);
        assertTrue(theRaw.contains("\\n"), "newlines should be escaped on disk: " + theRaw);

        Properties theRead = new Properties();
        try (InputStream in = Files.newInputStream(theFile)) {
            theRead.load(in);
        }
        assertEquals(theSql, theRead.getProperty("SQL_TEXT_0"),
                "the statement text must survive byte-for-byte, newlines included");
        Files.deleteIfExists(theFile);
    }

    /**
     * The naming coupling, stated so a later phase cannot break it unnoticed.
     *
     * <p>{@code SqlStatementWrangler} derives the generated class name from the filename by
     * removing a fixed four characters. Keeping the {@code .sql} suffix on {@code SQL_FILENAME_<i>}
     * is what makes the inlining a pure addition; this asserts the stored values really do carry
     * it, since the decision is worthless if some config already stores a bare name.
     */
    @Test
    void everyStoredFilenameKeepsItsSqlSuffix() {
        int theChecked = 0;
        for (File theConfig : configsWithSql()) {
            Properties p = load(theConfig);
            for (String key : p.stringPropertyNames()) {
                if (!key.startsWith("SQL_FILENAME_")) {
                    continue;
                }
                String theName = p.getProperty(key);
                theChecked++;
                assertTrue(theName.length() > 4 && theName.toLowerCase().endsWith(".sql"),
                        theConfig.getName() + " " + key + " = '" + theName
                                + "' - the class name is the filename minus its last FOUR"
                                + " characters, so a name without .sql is silently truncated");
            }
        }
        assertTrue(theChecked > 100, "expected a large corpus of filenames, checked " + theChecked);
    }
}
