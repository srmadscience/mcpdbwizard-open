package com.mcpdbwizard.app.procbuilder.gui;

//import javax.swing.UIManager;

import java.awt.*;
import java.io.*;

import com.mcpdbwizard.pub.*;

//import com.mcpdbwizard.app.procbuilder.*;
//import com.mcpdbwizard.app.common.gui.*;

/**
 * @author devteam@mcpdbwizard.com
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 * @version 2
 */
public class ProcBuilder {
    static final String PRODUCT_NAME = Namer.param_product_name;
    static final String FILE_EXTENSION = ".pb2";
    static final String CBD = "CODE_BASE_DIRECTORY=";
    static final String SFD = "SQL_FILE_DIRECTORY=";
    boolean packFrame = false;
    ApplicationShell mrApplicationShell;

    /**
     * Construct the application.
     *
     * <p><b>The leading {@code access_code} argument was REMOVED in 2026-08.</b> The command line
     * is now {@code <log_file_directory> [build] <configfile>...}; it used to carry a 19-character
     * code in front of that.
     *
     * <p>It was removed because it did nothing. The value was validated for SHAPE only — at least
     * 19 characters, not the literal {@code build}, no {@code : . / \}, not an absolute path — and
     * then handed to {@link ApplicationShell}, whose constructors ignored the parameter entirely.
     * It was never stored, compared, hashed or checked against anything. Any 19-character string
     * without punctuation worked, which is what the estate scripts had been passing for years.
     *
     * <p>Those checks are an argument-order guard rather than a licence test — {@code build}, a
     * colon, a dot, a slash and an absolute path are exactly what you get if someone passes the log
     * directory or a filename first — and are the residue of a licensing scheme this code no longer
     * has. Keeping it meant publishing a generator whose first argument is a meaningless secret,
     * and a value that looks like a credential to every reader and every secret scanner.
     */
    public ProcBuilder(String[] args) {

        if (args.length < 2) {
            printUsageAndExit();
        }

        String logName = new String(args[0]);

        File logFileDir = new File(logName);

        try {
            if (!logFileDir.exists()) {
                logFileDir.mkdirs();
                System.out.println("Log directory " + logFileDir.getAbsolutePath() + " created");
            }
        } catch (Exception e) {
            System.err.println(e.toString());
        }

        if (logFileDir.exists()
                && logFileDir.isDirectory()
                && logFileDir.canRead()
                && logFileDir.canWrite()) {
            // DeMorgan be dammed.....
        } else {

            System.out.println("Error: Log file Directory " + logFileDir.getAbsolutePath() + " does not exist");
            //System.out.println("");
            //System.exit(1);
        }

        if (args[1].equalsIgnoreCase("build")) {
            if (args.length == 2) {
                System.err.println("Error: No properties file specified");
                System.exit(1);
            }

            // Allow params to be overridden in batch mode
            String overideCodeBaseDirectory = "";
            String overideSqlFileDirectory = "";

            for (int i = 2; i < args.length; i++) {
                if (args[i].startsWith(CBD)) {
                    overideCodeBaseDirectory = new String(args[i].substring(CBD.length()));
                } else if (args[i].startsWith(SFD)) {
                    overideSqlFileDirectory = new String(args[i].substring(SFD.length()));
                } else {
                    mrApplicationShell = new ApplicationShell(logName, args[i]);
                    try {
                        TextLog mrStatusLog = new TextLog(logName, PRODUCT_NAME);
                        mrStatusLog.setConsoleOutput(true);
                        mrStatusLog.setAutoFlush(true);
                        mrStatusLog.setAutoLog(true);
                        mrStatusLog.setConsoleOutput(true);
                        mrStatusLog.debugOn();
                        mrApplicationShell.setLog(mrStatusLog);
                        mrApplicationShell.info("Processing file " + args[i] + " in batch mode");
                        mrStatusLog.debugOff();

                        File testFile = new File(args[i]);

                        if (testFile.exists()) {
                            if (testFile.isDirectory()) {
                                mrApplicationShell.error("File " + args[i] + " is a directory");
                            } else {
                                if (testFile.canRead()) {
                                    mrApplicationShell.generateCodeFromIniFile(overideCodeBaseDirectory, overideSqlFileDirectory);
                                    mrApplicationShell.info("Finished processing file " + args[i]);
                                } else {
                                    mrApplicationShell.error("File " + args[i] + " exists but is not readable");
                                }
                            }
                        } else {
                            mrApplicationShell.error("File " + args[i] + " does not exist");
                        }

                        // reset overrides so they arent used by next file
                        overideCodeBaseDirectory = "";
                        overideSqlFileDirectory = "";
                        mrApplicationShell.disconnect();


                    } catch (Exception e) {
                        mrApplicationShell.error(e);
                    }

                } //else

            } //for


            System.exit(0);
        } else {
            // complain if we have the wrong number of param files

            if (args.length > 2) {
                System.err.println("Error: Too many parameters");
                System.exit(1);
            }

            mrApplicationShell = new ApplicationShell(args[0], args[1]);

            try {
                // 1.5 swing fix
                System.setProperty("swing.metalTheme", "steel");
            } catch (Exception e) {
                System.err.println(e.toString());
            }

            ThingAdministratorFrame frame = new ThingAdministratorFrame(mrApplicationShell);
            //Validate frames that have preset sizes
            //Pack frames that have useful preferred size info, e.g. from their layout
            if (packFrame)
                frame.pack();
            else
                frame.validate();
            //Center the window
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Dimension frameSize = frame.getSize();
            if (frameSize.height > screenSize.height)
                frameSize.height = screenSize.height;
            if (frameSize.width > screenSize.width)
                frameSize.width = screenSize.width;
            frame.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
            frame.setVisible(true);
        }

    }

