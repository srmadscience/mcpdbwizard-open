package com.mcpdbwizard.pub;

import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.util.ArrayList;

import com.mcpdbwizard.pub.*;

/**
 * This is an implementation of
 * LogInterface that uses a <code>javax.swing.JLabel</code> field to display messages.
 * it also does modal messages and will log them if asked.
 * <p>
 * See <a href="https://mcpdbwizard.com/faq/logging">LogInterface</a>
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 */
public class UiLog extends TextLog implements LogInterface {
    /**
     * A constant used in formating messages in the field <code>theLabel</code>
     */
    private static final String MESSAGE_DELIMITER = ":";
    /**
     * A JLabel. We assume that it's a suitable location for a message, such as a
     * line at the bottom of the screen.
     */
    JLabel theLabel;
    /**
     * A JTextArea. We assume that it's a suitable location for a message and has multiple lines
     *
     */
    JTextArea theTextArea;
    /**
     * Array for storing messages
     */
    ArrayList messageArray;
    /**
     * Number of message lines
     */
    int lines = 100;
    /**
     * Flag for whether text area is active or not
     */
    boolean useTextArea = false;
    /**
     * The current application frame. We need to know this so we can refresh the graphics context
     * so that messages will become visible to the end user.
     */
    JFrame theFrame;

    /**
     * Create an instance of UiLog.
     *
     * @param theDirectoryName  A string containing a logging directory path name. If the directory doesn't
     *               exist we'll attempt to create it. If we can't create it then we lose the ability to log messages.
     * @param thePrefix  A string containing a the prefix used to name the log files.
     * @param theLabel  A JLabel. We assume that it's a suitable location for a message, such as a
     *               line at the bottom of the screen.
     * @param theFrame The current application frame. We need to know this in order to refresh the graphics context
     *               so that messages will become visible to the end user.
     *
     */
    public UiLog(String theDirectoryName, String thePrefix, JLabel theLabel, JFrame theFrame) {
        super(theDirectoryName, thePrefix);
        this.theLabel = theLabel;
        this.theFrame = theFrame;
    }

    /**
     * Map LogInterface message types to JOptionPane message types
     *
     * @param messageType A valid LogInterface messageType, such as <code>
     *               LogInterface.DEBUG, LogInterface.INFO, LogInterface.WARN, LogInterface.ERROR, LogInterface.SYSERR</code>
     * @return A JOptionPane message type.
     * @see com.mcpdbwizard.pub.LogInterface
     * @see javax.swing.JOptionPane
     */
    public static int mapMessageType(String messageType) {
        int newMessageType = JOptionPane.INFORMATION_MESSAGE;

        if (messageType.equals(LogInterface.DEBUG)) {
            newMessageType = JOptionPane.QUESTION_MESSAGE;
        } else if (messageType.equals(LogInterface.INFO)) {
            newMessageType = JOptionPane.INFORMATION_MESSAGE;
        } else if (messageType.equals(LogInterface.WARN)) {
            newMessageType = JOptionPane.WARNING_MESSAGE;
        } else if (messageType.equals(LogInterface.ERROR)) {
            newMessageType = JOptionPane.ERROR_MESSAGE;
        } else if (messageType.equals(LogInterface.SYSERR)) {
            newMessageType = JOptionPane.ERROR_MESSAGE;
        }

        return (newMessageType);
    }

    /**
     * Prevent or allow messages from being written to the text area as well
     * as the log file.
     */
    public void useJTextArea(boolean useArea) {
        this.useTextArea = useArea;
    }

    /**
     * Clear the text area
     */
    public void clearJTextArea() {
        theTextArea.setText("");
        messageArray = new ArrayList(lines);
    }

    /**
     * Set a new Text Area
     */
    public void addJTextArea(JTextArea newTextArea, int lines) {
        this.lines = lines;

        theTextArea = newTextArea;
        theTextArea.setText("");
        theTextArea.invalidate();
        theTextArea.validate();
        theFrame.paintAll(theFrame.getGraphics());
        messageArray = new ArrayList(lines);
    }

    /**
     * Format a message and write it to the JLabel <code>theLabel</code>.
     *
     * @param messageType should be one of the constants DEBUG, INFO, WARN, ERROR or SYSERR as defined in LogInterface.
     * @param messageText The message text
     * @param isModal If <code>true</code> then a modal window will
     *                also appear and force the user to acknowledge the message.
     * @param isLogged Whether this message is written to a log file. Passed to the parent class.
     */
    protected void writeMessage(String messageType, String messageText, boolean isModal, boolean isLogged) {
        // Set text in label intended for bottom of screen
        theLabel.setText(messageType + MESSAGE_DELIMITER + " " + messageText);

        // Pass request to parent text log object
        super.writeMessage(messageType, messageText, isModal, isLogged);

        // If this is a modal message pop up a suitable window.
        if (isModal) {
            // If the message is modal then it must be ok to spend time flushing the log...
            flush();

            JOptionPane.showMessageDialog(theFrame, messageText, messageType, mapMessageType(messageType));
        }

        if (theTextArea != null && useTextArea) {
            if (theTextArea.getLineCount() >= lines) {
                theTextArea.replaceRange("", 0, ((String) messageArray.get(0)).length());
                messageArray.remove(0);
            }

            messageArray.add(messageText + "\n");
            theTextArea.append(messageText + "\n");
        }

        // Refresh screen so message becomes visible.
        theFrame.paintAll(theFrame.getGraphics());
    }

}



