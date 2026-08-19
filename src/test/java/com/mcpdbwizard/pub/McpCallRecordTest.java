package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the per-call record the generated MCP server writes.
 *
 * <p>Two properties matter more than the formatting. The line must stay <em>one</em> line and stay
 * parseable whatever a caller puts in an argument name — a raw newline would split the record in two
 * and let a crafted name forge a second one. And argument <em>values</em> must never appear: they
 * were chosen by a model and can carry anything, which is exactly why only names are recorded.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class McpCallRecordTest {

    private static Map<String, Object> arguments(String... theNamesAndValues) {
        Map<String, Object> theArguments = new LinkedHashMap<String, Object>();
        for (int i = 0; i < theNamesAndValues.length; i += 2) {
            theArguments.put(theNamesAndValues[i], theNamesAndValues[i + 1]);
        }
        return theArguments;
    }

    @Test
    void aSuccessfulCallReadsAsOneGreppableLine() {
        assertEquals("MCP-CALL {\"tool\":\"ob_gen_pkg_greet\",\"outcome\":\"ok\",\"ms\":12,"
                        + "\"args\":[\"p_name\"]}",
                McpCallRecord.line("ob_gen_pkg_greet", arguments("p_name", "Ada"),
                        McpCallRecord.OUTCOME_OK, 12));
    }

    @Test
    void argumentValuesAreNeverRecorded() {
        String theLine = McpCallRecord.line("get_customer",
                arguments("email", "ada@example.com", "ssn", "123-45-6789"),
                McpCallRecord.OUTCOME_OK, 3);

        assertTrue(theLine.contains("\"email\""));
        assertTrue(theLine.contains("\"ssn\""));
        assertFalse(theLine.contains("ada@example.com"), "an argument value must never reach the log");
        assertFalse(theLine.contains("123-45-6789"), "an argument value must never reach the log");
    }

    @Test
    void aCallWithNoArgumentsIsStillWellFormed() {
        assertEquals("MCP-CALL {\"tool\":\"ob_gen_seq_nextval\",\"outcome\":\"ok\",\"ms\":1,\"args\":[]}",
                McpCallRecord.line("ob_gen_seq_nextval", arguments(), McpCallRecord.OUTCOME_OK, 1));
    }

    @Test
    void nullArgumentsAreToleratedRatherThanThrowing() {
        // A record that throws would turn a working tool call into a failure.
        assertEquals("MCP-CALL {\"tool\":\"t\",\"outcome\":\"ok\",\"ms\":0,\"args\":[]}",
                McpCallRecord.line("t", null, McpCallRecord.OUTCOME_OK, 0));
    }

    @Test
    void everyFailureModeIsDistinguishable() {
        // Pool exhaustion in particular: it is load, not a fault, and reads as a database error
        // unless it has its own outcome.
        assertTrue(McpCallRecord.line("t", null, McpCallRecord.OUTCOME_POOL_EXHAUSTED, 5)
                .contains("\"outcome\":\"pool-exhausted\""));
        assertTrue(McpCallRecord.line("t", null, McpCallRecord.OUTCOME_DATABASE_ERROR, 5)
                .contains("\"outcome\":\"database-error\""));
        assertTrue(McpCallRecord.line("t", null, McpCallRecord.OUTCOME_DOCUMENT_CHANGED, 5)
                .contains("\"outcome\":\"document-changed\""));
        assertTrue(McpCallRecord.line("t", null, McpCallRecord.OUTCOME_NOT_FOUND, 5)
                .contains("\"outcome\":\"not-found\""));
    }

    @Test
    void aCraftedArgumentNameCannotForgeASecondRecord() {
        String theLine = McpCallRecord.line("t",
                arguments("x\",\"y\nMCP-CALL {\"tool\":\"forged", "v"),
                McpCallRecord.OUTCOME_OK, 1);

        assertEquals(1, theLine.split("\n", -1).length, "the record must stay a single line");
        assertTrue(theLine.contains("\\n"), "the newline is escaped, not emitted");
        assertTrue(theLine.endsWith("]}"), "and the real record still terminates normally");
    }

    @Test
    void controlCharactersAndBackslashesAreEscaped() {
        // Built explicitly: a raw control byte in a source file is invisible and easily
        // mangled by an editor or a patch tool.
        String theName = "a\\b\tc" + ((char) 1) + "d";
        String theLine = McpCallRecord.line("t", arguments(theName, "v"),
                McpCallRecord.OUTCOME_OK, 1);

        assertTrue(theLine.contains("a\\\\b\\tc\\u0001d"), "got: " + theLine);
    }

    @Test
    void thePrefixIsStableBecauseOperatorsGrepForIt() {
        assertEquals("MCP-CALL", McpCallRecord.PREFIX);
        assertTrue(McpCallRecord.line("t", null, McpCallRecord.OUTCOME_OK, 1)
                .startsWith(McpCallRecord.PREFIX + " {"));
    }
}
