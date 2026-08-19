package com.mcpdbwizard.test;

import com.mcpdbwizard.pub.*;



/**
* @version 2
* @author  devteam@mcpdbwizard.com
* Copyright 2003-2026 ATB Consultancy Services Ltd
* (formerly Orinda Software Ltd, Dublin, Ireland)
*/
public class TSqlDefineChar extends AbstractLocalHarness
{

  boolean outcome = true;

  public TSqlDefineChar()
  {
  theLog.info("TSqlDefineChar started");
  theLog.setAutoFlush(true);
  }

  public String getTestName()
  {
  return("Test define char replacement");
  }
  public boolean test(boolean isInteractive)
  {
  boolean retCode = true;

  String[] inputText =
    { " emp = &emp "
    , "emp = ? "
    , "emp = &1 "
    , "emp = '&1' "
    , "emp = &ampersand and emp not between &ampersand and &ampersand "
    , "emp=&foo or to_date('&date','DD-MON-YYYY') < sysdate"
    , "emp = &&bar"
    };

  String[] outputText =
    { "emp = ? /* emp */"
    , "emp = ?"
    , "emp = ? /* 1 */"
    , "emp = ? /* 1 */"
    , "emp = ? /* ampersand */ and emp not between ? /* ampersand */ and ? /* ampersand */"
    , "emp=? /* foo */ or to_date(? /* date */,'DD-MON-YYYY') < sysdate"
    , "emp = ? /* bar */"
     };


  try
    {
    if (inputText.length != outputText.length)
      {
      throw (new Exception("Array lengths do not match"));
      }

    for (int i=0; i < inputText.length; i++)
      {
      theLog.info(i + ": " + inputText[i]);
      String result = com.mcpdbwizard.app.procbuilder.SqlStatementWrangler.sqlplusParams(inputText[i],'&');
      if (! outputText[i].equals(result))
        {
        theLog.error("define char mapping failed:");
        theLog.error(inputText[i]+"|");
        theLog.error(result+"|");
        theLog.error(outputText[i]+"|");
        }
      else
        {
        theLog.info(i + ": " + result);
        }
      }
    }
  catch (Exception e)
    {
    retCode = false;
    theLog.syserror(getTestName() + " Failed: " + e.toString());
    }

  return(retCode);
  }
}


