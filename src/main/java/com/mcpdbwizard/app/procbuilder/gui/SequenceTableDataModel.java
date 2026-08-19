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
public class SequenceTableDataModel extends RowSetTableModel {

    public static final int SELECTED = 2;
    protected static final int OWNER_COLUMN = 0;
    protected static final int OBJECT_NAME_COLUMN = 1;
    protected static final int ACCESSED_VIA = 3;
    protected static final int REAL_OWNER = 4;
    protected static final int REAL_NAME = 5;
    protected static final int ORACLE_NAME = 6;
    protected static final int JAVA_NAME = 7;
    protected static final int FIXED_JAVA_NAME = 8;

    protected static final int SEQ_START_ID = 0;
    protected static final int SEQ_MISSING_LIMIT = 50;

    protected final String SEQUENCE_COUNT = "SEQUENCE_COUNT";
    protected final String SEQUENCE_USER_NAME = "SEQUENCE_USER_";
    protected final String SEQUENCE_MCP_DESC = "SEQUENCE_MCP_DESC_";

    /**
     * Author-supplied MCP tool descriptions, keyed on OWNER/NAME rather than on the config
     * index, because the index is re-assigned on every save -- the same reason
     * {@code TableTableDataModel} carries its CRUD letters this way. Keyed by index, deleting
     * one sequence would silently move another's description onto it.
     */
    private final java.util.HashMap<String, String> mcpDescBySequence =
            new java.util.HashMap<String, String>();

    private static String mcpDescKey(String theOwner, String theName) {
        return theOwner + "." + theName;
    }
    protected final String SEQUENCE_SEQUENCE_NAME = "SEQUENCE_NAME_";

    public SequenceTableDataModel(WriteableRowSet pRowSet, LogInterface pLog,
                                  McpDbWizardEventListener pListener) {
        super(pRowSet, pLog, pListener);

        for (int i = 0; i < theRowSet.size(); i++) {
            theRowSet.setCurrentRowNumber(i);

            try {
                theRowSet.setString(SELECTED, "No");
                theRowSet.setString(JAVA_NAME, JavaUtils.getJavaName(theRowSet
                        .getString(OBJECT_NAME_COLUMN)));
                theRowSet.setString(FIXED_JAVA_NAME, theRowSet
                        .getString(JAVA_NAME));
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
                    newObject = Boolean.TRUE;
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
                            McpDbWizardEvent.A_SEQUENCE_SELECTED);

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

    private void clearSequenceInfo(Properties theProperties) {
        boolean moreProperties = true;
        int propertyCount = SEQ_START_ID;
        int missingCount = 0;
        String testString = "";

        while (moreProperties) {
            testString = new String(SEQUENCE_SEQUENCE_NAME + (propertyCount));

            if (theProperties.containsKey(testString)) {
                missingCount = 0;
                theProperties.remove(SEQUENCE_SEQUENCE_NAME + (propertyCount));
                theProperties.remove(SEQUENCE_USER_NAME + (propertyCount));
            } else {
                missingCount++;
            }

            if (missingCount > SEQ_MISSING_LIMIT) {
                moreProperties = false;
            }

            propertyCount++;
        }

    }

    void writeSequenceInfo(Properties theProperties) {
        clearSequenceInfo(theProperties);

        try {
            int j = SEQ_START_ID;

            for (int i = 0; i < theRowSet.size(); i++) {
                theRowSet.setCurrentRowNumber(i);

                if (theRowSet.getBoolean(SELECTED)) {
                    theProperties.setProperty(SEQUENCE_SEQUENCE_NAME + j,
                            theRowSet.getString("Sequence Name"));
                    theProperties.setProperty(SEQUENCE_USER_NAME + j, theRowSet
                            .getString("Sequence Owner"));
                    String theDesc = mcpDescBySequence.get(mcpDescKey(
                            theRowSet.getString("Sequence Owner"),
                            theRowSet.getString("Sequence Name")));
                    if (theDesc != null) {
                        theProperties.setProperty(SEQUENCE_MCP_DESC + j, theDesc);
                    }
                    j++;
                }
            }
        } catch (CSException e) {
        }
    }

    void readSequenceInfo(Properties theProperties) {
        boolean moreProperties = true;
        int propertyCount = SEQ_START_ID;
        int missingCount = 0;
        String testString = "";

        while (moreProperties) {
            testString = new String(SEQUENCE_SEQUENCE_NAME + (propertyCount));

            if (theProperties.containsKey(testString)) {
                String tempUserName = theProperties
                        .getProperty(SEQUENCE_USER_NAME + (propertyCount));
                String tempSeqName = theProperties
                        .getProperty(SEQUENCE_SEQUENCE_NAME + (propertyCount));
                String tempDesc = theProperties
                        .getProperty(SEQUENCE_MCP_DESC + (propertyCount));

                if (tempDesc != null) {
                    mcpDescBySequence.put(mcpDescKey(tempUserName, tempSeqName), tempDesc);
                }

                missingCount = 0;

                // Go through result set looking for this sequence. When found
                // set the flag.
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

            if (missingCount > SEQ_MISSING_LIMIT) {
                moreProperties = false;
            }

            propertyCount++;
        }
    }

    public SingleNamespaceObject[] getOracleSequences() {
        ArrayList tempArrayList = new ArrayList(0);

        for (int i = 0; i < theRowSet.size(); i++) {
            try {
                theRowSet.setCurrentRowNumber(i);
                if (theRowSet.getBoolean(SELECTED)) {
                    SingleNamespaceObject tempSequence = new SingleNamespaceObject(
                            theRowSet.getString(OWNER_COLUMN), theRowSet
                            .getString(OBJECT_NAME_COLUMN),
                            SingleNamespaceObject.SEQUENCE);
                    tempSequence.javaName = theRowSet.getString(JAVA_NAME);
                    tempSequence.fixedJavaName = theRowSet
                            .getString(FIXED_JAVA_NAME);
                    tempSequence.oracleName = theRowSet.getString(ORACLE_NAME);
                    tempSequence.oracleNameBasis = theRowSet
                            .getString(ACCESSED_VIA);
                    tempSequence.realOwner = theRowSet.getString(REAL_OWNER);
                    String theDesc = mcpDescBySequence.get(mcpDescKey(
                            theRowSet.getString(OWNER_COLUMN),
                            theRowSet.getString(OBJECT_NAME_COLUMN)));
                    if (theDesc != null) {
                        tempSequence.mcpDescriptions = new java.util.HashMap<String, String>();
                        tempSequence.mcpDescriptions.put(
                                SingleNamespaceObject.MCP_DESC_SOLE, theDesc);
                    }

                    tempArrayList.add(tempSequence);
                }
            } catch (CSException e) {
                theLog.syserror(e);
            }
        }

        tempArrayList.trimToSize();
        Object[] tempObjectArray = tempArrayList.toArray();
        SingleNamespaceObject[] tempOracleSequenceArray = new SingleNamespaceObject[tempObjectArray.length];

        for (int i = 0; i < tempObjectArray.length; i++) {
            tempOracleSequenceArray[i] = (SingleNamespaceObject) tempObjectArray[i];
        }

        return (tempOracleSequenceArray);
    }

    public void selectSequences(boolean newSetting) {
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
