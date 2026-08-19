package com.mcpdbwizard.app.common;

import java.util.ArrayList;

import com.mcpdbwizard.app.procbuilder.*;

/**
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class SqlStatementWranglerManager {

    ArrayList theArray = null;

    public SqlStatementWranglerManager(SqlStatementWrangler[] paramArray, String[] errorArray) {
        theArray = new ArrayList(paramArray.length);

        for (int i = 0; i < paramArray.length; i++) {
            theArray.add(paramArray[i]);
        }
    }
} 