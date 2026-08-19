package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cover for the in-protocol log message text. Mirrors {@code McpCallRecordTest}, and shares its
 * central concern: the argument NAMES cross, the values never do.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class McpProtocolLogTest {

    private static Map<String, Object> arguments() {
        Map<String, Object> theArguments = new LinkedHashMap<>();
        theArguments.put("p_name", "Alice");
        theArguments.put("p_id", 42);
        return theArguments;
    }

    @Test
    void theMessageCarriesToolOutcomeDurationAndArgumentNames() {
        String theLine = McpProtocolLog.line("ob_gen_pkg_greet", arguments(),
                McpCallRecord.OUTCOME_OK, 12L);
        assertEquals("ob_gen_pkg_greet outcome=ok ms=12 args=[p_name, p_id]", theLine);
    }

    /**
     * The rule the whole class exists to keep. A model chose these values, so they can carry
     * anything that was put in front of it — and unlike the operator-side record, this message goes
     * back over the wire. A regression here leaks rather than merely logs.
     */
    @Test
    void argumentValuesNeverAppear() {
        String theLine = McpProtocolLog.line("t", arguments(), McpCallRecord.OUTCOME_OK, 1L);
        assertFalse(theLine.contains("Alice"), "argument VALUES must never cross the protocol");
        assertFalse(theLine.contains("42"), "argument VALUES must never cross the protocol");
        assertTrue(theLine.contains("p_name"));
        assertTrue(theLine.contains("p_id"));
    }

    /** A tool that takes nothing is ordinary — a sequence nextval, for one. */
    @Test
    void noArgumentsGivesAnEmptyList() {
        assertEquals("ob_gen_seq_nextval outcome=ok ms=3 args=[]",
                McpProtocolLog.line("ob_gen_seq_nextval", null, McpCallRecord.OUTCOME_OK, 3L));
        assertEquals("t outcome=ok ms=0 args=[]",
                McpProtocolLog.line("t", new LinkedHashMap<>(), McpCallRecord.OUTCOME_OK, 0L));
    }

    /**
     * The funnel builds this in a {@code finally}, on the failure path as much as the success one,
     * where a null request is exactly what it may be holding. It must not throw there — that would
     * replace a reported failure with an unreported one.
     */
    @Test
    void nullsDoNotThrow() {
        assertDoesNotThrow(() -> McpProtocolLog.line(null, null, null, 0L));
        String theLine = McpProtocolLog.line(null, null, null, 5L);
        assertTrue(theLine.contains("ms=5"));
    }

    @Test
    void everyOutcomeIsCarriedThrough() {
        assertTrue(McpProtocolLog.line("t", null, McpCallRecord.OUTCOME_POOL_EXHAUSTED, 1L)
                .contains("outcome=pool-exhausted"));
        assertTrue(McpProtocolLog.line("t", null, McpCallRecord.OUTCOME_DATABASE_ERROR, 1L)
                .contains("outcome=database-error"));
    }

    /** The logger names the server so a client can tell two of them apart; never null or empty. */
    @Test
    void theLoggerNameFallsBackRatherThanBeingEmpty() {
        assertEquals("DaoFactoryMcpServer", McpProtocolLog.logger("DaoFactoryMcpServer"));
        assertEquals("mcp", McpProtocolLog.logger(null));
        assertEquals("mcp", McpProtocolLog.logger(""));
    }

    /**
     * Both records read the argument names through one shared helper, so they cannot drift apart.
     * If this fails, someone has copied the rule rather than reused it.
     */
    @Test
    void bothRecordsAgreeOnWhichNamesAreReported() {
        Map<String, Object> theArguments = arguments();
        String theProtocolLine = McpProtocolLog.line("t", theArguments, McpCallRecord.OUTCOME_OK, 1L);
        String theCallRecord = McpCallRecord.line("t", theArguments, McpCallRecord.OUTCOME_OK, 1L);
        for (String theName : theArguments.keySet()) {
            assertTrue(theProtocolLine.contains(theName), theName + " missing from the protocol line");
            assertTrue(theCallRecord.contains(theName), theName + " missing from the MCP-CALL record");
        }
    }
}
