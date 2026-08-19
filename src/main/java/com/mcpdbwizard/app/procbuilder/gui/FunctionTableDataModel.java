package com.mcpdbwizard.app.procbuilder.gui;

import java.util.Properties;
import java.util.ArrayList;

//import javax.swing.table.*;
import com.mcpdbwizard.pub.*;
import com.mcpdbwizard.app.common.gui.RowSetTableModel;
import com.mcpdbwizard.app.common.*;

/**
 * @author devteam@mcpdbwizard.com Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 * @version 2
 */
public class FunctionTableDataModel extends RowSetTableModel {

    public static final int SELECTED = 2;
    protected static final int OWNER_COLUMN = 0;
    protected static final int OBJECT_NAME_COLUMN = 1;
    protected static final int ACCESSED_VIA = 3;
    protected static final int REAL_OWNER = 4;
    protected static final int REAL_NAME = 5;
    protected static final int ORACLE_NAME = 6;
    protected static final int JAVA_NAME = 7;
    protected static final int FIXED_JAVA_NAME = 8;
    protected static final int PACKAGE_NAME = 9;
    protected static final int REAL_PACKAGE_NAME = 10;
    protected static final int OVERLOAD = 11;

    protected static final int SEQ_START_ID = 0;
    protected static final int SEQ_MISSING_LIMIT = 500;

    protected final String FUNCTION_USER_NAME = "PROC_USER_";
    protected final String FUNCTION_NAME = "PROC_NAME_";
    protected final String FUNCTION_PACKAGE = "PROC_PACKAGE_";
    protected final String FUNCTION_OVERLOAD = "PROC_OVERLOAD_";
    protected final String FUNCTION_MCP_DESC = "PROC_MCP_DESC_";

    /**
     * Author-supplied MCP tool descriptions, keyed on owner/name/package/OVERLOAD -- all four,
     * because overloads are separate config entries that become separate tools, and two of them
     * differ in nothing else. Not keyed by config index: that is re-assigned on every save.
     */
    private final java.util.HashMap<String, String> mcpDescByFunction =
            new java.util.HashMap<String, String>();

    private static String mcpDescKey(String theOwner, String theName, String thePackage,
                                     String theOverload) {
        return theOwner + "." + thePackage + "." + theName + "#" + theOverload;
    }

    /**
     * The same identity in the form the config uses — {@code owner::pkg::name::overload}.
     *
     * <p>Separate from {@link #mcpDescKey} on purpose: that one is this class's private lookup key
     * and its format may change freely, while this one is a contract with whatever reads the
     * generator's "not exposed as a tool" report.
     *
     * <p><b>The literal string {@code "null"} is passed through, deliberately.</b> An absent
     * package or overload reaches {@code writeFunctionInfo} through the {@code + ""} idiom, so what
     * lands in {@code PROC_PACKAGE_<i>} / {@code PROC_OVERLOAD_<i>} is four characters of text, not
     * an empty value — and a reader building the same key from the config gets {@code "null"} back.
     * "Tidying" it to empty here produces a key that matches nothing for exactly the routines that
     * have no package or no overload, which is most of them. The first draft did that.
     */
    private static String mcpConfigId(String theOwner, String theName, String thePackage,
                                      String theOverload) {
        return theOwner + SingleNamespaceObject.MCP_ID_SEP + thePackage
                + SingleNamespaceObject.MCP_ID_SEP + theName
                + SingleNamespaceObject.MCP_ID_SEP + theOverload;
    }

