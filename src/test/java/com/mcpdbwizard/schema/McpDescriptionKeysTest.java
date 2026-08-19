package com.mcpdbwizard.schema;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Author-supplied MCP tool descriptions round-trip through both config formats.
 *
 * <p>Phase 1 of {@code docs/mcp-tool-descriptions-plan.md}: storage only, no emission and no UI.
 * What is worth pinning is the part that fails SILENTLY. An unrecognised key still round-trips —
 * {@link Schema} keeps it in {@code extraProperties} to stay lossless — so a test that only checked
 * the value would pass while nothing in the application could read it. Every case here therefore
 * asserts {@code extraProperties} is EMPTY as well.
 *
 * <p>The table family is the one at risk: its keys carry a trailing OPERATION
 * ({@code TABLE_MCP_DESC_0_PK}), so they do not match the generic indexed-key pattern, whose index
 * is terminal.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class McpDescriptionKeysTest {

    private static Properties configWith(String... theKeyValuePairs) {
        Properties p = new Properties();
        p.setProperty("TABLE_USER_0", "SCOTT");
        p.setProperty("TABLE_NAME_0", "EMP");
        p.setProperty("SEQUENCE_USER_0", "SCOTT");
        p.setProperty("SEQUENCE_NAME_0", "EMP_SEQ");
        p.setProperty("PROC_USER_0", "SCOTT");
        p.setProperty("PROC_NAME_0", "RAISE_SALARY");
        p.setProperty("SQL_FILENAME_0", "headcount.sql");
        for (int seq = 0; seq < theKeyValuePairs.length; seq += 2) {
            p.setProperty(theKeyValuePairs[seq], theKeyValuePairs[seq + 1]);
        }
        return p;
    }

    // ---- the three one-tool families ----------------------------------------------------

    @Test
    void sequenceProcedureAndStatementDescriptionsSurvivePb2() {
        Schema theSchema = new Schema(configWith(
                "SEQUENCE_MCP_DESC_0", "Next employee number.",
                "PROC_MCP_DESC_0", "Give someone a raise.",
                "SQL_MCP_DESC_0", "Headcount by department."));

        assertEquals("Next employee number.", theSchema.getSequences().get(0).getMcpDescription());
        assertEquals("Give someone a raise.", theSchema.getProcedures().get(0).getMcpDescription());
        assertEquals("Headcount by department.", theSchema.getSqlStatements().get(0).getMcpDescription());
        assertTrue(theSchema.getExtraProperties().isEmpty(), "a key fell through to extraProperties");

        Properties theRebuilt = theSchema.toPb2();
        assertEquals("Next employee number.", theRebuilt.getProperty("SEQUENCE_MCP_DESC_0"));
        assertEquals("Give someone a raise.", theRebuilt.getProperty("PROC_MCP_DESC_0"));
        assertEquals("Headcount by department.", theRebuilt.getProperty("SQL_MCP_DESC_0"));
    }

    // ---- the table family, which is per operation ---------------------------------------

    @Test
    void everyTableOperationKeyIsRecognised() {
        Schema theSchema = new Schema(configWith(
                "TABLE_MCP_DESC_0_PK", "Fetch one employee.",
                "TABLE_MCP_DESC_0_INS", "Hire someone.",
                "TABLE_MCP_DESC_0_UPD", "Replace an employee row.",
                "TABLE_MCP_DESC_0_DEL", "Terminate an employee.",
                "TABLE_MCP_DESC_0_UK_EMP_CODE", "Find by payroll code.",
                "TABLE_MCP_DESC_0_IX_EMP_DEPT", "Find everyone in a department.",
                "TABLE_MCP_DESC_0_FK_EMP_MGR", "List someone's direct reports."));

        Table theTable = theSchema.getTables().get(0);
        assertEquals("Fetch one employee.", theTable.getMcpDescription(Table.OP_GET_BY_PK));
        assertEquals("Hire someone.", theTable.getMcpDescription(Table.OP_INSERT));
        assertEquals("Replace an employee row.", theTable.getMcpDescription(Table.OP_UPDATE));
        assertEquals("Terminate an employee.", theTable.getMcpDescription(Table.OP_DELETE));
        assertEquals("Find by payroll code.",
                theTable.getMcpDescription(Table.OP_UNIQUE_KEY_PREFIX + "EMP_CODE"));
        assertEquals("Find everyone in a department.",
                theTable.getMcpDescription(Table.OP_INDEX_PREFIX + "EMP_DEPT"));
        assertEquals("List someone's direct reports.",
                theTable.getMcpDescription(Table.OP_FOREIGN_KEY_PREFIX + "EMP_MGR"));

        assertTrue(theSchema.getExtraProperties().isEmpty(),
                "TABLE_MCP_DESC_* fell through to extraProperties - the trailing operation means it"
                        + " does not match the generic indexed-key pattern");

        Properties theRebuilt = theSchema.toPb2();
        assertEquals("Hire someone.", theRebuilt.getProperty("TABLE_MCP_DESC_0_INS"));
        assertEquals("List someone's direct reports.",
                theRebuilt.getProperty("TABLE_MCP_DESC_0_FK_EMP_MGR"));
    }

    @Test
    void aConstraintNameContainingUnderscoresIsNotTruncated() {
        // The operation is everything after the index, so an Oracle name with underscores - which
        // is most of them - must survive whole. A greedy split on "_" would keep only "UK".
        Schema theSchema = new Schema(configWith(
                "TABLE_MCP_DESC_0_UK_EMP_DEPT_LOCATION_CODE", "By department, location and code."));
        assertEquals("By department, location and code.",
                theSchema.getTables().get(0)
                        .getMcpDescription(Table.OP_UNIQUE_KEY_PREFIX + "EMP_DEPT_LOCATION_CODE"));
        assertTrue(theSchema.getExtraProperties().isEmpty());
    }

    // ---- the JSON leg -------------------------------------------------------------------

    @Test
    void descriptionsSurviveTheJsonRoundTrip() {
        Schema theSchema = new Schema(configWith(
                "SEQUENCE_MCP_DESC_0", "Next employee number.",
                "TABLE_MCP_DESC_0_PK", "Fetch one employee.",
                "TABLE_MCP_DESC_0_FK_EMP_MGR", "List someone's direct reports."));

        Schema theReloaded = new Schema(theSchema.toJson());
        assertEquals("Next employee number.", theReloaded.getSequences().get(0).getMcpDescription());
        assertEquals("Fetch one employee.",
                theReloaded.getTables().get(0).getMcpDescription(Table.OP_GET_BY_PK));
        assertEquals("List someone's direct reports.",
                theReloaded.getTables().get(0)
                        .getMcpDescription(Table.OP_FOREIGN_KEY_PREFIX + "EMP_MGR"));

        Properties theRebuilt = theReloaded.toPb2();
        assertEquals("Fetch one employee.", theRebuilt.getProperty("TABLE_MCP_DESC_0_PK"));
    }

    @Test
    void awkwardTextSurvivesBothFormats() {
        // Descriptions are free text an author typed. Quotes and backslashes are the ones that go
        // on to need escaping when this reaches emitted Java (phase 2); here they only have to
        // survive storage unchanged.
        String theAwkward = "Say \"hello\", use a \\ and a 'quote', 100% of the time.";
        Schema theSchema = new Schema(configWith("TABLE_MCP_DESC_0_INS", theAwkward));
        assertEquals(theAwkward, theSchema.getTables().get(0).getMcpDescription(Table.OP_INSERT));
        assertEquals(theAwkward, theSchema.toPb2().getProperty("TABLE_MCP_DESC_0_INS"));
        assertEquals(theAwkward, new Schema(theSchema.toJson()).getTables().get(0)
                .getMcpDescription(Table.OP_INSERT));
    }

    // ---- absent vs present-but-empty ----------------------------------------------------

    @Test
    void absentStaysAbsentSoOldConfigsAreUnchanged() {
        Schema theLegacy = new Schema(configWith());
        assertNull(theLegacy.getSequences().get(0).getMcpDescription());
        assertTrue(theLegacy.getTables().get(0).getMcpDescriptions().isEmpty());

        Properties theRebuilt = theLegacy.toPb2();
        assertFalse(theRebuilt.containsKey("SEQUENCE_MCP_DESC_0"));
        assertFalse(theRebuilt.containsKey("TABLE_MCP_DESC_0_PK"));
        // The JSON must not gain an empty object either, or every existing .json sibling changes.
        assertFalse(theLegacy.toJson().contains("mcpDescriptions"));
    }

    @Test
    void anEmptyDescriptionIsKeptAndIsNotTheSameAsAbsent() {
        // "" is an author deliberately choosing an empty description, which is legal MCP. Losing
        // the distinction would silently restore the generated default instead.
        Schema theSchema = new Schema(configWith(
                "SEQUENCE_MCP_DESC_0", "",
                "TABLE_MCP_DESC_0_DEL", ""));
        assertEquals("", theSchema.getSequences().get(0).getMcpDescription());
        assertEquals("", theSchema.getTables().get(0).getMcpDescription(Table.OP_DELETE));

        Properties theRebuilt = theSchema.toPb2();
        assertTrue(theRebuilt.containsKey("SEQUENCE_MCP_DESC_0"));
        assertEquals("", theRebuilt.getProperty("TABLE_MCP_DESC_0_DEL"));
        assertTrue(theSchema.getExtraProperties().isEmpty());
    }

    @Test
    void settingNullRemovesAnOperationRatherThanStoringNull() {
        Table theTable = new Table(0, "EMP", "SCOTT");
        theTable.setMcpDescription(Table.OP_INSERT, "Hire someone.");
        theTable.setMcpDescription(Table.OP_INSERT, null);
        assertTrue(theTable.getMcpDescriptions().isEmpty());
        assertNull(theTable.getMcpDescription(Table.OP_INSERT));
    }
}
