package com.mcpdbwizard.app.common.gui;

import javax.swing.table.*;

import com.mcpdbwizard.pub.*;
import com.mcpdbwizard.app.procbuilder.gui.McpDbWizardEventListener;

/**
 * @author devteam@mcpdbwizard.com
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 * @version 2
 */
public class RowSetTableModel extends AbstractTableModel {
    protected WriteableRowSet theRowSet = null;
    protected LogInterface theLog;
    protected McpDbWizardEventListener theListener;

    public RowSetTableModel(WriteableRowSet pRowSet, LogInterface pLog
            , McpDbWizardEventListener pListener) {
        theRowSet = pRowSet;
        theLog = pLog;
        theListener = pListener;
    }

    public void setRowSet(WriteableRowSet pRowSet) {
        theRowSet = pRowSet;
    }

    public int getColumnCount() {
        return theRowSet.width();
    }

    public int getRowCount() {
        return theRowSet.size();
    }

    public Object getValueAt(int row, int col) {
        Object newObject;
        theRowSet.setCurrentRowNumber(row);
        try {
            newObject = theRowSet.getString(col);
        } catch (CSException e) {
            newObject = "";
        }

        return (newObject);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    }

    public String getColumnName(int column) {
        String newColumnName = "";

        try {
            newColumnName = theRowSet.getColumnName(column);
        } catch (com.mcpdbwizard.pub.CSInvalidColumnIdException e) {
            newColumnName = null;
        }
        return (newColumnName);
    }


}





