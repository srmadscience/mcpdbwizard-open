package com.mcpdbwizard.test;

import com.mcpdbwizard.pub.*;
import java.io.*;

/**
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class TIoUtilsString2File extends AbstractLocalHarness
{
  File testFileDir = null;
  File tempFileDir;

  public TIoUtilsString2File()
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
    theLog.info("Start String and Byte files - normal");
    File stringFile = IOUtils.loadStringIntoFile(testString, prefix, suffix, testDir, theLog);
    File byteFile = IOUtils.loadByteArrayIntoFile(testBytes, prefix, suffix, testDir, theLog);

    if (stringFile == null)
      {
      throw new CSException("Null file seen");
      }

    if (! stringFile.exists())
      {
      throw new CSException("no file found");
      }

    if (stringFile.length() != testString.length())
      {
      throw new CSException("file size differs");
      }

    if (byteFile == null)
      {
      throw new CSException("Null file seen");
      }

    if (! byteFile.exists())
      {
      throw new CSException("no file found");
      }

    if (byteFile.length() != testString.length())
      {
      throw new CSException("file size differs");
      }


    stringFile.delete();
    byteFile.delete();

    theLog.info("End String and Byte files - normal");
    }
  catch (Exception e)
    {
    theLog.error("Test FAILED");
    theLog.error(e);
    retcode = false;
    }

  try
    {
    String nullString = null;
    byte[] nullBytes = null;

    theLog.info("Start String and Byte files - null");
    File stringFile = IOUtils.loadStringIntoFile(nullString, prefix, suffix, testDir, theLog);
    File byteFile = IOUtils.loadByteArrayIntoFile(nullBytes, prefix, suffix, testDir, theLog);

    if (stringFile == null)
      {
      throw new CSException("Null file seen");
      }

    if (! stringFile.exists())
      {
      throw new CSException("no file found");
      }

    if (stringFile.length() > 0)
      {
      throw new CSException("file size differs");
      }

    if (byteFile == null)
      {
      throw new CSException("Null file seen");
      }

    if (! byteFile.exists())
      {
      throw new CSException("no file found");
      }

    if (byteFile.length() > 0)
      {
      throw new CSException("file size differs");
      }


    stringFile.delete();
    byteFile.delete();

    theLog.info("End String and Byte files - null");
    }
  catch (Exception e)
    {
    theLog.error("Test FAILED");
    theLog.error(e);
    retcode = false;
    }

  try
    {
    String nullString = "";
    byte[] nullBytes = new byte[0];

    theLog.info("Start String and Byte files - 0");
    File stringFile = IOUtils.loadStringIntoFile(nullString, prefix, suffix, testDir, theLog);
    File byteFile = IOUtils.loadByteArrayIntoFile(nullBytes, prefix, suffix, testDir, theLog);

    if (stringFile == null)
      {
      throw new CSException("Null file seen");
      }

    if (! stringFile.exists())
      {
      throw new CSException("no file found");
      }

    if (stringFile.length() > 0)
      {
      throw new CSException("file size differs");
      }

    if (byteFile == null)
      {
      throw new CSException("Null file seen");
      }

    if (! byteFile.exists())
      {
      throw new CSException("no file found");
      }

    if (byteFile.length() > 0)
      {
      throw new CSException("file size differs");
      }


    stringFile.delete();
    byteFile.delete();

    theLog.info("End String and Byte files - 0");
    }
  catch (Exception e)
    {
    theLog.error("Test FAILED");
    theLog.error(e);
    retcode = false;
    }

  return(retcode);
  }
/**
 * Turns a file into a Byte array. The file is deleted.
 * @param theFile A file we wish to load into a Byte Array
 * @return A Byte array containing the contents of the file
 */
private byte[] loadFileIntoByteArray(java.io.File theFile) throws CSException
  {
  byte[] newArray = null;
  try
       {
       if (theFile != null && theFile.exists() && theFile.length() > 0)
      {
         if (theFile.length() > Integer.MAX_VALUE)
           {
           throw new CSException("loadFileIntoByteArray: File " 
                                    + theFile.getAbsolutePath()
                                                        + " is too big to be turned into a Byte array");
           }
         
      theLog.debug("CLASS_NAME: loadByteArrayIntoFile: Starting to load " 
                   + theFile.length()
                       + " from temp file " + theFile.getAbsolutePath() + " into byte array");

      newArray = new byte[(int)theFile.length()];
         java.io.BufferedInputStream theInputStream = new java.io.BufferedInputStream(new java.io.FileInputStream(theFile));
      theInputStream.read(newArray);
      // Close inputstream...
      theInputStream.close();
      theLog.debug("CLASS_NAME: loadByteArrayIntoFile: Finished loading byte array");
      }
       else
         {
         theLog.debug("CLASS_NAME: loadByteArrayIntoFile: Turning empty/null file into byte array"); 
         }
       
       }
  catch (java.io.IOException e)
       {
       throw new CSException(e.getMessage());
       }
  catch (Exception e)
       {
       throw new CSException(e.getMessage());
       }

  return (newArray);
  }


private java.io.File loadByteArrayIntoFile(byte[] theBytes) throws CSException
  {
  java.io.File newFile = null;
  try
       {
    java.io.FileOutputStream outStream = null;
    newFile = java.io.File.createTempFile("abc","tmp");
    newFile.deleteOnExit();
         
       outStream = new java.io.FileOutputStream(newFile);

       if (theBytes != null && theBytes.length > 0)
         {
      theLog.debug("CLASS_NAME: loadByteArrayIntoFile: Starting to write " 
                           + theBytes.length
                                       + " bytes to temp file " + newFile.getAbsolutePath());
       
      outStream.write(theBytes,0,theBytes.length);
         }
       else
         {
         theLog.debug("CLASS_NAME: loadByteArrayIntoFile: Turning empty/null byte array into temp file "+ newFile.getAbsolutePath());   
         }

       // Flush and close output stream
       outStream.flush();
       outStream.close();
       theLog.debug("CLASS_NAME: loadByteArrayIntoFile: Finished writing temp file " + newFile.getAbsolutePath());  

       }
  catch (java.io.IOException e)
       {
       throw new CSException("Unable to unload column of type " + e.getMessage());
       }
  catch (Exception e)
       {
       throw new CSException("Unable to unload column of type " + e.getMessage());
       }
  
  return(newFile);    
  }
}

