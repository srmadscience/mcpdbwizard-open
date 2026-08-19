package com.mcpdbwizard.app.common;

import java.sql.*;
import java.util.Properties;

import oracle.jdbc.driver.*;
//import java.util.Properties;
import java.net.InetAddress;
//import java.util.Map;

import com.mcpdbwizard.pub.*;

/***
 * This class handles connecting to Oracle and provides various
 * utility Methods.
 * @version 2
 * @author  devteam@mcpdbwizard.com
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class ConnectionWrangler {
    static final int DB2 = 2;
    static final int ORA = 11;
    static final String ORA_2_DB2 = "Oracle Trying to Speak to DB2";
    static final String ORA_2_DB2_ERROR_MESSAGE = "Got minus one from a read call";

    /**
     * Socket read timeout for the generator's own Oracle connection, in milliseconds.
     *
     * <p>Without one, a connection that dies mid-generation does not fail — it <em>hangs</em>. The
     * driver blocks in a socket read that never returns, because the server side is gone but no FIN
     * ever arrived, and the OS will not give up until TCP keepalive does (typically two hours). Seen
     * for real on 2026-08-02: a regen against a 12c box sat in {@code SocketDispatcher.read0} for
     * thirty minutes on 2.6 seconds of CPU while {@code v$session} showed no session at all.
     *
     * <p>Ten minutes is chosen to be far longer than any legitimate data-dictionary query — the live
     * harness caps a single dictionary statement at 90 seconds, and the slowest observed real query
     * across the test estate is a few seconds — while still turning an indefinite hang into a clean
     * error. Raise it with {@code -Dorinda.jdbc.readTimeoutMillis=<n>} on a box slow enough to need
     * it, or set {@code 0} to restore the old wait-forever behaviour.
     */
    public static final long DEFAULT_READ_TIMEOUT_MS = 600000L;

    /** How long to wait for the initial TCP connect, in milliseconds. */
    public static final long DEFAULT_CONNECT_TIMEOUT_MS = 30000L;

    /** Overrides {@link #DEFAULT_READ_TIMEOUT_MS}; {@code 0} disables the timeout. */
    public static final String READ_TIMEOUT_PROPERTY = "orinda.jdbc.readTimeoutMillis";

    /** Overrides {@link #DEFAULT_CONNECT_TIMEOUT_MS}; {@code 0} disables the timeout. */
    public static final String CONNECT_TIMEOUT_PROPERTY = "orinda.jdbc.connectTimeoutMillis";
    /**
     * TCP/IP hostname of server
     */
    public String mrHostName;
    /**
     * TCP/IP IP Address of server
     */
    public String mrIpAddress;
    /**
     * TCP/IP Port in use by Sql*Net
     */
    public int mrPort;
    /**
     * Database Instance Name
     */
    public String mrSid;
    /**
     * OCI Connect URL
     */
    public String mrURL;
    /**
     * Database User
     */
    public String mrUser;
    /**
     * Database Password
     */
    public String mrPassword;
    /**
     * Log Object
     */
    public LogInterface mrLog;
    /**
     * Oracle connection
     */
    public Connection mrConnection;
    /*                                      //DB2
     * What product 11 = oracle, 2 = DB2    //DB2
     */                                     //DB2
    public int dbProd = ORA;                 //DB2
    /**
     * Flag to show whether we are currently connected or not
     */
    protected boolean haveConnection = false;
    /**
     * How many times we have been used since we last logged in
     **/
    int usageCounter = 0;
    /**
     * Timestamp for last DB activity
     */
    java.util.Date usageDatetime = new java.util.Date();
    boolean connectRecursed = false;

    /***
     * Attempt to log into an Oracle database using the information provided. pPort
     * will default to 1521. If pUser is SYSTEM and pPassword is null a guess will
     * be made about the system password.
     *
     * @param pHostName TCP Hostname of class. An IP Address can also be provided
     * @param pPort The port the Oracle listener is running on
     * @param pSid  The Oracle SID
     * @param pUser An Oracle user with SA privs
     * @param pPassword Password for Oracle user with SA privs
     * @param pLog A Log object of some variety.
     * @see com.spookyaction.util.Log
     */
    public ConnectionWrangler(String pHostName
            , int pPort
            , String pSid
            , String pUser
            , String pPassword
            , LogInterface pLog) throws CSException {
        InetAddress tempAddress;


        if (pHostName == null || pHostName.length() == 0) {
            throw new CSException("Server Hostname or IP address must be provided");
        } else {
            mrHostName = pHostName;
        }

        // get IP Address from Hostname...
        try {
            tempAddress = InetAddress.getByName(mrHostName);
            mrIpAddress = tempAddress.getHostAddress();
        } catch (Exception e) {
            mrIpAddress = mrHostName;
        }

        mrPort = pPort;

        if (pSid == null || pSid.length() == 0) {
            throw new CSException("SID must be provided");
        } else {
            mrSid = pSid;
            if (pSid.startsWith("/") && pSid.length() > 1) {
                pLog.info("Connecting to Service " + pSid.substring(1));
            }
        }

        if (pUser == null || pUser.length() == 0) {
            throw new CSException("Database Username must be provided");
        } else {
            mrUser = pUser;
        }

        if (pPassword == null || pPassword.length() == 0) {
            throw new CSException("Database password must be provided");
        } else {
            mrPassword = pPassword;
        }

        mrLog = pLog;

        haveConnection = false;

//  if (mrPort > 60000)
//    {
//    dbProd = DB2;
//    }

        this.connect();

    }

    /***
     * Attempt to log into an Oracle database using the information provided.
     */
    public ConnectionWrangler(String pURL
            , String pUser
            , String pPassword
            , LogInterface pLog) throws CSException {


        if (pURL == null || pURL.length() == 0) {
            throw new CSException("OCI URL must be provided");
        } else {
            mrURL = pURL;
        }


        if (pUser == null || pUser.length() == 0) {
            throw new CSException("Username must be provided");
        } else {
            mrUser = pUser;
        }

        if (pPassword == null || pPassword.length() == 0) {
            throw new CSException("Password must be provided");
        } else {
            mrPassword = pPassword;
        }

        mrLog = pLog;

        haveConnection = false;

        //
        // Attempt to load database driver...
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
        } catch (ClassNotFoundException oe) {
            mrLog.error("ConnectionWrangler:Oracle Driver not found " + oe);
            throw new CSException("Unable to find Oracle driver");
        }

        this.connect();

    }

    /**
     * @throws CSException
     */
    private void loadDriver() throws CSException {
        //
        // Attempt to load database driver...
        try {
            if (dbProd == 11)
                Class.forName("oracle.jdbc.driver.OracleDriver");
            if (dbProd == 2)
                Class.forName("com.ibm.db2.jcc.DB2Driver").newInstance();
        } catch (ClassNotFoundException oe) {
            mrLog.error("ConnectionWrangler:Unable to find DB driver '" + oe.getMessage() + "'. Please add the JAR file containing "
                    + "it to your CLASSPATH and restart.");
            throw new CSException("Unable to find required DB driver '" + oe.getMessage() + "'. Please add the JAR file containing "
                    + "it to your CLASSPATH and restart.");
        } catch (InstantiationException oe) {
            mrLog.error("ConnectionWrangler:DB2 Driver not found " + oe);
            throw new CSException("Unable to find DB2 driver");
        } catch (IllegalAccessException oe) {
            mrLog.error("ConnectionWrangler:DB2 Driver not found " + oe);
            throw new CSException("Unable to find DB2 driver");
        }
    }

    /**
     * Connect to DB. If we are already connected disconnect first and then reconnect.
     */
    public synchronized void connect() throws CSException {

        if (haveConnection) {
            this.disconnect();
        }

        loadDriver();

        //
        // Attempt to login to the DB...
        //
        try {
            haveConnection = false;

            if (dbProd == DB2) {
                String url = "jdbc:db2://" + mrIpAddress + ":" + mrPort + "/" + mrSid;
                mrLog.info("Connect to DB2 '" + mrSid + "' database using JDBC Universal type 4 driver.");

                String nodeNumber = "0";
                Properties props = new Properties();

                props.setProperty("user", mrUser);
                props.setProperty("password", DbPasswordSource.resolve(mrPassword));
                props.setProperty("CONNECTNODE", nodeNumber);

                mrConnection = DriverManager.getConnection(url, props);
            } else {
                // mrPassword and mrURL keep whatever the config said - the sentinel included, since
                // the emitter needs to see it to generate an environment lookup rather than a
                // literal. Only the connection being opened here gets the resolved secret.
                Properties oracleProps = oracleConnectionProperties(mrUser, DbPasswordSource.resolve(mrPassword));

                if (mrURL == null)
                // thin driver
                {
                    if (mrSid.startsWith("/")) {
                        mrConnection = DriverManager.getConnection(
                                "jdbc:oracle:thin:@"
                                        + mrIpAddress
                                        + ":" + mrPort
                                        + "/" + mrSid.substring(1),
                                oracleProps);

                    } else {
                        mrConnection = DriverManager.getConnection(
                                "jdbc:oracle:thin:@"
                                        + mrIpAddress
                                        + ":" + mrPort
                                        + ":" + mrSid,
                                oracleProps);

                    }
                } else {
                    // oci driver - the URL may itself carry the credentials, so it needs resolving too
                    mrConnection = DriverManager.getConnection(DbPasswordSource.resolve(mrURL), oracleProps);
                }
            }
            mrLog.info("Logged in as user " + mrUser);

            haveConnection = true;
            usageCounter = 1;
            usageDatetime.setTime(System.currentTimeMillis());

            // By default JDBC drivers commit automatically. Turn this off...
            mrConnection.setAutoCommit(false);

            if (mrConnection instanceof OracleConnection) {
                // Tell Oracle Driver to send stuff back in batches of 50
                ((OracleConnection) mrConnection).setDefaultRowPrefetch(50);
                // Tell Oracle Driver to send updates in batches of 50
                ((OracleConnection) mrConnection).setDefaultExecuteBatch(50);

                // Oracle 19c may run dictionary queries (ALL_ARGUMENTS walks) in
                // parallel query servers, which do not receive the bind variables
                // and fail with ORA-12801 wrapping ORA-01008. Introspection is all
                // small dictionary reads, so parallel query buys nothing here.
                try (java.sql.Statement s = mrConnection.createStatement()) {
                    s.execute("ALTER SESSION DISABLE PARALLEL QUERY");
                } catch (SQLException e2) {
                    mrLog.info("Could not disable parallel query: " + e2.getMessage());
                }

                // Name the session so a DBA watching V$SESSION can tell the generator's dictionary
                // reads from whatever else is using the same account. Purely diagnostic, so a
                // failure is logged and ignored rather than allowed to fail the connection.
                try {
                    SessionInfo.setModule(mrConnection,
                            SessionInfo.GENERATOR_MODULE, SessionInfo.GENERATOR_ACTION);
                } catch (SQLException e2) {
                    mrLog.info("Could not set DBMS_APPLICATION_INFO module: " + e2.getMessage());
                }
            }

        } catch (SQLException e) {
            String mrMessage = e.getMessage();
            mrLog.info(mrMessage);

            // We get a specific error message when we try to connect to DB2 with an Oracle driver.
            if (mrMessage.indexOf(ORA_2_DB2_ERROR_MESSAGE) >= 0) {
                dbProd = DB2;
                if (connectRecursed) {
                    // We;ve been here before...
                    throw new CSException(ORA_2_DB2);
                } else {
                    // prevent recursion loop
                    connectRecursed = true;
                    connect();
                    connectRecursed = false;
                    return;
                }
            } else if (mrMessage.startsWith("Io exception: Connection refused")) {
                throw new CSException("Io Error: Contact made but connection refused. Check that instance '"
                        + mrSid + "' is up and the listener is running");
            } else if (e.getErrorCode() == 17002 /* Io Exception */) {
                throw new CSException("Network Error: Check Hostname '" + mrIpAddress + "' and Port '" + mrPort + "' are valid");
            } else if (e.getErrorCode() == 1017 /* Invalid Username/Password */) {
                throw new CSException("Login Error: Check Username '" + mrUser + "' and the password are valid");
            } else if (e.getErrorCode() == 1033 /* Startup of Shutdown in progress */) {
                throw new CSException("Server Error: Startup of Shutdown in progress");
            } else if (e.getErrorCode() == 12535 /* operation timed out */) {
                throw new CSException("Io Error: Hostname '" + mrIpAddress + "' known but not reachable");
            }

            throw new CSException(e.toString());
        }
    }

    /**
     * Make sure we have a connection by connecting if we are disconnected.
     */
    public synchronized void confirmConnected() throws CSException {
        if (!haveConnection) {
            this.connect();
        }
    }

    /**
     * @return <code>true</code> if we are connected.
     */
    public synchronized boolean haveConnection() {
        return (haveConnection);
    }

    /**
     * Credentials plus the socket timeouts for the generator's own Oracle connection.
     *
     * <p>These are deliberately connection <em>properties</em> rather than a
     * {@code Statement.setQueryTimeout}: a query timeout only bounds a call the server is still
     * answering, and the failure this guards against is one where the server is not answering at all
     * because the connection is half-open. Only a socket-level read timeout unblocks that.
     *
     * <p>An explicit {@code -Doracle.jdbc.ReadTimeout} / {@code -Doracle.net.CONNECT_TIMEOUT} on the
     * JVM wins: someone who has already tuned the driver directly should not have it silently
     * overridden here.
     *
     * <p>Applies to the generator only. Generated code builds its own connections and is unaffected.
     */
    static Properties oracleConnectionProperties(String theUser, String thePassword) {
        Properties theProperties = new Properties();

        if (theUser != null) {
            theProperties.setProperty("user", theUser);
        }
        if (thePassword != null) {
            theProperties.setProperty("password", thePassword);
        }

        applyTimeout(theProperties, "oracle.jdbc.ReadTimeout",
                READ_TIMEOUT_PROPERTY, DEFAULT_READ_TIMEOUT_MS);
        applyTimeout(theProperties, "oracle.net.CONNECT_TIMEOUT",
                CONNECT_TIMEOUT_PROPERTY, DEFAULT_CONNECT_TIMEOUT_MS);

        return theProperties;
    }

    /**
     * Set one driver timeout, unless the JVM already sets it directly or it has been turned off.
     *
     * @param theDriverProperty the Oracle driver's own property name
     * @param theOverrideProperty our system property that overrides the default; 0 disables
     * @param theDefault          the value used when nothing overrides it
     */
    private static void applyTimeout(Properties theProperties, String theDriverProperty,
                                     String theOverrideProperty, long theDefault) {
        if (System.getProperty(theDriverProperty) != null) {
            return;
        }

        long theValue = theDefault;
        String theOverride = System.getProperty(theOverrideProperty);
        if (theOverride != null && theOverride.trim().length() > 0) {
            try {
                theValue = Long.parseLong(theOverride.trim());
            } catch (NumberFormatException e) {
                // An unparseable override falls back to the default rather than stopping a
                // generation run: a wrong timeout is a much smaller problem than no output.
                theValue = theDefault;
            }
        }

        if (theValue > 0) {
            theProperties.setProperty(theDriverProperty, Long.toString(theValue));
        }
    }

    /**
     * Return a connection object.
     * <p>
     * Because of out habit of disconnecting and reconnecting access to this method is
     * limited.
     *
     * @return a live connection.
     */
    protected synchronized Connection getConnection() throws CSException {
        if (!haveConnection) {
            this.connect();
        }
        return (mrConnection);
    }

    /**
     * Cycle our connection
     */
    public synchronized Connection cycleConnection() throws CSException {
        this.disconnect();
        this.connect();
        return (mrConnection);
    }

    /**
     * Disconnect from the DB
     */
    public synchronized void disconnect() {
        if (haveConnection) {
            try {
                mrConnection.commit();
                mrConnection.close();
            } catch (SQLException e) {
                mrLog.debug(e.getMessage());
                mrConnection = null;
            }
            haveConnection = false;
            usageCounter = 0;
        }
    }

    /**
     * @return how many trancactions we have clocked up on this connection.
     */
    public synchronized int getUsageCount() {
        return (usageCounter);
    }

///**
//* increment our connection counter.
//*/
//private synchronized void recordUsage()
//  {
//  usageCounter++;
//  usageDatetime.setTime(System.currentTimeMillis());
//  }

    /**
     * Set Batch size
     */
    public void setBatchSize(int newBatchSize) {
        if (newBatchSize > 0) {
            try {
                // Tell Oracle Driver to send stuff back in batches of newBatchSize
                ((OracleConnection) mrConnection).setDefaultRowPrefetch(newBatchSize);

                // Tell Oracle Driver to send updates in batches of newBatchSize
                ((OracleConnection) mrConnection).setDefaultExecuteBatch(newBatchSize);
            } catch (SQLException e) {
                mrLog.error(e);
            }
        }
    }
}



