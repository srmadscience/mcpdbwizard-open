package com.mcpdbwizard.pub;

/**
 * Static class for providing information about the MCPDBWizard Public Library.
 * <p>
 * Under normal circumstances <a href="https://mcpdbwizard.com" target="_blank" class="manual">MCPDBWizard</a> users
 * will have no reason to use this class directly - the generated code will use it.
 * <br>Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 */

public class LibraryInfo {
    public LibraryInfo() {
    }

    /**
     * Return version of public library we are using.
     */
    public static String getLibraryVersion() {
        return ("PARAM_DB_VERSION");
    }

    /**
     * Return build of public library we are using.
     */
    public static String getProductVersion() {
        // Namer is in this package precisely so this does not have to be hand-synced:
        // pub ships with generated code and cannot reach into com.mcpdbwizard.app.
        return (Namer.param_product_version + "." + Namer.param_build);
    }
}


