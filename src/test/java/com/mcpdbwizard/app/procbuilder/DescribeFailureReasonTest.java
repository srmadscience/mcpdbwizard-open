package com.mcpdbwizard.app.procbuilder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The reason a SQL statement yields no MCP tool leads with the database's own complaint when there
 * was one.
 *
 * <p>Before this, a statement naming a table the config's account could not see was reported as
 * "it produced no metadata" (Postgres) or "return type WsdlRowSet is neither void nor a row array"
 * (Oracle): true, and no help to the person who then had to find the warning further up the
 * generation log.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class DescribeFailureReasonTest {

    private static final String WSDL = "return type WsdlRowSet is neither void nor a row array";

    /** The shape QueryStatement produced on FREE26 for a table the account cannot see (names generic). */
    private static final String FREE26_MESSAGE = "select * from orders:42000:Unable to execute this"
            + " statement. First Message:ORA-00942: table or view \"APPSCHEMA\".\"ORDERS\" does not exist"
            + " https://docs.oracle.com/error-help/db/ora-00942/ Second Message:ORA-00942: table or view"
            + " \"APPSCHEMA\".\"ORDERS\" does not exist\nhttps://docs.oracle.com/error-help/db/ora-00942/";

    @Test
    void aStatementThatDescribedKeepsItsReasonUnchanged() {
        assertEquals(WSDL, SAAdminWrangler.withDescribeFailure(null, WSDL));
        assertEquals(WSDL, SAAdminWrangler.withDescribeFailure("", WSDL));
    }

    @Test
    void aStatementThatCouldNotBeDescribedLeadsWithWhy() {
        assertEquals("the database could not describe it: ORA-00942: table or view does not exist (so "
                        + WSDL + ")",
                SAAdminWrangler.withDescribeFailure("ORA-00942: table or view does not exist", WSDL));
    }

    @Test
    void theOracleWrapperIsReducedToTheFinalMessageWithoutItsUrl() {
        assertEquals("ORA-00942: table or view \"APPSCHEMA\".\"ORDERS\" does not exist",
                SqlStatementWrangler.conciseDatabaseMessage(FREE26_MESSAGE));
    }

    @Test
    void aPlainMessagePassesThroughOnOneLine() {
        // The Postgres fork's describe failure, as the hotel demo produced it: no wrapper, no URL,
        // and a Position that counts into the generator's describe query rather than the
        // author's statement, so it is dropped.
        assertEquals("ERROR: relation \"hotels\" does not exist",
                SqlStatementWrangler.conciseDatabaseMessage("ERROR: relation \"hotels\" does not exist\n  Position: 47"));
        assertEquals("ERROR: syntax error at or near \"FORM\"",
                SqlStatementWrangler.conciseDatabaseMessage("ERROR: syntax error at or near \"FORM\"\n  Position: 10"),
                "on any message, not only a missing relation");
        // MariaDB Connector/J, measured against the MariaDB hotel demo: the session id is noise.
        assertEquals("Table 'mcpdemo.hotels_missing' doesn't exist",
                SqlStatementWrangler.conciseDatabaseMessage("(conn=2673) Table 'mcpdemo.hotels_missing' doesn't exist"));
        assertNull(SqlStatementWrangler.conciseDatabaseMessage(null));
        assertNull(SqlStatementWrangler.conciseDatabaseMessage("  "));
    }
}
