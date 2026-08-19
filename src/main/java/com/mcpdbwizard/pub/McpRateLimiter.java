package com.mcpdbwizard.pub;

/**
 * A token bucket bounding how often a generated MCP server starts work.
 *
 * <p>Configured from the environment: {@code MCP_RATE_LIMIT} is the sustained rate in calls per
 * second and {@code MCP_RATE_BURST} the bucket depth — how many may arrive at once after a quiet
 * spell. <b>Unset means unlimited</b>, so an existing deployment behaves exactly as before and there
 * is no new config-file surface to carry.
 *
 * <h2>What this does and does not protect against</h2>
 *
 * <p>It bounds how <em>often</em> calls start. It does nothing about how long one runs, and that is
 * the case that actually pins a pooled connection — a single expensive query holds a factory for as
 * long as Oracle takes, whatever the request rate. A statement timeout is the tool for that; see
 * {@code docs/mcp-rate-limiting-plan.md}. This is worth knowing before treating a configured rate
 * limit as protection against a runaway agent.
 *
 * <p>Refusal is deliberately distinct from pool exhaustion. Both shed load, but one is policy and the
 * other is saturation, and an operator reading the log needs to tell them apart.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class McpRateLimiter {

    /** Sustained calls per second. Unset or empty means no limit. */
    public static final String RATE_VARIABLE = "MCP_RATE_LIMIT";

    /** Bucket depth — how many calls may arrive at once. Defaults to one second's worth. */
    public static final String BURST_VARIABLE = "MCP_RATE_BURST";

    private final double theRatePerSecond;
    private final double theBurst;

    /** Tokens available; guarded by this object's monitor along with theLastRefillNanos. */
    private double theTokens;
    private long theLastRefillNanos;

    private McpRateLimiter(double theRateValue, double theBurstValue) {
        this.theRatePerSecond = theRateValue;
        this.theBurst = theBurstValue;
        this.theTokens = theBurstValue;
        this.theLastRefillNanos = System.nanoTime();
    }

    /** A limiter that permits everything, for when no rate is configured. */
    public static McpRateLimiter disabled() {
        return new McpRateLimiter(0.0, 0.0);
    }

    /**
     * Build one from numbers rather than from the environment.
     *
     * <p>The generated server has exactly one limiter and reads it from the environment. The web
     * application needs <b>one per caller</b>, built from settings it has already parsed, so it needs
     * a factory that takes values. Same bucket, same arithmetic — only the source of the numbers
     * differs.
     *
     * @param theRateValue  sustained permits per second; zero or less yields {@link #disabled()}
     * @param theBurstValue bucket depth; raised to 1 if smaller, since a bucket that cannot hold a
     *                      single permit would refuse everything
     */
    public static McpRateLimiter of(double theRateValue, double theBurstValue) {
        if (theRateValue <= 0.0 || Double.isNaN(theRateValue) || Double.isInfinite(theRateValue)) {
            return disabled();
        }
        double theDepth = theBurstValue < 1.0 ? 1.0 : theBurstValue;
        return new McpRateLimiter(theRateValue, theDepth);
    }

    /**
     * Build from the environment.
     *
     * @throws IllegalArgumentException if a variable is set but unusable — a mistyped limit must stop
     *                                  start-up rather than silently leave the server unlimited
     */
    public static McpRateLimiter fromEnvironment() {
        return fromSettings(System.getenv(RATE_VARIABLE), System.getenv(BURST_VARIABLE));
    }

    /** Testable half of {@link #fromEnvironment()} — the JVM cannot set its own environment. */
    static McpRateLimiter fromSettings(String theRateSetting, String theBurstSetting) {
        double theRateValue = positiveNumber(RATE_VARIABLE, theRateSetting, 0.0);
        if (theRateValue <= 0.0) {
            if (theBurstSetting != null && theBurstSetting.trim().length() > 0) {
                throw new IllegalArgumentException(BURST_VARIABLE + " is set but " + RATE_VARIABLE
                        + " is not, so there is no rate for it to burst above. Set both or neither.");
            }
            return disabled();
        }

        // A burst below the rate would make the configured rate unreachable, so default the bucket to
        // one second's worth rather than to some arbitrary constant.
        double theBurstValue = positiveNumber(BURST_VARIABLE, theBurstSetting, theRateValue);
        if (theBurstValue < 1.0) {
            theBurstValue = 1.0;
        }
        return new McpRateLimiter(theRateValue, theBurstValue);
    }

    /** Whether a limit is in force. */
    public boolean isEnabled() {
        return theRatePerSecond > 0.0;
    }

    public double getRatePerSecond() {
        return theRatePerSecond;
    }

    public double getBurst() {
        return theBurst;
    }

    /**
     * Take one token if there is one.
     *
     * @return true when the call may proceed; false when it should be refused
     */
    public synchronized boolean tryAcquire() {
        if (!isEnabled()) {
            return true;
        }

        long theNow = System.nanoTime();
        long theElapsedNanos = theNow - theLastRefillNanos;
        if (theElapsedNanos > 0L) {
            theTokens += (theElapsedNanos / 1_000_000_000.0) * theRatePerSecond;
            if (theTokens > theBurst) {
                theTokens = theBurst;
            }
            theLastRefillNanos = theNow;
        }

        if (theTokens >= 1.0) {
            theTokens -= 1.0;
            return true;
        }
        return false;
    }

    /** How the limiter describes itself in the log at start-up. */
    @Override
    public String toString() {
        return isEnabled()
                ? "rate limit " + theRatePerSecond + "/s, burst " + theBurst
                : "no rate limit";
    }

    private static double positiveNumber(String theName, String theSetting, double theDefault) {
        if (theSetting == null || theSetting.trim().length() == 0) {
            return theDefault;
        }
        double theValue;
        try {
            theValue = Double.parseDouble(theSetting.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(theName + " must be a number, not '"
                    + theSetting.trim() + "'");
        }
        if (theValue <= 0.0 || Double.isNaN(theValue) || Double.isInfinite(theValue)) {
            throw new IllegalArgumentException(theName + " must be greater than zero, not '"
                    + theSetting.trim() + "'");
        }
        return theValue;
    }
}
