package com.mcpdbwizard.pub;

/**
 * COMPILE-ONLY shim (see Scripts/testrun_current.sh): DAO_LOG_TYPE "Apache's
 * Log4J" makes the generated factory instantiate com.mcpdbwizard.pub.Log4JLog,
 * which historically shipped in the separate optional OBLog4JLog.jar (plus a
 * Log4j 1.x jar) and is NOT part of the pub runtime. This stub plays that
 * jar's role so generic_test5/6 compile; it logs to the console if ever run.
 */
public class Log4JLog extends ConsoleLog {
    public Log4JLog(String logName) {
        super();
    }
}
