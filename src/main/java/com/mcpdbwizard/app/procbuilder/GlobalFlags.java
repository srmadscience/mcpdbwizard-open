package com.mcpdbwizard.app.procbuilder;

/**
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class GlobalFlags {

    public static final int HAS_FILES = 0;
    public static final int HAS_BFILES = 1;
    public static final int HAS_ROWSETS = 2;
    public static final int HAS_IDS_ARRAY = 3;
    public static final int HAS_IYM_ARRAY = 4;
    public static final int HAS_TZ_ARRAY = 5;
    public static final int HAS_TZTS_ARRAY = 6;
    public static final int HAS_TZLTS_ARRAY = 7;
    public static final int HAS_BLOB_SCALER_ARRAY = 8;
    public static final int HAS_CLOB_SCALER_ARRAY = 9;
    public static final int HAS_BFILE_SCALER_ARRAY = 10;
    public static final int HAS_NUMBER_INDEXBY_ARRAY = 11;
    public static final int HAS_STRING_INDEXBY_ARRAY = 12;
    public static final int HAS_TZLTS = 13;
    public static final int HAS_DATE_INDEXBY_ARRAY = 14;
    public static final int HAS_RAW_INDEXBY_ARRAY = 15;
    public static final int HAS_TIMESTAMP_INDEXBY_ARRAY = 16;

    public static final int XMLTYPE_IN_USE = 17;
    public static final int XMLJAR_NEEDED = 18;

    public static final int ARRAYS_OBJ_IN_USE = 19;
    public static final int ARRAYS_PCK_IN_USE = 20;
    public static final int ARRAYS_IDX_IN_USE = 21;
    public static final int ROWTYPE_IN_USE = 22;
    public static final int HAS_OPAQUE_SCALER_ARRAY = 23;

    public static final int SDO_IN_USE = 24;
    public static final int SDOJAR_NEEDED = 25;
    public static final int HAS_STRUCT_SCALER_ARRAY = 26;


    private final int flagCount = 27;

    private boolean[] flags = null;


    public GlobalFlags() {
        reset();
    }

    public void reset() {
        flags = new boolean[flagCount];

        for (int i = 0; i < flags.length; i++) {
            flags[i] = false;
        }
    }

    public void setFlag(int theFlag) {
        flags[theFlag] = true;
    }

    public boolean getFlag(int theFlag) {
        return (flags[theFlag]);
    }


}

