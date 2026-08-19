package com.mcpdbwizard.app.procbuilder.gui;

import java.awt.*;
//import com.borland.jbcl.layout.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.JComboBox;
import java.awt.event.*;

import com.mcpdbwizard.app.common.TableList;
import com.mcpdbwizard.app.procbuilder.SqlStatementWrangler;

/**
 * A Java Utility that generates Java source for automating calls to Oracle Stored Procedures and Functions
 *
 * @author David Rolfe
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 * Portions Copyright (c) 1999 CodeSpooks
 */
public class AspFilePanel extends JPanel {
    static final int DEFAULT_SCREEN_WIDTH = 800;
    static final int SCROLLPANE_HEIGHT = 300;


    JCheckBox createJavaClassCheckBox = new JCheckBox();

    JComboBox theJComboBox = new JComboBox(com.mcpdbwizard.app.procbuilder.SqlStatementWrangler.ASP_DATA_TYPES);
    JTable aspFilePanelParamTable = new JTable(1, 2);
    JScrollPane aspFilePanelParamTableScrollPane = new JScrollPane();
    JScrollPane jScrollPane2 = new JScrollPane();

    com.mcpdbwizard.app.procbuilder.SqlStatementWrangler theSqlStatementWrangler = null;
    JTable sqlStatementTable = new JTable(10, 20);
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JLabel jLabel1 = new JLabel();
    Font aFont = new Font("Dialog", 1, 11);
    Font aNonBoldFont = new Font("Dialog", 0, 11);
    JCheckBox createRecordsCheckBox = new JCheckBox();

