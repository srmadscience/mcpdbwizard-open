package com.mcpdbwizard.util.files;

import com.mcpdbwizard.util.AbstractBaseStoredFile;

/**
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class GenericCompileTxt extends AbstractBaseStoredFile {
    public GenericCompileTxt() {
        super();

        this.name = "generic_compile.txt";
        this.descr = "A Generic Batch File That Compiles The code";
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
                , ""
                , "echo Code Directory=\"%CODEDIR%\" "
                , "echo Package name (dot)=\"%PNAME%\""
                , "echo Package name (slash)=\"%PNAME_S%\""
                , "echo JDBC Driver=\"%JDBCJAR%\""
                , "echo SDO=\"%SDO%\""
                , "echo XML=\"%XML%\""
                , "echo Javac=\"%JAVAC%\""
                , "echo com.mcpdbwizard.pub=\"%PUBJAR%\""
                , "echo DAO class name=\"%DFNAME%\""
                , "echo Interface Class %WS_INTER%"
                , "echo Impl class=%WS_IMPL%"
                , ""
                , ""
                , "%JAVAC% -d %OUTDIR%  -classpath %PUBJAR%;%JDBCJAR% %PNAME_S%\\%WS_IMPL%.java "
                , ""
                , "dir /s %OUTDIR%"
                , ""
                , ""
                , ""
        };


        this.contents = lines;
    }
}
