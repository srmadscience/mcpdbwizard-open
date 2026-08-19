package com.mcpdbwizard.util.files;

import com.mcpdbwizard.util.AbstractBaseStoredFile;

/**
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class GenericBatchTxt extends AbstractBaseStoredFile {
    public GenericBatchTxt() {
        super();

        this.name = "generic_batch.txt";
        this.descr = "A Generic Batch File that you can modify as you wish";
        this.os = "Windows";

        String[] lines = {
                "set CODEDIR=\"PARAM_CODE_BASE_DIRECTORY\""
                , "set OUTDIR=%CODEDIR%"
                , "set PNAME=PARAM_PACKAGE_NAME"
                , "set PNAME_S=PARAM_PACKAGE_NAME_SLASH"
                , "set JDBCJAR=\"PARAM_JDBCJAR\""
                , "set SDO=\"PARAM_SDOJAR\""
                , "set XML=\"PARAM_XMLJAR\""
                , "set JAVAC=\"PARAM_JAVAC\""
                , "set PUBJAR=\"PARAM_PUBJAR\""
                , "set DFNAME=PARAM_DAO_FACTORY_NAME"
                , "set WS_INTER=PARAM_WS_INTERFACE_NAME"
                , "set WS_IMPL=PARAM_WS_IMPL_NAME"
        };


        this.contents = lines;
    }
}
