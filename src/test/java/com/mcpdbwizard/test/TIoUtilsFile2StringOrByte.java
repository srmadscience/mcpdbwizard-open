package com.mcpdbwizard.test;

import com.mcpdbwizard.pub.*;
import java.io.*;

/**
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class TIoUtilsFile2StringOrByte extends AbstractLocalHarness
{
  File testFileDir = null;
  File tempFileDir;

  public TIoUtilsFile2StringOrByte()
  {
  tempFileDir = new File(TestdirWrangler.getTempDir());


  if (! tempFileDir.exists())
    {
    tempFileDir.mkdirs();
    }

  }


	public String getTestName() {
		return(this.getClass().getName());
	}

  public boolean test(boolean isInteractive)
  {
  boolean retcode = true;
  String testString = "A Quick Brown Fox Jumped over the Lazy Dog.";
  byte[]   testBytes = testString.getBytes();

  String prefix = "tIoUtils";
  String suffix = ".tmp";
  String testDir = tempFileDir.getAbsolutePath();

  try
    {
    theLog.info("Start Load File into String - normal");
    File stringFile = IOUtils.loadStringIntoFile(testString, prefix, suffix, testDir, theLog);

    String stringString = IOUtils.loadFileIntoString(stringFile);

    if (!stringString.equals(testString))
      {
      throw new CSException("File and String are not the same");
      }

    stringFile.delete();
    theLog.info("End Load File into String - normal");
    }
  catch (Exception e)
    {
    theLog.error("Test FAILED");
    theLog.error(e);
    retcode = false;
    }

  try
    {
    theLog.info("Start Load File into String - null");
    File stringFile = IOUtils.loadStringIntoFile(testString, prefix, suffix, testDir, theLog);

    String stringString = IOUtils.loadFileIntoString(null);

    if (stringString == null || stringString.length() > 0)
      {
      throw new CSException("File and String are not the same");
      }

    stringFile.delete();
    theLog.info("End Load File into String - null");
    }
  catch (Exception e)
    {
    theLog.error("Test FAILED");
    theLog.error(e);
    retcode = false;
    }

  try
    {
    theLog.info("Start Load File into String - zero");
    File stringFile = IOUtils.loadStringIntoFile("", prefix, suffix, testDir, theLog);

    String stringString = IOUtils.loadFileIntoString(stringFile);

    if (stringString == null || stringString.length() > 0)
      {
      throw new CSException("File and String are not the same");
      }

    stringFile.delete();
    theLog.info("End Load File into String - zero");
    }
  catch (Exception e)
    {
    theLog.error("Test FAILED");
    theLog.error(e);
    retcode = false;
    }

  try
    {
    theLog.info("Start Load File into String - no file");
    File stringFile = IOUtils.loadStringIntoFile("", prefix, suffix, testDir, theLog);
    stringFile.delete();

    String stringString = IOUtils.loadFileIntoString(stringFile);

    if (stringString == null || stringString.length() > 0)
      {
      throw new CSException("File and String are not the same");
      }

    //stringFile.delete();
    theLog.info("End Load File into String - no file");
    }
  catch (Exception e)
    {
    theLog.error("Test FAILED");
    theLog.error(e);
    retcode = false;
    }

   try
    {
    theLog.info("Start Load File into String - big");
    String bigString = new String(testString);

    StringBuffer bf = new StringBuffer(bigString);
    for (int i=0; i < 1000000; i++)
      {
      bf.append(bigString);
      //System.out.println(bf.length());
      if (bf.length() > (1024*1024))
        {
        break;
        }
      }

    int theLength = bf.length();
    
    File stringFile = IOUtils.loadStringIntoFile(bigString, prefix, suffix, testDir, theLog);

    String stringString = IOUtils.loadFileIntoString(stringFile);

    if (!stringString.equals(bigString))
      {
      throw new CSException("File and String are not the same");
      }

    stringFile.delete();
    theLog.info("End Load File into String - big");
    }
  catch (Exception e)
    {
    theLog.error("Test FAILED");
    theLog.error(e);
    retcode = false;
    }


  return(retcode);
  }

}


