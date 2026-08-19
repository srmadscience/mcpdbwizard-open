package com.mcpdbwizard.app.procbuilder;

import com.mcpdbwizard.pub.Namer;

import com.mcpdbwizard.pub.TextLog;
//import com.mcpdbwizard.app.procbuilder.*;
import com.mcpdbwizard.app.common.*;

import java.util.Properties;
import java.io.*;

/**
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class PlsqlHint {

    static final String SQL = ".SQL";
    static final int SEQ_MISSING_LIMIT = 10;
    static final String XTRA_PROP_FILE = "xtra.dat";
    static final String XTRA_END_FILE = "extra.ind";

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: PlsqlHint directory_name pb2_file logdir");
            System.exit(1);
        }

        String targetDirName = args[0];
        String iniFileName = args[1];
        String LogDir = args[2];
        String aspDirName = "";

        TextLog theLog = new TextLog(LogDir, "PlsqHint");
        // Get Properties
        FileInputStream mrInputStream = null;
        Properties fileProps = new Properties();

        try {
            fileProps = new Properties();
            mrInputStream = new FileInputStream(iniFileName);
            fileProps.load(mrInputStream);
            mrInputStream.close();
            aspDirName = fileProps.getProperty("SQL_FILE_DIRECTORY", System.getProperty("user.home"));
        } catch (Exception e) {
            theLog.warning("No saved configuration exists");
        }

        String[] aspStatementFileNames = null;

        int validSql = 0;
        int unselectedSql = 0;
        int invalidSql = 0;
        int selectedSql = 0;

        try {
            File aspDir = new File(aspDirName);
            aspStatementFileNames = aspDir.list((FilenameFilter) new EndsWithFilter(".SQL", "Sql Files"));
            if (aspStatementFileNames.length == 0) {
                theLog.info(aspDirName + " does not contain any usable SQL files", false, true);
            }
        } catch (Exception e) {
            theLog.error("Directory " + aspDirName + " is not usable");
            aspStatementFileNames = new String[0];
        }

        String[] aspStatementErrors = new String[aspStatementFileNames.length];

        SqlStatementWrangler[] aspStatementWranglerArray = new SqlStatementWrangler[aspStatementFileNames.length];

        // Populate array of wranglers...
        for (int i = 0; i < aspStatementFileNames.length; i++) {
            aspStatementErrors[i] = "";

            File tempFile = new File(aspDirName, aspStatementFileNames[i]);

            if (tempFile.exists()
                    && tempFile.canRead()
                    && tempFile.isFile()
                    && tempFile.length() > 0) {
                // Read contents of file into a String
                try {
                    String tempStatement = JavaUtils.readFileIntoString(tempFile);

                    // Find out which property record this is for.
                    int tempPropNumber = -1;

                    boolean moreProperties = true;
                    int propertyCount = 0;
                    int missingCount = 0;
                    String testString = "";

                    while (moreProperties) {
                        testString = new String(SqlStatementWrangler.SQL_FILENAME + (propertyCount));

                        if (fileProps.containsKey(testString)) {
                            if (fileProps.getProperty(testString).equals(tempFile.getName())) {
                                tempPropNumber = propertyCount;
                                missingCount = 0;
                                break;
                            }
                        } else {
                            missingCount++;
                        }

                        if (missingCount > SEQ_MISSING_LIMIT) {
                            moreProperties = false;
                        }

                        propertyCount++;
                    } // while

                    aspStatementWranglerArray[i] = new SqlStatementWrangler(tempFile.getName(), tempStatement, fileProps, tempPropNumber, theLog);

                    if (aspStatementWranglerArray[i].getFileType() == SqlStatementWrangler.VALID_SQL_STATEMENT) {
                        validSql++;
                        unselectedSql++;
                    } else if (aspStatementWranglerArray[i].getFileType() == SqlStatementWrangler.GOOD_FILE_BAD_SQL_STATEMENT) {
                        invalidSql++;
                        aspStatementErrors[i] = "SQL Statement not usable:\n" + aspStatementWranglerArray[i].getRawSqlStatement();
                    } else if (aspStatementWranglerArray[i].getFileType() == SqlStatementWrangler.SELECTED_SQL_STATEMENT) {
                        selectedSql++;
                        validSql++;
                    }

                } catch (Exception e) {
                    theLog.warning("SQL file '" + tempFile.getAbsolutePath() + "' is not usable: " + e.getMessage());
                    aspStatementWranglerArray[i] = null;
                    aspStatementErrors[i] = "'" + tempFile.getAbsolutePath() + "' is not usable: " + e.getMessage();
                }
            } else {
                theLog.warning("SQL file " + tempFile.getAbsolutePath() + " is not usable");
                aspStatementWranglerArray[i] = null;

                // Try and find out why file is bad...
                if (!tempFile.exists()) {
                    aspStatementErrors[i] = "'" + tempFile + "' does not exist";
                } else if (tempFile.isDirectory()) {
                    aspStatementErrors[i] = "'" + tempFile + "' is a Directory";
                } else if (!tempFile.canRead()) {
                    aspStatementErrors[i] = "'" + tempFile + "' is not readable";
                } else if (tempFile.length() == 0) {
                    aspStatementErrors[i] = "'" + tempFile + "' is empty";
                }

            }
        }

        // Create extra props
        Properties xtraProps = new Properties();

        if (aspStatementWranglerArray != null) {
            for (int i = 0; i < aspStatementWranglerArray.length; i++) {
                if (aspStatementWranglerArray[i] != null) {
                    aspStatementWranglerArray[i].writeHintProperties(xtraProps, i);
                }
                xtraProps.setProperty(SqlStatementWrangler.SQL_BROKEN + i, aspStatementErrors[i]);
            }
        }


        try {
            File ExtraPropFile = new File(targetDirName + File.separator + XTRA_PROP_FILE);
            File EndFile = new File(targetDirName + File.separator + XTRA_END_FILE);
            FileOutputStream mrOutputStream;

            // write new props
            mrOutputStream = new FileOutputStream(ExtraPropFile);
            xtraProps.store(mrOutputStream, Namer.param_product_name + " " + Namer.param_product_version
                    + " Build " + Namer.param_build + " GUI Properties");
            mrOutputStream.close();

            // write marker file
            mrOutputStream = new FileOutputStream(EndFile);
            byte[] something = new byte[0];

            mrOutputStream.write(something);
            mrOutputStream.flush();
            mrOutputStream.close();


        } catch (Exception e) {
            theLog.error("While saving Properties:");
            theLog.error(e);
        }

        theLog.closeFile();
    }

}