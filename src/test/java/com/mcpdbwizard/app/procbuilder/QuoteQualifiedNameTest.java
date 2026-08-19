package com.mcpdbwizard.app.procbuilder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A qualified Oracle name is quoted per identifier, not as a whole.
 *
 * <p>The sequence query used to wrap the entire name in one pair of quotes, so a sequence in
 * another schema emitted {@code SELECT "SYNUSER.JOB_ID".nextval FROM DUAL} — where
 * {@code "SYNUSER.JOB_ID"} is a single quoted identifier that happens to contain a dot, not
 * schema-dot-object. Oracle answers ORA-02289. Verified against a live database: the emitted form
 * fails, both forms below succeed.
 *
 * <p>It only ever worked when the generator connected AS the sequence's owner, because then the
 * name has no dot — which is why it survived until a config named a second schema.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class QuoteQualifiedNameTest {

    @Test
    void aQualifiedNameQuotesEachPartSeparately() {
        assertEquals("\"SYNUSER\".\"JOB_ID\"",
                SAAdminWrangler.quoteQualifiedName("SYNUSER.JOB_ID"));
    }

    @Test
    void anUnqualifiedNameIsUnchangedFromTheOldBehaviour() {
        // Every config whose sequences live in the connected schema must emit exactly what it did
        // before, which is what keeps this fix byte-neutral for all 35 propfiles.
        assertEquals("\"JOB_ID\"", SAAdminWrangler.quoteQualifiedName("JOB_ID"));
    }

    @Test
    void theSplitTakesTheLastDotSoTheObjectNameStaysWhole() {
        assertEquals("\"A.B\".\"C\"", SAAdminWrangler.quoteQualifiedName("A.B.C"));
    }

    @Test
    void aMalformedNameIsLeftAsOneIdentifierRatherThanProducingBrokenSql() {
        // A leading or trailing dot cannot be split into two usable identifiers. Quoting the whole
        // thing is wrong but INERT -- it fails as "no such object" -- where emitting `"".name` or
        // `name.""` would be a syntax error in every statement the class appears in.
        assertEquals("\".JOB_ID\"", SAAdminWrangler.quoteQualifiedName(".JOB_ID"));
        assertEquals("\"SYNUSER.\"", SAAdminWrangler.quoteQualifiedName("SYNUSER."));
        assertEquals("", SAAdminWrangler.quoteQualifiedName(null));
    }
}
