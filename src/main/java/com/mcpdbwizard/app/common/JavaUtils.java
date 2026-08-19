package com.mcpdbwizard.app.common;

import com.mcpdbwizard.pub.*;

import java.io.*;

//import java.util.ArrayList;

/**
 * @author devteam@mcpdbwizard.com
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 * @version 2
 */
public class JavaUtils {
    static final char[] evil_characters = // Space
            // quote
            // slash
            // backslash
            // semicolon
            // Asterisk
            {' ', '\'', '/', '\\', ';', '*'};

    static final String procBuilderExtension = "pb2";

    static final char[] legal_java_identifier_characters = /* ,'$'*/
            {'_', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
                    'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a',
                    'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o',
                    'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '1', '2', '3',
                    '4', '5', '6', '7', '8', '9', '0'};

    static final char replacement_character = '_';

    public JavaUtils() {
    }

    public static String getJavaName(String pString) {


        char[] newName = pString.toCharArray();

        // Remove characters that are not parts of legal java identifiers...
        for (int i = 0; i < newName.length; i++) {
            boolean foundChar = false;
            for (int j = 0; j < legal_java_identifier_characters.length; j++) {

                if (newName[i] == legal_java_identifier_characters[j]) {
                    foundChar = true;
                    break;
                }
            }

            if (!foundChar) {
                newName[i] = replacement_character;
            }
        }

        // if whats left begins with a number prepend it....
        String newNameString = new String(newName);

        if (newName[0] >= '0' && newName[0] <= '9') // SBUG 001
        {
            newNameString = new String(replacement_character + newNameString);
        }

        return (newNameString);
    }

    public static String getJavaName(String oldName,
                                     String javaNamingConvention,
                                     LogInterface theLog) {
        return (getJavaName(oldName, javaNamingConvention, theLog, false));
    }

    public static String getJavaName(String oldName,
                                     String javaNamingConvention,
                                     LogInterface theLog,
                                     boolean isAClassName) {
        String newName = new String(getJavaName(oldName));

        // replace all evil_characters with _


        if (javaNamingConvention.equals("InitialCapitalLetters.java")) {
            boolean underscore_seen = false;
            char[] tempCharArray = newName.toCharArray();
            String tempString = "";

            if (isAClassName && tempCharArray.length > 0 &&
                    tempCharArray[0] >= 'a' && tempCharArray[0] <= 'z') {
                tempCharArray[0] = Character.toUpperCase(tempCharArray[0]);
            }

            for (int j = 0; j < tempCharArray.length; j++) {
                if (tempCharArray[j] == '_') {
                    underscore_seen = true;
                } else {
                    if (underscore_seen) {
                        underscore_seen = false;
                        String newCharacter =
                                new String(tempCharArray[j] + "");
                        tempString =
                                new String(tempString + newCharacter.toUpperCase());
                    } else {
                        tempString = new String(tempString + tempCharArray[j]);
                    }
                }
            }
            if (tempCharArray[tempCharArray.length - 1] == '_') {
                tempString = new String(tempString + '_');
            }


            newName = new String(tempString);

            if (newName.length() == 0) {
                newName = "Nothing";
            }

            char[] newNameChar = newName.toCharArray();

            if (isAClassName && newNameChar[0] >= '0' &&
                    newNameChar[0] <= '9') // SBUG 001
            {
                newName = new String("A" + newName);
            }


        } else if (javaNamingConvention.equals("JavaStandard.java")) {
            //String newName = oldName.toLowerCase();
            boolean underscore_seen = false;
            char[] tempCharArray = newName.toCharArray();
            String tempString = "";

            if (isAClassName && tempCharArray.length > 0 &&
                    tempCharArray[0] >= 'a' && tempCharArray[0] <= 'z') {
                tempCharArray[0] = Character.toUpperCase(tempCharArray[0]);
            }

            for (int j = 0; j < tempCharArray.length; j++) {
                if (tempCharArray[j] == '_') {
                    underscore_seen = true;
                } else {
                    if (underscore_seen) {
                        underscore_seen = false;
                        String newCharacter =
                                new String(tempCharArray[j] + "");
                        tempString =
                                new String(tempString + newCharacter.toUpperCase());
                    } else {
                        if (isAClassName && j == 0) {
                            String newCharacter =
                                    new String(tempCharArray[j] + "");
                            tempString =
                                    new String(newCharacter.toUpperCase());
                        } else {
                            tempString =
                                    new String(tempString + tempCharArray[j]);
                        }
                    }
                }

            }
            if (tempCharArray[tempCharArray.length - 1] == '_') {
                tempString = new String(tempString + '_');
            }
            newName = new String(tempString);


            if (newName.length() == 0) {
                newName = "Nothing";
            }

            char[] newNameChar = newName.toCharArray();

            if (newNameChar[0] >= '0' && newNameChar[0] <= '9') // SBUG 001
            {
                newName = new String("A" + newName);
            }


        } else if (javaNamingConvention.equals("spaces_between_words.java")) {
            newName = newName.toLowerCase();

            char[] tempCharArray = newName.toCharArray();
            //String tempString = "";
            if (isAClassName && tempCharArray.length > 0 &&
                    tempCharArray[0] >= 'a' && tempCharArray[0] <= 'z') {
                tempCharArray[0] = Character.toUpperCase(tempCharArray[0]);
                newName = new String(tempCharArray);
            }

        } else {
            theLog.syserror("Unknown java naming convention of " +
                    javaNamingConvention);
        }
        // Search for and fix duplicates...
        return (newName);
    }

