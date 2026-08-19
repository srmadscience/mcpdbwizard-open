package com.mcpdbwizard.app.common;

import com.mcpdbwizard.mcpdbwizardconnector.BaseMethodRepresentation;

//import com.mcpdbwizard.pub.SqlUtils;

/**
 * @author devteam@mcpdbwizard.com
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 * @version 2
 */
public class SingleNamespaceObject {

    public static final int SEQUENCE = 0;
    public static final int FUNCTION = 1;
    public static final int PROC = 2;
    public static final int TABLE = 3;
    public static final int VIEW = 4;
    public static final int PLSQL_NAMED_RECORD = 5;
    public static final int PLSQL_ROWTYPE_RECORD = 6;
    public static final int ORACLE_TYPE_RECORD = 7;
    public static final int PLSQL_INDEXBY_ARRAY = 8;
    public static final int PLSQL_TABLE_ARRAY = 9;
    public static final int ORACLE_SCALER_TYPE = 10;
    public static final int PLSQL_NONCOLL_ARRAY = 11;
    public static final int PLSQL_SUBTYPE_RECORD = 12;
    public static final int PLSQL_PACK_SCALER_ARRAY = 13;
    public static final int PLSQL_PACK_ROWTYPE_ARRAY = 13; //14;
    public static final int PLSQL_INDEXBY_ARRAY_ROWTYPE = 15;


    /**
     * The name of the database Object.
     * maps to OBJECT_NAME in USER_OBJECTS
     */
    public String objectName;

    /**
     * The owner of the Object.
     * Not in USER_OBJECTS but needed anyway.
     */
    public String owner;

    /**
     * The owner of the Object.
     * Not in USER_OBJECTS but needed anyway.
     */
    public String realOwner;


    /**
     * The owner of the Object.
     * Not in USER_OBJECTS but needed anyway.
     */
    public String realName;

    /**
     *
     */
    public String javaName = null;

    // Attribute file file name.
    public String javaAttrName = null;

    /**
     * The string used to refer to this object from within oracle
     */
    public String oracleName = null;

    /**
     *
     */
    public String fixedJavaName = null;

    public String oracleNameBasis = null;

    public String packageName = null;

    public boolean hasBadRecords = false;

    /**
     * The number of the overload if the argument is overloaded...
     */
    public String overload = null;

    /**
     * File for web service method code
     */
    public java.io.File webserviceMethodCode;

    /**
     * File for web service interface code
     */
    public java.io.File webserviceInterfaceCode;


    public BaseMethodRepresentation theMetaData = null;

    /**
     * Duality-view metadata for the MCP server emission; null unless this object
     * is a 23ai JSON-relational duality view AND the MCP_SERVER flag is on.
     */
    public DualityViewMcpInfo mcpDualityViewInfo = null;

    /**
     * Secondary-lookup methods (unique-key / index) captured during table
     * generation for the MCP server emission; null unless this is an ordinary table
     * AND the MCP_SERVER flag is on. Consumed by {@code buildTableMcpInfo}.
     */
    public java.util.List<TableMcpInfo.Lookup> mcpTableLookups = null;

    /**
     * Which row operations this table may expose as MCP tools, as the letters of
     * {@code TABLE_MCP_CRUD_<i>} in the .pb2 ("C" insert, "R" get_by_pk and the
     * secondary lookups, "U" update, "D" delete). {@code null} means the key was
     * absent, which is ALL FOUR — every config written before this flag existed
     * therefore generates exactly the tool set it did before. Read from the config
     * by {@code TableTableDataModel.readTableInfo}, copied onto {@link TableMcpInfo}
     * by {@code buildTableMcpInfo}.
     */
    public String mcpCrud = null;

