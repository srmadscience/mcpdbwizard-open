package com.mcpdbwizard.schema;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * One bind parameter of a user SQL statement. Corresponds to the doubly-indexed
 * {@code SQL_PARAM_NAME_<stmt>_<i>} / {@code SQL_PARAM_DATATYPE_<stmt>_<i>} /
 * {@code SQL_PARAM_LINENUMBER_<stmt>_<i>} key family, where {@code <stmt>} is the owning
 * {@link SqlStatement}'s index and {@code <i>} is this parameter's index. The three per-parameter
 * keys are captured independently (each nullable) so any partial set round-trips exactly.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class SqlParam {

    private int index;
    private String name;
    private String datatype;
    private String lineNumber;

    public SqlParam() {
    }

    public SqlParam(int index, String name, String datatype, String lineNumber) {
        this.index = index;
        this.name = name;
        this.datatype = datatype;
        this.lineNumber = lineNumber;
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

    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }

    public String getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(String lineNumber) {
        this.lineNumber = lineNumber;
    }

    /** Emit this parameter's keys into the target Properties under the owning statement index. */
    public void toPb2(Properties p, int stmtIndex) {
        if (name != null) {
            p.setProperty("SQL_PARAM_NAME_" + stmtIndex + "_" + index, name);
        }
        if (datatype != null) {
            p.setProperty("SQL_PARAM_DATATYPE_" + stmtIndex + "_" + index, datatype);
        }
        if (lineNumber != null) {
            p.setProperty("SQL_PARAM_LINENUMBER_" + stmtIndex + "_" + index, lineNumber);
        }
    }

    public Map<String, Object> toJsonMap() {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        m.put("index", (double) index);
        m.put("name", name);
        m.put("datatype", datatype);
        m.put("lineNumber", lineNumber);
        return m;
    }

    public static SqlParam fromJsonMap(Map<String, Object> m) {
        SqlParam sp = new SqlParam();
        sp.index = ((Number) m.get("index")).intValue();
        sp.name = (String) m.get("name");
        sp.datatype = (String) m.get("datatype");
        sp.lineNumber = (String) m.get("lineNumber");
        return sp;
    }
}
