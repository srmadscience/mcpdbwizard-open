package com.mcpdbwizard.app.common;

import com.mcpdbwizard.pub.CSException;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How {@link MetadataCursorGuard} recognises ORA-01000, without a database. It must find it however
 * it arrives (the driver's vendor code, or the Oracle text inside the pub library's CSException) and
 * must not treat any other error as cursor exhaustion: a reconnect for the wrong reason would hide
 * the real fault behind a retry.
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class MetadataCursorGuardTest {

    @Test
    void theDriversVendorCodeIsRecognised() {
        assertTrue(MetadataCursorGuard.isCursorExhaustion(new SQLException("max cursors", "72000", 1000)));
    }

    @Test
    void theOracleTextInsideAWrappingExceptionIsRecognised() {
        // As the generator logged it: the pub library's message, carrying the driver's text
        assertTrue(MetadataCursorGuard.isCursorExhaustion(new CSException(
                "Unable to execute this statement. First Message:ORA-01000: maximum open cursors exceeded")));
        assertTrue(MetadataCursorGuard.isCursorExhaustion(new RuntimeException("wrapper",
                new SQLException("ORA-01000: maximum open cursors exceeded"))));
    }

    @Test
    void otherErrorsAreNotCursorExhaustion() {
        assertFalse(MetadataCursorGuard.isCursorExhaustion(new SQLException("ORA-00942: table or view does not exist", "42000", 942)));
        assertFalse(MetadataCursorGuard.isCursorExhaustion(new CSException("ORA-17008: Closed connection")));
        assertFalse(MetadataCursorGuard.isCursorExhaustion(new SQLException("ORA-10000: not this one", "72000", 10000)));
        assertFalse(MetadataCursorGuard.isCursorExhaustion(null));
    }
}
