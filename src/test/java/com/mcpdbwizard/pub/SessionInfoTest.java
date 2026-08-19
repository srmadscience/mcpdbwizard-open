package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Database-free tests for {@link SessionInfo}: the truncation rules and the guards that keep naming
 * a session from ever being the thing that breaks a caller. That the value actually reaches the
 * session needs a real connection and lives in {@link SessionInfoLiveTest}, which is a separate
 * class precisely so these keep running where there is no database.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class SessionInfoTest {

    @Test
    void leavesAShortValueAlone() {
        String theName = "DaoFactory";
        assertSame(theName, SessionInfo.truncate(theName, SessionInfo.MODULE_MAX_LENGTH),
                "a value within the limit should be returned as-is, not copied");
    }

    @Test
    void leavesAValueOfExactlyTheLimitAlone() {
        String theName = "x".repeat(SessionInfo.MODULE_MAX_LENGTH);
        assertEquals(theName, SessionInfo.truncate(theName, SessionInfo.MODULE_MAX_LENGTH));
    }

    @Test
    void cutsAValueOverTheLimit() {
        String theName = "x".repeat(SessionInfo.MODULE_MAX_LENGTH + 20);
        assertEquals(SessionInfo.MODULE_MAX_LENGTH,
                SessionInfo.truncate(theName, SessionInfo.MODULE_MAX_LENGTH).length());
    }

    @Test
    void cutsAnActionAtItsOwnShorterLimit() {
        // The two limits differ (48 and 32) and mixing them up would clip a module 16 characters
        // early, which reads as a mangled name rather than a wrong constant.
        String theAction = "a".repeat(SessionInfo.MODULE_MAX_LENGTH);
        assertEquals(SessionInfo.ACTION_MAX_LENGTH,
                SessionInfo.truncate(theAction, SessionInfo.ACTION_MAX_LENGTH).length());
    }

    @Test
    void passesNullThrough() {
        // Null is how a caller says "clear it", so it must not become the string "null".
        assertNull(SessionInfo.truncate(null, SessionInfo.MODULE_MAX_LENGTH));
    }

    @Test
    void ignoresANullConnectionRatherThanThrowing() throws SQLException {
        SessionInfo.setModule(null, "AnyModule", "AnyAction");
    }

    @Test
    void namesTheGeneratorWithoutASubstitutionToken() {
        // Namer's product-name constants are placeholders whose sed step no longer runs, so using
        // one here would have put the literal "SUBST_PROD_NAME" into V$SESSION.
        assertEquals(-1, SessionInfo.GENERATOR_MODULE.indexOf("SUBST_"));
        assertEquals(-1, SessionInfo.GENERATOR_ACTION.indexOf("SUBST_"));
    }
}
