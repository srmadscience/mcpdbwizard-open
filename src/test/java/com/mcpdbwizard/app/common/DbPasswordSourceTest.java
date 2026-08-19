package com.mcpdbwizard.app.common;

import com.mcpdbwizard.pub.CSException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for taking the database password from the environment instead of the config.
 *
 * <p>The case that matters most is {@link #aConfigWithARealPasswordIsUntouched()}: this mechanism is
 * opt-in, and every existing config must generate exactly what it generated before. The rest pin the
 * split between the two points of use — the generator resolves the secret to connect, the emitter
 * must NOT, or the password lands back in the generated source and nothing has been gained.
 *
 * <p>{@code resolve} is only exercised here for the paths that do not need the variable to be set;
 * the JDK gives no supported way to set one for the current process, so the happy path is covered by
 * generating against a live database with {@code DB_PASS} exported.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class DbPasswordSourceTest {

    private static final String CALL = "passwordFromEnvironment()";

    // ---- recognising the sentinel ---------------------------------------

    @Test
    void aPlainPasswordDefersToNothing() {
        assertFalse(DbPasswordSource.isFromEnvironment("appschema"));
        assertFalse(DbPasswordSource.isFromEnvironment(""));
        assertFalse(DbPasswordSource.isFromEnvironment(null));
    }

    @Test
    void theSentinelIsRecognisedAloneAndInsideAUrl() {
        assertTrue(DbPasswordSource.isFromEnvironment(DbPasswordSource.SENTINEL));
        assertTrue(DbPasswordSource.isFromEnvironment(
                "jdbc:oracle:thin:appschema/" + DbPasswordSource.SENTINEL + "@//box:1521/FREEPDB1"),
                "the Oracle hard-coded type carries the password inside the URL, not beside it");
    }

    // ---- emission: the half that must NOT resolve ------------------------

    @Test
    void aConfigWithARealPasswordIsUntouched() {
        // Byte-identical output for every config that predates this feature.
        assertEquals("\"appschema\"", DbPasswordSource.toJavaExpression("appschema", CALL));
        assertEquals("\"jdbc:oracle:thin:appschema/appschema@//box:1521/FREEPDB1\"",
                DbPasswordSource.toJavaExpression("jdbc:oracle:thin:appschema/appschema@//box:1521/FREEPDB1", CALL));
    }

    @Test
    void aBarePasswordBecomesTheCallAlone() {
        assertEquals(CALL, DbPasswordSource.toJavaExpression(DbPasswordSource.SENTINEL, CALL),
                "no empty string literals either side");
    }

    @Test
    void aUrlBecomesAConcatenationAroundTheCall() {
        String theUrl = "jdbc:oracle:thin:appschema/" + DbPasswordSource.SENTINEL + "@//box:1521/FREEPDB1";

        assertEquals("\"jdbc:oracle:thin:appschema/\" + " + CALL + " + \"@//box:1521/FREEPDB1\"",
                DbPasswordSource.toJavaExpression(theUrl, CALL));
    }

    @Test
    void theEmittedExpressionNeverContainsTheSecretOrTheSentinel() {
        // The whole point: what is written to disk names the variable, it does not hold the value.
        String theExpression = DbPasswordSource.toJavaExpression(
                "jdbc:oracle:thin:appschema/" + DbPasswordSource.SENTINEL + "@//box:1521/FREEPDB1", CALL);

        assertFalse(theExpression.contains(DbPasswordSource.SENTINEL),
                "the sentinel is replaced by a call, not carried through into the source");
        assertTrue(theExpression.contains(CALL));
    }

    @Test
    void aTrailingSentinelDoesNotProduceADanglingConcatenation() {
        assertEquals("\"jdbc:oracle:thin:appschema/\" + " + CALL,
                DbPasswordSource.toJavaExpression("jdbc:oracle:thin:appschema/" + DbPasswordSource.SENTINEL, CALL));
    }

    @Test
    void quotesAndBackslashesInTheLiteralPartsAreEscaped() {
        // A JDBC URL should contain neither, but emitting source that does not compile would be a
        // far worse failure than the one being guarded against.
        String theExpression = DbPasswordSource.toJavaExpression(
                "a\"b\\c" + DbPasswordSource.SENTINEL, CALL);

        assertEquals("\"a\\\"b\\\\c\" + " + CALL, theExpression);
    }

    // ---- resolution: the half that must ----------------------------------

    @Test
    void resolvingLeavesARealPasswordAlone() throws CSException {
        assertEquals("appschema", DbPasswordSource.resolve("appschema"));
        assertEquals(null, DbPasswordSource.resolve(null));
    }

    @Test
    void resolvingFailsClosedWhenTheVariableIsNotSet() {
        // Fail-closed, matching MCP_HTTP_TOKEN and the TLS keystore. A blank password would other-
        // wise reach the driver and come back as a login failure that blames the database.
        CSException theException = assertThrows(CSException.class,
                () -> DbPasswordSource.resolve(DbPasswordSource.SENTINEL));

        assertTrue(theException.getMessage().contains(DbPasswordSource.ENVIRONMENT_VARIABLE),
                "the message must name the variable to set, got: " + theException.getMessage());
    }
}
