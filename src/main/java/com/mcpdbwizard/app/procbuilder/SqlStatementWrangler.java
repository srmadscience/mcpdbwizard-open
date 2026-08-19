package com.mcpdbwizard.app.procbuilder;

import com.mcpdbwizard.mcpdbwizardconnector.BaseMethodRepresentation;
import com.mcpdbwizard.pub.*;
import com.mcpdbwizard.app.common.*;
import com.mcpdbwizard.app.procbuilder.gui.*;

import javax.swing.table.*;
//import javax.swing.event.TreeSelectionListener;
//import javax.swing.JComboBox;

/**
 * @author devteam@mcpdbwizard.com
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 * @version 2
 */
public class SqlStatementWrangler extends AbstractTableModel implements TableModel {

    public static final String[] ASP_DATA_TYPES
            = {"STRING"
            , "NUMBER"
            , "DATE"
            , "LONG"
            , "CLOB"
            , "BLOB"
            , "BFILE"
            , "RAW"
            , "LONG RAW"
            , "TIMESTAMP"
            // The two zoned timestamps. Distinct tokens rather than riding TIMESTAMP,
            // because SqlUtils resolves each to its own datatype and the generated
            // column is bound with setTIMESTAMPTZ / setTIMESTAMPLTZ. Without them the
            // comment hint "oracle.sql.TIMESTAMPTZ" matched nothing and fell back to
            // STRING, which emitted a setParam<Col>(String) the row's byte[] field
            // could not be passed to -- so a table with a PRIMARY KEY and a zoned
            // timestamp column generated code that DID NOT COMPILE.
            , "TIMESTAMPTZ"
            , "TIMESTAMPLTZ"
            , "XMLTYPE"
            // Oracle 23ai native column types. These tokens double as the Oracle
            // DATA_TYPE fed to the engine (see getDualQuery), so they must match the
            // names SqlUtils.getUnderlyingOracleDatatype / DatatypeWrangler recognise.
            , "BOOLEAN"
            , "JSON"
            , "VECTOR"
            , "VECTOR_BINARY"
            , "VECTOR_SPARSE"};

    public final static String SQL_FILENAME = "SQL_FILENAME_";

    /**
     * The statement's own text, so a config needs no SQL directory to be generated from.
     *
     * <p>Holds {@link #theOriginalStatement} — the text exactly as read — NOT {@link #theStatement},
     * which has already been through {@code cleanUpCommand} and {@code sqlplusParams}. Writing the
     * processed form would mean the next load processed it a SECOND time, and any step that is not
     * idempotent would quietly alter the statement a little more on every save.
     */
    public final static String SQL_TEXT = "SQL_TEXT_";
    public final static String SQL_PARAM_NAME = "SQL_PARAM_NAME_";
    public final static String SQL_CREATE_CLASS = "SQL_CREATE_CLASS_";
    public final static String SQL_PARAM_DATATYPE = "SQL_PARAM_DATATYPE_";
    public final static String SQL_PARAM_LINENUMBER = "SQL_PARAM_LINENUMBER_";

    // plsqldev cheat params
    public final static String SQL_PARAM_NAME_EDITABLE = "SQL_PARAM_NAME_EDITABLE_";
    public final static String SQL_PARAM_TYPE_EDITABLE = "SQL_PARAM_TYPE_EDITABLE_";
    public static final String SQL_IS_A_QUERY = "SQL_IS_A_QUERY_";
    public static final String SQL_BROKEN = "SQL_BROKEN_";
    public static final String SQL_TOOLTIP = "SQL_TOOLTIP_";

    /**
     * Constant for Cursor flag name
     */
    public static final String SQL_CURSOR_FLAG = "SQL_TURN_CURSORS_INTO_RECORDS_";
    public static final int BAD_FILE = 0;
    public static final int GOOD_FILE_BAD_SQL_STATEMENT = 1;
    public static final int VALID_SQL_STATEMENT = 2;
    public static final int SELECTED_SQL_STATEMENT = 3;
    static final char[] PARAM_TAGS = {'?'};
    /**
     * File for web service method code
     */
    public java.io.File webserviceMethodCode;
    /**
     * File for web service interface code
     */
    public java.io.File webserviceInterfaceCode;
    public BaseMethodRepresentation theMetaData = null;
    /**
     * Flag for whether bfile methods are needed by services
     */
    public boolean needBfileCreationRoutine;
    /*
     * Flag for whether we should turn cursors into records
     */
    public boolean turnCursorsIntoRecords = true;
    /**
     * CSE for query cols
     */
    public CallableStatementParameterEngine qryCse = null;
    /**
     * Dual query for cursor
     */
    public com.mcpdbwizard.pub.ReadOnlyRowSet queryTemplate = null;
    public String[] cursorColumnNames = null;
    String theStatement = null;
    String theOriginalStatement = null;
    String sqlFilename = null;
    String realFilename = null;
    String originalFilename = null;
    String cursorAttrFileName = null;
    String cursorMethodFileName = null;
    java.util.Properties theProperties = null;
    int thePropertiesRecordNumber;
    int statementType = Integer.MIN_VALUE;
    int fileType = BAD_FILE;
    LogInterface theLog = null;
    String[] paramDefaultName = null;
    String[] paramDefaultJavaDataTypes = null;
    String[] paramHintName = null;
    String[] paramHintJavaDataTypes = null;
    String[] paramPropName = null;
    String[] paramPropJavaDataTypes = null;
    int[] paramLocations = null;
    int[] paramLineNumbers = null;
    boolean[] paramsOverridden = null;
    AspFilePanel thePanel = null;
    int rowCount = 1;
    char[] sqlStatementCharArray = null;
    boolean[] sqlStatementComments = null;
    int[] sqlStatementParamLocations = null;
    int[] sqlStatementLineNumbers = null;
    String[] sqlStatementLinesArray = null;
    boolean isDDL = false;
    char defineChar = '&';

