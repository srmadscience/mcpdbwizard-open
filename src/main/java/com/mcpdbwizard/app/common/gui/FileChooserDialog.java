/**
 * @version 2
 * @author devteam@mcpdbwizard.com
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 **/
package com.mcpdbwizard.app.common.gui;

import com.mcpdbwizard.pub.Namer;

import java.awt.*;
//import java.awt.event.*;
import javax.swing.*;
//import java.io.*;

public class FileChooserDialog extends JDialog {
    JPanel panel1 = new JPanel();
    BorderLayout borderLayout1 = new BorderLayout();
    JFileChooser jFileChooser1 = new JFileChooser();
    String[] supportedFileTypes = {".pb2"};

    public FileChooserDialog(Frame frame, String title, boolean modal) {
        super(frame, title, modal);

        try {
            jFileChooser1.setMultiSelectionEnabled(false);
            //jFileChooser1.addChoosableFileFilter(new SimpleFileFilter(supportedFileTypes,Namer.param_product_name + " " + Namer.param_version + " Files"));
            jbInit();
            pack();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public FileChooserDialog() {
        this(null, "", false);
    }

    void jbInit() throws Exception {
        panel1.setLayout(borderLayout1);
        getContentPane().add(panel1);
        panel1.add(jFileChooser1, BorderLayout.CENTER);
    }
}




