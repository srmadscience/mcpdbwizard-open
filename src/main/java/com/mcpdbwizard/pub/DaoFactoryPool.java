package com.mcpdbwizard.pub;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientConnectionException;
import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * A pool of warm DAO factories: grows on demand to {@code DAO_POOL_MAX_SIZE}, makes borrowers wait
 * when saturated, and shrinks back to {@code DAO_POOL_MIN_IDLE} once traffic stops.
 *
 * <p>Wraps Apache Commons Pool 2. That dependency is <em>optional</em> in this module — nothing here
 * is loaded unless generated code was built with {@code DAO_POOL=YES}, so a deployment that does not
 * pool never needs the jar.
 *
 * <p>Normal use is {@link #withFactory}, which borrows, runs, and returns in one call:
 *
 * <pre>
 *   String theJson = thePool.withFactory(theFactory -&gt; theFactory.getFooTableDAO().get(id).toJson());
 * </pre>
 *
 * <p>{@link #borrow()} and {@link #release} are exposed for callers whose control flow will not fit
 * that shape, but they must be paired in a {@code finally} — a factory that is never returned is a
 * leaked Oracle session, and the pool cannot reclaim it.
 *
 * <h2>What happens on the return leg</h2>
 * <ul>
 *   <li>The borrower's transaction is settled — committed or rolled back per
 *       {@link DaoFactoryPoolConfig#isCommitOnReturn()}, but <strong>always rolled back if the
 *       borrower threw</strong>. Committing half a failed unit of work would be worse than either
 *       policy.</li>
 *   <li>Statements and DAOs are <strong>not</strong> released. Keeping them parsed is the entire
 *       reason to pool factories instead of connections; see {@link PooledResourceUser}.</li>
 *   <li>A factory whose connection has gone bad is destroyed rather than returned. An application
 *       error — a PL/SQL exception, a constraint violation — leaves the connection perfectly usable,
 *       so it is not grounds for discarding a warm factory.</li>
 * </ul>
 *
 * <p>This class is thread-safe. The factories it hands out are not, which is what the pool is for:
 * exactly one borrower holds a given factory at a time.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class DaoFactoryPool<T extends PooledResourceUser> implements AutoCloseable {

    /**
     * A unit of work to run against a borrowed factory.
     *
     * @param <T> the pooled factory type
     * @param <R> what the work returns
     */
    public interface PoolTask<T, R> {
        R run(T theFactory) throws Exception;
    }

    private final GenericObjectPool<T> thePool;
    private final DaoFactoryPoolConfig theConfig;
    private final LogInterface theLog;

    /**
     * Creations that threw. Commons Pool counts only the ones that succeeded, so without this a
     * server storming a refusing database looks idle: {@code created} stops climbing precisely
     * because every attempt is now failing.
     */
    private final AtomicLong theCreateFailures = new AtomicLong();

    /**
     * @param theSupplier creates a fresh, unconnected factory — typically
     *                    {@code () -> new DaoFactory(theLog)}
     * @param theConfig   sizing and lifetime settings; validated here
     * @param theLog      where the pool reports evictions and discarded factories. May be
     *                    <code>null</code> for silence.
     */
    public DaoFactoryPool(Supplier<T> theSupplier, DaoFactoryPoolConfig theConfig, LogInterface theLog) {
        if (theSupplier == null) {
            throw new IllegalArgumentException("A DaoFactoryPool needs a supplier of factories");
        }
        this.theConfig = (theConfig == null ? new DaoFactoryPoolConfig() : theConfig).validateSettings();
        this.theLog = theLog;
        this.thePool = new GenericObjectPool<T>(new FactoryLifecycle(theSupplier),
                buildPoolConfig(this.theConfig));
        debug("DaoFactoryPool - started with " + this.theConfig);
    }

    private static <T> GenericObjectPoolConfig<T> buildPoolConfig(DaoFactoryPoolConfig theConfig) {
        GenericObjectPoolConfig<T> thePoolConfig = new GenericObjectPoolConfig<T>();

        thePoolConfig.setMaxTotal(theConfig.getMaxSize());
        thePoolConfig.setMinIdle(theConfig.getMinIdle());

        // Commons Pool defaults maxIdle to 8 and destroys anything returned above it. With a
        // maxSize above 8 that would throw away warm factories the moment a burst subsided, which is
        // precisely what the idle timeout is supposed to decide. Shrinking is the evictor's job.
        thePoolConfig.setMaxIdle(theConfig.getMaxSize());

        thePoolConfig.setBlockWhenExhausted(true);
        thePoolConfig.setMaxWait(Duration.ofMillis(theConfig.getMaxWaitMillis()));
        thePoolConfig.setTestOnBorrow(theConfig.isValidateOnBorrow());

        // Shrink when inactive. softMinEvictableIdleDuration respects minIdle where
        // minEvictableIdleDuration would evict straight past it; leaving the hard one disabled is
        // what makes DAO_POOL_MIN_IDLE mean "keep this many warm".
        thePoolConfig.setTimeBetweenEvictionRuns(evictionInterval(theConfig.getIdleTimeoutMillis()));
        thePoolConfig.setSoftMinEvictableIdleDuration(Duration.ofMillis(theConfig.getIdleTimeoutMillis()));
        thePoolConfig.setMinEvictableIdleDuration(Duration.ofMillis(-1));

        // Default is 3 per run, so a pool that grew to 40 would take many passes to shrink.
        // Negative means "all idle objects this run".
        thePoolConfig.setNumTestsPerEvictionRun(-1);

        // The evictor is also where a session killed while idle gets noticed.
        thePoolConfig.setTestWhileIdle(true);

        // Registering MBeans per pool leaks names across restarts and buys a generated server
        // nothing; stats are on this class instead.
        thePoolConfig.setJmxEnabled(false);

        return thePoolConfig;
    }

    /**
     * Run the evictor often enough that a factory is closed reasonably soon after its idle timeout,
     * without waking up constantly on a long timeout. Quarter of the timeout, clamped to [1s, 30s].
     */
    private static Duration evictionInterval(long idleTimeoutMillis) {
        long theInterval = idleTimeoutMillis / 4;
        if (theInterval < 1000L) {
            theInterval = 1000L;
        }
        if (theInterval > 30000L) {
            theInterval = 30000L;
        }
        return Duration.ofMillis(theInterval);
    }

    /**
     * Borrow a factory, run the work against it, and return it — the safe shape, because the return
     * happens in a {@code finally}.
     *
     * <p>Exceptions from the work propagate unchanged, so callers keep whatever
     * {@link CSException} handling they already had.
     *
     * @param theTask the work to run
     * @return whatever the work returned
     * @throws CSPoolExhaustedException if no factory became free within {@code DAO_POOL_MAX_WAIT_MS}
     * @throws Exception                whatever the work threw
     */
    public <R> R withFactory(PoolTask<T, R> theTask) throws Exception {
        T theFactory = borrow();
        Exception theFailure = null;
        try {
            return theTask.run(theFactory);
        } catch (Exception e) {
            theFailure = e;
            throw e;
        } finally {
            if (theFailure != null) {
                releaseFailedWork(theFactory, theFailure);
            } else {
                release(theFactory);
            }
        }
    }

    /**
     * Take a factory out of the pool, creating one if the pool is below its maximum and none is
     * free. <strong>Must</strong> be paired with {@link #release} in a {@code finally}.
     *
     * @return a connected, validated factory owned exclusively by the caller until it is returned
     * @throws CSPoolExhaustedException if no factory became free within {@code DAO_POOL_MAX_WAIT_MS}
     * @throws CSException              if a new factory could not be created or connected
     */
    public T borrow() throws CSException {
        try {
            return thePool.borrowObject();
        } catch (NoSuchElementException e) {
            // Commons Pool uses this both for a wait timeout and for an exhausted non-blocking pool.
            throw new CSPoolExhaustedException("All " + theConfig.getMaxSize()
                    + " DAO factories are in use and none became free within "
                    + theConfig.getMaxWaitMillis() + "ms");
        } catch (CSException e) {
            // Thrown by the factory's own confirmConnection() through makeObject/activateObject.
            throw e;
        } catch (Exception e) {
            // CSException carries no cause, so wrapping alone would discard the stack and leave the
            // real fault undiagnosable - which is exactly what happened to an intermittent
            // ArrayIndexOutOfBoundsException seen while creating a factory under load. Log the
            // throwable first, then wrap.
            if (theLog != null) {
                theLog.error("Could not obtain a DAO factory from the pool: " + stackTraceOf(e));
            }
            throw new CSException("Could not obtain a DAO factory from the pool: " + e);
        }
    }

    /**
     * Give a factory back after successful work. Settles the transaction per the configured policy
     * and keeps the factory's parsed statements for the next borrower.
     *
     * @param theFactory a factory previously returned by {@link #borrow()}; <code>null</code> is
     *                   ignored so callers can return from a {@code finally} without a null check
     */
    public void release(T theFactory) {
        settleAndReturn(theFactory, theConfig.isCommitOnReturn(), false);
    }

    /**
     * Give a factory back after the work threw: always roll back, then re-check the connection only
     * if the failure was the kind that could have broken it.
     *
     * <p>Deciding that from the exception rather than from "something threw" is the whole point.
     * Re-checking after an application error costs a round trip and, worse, a false negative there
     * destroys a healthy factory — and each destroyed factory is a fresh Oracle logon, which adds
     * the load that makes the next check likelier to time out. That loop is not hypothetical: a
     * broken tool once drove a pool from 2 connections to 125 and exhausted the instance's
     * {@code processes} limit.
     *
     * @param theFailure what the borrower threw; decides whether the connection is suspect
     */
    private void releaseFailedWork(T theFactory, Throwable theFailure) {
        settleAndReturn(theFactory, false, isConnectionFatal(theFailure));
    }

    /**
     * The one place a borrow is settled. Deliberately not in {@code passivateObject}: the pool's
     * passivation hook cannot be told that <em>this</em> return follows a failure, so settling there
     * would commit on top of the rollback the failure path had just done — a wasted round trip that
     * also leaves "commit" as the last thing recorded against a failed call.
     *
     * @param commit             whether to commit the borrower's work
     * @param connectionSuspect  whether the failure was connection-fatal, in which case the
     *                           connection is worth re-checking before the factory goes back. An
     *                           application error leaves it perfectly usable and must not set this
     */
    private void settleAndReturn(T theFactory, boolean commit, boolean connectionSuspect) {
        if (theFactory == null) {
            return;
        }

        try {
            theFactory.settleTransaction(commit);
        } catch (Exception e) {
            // Could not even end the transaction: the connection is gone, so do not put it back.
            debug("DaoFactoryPool - could not settle a borrow, discarding the factory: " + e);
            invalidate(theFactory);
            return;
        }

        // On the success path the settle above was itself a round trip, so the session has just
        // proved it is alive and there is nothing to re-check. An application failure is treated the
        // same way: it never touched the connection's health.
        if (connectionSuspect && !theFactory.isConnectionUsable()) {
            debug("DaoFactoryPool - connection unusable after a failed call, discarding the factory");
            invalidate(theFactory);
            return;
        }

        try {
            thePool.returnObject(theFactory);
        } catch (Exception e) {
            debug("DaoFactoryPool - could not return a factory, it has been discarded: " + e);
        }
    }

    /**
     * Discard a borrowed factory instead of returning it, freeing its slot. For callers using
     * {@link #borrow()} directly who know the factory is no longer sound.
     *
     * @param theFactory a factory previously returned by {@link #borrow()}
     */
    public void invalidate(T theFactory) {
        if (theFactory == null) {
            return;
        }
        try {
            thePool.invalidateObject(theFactory);
        } catch (Exception e) {
            debug("DaoFactoryPool - could not invalidate a factory: " + e);
        }
    }

    /** Factories currently checked out. */
    public int getNumActive() {
        return thePool.getNumActive();
    }

    /** Factories currently sitting in the pool, connected and warm. */
    public int getNumIdle() {
        return thePool.getNumIdle();
    }

    /** The configured ceiling, so a status page can show "3 of 10". */
    public int getMaxSize() {
        return theConfig.getMaxSize();
    }

    /**
     * Creations that threw since start-up — a refused or failed logon, not a destroyed factory.
     * Climbing while {@link #getCreatedCount()} is flat means the database is turning us away.
     */
    public long getCreateFailedCount() {
        return theCreateFailures.get();
    }

    /** Total borrows since startup. */
    public long getBorrowedCount() {
        return thePool.getBorrowedCount();
    }

    /** Factories created since startup — how often the pool had to grow. */
    public long getCreatedCount() {
        return thePool.getCreatedCount();
    }

    /** Factories closed since startup: evicted when idle, or discarded when broken. */
    public long getDestroyedCount() {
        return thePool.getDestroyedCount();
    }

    /** The settings this pool is running with. */
    public DaoFactoryPoolConfig getConfig() {
        return theConfig;
    }

    /**
     * Marks a line of {@link #statsLine()} output. Public because it is a wire format between a
     * generated server and whatever reads its log — the web module's Runtime page parses these — so
     * both ends must agree on it in one place rather than by two matching string literals.
     */
    public static final String STATS_PREFIX = "POOL-STATS";

    /**
     * A one-line, machine-readable snapshot of this pool, for a generated server to log
     * periodically:
     *
     * <pre>POOL-STATS active=1 idle=3 max=4 borrowed=1201 created=4 destroyed=0 createfailed=0</pre>
     *
     * <p>Fields are only ever APPENDED. The reader matches {@code name=} rather than position, so an
     * older Runtime page ignores a field it does not know instead of failing to parse the line.
     *
     * <p>The log is the only channel available to a reader: a generated MCP server runs as its own
     * process, so nothing outside it can call the getters above. Logging is also why the format is
     * flat and fixed rather than JSON — it has to survive being read back out of a text log tail.
     */
    public String statsLine() {
        return STATS_PREFIX
                + " active=" + getNumActive()
                + " idle=" + getNumIdle()
                + " max=" + getMaxSize()
                + " borrowed=" + getBorrowedCount()
                + " created=" + getCreatedCount()
                + " destroyed=" + getDestroyedCount()
                + " createfailed=" + getCreateFailedCount();
    }

    /**
     * Close every factory, connected or idle, and stop the evictor. Borrowed factories are closed as
     * and when they are returned. Idempotent, so it is safe in a shutdown hook.
     */
    @Override
    public void close() {
        debug("DaoFactoryPool - closing (" + getNumActive() + " active, " + getNumIdle() + " idle)");
        thePool.close();
    }

    /**
     * Could this failure have broken the connection the work ran on?
     *
     * <p><strong>False is the important answer.</strong> A constraint violation, a PL/SQL exception,
     * a bad bind count — these leave the session perfectly healthy, and discarding a warm factory
     * over one is pure cost. Only a genuine transport or session failure justifies it.
     *
     * <p><strong>The message has to be read, not just the exception type.</strong>
     * {@link CSException} has no cause-carrying constructor, and generated DAO code wraps a driver
     * failure as {@code throw new CSException(e.getMessage())} — so a dropped connection arrives
     * here as a plain {@code CSException} whose text is all that survives. Classifying on type
     * alone would call every real Oracle failure harmless.
     *
     * <p>Our own pre-flight failures (a bad bind count, say) carry no ORA code at all, which is
     * what separates them. A factory wrongly kept is not dangerous either way: the pool validates
     * while idle and, when configured, on borrow, and {@code activateObject} reconnects a session
     * that died. A factory wrongly destroyed costs a logon, which is the expensive mistake.
     *
     * @param theFailure what the work threw; null counts as not fatal
     * @return true only if the connection itself is suspect
     */
    public static boolean isConnectionFatal(Throwable theFailure) {
        for (Throwable theCause = theFailure; theCause != null; theCause = theCause.getCause()) {
            if (theCause instanceof SQLRecoverableException
                    || theCause instanceof SQLNonTransientConnectionException
                    || theCause instanceof SQLTransientConnectionException) {
                return true;
            }
            if (theCause instanceof SQLException && isFatalSqlException((SQLException) theCause)) {
                return true;
            }
            if (namesAFatalOracleError(theCause.getMessage())) {
                return true;
            }
            if (theCause.getCause() == theCause) {
                // Self-referential cause chains exist in the wild; do not spin on one.
                break;
            }
        }
        return false;
    }

    /**
     * Does this message quote an ORA number that means the session or transport is gone? Reads
     * every {@code ORA-} in the text, because a wrapped message often carries more than one.
     *
     * @param theMessage exception text; null or ORA-free counts as not fatal
     * @return true if any quoted ORA number is in the fatal set
     */
    private static boolean namesAFatalOracleError(String theMessage) {
        if (theMessage == null) {
            return false;
        }
        int theAt = theMessage.indexOf("ORA-");
        while (theAt >= 0) {
            int theFrom = theAt + 4;
            int theTo = theFrom;
            while (theTo < theMessage.length() && Character.isDigit(theMessage.charAt(theTo))) {
                theTo++;
            }
            if (theTo > theFrom) {
                try {
                    // Quoted form is zero-padded ("ORA-03113"); parseInt drops the padding.
                    if (isFatalOracleCode(Integer.parseInt(theMessage.substring(theFrom, theTo)))) {
                        return true;
                    }
                } catch (NumberFormatException e) {
                    // A run of digits too long for an int is not an Oracle error number.
                }
            }
            theAt = theMessage.indexOf("ORA-", theTo);
        }
        return false;
    }

    /**
     * SQLState class 08 is "connection exception" in the standard, which covers most drivers. The
     * Oracle codes are the ones that mean the session or the transport is gone rather than that the
     * statement was wrong.
     */
    private static boolean isFatalSqlException(SQLException theException) {
        String theState = theException.getSQLState();
        if (theState != null && theState.startsWith("08")) {
            return true;
        }
        return isFatalOracleCode(theException.getErrorCode());
    }

    /** The one list of Oracle error numbers that mean the connection, not the statement, is gone. */
    private static boolean isFatalOracleCode(int theErrorCode) {
        switch (theErrorCode) {
            case 28:      // your session has been killed
            case 1012:    // not logged on
            case 1041:    // internal error, hostdef extension doesn't exist
            case 1089:    // immediate shutdown in progress
            case 3113:    // end-of-file on communication channel
            case 3114:    // not connected to ORACLE
            case 12152:   // TNS: unable to send break message
            case 12514:   // listener does not currently know of service
            case 12516:   // listener has no handler ready -- the exhaustion case
            case 12537:   // TNS: connection closed
            case 12571:   // TNS: packet writer failure
            case 17002:   // IO error
            case 17008:   // closed connection
            case 17410:   // no more data to read from socket
            case 17800:   // got minus one from a read call
                return true;
            default:
                return false;
        }
    }

    /** Full stack of a throwable, because CSException cannot carry a cause. */
    private static String stackTraceOf(Throwable theThrowable) {
        java.io.StringWriter theWriter = new java.io.StringWriter();
        theThrowable.printStackTrace(new java.io.PrintWriter(theWriter));
        return theWriter.toString();
    }

    private void debug(String theMessage) {
        if (theLog != null) {
            theLog.debug(theMessage);
        }
    }

    /**
     * Maps the pool's lifecycle callbacks onto {@link PooledResourceUser}. Kept private: the mapping
     * is the whole contract of this class and nothing outside should be able to vary it.
     */
    private class FactoryLifecycle implements PooledObjectFactory<T> {

        private final Supplier<T> theSupplier;

        FactoryLifecycle(Supplier<T> theSupplier) {
            this.theSupplier = theSupplier;
        }

        @Override
        public PooledObject<T> makeObject() throws Exception {
            try {
                T theFactory = theSupplier.get();
                if (theFactory == null) {
                    throw new CSException("The DAO factory supplier returned null");
                }
                theFactory.confirmConnection();
                debug("DaoFactoryPool - created a factory (" + (getCreatedCount() + 1) + " so far)");
                return new DefaultPooledObject<T>(theFactory);
            } catch (Exception e) {
                // Counted, not swallowed. A refused logon is invisible in created/destroyed --
                // both simply stop moving -- so without this the loudest symptom of a database
                // that has stopped accepting connections exists only as a log line.
                theCreateFailures.incrementAndGet();
                throw e;
            }
        }

        /**
         * Run before each hand-out. {@code confirmConnection()} is a no-op when connected and
         * re-connects when the session died, so a factory whose connection dropped while idle
         * repairs itself here rather than failing the borrower's call.
         */
        @Override
        public void activateObject(PooledObject<T> thePooledObject) throws Exception {
            thePooledObject.getObject().confirmConnection();
        }

        /**
         * Nothing to do. Settling the borrower's transaction happens in
         * {@link DaoFactoryPool#settleAndReturn}, which — unlike this hook — knows whether the
         * borrower succeeded. Releasing statements or DAOs here would throw away the warm state the
         * pool exists to keep.
         */
        @Override
        public void passivateObject(PooledObject<T> thePooledObject) {
        }

        @Override
        public boolean validateObject(PooledObject<T> thePooledObject) {
            return thePooledObject.getObject().isConnectionUsable();
        }

        @Override
        public void destroyObject(PooledObject<T> thePooledObject) {
            thePooledObject.getObject().closeFactory();
        }
    }
}
