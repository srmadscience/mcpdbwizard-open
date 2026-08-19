package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for reading a secret from an environment variable or the file one points at.
 *
 * <p>The behaviours worth pinning are the ones that would corrupt a password silently: the trailing
 * newline {@code echo secret > file} leaves, which is not part of the secret, and the trailing space
 * which may well be. Plus the refusal to guess when two sources are configured at once.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class EnvironmentSecretTest {

    private static Path fileHolding(Path theDirectory, String theContents) throws IOException {
        Path theFile = theDirectory.resolve("secret");
        Files.write(theFile, theContents.getBytes(StandardCharsets.UTF_8));
        return theFile;
    }

    @Test
    void thePlainVariableStillWorks() {
        assertEquals("hunter2", EnvironmentSecret.read("DB_PASS", "hunter2", null));
    }

    @Test
    void neitherSetIsNotAnError() {
        // The caller decides what an absent secret means; for a config that never asked for one it
        // is perfectly normal.
        assertNull(EnvironmentSecret.read("DB_PASS", null, null));
        assertNull(EnvironmentSecret.read("DB_PASS", "", "  "));
    }

    @Test
    void aFileIsReadWhenTheFileVariableIsSet(@TempDir Path theDirectory) throws IOException {
        Path theFile = fileHolding(theDirectory, "hunter2");

        assertEquals("hunter2", EnvironmentSecret.read("DB_PASS", null, theFile.toString()));
        assertEquals("hunter2", EnvironmentSecret.read("DB_PASS", null, " " + theFile + " "),
                "a path with stray whitespace around it is still a path");
    }

    @Test
    void theTrailingNewlineFromEchoIsNotPartOfTheSecret(@TempDir Path theDirectory) throws IOException {
        // `echo secret > file` is how most people will create this, and the newline it adds would
        // otherwise become part of the password and fail the login with nothing to see.
        assertEquals("hunter2",
                EnvironmentSecret.read("DB_PASS", null, fileHolding(theDirectory, "hunter2\n").toString()));

        Path theCrLfFile = theDirectory.resolve("crlf");
        Files.write(theCrLfFile, "hunter2\r\n\n".getBytes(StandardCharsets.UTF_8));
        assertEquals("hunter2", EnvironmentSecret.read("DB_PASS", null, theCrLfFile.toString()));
    }

    @Test
    void aTrailingSpaceIsKeptBecauseItMayBeTheSecret(@TempDir Path theDirectory) throws IOException {
        // Deliberately not trimmed: silently dropping it would break a legitimate password, and the
        // failure would look like a wrong credential rather than a mangled one.
        assertEquals("hunter2 ",
                EnvironmentSecret.read("DB_PASS", null, fileHolding(theDirectory, "hunter2 \n").toString()));
    }

    @Test
    void internalStructureIsPreserved(@TempDir Path theDirectory) throws IOException {
        assertEquals("line1\nline2",
                EnvironmentSecret.read("DB_PASS", null, fileHolding(theDirectory, "line1\nline2\n").toString()));
    }

    @Test
    void settingBothIsRefusedRatherThanResolvedByPrecedence(@TempDir Path theDirectory) throws IOException {
        Path theFile = fileHolding(theDirectory, "from-file");

        IllegalStateException theException = assertThrows(IllegalStateException.class,
                () -> EnvironmentSecret.read("DB_PASS", "from-env", theFile.toString()));

        assertTrue(theException.getMessage().contains("DB_PASS"), theException.getMessage());
        assertTrue(theException.getMessage().contains("DB_PASS_FILE"), theException.getMessage());
    }

    @Test
    void aMissingFileSaysWhichPathItTried(@TempDir Path theDirectory) {
        String theMissingPath = theDirectory.resolve("absent").toString();

        IllegalStateException theException = assertThrows(IllegalStateException.class,
                () -> EnvironmentSecret.read("DB_PASS", null, theMissingPath));

        assertTrue(theException.getMessage().contains(theMissingPath),
                "an operator needs the path to fix the mount: " + theException.getMessage());
    }

    @Test
    void anEmptyFileIsAnErrorNotAnEmptyPassword(@TempDir Path theDirectory) throws IOException {
        // An empty secret mount is a misconfiguration; connecting with a blank password would fail
        // later and blame the database.
        assertThrows(IllegalStateException.class,
                () -> EnvironmentSecret.read("DB_PASS", null, fileHolding(theDirectory, "").toString()));
        assertThrows(IllegalStateException.class,
                () -> EnvironmentSecret.read("DB_PASS", null, fileHolding(theDirectory, "\n\n").toString()));
    }

    @Test
    void theSuffixIsTheConventionalOne() {
        // Matches the postgres and mysql images, so operators already know the shape.
        assertEquals("_FILE", EnvironmentSecret.FILE_SUFFIX);
    }
}
