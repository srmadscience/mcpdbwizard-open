package com.mcpdbwizard.pub;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of {@link LogInterface} that routes every message to
 * {@code java.util.logging} (JUL), the logging framework built into the JDK. Unlike the
 * historical {@code JavaLog} (shipped separately so the library could stay Java 1.2/1.3
 * compatible), this backend lives directly in {@code com.mcpdbwizard.pub} and needs no extra jar —
 * JUL is part of the JDK.
 * <p>
 * The five {@code LogInterface} severities map onto JUL levels as follows. JUL has no level above
 * {@code SEVERE}, so both {@code error} and {@code syserror} use it:
 * <table border="1">
 * <caption>LogInterface severity to java.util.logging level mapping</caption>
 * <tr><th>{@code LogInterface}</th><th>{@code java.util.logging.Level}</th></tr>
 * <tr><td>{@code debug}</td><td>{@code FINE}</td></tr>
 * <tr><td>{@code info}</td><td>{@code INFO}</td></tr>
 * <tr><td>{@code warning}</td><td>{@code WARNING}</td></tr>
 * <tr><td>{@code error}</td><td>{@code SEVERE}</td></tr>
 * <tr><td>{@code syserror}</td><td>{@code SEVERE}</td></tr>
 * </table>
 * <p>
 * The {@code isModal} and {@code isLogged} flags carried by the detailed {@code LogInterface}
 * methods are accepted and ignored: JUL has no modal-dialog concept, and whether a message is
 * actually written is decided by the JUL configuration (usually
 * {@code $JAVA_HOME/conf/logging.properties}), not by this class. Debug messages additionally
 * honour {@link #debugOn()} / {@link #debugOff()} (off by default), matching the other
 * {@code LogInterface} implementations. If {@code debug} messages are not appearing, check the JUL
 * configuration for a {@code .level} entry and set it to {@code FINE} or {@code ALL}.
 * <p>
 * The exception-taking {@code error}/{@code syserror} methods pass the {@link Throwable} straight
 * to JUL, so the handler records the full stack trace rather than just the message text.
 * <p>
 * See <a href="https://mcpdbwizard.com/faq/logging">LogInterface</a>
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 */
public class JulLog extends GenericLog implements LogInterface {

    /**
     * The java.util.logging logger every message is routed through.
     */
    private final Logger logger;

    /**
     * Create an instance of {@code JulLog} backed by a logger named after this class.
     */
    public JulLog() {
        super();
        this.logger = Logger.getLogger(JulLog.class.getName());
    }

    /**
     * Create an instance of {@code JulLog} backed by a logger with the given name.
     *
     * @param loggerName the JUL logger name to use. This should be a dot-separated name, normally
     *                   based on the package or class name of the subsystem.
     */
    public JulLog(String loggerName) {
        super();
        this.logger = Logger.getLogger(loggerName);
    }

    /**
     * Create an instance of {@code JulLog} backed by a logger named after the given class.
     *
     * @param clazz the class whose name is used as the JUL logger name.
     */
    public JulLog(Class<?> clazz) {
        super();
        this.logger = Logger.getLogger(clazz.getName());
    }

    /**
     * Create an instance of {@code JulLog} that wraps an existing JUL logger.
     *
     * @param logger the {@link Logger} to route messages through.
     */
    public JulLog(Logger logger) {
        super();
        this.logger = logger;
    }

    /**
     * Return the underlying JUL logger.
     *
     * @return the {@link Logger} messages are routed through.
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Route a formatted message to the JUL level that corresponds to its severity. JUL applies its
     * own level filtering, so a message emitted here may still be discarded downstream.
     *
     * @param messageType one of the {@code DEBUG}, {@code INFO}, {@code WARN}, {@code ERROR} or
     *                    {@code SYSERR} constants defined in {@link LogInterface}.
     * @param messageText the message text.
     * @param isModal     ignored; JUL has no modal-dialog concept.
     * @param isLogged    ignored; the JUL configuration decides what is written.
     */
    protected synchronized void writeMessage(String messageType, String messageText, boolean isModal, boolean isLogged) {
        Level level;
        if (LogInterface.DEBUG.equals(messageType)) {
            level = Level.FINE;
        } else if (LogInterface.INFO.equals(messageType)) {
            level = Level.INFO;
        } else if (LogInterface.WARN.equals(messageType)) {
            level = Level.WARNING;
        } else {
            // ERROR, SYSERR and anything unrecognised map to JUL's most severe level.
            level = Level.SEVERE;
        }
        logger.log(level, messageText);
    }

    /**
     * Log an error at {@code SEVERE}, passing the exception to JUL so the full stack trace is
     * recorded.
     *
     * @param theException the exception to log.
     * @param isModal      ignored; JUL has no modal-dialog concept.
     * @param isLogged     ignored; the JUL configuration decides what is written.
     */
    public void error(Exception theException, boolean isModal, boolean isLogged) {
        logger.log(Level.SEVERE, theException == null ? null : theException.getMessage(), theException);
    }

    /**
     * Log a serious error at {@code SEVERE}, passing the exception to JUL so the full stack trace
     * is recorded.
     *
     * @param theException the exception to log.
     * @param isModal      ignored; JUL has no modal-dialog concept.
     * @param isLogged     ignored; the JUL configuration decides what is written.
     */
    public void syserror(Exception theException, boolean isModal, boolean isLogged) {
        logger.log(Level.SEVERE, theException == null ? null : theException.getMessage(), theException);
    }

    /**
     * Required by {@link LogInterface} but not used: JUL handlers manage their own flushing.
     */
    public void flush() {
    }

    /**
     * Returns the name of the JUL logger messages are routed through.
     *
     * @return the JUL logger name (not a file path; the actual destination is determined by the
     *         JUL configuration and its handlers).
     */
    public String getCurrentLog() {
        return logger.getName();
    }
}
