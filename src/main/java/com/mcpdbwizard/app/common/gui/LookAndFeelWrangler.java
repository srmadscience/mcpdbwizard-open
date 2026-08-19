package com.mcpdbwizard.app.common.gui;

import javax.swing.*;
import java.awt.event.*;

/**
 * @author devteam@mcpdbwizard.com
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 * @version 2
 */
public class LookAndFeelWrangler {

    public static JMenu createSwitchLFMenu(String label, JFrame f, boolean verify) {
        // Create a menu entry for each L&F we manage to load
        LookAndFeel lf;
        JMenu menu = new JMenu(label);
        int menuCount = 0;
        UIManager.LookAndFeelInfo[] lfInfo = UIManager.getInstalledLookAndFeels();

        for (int i = 0; i < lfInfo.length; i++) {
            try {
                String className = lfInfo[i].getClassName();
                if (verify) {
                    Class cl = Class.forName(className);
                    lf = (LookAndFeel) cl.newInstance();
                    if (!lf.isSupportedLookAndFeel()) {
                        className = null;
                    }
                }

                if (className != null) {
                    LFSwitchAction switchAction = new LFSwitchAction(lfInfo[i], f);
                    JMenuItem mi = menu.add(switchAction);
                    mi.setToolTipText(lfInfo[i].getName());
                    menuCount++;
                }
            } catch (Throwable t) {
                // Ignore all exceptions - don't add to menu
                System.out.println("Failed loading " + lfInfo[i].getClassName() + ":\n" + t);
            }
        }

        return menuCount == 0 ? null : menu;
    }

    // Action handler for switching L&F
    protected static class LFSwitchAction extends AbstractAction {
        protected UIManager.LookAndFeelInfo lfi;
        protected JFrame f;

        public LFSwitchAction(UIManager.LookAndFeelInfo lfi, JFrame f) {
            super(lfi.getName());
            this.lfi = lfi;
            this.f = f;
        }

        public void actionPerformed(ActionEvent evt) {
            LookAndFeel oldLF = UIManager.getLookAndFeel();
            try {
                UIManager.setLookAndFeel(lfi.getClassName());

                // Switch all component UI's
                SwingUtilities.updateComponentTreeUI(f);
                f.invalidate();
                f.validate();
                f.repaint();
            } catch (Throwable t) {
                // Ignore all exceptions
                System.out.println("Failed to install " + lfi.getName() + " L&F\n" + t);
                try {
                    UIManager.setLookAndFeel(oldLF);
                } catch (Throwable e) {
                    System.out.println("Failed to restore old L&F");
                    System.exit(0);
                }
            }
        }
    }
}