    public FunctionTableDataModel(WriteableRowSet pRowSet, LogInterface pLog,
                                  McpDbWizardEventListener pListener) {
        super(pRowSet, pLog, pListener);


        for (int i = 0; i < theRowSet.size(); i++) {
            theRowSet.setCurrentRowNumber(i);

            try {
                theRowSet.setString(SELECTED, "No");

                if (theRowSet.getString(PACKAGE_NAME) == null) {
                    theRowSet.setString(JAVA_NAME, JavaUtils
                            .getJavaName(theRowSet
                                    .getString(OBJECT_NAME_COLUMN)));
                } else {
                    theRowSet.setString(JAVA_NAME, JavaUtils
                            .getJavaName(theRowSet.getString(PACKAGE_NAME)
                                    + "_"
                                    + theRowSet.getString(OBJECT_NAME_COLUMN)));
                }

                String overload = theRowSet.getString(OVERLOAD);

                if (overload != null) {
                    theRowSet.setString(JAVA_NAME, theRowSet
                            .getString(JAVA_NAME)
                            + "_overload_" + overload);
                }

                theRowSet.setString(FIXED_JAVA_NAME, new String(theRowSet
                        .getString(JAVA_NAME)));

                theRowSet.setString(ORACLE_NAME, JavaUtils.getOracleName(
                        theRowSet.getString(OWNER_COLUMN), theRowSet
                                .getString(OBJECT_NAME_COLUMN), theRowSet
                                .getString(ACCESSED_VIA), theRowSet
                                .getString(PACKAGE_NAME), true));
            } catch (Exception e) {
                theLog.syserror(e);
            }
        }
    }

    public int getColumnCount() {

        return REAL_NAME + 1;
    }

    public int getRowCount() {

        return theRowSet.size();
    }

    public Object getValueAt(int row, int col) {

        Object newObject;

        try {
            theRowSet.setCurrentRowNumber(row);

            newObject = theRowSet.getObject(col);

            if (newObject == null) {
                newObject = "";
            }

            if (col == SELECTED) {
                if (newObject.equals("Yes")) {
                    newObject = Boolean.TRUE;
                } else {
                    newObject = Boolean.FALSE;
                }
            } else if (col == OBJECT_NAME_COLUMN) {
                String packName = theRowSet.getString(PACKAGE_NAME);

                if (packName == null) {
                    newObject = new String(theRowSet
                            .getString(OBJECT_NAME_COLUMN));
                } else {
                    newObject = new String(packName + "."
                            + theRowSet.getString(OBJECT_NAME_COLUMN));
                }

                String overload = theRowSet.getString(OVERLOAD);

                if (overload != null) {
                    newObject = newObject + " - overload #" + overload;
                }

            } else if (col == REAL_NAME) {
                String packName = null;

                packName = theRowSet.getString(REAL_PACKAGE_NAME);

                if (packName == null) {
                    newObject = new String(theRowSet.getString(REAL_NAME));
                    if (newObject == null) {
                        newObject = "?";
                    }
                } else {
                    newObject = new String(packName + "."
                            + theRowSet.getString(REAL_NAME));
                }
            }

            if (newObject == null) {
                newObject = "";
            }
        } catch (CSException e) {
            newObject = "";
        } catch (Exception e) {
            theLog.syserror("Unable to get row " + row + " column " + col);
            newObject = "";
        }
        return (newObject);
    }

    public Class getColumnClass(int columnIndex) {

        if (columnIndex == SELECTED) {
            return (Boolean.class);
        }
        return (String.class);
    }

    /**
     *
     */
    public boolean isCellEditable(int rowIndex, int columnIndex) {

        if (columnIndex == SELECTED) {
            return (true);
        }
        return (false);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

        if (columnIndex == SELECTED) {
            theRowSet.setCurrentRowNumber(rowIndex);
            try {
                boolean hasChanged = false;
                if (((Boolean) aValue).booleanValue()) {
                    if (!theRowSet.getString(SELECTED).equals("Yes")) {
                        theRowSet.setString(SELECTED, "Yes");
                        hasChanged = true;
                    }

                } else {
                    if (!theRowSet.getString(SELECTED).equals("No")) {
                        theRowSet.setString(SELECTED, "No");
                        hasChanged = true;
                    }
                }

                if (hasChanged) {
                    McpDbWizardEvent newEvent = new McpDbWizardEvent(
                            McpDbWizardEvent.A_FUNCTION_SELECTED);

                    newEvent.setThing(rowIndex);
                    Boolean thing2 = (Boolean) aValue;
                    newEvent.setThing2(thing2);

                    if (theListener != null) {
                        theListener.reportEvent(newEvent);
                    }
                }

            } catch (CSException e) {
            }
        }
    }

