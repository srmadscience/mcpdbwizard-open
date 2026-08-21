package com.mcpdbwizard.app.procbuilder.gui;

import javax.swing.*;

import java.util.Properties;

import java.io.*;

import javax.swing.tree.*;

//import java.util.HashMap;

//import java.sql.SQLException;

import java.lang.Integer;

import javax.swing.table.*;
//import javax.swing.JPanel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TableModelListener;

import java.awt.*;

import com.mcpdbwizard.pub.*;
import com.mcpdbwizard.app.common.*;
//import com.mcpdbwizard.app.common.gui.*;
import com.mcpdbwizard.app.procbuilder.*;

import javax.swing.event.TreeSelectionListener;

/**
 * @author devteam@mcpdbwizard.com
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 * @version 2
 */
public class ApplicationShell implements LogInterface, TreeSelectionListener, TableModelListener {

    public static final String[] javaNamingConventions =
            {"JavaStandard.java", "InitialCapitalLetters.java",
                    "spaces_between_words.java"};
    //static final boolean OB3 = true;
    public static final String[] WS_JAVA_NUMBER_TYPES =
            {"double", "float", "java.math.BigDecimal", "long", "int"};
    public static final String[] WS_REC_TYPES =
            {"public, no access methods", "private, set & get methods"};
    static final boolean OB2 = false;
    static final String FILE_EXTENSION = ".pb2";
    static final String DEFAULT_INI_FILE_NAME = "procbuilder.pb2";
    static final String LOG_FILE_PREFIX = "PROCBUILDER_";
    static final String PARSE_PLSQL_MESSAGE =
            "Parsing ALL_SOURCE to create list of stored procedures - This can take up to 30 seconds...";
    static final String PARSE_TABLE_MESSAGE = "Examining ALL_TABLES...";
    static final int OUTPUT_TEXT_AREA_LINES = 500;
    static final int DEFAULT_SCREEN_WIDTH = 800;
    static final int DEFAULT_SCREEN_HEIGHT = 600;
    static final int MIN_SCREEN_HEIGHT = 500;
    static final int TREE_ROW_HEIGHT = 22;
    static final String DEFAULT_FROM_LOGIN = "DEFAULT_FROM_LOGIN";
    static final String DEFAULT_HOST_NAME = "my_oracle_host";
    static final String DEFAULT_HOST_SID = "XE";
    static final String DEFAULT_HOST_PORT = "1521";
    public static final String[] javaAccessTypes =
            {"default", "public", "protected", "private"};

    /**
     * What returning a factory to the DAO pool does with the borrower's transaction.
     * {@code COMMIT} matches the unpooled {@code COMMIT_CONNECTIONS=YES} default. Public so the web
     * UI's option catalog can offer the same value set rather than repeating it.
     */
    public static final String DAO_POOL_ON_RETURN_COMMIT = "COMMIT";
    public static final String DAO_POOL_ON_RETURN_ROLLBACK = "ROLLBACK";
    public static final String[] DAO_POOL_ON_RETURN_ACTIONS =
            {DAO_POOL_ON_RETURN_COMMIT, DAO_POOL_ON_RETURN_ROLLBACK};

    // Pool sizing defaults, kept identical to com.mcpdbwizard.pub.DaoFactoryPoolConfig's so that a
    // config saying nothing and a config saying the default behave the same way.
    public static final String DEFAULT_DAO_POOL_MAX_SIZE = "10";
    public static final String DEFAULT_DAO_POOL_MIN_IDLE = "0";
    public static final String DEFAULT_DAO_POOL_MAX_WAIT_MS = "30000";
    public static final String DEFAULT_DAO_POOL_IDLE_TIMEOUT_MS = "300000";
    String iniFileName = DEFAULT_INI_FILE_NAME;
    SAAdminWrangler mrWrangler;
    boolean areConnected = false;
    JLabel mrStatusBar;
    ThingAdministratorFrame mrFrame;
    String user_working_dir;
    String uiName = "";
    UIManager.LookAndFeelInfo[] lookAndFeelInfoArray;
    String mrUiClassName = null;
    Properties fileProps = new Properties();
    String logFileDirectory = null;
    int today = -1;
    String ipField = null;
    String portField = null;
    String sidField = null;
    String userField = null;
    String passField = null;
    String authorField = null;
    String versionField = null;
    String commentField = null;
    boolean userObjectsFlag = true;
    boolean otherObjectsFlag = true;
    String otherUserFilter = null;
    String x_size = null;
    String y_size = null;
    String sqlDirField = null;
    String codeRootField = null;
    String packageNameField = null;
    String chosenOracleVersion = null;
    boolean debugMessagesCheckbox = true;
    boolean otherMessagesCheckbox = true;
    boolean commentsCheckBox = true;
    boolean useByteCheckBox = true;
    boolean useShortCheckBox = true;
    boolean useIntCheckBox = true;
    boolean useLongCheckBox = true;
    boolean useFloatCheckBox = true;
    boolean useDoubleCheckBox = true;
    boolean useByteObjCheckBox = true;
    boolean useShortObjCheckBox = true;
    boolean useIntObjCheckBox = true;
    boolean useLongObjCheckBox = true;
    boolean useFloatObjCheckBox = true;
    boolean useDoubleObjCheckBox = true;
    boolean validateCheckBox = true;
    boolean extraSQLCheckBox = true;
    boolean freezeCodeRootField = false;
    //11 09
    String targetJVM = "";
    String methodPlsqlPrefix = "";
    String methodSqlPrefix = "";
    String postscriptName = "";
    String postscriptContent = "";
    String extraClassCode = "";
    LogInterface mrStatusLog;
    SequenceTableDataModel sequenceModel;
    FunctionTableDataModel functionModel;
    TableTableDataModel tableModel;
    String[] versionInfoArray = null;
    int messageLevel = 0;
    boolean statsCheckBox = true;
    String javaAccessComboBox = null;
    String javanamingComboBox = null;
    boolean shutdownOnExit = true;
    String wsJavaNumberTypeComboBox = null;
    String wsRecTypeComboBox = null;
    AspToplevelPanel theAspToplevelPanel;
    AspFilePanel theAspFilePanel;
    AspBrokenPanel theBrokenPanel;
    SqlStatementWrangler[] aspStatementWranglerManager = null;
    //SqlStatementWranglerManager aspStatementWranglerManager = null;
    String[] aspStatementErrors = null;
    String[] aspStatementFileNames = null;
    int badFileWranglers = 0;
    int badSqlWranglers = 0;
    JSplitPane aspSplitPane;
    File aspSourceDirectory = null;
    McpDbWizardEventListener listener = null;
    ProgressMonitor theProgressMonitor = null;
    String ec30ProjectPath = "";
    String ec30ProjectRelPath = "";
    String factoryClassName = "";
    String connectionType = "";
    String connectionName = "";
    String logType = "";
    String logName = "";
    String ejbType = "";
    boolean webServicesFlag = false;
    boolean mcpServerFlag = false;
    // Enables bearer-token auth on the generated MCP server's HTTP transport (secret via the
    // MCP_HTTP_TOKEN environment variable at run time). Propfile-only flag; no GUI control.
    boolean mcpHttpTokenFlag = false;
    // Enables TLS on the generated MCP server's HTTP transport (keystore path + passwords via the
    // MCP_TLS_KEYSTORE / MCP_TLS_KEYSTORE_PASSWORD env vars at run time). Propfile-only; no GUI control.
    boolean mcpHttpsFlag = false;
    boolean mcpOAuthFlag = false;

    /**
     * Emit Prometheus metrics collection and the {@code /metrics} endpoint in the generated MCP
     * server. Propfile and web GUI only -- there is deliberately no Swing control, exactly as for
     * {@link #mcpHttpTokenFlag} and {@link #mcpHttpsFlag}.
     */
    /**
     * MCP_INSTRUCTIONS: author-supplied text prepended to the generated server's MCP
     * instructions. Propfile + web GUI, deliberately no Swing control, like MCP_HTTP_TOKEN
     * and PROMETHEUS_SERVER. Held so a save PRESERVES it -- a scalar this class does not read
     * is dropped on the next save, silently.
     */
    String mcpInstructions = null;

    boolean prometheusServerFlag = false;
    boolean webServicesBfilesAreAbstractFlag = false;
    boolean finalizeMethodFlag = false;
    boolean servicePreCallStubFlag = false;
    boolean servicePostCallStubFlag = false;
    boolean serviceAlwaysReleaseFlag = false;
    String bfileDirName = "";
    String bfilePrefix = "";
    String bfileSuffix = "";
    String tempDirName = "";
    String tempPrefix = "";
    String tempSuffix = "";
    String xwsTypeName = "";
    String xwsImpl = "";
    String xwsIface = "";
    //String classDAOFactoryName = "";
    String classWSInterfaceName = "";
    String classWSImplName = "";
    boolean closeConnectionFlag = false;
    boolean commitConnectionFlag = false;
    // DAO factory pooling (the "Pooling" tab). Held as Strings like every other scalar, so a value
    // absent from the propfile stays absent rather than being written back as a default. The
    // defaults below are the same ones com.mcpdbwizard.pub.DaoFactoryPoolConfig applies.
    boolean daoPoolFlag = false;
    String daoPoolMaxSize = DEFAULT_DAO_POOL_MAX_SIZE;
    String daoPoolMinIdle = DEFAULT_DAO_POOL_MIN_IDLE;
    String daoPoolMaxWaitMs = DEFAULT_DAO_POOL_MAX_WAIT_MS;
    String daoPoolIdleTimeoutMs = DEFAULT_DAO_POOL_IDLE_TIMEOUT_MS;
    String daoPoolOnReturn = DAO_POOL_ON_RETURN_COMMIT;
    String[] oracleDirectoryList = {""};
    private String genericString;

    //boolean isDB2 = false;

    /**
     * @param logDir  where the log file goes
     * @param iniFile the config to load
     *
     * <p>A third {@code accessCode} parameter was removed in 2026-08. It was accepted and then
     * ignored — never assigned, compared or read — so it protected nothing; see
     * {@link ProcBuilder} for the full account.
     */
    public ApplicationShell(String logDir, String iniFile) {
        user_working_dir = System.getProperty("user.dir");

        logFileDirectory = new String(logDir);

        iniFileName = new String(iniFile);



        try {
            mrUiClassName = UIManager.getSystemLookAndFeelClassName();
        } catch (Exception e) {
        }

        try {
            java.io.File warnFile =
                    new java.io.File(logFileDirectory + java.io.File.separator +
                            "warn2.txt");
            if (warnFile.exists()) {
                warnFile.delete();
            }
        } catch (Exception efoo) {
        }

    }

    /** As {@link #ApplicationShell(String, String)}, with the config already parsed. */
    public ApplicationShell(String logDir, Properties iniProps) {
        user_working_dir = System.getProperty("user.dir");

        logFileDirectory = new String(logDir);

        fileProps = new Properties(iniProps);
        iniFileName = "";



        try {
            mrUiClassName = UIManager.getSystemLookAndFeelClassName();
        } catch (Exception e) {
        }

        try {
            java.io.File warnFile =
                    new java.io.File(logFileDirectory + java.io.File.separator +
                            "warn2.txt");
            if (warnFile.exists()) {
                warnFile.delete();
            }
        } catch (Exception efoo) {
        }

    }

    public void setFrame(JLabel statusBar, ThingAdministratorFrame theFrame) {

        mrStatusBar = statusBar;
        mrFrame = theFrame;
        this.theAspToplevelPanel = mrFrame.theAspToplevelPanel;
        this.theAspFilePanel = mrFrame.theAspFilePanel;
        this.theBrokenPanel = mrFrame.theBrokenPanel;

        mrStatusBar.setText("");

        if (mrStatusLog == null) {
            mrStatusLog =
                    new UiLog(logFileDirectory, LOG_FILE_PREFIX, mrStatusBar,
                            theFrame);
            //((UiLog)mrStatusLog).setPanel(theFrame.outputLogPanel);
        }

        mrStatusLog.setAutoFlush(true);
        mrStatusLog.setAutoLog(true);

        try {
            lookAndFeelInfoArray = UIManager.getInstalledLookAndFeels();
        } catch (Exception e) {
            lookAndFeelInfoArray = null;
            warning("No look and feel information available");
        }

        getIniFile();

        if (freezeCodeRootField) {
            mrFrame.freezeCodeRootField();
        }

        setLookAndFeel();

    }

    public boolean currentlyConnected() {
        return (areConnected);
    }

    public boolean connectDB(JTextField pIpField, JTextField pPortField,
                             JTextField pSidField, JTextField pUserField,
                             JTextField pPassField, JButton pConnectButton,
                             JTextArea pConnectionStatus,
                             String pNewButtonText, JTable pSequenceTable,
                             JTextField pConnectionNameTextField) {
        //info("Attempting to connect to " + pSidField.getText());


        // force username to upper case
        pUserField.setText(pUserField.getText().toUpperCase());

        //int portNumber;
        //Integer portNumberInt;


        if (connectDB(pIpField.getText(), pPortField.getText(),
                pSidField.getText(), pUserField.getText().toUpperCase(),
                pPassField.getText())) {
            info("Connected to instance " + pSidField.getText());
            pConnectButton.setText(pNewButtonText);

            areConnected = true;

            pIpField.setEnabled(false);
            pPortField.setEnabled(false);
            pSidField.setEnabled(false);
            pUserField.setEnabled(false);
            pPassField.setEnabled(false);

            versionInfoArray = mrWrangler.getVersionInfo();
            pConnectionStatus.setText("");

            for (int i = 0; i < versionInfoArray.length; i++) {
                pConnectionStatus.append(versionInfoArray[i] + "\n");
            }

            if (pConnectionNameTextField.getText().equals(DEFAULT_FROM_LOGIN)) {
                pConnectionNameTextField.setText("jdbc:oracle:thin:" +
                        pUserField.getText().toUpperCase() +
                        "/" + pPassField.getText() +
                        "@" + pIpField.getText() +
                        ":" + pPortField.getText() +
                        ":" + pSidField.getText());
            }


        }


        return (true);
    }

    public boolean connectDB(String pIpField, String pPortField,
                             String pSidField, String pUserField,
                             String pPassField) {
        info("Attempting to connect to " + pSidField);

        McpDbWizardEvent newEvent =
                new McpDbWizardEvent(McpDbWizardEvent.TRYING_TO_LOG_IN);
        newEvent.thing = "Attempting to connect to " + pSidField;

        reportMcpDbWizardEvent(newEvent);

        // force username to upper case
        pUserField = new String(pUserField.toUpperCase());

        int portNumber;
        Integer portNumberInt;

        try {
            portNumberInt = Integer.valueOf(pPortField);
            portNumber = portNumberInt.intValue();
        } catch (Exception e) {
            error("Invalid Sql*Net port number", true, true);

            newEvent =
                    new McpDbWizardEvent(McpDbWizardEvent.UNABLE_TO_LOG_IN);
            newEvent.thing = "Invalid Sql*Net port number";

            reportMcpDbWizardEvent(newEvent);

            return (false);
        }

        try {
            boolean tempPhys = true;
            if (mrWrangler != null) {
                tempPhys = mrWrangler.getWritePhysicalFiles();
            }

            mrWrangler =
                    new SAAdminWrangler("FOO", pIpField, portNumber, pSidField,
                            pUserField, pPassField, this,
                            listener);
            mrWrangler.setWritePhysicalFiles(tempPhys);
        } catch (CSException e) {
            error(e.getMessage(), true, true);
            newEvent =
                    new McpDbWizardEvent(McpDbWizardEvent.UNABLE_TO_LOG_IN);
            newEvent.thing = e.getMessage();

            reportMcpDbWizardEvent(newEvent);

            return (false);
        }

        if (connectionName.equals(DEFAULT_FROM_LOGIN)) {
            connectionName =
                    "jdbc:oracle:thin:" + pUserField.toUpperCase() + "/" +
                            pPassField + "@" + pIpField + ":" + pPortField + ":" +
                            pSidField;
        }

        info("Connected to instance " + pSidField);
        //mrWrangler.setTrace(true);

        areConnected = true;

        versionInfoArray = mrWrangler.getVersionInfo();
        for (int i = 0; i < versionInfoArray.length; i++) {
            info(versionInfoArray[i]);
        }

        today = mrWrangler.getCurrentDay();


        getIniFile();

        newEvent =
                new McpDbWizardEvent(McpDbWizardEvent.LOGGED_IN);
        newEvent.thing = pIpField;
        newEvent.thing2 = sqlDirField;
        reportMcpDbWizardEvent(newEvent);

        return (true);
    }

