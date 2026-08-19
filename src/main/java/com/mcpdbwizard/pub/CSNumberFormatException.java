package com.mcpdbwizard.pub;

/**
 * Thrown when an attempt to turn a String into a number fails
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 */
public class CSNumberFormatException extends CSException {

    public CSNumberFormatException(String callLocation
            , String value) {
        super(callLocation + ":" + value);
    }
}