    public SqlStatementWrangler(String theFilename
            , String theFilesStatement
            , java.util.Properties theProperties
            , int thePropertiesRecordNumber
            , LogInterface theLog) {
        this.originalFilename = new String(theFilename);
        this.sqlFilename = new String(theFilename);
        // The author's MCP tool description for this statement, if any. Read straight from the
        // config here rather than plumbed in, because this class is already handed the properties
        // and its own index -- the two things the SQL_MCP_DESC_<i> key is made of. null means no
        // override, so the generator uses the description it derives itself.
        this.mcpDescription = theProperties.getProperty("SQL_MCP_DESC_" + thePropertiesRecordNumber);
        if (theFilename.length() >= 4) {
            this.realFilename = theFilename.substring(0, theFilename.length() - 4);
        }


        this.realFilename = JavaUtils.getJavaName(this.realFilename);
        // make sure first letter is upper case...
        if (this.realFilename.length() == 1) {
            this.realFilename = this.realFilename.toUpperCase();
        } else {
            this.realFilename = this.realFilename.substring(0, 1).toUpperCase() + this.realFilename.substring(1);
        }

        this.cursorMethodFileName = JavaUtils.getJavaName(this.realFilename + "Cursor");
        this.cursorAttrFileName = JavaUtils.getJavaName(this.realFilename + "CursorAttr");

        this.theStatement = theFilesStatement;
        this.theOriginalStatement = new String(theFilesStatement);
        this.theLog = theLog;


        // Clean up Statement
        this.theStatement = sqlplusParams(cleanUpCommand(theFilesStatement), defineChar);

        // Identify statement type
        statementType = SqlUtils.getStatementType(theStatement);
        if (statementType == SqlUtils.UNKNOWN) {
            theLog.error("SQL Statement is not of known type");
            fileType = GOOD_FILE_BAD_SQL_STATEMENT;
        } else {
            // if not query turn off createRecords...

            // count parameters. Add name/datatype info to vector as we go along

            // Identify comments
            sqlStatementCharArray = theStatement.toCharArray();
            sqlStatementComments = new boolean[sqlStatementCharArray.length];
            sqlStatementParamLocations = new int[sqlStatementCharArray.length];
            sqlStatementLineNumbers = new int[sqlStatementCharArray.length];

            int paramId = 0;
            int currentLineNumber = 1;

            for (int i = 0; i < sqlStatementCharArray.length; i++) {
                sqlStatementComments[i] = false;
                sqlStatementParamLocations[i] = -1;
            }

            boolean inAComment = false;
            for (int i = 0; i < (sqlStatementCharArray.length - 1); i++) {
                if (sqlStatementCharArray[i] == '\n') {
                    currentLineNumber++;
                    rowCount++;
                }

                sqlStatementLineNumbers[i] = currentLineNumber;

                if (sqlStatementCharArray[i + 0] == '/'
                        && sqlStatementCharArray[i + 1] == '*') {
                    inAComment = true;
                    sqlStatementComments[i + 0] = true;
                    sqlStatementComments[i + 1] = true;
                } else if (sqlStatementCharArray[i + 0] == '*'
                        && sqlStatementCharArray[i + 1] == '/') {
                    inAComment = false;
                    sqlStatementComments[i + 0] = true;
                    sqlStatementComments[i + 1] = true;
                } else if (inAComment) {
                    sqlStatementComments[i + 0] = true;
                }
            }

            sqlStatementLineNumbers[sqlStatementLineNumbers.length - 1] = currentLineNumber;

            // Now search for parameters...
            for (int i = 0; i < sqlStatementCharArray.length; i++) {
                if (!sqlStatementComments[i]) {
                    for (int j = 0; j < PARAM_TAGS.length; j++) {
                        if (sqlStatementCharArray[i] == PARAM_TAGS[j]) {
                            // Add param location to list
                            sqlStatementParamLocations[i] = ++paramId;
                        }
                    }
                }
            }

            // create data structure for parameters, default names if needed
            paramDefaultName = new String[paramId];
            paramDefaultJavaDataTypes = new String[paramId];

            paramHintName = new String[paramId];
            paramHintJavaDataTypes = new String[paramId];

            paramPropName = new String[paramId];
            paramPropJavaDataTypes = new String[paramId];

            paramLocations = new int[paramId];
            paramLineNumbers = new int[paramId];
            paramsOverridden = new boolean[paramId];

            // where we found the last param in the param array...
            int paramHighWaterMark = 0;

            for (int i = 0; i < paramDefaultName.length; i++) {
                // Now find exactly where this param is located
                for (int j = paramHighWaterMark; j < sqlStatementParamLocations.length; j++) {
                    if (sqlStatementParamLocations[j] == (i + 1)) {
                        paramLocations[i] = j;
                        paramLineNumbers[i] = sqlStatementLineNumbers[j];
                        paramHighWaterMark = j + 1;
                        // force end of loop
                        //         j = sqlStatementParamLocations.length;
                        break;
                    }
                }
            }

            for (int i = 0; i < paramDefaultName.length; i++) {
                setParamHints(i);
                setParamDefaults(i);

                // See if we can find a better name/data type for this param
                // Initialise names
                paramPropName[i] = paramDefaultName[i];
                paramPropJavaDataTypes[i] = paramDefaultJavaDataTypes[i];


            }

            fileType = VALID_SQL_STATEMENT;

            // Overwrite parameter info with prop file info.
            readProperties(theProperties, thePropertiesRecordNumber);

            // override prop file info with hint info
            for (int i = 0; i < paramDefaultName.length; i++) {
                if (paramHintName[i].length() > 0) {
                    paramPropName[i] = new String(paramHintName[i]);
                }

                if (paramHintJavaDataTypes[i].length() > 0) {
                    paramPropJavaDataTypes[i] = new String(paramHintJavaDataTypes[i]);
                }

            }

            makeSqlStatementLinesArray();
        }

    }

