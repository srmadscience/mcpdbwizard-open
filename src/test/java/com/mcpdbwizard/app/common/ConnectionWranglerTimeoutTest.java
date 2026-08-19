package com.mcpdbwizard.app.common;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Database-free tests for the socket timeouts the generator puts on its own Oracle connection.
 *
 * <p>The behaviour being pinned is what a dropped connection does. Without a read timeout the driver
 * blocks in a socket read that never returns — observed for real as a thirty-minute hang on 2.6
 * seconds of CPU, with no session on the server at all. These assertions are about the properties
 * because that is the only part testable without staging a half-open TCP connection.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class ConnectionWranglerTimeoutTest {

    private static final String DRIVER_READ = "oracle.jdbc.ReadTimeout";
    private static final String DRIVER_CONNECT = "oracle.net.CONNECT_TIMEOUT";

    /** Run something with system properties set, clearing them again whatever happens. */
    private static void withProperties(String[] theNamesAndValues, Runnable theWork) {
        try {
            for (int i = 0; i < theNamesAndValues.length; i += 2) {
                System.setProperty(theNamesAndValues[i], theNamesAndValues[i + 1]);
            }
            theWork.run();
        } finally {
            for (int i = 0; i < theNamesAndValues.length; i += 2) {
                System.clearProperty(theNamesAndValues[i]);
            }
        }
    }

    @Test
    void aConnectionGetsATimeoutWithoutAnyoneAskingForOne() {
        Properties theProperties = ConnectionWrangler.oracleConnectionProperties("scott", "tiger");

        assertEquals("scott", theProperties.getProperty("user"));
        assertEquals("tiger", theProperties.getProperty("password"));
        assertEquals(Long.toString(ConnectionWrangler.DEFAULT_READ_TIMEOUT_MS),
                theProperties.getProperty(DRIVER_READ),
                "a generation run must not be able to hang forever on a dead socket");
        assertEquals(Long.toString(ConnectionWrangler.DEFAULT_CONNECT_TIMEOUT_MS),
                theProperties.getProperty(DRIVER_CONNECT));
    }

    @Test
    void theDefaultLeavesRoomForARealDictionaryQuery() {
        // The live harness fails a single dictionary statement at 90s. A read timeout below that
        // would turn a merely slow box into a broken one, which is worse than the hang it replaces.
        assertFalse(ConnectionWrangler.DEFAULT_READ_TIMEOUT_MS <= 90000L,
                "read timeout must exceed the 90s the harness allows one dictionary statement");
    }

    @Test
    void aSlowBoxCanRaiseIt() {
        withProperties(new String[]{ConnectionWrangler.READ_TIMEOUT_PROPERTY, "1800000"}, () ->
                assertEquals("1800000",
                        ConnectionWrangler.oracleConnectionProperties("u", "p").getProperty(DRIVER_READ)));
    }

    @Test
    void zeroRestoresTheOldWaitForeverBehaviour() {
        withProperties(new String[]{ConnectionWrangler.READ_TIMEOUT_PROPERTY, "0"}, () ->
                assertNull(ConnectionWrangler.oracleConnectionProperties("u", "p").getProperty(DRIVER_READ),
                        "0 must mean no timeout at all, not a timeout of zero"));
    }

    @Test
    void anExplicitDriverSettingIsNotOverridden() {
        // Someone who has tuned the driver directly should keep what they set.
        withProperties(new String[]{DRIVER_READ, "12345"}, () ->
                assertNull(ConnectionWrangler.oracleConnectionProperties("u", "p").getProperty(DRIVER_READ),
                        "the JVM's own -Doracle.jdbc.ReadTimeout must win, so we must not set ours"));
    }

    @Test
    void anUnparseableOverrideFallsBackRatherThanStoppingGeneration() {
        // A wrong timeout is a far smaller problem than refusing to generate anything.
        withProperties(new String[]{ConnectionWrangler.READ_TIMEOUT_PROPERTY, "ages"}, () ->
                assertEquals(Long.toString(ConnectionWrangler.DEFAULT_READ_TIMEOUT_MS),
                        ConnectionWrangler.oracleConnectionProperties("u", "p").getProperty(DRIVER_READ)));
    }

    @Test
    void nullCredentialsDoNotProduceNullProperties() {
        // Properties.setProperty(k, null) throws; the OCI path can arrive with either unset.
        Properties theProperties = ConnectionWrangler.oracleConnectionProperties(null, null);

        assertNull(theProperties.getProperty("user"));
        assertNull(theProperties.getProperty("password"));
        assertEquals(Long.toString(ConnectionWrangler.DEFAULT_READ_TIMEOUT_MS),
                theProperties.getProperty(DRIVER_READ));
    }
}