    /**
     * public static String[] getJavaNames(String[] oldNames, String javaNamingConvention, LogInterface theLog)
     * {
     * return(getJavaNames(oldNames, javaNamingConvention, theLog, false));
     * }
     * <p>
     * public static String[] getJavaNames(String[] oldNames
     * , String javaNamingConvention
     * , LogInterface theLog
     * , boolean isAClassName)
     * {
     * String[] newNames = new String[oldNames.length];
     * <p>
     * for (int i=0; i < newNames.length; i++)
     * {
     * newNames[i] = getJavaName(oldNames[i], javaNamingConvention, theLog, isAClassName);
     * }
     * <p>
     * return(newNames);
     * }
     **/
    public static String osFilenameSafe(String pString) {
        String outputString = new String(pString);

        for (int i = 0; i < evil_characters.length; i++) {
            outputString =
                    outputString.replace(evil_characters[i], replacement_character);
        }
        return (outputString);
    }

    public static String dbSafe(String pString) {
        String tempString = pString;

        // Map " to ' to stop SAX breaking.
        tempString = replaceString(tempString, "\"", "'");

        // Map ' to '' to stop non-bound (no params) oracle statements from breaking
        tempString = replaceString(pString, "'", "''");

        // Map '+AMPERSAND+' to '&' to stop SAX from breaking.
        tempString = replaceString(tempString, "+AMPERSAND+", "&");

        return (tempString);
    }

    public static String removeDupSpaces(String pString) {

        String newString = new String(pString);


        int sLength = newString.length();
        int pLength = Integer.MAX_VALUE;

        while (sLength < pLength) {
            pLength = sLength;
            newString = new String(replaceString(newString, "  ", " "));
            sLength = newString.length();
        }

        if (newString.startsWith(" ") && newString.length() > 1) {
            newString = new String(newString.substring(1));
        }

        return (newString.trim());


    }

    public static String removeComments(String pString) {

        String newString = new String(pString);


        int sLength = newString.length();
        int pLength = Integer.MAX_VALUE;

        while (sLength < pLength) {
            pLength = sLength;

            int startComment = newString.indexOf("/*");
            int endComment = newString.indexOf("*/");

            if (startComment >= 0 && endComment >= 0 && startComment < endComment) {
                String start = newString.substring(0, startComment);
                String end = newString.substring(endComment + 2);
                newString = new String(start + end);
            }

            sLength = newString.length();
        }

        if (newString.startsWith(" ") && newString.length() > 1) {
            newString = new String(newString.substring(1));
        }

        return (newString.trim());


    }