    private void clearFunctionInfo(Properties theProperties) {

        boolean moreProperties = true;
        int propertyCount = SEQ_START_ID;
        int missingCount = 0;
        String testString = "";

        while (moreProperties) {
            testString = new String(FUNCTION_NAME + (propertyCount));

            if (theProperties.containsKey(testString)) {
                missingCount = 0;
                theProperties.remove(FUNCTION_NAME + (propertyCount));
                theProperties.remove(FUNCTION_USER_NAME + (propertyCount));
            } else {
                missingCount++;
            }

            if (missingCount > SEQ_MISSING_LIMIT) {
                moreProperties = false;
            }

            propertyCount++;
        }

    }

    void writeFunctionInfo(Properties theProperties) {

        clearFunctionInfo(theProperties);

        try {
            int j = SEQ_START_ID;

            for (int i = 0; i < theRowSet.size(); i++) {
                theRowSet.setCurrentRowNumber(i);

                if (theRowSet.getBoolean(SELECTED)) {
                    theProperties.setProperty(FUNCTION_NAME + j, theRowSet
                            .getString(OBJECT_NAME_COLUMN));
                    theProperties.setProperty(FUNCTION_USER_NAME + j, theRowSet
                            .getString(OWNER_COLUMN));
                    theProperties.setProperty(FUNCTION_PACKAGE + j, theRowSet
                            .getString(PACKAGE_NAME)
                            + "");
                    theProperties.setProperty(FUNCTION_OVERLOAD + j, theRowSet
                            .getString(OVERLOAD)
                            + "");
                    String theDesc = mcpDescByFunction.get(mcpDescKey(
                            theRowSet.getString(OWNER_COLUMN),
                            theRowSet.getString(OBJECT_NAME_COLUMN),
                            theRowSet.getString(PACKAGE_NAME) + "",
                            theRowSet.getString(OVERLOAD) + ""));
                    if (theDesc != null) {
                        theProperties.setProperty(FUNCTION_MCP_DESC + j, theDesc);
                    }
                    j++;
                }
            }
        } catch (CSException e) {
        }
    }

    void readFunctionInfo(Properties theProperties) {

        boolean moreProperties = true;
        int propertyCount = SEQ_START_ID;
        int missingCount = 0;
        String testString = "";

        while (moreProperties) {
            testString = new String(FUNCTION_NAME + (propertyCount));

            if (theProperties.containsKey(testString)) {
                String tempUserName = theProperties
                        .getProperty(FUNCTION_USER_NAME + (propertyCount));
                String tempSeqName = theProperties.getProperty(FUNCTION_NAME
                        + (propertyCount));
                String tempPackName = theProperties
                        .getProperty(FUNCTION_PACKAGE + (propertyCount));
                String tempOverload = theProperties
                        .getProperty(FUNCTION_OVERLOAD + (propertyCount));
                String tempDesc = theProperties
                        .getProperty(FUNCTION_MCP_DESC + (propertyCount));

                if (tempDesc != null) {
                    mcpDescByFunction.put(mcpDescKey(tempUserName, tempSeqName,
                            tempPackName + "", tempOverload + ""), tempDesc);
                }

                missingCount = 0;

                // Go through result set looking for this function. When found
                // set the flag.
                for (int i = 0; i < theRowSet.size(); i++) {
                    theRowSet.setCurrentRowNumber(i);

                    try {

                        if (theRowSet.getString(OWNER_COLUMN).equals(
                                tempUserName)
                                && theRowSet.getString(OBJECT_NAME_COLUMN)
                                .equals(tempSeqName)
                                && ("" + theRowSet.getString(PACKAGE_NAME))
                                .equals("" + tempPackName)
                                && ("" + theRowSet.getString(OVERLOAD))
                                .equals("" + tempOverload)) {
                            theRowSet.setString(SELECTED, "Yes");
                            break;
                        }
                    } catch (CSException e) {
                    }

                }
            } else {
                missingCount++;
            }

            if (missingCount > SEQ_MISSING_LIMIT) {
                moreProperties = false;
            }

            propertyCount++;
        }
    }