    public ProcBuilder(McpDbWizardEventListener theListener, String logDir, String iniFile) {
        mrApplicationShell = new ApplicationShell(logDir, iniFile);

        mrApplicationShell.setShutdownOnExit(false);
        mrApplicationShell.addMcpDbWizardEventListener(theListener);
        try {
            // 1.5 swing fix
            System.setProperty("swing.metalTheme", "steel");
        } catch (Exception e) {
            System.err.println(e.toString());
        }

        ThingAdministratorFrame frame = new ThingAdministratorFrame(mrApplicationShell);
        //Validate frames that have preset sizes
        //Pack frames that have useful preferred size info, e.g. from their layout
        if (packFrame)
            frame.pack();
        else
            frame.validate();

        //Center the window
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = frame.getSize();
        if (frameSize.height > screenSize.height)
            frameSize.height = screenSize.height;
        if (frameSize.width > screenSize.width)
            frameSize.width = screenSize.width;
        frame.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
        frame.setVisible(true);
    }

    //Main method
    public static void main(String[] args) {
        new ProcBuilder(args);
    }

    void printUsageAndExit() {
        System.out.println("Usage: ");
        System.out.println("Interactive mode: " + PRODUCT_NAME + " log_file_directory configfile" + FILE_EXTENSION);
        System.out.println(" ");
        System.out.println("Batch Mode: " + PRODUCT_NAME + " log_file_directory build configfile" + FILE_EXTENSION);
        System.out.println("or          " + PRODUCT_NAME + " log_file_directory build " + CBD + "/create/code/here " + SFD + "/use/sql/files/here configfile_1" + FILE_EXTENSION);
        System.out.println("or          " + PRODUCT_NAME + " log_file_directory build configfile_1" + FILE_EXTENSION + " configfile_2" + FILE_EXTENSION);
        System.out.println(" ");
        System.out.println("Config files may be the classic " + FILE_EXTENSION + " (Java properties) format or the new .json format.");
        System.exit(0);
    }
}