    public static String replaceString(String pString, String oldValue,
                                       String newValue) {
        int howFar = 0;
        int startOfOld = 0;
        String tempString = "";

        String frontHalf = "";
        String backHalf = "";

        if (pString != null) {
            //
            // For each occurance of oldString Replace it with newString...
            //
            howFar = 0;
            tempString = new String(pString);

            while (tempString.indexOf(oldValue, howFar) > -1) {
                startOfOld = tempString.indexOf(oldValue, howFar);

                frontHalf = tempString.substring(0, startOfOld);
                backHalf =
                        tempString.substring(startOfOld + oldValue.length());

                tempString = new String(frontHalf + newValue + backHalf);

                howFar = startOfOld + newValue.length();

            }
        }
        return (tempString);
    }

    public static String replaceFirstString(String pString, String oldValue,
                                            String newValue) {
        int howFar = 0;
        int startOfOld = 0;
        String tempString = "";

        String frontHalf = "";
        String backHalf = "";

        if (pString != null) {
            //
            // For each occurance of oldString Replace it with newString...
            //
            howFar = 0;
            tempString = new String(pString);

            if (tempString.indexOf(oldValue, howFar) > -1) {
                startOfOld = tempString.indexOf(oldValue, howFar);

                frontHalf = tempString.substring(0, startOfOld);
                backHalf =
                        tempString.substring(startOfOld + oldValue.length());

                tempString = new String(frontHalf + newValue + backHalf);

                howFar = startOfOld + newValue.length();

            }
        }
        return (tempString);
    }

    /**
     *
     */
    public static String getOracleName(String theUser, String theName,
                                       String objectType, String packageName,
                                       boolean loginNamespace) {
        String newName = new String(theName);

        if (packageName != null) {
            newName = packageName + "." + newName;
        }

        if (loginNamespace) {
            if (objectType.equals("Other User's Object")) {
                newName = new String(theUser + "." + formatName(newName));
            }
        } else {
            if (!objectType.equals("User's Object")) {
                newName = new String(theUser + "." + formatName(newName));
            }
        }


        // escape " so java remains legal...
        return (replaceString(formatName(newName), "\"", "\\\""));
    }

    /**
     * Make an identifier "legal" in the eyes of Oracle if it is either a reserved word or
     * contains an embedded space.
     *
     * @param theName A candidate oracle identifer - EMP, THE EMPLOYEES, FROM
     * @return A 'fixed' oracle identifer - EMP, "THE EMPLOYEES", "FROM"
     */
    public static String formatName(String theName) {
        // If we find an embedded space then we are dealing with the 0.0001% of oracle
        // DBs where there are embedded spaces in the name.
        boolean needsQuotes = false;

        for (int i = 0; i < evil_characters.length; i++) {
            if (theName.indexOf(evil_characters[i]) > -1) {
                needsQuotes = true;
                break;
            }
        }

        if (needsQuotes) {
            return ("\"" + theName + "\"");
        }
        return (theName);
    }

    static int findValueInStringArray(String[] theArray, String theValue) {
        int position = -1;
        for (int i = 0; i < theArray.length; i++) {
            if (theArray[i].equals(theValue)) {
                position = i;
                break;
            }
        }
        return (position);
    }

    public static String mapGuiNamesToCodeNames(String guiName) {
        String codeName = new String(guiName);

        if (guiName.equals("User Object")) {
            codeName = "THIS_USERS_OBJECT";
        } else if (guiName.equals("Other User's Object")) {
            codeName = "OTHER_USERS_OBJECT";
        } else if (guiName.equals("Private Synonym")) {
            codeName = "THIS_USERS_SYNONYM";
        } else if (guiName.equals("Public Synonym")) {
            codeName = "PUBLIC_SYNONYM";
        }
        return (codeName);

    }

    public static String mathValueMethod(String dataType) {
        String newCast = dataType.toLowerCase();

        if (dataType.equals("Integer")) {
            newCast = "int";
        }

        return (newCast + "Value");
    }

