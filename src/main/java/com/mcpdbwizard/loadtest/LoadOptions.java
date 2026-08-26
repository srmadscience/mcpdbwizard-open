package com.mcpdbwizard.loadtest;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * One run's settings, parsed from the command line.
 *
 * <p>Deliberately free of anything that touches the network or the clock, so the whole of the
 * argument handling — including every rejection message — is covered by ordinary unit tests. The
 * only thing {@link McpLoad} does with an instance is read it.
 *
 * <p><b>Every bad value is rejected here, before a connection is opened.</b> A load run that starts,
 * makes a hundred thousand failing calls and then reports a 100% error rate looks exactly like a
 * sick server; a typo in a flag should say so in the first second instead.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class LoadOptions {

    /** Where the MCP endpoint is, path included. */
    private String theUrl;

    /** Bearer token, or null for an unauthenticated server. */
    private String theToken;

    private boolean theListFlag;

    private boolean theHelpFlag;

    private final List<String> theToolNames = new ArrayList<String>();

    private String theWorkloadFile;

    /** Wall-clock budget in nanoseconds; 0 means "no time limit". */
    private long theDurationNanos;

    /** Call budget; 0 means "no call limit". */
    private long theCallBudget;

    private int theThreads = 1;

    /** Target calls per second across all threads; 0 means flat out. */
    private double theRate;

    private long theWarmupNanos;

    private Duration theRequestTimeout = Duration.ofSeconds(60);

    private long theProgressNanos = Duration.ofSeconds(10).toNanos();

    private String theOutputFile;

    private long theRandomSeed = 20260826L;

    private LoadOptions() {
    }

    /**
     * Parse a command line.
     *
     * @throws IllegalArgumentException with a message fit to print, never a stack trace fit to read
     */
    public static LoadOptions parse(String[] theArgs) {
        LoadOptions theOptions = new LoadOptions();
        for (int i = 0; i < theArgs.length; i++) {
            String theFlag = theArgs[i];
            switch (theFlag) {
                case "--help":
                case "-h":
                    theOptions.theHelpFlag = true;
                    break;
                case "--list":
                    theOptions.theListFlag = true;
                    break;
                case "--url":
                    theOptions.theUrl = value(theArgs, ++i, theFlag);
                    break;
                case "--token":
                    theOptions.theToken = value(theArgs, ++i, theFlag);
                    break;
                case "--tools":
                    for (String theName : value(theArgs, ++i, theFlag).split(",")) {
                        if (!theName.trim().isEmpty()) {
                            theOptions.theToolNames.add(theName.trim());
                        }
                    }
                    break;
                case "--workload":
                    theOptions.theWorkloadFile = value(theArgs, ++i, theFlag);
                    break;
                case "--for":
                    theOptions.theDurationNanos = parseDuration(value(theArgs, ++i, theFlag), theFlag);
                    break;
                case "--calls":
                    theOptions.theCallBudget = parsePositiveLong(value(theArgs, ++i, theFlag), theFlag);
                    break;
                case "--threads":
                    theOptions.theThreads =
                            (int) parsePositiveLong(value(theArgs, ++i, theFlag), theFlag);
                    break;
                case "--rate":
                    theOptions.theRate = parseRate(value(theArgs, ++i, theFlag), theFlag);
                    break;
                case "--warmup":
                    theOptions.theWarmupNanos = parseDuration(value(theArgs, ++i, theFlag), theFlag);
                    break;
                case "--timeout":
                    theOptions.theRequestTimeout =
                            Duration.ofNanos(parseDuration(value(theArgs, ++i, theFlag), theFlag));
                    break;
                case "--progress":
                    theOptions.theProgressNanos = parseDuration(value(theArgs, ++i, theFlag), theFlag);
                    break;
                case "--out":
                    theOptions.theOutputFile = value(theArgs, ++i, theFlag);
                    break;
                case "--seed":
                    theOptions.theRandomSeed = Long.parseLong(value(theArgs, ++i, theFlag));
                    break;
                default:
                    throw new IllegalArgumentException("unknown option '" + theFlag
                            + "' (try --help)");
            }
        }
        theOptions.validate();
        return theOptions;
    }

    private void validate() {
        if (theHelpFlag) {
            return;
        }
        if (theUrl == null || theUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("--url is required, e.g."
                    + " --url http://127.0.0.1:8090/mcp");
        }
        try {
            URI theCheck = new URI(theUrl);
            if (theCheck.getHost() == null || theCheck.getScheme() == null) {
                throw new URISyntaxException(theUrl, "no scheme or host");
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("--url is not a usable URL: " + theUrl
                    + " (" + e.getReason() + ")");
        }
        if (theListFlag) {
            // Listing needs nothing but a URL. Everything below describes a run there will not be.
            return;
        }
        if (theDurationNanos == 0L && theCallBudget == 0L) {
            throw new IllegalArgumentException("give a budget: --for 300s, --calls 5000, or both"
                    + " (both stops at whichever comes first)");
        }
        if (theToolNames.isEmpty() && theWorkloadFile == null) {
            throw new IllegalArgumentException("nothing to call: give --tools a,b,c or"
                    + " --workload <file>. Run with --list to see what this server publishes.");
        }
        if (theWarmupNanos > 0L && theDurationNanos > 0L && theWarmupNanos >= theDurationNanos) {
            throw new IllegalArgumentException("--warmup must be shorter than --for, otherwise"
                    + " every sample is discarded and the run reports nothing");
        }
    }

    private static String value(String[] theArgs, int theIndex, String theFlag) {
        if (theIndex >= theArgs.length) {
            throw new IllegalArgumentException(theFlag + " needs a value");
        }
        return theArgs[theIndex];
    }

    /**
     * Parse {@code 500ms}, {@code 30s}, {@code 5m}, {@code 2h} or a bare number of seconds.
     *
     * <p>A bare number means seconds because the harness this replaces took {@code 300s} and people
     * type {@code 300}; silently reading that as 300 milliseconds would turn a five-minute run into
     * one that finishes before the JVM has finished warming up.
     */
    public static long parseDuration(String theText, String theFlag) {
        String theTrimmed = theText.trim().toLowerCase();
        try {
            if (theTrimmed.endsWith("ms")) {
                return Long.parseLong(theTrimmed.substring(0, theTrimmed.length() - 2).trim())
                        * 1_000_000L;
            }
            if (theTrimmed.endsWith("h")) {
                return Long.parseLong(theTrimmed.substring(0, theTrimmed.length() - 1).trim())
                        * 3_600_000_000_000L;
            }
            if (theTrimmed.endsWith("m")) {
                return Long.parseLong(theTrimmed.substring(0, theTrimmed.length() - 1).trim())
                        * 60_000_000_000L;
            }
            if (theTrimmed.endsWith("s")) {
                return Long.parseLong(theTrimmed.substring(0, theTrimmed.length() - 1).trim())
                        * 1_000_000_000L;
            }
            return Long.parseLong(theTrimmed) * 1_000_000_000L;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(theFlag + " wants a duration like 30s, 5m, 500ms"
                    + " or a bare number of seconds — got '" + theText + "'");
        }
    }

    private static long parsePositiveLong(String theText, String theFlag) {
        long theValue;
        try {
            theValue = Long.parseLong(theText.trim().replace(",", "").replace("_", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(theFlag + " wants a whole number — got '"
                    + theText + "'");
        }
        if (theValue < 1L) {
            throw new IllegalArgumentException(theFlag + " must be at least 1 — got " + theValue);
        }
        return theValue;
    }

    private static double parseRate(String theText, String theFlag) {
        double theValue;
        try {
            theValue = Double.parseDouble(theText.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(theFlag + " wants calls per second — got '"
                    + theText + "'");
        }
        if (theValue <= 0.0) {
            throw new IllegalArgumentException(theFlag + " must be above zero; omit it entirely"
                    + " to run flat out");
        }
        return theValue;
    }

    /** Scheme, host and port — what the SDK's transport builder calls the base URL. */
    public String baseUrl() {
        URI theUri = URI.create(theUrl);
        StringBuilder theBase = new StringBuilder(theUri.getScheme()).append("://")
                .append(theUri.getHost());
        if (theUri.getPort() != -1) {
            theBase.append(':').append(theUri.getPort());
        }
        return theBase.toString();
    }

    /**
     * The endpoint path the transport POSTs to.
     *
     * <p>This is what lets one flag reach both targets: {@code /mcp} is a generated server,
     * {@code /mcp/dr} is the web proxy for the config named {@code dr}.
     */
    public String endpointPath() {
        String thePath = URI.create(theUrl).getPath();
        return thePath == null || thePath.isEmpty() ? "/mcp" : thePath;
    }

    public String url() {
        return theUrl;
    }

    public String token() {
        return theToken;
    }

    public boolean isListOnly() {
        return theListFlag;
    }

    public boolean isHelp() {
        return theHelpFlag;
    }

    public List<String> toolNames() {
        return List.copyOf(theToolNames);
    }

    public String workloadFile() {
        return theWorkloadFile;
    }

    public long durationNanos() {
        return theDurationNanos;
    }

    public long callBudget() {
        return theCallBudget;
    }

    public int threads() {
        return theThreads;
    }

    public double rate() {
        return theRate;
    }

    public long warmupNanos() {
        return theWarmupNanos;
    }

    public Duration requestTimeout() {
        return theRequestTimeout;
    }

    public long progressNanos() {
        return theProgressNanos;
    }

    public String outputFile() {
        return theOutputFile;
    }

    public long randomSeed() {
        return theRandomSeed;
    }

    public static String usage() {
        return String.join(System.lineSeparator(), Arrays.asList(
            "Drive a generated MCP server's tools over JSON-RPC at a controlled rate.",
            "",
            "  java -cp '/app/lib/*' com.mcpdbwizard.loadtest.McpLoad --url <url> [options]",
            "",
            "Target",
            "  --url <url>        the MCP endpoint, path included. A generated server is",
            "                       http://127.0.0.1:8090/mcp ; the web proxy for config 'dr' is",
            "                       http://host:8080/mcp/dr",
            "  --token <token>    bearer token. MCP_HTTP_TOKEN for a generated server, or a user's",
            "                       API token for the proxy. Omit for an unauthenticated server.",
            "",
            "What to call",
            "  --list             print every published tool, its input schema and a ready-made",
            "                       workload entry, then exit. Start here.",
            "  --tools a,b,c      call these tools in rotation. For tools that take no arguments.",
            "  --workload <file>  a JSON file of {tool, args, weight, check} entries. See --list.",
            "",
            "How long, how hard",
            "  --for <duration>   wall-clock budget: 300s, 5m, 2h, 500ms, or bare seconds",
            "  --calls <n>        call budget. With --for as well, whichever comes first wins.",
            "  --threads <n>      concurrent MCP sessions (default 1)",
            "  --rate <n>         target calls/sec across ALL threads. Omit to run flat out.",
            "  --warmup <dur>     discard samples from the first <dur>; the run still makes them",
            "  --timeout <dur>    per-call request timeout (default 60s)",
            "  --progress <dur>   progress line interval (default 10s)",
            "  --seed <n>         seed for ${random:a-b} substitution, so a run repeats",
            "  --out <file>       also write the results as JSON",
            "",
            "Examples",
            "  --url http://127.0.0.1:8090/mcp --list",
            "  --url http://127.0.0.1:8090/mcp --tools job_id_nextval --for 60s --threads 8",
            "  --url http://127.0.0.1:8090/mcp --workload work.json --for 5m --rate 200 --warmup 30s",
            "  --url http://host:8080/mcp/dr --token $API_TOKEN --workload work.json --calls 5000"));
    }
}
