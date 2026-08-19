package com.mcpdbwizard.schema;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * One Oracle sequence selected for generation. Corresponds to the {@code SEQUENCE_NAME_<i>} /
 * {@code SEQUENCE_USER_<i>} indexed key family in a {@code .pb2} file. The {@code index} is the
 * positional {@code <i>} carried through so {@link #toPb2(Properties)} reproduces the original key
 * exactly (indices are preserved verbatim, gaps and all).
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class Sequence {

    private int index;
    private String name;
    private String user;
    private String mcpDescription;

    public Sequence() {
    }

    public Sequence(int index, String name, String user) {
        this.index = index;
        this.name = name;
        this.user = user;
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
     * The MCP tool description an author supplied for this sequence's _nextval tool, or null to use the one the
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

    /** Emit this sequence's keys into the target Properties. Null fields are left absent. */
    public void toPb2(Properties p) {
        if (name != null) {
            p.setProperty("SEQUENCE_NAME_" + index, name);
        }
        if (user != null) {
            p.setProperty("SEQUENCE_USER_" + index, user);
        }
        if (mcpDescription != null) {
            p.setProperty("SEQUENCE_MCP_DESC_" + index, mcpDescription);
        }
    }

    public Map<String, Object> toJsonMap() {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        m.put("index", (double) index);
        m.put("name", name);
        m.put("user", user);
        if (mcpDescription != null) {
            m.put("mcpDescription", mcpDescription);
        }
        return m;
    }

    public static Sequence fromJsonMap(Map<String, Object> m) {
        Sequence s = new Sequence();
        s.index = ((Number) m.get("index")).intValue();
        s.name = (String) m.get("name");
        s.user = (String) m.get("user");
        s.mcpDescription = (String) m.get("mcpDescription");
        return s;
    }
}
