package com.mcpdbwizard.app.common;

/**
 * Metadata about one ordinary table, captured by the MCP server emission
 * ({@code SAAdminWrangler.generateMcpServerClass}) to generate row-level CRUD
 * tools (get_by_pk / insert / update / delete) over the table's generated
 * manager and row classes. Unlike {@link DualityViewMcpInfo} this is built by
 * querying the data dictionary directly at emission time (columns, primary key,
 * identity columns) rather than during table generation; the manager/row class
 * names are derived from the table's fixed Java name.
 * <p>
 * Only tables with a primary key whose key columns are all JSON-crossable get
 * tools; a non-key column of an unsupported type (LOB, RAW/binary vector, date,
 * record, …) is omitted from the tool schemas ({@link #hasSkippedColumns}) rather
 * than disqualifying the whole table.
 *
 * @author devteam@mcpdbwizard.com
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 * @version 1
 */
public class TableMcpInfo {

    /** One JSON-crossable column of the table. */
    public static class Column {
        /** The column's key in the JSON row document (the lower-cased column name). */
        public String jsonKey;
        /** The column's Oracle datatype (NUMBER, VARCHAR2, JSON, VECTOR, BOOLEAN, ...). */
        public String oracleType;
        /** The JSON-schema type word: number / string / boolean / object / array. */
        public String schemaType;
        /** The generated row getter to read this column (e.g. getRowFlag / getRowIdBigDecimalObj). */
        public String getterName;
        /** The generated row setter to write this column (e.g. setRowFlag). */
        public String setterName;
        /** The generated row null-setter (e.g. setRowFlagToNull). */
        public String setToNullName;
        /** 1-based position in the primary key; 0 for a non-key column. */
        public int primaryKeyPos;
        /** TRUE for a {@code GENERATED ALWAYS AS IDENTITY} column (the database assigns it). */
        public boolean generatedAlwaysIdentity;
        /**
         * LOB kind for a CLOB/BLOB column, {@code null} otherwise. A CLOB crosses as JSON
         * text, a BLOB as a base64 string. For a LOB column {@code getterName} is the
         * write-side byte-array getter and {@link #lobLocatorField} names the read-side public
         * locator field; the setter is the byte-array setter.
         */
        public String lobKind;
        /** LOB column only: the public {@code oracle.sql.CLOB}/{@code BLOB} locator field (read side). */
        public String lobLocatorField;
        /** The column's Java type (from {@code oracle2JavaDatatype}); drives date/RAW crossing. */
        public String javaType;

        public Column(String jsonKey, String oracleType, String schemaType,
                      String getterName, String setterName, String setToNullName,
                      int primaryKeyPos, boolean generatedAlwaysIdentity) {
            this.jsonKey = jsonKey;
            this.oracleType = oracleType;
            this.schemaType = schemaType;
            this.getterName = getterName;
            this.setterName = setterName;
            this.setToNullName = setToNullName;
            this.primaryKeyPos = primaryKeyPos;
            this.generatedAlwaysIdentity = generatedAlwaysIdentity;
        }
    }

    /** The table's Oracle name (for tool names and descriptions). */
    public String tableOracleName;

    /**
     * The table's Oracle owner, for the {@code db_object} label on the Prometheus metrics. Held
     * here because {@code addTableMcpTools} is handed this object and nothing else, while the owner
     * is resolved (through any synonym) back in {@code buildTableMcpInfo}.
     */
    public String tableOwner;

    /**
     * This table's identity as the CONFIG spells it, for the generator's "yields no MCP
     * tool" report. Carried rather than rebuilt from {@link #tableOwner} — that one is the
     * RESOLVED owner, which differs from the config's whenever the table is reached through a
     * synonym. See {@link com.mcpdbwizard.app.common.SingleNamespaceObject#mcpConfigId}.
     */
    public String tableConfigId;

    /** The generated manager class simple name (e.g. ObGen23aiMgr). */
    public String managerClassName;

    /** The generated row class simple name (e.g. ObGen23aiRow). */
    public String rowClassName;

    /** The factory getter that returns the manager (e.g. getObGen23aiTableDAO). */
    public String factoryGetterName;

    /** Lower-cased fixed Java name, used as the tool-method-name base (e.g. obGen23ai). */
    public String methodBase;

    /** TRUE when the table has one or more non-key columns of an unsupported type. */
    public boolean hasSkippedColumns;

