package com.mcpdbwizard.app.procbuilder.gui;

import com.mcpdbwizard.pub.Namer;

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
public class AspToplevelPanel extends JPanel {
    JLabel jLabel1 = new JLabel();
    JLabel jLabel2 = new JLabel();
    JTextField readableFiles = new JTextField();
    JTextField unreadableFiles = new JTextField();
    JTextField validSQLFiles = new JTextField();
    JTextField invalidSQLFiles = new JTextField();
    JTextField selectedSQLFiles = new JTextField();
    JTextField unselectedSQLFiles = new JTextField();
    JLabel jLabel3 = new JLabel();
    JLabel jLabel4 = new JLabel();
    JLabel jLabel5 = new JLabel();
    JLabel jLabel6 = new JLabel();
    GridBagLayout gridBagLayout1 = new GridBagLayout();

    public AspToplevelPanel() {
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        jLabel1.setFont(new java.awt.Font("Dialog", 1, 11));
        jLabel1.setText("Readable Files");
        this.setLayout(gridBagLayout1);
        jLabel2.setFont(new java.awt.Font("Dialog", 1, 11));
        jLabel2.setText("Unusable SQL Files");
        readableFiles.setFont(new java.awt.Font("Dialog", 0, 11));
        readableFiles.setToolTipText("The number of readable .sql files in this directory");
        readableFiles.setText("0");
        unreadableFiles.setText("0");
        unreadableFiles.setToolTipText("The number of unreadable .sql files in this directory - includes " +
                "directories and zero-length files");
        validSQLFiles.setFont(new java.awt.Font("Dialog", 0, 11));
        validSQLFiles.setToolTipText("The number of usable .sql files in this directory that " + Namer.param_prod_name + " " +
                "thinks are usable");
        validSQLFiles.setText("0");
        invalidSQLFiles.setFont(new java.awt.Font("Dialog", 0, 11));
        invalidSQLFiles.setToolTipText("The number of usable .sql files in this directory that " + Namer.param_prod_name + " " +
                "thinks are unusable");
        invalidSQLFiles.setText("0");
        selectedSQLFiles.setFont(new java.awt.Font("Dialog", 0, 11));
        selectedSQLFiles.setToolTipText("The number of selected .sql files in this directory");
        selectedSQLFiles.setText("0");
        unselectedSQLFiles.setFont(new java.awt.Font("Dialog", 0, 11));
        unselectedSQLFiles.setToolTipText("The number of unselected .sql files in this directory");
        unselectedSQLFiles.setText("0");
        jLabel3.setFont(new java.awt.Font("Dialog", 1, 11));
        jLabel3.setText("Unreadable Files");
        jLabel4.setFont(new java.awt.Font("Dialog", 1, 11));
        jLabel4.setText("Usable SQL Files");
        jLabel5.setFont(new java.awt.Font("Dialog", 1, 11));
        jLabel5.setText("Selected SQL Files");
        jLabel6.setFont(new java.awt.Font("Dialog", 1, 11));
        jLabel6.setText("Unselected SQL Files");
        this.add(unreadableFiles, new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 25, 0, 49), 59, 8));
        this.add(selectedSQLFiles, new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 46, 145, 0), 59, 8));
        this.add(jLabel1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 13, 0, 0), 45, 13));
        this.add(jLabel4, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(12, 13, 0, 0), 32, 13));
        this.add(jLabel5, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 13, 145, 0), 24, 13));
        this.add(unselectedSQLFiles, new GridBagConstraints(3, 2, 1, 1, 1.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 25, 145, 49), 59, 8));
        this.add(invalidSQLFiles, new GridBagConstraints(3, 1, 1, 1, 1.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(12, 25, 0, 49), 59, 8));
        this.add(readableFiles, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 46, 0, 0), 59, 8));
        this.add(jLabel6, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 23, 145, 0), 9, 13));
        this.add(jLabel2, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(12, 23, 0, 0), 18, 13));
        this.add(jLabel3, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 23, 0, 0), 34, 13));
        this.add(validSQLFiles, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
                , GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL, new Insets(12, 46, 0, 0), 59, 8));
    }

    public void setFileCounts(int readable
            , int unreadable
            , int validSQL
            , int invalidSQL
            , int selectedSQL
            , int unselectedSQL) {
        readableFiles.setText(readable + "");
        unreadableFiles.setText(unreadable + "");
        validSQLFiles.setText(validSQL + "");
        invalidSQLFiles.setText(invalidSQL + "");
        selectedSQLFiles.setText(selectedSQL + "");
        unselectedSQLFiles.setText(unselectedSQL + "");
    }

    public void setSelectedFileCounts(
            int selectedSQL
            , int unselectedSQL) {
        selectedSQLFiles.setText(selectedSQL + "");
        unselectedSQLFiles.setText(unselectedSQL + "");
    }
}


