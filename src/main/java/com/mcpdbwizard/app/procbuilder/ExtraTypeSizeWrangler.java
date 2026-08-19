package com.mcpdbwizard.app.procbuilder;

import com.mcpdbwizard.app.common.*;
import com.mcpdbwizard.app.procbuilder.gui.*;

import java.util.ArrayList;

import com.mcpdbwizard.pub.*;

/**
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class ExtraTypeSizeWrangler {


    String name = null;
    String sizeString = "";
    String dataType = null;
    Integer dataLength = null;


    public ExtraTypeSizeWrangler(ReadOnlyRowSet theRowSet, String name, boolean isGeneric) throws CSException {
        dataType = theRowSet.getString("DATA_TYPE");
        dataLength = theRowSet.getIntegerObj("DATA_LENGTH");

        if (dataLength == null) {
            dataLength = 0;
        }

        this.name = JavaUtils.mapOracleDatatypeToAlphaChar(dataType, dataLength.intValue()).toUpperCase();
        dataLength = JavaUtils.mapOracleDatatypeToLength(dataType, dataLength.intValue());
        if (dataLength.intValue() > 0 && (!dataType.equals("NUMBER"))) {
            sizeString = "(" + dataLength.intValue() + ")";
        }

        if (dataType.indexOf("TIMESTAMP") > -1) {
            sizeString = "(9)";
        }
    }

    public String getName() {

        return (name);
    }

    public String appendName(String oldName) {

        return (oldName + name);
    }

    public String getSizeDef() {
        return (sizeString);
    }

} 