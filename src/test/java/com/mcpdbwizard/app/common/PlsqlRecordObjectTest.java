package com.mcpdbwizard.app.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PlsqlRecordObject}'s two comparators, {@code plsqlEquals}
 * and {@code plsqlEqualsByArgs}. No database connection is required.
 *
 * <p>The point of these tests is the <b>null-safety</b> of the comparators: they run
 * inside the generator's duplicate-elimination pass, and a single NPE there is
 * swallowed by an outer catch and silently aborts the whole pass (historically this
 * doubled the generated file count when a synthesised record header had a null
 * {@code procArgName}). The comparators must therefore never throw on a null field —
 * they compare via {@link java.util.Objects#equals}, treating two nulls as equal.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class PlsqlRecordObjectTest {

    private static PlsqlRecordObject rec(String procOwner, String procObjectName,
                                         String procPackageName, String procArgName, String procOverload) {
        // owner/objectName must be non-null for the SingleNamespaceObject constructor.
        PlsqlRecordObject r = new PlsqlRecordObject("OWNER", "OBJECT", 0);
        r.procOwner = procOwner;
        r.procObjectName = procObjectName;
        r.procPackageName = procPackageName;
        r.procArgName = procArgName;
        r.procOverload = procOverload;
        return r;
    }

    // ---- plsqlEqualsByArgs -------------------------------------------------

    @Test
    void equalsByArgs_identicalNonNullFields_match() {
        PlsqlRecordObject a = rec("APPSCHEMA", "MYPROC", "MYPKG", "P_ARG", "1");
        PlsqlRecordObject b = rec("APPSCHEMA", "MYPROC", "MYPKG", "P_ARG", "1");
        assertTrue(a.plsqlEqualsByArgs(b));
        assertTrue(b.plsqlEqualsByArgs(a));
    }

    @Test
    void equalsByArgs_differingArgName_doNotMatch() {
        PlsqlRecordObject a = rec("APPSCHEMA", "MYPROC", "MYPKG", "P_ARG", "1");
        PlsqlRecordObject b = rec("APPSCHEMA", "MYPROC", "MYPKG", "P_OTHER", "1");
        assertFalse(a.plsqlEqualsByArgs(b));
    }

    @Test
    void equalsByArgs_nullArgNameOnOneSide_doesNotThrowAndDiffers() {
        PlsqlRecordObject withName = rec("APPSCHEMA", "MYPROC", null, "P_ARG", null);
        PlsqlRecordObject nullName = rec("APPSCHEMA", "MYPROC", null, null, null);
        // The historical NPE culprit: procArgName.equals(...) on a null procArgName.
        assertFalse(withName.plsqlEqualsByArgs(nullName));
        assertFalse(nullName.plsqlEqualsByArgs(withName));
    }

    @Test
    void equalsByArgs_bothArgNamesNull_matchWhenOtherFieldsEqual() {
        PlsqlRecordObject a = rec("APPSCHEMA", "MYPROC", null, null, null);
        PlsqlRecordObject b = rec("APPSCHEMA", "MYPROC", null, null, null);
        assertTrue(a.plsqlEqualsByArgs(b));
    }

    @Test
    void equalsByArgs_allFieldsNull_doNotThrow() {
        PlsqlRecordObject a = rec(null, null, null, null, null);
        PlsqlRecordObject b = rec(null, null, null, null, null);
        assertTrue(a.plsqlEqualsByArgs(b));
    }

    @Test
    void equalsByArgs_nullOther_isFalseNotThrow() {
        PlsqlRecordObject a = rec("APPSCHEMA", "MYPROC", "MYPKG", "P_ARG", "1");
        assertFalse(a.plsqlEqualsByArgs(null));
    }

    // ---- plsqlEquals ------------------------------------------------------

    @Test
    void equals_identicalOwnerAndJavaName_match() {
        PlsqlRecordObject a = new PlsqlRecordObject("OWNER", "OBJECT", 0);
        PlsqlRecordObject b = new PlsqlRecordObject("OWNER", "OBJECT", 0);
        a.realOwner = "APPSCHEMA";
        a.fixedJavaName = "MyRecord";
        b.realOwner = "APPSCHEMA";
        b.fixedJavaName = "MyRecord";
        assertTrue(a.plsqlEquals(b));
    }

    @Test
    void equals_nullJavaNameOnOneSide_doesNotThrowAndDiffers() {
        PlsqlRecordObject a = new PlsqlRecordObject("OWNER", "OBJECT", 0);
        PlsqlRecordObject b = new PlsqlRecordObject("OWNER", "OBJECT", 0);
        a.realOwner = "APPSCHEMA";
        a.fixedJavaName = "MyRecord";
        b.realOwner = "APPSCHEMA";
        // b.fixedJavaName left null (its declared default).
        assertFalse(a.plsqlEquals(b));
        assertFalse(b.plsqlEquals(a));
    }

    @Test
    void equals_bothFieldsNull_doNotThrow() {
        // realOwner / fixedJavaName both default to null after construction.
        PlsqlRecordObject a = new PlsqlRecordObject("OWNER", "OBJECT", 0);
        PlsqlRecordObject b = new PlsqlRecordObject("OWNER", "OBJECT", 0);
        assertTrue(a.plsqlEquals(b));
    }

    @Test
    void equals_nullOther_isFalseNotThrow() {
        PlsqlRecordObject a = new PlsqlRecordObject("OWNER", "OBJECT", 0);
        a.realOwner = "APPSCHEMA";
        a.fixedJavaName = "MyRecord";
        assertFalse(a.plsqlEquals(null));
    }
}
