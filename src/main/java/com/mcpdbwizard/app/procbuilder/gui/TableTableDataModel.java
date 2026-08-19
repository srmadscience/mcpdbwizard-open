package com.mcpdbwizard.app.procbuilder.gui;

import java.util.Properties;
import java.util.ArrayList;

//import javax.swing.table.*;
import com.mcpdbwizard.pub.*;
import com.mcpdbwizard.app.common.*;
import com.mcpdbwizard.app.common.gui.*;

/**
 * @author devteam@mcpdbwizard.com Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 * @version 2
 */
public class TableTableDataModel extends RowSetTableModel {

    public static final int OWNER_COLUMN = 0;
    public static final int OBJECT_NAME_COLUMN = 1;
    public static final int SELECTED = 2;
    public static final int ACCESSED_VIA = 3;
    public static final int REAL_OWNER = 4;
    public static final int REAL_NAME = 5;
    public static final int ORACLE_NAME = 6;
    public static final int JAVA_NAME = 7;
    public static final int FIXED_JAVA_NAME = 8;

    protected static final int TAB_START_ID = 0;
    protected static final int TAB_MISSING_LIMIT = 50;

    protected final String TABLE_COUNT = "TABLE_COUNT";
    protected final String TABLE_USER_NAME = "TABLE_USER_";
    protected final String TABLE_TABLE_NAME = "TABLE_NAME_";
    protected final String TABLE_MCP_CRUD = "TABLE_MCP_CRUD_";
    protected final String TABLE_MCP_DESC = "TABLE_MCP_DESC_";

    /**
     * Per-table MCP curation read from {@code TABLE_MCP_CRUD_<i>}, keyed
     * {@code owner + "." + tableName} (the same owner/name pair the selection itself is
     * matched on). Held here rather than in the row set because the row set's columns come
     * from the table query's SELECT list, and this value is not a database attribute.
     * A table with no entry keeps the default of all four operations.
     */
    private final java.util.HashMap<String, String> mcpCrudByTable =
            new java.util.HashMap<String, String>();

    /**
     * Author-supplied MCP tool descriptions, per table and then per OPERATION, because one table
     * yields several tools. Keyed on owner/name for the same reason as {@link #mcpCrudByTable}:
     * the config index is re-assigned on every save, so keying by index would move a description
     * onto a different table the moment one was removed.
     */
    private final java.util.HashMap<String, java.util.Map<String, String>> mcpDescByTable =
            new java.util.HashMap<String, java.util.Map<String, String>>();

    public TableTableDataModel(WriteableRowSet pRowSet, LogInterface pLog,
                               McpDbWizardEventListener pListener) {
        super(pRowSet, pLog, pListener);

        for (int i = 0; i < theRowSet.size(); i++) {
            theRowSet.setCurrentRowNumber(i);

            try {
                theRowSet.setString(SELECTED, "No");
                theRowSet.setString(JAVA_NAME, JavaUtils.getJavaName(theRowSet
                        .getString(OBJECT_NAME_COLUMN)));

                char[] tempCharArray = theRowSet.getString(JAVA_NAME)
                        .toCharArray();

                if (tempCharArray.length > 0 && tempCharArray[0] >= 'a'
                        && tempCharArray[0] <= 'z') {
                    tempCharArray[0] = Character.toUpperCase(tempCharArray[0]);
                }

                theRowSet.setString(FIXED_JAVA_NAME, new String(tempCharArray));

                theRowSet.setString(ORACLE_NAME, JavaUtils.getOracleName(
                        theRowSet.getString(OWNER_COLUMN), theRowSet
                                .getString(OBJECT_NAME_COLUMN), theRowSet
                                .getString(ACCESSED_VIA), null, true));
            } catch (CSException e) {
            }
        }
    }

    public int getColumnCount() {
        return REAL_NAME + 1;
    }

