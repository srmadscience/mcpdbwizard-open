package com.mcpdbwizard.app.procbuilder.gui;

import javax.swing.table.*;
import javax.swing.JTable;
import java.awt.Component;
import javax.swing.JLabel;
import java.awt.*;

import com.mcpdbwizard.app.procbuilder.*;

/**
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class SqlParameterCellRenderer extends JLabel implements TableCellRenderer {
    final int NUMBER_COL = 0;
    final int NAME_COL = 1;
    final int DATATYPE_COL = 2;

    SqlStatementWrangler theWrangler = null;

    public SqlParameterCellRenderer(SqlStatementWrangler theWrangler) {
        this.theWrangler = theWrangler;
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int rowIndex, int colIndex) {
        setEnabled(true);
        setForeground(Color.black);
        Font f = new Font("SansSerif", 0, 12); // this.getFont();

        setText(value.toString());

        //System.out.println(value);
        // Set tool tip if desired
        setToolTipText(new String("Parameter at line #" + (rowIndex + 1) + " "));

        // Now the funny business....
        if (colIndex != NUMBER_COL) {
            if (!theWrangler.isCellEditable(rowIndex, colIndex)) {
                setEnabled(false);
                if (colIndex == NAME_COL) {
                    setToolTipText(new String("'" + ((String) value) + "' set by hint at line " + theWrangler.getLineNumber(rowIndex) + "; Can not be overridden without changing SQL file"));
                } else if (colIndex == DATATYPE_COL) {
                    setToolTipText(new String("'" + ((String) value) + "' set by hint at line " + theWrangler.getLineNumber(rowIndex) + "; Can not be overridden without changing SQL file"));
                }
            } else {
                setEnabled(true);
                if (colIndex == NAME_COL) {
                    setToolTipText(new String("Parameter Name for parameter at line " + theWrangler.getLineNumber(rowIndex)));
                } else if (colIndex == DATATYPE_COL) {
                    setToolTipText(new String("Parameter Data Type for parameter at line " + theWrangler.getLineNumber(rowIndex)));
                }
            }
        }
        setFont(f);
        return this;
    }

}

