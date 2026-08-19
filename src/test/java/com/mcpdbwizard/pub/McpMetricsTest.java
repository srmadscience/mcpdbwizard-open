package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Database-free tests for the MCP metrics registry.
 *
 * <p>Two things here are worth more than the rest. The <b>quantile</b> tests pin exact values on
 * small sample sets, because a quantile is the kind of number nobody checks by eye and an
 * off-by-one in the rank goes unnoticed for years. The <b>exposition-format</b> tests pin the text
 * a scraper parses: it is a wire format, and a stray locale-formatted decimal or an unescaped quote
 * makes the whole scrape unparseable rather than one series wrong.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class McpMetricsTest {

    private static McpMetrics registry() {
        return McpMetrics.forServer("DaoFactoryMcpServer");
    }

    // ---- quantiles ------------------------------------------------------

    @Test
    void theQuantileIsTheSampleAtTheNearestRank() {
        long[] theSamples = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
        // ceil(0.5 * 10) = 5, so the 5th sample (1-based) = 50.
        assertEquals(50L, McpMetrics.quantile(theSamples.clone(), 0.5));
        assertEquals(80L, McpMetrics.quantile(theSamples.clone(), 0.75));
        assertEquals(90L, McpMetrics.quantile(theSamples.clone(), 0.9));
    }

    @Test
    void aQuantileSortsFirstSoSampleOrderDoesNotMatter() {
        long[] theShuffled = {90, 10, 50, 100, 30, 70, 20, 80, 40, 60};
        assertEquals(50L, McpMetrics.quantile(theShuffled.clone(), 0.5));
    }

    @Test
    void oneSampleIsEveryQuantile() {
        assertEquals(7L, McpMetrics.quantile(new long[] {7}, 0.5));
        assertEquals(7L, McpMetrics.quantile(new long[] {7}, 0.9));
    }

    @Test
    void noSamplesIsZeroRatherThanAnIndexError() {
        assertEquals(0L, McpMetrics.quantile(new long[0], 0.5));
        assertEquals(0L, McpMetrics.quantile(null, 0.5));
    }

    @Test
    void theRankNeverRunsOffEitherEndOfTheSamples() {
        long[] theSamples = {1, 2, 3};
        // q=0 would give ceil(0)-1 = -1, and q=1 would give the length itself.
        assertEquals(1L, McpMetrics.quantile(theSamples.clone(), 0.0));
        assertEquals(3L, McpMetrics.quantile(theSamples.clone(), 1.0));
    }

    // ---- seconds --------------------------------------------------------

    @Test
    void microsecondsBecomeSecondsWithSixDecimals() {
        // The unit is microseconds because milliseconds stopped being enough: calls are ~3 ms, so a
        // 1 ms quantum was a third of a call and anything under half a millisecond recorded as 0.
        assertEquals("0.000000", McpMetrics.seconds(0));
        assertEquals("0.000009", McpMetrics.seconds(9));
        assertEquals("0.000900", McpMetrics.seconds(900));
        assertEquals("0.001234", McpMetrics.seconds(1234));
        assertEquals("1.000000", McpMetrics.seconds(1000000));
        assertEquals("99.173000", McpMetrics.seconds(99173000));
        // The left-pad is the part that breaks silently: 1234 micros is .001234 of a second, and
        // appending the remainder unpadded would publish .1234 -- a hundredfold overstatement.
        assertEquals("2.000001", McpMetrics.seconds(2000001));
    }

    @Test
    void aSubMillisecondCallIsNoLongerRecordedAsZero() {
        // The whole point of the change. Under milliseconds every one of these was "0.000".
        assertEquals("0.000400", McpMetrics.seconds(400));
        assertEquals("0.000001", McpMetrics.seconds(1));
        assertNotEquals("0.000000", McpMetrics.seconds(400));
    }

    @Test
    void theDecimalPointIsNeverALocaleComma() {
        // String.format would honour the default locale and write "0,009" in half of Europe, which
        // a scraper rejects outright. The formatting is built by hand for exactly this reason.
        assertFalse(McpMetrics.seconds(9).contains(","));
        assertFalse(McpMetrics.seconds(1234).contains(","));
    }

    // ---- escaping -------------------------------------------------------

    @Test
    void labelValuesAreEscaped() {
        assertEquals("plain", McpMetrics.escape("plain"));
        assertEquals("a\\\\b", McpMetrics.escape("a\\b"));
        assertEquals("say \\\"hi\\\"", McpMetrics.escape("say \"hi\""));
        assertEquals("one\\ntwo", McpMetrics.escape("one\ntwo"));
        assertEquals("", McpMetrics.escape(null));
    }

    @Test
    void aQuotedOracleIdentifierCannotBreakTheScrape() {
        // Oracle allows "odd""name" as a quoted identifier, so an object name really can carry a
        // double quote. Unescaped it would close the label early and corrupt every later line.
        McpMetrics theMetrics = registry();
        theMetrics.describe("odd_tool", "APPSCHEMA.\"ODD\"\"NAME\"", "table");
        theMetrics.record("odd_tool", McpCallRecord.OUTCOME_OK, 5, 0, 0);

        String theScrape = theMetrics.scrape();
        assertTrue(theScrape.contains("db_object=\"APPSCHEMA.\\\"ODD\\\"\\\"NAME\\\"\""), theScrape);
    }

    // ---- what a scrape says ---------------------------------------------

    @Test
    void aToolThatWasNeverCalledHasNoSeries() {
        // Describing 1,682 tools must not create 1,682 counters sitting at zero.
        McpMetrics theMetrics = registry();
        theMetrics.describe("ob_gen_pkg_greet", "APPSCHEMA.FIXTURE_PKG.GREET", "procedure");

        String theScrape = theMetrics.scrape();
        assertFalse(theScrape.contains("ob_gen_pkg_greet"), theScrape);
        // The HELP/TYPE headers are still there, so a scraper sees a well-formed empty registry.
        assertTrue(theScrape.contains("# TYPE mcpdbwizard_mcp_calls_total counter"), theScrape);
    }

    @Test
    void aCallCarriesTheDatabaseObjectItReached() {
        McpMetrics theMetrics = registry();
        theMetrics.describe("ob_gen_pkg_greet", "APPSCHEMA.FIXTURE_PKG.GREET", "procedure");
        theMetrics.record("ob_gen_pkg_greet", McpCallRecord.OUTCOME_OK, 12, 40, 900);

        String theScrape = theMetrics.scrape();
        assertTrue(theScrape.contains("mcpdbwizard_mcp_calls_total{host=\"" + McpMetrics.hostName() + "\",config=\"unknown\",server=\"DaoFactoryMcpServer\","
                + "tool=\"ob_gen_pkg_greet\",db_object=\"APPSCHEMA.FIXTURE_PKG.GREET\","
                + "object_type=\"procedure\",outcome=\"ok\"} 1"), theScrape);
        assertTrue(theScrape.contains("mcpdbwizard_mcp_request_bytes_total{host=\"" + McpMetrics.hostName() + "\",config=\"unknown\",server=\"DaoFactoryMcpServer\","
                + "tool=\"ob_gen_pkg_greet\",db_object=\"APPSCHEMA.FIXTURE_PKG.GREET\","
                + "object_type=\"procedure\"} 40"), theScrape);
        assertTrue(theScrape.contains("object_type=\"procedure\"} 900"), theScrape);
    }

    @Test
    void severalToolsOnOneObjectAggregateByTheObjectLabel() {
        // A table yields at least four tools. The point of carrying db_object on every series is
        // that "how busy is this table?" is one PromQL sum rather than a join.
        McpMetrics theMetrics = registry();
        theMetrics.describe("ob_gen_23ai_get_by_pk", "APPSCHEMA.FIXTURE_TABLE", "table");
        theMetrics.describe("ob_gen_23ai_insert", "APPSCHEMA.FIXTURE_TABLE", "table");
        theMetrics.record("ob_gen_23ai_get_by_pk", McpCallRecord.OUTCOME_OK, 3, 10, 20);
        theMetrics.record("ob_gen_23ai_insert", McpCallRecord.OUTCOME_OK, 4, 10, 20);

        String theScrape = theMetrics.scrape();
        assertEquals(2, countOccurrences(theScrape,
                "mcpdbwizard_mcp_calls_total{host=\"" + McpMetrics.hostName() + "\",config=\"unknown\",server=\"DaoFactoryMcpServer\",tool=\""));
        // Nine series per tool - one counter, three quantiles, sum, count, max and the two byte
        // counters - and every one of them carries the object, so none is lost to a sum by it.
        assertEquals(18, countOccurrences(theScrape, "db_object=\"APPSCHEMA.FIXTURE_TABLE\""));
    }

    @Test
    void outcomesAreCountedSeparatelyButShareOneLatencySummary() {
        // Splitting the quantiles across seven outcomes would compute each from a handful of
        // samples. The counter is where outcome belongs; the summary is per tool.
        McpMetrics theMetrics = registry();
        theMetrics.describe("t", "S.T", "table");
        theMetrics.record("t", McpCallRecord.OUTCOME_OK, 10000, 0, 0);          // 10 ms
        theMetrics.record("t", McpCallRecord.OUTCOME_OK, 20000, 0, 0);          // 20 ms
        theMetrics.record("t", McpCallRecord.OUTCOME_DATABASE_ERROR, 30000, 0, 0);

        String theScrape = theMetrics.scrape();
        assertTrue(theScrape.contains("outcome=\"ok\"} 2"), theScrape);
        assertTrue(theScrape.contains("outcome=\"database-error\"} 1"), theScrape);
        assertTrue(theScrape.contains("call_duration_seconds_count{host=\"" + McpMetrics.hostName() + "\",config=\"unknown\",server=\"DaoFactoryMcpServer\","
                + "tool=\"t\",db_object=\"S.T\",object_type=\"table\"} 3"), theScrape);
        assertTrue(theScrape.contains("call_duration_seconds_sum{host=\"" + McpMetrics.hostName() + "\",config=\"unknown\",server=\"DaoFactoryMcpServer\","
                + "tool=\"t\",db_object=\"S.T\",object_type=\"table\"} 0.060000"), theScrape);
        assertFalse(theScrape.contains("call_duration_seconds{host=\"" + McpMetrics.hostName() + "\",config=\"unknown\",server=\"DaoFactoryMcpServer\","
                + "tool=\"t\",db_object=\"S.T\",object_type=\"table\",outcome="), theScrape);
    }

    @Test
    void theMaximumIsSinceStartUpNotOverTheWindow() {
        // The whole reason for a separate max family: the worst call is the one being looked for,
        // and a windowed max quietly throws it away once WINDOW newer calls have happened.
        McpMetrics theMetrics = registry();
        theMetrics.describe("t", "S.T", "table");
        theMetrics.record("t", McpCallRecord.OUTCOME_OK, 9999000, 0, 0);        // 9.999 s
        for (int seq = 0; seq < McpMetrics.WINDOW + 10; seq++) {
            theMetrics.record("t", McpCallRecord.OUTCOME_OK, 1000, 0, 0);       // 1 ms
        }

        String theScrape = theMetrics.scrape();
        assertTrue(theScrape.contains("call_duration_seconds_max{host=\"" + McpMetrics.hostName() + "\",config=\"unknown\",server=\"DaoFactoryMcpServer\","
                + "tool=\"t\",db_object=\"S.T\",object_type=\"table\"} 9.999000"), theScrape);
        // ...while the quantiles have moved on, which is what makes them useful.
        assertTrue(theScrape.contains("quantile=\"0.9\"} 0.001000"), theScrape);
    }

    @Test
    void theWindowKeepsOnlyTheMostRecentCalls() {
        McpMetrics theMetrics = registry();
        theMetrics.describe("t", "S.T", "table");
        // Fill the window with slow calls, then push it entirely over with fast ones.
        for (int seq = 0; seq < McpMetrics.WINDOW; seq++) {
            theMetrics.record("t", McpCallRecord.OUTCOME_OK, 500000, 0, 0);     // 500 ms
        }
        for (int seq = 0; seq < McpMetrics.WINDOW; seq++) {
            theMetrics.record("t", McpCallRecord.OUTCOME_OK, 2000, 0, 0);       // 2 ms
        }

        String theScrape = theMetrics.scrape();
        assertTrue(theScrape.contains("quantile=\"0.5\"} 0.002000"), theScrape);
        // The cumulative count is untouched: the window is a latency device, not a counter reset.
        assertTrue(theScrape.contains("call_duration_seconds_count{host=\"" + McpMetrics.hostName() + "\",config=\"unknown\",server=\"DaoFactoryMcpServer\","
                + "tool=\"t\",db_object=\"S.T\",object_type=\"table\"} " + (McpMetrics.WINDOW * 2)),
                theScrape);
    }

    @Test
    void anUndescribedToolIsStillCounted() {
        // A tool surface added without going through the name helper must show up as something
        // rather than vanish. It is visible AND obviously unlabelled, which is the point.
        McpMetrics theMetrics = registry();
        theMetrics.record("mystery_tool", McpCallRecord.OUTCOME_OK, 1, 0, 0);

        String theScrape = theMetrics.scrape();
        assertTrue(theScrape.contains("tool=\"mystery_tool\",db_object=\"\",object_type=\"unknown\""),
                theScrape);
    }

    @Test
    void aCallWithNoToolNameIsRecordedRatherThanDropped() {
        McpMetrics theMetrics = registry();
        theMetrics.record(null, McpCallRecord.OUTCOME_ERROR, 1, 0, 0);
        theMetrics.record("   ", McpCallRecord.OUTCOME_ERROR, 1, 0, 0);

        String theScrape = theMetrics.scrape();
        assertTrue(theScrape.contains("tool=\"unknown\""), theScrape);
        assertTrue(theScrape.contains("outcome=\"error\"} 2"), theScrape);
    }

    @Test
    void aMissingOutcomeIsAnError() {
        McpMetrics theMetrics = registry();
        theMetrics.record("t", null, 1, 0, 0);
        assertTrue(theMetrics.scrape().contains("outcome=\"error\"} 1"));
    }

    @Test
    void negativeMeasurementsDoNotCorruptTheCounters() {
        // A clock that steps backwards must not turn a monotonic counter into a decreasing one,
        // which Prometheus reads as a process restart.
        McpMetrics theMetrics = registry();
        theMetrics.record("t", McpCallRecord.OUTCOME_OK, -5, -10, -20);

        String theScrape = theMetrics.scrape();
        assertTrue(theScrape.contains("request_bytes_total{host=\"" + McpMetrics.hostName() + "\",config=\"unknown\",server=\"DaoFactoryMcpServer\","
                + "tool=\"t\",db_object=\"\",object_type=\"unknown\"} 0"), theScrape);
        assertTrue(theScrape.contains("call_duration_seconds_max{host=\"" + McpMetrics.hostName() + "\",config=\"unknown\",server=\"DaoFactoryMcpServer\","
                + "tool=\"t\",db_object=\"\",object_type=\"unknown\"} 0.000"), theScrape);
    }

    @Test
    void seriesAreOrderedSoTwoScrapesCanBeDiffed() {
        McpMetrics theMetrics = registry();
        theMetrics.record("zebra", McpCallRecord.OUTCOME_OK, 1, 0, 0);
        theMetrics.record("alpha", McpCallRecord.OUTCOME_OK, 1, 0, 0);

        String theScrape = theMetrics.scrape();
        assertTrue(theScrape.indexOf("tool=\"alpha\"") < theScrape.indexOf("tool=\"zebra\""),
                theScrape);
    }

    @Test
    void everyFamilyDeclaresItsTypeBeforeItsSamples() {
        McpMetrics theMetrics = registry();
        theMetrics.record("t", McpCallRecord.OUTCOME_OK, 1, 1, 1);

        String theScrape = theMetrics.scrape();
        String[] theFamilies = {"mcpdbwizard_mcp_calls_total", "mcpdbwizard_mcp_call_duration_seconds",
                "mcpdbwizard_mcp_call_duration_seconds_max", "mcpdbwizard_mcp_request_bytes_total",
                "mcpdbwizard_mcp_response_bytes_total"};
        for (int seq = 0; seq < theFamilies.length; seq++) {
            assertTrue(theScrape.contains("# HELP " + theFamilies[seq] + " "),
                    theFamilies[seq] + " has no HELP:\n" + theScrape);
            assertTrue(theScrape.contains("# TYPE " + theFamilies[seq] + " "),
                    theFamilies[seq] + " has no TYPE:\n" + theScrape);
        }
    }

    @Test
    void noPoolMeansNoPoolSeries() {
        McpMetrics theMetrics = registry();
        theMetrics.record("t", McpCallRecord.OUTCOME_OK, 1, 0, 0);
        assertFalse(theMetrics.scrape().contains("mcpdbwizard_mcp_pool_"));
    }

    /** Minimal pooled factory: the pool's own behaviour is tested in {@link DaoFactoryPoolTest}. */
    private static final class MetricsStubFactory implements PooledResourceUser {
        public void confirmConnection() { }

        public boolean isConnectionUsable() {
            return true;
        }

        public void settleTransaction(boolean commit) { }

        public boolean releaseResources() {
            return true;
        }

        public boolean hasResources() {
            return false;
        }

        public void closeFactory() { }
    }

    @Test
    void aBoundPoolExportsItsCountersIncludingFailedCreations() throws Exception {
        McpMetrics theMetrics = registry();
        DaoFactoryPoolConfig theConfig = new DaoFactoryPoolConfig()
                .setMaxSize(2).setMinIdle(0).setMaxWaitMillis(500L).setIdleTimeoutMillis(60000L);

        try (DaoFactoryPool<MetricsStubFactory> thePool =
                     new DaoFactoryPool<MetricsStubFactory>(MetricsStubFactory::new, theConfig, null)) {
            thePool.release(thePool.borrow());
            theMetrics.bindPool(thePool);

            String theScrape = theMetrics.scrape();
            String[] theFamilies = {
                    "mcpdbwizard_mcp_pool_active",
                    "mcpdbwizard_mcp_pool_idle",
                    "mcpdbwizard_mcp_pool_max",
                    "mcpdbwizard_mcp_pool_borrowed_total",
                    "mcpdbwizard_mcp_pool_created_total",
                    "mcpdbwizard_mcp_pool_destroyed_total",
                    "mcpdbwizard_mcp_pool_create_failed_total"};
            for (int seq = 0; seq < theFamilies.length; seq++) {
                assertTrue(theScrape.contains("# HELP " + theFamilies[seq] + " "),
                        theFamilies[seq] + " missing from scrape:\n" + theScrape);
            }
        }
    }

    @Test
    void refusedLogonsAreScrapableRatherThanOnlyLogged() throws Exception {
        // A database turning connections away is the case that has no other signal: created and
        // destroyed both simply stop moving, so without this series the scrape looks quiet.
        McpMetrics theMetrics = registry();
        DaoFactoryPoolConfig theConfig = new DaoFactoryPoolConfig()
                .setMaxSize(1).setMinIdle(0).setMaxWaitMillis(200L).setIdleTimeoutMillis(60000L);

        try (DaoFactoryPool<MetricsStubFactory> thePool =
                     new DaoFactoryPool<MetricsStubFactory>(() -> {
                         throw new IllegalStateException("ORA-12516: no handler ready");
                     }, theConfig, null)) {
            theMetrics.bindPool(thePool);
            assertThrows(CSException.class, thePool::borrow);

            assertTrue(theMetrics.scrape().contains("mcpdbwizard_mcp_pool_create_failed_total"));
            assertTrue(thePool.getCreateFailedCount() >= 1);
        }
    }

    // ---- concurrency ----------------------------------------------------

    @Test
    void concurrentCallsAreAllCounted() throws Exception {
        // record() runs on request threads while a scrape runs on the exporter's. A lost update
        // here would show as a counter that drifts below the truth, which nothing would ever flag.
        final McpMetrics theMetrics = registry();
        theMetrics.describe("t", "S.T", "table");

        final int theThreads = 8;
        final int thePerThread = 500;
        final CountDownLatch theStart = new CountDownLatch(1);
        final CountDownLatch theDone = new CountDownLatch(theThreads);
        List<Thread> theWorkers = new ArrayList<Thread>();
        for (int seq = 0; seq < theThreads; seq++) {
            Thread theWorker = new Thread(new Runnable() {
                public void run() {
                    try {
                        theStart.await();
                        for (int call = 0; call < thePerThread; call++) {
                            theMetrics.record("t", McpCallRecord.OUTCOME_OK, 1, 2, 3);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        theDone.countDown();
                    }
                }
            });
            theWorker.start();
            theWorkers.add(theWorker);
        }
        theStart.countDown();
        // Scrape while they run, to prove a reader cannot see a half-written tool.
        for (int seq = 0; seq < 20; seq++) {
            theMetrics.scrape();
        }
        theDone.await();
        for (int seq = 0; seq < theWorkers.size(); seq++) {
            theWorkers.get(seq).join();
        }

        int theExpected = theThreads * thePerThread;
        String theScrape = theMetrics.scrape();
        assertTrue(theScrape.contains("outcome=\"ok\"} " + theExpected), theScrape);
        assertTrue(theScrape.contains("request_bytes_total{host=\"" + McpMetrics.hostName() + "\",config=\"unknown\",server=\"DaoFactoryMcpServer\",tool=\"t\","
                + "db_object=\"S.T\",object_type=\"table\"} " + (theExpected * 2)), theScrape);
    }

    private static int countOccurrences(String theText, String theNeedle) {
        int theCount = 0;
        int theIndex = theText.indexOf(theNeedle);
        while (theIndex >= 0) {
            theCount++;
            theIndex = theText.indexOf(theNeedle, theIndex + theNeedle.length());
        }
        return theCount;
    }

    // ---- audit counters ------------------------------------------------------------------------

    /** A sink whose counters are whatever the test wants, including "does not report". */
    private static McpAuditSink sink(final long theDelivered, final long theDropped,
                                     final long thePending) {
        return new McpAuditSink() {
            @Override public void record(McpAuditEvent theEvent) { }
            @Override public void close() { }
            @Override public long getDeliveredCount() { return theDelivered; }
            @Override public long getDroppedCount() { return theDropped; }
            @Override public long getPendingCount() { return thePending; }
        };
    }

    @Test
    void anAuditSinkIsExportedWithItsCounters() {
        McpMetrics theMetrics = McpMetrics.forServer("srv");
        theMetrics.bindAuditSink(sink(120L, 3L, 7L));
        String theScrape = theMetrics.scrape();

        assertTrue(theScrape.contains("mcpdbwizard_mcp_audit_dropped_total{host=\"" + McpMetrics.hostName() + "\",config=\"unknown\",server=\"srv\"} 3"),
                theScrape);
        assertTrue(theScrape.contains("mcpdbwizard_mcp_audit_delivered_total{host=\"" + McpMetrics.hostName() + "\",config=\"unknown\",server=\"srv\"} 120"),
                theScrape);
        assertTrue(theScrape.contains("mcpdbwizard_mcp_audit_pending{host=\"" + McpMetrics.hostName() + "\",config=\"unknown\",server=\"srv\"} 7"), theScrape);
    }

    /**
     * THE rule. A sink that does not count reports -1, and -1 must never be EXPORTED: Prometheus
     * would graph it, average it and alert on it as though it were a measurement. An absent series
     * is how a monitoring system expresses "unknown", and a wrong number is not.
     */
    @Test
    void aSinkThatDoesNotCountExportsNoSeriesRatherThanMinusOne() {
        McpMetrics theMetrics = McpMetrics.forServer("srv");
        theMetrics.bindAuditSink(sink(-1L, -1L, -1L));
        String theScrape = theMetrics.scrape();

        assertFalse(theScrape.contains("audit_dropped_total"), theScrape);
        assertFalse(theScrape.contains("audit_delivered_total"), theScrape);
        assertFalse(theScrape.contains("audit_pending"), theScrape);
        assertFalse(theScrape.contains("-1"), "a -1 must never reach a scrape: " + theScrape);
    }

    /** Counters are independent: a sink may report some and not others. */
    @Test
    void onlyTheCountersASinkReportsAreExported() {
        McpMetrics theMetrics = McpMetrics.forServer("srv");
        theMetrics.bindAuditSink(sink(50L, 0L, -1L));
        String theScrape = theMetrics.scrape();

        assertTrue(theScrape.contains("audit_delivered_total"), theScrape);
        assertTrue(theScrape.contains("audit_dropped_total{host=\"" + McpMetrics.hostName() + "\",config=\"unknown\",server=\"srv\"} 0"),
                "zero drops is a measurement and must be exported: " + theScrape);
        assertFalse(theScrape.contains("audit_pending"), theScrape);
    }

    /** An unaudited server exports nothing at all, exactly as an unpooled one does. */
    @Test
    void anUnauditedServerExportsNoAuditSeries() {
        String theScrape = McpMetrics.forServer("srv").scrape();
        assertFalse(theScrape.contains("audit_"), theScrape);
    }

    // ---- the host label ------------------------------------------------------------------------

    /**
     * Every series must carry it. `server` is the generated server's CLASS name, so every config
     * that keeps the default factory name reports `DaoFactoryMcpServer` — several servers on one
     * host, or one config across several hosts, collapse into a single set of series without this.
     */
    @Test
    void everySeriesCarriesTheHostLabel() {
        McpMetrics theMetrics = McpMetrics.forServer("srv");
        theMetrics.bindAuditSink(new McpAuditSink() {
            @Override public void record(McpAuditEvent theEvent) { }
            @Override public void close() { }
            @Override public long getDroppedCount() { return 0L; }
            @Override public long getDeliveredCount() { return 1L; }
        });
        theMetrics.record("a_tool", McpCallRecord.OUTCOME_OK, 5L, 10L, 20L);

        for (String theLine : theMetrics.scrape().split("\n")) {
            if (theLine.startsWith("mcpdbwizard_")) {
                assertTrue(theLine.contains("host=\""),
                        "series without a host label: " + theLine);
            }
        }
    }

    /** The host comes first, so a reader sees WHERE before WHAT. */
    @Test
    void theHostLabelLeadsTheLabelSet() {
        McpMetrics theMetrics = McpMetrics.forServer("srv");
        theMetrics.record("a_tool", McpCallRecord.OUTCOME_OK, 1L, 0L, 0L);
        assertTrue(theMetrics.scrape().contains("{host=\""), theMetrics.scrape());
    }

    /**
     * A resolved name is never blank. An empty label reads as "this series has no host", which is a
     * different and wrong claim from "the host could not be named".
     */
    @Test
    void theHostNameIsNeverEmpty() {
        assertNotNull(McpMetrics.hostName());
        assertFalse(McpMetrics.hostName().isEmpty());
    }

    // ---- the config label ----------------------------------------------------------------------

    /**
     * The label that finally makes a series identify ONE process. host says which machine and
     * server says which class -- and every config keeping the default factory name emits
     * DaoFactoryMcpServer, so twenty servers on one host shared a label set until this existed.
     */
    @Test
    void everySeriesCarriesTheConfigLabel() {
        McpMetrics theMetrics = McpMetrics.forServer("srv", "com.example.payroll");
        theMetrics.record("a_tool", McpCallRecord.OUTCOME_OK, 5L, 10L, 20L);
        for (String theLine : theMetrics.scrape().split("\n")) {
            if (theLine.startsWith("mcpdbwizard_")) {
                assertTrue(theLine.contains("config=\"com.example.payroll\""),
                        "series without a config label: " + theLine);
            }
        }
    }

    /**
     * Two servers that differ ONLY by config must produce distinguishable series. This is the whole
     * point, so it is asserted directly rather than inferred from the label being present.
     */
    @Test
    void twoConfigsOnOneHostDoNotCollide() {
        McpMetrics thePayroll = McpMetrics.forServer("DaoFactoryMcpServer", "com.example.payroll");
        McpMetrics theOrders = McpMetrics.forServer("DaoFactoryMcpServer", "com.example.orders");
        thePayroll.record("t", McpCallRecord.OUTCOME_OK, 1L, 0L, 0L);
        theOrders.record("t", McpCallRecord.OUTCOME_OK, 1L, 0L, 0L);

        assertNotEquals(thePayroll.scrape(), theOrders.scrape(),
                "identical series for two different configs is the collision this label fixes");
        assertTrue(thePayroll.scrape().contains("config=\"com.example.payroll\""));
        assertTrue(theOrders.scrape().contains("config=\"com.example.orders\""));
    }

    /** Never blank: "belongs to no config" is a different claim from "nobody said which". */
    @Test
    void anUnnamedConfigBecomesUnknownRatherThanEmpty() {
        assertEquals("unknown", McpMetrics.configName(null));
        assertEquals("unknown", McpMetrics.configName("   "));
        assertEquals("com.example.x", McpMetrics.configName("com.example.x"));
        // ...and it reaches the wire that way. A call has to be recorded first: with none, the
        // scrape carries headers and no series at all, so asserting on it proves nothing.
        McpMetrics theMetrics = McpMetrics.forServer("srv");
        theMetrics.record("t", McpCallRecord.OUTCOME_OK, 1L, 0L, 0L);
        assertTrue(theMetrics.scrape().contains("config=\"unknown\""), theMetrics.scrape());
    }
}
