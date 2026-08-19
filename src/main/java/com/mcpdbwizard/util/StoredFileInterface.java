package com.mcpdbwizard.util;

import java.sql.Connection;
import java.util.Properties;

import com.mcpdbwizard.pub.LogInterface;

/**
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public interface StoredFileInterface {
    public final String SDO_JARS = "sdoapi.jar";
    public String[] XDB_JARS = {"xdb.jar", "xmlparserv2.jar"};

    public String toString();  // returns name

    public String getDescr();

    public String[] getContents(Properties paramList, LogInterface theLog, Connection mrConnection);

    public String getOs();
}
