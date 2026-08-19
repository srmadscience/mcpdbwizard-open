package com.mcpdbwizard.pub;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Reads a secret from the environment, either directly or from a file the environment points at.
 *
 * <p>Setting {@code DB_PASS} works. So does setting {@code DB_PASS_FILE} to a path whose contents are
 * the secret — the convention the official postgres and mysql images use, and what Docker Swarm
 * secrets and Kubernetes secret volumes are shaped for.
 *
 * <p>The file form is the better one, and not by much effort:
 *
 * <ul>
 *   <li>it does not appear in {@code docker inspect};</li>
 *   <li>it is not inherited by every child process, which matters here because the web application
 *       forks the generator and the generated MCP server;</li>
 *   <li>secret mounts are tmpfs-backed rather than written to the image or a disk layer;</li>
 *   <li>it can be rotated by replacing the file.</li>
 * </ul>
 *
 * <p>Both set at once is an <b>error</b> rather than a precedence rule. Two sources for one secret is
 * a misconfiguration, and quietly preferring one hides which credential is actually in use — the sort
 * of thing found only when the wrong one expires.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class EnvironmentSecret {

    /** Appended to a variable name to get the one naming a file instead. */
    public static final String FILE_SUFFIX = "_FILE";

    private EnvironmentSecret() {
    }

    /**
     * The secret, or null when neither variable is set.
     *
     * @param theVariableName e.g. {@code DB_PASS}; {@code DB_PASS_FILE} is consulted too
     * @throws IllegalStateException if both are set, or the file cannot be read, or it is empty
     */
    public static String read(String theVariableName) {
        return read(theVariableName,
                System.getenv(theVariableName),
                System.getenv(theVariableName + FILE_SUFFIX));
    }

    /** Testable half of {@link #read(String)} — the JVM cannot set its own environment. */
    static String read(String theVariableName, String thePlainValue, String theFilePath) {
        boolean theHavePlainFlag = thePlainValue != null && thePlainValue.length() > 0;
        boolean theHaveFileFlag = theFilePath != null && theFilePath.trim().length() > 0;

        if (theHavePlainFlag && theHaveFileFlag) {
            throw new IllegalStateException("Both " + theVariableName + " and " + theVariableName
                    + FILE_SUFFIX + " are set. They are alternatives, so set exactly one - otherwise"
                    + " which credential is in use depends on a precedence rule nobody remembers.");
        }
        if (theHavePlainFlag) {
            return thePlainValue;
        }
        if (!theHaveFileFlag) {
            return null;
        }

        String theContents;
        try {
            theContents = new String(Files.readAllBytes(Paths.get(theFilePath.trim())),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(theVariableName + FILE_SUFFIX + " points at "
                    + theFilePath.trim() + ", which cannot be read: " + e.getMessage());
        }

        String theSecret = stripTrailingNewlines(theContents);
        if (theSecret.length() == 0) {
            throw new IllegalStateException(theVariableName + FILE_SUFFIX + " points at "
                    + theFilePath.trim() + ", which is empty.");
        }
        return theSecret;
    }

    /**
     * Drop trailing line terminators only.
     *
     * <p>{@code echo secret > file} leaves a newline that is not part of the password, so it has to
     * go. Spaces are left alone: a password may legitimately end in one, and silently trimming it
     * would produce a login failure with nothing to see.
     */
    private static String stripTrailingNewlines(String theText) {
        int theEnd = theText.length();
        while (theEnd > 0 && (theText.charAt(theEnd - 1) == '\n' || theText.charAt(theEnd - 1) == '\r')) {
            theEnd--;
        }
        return theText.substring(0, theEnd);
    }
}
