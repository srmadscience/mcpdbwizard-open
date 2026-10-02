package com.mcpdbwizard.schema;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * One MCP context parameter a config declares: a name a client may supply on the connection URL,
 * which the generated server places in {@code SYS_CONTEXT('MCP', <name>)} for every tool call.
 * Corresponds to the {@code MCP_CONTEXT_PARAM_<i>} key family in a {@code .pb2} file. The rules
 * for a name are in {@link com.mcpdbwizard.pub.McpContextParams}.
 *
 * <p>The {@code index} is the positional {@code <i>}, carried through so {@link #toPb2(Properties)}
 * reproduces the original key exactly, gaps and all — the same contract as {@link Sequence}.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class McpContextParam {

    private int index;
    private String name;

    public McpContextParam() {
    }

    public McpContextParam(int index, String name) {
        this.index = index;
        this.name = name;
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

    /** Emit this parameter's key into the target Properties. A null name is left absent. */
    public void toPb2(Properties p) {
        if (name != null) {
            p.setProperty(com.mcpdbwizard.pub.McpContextParams.PB2_KEY_PREFIX + index, name);
        }
    }

    public Map<String, Object> toJsonMap() {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        m.put("index", (double) index);
        m.put("name", name);
        return m;
    }

    public static McpContextParam fromJsonMap(Map<String, Object> m) {
        McpContextParam p = new McpContextParam();
        p.index = ((Number) m.get("index")).intValue();
        p.name = (String) m.get("name");
        return p;
    }
}
