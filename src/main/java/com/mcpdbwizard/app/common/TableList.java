package com.mcpdbwizard.app.common;

import javax.swing.table.TableModel;
import javax.swing.event.TableModelListener;

import com.mcpdbwizard.app.procbuilder.gui.SqlListCellRenderer;

/**
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class TableList implements TableModel {
    String[] stringArray = null;

    public TableList(String[] stringArray) {
        this.stringArray = stringArray;
    }

    public int getRowCount() {
        return (stringArray.length);
    }

    public int getColumnCount() {
        return (2);
    }

    public String getColumnName(int columnIndex) {
        if (columnIndex == 0) {
            return ("Line #");
        }
        return ("Text");
    }

    public Class getColumnClass(int columnIndex) {
        if (columnIndex == 0) {
            return (String.class);
        }
        return (SqlListCellRenderer.class);
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return (false);
    }

    public Object getValueAt(int rowIndex, int columnIndex) {

        if (columnIndex == 0) {
            return (rowIndex + 1);
        }

        return (stringArray[rowIndex]);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

    }

    public void addTableModelListener(TableModelListener l) {
        //TODO: implement this javax.swing.table.TableModel method;
    }

    public void removeTableModelListener(TableModelListener l) {
        //TODO: implement this javax.swing.table.TableModel method;
    }
}

