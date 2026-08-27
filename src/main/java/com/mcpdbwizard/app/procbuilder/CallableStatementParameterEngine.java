package com.mcpdbwizard.app.procbuilder;

import com.mcpdbwizard.mcpdbwizardconnector.BaseMethodRepresentation;
import com.mcpdbwizard.pub.*;
import com.mcpdbwizard.app.common.*;

import com.mcpdbwizard.app.procbuilder.gui.McpDbWizardEvent;
import com.mcpdbwizard.app.procbuilder.gui.McpDbWizardEventListener;

import oracle.jdbc.OracleTypes;

import java.util.Properties;
import java.util.ArrayList;

import java.sql.Connection;

/**
 * @author devteam@mcpdbwizard.com
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 * @version 2
 */
public class CallableStatementParameterEngine {

    public static final int IS_A_FUNCTION = 0;
    public static final int IS_A_RECORD = 1;
    public static final int RENAME_PARAMS = 0;
    public static final int FORMAT_PARAMS = 1;
    public static final int DONTTOUCH_PARAMS = 2;
    public final static String EVIL_PLSQL_TABLES_WARNING_1 = " uses a PL/SQL Array type which can not be accessed with JDBC";
    public final static String EVIL_PLSQL_TABLES_WARNING_2 = "Go to " + Namer.faq("plsql-index-by-tables") + " for information about what you should do.";
    public final static String JAVA_PARAM_LIMIT_URL = Namer.param_java_param_limit_url;
    public static final int WS_CALL_TYPE_ALL = 0;
    public static final int WS_CALL_TYPE_SET = 1;
    public static final int WS_CALL_TYPE_GET = 2;
    final int JAVA_VERSION_21 = 21;
    final int MAX_CHAR_SIZE = 2000;
    final int MAX_STRING_SIZE = 4000;
    public boolean isComplex = false;
    //public boolean isComplexAndNotJustForBooleans = false;
    public boolean isAFunction = false;
    public boolean hasFiles = false;
    public boolean hasChildFiles = false;
    public boolean defineTypes = false;
    public boolean hasRowSets = false;
    public boolean hasLongs = false;
    public boolean isBroken = false;
    public String isBrokenString = "";
    public String overRideInOut = null;
    // Object names
    public String sobjectLoader = "";
    public String sprocName = "";
    public String stheLog = "";
    // Method Names
    public String sGetProcCallStatement = "";
    public String sBindParams = "";
    public String sGetProcCall = "";
    public String sReleaseResources = "";
    public String sGetStatementResults = "";
    // Variable Names
    public String sbufferSize = "";
    public String skeepFiles = "";
    public String skeepLobs = "";
    public String stempFileDir = "";
    public String stempFilePrefix = "";
    public String stempFileSuffix = "";
    /**
     * The configured (propfile) values emitted as the default temp-file prefix/suffix
     * in generated callable-statement wrappers. Default to the shared sane constants so
     * generated code never ships the raw PARAM_* template tokens as runtime filenames.
     */
    public String configTempFilePrefix = com.mcpdbwizard.pub.IOUtils.DEFAULT_TEMP_FILE_PREFIX;
    public String configTempFileSuffix = com.mcpdbwizard.pub.IOUtils.DEFAULT_TEMP_FILE_SUFFIX;
    public String smaxRows = "";
    public String sfunctionResult = "";
    // Variable Set Methods
    public String sSetBufferSize = "";
    public String sSetKeepFiles = "";
    public String sSetKeepLobs = "";
    public String sDelFilesMethod = "";
    public String sSetTempFileDir = "";
    public String sSetTempFilePrefix = "";
    public String sSetTempFileSuffix = "";
    public String sSetMaxRows = "";
    // Variable Get Methods
    public String sGetBufferSize = "";
    public String sGetKeepFiles = "";
    public String sGetKeepLobs = "";
    public String sGetTempFileDir = "";
    public String sGetTempFilePrefix = "";
    public String sGetTempFileSuffix = "";
    public String sGetMaxRows = "";

    // S varibles contain names of methods and variables that are used
    // for prosessing files
    public String sWsdlFileName = "";
    public String sWsdlRowFileName = "";
    public String sCastMethodName = "";
    public String sWsdlElementName = "";
    public boolean evilPlsqlTablesWarningGiven = false;
    public boolean needBfileCreationRoutine = false;
    int javaVersion = 21;
    boolean makeOwnTestCases = false;
    boolean arraysSupported = true;
    boolean needsPlsqlIndexByArray = false;
    Properties numberDataTypes = null;
    ReadOnlyRowSet theRowSet = null;
    LogInterface theLog = null;
    String[] variableName = null;
    String[] variableDataType = null;
    String[] variableWSDataType = null;
    String[] variableWSParentDataType = null;
    int[] oracleUnderlyingDatatype = null;
    String[] oracleParamDatatype = null;
    String javaNamingConvention = null;
    DatatypeWrangler theDatatypeWrangler = null;
    String oracleName = null;
    String[] otherName = null;
    String[] lobName = null;
    String[] byteName = null;
    String[] actualOracleDatatype = null;
    boolean[] useVariable = null;
    boolean[] isAFile = null;
    int[] paramInId = null;
    int[] paramOutId = null;
    int inParamCount = 0;
    int outParamCount = 0;
    int outArrayCount = 0;
    int[] extraObjectId = null;
    PlsqlRecordObject[] theRecords = null;
    // plsql index by stuff...
    String[] PlsqlIndexByTableName = null;
    int[] plsqlIndexByDataType = null;
    int[] plsqlIndexByRealDataType = null;
    int[] plsqlIndexByDataLength = null;
    int[] plsqlIndexByDataDecPlaces = null;
    String[] plsqlIndexByPlaceHolderVarType = null;
    String[] plsqlIndexByPlaceHolderVarName = null;
    int[] cursorRecordId = null;
    //final String UND_BIG_DEC = "_big_dec";
    //final String UND_BIG_DEC = "";
    int type;
    String targetVersion = "";
    String loginVersion = "";
    Connection theConnection = null;
    GlobalFlags theFlags = null;
    String wsJavaNumberTypeComboBox = "";
    String loginName = "";

    String[] plsqlArrayAssigns = null;
    String[] plsqlArrayWeirdAssigns = null;
    String[] plsqlArrayUnassigns = null;

    String generatedClassName = null;
    McpDbWizardEventListener listener = null;
    boolean useCharForCLOB = true;

    BaseMethodRepresentation metadata = new BaseMethodRepresentation();

    public CallableStatementParameterEngine(ReadOnlyRowSet theRowSet, String javaNamingConvention
            , LogInterface theLog, DatatypeWrangler theDatatypeWrangler
            , String oracleName
            , PlsqlRecordObject[] theRecords, String overRideInOut
            , String oracleNameBasis, int type
            , String targetVersion
            , String loginVersion
            , int renameParams, Properties numberDataTypes
            , Connection theConnection
            , String realName, String realOwner
            , boolean arraysSupported
            , GlobalFlags theFlags
            , String wsJavaNumberTypeComboBox
            , String loginName
            , String[] plsqlArrayAssigns
            , String[] plsqlArrayWeirdAssigns
            , String[] plsqlArrayUnassigns
            , String generatedClassName
            , McpDbWizardEventListener listener
    ) {

        this.listener = listener;

        if (generatedClassName != null) {
            this.generatedClassName = new String(generatedClassName);
        }

        this.loginName = loginName;
        this.wsJavaNumberTypeComboBox = wsJavaNumberTypeComboBox;
        this.theFlags = theFlags;
        this.theConnection = theConnection;
        this.type = type;
        this.targetVersion = targetVersion;
        this.loginVersion = loginVersion;
        this.theRowSet = theRowSet;

        if (theRowSet.size() == 0) {
            //theLog.info(realName + " " + realOwner);
        }
        this.theLog = theLog;
        this.javaNamingConvention = javaNamingConvention;
        this.theDatatypeWrangler = theDatatypeWrangler;
        this.theRecords = theRecords;
        this.overRideInOut = overRideInOut;
        this.arraysSupported = arraysSupported;
        this.plsqlArrayAssigns = plsqlArrayAssigns;
        this.plsqlArrayWeirdAssigns = plsqlArrayWeirdAssigns;


        this.plsqlArrayUnassigns = plsqlArrayUnassigns;

        if (this.overRideInOut == null) {
            this.overRideInOut = "";
        }

        this.numberDataTypes = numberDataTypes;

        variableName = new String[theRowSet.size()];
        variableDataType = new String[theRowSet.size()];
        variableWSDataType = new String[theRowSet.size()];
        variableWSParentDataType = new String[theRowSet.size()];
        oracleUnderlyingDatatype = new int[theRowSet.size()];
        oracleParamDatatype = new String[theRowSet.size()];
        this.oracleName = oracleName;
        otherName = new String[theRowSet.size()];
        byteName = new String[theRowSet.size()];
        lobName = new String[theRowSet.size()];
        actualOracleDatatype = new String[theRowSet.size()];
        useVariable = new boolean[theRowSet.size()];

        paramInId = new int[theRowSet.size()];
        paramOutId = new int[theRowSet.size()];

        extraObjectId = new int[theRowSet.size()];

        PlsqlIndexByTableName = new String[theRowSet.size()];
        plsqlIndexByDataType = new int[theRowSet.size()];
        plsqlIndexByRealDataType = new int[theRowSet.size()];
        plsqlIndexByDataLength = new int[theRowSet.size()];
        plsqlIndexByDataDecPlaces = new int[theRowSet.size()];
        cursorRecordId = new int[theRowSet.size()];
        plsqlIndexByPlaceHolderVarType = new String[theRowSet.size()];
        plsqlIndexByPlaceHolderVarName = new String[theRowSet.size()];

        setComplexFlag();

        int recordInParams = 0;
        int recordOutParams = 0;

        int latestInParam = 0;
        int latestOutParam = 0;


        sprocName = JavaUtils.getJavaName("proc_name", javaNamingConvention, theLog);
        sobjectLoader = JavaUtils.getJavaName("object_loader", javaNamingConvention, theLog);
        stheLog = JavaUtils.getJavaName("the_log", javaNamingConvention, theLog);

        sGetProcCallStatement = JavaUtils.getJavaName("get_proc_call_statement", javaNamingConvention, theLog);
        sDelFilesMethod = JavaUtils.getJavaName("delete_generated_files", javaNamingConvention, theLog);
        sBindParams = JavaUtils.getJavaName("bind_params", javaNamingConvention, theLog);
        sReleaseResources = JavaUtils.getJavaName("release_resources", javaNamingConvention, theLog);
        sGetStatementResults = JavaUtils.getJavaName("get_statement_results", javaNamingConvention, theLog);

        sbufferSize = JavaUtils.getJavaName("buffer_size", javaNamingConvention, theLog);
        skeepFiles = JavaUtils.getJavaName("keep_files", javaNamingConvention, theLog);
        skeepLobs = JavaUtils.getJavaName("keep_lobs", javaNamingConvention, theLog);
        stempFileDir = JavaUtils.getJavaName("temp_file_dir", javaNamingConvention, theLog);
        stempFilePrefix = JavaUtils.getJavaName("temp_file_prefix", javaNamingConvention, theLog);
        stempFileSuffix = JavaUtils.getJavaName("temp_file_suffix", javaNamingConvention, theLog);
        smaxRows = JavaUtils.getJavaName("max_rows", javaNamingConvention, theLog);
        sfunctionResult = JavaUtils.getJavaName("function_result", javaNamingConvention, theLog);

        sSetBufferSize = JavaUtils.getJavaName("set_buffer_size", javaNamingConvention, theLog);
        sSetKeepFiles = JavaUtils.getJavaName("set_keep_files", javaNamingConvention, theLog);
        sSetKeepLobs = JavaUtils.getJavaName("set_keep_lobs", javaNamingConvention, theLog);
        sSetTempFileDir = JavaUtils.getJavaName("set_temp_dir", javaNamingConvention, theLog);
        sSetTempFilePrefix = JavaUtils.getJavaName("set_temp_file_prefix", javaNamingConvention, theLog);
        sSetTempFileSuffix = JavaUtils.getJavaName("set_temp_file_suffix", javaNamingConvention, theLog);
        sSetMaxRows = JavaUtils.getJavaName("set_max_rows", javaNamingConvention, theLog);

        sGetBufferSize = JavaUtils.getJavaName("get_buffer_size", javaNamingConvention, theLog);
        sGetKeepFiles = JavaUtils.getJavaName("get_keep_files", javaNamingConvention, theLog);
        sGetKeepLobs = JavaUtils.getJavaName("get_keep_lobs", javaNamingConvention, theLog);
        sGetTempFileDir = JavaUtils.getJavaName("get_temp_file_dir", javaNamingConvention, theLog);
        sGetTempFilePrefix = JavaUtils.getJavaName("get_temp_file_prefix", javaNamingConvention, theLog);
        sGetTempFileSuffix = JavaUtils.getJavaName("get_temp_file_suffix", javaNamingConvention, theLog);
        sGetMaxRows = JavaUtils.getJavaName("get_max_rows", javaNamingConvention, theLog);

        sWsdlFileName = JavaUtils.getJavaName("Wsdl_row_set", javaNamingConvention, theLog);
        sWsdlRowFileName = JavaUtils.getJavaName("Wsdl_row", javaNamingConvention, theLog);
        sWsdlElementName = JavaUtils.getJavaName("Wsdl_element", javaNamingConvention, theLog);

        for (int i = 0; i < theRowSet.size(); i++) {

            paramInId[i] = -1;
            paramOutId[i] = -1;
            extraObjectId[i] = -1;

            PlsqlIndexByTableName[i] = "";
            plsqlIndexByDataType[i] = -1;
            plsqlIndexByRealDataType[i] = -1;
            plsqlIndexByDataLength[i] = -1;
            plsqlIndexByDataDecPlaces[i] = Integer.MIN_VALUE;
            cursorRecordId[i] = Integer.MIN_VALUE;

            plsqlIndexByPlaceHolderVarType[i] = "";
            plsqlIndexByPlaceHolderVarName[i] = "";

            try {
                theRowSet.setCurrentRowNumber(i);

                if (isComplex) {
                    if (theRowSet.getString("IN_OUT").equals("IN")
                            || theRowSet.getString("IN_OUT").equals("IN/OUT")
                        //  || this.overRideInOut.equals("IN/OUT")
                    ) {
                        paramInId[i] = recordInParams + ++latestInParam;
                        inParamCount++;
                    }

                    if (theRowSet.getString("IN_OUT").equals("OUT")
                            || theRowSet.getString("IN_OUT").equals("IN/OUT")
                        //    || this.overRideInOut.equals("IN/OUT")
                    ) {
                        paramOutId[i] = recordOutParams + ++latestOutParam;
                    }
                } else {
                    latestInParam++;
                    latestOutParam++;

                    // This counter is incremented even if the param is an OUT param.
                    // Don't mess with it.
                    inParamCount++;

                    paramInId[i] = latestInParam;
                    paramOutId[i] = latestOutParam;
                }

                String argName = theRowSet.getString("ARGUMENT_NAME");
                String argOverload = theRowSet.getString("OVERLOAD");

                if (argName == null) {
                    argName = "arg_" + i;
                }

                if (argOverload == null) {
                    argOverload = "";
                }

                try {
                    oracleParamDatatype[i] = theDatatypeWrangler.getOracletypeCode(theRowSet.getString("DATA_TYPE"), theLog);
                    actualOracleDatatype[i] = theRowSet.getString("DATA_TYPE");
                    // NCLOB rides the CLOB path end to end: oracle.sql.NCLOB extends
                    // oracle.sql.CLOB, the temporary-LOB factory and getParam both yield a
                    // CLOB, and LongObjectLoader only has loadCLOB. Normalising the actual
                    // datatype to CLOB here keeps every downstream "oracle.sql.<TYPE>" field
                    // declaration, cast and web-service setter consistent (otherwise the
                    // field is typed oracle.sql.NCLOB but assigned a CLOB).
                    if ("NCLOB".equals(actualOracleDatatype[i])) {
                        actualOracleDatatype[i] = "CLOB";
                    }
                } catch (CSException e) {
                    theLog.error(theRowSet.getString("OBJECT_NAME")
                            + " has unsupported datatype for parameter "
                            + argName + " of "
                            + theRowSet.getString("DATA_TYPE") + " " + theRowSet.getString("TYPE_NAME"));
                    variableDataType[i] = "Object";
                    isBroken = true;
                    isBrokenString = "//";

                }

                // Keep track out array out params. If all out params are array out params we don't
                // need OracleTypes in import as our bind code uses methods that don't refer
                // to OracleTypes
                if (theRowSet.getString("IN_OUT").equals("OUT")
                        || theRowSet.getString("IN_OUT").equals("IN/OUT")) {
                    outParamCount++;

                    if (oracleParamDatatype[i].equals("OracleTypes.PLSQL_INDEX_TABLE")
                            || oracleParamDatatype[i].equals("SqlUtils.ORACLE_TABLE_DATATYPE")
                            || oracleParamDatatype[i].equals("SqlUtils.ORACLE_VARRAY_DATATYPE")
                            || oracleParamDatatype[i].equals("SqlUtils.ORACLE_PLSQL_INDEXBY_ROWTYPE_DATATYPE")) {
                        outArrayCount++;
                    }
                }

                //System.out.println(argName + " b4");
                if (theRowSet.getInt("POSITION") == 0) {
                    variableName[i] = sfunctionResult;
                } else {
                    if (renameParams == RENAME_PARAMS) {
                        variableName[i] = JavaUtils.getJavaName("param_" + argName.toLowerCase()
                                , javaNamingConvention, theLog);
                    } else if (renameParams == FORMAT_PARAMS) {
                        variableName[i] = JavaUtils.getJavaName(argName.toLowerCase()
                                , javaNamingConvention, theLog);
                    } else {
                        variableName[i] = argName;
                    }
                }
                //System.out.println(variableName[i] + " a4");

                useVariable[i] = true;

                // create variable definition...
                switch (oracleUnderlyingDatatype[i]) {
                    case SqlUtils.ORACLE_TEXT_DATATYPE: {
                        variableDataType[i] = "String";
                        break;
                    }
                    case SqlUtils.ORACLE_XMLTYPE_DATATYPE: //DRKLUGE
                    {
                        //variableDataType[i] = "oracle.sql.OPAQUE";
                        variableWSDataType[i] = "char[]";
                        variableDataType[i] = "java.io.File";
                        if (theRowSet.getInt("POSITION") == 0) {
                            otherName[i] = JavaUtils.getJavaName("filename_" + "function_result"
                                    , javaNamingConvention, theLog);
                            lobName[i] = JavaUtils.getJavaName("lob_" + "function_result"
                                    , javaNamingConvention, theLog);
                            byteName[i] = JavaUtils.getJavaName("byte_array_" + "function_result"
                                    , javaNamingConvention, theLog);
                        } else {
                            otherName[i] = JavaUtils.getJavaName("filename_" + argName.toLowerCase()
                                    , javaNamingConvention, theLog);
                            lobName[i] = JavaUtils.getJavaName("lob_" + argName.toLowerCase()
                                    , javaNamingConvention, theLog);
                            byteName[i] = JavaUtils.getJavaName("byte_array_" + argName.toLowerCase()
                                    , javaNamingConvention, theLog);
                        }

                        hasFiles = true;
                        theFlags.setFlag(GlobalFlags.HAS_FILES);
                        theFlags.setFlag(GlobalFlags.XMLTYPE_IN_USE);
                        theFlags.setFlag(GlobalFlags.XMLJAR_NEEDED);

                        break;
                    }
                    case SqlUtils.ORACLE_NUMBER_DATATYPE: {
                        variableDataType[i] = "java.math.BigDecimal";
                        variableWSDataType[i] = wsJavaNumberTypeComboBox;
                        break;
                    }
                    case SqlUtils.ORACLE_DATE_DATATYPE: {
                        variableDataType[i] = "java.util.Date";
                        break;
                    }
                    case SqlUtils.ORACLE_SDO_GEOMETRY_DATATYPE: {             //NEWORA
                        //variableDataType[i] = "oracle.spatial.geometry.JGeometry";
                        variableDataType[i] = "JGeometryWrapper";
                        //variableWSDataType[i] = "oracle.spatial.geometry.JGeometry";
                        variableWSDataType[i] = "JGeometryWrapper";      //JGeometryWrapper
                        theFlags.setFlag(GlobalFlags.SDO_IN_USE);
                        theFlags.setFlag(GlobalFlags.SDOJAR_NEEDED);
                        break;
                    }
                    case SqlUtils.ORACLE_JSON_DATATYPE: {
                        // Native binary (OSON) JSON (21c+).
                        variableDataType[i] = "oracle.sql.json.OracleJsonValue";
                        variableWSDataType[i] = "oracle.sql.json.OracleJsonValue";
                        break;
                    }
                    case SqlUtils.ORACLE_VECTOR_DATATYPE: {
                        // Native VECTOR (23ai), one double per dimension.
                        variableDataType[i] = "double[]";
                        variableWSDataType[i] = "double[]";
                        break;
                    }
                    case SqlUtils.ORACLE_VECTOR_BINARY_DATATYPE: {
                        // Binary (bit-packed) VECTOR (23ai): n bits = n/8 bytes.
                        variableDataType[i] = "byte[]";
                        variableWSDataType[i] = "byte[]";
                        break;
                    }
                    case SqlUtils.ORACLE_VECTOR_SPARSE_DATATYPE: {
                        // Sparse VECTOR (23ai): {length, indices, values}, not densified.
                        variableDataType[i] = "com.mcpdbwizard.pub.SparseVector";
                        variableWSDataType[i] = "com.mcpdbwizard.pub.SparseVector";
                        break;
                    }
                    case SqlUtils.ORACLE_NATIVE_BOOLEAN_DATATYPE: {
                        // Native ISO-SQL BOOLEAN (23ai).
                        variableDataType[i] = "Boolean";
                        variableWSDataType[i] = "boolean";
                        break;
                    }
                    case SqlUtils.ORACLE_LONGTEXT_DATATYPE:
                    case SqlUtils.ORACLE_LONG_BINARY_DATATYPE: {
                        variableDataType[i] = "java.io.File";

                        if (theRowSet.getInt("POSITION") == 0) {
                            otherName[i] = JavaUtils.getJavaName("filename_" + "function_result"
                                    , javaNamingConvention, theLog);
                            byteName[i] = JavaUtils.getJavaName("byte_array_" + "function_result"
                                    , javaNamingConvention, theLog);
                        } else {
                            otherName[i] = JavaUtils.getJavaName("filename_" + argName.toLowerCase()
                                    , javaNamingConvention, theLog);
                            byteName[i] = JavaUtils.getJavaName("byte_array_" + argName.toLowerCase()
                                    , javaNamingConvention, theLog);
                        }

                        hasFiles = true;
                        hasLongs = true;

                        theFlags.setFlag(GlobalFlags.HAS_FILES);

                        break;
                    }
                    case SqlUtils.ORACLE_CLOB_DATATYPE: {
                        variableDataType[i] = "java.io.File";
                        if (useCharForCLOB) variableWSDataType[i] = "char[]";

                        if (theRowSet.getInt("POSITION") == 0) {
                            otherName[i] = JavaUtils.getJavaName("filename_" + "function_result"
                                    , javaNamingConvention, theLog);
                            lobName[i] = JavaUtils.getJavaName("lob_" + "function_result"
                                    , javaNamingConvention, theLog);
                            byteName[i] = JavaUtils.getJavaName("byte_array_" + "function_result"
                                    , javaNamingConvention, theLog);
                        } else {
                            otherName[i] = JavaUtils.getJavaName("filename_" + argName.toLowerCase()
                                    , javaNamingConvention, theLog);
                            lobName[i] = JavaUtils.getJavaName("lob_" + argName.toLowerCase()
                                    , javaNamingConvention, theLog);
                            byteName[i] = JavaUtils.getJavaName("byte_array_" + argName.toLowerCase()       //DRKLUGE
                                    , javaNamingConvention, theLog);
                        }

                        hasFiles = true;
                        theFlags.setFlag(GlobalFlags.HAS_FILES);
                        break;
                    }
                    case SqlUtils.ORACLE_BLOB_DATATYPE: {
                        variableDataType[i] = "java.io.File";

                        if (theRowSet.getInt("POSITION") == 0) {
                            otherName[i] = JavaUtils.getJavaName("filename_" + "function_result"
                                    , javaNamingConvention, theLog);
                            lobName[i] = JavaUtils.getJavaName("lob_" + "function_result"
                                    , javaNamingConvention, theLog);
                            byteName[i] = JavaUtils.getJavaName("byte_array_" + "function_result"
                                    , javaNamingConvention, theLog);
                        } else {
                            otherName[i] = JavaUtils.getJavaName("filename_" + argName.toLowerCase()
                                    , javaNamingConvention, theLog);
                            lobName[i] = JavaUtils.getJavaName("lob_" + argName.toLowerCase()
                                    , javaNamingConvention, theLog);
                            byteName[i] = JavaUtils.getJavaName("byte_array_" + argName.toLowerCase()
                                    , javaNamingConvention, theLog);
                        }

                        hasFiles = true;
                        theFlags.setFlag(GlobalFlags.HAS_FILES);
                        break;
                    }
                    case SqlUtils.ORACLE_BFILE_DATATYPE: {
                        variableDataType[i] = "java.io.File";

                        if (theRowSet.getInt("POSITION") == 0) {
                            otherName[i] = JavaUtils.getJavaName("filename_" + "function_result"
                                    , javaNamingConvention, theLog);
                            lobName[i] = JavaUtils.getJavaName("lob_" + "function_result"
                                    , javaNamingConvention, theLog);
                            byteName[i] = JavaUtils.getJavaName("byte_array_" + "function_result"
                                    , javaNamingConvention, theLog);
                        } else {
                            otherName[i] = JavaUtils.getJavaName("filename_" + argName.toLowerCase()
                                    , javaNamingConvention, theLog);
                            lobName[i] = JavaUtils.getJavaName("lob_" + argName.toLowerCase()
                                    , javaNamingConvention, theLog);
                            byteName[i] = JavaUtils.getJavaName("byte_array_" + argName.toLowerCase()
                                    , javaNamingConvention, theLog);
                        }

                        hasFiles = true;

                        theFlags.setFlag(GlobalFlags.HAS_FILES);
                        theFlags.setFlag(GlobalFlags.HAS_BFILES);

                        if (theRowSet.getString("IN_OUT").equals("IN")
                                && (!this.overRideInOut.equals("IN/OUT"))
                        ) {
                            // BFILE's can not be modified. Therefore we shouldnt have an in file.
                            useVariable[i] = false;
                        }

                        break;
                    }

                    case SqlUtils.ORACLE_NULL_DATATYPE: {
                        variableDataType[i] = "Object";
                        break;
                    }
                    case SqlUtils.ORACLE_BOOLEAN_DATATYPE: {
                        variableDataType[i] = "Boolean";
                        variableWSDataType[i] = "boolean";
                        break;
                    }
                    case SqlUtils.ORACLE_ROWID_DATATYPE: {
                        variableDataType[i] = "oracle.sql.ROWID";
                        variableWSDataType[i] = "String";
                        break;
                    }
                    case SqlUtils.ORACLE_INTERVAL_DAY_TO_SECOND_DATATYPE: {
                        variableDataType[i] = "String";
                        break;
                    }
                    case SqlUtils.ORACLE_INTERVAL_YEAR_TO_MONTH_DATATYPE: {
                        variableDataType[i] = "String";
                        break;
                    }
                    case SqlUtils.ORACLE_UROWID_DATATYPE: {
                        variableDataType[i] = "String";
                        break;
                    }
                    case SqlUtils.ORACLE_BINARY_DATATYPE: {
                        variableDataType[i] = "byte[]";
                        break;
                    }
                    case SqlUtils.ORACLE_TIMESTAMP_DATATYPE: {
                        variableDataType[i] = "String";
                        break;
                    }
                    case SqlUtils.ORACLE_TIMESTAMPTZ_DATATYPE: {
                        variableDataType[i] = "byte[]";
                        break;
                    }
                    case SqlUtils.ORACLE_TIMESTAMPLTZ_DATATYPE: {
                        variableDataType[i] = "byte[]";
                        theFlags.setFlag(GlobalFlags.HAS_TZLTS);
                        break;
                    }
                    case SqlUtils.MCPDBWIZARD_READONLYROWSET: {
                        variableDataType[i] = "com.mcpdbwizard.pub.ReadOnlyRowSet";
                        hasRowSets = true;
                        hasFiles = true;
                        theFlags.setFlag(GlobalFlags.HAS_FILES);
                        theFlags.setFlag(GlobalFlags.HAS_ROWSETS);

                        //theLog.info("Seeing if Cursor has defined columns");
                        try {
                            cursorRecordId[i] = getCursorRecordType(theRowSet.getString("OWNER")
                                    , theRowSet.getString("OBJECT_NAME")
                                    , theRowSet.getString("PACKAGE_NAME")
                                    , theRowSet.getString("OVERLOAD")
                                    , theRowSet.getInt("SEQUENCE")
                                    , theRowSet.getInt("DATA_LEVEL"));

                            variableWSDataType[i] = theRecords[cursorRecordId[i]].javaAttrName;
                            variableWSParentDataType[i] = theRecords[cursorRecordId[i]].fixedJavaName;
                        } catch (NullPointerException e) {
                            cursorRecordId[i] = Integer.MIN_VALUE;
                            variableWSDataType[i] = sWsdlFileName;
                        }

                        break;
                    }
                    case SqlUtils.ORACLE_OBJECT_DATATYPE:
                    case SqlUtils.ORACLE_VARRAY_DATATYPE:
                    case SqlUtils.ORACLE_ROWTYPE_DATATYPE:
                    case SqlUtils.ORACLE_TABLE_DATATYPE:
                    case SqlUtils.ORACLE_PLSQL_INDEXBY_ROWTYPE_DATATYPE:
                        //default:
                    {

                        // Flag use of custom data type

                        if (oracleUnderlyingDatatype[i] == SqlUtils.ORACLE_OBJECT_DATATYPE) {
                            theFlags.setFlag(GlobalFlags.ARRAYS_OBJ_IN_USE);
                            hasFiles = true;  //DRKLUGE
                            theFlags.setFlag(GlobalFlags.HAS_FILES); //DRKLUGE
                        } else if (oracleUnderlyingDatatype[i] == SqlUtils.ORACLE_VARRAY_DATATYPE) {
                            theFlags.setFlag(GlobalFlags.ARRAYS_PCK_IN_USE);
                        } else if (oracleUnderlyingDatatype[i] == SqlUtils.ORACLE_TABLE_DATATYPE) {
                            theFlags.setFlag(GlobalFlags.ARRAYS_PCK_IN_USE);
                        } else if (oracleUnderlyingDatatype[i] == SqlUtils.ORACLE_PLSQL_INDEXBY_ROWTYPE_DATATYPE) {
                            theFlags.setFlag(GlobalFlags.ARRAYS_IDX_IN_USE);
                        } else if (oracleUnderlyingDatatype[i] == SqlUtils.ORACLE_ROWTYPE_DATATYPE) {
                            theFlags.setFlag(GlobalFlags.ROWTYPE_IN_USE);
                        }

                        boolean doBreak = true;

                        // See if we created a class for this data type
                        try {
                            // step 1 -- iterate through theRecords looking for our proc
                            int ourProcId = Integer.MIN_VALUE;

                            //String a2 = theRowSet.getString("OWNER");

                            if (oracleNameBasis.equals("Public Synonym")
                                    || oracleNameBasis.equals("Private Synonym")) {
                                // synonym

                                for (int q = 0; q < theRecords.length; q++) {
                                    String packageName = theRowSet.getString("PACKAGE_NAME");
                                    String objectName = theRowSet.getString("OBJECT_NAME");

                                    if (loginVersion.equals("9.0.1") && objectName.equalsIgnoreCase(packageName)) {
                                        // In 9.0.1 ALL_ARGUMENTS puts the object name into the package name field.
                                        // remove it so our stuff works...
                                        packageName = null;
                                    }

                                    if (theRecords[q].procOwner.equals(theRowSet.getString("OWNER"))
                                            && theRecords[q].procObjectName.equals(theRowSet.getString("OBJECT_NAME"))
                                            && (theRecords[q].procArgName.equals(theRowSet.getString("ARGUMENT_NAME"))
                                            || (theRowSet.getString("ARGUMENT_NAME") == null && theRecords[q].procArgName == null)
                                    )
                                    ) {
                                        // See if package is same...
                                        if ((theRecords[q].procPackageName == null
                                                && packageName == null)
                                                || (theRecords[q].procPackageName != null
                                                && theRecords[q].procPackageName.equals(packageName))) {
                                            // See if overload is same...
                                            if ((theRecords[q].procOverload == null
                                                    && theRowSet.getString("OVERLOAD") == null)
                                                    || (theRecords[q].procOverload != null
                                                    && theRecords[q].procOverload.equals(theRowSet.getString("OVERLOAD")))) {
                                                ourProcId = q;
                                                break;
                                            }
                                        }
                                    }

                                }

                            } else {
                                // user or other users object
                                for (int q = 0; q < theRecords.length; q++) {
                                    String packageName = theRowSet.getString("PACKAGE_NAME");
                                    String objectName = theRowSet.getString("OBJECT_NAME");
                                    if (loginVersion.equals("9.0.1") && objectName.equalsIgnoreCase(packageName)) {
                                        // In 9.0.1 ALL_ARGUMENTS puts the object name into the package name field.
                                        // remove it so our stuff works...
                                        packageName = null;
                                    }

                                    //String a1a = theRowSet.getRowAsString(":","?");
                                    //String a1 = theRecords[q].procOwner;
                                    //String a3 = theRecords[q].realOwner;
                                    //String a4 = theRowSet.getString("PACKAGE_NAME");
                                    if (theRecords[q].procOwner.equals(theRowSet.getString("OWNER"))
                                            && theRecords[q].procObjectName.equals(theRowSet.getString("OBJECT_NAME"))
                                            && (theRecords[q].procArgName.equals(theRowSet.getString("ARGUMENT_NAME"))
                                            || (theRowSet.getString("ARGUMENT_NAME") == null && theRecords[q].procArgName == null)
                                    )
                                    ) {
                                        // See if package is same...
                                        if ((theRecords[q].procPackageName == null
                                                && packageName == null)
                                                || (theRecords[q].procPackageName != null
                                                && theRecords[q].procPackageName.equals(packageName))) {
                                            // See if overload is same...
                                            if ((theRecords[q].procOverload == null
                                                    && theRowSet.getString("OVERLOAD") == null)
                                                    || (theRecords[q].procOverload != null
                                                    && theRecords[q].procOverload.equals(theRowSet.getString("OVERLOAD")))) {
                                                ourProcId = q;
                                                break;
                                            }
                                        }
                                    }

                                }

                            }

                            // Item 7 (nested records): a record field synthesised from
                            // ALL_PLSQL_TYPE_ATTRS (23ai, where the nested field is not a walked
                            // ALL_ARGUMENTS row) has no positional theRecords entry to match on. Fall
                            // back to matching by the field's PL/SQL type identity
                            // (TYPE_OWNER/TYPE_NAME/TYPE_SUBNAME) against a record of the SAME type -
                            // semantically the same generated class. Only a genuine record/object-typed
                            // field carries all three, so scalars and every positional match above are
                            // unaffected; this only rescues cases that would otherwise throw below.
                            if (ourProcId == Integer.MIN_VALUE
                                    && theRowSet.getString("TYPE_OWNER") != null
                                    && theRowSet.getString("TYPE_NAME") != null
                                    && theRowSet.getString("TYPE_SUBNAME") != null) {
                                for (int q = 0; q < theRecords.length; q++) {
                                    if (theRowSet.getString("TYPE_OWNER").equals(theRecords[q].typeOwner)
                                            && theRowSet.getString("TYPE_NAME").equals(theRecords[q].typeName)
                                            && theRowSet.getString("TYPE_SUBNAME").equals(theRecords[q].typeSubName)) {
                                        ourProcId = q;
                                        break;
                                    }
                                }
                            }

                            if (ourProcId == Integer.MIN_VALUE) {
                                theLog.error(theRowSet.toString());

                                String evilThing = argName + " of ";

                                if (theRowSet.getString("PACKAGE_NAME") != null) {
                                    evilThing = evilThing + theRowSet.getString("PACKAGE_NAME") + ".";
                                }

                                evilThing = evilThing + theRowSet.getString("OBJECT_NAME");
                                if (theRowSet.getString("DATA_TYPE").startsWith("PL")) {
                                    if (!evilPlsqlTablesWarningGiven) {
                                        theLog.warning(evilThing + EVIL_PLSQL_TABLES_WARNING_1, true, true);
                                        theLog.info(EVIL_PLSQL_TABLES_WARNING_2, true, true);
                                        evilPlsqlTablesWarningGiven = true;
                                    } else {
                                        theLog.warning(evilThing + theRowSet.getString("OBJECT_NAME") + EVIL_PLSQL_TABLES_WARNING_1);
                                        theLog.info(EVIL_PLSQL_TABLES_WARNING_2);
                                    }
                                }

                                throw new CSException();
                            } else if (ourProcId > Integer.MIN_VALUE
                                    && theRecords[ourProcId].dataType.equals("PL/SQL TABLE")
                                    && theRecords[ourProcId].typeImplementingClass == null) {
                                // See if might be a scalar array
                                //This is totally insane, but needed.
                                oracleUnderlyingDatatype[i] = SqlUtils.ORACLE_PLSQL_INDEXBY_DATATYPE;
                                oracleParamDatatype[i] = "OracleTypes.PLSQL_INDEX_TABLE";
                                //OracleTypes.PLSQL_INDEX_TABLE
                                //oracleParamDatatype[i] = "OracleTypes.PLSQL_INDEX_TABLE";
                                doBreak = false;


                            } else {
                                extraObjectId[i] = ourProcId;

                                if (theRecords[ourProcId].hasFiles) {
                                    hasFiles = true;
                                }

                                // Item 7: use the RECURSIVE leaf count, not the direct fieldCount, so a
                                // record with nested-record fields adds the right number of extra bind
                                // positions (a nested record flattens to its own leaves). Equal to
                                // fieldCount for a flat record (byte-identical numbering there).
                                int recLeafCount = recursiveLeafCount(theRecords[ourProcId]);
                                if (theRowSet.getString("IN_OUT").equals("IN")
                                        || theRowSet.getString("IN_OUT").equals("IN/OUT")
                                    //  || overRideInOut.equals("IN/OUT")
                                ) {
                                    //recordOutParams = recordOutParams + (recLeafCount -1);
                                    if (recLeafCount <= 1) {
                                        //recordOutParams++;
                                    } else {
                                        recordInParams = recordInParams + (recLeafCount - 1);
                                    }
                                }

                                if (theRowSet.getString("IN_OUT").equals("OUT")
                                        || theRowSet.getString("IN_OUT").equals("IN/OUT")
                                    //  || overRideInOut.equals("IN/OUT")
                                ) {
                                    //recordOutParams = recordOutParams + (recLeafCount -1);
                                    if (recLeafCount <= 1) {
                                        //recordOutParams++;
                                    } else {
                                        recordOutParams = recordOutParams + (recLeafCount - 1);
                                    }

                                }

                                if (theRecords[ourProcId].replacedByArrayId > Integer.MIN_VALUE) {
                                    variableDataType[i] = theRecords[theRecords[ourProcId].replacedByArrayId].fixedJavaName;
                                    variableWSDataType[i] = theRecords[theRecords[ourProcId].replacedByArrayId].javaAttrName;
                                } else {
                                    variableDataType[i] = theRecords[ourProcId].fixedJavaName;
                                    variableWSDataType[i] = theRecords[ourProcId].javaAttrName;
                                }


                                if (theRecords[ourProcId].dataType.equals("TABLE")
                                        || theRecords[ourProcId].dataType.equals("VARRAY")
                                        || theRecords[ourProcId].dataType.equals("PL/SQL TABLE")
                                ) {
                                    if (theRecords[ourProcId].typeImplementingClass == null) {
                                        // must be implemented as a native type.
                                        if (theRecords[ourProcId].typeRecordClass.equals("oracle.sql.INTERVALDS")) {
                                            variableWSParentDataType[i] = "byte[]";
                                            variableWSDataType[i] = "byte[]";
                                            theFlags.setFlag(GlobalFlags.HAS_IDS_ARRAY);
                                        } else if (theRecords[ourProcId].typeRecordClass.equals("oracle.sql.INTERVALYM")) {
                                            variableWSParentDataType[i] = "byte[]";
                                            variableWSDataType[i] = "byte[]";
                                            theFlags.setFlag(GlobalFlags.HAS_IYM_ARRAY);
                                        } else if (theRecords[ourProcId].typeRecordClass.equals("oracle.sql.TIMESTAMP")) {
                                            variableWSParentDataType[i] = "byte[]"; //TODO: Db2 support
                                            variableWSDataType[i] = "byte[]";
                                            theFlags.setFlag(GlobalFlags.HAS_TZ_ARRAY);
                                        } else if (theRecords[ourProcId].typeRecordClass.equals("oracle.sql.TIMESTAMPTZ")) {
                                            variableWSParentDataType[i] = "byte[]";
                                            variableWSDataType[i] = "byte[]";
                                            theFlags.setFlag(GlobalFlags.HAS_TZTS_ARRAY);
                                        } else if (theRecords[ourProcId].typeRecordClass.equals("oracle.sql.TIMESTAMPLTZ")) {
                                            variableWSParentDataType[i] = "byte[]";
                                            variableWSDataType[i] = "byte[]";
                                            theFlags.setFlag(GlobalFlags.HAS_TZLTS_ARRAY);
                                        } else if (theRecords[ourProcId].typeRecordClass.equals("oracle.sql.BLOB")) {
                                            variableWSParentDataType[i] = "byte[]";
                                            variableWSDataType[i] = "byte[]";
                                            theFlags.setFlag(GlobalFlags.HAS_BLOB_SCALER_ARRAY);
                                        } else if (theRecords[ourProcId].typeRecordClass.equals("oracle.sql.CLOB")) {
                                            if (useCharForCLOB) {
                                                variableWSParentDataType[i] = "char[]";
                                                variableWSDataType[i] = "char[]";
                                            } else {
                                                variableWSParentDataType[i] = "byte[]";
                                                variableWSDataType[i] = "byte[]";
                                            }
                                            theFlags.setFlag(GlobalFlags.HAS_CLOB_SCALER_ARRAY);
                                        } else if (theRecords[ourProcId].typeRecordClass.equals("oracle.sql.OPAQUE")) {
                                            if (useCharForCLOB) {
                                                variableWSParentDataType[i] = "char[]";
                                                variableWSDataType[i] = "char[]";
                                            } else {
                                                variableWSParentDataType[i] = "byte[]";
                                                variableWSDataType[i] = "byte[]";
                                            }
                                            theFlags.setFlag(GlobalFlags.HAS_OPAQUE_SCALER_ARRAY);
                                        } else if (theRecords[ourProcId].typeRecordClass.equals("oracle.sql.STRUCT")) {
                                            //variableWSParentDataType[i] = "oracle.spatial.geometry.JGeometry[]";
                                            variableWSParentDataType[i] = "JGeometryWrapper[]";
                                            //variableWSDataType[i] = "oracle.spatial.geometry.JGeometry[]";
                                            variableWSDataType[i] = "JGeometryWrapper[]";
                                            theFlags.setFlag(GlobalFlags.HAS_STRUCT_SCALER_ARRAY);
                                        } else if (theRecords[ourProcId].typeRecordClass.equals("oracle.sql.BFILE")) {
                                            variableWSParentDataType[i] = "byte[]";
                                            variableWSDataType[i] = "byte[]";
                                            theFlags.setFlag(GlobalFlags.HAS_BFILE_SCALER_ARRAY);
                                            theFlags.setFlag(GlobalFlags.HAS_BFILES);
                                        } else {
                                            variableWSParentDataType[i] = theRecords[ourProcId].typeRecordClass;
                                            variableWSDataType[i] = theRecords[ourProcId].typeRecordClass;
                                        }

                                        variableWSDataType[i] = variableWSDataType[i] + "[]";

                                    } else {
                                        if (theRecords[ourProcId].typeImplementingClass.replacedByArrayId > Integer.MIN_VALUE) {
                                            int actualImplementingClassId = theRecords[ourProcId].typeImplementingClass.replacedByArrayId;

                                            variableWSParentDataType[i] = theRecords[actualImplementingClassId].fixedJavaName;
                                            variableWSDataType[i] = theRecords[actualImplementingClassId].javaAttrName;
                                        } else {
                                            variableWSParentDataType[i] = theRecords[ourProcId].typeImplementingClass.fixedJavaName;
                                            variableWSDataType[i] = theRecords[ourProcId].typeImplementingClass.javaAttrName;
                                        }

                                        variableWSDataType[i] = variableWSDataType[i] + "[]";
                                    }
                                }
                            }
                        } catch (CSException e) {

                            // increment recordParams with (1-att count) here...
                            recordInParams = recordInParams + 0;
                            recordOutParams = recordOutParams + 0;

                            theLog.error(theRowSet.getString("OBJECT_NAME")
                                    + " has unsupported datatype for parameter "
                                    + argName + " of "
                                    + theRowSet.getString("DATA_TYPE") + " " + theRowSet.getString("TYPE_NAME"));
                            variableDataType[i] = "Object";
                            isBroken = true;
                            isBrokenString = "//";
                        }

                        if (doBreak) {
                            break;
                        }
                    }
                    case SqlUtils.ORACLE_PLSQL_INDEXBY_DATATYPE: {
                        needsPlsqlIndexByArray = true;
                        variableDataType[i] = "com.mcpdbwizard.pub.PlsqlIndexByTable2";

                        plsqlIndexByPlaceHolderVarType[i] = argName + "_t";
                        plsqlIndexByPlaceHolderVarName[i] = argName + "_v";

                        ReadOnlyRowSet argRowSet = getAttrArguments(theRowSet.getString("OWNER")
                                , theRowSet.getString("OBJECT_NAME")
                                , theRowSet.getString("PACKAGE_NAME")
                                , theRowSet.getString("OVERLOAD")
                                , theRowSet.getInt("SEQUENCE")
                                , theRowSet.getInt("DATA_LEVEL")
                                , realName
                                , realOwner);

                        // Oracle 23ai: ALL_ARGUMENTS no longer expands the index-by table's element
                        // into a DATA_LEVEL>0 child row, so the walk above returns nothing (or, for a
                        // non-final argument, the next sibling argument at the same DATA_LEVEL). A real
                        // element child is at theRowSet's DATA_LEVEL+1; if we have no deeper row,
                        // synthesise the scalar element from ALL_PLSQL_COLL_TYPES so its datatype
                        // (NUMBER/VARCHAR2/...) is recognised below. 12c returns the real child, so this
                        // never triggers there.
                        boolean indexByHasElem = false;
                        if (argRowSet != null && argRowSet.size() > 0) {
                            argRowSet.first();
                            indexByHasElem = argRowSet.getInt("DATA_LEVEL") > theRowSet.getInt("DATA_LEVEL");
                        }
                        if (!indexByHasElem) {
                            QueryStatement idxElemQry = new QueryStatement(
                                    SqlStatementDictionary.getPlsqlIndexbyElemQry(loginVersion), theLog, theConnection);
                            idxElemQry.setParam(1, realOwner);
                            idxElemQry.setParam(2, theRowSet.getString("OBJECT_NAME"));
                            idxElemQry.setParam(3, theRowSet.getString("PACKAGE_NAME"));
                            idxElemQry.setParam(4, theRowSet.getString("OVERLOAD"));
                            idxElemQry.setParam(5, theRowSet.getInt("SEQUENCE"));
                            idxElemQry.setParam(6, theRowSet.getInt("DATA_LEVEL"));
                            idxElemQry.setParam(7, theRowSet.getString("TYPE_OWNER"));
                            idxElemQry.setParam(8, theRowSet.getString("TYPE_NAME"));
                            idxElemQry.setParam(9, theRowSet.getString("TYPE_SUBNAME"));
                            idxElemQry.setKeepFiles(false);
                            ReadOnlyRowSet synthElem = idxElemQry.execute();
                            idxElemQry.releaseResources();
                            if (synthElem != null && synthElem.size() > 0) {
                                argRowSet = synthElem;
                            }
                        }

                        PlsqlIndexByTableName[i] = theRowSet.getString("TYPE_NAME") + "." + theRowSet.getString("TYPE_SUBNAME");

                        if (!theRowSet.getString("OWNER").equals(theRowSet.getString("TYPE_OWNER"))) {
                            PlsqlIndexByTableName[i] = argRowSet.getString("TYPE_OWNER") + "." + PlsqlIndexByTableName[i];
                        } else if (!theRowSet.getString("OWNER").equals(loginName)) {
                            PlsqlIndexByTableName[i] = argRowSet.getString("OWNER") + "." + PlsqlIndexByTableName[i];
                        }

                        int dataType = SqlUtils.getUnderlyingOracleDatatype(argRowSet.getString("DATA_TYPE"));

                        if (dataType == SqlUtils.ORACLE_TEXT_DATATYPE) {
                            plsqlIndexByDataType[i] = OracleTypes.VARCHAR;
                            plsqlIndexByRealDataType[i] = oracle.jdbc.OracleTypes.VARCHAR;
                            plsqlIndexByDataLength[i] = argRowSet.getInt("DATA_LENGTH");
                            plsqlIndexByDataDecPlaces[i] = 0;
                            variableWSDataType[i] = "String[]";
                            theFlags.setFlag(GlobalFlags.HAS_STRING_INDEXBY_ARRAY);
                        } else if (dataType == SqlUtils.ORACLE_DATE_DATATYPE) {
                            plsqlIndexByDataType[i] = OracleTypes.DATE;
                            plsqlIndexByRealDataType[i] = oracle.jdbc.OracleTypes.VARCHAR;
                            plsqlIndexByDataLength[i] = 28; // yyyy-mm-dd hh24:mi:ss.fffffffff
                            plsqlIndexByDataDecPlaces[i] = 0;
                            //variableWSDataType[i] = "java.math.BigDecimal[]";
                            variableWSDataType[i] = "java.sql.Timestamp[]";
                            theFlags.setFlag(GlobalFlags.HAS_DATE_INDEXBY_ARRAY);
                        } else if (dataType == SqlUtils.ORACLE_TIMESTAMP_DATATYPE) {
                            plsqlIndexByDataType[i] = OracleTypes.TIMESTAMP;
                            plsqlIndexByRealDataType[i] = oracle.jdbc.OracleTypes.VARCHAR;
                            plsqlIndexByDataLength[i] = 28; // SYYYYMMDDHH24MISS
                            plsqlIndexByDataDecPlaces[i] = 6;
                            //variableWSDataType[i] = "java.math.BigDecimal[]";
                            variableWSDataType[i] = "java.sql.Timestamp[]";
                            theFlags.setFlag(GlobalFlags.HAS_TIMESTAMP_INDEXBY_ARRAY);
                        } else if (dataType == SqlUtils.ORACLE_TIMESTAMPTZ_DATATYPE
                                || dataType == SqlUtils.ORACLE_TIMESTAMPLTZ_DATATYPE) {
                            // A ZONED timestamp element. Before this arm existed the abbreviated
                            // dictionary spelling classified as plain TIMESTAMP and landed in the
                            // arm above, which marshals through a mask carrying no zone -- so the
                            // zone could neither be sent nor returned, on every Oracle version.
                            //
                            // It crosses as TEXT, not java.sql.Timestamp: that class has no zone to
                            // put one in, which is the whole reason the TIMESTAMP arm above cannot
                            // serve here. Text is also what lets the WEB SERVICE surface carry a
                            // zone -- the String[] helpers already exist, so this needs no new
                            // generated conversion code, and a zone crosses SOAP the same way a
                            // duality-view document does.
                            plsqlIndexByDataType[i] = (dataType == SqlUtils.ORACLE_TIMESTAMPTZ_DATATYPE)
                                    ? OracleTypes.TIMESTAMPTZ
                                    : OracleTypes.TIMESTAMPLTZ;
                            plsqlIndexByRealDataType[i] = oracle.jdbc.OracleTypes.VARCHAR;
                            // Wide enough for the longest region name, not just an offset:
                            // 'yyyy-mm-dd hh24:mi:ss.ff9' is 29 characters and the longest zone
                            // name in Oracle's own list ('America/Argentina/ComodRivadavia') is 32,
                            // so 62 is the true maximum and 80 leaves room. The 28 the unzoned arm
                            // uses truncates a region name silently -- the shuttle VARCHAR2 is
                            // sized from this number too.
                            plsqlIndexByDataLength[i] = 80;
                            plsqlIndexByDataDecPlaces[i] = 9;
                            variableWSDataType[i] = "String[]";
                            theFlags.setFlag(GlobalFlags.HAS_STRING_INDEXBY_ARRAY);
                        } else if (dataType == SqlUtils.ORACLE_NUMBER_DATATYPE) {
                            plsqlIndexByDataType[i] = OracleTypes.NUMBER;
                            plsqlIndexByRealDataType[i] = oracle.jdbc.OracleTypes.VARCHAR;
                            plsqlIndexByDataLength[i] = argRowSet.getInt("DATA_PRECISION");
                            try {
                                plsqlIndexByDataDecPlaces[i] = argRowSet.getInt("DATA_SCALE");
                                if (plsqlIndexByDataDecPlaces[i] == 0) {
                                    plsqlIndexByRealDataType[i] = oracle.jdbc.OracleTypes.NUMBER;
                                }
                            } catch (CSAttemptToGetNullException e) {
                                plsqlIndexByDataDecPlaces[i] = 0;
                                plsqlIndexByRealDataType[i] = oracle.jdbc.OracleTypes.NUMBER;
                            }


                            //variableWSDataType[i] = "java.math.BigDecimal[]";
                            variableWSDataType[i] = wsJavaNumberTypeComboBox + "[]";
                            theFlags.setFlag(GlobalFlags.HAS_NUMBER_INDEXBY_ARRAY);
                        } else if (dataType == SqlUtils.ORACLE_BINARY_DATATYPE) {
                            plsqlIndexByDataType[i] = OracleTypes.RAW;
                            plsqlIndexByRealDataType[i] = oracle.jdbc.OracleTypes.VARCHAR;
                            plsqlIndexByDataLength[i] = argRowSet.getInt("DATA_LENGTH") * 2;
                            plsqlIndexByDataDecPlaces[i] = 0;

                            //variableWSDataType[i] = "java.math.BigDecimal[]";
                            variableWSDataType[i] = "byte[][]";
                            theFlags.setFlag(GlobalFlags.HAS_RAW_INDEXBY_ARRAY);
                        } else {
                            String evilThing = argName + " of ";

                            if (theRowSet.getString("PACKAGE_NAME") != null) {
                                evilThing = evilThing + theRowSet.getString("PACKAGE_NAME") + ".";
                            }

                            evilThing = evilThing + theRowSet.getString("OBJECT_NAME");

                            if (!evilPlsqlTablesWarningGiven) {
                                theLog.warning(evilThing + EVIL_PLSQL_TABLES_WARNING_1, false, true);
                                theLog.info(EVIL_PLSQL_TABLES_WARNING_2, false, true);
                                evilPlsqlTablesWarningGiven = true;
                            } else {
                                theLog.warning(evilThing + theRowSet.getString("OBJECT_NAME") + EVIL_PLSQL_TABLES_WARNING_1);
                                theLog.info(EVIL_PLSQL_TABLES_WARNING_2);
                            }

                            throw new CSUnsupportedDatatypeException(Namer.param_prod_name + " does not support index by tables of data type " + argRowSet.getString("DATA_TYPE"), argRowSet.getString("DATA_TYPE"));
                        }


                        break;
                    }

                    default: {
                        throw new CSUnsupportedDatatypeException(Namer.param_prod_name + " does not support data type ", actualOracleDatatype[i] + "/" + oracleUnderlyingDatatype[i]);
                    }


                }
            } catch (CSNoDataInRowSetException e) {
            } catch (CSUnsupportedDatatypeException e) {
                theLog.error(e);
                isBroken = true;
                isBrokenString = "//";
            } catch (CSException e) {
                theLog.syserror("Row " + i + ": Unable to parse parameters:" + e.getMessage(), true, true);
                try {
                    theLog.syserror(theRowSet.getColumnNamesAsString(":"));
                    for (int q = 0; q < theRowSet.size(); q++) {
                        theRowSet.setCurrentRowNumber(q);
                        theLog.syserror(theRowSet.getRowAsString(":", "?"));
                    }
                } catch (CSException e2) {
                    theLog.syserror(e2);
                }
                theLog.syserror("Please email the log file for this session to " + Namer.param_support_email);
                isBroken = true;
                isBrokenString = "//";
            }

            if (variableWSDataType[i] == null) {
                variableWSDataType[i] = new String(variableDataType[i]);
            }

            if (variableWSDataType[i].equals("java.io.File")) {
                variableWSDataType[i] = "byte[]";
            } else if (variableWSDataType[i] != null && variableWSDataType[i].trim().length() == 0) {
                variableWSDataType[i] = new String(variableDataType[i]);
            }
        }

        // if complex update out params so they reflect real values
        if (isComplex && type == IS_A_FUNCTION) {
            for (int i = 0; i < paramOutId.length; i++) {
                if (paramOutId[i] > 0) {
                    paramOutId[i] = paramOutId[i] + inParamCount + recordInParams;
                }
            }
        }

        validate();

    }

    public BaseMethodRepresentation getMetadata() {
        return metadata;
    }

    /**
     * Top-level (DATA_LEVEL 0) parameter tuples for the MCP server emission, read from the
     * engine's parsed parameter state: one row per parameter as
     * {@code {argumentName, inOut, javaType, variableName, position, oracleUnderlyingDatatype,
     * elementJavaType}} (position 0 is the function return, whose argumentName is null;
     * oracleUnderlyingDatatype is the {@link com.mcpdbwizard.pub.SqlUtils} datatype constant, needed
     * because the wrapper names some accessors by Oracle type — ROWID/UROWID/TIMESTAMP all report
     * javaType "String" but read through {@code getParam<Cap>String()}; elementJavaType is the
     * immediate DATA_LEVEL-1 child's javaType — the element type of a collection). The MCP emission uses these to drive the
     * generated wrapper directly (setParam per IN, executeProc, getParam per OUT + return),
     * so it can expose procedures with any number of OUT / IN OUT parameters. Nested record
     * / collection children (DATA_LEVEL &gt; 0) are excluded; a record/collection parameter
     * itself surfaces as a non-JSON-crossable javaType at level 0, so the emission skips it.
     */
    public String[][] getMcpParamTuples() throws Exception {
        java.util.ArrayList<String[]> rows = new java.util.ArrayList<String[]>();
        for (int i = 0; i < theRowSet.size(); i++) {
            theRowSet.setCurrentRowNumber(i);
            if (theRowSet.getInt("DATA_LEVEL") != 0) {
                continue;
            }
            // For a collection param, look up its element type: ALL_ARGUMENTS does not expand a
            // package collection type into an element row, so query the type definition. The MCP
            // layer needs the element type to convert array elements and to gate the collection.
            String elementOracleType = "";
            int odt = oracleUnderlyingDatatype[i];
            if (odt == SqlUtils.ORACLE_VARRAY_DATATYPE || odt == SqlUtils.ORACLE_TABLE_DATATYPE
                    || odt == SqlUtils.ORACLE_PLSQL_INDEXBY_DATATYPE
                    || odt == SqlUtils.ORACLE_PLSQL_INDEXBY_ROWTYPE_DATATYPE) {
                elementOracleType = lookupCollectionElementType(theRowSet.getString("OWNER"),
                        theRowSet.getString("TYPE_NAME"), theRowSet.getString("TYPE_SUBNAME"));
            }
            rows.add(new String[]{
                    theRowSet.getString("ARGUMENT_NAME"),
                    theRowSet.getString("IN_OUT"),
                    variableDataType[i],
                    variableName[i],
                    String.valueOf(theRowSet.getInt("POSITION")),
                    String.valueOf(oracleUnderlyingDatatype[i]),
                    elementOracleType,
                    // The RAW Oracle type name, because oracleUnderlyingDatatype does not always
                    // distinguish what the emitted accessor is called. NCLOB is the case that bites:
                    // SqlUtils deliberately rides it on the CLOB path (oracle.sql.NCLOB extends CLOB
                    // and LongObjectLoader has no loadNCLOB), so it arrives here as
                    // ORACLE_CLOB_DATATYPE — yet the wrapper still names its accessor
                    // getParam<Cap>Nclob(), not ...Clob(). Without the name the MCP layer emitted a
                    // call to a method that was never written.
                    theRowSet.getString("DATA_TYPE") == null ? "" : theRowSet.getString("DATA_TYPE")
            });
        }
        if (theRowSet.size() > 0) {
            theRowSet.setCurrentRowNumber(0);
        }
        return rows.toArray(new String[0][]);
    }

    /**
     * The Oracle element type name of a collection type ({@code ELEM_TYPE_NAME}), or {@code ""} if
     * it cannot be resolved. A package collection type (TYPE_SUBNAME present) is read from
     * {@code ALL_PLSQL_COLL_TYPES}, a standalone type from {@code ALL_COLL_TYPES}. Best-effort: an
     * unresolved element type just leaves the collection unsupported by the MCP layer.
     */
    private String lookupCollectionElementType(String owner, String typeName, String typeSubname) {
        try {
            QueryStatement theQuery;
            if (typeSubname != null) {
                theQuery = new QueryStatement(
                        "SELECT elem_type_name FROM all_plsql_coll_types WHERE owner = ? AND package_name = ? AND type_name = ?",
                        theLog, theConnection);
                theQuery.setParam(1, owner);
                theQuery.setParam(2, typeName);
                theQuery.setParam(3, typeSubname);
            } else {
                theQuery = new QueryStatement(
                        "SELECT elem_type_name FROM all_coll_types WHERE owner = ? AND type_name = ?",
                        theLog, theConnection);
                theQuery.setParam(1, owner);
                theQuery.setParam(2, typeName);
            }
            ReadOnlyRowSet theResult = theQuery.execute();
            if (theResult.size() > 0) {
                theResult.setCurrentRowNumber(0);
                String elementType = theResult.getString("ELEM_TYPE_NAME");
                if (elementType != null) {
                    return elementType;
                }
            }
        } catch (Exception theException) {
            theLog.debug("MCP: could not resolve collection element type for " + typeName + ": " + theException);
        }
        return "";
    }

    public boolean validate() {
        boolean retCode = true;

        for (int i = 0; i < theRowSet.size(); i++) {
            theRowSet.setCurrentRowNumber(i);
            if (oracleParamDatatype[i] == "SqlUtils.ORACLE_OTHER_DATATYPE") {
                theLog.error("Error: parameter " + variableName[i] + " is broken");
                isBroken = true;
                isBrokenString = "//";
                retCode = false;
            } else if (variableDataType[i] == null) {
                theLog.error("Error: parameter " + variableName[i] + " is broken");
                isBroken = true;
                isBrokenString = "//";
                retCode = false;
            } else if (oracleParamDatatype[i] == "SqlUtils.ORACLE_TABLE_DATATYPE"
                    || oracleParamDatatype[i] == "SqlUtils.ORACLE_VARRAY_DATATYPE"
                    || oracleParamDatatype[i] == "SqlUtils.ORACLE_PLSQL_INDEXBY_ROWTYPE_DATATYPE"
                    || oracleParamDatatype[i] == "SqlUtils.ORACLE_OBJECT_DATATYPE") {
                if (variableDataType[i].equals("Object")) {
                    theLog.error("Error: parameter " + variableName[i] + " is broken");
                    isBroken = true;
                    isBrokenString = "//";
                    retCode = false;
                }
            }
        }
        return (retCode);
    }

    /**
     * This method is used to decide whether to import OracleTypes or not.
     */
    int getNonArrayOutParamCount() {
        return (outParamCount - outArrayCount);
    }

    String[] getDebugComment() {
        String[] tempString = new String[theRowSet.size() + 1];
        tempString[0] = theRowSet.getColumnNamesAsString(":");
        try {
            for (int i = 0; i < theRowSet.size(); i++) {
                theRowSet.setCurrentRowNumber(i);
                tempString[i + 1] = theRowSet.getRowAsString(":", "Unprintable");
            }
        } catch (CSNoDataInRowSetException e) {
            tempString = new String[0];
        }

        return (tempString);
    }

    void addOracleTypeDefinitionsOLD(TextChunk theTextChunk, String tableName) {
        final String OBJ_EXTENSION = "_OBJ";
        final String ARY_EXTENSION = "_ARY";

        String objectTypeName = tableName + OBJ_EXTENSION;
        String arrayTypeName = tableName + ARY_EXTENSION;

        theTextChunk.addLine("REM ");
        theTextChunk.addLine("REM Matching object Definition for table " + tableName);
        theTextChunk.addLine("REM ");
        theTextChunk.addLine(" ");
        theTextChunk.addLine("CREATE OR REPLACE TYPE " + objectTypeName + " AS OBJECT ");

        int maxLength = 0;

        for (int i = 0; i < theRowSet.size(); i++) {
            try {
                theRowSet.setCurrentRowNumber(i);
                if (theRowSet.getString("ARGUMENT_NAME").length() > maxLength) {
                    maxLength = theRowSet.getString("ARGUMENT_NAME").length();
                }

            } catch (Exception e) {
                theLog.syserror(e, true, true);
                maxLength = 30;
            }
        }

        maxLength++;

        for (int i = 0; i < theRowSet.size(); i++) {
            try {
                theRowSet.setCurrentRowNumber(i);
                String aLine = " ";
                if (i == 0) {
                    aLine = aLine + "(";
                } else {
                    aLine = aLine + ",";
                }

                final String spaces = "                                                                          ";

                aLine = aLine + theRowSet.getString("ARGUMENT_NAME").toUpperCase() + spaces.substring(0, (maxLength - theRowSet.getString("ARGUMENT_NAME").length())) + theRowSet.getString("DATA_TYPE");
                String dataType = theRowSet.getString("DATA_TYPE");
                if (dataType.equalsIgnoreCase("CHAR")
                        || dataType.equalsIgnoreCase("CHARACTER")
                        || dataType.equalsIgnoreCase("NCHAR")
                        || dataType.equalsIgnoreCase("NCHARACTER")
                        || dataType.equalsIgnoreCase("RAW")
                        || dataType.equalsIgnoreCase("STRING")
                        || dataType.equalsIgnoreCase("VARCHAR")
                        || dataType.equalsIgnoreCase("VARCHAR2")) {
                    aLine = aLine + "(" + theRowSet.getInt("DATA_LENGTH") + ")";
                } else {
                    if (theRowSet.getString("FMT") != null) {
                        aLine = aLine + theRowSet.getString("FMT");
                    }
                }

                if ((i + 1) == theRowSet.size()) {
                    aLine = aLine + ");";
                }

                theTextChunk.addLine(aLine);
            } catch (Exception e) {
                theLog.syserror(e, true, true);
            }
        }

        theTextChunk.addLine(".");
        theTextChunk.addLine("/");
        theTextChunk.addLine(" ");
        theTextChunk.addLine("SHOW ERRORS");
        theTextChunk.addLine(" ");
        theTextChunk.addLine("REM ");
        theTextChunk.addLine("REM Array Definition for Object Type " + objectTypeName);
        theTextChunk.addLine("REM ");
        theTextChunk.addLine(" ");
        theTextChunk.addLine("CREATE OR REPLACE TYPE " + arrayTypeName + " AS TABLE OF " + objectTypeName + ";");
        theTextChunk.addLine(".");
        theTextChunk.addLine("/");
        theTextChunk.addLine(" ");
        theTextChunk.addLine("SHOW ERRORS");
        theTextChunk.addLine(" ");
    }

    void addArrayMoveFilesToByteArrayMethod(JavaChunk theJavaChunk
            , boolean comments
            , boolean debugMessages
            , boolean webServicesFlag
            , String recordName) {

        if (webServicesFlag) {
            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* Method to move data from Files into Byte arrays.");
                theJavaChunk.print("* The files are deleted as part of this process.");
                theJavaChunk.print("* @since Build 4.0.2150");
                theJavaChunk.print("*/");
            }

            theJavaChunk.print("public void  moveFilesToByteArrays() throws CSException");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print(recordName + "[] theRecords = getCurrentValues();");
            theJavaChunk.print("");
            if (debugMessages)
                theJavaChunk.print(stheLog + ".debug(\"Moving files to byte arrays for each of the \" + theRecords.length + \" records in " + recordName + "[]\");");
            theJavaChunk.print("for (int i=0; i < theRecords.length; i++)");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("theRecords[i].moveFilesToByteArrays();");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
            theJavaChunk.print("");

            theJavaChunk.print("}");
            theJavaChunk.unIndent();

            theJavaChunk.print(" ");

            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* Method to move data from LOBs into Files.");
                theJavaChunk.print("* @param String " + stempFilePrefix + " The prefix for the temporary file's name");
                theJavaChunk.print("* @param String " + stempFileSuffix + " The suffix for the temporary file's name");
                theJavaChunk.print("* @param java.io.File " + stempFileDir + " The directory for the temporary files.");
                theJavaChunk.print("* @param com.mcpdbwizard.pub.LongObjectLoader lobUnloader An instance of " + Namer.param_prod_name + "'s LOB wrangler class");
                theJavaChunk.print("* @throws CSException");
                theJavaChunk.print("* @since Build 4.0.2150");
                theJavaChunk.print("*/");
            }

            theJavaChunk.print("public void  moveLobsToFiles(String " + stempFilePrefix);
            theJavaChunk.print("                            ,String " + stempFileSuffix);
            theJavaChunk.print("                            ,java.io.File " + stempFileDir);
            theJavaChunk.print("                            ,com.mcpdbwizard.pub.LongObjectLoader lobUnloader) throws CSException");
            theJavaChunk.indent();
            theJavaChunk.print("{");

            theJavaChunk.print(recordName + "[] theRecords = getCurrentValues();");
            theJavaChunk.print("");
            if (debugMessages)
                theJavaChunk.print(stheLog + ".debug(\"Moving LOBs to Files for  \" + theRecords.length + \" records in " + recordName + "[]\");");
            theJavaChunk.print("for (int i=0; i < theRecords.length; i++)");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("theRecords[i].moveLobsToFiles(" + stempFilePrefix + ", " + stempFileSuffix + ", " + stempFileDir + ", lobUnloader);");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
            theJavaChunk.print("");
            theJavaChunk.print(" ");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();

            theJavaChunk.print(" ");

            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* Method to move data from LOBs into Byte Arrays");
                theJavaChunk.print("* @param com.mcpdbwizard.pub.LongObjectLoader lobUnloader An instance of " + Namer.param_prod_name + "'s LOB wrangler class");
                theJavaChunk.print("* @throws CSException");
                theJavaChunk.print("* @since Build 4.0.2150");
                theJavaChunk.print("*/");
            }

            theJavaChunk.print("public void moveLobsToByteArrays(com.mcpdbwizard.pub.LongObjectLoader lobUnloader) throws CSException");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print(recordName + "[] theRecords = getCurrentValues();");
            theJavaChunk.print("");
            if (debugMessages)
                theJavaChunk.print(stheLog + ".debug(\"Moving LOBs to byte arrays for  \" + theRecords.length + \" records in " + recordName + "[]\");");
            theJavaChunk.print("for (int i=0; i < theRecords.length; i++)");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("theRecords[i].moveLobsToByteArrays(lobUnloader);");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
            theJavaChunk.print("");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
        }
    }

    void addMoveFilesToByteArrayMethod(JavaChunk theJavaChunk
            , boolean comments
            , boolean debugMessages
            , boolean webServicesFlag) {

        //if (webServicesFlag)
        //{
        if (comments) {
            theJavaChunk.print("/**");
            theJavaChunk.print("* Method to move data from Files into Byte arrays.");
            theJavaChunk.print("* The files are deleted as part of this process.");
            theJavaChunk.print("* @since Build 4.0.2150");
            theJavaChunk.print("*/");
        }

        theJavaChunk.print("public void  moveFilesToByteArrays() throws CSException");
        theJavaChunk.indent();
        theJavaChunk.print("{");

        for (int i = 0; i < theRowSet.size(); i++) {
            try {
                theRowSet.setCurrentRowNumber(i);

                //if variable name has already been seen do nothing...
                String currentVariableName = new String(variableName[i]);

                for (int j = 0; j < i; j++) {
                    //theRowSet.setCurrentRowNumber(j);
                    if (variableName[j].equals(currentVariableName)) {
                        throw (new CSSkipSectionException());
                    }
                }

                if (useVariable[i]) {
                    if (variableDataType[i].equalsIgnoreCase("java.io.File")) {
                        if (comments)
                            theJavaChunk.print("// Move " + variableName[i] + " from a file into byte array " + byteName[i]);
                        theJavaChunk.print(" if (" + variableName[i] + " != null && " + variableName[i] + ".exists())");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");
                        if (debugMessages)
                            theJavaChunk.print(stheLog + ".debug(\"Moving contents of " + variableName[i] + " to " + byteName[i] + " and then deleting it\");");
                        if (useCharForCLOB && variableWSDataType[i].equals("char[]")) {
                            theJavaChunk.print(byteName[i] + " = com.mcpdbwizard.pub.IOUtils.loadFileIntoCharArray(" + variableName[i] + ");");
                        } else {
                            theJavaChunk.print(byteName[i] + " = com.mcpdbwizard.pub.IOUtils.loadFileIntoByteArray(" + variableName[i] + ");");
                        }
                        theJavaChunk.print(variableName[i] + ".delete();");
                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();
                        theJavaChunk.print("");
                    }
                }
            } catch (CSSkipSectionException e) {
            }
//      catch (CSException e)
//        {
//        theLog.syserror(e, true,true);
//        }
        }

        theJavaChunk.print(" ");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        //}

        theJavaChunk.print(" ");

        if (comments) {
            theJavaChunk.print("/**");
            theJavaChunk.print("* Method to move data from LOBs into Files. If no file is specified a");
            theJavaChunk.print("* Temporary file is created and used instead.");
            theJavaChunk.print("* @param String " + stempFilePrefix + " The prefix for the temporary file's name");
            theJavaChunk.print("* @param String " + stempFileSuffix + " The suffix for the temporary file's name");
            theJavaChunk.print("* @param java.io.File " + stempFileDir + " The directory for the temporary files.");
            theJavaChunk.print("* @param com.mcpdbwizard.pub.LongObjectLoader lobUnloader An instance of " + Namer.param_prod_name + "'s LOB wrangler class");
            theJavaChunk.print("* @throws CSException");
            theJavaChunk.print("* @since Build 4.0.2150");
            theJavaChunk.print("*/");
        }

        theJavaChunk.print("public void  moveLobsToFiles(String " + stempFilePrefix);
        theJavaChunk.print("                            ,String " + stempFileSuffix);
        theJavaChunk.print("                            ,java.io.File " + stempFileDir);
        theJavaChunk.print("                            ,com.mcpdbwizard.pub.LongObjectLoader lobUnloader) throws CSException");
        theJavaChunk.indent();
        theJavaChunk.print("{");

        for (int i = 0; i < theRowSet.size(); i++) {
            try {
                theRowSet.setCurrentRowNumber(i);

                String currentVariableName = new String(variableName[i]);

                for (int j = 0; j < i; j++) {
                    //theRowSet.setCurrentRowNumber(j);
                    if (variableName[j].equals(currentVariableName)) {
                        throw (new CSSkipSectionException());
                    }
                }

                if (useVariable[i]) {
                    if (variableDataType[i].equalsIgnoreCase("java.io.File") && !(actualOracleDatatype[i].equals("LONG RAW"))) {
                        if (comments)
                            theJavaChunk.print("// Move LOB " + lobName[i] + " from a file into File " + variableName[i]);
                        theJavaChunk.print("if (" + lobName[i] + " != null )");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");
                        theJavaChunk.print("try");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");
                        if (debugMessages)
                            theJavaChunk.print(stheLog + ".debug(\"Moving contents of " + lobName[i] + " to file " + variableName[i] + ".\");");
                        theJavaChunk.print("");
                        theJavaChunk.print("if (" + variableName[i] + " == null )");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");
                        theJavaChunk.print(variableName[i] + " = java.io.File.createTempFile(" + stempFilePrefix + "," + stempFileSuffix + "," + stempFileDir + ");");
                        if (debugMessages)
                            theJavaChunk.print(stheLog + ".debug(\"File " + variableName[i] + " being set to \" + " + variableName[i] + ".getAbsolutePath());");
                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();
                        theJavaChunk.print("");
                        if (oracleParamDatatype[i].equals("OracleTypes.CLOB")) {
                            theJavaChunk.print("lobUnloader.unloadClob(" + variableName[i] + "," + lobName[i] + ");");
                        } else if (oracleParamDatatype[i].equals("OracleTypes.BLOB")) {
                            theJavaChunk.print("lobUnloader.unloadBlob(" + variableName[i] + "," + lobName[i] + ");");
                        } else if (oracleParamDatatype[i].equals("OracleTypes.BFILE")) {
                            theJavaChunk.print("lobUnloader.unloadBfile(" + variableName[i] + "," + lobName[i] + ");");
                        }
                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();
                        theJavaChunk.print("catch (Exception e)");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");
                        theJavaChunk.print("throw new CSException(\"moveLobsToFiles: " + lobName[i] + " could not to be downloaded to file " + variableName[i] + ".\");");
                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();
                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();
                        theJavaChunk.print("");
                    }
                }
            } catch (CSSkipSectionException e) {
            }
//      catch (CSException e)
//        {
//        theLog.syserror(e, true,true);
//        }
        }

        theJavaChunk.print(" ");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();

        theJavaChunk.print(" ");

        if (comments) {
            theJavaChunk.print("/**");
            theJavaChunk.print("* Method to move data from LOBs into Byte Arrays");
            theJavaChunk.print("* @param com.mcpdbwizard.pub.LongObjectLoader lobUnloader An instance of " + Namer.param_prod_name + "'s LOB wrangler class");
            theJavaChunk.print("* @throws CSException");
            theJavaChunk.print("* @since Build 4.0.2150");
            theJavaChunk.print("*/");
        }

        theJavaChunk.print("public void moveLobsToByteArrays(com.mcpdbwizard.pub.LongObjectLoader lobUnloader) throws CSException");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        //theJavaChunk.print("moveLobsToFiles(" + stempFilePrefix + ", " + stempFileSuffix + ", " + stempFileDir + ", lobUnloader);");
        //theJavaChunk.print("moveFilesToByteArrays();");


        for (int i = 0; i < theRowSet.size(); i++) {
            try {
                theRowSet.setCurrentRowNumber(i);

                String currentVariableName = new String(variableName[i]);

                for (int j = 0; j < i; j++) {
                    //theRowSet.setCurrentRowNumber(j);
                    if (variableName[j].equals(currentVariableName)) {
                        throw (new CSSkipSectionException());
                    }
                }

                if (useVariable[i]) {
                    if (variableDataType[i].equalsIgnoreCase("java.io.File")
                            && (!actualOracleDatatype[i].equals("LONG RAW"))
                        /*&& (! oracleParamDatatype[i].equals("OracleTypes.OPAQUE")) */
                    ) {
                        if (comments)
                            theJavaChunk.print("// Move LOB " + lobName[i] + "  into byte array " + byteName[i]);
                        theJavaChunk.print("");
                        theJavaChunk.print("if (" + lobName[i] + " != null )");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");
                        theJavaChunk.print("try");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");
                        theJavaChunk.print(byteName[i] + " = null;");
                        if (debugMessages)
                            theJavaChunk.print(stheLog + ".debug(\"Moving contents of " + lobName[i] + " to byte array " + byteName[i] + ".\");");

                        theJavaChunk.print("");
                        if (oracleParamDatatype[i].equals("OracleTypes.CLOB")) {
                            if (useCharForCLOB) {
                                theJavaChunk.print(byteName[i] + "  = lobUnloader.unloadClobIntoCharArray(" + lobName[i] + ");");
                            } else {
                                theJavaChunk.print(byteName[i] + "  = lobUnloader.unloadClobIntoByteArray(" + lobName[i] + ");");
                            }
                        } else if (oracleParamDatatype[i].equals("OracleTypes.BLOB")) {
                            theJavaChunk.print(byteName[i] + "  = lobUnloader.unloadBlobIntoByteArray(" + lobName[i] + ");");
                        } else if (oracleParamDatatype[i].equals("OracleTypes.OPAQUE")) {
                            //theJavaChunk.print(byteName[i] + "  = lobUnloader.unloadOpaqueIntoByteArray(" + lobName[i] + ");");


                            //


                            theJavaChunk.print("");
                            //theJavaChunk.print(isBrokenString + "try  ");
                            //theJavaChunk.indent();
                            //theJavaChunk.print(isBrokenString + "{ ");
                            theJavaChunk.print(isBrokenString + "oracle.xdb.XMLType tempXmlType = oracle.xdb.XMLType.createXML(" + lobName[i] + ");");

                            theJavaChunk.print(isBrokenString + "if (tempXmlType != null)");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{ ");


                            theJavaChunk.print(isBrokenString + "oracle.sql.CLOB tempClob = tempXmlType.getClobVal();");
                            if (useCharForCLOB) {
                                theJavaChunk.print(isBrokenString + "" + byteName[i] + " = lobUnloader.unloadClobIntoCharArray(tempClob);   ");
                            } else {
                                theJavaChunk.print(isBrokenString + "" + byteName[i] + " = lobUnloader.unloadClobIntoByteArray(tempClob);   ");
                            }
                            if (comments)
                                theJavaChunk.print("// The Javadoc for XMLType says it *must* be closed after use to avoid memory leaks..");
                            theJavaChunk.print("tempXmlType.close(); ");
                            theJavaChunk.print(isBrokenString + lobName[i] + " =  null; ");
                            theJavaChunk.print(isBrokenString + "}");
                            theJavaChunk.unIndent();

                            theJavaChunk.print(isBrokenString + "else");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{ ");
                            theJavaChunk.print(isBrokenString + lobName[i] + " = null;");
                            theJavaChunk.print(isBrokenString + "}                        ");
                            theJavaChunk.unIndent();

                            //theJavaChunk.print(isBrokenString + "}                        ");
                            //theJavaChunk.unIndent();


                            //theJavaChunk.print(isBrokenString + "}                        ");
                            //theJavaChunk.unIndent();


                        } else if (oracleParamDatatype[i].equals("OracleTypes.BFILE")) {
                            theJavaChunk.print(byteName[i] + "  = lobUnloader.unloadBfileIntoByteArray(" + lobName[i] + ");");
                        }
                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();
                        theJavaChunk.print("catch (Exception e)");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");
                        theJavaChunk.print("throw new CSException(\"moveLobsToByteArray: " + lobName[i] + " could not to be downloaded to array " + byteName[i] + ": \" + e.getMessage());");
                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();
                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();
                        theJavaChunk.print("");
                    }
                }
            } catch (CSSkipSectionException e) {
            }
//    catch (CSException e)
//      {
//      theLog.syserror(e, true,true);
//      }
        }


        theJavaChunk.print("}");
        theJavaChunk.unIndent();
    }

    void addRowCastMethod(JavaChunk theJavaChunk
            , boolean comments
            , boolean debugMessages
            , String extendedDataType
            , String baseDataType) {

        if (comments) {
            theJavaChunk.print("/**");
            theJavaChunk.print("* Static method to cast " + baseDataType + " to " + extendedDataType);
            theJavaChunk.print("* @param " + baseDataType);
            theJavaChunk.print("* @return " + extendedDataType);
            theJavaChunk.print("* @since Build 5.0.2255");
            theJavaChunk.print("*/");
        }

        theJavaChunk.print("public static " + extendedDataType + "  create" + extendedDataType + "From" + baseDataType + "(" + baseDataType + " theAttrs )");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print(extendedDataType + " newRecord = new " + extendedDataType + "();");
        theJavaChunk.print(" ");
        theJavaChunk.print("if (theAttrs == null)");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("return(newRecord);");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print(" ");

        for (int i = 0; i < theRowSet.size(); i++) {
            try {
                theRowSet.setCurrentRowNumber(i);

                //if variable name has already been seen do nothing...
                String currentVariableName = new String(variableName[i]);

                for (int j = 0; j < i; j++) {
                    //theRowSet.setCurrentRowNumber(j);
                    if (variableName[j].equals(currentVariableName)) {
                        throw (new CSSkipSectionException());
                    }
                }

                if (useVariable[i]) {
                    if (variableDataType[i].equalsIgnoreCase("java.io.File")) {
                        // Do nothing
                    } else {
                        theJavaChunk.print("newRecord." + variableName[i] + "  = theAttrs." + variableName[i] + ";");
                    }
                } else {
                    if (comments)
                        theJavaChunk.print("// " + variableDataType[i] + " " + variableName[i] + " not needed in this case.");
                }
            } catch (CSSkipSectionException e) {
            }
//      catch (CSException e)
//        {
//        theLog.syserror(e, true,true);
//        }

        }

        theJavaChunk.print(" ");
        theJavaChunk.print("return (newRecord);");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print(" ");

        if (comments) {
            theJavaChunk.print("/**");
            theJavaChunk.print("* Static method to cast " + extendedDataType + " to " + baseDataType);
            theJavaChunk.print("* @param " + extendedDataType);
            theJavaChunk.print("* @return " + baseDataType);
            theJavaChunk.print("* @since Build 5.0.2255");
            theJavaChunk.print("*/");
        }

        theJavaChunk.print("public static " + baseDataType + " create" + baseDataType + "From" + extendedDataType + "(" + extendedDataType + " theRow )");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print(baseDataType + " newRecord = new " + baseDataType + "();");
        theJavaChunk.print(" ");
        theJavaChunk.print("if (theRow == null)");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("return(newRecord);");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print(" ");

        for (int i = 0; i < theRowSet.size(); i++) {
            try {
                theRowSet.setCurrentRowNumber(i);

                //if variable name has already been seen do nothing...
                String currentVariableName = new String(variableName[i]);

                for (int j = 0; j < i; j++) {
                    //theRowSet.setCurrentRowNumber(j);
                    if (variableName[j].equals(currentVariableName)) {
                        throw (new CSSkipSectionException());
                    }
                }

                if (useVariable[i]) {
                    if (variableDataType[i].equalsIgnoreCase("java.io.File")) {
                        // Do nothing
                    } else {
                        theJavaChunk.print("newRecord." + variableName[i] + "  = theRow." + variableName[i] + ";");
                    }
                } else {
                    if (comments)
                        theJavaChunk.print("// " + variableDataType[i] + " " + variableName[i] + " not needed in this case.");
                }
            } catch (CSSkipSectionException e) {
            }
//      catch (CSException e)
//        {
//        theLog.syserror(e, true,true);
//        }

        }

        theJavaChunk.print(" ");
        theJavaChunk.print("return (newRecord);");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();

        theJavaChunk.print(" ");
        if (comments) {
            theJavaChunk.print("/**");
            theJavaChunk.print("* Static method to cast Array of " + extendedDataType + " to Array of " + baseDataType);
            theJavaChunk.print("* @param " + extendedDataType + "[]");
            theJavaChunk.print("* @return " + baseDataType + "[]");
            theJavaChunk.print("* @since Build 5.0.2257");
            theJavaChunk.print("*/");
        }

        theJavaChunk.print("public static " + baseDataType + "[]  create" + baseDataType + "ArrayFrom" + extendedDataType + "Array(" + extendedDataType + "[] theAttrsArray )");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print(baseDataType + "[] newRecords = null;");
        theJavaChunk.print(" ");
        theJavaChunk.print("if (theAttrsArray == null)");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("return(newRecords);");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print(" ");
        theJavaChunk.print("newRecords = new " + baseDataType + "[theAttrsArray.length];");
        theJavaChunk.print(" ");

        theJavaChunk.print("for (int i=0; i < newRecords.length; i++)");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("newRecords[i] = create" + baseDataType + "From" + extendedDataType + "(theAttrsArray[i]);");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print("");
        theJavaChunk.print("return (newRecords);");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();

        theJavaChunk.print(" ");
        if (comments) {
            theJavaChunk.print("/**");
            theJavaChunk.print("* Static method to cast ReadOnlyRowSet to Array of " + extendedDataType);
            theJavaChunk.print("* @param com.mcpdbwizard.pub.ReadOnlyRowSet theRowSet");
            theJavaChunk.print("* @return " + extendedDataType + "[]");
            theJavaChunk.print("* @since Build 5.0.2255");
            theJavaChunk.print("*/");
        }

        theJavaChunk.print("public static " + extendedDataType + "[]  create" + extendedDataType + "ArrayFromRowSet(com.mcpdbwizard.pub.ReadOnlyRowSet theRowSet");
        theJavaChunk.print("                                                                               ,com.mcpdbwizard.pub.LogInterface theLog) throws CSException");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print(extendedDataType + "[] newRecords = null;");
        theJavaChunk.print(" ");
        theJavaChunk.print("if (theRowSet == null)");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("return(newRecords);");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print(" ");
        theJavaChunk.print("try");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("newRecords = new " + extendedDataType + "[theRowSet.size()];");
        theJavaChunk.print(" ");

        theJavaChunk.print("for (int i=0; i < theRowSet.size(); i++)");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("theRowSet.setCurrentRowNumber(i);");
        theJavaChunk.print("newRecords[i] = new " + extendedDataType + "();");
        theJavaChunk.print("newRecords[i].setNewValues(theRowSet.getCurrentRow());");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print("");
        theJavaChunk.print("return (newRecords);");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print("catch (Exception e)");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("theLog.error(\"" + extendedDataType + ".create" + extendedDataType + "ArrayFromRowSet:\");");
        theJavaChunk.print("theLog.error(e);");
        theJavaChunk.print("throw new CSException(\"" + extendedDataType + ".create" + extendedDataType + "ArrayFromRowSet:\"+e.getMessage());");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
    }

    void addCastMethod(JavaChunk theJavaChunk
            , boolean comments
            , boolean debugMessages
            , String extendedDataType
            , String baseDataType) {

        if (comments) {
            theJavaChunk.print("/**");
            theJavaChunk.print("* Static method to cast " + baseDataType + " to " + extendedDataType);
            theJavaChunk.print("* @param " + baseDataType);
            theJavaChunk.print("* @return " + extendedDataType);
            theJavaChunk.print("* @since " + Namer.param_product_name + " Build 4.0.2040");
            theJavaChunk.print("*/");
        }

        theJavaChunk.print("public static " + extendedDataType + "  create" + extendedDataType + "From" + baseDataType + "(" + baseDataType + " theAttrs )");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print(extendedDataType + " newRecord = new " + extendedDataType + "();");
        theJavaChunk.print(" ");
        theJavaChunk.print("if (theAttrs == null)");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("return(newRecord);");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print(" ");

        for (int i = 0; i < theRowSet.size(); i++) {
            try {
                theRowSet.setCurrentRowNumber(i);

                //if variable name has already been seen do nothing...
                String currentVariableName = new String(variableName[i]);

                for (int j = 0; j < i; j++) {
                    //theRowSet.setCurrentRowNumber(j);
                    if (variableName[j].equals(currentVariableName)) {
                        throw (new CSSkipSectionException());
                    }
                }

                if (useVariable[i]) {
                    if (variableDataType[i].equalsIgnoreCase("java.io.File")) {
                        if (byteName[i] != null) {
                            theJavaChunk.print("newRecord." + byteName[i] + "  = theAttrs." + byteName[i] + ";");
                        }
                    } else {
                        theJavaChunk.print("newRecord." + variableName[i] + "  = theAttrs." + variableName[i] + ";");
                    }
                } else {
                    if (comments)
                        theJavaChunk.print("// " + variableDataType[i] + " " + variableName[i] + " not needed in this case.");
                }
            } catch (CSSkipSectionException e) {
            }
//      catch (CSException e)
//        {
//        theLog.syserror(e, true,true);
//        }

        }

        theJavaChunk.print(" ");
        theJavaChunk.print("return (newRecord);");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();

        theJavaChunk.print(" ");
        if (comments) {
            theJavaChunk.print("/**");
            theJavaChunk.print("* Static method to cast " + extendedDataType + " to " + baseDataType);
            theJavaChunk.print("* @param " + extendedDataType);
            theJavaChunk.print("* @return " + baseDataType);
            theJavaChunk.print("* @since " + Namer.param_product_name + " Build 5.0.2257");
            theJavaChunk.print("*/");
        }

        theJavaChunk.print("public static " + baseDataType + "  create" + baseDataType + "From" + extendedDataType + "(" + extendedDataType + " theRow )");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print(baseDataType + " newRecord = new " + baseDataType + "();");
        theJavaChunk.print(" ");
        theJavaChunk.print("if (theRow == null)");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("return(newRecord);");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print(" ");

        for (int i = 0; i < theRowSet.size(); i++) {
            try {
                theRowSet.setCurrentRowNumber(i);

                //if variable name has already been seen do nothing...
                String currentVariableName = new String(variableName[i]);

                for (int j = 0; j < i; j++) {
                    //theRowSet.setCurrentRowNumber(j);
                    if (variableName[j].equals(currentVariableName)) {
                        throw (new CSSkipSectionException());
                    }
                }

                if (useVariable[i]) {
                    if (variableDataType[i].equalsIgnoreCase("java.io.File")) {
                        if (byteName[i] != null) {
                            theJavaChunk.print("newRecord." + byteName[i] + "  = theRow." + byteName[i] + ";");
                        }
                    } else {
                        theJavaChunk.print("newRecord." + variableName[i] + "  = theRow." + variableName[i] + ";");
                    }
                } else {
                    if (comments)
                        theJavaChunk.print("// " + variableDataType[i] + " " + variableName[i] + " not needed in this case.");
                }
            } catch (CSSkipSectionException e) {
            }
//      catch (CSException e)
//        {
//        theLog.syserror(e, true,true);
//        }

        }

        theJavaChunk.print(" ");
        theJavaChunk.print("return (newRecord);");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();

        theJavaChunk.print(" ");
        if (comments) {
            theJavaChunk.print("/**");
            theJavaChunk.print("* Static method to cast Array of " + baseDataType + " to Array of " + extendedDataType);
            theJavaChunk.print("* @param " + baseDataType + "[]");
            theJavaChunk.print("* @return " + extendedDataType + "[]");
            theJavaChunk.print("* @since Build 4.0.2040");
            theJavaChunk.print("*/");
        }

        theJavaChunk.print("public static " + extendedDataType + "[]  create" + extendedDataType + "ArrayFrom" + baseDataType + "Array(" + baseDataType + "[] theAttrsArray )");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print(extendedDataType + "[] newRecords = null;");
        theJavaChunk.print(" ");
        theJavaChunk.print("if (theAttrsArray == null)");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("return(newRecords);");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print(" ");
        theJavaChunk.print("newRecords = new " + extendedDataType + "[theAttrsArray.length];");
        theJavaChunk.print(" ");

        theJavaChunk.print("for (int i=0; i < newRecords.length; i++)");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("newRecords[i] = create" + extendedDataType + "From" + baseDataType + "(theAttrsArray[i]);");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print("");
        theJavaChunk.print("return (newRecords);");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();

        theJavaChunk.print(" ");
        if (comments) {
            theJavaChunk.print("/**");
            theJavaChunk.print("* Static method to cast ReadOnlyRowSet to Array of " + extendedDataType);
            theJavaChunk.print("* @param com.mcpdbwizard.pub.ReadOnlyRowSet theRowSet");
            theJavaChunk.print("* @return " + extendedDataType + "[]");
            theJavaChunk.print("* @since Build 4.0.2153");
            theJavaChunk.print("*/");
        }

        theJavaChunk.print("public static " + extendedDataType + "[]  create" + extendedDataType + "ArrayFromRowSet(com.mcpdbwizard.pub.ReadOnlyRowSet theRowSet");
        theJavaChunk.print("                                                                               ,com.mcpdbwizard.pub.LogInterface theLog) throws CSException");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print(extendedDataType + "[] newRecords = null;");
        theJavaChunk.print(" ");
        theJavaChunk.print("if (theRowSet == null)");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("return(newRecords);");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print(" ");
        theJavaChunk.print("try");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("newRecords = new " + extendedDataType + "[theRowSet.size()];");
        theJavaChunk.print(" ");

        theJavaChunk.print("for (int i=0; i < theRowSet.size(); i++)");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("theRowSet.setCurrentRowNumber(i);");
        theJavaChunk.print("newRecords[i] = new " + extendedDataType + "(theLog,theRowSet.getCurrentRow());");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print("");
        theJavaChunk.print("return (newRecords);");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print("catch (Exception e)");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("theLog.error(\"" + extendedDataType + ".create" + extendedDataType + "ArrayFromRowSet:\");");
        theJavaChunk.print("theLog.error(e);");
        theJavaChunk.print("throw new CSException(\"" + extendedDataType + ".create" + extendedDataType + "ArrayFromRowSet:\"+e.getMessage());");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
    }

    void addVariables(JavaChunk theJavaChunk
            , JavaChunk theJavaAttrsChunk
            , boolean comments
            , boolean makeVarsPublic
            , boolean webServices
            , String webServiceRecType) {
        addVariables(theJavaChunk
                , theJavaAttrsChunk
                , comments
                , makeVarsPublic
                , false, webServices, webServiceRecType);
    }

    void addVariables(JavaChunk theJavaChunk
            , JavaChunk theJavaAttrsChunk
            , boolean comments
            , boolean makeVarsPublic
            , boolean skipDups
            , boolean webServices
            , String webServiceRecType) {
        // ignore makeVarsPublic...
        boolean newMakevarsPublic = true;

        if (!webServiceRecType.equals("public")) {
            newMakevarsPublic = false;
        }

        addFilenameVariables(theJavaChunk, theJavaAttrsChunk, comments, newMakevarsPublic, webServices);

        for (int i = 0; i < theRowSet.size(); i++) {
            try {
                theRowSet.setCurrentRowNumber(i);

                // Ignore this record if already seen...
                if (skipDups) {
                    //if variable name has already been seen do nothing...
                    String currentVariableName = new String(variableName[i]);

                    for (int j = 0; j < i; j++) {
                        //theRowSet.setCurrentRowNumber(j);
                        if (variableName[j].equals(currentVariableName)) {
                            throw (new CSSkipSectionException());
                        }
                    }

                    //theRowSet.setCurrentRowNumber(i);
                }

                String accessMode = "protected";
                if (makeVarsPublic) {
                    accessMode = "public";
                }

                if (webServices) {
                    if (webServiceRecType.equals("public")) {
                        accessMode = new String(webServiceRecType);
                    } else {
                        accessMode = new String("protected");
                    }

                }

                if (useVariable[i]) {

                    String desc = "";
                    if (theRowSet.getInt("POSITION") == 0) {
                        // We are dealing with a function - position 0 is the value to be returned.
                        desc = "* Variable to store result of calling function";
                    } else {
                        desc = "* Variable to store " + theRowSet.getString("ARGUMENT_NAME");
                    }

                    if (comments) {
                        if (variableDataType[i].equalsIgnoreCase("com.mcpdbwizard.pub.PlsqlIndexByTable2")) {
                            theJavaAttrsChunk.print(" ");
                            theJavaAttrsChunk.print("/**");
                            theJavaAttrsChunk.print(desc);
                            // this must be a PLSQL Index By Table
                            theJavaAttrsChunk.print("* " + variableName[i] + " is used to store the Index By Array");
                            theJavaAttrsChunk.print("*/");
                        } else if (variableDataType[i].equalsIgnoreCase("java.util.Date")) {
                            theJavaAttrsChunk.print(" ");
                            theJavaAttrsChunk.print("/**");
                            theJavaAttrsChunk.print(desc);
                            // Date has changed
                            theJavaAttrsChunk.print("* @since 4.0.2016 DATE columns are now represented with java.util.Date instead of java.sql.Timestamp");
                            theJavaAttrsChunk.print("*/");
                        } else if (variableDataType[i].equalsIgnoreCase("java.io.File")) {
                            hasFiles = true;
                            theJavaChunk.print(" ");
                            theJavaChunk.print("/**");
                            theJavaChunk.print(desc);
                            theJavaChunk.print("*/");
                        } else {
                            theJavaAttrsChunk.print(" ");
                            theJavaAttrsChunk.print("/**");
                            theJavaAttrsChunk.print(desc);
                            switch (oracleUnderlyingDatatype[i]) {
                                case SqlUtils.ORACLE_INTERVAL_DAY_TO_SECOND_DATATYPE: {
                                    theJavaAttrsChunk.print("* This value is a String representation of an INTERVAL DAY TO SECOND data type. ");
                                    theJavaAttrsChunk.print("* An interval of two days, three hours and 2.99 seconds would be stored as:");
                                    theJavaAttrsChunk.print("* \"2 3:0:2.99\"");
                                    theJavaAttrsChunk.print("* @since 4.0.2080 INTERVAL DAY TO SECOND is stored as a String to facilitate serialization.");
                                    break;
                                }
                                case SqlUtils.ORACLE_INTERVAL_YEAR_TO_MONTH_DATATYPE: {
                                    theJavaAttrsChunk.print("* This value is a String representation of an INTERVAL YEAR TO MONTH data type. ");
                                    theJavaAttrsChunk.print("* An interval of two years and three months would be stored as:");
                                    theJavaAttrsChunk.print("* \"2-3\"");
                                    theJavaAttrsChunk.print("* @since 4.0.2080 INTERVAL YEAR TO MONTH is stored as a String to facilitate serialization.");
                                    break;
                                }
                                case SqlUtils.ORACLE_TIMESTAMP_DATATYPE: {
                                    theJavaAttrsChunk.print("* This value is a String representation of an oracle.sql.TIMESTAMP data type. ");
                                    theJavaAttrsChunk.print("* The format used is 'yyyy-mm-dd hh:mm:ss.fffffffff'. ");
                                    theJavaAttrsChunk.print("* An oracle.sql.TIMESTAMP representing 9:26AM and 50.12346 seconds on ");
                                    theJavaAttrsChunk.print("* the 31st Jan, 1997  would be stored as: \"1997-1-31 9:26:50.123456000\"");
                                    theJavaAttrsChunk.print("* @see java.sql.Timestamp");
                                    theJavaAttrsChunk.print("* @since 4.0.2080 TIMESTAMP is stored as a String to facilitate serialization.");
                                    break;
                                }
                                case SqlUtils.ORACLE_TIMESTAMPTZ_DATATYPE: {
                                    theJavaAttrsChunk.print("* This value is a byte[] representation of an oracle.sql.TIMESTAMPTZ data type. ");
                                    theJavaAttrsChunk.print("* @since 4.0.2080 TIMESTAMP WITH TIME ZONE is stored as byte[] to facilitate serialization.");
                                    break;
                                }
                                case SqlUtils.ORACLE_TIMESTAMPLTZ_DATATYPE: {
                                    theJavaAttrsChunk.print("* This value is a byte[] representation of an oracle.sql.TIMESTAMPLTZ data type. ");
                                    theJavaAttrsChunk.print("* @since 4.0.2080 TIMESTAMP WITH LOCAL TIME ZONE is stored as byte[] to facilitate serialization.");
                                    break;
                                }
                                default: {
                                    break;
                                }
                            }
                            theJavaAttrsChunk.print("*/");
                        }


                    }

                    if (variableDataType[i].equalsIgnoreCase("com.mcpdbwizard.pub.PlsqlIndexByTable2")) {
                        //theJavaAttrsChunk.print(accessMode + " " + variableDataType[i] + " " + variableName[i] + " = new com.mcpdbwizard.pub.PlsqlIndexByTable();");
                        if (plsqlIndexByRealDataType[i] == oracle.jdbc.OracleTypes.NUMBER) {
                            theJavaAttrsChunk.print(accessMode + " " + variableDataType[i] + " " + variableName[i]);
                            theJavaAttrsChunk.print(" = new com.mcpdbwizard.pub.PlsqlIndexByTable2(oracle.jdbc.OracleTypes.NUMBER," + plsqlIndexByDataDecPlaces[i] + ");");
                        } else {
                            theJavaAttrsChunk.print(accessMode + " " + variableDataType[i] + " " + variableName[i]);
                            theJavaAttrsChunk.print(" = new com.mcpdbwizard.pub.PlsqlIndexByTable2(oracle.jdbc.OracleTypes.VARCHAR," + plsqlIndexByDataDecPlaces[i] + ");");
                        }
                    } else if (variableDataType[i].equalsIgnoreCase("java.io.File")) {
                        //theJavaChunk.print(accessMode + " " + variableDataType[i] + " " + variableName[i] + " = null;");
                        theJavaChunk.print("public" + " " + variableDataType[i] + " " + variableName[i] + " = null;");
                    } else {
                        theJavaAttrsChunk.print(accessMode + " " + variableDataType[i] + " " + variableName[i] + " = null;");
                    }

                    if (oracleUnderlyingDatatype[i] == SqlUtils.ORACLE_VECTOR_DATATYPE
                            && variableDataType[i].equals("double[]")) {
                        // An unconstrained VECTOR parameter is format-flexible (PL/SQL forbids
                        // VECTOR(n, BINARY) parameter constraints), so a binary (bit-packed)
                        // vector value travels as byte[] through this companion field. Exactly
                        // one of the dense/binary fields is non-null at a time. (The double[]
                        // check skips non-scalar rows that mis-classify as VECTOR here.)
                        if (comments) {
                            theJavaAttrsChunk.print(" ");
                            theJavaAttrsChunk.print("/**");
                            theJavaAttrsChunk.print(desc + " as a BINARY (bit-packed) vector");
                            theJavaAttrsChunk.print("*/");
                        }
                        theJavaAttrsChunk.print(accessMode + " byte[] " + variableName[i] + "VectorBinary = null;");
                        // ...and a sparse (VECTOR(n, t, SPARSE)) value travels as a SparseVector
                        // through this second companion field. Still exactly one of the three
                        // dense/binary/sparse fields is non-null at a time.
                        if (comments) {
                            theJavaAttrsChunk.print(" ");
                            theJavaAttrsChunk.print("/**");
                            theJavaAttrsChunk.print(desc + " as a SPARSE vector");
                            theJavaAttrsChunk.print("*/");
                        }
                        theJavaAttrsChunk.print(accessMode + " com.mcpdbwizard.pub.SparseVector " + variableName[i] + "VectorSparse = null;");
                    }

                    if (oracleParamDatatype[i].equals("SqlUtils.ORACLE_TABLE_DATATYPE")
                            || oracleParamDatatype[i].equals("SqlUtils.ORACLE_OBJECT_DATATYPE")
                            || oracleParamDatatype[i].equals("SqlUtils.ORACLE_PLSQL_INDEXBY_ROWTYPE_DATATYPE")
                            || oracleParamDatatype[i].equals("SqlUtils.ORACLE_VARRAY_DATATYPE")
                    ) {
                        if (extraObjectId[i] > -1
                                && theRecords[extraObjectId[i]] != null
                                && theRecords[extraObjectId[i]].usable
                                && theRecords[extraObjectId[i]].typeImplementingClass != null
                                && theRecords[extraObjectId[i]].typeImplementingClass.theEngine != null
                                && theRecords[extraObjectId[i]].typeImplementingClass.theEngine.hasFiles) {
                            hasChildFiles = true;
                        }
                    } else if (oracleParamDatatype[i].equals("SqlUtils.ORACLE_ROWTYPE_DATATYPE")
                    ) {
                        if (extraObjectId[i] > -1
                                && theRecords[extraObjectId[i]] != null
                                && theRecords[extraObjectId[i]].usable
                                //   && theRecords[extraObjectId[i]].typeImplementingClass != null
                                && theRecords[extraObjectId[i]].theEngine != null
                                && theRecords[extraObjectId[i]].theEngine.hasFiles) {
                            hasChildFiles = true;
                        }
                    }

                } else {
                    if (comments)
                        theJavaAttrsChunk.print("// " + variableDataType[i] + " " + variableName[i] + " not needed in this case.");
                }
            } catch (CSSkipSectionException e) {
            } catch (CSNoDataInRowSetException e) {
                if (comments) theJavaChunk.print("// This procedure has no parameters");
            } catch (CSException e) {
                theLog.syserror(e, true, true);
            }

        }

        if ((hasFiles || hasChildFiles) && type == IS_A_FUNCTION) {

            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* Variable to store Buffer Size for file access.");
                theJavaChunk.print("*/");
            }

            theJavaChunk.print("protected " + "int" + " " + sbufferSize + " = 4096;");

            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* boolean flag that specifies whether lobs such as CLOBS, BLOBS and BFILES");
                theJavaChunk.print("* will be kept as Oracle objects or downloaded into Files.");
                theJavaChunk.print("* @since 4.0.1847");
                theJavaChunk.print("*/");
            }

            theJavaChunk.print("protected " + "boolean" + " " + skeepLobs + " = false;");


            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* boolean flag that controls whether LOBs are turned into byte[]");
                theJavaChunk.print("* @since 5.0.2314");
                theJavaChunk.print("*/");
            }

            theJavaChunk.print("protected " + "boolean" + " " + "useByteArraysForLongsAndLOBS" + " = false;");

            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* Variable to store boolean flag that indicates whether created files should be deleted when the JVM exits.");
                theJavaChunk.print("*/");
            }

            theJavaChunk.print("protected " + "boolean" + " " + skeepFiles + " = true;");

            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* Variable to store temporary directory for downloaded files.");
                theJavaChunk.print("*/");
            }

            theJavaChunk.print("protected " + "java.io.File" + " " + stempFileDir + " = com.mcpdbwizard.pub.IOUtils.getOsTempDir();");

            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* Variable to store Long object loader.");
                theJavaChunk.print("*/");
            }

            theJavaChunk.print("protected " + "com.mcpdbwizard.pub.LongObjectLoader" + " " + sobjectLoader + " = new com.mcpdbwizard.pub.LongObjectLoader();");

            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* The Prefix for downloaded files containing BLOB, CLOB and BFILE data");
                theJavaChunk.print("*/");
            }

            theJavaChunk.print("protected String " + stempFilePrefix + " = \"" + configTempFilePrefix + "\";");

            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* The Suffix for downloaded files containing BLOB, CLOB and BFILE data");
                theJavaChunk.print("*/");
            }

            theJavaChunk.print("protected String " + stempFileSuffix + " = \"" + configTempFileSuffix + "\";");


            if (!targetVersion.startsWith("8")) {
                theJavaChunk.print("");
                if (comments) {
                    theJavaChunk.print("/**");
                    theJavaChunk.print("* If set to true this flag causes Temporary LOB's to be created if no ");
                    theJavaChunk.print("* LOB parameter has been set by the time the database is accessed ");
                    theJavaChunk.print("* @since 4.0.2107 / Oracle 9.0.1");
                    theJavaChunk.print("*/");
                }

                theJavaChunk.print("protected boolean createTempLobsIfNeeded = true;");

                theJavaChunk.print("");
                if (comments) {
                    theJavaChunk.print("/**");
                    theJavaChunk.print("* If true this flag causes Temporary LOB's to be deleted after ");
                    theJavaChunk.print("* the database is accessed ");
                    theJavaChunk.print("* @since 4.0.2107 / Oracle 9.0.1");
                    theJavaChunk.print("*/");
                }

                theJavaChunk.print("protected boolean deleteTempLobsAfterCall = false;");

            }
        }

        if (hasRowSets) {
            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* Variable to store max rows for ReadOnlyRowSet");
                theJavaChunk.print("*/");
            }

            theJavaChunk.print("protected " + "int" + " " + smaxRows + " = 10000;");
        }
    }

    void addObjectArrayGet(JavaChunk theJavaChunk, boolean comments) {
        if (comments) {
            theJavaChunk.print("/**");
            theJavaChunk.print("* Method to get all variables by returning an array of Object ");
            if (hasFiles) {
                theJavaChunk.print("* This method returns LOBs, not their associated files.");
            }
            theJavaChunk.print("* @return " + "Object[]");
            theJavaChunk.print("* @param " + "java.sql.Connection");
            theJavaChunk.print("* @since V5.0.2192 fixed java.sql.Date bug - now uses java.sql.Timestamp");
            theJavaChunk.print("* @since V6.0.2819 added support for SDO_GEOMETRY");
            theJavaChunk.print("*/");
        }

        theJavaChunk.print("public Object[] "
                + "getCurrentValues" + "(java.sql.Connection theConnection) ");
        theJavaChunk.indent();
        theJavaChunk.print("{");

        if (comments) {
            theJavaChunk.print("// Create temp array that has right number of elements");
        }
        theJavaChunk.print("Object[] tempObjectArray = new Object[" + theRowSet.size() + "];");
        theJavaChunk.print(" ");


        if (comments) {
            theJavaChunk.print("// load values into array");
            theJavaChunk.print(" ");
        }

        boolean doesntNeedConnection = true;

        for (int i = 0; i < theRowSet.size(); i++) {
            theRowSet.setCurrentRowNumber(i);
            switch (oracleUnderlyingDatatype[i]) {
                case SqlUtils.ORACLE_CLOB_DATATYPE:
                case SqlUtils.ORACLE_BLOB_DATATYPE:
                case SqlUtils.ORACLE_BFILE_DATATYPE:
                case SqlUtils.ORACLE_XMLTYPE_DATATYPE: {
                    theJavaChunk.print("tempObjectArray[" + i + "] = " + lobName[i] + ";");
                    break;
                }
                case SqlUtils.ORACLE_DATE_DATATYPE: {
                    theJavaChunk.print("");
                    theJavaChunk.print("if (" + variableName[i] + " == null)");
                    theJavaChunk.indent();
                    theJavaChunk.print("{");
                    theJavaChunk.print("tempObjectArray[" + i + "] = (java.sql.Timestamp)null;");
                    theJavaChunk.print("}");
                    theJavaChunk.unIndent();
                    theJavaChunk.print("else");
                    theJavaChunk.indent();
                    theJavaChunk.print("{");
                    theJavaChunk.print("tempObjectArray[" + i + "] = new java.sql.Timestamp(" + variableName[i] + ".getTime());");
                    theJavaChunk.print("}");
                    theJavaChunk.unIndent();
                    theJavaChunk.print("");
                    break;
                }
                case SqlUtils.ORACLE_SDO_GEOMETRY_DATATYPE: {
                    doesntNeedConnection = false;
                    theJavaChunk.print(" ");
                    if (comments) theJavaChunk.print("// Wrap SDO_GEOMETRY");


                    theJavaChunk.print("try ");
                    theJavaChunk.indent();
                    theJavaChunk.print("{");
                    theJavaChunk.print("if (" + variableName[i] + " != null)");
                    theJavaChunk.indent();
                    theJavaChunk.print("{");
                    //theJavaChunk.indent();
                    theJavaChunk.print("oracle.spatial.geometry.JGeometry theGeo =");
                    theJavaChunk.print("  JGeometryWrapper.jGeometryUnWrapper(" + variableName[i] + ");");
                    theJavaChunk.print("tempObjectArray[" + i + "]  = oracle.spatial.geometry.JGeometry.store(theConnection, theGeo);");
                    theJavaChunk.print("}");
                    theJavaChunk.unIndent();
                    theJavaChunk.print("} ");
                    theJavaChunk.unIndent();
                    theJavaChunk.print("catch (Exception e) ");
                    theJavaChunk.indent();
                    theJavaChunk.print("{");
                    if (comments)
                        theJavaChunk.print("// This is an odd way to handle things but it will turn out ok....");
                    theJavaChunk.print("tempObjectArray[" + i + "] = e;");
                    theJavaChunk.print("}");
                    theJavaChunk.unIndent();
                    break;


                }
                default: {
                    theJavaChunk.print("tempObjectArray[" + i + "] = " + variableName[i] + ";");
                }
            }
        }

        theJavaChunk.print("return(tempObjectArray);");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();

        if (doesntNeedConnection) {
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* Method to get all variables by returning an array of Object ");
                if (hasFiles) {
                    theJavaChunk.print("* This method returns LOBs, not their associated files.");
                }
                theJavaChunk.print("* @return " + "Object[]");
                theJavaChunk.print("* @since V5.0.2192 fixed java.sql.Date bug - now uses java.sql.Timestamp");
                theJavaChunk.print("* @since V6.0.2819 retained after SDO_GEOMETRY support for backward compatibility");
                theJavaChunk.print("*/");
            }

            theJavaChunk.print("public Object[] "
                    + "getCurrentValues" + "() ");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("return(getCurrentValues(null));");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
        }


    }

    void addObjectArraySet(JavaChunk theJavaChunk, boolean comments) {
        if (comments) {
            theJavaChunk.print("/**");
            theJavaChunk.print("* Method to set all variables by passing in an array of Object ");
            if (hasFiles) {
                theJavaChunk.print("* This method expects LOBs, not their associated files.");
            }
            theJavaChunk.print("* @param " + "Object[]" + " " + "newValues");
            theJavaChunk.print("* @throws " + "CSException" + " " + "when the values cant be matched");
            theJavaChunk.print("*/");
        }

        theJavaChunk.print("public void "
                + "setNewValues" + "(" + "Object[]"
                + " " + "newValues" + ") throws CSException ");
        theJavaChunk.indent();
        theJavaChunk.print("{");

        if (comments) {
            theJavaChunk.print("// Check that array has right number of elements");
        }
        theJavaChunk.print("if (newValues == null || newValues.length != " + theRowSet.size() + ")");


        if (theRowSet.size() == 0) {
            theLog.error("Generated Object has array size of zero. Bug 213 detected. Contact support.");
        }

        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("throw new CSException(\"Array has incorrect number of elements\");");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print(" ");


        if (comments) {
            theJavaChunk.print("// Check that array elements have correct data types");
            theJavaChunk.print(" ");
        }
        String charDataType = "byte[]";
        if (useCharForCLOB) charDataType = "char[]";


        for (int i = 0; i < theRowSet.size(); i++) {
            try {
                theRowSet.setCurrentRowNumber(i);

                if (comments) {
                    theJavaChunk.print("// Check that " + theRowSet.getString("ARGUMENT_NAME") + " has right data type");
                }

                if (true || hasFiles) {
                    switch (oracleUnderlyingDatatype[i]) {
                        case SqlUtils.ORACLE_CLOB_DATATYPE: {
                            theJavaChunk.print("if (newValues[" + i + "] != null && ! (   newValues[" + i + "] instanceof " + "oracle.sql.CLOB");
                            theJavaChunk.print("                               || newValues[" + i + "] instanceof " + charDataType + "))");
                            break;
                        }
                        case SqlUtils.ORACLE_BLOB_DATATYPE: {
                            theJavaChunk.print("if (newValues[" + i + "] != null && ! (   newValues[" + i + "] instanceof " + "oracle.sql.BLOB");
                            theJavaChunk.print("                               || newValues[" + i + "] instanceof " + "byte[]" + "))");
                            break;
                        }
                        case SqlUtils.ORACLE_XMLTYPE_DATATYPE: {
                            theJavaChunk.print("if (newValues[" + i + "] != null && ! (   newValues[" + i + "] instanceof " + "oracle.sql.OPAQUE");
                            theJavaChunk.print("                               || newValues[" + i + "] instanceof " + charDataType + "))");
                            break;
                        }
                        case SqlUtils.ORACLE_SDO_GEOMETRY_DATATYPE: {
                            theJavaChunk.print("if (newValues[" + i + "] != null && ! (   newValues[" + i + "] instanceof " + "oracle.sql.STRUCT");
                            theJavaChunk.print("                               || newValues[" + i + "] instanceof JGeometryWrapper))");
                            break;
                        }
                        case SqlUtils.ORACLE_BFILE_DATATYPE: {
                            theJavaChunk.print("if (newValues[" + i + "] != null && ! (   newValues[" + i + "] instanceof " + "oracle.sql.BFILE");
                            theJavaChunk.print("                               || newValues[" + i + "] instanceof " + "byte[]" + "))");
                            break;
                        }
                        case SqlUtils.ORACLE_LONG_BINARY_DATATYPE: {
                            theJavaChunk.print("if (newValues[" + i + "] != null && ! (   newValues[" + i + "] instanceof " + "java.io.File");
                            theJavaChunk.print("                               || newValues[" + i + "] instanceof " + "byte[]" + "))");
                            break;
                        }
                        default: {
                            theJavaChunk.print("if (newValues[" + i + "] != null && ! (newValues[" + i + "] instanceof " + variableDataType[i] + "))");
                        }
                    }
                } else {
                    theJavaChunk.print("if (newValues[" + i + "] != null && ! (newValues[" + i + "] instanceof " + variableDataType[i] + "))");
                }

                theJavaChunk.indent();
                theJavaChunk.print("{");
                theJavaChunk.print("throw new CSException(\"new value for " + variableName[i] + " has incorrect data type" + "\");");
                theJavaChunk.print("}");
                theJavaChunk.unIndent();
                theJavaChunk.print(" ");


            } catch (CSNoDataInRowSetException e) {
            } catch (CSInvalidColumnIdException e) {
            } catch (CSDBInvalidDatatypeCastException e) {
            } catch (CSUnsupportedDatatypeException e) {
            }
        }

        if (comments) {
            theJavaChunk.print("// Copy array elements");
        }

        for (int i = 0; i < theRowSet.size(); i++) {
            theRowSet.setCurrentRowNumber(i);

            if (true || hasFiles) //DRKLUGE
            {
                switch (oracleUnderlyingDatatype[i]) {
                    case SqlUtils.ORACLE_CLOB_DATATYPE: {
                        theJavaChunk.print("if (newValues[" + i + "] instanceof " + charDataType + ")");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");
                        if (useCharForCLOB) {
                            theJavaChunk.print(byteName[i] + " = (char[])newValues[" + i + "];");
                        } else {
                            theJavaChunk.print(byteName[i] + " = (byte[])newValues[" + i + "];");
                        }

                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();
                        theJavaChunk.print("else");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");
                        theJavaChunk.print(lobName[i] + " = (" + "oracle.sql.CLOB" + ")newValues[" + i + "];");
                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();
                        theJavaChunk.print(" ");
                        break;
                    }
                    case SqlUtils.ORACLE_SDO_GEOMETRY_DATATYPE: {

                        /**
                         theJavaChunk.print("try {  ");
                         if (newValues[8] != null)     ");
                         {                   ");
                         oracle.spatial.geometry.JGeometry x; ");
                         x = oracle.spatial.geometry.JGeometry.load((oracle.sql.STRUCT)newValues[8]);  ");
                         paramAnSdogeomColumn = JGeometryWrapper.createWrappedClass(x);        ");
                         }           ");
                         else         ");
                         {              ");
                         paramAnSdogeomColumn = null;     ");
                         }
                         } catch (SQLException e) {
                         throw new CSException("Error while unloading paramAnSdogeomColumn: " + e.getMessage());
                         }
                         ***/
                        theJavaChunk.print("if (newValues[" + i + "] == null)");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");
                        theJavaChunk.print(variableName[i] + " = null;");
                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();
                        theJavaChunk.print("else if (newValues[" + i + "] instanceof JGeometryWrapper)");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");
                        theJavaChunk.print(variableName[i] + " = (JGeometryWrapper)newValues[" + i + "];");
                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();
                        theJavaChunk.print("else");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");


                        theJavaChunk.print("try");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");
                        theJavaChunk.print("oracle.spatial.geometry.JGeometry tempSdo = null;");
                        theJavaChunk.print("tempSdo = oracle.spatial.geometry.JGeometry.load((oracle.sql.STRUCT)newValues[" + i + "]); ");
                        theJavaChunk.print(variableName[i] + " = JGeometryWrapper.createWrappedClass(tempSdo);");
                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();
                        theJavaChunk.print("catch (java.sql.SQLException e)");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");
                        theJavaChunk.print("throw new CSException(\"Error while unloading " + byteName[i] + " : \" + e.getMessage());");
                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();


                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();
                        theJavaChunk.print(" ");
                        break;
                    }
                    case SqlUtils.ORACLE_XMLTYPE_DATATYPE: {
                        theJavaChunk.print("if (newValues[" + i + "] instanceof " + charDataType + ")");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");
                        if (useCharForCLOB) {
                            theJavaChunk.print(byteName[i] + " = (char[])newValues[" + i + "];");
                        } else {
                            theJavaChunk.print(byteName[i] + " = (byte[])newValues[" + i + "];");
                        }
                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();
                        theJavaChunk.print("else");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");
                        theJavaChunk.print(lobName[i] + " = (" + "oracle.sql.OPAQUE" + ")newValues[" + i + "];");
                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();
                        theJavaChunk.print(" ");
                        break;
                    }
                    case SqlUtils.ORACLE_BLOB_DATATYPE: {
                        theJavaChunk.print("if (newValues[" + i + "] instanceof byte[])");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");
                        theJavaChunk.print(byteName[i] + " = (byte[])newValues[" + i + "];");
                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();
                        theJavaChunk.print("else");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");
                        theJavaChunk.print(lobName[i] + " = (" + "oracle.sql.BLOB" + ")newValues[" + i + "];");
                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();
                        theJavaChunk.print(" ");
                        break;
                    }
                    case SqlUtils.ORACLE_BFILE_DATATYPE: {
                        theJavaChunk.print("if (newValues[" + i + "] instanceof byte[])");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");
                        theJavaChunk.print(byteName[i] + " = (byte[])newValues[" + i + "];");
                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();
                        theJavaChunk.print("else");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");
                        theJavaChunk.print(lobName[i] + " = (" + "oracle.sql.BFILE" + ")newValues[" + i + "];");
                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();
                        theJavaChunk.print(" ");
                        break;
                    }
                    case SqlUtils.ORACLE_LONG_BINARY_DATATYPE: {
                        theJavaChunk.print("if (newValues[" + i + "] instanceof byte[])");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");
                        theJavaChunk.print(byteName[i] + " = (byte[])newValues[" + i + "];");
                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();
                        theJavaChunk.print("else");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");
                        theJavaChunk.print(variableName[i] + " = (" + "java.io.File" + ")newValues[" + i + "];");
                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();
                        theJavaChunk.print(" ");
                        break;
                    }
                    default: {
                        theJavaChunk.print(variableName[i] + " = (" + variableDataType[i] + ")newValues[" + i + "];");
                    }
                }
            } else {
                theJavaChunk.print(variableName[i] + " = (" + variableDataType[i] + ")newValues[" + i + "];");
            }
        }


        theJavaChunk.print("}");
        theJavaChunk.unIndent();
    }

    void addSetMethods(String webServiceRecType, JavaChunk theJavaChunk, JavaChunk theJavaAttrsChunk, boolean comments, boolean debugMessages, boolean otherMessages) {
        addSetMethods(webServiceRecType, theJavaChunk, theJavaAttrsChunk, comments, debugMessages, otherMessages, false);
    }

    void addSetMethods(String webServiceRecType, JavaChunk theJavaChunk, JavaChunk theJavaAttrsChunk, boolean comments, boolean debugMessages, boolean otherMessages, boolean skipDups) {
        for (int i = 0; i < theRowSet.size(); i++) {
            String setMethod = JavaUtils.getJavaName("set_" + variableName[i], javaNamingConvention, theLog);
            String setNullMethod = JavaUtils.getJavaName("set_" + variableName[i] + "_to_null", javaNamingConvention, theLog);


            try {
                theRowSet.setCurrentRowNumber(i);

                // Ignore this record if already seen...
                if (skipDups) {
                    //if variable name has already been seen do nothing...
                    String currentVariableName = new String(variableName[i]);

                    for (int j = 0; j < i; j++) {
                        //theRowSet.setCurrentRowNumber(j);
                        if (variableName[j].equals(currentVariableName)) {
                            throw (new CSSkipSectionException());
                        }
                    }

                    //theRowSet.setCurrentRowNumber(i);
                }

                if (theRowSet.getInt("POSITION") > 0 && (theRowSet.getString("IN_OUT").equals("IN")
                        || theRowSet.getString("IN_OUT").equals("IN/OUT")
                        || overRideInOut.equals("IN/OUT"))
                ) {

                    switch (oracleUnderlyingDatatype[i]) {
                        case SqlUtils.ORACLE_NUMBER_DATATYPE: {
                            final String[] other_math_object_datatypes = {"Double", "Float", "Long", "Integer", "Short", "Byte"};
                            final String[] other_math_scaler_datatypes = {"double", "float", "long", "int", "short", "byte"};

                            for (int dtype = 0; dtype < other_math_object_datatypes.length; dtype++) {
                                if (!numberDataTypes.getProperty(other_math_object_datatypes[dtype], "NONE").equals("NONE")) {
                                    addObjectScalerSetMethod(theJavaChunk, setMethod, "java.math.BigDecimal", ".doubleValue()", theRowSet.getString("ARGUMENT_NAME")
                                            , other_math_object_datatypes[dtype], variableName[i], comments);
                                }
                            }

                            for (int dtype = 0; dtype < other_math_scaler_datatypes.length; dtype++) {
                                if (!numberDataTypes.getProperty(other_math_scaler_datatypes[dtype], "NONE").equals("NONE")) {
                                    addScalarSetMethod(theJavaChunk, setMethod, "java.math.BigDecimal", "double", theRowSet.getString("ARGUMENT_NAME")
                                            , other_math_scaler_datatypes[dtype], variableName[i], comments);
                                }
                            }

                            setNullMethod = JavaUtils.getJavaName("set_" + variableName[i] + "_to_null", javaNamingConvention, theLog);
                            setMethod = JavaUtils.getJavaName("set_" + variableName[i], javaNamingConvention, theLog);
                            if (webServiceRecType.equals("public")) {
                                addObjectScalerSetMethod(theJavaChunk, setMethod, "java.math.BigDecimal", ".doubleValue()", theRowSet.getString("ARGUMENT_NAME")
                                        , "java.math.BigDecimal", variableName[i], comments);

                                addScalarSetNullMethod(theJavaChunk, setNullMethod, theRowSet.getString("ARGUMENT_NAME"), variableName[i], comments);
                            } else {
                                addObjectScalerSetMethod(theJavaChunk, setMethod, "java.math.BigDecimal", ".doubleValue()", theRowSet.getString("ARGUMENT_NAME")
                                        , "java.math.BigDecimal", variableName[i], comments);

                                addScalarSetNullMethod(theJavaChunk, setNullMethod, theRowSet.getString("ARGUMENT_NAME"), variableName[i], comments);
                                //setMethod = JavaUtils.getJavaName("set_" + variableName[i] + "", javaNamingConvention, theLog);
                                //addObjectScalerSetMethod(theJavaAttrsChunk, setMethod ,"java.math.BigDecimal",".doubleValue()",theRowSet.getString("ARGUMENT_NAME")
                                //    , "java.math.BigDecimal", variableName[i],comments);
                            }
                            break;
                        }
                        case SqlUtils.ORACLE_DATE_DATATYPE: {
                            if (webServiceRecType.equals("public")) {
                                addDateSetMethods(theJavaChunk, theJavaChunk, setMethod, theRowSet.getString("ARGUMENT_NAME"), variableDataType[i]
                                        , variableName[i], comments);
                            } else {
                                addDateSetMethods(theJavaChunk, theJavaAttrsChunk, setMethod, theRowSet.getString("ARGUMENT_NAME"), variableDataType[i]
                                        , variableName[i], comments);
                            }
                            setNullMethod = JavaUtils.getJavaName("set_" + variableName[i] + "_to_null", javaNamingConvention, theLog);
                            addScalarSetNullMethod(theJavaChunk, setNullMethod, theRowSet.getString("ARGUMENT_NAME"), variableName[i], comments);
                            break;
                        }
                        case SqlUtils.ORACLE_TEXT_DATATYPE: {
                            if (webServiceRecType.equals("public")) {
                                addObjectScalerSetMethod(theJavaChunk, setMethod, variableDataType[i], "", theRowSet.getString("ARGUMENT_NAME")
                                        , variableDataType[i], variableName[i], comments);
                            } else {
                                addObjectScalerSetMethod(theJavaAttrsChunk, setMethod, variableDataType[i], "", theRowSet.getString("ARGUMENT_NAME")
                                        , variableDataType[i], variableName[i], comments);
                            }
                            setNullMethod = JavaUtils.getJavaName("set_" + variableName[i] + "_to_null", javaNamingConvention, theLog);
                            addScalarSetNullMethod(theJavaChunk, setNullMethod, theRowSet.getString("ARGUMENT_NAME"), variableName[i], comments);
                            break;
                        }
                        case SqlUtils.ORACLE_PLSQL_INDEXBY_DATATYPE: {
                            if (webServiceRecType.equals("public")) {
                                addObjectScalerSetMethodPLSQLIBTable(theJavaChunk, setMethod, theRowSet.getString("ARGUMENT_NAME"), variableDataType[i], variableName[i], comments, plsqlIndexByRealDataType[i]);
                            } else {
                                addObjectScalerSetMethodPLSQLIBTable(theJavaAttrsChunk, setMethod, theRowSet.getString("ARGUMENT_NAME"), variableDataType[i], variableName[i], comments, plsqlIndexByRealDataType[i]);
                            }
                            //setNullMethod = JavaUtils.getJavaName("set_" + variableName[i] +"_to_null", javaNamingConvention, theLog);
                            //addScalarSetNullMethod(theJavaChunk, setNullMethod,theRowSet.getString("ARGUMENT_NAME"),variableName[i],comments);
                            break;
                        }
                        case SqlUtils.ORACLE_LONGTEXT_DATATYPE:
                        case SqlUtils.ORACLE_LONG_BINARY_DATATYPE: {
                            //addScalarSetMethod(theJavaAttrsChunk, setMethod,"byte[]","byte[]",theRowSet.getString("ARGUMENT_NAME")
                            //             , "byte[]", byteName[i],comments);
                            if (webServiceRecType.equals("public")) {
                                addFileSetMethod(theJavaChunk, theJavaChunk, setMethod, theRowSet.getString("ARGUMENT_NAME")
                                        , variableDataType[i], variableName[i], otherName[i], theRowSet.getString("IN_OUT"), comments, actualOracleDatatype[i], byteName[i]);
                            } else {
                                addFileSetMethod(theJavaChunk, theJavaAttrsChunk, setMethod, theRowSet.getString("ARGUMENT_NAME")
                                        , variableDataType[i], variableName[i], otherName[i], theRowSet.getString("IN_OUT"), comments, actualOracleDatatype[i], byteName[i]);
                            }
                            setNullMethod = JavaUtils.getJavaName("set_" + variableName[i] + "_to_null", javaNamingConvention, theLog);
                            addScalarSetNullMethod(theJavaChunk, setNullMethod, theRowSet.getString("ARGUMENT_NAME"), variableName[i], comments);
                            break;
                        }
                        case SqlUtils.ORACLE_CLOB_DATATYPE:
                        case SqlUtils.ORACLE_BLOB_DATATYPE: {
                            //addScalarSetMethod(theJavaAttrsChunk, setMethod,"byte[]","byte[]",theRowSet.getString("ARGUMENT_NAME")
                            //             , "byte[]", byteName[i],comments);
                            if (webServiceRecType.equals("public")) {
                                addFileSetMethod(theJavaChunk, theJavaChunk, setMethod, theRowSet.getString("ARGUMENT_NAME")
                                        , variableDataType[i], variableName[i], otherName[i], theRowSet.getString("IN_OUT"), comments, actualOracleDatatype[i], byteName[i]);
                            } else {
                                addFileSetMethod(theJavaChunk, theJavaAttrsChunk, setMethod, theRowSet.getString("ARGUMENT_NAME")
                                        , variableDataType[i], variableName[i], otherName[i], theRowSet.getString("IN_OUT"), comments, actualOracleDatatype[i], byteName[i]);
                            }

                            addLobSetMethod(theJavaChunk, setMethod, theRowSet.getString("ARGUMENT_NAME"), actualOracleDatatype[i]
                                    , variableName[i], lobName[i], theRowSet.getString("IN_OUT"), comments);

                            setNullMethod = JavaUtils.getJavaName("set_" + variableName[i] + "_to_null", javaNamingConvention, theLog);
                            addScalarSetNullMethod(theJavaChunk, setNullMethod, theRowSet.getString("ARGUMENT_NAME"), variableName[i], comments);

                            setNullMethod = JavaUtils.getJavaName("set_" + lobName[i] + "_to_null", javaNamingConvention, theLog);
                            addScalarSetNullMethod(theJavaChunk, setNullMethod, theRowSet.getString("ARGUMENT_NAME"), lobName[i], comments);

                            break;
                        }
                        case SqlUtils.ORACLE_BFILE_DATATYPE: {
                            if (useVariable[i]) {
                                //addScalarSetMethod(theJavaAttrsChunk, setMethod,"byte[]","byte[]",theRowSet.getString("ARGUMENT_NAME")
                                //           , "byte[]", byteName[i],comments);
                                if (webServiceRecType.equals("public")) {
                                    addFileSetMethod(theJavaChunk, theJavaChunk, setMethod, theRowSet.getString("ARGUMENT_NAME")
                                            , variableDataType[i], variableName[i], otherName[i], theRowSet.getString("IN_OUT"), comments, actualOracleDatatype[i], byteName[i]);
                                } else {
                                    addFileSetMethod(theJavaChunk, theJavaAttrsChunk, setMethod, theRowSet.getString("ARGUMENT_NAME")
                                            , variableDataType[i], variableName[i], otherName[i], theRowSet.getString("IN_OUT"), comments, actualOracleDatatype[i], byteName[i]);
                                }

                                setNullMethod = JavaUtils.getJavaName("set_" + variableName[i] + "_to_null", javaNamingConvention, theLog);
                                addScalarSetNullMethod(theJavaChunk, setNullMethod, theRowSet.getString("ARGUMENT_NAME"), variableName[i], comments);

                            }
                            addLobSetMethod(theJavaChunk, setMethod, theRowSet.getString("ARGUMENT_NAME"), actualOracleDatatype[i]
                                    , variableName[i], lobName[i], theRowSet.getString("IN_OUT"), comments);

                            setNullMethod = JavaUtils.getJavaName("set_" + lobName[i] + "_to_null", javaNamingConvention, theLog);
                            addScalarSetNullMethod(theJavaChunk, setNullMethod, theRowSet.getString("ARGUMENT_NAME"), lobName[i], comments);
                            break;
                        }
                        case SqlUtils.ORACLE_ROWID_DATATYPE: {
                            addObjectScalerSetMethod(theJavaChunk, setMethod, variableDataType[i], ".getBytes()", theRowSet.getString("ARGUMENT_NAME")
                                    , variableDataType[i], variableName[i], comments);
                            if (webServiceRecType.equals("public")) {
                                addObjectScalerSetMethod(theJavaChunk, setMethod, variableDataType[i], ".getBytes()", theRowSet.getString("ARGUMENT_NAME")
                                        , "String", variableName[i], comments);
                            } else {
                                addObjectScalerSetMethod(theJavaAttrsChunk, setMethod, variableDataType[i], ".getBytes()", theRowSet.getString("ARGUMENT_NAME")
                                        , "String", variableName[i], comments);
                            }
                            setNullMethod = JavaUtils.getJavaName("set_" + variableName[i] + "_to_null", javaNamingConvention, theLog);
                            addScalarSetNullMethod(theJavaChunk, setNullMethod, theRowSet.getString("ARGUMENT_NAME"), variableName[i], comments);
                            break;
                        }
                        case SqlUtils.ORACLE_UROWID_DATATYPE: {
                            if (webServiceRecType.equals("public")) {
                                addObjectScalerSetMethod(theJavaChunk, setMethod, variableDataType[i], ".getBytes()", theRowSet.getString("ARGUMENT_NAME")
                                        , variableDataType[i], variableName[i], comments);
                            } else {
                                addObjectScalerSetMethod(theJavaAttrsChunk, setMethod, variableDataType[i], ".getBytes()", theRowSet.getString("ARGUMENT_NAME")
                                        , variableDataType[i], variableName[i], comments);
                            }
                            setNullMethod = JavaUtils.getJavaName("set_" + variableName[i] + "_to_null", javaNamingConvention, theLog);
                            addScalarSetNullMethod(theJavaChunk, setNullMethod, theRowSet.getString("ARGUMENT_NAME"), variableName[i], comments);
                            break;
                        }
                        case SqlUtils.ORACLE_BINARY_DATATYPE: {
                            //setMethod = JavaUtils.getJavaName("set_" + variableName[i] + "_byte_array" ,javaNamingConvention, theLog);
                            if (webServiceRecType.equals("public")) {
                                addScalarSetMethod(theJavaChunk, setMethod, variableDataType[i], variableDataType[i], theRowSet.getString("ARGUMENT_NAME")
                                        , variableDataType[i], variableName[i], comments);
                            } else {
                                addScalarSetMethod(theJavaAttrsChunk, setMethod, variableDataType[i], variableDataType[i], theRowSet.getString("ARGUMENT_NAME")
                                        , variableDataType[i], variableName[i], comments);
                            }
                            setNullMethod = JavaUtils.getJavaName("set_" + variableName[i] + "_to_null", javaNamingConvention, theLog);
                            addScalarSetNullMethod(theJavaChunk, setNullMethod, theRowSet.getString("ARGUMENT_NAME"), variableName[i], comments);
                            break;
                        }
                        case SqlUtils.ORACLE_BOOLEAN_DATATYPE: {
                            addObjectScalerSetMethod(theJavaChunk, setMethod, variableDataType[i], ".booleanValue()", theRowSet.getString("ARGUMENT_NAME")
                                    , variableDataType[i], variableName[i], comments);
                            if (webServiceRecType.equals("public")) {
                                addScalarSetMethod(theJavaChunk, setMethod, "Boolean", "boolean", theRowSet.getString("ARGUMENT_NAME")
                                        , "boolean", variableName[i], comments);
                            } else {
                                addScalarSetMethod(theJavaAttrsChunk, setMethod, "Boolean", "boolean", theRowSet.getString("ARGUMENT_NAME")
                                        , "boolean", variableName[i], comments);
                            }
                            setNullMethod = JavaUtils.getJavaName("set_" + variableName[i] + "_to_null", javaNamingConvention, theLog);
                            addScalarSetNullMethod(theJavaChunk, setNullMethod, theRowSet.getString("ARGUMENT_NAME"), variableName[i], comments);
                            break;
                        }
                        case SqlUtils.ORACLE_NULL_DATATYPE: {
                            if (comments) theJavaChunk.print("// This procedure has no parameters");
                            break;
                        }
                        case SqlUtils.ORACLE_TIMESTAMP_DATATYPE: {
                            if (webServiceRecType.equals("public")) {
                                addScalarSetMethod(theJavaChunk, setMethod, variableDataType[i], variableDataType[i], theRowSet.getString("ARGUMENT_NAME")
                                        , /*variableDataType[i]*/ "java.sql.Timestamp", variableName[i], comments);
                            } else {
                                addScalarSetMethod(theJavaAttrsChunk, setMethod, variableDataType[i], variableDataType[i], theRowSet.getString("ARGUMENT_NAME")
                                        , /*variableDataType[i]*/ "java.sql.Timestamp", variableName[i], comments);
                            }
                            if (!targetVersion.startsWith("DB2")) {
                                addScalarSetMethod(theJavaChunk, setMethod, variableDataType[i], variableDataType[i], theRowSet.getString("ARGUMENT_NAME")
                                        , /*variableDataType[i]*/ "oracle.sql.TIMESTAMP", variableName[i], comments);

                            }
                            setNullMethod = JavaUtils.getJavaName("set_" + variableName[i] + "_to_null", javaNamingConvention, theLog);
                            addScalarSetNullMethod(theJavaChunk, setNullMethod, theRowSet.getString("ARGUMENT_NAME"), variableName[i], comments);
                            break;
                        }
                        case SqlUtils.ORACLE_TIMESTAMPTZ_DATATYPE: {
                            addScalarSetMethod(theJavaChunk, setMethod, variableDataType[i], variableDataType[i], theRowSet.getString("ARGUMENT_NAME")
                                    , variableDataType[i], variableName[i], comments);
                            addScalarSetMethod(theJavaChunk, setMethod, variableDataType[i], variableDataType[i], theRowSet.getString("ARGUMENT_NAME")
                                    , /*variableDataType[i]*/ "oracle.sql.TIMESTAMPTZ", variableName[i], comments);
                            setNullMethod = JavaUtils.getJavaName("set_" + variableName[i] + "_to_null", javaNamingConvention, theLog);
                            addScalarSetNullMethod(theJavaChunk, setNullMethod, theRowSet.getString("ARGUMENT_NAME"), variableName[i], comments);
                            break;
                        }
                        case SqlUtils.ORACLE_TIMESTAMPLTZ_DATATYPE: {
                            addScalarSetMethod(theJavaChunk, setMethod, variableDataType[i], variableDataType[i], theRowSet.getString("ARGUMENT_NAME")
                                    , variableDataType[i], variableName[i], comments);
                            addScalarSetMethod(theJavaChunk, setMethod, variableDataType[i], variableDataType[i], theRowSet.getString("ARGUMENT_NAME")
                                    , /*variableDataType[i]*/ "oracle.sql.TIMESTAMPLTZ", variableName[i], comments);
                            setNullMethod = JavaUtils.getJavaName("set_" + variableName[i] + "_to_null", javaNamingConvention, theLog);
                            addScalarSetNullMethod(theJavaChunk, setNullMethod, theRowSet.getString("ARGUMENT_NAME"), variableName[i], comments);
                            break;
                        }
                        case SqlUtils.ORACLE_INTERVAL_DAY_TO_SECOND_DATATYPE: {
                            addScalarSetMethod(theJavaChunk, setMethod, variableDataType[i], variableDataType[i], theRowSet.getString("ARGUMENT_NAME")
                                    , /*variableDataType[i]*/ "oracle.sql.INTERVALDS", variableName[i], comments);
                            setNullMethod = JavaUtils.getJavaName("set_" + variableName[i] + "_to_null", javaNamingConvention, theLog);
                            addScalarSetNullMethod(theJavaChunk, setNullMethod, theRowSet.getString("ARGUMENT_NAME"), variableName[i], comments);
                            break;
                        }
                        case SqlUtils.ORACLE_INTERVAL_YEAR_TO_MONTH_DATATYPE: {
                            addScalarSetMethod(theJavaChunk, setMethod, variableDataType[i], variableDataType[i], theRowSet.getString("ARGUMENT_NAME")
                                    , /*variableDataType[i]*/ "oracle.sql.INTERVALYM", variableName[i], comments);
                            setNullMethod = JavaUtils.getJavaName("set_" + variableName[i] + "_to_null", javaNamingConvention, theLog);
                            addScalarSetNullMethod(theJavaChunk, setNullMethod, theRowSet.getString("ARGUMENT_NAME"), variableName[i], comments);
                            break;
                        }
                        case SqlUtils.MCPDBWIZARD_READONLYROWSET: {
                            if (comments)
                                theJavaChunk.print("// A Set method for " + variableDataType[i] + " is not supported");
                            theLog.error("Procedure or Function has Cursor as IN parameter. Unable to generate code for this situation.");
                            // addObjectScalerSetMethod(theJavaChunk, setMethod,variableDataType[i],"",theRowSet.getString("ARGUMENT_NAME")
                            //              , variableDataType[i], variableName[i],comments);
                            // setNullMethod = JavaUtils.getJavaName("set_" + variableName[i] +"_to_null", javaNamingConvention, theLog);
                            // addScalarSetNullMethod(theJavaChunk, setNullMethod,theRowSet.getString("ARGUMENT_NAME"),variableName[i],comments);
                            break;
                        }
                        case SqlUtils.ORACLE_XMLTYPE_DATATYPE: {
                            //   if (variableDataType[i].equals("Object"))
                            //     {
                            //    if (comments) theJavaChunk.print("//  A Set method for " + variableDataType[i] + " is not supported");
                            ////    }
                            //  else
                            //    {
                            //  if (webServiceRecType.equals("public"))
                            //   {
                            //    addObjectScalerSetMethod(theJavaChunk, setMethod,variableDataType[i],"",theRowSet.getString("ARGUMENT_NAME")
                            //               , variableDataType[i], variableName[i],comments);
                            //    addObjectScalerSetMethodDowncast(theJavaChunk, setMethod,"oracle.sql.OPAQUE","oracle.sql.OPAQUE",theRowSet.getString("ARGUMENT_NAME")
                            //               , "oracle.xdb.XMLType", variableName[i],comments);
                            //    }
                            //  else
                            //     {
                            //     addObjectScalerSetMethod(theJavaAttrsChunk, setMethod,variableDataType[i],"",theRowSet.getString("ARGUMENT_NAME")
                            //                , variableDataType[i], variableName[i],comments);
                            //     addObjectScalerSetMethodDowncast(theJavaAttrsChunk, setMethod,"oracle.sql.OPAQUE","oracle.sql.OPAQUE",theRowSet.getString("ARGUMENT_NAME")
                            //                , "oracle.xdb.XMLType", variableName[i],comments);
                            //     }
                            //  setNullMethod = JavaUtils.getJavaName("set_" + variableName[i] +"_to_null", javaNamingConvention, theLog);
                            //   addScalarSetNullMethod(theJavaChunk, setNullMethod,theRowSet.getString("ARGUMENT_NAME"),variableName[i],comments);
                            //  }


                            //addScalarSetMethod(theJavaAttrsChunk, setMethod,"byte[]","byte[]",theRowSet.getString("ARGUMENT_NAME")
                            //             , "byte[]", byteName[i],comments);
                            if (webServiceRecType.equals("public")) {
                                addFileSetMethod(theJavaChunk, theJavaChunk, setMethod, theRowSet.getString("ARGUMENT_NAME")
                                        , variableDataType[i], variableName[i], otherName[i], theRowSet.getString("IN_OUT"), comments, actualOracleDatatype[i], byteName[i]);
                            } else {
                                addFileSetMethod(theJavaChunk, theJavaAttrsChunk, setMethod, theRowSet.getString("ARGUMENT_NAME")
                                        , variableDataType[i], variableName[i], otherName[i], theRowSet.getString("IN_OUT"), comments, actualOracleDatatype[i], byteName[i]);
                            }

                            addLobSetMethod(theJavaChunk, setMethod, theRowSet.getString("ARGUMENT_NAME"), "OPAQUE" //actualOracleDatatype[i]
                                    , variableName[i], lobName[i], theRowSet.getString("IN_OUT"), comments);

                            setNullMethod = JavaUtils.getJavaName("set_" + variableName[i] + "_to_null", javaNamingConvention, theLog);
                            addScalarSetNullMethod(theJavaChunk, setNullMethod, theRowSet.getString("ARGUMENT_NAME"), variableName[i], comments);

                            setNullMethod = JavaUtils.getJavaName("set_" + lobName[i] + "_to_null", javaNamingConvention, theLog);
                            addScalarSetNullMethod(theJavaChunk, setNullMethod, theRowSet.getString("ARGUMENT_NAME"), lobName[i], comments);

                            break;
                        }
                        case SqlUtils.ORACLE_OBJECT_DATATYPE:
                        case SqlUtils.ORACLE_TABLE_DATATYPE:
                        case SqlUtils.ORACLE_VARRAY_DATATYPE:
                        case SqlUtils.ORACLE_PLSQL_INDEXBY_ROWTYPE_DATATYPE:
                        case SqlUtils.ORACLE_VECTOR_DATATYPE: {
                            // GUARD: some non-scalar rows (records/collections fed through the
                            // ASP comment-hint path) can classify as VECTOR here without BEING
                            // vectors; a real vector param always has variableDataType double[].
                            if (!variableDataType[i].equals("double[]")) {
                                if (webServiceRecType.equals("public")) {
                                    addObjectScalerSetMethod(theJavaChunk, setMethod, variableDataType[i], "", theRowSet.getString("ARGUMENT_NAME")
                                            , variableDataType[i], variableName[i], comments);
                                } else {
                                    addObjectScalerSetMethod(theJavaAttrsChunk, setMethod, variableDataType[i], "", theRowSet.getString("ARGUMENT_NAME")
                                            , variableDataType[i], variableName[i], comments);
                                }
                                setNullMethod = JavaUtils.getJavaName("set_" + variableName[i] + "_to_null", javaNamingConvention, theLog);
                                addScalarSetNullMethod(theJavaChunk, setNullMethod, theRowSet.getString("ARGUMENT_NAME"), variableName[i], comments);
                                break;
                            }

                            // Format-flexible VECTOR parameter: dense (double[]) and binary
                            // (byte[]) setters, each clearing the other so the last-set form
                            // is what gets bound.
                            JavaChunk vecChunk = webServiceRecType.equals("public") ? theJavaChunk : theJavaAttrsChunk;

                            vecChunk.print(" ");
                            if (comments) {
                                vecChunk.print("/**");
                                vecChunk.print("* Method to set parameter " + theRowSet.getString("ARGUMENT_NAME") + " as a dense vector");
                                vecChunk.print("* @param double[] " + variableName[i]);
                                vecChunk.print("*/");
                            }
                            vecChunk.print("public void " + setMethod + "(double[] " + variableName[i] + ")");
                            vecChunk.indent();
                            vecChunk.print("{");
                            vecChunk.print("this." + variableName[i] + " = " + variableName[i] + ";");
                            vecChunk.print("this." + variableName[i] + "VectorBinary = null;");
                            vecChunk.print("this." + variableName[i] + "VectorSparse = null;");
                            vecChunk.print("}");
                            vecChunk.unIndent();

                            vecChunk.print(" ");
                            if (comments) {
                                vecChunk.print("/**");
                                vecChunk.print("* Method to set parameter " + theRowSet.getString("ARGUMENT_NAME") + " as a BINARY (bit-packed) vector");
                                vecChunk.print("* @param byte[] " + variableName[i] + "VectorBinary");
                                vecChunk.print("*/");
                            }
                            vecChunk.print("public void " + setMethod + "(byte[] " + variableName[i] + "VectorBinary)");
                            vecChunk.indent();
                            vecChunk.print("{");
                            vecChunk.print("this." + variableName[i] + "VectorBinary = " + variableName[i] + "VectorBinary;");
                            vecChunk.print("this." + variableName[i] + " = null;");
                            vecChunk.print("this." + variableName[i] + "VectorSparse = null;");
                            vecChunk.print("}");
                            vecChunk.unIndent();

                            vecChunk.print(" ");
                            if (comments) {
                                vecChunk.print("/**");
                                vecChunk.print("* Method to set parameter " + theRowSet.getString("ARGUMENT_NAME") + " as a SPARSE vector");
                                vecChunk.print("* @param com.mcpdbwizard.pub.SparseVector " + variableName[i] + "VectorSparse");
                                vecChunk.print("*/");
                            }
                            vecChunk.print("public void " + setMethod + "(com.mcpdbwizard.pub.SparseVector " + variableName[i] + "VectorSparse)");
                            vecChunk.indent();
                            vecChunk.print("{");
                            vecChunk.print("this." + variableName[i] + "VectorSparse = " + variableName[i] + "VectorSparse;");
                            vecChunk.print("this." + variableName[i] + " = null;");
                            vecChunk.print("this." + variableName[i] + "VectorBinary = null;");
                            vecChunk.print("}");
                            vecChunk.unIndent();

                            setNullMethod = JavaUtils.getJavaName("set_" + variableName[i] + "_to_null", javaNamingConvention, theLog);
                            theJavaChunk.print(" ");
                            if (comments) {
                                theJavaChunk.print("/**");
                                theJavaChunk.print("* Method to set parameter " + theRowSet.getString("ARGUMENT_NAME") + " to null (all forms)");
                                theJavaChunk.print("*/");
                            }
                            theJavaChunk.print("public void " + setNullMethod + "()");
                            theJavaChunk.indent();
                            theJavaChunk.print("{");
                            theJavaChunk.print("this." + variableName[i] + " = null;");
                            theJavaChunk.print("this." + variableName[i] + "VectorBinary = null;");
                            theJavaChunk.print("this." + variableName[i] + "VectorSparse = null;");
                            theJavaChunk.print("}");
                            theJavaChunk.unIndent();
                            break;
                        }
                        default: {
                            if (variableDataType[i].equals("Object")) {
                                if (comments)
                                    theJavaChunk.print("//  A Set method for " + variableDataType[i] + " is not supported");
                            } else {
                                if (webServiceRecType.equals("public")) {
                                    addObjectScalerSetMethod(theJavaChunk, setMethod, variableDataType[i], "", theRowSet.getString("ARGUMENT_NAME")
                                            , variableDataType[i], variableName[i], comments);
                                } else {
                                    addObjectScalerSetMethod(theJavaAttrsChunk, setMethod, variableDataType[i], "", theRowSet.getString("ARGUMENT_NAME")
                                            , variableDataType[i], variableName[i], comments);
                                }
                                setNullMethod = JavaUtils.getJavaName("set_" + variableName[i] + "_to_null", javaNamingConvention, theLog);
                                addScalarSetNullMethod(theJavaChunk, setNullMethod, theRowSet.getString("ARGUMENT_NAME"), variableName[i], comments);
                            }
                            break;
                        }
                    }
                }
            } catch (CSSkipSectionException e) {
            } catch (CSNoDataInRowSetException e) {
                if (comments) theJavaChunk.print("// This procedure has no parameters and hence no Set methods");
            } catch (CSException e) {
                theLog.syserror(e);
            }

        }

        for (int i = 0; i < theRowSet.size(); i++) {
            String setMethod = JavaUtils.getJavaName("set_" + variableName[i], javaNamingConvention, theLog);
            //String setNullMethod = JavaUtils.getJavaName("set_" + variableName[i] +"_to_null", javaNamingConvention, theLog);


            try {
                theRowSet.setCurrentRowNumber(i);

                // Ignore this record if already seen...
                if (skipDups) {
                    //if variable name has already been seen do nothing...
                    String currentVariableName = new String(variableName[i]);

                    for (int j = 0; j < i; j++) {
                        //theRowSet.setCurrentRowNumber(j);
                        if (variableName[j].equals(currentVariableName)) {
                            throw (new CSSkipSectionException());
                        }
                    }

                    //theRowSet.setCurrentRowNumber(i);
                }

                // TIMESTAMP and INTERVAL datatypes have private set methods if they are OUT.
                if (theRowSet.getString("IN_OUT").equals("OUT") && (!overRideInOut.equals("IN/OUT"))) {

                    switch (oracleUnderlyingDatatype[i]) {
                        case SqlUtils.ORACLE_TIMESTAMP_DATATYPE: {
                            // both of these are needed.
                            if (!targetVersion.startsWith("DB2")) {
                                addScalarSetMethod(theJavaChunk, setMethod, variableDataType[i], variableDataType[i], theRowSet.getString("ARGUMENT_NAME")
                                        , /*variableDataType[i]*/ "oracle.sql.TIMESTAMP", variableName[i], comments, "private");
                            }
                            addScalarSetMethod(theJavaChunk, setMethod, variableDataType[i], variableDataType[i], theRowSet.getString("ARGUMENT_NAME")
                                    , /*variableDataType[i]*/ "java.sql.Timestamp", variableName[i], comments, "private");
                            break;
                        }
                        case SqlUtils.ORACLE_TIMESTAMPTZ_DATATYPE: {
                            addScalarSetMethod(theJavaChunk, setMethod, variableDataType[i], variableDataType[i], theRowSet.getString("ARGUMENT_NAME")
                                    , /*variableDataType[i]*/ "oracle.sql.TIMESTAMPTZ", variableName[i], comments, "private");
                            break;
                        }
                        case SqlUtils.ORACLE_TIMESTAMPLTZ_DATATYPE: {
                            addScalarSetMethod(theJavaChunk, setMethod, variableDataType[i], variableDataType[i], theRowSet.getString("ARGUMENT_NAME")
                                    , /*variableDataType[i]*/ "oracle.sql.TIMESTAMPLTZ", variableName[i], comments, "private");
                            break;
                        }
                        case SqlUtils.ORACLE_INTERVAL_DAY_TO_SECOND_DATATYPE: {
                            addScalarSetMethod(theJavaChunk, setMethod, variableDataType[i], variableDataType[i], theRowSet.getString("ARGUMENT_NAME")
                                    , /*variableDataType[i]*/ "oracle.sql.INTERVALDS", variableName[i], comments, "private");
                            break;
                        }
                        case SqlUtils.ORACLE_INTERVAL_YEAR_TO_MONTH_DATATYPE: {
                            addScalarSetMethod(theJavaChunk, setMethod, variableDataType[i], variableDataType[i], theRowSet.getString("ARGUMENT_NAME")
                                    , /*variableDataType[i]*/ "oracle.sql.INTERVALYM", variableName[i], comments, "private");
                            break;
                        }
                        default: {
                            break;
                        }
                    }
                }
            } catch (CSSkipSectionException e) {
            } catch (CSNoDataInRowSetException e) {
                if (comments) theJavaChunk.print("// This procedure has no parameters and hence no Set methods");
            } catch (CSException e) {
                theLog.syserror(e);
            }

        }


        if ((hasFiles || hasChildFiles) && type == IS_A_FUNCTION) {
            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* Set new file io buffer size");
                theJavaChunk.print("* @param int " + sbufferSize + " A new Buffer size in bytes.");
                theJavaChunk.print("*/");
            }

            theJavaChunk.print("public " + "void "
                    + sSetBufferSize + "(int " + sbufferSize + ")");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("this." + sbufferSize + " = " + sbufferSize + ";");

            if (debugMessages) {
                theJavaChunk.print(stheLog + ".debug(\"" + sbufferSize + " set to \" + " + sbufferSize + ");");
            }
            theJavaChunk.print("}");
            theJavaChunk.unIndent();

            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* Set " + skeepLobs + "");
                theJavaChunk.print("* @param boolean " + skeepLobs + " Keep Lobs as pointers rather than turn them into files.");
                theJavaChunk.print("* @since 4.0.1847");
                theJavaChunk.print("*/");
            }

            theJavaChunk.print("public " + "void "
                    + sSetKeepLobs + "(boolean " + skeepLobs + " )");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("this." + skeepLobs + " = " + skeepLobs + ";");

            if (debugMessages) {
                theJavaChunk.print(stheLog + ".debug(\"" + skeepLobs + " set to \" + " + skeepLobs + ");");
            }
            theJavaChunk.print("}");
            theJavaChunk.unIndent();

            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* Set " + "useByteArraysForLongsAndLOBS" + "");
                theJavaChunk.print("* @param boolean " + "useByteArraysForLongsAndLOBS" + " Turn lobs into byte[]");
                theJavaChunk.print("* @since 5.0.2314");
                theJavaChunk.print("*/");
            }

            theJavaChunk.print("public " + "void "
                    + "setUseByteArraysForLongsAndLOBS(boolean useByteArraysForLongsAndLOBS)");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("this.useByteArraysForLongsAndLOBS = useByteArraysForLongsAndLOBS;");

            if (debugMessages) {
                theJavaChunk.print(stheLog + ".debug(\"useByteArraysForLongsAndLOBSset to \" + useByteArraysForLongsAndLOBS);");
            }
            theJavaChunk.print("}");
            theJavaChunk.unIndent();

            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* Set " + skeepFiles + "");
                theJavaChunk.print("* @param boolean " + skeepFiles + " Keep generated files after JVM exits");
                theJavaChunk.print("*/");
            }

            theJavaChunk.print("public " + "void "
                    + sSetKeepFiles + "(boolean " + skeepFiles + " )");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("this." + skeepFiles + " = " + skeepFiles + ";");

            if (debugMessages) {
                theJavaChunk.print(stheLog + ".debug(\"" + skeepFiles + " set to \" + " + skeepFiles + ");");
            }
            theJavaChunk.print("}");
            theJavaChunk.unIndent();

            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* Delete all generated files");
                theJavaChunk.print("* This method deletes all Files created by accessing CLOB's, BLOB's, etc");
                theJavaChunk.print("* It was added because '" + sSetKeepFiles + "' only works when the JVM exits, which ");
                theJavaChunk.print("* could be several days in the future in some scenarios. Only use this method if ");
                theJavaChunk.print("* you want <b>all<b> your generated files to be deleted. ");
                theJavaChunk.print("* @since 2.0.1176");
                theJavaChunk.print("*/");
            }

            theJavaChunk.print("public " + "void "
                    + sDelFilesMethod + "()");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            this.addDeleteFilesCode(theJavaChunk, comments, debugMessages, null, null);
            theJavaChunk.print("}");
            theJavaChunk.unIndent();


            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* Set temporary directory");
                theJavaChunk.print("* @param java.io.File " + stempFileDir + " a new Temporary Directory");
                theJavaChunk.print("* @throws CSException if the directory is not viable");
                theJavaChunk.print("*/");
            }

            theJavaChunk.print("public " + "void "
                    + sSetTempFileDir + "(java.io.File " + stempFileDir + ") throws CSException");
            theJavaChunk.indent();
            theJavaChunk.print("{");

            theJavaChunk.print("if (" + stempFileDir + " == null)                                   ");
            theJavaChunk.indent();
            theJavaChunk.print("{                                                             ");
            theJavaChunk.print("throw (new CSException(\"Attempt made to set " + stempFileDir + " to null\")); ");
            theJavaChunk.print("}                                      ");
            theJavaChunk.unIndent();
            theJavaChunk.print("else if (! " + stempFileDir + ".exists())       ");
            theJavaChunk.indent();
            theJavaChunk.print("{       ");
            theJavaChunk.print("try    ");
            theJavaChunk.indent();
            theJavaChunk.print("{     ");
            if (otherMessages)
                theJavaChunk.print(stheLog + ".info(\"Creating temporary directory \" + " + stempFileDir + ".getAbsolutePath()); ");
            theJavaChunk.print("" + stempFileDir + ".mkdirs(); ");
            theJavaChunk.print("}  ");
            theJavaChunk.unIndent();
            theJavaChunk.print("catch (Exception e) ");
            theJavaChunk.indent();
            theJavaChunk.print("{ ");
            theJavaChunk.print("throw (new CSException(\"" + stempFileDir + " \" + " + stempFileDir + ".getAbsolutePath() + \" can not be created\"));  ");
            theJavaChunk.print("} ");
            theJavaChunk.unIndent();
            theJavaChunk.print("}  ");
            theJavaChunk.unIndent();
            theJavaChunk.print("");
            theJavaChunk.print("this." + stempFileDir + " = " + stempFileDir + ";");

            if (debugMessages) {
                theJavaChunk.print(stheLog + ".debug(\"" + stempFileDir + " set to \" + " + stempFileDir + ".getAbsolutePath());");
            }
            theJavaChunk.print("}");
            theJavaChunk.unIndent();

            theJavaChunk.print("");

            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* Set the prefix used for generating temporary files");
                theJavaChunk.print("*/");
            }

            theJavaChunk.print("public " + "void "
                    + sSetTempFilePrefix + "(String " + stempFilePrefix + ")");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("this." + stempFilePrefix + " = " + stempFilePrefix + "" + ";");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();

            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* Set the suffix used for generating temporary files");
                theJavaChunk.print("*/");
            }

            theJavaChunk.print("public " + "void "
                    + sSetTempFileSuffix + "(String " + stempFileSuffix + ")");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("this." + stempFileSuffix + " = " + stempFileSuffix + "" + ";");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();

            addFilenameSetMethods(theJavaChunk, comments, debugMessages);

            if (!targetVersion.startsWith("8")) {
                theJavaChunk.print("");

                if (comments) {
                    theJavaChunk.print("/**");
                    theJavaChunk.print("* Set flag that controls whether Temporary Lobs are created before ");
                    theJavaChunk.print("* each database call if no LOB parameter has been set");
                    theJavaChunk.print("* @since 4.0.2107 / Oracle 9.0.1");
                    theJavaChunk.print("*/");
                }

                theJavaChunk.print("public void setCreateTempLobsIfNeeded(boolean  createTempLobsIfNeeded)");
                theJavaChunk.indent();
                theJavaChunk.print("{");
                theJavaChunk.print("this.createTempLobsIfNeeded = createTempLobsIfNeeded;");
                theJavaChunk.print("}");
                theJavaChunk.unIndent();

                theJavaChunk.print("");
                if (comments) {
                    theJavaChunk.print("/**");
                    theJavaChunk.print("* Set flag that controls whether Temporary Lobs are deleted after each database call");
                    theJavaChunk.print("* @since 4.0.2107 / Oracle 9.0.1");
                    theJavaChunk.print("*/");
                }

                theJavaChunk.print("public void setDeleteTempLobsAfterCall(boolean  deleteTempLobsAfterCall)");
                theJavaChunk.indent();
                theJavaChunk.print("{");
                theJavaChunk.print("this.deleteTempLobsAfterCall = deleteTempLobsAfterCall;");
                theJavaChunk.print("}");
                theJavaChunk.unIndent();
            }
        }

        if (hasRowSets) {
            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* Set limit to number of rows returned in ReadOnlyRowSets");
                theJavaChunk.print("* @param int " + smaxRows + " Maximum number of rows that will be retrieved. Some JDBC drivers will ");
                theJavaChunk.print("* stop returning rows after about 30,000. ");
                theJavaChunk.print("*/");
            }

            theJavaChunk.print("public " + "void "
                    + sSetMaxRows + "(int " + smaxRows + ")");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("this." + smaxRows + " = " + smaxRows + ";");

            if (debugMessages) {
                theJavaChunk.print(stheLog + ".debug(\"" + smaxRows + " set to \" + " + smaxRows + ");");
            }
            theJavaChunk.print("}");
            theJavaChunk.unIndent();

            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* Get number of rows returned in ReadOnlyRowSets");
                theJavaChunk.print("* @return int " + smaxRows + " Maximum number of rows that will be retrieved. ");
                theJavaChunk.print("*/");
            }

            theJavaChunk.print("public " + "int "
                    + sGetMaxRows + "()");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("return(" + smaxRows + ");");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
        }

    }

    void addOraDataMethods(JavaChunk theJavaChunk, boolean comments, boolean debugMessages, boolean otherMessages, String className) {
        if (targetVersion.startsWith("DB2")) {

        } else {
            if (arraysSupported) {
                if (comments) theJavaChunk.print("/**");
                if (comments) theJavaChunk.print("* Return this instance as a STRUCT.");
                if (comments) theJavaChunk.print("*/");

                if (targetVersion.startsWith("8")) {
                    theJavaChunk.print("public Datum toDatum(oracle.jdbc.driver.OracleConnection parm1) throws java.sql.SQLException");
                } else {
                    theJavaChunk.print("public Datum toDatum(java.sql.Connection parm1) throws java.sql.SQLException");
                }

                theJavaChunk.indent();
                theJavaChunk.print("{");
                if (comments) theJavaChunk.print("// Create a descriptor that says which DB object this is");
                theJavaChunk.print("StructDescriptor sd = StructDescriptor.createDescriptor(recordName, parm1);");
                theJavaChunk.print("");
                if (comments)
                    theJavaChunk.print("// Create and return a STRUCT that consists of the descriptor and the");
                if (comments) theJavaChunk.print("// fields of this class in an object array.");
                theJavaChunk.print("return new STRUCT(sd, parm1, getCurrentValues(parm1));");
                theJavaChunk.print("}");
                theJavaChunk.print("");
                theJavaChunk.unIndent();

                if (comments) theJavaChunk.print("/**");
                if (comments) theJavaChunk.print("* Create an instance of this class from a Datum object.");
                if (comments) theJavaChunk.print("*/");

                if (targetVersion.startsWith("8")) {
                    theJavaChunk.print("public CustomDatum create(Datum parm1, int parm2) throws java.sql.SQLException");
                } else {
                    theJavaChunk.print("public ORAData create(Datum parm1, int parm2) throws java.sql.SQLException");
                }

                theJavaChunk.indent();
                theJavaChunk.print("{");
                theJavaChunk.print("if (parm1 == null)");
                theJavaChunk.indent();
                theJavaChunk.print("{");
                theJavaChunk.print("return null;");
                theJavaChunk.print("}");
                theJavaChunk.unIndent();
                theJavaChunk.print("");
                if (comments) theJavaChunk.print("// Create an instance of this class from a STRUCT.");
                if (comments)
                    theJavaChunk.print("// We may get a CSException if our STRUCT's object array can not be used");
                if (comments)
                    theJavaChunk.print("// to create an instance of this class. If this happens we log the error");
                if (comments)
                    theJavaChunk.print("// and return null as the oraDataFactory interface does not allow us to");
                if (comments) theJavaChunk.print("// throw anything other than java.sql.SQLException.");
                theJavaChunk.print("try");
                theJavaChunk.indent();
                theJavaChunk.print("{");
                theJavaChunk.print("Object[] attributes = ((STRUCT)parm1).getAttributes();");
                theJavaChunk.print(className + " newClassInstance = new " + className + "(" + stheLog + ", attributes);");
                theJavaChunk.print("return(newClassInstance);");
                theJavaChunk.print("}");
                theJavaChunk.unIndent();
                theJavaChunk.print("catch (CSException e)");
                theJavaChunk.indent();
                theJavaChunk.print("{");
                theJavaChunk.print(stheLog + ".error(\"Unable to create instance of \" + recordName + \" from DATUM:\" );");
                theJavaChunk.print(stheLog + ".error(e);");
                theJavaChunk.print(stheLog + ".error(\"Returning null instead\");");
                theJavaChunk.print("return(null);");
                theJavaChunk.print("}");
                theJavaChunk.unIndent();
                theJavaChunk.print("");
                theJavaChunk.print("}");
                theJavaChunk.print("");
                theJavaChunk.unIndent();
            }
        }
    }

    void addDeleteFilesCode(JavaChunk theJavaChunk, boolean comments
            , boolean debugMessages
            , String parentVariableName, String parentVariableClassName) {
        String qualifiedParentVariableName = null;

        if (debugMessages && parentVariableName == null) {
            theJavaChunk.print(isBrokenString + stheLog + ".debug(\"Starting to delete files if they exist\");");
        }

        if (parentVariableName != null) {
            qualifiedParentVariableName = parentVariableName + ".";
        } else {
            qualifiedParentVariableName = "";
        }

        try {

            for (int i = 0; i < theRowSet.size(); i++) {
                theRowSet.setCurrentRowNumber(i);


                if (oracleParamDatatype[i] == "SqlUtils.ORACLE_OTHER_DATATYPE"
                        || oracleParamDatatype[i] == "SqlUtils.ORACLE_ROWTYPE_DATATYPE"
                        || oracleParamDatatype[i] == "SqlUtils.ORACLE_TABLE_DATATYPE"
                        || oracleParamDatatype[i] == "SqlUtils.ORACLE_VARRAY_DATATYPE"
                        || oracleParamDatatype[i] == "SqlUtils.ORACLE_PLSQL_INDEXBY_ROWTYPE_DATATYPE"
                        || oracleParamDatatype[i] == "SqlUtils.ORACLE_OBJECT_DATATYPE") {
                    if (!variableDataType[i].equals("Object")) {
                        try {
                            if (theRecords[extraObjectId[i]].usable && theRecords[extraObjectId[i]].theEngine.hasDeleteFiles(extraObjectId[i])) {
                                theJavaChunk.print("if (" + qualifiedParentVariableName + variableName[i] + " != null)");
                                theJavaChunk.indent();
                                theJavaChunk.print("{");
                                //DRKLUGE - may be null....
                                theRecords[extraObjectId[i]].theEngine.addDeleteFilesCode(theJavaChunk, comments, debugMessages, variableName[i], variableDataType[i]);

                                theJavaChunk.print("}");
                                theJavaChunk.print(" ");
                                theJavaChunk.unIndent();
                            }
                        } catch (Exception e) {
                            theLog.syserror(e);
                        }
                    }
                } else if (variableDataType[i].equals("java.io.File")
                        && useVariable[i]
                        && (theRowSet.getString("IN_OUT").equals("OUT") || overRideInOut.equals("IN/OUT"))) {
                    if (debugMessages) {
                        theJavaChunk.print(isBrokenString + stheLog + ".debug(\"Delete file " + qualifiedParentVariableName + variableName[i] + " if it exists \");");
                    }

                    if (comments) {
                        if (theRowSet.getInt("POSITION") > 0) {
                            if (comments)
                                theJavaChunk.print("// Delete file for " + theRowSet.getString("ARGUMENT_NAME"));
                        } else {
                            if (comments) theJavaChunk.print("// Delete file that holds holds function result");
                        }
                    }

                    theJavaChunk.print("if (" + qualifiedParentVariableName + variableName[i] + " != null)");
                    theJavaChunk.indent();
                    theJavaChunk.print("{");

                    theJavaChunk.print("if (" + qualifiedParentVariableName + variableName[i] + ".exists())");
                    theJavaChunk.indent();
                    theJavaChunk.print("{");
                    if (debugMessages) {
                        theJavaChunk.print(isBrokenString + stheLog + ".debug(\"Deleting file " + qualifiedParentVariableName + variableName[i] + "\");");
                    }
                    theJavaChunk.print(qualifiedParentVariableName + variableName[i] + ".delete();");
                    theJavaChunk.print("}");
                    theJavaChunk.unIndent();
                    theJavaChunk.print("}");
                    theJavaChunk.unIndent();
                    theJavaChunk.print(" ");
                } else if (variableDataType[i].equals("com.mcpdbwizard.pub.ReadOnlyRowSet")
                        && useVariable[i]) {
                    if (debugMessages) {
                        theJavaChunk.print(isBrokenString + stheLog + ".debug(\"Delete files owned by ReadOnlyRowSet" + qualifiedParentVariableName + variableName[i] + " if they exist\");");
                    }

                    if (comments) {
                        if (theRowSet.getInt("POSITION") > 0) {
                            if (comments)
                                theJavaChunk.print("// Delete ReadOnlyRowSet files for " + theRowSet.getString("ARGUMENT_NAME"));
                        } else {
                            if (comments)
                                theJavaChunk.print("// Delete files from ReadOnlyRowSet that holds holds function result");
                        }
                    }

                    theJavaChunk.print("if (" + qualifiedParentVariableName + variableName[i] + " != null)");
                    theJavaChunk.indent();
                    theJavaChunk.print("{");
                    if (comments) theJavaChunk.print("// Because the fields in a cursor aren't known up front we ");
                    if (comments) theJavaChunk.print("// have no way of telling whether any ReadOnlyRowSet");
                    if (comments) theJavaChunk.print("// has files in it so call the delete method regardless ");
                    theJavaChunk.print(qualifiedParentVariableName + variableName[i] + ".deleteGeneratedFiles();");
                    theJavaChunk.print("}");
                    theJavaChunk.unIndent();
                    theJavaChunk.print(" ");
                }

                // deleteGeneratedFiles
            } // End of for...

        } catch (CSNoDataInRowSetException e) {
        } catch (CSException e) {
            theLog.syserror(e, true, true);
        }

        if (debugMessages && parentVariableName == null) {
            theJavaChunk.print(isBrokenString + stheLog + ".debug(\"Finished deleting files\");");
        }

    }

    boolean hasDeleteFiles(int whichOne) {
        boolean retCode = false;

        for (int i = 0; i < theRowSet.size(); i++) {

            if (oracleParamDatatype[i] == "SqlUtils.ORACLE_OTHER_DATATYPE"
                    || oracleParamDatatype[i] == "SqlUtils.ORACLE_ROWTYPE_DATATYPE"
                    || oracleParamDatatype[i] == "SqlUtils.ORACLE_TABLE_DATATYPE"
                    || oracleParamDatatype[i] == "SqlUtils.ORACLE_VARRAY_DATATYPE"
                    || oracleParamDatatype[i] == "SqlUtils.ORACLE_PLSQL_INDEXBY_ROWTYPE_DATATYPE"
                    || oracleParamDatatype[i] == "SqlUtils.ORACLE_OBJECT_DATATYPE") {
                if (!variableDataType[i].equals("Object")) {
                    try {
                        if (theRecords[extraObjectId[i]].theEngine != this && theRecords[extraObjectId[i]].theEngine.hasDeleteFiles(extraObjectId[i])) {
                            retCode = true;
                            break;
                        }
                    } catch (Exception e) {
                        theLog.syserror(e);
                    }
                }
            } else if (variableDataType[i].equals("java.io.File")
                    && useVariable[i]) {
                retCode = true;
                break;
            }
        } // End of for...
        return (retCode);
    }

    String addBindCode(JavaChunk theJavaChunk, boolean comments
            , boolean debugMessages, int inOffSet, int outOffSet
            , String parentVariableName, String parentVariableClassName
            , String bindOverRideInOut
            , boolean webServices, String oracleVersion) {
        return (addBindCode(theJavaChunk, comments
                , debugMessages, inOffSet, outOffSet
                , parentVariableName, parentVariableClassName
                , bindOverRideInOut, "theCallableStatement", "theParameters", webServices, oracleVersion));
    }

    String addBindCode(JavaChunk theJavaChunk, boolean comments
            , boolean debugMessages, int inOffSet, int outOffSet
            , String parentVariableName, String parentVariableClassName
            , String bindOverRideInOut
            , String bindObjectName
            , String paramObjectName
            , boolean webServices
            , String oracleVersion) {
        String qualifiedParentVariableName = null;
        boolean hasBlobTrimMethod = false;
        boolean hasClobTrimMethod = false;

        int[] oldParamOutId = new int[paramOutId.length];
        int[] oldParamInId = new int[paramInId.length];

        if (bindOverRideInOut != null && (bindOverRideInOut.equals("IN/OUT")
                || bindOverRideInOut.equals("IN")
                || bindOverRideInOut.equals("OUT"))) {
            // replace out params for this method call....
            for (int i = 0; i < paramOutId.length; i++) {
                oldParamOutId[i] = paramOutId[i];
                paramOutId[i] = i + 1;
                oldParamInId[i] = paramInId[i];
                paramInId[i] = i + 1;
            }
        }

        if (parentVariableName != null) {
            qualifiedParentVariableName = parentVariableName + ".";
            if (comments) theJavaChunk.print(isBrokenString + "// Prevent null pointer exception later on");
            theJavaChunk.print(isBrokenString + "if (" + parentVariableName + " == null)");
            theJavaChunk.print(isBrokenString + "  {");
            theJavaChunk.print(isBrokenString + "  " + parentVariableName + " = new " + parentVariableClassName + "(" + stheLog + ");");
            theJavaChunk.print(isBrokenString + "  }");
            theJavaChunk.print(isBrokenString + "  ");
        } else {
            qualifiedParentVariableName = "";
        }

        try {

            if (parentVariableName == null && evilPlsqlTablesWarningGiven) {
                theJavaChunk.print(stheLog + ".error(\"This procedure" + EVIL_PLSQL_TABLES_WARNING_1 + "\");");
                theJavaChunk.print(stheLog + ".error(\"" + EVIL_PLSQL_TABLES_WARNING_2 + "\");");
            }


            if (debugMessages && parentVariableName == null) {
                theJavaChunk.print(isBrokenString + "" + stheLog + ".debug(\"Starting to bind parameters\");");
            }


            for (int i = 0; i < theRowSet.size(); i++) {
                theRowSet.setCurrentRowNumber(i);

                if (oracleParamDatatype[i].equals("SqlUtils.ORACLE_TABLE_DATATYPE")
                        || oracleParamDatatype[i].equals("SqlUtils.ORACLE_VARRAY_DATATYPE")
                        || oracleParamDatatype[i].equals("SqlUtils.ORACLE_PLSQL_INDEXBY_ROWTYPE_DATATYPE")
                ) {
                    if (comments) {
                        theJavaChunk.print("// Make sure " + theRowSet.getString("ARGUMENT_NAME") + " is not null");
                    }

                    theJavaChunk.print(isBrokenString + "if (" + qualifiedParentVariableName + variableName[i] + " == null)");
                    theJavaChunk.indent();
                    theJavaChunk.print(isBrokenString + "{  ");
                    theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + " = new " + variableDataType[i] + "(" + stheLog + ");");
                    theJavaChunk.print(isBrokenString + "}   ");
                    theJavaChunk.print(isBrokenString + " ");
                    theJavaChunk.unIndent();

                    if (theRowSet.getString("IN_OUT").equals("IN")
                            || theRowSet.getString("IN_OUT").equals("IN/OUT")
                            || overRideInOut.equals("IN/OUT")) {
                        // Find out if the Array has records that have files....
                        if (theRecords[extraObjectId[i]].usable
                                && theRecords[extraObjectId[i]].typeImplementingClass != null
                                && theRecords[extraObjectId[i]].typeImplementingClass.theEngine != null
                                && theRecords[extraObjectId[i]].typeImplementingClass.theEngine.hasFiles) {
                            theJavaChunk.print(isBrokenString + "// Create temporary LOBs for " + qualifiedParentVariableName + variableName[i]);

                            theRecords[extraObjectId[i]].typeImplementingClass.theEngine.addArrayLobSetCode(theJavaChunk
                                    , theRecords[extraObjectId[i]].typeImplementingClass.javaName
                                    , comments
                                    , debugMessages
                                    , false
                                    , qualifiedParentVariableName + variableName[i]
                                    , webServices, oracleVersion);
                        }
                    }


                    //
                } // if table or varray
            } //for


            for (int i = 0; i < theRowSet.size(); i++) {

                theRowSet.setCurrentRowNumber(i);
                String inOut = theRowSet.getString("IN_OUT");

                if (overRideInOut != null && overRideInOut != "") {
                    inOut = overRideInOut;
                }
                if (bindOverRideInOut != null && bindOverRideInOut != "") {
                    inOut = bindOverRideInOut;
                }

                if (comments) {
                    if (theRowSet.getInt("POSITION") > 0) {
                        if (comments) theJavaChunk.print("// Bind parameter " + theRowSet.getString("ARGUMENT_NAME"));
                    } else {
                        if (comments) theJavaChunk.print("// Bind parameter that will hold function result");
                    }
                }


                // ((oracle.jdbc.OracleStatement)theCallableStatement).defineColumnType(1, OracleTypes.);

                if (oracleParamDatatype[i] == "SqlUtils.ORACLE_OTHER_DATATYPE"
                        || oracleParamDatatype[i] == "SqlUtils.ORACLE_ROWTYPE_DATATYPE"
                        || oracleParamDatatype[i] == "SqlUtils.ORACLE_OBJECT_DATATYPE") {
                    if (!variableDataType[i].equals("Object")) {
                        try {
                            if (theRecords[extraObjectId[i]].usable) {
                                // Item 7 (nested records): recurse with the FULL accessor path
                                // (qualifiedParentVariableName + variableName[i]) as the child's parent,
                                // not just variableName[i], so a record nested inside a record binds
                                // "outer.inner.field", not "inner.field". Byte-identical for a top-level
                                // record param (qualifiedParentVariableName is "" there).
                                // Item 7 (nested records): accumulate THIS engine's offsets into the
                                // child's so a record nested inside a record binds at the right absolute
                                // positions (outer.inner.field). inOffSet/outOffSet are 0 at the top level,
                                // so this is byte-identical for a single-level record param.
                                String bindResult = theRecords[extraObjectId[i]].theEngine.addBindCode(theJavaChunk, comments, debugMessages, (inOffSet + paramInId[i] - 1), (outOffSet + paramOutId[i] - 1), qualifiedParentVariableName + variableName[i], variableDataType[i], inOut, bindObjectName, paramObjectName, webServices, oracleVersion);

                                if (bindResult.indexOf("clob") > -1) {
                                    hasClobTrimMethod = true;
                                }

                                if (bindResult.indexOf("blob") > -1) {
                                    hasBlobTrimMethod = true;
                                }

                            } else {
                                theJavaChunk.print("// Error: parameter " + qualifiedParentVariableName + variableName[i] + " can not be set");
                                theLog.error("Error: parameter " + qualifiedParentVariableName + variableName[i] + " can not be set");
                            }
                        } catch (Exception e) {
                            theLog.syserror(e);
                        }
                    }
                } else if (true) {
                    if (theRowSet.getInt("POSITION") > 0     // not a function result
                            && (inOut.equals("IN") // in
                            || inOut.equals("IN/OUT")) // in
                            && paramInId[i] > 0) // in
                    {
                        if (debugMessages) {
                            theJavaChunk.print(isBrokenString + "");
                            theJavaChunk.print(isBrokenString + stheLog + ".debug(\"binding input parameter " + qualifiedParentVariableName + variableName[i] + " to position " + (paramInId[i] + inOffSet) + "\");");
                        }

                        String realDataType = theRowSet.getString("DATA_TYPE");
                        // NCLOB rides the CLOB path: the LOB locator/loader are CLOB-based and
                        // LongObjectLoader has no loadNCLOB, so emit loadCLOB(...) for it.
                        if ("NCLOB".equals(realDataType)) {
                            realDataType = "CLOB";
                        }

                        if (oracleParamDatatype[i].equals("OracleTypes.VECTOR_BINARY")) {
                            // Binary (bit-packed) VECTOR (23ai): a byte[] is ambiguous with RAW, so bind
                            // through the dedicated setter that types the parameter VECTOR_BINARY — plain
                            // setParam(int, byte[]) would bind it as RAW.
                            theJavaChunk.print(isBrokenString + paramObjectName + ".setVectorBinaryParam(" + (paramInId[i] + inOffSet) + "," + qualifiedParentVariableName + variableName[i] + ");");
                        } else if (oracleParamDatatype[i].equals("OracleTypes.VECTOR")
                                && variableDataType[i].equals("com.mcpdbwizard.pub.SparseVector")) {
                            // Sparse VECTOR (23ai): bind the SparseVector through its dedicated setter,
                            // which builds an oracle.sql.VECTOR (FLOAT64; Oracle coerces the element format).
                            theJavaChunk.print(isBrokenString + paramObjectName + ".setVectorSparseParam(" + (paramInId[i] + inOffSet) + "," + qualifiedParentVariableName + variableName[i] + ");");
                        } else if (oracleParamDatatype[i].equals("OracleTypes.VECTOR")
                                && variableDataType[i].equals("double[]")) {
                            // Format-flexible VECTOR parameter: bind whichever form the caller set —
                            // sparse (SparseVector), then binary (byte[]), else dense (double[]).
                            // (The setters keep the three fields mutually exclusive.)
                            theJavaChunk.print(isBrokenString + "if (" + qualifiedParentVariableName + variableName[i] + "VectorSparse != null)");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{");
                            theJavaChunk.print(isBrokenString + paramObjectName + ".setVectorSparseParam(" + (paramInId[i] + inOffSet) + "," + qualifiedParentVariableName + variableName[i] + "VectorSparse);");
                            theJavaChunk.print(isBrokenString + "}");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "else if (" + qualifiedParentVariableName + variableName[i] + "VectorBinary != null)");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{");
                            theJavaChunk.print(isBrokenString + paramObjectName + ".setVectorBinaryParam(" + (paramInId[i] + inOffSet) + "," + qualifiedParentVariableName + variableName[i] + "VectorBinary);");
                            theJavaChunk.print(isBrokenString + "}");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "else");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{");
                            theJavaChunk.print(isBrokenString + paramObjectName + ".setParam(" + (paramInId[i] + inOffSet) + "," + qualifiedParentVariableName + variableName[i] + ");");
                            theJavaChunk.print(isBrokenString + "}");
                            theJavaChunk.unIndent();
                        } else if (oracleParamDatatype[i].equals("OracleTypes.LONGVARCHAR")
                                || oracleParamDatatype[i].equals("OracleTypes.LONGVARBINARY")) {

                            theJavaChunk.print(isBrokenString + "try");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{");


                            theJavaChunk.print(isBrokenString + "if (" + qualifiedParentVariableName + byteName[i] + " != null && useByteArraysForLongsAndLOBS)  ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{       ");
                            theJavaChunk.print(isBrokenString + paramObjectName + ".setParam(" + (paramInId[i] + inOffSet) + ","
                                    + "new java.io.ByteArrayInputStream(" + qualifiedParentVariableName + byteName[i] + "), " + qualifiedParentVariableName + byteName[i] + ".length," + oracleParamDatatype[i] + ");");
                            theJavaChunk.print(isBrokenString + "}               ");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "else if (" + qualifiedParentVariableName + variableName[i] + " != null)  ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{       ");
                            theJavaChunk.print(isBrokenString + paramObjectName + ".setParam(" + (paramInId[i] + inOffSet) + ","
                                    + "new java.io.BufferedInputStream(new java.io.FileInputStream(" + qualifiedParentVariableName + variableName[i] + ")," + sbufferSize + "),(int)" + qualifiedParentVariableName + variableName[i] + ".length()," + oracleParamDatatype[i] + ");");
                            theJavaChunk.print(isBrokenString + "}               ");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "else            ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{                 ");
                            theJavaChunk.print(isBrokenString + paramObjectName + ".setParam(" + (paramInId[i] + inOffSet) + ",(java.io.BufferedInputStream)null,0," + oracleParamDatatype[i] + ");   ");
                            theJavaChunk.print(isBrokenString + "}                 ");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "}");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "catch (java.io.FileNotFoundException e)");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{");
                            theJavaChunk.print(isBrokenString + "throw new CSException(\"Unable to bind parameter " + (paramInId[i] + inOffSet) + ": File \" + " + qualifiedParentVariableName + variableName[i] + ".getAbsolutePath() +\" not found\");");
                            theJavaChunk.print(isBrokenString + "}");
                            theJavaChunk.print("");
                            theJavaChunk.unIndent();

                        } else if (oracleParamDatatype[i].equals("OracleTypes.TIMESTAMP")) {
                            theJavaChunk.print(isBrokenString + "if (" + qualifiedParentVariableName + variableName[i] + " != null)  ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{       ");
                            if (variableDataType[i].equals("java.util.Date")) {
                                theJavaChunk.print(isBrokenString + paramObjectName + ".setParam(" + (paramInId[i] + inOffSet) + ","
                                        + qualifiedParentVariableName + variableName[i] + ");");
                            } else {
                                // 9.0.1 doesn't like oracle.sql.TIMESTAMP.
                                if (targetVersion.equals("9.0.1") || targetVersion.startsWith("DB2")) {
                                    theJavaChunk.print(isBrokenString + paramObjectName + ".setParam(" + (paramInId[i] + inOffSet) + ","
                                            + " java.sql.Timestamp.valueOf(" + qualifiedParentVariableName + variableName[i] + "));");
                                } else {
                                    theJavaChunk.print(isBrokenString + paramObjectName + ".setParam(" + (paramInId[i] + inOffSet) + ","
                                            + " new oracle.sql.TIMESTAMP(" + qualifiedParentVariableName + variableName[i] + "));");
                                }
                            }
                            theJavaChunk.print(isBrokenString + "}               ");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "else            ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{                 ");
                            if (variableDataType[i].equals("java.util.Date")) {
                                theJavaChunk.print(isBrokenString + paramObjectName + ".setParam(" + (paramInId[i] + inOffSet) + ",(java.util.Date)null);   ");
                            } else {
                                if (targetVersion.equals("9.0.1") || targetVersion.startsWith("DB2")) {
                                    theJavaChunk.print(isBrokenString + paramObjectName + ".setParam(" + (paramInId[i] + inOffSet) + ",(java.sql.Timestamp)null);   ");
                                } else {
                                    theJavaChunk.print(isBrokenString + paramObjectName + ".setParam(" + (paramInId[i] + inOffSet) + ",(oracle.sql.TIMESTAMP)null);   ");
                                }
                            }
                            theJavaChunk.print(isBrokenString + "}                 ");
                            theJavaChunk.unIndent();

                        } else if (oracleParamDatatype[i].equals("OracleTypes.TIMESTAMPTZ")) {
                            theJavaChunk.print(isBrokenString + "if (" + qualifiedParentVariableName + variableName[i] + " != null)  ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{       ");
                            theJavaChunk.print(isBrokenString + "try  ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{           ");
                            theJavaChunk.print(isBrokenString + paramObjectName + ".setParam(" + (paramInId[i] + inOffSet) + ","
                                    + " new oracle.sql.TIMESTAMPTZ(" + qualifiedParentVariableName + variableName[i] + "));");
                            theJavaChunk.print(isBrokenString + "}               ");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "catch (Exception e)   ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{           ");
                            theJavaChunk.print(isBrokenString + "throw (new CSException(\"" + qualifiedParentVariableName + variableName[i] + " could not be loaded:\" + e.toString()));   ");
                            theJavaChunk.print(isBrokenString + "}          ");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "}               ");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "else            ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{                 ");
                            theJavaChunk.print(isBrokenString + paramObjectName + ".setParam(" + (paramInId[i] + inOffSet) + ",(oracle.sql.TIMESTAMPTZ)null);   ");
                            theJavaChunk.print(isBrokenString + "}                 ");
                            theJavaChunk.unIndent();

                        } else if (oracleParamDatatype[i].equals("OracleTypes.TIMESTAMPLTZ")) {
                            theJavaChunk.print(isBrokenString + "if (" + qualifiedParentVariableName + variableName[i] + " != null)  ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{       ");
                            theJavaChunk.print(isBrokenString + "try  ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{           ");
                            theJavaChunk.print(isBrokenString + paramObjectName + ".setParam(" + (paramInId[i] + inOffSet) + ","
                                    + " new oracle.sql.TIMESTAMPLTZ(" + qualifiedParentVariableName + variableName[i] + "));");
                            theJavaChunk.print(isBrokenString + "}               ");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "catch (Exception e)   ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{           ");
                            theJavaChunk.print(isBrokenString + "throw (new CSException(\"" + qualifiedParentVariableName + variableName[i] + " could not be loaded:\" + e.toString()));   ");
                            theJavaChunk.print(isBrokenString + "}          ");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "}               ");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "else            ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{                 ");
                            theJavaChunk.print(isBrokenString + paramObjectName + ".setParam(" + (paramInId[i] + inOffSet) + ",(oracle.sql.TIMESTAMPLTZ)null);   ");
                            theJavaChunk.print(isBrokenString + "}                 ");
                            theJavaChunk.unIndent();

                        } else if (oracleParamDatatype[i].equals("OracleTypes.INTERVALDS")) {
                            theJavaChunk.print(isBrokenString + "if (" + qualifiedParentVariableName + variableName[i] + " != null)  ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{       ");
                            theJavaChunk.print(isBrokenString + paramObjectName + ".setParam(" + (paramInId[i] + inOffSet) + ","
                                    + " new oracle.sql.INTERVALDS(" + qualifiedParentVariableName + variableName[i] + "));");
                            theJavaChunk.print(isBrokenString + "}               ");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "else            ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{                 ");
                            theJavaChunk.print(isBrokenString + paramObjectName + ".setParam(" + (paramInId[i] + inOffSet) + ",(oracle.sql.INTERVALDS)null);   ");
                            theJavaChunk.print(isBrokenString + "}                 ");
                            theJavaChunk.unIndent();

                        } else if (oracleParamDatatype[i].equals("OracleTypes.INTERVALYM")) {
                            theJavaChunk.print(isBrokenString + "if (" + qualifiedParentVariableName + variableName[i] + " != null)  ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{       ");
                            theJavaChunk.print(isBrokenString + paramObjectName + ".setParam(" + (paramInId[i] + inOffSet) + ","
                                    + " new oracle.sql.INTERVALYM(" + qualifiedParentVariableName + variableName[i] + "));");
                            theJavaChunk.print(isBrokenString + "}               ");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "else            ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{                 ");
                            theJavaChunk.print(isBrokenString + paramObjectName + ".setParam(" + (paramInId[i] + inOffSet) + ",(oracle.sql.INTERVALYM)null);   ");
                            theJavaChunk.print(isBrokenString + "}                 ");
                            theJavaChunk.unIndent();

                        }         //
                        else if (oracleParamDatatype[i].equals("OracleTypes.OPAQUE")
                        ) {
                            theJavaChunk.print(isBrokenString + "try  ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{");

                            theJavaChunk.print(isBrokenString + "if (" + qualifiedParentVariableName + variableName[i] + " != null || " + qualifiedParentVariableName + byteName[i] + " != null)");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{");
                            theJavaChunk.print(isBrokenString + "if (" + qualifiedParentVariableName + byteName[i] + " == null)");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{");
                            if (useCharForCLOB) {
                                theJavaChunk.print(isBrokenString + qualifiedParentVariableName + byteName[i] + " = com.mcpdbwizard.pub.IOUtils.loadFileIntoCharArray(" + qualifiedParentVariableName + variableName[i] + ");");
                            } else {
                                theJavaChunk.print(isBrokenString + qualifiedParentVariableName + byteName[i] + " = com.mcpdbwizard.pub.IOUtils.loadFileIntoByteArray(" + qualifiedParentVariableName + variableName[i] + ");");
                            }
                            theJavaChunk.print(isBrokenString + "}");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "");
                            if (useCharForCLOB) {
                                theJavaChunk.print(isBrokenString + "String tempString = new String(" + qualifiedParentVariableName + byteName[i] + ");");  //DRKLUGE
                                theJavaChunk.print(isBrokenString + "java.io.ByteArrayInputStream inStr = new java.io.ByteArrayInputStream(tempString.getBytes());");  //DRKLUGE
                            } else {
                                theJavaChunk.print(isBrokenString + "java.io.ByteArrayInputStream inStr = new java.io.ByteArrayInputStream(" + qualifiedParentVariableName + byteName[i] + ");");  //DRKLUGE
                            }
                            theJavaChunk.print(isBrokenString + qualifiedParentVariableName + lobName[i] + " = new oracle.xdb.XMLType(theConnection, inStr);");
                            theJavaChunk.print(isBrokenString + "}");
                            theJavaChunk.unIndent();
                            //theJavaChunk.print(isBrokenString + "else");

                            /**
                             if (byteArrayInParam == null)
                             {
                             byteArrayInParam = IOUtils.loadFileIntoByteArray(filelelel);
                             }

                             lobInParam = new oracle.xdb.XMLType(theConnection, new java.io.ByteArrayInputStream(byteArrayInParam));
                             ***/
                            //    +      byteName[i] + "));");
                            //lobInParam = new oracle.xdb.XMLType(theConnection, new java.io.ByteArrayInputStream(byteArrayInParam));");
                            //if (! targetVersion.startsWith("8"))
                            //  {
                            //  theJavaChunk.print(isBrokenString + "if (" + qualifiedParentVariableName + variableName[i] + " != null || " + qualifiedParentVariableName + byteName[i] + " != null)");
                            //  theJavaChunk.indent();
                            //  theJavaChunk.print(isBrokenString + "{                 ");
                            //theJavaChunk.print(isBrokenString + "if (" + qualifiedParentVariableName + lobName[i] + " == null)");
                            //theJavaChunk.indent();
                            //theJavaChunk.print(isBrokenString + "{                 ");
                            //theJavaChunk.print(isBrokenString + "if (createTempLobsIfNeeded)");
                            //theJavaChunk.indent();
                            //theJavaChunk.print(isBrokenString + "{                 ");
                            //if (comments) theJavaChunk.print("// Create temporary LOB so call will work");
                            //if (oracleParamDatatype[i].equals("OracleTypes.OPAQUE"))
                            //  {
                            //  theJavaChunk.print(isBrokenString + qualifiedParentVariableName + lobName[i]  + " = oracle.sql.CLOB.createTemporary(theConnection, true, oracle.sql.CLOB.DURATION_SESSION);");
                            //  }
                            //else
                            //  {
                            //  theJavaChunk.print(isBrokenString + qualifiedParentVariableName + lobName[i]  + " = oracle.sql.BLOB.createTemporary(theConnection, true, oracle.sql.BLOB.DURATION_SESSION);");
                            //  }
                            //theJavaChunk.print(isBrokenString + "}                 ");
                            //theJavaChunk.unIndent();
                            //theJavaChunk.print(isBrokenString + "else");
                            //theJavaChunk.indent();
                            //theJavaChunk.print(isBrokenString + "{                 ");
                            //if (comments) theJavaChunk.print("// We have a file but no LOB to associate it with...");
                            //theJavaChunk.print(isBrokenString + "throw (new CSException(\"No LOB Pointer Provided\"));");
                            //theJavaChunk.print(isBrokenString + "}                 ");
                            //theJavaChunk.unIndent();
                            //theJavaChunk.print(isBrokenString + "}                 ");
                            //theJavaChunk.print(isBrokenString + " ");
                            //theJavaChunk.unIndent();
                            //}/
                            /**
                             else
                             {
                             theJavaChunk.print(isBrokenString + "if (" + qualifiedParentVariableName + variableName[i] + " != null || " + qualifiedParentVariableName + byteName[i] + " != null)");
                             theJavaChunk.indent();
                             theJavaChunk.print(isBrokenString + "{  ");
                             }

                             if (   targetVersion.equals("8.1.5")
                             || targetVersion.equals("8.1.6")
                             || targetVersion.equals("8.1.7"))
                             {
                             theJavaChunk.print("if (   " + qualifiedParentVariableName +  byteName[i]      + " != null && " + qualifiedParentVariableName +  lobName[i]  + ".length() > "+qualifiedParentVariableName + byteName[i]+".length      ");
                             theJavaChunk.print("    || " + qualifiedParentVariableName +  variableName[i]  + " != null && " + qualifiedParentVariableName +  lobName[i]  + ".length() > "+qualifiedParentVariableName + variableName[i]+".length())      ");
                             theJavaChunk.indent();
                             theJavaChunk.print("{     ");
                             if (comments) theJavaChunk.print("// If the LOB is currently longer than the value we are trying to update it with                                ");
                             if (comments) theJavaChunk.print("// we have to set the length back to zero first. From 9i this can be done using the driver.        ");
                             // theJavaChunk.print("throw new CSException(\"Unable to update LOB as new value is shorter than existing value\");                                                                                             ");
                             if (   oracleParamDatatype[i].equals("OracleTypes.BLOB"))
                             {
                             theJavaChunk.print("blobTrim(" + qualifiedParentVariableName +  lobName[i] + ", 0);");
                             hasBlobTrimMethod = true;
                             }
                             else
                             {
                             theJavaChunk.print("clobTrim(" + qualifiedParentVariableName +  lobName[i] + ", 0);");
                             hasClobTrimMethod = true;
                             }
                             theJavaChunk.print("}                                                                                                             ");
                             theJavaChunk.unIndent();
                             theJavaChunk.print("                                                                                    ");
                             }
                             else
                             {
                             theJavaChunk.print("if (   " + qualifiedParentVariableName +  byteName[i]      + " != null && " + qualifiedParentVariableName +  lobName[i]  + ".length() > "+qualifiedParentVariableName + byteName[i]+".length      ");
                             theJavaChunk.print("    || " + qualifiedParentVariableName +  variableName[i]  + " != null && " + qualifiedParentVariableName +  lobName[i]  + ".length() > "+qualifiedParentVariableName + variableName[i]+".length())      ");
                             theJavaChunk.indent();
                             theJavaChunk.print("{                                                                                                                   ");
                             if (comments) theJavaChunk.print("// If the LOB is currently longer than the value we are trying to update it with                                  ");
                             if (comments) theJavaChunk.print("// we have to set the length back to zero first.    ");
                             if (comments) theJavaChunk.print("// In 9i we use trim() for this. In 10g we use truncate().");
                             if (   targetVersion.equals("9.0.1")
                             || targetVersion.equals("9.2.0")
                             )
                             {
                             theJavaChunk.print(qualifiedParentVariableName +  lobName[i] + ".trim(0);   ");
                             }
                             else
                             {
                             theJavaChunk.print(qualifiedParentVariableName +  lobName[i] + ".truncate(0);   ");
                             }
                             theJavaChunk.print("} ");
                             theJavaChunk.unIndent();

                             }

                             // qualifiedParentVariableName + byteName[i]
                             theJavaChunk.print(isBrokenString + "if (" + qualifiedParentVariableName + byteName[i] + " != null)");
                             theJavaChunk.indent();
                             theJavaChunk.print(isBrokenString + "{                                                                                                                   ");
                             theJavaChunk.print(isBrokenString + "com.mcpdbwizard.pub.LongObjectLoader.load" + realDataType + "(" + qualifiedParentVariableName +  lobName[i] + "," + qualifiedParentVariableName + byteName[i] + ");     ");
                             theJavaChunk.print(isBrokenString + "} ");
                             theJavaChunk.unIndent();
                             theJavaChunk.print(isBrokenString + "else");
                             theJavaChunk.indent();
                             theJavaChunk.print(isBrokenString + "{                                                                                                                   ");
                             theJavaChunk.print(isBrokenString + "com.mcpdbwizard.pub.LongObjectLoader.load" + realDataType + "(" + qualifiedParentVariableName +  lobName[i] + "," + qualifiedParentVariableName + variableName[i] + "," + sbufferSize + ");     ");
                             theJavaChunk.print(isBrokenString + "} ");
                             theJavaChunk.unIndent();
                             theJavaChunk.print(isBrokenString + "}   ");
                             theJavaChunk.unIndent();

                             if (   targetVersion.equals("8.1.5")
                             || targetVersion.equals("8.1.6")
                             || targetVersion.equals("8.1.7"))
                             {
                             theJavaChunk.print("else if (" + qualifiedParentVariableName +  lobName[i]  + " == null && ("+qualifiedParentVariableName + variableName[i]+" != null || "+ qualifiedParentVariableName + byteName[i]+ " != null ))     ");
                             theJavaChunk.indent();
                             theJavaChunk.print("{ ");
                             if (comments) theJavaChunk.print("// We have a file but no LOB to associate it with...");
                             if (comments) theJavaChunk.print("// " + Namer.param_prod_name + " can use Temporary Lobs to get round this,");
                             if (comments) theJavaChunk.print("// but only if the Oracle DB Version is 9.0.1 or higher.");
                             theJavaChunk.print(isBrokenString + "throw (new CSException(\"No LOB Pointer Provided\"));");
                             theJavaChunk.print("}                                                                                                                   ");
                             theJavaChunk.unIndent();
                             }
                             **/
                            theJavaChunk.print("                                                                                     ");


                            theJavaChunk.print(isBrokenString + paramObjectName + ".setParam(" + (paramInId[i] + inOffSet) + ",\"SYS." + realDataType + "\", "
                                    + qualifiedParentVariableName + lobName[i] + ");");
                            theJavaChunk.print(isBrokenString + "}   ");
                            theJavaChunk.unIndent();

            /*\
    catch (java.lang.NoClassDefFoundError e)
      {
      throw (new CSException("paramInParam may be missing an XML parser, usually in xmlparserv2.jar:" + e.toString()));
      }
            */
                            theJavaChunk.print(isBrokenString + "catch (java.lang.NoClassDefFoundError e)   ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{           ");
                            //theJavaChunk.print(isBrokenString + "throw (new CSException(" + qualifiedParentVariableName + variableName[i] + ".getAbsolutePath() + \" could not be loaded:\" + e.toString()));   ");
                            theJavaChunk.print(isBrokenString + "throw (new CSException(\"" + qualifiedParentVariableName + variableName[i] + " may be missing an XML parser, usually in xmlparserv2.jar:\" + e.toString()));   ");
                            theJavaChunk.print(isBrokenString + "}          ");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "catch (SQLException e)   ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{           ");
                            //theJavaChunk.print(isBrokenString + "throw (new CSException(" + qualifiedParentVariableName + variableName[i] + ".getAbsolutePath() + \" could not be loaded:\" + e.toString()));   ");
                            theJavaChunk.print(isBrokenString + "throw (new CSException(\"" + qualifiedParentVariableName + variableName[i] + " could not be loaded SQLException:\" + e.toString()));   ");
                            theJavaChunk.print(isBrokenString + "}          ");
                            theJavaChunk.unIndent();
            /*
              }
            else
              {
              theJavaChunk.print(isBrokenString + "catch (SQLException e)   ");
              theJavaChunk.indent();
              theJavaChunk.print(isBrokenString + "{           ");
              theJavaChunk.print(isBrokenString + "throw (new CSException(" + qualifiedParentVariableName + variableName[i] + ".getAbsolutePath() + \" could not be loaded:\" + e.toString()));   ");
              theJavaChunk.print(isBrokenString + "}          ");
              theJavaChunk.unIndent();
              }
            */
                            theJavaChunk.print(isBrokenString + "catch (CSException e)   ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{           ");
                            //theJavaChunk.print(isBrokenString + "throw (new CSException(" + qualifiedParentVariableName + variableName[i] + ".getAbsolutePath() + \" could not be loaded:\" + e.toString()));   ");
                            theJavaChunk.print(isBrokenString + "throw (new CSException(\"" + qualifiedParentVariableName + variableName[i] + " could not be loaded:\" + e.toString()));   ");
                            theJavaChunk.print(isBrokenString + "}          ");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "");
                        } else if (oracleParamDatatype[i].equals("OracleTypes.BLOB")
                                || oracleParamDatatype[i].equals("OracleTypes.CLOB")
                        ) {
                            theJavaChunk.print(isBrokenString + "try  ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{  ");
                            if (!targetVersion.startsWith("8")) {
                                theJavaChunk.print(isBrokenString + "if (" + qualifiedParentVariableName + variableName[i] + " != null || " + qualifiedParentVariableName + byteName[i] + " != null)");
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{                 ");
                                theJavaChunk.print(isBrokenString + "if (" + qualifiedParentVariableName + lobName[i] + " == null)");
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{                 ");
                                theJavaChunk.print(isBrokenString + "if (createTempLobsIfNeeded)");
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{                 ");
                                if (comments) theJavaChunk.print("// Create temporary LOB so call will work");
                                if (oracleParamDatatype[i].equals("OracleTypes.CLOB")) {
                                    theJavaChunk.print(isBrokenString + qualifiedParentVariableName + lobName[i] + " = oracle.sql.CLOB.createTemporary(theConnection, true, oracle.sql.CLOB.DURATION_SESSION);");
                                } else {
                                    theJavaChunk.print(isBrokenString + qualifiedParentVariableName + lobName[i] + " = oracle.sql.BLOB.createTemporary(theConnection, true, oracle.sql.BLOB.DURATION_SESSION);");
                                }
                                theJavaChunk.print(isBrokenString + "}                 ");
                                theJavaChunk.unIndent();
                                theJavaChunk.print(isBrokenString + "else");
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{                 ");
                                if (comments)
                                    theJavaChunk.print("// We have a file but no LOB to associate it with...");
                                theJavaChunk.print(isBrokenString + "throw (new CSException(\"No LOB Pointer Provided\"));");
                                theJavaChunk.print(isBrokenString + "}                 ");
                                theJavaChunk.unIndent();
                                theJavaChunk.print(isBrokenString + "}                 ");
                                theJavaChunk.print(isBrokenString + " ");
                                theJavaChunk.unIndent();
                            } else {
                                theJavaChunk.print(isBrokenString + "if (" + qualifiedParentVariableName + variableName[i] + " != null || " + qualifiedParentVariableName + byteName[i] + " != null)");
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{  ");
                            }

                            if (targetVersion.equals("8.1.5")
                                    || targetVersion.equals("8.1.6")
                                    || targetVersion.equals("8.1.7")) {
                                theJavaChunk.print("if (   " + qualifiedParentVariableName + byteName[i] + " != null && " + qualifiedParentVariableName + lobName[i] + ".length() > " + qualifiedParentVariableName + byteName[i] + ".length      ");
                                theJavaChunk.print("    || " + qualifiedParentVariableName + variableName[i] + " != null && " + qualifiedParentVariableName + lobName[i] + ".length() > " + qualifiedParentVariableName + variableName[i] + ".length())      ");
                                theJavaChunk.indent();
                                theJavaChunk.print("{     ");
                                if (comments)
                                    theJavaChunk.print("// If the LOB is currently longer than the value we are trying to update it with                                ");
                                if (comments)
                                    theJavaChunk.print("// we have to set the length back to zero first. From 9i this can be done using the driver.        ");
                                // theJavaChunk.print("throw new CSException(\"Unable to update LOB as new value is shorter than existing value\");                                                                                             ");
                                if (oracleParamDatatype[i].equals("OracleTypes.BLOB")) {
                                    theJavaChunk.print("blobTrim(" + qualifiedParentVariableName + lobName[i] + ", 0);");
                                    hasBlobTrimMethod = true;
                                } else {
                                    theJavaChunk.print("clobTrim(" + qualifiedParentVariableName + lobName[i] + ", 0);");
                                    hasClobTrimMethod = true;
                                }
                                theJavaChunk.print("}                                                                                                             ");
                                theJavaChunk.unIndent();
                                theJavaChunk.print("                                                                                    ");
                            } else {
                                theJavaChunk.print("if (   " + qualifiedParentVariableName + byteName[i] + " != null && " + qualifiedParentVariableName + lobName[i] + ".length() > " + qualifiedParentVariableName + byteName[i] + ".length      ");
                                theJavaChunk.print("    || " + qualifiedParentVariableName + variableName[i] + " != null && " + qualifiedParentVariableName + lobName[i] + ".length() > " + qualifiedParentVariableName + variableName[i] + ".length())      ");
                                theJavaChunk.indent();
                                theJavaChunk.print("{                                                                                                                   ");
                                if (comments)
                                    theJavaChunk.print("// If the LOB is currently longer than the value we are trying to update it with                                  ");
                                if (comments)
                                    theJavaChunk.print("// we have to set the length back to zero first.    ");
                                if (comments)
                                    theJavaChunk.print("// In 9i we use trim() for this. In 10g we use truncate().");
                                if (targetVersion.equals("9.0.1")
                                        || targetVersion.equals("9.2.0")
                                ) {
                                    theJavaChunk.print(qualifiedParentVariableName + lobName[i] + ".trim(0);   ");
                                } else {
                                    theJavaChunk.print(qualifiedParentVariableName + lobName[i] + ".truncate(0);   ");
                                }
                                theJavaChunk.print("} ");
                                theJavaChunk.unIndent();

                            }

// qualifiedParentVariableName + byteName[i]
                            theJavaChunk.print(isBrokenString + "if (" + qualifiedParentVariableName + byteName[i] + " != null)");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{                                                                                                                   ");
                            theJavaChunk.print(isBrokenString + "com.mcpdbwizard.pub.LongObjectLoader.load" + realDataType + "(" + qualifiedParentVariableName + lobName[i] + "," + qualifiedParentVariableName + byteName[i] + ");     ");
                            theJavaChunk.print(isBrokenString + "} ");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "else");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{                                                                                                                   ");
                            theJavaChunk.print(isBrokenString + "com.mcpdbwizard.pub.LongObjectLoader.load" + realDataType + "(" + qualifiedParentVariableName + lobName[i] + "," + qualifiedParentVariableName + variableName[i] + "," + sbufferSize + ");     ");
                            theJavaChunk.print(isBrokenString + "} ");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "}   ");
                            theJavaChunk.unIndent();

                            if (targetVersion.equals("8.1.5")
                                    || targetVersion.equals("8.1.6")
                                    || targetVersion.equals("8.1.7")) {
                                theJavaChunk.print("else if (" + qualifiedParentVariableName + lobName[i] + " == null && (" + qualifiedParentVariableName + variableName[i] + " != null || " + qualifiedParentVariableName + byteName[i] + " != null ))     ");
                                theJavaChunk.indent();
                                theJavaChunk.print("{ ");
                                if (comments)
                                    theJavaChunk.print("// We have a file but no LOB to associate it with...");
                                if (comments)
                                    theJavaChunk.print("// " + Namer.param_prod_name + " can use Temporary Lobs to get round this,");
                                if (comments)
                                    theJavaChunk.print("// but only if the Oracle DB Version is 9.0.1 or higher.");
                                theJavaChunk.print(isBrokenString + "throw (new CSException(\"No LOB Pointer Provided\"));");
                                theJavaChunk.print("}                                                                                                                   ");
                                theJavaChunk.unIndent();
                            }

                            theJavaChunk.print("                                                                                     ");

                            theJavaChunk.print(isBrokenString + paramObjectName + ".setParam(" + (paramInId[i] + inOffSet) + ","
                                    + qualifiedParentVariableName + lobName[i] + ");");
                            theJavaChunk.print(isBrokenString + "}   ");
                            theJavaChunk.unIndent();

            /*\
            if (   version.equals("8.1.5")
                || version.equals("8.1.6")
                || version.equals("8.1.7"))
              {
            */
                            theJavaChunk.print(isBrokenString + "catch (SQLException e)   ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{           ");
                            //theJavaChunk.print(isBrokenString + "throw (new CSException(" + qualifiedParentVariableName + variableName[i] + ".getAbsolutePath() + \" could not be loaded:\" + e.toString()));   ");
                            theJavaChunk.print(isBrokenString + "throw (new CSException(\"" + qualifiedParentVariableName + variableName[i] + " could not be loaded SQLException:\" + e.toString()));   ");
                            theJavaChunk.print(isBrokenString + "}          ");
                            theJavaChunk.unIndent();
            /*
              }
            else
              {
              theJavaChunk.print(isBrokenString + "catch (SQLException e)   ");
              theJavaChunk.indent();
              theJavaChunk.print(isBrokenString + "{           ");
              theJavaChunk.print(isBrokenString + "throw (new CSException(" + qualifiedParentVariableName + variableName[i] + ".getAbsolutePath() + \" could not be loaded:\" + e.toString()));   ");
              theJavaChunk.print(isBrokenString + "}          ");
              theJavaChunk.unIndent();
              }
            */
                            theJavaChunk.print(isBrokenString + "catch (CSException e)   ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{           ");
                            //theJavaChunk.print(isBrokenString + "throw (new CSException(" + qualifiedParentVariableName + variableName[i] + ".getAbsolutePath() + \" could not be loaded:\" + e.toString()));   ");
                            theJavaChunk.print(isBrokenString + "throw (new CSException(\"" + qualifiedParentVariableName + variableName[i] + " could not be loaded:\" + e.toString()));   ");
                            theJavaChunk.print(isBrokenString + "}          ");
                            theJavaChunk.unIndent();
                        } else if (oracleParamDatatype[i].equals("OracleTypes.BFILE")
                        ) {
                            if (useVariable[i]) {
                                theJavaChunk.print(isBrokenString + "try  ");
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{  ");

                                // BFILES can not be loaded...
                                //
                                //   {
                                //   theJavaChunk.print(isBrokenString + "if (" + lobName[i] + " != null && " + variableName[i] + " != null)");
                                //   theJavaChunk.indent();
                                //   theJavaChunk.print(isBrokenString + "{  ");
                                //   theJavaChunk.print(isBrokenString + "" + sobjectLoader + ".load" + realDataType + "(" + lobName[i] + "," + variableName[i] + "," + sbufferSize + ");     ");
                                //   theJavaChunk.print(isBrokenString + "}   ");
                                //   theJavaChunk.unIndent();
                                //   }

                                theJavaChunk.print(isBrokenString + paramObjectName + ".setParam(" + (paramInId[i] + inOffSet) + ","
                                        + qualifiedParentVariableName + lobName[i] + ");");
                                theJavaChunk.print(isBrokenString + "}   ");
                                theJavaChunk.unIndent();
                                theJavaChunk.print(isBrokenString + "catch (CSException e)   ");
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{           ");
                                theJavaChunk.print(isBrokenString + "throw (new CSException(" + qualifiedParentVariableName + variableName[i] + ".getAbsolutePath() + \" could not be loaded:\" + e.toString()));   ");
                                theJavaChunk.print(isBrokenString + "}          ");
                                theJavaChunk.unIndent();
                            } else {
                                theJavaChunk.print(isBrokenString + "try  ");
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{  ");
                                theJavaChunk.print(isBrokenString + paramObjectName + ".setParam(" + (paramInId[i] + inOffSet) + ","
                                        + qualifiedParentVariableName + lobName[i] + ");");
                                theJavaChunk.print(isBrokenString + "}   ");
                                theJavaChunk.unIndent();
                                theJavaChunk.print(isBrokenString + "catch (CSException e)   ");
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{           ");
                                theJavaChunk.print(isBrokenString + "throw (new CSException(" + lobName[i] + " + \" could not be loaded:\" + e.toString()));   ");
                                theJavaChunk.print(isBrokenString + "}          ");
                                theJavaChunk.unIndent();
                            }
                        } else if (oracleParamDatatype[i].equals("OracleTypes.PLSQL_INDEX_TABLE")
                        ) {
                            theJavaChunk.print(isBrokenString + "try  ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{  ");

                            if (plsqlIndexByDataType[i] == OracleTypes.VARCHAR) {
                                theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + ".setDataType(oracle.jdbc.OracleTypes.VARCHAR);");
                            } else if (plsqlIndexByDataType[i] == OracleTypes.DATE) {
                                theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + ".setDataType(oracle.jdbc.OracleTypes.DATE);");
                            } else if (plsqlIndexByDataType[i] == OracleTypes.TIMESTAMP) {
                                theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + ".setDataType(oracle.jdbc.OracleTypes.TIMESTAMP);");
                            } else if (plsqlIndexByDataType[i] == OracleTypes.TIMESTAMPTZ) {
                                theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + ".setDataType(oracle.jdbc.OracleTypes.TIMESTAMPTZ);");
                            } else if (plsqlIndexByDataType[i] == OracleTypes.TIMESTAMPLTZ) {
                                theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + ".setDataType(oracle.jdbc.OracleTypes.TIMESTAMPLTZ);");
                            } else if (plsqlIndexByDataType[i] == OracleTypes.RAW) {
                                theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + ".setDataType(oracle.jdbc.OracleTypes.RAW);");
                            } else if (plsqlIndexByDataType[i] == OracleTypes.NUMBER) {
                                if (targetVersion.startsWith("DB2")) {
                                    theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + ".setDataType(Types.NUMERIC);");
                                } else {
                                    theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + ".setDataType(oracle.jdbc.OracleTypes.NUMBER);");
                                }
                            } else {
                                theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + ".setDataType(" + plsqlIndexByDataType[i] + ");");
                            }

                            theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + ".setElementMaxLength(" + plsqlIndexByDataLength[i] + ");");

                            // A zoned element is converted by a mask carrying TZR, and Oracle stops
                            // tolerating a missing fractional-seconds part once the mask names a
                            // zone -- '2019-03-01 14:25:36' raises ORA-01843 under it while the
                            // unzoned mask accepts it. Normalising here, in the generated source
                            // where it is visible, keeps every string that worked before working,
                            // without weakening the mask and losing region names. Emitted ONLY for
                            // the zoned types: nothing else needs it and a blanket call would be
                            // touching values it has no business touching.
                            if (plsqlIndexByDataType[i] == OracleTypes.TIMESTAMPTZ
                                    || plsqlIndexByDataType[i] == OracleTypes.TIMESTAMPLTZ) {
                                theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + ".ensureFractionalSeconds();");
                            }

                            theJavaChunk.print(isBrokenString + paramObjectName + ".setPlSqlIndexArrayParam(" + (paramInId[i] + inOffSet) + ","
                                    + qualifiedParentVariableName + variableName[i] + ");");

                            theJavaChunk.print(isBrokenString + "}   ");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "catch (Exception e)   ");
                            theJavaChunk.indent();

                            theJavaChunk.print(isBrokenString + "{           ");
                            theJavaChunk.print(isBrokenString + "throw (new CSException(\"PL/SQL Index By Table Input Parameter " + qualifiedParentVariableName + variableName[i] + "  could not be loaded:\" + e.toString()));   ");
                            theJavaChunk.print(isBrokenString + "}          ");
                            theJavaChunk.print(isBrokenString + "   ");
                            theJavaChunk.unIndent();
                        } else if (oracleParamDatatype[i].equals("SqlUtils.ORACLE_TABLE_DATATYPE")
                                || oracleParamDatatype[i].equals("SqlUtils.ORACLE_VARRAY_DATATYPE")
                                || oracleParamDatatype[i].equals("SqlUtils.ORACLE_PLSQL_INDEXBY_ROWTYPE_DATATYPE")
                        ) {
                            theJavaChunk.print(isBrokenString + "try  ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{  ");

                            theJavaChunk.print(isBrokenString + paramObjectName + ".setPlSqlTableArrayParam(" + (paramInId[i] + inOffSet) + ","
                                    + qualifiedParentVariableName + variableName[i] + ");");

                            theJavaChunk.print(isBrokenString + "}   ");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "catch (Exception e)   ");
                            theJavaChunk.indent();

                            theJavaChunk.print(isBrokenString + "{           ");
                            theJavaChunk.print(isBrokenString + "throw (new CSException(\"PL/SQL Array Input Parameter " + qualifiedParentVariableName + variableName[i] + "  could not be loaded:\" + e.toString()));   ");
                            theJavaChunk.print(isBrokenString + "}          ");
                            theJavaChunk.print(isBrokenString + "   ");
                            theJavaChunk.unIndent();
                        }
                        //else if (   oracleParamDatatype[i].equals("OracleTypes.OPAQUE"))
                        //   {
                        //  theJavaChunk.print(isBrokenString + paramObjectName + ".setParam(" + (paramInId[i] + inOffSet) + ", \"SYS." + realDataType + "\", "
                        //  + qualifiedParentVariableName + variableName[i] + ");");
                        //  }
                        else if (oracleParamDatatype[i].equals("SqlUtils.ORACLE_SDO_GEOMETRY_DATATYPE")
                        ) {
                            theJavaChunk.print(isBrokenString + "try  ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{  ");

                            theJavaChunk.print(isBrokenString + "if  (" + qualifiedParentVariableName + variableName[i] + " == null)");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{  ");
                            theJavaChunk.print(isBrokenString + paramObjectName + ".setParam(" + (paramInId[i] + inOffSet) + ","
                                    + "\"SDO_GEOMETRY\",(oracle.sql.STRUCT)null);");

                            theJavaChunk.print(isBrokenString + "}   ");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "else");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{  ");

                            theJavaChunk.print(isBrokenString + paramObjectName + ".setParam(" + (paramInId[i] + inOffSet) + ","
                                    + "\"SDO_GEOMETRY\",oracle.spatial.geometry.JGeometry.store(JGeometryWrapper.jGeometryUnWrapper("
                                    + qualifiedParentVariableName + variableName[i] + "),theConnection));");

                            theJavaChunk.print(isBrokenString + "}   ");
                            theJavaChunk.unIndent();

                            theJavaChunk.print(isBrokenString + "}   ");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "catch (Exception e)   ");
                            theJavaChunk.indent();

                            theJavaChunk.print(isBrokenString + "{           ");
                            theJavaChunk.print(isBrokenString + "throw (new CSException(\"JGeometry Input Parameter " + qualifiedParentVariableName + variableName[i] + "  could not be loaded:\" + e.toString()));   ");
                            theJavaChunk.print(isBrokenString + "}          ");
                            theJavaChunk.print(isBrokenString + "   ");
                            theJavaChunk.unIndent();
                        } else if (oracleParamDatatype[i].equals("OracleTypes.BOOLEAN")) {
                            // Native ISO-SQL BOOLEAN (23ai): bind via the native-boolean
                            // setter so it is NOT converted to the legacy PL/SQL ±1 number.
                            theJavaChunk.print(isBrokenString + paramObjectName + ".setNativeBooleanParam(" + (paramInId[i] + inOffSet) + ","
                                    + qualifiedParentVariableName + variableName[i] + ");");
                        } else {
                            theJavaChunk.print(isBrokenString + paramObjectName + ".setParam(" + (paramInId[i] + inOffSet) + ","
                                    + qualifiedParentVariableName + variableName[i] + ");");
                        }

                    }

                    if (true) {
                        if ((inOut.equals("OUT")
                                || inOut.equals("IN/OUT"))
                                && paramOutId[i] > 0 // Out known
                        ) {
                            if (debugMessages) {
                                theJavaChunk.print(isBrokenString + stheLog + ".debug(\"binding output parameter " + qualifiedParentVariableName + variableName[i] + " to position " + (paramOutId[i] + outOffSet) + "\" );");
                            }

                            if (oracleParamDatatype[i] == "SqlUtils.ORACLE_OTHER_DATATYPE"
                                    || oracleParamDatatype[i] == "SqlUtils.ORACLE_ROWTYPE_DATATYPE"
                                    //   || oracleParamDatatype[i] == "SqlUtils.ORACLE_TABLE_DATATYPE"
                                    //   || oracleParamDatatype[i] == "SqlUtils.ORACLE_VARRAY_DATATYPE"
                                    || oracleParamDatatype[i] == "SqlUtils.ORACLE_OBJECT_DATATYPE") {
                            } else if (oracleParamDatatype[i].equals("SqlUtils.ORACLE_TABLE_DATATYPE")
                                    || oracleParamDatatype[i].equals("SqlUtils.ORACLE_VARRAY_DATATYPE")
                                    || oracleParamDatatype[i].equals("SqlUtils.ORACLE_PLSQL_INDEXBY_ROWTYPE_DATATYPE")) {
                                theJavaChunk.print(isBrokenString + paramObjectName + ".setPlSqlTableArrayOutParam(" + (paramOutId[i] + outOffSet) + "," + qualifiedParentVariableName + variableName[i] + ");");
                            } else if (actualOracleDatatype[i].equals("PL/SQL TABLE")
                                    && oracleUnderlyingDatatype[i] == SingleNamespaceObject.PLSQL_INDEXBY_ARRAY_ROWTYPE) {
                                theJavaChunk.print(isBrokenString + paramObjectName + ".setPlSqlTableArrayOutParam(" + (paramOutId[i] + outOffSet) + "," + qualifiedParentVariableName + variableName[i] + ");");
                            } else if (oracleParamDatatype[i] == "OracleTypes.PLSQL_INDEX_TABLE") {
                                theJavaChunk.print(isBrokenString + "try  ");
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{  ");

                                if (inOut.equals("OUT")) {


                                    if (plsqlIndexByDataType[i] == OracleTypes.VARCHAR) {
                                        theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + ".setDataType(oracle.jdbc.OracleTypes.VARCHAR);");
                                    } else if (plsqlIndexByDataType[i] == OracleTypes.DATE) {
                                        theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + ".setDataType(oracle.jdbc.OracleTypes.DATE);");
                                    } else if (plsqlIndexByDataType[i] == OracleTypes.TIMESTAMP) {
                                        theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + ".setDataType(oracle.jdbc.OracleTypes.TIMESTAMP);");
                                    } else if (plsqlIndexByDataType[i] == OracleTypes.TIMESTAMPTZ) {
                                        theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + ".setDataType(oracle.jdbc.OracleTypes.TIMESTAMPTZ);");
                                    } else if (plsqlIndexByDataType[i] == OracleTypes.TIMESTAMPLTZ) {
                                        // No ensureFractionalSeconds() here, deliberately: this is
                                        // the pure-OUT arm, so there is no caller-supplied value to
                                        // normalise. An IN OUT parameter takes the IN path above.
                                        theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + ".setDataType(oracle.jdbc.OracleTypes.TIMESTAMPLTZ);");
                                    } else if (plsqlIndexByDataType[i] == OracleTypes.NUMBER) {
                                        if (targetVersion.startsWith("DB2")) {
                                            theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + ".setDataType(Types.NUMERIC);");
                                        } else {
                                            theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + ".setDataType(oracle.jdbc.OracleTypes.NUMBER);");
                                        }
                                    } else if (plsqlIndexByDataType[i] == OracleTypes.RAW) {
                                        theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + ".setDataType(oracle.jdbc.OracleTypes.RAW);");
                                    } else {
                                        theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + ".setDataType(" + plsqlIndexByDataType[i] + ");");
                                    }

                                    theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + ".setElementMaxLength(" + plsqlIndexByDataLength[i] + ");");
                                }


                                theJavaChunk.print(isBrokenString + paramObjectName + ".setPlSqlIndexArrayOutParam(" + (paramOutId[i] + outOffSet) + "," + qualifiedParentVariableName + variableName[i] + ");");
                                theJavaChunk.print(isBrokenString + "}   ");
                                theJavaChunk.unIndent();
                                theJavaChunk.print(isBrokenString + "catch (Exception e)   ");
                                theJavaChunk.indent();

                                theJavaChunk.print(isBrokenString + "{           ");
                                theJavaChunk.print(isBrokenString + "throw (new CSException(\"PL/SQL Index By Table Output Parameter " + qualifiedParentVariableName + variableName[i] + " could not be registered:\" + e.toString()));   ");
                                theJavaChunk.print(isBrokenString + "}          ");
                                theJavaChunk.print(isBrokenString + "   ");
                                theJavaChunk.unIndent();
                            } else if (oracleParamDatatype[i].equals("SqlUtils.ORACLE_TABLE_DATATYPE")
                                    || oracleParamDatatype[i].equals("SqlUtils.ORACLE_VARRAY_DATATYPE")
                                    || oracleParamDatatype[i].equals("SqlUtils.ORACLE_PLSQL_INDEXBY_ROWTYPE_DATATYPE")) {
                                theJavaChunk.print(isBrokenString + paramObjectName + ".setPlSqlTableArrayOutParam(" + (paramOutId[i] + outOffSet) + "," + qualifiedParentVariableName + variableName[i] + ");");
                            } else if (oracleParamDatatype[i].equals("OracleTypes.OPAQUE")) {
                                String realDataType = theRowSet.getString("DATA_TYPE");
                                theJavaChunk.print(isBrokenString + paramObjectName + ".setOutParam(" + (paramOutId[i] + outOffSet) + "," + oracleParamDatatype[i] + ", \"SYS." + realDataType + "\");");
                            } else if (oracleParamDatatype[i].equals("SqlUtils.ORACLE_SDO_GEOMETRY_DATATYPE")) {
                                theJavaChunk.print(isBrokenString + paramObjectName + ".setOutParam(" + (paramOutId[i] + outOffSet) + ",OracleTypes.STRUCT,\"SDO_GEOMETRY\");");
                            } else if (targetVersion.startsWith("DB2") && oracleParamDatatype[i].equals("OracleTypes.CURSOR")) {
                                theJavaChunk.print(isBrokenString + paramObjectName + ".setOutParam(" + (paramOutId[i] + outOffSet) + ",com.ibm.db2.jcc.DB2Types.CURSOR);");
                            } else if (targetVersion.startsWith("DB2") && oracleParamDatatype[i].equals("OracleTypes.NUMBER")) {
                                theJavaChunk.print(isBrokenString + paramObjectName + ".setOutParam(" + (paramOutId[i] + outOffSet) + ",Types.NUMERIC);");
                            } else {
                                theJavaChunk.print(isBrokenString + paramObjectName + ".setOutParam(" + (paramOutId[i] + outOffSet) + "," + oracleParamDatatype[i] + ");");
                            }
                        }

                    }
                }

            } // End of for...

            if (isBroken) {
                if (comments) {
                    theJavaChunk.print("// We throw an exception because there is at least one parameter that we");
                    theJavaChunk.print("// do not know how to set.");
                }
                theJavaChunk.print("if (1 == 1) throw new com.mcpdbwizard.pub.CSUnsupportedDatatypeException(\"Unsupported data type seen\", \""
                        + theRowSet.getString("DATA_TYPE") + "\");");
            }

        } catch (CSNoDataInRowSetException e) {
            if (comments) theJavaChunk.print("// This procedure has no parameters and hence no bind code");
        } catch (java.lang.StringIndexOutOfBoundsException e) {
            if (comments) theJavaChunk.print("// This procedure has no parameters and hence no bind code");

            //String[] theLines = theJavaChunk.getLines();
        } catch (CSException e) {
            theLog.syserror(e, true, true);
        }


        if (parentVariableName == null) {
            if (bindObjectName != null && bindObjectName.length() > 0) {
                if (debugMessages) {
                    theJavaChunk.print(isBrokenString + stheLog + ".debug(\"Associating parameters with statement\");");
                }
                theJavaChunk.print(isBrokenString + paramObjectName + ".bindParameters(" + bindObjectName + "); ");
            }
        }

        if (debugMessages && parentVariableName == null) {
            theJavaChunk.print(isBrokenString + stheLog + ".debug(\"Finished binding parameters\");");
        }

        if (hasBlobTrimMethod && qualifiedParentVariableName.length() == 0) {
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/** ");
                theJavaChunk.print("* blobTrim - set length of a BLOB to zero.");
                theJavaChunk.print("* @param oracle.sql.BLOB theBlob A BLOB whose length needs to be trimmed.");
                theJavaChunk.print("* @param int newLength the new length for the BLOB.");
                theJavaChunk.print("* @throws SQLException");
                theJavaChunk.print("* @since 4.0.1697");
                theJavaChunk.print("*/");
            }
            theJavaChunk.print("public void blobTrim(oracle.sql.BLOB theBlob, int newLength) throws SQLException");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print(stheLog + ".debug(\"Calling DBMS_LOB.TRIM\");");
            theJavaChunk.print("CallableStatement lobTrimStatement = theConnection.prepareCall(\"BEGIN DBMS_LOB.TRIM(?,?); END; \");");
            theJavaChunk.print("lobTrimStatement.setBlob(1,theBlob);");
            theJavaChunk.print("lobTrimStatement.setInt(2,newLength);");
            theJavaChunk.print("lobTrimStatement.execute();");
            theJavaChunk.print(stheLog + ".debug(\"Finished calling DBMS_LOB.TRIM\");");
        }

        if (hasClobTrimMethod && qualifiedParentVariableName.length() == 0) {
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/** ");
                theJavaChunk.print("* clobTrim - set length of a CLOB to zero.");
                theJavaChunk.print("* @param oracle.sql.CLOB theBlob A CLOB whose length needs to be trimmed.");
                theJavaChunk.print("* @param int newLength the new length for the CLOB.");
                theJavaChunk.print("* @throws SQLException");
                theJavaChunk.print("* @since 4.0.1697");
                theJavaChunk.print("*/");
            }
            theJavaChunk.print("public void clobTrim(oracle.sql.CLOB theClob, int newLength) throws SQLException");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            if (debugMessages) theJavaChunk.print(stheLog + ".debug(\"Calling DBMS_LOB.TRIM\");");
            theJavaChunk.print("CallableStatement lobTrimStatement = theConnection.prepareCall(\"BEGIN DBMS_LOB.TRIM(?,?); END; \");");
            theJavaChunk.print("lobTrimStatement.setClob(1,theClob);");
            theJavaChunk.print("lobTrimStatement.setInt(2,newLength);");
            theJavaChunk.print("lobTrimStatement.execute();");
            if (debugMessages) theJavaChunk.print(stheLog + ".debug(\"Finished calling DBMS_LOB.TRIM\");");


        }


        if (bindOverRideInOut != null && (bindOverRideInOut.equals("IN/OUT")
                || bindOverRideInOut.equals("IN")
                || bindOverRideInOut.equals("OUT"))) {
            // replace out params for this method call....
            for (int i = 0; i < paramOutId.length; i++) {
                paramOutId[i] = oldParamOutId[i];
                paramInId[i] = oldParamInId[i];
            }
        }

        String bindResult2 = "";

        if (hasClobTrimMethod) {
            bindResult2 = "clob";
        }

        if (hasBlobTrimMethod) {
            bindResult2 = bindResult2 + " " + "blob";
        }

        return (bindResult2);

    }

    void addGetResultsCode(JavaChunk theJavaChunk, boolean comments, boolean debugMessages, boolean stats, int inOffSet, int outOffSet, String parentVariableName, String parentVariableClassName
            , boolean forceUseByte) {
        boolean weHaveRecursed = false;

        String qualifiedParentVariableName = null;

        if (parentVariableName != null) {
            qualifiedParentVariableName = parentVariableName + ".";
            theJavaChunk.print(isBrokenString + parentVariableName + " = new " + parentVariableClassName + "(" + stheLog + ");");
        } else {
            qualifiedParentVariableName = "";
        }

        if (parentVariableName != null) {
            weHaveRecursed = true;
        }

        try {

            if (!weHaveRecursed) {
                if (debugMessages) {
                    theJavaChunk.print(isBrokenString + stheLog + ".debug(\"Starting to unload data\");");
                }

                if (stats) {
                    theJavaChunk.print(isBrokenString + "startStatsTimer();");
                }

                if (hasFiles) {
                    theJavaChunk.print(isBrokenString + sobjectLoader + ".setBufferSize(" + sbufferSize + ");");
                    theJavaChunk.print(isBrokenString + sobjectLoader + ".setKeepFiles(" + skeepFiles + ");");
                    //theJavaChunk.print(isBrokenString + sobjectLoader + ".setKeepFiles(" + skeepFiles + ");");
                }

                theJavaChunk.print(isBrokenString + "");
                theJavaChunk.print(isBrokenString + "theParameters.unloadParameters(theCallableStatement); ");

            }

            for (int i = 0; i < theRowSet.size(); i++) {
                theRowSet.setCurrentRowNumber(i);
                if (theRowSet.getString("IN_OUT").equals("OUT")
                        || theRowSet.getString("IN_OUT").equals("IN/OUT")
                        || overRideInOut.equals("IN/OUT") //  this will break *something* - appraently it doesnt
                ) {
                    if (comments) {
                        theJavaChunk.print("");
                        if (theRowSet.getInt("POSITION") > 0) {
                            theJavaChunk.print("// Unload parameter " + qualifiedParentVariableName + theRowSet.getString("ARGUMENT_NAME"));
                        } else {
                            theJavaChunk.print("// Unload parameter that will hold function result");
                        }
                    }

                    switch (oracleUnderlyingDatatype[i]) {
                        case SqlUtils.ORACLE_LONGTEXT_DATATYPE:
                        case SqlUtils.ORACLE_LONG_BINARY_DATATYPE:
                        case SqlUtils.ORACLE_CLOB_DATATYPE:
                        case SqlUtils.ORACLE_BLOB_DATATYPE:
                        case SqlUtils.ORACLE_XMLTYPE_DATATYPE:
                        case SqlUtils.ORACLE_BFILE_DATATYPE: {

                            theJavaChunk.print("");
                            if (comments)
                                theJavaChunk.print("// Unload " + qualifiedParentVariableName + variableName[i] + " into file " + qualifiedParentVariableName + otherName[i]);
                            if ((hasFiles || hasChildFiles) && (type == IS_A_FUNCTION || forceUseByte)) {
                                theJavaChunk.print(isBrokenString + "                                     ");
                                theJavaChunk.print(isBrokenString + "if (! useByteArraysForLongsAndLOBS)    ");
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{                                      ");
                            }
                            theJavaChunk.print(isBrokenString + "if (" + qualifiedParentVariableName + otherName[i] + " == null)    ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{                                      ");
                            theJavaChunk.print(isBrokenString + "try                                    ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{                                      ");
                            theJavaChunk.print(isBrokenString + "" + qualifiedParentVariableName + variableName[i] + " = java.io.File.createTempFile(" + stempFilePrefix + "," + stempFileSuffix + "," + stempFileDir + ");   ");
                            theJavaChunk.print(isBrokenString + "}                                                                             ");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "catch (java.io.IOException e)     ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{                                 ");
                            theJavaChunk.print(isBrokenString + "throw new CSException(\"Unable to create temp file in \" + " + stempFileDir + ".getAbsolutePath());   ");
                            theJavaChunk.print(isBrokenString + "}       ");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "}       ");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "else    ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{    ");
                            theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + " = new java.io.File(" + qualifiedParentVariableName + otherName[i] + ");    ");
                            theJavaChunk.print(isBrokenString + "                                          ");
                            if (comments)
                                theJavaChunk.print(isBrokenString + "// If filenameOutParam turns out to be a relative path name move it to " + stempFileDir + "...");
                            if (comments)
                                theJavaChunk.print(isBrokenString + "// Otherwise it will appear relative to the current working directory");
                            theJavaChunk.print(isBrokenString + "if (! " + qualifiedParentVariableName + variableName[i] + ".isAbsolute())");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{");
                            theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + " = new java.io.File(" + stempFileDir + "," + qualifiedParentVariableName + otherName[i] + ");");
                            theJavaChunk.print(isBrokenString + "}");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "                                          ");
                            theJavaChunk.print(isBrokenString + "if (" + qualifiedParentVariableName + variableName[i] + ".exists()  && ! " + qualifiedParentVariableName + variableName[i] + ".canWrite())    ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{                                   ");
                            theJavaChunk.print(isBrokenString + "throw new CSException(\"Can not write to file \" + " + qualifiedParentVariableName + variableName[i] + ".getAbsolutePath() + \" - not writable\"); ");
                            theJavaChunk.print(isBrokenString + "}                                      ");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "                                    ");
                            theJavaChunk.print(isBrokenString + "if (" + qualifiedParentVariableName + variableName[i] + ".isDirectory())   ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{    ");
                            theJavaChunk.print(isBrokenString + "throw new CSException(\"Can not write to file \" + " + qualifiedParentVariableName + variableName[i] + ".getAbsolutePath() + \" - is a Directory\"); ");
                            theJavaChunk.print(isBrokenString + "} ");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "  ");
                            theJavaChunk.print(isBrokenString + "} ");
                            theJavaChunk.print(isBrokenString + "  ");
                            theJavaChunk.unIndent();

                            if (debugMessages) {
                                theJavaChunk.print(isBrokenString);
                                theJavaChunk.print(isBrokenString + stheLog + ".debug(\"Unloading parameter " + qualifiedParentVariableName + variableName[i] + " to file \" + " + qualifiedParentVariableName + variableName[i] + ".getAbsolutePath() );");
                                theJavaChunk.print(isBrokenString);
                            }
                            if ((hasFiles || hasChildFiles) && (type == IS_A_FUNCTION || forceUseByte)) {
                                theJavaChunk.print(isBrokenString + "} ");
                                theJavaChunk.unIndent();
                                if (debugMessages) {
                                    theJavaChunk.print(isBrokenString + "else");
                                    theJavaChunk.indent();
                                    theJavaChunk.print(isBrokenString + "{    ");
                                    theJavaChunk.print(isBrokenString + stheLog + ".debug(\"Unloading parameter " + qualifiedParentVariableName + variableName[i] + " to array \" );");
                                    theJavaChunk.print(isBrokenString + "} ");
                                    theJavaChunk.unIndent();
                                }
                            }

                            if (actualOracleDatatype[i].equals("LONG")
                                    || actualOracleDatatype[i].equals("LONG RAW")
                            ) {
                                theJavaChunk.print("");
                                if (comments)
                                    theJavaChunk.print(isBrokenString + "// LONG columns can come back as either a String or an Inputstream");
                                theJavaChunk.print(isBrokenString + "try  ");
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{ ");
                                theJavaChunk.print(isBrokenString + "Object " + /*qualifiedParentVariableName + */variableName[i] + "Object = theParameters.getParam(" + (paramOutId[i] + outOffSet) + ");");
                                theJavaChunk.print(isBrokenString + "if (" + /*qualifiedParentVariableName + */ variableName[i] + "Object == null)");
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{");
                                if (comments)
                                    theJavaChunk.print(isBrokenString + "// Do nothing - there is nothing to unload...");
                                //theJavaChunk.print(isBrokenString + "" + qualifiedParentVariableName + variableName[i] + " = null;");
                                theJavaChunk.print(isBrokenString + "}");
                                theJavaChunk.unIndent();
                                theJavaChunk.print(isBrokenString + "else if (" + /*qualifiedParentVariableName + */variableName[i] + "Object instanceof String)");
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{");
                                // A LONG can come back as a String; in useByteArrays mode there is no
                                // output File to unload into (it stays null), so take the String's bytes
                                // directly. Mirrors the InputStream branch below. The else keeps the
                                // original File-unload behaviour byte-for-byte when useByteArrays is off.
                                theJavaChunk.print(isBrokenString + "if (useByteArraysForLongsAndLOBS)");
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{");
                                theJavaChunk.print(isBrokenString + "" + qualifiedParentVariableName + byteName[i] + " = ((String)theParameters.getParam(" + (paramOutId[i] + outOffSet) + ")).getBytes();");
                                theJavaChunk.print(isBrokenString + "}");
                                theJavaChunk.print(isBrokenString + "else");
                                theJavaChunk.print(isBrokenString + "{");
                                theJavaChunk.print(isBrokenString + "" + qualifiedParentVariableName + variableName[i] + " = " + sobjectLoader + ".unloadBinaryStream(" + qualifiedParentVariableName + variableName[i] + ",(String)theParameters.getParam(" + (paramOutId[i] + outOffSet) + "),\"LONG\");");
                                theJavaChunk.print(isBrokenString + "}");
                                theJavaChunk.unIndent();
                                theJavaChunk.print(isBrokenString + "}");
                                theJavaChunk.unIndent();
                                theJavaChunk.print(isBrokenString + "else if (" + /*qualifiedParentVariableName + */ variableName[i] + "Object instanceof java.io.InputStream)");
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{");
                                theJavaChunk.print(isBrokenString + "if (useByteArraysForLongsAndLOBS)");
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{");
                                theJavaChunk.print(isBrokenString + "" + qualifiedParentVariableName + byteName[i] + " = " + sobjectLoader + ".unloadBinaryStreamIntoByteArray((java.io.InputStream)theParameters.getParam(" + (paramOutId[i] + outOffSet) + "),\"LONG\");");
                                theJavaChunk.print(isBrokenString + "}");
                                theJavaChunk.print(isBrokenString + "else");
                                theJavaChunk.print(isBrokenString + "{");
                                theJavaChunk.print(isBrokenString + "" + qualifiedParentVariableName + variableName[i] + " = " + sobjectLoader + ".unloadBinaryStream(" + qualifiedParentVariableName + variableName[i] + ",(java.io.InputStream)theParameters.getParam(" + (paramOutId[i] + outOffSet) + "),\"LONG\");");
                                theJavaChunk.print(isBrokenString + "}");
                                theJavaChunk.unIndent();
                                theJavaChunk.print(isBrokenString + "}");
                                theJavaChunk.unIndent();
                                theJavaChunk.print(isBrokenString + "else");
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{");
                                theJavaChunk.print(isBrokenString + "throw (new CSException(\"" + qualifiedParentVariableName + variableName[i] + ": Unable to turn \" + " + variableName[i] + "Object.getClass().getName() + \" into a file\"));");
                                theJavaChunk.print(isBrokenString + "}");
                                theJavaChunk.unIndent();
                                theJavaChunk.print(isBrokenString + "}                        ");
                                theJavaChunk.unIndent();
                                theJavaChunk.print(isBrokenString + "catch (Exception e)   ");
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{                                 ");
                                theJavaChunk.print(isBrokenString + "throw (new CSException(" + qualifiedParentVariableName + variableName[i] + " + \" could not be unloaded:\" + e.toString()));  ");
                                theJavaChunk.print(isBrokenString + "}    ");
                                theJavaChunk.unIndent();
                                theJavaChunk.print(isBrokenString + "");
                            } else if (actualOracleDatatype[i].equals("CLOB")) {
                                theJavaChunk.print("");
                                theJavaChunk.print(isBrokenString + "try  ");
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{ ");
                                theJavaChunk.print(isBrokenString + qualifiedParentVariableName + lobName[i] + " =  (oracle.sql." + actualOracleDatatype[i] + ")theParameters.getParam(" + (paramOutId[i] + outOffSet) + ");    ");
                                theJavaChunk.print(isBrokenString + "if (" + qualifiedParentVariableName + lobName[i] + " != null && useByteArraysForLongsAndLOBS)");     //DRKLUGE
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{ ");
                                if (useCharForCLOB) {
                                    theJavaChunk.print(isBrokenString + "" + qualifiedParentVariableName + byteName[i] + " = " + sobjectLoader + ".unloadClobIntoCharArray(" + qualifiedParentVariableName + lobName[i] + ");");
                                } else {
                                    theJavaChunk.print(isBrokenString + "" + qualifiedParentVariableName + byteName[i] + " = " + sobjectLoader + ".unloadBinaryStreamIntoByteArray(" + qualifiedParentVariableName + lobName[i] + ".getAsciiStream(),\"" + actualOracleDatatype[i] + "\");   ");
                                }

                                theJavaChunk.print(isBrokenString + "}");
                                theJavaChunk.unIndent();
                                theJavaChunk.print(isBrokenString + "else if (" + qualifiedParentVariableName + lobName[i] + " != null && (! " + skeepLobs + "))");     //DRKLUGE
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{ ");
                                if (useCharForCLOB) {
                                    theJavaChunk.print(isBrokenString + "" + qualifiedParentVariableName + variableName[i] + " = " + sobjectLoader + ".unloadClob(" + qualifiedParentVariableName + variableName[i] + ", " + qualifiedParentVariableName + lobName[i] + ");   ");
                                } else {
                                    theJavaChunk.print(isBrokenString + "" + qualifiedParentVariableName + variableName[i] + " = " + sobjectLoader + ".unloadBinaryStream(" + qualifiedParentVariableName + variableName[i] + "," + qualifiedParentVariableName + lobName[i] + ".getAsciiStream(),\"" + actualOracleDatatype[i] + "\");   ");
                                }
                                theJavaChunk.print(isBrokenString + "}");
                                theJavaChunk.unIndent();
                                theJavaChunk.print(isBrokenString + "}                        ");
                                theJavaChunk.unIndent();
                                theJavaChunk.print(isBrokenString + "catch (Exception e)   ");
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{                                 ");
                                theJavaChunk.print(isBrokenString + "throw (new CSException(" + qualifiedParentVariableName + variableName[i] + " + \" could not be unloaded:\" + e.toString()));  ");
                                theJavaChunk.print(isBrokenString + "}    ");
                                theJavaChunk.unIndent();
                            } else if (actualOracleDatatype[i].equals("XMLTYPE")) {
                                theJavaChunk.print("");
                                theJavaChunk.print(isBrokenString + "try  ");
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{ ");
                                theJavaChunk.print(isBrokenString + "oracle.xdb.XMLType tempXmlType = (oracle.xdb.XMLType)theParameters.getParam(" + (paramOutId[i] + outOffSet) + ");    ");

                                //theJavaChunk.print(isBrokenString + "try  ");
                                //theJavaChunk.indent();
                                //theJavaChunk.print(isBrokenString + "{ ");

                                theJavaChunk.print(isBrokenString + "if (tempXmlType != null)");
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{ ");


                                //theJavaChunk.print(isBrokenString + "if (" + qualifiedParentVariableName + lobName[i] + " != null )");     //DRKLUGE
                                //theJavaChunk.indent();
                                //theJavaChunk.print(isBrokenString + "{ ");

                                theJavaChunk.print(isBrokenString + qualifiedParentVariableName + lobName[i] + " =  (oracle.sql.OPAQUE)tempXmlType; ");
                                //theJavaChunk.print(isBrokenString + "java.io.InputStream s = new java.io.ByteArrayInputStream(tempXmlType.getBytesValue());");
                                theJavaChunk.print(isBrokenString + "if (useByteArraysForLongsAndLOBS)");     //DRKLUGE
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{ ");
                                //theJavaChunk.print(isBrokenString + qualifiedParentVariableName +byteName[i] + " = tempXmlType.getStringVal().getBytes();");
                                theJavaChunk.print(isBrokenString + "oracle.sql.CLOB tempClob = tempXmlType.getClobVal();");
                                if (useCharForCLOB) {
                                    theJavaChunk.print(isBrokenString + "" + qualifiedParentVariableName + byteName[i] + " = " + sobjectLoader + ".unloadClobIntoCharArray(tempClob);   ");
                                } else {
                                    theJavaChunk.print(isBrokenString + "" + qualifiedParentVariableName + byteName[i] + " = " + sobjectLoader + ".unloadClobIntoByteArray(tempClob);   ");
                                }
                                //theJavaChunk.print(isBrokenString + "" + qualifiedParentVariableName +byteName[i] + " = " + sobjectLoader + ".unloadBinaryStreamIntoByteArray(s,\"" + actualOracleDatatype[i] + "\");   ");
                                if (comments)
                                    theJavaChunk.print("// The Javadoc for XMLType says it *must* be closed after use to avoid memory leaks..");
                                theJavaChunk.print("tempXmlType.close(); ");
                                theJavaChunk.print(isBrokenString + qualifiedParentVariableName + lobName[i] + " =  null; ");
                                theJavaChunk.print(isBrokenString + "}");
                                theJavaChunk.unIndent();
                                theJavaChunk.print(isBrokenString + "else if (! " + skeepLobs + ")");     //DRKLUGE
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{ ");
                                theJavaChunk.print(isBrokenString + "oracle.sql.CLOB tempClob = tempXmlType.getClobVal();");
                                theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + " = " + sobjectLoader + ".unloadClob(" + qualifiedParentVariableName + variableName[i] + ",tempClob);");
                                //theJavaChunk.print(isBrokenString + "");
                                //theJavaChunk.print(isBrokenString +  qualifiedParentVariableName + variableName[i] + " = com.mcpdbwizard.pub.IOUtils.loadStringIntoFile(tempXmlType.getStringVal(), " +qualifiedParentVariableName + variableName[i]+ ", "+stheLog+");");
                                //theJavaChunk.print(isBrokenString + "" + qualifiedParentVariableName + variableName[i] + " = " + sobjectLoader + ".unloadBinaryStream(" + qualifiedParentVariableName + variableName[i] + ", s, \"" + actualOracleDatatype[i] + "\");   ");
                                if (comments)
                                    theJavaChunk.print("// The Javadoc for XMLType says it *must* be closed after use to avoid memory leaks..");
                                theJavaChunk.print("tempXmlType.close(); ");
                                theJavaChunk.print(isBrokenString + qualifiedParentVariableName + lobName[i] + " =  null; ");
                                theJavaChunk.print(isBrokenString + "}");
                                theJavaChunk.unIndent();
                                theJavaChunk.print(isBrokenString + "}                        ");
                                theJavaChunk.unIndent();
                                theJavaChunk.print(isBrokenString + "else");
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{ ");
                                theJavaChunk.print(isBrokenString + qualifiedParentVariableName + lobName[i] + " = null;");
                                theJavaChunk.print(isBrokenString + "}                        ");
                                theJavaChunk.unIndent();

                                //theJavaChunk.print(isBrokenString + "}                        ");
                                //theJavaChunk.unIndent();


                                theJavaChunk.print(isBrokenString + "}                        ");
                                theJavaChunk.unIndent();
                                theJavaChunk.print(isBrokenString + "catch (java.sql.SQLException e)   ");
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{                                 ");
                                theJavaChunk.print(isBrokenString + "throw (new CSException(\"" + qualifiedParentVariableName + variableName[i] + " + could not be unloaded:\" + e.toString()));  ");
                                theJavaChunk.print(isBrokenString + "}    ");
                                theJavaChunk.unIndent();
                            } else if (actualOracleDatatype[i].equals("BLOB")) {
                                theJavaChunk.print("");
                                theJavaChunk.print(isBrokenString + "try  ");
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{ ");
                                theJavaChunk.print(isBrokenString + qualifiedParentVariableName + lobName[i] + " =  (oracle.sql." + actualOracleDatatype[i] + ")theParameters.getParam(" + (paramOutId[i] + outOffSet) + ");    ");
                                theJavaChunk.print(isBrokenString + "if (" + qualifiedParentVariableName + lobName[i] + " != null && useByteArraysForLongsAndLOBS)");     //DRKLUGE
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{ ");
                                theJavaChunk.print(isBrokenString + "" + qualifiedParentVariableName + byteName[i] + " = " + sobjectLoader + ".unloadBinaryStreamIntoByteArray(" + qualifiedParentVariableName + lobName[i] + ".getBinaryStream(),\"" + actualOracleDatatype[i] + "\");   ");
                                theJavaChunk.print(isBrokenString + "}");
                                theJavaChunk.unIndent();
                                theJavaChunk.print(isBrokenString + "else if (" + qualifiedParentVariableName + lobName[i] + " != null && (! " + skeepLobs + "))");
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{ ");
                                theJavaChunk.print(isBrokenString + "" + qualifiedParentVariableName + variableName[i] + " = " + sobjectLoader + ".unloadBinaryStream(" + qualifiedParentVariableName + variableName[i] + "," + qualifiedParentVariableName + lobName[i] + ".getBinaryStream(),\"" + actualOracleDatatype[i] + "\");   ");
                                theJavaChunk.print(isBrokenString + "}");
                                theJavaChunk.unIndent();
                                theJavaChunk.print(isBrokenString + "}                        ");
                                theJavaChunk.unIndent();
                                theJavaChunk.print(isBrokenString + "catch (java.sql.SQLException e)   ");
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{                                 ");
                                theJavaChunk.print(isBrokenString + "throw (new CSException(" + qualifiedParentVariableName + variableName[i] + " + \" could not be unloaded:\" + e.toString()));  ");
                                theJavaChunk.print(isBrokenString + "}    ");
                                theJavaChunk.unIndent();
                            } else if (actualOracleDatatype[i].equals("BFILE")) {
                                theJavaChunk.print("");
                                theJavaChunk.print(isBrokenString + qualifiedParentVariableName + lobName[i] + " =  (oracle.sql." + actualOracleDatatype[i] + ")theParameters.getParam(" + (paramOutId[i] + outOffSet) + ");    ");
                                theJavaChunk.print(isBrokenString + "if (" + qualifiedParentVariableName + lobName[i] + " != null && useByteArraysForLongsAndLOBS)");     //DRKLUGE
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{ ");
                                theJavaChunk.print(isBrokenString + "" + qualifiedParentVariableName + byteName[i] + " = " + sobjectLoader + ".unloadBfileIntoByteArray(" + qualifiedParentVariableName + lobName[i] + ");   ");
                                theJavaChunk.print(isBrokenString + "}");
                                theJavaChunk.unIndent();
                                theJavaChunk.print(isBrokenString + "else if (" + qualifiedParentVariableName + lobName[i] + " != null && (! " + skeepLobs + "))");
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{ ");
                                theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + " = " + sobjectLoader + ".unloadBfile(" + qualifiedParentVariableName + variableName[i] + "," + qualifiedParentVariableName + lobName[i] + ");   ");
                                theJavaChunk.print(isBrokenString + "}");
                                theJavaChunk.unIndent();
                            } else {
                                // Who uses this?
                                theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + " = " + sobjectLoader + ".unloadBinaryStream("
                                        + qualifiedParentVariableName + variableName[i] + ",(java.io.InputStream)theParameters.getParam(" + (paramOutId[i] + outOffSet) + "),\"" + actualOracleDatatype[i] + "\");");
                            }

                            break;
                        }
                        case SqlUtils.MCPDBWIZARD_READONLYROWSET: {
                            theJavaChunk.print("");

                            if (debugMessages) {
                                theJavaChunk.print(isBrokenString + stheLog + ".debug(\"Unloading parameter " + qualifiedParentVariableName + variableName[i] + "\" );");
                            }


                            theJavaChunk.print(isBrokenString + "try");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{");
                            if ((hasFiles || hasChildFiles) && type == IS_A_FUNCTION) {
                                theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i]
                                        + " = new com.mcpdbwizard.pub.ReadOnlyRowSet((ResultSet)"
                                        + "theParameters.getParam(" + (paramOutId[i] + outOffSet) + "), " + sGetProcCallStatement + "()," + smaxRows + ", " + stheLog + "," + stempFileDir + "," + skeepFiles + "," + stempFilePrefix + "," + stempFileSuffix + "," + skeepLobs + ", useByteArraysForLongsAndLOBS);");
                            } else {
                                theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i]
                                        + " = new com.mcpdbwizard.pub.ReadOnlyRowSet((ResultSet)"
                                        //DRKLUGE ?reachable
                                        + "theParameters.getParam(" + (paramOutId[i] + outOffSet) + "), " + sGetProcCallStatement + "()," + smaxRows + ", " + stheLog + "," + stempFileDir + "," + skeepFiles + "," + stempFilePrefix + "," + stempFileSuffix + "," + skeepLobs + ");");
                            }
                            theJavaChunk.print(isBrokenString + "}");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "catch (com.mcpdbwizard.pub.CSDBNullObjectException e)");
                            theJavaChunk.indent();
                            if (comments) {
                                theJavaChunk.print("// A CSDBNullObjectException here  means that a stored procedure or function");
                                theJavaChunk.print("// has returned a null Ref Cursor");
                            }
                            theJavaChunk.print(isBrokenString + "{");
                            theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + " = null;");
                            if (debugMessages) {
                                theJavaChunk.print(isBrokenString + stheLog + ".debug(\"Ref Cursor for " + qualifiedParentVariableName + variableName[i] + " is null. No ReadOnlyRowSet created.\" );");
                            }
                            theJavaChunk.print(isBrokenString + "}");
                            theJavaChunk.unIndent();

                            break;
                        }
                        case SqlUtils.ORACLE_BOOLEAN_DATATYPE: {
                            theJavaChunk.print("");

                            if (debugMessages) {
                                theJavaChunk.print(isBrokenString + stheLog + ".debug(\"Unloading parameter " + qualifiedParentVariableName + variableName[i] + "\" );");
                            }

                            if (comments) {
                                theJavaChunk.print("// Booleans come back from the DB as int and have to be converted");
                            }

                            theJavaChunk.print(isBrokenString + "if (   theParameters.getParam(" + (paramOutId[i] + outOffSet) + ") == null ");
                            theJavaChunk.print(isBrokenString + "    || ((java.math.BigDecimal)theParameters.getParam(" + (paramOutId[i] + outOffSet) + ")).intValue() == 0)");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{");
                            theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i]
                                    + " = null;");
                            theJavaChunk.print(isBrokenString + "}");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "else if (((java.math.BigDecimal)theParameters.getParam(" + (paramOutId[i] + outOffSet) + ")).intValue() == -1)");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{");
                            theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i]
                                    + " = Boolean.valueOf(false);");
                            theJavaChunk.print(isBrokenString + "}");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "else ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{");
                            theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i]
                                    + " = Boolean.valueOf(true);");
                            theJavaChunk.print(isBrokenString + "}");
                            theJavaChunk.unIndent();

                            break;
                        }
                        case SqlUtils.ORACLE_OTHER_DATATYPE:
                        case SqlUtils.ORACLE_ROWTYPE_DATATYPE:
                        case SqlUtils.ORACLE_OBJECT_DATATYPE: {
                            if (!variableDataType[i].equals("Object")) {
                                if (theRecords[extraObjectId[i]].usable) {
                                    // Item 7 (nested records): recurse into the nested record on the OUT
                                    // read with the FULL path (qualifiedParentVariableName + variableName[i])
                                    // and ACCUMULATED offsets, mirroring addBindCode. The old "Multiple
                                    // levels of recursion are not supported" throw that guarded here is
                                    // removed now that the path + offsets are threaded correctly. (The
                                    // weHaveRecursed flag still gates the top-level-only demo /
                                    // clearParameters code, which is correct.) Byte-identical for a
                                    // single-level record param (offsets are 0 and the path has no dots).
                                    theRecords[extraObjectId[i]].theEngine.addGetResultsCode(theJavaChunk, comments, debugMessages, stats, (inOffSet + paramInId[i] - 1), (outOffSet + paramOutId[i] - 1), qualifiedParentVariableName + variableName[i], variableDataType[i], true);
                                } else {
                                    theJavaChunk.print("// Error: parameter " + qualifiedParentVariableName + variableName[i] + " can not be set");
                                    theLog.error("Error: parameter " + qualifiedParentVariableName + variableName[i] + " can not be set");
                                }
                            }
                            break;
                        }
                        case SqlUtils.ORACLE_TEXT_DATATYPE: {
                            theJavaChunk.print("");

                            if (debugMessages) {
                                theJavaChunk.print(isBrokenString + stheLog + ".debug(\"Unloading parameter " + qualifiedParentVariableName + variableName[i] + "\" );");
                            }

                            theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i]
                                    + " =  (" + variableDataType[i]
                                    + ")theParameters.getParam(" + (paramOutId[i] + outOffSet) + ");");
                            // FIX for CHAR
                            if (actualOracleDatatype[i].equals("CHAR")
                                    || actualOracleDatatype[i].equals("CHARACTER")
                                    || actualOracleDatatype[i].equals("NCHAR")
                                    || actualOracleDatatype[i].equals("NCHARACTER")) {
                                if (comments) {
                                    theJavaChunk.print("// CHAR, CHARACTER, NCHAR and NCHARACTER columns tend to come");
                                    theJavaChunk.print("// back with large numbers of trailing spaces.");
                                }

                                theJavaChunk.print(isBrokenString + "if (" + qualifiedParentVariableName + variableName[i] + " != null)");
                                theJavaChunk.indent();
                                theJavaChunk.print(isBrokenString + "{");
                                theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + " = " + qualifiedParentVariableName + variableName[i] + ".trim();");
                                theJavaChunk.print(isBrokenString + "}");
                                theJavaChunk.unIndent();
                            }
                            break;
                        }
                        case SqlUtils.ORACLE_TIMESTAMP_DATATYPE: {
                            theJavaChunk.print("");

                            if (debugMessages) {
                                theJavaChunk.print(isBrokenString + stheLog + ".debug(\"Unloading parameter " + qualifiedParentVariableName + variableName[i] + "\" );");
                            }

                            theJavaChunk.print(isBrokenString + qualifiedParentVariableName + JavaUtils.getJavaName("set_" + variableName[i], javaNamingConvention, theLog)
                                    + "((" + "java.sql.Timestamp" //"oracle.sql.TIMESTAMP"
                                    + ")theParameters.getParam(" + (paramOutId[i] + outOffSet) + "));");

                            break;
                        }
                        case SqlUtils.ORACLE_TIMESTAMPTZ_DATATYPE: {
                            theJavaChunk.print("");

                            if (debugMessages) {
                                theJavaChunk.print(isBrokenString + stheLog + ".debug(\"Unloading parameter " + qualifiedParentVariableName + variableName[i] + "\" );");
                            }

                            theJavaChunk.print(isBrokenString + qualifiedParentVariableName + JavaUtils.getJavaName("set_" + variableName[i], javaNamingConvention, theLog)
                                    + "((" + "oracle.sql.TIMESTAMPTZ"
                                    + ")theParameters.getParam(" + (paramOutId[i] + outOffSet) + "));");

                            break;
                        }
                        case SqlUtils.ORACLE_TIMESTAMPLTZ_DATATYPE: {
                            theJavaChunk.print("");

                            if (debugMessages) {
                                theJavaChunk.print(isBrokenString + stheLog + ".debug(\"Unloading parameter " + qualifiedParentVariableName + variableName[i] + "\" );");
                            }

                            theJavaChunk.print(isBrokenString + qualifiedParentVariableName + JavaUtils.getJavaName("set_" + variableName[i], javaNamingConvention, theLog)
                                    + "((" + "oracle.sql.TIMESTAMPLTZ"
                                    + ")theParameters.getParam(" + (paramOutId[i] + outOffSet) + "));");


                            break;
                        }
                        case SqlUtils.ORACLE_INTERVAL_DAY_TO_SECOND_DATATYPE: {
                            theJavaChunk.print("");

                            if (debugMessages) {
                                theJavaChunk.print(isBrokenString + stheLog + ".debug(\"Unloading parameter " + qualifiedParentVariableName + variableName[i] + "\" );");
                            }

                            theJavaChunk.print(isBrokenString + qualifiedParentVariableName + JavaUtils.getJavaName("set_" + variableName[i], javaNamingConvention, theLog)
                                    + "((" + "oracle.sql.INTERVALDS"
                                    + ")theParameters.getParam(" + (paramOutId[i] + outOffSet) + "));");

                            break;
                        }
                        case SqlUtils.ORACLE_INTERVAL_YEAR_TO_MONTH_DATATYPE: {
                            theJavaChunk.print("");

                            if (debugMessages) {
                                theJavaChunk.print(isBrokenString + stheLog + ".debug(\"Unloading parameter " + qualifiedParentVariableName + variableName[i] + "\" );");
                            }

                            theJavaChunk.print(isBrokenString + qualifiedParentVariableName + JavaUtils.getJavaName("set_" + variableName[i], javaNamingConvention, theLog)
                                    + "((" + "oracle.sql.INTERVALYM"
                                    + ")theParameters.getParam(" + (paramOutId[i] + outOffSet) + "));");


                            break;
                        }
                        case SqlUtils.ORACLE_SDO_GEOMETRY_DATATYPE: {
                            theJavaChunk.print("");

                            if (debugMessages) {
                                theJavaChunk.print(isBrokenString + stheLog + ".debug(\"Unloading parameter " + qualifiedParentVariableName + variableName[i] + "\" );");
                            }
                            theJavaChunk.print("");
                            theJavaChunk.print(isBrokenString + "try  ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{ ");

                            // paramPInout =  oracle.spatial.geometry.JGeometry.load((oracle.sql.STRUCT)theParameters.getParam(2));

                            if (comments)
                                theJavaChunk.print(isBrokenString + "// JGeometry constructor doesn't like NULL...");
                            theJavaChunk.print(isBrokenString + "oracle.sql.STRUCT tempStruct = (oracle.sql.STRUCT)theParameters.getParam(" + (paramOutId[i] + outOffSet) + ");");

                            theJavaChunk.print(isBrokenString + "if (tempStruct != null)  ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{ ");

                            theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i]
                                    + " =  JGeometryWrapper.createWrappedClass(oracle.spatial.geometry.JGeometry.load(tempStruct));");

                            theJavaChunk.print(isBrokenString + "}                        ");
                            theJavaChunk.unIndent();

                            theJavaChunk.print(isBrokenString + "else");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{ ");

                            theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + " =  null;");

                            theJavaChunk.print(isBrokenString + "}                        ");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "}                        ");
                            theJavaChunk.unIndent();

                            theJavaChunk.print(isBrokenString + "catch (java.sql.SQLException e)   ");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{                                 ");
                            theJavaChunk.print(isBrokenString + "throw (new CSException(\"oracle.spatial.geometry.JGeometry " + qualifiedParentVariableName + variableName[i] + " + could not be unloaded:\" + e.toString()));  ");
                            theJavaChunk.print(isBrokenString + "}    ");
                            theJavaChunk.unIndent();


                            break;
                        }
                        case SqlUtils.ORACLE_TABLE_DATATYPE: {
                            if (targetVersion.startsWith("DB2")) {
                                theJavaChunk.print("");
                                if (comments)
                                    theJavaChunk.print("// Unload " + qualifiedParentVariableName + variableName[i]);

                                if (debugMessages) {
                                    theJavaChunk.print(isBrokenString + stheLog + ".debug(\"Unloading parameter " + qualifiedParentVariableName + variableName[i] + "\" );");
                                }

                                theJavaChunk.print("java.sql.Array tempArray = (java.sql.Array)theParameters.getParam(" + (paramOutId[i] + outOffSet) + ");");
                                theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i]
                                        + " = new " + variableDataType[i]
                                        + "(" + stheLog + ");");
                                theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i]
                                        + ".setArray(tempArray);");
                            } else {
                                theJavaChunk.print("");
                                if (comments)
                                    theJavaChunk.print("// Unload " + qualifiedParentVariableName + variableName[i]);

                                if (debugMessages) {
                                    theJavaChunk.print(isBrokenString + stheLog + ".debug(\"Unloading parameter " + qualifiedParentVariableName + variableName[i] + "\" );");
                                }

                                theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i]
                                        + " =  (" + variableDataType[i]
                                        + ")theParameters.getParam(" + (paramOutId[i] + outOffSet) + ");");
                            }
                            break;
                        }
                        case SqlUtils.ORACLE_VECTOR_DATATYPE: {
                            if (!variableDataType[i].equals("double[]")) {
                                // Non-scalar row mis-classified as VECTOR (see the setter arm):
                                // behave exactly like the default arm.
                                theJavaChunk.print("");
                                if (comments)
                                    theJavaChunk.print("// Unload " + qualifiedParentVariableName + variableName[i]);
                                if (debugMessages) {
                                    theJavaChunk.print(isBrokenString + stheLog + ".debug(\"Unloading parameter " + qualifiedParentVariableName + variableName[i] + "\" );");
                                }
                                theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i]
                                        + " =  (" + variableDataType[i]
                                        + ")theParameters.getParam(" + (paramOutId[i] + outOffSet) + ");");
                                break;
                            }
                            theJavaChunk.print("");
                            if (comments) {
                                theJavaChunk.print("// Unload " + qualifiedParentVariableName + variableName[i]
                                        + " - a format-flexible VECTOR comes back as double[] (dense), byte[] (binary) or SparseVector (sparse).");
                            }

                            if (debugMessages) {
                                theJavaChunk.print(isBrokenString + stheLog + ".debug(\"Unloading parameter " + qualifiedParentVariableName + variableName[i] + "\" );");
                            }

                            theJavaChunk.print(isBrokenString + "Object " + variableName[i] + "VectorObject = theParameters.getParam(" + (paramOutId[i] + outOffSet) + ");");
                            theJavaChunk.print(isBrokenString + "if (" + variableName[i] + "VectorObject instanceof com.mcpdbwizard.pub.SparseVector)");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{");
                            theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + "VectorSparse = (com.mcpdbwizard.pub.SparseVector) " + variableName[i] + "VectorObject;");
                            theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + " = null;");
                            theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + "VectorBinary = null;");
                            theJavaChunk.print(isBrokenString + "}");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "else if (" + variableName[i] + "VectorObject instanceof byte[])");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{");
                            theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + "VectorBinary = (byte[]) " + variableName[i] + "VectorObject;");
                            theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + " = null;");
                            theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + "VectorSparse = null;");
                            theJavaChunk.print(isBrokenString + "}");
                            theJavaChunk.unIndent();
                            theJavaChunk.print(isBrokenString + "else");
                            theJavaChunk.indent();
                            theJavaChunk.print(isBrokenString + "{");
                            theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + " = (double[]) " + variableName[i] + "VectorObject;");
                            theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + "VectorBinary = null;");
                            theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i] + "VectorSparse = null;");
                            theJavaChunk.print(isBrokenString + "}");
                            theJavaChunk.unIndent();
                            break;
                        }
                        default: {
                            theJavaChunk.print("");
                            if (comments)
                                theJavaChunk.print("// Unload " + qualifiedParentVariableName + variableName[i]);

                            if (debugMessages) {
                                theJavaChunk.print(isBrokenString + stheLog + ".debug(\"Unloading parameter " + qualifiedParentVariableName + variableName[i] + "\" );");
                            }

                            theJavaChunk.print(isBrokenString + qualifiedParentVariableName + variableName[i]
                                    + " =  (" + variableDataType[i]
                                    + ")theParameters.getParam(" + (paramOutId[i] + outOffSet) + ");");
                            break;
                        }
                    } // switch
                } // if
            } // for

            if (isBroken) {
                theJavaChunk.print("if (1 == 1) throw new com.mcpdbwizard.pub.CSUnsupportedDatatypeException(\"Unsppported data type seen\", \""
                        + theRowSet.getString("DATA_TYPE") + "\");");
            }
            theJavaChunk.print(" ");


            if ((!targetVersion.startsWith("8")) && hasFiles && type == IS_A_FUNCTION) {
                // First: See if thrre are any BLOBS or CLOBS. 'hasFiles' is set for BFILES as well...
                int foundClobOrBlob = 0;

                for (int i = 0; i < theRowSet.size(); i++) {
                    theRowSet.setCurrentRowNumber(i);
                    if (theRowSet.getString("IN_OUT").equals("IN")
                            || theRowSet.getString("IN_OUT").equals("IN/OUT")
                            || overRideInOut.equals("IN/OUT") //  this will break *something* - appraently it doesnt
                    ) {

                        switch (oracleUnderlyingDatatype[i]) {
                            case SqlUtils.ORACLE_CLOB_DATATYPE:
                            case SqlUtils.ORACLE_BLOB_DATATYPE: {
                                foundClobOrBlob++;
                                break;
                            }
                        }
                    } //if
                } //for

                if (foundClobOrBlob > 0) {

                    theJavaChunk.print("");

                    if (comments) theJavaChunk.print("// Clear temporary LOBS if appropriate ");
                    theJavaChunk.print(isBrokenString + "if (deleteTempLobsAfterCall)    ");
                    theJavaChunk.print(isBrokenString + "  {");
                    theJavaChunk.print(isBrokenString + "  try");
                    theJavaChunk.print(isBrokenString + "    {");

                    for (int i = 0; i < theRowSet.size(); i++) {
                        theRowSet.setCurrentRowNumber(i);
                        if (theRowSet.getString("IN_OUT").equals("IN")
                                || theRowSet.getString("IN_OUT").equals("IN/OUT")
                                || overRideInOut.equals("IN/OUT") //  this will break *something* - appraently it doesnt
                        ) {
                            switch (oracleUnderlyingDatatype[i]) {
                                case SqlUtils.ORACLE_CLOB_DATATYPE:
                                case SqlUtils.ORACLE_BLOB_DATATYPE:
                                    //case SqlUtils.ORACLE_BFILE_DATATYPE:
                                {
                                    theJavaChunk.print(isBrokenString + "    if (" + qualifiedParentVariableName + lobName[i] + ".isTemporary())    ");
                                    theJavaChunk.print(isBrokenString + "      {");
                                    theJavaChunk.print(isBrokenString + "      " + qualifiedParentVariableName + lobName[i] + ".freeTemporary();");
                                    theJavaChunk.print(isBrokenString + "      }");
                                    theJavaChunk.print(isBrokenString + " ");

                                    break;
                                }
                            } // switch
                        } //if
                    } // for
                    theJavaChunk.print(isBrokenString + "    }");
                    theJavaChunk.print(isBrokenString + "  catch (SQLException e) ");
                    theJavaChunk.print(isBrokenString + "    {");
                    if (comments) {
                        theJavaChunk.print(isBrokenString + "    // Handle BUG-6668732 - Temp Lobs break on closed if not used. We now warn instead of throwing an Exception");
                    }
                    theJavaChunk.print(isBrokenString + "    " + stheLog + ".warning(\"" + sGetStatementResults + ": Unable to release temporary LOBS:\" + e.toString()); ");
                    theJavaChunk.print(isBrokenString + "    }");
                    theJavaChunk.print(isBrokenString + "  }");
                    theJavaChunk.print(isBrokenString + " ");
                } // if
            } // if
        } // try
        catch (CSNoDataInRowSetException e) {
            if (comments) theJavaChunk.print("// This procedure has no parameters and hence nothing to unload methods");
        } catch (CSException e) {
            theLog.syserror(e);
        }


        // Clear Lob params if needed

        if (!weHaveRecursed) {
            theJavaChunk.print(isBrokenString + "try ");
            theJavaChunk.print(isBrokenString + "  { ");
            theJavaChunk.print(isBrokenString + "  theCallableStatement.clearParameters(); ");
            theJavaChunk.print(isBrokenString + "  theParameters.clearParameters(); ");
            theJavaChunk.print(isBrokenString + "  } ");
            theJavaChunk.print(isBrokenString + "catch (SQLException e) ");
            theJavaChunk.print(isBrokenString + "  {");
            theJavaChunk.print(isBrokenString + "  throw (new CSException(\"" + sGetStatementResults + ": Unable to clear parameters:\" + e.toString())); ");
            theJavaChunk.print(isBrokenString + "  }");

            if (stats) {
                theJavaChunk.print(isBrokenString + "incRetrieveTime();");
            }

            if (debugMessages) {
                theJavaChunk.print(isBrokenString + stheLog + ".debug(\"Finished unloading data\");");
            }
        }
    }

    void addGetMethods(String webServiceRecType, JavaChunk theJavaChunk, JavaChunk theJavaAttrsChunk, boolean comments) {
        for (int i = 0; i < theRowSet.size(); i++) {
            String getMethod;

            try {
                theRowSet.setCurrentRowNumber(i);

                if (theRowSet.getString("IN_OUT").equals("OUT")
                        || theRowSet.getString("IN_OUT").equals("IN/OUT")
                        || overRideInOut.equals("IN/OUT")
                ) {
                    switch (oracleUnderlyingDatatype[i]) {
                        case SqlUtils.ORACLE_NUMBER_DATATYPE: {
                            final String[] other_math_object_datatypes = {"Double", "Float", "Long", "Integer", "Short", "Byte"};
                            final String[] other_math_scaler_datatypes = {"double", "float", "long", "int", "short", "byte"};

                            for (int dtype = 0; dtype < other_math_object_datatypes.length; dtype++) {
                                if (!numberDataTypes.getProperty(other_math_object_datatypes[dtype], "NONE").equals("NONE")) {
                                    getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_" + other_math_object_datatypes[dtype] + "_obj", javaNamingConvention, theLog);
                                    addObjectScalerGetMethod(theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                            , other_math_object_datatypes[dtype], JavaUtils.mathValueMethod(other_math_object_datatypes[dtype]), variableDataType[i], variableName[i], comments);
                                }
                            }


                            getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_" + "BigDecimal" + "_obj", javaNamingConvention, theLog);
                            addObjectScalerGetMethod(theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                    , "java.math.BigDecimal", JavaUtils.mathValueMethod("Double"), variableDataType[i], variableName[i], comments);

                            if (!webServiceRecType.equals("public")) {
                                getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "", javaNamingConvention, theLog);
                                addObjectScalerGetMethod(theJavaAttrsChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , "java.math.BigDecimal", JavaUtils.mathValueMethod("Double"), variableDataType[i], variableName[i], comments);
                            }

                            for (int dtype = 0; dtype < other_math_scaler_datatypes.length; dtype++) {
                                if (!numberDataTypes.getProperty(other_math_scaler_datatypes[dtype], "NONE").equals("NONE")) {
                                    getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_" + other_math_scaler_datatypes[dtype], javaNamingConvention, theLog);
                                    addScalarGetMethod(theJavaChunk, theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                            , other_math_scaler_datatypes[dtype], JavaUtils.mathValueMethod(other_math_scaler_datatypes[dtype]), variableDataType[i], variableName[i], comments);
                                    addScalarGetNVLMethod(theJavaChunk, theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                            , other_math_scaler_datatypes[dtype], JavaUtils.mathValueMethod(other_math_scaler_datatypes[dtype]), variableDataType[i], variableName[i], comments);
                                }
                            }

                            break;
                        }
                        case SqlUtils.ORACLE_DATE_DATATYPE: {
                            if (webServiceRecType.equals("public")) {
                                addDateGetMethods(theJavaChunk, theJavaChunk, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , variableName[i] + ".getTime()", variableDataType[i], variableName[i], comments, javaNamingConvention);
                            } else {
                                addDateGetMethods(theJavaChunk, theJavaAttrsChunk, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , variableName[i] + ".getTime()", variableDataType[i], variableName[i], comments, javaNamingConvention);
                            }
                            break;
                        }
                        case SqlUtils.ORACLE_PLSQL_INDEXBY_DATATYPE: {
                            getMethod = JavaUtils.getJavaName("get_" + variableName[i] /* + "_" + variableDataType[i]  + "_obj" */, javaNamingConvention, theLog);
                            if (webServiceRecType.equals("public")) {
                                addObjectScalerGetMethodNoNull(theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , "com.mcpdbwizard.pub.PlsqlIndexByTable2", "", variableDataType[i], variableName[i], comments);
                            } else {
                                addObjectScalerGetMethodNoNull(theJavaAttrsChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , "com.mcpdbwizard.pub.PlsqlIndexByTable2", "", variableDataType[i], variableName[i], comments);
                            }

                            break;
                        }
                        case SqlUtils.ORACLE_TEXT_DATATYPE: {
                            getMethod = JavaUtils.getJavaName("get_" + variableName[i] /* + "_" + variableDataType[i]  + "_obj" */, javaNamingConvention, theLog);
                            if (webServiceRecType.equals("public")) {
                                addObjectScalerGetMethod(theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , "String", "toString", variableDataType[i], variableName[i], comments);
                            } else {
                                addObjectScalerGetMethod(theJavaAttrsChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , "String", "toString", variableDataType[i], variableName[i], comments);
                            }
                            break;
                        }
                        case SqlUtils.ORACLE_ROWID_DATATYPE: {
                            getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_" + "String", javaNamingConvention, theLog);
                            if (webServiceRecType.equals("public")) {
                                addObjectScalerGetMethod(theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , "String", "getBytes", variableDataType[i], variableName[i], comments);
                            } else {
                                addObjectScalerGetMethod(theJavaAttrsChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , "String", "getBytes", variableDataType[i], variableName[i], comments);
                            }

                            getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_" + "oracle_sql_rowid", javaNamingConvention, theLog);
                            addObjectScalerGetMethod(theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                    , "oracle.sql.ROWID", "getBytes", variableDataType[i], variableName[i], comments);

                            break;
                        }
                        case SqlUtils.ORACLE_UROWID_DATATYPE: {
                            getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_" + "String", javaNamingConvention, theLog);
                            if (webServiceRecType.equals("public")) {
                                addObjectScalerGetMethod(theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , "String", "getBytes", variableDataType[i], variableName[i], comments);
                            } else {
                                addObjectScalerGetMethod(theJavaAttrsChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , "String", "getBytes", variableDataType[i], variableName[i], comments);
                            }
                            break;
                        }
                        case SqlUtils.ORACLE_BINARY_DATATYPE: {
                            //getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_" + "ByteArray", javaNamingConvention, theLog);
                            getMethod = JavaUtils.getJavaName("get_" + variableName[i], javaNamingConvention, theLog);
                            //getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_byte_array" ,javaNamingConvention, theLog);
                            if (webServiceRecType.equals("public")) {
                                addScalarGetMethod(theJavaChunk, theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , "byte[]", "", variableDataType[i], variableName[i], comments);
                            } else {
                                addScalarGetMethod(theJavaAttrsChunk, theJavaAttrsChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , "byte[]", "", variableDataType[i], variableName[i], comments);
                            }
                            break;
                        }
                        case SqlUtils.ORACLE_BOOLEAN_DATATYPE: {
                            getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_" + "Boolean" + "_obj", javaNamingConvention, theLog);
                            addObjectScalerGetMethod(theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                    , "Boolean", "booleanValue", variableDataType[i], variableName[i], comments);

                            getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_" + "Boolean", javaNamingConvention, theLog);
                            if (webServiceRecType.equals("public")) {
                                addScalarGetMethod(theJavaChunk, theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , "boolean", "booleanValue", variableDataType[i], variableName[i], comments);
                            } else {
                                addScalarGetMethod(theJavaAttrsChunk, theJavaAttrsChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , "boolean", "booleanValue", variableDataType[i], variableName[i], comments);
                            }
                            addScalarGetNVLMethod(theJavaChunk, theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                    , "boolean", "booleanValue", variableDataType[i], variableName[i], comments);
                            break;
                        }
                        case SqlUtils.ORACLE_NULL_DATATYPE: {
                            if (comments) theJavaChunk.print("// This procedure has no parameters");
                            break;
                        }
                        case SqlUtils.ORACLE_TIMESTAMP_DATATYPE: {
                            getMethod = JavaUtils.getJavaName("get_" + variableName[i], javaNamingConvention, theLog);
                            if (webServiceRecType.equals("public")) {
                                addScalarGetMethod(theJavaChunk, theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , "java.sql.Timestamp", "", variableDataType[i], variableName[i], comments);
                            } else {
                                addScalarGetMethod(theJavaChunk, theJavaAttrsChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , "java.sql.Timestamp", "", variableDataType[i], variableName[i], comments);
                            }

                            getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_" + "TIMESTAMP", javaNamingConvention, theLog);
                            if (!targetVersion.startsWith("DB2")) {
                                addScalarGetMethod(theJavaChunk, theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , "oracle.sql.TIMESTAMP", "", variableDataType[i], variableName[i], comments);
                            }

                            break;
                        }
                        case SqlUtils.ORACLE_TIMESTAMPTZ_DATATYPE: {
                            getMethod = JavaUtils.getJavaName("get_" + variableName[i], javaNamingConvention, theLog);
                            addScalarGetMethod(theJavaChunk, theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                    , "oracle.sql.TIMESTAMPTZ", "", variableDataType[i], variableName[i], comments);
                            getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_byte_array", javaNamingConvention, theLog);
                            if (webServiceRecType.equals("public")) {
                                addScalarGetMethod(theJavaChunk, theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , "byte[]", "", variableDataType[i], variableName[i], comments);
                            } else {
                                addScalarGetMethod(theJavaAttrsChunk, theJavaAttrsChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , "byte[]", "", variableDataType[i], variableName[i], comments);
                            }
                            break;
                        }
                        case SqlUtils.ORACLE_TIMESTAMPLTZ_DATATYPE: {
                            getMethod = JavaUtils.getJavaName("get_" + variableName[i], javaNamingConvention, theLog);
                            addScalarGetMethod(theJavaChunk, theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                    , "oracle.sql.TIMESTAMPLTZ", "", variableDataType[i], variableName[i], comments);
                            getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_byte_array", javaNamingConvention, theLog);
                            if (webServiceRecType.equals("public")) {
                                addScalarGetMethod(theJavaChunk, theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , "byte[]", "", variableDataType[i], variableName[i], comments);
                            } else {
                                addScalarGetMethod(theJavaAttrsChunk, theJavaAttrsChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , "byte[]", "", variableDataType[i], variableName[i], comments);
                            }
                            break;
                        }
                        case SqlUtils.ORACLE_INTERVAL_DAY_TO_SECOND_DATATYPE: {
                            getMethod = JavaUtils.getJavaName("get_" + variableName[i], javaNamingConvention, theLog);
                            addScalarGetMethod(theJavaChunk, theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                    , "oracle.sql.INTERVALDS", "", variableDataType[i], variableName[i], comments);
                            break;
                        }
                        case SqlUtils.ORACLE_INTERVAL_YEAR_TO_MONTH_DATATYPE: {
                            getMethod = JavaUtils.getJavaName("get_" + variableName[i], javaNamingConvention, theLog);
                            addScalarGetMethod(theJavaChunk, theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                    , "oracle.sql.INTERVALYM", "", variableDataType[i], variableName[i], comments);
                            break;
                        }
                        case SqlUtils.ORACLE_LONGTEXT_DATATYPE:
                        case SqlUtils.ORACLE_LONG_BINARY_DATATYPE: {
                            getMethod = JavaUtils.getJavaName("get_" + variableName[i], javaNamingConvention, theLog);
                            if (webServiceRecType.equals("public")) {
                                addScalarGetMethod(theJavaChunk, theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , variableDataType[i], "", variableDataType[i], variableName[i], comments);
                                getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_byte_array", javaNamingConvention, theLog);
                                addScalarGetMethod(theJavaChunk, theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , "byte[]", "", variableDataType[i], byteName[i], comments);
                            } else {
                                addScalarGetMethod(theJavaChunk, theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , variableDataType[i], "", variableDataType[i], variableName[i], comments);
                                getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_byte_array", javaNamingConvention, theLog);
                                addScalarGetMethod(theJavaChunk, theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , "byte[]", "", variableDataType[i], byteName[i], comments);
                            }
                            addScalarGetNVLMethod(theJavaChunk, theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                    , variableDataType[i], "", variableDataType[i], variableName[i], comments);
                            break;
                        }
                        case SqlUtils.ORACLE_XMLTYPE_DATATYPE: {

                            getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_file", javaNamingConvention, theLog);
                            if (webServiceRecType.equals("public")) {
                                addScalarGetMethod(theJavaChunk, theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , variableDataType[i], "", variableDataType[i], variableName[i], comments);
                                //addScalarGetMethod(theJavaChunk, theJavaChunk, getMethod,"get",theRowSet.getString("ARGUMENT_NAME")
                                //    , "byte[]","" ,variableDataType[i],byteName[i],comments);
                                getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_byte_array", javaNamingConvention, theLog);
                                if (useCharForCLOB) {
                                    addScalarGetMethod(theJavaAttrsChunk, theJavaAttrsChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                            , "char[]", "", variableDataType[i], byteName[i], comments);
                                } else {
                                    addScalarGetMethod(theJavaAttrsChunk, theJavaAttrsChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                            , "byte[]", "", variableDataType[i], byteName[i], comments);
                                }
                            } else {
                                if (useCharForCLOB) {

                                    addScalarGetMethod(theJavaAttrsChunk, theJavaAttrsChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                            , "char[]", "", variableDataType[i], byteName[i] /*variableName[i]*/, comments);

                                    getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_byte_array", javaNamingConvention, theLog);
                                    addScalarGetMethod(theJavaChunk, theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                            , "char[]", "", variableDataType[i], byteName[i], comments);
                                } else {
                                    addScalarGetMethod(theJavaAttrsChunk, theJavaAttrsChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                            , "byte[]", "", variableDataType[i], byteName[i] /*variableName[i]*/, comments);

                                    getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_byte_array", javaNamingConvention, theLog);
                                    addScalarGetMethod(theJavaChunk, theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                            , "byte[]", "", variableDataType[i], byteName[i], comments);
                                }
                            }

                            getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_" + theRowSet.getString("DATA_TYPE").toLowerCase(), javaNamingConvention, theLog);
                            addScalarGetMethod(theJavaChunk, theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                    , "oracle.sql.OPAQUE", "", "oracle.sql.OPAQUE", lobName[i], comments);
                            break;
                        }
                        case SqlUtils.ORACLE_CLOB_DATATYPE: {
                            String byteOrChar = "byte";
                            if (useCharForCLOB) byteOrChar = "char";

                            getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_file", javaNamingConvention, theLog);
                            if (webServiceRecType.equals("public")) {
                                addScalarGetMethod(theJavaChunk, theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , variableDataType[i], "", variableDataType[i], variableName[i], comments);
                                getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_byte_array", javaNamingConvention, theLog);
                                addScalarGetMethod(theJavaChunk, theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , byteOrChar + "[]", "", variableDataType[i], byteName[i], comments);
                                //addScalarGetMethod(theJavaAttrsChunk, theJavaAttrsChunk, getMethod,"get",theRowSet.getString("ARGUMENT_NAME")
                                //    , "byte[]","" ,variableDataType[i],variableName[i],comments);
                            } else {
                                //addScalarGetMethod(theJavaAttrsChunk, theJavaAttrsChunk, getMethod,"get",theRowSet.getString("ARGUMENT_NAME")
                                //    , "byte[]","" ,variableDataType[i],variableName[i],comments);
                                addScalarGetMethod(theJavaChunk, theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , variableDataType[i], "", variableDataType[i], variableName[i], comments);
                                getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_byte_array", javaNamingConvention, theLog);
                                addScalarGetMethod(theJavaAttrsChunk, theJavaAttrsChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , byteOrChar + "[]", "", variableDataType[i], byteName[i], comments);
                            }

                            getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_" + theRowSet.getString("DATA_TYPE").toLowerCase(), javaNamingConvention, theLog);
                            addScalarGetMethod(theJavaChunk, theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                    , "oracle.sql." + actualOracleDatatype[i], "", "oracle.sql." + actualOracleDatatype[i], lobName[i], comments);
                            break;
                        }
                        case SqlUtils.ORACLE_BLOB_DATATYPE:
                        case SqlUtils.ORACLE_BFILE_DATATYPE: {

                            getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_file", javaNamingConvention, theLog);
                            if (webServiceRecType.equals("public")) {
                                addScalarGetMethod(theJavaChunk, theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , variableDataType[i], "", variableDataType[i], variableName[i], comments);
                                getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_byte_array", javaNamingConvention, theLog);
                                addScalarGetMethod(theJavaChunk, theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , "byte[]", "", variableDataType[i], byteName[i], comments);
                                //addScalarGetMethod(theJavaAttrsChunk, theJavaAttrsChunk, getMethod,"get",theRowSet.getString("ARGUMENT_NAME")
                                //    , "byte[]","" ,variableDataType[i],variableName[i],comments);
                            } else {
                                //addScalarGetMethod(theJavaAttrsChunk, theJavaAttrsChunk, getMethod,"get",theRowSet.getString("ARGUMENT_NAME")
                                //    , "byte[]","" ,variableDataType[i],variableName[i],comments);
                                addScalarGetMethod(theJavaChunk, theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , variableDataType[i], "", variableDataType[i], variableName[i], comments);
                                getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_byte_array", javaNamingConvention, theLog);
                                addScalarGetMethod(theJavaAttrsChunk, theJavaAttrsChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , "byte[]", "", variableDataType[i], byteName[i], comments);
                            }

                            getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_" + theRowSet.getString("DATA_TYPE").toLowerCase(), javaNamingConvention, theLog);
                            addScalarGetMethod(theJavaChunk, theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                    , "oracle.sql." + actualOracleDatatype[i], "", "oracle.sql." + actualOracleDatatype[i], lobName[i], comments);
                            break;
                        }
                        case SqlUtils.MCPDBWIZARD_READONLYROWSET: {
                            getMethod = JavaUtils.getJavaName("get_" + variableName[i], javaNamingConvention, theLog);
                            addScalarGetMethod(theJavaChunk, theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                    , variableDataType[i], "", variableDataType[i], variableName[i], comments);

                            if (cursorRecordId[i] > Integer.MIN_VALUE) {
                                getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_array", javaNamingConvention, theLog);
                                addArrayReturnMethod(theJavaChunk, getMethod, variableName[i], variableWSDataType[i], variableWSParentDataType[i], comments);
                            }

                            break;
                        }
                        case SqlUtils.ORACLE_OBJECT_DATATYPE:
                        case SqlUtils.ORACLE_TABLE_DATATYPE:
                        case SqlUtils.ORACLE_VARRAY_DATATYPE:
                        case SqlUtils.ORACLE_PLSQL_INDEXBY_ROWTYPE_DATATYPE:
                        case SqlUtils.ORACLE_VECTOR_DATATYPE: {
                            if (!variableDataType[i].equals("double[]")) {
                                // Non-scalar row mis-classified as VECTOR (see the setter arm):
                                // behave exactly like the default arm.
                                getMethod = JavaUtils.getJavaName("get_" + variableName[i], javaNamingConvention, theLog);
                                if (webServiceRecType.equals("public")) {
                                    addObjectScalerGetMethodNoNull(theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                            , variableDataType[i], null, variableDataType[i], variableName[i], comments);
                                } else {
                                    addObjectScalerGetMethodNoNull(theJavaAttrsChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                            , variableDataType[i], null, variableDataType[i], variableName[i], comments);
                                }
                                break;
                            }
                            // Dense getter, identical to the default path...
                            getMethod = JavaUtils.getJavaName("get_" + variableName[i], javaNamingConvention, theLog);
                            if (webServiceRecType.equals("public")) {
                                addObjectScalerGetMethodNoNull(theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , variableDataType[i], null, variableDataType[i], variableName[i], comments);
                            } else {
                                addObjectScalerGetMethodNoNull(theJavaAttrsChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , variableDataType[i], null, variableDataType[i], variableName[i], comments);
                            }
                            // ...plus the binary companion: non-null when the value came back
                            // as a BINARY (bit-packed) vector.
                            getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_vector_binary", javaNamingConvention, theLog);
                            if (webServiceRecType.equals("public")) {
                                addObjectScalerGetMethodNoNull(theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , "byte[]", null, "byte[]", variableName[i] + "VectorBinary", comments);
                            } else {
                                addObjectScalerGetMethodNoNull(theJavaAttrsChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , "byte[]", null, "byte[]", variableName[i] + "VectorBinary", comments);
                            }
                            // ...plus the sparse companion: non-null when the value came back
                            // as a SPARSE vector.
                            getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_vector_sparse", javaNamingConvention, theLog);
                            if (webServiceRecType.equals("public")) {
                                addObjectScalerGetMethodNoNull(theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , "com.mcpdbwizard.pub.SparseVector", null, "com.mcpdbwizard.pub.SparseVector", variableName[i] + "VectorSparse", comments);
                            } else {
                                addObjectScalerGetMethodNoNull(theJavaAttrsChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                        , "com.mcpdbwizard.pub.SparseVector", null, "com.mcpdbwizard.pub.SparseVector", variableName[i] + "VectorSparse", comments);
                            }
                            break;
                        }
                        default: {
                            if (variableDataType[i].equals("Object")) {
                                if (comments) theJavaChunk.print("// " + variableDataType[i] + " is not supported");
                            } else {
                                getMethod = JavaUtils.getJavaName("get_" + variableName[i] /* + "_" + variableDataType[i]  + "_obj" */, javaNamingConvention, theLog);
                                if (webServiceRecType.equals("public")) {
                                    addObjectScalerGetMethodNoNull(theJavaChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                            , variableDataType[i], null, variableDataType[i], variableName[i], comments);
                                } else {
                                    addObjectScalerGetMethodNoNull(theJavaAttrsChunk, getMethod, "get", theRowSet.getString("ARGUMENT_NAME")
                                            , variableDataType[i], null, variableDataType[i], variableName[i], comments);
                                }
                            }
                            break;
                        }
                    }

                }
            } catch (CSNoDataInRowSetException e) {
                if (comments) theJavaChunk.print("// This procedure has no parameters and hence no Get methods");
            } catch (CSException e) {
                theLog.syserror(e, true, true);
            }

        }

        if ((hasFiles || hasChildFiles) && type == IS_A_FUNCTION) {
            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* Get current file io buffer size");
                theJavaChunk.print("* @return int " + sbufferSize + " Buffer size in bytes.");
                theJavaChunk.print("*/");
            }

            theJavaChunk.print("public " + "int "
                    + sGetBufferSize + "()");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("return (" + "" + sbufferSize + "" + ");");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();

            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* Get " + skeepLobs + " flag");
                theJavaChunk.print("* @return int <code>true</code> if LOBS are left as they are found in the DB");
                theJavaChunk.print("* @return int <code>false</code> if LOBS are turned into files on retrieval.");
                theJavaChunk.print("* @since 4.0.1847");
                theJavaChunk.print("*/");
            }

            theJavaChunk.print("public " + "boolean "
                    + sGetKeepLobs + "()");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("return (" + skeepLobs + ");");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();


            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* Get flag that controls whether LOBs are turned into byte[].");
                theJavaChunk.print("* @return boolean useByteArraysForLongsAndLOBS ");
                theJavaChunk.print("* @since 5.0.2314");
                theJavaChunk.print("*/");
            }

            theJavaChunk.print("public " + "boolean "
                    + "getUseByteArraysForLongsAndLOBS()");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("return (useByteArraysForLongsAndLOBS);");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();

            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* Get " + skeepFiles + " flag");
                theJavaChunk.print("* @return int <code>true</code> if temporary files are kept after the JVM exits.");
                theJavaChunk.print("* @return int <code>false</code> if temporary files are deleted after the JVM exits.");
                theJavaChunk.print("*/");
            }

            theJavaChunk.print("public " + "boolean "
                    + sGetKeepFiles + "()");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("return (" + "" + skeepFiles + "" + ");");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();


            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* Get temporary directory");
                theJavaChunk.print("* @return java.io.File " + stempFileDir + " the temporary Directory");
                theJavaChunk.print("*/");
            }

            theJavaChunk.print("public " + "java.io.File "
                    + sGetTempFileDir + "()");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("return(" + stempFileDir + ");");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();

            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* Get the prefix used for generating temporary files");
                theJavaChunk.print("*/");
            }

            theJavaChunk.print("public " + "String "
                    + sGetTempFilePrefix + "()");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("return (" + stempFilePrefix + ");");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();

            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* Get the suffix used for generating temporary files");
                theJavaChunk.print("*/");
            }

            theJavaChunk.print("public " + "String "
                    + sGetTempFileSuffix + "()");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("return (" + stempFileSuffix + ");");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();


        }
    }

    void addFilenameSetMethods(JavaChunk theJavaChunk, boolean comments, boolean debugMessages) {
        if (hasFiles) {
            for (int i = 0; i < theRowSet.size(); i++) {
                String setMethod = JavaUtils.getJavaName("set_" + otherName[i], javaNamingConvention, theLog);

                try {
                    theRowSet.setCurrentRowNumber(i);

                    if (theRowSet.getString("IN_OUT").equals("OUT")
                            || theRowSet.getString("IN_OUT").equals("IN/OUT")
                            || overRideInOut.equals("IN/OUT")
                    ) {
                        switch (oracleUnderlyingDatatype[i]) {
                            case SqlUtils.ORACLE_LONGTEXT_DATATYPE:
                            case SqlUtils.ORACLE_LONG_BINARY_DATATYPE:
                            case SqlUtils.ORACLE_CLOB_DATATYPE:
                            case SqlUtils.ORACLE_BLOB_DATATYPE:
                            case SqlUtils.ORACLE_BFILE_DATATYPE: {
                                theJavaChunk.print("");
                                if (comments) {
                                    theJavaChunk.print("/**");
                                    theJavaChunk.print("* Method to set desired filename for " + variableName[i]);
                                    theJavaChunk.print("* @param String " + otherName[i]);
                                    theJavaChunk.print("*/");
                                }

                                theJavaChunk.print("public void " + setMethod + " (String " + otherName[i] + ")");
                                theJavaChunk.indent();
                                theJavaChunk.print("{");
                                theJavaChunk.print("this." + otherName[i] + " = " + otherName[i] + ";");

                                if (debugMessages) {
                                    theJavaChunk.print(stheLog + ".debug(\"" + otherName[i] + " set to \" + " + otherName[i] + ");");
                                }

                                theJavaChunk.print("}");
                                theJavaChunk.unIndent();
                                break;
                            }
                            default: {
                                break;
                            }
                        }

                    }
                } catch (CSNoDataInRowSetException e) {
                } catch (CSException e) {
                    theLog.syserror(e, true, true);
                }
            } // for
        } // if

    }

    void addFilenameVariables(JavaChunk theJavaChunk, JavaChunk theJavaAttrsChunk, boolean comments, boolean webServices) {
        addFilenameVariables(theJavaChunk, theJavaAttrsChunk, comments, false, webServices);
    }

    void addFilenameVariables(JavaChunk theJavaChunk, JavaChunk theJavaAttrsChunk, boolean comments, boolean makeVarsPublic, boolean webServices) {
        //String accessLevel = "protected";

//  if (makeVarsPublic)
//    {
//    accessLevel = "public";
//    }

        if (hasFiles) {
            for (int i = 0; i < theRowSet.size(); i++) {

                try {
                    theRowSet.setCurrentRowNumber(i);

                    if (theRowSet.getString("IN_OUT").equals("OUT")
                            || theRowSet.getString("IN_OUT").equals("IN/OUT")
                            || theRowSet.getString("IN_OUT").equals("IN") //DRKLUGE
                            || overRideInOut.equals("IN/OUT")
                    ) {
                        switch (oracleUnderlyingDatatype[i]) {
                            case SqlUtils.ORACLE_CLOB_DATATYPE: {
                                if (comments) {
                                    theJavaChunk.print("");
                                    theJavaChunk.print("/**");
                                    theJavaChunk.print("* Variable to store desired filename for " + variableName[i]);
                                    theJavaChunk.print("*/");
                                }
                                theJavaChunk.print("public" + /* accessLevel */    " String" + " " + otherName[i] + " = null;");

                                if (true || webServices) {
                                    if (comments) {
                                        theJavaAttrsChunk.print("");
                                        theJavaAttrsChunk.print("/**");
                                        theJavaAttrsChunk.print("* Variable to store byte array for " + variableName[i]);
                                        theJavaAttrsChunk.print("*/");
                                    }

                                    if (useCharForCLOB) {
                                        theJavaAttrsChunk.print("public" + /* accessLevel */   " " + "char[]" + " " + byteName[i] + " = null;");
                                    } else {
                                        theJavaAttrsChunk.print("public" + /* accessLevel */   " " + "byte[]" + " " + byteName[i] + " = null;");
                                    }
                                }

                                break;
                            }
                            case SqlUtils.ORACLE_XMLTYPE_DATATYPE: {
                                if (comments) {
                                    theJavaChunk.print("");
                                    theJavaChunk.print("/**");
                                    theJavaChunk.print("* Variable to store desired filename for " + variableName[i]);
                                    theJavaChunk.print("*/");
                                }
                                theJavaChunk.print("public" + /* accessLevel */    " String" + " " + otherName[i] + " = null;");

                                if (true || webServices) {
                                    if (comments) {
                                        theJavaAttrsChunk.print("");
                                        theJavaAttrsChunk.print("/**");
                                        theJavaAttrsChunk.print("* Variable to store byte array for " + variableName[i]);
                                        theJavaAttrsChunk.print("*/");
                                    }

                                    if (useCharForCLOB) {
                                        theJavaAttrsChunk.print("public" + /* accessLevel */   " " + "char[]" + " " + byteName[i] + " = null;");
                                    } else {
                                        theJavaAttrsChunk.print("public" + /* accessLevel */   " " + "byte[]" + " " + byteName[i] + " = null;");
                                    }
                                }

                                break;
                            }
                            case SqlUtils.ORACLE_LONGTEXT_DATATYPE:
                            case SqlUtils.ORACLE_LONG_BINARY_DATATYPE:
                            case SqlUtils.ORACLE_BLOB_DATATYPE:
                            case SqlUtils.ORACLE_BFILE_DATATYPE: {
                                if (comments) {
                                    theJavaChunk.print("");
                                    theJavaChunk.print("/**");
                                    theJavaChunk.print("* Variable to store desired filename for " + variableName[i]);
                                    theJavaChunk.print("*/");
                                }
                                theJavaChunk.print("public" + /* accessLevel */    " String" + " " + otherName[i] + " = null;");

                                if (true || webServices) {
                                    if (comments) {
                                        theJavaAttrsChunk.print("");
                                        theJavaAttrsChunk.print("/**");
                                        theJavaAttrsChunk.print("* Variable to store byte array for " + variableName[i]);
                                        theJavaAttrsChunk.print("*/");
                                    }

                                    theJavaAttrsChunk.print("public" + /* accessLevel */   " " + "byte[]" + " " + byteName[i] + " = null;");
                                }

                                break;
                            }
                            default: {
                                break;
                            }
                        }
                    }

                    switch (oracleUnderlyingDatatype[i]) {
                        case SqlUtils.ORACLE_CLOB_DATATYPE:
                        case SqlUtils.ORACLE_BLOB_DATATYPE:
                        case SqlUtils.ORACLE_BFILE_DATATYPE: {
                            if (comments) {
                                theJavaChunk.print("");
                                theJavaChunk.print("/**");
                                theJavaChunk.print("* Variable to store LOB locator for " + variableName[i]);
                                theJavaChunk.print("* @see oracle.sql." + actualOracleDatatype[i].toUpperCase());
                                theJavaChunk.print("*/");
                            }

                            theJavaChunk.print("public" + /* accessLevel */  " " + "oracle.sql." + actualOracleDatatype[i].toUpperCase() + " " + lobName[i] + " = null;");
                            break;
                        }
                        case SqlUtils.ORACLE_XMLTYPE_DATATYPE: {
                            if (comments) {
                                theJavaChunk.print("");
                                theJavaChunk.print("/**");
                                theJavaChunk.print("* Variable to store XMLType " + variableName[i]);
                                theJavaChunk.print("* @see oracle.sql." + actualOracleDatatype[i].toUpperCase());
                                theJavaChunk.print("*/");
                            }

                            theJavaChunk.print("public" + /* accessLevel */  " oracle.sql.OPAQUE " + lobName[i] + " = null;");
                            break;
                        }
                        default: {
                            break;
                        }

                    }
                } catch (CSNoDataInRowSetException e) {
                } catch (CSException e) {
                    theLog.syserror(e, true, true);
                }
            } // for
        } // if

    }

    String[] getStatementParamString() {
        String qString = "";
        String startString = "BEGIN ";

        boolean isAFunction = false;

        try {
            // See if is a complex statement...


            if (isComplex) {
                return (this.getComplexStatementParamString());
            }

            theRowSet.setCurrentRowNumber(0);
            if (theRowSet.getInt("POSITION") == 0) {
                isAFunction = true;
            }

            if (isAFunction) {
                startString = "BEGIN ? := ";
                for (int i = 1; i < theRowSet.size(); i++) {
                    theRowSet.setCurrentRowNumber(i);
                    String aName = theRowSet.getString("ARGUMENT_NAME") + " => ";
                    if (i == 1) {
                        qString = qString + " ?";
                    } else {
                        qString = qString + ", " + aName + " ?";
                    }
                }
            } else {
                for (int i = 0; i < theRowSet.size(); i++) {
                    theRowSet.setCurrentRowNumber(i);
                    String aName = theRowSet.getString("ARGUMENT_NAME") + " => ";
                    if (i == 0) {
                        qString = aName + " ?";
                    } else {
                        qString = qString + ", " + aName + " ?";
                    }
                }
            }
        } catch (CSNoDataInRowSetException e) {
            qString = "";
        } catch (CSException e) {
            theLog.syserror(e, true, true);
        }

        if (qString.length() > 0) {
            qString = "(" + qString + ")";
        }

        String[] tempString = new String[1];
        // The SIMPLE call shape -- a bare BEGIN block, used when nothing needs declaring. It is
        // the OTHER half of the PL/SQL surface: the DECLARE builder below has carried the marker
        // for years, this one never did, so half of all routine calls were unfindable in V$SQL by
        // any pattern at all. Marked here so "PL/SQL calls are greppable" is true of all of them.
        tempString[0] = SqlUtils.GENERATED_BY_COMMENT + " " + startString + oracleName + qString + "; END; ";
        return (tempString);
    }

    String[] OLDgetStatementParamString() {
        String qString = "";
        String startString = "BEGIN ";

        boolean isAFunction = false;

        try {
            // See if is a complex statement...


            if (isComplex) {
                return (this.getComplexStatementParamString());
            }

            theRowSet.setCurrentRowNumber(0);
            if (theRowSet.getInt("POSITION") == 0) {
                isAFunction = true;
            }

            if (isAFunction) {
                startString = "BEGIN ? := ";
                for (int i = 1; i < theRowSet.size(); i++) {
                    if (i == 1) {
                        qString = "?";
                    } else {
                        qString = qString + ",?";
                    }
                }
            } else {
                for (int i = 0; i < theRowSet.size(); i++) {
                    if (i == 0) {
                        qString = "?";
                    } else {
                        qString = qString + ",?";
                    }
                }
            }
        } catch (CSNoDataInRowSetException e) {
            qString = "";
        } catch (CSException e) {
            theLog.syserror(e, true, true);
        }

        if (qString.length() > 0) {
            qString = "(" + qString + ")";
        }

        String[] tempString = new String[1];
        tempString[0] = startString + oracleName + qString + "; END; ";
        return (tempString);
    }

    private void getComplexStatementRecordVariables(String recName, ArrayList plsqlText, PlsqlRecordObject thisRecord, LegalOracleNameWranger n) throws CSException {


        for (int i = 0; i < thisRecord.argRowSet.size(); i++) {
            thisRecord.argRowSet.setCurrentRowNumber(i);

            String argName = null;

            if (thisRecord.argRowSet.getInt("POSITION") == 0) {
                argName = sfunctionResult; // KLUGE 012 need to be sure name is unique
            } else {
                argName = thisRecord.argRowSet.getString("ARGUMENT_NAME").toLowerCase();
            }

            String dataType = thisRecord.argRowSet.getString("DATA_TYPE");
            //String dataSizeString = "";

            if (dataType.equalsIgnoreCase("REF CURSOR")) {
                //plsqlText.add("TYPE " + argName + "_RC IS " + dataType + ";");
                //plsqlText.add(argName + " " + argName + "_RC " + ";");
            } else if (dataType.equalsIgnoreCase("PL/SQL BOOLEAN")) {
                //plsqlText.add(argName + " " + "BOOLEAN" + " := null;");
                //if (   theRowSet.getString("IN_OUT").equals("IN")
                ///    || theRowSet.getString("IN_OUT").equals("IN/OUT")
                //    )
                //  {
                //  if (inParamsAfterBegin)
                //    {
                if (targetVersion.startsWith("DB2")) {
                    plsqlText.add(n.createName(recName + "_" + argName + "_SN") + " NUMBER" + " := NULL;");
                } else {
                    plsqlText.add(n.createName(recName + "_" + argName + "_SN") + " SIGNTYPE" + " := NULL;");
                }
                //    }
                //  else
                //    {
                //    plsqlText.add(argName + "_SN " + "SIGNTYPE" + " := ?;");
                //    }
                //  }
                //else
                //  {
                //  plsqlText.add(argName + "_SN " + "SIGNTYPE" + " := null;");
                //  }
            } else if (dataType.equalsIgnoreCase("PL/SQL RECORD")) {
                // Item 7 (nested records): recurse so a boolean field of a nested record gets its
                // SIGNTYPE flag declared with the full path-derived name (recName_field_..._SN), matching
                // emitRecordInAssigns/emitRecordOutAssigns. No-op for a flat record (no PL/SQL RECORD field).
                int nid = findRecordIdByType(thisRecord.argRowSet.getString("TYPE_OWNER"),
                        thisRecord.argRowSet.getString("TYPE_NAME"), thisRecord.argRowSet.getString("TYPE_SUBNAME"));
                if (nid > Integer.MIN_VALUE) {
                    getComplexStatementRecordVariables(recName + "_" + argName, plsqlText, theRecords[nid], n);
                }
            }
        } // end of 1st pass through rowset
    }

    /**
     * Index in {@code theRecords} of the record whose PL/SQL type identity matches
     * (TYPE_OWNER/TYPE_NAME/TYPE_SUBNAME), following the {@code replacedByArrayId} dedup, or
     * {@code Integer.MIN_VALUE}. Item 7: resolves a nested record field (which has no positional
     * theRecords entry on 23ai) to its generated class, exactly as the field-type switch does.
     */
    private int findRecordIdByType(String typeOwner, String typeName, String typeSubName) {
        if (typeOwner == null || typeName == null || typeSubName == null) {
            return Integer.MIN_VALUE;
        }
        for (int q = 0; q < theRecords.length; q++) {
            if (typeOwner.equals(theRecords[q].typeOwner)
                    && typeName.equals(theRecords[q].typeName)
                    && typeSubName.equals(theRecords[q].typeSubName)) {
                if (theRecords[q].replacedByArrayId > Integer.MIN_VALUE) {
                    return theRecords[q].replacedByArrayId;
                }
                return q;
            }
        }
        return Integer.MIN_VALUE;
    }

    /**
     * The recursive scalar-leaf count of a record: a nested-record field contributes its OWN leaf
     * count, not 1. Item 7: the flat {@code fieldCount} undercounts a record with nested-record
     * fields, which throws off the IN/OUT parameter numbering ({@code recordInParams}/{@code
     * recordOutParams}). Equal to {@code fieldCount} for a flat record (byte-identical numbering).
     */
    private int recursiveLeafCount(PlsqlRecordObject rec) throws CSException {
        if (rec == null || rec.argRowSet == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < rec.argRowSet.size(); i++) {
            rec.argRowSet.setCurrentRowNumber(i);
            if ("PL/SQL RECORD".equalsIgnoreCase(rec.argRowSet.getString("DATA_TYPE"))) {
                int nid = findRecordIdByType(rec.argRowSet.getString("TYPE_OWNER"),
                        rec.argRowSet.getString("TYPE_NAME"), rec.argRowSet.getString("TYPE_SUBNAME"));
                count += (nid > Integer.MIN_VALUE) ? recursiveLeafCount(theRecords[nid]) : 1;
            } else {
                count += 1;
            }
        }
        return count;
    }

    /**
     * Emit the DECLARE-block IN assignments for a record's fields ({@code path.field := ?}), recursing
     * into a nested PL/SQL RECORD field so it flattens to leaf scalars ({@code path.inner.field := ?})
     * rather than a single (impossible) record bind. Item 7. Byte-identical to the old inline loop for a
     * flat record (path has no dots, so the boolean SIGNTYPE names are unchanged).
     */
    private void emitRecordInAssigns(String path, PlsqlRecordObject rec, ArrayList plsqlText, LegalOracleNameWranger n) throws CSException {
        String snBase = path.replace('.', '_');
        for (int argList = 0; argList < rec.argRowSet.size(); argList++) {
            rec.argRowSet.setCurrentRowNumber(argList);
            String argRecDataType = rec.argRowSet.getString("DATA_TYPE");
            String argRecArgName = rec.argRowSet.getString("ARGUMENT_NAME").toLowerCase();
            if (argRecDataType.equalsIgnoreCase("PL/SQL BOOLEAN")) {
                String sn = n.createName(snBase + "_" + argRecArgName + "_SN");
                plsqlText.add(" ");
                plsqlText.add(sn + " := ?;");
                plsqlText.add("IF (" + sn + " IS NULL OR " + sn + " = 0) THEN ");
                plsqlText.add("  " + path + "." + argRecArgName + " := null; ");
                plsqlText.add("ELSIF " + sn + " = -1 THEN ");
                plsqlText.add("  " + path + "." + argRecArgName + " := FALSE; ");
                plsqlText.add("ELSIF " + sn + " = 1 THEN ");
                plsqlText.add("  " + path + "." + argRecArgName + " := TRUE; ");
                plsqlText.add("END IF; ");
            } else if (argRecDataType.equalsIgnoreCase("PL/SQL RECORD")) {
                int nid = findRecordIdByType(rec.argRowSet.getString("TYPE_OWNER"),
                        rec.argRowSet.getString("TYPE_NAME"), rec.argRowSet.getString("TYPE_SUBNAME"));
                if (nid > Integer.MIN_VALUE) {
                    emitRecordInAssigns(path + "." + argRecArgName, theRecords[nid], plsqlText, n);
                } else {
                    plsqlText.add(path + "." + argRecArgName + " := ?;");
                }
            } else {
                plsqlText.add(path + "." + argRecArgName + " := ?;");
            }
        }
    }

    /**
     * Emit the DECLARE-block OUT reads for a record's fields ({@code ? := path.field}), recursing into a
     * nested PL/SQL RECORD field. Item 7. Byte-identical to the old inline loop for a flat record/object.
     */
    private void emitRecordOutAssigns(String path, PlsqlRecordObject rec, ArrayList plsqlText, LegalOracleNameWranger n) throws CSException {
        String snBase = path.replace('.', '_');
        for (int argList = 0; argList < rec.argRowSet.size(); argList++) {
            rec.argRowSet.setCurrentRowNumber(argList);
            String argRecDataType = rec.argRowSet.getString("DATA_TYPE");
            String argRecArgName = rec.argRowSet.getString("ARGUMENT_NAME").toLowerCase();
            if (argRecDataType.equalsIgnoreCase("PL/SQL BOOLEAN")) {
                String sn = n.createName(snBase + "_" + argRecArgName + "_SN");
                plsqlText.add(" ");
                plsqlText.add("IF " + path + "." + argRecArgName + " IS NULL THEN ");
                plsqlText.add("  " + sn + " := 0; ");
                plsqlText.add("ELSIF " + path + "." + argRecArgName + " = TRUE THEN ");
                plsqlText.add("  " + sn + " := 1; ");
                plsqlText.add("ELSE");
                plsqlText.add("  " + sn + " := -1; ");
                plsqlText.add("END IF; ");
                plsqlText.add("? := " + sn + ";");
                plsqlText.add(" ");
            } else if (argRecDataType.equalsIgnoreCase("PL/SQL RECORD")) {
                int nid = findRecordIdByType(rec.argRowSet.getString("TYPE_OWNER"),
                        rec.argRowSet.getString("TYPE_NAME"), rec.argRowSet.getString("TYPE_SUBNAME"));
                if (nid > Integer.MIN_VALUE) {
                    emitRecordOutAssigns(path + "." + argRecArgName, theRecords[nid], plsqlText, n);
                } else {
                    plsqlText.add("? := " + path + "." + argRecArgName + ";");
                }
            } else {
                plsqlText.add("? := " + path + "." + argRecArgName + ";");
            }
        }
    }

    String[] getComplexStatementParamString() {
        final String ORACLE_NINES = "9999999999999999999999999999999999999999";

        boolean inParamsAfterBegin = false;

        ArrayList plsqlText = new ArrayList();

        //final String padString = "           ";

        String qString = "";
        String startString = "";

        LegalOracleNameWranger n = new LegalOracleNameWranger(theLog);       //EVS

        plsqlText.add(new String("DECLARE"));
        // Shared with the SQL statement builder so both halves stamp the SAME text -- this read
        // "Created  By" with two spaces until 2026-08-16, which meant anyone grepping V$SQL had to
        // reproduce the double space to find PL/SQL calls.
        plsqlText.add(new String(SqlUtils.GENERATED_BY_COMMENT));
        plsqlText.add(new String("/* Which can be obtained at " + Namer.param_product_www + " */"));

        try {

            // Make pass and check to see if we assign in the declare section or not
            for (int i = 0; i < theRowSet.size(); i++) {
                theRowSet.setCurrentRowNumber(i);

                if (theRowSet.getString("DATA_TYPE").equalsIgnoreCase("PL/SQL RECORD")
                        || theRowSet.getString("DATA_TYPE").equalsIgnoreCase("OBJECT")
                        || theRowSet.getString("DATA_TYPE").equalsIgnoreCase("XMLTYPE") //DRKLUGE
                        || theRowSet.getString("DATA_TYPE").equalsIgnoreCase("PL/SQL TABLE")) {
                    inParamsAfterBegin = true;
                }
            }

            // Declare variables...
            for (int i = 0; i < theRowSet.size(); i++) {
                theRowSet.setCurrentRowNumber(i);

                String argName = null;

                if (theRowSet.getInt("POSITION") == 0) {
                    argName = sfunctionResult; // KLUGE 012 need to be sure name is unique
                } else {
                    argName = theRowSet.getString("ARGUMENT_NAME").toLowerCase();
                }


                // Add arg to list...
                n.createName(argName);

                String dataType = theRowSet.getString("DATA_TYPE");
                String dataSizeString = "";
                String initVal = "null";


                String variableRecordType = theRowSet.getString("TYPE_NAME") + "." + theRowSet.getString("TYPE_SUBNAME");

                if (paramInId[i] > 0) {
                    if (inParamsAfterBegin) {
                        initVal = "NULL";
                    } else {
                        initVal = "?";
                    }
                }

                if (dataType.equalsIgnoreCase("CHAR")
                        || dataType.equalsIgnoreCase("CHARACTER")
                        || dataType.equalsIgnoreCase("NCHAR")
                        || dataType.equalsIgnoreCase("NCHARACTER")
                        || dataType.equalsIgnoreCase("RAW")) {
                    // figure out what datasize should be...
                    Integer dataSizeInteger = theRowSet.getIntegerObj("DATA_LENGTH");
                    if (dataSizeInteger == null) {
                        dataSizeString = "(" + MAX_CHAR_SIZE + ")";
                    } else {
                        dataSizeString = "(" + dataSizeInteger.intValue() + ")";
                    }
                } else if (
                        dataType.equalsIgnoreCase("STRING")
                                || dataType.equalsIgnoreCase("VARCHAR")
                                || dataType.equalsIgnoreCase("VARCHAR2")) {
                    // figure out what datasize should be...
                    Integer dataSizeInteger = theRowSet.getIntegerObj("DATA_LENGTH");
                    if (dataSizeInteger == null) {
                        dataSizeString = "(" + MAX_STRING_SIZE + ")";
                    } else {
                        dataSizeString = "(" + dataSizeInteger.intValue() + ")";
                    }
                } else if (dataType.equalsIgnoreCase("PL/SQL TABLE")) {
                    dataSizeString = "(" + (plsqlIndexByDataLength[i] + 2) + ")";
                }

                if (dataType.equalsIgnoreCase("REF CURSOR")) {
                    plsqlText.add("TYPE " + n.createName(argName + "_RC") + " IS " + dataType + ";");

                    if (theRowSet.getString("IN_OUT").equals("IN")
                            || theRowSet.getString("IN_OUT").equals("IN/OUT")
                    ) {

                        plsqlText.add("/* Build 2701: Need to call procs that take null cursors as inputs. Oracle JDBC does not support this. */");
                        plsqlText.add("/* Workaround involves creating placeholder and assigning null to it. */ ");

                        plsqlText.add(argName + " " + n.createName(argName + "_RC") + "; " + n.createName(argName + "_X") + " VARCHAR2(1) := ?;  ");
                    } else {
                        plsqlText.add(argName + " " + n.createName(argName + "_RC") + ";");
                    }
                } else if (dataType.equalsIgnoreCase("PL/SQL BOOLEAN")) {
                    String signtype = "SIGNTYPE";
                    if (targetVersion.startsWith("DB2")) {
                        signtype = "NUMBER";
                    }

                    plsqlText.add(argName + " " + "BOOLEAN" + " := null;");

                    if (theRowSet.getString("IN_OUT").equals("IN")
                            || theRowSet.getString("IN_OUT").equals("IN/OUT")
                    ) {

                        if (inParamsAfterBegin) {
                            plsqlText.add(n.createName(argName + "_SN") + " " + signtype + " := NULL;");
                        } else {
                            plsqlText.add(n.createName(argName + "_SN") + " " + signtype + " := ?;");
                        }
                    } else {
                        plsqlText.add(n.createName(argName + "_SN") + " " + signtype + " := null;");
                    }
                } else if (dataType.equalsIgnoreCase("PL/SQL TABLE")
                        && oracleParamDatatype[i].equals("SqlUtils.ORACLE_PLSQL_INDEXBY_ROWTYPE_DATATYPE")) {
                    if (initVal.equals("?")) {
                        if (theRecords[extraObjectId[i]].generatedGenericTypeName == null) {
                            plsqlText.add(argName + " " + JavaUtils.StripLeadingUsername(theRecords[extraObjectId[i]].oracleName, loginName) + " := ?;");
                        } else {
                            plsqlText.add(n.createName(argName + "_T") + " " + theRecords[extraObjectId[i]].generatedGenericTypeName + " := ?;");
                            plsqlText.add(argName
                                    + " "
                                    + JavaUtils.StripLeadingUsername(theRecords[extraObjectId[i]].oracleName, loginName) + ";"); //" := " + JavaUtils.StripLeadingUsername(theRecords[extraObjectId[i]].oracleName,loginName) + "();");

                            plsqlIndexByPlaceHolderVarName[i] = n.createName(argName + "_T");

                        }
                    } else {
                        if (theRecords[extraObjectId[i]].generatedGenericTypeName == null) {
                            plsqlText.add(argName + " " + JavaUtils.StripLeadingUsername(theRecords[extraObjectId[i]].oracleName, loginName) + ";");
                        } else {
                            plsqlText.add(n.createName(argName + "_T") + " " + theRecords[extraObjectId[i]].generatedGenericTypeName + " := " + theRecords[extraObjectId[i]].generatedGenericTypeName + "();");
                            plsqlText.add(argName
                                    + " "
                                    + JavaUtils.StripLeadingUsername(theRecords[extraObjectId[i]].oracleName, loginName) + ";");//" := " + JavaUtils.StripLeadingUsername(theRecords[extraObjectId[i]].oracleName,loginName) + "();");

                            plsqlIndexByPlaceHolderVarName[i] = n.createName(argName + "_T");
                        }
                    }

                    /**
                     if (initVal.equals("?"))
                     {
                     plsqlText.add(argName + " " + PlsqlIndexByTableName[i] + " := ?;");
                     }
                     else
                     {
                     if (theRowSet.getString("IN_OUT").equals("OUT"))
                     {
                     plsqlText.add(argName + " " + PlsqlIndexByTableName[i] + " := NULL;");
                     }
                     else
                     {
                     plsqlText.add(argName + " " + PlsqlIndexByTableName[i] + ";");
                     }
                     }
                     **/
                } else if (dataType.equalsIgnoreCase("PL/SQL TABLE")) {

                    if (plsqlIndexByDataType[i] == OracleTypes.VARCHAR
                            || (plsqlIndexByDataType[i] == OracleTypes.NUMBER && plsqlIndexByRealDataType[i] == oracle.jdbc.OracleTypes.NUMBER)) {
                        plsqlText.add(argName + " " + variableRecordType + ";");
                    } else {
                        plsqlText.add("TYPE " + plsqlIndexByPlaceHolderVarType[i] + " IS TABLE OF VARCHAR2" + dataSizeString + " INDEX BY BINARY_INTEGER;");
                        plsqlText.add(plsqlIndexByPlaceHolderVarName[i] + " " + plsqlIndexByPlaceHolderVarType[i] + ";");
                        plsqlText.add(argName + " " + variableRecordType + ";");
                    }

                } else if (dataType.equalsIgnoreCase("PL/SQL RECORD")) {

                    if (extraObjectId[i] < 0) {
                        theLog.error("Unable to find %ROWTYPE " + dataType + " " + argName);
                        plsqlText.add("Unable to find %ROWTYPE " + dataType + " " + argName);
                    } else {
                        if (theRecords[extraObjectId[i]].objectType == PlsqlRecordObject.PLSQL_ROWTYPE_RECORD
                                || theRecords[extraObjectId[i]].isTableRowtype) {
                            plsqlText.add(argName + " " + JavaUtils.StripLeadingUsername(theRecords[extraObjectId[i]].oracleName, loginName) + "%ROWTYPE;");
                        } else {
                            plsqlText.add(argName + " " + JavaUtils.StripLeadingUsername(theRecords[extraObjectId[i]].oracleName, loginName) + ";");
                        }

                        // Add boolean signtype flags
                        getComplexStatementRecordVariables(argName, plsqlText, theRecords[extraObjectId[i]], n);

                    }
                } else if (dataType.equalsIgnoreCase("TABLE") && arraysSupported) {

                    if (initVal.equals("?")) {
                        if (theRecords[extraObjectId[i]].generatedGenericTypeName == null) {
                            plsqlText.add(argName + " " + JavaUtils.StripLeadingUsername(theRecords[extraObjectId[i]].oracleName, loginName) + " := ?;");
                        } else {
                            plsqlText.add(n.createName(argName + "_T") + " " + theRecords[extraObjectId[i]].generatedGenericTypeName + " := ?;");
                            plsqlText.add(argName + " " + JavaUtils.StripLeadingUsername(theRecords[extraObjectId[i]].oracleName, loginName) + " := " + JavaUtils.StripLeadingUsername(theRecords[extraObjectId[i]].oracleName, loginName) + "();");
                        }
                    } else {
                        if (theRecords[extraObjectId[i]].generatedGenericTypeName == null) {
                            plsqlText.add(argName + " " + JavaUtils.StripLeadingUsername(theRecords[extraObjectId[i]].oracleName, loginName) + ";");
                        } else {
                            //plsqlText.add(argName + "_T " + theRecords[extraObjectId[i]].generatedGenericTypeName + ";");
                            plsqlText.add(n.createName(argName + "_T") + " " + theRecords[extraObjectId[i]].generatedGenericTypeName + " := " + theRecords[extraObjectId[i]].generatedGenericTypeName + "();");
                            plsqlText.add(argName + " " + JavaUtils.StripLeadingUsername(theRecords[extraObjectId[i]].oracleName, loginName) + " := " + JavaUtils.StripLeadingUsername(theRecords[extraObjectId[i]].oracleName, loginName) + "();");
                        }
                    }
                } else if (dataType.equalsIgnoreCase("VARRAY") && arraysSupported) {

                    if (initVal.equals("?")) {
                        if (theRecords[extraObjectId[i]].generatedGenericTypeName == null) {
                            plsqlText.add(argName + " " + JavaUtils.StripLeadingUsername(theRecords[extraObjectId[i]].oracleName, loginName) + " := ?;");
                        } else {
                            plsqlText.add(n.createName(argName + "_T") + " " + theRecords[extraObjectId[i]].generatedGenericTypeName + " := ?;");
                            plsqlText.add(argName + " " + JavaUtils.StripLeadingUsername(theRecords[extraObjectId[i]].oracleName, loginName) + " := " + JavaUtils.StripLeadingUsername(theRecords[extraObjectId[i]].oracleName, loginName) + "();");
                        }
                    } else {
                        if (theRecords[extraObjectId[i]].generatedGenericTypeName == null) {
                            plsqlText.add(argName + " " + JavaUtils.StripLeadingUsername(theRecords[extraObjectId[i]].oracleName, loginName) + ";");
                        } else {
                            //plsqlText.add(argName + "_T " + theRecords[extraObjectId[i]].generatedGenericTypeName + ";");
                            plsqlText.add(n.createName(argName + "_T") + " " + theRecords[extraObjectId[i]].generatedGenericTypeName + " := " + theRecords[extraObjectId[i]].generatedGenericTypeName + "();");
                            plsqlText.add(argName + " " + JavaUtils.StripLeadingUsername(theRecords[extraObjectId[i]].oracleName, loginName) + " := " + JavaUtils.StripLeadingUsername(theRecords[extraObjectId[i]].oracleName, loginName) + "();");
                        }
                    }
                } else if (dataType.equalsIgnoreCase("OBJECT")) {
                    if (theRowSet.getString("TYPE_SUBNAME") == null) {
                        plsqlText.add(argName + " " + theRowSet.getString("TYPE_OWNER") + "." + theRowSet.getString("TYPE_NAME") + ";");
                    } else {
                        plsqlText.add(argName + " " + theRowSet.getString("TYPE_OWNER") + "." + theRowSet.getString("TYPE_NAME") + "." + theRowSet.getString("TYPE_SUBNAME") + ";");
                    }

                } else {
                    plsqlText.add(argName + " " + dataType + dataSizeString + " := " + initVal + ";");
                }

            } // end of 1st pass through rowset

            plsqlText.add("BEGIN ");

            // Do assignments if needed
            if (inParamsAfterBegin) {
                for (int i = 0; i < theRowSet.size(); i++) {
                    theRowSet.setCurrentRowNumber(i);

                    String argName = theRowSet.getString("ARGUMENT_NAME").toLowerCase();
                    if (theRowSet.getInt("POSITION") == 0) {
                        argName = sfunctionResult; // KLUGE 012 need to be sure name is unique
                    }

                    String dataType = theRowSet.getString("DATA_TYPE");
                    //String dataSizeString = "";
                    String initVal = "null";

                    if (paramInId[i] > 0) {
                        initVal = "?";
                    }

                    if (dataType.equalsIgnoreCase("REF CURSOR")) {
                    } else if (dataType.equalsIgnoreCase("PL/SQL BOOLEAN")) {
                        //plsqlText.add(argName + " " + "BOOLEAN" + " := null;");
                        if (theRowSet.getString("IN_OUT").equals("IN")
                                || theRowSet.getString("IN_OUT").equals("IN/OUT")
                        ) {
                            plsqlText.add(n.createName(argName + "_SN") + " " + " := ?;");
                        } else {
                            plsqlText.add(n.createName(argName + "_SN") + " " + " := null;");
                        }
                    } else if (dataType.equalsIgnoreCase("PL/SQL TABLE")) {

                        if (theRowSet.getString("IN_OUT").equals("IN")
                                || theRowSet.getString("IN_OUT").equals("IN/OUT")
                        ) {
                            if (plsqlIndexByDataType[i] == OracleTypes.VARCHAR
                                    || (plsqlIndexByDataType[i] == OracleTypes.NUMBER && plsqlIndexByRealDataType[i] == oracle.jdbc.OracleTypes.NUMBER)) {
                                plsqlText.add(argName + " := ?;");
                            } else {
                                plsqlText.add(plsqlIndexByPlaceHolderVarName[i] + " := ?;");
                            }
                        } else {
                            //  if (   plsqlIndexByDataType[i] == OracleTypes.VARCHAR
                            //    || (plsqlIndexByDataType[i] == OracleTypes.NUMBER && plsqlIndexByRealDataType[i] == oracle.jdbc.OracleTypes.NUMBER))
                            //    {
                            //    plsqlText.add(argName + " := null;");                                                     //DRKLUGE not needed
                            //    }
                            //  else
                            //    {
                            //    plsqlText.add(plsqlIndexByPlaceHolderVarName[i] + " := null;");     //DRKLUGE - not needed
                            //    }
                        }

                    } else if (dataType.equalsIgnoreCase("PL/SQL RECORD")) {
                        if (theRowSet.getString("IN_OUT").equals("IN")
                                || theRowSet.getString("IN_OUT").equals("IN/OUT")
                        ) {
                            if (extraObjectId[i] < 0) {
                                theLog.error("Unable to find java class for " + dataType + " " + argName);
                            } else {
                                // Item 7: recurse so a nested PL/SQL RECORD field flattens to leaf scalars.
                                emitRecordInAssigns(argName, theRecords[extraObjectId[i]], plsqlText, n);
                            }
                        }
                    } else if (dataType.equalsIgnoreCase("OBJECT")) {
                        if (theRowSet.getString("IN_OUT").equals("IN")
                                || theRowSet.getString("IN_OUT").equals("IN/OUT")
                        ) {
                            if (extraObjectId[i] < 0) {
                                theLog.error("Unable to find java class for " + dataType + " " + argName);
                            } else {
                                // for (int argList=0; argList < theRecords[extraObjectId[i]].argRowSet.size(); argList++)
                                //  {
                                //  theRecords[extraObjectId[i]].argRowSet.setCurrentRowNumber(argList);
                                //  plsqlText.add(argName + "." + theRecords[extraObjectId[i]].argRowSet.getString("ARGUMENT_NAME").toLowerCase() + " := ?;");
                                String qCount = "";
                                for (int argList = 0; argList < theRecords[extraObjectId[i]].argRowSet.size(); argList++) {
                                    if (argList == 0) {
                                        qCount = "?";
                                    } else {
                                        qCount = qCount + ",?";
                                    }
                                }
                                plsqlText.add(argName + " := " + theRowSet.getString("TYPE_OWNER") + "." + theRowSet.getString("TYPE_NAME") + "(" + qCount + ");");
                            }
                        }
                    } else if (dataType.equalsIgnoreCase("VARRAY")
                            || dataType.equalsIgnoreCase("TABLE")) {
                        if (theRowSet.getString("IN_OUT").equals("IN")
                                || theRowSet.getString("IN_OUT").equals("IN/OUT")
                        ) {
                            if (theRecords[extraObjectId[i]].generatedGenericTypeName == null) {
                                plsqlText.add(argName + " := ?;");
                            } else {
                                plsqlText.add(n.createName(argName + "_T") + " := ?;");
                            }
                        }
                    } else {
                        plsqlText.add(argName + " := " + initVal + ";");
                    }
                }
            }


            // DO Boolean conversions
            for (int i = 0; i < theRowSet.size(); i++) {
                theRowSet.setCurrentRowNumber(i);
                String argName = null;

                if (theRowSet.getInt("POSITION") == 0) {
                    argName = sfunctionResult; // KLUGE 012 need to be sure name is unique
                } else {
                    argName = theRowSet.getString("ARGUMENT_NAME").toLowerCase();
                }

                String dataType = theRowSet.getString("DATA_TYPE");

                if (dataType.equalsIgnoreCase("PL/SQL BOOLEAN")) {
                    if (theRowSet.getString("IN_OUT").equals("IN")
                            || theRowSet.getString("IN_OUT").equals("IN/OUT")) {
                        plsqlText.add(" ");
                        plsqlText.add("IF (" + n.createName(argName + "_SN") + " IS NULL OR " + n.createName(argName + "_SN") + " = 0) THEN ");
                        plsqlText.add("  " + argName + " := null; ");
                        plsqlText.add("ELSIF " + n.createName(argName + "_SN") + " = -1 THEN ");
                        plsqlText.add("  " + argName + " := FALSE; ");
                        plsqlText.add("ELSIF " + n.createName(argName + "_SN") + " = 1 THEN ");
                        plsqlText.add("  " + argName + " := TRUE; ");
                        plsqlText.add("END IF; ");
                        plsqlText.add(" ");
                    }
                } else if (dataType.equalsIgnoreCase("PL/SQL TABLE")
                        && oracleParamDatatype[i].equals("SqlUtils.ORACLE_PLSQL_INDEXBY_ROWTYPE_DATATYPE")) {
                    if (theRowSet.getString("IN_OUT").equals("IN")
                            || theRowSet.getString("IN_OUT").equals("IN/OUT")) {
                        if (theRecords[extraObjectId[i]].generatedGenericTypeName != null) {

                            plsqlText.add("IF " + n.createName(argName + "_T") + ".COUNT > 0 THEN");
                            //plsqlText.add("  " + argName + ".EXTEND("+ argName + "_T.COUNT);");
                            plsqlText.add("  FOR i IN " + n.createName(argName + "_T") + ".FIRST.." + n.createName(argName + "_T") + ".LAST LOOP");
                            //plsqlText.add(" "+argName+"(i) := " + argName + "");
                            if (theRecords[extraObjectId[i]].plsqlPackAssign != null) {

                                for (int z = 0; z < theRecords[extraObjectId[i]].plsqlPackAssign.length; z++) {
                                    String tempString = JavaUtils.replaceString(theRecords[extraObjectId[i]].plsqlPackAssign[z], ExtraType.PARAM_TARGET_PARAM_ARRAY_NAME, n.createName(argName + "_T"));
                                    tempString = JavaUtils.replaceString(tempString, ExtraType.PARAM_TARGET_PARAM_NAME, argName);
                                    tempString = JavaUtils.replaceString(tempString, ExtraType.PARAM_TARGET_PARAM_NAME_ATYPE, theRecords[extraObjectId[i]].generatedGenericTypeName);
                                    tempString = JavaUtils.replaceString(tempString, ExtraType.PARAM_TARGET_PARAM_NAME_REALTYPE, JavaUtils.StripLeadingUsername(theRecords[extraObjectId[i]].oracleName, loginName));
                                    plsqlText.add("    " + tempString);
                                }
                            } else {
                                plsqlText.add("    " + argName + "(i) := " + n.createName(argName + "_T") + "(i);");
                            }

                            plsqlText.add("  END LOOP;");
                            plsqlText.add("END IF;");
                        }
                    }
                } else if (dataType.equalsIgnoreCase("PL/SQL TABLE")) {


                    if (theRowSet.getString("IN_OUT").equals("IN")
                            || theRowSet.getString("IN_OUT").equals("IN/OUT")) {
                        if (plsqlIndexByDataType[i] == OracleTypes.VARCHAR
                                || (plsqlIndexByDataType[i] == OracleTypes.NUMBER && plsqlIndexByRealDataType[i] == oracle.jdbc.OracleTypes.NUMBER)) {
                        } else {


                            plsqlText.add(" ");
                            plsqlText.add("FOR i IN " + plsqlIndexByPlaceHolderVarName[i] + ".FIRST.." + plsqlIndexByPlaceHolderVarName[i] + ".LAST LOOP");
                            if (plsqlIndexByDataType[i] == OracleTypes.DATE) {
                                plsqlText.add(" " + argName + "(i) := TO_DATE(" + plsqlIndexByPlaceHolderVarName[i] + "(i),'" + PlsqlIndexByTable2.ORACLE_DATE_TO_CHAR_MASK + "');");
                            } else if (plsqlIndexByDataType[i] == OracleTypes.TIMESTAMP) {
                                plsqlText.add(" " + argName + "(i) := TO_TIMESTAMP(" + plsqlIndexByPlaceHolderVarName[i] + "(i),'" + PlsqlIndexByTable2.ORACLE_TIMESTAMP_TO_CHAR_MASK + "');");
                            } else if (plsqlIndexByDataType[i] == OracleTypes.TIMESTAMPTZ
                                    || plsqlIndexByDataType[i] == OracleTypes.TIMESTAMPLTZ) {
                                // TO_TIMESTAMP_TZ, not TO_TIMESTAMP: the unzoned function discards
                                // whatever zone the text carries and hands PL/SQL a value that then
                                // silently acquires the SESSION's zone. Assigning the result to a
                                // LOCAL variable converts it to the session zone, which is the
                                // correct meaning of LTZ, so both share this one line.
                                plsqlText.add(" " + argName + "(i) := TO_TIMESTAMP_TZ(" + plsqlIndexByPlaceHolderVarName[i] + "(i),'" + PlsqlIndexByTable2.ORACLE_TIMESTAMPTZ_TO_CHAR_MASK + "');");
                            } else if (plsqlIndexByDataType[i] == OracleTypes.NUMBER) {
                                plsqlText.add(" " + argName + "(i) := TO_NUMBER(" + plsqlIndexByPlaceHolderVarName[i] + "(i));");
                            } else if (plsqlIndexByDataType[i] == OracleTypes.RAW) {
                                plsqlText.add(" " + argName + "(i) := HEXTORAW(" + plsqlIndexByPlaceHolderVarName[i] + "(i));");
                            }
                            plsqlText.add("END LOOP;");
                        }
                    }


                } else if (dataType.equalsIgnoreCase("TABLE")
                        || dataType.equalsIgnoreCase("VARRAY")) {
                    if (theRowSet.getString("IN_OUT").equals("IN")
                            || theRowSet.getString("IN_OUT").equals("IN/OUT")) {
                        if (theRecords[extraObjectId[i]].generatedGenericTypeName != null) {

                            plsqlText.add("IF " + n.createName(argName + "_T") + ".COUNT > 0 THEN");
                            plsqlText.add("  " + argName + ".EXTEND(" + n.createName(argName + "_T") + ".COUNT);");
                            plsqlText.add("  FOR i IN " + n.createName(argName + "_T") + ".FIRST.." + n.createName(argName + "_T") + ".LAST LOOP");
                            //plsqlText.add(" "+argName+"(i) := " + argName + "");
                            boolean useWeirdAssign = false;

                            if (theRecords[extraObjectId[i]].typeImplementingClass != null
                                    && theRecords[extraObjectId[i]].typeImplementingClass.isWeirdpackageArrayOfOracleType
                                    && theRecords[extraObjectId[i]].plsqlPackWeirdAssign != null) {
                                useWeirdAssign = true;
                            }

                            if (useWeirdAssign) {
                                for (int z = 0; z < theRecords[extraObjectId[i]].plsqlPackWeirdAssign.length; z++) {
                                    //System.out.println("1 " + z);
                                    if (z == 8) {
                                        // System.out.println("1 " + z);
                                    }
                                    String tempString = JavaUtils.replaceString(theRecords[extraObjectId[i]].plsqlPackWeirdAssign[z], ExtraType.PARAM_TARGET_PARAM_ARRAY_NAME, n.createName(argName + "_T"));
                                    //System.out.println("5");
                                    tempString = JavaUtils.replaceString(tempString, ExtraType.PARAM_TARGET_PARAM_NAME, argName);

                                    //System.out.println("10");

                                    //String tempString = JavaUtils.replaceString(theRecords[extraObjectId[i]].plsqlPackWeirdAssign[z], ExtraType.PARAM_TARGET_PARAM_NAME ,argName );
                                    tempString = JavaUtils.replaceString(tempString, ExtraType.PARAM_TARGET_PARAM_NAME_ATYPE, theRecords[extraObjectId[i]].generatedGenericTypeName);
                                    //System.out.println("20");
                                    tempString = JavaUtils.replaceString(tempString, ExtraType.PARAM_TARGET_PARAM_NAME_REALTYPE, JavaUtils.StripLeadingUsername(theRecords[extraObjectId[i]].oracleName, loginName));
                                    if (theRecords[extraObjectId[i]].typeImplementingClass != null) {
                                        String recordName = theRecords[extraObjectId[i]].typeImplementingClass.typeName;

                                        //System.out.println("30");
                                        if (theRecords[extraObjectId[i]].typeImplementingClass.typeOwner != null) {
                                            if (!theRecords[extraObjectId[i]].typeImplementingClass.typeOwner.equals(loginName)) {
                                                recordName = theRecords[extraObjectId[i]].typeImplementingClass.typeOwner + "." + recordName;

                                            }
                                        }

                                        tempString = JavaUtils.replaceString(tempString, ExtraType.PARAM_TARGET_PARAM_NAME_RTYPE, JavaUtils.StripLeadingUsername(recordName, loginName));
                                    }
                                    plsqlText.add("    " + tempString);
                                    //System.out.println("100");
                                }
                            } else if (theRecords[extraObjectId[i]].plsqlPackAssign != null) {

                                for (int z = 0; z < theRecords[extraObjectId[i]].plsqlPackAssign.length; z++) {
                                    String tempString = JavaUtils.replaceString(theRecords[extraObjectId[i]].plsqlPackAssign[z], ExtraType.PARAM_TARGET_PARAM_ARRAY_NAME, n.createName(argName + "_T"));
                                    tempString = JavaUtils.replaceString(tempString, ExtraType.PARAM_TARGET_PARAM_NAME, argName);
                                    //String tempString = JavaUtils.replaceString(theRecords[extraObjectId[i]].plsqlPackAssign[z], ExtraType.PARAM_TARGET_PARAM_NAME ,argName );
                                    tempString = JavaUtils.replaceString(tempString, ExtraType.PARAM_TARGET_PARAM_NAME_ATYPE, theRecords[extraObjectId[i]].generatedGenericTypeName);
                                    tempString = JavaUtils.replaceString(tempString, ExtraType.PARAM_TARGET_PARAM_NAME_REALTYPE, JavaUtils.StripLeadingUsername(theRecords[extraObjectId[i]].oracleName, loginName));

                                    plsqlText.add("    " + tempString);
                                }
                            } else {
                                plsqlText.add("    " + argName + "(i) := " + n.createName(argName + "_T") + "(i);");
                            }

                            plsqlText.add("  END LOOP;");
                            plsqlText.add("END IF;");
                        }
                    }
                }

            } // end of 2nd pass


/**
 if (isAFunction)
 {
 startString = sfunctionResult + " := ";
 for (int i=1; i < theRowSet.size(); i++)
 {
 theRowSet.setCurrentRowNumber(i);
 if (i == 1)
 {
 qString = theRowSet.getString("ARGUMENT_NAME").toLowerCase();
 }
 else
 {
 qString = qString + ","+theRowSet.getString("ARGUMENT_NAME").toLowerCase();
 }
 }
 }
 else
 {
 for (int i=0; i < theRowSet.size(); i++)
 {
 theRowSet.setCurrentRowNumber(i);
 if (i == 0)
 {
 qString = theRowSet.getString("ARGUMENT_NAME").toLowerCase();
 }
 else
 {
 qString = qString + ","+theRowSet.getString("ARGUMENT_NAME").toLowerCase();
 }

 //if (i%10 == 9)
 //  {
 //  qString = qString + "\n";
 //  }
 }
 }
 ***/

            if (isAFunction) {
                startString = sfunctionResult + " := ";
                for (int i = 1; i < theRowSet.size(); i++) {
                    theRowSet.setCurrentRowNumber(i);
                    if (i == 1) {
                        qString = theRowSet.getString("ARGUMENT_NAME").toLowerCase() + " => " + theRowSet.getString("ARGUMENT_NAME").toLowerCase();
                    } else {
                        qString = qString + "," + theRowSet.getString("ARGUMENT_NAME").toLowerCase() + " => " + theRowSet.getString("ARGUMENT_NAME").toLowerCase();
                    }
                }
            } else {
                for (int i = 0; i < theRowSet.size(); i++) {
                    theRowSet.setCurrentRowNumber(i);
                    if (i == 0) {
                        qString = theRowSet.getString("ARGUMENT_NAME").toLowerCase() + " => " + theRowSet.getString("ARGUMENT_NAME").toLowerCase();
                    } else {
                        qString = qString + "," + theRowSet.getString("ARGUMENT_NAME").toLowerCase() + " => " + theRowSet.getString("ARGUMENT_NAME").toLowerCase();
                    }

                    //if (i%10 == 9)
                    //  {
                    //  qString = qString + "\n";
                    //  }
                }
            }

            if (qString.length() > 0) {
                qString = "(" + qString + ")";
            }

            plsqlText.add("");
            plsqlText.add(new String(startString + oracleName + qString + ";"));

            // Unload variables...

            for (int i = 0; i < theRowSet.size(); i++) {
                theRowSet.setCurrentRowNumber(i);
                String argName = null;

                if (theRowSet.getInt("POSITION") == 0) {
                    argName = sfunctionResult; // KLUGE 012 need to be sure name is unique
                } else {
                    argName = theRowSet.getString("ARGUMENT_NAME").toLowerCase();
                }

                String dataType = theRowSet.getString("DATA_TYPE");

                if (dataType.equalsIgnoreCase("PL/SQL BOOLEAN")) {
                    if (theRowSet.getString("IN_OUT").equals("OUT")
                            || theRowSet.getString("IN_OUT").equals("IN/OUT")) {
                        plsqlText.add(" ");
                        plsqlText.add("IF " + argName + " IS NULL THEN ");
                        plsqlText.add("  " + n.createName(argName + "_SN") + " := 0; ");
                        plsqlText.add("ELSIF " + argName + " = FALSE THEN ");
                        plsqlText.add("  " + n.createName(argName + "_SN") + " := -1; ");
                        plsqlText.add("ELSIF " + argName + " = TRUE THEN ");
                        plsqlText.add("  " + n.createName(argName + "_SN") + " := 1; ");
                        plsqlText.add("END IF; ");
                        plsqlText.add(" ");
                    } // out
                } // bool
                else if (dataType.equalsIgnoreCase("TABLE")
                        || dataType.equalsIgnoreCase("VARRAY")) {
                    if (theRowSet.getString("IN_OUT").equals("OUT")
                            || theRowSet.getString("IN_OUT").equals("IN/OUT")) {

                        if (theRecords[extraObjectId[i]].generatedGenericTypeName != null) {

                            plsqlText.add(" ");
                            if (theRowSet.getString("IN_OUT").equals("IN/OUT")) {
                                plsqlText.add(n.createName(argName + "_T") + ".DELETE;");
                            }
                            plsqlText.add("IF " + argName + ".COUNT > 0 THEN");
                            plsqlText.add("  " + n.createName(argName + "_T") + ".EXTEND(" + argName + ".COUNT);");
                            plsqlText.add("  FOR i IN " + argName + ".FIRST.." + argName + ".LAST LOOP");
                            if (theRecords[extraObjectId[i]].plsqlPackUnassign != null) {
                                for (int z = 0; z < theRecords[extraObjectId[i]].plsqlPackUnassign.length; z++) {
                                    //String tempString = JavaUtils.replaceString(theRecords[extraObjectId[i]].plsqlPackUnassign[z], ExtraType.PARAM_TARGET_PARAM_NAME ,argName );
                                    //String tempString = JavaUtils.replaceString(theRecords[extraObjectId[i]].plsqlPackAssign[z], ExtraType.PARAM_TARGET_PARAM_ARRAY_NAME ,n.createName(argName + "_T") );
                                    //tempString = JavaUtils.replaceString(tempString, ExtraType.PARAM_TARGET_PARAM_NAME ,argName );


                                    //String tempString = JavaUtils.replaceString(theRecords[extraObjectId[i]].plsqlPackAssign[z], ExtraType.PARAM_TARGET_PARAM_ARRAY_NAME ,n.createName(argName + "_T") );

                                    String tempString = JavaUtils.replaceString(theRecords[extraObjectId[i]].plsqlPackUnassign[z], ExtraType.PARAM_TARGET_PARAM_ARRAY_NAME, n.createName(argName + "_T"));
                                    tempString = JavaUtils.replaceString(tempString, ExtraType.PARAM_TARGET_PARAM_NAME, argName);

                                    tempString = JavaUtils.replaceString(tempString, ExtraType.PARAM_TARGET_PARAM_NAME, argName);
                                    tempString = JavaUtils.replaceString(tempString, ExtraType.PARAM_TARGET_PARAM_NAME_ATYPE, theRecords[extraObjectId[i]].generatedGenericTypeName);
                                    tempString = JavaUtils.replaceString(tempString, ExtraType.PARAM_TARGET_PARAM_NAME_RTYPE, theRecords[extraObjectId[i]].generatedGenericTypeName.substring(0, theRecords[extraObjectId[i]].generatedGenericTypeName.length() - 2) + "_T");
                                    tempString = JavaUtils.replaceString(tempString, ExtraType.PARAM_TARGET_PARAM_NAME_REALTYPE, JavaUtils.StripLeadingUsername(theRecords[extraObjectId[i]].oracleName, loginName));
                                    plsqlText.add("  " + tempString);
                                }
                            } else {
                                plsqlText.add(" " + n.createName(argName + "_T") + "(i) := " + argName + "(i);");
                            }
                            plsqlText.add("  END LOOP;");
                            plsqlText.add("END IF;");
                        }
                    }
                } else if (dataType.equalsIgnoreCase("PL/SQL TABLE")
                        && oracleParamDatatype[i].equals("SqlUtils.ORACLE_PLSQL_INDEXBY_ROWTYPE_DATATYPE")) {
                    if (theRowSet.getString("IN_OUT").equals("OUT")
                            || theRowSet.getString("IN_OUT").equals("IN/OUT")) {

                        if (theRecords[extraObjectId[i]].generatedGenericTypeName != null) {

                            plsqlText.add(" ");
                            if (theRowSet.getString("IN_OUT").equals("IN/OUT")) {
                                plsqlText.add(n.createName(argName + "_T") + ".DELETE;");
                            }
                            plsqlText.add("IF " + argName + ".COUNT > 0 THEN");
                            plsqlText.add("  " + n.createName(argName + "_T") + ".EXTEND(" + argName + ".COUNT);");
                            plsqlText.add("  FOR i IN " + argName + ".FIRST.." + argName + ".LAST LOOP");
                            plsqlText.add("   IF " + argName + ".EXISTS(i) THEN");
                            if (theRecords[extraObjectId[i]].plsqlPackUnassign != null) {
                                for (int z = 0; z < theRecords[extraObjectId[i]].plsqlPackUnassign.length; z++) {
                                    //String tempString = JavaUtils.replaceString(theRecords[extraObjectId[i]].plsqlPackUnassign[z], ExtraType.PARAM_TARGET_PARAM_NAME ,argName );
                                    String tempString = JavaUtils.replaceString(theRecords[extraObjectId[i]].plsqlPackUnassign[z], ExtraType.PARAM_TARGET_PARAM_ARRAY_NAME, n.createName(argName + "_T"));
                                    tempString = JavaUtils.replaceString(tempString, ExtraType.PARAM_TARGET_PARAM_NAME, argName);
                                    tempString = JavaUtils.replaceString(tempString, ExtraType.PARAM_TARGET_PARAM_NAME_ATYPE, theRecords[extraObjectId[i]].generatedGenericTypeName);
                                    tempString = JavaUtils.replaceString(tempString, ExtraType.PARAM_TARGET_PARAM_NAME_RTYPE, theRecords[extraObjectId[i]].generatedGenericTypeName.substring(0, theRecords[extraObjectId[i]].generatedGenericTypeName.length() - 2) + "_T");
                                    tempString = JavaUtils.replaceString(tempString, ExtraType.PARAM_TARGET_PARAM_NAME_REALTYPE, JavaUtils.StripLeadingUsername(theRecords[extraObjectId[i]].oracleName, loginName));
                                    plsqlText.add("    " + tempString);
                                }
                            } else {
                                plsqlText.add("     " + n.createName(argName + "_T") + "(i) := " + argName + "(i);");
                            }
                            plsqlText.add("   END IF;");
                            plsqlText.add("  END LOOP;");
                            plsqlText.add("END IF;");
                        }
                    }
                } else if (dataType.equalsIgnoreCase("PL/SQL TABLE")) {
                    if (theRowSet.getString("IN_OUT").equals("OUT")
                            || theRowSet.getString("IN_OUT").equals("IN/OUT")) {

                        if (plsqlIndexByDataType[i] == OracleTypes.VARCHAR
                                || (plsqlIndexByDataType[i] == OracleTypes.NUMBER && plsqlIndexByRealDataType[i] == oracle.jdbc.OracleTypes.NUMBER)) {
                        } else {
                            plsqlText.add(" ");
                            if (theRowSet.getString("IN_OUT").equals("IN/OUT")) {
                                plsqlText.add(plsqlIndexByPlaceHolderVarName[i] + ".DELETE;");
                            }

                            plsqlText.add("FOR i IN " + argName + ".FIRST.." + argName + ".LAST LOOP");
                            if (plsqlIndexByDataType[i] == OracleTypes.DATE) {
                                //        +" IF P2.exists(i) THEN  P2_v(i) :=  TO_CHAR(P2(i),'yyyy-mm-dd hh24:mi:ss'); END IF;\n" // 31

                                plsqlText.add(" IF " + argName + ".exists(i) THEN " + plsqlIndexByPlaceHolderVarName[i] + "(i) := TO_CHAR(" + argName + "(i),'" + PlsqlIndexByTable2.ORACLE_DATE_TO_CHAR_MASK + "'); END IF;");
                            }
                            if (plsqlIndexByDataType[i] == OracleTypes.TIMESTAMP) {
                                plsqlText.add(" IF " + argName + ".exists(i) THEN " + plsqlIndexByPlaceHolderVarName[i] + "(i) := TO_CHAR(" + argName + "(i),'" + PlsqlIndexByTable2.ORACLE_TIMESTAMP_TO_CHAR_MASK + "'); END IF;");
                            } else if (plsqlIndexByDataType[i] == OracleTypes.TIMESTAMPTZ
                                    || plsqlIndexByDataType[i] == OracleTypes.TIMESTAMPLTZ) {
                                // The same mask both ways, LOCAL included. A LOCAL timestamp keeps
                                // no zone of its own -- it is held in the session's -- so TZR here
                                // renders the SESSION zone rather than the caller's. That is worth
                                // emitting rather than omitting: the wall clock alone does not say
                                // which zone it is a wall clock in, and a reader who gets 'GMT'
                                // back knows. Verified on 12c and 23ai that TO_CHAR accepts TZR for
                                // both types; it does not error on LOCAL.
                                plsqlText.add(" IF " + argName + ".exists(i) THEN " + plsqlIndexByPlaceHolderVarName[i] + "(i) := TO_CHAR(" + argName + "(i),'" + PlsqlIndexByTable2.ORACLE_TIMESTAMPTZ_TO_CHAR_MASK + "'); END IF;");
                            } else if (plsqlIndexByDataType[i] == OracleTypes.NUMBER) {
                                int actualPlaces = plsqlIndexByDataDecPlaces[i];
                                int actualLength = plsqlIndexByDataLength[i] - actualPlaces;

                                if (actualPlaces < 0) {
                                    actualPlaces = 0;
                                }

                                // .exists(i), like every other arm in this loop. This one was the
                                // LAST to get it and is the one most likely to have bitten someone:
                                // a NUMBER index-by only reaches this conversion at all when it does
                                // NOT ride the numeric slot -- a high-precision number(30,15) does
                                // -- so it is an ordinary type, unlike the RAW that led here.
                                // Without the guard a sparse OUT collection raises NO_DATA_FOUND
                                // from inside the emitted block, naming neither the parameter nor
                                // the gap. See IBA_TEST.TEST_SPARSE and TSparseIndexBy.
                                if (actualPlaces <= 0) {
                                    plsqlText.add(" IF " + argName + ".exists(i) THEN " + plsqlIndexByPlaceHolderVarName[i] + "(i) := LTRIM(TO_CHAR(" + argName + "(i),'"
                                            + ORACLE_NINES.substring(0, actualLength) + "')); END IF;");
                                } else {
                                    plsqlText.add(" IF " + argName + ".exists(i) THEN " + plsqlIndexByPlaceHolderVarName[i] + "(i) := LTRIM(TO_CHAR(" + argName + "(i),'"
                                            + ORACLE_NINES.substring(0, actualLength)
                                            + "."
                                            + ORACLE_NINES.substring(0, actualPlaces) + "')); END IF;");
                                }
                            } else if (plsqlIndexByDataType[i] == OracleTypes.RAW) {
                                // .exists(i), like the DATE / TIMESTAMP / zoned arms above -- and
                                // UNLIKE this one until 2026-08-26. An index-by table is sparse by
                                // nature: PL/SQL lets a routine assign p_out(1) and p_out(7) and
                                // nothing between, while this loop walks FIRST..LAST, so reading a
                                // missing index raises NO_DATA_FOUND from inside the emitted block
                                // -- an error naming neither the parameter nor the gap.
                                //
                                // It survived because nothing reached it. No fixture returns a
                                // sparse RAW collection, and until the same day as this comment a
                                // RAW index-by could not be published as an MCP tool at all, so the
                                // only caller was a hand-written DAO client whose author had read
                                // the routine. Found by reading the three sibling arms rather than
                                // by a failure, which is why the fix arrives without a test that
                                // fails before it: writing one needs a fixture with a deliberately
                                // sparse OUT collection, and generic_testd's test_raw does
                                // p_out := p_in on a dense input.
                                plsqlText.add(" IF " + argName + ".exists(i) THEN " + plsqlIndexByPlaceHolderVarName[i] + "(i) := RAWTOHEX(" + argName + "(i)); END IF;");
                            }
                            plsqlText.add("END LOOP;");
                        }

                    }
                }
            } // for rowset

            int startUnload = 0;

            for (int i = startUnload; i < theRowSet.size(); i++) {
                theRowSet.setCurrentRowNumber(i);
                String argName = null;

                if (theRowSet.getInt("POSITION") == 0) {
                    argName = sfunctionResult; // KLUGE 012 need to be sure name is unique
                } else {
                    argName = theRowSet.getString("ARGUMENT_NAME").toLowerCase();
                }

                if (paramOutId[i] > 0) {
                    String dataType = theRowSet.getString("DATA_TYPE");

                    if (dataType.equalsIgnoreCase("PL/SQL BOOLEAN")) {
                        plsqlText.add("? := " + n.createName(argName + "_SN") + ";");
                    } else if (dataType.equalsIgnoreCase("PL/SQL TABLE")
                            && oracleParamDatatype[i].equals("SqlUtils.ORACLE_PLSQL_INDEXBY_ROWTYPE_DATATYPE")) {
                        if (theRecords[extraObjectId[i]].generatedGenericTypeName != null) {
                            plsqlText.add("? := " + n.createName(argName + "_T") + ";");
                        } else {
                            plsqlText.add("? := " + argName + ";");
                        }
                    } else if (dataType.equalsIgnoreCase("TABLE")
                            || dataType.equalsIgnoreCase("VARRAY")) {
                        if (theRecords[extraObjectId[i]].generatedGenericTypeName != null) {
                            plsqlText.add("? := " + n.createName(argName + "_T") + ";");
                        } else {
                            plsqlText.add("? := " + argName + ";");
                        }
                    } else if (dataType.equalsIgnoreCase("PL/SQL TABLE")) {
                        if (plsqlIndexByDataType[i] == OracleTypes.VARCHAR
                                || (plsqlIndexByDataType[i] == OracleTypes.NUMBER && plsqlIndexByRealDataType[i] == oracle.jdbc.OracleTypes.NUMBER)) {
                            plsqlText.add("? := " + argName + ";");
                        } else {
                            plsqlText.add("? := " + plsqlIndexByPlaceHolderVarName[i] + ";");
                        }
                    } else if (dataType.equalsIgnoreCase("PL/SQL RECORD")
                            || dataType.equalsIgnoreCase("OBJECT")) {
                        if (extraObjectId[i] < 0) {
                            theLog.error("Unable to find java class for " + dataType + " " + argName);
                        } else {
                            // Item 7: recurse so a nested PL/SQL RECORD field flattens to leaf scalars.
                            emitRecordOutAssigns(argName, theRecords[extraObjectId[i]], plsqlText, n);
                        }
                    } // if record
                    else {
                        plsqlText.add("? := " + argName + ";");
                    }
                }
            } // for

            plsqlText.add(new String("END;"));

        } catch (CSNoDataInRowSetException e) {
            qString = "";
        } catch (CSException e) {
            theLog.syserror(e, true, true);
        }

        plsqlText.trimToSize();
        String[] tempString = new String[plsqlText.size()];
        for (int i = 0; i < tempString.length; i++) {
            tempString[i] = (String) plsqlText.get(i);
        }

        return (tempString);
    }


    private void addObjectScalerSetMethod(JavaChunk theJavaChunk, String setMethod, String castee, String castMethod, String argName, String variableDataType, String variableName, boolean comments) {

        theJavaChunk.print(" ");

        if (comments) {
            theJavaChunk.print("/**");
            theJavaChunk.print("* Method to set parameter " + argName);
            theJavaChunk.print("* @param " + variableDataType + " " + variableName);
            theJavaChunk.print("*/");
        }

        theJavaChunk.print("public void "
                + setMethod + "(" + variableDataType
                + " " + variableName + ")");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        //theJavaChunk.print("this." + variableName + " = new " + castee  + "(" + variableName + castMethod +");" );
        if (castMethod == null || castMethod.equals("") || castMethod.equals(".booleanValue()") || castee.equalsIgnoreCase("Boolean")) {
            theJavaChunk.print("this." + variableName + " = " + variableName + ";");
        } else {
            theJavaChunk.print("if (" + variableName + " == null)");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("this." + variableName + " = null;");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
            theJavaChunk.print("else");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("this." + variableName + " = new " + castee + "(" + variableName + castMethod + ");");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
        }
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
    }

    private void addObjectScalerSetMethodDowncast(JavaChunk theJavaChunk, String setMethod, String castee, String castMethod, String argName, String variableDataType, String variableName, boolean comments) {

        theJavaChunk.print(" ");

        if (comments) {
            theJavaChunk.print("/**");
            theJavaChunk.print("* Method to set parameter " + argName);
            theJavaChunk.print("* @param " + variableDataType + " " + variableName);
            theJavaChunk.print("*/");
        }

        theJavaChunk.print("public void "
                + setMethod + "(" + variableDataType
                + " " + variableName + ")");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        //theJavaChunk.print("this." + variableName + " = new " + castee  + "(" + variableName + castMethod +");" );
        if (castMethod == null || castMethod.equals("")) {
            theJavaChunk.print("this." + variableName + " = " + variableName + ";");
        } else {
            theJavaChunk.print("if (" + variableName + " == null)");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("this." + variableName + " = null;");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
            theJavaChunk.print("else");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("this." + variableName + " =  (" + castee + ")" + variableName + ";");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
        }
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
    }


    private void addObjectScalerSetMethodPLSQLIBTable(JavaChunk theJavaChunk, String setMethod, String argName, String variableDataType, String variableName, boolean comments, int realDataType) {

        String realDataTypeString = "" + realDataType;

        if (realDataType == oracle.jdbc.OracleTypes.NUMBER) {
            realDataTypeString = "oracle.jdbc.OracleTypes.NUMBER";
        } else {
            realDataTypeString = "oracle.jdbc.OracleTypes.VARCHAR";
        }

        theJavaChunk.print(" ");

        if (comments) {
            theJavaChunk.print("/**");
            theJavaChunk.print("* Method to set PL/SQL INDEX BY parameter " + argName);
            theJavaChunk.print("* @param " + "com.mcpdbwizard.pub.PlsqlIndexByTable2" + " " + variableName + " An Object[] containing numbers or Strings.");
            theJavaChunk.print("*/");
        }

        theJavaChunk.print("public void "
                + setMethod + "(com.mcpdbwizard.pub.PlsqlIndexByTable2"
                + " " + variableName + ")");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("this." + variableName + " = " + variableName + ";");
        theJavaChunk.print("this." + variableName + ".setRealDataType(" + realDataTypeString + ");");


        theJavaChunk.print("}");
        theJavaChunk.unIndent();
    }

    private void addDateSetMethods(JavaChunk theJavaChunk, JavaChunk theJavaAttrsChunk, String setMethod, String argName, String variableDataType, String variableName, boolean comments) {

        theJavaAttrsChunk.print(" ");

        if (comments) {
            theJavaAttrsChunk.print("/**");
            theJavaAttrsChunk.print("* Method to set parameter " + argName);
            theJavaAttrsChunk.print("* @param " + variableDataType + " " + variableName);
            theJavaAttrsChunk.print("*/");
        }

        theJavaAttrsChunk.print("public void "
                + setMethod + "(" + "java.util.Date"
                + " " + variableName + ")");
        theJavaAttrsChunk.indent();
        theJavaAttrsChunk.print("{");

        theJavaAttrsChunk.print("if (" + variableName + " == null)");
        theJavaAttrsChunk.indent();
        theJavaAttrsChunk.print("{");
        theJavaAttrsChunk.print("this." + variableName + " = null;");
        theJavaAttrsChunk.print("}");
        theJavaAttrsChunk.unIndent();
        theJavaAttrsChunk.print("else");
        theJavaAttrsChunk.indent();
        theJavaAttrsChunk.print("{");
        theJavaAttrsChunk.print("this." + variableName + " = new java.util.Date(" + variableName + ".getTime());");
        theJavaAttrsChunk.print("}");
        theJavaAttrsChunk.unIndent();

        theJavaAttrsChunk.print("}");
        theJavaAttrsChunk.unIndent();

        theJavaChunk.print(" ");

        if (comments) {
            theJavaChunk.print("/**");
            theJavaChunk.print("* Method to set parameter " + argName);
            theJavaChunk.print("* @param " + "long" + " " + variableName);
            theJavaChunk.print("*/");
        }

        theJavaChunk.print("public void "
                + setMethod + "(" + "long"
                + " " + variableName + ")");
        theJavaChunk.indent();

        theJavaChunk.print("{");
        theJavaChunk.print("this." + variableName + " = new java.util.Date(" + variableName + ");");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print(" ");

        if (comments) {
            theJavaChunk.print("/**");
            theJavaChunk.print("* Method to set parameter " + argName);
            theJavaChunk.print("* @param " + "java.sql.Timestamp" + " " + variableName);
            theJavaChunk.print("*/");
        }

        theJavaChunk.print("public void "
                + setMethod + "(" + "java.sql.Timestamp"
                + " " + variableName + ")");
        theJavaChunk.indent();
        //theJavaChunk.print("{");

        theJavaChunk.print("{");
        theJavaChunk.print("if (" + variableName + " == null)");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("this." + variableName + " = null;");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print("else");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        // theJavaChunk.print("this." + variableName + " = new java.sql.Timestamp(" + variableName + ".getTime());" );
        theJavaChunk.print("this." + variableName + " = new java.util.Date(" + variableName + ".getTime());");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();

        theJavaChunk.print("}");
        theJavaChunk.unIndent();

        // A String overload, parsed as a Timestamp. The row class stores a TIMESTAMP column as a
        // String ("yyyy-mm-dd hh:mm:ss.fffffffff"), and the manager binds it through this
        // date-token setParam; without this overload a TIMESTAMP table column will not compile.
        // (The row class already has a matching setRow(String).) Date columns keep binding through
        // the java.util.Date overload; this is purely an additional overload.
        theJavaChunk.print(" ");

        if (comments) {
            theJavaChunk.print("/**");
            theJavaChunk.print("* Method to set parameter " + argName);
            theJavaChunk.print("* @param " + "String" + " " + variableName + " in the format \"yyyy-mm-dd hh:mm:ss.fffffffff\"");
            theJavaChunk.print("*/");
        }

        theJavaChunk.print("public void " + setMethod + "(String " + variableName + ")");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("if (" + variableName + " == null)");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("this." + variableName + " = null;");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print("else");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("this." + variableName + " = new java.util.Date(java.sql.Timestamp.valueOf(" + variableName + ").getTime());");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
    }

    private void addScalarSetNullMethod(JavaChunk theJavaChunk, String setMethod, String argName, String variableName, boolean comments) {

        theJavaChunk.print(" ");

        if (comments) {
            theJavaChunk.print("/**");
            theJavaChunk.print("* Method to set parameter " + argName + " to null");
            theJavaChunk.print("*/");
        }

        theJavaChunk.print("public void " + setMethod + "()");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("this." + variableName + " = null;");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        //Double paramLongp;
    }

    private void addScalarSetMethod(JavaChunk theJavaChunk, String setMethod, String castee, String castMethod, String argName, String variableDataType, String variableName, boolean comments) {
        addScalarSetMethod(theJavaChunk, setMethod, castee, castMethod, argName, variableDataType, variableName, comments, "public");
    }

    private void addScalarSetMethod(JavaChunk theJavaChunk, String setMethod, String castee, String castMethod, String argName, String variableDataType, String variableName, boolean comments, String accessMethod) {
        String throwsString = "";

        if (variableDataType.equals("oracle.sql.TIMESTAMPTZ")
                || variableDataType.equals("oracle.sql.TIMESTAMPLTZ")
                || variableDataType.equals("oracle.sql.TIMESTAMP")
                || variableDataType.equals("oracle.sql.INTERVALDS")
                || variableDataType.equals("oracle.sql.INTERVALYM")
        ) {
            throwsString = "throws com.mcpdbwizard.pub.CSException";
        }

        theJavaChunk.print(" ");

        if (comments) {
            theJavaChunk.print("/**");
            theJavaChunk.print("* Method to set parameter " + argName);
            theJavaChunk.print("* @param " + variableDataType + " " + variableName);
            if (throwsString.length() > 0) {
                theJavaChunk.print("* @" + throwsString);
            }
            theJavaChunk.print("*/");
        }

        theJavaChunk.print(accessMethod + " void "
                + setMethod + "(" + variableDataType
                + " " + variableName + ") " + throwsString);
        theJavaChunk.indent();
        theJavaChunk.print("{");
        if (variableDataType.endsWith("[]")
                || variableDataType.startsWith("java.io.Fil")) {
            theJavaChunk.print("this." + variableName + " = " + variableName + ";");
        } else if (castee.equals("Boolean")) {
            theJavaChunk.print("this." + variableName + " = " + variableName + ";");
        } else if (variableDataType.equals("java.sql.Timestamp")) {
            theJavaChunk.print("if (" + variableName + " != null)");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("this." + variableName + " = " + variableName + ".toString();");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
            theJavaChunk.print("else");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("this." + variableName + " = null;");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
        } else if (variableDataType.equals("oracle.sql.TIMESTAMP")) {
            theJavaChunk.print("if (" + variableName + " != null)");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("try");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("this." + variableName + " = " + variableName + ".timestampValue().toString();");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
            theJavaChunk.print("catch (Exception e)");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("throw (new com.mcpdbwizard.pub.CSException(\"" + setMethod + " threw Exception while trying to process " + variableDataType + ": \"+ e.getMessage()));");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
            theJavaChunk.print("else");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("this." + variableName + " = null;");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
        } else if (variableDataType.equals("oracle.sql.INTERVALYM")) {
            theJavaChunk.print("if (" + variableName + " != null)");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("this." + variableName + " = " + variableName + ".toString().trim();");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
            theJavaChunk.print("else");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("this." + variableName + " = null;");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
        } else if (variableDataType.equals("oracle.sql.INTERVALDS")) {
            theJavaChunk.print("if (" + variableName + " != null)");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("this." + variableName + " = " + variableName + ".toString().trim();");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
            theJavaChunk.print("else");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("this." + variableName + " = null;");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
        } else if (variableDataType.equals("oracle.sql.TIMESTAMPTZ")) {
            theJavaChunk.print("try");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            if (targetVersion.startsWith("8") || targetVersion.startsWith("9")) {
                theJavaChunk.print("if (" + variableName + " != null && " + variableName + ".getLength() > 0)");
            } else {
                theJavaChunk.print("if (" + variableName + " != null)");
            }
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("this." + variableName + " = " + variableName + ".toBytes();");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
            theJavaChunk.print("else");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("this." + variableName + " = null;");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
            theJavaChunk.print("catch (Exception e)");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            //theJavaChunk.print( stheLog + ".error(" + "\"" + setMethod + " threw Exception while trying to process " + variableDataType +"\");" );
            //theJavaChunk.print( stheLog + ".error(e);" );
            theJavaChunk.print("throw (new com.mcpdbwizard.pub.CSException(\"" + setMethod + " threw Exception while trying to process " + variableDataType + ": \" + e.getMessage()));");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();

        } else if (variableDataType.equals("oracle.sql.TIMESTAMPLTZ")) {
            theJavaChunk.print("try");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            if (targetVersion.startsWith("8") || targetVersion.startsWith("9")) {
                theJavaChunk.print("if (" + variableName + " != null && " + variableName + ".getLength() > 0)");
            } else {
                theJavaChunk.print("if (" + variableName + " != null)");
            }
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("this." + variableName + " = " + variableName + ".toBytes();");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
            theJavaChunk.print("else");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("this." + variableName + " = null;");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
            theJavaChunk.print("catch (Exception e)");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("throw (new com.mcpdbwizard.pub.CSException(\"" + setMethod + " threw Exception while trying to process " + variableDataType + ": \"+ e.getMessage()));");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();


        } else if (castMethod.equals(variableDataType)) {
            theJavaChunk.print("this." + variableName + " = new " + castee + "(" + variableName + ");");
        } else {
            theJavaChunk.print("this." + variableName + " = new " + castee + "(" + "(" + castMethod + ")" + variableName + ");");
        }
        theJavaChunk.print("}");
        theJavaChunk.unIndent();


        // Add extra string set method if appopriate
        if (
                (variableDataType.equals("oracle.sql.TIMESTAMP")
                        || variableDataType.equals("oracle.sql.TIMESTAMPTZ")
                        || variableDataType.equals("oracle.sql.TIMESTAMPLTZ")
                        || variableDataType.equals("oracle.sql.INTERVALDS")
                        || variableDataType.equals("oracle.sql.INTERVALYM"))
                        && accessMethod.equals("public")) // String set methods not needed if this is private i.e. OUT only
        {

            String connectionString = "";
            if (variableDataType.equals("oracle.sql.TIMESTAMPTZ")
                    || variableDataType.equals("oracle.sql.TIMESTAMPLTZ")
            ) {
                connectionString = "java.sql.Connection paramConnection, ";
            }


            if (variableDataType.equals("oracle.sql.TIMESTAMP")) {
                throwsString = "throws com.mcpdbwizard.pub.CSException";
            } else if (variableDataType.equals("oracle.sql.TIMESTAMPTZ")) {
                throwsString = "throws com.mcpdbwizard.pub.CSException";
            } else if (variableDataType.equals("oracle.sql.TIMESTAMPLTZ")) {
                throwsString = "throws com.mcpdbwizard.pub.CSException";
            }

            theJavaChunk.print(" ");

            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* Method to set parameter " + argName);

                if (variableDataType.equals("oracle.sql.TIMESTAMPTZ")) {
                    theJavaChunk.print("* This method requires a Connection in order to work");
                    theJavaChunk.print("* @param Connection paramConnection");
                    theJavaChunk.print("* @param String " + variableName + " a String in the format \"yyyy-mm-dd hh:mm:ss.fffffffff timezone\",");
                    theJavaChunk.print("* for example \"2005-1-5 19:51:18.582000000 Europe/London\"");
                } else if (variableDataType.equals("oracle.sql.TIMESTAMPLTZ")) {
                    theJavaChunk.print("* This method requires a Connection in order to work");
                    theJavaChunk.print("* @param Connection paramConnection");
                    theJavaChunk.print("* @param String " + variableName + " a String in the format \"yyyy-mm-dd hh:mm:ss.fffffffff\"");
                } else if (variableDataType.equals("oracle.sql.TIMESTAMP")) {
                    theJavaChunk.print("* @param String " + variableName + " a String in the format \"yyyy-mm-dd hh:mm:ss.fffffffff\"");
                }

                theJavaChunk.print("* @throws CSException if the String can't be turned into a " + variableDataType);
                theJavaChunk.print("*/");
            }

            theJavaChunk.print(accessMethod + " void "
                    + setMethod + "(" + connectionString + "String"
                    + " " + variableName + ") " + throwsString);
            theJavaChunk.indent();
            theJavaChunk.print("{");

            if (variableDataType.equals("oracle.sql.TIMESTAMPTZ")
                    || variableDataType.equals("oracle.sql.TIMESTAMPLTZ")) {
                theJavaChunk.print("try");
                theJavaChunk.indent();
                theJavaChunk.print("{");
                theJavaChunk.print(setMethod + "(new " + variableDataType + "(paramConnection, " + variableName + "));");
                theJavaChunk.print("}");
                theJavaChunk.unIndent();
                if (comments)
                    theJavaChunk.print("// The constuctor for " + variableDataType + " can throw a variety of exceptions including");
                if (comments)
                    theJavaChunk.print("// NullPointerException and IllegalArgumentException if it can't parse the String");
                theJavaChunk.print("catch (Exception e)");
                theJavaChunk.indent();
                theJavaChunk.print("{");
                theJavaChunk.print("throw (new com.mcpdbwizard.pub.CSException(\"" + setMethod + " threw Exception while trying to turn '\"  + " + variableName + " + \"' into " + variableDataType + ". Error is: \" + e.getMessage()));");
                theJavaChunk.print("}");
                theJavaChunk.unIndent();
            } else if (variableDataType.equals("oracle.sql.TIMESTAMP")) {
                theJavaChunk.print("try");
                theJavaChunk.indent();
                theJavaChunk.print("{");
                theJavaChunk.print("if (" + variableName + " == null)");
                theJavaChunk.indent();
                theJavaChunk.print("{");
                theJavaChunk.print("this." + variableName + " = null;");
                theJavaChunk.print("}");
                theJavaChunk.unIndent();
                theJavaChunk.print("else");
                theJavaChunk.indent();
                theJavaChunk.print("{");
                //if (comments) theJavaChunk.print("// ");
                theJavaChunk.print(setMethod + "(java.sql.Timestamp.valueOf(" + variableName + "));");
                theJavaChunk.print("}");
                theJavaChunk.unIndent();
                theJavaChunk.print("}");
                theJavaChunk.unIndent();
                if (comments)
                    theJavaChunk.print("// The constuctor for " + variableDataType + " often throws a NullPointerException if it can't parse the String");
                theJavaChunk.print("catch (Exception e)");
                theJavaChunk.indent();
                theJavaChunk.print("{");
                theJavaChunk.print("throw (new com.mcpdbwizard.pub.CSException(\"" + setMethod + " threw Exception while trying to turn '\"  + " + variableName + " + \"' into " + variableDataType + ". Error is: \" + e.getMessage()));");
                theJavaChunk.print("}");
                theJavaChunk.unIndent();
            } else {
                theJavaChunk.print("try");
                theJavaChunk.indent();
                theJavaChunk.print("{");
                theJavaChunk.print("if (" + variableName + " == null)");
                theJavaChunk.indent();
                theJavaChunk.print("{");
                theJavaChunk.print("this." + variableName + " = null;");
                theJavaChunk.print("}");
                theJavaChunk.unIndent();
                theJavaChunk.print("else");
                theJavaChunk.indent();
                theJavaChunk.print("{");
                //if (comments) theJavaChunk.print("// ");
                theJavaChunk.print(setMethod + "(new " + variableDataType + "(" + variableName + "));");
                theJavaChunk.print("}");
                theJavaChunk.unIndent();
                theJavaChunk.print("}");
                theJavaChunk.unIndent();
                if (comments)
                    theJavaChunk.print("// The constuctor for " + variableDataType + " often throws a NullPointerException if it can't parse the String");
                theJavaChunk.print("catch (Exception e)");
                theJavaChunk.indent();
                theJavaChunk.print("{");
                theJavaChunk.print("throw (new com.mcpdbwizard.pub.CSException(\"" + setMethod + " threw Exception while trying to turn '\"  + " + variableName + " + \"' into " + variableDataType + ". Error is: \" + e.getMessage()));");
                theJavaChunk.print("}");
                theJavaChunk.unIndent();
            }

            theJavaChunk.print("}");
            theJavaChunk.unIndent();

        }
    }

    private void addFileSetMethod(JavaChunk theJavaChunk, JavaChunk theJavaAttrsChunk, String setMethod, String argName
            , String variableDataType, String variableName
            , String otherName, String inOut, boolean comments, String actualOracleDataType
            , String byteArrayName) {

        String byteArraySetMethod = setMethod;
        if (javaNamingConvention.equals("spaces_between_words.java")) {
            byteArraySetMethod = byteArraySetMethod + "_byte_array";
        } else {
            byteArraySetMethod = byteArraySetMethod + "ByteArray";
        }

        theJavaChunk.print(" ");

        if (comments) {
            theJavaChunk.print("/**");
            theJavaChunk.print("* Method to set parameter " + argName);
            theJavaChunk.print("* @param " + variableDataType + " " + variableName);
            theJavaChunk.print("*/");
        }

        theJavaChunk.print("public void "
                + setMethod + "(" + variableDataType
                + " " + variableName + ")");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("this." + variableName + " = " + variableName + ";");

        if (inOut.equals("OUT") || inOut.equals("IN/OUT")) {
            theJavaChunk.print("");
            theJavaChunk.print("if (" + variableName + " == null) ");
            theJavaChunk.indent();
            theJavaChunk.print("{                          ");
            theJavaChunk.print(otherName + " = null; ");
            theJavaChunk.print("}    ");
            theJavaChunk.unIndent();
            theJavaChunk.print("else ");
            theJavaChunk.indent();
            theJavaChunk.print("{ ");
            theJavaChunk.print(otherName + " = this." + variableName + ".getAbsolutePath();");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
        }

        theJavaChunk.print("}");
        theJavaChunk.unIndent();

        if (actualOracleDataType.equals("CLOB") && type == IS_A_FUNCTION) {
            theJavaAttrsChunk.print(" ");

            if (comments) {
                theJavaAttrsChunk.print("/**");
                theJavaAttrsChunk.print("* Method to set parameter " + argName);
                theJavaAttrsChunk.print("* @param " + "String" + " " + variableName);
                theJavaAttrsChunk.print("* @since 5.0.2314");
                theJavaAttrsChunk.print("* @throws com.mcpdbwizard.pub.CSException if the String can not be turned into a temporary file");
                theJavaAttrsChunk.print("*/");
            }

            theJavaAttrsChunk.print("public void "
                    + setMethod + "(" + "String"
                    + " " + variableName + ") throws com.mcpdbwizard.pub.CSException");
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print("if (useByteArraysForLongsAndLOBS)");
            ;
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            if (useCharForCLOB) {
                theJavaAttrsChunk.print(byteArrayName + " = " + variableName + ".toCharArray();");
            } else {
                theJavaAttrsChunk.print(byteArrayName + " = " + variableName + ".getBytes();");
            }
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
            theJavaAttrsChunk.print("else");
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print("java.io.File tempFile = com.mcpdbwizard.pub.IOUtils.loadStringIntoFile(" + variableName + ", " + stempFilePrefix + ", " + stempFileSuffix + ", " + stempFileDir + ".getAbsolutePath(), " + stheLog + ");");
            theJavaAttrsChunk.print(setMethod + "(tempFile);");
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
            theJavaAttrsChunk.print(" ");

            if (comments) {
                theJavaAttrsChunk.print("/**");
                theJavaAttrsChunk.print("* Method to set parameter " + argName);
                if (useCharForCLOB) {
                    theJavaAttrsChunk.print("* @param " + "char[]" + " " + variableName);
                } else {
                    theJavaAttrsChunk.print("* @param " + "byte[]" + " " + variableName);
                }
                theJavaAttrsChunk.print("* @since 5.0.2314");
                theJavaAttrsChunk.print("* @throws com.mcpdbwizard.pub.CSException if the String can not be turned into a temporary file");
                theJavaAttrsChunk.print("*/");
            }

            if (useCharForCLOB) {
                theJavaAttrsChunk.print("public void "
                        + setMethod + "(" + "char[]"
                        + " " + variableName + ") throws com.mcpdbwizard.pub.CSException");
            } else {
                theJavaAttrsChunk.print("public void "
                        + setMethod + "(" + "byte[]"
                        + " " + variableName + ") throws com.mcpdbwizard.pub.CSException");
            }
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print("if (useByteArraysForLongsAndLOBS)");
            ;
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print(byteArrayName + " = " + variableName + ";");
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
            theJavaAttrsChunk.print("else");
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            if (useCharForCLOB) {
                theJavaAttrsChunk.print("java.io.File tempFile = com.mcpdbwizard.pub.IOUtils.loadCharArrayIntoFile(" + variableName + ", " + stempFilePrefix + ", " + stempFileSuffix + ", " + stempFileDir + ".getAbsolutePath(), " + stheLog + ");");
            } else {
                theJavaAttrsChunk.print("java.io.File tempFile = com.mcpdbwizard.pub.IOUtils.loadByteArrayIntoFile(" + variableName + ", " + stempFilePrefix + ", " + stempFileSuffix + ", " + stempFileDir + ".getAbsolutePath(), " + stheLog + ");");
            }
            theJavaAttrsChunk.print(setMethod + "(tempFile);");
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
        } else if (actualOracleDataType.equals("XMLTYPE") && type == IS_A_FUNCTION) {
            theJavaAttrsChunk.print(" ");

            if (comments) {
                theJavaAttrsChunk.print("/**");
                theJavaAttrsChunk.print("* Method to set parameter " + argName);
                theJavaAttrsChunk.print("* @param " + "String" + " " + variableName);
                theJavaAttrsChunk.print("* @since 5.0.2314");
                theJavaAttrsChunk.print("* @throws com.mcpdbwizard.pub.CSException if the String can not be turned into a temporary file");
                theJavaAttrsChunk.print("*/");
            }

            theJavaAttrsChunk.print("public void "
                    + setMethod + "(" + "String"
                    + " " + variableName + ") throws com.mcpdbwizard.pub.CSException");
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print("if (useByteArraysForLongsAndLOBS)");
            ;
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            if (useCharForCLOB) {
                theJavaAttrsChunk.print(byteArrayName + " = " + variableName + ".toCharArray();");
            } else {
                theJavaAttrsChunk.print(byteArrayName + " = " + variableName + ".getBytes();");
            }
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
            theJavaAttrsChunk.print("else");
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print("java.io.File tempFile = com.mcpdbwizard.pub.IOUtils.loadStringIntoFile(" + variableName + ", " + stempFilePrefix + ", " + stempFileSuffix + ", " + stempFileDir + ".getAbsolutePath(), " + stheLog + ");");
            theJavaAttrsChunk.print(setMethod + "(tempFile);");
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
            theJavaAttrsChunk.print(" ");

            if (comments) {
                theJavaAttrsChunk.print("/**");
                theJavaAttrsChunk.print("* Method to set parameter " + argName);
                if (useCharForCLOB) {
                    theJavaAttrsChunk.print("* @param " + "char[]" + " " + variableName);
                } else {
                    theJavaAttrsChunk.print("* @param " + "byte[]" + " " + variableName);
                }
                theJavaAttrsChunk.print("* @since 5.0.2314");
                theJavaAttrsChunk.print("* @throws com.mcpdbwizard.pub.CSException if the String can not be turned into a temporary file");
                theJavaAttrsChunk.print("*/");
            }
            if (useCharForCLOB) {
                theJavaAttrsChunk.print("public void "
                        + setMethod + "(" + "char[]"
                        + " " + variableName + ") throws com.mcpdbwizard.pub.CSException");
            } else {
                theJavaAttrsChunk.print("public void "
                        + setMethod + "(" + "byte[]"
                        + " " + variableName + ") throws com.mcpdbwizard.pub.CSException");
            }
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print("if (useByteArraysForLongsAndLOBS)");
            ;
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print(byteArrayName + " = " + variableName + ";");
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
            theJavaAttrsChunk.print("else");
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            if (useCharForCLOB) {
                theJavaAttrsChunk.print("java.io.File tempFile = com.mcpdbwizard.pub.IOUtils.loadCharArrayIntoFile(" + variableName + ", " + stempFilePrefix + ", " + stempFileSuffix + ", " + stempFileDir + ".getAbsolutePath(), " + stheLog + ");");
            } else {
                theJavaAttrsChunk.print("java.io.File tempFile = com.mcpdbwizard.pub.IOUtils.loadByteArrayIntoFile(" + variableName + ", " + stempFilePrefix + ", " + stempFileSuffix + ", " + stempFileDir + ".getAbsolutePath(), " + stheLog + ");");
            }
            theJavaAttrsChunk.print(setMethod + "(tempFile);");
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
        } else if (actualOracleDataType.equals("BLOB") && type == IS_A_FUNCTION) {
            theJavaAttrsChunk.print(" ");

            if (comments) {
                theJavaAttrsChunk.print("/**");
                theJavaAttrsChunk.print("* Method to set parameter " + argName);
                theJavaAttrsChunk.print("* @param " + "byte[]" + " " + variableName);
                theJavaAttrsChunk.print("* @since 5.0.2314");
                theJavaAttrsChunk.print("* @throws com.mcpdbwizard.pub.CSException if the byte array can not be turned into a temporary file");
                theJavaAttrsChunk.print("*/");
            }

            theJavaAttrsChunk.print("public void "
                    + setMethod + "(" + "byte[]"
                    + " " + variableName + ") throws com.mcpdbwizard.pub.CSException");
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print("if (useByteArraysForLongsAndLOBS)");
            ;
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print(byteArrayName + " = " + variableName + ";");
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
            theJavaAttrsChunk.print("else");
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print("java.io.File tempFile = com.mcpdbwizard.pub.IOUtils.loadByteArrayIntoFile(" + variableName + ", " + stempFilePrefix + ", " + stempFileSuffix + ", " + stempFileDir + ".getAbsolutePath(), " + stheLog + ");");
            theJavaAttrsChunk.print(setMethod + "(tempFile);");
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
        } else if (actualOracleDataType.equals("BLOB") && type == IS_A_RECORD) {
            theJavaAttrsChunk.print(" ");

            if (comments) {
                theJavaAttrsChunk.print("/**");
                theJavaAttrsChunk.print("* Method to set parameter " + argName);
                theJavaAttrsChunk.print("* @param " + "byte[]" + " " + variableName);
                theJavaAttrsChunk.print("* @since 5.0.2241");
                theJavaAttrsChunk.print("* @throws com.mcpdbwizard.pub.CSException if the byte array can not be turned into a temporary file");
                theJavaAttrsChunk.print("*/");
            }

            theJavaAttrsChunk.print("public void "
                    + byteArraySetMethod + "(" + "byte[]"
                    + " " + variableName + ") throws com.mcpdbwizard.pub.CSException");
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print(byteArrayName + " = " + variableName + ";");
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
            //DRKLUGE covereage
        } else if (actualOracleDataType.equals("CLOB") && type == IS_A_RECORD) {
            theJavaAttrsChunk.print(" ");

            if (comments) {
                theJavaAttrsChunk.print("/**");
                theJavaAttrsChunk.print("* Method to set parameter " + argName);
                theJavaAttrsChunk.print("* @param " + "byte[]" + " " + variableName);
                theJavaAttrsChunk.print("* @since 5.0.2241");
                theJavaAttrsChunk.print("* @throws com.mcpdbwizard.pub.CSException if the byte array can not be turned into a temporary file");
                theJavaAttrsChunk.print("*/");
            }

            //DRKLUGE covereage
            if (useCharForCLOB) {
                theJavaAttrsChunk.print("public void "
                        + byteArraySetMethod + "(" + "char[]"
                        + " " + variableName + ") throws com.mcpdbwizard.pub.CSException");
            } else {
                theJavaAttrsChunk.print("public void "
                        + byteArraySetMethod + "(" + "byte[]"
                        + " " + variableName + ") throws com.mcpdbwizard.pub.CSException");

            }
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print(byteArrayName + " = " + variableName + ";");
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
        } else if (actualOracleDataType.equals("XMLTYPE") && type == IS_A_RECORD) {
            theJavaAttrsChunk.print(" ");

            if (comments) {
                theJavaAttrsChunk.print("/**");
                theJavaAttrsChunk.print("* Method to set parameter " + argName);
                theJavaAttrsChunk.print("* @param " + "byte[]" + " " + variableName);
                theJavaAttrsChunk.print("* @since 5.0.2241");
                theJavaAttrsChunk.print("* @throws com.mcpdbwizard.pub.CSException if the byte array can not be turned into a temporary file");
                theJavaAttrsChunk.print("*/");
            }

            //DRKLUGE covereage
            if (useCharForCLOB) {
                theJavaAttrsChunk.print("public void "
                        + byteArraySetMethod + "(" + "char[]"
                        + " " + variableName + ") throws com.mcpdbwizard.pub.CSException");
            } else {
                theJavaAttrsChunk.print("public void "
                        + byteArraySetMethod + "(" + "byte[]"
                        + " " + variableName + ") throws com.mcpdbwizard.pub.CSException");

            }
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print(byteArrayName + " = " + variableName + ";");
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
        } else if (actualOracleDataType.equals("BFILE") && type == IS_A_RECORD) {
            theJavaAttrsChunk.print(" ");

            if (comments) {
                theJavaAttrsChunk.print("/**");
                theJavaAttrsChunk.print("* Method to set parameter " + argName);
                theJavaAttrsChunk.print("* @param " + "byte[]" + " " + variableName);
                theJavaAttrsChunk.print("* @since 5.0.2241");
                theJavaAttrsChunk.print("* @throws com.mcpdbwizard.pub.CSException if the byte array can not be turned into a temporary file");
                theJavaAttrsChunk.print("*/");
            }

            theJavaAttrsChunk.print("public void "
                    + byteArraySetMethod + "(" + "byte[]"
                    + " " + variableName + ") throws com.mcpdbwizard.pub.CSException");
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print(byteArrayName + " = " + variableName + ";");
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
        } else if ((actualOracleDataType.equals("LONG RAW") || actualOracleDataType.equals("LONG"))
                && type == IS_A_FUNCTION) {
            theJavaAttrsChunk.print(" ");

            if (comments) {
                theJavaAttrsChunk.print("/**");
                theJavaAttrsChunk.print("* Method to set parameter " + argName);
                theJavaAttrsChunk.print("* @param " + "byte[]" + " " + variableName);
                theJavaAttrsChunk.print("* @since 5.0.2314");
                theJavaAttrsChunk.print("* @throws com.mcpdbwizard.pub.CSException if the byte array can not be turned into a temporary file");
                theJavaAttrsChunk.print("*/");
            }

            theJavaAttrsChunk.print("public void "
                    + setMethod + "(" + "byte[]"
                    + " " + variableName + ") throws com.mcpdbwizard.pub.CSException");
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print("if (useByteArraysForLongsAndLOBS)");
            ;
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print(byteArrayName + " = " + variableName + ";");
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
            theJavaAttrsChunk.print("else");
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print("java.io.File tempFile = com.mcpdbwizard.pub.IOUtils.loadByteArrayIntoFile(" + variableName + ", " + stempFilePrefix + ", " + stempFileSuffix + ", " + stempFileDir + ".getAbsolutePath(), " + stheLog + ");");
            theJavaAttrsChunk.print(setMethod + "(tempFile);");
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
        } else if ((actualOracleDataType.equals("LONG RAW") || actualOracleDataType.equals("LONG"))
                && type == IS_A_RECORD) {
            theJavaAttrsChunk.print(" ");

            if (comments) {
                theJavaAttrsChunk.print("/**");
                theJavaAttrsChunk.print("* Method to set parameter " + argName);
                theJavaAttrsChunk.print("* @param " + "byte[]" + " " + variableName);
                theJavaAttrsChunk.print("* @since 5.0.2241");
                theJavaAttrsChunk.print("* @throws com.mcpdbwizard.pub.CSException if the byte array can not be turned into a temporary file");
                theJavaAttrsChunk.print("*/");
            }

            theJavaAttrsChunk.print("public void "
                    + byteArraySetMethod + "(" + "byte[]"
                    + " " + variableName + ") throws com.mcpdbwizard.pub.CSException");
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print(byteArrayName + " = " + variableName + ";");
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
            //DRKLUGE covereage
        }

    }

    private void addLobSetMethod(JavaChunk theJavaChunk, String setMethod, String argName
            , String actualDataType, String variableName
            , String lobName, String inOut, boolean comments) {

        theJavaChunk.print(" ");

        if (comments) {
            theJavaChunk.print("/**");
            theJavaChunk.print("* Method to set lob locator for  " + variableName);
            theJavaChunk.print("* @param oracle.sql." + actualDataType + " " + lobName);
            theJavaChunk.print("*/");
        }

        theJavaChunk.print("public void "
                + setMethod + "(oracle.sql." + actualDataType
                + " " + lobName + ")");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("this." + lobName + " = " + lobName + ";");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
    }


    private void addObjectScalerGetMethod(JavaChunk theJavaChunk, String getMethod, String methodDesc, String argName, String castedDataType, String castMethod, String variableDataType, String variableName, boolean comments) {

        theJavaChunk.print(" ");

        if (comments) {
            theJavaChunk.print("/**");

            if (argName == null) {
                theJavaChunk.print("* Method to " + methodDesc + " function result");
            } else {
                theJavaChunk.print("* Method to " + methodDesc + " " + argName);
            }

            theJavaChunk.print("* @return " + castedDataType);
            theJavaChunk.print("*/");
        }

        theJavaChunk.print("public " + castedDataType + " " + getMethod + "()");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("if (" + variableName + " == null)");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("return(null);");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print(" ");
        boolean isANumberType = false;

        if (castedDataType.equals("Double") ||
                castedDataType.equals("Float") ||
                castedDataType.equals("Long") ||
                castedDataType.equals("Integer") ||
                castedDataType.equals("Short") ||
                castedDataType.equals("double") ||
                castedDataType.equals("float") ||
                castedDataType.equals("long") ||
                castedDataType.equals("int") ||
                castedDataType.equals("short") ||
                castedDataType.equals("Byte") ||
                castedDataType.equals("byte") ||
                castedDataType.equals("Boolean")) {
            isANumberType = true;
        }

        if (javaVersion >= JAVA_VERSION_21 && isANumberType) {

            theJavaChunk.print("return (" + castedDataType + ".valueOf(" + variableName + ".toString()));");
        } else {
            theJavaChunk.print("return (new " + castedDataType + "(" + variableName + "." + castMethod + "()));");
        }
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
    }

    private void addObjectScalerGetMethodNoNull(JavaChunk theJavaChunk, String getMethod, String methodDesc, String argName, String castedDataType, String castMethod, String variableDataType, String variableName, boolean comments) {

        theJavaChunk.print(" ");

        if (comments) {
            theJavaChunk.print("/**");

            if (argName == null) {
                theJavaChunk.print("* Method to " + methodDesc + " function result");
            } else {
                theJavaChunk.print("* Method to " + methodDesc + " " + argName);
            }

            theJavaChunk.print("* @return " + castedDataType);
            theJavaChunk.print("*/");
        }

        theJavaChunk.print("public " + castedDataType + " " + getMethod + "()");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("return(" + variableName + ");");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();

        if (variableDataType.equals("com.mcpdbwizard.pub.PlsqlIndexByTable2")) {
            theJavaChunk.print("");

            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* Set number of rows in PL/SQL INDEX BY table");
                theJavaChunk.print("* @param int maxRows Maximum number of rows in this array");
                theJavaChunk.print("**/");
            }

            theJavaChunk.print("public " + "void" + " " + "setMaxRowsFor" + getMethod.substring(3) + "(int maxRows)");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print(variableName + ".setElementMaxCount(maxRows);");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
        }
    }

    private void addDateGetMethods(JavaChunk theJavaChunk, JavaChunk theJavaAttrsChunk, String methodDesc, String argName, String castExpression, String variableDataType, String variableName, boolean comments, String javaNamingConvention) {

        //String getMethod = JavaUtils.getJavaName("get_" + variableName + "_" + "date" , javaNamingConvention, theLog);
        String getMethod = JavaUtils.getJavaName("get_" + variableName /* + "_" + "date" */, javaNamingConvention, theLog);

        theJavaAttrsChunk.print(" ");

        if (comments) {
            theJavaAttrsChunk.print("/**");

            if (argName == null) {
                theJavaAttrsChunk.print("* Method to " + methodDesc + " function result");
            } else {
                theJavaAttrsChunk.print("* Method to " + methodDesc + " " + argName);
            }

            theJavaAttrsChunk.print("* @return " + "java.util.Date");
            theJavaAttrsChunk.print("*/");
        }

        theJavaAttrsChunk.print("public " + "java.util.Date" + " " + getMethod + "()");
        theJavaAttrsChunk.indent();
        theJavaAttrsChunk.print("{");
        theJavaAttrsChunk.print("if (" + variableName + " == null)");
        theJavaAttrsChunk.indent();
        theJavaAttrsChunk.print("{");
        theJavaAttrsChunk.print("return(null);");
        theJavaAttrsChunk.print("}");
        theJavaAttrsChunk.unIndent();
        theJavaAttrsChunk.print(" ");
        theJavaAttrsChunk.print("return (new " + "java.util.Date" + "(" + variableName + ".getTime()));");
        theJavaAttrsChunk.print("}");
        theJavaAttrsChunk.unIndent();


        theJavaChunk.print(" ");

        getMethod = JavaUtils.getJavaName("get_" + variableName + "_" + "long", javaNamingConvention, theLog);
        if (comments) {
            theJavaChunk.print("/**");

            if (argName == null) {
                theJavaChunk.print("* Method to " + methodDesc + " function result");
            } else {
                theJavaChunk.print("* Method to " + methodDesc + " " + argName);
            }

            theJavaChunk.print("* @return long");
            theJavaChunk.print("*/");
        }

        theJavaChunk.print("public " + "long" + " " + getMethod + "()");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("return (" + variableName + ".getTime());");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();

        getMethod = JavaUtils.getJavaName("get_" + variableName + "_" + "timestamp", javaNamingConvention, theLog);
        if (comments) {
            theJavaChunk.print("/**");

            if (argName == null) {
                theJavaChunk.print("* Method to " + methodDesc + " function result");
            } else {
                theJavaChunk.print("* Method to " + methodDesc + " " + argName);
            }

            theJavaChunk.print("* @return java.sql.Timestamp");
            theJavaChunk.print("*/");
        }

        theJavaChunk.print("public " + "java.sql.Timestamp" + " " + getMethod + "()");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("if (" + variableName + " == null)");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("return(null);");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print(" ");
        theJavaChunk.print("return (new java.sql.Timestamp(" + variableName + ".getTime()));");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print(" ");
    }

    private void addArrayReturnMethod(JavaChunk theJavaChunk, String getMethod, String thisVariableName, String thisVariableWSDataType, String thisVariableWSParentDataType, boolean comments) {
        theJavaChunk.print("");

        if (comments) {
            theJavaChunk.print("/**");
            theJavaChunk.print("* Return " + thisVariableName + " as an array of " + thisVariableWSDataType);
            theJavaChunk.print("* @return " + thisVariableWSDataType + "[] an Array of " + thisVariableWSDataType);
            theJavaChunk.print("* @throws CSException");
            theJavaChunk.print("* @since V4.0.2160");
            theJavaChunk.print("*/");
        }

        theJavaChunk.print("public " + thisVariableWSDataType + "[] " + getMethod + "() throws CSException");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("if (" + thisVariableName + " == null)");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("return(null);");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print(" ");
        theJavaChunk.print("return ((" + thisVariableWSDataType + "[])" + thisVariableWSParentDataType + ".create" + thisVariableWSParentDataType + "ArrayFromRowSet(" + thisVariableName + "," + stheLog + "));");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print(" ");

    }

    private void addScalarGetNVLMethod(JavaChunk theJavaChunk, JavaChunk theJavaAttrsChunk, String getMethod, String methodDesc, String argName, String castedDataType, String castMethod, String variableDataType, String variableName, boolean comments) {
        theJavaAttrsChunk.print(" ");

        if (comments) {
            theJavaAttrsChunk.print("/**");

            if (argName == null) {
                theJavaAttrsChunk.print("* Method to " + methodDesc + " function result " + " or a default value");
            } else {
                theJavaAttrsChunk.print("* Method to " + methodDesc + " " + argName + " or a default value");
            }

            theJavaAttrsChunk.print("* @param defaultValue " + castedDataType);
            theJavaAttrsChunk.print("* @return " + castedDataType);
            theJavaAttrsChunk.print("* @since V5.0.2293");
            theJavaAttrsChunk.print("*/");
        }

        theJavaAttrsChunk.print("public " + castedDataType + " " + getMethod + "(" + castedDataType + " defaultValue) ");
        theJavaAttrsChunk.indent();
        theJavaAttrsChunk.print("{");
        theJavaAttrsChunk.print("if (" + variableName + " == null)");
        theJavaAttrsChunk.indent();
        theJavaAttrsChunk.print("{");
        theJavaAttrsChunk.print("return(defaultValue);");
        theJavaAttrsChunk.print("}");
        theJavaAttrsChunk.unIndent();
        theJavaAttrsChunk.print(" ");

        if (castMethod.length() == 0) {
            theJavaAttrsChunk.print("return (" + variableName + ");");
        } else {
            theJavaAttrsChunk.print("return (" + variableName + "." + castMethod + "());");
        }

        theJavaAttrsChunk.print("}");
        theJavaAttrsChunk.unIndent();


    }

    private void addScalarGetMethod(JavaChunk theJavaChunk, JavaChunk theJavaAttrsChunk, String getMethod, String methodDesc, String argName, String castedDataType, String castMethod, String variableDataType, String variableName, boolean comments) {
        theJavaAttrsChunk.print(" ");

        String throwsString = "";

        if (castedDataType.equals("oracle.sql.TIMESTAMPTZ")
                || castedDataType.equals("oracle.sql.TIMESTAMPLTZ")
                || castedDataType.equals("oracle.sql.TIMESTAMP")
                || castedDataType.equals("java.sql.Timestamp")
                || castedDataType.equals("oracle.sql.INTERVALDS")
                || castedDataType.equals("oracle.sql.INTERVALYM")
        ) {
            throwsString = "throws com.mcpdbwizard.pub.CSException";
        }

        if (comments) {
            theJavaAttrsChunk.print("/**");

            if (argName == null) {
                theJavaAttrsChunk.print("* Method to " + methodDesc + " function result");
            } else {
                theJavaAttrsChunk.print("* Method to " + methodDesc + " " + argName);
            }

            theJavaAttrsChunk.print("* @return " + castedDataType);

            if (throwsString.length() > 0) {
                theJavaAttrsChunk.print("* @" + throwsString + " if " + argName + " is null or can't be turned into a " + castedDataType);
            } else {
                theJavaChunk.print("* @throws NullPointerException if " + argName + " is null");
            }

            theJavaAttrsChunk.print("*/");
        }

        theJavaAttrsChunk.print("public " + castedDataType + " " + getMethod + "() " + throwsString);
        theJavaAttrsChunk.indent();
        theJavaAttrsChunk.print("{");
        if (castedDataType.equals("oracle.sql.TIMESTAMP")) {
            theJavaAttrsChunk.print("try");
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print("if (" + variableName + " == null)");
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print("return(null);");
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
            theJavaAttrsChunk.print(" ");
            theJavaAttrsChunk.print("return (new " + castedDataType + "(java.sql.Timestamp.valueOf(" + variableName + ")));");
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
            theJavaAttrsChunk.print("catch (Exception e)");
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print("throw (new com.mcpdbwizard.pub.CSException(\"" + getMethod + " threw Exception while trying to process " + variableDataType + ": \" + e.getMessage()));");
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
        } else if (castedDataType.equals("java.sql.Timestamp")) {
            theJavaAttrsChunk.print("try");
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print("if (" + variableName + " == null)");
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print("return(null);");
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
            theJavaAttrsChunk.print(" ");
            theJavaAttrsChunk.print("return (" + castedDataType + ".valueOf(" + variableName + "));");
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
            theJavaAttrsChunk.print("catch (Exception e)");
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print("throw (new com.mcpdbwizard.pub.CSException(\"" + getMethod + " threw Exception while trying to process " + variableDataType + ": \" + e.getMessage()));");
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
        } else if (castedDataType.equals("oracle.sql.INTERVALDS")) {
            theJavaAttrsChunk.print("try");
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print("if (" + variableName + " == null)");
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print("return(null);");
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
            theJavaAttrsChunk.print(" ");
            theJavaAttrsChunk.print("return (new " + castedDataType + "(" + variableName + "));");
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
            theJavaAttrsChunk.print("catch (Exception e)");
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print("throw (new com.mcpdbwizard.pub.CSException(\"" + getMethod + " threw Exception while trying to process " + variableDataType + ": \" + e.getMessage()));");
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
        } else if (castedDataType.equals("oracle.sql.INTERVALYM")) {
            theJavaAttrsChunk.print("try");
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print("if (" + variableName + " == null)");
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print("return(null);");
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
            theJavaAttrsChunk.print(" ");
            theJavaAttrsChunk.print("return (new " + castedDataType + "(" + variableName + "));");
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
            theJavaAttrsChunk.print("catch (Exception e)");
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print("throw (new com.mcpdbwizard.pub.CSException(\"" + getMethod + " threw Exception while trying to process " + variableDataType + ": \" + e.getMessage()));");
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
        } else if (castedDataType.equals("oracle.sql.TIMESTAMPTZ")
                || castedDataType.equals("oracle.sql.TIMESTAMPLTZ")) {
            theJavaAttrsChunk.print("try");
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print("if (" + variableName + " == null)");
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print("return(null);");
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
            theJavaAttrsChunk.print(" ");
            theJavaAttrsChunk.print("return (new " + castedDataType + "(" + variableName + "));");
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();
            theJavaAttrsChunk.print("catch (Exception e)");
            theJavaAttrsChunk.indent();
            theJavaAttrsChunk.print("{");
            theJavaAttrsChunk.print("throw (new com.mcpdbwizard.pub.CSException(\"" + getMethod + " threw Exception while trying to process " + variableDataType + ": \" + e.getMessage()));");
            theJavaAttrsChunk.print("}");
            theJavaAttrsChunk.unIndent();

        } else {
            if (castMethod.length() == 0) {
                theJavaAttrsChunk.print("return (" + variableName + ");");
            } else {
                theJavaAttrsChunk.print("return (" + variableName + "." + castMethod + "());");
            }
        }
        theJavaAttrsChunk.print("}");
        theJavaAttrsChunk.unIndent();


        if (castedDataType.equals("oracle.sql.TIMESTAMPTZ")
                || castedDataType.equals("java.sql.Timestamp")
                || castedDataType.equals("oracle.sql.TIMESTAMPLTZ")
                || castedDataType.equals("oracle.sql.INTERVALDS")
                || castedDataType.equals("oracle.sql.INTERVALYM")
        ) {
            theJavaChunk.print(" ");

            String connectionString = "";
            //String throwsString = "";

            if (castedDataType.equals("oracle.sql.TIMESTAMPTZ")
                    || castedDataType.equals("oracle.sql.TIMESTAMPLTZ")
            ) {
                connectionString = "java.sql.Connection paramConnection";
                throwsString = "throws CSException";
            }

            if (castedDataType.equals("java.sql.Timestamp")) {
                throwsString = "";
            }

            if (comments) {
                theJavaChunk.print("/**");

                if (argName == null) {
                    theJavaChunk.print("* Method to " + methodDesc + " function result");
                } else {
                    theJavaChunk.print("* Method to " + methodDesc + " " + argName);
                }

                if (castedDataType.equals("oracle.sql.TIMESTAMPTZ")) {
                    theJavaChunk.print("* This method requires a Connection in order to work");
                    theJavaChunk.print("* @param java.sql.Connection paramConnection");
                    theJavaChunk.print("* @throws CSException if " + argName + " can not be converted to a String");
                } else if (castedDataType.equals("oracle.sql.TIMESTAMPLTZ")) {
                    theJavaChunk.print("* This method requires a Connection in order to work");
                    theJavaChunk.print("* @param java.sql.Connection paramConnection");
                    theJavaChunk.print("* @throws CSException if " + argName + " can not be converted to a String");
                }

                theJavaChunk.print("* @return String");
                theJavaChunk.print("*/");
            }

            theJavaChunk.print("public String " + getMethod + "String(" + connectionString + ") " + throwsString);
            theJavaChunk.indent();
            theJavaChunk.print("{");

            if (castedDataType.equals("oracle.sql.TIMESTAMPTZ")
                    || castedDataType.equals("oracle.sql.TIMESTAMPLTZ")) {
                theJavaChunk.print("if (" + variableName + " == null)");
                theJavaChunk.indent();
                theJavaChunk.print("{");
                theJavaChunk.print("return(null);");
                theJavaChunk.print("}");
                theJavaChunk.unIndent();
                theJavaChunk.print(" ");
                theJavaChunk.print("try");
                theJavaChunk.indent();
                theJavaChunk.print("{");
                // Convert WITH the connection: oracle.sql.TIMESTAMPTZ/LTZ built from raw bytes cannot
                // be stringified by the no-arg stringValue() (ojdbc throws "Conversion to String
                // failed"); the connection carries the session time zone needed to render it.
                theJavaChunk.print("return ((new " + castedDataType + "(" + variableName + ")).stringValue(paramConnection));");
                theJavaChunk.print("}");
                theJavaChunk.unIndent();
                theJavaChunk.print("catch (java.sql.SQLException e)");
                theJavaChunk.indent();
                theJavaChunk.print("{");
                theJavaChunk.print("throw (new com.mcpdbwizard.pub.CSException(\"" + getMethod + " threw Exception while trying to process " + variableDataType + ": \" + e.getMessage()));");
                theJavaChunk.print("}");
                theJavaChunk.unIndent();
                theJavaChunk.print("}");
                theJavaChunk.unIndent();
            } else {
                theJavaChunk.print("return (" + variableName + ");");
                theJavaChunk.print("}");
                theJavaChunk.unIndent();
            }

        }
    }

    void addLobReleaseResourcesMethod(JavaChunk theJavaCode, boolean comments, boolean debugMessages, boolean otherMessages) {
        boolean hasLobs = false;
        boolean isOpenMessageNotDone = true;

        for (int i = 0; i < lobName.length; i++) {
            if (lobName[i] != null && (!actualOracleDatatype[i].equals("XMLTYPE"))) {
                hasLobs = true;
                break;
            }
        }


        if (hasLobs) {
            theJavaCode.print(" ");
            if (comments) {
                theJavaCode.print("/** ");
                theJavaCode.print("* Release all db resources that we know to be in use. Further attempts to use this ");
                theJavaCode.print("*  will fail until we are given another connection to play with. ");
                theJavaCode.print("* We explicitly rollback at the end of this method. ");
                theJavaCode.print("*  @return boolean <code>true</code> if we didn't encounter any problems, otherwise <code>false</code> ");
                theJavaCode.print("*/ ");
            }
            theJavaCode.print(" ");
            theJavaCode.print("public boolean releaseResources() ");
            theJavaCode.indent();
            theJavaCode.print("{ ");
            //theJavaCode.print("boolean returnCode = true; ");
            theJavaCode.print("");
            if (debugMessages) theJavaCode.print(stheLog + ".debug(\"Attempting to release LOB's\");");
            //theJavaCode.print("");
            //theJavaCode.print("try ");
            //theJavaCode.indent();
            //theJavaCode.print("{ ");

            for (int i = 0; i < lobName.length; i++) {
                if (lobName[i] != null && (!actualOracleDatatype[i].equals("XMLTYPE"))) {
                    theJavaCode.print("if (" + lobName[i] + " != null)");
                    theJavaCode.indent();
                    theJavaCode.print("{");


                    if (targetVersion.startsWith("9")) {
                        if (comments && isOpenMessageNotDone) {
                            theJavaCode.print("// The methods \"close()\" and \"isOpen()\" do not exist in the 8.1.5, 8.1.6, and 8.1.7 drivers");
                            isOpenMessageNotDone = false;
                        }

                        theJavaCode.print("try");
                        theJavaCode.indent();
                        theJavaCode.print("{");
                        theJavaCode.print("if (" + lobName[i] + ".isOpen())");
                        theJavaCode.indent();
                        theJavaCode.print("{");
                        theJavaCode.print(lobName[i] + ".close();");
                        theJavaCode.print("}");
                        theJavaCode.unIndent();
                        theJavaCode.print("}");
                        theJavaCode.unIndent();


                        theJavaCode.print("catch (Exception e)");
                        theJavaCode.indent();
                        theJavaCode.print("{");
                        if (comments && otherMessages) theJavaCode.print("// Log error message.");
                        if (otherMessages) theJavaCode.print("" + stheLog + ".warning(\"Exception while closing "
                                + lobName[i] + ":\" + e.toString()); ");
                        theJavaCode.print("}");
                        theJavaCode.unIndent();

                        theJavaCode.print("catch (java.lang.NoSuchMethodError e)");
                        theJavaCode.indent();
                        theJavaCode.print("{");
                        if (comments && otherMessages)
                            theJavaCode.print("// This Error will be thrown if we attempt to use this ");
                        if (comments && otherMessages)
                            theJavaCode.print("// code using an Oracle 8 driver as the 8i drivers don't ");
                        if (comments && otherMessages)
                            theJavaCode.print("// have the methods \"close()\" and \"isOpen()\". We catch and ");
                        if (comments && otherMessages)
                            theJavaCode.print("// log it because it will halt the JVM if allowed to propogate");
                        if (otherMessages)
                            theJavaCode.print("" + stheLog + ".warning(\"java.lang.NoSuchMethodError while closing "
                                    + lobName[i] + ":\" + e.toString()); ");
                        theJavaCode.print("}");
                        theJavaCode.unIndent();

                        // theJavaCode.print("catch (java.lang.NoSuchMethodError e)");
                        // theJavaCode.indent();
                        // theJavaCode.print("{");
                        // theJavaCode.print("}");
                        // theJavaCode.unIndent();
                        theJavaCode.print(" ");
                    }
                    theJavaCode.print(lobName[i] + " = null; ");
                    theJavaCode.print("}");
                    theJavaCode.unIndent();
                    theJavaCode.print("");
                }
            }

            //theJavaCode.print("} ");
            //theJavaCode.unIndent();
            //theJavaCode.print("catch (SQLException e) ");
            //theJavaCode.indent();
            //theJavaCode.print("{ ");
            //if (otherMessages) theJavaCode.print("" + stheLog + ".warning(e.toString()); ");
            //theJavaCode.print("} ");
            //theJavaCode.unIndent();
            theJavaCode.print("");
            theJavaCode.print("return(super.releaseResources()); ");
            theJavaCode.print("} ");
            theJavaCode.unIndent();
            theJavaCode.print("");
        }
    }

    private void setComplexFlag() {

        //theLog.info("XXXXXXX"+theRowSet.getColumnNamesAsString(":")); //DRKLUGE
        for (int i = 0; i < theRowSet.size(); i++) {
            //int recordInParams = 0;
            //int recordOutParams = 0;
            try {
                theRowSet.setCurrentRowNumber(i);
                oracleUnderlyingDatatype[i] = SqlUtils.getUnderlyingOracleDatatype(theRowSet.getString("DATA_TYPE"));

                //  theLog.info("XXXXXXX"+theRowSet.getRowAsString(":","?")); //DRKLUGE
                try {
                    if (theRowSet.getInt("POSITION") == 0) {
                        isAFunction = true;
                    }
                } catch (Exception e) {
                    if (theRowSet.getBigDecimal("POSITION").intValue() == 0) {
                        isAFunction = true;
                    }
                }

                switch (oracleUnderlyingDatatype[i]) {
                    case SqlUtils.ORACLE_BOOLEAN_DATATYPE: {
                        isComplex = true;
                        break;
                    }
                    case SqlUtils.ORACLE_XMLTYPE_DATATYPE: {
                        isComplex = true;
                        break;
                    }
                    case SqlUtils.ORACLE_OBJECT_DATATYPE:
                    case SqlUtils.ORACLE_TABLE_DATATYPE:
                    case SqlUtils.ORACLE_VARRAY_DATATYPE:
                    case SqlUtils.MCPDBWIZARD_READONLYROWSET:
                    case SqlUtils.ORACLE_ROWTYPE_DATATYPE: {
                        isComplex = true;
                        break;
                    }
                    case SqlUtils.ORACLE_PLSQL_INDEXBY_DATATYPE: {
                        isComplex = true;

                        String dataType = theRowSet.getString("DATA_TYPE");
                        String owner = theRowSet.getString("OWNER");
                        String all = theRowSet.getRowAsString(":", "?");

                        if (dataType.equals("PL/SQL TABLE") /**  && generatedClassName != null*/) {
                            oracleUnderlyingDatatype[i] = SqlUtils.ORACLE_PLSQL_INDEXBY_ROWTYPE_DATATYPE;
                        }

                        break;
                    }
                }
            } catch (CSNoDataInRowSetException e) {
            } catch (CSException e) {
                theLog.syserror(e, true, true);
            }
        }

    }

    public int getCursorRecordType(String owner
            , String objectName
            , String packageName
            , String overload
            , int seq
            , int level) {
        for (int i = 0; i < theRecords.length; i++) {
            if (theRecords[i].procSequence == (seq + 1)
                    && theRecords[i].procDataLevel == (level + 1)
                    && ((theRecords[i].procOverload == null && overload == null)
                    || theRecords[i].procOverload.equals(overload))
                    && theRecords[i].procPackageName.equals(packageName)
                    && theRecords[i].procObjectName.equals(objectName)
                    && theRecords[i].procOwner.equals(owner)
            ) {
                if (theRecords[i].replacedByArrayId > Integer.MIN_VALUE) {
                    return (theRecords[i].replacedByArrayId);
                }
                return (i);
            }
        }

        throw new NullPointerException();
    }

    public ReadOnlyRowSet getAttrArguments(String owner
            , String objectName
            , String packageName
            , String overload
            , int seq
            , int level
            , String realOwner
            , String realName) {
        ReadOnlyRowSet newRowSet = null;
        QueryStatement argQry = null;
        int mode = SqlStatementDictionary.ATTR_ARG_QUERY;

        try {
            if (packageName == null) {
                if (overload == null) {
                    argQry = new QueryStatement(SqlStatementDictionary.getArgQry(loginVersion, mode), theLog, theConnection);
                    argQry.setParam(1, realOwner);
                    argQry.setParam(2, objectName);
                    argQry.setParam(3, seq);
                    argQry.setParam(4, level);
                    argQry.setParam(5, seq);
                } else {
                    argQry = new QueryStatement(SqlStatementDictionary.getArgQryOverload(loginVersion, mode), theLog, theConnection);
                    argQry.setParam(1, realOwner);
                    argQry.setParam(2, objectName);
                    argQry.setParam(3, seq);
                    argQry.setParam(4, level);
                    argQry.setParam(5, seq);
                }
            } else // pack name not null
            {
                if (overload == null) {
                    argQry = new QueryStatement(SqlStatementDictionary.getArgQryPack(loginVersion, mode), theLog, theConnection);
                    argQry.setParam(1, realOwner);
                    argQry.setParam(2, objectName);
                    argQry.setParam(3, packageName);
                    argQry.setParam(4, seq);
                    argQry.setParam(5, level);
                    argQry.setParam(6, seq);
                } else {
                    argQry = new QueryStatement(SqlStatementDictionary.getArgQryPackOverload(loginVersion, mode), theLog, theConnection);
                    argQry.setParam(1, realOwner);
                    argQry.setParam(2, objectName);
                    argQry.setParam(3, packageName);
                    argQry.setParam(4, overload);
                    argQry.setParam(5, seq);
                    argQry.setParam(6, level);
                    argQry.setParam(7, seq);
                }
            }


            argQry.setKeepFiles(false);
            newRowSet = argQry.execute();

            //mrLog.info(newRowSet.toString());
            argQry.releaseResources();
        } catch (CSException e) {
            theLog.syserror(e, true, true);
        }

        return (newRowSet);
    }

    void cleanup() {
        theRowSet = null;
        theConnection = null;
        targetVersion = null;
        loginVersion = null;

        theLog = null;
        javaNamingConvention = null;
        theDatatypeWrangler = null;
        theRecords = null;
        overRideInOut = null;

        overRideInOut = null;
        numberDataTypes = null;

        variableName = null;
        variableDataType = null;
        oracleUnderlyingDatatype = null;
        oracleParamDatatype = null;
        oracleName = null;
        otherName = null;
        lobName = null;
        actualOracleDatatype = null;
        useVariable = null;

        paramInId = null;
        paramOutId = null;

        extraObjectId = null;

        PlsqlIndexByTableName = null;
        plsqlIndexByDataType = null;
        plsqlIndexByDataLength = null;
    }

    void createWebServicesCode(java.io.File serviceFile
            , java.io.File interfaceFile
            , java.io.File outputClassFile
            , java.io.File outputDir
            , boolean comments, boolean debugMessages, boolean otherMessages
            , SAAdminWrangler theWrangler
            , String procClassName, String procRecordName
            , String procPackageName, String packageName
            , String factoryClassName
            , int statementType
            , long startTime
            , int WScallType
            , String parentVariableName, String parentVariableClassName
            , boolean alwaysReleaseResources
            , boolean servicePreCallStubFlag
            , boolean servicePostCallStubFlag
            , SqlStatementWrangler theAspWrangler
            , boolean webServicesFlag
            , String wsRecTypeComboBox
            , String targetJVM
            , String methodPlsqlPrefix
            , String methodSqlPrefix
            , String postscriptName
            , String postscriptContent
            , String extraClassCode
            , String oracleVersion) {
        final int ROWSET_MAX_SIZE = 245;
        final int ROWSET_WARNING_SIZE = 200;

        JavaChunk serviceCode = new JavaChunk();
        JavaChunk interfaceCode = new JavaChunk();

        String returnType = "";
        int outParams = 0;
        JavaChunk outputCode = null;

        String serviceMethod = "";
        //String qualifiedParentVariableName = null;

        boolean xmltypeNotWarned = true;

        String webServiceRecType = "public";
        if (wsRecTypeComboBox.equals(com.mcpdbwizard.app.procbuilder.gui.ApplicationShell.WS_REC_TYPES[0])) {
            webServiceRecType = "public";
        } else if (wsRecTypeComboBox.equals(com.mcpdbwizard.app.procbuilder.gui.ApplicationShell.WS_REC_TYPES[1])) {
            webServiceRecType = "protected";
        }

        try {

            String getDaoMethodName = "get" + procClassName + "PlSqlDAO";
            //String sqlOrPlsql = "_plsql_";
            String sqlOrPlsql = new String(methodPlsqlPrefix);

            //if (procPackageName.endsWith(".sql"))
            //  {
            //  getDaoMethodName = "get" + procClassName + "SqlDAO";
            //  sqlOrPlsql = "_sql_";
            //  }
            if (procPackageName.endsWith(".sql")) {
                getDaoMethodName = "get" + procClassName + "SqlDAO";
                sqlOrPlsql = new String(methodSqlPrefix);
            }

            //serviceMethod = JavaUtils.getJavaName("service"  + sqlOrPlsql + procClassName, javaNamingConvention, theLog);      //DRKLUGE123
            serviceMethod = JavaUtils.getJavaName(sqlOrPlsql + procClassName, javaNamingConvention, theLog);      //DRKLUGE123

            String exceptionName = factoryClassName + "ServiceException";

            //if (javaNamingConvention.equalsIgnoreCase("spaces_between_words.java"))
            //  {
            //  exceptionName = factoryClassName + "_service_exception";
            //  }


            if (parentVariableName != null && parentVariableName.length() > 0) {
                ///qualifiedParentVariableName = parentVariableName + ".";
            } else {
                //qualifiedParentVariableName = "";
            }

            if (WScallType == CallableStatementParameterEngine.WS_CALL_TYPE_ALL) {
                String[] tooManyParamsMessage =
                        {"Can't Call Class " + procClassName + " as a service. It has too many parameters."
                                , "Max number of parameters is " + ROWSET_MAX_SIZE + "." + procClassName + " has " + theRowSet.size()
                                , "See: " + JAVA_PARAM_LIMIT_URL
                        };

                if (theRowSet.size() > ROWSET_MAX_SIZE) {

                    for (int tmp = 0; tmp < tooManyParamsMessage.length; tmp++) {
                        serviceCode.print("// " + tooManyParamsMessage[tmp]);
                        interfaceCode.print("// " + tooManyParamsMessage[tmp]);
                        theLog.error(tooManyParamsMessage[tmp]);
                    }

                    throw (new CSSkipSectionException());
                }

                if (comments) {
                    serviceCode.print("");
                    serviceCode.print("/** ");
                    serviceCode.print("* Call Class " + procClassName + " as a service");

                    interfaceCode.print("");
                    interfaceCode.print("/** ");
                    interfaceCode.print("* Call Class " + procClassName + " as a service");
                    if (theRowSet.size() > ROWSET_WARNING_SIZE) {
                        serviceCode.print("* <b>WARNING:</b> This procedure has " + theRowSet.size() + " parameters. The limit is around " + ROWSET_MAX_SIZE);
                        serviceCode.print("* See: " + JAVA_PARAM_LIMIT_URL);
                        interfaceCode.print("* <b>WARNING:</b> This procedure has " + theRowSet.size() + " parameters. The limit is around " + ROWSET_MAX_SIZE);
                        interfaceCode.print("* See: " + JAVA_PARAM_LIMIT_URL);
                    }

                    for (int i = 0; i < theRowSet.size(); i++) {

                        theRowSet.setCurrentRowNumber(i);

                        if (theRowSet.getInt("POSITION") > 0 && (theRowSet.getString("IN_OUT").equals("IN")
                                || theRowSet.getString("IN_OUT").equals("IN/OUT")
                                || overRideInOut.equals("IN/OUT"))
                        ) {
                            try {
                                String currentVariableName = new String(variableName[i]);
                                for (int j = 0; j < i; j++) {
                                    if (variableName[j].equals(currentVariableName)) {
                                        throw (new CSSkipSectionException());
                                    }
                                }

                                serviceCode.print("* @param " + variableWSDataType[i] + " " + variableName[i] + " " + actualOracleDatatype[i]);
                                interfaceCode.print("* @param " + variableWSDataType[i] + " " + variableName[i] + " " + actualOracleDatatype[i]);
                            } //try
                            catch (CSSkipSectionException e) {
                            }
                        } //if
                    } //for

                }

                // Figure out what the output will be....
                returnType = "void";
                outParams = 0;

                // Create record class
                outputCode = new JavaChunk();


                outputCode.print("package " + procPackageName + ";");

                if (comments) {
                    outputCode.print("");
                    outputCode.print("/** ");
                    outputCode.print("* Record class for output of " + procClassName);
                    if (webServiceRecType.equals("protected"))
                        outputCode.print("* @since 5.0.2234 - 'set' and 'get' methods added for Axis 1.2 compatability.");
                    outputCode.print("* @since 5.0.2253 - Implements java.io.Serializable");
                    outputCode.print("* @since 6.0.2806 - get and set methods removed if protected");
                    outputCode.print("* @see " + procPackageName + "." + procClassName);
                    outputCode.print("*/");
                }

                outputCode.print(" ");
                outputCode.print("public class " + procRecordName + " implements java.io.Serializable");
                outputCode.print("{");
                outputCode.indent();

                for (int i = 0; i < theRowSet.size(); i++) {
                    theRowSet.setCurrentRowNumber(i);
                    //System.out.println(theRowSet.getString("IN_OUT") + " " + theRowSet.getInt("POSITION"));

                    if (theRowSet.getInt("POSITION") >= 0 && (theRowSet.getString("IN_OUT").equals("OUT")
                            || theRowSet.getString("IN_OUT").equals("IN/OUT")
                            || overRideInOut.equals("IN/OUT"))
                    ) {
                        try {
                            String currentVariableName = new String(variableName[i]);

                            for (int j = 0; j < i; j++) {
                                //theRowSet.setCurrentRowNumber(j);
                                if (variableName[j].equals(currentVariableName)) {
                                    throw (new CSSkipSectionException());
                                }
                            }

                            returnType = variableWSDataType[i];
                            outParams++;

                            if (variableWSDataType[i].equals(sWsdlFileName)) {
                                if (comments) {
                                    outputCode.print("/**");
                                    outputCode.print("* Rowset for " + variableName[i]);
                                    outputCode.print("*/");
                                }
                                outputCode.print(webServiceRecType + " " + packageName + "." + variableWSDataType[i] + " " + variableName[i] + " = null;");
                            } else if (cursorRecordId[i] > Integer.MIN_VALUE) {
                                if (comments) {
                                    outputCode.print("/**");
                                    outputCode.print("* Record for " + variableName[i]);
                                    outputCode.print("*/");
                                }
                                outputCode.print(webServiceRecType + " " + procPackageName + "." + variableWSDataType[i] + "[] " + variableName[i] + " = null;");
                                returnType = procPackageName + "." + returnType + "[]";
                            } else if (variableWSDataType[i].equals("boolean")) {
                                if (comments) {
                                    outputCode.print("/**");
                                    outputCode.print("* flag for " + variableName[i]);
                                    outputCode.print("*/");
                                }
                                outputCode.print(webServiceRecType + " " + variableWSDataType[i] + " " + variableName[i] + ";");
                            } else if (variableDataType[i].equals("java.math.BigDecimal") && (!variableWSDataType[i].equals("java.math.BigDecimal"))) {
                                if (comments) {
                                    outputCode.print("/**");
                                    outputCode.print("* " + variableName[i]);
                                    outputCode.print("*/");
                                }
                                outputCode.print(webServiceRecType + " " + variableWSDataType[i] + " " + variableName[i] + ";");
                            } else {
                                if (comments) {
                                    outputCode.print("/**");
                                    outputCode.print("* " + variableName[i]);
                                    outputCode.print("*/");
                                }
                                outputCode.print(webServiceRecType + " " + variableWSDataType[i] + " " + variableName[i] + " = null;");
                            }

                            outputCode.print("");
                        } catch (CSSkipSectionException e) {
                        }
                    }
                }

                if (outParams == 0 && statementType == SqlUtils.SELECT && theAspWrangler != null) {
                    if (theAspWrangler.qryCse != null) // We know how to return records
                    {
                        returnType = packageName + ".sql." + theAspWrangler.cursorAttrFileName + "[]";
                    } else {
                        returnType = sWsdlFileName;
                    }
                } else if (outParams == 0 && statementType == SqlUtils.SELECT) {
                    // this is a query.
                    returnType = sWsdlFileName;
                } else if (outParams > 1) {
                    returnType = procPackageName + "." + procRecordName;


                    for (int i = 0; i < theRowSet.size(); i++) {
                        theRowSet.setCurrentRowNumber(i);
                        //System.out.println(theRowSet.getString("IN_OUT") + " " + theRowSet.getInt("POSITION"));

                        if (theRowSet.getInt("POSITION") >= 0 && (theRowSet.getString("IN_OUT").equals("OUT")
                                || theRowSet.getString("IN_OUT").equals("IN/OUT")
                                || overRideInOut.equals("IN/OUT"))
                        ) {
                            try {
                                String currentVariableName = new String(variableName[i]);
                                String initcapVariableName = new String(currentVariableName.substring(0, 1).toUpperCase());
                                if (currentVariableName.length() > 1) {
                                    initcapVariableName = initcapVariableName + currentVariableName.substring(1);
                                }


                                for (int j = 0; j < i; j++) {
                                    //theRowSet.setCurrentRowNumber(j);
                                    if (variableName[j].equals(currentVariableName)) {
                                        throw (new CSSkipSectionException());
                                    }
                                }


                                if (webServiceRecType.equals("protected")) {

                                    if (comments) {
                                        outputCode.print("/**");
                                        outputCode.print("* Set " + variableName[i]);
                                        outputCode.print("* @param " + variableWSDataType[i] + " " + variableName[i]);
                                        outputCode.print("* @since 5.0.2234");
                                        outputCode.print("*/");
                                    }
                                    String brackets = "";
                                    if (cursorRecordId[i] > Integer.MIN_VALUE) {
                                        brackets = "[]";
                                    }

                                    if (variableWSDataType[i].equals(sWsdlFileName)) {
                                        outputCode.print("public void set" + initcapVariableName + "(" + packageName + "." + variableWSDataType[i] + brackets + " " + variableName[i] + ")");
                                    } else {
                                        outputCode.print("public void set" + initcapVariableName + "(" + variableWSDataType[i] + brackets + " " + variableName[i] + ")");
                                    }

                                    outputCode.indent();
                                    outputCode.print("{");
                                    outputCode.print("this." + variableName[i] + " = " + variableName[i] + ";");
                                    outputCode.print("}");
                                    outputCode.unIndent();
                                    outputCode.print("");

                                    if (comments) {
                                        outputCode.print("/**");
                                        outputCode.print("* Get " + variableName[i]);
                                        outputCode.print("* @return " + variableName[i]);
                                        outputCode.print("* @since 5.0.2234");
                                        outputCode.print("*/");
                                    }
                                    if (variableWSDataType[i].equals(sWsdlFileName)) {
                                        outputCode.print("public " + packageName + "." + variableWSDataType[i] + brackets + " get" + initcapVariableName + "()");
                                    } else {
                                        outputCode.print("public " + variableWSDataType[i] + brackets + " get" + initcapVariableName + "()");
                                    }
                                    outputCode.indent();
                                    outputCode.print("{");
                                    outputCode.print("return(" + variableName[i] + ");");
                                    outputCode.print("}");
                                    outputCode.unIndent();
                                    outputCode.print("");


                                } // protected
                            } catch (CSSkipSectionException e) {
                            }
                        }
                    }

                    outputCode.print(" ");
                    outputCode.unIndent();
                    outputCode.print("} // Generated by " + Namer.param_prod_name + " " + Namer.param_version + " ");
                    outputCode.unIndent();

                    theWrangler.writeFile(outputClassFile, outputCode.getLines());

                }

                if (comments) {
                    serviceCode.print("* @return " + returnType);
                    serviceCode.print("* @throws " + exceptionName);
                    serviceCode.print("*/");

                    interfaceCode.print("* @return " + returnType);
                    interfaceCode.print("* @throws " + exceptionName);
                    interfaceCode.print("*/");
                }


                if (targetJVM.equals("1.2") || targetJVM.equals("1.3") || targetJVM.equals("1.4") || targetJVM.equals("1.5")) {
                    if (comments)
                        serviceCode.print("// JSR-181 support is available if the target JVm is 1.6 or higher");
                    if (comments)
                        interfaceCode.print("// JSR-181 support is available if the target JVm is 1.6 or higher");
                    serviceCode.print("//@WebMethod");
                    interfaceCode.print("//@WebMethod");
                } else {
                    if (comments) serviceCode.print("// JSR-181 support - make this method part of a web service");
                    if (comments) interfaceCode.print("// JSR-181 support - make this method part of a web service");
                    serviceCode.print("@WebMethod");
                    interfaceCode.print("@WebMethod");
                }

                String rightParenOrComma = "(";
                String methodStart = "public " + returnType + " " + serviceMethod;

                metadata.setMethodName(serviceMethod);
                String[] metadataExceptions = {exceptionName};
                metadata.setMethodExceptions(metadataExceptions);
                metadata.setMethodReturn(returnType);

                String wsMethodAnnotation = "";

                serviceCode.print(methodStart);
                serviceCode.indent();
                serviceCode.indent();

                interfaceCode.print(methodStart);
                interfaceCode.indent();
                interfaceCode.indent();

                ArrayList<String[]> metadataParams = new ArrayList<String[]>();

                for (int i = 0; i < theRowSet.size(); i++) {

                    theRowSet.setCurrentRowNumber(i);

                    if (theRowSet.getInt("POSITION") > 0 && (theRowSet.getString("IN_OUT").equals("IN")
                            || theRowSet.getString("IN_OUT").equals("IN/OUT")
                            || overRideInOut.equals("IN/OUT"))
                    ) {
                        try {
                            String currentVariableName = new String(variableName[i]);

                            for (int j = 0; j < i; j++) {
                                //theRowSet.setCurrentRowNumber(j);
                                if (variableName[j].equals(currentVariableName)) {
                                    throw (new CSSkipSectionException());
                                }
                            }

                            if (targetJVM.equals("1.2") || targetJVM.equals("1.3") || targetJVM.equals("1.4") || targetJVM.equals("1.5")) {
                                wsMethodAnnotation = "";
                            } else {
                                wsMethodAnnotation = "@WebParam(name=\"" + variableName[i] + "\") ";
                            }

                            serviceCode.print(rightParenOrComma + wsMethodAnnotation + variableWSDataType[i] + " " + variableName[i]);
                            interfaceCode.print(rightParenOrComma + wsMethodAnnotation + variableWSDataType[i] + " " + variableName[i]);
                            //String[] ourParams = {variableWSDataType[i], variableName[i]};
                            String[] ourParams = {variableName[i], variableWSDataType[i]};
                            metadataParams.add(ourParams);

                            rightParenOrComma = ",";
                        } //try
                        catch (CSSkipSectionException e) {
                            // DELIBERATE, and the only thing thrown into it is the
                            // duplicate-name check a few lines above: a parameter whose
                            // name repeats one already emitted is dropped rather than
                            // declared twice, which would not compile. Empty because
                            // skipping IS the handling. Commented because an empty catch
                            // reads as a swallowed bug -- this one has already sent
                            // someone hunting for a defect that is not there.
                        }

                    } //if

                } //for

                try {
                    metadataParams.trimToSize();
                    String[][] tempParams = new String[metadataParams.size()][2];
                    tempParams = metadataParams.toArray(tempParams);
                    metadata.setMethodParams(tempParams);
                } catch (Exception e1) {
                    // Unreachable in practice, and kept rather than deleted because
                    // proving that is the useful part: trimToSize and toArray cannot
                    // fail on a list-sized array, setMethodParams is a bare field
                    // assignment, and `metadata` is initialised at its declaration and
                    // never set to null. The one thing that could throw -- an
                    // OutOfMemoryError on the allocation -- is an Error, so it would
                    // pass straight through this catch. Were it ever reached the result
                    // would be LOUD, not silent: an empty parameter list makes the MCP
                    // emitter write a no-argument call to a method that takes
                    // arguments, which javac rejects.
                    theLog.error("Metadata Error: " + e1.getMessage());
                }

                if (rightParenOrComma.equals("(")) {
                    serviceCode.appendToLastLine(rightParenOrComma);
                    interfaceCode.appendToLastLine(rightParenOrComma);
                }

                serviceCode.appendToLastLine(") throws " + exceptionName);
                interfaceCode.appendToLastLine(") throws " + exceptionName + ";");
                interfaceCode.print(" ");

                serviceCode.unIndent();
                serviceCode.print("{");

                serviceCode.print("try");
                serviceCode.indent();
                serviceCode.print("{");

                if (debugMessages) {
                    serviceCode.print(stheLog + ".debug(\"" + serviceMethod + " Starting\");");
                    serviceCode.print("");
                }


                boolean needsNullCasting = false;
                for (int i = 0; i < theRowSet.size(); i++) {

                    theRowSet.setCurrentRowNumber(i);

                    if (theRowSet.getInt("POSITION") > 0 && (theRowSet.getString("IN_OUT").equals("IN")
                            || theRowSet.getString("IN_OUT").equals("IN/OUT")
                            || overRideInOut.equals("IN/OUT"))
                    ) {
                        try {
                            String currentVariableName = new String(variableName[i]);

                            for (int j = 0; j < i; j++) {
                                //theRowSet.setCurrentRowNumber(j);
                                if (variableName[j].equals(currentVariableName)) {
                                    throw (new CSSkipSectionException());
                                }
                            }

                            switch (oracleUnderlyingDatatype[i]) {
                                case SqlUtils.ORACLE_TABLE_DATATYPE:
                                case SqlUtils.ORACLE_VARRAY_DATATYPE:
                                case SqlUtils.ORACLE_PLSQL_INDEXBY_ROWTYPE_DATATYPE:
                                case SqlUtils.ORACLE_OBJECT_DATATYPE:
                                case SqlUtils.ORACLE_ROWTYPE_DATATYPE: {
                                    needsNullCasting = true;
                                    break;
                                }
                                default: {
                                    break;
                                }
                            }
                            //interfaceCode.print(rightParenOrComma + wsMethodAnnotation + variableWSDataType[i] + " " + variableName[i]);

                        } //try
                        catch (CSSkipSectionException e) {
                        }

                    } //if

                } //for


                if (needsNullCasting) {

                    if (comments) serviceCode.print("// Fill in null parameters with objects if requested");

                    serviceCode.print("if (castNullToNewObject) ");
                    serviceCode.indent();
                    serviceCode.print("{");
                    for (int i = 0; i < theRowSet.size(); i++) {

                        theRowSet.setCurrentRowNumber(i);

                        if (theRowSet.getInt("POSITION") > 0 && (theRowSet.getString("IN_OUT").equals("IN")
                                || theRowSet.getString("IN_OUT").equals("IN/OUT")
                                || overRideInOut.equals("IN/OUT"))
                        ) {
                            try {
                                String currentVariableName = new String(variableName[i]);

                                for (int j = 0; j < i; j++) {
                                    //theRowSet.setCurrentRowNumber(j);
                                    if (variableName[j].equals(currentVariableName)) {
                                        throw (new CSSkipSectionException());
                                    }
                                }

                                switch (oracleUnderlyingDatatype[i]) {
                                    case SqlUtils.ORACLE_TABLE_DATATYPE:
                                    case SqlUtils.ORACLE_VARRAY_DATATYPE:
                                    case SqlUtils.ORACLE_PLSQL_INDEXBY_ROWTYPE_DATATYPE:
                                    case SqlUtils.ORACLE_OBJECT_DATATYPE:
                                    case SqlUtils.ORACLE_ROWTYPE_DATATYPE: {
                                        //serviceCode.print("// "+ variableWSDataType[i]);
                                        if (variableWSDataType[i].endsWith("[][]")) {
                                            serviceCode.print("if (" + variableName[i] + " == null) " + variableName[i] + " = new " + variableWSDataType[i].substring(0, variableWSDataType[i].length() - 4) + "[0][0];");
                                        } else if (variableWSDataType[i].endsWith("[]")) {
                                            serviceCode.print("if (" + variableName[i] + " == null) " + variableName[i] + " = new " + variableWSDataType[i].substring(0, variableWSDataType[i].length() - 2) + "[0];");
                                        } else {
                                            serviceCode.print("if (" + variableName[i] + " == null) " + variableName[i] + " = new " + variableWSDataType[i] + "();");
                                        }
                                    }
                                    default: {
                                        break;
                                    }
                                }
                                //interfaceCode.print(rightParenOrComma + wsMethodAnnotation + variableWSDataType[i] + " " + variableName[i]);

                            } //try
                            catch (CSSkipSectionException e) {
                            }

                        } //if

                    } //for


                    serviceCode.print("}");
                    serviceCode.unIndent();
                    serviceCode.print(" ");


                }


                if (!returnType.equals("void")) {
                    if (comments) {
                        serviceCode.print("// Declare variable to hold result of service");
                    }

                    if (outParams > 1) {
                        serviceCode.print(returnType + " serviceOutput = new " + returnType + "();");
                    } else {
                        serviceCode.print(returnType + " serviceOutput;");
                    }

                    serviceCode.print("");
                } //void

                if (comments) {
                    serviceCode.print("// Get instance of created class");
                }

                if (debugMessages) {
                    serviceCode.print(stheLog + ".debug(\"" + serviceMethod + ": Get instance of created class\");");
                }

                serviceCode.print(procPackageName + "." + procClassName + " theService = " + getDaoMethodName + "();");
                serviceCode.print("");

        /*
         *
         *if (hasFiles)
          {
          if (comments)        //DRKLUGE
            {
            serviceCode.print("// Make sure lobs are downloaded to files");
            }
          serviceCode.print("theService."+sSetKeepLobs+"(false);");
          serviceCode.print("");

          if (comments)
            {
            serviceCode.print("// Make sure temporary files are deleted when JVM exits");
            }
          serviceCode.print("theService."+sSetKeepFiles+"(false);");
          serviceCode.print("");
          }
*/

                if ((hasFiles || hasChildFiles) && type == IS_A_FUNCTION) {
                    if (comments) {
                        serviceCode.print("// Make sure we avoid temporary files");
                    }

                    serviceCode.print("theService.setUseByteArraysForLongsAndLOBS(true);"); //DRKLUGE
                    serviceCode.print("");
                }

                if (!returnType.equals("void")) {
                    if (comments) {
                        serviceCode.print("// Set parameters");
                    }

                    if (debugMessages) {
                        serviceCode.print(stheLog + ".debug(\"" + serviceMethod + ": Set parameters\");");
                    }
                }

            } // ws all

            // Set parameters...

            if (WScallType == CallableStatementParameterEngine.WS_CALL_TYPE_ALL
                    || WScallType == CallableStatementParameterEngine.WS_CALL_TYPE_SET) {
                for (int i = 0; i < theRowSet.size(); i++) {
                    theRowSet.setCurrentRowNumber(i);
                    String setMethod = JavaUtils.getJavaName("set_" + variableName[i], javaNamingConvention, theLog);
                    String setNullMethod = JavaUtils.getJavaName("set_" + variableName[i] + "_to_null", javaNamingConvention, theLog);

                    String extraFrontBit = "";
                    String extraBackBit = "";

                    switch (oracleUnderlyingDatatype[i]) {
                        case SqlUtils.ORACLE_TABLE_DATATYPE:
                        case SqlUtils.ORACLE_VARRAY_DATATYPE:
                        case SqlUtils.ORACLE_PLSQL_INDEXBY_ROWTYPE_DATATYPE: {
                            if (variableWSDataType[i].equals(variableWSParentDataType[i] + "[]")) {
                                // no point in casting something to itself
                                extraFrontBit = "new " + variableDataType[i] + "(" + stheLog + ", ";
                                extraBackBit = ")";
                            } else {
                                extraFrontBit = "new " + variableDataType[i] + "(" + stheLog + ", (" + variableWSParentDataType[i] + ".create" + variableWSParentDataType[i] + "ArrayFrom" + JavaUtils.replaceString(variableWSDataType[i], "[]", "") + "Array(";
                                extraBackBit = ")))";
                            }
                            break;
                        }
                        case SqlUtils.ORACLE_LONGTEXT_DATATYPE:
                        case SqlUtils.ORACLE_LONG_BINARY_DATATYPE: {
                            extraFrontBit = "loadByteArrayIntoFile(";//DRKLUGE
                            extraBackBit = ")";
                            break;
                        }
                        case SqlUtils.ORACLE_CLOB_DATATYPE:
                        case SqlUtils.ORACLE_BLOB_DATATYPE:
                        case SqlUtils.ORACLE_BFILE_DATATYPE: {
                            extraFrontBit = "";
                            extraBackBit = "";
                            break;
                        }
                        case SqlUtils.ORACLE_PLSQL_INDEXBY_DATATYPE: {
                            if (plsqlIndexByDataType[i] == OracleTypes.VARCHAR) {
                                extraFrontBit = "createIndexByTableFromStringArray(";
                                extraBackBit = ")";
                            } else {
                                String dType = "oracle.jdbc.OracleTypes.NUMBER";
                                if (plsqlIndexByRealDataType[i] != oracle.jdbc.OracleTypes.NUMBER) {
                                    dType = "oracle.jdbc.OracleTypes.VARCHAR";
                                }

                                if (variableWSDataType[i].equals("java.math.BigDecimal[]")) {
                                    extraFrontBit = "createIndexByTableFromBigDecimalArray(";
                                    extraBackBit = "," + dType + "," + plsqlIndexByDataDecPlaces[i] + ")";
                                } else if (variableWSDataType[i].equals("int[]")) {
                                    extraFrontBit = "createIndexByTableFromBigDecimalArray(createBigDecimalArrayFromIntegerArray(";
                                    extraBackBit = ")," + dType + "," + plsqlIndexByDataDecPlaces[i] + ")";
                                } else if (variableWSDataType[i].equals("long[]")) {
                                    extraFrontBit = "createIndexByTableFromBigDecimalArray(createBigDecimalArrayFromLongArray(";
                                    extraBackBit = ")," + dType + "," + plsqlIndexByDataDecPlaces[i] + ")";
                                } else if (variableWSDataType[i].equals("double[]")) {
                                    extraFrontBit = "createIndexByTableFromBigDecimalArray(createBigDecimalArrayFromDoubleArray(";
                                    extraBackBit = ")," + dType + "," + plsqlIndexByDataDecPlaces[i] + ")";
                                } else if (variableWSDataType[i].equals("float[]")) {
                                    extraFrontBit = "createIndexByTableFromBigDecimalArray(createBigDecimalArrayFromFloatArray(";
                                    extraBackBit = ")," + dType + "," + plsqlIndexByDataDecPlaces[i] + ")";
                                } else if (variableWSDataType[i].equals("String[]")) {
                                    // A ZONED timestamp element. It reaches this branch rather than
                                    // the plsqlIndexByDataType == VARCHAR one above because its type
                                    // code is TIMESTAMPTZ/LTZ, but it crosses the service boundary as
                                    // TEXT -- which is what carries the zone, and what lets the
                                    // existing String helpers serve it with no new generated code.
                                    extraFrontBit = "createIndexByTableFromStringArray(";
                                    extraBackBit = ")";
                                } else if (variableWSDataType[i].equals("java.sql.Timestamp[]")) {
                                    extraFrontBit = "createIndexByTableFromTimestampArray(";
                                    extraBackBit = "," + plsqlIndexByDataDecPlaces[i] + ")";
                                } else if (variableWSDataType[i].equals("byte[][]")) {
                                    extraFrontBit = "createIndexByTableFromByteArray(";
                                    extraBackBit = ")";
                                } else if (variableWSDataType[i].equals("char[][]")) {
                                    extraFrontBit = "createIndexByTableFromCharArray("; //DRKLUGE
                                    extraBackBit = ")";
                                }
                                //createIndexByTableFromByteArray
                                else {
                                    extraFrontBit = "createIndexByTableFromBigDecimalArray(";
                                    extraBackBit = ",oracle.jdbc.OracleTypes.VARCHAR," + plsqlIndexByDataDecPlaces[i] + ")";
                                }
                            }
                            break;
                        }
                        case SqlUtils.ORACLE_BINARY_DATATYPE:
                        case SqlUtils.ORACLE_NUMBER_DATATYPE:
                        case SqlUtils.ORACLE_DATE_DATATYPE:
                        case SqlUtils.ORACLE_TEXT_DATATYPE:
                        case SqlUtils.ORACLE_ROWID_DATATYPE:
                        case SqlUtils.ORACLE_UROWID_DATATYPE:
                        case SqlUtils.ORACLE_BOOLEAN_DATATYPE:
                        case SqlUtils.ORACLE_NATIVE_BOOLEAN_DATATYPE:
                        case SqlUtils.ORACLE_JSON_DATATYPE:
                        case SqlUtils.ORACLE_VECTOR_DATATYPE:
                        case SqlUtils.ORACLE_VECTOR_BINARY_DATATYPE:
                        case SqlUtils.ORACLE_VECTOR_SPARSE_DATATYPE:
                        case SqlUtils.ORACLE_NULL_DATATYPE:
                        case SqlUtils.ORACLE_TIMESTAMP_DATATYPE:
                        case SqlUtils.ORACLE_TIMESTAMPTZ_DATATYPE:
                        case SqlUtils.ORACLE_TIMESTAMPLTZ_DATATYPE:
                        case SqlUtils.ORACLE_INTERVAL_YEAR_TO_MONTH_DATATYPE:
                        case SqlUtils.ORACLE_INTERVAL_DAY_TO_SECOND_DATATYPE:
                        case SqlUtils.MCPDBWIZARD_READONLYROWSET:
                        case SqlUtils.ORACLE_XMLTYPE_DATATYPE: {
                            // Native JSON/VECTOR/BOOLEAN ride the scalar pass-through path
                            // (no createXFromY wrapper-class conversion).
                            break;
                        }
                        case SqlUtils.ORACLE_SDO_GEOMETRY_DATATYPE: {
                            //extraFrontBit = "JGeometryWrapper.jGeometryUnWrapper(";
                            //extraBackBit = ")";
                            break;
                        }
                        default: {
                            extraFrontBit = variableDataType[i] + ".create" + variableDataType[i] + "From" + variableWSDataType[i] + "(";
                            extraBackBit = ")";
                            break;
                        }
                    }

                    if (theRowSet.getInt("POSITION") > 0 && (theRowSet.getString("IN_OUT").equals("IN")
                            || theRowSet.getString("IN_OUT").equals("IN/OUT")
                            || overRideInOut.equals("IN/OUT"))
                    ) {

                        switch (oracleUnderlyingDatatype[i]) {
                            case SqlUtils.ORACLE_XMLTYPE_DATATYPE: {
                                String commentOutString = "";

                                if (targetVersion.startsWith("8.1") || targetVersion.startsWith("9") || targetVersion.equals("10.1.0")) {
                                    serviceCode.print("// This code is being created for Oracle V" + targetVersion + ". Access to XMLType");
                                    serviceCode.print("// is not supported by " + Namer.param_prod_name + " prior to Oracle V10.2.0 ");
                                    serviceCode.print(" ");
                                    commentOutString = "// ";
                                }


                                serviceCode.print(commentOutString + " ");
                                if (comments && xmltypeNotWarned) {
                                    serviceCode.print("// Passing in a zero length XMLType is risky - it will raise this error if the procedure attempts ");
                                    serviceCode.print("// to parse it: ");
                                    serviceCode.print("// ORA-19032: Expected XML tag , got no content ");
                                    serviceCode.print("// But there's no way to create a Null XMLType for some reason...");
                                    xmltypeNotWarned = false;
                                }
                                serviceCode.print(commentOutString + "if (" + variableName[i] + " != null && " + variableName[i] + ".length > 0)");
                                serviceCode.indent();
                                serviceCode.print(commentOutString + "{");
                                //serviceCode.print(commentOutString + "try");
                                //serviceCode.indent();
                                //serviceCode.print(commentOutString + "{");
                                serviceCode.print(commentOutString + "theService." + setMethod + "(" + variableName[i] + ");");
                                //serviceCode.print(commentOutString + "}");
                                //serviceCode.unIndent();
                                //serviceCode.print(commentOutString + "catch (java.sql.SQLException e)");
                                //serviceCode.indent();
                                //serviceCode.print(commentOutString + "{");
                                //serviceCode.print(commentOutString + "throw new CSException(\"" +  variableName[i]  + ": XMLType input parameter could not be loaded:\" + e.getMessage());");
                                //serviceCode.print(commentOutString + "}");
                                //serviceCode.unIndent();
                                serviceCode.print(commentOutString + "}");
                                serviceCode.unIndent();

                                serviceCode.print(commentOutString + "else");
                                serviceCode.indent();
                                serviceCode.print(commentOutString + "{");
                                //serviceCode.print(commentOutString + "try");
                                //serviceCode.indent();
                                //serviceCode.print(commentOutString + "{");

                                serviceCode.print(commentOutString + "theService." + setNullMethod + "();");
                                //serviceCode.print(commentOutString + "}");
                                //serviceCode.unIndent();
                                //serviceCode.print(commentOutString + "catch (java.sql.SQLException e)");
                                //serviceCode.indent();
                                //serviceCode.print(commentOutString + "{");
                                //serviceCode.print(commentOutString + "throw new CSException(\"" +  variableName[i]  + ": Null XMLType input parameter could not be loaded:\" + e.getMessage());");
                                //serviceCode.print(commentOutString + "}");
                                //serviceCode.unIndent();
                                serviceCode.print(commentOutString + "}");
                                serviceCode.unIndent();


                                if (commentOutString.length() > 0) {
                                    serviceCode.print(stheLog + ".error(\"" + variableName[i] + ": XMLType could not be created because target database is Oracle V" + targetVersion + "\"); ");
                                }
                                break;
                            }
                            case SqlUtils.ORACLE_BFILE_DATATYPE: {
                                String commentOutString = "";

                                if (targetVersion.equals("8.1.5") || targetVersion.equals("8.1.6")) {
                                    serviceCode.print("// This code is being created for Oracle V" + targetVersion + ". Access to BFILEs");
                                    serviceCode.print("// is not supported by " + Namer.param_prod_name + " prior to Oracle V8.1.7 ");
                                    serviceCode.print(" ");
                                    commentOutString = "// ";
                                } else {
                                    needBfileCreationRoutine = true;
                                }

                                serviceCode.print(commentOutString + " ");
                                serviceCode.print(commentOutString + "if (" + variableName[i] + " != null && " + variableName[i] + ".length > 0)");
                                serviceCode.indent();
                                serviceCode.print(commentOutString + "{");
                                serviceCode.print(commentOutString + "try");
                                serviceCode.indent();
                                serviceCode.print(commentOutString + "{");
                                serviceCode.print(commentOutString + "oracle.sql.BFILE tempBFILE = createNewBfilePointer(\"" + serviceMethod + "\",\"" + variableName[i] + "\"," + variableName[i] + "); ");
                                serviceCode.print(commentOutString + "theService." + setMethod + "(tempBFILE);");
                                serviceCode.print(commentOutString + "}");
                                serviceCode.unIndent();
                                serviceCode.print(commentOutString + "catch (Exception e)");
                                serviceCode.indent();
                                serviceCode.print(commentOutString + "{");
                                serviceCode.print(commentOutString + "throw new CSException(\"" + variableName[i] + ": BFILE pointer could not be created:\" + e.getMessage());");
                                serviceCode.print(commentOutString + "}");
                                serviceCode.unIndent();
                                serviceCode.print(commentOutString + "}");
                                serviceCode.unIndent();

                                if (commentOutString.length() > 0) {
                                    serviceCode.print(stheLog + ".error(\"" + variableName[i] + ": BFILE pointer could not be created because target database is Oracle V" + targetVersion + "\"); ");
                                }
                                break;
                            }
                            case SqlUtils.ORACLE_BLOB_DATATYPE:
                            case SqlUtils.ORACLE_CLOB_DATATYPE: {
                                serviceCode.print(" ");
                                serviceCode.print("if (" + variableName[i] + " != null && " + variableName[i] + ".length > 0)");
                                serviceCode.indent();
                                serviceCode.print("{");
                                //serviceCode.print("try");
                                //serviceCode.indent();
                                //serviceCode.print("{");
               /*
               serviceCode.print("java.io.File tempFile = loadByteArrayIntoFile(" + variableName[i] +");");
               serviceCode.print("theService." + setMethod + "(tempFile);");
               */
                                serviceCode.print("theService." + setMethod + "(" + variableName[i] + ");");
                                //serviceCode.print("}");
                                //serviceCode.unIndent();
                                //serviceCode.print("catch (Exception e)");
                                //serviceCode.indent();
                                //serviceCode.print("{");
                                //serviceCode.print("throw new CSException(\"" +  variableName[i]  + ": LOB pointer could not be created:\" + e.getMessage());");
                                //serviceCode.print("}");
                                //serviceCode.unIndent();
                                serviceCode.print("}");
                                serviceCode.unIndent();
                                break;
                            }
                            case SqlUtils.ORACLE_OTHER_DATATYPE:
                            case SqlUtils.MCPDBWIZARD_READONLYROWSET: {
                                if (comments) serviceCode.print("// You can't bind a cursor as an input variable...");
                                serviceCode.print("// theService." + setMethod + "(" + extraFrontBit + variableName[i] + extraBackBit + ");");
                                break;
                            }
                            case SqlUtils.ORACLE_TIMESTAMPTZ_DATATYPE:
                            case SqlUtils.ORACLE_TIMESTAMPLTZ_DATATYPE: {
                                serviceCode.print("theService." + setMethod + "(" + extraFrontBit + variableName[i] + extraBackBit + ");");
                                break;
                            }
                            case SqlUtils.ORACLE_VARRAY_DATATYPE:
                            case SqlUtils.ORACLE_TABLE_DATATYPE:
                            case SqlUtils.ORACLE_PLSQL_INDEXBY_ROWTYPE_DATATYPE: {
                                serviceCode.print(" ");    //EVS
                                serviceCode.print("if (" + variableName[i] + " != null)");
                                serviceCode.indent();
                                serviceCode.print("{");

                                if (theRecords[extraObjectId[i]].typeImplementingClass != null) {
                                    theRecords[extraObjectId[i]].typeImplementingClass.theEngine.addArrayBfileDefaultCode(serviceCode, variableWSParentDataType[i], comments, debugMessages, otherMessages, variableName[i], variableWSParentDataType[i] + ".create" + variableWSParentDataType[i] + "ArrayFrom" + JavaUtils.replaceString(variableWSDataType[i], "[]", "") + "Array(" + variableName[i] + ");"
                                            , serviceMethod, "theService." + setMethod + "(" + extraFrontBit + variableName[i] + extraBackBit + ");"
                                            , "theService." + setMethod + "(" + "new " + variableDataType[i] + "(" + stheLog + ", theArray));");
                                } else {
                                    if (theRecords[extraObjectId[i]].typeRecordClass.equals("oracle.sql.BLOB")) {
                                        serviceCode.print("theService." + setMethod + "(new " + theRecords[extraObjectId[i]].javaName + "(" + stheLog + ", (createBLOBarrayFromByteArray(" + variableName[i] + "))));");
                                    } else if (theRecords[extraObjectId[i]].typeRecordClass.equals("oracle.sql.OPAQUE")) {                                                                                                                                    //DRKLUGE
                                        serviceCode.print("theService." + setMethod + "(new " + theRecords[extraObjectId[i]].javaName + "(" + stheLog + ", (createOPAQUEarrayFromCharArray(" + variableName[i] + ",theConnection,\"SYS.XMLTYPE\"))));");
                                    } else if (theRecords[extraObjectId[i]].typeRecordClass.equals("oracle.sql.CLOB")) {
                                        serviceCode.print("theService." + setMethod + "(new " + theRecords[extraObjectId[i]].javaName + "(" + stheLog + ", (createCLOBarrayFromCharArray(" + variableName[i] + "))));");
                                    } else if (theRecords[extraObjectId[i]].typeRecordClass.equals("oracle.sql.BFILE")) {
                                        serviceCode.print("theService." + setMethod + "(new " + theRecords[extraObjectId[i]].javaName + "(" + stheLog + ", (createBFILEarrayFromByteArray(" + variableName[i] + "))));");
                                    } else {
                                        serviceCode.print("theService." + setMethod + "(" + extraFrontBit + variableName[i] + extraBackBit + ");");
                                    }
                                }

                                serviceCode.print("}");    //EVS
                                serviceCode.unIndent();
                                serviceCode.print("else");    //EVS
                                serviceCode.indent();
                                serviceCode.print("{");
                                serviceCode.print("theService." + setNullMethod + "();");
                                serviceCode.print("}");
                                serviceCode.unIndent();

                                break;
                            }
                            case SqlUtils.ORACLE_ROWTYPE_DATATYPE: //DRKLUGE
                            {
                                boolean reallyDoesHaveBfiles = false;
                                int oldRowNumber = theRowSet.getCurrentRowNumber();
                                for (int j = 0; j < theRowSet.size(); j++) {
                                    theRowSet.setCurrentRowNumber(j);


                                    if (oracleParamDatatype[j].equals("OracleTypes.BFILE")) {
                                        reallyDoesHaveBfiles = true;
                                    }
                                }
                                theRowSet.setCurrentRowNumber(oldRowNumber);

                                serviceCode.print(" ");    //EVS
                                serviceCode.print("if (" + variableName[i] + " != null)");
                                serviceCode.indent();
                                serviceCode.print("{");

                                if (theRecords[extraObjectId[i]].theEngine != null && reallyDoesHaveBfiles && theFlags.getFlag(GlobalFlags.HAS_BFILES)) {
                                    theRecords[extraObjectId[i]].theEngine.addRecordBfileDefaultCode(serviceCode, /*variableWSDataType[i]*/ variableDataType[i], comments, debugMessages, otherMessages, variableName[i], variableWSParentDataType[i] + ".create" + variableWSParentDataType[i] + "ArrayFrom" + JavaUtils.replaceString(variableWSDataType[i], "[]", "") + "Array(" + variableName[i] + ");"
                                            , serviceMethod, variableDataType[i] + ".create" + variableDataType[i] + "From" + JavaUtils.replaceString(variableDataType[i], "[]", "") + JavaUtils.getJavaName("_attrs", javaNamingConvention, theLog) + "(" + variableName[i] + ");"
                                            , "theService." + setMethod + "(theRow);");

                                    //theRecords[extraObjectId[i]].theEngine.addRecordBfileDefaultCode(serviceCode, /*variableWSDataType[i]*/ variableDataType[i], comments,debugMessages,otherMessages, variableName[i],variableWSParentDataType[i] + ".create"+variableWSParentDataType[i]+"ArrayFrom"+ JavaUtils.replaceString(variableWSDataType[i],"[]","")  +"Array("+variableName[i]+");"
                                    //,serviceMethod, "theService." + setMethod + "(" + variableDataType[i] + ".create"+variableDataType[i]+"From"+ JavaUtils.replaceString(variableDataType[i],"[]","") +JavaUtils.getJavaName("_attrs",javaNamingConvention, theLog)   +"("+variableName[i]+"));"
                                    //,"theService." + setMethod + "(theRow);");

                                } else {
                                    serviceCode.print("theService." + setMethod + "(" + extraFrontBit + variableName[i] + extraBackBit + ");");
                                }
                                serviceCode.print("}");    //EVS
                                serviceCode.unIndent();
                                serviceCode.print("else");    //EVS
                                serviceCode.indent();
                                serviceCode.print("{");
                                serviceCode.print("theService." + setNullMethod + "();");
                                serviceCode.print("}");
                                serviceCode.unIndent();

                                break;
                            }
                            case SqlUtils.ORACLE_BINARY_DATATYPE: {
                                //DRKLUGE
                                //setMethod = JavaUtils.getJavaName("set_" + variableName[i] + "_byte_array" ,javaNamingConvention, theLog);
                                serviceCode.print("theService." + setMethod + "(" + extraFrontBit + variableName[i] + extraBackBit + ");");
                                break;
                            }
                            case SqlUtils.ORACLE_NUMBER_DATATYPE: {
                                if (wsJavaNumberTypeComboBox.equals("java.math.BigDecimal")) {
                                    serviceCode.print("theService." + setMethod + "(" + extraFrontBit + variableName[i] + extraBackBit + ");");
                                } else {
                                    serviceCode.print(" ");
                                    serviceCode.print("if (minValueIsNULL && "
                                            + extraFrontBit + variableName[i] + extraBackBit + " == " + JavaUtils.mapScalarTypeToObjectType(wsJavaNumberTypeComboBox) + ".MIN_VALUE)");
                                    serviceCode.indent();
                                    serviceCode.print("{");
                                    serviceCode.print("theService." + setNullMethod + "();");
                                    serviceCode.print("}");
                                    serviceCode.unIndent();
                                    serviceCode.print("else");
                                    serviceCode.indent();
                                    serviceCode.print("{");
                                    serviceCode.print("theService." + setMethod + "(" + extraFrontBit + variableName[i] + extraBackBit + ");");
                                    serviceCode.print("}");
                                    serviceCode.print(" ");
                                    serviceCode.unIndent();

                                }
                                break;
                            }
                            default: {
                                serviceCode.print("theService." + setMethod + "(" + extraFrontBit + variableName[i] + extraBackBit + ");");
                                break;
                            }
                        }
                        //serviceCode.print("");
                    }

                }
            } //WSALL

            if (WScallType == CallableStatementParameterEngine.WS_CALL_TYPE_ALL) {

                /**
                 if (statementType == SqlUtils.SELECT)
                 {
                 if (comments)        //DRKLUGE
                 {
                 serviceCode.print("// Make sure lobs are downloaded to files");
                 }
                 serviceCode.print("theService.setKeepLobs(false);");
                 serviceCode.print("");
                 }
                 **/

                if (servicePreCallStubFlag) {
                    serviceCode.print("");
                    if (comments)
                        serviceCode.print("// See if there is anything we need to do before we call the service.");
                    serviceCode.print("doPreServiceEvent(theService);");
                }

                serviceCode.print("");

                if (statementType == SqlUtils.SELECT) {
                    if (comments) {
                        serviceCode.print("// Call Query");
                    }
                    if (debugMessages) {
                        serviceCode.print(stheLog + ".debug(\"" + serviceMethod + ": Call Query\");");
                    }

                    if (returnType.equals(sWsdlFileName)) {
                        serviceCode.print("serviceOutput = createWSDLRowset(theService.executeQuery());");
                    } else {
                        if (webServicesFlag) {
                            serviceCode.print("serviceOutput = (" + procPackageName + "." + theAspWrangler.cursorAttrFileName + "[])" + procPackageName + "." + procClassName + ".getArrayFromReadOnlyRowSet(theService.executeQuery(), " + stheLog + ", true );");
                        } else {
                            serviceCode.print("serviceOutput = (" + procPackageName + "." + theAspWrangler.cursorAttrFileName + "[])" + procPackageName + "." + procClassName + ".getArrayFromReadOnlyRowSet(theService.executeQuery(), " + stheLog + ");");
                        }
                    }
                } else if (procPackageName.endsWith(".sql")) {
                    if (comments) {
                        serviceCode.print("// Call statement");
                    }
                    if (debugMessages) {
                        serviceCode.print(stheLog + ".debug(\"" + serviceMethod + ": Call statement\");");
                    }
                    serviceCode.print("theService.executeCall();");
                } else {
                    if (comments) {
                        serviceCode.print("// Call procedure");
                    }
                    if (debugMessages) {
                        serviceCode.print(stheLog + ".debug(\"" + serviceMethod + ": Call procedure\");");
                    }
                    serviceCode.print("theService.executeProc();");
                }
                serviceCode.print("");
            } //WS ALL

            if (WScallType == CallableStatementParameterEngine.WS_CALL_TYPE_GET
                    || WScallType == CallableStatementParameterEngine.WS_CALL_TYPE_ALL) {
                if (!returnType.equals("void")) {
                    if (comments) {
                        serviceCode.print("// Unload results");
                    }

                    if (debugMessages && theRowSet.size() > 1) {
                        serviceCode.print(stheLog + ".debug(\"" + serviceMethod + ": Unload results\");");
                    }

                    for (int i = 0; i < theRowSet.size(); i++) {
                        theRowSet.setCurrentRowNumber(i);
                        //String setMethod = JavaUtils.getJavaName("set_" + variableName[i], javaNamingConvention, theLog);

                        String setMethod = JavaUtils.getJavaName("set_" + variableName[i], javaNamingConvention, theLog);
                        String getMethod = JavaUtils.getJavaName("get_" + variableName[i], javaNamingConvention, theLog);


                        if (javaNamingConvention.equals("spaces_between_words.java")) {
                            String currentVariableName = new String(variableName[i]);
                            String initcapVariableName = new String(currentVariableName.substring(0, 1).toUpperCase());
                            if (currentVariableName.length() > 1) {
                                initcapVariableName = initcapVariableName + currentVariableName.substring(1);
                            }

                            setMethod = new String("set" + initcapVariableName);
                        }

                        if (theRowSet.getInt("POSITION") >= 0 && (theRowSet.getString("IN_OUT").equals("OUT")
                                || theRowSet.getString("IN_OUT").equals("IN/OUT")
                                || overRideInOut.equals("IN/OUT"))
                        ) {
                            String assignBit = "";

                            switch (oracleUnderlyingDatatype[i]) {
                                case SqlUtils.ORACLE_ROWID_DATATYPE:
                                case SqlUtils.ORACLE_UROWID_DATATYPE: {
                                    getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_string", javaNamingConvention, theLog);
                                    serviceCode.print("if (theService." + getMethod + "()" + " == null)");
                                    serviceCode.indent();
                                    serviceCode.print("{");
                                    if (outParams > 1) {
                                        if (webServiceRecType.equals("protected")) {
                                            serviceCode.print("serviceOutput." + setMethod + "(null);");
                                        } else {
                                            serviceCode.print("serviceOutput." + variableName[i] + " = null;");
                                        }
                                    } else // outParams == 1
                                    {
                                        serviceCode.print("serviceOutput = null;");
                                    }
                                    serviceCode.print("}");
                                    serviceCode.unIndent();
                                    serviceCode.print("else");
                                    serviceCode.indent();
                                    serviceCode.print("{");
                                    assignBit = "theService." + getMethod + "()";

                                    if (outParams > 1) {
                                        if (webServiceRecType.equals("protected")) {
                                            serviceCode.print("serviceOutput." + setMethod + "(" + assignBit + ");");
                                        } else {
                                            serviceCode.print("serviceOutput." + variableName[i] + " = " + assignBit + ";");
                                        }
                                    } else // outParams == 1
                                    {
                                        serviceCode.print("serviceOutput = " + assignBit + ";");
                                    }
                                    serviceCode.print("}");
                                    serviceCode.unIndent();
                                    break;
                                }
                            }

                            switch (oracleUnderlyingDatatype[i]) {
                                case SqlUtils.ORACLE_LONGTEXT_DATATYPE: {
                                    getMethod = JavaUtils.getJavaName("get_" + variableName[i], javaNamingConvention, theLog);
                                    assignBit = "loadFileIntoByteArray(theService."
                                            + getMethod + "(),theService." + JavaUtils.getJavaName("get_" + variableName[i] + "_byte_array", javaNamingConvention, theLog) + "())";
                                    break;
                                }
                                case SqlUtils.ORACLE_LONG_BINARY_DATATYPE: {
                                    getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_byte_array", javaNamingConvention, theLog);
                                    //assignBit = "loadFileIntoByteArray(theService."
                                    //    + getMethod + "(),theService."+JavaUtils.getJavaName("get_" + variableName[i] + "_byte_array", javaNamingConvention, theLog)+"())";
                                    assignBit = "theService." + getMethod + "()";
                                    break;
                                }
                                case SqlUtils.ORACLE_CLOB_DATATYPE:
                                case SqlUtils.ORACLE_BLOB_DATATYPE:
                                case SqlUtils.ORACLE_XMLTYPE_DATATYPE:
                                case SqlUtils.ORACLE_BFILE_DATATYPE: {
                                    getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_byte_array", javaNamingConvention, theLog);
                                    ///assignBit = "loadFileIntoByteArray(theService."
                                    // + getMethod + "(),theService."+JavaUtils.getJavaName("get_" + variableName[i] + "_byte_array", javaNamingConvention, theLog)+"())";
                                    assignBit = "theService." + getMethod + "()";
                                    break;
                                }
                                case SqlUtils.ORACLE_OBJECT_DATATYPE: {
                                    assignBit = "theService." + getMethod + "()";
                                    break;
                                }
                                case SqlUtils.ORACLE_TABLE_DATATYPE:
                                case SqlUtils.ORACLE_VARRAY_DATATYPE:
                                case SqlUtils.ORACLE_PLSQL_INDEXBY_ROWTYPE_DATATYPE: {
                                    if (hasChildFiles) {
                                        serviceCode.print("theService." + getMethod + "().moveLobsToByteArrays(getLoader());");
                                    }

                                    if (theRecords[extraObjectId[i]].typeRecordClass.equals("oracle.sql.INTERVALYM")
                                            || theRecords[extraObjectId[i]].typeRecordClass.equals("oracle.sql.INTERVALDS")
                                            || theRecords[extraObjectId[i]].typeRecordClass.equals("oracle.sql.TIMESTAMP")
                                            || theRecords[extraObjectId[i]].typeRecordClass.equals("oracle.sql.TIMESTAMPTZ")
                                            || theRecords[extraObjectId[i]].typeRecordClass.equals("oracle.sql.TIMESTAMPLTZ")
                                    ) {
                                        assignBit = "theService." + getMethod + "().getCurrentValuesAsByteArray()";
                                    } else if (theRecords[extraObjectId[i]].typeRecordClass.equals("oracle.sql.BLOB")) {
                                        assignBit = "createByteArrayFromBLOBArray(theService." + getMethod + "().getCurrentValues())";
                                    } else if (theRecords[extraObjectId[i]].typeRecordClass.equals("oracle.sql.CLOB")) {
                                        if (useCharForCLOB) {
                                            assignBit = "createCharArrayFromCLOBArray(theService." + getMethod + "().getCurrentValues())";
                                        } else {
                                            assignBit = "createByteArrayFromCLOBArray(theService." + getMethod + "().getCurrentValues())";
                                        }
                                    } else if (theRecords[extraObjectId[i]].typeRecordClass.equals("oracle.sql.OPAQUE")) {
                                        if (useCharForCLOB) {
                                            assignBit = "createCharArrayFromOPAQUEArray(theService." + getMethod + "().getCurrentValues())";
                                        } else {
                                            assignBit = "createByteArrayFromOPAQUEArray(theService." + getMethod + "().getCurrentValues())";
                                        }
                                    } else if (theRecords[extraObjectId[i]].typeRecordClass.equals("oracle.sql.BFILE")) {
                                        assignBit = "createByteArrayFromBFILEArray(theService." + getMethod + "().getCurrentValues())";
                                    } else {
                                        assignBit = "theService." + getMethod + "().getCurrentValues()";
                                    }
                                    break;
                                }
                                case SqlUtils.MCPDBWIZARD_READONLYROWSET: {
                                    if (cursorRecordId[i] > Integer.MIN_VALUE) {
                                        if (sqlOrPlsql.equals("_sql_")) {
                                            assignBit = "(" + procPackageName + "." + variableWSDataType[i] + "[])" + procPackageName + "." + variableWSParentDataType[i] + ".create" + variableWSParentDataType[i] + "ArrayFromRowSet(theService." + getMethod + "()," + stheLog + ")";
                                        } else {
                                            assignBit = "(" + variableWSDataType[i] + "[])" + variableWSParentDataType[i] + ".create" + variableWSParentDataType[i] + "ArrayFromRowSet(theService." + getMethod + "()," + stheLog + ")";
                                        }
                                    } else {
                                        assignBit = "createWSDLRowset(theService." + getMethod + "())";
                                    }


                                    break;
                                }
                                case SqlUtils.ORACLE_ROWTYPE_DATATYPE: {
                                    if (hasChildFiles) {
                                        serviceCode.print("theService." + getMethod + "().moveLobsToByteArrays(getLoader());");
                                    }
                                    //assignBit = "theService." + getMethod + "()";
                                    // AllNormalDatatypes.createAllNormalDatatypesAttrsFromAllNormalDatatypes(theService.getParamOutParam());
                                    if (variableWSDataType[i] == null) {
                                        assignBit = "theService." + getMethod + "()"; // may not be reachable
                                    } else {
                                        assignBit = variableDataType[i] + ".create" + variableWSDataType[i] + "From" + variableDataType[i] + "(theService." + getMethod + "())";
                                    }

                                    break;
                                }
                                case SqlUtils.ORACLE_PLSQL_INDEXBY_DATATYPE: {

                                    if (plsqlIndexByDataType[i] == OracleTypes.VARCHAR) {
                                        assignBit = "createStringArrayFromIndexByTable(theService." + getMethod + "())";
                                    } else {
                                        if (variableWSDataType[i].equals("java.math.BigDecimal[]")) {
                                            //DRKLUGE
                                            assignBit = "createBigDecimalArrayFromIndexByTable(theService." + getMethod + "())";
                                        } else if (variableWSDataType[i].equals("int[]")) {
                                            assignBit = "createIntegerArrayFromBigDecimalArray(createBigDecimalArrayFromIndexByTable(theService." + getMethod + "()))";
                                        } else if (variableWSDataType[i].equals("long[]")) {
                                            assignBit = "createLongArrayFromBigDecimalArray(createBigDecimalArrayFromIndexByTable(theService." + getMethod + "()))";
                                        } else if (variableWSDataType[i].equals("double[]")) {
                                            assignBit = "createDoubleArrayFromBigDecimalArray(createBigDecimalArrayFromIndexByTable(theService." + getMethod + "()))";
                                        } else if (variableWSDataType[i].equals("float[]")) {
                                            assignBit = "createFloatArrayFromBigDecimalArray(createBigDecimalArrayFromIndexByTable(theService." + getMethod + "()))";
                                        } else if (variableWSDataType[i].equals("String[]")) {
                                            // The zoned-element counterpart of the IN branch above:
                                            // the zone comes back as text because java.sql.Timestamp
                                            // has nowhere to put one.
                                            assignBit = "createStringArrayFromIndexByTable(theService." + getMethod + "())";
                                        } else if (variableWSDataType[i].equals("java.sql.Timestamp[]")) {
                                            assignBit = "createTimestampArrayFromIndexByTable(theService." + getMethod + "())";
                                        } else if (variableWSDataType[i].equals("byte[][]")) {
                                            assignBit = "createByteArrayFromIndexByTable(theService." + getMethod + "())";
                                        } else if (variableWSDataType[i].equals("char[][]")) {
                                            assignBit = "createCharArrayFromIndexByTable(theService." + getMethod + "())"; //DRKLUGE
                                        } else {
                                            assignBit = "createBigDecimalArrayFromIndexByTable(theService." + getMethod + "())";
                                        }
                                    }
                                    break;
                                }
                                case SqlUtils.ORACLE_NUMBER_DATATYPE: {

                                    getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_BigDecimal_Obj", javaNamingConvention, theLog);
                                    if (variableWSDataType[i].equals("java.math.BigDecimal")) {
                                        assignBit = "theService." + getMethod + "()";
                                    } else {
                                        assignBit = "theService." + getMethod + "()." + variableWSDataType[i] + "Value()";
                                    }

                                    if (variableWSDataType[i].equals("java.math.BigDecimal")) {
                                        if (outParams > 1) {
                                            if (webServiceRecType.equals("protected")) {
                                                serviceCode.print("serviceOutput." + setMethod + "(" + assignBit + ");");
                                            } else {
                                                serviceCode.print("serviceOutput." + variableName[i] + " = " + assignBit + ";");
                                            }
                                        } else // outParams == 1
                                        {
                                            serviceCode.print("serviceOutput = " + assignBit + ";");
                                        }
                                    } else {
                                        serviceCode.print("");
                                        serviceCode.print("if (theService." + getMethod + "() == null)");
                                        serviceCode.indent();
                                        if (comments)
                                            serviceCode.print("// From build 6.0.2724 we throw an exception if we are being asked to convert Oracle's NULL to a ");
                                        if (comments) serviceCode.print("// scalar and have not set minValueIsNULL ");
                                        serviceCode.print("{");
                                        serviceCode.print("if (minValueIsNULL)");
                                        serviceCode.indent();
                                        serviceCode.print("{");

                                        if (outParams > 1) {
                                            if (webServiceRecType.equals("protected")) {
                                                serviceCode.print("serviceOutput." + setMethod + "(" + JavaUtils.mapScalarTypeToObjectType(wsJavaNumberTypeComboBox) + ".MIN_VALUE);");
                                            } else {
                                                serviceCode.print("serviceOutput." + variableName[i] + " = " + JavaUtils.mapScalarTypeToObjectType(wsJavaNumberTypeComboBox) + ".MIN_VALUE;");
                                            }
                                        } else // outParams == 1
                                        {
                                            serviceCode.print("serviceOutput = " + JavaUtils.mapScalarTypeToObjectType(wsJavaNumberTypeComboBox) + ".MIN_VALUE;");
                                        }

                                        serviceCode.print("}");
                                        serviceCode.unIndent();
                                        serviceCode.print("else");
                                        serviceCode.indent();
                                        serviceCode.print("{");

                                        serviceCode.print("throw new CSException(\"" + serviceMethod + ": Unable to map Oracle NULL to MIN_VALUE for Java Scalar Variable " + variableName[i] + "\");");
                                        serviceCode.print("}");
                                        serviceCode.unIndent();
                                        serviceCode.print("}");
                                        serviceCode.unIndent();

                                        serviceCode.print("else");
                                        serviceCode.indent();
                                        serviceCode.print("{");

                                        if (outParams > 1) {
                                            if (webServiceRecType.equals("protected")) {
                                                serviceCode.print("serviceOutput." + setMethod + "(" + assignBit + ");");
                                            } else {
                                                serviceCode.print("serviceOutput." + variableName[i] + " = " + assignBit + ";");
                                            }
                                        } else // outParams == 1
                                        {
                                            serviceCode.print("serviceOutput = " + assignBit + ";");
                                        }

                                        serviceCode.print("}");
                                        serviceCode.unIndent();
                                        serviceCode.print(" ");
                                    }


                                    /**
                                     serviceCode.print("if (theService." + getMethod +"() == null)");
                                     serviceCode.indent();
                                     if (comments)  serviceCode.print("// From build 6.0.2724 we throw an exception if we are being asked to convert Oracle's NULL to a ");
                                     if (comments)  serviceCode.print("// scalar and have not set minValueIsNULL ");
                                     serviceCode.print("{");
                                     serviceCode.print("if (minValueIsNULL)");
                                     serviceCode.indent();
                                     serviceCode.print("{");

                                     if (outParams > 1)
                                     {
                                     serviceCode.print("serviceOutput." + setMethod + "(" + JavaUtils.mapScalarTypeToObjectType(wsJavaNumberTypeComboBox) + ".MIN_VALUE);");
                                     }
                                     else // outParams == 1
                                     {
                                     serviceCode.print("serviceOutput = " + JavaUtils.mapScalarTypeToObjectType(wsJavaNumberTypeComboBox) + ".MIN_VALUE;");
                                     }

                                     serviceCode.print("}");
                                     serviceCode.unIndent();
                                     serviceCode.print("else");
                                     serviceCode.indent();
                                     serviceCode.print("{");

                                     serviceCode.print("throw new CSException(\"" + serviceMethod  + ": Unable to map Oracle NULL to MIN_VALUE for Java Scalar Variable " + variableName[i] + "\");");
                                     serviceCode.print("}");
                                     serviceCode.unIndent();
                                     serviceCode.print("}");
                                     serviceCode.unIndent();

                                     serviceCode.print("else");
                                     serviceCode.indent();
                                     serviceCode.print("{");

                                     if (outParams > 1)
                                     {
                                     serviceCode.print("serviceOutput." + setMethod + "(" + assignBit + ");");
                                     }
                                     else // outParams == 1
                                     {
                                     serviceCode.print("serviceOutput = " + assignBit + ";");
                                     }

                                     serviceCode.print("}");
                                     serviceCode.unIndent();
                                     serviceCode.print(" ");
                                     **/


                                    break;

                                }
                                case SqlUtils.ORACLE_BOOLEAN_DATATYPE: {
                                    getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_boolean", javaNamingConvention, theLog);
                                    //assignBit = "theService." + getMethod +"()." + variableWSDataType[i] +"Value()";
                                    assignBit = "theService." + getMethod + "(false)";    //DRKLUGE
                                    break;
                                }
                                case SqlUtils.ORACLE_OTHER_DATATYPE: {
                                    assignBit = "null";
                                    break;
                                }
                                case SqlUtils.ORACLE_BINARY_DATATYPE: {
                                    //getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_byteArray", javaNamingConvention, theLog);
                                    getMethod = JavaUtils.getJavaName("get_" + variableName[i], javaNamingConvention, theLog);
                                    //DRKLUGE
                                    //getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_byte_array" ,javaNamingConvention, theLog);
                                    assignBit = "theService." + getMethod + "()";
                                    break;
                                }
                                case SqlUtils.ORACLE_TIMESTAMPTZ_DATATYPE:
                                case SqlUtils.ORACLE_TIMESTAMPLTZ_DATATYPE: {
                                    getMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_byte_array", javaNamingConvention, theLog);
                                    assignBit = "theService." + getMethod + "()";
                                    break;
                                }
                                case SqlUtils.ORACLE_TIMESTAMP_DATATYPE: {
                                    getMethod = JavaUtils.getJavaName("get_" + variableName[i], javaNamingConvention, theLog) + "String";
                                    assignBit = "theService." + getMethod + "()";
                                    break;
                                }
                                case SqlUtils.ORACLE_INTERVAL_DAY_TO_SECOND_DATATYPE:
                                case SqlUtils.ORACLE_INTERVAL_YEAR_TO_MONTH_DATATYPE: {
                                    // The service-output field is a String and the wrapper's plain
                                    // getParam<Cap>() returns an oracle.sql.INTERVALDS/YM; read the
                                    // String variant instead (mirrors the TIMESTAMP case above).
                                    // Without this the ServiceImpl won't compile for an INTERVAL OUT.
                                    getMethod = JavaUtils.getJavaName("get_" + variableName[i], javaNamingConvention, theLog) + "String";
                                    assignBit = "theService." + getMethod + "()";
                                    break;
                                }
                                case SqlUtils.ORACLE_DATE_DATATYPE: {
                                    //getMethod = JavaUtils.getJavaName("get_" + variableName[i] +"_date", javaNamingConvention, theLog);
                                    getMethod = JavaUtils.getJavaName("get_" + variableName[i], javaNamingConvention, theLog);
                                    assignBit = "theService." + getMethod + "()";
                                    break;
                                }
                                ///case SqlUtils.ORACLE_SDO_GEOMETRY_DATATYPE:
                                // {
                                //  //getMethod = JavaUtils.getJavaName("get_" + variableName[i] +"_date", javaNamingConvention, theLog);
                                //  getMethod = JavaUtils.getJavaName("get_" + variableName[i] , javaNamingConvention, theLog);
                                //  assignBit = "JGeometryWrapper.createWrappedClass(theService." + getMethod +"())";
                                //  break;
                                //  }
                                default: {
                                    assignBit = "theService." + getMethod + "()";
                                    break;
                                }
                            } //switch

                            switch (oracleUnderlyingDatatype[i]) {
                                case SqlUtils.ORACLE_ROWID_DATATYPE:
                                case SqlUtils.ORACLE_UROWID_DATATYPE:
                                case SqlUtils.ORACLE_NUMBER_DATATYPE: {
                                    // We've already done rowid  and number
                                    break;
                                }
                                case SqlUtils.ORACLE_XMLTYPE_DATATYPE: {
                                    // We need to catch a SQL Exception...
                                    serviceCode.print("");
                                    serviceCode.print("try");
                                    serviceCode.indent();
                                    serviceCode.print("{");

                                    if (outParams > 1) {
                                        serviceCode.print("if (theService." + getMethod + "() != null)");
                                        serviceCode.indent();
                                        serviceCode.print("{");
                                        if (webServiceRecType.equals("protected")) {
                                            serviceCode.print("serviceOutput." + setMethod + "(" + assignBit + ");");
                                        } else {
                                            serviceCode.print("serviceOutput." + variableName[i] + " = " + assignBit + ";");
                                        }
                                        serviceCode.print("}");
                                        serviceCode.unIndent();
                                        serviceCode.print("else");
                                        serviceCode.indent();
                                        serviceCode.print("{");

                                        serviceCode.print("if (castNullToNewObject)");
                                        serviceCode.indent();
                                        serviceCode.print("{");
                                        if (useCharForCLOB) {
                                            if (webServiceRecType.equals("protected")) {
                                                serviceCode.print("serviceOutput." + setMethod + "(new char[0]);");
                                            } else {
                                                serviceCode.print("serviceOutput." + variableName[i] + "= new char[0];");
                                            }
                                        } else {
                                            if (webServiceRecType.equals("protected")) {
                                                serviceCode.print("serviceOutput." + setMethod + "(new byte[0]);");
                                            } else {
                                                serviceCode.print("serviceOutput." + variableName[i] + " = new byte[0];");
                                            }
                                        }
                                        serviceCode.print("}");
                                        serviceCode.unIndent();
                                        serviceCode.print("else");
                                        serviceCode.indent();
                                        serviceCode.print("{");


                                        if (webServiceRecType.equals("protected")) {
                                            serviceCode.print("serviceOutput." + setMethod + "(null);");
                                        } else {
                                            serviceCode.print("serviceOutput." + variableName[i] + " = null;");
                                        }

                                        serviceCode.print("}");
                                        serviceCode.unIndent();

                                        serviceCode.print("}");
                                        serviceCode.unIndent();
                                    } else // outParams == 1
                                    {
                                        serviceCode.print("if (theService." + getMethod + "() != null)");
                                        serviceCode.indent();
                                        serviceCode.print("{");
                                        serviceCode.print("serviceOutput = " + assignBit + ";");
                                        serviceCode.print("}");
                                        serviceCode.unIndent();
                                        serviceCode.print("else");
                                        serviceCode.indent();
                                        serviceCode.print("{");

                                        serviceCode.print("if (castNullToNewObject)");
                                        serviceCode.indent();
                                        serviceCode.print("{");
                                        if (useCharForCLOB) {
                                            serviceCode.print("serviceOutput = new char[0];");
                                        } else {
                                            serviceCode.print("serviceOutput = new byte[0];");
                                        }

                                        serviceCode.print("}");
                                        serviceCode.unIndent();
                                        serviceCode.print("else");
                                        serviceCode.indent();
                                        serviceCode.print("{");


                                        serviceCode.print("serviceOutput = null;");

                                        serviceCode.print("}");
                                        serviceCode.unIndent();

                                        serviceCode.print("}");
                                        serviceCode.unIndent();
                                    }

                                    serviceCode.print("}");
                                    serviceCode.unIndent();
                                    serviceCode.print("catch (Exception e)");
                                    serviceCode.indent();
                                    serviceCode.print("{");
                                    serviceCode.print("throw new CSException(\"" + variableName[i] + ": XMLType could not be unloaded:\" + e.getMessage());");
                                    serviceCode.print("}");
                                    serviceCode.unIndent();
                                    serviceCode.print(" ");

                                    break;
                                }
                                default: {
                                    if (outParams > 1) {
                                        if (webServiceRecType.equals("protected")) {
                                            serviceCode.print("serviceOutput." + setMethod + "(" + assignBit + ");");
                                        } else {
                                            serviceCode.print("serviceOutput." + variableName[i] + " = " + assignBit + ";");
                                        }
                                    } else // outParams == 1
                                    {
                                        serviceCode.print("serviceOutput = " + assignBit + ";");
                                    }
                                    break;
                                }
                            }

              /*
              switch (oracleUnderlyingDatatype[i])
                {
                case SqlUtils.ORACLE_LONGTEXT_DATATYPE:
                case SqlUtils.ORACLE_LONG_BINARY_DATATYPE:
                case SqlUtils.ORACLE_CLOB_DATATYPE:
                case SqlUtils.ORACLE_BLOB_DATATYPE:
                case SqlUtils.ORACLE_BFILE_DATATYPE:
                  {
                  serviceCode.print("if (! useByteArraysForLongsAndLOBS)");
                  serviceCode.indent();
                  serviceCode.print("{");
                  serviceCode.print("theService." + getMethod + "().delete();");
                  serviceCode.print("}");
                  serviceCode.unIndent();
                  serviceCode.print("");
                  break;
                  }

                }
                */
                        } //switch
                    } //for
                } //void


                if (servicePostCallStubFlag) {
                    //serviceCode.print("");
                    if (comments)
                        serviceCode.print("// See if there is anything we need to do after we call the service.");
                    serviceCode.print("doPostServiceEvent(theService);");
                }

                //if (alwaysReleaseResources)
                //  {
                ///// addAlwaysReleaseResourcesCode(serviceCode, comments);
                //  }
                // else
                //  {
                //  serviceCode.print("");
                //  }

                if (!returnType.equals("void")) {
                    if (debugMessages) {
                        serviceCode.print("");
                        serviceCode.print(stheLog + ".debug(\"" + serviceMethod + " Returning Data\");");
                    }
                    serviceCode.print("return(serviceOutput);");

                } // !void

            } //WS ALL
            serviceCode.print("}");
            serviceCode.unIndent();

            serviceCode.print("catch (com.mcpdbwizard.pub.CSException e)");
            serviceCode.indent();
            serviceCode.print("{");
            serviceCode.print(exceptionName + " e2 = new " + exceptionName + "(e.getMessage());");
            serviceCode.print(stheLog + ".error(\"" + serviceMethod + " Failed:\");");
            serviceCode.print(stheLog + ".error(e.getMessage());");


            if (comments) serviceCode.print("// Assume this error is db related and will go ");
            if (comments) serviceCode.print("// away when we reset the connection ");
            serviceCode.print("if (! alwaysReleaseResources)");
            serviceCode.indent();
            serviceCode.print("{");
            serviceCode.print("// release resources if finally wont");
            if (comments) serviceCode.print("releaseResources();");
            serviceCode.print("}");
            serviceCode.unIndent();
            serviceCode.print("");


            serviceCode.print("throw (e2);");
            serviceCode.print("}");
            serviceCode.unIndent();

            serviceCode.print("finally");
            serviceCode.indent();
            serviceCode.print("{");
            addAlwaysReleaseResourcesCode(serviceCode, comments);
            serviceCode.print("}");
            serviceCode.unIndent();
            serviceCode.print("}");
            //serviceCode.unIndent();

        } // TRY
        catch (CSSkipSectionException e) {
        } catch (CSNoDataInRowSetException e) {
            if (comments) serviceCode.print("// This procedure has no parameters and hence no Set methods");
        } catch (CSException e) {
            theLog.syserror("Error While Creating web services code:" + e.toString());
        }


        theWrangler.writeFile(serviceFile, serviceCode.getLines());
        theWrangler.writeFile(interfaceFile, interfaceCode.getLines());

        serviceFile.deleteOnExit();
        interfaceFile.deleteOnExit();

        // Create WSDLRowSet if needed
        if (hasRowSets || statementType == SqlUtils.SELECT) {
            java.io.File wsdlSetFile = new java.io.File(outputDir, sWsdlFileName + ".java");

            // Delete file if created before start of this session
//      if (wsdlSetFile.exists() && wsdlSetFile.lastModified() < startTime)
//        {
//        wsdlSetFile.delete();
//        }

//      if (! wsdlSetFile.exists())
            //       {
            JavaChunk theJavaChunk = new JavaChunk();

            if (comments) {
                theJavaChunk.print("/** ");
                theJavaChunk.print("* " + sWsdlFileName + " - Holds the result of a query in a serializable form ");
                theJavaChunk.print("* @since 4.0.2107 ");
                theJavaChunk.print("* @since 5.0.2253 Implements serializable ");
                theJavaChunk.print("*/ ");
                theJavaChunk.print(" ");
            }

            theJavaChunk.print("package " + packageName + ";");
            theJavaChunk.print("");
            //theJavaChunk.print("import com.mcpdbwizard.pub.ReadOnlyRowSet;");
            //theJavaChunk.print("");
            theJavaChunk.print("public class " + sWsdlFileName + " implements java.io.Serializable");
            theJavaChunk.print("{");
            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* An Array holding the names of columns in a query.");
                theJavaChunk.print("* There is no requirement that these names be meaningful or unique.");
                theJavaChunk.print("*/");
            }
            theJavaChunk.print(webServiceRecType + " String[] columnNames;");
            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* An Array of 0 or more " + sWsdlRowFileName + " that represent rows.");
                theJavaChunk.print("*/");
            }
            theJavaChunk.print(webServiceRecType + " " + sWsdlRowFileName + "[] rows;");
            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* Create a new, empty" + sWsdlRowFileName + ".");
                theJavaChunk.print("*/");
            }
            theJavaChunk.print("public " + sWsdlFileName + "()");
            theJavaChunk.print("  {");
            theJavaChunk.print("  }");
            theJavaChunk.print("");

            if (webServiceRecType.equals("protected")) {
                if (comments) {
                    theJavaChunk.print("/**");
                    theJavaChunk.print("* Set column names");
                    theJavaChunk.print("* @param String[] columnNames");
                    theJavaChunk.print("* @since 5.0.2234");
                    theJavaChunk.print("*/");
                }
                theJavaChunk.print("public void setColumnNames(java.lang.String[] columnNames)");
                theJavaChunk.indent();
                theJavaChunk.print("{");
                theJavaChunk.print("this.columnNames = columnNames;");
                theJavaChunk.print("}");
                theJavaChunk.unIndent();
                theJavaChunk.print("");

                if (comments) {
                    theJavaChunk.print("/**");
                    theJavaChunk.print("* Return column names");
                    theJavaChunk.print("* @return String[] columnNames");
                    theJavaChunk.print("* @since 5.0.2234");
                    theJavaChunk.print("*/");
                }
                theJavaChunk.print("public java.lang.String[] getColumnNames()");
                theJavaChunk.indent();
                theJavaChunk.print("{");
                theJavaChunk.print("return(columnNames);");
                theJavaChunk.print("}");
                theJavaChunk.unIndent();
                theJavaChunk.print("");

                if (comments) {
                    theJavaChunk.print("/**");
                    theJavaChunk.print("* Set rows");
                    theJavaChunk.print("* @param " + sWsdlRowFileName + "[] newRows");
                    theJavaChunk.print("* @since 5.0.2234");
                    theJavaChunk.print("*/");
                }
                theJavaChunk.print("public void set" + sWsdlRowFileName + "s(" + packageName + "." + sWsdlRowFileName + "[] rows)");
                theJavaChunk.indent();
                theJavaChunk.print("{");
                theJavaChunk.print("this.rows = rows;");
                theJavaChunk.print("}");
                theJavaChunk.unIndent();
                theJavaChunk.print("");

                if (comments) {
                    theJavaChunk.print("/**");
                    theJavaChunk.print("* Return array of " + sWsdlRowFileName);
                    theJavaChunk.print("* @return " + sWsdlRowFileName + "[] ");
                    theJavaChunk.print("* @since 5.0.2234");
                    theJavaChunk.print("*/");
                }
                theJavaChunk.print("public " + packageName + "." + sWsdlRowFileName + "[] get" + sWsdlRowFileName + "s()");
                theJavaChunk.indent();
                theJavaChunk.print("{");
                theJavaChunk.print("return(rows);");
                theJavaChunk.print("}");
                theJavaChunk.unIndent();
                theJavaChunk.print("");
            }

            theJavaChunk.print("");
            theJavaChunk.print("} // Generated by " + Namer.param_prod_name + " " + Namer.param_version + "    ");

            theWrangler.writeFile(wsdlSetFile, theJavaChunk.getLines());
            //}

            java.io.File wsdlRowSetFile = new java.io.File(outputDir, sWsdlRowFileName + ".java");

            // Delete file if created before start of this session
//      if (wsdlRowSetFile.exists() && wsdlRowSetFile.lastModified() < startTime)
//        {
//        wsdlRowSetFile.delete(); //DRKLUGE
//        }

            //   if (! wsdlRowSetFile.exists())
            //     {
            theJavaChunk = new JavaChunk();

            if (comments) {
                theJavaChunk.print("/** ");
                theJavaChunk.print("* " + sWsdlRowFileName + " - Holds a single row of a query in a serializable form ");
                theJavaChunk.print("* @since 4.0.2107 ");
                theJavaChunk.print("* @since 5.0.2253 Implements serializable ");
                theJavaChunk.print("*/ ");
                theJavaChunk.print(" ");
            }

            theJavaChunk.print("package " + packageName + ";");
            theJavaChunk.print("");
            theJavaChunk.print("public class " + sWsdlRowFileName + " implements java.io.Serializable");
            theJavaChunk.print("{");
            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* The columns in this query row.");
                theJavaChunk.print("*/");
            }
            theJavaChunk.print(webServiceRecType + " " + packageName + "." + sWsdlElementName + "[] rowColumns;");
            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* Create a new, empty" + sWsdlRowFileName + ".");
                theJavaChunk.print("*/");
            }
            theJavaChunk.print("public " + sWsdlRowFileName + "()");
            theJavaChunk.print("  {");
            theJavaChunk.print("  }");
            theJavaChunk.print("");

            if (webServiceRecType.equals("protected")) {
                if (comments) {
                    theJavaChunk.print("/**");
                    theJavaChunk.print("* Set columns");
                    theJavaChunk.print("* @param " + sWsdlElementName + "[] rowColumns");
                    theJavaChunk.print("* @since 5.0.2234");
                    theJavaChunk.print("*/");
                }
                theJavaChunk.print("public void set" + sWsdlElementName + "s(" + packageName + "." + sWsdlElementName + "[] rowColumns)");
                theJavaChunk.indent();
                theJavaChunk.print("{");
                theJavaChunk.print("this.rowColumns = rowColumns;");
                theJavaChunk.print("}");
                theJavaChunk.unIndent();
                theJavaChunk.print("");

                if (comments) {
                    theJavaChunk.print("/**");
                    theJavaChunk.print("* Return column data");
                    theJavaChunk.print("* @return " + sWsdlElementName + "[] rowColumns");
                    theJavaChunk.print("* @since 5.0.2234");
                    theJavaChunk.print("*/");
                }
                theJavaChunk.print("public " + packageName + "." + sWsdlElementName + "[] get" + sWsdlElementName + "s()");
                theJavaChunk.indent();
                theJavaChunk.print("{");
                theJavaChunk.print("return(rowColumns);");
                theJavaChunk.print("}");
                theJavaChunk.unIndent();
                theJavaChunk.print("");
            }

            theJavaChunk.print("} // Generated by " + Namer.param_prod_name + " " + Namer.param_version + "    ");

            theWrangler.writeFile(wsdlRowSetFile, theJavaChunk.getLines());
            //   }

            java.io.File wsdlElementFile = new java.io.File(outputDir, sWsdlElementName + ".java");

            // Delete file if created before start of this session
//      if (wsdlElementFile.exists() && wsdlElementFile.lastModified() < startTime)
//        {
//        wsdlElementFile.delete();
//        }

//      if (! wsdlElementFile.exists())
//        {
            theJavaChunk = new JavaChunk();

            if (comments) {
                theJavaChunk.print("/** ");
                theJavaChunk.print("* " + sWsdlElementName + " - Holds a field from a query row in a serializable form ");
                theJavaChunk.print("* @since 4.0.2107");
                theJavaChunk.print("* @since 5.0.2253 Implements serializable ");
                theJavaChunk.print("*/ ");
                theJavaChunk.print(" ");
            }

            theJavaChunk.print("package " + packageName + ";");
            theJavaChunk.print("");
            theJavaChunk.print("public class " + sWsdlElementName + " implements java.io.Serializable");
            theJavaChunk.print("{");
            theJavaChunk.print("");
            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* A value for a column in a row in a query.");
                theJavaChunk.print("*/");
            }
            theJavaChunk.print(webServiceRecType + " Object column;");
            theJavaChunk.print("");

            if (comments) {
                theJavaChunk.print("/**");
                theJavaChunk.print("* Create a new, empty" + sWsdlElementName + ".");
                theJavaChunk.print("*/");
            }
            theJavaChunk.print("public " + sWsdlElementName + "()");
            theJavaChunk.print("  {");
            theJavaChunk.print("  }");
            theJavaChunk.print("");

            if (webServiceRecType.equals("protected")) {
                if (comments) {
                    theJavaChunk.print("/**");
                    theJavaChunk.print("* Set column");
                    theJavaChunk.print("* @param Object column");
                    theJavaChunk.print("* @since 5.0.2234");
                    theJavaChunk.print("*/");
                }
                theJavaChunk.print("public void setColumn(Object column)");
                theJavaChunk.indent();
                theJavaChunk.print("{");
                theJavaChunk.print("this.column = column;");
                theJavaChunk.print("}");
                theJavaChunk.unIndent();
                theJavaChunk.print("");

                if (comments) {
                    theJavaChunk.print("/**");
                    theJavaChunk.print("* Return column");
                    theJavaChunk.print("* @return Object column");
                    theJavaChunk.print("* @since 5.0.2234");
                    theJavaChunk.print("*/");
                }
                theJavaChunk.print("public Object getColumn()");
                theJavaChunk.indent();
                theJavaChunk.print("{");
                theJavaChunk.print("return(column);");
                theJavaChunk.print("}");
                theJavaChunk.unIndent();
                theJavaChunk.print("");
            }

            theJavaChunk.print("} // Generated by " + Namer.param_prod_name + " " + Namer.param_version + "    ");

            theWrangler.writeFile(wsdlElementFile, theJavaChunk.getLines());
            //}
        }
    }

    void addArrayLobSetCode(JavaChunk theJavaChunk
            , String javaClassName
            , boolean comments
            , boolean debugMessages
            , boolean otherMessages
            , String parentArrayName
            , boolean webServices
            , String oracleVersion) {

        boolean XmlTypeBadCodeDone = false;

        theJavaChunk.print("try");
        theJavaChunk.indent();
        theJavaChunk.print("{       ");
        theJavaChunk.print(javaClassName + "[] theArray = " + parentArrayName + ".getCurrentValues();");
        theJavaChunk.print("for (int i=0; i < theArray.length; i++)");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("if (theArray[i] != null)");
        theJavaChunk.indent();
        theJavaChunk.print("{");

        for (int i = 0; i < theRowSet.size(); i++) {
            theRowSet.setCurrentRowNumber(i);
            //String setMethod = JavaUtils.getJavaName("set_" + variableName[i], javaNamingConvention, theLog);

            if (oracleParamDatatype[i].equals("OracleTypes.CLOB")
                    || oracleParamDatatype[i].equals("OracleTypes.BLOB")
            ) {
                if (comments) theJavaChunk.print("// See if we have a file but no LOB...");
                if (webServices) {
                    //DRKLUGE ?Coverage
                    theJavaChunk.print("if ((theArray[i]." + variableName[i] + " != null || theArray[i]." + byteName[i] + " != null) && theArray[i]." + lobName[i] + " == null )");
                } else {
                    theJavaChunk.print("if (theArray[i]." + variableName[i] + " != null && theArray[i]." + lobName[i] + " == null )");
                }

                theJavaChunk.indent();
                theJavaChunk.print("{");


                if (targetVersion.startsWith("8")) {
                    if (comments) theJavaChunk.print("// We have a file but no LOB to associate it with...");
                    theJavaChunk.print("throw (new CSException(\"No LOB Pointer Provided for " + lobName[i] + ", row \" + i));");
                    theJavaChunk.print("}");
                    theJavaChunk.unIndent();
                } else {
                    theJavaChunk.print("if (createTempLobsIfNeeded)");
                    theJavaChunk.indent();
                    theJavaChunk.print("{");
                    if (comments) theJavaChunk.print("// Create temporary LOB so call will work");
                    if (oracleParamDatatype[i].equals("OracleTypes.CLOB")) {
                        theJavaChunk.print("theArray[i]." + lobName[i] + " = oracle.sql.CLOB.createTemporary(theConnection, true, oracle.sql.CLOB.DURATION_SESSION);");
                    } else if (oracleParamDatatype[i].equals("OracleTypes.BLOB")) {
                        theJavaChunk.print("theArray[i]." + lobName[i] + " = oracle.sql.BLOB.createTemporary(theConnection, true, oracle.sql.BLOB.DURATION_SESSION);");
                    }

                    theJavaChunk.print("}");
                    theJavaChunk.unIndent();
                    theJavaChunk.print("else");
                    theJavaChunk.indent();
                    theJavaChunk.print("{");
                    if (comments) theJavaChunk.print("// We have a file but no LOB to associate it with...");
                    theJavaChunk.print("throw (new CSException(\"No LOB Pointer Provided for " + lobName[i] + ", row \" + i));");
                    theJavaChunk.print("}");
                    theJavaChunk.unIndent();


        /*//DRKLUGE
        if (webServices)
          {
          theJavaChunk.print(" ");
          theJavaChunk.print("if (theArray[i]."+byteName[i]+" != null)");
          theJavaChunk.indent();
          theJavaChunk.print("{");
          theJavaChunk.print("theArray[i]."+variableName[i] + " = com.mcpdbwizard.pub.IOUtils.loadByteArrayIntoFile(theArray[i]."+byteName[i]+","+stempFilePrefix+", "+ stempFileSuffix +", "+ stempFileDir +".getAbsolutePath(),"+stheLog+");");
          theJavaChunk.print("theArray[i]."+byteName[i]+" = null;");
          theJavaChunk.print("}");
          theJavaChunk.unIndent();
          }

        theJavaChunk.print(" ");
        if (   oracleParamDatatype[i].equals("OracleTypes.CLOB"))
          {
          theJavaChunk.print("com.mcpdbwizard.pub.LongObjectLoader.loadCLOB(theArray[i]."+lobName[i]+",theArray[i]."+variableName[i]+"," + sbufferSize + ");");
          }
        else
          {
          theJavaChunk.print("com.mcpdbwizard.pub.LongObjectLoader.loadBLOB(theArray[i]."+lobName[i]+",theArray[i]."+variableName[i]+"," + sbufferSize + ");");
          }
        */

                    String realDataType = "CLOB";
                    if (oracleParamDatatype[i].equals("OracleTypes.BLOB")) {
                        realDataType = "BLOB";
                    }
                    theJavaChunk.print(isBrokenString + " ");
                    theJavaChunk.print(isBrokenString + "if (theArray[i]." + byteName[i] + " != null)");
                    theJavaChunk.indent();
                    theJavaChunk.print(isBrokenString + "{                                                                                                                   ");
                    theJavaChunk.print(isBrokenString + "com.mcpdbwizard.pub.LongObjectLoader.load" + realDataType + "(theArray[i]." + lobName[i] + ", theArray[i]." + byteName[i] + ");     ");
                    theJavaChunk.print(isBrokenString + "} ");
                    theJavaChunk.unIndent();
                    theJavaChunk.print(isBrokenString + "else");
                    theJavaChunk.indent();
                    theJavaChunk.print(isBrokenString + "{                                                                                                                   ");
                    theJavaChunk.print(isBrokenString + "com.mcpdbwizard.pub.LongObjectLoader.load" + realDataType + "(theArray[i]." + lobName[i] + ", theArray[i]." + variableName[i] + "," + sbufferSize + ");     ");
                    theJavaChunk.print(isBrokenString + "} ");
                    theJavaChunk.unIndent();


                    theJavaChunk.print("}");
                    theJavaChunk.unIndent();
                    theJavaChunk.print(" ");
                }
            } else if (oracleParamDatatype[i].equals("OracleTypes.BFILE")) {
                if (comments) theJavaChunk.print("// See if we have a file but no BFILE...");
                theJavaChunk.print("if (theArray[i]." + variableName[i] + " != null && theArray[i]." + lobName[i] + " == null )");
                theJavaChunk.indent();
                theJavaChunk.print("{");
                if (comments) theJavaChunk.print("// We have a file but no LOB to associate it with...");
                theJavaChunk.print("throw (new CSException(\"No BFILE Pointer Provided for " + lobName[i] + ", row \" + i));");
                theJavaChunk.print("}");
                theJavaChunk.unIndent();
                theJavaChunk.print(" ");
            } else if (oracleParamDatatype[i].equals("OracleTypes.OPAQUE")) {

                if (!XmlTypeBadCodeDone) {
                    if (oracleVersion.startsWith("10")
                            || oracleVersion.startsWith("11.1")) {
                        if (comments)
                            theJavaChunk.print("// If you are using a pre-11.2 driver passing arrays of XMLType back and forth doesn't work");
                        if (comments)
                            theJavaChunk.print("// XMLType array operations require the 11.2 or later Oracle JDBC driver,");
                        if (comments) theJavaChunk.print("// regardless of what version of Oracle you connect to.");
                        theJavaChunk.print(" ");
                    } else {
                        if (comments)
                            theJavaChunk.print("// If you are using a pre-11.2 driver passing arrays of XMLType back and forth doesn't work");
                        if (comments)
                            theJavaChunk.print("// XMLType array operations require the 11.2 or later Oracle JDBC driver,");
                        if (comments) theJavaChunk.print("// regardless of what version of Oracle you connect to.");
                        theJavaChunk.print("if (! (oracle.jdbc.OracleDriver.getDriverVersion().startsWith(\"11.2\") || oracle.jdbc.OracleDriver.getDriverVersion().startsWith(\"12.\")  ))");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");
                        theJavaChunk.print("throw (new CSException(\"XMLType array operations require the 11.2 or later Oracle JDBC driver, regardless of what version of Oracle you connect to.\"));");
                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();
                        theJavaChunk.print(" ");
                    }
                    XmlTypeBadCodeDone = true;
                }

                if (comments) theJavaChunk.print("// See if we have a file but no LOB...");
                if (webServices) {
                    //DRKLUGE ?Coverage
                    theJavaChunk.print("if ((theArray[i]." + variableName[i] + " != null || theArray[i]." + byteName[i] + " != null) && theArray[i]." + lobName[i] + " == null )");
                } else {
                    theJavaChunk.print("if (theArray[i]." + variableName[i] + " != null && theArray[i]." + lobName[i] + " == null )");
                }

                theJavaChunk.indent();
                theJavaChunk.print("{");

                theJavaChunk.print("try");
                theJavaChunk.indent();
                theJavaChunk.print("{");

                String realDataType = "OPAQUE";
                theJavaChunk.print(isBrokenString + " ");
                if (comments) theJavaChunk.print("// Load into byte array if in file..");
                theJavaChunk.print(isBrokenString + "if (theArray[i]." + byteName[i] + " == null)");
                theJavaChunk.indent();
                theJavaChunk.print(isBrokenString + "{ ");
                if (useCharForCLOB) {
                    theJavaChunk.print(isBrokenString + "theArray[i]." + byteName[i] + " = com.mcpdbwizard.pub.IOUtils.loadFileIntoCharArray(theArray[i]." + variableName[i] + "); ");
                } else {
                    theJavaChunk.print(isBrokenString + "theArray[i]." + byteName[i] + " = com.mcpdbwizard.pub.IOUtils.loadFileIntoByteArray(theArray[i]." + variableName[i] + "); ");
                }

                theJavaChunk.print(isBrokenString + "} ");
                theJavaChunk.unIndent();
                theJavaChunk.print(isBrokenString + " ");


                if (useCharForCLOB) {
                    theJavaChunk.print(isBrokenString + "String tempString = new String(theArray[i]." + byteName[i] + ");");  //DRKLUGE
                    theJavaChunk.print(isBrokenString + "java.io.ByteArrayInputStream inStr = new java.io.ByteArrayInputStream(tempString.getBytes());");  //DRKLUGE
                } else {
                    theJavaChunk.print(isBrokenString + "java.io.ByteArrayInputStream inStr = new java.io.ByteArrayInputStream(theArray[i]." + byteName[i] + ");");  //DRKLUGE
                }
                theJavaChunk.print(isBrokenString + "theArray[i]." + lobName[i] + " = new oracle.xdb.XMLType(theConnection, inStr);");

                theJavaChunk.print("}");
                theJavaChunk.unIndent();
                theJavaChunk.print(" ");

                theJavaChunk.print(isBrokenString + "catch (SQLException e)   ");
                theJavaChunk.indent();
                theJavaChunk.print(isBrokenString + "{           ");
                theJavaChunk.print(isBrokenString + "throw (new CSException(\"" + variableName[i] + " could not be loaded SQLException:\" + e.toString()));   ");
                theJavaChunk.print(isBrokenString + "}          ");
                theJavaChunk.unIndent();
                theJavaChunk.print(isBrokenString + "catch (CSException e)   ");
                theJavaChunk.indent();
                theJavaChunk.print(isBrokenString + "{           ");
                theJavaChunk.print(isBrokenString + "throw (new CSException(\"" + variableName[i] + " could not be loaded:\" + e.toString()));   ");
                theJavaChunk.print(isBrokenString + "}          ");
                theJavaChunk.unIndent();
                theJavaChunk.print(isBrokenString + "");

                theJavaChunk.print("}");
                theJavaChunk.unIndent();
                theJavaChunk.print(" ");


            }

        }

        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print("catch (Exception e)");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("throw new CSException(\"" + parentArrayName + ": Needed LOB pointers are missing:\" + e.getMessage());");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print(" ");
    }

    void addArrayBfileDefaultCode(JavaChunk theJavaChunk
            , String javaClassName
            , boolean comments
            , boolean debugMessages
            , boolean otherMessages
            , String parentArrayName
            , String arraySetStatement
            , String serviceMethodName
            , String assignStatement
            , String assignStatement2) {

        boolean reallyDoesHaveBfiles = false;

        for (int i = 0; i < theRowSet.size(); i++) {
            theRowSet.setCurrentRowNumber(i);


            if (oracleParamDatatype[i].equals("OracleTypes.BFILE")) {
                reallyDoesHaveBfiles = true;
            }
        }

        if (theFlags.getFlag(GlobalFlags.HAS_BFILES) && reallyDoesHaveBfiles) {

            theJavaChunk.print("");

            if (comments) {
                theJavaChunk.print("// Fill in missing BFILEs");
            }

            theJavaChunk.print("try");
            theJavaChunk.indent();
            theJavaChunk.print("{");

            theJavaChunk.print(javaClassName + "[] theArray = " + arraySetStatement);
            theJavaChunk.print("if (theArray != null)");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("for (int i=0; i < theArray.length; i++)");
            theJavaChunk.indent();
            theJavaChunk.print("{");

            for (int i = 0; i < theRowSet.size(); i++) {
                theRowSet.setCurrentRowNumber(i);

                String setMethod = JavaUtils.getJavaName("set_" + variableName[i], javaNamingConvention, theLog);
                String getByteArrayMethod = JavaUtils.getJavaName("get_" + variableName[i] + "_byte_array", javaNamingConvention, theLog);

                if (oracleParamDatatype[i].equals("OracleTypes.BFILE")) {
                    if (comments) theJavaChunk.print("// See if we have a file but no LOB...");
                    //theJavaChunk.print("if (theArray[i]."+byteName[i]+" != null && theArray[i]."+byteName[i]+".length > 0)");
                    theJavaChunk.print("if (theArray[i]." + getByteArrayMethod + "() != null && theArray[i]." + getByteArrayMethod + "().length > 0)");
                    theJavaChunk.indent();
                    theJavaChunk.print("{");

                    if (targetVersion.startsWith("8")) {
                        if (comments) theJavaChunk.print("// We have a file but no BFILES to associate it with.");
                        if (comments)
                            theJavaChunk.print("// This code is created for Oracle V" + targetVersion + ". " + Namer.param_prod_name + " ");
                        if (comments)
                            theJavaChunk.print("// can create BFILEs when generating code for Oracle V9.0.1 or higher.");

                        theJavaChunk.print("throw (new CSException(\"No LOB Pointer Provided for " + lobName[i] + ", row \" + i));");
                    } else {
                        if (comments) theJavaChunk.print("// Create needed BFILE object");
                        theJavaChunk.print("try");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");
                        theJavaChunk.print("oracle.sql.BFILE tempBFILE = createNewBfilePointer(\"" + serviceMethodName + "\",\"" + parentArrayName + "\", theArray[i]." + getByteArrayMethod + "());");
                        theJavaChunk.print("theArray[i]." + setMethod + "(tempBFILE);");
                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();
                        theJavaChunk.print("catch (Exception e)");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");
                        theJavaChunk.print("throw new CSException(\"" + variableName[i] + ", row \" + i + \": BFILE pointer could not be created:\" + e.getMessage());");
                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();
                    }

                    theJavaChunk.print("}");
                    theJavaChunk.unIndent();
                }
            }

            theJavaChunk.print("}");
            theJavaChunk.unIndent();

            theJavaChunk.print("}");
            theJavaChunk.unIndent();
            theJavaChunk.print("");
            theJavaChunk.print(assignStatement2);
            theJavaChunk.print("");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
            theJavaChunk.print("catch (Exception e)");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("throw new CSException(\"" + parentArrayName + ": Unable to set LOB pointers:\" + e.getMessage());");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
            theJavaChunk.print(" ");
        } // !8
        else {
            theJavaChunk.print(assignStatement);
        }
    } //

    void addRecordBfileDefaultCode(JavaChunk theJavaChunk
            , String javaClassName
            , boolean comments
            , boolean debugMessages
            , boolean otherMessages
            , String parentRecordName
            , String arraySetStatement
            , String serviceMethodName
            , String assignStatement
            , String assignStatement2) {

        boolean reallyDoesHaveBfiles = false;

        for (int i = 0; i < theRowSet.size(); i++) {
            theRowSet.setCurrentRowNumber(i);


            if (oracleParamDatatype[i].equals("OracleTypes.BFILE")) {
                reallyDoesHaveBfiles = true;
            }
        }


        if (theFlags.getFlag(GlobalFlags.HAS_BFILES) && reallyDoesHaveBfiles) {

            theJavaChunk.print("");

            if (comments) {
                theJavaChunk.print("// Fill in missing BFILEs");
            }

            theJavaChunk.print("try");
            theJavaChunk.indent();
            theJavaChunk.print("{");

            theJavaChunk.print(javaClassName + " theRow =  " + assignStatement + ";");
            //theJavaChunk.print("if (theArray != null)");
            //theJavaChunk.indent();
            //theJavaChunk.print("{");
            //theJavaChunk.print("for (int i=0; i < theArray.length; i++)");
            //theJavaChunk.indent();
            //theJavaChunk.print("{");

            for (int i = 0; i < theRowSet.size(); i++) {
                theRowSet.setCurrentRowNumber(i);

                String setMethod = JavaUtils.getJavaName("set_" + variableName[i], javaNamingConvention, theLog);
                String setNullMethod = JavaUtils.getJavaName("set_" + variableName[i] + "_to_null", javaNamingConvention, theLog);
                //String getByteArrayMethod = JavaUtils.getJavaName("get_" + variableName[i]+ "_byte_array", javaNamingConvention, theLog);

                if (oracleParamDatatype[i].equals("OracleTypes.BFILE")) {
                    if (comments) theJavaChunk.print("// See if we have a file but no LOB...");
                    //theJavaChunk.print("if (theArray[i]."+byteName[i]+" != null && theArray[i]."+byteName[i]+".length > 0)");
                    theJavaChunk.print("if (" + parentRecordName + " != null && " + parentRecordName + "." + byteName[i] + " != null && " + parentRecordName + "." + byteName[i] + ".length > 0)");
                    theJavaChunk.indent();
                    theJavaChunk.print("{");

                    if (targetVersion.startsWith("8")) {
                        if (comments) theJavaChunk.print("// We have a file but no BFILES to associate it with.");
                        if (comments)
                            theJavaChunk.print("// This code is created for Oracle V" + targetVersion + ". " + Namer.param_prod_name + " ");
                        if (comments)
                            theJavaChunk.print("// can create BFILEs when generating code for Oracle V9.0.1 or higher.");

                        theJavaChunk.print("throw (new CSException(\"No LOB Pointer Provided for " + lobName[i] + ", row \"));");
                    } else {
                        if (comments) theJavaChunk.print("// Create needed BFILE object");
                        theJavaChunk.print("try");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");
                        theJavaChunk.print("oracle.sql.BFILE tempBFILE = createNewBfilePointer(\"" + serviceMethodName + "\",\"" + parentRecordName + "\", " + parentRecordName + "." + byteName[i] + ");");
                        theJavaChunk.print("theRow." + setMethod + "(tempBFILE);");
                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();
                        theJavaChunk.print("catch (Exception e)");
                        theJavaChunk.indent();
                        theJavaChunk.print("{");
                        theJavaChunk.print("throw new CSException(\"" + variableName[i] + ": BFILE pointer could not be created:\" + e.getMessage());");
                        theJavaChunk.print("}");
                        theJavaChunk.unIndent();
                    }

                    theJavaChunk.print("}");
                    theJavaChunk.unIndent();
                }
            }

            //theJavaChunk.print("}");
            //theJavaChunk.unIndent();

            //theJavaChunk.print("}");
            //theJavaChunk.unIndent();
            theJavaChunk.print("");
            theJavaChunk.print(assignStatement2);
            theJavaChunk.print("");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
            theJavaChunk.print("catch (Exception e)");
            theJavaChunk.indent();
            theJavaChunk.print("{");
            theJavaChunk.print("throw new CSException(\"" + parentRecordName + ": Unable to set LOB pointers:\" + e.getMessage());");
            theJavaChunk.print("}");
            theJavaChunk.unIndent();
            theJavaChunk.print(" ");
        } // !8
        else {
            theJavaChunk.print(assignStatement);
        }
    } //m

    private void addAlwaysReleaseResourcesCode(JavaChunk theJavaCode, boolean comments) {
        theJavaCode.print("");
        theJavaCode.print("if (alwaysReleaseResources)");
        theJavaCode.indent();
        theJavaCode.print("{");
        if (comments) theJavaCode.print("// Hand back DB connection...");
        theJavaCode.print("releaseResources();");
        theJavaCode.print("}");
        theJavaCode.unIndent();
        theJavaCode.print("");

    }

    public boolean needsPlsqlIndexByArray() {
        return (needsPlsqlIndexByArray);
    }

}



