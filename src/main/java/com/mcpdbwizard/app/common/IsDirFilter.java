package com.mcpdbwizard.app.common;

import javax.swing.filechooser.*;
import java.io.File;

/**
 * @author devteam@mcpdbwizard.com
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 * @version 2
 */
public class IsDirFilter extends FileFilter {

    public IsDirFilter() {
    }

    public boolean accept(File dir) {
        return dir.isDirectory();
    }

    public String getDescription() {
        return ("Directories");
    }
}


