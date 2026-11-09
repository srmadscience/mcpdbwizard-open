package com.mcpdbwizard.app.procbuilder;

import com.mcpdbwizard.app.common.JavaUtils;
import org.junit.jupiter.api.Test;

import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A sequence tool's default description reaches the agent with the sequence's name as it is,
 * escaped once for the generated Java and no more.
 *
 * <p>{@code JavaUtils.getOracleName} returns a name that needs quoting already escaped for Java
 * source ({@code \"test seq 4\"}), and the description escaped it a second time, so the agent read
 * {@code Get the next value of sequence \"test seq 4\"} with the backslashes. Found by the first
 * MariaDB end-to-end run (2026-11-08); the code is shared, so Oracle had it too.
 *
 * <p>The check compiles the emitted literal and reads the string back, rather than comparing
 * escaped text: a quote escaped once too few or too many is exactly what a text comparison gets
 * wrong in the same way the code does.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class SequenceDescriptionLiteralTest {

    /** What the generated server's string holds, by compiling {@code literalContents} with javac. */
    private static String compiled(String literalContents) throws Exception {
        Path dir = Files.createTempDirectory("seq-desc-");
        Path src = dir.resolve("D.java");
        Files.write(src, ("public class D { public static final String S = \"" + literalContents + "\"; }\n")
                .getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int rc = ToolProvider.getSystemJavaCompiler().run(null, null, err, "-d", dir.toString(), src.toString());
        assertEquals(0, rc, err.toString(StandardCharsets.UTF_8));
        try (URLClassLoader loader = new URLClassLoader(new URL[]{dir.toUri().toURL()})) {
            return (String) loader.loadClass("D").getField("S").get(null);
        }
    }

    @Test
    void aNameThatNeedsQuotingIsShownOnce() throws Exception {
        String oracleName = JavaUtils.getOracleName("SCOTT", "test seq 4", "User's Object", null, false);
        assertEquals("\\\"test seq 4\\\"", oracleName, "the input really is Java-escaped");
        assertEquals("Get the next value of sequence \"test seq 4\". Returns {\"nextval\":<n>}.",
                compiled(SAAdminWrangler.mcpSequenceDescriptionLiteral(oracleName)));
    }

    @Test
    void anOrdinaryNameIsUnchanged() throws Exception {
        assertEquals("Get the next value of sequence TEST_SEQ_1. Returns {\"nextval\":<n>}.",
                compiled(SAAdminWrangler.mcpSequenceDescriptionLiteral("TEST_SEQ_1")));
        String other = JavaUtils.getOracleName("SYNUSER", "JOB_ID", "Other User's Object", null, false);
        assertEquals("Get the next value of sequence SYNUSER.JOB_ID. Returns {\"nextval\":<n>}.",
                compiled(SAAdminWrangler.mcpSequenceDescriptionLiteral(other)));
    }
}