    public static String sqlplusParams(String theCommand, char defineChar) {
        //int highWatermark = 0;
        //boolean moreParams = false;
        final char[] legalNameChars =
                {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
                        , 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
                        , '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '_'};

        if (theCommand.indexOf(defineChar) == -1) {
            return (theCommand.trim());
        }

        // get rid of double define characters
        char[] doubleChar = {defineChar, defineChar};
        char[] singleChar = {defineChar};
        String newString = JavaUtils.replaceString(theCommand, new String(doubleChar), new String(singleChar)) + " ";

        while (newString.indexOf(defineChar) > -1) {
            String leftHalf = newString.substring(0, (newString.indexOf(defineChar)));
            String rightHalfComment = "";
            String rightHalf = newString.substring(newString.indexOf(defineChar) + 1);
            boolean hasQuote = false;
            //System.out.println(leftHalf + "| |" + rightHalfComment + "| |" + rightHalf);
            if (leftHalf.endsWith("'")) {
                hasQuote = true;
            }

            // Turn characters after defineChar into a comment...
            int lastDefineChar = Integer.MIN_VALUE;
            char[] rightHalfArray = rightHalf.toCharArray();

            for (int i = 0; i < rightHalfArray.length; i++) {
                boolean isLegalChar = false;

                for (int z = 0; z < legalNameChars.length; z++) {
                    if (rightHalfArray[i] == legalNameChars[z]) {
                        isLegalChar = true;
                        break;
                    }
                }

                if (!isLegalChar) {
                    lastDefineChar = i;
                    break;
                }

            }

            if (lastDefineChar > 0) {
                rightHalfComment = " /* " + rightHalf.substring(0, lastDefineChar) + " */";
                rightHalf = rightHalf.substring(lastDefineChar);
            }

            // Remove quotes if present
            if (hasQuote) {

                if (leftHalf.endsWith("'")) {
                    leftHalf = leftHalf.substring(0, leftHalf.length() - 1);
                }

                if (rightHalf.startsWith("'")) {
                    rightHalf = rightHalf.substring(1);
                }
            }

            newString = leftHalf + "?" + rightHalfComment + rightHalf;
        } // while
        //System.out.println(leftHalf + "| |" + rightHalfComment + "| |" + rightHalf);

        return (newString.trim());
    }

    private static String cleanUpCommand(String theCommand) {
        String goodChar = "*/";

        String[] endChars = {"/", ";"};

        // Fix double quotes
        String newString = JavaUtils.replaceString(theCommand, "\"", "\\\"");
        newString = JavaUtils.replaceString(newString, "\\\\\"", "\\\"");

        // keep going round removing trailing spaces and endChars until
        // we can't any more
        int oldLength = Integer.MIN_VALUE;
        int newLength = newString.length();

        while (oldLength != newLength) {
            //System.out.println("b4"+newString);
            oldLength = newString.length();

            newString = newString.trim();

            for (int i = 0; i < endChars.length; i++) {
                if (newString.endsWith(endChars[i])
                        && (!newString.endsWith(goodChar))
                ) {
                    newString = newString.substring(0, (newString.length() - 1));
                }
            }

            newLength = newString.length();
            //System.out.println("a4"+newString);
        }

        return (newString);
    }

    private void setParamDefaults(int i) {
        // Initialise defaults
        if (paramHintName[i].length() > 0) {
            paramDefaultName[i] = new String(paramHintName[i]);
        } else {
            paramDefaultName[i] = "Param" + (1 + i);
        }

        if (paramHintJavaDataTypes[i].length() > 0) {
            paramDefaultJavaDataTypes[i] = new String(paramHintJavaDataTypes[i]);
        } else {
            paramDefaultJavaDataTypes[i] = ASP_DATA_TYPES[0];
        }

    }

    private void setParamHints(int i) {
        // initialise hints....
        paramHintName[i] = "";
        paramHintJavaDataTypes[i] = "";

        // Only do this if there are comments present
        if (theStatement.indexOf("/*") > -1) {
            // See if we can find a comment with a better default value/data type

            int startValue = paramLocations[i];
            int endValue = sqlStatementCharArray.length - 1;

            // 2. Remove part of string after next param if present
            if ((i + 1) < paramHintName.length) {
                endValue = paramLocations[i + 1];
            }

            // loop through the comments between these two points. ignore / and *.
            // stop if we hit a new line

            String commentText = "";

            for (int j = startValue; j < endValue; j++) {
                if (sqlStatementCharArray[j] == '\n'
                        || sqlStatementCharArray[j] == '\r') {
                    break;
                } else if (sqlStatementComments[j]) {
                    if (sqlStatementCharArray[j] == '/'
                            || sqlStatementCharArray[j] == '*') {
                    } else {
                        commentText = commentText + sqlStatementCharArray[j];
                    }
                }
            }

            commentText = commentText.trim();

            while (commentText.length() > 1 && commentText.startsWith(" ")) {
                commentText = commentText.substring(1);
            }

            int secondWord = commentText.indexOf(' ');

            if (secondWord == -1) {
                paramHintName[i] = new String(commentText);
            } else {
                paramHintName[i] = new String(commentText.substring(0, secondWord));
                paramHintJavaDataTypes[i] = new String(commentText.substring(secondWord + 1).toUpperCase());
            }

            // Clean up
            paramHintName[i] = paramHintName[i].trim();
            paramHintJavaDataTypes[i] = paramHintJavaDataTypes[i].trim();

            // Check for common synonyms of STRING
            String[] stringSyns = {"VARCHAR", "VARCHAR2", "TEXT"};
            for (int q = 0; q < stringSyns.length; q++) {
                if (paramHintJavaDataTypes[i].equalsIgnoreCase(stringSyns[q])) {
                    paramHintJavaDataTypes[i] = "STRING";
                    break;
                }
            }

            // Check for common synonyms of NUMBER
            String[] numberSyns = {"java.math.BigDecimal"};
            for (int q = 0; q < numberSyns.length; q++) {
                if (paramHintJavaDataTypes[i].equalsIgnoreCase(numberSyns[q])) {
                    paramHintJavaDataTypes[i] = "NUMBER";
                }
            }

            // Check for common synonyms of DATE
            String[] dateSyns = {"java.sql.Timestamp", "java.util.Date"};
            for (int q = 0; q < dateSyns.length; q++) {
                if (paramHintJavaDataTypes[i].equalsIgnoreCase(dateSyns[q])) {
                    paramHintJavaDataTypes[i] = "DATE";
                }
            }

            // The zoned timestamps, whose generated Java spelling is the oracle.sql
            // wrapper class. Checked BEFORE the RAW block deliberately: the row field
            // for one of these is a byte[] (Oracle's wire form), so anything that let
            // it reach the byte[] synonym would bind it as RAW -- the same ambiguity
            // that binary VECTOR columns had to be given their own token to escape.
            String[] timestampTzSyns = {"oracle.sql.TIMESTAMPTZ"};
            for (int q = 0; q < timestampTzSyns.length; q++) {
                if (paramHintJavaDataTypes[i].equalsIgnoreCase(timestampTzSyns[q])) {
                    paramHintJavaDataTypes[i] = "TIMESTAMPTZ";
                }
            }

            String[] timestampLtzSyns = {"oracle.sql.TIMESTAMPLTZ"};
            for (int q = 0; q < timestampLtzSyns.length; q++) {
                if (paramHintJavaDataTypes[i].equalsIgnoreCase(timestampLtzSyns[q])) {
                    paramHintJavaDataTypes[i] = "TIMESTAMPLTZ";
                }
            }

            // Check for common synonyms of RAW
            String[] rawSyns = {"byte[]"};
            for (int q = 0; q < rawSyns.length; q++) {
                if (paramHintJavaDataTypes[i].equalsIgnoreCase(rawSyns[q])) {
                    paramHintJavaDataTypes[i] = "RAW";
                }
            }

            // Oracle 23ai native types: map the generated Java type spellings (as
            // emitted into the SQL comment by JavaUtils.oracle2JavaDatatype) onto the
            // BOOLEAN / JSON / VECTOR tokens. ("Boolean" already upper-cases to the
            // BOOLEAN token, so only JSON and VECTOR need a synonym.)
            String[] jsonSyns = {"oracle.sql.json.OracleJsonValue"};
            for (int q = 0; q < jsonSyns.length; q++) {
                if (paramHintJavaDataTypes[i].equalsIgnoreCase(jsonSyns[q])) {
                    paramHintJavaDataTypes[i] = "JSON";
                }
            }

            String[] vectorSyns = {"double[]"};
            for (int q = 0; q < vectorSyns.length; q++) {
                if (paramHintJavaDataTypes[i].equalsIgnoreCase(vectorSyns[q])) {
                    paramHintJavaDataTypes[i] = "VECTOR";
                }
            }

            // make sure data type is on supported list...
            if (paramHintJavaDataTypes[i] != null && paramHintJavaDataTypes[i].length() > 0) {
                boolean knownDatatype = false;
                for (int z = 0; z < ASP_DATA_TYPES.length; z++) {
                    if (paramHintJavaDataTypes[i].equals(ASP_DATA_TYPES[z])) {
                        knownDatatype = true;
                        break;
                    }
                }

                if (!knownDatatype) {
                    theLog.warning("Data type of " + paramHintJavaDataTypes[i] + " not recognized; set to " + ASP_DATA_TYPES[0], true, true);
                    paramHintJavaDataTypes[i] = ASP_DATA_TYPES[0];
                }
            }

            //System.out.println(commentText + " |" + paramHintName[i] + "| |" + paramHintJavaDataTypes[i]+"|");
        }
    }

    public boolean getCreateJava() {
        boolean value;

        if (fileType == SELECTED_SQL_STATEMENT) {
            value = true;
        } else {
            value = false;
        }

        return (value);
    }

    public void setCreateJava(boolean newValue) {

        if (newValue) {
            if (fileType == VALID_SQL_STATEMENT) {
                fileType = SELECTED_SQL_STATEMENT;
            }
        } else {
            if (fileType == SELECTED_SQL_STATEMENT) {
                fileType = VALID_SQL_STATEMENT;
            }
        }
    }

    public boolean getCreateRecords() {
        return (turnCursorsIntoRecords);
    }

    public void setCreateRecords(boolean newValue) {

        if (newValue) {
            turnCursorsIntoRecords = true;
        } else {
            turnCursorsIntoRecords = false;
        }
    }

    public int getFileType() {
        return (fileType);
    }

    public void readProperties(java.util.Properties theProperties
            , int thePropertiesRecordNumber) {
        this.theProperties = theProperties;
        this.thePropertiesRecordNumber = thePropertiesRecordNumber;

        // Name to search for in prop file
        String testString = SQL_FILENAME + thePropertiesRecordNumber;
        String createJava = "YES";

        sqlFilename = theProperties.getProperty(testString, sqlFilename);

        // Remove .sql from filename...
        final char[] sArray = {'S', 's'};
        final char[] qArray = {'Q', 'q'};
        final char[] lArray = {'L', 'l'};

        for (int s = 0; s < sArray.length; s++) {
            for (int q = 0; q < qArray.length; q++) {
                for (int l = 0; l < lArray.length; l++) {
                    sqlFilename = JavaUtils.replaceString(sqlFilename, "." + sArray[s] + qArray[q] + lArray[l], "");
                }
            }
        }

        if (theProperties.containsKey(testString)) {
            for (int i = 0; i < paramPropName.length; i++) {
                paramPropName[i] = theProperties.getProperty(SQL_PARAM_NAME + thePropertiesRecordNumber + "_" + i, paramDefaultName[i]);
                if (paramPropName[i] == null || paramPropName[i].length() == 0) {
                    paramPropName[i] = new String(paramDefaultName[i]);
                }

                paramPropJavaDataTypes[i] = theProperties.getProperty(SQL_PARAM_DATATYPE + thePropertiesRecordNumber + "_" + i, paramDefaultJavaDataTypes[i]);
                if (paramPropJavaDataTypes[i] == null || paramPropJavaDataTypes[i].length() == 0) {
                    paramPropJavaDataTypes[i] = new String(paramDefaultJavaDataTypes[i]);
                }

                // make sure data type is on supported list...
                boolean knownDatatype = false;
                for (int z = 0; z < ASP_DATA_TYPES.length; z++) {
                    if (paramPropJavaDataTypes[i].equals(ASP_DATA_TYPES[z])) {
                        knownDatatype = true;
                        break;
                    }
                }

                if (!knownDatatype) {
                    theLog.warning("Data type of " + paramPropJavaDataTypes[i] + " not recognized; set to " + ASP_DATA_TYPES[0], true, true);
                    paramPropJavaDataTypes[i] = ASP_DATA_TYPES[0];
                }
            }
        }

        if (theProperties.getProperty(SQL_CURSOR_FLAG + thePropertiesRecordNumber, "YES").equals("YES")) {
            turnCursorsIntoRecords = true;
        } else {
            turnCursorsIntoRecords = false;
        }

        createJava = theProperties.getProperty(SQL_CREATE_CLASS + thePropertiesRecordNumber, "YES");

        if (createJava.equalsIgnoreCase("NO")) {
            if (fileType == SELECTED_SQL_STATEMENT) {
                fileType = VALID_SQL_STATEMENT;
            }
        } else {
            // must be YES
            if (fileType == VALID_SQL_STATEMENT) {
                fileType = SELECTED_SQL_STATEMENT;
            }
        }

    }

    public void writeProperties(java.util.Properties theProperties
            , int thePropertiesRecordNumber) {
        this.theProperties = theProperties;
        this.thePropertiesRecordNumber = thePropertiesRecordNumber;

        // Write filename
        String testString = SQL_FILENAME + thePropertiesRecordNumber;
        theProperties.setProperty(testString, originalFilename);

        // ...and the statement itself, so the config stands alone. The UNPROCESSED text: see
        // SQL_TEXT. The filename stays, because it is this statement's identity and the source of
        // its generated class name, not merely a pointer at a file.
        if (theOriginalStatement != null) {
            theProperties.setProperty(SQL_TEXT + thePropertiesRecordNumber, theOriginalStatement);
        }

        String createJava = "";

        if (fileType == SELECTED_SQL_STATEMENT) {
            createJava = "YES";
        } else {
            createJava = "NO";
        }

        theProperties.setProperty(SQL_CREATE_CLASS + thePropertiesRecordNumber, createJava);

        if (turnCursorsIntoRecords) {
            theProperties.setProperty(SQL_CURSOR_FLAG + thePropertiesRecordNumber, "YES");
        } else {
            theProperties.setProperty(SQL_CURSOR_FLAG + thePropertiesRecordNumber, "NO");
        }

        if (paramPropName != null && paramPropJavaDataTypes != null) {
            for (int i = 0; i < paramPropName.length; i++) {
                theProperties.setProperty(SQL_PARAM_LINENUMBER + thePropertiesRecordNumber + "_" + i, paramLineNumbers[i] + "");

                if (paramPropName[i] != null) {
                    theProperties.setProperty(SQL_PARAM_NAME + thePropertiesRecordNumber + "_" + i, paramPropName[i]);
                }
                if (paramPropJavaDataTypes[i] != null) {
                    theProperties.setProperty(SQL_PARAM_DATATYPE + thePropertiesRecordNumber + "_" + i, paramPropJavaDataTypes[i]);
                }
            }
        }
    }

    public void writeHintProperties(java.util.Properties theProperties
            , int thePropertiesRecordNumber) {
        this.theProperties = theProperties;
        this.thePropertiesRecordNumber = thePropertiesRecordNumber;
        String testString = SQL_FILENAME + thePropertiesRecordNumber;
        theProperties.setProperty(testString, originalFilename);

        if (paramPropName != null && paramPropJavaDataTypes != null) {
            for (int i = 0; i < paramPropName.length; i++) {
                theProperties.setProperty(SQL_PARAM_LINENUMBER + thePropertiesRecordNumber + "_" + i, paramLineNumbers[i] + "");

                if (paramPropName[i] != null) {
                    theProperties.setProperty(SQL_PARAM_NAME + thePropertiesRecordNumber + "_" + i, paramPropName[i]);
                }
                if (paramPropJavaDataTypes[i] != null) {
                    theProperties.setProperty(SQL_PARAM_DATATYPE + thePropertiesRecordNumber + "_" + i, paramPropJavaDataTypes[i]);
                }

                if (paramHintName[i] != null && paramHintName[i].length() > 0) {
                    theProperties.setProperty(SQL_PARAM_NAME_EDITABLE + thePropertiesRecordNumber + "_" + i, "NO");
                } else {
                    theProperties.setProperty(SQL_PARAM_NAME_EDITABLE + thePropertiesRecordNumber + "_" + i, "YES");
                }

                if (paramHintJavaDataTypes[i] != null && paramHintJavaDataTypes[i].length() > 0) {
                    theProperties.setProperty(SQL_PARAM_TYPE_EDITABLE + thePropertiesRecordNumber + "_" + i, "NO");
                } else {
                    theProperties.setProperty(SQL_PARAM_TYPE_EDITABLE + thePropertiesRecordNumber + "_" + i, "YES");
                }
            }
        }
    }

    /** The statement exactly as read, before cleanUpCommand/sqlplusParams. */
    public String getOriginalSqlStatement() {
        return theOriginalStatement;
    }

    public String getRawSqlStatement() {
        return (theStatement);
    }

    public String getStatementToolTipText(int rowIndex) {
        String toolTipText = "SQL Statement Text";

        String otherToolTipText = "";
        String s = "";

        for (int i = 0; i < paramLineNumbers.length; i++) {
            if (paramLineNumbers[i] == (rowIndex + 1)) {
                if (otherToolTipText.length() == 0) {
                    otherToolTipText = otherToolTipText + paramPropName[i] + " (" + paramPropJavaDataTypes[i] + ")";
                } else {
                    s = "s";
                    otherToolTipText = otherToolTipText + "," + paramPropName[i] + " (" + paramPropJavaDataTypes[i] + ")";
                }
            }
        }

        if (otherToolTipText.length() > 0) {
            return ("Parameter" + s + " " + otherToolTipText);
        }

        return (toolTipText);
    }

    public int getLineNumber(int param) {
        return (paramLineNumbers[param]);
    }

    public String[] getSqlStatementLinesArray() {
        return (sqlStatementLinesArray);
    }

    public String getSqlStatement() {
        String displayStatement = "1\t| ";

        for (int i = 1; i <= rowCount; i++) {

            for (int j = 0; j < sqlStatementCharArray.length; j++) {
                if (sqlStatementLineNumbers[j] == i) {
                    if (sqlStatementComments[j] && sqlStatementCharArray[j] == '?') {
                        // prevent statementParametets being baffled by comments
                        displayStatement = displayStatement + ' ';
                    } else {
                        displayStatement = displayStatement + sqlStatementCharArray[j];
                    }

                    if (sqlStatementCharArray[j] == '\n') {
                        displayStatement = displayStatement + i + "\t| ";
                    }
                }
            }
        }

        return (displayStatement);
    }

    private void makeSqlStatementLinesArray() {

        sqlStatementLinesArray = new String[rowCount];

        for (int i = 1; i <= rowCount; i++) {
            String aLine = "";

            for (int j = 0; j < sqlStatementCharArray.length; j++) {
                if (sqlStatementLineNumbers[j] == i) {
                    if (sqlStatementCharArray[j] != '\n'
                            && sqlStatementCharArray[j] != '\r') {
                        if (sqlStatementComments[j] && sqlStatementCharArray[j] == '?') {
                            // prevent statementParametets being baffled by comments
                            aLine = aLine + '_';
                        } else {
                            aLine = aLine + sqlStatementCharArray[j];
                        }
                    }
                }
            }

            sqlStatementLinesArray[i - 1] = new String(aLine);
        }
    }

    /** The author's MCP tool description from {@code SQL_MCP_DESC_<i>}, or null to use the generated one. */
    private String mcpDescription = null;

    /** @return the configured MCP tool description, or null when none was set */
    public String getMcpDescription() {
        return mcpDescription;
    }

    public String getRealFileName() {
        return (realFilename);
    }

    public String getSqlFileName() {
        return (sqlFilename);
    }

    public String getOriginalFileName() {
        return (originalFilename);
    }

    public String getJavaFileName() {
        return (realFilename + ".java");
    }

    public void setPanel(AspFilePanel thePanel) {
        thePanel.setSqlStatementWrangler(this);
        thePanel.setFileName(sqlFilename);
        //thePanel.setFileText(getSqlStatement(),rowCount+1);
        thePanel.setFileText(getSqlStatementLinesArray());
        thePanel.setTableModel(this);
        thePanel.setCreateJavaClass(fileType);
        thePanel.setCreateRecords(turnCursorsIntoRecords);
        if (statementType != SqlUtils.SELECT) {
            thePanel.setCreateRecordsEnabled(false);
        } else {
            thePanel.setCreateRecordsEnabled(true);
        }

    }

    public void getPanel(AspFilePanel thePanel) {
        boolean createClass = thePanel.getCreateJavaClass();

        if (fileType == VALID_SQL_STATEMENT
                || fileType == SELECTED_SQL_STATEMENT) {
            if (createClass) {
                fileType = SELECTED_SQL_STATEMENT;
            } else {
                fileType = VALID_SQL_STATEMENT;
            }
        }

        turnCursorsIntoRecords = thePanel.getCreateRecords();
    }

    public int getRowCount() {
        return (paramDefaultName.length);
    }

    public int getColumnCount() {
        return (3);
    }

    public String getColumnName(int columnIndex) {
        String[] colNames = {"Line #", "Parameter Name", "Data Type"};
        return (colNames[columnIndex]);
    }

    /**
     * Returns Object.class by default
     */
    public Class getColumnClass(int columnIndex) {
        Class value = null;
        if (columnIndex == 0) {
            value = String.class;
        } else if (columnIndex == 1) {
            value = SqlParameterCellRenderer.class;
        } else if (columnIndex == 2) {
            value = SqlParameterCellRenderer.class;
        }
        return (value);
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return (false);
        } else if (columnIndex == 1) {
            if (paramHintName[rowIndex].length() > 0) {
                return (false);
            }
        } else if (columnIndex == 2) {
            if (paramHintJavaDataTypes[rowIndex].length() > 0) {
                return (false);
            }
        }


        return (true);
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        Object value = null;


        if (columnIndex == 0) {
            value = "#" + paramLineNumbers[rowIndex];
        } else if (columnIndex == 1) {
            value = paramPropName[rowIndex];
        } else if (columnIndex == 2) {
            value = paramPropJavaDataTypes[rowIndex];
        }

        return (value);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        //Object value;

        if (columnIndex == 1) {
            paramPropName[rowIndex] = new String(aValue.toString());
        } else if (columnIndex == 2) {
            paramPropJavaDataTypes[rowIndex] = new String(aValue.toString());
        }

    }

