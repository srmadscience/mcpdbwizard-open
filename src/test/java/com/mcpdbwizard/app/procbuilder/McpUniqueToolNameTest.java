package com.mcpdbwizard.app.procbuilder;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link SAAdminWrangler#mcpUniqueToolName}: two tools of one name kill a generated MCP server at
 * start-up ("Tool with name '...' is already registered"), so a later one is suffixed. The first
 * keeps its name: every name that already worked is unchanged.
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class McpUniqueToolNameTest {

    @Test
    void aStandaloneRoutineAndAPackageMemberOfTheSameNameBothGetATool() {
        Map<String, String> used = new LinkedHashMap<>();
        // standalone PKG_MEMBER, then package PKG's member MEMBER: both display as pkg_member
        assertEquals("pkg_member", SAAdminWrangler.mcpUniqueToolName(used, "pkg_member", "SCOTT.PKG_MEMBER"));
        assertEquals("pkg_member_2", SAAdminWrangler.mcpUniqueToolName(used, "pkg_member", "SCOTT.PKG.MEMBER"));
        assertEquals("pkg_member_3", SAAdminWrangler.mcpUniqueToolName(used, "pkg_member", "SCOTT.PKG2.MEMBER"));
        assertEquals("SCOTT.PKG.MEMBER", used.get("pkg_member_2"));
    }

    @Test
    void aSuffixThatIsItselfTakenIsSkipped() {
        Map<String, String> used = new LinkedHashMap<>();
        SAAdminWrangler.mcpUniqueToolName(used, "a_2", "SCOTT.A_2");
        SAAdminWrangler.mcpUniqueToolName(used, "a", "SCOTT.A");
        assertEquals("a_3", SAAdminWrangler.mcpUniqueToolName(used, "a", "SCOTT.PKG.A"));
    }

    @Test
    void distinctNamesAreUnchanged() {
        Map<String, String> used = new LinkedHashMap<>();
        assertEquals("emp_get_by_pk", SAAdminWrangler.mcpUniqueToolName(used, "emp_get_by_pk", "SCOTT.EMP"));
        assertEquals("dept_get_by_pk", SAAdminWrangler.mcpUniqueToolName(used, "dept_get_by_pk", "SCOTT.DEPT"));
        assertEquals(2, used.size());
    }
}
