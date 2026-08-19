package com.mcpdbwizard.pub;

/**
 * Chooses and builds the {@link McpAuditSink} a generated server uses.
 *
 * <table>
 *   <caption>Environment</caption>
 *   <tr><td>{@code MCP_AUDIT_SINK}</td>
 *       <td>Fully-qualified class name of an {@link McpAuditSink} to send records OFF this machine.
 *           Unset means records stay here.</td></tr>
 *   <tr><td>{@code MCP_AUDIT_FILE_DIR}</td>
 *       <td>Where the local trail is kept. See {@link FileAuditSink}.</td></tr>
 *   <tr><td>{@code MCP_AUDIT_RETENTION_DAYS}</td>
 *       <td>How long the local trail is kept. 0 keeps nothing.</td></tr>
 *   <tr><td>{@code MCP_AUDIT_LEVEL}</td>
 *       <td>{@code names} (default) or {@code values}. See below.</td></tr>
 *   <tr><td>{@code MCP_AUDIT_MAX_BYTES}</td>
 *       <td>Cap on the recorded response, default {@value #DEFAULT_MAX_BYTES}. 0 means no cap.</td></tr>
 * </table>
 *
 * <p><b>{@code names} is the default deliberately.</b> Argument values and response bodies are
 * production data chosen by a model, so recording them turns the sink into a store with retention,
 * encryption and erasure obligations. A default install must not begin exporting personal data
 * because someone switched auditing on.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class McpAuditSinks {

    /**
     * One audit setting, from the environment or a system property.
     *
     * <p><b>The environment WINS.</b> A deployment that pins a value in its compose file or its
     * container spec must not be silently overridden from a web page — otherwise the file an
     * operator reads to learn where production data goes becomes a lie. The system property is the
     * fallback, and exists so the web application can set what its own deployment left open: the
     * audit sink is built inside that process, and a process cannot change its own environment.
     *
     * <p>The property spelling is the variable lower-cased with underscores as dots, matching the
     * {@code DAO_POOL_*} / {@code -Ddao.pool.*} pairing the generated pool already uses.
     *
     * <p>An environment value that is present but BLANK counts as unset, so a compose file can list
     * every variable for documentation without thereby pinning them all.
     */
    public static String setting(String theVariable) {
        String theFromEnvironment = System.getenv(theVariable);
        if (theFromEnvironment != null && theFromEnvironment.trim().length() > 0) {
            return theFromEnvironment;
        }
        return System.getProperty(propertyNameFor(theVariable));
    }

    /** The system-property spelling of an audit environment variable. */
    public static String propertyNameFor(String theVariable) {
        return theVariable.toLowerCase(java.util.Locale.ROOT).replace('_', '.');
    }

    /** True when the environment pins this setting, so a UI must not offer to change it. */
    public static boolean isFixedByEnvironment(String theVariable) {
        String theValue = System.getenv(theVariable);
        return theValue != null && theValue.trim().length() > 0;
    }

    public static final String SINK_VARIABLE = "MCP_AUDIT_SINK";
    public static final String LEVEL_VARIABLE = "MCP_AUDIT_LEVEL";
    public static final String MAX_BYTES_VARIABLE = "MCP_AUDIT_MAX_BYTES";

    /** Enough for a realistic response without letting one call dominate the queue. */
    public static final int DEFAULT_MAX_BYTES = 8192;

    /** How much of a call is recorded. */
    public enum Level {
        /** Tool, outcome, duration and argument NAMES. No production data. */
        NAMES,
        /** Adds argument values and the response body, subject to the byte cap. */
        VALUES
    }

    private McpAuditSinks() {
    }

    /** A sink that discards everything, used when none is configured. */
    public static McpAuditSink noOp() {
        return new McpAuditSink() {
            public void record(McpAuditEvent theEvent) {
            }

            public void close() {
            }
        };
    }

    /**
     * Whether anything at all is recording — a local trail, a remote sink, or both.
     *
     * <p><b>Widened on 2026-08-19</b>, when the local trail arrived. It used to ask only whether
     * {@link #SINK_VARIABLE} named a class, and its callers use it to decide whether to install a
     * shutdown hook and whether to tell an operator that auditing is on. Left as it was, a
     * deployment with a local trail and no remote sink would have had a sink that was never closed
     * and a status page saying it was not auditing.
     */
    public static boolean isConfigured() {
        return isStreamConfigured() || isLocalTrailConfigured();
    }

    /** Whether {@link #SINK_VARIABLE} names a sink to send records off this machine. */
    public static boolean isStreamConfigured() {
        String theName = setting(SINK_VARIABLE);
        return theName != null && theName.trim().length() > 0;
    }

    /**
     * Whether a local trail is wanted: a directory is named and the window is not zero.
     *
     * <p>Zero is a deliberate setting meaning "keep nothing here", not an absence.
     */
    public static boolean isLocalTrailConfigured() {
        String theDirectory = setting(FileAuditSink.DIRECTORY_VARIABLE);
        if (theDirectory == null || theDirectory.trim().length() == 0) {
            return false;
        }
        return FileAuditSink.retentionDays(setting(FileAuditSink.RETENTION_DAYS_VARIABLE)) > 0;
    }

    /**
     * Build the configured sink, or a no-op when none is named.
     *
     * @throws IllegalArgumentException if a sink is named but cannot be constructed — a mistyped
     *                                  class name must stop start-up rather than silently leave the
     *                                  server unaudited, which is the failure where an operator
     *                                  believes calls are being recorded and they are not
     */
    public static McpAuditSink fromEnvironment() {
        return fromEnvironment(null);
    }

    /**
     * As {@link #fromEnvironment()}, but giving this process its own spool subdirectory.
     *
     * <p>Only a process that shares a host with other audited processes needs this — in practice the
     * web application, which records proxied requests while the generated servers it launched record
     * their own tool calls. A spool tolerates exactly one writer; see
     * {@link SpoolingAuditSink#wrap(McpAuditSink, String)}.
     *
     * @param theSpoolSubdirectory a directory name under {@code MCP_AUDIT_SPOOL_DIR}, or null
     */
    public static McpAuditSink fromEnvironment(String theSpoolSubdirectory) {
        return fromEnvironment(theSpoolSubdirectory, Integer.MAX_VALUE);
    }

    /**
     * As {@link #fromEnvironment(String)}, capping the local trail's window.
     *
     * <p>See {@link FileAuditSink#fromEnvironment(String, int)} for why a caller would need to.
     *
     * @param theRetentionCeilingDays the largest local window to honour
     */
    public static McpAuditSink fromEnvironment(String theSpoolSubdirectory,
                                               int theRetentionCeilingDays) {
        // The local trail is not a sink an operator selects; it is what the deployment keeps. It is
        // therefore built from its own variables rather than from SINK_VARIABLE, which leaves that
        // meaning what it always meant: where records go when they leave this machine.
        McpAuditSink theLocalTrail =
                FileAuditSink.fromEnvironment(theSpoolSubdirectory, theRetentionCeilingDays);
        McpAuditSink theStream = streamFromEnvironment(theSpoolSubdirectory);

        if (theLocalTrail == null) {
            return theStream == null ? noOp() : theStream;
        }
        if (theStream == null) {
            return theLocalTrail;
        }
        return new FanOutAuditSink(theLocalTrail, theStream);
    }

    /** The remote sink named by {@link #SINK_VARIABLE}, spooled, or null when none is named. */
    private static McpAuditSink streamFromEnvironment(String theSpoolSubdirectory) {
        if (!isStreamConfigured()) {
            return null;
        }
        String theName = setting(SINK_VARIABLE).trim();
        if (FileAuditSink.class.getName().equals(theName)) {
            // Reachable, and confusing enough to be worth naming: it would be spooled -- two copies
            // of the same bytes -- and it would ignore the retention window, because the window is
            // read where the trail is built and not here.
            throw new IllegalArgumentException(SINK_VARIABLE + " must not be "
                    + FileAuditSink.class.getName() + ". The local trail is configured with "
                    + FileAuditSink.DIRECTORY_VARIABLE + " and "
                    + FileAuditSink.RETENTION_DAYS_VARIABLE + "; " + SINK_VARIABLE
                    + " is for sending records off this machine.");
        }
        try {
            Object theInstance = Class.forName(theName).getDeclaredConstructor().newInstance();
            if (!(theInstance instanceof McpAuditSink)) {
                throw new IllegalArgumentException(SINK_VARIABLE + "=" + theName
                        + " does not implement " + McpAuditSink.class.getName());
            }
            // Wrapped when MCP_AUDIT_SPOOL_DIR is set, so durability is a deployment choice rather
            // than something each sink implementation has to solve for itself.
            return SpoolingAuditSink.wrap((McpAuditSink) theInstance, theSpoolSubdirectory);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not create the audit sink " + theName
                    + ": " + e + ". It needs a public no-argument constructor and must be on the"
                    + " classpath.");
        }
    }

    /** The configured level, defaulting to names. */
    public static Level level() {
        return level(setting(LEVEL_VARIABLE));
    }

    /** Testable half of {@link #level()}. */
    static Level level(String theSetting) {
        if (theSetting == null || theSetting.trim().length() == 0) {
            return Level.NAMES;
        }
        String theValue = theSetting.trim();
        if ("values".equalsIgnoreCase(theValue)) {
            return Level.VALUES;
        }
        if ("names".equalsIgnoreCase(theValue)) {
            return Level.NAMES;
        }
        throw new IllegalArgumentException(LEVEL_VARIABLE + " must be 'names' or 'values', not '"
                + theValue + "'");
    }

    /** The configured response cap in bytes. */
    public static int maxBytes() {
        return maxBytes(setting(MAX_BYTES_VARIABLE));
    }

    /** Testable half of {@link #maxBytes()}. */
    static int maxBytes(String theSetting) {
        if (theSetting == null || theSetting.trim().length() == 0) {
            return DEFAULT_MAX_BYTES;
        }
        try {
            int theValue = Integer.parseInt(theSetting.trim());
            if (theValue < 0) {
                throw new IllegalArgumentException(MAX_BYTES_VARIABLE
                        + " cannot be negative, got '" + theSetting.trim() + "'");
            }
            return theValue;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(MAX_BYTES_VARIABLE
                    + " must be a whole number of bytes, not '" + theSetting.trim() + "'");
        }
    }
}
