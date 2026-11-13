package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Known issue 10 against a real Oracle session, and the pool's ORA-01000 recovery (2.0.37+).
 * <p>
 * A generated DAO factory keeps the statement of every DAO it has used, so each distinct tool a
 * server has served holds a cursor for the life of the session. The factory here does exactly that
 * with real statements: every call runs a query no earlier call ran and keeps it open, until
 * {@link #releaseStatements()} closes them all, as the generated method does. Driven straight, it hits
 * ORA-01000 at the session's limit, which is the issue. Driven through the pool, twice the limit's
 * worth of calls must all succeed, with exactly one recovery.
 * <p>
 * Gated on a reachable database ({@link DbTestSupport}); skips without one.
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class DaoFactoryPoolCursorLiveTest {

    private static final AtomicInteger NEXT_QUERY = new AtomicInteger();

    /** Gated here, not inside a factory: the pool would wrap a skip into an error. */
    @org.junit.jupiter.api.BeforeAll
    static void needsADatabase() {
        org.junit.jupiter.api.Assumptions.assumeTrue(DbTestSupport.databaseAvailable(),
                "no Oracle database reachable - skipping the live pool cursor tests");
    }

    /** Holds every statement it has used, as a generated factory holds its DAOs. */
    private static final class CursorHoldingFactory implements PooledResourceUser {
        Connection connection;
        final List<PreparedStatement> held = new ArrayList<>();

        String runANewTool() throws CSException {
            int n = NEXT_QUERY.incrementAndGet();
            try {
                PreparedStatement ps = connection.prepareStatement("SELECT " + n + " AS v FROM dual");
                held.add(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getString(1);
                }
            } catch (SQLException e) {
                // As a generated DAO reports it: the pub library's exception with Oracle's text
                throw new CSException("Unable to execute this statement. First Message:" + e.getMessage());
            }
        }

        @Override
        public void confirmConnection() throws CSException {
            if (connection == null) {
                connection = DbTestSupport.requireConnection();
            }
        }

        @Override
        public boolean isConnectionUsable() {
            try {
                return connection != null && !connection.isClosed();
            } catch (SQLException e) {
                return false;
            }
        }

        @Override
        public void settleTransaction(boolean commit) {
        }

        @Override
        public void releaseStatements() {
            for (PreparedStatement ps : held) {
                try {
                    ps.close();
                } catch (SQLException ignored) {
                    // releasing
                }
            }
            held.clear();
        }

        @Override
        public void closeFactory() {
            releaseStatements();
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException ignored) {
                // closing
            }
            connection = null;
        }

        @Override
        public boolean hasResources() {
            return connection != null;
        }

        @Override
        public boolean releaseResources() {
            closeFactory();
            return true;
        }
    }

    /** How many cursors one session may hold here: found by holding them until Oracle refuses. */
    private static int sessionCursorLimit() throws Exception {
        CursorHoldingFactory probe = new CursorHoldingFactory();
        probe.confirmConnection();
        try {
            for (int i = 0; i < 100_000; i++) {
                try {
                    probe.runANewTool();
                } catch (CSException e) {
                    assertTrue(DaoFactoryPool.isCursorExhaustion(e), "expected ORA-01000, got " + e.getMessage());
                    return i;
                }
            }
            throw new AssertionError("no ORA-01000 after 100000 open statements");
        } finally {
            probe.closeFactory();
        }
    }

    @Test
    void withoutThePoolTheSessionRunsOutOfCursors() throws Exception {
        int limit = sessionCursorLimit();
        assertTrue(limit > 10, "the session held " + limit + " cursors before ORA-01000");
    }

    @Test
    void throughThePoolTwiceTheLimitOfNewToolsAllSucceed() throws Exception {
        int limit = sessionCursorLimit();
        DaoFactoryPool<CursorHoldingFactory> pool = new DaoFactoryPool<>(CursorHoldingFactory::new,
                new DaoFactoryPoolConfig().setMaxSize(1).setMinIdle(0).setMaxWaitMillis(30000L)
                        .setIdleTimeoutMillis(60000L), new ConsoleLog());
        try {
            for (int i = 0; i < 2 * limit; i++) {
                String v = pool.withFactory(CursorHoldingFactory::runANewTool);
                assertTrue(v != null && !v.isEmpty());
            }
            assertTrue(pool.isReleasingStatementsOnReturn(), "the limit was reached, so the flag is set");
            assertEquals(1, pool.getCursorRecoveries(), "one recovery; after it, statements are released");
        } finally {
            pool.close();
        }
    }

    @Test
    void aPoolThatNeverReachesTheLimitKeepsItsStatements() throws Exception {
        DaoFactoryPool<CursorHoldingFactory> pool = new DaoFactoryPool<>(CursorHoldingFactory::new,
                new DaoFactoryPoolConfig().setMaxSize(1).setMinIdle(0).setMaxWaitMillis(30000L)
                        .setIdleTimeoutMillis(60000L), new ConsoleLog());
        try {
            CursorHoldingFactory[] seen = new CursorHoldingFactory[1];
            for (int i = 0; i < 20; i++) {
                pool.withFactory(f -> {
                    seen[0] = f;
                    return f.runANewTool();
                });
            }
            assertEquals(20, seen[0].held.size(), "statements are kept, as before the change");
            assertEquals(0, pool.getCursorRecoveries());
        } finally {
            pool.close();
        }
    }
}
