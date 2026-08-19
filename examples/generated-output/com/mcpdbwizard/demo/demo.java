package com.mcpdbwizard.demo;

import com.mcpdbwizard.pub.ReadOnlyRowSet;
import com.mcpdbwizard.pub.TextLog;
import com.mcpdbwizard.demo.generated.*;
import java.sql.*;

/***
* This class demonstrates PARAM_PROD_NAME.
* This demo class assumes that the PARAM_PROD_NAME database demo user
* has been created along with its tables and procedures.
*
* @version PARAM_VERSION
* @author  PARAM_AUTHOR
*/
public class demo
{
  /**
  * TCP/IP IP Address of server
  * Change this value to one that will work for you.
  */
  public String theIpAddress = "203.0.113.10";

  /**
  * TCP/IP Port in use by Sql*Net
  * Change this value to one that will work for you.
  */
  public int thePort = 1521;

  /**
  * Database Instance Name
  * Change this value to one that will work for you.
  */
  public String theSid = "DB920";

  /**
  * Database User
   * Change this value to one that will work for you.
 */
  public String theUser = "ORINDADEMO";

  /**
  * Database Password
  * Change this value to one that will work for you.
  */
  public String thePassword = "ORINDADEMO";

  /**
  *  Log Object
  */
  public com.mcpdbwizard.pub.TextLog theLog;

  /**
  * Database connection
  */
  Connection theConnection = null;

  public demo(String[] args)
  {

  // Create a log object. It will assume that the direcotry refererred to
  // by the Java property user.dir is a suitable location for log files.
  theLog = new TextLog(".log");

  // Tell TextLog to write messages to the console as well as
  // the log file.
  theLog.setConsoleOutput(true);

  // Attempt to log into the database.
  try
    {
    theConnection = DriverManager.getConnection (
                  "jdbc:oracle:thin:@"
                  + theIpAddress
                  + ":" + thePort
                  + ":" + theSid,
                  theUser, thePassword);

    theLog.info("Logged in as user " + theUser);

    // By default JDBC drivers commit automatically. Turn this off.
    theConnection.setAutoCommit(false);

    theLog.info(" ");
    theLog.info(" First Example: Retrieve lists of airlines, airports and aircraft");
    theLog.info(" ");

    // Create an instance of our generated access class.
    simpleExamplesGetlists theListGetter = new simpleExamplesGetlists(theConnection, theLog);

    // Execute it...
    theListGetter.executeProc();

    // Print out the first 10 items of each list....
    com.mcpdbwizard.pub.ReadOnlyRowSet airportList = theListGetter.getParamPAirportListing();

    // Loop through airportList until we get to 10 records or run out of records
    for (int i=0; i < 10 && i < airportList.size(); i++)
      {
      // You navigate up and down a ReadOnlyRowSet by moving between rows.
      airportList.setCurrentRowNumber(i);

      // Once you have picked your row most methods will operate on the current one.
      // the ":" character is used as a field seperator
      // the "?" character is used when the field is unprintable.
      theLog.info(airportList.getRowAsString(":","?"));
      }

    }
  catch (com.mcpdbwizard.pub.CSException e)
    {
    theLog.syserror(e);
    theLog.syserror("Exiting due to MCPDBWizard library Exception");
    System.exit(1);
    }
  catch (SQLException e)
    {
    theLog.syserror(e);
    theLog.syserror("Exiting due to SQL Exception");
    System.exit(2);
    }

  // Flush the log file.
  theLog.flush();
  }

  public static void main(String[] args)
  {
    demo vTestRunner = new demo(args);
    vTestRunner.invokedStandalone = true;
  }
  private boolean invokedStandalone = false;

  }