    public SingleNamespaceObject[] getOracleFunctions() {

        ArrayList tempArrayList = new ArrayList(0);

        for (int i = 0; i < theRowSet.size(); i++) {
            try {
                theRowSet.setCurrentRowNumber(i);
                if (theRowSet.getBoolean(SELECTED)) {
                    SingleNamespaceObject tempfunction = new SingleNamespaceObject(
                            theRowSet.getString(OWNER_COLUMN), theRowSet
                            .getString(OBJECT_NAME_COLUMN),
                            SingleNamespaceObject.FUNCTION);
                    tempfunction.javaName = theRowSet.getString(JAVA_NAME);
                    tempfunction.fixedJavaName = theRowSet
                            .getString(FIXED_JAVA_NAME);
                    tempfunction.oracleName = theRowSet.getString(ORACLE_NAME);
                    tempfunction.oracleNameBasis = theRowSet
                            .getString(ACCESSED_VIA);
                    tempfunction.realOwner = theRowSet.getString(REAL_OWNER);
                    tempfunction.realName = theRowSet.getString(REAL_NAME);
                    tempfunction.packageName = theRowSet
                            .getString(REAL_PACKAGE_NAME);
                    tempfunction.overload = theRowSet.getString(OVERLOAD);
                    // PACKAGE_NAME, not REAL_PACKAGE_NAME: the config stores the former (it is what
                    // writeFunctionInfo puts in PROC_PACKAGE_<i>), so a key built from the resolved
                    // name would never match the one built when reading the config back. The two
                    // differ whenever the routine is reached through a synonym.
                    // Same four components, joined the way the CONFIG identifies a routine, and
                    // carried on the object so a "this yields no MCP tool" report can name it that
                    // way — see SingleNamespaceObject.mcpConfigId for why rebuilding it later from
                    // the object's own fields would miss every synonym-reached routine.
                    tempfunction.mcpConfigId = mcpConfigId(
                            theRowSet.getString(OWNER_COLUMN),
                            theRowSet.getString(OBJECT_NAME_COLUMN),
                            theRowSet.getString(PACKAGE_NAME) + "",
                            theRowSet.getString(OVERLOAD) + "");
                    String theDesc = mcpDescByFunction.get(mcpDescKey(
                            theRowSet.getString(OWNER_COLUMN),
                            theRowSet.getString(OBJECT_NAME_COLUMN),
                            theRowSet.getString(PACKAGE_NAME) + "",
                            theRowSet.getString(OVERLOAD) + ""));
                    if (theDesc != null) {
                        tempfunction.mcpDescriptions = new java.util.HashMap<String, String>();
                        tempfunction.mcpDescriptions.put(
                                SingleNamespaceObject.MCP_DESC_SOLE, theDesc);
                    }
                    tempArrayList.add(tempfunction);
                }
            } catch (CSException e) {
                theLog.syserror(e);
            }
        }

        tempArrayList.trimToSize();
        Object[] tempObjectArray = tempArrayList.toArray();
        SingleNamespaceObject[] tempOraclefunctionArray = new SingleNamespaceObject[tempObjectArray.length];

        for (int i = 0; i < tempObjectArray.length; i++) {
            tempOraclefunctionArray[i] = (SingleNamespaceObject) tempObjectArray[i];
        }

        return (tempOraclefunctionArray);
    }

    public void selectFunctions(boolean newSetting) {

        for (int i = 0; i < theRowSet.size(); i++) {
            theRowSet.setCurrentRowNumber(i);
            try {
                if (newSetting) {
                    theRowSet.setString(SELECTED, "Yes");
                } else {
                    theRowSet.setString(SELECTED, "No");
                }
            } catch (CSException e) {
            }
        }
    }

}
