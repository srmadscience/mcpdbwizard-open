package com.mcpdbwizard.pub;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An implementation of {@link LogInterface} that routes every message to
 * <a href="https://logging.apache.org/log4j/2.x/" target="_blank">Apache Log4j 2</a>. This lets
 * MCPDBWizard software — and the JDBC code the wizard generates — log through a Log4j 2
 * configuration the host application already owns, instead of MCPDBWizard's own file/console logs.
 * <p>
 * The five {@code LogInterface} severities map onto Log4j 2 levels as follows. Because Log4j 2
 * has a dedicated {@code FATAL} level, {@code syserror} (reserved for "really bad and never
 * supposed to happen") is distinguished from ordinary {@code error}:
 * <table border="1">
 * <caption>LogInterface severity to Log4j 2 level mapping</caption>
 * <tr><th>{@code LogInterface}</th><th>Log4j 2</th></tr>
 * <tr><td>{@code debug}</td><td>{@code DEBUG}</td></tr>
 * <tr><td>{@code info}</td><td>{@code INFO}</td></tr>
 * <tr><td>{@code warning}</td><td>{@code WARN}</td></tr>
 * <tr><td>{@code error}</td><td>{@code ERROR}</td></tr>
 * <tr><td>{@code syserror}</td><td>{@code FATAL}</td></tr>
 * </table>
 * <p>
 * The {@code isModal} and {@code isLogged} flags carried by the detailed {@code LogInterface}
 * methods are accepted and ignored: Log4j 2 has no modal-dialog concept, and whether a message is
 * actually written is decided by the configured Log4j 2 level filtering, not by this class. Debug
 * messages additionally honour {@link #debugOn()} / {@link #debugOff()} (off by default), matching
 * the other {@code LogInterface} implementations.
 * <p>
 * The exception-taking {@code error}/{@code syserror} methods pass the {@link Throwable} straight
 * to Log4j 2, so the backend records the full stack trace rather than just the message text.
 * <p>
 * This backend depends only on the Log4j 2 API ({@code log4j-api}); the host application supplies
 * the Log4j 2 core implementation and its configuration.
 * <p>
 * See <a href="https://mcpdbwizard.com/faq/logging">LogInterface</a>
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 */
public class Log4j2Log extends GenericLog implements LogInterface {

    /**
     * The Log4j 2 logger every message is routed through.
     */
    private final Logger logger;

    /**
     * Create an instance of {@code Log4j2Log} backed by a logger named after this class.
     */
    public Log4j2Log() {
        super();
        this.logger = LogManager.getLogger(Log4j2Log.class);
    }

    /**
     * Create an instance of {@code Log4j2Log} backed by a logger with the given name.
     *
     * @param loggerName the Log4j 2 logger name to use (for example a fully qualified class name
     *                   or a functional category).
     */
    public Log4j2Log(String loggerName) {
        super();
        this.logger = LogManager.getLogger(loggerName);
    }

    /**
     * Create an instance of {@code Log4j2Log} backed by a logger named after the given class.
     *
     * @param clazz the class whose name is used as the Log4j 2 logger name.
     */
    public Log4j2Log(Class<?> clazz) {
        super();
        this.logger = LogManager.getLogger(clazz);
    }

    /**
     * Route a formatted message to the Log4j 2 level that corresponds to its severity. Log4j 2
     * applies its own level filtering, so a message emitted here may still be discarded downstream.
     *
     * @param messageType one of the {@code DEBUG}, {@code INFO}, {@code WARN}, {@code ERROR} or
     *                    {@code SYSERR} constants defined in {@link LogInterface}.
     * @param messageText the message text.
     * @param isModal     ignored; Log4j 2 has no modal-dialog concept.
     * @param isLogged    ignored; the configured Log4j 2 level filtering decides what is written.
     */
    protected synchronized void writeMessage(String messageType, String messageText, boolean isModal, boolean isLogged) {
        if (LogInterface.DEBUG.equals(messageType)) {
            logger.debug(messageText);
        } else if (LogInterface.INFO.equals(messageType)) {
            logger.info(messageText);
        } else if (LogInterface.WARN.equals(messageType)) {
            logger.warn(messageText);
        } else if (LogInterface.SYSERR.equals(messageType)) {
            logger.fatal(messageText);
        } else {
            // ERROR and anything unrecognised map to Log4j 2's ERROR level.
            logger.error(messageText);
        }
    }

    /**
     * Log an error, passing the exception to Log4j 2 so the full stack trace is recorded.
     *
     * @param theException the exception to log.
     * @param isModal      ignored; Log4j 2 has no modal-dialog concept.
     * @param isLogged     ignored; the configured Log4j 2 level filtering decides what is written.
     */
    public void error(Exception theException, boolean isModal, boolean isLogged) {
        logger.error(theException == null ? null : theException.getMessage(), theException);
    }

    /**
     * Log a serious error at {@code FATAL}, passing the exception to Log4j 2 so the full stack
     * trace is recorded.
     *
     * @param theException the exception to log.
     * @param isModal      ignored; Log4j 2 has no modal-dialog concept.
     * @param isLogged     ignored; the configured Log4j 2 level filtering decides what is written.
     */
    public void syserror(Exception theException, boolean isModal, boolean isLogged) {
        logger.fatal(theException == null ? null : theException.getMessage(), theException);
    }

    /**
     * Required by {@link LogInterface} but not used: Log4j 2 manages its own buffering and
     * flushing.
     */
    public void flush() {
    }

    /**
     * Returns the name of the Log4j 2 logger messages are routed through.
     *
     * @return the Log4j 2 logger name (not a file path; the actual destination is determined by
     *         the Log4j 2 configuration).
     */
    public String getCurrentLog() {
        return logger.getName();
    }
}