    /**
     * Author-supplied MCP tool descriptions for this object, or null when none were configured.
     *
     * <p>A TABLE keys them by operation — {@code PK}, {@code INS}, {@code UPD}, {@code DEL},
     * {@code UK_<name>}, {@code IX_<name>}, {@code FK_<name>} — because one table yields several
     * tools. A sequence, routine or SQL statement yields exactly one, so it uses the single key
     * {@link #MCP_DESC_SOLE}: one field serves both shapes rather than two that could disagree.
     *
     * <p>null, and a key absent from a non-null map, both mean "use the description the generator
     * derives from the data dictionary". A present-but-empty value is an author choosing an empty
     * description. See {@code com.mcpdbwizard.schema.Table} for the storage contract.
     */
    public java.util.Map<String, String> mcpDescriptions = null;

    /** The map key a single-tool object (sequence, routine, statement) stores its description under. */
    public static final String MCP_DESC_SOLE = "SOLE";

    /**
     * This object's identity <b>as the config spells it</b>, joined with {@link #MCP_ID_SEP} —
     * {@code owner::pkg::name::overload} for a routine, {@code owner::name} for a table or
     * sequence. Null when nothing set it.
     *
     * <p><b>Why a carried field rather than something rebuilt from the other fields on this
     * object.</b> When the generator declines to expose an object as an MCP tool it reports which
     * one, so an editor can tell an author that a description they wrote will go nowhere. That
     * report is useless unless it names the object the way the <em>config</em> does — and this
     * object's own {@link #packageName} is {@code REAL_PACKAGE_NAME}, the resolved name, while the
     * config stores {@code PACKAGE_NAME}. The two differ for every routine reached through a
     * synonym. Rebuilding a key here would therefore silently fail to match exactly the objects
     * whose indirection makes them worth explaining, so the key is captured where the config is
     * read and carried unchanged. {@code FunctionTableDataModel} already computes it correctly for
     * its own lookup and carries the same warning.
     */
    public String mcpConfigId = null;

    /**
     * Separator for {@link #mcpConfigId}. Matches what the web editor keys its rows by; it is a
     * property of the config identity, not of either UI, which is why it lives here.
     */
    public static final String MCP_ID_SEP = "::";

    /**
     * This object's description for one operation, or null to use the generated default.
     *
     * <p>Null-safe on both the map and the key, because the overwhelmingly common case is a config
     * that has set no descriptions at all, and every emission site would otherwise need the guard.
     */
    public String mcpDescriptionFor(String theOperation) {
        return (mcpDescriptions == null) ? null : mcpDescriptions.get(theOperation);
    }

    /** Convenience for the single-tool objects. */
    public String mcpSoleDescription() {
        return mcpDescriptionFor(MCP_DESC_SOLE);
    }

    /**
     * Full top-level parameter metadata for a PL/SQL procedure/function, captured from
     * the callable-statement engine during wrapper generation, for the MCP server
     * emission to drive the wrapper directly (all IN/OUT/IN OUT params + return).
     * Each row is {@code {argumentName, inOut, javaType, variableName, position}}
     * (position 0 is the function return). Null unless this is a proc/function.
     */
    public String[][] mcpProcParams = null;

    /**
     *
     */
    public int objectType;

    public boolean objectInUse = true;

    /**
     * ID of object in parent array...
     */
    public int objectArrayId = 0;

    /**
     * String used to contain comment characters if this object is invalid.
     */
    public String cmt = "";

    public boolean hasFiles = false;

    public SingleNamespaceObject(String owner, String objectName, int objectType) {
        this.owner = new String(owner);
        this.objectName = new String(objectName);
        this.objectType = objectType;
    }

    public boolean checkObjectCreated(java.io.File baseDirectory, String subDirectory) {
        return (checkObjectCreated(baseDirectory, subDirectory, null));
    }

    public boolean checkObjectCreated(java.io.File baseDirectory, String subDirectory, String fileName) {
        boolean iExist = false;

        String searchFileName = fixedJavaName;
        if (fileName != null) {
            searchFileName = fileName;
        }

        java.io.File objectFileName = new java.io.File(baseDirectory.getAbsolutePath()
                + java.io.File.separator + subDirectory
                + java.io.File.separator + searchFileName + ".java");

        if (objectFileName.canRead()) {
            iExist = true;
        } else {
            // Set comment field so that generated code in DAO factory won't break.
            cmt = "// ";
        }
        return (iExist);
    }


}



