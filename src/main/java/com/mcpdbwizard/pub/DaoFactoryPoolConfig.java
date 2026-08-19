package com.mcpdbwizard.pub;

/**
 * Sizing and lifetime settings for a {@link DaoFactoryPool}.
 *
 * <p>Generated code bakes in the values chosen at design time (the {@code DAO_POOL_*} config keys),
 * then calls {@link #applyOverrides()} so a deployment can retune without regenerating. Pool size is
 * a property of where the code runs, not of the schema it was generated from — the same generated
 * server may want 4 factories on a laptop and 40 in production.
 *
 * <p>Overrides are read from a system property first, then an environment variable. The environment
 * variable names are exactly the config keys, so what appears in the {@code .pb2}/{@code .json} is
 * what a container sets:
 *
 * <pre>
 *   DAO_POOL_MAX_SIZE          -Ddao.pool.maxSize
 *   DAO_POOL_MIN_IDLE          -Ddao.pool.minIdle
 *   DAO_POOL_MAX_WAIT_MS       -Ddao.pool.maxWaitMillis
 *   DAO_POOL_IDLE_TIMEOUT_MS   -Ddao.pool.idleTimeoutMillis
 *   DAO_POOL_ON_RETURN         -Ddao.pool.onReturn          (COMMIT | ROLLBACK)
 *   DAO_POOL_VALIDATE_ON_BORROW  -Ddao.pool.validateOnBorrow  (deployment-only, no config key)
 * </pre>
 *
 * <p>A malformed or out-of-range override throws rather than silently falling back to the baked-in
 * value: starting with a pool sized differently from what the operator asked for is worse than not
 * starting.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class DaoFactoryPoolConfig {

    /** Ceiling on live factories. The pool grows to this and then makes borrowers wait. */
    private int maxSize = 10;

    /**
     * Floor the evictor shrinks to. Zero — the default — lets an idle server give every Oracle
     * session back; raise it to keep a few factories warm for the next burst.
     */
    private int minIdle = 0;

    /** How long a borrower waits for a factory before {@link CSPoolExhaustedException}. */
    private long maxWaitMillis = 30000L;

    /** A factory idle longer than this is closed, subject to {@link #minIdle}. */
    private long idleTimeoutMillis = 300000L;

    /**
     * Whether returning a factory commits the borrower's work. <code>true</code> matches the
     * unpooled generated default ({@code COMMIT_CONNECTIONS=YES}). The pool overrides this to a
     * rollback whenever the borrower threw.
     */
    private boolean commitOnReturn = true;

    /**
     * Ping the session before handing a factory out. On by default: a factory that has sat idle may
     * have had its session killed by the server or a firewall, and finding that out mid-call costs
     * far more than the ping. Deployment-only knob — turn it off with the system property or
     * environment variable above if borrow rate matters more than a rare failed call.
     */
    private boolean validateOnBorrow = true;

    /** Defaults as documented on each field. */
    public DaoFactoryPoolConfig() {
    }

    /**
     * Overlay any settings supplied by the deployment, then validate. Generated code calls this
     * after applying its baked-in design-time values.
     *
     * @return this config, for chaining
     * @throws IllegalArgumentException if an override is unparseable, or if the resulting values are
     *                                  inconsistent
     */
    public DaoFactoryPoolConfig applyOverrides() {
        maxSize = intOverride("DAO_POOL_MAX_SIZE", "dao.pool.maxSize", maxSize);
        minIdle = intOverride("DAO_POOL_MIN_IDLE", "dao.pool.minIdle", minIdle);
        maxWaitMillis = longOverride("DAO_POOL_MAX_WAIT_MS", "dao.pool.maxWaitMillis", maxWaitMillis);
        idleTimeoutMillis = longOverride("DAO_POOL_IDLE_TIMEOUT_MS", "dao.pool.idleTimeoutMillis",
                idleTimeoutMillis);

        String onReturn = override("DAO_POOL_ON_RETURN", "dao.pool.onReturn");
        if (onReturn != null) {
            if ("COMMIT".equalsIgnoreCase(onReturn)) {
                commitOnReturn = true;
            } else if ("ROLLBACK".equalsIgnoreCase(onReturn)) {
                commitOnReturn = false;
            } else {
                throw new IllegalArgumentException(
                        "DAO_POOL_ON_RETURN must be COMMIT or ROLLBACK, not '" + onReturn + "'");
            }
        }

        String validate = override("DAO_POOL_VALIDATE_ON_BORROW", "dao.pool.validateOnBorrow");
        if (validate != null) {
            if ("true".equalsIgnoreCase(validate) || "YES".equalsIgnoreCase(validate)) {
                validateOnBorrow = true;
            } else if ("false".equalsIgnoreCase(validate) || "NO".equalsIgnoreCase(validate)) {
                validateOnBorrow = false;
            } else {
                throw new IllegalArgumentException("DAO_POOL_VALIDATE_ON_BORROW must be true or false, not '"
                        + validate + "'");
            }
        }

        return validateSettings();
    }

    /**
     * Check the settings make sense together.
     *
     * @return this config, for chaining
     * @throws IllegalArgumentException if they do not
     */
    public DaoFactoryPoolConfig validateSettings() {
        if (maxSize < 1) {
            throw new IllegalArgumentException("DAO_POOL_MAX_SIZE must be at least 1, not " + maxSize);
        }
        if (minIdle < 0) {
            throw new IllegalArgumentException("DAO_POOL_MIN_IDLE cannot be negative, was " + minIdle);
        }
        if (minIdle > maxSize) {
            throw new IllegalArgumentException("DAO_POOL_MIN_IDLE (" + minIdle
                    + ") cannot exceed DAO_POOL_MAX_SIZE (" + maxSize + ")");
        }
        if (maxWaitMillis < 0) {
            throw new IllegalArgumentException("DAO_POOL_MAX_WAIT_MS cannot be negative, was " + maxWaitMillis);
        }
        if (idleTimeoutMillis < 1) {
            throw new IllegalArgumentException("DAO_POOL_IDLE_TIMEOUT_MS must be positive, was "
                    + idleTimeoutMillis);
        }
        return this;
    }

    /** System property wins over environment variable; null when neither is set or both are blank. */
    private static String override(String environmentName, String propertyName) {
        String theValue = System.getProperty(propertyName);
        if (theValue == null || theValue.trim().length() == 0) {
            theValue = System.getenv(environmentName);
        }
        if (theValue == null || theValue.trim().length() == 0) {
            return null;
        }
        return theValue.trim();
    }

    private static int intOverride(String environmentName, String propertyName, int theDefault) {
        String theValue = override(environmentName, propertyName);
        if (theValue == null) {
            return theDefault;
        }
        try {
            return Integer.parseInt(theValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(environmentName + " must be a whole number, not '"
                    + theValue + "'");
        }
    }

    private static long longOverride(String environmentName, String propertyName, long theDefault) {
        String theValue = override(environmentName, propertyName);
        if (theValue == null) {
            return theDefault;
        }
        try {
            return Long.parseLong(theValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(environmentName + " must be a whole number, not '"
                    + theValue + "'");
        }
    }

    public int getMaxSize() {
        return maxSize;
    }

    public DaoFactoryPoolConfig setMaxSize(int maxSize) {
        this.maxSize = maxSize;
        return this;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public DaoFactoryPoolConfig setMinIdle(int minIdle) {
        this.minIdle = minIdle;
        return this;
    }

    public long getMaxWaitMillis() {
        return maxWaitMillis;
    }

    public DaoFactoryPoolConfig setMaxWaitMillis(long maxWaitMillis) {
        this.maxWaitMillis = maxWaitMillis;
        return this;
    }

    public long getIdleTimeoutMillis() {
        return idleTimeoutMillis;
    }

    public DaoFactoryPoolConfig setIdleTimeoutMillis(long idleTimeoutMillis) {
        this.idleTimeoutMillis = idleTimeoutMillis;
        return this;
    }

    public boolean isCommitOnReturn() {
        return commitOnReturn;
    }

    public DaoFactoryPoolConfig setCommitOnReturn(boolean commitOnReturn) {
        this.commitOnReturn = commitOnReturn;
        return this;
    }

    public boolean isValidateOnBorrow() {
        return validateOnBorrow;
    }

    public DaoFactoryPoolConfig setValidateOnBorrow(boolean validateOnBorrow) {
        this.validateOnBorrow = validateOnBorrow;
        return this;
    }

    @Override
    public String toString() {
        return "maxSize=" + maxSize + ", minIdle=" + minIdle + ", maxWaitMillis=" + maxWaitMillis
                + ", idleTimeoutMillis=" + idleTimeoutMillis
                + ", onReturn=" + (commitOnReturn ? "COMMIT" : "ROLLBACK")
                + ", validateOnBorrow=" + validateOnBorrow;
    }
}