    public AspFilePanel() {
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        this.setLayout(gridBagLayout1);
        createJavaClassCheckBox.setToolTipText("If ticked a java class to execute this statement will be created");
        createJavaClassCheckBox.setPreferredSize(new Dimension(800, 25));
        createJavaClassCheckBox.setText("Create Java class for this statement");
        createJavaClassCheckBox.setFont(new java.awt.Font("Dialog", 1, 11));
        createJavaClassCheckBox.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                createJavaClassCheckBox_actionPerformed(e);
            }
        });
        this.setMaximumSize(new Dimension(1000, 600));
        this.setMinimumSize(new Dimension(300, 350));
        this.setPreferredSize(new Dimension(400, 300));
        aspFilePanelParamTable.setFont(new java.awt.Font("Dialog", 0, 11));
        aspFilePanelParamTable.setAutoscrolls(false);
        aspFilePanelParamTable.setBorder(BorderFactory.createLoweredBevelBorder());
        aspFilePanelParamTable.setMaximumSize(new Dimension(485, 32767));
        aspFilePanelParamTable.setMinimumSize(new Dimension(485, 500));
        aspFilePanelParamTable.setPreferredSize(new Dimension(485, 500));
        aspFilePanelParamTable.setToolTipText("Use this Table to name parameters and define their data types");
        aspFilePanelParamTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        jScrollPane2.setAutoscrolls(true);
        jScrollPane2.setDoubleBuffered(true);
        jScrollPane2.setMinimumSize(new Dimension(800, 600));
        jScrollPane2.setPreferredSize(new Dimension(800, 600));
        aspFilePanelParamTableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        sqlStatementTable.setFont(new java.awt.Font("Dialog", 0, 11));
        sqlStatementTable.setBorder(BorderFactory.createLoweredBevelBorder());
        sqlStatementTable.setMaximumSize(new Dimension(485, 32767));
        sqlStatementTable.setMinimumSize(new Dimension(300, 80));
        sqlStatementTable.setPreferredSize(new Dimension(485, 100));
        sqlStatementTable.setToolTipText("This shows the SQL statement you wish to generate code for.");

        jLabel1.setText(" ");
        createRecordsCheckBox.setToolTipText("If ticked try and return an array of custom records.");
        createRecordsCheckBox.setText("Turn queries into records where possible");
        createRecordsCheckBox.setFont(new java.awt.Font("Dialog", 1, 11));
        createRecordsCheckBox.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                createRecordsCheckBox_actionPerformed(e);
            }
        });
        this.add(aspFilePanelParamTableScrollPane, new GridBagConstraints(0, 2, 3, 1, 1.0, 1.0
                , GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 12, 31), 47, 137));
        aspFilePanelParamTableScrollPane.getViewport().add(aspFilePanelParamTable, null);
        this.add(jScrollPane2, new GridBagConstraints(0, 3, 3, 1, 1.0, 1.0
                , GridBagConstraints.NORTHEAST, GridBagConstraints.HORIZONTAL, new Insets(2, 9, 0, 30), -15, -404));
        this.add(jLabel1, new GridBagConstraints(0, 4, 3, 1, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 30));
        this.add(createJavaClassCheckBox, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
                , GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(4, 11, 3, 0), 0, 0));
        this.add(createRecordsCheckBox, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
                , GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, new Insets(4, 11, 3, 0), 0, 0));
        jScrollPane2.getViewport().add(sqlStatementTable, null);

    }

    public void setFileName(String newName) {
        //createJavaClassCheckBox.setText("Generate matching Java class for '" + newName + "'");
    }

    public boolean getCreateJavaClass() {
        return (createJavaClassCheckBox.isSelected());
    }

    public void setCreateJavaClass(boolean aValue) {
        createJavaClassCheckBox.setSelected(aValue);
    }

    public void setCreateJavaClass(int fileType) {
        if (fileType == com.mcpdbwizard.app.procbuilder.SqlStatementWrangler.BAD_FILE
                || fileType == com.mcpdbwizard.app.procbuilder.SqlStatementWrangler.GOOD_FILE_BAD_SQL_STATEMENT) {
            // Disable everything
            createJavaClassCheckBox.setEnabled(false);
        } else if (fileType == com.mcpdbwizard.app.procbuilder.SqlStatementWrangler.VALID_SQL_STATEMENT) {
            createJavaClassCheckBox.setEnabled(true);
            createJavaClassCheckBox.setSelected(false);
        } else if (fileType == com.mcpdbwizard.app.procbuilder.SqlStatementWrangler.SELECTED_SQL_STATEMENT) {
            createJavaClassCheckBox.setEnabled(true);
            createJavaClassCheckBox.setSelected(true);
        }
    }

    public JCheckBox getCreateJavaClassCheckBox() {
        return (createJavaClassCheckBox);
    }

    public boolean getCreateRecords() {
        return (createRecordsCheckBox.isSelected());
    }

    public void setCreateRecords(boolean aValue) {
        createRecordsCheckBox.setSelected(aValue);
    }

    public JCheckBox getRecordsCheckBox() {
        return (createRecordsCheckBox);
    }

    public void setTableModel(SqlStatementWrangler theWrangler) {
        int screenHeight = SCROLLPANE_HEIGHT;

        TableModel newTableModel = (TableModel) theWrangler;

        if (aspFilePanelParamTable.getEditingRow() > newTableModel.getRowCount()) {
            aspFilePanelParamTable.setEditingRow(newTableModel.getRowCount() - 1);
        }

        if ((((aspFilePanelParamTable.getRowHeight() + 1) * (1 + newTableModel.getRowCount())) + 10) > screenHeight) {
            screenHeight = 10 + ((aspFilePanelParamTable.getRowHeight() + 1) * (1 + newTableModel.getRowCount()));
        }
        //System.out.println(screenHeight);

        Dimension xYSize = new Dimension(aspFilePanelParamTable.getWidth(), screenHeight);
        aspFilePanelParamTable.setPreferredSize(xYSize);
        aspFilePanelParamTable.setModel(newTableModel);
        aspFilePanelParamTable.setEditingRow(0);
        sqlStatementTable.setEditingRow(0);

        sqlStatementTable.setDefaultRenderer(SqlListCellRenderer.class, new SqlListCellRenderer(theWrangler));
        aspFilePanelParamTable.setDefaultRenderer(SqlParameterCellRenderer.class, new SqlParameterCellRenderer(theWrangler));

        // Text only seen when data not present
        if (theWrangler.getRowCount() == 0) {
            aspFilePanelParamTable.setToolTipText("This SQL statement has no parameters");
        } else {
            aspFilePanelParamTable.setToolTipText("Parameters for this SQL statement");
        }

        aspFilePanelParamTable.updateUI();
        aspFilePanelParamTable.validate();
        //aspFilePanelParamTable.setPreferredSize(xYSize);
        JTableHeader newHeader = aspFilePanelParamTable.getTableHeader();
        newHeader.setFont(aNonBoldFont);

        TableColumnModel newColumnModel = aspFilePanelParamTable.getColumnModel();

        for (int i = 0; i < newColumnModel.getColumnCount(); i++) {
            TableColumn tempColumn = newColumnModel.getColumn(i);

            if (((String) tempColumn.getHeaderValue()).equals("Line #")) {
                tempColumn.setMinWidth(40);
                tempColumn.setPreferredWidth(40);
                tempColumn.setMaxWidth(40);
            } else if (((String) tempColumn.getHeaderValue()).equals("Parameter Name")) {
                tempColumn.setMinWidth(350);
                tempColumn.setPreferredWidth(500);
                tempColumn.setMaxWidth(500);
            } else if (((String) tempColumn.getHeaderValue()).equals("Data Type")) {

                tempColumn.setMinWidth(150);
                tempColumn.setPreferredWidth(150);
                tempColumn.setMaxWidth(150);
                theJComboBox.setFont(aNonBoldFont);
                tempColumn.setCellEditor(new DefaultCellEditor(theJComboBox));
            }

        }

        newHeader.validate();
        newHeader.setReorderingAllowed(false);

    }


    public void setFileText(String[] newText) {
        int screenHeight = SCROLLPANE_HEIGHT;
        //int charwidth = 7;
        int screenWidth = 850;
        int maxStringLength = 10;

        for (int i = 0; i < newText.length; i++) {
            if (newText[i].length() > maxStringLength) {
                maxStringLength = newText[i].length();
            }
        }

        TableList theTableList = new TableList(newText);
        sqlStatementTable.setAutoscrolls(true);
        sqlStatementTable.setModel(theTableList);
        if ((((sqlStatementTable.getRowHeight() + 1) * (1 + theTableList.getRowCount())) + 10) > screenHeight) {
            screenHeight = 10 + ((sqlStatementTable.getRowHeight() + 1) * (1 + theTableList.getRowCount()));
        }

        Dimension xYSize = new Dimension(sqlStatementTable.getWidth(), screenHeight);

        sqlStatementTable.setPreferredSize(xYSize);
        sqlStatementTable.setMinimumSize(xYSize);
        sqlStatementTable.setModel(theTableList);

        JTableHeader newHeader = sqlStatementTable.getTableHeader();
        newHeader.setFont(aNonBoldFont);

        TableColumnModel newColumnModel = sqlStatementTable.getColumnModel();

        for (int i = 0; i < newColumnModel.getColumnCount(); i++) {
            TableColumn tempColumn = newColumnModel.getColumn(i);

            if (((String) tempColumn.getHeaderValue()).equals("Line #")) {
                tempColumn.setMinWidth(40);
                tempColumn.setPreferredWidth(40);
                tempColumn.setMaxWidth(40);
            } else {
                tempColumn.setMinWidth(490);
                tempColumn.setPreferredWidth(screenWidth);
                tempColumn.setMaxWidth(screenWidth);
            }
        }

        newHeader.validate();
        newHeader.setReorderingAllowed(false);

    }

    void createJavaClassCheckBox_actionPerformed(ActionEvent e) {
        theSqlStatementWrangler.setCreateJava(createJavaClassCheckBox.isSelected());
    }

    public void setSqlStatementWrangler(com.mcpdbwizard.app.procbuilder.SqlStatementWrangler wrangler) {
        theSqlStatementWrangler = wrangler;
    }

    void createRecordsCheckBox_actionPerformed(ActionEvent e) {
        theSqlStatementWrangler.setCreateRecords(createRecordsCheckBox.isSelected());
    }

    public void setCreateRecordsEnabled(boolean isEnabled) {
        createRecordsCheckBox.setEnabled(isEnabled);
    }
}


