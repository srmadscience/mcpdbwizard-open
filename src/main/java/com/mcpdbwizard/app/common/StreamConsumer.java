package com.mcpdbwizard.app.common;

import java.util.*;
import java.io.*;

import com.mcpdbwizard.pub.*;


/**
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class StreamConsumer extends Thread {

    boolean keepGoing = true;

    InputStream is;
    String type;
    // OutputStream os;
    LogInterface theLog = null;


    //  StreamConsumer(InputStream is, String type, LogInterface theLog)
    //  {
    //      this(is, type, null, theLog);
//    }
    StreamConsumer(InputStream is, String type, /* OutputStream redirect,*/ LogInterface theLog) {
        this.is = is;
        this.type = type;
        //  this.os = redirect;
        this.theLog = theLog;
    }

    public void run() {
        try {

            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null && keepGoing) {
                if (line.length() > 0) {
                    if (this.type.equals(LogInterface.INFO)) {
                        theLog.info(line);
                    } else {
                        theLog.error(line);
                    }
                }

            }
            br.close();
            theLog.flush();

        } catch (IOException ioe) {
            theLog.error(ioe);
        }
    }

    @Override
    public void interrupt() {
        super.interrupt();
        keepGoing  = false;
    }
}

