package com.mcpdbwizard.schema;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Schema#selectsNothing()}.
 *
 * <p>An empty config generates successfully and then fails much later, in the Runtime page, with a
 * message about something else — which is the detour this predicate exists to prevent. What is
 * worth pinning is that it keys on the four SELECTION lists and nothing else: a config carrying a
 * connection, a package name and every flag set is still empty if it names no object, and it is
 * easy to "improve" this into checking a populated-looking field instead.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class SchemaSelectsNothingTest {

    @Test
    void aFreshSchemaSelectsNothing() {
        assertTrue(new Schema().selectsNothing());
    }

    @Test
    void anyOneOfTheFourListsIsEnoughToMakeItNonEmpty() {
        Schema theTables = new Schema();
        theTables.getTables().add(new Table());
        assertFalse(theTables.selectsNothing(), "a table is a selection");

        Schema theProcedures = new Schema();
        theProcedures.getProcedures().add(new Procedure());
        assertFalse(theProcedures.selectsNothing(), "a procedure is a selection");

        Schema theSequences = new Schema();
        theSequences.getSequences().add(new Sequence());
        assertFalse(theSequences.selectsNothing(), "a sequence is a selection");

        Schema theStatements = new Schema();
        theStatements.getSqlStatements().add(new SqlStatement());
        assertFalse(theStatements.selectsNothing(), "a SQL statement is a selection");
    }

    @Test
    void settingsAreNotSelections() {
        // The case that made the message wrong in the first place: MCP_SERVER=YES, a real
        // connection and a package name all present, and still nothing to generate. Every field
        // here describes HOW to generate, not WHAT.
        Schema theSchema = new Schema();
        theSchema.setMcpServer("YES");
        theSchema.setTargetJvm("21");
        theSchema.setWebServices("YES");
        theSchema.setHostname("10.0.0.1");
        theSchema.setUser("SCOTT");
        theSchema.setPackageName("com.example.thing");
        assertTrue(theSchema.selectsNothing());
    }

    @Test
    void nullingAListLeavesItEmptyRatherThanNull() {
        // The setters substitute an empty list, which is what lets selectsNothing() skip null
        // guards. If that ever changes this test fails here rather than as an NPE on the error path.
        Schema theSchema = new Schema();
        theSchema.setTables(null);
        theSchema.setProcedures(null);
        theSchema.setSequences(null);
        theSchema.setSqlStatements(null);
        assertTrue(theSchema.selectsNothing());
    }
}
