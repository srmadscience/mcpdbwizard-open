package com.mcpdbwizard.loadtest;

import io.modelcontextprotocol.client.McpSyncClient;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Drive a generated MCP server's tools over JSON-RPC, at a controlled rate, for a bounded time.
 *
 * <p>This exists to measure the server <b>without an LLM in the way</b>. An agent-driven session is
 * slow, expensive and different every time, so it cannot answer the questions a deployment actually
 * has: how many calls a second does this configuration sustain, what does p99 look like at the rate
 * we expect, and does the answer stay correct under load.
 *
 * <p>Run {@code --list} first: it prints every published tool with its parameters and a ready-made
 * workload entry, which is what makes "call any published tool" a copy-and-paste job rather than a
 * source edit. Then point a workload file at it.
 *
 * <p>It reaches both surfaces. {@code --url http://127.0.0.1:8090/mcp} is a generated server;
 * {@code --url http://host:8080/mcp/{config}} with {@code --token} is the web proxy, which is the
 * path real clients use and the only one carrying the per-caller rate limit, the call quota and the
 * token check.
 *
 * <p><b>It is a measuring tool, not a test.</b> Nothing in the suite runs it: it needs a server
 * someone has started and a database with real data in it, and it deliberately never starts or stops
 * a server itself — {@code Scripts/loadtest/mcp-load.sh} does that, and says so when it leaves one
 * running.
 *
 * <p>Prefer a duration to a call count for anything long. Throughput climbs steeply for the first
 * minute while the JVM JITs, the DAO pool grows and Oracle caches cursors: a count sized from a
 * five-second sample of 958/sec once finished in two minutes at 2,403/sec. {@code --warmup} exists
 * for the same reason — a run reported without one is averaging two different systems.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class McpLoad {

    private static final AtomicLong PROGRESS_DONE = new AtomicLong();

    private static final AtomicLong PROGRESS_ERRORS = new AtomicLong();

    private McpLoad() {
    }

    public static void main(String[] theArgs) throws Exception {
        LoadOptions theOptions;
        try {
            theOptions = LoadOptions.parse(theArgs);
        } catch (IllegalArgumentException e) {
            System.err.println("mcp-load: " + e.getMessage());
            System.exit(2);
            return;
        }
        if (theOptions.isHelp() || theArgs.length == 0) {
            System.out.println(LoadOptions.usage());
            return;
        }
        System.exit(run(theOptions));
    }

    /**
     * @return the process exit status: 0 for a clean run, 1 for one that could not be believed
     */
    static int run(LoadOptions theOptions) throws Exception {
        Workload theWorkload = null;
        if (!theOptions.isListOnly()) {
            try {
                theWorkload = theOptions.workloadFile() != null
                        ? Workload.fromFile(Paths.get(theOptions.workloadFile()))
                        : Workload.ofToolNames(theOptions.toolNames());
            } catch (IllegalArgumentException e) {
                System.err.println("mcp-load: " + theOptions.workloadFile() + ": " + e.getMessage());
                return 2;
            }
        }

        // One throwaway session to read the catalogue. Done before the workers start so a mistyped
        // tool name or a bad token costs a second rather than a whole run's worth of failures.
        ToolCatalogue theCatalogue;
        McpSyncClient theProbe = null;
        try {
            theProbe = McpClients.connect(theOptions);
            theCatalogue = ToolCatalogue.from(theProbe);
        } catch (RuntimeException e) {
            System.err.println("mcp-load: cannot reach " + theOptions.url() + " — "
                    + LoadWorker.describe(e));
            if (theOptions.token() == null) {
                System.err.println("  (no --token was given; a server with MCP_HTTP_TOKEN set, or"
                        + " the web proxy, will refuse an unauthenticated client)");
            }
            return 1;
        } finally {
            McpClients.closeQuietly(theProbe);
        }

        if (theOptions.isListOnly()) {
            System.out.print(theCatalogue.render());
            return 0;
        }

        List<String> theMissing = theCatalogue.missing(theWorkload.toolNames());
        if (!theMissing.isEmpty()) {
            System.err.println("mcp-load: this server does not publish: "
                    + String.join(", ", theMissing));
            for (String theName : theMissing) {
                List<String> theNear = theCatalogue.nearestTo(theName);
                if (!theNear.isEmpty()) {
                    System.err.println("  '" + theName + "' — did you mean: "
                            + String.join(", ", theNear));
                }
            }
            System.err.println("  run with --list to see all " + theCatalogue.size() + " tool(s)");
            return 2;
        }

        return execute(theOptions, theWorkload);
    }

    private static int execute(LoadOptions theOptions, Workload theWorkload)
            throws InterruptedException {
        System.out.println("url=" + theOptions.url()
                + " tools=" + String.join(",", theWorkload.toolNames())
                + (theOptions.durationNanos() > 0L
                        ? " for=" + theOptions.durationNanos() / 1_000_000_000L + "s" : "")
                + (theOptions.callBudget() > 0L ? " calls=" + theOptions.callBudget() : "")
                + " threads=" + theOptions.threads()
                + (theOptions.rate() > 0.0 ? " rate=" + theOptions.rate() + "/sec" : " rate=flat-out")
                + (theOptions.warmupNanos() > 0L
                        ? " warmup=" + theOptions.warmupNanos() / 1_000_000_000L + "s" : ""));

        long theStartNanos = System.nanoTime();
        long theDeadlineNanos = theOptions.durationNanos() > 0L
                ? theStartNanos + theOptions.durationNanos() : Long.MAX_VALUE;
        long theWarmupEndNanos = theStartNanos + theOptions.warmupNanos();
        RatePacer thePacer = RatePacer.at(theOptions.rate(), theStartNanos);

        List<LoadWorker> theWorkers = new ArrayList<LoadWorker>();
        Thread[] theThreads = new Thread[theOptions.threads()];
        for (int i = 0; i < theOptions.threads(); i++) {
            LoadWorker theWorker = new LoadWorker(i, theOptions, theWorkload, thePacer,
                    theDeadlineNanos, theWarmupEndNanos, PROGRESS_DONE, PROGRESS_ERRORS);
            theWorkers.add(theWorker);
            theThreads[i] = new Thread(theWorker, "mcp-load-" + i);
        }

        Thread theReporter = startProgress(theOptions, theStartNanos);
        for (Thread theThread : theThreads) {
            theThread.start();
        }
        for (Thread theThread : theThreads) {
            theThread.join();
        }
        long theWallMillis = (System.nanoTime() - theStartNanos) / 1_000_000L;
        theReporter.interrupt();

        Map<String, ToolSamples> theMerged = new LinkedHashMap<String, ToolSamples>();
        long theDiscarded = 0L;
        List<String> theFailures = new ArrayList<String>();
        Set<String> theSeen = new LinkedHashSet<String>();
        for (LoadWorker theWorker : theWorkers) {
            theDiscarded += theWorker.discarded();
            if (theWorker.failure() != null) {
                theFailures.add(theWorker.failure());
            }
            for (Map.Entry<String, ToolSamples> theEntry : theWorker.results().entrySet()) {
                theSeen.add(theEntry.getKey());
                theMerged.computeIfAbsent(theEntry.getKey(), k -> new ToolSamples())
                        .merge(theEntry.getValue());
            }
        }

        LoadReport theReport =
                new LoadReport(theOptions, theMerged, theWallMillis, theDiscarded, theFailures);
        System.out.print(theReport.render());
        if (theOptions.outputFile() != null) {
            Path thePath = Paths.get(theOptions.outputFile());
            try {
                theReport.writeJson(thePath);
                System.out.println("results written to " + thePath.toAbsolutePath());
            } catch (java.io.IOException e) {
                System.err.println("could not write " + thePath + ": " + e.getMessage());
            }
        }
        // Non-zero when the run itself did not hold together. A run whose TOOLS returned errors is
        // still a successful measurement -- the error count is the result -- so that alone is not a
        // failure here; a worker that never connected is.
        return theFailures.isEmpty() ? 0 : 1;
    }

    private static Thread startProgress(LoadOptions theOptions, long theStartNanos) {
        Thread theReporter = new Thread(() -> {
            long theEveryMillis = Math.max(1000L, theOptions.progressNanos() / 1_000_000L);
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(theEveryMillis);
                    long theDone = PROGRESS_DONE.get();
                    long theElapsedMillis = (System.nanoTime() - theStartNanos) / 1_000_000L;
                    System.out.printf("progress %,d calls  %,.1f/sec  errors %,d  elapsed %s%s%n",
                            Long.valueOf(theDone),
                            Double.valueOf(theDone * 1000.0 / Math.max(1L, theElapsedMillis)),
                            Long.valueOf(PROGRESS_ERRORS.get()),
                            LoadReport.duration(theElapsedMillis),
                            remaining(theOptions, theDone, theElapsedMillis));
                    System.out.flush();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "mcp-load-progress");
        theReporter.setDaemon(true);
        theReporter.setPriority(Thread.MIN_PRIORITY);
        theReporter.start();
        return theReporter;
    }

    private static String remaining(LoadOptions theOptions, long theDone, long theElapsedMillis) {
        if (theOptions.durationNanos() > 0L) {
            return "  remaining "
                    + LoadReport.duration(theOptions.durationNanos() / 1_000_000L - theElapsedMillis);
        }
        if (theOptions.callBudget() > 0L && theDone > 0L) {
            double theRate = theDone * 1000.0 / Math.max(1L, theElapsedMillis);
            return "  eta " + LoadReport.duration(
                    (long) ((theOptions.callBudget() - theDone) / Math.max(0.001, theRate) * 1000.0));
        }
        return "";
    }
}