    public void refreshTables(JCheckBox meCheckBox, JTextField oraUserField,
                              JCheckBox otherCheckBox,
                              JTextField otherUserName, JTable sequenceTable,
                              int sequenceTableHeight, JTable tableTable,
                              int tableTableHeight, JTable functionTable,
                              int functionTableHeight, JTree aspTree,
                              JTextField aspDirName, JSplitPane aspSplitPane,
                              JScrollPane aspTreeScollPane) {

        boolean oraUserFlag = false;
        boolean otherUserFlag = false;
        String oraUser = new String(oraUserField.getText().toUpperCase());
        String otherUser = new String(otherUserName.getText().toUpperCase());

        java.awt.Font preferredFont = new java.awt.Font("Dialog", 0, 11);

        if (meCheckBox.isSelected()) {
            oraUserFlag = true;
        }

        if (otherCheckBox.isSelected()) {
            otherUserFlag = true;
        }

        initProgressMonitor("Retrieving data from the database",
                "Checking Sequences...");

        try {
            mrWrangler.confirmConnected();
        } catch (CSException e) {
            syserror(e);
        }

        setProgress("Checking PL/SQL...", 20);

        createFunctionTable(oraUser, oraUserFlag, otherUser, otherUserFlag,
                functionTable, functionTableHeight);
        functionTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        functionTable.setFont(preferredFont);
        if (listener != null) {
            McpDbWizardEvent obEvent = new McpDbWizardEvent(McpDbWizardEvent.FUNCTION_TABLE_LOADED);
            obEvent.setThing(functionTable.getModel());

            listener.reportEvent(obEvent);
        }

        setProgress("Checking Sequences...", 40);

        createSequenceTable(oraUser, oraUserFlag, otherUser, otherUserFlag,
                sequenceTable, sequenceTableHeight);
        sequenceTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        sequenceTable.setFont(preferredFont);
        if (listener != null) {
            McpDbWizardEvent obEvent = new McpDbWizardEvent(McpDbWizardEvent.SEQUENCE_TABLE_LOADED);
            obEvent.setThing(sequenceTable.getModel());

            listener.reportEvent(obEvent);
        }

        setProgress("Checking Tables...", 40);

        createTableTable(oraUser, oraUserFlag, otherUser, otherUserFlag,
                tableTable, tableTableHeight);
        tableTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tableTable.setFont(preferredFont);
        if (listener != null) {
            McpDbWizardEvent obEvent = new McpDbWizardEvent(McpDbWizardEvent.TABLE_TABLE_LOADED);
            obEvent.setThing(tableTable.getModel());

            listener.reportEvent(obEvent);
        }

        setProgress("Checking SQL Statements...", 80);

        refreshASPTables(aspTree, aspDirName, aspSplitPane, aspTreeScollPane);

        //setProgress("Finished", 80);
        info("Refresh Complete");

    }

    public void refreshASPTables(JTree aspTree, JTextField aspDirName,
                                 JSplitPane aspSplitPane,
                                 JScrollPane aspTreeScollPane) {
        createAspTree(aspTree, aspDirName.getText(), theAspFilePanel,
                theAspToplevelPanel);
        this.aspSplitPane = aspSplitPane;
        aspSplitPane.setDividerLocation(200);
        aspSplitPane.setRightComponent(theAspToplevelPanel);
        //System.out.println("h c" +  aspTree.getRowCount());

        Dimension treeDim =
                new Dimension(aspTree.getWidth(), TREE_ROW_HEIGHT * aspTree.getRowCount());
        aspTree.setPreferredSize(treeDim);

        //aspTreeScollPane.setPreferredSize(treeDim);
    }

    public boolean disconnectDB(JTextField pIpField, JTextField pPortField,
                                JTextField pSidField, JTextField pUserField,
                                JTextField pPassField, JButton pConnectButton,
                                JTextArea pConnectionStatus,
                                String pNewButtonText) {
        if (areConnected) {

            info("Disconnecting from " + pSidField.getText());

            areConnected = false;
            mrWrangler.disconnect();
            pIpField.setEnabled(true);
            pPortField.setEnabled(true);
            pSidField.setEnabled(true);
            pUserField.setEnabled(true);
            pPassField.setEnabled(true);
            pConnectionStatus.setText("Disconnected");

            pConnectButton.setText(pNewButtonText);
            info("Disconnected from " + pSidField.getText());
            reportMcpDbWizardEvent(new McpDbWizardEvent(McpDbWizardEvent.NOT_LOGGED_IN));

        }

        return (true);
    }

    public Properties getFileProps() {
        return (fileProps);
    }

    public void setFileProps(Properties fileProps) {
        this.fileProps = fileProps;
    }

