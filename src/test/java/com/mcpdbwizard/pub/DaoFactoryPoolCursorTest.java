package com.mcpdbwizard.pub;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The DAO factory pool's ORA-01000 policy (2.0.37+, known issue 10), without a database.
 * <p>
 * A generated server's factory keeps the statement of every DAO it has used, so a server that has
 * served enough distinct tools runs its session out of cursors, and from then on the next new tool
 * fails. The policy (David, 2026-11-12): change nothing until ORA-01000 is seen; then log it, discard
 * that factory (which disconnects its session and releases its cursors, logged), retry on a fresh one
 * (logged), and from then on release every factory's statements as it comes back, keeping its
 * connection. These tests pin each half, and that a pool which never sees ORA-01000 behaves exactly
 * as before.
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class DaoFactoryPoolCursorTest {

    private static final class StubFactory implements PooledResourceUser {
        static final AtomicInteger SERIAL = new AtomicInteger();
        final int id = SERIAL.incrementAndGet();
        boolean connected;
        boolean closed;
        int releaseStatementsCount;

        @Override
        public void confirmConnection() {
            connected = true;
        }

        @Override
        public boolean isConnectionUsable() {
            return connected && !closed;
        }

        @Override
        public void settleTransaction(boolean commit) {
        }

        @Override
        public void closeFactory() {
            closed = true;
            connected = false;
        }

        @Override
        public void releaseStatements() {
            releaseStatementsCount++;
        }

        @Override
        public boolean hasResources() {
            return connected;
        }

        @Override
        public boolean releaseResources() {
            return true;
        }
    }

    /** A factory generated before releaseStatements() existed: it keeps the interface's no-op. */
    private static final class OldFactory implements PooledResourceUser {
        boolean closed;

        @Override
        public void confirmConnection() {
        }

        @Override
        public boolean isConnectionUsable() {
            return !closed;
        }

        @Override
        public void settleTransaction(boolean commit) {
        }

        @Override
        public void closeFactory() {
            closed = true;
        }

        @Override
        public boolean hasResources() {
            return true;
        }

        @Override
        public boolean releaseResources() {
            return true;
        }
    }

    private static final class CapturingLog extends GenericLog {
        final List<String> warnings = new ArrayList<>();

        @Override
        protected void writeMessage(String messageType, String messageText, boolean isModal, boolean isLogged) {
            if (messageType != null && messageType.toLowerCase().contains("warn")) {
                warnings.add(messageText);
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public String getCurrentLog() {
            return "capture";
        }
    }

    private final List<StubFactory> made = new ArrayList<>();
    private final CapturingLog log = new CapturingLog();
    private DaoFactoryPool<StubFactory> pool;

    @AfterEach
    void close() {
        if (pool != null) {
            pool.close();
        }
    }

    private DaoFactoryPool<StubFactory> pool() {
        pool = new DaoFactoryPool<>(() -> {
            StubFactory f = new StubFactory();
            made.add(f);
            return f;
        }, new DaoFactoryPoolConfig().setMaxSize(2).setMinIdle(0).setMaxWaitMillis(2000L)
                .setIdleTimeoutMillis(60000L), log);
        return pool;
    }

    /** The error a generated DAO raises: the pub library's exception carrying Oracle's text. */
    private static CSException ora01000() {
        return new CSException("Unable to execute this statement. First Message:ORA-01000: maximum open cursors exceeded");
    }

    @Test
    void withoutOra01000NothingChanges() throws Exception {
        DaoFactoryPool<StubFactory> p = pool();
        for (int i = 0; i < 10; i++) {
            assertEquals("ok", p.withFactory(f -> "ok"));
        }
        assertFalse(p.isReleasingStatementsOnReturn());
        assertEquals(0, p.getCursorRecoveries());
        assertEquals(0, made.get(0).releaseStatementsCount, "statements are kept, as before");
        assertEquals(1, made.size(), "one factory, reused");
        assertTrue(log.warnings.isEmpty(), log.warnings.toString());
    }

    @Test
    void ora01000IsLoggedTheSessionDisconnectedAndTheCallRetriedOnANewOne() throws Exception {
        DaoFactoryPool<StubFactory> p = pool();
        p.withFactory(f -> "warm");                      // the factory that will run out
        StubFactory exhausted = made.get(0);
        AtomicInteger calls = new AtomicInteger();

        String result = p.withFactory(f -> {
            if (calls.incrementAndGet() == 1) {
                assertEquals(exhausted.id, f.id, "the first attempt runs on the exhausted factory");
                throw ora01000();
            }
            return "answered by " + f.id;
        });

        StubFactory fresh = made.get(made.size() - 1);
        assertNotSame(exhausted, fresh);
        assertEquals("answered by " + fresh.id, result, "the retry runs on a new factory");
        assertTrue(exhausted.closed, "the exhausted factory is discarded, closing its session");
        assertTrue(p.isReleasingStatementsOnReturn(), "ORA-01000 sets the flag");
        assertEquals(1, p.getCursorRecoveries());
        assertEquals(3, log.warnings.size(), log.warnings.toString());
        assertTrue(log.warnings.get(0).startsWith("ORA-01000 (maximum open cursors exceeded)"), log.warnings.get(0));
        assertTrue(log.warnings.get(1).startsWith("Disconnecting"), log.warnings.get(1));
        assertTrue(log.warnings.get(2).startsWith("Reconnecting"), log.warnings.get(2));
        assertEquals(1, fresh.releaseStatementsCount, "the retried call's factory releases on the way back");
    }

    @Test
    void afterOra01000EveryCallReleasesItsFactorysStatements() throws Exception {
        DaoFactoryPool<StubFactory> p = pool();
        AtomicInteger calls = new AtomicInteger();
        p.withFactory(f -> {
            if (calls.incrementAndGet() == 1) {
                throw ora01000();
            }
            return "x";
        });
        StubFactory current = made.get(made.size() - 1);
        int before = current.releaseStatementsCount;
        for (int i = 0; i < 5; i++) {
            p.withFactory(f -> "y");
        }
        assertEquals(before + 5, current.releaseStatementsCount, "released after each call");
        assertFalse(current.closed, "the connection is kept: no logon per call");
    }

    @Test
    void ora01000OnTheRetryToGivesUpAfterOneMoreAttempt() {
        DaoFactoryPool<StubFactory> p = pool();
        CSException e = assertThrows(CSException.class, () -> p.withFactory(f -> {
            throw ora01000();
        }));
        assertTrue(DaoFactoryPool.isCursorExhaustion(e));
        assertEquals(1, p.getCursorRecoveries(), "one retry, never a loop");
        assertTrue(log.warnings.stream().anyMatch(w -> w.contains("again")), log.warnings.toString());
    }

    @Test
    void otherDatabaseErrorsAreNotRetried() {
        DaoFactoryPool<StubFactory> p = pool();
        AtomicInteger calls = new AtomicInteger();
        assertThrows(CSException.class, () -> p.withFactory(f -> {
            calls.incrementAndGet();
            throw new CSException("ORA-00942: table or view does not exist");
        }));
        assertEquals(1, calls.get());
        assertFalse(p.isReleasingStatementsOnReturn());
        assertTrue(log.warnings.isEmpty(), log.warnings.toString());
    }

    @Test
    void aFactoryGeneratedBeforeTheFixStillRecovers() throws Exception {
        DaoFactoryPool<OldFactory> p = new DaoFactoryPool<>(OldFactory::new,
                new DaoFactoryPoolConfig().setMaxSize(2).setMinIdle(0).setMaxWaitMillis(2000L)
                        .setIdleTimeoutMillis(60000L), log);
        try {
            AtomicInteger calls = new AtomicInteger();
            assertEquals("ok", p.withFactory(f -> {
                if (calls.incrementAndGet() == 1) {
                    throw ora01000();
                }
                return "ok";
            }));
            assertEquals("ok", p.withFactory(f -> "ok"), "the default releaseStatements() is a no-op");
        } finally {
            p.close();
        }
    }

    @Test
    void ora01000IsRecognisedHoweverItArrives() {
        assertTrue(DaoFactoryPool.isCursorExhaustion(new SQLException("x", "72000", 1000)));
        assertTrue(DaoFactoryPool.isCursorExhaustion(ora01000()));
        assertFalse(DaoFactoryPool.isCursorExhaustion(new SQLException("ORA-00942", "42000", 942)));
        assertFalse(DaoFactoryPool.isCursorExhaustion(null));
    }
}