    public static String noDotSql(String sqlFileName) {
        // Remove .sql from filename...
        final char[] sArray = {'S', 's'};
        final char[] qArray = {'Q', 'q'};
        final char[] lArray = {'L', 'l'};

        for (int s = 0; s < sArray.length; s++) {
            for (int q = 0; q < qArray.length; q++) {
                for (int l = 0; l < lArray.length; l++) {
                    sqlFileName =
                            JavaUtils.replaceString(sqlFileName, "." + sArray[s] +
                                    qArray[q] + lArray[l], "");
                }
            }
        }
        return (sqlFileName);
    }

    /**
     * Reads a file into a String
     *
     * @param File theFile the file you want read
     */
    public static String readFileIntoString(File theFile) throws CSException {
        byte[] buff = new byte[com.mcpdbwizard.pub.IOUtils.IO_BUFFER_SIZE];
        int bytesRead;
        String aString = "";

        try {
            BufferedInputStream source =
                    new BufferedInputStream(new FileInputStream(theFile),
                            com.mcpdbwizard.pub.IOUtils.IO_BUFFER_SIZE);
            while (true) {
                bytesRead = source.read(buff);

                if (bytesRead == -1) {
                    break;
                }

                aString =
                        new String(aString + (new String(buff, 0, bytesRead)));
            }

            source.close();
            aString = aString.trim();
        } catch (IOException error) {
            throw new CSException(error.getMessage());
        }

        return (aString);
    }

    public static String oracle2JavaDatatype(String oracleDataType) {
        int oracleUnderlyingDatatype =
                SqlUtils.getUnderlyingOracleDatatype(oracleDataType);

        String javaDataType = "Object";

        // create variable definition...
        switch (oracleUnderlyingDatatype) {
            case SqlUtils.ORACLE_TEXT_DATATYPE: {
                javaDataType = "String";
                break;
            }
            case SqlUtils.ORACLE_NUMBER_DATATYPE: {
                javaDataType = "java.math.BigDecimal";
                break;
            }
            case SqlUtils.ORACLE_DATE_DATATYPE: {
                javaDataType = "java.util.Date";
                break;
            }
            case SqlUtils.ORACLE_LONGTEXT_DATATYPE:
            case SqlUtils.ORACLE_LONG_BINARY_DATATYPE:
            case SqlUtils.ORACLE_CLOB_DATATYPE:
            case SqlUtils.ORACLE_BLOB_DATATYPE:
            case SqlUtils.ORACLE_BFILE_DATATYPE:
            case SqlUtils.ORACLE_XMLTYPE_DATATYPE: {
                javaDataType = "java.io.File";
                break;
            }
            case SqlUtils.ORACLE_NULL_DATATYPE: {
                javaDataType = "Object";
                break;
            }
            case SqlUtils.ORACLE_BOOLEAN_DATATYPE: {
                javaDataType = "Boolean";
                break;
            }
            case SqlUtils.ORACLE_NATIVE_BOOLEAN_DATATYPE: {
                // Native ISO-SQL BOOLEAN (23ai).
                javaDataType = "Boolean";
                break;
            }
            case SqlUtils.ORACLE_JSON_DATATYPE: {
                // Native binary (OSON) JSON (21c+).
                javaDataType = "oracle.sql.json.OracleJsonValue";
                break;
            }
            case SqlUtils.ORACLE_VECTOR_DATATYPE: {
                // Native VECTOR (23ai), read/written as one double per dimension.
                javaDataType = "double[]";
                break;
            }
            case SqlUtils.ORACLE_VECTOR_BINARY_DATATYPE: {
                // Binary (bit-packed) VECTOR (23ai): read/written as byte[] (n bits = n/8 bytes).
                javaDataType = "byte[]";
                break;
            }
            case SqlUtils.ORACLE_VECTOR_SPARSE_DATATYPE: {
                // Sparse VECTOR (23ai): read/written as a com.mcpdbwizard.pub.SparseVector
                // {length, indices, values} — not densified. Needs ojdbc 23.26+ at runtime.
                javaDataType = "com.mcpdbwizard.pub.SparseVector";
                break;
            }
            case SqlUtils.ORACLE_ROWID_DATATYPE: {
                javaDataType = "oracle.sql.ROWID";
                break;
            }
            case SqlUtils.ORACLE_UROWID_DATATYPE: {
                javaDataType = "String";
                break;
            }
            case SqlUtils.ORACLE_BINARY_DATATYPE: {
                javaDataType = "byte[]";
                break;
            }
            case SqlUtils.ORACLE_TIMESTAMP_DATATYPE: {
                javaDataType = "java.sql.Timestamp";
                break;
            }
            case SqlUtils.ORACLE_TIMESTAMPTZ_DATATYPE: {
                javaDataType = "oracle.sql.TIMESTAMPTZ";
                break;
            }
            case SqlUtils.ORACLE_TIMESTAMPLTZ_DATATYPE: {
                javaDataType = "oracle.sql.TIMESTAMPLTZ";
                break;
            }
            case SqlUtils.ORACLE_INTERVAL_DAY_TO_SECOND_DATATYPE: {
                javaDataType = "oracle.sql.INTERVALDS";
                break;
            }
            case SqlUtils.ORACLE_INTERVAL_YEAR_TO_MONTH_DATATYPE: {
                javaDataType = "oracle.sql.INTERVALYM";
                break;
            }
            case SqlUtils.MCPDBWIZARD_READONLYROWSET: {
                javaDataType = "com.mcpdbwizard.pub.ReadOnlyRowSet";
                break;
            }
            case SqlUtils.ORACLE_PLSQL_INDEXBY_DATATYPE: {
                javaDataType = "com.mcpdbwizard.pub.PlsqlIndexByTable2";
                break;
            }
            case SqlUtils.ORACLE_SDO_GEOMETRY_DATATYPE: {
                //javaDataType = "oracle.spatial.geometry.JGeometry";
                javaDataType = "JGeometryWrapper";
                break;
            }
            case SqlUtils.ORACLE_VARRAY_DATATYPE:
            case SqlUtils.ORACLE_TABLE_DATATYPE:
            case SqlUtils.ORACLE_PLSQL_INDEXBY_ROWTYPE_DATATYPE: {
                javaDataType = "Object";
                break;
            }
            case SqlUtils.ORACLE_OBJECT_DATATYPE:
            case SqlUtils.ORACLE_ROWTYPE_DATATYPE:
            default: {
                javaDataType = "Object";
                break;
            }
        }
        return (javaDataType);
    }

