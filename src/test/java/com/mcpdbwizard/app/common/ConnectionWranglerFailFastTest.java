package com.mcpdbwizard.app.common;

import com.mcpdbwizard.pub.CSException;
import com.mcpdbwizard.pub.ConsoleLog;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fail-fast on a lost connection, database-free.
 *
 * <p>The behaviour this pins was measured end to end on 2026-10-03 by running the real generator
 * through a TCP proxy that could freeze: before, a frozen host cost one read timeout per reconnect
 * point -- tens of minutes at the real 10-minute timeout -- and the run then exited 0 with half a
 * tree; after, one timeout and exit 3. Here the "dead" connection is a stand-in the driver reports as
 * closed, which is exactly what the Oracle driver does after a read timeout.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class ConnectionWranglerFailFastTest {

    /** A Connection whose isClosed() is true, as the driver reports after a read timeout. */
    private static Connection closedConnection() {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[] {Connection.class}, (theProxy, theMethod, theArgs) -> {
                    if (theMethod.getName().equals("isClosed")) {
                        return Boolean.TRUE;
                    }
                    throw new java.sql.SQLRecoverableException("ORA-17008: Closed connection");
                });
    }

    private static ConnectionWrangler wranglerWithDeadConnection(boolean theFailFast) throws CSException {
        ConnectionWrangler w = new ConnectionWrangler(new ConsoleLog(), "127.0.0.1");
        w.mrPort = 1;
        w.mrSid = "/NOPE";
        w.mrUser = "u";
        w.mrPassword = "p";
        w.setFailFastOnLostConnection(theFailFast);
        w.mrConnection = closedConnection();
        w.haveConnection = true;
        return w;
    }

    @Test
    void aConnectionTheDriverClosedStopsTheRunInsteadOfReconnecting() throws Exception {
        ConnectionWrangler w = wranglerWithDeadConnection(true);

        CSException theFirst = assertThrows(CSException.class, w::connect);
        assertTrue(theFirst.getMessage().contains("lost mid-run"), theFirst.getMessage());
        assertNotNull(w.getConnectionLostReason());

        // And every later attempt refuses at once -- this is what stops a second, third and fourth
        // read timeout being waited out on a host that has stopped answering.
        CSException theSecond = assertThrows(CSException.class, w::connect);
        assertTrue(theSecond.getMessage().startsWith("Not reconnecting"), theSecond.getMessage());
    }

    /**
     * Off by default, because the web console's Design session uses this class in-process and must be
     * able to connect again once a database is back. Without fail-fast nothing is recorded, and connect
     * goes on to try the network (which fails here for an unrelated reason: there is no database).
     */
    @Test
    void withoutFailFastNothingIsRecorded() throws Exception {
        ConnectionWrangler w = wranglerWithDeadConnection(false);

        CSException theFailure = assertThrows(CSException.class, w::connect);
        assertNull(w.getConnectionLostReason());
        assertTrue(!theFailure.getMessage().contains("lost mid-run"), theFailure.getMessage());
    }
}
