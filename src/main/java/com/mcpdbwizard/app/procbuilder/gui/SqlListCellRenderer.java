package com.mcpdbwizard.app.procbuilder.gui;

import javax.swing.table.*;
import javax.swing.JTable;
import java.awt.Component;
import java.awt.Font;
import java.awt.Color;
import javax.swing.JLabel;

import com.mcpdbwizard.app.procbuilder.*;

/**
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class SqlListCellRenderer extends JLabel implements TableCellRenderer {
    final int NUMBER_COL = 0;
    final int TEXT_COL = 1;

    SqlStatementWrangler theWrangler = null;

    public SqlListCellRenderer(SqlStatementWrangler theWrangler) {
        this.theWrangler = theWrangler;
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int rowIndex, int colIndex) {
        setEnabled(true);

        setText(value.toString());

        setForeground(Color.black);
        Font f = new Font("SansSerif", 0, 12); // this.getFont();

        //System.out.println(value);
        // Set tool tip if desired

        // Now the funny business....
        if (colIndex == NUMBER_COL) {
            setToolTipText(new String("Line Number"));
        } else if (colIndex == TEXT_COL) {
            setToolTipText(theWrangler.getStatementToolTipText(rowIndex));
        }
        setFont(f);
        return this;
    }

}


