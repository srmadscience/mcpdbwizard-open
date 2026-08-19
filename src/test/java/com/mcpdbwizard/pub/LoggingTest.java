package com.mcpdbwizard.pub;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the logging abstraction: {@link LogInterface}, the shared
 * {@link GenericLog} base, and the concrete {@link ConsoleLog}.
 *
 * <p>A {@link CapturingLog} subclass records the message type and text that
 * {@code GenericLog} hands to {@code writeMessage}, so we can assert that each
 * convenience method routes to the correct {@link LogInterface} severity
 * constant without touching the console.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class LoggingTest {

    /** Minimal GenericLog that captures the last message instead of writing it. */
    private static final class CapturingLog extends GenericLog {
        String lastType;
        String lastText;
        int flushCount;

        @Override
        protected void writeMessage(String messageType, String messageText, boolean isModal, boolean isLogged) {
            this.lastType = messageType;
            this.lastText = messageText;
        }

        @Override
        public void flush() {
            flushCount++;
        }

        @Override
        public String getCurrentLog() {
            return "capture";
        }
    }

    private CapturingLog log;

    @BeforeEach
    void setUp() {
        log = new CapturingLog();
        // debugMessagesPrinted is shared (static) state on GenericLog; normalise it.
        log.debugOff();
    }

    @Test
    void infoRoutesToInfoSeverity() {
        log.info("hello");
        assertEquals(LogInterface.INFO, log.lastType);
        assertEquals("hello", log.lastText);
    }

    @Test
    void warningRoutesToWarnSeverity() {
        log.warning("careful");
        assertEquals(LogInterface.WARN, log.lastType);
        assertEquals("careful", log.lastText);
    }

    @Test
    void errorRoutesToErrorSeverity() {
        log.error("bad");
        assertEquals(LogInterface.ERROR, log.lastType);
        assertEquals("bad", log.lastText);
    }

    @Test
    void errorFromExceptionUsesItsMessage() {
        log.error(new IllegalStateException("kaboom"));
        assertEquals(LogInterface.ERROR, log.lastType);
        assertEquals("kaboom", log.lastText);
    }

    @Test
    void syserrorRoutesToSysErrSeverityAndFlushes() {
        log.syserror("fatal");
        assertEquals(LogInterface.SYSERR, log.lastType);
        assertEquals("fatal", log.lastText);
        assertTrue(log.flushCount > 0, "syserror(String) should flush the log");
    }

    @Test
    void debugIsSuppressedWhenDebugIsOff() {
        log.debugOff();
        assertFalse(log.getDebug());
        log.debug("verbose");
        assertNull(log.lastType, "debug message must not be written while debug is off");
    }

    @Test
    void debugIsWrittenWhenDebugIsOn() {
        log.debugOn();
        assertTrue(log.getDebug());
        log.debug("verbose");
        assertEquals(LogInterface.DEBUG, log.lastType);
        assertEquals("verbose", log.lastText);
        log.debugOff(); // restore shared state for other tests
    }

    @Test
    void formatMessageIncludesSeverityMessageAndDelimiter() {
        String formatted = log.formatMessage(LogInterface.INFO, "the body");
        assertTrue(formatted.contains(LogInterface.INFO), "should contain severity label");
        assertTrue(formatted.contains("the body"), "should contain the message body");
        assertTrue(formatted.contains(LogInterface.DEFAULT_FIELD_DELIMITER), "should use the field delimiter");
        // Layout is <timestamp>:<severity>:<message>
        assertTrue(formatted.endsWith(LogInterface.DEFAULT_FIELD_DELIMITER + LogInterface.INFO
                + LogInterface.DEFAULT_FIELD_DELIMITER + "the body"));
    }

    @Test
    void getDateFormatReturnsTheConfiguredPattern() {
        assertEquals(LogInterface.DEFAULT_TIME_FORMAT_STRING, log.getDateFormat());
    }

    @Test
    void consoleLogReportsItsClassNameAsCurrentLog() {
        ConsoleLog console = new ConsoleLog();
        assertEquals(ConsoleLog.class.getName(), console.getCurrentLog());
    }
}
