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
    /**
     * The subscript the INBOUND copy writes at. An index-by table accepts any subscript, so that arm
     * substitutes the carried position; a nested table or VARRAY has been EXTENDed to COUNT and must
     * stay dense, so that arm substitutes the loop variable. One template, two answers.
     */
    public final static String PARAM_TARGET_TARGET_INDEX = "PARAM_TARGET_TARGET_INDEX";
    /** The source collection's own key, carried OUT as {@link #POSITION_ATTRIBUTE}. */
    public final static String PARAM_TARGET_SOURCE_INDEX = "PARAM_TARGET_SOURCE_INDEX";
    /**
     * The trailing attribute that carries a collection element's PL/SQL subscript across the
     * boundary. Without it a round trip silently renumbers: the shadow SQL type is keyed 1..COUNT,
     * so an index-by keyed 0.., sparse or negative comes back dense and goes back in dense.
     */
    public final static String POSITION_ATTRIBUTE = "MCPDBWIZARD_POS";
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


        // The DESTINATION subscript is the shadow array's own LAST, never the source key. An
        // index-by table is keyed by arbitrary BINARY_INTEGERs -- base 0, negative, or sparse --
        // while a nested table is keyed 1..COUNT, and indexing the second with the first is
        // ORA-06532 (base 0 / negative) or ORA-06533 (sparse). The emitters that consume this
        // template EXTEND one element per copied row, so LAST is the row just made.
        unAssignStatement.add(ExtraType.PARAM_TARGET_PARAM_ARRAY_NAME + "(" + ExtraType.PARAM_TARGET_PARAM_ARRAY_NAME + ".LAST) := " + ExtraType.PARAM_TARGET_PARAM_NAME_RTYPE);

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
                            + "(" + ExtraType.PARAM_TARGET_TARGET_INDEX + ")." + theRowSet.getString("ARGUMENT_NAME").toUpperCase()
                            + spaces.substring(0, (maxLength - theRowSet.getString("ARGUMENT_NAME").length()))
                            + " := "
                            + ExtraType.PARAM_TARGET_PARAM_ARRAY_NAME + "(i).COL_" + i
                            + ";";

                    unAssign = unAssign + ExtraType.PARAM_TARGET_PARAM_NAME + "(i)." + theRowSet.getString("ARGUMENT_NAME").toUpperCase();
                } else {
                    aLine = aLine + theRowSet.getString("ARGUMENT_NAME").toUpperCase() + spaces.substring(0, (maxLength - theRowSet.getString("ARGUMENT_NAME").length()))
                            + " " + theRowSet.getString("DATA_TYPE");

                    weirdAssign = weirdAssign + ExtraType.PARAM_TARGET_PARAM_ARRAY_NAME + "(i)." + theRowSet.getString("ARGUMENT_NAME").toUpperCase();
                    assign = assign + ExtraType.PARAM_TARGET_PARAM_NAME + "(" + ExtraType.PARAM_TARGET_TARGET_INDEX + ")." + theRowSet.getString("ARGUMENT_NAME").toUpperCase()
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

            // The element type has to be spelled the way CREATE TYPE accepts, not the way the
            // dictionary reported it -- a collection element read from the PL/SQL type views comes
            // back as 'TIMESTAMP WITH TZ', and 'TABLE OF TIMESTAMP(9) WITH TZ' is a syntax error.
            // Oracle does not reject it at generation time, only when the emitted DDL is run, so
            // without this the failure surfaces as a missing type at bind time.
            String theElementType = JavaUtils.oracleSqlTypeName(newPlsql.typeImplementingClass.dataType);

            if (theElementType.indexOf("TIMESTAMP") > -1) {
                createArrayStatement = "CREATE OR REPLACE TYPE " + name.toUpperCase() + "_A  AS TABLE OF "
                        + JavaUtils.replaceString(theElementType, "TIMESTAMP", "TIMESTAMP" + sizeDef)
                        + ";";
            } else {
                createArrayStatement = "CREATE OR REPLACE TYPE " + name.toUpperCase() + "_A  AS TABLE OF " + theElementType + sizeDef + ";";
            }
        } else {
            createStatement.set(0, "CREATE OR REPLACE TYPE " + name.toUpperCase() + "_T AS OBJECT");
            dropTypeStatement = "DROP TYPE " + name.toUpperCase() + "_T;";
            createArrayStatement = "CREATE OR REPLACE TYPE " + name.toUpperCase() + "_A  AS TABLE OF " + name.toUpperCase() + "_T;";

            // Only a RECORD element gets the position attribute, which is why this sits here rather
            // than in the field loop: the arm above builds a TABLE OF NUMBER (or VARCHAR2, ...) with
            // no attributes at all, and an object attribute on it would not compile.
            //
            // It goes LAST so every existing COL_n keeps its index, and the shape-derived type NAME
            // is deliberately left alone: every record collection gains the attribute uniformly, so
            // no two shapes can collide, and a name fork would force a second element class per
            // record shape (one class names one type).
            appendPositionAttribute();
        }

        dropArrayStatement = "DROP TYPE " + name.toUpperCase() + "_A;";


    }

    /**
     * Appends {@link #POSITION_ATTRIBUTE} to the object type and to the constructor call that fills
     * it. Both ends move together on purpose: the CREATE and the constructor must agree on arity, and
     * a mismatch is not a compile error -- it is an ORA-06550 at bind time, in generated code, on a
     * path the developer did not touch.
     */
    private void appendPositionAttribute() {
        int lastLine = createStatement.size() - 1;
        String ddl = (String) createStatement.get(lastLine);
        if (!ddl.endsWith(");")) {
            // No attribute list to extend -- the "record with no fields" case, which is already
            // failed discovery and emits an INVALID type. Adding to it would only hide that.
            return;
        }
        // A SEPARATE list entry, never a newline inside this one. createExtraTypeObjects() emits
        // each of these lines as a Java string literal, and an embedded newline makes the generated
        // ServiceImpl fail to compile with "unclosed string literal" -- far from the cause.
        createStatement.set(lastLine, ddl.substring(0, ddl.length() - 2));
        createStatement.add(" ," + POSITION_ATTRIBUTE + " NUMBER);");

        int lastUnassign = unAssignStatement.size() - 1;
        String ctor = (String) unAssignStatement.get(lastUnassign);
        if (ctor.endsWith(");")) {
            unAssignStatement.set(lastUnassign,
                    ctor.substring(0, ctor.length() - 2) + "," + PARAM_TARGET_SOURCE_INDEX + ");");
        }
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
