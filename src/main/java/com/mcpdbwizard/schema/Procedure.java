package com.mcpdbwizard.schema;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * One PL/SQL procedure or function selected for generation. Corresponds to the
 * {@code PROC_NAME_<i>} / {@code PROC_USER_<i>} / {@code PROC_PACKAGE_<i>} /
 * {@code PROC_OVERLOAD_<i>} indexed key family in a {@code .pb2} file. A standalone routine
 * carries {@code package == "null"} (the literal string the wizard writes), and an
 * un-overloaded routine carries {@code overload == "null"}.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class Procedure {

    private int index;
    private String name;
    private String user;
    private String pkg;
    private String overload;
    private String mcpDescription;

    public Procedure() {
    }

    public Procedure(int index, String name, String user, String pkg, String overload) {
        this.index = index;
        this.name = name;
        this.user = user;
        this.pkg = pkg;
        this.overload = overload;
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

    public String getPkg() {
        return pkg;
    }

    public void setPkg(String pkg) {
        this.pkg = pkg;
    }

    public String getOverload() {
        return overload;
    }

    public void setOverload(String overload) {
        this.overload = overload;
    }


    /**
     * The MCP tool description an author supplied for this routine's tool, or null to use the one the
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

    /** Emit this procedure's keys into the target Properties. Null fields are left absent. */
    public void toPb2(Properties p) {
        if (name != null) {
            p.setProperty("PROC_NAME_" + index, name);
        }
        if (user != null) {
            p.setProperty("PROC_USER_" + index, user);
        }
        if (pkg != null) {
            p.setProperty("PROC_PACKAGE_" + index, pkg);
        }
        if (overload != null) {
            p.setProperty("PROC_OVERLOAD_" + index, overload);
        }
        if (mcpDescription != null) {
            p.setProperty("PROC_MCP_DESC_" + index, mcpDescription);
        }
    }

    public Map<String, Object> toJsonMap() {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        m.put("index", (double) index);
        m.put("name", name);
        m.put("user", user);
        m.put("pkg", pkg);
        m.put("overload", overload);
        if (mcpDescription != null) {
            m.put("mcpDescription", mcpDescription);
        }
        return m;
    }

    public static Procedure fromJsonMap(Map<String, Object> m) {
        Procedure p = new Procedure();
        p.index = ((Number) m.get("index")).intValue();
        p.name = (String) m.get("name");
        p.user = (String) m.get("user");
        p.pkg = (String) m.get("pkg");
        p.overload = (String) m.get("overload");
        p.mcpDescription = (String) m.get("mcpDescription");
        return p;
    }
}
