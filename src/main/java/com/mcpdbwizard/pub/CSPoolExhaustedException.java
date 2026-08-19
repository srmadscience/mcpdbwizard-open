package com.mcpdbwizard.pub;

/**
 * Thrown when {@link DaoFactoryPool} could not supply a factory within
 * {@code DAO_POOL_MAX_WAIT_MS}: every factory is checked out and the pool is at
 * {@code DAO_POOL_MAX_SIZE}.
 *
 * <p>Distinct from a plain {@link CSException} so callers can tell "the server is saturated, retry"
 * apart from "the database rejected your work". The generated MCP server maps it to a busy-server
 * tool error rather than a database error.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class CSPoolExhaustedException extends CSException {

    /**
     * Default constructor
     */
    public CSPoolExhaustedException() {
        super();
    }

    /**
     * Constructor with parameters.
     *
     * @param theExceptionMessage An exception message
     */
    public CSPoolExhaustedException(String theExceptionMessage) {
        super(theExceptionMessage);
    }
}
