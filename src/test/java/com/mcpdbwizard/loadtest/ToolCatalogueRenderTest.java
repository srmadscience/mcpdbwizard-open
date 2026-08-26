package com.mcpdbwizard.loadtest;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The listing that turns "call any published tool" into copy-and-paste, and the check that stops a
 * typo being reported as a broken server.
 *
 * <p>No network: a {@link McpSchema.Tool} is built by hand from the shape a generated server
 * publishes, which is exactly what {@code tools/list} returns.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class ToolCatalogueRenderTest {

    private static McpSchema.Tool tool(String theName, Map<String, Object> theProperties,
            List<String> theRequired) {
        Map<String, Object> theSchema = new LinkedHashMap<String, Object>();
        theSchema.put("type", "object");
        theSchema.put("properties", theProperties);
        theSchema.put("required", theRequired);
        return McpSchema.Tool.builder().name(theName).description("Call " + theName)
                .inputSchema(theSchema).build();
    }

    private static Map<String, Object> property(String theType, String theDescription) {
        Map<String, Object> theProperty = new LinkedHashMap<String, Object>();
        theProperty.put("type", theType);
        if (theDescription != null) {
            theProperty.put("description", theDescription);
        }
        return theProperty;
    }

    private static ToolCatalogue employeeCatalogue() {
        Map<String, Object> theProperties = new LinkedHashMap<String, Object>();
        theProperties.put("p_empno", property("integer", "Employee number"));
        theProperties.put("p_name", property("string", null));
        theProperties.put("p_active", property("boolean", null));
        return new ToolCatalogue(List.of(
                tool("get_employee", theProperties, List.of("p_empno")),
                tool("job_id_nextval", Map.of(), List.of())));
    }

    @Test
    void everyToolIsListedWithItsParametersAndWhichAreRequired() {
        String theListing = employeeCatalogue().render();
        assertTrue(theListing.contains("2 tool(s) published"), theListing);
        assertTrue(theListing.contains("get_employee"), theListing);
        assertTrue(theListing.contains("p_empno : integer (required)"), theListing);
        assertTrue(theListing.contains("Employee number"), theListing);
        assertTrue(theListing.contains("p_name : string"), theListing);
    }

    @Test
    void aToolWithNoParametersSaysSoRatherThanShowingNothing() {
        assertTrue(employeeCatalogue().render().contains("(no parameters)"));
    }

    /**
     * The skeleton is the point of {@code --list}: it must be a workload entry that
     * {@link Workload} will actually parse, or the copy-and-paste fails on the first attempt.
     */
    @Test
    void theSkeletonEntryIsValidWorkloadJson() throws Exception {
        Map<String, Object> theProperties = new LinkedHashMap<String, Object>();
        theProperties.put("p_empno", property("integer", null));
        theProperties.put("p_name", property("string", null));
        theProperties.put("p_active", property("boolean", null));
        String theSkeleton = ToolCatalogue.skeletonEntry(tool("get_employee", theProperties,
                List.of("p_empno")));

        Workload theWorkload = Workload.fromJson("[" + theSkeleton + "]");
        WorkloadEntry theEntry = theWorkload.entries().get(0);
        assertEquals("get_employee", theEntry.tool());
        assertEquals(3, theEntry.rawArgs().size());
        assertEquals(Integer.valueOf(0), theEntry.rawArgs().get("p_empno"));
        assertEquals("CHANGE_ME", theEntry.rawArgs().get("p_name"));
        assertEquals(Boolean.FALSE, theEntry.rawArgs().get("p_active"));
    }

    @Test
    void aToolWithNoParametersGetsASkeletonWithNoArgs() throws Exception {
        String theSkeleton = ToolCatalogue.skeletonEntry(tool("job_id_nextval", Map.of(), List.of()));
        assertTrue(theSkeleton.contains("\"tool\": \"job_id_nextval\""), theSkeleton);
        assertEquals(0, Workload.fromJson("[" + theSkeleton + "]").entries().get(0)
                .rawArgs().size());
    }

    @Test
    void missingNamesTheToolsTheServerDoesNotPublish() {
        assertEquals(List.of("get_emplyee"),
                employeeCatalogue().missing(List.of("get_emplyee", "job_id_nextval")));
        assertTrue(employeeCatalogue().missing(List.of("job_id_nextval")).isEmpty());
    }

    /** A near miss is what a typo produces, so the rejection should offer the real name. */
    @Test
    void aNearMissSuggestsTheRealName() {
        assertTrue(employeeCatalogue().nearestTo("employee").contains("get_employee"));
    }

    @Test
    void aToolWithNoInputSchemaAtAllStillRenders() {
        ToolCatalogue theCatalogue = new ToolCatalogue(List.of(
                McpSchema.Tool.builder().name("bare").build()));
        assertTrue(theCatalogue.render().contains("(no parameters)"));
        assertTrue(ToolCatalogue.skeletonEntry(theCatalogue.tools().get(0))
                .contains("\"tool\": \"bare\""));
    }
}
