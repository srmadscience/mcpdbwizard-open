package com.mcpdbwizard.app.procbuilder.gui;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

/**
 * A Java Utility that generates Java source for automating calls to Oracle Stored Procedures and Functions
 *
 * @author David Rolfe
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 * Portions Copyright (c) 1999 MCPDBWizard
 */
public class DbInfoPanel extends JPanel {
    BorderLayout borderLayout1 = new BorderLayout();
    JLabel passwordLabel = new JLabel();
    JLabel hostLabel = new JLabel();
    JLabel SIDLabel = new JLabel();
    JTextField sidField = new JTextField();
    JPasswordField oraPassField = new JPasswordField();
    JTextField portField = new JTextField();
    JPanel dbPanel = new JPanel();
    JLabel userLabel = new JLabel();
    JTextField oraUserField = new JTextField();
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JTextField hostnameField = new JTextField();
    JLabel portLabel = new JLabel();

    public DbInfoPanel() {
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        dbPanel.setLayout(gridBagLayout1);
        portField.setToolTipText("Sql*Net Port. Usually 1521.");
        oraPassField.setToolTipText("Password For SA Database Owner");
        oraPassField.setText("JSCHED");
        sidField.setToolTipText("Oracle Instance Name");
        SIDLabel.setText("Oracle SID");
        hostLabel.setText("TCP/IP Hostname");
        passwordLabel.setText("Oracle Password");
        this.setLayout(borderLayout1);
        userLabel.setText("Oracle User");
        oraUserField.setToolTipText("SA Database Owner");
        hostnameField.setToolTipText("Hostname of the Server with the Oracle Database");
        portLabel.setText("Sql*Net Port");
        this.add(dbPanel, BorderLayout.CENTER);
        dbPanel.add(hostLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(44, 107, 0, 0), 22, 0));
        dbPanel.add(SIDLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 107, 0, 0), 62, 0));
        dbPanel.add(sidField, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 25, 0, 108), 98, 0));
        dbPanel.add(portLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 107, 0, 0), 54, 0));
        dbPanel.add(portField, new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 25, 0, 108), 104, 0));
        dbPanel.add(oraUserField, new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 25, 0, 108), 85, 0));
        dbPanel.add(passwordLabel, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 107, 0, 0), 26, 0));
        dbPanel.add(hostnameField, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(44, 25, 0, 108), 67, 0));
        dbPanel.add(oraPassField, new GridBagConstraints(1, 4, 1, 1, 1.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 25, 0, 108), 83, 0));
        dbPanel.add(userLabel, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 107, 0, 0), 55, 0));
    }

    void connectButton_actionPerformed(ActionEvent e) {

    }
}    



