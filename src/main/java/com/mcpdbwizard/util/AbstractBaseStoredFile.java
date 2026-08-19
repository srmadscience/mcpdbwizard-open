package com.mcpdbwizard.util;

import java.io.File;
import java.sql.Connection;
import java.util.Enumeration;
import java.util.Properties;

import com.mcpdbwizard.app.templates.TemplateWrangler;
import com.mcpdbwizard.pub.LogInterface;

/**
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public abstract class AbstractBaseStoredFile implements StoredFileInterface {

    protected String name = "";
    protected String descr = "";
    protected String os = "";
    protected String[] contents = {""};


    //@Override
    public String getDescr() {
        return descr;
    }

    //@Override
    public String[] getContents(Properties paramList, LogInterface theLog, Connection mrConnection) {

        String packageNameSlash = paramList.getProperty("PACKAGE_NAME", "fhksdddddddddddddddddddddddddddddddddddddddddddddddddddd"); // handle null package
        //System.out.println(packageNameSlash);
        packageNameSlash = packageNameSlash.replace(".", File.separator);
        //System.out.println(packageNameSlash);
        String jdbcJar = TemplateWrangler.getJdbcJarFile(theLog, mrConnection).getAbsolutePath();
        String pubJar = TemplateWrangler.getPubJarFile(theLog, mrConnection).getAbsolutePath();
        String sdoJar = TemplateWrangler.getSdoJarFile(theLog, mrConnection);
        String xmlJar = TemplateWrangler.getXmlJarFile(theLog, mrConnection);
        String javac = TemplateWrangler.findJavac(theLog);

        String[] newLines = new String[contents.length];

        for (int i = 0; i < newLines.length; i++) {
            newLines[i] = new String(contents[i]);

            newLines[i] = newLines[i].replace("PARAM_PACKAGE_NAME_SLASH", packageNameSlash);
            newLines[i] = newLines[i].replace("PARAM_JDBCJAR", jdbcJar);
            newLines[i] = newLines[i].replace("PARAM_PUBJAR", pubJar);
            newLines[i] = newLines[i].replace("PARAM_SDOJAR", sdoJar);
            newLines[i] = newLines[i].replace("PARAM_XMLJAR", xmlJar);
            newLines[i] = newLines[i].replace("PARAM_JAVAC", javac);


            Enumeration e = paramList.propertyNames();

            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                String match = paramList.getProperty(key);

                newLines[i] = newLines[i].replace("PARAM_" + key, match);

            }

        }

        return newLines;
    }


    //@Override
    public String getOs() {
        return os;
    }

    public String toString() {
        return name;
    }

}
