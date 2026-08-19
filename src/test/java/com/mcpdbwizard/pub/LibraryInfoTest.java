package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LibraryInfo}.
 *
 * <p>The product strings now come from {@link Namer} rather than from {@code PARAM_*} tokens
 * rewritten by a release-time {@code sed} step. Two consequences for these tests:
 *
 * <ul>
 *   <li>{@code getProductVersion()} is no longer a fixed string. Its build component is a
 *       {@code yyyyMMddHHmm} stamp taken when {@code Namer} initialises, so the tests assert
 *       the SHAPE and that it tracks {@code Namer} — not a literal. Pinning a literal is
 *       exactly what made this test fail the moment the value became a timestamp.</li>
 *   <li>{@code getLibraryVersion()} still returns {@code PARAM_DB_VERSION} verbatim.
 *       {@code PARAM_DB_VERSION} is NOT one of the eight branding tokens — it is consumed at
 *       generation time and never reaches generated output — so it was deliberately left
 *       alone, and this test characterises that.</li>
 * </ul>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class LibraryInfoTest {

    @Test
    void getLibraryVersionReturnsTheVersionToken() {
        assertEquals("PARAM_DB_VERSION", LibraryInfo.getLibraryVersion());
    }

    @Test
    void getProductVersionCombinesTheNamerValues() {
        assertEquals(Namer.param_product_version + "." + Namer.param_build,
                LibraryInfo.getProductVersion());
    }

    @Test
    void buildComponentIsAMinutePrecisionTimestamp() {
        String build = LibraryInfo.getProductVersion()
                .substring(Namer.param_product_version.length() + 1);
        assertTrue(build.matches("\\d{12}"),
                "build should be yyyyMMddHHmm (12 digits), was: " + build);

        // Check the FIELD ORDER, not just the length. Java's pattern letters differ from
        // Oracle's -- MM is months, mm is minutes, HH is 24-hour -- so a pattern typo would
        // still yield 12 digits that merely look right. Range-checking each field catches it.
        int year = Integer.parseInt(build.substring(0, 4));
        int month = Integer.parseInt(build.substring(4, 6));
        int day = Integer.parseInt(build.substring(6, 8));
        int hour = Integer.parseInt(build.substring(8, 10));
        int minute = Integer.parseInt(build.substring(10, 12));
        assertTrue(year >= 2026 && year <= 2999, "year: " + year);
        assertTrue(month >= 1 && month <= 12, "month: " + month);
        assertTrue(day >= 1 && day <= 31, "day: " + day);
        assertTrue(hour <= 23, "hour (must be 24-hour): " + hour);
        assertTrue(minute <= 59, "minute: " + minute);
    }
}
