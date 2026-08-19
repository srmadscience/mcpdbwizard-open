package com.mcpdbwizard.app.procbuilder.gui;

import java.awt.*;
//import com.borland.jbcl.layout.*;
import javax.swing.*;

/**
 * A Java Utility that generates Java source for automating calls to Oracle Stored Procedures and Functions
 *
 * @author David Rolfe
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 * Portions Copyright (c) 1999 CodeSpooks
 */
public class AspBrokenPanel extends JPanel {
    JLabel brokenReason = new JLabel();
    JTextArea brokenText = new JTextArea();
    JScrollPane jScrollPane1 = new JScrollPane();
    GridBagLayout gridBagLayout1 = new GridBagLayout();

    public AspBrokenPanel() {
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        this.setLayout(gridBagLayout1);
        brokenText.setLineWrap(true);
        brokenText.setBorder(BorderFactory.createLoweredBevelBorder());
        brokenText.setEditable(false);
        brokenText.setFont(new java.awt.Font("Dialog", 1, 11));
        this.add(brokenReason, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(8, 11, 0, 54), 489, 33));
        this.add(jScrollPane1, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
                , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(14, 11, 8, 26), 0, 177));
        jScrollPane1.getViewport().add(brokenText, null);
    }

    public void setMessage(String theTitle, String theMessage) {
        brokenReason.setText(theTitle);
        brokenText.setText(theMessage);
    }
}