    public String getDualQuery(int aParam, String targetOracleVersion) {
        if (aParam != 42) {
            return ("SELECT * from DUAL");
        }

        String dualQuery = "";

        if (paramPropName.length == 0) {
            dualQuery = dualQuery + "SELECT 'IN' IN_OUT, '" + "param1" + "' ARGUMENT_NAME, "
                    + "0 OVERLOAD, '" + "VARCHAR2" + "' DATA_TYPE, "
                    + (0 + 1) + " POSITION, USER OWNER, '' PACKAGE_NAME, '' OBJECT_NAME FROM DUAL WHERE  1 = 2 ";
        } else {
            for (int i = 0; i < paramPropName.length; i++) {
                String theDataType = new String(paramPropJavaDataTypes[i]);
                if (targetOracleVersion.equals("8.1.5") && theDataType.equals("TIMESTAMP")) {
                    theDataType = "DATE";
                }

                dualQuery = dualQuery + "SELECT 'IN' IN_OUT, 'Param" + paramPropName[i] + "' ARGUMENT_NAME, "
                        + "0 OVERLOAD, '" + theDataType + "' DATA_TYPE, "
                        + (i + 1) + " POSITION, USER OWNER, '' PACKAGE_NAME, '' OBJECT_NAME FROM DUAL ";

                if (i < (paramPropName.length - 1)) {
                    dualQuery = dualQuery + " UNION ";
                }
            }
        }
        dualQuery = dualQuery + " ORDER BY POSITION ";
        return (dualQuery);
    }

