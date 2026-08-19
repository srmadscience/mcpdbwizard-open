package com.mcpdbwizard.app.procbuilder;

import com.mcpdbwizard.pub.JulLog;
import com.mcpdbwizard.pub.LogInterface;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2: saving a config writes each statement's text into {@code SQL_TEXT_<i>}, so a config
 * saved from now on stands alone.
 *
 * <p><b>The text written is the UNPROCESSED one, and that is the whole difficulty.</b>
 * {@code SqlStatementWrangler} keeps two forms: {@code theOriginalStatement}, exactly as read, and
 * {@code theStatement}, which has been through {@code cleanUpCommand} and {@code sqlplusParams}.
 * The obvious accessor — {@code getRawSqlStatement()} — returns the SECOND. Persisting that would
 * mean the next load processed it again.
 *
 * <p>{@code JulLog} rather than {@code ConsoleLog}: the console log blocks on a warning waiting for
 * a keypress, which hangs a test run rather than failing it.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class InlineSqlWriteTest {

    private static final LogInterface LOG = new JulLog("InlineSqlWriteTest");

    private static SqlStatementWrangler wrangler(String theName, String theSql) {
        return new SqlStatementWrangler(theName, theSql, new Properties(), 0, LOG);
    }

    @Test
    void savingWritesTheStatementIntoTheConfig() {
        String theSql = "select ename\n  from emp\n where empno = ?\n";
        SqlStatementWrangler theWrangler = wrangler("byempno.sql", theSql);

        Properties theSaved = new Properties();
        theWrangler.writeProperties(theSaved, 0);

        assertEquals("byempno.sql", theSaved.getProperty("SQL_FILENAME_0"),
                "the filename stays - it is the statement's identity and its class name");
        assertNotNull(theSaved.getProperty(SqlStatementWrangler.SQL_TEXT + "0"),
                "the statement itself must now be in the config");
    }

    /**
     * The trap this phase exists to avoid, made concrete.
     *
     * <p>{@code cleanUpCommand} escapes a double quote, so running it twice escalates {@code \"}
     * to {@code \\"}. Persisting the PROCESSED text would therefore alter the statement a little
     * more on every save — a corruption that compounds silently and would be blamed on the database
     * long before anyone suspected the config writer.
     */
    @Test
    void theTextStoredIsTheOriginalNotTheProcessedForm() {
        String theSql = "select \"QUOTED COLUMN\" from emp";
        SqlStatementWrangler theWrangler = wrangler("quoted.sql", theSql);

        Properties theSaved = new Properties();
        theWrangler.writeProperties(theSaved, 0);
        String theStored = theSaved.getProperty(SqlStatementWrangler.SQL_TEXT + "0");

        assertEquals(theSql, theStored, "the config must hold the statement as it was written");
        assertEquals(theSql, theWrangler.getOriginalSqlStatement());
        // And demonstrate that the other accessor really would have been wrong.
        assertTrue(theWrangler.getRawSqlStatement().contains("\\\""),
                "precondition: the processed form escapes the quote, which is why it must not be"
                        + " the thing persisted");
    }

    /**
     * Saving twice must not drift. This is the property that makes the migration safe to run on
     * every propfile: load from a file, save, load from the saved text, save again — identical.
     */
    @Test
    void aSaveReloadSaveCycleIsIdempotent() {
        String theSql = "update emp set sal = sal * 1.1 where deptno = ?\n";

        Properties theFirst = new Properties();
        wrangler("raise.sql", theSql).writeProperties(theFirst, 0);
        String theOnce = theFirst.getProperty(SqlStatementWrangler.SQL_TEXT + "0");

        // Now load from what was saved, exactly as the loader would after inlining, and save again.
        Properties theSecond = new Properties();
        wrangler("raise.sql", theOnce).writeProperties(theSecond, 0);
        String theTwice = theSecond.getProperty(SqlStatementWrangler.SQL_TEXT + "0");

        assertEquals(theOnce, theTwice,
                "a second save changed the statement - the stored form is not a fixed point, so"
                        + " every save would alter it a little more");
        assertEquals(theSql, theTwice, "and it must still be what the author wrote");
    }

    /** A quoted statement through the same cycle, since quotes are what cleanUpCommand touches. */
    @Test
    void theIdempotenceHoldsForTheCaseThatWouldBreakIt() {
        String theSql = "select \"A\", \"B\" from \"MY TABLE\"";

        Properties theFirst = new Properties();
        wrangler("q.sql", theSql).writeProperties(theFirst, 0);
        String theOnce = theFirst.getProperty(SqlStatementWrangler.SQL_TEXT + "0");

        Properties theSecond = new Properties();
        wrangler("q.sql", theOnce).writeProperties(theSecond, 0);

        assertEquals(theSql, theSecond.getProperty(SqlStatementWrangler.SQL_TEXT + "0"),
                "quotes must survive an unlimited number of save cycles unchanged");
    }

    /**
     * The generated class name must not move. It comes from the FILENAME, and this phase does not
     * touch that — but the phase is where a well-meaning "the filename is redundant now" change
     * would land, so the coupling is asserted here rather than left implied.
     */
    @Test
    void theGeneratedClassNameStillComesFromTheFilename() {
        SqlStatementWrangler theWrangler = wrangler("update5.sql", "update emp set sal = 1");

        Properties theSaved = new Properties();
        theWrangler.writeProperties(theSaved, 0);

        assertEquals("update5.sql", theSaved.getProperty("SQL_FILENAME_0"),
                "dropping the name or its .sql suffix renames the generated class, its cursor"
                        + " classes and the MCP tool - the suffix is removed by a fixed-length"
                        + " substring, so 'update5' would become 'Upda'");
    }
}
