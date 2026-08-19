import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Load-test one or more MCP tools and check the answers, not just the throughput.
 *
 * <p>Usage: {@code McpLoad <baseUrl> <calls|Ns> <tool[,tool...]> [threads] [progressEvery]}
 *
 * <p>The second argument is either a call COUNT ({@code 5000}) or a DURATION ({@code 300s}).
 * Duration is the honest way to ask for "five minutes": throughput climbs steeply while the JVM
 * JITs, the pool grows and Oracle caches cursors, so a count derived from a short sample
 * under-shoots badly — 290,000 calls sized from a 5-second measurement of 958/sec finished in two
 * minutes at 2,403/sec.
 *
 * <p>Several tools can be named; each thread rotates through them, so one run exercises a mixed
 * workload and reports each tool separately. A sequence tool is additionally checked for
 * uniqueness: every call must return a different value, which is what proves the calls reached
 * Oracle rather than a cache.
 *
 * <p>Deliberately dependency-free beyond the MCP SDK that is already on the generated server's
 * classpath, and deliberately NOT a JUnit test: it is a measuring tool, run by hand against a
 * server someone has started, not something a suite should run.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class McpLoad {

    private static final AtomicLong PROGRESS_DONE = new AtomicLong();
    private static final AtomicLong PROGRESS_ERRORS = new AtomicLong();
    private static volatile boolean STOP = false;

    /** Growable long array: a duration run does not know how many samples it will take. */
    private static final class LongList {
        private long[] a = new long[1024];
        private int n = 0;
        void add(long v) {
            if (n == a.length) {
                a = Arrays.copyOf(a, a.length * 2);
            }
            a[n++] = v;
        }
        long[] toArray() {
            return Arrays.copyOf(a, n);
        }
        int size() {
            return n;
        }
    }

    /** What one thread collected for one tool. */
    private static final class Samples {
        final LongList latencies = new LongList();
        final LongList values = new LongList();   // sequence tools only
        long errors = 0;
    }

    public static void main(String[] args) throws Exception {
        String theUrl = args.length > 0 ? args[0] : "http://127.0.0.1:8090";
        String theBudget = args.length > 1 ? args[1] : "1000";
        String[] theTools = (args.length > 2 ? args[2] : "job_id_nextval").split(",");
        int theThreads = args.length > 3 ? Integer.parseInt(args[3]) : 1;
        long theProgressEvery = args.length > 4 ? Long.parseLong(args[4]) : 25000L;

        boolean theTimed = theBudget.endsWith("s");
        long theSeconds = theTimed ? Long.parseLong(theBudget.substring(0, theBudget.length() - 1)) : 0;
        int theCalls = theTimed ? 0 : Integer.parseInt(theBudget);

        System.out.println("url=" + theUrl + " tools=" + String.join(",", theTools)
                + (theTimed ? " duration=" + theSeconds + "s" : " calls=" + theCalls)
                + " threads=" + theThreads);

        // [thread][tool]
        Samples[][] theSamples = new Samples[theThreads][theTools.length];
        for (int t = 0; t < theThreads; t++) {
            for (int k = 0; k < theTools.length; k++) {
                theSamples[t][k] = new Samples();
            }
        }

        long theWallStart = System.nanoTime();
        long theDeadline = theTimed ? theWallStart + theSeconds * 1_000_000_000L : Long.MAX_VALUE;
        Thread theReporter = startReporter(theCalls, theTimed, theSeconds, theProgressEvery, theWallStart);

        Thread[] theWorkers = new Thread[theThreads];
        int theChunk = theTimed ? Integer.MAX_VALUE : theCalls / theThreads;
        for (int t = 0; t < theThreads; t++) {
            final int theSlice = t;
            final int theLimit = theTimed ? Integer.MAX_VALUE
                    : (t == theThreads - 1 ? theCalls - theChunk * (theThreads - 1) : theChunk);
            theWorkers[t] = new Thread(() -> runSlice(theUrl, theTools, theLimit, theDeadline,
                    theSamples[theSlice]), "mcp-load-" + t);
            theWorkers[t].start();
        }
        for (Thread theWorker : theWorkers) {
            theWorker.join();
        }
        STOP = true;
        long theWallMs = (System.nanoTime() - theWallStart) / 1_000_000L;
        theReporter.interrupt();

        report(theTools, theSamples, theWallMs, theThreads);
    }

    private static void runSlice(String theUrl, String[] theTools, int theLimit, long theDeadline,
            Samples[] theMine) {
        HttpClientStreamableHttpTransport theTransport =
                HttpClientStreamableHttpTransport.builder(theUrl).endpoint("/mcp").build();
        McpSyncClient theClient = McpClient.sync(theTransport)
                .requestTimeout(Duration.ofSeconds(60))
                .clientInfo(new McpSchema.Implementation("mcp-load", "1.0.0"))
                .build();
        try {
            theClient.initialize();
            for (int seq = 0; seq < theLimit && System.nanoTime() < theDeadline; seq++) {
                int k = seq % theTools.length;
                String theTool = theTools[k];
                Samples theTarget = theMine[k];
                long theStart = System.nanoTime();
                try {
                    McpSchema.CallToolResult theResult = theClient.callTool(
                            new McpSchema.CallToolRequest(theTool, argsFor(theTool, seq)));
                    theTarget.latencies.add((System.nanoTime() - theStart) / 1000L);
                    if (Boolean.TRUE.equals(theResult.isError()) || theResult.content().isEmpty()) {
                        theTarget.errors++;
                        PROGRESS_ERRORS.incrementAndGet();
                    } else if (theTool.endsWith("_nextval")) {
                        theTarget.values.add(extractNumber(
                                ((McpSchema.TextContent) theResult.content().get(0)).text()));
                    }
                } catch (RuntimeException e) {
                    theTarget.latencies.add((System.nanoTime() - theStart) / 1000L);
                    theTarget.errors++;
                    PROGRESS_ERRORS.incrementAndGet();
                }
                PROGRESS_DONE.incrementAndGet();
            }
        } catch (Exception e) {
            System.out.println("slice failed: " + e);
        } finally {
            try {
                theClient.closeGracefully();
            } catch (RuntimeException ignored) {
                // Best effort; the numbers are already collected.
            }
        }
    }

    /** Arguments for a tool. Extend here when a run needs a tool with a different signature. */
    private static Map<String, Object> argsFor(String theTool, int theSeq) {
        if (theTool.equals("single_param_func")) {
            return Map.of("a_param", Integer.valueOf(theSeq % 1000));
        }
        return Map.of();
    }

    private static Thread startReporter(int theCalls, boolean theTimed, long theSeconds,
            long theEvery, long theWallStart) {
        Thread theReporter = new Thread(() -> {
            long theNextMark = theEvery;
            try {
                while (!STOP && !Thread.currentThread().isInterrupted()) {
                    Thread.sleep(1000);
                    long theDone = PROGRESS_DONE.get();
                    long theElapsedMs = (System.nanoTime() - theWallStart) / 1_000_000L;
                    if (theTimed) {
                        // Report on a clock, not a call count: the whole point of a timed run is
                        // that the rate is not known in advance.
                        if (theElapsedMs / 1000 >= theNextMark) {
                            System.out.printf("progress %,d calls  %.1f/sec  errors %,d  elapsed %s"
                                            + "  remaining %s%n", theDone,
                                    theDone * 1000.0 / Math.max(1, theElapsedMs), PROGRESS_ERRORS.get(),
                                    hms(theElapsedMs / 1000), hms(theSeconds - theElapsedMs / 1000));
                            System.out.flush();
                            theNextMark += 30;   // every 30s
                        }
                    } else if (theDone >= theNextMark) {
                        double theRate = theDone * 1000.0 / Math.max(1, theElapsedMs);
                        System.out.printf("progress %,d/%,d  %.1f/sec  errors %,d  elapsed %s  eta %s%n",
                                theDone, theCalls, theRate, PROGRESS_ERRORS.get(),
                                hms(theElapsedMs / 1000),
                                hms((long) ((theCalls - theDone) / Math.max(0.001, theRate))));
                        System.out.flush();
                        theNextMark = ((theDone / theEvery) + 1) * theEvery;
                    }
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

    private static void report(String[] theTools, Samples[][] theSamples, long theWallMs,
            int theThreads) {
        long theTotal = 0;
        long theErrors = 0;
        System.out.println();
        System.out.println("=== results ===");
        for (int k = 0; k < theTools.length; k++) {
            LongList theMerged = new LongList();
            LongList theValues = new LongList();
            long theToolErrors = 0;
            for (int t = 0; t < theSamples.length; t++) {
                for (long v : theSamples[t][k].latencies.toArray()) {
                    theMerged.add(v);
                }
                for (long v : theSamples[t][k].values.toArray()) {
                    theValues.add(v);
                }
                theToolErrors += theSamples[t][k].errors;
            }
            long[] theLat = theMerged.toArray();
            Arrays.sort(theLat);
            theTotal += theLat.length;
            theErrors += theToolErrors;
            System.out.printf("%-24s calls %,9d   errors %,d%n", theTools[k], theLat.length, theToolErrors);
            if (theLat.length > 0) {
                System.out.printf("%-24s ms    min %s  p50 %s  p90 %s  p99 %s  p999 %s  max %s%n", "",
                        ms(theLat[0]), ms(theLat[theLat.length / 2]),
                        ms(theLat[(int) (theLat.length * 0.90)]), ms(theLat[(int) (theLat.length * 0.99)]),
                        ms(theLat[(int) (theLat.length * 0.999)]), ms(theLat[theLat.length - 1]));
            }
            if (theValues.size() > 0) {
                long[] theSorted = theValues.toArray();
                Arrays.sort(theSorted);
                long theDuplicates = 0;
                for (int i = 1; i < theSorted.length; i++) {
                    if (theSorted[i] == theSorted[i - 1]) {
                        theDuplicates++;
                    }
                }
                System.out.printf("%-24s seq   %,d..%,d  distinct %,d  duplicates %,d  %s%n", "",
                        theSorted[0], theSorted[theSorted.length - 1],
                        theSorted.length - theDuplicates, theDuplicates,
                        theDuplicates == 0 ? "(correct)" : "*** WRONG ***");
                System.out.printf("%-24s span  %,d values over %,d calls  %s%n", "",
                        theSorted[theSorted.length - 1] - theSorted[0] + 1, theSorted.length,
                        (theSorted[theSorted.length - 1] - theSorted[0] + 1) == theSorted.length
                                ? "(contiguous - none lost)" : "(gaps)");
            }
        }
        System.out.println();
        System.out.printf("total       : %,d calls, %,d errors%n", theTotal, theErrors);
        System.out.printf("wall clock  : %s  (%.1f calls/sec, %d thread(s))%n",
                hms(theWallMs / 1000), theTotal * 1000.0 / Math.max(1, theWallMs), theThreads);
    }

    private static String hms(long theSeconds) {
        if (theSeconds < 0) {
            theSeconds = 0;
        }
        return String.format("%d:%02d:%02d", theSeconds / 3600, (theSeconds % 3600) / 60, theSeconds % 60);
    }

    private static String ms(long theMicros) {
        return String.format("%.2f", theMicros / 1000.0);
    }

    private static long extractNumber(String theText) {
        StringBuilder theDigits = new StringBuilder();
        for (char theChar : theText.toCharArray()) {
            if (Character.isDigit(theChar)) {
                theDigits.append(theChar);
            } else if (theDigits.length() > 0) {
                break;
            }
        }
        return theDigits.length() == 0 ? -1 : Long.parseLong(theDigits.toString());
    }
}
