package com.mcpdbwizard.app.procbuilder.gui;

import com.mcpdbwizard.pub.Namer;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*; //import com.borland.jbcl.layout.*;
//import com.mcpdbwizard.pub.LogInterface;
import com.mcpdbwizard.app.common.gui.*;
import com.mcpdbwizard.app.common.*;
import com.mcpdbwizard.app.procbuilder.*;
import com.mcpdbwizard.app.templates.TemplateWrangler;
//import com.mcpdbwizard.app.procbuilder.gui.*;
//import javax.swing.filechooser.*;
//import javax.swing.plaf.FileChooserUI;
//import javax.swing.table.*;

import javax.swing.border.*;

/**
 * @author devteam@mcpdbwizard.com Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 * @version 3
 */
public class ThingAdministratorFrame extends JFrame {

    public static final String FILE_EXTENSION = ".pb2";
    public static final int LOGIN_MODE = 1;
    static final boolean OB4 = true;
    static final String CONNECT_BUTTON_TEXT = "Connect...";
    static final String DISCONNECT_BUTTON_TEXT = "Disconnect...";
    // Possible operation modes...
    static final int PRE_LOGIN = 0;
    static final int LOGGED_IN = 2;
    private static final String SELECT_GENERIC_TEMPLATE = " Select a generic template...";
    JFileChooser chooser;
    JFileChooser dirChooser;
    // Initial operation mode.
    int currentMode = PRE_LOGIN;

    String[] sequenceHeadings = {"User", "Sequence Name", "Recent Value",
            "Select"};
    String[][] sequenceValues = new String[400][4];

    boolean labelsHaveDigits = true;

    JComboBox javaAccessComboBox = new JComboBox(sequenceHeadings);

    Dimension xYSize = new Dimension(800, 600);
    ApplicationShell mrApplicationShell;
    JMenuBar menuBar1 = new JMenuBar();
    JMenu menuFile = new JMenu();
    JMenuItem menuFileNew = new JMenuItem();
    JMenuItem menuFileOpen = new JMenuItem();
    JMenuItem menuFileSave = new JMenuItem();
    JMenuItem menuFileSaveAs = new JMenuItem();
    JMenuItem menuFileExit = new JMenuItem();
    JMenu menuHelp = new JMenu();
    JMenuItem menuHelpAbout = new JMenuItem();
    JLabel adminStatusBar = new JLabel();
    BorderLayout adminLayout = new BorderLayout();
    JPanel loginPanel = new JPanel();
    JLabel hostLabel = new JLabel();
    JTextField hostnameField = new JTextField();
    JLabel SIDLabel = new JLabel();
    JTextField sidField = new JTextField();

    JLabel portLabel = new JLabel();
    JTextField portField = new JTextField();
    JLabel userLabel = new JLabel();
    JTextField oraUserField = new JTextField();
    JLabel passwordLabel = new JLabel();
    JPasswordField oraPassField = new JPasswordField();
    JTextArea connectionStatusField = new JTextArea();
    JButton connectButton = new JButton();
    GridBagLayout gridBagLayout1 = new GridBagLayout();

    JMenu switchMenu = LookAndFeelWrangler.createSwitchLFMenu("Skin", this,
            false);

    FlowLayout flowLayout1 = new FlowLayout();
    JTabbedPane adminTabbedPane = new JTabbedPane();
    JPanel selectObjectsPanel = new JPanel();
    BorderLayout borderLayout1 = new BorderLayout();
    JPanel selectObjectsButtonPanel = new JPanel();

    JPanel codeOptionsPanel = new JPanel();
    JPanel postOptionsPanel = new JPanel();
    //JTextArea authorVersionTextArea2 = new JTextArea();
    JLabel authorLabel = new JLabel();
    JTextField authorTextField = new JTextField();
    JLabel versionLabel = new JLabel();
    JTextField versionTextField = new JTextField();
    JTextArea buildOptionsTextArea = new JTextArea();
    JTextArea commentTextArea = new JTextArea();
    JLabel commentLabel1 = new JLabel();
    TitledBorder titledBorder1;
    JTextArea authorVersionTextArea4 = new JTextArea();

    JTable sequenceTable = new JTable(sequenceValues, sequenceHeadings);
    JCheckBox otherCheckBox = new JCheckBox();
    JPanel optionPanel = new JPanel();
    BorderLayout borderLayout2 = new BorderLayout();
    JTabbedPane selectObjectsTabbedPane = new JTabbedPane();
    JLabel userOrAllLabel = new JLabel();
    JTextField otherUserName = new JTextField();
    JCheckBox meCheckBox = new JCheckBox();
    JPanel sequencePanel = new JPanel();
    JPanel sequenceTablePanel = new JPanel();
    JButton objectRefreshButton = new JButton();
    JScrollPane sequenceTableScrollPane = new JScrollPane();
    JFileChooser saveAsFileChooser = new JFileChooser();
    JButton selectAllSequencesButton = new JButton();
    JButton selectNoSequencesButton = new JButton();
    JComboBox oracleVersionComboBox = new JComboBox();
    JCheckBox debugMessagesCheckbox = new JCheckBox();
    JCheckBox otherMessagesCheckbox = new JCheckBox();
    JCheckBox commentsCheckBox = new JCheckBox();
    JCheckBox statsCheckBox1 = new JCheckBox();
    JComboBox javaNamingConventionComboBox = new JComboBox();
    JPanel jPanel2 = new JPanel();
    BorderLayout borderLayout3 = new BorderLayout();
    JPanel outputPanel = new JPanel();
    BorderLayout borderLayout4 = new BorderLayout();
    JPanel outputLogPanel = new JPanel();
    JPanel outputButtonPanel = new JPanel();
    JButton generateCodeButton = new JButton();
    BorderLayout borderLayout5 = new BorderLayout();
    JTextArea guiLogTextArea = new JTextArea();
    FlowLayout flowLayout2 = new FlowLayout();
    JScrollPane outputAreaScrollPane = new JScrollPane();
    JButton selectAllFunctionsButton = new JButton();
    JTable functionTable = new JTable(sequenceValues, sequenceHeadings);
    FlowLayout flowLayout3 = new FlowLayout();
    JScrollPane functionTableScrollPane = new JScrollPane();
    JPanel functionPanel = new JPanel();
    BorderLayout borderLayout6 = new BorderLayout();
    JButton selectNoFunctionsButton1 = new JButton();
    JPanel jPanel3 = new JPanel();
    JPanel jPanel4 = new JPanel();
    JButton selectAllTablesButton = new JButton();
    JTable tableTable = new JTable(sequenceValues, sequenceHeadings);
    JPanel tableTablePanel = new JPanel();
    FlowLayout flowLayout4 = new FlowLayout();
    JScrollPane tableTableScrollPane = new JScrollPane();
    JPanel tablePanel = new JPanel();
    BorderLayout borderLayout7 = new BorderLayout();
    JButton selectNoTablesButton = new JButton();
    JPanel jPanel5 = new JPanel();
    JButton selectAllSqlButton = new JButton();
    JPanel sqlTablePanel = new JPanel();
    JButton selectNoSqlButton = new JButton();
    JScrollPane sqlTableScrollPane = new JScrollPane();
    BorderLayout borderLayout8 = new BorderLayout();
    JPanel sqlPanel = new JPanel();
    JPanel sqlTableHeaderPanel = new JPanel();
    JSplitPane sqlTableSpiltPane = new JSplitPane();
    JPanel sqlTableEditPanel = new JPanel();
    JTextField aspRootDirTextField = new JTextField();
    JTextArea aspFileDirectorytextArea = new JTextArea();
    JButton aspDirFileChooserButton = new JButton();
    JTextArea aspFileDirectorytextArea1 = new JTextArea();
    JScrollPane jScrollPane2 = new JScrollPane();
    JScrollPane jScrollPane1 = new JScrollPane();
    JScrollPane jScrollPane3 = new JScrollPane();
    JScrollPane sqlTableTreeScrollPane = new JScrollPane();
    JTree sqlTableTree = new JTree();
    // JMenuItem menuCache = new JMenuItem();

    AspToplevelPanel theAspToplevelPanel = new AspToplevelPanel();
    AspFilePanel theAspFilePanel = new AspFilePanel();
    AspBrokenPanel theBrokenPanel = new AspBrokenPanel();
    JLabel jLabel1 = new JLabel();
    JLabel jLabel2 = new JLabel();
    JLabel jLabel3 = new JLabel();
    JLabel jLabel4 = new JLabel();
    JLabel jLabel5 = new JLabel();
    JLabel jLabel6 = new JLabel();
    JCheckBox useShortCB = new JCheckBox();
    JCheckBox useIntCB = new JCheckBox();
    JCheckBox useByteCB = new JCheckBox();
    JCheckBox useLongCB = new JCheckBox();
    JCheckBox useFloatCB = new JCheckBox();
    JCheckBox useDoubleCB = new JCheckBox();

    JCheckBox useShortOCB = new JCheckBox();
    JCheckBox useIntOCB = new JCheckBox();
    JCheckBox useByteOCB = new JCheckBox();
    JCheckBox useLongOCB = new JCheckBox();
    JCheckBox useFloatOCB = new JCheckBox();
    JCheckBox useDoubleOCB = new JCheckBox();
    JLabel jLabel7 = new JLabel();
    JCheckBox validateCB = new JCheckBox();
    GridBagLayout gridBagLayout2 = new GridBagLayout();
    GridBagLayout gridBagLayout3 = new GridBagLayout();
    GridBagLayout gridBagLayout4 = new GridBagLayout();
    GridBagLayout gridBagLayout5 = new GridBagLayout();
    GridBagLayout gridBagLayout6 = new GridBagLayout();
    GridBagLayout gridBagLayout9 = new GridBagLayout();
    GridBagLayout gridBagLayout10 = new GridBagLayout();
    Border border1;
    java.awt.Cursor defaultCursor = null;
    JCheckBox extraSqlCheckBox = new JCheckBox();
    JTextField codeRootDirTextField = new JTextField();
    JTextArea packageNameTextArea = new JTextArea();
    JPanel fileOptionsPanel = new JPanel();

    // ---- "5. Pooling": DAO factory pooling. Its own tab rather than more controls on the Service
    // Options page, because the sizes here have operational consequences on the database server
    // (each pooled factory pins a session and its DAOs' cursors) and that needs room to be said.
    JPanel poolOptionsPanel = new JPanel();
    GridBagLayout gridBagLayoutPool = new GridBagLayout();
    JCheckBox daoPoolCheckBox = new JCheckBox();
    JLabel daoPoolMaxSizeLabel = new JLabel();
    JTextField daoPoolMaxSizeTextField = new JTextField();
    JLabel daoPoolMinIdleLabel = new JLabel();
    JTextField daoPoolMinIdleTextField = new JTextField();
    JLabel daoPoolMaxWaitLabel = new JLabel();
    JTextField daoPoolMaxWaitTextField = new JTextField();
    JLabel daoPoolIdleTimeoutLabel = new JLabel();
    JTextField daoPoolIdleTimeoutTextField = new JTextField();
    JLabel daoPoolOnReturnLabel = new JLabel();
    JComboBox daoPoolOnReturnComboBox = new JComboBox();
    JTextArea daoPoolBlurbTextArea = new JTextArea();

    JTextField packageNameTextField1 = new JTextField();
    JTextArea directoriesMessedWithtextArea1 = new JTextArea();
    JButton codeRootFileChooserButton = new JButton();
    GridBagLayout gridBagLayout8 = new GridBagLayout();
    JPanel fileOptionsPanelExtraPanel = new JPanel();
    JTextArea directoriesMessedWithtextArea2 = new JTextArea();
    JLabel daoFactoryClassNameLabel = new JLabel();
    JTextField daoFactoryNameTextField = new JTextField();
    JCheckBox generateSessionBeanCheckBox = new JCheckBox();
    JLabel logTypeLabel = new JLabel();
    JTextField logNameTextField1 = new JTextField();
    JLabel logNameLabel = new JLabel();
    JComboBox logTypeComboBox = new JComboBox();
    JLabel connectionTypeLabel = new JLabel();
    JComboBox connectionTypeComboBox = new JComboBox();
    JLabel connectionNameLabel = new JLabel();
    JTextField connectionNameTextField = new JTextField();
    GridBagLayout gridBagLayout7 = new GridBagLayout();
    GridBagLayout gridBagLayout11 = new GridBagLayout();
    JLabel jLabel8 = new JLabel();
    // JCheckBox webServicesCB = new JCheckBox();
    JCheckBox addFinalizeMethodCheckBox = new JCheckBox();
    JLabel jLabel10 = new JLabel();
    JTextField tempDirTextField = new JTextField();
    JLabel jLabel11 = new JLabel();
    JTextField tempFilePrefixTextField = new JTextField();
    JLabel jLabel12 = new JLabel();
    JTextField tempFileSuffixTextField = new JTextField();
    JCheckBox createWSCheckBox = new JCheckBox();
    JCheckBox mcpServerCheckBox = new JCheckBox();
    JTextArea jTextArea1 = new JTextArea();
    JLabel jLabel9 = new JLabel();
    JLabel jLabel13 = new JLabel();
    JLabel jLabel13B = new JLabel();
    JTextField wsInterfaceClassTextField = new JTextField();
    JTextField wsImplClassTextField = new JTextField();
    JCheckBox wsPreCallCheckBox = new JCheckBox();
    JCheckBox wsPostCallCheckBox = new JCheckBox();
    JCheckBox wsAlwaysReleaseCheckBox = new JCheckBox();
    JCheckBox closeConnCheckBox = new JCheckBox();
    JCheckBox commitConnCheckBox = new JCheckBox();
    JComboBox javaNumberTypeComboBox = new JComboBox();
    JComboBox wsRecTypeComboBox = new JComboBox();
    JLabel jLabel14 = new JLabel();
    JLabel jLabel15 = new JLabel();
    JLabel jLabel16 = new JLabel();
    JLabel xwsLabel = new JLabel();
    JLabel xwsImplLabel = new JLabel();
    JLabel xwsIfaceLabel = new JLabel();

    JLabel jvmLabel = new JLabel();
    JComboBox jvmComboBox = new JComboBox();
    JLabel plsqlLabel = new JLabel();
    JLabel sqlLabel = new JLabel();
    JTextField methodPlsqlTextField = new JTextField();
    JTextField methodSqlTextField = new JTextField();
    GridBagLayout gridBagLayout12 = new GridBagLayout();
    JTextArea postScriptDescrTextArea = new JTextArea();
    JLabel jLabel17 = new JLabel();
    JTextField postScriptNameTextField = new JTextField();
    JTextArea postScriptCodeTextArea = new JTextArea();
    JLabel jLabel18 = new JLabel();
    //JTextField postDescrTextArea = new JTextField();
    Border border2;
    JTextArea extraCodeTextArea = new JTextArea();
    JLabel jLabel19 = new JLabel();
    JTextArea postDescrTextArea = new JTextArea();
    JScrollPane postScriptScrollPane = new JScrollPane();
    JScrollPane jScrollPane4 = new JScrollPane();
    JLabel jLabel20 = new JLabel();
    //JButton genericTemplateButton = new JButton();

    String[] xwsTypeNameList = {"NONE", "JSON", "JSON-RPC"};
    JComboBox xwsTypeName = new JComboBox(xwsTypeNameList);
    JTextField xwsImpl = new JTextField();
    JTextField xwsIface = new JTextField();


    String[] dummyTemplateList = {SELECT_GENERIC_TEMPLATE};

    JComboBox genericTemplateComboBox = new JComboBox(dummyTemplateList);

