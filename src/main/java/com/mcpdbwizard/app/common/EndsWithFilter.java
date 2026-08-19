package com.mcpdbwizard.app.common;

import javax.swing.filechooser.*;
import java.io.File;
import java.io.FilenameFilter;

/**
 * @author devteam@mcpdbwizard.com
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 * @version 2
 */
public class EndsWithFilter extends FileFilter implements FilenameFilter {

    private String fileSuffix;
    private String descr;

    public EndsWithFilter(String fileSuffix, String descr) {
        this.fileSuffix = fileSuffix;
        this.descr = descr;
    }

    public boolean accept(File aFile) {
        boolean acceptThis = false;
        if (aFile.getName().toLowerCase().endsWith(fileSuffix.toLowerCase())) {
            acceptThis = true;
        } else if (aFile.isDirectory()) {
            acceptThis = true;
        }
        return (acceptThis);
    }

    public String getDescription() {
        return (descr);
    }

    public boolean accept(File dir, String name) {
        return name.toLowerCase().endsWith(fileSuffix.toLowerCase());
    }
}


