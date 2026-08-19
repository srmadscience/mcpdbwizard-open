package com.mcpdbwizard.pub;

// We are working with JDBC

import java.sql.*;

// We use a HashMap to cache results
import java.util.HashMap;

// We use Set and Iterator to purge results from the cache.
import java.util.Set;
import java.util.Iterator;
import java.util.Map;

// We may use stats
import com.mcpdbwizard.pub.StatsInterface;

/**
 * A SELECT statement with caching.
 * <p>
 * This class implements a parameterized SQL statement with query result caching
 * that continues to exist even if the connection it uses is withdrawn. It extends
 * DMLStatement
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 * @since 2.0.1477: execute() will now attempt to reparse if the first attempt at execution fails
 */
public class QueryStatement extends DmlStatement implements OracleResourceUser
        , StatsInterface {
    /**
     * Default value for how many rows returned by a query we actually use...
     */
    public static final int DEFAULT_QUERY_ROWS = 10000;

    /**
     * Default value for how many different queries we can cache...
     */
    public static final int DEFAULT_QUERIES_CACHED = 5000;

    /**
     * Constant for caching data for the life of the class. Usage:
     * <code>setCacheSeconds(CACHE_FOREVER);</code>
     */
    public static final int CACHE_FOREVER = -1;

    /**
     * Constant for not caching data. Usage:
     * <code>setCacheSeconds(CACHE_NEVER);</code>
     */
    public static final int CACHE_NEVER = 0;
    /**
     * Variable to store Buffer Size for file access.
     */
    protected int bufferSize = 4096;
    /**
     * Variable to store boolean flag that indicates whether created files should be deletes when the JVM exits.
     */
    protected boolean keepFiles = true;
    /**
     * Variable to store boolean flag that indicates whether we should turn LOBs into files or not.
     */
    protected boolean keepLobs = false;
    /**
     * boolean flag that specifies whether lobs such as CLOBS, BLOBS and BFILES
     * and LONG columns
     * will be kept as byte arrays. This only works if keepFiles == false.
     *
     * @since 5.0.2314
     */
    protected boolean useByteArraysForLongsAndLOBS = false;
    /**
     * Variable to store temporary directory for downloaded files.
     * If you are downloading lots of files you will want to change this to
     * an application specific directory
     */
    protected java.io.File tempFileDir = IOUtils.getOsTempDir();
    /**
     * The Prefix for downloaded files containing BLOB, CLOB and BFILE data
     */
    protected String tempFilePrefix = IOUtils.DEFAULT_TEMP_FILE_PREFIX;
    /**
     * The Suffix for downloaded files containing BLOB, CLOB and BFILE data
     */
    protected String tempFileSuffix = IOUtils.DEFAULT_TEMP_FILE_SUFFIX;
    /**
     * How many rows returned by a query we actually use...
     */
    int maxQueryRows = DEFAULT_QUERY_ROWS;
    /**
     * How long we cache the results for. Setting this to a non-zero value turns caching on.
     */
    int maxCacheSeconds = CACHE_NEVER;
    /**
     * The maximum number of rows we will cache. Once this limit is reached nothing
     * will be added to the cache until space is freed up.
     */
    int maxCacheRows = DEFAULT_QUERIES_CACHED;
    /**
     * Cached results of queries...
     */
    HashMap cachedQueryResults = new HashMap();
    /**
     * Signature of latest set of results.
     */
    String latestQuerySignature = null;
    /**
     * Counter for number of Parses - used by StatsInterface
     *
     * @see StatsInterface
     * See <a href="https://mcpdbwizard.com/faq/stats">StatsInterface - a generic set of performance measuring methods</a>
     */
    long parseCount = 0;

    /**
     * Counter for amount of time spent parsing in milliseconds - used by StatsInterface
     *
     * @see StatsInterface
     * See <a href="https://mcpdbwizard.com/faq/stats">StatsInterface - a generic set of performance measuring methods</a>
     */
    long parseTimeMilliseconds = 0;

    /**
     * Counter for number of executions - used by StatsInterface
     *
     * @see StatsInterface
     * See <a href="https://mcpdbwizard.com/faq/stats">StatsInterface - a generic set of performance measuring methods</a>
     */
    long executionCount = 0;

    /**
     * Counter for amount of time spent executing - used by StatsInterface
     *
     * @see StatsInterface
     * See <a href="https://mcpdbwizard.com/faq/stats">StatsInterface - a generic set of performance measuring methods</a>
     */
    long executionTimeMilliseconds = 0;

    /**
     * Counter for amount of time spent retrieving - used by StatsInterface
     *
     * @see StatsInterface
     * See <a href="https://mcpdbwizard.com/faq/stats">StatsInterface - a generic set of performance measuring methods</a>
     */
    long retrieveTimeMilliseconds = 0;

    /**
     * Counter for number of Connection Releases - used by StatsInterface
     *
     * @see StatsInterface
     * See <a href="https://mcpdbwizard.com/faq/stats">StatsInterface - a generic set of performance measuring methods</a>
     */
    long releaseCount = 0;

    /**
     * Counter for number of Errors - used by StatsInterface
     *
     * @see StatsInterface
     * See <a href="https://mcpdbwizard.com/faq/stats">StatsInterface - a generic set of performance measuring methods</a>
     */
    long errorCount = 0;

    /**
     * Counter for how long a statement has spent executing or parsing - used by StatsInterface
     *
     * @see StatsInterface
     * See <a href="https://mcpdbwizard.com/faq/stats">StatsInterface - a generic set of performance measuring methods</a>
     */
    long statsEventTimer = 0;

    /**
     * Create a DML Statement object and give it a connection
     */
    public QueryStatement(String statementSqlText, LogInterface theLog, Connection theConnection) {
        super(statementSqlText, theLog);
        setConnection(theConnection);
    }

    /**
     * Create a DML Statement object.
     */
    public QueryStatement(String statementSqlText, LogInterface theLog) {
        super(statementSqlText, theLog);
    }

    /**
     * Specify how long results will be cached.
     * <p>
     * Setting this to a non-zero value turns caching on.
     *
     * @param newCacheSeconds
     */
    public void setCacheSeconds(int newCacheSeconds) {
        if (newCacheSeconds >= CACHE_FOREVER) {
            maxCacheSeconds = newCacheSeconds;
        }
    }

    /**
     * Specify how many results will be cached.
     * <p>
     * By default this is DEFAULT_QUERIES_CACHED.
     *
     * @param newCacheRows
     */
    public void setCacheRows(int newCacheRows) {
        if (newCacheRows >= 0) {
            maxCacheRows = newCacheRows;
        }
    }

    /**
     * Return the number of queries currently in the cache.
     */
    public int getCacheSize() {
        return (cachedQueryResults.size());
    }


    /**
     * Clear the cache.
     */
    public void clearCache() {
        cachedQueryResults = new HashMap();
    }

    /**
     * Specify how many rows can be brought back
     *
     * @param newQueryRows
     */
    public void setQueryRows(int newQueryRows) {
        if (newQueryRows >= 0) {
            maxQueryRows = newQueryRows;
        }
    }

    /**
     * Execute the statement and return a ReadOnlyRowSet
     * <p>
     * If caching is in use the cache will be checked and a cached
     * copy returned if possible.
     *
     * @return ReadOnlyRowSet The results from this query
     * @throws CSException
     * @since 2.0.1477: execute() will now attempt to reparse if the first attempt at execution fails
     * @since 5.0.2314 'useByteArraysForLongsAndLOBS' added.
     */
    public ReadOnlyRowSet execute() throws CSException {
        ReadOnlyRowSet latestQueryRowSet = null;

        // The signature is used to see if we have cached this query already.
        String parameterSig = getSignature();

        // If we are caching attempt to return a cached copy.
        // Note that we don't worry about having a connection at this point.
        // Check our cached copy before we hand it back in case its stale.
        if (maxCacheSeconds != CACHE_NEVER // Caching is turned on
                && cachedQueryResults.containsKey(parameterSig)) // The cache contains our query
        {
            //A cached copy exists....
            ReadOnlyRowSet tempRowSet = (ReadOnlyRowSet) cachedQueryResults.get(parameterSig);

            // check expiry date
            if (maxCacheSeconds != CACHE_FOREVER && tempRowSet.hasExpired()) {
                // but it's too old.
                // Remove it from HashMap
                cachedQueryResults.remove(parameterSig);
            } else {
                // Its ok so we'll return it.
                // Increment hit counter and return the temp row set.
                tempRowSet.incrementTimesUsed();
                return (tempRowSet);
            }
        }

        // Since we can't find it in the cache we're going to have to go to the DB...

        // Complain if we are without a connection...
        testConnection();

        try {
            // Create prepared Statement if we need to.
            startStatsTimer();
            createPreparedStatement();
            incParseCount();

            //If we have any parameters bind them
            bindParameters(thePreparedStatement);

            // Execute our query
            startStatsTimer();
            ResultSet theResultSet = thePreparedStatement.executeQuery();
            incExecutionCount();

            // Retrieve the results into a ReadOnlyRowSet
            startStatsTimer();
            latestQueryRowSet = new ReadOnlyRowSet(theResultSet, statementSqlText
                    , maxQueryRows, theLog, tempFileDir, keepFiles
                    , tempFilePrefix, tempFileSuffix
                    , keepLobs, useByteArraysForLongsAndLOBS);
            latestQuerySignature = new String(parameterSig);
            incRetrieveTime();

        } catch (SQLException e) {
            // Something went wrong - it could be that since we last tried to use this
            // connection object someone called COMMIT or ROLLBACK and destroyed our
            // prepared statement. Try once more.

            // Force recreation of prepared Statement.
            thePreparedStatement = null;

            try {
                // Parse
                startStatsTimer();
                createPreparedStatement();
                incParseCount();

                // Bind
                bindParameters(thePreparedStatement);

                // Execute
                startStatsTimer();
                ResultSet theResultSet = thePreparedStatement.executeQuery();
                incExecutionCount();

                // Retrieve
                startStatsTimer();
                latestQueryRowSet = new ReadOnlyRowSet(theResultSet, statementSqlText
                        , maxQueryRows, theLog, tempFileDir, keepFiles
                        , tempFilePrefix, tempFileSuffix
                        , keepLobs);
                latestQuerySignature = new String(parameterSig);
                incRetrieveTime();
            } catch (SQLException e2) {
                // if we get here it means that we had an error, reset everything, tried again
                // and still got an error. It's time to give up.
                thePreparedStatement = null;
                throw new CSDBException(e2.getErrorCode(), e2.getSQLState(), statementSqlText
                        , "Unable to execute this statement. First Message:" + e.getMessage()
                        + " Second Message:" + e2.getMessage());
            }
        }

        // If some form of caching is in use...
        if (maxCacheSeconds != CACHE_NEVER) {
            if (cachedQueryResults.size() >= maxCacheRows) {
                // Unable to cache query results due to cache being full.
            } else if (ResourceWatcher.freeMemAsPct() > ResourceWatcher.MIN_SAFE_MEMORY_PCT) {
                // If the rows are to be cached for a finite amount of time set an expiry date.
                if (maxCacheSeconds != CACHE_FOREVER) {
                    latestQueryRowSet.setExpireDate(maxCacheSeconds * 1000);
                }

                cachedQueryResults.put(parameterSig, latestQueryRowSet);
            } else {
                theLog.debug("Unable to cache query results for the statement due to shortage of memory. Cache limited to " + cachedQueryResults.size(), false, true);
            }
        }

        return (latestQueryRowSet);
    }

    /**
     * Remove entries that are too old from the cache
     *
     * @param howManyMilliseconds The maximum amount of time to be spent removing entries.
     * @return int How many entries were removed.
     */
    public int purgeExpiredCacheEntries(int howManyMilliseconds) {
        // Define when we should stop purging the cache because we've used up our
        // alloted time
        long endTime = System.currentTimeMillis() + howManyMilliseconds;

        // How many items we have removed so far
        int howMany = 0;

        Set cachedSet = cachedQueryResults.entrySet();
        Iterator cachedSetIterator = cachedSet.iterator();

        // Go thorugh the cache and look for stuff thats past its sell by date
        while (cachedSetIterator.hasNext()) {
            Map.Entry me = (Map.Entry) cachedSetIterator.next();
            ReadOnlyRowSet aRow = (ReadOnlyRowSet) me.getValue();

            if (aRow.hasExpired()) {
                // Remove row.
                cachedSetIterator.remove();
                howMany++;
            }

            if (endTime < System.currentTimeMillis()) {
                theLog.warning("Ran out of time while trying to purge expired cache entries - " +
                        "was able to remove " + howMany + " in " + howManyMilliseconds + "ms ");
                break;
            }
        }

        return (howMany);
    }

    /**
     * Remove entries that have not been reused from the cache
     *
     * @param minAcceptableReuses How many times an entry must have been used to avoid
     *            deletion.
     * @param howManyMilliseconds The maximum amount of time to be spent removing entries.
     * @return int How many entries were removed.
     */
    public int purgeUnderusedCacheEntries(int minAcceptableReuses, int howManyMilliseconds) {
        // Define when we should stop purging the cache because we've used up our
        // alloted time
        long endTime = System.currentTimeMillis() + howManyMilliseconds;

        // How many items we have removed so far
        int howMany = 0;

        Set cachedSet = cachedQueryResults.entrySet();
        Iterator cachedSetIterator = cachedSet.iterator();

        // Go thorugh the cache and look for stuff that isnt being used...
        while (cachedSetIterator.hasNext()) {
            Map.Entry me = (Map.Entry) cachedSetIterator.next();
            ReadOnlyRowSet aRow = (ReadOnlyRowSet) me.getValue();

            if (aRow.getTimesUsed() < minAcceptableReuses) {
                // Remove row.
                cachedSetIterator.remove();
                howMany++;
            }

            if (endTime < System.currentTimeMillis()) {
                theLog.warning("Ran out of time while trying to purge underused cache entries - " +
                        "was able to remove " + howMany + " in " + howManyMilliseconds + "ms ");
                break;
            }
        }

        return (howMany);
    }

    /**
     * Return an up to date signature of the query parameters.
     */
    public String getLatestQuerySignature() {
        return (latestQuerySignature);
    }

    /**
     * Get current file io buffer size
     *
     * @return int bufferSize Buffersize in bytes.
     */
    public int getBufferSize() {
        return (bufferSize);
    }

    /**
     * Set new file io buffer size
     *
     * @param bufferSize A new Buffersize in bytes.
     */
    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    /**
     * Get keepFiles flag
     *
     * @return int <code>true</code> if temporary files are kept after the JVM exits.
     *         int <code>false</code> if temporary files are deleted after the JVM exits.
     */
    public boolean getKeepFiles() {
        return (keepFiles);
    }

    /**
     * Set keepFiles
     *
     * @param keepFiles Keep generated files after JVM exits
     */
    public void setKeepFiles(boolean keepFiles) {
        this.keepFiles = keepFiles;
    }

    /**
     * Get temporary directory
     *
     * @return the temporary directory
     */
    public java.io.File getTempDir() {
        return (tempFileDir);
    }

    /**
     * Set temporary directory
     *
     * @param tempFileDir a new Temporary Directory
     * @throws CSException if the directory is not viable
     * @since 2.0.879
     */
    public void setTempDir(java.io.File tempFileDir) throws CSException {
        if (tempFileDir == null) {
            throw (new CSException("Attempt made to set tempFileDir to null"));
        } else if (!tempFileDir.exists()) {
            try {
                theLog.info("Creating temporary directory " + tempFileDir.getAbsolutePath());
                tempFileDir.mkdirs();
            } catch (Exception e) {
                throw (new CSException("tempFileDir " + tempFileDir.getAbsolutePath() + " can not be created"));
            }
        }

        this.tempFileDir = tempFileDir;
    }

    /**
     * Get the prefix used for generating temporary files
     */
    public String getTempFilePrefix() {
        return (tempFilePrefix);
    }

    /**
     * Set the prefix used for generating temporary files
     */
    public void setTempFilePrefix(String tempFilePrefix) {
        this.tempFilePrefix = tempFilePrefix;
    }

    /**
     * Get the suffix used for generating temporary files
     */
    public String getTempFileSuffix() {
        return (tempFileSuffix);
    }

    /**
     * Set the suffix used for generating temporary files
     */
    public void setTempFileSuffix(String tempFileSuffix) {
        this.tempFileSuffix = tempFileSuffix;
    }

    /**
     * Get keepLobs
     *
     * @return boolean keepLobs Keep LOB objects such as CLOB, BLOB and BFILE as LOBS instead of turning them into files on retrieval.
     * @since 2.0.1505
     */
    public boolean getKeepLobs() {
        return (keepLobs);
    }

    /**
     * Set keepLobs
     *
     * @param keepLobs Keep LOB objects such as CLOB, BLOB and BFILE as LOBS instead of turning them into files on retrieval.
     * @since 2.0.1505
     */
    public void setKeepLobs(boolean keepLobs) {
        this.keepLobs = keepLobs;
    }

    /**
     * Get keepLobs
     *
     * @return boolean useByteArraysForLongsAndLOBS flag that specifies whether lobs such as CLOBS, BLOBS and BFILES
     * and LONG columns will be kept as byte arrays
     * @since 5.0.2314
     */
    public boolean getUseByteArraysForLongsAndLOBS() {
        return (useByteArraysForLongsAndLOBS);
    }

    /**
     * SetUseByteArraysForLongsAndLOBS
     *
     * @param useByteArraysForLongsAndLOBS  flag that specifies whether lobs such as CLOBS, BLOBS and BFILES
     *                and LONG columns will be kept as byte arrays
     * @since 5.0.2314
     */
    public void setUseByteArraysForLongsAndLOBS(boolean useByteArraysForLongsAndLOBS) {
        this.useByteArraysForLongsAndLOBS = useByteArraysForLongsAndLOBS;
    }

    /**
     * Reset all stats counters to 0. Used to implement StatsInterface
     *
     * @see StatsInterface
     * See <a href="https://mcpdbwizard.com/faq/stats">StatsInterface - a generic set of performance measuring methods</a>
     */
    public void resetStatsCounters() {
        parseCount = 0;
        executionCount = 0;
        parseTimeMilliseconds = 0;
        executionTimeMilliseconds = 0;
        retrieveTimeMilliseconds = 0;
        releaseCount = 0;
        errorCount = 0;
    }

    /**
     * Return counter containing number of parses. Used to implement StatsInterface
     *
     * @see StatsInterface
     * See <a href="https://mcpdbwizard.com/faq/stats">StatsInterface - a generic set of performance measuring methods</a>
     */
    public long getParses() {
        return (parseCount);
    }

    /**
     * Return counter containing time spent parsing in milliseconds. Used to implement StatsInterface
     *
     * @see StatsInterface
     * See <a href="https://mcpdbwizard.com/faq/stats">StatsInterface - a generic set of performance measuring methods</a>
     */
    public long getParseTime() {
        return (parseTimeMilliseconds);
    }

    /**
     * Return counter containing number of executions. Used to implement StatsInterface
     *
     * @see StatsInterface
     * See <a href="https://mcpdbwizard.com/faq/stats">StatsInterface - a generic set of performance measuring methods</a>
     */
    public long getExecutions() {
        return (executionCount);
    }

    /**
     * Return counter containing time spent executing statement in milliseconds. Used to implement StatsInterface
     *
     * @see StatsInterface
     * See <a href="https://mcpdbwizard.com/faq/stats">StatsInterface - a generic set of performance measuring methods</a>
     */
    public long getExecutionTime() {
        return (executionTimeMilliseconds);
    }

    /**
     * Return counter containing time spent retrieving data in milliseconds. Used to implement StatsInterface
     *
     * @see StatsInterface
     * See <a href="https://mcpdbwizard.com/faq/stats">StatsInterface - a generic set of performance measuring methods</a>
     */
    public long getRetrievalTime() {
        return (retrieveTimeMilliseconds);
    }

    /**
     * Return counter containing number of releases. Used to implement StatsInterface
     *
     * @see StatsInterface
     * See <a href="https://mcpdbwizard.com/faq/stats">StatsInterface - a generic set of performance measuring methods</a>
     */
    public long getReleases() {
        return (releaseCount);
    }

    /**
     * Return counter containing number of errors. Used to implement StatsInterface
     *
     * @see StatsInterface
     * See <a href="https://mcpdbwizard.com/faq/stats">StatsInterface - a generic set of performance measuring methods</a>
     */
    public long getErrors() {
        return (errorCount);
    }

    /**
     * Start timer used to keep track of parse and execution time - used to implement StatsInterface
     *
     * @see StatsInterface
     * See <a href="https://mcpdbwizard.com/faq/stats">StatsInterface - a generic set of performance measuring methods</a>
     */
    protected void startStatsTimer() {
        statsEventTimer = System.currentTimeMillis();
    }

    /**
     * Increment counter used to keep track of parses - used to implement StatsInterface
     *
     * @see StatsInterface
     * See <a href="https://mcpdbwizard.com/faq/stats">StatsInterface - a generic set of performance measuring methods</a>
     */
    private void incParseCount() {
        parseTimeMilliseconds = parseTimeMilliseconds + (System.currentTimeMillis() - statsEventTimer);
        statsEventTimer = 0;

        if (parseCount < Long.MAX_VALUE) {
            parseCount++;
        } else {
            theLog.syserror("parse counter is greater than " + parseCount);
        }
    }

    /**
     * Increment counter used to keep track of executions - used to implement StatsInterface
     *
     * @see StatsInterface
     * See <a href="https://mcpdbwizard.com/faq/stats">StatsInterface - a generic set of performance measuring methods</a>
     */
    private void incExecutionCount() {
        executionTimeMilliseconds = executionTimeMilliseconds + (System.currentTimeMillis() - statsEventTimer);
        statsEventTimer = 0;

        if (executionCount < Long.MAX_VALUE) {
            executionCount++;
        } else {
            theLog.syserror("execution counter is greater than " + executionCount);
        }
    }

    /**
     * Increment variable used to keep track of retrieval time - used to implement StatsInterface
     *
     * @see StatsInterface
     * See <a href="https://mcpdbwizard.com/faq/stats">StatsInterface - a generic set of performance measuring methods</a>
     */
    protected void incRetrieveTime() {
        retrieveTimeMilliseconds = retrieveTimeMilliseconds + (System.currentTimeMillis() - statsEventTimer);
        statsEventTimer = 0;
    }

    /**
     * Increment counter used to keep track of releases - used to implement StatsInterface
     *
     * @see StatsInterface
     * See <a href="https://mcpdbwizard.com/faq/stats">StatsInterface - a generic set of performance measuring methods</a>
     */
    private void incReleaseCount() {
        if (releaseCount < Long.MAX_VALUE) {
            releaseCount++;
        } else {
            theLog.syserror("release counter is greater than " + releaseCount);
        }
    }

    /**
     * Increment counter used to keep track of errors - used to implement StatsInterface
     *
     * @see StatsInterface
     * See <a href="https://mcpdbwizard.com/faq/stats">StatsInterface - a generic set of performance measuring methods</a>
     */
    private void incErrorCount() {
        if (errorCount < Long.MAX_VALUE) {
            errorCount++;
        } else {
            theLog.syserror("error counter is greater than " + releaseCount);
        }
    }
}



