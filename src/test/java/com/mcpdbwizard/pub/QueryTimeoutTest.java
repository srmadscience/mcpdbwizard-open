package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the optional per-statement time cap.
 *
 * <p>Resolution is what is testable here — the cap is read once into a static, because it is consulted
 * on every statement and cannot change while the JVM runs. The behaviour that matters is that
 * anything unusable means "no cap" rather than an exception: this is read from a static initialiser,
 * so throwing would take the whole application down over a typo in an optional setting.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class QueryTimeoutTest {

    @Test
    void unsetMeansNoCap() {
        assertEquals(0, QueryTimeout.resolve(null, null));
        assertEquals(0, QueryTimeout.resolve("", "   "));
    }

    @Test
    void theEnvironmentVariableIsUsedWhenNoPropertyIsSet() {
        assertEquals(30, QueryTimeout.resolve(null, "30"));
        assertEquals(30, QueryTimeout.resolve(null, " 30 "));
    }

    @Test
    void theSystemPropertyWinsOverTheEnvironment() {
        // A deployment overriding on the command line should not have to unset the container's env.
        assertEquals(5, QueryTimeout.resolve("5", "30"));
    }

    @Test
    void anUnusableValueMeansNoCapRatherThanAnException() {
        assertEquals(0, QueryTimeout.resolve(null, "thirty"));
        assertEquals(0, QueryTimeout.resolve(null, "-1"), "negative is meaningless to JDBC");
        assertEquals(0, QueryTimeout.resolve(null, "0"), "zero already means no cap in JDBC");
    }

    @Test
    void applyToleratesANullStatement() {
        QueryTimeout.apply(null);
    }
}
