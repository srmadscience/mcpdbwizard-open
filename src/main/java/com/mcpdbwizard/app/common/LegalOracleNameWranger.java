package com.mcpdbwizard.app.common;

import java.util.HashMap;

import com.mcpdbwizard.test.TestInterface;
import com.mcpdbwizard.pub.LogInterface;

/**
 * @author devteam@mcpdbwizard.com
 * Copyright 2010-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 * @version 6
 */

public class LegalOracleNameWranger //implements TestInterface
{

    final int MAX_LENGTH = 30;
    final String[] STRIPABLE_CHARS = {"_", "$", "A", "E", "I", "O", "U"};
    java.util.HashMap knownNames = new HashMap();
    int arbCount = 0;

    LogInterface theLog;

    public LegalOracleNameWranger(LogInterface theLog) {
        this.theLog = theLog;
    }

    public static void main(String[] args) {
        com.mcpdbwizard.pub.ConsoleLog theLog = new com.mcpdbwizard.pub.ConsoleLog();
        theLog.debugOn();
        theLog.info("Start");

        LegalOracleNameWranger w = new LegalOracleNameWranger(theLog);
        w.test(false);

        theLog.info("End");

    }

    public String createName(String pStartName) {

        // Test obvious case - no problem
        if (pStartName == null) {
            return pStartName;
        }

        String startValue = pStartName.toUpperCase();
        String returnValue = pStartName.toUpperCase();


        // See if already mapped...
        if (haveKey(startValue)) {
            returnValue = getKey(startValue);
            return returnValue;
        }

        // Test not null but ok
        if (returnValue.length() <= MAX_LENGTH && !haveValue(startValue)) {
            addKeyValue(startValue, returnValue);
            return returnValue;
        }

        // First pass - remove STRIPABLE_CHARS
        for (int i = 0; i < STRIPABLE_CHARS.length; i++) {
            returnValue = JavaUtils.replaceString(returnValue, STRIPABLE_CHARS[i], "");

            if (returnValue.length() <= MAX_LENGTH) {
                if (!haveValue(returnValue)) {
                    addKeyValue(startValue, returnValue);
                    return returnValue;
                }
            }

        }

        // Have now tried obvious stuff. See if we can just drop first x chars...
        if (returnValue.length() > MAX_LENGTH) {
            // try dropping front chars
            if (!haveValue(returnValue.substring(returnValue.length() - MAX_LENGTH))) {
                returnValue = returnValue.substring(returnValue.length() - MAX_LENGTH);
                addKeyValue(startValue, returnValue);
                return returnValue;
            }

            // try dropping back chars - unlikely to work.
            if (!haveValue(returnValue.substring(0, MAX_LENGTH))) {
                returnValue = returnValue.substring(0, MAX_LENGTH);
                addKeyValue(startValue, returnValue);
                return returnValue;
            }


        }

        // Now we get desperate...
        returnValue = getNextArbName(startValue);
        addKeyValue(startValue, returnValue);

        return returnValue;
    }

    private boolean haveKey(String pString) {
        return knownNames.containsKey(pString);
    }

    private String getKey(String pString) {
        return (String) knownNames.get(pString);
    }

    private boolean haveValue(String pString) {
        return knownNames.containsValue(pString);
    }

    private void addKeyValue(String pKey, String pValue) {
        knownNames.put(pKey, pValue);
    }

    // test stuff

    private String getNextArbName(String pValue) {
        return "ARBITRARY_VARIABLE_NAME_" + arbCount++;
    }

    public String getTestName() {
        return ("LegalOracleNameWranger");
    }

    public boolean test(boolean isInteractive) {
        boolean result = true;

        theLog.info("null test");
        createName(null);

        String[][] testValues = {
                {"A", "A"}
                , {"B", "B"}
                , {"C", "C"}
                , {"VERY_LONG_______________________________________ID", "VERYLONGID"}
                , {"VERY_LONG_UDUDUDUDUDUDUDUDUDDUDUDUDUDUDUDUDUDUDUDUDU", "VRYLNGDDDDDDDDDDDDDDDDDDDDD"}   //strip vowel
                , {"VRYLNGDDDDDDDDDDDDDDDDDDDDD", "ARBITRARY_VARIABLE_NAME_0"} // dup value of value
                , {"QRYLNGDDDDDDDDDDDDDDDDDDDDQQQQ123454567890", "DDDDDDDDDDDDDDQQQQ123454567890"}  // lose first n chars
                , {"ZDDDDDDDDDDDDDQQQQ123454567890QRYLNGDDDDDDDDDDDDDDDDDDDDQQQQ123454567890", "ZDDDDDDDDDDDDDQQQQ123454567890"}  // trimsurplus
//   , { "1DDDDDDDDDDDDQQQQ123454567890EE",   "1DDDDDDDDDDDDQQQQ123454567890E"}  // trim back
                //  , { "2DDDDDDDDDDDDQQQQ123454567890FF",   "DDDDDDDDDDDDDQQQQ123454567890F"}  // trim back
                //  123456789012345678901234567890   //  123456789012345678901234567890
        };

        for (int i = 0; i < testValues.length; i++) {
            theLog.info("Key = " + testValues[i][0]
                    + " Value should be " + testValues[i][1]);
            String testResult = createName(testValues[i][0]);
            if (!testResult.equals(testValues[i][1])) {
                result = false;
                theLog.error("Bad Result of key " + testValues[i][0] + " Seen: " + testResult);
            }
        }


        return result;
    }


}