package com.mcpdbwizard.pub;

import java.util.Map;

/**
 * The text of one MCP {@code notifications/message} — a log line sent to the CLIENT over the
 * protocol, not to the operator.
 *
 * <h2>Why this exists at all, given the feature is deprecated</h2>
 *
 * <p>MCP's in-protocol logging utility is deprecated as of protocol revision {@code 2026-07-28}
 * (SEP-2577), and a new implementation would normally decline to adopt it. What forces the issue is
 * that <b>declining is not available</b>: both {@code McpAsyncServer} constructors in SDK 2.0.0 run
 * {@code features.serverCapabilities().mutate().logging().build()} unconditionally, so whatever the
 * generated server passes to {@code .capabilities(...)} is overwritten and every server advertises
 * {@code logging} whether or not it implements it. Verified in the bytecode of both constructors.
 *
 * <p>So the choice is not "adopt or abstain" but "advertise and implement" or "advertise and do
 * nothing". The second leaves an always-empty stream behind a capability a client is entitled to
 * act on, which is worse than either honest position. This is the cheap half of making the wire
 * true.
 *
 * <h2>Kept free of the MCP SDK, deliberately</h2>
 *
 * <p>This class handles only text: {@code String}/{@code Map}/{@code long} in, {@code String} out.
 * Nothing in {@code com.mcpdbwizard.pub} may reference the MCP SDK — {@code pub} ships with every
 * generated DAO layer, and a DAO layer must not drag an MCP dependency behind it. The SDK types
 * ({@code LoggingMessageNotification}, {@code LoggingLevel}) appear at exactly one emitted call
 * site in the generated server.
 *
 * <p>That split is also the deprecation hedge. When the SDK eventually removes those types, the
 * damage is one emitted site behind a generator edit, and no customer's {@code pub} jar names the
 * removed classes at all.
 *
 * <h2>Level and volume</h2>
 *
 * <p>These are sent at {@code DEBUG}. A session's threshold defaults to {@code INFO} and the SDK
 * drops anything below it before it reaches the wire, so a client that never calls
 * {@code logging/setLevel} receives <b>nothing</b> — no added traffic and no tokens spent. That is
 * what makes sending them unconditionally affordable, and unconditional is what makes it honest:
 * the capability is advertised for every config, so a flag would leave the mismatch in place for
 * every config that did not set it.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class McpProtocolLog {

    /**
     * The logger name carried on each notification. The MCP spec leaves this free-form; the server
     * class is what a client would need in order to tell two servers apart.
     */
    public static String logger(String theServerClassName) {
        if (theServerClassName == null || theServerClassName.isEmpty()) {
            return "mcp";
        }
        return theServerClassName;
    }

    private McpProtocolLog() {
    }

    /**
     * The message text: which tool ran, how it ended, how long it took, and which arguments it was
     * given BY NAME.
     *
     * <pre>ob_gen_pkg_greet outcome=ok ms=12 args=[p_name]</pre>
     *
     * <p>Values are omitted for the same reason {@link McpCallRecord} omits them — they were chosen
     * by a model and can carry anything that was put in front of it. The rule is weaker here, since
     * this notification goes back to the caller that supplied those values, but one rule for both
     * records is worth more than the marginal difference: log frames do not always end up in the
     * same place the result does.
     *
     * @param theToolName     the MCP tool that was invoked
     * @param theArguments    the call's arguments; only the keys are used, and null is fine
     * @param theOutcome      one of {@link McpCallRecord}'s {@code OUTCOME_} constants
     * @param theMilliseconds wall-clock duration of the call
     */
    public static String line(String theToolName, Map<String, Object> theArguments,
                              String theOutcome, long theMilliseconds) {
        StringBuilder theLine = new StringBuilder();
        theLine.append(theToolName == null ? "" : theToolName);
        theLine.append(" outcome=").append(theOutcome == null ? "" : theOutcome);
        theLine.append(" ms=").append(theMilliseconds);
        theLine.append(" args=[").append(McpCallRecord.argumentNames(theArguments)).append(']');
        return theLine.toString();
    }
}
