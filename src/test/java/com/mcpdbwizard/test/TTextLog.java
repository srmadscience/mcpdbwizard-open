package com.mcpdbwizard.test;

/**
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class TTextLog extends AbstractLocalHarness
{

  public String getTestName()
  {
  return("Text log user.home + user.dir");
  }

  public boolean test(boolean isInteractive)
  {
  String userHome = System.getProperty("user.home");
  String userDir = System.getProperty("user.dir");

  com.mcpdbwizard.pub.TextLog aLog;

  theLog.info("Checking user.dir");
  aLog = new com.mcpdbwizard.pub.TextLog(userDir,"OBTEST");
  aLog.info("foo");

  if (! aLog.getCurrentLog().startsWith(userDir))
    {
    theLog.error("user.dir not working");
    return(false);
    }

  java.io.File tempFile = new java.io.File(aLog.getCurrentLog());
  tempFile.delete();

  theLog.info("Checking user.home");
  aLog = new com.mcpdbwizard.pub.TextLog(userHome,"OBTEST");
  aLog.info("foo");

  if (! aLog.getCurrentLog().startsWith(userHome))
    {
    theLog.error("user.home not working");
    return(false);
    }

  tempFile = new java.io.File(aLog.getCurrentLog());
  tempFile.delete();



  return (true);
  }
}

