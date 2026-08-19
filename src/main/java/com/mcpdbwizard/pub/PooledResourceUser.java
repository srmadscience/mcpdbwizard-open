package com.mcpdbwizard.pub;

/**
 * What {@link DaoFactoryPool} needs from a poolable DAO factory. Generated {@code <Factory>} classes
 * implement this when the config sets {@code DAO_POOL=YES}.
 *
 * <p>The point of pooling a factory rather than a {@link java.sql.Connection} is that a factory
 * carries far more warm state than its connection: one cached instance of every DAO, each holding
 * its own parsed {@code CallableStatement}s. A connection pool would hand back a bare connection and
 * every borrower would re-parse. So {@link #settleTransaction} deliberately does <em>not</em>
 * release resources — {@link OracleResourceUser#releaseResources()} is for teardown, not for the
 * return leg of a borrow.
 *
 * <p>The cost of that choice is that a pool of N factories pins N Oracle sessions and
 * N &times; (DAOs per factory) &times; (statements per DAO) cursors. Size {@code DAO_POOL_MAX_SIZE}
 * against the server's {@code open_cursors} and {@code sessions}, not against CPU.
 *
 * <p>Implementations are <strong>not</strong> thread-safe, and the pool does not make them so — it
 * guarantees only that one borrower holds a given factory at a time. Never retain a reference to a
 * factory after returning it.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public interface PooledResourceUser extends OracleResourceUser {

    /**
     * Establish the database connection if there is not already a usable one. Called when the pool
     * creates a factory and again each time one is handed out, so an implementation must be cheap
     * and idempotent when already connected. Re-connecting here is what lets a factory whose session
     * died survive as a pool member.
     *
     * @throws CSException if no connection could be established
     */
    void confirmConnection() throws CSException;

    /**
     * Is this factory's connection still usable? Used to validate a factory before it is handed out
     * and, in the background, while it sits idle. Must never throw: a factory that cannot answer the
     * question is by definition not usable.
     *
     * @return <code>true</code> if the connection is open and the session is alive
     */
    boolean isConnectionUsable();

    /**
     * End the transaction opened by whoever just finished with this factory, so the next borrower
     * starts clean. Called on the return leg of every borrow. Does not release statements or DAOs —
     * see the class comment.
     *
     * @param commit <code>true</code> to commit the borrower's work, <code>false</code> to roll it
     *               back. The pool always passes <code>false</code> when the borrower threw.
     * @throws CSException if the transaction could not be settled, which the pool treats as grounds
     *                     to destroy this factory rather than reuse it
     */
    void settleTransaction(boolean commit) throws CSException;

    /**
     * Release everything: statements, DAOs and the connection itself. Called when the pool evicts an
     * idle factory, discards a broken one, or is closed. Never throws — a factory being destroyed
     * has nowhere to report a failure to.
     */
    void closeFactory();
}
