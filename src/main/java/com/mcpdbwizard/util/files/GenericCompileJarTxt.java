package com.mcpdbwizard.util.files;

import com.mcpdbwizard.util.AbstractBaseStoredFile;

/**
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class GenericCompileJarTxt extends AbstractBaseStoredFile {
    public GenericCompileJarTxt() {
        super();

        this.name = "generic_compile_jar.txt";
        this.descr = "Compile and JAR files after generation";
        this.os = "Windows";

        String[] lines = {
                "echo \"XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX\""
                , " echo Code Directory = %1"
                , "echo Package name (dot) = %2"
                , "echo Package name (slash) = %3"
                , "echo Interface Class = %4"
                , "echo Impl Class = %5"
                , ""
                , "echo \"XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX\""
        };


        this.contents = lines;
    }
}
