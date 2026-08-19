package com.mcpdbwizard.mcpdbwizardconnector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

import com.mcpdbwizard.app.procbuilder.gui.McpDbWizardEvent;
import com.mcpdbwizard.app.procbuilder.gui.McpDbWizardEventListener;
import com.mcpdbwizard.pub.ConsoleLog;
import com.mcpdbwizard.pub.LogInterface;
//import com.mcpdbwizard.test.TestInterface;

//import javax.jws.*;

/**
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class McpDbWizardConnectorGenerator {

	private static final String BOOLEAN = "boolean";
	private static final String DOT_BAT = ".bat";
	private static final String DOT_SH = ".sh";
	private boolean writePhysicalFiles = true;
	McpDbWizardEventListener listener = null;
	LogInterface theLog = null;

	public static final String NO_WS = "None";
	public static final String JSON_WS = "JSON";
	public static final String JSONRPC_WS = "JSON-RPC";
	private static final String JSONRPC = "JSONRPC";

	private static final String JSONRPC_REQUEST = "JsonRpc20Request";
	private static final String JSONRPC_RESPONSE = "JsonRpc20Response";
	private static final String JSONRPC_ERROR_RESPONSE = "JsonRpc20ErrorObject";
	private static final String JSONRPC_ERROR = "JsonRpc20Error";

	ArrayList<BaseMethodRepresentation> methList = null;

	public static String[] getWsOptions() {
		final String[] options = { NO_WS, JSON_WS, JSONRPC_WS };
		return options;
	}

	public McpDbWizardConnectorGenerator(McpDbWizardEventListener listener, LogInterface theLog) {
		this.listener = listener;
		this.theLog = theLog;
	}

	boolean writeFile(File newFile, String[] fileContents)

	{

		boolean returnCode = true;

		if (writePhysicalFiles)

		{

			try

			{
				// Create new File Stream
				FileOutputStream theFileStream = new FileOutputStream(newFile);

				// Create new Printwriter
				PrintWriter thePrintWriter = new PrintWriter(theFileStream);

				for (String fileContent : fileContents) {
					thePrintWriter.println(fileContent);
				}

				thePrintWriter.flush();
				theFileStream.flush();
				thePrintWriter.close();
				theFileStream.close();

			}

			catch (java.io.IOException e)

			{

				theLog.syserror(
						"Unexpected IO Error while writing file '" + newFile.getAbsolutePath() + "': " + e.getMessage(),
						true, true);

				returnCode = false;

			}

		}

		if (listener != null && newFile.getName().endsWith(".java"))

		{

			McpDbWizardEvent obEvent = new McpDbWizardEvent(McpDbWizardEvent.FILE_CREATED);

			obEvent.setThing(newFile);

			obEvent.setThing2(fileContents);

			listener.reportEvent(obEvent);

		}

		else if (listener != null && newFile.getName().endsWith(".sql"))

		{

			McpDbWizardEvent obEvent = new McpDbWizardEvent(McpDbWizardEvent.FILE_CREATED);

			obEvent.setThing(newFile);

			obEvent.setThing2(fileContents);

			listener.reportEvent(obEvent);

		}

		else if (listener != null && newFile.getName().endsWith(DOT_SH))

		{

			McpDbWizardEvent obEvent = new McpDbWizardEvent(McpDbWizardEvent.FILE_CREATED);

			obEvent.setThing(newFile);

			obEvent.setThing2(fileContents);

			listener.reportEvent(obEvent);

		}

		else if (listener != null && newFile.getName().toLowerCase().endsWith(DOT_BAT))

		{

			McpDbWizardEvent obEvent = new McpDbWizardEvent(McpDbWizardEvent.FILE_CREATED);

			obEvent.setThing(newFile);

			obEvent.setThing2(fileContents);

			listener.reportEvent(obEvent);

		}

		// /newFile.getName().endsWith(DOT_SH) ||
		// newFile.getName().toLowerCase().endsWith(DOT_BAT)
		//

		return (returnCode);

	}

//	public AbstractmethodInterface[] generateBaseMethods(LogInterface theLog,
//			@SuppressWarnings("rawtypes") Class impl,
//			@SuppressWarnings("rawtypes") Class iface, File theDir,
//			String newPackage, String newInterfaceFilename,
//			String newImplFileName) {
//
//		theLog.info("Processing Interface " + iface.getCanonicalName());
//
//		Method[] allMethods = iface.getDeclaredMethods();
//		BaseMethodRepresentation[] theMethods = new BaseMethodRepresentation[allMethods.length];
//
//		for (int m = 0; m < allMethods.length; m++) {
//			theMethods[m] = new BaseMethodRepresentation();
//			theMethods[m].setMethodName(allMethods[m].getName());
//			theMethods[m].setMethodReturn(allMethods[m].getReturnType()
//					.getCanonicalName());
//
//			theLog.info("Processing Method " + allMethods[m].getName() + "...");
//
//			Class<?>[] methodParamTypes = allMethods[m].getParameterTypes();
//			String[] methodParamNames = new String[methodParamTypes.length];
//
//			for (int i = 0; i < methodParamNames.length; i++) {
//				methodParamNames[i] = "param" + i;
//			}
//
//			Annotation[][] theAnnos = allMethods[m].getParameterAnnotations();
//
//			for (int i = 0; i < methodParamTypes.length && i < theAnnos.length; i++) {
//				Annotation[] annosForAParam = theAnnos[i];
//				for (int j = 0; j < annosForAParam.length; j++) {
//					WebParam p = null;
//
//					try {
//						p = (WebParam) annosForAParam[j];
//
//					} catch (Exception e) {
//
//						e.printStackTrace();
//					}
//					if (p != null) {
//						methodParamNames[i] = p.name();
//						break;
//					}
//
//				}
//
//			}
//
//			String[][] newParams = new String[methodParamNames.length][2];
//
//			for (int i = 0; i < methodParamNames.length; i++) {
//				theLog.info("Processing Parameter " + methodParamNames[i] + " "
//						+ methodParamTypes[i].getName() + "...");
//				newParams[i][0] = methodParamNames[i];
//				if (methodParamTypes[i].isArray()) {
//					Object foo = methodParamTypes[i].getCanonicalName();
//					Object bar = methodParamTypes[i].getComponentType();
//
//					newParams[i][1] = methodParamTypes[i].getCanonicalName();
//
//					// newParams[i][1] = methodParamTypes[i].getComponentType()
//					// + "[]";
//				} else {
//					newParams[i][1] = methodParamTypes[i].getName();
//				}
//				newParams[i][1] = newParams[i][1].replace("class ", "");
//
//			}
//
//			theMethods[m].setMethodParams(newParams);
//
//		}
//
//		return theMethods;
//	}

	//

//	public void generateJsonMethods(LogInterface theLog,
//			@SuppressWarnings("rawtypes") Class impl,
//			@SuppressWarnings("rawtypes") Class iface, File theDir,
//			String newPackage, String newInterfaceFilename,
//			String newImplFileName, AbstractmethodInterface[] theMethods,
//			boolean comments, boolean dubug, String wsOption) {
//
//		final String SPACES = "                                                                                     ";
//
//		final String I = "  ";
//
//		if (wsOption.equalsIgnoreCase(NO_WS)) {
//			return;
//		} else if (wsOption.equalsIgnoreCase(JSON_WS)
//				|| wsOption.equalsIgnoreCase(JSONRPC_WS)) {
//			theLog.info("Creating additional service classes to support '"
//					+ wsOption + "'");
//		} else {
//			theLog.error("Invalid WS option of '" + wsOption + " 'received");
//			return;
//		}
//
//		ArrayList<String> theInterface = new ArrayList<String>();
//		ArrayList<String> theImpl = new ArrayList<String>();
//
//		theInterface.add("package " + newPackage + ";");
//		theInterface.add("public interface " + newInterfaceFilename + "{");
//
//		theImpl.add("package " + newPackage + ";");
//		if (comments) {
//			theImpl.add("// We use Google's GSON, without which this code will not work.");
//			theImpl.add("// See http://code.google.com/p/google-gson/");
//		}
//		final String[] IMPORTS = { "java.util.ArrayList",
//				"com.google.gson.Gson", "com.google.gson.JsonElement",
//				"com.google.gson.JsonParser", "com.google.gson.JsonObject", newPackage + ".plsql.*" };
//		for (int i = 0; i < IMPORTS.length; i++) {
//			theImpl.add("import " + IMPORTS[i] + ";");
//		}
//
//		theImpl.add("");
//		if (comments) {
//			theImpl.add("/*****");
//			theImpl.add("* " + newImplFileName + " - A " + wsOption
//					+ " compatible interpretation of  "
//					+ impl.getCanonicalName());
//			theImpl.add("* Note that this class contains an inner class for all methods that take parameters.");
//			theImpl.add("* The inner class is needed as Google's GSON expects a target class to deserialize JSON messages into");
//			theImpl.add("*");
//			if (wsOption.equalsIgnoreCase(JSONRPC_WS)) {
//
//				theImpl.add("*");
//				theImpl.add("* This service complies with the JSON-RPC 2.0 specification");
//				theImpl.add("* @see http://www.jsonrpc.org/specification");
//				theImpl.add("*");
//			}
//
//			theImpl.add("*****/");
//
//			theInterface.add("/*****");
//			theInterface.add("* " + newInterfaceFilename + " - A " + wsOption
//					+ " compatible interpretation of  "
//					+ iface.getCanonicalName());
//			if (wsOption.equalsIgnoreCase(JSONRPC_WS)) {
//
//				theInterface.add("*");
//				theInterface
//						.add("* This service complies with the JSON-RPC 2.0 specification");
//				theInterface.add("* see http://www.jsonrpc.org/specification");
//				theInterface.add("*");
//			}
//
//			theInterface.add("*****/");
//		}
//
//		if (wsOption.equalsIgnoreCase(JSONRPC_WS)) {
//
//			/**
//			 * -32700 Parse error Invalid JSON was received by the server. An
//			 * error occurred on the server while parsing the JSON text. -32600
//			 * Invalid Request The JSON sent is not a valid Request object.
//			 * -32601 Method not found The method does not exist / is not
//			 * available. -32602 Invalid params Invalid method parameter(s).
//			 * -32603 Internal error Internal JSON-RPC error.
//			 */
//			if (comments)
//				theInterface
//						.add("// Parse error 	Invalid JSON was received by the server.");
//			if (comments)
//				theInterface
//						.add("// An error occurred on the server while parsing the JSON text.");
//
//			theInterface.add("public static final int PARSE_ERROR = -32700;");
//			theInterface.add("");
//
//			if (comments)
//				theInterface
//						.add("// Invalid Request - The JSON sent is not a valid Request object..");
//			theInterface
//					.add("public static final int INVALID_REQUEST = -32600;");
//			theInterface.add("");
//
//			if (comments)
//				theInterface
//						.add("// Method not found 	The method does not exist / is not available.");
//			theInterface
//					.add("public static final int METHOD_NOT_FOUND = -32601;");
//			theInterface.add("");
//
//			if (comments)
//				theInterface
//						.add("// Invalid params 	Invalid method parameter(s).");
//			theInterface.add("public static final int BAD_PARAMS = -32602;");
//			theInterface.add("");
//
//			if (comments)
//				theInterface.add("// Internal error 	Internal JSON-RPC error.");
//			theInterface
//					.add("public static final int INTERNAL_ERROR = -32603;");
//			theInterface.add("");
//
//			if (comments)
//				theInterface.add("// Required by protocol");
//			theInterface
//					.add("public static final String JSON_RPC_VERSION = \"2.0\";");
//			theInterface.add("");
//
//			if (comments)
//				theInterface.add("/*****");
//			if (comments)
//				theInterface.add("* RPC Method Declaration");
//			if (comments)
//				theInterface
//						.add("* @param inParam A Request that complies with JAX RPC 2.0");
//			if (comments)
//				theInterface
//						.add("* @return inParam The response to this request");
//			if (comments)
//				theInterface.add("*****/");
//			theInterface.add("public String " + JSONRPC + "(String inParam);");
//			theInterface.add("");
//
//		} else {
//			if (comments)
//				theInterface
//						.add("// Invalid params 	Invalid method parameter(s).");
//			theInterface.add("public static final int BAD_PARAMS = -32602;");
//			theInterface.add("");
//
//			if (comments)
//				theInterface.add("// Internal error 	Internal JSON-RPC error.");
//			theInterface
//					.add("public static final int INTERNAL_ERROR = -32603;");
//			theInterface.add("");
//
//			if (comments)
//				theInterface.add("// Required by protocol");
//			theInterface
//					.add("public static final String JSON_RPC_VERSION = \"2.0\";");
//			theInterface.add("");
//		}
//
//		theImpl.add("public class " + newImplFileName + " implements "
//				+ newInterfaceFilename + " {");
//		theImpl.add("");
//		theImpl.add("static final String MISSING_PARAMETER = \"Missing Json Parameter\";");
//		theImpl.add("");
//		if (comments) {
//			theImpl.add("// We need an instance of the generated service to handle requests.");
//
//		}
//		theImpl.add("private " + iface.getName() + " theService = new "
//				+ impl.getName() + "();");
//		theImpl.add("");
//		if (comments) {
//			theImpl.add("// Get LogInterface from service object.");
//
//		}
//		theImpl.add("private com.mcpdbwizard.pub.LogInterface theLog = (("
//				+ impl.getName() + ")theService).theLog;");
//		theImpl.add("");
//		if (comments) {
//			theImpl.add("// Note that you need to have downloaded google's GSON for this line to compile.");
//
//		}
//		theImpl.add("Gson gson = new Gson();");
//		theImpl.add("JsonParser theParser = new JsonParser();");
//		theImpl.add("");
//
//		String priOrPub = "public";
//		String exception = "";
//
//		if (wsOption.equalsIgnoreCase(JSONRPC_WS)) {
//			priOrPub = "private";
//			exception = "throws Exception";
//
//			if (comments)
//				theImpl.add("/*****");
//			if (comments)
//				theImpl.add("* RPC Method Declaration");
//			if (comments)
//				theImpl.add("* @param inParam A Request that complies with JAX RPC 2.0");
//			if (comments)
//				theImpl.add("* @return The response to this request");
//			if (comments)
//				theImpl.add("*****/");
//			theImpl.add("@SuppressWarnings(\"unchecked\")");
//			theImpl.add("public String " + JSONRPC + "(String inParam)");
//			theImpl.add(I + "{");
//			theImpl.add(I + "String theReturnString = \"\";");
//
//			theImpl.add(I + JSONRPC_REQUEST + "[] reqArray = null;");
//			if (comments)
//				theImpl.add(I
//						+ "// Not all requests result in responses, and responses might be errors, so we use an ArrayList");
//			theImpl.add(I + "@SuppressWarnings(\"rawtypes\")");
//			theImpl.add(I + "ArrayList ral = null;");
//
//			theImpl.add(I + "boolean isAnArray = false;");
//			theImpl.add(I + "");
//			theImpl.add(I + "// Handle nulls and empty strings");
//			theImpl.add(I + "if (inParam == null || inParam.length() == 0 || inParam.equals(\"null\") /* Seen in testing */) {");
//			theImpl.add(I +I + "");
//			theImpl.add(I + I +  JSONRPC_ERROR + " err = new "
//					+ JSONRPC_ERROR + "();");
//
//			theImpl.add(I + I  + "err.code = " + newInterfaceFilename
//					+ ".BAD_PARAMS;");
//			theImpl.add(I
//					+ I
//					+ "err.message = \"Request  appears to be null or zero length\";");
//			theImpl.add(I +I + "");
//			theImpl.add(I + I + JSONRPC_ERROR_RESPONSE + " er = new "
//					+ JSONRPC_ERROR_RESPONSE + "();");
//			theImpl.add(I +  I + "er.jsonrpc = " + newInterfaceFilename
//					+ ".JSON_RPC_VERSION;");
//			theImpl.add(I + I + "er.error = err;");
//			theImpl.add(I + I + "er.id = null;");
//			theImpl.add("");
//			theImpl.add(I
//					+ I
//					+ "// Turn our error response into JSON and send it back...");
//
//			theImpl.add(I
//					+ I
//
//					+ "theLog.error(err.message);");
//			theImpl.add(I  + I + "theReturnString = gson.toJson(er);");
//			theImpl.add(I  + I + "return (theReturnString);");
//			theImpl.add(I +I + "}");
//			theImpl.add(I + "");
//
//			if (comments) {
//				theImpl.add(I + "//");
//				theImpl.add(I
//						+ "// Note that since we can't control what is in 'inParam' the 'catch'");
//				theImpl.add(I
//						+ "// section of this try block will be frequently used.");
//				theImpl.add(I + "//");
//
//			}
//			theImpl.add(I + "try {");
//			theImpl.add(I + I + "// See if it's an array");
//
//			theImpl.add(I + I + "reqArray = gson.fromJson(inParam, "
//					+ JSONRPC_REQUEST + "[].class);");
//
//			theImpl.add("");
//			theImpl.add(I + I + "isAnArray = true;");
//			// theImpl.add("ArrayList responseArraList = null;");
//			theImpl.add(I + I + "if (theLog.getDebug()) {");
//			theImpl.add(I
//					+ I
//					+ I
//					+ "theLog.debug(\"Got Array of \" + reqArray.length + \" Requests:\");");
//
//			theImpl.add(I + I + I + "for (int i=0; i < reqArray.length; i++)");
//
//			theImpl.add(I + I + I + I + "{");
//			theImpl.add(I + I + I + I + "theLog.debug(reqArray[i].toString());");
//			theImpl.add(I + I + I + I + "}");
//
//			theImpl.add(I + I + I + "}");
//			theImpl.add(I + I + "} catch (Exception eNotAnArray) {");
//
//			theImpl.add(I + I + "try {");
//			theImpl.add(I + I + I + "// See if it's a single instance of a "
//					+ JSONRPC_REQUEST);
//			theImpl.add(I + I + I + JSONRPC_REQUEST + " singleRequest = new "
//					+ JSONRPC_REQUEST + "();");
//			theImpl.add(I + I + I + "singleRequest = gson.fromJson(inParam, "
//					+ JSONRPC_REQUEST + ".class);");
//			theImpl.add(I + I + I + "reqArray = new " + JSONRPC_REQUEST
//					+ "[1];");
//			theImpl.add(I + I + I + "reqArray[0] = singleRequest;");
//			theImpl.add(I + I + I + "");
//
//			theImpl.add(I + I + I + "if (theLog.getDebug()) {");
//			theImpl.add(I + I + I + I + I
//					+ "theLog.debug(\"Got Valid Request:\" + singleRequest);");
//			theImpl.add(I + I + I + I + "}");
//
//			theImpl.add(I + I + I + "} catch (Exception eNotARequest) {");
//
//			theImpl.add(I + I + I
//					+ "// Whatever this request is it isn't a valid "
//					+ JSONRPC_REQUEST);
//
//			theImpl.add(I + I + I + JSONRPC_ERROR + " err = new "
//					+ JSONRPC_ERROR + "();");
//
//			// theImpl.add("try {");
//			theImpl.add(I + I + I + "// See if it is in fact JSON at all...");
//
//			theImpl.add(I + I + I + "if (isItJson(inParam)) {");
//			// theImpl.add("@SuppressWarnings(\"unused\")");
//			theImpl.add(I + I + I + I + "err.code = " + newInterfaceFilename
//					+ ".INVALID_REQUEST;");
//			theImpl.add(I
//					+ I
//					+ I
//					+ I
//					+ "err.message = \"Input is a JSON object but not a request\";");
//			theImpl.add("");
//
//			theImpl.add(I + I + I + I + "} else {");
//			theImpl.add(I + I + I + I + "err.code = " + newInterfaceFilename
//					+ ".BAD_PARAMS;");
//			theImpl.add(I
//					+ I
//					+ I
//					+ I
//					+ "err.message = \"Request doesn't appear to be a valid JSON object\";");
//			theImpl.add(I + I + I + I + "}");
//			theImpl.add("");
//
//			theImpl.add(I + I + I
//					+ "theLog.warning(err.code + \":\" + err.message);");
//			theImpl.add(I + I + I + "theLog.warning(inParam);");
//
//			theImpl.add(I + I + I + "err.data = inParam;");
//			theImpl.add("");
//			theImpl.add(I + I + I + JSONRPC_ERROR_RESPONSE + " er = new "
//					+ JSONRPC_ERROR_RESPONSE + "();");
//			theImpl.add(I + I + I + "er.jsonrpc = " + newInterfaceFilename
//					+ ".JSON_RPC_VERSION;");
//			theImpl.add(I + I + I + "er.error = err;");
//			theImpl.add(I + I + I + "er.id = null;");
//			theImpl.add("");
//			theImpl.add(I + I + I + "try {");
//			theImpl.add(I
//					+ I
//					+ I
//					+ I
//					+ "// Turn our error response into JSON and send it back...");
//
//			theImpl.add(I
//					+ I
//					+ I
//					+ I
//					+ "theLog.warning(\"Request doesn't appear to be a valid JSON object:\");");
//			theImpl.add(I + I + I + I + "theLog.warning(inParam);");
//			theImpl.add(I + I + I + I + "theReturnString = gson.toJson(er);");
//			theImpl.add(I + I + I + I + "theLog.warning(theReturnString);");
//			theImpl.add(I + I + I + I + "return (theReturnString);");
//			theImpl.add("");
//
//			theImpl.add(I + I + I + I
//					+ "} catch (Exception unableToCreateJsonError) {");
//			theImpl.add(I
//					+ I
//					+ I
//					+ I
//					+ "theLog.error(\"Unable to encode JSON error message. Input request was\");");
//			theImpl.add(I + I + I + I + "theLog.error(inParam);");
//			theImpl.add(I + I + I + I
//					+ "theLog.error(unableToCreateJsonError);");
//
//			if (comments)
//				theImpl.add(I
//						+ I
//						+ I
//						+ I
//						+ "// We should never do this but we're out of alternatives");
//			theImpl.add(I
//					+ I
//					+ I
//					+ I
//					+ "return ( \"{\\\"jsonrpc\\\": \\\"2.0\\\", \\\"error\\\": {\\\"code\\\": \" + "
//					+ newInterfaceFilename
//					+ ".BAD_PARAMS + \", \\\"message\\\": \\\"Request doesn't appear to be a valid JSON object\\\"}, \\\"id\\\": null})\\\"\");");
//			theImpl.add(I + I + I + I + "}");
//			theImpl.add("");
//
//			theImpl.add(I + I + I + "}");
//			theImpl.add("");
//
//			theImpl.add(I + I + "}");
//			theImpl.add("");
//			// if (comments) theImpl.add("// Note ");
//			// theImpl.add(I+"@SuppressWarnings(\"rawtypes\")");
//
//			theImpl.add(I + "ral = new ArrayList(reqArray.length);");
//			if (comments)
//				theImpl.add(I
//						+ "// We could in theory do all of this without the rpc* methods but we found ");
//			if (comments)
//				theImpl.add(I
//						+ "// that one of regression tests broke because of the size of this method if  ");
//			if (comments)
//				theImpl.add(I
//						+ "// we don't parcel out the work to rpc methods  ");
//			theImpl.add(I + "for (int i=0; i < reqArray.length; i++) {");
//
//
//			theImpl.add(I + I + "");
//			theImpl.add(I + I + "if (reqArray[i].jsonrpc == null || (! reqArray[i].jsonrpc.equals(JSON_RPC_VERSION))) {");
//			theImpl.add(I + I + "");
//			theImpl.add(I + I + "ral.add(makeError(new Exception(\"invalid value for jsonrpc\"), reqArray[i].jsonrpc"
//					+ ", reqArray[i].id));");
//			theImpl.add(I + I + "theLog.error(\"Unable to call service due to bad value of jsonrpc:'\" + reqArray[i].jsonrpc + \"'. Input request was\");");
//			theImpl.add(I + I + "theLog.error(reqArray[i].toString());");
//			theImpl.add(I + I + "");
//			theImpl.add(I + I + "}");
//			theImpl.add(I + I + "else if (reqArray[i].method == null || reqArray[i].method.length() == 0) {");
//			theImpl.add(I + I + "");
//			theImpl.add(I + I + "ral.add(makeError(new Exception(\"null or zero length method name\"), reqArray[i].method"
//					+ ", reqArray[i].id));");
//			theImpl.add(I + I + "theLog.error(\"Unable to call service due to null or zero length method name. request id was \" + reqArray[i].id );");
//			theImpl.add(I + I + "");
//			theImpl.add(I + I + "}");
//
//
//
//			String elseOrSpace = "else ";
//			for (int i = 0; i < theMethods.length; i++) {
//				theImpl.add(I + I + elseOrSpace
//						+ "if (reqArray[i].method.equals(\""
//						+ theMethods[i].getMethodName() + "\")) {");
//				elseOrSpace = "else ";
//
//				// theImpl.add(I +I +"try {");
//				theImpl.add("");
//				theImpl.add(I + I + I + "rpc" + theMethods[i].getMethodName()
//						+ "(reqArray[i],ral);");
//				theImpl.add("");
//				// theImpl.add(I +I +I +JSONRPC_RESPONSE + " sr = new "
//				// + JSONRPC_RESPONSE + "();");
//				// theImpl.add(I +I +I +"sr.jsonrpc = " + newInterfaceFilename
//				// + ".JSON_RPC_VERSION;");
//				// theImpl.add(I +I +I +"sr.id = reqArray[i].id;");
//				// theImpl.add("");
//				// theImpl.add(I +I +I +"if (theLog.getDebug()) {");
//				// theImpl.add(I +I +I +I +"theLog.debug(\""
//				// + theMethods[i].getMethodName()
//				// +
//				// ": Starting request \" + sr.id + \":\" + reqArray[i].params);");
//				// theImpl.add(I +I +I +I +"}");
//				// theImpl.add("");
//				// if (theMethods[i].getMethodParams().length > 0) {
//				// if (theMethods[i].getMethodReturn().equals("void")) {
//				// theImpl.add(I +I +I +theMethods[i].getMethodName()
//				// + "(reqArray[i].params);");
//				// theImpl.add(I +I +I +"sr.result = \"\";");
//				// } else {
//				// theImpl.add(I +I +I +"sr.result = "
//				// + theMethods[i].getMethodName()
//				// + "(reqArray[i].params);");
//				//
//				// }
//				//
//				// } else {
//				// if (theMethods[i].getMethodReturn().equals("void")) {
//				// theImpl.add(I +I +I +theMethods[i].getMethodName() + "();");
//				// theImpl.add(I +I +I +"sr.result = \"\";");
//				// } else {
//				// theImpl.add(I +I +I +"sr.result = "
//				// + theMethods[i].getMethodName() + "();");
//				// }
//				//
//				// }
//				//
//				// // if (theMethods[i].getMethodReturn().equals("void")) {
//				// theImpl.add("");
//				// theImpl.add("");
//				// theImpl.add(I +I +I +"if (theLog.getDebug()) {");
//				// theImpl.add(I +I +I +I +"theLog.debug(\""
//				// + theMethods[i].getMethodName()
//				// +
//				// ": Processed request \" + sr.id + \": returning \" + sr.result);");
//				// theImpl.add(I +I +I +"}");
//				// theImpl.add("");
//				// theImpl.add(I+I+I+"if (reqArray[i].id != null && reqArray[i].id.length() > 0)");
//				// //theImpl.add(I+I+I+"@SuppressWarnings(\"rawtypes\")");
//				// theImpl.add(I+I+I+I+"{");
//				// if (comments)
//				// theImpl.add(I+I+I+I+"// Only send result if we have an id, otherwise this");
//				// if (comments)
//				// theImpl.add(I+I+I+I+"// is a 'Notification' and no response is expected");
//				// theImpl.add(I +I +I +I+"ral.add(sr);");
//				// theImpl.add(I+I+I+I+"}");
//				// theImpl.add("");
//				//
//				// theImpl.add(I +I +I
//				// +"} catch (Exception unableToCallService) {");
//				// theImpl.add("");
//				// // theImpl.add(I +I +I +I +JSONRPC_ERROR + " err = new " +
//				// JSONRPC_ERROR
//				// // + "();");
//				// // theImpl.add(I +I +I +I +"err.code = " +
//				// newInterfaceFilename
//				// // + ".INTERNAL_ERROR;");
//				// // theImpl.add(I +I +I +I
//				// +"err.message = unableToCallService.getMessage();");
//				// // theImpl.add(I +I +I +I +"err.data = reqArray[i].method;");
//				// // theImpl.add("");
//				// // theImpl.add(I +I +I +I +JSONRPC_ERROR_RESPONSE +
//				// " er = new "
//				// // + JSONRPC_ERROR_RESPONSE + "();");
//				// // theImpl.add(I +I +I +I +"er.jsonrpc = " +
//				// newInterfaceFilename
//				// // + ".JSON_RPC_VERSION;");
//				// // theImpl.add(I +I +I +I +"er.error = err;");
//				// // theImpl.add(I +I +I +I +"er.id = reqArray[i].id;");
//				// // theImpl.add("");
//				// // theImpl.add(I +I +I +I +"ral.add(er);");
//				// theImpl.add("");
//				// //theImpl.add(I +I +I +I+"@SuppressWarnings(\"rawtypes\")");
//				//
//				// theImpl.add(I +I +I +I
//				// +"ral.add(makeError(unableToCallService,\"" +
//				// theMethods[i].getMethodName() + "\", reqArray[i].id));");
//				// theImpl.add("");
//				// theImpl.add(I +I +I +I
//				// +"theLog.error(\"Unable to call service '"
//				// + theMethods[i].getMethodName()
//				// + "'. Input request was\");");
//				// theImpl.add(I +I +I +I +"theLog.error(reqArray[i].params);");
//				// theImpl.add(I +I +I +I
//				// +"theLog.error(unableToCallService);");
//				//
//				// theImpl.add(I +I +I +I +"}");
//				//
//				// theImpl.add(I +I +I +"//");
//				theImpl.add(I + I + I + "}");
//			}
//
//			theImpl.add(I + I + "else {");
//			if (comments)
//				theImpl.add(I + I + I
//						+ "// We don't recognize the method name...");
//			theImpl.add(I + I + I + JSONRPC_ERROR + " err = new "
//					+ JSONRPC_ERROR + "();");
//			theImpl.add(I + I + I + "err.code = " + newInterfaceFilename
//					+ ".METHOD_NOT_FOUND;");
//			theImpl.add(I
//					+ I
//					+ I
//					+ "err.message = \"Request '\" + reqArray[i].id + \"' has unrecognized method of '\" + reqArray[i].method + \"'\";");
//			theImpl.add(I + I + I + "err.data = inParam;");
//			theImpl.add("");
//			theImpl.add(I + I + I + JSONRPC_ERROR_RESPONSE + " er = new "
//					+ JSONRPC_ERROR_RESPONSE + "();");
//			theImpl.add(I + I + I + "er.jsonrpc = " + newInterfaceFilename
//					+ ".JSON_RPC_VERSION;");
//			theImpl.add(I + I + I + "er.error = err;");
//			theImpl.add(I + I + I + "er.id = reqArray[i].id;");
//			// theImpl.add(I +I +I +"@SuppressWarnings(\"rawtypes\")");
//			theImpl.add(I + I + I + "ral.add(er);");
//			theImpl.add("");
//			theImpl.add(I + I + I + "theLog.error(err.message);");
//			theImpl.add(I + I + I + "theLog.error(inParam);");
//			theImpl.add(I + I + I + "}");
//
//			theImpl.add(I + I + "}");
//
//			theImpl.add(I + "try {");
//			theImpl.add(I + I + "ral.trimToSize(); ");
//			theImpl.add(I + I + "Object[] responseObjectArray = ral.toArray();");
//			theImpl.add(I + I + "if (responseObjectArray.length == 0) {");
//			if (comments)
//				theImpl.add(I + I + I + "// no responses - return empty string");
//			theImpl.add(I + I + I + "theReturnString = \"\";");
//			theImpl.add(I + I + I + "}");
//			theImpl.add(I + I + I + "else if (isAnArray) {");
//			theImpl.add(I + I + I + I
//					+ "theReturnString = gson.toJson(responseObjectArray);");
//			theImpl.add(I + I + I + "}");
//			theImpl.add(I + I + "else {");
//			theImpl.add(I + I + I
//					+ "theReturnString = gson.toJson(responseObjectArray[0]);");
//			theImpl.add(I + I + I + "}");
//			theImpl.add(I + I + "} catch (Exception unableToJsonizeOutput) {");
//			theImpl.add("");
//			theImpl.add(I + I + I + JSONRPC_ERROR + " err = new "
//					+ JSONRPC_ERROR + "();");
//			theImpl.add("");
//			theImpl.add(I + I + I
//					+ "if (unableToJsonizeOutput.getMessage().startsWith("
//					+ newInterfaceFilename + ".BAD_PARAMS+\"\")) {");
//			theImpl.add(I + I + I + "err.code = " + newInterfaceFilename
//					+ ".BAD_PARAMS;");
//			theImpl.add(I + I + I + "} else {");
//			theImpl.add(I + I + I + "err.code = " + newInterfaceFilename
//					+ ".INTERNAL_ERROR;");
//			theImpl.add(I + I + I + "} ");
//			theImpl.add("");
//			theImpl.add(I + I
//					+ "err.message = unableToJsonizeOutput.getMessage();");
//			// theImpl.add("err.data = reqArray[i].method;");
//			theImpl.add("");
//			theImpl.add(I + I + JSONRPC_ERROR_RESPONSE + " er = new "
//					+ JSONRPC_ERROR_RESPONSE + "();");
//			theImpl.add(I + I + "er.jsonrpc = " + newInterfaceFilename
//					+ ".JSON_RPC_VERSION;");
//			theImpl.add(I + I + "er.error = err;");
//			theImpl.add("");
//			// theImpl.add("er.id = reqArray[i].id;");
//			// theImpl.add("ral.add(er);");
//			theImpl.add(I + I
//					+ "theLog.error(\"Unable to serialize JSON output:\");");
//			// theImpl.add("theLog.error(err);");
//			theImpl.add(I + I + "theLog.error(unableToJsonizeOutput);");
//			theImpl.add("");
//			theImpl.add(I + I + "theReturnString = gson.toJson(er);");
//			theImpl.add(I + I + "}");
//
//			// reqArray
//			theImpl.add(I + "return (theReturnString);");
//			theImpl.add(I + "}");
//
//			for (int i = 0; i < theMethods.length; i++) {
//				if (comments)
//					theImpl.add("/*****");
//				if (comments)
//					theImpl.add("* Internal method to call "
//							+ theMethods[i].getMethodName());
//				if (comments)
//					theImpl.add("* Only used by " + JSONRPC + ".");
//				if (comments)
//					theImpl.add("* @param JsonRpc20Request theRequest JAX RPC 2.0 request");
//				if (comments)
//					theImpl.add("* @param ArrayList ral Arraylist for results.");
//				if (comments)
//					theImpl.add("*****/");
//
//				theImpl.add("private void rpc" + theMethods[i].getMethodName()
//						+ "(JsonRpc20Request theRequest, ArrayList ral) {");
//				// theImpl.add(I +I +elseOrSpace
//				// + "if (reqArray[i].method.equals(\""
//				// + theMethods[i].getMethodName() + "\")) {");
//				// elseOrSpace = "else ";
//				//
//				theImpl.add(I + "try {");
//				theImpl.add("");
//				theImpl.add(I + I + JSONRPC_RESPONSE + " sr = new "
//						+ JSONRPC_RESPONSE + "();");
//				theImpl.add(I + I + "sr.jsonrpc = " + newInterfaceFilename
//						+ ".JSON_RPC_VERSION;");
//				theImpl.add(I + I + "sr.id = theRequest.id;");
//				theImpl.add("");
//				theImpl.add(I + I + "if (theLog.getDebug()) {");
//				theImpl.add(I
//						+ I
//						+ I
//						+ "theLog.debug(\""
//						+ theMethods[i].getMethodName()
//						+ ": Starting request \" + sr.id + \":\" + theRequest.params);");
//				theImpl.add(I + I + I + "}");
//				theImpl.add("");
//				if (theMethods[i].getMethodParams().length > 0) {
//					if (theMethods[i].getMethodReturn().equals("void")) {
//						theImpl.add(I + I + theMethods[i].getMethodName()
//								+ "(theRequest.params);");
//						theImpl.add(I + I + "sr.result = \"\";");
//					} else {
//						theImpl.add(I + I + "sr.result = "
//								+ theMethods[i].getMethodName()
//								+ "(theRequest.params);");
//
//					}
//
//				} else {
//					if (theMethods[i].getMethodReturn().equals("void")) {
//						theImpl.add(I + I + theMethods[i].getMethodName()
//								+ "();");
//						theImpl.add(I + I + "sr.result = \"\";");
//					} else {
//						theImpl.add(I + I + "sr.result = "
//								+ theMethods[i].getMethodName() + "();");
//					}
//
//				}
//
//				// if (theMethods[i].getMethodReturn().equals("void")) {
//				theImpl.add("");
//				theImpl.add("");
//				theImpl.add(I + I + "if (theLog.getDebug()) {");
//				theImpl.add(I
//						+ I
//						+ I
//						+ "theLog.debug(\""
//						+ theMethods[i].getMethodName()
//						+ ": Processed request \" + sr.id + \": returning \" + sr.result);");
//				theImpl.add(I + I + "}");
//				theImpl.add("");
//				theImpl.add(I
//						+ I
//						+ "if (theRequest.id != null && theRequest.id.length() > 0)");
//				// theImpl.add(I+I+I+"@SuppressWarnings(\"rawtypes\")");
//				theImpl.add(I + I + I + "{");
//				if (comments)
//					theImpl.add(I
//							+ I
//							+ I
//							+ "// Only send result if we have an id, otherwise this");
//				if (comments)
//					theImpl.add(I
//							+ I
//							+ I
//							+ "// is a 'Notification' and no response is expected");
//				theImpl.add(I + I + I + "ral.add(sr);");
//				theImpl.add(I + I + I + "}");
//				theImpl.add("");
//
//				theImpl.add(I + I + "} catch (Exception unableToCallService) {");
//				theImpl.add("");
//				// theImpl.add(I +I +I +I +JSONRPC_ERROR + " err = new " +
//				// JSONRPC_ERROR
//				// + "();");
//				// theImpl.add(I +I +I +I +"err.code = " + newInterfaceFilename
//				// + ".INTERNAL_ERROR;");
//				// theImpl.add(I +I +I +I
//				// +"err.message = unableToCallService.getMessage();");
//				// theImpl.add(I +I +I +I +"err.data = reqArray[i].method;");
//				// theImpl.add("");
//				// theImpl.add(I +I +I +I +JSONRPC_ERROR_RESPONSE + " er = new "
//				// + JSONRPC_ERROR_RESPONSE + "();");
//				// theImpl.add(I +I +I +I +"er.jsonrpc = " +
//				// newInterfaceFilename
//				// + ".JSON_RPC_VERSION;");
//				// theImpl.add(I +I +I +I +"er.error = err;");
//				// theImpl.add(I +I +I +I +"er.id = reqArray[i].id;");
//				// theImpl.add("");
//				// theImpl.add(I +I +I +I +"ral.add(er);");
//				theImpl.add("");
//				// theImpl.add(I +I +I +I+"@SuppressWarnings(\"rawtypes\")");
//
//				theImpl.add(I + I + "ral.add(makeError(unableToCallService,\""
//						+ theMethods[i].getMethodName()
//						+ "\", theRequest.id));");
//				theImpl.add("");
//				theImpl.add(I + I + "theLog.error(\"Unable to call service '"
//						+ theMethods[i].getMethodName()
//						+ "'. Input request was\");");
//				theImpl.add(I + I + "theLog.error(theRequest.params);");
//				theImpl.add(I + I + "theLog.error(unableToCallService);");
//
//				theImpl.add(I + I + "}");
//				//
//				// theImpl.add(I +I +I +"//");
//				theImpl.add(I + "}");
//			}
//
//			if (comments) {
//				theImpl.add("/*****");
//				theImpl.add("* JSON-RPC 2.0 Request Object");
//				theImpl.add("* @see http://www.jsonrpc.org/specification#request_object");
//				theImpl.add("*****/");
//
//			}
//			theImpl.add("class " + JSONRPC_REQUEST);
//			theImpl.add("{");
//			theImpl.add(I + "String jsonrpc;");
//			theImpl.add(I + "String method;");
//			theImpl.add(I + "String params;");
//			theImpl.add(I + "String id;");
//			theImpl.add(I + "public String toString() {");
//			theImpl.add(I
//					+ I
//					+ "return jsonrpc + \":\"  + id + \":\" + method + \":\" + params;");
//			theImpl.add(I + "}");
//			theImpl.add("}");
//
//			if (comments) {
//				theImpl.add("/*****");
//				theImpl.add("* JSON-RPC 2.0 Response Object");
//				theImpl.add("* @see http://www.jsonrpc.org/specification#response_object");
//				theImpl.add("*****/");
//
//			}
//			theImpl.add("class " + JSONRPC_RESPONSE);
//			theImpl.add("{");
//			theImpl.add(I + "String jsonrpc;");
//			theImpl.add(I + "String result;");
//			theImpl.add(I + "String id;");
//			theImpl.add(I + "public String toString() {");
//			theImpl.add(I + I
//					+ "return jsonrpc + \":\"  + id + \":\" + result;");
//			theImpl.add(I + "}");
//			theImpl.add("}");
//		}
//		if (comments) {
//			theImpl.add("/*****");
//			theImpl.add("* JSON-RPC 2.0 Error Object");
//			theImpl.add("* @see http://www.jsonrpc.org/specification#error_object");
//			theImpl.add("*****/");
//
//		}
//		theImpl.add("class " + JSONRPC_ERROR);
//		theImpl.add("{");
//		theImpl.add(I + "int code;");
//		theImpl.add(I + "String message;");
//		theImpl.add(I + "String data;");
//		theImpl.add(I + "public String toString() {");
//		theImpl.add(I + I + "return code + \":\"  + message + \":\" + data;");
//		theImpl.add(I + "}");
//		theImpl.add("}");
//		// if (wsOption.equalsIgnoreCase(JSONRPC_WS)) {
//		if (comments) {
//			theImpl.add("/*****");
//			theImpl.add("* JSON-RPC 2.0 Error Response Object");
//			theImpl.add("* @see http://www.jsonrpc.org/specification#response_object");
//			theImpl.add("*****/");
//
//		}
//		theImpl.add("class " + JSONRPC_ERROR_RESPONSE);
//		theImpl.add("{");
//		theImpl.add(I + "String jsonrpc;");
//		theImpl.add(I + JSONRPC_ERROR + " error;");
//		theImpl.add(I + "String id;");
//		theImpl.add(I + "public String toString() {");
//		theImpl.add(I + I + "return jsonrpc + \":\"  + error + \":\" + id;");
//		theImpl.add(I + "}");
//		theImpl.add("}");
//
//		// }
//
//		for (int i = 0; i < theMethods.length; i++) {
//
//			// if (theMethods[i].getMethodParams().length > 0) {
//			// if (comments) {
//			// theImpl.add("/*****");
//			// theImpl.add("* Inner class used to hold results of GSON de-serialization for ");
//			// theImpl.add("* " + theMethods[i].getMethodName());
//			// theImpl.add("*****/");
//			//
//			// }
//			// theImpl.add("class GsonTarget" + theMethods[i].getMethodName()
//			// + " {");
//			//
//			// for (int j = 0; j < theMethods[i].getMethodParams().length; j++)
//			// {
//			// theImpl.add(I+theMethods[i].getMethodParams()[j][1]
//			// + " "
//			// + theMethods[i].getMethodParams()[j][0]
//			// + getNullAssign(theMethods[i].getMethodParams()[j][1])
//			// + ";");
//			//
//			// }
//			// theImpl.add("}");
//			// }
//
//			String header = priOrPub + " String "
//					+ theMethods[i].getMethodName();
//
//			if (theMethods[i].getMethodReturn().equals("void")) {
//				header = priOrPub + " void " + theMethods[i].getMethodName();
//			}
//			if (comments) {
//				theImpl.add("/*****");
//
//				theImpl.add("* " + theMethods[i].getMethodName());
//
//				theImpl.add("* @param inParam A JSON-encoded list of parameters");
//
//				if (!theMethods[i].getMethodReturn().equals("void"))
//					theImpl.add("* @return The response to this request, encoded in JSON. Note it could be an error message.");
//
//				theImpl.add("*****/");
//			}
//
//			// theImpl.add(header + "(");
//			if (theMethods[i].getMethodParams().length > 0) {
//				if (wsOption.equalsIgnoreCase(JSON_WS))
//					theInterface.add(header + " (String inParam);");
//				theImpl.add(header + " (String inParam) " + exception + " {");
//			} else {
//				if (wsOption.equalsIgnoreCase(JSON_WS))
//					theInterface.add(header + " ();");
//				theImpl.add(header + " () " + exception + " {");
//
//			}
//
//			if (!theMethods[i].getMethodReturn().equals("void")) {
//				theImpl.add(I + theMethods[i].getMethodReturn() + " theReturn "
//						+ getNullAssign(theMethods[i].getMethodReturn()) + ";");
//				theImpl.add(I + "String theReturnString = \"\";");
//			}
//
//			if (theMethods[i].getMethodParams().length > 0) {
//				theImpl.add(I + "GsonTarget" + theMethods[i].getMethodName()
//						+ " gTarget ");
//
//				theImpl.add(I + " = new " + "GsonTarget"
//						+ theMethods[i].getMethodName() + "();");
//			}
//			theImpl.add("");
//			if (theMethods[i].getMethodParams().length > 0) {
//				if (comments) {
//					theImpl.add(I + "//");
//					theImpl.add(I
//							+ "// Note that since we can't control what is in 'inParam' the 'catch'");
//					theImpl.add(I
//							+ "// section of this try block will be frequently used.");
//					theImpl.add(I + "//");
//
//				}
//				theImpl.add(I + "try {");
//				theImpl.add(I + I + "// Set parameters contained in inParam");
//				theImpl.add(I + I + "JsonElement j = theParser.parse(inParam);");
//
//				for (int j = 0; j < theMethods[i].getMethodParams().length; j++) {
//
//					theImpl.add(I + I + "checkHasParam(\""
//							+ theMethods[i].getMethodParams()[j][0] + "\",j);");
//
//				}
//
//				theImpl.add(I + I
//						+ "gTarget = gson.fromJson(inParam,GsonTarget"
//						+ theMethods[i].getMethodName() + ".class);");
//
//				theImpl.add(I + "} catch (Exception e) {");
//				theImpl.add(I + I + "String message = null;");
//				theImpl.add(I + I
//						+ "if (e.getMessage().startsWith(MISSING_PARAMETER)) {");
//				theImpl.add(I + I + "message = " + newInterfaceFilename
//						+ ".BAD_PARAMS + \":\" + \""
//						+ theMethods[i].getMethodName()
//						+ ": Parse Json:\" + e.getMessage();");
//				theImpl.add("");
//				theImpl.add(I + I + "} else if (isItJson(inParam)) {");
//
//				theImpl.add(I + I + "message = " + newInterfaceFilename
//						+ ".BAD_PARAMS + \":\" + \""
//						+ theMethods[i].getMethodName()
//						+ ": Parse Json:\" + e.getMessage();");
//				theImpl.add("");
//				theImpl.add(I + I + "} else {");
//				theImpl.add(I + I + "message = " + newInterfaceFilename
//						+ ".INTERNAL_ERROR + \":\" + \""
//						+ theMethods[i].getMethodName()
//						+ ": Parse Json:\" + e.getMessage();");
//				theImpl.add("");
//				theImpl.add(I + I + "}");
//				theImpl.add(I + "theLog.warning(message);");
//				if (wsOption.equalsIgnoreCase(JSON_WS)) {
//					if (theMethods[i].getMethodReturn().equals("void")) {
//						if (comments)
//							theImpl.add(I
//									+ "// Nothing we can do here - calling program doesn't expect a response.  ");
//						theImpl.add(I + "return;");
//
//					} else {
//						if (comments)
//							theImpl.add(I
//									+ "// Not much else we can do, as JSON API doesn't cover errors. Send a  ");
//						if (comments)
//							theImpl.add(I
//									+ "// JSON-RPC error message so at least they get JSON back  ");
//						theImpl.add(I + "return(message);");
//
//					}
//				} else {
//					theImpl.add(I + "throw new Exception(message);");
//
//				}
//				theImpl.add(I + "}");
//				theImpl.add("");
//			}
//
//			theImpl.add("");
//			theImpl.add(I + "try {");
//
//			theImpl.add(I + I + "// Call Service");
//			String assign = "";
//			if (theMethods[i].getMethodReturn().equals("void")) {
//				assign = "theService." + theMethods[i].getMethodName() + "(";
//			} else {
//				assign = "theReturn = theService."
//						+ theMethods[i].getMethodName() + "(";
//
//			}
//			String spaceOrBracket = "";
//
//			for (int j = 0; j < theMethods[i].getMethodParams().length; j++) {
//				if (j == theMethods[i].getMethodParams().length - 1) {
//					spaceOrBracket = ");";
//				}
//
//				theImpl.add(I + I + assign + "gTarget."
//						+ theMethods[i].getMethodParams()[j][0]
//						+ spaceOrBracket);
//				assign = SPACES.substring(0, assign.length() - 1) + ",";
//			}
//			if (theMethods[i].getMethodParams().length == 0) {
//				theImpl.add(I + I + assign + ");");
//			}
//
//			// theImpl.add(assign);
//			theImpl.add("");
//			theImpl.add(I + I + "} catch (Exception e) {");
//			theImpl.add(I + I + "String message = " + newInterfaceFilename
//					+ ".INTERNAL_ERROR + \":\" + \""
//					+ theMethods[i].getMethodName()
//					+ ": Call Service:\" + e.getMessage();");
//			theImpl.add(I + I + "theLog.error(message);");
//			if (wsOption.equalsIgnoreCase(JSON_WS)) {
//				if (theMethods[i].getMethodReturn().equals("void")) {
//					theImpl.add(I + I + "return;");
//				} else {
//					theImpl.add(I + I + "return(message);");
//				}
//			} else {
//				theImpl.add(I + I + "throw new Exception(message);");
//
//			}
//			theImpl.add(I + "}");
//			theImpl.add("");
//			if (!theMethods[i].getMethodReturn().equals("void")) {
//				theImpl.add(I + "try {");
//				theImpl.add(I + I + "// Create return JSON object");
//				theImpl.add(I + I + "theReturnString = gson.toJson(theReturn);");
//				theImpl.add(I + I + "} catch (Exception e) {");
//				theImpl.add(I + I + "String message = " + newInterfaceFilename
//						+ ".INTERNAL_ERROR + \":\" + \""
//						+ theMethods[i].getMethodName()
//						+ ": JSONize result:\" + e.getMessage();");
//				theImpl.add(I + I + "theLog.error(message);");
//				if (wsOption.equalsIgnoreCase(JSON_WS)) {
//					theImpl.add(I + I + "return(message);");
//				} else {
//					theImpl.add(I + I + "throw new Exception(message);");
//
//				}
//				theImpl.add(I + I + "}");
//
//				theImpl.add(I + "return (theReturnString);");
//
//			}
//			// theImpl.add(theMethods[i].getMethodName());
//
//			header = header + "(";
//
//			// String spaceOrBracket = "";
//			// String closingBracket = "";
//			//
//			// for (int j=0; j < theMethods[i].getMethodParams().length; j++) {
//			// if (j == theMethods[i].getMethodParams().length -1) {
//			// spaceOrBracket = ") {";
//			// }
//			// theImpl.add(header +
//			// theMethods[i].getMethodParams()[j][1] + " " +
//			// theMethods[i].getMethodParams()[j][0] + spaceOrBracket);
//			// header = SPACES.substring(0,header.length()-1)+",";
//			//
//			// }
//
//			//
//			theImpl.add(I + "}");
//
//		}
//
//		// checkHasParam
//		if (comments) {
//			theImpl.add("/*****");
//			theImpl.add("* See if something is actually JSON or not");
//			theImpl.add("* @param String possibleJson A String which may or may not be JSON");
//			theImpl.add("* @param JsonElement theJson A partially parsed JSON string");
//			// theImpl.add("* @return true if it's JSON, false otherwise");
//			theImpl.add("* @throws Exception if not parsable as JSON");
//			theImpl.add("*****/");
//
//		}
//		theImpl.add("private void checkHasParam(String paramName, JsonElement theJsonElement) throws Exception {");
//		// theImpl.add(I + "try {");
//		theImpl.add(I
//				+ "JsonObject jObject = (JsonObject)theJsonElement.getAsJsonObject();");
//
//		theImpl.add(I + "if (jObject.get(paramName) == null) {");
//		theImpl.add(I + I + "// Parameter is not in in JSON object...");
//
//		theImpl.add(I + I
//				+ "throw new Exception(MISSING_PARAMETER+\":\" + paramName);");
//		theImpl.add(I + I + "}");
//
//		// theImpl.add(I + I + "} catch (Exception notJsonException) {");
//		// theImpl.add(I + I + "return (false);");
//		// theImpl.add(I + I + "}");
//		theImpl.add(I + "}");
//		theImpl.add("");
//
//		if (comments) {
//			theImpl.add("/*****");
//			theImpl.add("* See if something is actually JSON or not");
//			theImpl.add("* @param String possibleJson A String which may or may not be JSON");
//			theImpl.add("* @return true if it's JSON, false otherwise");
//			theImpl.add("*****/");
//
//		}
//		theImpl.add("private boolean isItJson(String possibleJson) {");
//		theImpl.add(I + "try {");
//		theImpl.add(I + I + "// See if it is in fact JSON at all...");
//
//		theImpl.add(I + I + "@SuppressWarnings(\"unused\")");
//		theImpl.add(I + I
//				+ "JsonElement thejson = theParser.parse(possibleJson); ");
//		theImpl.add(I + I + "// Must be JSON to get to this line...");
//
//		theImpl.add(I + I + "return (true);");
//		theImpl.add(I + I + "");
//
//		theImpl.add(I + I + "} catch (Exception notJsonException) {");
//		theImpl.add(I + I + "return (false);");
//		theImpl.add(I + I + "}");
//		theImpl.add(I + "}");
//		theImpl.add("");
//
//		if (wsOption.equalsIgnoreCase(JSONRPC_WS)) {
//			if (comments) {
//				theImpl.add("/*****");
//				theImpl.add("* Compose error response");
//				theImpl.add("* @param Exception ex the exception we got");
//				theImpl.add("* @param String meth The method we are on");
//				theImpl.add("* @param String id The client's transaction id");
//				theImpl.add("* @return JSONRPC_ERROR_RESPONSE an JSON RPC 2.0 Error response");
//				theImpl.add("*****/");
//
//			}
//			theImpl.add("private " + JSONRPC_ERROR_RESPONSE
//					+ " makeError(Exception ex");
//			theImpl.add("                             ,String meth");
//			theImpl.add("                             ,String id) {");
//			theImpl.add("");
//			theImpl.add(I + JSONRPC_ERROR + " er = new " + JSONRPC_ERROR
//					+ "();");
//			theImpl.add(I + "er.code = " + newInterfaceFilename
//					+ ".INTERNAL_ERROR;");
//			theImpl.add(I + "er.message = ex.getMessage();");
//			theImpl.add(I + "er.data = meth;");
//			theImpl.add("");
//			theImpl.add(I + JSONRPC_ERROR_RESPONSE + " res = new "
//					+ JSONRPC_ERROR_RESPONSE + "();");
//			theImpl.add(I + "res.jsonrpc = " + newInterfaceFilename
//					+ ".JSON_RPC_VERSION;");
//			theImpl.add(I + "res.error = er;");
//			theImpl.add(I + "res.id = id;");
//
//			theImpl.add(I + "");
//			theImpl.add(I + "return(res);");
//			theImpl.add(I + "}");
//		}
//		for (int i = 0; i < theMethods.length; i++) {
//
//			if (theMethods[i].getMethodParams().length > 0) {
//				if (comments) {
//					theImpl.add("/*****");
//					theImpl.add("* Inner class used to hold results of GSON de-serialization for ");
//					theImpl.add("* " + theMethods[i].getMethodName());
//					theImpl.add("*****/");
//
//				}
//				theImpl.add("class GsonTarget" + theMethods[i].getMethodName()
//						+ " {");
//
//				for (int j = 0; j < theMethods[i].getMethodParams().length; j++) {
//					theImpl.add(I
//							+ theMethods[i].getMethodParams()[j][1]
//							+ " "
//							+ theMethods[i].getMethodParams()[j][0]
//							+ getNullAssign(theMethods[i].getMethodParams()[j][1])
//							+ ";");
//
//				}
//				theImpl.add("}");
//			}
//		}
//
//		theInterface.add("}");
//		theImpl.add("}");
//
//		String[] theInterfaceArray = { "" };
//		theInterfaceArray = theInterface.toArray(theInterfaceArray);
//
////		for (int i = 0; i < theInterfaceArray.length; i++) {
////			System.out.println(theInterfaceArray[i]);
////		}
//
//		String[] theImplArray = { "" };
//		theImplArray = theImpl.toArray(theImplArray);
//
////		for (int i = 0; i < theImplArray.length; i++) {
////			System.out.println(theImplArray[i]);
////		}
//
//		File tempFile = new File(theDir.getAbsolutePath() + File.separator
//				+ newPackage.replace(".", File.separator) + File.separator
//				+ "Temp" + ".java");
//
//		File ifaceFile = new File(theDir.getAbsolutePath() + File.separator
//				+ newPackage.replace(".", File.separator) + File.separator
//				+ newInterfaceFilename + ".java");
//
//		File implFile = new File(theDir.getAbsolutePath() + File.separator
//				+ newPackage.replace(".", File.separator) + File.separator
//				+ newImplFileName + ".java");
//
//		ifaceFile.getParentFile().mkdirs();
//
//		writeFile(ifaceFile, theInterfaceArray);
//
//		writeFile(implFile, theImplArray);
//
//		// Jalopy j = new Jalopy();
//		//
//		// writeFile(tempFile, theInterfaceArray);
//		// try {
//		// j.setInput(tempFile);
//		// } catch (FileNotFoundException e) {
//		// // TODO Auto-generated catch block
//		// e.printStackTrace();
//		// }
//		// j.setOutput(ifaceFile);
//		// if (!j.format()) {
//		// try {
//		// com.mcpdbwizard.pub.IOUtils.copyFile(tempFile, ifaceFile);
//		// } catch (CSException e) {
//		// // TODO Auto-generated catch block
//		// theLog.error(e);
//		// }
//		// }
//		//
//		// tempFile.delete();
//		//
//		// writeFile(tempFile, theImplArray);
//		// try {
//		// j.setInput(tempFile);
//		// } catch (FileNotFoundException e) {
//		//
//		// e.printStackTrace();
//		// }
//		// j.setOutput(implFile);
//		// if (true /* !j.format() */) {
//		// try {
//		// com.mcpdbwizard.pub.IOUtils.copyFile(tempFile, implFile);
//		// } catch (CSException e) {
//		//
//		// theLog.error(e);
//		// }
//		// }
//		//
//		// tempFile.delete();
//
//	}

	public void generateJsonMethods(LogInterface theLog, String existingImpl, String existingIface, File theDir,
			String newPackage, String newInterfaceFilename, String newImplFileName,
			ArrayList<BaseMethodRepresentation> methList, boolean comments, boolean dubug, String wsOption) {
//
		methList.trimToSize();
		AbstractmethodInterface[] theMethods = new AbstractmethodInterface[methList.size()];
		theMethods = methList.toArray(theMethods);

		final String SPACES = "                                                                                     ";

		final String I = "  ";

		if (wsOption.equalsIgnoreCase(NO_WS)) {
			return;
		} else if (wsOption.equalsIgnoreCase(JSON_WS) || wsOption.equalsIgnoreCase(JSONRPC_WS)) {
			theLog.info("Creating additional service classes to support '" + wsOption + "'");
		} else {
			theLog.error("Invalid WS option of '" + wsOption + " 'received");
			return;
		}

		ArrayList<String> theInterface = new ArrayList<>();
		ArrayList<String> theImpl = new ArrayList<>();

		theInterface.add("package " + newPackage + ";");
		theInterface.add("public interface " + newInterfaceFilename + "{");

		theImpl.add("package " + newPackage + ";");
		if (comments) {
			theImpl.add("// We use Google's GSON, without which this code will not work.");
			theImpl.add("// See http://code.google.com/p/google-gson/");
		}
		final String[] IMPORTS = { "java.util.ArrayList", "com.google.gson.Gson", "com.google.gson.JsonElement",
				"com.google.gson.JsonParser", "com.google.gson.JsonObject", newPackage + ".plsql.*" };
		for (String element : IMPORTS) {
			theImpl.add("import " + element + ";");
		}

		theImpl.add("");
		if (comments) {
			theImpl.add("/*****");
			theImpl.add("* " + newImplFileName + " - A " + wsOption + " compatible interpretation of  " + existingImpl);
			theImpl.add("* Note that this class contains an inner class for all methods that take parameters.");
			theImpl.add(
					"* The inner class is needed as Google's GSON expects a target class to deserialize JSON messages into");
			theImpl.add("*");
			if (wsOption.equalsIgnoreCase(JSONRPC_WS)) {

				theImpl.add("*");
				theImpl.add("* This service complies with the JSON-RPC 2.0 specification");
				theImpl.add("* @see http://www.jsonrpc.org/specification");
				theImpl.add("*");
			}

			theImpl.add("*****/");

			theInterface.add("/*****");
			theInterface.add("* " + newInterfaceFilename + " - A " + wsOption + " compatible interpretation of  "
					+ existingIface);
			if (wsOption.equalsIgnoreCase(JSONRPC_WS)) {

				theInterface.add("*");
				theInterface.add("* This service complies with the JSON-RPC 2.0 specification");
				theInterface.add("* see http://www.jsonrpc.org/specification");
				theInterface.add("*");
			}

			theInterface.add("*****/");
		}

		if (wsOption.equalsIgnoreCase(JSONRPC_WS)) {

			/**
			 * -32700 Parse error Invalid JSON was received by the server. An error occurred
			 * on the server while parsing the JSON text. -32600 Invalid Request The JSON
			 * sent is not a valid Request object. -32601 Method not found The method does
			 * not exist / is not available. -32602 Invalid params Invalid method
			 * parameter(s). -32603 Internal error Internal JSON-RPC error.
			 */
			if (comments) {
				theInterface.add("// Parse error 	Invalid JSON was received by the server.");
			}
			if (comments) {
				theInterface.add("// An error occurred on the server while parsing the JSON text.");
			}

			theInterface.add("public static final int PARSE_ERROR = -32700;");
			theInterface.add("");

			if (comments) {
				theInterface.add("// Invalid Request - The JSON sent is not a valid Request object..");
			}
			theInterface.add("public static final int INVALID_REQUEST = -32600;");
			theInterface.add("");

			if (comments) {
				theInterface.add("// Method not found 	The method does not exist / is not available.");
			}
			theInterface.add("public static final int METHOD_NOT_FOUND = -32601;");
			theInterface.add("");

			if (comments) {
				theInterface.add("// Invalid params 	Invalid method parameter(s).");
			}
			theInterface.add("public static final int BAD_PARAMS = -32602;");
			theInterface.add("");

			if (comments) {
				theInterface.add("// Internal error 	Internal JSON-RPC error.");
			}
			theInterface.add("public static final int INTERNAL_ERROR = -32603;");
			theInterface.add("");

			if (comments) {
				theInterface.add("// Required by protocol");
			}
			theInterface.add("public static final String JSON_RPC_VERSION = \"2.0\";");
			theInterface.add("");

			if (comments) {
				theInterface.add("/*****");
			}
			if (comments) {
				theInterface.add("* RPC Method Declaration");
			}
			if (comments) {
				theInterface.add("* @param inParam A Request that complies with JAX RPC 2.0");
			}
			if (comments) {
				theInterface.add("* @return inParam The response to this request");
			}
			if (comments) {
				theInterface.add("*****/");
			}
			theInterface.add("public String " + JSONRPC + "(String inParam);");
			theInterface.add("");

		} else {
			if (comments) {
				theInterface.add("// Invalid params 	Invalid method parameter(s).");
			}
			theInterface.add("public static final int BAD_PARAMS = -32602;");
			theInterface.add("");

			if (comments) {
				theInterface.add("// Internal error 	Internal JSON-RPC error.");
			}
			theInterface.add("public static final int INTERNAL_ERROR = -32603;");
			theInterface.add("");

			if (comments) {
				theInterface.add("// Required by protocol");
			}
			theInterface.add("public static final String JSON_RPC_VERSION = \"2.0\";");
			theInterface.add("");
		}

		theImpl.add("public class " + newImplFileName + " implements " + newInterfaceFilename + " {");
		theImpl.add("");
		theImpl.add("static final String MISSING_PARAMETER = \"Missing Json Parameter\";");
		theImpl.add("");
		if (comments) {
			theImpl.add("// We need an instance of the generated service to handle requests.");
			theImpl.add("// It's public so apache pools can work with it.");
		}
		theImpl.add("public " + existingIface + " theService = new " + existingImpl + "();");
		theImpl.add("");
		if (comments) {
			theImpl.add("// Get LogInterface from service object.");

		}
		theImpl.add("private com.mcpdbwizard.pub.LogInterface theLog = ((" + existingImpl + ")theService).theLog;");
		theImpl.add("");
		if (comments) {
			theImpl.add("// Note that you need to have downloaded google's GSON for this line to compile.");

		}
		theImpl.add("Gson gson = new Gson();");
		theImpl.add("JsonParser theParser = new JsonParser();");
		theImpl.add("");

		String priOrPub = "public";
		String exception = "";

		if (wsOption.equalsIgnoreCase(JSONRPC_WS)) {
			priOrPub = "private";
			exception = "throws Exception";

			if (comments) {
				theImpl.add("/*****");
			}
			if (comments) {
				theImpl.add("* RPC Method Declaration");
			}
			if (comments) {
				theImpl.add("* @param inParam A Request that complies with JAX RPC 2.0");
			}
			if (comments) {
				theImpl.add("* @return The response to this request");
			}
			if (comments) {
				theImpl.add("*****/");
			}
			theImpl.add("@SuppressWarnings(\"unchecked\")");
			theImpl.add("public String " + JSONRPC + "(String inParam)");
			theImpl.add(I + "{");
			theImpl.add(I + "String theReturnString = \"\";");

			theImpl.add(I + JSONRPC_REQUEST + "[] reqArray = null;");
			if (comments) {
				theImpl.add(I
						+ "// Not all requests result in responses, and responses might be errors, so we use an ArrayList");
			}
			theImpl.add(I + "@SuppressWarnings(\"rawtypes\")");
			theImpl.add(I + "ArrayList ral = null;");

			theImpl.add(I + "boolean isAnArray = false;");
			theImpl.add(I + "");
			theImpl.add(I + "// Handle nulls and empty strings");
			theImpl.add(I
					+ "if (inParam == null || inParam.length() == 0 || inParam.equals(\"null\") /* Seen in testing */) {");
			theImpl.add(I + I + "");
			theImpl.add(I + I + JSONRPC_ERROR + " err = new " + JSONRPC_ERROR + "();");

			theImpl.add(I + I + "err.code = " + newInterfaceFilename + ".BAD_PARAMS;");
			theImpl.add(I + I + "err.message = \"Request appears to be null or zero length\";");
			theImpl.add(I + I + "");
			theImpl.add(I + I + JSONRPC_ERROR_RESPONSE + " er = new " + JSONRPC_ERROR_RESPONSE + "();");
			theImpl.add(I + I + "er.jsonrpc = " + newInterfaceFilename + ".JSON_RPC_VERSION;");
			theImpl.add(I + I + "er.error = err;");
			theImpl.add(I + I + "er.id = null;");
			theImpl.add("");
			theImpl.add(I + I + "// Turn our error response into JSON and send it back...");

			theImpl.add(I + I

					+ "theLog.error(err.message);");
			theImpl.add(I + I + "theReturnString = gson.toJson(er);");
			theImpl.add(I + I + "return (theReturnString);");
			theImpl.add(I + I + "}");
			theImpl.add(I + "");

			if (comments) {
				theImpl.add(I + "//");
				theImpl.add(I + "// Note that since we can't control what is in 'inParam' the 'catch'");
				theImpl.add(I + "// section of this try block will be frequently used.");
				theImpl.add(I + "//");

			}
			theImpl.add(I + "try {");
			theImpl.add(I + I + "// See if it's an array");

			theImpl.add(I + I + "reqArray = gson.fromJson(inParam, " + JSONRPC_REQUEST + "[].class);");

			theImpl.add("");
			theImpl.add(I + I + "isAnArray = true;");
			// theImpl.add("ArrayList responseArraList = null;");
			theImpl.add(I + I + "if (theLog.getDebug()) {");
			theImpl.add(I + I + I + "theLog.debug(\"Got Array of \" + reqArray.length + \" Requests:\");");

			theImpl.add(I + I + I + "for (int i=0; i < reqArray.length; i++)");

			theImpl.add(I + I + I + I + "{");
			theImpl.add(I + I + I + I + "theLog.debug(reqArray[i].toString());");
			theImpl.add(I + I + I + I + "}");

			theImpl.add(I + I + I + "}");
			theImpl.add(I + I + "} catch (Exception eNotAnArray) {");

			theImpl.add(I + I + "try {");
			theImpl.add(I + I + I + "// See if it's a single instance of a " + JSONRPC_REQUEST);
			theImpl.add(I + I + I + JSONRPC_REQUEST + " singleRequest = new " + JSONRPC_REQUEST + "();");
			theImpl.add(I + I + I + "singleRequest = gson.fromJson(inParam, " + JSONRPC_REQUEST + ".class);");
			theImpl.add(I + I + I + "reqArray = new " + JSONRPC_REQUEST + "[1];");
			theImpl.add(I + I + I + "reqArray[0] = singleRequest;");
			theImpl.add(I + I + I + "");

			theImpl.add(I + I + I + "if (theLog.getDebug()) {");
			theImpl.add(I + I + I + I + I + "theLog.debug(\"Got Valid Request:\" + singleRequest);");
			theImpl.add(I + I + I + I + "}");

			theImpl.add(I + I + I + "} catch (Exception eNotARequest) {");

			theImpl.add(I + I + I + "// Whatever this request is it isn't a valid " + JSONRPC_REQUEST);

			theImpl.add(I + I + I + JSONRPC_ERROR + " err = new " + JSONRPC_ERROR + "();");

			// theImpl.add("try {");
			theImpl.add(I + I + I + "// See if it is in fact JSON at all...");

			theImpl.add(I + I + I + "if (isItJson(inParam)) {");
			// theImpl.add("@SuppressWarnings(\"unused\")");
			theImpl.add(I + I + I + I + "err.code = " + newInterfaceFilename + ".INVALID_REQUEST;");
			theImpl.add(I + I + I + I + "err.message = \"Input is a JSON object but not a request\";");
			theImpl.add("");

			theImpl.add(I + I + I + I + "} else {");
			theImpl.add(I + I + I + I + "err.code = " + newInterfaceFilename + ".BAD_PARAMS;");
			theImpl.add(I + I + I + I + "err.message = \"Request doesn't appear to be a valid JSON object\";");
			theImpl.add(I + I + I + I + "}");
			theImpl.add("");

			theImpl.add(I + I + I + "theLog.warning(err.code + \":\" + err.message);");
			theImpl.add(I + I + I + "theLog.warning(inParam);");

			theImpl.add(I + I + I + "err.data = inParam;");
			theImpl.add("");
			theImpl.add(I + I + I + JSONRPC_ERROR_RESPONSE + " er = new " + JSONRPC_ERROR_RESPONSE + "();");
			theImpl.add(I + I + I + "er.jsonrpc = " + newInterfaceFilename + ".JSON_RPC_VERSION;");
			theImpl.add(I + I + I + "er.error = err;");
			theImpl.add(I + I + I + "er.id = null;");
			theImpl.add("");
			theImpl.add(I + I + I + "try {");
			theImpl.add(I + I + I + I + "// Turn our error response into JSON and send it back...");

			theImpl.add(I + I + I + I + "theLog.warning(\"Request doesn't appear to be a valid JSON object:\");");
			theImpl.add(I + I + I + I + "theLog.warning(inParam);");
			theImpl.add(I + I + I + I + "theReturnString = gson.toJson(er);");
			theImpl.add(I + I + I + I + "theLog.warning(theReturnString);");
			theImpl.add(I + I + I + I + "return (theReturnString);");
			theImpl.add("");

			theImpl.add(I + I + I + I + "} catch (Exception unableToCreateJsonError) {");
			theImpl.add(I + I + I + I + "theLog.error(\"Unable to encode JSON error message. Input request was\");");
			theImpl.add(I + I + I + I + "theLog.error(inParam);");
			theImpl.add(I + I + I + I + "theLog.error(unableToCreateJsonError);");

			if (comments) {
				theImpl.add(I + I + I + I + "// We should never do this but we're out of alternatives");
			}
			theImpl.add(I + I + I + I + "return ( \"{\\\"jsonrpc\\\": \\\"2.0\\\", \\\"error\\\": {\\\"code\\\": \" + "
					+ newInterfaceFilename
					+ ".BAD_PARAMS + \", \\\"message\\\": \\\"Request doesn't appear to be a valid JSON object\\\"}, \\\"id\\\": null})\\\"\");");
			theImpl.add(I + I + I + I + "}");
			theImpl.add("");

			theImpl.add(I + I + I + "}");
			theImpl.add("");

			theImpl.add(I + I + "}");
			theImpl.add("");
			// if (comments) theImpl.add("// Note ");
			// theImpl.add(I+"@SuppressWarnings(\"rawtypes\")");

			theImpl.add(I + "ral = new ArrayList(reqArray.length);");
			if (comments) {
				theImpl.add(I + "// We could in theory do all of this without the rpc* methods but we found ");
			}
			if (comments) {
				theImpl.add(I + "// that one of regression tests broke because of the size of this method if  ");
			}
			if (comments) {
				theImpl.add(I + "// we don't parcel out the work to rpc methods  ");
			}
			theImpl.add(I + "for (int i=0; i < reqArray.length; i++) {");

			theImpl.add(I + I + "");
			theImpl.add(
					I + I + "if (reqArray[i].jsonrpc == null || (! reqArray[i].jsonrpc.equals(JSON_RPC_VERSION))) {");
			theImpl.add(I + I + "");
			theImpl.add(I + I + "ral.add(makeError(new Exception(\"invalid value for jsonrpc\"), reqArray[i].jsonrpc"
					+ ", reqArray[i].id));");
			theImpl.add(I + I
					+ "theLog.error(\"Unable to call service due to bad value of jsonrpc:'\" + reqArray[i].jsonrpc + \"'. Input request was\");");
			theImpl.add(I + I + "theLog.error(reqArray[i].toString());");
			theImpl.add(I + I + "");
			theImpl.add(I + I + "}");
			theImpl.add(I + I + "else if (reqArray[i].method == null || reqArray[i].method.length() == 0) {");
			theImpl.add(I + I + "");
			theImpl.add(
					I + I + "ral.add(makeError(new Exception(\"null or zero length method name\"), reqArray[i].method"
							+ ", reqArray[i].id));");
			theImpl.add(I + I
					+ "theLog.error(\"Unable to call service due to null or zero length method name. request id was \" + reqArray[i].id );");
			theImpl.add(I + I + "");
			theImpl.add(I + I + "}");

			String elseOrSpace = "else ";
			for (AbstractmethodInterface theMethod : theMethods) {
				theImpl.add(I + I + elseOrSpace + "if (reqArray[i].method.equals(\"" + theMethod.getMethodName()
						+ "\")) {");
				elseOrSpace = "else ";

				// theImpl.add(I +I +"try {");
				theImpl.add("");
				theImpl.add(I + I + I + "rpc" + theMethod.getMethodName() + "(reqArray[i],ral);");
				theImpl.add("");
				// theImpl.add(I +I +I +JSONRPC_RESPONSE + " sr = new "
				// + JSONRPC_RESPONSE + "();");
				// theImpl.add(I +I +I +"sr.jsonrpc = " + newInterfaceFilename
				// + ".JSON_RPC_VERSION;");
				// theImpl.add(I +I +I +"sr.id = reqArray[i].id;");
				// theImpl.add("");
				// theImpl.add(I +I +I +"if (theLog.getDebug()) {");
				// theImpl.add(I +I +I +I +"theLog.debug(\""
				// + theMethods[i].getMethodName()
				// +
				// ": Starting request \" + sr.id + \":\" + reqArray[i].params);");
				// theImpl.add(I +I +I +I +"}");
				// theImpl.add("");
				// if (theMethods[i].getMethodParams().length > 0) {
				// if (theMethods[i].getMethodReturn().equals("void")) {
				// theImpl.add(I +I +I +theMethods[i].getMethodName()
				// + "(reqArray[i].params);");
				// theImpl.add(I +I +I +"sr.result = \"\";");
				// } else {
				// theImpl.add(I +I +I +"sr.result = "
				// + theMethods[i].getMethodName()
				// + "(reqArray[i].params);");
				//
				// }
				//
				// } else {
				// if (theMethods[i].getMethodReturn().equals("void")) {
				// theImpl.add(I +I +I +theMethods[i].getMethodName() + "();");
				// theImpl.add(I +I +I +"sr.result = \"\";");
				// } else {
				// theImpl.add(I +I +I +"sr.result = "
				// + theMethods[i].getMethodName() + "();");
				// }
				//
				// }
				//
				// // if (theMethods[i].getMethodReturn().equals("void")) {
				// theImpl.add("");
				// theImpl.add("");
				// theImpl.add(I +I +I +"if (theLog.getDebug()) {");
				// theImpl.add(I +I +I +I +"theLog.debug(\""
				// + theMethods[i].getMethodName()
				// +
				// ": Processed request \" + sr.id + \": returning \" + sr.result);");
				// theImpl.add(I +I +I +"}");
				// theImpl.add("");
				// theImpl.add(I+I+I+"if (reqArray[i].id != null && reqArray[i].id.length() >
				// 0)");
				// //theImpl.add(I+I+I+"@SuppressWarnings(\"rawtypes\")");
				// theImpl.add(I+I+I+I+"{");
				// if (comments)
				// theImpl.add(I+I+I+I+"// Only send result if we have an id, otherwise this");
				// if (comments)
				// theImpl.add(I+I+I+I+"// is a 'Notification' and no response is expected");
				// theImpl.add(I +I +I +I+"ral.add(sr);");
				// theImpl.add(I+I+I+I+"}");
				// theImpl.add("");
				//
				// theImpl.add(I +I +I
				// +"} catch (Exception unableToCallService) {");
				// theImpl.add("");
				// // theImpl.add(I +I +I +I +JSONRPC_ERROR + " err = new " +
				// JSONRPC_ERROR
				// // + "();");
				// // theImpl.add(I +I +I +I +"err.code = " +
				// newInterfaceFilename
				// // + ".INTERNAL_ERROR;");
				// // theImpl.add(I +I +I +I
				// +"err.message = unableToCallService.getMessage();");
				// // theImpl.add(I +I +I +I +"err.data = reqArray[i].method;");
				// // theImpl.add("");
				// // theImpl.add(I +I +I +I +JSONRPC_ERROR_RESPONSE +
				// " er = new "
				// // + JSONRPC_ERROR_RESPONSE + "();");
				// // theImpl.add(I +I +I +I +"er.jsonrpc = " +
				// newInterfaceFilename
				// // + ".JSON_RPC_VERSION;");
				// // theImpl.add(I +I +I +I +"er.error = err;");
				// // theImpl.add(I +I +I +I +"er.id = reqArray[i].id;");
				// // theImpl.add("");
				// // theImpl.add(I +I +I +I +"ral.add(er);");
				// theImpl.add("");
				// //theImpl.add(I +I +I +I+"@SuppressWarnings(\"rawtypes\")");
				//
				// theImpl.add(I +I +I +I
				// +"ral.add(makeError(unableToCallService,\"" +
				// theMethods[i].getMethodName() + "\", reqArray[i].id));");
				// theImpl.add("");
				// theImpl.add(I +I +I +I
				// +"theLog.error(\"Unable to call service '"
				// + theMethods[i].getMethodName()
				// + "'. Input request was\");");
				// theImpl.add(I +I +I +I +"theLog.error(reqArray[i].params);");
				// theImpl.add(I +I +I +I
				// +"theLog.error(unableToCallService);");
				//
				// theImpl.add(I +I +I +I +"}");
				//
				// theImpl.add(I +I +I +"//");
				theImpl.add(I + I + I + "}");
			}

			theImpl.add(I + I + "else {");
			if (comments) {
				theImpl.add(I + I + I + "// We don't recognize the method name...");
			}
			theImpl.add(I + I + I + JSONRPC_ERROR + " err = new " + JSONRPC_ERROR + "();");
			theImpl.add(I + I + I + "err.code = " + newInterfaceFilename + ".METHOD_NOT_FOUND;");
			theImpl.add(I + I + I
					+ "err.message = \"Request '\" + reqArray[i].id + \"' has unrecognized method of '\" + reqArray[i].method + \"'\";");
			theImpl.add(I + I + I + "err.data = inParam;");
			theImpl.add("");
			theImpl.add(I + I + I + JSONRPC_ERROR_RESPONSE + " er = new " + JSONRPC_ERROR_RESPONSE + "();");
			theImpl.add(I + I + I + "er.jsonrpc = " + newInterfaceFilename + ".JSON_RPC_VERSION;");
			theImpl.add(I + I + I + "er.error = err;");
			theImpl.add(I + I + I + "er.id = reqArray[i].id;");
			// theImpl.add(I +I +I +"@SuppressWarnings(\"rawtypes\")");
			theImpl.add(I + I + I + "ral.add(er);");
			theImpl.add("");
			theImpl.add(I + I + I + "theLog.error(err.message);");
			theImpl.add(I + I + I + "theLog.error(inParam);");
			theImpl.add(I + I + I + "}");

			theImpl.add(I + I + "}");

			theImpl.add(I + "try {");
			theImpl.add(I + I + "ral.trimToSize(); ");
			theImpl.add(I + I + "Object[] responseObjectArray = ral.toArray();");
			theImpl.add(I + I + "if (responseObjectArray.length == 0) {");
			if (comments) {
				theImpl.add(I + I + I + "// no responses - return empty string");
			}
			theImpl.add(I + I + I + "theReturnString = \"\";");
			theImpl.add(I + I + I + "}");
			theImpl.add(I + I + I + "else if (isAnArray) {");
			theImpl.add(I + I + I + I + "theReturnString = gson.toJson(responseObjectArray);");
			theImpl.add(I + I + I + "}");
			theImpl.add(I + I + "else {");
			theImpl.add(I + I + I + "theReturnString = gson.toJson(responseObjectArray[0]);");
			theImpl.add(I + I + I + "}");
			theImpl.add(I + I + "} catch (Exception unableToJsonizeOutput) {");
			theImpl.add("");
			theImpl.add(I + I + I + JSONRPC_ERROR + " err = new " + JSONRPC_ERROR + "();");
			theImpl.add("");
			theImpl.add(I + I + I + "if (unableToJsonizeOutput.getMessage().startsWith(" + newInterfaceFilename
					+ ".BAD_PARAMS+\"\")) {");
			theImpl.add(I + I + I + "err.code = " + newInterfaceFilename + ".BAD_PARAMS;");
			theImpl.add(I + I + I + "} else {");
			theImpl.add(I + I + I + "err.code = " + newInterfaceFilename + ".INTERNAL_ERROR;");
			theImpl.add(I + I + I + "} ");
			theImpl.add("");
			theImpl.add(I + I + "err.message = unableToJsonizeOutput.getMessage();");
			// theImpl.add("err.data = reqArray[i].method;");
			theImpl.add("");
			theImpl.add(I + I + JSONRPC_ERROR_RESPONSE + " er = new " + JSONRPC_ERROR_RESPONSE + "();");
			theImpl.add(I + I + "er.jsonrpc = " + newInterfaceFilename + ".JSON_RPC_VERSION;");
			theImpl.add(I + I + "er.error = err;");
			theImpl.add("");
			// theImpl.add("er.id = reqArray[i].id;");
			// theImpl.add("ral.add(er);");
			theImpl.add(I + I + "theLog.error(\"Unable to serialize JSON output:\");");
			// theImpl.add("theLog.error(err);");
			theImpl.add(I + I + "theLog.error(unableToJsonizeOutput);");
			theImpl.add("");
			theImpl.add(I + I + "theReturnString = gson.toJson(er);");
			theImpl.add(I + I + "}");

			// reqArray
			theImpl.add(I + "return (theReturnString);");
			theImpl.add(I + "}");

			for (AbstractmethodInterface theMethod : theMethods) {
				if (comments) {
					theImpl.add("/*****");
				}
				if (comments) {
					theImpl.add("* Internal method to call " + theMethod.getMethodName());
				}
				if (comments) {
					theImpl.add("* Only used by " + JSONRPC + ".");
				}
				if (comments) {
					theImpl.add("* @param JsonRpc20Request theRequest JAX RPC 2.0 request");
				}
				if (comments) {
					theImpl.add("* @param ArrayList ral Arraylist for results.");
				}
				if (comments) {
					theImpl.add("*****/");
				}

				theImpl.add("private void rpc" + theMethod.getMethodName()
						+ "(JsonRpc20Request theRequest, ArrayList ral) {");
				// theImpl.add(I +I +elseOrSpace
				// + "if (reqArray[i].method.equals(\""
				// + theMethods[i].getMethodName() + "\")) {");
				// elseOrSpace = "else ";
				//
				theImpl.add(I + "try {");
				theImpl.add("");
				theImpl.add(I + I + JSONRPC_RESPONSE + " sr = new " + JSONRPC_RESPONSE + "();");
				theImpl.add(I + I + "sr.jsonrpc = " + newInterfaceFilename + ".JSON_RPC_VERSION;");
				theImpl.add(I + I + "sr.id = theRequest.id;");
				theImpl.add("");
				theImpl.add(I + I + "if (theLog.getDebug()) {");
				theImpl.add(I + I + I + "theLog.debug(\"" + theMethod.getMethodName()
						+ ": Starting request \" + sr.id + \":\" + theRequest.params);");
				theImpl.add(I + I + I + "}");
				theImpl.add("");
				if (theMethod.getMethodParams().length > 0) {
					if (theMethod.getMethodReturn().equals("void")) {
						theImpl.add(I + I + theMethod.getMethodName() + "(theRequest.params);");
						theImpl.add(I + I + "sr.result = \"\";");
					} else {
						theImpl.add(I + I + "sr.result = " + theMethod.getMethodName() + "(theRequest.params);");

					}

				} else {
					if (theMethod.getMethodReturn().equals("void")) {
						theImpl.add(I + I + theMethod.getMethodName() + "();");
						theImpl.add(I + I + "sr.result = \"\";");
					} else {
						theImpl.add(I + I + "sr.result = " + theMethod.getMethodName() + "();");
					}

				}

				// if (theMethods[i].getMethodReturn().equals("void")) {
				theImpl.add("");
				theImpl.add("");
				theImpl.add(I + I + "if (theLog.getDebug()) {");
				theImpl.add(I + I + I + "theLog.debug(\"" + theMethod.getMethodName()
						+ ": Processed request \" + sr.id + \": returning \" + sr.result);");
				theImpl.add(I + I + "}");
				theImpl.add("");
				theImpl.add(I + I + "if (theRequest.id != null && theRequest.id.length() > 0)");
				// theImpl.add(I+I+I+"@SuppressWarnings(\"rawtypes\")");
				theImpl.add(I + I + I + "{");
				if (comments) {
					theImpl.add(I + I + I + "// Only send result if we have an id, otherwise this");
				}
				if (comments) {
					theImpl.add(I + I + I + "// is a 'Notification' and no response is expected");
				}
				theImpl.add(I + I + I + "ral.add(sr);");
				theImpl.add(I + I + I + "}");
				theImpl.add("");

				theImpl.add(I + I + "} catch (Exception unableToCallService) {");
				theImpl.add("");
				// theImpl.add(I +I +I +I +JSONRPC_ERROR + " err = new " +
				// JSONRPC_ERROR
				// + "();");
				// theImpl.add(I +I +I +I +"err.code = " + newInterfaceFilename
				// + ".INTERNAL_ERROR;");
				// theImpl.add(I +I +I +I
				// +"err.message = unableToCallService.getMessage();");
				// theImpl.add(I +I +I +I +"err.data = reqArray[i].method;");
				// theImpl.add("");
				// theImpl.add(I +I +I +I +JSONRPC_ERROR_RESPONSE + " er = new "
				// + JSONRPC_ERROR_RESPONSE + "();");
				// theImpl.add(I +I +I +I +"er.jsonrpc = " +
				// newInterfaceFilename
				// + ".JSON_RPC_VERSION;");
				// theImpl.add(I +I +I +I +"er.error = err;");
				// theImpl.add(I +I +I +I +"er.id = reqArray[i].id;");
				// theImpl.add("");
				// theImpl.add(I +I +I +I +"ral.add(er);");
				theImpl.add("");
				// theImpl.add(I +I +I +I+"@SuppressWarnings(\"rawtypes\")");

				theImpl.add(I + I + "ral.add(makeError(unableToCallService,\"" + theMethod.getMethodName()
						+ "\", theRequest.id));");
				theImpl.add("");
				theImpl.add(I + I + "theLog.error(\"Unable to call service '" + theMethod.getMethodName()
						+ "'. Input request was\");");
				theImpl.add(I + I + "theLog.error(theRequest.params);");
				theImpl.add(I + I + "theLog.error(unableToCallService);");

				theImpl.add(I + I + "}");
				//
				// theImpl.add(I +I +I +"//");
				theImpl.add(I + "}");
			}

			if (comments) {
				theImpl.add("/*****");
				theImpl.add("* JSON-RPC 2.0 Request Object");
				theImpl.add("* @see http://www.jsonrpc.org/specification#request_object");
				theImpl.add("*****/");

			}
			theImpl.add("class " + JSONRPC_REQUEST);
			theImpl.add("{");
			theImpl.add(I + "String jsonrpc;");
			theImpl.add(I + "String method;");
			theImpl.add(I + "String params;");
			theImpl.add(I + "String id;");
			theImpl.add(I + "public String toString() {");
			theImpl.add(I + I + "return jsonrpc + \":\"  + id + \":\" + method + \":\" + params;");
			theImpl.add(I + "}");
			theImpl.add("}");

			if (comments) {
				theImpl.add("/*****");
				theImpl.add("* JSON-RPC 2.0 Response Object");
				theImpl.add("* @see http://www.jsonrpc.org/specification#response_object");
				theImpl.add("*****/");

			}
			theImpl.add("class " + JSONRPC_RESPONSE);
			theImpl.add("{");
			theImpl.add(I + "String jsonrpc;");
			theImpl.add(I + "String result;");
			theImpl.add(I + "String id;");
			theImpl.add(I + "public String toString() {");
			theImpl.add(I + I + "return jsonrpc + \":\"  + id + \":\" + result;");
			theImpl.add(I + "}");
			theImpl.add("}");
		}
		if (comments) {
			theImpl.add("/*****");
			theImpl.add("* JSON-RPC 2.0 Error Object");
			theImpl.add("* @see http://www.jsonrpc.org/specification#error_object");
			theImpl.add("*****/");

		}
		theImpl.add("class " + JSONRPC_ERROR);
		theImpl.add("{");
		theImpl.add(I + "int code;");
		theImpl.add(I + "String message;");
		theImpl.add(I + "String data;");
		theImpl.add(I + "public String toString() {");
		theImpl.add(I + I + "return code + \":\"  + message + \":\" + data;");
		theImpl.add(I + "}");
		theImpl.add("}");
		// if (wsOption.equalsIgnoreCase(JSONRPC_WS)) {
		if (comments) {
			theImpl.add("/*****");
			theImpl.add("* JSON-RPC 2.0 Error Response Object");
			theImpl.add("* @see http://www.jsonrpc.org/specification#response_object");
			theImpl.add("*****/");

		}
		theImpl.add("class " + JSONRPC_ERROR_RESPONSE);
		theImpl.add("{");
		theImpl.add(I + "String jsonrpc;");
		theImpl.add(I + JSONRPC_ERROR + " error;");
		theImpl.add(I + "String id;");
		theImpl.add(I + "public String toString() {");
		theImpl.add(I + I + "return jsonrpc + \":\"  + error + \":\" + id;");
		theImpl.add(I + "}");
		theImpl.add("}");

		// }

		for (AbstractmethodInterface theMethod : theMethods) {

			// if (theMethods[i].getMethodParams().length > 0) {
			// if (comments) {
			// theImpl.add("/*****");
			// theImpl.add("* Inner class used to hold results of GSON de-serialization for
			// ");
			// theImpl.add("* " + theMethods[i].getMethodName());
			// theImpl.add("*****/");
			//
			// }
			// theImpl.add("class GsonTarget" + theMethods[i].getMethodName()
			// + " {");
			//
			// for (int j = 0; j < theMethods[i].getMethodParams().length; j++)
			// {
			// theImpl.add(I+theMethods[i].getMethodParams()[j][1]
			// + " "
			// + theMethods[i].getMethodParams()[j][0]
			// + getNullAssign(theMethods[i].getMethodParams()[j][1])
			// + ";");
			//
			// }
			// theImpl.add("}");
			// }

			if (theMethod == null) {
				System.out.println("null");
			}
			if (theMethod.getMethodName() == null) {
				System.out.println("null");
				System.out.println(theMethod.getMethodName());
			}
			String header = priOrPub + " String " + theMethod.getMethodName();

			if (theMethod.getMethodReturn().equals("void")) {
				header = priOrPub + " void " + theMethod.getMethodName();
			}
			if (comments) {
				theImpl.add("/*****");

				theImpl.add("* " + theMethod.getMethodName());

				theImpl.add("* @param inParam A JSON-encoded list of parameters");

				if (!theMethod.getMethodReturn().equals("void")) {
					theImpl.add(
							"* @return The response to this request, encoded in JSON. Note it could be an error message.");
				}

				theImpl.add("*****/");
			}

			// theImpl.add(header + "(");
			if (theMethod.getMethodParams().length > 0) {
				if (wsOption.equalsIgnoreCase(JSON_WS)) {
					theInterface.add(header + " (String inParam);");
				}
				theImpl.add(header + " (String inParam) " + exception + " {");
			} else {
				if (wsOption.equalsIgnoreCase(JSON_WS)) {
					theInterface.add(header + " ();");
				}
				theImpl.add(header + " () " + exception + " {");

			}

			if (!theMethod.getMethodReturn().equals("void")) {
				theImpl.add(I + theMethod.getMethodReturn() + " theReturn "
						+ getNullAssign(theMethod.getMethodReturn()) + ";");
				theImpl.add(I + "String theReturnString = \"\";");
			}

			if (theMethod.getMethodParams().length > 0) {
				theImpl.add(I + "GsonTarget" + theMethod.getMethodName() + " gTarget ");

				theImpl.add(I + " = new " + "GsonTarget" + theMethod.getMethodName() + "();");
			}
			theImpl.add("");
			if (theMethod.getMethodParams().length > 0) {
				if (comments) {
					theImpl.add(I + "//");
					theImpl.add(I + "// Note that since we can't control what is in 'inParam' the 'catch'");
					theImpl.add(I + "// section of this try block will be frequently used.");
					theImpl.add(I + "//");

				}
				theImpl.add(I + "try {");
				theImpl.add(I + I + "// Set parameters contained in inParam");
				theImpl.add(I + I + "JsonElement j = theParser.parse(inParam);");

				for (int j = 0; j < theMethod.getMethodParams().length; j++) {

					theImpl.add(I + I + "checkHasParam(\"" + theMethod.getMethodParams()[j][0] + "\",j);");

				}

				theImpl.add(I + I + "gTarget = gson.fromJson(inParam,GsonTarget" + theMethod.getMethodName()
						+ ".class);");

				theImpl.add(I + "} catch (Exception e) {");
				theImpl.add(I + I + "String message = null;");
				theImpl.add(I + I + "if (e.getMessage().startsWith(MISSING_PARAMETER)) {");
				theImpl.add(I + I + "message = " + newInterfaceFilename + ".BAD_PARAMS + \":\" + \""
						+ theMethod.getMethodName() + ": Parse Json:\" + e.getMessage();");
				theImpl.add("");
				theImpl.add(I + I + "} else if (isItJson(inParam)) {");

				theImpl.add(I + I + "message = " + newInterfaceFilename + ".BAD_PARAMS + \":\" + \""
						+ theMethod.getMethodName() + ": Parse Json:\" + e.getMessage();");
				theImpl.add("");
				theImpl.add(I + I + "} else {");
				theImpl.add(I + I + "message = " + newInterfaceFilename + ".INTERNAL_ERROR + \":\" + \""
						+ theMethod.getMethodName() + ": Parse Json:\" + e.getMessage();");
				theImpl.add("");
				theImpl.add(I + I + "}");
				theImpl.add(I + "theLog.warning(message);");
				if (wsOption.equalsIgnoreCase(JSON_WS)) {
					if (theMethod.getMethodReturn().equals("void")) {
						if (comments) {
							theImpl.add(I + "// Nothing we can do here - calling program doesn't expect a response.  ");
						}
						theImpl.add(I + "return;");

					} else {
						if (comments) {
							theImpl.add(I + "// Not much else we can do, as JSON API doesn't cover errors. Send a  ");
						}
						if (comments) {
							theImpl.add(I + "// JSON-RPC error message so at least they get JSON back  ");
						}
						theImpl.add(I + "return(message);");

					}
				} else {
					theImpl.add(I + "throw new Exception(message);");

				}
				theImpl.add(I + "}");
				theImpl.add("");
			}

			theImpl.add("");
			theImpl.add(I + "try {");

			theImpl.add(I + I + "// Call Service");
			String assign = "";
			if (theMethod.getMethodReturn().equals("void")) {
				assign = "theService." + theMethod.getMethodName() + "(";
			} else {
				assign = "theReturn = theService." + theMethod.getMethodName() + "(";

			}
			String spaceOrBracket = "";

			for (int j = 0; j < theMethod.getMethodParams().length; j++) {
				if (j == theMethod.getMethodParams().length - 1) {
					spaceOrBracket = ");";
				}

				theImpl.add(I + I + assign + "gTarget." + theMethod.getMethodParams()[j][0] + spaceOrBracket);
				assign = SPACES.substring(0, assign.length() - 1) + ",";
			}
			if (theMethod.getMethodParams().length == 0) {
				theImpl.add(I + I + assign + ");");
			}

			// theImpl.add(assign);
			theImpl.add("");
			theImpl.add(I + I + "} catch (Exception e) {");
			theImpl.add(I + I + "String message = " + newInterfaceFilename + ".INTERNAL_ERROR + \":\" + \""
					+ theMethod.getMethodName() + ": Call Service:\" + e.getMessage();");
			theImpl.add(I + I + "theLog.error(message);");
			if (wsOption.equalsIgnoreCase(JSON_WS)) {
				if (theMethod.getMethodReturn().equals("void")) {
					theImpl.add(I + I + "return;");
				} else {
					theImpl.add(I + I + "return(message);");
				}
			} else {
				theImpl.add(I + I + "throw new Exception(message);");

			}
			theImpl.add(I + "}");
			theImpl.add("");
			if (!theMethod.getMethodReturn().equals("void")) {
				theImpl.add(I + "try {");
				theImpl.add(I + I + "// Create return JSON object");
				theImpl.add(I + I + "theReturnString = gson.toJson(theReturn);");
				theImpl.add(I + I + "} catch (Exception e) {");
				theImpl.add(I + I + "String message = " + newInterfaceFilename + ".INTERNAL_ERROR + \":\" + \""
						+ theMethod.getMethodName() + ": JSONize result:\" + e.getMessage();");
				theImpl.add(I + I + "theLog.error(message);");
				if (wsOption.equalsIgnoreCase(JSON_WS)) {
					theImpl.add(I + I + "return(message);");
				} else {
					theImpl.add(I + I + "throw new Exception(message);");

				}
				theImpl.add(I + I + "}");

				theImpl.add(I + "return (theReturnString);");

			}
			// theImpl.add(theMethods[i].getMethodName());

			header = header + "(";

			// String spaceOrBracket = "";
			// String closingBracket = "";
			//
			// for (int j=0; j < theMethods[i].getMethodParams().length; j++) {
			// if (j == theMethods[i].getMethodParams().length -1) {
			// spaceOrBracket = ") {";
			// }
			// theImpl.add(header +
			// theMethods[i].getMethodParams()[j][1] + " " +
			// theMethods[i].getMethodParams()[j][0] + spaceOrBracket);
			// header = SPACES.substring(0,header.length()-1)+",";
			//
			// }

			//
			theImpl.add(I + "}");

		}

		// checkHasParam
		if (comments) {
			theImpl.add("/*****");
			theImpl.add("* See if something is actually JSON or not");
			theImpl.add("* @param String possibleJson A String which may or may not be JSON");
			theImpl.add("* @param JsonElement theJson A partially parsed JSON string");
			// theImpl.add("* @return true if it's JSON, false otherwise");
			theImpl.add("* @throws Exception if not parsable as JSON");
			theImpl.add("*****/");

		}
		theImpl.add("private void checkHasParam(String paramName, JsonElement theJsonElement) throws Exception {");
		// theImpl.add(I + "try {");
		theImpl.add(I + "JsonObject jObject = (JsonObject)theJsonElement.getAsJsonObject();");

		theImpl.add(I + "if (jObject.get(paramName) == null) {");
		theImpl.add(I + I + "// Parameter is not in in JSON object...");

		theImpl.add(I + I + "throw new Exception(MISSING_PARAMETER+\":\" + paramName);");
		theImpl.add(I + I + "}");

		// theImpl.add(I + I + "} catch (Exception notJsonException) {");
		// theImpl.add(I + I + "return (false);");
		// theImpl.add(I + I + "}");
		theImpl.add(I + "}");
		theImpl.add("");

		if (comments) {
			theImpl.add("/*****");
			theImpl.add("* See if something is actually JSON or not");
			theImpl.add("* @param String possibleJson A String which may or may not be JSON");
			theImpl.add("* @return true if it's JSON, false otherwise");
			theImpl.add("*****/");

		}
		theImpl.add("private boolean isItJson(String possibleJson) {");
		theImpl.add(I + "try {");
		theImpl.add(I + I + "// See if it is in fact JSON at all...");

		theImpl.add(I + I + "@SuppressWarnings(\"unused\")");
		theImpl.add(I + I + "JsonElement thejson = theParser.parse(possibleJson); ");
		theImpl.add(I + I + "// Must be JSON to get to this line...");

		theImpl.add(I + I + "return (true);");
		theImpl.add(I + I + "");

		theImpl.add(I + I + "} catch (Exception notJsonException) {");
		theImpl.add(I + I + "return (false);");
		theImpl.add(I + I + "}");
		theImpl.add(I + "}");
		theImpl.add("");

		if (wsOption.equalsIgnoreCase(JSONRPC_WS)) {
			if (comments) {
				theImpl.add("/*****");
				theImpl.add("* Compose error response");
				theImpl.add("* @param Exception ex the exception we got");
				theImpl.add("* @param String meth The method we are on");
				theImpl.add("* @param String id The client's transaction id");
				theImpl.add("* @return JSONRPC_ERROR_RESPONSE an JSON RPC 2.0 Error response");
				theImpl.add("*****/");

			}
			theImpl.add("private " + JSONRPC_ERROR_RESPONSE + " makeError(Exception ex");
			theImpl.add("                             ,String meth");
			theImpl.add("                             ,String id) {");
			theImpl.add("");
			theImpl.add(I + JSONRPC_ERROR + " er = new " + JSONRPC_ERROR + "();");
			theImpl.add(I + "er.code = " + newInterfaceFilename + ".INTERNAL_ERROR;");
			theImpl.add(I + "er.message = ex.getMessage();");
			theImpl.add(I + "er.data = meth;");
			theImpl.add("");
			theImpl.add(I + JSONRPC_ERROR_RESPONSE + " res = new " + JSONRPC_ERROR_RESPONSE + "();");
			theImpl.add(I + "res.jsonrpc = " + newInterfaceFilename + ".JSON_RPC_VERSION;");
			theImpl.add(I + "res.error = er;");
			theImpl.add(I + "res.id = id;");

			theImpl.add(I + "");
			theImpl.add(I + "return(res);");
			theImpl.add(I + "}");
		}
		for (AbstractmethodInterface theMethod : theMethods) {

			if (theMethod.getMethodParams().length > 0) {
				if (comments) {
					theImpl.add("/*****");
					theImpl.add("* Inner class used to hold results of GSON de-serialization for ");
					theImpl.add("* " + theMethod.getMethodName());
					theImpl.add("*****/");

				}
				theImpl.add("class GsonTarget" + theMethod.getMethodName() + " {");

				for (int j = 0; j < theMethod.getMethodParams().length; j++) {
					theImpl.add(I + theMethod.getMethodParams()[j][1] + " " + theMethod.getMethodParams()[j][0]
							+ getNullAssign(theMethod.getMethodParams()[j][1]) + ";");

				}
				theImpl.add("}");
			}
		}

		theInterface.add("}");
		theImpl.add("}");

		String[] theInterfaceArray = { "" };
		theInterfaceArray = theInterface.toArray(theInterfaceArray);

//		for (int i = 0; i < theInterfaceArray.length; i++) {
//			System.out.println(theInterfaceArray[i]);
//		}

		String[] theImplArray = { "" };
		theImplArray = theImpl.toArray(theImplArray);

//		for (int i = 0; i < theImplArray.length; i++) {
//			System.out.println(theImplArray[i]);
//		}

		File tempFile = new File(theDir.getAbsolutePath() + File.separator + newPackage.replace(".", File.separator)
				+ File.separator + "Temp" + ".java");

		File ifaceFile = new File(theDir.getAbsolutePath() + File.separator + newPackage.replace(".", File.separator)
				+ File.separator + newInterfaceFilename + ".java");

		File implFile = new File(theDir.getAbsolutePath() + File.separator + newPackage.replace(".", File.separator)
				+ File.separator + newImplFileName + ".java");

		ifaceFile.getParentFile().mkdirs();

		writeFile(ifaceFile, theInterfaceArray);

		writeFile(implFile, theImplArray);

		// Jalopy j = new Jalopy();
		//
		// writeFile(tempFile, theInterfaceArray);
		// try {
		// j.setInput(tempFile);
		// } catch (FileNotFoundException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// j.setOutput(ifaceFile);
		// if (!j.format()) {
		// try {
		// com.mcpdbwizard.pub.IOUtils.copyFile(tempFile, ifaceFile);
		// } catch (CSException e) {
		// // TODO Auto-generated catch block
		// theLog.error(e);
		// }
		// }
		//
		// tempFile.delete();
		//
		// writeFile(tempFile, theImplArray);
		// try {
		// j.setInput(tempFile);
		// } catch (FileNotFoundException e) {
		//
		// e.printStackTrace();
		// }
		// j.setOutput(implFile);
		// if (true /* !j.format() */) {
		// try {
		// com.mcpdbwizard.pub.IOUtils.copyFile(tempFile, implFile);
		// } catch (CSException e) {
		//
		// theLog.error(e);
		// }
		// }
		//
		// tempFile.delete();

	}

	public static String getNullAssign(String javaDataType) {
		if (javaDataType.equals(BOOLEAN)) {
			return "= false";
		} else if (javaDataType.endsWith("[]")) {
			return " = new " + javaDataType.replace("[]", "[0]");
		} else if (javaDataType != javaDataType.toLowerCase()) {
			return " = null";
		} else {
			return " = 0";
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		LogInterface log = new ConsoleLog();

//		if (args.length == 1 && args[0].equals("TEST")) {
//			ConsoleLog theLog = new ConsoleLog();
//				McpDbWizardConnectorTest tst = new McpDbWizardConnectorTest (theLog);
//			TestInterface t = (TestInterface) tst;
//			t.test(false);
//			System.exit(0);
//		}

//		if (args.length == 1 && args[0].equals("DATA")) {
//
//			String codeDir = "C:\\Work\\CodeSpooks2\\OrindaConnectionWrangler\\src";
//			String[] options = { "JSON-RPC", "JSON" };
//
//			String sourceIface = "com.mcpdbwizard.mcpdbwizardconnector.test.service.DAOFactoryServiceInterface";
//			String sourceImpl = "com.mcpdbwizard.mcpdbwizardconnector.test.service.DAOFactoryServiceImpl";
//			String packageBase = "com.mcpdbwizard.mcpdbwizardconnector.test.";
//			String iFaceName = "JsonIface";
//			String implName = "JsonImpl";
//
//			for (int z = 0; z < options.length; z++) {
//				McpDbWizardConnectorGenerator g = new McpDbWizardConnectorGenerator(null,
//						log);
//
//				File dir = new File(codeDir);
//
//				if (!dir.exists()) {
//					log.error("Directory '" + dir.getAbsolutePath()
//							+ "' does not exist");
//
//				}
//
//				Class<?> iFace = null;
//				Class<?> impl = null;
//
//				String target = options[z];
//
//				boolean targetValid = false;
//				for (int i = 1; i < getWsOptions().length; i++) {
//					if (target.equalsIgnoreCase(getWsOptions()[i])) {
//						target = getWsOptions()[i];
//						targetValid = true;
//						break;
//					}
//
//				}
//				try {
//					iFace = Class.forName(sourceIface);
//				} catch (ClassNotFoundException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				try {
//					impl = Class.forName(sourceImpl);
//				} catch (ClassNotFoundException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//
//				String packageName = packageBase
//						+ options[z].toLowerCase().replace("-", "_");
//				String tgtIface = iFaceName;
//				String tgtImpl = implName;
//
//				AbstractmethodInterface[] theMethods = g.generateBaseMethods(
//						log, impl, iFace, dir, packageName, tgtIface, tgtImpl);
//
//				if (target.equalsIgnoreCase(JSON_WS)
//						|| target.equalsIgnoreCase(JSONRPC_WS)) {
//					g.generateJsonMethods(log, impl, iFace, dir, packageName,
//							tgtIface, tgtImpl, theMethods, true, true, target);
//				}
//
//			}
//			System.exit(0);
//
//		}

//		if (args.length != 7) {
//			log.info("Usage  : "
//					+ "McpDbWizardConnectorGenerator"
//					+ "code_directory service_type source_interface_class source_impl_class target_package target_interface_class target_impl_class");
//
//			log.info("Example: "
//					+ "McpDbWizardConnectorGenerator"
//					+ "C:\\Test JSON com.foo.gar.myInterface com.foo.gar.myService com.foo.json JsonIface JsonService");
//			log.info("Valid values for service_type are:");
//			for (int i = 1; i < getWsOptions().length; i++) {
//				log.info(getWsOptions()[i]);
//
//			}
//			System.exit(1);
//		}
//
//		McpDbWizardConnectorGenerator g = new McpDbWizardConnectorGenerator(null, log);
//
//		File dir = new File(args[0]);
//
//		if (!dir.exists()) {
//			log.error("Directory '" + dir.getAbsolutePath()
//					+ "' does not exist");
//
//		}
//
//		Class<?> iFace = null;
//		Class<?> impl = null;
//
//		String target = args[1];
//
//		boolean targetValid = false;
//		for (int i = 1; i < getWsOptions().length; i++) {
//			if (target.equalsIgnoreCase(getWsOptions()[i])) {
//				target = getWsOptions()[i];
//				targetValid = true;
//				break;
//			}
//
//		}
//		try {
//			iFace = Class.forName(args[2]);
//		} catch (ClassNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		try {
//			impl = Class.forName(args[3]);
//		} catch (ClassNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		String packageName = args[4];
//		String tgtIface = args[5];
//		String tgtImpl = args[6];
//
//		AbstractmethodInterface[] theMethods = g.generateBaseMethods(log, impl,
//				iFace, dir, packageName, tgtIface, tgtImpl);
//
//		if (target.equalsIgnoreCase(JSON_WS)
//				|| target.equalsIgnoreCase(JSONRPC_WS)) {
//			g.generateJsonMethods(log, impl, iFace, dir, packageName, tgtIface,
//					tgtImpl, theMethods, true, true, target);
//		}
	}

//	public void generate(String target, LogInterface log, String impl, String iface, String iFace, File dir, String packageName, String tgtIface, String tgtImpl) {
//		McpDbWizardConnectorGenerator g = new McpDbWizardConnectorGenerator(null, log);
//		AbstractmethodInterface[] theMethods = g.generateBaseMethods(log, impl,
//				iFace, dir, packageName, tgtIface, tgtImpl);
//
//		if (target.equalsIgnoreCase(JSON_WS)
//				|| target.equalsIgnoreCase(JSONRPC_WS)) {
//			g.generateJsonMethods(log, impl, iFace, dir, packageName, tgtIface,
//					tgtImpl, theMethods, true, true, target);
//
//	}

}