    public Object getValueAt(int row, int col) {
        Object newObject;
        theRowSet.setCurrentRowNumber(row);
        try {
            newObject = theRowSet.getObject(col);
            if (col == SELECTED) {
                if (newObject.equals("Yes")) {
                    newObject =  Boolean.TRUE;
                } else {
                    newObject =  Boolean.FALSE;
                }
            }
        } catch (CSException e) {
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
                            McpDbWizardEvent.A_TABLE_SELECTED);

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

    private void clearTableInfo(Properties theProperties) {
        boolean moreProperties = true;
        int propertyCount = TAB_START_ID;
        int missingCount = 0;
        String testString = "";

        while (moreProperties) {
            testString = new String(TABLE_TABLE_NAME + (propertyCount));

            if (theProperties.containsKey(testString)) {
                missingCount = 0;
                theProperties.remove(TABLE_TABLE_NAME + (propertyCount));
                theProperties.remove(TABLE_USER_NAME + (propertyCount));
                theProperties.remove(TABLE_MCP_CRUD + (propertyCount));
            } else {
                missingCount++;
            }

            if (missingCount > TAB_MISSING_LIMIT) {
                moreProperties = false;
            }

            propertyCount++;
        }

    }

    void writeTableInfo(Properties theProperties) {
        clearTableInfo(theProperties);

        try {
            int j = TAB_START_ID;

            for (int i = 0; i < theRowSet.size(); i++) {
                theRowSet.setCurrentRowNumber(i);

                if (theRowSet.getBoolean(SELECTED)) {
                    theProperties.setProperty(TABLE_TABLE_NAME + j, theRowSet
                            .getString("Table Name"));
                    theProperties.setProperty(TABLE_USER_NAME + j, theRowSet
                            .getString("Table Owner"));

                    // Written only when curation was actually set: an absent key means all
                    // four operations, so a config that never touched this keeps its exact
                    // previous key set. Note the index j is re-assigned on every save, which
                    // is why the value is carried keyed on owner/name rather than by index.
                    String theCrud = mcpCrudByTable.get(mcpCrudKey(
                            theRowSet.getString("Table Owner"),
                            theRowSet.getString("Table Name")));
                    if (theCrud != null) {
                        theProperties.setProperty(TABLE_MCP_CRUD + j, theCrud);
                    }
                    java.util.Map<String, String> theDescs = mcpDescByTable.get(mcpCrudKey(
                            theRowSet.getString("Table Owner"),
                            theRowSet.getString("Table Name")));
                    if (theDescs != null) {
                        for (java.util.Map.Entry<String, String> e : theDescs.entrySet()) {
                            theProperties.setProperty(TABLE_MCP_DESC + j + "_" + e.getKey(),
                                    e.getValue());
                        }
                    }
                    j++;
                }
            }
        } catch (CSException e) {
        }
    }

    void readTableInfo(Properties theProperties) {
        boolean moreProperties = true;
        int propertyCount = TAB_START_ID;
        int missingCount = 0;
        String testString = "";

        while (moreProperties) {
            testString = new String(TABLE_TABLE_NAME + (propertyCount));

            if (theProperties.containsKey(testString)) {
                String tempUserName = theProperties.getProperty(TABLE_USER_NAME
                        + (propertyCount));
                String tempSeqName = theProperties.getProperty(TABLE_TABLE_NAME
                        + (propertyCount));
                String tempCrud = theProperties.getProperty(TABLE_MCP_CRUD
                        + (propertyCount));

                if (tempCrud != null) {
                    mcpCrudByTable.put(mcpCrudKey(tempUserName, tempSeqName), tempCrud);
                }

                // A table has one description PER OPERATION, so the keys carry a trailing
                // operation and cannot be fetched by name -- collect every key for this index.
                String theDescPrefix = TABLE_MCP_DESC + propertyCount + "_";
                for (String theKey : theProperties.stringPropertyNames()) {
                    if (theKey.startsWith(theDescPrefix)) {
                        java.util.Map<String, String> theMap = mcpDescByTable.get(
                                mcpCrudKey(tempUserName, tempSeqName));
                        if (theMap == null) {
                            theMap = new java.util.LinkedHashMap<String, String>();
                            mcpDescByTable.put(mcpCrudKey(tempUserName, tempSeqName), theMap);
                        }
                        theMap.put(theKey.substring(theDescPrefix.length()),
                                theProperties.getProperty(theKey));
                    }
                }

                missingCount = 0;

                // Go through result set looking for this table. When found set
                // the flag.
                for (int i = 0; i < theRowSet.size(); i++) {
                    theRowSet.setCurrentRowNumber(i);

                    try {

                        if (theRowSet.getString(OWNER_COLUMN).equals(
                                tempUserName)
                                && theRowSet.getString(OBJECT_NAME_COLUMN)
                                .equals(tempSeqName)) {
                            theRowSet.setString(SELECTED, "Yes");
                            break;
                        }
                    } catch (CSException e) {
                    }

                }
            } else {
                missingCount++;
            }

            if (missingCount > TAB_MISSING_LIMIT) {
                moreProperties = false;
            }

            propertyCount++;
        }
    }

    private static String mcpCrudKey(String theOwner, String theName) {
        return theOwner + "." + theName;
    }

    /**
     * Set the MCP row operations for one table, as the letters of {@code TABLE_MCP_CRUD_<i>}
     * ("C" insert, "R" get_by_pk + lookups, "U" update, "D" delete). Passing {@code null}
     * clears the entry, restoring the default of all four. For the web UI, which edits
     * curation directly rather than through the Swing grid.
     */
    public void setMcpCrud(String theOwner, String theName, String theFlags) {
        if (theFlags == null) {
            mcpCrudByTable.remove(mcpCrudKey(theOwner, theName));
        } else {
            mcpCrudByTable.put(mcpCrudKey(theOwner, theName), theFlags);
        }
    }

    /** The MCP row operations for one table, or {@code null} when unset (meaning all four). */
    public String getMcpCrud(String theOwner, String theName) {
        return mcpCrudByTable.get(mcpCrudKey(theOwner, theName));
    }

    public SingleNamespaceObject[] getOracleTables() {
        ArrayList tempArrayList = new ArrayList(0);

        for (int i = 0; i < theRowSet.size(); i++) {
            try {
                theRowSet.setCurrentRowNumber(i);
                if (theRowSet.getBoolean(SELECTED)) {
                    SingleNamespaceObject tempTable = new SingleNamespaceObject(
                            theRowSet.getString(OWNER_COLUMN), theRowSet
                            .getString(OBJECT_NAME_COLUMN),
                            SingleNamespaceObject.TABLE);
                    tempTable.javaName = theRowSet.getString(JAVA_NAME);
                    tempTable.fixedJavaName = theRowSet
                            .getString(FIXED_JAVA_NAME);
                    tempTable.oracleName = theRowSet.getString(ORACLE_NAME);
                    tempTable.oracleNameBasis = theRowSet
                            .getString(ACCESSED_VIA);
                    tempTable.realOwner = theRowSet.getString(REAL_OWNER);
                    tempTable.realName = theRowSet.getString(REAL_NAME);
                    tempTable.objectArrayId = i;
                    tempTable.mcpCrud = mcpCrudByTable.get(mcpCrudKey(
                            theRowSet.getString(OWNER_COLUMN),
                            theRowSet.getString(OBJECT_NAME_COLUMN)));
                    tempTable.mcpDescriptions = mcpDescByTable.get(mcpCrudKey(
                            theRowSet.getString(OWNER_COLUMN),
                            theRowSet.getString(OBJECT_NAME_COLUMN)));
                    // The config's own spelling of this table, carried so a "yields no MCP tool"
                    // report can name it that way — see SingleNamespaceObject.mcpConfigId.
                    tempTable.mcpConfigId = theRowSet.getString(OWNER_COLUMN)
                            + SingleNamespaceObject.MCP_ID_SEP
                            + theRowSet.getString(OBJECT_NAME_COLUMN);

                    tempArrayList.add(tempTable);
                }
            } catch (CSException e) {
                theLog.syserror(e);
            }
        }

        tempArrayList.trimToSize();
        Object[] tempObjectArray = tempArrayList.toArray();
        SingleNamespaceObject[] tempOracleTableArray = new SingleNamespaceObject[tempObjectArray.length];

        for (int i = 0; i < tempObjectArray.length; i++) {
            tempOracleTableArray[i] = (SingleNamespaceObject) tempObjectArray[i];
        }

        return (tempOracleTableArray);
    }

    public void selectTables(boolean newSetting) {
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
