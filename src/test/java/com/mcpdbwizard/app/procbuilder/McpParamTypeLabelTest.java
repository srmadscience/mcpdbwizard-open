package com.mcpdbwizard.app.procbuilder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PL/SQL tool descriptions name Oracle types, not Java ones.
 *
 * <p>They used to say {@code p_raw (byte[])} and {@code p_date (java.util.Date)} — neither of which
 * tells a caller to send base64 or ISO-8601 — and {@code p_out (oracle.sql.json.OracleJsonValue)},
 * which says nothing except which language this was written in. The table descriptions always named
 * Oracle types. The {@code inputSchema} always carried the right JSON type, so this was redundant
 * as well as misleading, which is the worst combination.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class McpParamTypeLabelTest {

    @Test
    void aCrossingFormatIsSpeltOutWhereItIsNotObvious() {
        // The whole point: "RAW" does not tell a caller to send base64, and "DATE" does not tell
        // them the format.
        assertEquals("RAW, base64", SAAdminWrangler.mcpParamTypeLabel("RAW", "byte[]"));
        assertEquals("DATE, ISO-8601 string",
                SAAdminWrangler.mcpParamTypeLabel("DATE", "java.util.Date"));
        // ...but a note the Oracle name already carries is suppressed: "JSON, JSON value" is
        // noise where "RAW, base64" is the whole point.
        assertEquals("JSON",
                SAAdminWrangler.mcpParamTypeLabel("JSON", "oracle.sql.json.OracleJsonValue"));
        assertEquals("VECTOR, array of numbers",
                SAAdminWrangler.mcpParamTypeLabel("VECTOR", "double[]"));
    }

    @Test
    void anObviousTypeIsLeftAlone() {
        // NUMBER crosses as a JSON number and VARCHAR2 as a string; a note there would be noise.
        assertEquals("NUMBER", SAAdminWrangler.mcpParamTypeLabel("NUMBER", "java.math.BigDecimal"));
        assertEquals("VARCHAR2", SAAdminWrangler.mcpParamTypeLabel("VARCHAR2", "String"));
        assertEquals("BOOLEAN", SAAdminWrangler.mcpParamTypeLabel("BOOLEAN", "Boolean"));
    }

    @Test
    void theOracleNameIsPreferredOverAnyNormalisedForm() {
        // ROWID, TIMESTAMP and INTERVAL all read/write through String accessors, and the old code
        // called every one of them "String". The dictionary spelling is what a caller can look up.
        assertEquals("ROWID", SAAdminWrangler.mcpParamTypeLabel("ROWID", "String"));
        assertEquals("TIMESTAMP", SAAdminWrangler.mcpParamTypeLabel("TIMESTAMP", "String"));
        assertEquals("INTERVAL DAY TO SECOND",
                SAAdminWrangler.mcpParamTypeLabel("INTERVAL DAY TO SECOND", "String"));
    }

    @Test
    void aMissingDictionaryNameFallsBackUsefullyRatherThanToNothing() {
        // Synthesised entries can arrive without a raw type name. The crossing note is still worth
        // more than a Java class name; with neither, the Java type is better than an empty string.
        assertEquals("base64", SAAdminWrangler.mcpParamTypeLabel("", "byte[]"));
        assertEquals("ISO-8601 string", SAAdminWrangler.mcpParamTypeLabel(null, "java.util.Date"));
        assertEquals("String", SAAdminWrangler.mcpParamTypeLabel("", "String"));
        // With no dictionary name the JSON note is all there is, and it still beats the class name.
        assertEquals("JSON value",
                SAAdminWrangler.mcpParamTypeLabel("", "oracle.sql.json.OracleJsonValue"));
    }

    @Test
    void surroundingSpaceInTheDictionaryNameIsTrimmed() {
        assertEquals("RAW, base64", SAAdminWrangler.mcpParamTypeLabel("  RAW  ", "byte[]"));
    }
}
