package com.mcpdbwizard.pub;

import java.io.*;

import java.text.SimpleDateFormat;

/**
 * A heavyweight implementation of <code>LogInterface</code>.
 * <p>
 * TextLog is a logging mechanism designed for large volumes of data
 * over a protracted period of time. Log files are stored in their own
 * dedicated directory structure.
 * <p>
 * See <a href="https://mcpdbwizard.com/faq/logging">LogInterface</a>
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 */
public class TextLog extends GenericLog implements LogInterface, AutoCloseable {

    /**
     * Max number of messages in a log file.
     * <p>
     * Once this limit is reached the log file will switch.
     */
    protected static final int MAX_MESSAGES_PER_FILE = 100000;

    /**
     * File extension for log files.
     */
    protected static final String LOG_FILE_EXTENSION = "log";

    /**
     * How many messages are in this log file.
     */
    protected int howManyMessagesInThisFile = 0;

    /**
     * A mnemonic prefix used for naming the log files
     */
    protected String thePrefix = "";

    /**
     * The Year/Month/Day format in use
     */
    protected SimpleDateFormat logFileDayFormat;

    /**
     * The Hour/Minute format in use
     */
    protected SimpleDateFormat logFileTimeFormat;

    /**
     * The directory log files will be kept in.
     */
    protected File theDirectoryFile = null;

    /**
     * The current log file
     */
    protected File theCurrentLogFile = null;

    /**
     * A flag that is set if we can't write any more
     */
    protected boolean iAmBroken = false;

    /**
     * Whether messages are to be printed in standard output/error
     * as well as written to a log file
     */
    protected boolean consoleOutput = false;

    /**
     * Fileoutputstream for log file
     */
    private java.io.FileOutputStream theFileStream;

    /**
     * Printwriter for log file
     */
    private java.io.PrintWriter thePrintWriter;

    /**
     * Create an instance of TextLog. The log files will reside in the current working directory.
     *
     * @param  thePrefix A mnemonic prefix used for naming the log file.
     */
    public TextLog(String thePrefix) {
        this(System.getProperty("user.dir"), thePrefix);
    }

    /**
     * Create an instance of TextLog.  The log files will reside in the directory specified.
     *
     * @param  theDirectoryName the absolute path of the logging directory.
     * @param  thePrefix An mnemonic prefix used for naming the log file.
     */
    public TextLog(String theDirectoryName, String thePrefix) {
        super();
        this.thePrefix = new String(thePrefix);
        logFileDayFormat = new SimpleDateFormat(LogInterface.DEFAULT_FILENAME_DAY_FORMAT);
        logFileTimeFormat = new SimpleDateFormat(LogInterface.DEFAULT_FILENAME_TIME_FORMAT);

        // Try and find a viable log directory...
        setDirectory(theDirectoryName);

        if (theDirectoryFile == null) {
            iAmBroken = true;
            setConsoleOutput(true);
        } else {
            setConsoleOutput(false);
        }


        setAutoLog(true);

    }

    /**
     * Set the directory name. If the value passed in in unusable user.dir and
     * user.home are both evaluated as alternatives.
     *
     * @param theDirectoryName One of user.dir, user.home or a directory name.
     */
    private void setDirectory(String theDirectoryName) {
        File tempFile = null;
        iAmBroken = false;

        String actualDirectory = new String(theDirectoryName);

        if (actualDirectory == null || actualDirectory.length() == 0) {
            actualDirectory = System.getProperty("user.dir");
        } else if (actualDirectory.equals("user.dir")
                || actualDirectory.equals("user.home")) {
            actualDirectory = System.getProperty(actualDirectory);
        }

        final String[] possibleDirectories =
                {actualDirectory  // Directory requested by user.
                        , System.getProperty("user.home") + File.separator + Namer.param_prod_name// users home directory + subdir
                        , System.getProperty("user.home") // users home directory
                        , System.getProperty("user.dir") // current working directory
                };

        for (int i = 0; i < possibleDirectories.length; i++) {
            try {
                tempFile = new File(possibleDirectories[i]);

                if (!tempFile.exists()) {
                    tempFile.mkdirs();
                }

                if (tempFile.exists()
                        && tempFile.canRead()
                        && tempFile.canWrite()
                        && tempFile.isDirectory()) {
                    theDirectoryFile = tempFile;
                    switchLogFileIfAppropriate();
                    if (!iAmBroken) {
                        System.out.println("Log messages being written to directory " + theDirectoryFile.getAbsolutePath());
                        break;
                    }

                } else {
                    System.err.println("Log Failure: Unable to use Directory " + tempFile.getAbsolutePath());
                    tempFile = null;
                }
            } catch (Exception e) {
                tempFile = null;
            }
        }

    }

    /**
     * Format a message and log it to the current log file.
     *
     * @param   messageType should be one of the constants DEBUG, INFO, WARN, ERROR or SYSERR as defined in LogInterface.
     * @param   messageText The message
     * @param  isModal ignored as we are dumping messages to a file.
     * @param  isLogged whether the message is logged or not.
     */
    protected synchronized void writeMessage(String messageType, String messageText, boolean isModal, boolean isLogged) {
        String outputMessage = formatMessage(messageType, messageText);

        if (consoleOutput || iAmBroken) {
            if (messageType.equals(LogInterface.INFO)) {
                System.out.println(outputMessage);
            } else {
                System.err.println(outputMessage);
            }
        }

        if (!iAmBroken) {
            if (isLogged) {
                switchLogFileIfAppropriate();
                {
                    thePrintWriter.println(outputMessage);
                    howManyMessagesInThisFile++;
                }
            }

            if (autoFlush) {
                flush();
            }
        }
    }

