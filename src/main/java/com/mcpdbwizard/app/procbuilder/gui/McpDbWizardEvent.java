package com.mcpdbwizard.app.procbuilder.gui;

/**
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class McpDbWizardEvent {


    public static final int LOGGED_IN = 0;
    public static final int NOT_LOGGED_IN = 1;
    public static final int REPAINT_NEEDED = 2;
    public static final int FILE_CREATED = 3;

    public static final int ALL_SEQUENCES_SELECTED = 4;
    public static final int NO_SEQUENCES_SELECTED = 5;

    public static final int ALL_FUNCTIONS_SELECTED = 6;
    public static final int NO_FUNCTIONS_SELECTED = 7;

    public static final int ALL_TABLES_SELECTED = 8;
    public static final int NO_TABLES_SELECTED = 9;

    public static final int ALL_ASP_SELECTED = 10;
    public static final int NO_ASP_SELECTED = 11;

    public static final int REFRESH_NEEDED = 12;
    public static final int CONFIG_FILE_WRITTEN = 14;
    public static final int APACHE_LOG4J_IN_USE = 15;
    public static final int JAVA_UTIL_LOGGING_IN_USE = 16;

    public static final int CONFIG_CHANGED = 18;
    public static final int GENERATION_STARTED = 19;
    public static final int GENERATION_FINISHED = 20;

    public static final int CHECK_FOR_OTHER_FILES_REQUESTED = 21;
    public static final int NEED_EC_TO_OS_TRANSLATION = 22;
    public static final int NEED_OS_TO_EC_TRANSLATION = 23;

    public static final int A_SEQUENCE_SELECTED = 24;
    public static final int A_FUNCTION_SELECTED = 25;
    public static final int A_TABLE_SELECTED = 26;
    public static final int A_ASP_SELECTED = 27;

    public static final int FUNCTION_TABLE_LOADED = 28;
    public static final int SEQUENCE_TABLE_LOADED = 29;
    public static final int TABLE_TABLE_LOADED = 30;

    public static final int FUNCTION_TABLE_ABOUT_TO_SAVE = 31;
    public static final int SEQUENCE_TABLE_ABOUT_TO_SAVE = 32;
    public static final int TABLE_TABLE_ABOUT_TO_SAVE = 33;

    public static final int TRYING_TO_LOG_IN = 34;
    public static final int UNABLE_TO_LOG_IN = 35;

    public static final int NEED_EC33_CODE_DIR_TRANSLATED = 36;
    public static final int NEED_EC33_SQL_DIR_TRANSLATED = 37;

    public static final int ORACLE_JDBC_DRIVER_NEEDED = 38;

    public static final int NEED_EC33_BASEDIR = 39;
    public static final int NEED_EC33_DEFAULT_SQLDIR = 40;

    public static final int NEED_CLASSPATH = 41;

    public static final int XMLTYPE_IN_USE = 43;
    public static final int XMLJAR_NEEDED = 44;

    public static final int ARRAYS_OBJ_IN_USE = 45;
    public static final int ARRAYS_PCK_IN_USE = 46;
    public static final int ARRAYS_IDX_IN_USE = 47;

    public static final int ROWTYPE_IN_USE = 48;

    public static final int SDO_IN_USE = 49;
    public static final int SDOJAR_NEEDED = 50;


    int event = -1;
    Object thing = null;
    Object thing2 = null;

    public McpDbWizardEvent(int event) {
        this.event = event;
    }

    public Object getThing() {
        return (thing);
    }

    public void setThing(Object thing) {
        this.thing = thing;
    }

    public Object getThing2() {
        return (thing2);
    }

    public void setThing2(Object thing2) {
        this.thing2 = thing2;
    }

    public int getEvent() {
        return (event);
    }
}


