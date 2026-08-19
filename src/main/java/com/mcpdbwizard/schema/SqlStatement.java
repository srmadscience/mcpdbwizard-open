package com.mcpdbwizard.schema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * One user SQL statement (an {@code EXTRA_SQL} entry). Corresponds to the single-indexed
 * {@code SQL_FILENAME_<i>} / {@code SQL_CREATE_CLASS_<i>} / {@code SQL_TURN_CURSORS_INTO_RECORDS_<i>}
 * keys plus the statement's {@link SqlParam} bind parameters (the doubly-indexed
 * {@code SQL_PARAM_*_<i>_<m>} keys). Each scalar field is nullable so presence/absence of the
 * underlying key round-trips exactly.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class SqlStatement {

    private int index;
    private String filename;
    private String createClass;
    private String turnCursorsIntoRecords;
    private List<SqlParam> params = new ArrayList<>();
    private String mcpDescription;

    /**
     * The statement itself.
     *
     * <p>Stored in the config as {@code SQL_TEXT_<i>} so a config is self-contained. Historically
     * only the {@link #filename} was stored and the text lived on disk under the global
     * {@code SQL_FILE_DIRECTORY} — which is why a committed propfile still carries an absolute
     * Windows path to a machine that no longer exists, and why every consumer has to rewrite that
     * directory before it can generate.
     *
     * <p>{@code filename} is NOT redundant: it remains the statement's identity and the source of
     * its generated class name. Null here means an older config that still keeps its text on disk.
     */
    private String sql;

    public SqlStatement() {
    }

    public SqlStatement(int index, String filename, String createClass, String turnCursorsIntoRecords,
                        List<SqlParam> params) {
        this.index = index;
        this.filename = filename;
        this.createClass = createClass;
        this.turnCursorsIntoRecords = turnCursorsIntoRecords;
        this.params = (params != null) ? params : new ArrayList<>();
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getCreateClass() {
        return createClass;
    }

    public void setCreateClass(String createClass) {
        this.createClass = createClass;
    }

    public String getTurnCursorsIntoRecords() {
        return turnCursorsIntoRecords;
    }

    public void setTurnCursorsIntoRecords(String turnCursorsIntoRecords) {
        this.turnCursorsIntoRecords = turnCursorsIntoRecords;
    }

    public List<SqlParam> getParams() {
        return params;
    }

    public void setParams(List<SqlParam> params) {
        this.params = (params != null) ? params : new ArrayList<>();
    }


    /**
     * The MCP tool description an author supplied for this statement's tool, or null to use the one the
     * generator writes from the data dictionary.
     *
     * <p><b>null and "" mean different things and must stay different.</b> null is "the key is
     * absent", which selects the generated default and keeps a config that predates this feature
     * byte-identical after a load and save; "" is an author deliberately choosing an empty
     * description, which is legal MCP.
     */
    public String getMcpDescription() {
        return mcpDescription;
    }

    public void setMcpDescription(String mcpDescription) {
        this.mcpDescription = mcpDescription;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    /** Emit this statement's keys (and its parameters' keys) into the target Properties. */
    public void toPb2(Properties p) {
        if (filename != null) {
            p.setProperty("SQL_FILENAME_" + index, filename);
        }
        if (createClass != null) {
            p.setProperty("SQL_CREATE_CLASS_" + index, createClass);
        }
        if (turnCursorsIntoRecords != null) {
            p.setProperty("SQL_TURN_CURSORS_INTO_RECORDS_" + index, turnCursorsIntoRecords);
        }
        for (SqlParam sp : params) {
            sp.toPb2(p, index);
        }
        if (mcpDescription != null) {
            p.setProperty("SQL_MCP_DESC_" + index, mcpDescription);
        }
        if (sql != null) {
            // Properties escapes the newlines on the way out and restores them on the way in, so a
            // multi-line statement survives a .pb2 byte-for-byte.
            p.setProperty("SQL_TEXT_" + index, sql);
        }
    }

    public Map<String, Object> toJsonMap() {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        m.put("index", (double) index);
        m.put("filename", filename);
        m.put("createClass", createClass);
        m.put("turnCursorsIntoRecords", turnCursorsIntoRecords);
        List<Object> paramMaps = new ArrayList<>();
        for (SqlParam sp : params) {
            paramMaps.add(sp.toJsonMap());
        }
        m.put("params", paramMaps);
        if (mcpDescription != null) {
            m.put("mcpDescription", mcpDescription);
        }
        if (sql != null) {
            m.put("sql", sql);
        }
        return m;
    }

    @SuppressWarnings("unchecked")
    public static SqlStatement fromJsonMap(Map<String, Object> m) {
        SqlStatement s = new SqlStatement();
        s.index = ((Number) m.get("index")).intValue();
        s.filename = (String) m.get("filename");
        s.createClass = (String) m.get("createClass");
        s.turnCursorsIntoRecords = (String) m.get("turnCursorsIntoRecords");
        s.sql = (String) m.get("sql");
        s.params = new ArrayList<>();
        Object rawParams = m.get("params");
        if (rawParams instanceof List) {
            for (Object o : (List<Object>) rawParams) {
                s.params.add(SqlParam.fromJsonMap((Map<String, Object>) o));
            }
        }
        s.mcpDescription = (String) m.get("mcpDescription");
        return s;
    }
}
