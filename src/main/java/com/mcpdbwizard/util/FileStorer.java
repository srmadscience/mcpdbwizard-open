package com.mcpdbwizard.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.Properties;

import com.mcpdbwizard.pub.*;

//import com.mcpdbwizard.util.files.GenericBatchTxt;
//import com.mcpdbwizard.util.files.GenericCompileTxt;
/**
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class FileStorer {

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

//		StoredFileInterface t = new GenericCompileTxt();
//		System.out.println(t.getDescr());
//		System.out.println(t.getOs());
//		System.out.println(t.toString());
//		Properties p = new Properties();
//		p.put("PARAM_CODEDIR", "FPP");
//
//		String[]lines = t.getContents(p);
//
//		for (int i=0; i < lines.length; i++) {
//			System.out.println(lines[i]);
//		}
//

        LogInterface theLog = new ConsoleLog();


        File inDir = new File(args[0]);

        if (!inDir.exists() || !inDir.isDirectory()) {
            theLog.error("file does nto exist");

            System.exit(1);
        }

        File outDir = new File(args[1]);
        outDir.mkdirs();


        String os = args[2];

        File[] fileList = inDir.listFiles();

        String spaceOrComma = " ";

        System.out.println("StoredFileInterface[] availableTemplates = { ");

        for (int i = 0; i < fileList.length; i++) {
            System.out.println(spaceOrComma + " new " + fileList[i].getName().replace(".", "_") + "()");


            try {
                FileInputStream in = new FileInputStream(fileList[i]);
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String strLine;
                String descr = br.readLine();
                File outFile = new File(outDir, fileList[i].getName().replace(".", "_") + ".java");

                FileWriter w = new FileWriter(outFile);
                BufferedWriter bw = new BufferedWriter(w);
                bw.write("package com.mcpdbwizard.util.files;");
                bw.newLine();

                bw.write("import com.mcpdbwizard.util.AbstractBaseStoredFile;");
                bw.newLine();
                bw.write("public class " + fileList[i].getName().replace(".", "_") + " extends AbstractBaseStoredFile {");
                bw.newLine();
                bw.write("public " + fileList[i].getName().replace(".", "_") + "() {");
                bw.newLine();
                bw.write("super();" + "\n");
                bw.newLine();
                bw.write("this.name = \"" + fileList[i].getName() + "\";");
                bw.newLine();
                bw.write("this.descr = \"" + descr + "\";");
                bw.newLine();
                bw.write("this.os = \"" + os + "\";");
                bw.newLine();

                bw.newLine();
                bw.write("String[] lines = { ");
                bw.write("");
                bw.newLine();
                spaceOrComma = " ";
                while ((strLine = br.readLine()) != null) {
                    bw.write(spaceOrComma + "\"" + strLine.replace("\\", "\\\\").replace("\"", "\\\"") + "\"");
                    bw.newLine();
                    spaceOrComma = ",";
                }

                bw.write("};");
                bw.write("" + "\n");
                bw.write("" + "\n");
                bw.newLine();
                bw.newLine();

                bw.newLine();
                bw.write("this.contents = lines;" + "\n");
                bw.write("}");
                bw.newLine();
                bw.write("}");
                bw.newLine();

                bw.flush();

            } catch (Exception e) {
                System.out.println(e);
            }


        }

        System.out.println("};");


    }

    /**
     * Turns a file into a Byte array. The file is deleted.
     *
     * @param theFile A file we wish to load into a Byte Array
     * @return A Byte array containing the contents of the file
     * @throws CSException
     */
    protected byte[] loadFileIntoByteArray(LogInterface theLog, java.io.File theFile) throws Exception {
        byte[] newArray = null;
        try {
            if (theFile != null && theFile.exists() && theFile.length() > 0) {
                if (theFile.length() > Integer.MAX_VALUE) {
                    throw new Exception("loadFileIntoByteArray: File "
                            + theFile.getAbsolutePath()
                            + " is too big to be turned into a Byte array");
                }

                theLog.debug("loadFileIntoByteArray: Starting to load "
                        + theFile.length()
                        + " from temp file " + theFile.getAbsolutePath() + " into byte array");

                newArray = new byte[(int) theFile.length()];
                java.io.BufferedInputStream theInputStream = new java.io.BufferedInputStream(new java.io.FileInputStream(theFile));
                theInputStream.read(newArray);
                theInputStream.close();
                theLog.debug("loadFileIntoByteArray: Finished loading byte array");
            } else {
                theLog.debug("loadFileIntoByteArray: Turning empty/null file into byte array");
            }

        } catch (java.io.IOException e) {
            throw new CSException("loadFileIntoByteArray:" + e.getMessage());
        }

        return (newArray);
    }

}