    private String getDualCursorQuery(int aParam, String targetOracleVersion, ReadOnlyRowSet theRowSet, String javaNamingConvention) {
        if (aParam != 42) {
            return ("SELECT * from DUAL");
        }

        String dualQuery = "";

        try {

            // fix column names.
            cursorColumnNames = new String[theRowSet.getColumnOracleDatatypeNames().length];

            for (int i = 0; i < theRowSet.getColumnOracleDatatypeNames().length; i++) {
                String theColumnName = new String(theRowSet.getColumnName(i));
                if (theColumnName.startsWith(":")) {
                    cursorColumnNames[i] = "Column" + i;
                } else {
                    cursorColumnNames[i] = JavaUtils.getJavaName(theColumnName.toLowerCase(), javaNamingConvention, theLog);
                }
            }


            // walk through array and fix dups   //DRKLUGE could be better
            for (int i = 0; i < cursorColumnNames.length; i++) {
                for (int j = i; j < cursorColumnNames.length; j++) {
                    if (i != j) {
                        if (cursorColumnNames[i].equals(cursorColumnNames[j])) {
                            cursorColumnNames[i] = cursorColumnNames[i] + "_" + i;
                            cursorColumnNames[j] = cursorColumnNames[j] + "_" + j;
                            break;
                        }
                    }
                }

            }

            for (int i = 0; i < theRowSet.getColumnOracleDatatypeNames().length; i++) {
                String theDataType = new String(theRowSet.getColumnOracleDatatypeNames()[i]);
                if (targetOracleVersion.equals("8.1.5") && theDataType.equals("TIMESTAMP")) {
                    theDataType = "DATE";
                }


                dualQuery = dualQuery + "SELECT 'OUT' IN_OUT, '" + cursorColumnNames[i] + "' ARGUMENT_NAME, "
                        + "0 OVERLOAD, '" + theDataType + "' DATA_TYPE, "
                        + (i + 1) + " POSITION, USER OWNER, '' PACKAGE_NAME, '' OBJECT_NAME FROM DUAL ";

                if (i < (theRowSet.getColumnOracleDatatypeNames().length - 1)) {
                    dualQuery = dualQuery + " UNION ";
                }
            }
        } catch (CSException e) {
            theLog.error("SqlStatementWrangler.getDualCursorQuery:");
            theLog.error(e);
            return ("");
        }

        dualQuery = dualQuery + " ORDER BY POSITION ";
        return (dualQuery);
    }

