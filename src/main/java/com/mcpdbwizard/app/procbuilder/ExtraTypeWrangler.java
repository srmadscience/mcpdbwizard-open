package com.mcpdbwizard.app.procbuilder;

import java.sql.*;
//import oracle.sql.*;
//import oracle.sql.ArrayDescriptor;
//import oracle.jdbc.driver.*;
import java.util.Properties;
////import java.util.Map;
//import java.util.List;
import java.util.ArrayList;
//import java.util.Hashtable;
import java.io.*;
import java.text.SimpleDateFormat;

//import javax.swing.*;
//import javax.swing.tree.*;

import com.mcpdbwizard.pub.*;
import com.mcpdbwizard.app.common.*;
import com.mcpdbwizard.app.common.JavaUtils;
import com.mcpdbwizard.app.procbuilder.gui.*;


/**
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class ExtraTypeWrangler {

    LogInterface theLog = null;

    String prefix = "X";

    ArrayList extraTypeList = new ArrayList();


    ArrayList longNames = new ArrayList();

    int lastFoundType = Integer.MIN_VALUE;

    public ExtraTypeWrangler(LogInterface theLog) {
        this.theLog = theLog;
    }

    public void reset(String prefix) {
        this.prefix = prefix;
    }

    public void addCreateMissingTypeObjectsMethod(JavaChunk theJavaChunk, boolean comments, boolean debugMessages
            , String stheLog, String factoryClassName, String stempFilePrefix) {

        extraTypeList.trimToSize();
        Object[] entries = extraTypeList.toArray();

        boolean found = false;


        theJavaChunk.print(" ");

        if (comments) {
            theJavaChunk.print("/** ");
            theJavaChunk.print("* Executes a DDL statement");
            theJavaChunk.print("* @return true if the statement ran, false if it failed with");
            theJavaChunk.print("*         allowableErrorCode and that failure was tolerated. Callers that");
            theJavaChunk.print("*         only need the object to exist can ignore this; a CREATE OR REPLACE");
            theJavaChunk.print("*         caller cannot, because false means the database still holds the");
            theJavaChunk.print("*         PREVIOUS definition.");
            theJavaChunk.print("* @since 5.1.2557 ");
            theJavaChunk.print("*/ ");
        }

        theJavaChunk.print("private boolean executeImmediate(String ddl, int allowableErrorCode) throws CSException");
        theJavaChunk.indent();
        theJavaChunk.print("{                                                                   ");
        theJavaChunk.print("java.sql.CallableStatement theStatement = null;                                      ");
        theJavaChunk.print("                                                                 ");


        theJavaChunk.print("if (theConnection == null)");
        theJavaChunk.indent();
        theJavaChunk.print("{                                  ");
        theJavaChunk.print("throw (new CSException(\"Not Connected - Statement can not be issued\"));  ");
        theJavaChunk.print("}                      ");
        theJavaChunk.unIndent();


        theJavaChunk.print("                                                                 ");
        //if (stats) theJavaChunk.print("startStatsTimer();     ");
        if (comments) theJavaChunk.print("// Parse execute immediate request...                          ");
        theJavaChunk.print("try                                                              ");
        theJavaChunk.indent();
        theJavaChunk.print("{                                                                ");
        theJavaChunk.print("" + stheLog + ".info(ddl);");
        if (debugMessages)
            theJavaChunk.print("" + stheLog + ".debug(\"" + "executeImmediate" + " - Starting to parse statement\");");
        theJavaChunk.print("theStatement = theConnection.prepareCall(ddl);    ");
        if (debugMessages)
            theJavaChunk.print("" + stheLog + ".debug(\"" + "executeImmediate" + " - Finished parsing statement\");");
        theJavaChunk.print("}                                                                  ");
        theJavaChunk.unIndent();
        theJavaChunk.print("catch (java.sql.SQLException e)                                    ");
        theJavaChunk.indent();
        theJavaChunk.print("{                                                                   ");
        //if (stats)theJavaChunk.print("incErrorCount();");
        theJavaChunk.print("" + stheLog + ".error(\"" + "executeImmediate" + ": Prepare Statement failed with \" + e.toString());");
        theJavaChunk.print("CSDBException e2 = new CSDBException(e.getErrorCode(),e.getMessage(),\"executeImmediate\",\"" + factoryClassName + "\");");
        theJavaChunk.print("throw (e2);   ");
        theJavaChunk.print("}       ");
        theJavaChunk.unIndent();
        //theJavaChunk.print("          ");
        //if (stats) theJavaChunk.print("incParseCount();   ");
        //theJavaChunk.print("                  ");


        theJavaChunk.print("                                                              ");
        theJavaChunk.print("try                                                              ");
        theJavaChunk.indent();
        theJavaChunk.print("{                                                                ");
        theJavaChunk.print("theStatement.execute();");
        theJavaChunk.print("theStatement.close();");

        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print("catch (java.sql.SQLException e)");
        theJavaChunk.indent();
        theJavaChunk.print("{");

        theJavaChunk.print("if (e.getErrorCode() != allowableErrorCode)");
        theJavaChunk.indent();
        theJavaChunk.print("{                                  ");


        theJavaChunk.print(stheLog + ".error(\"" + factoryClassName + " - Execute Immediate Failed:\");");
        theJavaChunk.print(stheLog + ".error(e);");
        theJavaChunk.print("CSDBException e2 = new CSDBException(e.getErrorCode(),e.getMessage(),\"executeImmediate\",\"" + factoryClassName + "\");");
        theJavaChunk.print("throw(e2);");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print("return(false);");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();

        theJavaChunk.print("return(true);");

        theJavaChunk.print("}                      ");
        theJavaChunk.unIndent();

        addConfirmTypeMatchesMethod(theJavaChunk, comments, debugMessages, stheLog, factoryClassName);

        theJavaChunk.print(" ");
        if (comments) {
            theJavaChunk.print("/** ");
            theJavaChunk.print("* Creates generated Oracle TYPE objects that are needed to ");
            theJavaChunk.print("* access PL/SQL Package Array Parameters.");
            theJavaChunk.print("* @since 5.1.2557");
            theJavaChunk.print("*/ ");
        }

        theJavaChunk.print("public void createExtraTypeObjects() throws CSException");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("confirmConnection(\"createExtraTypeObjects\");");
        theJavaChunk.print(" ");

        // The DDL is held in a local so it can be handed to confirmExtraTypeMatches without being
        // written into the source twice.
        if (entries.length > 0) {
            theJavaChunk.print("String theDdl = null;");
            theJavaChunk.print(" ");
        }

        for (int i = 0; i < entries.length; i++) {
            ExtraType t = (ExtraType) entries[i];

            String[] tlines = t.getCreateStatement();

            // An array of scalars has no object type of its own - only the _A collection below.
            boolean haveObjectType = !(tlines.length == 1 && tlines[0].length() == 0);

            // A record with NO ATTRIBUTES yields "CREATE OR REPLACE TYPE x_T AS OBJECT" and nothing
            // else. Oracle ACCEPTS that and leaves the type INVALID rather than rejecting it, so it
            // fails silently: the _A collection built on it is invalid too, and any bind through
            // either raises at runtime instead of at generation. Nothing in the shipped propfiles
            // exercises one, which is why it has survived - so this warns rather than changing the
            // DDL, because ten generated classes name these types and inventing an attribute would
            // make the type valid while leaving every bind through it wrong.
            if (haveObjectType && tlines.length == 1) {
                warnEmptyRecord(t.getName());
            }

            for (int j = 0; j < tlines.length; j++) {
                tlines[j] = JavaUtils.replaceString(tlines[j], ";", "");

                if (tlines.length == 1) {
                    if (tlines[j].length() > 0) {
                        theJavaChunk.print("theDdl = \"" + tlines[j] + "\";");
                    }
                } else if (j == 0) {
                    theJavaChunk.print("theDdl = \"" + tlines[j] + "\" ");
                } else if (j == (tlines.length - 1)) {
                    theJavaChunk.print("               + \"" + tlines[j] + "\";");
                } else {
                    theJavaChunk.print("               + \"" + tlines[j] + "\"");
                }

                theJavaChunk.print(" ");
            }

            if (haveObjectType) {
                addCreateTypeCall(theJavaChunk, t.getName().toUpperCase() + "_T");
            }

            // Kept verbatim, trailing ';' included: this is the exact text that has always been
            // sent for the collection type, and normaliseTypeSpec discounts the semicolon anyway.
            theJavaChunk.print("theDdl = \"" + t.getCreateArrayStatement() + "\";");
            addCreateTypeCall(theJavaChunk, t.getName().toUpperCase() + "_A");

        }

        theJavaChunk.print(" ");

        theJavaChunk.print("}");
        theJavaChunk.unIndent();

        theJavaChunk.print(" ");
        if (comments) {
            theJavaChunk.print("/** ");
            theJavaChunk.print("* Removes generated Oracle TYPE objects that are needed to ");
            theJavaChunk.print("* access PL/SQL Package Array Parameters.");
            theJavaChunk.print("* @since 5.1.2557");
            theJavaChunk.print("*/ ");
        }

        theJavaChunk.print("public void dropExtraTypeObjects() throws CSException");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("confirmConnection(\"dropExtraTypeObjects\");");


        for (int i = 0; i < entries.length; i++) {
            ExtraType t = (ExtraType) entries[i];

            theJavaChunk.print(" ");
            theJavaChunk.print("executeImmediate(\"" + JavaUtils.replaceString(t.getDropArrayStatement(), ";", "") + "\",SqlUtils.OBJECT_DOES_NOT_EXIST); ");

            if (t.getDropTypeStatement() != null && JavaUtils.replaceString(t.getDropTypeStatement(), ";", "").length() > 0) {
                theJavaChunk.print(" ");
                theJavaChunk.print("executeImmediate(\"" + JavaUtils.replaceString(t.getDropTypeStatement(), ";", "") + "\",SqlUtils.OBJECT_DOES_NOT_EXIST); ");
            }

        }

        theJavaChunk.print(" ");

        theJavaChunk.print("}");
        theJavaChunk.unIndent();

        theJavaChunk.print(" ");
        if (comments) {
            theJavaChunk.print("/** ");
            theJavaChunk.print("* Removes generated Oracle TYPE objects that appear to have ");
            theJavaChunk.print("* been created by " + Namer.param_prod_name + ", even if they aren't used by this generated service.");
            theJavaChunk.print("* @since 5.1.2557");
            theJavaChunk.print("*/ ");
        }

        theJavaChunk.print("public void dropAllExtraTypeObjects() throws CSException");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print(" ");
        theJavaChunk.print("final String DROP_ALL_TYPES_BLOCK = ");
        theJavaChunk.print("    \"DECLARE\\n\" ");
        theJavaChunk.print("  + \"--\\n\"  ");
        theJavaChunk.print("  + \"CURSOR typecur IS\\n\" ");
        theJavaChunk.print("  + \"SELECT 'DROP TYPE '||type_name cmd\\n\" ");
        theJavaChunk.print("  + \"FROM user_types\\n\"");
        theJavaChunk.print("  + \"WHERE type_name LIKE '\" + " + stempFilePrefix + ".toUpperCase()  + \"%'\\n\" ");
        theJavaChunk.print("  + \"ORDER BY DECODE (typecode,'COLLECTION',1,2), type_name;\\n\"");
        theJavaChunk.print("  + \"--\\n\" ");
        theJavaChunk.print("  + \"BEGIN\\n\"  ");
        theJavaChunk.print("  + \"--\\n\"");
        theJavaChunk.print("  + \"FOR typerec IN typecur LOOP\\n\" ");
        theJavaChunk.print("  + \"--\\n\" ");
        theJavaChunk.print("  + \"  EXECUTE IMMEDIATE typerec.cmd;\\n\" ");
        theJavaChunk.print("  + \"--\\n\" ");
        theJavaChunk.print("  + \"END LOOP;\\n\" ");
        theJavaChunk.print("  + \"--\\n\" ");
        theJavaChunk.print("  + \"END;\\n\"; ");

        theJavaChunk.print(" ");
        theJavaChunk.print("confirmConnection(\"dropAllExtraTypeObjects\");");
        theJavaChunk.print("executeImmediate(DROP_ALL_TYPES_BLOCK,0); ");
        theJavaChunk.print(" ");

        theJavaChunk.print("}");
        theJavaChunk.unIndent();

    }

    /**
     * Emits the guarded CREATE for one type: run the DDL in {@code theDdl}, and when the replace was
     * refused, check what the database actually holds.
     *
     * <p>ORA-2303 ("cannot drop or replace a type with type or table dependents") is the ORDINARY
     * outcome here, not an exceptional one — the {@code _A} collection type is a dependent of the
     * {@code _T} object type, so from the second run onwards the object type can never be replaced.
     * That is why the error is tolerated. The catch is that a type whose SHAPE HAS CHANGED fails in
     * exactly the same way, leaving the old definition in place while this program goes on to bind
     * against it, so the caller silently reads and writes the wrong columns.
     *
     * @param theTypeName the Oracle type the DDL creates, used to look the stored definition up
     */
    private void addCreateTypeCall(JavaChunk theJavaChunk, String theTypeName) {
        theJavaChunk.print("if (!executeImmediate(theDdl,SqlUtils.TYPE_HAS_DEPENDENTS))");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("confirmExtraTypeMatches(\"" + theTypeName + "\", theDdl);");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print(" ");
    }

    /**
     * Emits {@code confirmExtraTypeMatches}.
     *
     * <p>Oracle keeps a type's specification text in {@code USER_SOURCE} exactly as it was submitted
     * (verified for object AND collection types), so the check is a text comparison against the very
     * statement we just tried to run — no mapping from the DDL's datatype spellings onto the data
     * dictionary's, which is where a comparison against {@code USER_TYPE_ATTRS} would spend all its
     * effort and earn its false alarms. The comparison itself is
     * {@link com.mcpdbwizard.pub.SqlUtils#normaliseTypeSpec}, in the runtime library rather than
     * emitted, so it can be unit-tested.
     */
    private void addConfirmTypeMatchesMethod(JavaChunk theJavaChunk, boolean comments
            , boolean debugMessages, String stheLog, String factoryClassName) {

        theJavaChunk.print(" ");
        if (comments) {
            theJavaChunk.print("/** ");
            theJavaChunk.print("* Checks that an existing Oracle TYPE matches the definition this code expects.");
            theJavaChunk.print("* <p>");
            theJavaChunk.print("* Called only when CREATE OR REPLACE was refused because the type has dependents,");
            theJavaChunk.print("* which is normal from the second run onwards. Normally the stored definition is");
            theJavaChunk.print("* the one we wanted and this returns quietly; if it differs, the type is stale and");
            theJavaChunk.print("* binding against it would silently move the wrong values, so this throws.");
            theJavaChunk.print("* @since 5.1.2557");
            theJavaChunk.print("*/ ");
        }
        theJavaChunk.print("private void confirmExtraTypeMatches(String theTypeName, String theIntendedDdl) throws CSException");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("StringBuffer theStored = new StringBuffer();");
        theJavaChunk.print("java.sql.PreparedStatement theStatement = null;");
        theJavaChunk.print("java.sql.ResultSet theResultSet = null;");
        theJavaChunk.print(" ");
        theJavaChunk.print("try");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("theStatement = theConnection.prepareStatement(\"select text from user_source"
                + " where name = ? and type = 'TYPE' order by line\");");
        theJavaChunk.print("theStatement.setString(1, theTypeName);");
        theJavaChunk.print("theResultSet = theStatement.executeQuery();");
        theJavaChunk.print("while (theResultSet.next())");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print("theStored.append(theResultSet.getString(1));");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print("theResultSet.close();");
        theJavaChunk.print("theStatement.close();");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print("catch (java.sql.SQLException e)");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        if (comments) {
            theJavaChunk.print("// Not being able to LOOK is not the same as finding a mismatch. Say so and");
            theJavaChunk.print("// carry on, exactly as this code did before the check existed.");
        }
        theJavaChunk.print(stheLog + ".error(\"" + factoryClassName + " - could not read the stored definition of \""
                + " + theTypeName + \" (\" + e.getMessage() + \"). Its definition has NOT been verified.\");");
        theJavaChunk.print("return;");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print(" ");
        if (comments) {
            theJavaChunk.print("// No rows means the type is not in THIS schema - it may be another user's, reached");
            theJavaChunk.print("// through a synonym or a grant. Nothing to compare against.");
        }
        theJavaChunk.print("if (theStored.length() == 0)");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        theJavaChunk.print(stheLog + ".info(\"" + factoryClassName + " - \" + theTypeName + \" is not owned by"
                + " this schema, so its definition has not been verified.\");");
        theJavaChunk.print("return;");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print(" ");
        theJavaChunk.print("String theWanted = SqlUtils.normaliseTypeSpec(theIntendedDdl);");
        theJavaChunk.print("String theActual = SqlUtils.normaliseTypeSpec(theStored.toString());");
        theJavaChunk.print(" ");
        theJavaChunk.print("if (theWanted.equals(theActual))");
        theJavaChunk.indent();
        theJavaChunk.print("{");
        if (debugMessages) {
            theJavaChunk.print(stheLog + ".debug(\"confirmExtraTypeMatches - \" + theTypeName + \" already exists"
                    + " with the expected definition\");");
        }
        theJavaChunk.print("return;");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();
        theJavaChunk.print(" ");
        if (comments) {
            theJavaChunk.print("// Stale. Both definitions go into the log because the difference is usually one");
            theJavaChunk.print("// column, and the fix depends on which.");
        }
        theJavaChunk.print(stheLog + ".error(\"" + factoryClassName + " - Oracle TYPE \" + theTypeName + \" is out of date.\");");
        theJavaChunk.print(stheLog + ".error(\"  in the database: \" + theStored.toString());");
        theJavaChunk.print(stheLog + ".error(\"  this code needs: \" + theIntendedDdl);");
        theJavaChunk.print("throw(new CSException(\"Oracle TYPE \" + theTypeName + \" does not match the definition"
                + " this code was generated against, and could not be replaced because other types or tables depend"
                + " on it. Drop the dependent types (see dropExtraTypeObjects) or run extraObjects.sql by hand,"
                + " then start again. Continuing would bind the wrong values.\"));");
        theJavaChunk.print("}");
        theJavaChunk.unIndent();

    }

    public void createExtraObjectsDotSql(TextChunk extraObjectsSql) {

        extraTypeList.trimToSize();
        Object[] entries = extraTypeList.toArray();

        boolean found = false;

        extraObjectsSql.addLine("/* Extra Objects creation file for " + Namer.param_prod_name + " */");
        extraObjectsSql.addLine("");

        for (int i = 0; i < entries.length; i++) {
            ExtraType t = (ExtraType) entries[i];

            extraObjectsSql.addLine("/* Extra Object " + t.getName() + " */");
            extraObjectsSql.addLine("");

            if (t.getCreateStatement().length == 1
                    && t.getCreateStatement()[0].equals("")) {
            } else {
                if (t.getCreateStatement().length == 1) {
                    // See warnEmptyRecord: this statement is accepted by Oracle and leaves an
                    // INVALID type. Said here too, because extraObjects.sql is run by hand by
                    // somebody who will not have seen the generation log.
                    extraObjectsSql.addLine("REM WARNING: " + t.getName() + " has no attributes.");
                    extraObjectsSql.addLine("REM The CREATE below is accepted but leaves an INVALID"
                            + " type, and the _A collection built on it is invalid too.");
                    warnEmptyRecord(t.getName());
                }
                extraObjectsSql.addLine(t.getCreateStatement());
                extraObjectsSql.addLine("");
            }

            extraObjectsSql.addLine(t.getCreateArrayStatement());
            extraObjectsSql.addLine("");

        }


    }

    /**
     * Report a record that produced an object type with no attributes.
     *
     * <p>Deliberately loud and deliberately not fatal: the tree still generates and compiles, and
     * every propfile in the suite has done so for years with one of these in it. What was missing
     * was any way to find out.
     */
    private void warnEmptyRecord(String theTypeName) {
        theLog.warning("Extra TYPE " + theTypeName.toUpperCase() + "_T has NO ATTRIBUTES, so the"
                + " emitted 'CREATE OR REPLACE TYPE ... AS OBJECT' is accepted by Oracle but leaves"
                + " the type INVALID. " + theTypeName.toUpperCase() + "_A is built on it and is"
                + " invalid too. Any PL/SQL array bind through either will fail at run time.");
    }

    public String registerObject(PlsqlRecordObject newPlsql, int arrayId) {


        ExtraType newType = new ExtraType(prefix, newPlsql, theLog, arrayId, longNames);


        extraTypeList.trimToSize();
        Object[] entries = extraTypeList.toArray();
        lastFoundType = Integer.MIN_VALUE;
        boolean found = false;

        for (int i = 0; i < entries.length; i++) {
            if (((ExtraType) entries[i]).getName().equals(newType.name)) {
                lastFoundType = i;
                found = true;
                break;
            }
        }


        if (!found) {
            extraTypeList.add(newType);
            lastFoundType = entries.length;
        }

        return (newType.name);
    }


    public ExtraType getLatestType() {
        extraTypeList.trimToSize();
        Object[] theObjects = extraTypeList.toArray();

        if (lastFoundType == Integer.MIN_VALUE) {
            return (null);
        }

        return ((ExtraType) theObjects[lastFoundType]);
    }


} 