/*
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
package com.mcpdbwizard.app.procbuilder;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two fixtures that exercise {@code CallableStatementParameterEngine}'s parameter-count
 * thresholds still straddle them.
 *
 * <p>WHY THIS TEST EXISTS. The engine has two: {@code ROWSET_WARNING_SIZE = 200} emits a WARNING
 * javadoc and still generates the service method, and {@code ROWSET_MAX_SIZE = 245} refuses to
 * generate it at all. Until 2026-08-19 the only fixture above either was a 430-bind INSERT against
 * a customer schema. It was withdrawn with that schema, and NOTHING replaced it -- found by
 * grepping a full 41-config tree for the refusal message and getting no hits.
 *
 * <p>That is the failure worth preventing, and it is quiet in both directions: the branch simply
 * stops being reached, every box stays green, and the count that would have shown it (a service
 * method appearing where a comment belongs) is three lines inside one generated file nobody diffs.
 *
 * <p>So this asserts the SHAPE OF THE FIXTURES rather than the generator's behaviour. Checking the
 * emitted output would need a live database and a regenerated tree; checking the inputs needs
 * neither, and the inputs are what rot. If someone trims these files, this fails immediately with
 * a message saying why they are that size.
 *
 * <p>Deliberately NOT pinned to exact counts: the point is which side of each threshold a file
 * falls on, and pinning 220 and 260 exactly would turn a harmless edit into a failure.
 */
class WideParameterFixtureTest {

    /** Mirrors CallableStatementParameterEngine.ROWSET_WARNING_SIZE. */
    private static final int WARNING_SIZE = 200;
    /** Mirrors CallableStatementParameterEngine.ROWSET_MAX_SIZE. */
    private static final int MAX_SIZE = 245;

    private static final File DIR = new File("Sqlfiles/generic_test3");

    private int bindCount(String theName) throws Exception {
        File f = new File(DIR, theName);
        assertTrue(f.isFile(), theName + " is missing -- it is the only fixture on its side of the"
                + " threshold, so losing it silently disables a generator branch. See its header.");
        String sql = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        int n = 0;
        for (int i = 0; i < sql.length(); i++) {
            if (sql.charAt(i) == '?') { n++; }
        }
        return n;
    }

    @Test
    void oneFixtureSitsBetweenTheWarningAndTheRefusal() throws Exception {
        Assumptions.assumeTrue(DIR.isDirectory(), "no Sqlfiles/ -- not this tree");
        int n = bindCount("wideparams220.sql");
        assertTrue(n > WARNING_SIZE,
                "wideparams220.sql has " + n + " binds; it must exceed " + WARNING_SIZE
                        + " or the WARNING branch is never compiled against anything");
        assertTrue(n <= MAX_SIZE,
                "wideparams220.sql has " + n + " binds; above " + MAX_SIZE + " it stops testing the"
                        + " warning band and duplicates the refusal fixture instead");
    }

    @Test
    void theOtherFixtureExceedsTheRefusalThreshold() throws Exception {
        Assumptions.assumeTrue(DIR.isDirectory(), "no Sqlfiles/ -- not this tree");
        int n = bindCount("wideparams260.sql");
        assertTrue(n > MAX_SIZE,
                "wideparams260.sql has " + n + " binds; it must exceed " + MAX_SIZE + " or nothing"
                        + " reaches the branch that refuses to emit a service method");
    }
}