    /**
     * Per-table curation, from {@code TABLE_MCP_CRUD_<i>} in the config. An operation
     * whose flag is FALSE has no tool emitted at all — the method is not generated and
     * is not in the server's tool list, so there is no code path to it, rather than a
     * runtime check that could be bypassed. All four default to TRUE, which is what an
     * absent key means, so a config written before this flag existed is unaffected.
     * <p>
     * {@link #readable} additionally gates the secondary lookups ({@link #lookups}):
     * they are reads of this table, and a foreign-key child lookup on a PARENT table is
     * dropped when the CHILD is not readable (the child's row-to-JSON helper is only
     * generated for an exposed table — see the fk post-pass in
     * {@code generateMcpServerClass}).
     */
    public boolean readable = true;
    public boolean insertable = true;
    public boolean updatable = true;
    public boolean deletable = true;

    /**
     * TRUE when no operation at all is exposed, in which case the table contributes no
     * tools and no row&lt;-&gt;JSON helpers. Callers drop the table entirely.
     */
    public boolean isFullySuppressed() {
        return !readable && !insertable && !updatable && !deletable;
    }

    /**
     * Apply a {@code TABLE_MCP_CRUD_<i>} value. {@code null} (key absent) leaves all four
     * TRUE. Letters are case-insensitive; any unrecognised character is ignored, and an
     * empty (but present) value suppresses every operation.
     */
    /**
     * Author-supplied tool descriptions for this table's operations, keyed as
     * {@code com.mcpdbwizard.schema.Table} defines them ({@code PK}, {@code INS}, {@code UPD},
     * {@code DEL}, {@code UK_<name>}, {@code IX_<name>}, {@code FK_<name>}). Copied off the
     * {@code SingleNamespaceObject} alongside the CRUD flags; null when none were configured.
     */
    public java.util.Map<String, String> mcpDescriptions = null;

    /**
     * The author's description for one operation, or null to use the generated default.
     *
     * <p>Null-safe on the map because a config with no descriptions at all is the common case and
     * the alternative is the same guard repeated at every emission site.
     */
    public String descriptionFor(String theOperation) {
        return (mcpDescriptions == null) ? null : mcpDescriptions.get(theOperation);
    }

    public void applyCrudFlags(String theFlags) {
        if (theFlags == null) {
            return;
        }
        String upper = theFlags.toUpperCase();
        readable = upper.indexOf('R') >= 0;
        insertable = upper.indexOf('C') >= 0;
        updatable = upper.indexOf('U') >= 0;
        deletable = upper.indexOf('D') >= 0;
    }

    /** The JSON-crossable columns, in dictionary order. */
    public java.util.List<Column> columns = new java.util.ArrayList<Column>();

    /**
     * One generated secondary-lookup method (unique-key or index) exposed as a
     * row-lookup tool. Captured during table generation (the exact method name and
     * cardinality the generator emitted), then its key columns are resolved to
     * {@link Column}s at emission time. A unique-key lookup is called through its
     * row overload ({@code <method>(<Row>)}); an index lookup has no row overload,
     * so it is called with the key values as ordered scalar arguments.
     */
    public static class Lookup {
        /** The generated manager method (e.g. getByUkUkCode / getChildByIxIxCategory / getChildByFkR55). */
        public String methodName;
        /**
         * The constraint or index whose columns are the lookup KEY (resolved via the
         * column query): for a unique key its UNIQUE constraint, for an index the index,
         * for a foreign-key child lookup the PARENT primary/unique constraint the FK
         * references (whose columns live on this table). Name + owner.
         */
        public String constraintName;
        public String constraintOwner;
        /** The name the tool is built from (the UK/index name, or the child FK constraint name). */
        public String toolNameBasis;
        /** "uk" (unique key), "ix" (index), or "fk" (foreign-key child lookup). */
        public String kind;
        /** TRUE when the method returns a single row (unique key / unique index); FALSE for a row array. */
        public boolean single;
        /** TRUE when the method has a row overload (unique keys / FK child); FALSE means scalar arguments (indexes). */
        public boolean useRowOverload;
        /** FK child lookup only: the child table's row class (the array element type). */
        public String childRowClass;
        /** FK child lookup only: the child table's fixed Java name (to find its RowToJson helper + confirm it is exposed). */
        public String childTableFixedName;
        /** FK child lookup only: the child table's method-name base (lower-cased fixed name); set during the emission post-pass. */
        public String childMethodBase;
        /** The key columns (resolved to the table's crossable columns at emission time), in key order. */
        public java.util.List<Column> keyColumns = new java.util.ArrayList<Column>();
    }

    /** The generated unique-key / index row-lookup methods exposed as tools. */
    public java.util.List<Lookup> lookups = new java.util.ArrayList<Lookup>();
}
