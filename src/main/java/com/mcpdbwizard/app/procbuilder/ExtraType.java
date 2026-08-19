package com.mcpdbwizard.app.procbuilder;

import com.mcpdbwizard.app.common.*;
import com.mcpdbwizard.app.procbuilder.gui.*;

import java.util.ArrayList;

import com.mcpdbwizard.pub.*;

/**
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class ExtraType {

    public final static String PARAM_TARGET_PARAM_ARRAY_NAME = "PARAM_TARGET_PARAM_NAME_AARRAY_NAME";
    public final static String PARAM_TARGET_PARAM_NAME = "PARAM_TARGET_PARAM_NAME_NAME";
    public final static String PARAM_TARGET_PARAM_NAME_ATYPE = "PARAM_TARGET_PARAM_NAME_ATYPE";
    public final static String PARAM_TARGET_PARAM_NAME_RTYPE = "PARAM_TARGET_PARAM_NAME_RTYPE";
    public final static String PARAM_TARGET_PARAM_NAME_REALTYPE = "PARAM_TARGET_PARAM_NAME_REALTYPE";
    int arrayId = 0;
    String name = "";
    ArrayList createStatement = null;
    String createArrayStatement = null;
    String dropTypeStatement = null;
    String dropArrayStatement = null;
    ArrayList weirdAssignStatement = null;
    ArrayList assignStatement = null;
    ArrayList unAssignStatement = null;
    boolean isGeneric = true;
    boolean isWeirdpackageArrayOfOracleType = false;

    String sizeDef = "";

    public ExtraType(String prefix, PlsqlRecordObject newPlsql, LogInterface theLog, int arrayId
            , ArrayList longNames) {

        if (newPlsql.typeImplementingClass != null && newPlsql.typeImplementingClass.isWeirdpackageArrayOfOracleType) {
            isWeirdpackageArrayOfOracleType = true;
        }

        final int MAX_NAME_LENGTH = 28;

        this.arrayId = arrayId;

        ReadOnlyRowSet theRowSet = newPlsql.typeImplementingClass.argRowSet;

        // The shadow TYPE is built from the ELEMENT's rows, and the element here is a separate object
        // from the entry the field synthesis populates -- getChildRecord builds a fresh one per
        // collection -- so the Java class can come out correct while this comes out empty. Where the
        // element is a table %ROWTYPE, getChildRecord left the table's columns on the object for
        // exactly this moment; they are the same columns 12c puts in argRowSet, so the emitted type
        // is the one 12c emits.
        if ((theRowSet == null || theRowSet.size() == 0)
                && newPlsql.typeImplementingClass.rowtypeFieldRows != null
                && newPlsql.typeImplementingClass.rowtypeFieldRows.size() > 0) {
            theRowSet = newPlsql.typeImplementingClass.rowtypeFieldRows;
        }

        if (theRowSet == null || theRowSet.size() == 0) {
            theLog.warning("No fields to build the extra TYPE from: collection " + newPlsql.oracleName
                    + ", element " + newPlsql.typeImplementingClass.oracleName
                    + ". The emitted type will have no attributes and Oracle will hold it INVALID.");
        }

        createStatement = new ArrayList(theRowSet.size());
        assignStatement = new ArrayList(theRowSet.size());
        weirdAssignStatement = new ArrayList(theRowSet.size());
        unAssignStatement = new ArrayList(theRowSet.size());

        createStatement.add("");

        name = new String(prefix.toUpperCase());

        final String spaces = "                                                                          ";

        int startPoint = 0;
        //if (theRowSet.size() == 1)
        //  {
        //  startPoint = 0;
        //  }

        // Walk through newPlsql and build up create statements and name
        theRowSet.first();


        int maxLength = 0;

        for (int i = startPoint; i < theRowSet.size(); i++) {
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


        unAssignStatement.add(ExtraType.PARAM_TARGET_PARAM_ARRAY_NAME + "(i) := " + ExtraType.PARAM_TARGET_PARAM_NAME_RTYPE);

        weirdAssignStatement.add(ExtraType.PARAM_TARGET_PARAM_NAME + "(i) := " + ExtraType.PARAM_TARGET_PARAM_NAME_RTYPE);

        for (int i = startPoint; i < theRowSet.size(); i++) {
            sizeDef = "";

            try {
                theRowSet.setCurrentRowNumber(i);

                ExtraTypeSizeWrangler colLengthAndName = new ExtraTypeSizeWrangler(theRowSet, name, isGeneric);

                sizeDef = colLengthAndName.getSizeDef();

                if (isGeneric) {
                    name = colLengthAndName.appendName(name);
                }


                String aLine = " ";
                String assign = " ";
                String weirdAssign = " ";
                String unAssign = " ";

                if (i == startPoint) {
                    aLine = aLine + "(";
                    weirdAssign = weirdAssign + "(";
                    assign = assign + " ";
                    unAssign = unAssign + "(";
                } else {
                    aLine = aLine + ",";
                    weirdAssign = weirdAssign + ",";
                    assign = assign + " ";
                    unAssign = unAssign + ",";
                }


                if (isGeneric) {
                    aLine = aLine + "COL_" + i + " " + theRowSet.getString("DATA_TYPE");

                    weirdAssign = weirdAssign + ExtraType.PARAM_TARGET_PARAM_ARRAY_NAME + "(i).COL_" + i;
                    assign = assign + ExtraType.PARAM_TARGET_PARAM_NAME
                            + "(i)." + theRowSet.getString("ARGUMENT_NAME").toUpperCase()
                            + spaces.substring(0, (maxLength - theRowSet.getString("ARGUMENT_NAME").length()))
                            + " := "
                            + ExtraType.PARAM_TARGET_PARAM_ARRAY_NAME + "(i).COL_" + i
                            + ";";

                    unAssign = unAssign + ExtraType.PARAM_TARGET_PARAM_NAME + "(i)." + theRowSet.getString("ARGUMENT_NAME").toUpperCase();
                } else {
                    aLine = aLine + theRowSet.getString("ARGUMENT_NAME").toUpperCase() + spaces.substring(0, (maxLength - theRowSet.getString("ARGUMENT_NAME").length()))
                            + " " + theRowSet.getString("DATA_TYPE");

                    weirdAssign = weirdAssign + ExtraType.PARAM_TARGET_PARAM_ARRAY_NAME + "(i)." + theRowSet.getString("ARGUMENT_NAME").toUpperCase();
                    assign = assign + ExtraType.PARAM_TARGET_PARAM_NAME + "(i)." + theRowSet.getString("ARGUMENT_NAME").toUpperCase()
                            + spaces.substring(0, (maxLength - theRowSet.getString("ARGUMENT_NAME").length()))
                            + " := "
                            + ExtraType.PARAM_TARGET_PARAM_ARRAY_NAME
                            + "(i)." + theRowSet.getString("ARGUMENT_NAME").toUpperCase()
                            + ";";


                    unAssign = unAssign + ExtraType.PARAM_TARGET_PARAM_NAME + "(i)." + theRowSet.getString("ARGUMENT_NAME").toUpperCase();
                }


                String dataType = theRowSet.getString("DATA_TYPE");

                if (dataType.equalsIgnoreCase("CHAR")
                        || dataType.equalsIgnoreCase("CHARACTER")
                        || dataType.equalsIgnoreCase("NCHAR")
                        || dataType.equalsIgnoreCase("NCHARACTER")
                        || dataType.equalsIgnoreCase("RAW")
                        || dataType.equalsIgnoreCase("STRING")
                        || dataType.equalsIgnoreCase("VARCHAR")
                        || dataType.equalsIgnoreCase("VARCHAR2")) {
                    aLine = aLine + colLengthAndName.getSizeDef();
                } else if (dataType.equals("TIMESTAMP WITH LOCAL TIME ZONE") // Datatype according to ALL_SOURCE
                        || dataType.equals("TIMESTAMPLTZ") // Datatype according to ResultSet
                        || (dataType.startsWith("TIMESTAMP") // Allow for "TIMESTAMP(6) WITH LOCAL TIME ZONE"
                        && dataType.endsWith("LOCAL TIME ZONE"))) {
                    aLine = JavaUtils.replaceString(aLine, "TIMESTAMP", "TIMESTAMP" + colLengthAndName.getSizeDef());
                } else if (dataType.equals("TIMESTAMP WITH TIME ZONE")  // Datatype according to ALL_SOURCE
                        || dataType.equals("TIMESTAMPTZ")   // Datatype according to ResultSet
                        || (dataType.startsWith("TIMESTAMP") // Allow for "TIMESTAMP(6) WITH LOCAL TIME ZONE"
                        && dataType.endsWith("TIME ZONE"))) {
                    aLine = JavaUtils.replaceString(aLine, "TIMESTAMP", "TIMESTAMP" + colLengthAndName.getSizeDef());
                } else if (dataType.startsWith("TIMESTAMP")) {
                    aLine = JavaUtils.replaceString(aLine, "TIMESTAMP", "TIMESTAMP" + colLengthAndName.getSizeDef());
                } else {
                    //String foo = theRowSet.getRowAsString(":","?");

                    if (theRowSet.checkColumnName("FMT") && theRowSet.getString("FMT") != null && theRowSet.getString("FMT") != "") {
                        aLine = aLine + theRowSet.getString("FMT");
                    }
                }

                if ((i + 1) == theRowSet.size()) {
                    aLine = aLine + ");";

                    weirdAssign = weirdAssign + ");";

                    unAssign = unAssign + ");";
                }

                createStatement.add(aLine);

                assignStatement.add(assign);
                weirdAssignStatement.add(weirdAssign);
                unAssignStatement.add(unAssign);


            } catch (Exception e) {
                theLog.syserror(e, true, true);
            }
        }

        if (name.length() > 28) {
            // Name will be too long....

            // Find name in array list
            longNames.trimToSize();
            int lSize = longNames.size();
            int longNameId = Integer.MIN_VALUE;

            if (lSize > 0) {
                for (int q = 0; q < lSize; q++) {
                    if (((String) longNames.get(q)).equals(name)) {
                        longNameId = q;
                        break;
                    }
                }
            }

            if (longNameId == Integer.MIN_VALUE) {
                longNames.add(name);
                longNameId = lSize;
            }

            if (prefix.length() > 15) {
                name = new String(prefix.substring(0, 15) + "__" + theRowSet.size() + "_" + longNameId);
            } else {
                name = new String(prefix + "__" + theRowSet.size() + "_" + longNameId);
            }

        }


        // Types that are arrays of scalers don't  have 'create type' statements...

        if (newPlsql.typeImplementingClass != null
                && newPlsql.typeImplementingClass.objectType == SingleNamespaceObject.PLSQL_PACK_SCALER_ARRAY
                && (!newPlsql.typeImplementingClass.dataType.equals("PL/SQL RECORD"))) {
            createStatement.clear();
            createStatement.add("");

            if (newPlsql.typeImplementingClass.dataType.indexOf("TIMESTAMP") > -1) {
                createArrayStatement = "CREATE OR REPLACE TYPE " + name.toUpperCase() + "_A  AS TABLE OF "
                        + JavaUtils.replaceString(newPlsql.typeImplementingClass.dataType, "TIMESTAMP", "TIMESTAMP" + sizeDef)
                        + ";";
            } else {
                createArrayStatement = "CREATE OR REPLACE TYPE " + name.toUpperCase() + "_A  AS TABLE OF " + newPlsql.typeImplementingClass.dataType + sizeDef + ";";
            }
        } else {
            createStatement.set(0, "CREATE OR REPLACE TYPE " + name.toUpperCase() + "_T AS OBJECT");
            dropTypeStatement = "DROP TYPE " + name.toUpperCase() + "_T;";
            createArrayStatement = "CREATE OR REPLACE TYPE " + name.toUpperCase() + "_A  AS TABLE OF " + name.toUpperCase() + "_T;";
        }

        dropArrayStatement = "DROP TYPE " + name.toUpperCase() + "_A;";


    }

    String getName() {
        return name;
    }


    public String[] getCreateStatement() {
        createStatement.trimToSize();

        String[] outStr = new String[createStatement.size()];
        Object[] outObj = createStatement.toArray();

        for (int i = 0; i < outObj.length; i++) {
            outStr[i] = (String) outObj[i];
        }

        return outStr;
    }

    public String[] getAssignStatement() {
        assignStatement.trimToSize();

        String[] outStr = new String[assignStatement.size()];
        Object[] outObj = assignStatement.toArray();

        if (outStr.length == 1) {
            String foo = "bar"; //DRKLUGE
        }
        for (int i = 0; i < outObj.length; i++) {
            outStr[i] = (String) outObj[i];
        }

        return outStr;
    }

    public String[] getWeirdAssignStatement() {
        weirdAssignStatement.trimToSize();

        String[] outStr = new String[weirdAssignStatement.size()];
        Object[] outObj = weirdAssignStatement.toArray();

        if (outStr.length == 1) {
            String foo = "bar"; //DRKLUGE
        }
        for (int i = 0; i < outObj.length; i++) {
            outStr[i] = (String) outObj[i];
        }

        return outStr;
    }

    public String[] getUnassignStatement() {
        unAssignStatement.trimToSize();

        String[] outStr = new String[unAssignStatement.size()];
        Object[] outObj = unAssignStatement.toArray();

        for (int i = 0; i < outObj.length; i++) {
            outStr[i] = (String) outObj[i];
        }

        return outStr;
    }

    public String getCreateArrayStatement() {
        return createArrayStatement;
    }

    public String getDropTypeStatement() {
        return dropTypeStatement;
    }

    public String getDropArrayStatement() {
        return dropArrayStatement;
    }


    private String getCustomName(ReadOnlyRowSet theRowSet) throws CSException {
        theRowSet.first();

        String customName = "";

        try {
            customName = theRowSet.getInt("OBJECT_ID") + "_" + theRowSet.getInt("SUBPROGRAM_ID");
        } catch (Exception e) {
            customName = arrayId + "_" + theRowSet.getInt("OBJECT_ID");
        }

        customName = customName + "_" + theRowSet.getInt("POSITION");

        return customName;
    }


}
