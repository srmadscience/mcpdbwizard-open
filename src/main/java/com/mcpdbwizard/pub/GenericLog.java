package com.mcpdbwizard.pub;

import java.util.Date;

import java.text.SimpleDateFormat;

/**
 * Partial implementation of LogInterface that composes error messages but leaves the actual
 * logging to other classes.
 * <p>
 * See <a href="https://mcpdbwizard.com/faq/logging" target="_blank" class="news">LogInterface</a>
 *
 * <br>(c) Copyright 2004-2026 ATB Consultancy Services Ltd (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author <a href="https://mcpdbwizard.com" target="_blank" class="news">ATB Consultancy Services Ltd</a>
 * @version 5.0
 */
public abstract class GenericLog implements LogInterface {

    /**
     * "Log file being flushed" message
     */
    protected static final String LOG_BEING_FLUSHED = "Log File Flush Request Received";

    /**
     * SimpleDateFormat used in the message formatting process, one per thread.
     *
     * <p><b>Per-thread deliberately.</b> {@code SimpleDateFormat} is not thread-safe: concurrent
     * {@code format()} calls corrupt its internal calendar and it throws an
     * {@code ArrayIndexOutOfBoundsException} with a nonsensical index, or silently returns a wrong
     * time. This field used to be a single shared static that the constructor also <em>reassigned</em>,
     * so two threads could be inside {@code format()} while a third swapped the object underneath
     * them. Every generated application that logs from more than one thread was exposed - which is
     * every pooled MCP server, since the pool warms several factories at once and each logs as it
     * starts. It surfaced as an intermittent
     * "Could not obtain a DAO factory from the pool: ArrayIndexOutOfBoundsException".
     *
     * <p>A {@code ThreadLocal} rather than a lock: formatting happens on every message, and the
     * per-thread copy costs one object per logging thread instead of contention on every log line.
     */
    private static final ThreadLocal<java.text.SimpleDateFormat> theDateFormat =
            new ThreadLocal<java.text.SimpleDateFormat>() {
                @Override
                protected java.text.SimpleDateFormat initialValue() {
                    return new SimpleDateFormat(LogInterface.DEFAULT_TIME_FORMAT_STRING);
                }
            };

    /**
     * Flag to indicate whether we are printing debug messages. Defaults to <code>false</code>.
     */
    protected static boolean debugMessagesPrinted = false;

    /**
     * Flag to indicate whether messages should be logged by default. Defaults to <code>true</code>.
     */
    protected boolean autoLog = true;

    /**
     * Flag to indicate whether we should call the <code>flush()</code> method every time we print a message. Defaults to <code>false</code>.
     *
     * @see GenericLog#flush
     */
    protected boolean autoFlush = false;

    /**
     * Flag to indicate whether we should include the Thread id every time we print a message. Defaults to <code>false</code>.
     */
    protected boolean printThreadId = false;

    /**
     * Create an instance of GenericLog.
     */
    public GenericLog() {
        // Nothing to set up: the formatter is created per thread on first use. Assigning a shared
        // one here was itself a race - a log created on one thread would replace the instance
        // another thread was formatting with.
    }

    /**
     * Return date format string
     */
    public String getDateFormat() {
        return (theDateFormat.get().toPattern());
    }

    /**
     * Turn debug messages on
     */
    public void debugOn() {
        debugMessagesPrinted = true;
    }

    /**
     * Turn debug messages off
     */
    public void debugOff() {
        debugMessagesPrinted = false;
    }

    /**
     * Get debug status
     *
     * @return <code>true</code> if debug messages are being printed.
     */
    public boolean getDebug() {
        return (debugMessagesPrinted);
    }

    public void debug(String theMessage) {
        debug(theMessage, false, autoLog);
    }

    public void debug(String theMessage, boolean isModal, boolean isLogged) {
        // Only generate message if we are supposed to...
        if (debugMessagesPrinted) {
            writeMessage(LogInterface.DEBUG, theMessage, isModal, isLogged);
        }
    }

    public void info(String theMessage) {
        info(theMessage, false, autoLog);
    }

    public void info(String theMessage, boolean isModal, boolean isLogged) {
        writeMessage(LogInterface.INFO, theMessage, isModal, isLogged);
    }

    public void warning(String theMessage) {
        warning(theMessage, false, autoLog);
    }

    public void warning(String theMessage, boolean isModal, boolean isLogged) {
        writeMessage(LogInterface.WARN, theMessage, isModal, isLogged);
    }

    public void error(String theMessage) {
        error(theMessage, false, autoLog);
    }

    public void error(String theMessage, boolean isModal, boolean isLogged) {
        writeMessage(LogInterface.ERROR, theMessage, isModal, isLogged);
    }

    public void error(Exception theException) {
        error(theException, false, autoLog);
    }

    public void error(Exception theException, boolean isModal, boolean isLogged) {
        writeMessage(LogInterface.ERROR, theException.getMessage(), isModal, isLogged);
    }

    public void syserror(String theMessage) {
        syserror(theMessage, false, autoLog);
        flush();
    }

    public void syserror(String theMessage, boolean isModal, boolean isLogged) {
        writeMessage(LogInterface.SYSERR, theMessage, isModal, isLogged);
    }

    public void syserror(Exception theException) {
        syserror(theException, false, autoLog);
        flush();
    }

    public void syserror(Exception theException, boolean isModal, boolean isLogged) {
        writeMessage(LogInterface.ERROR, theException.getMessage(), isModal, isLogged);
    }

    public abstract void flush();

    public abstract String getCurrentLog();

    /**
     * Format a message and return it as a printable string
     *
     * @param  messageType should be one of the constants DEBUG, INFO, WARN, ERROR or SYSERR as defined in LogInterface.
     * @param  messageText The message
     * @return String a formatted message
     */
    protected String formatMessage(String messageType, String messageText) {
        Date tempDate = new Date(System.currentTimeMillis());
        if (printThreadId) {
            return (theDateFormat.get().format(tempDate) + LogInterface.DEFAULT_FIELD_DELIMITER
                    + messageType
                    //REMOVEFORJDK1.5   + " ["+Thread.currentThread().getId()+"] "
                    + LogInterface.DEFAULT_FIELD_DELIMITER
                    + messageText);
        }

        return (theDateFormat.get().format(tempDate) + LogInterface.DEFAULT_FIELD_DELIMITER
                + messageType + LogInterface.DEFAULT_FIELD_DELIMITER
                + messageText);
    }

    /**
     * Controls whether the class should log every message unless told not to.
     *
     * @param  logEveryMessageByDefault <code>true</code> if messages are to be logged by default.
     */
    public void setAutoLog(boolean logEveryMessageByDefault) {
        autoLog = logEveryMessageByDefault;
    }

    /**
     * Controls whether the class should flush the log every time it is asked to print a message. Slower but more reliable.
     *
     * @param  flushEveryMessage <code>true</code> if the log is to be flushed every time a message is written to it.
     */
    public void setAutoFlush(boolean flushEveryMessage) {
        autoFlush = flushEveryMessage;
    }

    /**
     * Format a message and write it to whatever it is we are writing to.
     *
     * @param  messageType should be one of the constants DEBUG, INFO, WARN, ERROR or SYSERR as defined in LogInterface.
     * @param  messageText The message
     */
    protected abstract void writeMessage(String messageType, String messageText, boolean isModal, boolean isLogged);

}




