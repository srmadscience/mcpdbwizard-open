package com.mcpdbwizard.app.common;

import java.lang.Integer;
import javax.swing.*;

import com.mcpdbwizard.pub.*;

/**
 * A collection of static methods that are of use to SWING Gui's
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 */
public class GUIUtils {

    /**
     * A collection of static methods that are of use to SWING Gui's
     *
     */
    public GUIUtils() {
    }

    /**
     * Attempt to turn input value into an <code>int</code>.
     *
     * @param  theString A String that is supposedly a valid <code>int</code>.
     * @param     theDefault what to return if theString isn't an <code>int</code>.
     * @return an int containing either <code>theString</code> or <code>theDefault</code>
     */
    public static int StringToInt(String theString, int theDefault) {
        Integer mrBigInt = null;
        int mrLittleInt = theDefault;

        try {
            mrBigInt =  Integer.valueOf(theString);
            mrLittleInt = mrBigInt.intValue();
            return (mrLittleInt);
        } catch (Exception e) {
            return (theDefault);
        }
    }

    /**
     * Attempt to turn input value into an <code>java.lang.Integer</code>.
     *
     * @param  theString A String that is supposedly a valid <code>java.lang.Integer</code>.
     * @param     theDefault what to return if theString isn't an <code>int</code>.
     * @return an java.lang.Integer containing either <code>theString</code> or <code>theDefault</code>
     */
    public static Integer StringToInteger(String theString, int theDefault) {
        Integer mrBigInt = null;

        try {
            mrBigInt =  Integer.valueOf(theString);
            return (mrBigInt);
        } catch (Exception e) {
            return (theDefault);
        }
    }

    /**
     * Map LogInterface message types to JOptionPane message types
     *
     * @param  messageType A valid LogInterface messageType, such as <code>
     *               LogInterface.DEBUG, LogInterface.INFO, LogInterface.WARN, LogInterface.ERROR, LogInterface.SYSERR
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


}



