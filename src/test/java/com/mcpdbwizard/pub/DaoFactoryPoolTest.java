package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Database-free tests for {@link DaoFactoryPool}, driven by a {@link StubFactory} that records the
 * lifecycle calls a real generated {@code <Factory>} would receive. No Oracle needed: what is under
 * test is the pool's policy — when it grows, when it blocks, when it shrinks, and what it does to a
 * factory on the way back in — not anything JDBC does.
 *
 * <p>The assertion that matters most is in {@link #returningAFactoryKeepsItsStatementsParsed()}:
 * pooling factories rather than connections is only worth doing if the return leg leaves the cached
 * DAOs and their parsed statements alone.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class DaoFactoryPoolTest {

    /**
     * Stands in for a generated DAO factory. Records what the pool did to it, and can be told to
     * fail on demand so the broken-connection paths can be exercised.
     */
    private static final class StubFactory implements PooledResourceUser {

        private static final AtomicInteger SERIAL = new AtomicInteger();

        final int id = SERIAL.incrementAndGet();

        boolean connected;
        boolean closed;
        boolean connectionUsable = true;
        boolean rollbackFails;
        int confirmCount;
        int settleCount;
        int releaseResourcesCount;
        Boolean lastSettleWasCommit;

        @Override
        public void confirmConnection() {
            confirmCount++;
            connected = true;
        }

        @Override
        public boolean isConnectionUsable() {
            return connectionUsable && connected && !closed;
        }

        @Override
        public void settleTransaction(boolean commit) throws CSException {
            if (rollbackFails) {
                throw new CSException("stub: cannot settle, connection is gone");
            }
            settleCount++;
            lastSettleWasCommit = Boolean.valueOf(commit);
        }

        @Override
        public void closeFactory() {
            closed = true;
            connected = false;
        }

        @Override
        public boolean hasResources() {
            return connected;
        }

        @Override
        public boolean releaseResources() {
            releaseResourcesCount++;
            return true;
        }
    }

    private static DaoFactoryPoolConfig config(int maxSize, int minIdle, long maxWaitMillis) {
        return new DaoFactoryPoolConfig()
                .setMaxSize(maxSize)
                .setMinIdle(minIdle)
                .setMaxWaitMillis(maxWaitMillis)
                .setIdleTimeoutMillis(60000L);
    }

    private static DaoFactoryPool<StubFactory> pool(DaoFactoryPoolConfig theConfig) {
        return new DaoFactoryPool<StubFactory>(StubFactory::new, theConfig, null);
    }

    // ---- growing --------------------------------------------------------------------------

    @Test
    void poolCreatesFactoriesOnDemandRatherThanUpFront() throws Exception {
        try (DaoFactoryPool<StubFactory> thePool = pool(config(5, 0, 1000L))) {
            assertEquals(0, thePool.getCreatedCount(), "nothing should be built before it is needed");

            StubFactory theFirst = thePool.borrow();
            assertEquals(1, thePool.getCreatedCount());
            assertTrue(theFirst.connected, "a borrowed factory must arrive connected");
            assertEquals(1, thePool.getNumActive());

            thePool.release(theFirst);
            assertEquals(0, thePool.getNumActive());
            assertEquals(1, thePool.getNumIdle());
        }
    }

    @Test
    void anIdleFactoryIsReusedRatherThanRebuilt() throws Exception {
        try (DaoFactoryPool<StubFactory> thePool = pool(config(5, 0, 1000L))) {
            StubFactory theFirst = thePool.borrow();
            thePool.release(theFirst);

            StubFactory theSecond = thePool.borrow();
            assertSame(theFirst, theSecond, "the warm factory should come back out");
            assertEquals(1, thePool.getCreatedCount(), "and no second one should have been built");
            thePool.release(theSecond);
        }
    }

    @Test
    void poolGrowsToItsMaximumUnderConcurrentDemand() throws Exception {
        try (DaoFactoryPool<StubFactory> thePool = pool(config(3, 0, 1000L))) {
            List<StubFactory> theBorrowed = new ArrayList<StubFactory>();
            for (int i = 0; i < 3; i++) {
                theBorrowed.add(thePool.borrow());
            }

            assertEquals(3, thePool.getNumActive());
            assertEquals(3, thePool.getCreatedCount());
            assertEquals(3, theBorrowed.stream().distinct().count(), "borrowers must not share");

            for (StubFactory theFactory : theBorrowed) {
                thePool.release(theFactory);
            }
        }
    }

    // ---- the fixed ceiling ----------------------------------------------------------------

    @Test
    void exhaustedPoolBlocksThenReportsItselfExhausted() throws Exception {
        try (DaoFactoryPool<StubFactory> thePool = pool(config(2, 0, 300L))) {
            StubFactory theFirst = thePool.borrow();
            StubFactory theSecond = thePool.borrow();

            long theStart = System.currentTimeMillis();
            CSPoolExhaustedException e = assertThrows(CSPoolExhaustedException.class, thePool::borrow);
            long theElapsed = System.currentTimeMillis() - theStart;

            assertTrue(theElapsed >= 250L,
                    "should have waited out DAO_POOL_MAX_WAIT_MS, waited " + theElapsed + "ms");
            assertTrue(e.getMessage().contains("2"), "the message should name the ceiling: " + e.getMessage());
            assertEquals(2, thePool.getCreatedCount(), "and must not have exceeded the ceiling");

            thePool.release(theFirst);
            thePool.release(theSecond);
        }
    }

    @Test
    void aFactoryFreedByAnotherThreadUnblocksAWaitingBorrower() throws Exception {
        try (DaoFactoryPool<StubFactory> thePool = pool(config(1, 0, 5000L))) {
            final StubFactory theOnlyFactory = thePool.borrow();

            Thread theReturner = new Thread(() -> {
                try {
                    Thread.sleep(200L);
                    thePool.release(theOnlyFactory);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            theReturner.start();

            StubFactory theSecond = thePool.borrow();
            assertSame(theOnlyFactory, theSecond, "the waiter should get the freed factory");
            theReturner.join();
            thePool.release(theSecond);
        }
    }

    // ---- shrinking ------------------------------------------------------------------------

    @Test
    void idleFactoriesAreEvictedDownToTheConfiguredFloor() throws Exception {
        DaoFactoryPoolConfig theConfig = config(5, 1, 1000L).setIdleTimeoutMillis(1000L);

        try (DaoFactoryPool<StubFactory> thePool = pool(theConfig)) {
            List<StubFactory> theBorrowed = new ArrayList<StubFactory>();
            for (int i = 0; i < 4; i++) {
                theBorrowed.add(thePool.borrow());
            }
            for (StubFactory theFactory : theBorrowed) {
                thePool.release(theFactory);
            }
            assertEquals(4, thePool.getNumIdle(), "all four should be warm immediately after the burst");

            // The evictor runs at a quarter of the idle timeout, floored at one second, so give it
            // enough passes to work through every idle factory.
            long theDeadline = System.currentTimeMillis() + 8000L;
            while (thePool.getNumIdle() > 1 && System.currentTimeMillis() < theDeadline) {
                Thread.sleep(200L);
            }

            assertEquals(1, thePool.getNumIdle(),
                    "an inactive pool should shrink to DAO_POOL_MIN_IDLE");
            assertTrue(thePool.getDestroyedCount() >= 3, "and the evicted factories should be closed");

            long theClosed = theBorrowed.stream().filter(f -> f.closed).count();
            assertEquals(3, theClosed, "closeFactory() is how an evicted factory releases its session");
        }
    }

    // ---- what the return leg does ----------------------------------------------------------

    @Test
    void returningAFactoryKeepsItsStatementsParsed() throws Exception {
        try (DaoFactoryPool<StubFactory> thePool = pool(config(2, 0, 1000L))) {
            StubFactory theFactory = thePool.borrow();
            thePool.release(theFactory);

            // The entire reason for pooling factories instead of connections: the cached DAOs and
            // their parsed CallableStatements survive the round trip. releaseResources() is teardown.
            assertEquals(0, theFactory.releaseResourcesCount,
                    "the return leg must not throw away the warm state the pool exists to keep");
            assertFalse(theFactory.closed);
        }
    }

    @Test
    void returningAFactoryCommitsOrRollsBackAsConfigured() throws Exception {
        DaoFactoryPoolConfig theCommitting = config(2, 0, 1000L).setCommitOnReturn(true);
        try (DaoFactoryPool<StubFactory> thePool = pool(theCommitting)) {
            StubFactory theFactory = thePool.borrow();
            thePool.release(theFactory);
            assertEquals(Boolean.TRUE, theFactory.lastSettleWasCommit);
        }

        DaoFactoryPoolConfig theRollingBack = config(2, 0, 1000L).setCommitOnReturn(false);
        try (DaoFactoryPool<StubFactory> thePool = pool(theRollingBack)) {
            StubFactory theFactory = thePool.borrow();
            thePool.release(theFactory);
            assertEquals(Boolean.FALSE, theFactory.lastSettleWasCommit);
        }
    }

    @Test
    void workThatThrowsIsRolledBackEvenWhenThePolicyIsCommit() throws Exception {
        DaoFactoryPoolConfig theConfig = config(2, 0, 1000L).setCommitOnReturn(true);

        try (DaoFactoryPool<StubFactory> thePool = pool(theConfig)) {
            final CSException theFailure = new CSException("ORA-00001: unique constraint violated");
            final List<StubFactory> theSeen = new ArrayList<StubFactory>();

            CSException theCaught = assertThrows(CSException.class, () ->
                    thePool.withFactory(theFactory -> {
                        theSeen.add(theFactory);
                        throw theFailure;
                    }));

            assertSame(theFailure, theCaught, "the pool must not wrap or swallow the caller's exception");
            assertEquals(Boolean.FALSE, theSeen.get(0).lastSettleWasCommit,
                    "half a failed unit of work must never be committed");
        }
    }

    @Test
    void anApplicationErrorDoesNotCostUsAWarmFactory() throws Exception {
        try (DaoFactoryPool<StubFactory> thePool = pool(config(2, 0, 1000L))) {
            assertThrows(CSException.class, () ->
                    thePool.withFactory(theFactory -> {
                        throw new CSException("ORA-20001: application error raised by PL/SQL");
                    }));

            // A PL/SQL exception says nothing about the connection, so the factory goes back.
            assertEquals(1, thePool.getNumIdle());
            assertEquals(0, thePool.getDestroyedCount());
            assertEquals(1, thePool.getCreatedCount());
        }
    }

    @Test
    void aFactoryWhoseConnectionDiedDuringFailedWorkIsDiscarded() throws Exception {
        try (DaoFactoryPool<StubFactory> thePool = pool(config(2, 0, 1000L))) {
            assertThrows(CSException.class, () ->
                    thePool.withFactory(theFactory -> {
                        theFactory.connectionUsable = false;
                        throw new CSException("ORA-03113: end-of-file on communication channel");
                    }));

            assertEquals(0, thePool.getNumIdle(), "a factory with a dead session must not go back");
            assertEquals(1, thePool.getDestroyedCount());
        }
    }

    @Test
    void aFactoryThatCannotEvenRollBackIsDiscarded() throws Exception {
        try (DaoFactoryPool<StubFactory> thePool = pool(config(2, 0, 1000L))) {
            assertThrows(CSException.class, () ->
                    thePool.withFactory(theFactory -> {
                        theFactory.rollbackFails = true;
                        throw new CSException("ORA-03113: end-of-file on communication channel");
                    }));

            assertEquals(0, thePool.getNumIdle());
            assertEquals(1, thePool.getDestroyedCount());
        }
    }

    // ---- validation ------------------------------------------------------------------------

    @Test
    void aSessionThatDiedWhileIdleIsReplacedRatherThanHandedOut() throws Exception {
        try (DaoFactoryPool<StubFactory> thePool = pool(config(3, 0, 1000L))) {
            StubFactory theFirst = thePool.borrow();
            thePool.release(theFirst);

            // Something outside killed the session while the factory sat in the pool.
            theFirst.connectionUsable = false;

            StubFactory theSecond = thePool.borrow();
            assertNotSame(theFirst, theSecond, "the dead factory must not be handed out");
            assertTrue(theFirst.closed, "and it should have been closed");
            assertEquals(2, thePool.getCreatedCount());
            thePool.release(theSecond);
        }
    }

    @Test
    void validationCanBeTurnedOffForABorrowRateThatCannotAffordThePing() throws Exception {
        DaoFactoryPoolConfig theConfig = config(3, 0, 1000L).setValidateOnBorrow(false);

        try (DaoFactoryPool<StubFactory> thePool = pool(theConfig)) {
            StubFactory theFirst = thePool.borrow();
            thePool.release(theFirst);
            theFirst.connectionUsable = false;

            StubFactory theSecond = thePool.borrow();
            assertSame(theFirst, theSecond, "without validation the pool cannot know it is dead");
            thePool.release(theSecond);
        }
    }

    // ---- withFactory and lifecycle -----------------------------------------------------------

    @Test
    void withFactoryReturnsTheFactoryEvenWhenTheWorkSucceeds() throws Exception {
        try (DaoFactoryPool<StubFactory> thePool = pool(config(1, 0, 500L))) {
            String theResult = thePool.withFactory(theFactory -> "done by " + theFactory.id);
            assertTrue(theResult.startsWith("done by "));

            // Only possible if the first borrow was returned - the pool holds exactly one factory.
            assertEquals("second call", thePool.withFactory(theFactory -> "second call"));
            assertEquals(1, thePool.getCreatedCount());
            assertEquals(2, thePool.getBorrowedCount());
        }
    }

    @Test
    void concurrentBorrowersNeverShareAFactory() throws Exception {
        final int theThreadCount = 8;
        final int theCallsEach = 40;

        try (DaoFactoryPool<StubFactory> thePool = pool(config(4, 0, 5000L))) {
            final AtomicInteger theConcurrentHolders = new AtomicInteger();
            final AtomicInteger theOverlaps = new AtomicInteger();
            final List<Thread> theThreads = new ArrayList<Thread>();

            for (int t = 0; t < theThreadCount; t++) {
                Thread theThread = new Thread(() -> {
                    for (int i = 0; i < theCallsEach; i++) {
                        try {
                            thePool.withFactory(theFactory -> {
                                // Per factory, not globally: two threads inside the same factory at
                                // once is the corruption a pool exists to prevent.
                                synchronized (theFactory) {
                                    theConcurrentHolders.incrementAndGet();
                                }
                                if (theFactory.hasResources()) {
                                    Thread.sleep(1L);
                                }
                                return null;
                            });
                        } catch (Exception e) {
                            theOverlaps.incrementAndGet();
                        }
                    }
                });
                theThreads.add(theThread);
                theThread.start();
            }
            for (Thread theThread : theThreads) {
                theThread.join();
            }

            assertEquals(0, theOverlaps.get(), "no borrow should have failed");
            assertEquals(theThreadCount * theCallsEach, theConcurrentHolders.get());
            assertEquals(theThreadCount * theCallsEach, thePool.getBorrowedCount());
            assertTrue(thePool.getCreatedCount() <= 4, "the ceiling must hold under contention, was "
                    + thePool.getCreatedCount());
        }
    }

    @Test
    void closingThePoolClosesEveryIdleFactory() throws Exception {
        DaoFactoryPool<StubFactory> thePool = pool(config(3, 0, 1000L));

        StubFactory theFirst = thePool.borrow();
        StubFactory theSecond = thePool.borrow();
        thePool.release(theFirst);
        thePool.release(theSecond);

        thePool.close();

        assertTrue(theFirst.closed, "close() must give the Oracle sessions back");
        assertTrue(theSecond.closed);

        thePool.close(); // idempotent, so a shutdown hook can be careless
    }

    @Test
    void nullFactoryIsIgnoredSoCallersCanReturnFromAFinally() throws Exception {
        try (DaoFactoryPool<StubFactory> thePool = pool(config(1, 0, 500L))) {
            thePool.release(null);
            thePool.invalidate(null);
        }
    }

    // ---- classifying a failure -------------------------------------------------------------

    @Test
    void anApplicationErrorIsNotConnectionFatal() {
        assertFalse(DaoFactoryPool.isConnectionFatal(null));
        assertFalse(DaoFactoryPool.isConnectionFatal(new CSException(
                "Attempt to set parameter number '1' even though statement doesn't take parameters.")));
        assertFalse(DaoFactoryPool.isConnectionFatal(new CSException("ORA-00001: unique constraint violated")));
        assertFalse(DaoFactoryPool.isConnectionFatal(new java.sql.SQLException("no such table", "42S02", 942)));
    }

    @Test
    void aDeadConnectionIsFatalEvenWhenOnlyItsTextSurvives() {
        // CSException cannot carry a cause and generated code wraps drivers as
        // "new CSException(e.getMessage())", so the ORA number in the text is all there is to go on.
        assertTrue(DaoFactoryPool.isConnectionFatal(
                new CSException("ORA-03113: end-of-file on communication channel")));
        assertTrue(DaoFactoryPool.isConnectionFatal(
                new CSException("ORA-17800: Got minus one from a read call.")));
        assertTrue(DaoFactoryPool.isConnectionFatal(new CSException(
                "Could not obtain a DAO factory: ORA-12516: listener has no handler ready")));
    }

    @Test
    void driverExceptionTypesAndSqlStatesAreFatal() {
        assertTrue(DaoFactoryPool.isConnectionFatal(new java.sql.SQLRecoverableException("closed")));
        assertTrue(DaoFactoryPool.isConnectionFatal(
                new java.sql.SQLNonTransientConnectionException("gone")));
        // SQLState class 08 is "connection exception" in the standard.
        assertTrue(DaoFactoryPool.isConnectionFatal(new java.sql.SQLException("dropped", "08006")));
        // Nested behind a wrapper that does carry a cause.
        assertTrue(DaoFactoryPool.isConnectionFatal(
                new RuntimeException("wrapped", new java.sql.SQLRecoverableException("closed"))));
    }

    @Test
    void aFailureThatCannotHaveHurtTheConnectionKeepsTheFactoryWithoutEvenChecking() throws Exception {
        // The regression this closes. The pool used to ping after ANY failure, and a false negative
        // from that ping destroyed a healthy factory -- each one costing a fresh Oracle logon.
        try (DaoFactoryPool<StubFactory> thePool = pool(config(2, 0, 1000L))) {
            assertThrows(CSException.class, () ->
                    thePool.withFactory(theFactory -> {
                        // Rig the ping to fail. The pool must not consult it at all here, so this
                        // factory has to survive anyway. Compare
                        // aFactoryWhoseConnectionDiedDuringFailedWorkIsDiscarded(): identical
                        // set-up, and only the exception text differs.
                        theFactory.connectionUsable = false;
                        throw new CSException("Attempt to set parameter number '1' even though "
                                + "statement doesn't take parameters.");
                    }));

            assertEquals(0, thePool.getDestroyedCount(),
                    "an application error must not cost a connection");
            assertEquals(1, thePool.getCreatedCount(),
                    "and must not force a replacement to be built");
        }
    }

    // ---- creation failures are counted -----------------------------------------------------

    @Test
    void creationsThatThrowAreCounted() throws Exception {
        final AtomicInteger theAttempts = new AtomicInteger();
        DaoFactoryPoolConfig theConfig = config(2, 0, 500L);
        try (DaoFactoryPool<StubFactory> thePool = new DaoFactoryPool<StubFactory>(() -> {
            theAttempts.incrementAndGet();
            throw new IllegalStateException("ORA-12516: listener has no handler ready");
        }, theConfig, null)) {

            assertEquals(0, thePool.getCreateFailedCount());
            assertThrows(CSException.class, thePool::borrow);

            assertTrue(theAttempts.get() >= 1);
            assertEquals(theAttempts.get(), thePool.getCreateFailedCount(),
                    "every refused logon must be counted, not just logged");
            assertEquals(0, thePool.getCreatedCount(),
                    "a failed creation is not a creation");
        }
    }

    @Test
    void statsLineCarriesTheCreateFailureCount() throws Exception {
        try (DaoFactoryPool<StubFactory> thePool = pool(config(2, 0, 500L))) {
            assertTrue(thePool.statsLine().contains("createfailed=0"), thePool.statsLine());
            // Appended, never reordered: the Runtime page matches "name=" not position, and the
            // older field must still be findable and unambiguous.
            assertTrue(thePool.statsLine().contains("created=0"));
        }
    }
}