    public static String StripLeadingUsername(String targetString,
                                              String unwantedUsername) {

        if (targetString.startsWith(unwantedUsername.toUpperCase() + ".")) {
            return (targetString.substring(unwantedUsername.length() + 1));
        }

        return (targetString);
    }

    /**
     * Return an <tt>int</tt> that represents the underlying oracle data type.
     * This method takes an oracle data type and classifies it as Text, Number, Date
     * Long Text or Other.
     *
     * @param String An oracle data type
     * @return int A SqlUtils constant that represents the underlying oracle data type.
     */
    private static Object mapOracleDatatypeToAlphaCharOrLength(String theColumnDataType, int length, boolean wantLength) {

        /**

         A ROWID
         B BLOB
         C CLOB
         D Date
         E LONG RAW
         F BFILE
         G IYM
         H TZ
         I Raw(2048)
         J Raw(1024)
         K Raw(512)
         L Long
         M Raw(256)
         N Number
         O Raw(128)
         P Raw(64)
         Q TLTZ
         R Raw(4000)
         S IYS
         T Timestamp
         U UROWID

         V v2 1024
         W v2 2048
         X Varchar2 etc 4000
         Y Raw(16)
         Z v2 Raw(32)

         0 V2(1)
         1 V2 2
         2 V2 4
         3 V2 8
         4 v2 16
         5 V2 32
         6 V2 64
         7 V2 128
         8 V2 256
         9 v2 512

         _ other
         **/

        // This will be used for all unmatched types...
        String returnCode = "_";

        Integer returnLength = (length);

        if (theColumnDataType.equals("VARCHAR2")
                || theColumnDataType.equals("VARCHAR")
                || theColumnDataType.equals("CHAR")
                || theColumnDataType.equals("CHARACTER")
                || theColumnDataType.equals("STRING")
        ) {
            returnCode = "X";

            if (returnLength == null) {
                returnLength = Integer.valueOf(4000);
            } else if (returnLength.intValue() <= 1) {
                returnLength = Integer.valueOf(1);
                returnCode = "0";
            } else if (returnLength.intValue() <= 2) {
                returnLength = Integer.valueOf(2);
                returnCode = "1";
            } else if (returnLength.intValue() <= 4) {
                returnLength = Integer.valueOf(4);
                returnCode = "2";
            } else if (returnLength.intValue() <= 8) {
                returnLength = Integer.valueOf(8);
                returnCode = "3";
            } else if (returnLength.intValue() <= 16) {
                returnLength = Integer.valueOf(16);
                returnCode = "4";
            } else if (returnLength.intValue() <= 32) {
                returnLength = Integer.valueOf(32);
                returnCode = "5";
            } else if (returnLength.intValue() <= 64) {
                returnLength = Integer.valueOf(64);
                returnCode = "6";
            } else if (returnLength.intValue() <= 128) {
                returnLength = Integer.valueOf(128);
                returnCode = "7";
            } else if (returnLength.intValue() <= 256) {
                returnLength = Integer.valueOf(256);
                returnCode = "8";
            } else if (returnLength.intValue() <= 512) {
                returnLength = Integer.valueOf(512);
                returnCode = "9";
            } else if (returnLength.intValue() <= 1024) {
                returnLength = Integer.valueOf(1024);
                returnCode = "V";
            } else if (returnLength.intValue() <= 2048) {
                returnLength = Integer.valueOf(2048);
                returnCode = "W";
            } else {
                returnLength = Integer.valueOf(4000);
                returnCode = "X";
            }


        } else if (theColumnDataType.equals("ROWID")) {
            returnCode = "A";
        } else if (theColumnDataType.equals("UROWID")) {
            returnCode = "U";
        } else if (theColumnDataType.equals("DATE")) {
            returnCode = "D";
        } else if (theColumnDataType.equals("NUMBER")
                || theColumnDataType.equals("FLOAT")
                || theColumnDataType.equals("NATURAL")
                || theColumnDataType.equals("NATURALN")
                || theColumnDataType.equals("POSITIVE")
                || theColumnDataType.equals("POSITIVEN")
                || theColumnDataType.equals("SIGNTYPE")
                || theColumnDataType.equals("BINARY_INTEGER")
                || theColumnDataType.equals("DEC")
                || theColumnDataType.equals("DECIMAL")
                || theColumnDataType.equals("DOUBLE PRECISION")
                || theColumnDataType.equals("FLOAT")
                || theColumnDataType.equals("INTEGER")
                || theColumnDataType.equals("INT")
                || theColumnDataType.equals("NUMERIC")
                || theColumnDataType.equals("REAL")
                || theColumnDataType.equals("SMALLINT")
                || theColumnDataType.equals("PLS_INTEGER")
        ) {
            returnCode = "N";
        } else if (theColumnDataType.equals("LONG")) {
            returnCode = "L";
        } else if (theColumnDataType.equals("CLOB")) {
            returnCode = "C";
        } else if (theColumnDataType.equals("LONG RAW")) {
            returnCode = "E";
        } else if (theColumnDataType.equals("BLOB")) {
            returnCode = "B";
        } else if (theColumnDataType.equals("BFILE")) {
            returnCode = "F";
        } else if (theColumnDataType.equals("RAW")) {
            returnCode = "R";

            if (returnLength == null) {
                returnLength = Integer.valueOf(4000);
            }
            /**
             else if (returnLength.intValue() <= 1)
             {
             returnLength = new Integer(1);
             returnCode = "0";
             }
             else if (returnLength.intValue() <= 2)
             {
             returnLength = new Integer(2);
             returnCode = "7";
             }
             else if (returnLength.intValue() <= 4)
             {
             returnLength = new Integer(4);
             returnCode = "6";
             }
             else if (returnLength.intValue() <= 8)
             {
             returnLength = new Integer(8);
             returnCode = "5";
             }
             **/
            else if (returnLength.intValue() <= 16) {
                returnLength = Integer.valueOf(16);
                returnCode = "Y";
            } else if (returnLength.intValue() <= 32) {
                returnLength = Integer.valueOf(32);
                returnCode = "Z";
            } else if (returnLength.intValue() <= 64) {
                returnLength = Integer.valueOf(64);
                returnCode = "P";
            } else if (returnLength.intValue() <= 128) {
                returnLength = Integer.valueOf(128);
                returnCode = "O";
            } else if (returnLength.intValue() <= 256) {
                returnLength = Integer.valueOf(256);
                returnCode = "M";
            } else if (returnLength.intValue() <= 512) {
                returnLength = Integer.valueOf(512);
                returnCode = "K";
            } else if (returnLength.intValue() <= 1024) {
                returnLength = Integer.valueOf(1024);
                returnCode = "J";
            } else if (returnLength.intValue() <= 2048) {
                returnLength = Integer.valueOf(2048);
                returnCode = "I";
            }


        } else if (theColumnDataType.equals("INTERVAL YEAR TO MONTH")
                || theColumnDataType.equals("INTERVALYM")
                || (theColumnDataType.startsWith("INTERVAL YEAR")
                && theColumnDataType.endsWith("TO MONTH"))) {
            returnCode = "G";
        } else if (theColumnDataType.equals("INTERVAL DAY TO SECOND")
                || theColumnDataType.equals("INTERVALDS")
                || (theColumnDataType.startsWith("INTERVAL DAY")
                && theColumnDataType.indexOf("TO SECOND") > -1))
        {
            returnCode = "S";
        }
        else if (theColumnDataType.equals("TIMESTAMP WITH LOCAL TIME ZONE") // Datatype according to ALL_SOURCE
                || theColumnDataType.equals("TIMESTAMPLTZ") // Datatype according to ResultSet
                || (theColumnDataType.startsWith("TIMESTAMP") // Allow for "TIMESTAMP(6) WITH LOCAL TIME ZONE"
                && theColumnDataType.endsWith("LOCAL TIME ZONE"))) {
            returnCode = "Q";
        } else if (theColumnDataType.equals("TIMESTAMP WITH TIME ZONE")  // Datatype according to ALL_SOURCE
                || theColumnDataType.equals("TIMESTAMPTZ")   // Datatype according to ResultSet
                || (theColumnDataType.startsWith("TIMESTAMP") // Allow for "TIMESTAMP(6) WITH LOCAL TIME ZONE"
                && theColumnDataType.endsWith("TIME ZONE"))) {
            returnCode = "H";
        } else if (theColumnDataType.startsWith("TIMESTAMP")) {
            // Possible issue with Precision here...
            returnCode = "T";
        }

        if (wantLength) {
            return (returnLength);
        }

        return (returnCode);
    }

    /**
     *
     */
    public static String mapOracleDatatypeToAlphaChar(String theColumnDataType, int length) {

        String returnCode = (String) mapOracleDatatypeToAlphaCharOrLength(theColumnDataType, length, false);
        return (returnCode);
    }

    /**
     *
     */
    public static int mapOracleDatatypeToLength(String theColumnDataType, int length) {

        Integer returnCode = (Integer) mapOracleDatatypeToAlphaCharOrLength(theColumnDataType, length, true);
        return (returnCode.intValue());
    }


    public static String mapScalarTypeToObjectType(String theType) {
        // { "double", "float",  "long", "int" };
        if (theType.equals("double")) return ("Double");

        if (theType.equals("float")) return ("Float");

        if (theType.equals("long")) return ("Long");

        if (theType.equals("int")) return ("Integer");

        return (theType);
    }


}