    public int getStatementType() {
        return (statementType);
    }

    public void populateQueryTemplate(java.sql.Connection theConnection, String loginOracleVersion, String javaNamingConvention) throws CSException {

        if (statementType == SqlUtils.SELECT) {
            //theLog.info("Seeing what columns query in file " + originalFilename + " returns...");
            queryTemplate = null;

            com.mcpdbwizard.pub.QueryStatement deadQuery = null;

            try {
                // Try issuing a knobbled version of query that can't return results.
                deadQuery = new QueryStatement(returnDeadQuery(true), theLog, theConnection);

                for (int i = 0; i < paramLocations.length; i++) {
                    deadQuery.setParam(i + 1, "");
                }

                deadQuery.setQueryRows(1);
                deadQuery.setKeepLobs(true);

                ReadOnlyRowSet queryResult = deadQuery.execute();
                deadQuery.releaseResources();

                com.mcpdbwizard.pub.QueryStatement colQuery = new QueryStatement(getDualCursorQuery(42, loginOracleVersion, queryResult, javaNamingConvention), theLog, theConnection);

                queryTemplate = colQuery.execute();
                colQuery.releaseResources();
            } catch (Exception e) {
                try {
                    // The knobbled version won't run. Try the original version.
                    deadQuery = new QueryStatement(returnDeadQuery(false), theLog, theConnection);

                    for (int i = 0; i < paramLocations.length; i++) {
                        deadQuery.setParam(i + 1, "");
                    }

                    deadQuery.setQueryRows(1);
                    deadQuery.setKeepLobs(true);

                    ReadOnlyRowSet queryResult = deadQuery.execute();
                    deadQuery.releaseResources();

                    com.mcpdbwizard.pub.QueryStatement colQuery = new QueryStatement(getDualCursorQuery(42, loginOracleVersion, queryResult, javaNamingConvention), theLog, theConnection);

                    queryTemplate = colQuery.execute();
                    colQuery.releaseResources();
                } catch (Exception e2) {
                    // Not that one either. Maybe this is a bad SQL statement.
                    theLog.warning("Unable to determine columns returned by " + originalFilename + ". No matching records will be created.");
                }
            }

        }
    }

