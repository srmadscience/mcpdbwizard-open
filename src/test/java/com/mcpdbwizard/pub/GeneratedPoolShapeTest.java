package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compiles and runs the <em>shape</em> of the class
 * {@code SAAdminWrangler.generateDaoPoolClass} emits, without needing a database or a generated
 * tree.
 *
 * <p>Proving a generated class compiles normally means regenerating against a live Oracle box. This
 * test cannot replace that — it knows nothing about the real DAO fields — but it does pin the parts
 * of the emitted shape that are easy to get wrong and that fail at <em>compile</em> time, where a
 * broken generator is most expensive to discover:
 *
 * <ul>
 *   <li>a static field initialiser referenced from the {@code super(...)} call, which depends on
 *       class initialisation running before construction;</li>
 *   <li>a static method reference as the pool's supplier, taken on the class currently being
 *       constructed;</li>
 *   <li>{@code extends com.mcpdbwizard.pub.DaoFactoryPool<T>} where {@code T} gets
 *       {@code PooledResourceUser} by inheritance rather than by declaring it — which is how the
 *       generated web-service impl gets it, since it extends the factory;</li>
 *   <li>the pooled class sharing its simple name with something in this package.</li>
 * </ul>
 *
 * <p>If the emission changes shape, this test has to change with it — that is the point.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class GeneratedPoolShapeTest {

    /** Stands in for the generated {@code <Factory>} once it implements the pooling contract. */
    static class StubFactory implements PooledResourceUser {

        public LogInterface theLog = new ConsoleLog();

        boolean connected;
        boolean closed;
        boolean releasedPerCall = true;

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
        public boolean hasResources() {
            return connected;
        }

        @Override
        public boolean releaseResources() {
            return true;
        }

        public void setAlwaysReleaseResourcesFlag(boolean alwaysRelease) {
            this.releasedPerCall = alwaysRelease;
        }
    }

    /**
     * Stands in for the generated {@code <WsImpl>}, which extends the factory — so it picks up
     * {@link PooledResourceUser} without naming it, exactly as the generated one does.
     */
    static class StubServiceImpl extends StubFactory {
    }

    /**
     * The emitted {@code <Factory>Pool}, transcribed. Note it extends a class whose simple name it
     * would share if the factory were called {@code DaoFactory}; the generator always writes the
     * superclass in full for that reason.
     */
    static class StubFactoryPool extends DaoFactoryPool<StubServiceImpl> {

        private static final LogInterface POOL_LOG = new StubServiceImpl().theLog;

        public StubFactoryPool() {
            super(StubFactoryPool::newPooledObject, buildConfig(), POOL_LOG);
        }

        protected static DaoFactoryPoolConfig buildConfig() {
            return new DaoFactoryPoolConfig()
                    .setMaxSize(4)
                    .setMinIdle(0)
                    .setMaxWaitMillis(1000L)
                    .setIdleTimeoutMillis(300000L)
                    .setCommitOnReturn(true)
                    .applyOverrides();
        }

        protected static StubServiceImpl newPooledObject() {
            StubServiceImpl theNewObject = new StubServiceImpl();
            theNewObject.setAlwaysReleaseResourcesFlag(false);
            return theNewObject;
        }
    }

    @Test
    void theEmittedShapeConstructsAndServes() throws Exception {
        try (StubFactoryPool thePool = new StubFactoryPool()) {
            assertEquals(4, thePool.getMaxSize(), "the baked-in size should reach the pool");

            String theResult = thePool.withFactory(theFactory -> {
                assertTrue(theFactory.connected, "the pool connects a member before handing it out");
                return "served";
            });

            assertEquals("served", theResult);
            assertEquals(1, thePool.getNumIdle());
        }
    }

    @Test
    void perCallReleaseIsTurnedOffOnEveryPoolMember() throws Exception {
        // Under pooling the pool owns the object's lifetime; releasing after every call would throw
        // away the parsed statements that make a pooled factory worth keeping.
        try (StubFactoryPool thePool = new StubFactoryPool()) {
            StubServiceImpl theBorrowed = thePool.borrow();

            assertTrue(!theBorrowed.releasedPerCall,
                    "newPooledObject() must clear the always-release flag");
            thePool.release(theBorrowed);
        }
    }

    @Test
    void aServiceImplPoolMemberIsAlsoTheFactory() throws Exception {
        // Why no separate holder class pairing a factory with a service impl: one object is both.
        try (StubFactoryPool thePool = new StubFactoryPool()) {
            StubServiceImpl theBorrowed = thePool.borrow();

            StubFactory theSameObjectAsAFactory = theBorrowed;
            assertSame(theBorrowed, theSameObjectAsAFactory);
            assertNotNull(theBorrowed.theLog);
            thePool.release(theBorrowed);
        }
    }

    @Test
    void closingThePoolClosesItsMembers() throws Exception {
        StubFactoryPool thePool = new StubFactoryPool();
        StubServiceImpl theBorrowed = thePool.borrow();
        thePool.release(theBorrowed);

        thePool.close();

        assertTrue(theBorrowed.closed, "close() must give the Oracle sessions back");
    }
}
