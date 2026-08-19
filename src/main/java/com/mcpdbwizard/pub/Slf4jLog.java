package com.mcpdbwizard.pub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link LogInterface} that routes every message to
 * <a href="https://www.slf4j.org/" target="_blank">SLF4J</a> (the Simple Logging Facade for
 * Java). This lets MCPDBWizard software — and the JDBC code the wizard generates — log through
 * whatever SLF4J binding the host application already uses (Logback, Log4j 2, java.util.logging,
 * and so on) instead of MCPDBWizard's own file/console logs.
 * <p>
 * The five {@code LogInterface} severities map onto SLF4J levels as follows:
 * <table border="1">
 * <caption>LogInterface severity to SLF4J level mapping</caption>
 * <tr><th>{@code LogInterface}</th><th>SLF4J</th></tr>
 * <tr><td>{@code debug}</td><td>{@code DEBUG}</td></tr>
 * <tr><td>{@code info}</td><td>{@code INFO}</td></tr>
 * <tr><td>{@code warning}</td><td>{@code WARN}</td></tr>
 * <tr><td>{@code error}</td><td>{@code ERROR}</td></tr>
 * <tr><td>{@code syserror}</td><td>{@code ERROR}</td></tr>
 * </table>
 * <p>
 * The {@code isModal} and {@code isLogged} flags carried by the detailed {@code LogInterface}
 * methods are accepted and ignored: SLF4J has no modal-dialog concept, and whether a message is
 * actually written is decided by the configured SLF4J backend's level filtering, not by this
 * class. Debug messages additionally honour {@link #debugOn()} / {@link #debugOff()} (off by
 * default), matching the other {@code LogInterface} implementations.
 * <p>
 * The exception-taking {@code error}/{@code syserror} methods pass the {@link Throwable} straight
 * to SLF4J, so the backend records the full stack trace rather than just the message text.
 * <p>
 * See <a href="https://mcpdbwizard.com/faq/logging">LogInterface</a>
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 */
public class Slf4jLog extends GenericLog implements LogInterface {

    /**
     * The SLF4J logger every message is routed through.
     */
    private final Logger logger;

    /**
     * Create an instance of {@code Slf4jLog} backed by a logger named after this class.
     */
    public Slf4jLog() {
        super();
        this.logger = LoggerFactory.getLogger(Slf4jLog.class);
    }

    /**
     * Create an instance of {@code Slf4jLog} backed by a logger with the given name.
     *
     * @param loggerName the SLF4J logger name to use (for example a fully qualified class name
     *                   or a functional category).
     */
    public Slf4jLog(String loggerName) {
        super();
        this.logger = LoggerFactory.getLogger(loggerName);
    }

    /**
     * Create an instance of {@code Slf4jLog} backed by a logger named after the given class.
     *
     * @param clazz the class whose name is used as the SLF4J logger name.
     */
    public Slf4jLog(Class<?> clazz) {
        super();
        this.logger = LoggerFactory.getLogger(clazz);
    }

    /**
     * Route a formatted message to the SLF4J level that corresponds to its severity. The SLF4J
     * backend applies its own level filtering, so a message emitted here may still be discarded
     * downstream.
     *
     * @param messageType one of the {@code DEBUG}, {@code INFO}, {@code WARN}, {@code ERROR} or
     *                    {@code SYSERR} constants defined in {@link LogInterface}.
     * @param messageText the message text.
     * @param isModal     ignored; SLF4J has no modal-dialog concept.
     * @param isLogged    ignored; the configured SLF4J backend decides what is written.
     */
    protected synchronized void writeMessage(String messageType, String messageText, boolean isModal, boolean isLogged) {
        if (LogInterface.DEBUG.equals(messageType)) {
            logger.debug(messageText);
        } else if (LogInterface.INFO.equals(messageType)) {
            logger.info(messageText);
        } else if (LogInterface.WARN.equals(messageType)) {
            logger.warn(messageText);
        } else {
            // ERROR, SYSERR and anything unrecognised map to SLF4J's most severe level.
            logger.error(messageText);
        }
    }

    /**
     * Log an error, passing the exception to SLF4J so the full stack trace is recorded.
     *
     * @param theException the exception to log.
     * @param isModal      ignored; SLF4J has no modal-dialog concept.
     * @param isLogged     ignored; the configured SLF4J backend decides what is written.
     */
    public void error(Exception theException, boolean isModal, boolean isLogged) {
        logger.error(theException == null ? null : theException.getMessage(), theException);
    }

    /**
     * Log a serious error, passing the exception to SLF4J so the full stack trace is recorded.
     *
     * @param theException the exception to log.
     * @param isModal      ignored; SLF4J has no modal-dialog concept.
     * @param isLogged     ignored; the configured SLF4J backend decides what is written.
     */
    public void syserror(Exception theException, boolean isModal, boolean isLogged) {
        logger.error(theException == null ? null : theException.getMessage(), theException);
    }

    /**
     * Required by {@link LogInterface} but not used: SLF4J backends manage their own buffering
     * and flushing.
     */
    public void flush() {
    }

    /**
     * Returns the name of the SLF4J logger messages are routed through.
     *
     * @return the SLF4J logger name (not a file path; the actual destination is determined by the
     *         SLF4J backend's configuration).
     */
    public String getCurrentLog() {
        return logger.getName();
    }
}
