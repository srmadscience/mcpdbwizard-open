package com.mcpdbwizard.app.templates;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Properties;

import com.mcpdbwizard.app.procbuilder.gui.ProcBuilder;
import com.mcpdbwizard.pub.ConsoleLog;
import com.mcpdbwizard.pub.LogInterface;
import com.mcpdbwizard.test.TestInterface;
import com.mcpdbwizard.util.StoredFileInterface;
import com.mcpdbwizard.util.files.*;

/**
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class TemplateWrangler implements TestInterface {

    LogInterface theLog = null;

    StoredFileInterface[] availableTemplates = {
            new GenericBatchTxt()
            , new GenericCompileTxt()
    };

    public TemplateWrangler(LogInterface theLog) {
        this.theLog = theLog;
    }

    // @SuppressWarnings("deprecation")

    // Used for testing
    public static void main(String[] args) {

        ConsoleLog l = new ConsoleLog();
        l.debugOn();
        TemplateWrangler t = new TemplateWrangler(l);
        l.info(t.getTestName());
        t.test(false);
    }

    public static String findJavac(LogInterface theLog) {

        File javaExecFile = new File(System.getProperties().getProperty(
                "java.home"));
        String javacExec = javaExecFile.getAbsolutePath();

        if (javaExecFile.getName().indexOf("jre") > -1) {
            // Now we have an issue. We need to get the most
            // advanced JSK in the parent directory.
            javaExecFile = javaExecFile.getParentFile();
            if (javaExecFile.getName().toLowerCase().indexOf("jdk") > -1) {
                javaExecFile = new File(javaExecFile.getAbsolutePath()
                        + File.separator + "bin" + File.separator + "javac");
            } else {
                File[] possibleJDKs = javaExecFile.listFiles();

                if (possibleJDKs != null && possibleJDKs.length > 0) {
                    boolean found = false;
                    theLog.info("Checking directory "
                            + javaExecFile.getAbsolutePath());
                    for (int i = 0; i < possibleJDKs.length; i++) {
                        theLog.info("Examining "
                                + possibleJDKs[i].getAbsolutePath());
                        if (possibleJDKs[i].getName().toLowerCase()
                                .indexOf("jdk") > -1) {
                            javaExecFile = new File(
                                    possibleJDKs[i].getAbsolutePath()
                                            + File.separator + "bin"
                                            + File.separator + "javac");
                            found = true;
                            break;
                        }

                    }

                    if (!found) {
                        theLog.warning("javac being defaulted to incorrect value of '"
                                + javaExecFile.getAbsolutePath()
                                + "'; Valid path needed.");
                    }

                } else {

                    javaExecFile = new File(javaExecFile.getAbsolutePath()
                            + "bin" + File.separator + "javac");
                    theLog.warning("javac being defaulted to wrong value of '"
                            + javaExecFile.getAbsolutePath()
                            + "'; Valid path needed.");
                }

                // javaExecFile = new
                // File(javaExecFile.getParentFile().getAbsolutePath()
                // + File.separator + "bin" + File.separator +
                // "javac");
            }
        } else {
            javaExecFile = new File(javaExecFile.getAbsolutePath()
                    + File.separator + "bin" + File.separator + "javac");
        }
        javacExec = javaExecFile.getAbsolutePath();
        theLog.info("Selected '" + javacExec + "' as javac executable");
        return javacExec;
    }

    public static File getJdbcJarFile(LogInterface theLog,
                                      Connection mrConnection) {

        File jdbcJarFile = null;
        try {
            jdbcJarFile = new File(mrConnection.getClass()
                    .getProtectionDomain().getCodeSource().getLocation()
                    .toURI());
        } catch (URISyntaxException e) {
            theLog.error(e);
            theLog.error("JDBC Jar file defaulted to jdbc.jar");
            jdbcJarFile = new File("jdbc.jar");
        }

        if (jdbcJarFile != null) {
            jdbcJarFile.getAbsolutePath();
        }
        theLog.info("Selected '" + jdbcJarFile.getAbsolutePath()
                + "' as JDBC driver");

        return jdbcJarFile;

    }

    public static File getPubJarFile(LogInterface theLog,
                                     Connection mrConnection) {

        File pubJarFile = null;
        try {
            pubJarFile = new File(ConsoleLog.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            theLog.error(e);
            theLog.error("JDBC Jar file defaulted to jdbc.jar");
            pubJarFile = new File("jdbc.jar");
        }

        if (pubJarFile != null) {
            pubJarFile.getAbsolutePath();
        }
        theLog.info("Selected '" + pubJarFile.getAbsolutePath()
                + "' as com.mcpdbwizard.pub JAR file");

        return pubJarFile;

    }

    public static String getSdoJarFile(LogInterface theLog,
                                       Connection mrConnection) {
        String otherJarPath = "";
        File jdbcJarFile = getJdbcJarFile(theLog, mrConnection);

        try {

            if (jdbcJarFile != null && jdbcJarFile.exists()) {
                File sdoJar = new File(jdbcJarFile.getParentFile()
                        .getParentFile()
                        + File.separator
                        + StoredFileInterface.SDO_JARS);
                if (sdoJar.exists()) {
                    otherJarPath = sdoJar.getAbsolutePath();
                } else {
                    theLog.warning("SDO Jar file path being defaulted to wrong value of '"
                            + StoredFileInterface.SDO_JARS
                            + "'; Full path needed.");
                    otherJarPath = StoredFileInterface.SDO_JARS;
                }

            }
        } catch (Exception e) {
            theLog.error("Attempt to find SDO jar file failed:");
            theLog.error(e);
        }
        return otherJarPath;
    }

    public static String getXmlJarFile(LogInterface theLog,
                                       Connection mrConnection) {
        String otherJarPath = "";
        File jdbcJarFile = getJdbcJarFile(theLog, mrConnection);

        try {

            if (jdbcJarFile != null && jdbcJarFile.exists()) {
                File XmlJar = new File(jdbcJarFile.getParentFile()
                        .getParentFile().getAbsolutePath());
                String spaceOrSemi = "";
                for (int i = 0; i < StoredFileInterface.XDB_JARS.length; i++) {
                    otherJarPath = otherJarPath + spaceOrSemi + XmlJar.getAbsolutePath()
                            + File.separator
                            + StoredFileInterface.XDB_JARS[i];
                    spaceOrSemi = ";";
                }
//				if (sdoJar.exists()) {
//					otherJarPath = sdoJar.getAbsolutePath();
//				} else {
//					theLog.warning("SDO Jar file path being defaulted to wrong value of '"
//							+ StoredFileInterface.SDO_JARS
//							+ "'; Full path needed.");
//					otherJarPath = StoredFileInterface.SDO_JARS;
//				}

            }
        } catch (Exception e) {
            theLog.error("Attempt to find XML jar file failed:");
            theLog.error(e);
        }
        return otherJarPath;
    }

    public String[] getAllTemplates(String envDetails) {

        String[] theTemplates = new String[availableTemplates.length];

        for (int i = 0; i < availableTemplates.length; i++) {
            theTemplates[i] = availableTemplates[i].toString();
        }

        return theTemplates;
    }

    public String[] getTemplate(String templateName, Properties p,
                                LogInterface theLog, Connection theConnection) {

        String[] theTemplate = {};

        theLog.info("Getting file " + templateName);

        for (int i = 0; i < availableTemplates.length; i++) {
            if (templateName.equals(availableTemplates[i].toString())) {
                theTemplate = availableTemplates[i].getContents(p, theLog,
                        theConnection);
                break;
            }
        }

        return theTemplate;
    }

    public String getDescr(String templateName) {

        String theDescr = "";

        // theLog.info("Getting file " + templateName );

        for (int i = 0; i < availableTemplates.length; i++) {
            if (templateName.equals(availableTemplates[i].toString())) {
                theDescr = availableTemplates[i].getDescr();
                break;
            }
        }

        return theDescr;
    }

    public String getOs(String templateName) {

        String theOs = "";

        // theLog.info("Getting file " + templateName );

        for (int i = 0; i < availableTemplates.length; i++) {
            if (templateName.equals(availableTemplates[i].toString())) {
                theOs = availableTemplates[i].getOs();
                break;
            }
        }

        return theOs;
    }

    public String getTestName() {
        return "TemplateWrangler";
    }

    // @Override
    public boolean test(boolean isInteractive) {
        boolean torf = true;

        theLog.info("Test start");

        String[] result = {};

        try {
            theLog.info("Test get template file list");

            for (int i = 0; i < 10; i++) {
                result = getAllTemplates("");
                theLog.info("l=" + result.length);
            }

            if (result == null) {
                theLog.error("result is null");
                torf = false;
            }

            if (result.length < 2) {
                theLog.error("result is <2");
                torf = false;
            }

            for (int i = 0; i < result.length; i++) {
                theLog.info(result[i]);
            }
        } catch (Exception e) {
            theLog.error(e);
            torf = false;
        }

        String[] resultFile = new String[result.length];

        try {
            theLog.info("Get file names");
            for (int i = 0; i < result.length; i++) {
                resultFile[i] = result[i];
            }
            theLog.info("Get file names ... end ");
        } catch (Exception e) {
            theLog.error(e);
            torf = false;
        }

        try {
            theLog.info("Test files mentioned in template file list");

            if (resultFile.length < 2) {
                theLog.error("result is <2");
                torf = false;
            }

            for (int i = 0; i < resultFile.length; i++) {
                theLog.info(resultFile[i]);
            }
        } catch (Exception e) {
            theLog.error(e);
            torf = false;
        }

        // try {
        // theLog.info("Download each file for current OS");
        // Properties p = new Properties();
        //
        // for (int i = 0; i < resultFile.length; i++) {
        // theLog.info(resultFile[i]);
        // String[] thisFile = getTemplate(resultFile[i], p);
        //
        // for (int j = 0; j < thisFile.length; j++) {
        // theLog.info(thisFile[j]);
        // }
        //
        // }
        // } catch (Exception e) {
        // theLog.error(e);
        // torf = false;
        // }
        theLog.info("Test end");
        return torf;
    }
}