    // Construct the frame
    @SuppressWarnings("unchecked")
    public ThingAdministratorFrame(ApplicationShell theApplicationShell) {
        // Start
        defaultCursor = this.getCursor();

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                sequenceValues[i][j] = new String("i=" + i + " j=" + j);
            }
        }

        mrApplicationShell = theApplicationShell;
        mrApplicationShell.setFrame(adminStatusBar, this);


        try {
            // get list of templates
            TemplateWrangler tWrangler = new TemplateWrangler(theApplicationShell);

            String[] templateList = tWrangler.getAllTemplates("");

            for (int i = 0; i < templateList.length; i++) {
                String descr = tWrangler.getDescr(templateList[i]);
                genericTemplateComboBox.addItem(templateList[i] + " - " + descr);

            }
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            mrApplicationShell.error("get list of templates: " + e1.getMessage());
        }


        this.paintAll(this.getGraphics());

        createChoosers();

        // Fill in values from ini file...
        fillInDefaults();
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);

        try {
            currentMode = PRE_LOGIN;
            setNewMode(currentMode);
            jbInit();
            SwingUtilities.updateComponentTreeUI(this);
            this.invalidate();
            this.validate();
            this.repaint();

            if (!OB4) {
                removeV4Components();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Component initialization
    private void jbInit() throws Exception {

        titledBorder1 = new TitledBorder("");
        border1 = BorderFactory.createEmptyBorder();
        border2 = BorderFactory.createEmptyBorder();
        this.getContentPane().setLayout(adminLayout);
        this.setFont(new java.awt.Font("Dialog", 0, 11));
        this.setSize(new Dimension(794, 600));

        this
                // Read this one on screen before changing it: until 2026-08-07 the title bar
                // literally said "... VPARAM_PRODUCT_VERSION - PARAM_COPYRIGHT_NOTICE_LONG",
                // because two unsubstituted tokens sat inside the string literal where the
                // branding audit could not see them.
                .setTitle(Namer.param_product_name + " V" + Namer.param_product_version
                        + " - " + Namer.param_vendor_name);
        this.addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                this_windowClosing(e);
            }
        });
        adminStatusBar.setFont(new java.awt.Font("Dialog", 1, 11));
        adminStatusBar.setDoubleBuffered(true);
        adminStatusBar.setText(" ");
        menuFile.setText("File");

        menuFileNew.setText("New");
        menuFileNew.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                fileNew_actionPerformed(e);
            }
        });

        menuFileOpen.setText("Open");
        menuFileOpen.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                fileOpen_actionPerformed(e);
            }
        });

        menuFileSave.setText("Save");
        menuFileSave.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                fileSave_actionPerformed(e);
            }
        });

        menuFileSaveAs.setText("Save As");
        menuFileSaveAs.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                fileSaveAs_actionPerformed(e);
            }
        });

        menuFileExit.setText("Exit");
        menuFileExit.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                fileExit_actionPerformed(e);
            }
        });

        menuHelp.setText("Help");
        menuHelpAbout.setText("About");
        menuHelpAbout.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                helpAbout_actionPerformed(e);
            }
        });
        loginPanel.setLayout(gridBagLayout1);
        hostLabel.setFont(new java.awt.Font("Dialog", 1, 11));
        hostLabel.setDoubleBuffered(true);
        hostLabel.setText("TCP/IP Hostname");
        hostnameField.setFont(new java.awt.Font("Dialog", 0, 11));
        hostnameField.setDoubleBuffered(true);
        hostnameField
                .setToolTipText("Hostname of the Server with the Oracle Database");
        SIDLabel.setFont(new java.awt.Font("Dialog", 1, 11));
        SIDLabel.setDoubleBuffered(true);
        SIDLabel.setText("Oracle SID/ /Service");
        SIDLabel.setToolTipText("Prefix service names with '/'");
        sidField.setFont(new java.awt.Font("Dialog", 0, 11));
        sidField.setDoubleBuffered(true);
        //sidField.setToolTipText("Oracle Instance Name");
        sidField.setToolTipText("Oracle Instance Name - Prefix service names with '/'");
        portLabel.setFont(new java.awt.Font("Dialog", 1, 11));
        portLabel.setDoubleBuffered(true);
        portLabel.setText("SQL*NET Port");
        portField.setFont(new java.awt.Font("Dialog", 0, 11));
        portField.setDoubleBuffered(true);
        portField.setToolTipText("Sql*Net Port. Usually 1521.");
        userLabel.setFont(new java.awt.Font("Dialog", 1, 11));
        userLabel.setDoubleBuffered(true);
        userLabel.setText("Oracle User");
        oraUserField.setFont(new java.awt.Font("Dialog", 0, 11));
        oraUserField.setDoubleBuffered(true);
        oraUserField
                .setToolTipText("Username of schema code will be run against");
        oraUserField.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                oraUserField_actionPerformed(e);
            }
        });
        passwordLabel.setFont(new java.awt.Font("Dialog", 1, 11));
        passwordLabel.setDoubleBuffered(true);
        passwordLabel.setText("Oracle Password");
        oraPassField
                .setToolTipText("Password for schema code will be run against");
        oraPassField.setDoubleBuffered(true);
        oraPassField.setFont(new java.awt.Font("Monospaced", 0, 11));
        connectionStatusField.setBorder(BorderFactory
                .createLoweredBevelBorder());
        connectionStatusField.setLineWrap(true);
        connectionStatusField.setDoubleBuffered(true);
        connectionStatusField.setDisabledTextColor(Color.red);
        connectionStatusField.setEditable(false);
        connectionStatusField.setFont(new java.awt.Font("Monospaced", 0, 11));
        connectionStatusField.setText("Not Connected");
        connectButton.setFont(new java.awt.Font("Dialog", 1, 11));
        connectButton.setDoubleBuffered(true);
        connectButton.setText(CONNECT_BUTTON_TEXT);
        connectButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                connectButton_actionPerformed(e);
            }
        });

        adminTabbedPane.setFont(new java.awt.Font("Dialog", 1, 11));
        adminTabbedPane.setDoubleBuffered(true);
        adminTabbedPane.setToolTipText("");
        selectObjectsPanel.setEnabled(false);
        selectObjectsPanel.setFont(new java.awt.Font("Dialog", 0, 11));
        selectObjectsPanel
                .setToolTipText("This screen allows you to select the objects you wish to generate "
                        + "code for.");
        selectObjectsPanel.setLayout(borderLayout1);

        codeOptionsPanel.setLayout(gridBagLayout8);
        // codeRootDirTextField.setText("C:\\DR\\Test");
        // packageNameTextField1.setText("com.foo.bar");
        // creationInfoTextArea.setText("a --- b");
        //authorVersionTextArea2.setLineWrap(true);
        //authorVersionTextArea2.setPreferredSize(new Dimension(765, 28));
        //authorVersionTextArea2.setWrapStyleWord(true);
        //authorVersionTextArea2.setMargin(new Insets(10, 10, 10, 10));
        //authorVersionTextArea2.setDoubleBuffered(true);
        //authorVersionTextArea2.setBackground(Color.lightGray);
        //authorVersionTextArea2.setBorder(BorderFactory.createEtchedBorder());
        //authorVersionTextArea2.setMinimumSize(new Dimension(765, 28));
        //authorVersionTextArea2
        //		.setText("3.2 Enter a comment to appear at the start of every generated file. "
        //				+ "If you enter the name of a text file in this field its contents will "
        //				+ "be used.");
        //authorVersionTextArea2.setEditable(false);
        //authorVersionTextArea2.setRequestFocusEnabled(false);
        //authorVersionTextArea2.setFont(new java.awt.Font("Dialog", 1, 11));
        authorLabel.setFont(new java.awt.Font("Dialog", 1, 11));
        authorLabel.setDoubleBuffered(true);
        authorLabel.setText("@author");
        versionLabel.setFont(new java.awt.Font("Dialog", 1, 11));
        versionLabel.setDoubleBuffered(true);
        versionLabel.setText("@version");
        buildOptionsTextArea.setFont(new java.awt.Font("Dialog", 1, 11));
        buildOptionsTextArea.setRequestFocusEnabled(false);
        buildOptionsTextArea.setEditable(false);
        buildOptionsTextArea
                .setText("3.2 Change these values if you want to alter the code that is generated. "
                        + "Accept the default values if you are not sure which options to use.");
        buildOptionsTextArea.setBorder(BorderFactory.createEtchedBorder());
        buildOptionsTextArea.setMinimumSize(new Dimension(765, 28));
        buildOptionsTextArea.setBackground(Color.lightGray);
        buildOptionsTextArea.setMargin(new Insets(10, 10, 10, 10));
        buildOptionsTextArea.setDoubleBuffered(true);
        buildOptionsTextArea.setWrapStyleWord(true);
        buildOptionsTextArea.setLineWrap(true);
        buildOptionsTextArea.setPreferredSize(new Dimension(765, 28));
        commentTextArea.setLineWrap(true);
        commentTextArea.setPreferredSize(new Dimension(300, 28));
        commentTextArea
                .setToolTipText("Examples: \"My Code\", /extra/smith/copyright_notice.txt");
        commentTextArea.setDoubleBuffered(true);
        commentTextArea.setBorder(BorderFactory.createLoweredBevelBorder());
        commentTextArea.setMinimumSize(new Dimension(300, 28));
        commentTextArea.setFont(new java.awt.Font("Dialog", 0, 11));
        // commentTextArea.setText("This module was generated by
        // ATB Consultancy Services Ltd\'s product MCPDBWizard.");
        commentLabel1.setFont(new java.awt.Font("Dialog", 1, 11));
        commentLabel1.setDoubleBuffered(true);
        commentLabel1.setText("@comment");
        authorVersionTextArea4.setFont(new java.awt.Font("Dialog", 1, 11));
        authorVersionTextArea4.setRequestFocusEnabled(false);
        authorVersionTextArea4.setEditable(false);
        authorVersionTextArea4
                .setText("3.1 Enter your name and the version of your product. This information "
                        + "will be used by Javadoc.");
        authorVersionTextArea4.setBorder(BorderFactory.createEtchedBorder());
        authorVersionTextArea4.setMinimumSize(new Dimension(765, 28));
        authorVersionTextArea4.setBackground(Color.lightGray);
        authorVersionTextArea4.setMargin(new Insets(10, 10, 10, 10));
        authorVersionTextArea4.setDoubleBuffered(true);
        authorVersionTextArea4.setWrapStyleWord(true);
        authorVersionTextArea4.setLineWrap(true);
        authorVersionTextArea4.setPreferredSize(new Dimension(765, 28));

        otherCheckBox
                .setToolTipText("Selecting this box includes all objects owned by the user named in "
                        + "the field to the right");
        otherCheckBox.setDoubleBuffered(true);
        otherCheckBox.setText("User");
        otherCheckBox.setFont(new java.awt.Font("Dialog", 1, 11));
        optionPanel.setLayout(gridBagLayout2);
        userOrAllLabel.setFont(new java.awt.Font("Dialog", 1, 11));
        userOrAllLabel.setDoubleBuffered(true);
        userOrAllLabel.setText("2.1 Objects Belonging To...");
        meCheckBox
                .setToolTipText("Selecting this box includes all objects owned by the user you are "
                        + "logged in as.");
        meCheckBox.setDoubleBuffered(true);
        meCheckBox.setText("Current User");
        meCheckBox.setFont(new java.awt.Font("Dialog", 1, 11));
        meCheckBox.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                meCheckBox_actionPerformed(e);
            }
        });
        sequencePanel.setLayout(borderLayout3);
        objectRefreshButton.setFont(new java.awt.Font("Dialog", 1, 11));
        objectRefreshButton.setDoubleBuffered(true);
        objectRefreshButton
                .setToolTipText("Refresh the data from the Database");
        objectRefreshButton.setText("Refresh");
        objectRefreshButton
                .addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        objectRefreshButton_actionPerformed(e);
                    }
                });
        sequenceTablePanel.setLayout(gridBagLayout4);
        sequencePanel.setFont(new java.awt.Font("Dialog", 0, 11));
        sequencePanel.setPreferredSize(new Dimension(800, 800));
        sequencePanel
                .setToolTipText("This table shows all the Sequences that belong to the users you specified "
                        + "in step 2.1");
        sequenceTable.setFont(new java.awt.Font("Dialog", 0, 11));
        sequenceTable.setDoubleBuffered(true);
        sequenceTable.setMaximumSize(new Dimension(632767, 632767));
        sequenceTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        sequenceTableScrollPane.setFont(new java.awt.Font("Dialog", 0, 11));
        sequenceTableScrollPane.setAutoscrolls(true);
        sequenceTableScrollPane.setDoubleBuffered(true);
        sequenceTableScrollPane.setMaximumSize(new Dimension(632767, 632767));
        selectAllSequencesButton.setFont(new java.awt.Font("Dialog", 1, 11));
        selectAllSequencesButton.setDoubleBuffered(true);
        selectAllSequencesButton
                .setToolTipText("Pressing this button selects all visible sequences");
        selectAllSequencesButton.setText("Select All...");
        selectAllSequencesButton
                .addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        selectAllSequencesButton_actionPerformed(e);
                    }
                });
        selectNoSequencesButton.setFont(new java.awt.Font("Dialog", 1, 11));
        selectNoSequencesButton.setDoubleBuffered(true);
        selectNoSequencesButton
                .setToolTipText("Pressing this button de-selects all visible sequences");
        selectNoSequencesButton.setText("Select None...");
        selectNoSequencesButton
                .addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        selectNoSequencesButton_actionPerformed(e);
                    }
                });
        debugMessagesCheckbox
                .setToolTipText("Setting this flag leads to code that has detailed debug messages");
        debugMessagesCheckbox.setDoubleBuffered(true);
        debugMessagesCheckbox.setText("Debug Messages in Code");
        debugMessagesCheckbox.setFont(new java.awt.Font("Dialog", 1, 11));
        debugMessagesCheckbox
                .addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        debugMessagesCheckbox_actionPerformed(e);
                    }
                });
        otherMessagesCheckbox
                .addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        otherMessagesCheckbox_actionPerformed(e);
                    }
                });
        otherMessagesCheckbox
                .setToolTipText("Setting this flag leads to code that generates other messages such "
                        + "as warnings");
        otherMessagesCheckbox.setDoubleBuffered(true);
        otherMessagesCheckbox.setText("Other  Messages in Code");
        otherMessagesCheckbox.setFont(new java.awt.Font("Dialog", 1, 11));
        oracleVersionComboBox
                .addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        oracleVersionComboBox_actionPerformed(e);
                    }
                });
        commentsCheckBox
                .setToolTipText("Setting this flag leads to code that has comments");
        commentsCheckBox.setDoubleBuffered(true);
        commentsCheckBox.setText("Comments in Code");
        commentsCheckBox.setFont(new java.awt.Font("Dialog", 1, 11));
        statsCheckBox1
                .setToolTipText("Setting this flag leads to code that implements StatsInterface, a "
                        + "simple performance monitoring package");
        statsCheckBox1.setDoubleBuffered(true);
        statsCheckBox1.setText("Maintain Usage Statistics");
        statsCheckBox1.setFont(new java.awt.Font("Dialog", 1, 11));
        statsCheckBox1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                statsCheckBox1_actionPerformed(e);
            }
        });
        javaNamingConventionComboBox
                .addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        javaNamingConventionComboBox_actionPerformed(e);
                    }
                });
        outputPanel.setLayout(borderLayout4);
        generateCodeButton.setFont(new java.awt.Font("Dialog", 1, 11));
        generateCodeButton.setDoubleBuffered(true);
        generateCodeButton.setToolTipText("Press to create code");
        generateCodeButton.setActionCommand("generateCodeButton");
        generateCodeButton.setText("Generate Code");
        generateCodeButton
                .addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        generateCodeButton_actionPerformed(e);
                    }
                });
        outputButtonPanel.setLayout(gridBagLayout9);
        outputLogPanel.setLayout(borderLayout5);
        outputButtonPanel.setFont(new java.awt.Font("Dialog", 0, 11));
        outputButtonPanel.setMinimumSize(new Dimension(400, 70));
        outputButtonPanel.setPreferredSize(new Dimension(400, 70));
        jPanel2.setLayout(flowLayout2);
        flowLayout2.setAlignment(FlowLayout.LEFT);
        outputAreaScrollPane
                .setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        outputAreaScrollPane.setFont(new java.awt.Font("Dialog", 0, 11));
        outputAreaScrollPane.setAutoscrolls(true);
        outputAreaScrollPane.setDoubleBuffered(true);
        outputAreaScrollPane
                .setToolTipText("Display area for messages written to log file.");
        guiLogTextArea.setDoubleBuffered(true);
        guiLogTextArea.setEditable(false);
        guiLogTextArea.setFont(new java.awt.Font("Monospaced", 0, 11));
        selectAllFunctionsButton
                .addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        selectAllFunctionsButton_actionPerformed(e);
                    }
                });
        selectAllFunctionsButton.setFont(new java.awt.Font("Dialog", 1, 11));
        selectAllFunctionsButton.setDoubleBuffered(true);
        selectAllFunctionsButton
                .setToolTipText("Pressing this button selects all visible PL/SQL procedures and Functions");
        selectAllFunctionsButton.setText("Select All...");
        functionTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        functionTable.setMaximumSize(new Dimension(632767, 632767));
        functionTable.setFont(new java.awt.Font("Dialog", 0, 11));
        functionTable.setDoubleBuffered(true);

        flowLayout3.setAlignment(FlowLayout.LEFT);
        functionTableScrollPane.setMaximumSize(new Dimension(632767, 632767));
        functionTableScrollPane.setDoubleBuffered(true);
        functionTableScrollPane.setFont(new java.awt.Font("Dialog", 0, 11));
        functionTableScrollPane.setAutoscrolls(true);
        functionPanel.setLayout(borderLayout6);
        functionPanel.setFont(new java.awt.Font("Dialog", 0, 11));
        functionPanel.setPreferredSize(new Dimension(800, 800));
        functionPanel
                .setToolTipText("This table shows all the PL/SQL Functions and Procedures that belong "
                        + "to the users you specified in step 2.1");
        selectNoFunctionsButton1.setFont(new java.awt.Font("Dialog", 1, 11));
        selectNoFunctionsButton1.setDoubleBuffered(true);
        selectNoFunctionsButton1
                .setToolTipText("Pressing this button de-selects all visible PL/SQL procedures and "
                        + "Functions");
        selectNoFunctionsButton1.setText("Select None...");
        selectNoFunctionsButton1
                .addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        selectNoFunctionsButton1_actionPerformed(e);
                    }
                });
        jPanel3.setLayout(gridBagLayout3);
        jPanel4.setLayout(flowLayout3);
        authorTextField.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                authorTextField_actionPerformed(e);
            }
        });
        otherUserName.setFont(new java.awt.Font("Dialog", 0, 11));
        otherUserName.setDoubleBuffered(true);
        otherUserName
                .setToolTipText("This field contains a username or pattern so you can see objects "
                        + "owned by other people");
        optionPanel.setFont(new java.awt.Font("Dialog", 0, 11));
        optionPanel
                .setToolTipText("This screen allows you to select the objects you wish to generate "
                        + "code for.");
        selectObjectsTabbedPane.setFont(new java.awt.Font("Dialog", 1, 11));
        selectObjectsTabbedPane.setDoubleBuffered(true);
        selectObjectsTabbedPane
                .setToolTipText("This tab shows all the Packages, procedures and functions that are "
                        + "visible to you..");
        authorTextField.setFont(new java.awt.Font("Dialog", 0, 11));
        authorTextField.setBorder(BorderFactory.createLoweredBevelBorder());
        authorTextField.setDoubleBuffered(true);
        authorTextField.setToolTipText("Example: jsmith@mycompany.com");
        versionTextField.setFont(new java.awt.Font("Dialog", 0, 11));
        versionTextField.setBorder(BorderFactory.createLoweredBevelBorder());
        versionTextField.setDoubleBuffered(true);
        versionTextField.setToolTipText("Example: 2.0");
        versionTextField.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                versionTextField_actionPerformed(e);
            }
        });
        oracleVersionComboBox.setFont(new java.awt.Font("Dialog", 1, 11));
        oracleVersionComboBox.setDoubleBuffered(true);
        oracleVersionComboBox.setMaximumSize(new Dimension(162, 24));
        oracleVersionComboBox
                .setToolTipText("Which version of the Oracle server will the code be run against?");
        javaNamingConventionComboBox
                .setFont(new java.awt.Font("Dialog", 1, 11));
        javaNamingConventionComboBox.setDoubleBuffered(true);
        javaNamingConventionComboBox.setMaximumSize(new Dimension(162, 24));
        javaNamingConventionComboBox
                .setToolTipText("You can generate java files whose names closely match their database "
                        + "objects or attempt to conform to normal java naming conventions. "
                        + "The \'spaces_between_words\' naming convention is deprecated.");
        codeOptionsPanel.setFont(new java.awt.Font("Dialog", 0, 11));
        codeOptionsPanel.setMinimumSize(new Dimension(800, 445));
        codeOptionsPanel.setPreferredSize(new Dimension(800, 445));
        codeOptionsPanel
                .setToolTipText("This tab allows you to specify which version of the database and "
                        + "various optional features such as comments");
        loginPanel.setFont(new java.awt.Font("Dialog", 0, 11));
        loginPanel.setToolTipText("Use this window to log into the database.");
        selectAllTablesButton
                .addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        selectAllTablesButton_actionPerformed(e);
                    }
                });
        selectAllTablesButton.setText("Select All...");
        selectAllTablesButton
                .setToolTipText("Pressing this button selects all visible tables");
        selectAllTablesButton.setFont(new java.awt.Font("Dialog", 1, 11));
        selectAllTablesButton.setDoubleBuffered(true);
        tableTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tableTable.setMaximumSize(new Dimension(632767, 632767));
        tableTable.setFont(new java.awt.Font("Dialog", 0, 11));
        tableTable.setDoubleBuffered(true);
        tableTablePanel.setLayout(gridBagLayout10);
        flowLayout4.setAlignment(FlowLayout.LEFT);
        tableTableScrollPane.setMaximumSize(new Dimension(632767, 632767));
        tableTableScrollPane.setDoubleBuffered(true);
        tableTableScrollPane.setFont(new java.awt.Font("Dialog", 0, 11));
        tableTableScrollPane.setAutoscrolls(true);
        tablePanel.setLayout(borderLayout7);
        tablePanel.setFont(new java.awt.Font("Dialog", 0, 11));
        tablePanel.setPreferredSize(new Dimension(800, 800));
        tablePanel
                .setToolTipText("This table shows all the Sequences that belong to the users you specified "
                        + "in step 2.1");
        selectNoTablesButton.setFont(new java.awt.Font("Dialog", 1, 11));
        selectNoTablesButton.setDoubleBuffered(true);
        selectNoTablesButton
                .setToolTipText("Pressing this button de-selects all visible tables");
        selectNoTablesButton.setText("Select None...");
        selectNoTablesButton
                .addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        selectNoTablesButton_actionPerformed(e);
                    }
                });
        jPanel5.setLayout(flowLayout4);
        selectAllSqlButton.setFont(new java.awt.Font("Dialog", 1, 11));
        selectAllSqlButton.setDoubleBuffered(true);
        selectAllSqlButton
                .setToolTipText("Pressing this button selects all visible files");
        selectAllSqlButton.setText("Select All...");
        selectAllSqlButton
                .addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        selectAllSqlButton_actionPerformed(e);
                    }
                });
        sqlTablePanel.setLayout(gridBagLayout5);
        selectNoSqlButton
                .addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        selectNoSqlButton_actionPerformed(e);
                    }
                });
        selectNoSqlButton.setText("Select None...");
        selectNoSqlButton
                .setToolTipText("Pressing this button de-selects all visible files");
        selectNoSqlButton.setFont(new java.awt.Font("Dialog", 1, 11));
        selectNoSqlButton.setDoubleBuffered(true);
        sqlTableScrollPane
                .setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sqlTableScrollPane
                .setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        sqlTableScrollPane.setAutoscrolls(true);
        sqlTableScrollPane.setDoubleBuffered(true);
        sqlTableScrollPane.setMaximumSize(new Dimension(632767, 632767));
        sqlTableScrollPane.setPreferredSize(new Dimension(800, 500));
        sqlPanel.setLayout(borderLayout8);
        sqlPanel.setFont(new java.awt.Font("Dialog", 0, 11));
        sqlPanel.setPreferredSize(new Dimension(800, 800));
        sqlPanel
                .setToolTipText("This window is used to select SQL files you wish to turn into Java "
                        + "classes.");
        sqlTableHeaderPanel.setLayout(gridBagLayout6);
        sqlTableHeaderPanel.setFont(new java.awt.Font("Dialog", 0, 11));
        sqlTableHeaderPanel.setMinimumSize(new Dimension(214, 170));
        sqlTableHeaderPanel.setPreferredSize(new Dimension(214, 170));
        aspRootDirTextField
                .setToolTipText("All the files in this directory that end in \'.sql\' can be used to "
                        + "generate matching access classes.");
        aspRootDirTextField
                .addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        aspRootDirTextField_actionPerformed(e);
                    }
                });
        aspRootDirTextField.setFont(new java.awt.Font("Dialog", 0, 11));
        aspRootDirTextField.setDoubleBuffered(true);
        aspFileDirectorytextArea.setLineWrap(true);
        aspFileDirectorytextArea.setWrapStyleWord(true);
        aspFileDirectorytextArea.setDoubleBuffered(true);
        aspFileDirectorytextArea.setMargin(new Insets(2, 2, 2, 2));
        aspFileDirectorytextArea.setBackground(Color.lightGray);
        aspFileDirectorytextArea.setBorder(BorderFactory.createEtchedBorder());
        aspFileDirectorytextArea
                .setText("2.2.3.2 Select the SQL statements you wish to generate code for and "
                        + "specify data types and names for their parameters");
        aspFileDirectorytextArea.setEditable(false);
        aspFileDirectorytextArea.setRequestFocusEnabled(false);
        aspFileDirectorytextArea.setFont(new java.awt.Font("Dialog", 1, 11));
        aspDirFileChooserButton
                .addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        aspDirFileChooserButton_actionPerformed(e);
                    }
                });
        aspDirFileChooserButton.setText("Find Directory...");
        aspDirFileChooserButton.setActionCommand("codeRootFileChooserButton");
        aspDirFileChooserButton.setFont(new java.awt.Font("Dialog", 1, 11));
        aspDirFileChooserButton.setDoubleBuffered(true);
        aspFileDirectorytextArea1.setFont(new java.awt.Font("Dialog", 1, 11));
        aspFileDirectorytextArea1.setRequestFocusEnabled(false);
        aspFileDirectorytextArea1.setEditable(false);
        aspFileDirectorytextArea1
                .setText("2.2.3.1 Enter the directory where you keep your SQL statement files");
        aspFileDirectorytextArea1.setBorder(BorderFactory.createEtchedBorder());
        aspFileDirectorytextArea1.setBackground(Color.lightGray);
        aspFileDirectorytextArea1.setMargin(new Insets(2, 2, 2, 2));
        aspFileDirectorytextArea1.setToolTipText("");
        aspFileDirectorytextArea1.setDoubleBuffered(true);
        aspFileDirectorytextArea1.setWrapStyleWord(true);
        aspFileDirectorytextArea1.setLineWrap(true);
        sqlTableSpiltPane.setFont(new java.awt.Font("Dialog", 0, 11));
        sqlTableSpiltPane.setBottomComponent(null);
        sqlTableSpiltPane.setLastDividerLocation(100);
        sqlTableSpiltPane.setLeftComponent(sqlTableTreeScrollPane);
        sqlTableSpiltPane.setTopComponent(null);
        sqlTableTree.setFont(new java.awt.Font("Dialog", 0, 11));
        sqlTableTree.setAutoscrolls(true);
        sqlTableTree.setDoubleBuffered(true);
        sqlTableTree.setMaximumSize(new Dimension(200, 1000));
        sqlTableTree.setMinimumSize(new Dimension(220, 180));
        sqlTableTree.setPreferredSize(new Dimension(220, 1000));
        sqlTableTree
                .setToolTipText("This tree shows the SQL files in the directory you entered in step "
                        + "2.2.4.1");
        sqlTableTreeScrollPane
                .setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        sqlTableTreeScrollPane.setFont(new java.awt.Font("Dialog", 0, 11));
        sqlTableTreeScrollPane.setDoubleBuffered(true);
        sqlTableTreeScrollPane.setMinimumSize(new Dimension(220, 190));
        sqlTableTreeScrollPane.setPreferredSize(new Dimension(220, 200));
        jLabel1.setFont(new java.awt.Font("Dialog", 1, 11));
        jLabel1.setText("3.2.1 Messages in generated code");
        jLabel2.setFont(new java.awt.Font("Dialog", 1, 11));
        jLabel2.setMaximumSize(new Dimension(162, 16));
        jLabel2.setMinimumSize(new Dimension(162, 16));
        jLabel2.setPreferredSize(new Dimension(162, 16));
        jLabel2.setText("3.2.2 Comments in generated code");
        jLabel3.setFont(new java.awt.Font("Dialog", 1, 11));
        jLabel3.setMaximumSize(new Dimension(170, 16));
        jLabel3.setMinimumSize(new Dimension(170, 16));
        jLabel3.setPreferredSize(new Dimension(170, 16));
        jLabel3.setText("3.2.3 Basic statistics  in generated code");
        jLabel4.setFont(new java.awt.Font("Dialog", 1, 11));
        jLabel4
                .setToolTipText("You can generate java files whose names closely match their database "
                        + "objects or attempt to conform to normal java naming conventions. "
                        + "The \'spaces_between_words\' naming convention is deprecated.");
        jLabel4.setText("3.2.4 Naming convention");
        jLabel5.setFont(new java.awt.Font("Dialog", 1, 11));
        jLabel5
                .setToolTipText("What version of oracle will you be running the code against?");
        jLabel5.setText("3.2.5 Target Version of Oracle");
        jLabel6.setFont(new java.awt.Font("Dialog", 1, 11));
        jLabel6.setToolTipText("");
        jLabel6.setText("3.2.6 Use the following Numeric data types:");
        useShortCB
                .setToolTipText("Select this if you want generated code to create methods that take "
                        + "short when working with NUMBER datatypes in oracle");
        useShortCB.setText("short");
        useShortCB.setFont(new java.awt.Font("Dialog", 1, 11));
        useIntCB.setText("int");
        useIntCB.setFont(new java.awt.Font("Dialog", 1, 11));
        useIntCB
                .setToolTipText("Select this if you want generated code to create methods that take "
                        + "int when working with NUMBER datatypes in oracle");
        useByteCB.setText("byte");
        useByteCB.setFont(new java.awt.Font("Dialog", 1, 11));
        useByteCB
                .setToolTipText("Select this if you want generated code to create methods that take "
                        + "byte when working with NUMBER datatypes in oracle");
        useLongCB.setText("long");
        useLongCB.setFont(new java.awt.Font("Dialog", 1, 11));
        useLongCB
                .setToolTipText("Select this if you want generated code to create methods that take "
                        + "long when working with NUMBER datatypes in oracle");
        useFloatCB.setText("float");
        useFloatCB.setFont(new java.awt.Font("Dialog", 1, 11));
        useFloatCB
                .setToolTipText("Select this if you want generated code to create methods that take "
                        + "float when working with NUMBER datatypes in oracle");
        useDoubleCB.setText("double");
        useDoubleCB.setFont(new java.awt.Font("Dialog", 1, 11));
        useDoubleCB.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                useDoubleCB_actionPerformed(e);
            }
        });
        useDoubleCB
                .setToolTipText("Select this if you want generated code to create methods that take "
                        + "double when working with NUMBER datatypes in oracle");
        useShortOCB
                .setToolTipText("Select this if you want generated code to create methods that take "
                        + "Short when working with NUMBER datatypes in oracle");
        useShortOCB.setText("java.lang.Short");
        useShortOCB.setFont(new java.awt.Font("Dialog", 1, 11));
        useIntOCB.setText("java.lang.Integer");
        useIntOCB.setFont(new java.awt.Font("Dialog", 1, 11));
        useIntOCB
                .setToolTipText("Select this if you want generated code to create methods that take "
                        + "Integer when working with NUMBER datatypes in oracle");
        useByteOCB.setText("java.lang.Byte");
        useByteOCB.setFont(new java.awt.Font("Dialog", 1, 11));
        useByteOCB
                .setToolTipText("Select this if you want generated code to create methods that take "
                        + "Byte when working with NUMBER datatypes in oracle");
        useLongOCB.setText("java.lang.Long");
        useLongOCB.setFont(new java.awt.Font("Dialog", 1, 11));
        useLongOCB
                .setToolTipText("Select this if you want generated code to create methods that take "
                        + "Long when working with NUMBER datatypes in oracle");
        useFloatOCB.setText("java.lang.Float");
        useFloatOCB.setFont(new java.awt.Font("Dialog", 1, 11));
        useFloatOCB
                .setToolTipText("Select this if you want generated code to create methods that take "
                        + "Float when working with NUMBER datatypes in oracle");
        useDoubleOCB.setText("java.lang.Double");
        useDoubleOCB.setFont(new java.awt.Font("Dialog", 1, 11));
        useDoubleOCB.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                useDoubleOCB_actionPerformed(e);
            }
        });
        useDoubleOCB
                .setToolTipText("Select this if you want generated code to create methods that take "
                        + "Double when working with NUMBER datatypes in oracle");
        jLabel7.setText("3.2.7 Table specific options");
        jLabel7.setFont(new java.awt.Font("Dialog", 1, 11));
        jLabel7.setToolTipText("");
        validateCB
                .setToolTipText("If checked an exception will be thrown if you tyr to insert or update "
                        + "a record that has a not null value for a null column or one where "
                        + "the length of a field exceeds the precision of the data type in the "
                        + "database.");
        validateCB.setText("Check columns before Insert or Update");
        validateCB.setFont(new java.awt.Font("Dialog", 1, 11));
        validateCB.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                validateCB_actionPerformed(e);
            }
        });
        extraSqlCheckBox
                .setToolTipText("This option creates additional Oracle TYPE "
                        + "objects needed to run procedures with array objects defined inside packages");
        extraSqlCheckBox
                .setText("Create Oracle TYPE definitions if they are needed to run procedures");
        extraSqlCheckBox.setText("Create needed TYPE objects");
        extraSqlCheckBox.setFont(new java.awt.Font("Dialog", 1, 11));
        codeRootDirTextField.setFont(new java.awt.Font("SansSerif", 0, 11));
        codeRootDirTextField.setDoubleBuffered(true);
        codeRootDirTextField.setMaximumSize(new Dimension(2147483647, 21));
        codeRootDirTextField.setMinimumSize(new Dimension(4, 21));
        codeRootDirTextField.setToolTipText("Example: C:\\MyProject\\Src");
        codeRootDirTextField
                .addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        codeRootDirTextField_actionPerformed(e);
                    }
                });
        packageNameTextArea.setLineWrap(true);
        packageNameTextArea.setWrapStyleWord(true);
        packageNameTextArea.setMargin(new Insets(2, 2, 2, 2));
        packageNameTextArea.setDoubleBuffered(true);
        packageNameTextArea.setMaximumSize(new Dimension(2147483647, 55));
        packageNameTextArea.setBackground(Color.lightGray);
        packageNameTextArea.setBorder(BorderFactory.createEtchedBorder());
        packageNameTextArea
                .setText("4.2 Enter the package name you want the generated classes to belong "
                        + "to. e.g. com.mycompany.myapplication.generated ");
        packageNameTextArea.setEditable(false);
        packageNameTextArea.setRequestFocusEnabled(false);
        packageNameTextArea.setFont(new java.awt.Font("Dialog", 1, 11));
        fileOptionsPanel.setLayout(gridBagLayout11);
        fileOptionsPanel.setFont(new java.awt.Font("Dialog", 0, 11));
        fileOptionsPanel.setMinimumSize(new Dimension(800, 500));
        fileOptionsPanel.setPreferredSize(new Dimension(800, 500));
        fileOptionsPanel
                .setToolTipText("This screen controls where the code is generated ");
        packageNameTextField1.setFont(new java.awt.Font("Dialog", 0, 11));
        packageNameTextField1.setDoubleBuffered(true);
        packageNameTextField1.setMaximumSize(new Dimension(2147483647, 21));
        packageNameTextField1.setMinimumSize(new Dimension(4, 21));
        packageNameTextField1.setPreferredSize(new Dimension(4, 21));
        packageNameTextField1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                packageNameTextField1_actionPerformed(e);
            }
        });
        packageNameTextField1
                .addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        packageNameTextField1_actionPerformed(e);
                    }
                });
        directoriesMessedWithtextArea1.setPreferredSize(new Dimension(660, 28));
        directoriesMessedWithtextArea1.setLineWrap(true);
        directoriesMessedWithtextArea1.setWrapStyleWord(true);
        directoriesMessedWithtextArea1.setDoubleBuffered(true);
        directoriesMessedWithtextArea1.setMaximumSize(new Dimension(2147483647,
                50));
        directoriesMessedWithtextArea1.setMargin(new Insets(2, 2, 2, 2));
        directoriesMessedWithtextArea1.setBackground(Color.lightGray);
        directoriesMessedWithtextArea1.setMinimumSize(new Dimension(660, 20));
        directoriesMessedWithtextArea1
                .setText("4.1 Enter the root directory for your Java code. This is usually "
                        + " the one above \'com\'. For Example: C:\\Test\\Src");
        directoriesMessedWithtextArea1.setEditable(false);
        directoriesMessedWithtextArea1.setRequestFocusEnabled(false);
        directoriesMessedWithtextArea1.setFont(new java.awt.Font("Dialog", 1, 11));
        directoriesMessedWithtextArea1.setBorder(BorderFactory.createEtchedBorder());

        codeRootFileChooserButton.setFont(new java.awt.Font("Dialog", 1, 11));
        codeRootFileChooserButton.setDoubleBuffered(true);
        codeRootFileChooserButton.setActionCommand("codeRootFileChooserButton");
        codeRootFileChooserButton.setText("Find Directory...");
        codeRootFileChooserButton
                .addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        codeRootFileChooserButton_actionPerformed(e);
                    }
                });
        fileOptionsPanelExtraPanel.setLayout(gridBagLayout7);
        directoriesMessedWithtextArea2.setFont(new java.awt.Font("Dialog", 1,
                11));
        directoriesMessedWithtextArea2.setRequestFocusEnabled(false);
        directoriesMessedWithtextArea2.setEditable(false);
        directoriesMessedWithtextArea2
                .setText("4.3 Use the options below to define how your DAO Factory class will "
                        + "behave. ");
        directoriesMessedWithtextArea2.setBorder(BorderFactory
                .createEtchedBorder());
        directoriesMessedWithtextArea2.setMinimumSize(new Dimension(660, 21));
        directoriesMessedWithtextArea2.setBackground(Color.lightGray);
        directoriesMessedWithtextArea2.setMargin(new Insets(2, 2, 2, 2));
        directoriesMessedWithtextArea2.setDoubleBuffered(true);
        directoriesMessedWithtextArea2.setMaximumSize(new Dimension(2147483647,
                200));
        directoriesMessedWithtextArea2.setWrapStyleWord(true);
        directoriesMessedWithtextArea2.setLineWrap(true);
        directoriesMessedWithtextArea2.setPreferredSize(new Dimension(660, 21));
        daoFactoryClassNameLabel.setFont(new java.awt.Font("Dialog", 1, 11));
        daoFactoryClassNameLabel.setText("DAO Factory Class name");
        daoFactoryNameTextField
                .addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        daoFactoryNameTextField_actionPerformed(e);
                    }
                });
        generateSessionBeanCheckBox
                .setToolTipText("Selecting this means that the DAO Factory class will work as a Session "
                        + "Bean");
        generateSessionBeanCheckBox.setText("Implement javax.ejb.SessionBean");
        generateSessionBeanCheckBox.setFont(new java.awt.Font("Dialog", 1, 11));
        generateSessionBeanCheckBox
                .addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        generateSessionBeanCheckBox_actionPerformed(e);
                    }
                });
        logTypeLabel.setFont(new java.awt.Font("Dialog", 1, 11));
        logTypeLabel
                .setToolTipText("The generated class can use one of 4 different logging mechanisms.");
        logTypeLabel.setText("Log messages using...");
        logNameLabel.setFont(new java.awt.Font("Dialog", 1, 11));
        logNameLabel
                .setToolTipText("Logs have either a file name or a logging context");
        logNameLabel.setText("Log Name/Directory");
        connectionTypeLabel.setFont(new java.awt.Font("Dialog", 1, 11));
        connectionTypeLabel
                .setToolTipText("The generated class has 4 different ways of getting a DB connection");
        connectionTypeLabel.setText("Get DB connection using ...");
        connectionNameLabel.setFont(new java.awt.Font("Dialog", 1, 11));
        connectionNameLabel
                .setToolTipText("The name of the database connection. This could be a connect string "
                        + "or a JNDI name. ");
        connectionNameLabel.setText("Connection Name");
        connectionNameTextField.setFont(new java.awt.Font("Dialog", 0, 11));
        connectionNameTextField.setPreferredSize(new Dimension(200, 21));
        connectionNameTextField
                .setToolTipText("The name of the database connection. This could be a connect string "
                        + "or a JNDI name. ");
        connectionNameTextField
                .addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        connectionNameTextField_actionPerformed(e);
                    }
                });
        logNameTextField1.setFont(new java.awt.Font("SansSerif", 0, 11));
        logNameTextField1.setMinimumSize(new Dimension(167, 20));
        logNameTextField1.setPreferredSize(new Dimension(200, 21));
        logNameTextField1
                .setToolTipText("This field should contain either the directory name for log files "
                        + "or the name used for Java logging and and Log4J");
        logNameTextField1
                .addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        logNameTextField1_actionPerformed(e);
                    }
                });
        daoFactoryNameTextField.setFont(new java.awt.Font("Dialog", 0, 11));
        daoFactoryNameTextField.setMinimumSize(new Dimension(167, 20));
        daoFactoryNameTextField
                .setToolTipText("Enter the name for the DAO Factory class without \".java\". If this "
                        + "field is blank no class will be created.");
        jLabel8.setFont(new java.awt.Font("Dialog", 1, 11));
        jLabel8.setMaximumSize(new Dimension(41, 1700));
        jLabel8.setText("            ");
        logTypeComboBox.setFont(new java.awt.Font("Dialog", 0, 11));
        logTypeComboBox
                .setToolTipText("The generated class can use one of 4 different logging mechanisms.");
        logTypeComboBox.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                logTypeComboBox_actionPerformed(e);
            }
        });
        connectionTypeComboBox
                .addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        connectionTypeComboBox_actionPerformed(e);
                    }
                });
        connectionTypeComboBox.setFont(new java.awt.Font("Dialog", 0, 11));
        connectionTypeComboBox
                .setToolTipText("The generated class has 4 different ways of getting a DB connection");
        fileOptionsPanelExtraPanel.setFont(new java.awt.Font("Dialog", 0, 11));
        fileOptionsPanelExtraPanel
                .setMaximumSize(new Dimension(2147483647, 216));
        fileOptionsPanelExtraPanel.setMinimumSize(new Dimension(700, 330));
        fileOptionsPanelExtraPanel.setPreferredSize(new Dimension(700, 400));
        dirChooser.setFont(new java.awt.Font("Dialog", 0, 10));
        selectObjectsButtonPanel.setFont(new java.awt.Font("Dialog", 0, 10));
        outputPanel.setFont(new java.awt.Font("Dialog", 0, 11));
        jPanel3.setFont(new java.awt.Font("Dialog", 0, 11));
        jPanel4.setFont(new java.awt.Font("Dialog", 0, 11));
        sequenceTablePanel.setFont(new java.awt.Font("Dialog", 0, 11));
        jPanel2.setFont(new java.awt.Font("Dialog", 0, 11));
        sqlTablePanel.setFont(new java.awt.Font("Dialog", 0, 11));
        sqlTableEditPanel.setFont(new java.awt.Font("Dialog", 0, 11));
        tableTablePanel.setFont(new java.awt.Font("Dialog", 0, 11));
        jPanel5.setFont(new java.awt.Font("Dialog", 0, 11));
        outputLogPanel.setFont(new java.awt.Font("Dialog", 0, 11));
        theAspToplevelPanel.setFont(new java.awt.Font("Dialog", 0, 11));
        theAspFilePanel.setFont(new java.awt.Font("Dialog", 0, 11));
        theBrokenPanel.setFont(new java.awt.Font("Dialog", 0, 11));
        // webServicesCB.setFont(new java.awt.Font("Dialog", 0, 11));
        addFinalizeMethodCheckBox.setText("Add \'finalize()\' method");
        addFinalizeMethodCheckBox.setFont(new java.awt.Font("Dialog", 1, 11));
        jLabel10.setFont(new java.awt.Font("Dialog", 1, 11));
        jLabel10.setText("Temporary Directory");
        tempDirTextField.setMinimumSize(new Dimension(150, 21));
        jLabel11.setFont(new java.awt.Font("Dialog", 1, 11));
        jLabel11.setText("Temporary File Prefix/Suffix");
        tempFilePrefixTextField.setFont(new java.awt.Font("Dialog", 0, 11));
        jLabel12.setFont(new java.awt.Font("Dialog", 1, 11));
        jLabel12.setText("/");
        tempFileSuffixTextField.setFont(new java.awt.Font("Dialog", 0, 11));
        createWSCheckBox
                .setToolTipText("If selected two additional classes will be created");
        createWSCheckBox
                .setText("Create Web Service Classes");
        createWSCheckBox.setFont(new java.awt.Font("Dialog", 1, 11));
        mcpServerCheckBox
                .setToolTipText("If selected an MCP (Model Context Protocol) server class is created (needs target JVM 17+)");
        mcpServerCheckBox
                .setText("Create MCP Server Class");
        mcpServerCheckBox.setFont(new java.awt.Font("Dialog", 1, 11));
        jTextArea1.setBackground(Color.lightGray);
        jTextArea1.setBorder(BorderFactory.createEtchedBorder());
        jTextArea1
                .setText("4.4 Use the options below to control how the service classes behave.");
        jTextArea1.setEditable(false);
        jTextArea1.setFont(new java.awt.Font("Dialog", 1, 11));
        jLabel9.setFont(new java.awt.Font("Dialog", 1, 11));
        jLabel9.setText("Interface Class Name");
        jLabel13.setFont(new java.awt.Font("Dialog", 1, 11));
        jLabel13.setText("Implementing Class Name");
        jLabel13B.setFont(new java.awt.Font("Dialog", 1, 11));
        jLabel13B
                .setToolTipText("Whether records are small with public variables or large with private "
                        + "ones");
        jLabel13B.setText("Web Service Record Type");
        wsImplClassTextField.setMinimumSize(new Dimension(167, 20));
        wsImplClassTextField.setPreferredSize(new Dimension(200, 21));
        wsImplClassTextField
                .setToolTipText("Enter the name for the DAO Implementing  class without \".java\". ");
        wsPreCallCheckBox.setText("Add pre call code stubs");
        wsPreCallCheckBox.setFont(new java.awt.Font("Dialog", 1, 11));
        wsPreCallCheckBox
                .addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        wsPreCallCheckBox_actionPerformed(e);
                    }
                });
        wsPostCallCheckBox.setText("Add post call code stubs");
        wsPostCallCheckBox.setFont(new java.awt.Font("Dialog", 1, 11));
        wsAlwaysReleaseCheckBox
                .setText("Always release database connection after call");
        wsAlwaysReleaseCheckBox
                .setActionCommand("Always release database connection after call");
        wsAlwaysReleaseCheckBox.setFont(new java.awt.Font("Dialog", 1, 11));
        wsInterfaceClassTextField.setMinimumSize(new Dimension(167, 20));
        wsInterfaceClassTextField
                .setToolTipText("Enter the name for the Interface class without \".java\". ");
        closeConnCheckBox.setText("Close Connections");
        closeConnCheckBox.setFont(new java.awt.Font("Dialog", 1, 11));
        commitConnCheckBox.setText("Commit Connections");
        commitConnCheckBox.setFont(new java.awt.Font("Dialog", 1, 11));

        setUpPoolOptionsTab();
        javaNumberTypeComboBox.setFont(new java.awt.Font("Dialog", 0, 11));
        javaNumberTypeComboBox
                .setToolTipText("The number data type used by generated service code");
        javaNumberTypeComboBox
                .addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        javaNumberTypeComboBox_actionPerformed(e);
                    }
                });
        xwsLabel.setText("External Web Service");
        xwsLabel.setFont(new java.awt.Font("Dialog", 1, 11));

        xwsImplLabel.setText("External Implementation Class");
        xwsImplLabel.setFont(new java.awt.Font("Dialog", 1, 11));

        xwsIfaceLabel.setText("External Interface Class");
        xwsIfaceLabel.setFont(new java.awt.Font("Dialog", 1, 11));

        jLabel14.setFont(new java.awt.Font("Dialog", 1, 11));
        jLabel14.setText("Number type used by service");
        jLabel16.setText(" ");
        wsRecTypeComboBox
                .setToolTipText("Whether records are small with public variables or large with private "
                        + "ones");
        wsRecTypeComboBox.setActionCommand("wsRecTypeComboBoxChanged");
        wsRecTypeComboBox
                .addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        wsRecTypeComboBox_actionPerformed(e);
                    }
                });
        jvmLabel.setFont(new java.awt.Font("Dialog", 1, 11));
        jvmLabel.setToolTipText("Select the Java Virtual Machine you want generated code to be compatible " +
                "with.");
        jvmLabel.setText("3.2.8 Target JVM:");
        plsqlLabel.setFont(new java.awt.Font("Dialog", 1, 11));
        plsqlLabel.setToolTipText("This text is used to create method names for calls to PL/SQL procedures " +
                "in generated code. Warning: Changing the default value can lead to " +
                "non-compiling code");
        plsqlLabel.setText("3.2.9 Prefix for PL/SQL Methods:");
        sqlLabel.setFont(new java.awt.Font("Dialog", 1, 11));
        sqlLabel.setText("3.2.10 Prefix for SQL Methods:");
        postOptionsPanel.setLayout(gridBagLayout12);
        postScriptDescrTextArea.setBackground(Color.lightGray);
        postScriptDescrTextArea.setBorder(BorderFactory.createEtchedBorder());
        postScriptDescrTextArea.setMinimumSize(new Dimension(652, 20));
        postScriptDescrTextArea.setText("5.2 In the area you below you can define a batch file that will be " +
                "automatically run when you have finished generating code");
        postScriptDescrTextArea.setEditable(false);
        postScriptDescrTextArea.setFont(new java.awt.Font("Dialog", 1, 11));
        postScriptCodeTextArea.setLineWrap(true);
        postScriptCodeTextArea.setPreferredSize(new Dimension(924, 50));
        postScriptCodeTextArea.setColumns(132);
        postScriptCodeTextArea.setRows(300);
        postScriptCodeTextArea.setToolTipText("This field contains the content for the OS batch script whose name " +
                "you enter above. " + Namer.param_prod_name + " will call the script with the following " +
                "parameters: Directory code generated in, Java  package name, package " +
                "name with OS file seperator, interface class name, implementation " +
                "class name");
        postScriptCodeTextArea.setDoubleBuffered(true);
        postScriptCodeTextArea.setMaximumSize(new Dimension(200, 9000));
        postScriptCodeTextArea.setBorder(BorderFactory.createLoweredBevelBorder());
        postScriptCodeTextArea.setMinimumSize(new Dimension(200, 20));
        jLabel18.setFont(new java.awt.Font("Dialog", 1, 11));
        jLabel18.setText("Post Generation Script Name:");
        //postDescrTextArea.setBackground(Color.lightGray);
        //postDescrTextArea.setFont(new java.awt.Font("Dialog", 1, 11));
        //postDescrTextArea.setBorder(BorderFactory.createEtchedBorder());
        // postDescrTextArea.setEditable(false);
        //postDescrTextArea.setText("5.1 If you need to add a custom method or code to your implementation " +
        //"class define it here. Anything you put here will be added directly " +
        //"to the code. ");
        extraCodeTextArea.setLineWrap(true);
        extraCodeTextArea.setPreferredSize(new Dimension(200, 100));
        extraCodeTextArea.setRows(300);
        extraCodeTextArea.setToolTipText("Any text entered here is added to the Implementation class source " +
                "code exactly as you enter it. This feature was added with Java Annotations " +
                "in mind but any legal Java syntax will work.");
        extraCodeTextArea.setDoubleBuffered(true);
        extraCodeTextArea.setMaximumSize(new Dimension(200, 9000));
        extraCodeTextArea.setBorder(BorderFactory.createLoweredBevelBorder());
        extraCodeTextArea.setMinimumSize(new Dimension(200, 100));
        extraCodeTextArea.setFont(new java.awt.Font("Monospaced", 1, 11));
        postOptionsPanel.setPreferredSize(new Dimension(800, 481));
        jLabel19.setFont(new java.awt.Font("Dialog", 1, 11));
        jLabel19.setText("Post Generation Script Content:");
        postDescrTextArea.setPreferredSize(new Dimension(672, 32));
        postDescrTextArea.setRows(2);
        postDescrTextArea.setBackground(Color.lightGray);
        postDescrTextArea.setBorder(BorderFactory.createEtchedBorder());
        postDescrTextArea.setMinimumSize(new Dimension(672, 20));
        postDescrTextArea.setText("5.1 Enter any extra Java code you want added to the implementaton " +
                "class below");
        postDescrTextArea.setFont(new java.awt.Font("Dialog", 1, 11));
        postScriptScrollPane.setDoubleBuffered(true);
        postScriptScrollPane.setMaximumSize(new Dimension(800, 300));
        postScriptScrollPane.setMinimumSize(new Dimension(100, 150));
        postScriptScrollPane.setPreferredSize(new Dimension(100, 150));
        jScrollPane4.setDoubleBuffered(true);
        jScrollPane4.setMaximumSize(new Dimension(800, 300));
        jScrollPane4.setMinimumSize(new Dimension(652, 100));
        jScrollPane4.setPreferredSize(new Dimension(204, 150));
        postScriptNameTextField.setDoubleBuffered(true);
        postScriptNameTextField.setToolTipText("If you provide a name for this script it will be created and run " +
                "for you after generation. This allows you to compile and package " +
                "generated code.");
        methodPlsqlTextField.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                methodPlsqlTextField_actionPerformed(e);
            }
        });
        jvmComboBox.setToolTipText("The JVM you want generated code to be compatible with. ");
        methodPlsqlTextField.setToolTipText("The prefix used to distingusish methods that call PL/SQL from methods " +
                "that call SQL in the Implementation class. If you change this from " +
                "the default value method name collisions become possible.");
        methodSqlTextField.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                methodSqlTextField_actionPerformed(e);
            }
        });
        methodSqlTextField.setToolTipText("The prefix used to distingusish methods that call PL/SQL from methods " +
                "that call SQL in the Implementation class. If you change this from " +
                "the default value method name collisions become possible.");
        jLabel20.setFont(new java.awt.Font("Dialog", 1, 11));
        jLabel20.setText("Use Template:");
        genericTemplateComboBox.setFont(new java.awt.Font("Dialog", 1, 11));
        genericTemplateComboBox.setToolTipText("Pressing this button replaces the script with a generic batch script");
        // genericTemplateComboBox.setToolTipText("Generic Batch File");
        genericTemplateComboBox.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                genericTemplateButton_actionPerformed(e);
            }
        });
        menuFile.add(menuFileNew);
        menuFile.add(menuFileOpen);
        menuFile.add(menuFileSave);
        menuFile.add(menuFileSaveAs);
        menuFile.add(menuFileExit);
        menuHelp.add(menuHelpAbout);
        menuBar1.add(menuFile);

        // Look and Feel menu
        if (switchMenu != null) {
            menuBar1.add(switchMenu);
        }

        menuBar1.add(menuHelp);
        this.setJMenuBar(menuBar1);
        this.getContentPane().add(adminStatusBar, BorderLayout.SOUTH);
        this.getContentPane().add(adminTabbedPane, BorderLayout.CENTER);
        adminTabbedPane.add(loginPanel, "1. Log In");
        loginPanel.add(portLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,
                14, 0, 93), 54, 0));
        loginPanel.add(passwordLabel, new GridBagConstraints(0, 4, 1, 1, 0.0,
                0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 14, 0, 93), 26, 0));
        loginPanel.add(userLabel, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,
                14, 0, 93), 55, 0));
        loginPanel.add(SIDLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,
                14, 0, 93), 62, 0));
        loginPanel.add(hostLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(
                44, 14, 0, 93), 22, 0));
        loginPanel.add(connectButton, new GridBagConstraints(0, 5, 1, 1, 0.0,
                0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(5, 17, 46, 90), 32, 0));
        loginPanel.add(oraPassField, new GridBagConstraints(1, 4, 1, 1, 1.0,
                0.0, GridBagConstraints.SOUTHWEST,
                GridBagConstraints.HORIZONTAL, new Insets(0, -48, 0, 100), 83,
                0));
        loginPanel.add(connectionStatusField, new GridBagConstraints(1, 5, 1,
                1, 1.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(9, -48, 43, 100),
                120, 59));
        loginPanel.add(oraUserField, new GridBagConstraints(1, 3, 1, 1, 1.0,
                0.0, GridBagConstraints.SOUTHWEST,
                GridBagConstraints.HORIZONTAL, new Insets(0, -48, 0, 100), 85,
                0));
        loginPanel.add(portField, new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,
                GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL,
                new Insets(0, -48, 0, 100), 104, 0));
        loginPanel.add(sidField, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,
                GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL,
                new Insets(0, -48, 0, 100), 98, 0));
        loginPanel.add(hostnameField, new GridBagConstraints(1, 0, 1, 1, 1.0,
                0.0, GridBagConstraints.SOUTHWEST,
                GridBagConstraints.HORIZONTAL, new Insets(44, -48, 0, 100), 67,
                0));
        adminTabbedPane.add(selectObjectsPanel, "2. Select Objects");
        selectObjectsPanel.add(optionPanel, BorderLayout.NORTH);
        optionPanel.add(userOrAllLabel, new GridBagConstraints(0, 0, 1, 1, 0.0,
                0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(1, 9, 0, 0), 0, 0));
        optionPanel.add(otherCheckBox, new GridBagConstraints(2, 0, 1, 1, 0.0,
                0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(1, 0, 0, 0), 0, 0));
        optionPanel.add(meCheckBox, new GridBagConstraints(1, 0, 1, 1, 0.0,
                0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(1, 22, 0, 0), 0, 0));
        optionPanel.add(objectRefreshButton, new GridBagConstraints(4, 0, 1, 1,
                0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(1, 17, 0, 149), 0, 0));
        optionPanel.add(otherUserName, new GridBagConstraints(3, 0, 1, 1, 1.0,
                0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(1, 38, 0, 0), 170, 0));
        selectObjectsPanel.add(selectObjectsTabbedPane, BorderLayout.CENTER);
        selectObjectsTabbedPane.add(functionPanel,
                "2.2.1 PL/SQL Packages, Procedures and Functions");
        functionPanel.add(jPanel3, BorderLayout.SOUTH);
        functionPanel.add(functionTableScrollPane, BorderLayout.CENTER);
        functionPanel.add(jPanel4, BorderLayout.NORTH);
        jPanel4.add(selectAllFunctionsButton, null);
        jPanel4.add(selectNoFunctionsButton1, null);
        functionTableScrollPane.getViewport().add(functionTable, null);
        sequenceTableScrollPane.getViewport().add(sequenceTable, null);
        selectObjectsTabbedPane.add(sqlPanel, "2.2.2 Sql Statements");
        sqlPanel.add(sqlTablePanel, BorderLayout.SOUTH);
        sqlPanel.add(sqlTableScrollPane, BorderLayout.CENTER);
        sqlTableScrollPane.getViewport().add(sqlTableSpiltPane, null);
        sqlTableSpiltPane.add(sqlTableEditPanel, JSplitPane.RIGHT);
        sqlTableSpiltPane.add(sqlTableTreeScrollPane, JSplitPane.LEFT);
        sqlTableTreeScrollPane.getViewport().add(sqlTableTree, null);
        sqlPanel.add(sqlTableHeaderPanel, BorderLayout.NORTH);
        sqlTableHeaderPanel.add(aspDirFileChooserButton,
                new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.NONE,
                        new Insets(8, 17, 0, 109), 26, 3));
        sqlTableHeaderPanel.add(aspFileDirectorytextArea1,
                new GridBagConstraints(0, 0, 3, 1, 1.0, 1.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(10, 7, 0, 68), 0, 7));
        sqlTableHeaderPanel.add(selectNoSqlButton, new GridBagConstraints(1, 3,
                1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(10, 8, 14, 259), 0, 0));
        sqlTableHeaderPanel.add(aspFileDirectorytextArea,
                new GridBagConstraints(0, 2, 3, 1, 1.0, 1.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(15, 7, 0, 68), 0, 7));
        sqlTableHeaderPanel.add(selectAllSqlButton, new GridBagConstraints(0,
                3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(10, 7, 14, 0), 0, 0));
        sqlTableHeaderPanel.add(aspRootDirTextField, new GridBagConstraints(0,
                1, 2, 1, 1.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(9, 7, 0, 0), 496, 9));
        tableTableScrollPane.getViewport().add(tableTable, null);
        selectObjectsTabbedPane.add(tablePanel, "2.2.3 Tables");
        tablePanel.add(tableTablePanel, BorderLayout.SOUTH);
        tablePanel.add(tableTableScrollPane, BorderLayout.CENTER);
        tablePanel.add(jPanel5, BorderLayout.NORTH);
        jPanel5.add(selectAllTablesButton, null);
        jPanel5.add(selectNoTablesButton, null);

        selectObjectsTabbedPane.add(sequencePanel, "2.2.4 Sequences");
        sequencePanel.add(sequenceTablePanel, BorderLayout.SOUTH);
        sequencePanel.add(sequenceTableScrollPane, BorderLayout.CENTER);
        sequencePanel.add(jPanel2, BorderLayout.NORTH);
        jPanel2.add(selectAllSequencesButton, null);
        jPanel2.add(selectNoSequencesButton, null);

        codeOptionsPanel
                .add(authorTextField, new GridBagConstraints(0, 1, 2, 1, 1.0,
                        0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL, new Insets(7, 80, 0, 8),
                        251, 4));
        //	codeOptionsPanel
        //		.add(authorVersionTextArea2, new GridBagConstraints(2, 1, 4, 1,
        //				1.0, 1.0, GridBagConstraints.CENTER,
        //			GridBagConstraints.HORIZONTAL,
        //			new Insets(11, 10, 0, 11), 1, 5));

        adminTabbedPane.add(codeOptionsPanel, "3. Code Options");
        adminTabbedPane.add(fileOptionsPanel, "4. Service Options");
        fileOptionsPanel
                .add(codeRootDirTextField, new GridBagConstraints(0, 1, 1, 1,
                        1.0, 1.0, GridBagConstraints.NORTH,
                        GridBagConstraints.HORIZONTAL, new Insets(8, 14, 0, 0),
                        300, 0));
        fileOptionsPanel
                .add(packageNameTextArea, new GridBagConstraints(0, 2, 2, 1,
                        1.0, 0.0, GridBagConstraints.WEST,
                        GridBagConstraints.HORIZONTAL,
                        new Insets(0, 14, 2, 21), 0, 0));
        fileOptionsPanel.add(directoriesMessedWithtextArea1,
                new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(6, 14, 0, 21), 105, 0));
        fileOptionsPanel.add(packageNameTextField1,
                new GridBagConstraints(0, 3, 2, 1, 1.0, 1.0,
                        GridBagConstraints.NORTH,
                        GridBagConstraints.HORIZONTAL,
                        new Insets(6, 14, 0, 21), 300, 0));
        fileOptionsPanel.add(codeRootFileChooserButton, new GridBagConstraints(
                1, 1, 1, 1, 0.0, 1.0, GridBagConstraints.NORTH,
                GridBagConstraints.NONE, new Insets(8, 10, 4, 21), 0, 2));
        fileOptionsPanel.add(fileOptionsPanelExtraPanel,
                new GridBagConstraints(0, 4, 2, 1, 1.0, 1.0,
                        GridBagConstraints.SOUTHWEST, GridBagConstraints.BOTH,
                        new Insets(1, 14, 8, 20), 0, 0));
        fileOptionsPanelExtraPanel.add(daoFactoryClassNameLabel,
                new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(5, 0, 0, 0), 24, 2));
        fileOptionsPanelExtraPanel.add(daoFactoryNameTextField,
                new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(5, 0, 0, 0), 167, 2));
        fileOptionsPanelExtraPanel.add(logTypeLabel, new GridBagConstraints(0,
                2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(5, 0, 0, 0), 32, 0));
        fileOptionsPanelExtraPanel.add(logTypeComboBox, new GridBagConstraints(
                1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(5, 0, 0, 0), 167, 0));
        fileOptionsPanelExtraPanel.add(connectionTypeLabel,
                new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                        new Insets(5, 0, 0, 0), 14, 9));
        fileOptionsPanelExtraPanel.add(connectionTypeComboBox,
                new GridBagConstraints(1, 4, 1, 1, 1.0, 0.0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                        new Insets(5, 0, 0, 0), 167, 2));
        fileOptionsPanelExtraPanel.add(connectionNameTextField,
                new GridBagConstraints(1, 5, 5, 1, 0.0, 0.0,
                        GridBagConstraints.NORTHWEST,
                        GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0),
                        0, 0));
        fileOptionsPanelExtraPanel.add(logNameLabel, new GridBagConstraints(0,
                3, 2, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(5, 0, 0, 0), 14, 2));
        fileOptionsPanelExtraPanel.add(connectionNameLabel,
                new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                        new Insets(5, 0, 0, 0), 14, 0));
        fileOptionsPanelExtraPanel.add(generateSessionBeanCheckBox,
                new GridBagConstraints(3, 1, 4, 1, 0.0, 0.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(2, 13, 0, 0), 13, 1));
        fileOptionsPanelExtraPanel.add(logNameTextField1,
                new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                        new Insets(5, 1, 0, 0), 167, 2));
        fileOptionsPanelExtraPanel.add(jLabel8, new GridBagConstraints(3, 4, 1,
                1, 0.0, 0.0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.BOTH, new Insets(11, 13, 0, 0), 0, 0));
        fileOptionsPanelExtraPanel.add(addFinalizeMethodCheckBox,
                new GridBagConstraints(3, 2, 2, 1, 0.0, 0.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(2, 13, 0, 0), 0, 0));
        fileOptionsPanelExtraPanel.add(jLabel10, new GridBagConstraints(3, 3,
                1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.NONE, new Insets(5, 13, 0, 0), 0, 0));
        fileOptionsPanelExtraPanel.add(tempDirTextField,
                new GridBagConstraints(4, 3, 3, 1, 0.0, 0.0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                        new Insets(5, 5, 0, 0), 12, 0));
        fileOptionsPanelExtraPanel.add(jLabel11, new GridBagConstraints(3, 4,
                1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.NONE, new Insets(11, 13, 0, 0), 0, 0));
        fileOptionsPanelExtraPanel.add(tempFilePrefixTextField,
                new GridBagConstraints(4, 4, 1, 1, 0.0, 0.0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                        new Insets(11, 5, 0, 0), 50, 0));
        fileOptionsPanelExtraPanel.add(jLabel12, new GridBagConstraints(5, 4,
                1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(11, 2, 0, 4), 0, 0));
        fileOptionsPanelExtraPanel.add(tempFileSuffixTextField,
                new GridBagConstraints(6, 4, 1, 1, 0.0, 0.0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                        new Insets(11, 0, 0, 0), 40, 0));

        //createWSCheckBox.setMinimumSize(new Dimension(200,13));
        fileOptionsPanelExtraPanel.add(createWSCheckBox,
                new GridBagConstraints(0, 7, 2, 1, 0.0, 0.0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                        new Insets(5, 0, 0, 0), 0, 0));

        fileOptionsPanelExtraPanel.add(mcpServerCheckBox,
                new GridBagConstraints(3, 7, 4, 1, 0.0, 0.0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                        new Insets(5, 0, 0, 0), 0, 0));

        fileOptionsPanelExtraPanel
                .add(jTextArea1, new GridBagConstraints(0, 6, 7, 1, 1.0, 0.0,
                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                        new Insets(3, 0, 0, 0), 706, 0));
        fileOptionsPanelExtraPanel.add(jLabel9, new GridBagConstraints(0, 8, 2,
                1, 0.0, 0.0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.NONE, new Insets(5, 0, 0, 0), 0, 2));
        fileOptionsPanelExtraPanel.add(jLabel13, new GridBagConstraints(0, 9,
                3, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.NONE, new Insets(5, 0, 0, 0), 0, 2));
        fileOptionsPanelExtraPanel.add(jLabel13B, new GridBagConstraints(3, 9,
                3, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.NONE, new Insets(5, 13, 0, 0), 0, 2));
        fileOptionsPanelExtraPanel.add(wsInterfaceClassTextField,
                new GridBagConstraints(1, 8, 1, 1, 0.0, 0.0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                        new Insets(5, 0, 0, 0), 225, 0));
        fileOptionsPanelExtraPanel.add(wsImplClassTextField,
                new GridBagConstraints(1, 9, 1, 1, 0.0, 0.0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                        new Insets(5, 0, 0, 0), 167, 0));
        fileOptionsPanelExtraPanel.add(wsPreCallCheckBox,
                new GridBagConstraints(3, 8, 2, 1, 0.0, 0.0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                        new Insets(5, 13, 0, 10), 2, 0));
        fileOptionsPanelExtraPanel.add(wsPostCallCheckBox,
                new GridBagConstraints(4, 8, 3, 1, 0.0, 0.0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                        new Insets(5, 13, 0, 0), 0, 0));
        fileOptionsPanelExtraPanel.add(wsAlwaysReleaseCheckBox,
                new GridBagConstraints(3, 7, 4, 1, 0.0, 0.0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                        new Insets(5, 13, 0, 0), 2, 0));
        fileOptionsPanelExtraPanel.add(closeConnCheckBox,
                new GridBagConstraints(6, 1, 1, 1, 0.0, 0.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(0, 0, 0, 0), 0, 0));
        fileOptionsPanelExtraPanel.add(commitConnCheckBox,
                new GridBagConstraints(6, 2, 1, 1, 0.0, 0.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        new Insets(0, 0, 0, 0), 0, 0));
        fileOptionsPanelExtraPanel.add(directoriesMessedWithtextArea2,
                new GridBagConstraints(0, 0, 7, 1, 1.0, 1.0,
                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                        new Insets(1, 0, 0, 0), 241, 0));
        fileOptionsPanelExtraPanel.add(javaNumberTypeComboBox,
                new GridBagConstraints(1, 10, 1, 1, 0.0, 0.0,
                        GridBagConstraints.NORTHWEST,
                        GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0),
                        0, 0));
        fileOptionsPanelExtraPanel.add(wsRecTypeComboBox,
                new GridBagConstraints(4, 9, 4, 1, 0.0, 0.0,
                        GridBagConstraints.NORTHWEST,
                        GridBagConstraints.NONE, new Insets(5, 0, 0, 0),
                        0, 0));
        fileOptionsPanelExtraPanel.add(jLabel14, new GridBagConstraints(0, 10,
                1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.NONE, new Insets(5, 0, 0, 10), 0, 0));

        fileOptionsPanelExtraPanel.add(xwsLabel, new GridBagConstraints(0, 11,
                1, 2, 0.0, 0.0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.NONE, new Insets(5, 0, 0, 0), 0, 0));

        fileOptionsPanelExtraPanel.add(xwsTypeName, new GridBagConstraints(1, 11,
                1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.NONE, new Insets(5, 0, 0, 0), 0, 0));

        fileOptionsPanelExtraPanel.add(xwsImplLabel, new GridBagConstraints(3, 11,
                1, 1, 0.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                new Insets(5, 13, 0, 0), 0, 0));

        fileOptionsPanelExtraPanel.add(xwsImpl, new GridBagConstraints(4, 11,
                6, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL, new Insets(5, 2, 0, 0), 0, 0));

        fileOptionsPanelExtraPanel.add(xwsIfaceLabel, new GridBagConstraints(3, 12,
                1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.NONE, new Insets(5, 13, 0, 0), 0, 0));

        fileOptionsPanelExtraPanel.add(xwsIface,
                new GridBagConstraints(4, 12, 6, 1, 0.0, 0.0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                        new Insets(5, 2, 0, 0), 0, 0));


        outputPanel.add(outputLogPanel, BorderLayout.CENTER);
        outputLogPanel.add(outputAreaScrollPane, BorderLayout.CENTER);
        outputAreaScrollPane.getViewport().add(guiLogTextArea, null);
        outputPanel.add(outputButtonPanel, BorderLayout.NORTH);
        outputButtonPanel.add(generateCodeButton, new GridBagConstraints(0, 0,
                1, 2, 0.0, 0.0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.NONE, new Insets(10, 30, 33, 0), 19, 0));
        outputButtonPanel.add(jLabel15, new GridBagConstraints(6, 0, 1, 1, 0.0,
                0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        outputButtonPanel.add(jLabel16, new GridBagConstraints(5, 0, 1, 1, 1.0,
                0.0, GridBagConstraints.NORTHEAST,
                GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 0), 19, 0));
        // Pooling sits immediately after the Service Options page whose connection settings it
        // changes the meaning of. Inserting here renumbers the two tabs below it but NOT tab 4,
        // which matters: emitted javadoc refers to "step 4.3" in several places.
        adminTabbedPane.add(poolOptionsPanel, "5. Pooling");
        adminTabbedPane.add(postOptionsPanel, "6. Extra Options");
        postOptionsPanel.add(jLabel17, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        postOptionsPanel.add(postScriptNameTextField, new GridBagConstraints(2, 7, 9, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(7, 0, 0, 0), 200, 0));
        postOptionsPanel.add(jLabel18, new GridBagConstraints(1, 7, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(8, 14, 0, 21), 0, 0));
        postOptionsPanel.add(jLabel19, new GridBagConstraints(1, 8, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(8, 14, 0, 21), 0, 0));
        postOptionsPanel.add(postScriptDescrTextArea, new GridBagConstraints(0, 6, 13, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(8, 14, 0, 21), 79, 8));
        postOptionsPanel.add(postDescrTextArea, new GridBagConstraints(0, 0, 13, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(8, 14, 0, 21), 63, 0));
        postOptionsPanel.add(postScriptScrollPane, new GridBagConstraints(1, 10, 13, 8, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(8, 14, 6, 21), 0, 0));
        postOptionsPanel.add(jScrollPane4, new GridBagConstraints(0, 1, 13, 4, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(8, 14, 6, 21), 100, 50));
        postOptionsPanel.add(jLabel20, new GridBagConstraints(1, 19, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(8, 14, 0, 21), 0, 0));
        postOptionsPanel.add(genericTemplateComboBox, new GridBagConstraints(8, 19, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(8, 14, 0, 21), 0, 0));
        jScrollPane4.getViewport().add(extraCodeTextArea, null);
        postScriptScrollPane.getViewport().add(postScriptCodeTextArea, null);
        //postOptionsPanel.add(postDescrTextArea, new GridBagConstraints(0, 0, 4, 1, 0.0, 0.0
        //        ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(8, 14, 0, 21), 767, 15));
        codeOptionsPanel.add(authorLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(9, 10, 0, 0), 22, 5));
        codeOptionsPanel.add(versionLabel, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(9, 50, 0, 0), 25, 5));
        codeOptionsPanel.add(commentTextArea, new GridBagConstraints(0, 2, 7, 1, 1.0, 1.0
                , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(7, 80, 0, 11), 395, 5));
        codeOptionsPanel.add(commentLabel1, new GridBagConstraints(0, 2, 2, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(13, 10, 0, 180), 11, 9));
        codeOptionsPanel.add(jLabel5, new GridBagConstraints(0, 13, 2, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 9), 65, 3));
        codeOptionsPanel.add(jLabel6, new GridBagConstraints(2, 4, 5, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(7, 5, 0, 0), 30, 11));
        codeOptionsPanel.add(useIntCB, new GridBagConstraints(2, 7, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 10), 19, 3));
        codeOptionsPanel.add(useShortCB, new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 10), 4, 3));
        codeOptionsPanel.add(useLongCB, new GridBagConstraints(2, 8, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 10), 8, 3));
        codeOptionsPanel.add(useFloatCB, new GridBagConstraints(2, 9, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 10), 9, 3));
        codeOptionsPanel.add(useDoubleCB, new GridBagConstraints(2, 10, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 10), 12, 3));
        codeOptionsPanel.add(useByteCB, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 10), 10, 3));
        codeOptionsPanel.add(useIntOCB, new GridBagConstraints(3, 7, 2, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 50, 3));
        codeOptionsPanel.add(useShortOCB, new GridBagConstraints(3, 6, 4, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 50, 3));
        codeOptionsPanel.add(useLongOCB, new GridBagConstraints(3, 8, 2, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 50, 3));
        codeOptionsPanel.add(useFloatOCB, new GridBagConstraints(3, 9, 3, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 50, 3));
        codeOptionsPanel.add(useByteOCB, new GridBagConstraints(3, 5, 2, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 50, 3));
        codeOptionsPanel.add(authorVersionTextArea4, new GridBagConstraints(0, 0, 7, 1, 1.0, 1.0
                , GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 11), 1, 5));
        codeOptionsPanel.add(jLabel7, new GridBagConstraints(2, 11, 5, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 249), 107, 11));
        codeOptionsPanel.add(validateCB, new GridBagConstraints(2, 12, 5, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 10), 77, 3));
        codeOptionsPanel.add(oracleVersionComboBox, new GridBagConstraints(0, 14, 2, 1, 1.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 20), 100, 1));
        codeOptionsPanel.add(javaNamingConventionComboBox, new GridBagConstraints(0, 12, 2, 1, 1.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 1), 10, 1));
        codeOptionsPanel.add(jLabel4, new GridBagConstraints(0, 11, 2, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 7), 0, 11));
        codeOptionsPanel.add(jLabel3, new GridBagConstraints(0, 9, 2, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 55, 11));
        codeOptionsPanel.add(commentsCheckBox, new GridBagConstraints(0, 8, 2, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 30), 30, 3));
        codeOptionsPanel.add(jLabel2, new GridBagConstraints(0, 7, 2, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 2, 7), 40, 11));
        codeOptionsPanel.add(jLabel1, new GridBagConstraints(0, 4, 2, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(7, 10, 0, 0), 25, 11));
        codeOptionsPanel.add(buildOptionsTextArea, new GridBagConstraints(0, 3, 7, 1, 1.0, 1.0
                , GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(7, 10, 0, 11), 1, 5));
        codeOptionsPanel.add(otherMessagesCheckbox, new GridBagConstraints(0, 6, 2, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 75), 6, 3));
        codeOptionsPanel.add(versionTextField, new GridBagConstraints(4, 1, 3, 1, 1.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(7, 36, 0, 132), 186, 4));
        codeOptionsPanel.add(extraSqlCheckBox, new GridBagConstraints(2, 13, 4, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 30, 0));
        codeOptionsPanel.add(debugMessagesCheckbox, new GridBagConstraints(0, 5, 2, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 64), 69, 3));
        codeOptionsPanel.add(useDoubleOCB, new GridBagConstraints(3, 10, 4, 1, 0.0, 0.0
                , GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 50, 3));
        codeOptionsPanel.add(jvmLabel, new GridBagConstraints(4, 4, 2, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(7, 40, 0, 0), 0, 0));
        codeOptionsPanel.add(jvmComboBox, new GridBagConstraints(4, 5, 2, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 40, 0, 0), 43, 0));
        codeOptionsPanel.add(plsqlLabel, new GridBagConstraints(4, 6, 2, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 40, 0, 0), 0, 0));
        codeOptionsPanel.add(sqlLabel, new GridBagConstraints(4, 8, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 40, 0, 0), 0, 0));
        codeOptionsPanel.add(methodPlsqlTextField, new GridBagConstraints(4, 7, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 40, 0, 0), 120, 0));
        codeOptionsPanel.add(methodSqlTextField, new GridBagConstraints(4, 9, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 40, 0, 0), 120, 0));
        codeOptionsPanel.add(statsCheckBox1, new GridBagConstraints(0, 10, 2, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 6), 0, 3));
        adminTabbedPane.add(outputPanel, "7. Generate Code");

        xwsTypeName.setMinimumSize(new Dimension(100, 21));
        xwsImpl.setMinimumSize(new Dimension(125, 21));
        xwsIface.setMinimumSize(new Dimension(125, 21));

    }

    /**
     * Build the "5. Pooling" tab: a master checkbox, the four sizes, the settle-on-return policy,
     * and a blurb explaining what the numbers cost on the database server.
     *
     * <p>Everything below the checkbox is disabled while pooling is off, so the page cannot be
     * filled in under the impression that it is doing something.
     */
    private void setUpPoolOptionsTab() {
        poolOptionsPanel.setLayout(gridBagLayoutPool);
        poolOptionsPanel.setFont(new java.awt.Font("Dialog", 0, 11));
        poolOptionsPanel.setMinimumSize(new Dimension(800, 500));
        poolOptionsPanel.setPreferredSize(new Dimension(800, 500));

        daoPoolCheckBox.setText("Pool DAO factory objects");
        daoPoolCheckBox.setFont(new java.awt.Font("Dialog", 1, 11));
        daoPoolCheckBox.setToolTipText("Generate a pool of ready-to-use DAO factories so concurrent "
                + "callers do not queue on a single database connection");
        daoPoolCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enablePoolFields();
            }
        });

        daoPoolMaxSizeLabel.setText("Maximum pool size");
        daoPoolMaxSizeLabel.setFont(new java.awt.Font("Dialog", 1, 11));
        daoPoolMaxSizeTextField.setMinimumSize(new Dimension(80, 20));
        daoPoolMaxSizeTextField.setPreferredSize(new Dimension(80, 20));
        daoPoolMaxSizeTextField.setToolTipText("The pool never grows beyond this. Each factory holds "
                + "one Oracle session, so this is bounded by the server's SESSIONS and OPEN_CURSORS");

        daoPoolMinIdleLabel.setText("Factories kept warm");
        daoPoolMinIdleLabel.setFont(new java.awt.Font("Dialog", 1, 11));
        daoPoolMinIdleTextField.setMinimumSize(new Dimension(80, 20));
        daoPoolMinIdleTextField.setPreferredSize(new Dimension(80, 20));
        daoPoolMinIdleTextField.setToolTipText("The pool shrinks to this when idle. Zero gives every "
                + "session back; raise it to keep a few ready for the next burst");

        daoPoolMaxWaitLabel.setText("Wait for a factory (ms)");
        daoPoolMaxWaitLabel.setFont(new java.awt.Font("Dialog", 1, 11));
        daoPoolMaxWaitTextField.setMinimumSize(new Dimension(80, 20));
        daoPoolMaxWaitTextField.setPreferredSize(new Dimension(80, 20));
        daoPoolMaxWaitTextField.setToolTipText("How long a caller waits when every factory is busy "
                + "before the call is refused as server-busy");

        daoPoolIdleTimeoutLabel.setText("Close after idle (ms)");
        daoPoolIdleTimeoutLabel.setFont(new java.awt.Font("Dialog", 1, 11));
        daoPoolIdleTimeoutTextField.setMinimumSize(new Dimension(80, 20));
        daoPoolIdleTimeoutTextField.setPreferredSize(new Dimension(80, 20));
        daoPoolIdleTimeoutTextField.setToolTipText("A factory unused for this long is closed and its "
                + "session returned, down to the number kept warm");

        daoPoolOnReturnLabel.setText("On returning a factory");
        daoPoolOnReturnLabel.setFont(new java.awt.Font("Dialog", 1, 11));
        daoPoolOnReturnComboBox.setFont(new java.awt.Font("Dialog", 0, 11));
        daoPoolOnReturnComboBox.setToolTipText("What to do with the caller's transaction when it "
                + "finishes. A call that fails is always rolled back whatever this says");

        daoPoolBlurbTextArea.setText(
                "Pooling gives each concurrent caller its own DAO factory instead of making them "
                        + "queue on one shared database connection.\n\n"
                        + "A pooled factory keeps its connection AND its already-parsed statements, "
                        + "which is why this pools factories rather than connections — a "
                        + "connection pool would hand back a bare connection and every caller would "
                        + "re-parse.\n\n"
                        + "The cost is server-side: each factory in the pool holds one Oracle session "
                        + "and the cursors of every DAO it has used. Size the maximum against the "
                        + "server's SESSIONS and OPEN_CURSORS, not against how many CPUs you have. "
                        + "If you run several generated servers against one database, it is the TOTAL "
                        + "across them that has to fit.\n\n"
                        + "Note that pooling changes when work is committed: the transaction now ends "
                        + "when a caller finishes, not when the connection is released. Leave pooling "
                        + "off if code you already ship relies on the old timing.\n\n"
                        + "Any of these values can be overridden where the generated code runs, "
                        + "without regenerating, using the matching DAO_POOL_* environment variable.");
        daoPoolBlurbTextArea.setFont(new java.awt.Font("Dialog", 0, 11));
        daoPoolBlurbTextArea.setLineWrap(true);
        daoPoolBlurbTextArea.setWrapStyleWord(true);
        daoPoolBlurbTextArea.setEditable(false);
        daoPoolBlurbTextArea.setOpaque(false);

        poolOptionsPanel.add(daoPoolCheckBox, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                new Insets(15, 15, 10, 0), 0, 0));

        addPoolRow(daoPoolMaxSizeLabel, daoPoolMaxSizeTextField, 1);
        addPoolRow(daoPoolMinIdleLabel, daoPoolMinIdleTextField, 2);
        addPoolRow(daoPoolMaxWaitLabel, daoPoolMaxWaitTextField, 3);
        addPoolRow(daoPoolIdleTimeoutLabel, daoPoolIdleTimeoutTextField, 4);
        addPoolRow(daoPoolOnReturnLabel, daoPoolOnReturnComboBox, 5);

        poolOptionsPanel.add(daoPoolBlurbTextArea, new GridBagConstraints(0, 6, 2, 1, 1.0, 1.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH,
                new Insets(20, 15, 15, 15), 0, 0));

        enablePoolFields();
    }

    /** One "label: control" line on the Pooling tab. */
    private void addPoolRow(JLabel theLabel, java.awt.Component theControl, int theRow) {
        poolOptionsPanel.add(theLabel, new GridBagConstraints(0, theRow, 1, 1, 0.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                new Insets(5, 30, 0, 10), 0, 0));
        poolOptionsPanel.add(theControl, new GridBagConstraints(1, theRow, 1, 1, 0.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                new Insets(5, 0, 0, 0), 0, 0));
    }

    /** Grey out the pool settings unless pooling is actually on. */
    private void enablePoolFields() {
        boolean isOn = daoPoolCheckBox.isSelected();

        daoPoolMaxSizeLabel.setEnabled(isOn);
        daoPoolMaxSizeTextField.setEnabled(isOn);
        daoPoolMinIdleLabel.setEnabled(isOn);
        daoPoolMinIdleTextField.setEnabled(isOn);
        daoPoolMaxWaitLabel.setEnabled(isOn);
        daoPoolMaxWaitTextField.setEnabled(isOn);
        daoPoolIdleTimeoutLabel.setEnabled(isOn);
        daoPoolIdleTimeoutTextField.setEnabled(isOn);
        daoPoolOnReturnLabel.setEnabled(isOn);
        daoPoolOnReturnComboBox.setEnabled(isOn);
    }

    // File | Exit action performed
    public void fileExit_actionPerformed(ActionEvent e) {
        exitGUI();
        if (mrApplicationShell != null
                && mrApplicationShell.getShutdownOnExit()) {
            System.exit(0);
        } else {
            this.dispose();
        }

    }

    // File | New action performed
    public void fileNew_actionPerformed(ActionEvent e) {
        createChoosers();
        int option = chooser.showOpenDialog(this);

        if (option == JFileChooser.APPROVE_OPTION) {
            mrApplicationShell.disconnectDB(hostnameField, portField, sidField,
                    oraUserField, oraPassField, connectButton,
                    connectionStatusField, CONNECT_BUTTON_TEXT);
            setNewMode(LOGIN_MODE);
            String newFileName = chooser.getSelectedFile().getAbsolutePath();

            if (!newFileName.endsWith(FILE_EXTENSION)) {
                newFileName = newFileName + FILE_EXTENSION;
            }

            // mrApplicationShell.newIniFile(chooser.getSelectedFile());
            mrApplicationShell.newIniFile(new java.io.File(newFileName));
            fillInDefaults();
        }
    }

    // File | Open action performed
    public void fileOpen_actionPerformed(ActionEvent e) {
        createChoosers();
        int option = chooser.showOpenDialog(this);

        if (option == JFileChooser.APPROVE_OPTION) {
            mrApplicationShell.disconnectDB(hostnameField, portField, sidField,
                    oraUserField, oraPassField, connectButton,
                    connectionStatusField, CONNECT_BUTTON_TEXT);
            setNewMode(LOGIN_MODE);
            mrApplicationShell.openIniFile(chooser.getSelectedFile());
            fillInDefaults();
        }
    }

    // File | Save action performed
    public void fileSave_actionPerformed(ActionEvent e) {
        saveNewDefaults();
    }

    // File | SaveAs action performed
    public void fileSaveAs_actionPerformed(ActionEvent e) {

        createChoosers();
        int option = chooser.showOpenDialog(this);

        if (option == JFileChooser.APPROVE_OPTION) {
            java.io.File newFile = chooser.getSelectedFile();
            if (!newFile.getAbsolutePath().endsWith(FILE_EXTENSION)) {
                // System.out.println("Extension '" + FILE_EXTENSION + "' being
                // added to " + newFile.getAbsolutePath());
                newFile = new java.io.File(newFile.getAbsolutePath()
                        + FILE_EXTENSION);
            }

            mrApplicationShell.saveAsIniFile(newFile);
            saveNewDefaults();
        }

    }

    // Help | About action performed
    public void helpAbout_actionPerformed(ActionEvent e) {
        ThingAdministratorFrame_AboutBox dlg = new ThingAdministratorFrame_AboutBox(
                this);
        Dimension dlgSize = dlg.getPreferredSize();
        Dimension frmSize = getSize();
        Point loc = getLocation();
        dlg.setLocation((frmSize.width - dlgSize.width) / 2 + loc.x,
                (frmSize.height - dlgSize.height) / 2 + loc.y);
        dlg.setModal(true);
        dlg.show();
    }

    // Overridden so we can exit on System Close
    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            fileExit_actionPerformed(null);
        }
    }

    public void connect() {

        if ((connectButton.getText()).equals(CONNECT_BUTTON_TEXT)) {
            connectButton_actionPerformed(null);
        }
    }

    void connectButton_actionPerformed(ActionEvent e2) {
        if ((connectButton.getText()).equals(CONNECT_BUTTON_TEXT)) {

            try {
                java.io.File warnFile = new java.io.File(
                        mrApplicationShell.logFileDirectory
                                + java.io.File.separator + "warn.txt");
                if (warnFile.exists()) {
                    mrApplicationShell
                            .info(
                                    "                            Logging in to the database can take up to 20 seconds\n"
                                            + Namer.param_prod_name + " has to create a list of every stored procedure and table you can see once logged in\n"
                                            + "               The status line on the bottom of the screen shows what " + Namer.param_prod_name + " is doing",
                                    true, true);
                    // warnFile.delete();
                }
            } catch (Exception efoo) {
            }

            mrApplicationShell.info("Attempting to Connect...");
            mrApplicationShell.connectDB(hostnameField, portField, sidField,
                    oraUserField, oraPassField, connectButton,
                    connectionStatusField, DISCONNECT_BUTTON_TEXT,
                    sequenceTable, connectionNameTextField);

            if (mrApplicationShell.areConnected) {
                mrApplicationShell.setOutputTextArea(guiLogTextArea);
                mrApplicationShell.info("Connected to Oracle Version "
                        + mrApplicationShell.getVersion(), false, false);


                String oldAspDirName = aspRootDirTextField.getText();

                // Ask plugin to translate ec file name in case it is one///
                McpDbWizardEvent event = new McpDbWizardEvent(
                        McpDbWizardEvent.NEED_EC_TO_OS_TRANSLATION);
                event.setThing(aspRootDirTextField.getText());
                mrApplicationShell.setGenericString(null);
                mrApplicationShell.reportMcpDbWizardEvent(event);
                String hintString = mrApplicationShell.getGenericString();

                if (!(hintString == null)) {
                    aspRootDirTextField.setText(hintString);
                }


                mrApplicationShell.refreshTables(meCheckBox, oraUserField,
                        otherCheckBox, otherUserName, sequenceTable,
                        sequenceTableScrollPane.getHeight(), tableTable,
                        tableTableScrollPane.getHeight(), functionTable,
                        functionTableScrollPane.getHeight(), sqlTableTree,
                        aspRootDirTextField, sqlTableSpiltPane,
                        sqlTableTreeScrollPane);


                if (!(hintString == null)) {
                    aspRootDirTextField.setText(oldAspDirName);
                }

                mrApplicationShell.info("Ready", false, false);
                setNewMode(LOGGED_IN);

                try {
                    java.io.File warnFile = new java.io.File(
                            mrApplicationShell.logFileDirectory
                                    + java.io.File.separator + "warn.txt");
                    if (warnFile.exists()) {
                        mrApplicationShell
                                .info(
                                        "                 You are now logged in.\n"
                                                + "Use the Tabs at the top of the screen to navigate",
                                        true, true);
                        warnFile.delete();
                    }
                } catch (Exception efoo) {
                }
            }

        } else {
            mrApplicationShell.disconnectDB(hostnameField, portField, sidField,
                    oraUserField, oraPassField, connectButton,
                    connectionStatusField, CONNECT_BUTTON_TEXT);
            setNewMode(LOGIN_MODE);

        }
    }

    public void setLoginFields(boolean enabled) {
        hostnameField.setEnabled(enabled);
        portField.setEnabled(enabled);
        sidField.setEnabled(enabled);
        oraUserField.setEnabled(enabled);
        oraPassField.setEnabled(enabled);

    }

    void this_windowClosing(WindowEvent e) {
        shutDownGUI();
    }

    void setAdminLabel(String pMsg, int pImportance) {
        adminStatusBar.setText(pMsg);
        this.paintAll(this.getGraphics());
    }

    public void saveNewDefaults() {
        xYSize = this.getSize();

        // Fill in values from ini file...
        mrApplicationShell.saveNewDefaults(hostnameField, portField, sidField,
                oraUserField, oraPassField, xYSize, authorTextField,
                versionTextField, commentTextArea, meCheckBox, otherCheckBox,
                otherUserName, codeRootDirTextField, packageNameTextField1,
                oracleVersionComboBox, debugMessagesCheckbox,
                otherMessagesCheckbox, commentsCheckBox, statsCheckBox1,
                javaAccessComboBox, javaNamingConventionComboBox,
                aspRootDirTextField, useByteCB, useShortCB, useIntCB,
                useLongCB, useFloatCB, useDoubleCB, useByteOCB, useShortOCB,
                useIntOCB, useLongOCB, useFloatOCB, useDoubleOCB, validateCB,
                extraSqlCheckBox, daoFactoryNameTextField,
                generateSessionBeanCheckBox, logTypeComboBox,
                logNameTextField1, connectionTypeComboBox,
                connectionNameTextField, createWSCheckBox, mcpServerCheckBox,
                addFinalizeMethodCheckBox, tempDirTextField,
                tempFilePrefixTextField, tempFileSuffixTextField,
                xwsTypeName,
                xwsImpl,
                xwsIface,
                wsImplClassTextField, wsInterfaceClassTextField,
                wsPreCallCheckBox, wsPostCallCheckBox, wsAlwaysReleaseCheckBox,
                closeConnCheckBox,
                commitConnCheckBox,
                daoPoolCheckBox, daoPoolMaxSizeTextField, daoPoolMinIdleTextField,
                daoPoolMaxWaitTextField, daoPoolIdleTimeoutTextField, daoPoolOnReturnComboBox,
                javaNumberTypeComboBox, wsRecTypeComboBox, jvmComboBox
                , methodPlsqlTextField
                , methodSqlTextField
                , postScriptNameTextField
                , postScriptCodeTextArea
                , extraCodeTextArea);
    }

    void shutDownGUI() {
        saveNewDefaults();

        if (mrApplicationShell.currentlyConnected()) {
            mrApplicationShell.disconnectDB(hostnameField, portField, sidField,
                    oraUserField, oraPassField, connectButton,
                    connectionStatusField, DISCONNECT_BUTTON_TEXT);
        }

    }

    void exitGUI() {

        if (mrApplicationShell.currentlyConnected()) {
            mrApplicationShell.disconnectDB(hostnameField, portField, sidField,
                    oraUserField, oraPassField, connectButton,
                    connectionStatusField, DISCONNECT_BUTTON_TEXT);
        }

    }

    void turnAllTabsOff() {
        // masterTabbedPane.setEnabled(false);
        adminTabbedPane.setEnabled(false);
        loginPanel.setEnabled(false);
    }

    public void setNewMode(int newMode) {
        mrApplicationShell.debug("Disabling Existing Panels");

        turnAllTabsOff();

        if (newMode == PRE_LOGIN) {
            mrApplicationShell.debug("Entering LOGIN mode");
            loginPanel.setEnabled(true);
            currentMode = LOGIN_MODE;
        } else if (newMode == LOGIN_MODE) {
            try {
                if (adminTabbedPane.getTabCount() > 0) {
                    adminTabbedPane.setSelectedIndex(0);
                }
            } catch (Exception e) {
                System.out.println(e);
            }

        } else if (newMode == LOGGED_IN) {
            mrApplicationShell.debug("Entering LOGGED_IN mode");
            // masterTabbedPane.setEnabled(true);
            adminTabbedPane.setEnabled(true);
            loginPanel.setEnabled(true);
        }

        this.paintAll(this.getGraphics());
    }

    void generateCodeButton_actionPerformed(ActionEvent e) {

        mrApplicationShell.info("Validating Options");
        //String foo =          methodPlsqlTextField.getText();
        methodPlsqlTextField.setText(methodPlsqlTextField.getText());
        saveNewDefaults();
        //	fillInDefaults();


        mrApplicationShell.generateCodeFromGUI(codeRootDirTextField,
                packageNameTextField1, authorTextField, versionTextField,
                commentTextArea, aspRootDirTextField);
    }

    public void objectRefreshButton_actionPerformed(ActionEvent e) // DRKLUGE
    {

        String oldAspDirName = aspRootDirTextField.getText();

        // Ask plugin to translate ec file name in case it is one///
        McpDbWizardEvent event = new McpDbWizardEvent(
                McpDbWizardEvent.NEED_EC_TO_OS_TRANSLATION);
        event.setThing(aspRootDirTextField.getText());
        mrApplicationShell.setGenericString(null);
        mrApplicationShell.reportMcpDbWizardEvent(event);
        String hintString = mrApplicationShell.getGenericString();

        if (!(hintString == null)) {
            aspRootDirTextField.setText(hintString);
        }

        otherUserName.setText(otherUserName.getText().toUpperCase());
        mrApplicationShell.refreshTables(meCheckBox, oraUserField,
                otherCheckBox, otherUserName, sequenceTable,
                sequenceTableScrollPane.getHeight(), tableTable,
                tableTableScrollPane.getHeight(), functionTable,
                functionTableScrollPane.getHeight(), sqlTableTree,
                aspRootDirTextField, sqlTableSpiltPane, sqlTableTreeScrollPane);


        if (!(hintString == null)) {
            aspRootDirTextField.setText(oldAspDirName);
        }
    }

    void meCheckBox_actionPerformed(ActionEvent e) {
    }

    void selectAllSequencesButton_actionPerformed(ActionEvent e) {
        mrApplicationShell.selectAllSequences();
        this.paintAll(this.getGraphics());
        forceRefresh();

    }

    void selectNoSequencesButton_actionPerformed(ActionEvent e) {
        mrApplicationShell.selectNoSequences();
        this.paintAll(this.getGraphics());
        forceRefresh();
    }

    void selectAllTablesButton_actionPerformed(ActionEvent e) {
        mrApplicationShell.selectAllTables();
        this.paintAll(this.getGraphics());
        forceRefresh();
    }

    void selectNoTablesButton_actionPerformed(ActionEvent e) {
        mrApplicationShell.selectNoTables();
        this.paintAll(this.getGraphics());
        forceRefresh();
    }

    void debugMessagesCheckbox_actionPerformed(ActionEvent e) {

    }

    void otherMessagesCheckbox_actionPerformed(ActionEvent e) {

    }

    void oracleVersionComboBox_actionPerformed(ActionEvent e) {

    }

    void javaAccessComboBox_actionPerformed(ActionEvent e) {

    }

    void javaNamingConventionComboBox_actionPerformed(ActionEvent e) {

    }

    void selectAllFunctionsButton_actionPerformed(ActionEvent e) {
        mrApplicationShell.selectAllFunctions();
        this.paintAll(this.getGraphics());
        forceRefresh();
    }

    void selectNoFunctionsButton1_actionPerformed(ActionEvent e) {
        mrApplicationShell.selectNoFunctions();
        this.paintAll(this.getGraphics());
        forceRefresh();
    }

    void codeRootFileChooserButton_actionPerformed(ActionEvent e) {

        String oldValue = codeRootDirTextField.getText();

        // Ask plugin to translate ec file name in case it is one///
        McpDbWizardEvent event = new McpDbWizardEvent(
                McpDbWizardEvent.NEED_EC_TO_OS_TRANSLATION);
        event.setThing(codeRootDirTextField.getText());
        mrApplicationShell.setGenericString(null);
        mrApplicationShell.reportMcpDbWizardEvent(event);
        String hintString = mrApplicationShell.getGenericString();

        if (!(hintString == null)) {
            codeRootDirTextField.setText(hintString);
        }

        // Create a chooser.
        java.io.File dirFile = new java.io.File(codeRootDirTextField.getText());
        createChoosers(dirFile);

        // Show dialog....
        int option = dirChooser.showOpenDialog(this);

        if (option == JFileChooser.APPROVE_OPTION) {
            java.io.File tempFile = dirChooser.getSelectedFile();

            if (tempFile == null) {
                mrApplicationShell.info("No directory selected", true, true);
            } else {
                mrApplicationShell.info("Directory "
                        + tempFile.getAbsolutePath() + " selected", true, true);

                if (hintString != null) {

                    event = new McpDbWizardEvent(
                            McpDbWizardEvent.NEED_OS_TO_EC_TRANSLATION);
                    event.setThing(tempFile.getAbsolutePath());
                    mrApplicationShell.setGenericString(null);
                    mrApplicationShell.reportMcpDbWizardEvent(event);

                    String newDirName = mrApplicationShell.getGenericString();

                    if (newDirName == null) {
                        mrApplicationShell.error("Directory "
                                + tempFile.getAbsolutePath()
                                + " isn't in this project", true, true);
                    } else {
                        codeRootDirTextField.setText(newDirName);
                    }

                } else {
                    codeRootDirTextField.setText(tempFile.getAbsolutePath());
                }

            }
        } else {
            // put old value back fast....
            codeRootDirTextField.setText(oldValue);
        }
    }

    void createChoosers() {
        chooser = new JFileChooser();
        EndsWithFilter filter = new EndsWithFilter(FILE_EXTENSION,
                Namer.param_prod_name + " Files");
        IsDirFilter dirFilter = new IsDirFilter();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.addChoosableFileFilter(filter);

        dirChooser = new JFileChooser();
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        dirChooser.addChoosableFileFilter(dirFilter);
    }

    void createChoosers(java.io.File defaultDir) {
        chooser = new JFileChooser(defaultDir);
        EndsWithFilter filter = new EndsWithFilter(FILE_EXTENSION,
                Namer.param_prod_name + " Files");
        IsDirFilter dirFilter = new IsDirFilter();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.addChoosableFileFilter(filter);

        dirChooser = new JFileChooser(defaultDir);
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        dirChooser.addChoosableFileFilter(dirFilter);
    }

    void selectAllProcsButton_actionPerformed(ActionEvent e) {

    }

    void selectNoProcsButton_actionPerformed(ActionEvent e) {

    }

    public void fillInDefaults() {
        // Fill in values from ini file...
        mrApplicationShell.fillInDefaults(hostnameField, portField, sidField,
                oraUserField, oraPassField, xYSize, authorTextField,
                versionTextField, commentTextArea, meCheckBox, otherCheckBox,
                otherUserName, codeRootDirTextField, packageNameTextField1,
                oracleVersionComboBox, debugMessagesCheckbox,
                otherMessagesCheckbox, commentsCheckBox, statsCheckBox1,
                javaAccessComboBox, javaNamingConventionComboBox,
                aspRootDirTextField, useByteCB, useShortCB, useIntCB,
                useLongCB, useFloatCB, useDoubleCB, useByteOCB, useShortOCB,
                useIntOCB, useLongOCB, useFloatOCB, useDoubleOCB, validateCB,
                extraSqlCheckBox, daoFactoryNameTextField,
                generateSessionBeanCheckBox, logTypeComboBox,
                logNameTextField1, connectionTypeComboBox,
                connectionNameTextField, createWSCheckBox, mcpServerCheckBox,
                addFinalizeMethodCheckBox, tempDirTextField,
                tempFilePrefixTextField, tempFileSuffixTextField,
                xwsTypeName,
                xwsImpl,
                xwsIface,
                wsImplClassTextField, wsInterfaceClassTextField,
                wsPreCallCheckBox, wsPostCallCheckBox, wsAlwaysReleaseCheckBox,
                closeConnCheckBox,
                commitConnCheckBox,
                daoPoolCheckBox, daoPoolMaxSizeTextField, daoPoolMinIdleTextField,
                daoPoolMaxWaitTextField, daoPoolIdleTimeoutTextField, daoPoolOnReturnComboBox,
                javaNumberTypeComboBox, wsRecTypeComboBox
                , jvmComboBox
                , methodPlsqlTextField
                , methodSqlTextField
                , postScriptNameTextField
                , postScriptCodeTextArea
                , extraCodeTextArea);

        // The checkbox was just set from the config rather than clicked, so its listener did not
        // fire; without this, loading a pooling config shows the settings greyed out.
        enablePoolFields();

        this.setSize(xYSize);
        this.invalidate();
        this.validate();
        this.repaint();

    }

    void authorTextField_actionPerformed(ActionEvent e) {

    }

    public JScrollPane getOutputAreaScrollPane() {
        return (outputAreaScrollPane);
    }

    void selectAllSqlButton_actionPerformed(ActionEvent e) {
        mrApplicationShell.selectAllASPStatements(true);
    }

    void forceRefresh() {
        String os_name = System.getProperty("os.name");
        if ((!os_name.equalsIgnoreCase("Sunos"))
                && java.io.File.separator.equals("/")) {
            // Dimension currentDimension = this.getSize();
            // Dimension newDimension = new
            // Dimension((int)currentDimension.getWidth()-1,
            // (int)currentDimension.getHeight());

            this.invalidate();
            this.repaint();
            this.validate();
            // System.out.println("Resize: " + currentDimension.getHeight() + "
            // x " + currentDimension.getWidth());
        }
    }

    void freezeCodeRootField() {
        codeRootDirTextField.setEditable(false);
        codeRootDirTextField.setEnabled(false);
        codeRootDirTextField
                .setToolTipText("This field is frozen when calling " + Namer.param_prod_name + " from an IDE");

        codeRootFileChooserButton.setEnabled(false);
        codeRootFileChooserButton
                .setToolTipText("This Button is frozen when calling " + Namer.param_prod_name + " from an IDE");

        String currentText = directoriesMessedWithtextArea1.getText();

        directoriesMessedWithtextArea1
                .setText(currentText
                        + " If you are using " + Namer.param_prod_name + " from within an IDE this field will be determined by the IDE.");

    }

    void selectNoSqlButton_actionPerformed(ActionEvent e) {
        mrApplicationShell.selectAllASPStatements(false);
    }

    void aspDirFileChooserButton_actionPerformed(ActionEvent e) {

        String oldValue = aspRootDirTextField.getText();

        // Ask plugin to translate ec file name in case it is one///
        McpDbWizardEvent event = new McpDbWizardEvent(
                McpDbWizardEvent.NEED_EC_TO_OS_TRANSLATION);
        event.setThing(aspRootDirTextField.getText());
        mrApplicationShell.setGenericString(null);
        mrApplicationShell.reportMcpDbWizardEvent(event);
        String hintString = mrApplicationShell.getGenericString();

        if (!(hintString == null)) {
            aspRootDirTextField.setText(hintString);
        }

        // See if current selection is valid


        // Create a chooser.
        java.io.File dirFile = new java.io.File(aspRootDirTextField.getText());

        if (!dirFile.exists()) {
            event = new McpDbWizardEvent(
                    McpDbWizardEvent.NEED_EC33_BASEDIR);
            mrApplicationShell.setGenericString(null);
            mrApplicationShell.reportMcpDbWizardEvent(event);

            String newBaseDirName = mrApplicationShell.getGenericString();

            if (newBaseDirName != null) {
                dirFile = new java.io.File(newBaseDirName);
            }
        }

        createChoosers(dirFile);

        int option = dirChooser.showOpenDialog(this);

        if (option == JFileChooser.APPROVE_OPTION) {
            java.io.File tempFile = dirChooser.getSelectedFile();

            if (tempFile == null) {
                mrApplicationShell.info("No directory selected", true, true);
            } else {
                mrApplicationShell.info("Directory "
                        + tempFile.getAbsolutePath() + " selected", true, true);

                if (hintString != null) {

                    event = new McpDbWizardEvent(
                            McpDbWizardEvent.NEED_OS_TO_EC_TRANSLATION);
                    event.setThing(tempFile.getAbsolutePath());
                    mrApplicationShell.setGenericString(null);
                    mrApplicationShell.reportMcpDbWizardEvent(event);

                    String newDirName = mrApplicationShell.getGenericString();

                    if (newDirName == null || newDirName.length() == 0) {
                        mrApplicationShell.error("Directory "
                                + tempFile.getAbsolutePath()
                                + " isn't in this project", true, true);
                        // put old value back fast....
                        aspRootDirTextField.setText(oldValue);
                    } else {

                        aspRootDirTextField.setText(tempFile.getAbsolutePath());
                        mrApplicationShell.refreshASPTables(sqlTableTree,
                                aspRootDirTextField, sqlTableSpiltPane,
                                sqlTableTreeScrollPane);
                        aspRootDirTextField.setText(newDirName);
                    }

                } else {
                    aspRootDirTextField.setText(tempFile.getAbsolutePath());
                    mrApplicationShell.refreshASPTables(sqlTableTree,
                            aspRootDirTextField, sqlTableSpiltPane,
                            sqlTableTreeScrollPane);
                }

            }
        } else {
            // put old value back fast....
            aspRootDirTextField.setText(oldValue);
        }

    }

    void aspRootDirTextField_actionPerformed(ActionEvent e) {

        String oldValue = aspRootDirTextField.getText();

        if (oldValue.length() == 0) {
            McpDbWizardEvent event = new McpDbWizardEvent(
                    McpDbWizardEvent.NEED_EC33_DEFAULT_SQLDIR);
            event.setThing("");
            mrApplicationShell.setGenericString(null);
            mrApplicationShell.reportMcpDbWizardEvent(event);
            String hintString = mrApplicationShell.getGenericString();

            if (!(hintString == null)) {
                aspRootDirTextField.setText(hintString);
                oldValue = aspRootDirTextField.getText();
            }
        }

        // Ask plugin to translate ec file name in case it is one///
        McpDbWizardEvent event = new McpDbWizardEvent(
                McpDbWizardEvent.NEED_EC_TO_OS_TRANSLATION);
        event.setThing(aspRootDirTextField.getText());
        mrApplicationShell.setGenericString(null);
        mrApplicationShell.reportMcpDbWizardEvent(event);
        String hintString = mrApplicationShell.getGenericString();

        if (!(hintString == null)) {
            aspRootDirTextField.setText(hintString);
        }

        mrApplicationShell.refreshASPTables(sqlTableTree, aspRootDirTextField,
                sqlTableSpiltPane, sqlTableTreeScrollPane);

        if (!(hintString == null)) {
            aspRootDirTextField.setText(oldValue);
        }

    }

    private void removeV4Components() {
        try {
            selectObjectsTabbedPane.remove(tablePanel);
            // selectObjectsTabbedPane.remove(sqlPanel);
            this.paintAll(this.getGraphics());
        } catch (Exception e) {
            System.out.println(e);
            System.exit(1);
        }
    }

    public JComponent getFrameObject(long theNumber) {
        if (labelsHaveDigits) {
            labelsHaveDigits = false;

            jLabel1.setText("Messages in generated code");
            jLabel2.setText("Comments in generated code");
            jLabel3.setText("Basic statistics  in generated code");
            jLabel4.setText("Naming convention for generated files");
            jLabel5.setText("Target Version of Oracle");
            jLabel6.setText("Use the following Numeric data types:");
            jLabel7.setText("Table specific options");
            userOrAllLabel.setText("Objects Belonging To...");

            //authorVersionTextArea2
            //			.setText("Enter a comment to appear at the start of every generated file. "
            //					+ "If you enter the name of a text file in this field its contents will "
            //					+ "be used.");

            buildOptionsTextArea
                    .setText("Change these values if you want to alter the code that is generated. "
                            + "Accept the default values if you are not sure which options to use.");

            authorVersionTextArea4
                    .setText("Enter your name and the version of your product. This information "
                            + "will be used by Javadoc.");

            aspFileDirectorytextArea
                    .setText("Select the SQL statements you wish to generate code for and "
                            + "specify data types and names for their parameters");

            aspFileDirectorytextArea1
                    .setText("Enter the directory where you keep your SQL statement files");

            selectObjectsTabbedPane.removeAll();
            selectObjectsTabbedPane.add(functionPanel,
                    "PL/SQL Packages, Procedures and Functions");
            selectObjectsTabbedPane.add(sequencePanel, "Sequences");
            selectObjectsTabbedPane.add(sqlPanel, "Sql Statements");
            selectObjectsTabbedPane.add(tablePanel, "Tables");

            directoriesMessedWithtextArea2
                    .setText("Use the options below to define how your DAO Factory class will "
                            + "behave. This class handles logging and connection management for "
                            + "generated code. It is created at the top level of the generated code "
                            + "hirearchy. If you do not want this class created leave the Factory Class name field blank.");

            directoriesMessedWithtextArea1
                    .setText("Enter the root directory for your Java code. This is usually "
                            + " the one above \'com\'. For Example: C:\\Test\\Src");
            jTextArea1
                    .setText("Use the options below to control how the service classes behave.");

            packageNameTextArea
                    .setText("Enter the package name you want the generated classes to belong "
                            + "to. e.g. com.mycompany.myapplication.generated ");

            jvmLabel.setText("Target JVM:");
            sqlLabel.setText("Prefix for SQL Methods:");
            plsqlLabel.setText("Prefix for PL/SQL Methods:");

            postDescrTextArea.setText("Enter any extra Java code you want added to the implementaton class below");
            postScriptDescrTextArea.setText("In the area you below you can define a batch file that will be automatically run when you have finished generating code");
        }

        if (theNumber == 4201) {
            hostnameField.setEnabled(false);
            sidField.setEnabled(false);
            portField.setEnabled(false);
            oraUserField.setEnabled(false);
            oraPassField.setEnabled(false);
            return (loginPanel);
        }
        if (theNumber == 4202) {
            return (selectObjectsPanel);
        }
        if (theNumber == 4203) {
            return (codeOptionsPanel);
        } else if (theNumber == 4204) {
            return (fileOptionsPanel);
        } else if (theNumber == 4205) {
            return (sequenceTable);
        } else if (theNumber == 4206) {
            return (functionTable);
        } else if (theNumber == 4207) {
            return (tableTable);
        } else if (theNumber == 4209) {
            return (fileOptionsPanelExtraPanel);
        } else if (theNumber == 4210) {
            return (outputPanel);
        } else if (theNumber == 4211) {
            return (postOptionsPanel);
        }

        return (null);
    }

    void validateCB_actionPerformed(ActionEvent e) {

    }

    void useDoubleCB_actionPerformed(ActionEvent e) {

    }

    void useDoubleOCB_actionPerformed(ActionEvent e) {

    }

    public boolean adminTabbedPaneUsable() {
        return (adminTabbedPane.isEnabled());
    }

    void versionTextField_actionPerformed(ActionEvent e) {

    }

    void codeRootDirTextField_actionPerformed(ActionEvent e) {

    }

    void oraUserField_actionPerformed(ActionEvent e) {

    }

    void statsCheckBox1_actionPerformed(ActionEvent e) {

    }

    void connectionNameTextField_actionPerformed(ActionEvent e) {

    }

    void daoFactoryNameTextField_actionPerformed(ActionEvent e) {

    }

    void generateSessionBeanCheckBox_actionPerformed(ActionEvent e) {

    }

    void connectionTypeComboBox_actionPerformed(ActionEvent e) {

    }

    void logTypeComboBox_actionPerformed(ActionEvent e) {

    }

    void logNameTextField1_actionPerformed(ActionEvent e) {

    }

    void wsPreCallCheckBox_actionPerformed(ActionEvent e) {

    }

    void packageNameTextField1_actionPerformed(ActionEvent e) {

    }

    void javaNumberTypeComboBox_actionPerformed(ActionEvent e) {

    }

    void wsRecTypeComboBox_actionPerformed(ActionEvent e) {

    }

    void methodPlsqlTextField_actionPerformed(ActionEvent e) {

    }

    void methodPlsqlTextField1_actionPerformed(ActionEvent e) {

    }

    void methodSqlTextField_actionPerformed(ActionEvent e) {

    }

    void genericTemplateButton_actionPerformed(ActionEvent e) {
        try {
            String selection = (String) genericTemplateComboBox.getSelectedItem();

            if (selection.indexOf("-") > -1) {
                selection = selection.substring(0, selection.indexOf("-"));
                selection = selection.trim();
            }
            if (!selection.equals(SELECT_GENERIC_TEMPLATE)) {

                if (postScriptCodeTextArea.getText() == null || postScriptCodeTextArea.getText().length() > 0) {
                    mrApplicationShell.error("A Generic template can only be used if the post generaction script area is empty", true, true);


                } else {

                    TemplateWrangler t = new TemplateWrangler(mrApplicationShell);
                    String[] fileContents = t.getTemplate(selection, mrApplicationShell.getFileProps(), mrApplicationShell, mrApplicationShell.mrWrangler.mrConnection);
                    String fileContents1String = "";

                    for (int i = 0; i < fileContents.length; i++) {
                        fileContents1String = fileContents1String + "\n" + fileContents[i];
                    }
                    postScriptCodeTextArea.setText(fileContents1String);
                }
            }
        } catch (Exception e1) {
            mrApplicationShell.error("genericTemplateButton_actionPerformed");
            mrApplicationShell.error(e1);
        }
    }


}