    private String returnDeadQuery(boolean tweakQuery) {
        String deadQuery = "";
        for (int i = 0; i < sqlStatementLinesArray.length; i++) {
            deadQuery = deadQuery + " " + sqlStatementLinesArray[i];
        }

        deadQuery = deadQuery.trim();

        if (tweakQuery) {
            deadQuery = deadQuery.toUpperCase();

            // Where is the FROM clause
            int fromValue = deadQuery.lastIndexOf("FROM");

            // Does it have a WHERE clause?
            int whereValue = deadQuery.lastIndexOf("WHERE");

            // Does it an ORDER BY clause?
            int orderByValue = deadQuery.lastIndexOf("ORDER");

            // Does it have a GROUP BY clause?
            int groupByValue = deadQuery.lastIndexOf("GROUP");

            // Does it have a HAVING clause?
            int havingValue = deadQuery.lastIndexOf("HAVING");

            //fix query so it can't run....
            if (whereValue >= 0) {
                String frontBit = deadQuery.substring(0, whereValue);
                String backBit = deadQuery.substring(whereValue + 5);
                deadQuery = frontBit + " WHERE 1 = 2 AND " + backBit;
            } else if (groupByValue >= 0) {
                String frontBit = deadQuery.substring(0, groupByValue);
                String backBit = deadQuery.substring(groupByValue);
                deadQuery = frontBit + " WHERE 1 = 2 " + backBit;
            } else if (orderByValue >= 0) {
                String frontBit = deadQuery.substring(0, orderByValue);
                String backBit = deadQuery.substring(orderByValue);
                deadQuery = frontBit + " WHERE 1 = 2 " + backBit;
            } else if (havingValue >= 0) {
                String frontBit = deadQuery.substring(0, havingValue);
                String backBit = deadQuery.substring(havingValue);
                deadQuery = frontBit + " WHERE 1 = 2 " + backBit;
            } else {
                deadQuery = deadQuery + " WHERE 1 = 2 ";
            }

        }

        return (deadQuery);
    }