    /**
     * figure out what the date part of the log file should be called
     */
    protected String getLogFileDate() {
        java.util.Date tempDate = new java.util.Date();
        return (logFileDayFormat.format(tempDate));
    }

    /**
     * figure out what the time part of the log file should be called
     */
    protected String getLogFileTime() {
        java.util.Date tempDate = new java.util.Date();
        return (logFileTimeFormat.format(tempDate));
    }

    /**
     * Return a new log file name.
     */
    protected String getLogFileName(String thePrefix, String fileExtension) {
        return (thePrefix + getLogFileDate() + "_" + getLogFileTime() + "." + fileExtension);
    }

    /**
     * Change the log file if needed.
     * There are 4 reasons we'd want to do this:
     * <P>We've just started and the current log file is <code>null</code>.
     * <P>We've reached <code>MAX_MESSAGES_PER_FILE</code> and need to start a new one.
     * <P>The day (as defined by <codeLogInterface.DEFAULT_FILENAME_DAY_FORMAT</code>) has changed so we need to start a new file.
     */
    private boolean switchLogFileIfAppropriate() {
        boolean switchedLogFile = false;

        String newLogFileName = "";
        String unableToCloseMessage = "";
        boolean newLogFileAlreadyExisted = false;

        // if we haven't started a log file
        // OR we have too many messages in the current log file
        // OR the current log file's name is no longer that of the current day
        if (theCurrentLogFile == null
                || howManyMessagesInThisFile >= MAX_MESSAGES_PER_FILE
                || (!theCurrentLogFile.getName().startsWith(thePrefix + this.getLogFileDate()))) {
            switchedLogFile = true;

            // Figure out what the new file is called.
            newLogFileName = getLogFileName(thePrefix, LOG_FILE_EXTENSION);

            // if we currently have a printwriter/Filestream flush and close them.
            this.flush();
            try {
                if (thePrintWriter != null) {
                    thePrintWriter.close();
                    theFileStream.close();
                }
            } catch (java.io.IOException e) {
                unableToCloseMessage = new String("Unable to close log file " + theCurrentLogFile.getAbsolutePath() + " :" + e.getMessage());
            }

            try {
                // Create new log file.
                theCurrentLogFile = new File(theDirectoryFile, newLogFileName);

                if (theCurrentLogFile.exists()) {
                    newLogFileAlreadyExisted = true;
                }

                // Create new File Stream
                theFileStream = new FileOutputStream(theCurrentLogFile);

                // Create new Printwriter
                thePrintWriter = new PrintWriter(theFileStream);

                // Reset message counter...
                howManyMessagesInThisFile = 0;

                // Print out unable to close message if length > 0...
                if (unableToCloseMessage.length() > 0) {
                    thePrintWriter.println(formatMessage(LogInterface.ERROR, unableToCloseMessage));
                }

                // Print out message if log file already existed....
                if (newLogFileAlreadyExisted) {
                    thePrintWriter.println(formatMessage(LogInterface.WARN, "Log file " + theCurrentLogFile.getAbsolutePath() + " overwrites another file with the same name"));
                }

            } catch (java.io.IOException e) {
                System.err.println("Error: Unable to open Log File " + theCurrentLogFile.getAbsolutePath());
                iAmBroken = true;
            }

        }

        return (switchedLogFile);
    }

    /**
     * Flush and close the current log file.
     */
    public void closeFile() {
        try {
            if (!iAmBroken
                    && theCurrentLogFile != null) {
                info("Log file closed", false, true);
                flush();
                theFileStream.close();
                theCurrentLogFile = null;
                thePrintWriter = null;
                theFileStream = null;
            }
        } catch (java.io.IOException e) {
            System.err.println("Error: Unable to flush and close outputStream for file " + theCurrentLogFile);
            iAmBroken = true;
        }
    }

    /**
     * Flush any outstanding changes.
     */
    public void flush() {
        try {
            if (!iAmBroken
                    && thePrintWriter != null) {
                thePrintWriter.flush();
            }
        } catch (Exception e) {
            System.err.println("Error: Unable to flush outputStream for file " + theCurrentLogFile);
            iAmBroken = true;
        }
    }

    /**
     * Get current log file name.
     *
     * @return An absolute file name if we are writing to a log file.
     *         "NONE" If we haven't started writing to a file yet. Log files aren't opened until the first call to <code>writeMessage</code>
     *         "BROKEN" If we were unable to open a log file.
     */
    public String getCurrentLog() {
        String tempLogFileName = "";

        if (iAmBroken) {
            tempLogFileName = "BROKEN";
        } else if (theCurrentLogFile == null) {
            tempLogFileName = "NONE";
        } else {
            tempLogFileName = theCurrentLogFile.getAbsolutePath();
        }

        return (tempLogFileName);
    }

    /**
     * Find out whether messages are being sent to standard output and standard error.
     */
    public boolean getConsoleOutput() {
        return (consoleOutput);
    }

    /**
     * Set whether messages are to be sent to standard output and standard error.
     */
    public void setConsoleOutput(boolean sendOutputToConsole) {
        consoleOutput = sendOutputToConsole;
    }

    /**
     * Close log file if currently open.
     */
    public void close() {
        closeFile();
    }

}







