package com.mcpdbwizard.schema;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * One Oracle table (or view) selected for generation. Corresponds to the {@code TABLE_NAME_<i>} /
 * {@code TABLE_USER_<i>} indexed key family in a {@code .pb2} file.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class Table {

    private int index;
    private String name;
    private String user;
    private String mcpCrud;
    private Map<String, String> mcpDescriptions = new LinkedHashMap<>();

    public Table() {
    }

    public Table(int index, String name, String user) {
        this.index = index;
        this.name = name;
        this.user = user;
    }

    public Table(int index, String name, String user, String mcpCrud) {
        this.index = index;
        this.name = name;
        this.user = user;
        this.mcpCrud = mcpCrud;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Which row operations this table may expose as MCP tools, as the letters of
     * {@code TABLE_MCP_CRUD_<i>}: "C" insert, "R" get_by_pk and the secondary lookups,
     * "U" update, "D" delete. {@code null} means the key is absent, which is all four —
     * so a config that predates this flag is unchanged. An empty string exposes none.
     */
    public String getMcpCrud() {
        return mcpCrud;
    }

    public void setMcpCrud(String mcpCrud) {
        this.mcpCrud = mcpCrud;
    }

    /** {@code TABLE_MCP_DESC_&lt;i&gt;_PK} — the {@code get_by_pk} tool. */
    public static final String OP_GET_BY_PK = "PK";
    /** {@code ..._INS} — the {@code insert} tool. */
    public static final String OP_INSERT = "INS";
    /** {@code ..._UPD} — the {@code update} tool. */
    public static final String OP_UPDATE = "UPD";
    /** {@code ..._DEL} — the {@code delete} tool. */
    public static final String OP_DELETE = "DEL";
    /** Prefix for a unique-key lookup, completed with the constraint name: {@code UK_EMP_CODE}. */
    public static final String OP_UNIQUE_KEY_PREFIX = "UK_";
    /** Prefix for an index lookup, completed with the index name. */
    public static final String OP_INDEX_PREFIX = "IX_";
    /** Prefix for a foreign-key child lookup, completed with the constraint name. */
    public static final String OP_FOREIGN_KEY_PREFIX = "FK_";

    /**
     * Author-supplied MCP tool descriptions for this table's operations, keyed by operation.
     *
     * <p><b>A table is not one tool.</b> It yields four fixed ones — {@link #OP_GET_BY_PK},
     * {@link #OP_INSERT}, {@link #OP_UPDATE}, {@link #OP_DELETE} — plus one per unique key, index
     * and foreign-key child, which is why this is a map and {@code mcpCrud} beside it is a set of
     * letters. Duality views ride the same family (a view is selected as a table) with
     * {@code DOC_*} operations.
     *
     * <p>An operation ABSENT from the map uses the description the generator writes from the data
     * dictionary. A present-but-empty value is an author choosing an empty description; the two
     * must stay distinguishable, so this map never stores null.
     *
     * <p><b>Keys are stable identifiers, not tool names.</b> The emitted tool name is derived at
     * generation time (lower-cased, punctuation-stripped, overload-suffixed) and the web UI cannot
     * reproduce it without duplicating generator logic. Keying on the operation plus the Oracle
     * constraint or index name lets both ends agree without either guessing.
     *
     * @return the live map, never null
     */
    public Map<String, String> getMcpDescriptions() {
        return mcpDescriptions;
    }

    public void setMcpDescriptions(Map<String, String> mcpDescriptions) {
        this.mcpDescriptions = (mcpDescriptions != null) ? mcpDescriptions : new LinkedHashMap<>();
    }

    /** The description for one operation, or null when none was supplied. */
    public String getMcpDescription(String theOperation) {
        return mcpDescriptions.get(theOperation);
    }

    /** Set one operation's description; null removes it, restoring the generated default. */
    public void setMcpDescription(String theOperation, String theDescription) {
        if (theDescription == null) {
            mcpDescriptions.remove(theOperation);
        } else {
            mcpDescriptions.put(theOperation, theDescription);
        }
    }

    /** Emit this table's keys into the target Properties. Null fields are left absent. */
    public void toPb2(Properties p) {
        if (name != null) {
            p.setProperty("TABLE_NAME_" + index, name);
        }
        if (user != null) {
            p.setProperty("TABLE_USER_" + index, user);
        }
        if (mcpCrud != null) {
            p.setProperty("TABLE_MCP_CRUD_" + index, mcpCrud);
        }
        for (Map.Entry<String, String> e : mcpDescriptions.entrySet()) {
            p.setProperty("TABLE_MCP_DESC_" + index + "_" + e.getKey(), e.getValue());
        }
    }

    public Map<String, Object> toJsonMap() {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        m.put("index", (double) index);
        m.put("name", name);
        m.put("user", user);
        if (mcpCrud != null) {
            m.put("mcpCrud", mcpCrud);
        }
        // Omitted entirely when empty, so a config with no descriptions serialises exactly as it
        // did before this existed - the same contract the null scalars keep.
        if (!mcpDescriptions.isEmpty()) {
            m.put("mcpDescriptions", new LinkedHashMap<String, Object>(mcpDescriptions));
        }
        return m;
    }

    @SuppressWarnings("unchecked")
    public static Table fromJsonMap(Map<String, Object> m) {
        Table t = new Table();
        t.index = ((Number) m.get("index")).intValue();
        t.name = (String) m.get("name");
        t.user = (String) m.get("user");
        t.mcpCrud = (String) m.get("mcpCrud");
        Object theDescriptions = m.get("mcpDescriptions");
        if (theDescriptions instanceof Map) {
            for (Map.Entry<String, Object> e : ((Map<String, Object>) theDescriptions).entrySet()) {
                t.mcpDescriptions.put(e.getKey(), (String) e.getValue());
            }
        }
        return t;
    }
}
