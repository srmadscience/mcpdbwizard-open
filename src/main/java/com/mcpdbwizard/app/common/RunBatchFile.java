package com.mcpdbwizard.app.common;

import java.util.*;
import java.io.*;

import com.mcpdbwizard.pub.*;

/**
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class RunBatchFile {
    LogInterface theLog;

    public RunBatchFile(LogInterface theLog) {
        this.theLog = theLog;
        theLog.setAutoFlush(true);
    }

    public static void main(String args[]) {
        LogInterface theLog = new ConsoleLog();
        RunBatchFile r = new RunBatchFile(theLog);
        for (int i = 17; i < 25; i++)
            r.runCmd("ping 192.168.0." + i);
        r.runCmd("dir");

    }

    public void runCmd(String theCommand) {
        try {
            theLog.info("Executing OS command: " + theCommand);

            Runtime rt = Runtime.getRuntime();
            String osName = System.getProperty("os.name");
            String[] cmd = new String[3];
            if (osName.toUpperCase().indexOf("WINDOWS") > -1) {

                cmd[0] = "cmd.exe";
                cmd[1] = "/C";
                cmd[2] = theCommand;

            } else {
                cmd[0] = "sh";
                cmd[1] = "-c";
                cmd[2] = theCommand;
            }

            Process proc = rt.exec(cmd);
            int exitVal;
            {
                // any error message?

                InputStream eStream = proc.getErrorStream();
                InputStream iStream = proc.getInputStream();

                StreamConsumer errorGetter = new StreamConsumer(eStream,
                        LogInterface.ERROR, theLog);
                //
                // // any output?

                StreamConsumer outputGetter = new StreamConsumer(iStream,
                        LogInterface.INFO, theLog);
                //
                errorGetter.start();
                outputGetter.start();
                //
                // any error???

                //theLog.debug("wait start");
                exitVal = proc.waitFor();

                Thread.sleep(1000);

                eStream.close();
                errorGetter.interrupt();
                iStream.close();
                outputGetter.interrupt();

                //theLog.debug("wait end");

            }
            for (int i = 0; i < 2; i++) {
                theLog.info("");
            }
            theLog.flush();
            theLog.info("OS Return Code: " + exitVal);
        } catch (Error e) {
            theLog.error("RunBatchFile.runCmd Error :" + theCommand);
            theLog.error(e.getMessage());
        } catch (Exception e) {
            theLog.error("RunBatchFile.runCmd Exception :" + theCommand);
            theLog.error(e);
        } catch (Throwable t) {
            theLog.error("RunBatchFile.runCmd Throwable :" + theCommand);
            theLog.error(t.getMessage());
        }
    }
}
