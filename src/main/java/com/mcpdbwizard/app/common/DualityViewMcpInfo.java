package com.mcpdbwizard.app.common;

/**
 * Metadata about one 23ai JSON-relational duality view, captured during table
 * generation and consumed by the MCP server emission
 * ({@code SAAdminWrangler.generateMcpServerClass}). Only populated when the
 * MCP_SERVER propfile flag is on and the introspected object turned out to be a
 * duality view; carried on the view's {@link SingleNamespaceObject}.
 *
 * @author devteam@mcpdbwizard.com
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 * @version 1
 */
public class DualityViewMcpInfo {

    /**
     * One field of the view's JSON document, from ALL_JSON_DUALITY_VIEW_TAB_COLS.
     * Only root-table fields are captured; a field with a non-zero
     * {@code primaryKeyPos} lives inside the document's {@code _id} (as the whole
     * scalar {@code _id} for a single-column key, or as one field of the
     * {@code _id} object for a composite key) rather than at the top level.
     */
    public static class DocField {
        /** The field's key in the JSON document (e.g. "name", or "_id"/"k1" for key fields). */
        public String jsonKeyName;
        /** The underlying column's Oracle datatype (NUMBER, VARCHAR2, ...). */
        public String oracleType;
        /** TRUE when the view forbids updating this field. */
        public boolean readOnly;
        /** 1-based position in the root table's primary key; 0 when not a key column. */
        public int primaryKeyPos;

        public DocField(String jsonKeyName, String oracleType, boolean readOnly, int primaryKeyPos) {
            this.jsonKeyName = jsonKeyName;
            this.oracleType = oracleType;
            this.readOnly = readOnly;
            this.primaryKeyPos = primaryKeyPos;
        }
    }

    /** The view's single JSON document column (usually DATA). */
    public String jsonColumnName;

    /** Which document operations the view permits (ALLOW_INSERT/UPDATE/DELETE). */
    public boolean allowInsert;
    public boolean allowUpdate;
    public boolean allowDelete;

    /** TRUE when the root table's PK has more than one column, making _id a JSON object. */
    public boolean compositeId;

    /**
     * TRUE when every root-table primary-key column is a database-generated
     * identity column, i.e. the database assigns the document's _id on insert and
     * the caller must not supply one.
     */
    public boolean idGenerated;

    /** The root table the view is built over (for documentation text). */
    public String rootTableName;

    /**
     * TRUE when the view maps in tables besides the root, i.e. the document nests
     * child objects/arrays whose keys are NOT in {@link #docFields} — the emitted
     * document schemas must then leave additionalProperties open.
     */
    public boolean hasNestedTables;

    /** Simple class names of the generated manager/row classes for this view. */
    public String managerClassName;
    public String rowClassName;

    /** The document's fields, in dictionary order (root-table columns only). */
    public java.util.List<DocField> docFields = new java.util.ArrayList<DocField>();
}