    /** True when a config filename denotes the JSON format ({@code .json}) rather than a {@code .pb2}. */
    static boolean isJsonConfig(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".json");
    }

    /**
     * Load a generation-config file into a {@link Properties} set, transparently accepting either
     * the classic {@code .pb2} (Java properties) format or the new {@code .json} format. A JSON file
     * is parsed into a {@link com.mcpdbwizard.schema.Schema} and expanded via
     * {@link com.mcpdbwizard.schema.Schema#toPb2()} to the exact same property set the equivalent
     * {@code .pb2} would have produced, so every code path downstream is unchanged.
     */
    static Properties loadConfig(String fileName) throws IOException {
        if (isJsonConfig(fileName)) {
            String json = new String(
                    java.nio.file.Files.readAllBytes(new File(fileName).toPath()),
                    java.nio.charset.StandardCharsets.UTF_8);
            return new com.mcpdbwizard.schema.Schema(json).toPb2();
        }
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(fileName)) {
            p.load(in);
        }
        return p;
    }

    public void getIniFile() {
        // populate various fields in mrFrame from the ini file.
        debug("Loading saved properties");

        if (iniFileName != null && iniFileName.length() > 0 && getWritePhysicalFiles()) {
            try {
                fileProps = loadConfig(iniFileName);
            } catch (Exception e) {
                fileProps = new Properties();
                fileProps.setProperty("UINAME", "Metal");
                warning("No saved configuration exists");
            }
        }

        try {
            ipField = fileProps.getProperty("HOSTNAME", DEFAULT_HOST_NAME);
            portField = fileProps.getProperty("PORT", DEFAULT_HOST_PORT);
            sidField = fileProps.getProperty("ORACLE_SID", DEFAULT_HOST_SID);
            userField = fileProps.getProperty("USER", "ORINDADEMO" + Namer.param_build);
            passField = fileProps.getProperty("PASS", "ORINDADEMO");

            x_size =
                    fileProps.getProperty("X_SIZE", DEFAULT_SCREEN_WIDTH + "");
            y_size =
                    fileProps.getProperty("Y_SIZE", DEFAULT_SCREEN_HEIGHT + "");

            authorField = fileProps.getProperty("AUTHOR", "");
            versionField = fileProps.getProperty("VERSION", "");
            commentField = fileProps.getProperty("COMMENT", "");

            chosenOracleVersion =
                    fileProps.getProperty("ORACLE_VERSION", SqlStatementDictionary.latestOracleVersion);

            if (fileProps.getProperty("DEBUG_MESSAGES", "YES").equals("YES")) {
                debugMessagesCheckbox = true;
            } else {
                debugMessagesCheckbox = false;
            }

            if (fileProps.getProperty("OTHER_MESSAGES", "YES").equals("YES")) {
                otherMessagesCheckbox = true;
            } else {
                otherMessagesCheckbox = false;
            }

            if (fileProps.getProperty("CODE_COMMENTS", "YES").equals("YES")) {
                commentsCheckBox = true;
            } else {
                commentsCheckBox = false;
            }

            if (fileProps.getProperty("USER_OBJECTS", "YES").equals("YES")) {
                userObjectsFlag = true;
            } else {
                userObjectsFlag = false;
            }

            if (fileProps.getProperty("OTHER_OBJECTS", "YES").equals("YES")) {
                otherObjectsFlag = true;
            } else {
                otherObjectsFlag = false;
            }

            if (fileProps.getProperty("METHODS_BYTE", "YES").equals("YES")) {
                useByteCheckBox = true;
            } else {
                useByteCheckBox = false;
            }

            if (fileProps.getProperty("METHODS_SHORT", "YES").equals("YES")) {
                useShortCheckBox = true;
            } else {
                useShortCheckBox = false;
            }

            if (fileProps.getProperty("METHODS_INT", "YES").equals("YES")) {
                useIntCheckBox = true;
            } else {
                useIntCheckBox = false;
            }

            if (fileProps.getProperty("METHODS_LONG", "YES").equals("YES")) {
                useLongCheckBox = true;
            } else {
                useLongCheckBox = false;
            }

            if (fileProps.getProperty("METHODS_FLOAT", "YES").equals("YES")) {
                useFloatCheckBox = true;
            } else {
                useFloatCheckBox = false;
            }

            if (fileProps.getProperty("METHODS_DOUBLE", "YES").equals("YES")) {
                useDoubleCheckBox = true;
            } else {
                useDoubleCheckBox = false;
            }


            if (fileProps.getProperty("METHODS_BYTE_OBJ",
                    "YES").equals("YES")) {
                useByteObjCheckBox = true;
            } else {
                useByteObjCheckBox = false;
            }

            if (fileProps.getProperty("METHODS_SHORT_OBJ",
                    "YES").equals("YES")) {
                useShortObjCheckBox = true;
            } else {
                useShortObjCheckBox = false;
            }

            if (fileProps.getProperty("METHODS_INT_OBJ",
                    "YES").equals("YES")) {
                useIntObjCheckBox = true;
            } else {
                useIntObjCheckBox = false;
            }

            if (fileProps.getProperty("METHODS_LONG_OBJ",
                    "YES").equals("YES")) {
                useLongObjCheckBox = true;
            } else {
                useLongObjCheckBox = false;
            }

            if (fileProps.getProperty("METHODS_FLOAT_OBJ",
                    "YES").equals("YES")) {
                useFloatObjCheckBox = true;
            } else {
                useFloatObjCheckBox = false;
            }

            if (fileProps.getProperty("METHODS_DOUBLE_OBJ",
                    "YES").equals("YES")) {
                useDoubleObjCheckBox = true;
            } else {
                useDoubleObjCheckBox = false;
            }

            if (fileProps.getProperty("VALIDATE", "YES").equals("YES")) {
                validateCheckBox = true;
            } else {
                validateCheckBox = false;
            }

            if (fileProps.getProperty("EXTRA_SQL", "YES").equals("YES")
            ) {
                extraSQLCheckBox = true;
            } else {
                extraSQLCheckBox = false;
            }

            otherUserFilter = fileProps.getProperty("OTHER_USER_NAME");

            codeRootField = fileProps.getProperty("CODE_BASE_DIRECTORY");

            if (uiName.equals("EC33")) {
                codeRootField = fileProps.getProperty("CODE_CREATE_DIRECTORY_EC33");


            }


            sqlDirField =
                    fileProps.getProperty("SQL_FILE_DIRECTORY", System.getProperty("user.home"));

            if (uiName.equals("EC33")) {
                sqlDirField = fileProps.getProperty("SQL_FILE_DIRECTORY_EC33", System.getProperty("user.home"));


            }


            // Perform magic if we get value of SQLFILEDEMO...
            if (sqlDirField.equalsIgnoreCase("SQLFILEDEMO")) {
                try {
                    File currentIniFile = new File(iniFileName);
                    File currentIniDir = currentIniFile.getParentFile();
                    File currentInstallDir = currentIniDir.getParentFile();
                    sqlDirField =
                            currentInstallDir.getAbsolutePath() + File.separator +
                                    "demo" + File.separator + "Sqlfiles"; //DRKLUGE need to make work for EC33
                } catch (Exception e) {
                    sqlDirField = System.getProperty("user.home");
                }
            }

            packageNameField = fileProps.getProperty("PACKAGE_NAME");

            if (fileProps.getProperty("CODE_STATISTICS",
                    "YES").equals("YES")) {
                statsCheckBox = true;
            } else {
                statsCheckBox = false;
            }

            javaAccessComboBox =
                    fileProps.getProperty("JAVA_ACCESS_TYPE", "public");

            javanamingComboBox =
                    fileProps.getProperty("JAVA_NAMING_CONVENTION",
                            javaNamingConventions[0]);

            if (factoryClassName == null) {
                factoryClassName = "DAOFactory";
            }

            if (fileProps.getProperty("WEB_SERVICES", "YES").equals("YES")) {
                webServicesFlag = true;
            } else {
                webServicesFlag = false;
            }

            // MCP server emission (the "Create MCP Server Class" checkbox). The
            // generated <Factory>McpServer needs a 17+ target JVM; it defaults to NO.
            if (fileProps.getProperty("MCP_SERVER", "NO").equals("YES")) {
                mcpServerFlag = true;
            } else {
                mcpServerFlag = false;
            }

            // Bearer-token auth on the generated MCP HTTP transport (propfile-only flag; the
            // secret is supplied at run time via the MCP_HTTP_TOKEN environment variable).
            if (fileProps.getProperty("MCP_HTTP_TOKEN", "NO").equals("YES")) {
                mcpHttpTokenFlag = true;
            } else {
                mcpHttpTokenFlag = false;
            }

            // TLS on the generated MCP HTTP transport (propfile-only flag; the keystore path and
            // passwords are supplied at run time via the MCP_TLS_KEYSTORE / MCP_TLS_KEYSTORE_PASSWORD
            // environment variables).
            if (fileProps.getProperty("MCP_HTTPS", "NO").equals("YES")) {
                mcpHttpsFlag = true;
            } else {
                mcpHttpsFlag = false;
            }

            if (fileProps.getProperty("MCP_OAUTH", "NO").equals("YES")) {
                mcpOAuthFlag = true;
            } else {
                mcpOAuthFlag = false;
            }

            // Prometheus metrics on the generated MCP server (propfile + web GUI; deliberately no
            // Swing control, like MCP_HTTP_TOKEN and MCP_HTTPS above). It emits the collection and
            // the /metrics endpoint; the MCP_METRICS_PORT environment variable is what starts the
            // listener at run time.
            if (fileProps.getProperty("PROMETHEUS_SERVER", "NO").equals("YES")) {
                prometheusServerFlag = true;
            } else {
                prometheusServerFlag = false;
            }

            // Read as-is, with no default: null means the key was absent and empty means an
            // author cleared it. Neither emits anything, so the distinction only keeps the
            // round trip lossless.
            mcpInstructions = fileProps.getProperty("MCP_INSTRUCTIONS");

            // WEB_SERVICES_ABSTRACT_BFILE is DELIBERATELY NOT READ. The option made the
            // generated ServiceImpl abstract, so callers supplied the BFILE upload naming
            // themselves. An MCP_SERVER=YES config cannot use such a class: the emitted
            // server instantiates it directly (`new <Factory>ServiceImpl(...)`), so the two
            // settings together generated code that would not compile -- and nothing
            // checked the pair, so it surfaced as a javac error rather than a refusal.
            //
            // Both GUIs dropped the control and now write NO on save, but that alone left
            // the setting reachable by hand-editing a .pb2/.json. Ignoring the key here is
            // what makes YES unreachable: this is the only place the flag is read.
            // Schema still round-trips the key so a legacy config converts .pb2 <-> .json
            // without losing it; it simply no longer has any effect.
            webServicesBfilesAreAbstractFlag = false;

            if (fileProps.getProperty("DAO_FINALIZE", "NO").equals("YES")) {
                finalizeMethodFlag = true;
            } else {
                finalizeMethodFlag = false;
            }

            if (fileProps.getProperty("WS_PRE_CALL_STUB",
                    "YES").equals("YES")) {
                servicePreCallStubFlag = true;
            } else {
                servicePreCallStubFlag = false;
            }

            if (fileProps.getProperty("WS_POST_CALL_STUB",
                    "YES").equals("YES")) {
                servicePostCallStubFlag = true;
            } else {
                servicePostCallStubFlag = false;
            }

            if (fileProps.getProperty("WS_ALWAYS_RELEASE",
                    "NO").equals("YES")) {
                serviceAlwaysReleaseFlag = true;
            } else {
                serviceAlwaysReleaseFlag = false;
            }

            bfileDirName = fileProps.getProperty("BFILE_DIR_NAME", "");
            bfilePrefix = fileProps.getProperty("BFILE_PREFIX", "BFILE");
            bfileSuffix = fileProps.getProperty("BFILE_SUFFIX", ".dat");

            tempDirName = fileProps.getProperty("DEFAULT_TEMPDIR", "user.dir");
            tempPrefix = fileProps.getProperty("DEFAULT_TEMP_PREFIX", "OSOFT");
            tempSuffix = fileProps.getProperty("DEFAULT_TEMP_SUFFIX", ".tmp");

            xwsTypeName = fileProps.getProperty("XWS_TYPE", "NONE");
            xwsImpl = fileProps.getProperty("XMS_IMPL", "");
            xwsIface = fileProps.getProperty("XWS_IFACE", "");

            factoryClassName =
                    fileProps.getProperty("DAO_FACTORY_NAME", "DAOFactory");
            classWSInterfaceName =
                    fileProps.getProperty("WS_INTERFACE_NAME", "DAOFactoryServiceInterface");
            classWSImplName =
                    fileProps.getProperty("WS_IMPL_NAME", "DAOFactoryServiceImpl");

            connectionType =
                    fileProps.getProperty("DAO_CONNECTION_TYPE", SAAdminWrangler.CONNECTION_TYPE_HC);
            // connectionName = fileProps.getProperty("DAO_CONNECTION_NAME","jdbc:oracle:thin:<user>/<password>@localhost:1521:ORCL");
            connectionName =
                    fileProps.getProperty("DAO_CONNECTION_NAME", "DEFAULT_FROM_LOGIN");

            logType =
                    fileProps.getProperty("DAO_LOG_TYPE", SAAdminWrangler.LOG_TYPE_DESCRIPTIONS[0]);
            logName = fileProps.getProperty("DAO_LOG_NAME", "user.home");
            ejbType = fileProps.getProperty("DAO_EJB", "NONE");

            wsJavaNumberTypeComboBox =
                    fileProps.getProperty("WS_NUMBER_TYPE", WS_JAVA_NUMBER_TYPES[0]);
            // WS_RECORD_TYPE is DELIBERATELY NOT READ. It chose the visibility of a generated
            // record class's own attributes: "public, no access methods" emitted public fields,
            // "private, set & get methods" emitted PROTECTED ones -- the label was never accurate.
            //
            // Retired because it bought encapsulation on generated DTO classes, which is worth
            // approximately nothing, and paid for it with a code path that 39 of the 41 propfiles
            // never took. That minority path is exactly what hid bug F: the MCP emitter reached
            // past the accessors to assign the field directly, which compiled for every config but
            // the two using this setting, and failed there with "paramData has protected access".
            // A setting that makes a rare shape of emitted code is a trap whatever it is worth.
            //
            // Both GUIs dropped the control and normalise the value on save, but that alone left
            // it reachable by hand-editing a .pb2/.json -- and the web form binds the SESSION
            // schema, so a legacy value would survive every save untouched. Ignoring the key here
            // is what makes it unreachable: this is the only place it is read. Schema still
            // round-trips it so a legacy config converts .pb2 <-> .json losslessly; it simply has
            // no effect. Note MCP was never at risk from it -- the emitted RECORD_MAPPER uses
            // Visibility.ANY, so Jackson saw protected fields either way.
            wsRecTypeComboBox = WS_REC_TYPES[0];

            freezeCodeRootField = false;

            // This is used by the eclipse plugin to stop people changing the code base from its location
            // underneath the project
            if (fileProps.getProperty("CODE_BASE_DIRECTORY_FROZEN",
                    "NO").equals("YES")) {
                freezeCodeRootField = true;
            }

            // 11 09
            targetJVM = fileProps.getProperty("TARGET_JVM", System.getProperty("java.specification.version"));

            methodPlsqlPrefix = fileProps.getProperty("METHOD_PLSQL_PREFIX", "servicePlsql");
            methodSqlPrefix = fileProps.getProperty("METHOD_SQL_PREFIX", "serviceSql");
            postscriptName = fileProps.getProperty("POST_SCRIPT_NAME", "ob_postgen.bat");
            postscriptContent = fileProps.getProperty("POST_SCRIPT_CONTENT", "");
            extraClassCode = fileProps.getProperty("EXTRA_CLASS_CODE", "");

        } catch (Exception e) {
            warning("Problem retrieving configuration file: " + e.toString());
        }

        mrUiClassName = fileProps.getProperty("UINAME");

        // Eclipse 30 SQL path workaround
        ec30ProjectPath = fileProps.getProperty("EC30_PROJECT_PATH", "");
        ec30ProjectRelPath = fileProps.getProperty("EC30_REL_PATH", "");

        if (fileProps.getProperty("CLOSE_CONNECTIONS", "YES").equals("YES")) {
            closeConnectionFlag = true;
        } else {
            closeConnectionFlag = false;
        }

        if (fileProps.getProperty("COMMIT_CONNECTIONS", "YES").equals("YES")) {
            commitConnectionFlag = true;
        } else {
            commitConnectionFlag = false;
        }

        // DAO factory pooling. Off unless the config asks for it, because pooling moves the
        // transaction boundary to the borrow - see the Pooling tab.
        daoPoolFlag = fileProps.getProperty("DAO_POOL", "NO").equals("YES");
        daoPoolMaxSize = fileProps.getProperty("DAO_POOL_MAX_SIZE", DEFAULT_DAO_POOL_MAX_SIZE);
        daoPoolMinIdle = fileProps.getProperty("DAO_POOL_MIN_IDLE", DEFAULT_DAO_POOL_MIN_IDLE);
        daoPoolMaxWaitMs = fileProps.getProperty("DAO_POOL_MAX_WAIT_MS", DEFAULT_DAO_POOL_MAX_WAIT_MS);
        daoPoolIdleTimeoutMs =
                fileProps.getProperty("DAO_POOL_IDLE_TIMEOUT_MS", DEFAULT_DAO_POOL_IDLE_TIMEOUT_MS);
        daoPoolOnReturn = fileProps.getProperty("DAO_POOL_ON_RETURN", DAO_POOL_ON_RETURN_COMMIT);

    }

    public void setAdditionalObject(Object theObject) {
    }

    public void setUIName(String uiName) {
        this.uiName = uiName;
    }

    public void setIniFileName(String iniFileName) {
        this.iniFileName = iniFileName;
    }

    void setIniFile() {
        if (iniFileName != null && iniFileName.length() > 0) {
            // populate the ini file.
            FileOutputStream mrOutputStream;
            LookAndFeel mrLookAndFeel;
            try {
                mrLookAndFeel = UIManager.getLookAndFeel();
                fileProps.setProperty("UINAME", mrLookAndFeel.getName());
            } catch (Exception e) {
                fileProps.remove("UINAME");
            }

            try {
                info("Saving Properties to " + iniFileName);
                mrOutputStream = new FileOutputStream(iniFileName);
                if (isJsonConfig(iniFileName)) {
                    // Round-trip the properties through a Schema and write our JSON format.
                    byte[] json = new com.mcpdbwizard.schema.Schema(fileProps).toJson()
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    mrOutputStream.write(json);
                } else {
                    fileProps.store(mrOutputStream,
                            Namer.param_prod_name + " " + Namer.param_product_version + " Properties");
                }
                mrOutputStream.close();
            } catch (Exception e) {
                syserror("Unable to save configuration: " + e.toString(), true,
                        true);
            }

            // Now make a copy....
            try {

                File backupDir =
                        new File(logFileDirectory + File.separator + "OldIniFiles");

                if (mrStatusLog != null && mrStatusLog instanceof TextLog) {
                    TextLog tempLog = (TextLog) mrStatusLog;
                    String currentLogName = tempLog.getCurrentLog();
                    if (currentLogName != null && (!currentLogName.equals("NONE")) && (!currentLogName.equals("BROKEN"))) {
                        File newBackupDir =
                                new File(tempLog.getCurrentLog());
                        newBackupDir = newBackupDir.getParentFile();
                        backupDir =
                                new File(newBackupDir.getAbsolutePath() + File.separator + "OldIniFiles");

                    }

                }


                int howManyBackupFiles = 10;

                if (!backupDir.exists()) {
                    backupDir.mkdirs();
                }

                File[] backupFiles = new File[howManyBackupFiles];

                for (int i = 0; i < backupFiles.length; i++) {
                    backupFiles[i] =
                            new File(backupDir + File.separator + "Old_Ini_File_" +
                                    (i + 1) + FILE_EXTENSION);
                }

                // Delete oldest file....
                if (backupFiles[backupFiles.length - 1].exists()) {
                    debug("Deleting old ini file " +
                            backupFiles[backupFiles.length - 1].getAbsolutePath());
                    backupFiles[backupFiles.length - 1].delete();
                }

                for (int i = backupFiles.length - 1; i >= 0; i--) {
                    if (backupFiles[i].exists()) {
                        debug("Moving old config file " +
                                backupFiles[i].getAbsolutePath() + " to " +
                                backupFiles[i + 1].getAbsolutePath());
                        IOUtils.copyFile(backupFiles[i], backupFiles[i + 1]);
                        backupFiles[i].delete();
                    }
                }

                IOUtils.copyFile(new File(iniFileName), backupFiles[0]);
                info("Configuration saved to " + iniFileName + " and " +
                        backupFiles[0].getAbsolutePath());

                if (listener != null) {
                    McpDbWizardEvent obEvent =
                            new McpDbWizardEvent(McpDbWizardEvent.CONFIG_FILE_WRITTEN);
                    obEvent.setThing(new File(iniFileName));
                    listener.reportEvent(obEvent);
                }

            } catch (Exception e) {
                syserror("Unable to save backup copy of configuration: " +
                        e.toString(), true, true);
            }
        }
    }

    public void fillInDefaults(JTextField pIpField, JTextField pPortField,
                               JTextField pSidField, JTextField pUserField,
                               JTextField pPassField,
                               java.awt.Dimension pXYSize,
                               JTextField pAuthorField,
                               JTextField pVersionField,
                               JTextArea pCommentArea, JCheckBox pUserObjects,
                               JCheckBox pOtherObjects,
                               JTextField pOtherUserFilter,
                               JTextField pCodeRootField,
                               JTextField pPackageNameField,
                               JComboBox pOracleVersionComboBox,
                               JCheckBox pDebugMessagesCheckbox,
                               JCheckBox pOtherMessagesCheckbox,
                               JCheckBox pCommentsCheckBox,
                               JCheckBox pStatsCheckBox,
                               JComboBox pJavaAccessCombobox,
                               JComboBox pJavaNamingCombobox,
                               JTextField pSqlFileDirField,
                               JCheckBox pByteMethods, JCheckBox pShortMethods,
                               JCheckBox pIntMethods, JCheckBox pLongMethods,
                               JCheckBox pFloatMethods,
                               JCheckBox pDoubleMethods,
                               JCheckBox pByteObjectMethods,
                               JCheckBox pShortObjectMethods,
                               JCheckBox pIntObjectMethods,
                               JCheckBox pLongObjectMethods,
                               JCheckBox pFloatObjectMethods,
                               JCheckBox pDoubleObjectMethods,
                               JCheckBox pValidate, JCheckBox pExtraSql,
                               JTextField pDaoFactoryNameTextField,
                               JCheckBox pGenerateSessionBeanCheckBox,
                               JComboBox pLogTypeComboBox,
                               JTextField pLogNameTextField1,
                               JComboBox pConnectionTypeComboBox,
                               JTextField pConnectionNameTextField,
                               JCheckBox pWebServices,
                               JCheckBox pMcpServer,
                               JCheckBox pAddFinalizeMethodCheckBox,
                               JTextField pTempDirTextField,
                               JTextField pTempFilePrefixTextField,
                               JTextField pTempFileSuffixTextField,
                               JComboBox pXwsTypeName,
                               JTextField pXwsImpl,
                               JTextField pXwsIface,
                               JTextField pWsImplClassTextField,
                               JTextField pWsInterfaceClassTextField,
                               JCheckBox pWsPreCallCheckBox,
                               JCheckBox pWsPostCallCheckBox,
                               JCheckBox pWsAlwaysReleaseCheckBox,
                               JCheckBox pCloseConnectionsCheckBox,
                               JCheckBox pCommitConnectionsCheckBox,
                               JCheckBox pDaoPoolCheckBox,
                               JTextField pDaoPoolMaxSizeTextField,
                               JTextField pDaoPoolMinIdleTextField,
                               JTextField pDaoPoolMaxWaitTextField,
                               JTextField pDaoPoolIdleTimeoutTextField,
                               JComboBox pDaoPoolOnReturnComboBox,
                               JComboBox pWsJavaNumberTypeComboBox,
                               JComboBox pWsRecTypeComboBox,
                               JComboBox pJvmComboBox,
                               JTextField pMethodPlsqlTextField,
                               JTextField pMethodSqlTextField,
                               JTextField pPostScriptNameTextField,
                               JTextArea pPostScriptCodeTextArea,
                               JTextArea pExtraCodeTextArea) {
        pIpField.setText(ipField);
        pPortField.setText(portField);
        pSidField.setText(sidField);
        pUserField.setText(userField);
        pPassField.setText(passField);
        pXYSize.setSize(GUIUtils.StringToInt(x_size, DEFAULT_SCREEN_WIDTH),
                GUIUtils.StringToInt(y_size, DEFAULT_SCREEN_HEIGHT));

        pAuthorField.setText(authorField);
        pVersionField.setText(versionField);
        pCommentArea.setText(commentField);

        populateOracleVersionComboBox(pOracleVersionComboBox);
        populateJavaVersionComboBox(pJvmComboBox);
        populateJavaAccessComboBox(pJavaAccessCombobox);
        populateJavaNamingConventionComboBox(pJavaNamingCombobox);
        populateWsJavaNumberTypeComboBox(pWsJavaNumberTypeComboBox);
        populateWsRecTypeComboBox(pWsRecTypeComboBox);

        populateLogTypeComboBox(pLogTypeComboBox);
        populateConnectionTypeComboBox(pConnectionTypeComboBox);

        pOracleVersionComboBox.setSelectedItem(chosenOracleVersion);
        pJavaAccessCombobox.setSelectedItem(javaAccessComboBox);
        pJavaNamingCombobox.setSelectedItem(javanamingComboBox);
        pWsJavaNumberTypeComboBox.setSelectedItem(wsJavaNumberTypeComboBox);
        pWsRecTypeComboBox.setSelectedItem(wsRecTypeComboBox);

        pJvmComboBox.setSelectedItem(targetJVM);
        pMethodPlsqlTextField.setText(methodPlsqlPrefix);
        pMethodSqlTextField.setText(methodSqlPrefix);
        pPostScriptNameTextField.setText(postscriptName);
        pPostScriptCodeTextArea.setText(postscriptContent);

        pExtraCodeTextArea.setText(extraClassCode);

        if (debugMessagesCheckbox) {
            pDebugMessagesCheckbox.setSelected(true);
        } else {
            pDebugMessagesCheckbox.setSelected(false);
        }

        if (otherMessagesCheckbox) {
            pOtherMessagesCheckbox.setSelected(true);
        } else {
            pOtherMessagesCheckbox.setSelected(false);
        }

        if (commentsCheckBox) {
            pCommentsCheckBox.setSelected(true);
        } else {
            pCommentsCheckBox.setSelected(false);
        }

        if (userObjectsFlag) {
            pUserObjects.setSelected(true);
        } else {
            pUserObjects.setSelected(false);
        }

        if (otherObjectsFlag) {
            pOtherObjects.setSelected(true);
        } else {
            pOtherObjects.setSelected(false);
        }

        if (statsCheckBox) {
            pStatsCheckBox.setSelected(true);
        } else {
            pStatsCheckBox.setSelected(false);
        }

        pOtherUserFilter.setText(otherUserFilter);
        pCodeRootField.setText(codeRootField);
        pSqlFileDirField.setText(sqlDirField);
        pPackageNameField.setText(packageNameField);

        if (useByteCheckBox) {
            pByteMethods.setSelected(true);
        } else {
            pByteMethods.setSelected(false);
        }

        if (useShortCheckBox) {
            pShortMethods.setSelected(true);
        } else {
            pShortMethods.setSelected(false);
        }

        if (useIntCheckBox) {
            pIntMethods.setSelected(true);
        } else {
            pIntMethods.setSelected(false);
        }

        if (useLongCheckBox) {
            pLongMethods.setSelected(true);
        } else {
            pLongMethods.setSelected(false);
        }

        if (useFloatCheckBox) {
            pFloatMethods.setSelected(true);
        } else {
            pFloatMethods.setSelected(false);
        }

        if (useDoubleCheckBox) {
            pDoubleMethods.setSelected(true);
        } else {
            pDoubleMethods.setSelected(false);
        }

        if (useByteObjCheckBox) {
            pByteObjectMethods.setSelected(true);
        } else {
            pByteObjectMethods.setSelected(false);
        }

        if (useShortObjCheckBox) {
            pShortObjectMethods.setSelected(true);
        } else {
            pShortObjectMethods.setSelected(false);
        }

        if (useIntObjCheckBox) {
            pIntObjectMethods.setSelected(true);
        } else {
            pIntObjectMethods.setSelected(false);
        }

        if (useLongObjCheckBox) {
            pLongObjectMethods.setSelected(true);
        } else {
            pLongObjectMethods.setSelected(false);
        }

        if (useFloatObjCheckBox) {
            pFloatObjectMethods.setSelected(true);
        } else {
            pFloatObjectMethods.setSelected(false);
        }

        if (useDoubleObjCheckBox) {
            pDoubleObjectMethods.setSelected(true);
        } else {
            pDoubleObjectMethods.setSelected(false);
        }


        if (validateCheckBox) {
            pValidate.setSelected(true);
        } else {
            pValidate.setSelected(false);
        }


        if (extraSQLCheckBox) {
            pExtraSql.setSelected(true);
        } else {
            pExtraSql.setSelected(false);
        }

        pDaoFactoryNameTextField.setText(factoryClassName);
        if (ejbType.equals("SESSION")) {
            pGenerateSessionBeanCheckBox.setSelected(true);
        } else {
            pGenerateSessionBeanCheckBox.setSelected(false);
        }


        pLogTypeComboBox.setSelectedItem(logType);
        pLogNameTextField1.setText(logName);
        pConnectionTypeComboBox.setSelectedItem(connectionType);

        pConnectionNameTextField.setText(connectionName);

        if (finalizeMethodFlag) {
            pAddFinalizeMethodCheckBox.setSelected(true);
        } else {
            pAddFinalizeMethodCheckBox.setSelected(false);
        }

        pTempDirTextField.setText(tempDirName);
        pTempFilePrefixTextField.setText(tempPrefix);
        pTempFileSuffixTextField.setText(tempSuffix);

        pXwsTypeName.setSelectedItem(xwsTypeName);
        pXwsImpl.setText(xwsImpl);
        pXwsIface.setText(xwsIface);

        if (webServicesFlag) {
            pWebServices.setSelected(true);
        } else {
            pWebServices.setSelected(false);
        }

        pMcpServer.setSelected(mcpServerFlag);

        pWsImplClassTextField.setText(classWSImplName);
        pWsInterfaceClassTextField.setText(classWSInterfaceName);

        if (servicePreCallStubFlag) {
            pWsPreCallCheckBox.setSelected(true);
        } else {
            pWsPreCallCheckBox.setSelected(false);
        }

        if (servicePostCallStubFlag) {
            pWsPostCallCheckBox.setSelected(true);
        } else {
            pWsPostCallCheckBox.setSelected(false);
        }


        if (serviceAlwaysReleaseFlag) {
            pWsAlwaysReleaseCheckBox.setSelected(true);
        } else {
            pWsAlwaysReleaseCheckBox.setSelected(false);
        }


        if (closeConnectionFlag) {
            pCloseConnectionsCheckBox.setSelected(true);
        } else {
            pCloseConnectionsCheckBox.setSelected(false);
        }

        if (commitConnectionFlag) {
            pCommitConnectionsCheckBox.setSelected(true);
        } else {
            pCommitConnectionsCheckBox.setSelected(false);
        }

        populateDaoPoolOnReturnComboBox(pDaoPoolOnReturnComboBox);
        pDaoPoolCheckBox.setSelected(daoPoolFlag);
        pDaoPoolMaxSizeTextField.setText(daoPoolMaxSize);
        pDaoPoolMinIdleTextField.setText(daoPoolMinIdle);
        pDaoPoolMaxWaitTextField.setText(daoPoolMaxWaitMs);
        pDaoPoolIdleTimeoutTextField.setText(daoPoolIdleTimeoutMs);
        pDaoPoolOnReturnComboBox.setSelectedItem(daoPoolOnReturn);

    }

    /**
     * Read one pool size out of its text field, complaining and keeping the previous value if it is
     * not a whole number in range. Nothing else on these option pages validates, because every other
     * scalar is free text where any string is a legal answer; a pool size is not — an unusable one
     * stops the generated server starting at all, and finding that out at run time is far worse than
     * a dialog here.
     *
     * @return the accepted value, which is {@code theCurrentValue} when the entry was rejected
     */
    private String acceptPoolNumber(JTextField theField, String theLabel, int theMinimum,
                                    int theMaximum, String theCurrentValue) {
        String theEntry = (theField.getText() + "").trim();
        int theNumber;

        try {
            theNumber = Integer.parseInt(theEntry);
        } catch (NumberFormatException e) {
            warning(theLabel + " must be a whole number; '" + theEntry + "' has been ignored and the"
                    + " previous value (" + theCurrentValue + ") kept.");
            theField.setText(theCurrentValue);
            return theCurrentValue;
        }

        if (theNumber < theMinimum || theNumber > theMaximum) {
            warning(theLabel + " must be between " + theMinimum + " and " + theMaximum + "; "
                    + theNumber + " has been ignored and the previous value (" + theCurrentValue
                    + ") kept.");
            theField.setText(theCurrentValue);
            return theCurrentValue;
        }

        return Integer.toString(theNumber);
    }

    /** Offer both settle-on-return policies, defaulting to COMMIT if the config named neither. */
    private void populateDaoPoolOnReturnComboBox(JComboBox pDaoPoolOnReturnComboBox) {
        pDaoPoolOnReturnComboBox.removeAllItems();
        for (int seq = 0; seq < DAO_POOL_ON_RETURN_ACTIONS.length; seq++) {
            pDaoPoolOnReturnComboBox.addItem(DAO_POOL_ON_RETURN_ACTIONS[seq]);
        }
        pDaoPoolOnReturnComboBox.setSelectedItem(DAO_POOL_ON_RETURN_COMMIT);
    }

    public void saveNewDefaults(JTextField pIpField, JTextField pPortField,
                                JTextField pSidField, JTextField pUserField,
                                JTextField pPassField,
                                java.awt.Dimension pXYSize,
                                JTextField pAuthorField,
                                JTextField pVersionField,
                                JTextArea pCommentArea, JCheckBox pUserObjects,
                                JCheckBox pOtherObjects,
                                JTextField pOtherUserFilter,
                                JTextField pCodeRootField,
                                JTextField pPackageNameField,
                                JComboBox pOracleVersionComboBox,
                                JCheckBox pDebugMessagesCheckbox,
                                JCheckBox pOtherMessagesCheckbox,
                                JCheckBox pCommentsCheckBox,
                                JCheckBox pStatsCheckBox,
                                JComboBox pJavaAccessCombobox,
                                JComboBox pJavaNamingCombobox,
                                JTextField pSqlFileDirField,
                                JCheckBox pByteMethods,
                                JCheckBox pShortMethods, JCheckBox pIntMethods,
                                JCheckBox pLongMethods,
                                JCheckBox pFloatMethods,
                                JCheckBox pDoubleMethods,
                                JCheckBox pByteObjectMethods,
                                JCheckBox pShortObjectMethods,
                                JCheckBox pIntObjectMethods,
                                JCheckBox pLongObjectMethods,
                                JCheckBox pFloatObjectMethods,
                                JCheckBox pDoubleObjectMethods,
                                JCheckBox pValidate, JCheckBox pExtraSql,
                                JTextField pDaoFactoryNameTextField,
                                JCheckBox pGenerateSessionBeanCheckBox,
                                JComboBox pLogTypeComboBox,
                                JTextField pLogNameTextField1,
                                JComboBox pConnectionTypeComboBox,
                                JTextField pConnectionNameTextField,
                                JCheckBox pWebServices,
                                JCheckBox pMcpServer,
                                JCheckBox pAddFinalizeMethodCheckBox,
                                JTextField pTempDirTextField,
                                JTextField pTempFilePrefixTextField,
                                JTextField pTempFileSuffixTextField,
                                JComboBox pXwsType,
                                JTextField pXwsImpl,
                                JTextField pXwsIface,

                                JTextField pWsImplClassTextField,
                                JTextField pWsInterfaceClassTextField,
                                JCheckBox pWsPreCallCheckBox,
                                JCheckBox pWsPostCallCheckBox,
                                JCheckBox pWsAlwaysReleaseCheckBox,
                                JCheckBox pCloseConnectionsCheckBox,
                                JCheckBox pCommitConnectionsCheckBox,
                                JCheckBox pDaoPoolCheckBox,
                                JTextField pDaoPoolMaxSizeTextField,
                                JTextField pDaoPoolMinIdleTextField,
                                JTextField pDaoPoolMaxWaitTextField,
                                JTextField pDaoPoolIdleTimeoutTextField,
                                JComboBox pDaoPoolOnReturnComboBox,
                                JComboBox pWsJavaNumberTypeComboBox,
                                JComboBox pWsRecTypeComboBox,
                                JComboBox pJvmComboBox,
                                JTextField pMethodPlsqlTextField,
                                JTextField pMethodSqlTextField,
                                JTextField pPostScriptNameTextField,
                                JTextArea pPostScriptCodeTextArea,
                                JTextArea pExtraCodeTextArea) {
        try {
            fileProps = new Properties();

            fileProps.setProperty("TARGET_JVM", pJvmComboBox.getSelectedItem() + "");
            targetJVM = pJvmComboBox.getSelectedItem() + "";

            fileProps.setProperty("METHOD_PLSQL_PREFIX", pMethodPlsqlTextField.getText());
            methodPlsqlPrefix = pMethodPlsqlTextField.getText();

            fileProps.setProperty("METHOD_SQL_PREFIX", pMethodSqlTextField.getText());
            methodSqlPrefix = pMethodSqlTextField.getText();

            fileProps.setProperty("POST_SCRIPT_NAME", pPostScriptNameTextField.getText());
            postscriptName = pPostScriptNameTextField.getText();

            // String foo = pPostScriptCodeTextArea.getText();
            fileProps.setProperty("POST_SCRIPT_CONTENT", pPostScriptCodeTextArea.getText());
            postscriptContent = pPostScriptCodeTextArea.getText();

            fileProps.setProperty("EXTRA_CLASS_CODE", pExtraCodeTextArea.getText());
            extraClassCode = pExtraCodeTextArea.getText();

            fileProps.setProperty("WS_NUMBER_TYPE",
                    pWsJavaNumberTypeComboBox.getSelectedItem() +
                            "");
            wsJavaNumberTypeComboBox =
                    pWsJavaNumberTypeComboBox.getSelectedItem() + "";

            // WS_RECORD_TYPE is retired (see the read site): the protected-field variant made a
            // shape of emitted code only two propfiles ever produced, which is where bug F hid.
            // Saving normalises any legacy value to the public form.
            fileProps.setProperty("WS_RECORD_TYPE", WS_REC_TYPES[0]);
            wsRecTypeComboBox = WS_REC_TYPES[0];

            if (pWebServices.isSelected()) {
                fileProps.setProperty("WEB_SERVICES", "YES");
                webServicesFlag = true;
            } else {
                fileProps.setProperty("WEB_SERVICES", "NO");
                webServicesFlag = false;
            }

            if (pMcpServer.isSelected()) {
                fileProps.setProperty("MCP_SERVER", "YES");
                mcpServerFlag = true;
            } else {
                fileProps.setProperty("MCP_SERVER", "NO");
                mcpServerFlag = false;
            }

            // MCP HTTP bearer-auth flag has no GUI control; preserve it across a save (write only
            // when enabled so propfiles that never used it stay unchanged).
            if (mcpHttpTokenFlag) {
                fileProps.setProperty("MCP_HTTP_TOKEN", "YES");
            }

            // MCP HTTPS/TLS flag likewise has no GUI control; preserve it across a save.
            if (mcpHttpsFlag) {
                fileProps.setProperty("MCP_HTTPS", "YES");
            }
            if (mcpOAuthFlag) {
                fileProps.setProperty("MCP_OAUTH", "YES");
            }

            // Prometheus flag likewise has no Swing control; preserve it across a save.
            if (prometheusServerFlag) {
                fileProps.setProperty("PROMETHEUS_SERVER", "YES");
            }

            // Likewise no Swing control. Written only when present, so a config that never
            // set it does not gain an empty key on its first save.
            if (mcpInstructions != null) {
                fileProps.setProperty("MCP_INSTRUCTIONS", mcpInstructions);
            }

            fileProps.setProperty("HOSTNAME", pIpField.getText() + "");
            ipField = pIpField.getText();

            fileProps.setProperty("PORT", pPortField.getText() + "");
            portField = pPortField.getText();

            fileProps.setProperty("ORACLE_SID", pSidField.getText() + "");
            sidField = pSidField.getText();

            fileProps.setProperty("USER", pUserField.getText() + "");
            userField = pUserField.getText();

            fileProps.setProperty("PASS", pPassField.getText() + "");
            passField = pPassField.getText();

            fileProps.setProperty("X_SIZE", mrFrame.getWidth() + "");
            x_size = mrFrame.getWidth() + "";

            fileProps.setProperty("Y_SIZE", mrFrame.getHeight() + "");
            y_size = mrFrame.getHeight() + "";

            fileProps.setProperty("AUTHOR", pAuthorField.getText() + "");
            authorField = pAuthorField.getText();

            fileProps.setProperty("VERSION", pVersionField.getText() + "");
            versionField = pVersionField.getText();

            fileProps.setProperty("COMMENT", pCommentArea.getText() + "");
            commentField = pCommentArea.getText();

            fileProps.setProperty("CODE_BASE_DIRECTORY",
                    pCodeRootField.getText() + "");
            codeRootField = pCodeRootField.getText();

            fileProps.setProperty("SQL_FILE_DIRECTORY",
                    pSqlFileDirField.getText() + "");
            sqlDirField = pSqlFileDirField.getText();

            fileProps.setProperty("PACKAGE_NAME",
                    pPackageNameField.getText() + "");
            packageNameField = pPackageNameField.getText();

            fileProps.setProperty("ORACLE_VERSION",
                    (String) pOracleVersionComboBox.getSelectedItem() +
                            "");
            chosenOracleVersion =
                    (String) pOracleVersionComboBox.getSelectedItem();

            fileProps.setProperty("JAVA_ACCESS_TYPE",
                    (String) pJavaAccessCombobox.getSelectedItem() +
                            "");
            javaAccessComboBox = (String) pJavaAccessCombobox.getSelectedItem();

            fileProps.setProperty("JAVA_NAMING_CONVENTION",
                    (String) pJavaNamingCombobox.getSelectedItem() +
                            "");
            javanamingComboBox = (String) pJavaNamingCombobox.getSelectedItem();

            if (pDebugMessagesCheckbox.isSelected()) {
                fileProps.setProperty("DEBUG_MESSAGES", "YES");
                debugMessagesCheckbox = true;
            } else {
                fileProps.setProperty("DEBUG_MESSAGES", "NO");
                debugMessagesCheckbox = false;
            }

            if (pOtherMessagesCheckbox.isSelected()) {
                fileProps.setProperty("OTHER_MESSAGES", "YES");
                otherMessagesCheckbox = true;
            } else {
                fileProps.setProperty("OTHER_MESSAGES", "NO");
                otherMessagesCheckbox = false;
            }

            if (pStatsCheckBox.isSelected()) {
                fileProps.setProperty("CODE_STATISTICS", "YES");
                statsCheckBox = true;
            } else {
                fileProps.setProperty("CODE_STATISTICS", "NO");
                statsCheckBox = false;
            }

            if (pCommentsCheckBox.isSelected()) {
                fileProps.setProperty("CODE_COMMENTS", "YES");
                commentsCheckBox = true;
            } else {
                fileProps.setProperty("CODE_COMMENTS", "NO");
                commentsCheckBox = false;
            }

            if (pUserObjects.isSelected()) {
                fileProps.setProperty("USER_OBJECTS", "YES");
                userObjectsFlag = true;
            } else {
                fileProps.setProperty("USER_OBJECTS", "NO");
                userObjectsFlag = false;
            }

            if (pOtherObjects.isSelected()) {
                fileProps.setProperty("OTHER_OBJECTS", "YES");
                otherObjectsFlag = true;
            } else {
                fileProps.setProperty("OTHER_OBJECTS", "NO");
                userObjectsFlag = false;
            }

            if (pByteMethods.isSelected()) {
                fileProps.setProperty("METHODS_BYTE", "YES");
                useByteCheckBox = true;
            } else {
                fileProps.setProperty("METHODS_BYTE", "NO");
                useByteCheckBox = false;
            }

            if (pShortMethods.isSelected()) {
                fileProps.setProperty("METHODS_SHORT", "YES");
                useShortCheckBox = true;
            } else {
                fileProps.setProperty("METHODS_SHORT", "NO");
                useShortCheckBox = false;
            }


            if ((!pIntMethods.isSelected()) && webServicesFlag &&
                    wsJavaNumberTypeComboBox.equals("int")) {
                pIntMethods.setSelected(true);
                warning("Get and Set methods will be created for 'int' because the web service code is using 'int' to represent numbers.",
                        true, true);
            }

            if (pIntMethods.isSelected()) {
                fileProps.setProperty("METHODS_INT", "YES");
                useIntCheckBox = true;
            } else {
                fileProps.setProperty("METHODS_INT", "NO");
                useIntCheckBox = false;
            }

            if ((!pLongMethods.isSelected()) && webServicesFlag &&
                    wsJavaNumberTypeComboBox.equals("long")) {
                pLongMethods.setSelected(true);
                warning("Get and Set methods will be created for 'long' because the web service code is using 'long' to represent numbers.",
                        true, true);
            }

            if (pLongMethods.isSelected()) {
                fileProps.setProperty("METHODS_LONG", "YES");
                useLongCheckBox = true;
            } else {
                fileProps.setProperty("METHODS_LONG", "NO");
                useLongCheckBox = false;
            }

            if ((!pFloatMethods.isSelected()) && webServicesFlag &&
                    wsJavaNumberTypeComboBox.equals("float")) {
                pFloatMethods.setSelected(true);
                warning("Get and Set methods will be created for 'float' because the web service code is using 'float' to represent numbers.",
                        true, true);
            }

            if (pFloatMethods.isSelected()) {
                fileProps.setProperty("METHODS_FLOAT", "YES");
                useFloatCheckBox = true;
            } else {
                fileProps.setProperty("METHODS_FLOAT", "NO");
                useFloatCheckBox = false;
            }

            if ((!pDoubleMethods.isSelected()) && webServicesFlag &&
                    wsJavaNumberTypeComboBox.equals("double")) {
                pDoubleMethods.setSelected(true);
                warning("Get and Set methods will be created for 'double' because the web service code is using 'double' to represent numbers.",
                        true, true);
            }

            if (pDoubleMethods.isSelected()) {
                fileProps.setProperty("METHODS_DOUBLE", "YES");
                useDoubleCheckBox = true;
            } else {
                fileProps.setProperty("METHODS_DOUBLE", "NO");
                useDoubleCheckBox = false;
            }

            if (pByteObjectMethods.isSelected()) {
                fileProps.setProperty("METHODS_BYTE_OBJ", "YES");
                useByteObjCheckBox = true;
            } else {
                fileProps.setProperty("METHODS_BYTE_OBJ", "NO");
                useByteObjCheckBox = false;
            }

            if (pShortObjectMethods.isSelected()) {
                fileProps.setProperty("METHODS_SHORT_OBJ", "YES");
                useShortObjCheckBox = true;
            } else {
                fileProps.setProperty("METHODS_SHORT_OBJ", "NO");
                useShortObjCheckBox = false;
            }

            if (pIntObjectMethods.isSelected()) {
                fileProps.setProperty("METHODS_INT_OBJ", "YES");
                useIntObjCheckBox = true;
            } else {
                fileProps.setProperty("METHODS_INT_OBJ", "NO");
                useIntObjCheckBox = false;
            }

            if (pLongObjectMethods.isSelected()) {
                fileProps.setProperty("METHODS_LONG_OBJ", "YES");
                useLongObjCheckBox = true;
            } else {
                fileProps.setProperty("METHODS_LONG_OBJ", "NO");
                useLongObjCheckBox = false;
            }

            if (pFloatObjectMethods.isSelected()) {
                fileProps.setProperty("METHODS_FLOAT_OBJ", "YES");
                useFloatObjCheckBox = true;
            } else {
                fileProps.setProperty("METHODS_FLOAT_OBJ", "NO");
                useFloatObjCheckBox = false;
            }

            if (pDoubleObjectMethods.isSelected()) {
                fileProps.setProperty("METHODS_DOUBLE_OBJ", "YES");
                useDoubleObjCheckBox = true;
            } else {
                fileProps.setProperty("METHODS_DOUBLE_OBJ", "NO");
                useDoubleObjCheckBox = false;
            }

            if (pValidate.isSelected()) {
                fileProps.setProperty("VALIDATE", "YES");
                validateCheckBox = true;
            } else {
                fileProps.setProperty("VALIDATE", "NO");
                validateCheckBox = false;
            }

            if (pExtraSql.isSelected()) {
                fileProps.setProperty("EXTRA_SQL", "YES");
                extraSQLCheckBox = true;
            } else {
                fileProps.setProperty("EXTRA_SQL", "NO");
                extraSQLCheckBox = false;
            }


            fileProps.setProperty("OTHER_USER_NAME",
                    pOtherUserFilter.getText() + "");
            otherUserFilter = pOtherUserFilter.getText();

            fileProps.setProperty("PRODUCT_VERSION", Namer.param_product_version);
            fileProps.setProperty("PRODUCT_BUILD", Namer.param_build);

            fileProps.setProperty("CODE_BASE_DIRECTORY_FROZEN", "NO");

            // ec30 sql dir workarounde
            fileProps.setProperty("EC30_PROJECT_PATH", ec30ProjectPath);
            fileProps.setProperty("EC30_REL_PATH", ec30ProjectRelPath);

            // dao factory

            fileProps.setProperty("DAO_FACTORY_NAME",
                    pDaoFactoryNameTextField.getText() + "");
            factoryClassName = pDaoFactoryNameTextField.getText() + "";

            if (pGenerateSessionBeanCheckBox.isSelected()) {
                fileProps.setProperty("DAO_EJB", "SESSION");
                ejbType = "SESSION";
            } else {
                fileProps.setProperty("DAO_EJB", "NONE");
                ejbType = "NONE";
            }

            fileProps.setProperty("DAO_CONNECTION_TYPE",
                    (String) pConnectionTypeComboBox.getSelectedItem() +
                            "");
            connectionType =
                    (String) pConnectionTypeComboBox.getSelectedItem() + "";

            fileProps.setProperty("DAO_CONNECTION_NAME",
                    pConnectionNameTextField.getText() + "");
            connectionName = pConnectionNameTextField.getText() + "";

            fileProps.setProperty("DAO_LOG_TYPE",
                    (String) pLogTypeComboBox.getSelectedItem() +
                            "");
            logType = (String) pLogTypeComboBox.getSelectedItem() + "";

            fileProps.setProperty("DAO_LOG_NAME",
                    pLogNameTextField1.getText() + "");
            logName = pLogNameTextField1.getText() + "";


            // JCheckBox           pAddFinalizeMethodCheckBox
            if (pAddFinalizeMethodCheckBox.isSelected()) {
                fileProps.setProperty("DAO_FINALIZE", "YES");
                finalizeMethodFlag = true;
            } else {
                fileProps.setProperty("DAO_FINALIZE", "NO");
                finalizeMethodFlag = false;
            }

            // JTextField          pTempDirTextField
            fileProps.setProperty("DEFAULT_TEMPDIR",
                    (String) pTempDirTextField.getText() + "");
            tempDirName = (String) pTempDirTextField.getText() + "";

            // JTextField          pTempFilePrefixTextField
            fileProps.setProperty("DEFAULT_TEMP_PREFIX",
                    (String) pTempFilePrefixTextField.getText() +
                            "");
            tempPrefix = (String) pTempFilePrefixTextField.getText() + "";

            // JTextField          pTempFileSuffixTextField
            fileProps.setProperty("DEFAULT_TEMP_SUFFIX",
                    (String) pTempFileSuffixTextField.getText() +
                            "");
            tempSuffix = (String) pTempFileSuffixTextField.getText() + "";


            // JTextField          pXwsType
            fileProps.setProperty("XWS_TYPE",
                    (String) pXwsType.getSelectedItem() + "");
            xwsTypeName = (String) pXwsType.getSelectedItem() + "";

            // JTextField          pXwsImpl
            fileProps.setProperty("XMS_IMPL",
                    (String) pXwsImpl.getText() + "");
            xwsImpl = (String) pXwsImpl.getText() + "";

            // JTextField          pXwsIface
            fileProps.setProperty("XWS_IFACE",
                    (String) pXwsIface.getText() + "");
            xwsIface = (String) pXwsIface.getText() + "";

            // JTextField          pWsImplClassTextField
            fileProps.setProperty("WS_IMPL_NAME",
                    (String) pWsImplClassTextField.getText() +
                            "");
            classWSImplName = (String) pWsImplClassTextField.getText() + "";

            // JTextField          pWsInterfaceClassTextField
            fileProps.setProperty("WS_INTERFACE_NAME",
                    (String) pWsInterfaceClassTextField.getText() +
                            "");
            classWSInterfaceName =
                    (String) pWsInterfaceClassTextField.getText() + "";

            // JCheckBox           pWsPreCallCheckBox
            if (pWsPreCallCheckBox.isSelected()) {
                fileProps.setProperty("WS_PRE_CALL_STUB", "YES");
                servicePreCallStubFlag = true;
            } else {
                fileProps.setProperty("WS_PRE_CALL_STUB", "NO");
                servicePreCallStubFlag = false;
            }


            // JCheckBox           pWsPostCallCheckBox
            if (pWsPostCallCheckBox.isSelected()) {
                fileProps.setProperty("WS_POST_CALL_STUB", "YES");
                servicePostCallStubFlag = true;
            } else {
                fileProps.setProperty("WS_POST_CALL_STUB", "NO");
                servicePostCallStubFlag = false;
            }


            // JCheckBox           pWsAlwaysReleaseCheckBox
            if (pWsAlwaysReleaseCheckBox.isSelected()) {
                fileProps.setProperty("WS_ALWAYS_RELEASE", "YES");
                serviceAlwaysReleaseFlag = true;
            } else {
                fileProps.setProperty("WS_ALWAYS_RELEASE", "NO");
                serviceAlwaysReleaseFlag = false;
            }


            // The "Uploaded BFILE naming is abstract" option no longer has a GUI
            // control. It made the generated ServiceImpl abstract, which an
            // MCP_SERVER=YES config cannot use: the emitted server instantiates
            // that class directly (`new <Factory>ServiceImpl(...)`), so the two
            // settings together produced code that would not compile. Saving
            // normalises any legacy YES to NO.
            fileProps.setProperty("WEB_SERVICES_ABSTRACT_BFILE", "NO");
            webServicesBfilesAreAbstractFlag = false;

            // JCheckBox           pCloseConnectionsCheckBox
            if (pCloseConnectionsCheckBox.isSelected()) {
                fileProps.setProperty("CLOSE_CONNECTIONS", "YES");
                closeConnectionFlag = true;
            } else {
                fileProps.setProperty("CLOSE_CONNECTIONS", "NO");
                closeConnectionFlag = false;
            }

            // JCheckBox           pCommitConnectionsCheckBox
            if (pCommitConnectionsCheckBox.isSelected()) {
                fileProps.setProperty("COMMIT_CONNECTIONS", "YES");
                commitConnectionFlag = true;
            } else {
                fileProps.setProperty("COMMIT_CONNECTIONS", "NO");
                commitConnectionFlag = false;
            }

            // ---- the Pooling tab ----
            daoPoolFlag = pDaoPoolCheckBox.isSelected();
            fileProps.setProperty("DAO_POOL", daoPoolFlag ? "YES" : "NO");

            // Sizes are checked here rather than at generation time so the number that is wrong is
            // still on screen next to the complaint. A rejected value keeps its previous setting.
            daoPoolMaxSize = acceptPoolNumber(pDaoPoolMaxSizeTextField, "Maximum pool size",
                    1, Integer.MAX_VALUE, daoPoolMaxSize);
            daoPoolMinIdle = acceptPoolNumber(pDaoPoolMinIdleTextField, "Minimum idle factories",
                    0, Integer.MAX_VALUE, daoPoolMinIdle);
            daoPoolMaxWaitMs = acceptPoolNumber(pDaoPoolMaxWaitTextField, "Maximum wait (ms)",
                    0, Integer.MAX_VALUE, daoPoolMaxWaitMs);
            daoPoolIdleTimeoutMs = acceptPoolNumber(pDaoPoolIdleTimeoutTextField, "Idle timeout (ms)",
                    1, Integer.MAX_VALUE, daoPoolIdleTimeoutMs);

            // A floor above the ceiling can never be honoured, and the pool would refuse to start.
            if (GUIUtils.StringToInt(daoPoolMinIdle, 0) > GUIUtils.StringToInt(daoPoolMaxSize, 1)) {
                warning("Minimum idle factories (" + daoPoolMinIdle + ") cannot exceed the maximum pool"
                        + " size (" + daoPoolMaxSize + "); the minimum has been reset to 0.");
                daoPoolMinIdle = DEFAULT_DAO_POOL_MIN_IDLE;
                pDaoPoolMinIdleTextField.setText(daoPoolMinIdle);
            }

            daoPoolOnReturn = pDaoPoolOnReturnComboBox.getSelectedItem() + "";

            fileProps.setProperty("DAO_POOL_MAX_SIZE", daoPoolMaxSize);
            fileProps.setProperty("DAO_POOL_MIN_IDLE", daoPoolMinIdle);
            fileProps.setProperty("DAO_POOL_MAX_WAIT_MS", daoPoolMaxWaitMs);
            fileProps.setProperty("DAO_POOL_IDLE_TIMEOUT_MS", daoPoolIdleTimeoutMs);
            fileProps.setProperty("DAO_POOL_ON_RETURN", daoPoolOnReturn);


            if (sequenceModel != null) {
                if (listener != null) {
                    McpDbWizardEvent obEvent = new McpDbWizardEvent(McpDbWizardEvent.SEQUENCE_TABLE_ABOUT_TO_SAVE);
                    obEvent.setThing(sequenceModel);

                    listener.reportEvent(obEvent);
                }
                sequenceModel.writeSequenceInfo(fileProps);
            }

            if (functionModel != null) {
                if (listener != null) {
                    McpDbWizardEvent obEvent = new McpDbWizardEvent(McpDbWizardEvent.FUNCTION_TABLE_ABOUT_TO_SAVE);
                    obEvent.setThing(functionModel);

                    listener.reportEvent(obEvent);
                }
                functionModel.writeFunctionInfo(fileProps);
            }

            if (tableModel != null) {
                if (listener != null) {
                    McpDbWizardEvent obEvent = new McpDbWizardEvent(McpDbWizardEvent.TABLE_TABLE_ABOUT_TO_SAVE);
                    obEvent.setThing(tableModel);

                    listener.reportEvent(obEvent);
                }
                tableModel.writeTableInfo(fileProps);
            }

            if (aspStatementWranglerManager != null) {
                for (int i = 0; i < aspStatementWranglerManager.length; i++) {
                    if (aspStatementWranglerManager[i] != null) {
                        aspStatementWranglerManager[i].writeProperties(fileProps,
                                i);
                    }
                }
            }
        } catch (Exception e) {
            syserror("Unable to save Configuration " + e.toString(), true,
                    true);
        }

        setIniFile();

    }

    void setLookAndFeel() {

        try {
            if (!uiName.equals("JDEV")) {
                for (int i = 0; i < lookAndFeelInfoArray.length; i++) {
                    if (mrUiClassName.equals(lookAndFeelInfoArray[i].getName())) {
                        debug("Setting UI to " + mrUiClassName);
                        UIManager.setLookAndFeel(lookAndFeelInfoArray[i].getClassName());

                        // Switch all component UI's
                        SwingUtilities.updateComponentTreeUI(mrFrame);
                        mrFrame.invalidate();
                        mrFrame.validate();
                        mrFrame.repaint();
                        break;
                    }
                }
            }
            if (uiName.equals("EC33")) {

                debug("Setting UI to " + mrUiClassName);
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

                // Switch all component UI's
                SwingUtilities.updateComponentTreeUI(mrFrame);
                mrFrame.invalidate();
                mrFrame.validate();
                mrFrame.repaint();


            }
        } catch (Exception e) {
            error("Unable to load saved Look and Feel");
        }

    }

    public void seekScrollPane() {
        try {
            if (mrFrame != null) {
                JScrollPane thePane = mrFrame.getOutputAreaScrollPane();
                JViewport theViewPort = thePane.getViewport();
                Point thePoint = theViewPort.getViewPosition();
                thePoint.setLocation(thePoint.getX(), 200000);
                theViewPort.setViewPosition(thePoint);
            }
        } catch (Exception e) {
            System.err.println("seekScrollPane:" + e.toString());
        }

    }

    public void setLog(LogInterface newLog) {
        mrStatusLog = newLog;
    }

    public void flush() {
        mrStatusLog.flush();
    }

    public void debugOn() {
        mrStatusLog.debugOn();
    }

    public void debugOff() {
        mrStatusLog.debugOff();
    }

    public boolean getDebug() {
        return (mrStatusLog.getDebug());
    }

    public void debug(String theMessage) {
        mrStatusLog.debug(theMessage);
        seekScrollPane();
    }

    public void debug(String theMessage, boolean isModal, boolean isLogged) {
        mrStatusLog.debug(theMessage, isModal, isLogged);
        seekScrollPane();
    }

    /**
     * Warn about every {@code SQL_FILENAME_<i>} the config names that the SQL directory does not
     * hold.
     *
     * <p><b>Why this is needed at all.</b> The statement list is built by SCANNING the directory and
     * matching what it finds back to the config's keys — the config's list never drives anything. A
     * named file that is absent therefore produces no {@code SqlStatementWrangler}, no entry in the
     * array, and nothing downstream can mention it: not the per-file "is not usable" warnings above
     * (they iterate files that exist), and not the MCP "yields no tool" report (it iterates the same
     * array). Generation returns 0 and the statement is simply gone. Measured across the committed
     * propfiles when this was written: 84 such references in 32 of 41 configs, ten of them with
     * {@code SQL_CREATE_CLASS=YES} — a class the author asked for and did not get.
     *
     * <p><b>The create flag is in the message on purpose.</b> It is what separates "you will not get
     * the class you asked for" from an inert leftover, and most of the 84 are the latter. A warning
     * that cannot be triaged gets ignored wholesale, which would waste the ten that matter.
     *
     * <p><b>Silent when the directory itself was unusable.</b> That is already reported once, and
     * following it with one line per named file would bury it under its own consequence.
     *
     * @param theDirectoryName  the directory that was searched, named in the message because the
     *                          usual cause is a config pointed at the wrong one rather than a
     *                          genuinely missing file
     * @param theFilesFound     what the scan returned; empty means the directory was empty or bad
     * @param theMissingLimit   how far past a gap to keep looking for indexed keys, matching the
     *                          scan the selection loop above already uses
     */
    private void reportSqlFilesNamedButNotFound(String theDirectoryName, String[] theFilesFound,
                                                int theMissingLimit) {
        if (theFilesFound == null || theFilesFound.length == 0) {
            return;
        }
        java.util.Set<String> theFound = new java.util.HashSet<String>();
        for (int i = 0; i < theFilesFound.length; i++) {
            theFound.add(theFilesFound[i]);
        }

        for (String[] theMiss : sqlFilesNamedButNotFound(fileProps, theFound, theMissingLimit)) {
            String theName = theMiss[0];
            boolean theCreateWasAsked = "YES".equals(theMiss[1]);
            warning("SQL file '" + theName + "' is named by this configuration but is not in "
                    + theDirectoryName + ", so no statement was read for it"
                    + (theCreateWasAsked
                    ? " -- and it asked for a class, which will NOT be generated."
                    : " (it did not ask for a class, so nothing is generated either way)."),
                    false, true);
            // Machine-readable sibling, in the format the description editor already parses, so a
            // description written against this statement is marked as going nowhere. Same reasoning
            // as the MCP report: the prose names it for a human, this names it the way the CONFIG
            // does.
            mrStatusLog.info(SAAdminWrangler.MCP_UNEXPOSED_PREFIX + "sql|" + theName + "||"
                    + "its file is not in " + theDirectoryName);
        }
    }

    /**
     * The pure half of {@link #reportSqlFilesNamedButNotFound}: which named files are absent.
     *
     * <p>Separated so it can be tested without a Swing shell, a config on disk or a database — the
     * reporting half is three lines of string building, the interesting half is this walk. Package
     * private for the test.
     *
     * @return one {@code {filename, createClassFlag}} per miss, in config order
     */
    static java.util.List<String[]> sqlFilesNamedButNotFound(java.util.Properties theConfig,
                                                             java.util.Set<String> theFilesFound,
                                                             int theMissingLimit) {
        java.util.List<String[]> theMisses = new java.util.ArrayList<String[]>();
        int theIndex = 0;
        int theMissingRun = 0;
        // Indices are not guaranteed contiguous, so keep looking past a gap -- the same tolerance
        // the selection loop uses, and for the same reason.
        while (theMissingRun <= theMissingLimit) {
            String theName = theConfig.getProperty(SqlStatementWrangler.SQL_FILENAME + theIndex);
            if (theName == null) {
                theMissingRun++;
            } else {
                theMissingRun = 0;
                if (!theFilesFound.contains(theName)) {
                    theMisses.add(new String[] {theName, theConfig.getProperty(
                            SqlStatementWrangler.SQL_CREATE_CLASS + theIndex)});
                }
            }
            theIndex++;
        }
        return theMisses;
    }

    public void info(String theMessage) {
        mrStatusLog.info(theMessage);
        seekScrollPane();
    }

    public void info(String theMessage, boolean isModal, boolean isLogged) {
        mrStatusLog.info(theMessage, isModal, isLogged);
        seekScrollPane();
    }

    public void warning(String theMessage) {
        mrStatusLog.warning(theMessage);
        seekScrollPane();
    }

    public void warning(String theMessage, boolean isModal, boolean isLogged) {
        mrStatusLog.warning(theMessage, isModal, isLogged);
        seekScrollPane();
    }

    public void error(String theMessage) {
        mrStatusLog.error(theMessage);
        seekScrollPane();
    }

    public void error(String theMessage, boolean isModal, boolean isLogged) {
        mrStatusLog.error(theMessage, isModal, isLogged);
        seekScrollPane();
    }

    public void error(Exception theException) {
        mrStatusLog.error(theException);
        seekScrollPane();
    }

    public void error(Exception theException, boolean isModal,
                      boolean isLogged) {
        mrStatusLog.error(theException, isModal, isLogged);
        seekScrollPane();
    }

    public String getCurrentLog() {
        return (mrStatusLog.getCurrentLog());
    }

    public String getDateFormat() {
        return (mrStatusLog.getDateFormat());
    }

    /**
     * Controls whether the class should log every message unless told not to.
     *
     * @param  logEveryMessageByDefault <code>true</code> if messages are to be logged by default.
     */
    public void setAutoLog(boolean logEveryMessageByDefault) {
        mrStatusLog.setAutoLog(logEveryMessageByDefault);
    }

    /**
     * Controls whether the class should flush the log every time it is asked to print a message. Slower but more reliable.
     *
     * @param  flushEveryMessage <code>true</code> if the log is to be flushed every time a message is written to it.
     */
    public void setAutoFlush(boolean flushEveryMessage) {
        mrStatusLog.setAutoFlush(flushEveryMessage);
    }

    public void syserror(String theMessage) {
        mrStatusLog.syserror(theMessage, false, true);
    }

    public void syserror(String theMessage, boolean isModal,
                         boolean isLogged) {
        mrStatusLog.syserror(theMessage, isModal, isLogged);
    }

    public void syserror(Exception theException) {
        mrStatusLog.syserror(theException);
    }

    public void syserror(Exception theException, boolean isModal,
                         boolean isLogged) {
        mrStatusLog.syserror(theException, isModal, isLogged);
    }

    boolean generateCodeFromGUI(JTextField codeRootField,
                                JTextField packageNameField,
                                JTextField authorField,
                                JTextField versionField,
                                JTextArea commentTextArea,
                                JTextField sqlDirField) {
        boolean retCode = true;

        File codeRoot = new File(codeRootField.getText());
        if (codeRootField.getText().length() == 0) {
            codeRoot = null;
        }

        File sqlDir = new File(sqlDirField.getText());
        if (sqlDirField.getText().length() == 0) {
            sqlDir = null;
        }

        String packageName = packageNameField.getText();
        String authorName = authorField.getText();
        String versionName = versionField.getText();
        String commentField = commentTextArea.getText();

        ((UiLog) mrStatusLog).clearJTextArea();
        ((UiLog) mrStatusLog).useJTextArea(true);

        retCode =
                generateCode(codeRoot, packageName, authorName, versionName, commentField,
                        sqlDir);

        ((UiLog) mrStatusLog).useJTextArea(false);

        return (retCode);
    }

    boolean generateCode(File codeRoot, String packageNameString,
                         String authorNameString, String versionNameString,
                         String commentNameString, File sqlDir) {
        {
            try {
                checkStringNotNull(packageNameString, "Java Package Name");
                checkStringNotNull(javanamingComboBox,
                        "Java Naming Convention");
                checkStringNotNull(chosenOracleVersion, "Oracle Version");

                if (factoryClassName != null && classWSInterfaceName != null &&
                        factoryClassName.equalsIgnoreCase(classWSInterfaceName)) {
                    throw new CSException("Generation failed - DAOFactory class and Web Services interface class have same name");
                } else if (factoryClassName != null &&
                        classWSImplName != null &&
                        factoryClassName.equalsIgnoreCase(classWSImplName)) {
                    throw new CSException("Generation failed - DAOFactory class and Web Services implementation class have same name");
                } else if (classWSInterfaceName != null &&
                        classWSImplName != null &&
                        classWSInterfaceName.equalsIgnoreCase(classWSImplName)) {
                    throw new CSException("Generation failed - Web Services interface class and Web Services implementation class have same name");
                } else if (methodPlsqlPrefix != null &&
                        methodPlsqlPrefix.indexOf(" ") > -1) {
                    throw new CSException("PL/SQL Method prefix  of '" + methodPlsqlPrefix + "' can not have embedded spaces");
                } else if (methodSqlPrefix != null &&
                        methodSqlPrefix.indexOf(" ") > -1) {
                    throw new CSException("SQL Method prefix of '" + methodPlsqlPrefix + "' can not have embedded spaces");
                } else if (daoPoolFlag
                        && SAAdminWrangler.CONNECTION_TYPE_JBOSERVER.equals(connectionType)) {
                    // BC4J owns the transaction through its DBTransaction, so a pool settling the
                    // transaction on the way back in would be fighting the framework for it.
                    throw new CSException("Generation failed - DAO factory pooling cannot be used with"
                            + " the " + SAAdminWrangler.CONNECTION_TYPE_JBOSERVER + " connection type,"
                            + " which manages transactions itself. Turn pooling off on the Pooling tab.");
                }
                try {

                } catch (Exception e2) {
                    error("URL Pinger Failed to set with Exception:");
                    error(e2);
                } catch (Error e3) {
                    error("URL Pinger Failed to set  with Error:");
                    error(e3.getMessage());
                }


                boolean bldStatus =
                        mrWrangler.generateCodeV3(codeRoot, packageNameString,
                                authorNameString,
                                versionNameString,
                                commentNameString,
                                javaAccessComboBox,
                                debugMessagesCheckbox,
                                otherMessagesCheckbox,
                                statsCheckBox, commentsCheckBox,
                                javanamingComboBox,
                                chosenOracleVersion,
                                sequenceModel, functionModel,
                                tableModel, aspSourceDirectory,
                                aspStatementWranglerManager,
                                aspStatementErrors,
                                aspStatementFileNames, sqlDir,
                                getNumberMethodsAsProperties(),
                                validateCheckBox,
                                extraSQLCheckBox,
                                factoryClassName, connectionType,
                                connectionName, logType, logName,
                                ejbType, webServicesFlag,
                                mcpServerFlag,
                                mcpHttpTokenFlag,
                                mcpHttpsFlag,
                                mcpOAuthFlag,
                                prometheusServerFlag,
                                mcpInstructions,
                                webServicesBfilesAreAbstractFlag,
                                finalizeMethodFlag,
                                servicePreCallStubFlag,
                                servicePostCallStubFlag,
                                serviceAlwaysReleaseFlag,
                                bfileDirName, bfilePrefix,
                                bfileSuffix, tempDirName,
                                tempPrefix, tempSuffix,
                                classWSInterfaceName,
                                classWSImplName,
                                closeConnectionFlag,
                                commitConnectionFlag,
                                new DaoPoolSettings(daoPoolFlag, daoPoolMaxSize, daoPoolMinIdle,
                                        daoPoolMaxWaitMs, daoPoolIdleTimeoutMs, daoPoolOnReturn),
                                wsJavaNumberTypeComboBox,
                                wsRecTypeComboBox,
                                targetJVM,
                                methodPlsqlPrefix,
                                methodSqlPrefix,
                                postscriptName,
                                postscriptContent,
                                extraClassCode, xwsTypeName,
                                xwsImpl,
                                xwsIface);

                McpDbWizardEvent obEvent =
                        new McpDbWizardEvent(McpDbWizardEvent.REFRESH_NEEDED);
                obEvent.setThing(codeRoot);
                obEvent.setThing2(chosenOracleVersion);

                reportMcpDbWizardEvent(obEvent);


                return (bldStatus);
            } catch (CSException e) {
                error(e, true, true);
                return (false);
            }
        }


    }

    public boolean generateCodeFromIniFile(String overideCodeBaseDirectory,
                                           String overideSqlFileDirectory) {

        String step = "start";

        info("Generating code from " + iniFileName);

        McpDbWizardEvent obEvent =
                new McpDbWizardEvent(McpDbWizardEvent.GENERATION_STARTED);
        reportMcpDbWizardEvent(obEvent);

        try {
            mrFrame.connectButton.setEnabled(false);
        } catch (Exception e) {
            //    syserror("ini=" + e.getMessage()); //DRKLUGE
        }

        if (mrWrangler == null || mrWrangler.getWritePhysicalFiles()) {
            getIniFile();
        }

        if (connectDB(ipField, portField, sidField, userField, passField)) {
            {
                if (overideCodeBaseDirectory != null &&
                        overideCodeBaseDirectory.length() > 0) {
                    info("");
                    info("Code Base Directory in " + FILE_EXTENSION +
                            " file being overridden");
                    info("Code will be created in " +
                            overideCodeBaseDirectory);
                    info("");
                    codeRootField = new String(overideCodeBaseDirectory);
                }

                if (overideSqlFileDirectory != null &&
                        overideSqlFileDirectory.length() > 0) {
                    info("");
                    info("SQL File Directory in " + FILE_EXTENSION +
                            " file being overridden");
                    info(overideSqlFileDirectory +
                            " will be used as the SQL File directory");
                    info("");
                    sqlDirField = new String(overideSqlFileDirectory);
                }

                step = "start of models";

                createFunctionModel(userField, userObjectsFlag,
                        otherUserFilter, otherObjectsFlag);
                createSequenceModel(userField, userObjectsFlag,
                        otherUserFilter, otherObjectsFlag);
                createTableModel(userField, userObjectsFlag, otherUserFilter,
                        otherObjectsFlag);
                createSqlModel(sqlDirField, theAspToplevelPanel,
                        theAspFilePanel);
                aspSourceDirectory = new File(sqlDirField);
                step = "start of generate";

                boolean retCode =
                        mrWrangler.generateCodeV3(new File(codeRootField),
                                packageNameField, authorField,
                                versionField, commentField,
                                javaAccessComboBox,
                                debugMessagesCheckbox,
                                otherMessagesCheckbox,
                                statsCheckBox, commentsCheckBox,
                                javanamingComboBox,
                                chosenOracleVersion,
                                sequenceModel, functionModel,
                                tableModel, aspSourceDirectory,
                                aspStatementWranglerManager,
                                aspStatementErrors,
                                aspStatementFileNames,
                                new File(sqlDirField),
                                getNumberMethodsAsProperties(),
                                validateCheckBox,
                                extraSQLCheckBox,
                                factoryClassName, connectionType,
                                connectionName, logType, logName,
                                ejbType, webServicesFlag,
                                mcpServerFlag,
                                mcpHttpTokenFlag,
                                mcpHttpsFlag,
                                mcpOAuthFlag,
                                prometheusServerFlag,
                                mcpInstructions,
                                webServicesBfilesAreAbstractFlag,
                                finalizeMethodFlag,
                                servicePreCallStubFlag,
                                servicePostCallStubFlag,
                                serviceAlwaysReleaseFlag,
                                bfileDirName, bfilePrefix,
                                bfileSuffix, tempDirName,
                                tempPrefix, tempSuffix,
                                classWSInterfaceName,
                                classWSImplName,
                                closeConnectionFlag,
                                commitConnectionFlag,
                                new DaoPoolSettings(daoPoolFlag, daoPoolMaxSize, daoPoolMinIdle,
                                        daoPoolMaxWaitMs, daoPoolIdleTimeoutMs, daoPoolOnReturn),
                                wsJavaNumberTypeComboBox,
                                wsRecTypeComboBox, targetJVM,
                                methodPlsqlPrefix,
                                methodSqlPrefix,
                                postscriptName,
                                postscriptContent,
                                extraClassCode, xwsTypeName,
                                xwsImpl,
                                xwsIface);


                try {

                    step = "start of repaint";

                    try {
                        mrFrame.connectButton.setEnabled(true);
                        mrFrame.connectButton.repaint();
                        mrFrame.repaint();
                    } catch (Exception e) {
                    }

                    step = "start of refresh event ";

                    obEvent =
                            new McpDbWizardEvent(McpDbWizardEvent.REFRESH_NEEDED);
                    obEvent.setThing(codeRootField);
                    obEvent.setThing2(chosenOracleVersion);

                    reportMcpDbWizardEvent(obEvent);

                    step = "after refresh";

                    step = "start of jdbc event ";

                    obEvent =
                            new McpDbWizardEvent(McpDbWizardEvent.ORACLE_JDBC_DRIVER_NEEDED);
                    obEvent.setThing2(chosenOracleVersion);

                    reportMcpDbWizardEvent(obEvent);

                    step = "after jdbc";

                    if (logType.equals("Apache's Log4J")) {


                        obEvent =
                                new McpDbWizardEvent(McpDbWizardEvent.APACHE_LOG4J_IN_USE);
                        reportMcpDbWizardEvent(obEvent);
                    }

                    if (logType.equals("Java 1.4 Logging")) {


                        obEvent =
                                new McpDbWizardEvent(McpDbWizardEvent.JAVA_UTIL_LOGGING_IN_USE);
                        reportMcpDbWizardEvent(obEvent);
                    }
                    step = "after logs";

                    obEvent =
                            new McpDbWizardEvent(McpDbWizardEvent.GENERATION_FINISHED);
                    reportMcpDbWizardEvent(obEvent);

                } catch (Exception e) {
                    syserror("generate failed on step " + step + ":" + e.getMessage());
                }

                return (retCode);

            }


        }
        return (true);
    }

    /**
     *
     */
    public void setOutputTextArea(JTextArea newTextArea) {
        if (uiName.equals("JDEV") || uiName.equals("EC33")) {
        } else {
            ((UiLog) mrStatusLog).addJTextArea(newTextArea,
                    OUTPUT_TEXT_AREA_LINES);
        }
    }

    Properties getNumberMethodsAsProperties() {
        Properties numberDataTypes = new Properties();

        if (useByteCheckBox) {
            numberDataTypes.put("byte", "byte");
        }

        if (useShortCheckBox) {
            numberDataTypes.put("short", "short");
        }

        if (useIntCheckBox) {
            numberDataTypes.put("int", "int");
        }

        if (useLongCheckBox) {
            numberDataTypes.put("long", "long");
        }

        if (useFloatCheckBox) {
            numberDataTypes.put("float", "float");
        }

        if (useDoubleCheckBox) {
            numberDataTypes.put("double", "double");
        }

        if (useByteObjCheckBox) {
            numberDataTypes.put("Byte", "Byte");
        }

        if (useShortObjCheckBox) {
            numberDataTypes.put("Short", "Short");
        }

        if (useIntObjCheckBox) {
            numberDataTypes.put("Integer", "Integer");
        }

        if (useLongObjCheckBox) {
            numberDataTypes.put("Long", "Long");
        }

        if (useFloatObjCheckBox) {
            numberDataTypes.put("Float", "Float");
        }

        if (useDoubleObjCheckBox) {
            numberDataTypes.put("Double", "Double");
        }

        numberDataTypes.put("java.math.BigDecimal", "java.math.BigDecimal");

        return (numberDataTypes);

    }

    /**
     *
     */
    public void createSequenceTable(String userName, boolean userFlag,
                                    String otherUsername,
                                    boolean otherUserFlag, JTable pTable,
                                    int tablePanelHeight) {
        int screenHeight = tablePanelHeight;

        info("Retrieving Sequence Data");

        createSequenceModel(userName, userFlag, otherUsername, otherUserFlag);
        pTable.setModel(sequenceModel);

        if (((pTable.getRowHeight() + 1) *
                (15 + sequenceModel.getRowCount())) > screenHeight) {
            screenHeight =
                    (pTable.getRowHeight() + 1) * (15 + sequenceModel.getRowCount());
        }

        Dimension xYSize = new Dimension(DEFAULT_SCREEN_WIDTH, screenHeight);
        pTable.setPreferredSize(xYSize);
        JTableHeader newHeader = pTable.getTableHeader();
        TableColumnModel newColumnModel = pTable.getColumnModel();

        for (int i = 0; i < newColumnModel.getColumnCount(); i++) {
            TableColumn tempColumn = newColumnModel.getColumn(i);

            if (((String) tempColumn.getHeaderValue()).equals("Selected")) {
                tempColumn.setMinWidth(50);
                tempColumn.setPreferredWidth(70);
                tempColumn.setMaxWidth(100);
            } else if (((String) tempColumn.getHeaderValue()).equals("Sequence Owner")) {
                tempColumn.setMinWidth(10);
                tempColumn.setPreferredWidth(70);
                tempColumn.setMaxWidth(200);
            } else if (((String) tempColumn.getHeaderValue()).equals("Accessed Via")) {
                tempColumn.setMinWidth(10);
                tempColumn.setPreferredWidth(100);
                tempColumn.setMaxWidth(200);
            } else if (((String) tempColumn.getHeaderValue()).equals("Real Owner")) {
                tempColumn.setMinWidth(10);
                tempColumn.setPreferredWidth(70);
                tempColumn.setMaxWidth(200);
            }

        }

        newHeader.setReorderingAllowed(false);
        pTable.sizeColumnsToFit(true);

    }

    /**
     *
     */
    public void createTableTable(String userName, boolean userFlag,
                                 String otherUsername, boolean otherUserFlag,
                                 JTable pTable, int tablePanelHeight) {
        int screenHeight = tablePanelHeight;

        info("Retrieving Table Data");

        createTableModel(userName, userFlag, otherUsername, otherUserFlag);
        pTable.setModel(tableModel);

        if (((pTable.getRowHeight() + 1) * (15 + tableModel.getRowCount())) >
                screenHeight) {
            screenHeight =
                    (pTable.getRowHeight() + 1) * (15 + tableModel.getRowCount());
        }

        Dimension xYSize = new Dimension(DEFAULT_SCREEN_WIDTH, screenHeight);
        pTable.setPreferredSize(xYSize);
        JTableHeader newHeader = pTable.getTableHeader();
        TableColumnModel newColumnModel = pTable.getColumnModel();

        for (int i = 0; i < newColumnModel.getColumnCount(); i++) {
            TableColumn tempColumn = newColumnModel.getColumn(i);

            if (((String) tempColumn.getHeaderValue()).equals("Selected")) {
                tempColumn.setMinWidth(50);
                tempColumn.setPreferredWidth(70);
                tempColumn.setMaxWidth(100);
            } else if (((String) tempColumn.getHeaderValue()).equals("Table Owner")) {
                tempColumn.setMinWidth(10);
                tempColumn.setPreferredWidth(70);
                tempColumn.setMaxWidth(200);
            } else if (((String) tempColumn.getHeaderValue()).equals("Accessed Via")) {
                tempColumn.setMinWidth(10);
                tempColumn.setPreferredWidth(100);
                tempColumn.setMaxWidth(200);
            } else if (((String) tempColumn.getHeaderValue()).equals("Real Owner")) {
                tempColumn.setMinWidth(10);
                tempColumn.setPreferredWidth(70);
                tempColumn.setMaxWidth(200);
            }

        }

        newHeader.setReorderingAllowed(false);
        pTable.sizeColumnsToFit(true);

    }

    /**
     *
     */
    public void setAspTreeSelection(TreeSelectionEvent e) {
        TreePath aPath = e.getPath();
        Object[] pathArray = aPath.getPath();

        if (pathArray.length == 1)
        // must be root node
        {
            // Update fields in aspTopLevelPanel

            aspSplitPane.setRightComponent(theAspToplevelPanel);
            updateASPFileCounts();
            SwingUtilities.updateComponentTreeUI(theAspToplevelPanel);
            reportMcpDbWizardEvent(new McpDbWizardEvent(McpDbWizardEvent.REPAINT_NEEDED));
        } else {
            String filename = pathArray[1].toString();

            for (int i = 0; i < aspStatementFileNames.length; i++) {
                if (aspStatementFileNames[i].equals(filename)) {
                    // See if error message exists...
                    if (aspStatementErrors[i].length() > 0) {
                        aspSplitPane.setRightComponent(theBrokenPanel);
                        SwingUtilities.updateComponentTreeUI(theBrokenPanel);
                        theBrokenPanel.setMessage("File '" + filename +
                                        "' is not usable:",
                                aspStatementErrors[i]);
                    } else {
                        aspStatementWranglerManager[i].setPanel(theAspFilePanel);
                        SwingUtilities.updateComponentTreeUI(theAspFilePanel);
                        aspSplitPane.setRightComponent(theAspFilePanel);
                        theAspFilePanel.validate();
                        break;
                    }
                }
            }
            reportMcpDbWizardEvent(new McpDbWizardEvent(McpDbWizardEvent.REPAINT_NEEDED));
        }
    }

    /**
     *
     */
    public void createAspTree(JTree aspTree, String aspDirName,
                              AspFilePanel theAspFilePanel,
                              AspToplevelPanel theAspToplevelPanel) {
        info("Retrieving SQL Data");

        createSqlModel(aspDirName, theAspToplevelPanel, theAspFilePanel);
        aspSourceDirectory = new File(aspDirName);


        File aspDir = new File(aspDirName);

        DefaultMutableTreeNode rootNode =
                new DefaultMutableTreeNode(aspDir.getName());

        for (int i = 0; i < aspStatementFileNames.length; i++) {
            rootNode.add(new DefaultMutableTreeNode(aspStatementFileNames[i]));
        }

        DefaultTreeModel theModel = new DefaultTreeModel(rootNode);
        aspTree.setModel(theModel);
        aspTree.addTreeSelectionListener(this);


    }

    /**
     *
     */
    public void createSequenceModel(String userName, boolean userObjectsFlag,
                                    String otherUserName,
                                    boolean otherObjectsFlag) {
        String queryUserName = new String(userName);
        String queryOtherName = new String(otherUserName);

        if (!userObjectsFlag) {
            queryUserName = "";
        }

        if (!otherObjectsFlag) {
            queryOtherName = "";
        }

        info("Retrieving sequences...");
        WriteableRowSet sequenceRowSet =
                mrWrangler.getSequenceData(queryUserName, queryOtherName);
        sequenceModel = new SequenceTableDataModel(sequenceRowSet, this, listener);
        sequenceModel.readSequenceInfo(fileProps);
        info(sequenceRowSet.size() + " sequences found");
    }

    public void selectAllSequences() {
        sequenceModel.selectSequences(true);
        McpDbWizardEvent theEvent =
                new McpDbWizardEvent(McpDbWizardEvent.ALL_SEQUENCES_SELECTED);
        reportMcpDbWizardEvent(theEvent);
    }

    public void selectNoSequences() {
        sequenceModel.selectSequences(false);
        McpDbWizardEvent theEvent =
                new McpDbWizardEvent(McpDbWizardEvent.NO_SEQUENCES_SELECTED);
        reportMcpDbWizardEvent(theEvent);
    }

    public void selectNoTables() {
        tableModel.selectTables(false);
        reportMcpDbWizardEvent(new McpDbWizardEvent(McpDbWizardEvent.NO_TABLES_SELECTED));
    }

    public void selectAllTables() {
        tableModel.selectTables(true);
        reportMcpDbWizardEvent(new McpDbWizardEvent(McpDbWizardEvent.ALL_TABLES_SELECTED));
    }

    /**
     *
     */
    public void createFunctionTable(String userName, boolean userFlag,
                                    String otherUsername,
                                    boolean otherUserFlag, JTable pTable,
                                    int tablePanelHeight) {
        int screenHeight = tablePanelHeight;

        info(PARSE_PLSQL_MESSAGE);

        createFunctionModel(userName, userFlag, otherUsername, otherUserFlag);
        pTable.setModel(functionModel);

        if (((pTable.getRowHeight() + 1) *
                (25 + functionModel.getRowCount())) > screenHeight) {
            screenHeight =
                    (pTable.getRowHeight() + 1) * (25 + functionModel.getRowCount());
        }

        Dimension xYSize = new Dimension(DEFAULT_SCREEN_WIDTH, screenHeight);
        pTable.setPreferredSize(xYSize);
        JTableHeader newHeader = pTable.getTableHeader();
        TableColumnModel newColumnModel = pTable.getColumnModel();

        for (int i = 0; i < newColumnModel.getColumnCount(); i++) {
            TableColumn tempColumn = newColumnModel.getColumn(i);

            if (((String) tempColumn.getHeaderValue()).equals("Selected")) {
                tempColumn.setMinWidth(50);
                tempColumn.setPreferredWidth(70);
                tempColumn.setMaxWidth(100);
            } else if (((String) tempColumn.getHeaderValue()).equals("Function Owner")) {
                tempColumn.setMinWidth(10);
                tempColumn.setPreferredWidth(70);
                tempColumn.setMaxWidth(200);
            } else if (((String) tempColumn.getHeaderValue()).equals("Accessed Via")) {
                tempColumn.setMinWidth(10);
                tempColumn.setPreferredWidth(100);
                tempColumn.setMaxWidth(200);
            } else if (((String) tempColumn.getHeaderValue()).equals("Real Owner")) {
                tempColumn.setMinWidth(10);
                tempColumn.setPreferredWidth(70);
                tempColumn.setMaxWidth(200);
            }

        }

        newHeader.setReorderingAllowed(false);
        pTable.sizeColumnsToFit(true);

    }

    /**
     *
     */
    public void createFunctionModel(String userName, boolean userObjectsFlag,
                                    String otherUserName,
                                    boolean otherObjectsFlag) {
        String queryUserName = new String(userName);
        String queryOtherName = new String(otherUserName);

        if (!userObjectsFlag) {
            queryUserName = "";
        }

        if (!otherObjectsFlag) {
            queryOtherName = "";
        }

        info(PARSE_PLSQL_MESSAGE);
        WriteableRowSet functionRowSet =
                mrWrangler.getFunctionData(queryUserName, queryOtherName);
        functionModel = new FunctionTableDataModel(functionRowSet, this, listener);
        functionModel.readFunctionInfo(fileProps);
        info(functionRowSet.size() + " Procedures and Functions found");

    }

    /**
     *
     */
    public void createTableModel(String userName, boolean userObjectsFlag,
                                 String otherUserName,
                                 boolean otherObjectsFlag) {
        String queryUserName = new String(userName);
        String queryOtherName = new String(otherUserName);

        if (!userObjectsFlag) {
            queryUserName = "";
        }

        if (!otherObjectsFlag) {
            queryOtherName = "";
        }

        info(PARSE_TABLE_MESSAGE);
        WriteableRowSet tableRowSet =
                mrWrangler.getTableData(queryUserName, queryOtherName);
        tableModel = new TableTableDataModel(tableRowSet, this, listener);
        tableModel.readTableInfo(fileProps);
        info(tableRowSet.size() + " Tables and views found");

    }

    /**
     *
     */
    public void selectAllASPStatements(boolean value) {
        if (aspStatementWranglerManager != null) {
            for (int i = 0; i < aspStatementWranglerManager.length; i++) {
                if (aspStatementWranglerManager[i] != null) {
                    aspStatementWranglerManager[i].setCreateJava(value);
                }
            }
            theAspFilePanel.setCreateJavaClass(value);
        }

        updateASPFileCounts();

        if (value) {
            reportMcpDbWizardEvent(new McpDbWizardEvent(McpDbWizardEvent.ALL_ASP_SELECTED));
        } else {
            reportMcpDbWizardEvent(new McpDbWizardEvent(McpDbWizardEvent.NO_ASP_SELECTED));
        }

    }

    /**
     *
     */
    private void updateASPFileCounts() {
        if (aspStatementWranglerManager != null && theAspToplevelPanel != null) {
            int unSelected = 0;
            int selected = 0;

            for (int i = 0; i < aspStatementWranglerManager.length; i++) {
                if (aspStatementWranglerManager[i] != null) {
                    if (aspStatementWranglerManager[i].getCreateJava()) {
                        selected++;
                    } else {
                        unSelected++;
                    }
                }
            }

            theAspToplevelPanel.setSelectedFileCounts(selected, unSelected);
        }

    }

    /**
     *
     */
    private void createSqlModel(String aspDirName,
                                AspToplevelPanel theAspToplevelPanel,
                                AspFilePanel theAspFilePanel) {
        final int SEQ_MISSING_LIMIT = 100;
        //int goodWranglers = 0;

        int invalidSql = 0;
        int validSql = 0;

        int unselectedSql = 0;
        int selectedSql = 0;


        this.theAspToplevelPanel = theAspToplevelPanel;
        this.theAspFilePanel = theAspFilePanel;

        try {
            File aspDir = new File(aspDirName);
            aspStatementFileNames =
                    aspDir.list((FilenameFilter) new EndsWithFilter(".SQL",
                            "Sql Files"));
            if (aspStatementFileNames.length == 0) {
                info(aspDirName + " does not contain any usable SQL files",
                        false, true);
            }
        } catch (Exception e) {
            error("Directory " + aspDirName + " is not usable");
            aspStatementFileNames = new String[0];
        }

        // A statement can come from EITHER a .sql file in the directory or the config itself
        // (SQL_TEXT_<i>). Merge the two, so a config carrying its own text works with no directory
        // at all -- the point of inlining. A config with neither is unchanged from before.
        aspStatementFileNames = mergeInlineSqlNames(aspStatementFileNames, fileProps, SEQ_MISSING_LIMIT);

        aspStatementWranglerManager =
                new SqlStatementWrangler[aspStatementFileNames.length];
        aspStatementErrors = new String[aspStatementFileNames.length];
        //superSqlWrangler = new  SqlStatementWranglerManager(aspStatementWranglerManager, aspStatementErrors);

        // Populate array of wranglers...
        for (int i = 0; i < aspStatementFileNames.length; i++) {
            aspStatementErrors[i] = "";

            File tempFile = new File(aspDirName, aspStatementFileNames[i]);

            // Which config record this statement is, resolved BEFORE the text is fetched: it is
            // what says whether the config carries the text itself.
            int tempPropNumber = findSqlPropertyIndex(fileProps, aspStatementFileNames[i], SEQ_MISSING_LIMIT);
            String theInlineSql = tempPropNumber < 0
                    ? null
                    : fileProps.getProperty(SQL_TEXT_KEY + tempPropNumber);
            boolean haveInlineSql = theInlineSql != null && theInlineSql.length() > 0;

            if (haveInlineSql || (tempFile.exists() && tempFile.canRead() && tempFile.isFile() &&
                    tempFile.length() > 0)) {
                // Read contents of file into a String
                try {
                    // Inline text WINS over a file of the same name. A config that carries its own
                    // statement is self-contained by definition, and a stale file beside it must
                    // not silently take precedence over what the config says.
                    String tempStatement = haveInlineSql
                            ? theInlineSql
                            : JavaUtils.readFileIntoString(tempFile);

                    aspStatementWranglerManager[i] =
                            new SqlStatementWrangler(aspStatementFileNames[i],
                                    tempStatement, fileProps,
                                    tempPropNumber, this);

                    if (aspStatementWranglerManager[i].getFileType() ==
                            SqlStatementWrangler.VALID_SQL_STATEMENT) {
                        validSql++;
                        unselectedSql++;
                    } else if (aspStatementWranglerManager[i].getFileType() ==
                            SqlStatementWrangler.GOOD_FILE_BAD_SQL_STATEMENT) {
                        invalidSql++;
                        aspStatementErrors[i] =
                                "SQL Statement not usable:\n" + aspStatementWranglerManager[i].getRawSqlStatement();
                    } else if (aspStatementWranglerManager[i].getFileType() ==
                            SqlStatementWrangler.SELECTED_SQL_STATEMENT) {
                        selectedSql++;
                        validSql++;
                    }

                } catch (Exception e) {
                    warning("SQL file '" + tempFile.getAbsolutePath() +
                            "' is not usable: " + e.getMessage());
                    aspStatementWranglerManager[i] = null;
                    aspStatementErrors[i] =
                            "'" + tempFile.getAbsolutePath() + "' is not usable: " +
                                    e.getMessage();
                    badFileWranglers++;
                }
            } else {
                warning("SQL file " + tempFile.getAbsolutePath() +
                        " is not usable");
                aspStatementWranglerManager[i] = null;

                // Try and find out why file is bad...
                if (!tempFile.exists()) {
                    aspStatementErrors[i] =
                            "'" + tempFile + "' does not exist";
                } else if (tempFile.isDirectory()) {
                    aspStatementErrors[i] =
                            "'" + tempFile + "' is a Directory";
                } else if (!tempFile.canRead()) {
                    aspStatementErrors[i] =
                            "'" + tempFile + "' is not readable";
                } else if (tempFile.length() == 0) {
                    aspStatementErrors[i] = "'" + tempFile + "' is empty";
                }

                badFileWranglers++;
            }
        }

        // A config entry naming a file the scan did not return produces NO wrangler at all, so
        // nothing above this point can report it -- the loop iterates the files it found, not the
        // entries the config asked for. Diff the two sets here, where both are known.
        reportSqlFilesNamedButNotFound(aspDirName, aspStatementFileNames, SEQ_MISSING_LIMIT);

        if (theAspToplevelPanel != null) {
            theAspToplevelPanel.setFileCounts(aspStatementWranglerManager.length -
                            badFileWranglers,
                    badFileWranglers, validSql,
                    invalidSql, selectedSql,
                    unselectedSql);
            try {
                SwingUtilities.updateComponentTreeUI(theAspToplevelPanel);
            } catch (Exception e) {
            }
        }
    }

    public void selectAllFunctions() {
        functionModel.selectFunctions(true);
        reportMcpDbWizardEvent(new McpDbWizardEvent(McpDbWizardEvent.ALL_FUNCTIONS_SELECTED));
    }

    public void selectNoFunctions() {
        functionModel.selectFunctions(false);
        reportMcpDbWizardEvent(new McpDbWizardEvent(McpDbWizardEvent.NO_FUNCTIONS_SELECTED));
    }

    public String getVersion() {
        return (mrWrangler.getVersion());
    }

    public String[] getSupportedVersions() {


        return (SqlStatementDictionary.oracleVersions);
    }

    public void populateOracleVersionComboBox(JComboBox theBox) {
        if (theBox.getItemCount() > 0) {
            theBox.removeAllItems();
        }

        for (int i = 0; i < SqlStatementDictionary.oracleVersions.length;
             i++) {
            theBox.addItem(SqlStatementDictionary.oracleVersions[i]);
        }
    }

    public void populateJavaVersionComboBox(JComboBox theBox) {
        if (theBox.getItemCount() > 0) {
            theBox.removeAllItems();
        }

        for (int i = 2; i < 8;
             i++) {
            theBox.addItem("1." + i);
        }
    }

    public void populateJavaAccessComboBox(JComboBox theBox) {
        if (theBox.getItemCount() > 0) {
            theBox.removeAllItems();
        }

        for (int i = 0; i < javaAccessTypes.length; i++) {
            theBox.addItem(javaAccessTypes[i]);
        }
    }

    public void populateJavaNamingConventionComboBox(JComboBox theBox) {
        if (theBox.getItemCount() > 0) {
            theBox.removeAllItems();
        }

        for (int i = 0; i < javaNamingConventions.length; i++) {
            theBox.addItem(javaNamingConventions[i]);
        }
    }

    public void populateWsJavaNumberTypeComboBox(JComboBox theBox) {
        if (theBox.getItemCount() > 0) {
            theBox.removeAllItems();
        }

        for (int i = 0; i < WS_JAVA_NUMBER_TYPES.length; i++) {
            theBox.addItem(WS_JAVA_NUMBER_TYPES[i]);
        }
    }

    /**
     * The config key holding a statement's own text, so a config needs no SQL directory.
     * Delegates to {@link SqlStatementWrangler#SQL_TEXT} rather than repeating the literal: the
     * writer and the reader must never disagree about the spelling.
     */
    public static final String SQL_TEXT_KEY = SqlStatementWrangler.SQL_TEXT;

    /**
     * Which {@code SQL_FILENAME_<i>} record a statement name belongs to, or -1.
     *
     * <p>Lifted out of the load loop unchanged, including its gap tolerance: indexes are not
     * guaranteed contiguous, so the scan gives up only after {@code theMissingLimit} consecutive
     * misses rather than at the first hole. The limit is passed rather than duplicated -- it is a
     * local in the caller, and two copies could drift.
     */
    static int findSqlPropertyIndex(java.util.Properties theProperties, String theName,
                                    int theMissingLimit) {
        int theMissing = 0;
        for (int theIndex = 0; ; theIndex++) {
            String theKey = SqlStatementWrangler.SQL_FILENAME + theIndex;
            if (theProperties.containsKey(theKey)) {
                if (theProperties.getProperty(theKey).equals(theName)) {
                    return theIndex;
                }
            } else {
                theMissing++;
                if (theMissing > theMissingLimit) {
                    return -1;
                }
            }
        }
    }

    /**
     * The directory's {@code .sql} files, plus any statement the CONFIG carries inline that has no
     * file of that name.
     *
     * <p>Without this a config could hold its own text and still show nothing, because the SQL tab
     * has always been populated by listing a directory. Order is preserved and the file list comes
     * first, so a deployment with files behaves exactly as before -- an inline-only statement is
     * simply appended.
     */
    static String[] mergeInlineSqlNames(String[] theFileNames, java.util.Properties theProperties,
                                        int theMissingLimit) {
        java.util.LinkedHashSet<String> theNames = new java.util.LinkedHashSet<String>();
        if (theFileNames != null) {
            for (int i = 0; i < theFileNames.length; i++) {
                theNames.add(theFileNames[i]);
            }
        }
        int theMissing = 0;
        for (int theIndex = 0; ; theIndex++) {
            String theNameKey = SqlStatementWrangler.SQL_FILENAME + theIndex;
            if (theProperties.containsKey(theNameKey)) {
                String theText = theProperties.getProperty(SQL_TEXT_KEY + theIndex);
                if (theText != null && theText.length() > 0) {
                    theNames.add(theProperties.getProperty(theNameKey));
                }
            } else {
                theMissing++;
                if (theMissing > theMissingLimit) {
                    break;
                }
            }
        }
        return theNames.toArray(new String[0]);
    }

    public void populateWsRecTypeComboBox(JComboBox theBox) {
        if (theBox.getItemCount() > 0) {
            theBox.removeAllItems();
        }

        for (int i = 0; i < WS_REC_TYPES.length; i++) {
            theBox.addItem(WS_REC_TYPES[i]);
        }
    }

    public void populateOracleDirectoriesComboBox(JComboBox theBox) {
        if (theBox.getItemCount() > 0) {
            theBox.removeAllItems();
        }

        for (int i = 0; i < oracleDirectoryList.length; i++) {
            theBox.addItem(oracleDirectoryList[i]);
        }
    }

    public void populateLogTypeComboBox(JComboBox theBox) {
        if (theBox.getItemCount() > 0) {
            theBox.removeAllItems();
        }

        for (int i = 0;
             i < com.mcpdbwizard.app.procbuilder.SAAdminWrangler.LOG_TYPE_DESCRIPTIONS.length;
             i++) {
            theBox.addItem(com.mcpdbwizard.app.procbuilder.SAAdminWrangler.LOG_TYPE_DESCRIPTIONS[i]);
        }
    }

    public void populateConnectionTypeComboBox(JComboBox theBox) {
        if (theBox.getItemCount() > 0) {
            theBox.removeAllItems();
        }

        theBox.addItem(com.mcpdbwizard.app.procbuilder.SAAdminWrangler.CONNECTION_TYPE_HC);
        theBox.addItem(com.mcpdbwizard.app.procbuilder.SAAdminWrangler.CONNECTION_TYPE_JNDI_REAL);
        theBox.addItem(com.mcpdbwizard.app.procbuilder.SAAdminWrangler.CONNECTION_TYPE_DATASOURCE);
        theBox.addItem(com.mcpdbwizard.app.procbuilder.SAAdminWrangler.CONNECTION_TYPE_JBOSERVER);
    }

    public void newIniFile(File theNewIniFile) {
        info("Creating new file " + theNewIniFile.getAbsolutePath(), true,
                true);
        iniFileName = new String(theNewIniFile.getAbsolutePath());
        setIniFile();
    }

    public void openIniFile(File theNewIniFile) {
        info("Opening file " + theNewIniFile.getAbsolutePath(), true, true);
        iniFileName = new String(theNewIniFile.getAbsolutePath());
        getIniFile();
        info("File " + theNewIniFile.getName() + " loaded", true, true);
    }

    public void saveAsIniFile(File theNewIniFile) {
        info("Saving file as " + theNewIniFile.getAbsolutePath(), true, true);
        iniFileName = new String(theNewIniFile.getAbsolutePath());
        setIniFile();
    }

    public void addMcpDbWizardEventListener(McpDbWizardEventListener listener) {
        this.listener = listener;
    }

    public void reportMcpDbWizardEvent(McpDbWizardEvent event) {
        if (listener != null) {
            listener.reportEvent(event);
        }
    }

    public void disconnect() {
        if (mrWrangler != null) {
            mrWrangler.disconnect();
        }
    }


    public boolean getAreConnected() {
        return (areConnected);
    }

    public void checkStringNotNull(String aString,
                                   String description) throws CSException {
        if (aString == null || aString.length() == 0) {
            //this.error(description + " must be provided",true,true);
            throw new CSException(description + " not provided");
        }
    }

    public void valueChanged(TreeSelectionEvent e) {
        setAspTreeSelection(e);
    }

    private void initProgressMonitor(String message, String note) {
        if (uiName.equals("JDEV") &&
                false) // KLUGE - can't get this to work b4 deadline
        {
            theProgressMonitor =
                    new ProgressMonitor(null, message, note, 0, 100);
        }
    }

    private void setProgress(String note, int pct) {
        if (theProgressMonitor != null) {
            theProgressMonitor.setNote(note);
            theProgressMonitor.setProgress(pct);
            theProgressMonitor.setMillisToDecideToPopup(500);
            theProgressMonitor.setMillisToPopup(500);
            Thread.yield();

            if (pct == 100) {
                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                }

                theProgressMonitor.close();
                theProgressMonitor = null;
            }

        }
    }

    public boolean getShutdownOnExit() {
        return (shutdownOnExit);
    }

    public void setShutdownOnExit(boolean shutdownOnExit) {
        this.shutdownOnExit = shutdownOnExit;
    }

    public boolean getWritePhysicalFiles() {
        if (mrWrangler == null)
            return (true);

        return mrWrangler.getWritePhysicalFiles();
    }

    public void setWritePhysicalFiles(boolean writePhysicalFiles) {
        mrWrangler.setWritePhysicalFiles(writePhysicalFiles);
    }

    public void tableChanged(TableModelEvent e) {

    }

    public String getGenericString() {
        return genericString;
    }

    public void setGenericString(String value) {
        genericString = value;
    }
}


