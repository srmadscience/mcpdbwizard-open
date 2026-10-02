package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The name and value rules for MCP context parameters. Both the config editor and the generated
 * server ask these, so a name one accepts is a name the other can set.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class McpContextParamsTest {

    @Test
    void ordinaryNamesAreAcceptedAndLowerCaseIsNormalised() {
        assertNull(McpContextParams.nameProblem("CUSTOMER_ID"));
        assertNull(McpContextParams.nameProblem("customer_id"));
        assertNull(McpContextParams.nameProblem("R2"));
        assertEquals("CUSTOMER_ID", McpContextParams.normalise(" customer_id "));
    }

    @Test
    void thirtyCharactersIsTheLimitBecause12cIsTheLimit() {
        assertNull(McpContextParams.nameProblem("A".repeat(30)));
        assertNotNull(McpContextParams.nameProblem("A".repeat(31)));
    }

    @Test
    void namesThatAreNotSimpleIdentifiersAreRefused() {
        for (String theBad : Arrays.asList(null, "", "  ", "9LIVES", "_X", "CUSTOMER-ID", "A B", "A.B", "ÉTÉ")) {
            assertNotNull(McpContextParams.nameProblem(theBad), "accepted '" + theBad + "'");
        }
    }

    @Test
    void aListIsRefusedForADuplicateInAnyCase() {
        assertNull(McpContextParams.listProblem(List.of("CUSTOMER_ID", "REGION")));
        String theProblem = McpContextParams.listProblem(List.of("customer_id", "CUSTOMER_ID"));
        assertNotNull(theProblem);
        assertTrue(theProblem.contains("twice"), theProblem);
    }

    @Test
    void aListReportsItsFirstBadName() {
        String theProblem = McpContextParams.listProblem(List.of("OK", "NOT-OK"));
        assertNotNull(theProblem);
        assertTrue(theProblem.contains("NOT-OK"), theProblem);
    }

    /** Bytes, not characters: four thousand two-byte characters is eight thousand bytes. */
    @Test
    void theValueLimitCountsUtf8Bytes() {
        assertNull(McpContextParams.valueProblem("X", "a".repeat(4000)));
        assertNotNull(McpContextParams.valueProblem("X", "a".repeat(4001)));
        assertNull(McpContextParams.valueProblem("X", "é".repeat(2000)));
        assertNotNull(McpContextParams.valueProblem("X", "é".repeat(2001)));
    }

    /** Oracle stores '' as NULL, so an empty value is no value: ?CUSTOMER_ID= must not pass. */
    @Test
    void missingAndEmptyAreBothRefused() {
        assertNotNull(McpContextParams.valueProblem("X", null));
        assertNotNull(McpContextParams.valueProblem("X", ""));
        assertNull(McpContextParams.valueProblem("X", " "), "a space is a value; the procedure decides");
    }

    /** Missing package and missing grant both surface as PLS-00201; nothing else is "not installed". */
    @Test
    void notInstalledIsRecognisedAndNothingElseIs() {
        java.sql.SQLException theMissing = new java.sql.SQLException(
                "ORA-06550: line 1, column 7:\nPLS-00201: identifier 'MCPDBWIZARD.MCPDBWIZARD_CTX' must be declared");
        String theReason = McpContextParams.notInstalledReason(theMissing);
        assertNotNull(theReason);
        assertTrue(theReason.contains("install.sql") && theReason.contains("grant.sql"), theReason);

        assertNull(McpContextParams.notInstalledReason(new java.sql.SQLException(
                "ORA-20902: MCP context: parameter REGION has no value.")));
    }

    // ---- parsing what a request or a process supplies ----------------------------------

    private static final List<String> DECLARED = List.of("CUSTOMER_ID", "REGION");

    @Test
    void aCompleteQueryStringResolvesInDeclarationOrder() {
        McpContextParams.Resolved r = McpContextParams.fromQueryString("region=north&CUSTOMER_ID=42", DECLARED);
        assertNull(r.getProblem());
        assertEquals(List.of("CUSTOMER_ID", "REGION"), List.copyOf(r.getValues().keySet()));
        assertEquals("42", r.getValues().get("CUSTOMER_ID"));
        assertEquals("north", r.getValues().get("REGION"));
    }

    @Test
    void valuesAreUrlDecoded() {
        McpContextParams.Resolved r = McpContextParams.fromQueryString(
                "CUSTOMER_ID=a%20b%26c&REGION=x+y", DECLARED);
        assertEquals("a b&c", r.getValues().get("CUSTOMER_ID"));
        assertEquals("x y", r.getValues().get("REGION"));
    }

    @Test
    void everyDeclaredNameIsRequired() {
        String theProblem = McpContextParams.fromQueryString("CUSTOMER_ID=42", DECLARED).getProblem();
        assertNotNull(theProblem);
        assertTrue(theProblem.contains("REGION"), theProblem);
        assertNotNull(McpContextParams.fromQueryString(null, DECLARED).getProblem(), "no query at all");
    }

    @Test
    void anEmptyValueIsMissing() {
        String theProblem = McpContextParams.fromQueryString("CUSTOMER_ID=&REGION=north", DECLARED).getProblem();
        assertNotNull(theProblem);
        assertTrue(theProblem.contains("CUSTOMER_ID"), theProblem);
        assertNotNull(McpContextParams.fromQueryString("CUSTOMER_ID&REGION=north", DECLARED).getProblem(),
                "a bare name with no '=' is an empty value");
    }

    @Test
    void anUndeclaredNameIsRefusedAndNamed() {
        String theProblem = McpContextParams.fromQueryString(
                "CUSTOMER_ID=42&REGION=north&CUSTMER=1", DECLARED).getProblem();
        assertNotNull(theProblem);
        assertTrue(theProblem.contains("CUSTMER") && theProblem.contains("CUSTOMER_ID, REGION"), theProblem);
    }

    @Test
    void aRepeatedNameIsRefusedInAnyCase() {
        assertNotNull(McpContextParams.fromQueryString(
                "CUSTOMER_ID=42&customer_id=43&REGION=north", DECLARED).getProblem());
    }

    @Test
    void anOverlongValueIsRefusedNotTruncated() {
        assertNotNull(McpContextParams.fromQueryString(
                "CUSTOMER_ID=" + "v".repeat(4001) + "&REGION=north", DECLARED).getProblem());
    }

    @Test
    void aMalformedEscapeIsRefused() {
        assertNotNull(McpContextParams.fromQueryString("CUSTOMER_ID=%zz&REGION=north", DECLARED).getProblem());
    }

    @Test
    void stdioReadsOnlyTheDeclaredVariables() {
        java.util.Map<String, String> theEnv = new java.util.HashMap<>();
        theEnv.put("MCP_CONTEXT_CUSTOMER_ID", "42");
        theEnv.put("MCP_CONTEXT_REGION", "north");
        theEnv.put("MCP_CONTEXT_UNRELATED", "ignored");
        McpContextParams.Resolved r = McpContextParams.fromEnvironment(theEnv, DECLARED);
        assertNull(r.getProblem(), "an undeclared MCP_CONTEXT_ variable is not a refusal");
        assertEquals("42", r.getValues().get("CUSTOMER_ID"));

        theEnv.remove("MCP_CONTEXT_REGION");
        String theProblem = McpContextParams.fromEnvironment(theEnv, DECLARED).getProblem();
        assertNotNull(theProblem);
        assertTrue(theProblem.contains("REGION") && theProblem.contains("MCP_CONTEXT_"), theProblem);
    }

    @Test
    void notInstalledIsFoundInsideAWrapper() {
        Exception theWrapped = new Exception("borrow failed", new java.sql.SQLException(
                "ORA-06550: line 1, column 7:\nPLS-00201: identifier 'MCPDBWIZARD.MCPDBWIZARD_CTX' must be declared"));
        assertNotNull(McpContextParams.notInstalledReason(theWrapped));
    }

    /** Claude Code sends ${VAR} literally when VAR is unset; that must refuse, not run as a customer. */
    @Test
    void anUnexpandedClientVariableIsRefused() {
        String theProblem = McpContextParams.fromQueryString("CUSTOMER_ID=${CUST}&REGION=north",
                DECLARED).getProblem();
        assertNotNull(theProblem);
        assertTrue(theProblem.contains("did not substitute"), theProblem);
        assertNotNull(McpContextParams.fromQueryString("CUSTOMER_ID=%24%7BCUST%7D&REGION=north",
                DECLARED).getProblem(), "the same, after a proxy has percent-encoded it");
        assertNotNull(McpContextParams.valueProblem("X", "${CUST:-}"));
        assertNull(McpContextParams.valueProblem("X", "price$5"), "a lone $ is an ordinary character");
    }
}