    public void addCursorMethods(JavaChunk theJavaChunk, boolean comments, String sTheLog, boolean webServices) {
        if (comments) {
            theJavaChunk.print("/**");
            theJavaChunk.print("* Return query as an array of " + cursorMethodFileName);
            theJavaChunk.print("* This static method turns a ReadOnlyRowSet into an array of records.");
            theJavaChunk.print("* The ReadOnlyRowSet can be from any query but the number and datatypes ");
            theJavaChunk.print("* of its columns must match those expected by this method. ");
            theJavaChunk.print("* @param com.mcpdbwizard.pub.ReadOnlyRowSet theRowSet");
            theJavaChunk.print("* @param com.mcpdbwizard.pub.LogInterface " + sTheLog);
            if (webServices) {
                theJavaChunk.print("* @param boolean moveFilesToByteArrays Copies any files to matching byte arrays and then deletes them.");
            }
            theJavaChunk.print("* @return " + cursorMethodFileName + "[]");
            theJavaChunk.print("* @throws CSException");
            theJavaChunk.print("* @since V5.0.2179");
            theJavaChunk.print("*/");
        }

        if (webServices) {
            theJavaChunk.print("public static " + cursorMethodFileName + "[] getArrayFromReadOnlyRowSet(com.mcpdbwizard.pub.ReadOnlyRowSet theRowSet, com.mcpdbwizard.pub.LogInterface " + sTheLog + ", boolean moveFilesToByteArrays) throws CSException");
        } else {
            theJavaChunk.print("public static " + cursorMethodFileName + "[] getArrayFromReadOnlyRowSet(com.mcpdbwizard.pub.ReadOnlyRowSet theRowSet, com.mcpdbwizard.pub.LogInterface " + sTheLog + ") throws CSException");
        }

        theJavaChunk.indent();
        theJavaChunk.print("{                                       ");
        theJavaChunk.print(cursorMethodFileName + "[] theArray = null;            ");
        theJavaChunk.print("                                        ");
        theJavaChunk.print("if (theRowSet.size() > 0)                ");
        theJavaChunk.indent();
        theJavaChunk.print("try                                        ");
        theJavaChunk.indent();
        theJavaChunk.print("{                                        ");
        theJavaChunk.print("theArray = new " + cursorMethodFileName + "[theRowSet.size()];  ");
        theJavaChunk.print("                                             ");
        theJavaChunk.print("for (int i=0; i < theRowSet.size(); i++)     ");
        theJavaChunk.indent();
        theJavaChunk.print("{ ");
        theJavaChunk.print("theRowSet.setCurrentRowNumber(i);");
        theJavaChunk.print("theArray[i] = new " + cursorMethodFileName + "();                ");
        theJavaChunk.print("Object[] theRow = theRowSet.getCurrentRow();");
        theJavaChunk.print(" ");

        for (int j = 0; j < qryCse.variableDataType.length; j++) {

            switch (qryCse.oracleUnderlyingDatatype[j]) {
                case SqlUtils.ORACLE_LONGTEXT_DATATYPE:
                case SqlUtils.ORACLE_LONG_BINARY_DATATYPE:
                case SqlUtils.ORACLE_CLOB_DATATYPE:
                case SqlUtils.ORACLE_BLOB_DATATYPE:
                case SqlUtils.ORACLE_BFILE_DATATYPE: {
                    theJavaChunk.print("");
                    if (comments)
                        theJavaChunk.print("// " + qryCse.variableName[j] + " can be null, a File, a LOB or an Exception");
                    theJavaChunk.print("if (theRow[" + j + "] == null)");
                    theJavaChunk.indent();
                    theJavaChunk.print("{");
                    theJavaChunk.print("theArray[i]." + qryCse.variableName[j] + " = null;");
                    theJavaChunk.print("}");
                    theJavaChunk.unIndent();
                    theJavaChunk.print("else if (theRow[" + j + "] instanceof java.io.File)");
                    theJavaChunk.indent();
                    theJavaChunk.print("{");
                    theJavaChunk.print("theArray[i]." + qryCse.variableName[j] + " = (java.io.File)theRow[" + j + "];");
                    theJavaChunk.print("}");
                    theJavaChunk.unIndent();
                    if (qryCse.actualOracleDatatype[j].equals("BLOB")) {
                        theJavaChunk.print("else if (theRow[" + j + "] instanceof oracle.sql.BLOB)");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");
                        theJavaChunk.print("theArray[i]." + qryCse.lobName[j] + " = (oracle.sql.BLOB)theRow[" + j + "];");
                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();
                    } else if (qryCse.actualOracleDatatype[j].equals("CLOB")) {
                        theJavaChunk.print("else if (theRow[" + j + "] instanceof oracle.sql.CLOB)");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");
                        theJavaChunk.print("theArray[i]." + qryCse.lobName[j] + " = (oracle.sql.CLOB)theRow[" + j + "];");
                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();
                    } else if (qryCse.actualOracleDatatype[j].equals("BFILE")) {
                        theJavaChunk.print("else if (theRow[" + j + "] instanceof oracle.sql.BFILE)");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");
                        theJavaChunk.print("theArray[i]." + qryCse.lobName[j] + " = (oracle.sql.BFILE)theRow[" + j + "];");
                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();
                    }
                    theJavaChunk.print("else if (theRow[" + j + "] instanceof Exception)");
                    theJavaChunk.indent();
                    theJavaChunk.print("{");
                    theJavaChunk.print("throw(new Exception(((Exception)theRow[" + j + "]).getMessage()));");
                    theJavaChunk.print("}");
                    theJavaChunk.unIndent();
                    theJavaChunk.print("else");
                    theJavaChunk.indent();
                    theJavaChunk.print("{");
                    if (comments)
                        theJavaChunk.print("// We shouldn't get to this line if the ReadOnlyRowSet passed in as a parameter ");
                    if (comments) theJavaChunk.print("// is the one the code expects. ");
                    theJavaChunk.print("throw(new Exception(\"Unrecognized data type in column " + j + ":\" + theArray[i].toString()));");
                    theJavaChunk.print("}");
                    theJavaChunk.unIndent();


                    if (webServices && qryCse.byteName[j] != null) {   //moveFilesToByteArrays
                        theJavaChunk.print(" ");
                        theJavaChunk.print("if (moveFilesToByteArrays)");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");
                        if (comments)
                            theJavaChunk.print("// Move file " + qryCse.variableName[j] + " to byte array " + qryCse.byteName[j]);
                        theJavaChunk.print("if (theArray[i]." + qryCse.variableName[j] + " != null && theArray[i]." + qryCse.variableName[j] + ".exists())");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");
                        if (qryCse.actualOracleDatatype[j].equals("CLOB")) {
                            theJavaChunk.print("theArray[i]." + qryCse.byteName[j] + " = com.mcpdbwizard.pub.IOUtils.loadFileIntoCharArray(theArray[i]." + qryCse.variableName[j] + ");");
                        } else {
                            theJavaChunk.print("theArray[i]." + qryCse.byteName[j] + " = com.mcpdbwizard.pub.IOUtils.loadFileIntoByteArray(theArray[i]." + qryCse.variableName[j] + ");");
                        }
                        theJavaChunk.print("theArray[i]." + qryCse.variableName[j] + ".delete();");
                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();
                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();
                        theJavaChunk.print(" ");
                    }

                    break;
                }
                case SqlUtils.ORACLE_TIMESTAMP_DATATYPE: {
                    theJavaChunk.print("theArray[i]." + qryCse.variableName[j] + " = ((oracle.sql.TIMESTAMP)theRow[" + j + "]).toString();");
                    break;
                }
                case SqlUtils.ORACLE_TIMESTAMPTZ_DATATYPE: {
                    theJavaChunk.print("theArray[i]." + qryCse.variableName[j] + " = ((oracle.sql.TIMESTAMPTZ)theRow[" + j + "]).toBytes();");
                    break;
                }
                case SqlUtils.ORACLE_TIMESTAMPLTZ_DATATYPE: {
                    theJavaChunk.print("theArray[i]." + qryCse.variableName[j] + " = ((oracle.sql.TIMESTAMPLTZ)theRow[" + j + "]).toBytes();");
                    break;
                }
                default: {
                    theJavaChunk.print("theArray[i]." + qryCse.variableName[j] + " = (" + qryCse.variableDataType[j] + ")theRow[" + j + "];");
                }
            }

        }

        theJavaChunk.print("}                ");
        theJavaChunk.unIndent();
        theJavaChunk.print("}                ");
        theJavaChunk.unIndent();
        theJavaChunk.print("catch (Exception e)                                        ");
        theJavaChunk.indent();
        theJavaChunk.print("{                                        ");
        theJavaChunk.print(sTheLog + ".error(\"Unable to turn ReadOnlyRowSet into array of " + cursorMethodFileName + ":\");");
        theJavaChunk.print(sTheLog + ".error(e);");
        theJavaChunk.print("throw new CSException(\"" + cursorMethodFileName + ".getArrayFromReadOnlyRowSet: Unable to turn ReadOnlyRowSet into array of " + cursorMethodFileName + ":\" + e.getMessage());");
        theJavaChunk.print("}                ");
        theJavaChunk.unIndent();
        theJavaChunk.print("                 ");
        theJavaChunk.print("                 ");
        theJavaChunk.print("return(theArray); ");
        theJavaChunk.print("} ");
        theJavaChunk.unIndent();

        theJavaChunk.print(" ");
        if (comments) {
            theJavaChunk.print("/**");
            theJavaChunk.print("* Execute query and return results as an array of " + cursorMethodFileName);
            if (webServices) {
                theJavaChunk.print("* @param boolean moveFilesToByteArrays Copies any files to matching byte arrays and then deletes them.");
            }
            theJavaChunk.print("* @return " + cursorMethodFileName + "[]");
            theJavaChunk.print("* @throws CSException");
            theJavaChunk.print("* @since V5.0.2192");
            theJavaChunk.print("*/");
        }

        if (webServices) {
            theJavaChunk.print("public " + cursorMethodFileName + "[] executeQueryArray(boolean moveFilesToByteArrays) throws CSException");
        } else {
            theJavaChunk.print("public " + cursorMethodFileName + "[] executeQueryArray() throws CSException");
        }

        theJavaChunk.indent();
        theJavaChunk.print("{                                       ");
        if (webServices) {
            theJavaChunk.print("return(" + realFilename + ".getArrayFromReadOnlyRowSet(executeQuery()," + sTheLog + ", moveFilesToByteArrays));");
        } else {
            theJavaChunk.print("return(" + realFilename + ".getArrayFromReadOnlyRowSet(executeQuery()," + sTheLog + "));");
        }
        theJavaChunk.print("} ");
        theJavaChunk.unIndent();
    }
}


