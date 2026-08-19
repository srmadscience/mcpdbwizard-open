package com.mcpdbwizard.app.procbuilder;

import com.mcpdbwizard.app.procbuilder.gui.ApplicationShell;

/**
 * The {@code DAO_POOL_*} config settings, carried as one object rather than six more positional
 * parameters through {@code generateCodeV3} and {@code generateDAOFactoryClass}. They always travel
 * together and are meaningless apart, so a holder is the honest shape.
 *
 * <p>The values are the design-time defaults baked into generated code. A deployment overrides them
 * at run time through {@code com.mcpdbwizard.pub.DaoFactoryPoolConfig.applyOverrides()} without
 * regenerating, so what is emitted here is a starting point, not a commitment.
 *
 * <p>{@link #disabled()} is what every existing config produces, and it emits nothing at all — the
 * generated factory is byte-identical to what it was before pooling existed.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class DaoPoolSettings {

    private final boolean enabled;
    private final String maxSize;
    private final String minIdle;
    private final String maxWaitMs;
    private final String idleTimeoutMs;
    private final boolean commitOnReturn;

    public DaoPoolSettings(boolean enabled, String maxSize, String minIdle, String maxWaitMs,
                           String idleTimeoutMs, String onReturn) {
        this.enabled = enabled;
        this.maxSize = defaulted(maxSize, ApplicationShell.DEFAULT_DAO_POOL_MAX_SIZE);
        this.minIdle = defaulted(minIdle, ApplicationShell.DEFAULT_DAO_POOL_MIN_IDLE);
        this.maxWaitMs = defaulted(maxWaitMs, ApplicationShell.DEFAULT_DAO_POOL_MAX_WAIT_MS);
        this.idleTimeoutMs = defaulted(idleTimeoutMs, ApplicationShell.DEFAULT_DAO_POOL_IDLE_TIMEOUT_MS);
        this.commitOnReturn =
                !ApplicationShell.DAO_POOL_ON_RETURN_ROLLBACK.equalsIgnoreCase(onReturn == null ? "" : onReturn.trim());
    }

    /** Pooling off: what every config that says nothing about it produces. */
    public static DaoPoolSettings disabled() {
        return new DaoPoolSettings(false, null, null, null, null, null);
    }

    private static String defaulted(String theValue, String theDefault) {
        if (theValue == null || theValue.trim().length() == 0) {
            return theDefault;
        }
        return theValue.trim();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getMaxSize() {
        return maxSize;
    }

    public String getMinIdle() {
        return minIdle;
    }

    public String getMaxWaitMs() {
        return maxWaitMs;
    }

    public String getIdleTimeoutMs() {
        return idleTimeoutMs;
    }

    public boolean isCommitOnReturn() {
        return commitOnReturn;
    }
}
