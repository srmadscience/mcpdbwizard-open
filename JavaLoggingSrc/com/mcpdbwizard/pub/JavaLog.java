package com.mcpdbwizard.pub;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.MissingResourceException;

/**
* An Implementation of LogInterface that uses GenericLog to create a logging
* mechanism that is compatible with java.util.logging.Logger
* 
* This class extends GenericLog - a partially abstract implementation of LogInterface.
* <p>
* <b>Note:</b> java.util.logging.Logger uses a configuration file (usually 
* JAVA_HOME/lib/logging.properties) to decide what messages should be printed. 
* If your <i>debug</i> messages are vanishing check this file and look for entries 
* that end in <i>'.level'</i>. Changing <i>'.level'</i> to <i>ALL</i> will result in 
* all messages being printed.
* <p></p>
* <b>Availability:</b> This class is shipped in a seperate JAR file called 'OBJavaLog.jar'.
* <p></p> 
* <b>Prerequisites:</b> You must be using Java 1.4 or higher.
* <p></p> 
*
* Copyright 2003-2026 ATB Consultancy Services Ltd
* (formerly Orinda Software Ltd, Dublin, Ireland)
*
* @see <a href=PARAM_LOGINTERFACE_URL>LogInterface</a>
* @version PARAM_VERSION
* @author  PARAM_AUTHOR
* @since PARAM_PRODUCT_NAME 4.0.1989/Java 1.4
*/
public class JavaLog extends GenericLog implements LogInterface {


	/**
	* An instance of java.util.logging.Logger
	*/
	Logger theLogger = null;
	
	/**
	*  Constructor that creates a java.util.logging.Logger based on supplied parameters.
	*
	*  If a new logger is created its log level will be configured based on the LogManager and it will configured to also send logging output to its parent loggers Handlers. It will be registered in the LogManager global namespace.
	*
	*  If the named Logger already exists and does not yet have a localization resource bundle then the given resource bundle name is used. If the named Logger already exists and has a different resource bundle name then an IllegalArgumentException is thrown.
	*
	*  @param name - A name for the logger. This should be a dot-separated name and should normally be based on the package name or class name of the subsystem, such as java.net or javax.swing
	*  @param resourceBundleName - name of ResourceBundle to be used for localizing messages for this logger. 
	*  @throws MissingResourceException - if the named ResourceBundle cannot be found. 
	*  @throws IllegalArgumentException - if the Logger already exists and uses a different resource bundle name.
	* 
	*/	
	public JavaLog(String name,
                   String resourceBundleName) throws MissingResourceException, IllegalArgumentException
	  {
      super();
	  theLogger = Logger.getLogger(name,resourceBundleName);	
	  }
	
	/**
	*  Constructor that creates a java.util.logging.Logger based on a supplied name.
	*
	*  If a new logger is created its log level will be configured based on the LogManager and it will configured to also send logging output to its parent loggers Handlers. It will be registered in the LogManager global namespace.
	*
	*  @param name - A name for the logger. This should be a dot-separated name and should normally be based on the package name or class name of the subsystem, such as java.net or javax.swing
	* 
	*/	
	public JavaLog(String name)
	  {
	  super();
	  theLogger = Logger.getLogger(name);	
	  }
	
	/**
	*  Constructor that uses a supplied java.util.logging.Logger.
    *
	*  @param Logger theLogger - An existing logger. 
	* 
	*/	
	public JavaLog(Logger theLogger)
	  {
	  super();
	  this.theLogger = theLogger;	
	  }
	
	/**
	* Return a copy of the current Logger object
	* @return Logger The current Logger object
	*/
	public Logger getLogger()
	  {
	  return(theLogger);
	  }
	
	/**  This method is not meaningful to java.util.logging.Logger but 
	 * is implemented as a null-op because it's part of LogInterface
	 * @see com.mcpdbwizard.pub.LogInterface#flush()
	 */
	public void flush() {
		// This method is not meaningful to java.util.logging.Logger

	}

	/**  
	* Returns the name of the current logging object.
	* @see com.mcpdbwizard.pub.LogInterface#getCurrentLog()
	*/
	public String getCurrentLog() {
		return(theLogger.getName());
	}

	/**
	* This method is declared abstract in GenericLog, which this class extends. 
	* GenericLog handles the creation of messages and timestamps but leaves the actual writing 
	* of messages to classes that extend it. This class writes the messages using an instance of 
	* java.util.logging.Logger. This method is not supposed to be called directly by clients,
	* which should use the debug(), info(), warn(), error() and syserror() methods that are implemented
	* by GenericLog.
	* 
    * @param String messageType one of the message types defined in com.mcpdbwizard.pub.LogInterface
    * @param String messageText Message text
    * @param boolean isModal Whether this message should appear in a modal window or not. Ignored by this implementation.
    * @param boolean isLogged Whether this message should be logged or not. Sometimes used in conjunction with 'isModal' for user interface messages of no importance.
	* 
	* @see com.mcpdbwizard.pub.GenericLog#writeMessage(java.lang.String, java.lang.String, boolean, boolean)
	*/
	protected void writeMessage(String messageType
			                  , String messageText
							  , boolean isModal
							  , boolean isLogged) 
	  {
	  // We need a variable to represent the message level used by
	  // java.util.logging.Logger.
	  Level javaLoggingMessageLevel = Level.ALL;

	  // Translate com.mcpdbwizard.pub.LogInterface messageType into java.util.Logging equiv...
	  if (messageType.equals(LogInterface.SYSERR))
		{
		javaLoggingMessageLevel = Level.SEVERE;
		}
	  else if (messageType.equals(LogInterface.ERROR))
		{
		javaLoggingMessageLevel = Level.SEVERE;
		}
	  else if (messageType.equals(LogInterface.WARN))
		{
		javaLoggingMessageLevel = Level.WARNING;
		}
	  else if (messageType.equals(LogInterface.INFO))
		{
		javaLoggingMessageLevel = Level.INFO;
		}
	  else if (messageType.equals(LogInterface.DEBUG))
		{
	  	// See the top level comment for this class if your
	  	// debug messages aren't appearing.
		javaLoggingMessageLevel = Level.FINE;
		}
		
	  // Log message
	  theLogger.log(javaLoggingMessageLevel,messageText);

	  }

}

