package com.mcpdbwizard.pub;

/**
 * Thrown when a Timestamp contains nanoseconds but the Oracle table/record
 * it will be used against is DATA and doesn't support nanonseconds.
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 */

public class CSNoNanosAllowedException extends CSException {
    public CSNoNanosAllowedException(String location
            , java.sql.Timestamp ts) {
        super(location + ":Nanoseconds found where none allowed. Value is :" + ts.toString());
    }
}
