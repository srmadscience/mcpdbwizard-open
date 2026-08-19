package com.mcpdbwizard.app.common;

import com.mcpdbwizard.pub.*;
import com.mcpdbwizard.app.procbuilder.*;

/**
 * @author devteam@mcpdbwizard.com
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 * @version 2
 */
public class PlsqlRecordObject extends SingleNamespaceObject {
    // PK of all_arguments...
    public String procOwner = null;
    public String procObjectName = null;
    public String procPackageName = null;
    public String procOverload = null;
    public String procArgName = null;

    public boolean usable = true;

    // Seqno of arg
    public int procSequence = Integer.MIN_VALUE;

    // position of arg
    public int procPosition = Integer.MIN_VALUE;

    // Data level of arg
    public int procDataLevel = Integer.MIN_VALUE;

    public String inOut = null;
    //
    public int replacedByArrayId = Integer.MIN_VALUE;

    public int fieldCount = Integer.MIN_VALUE;

    public int functionId = Integer.MIN_VALUE;

    public ReadOnlyRowSet argRowSet = null;
    public CallableStatementParameterEngine theEngine = null;

    // ARRAY fields
    public String dataType = null;
    public String typeOwner = null;
    public String typeName = null;
    public String typeSubName = null;
    public PlsqlRecordObject typeImplementingClass = null;

    public String typeRecordClass = "Object";

    // VARRAY Field
    public int varrayLength = Integer.MIN_VALUE;


    // PL Sql Packahe Oracle Type Name
// if this field is non-null we flag this as needing type genertion
    public String generatedGenericTypeName = null;

    public String[] plsqlPackAssign;
    public String[] plsqlPackWeirdAssign;
    public String[] plsqlPackUnassign;
    public PlsqlLiteParser theParser;

    public boolean isWeirdpackageArrayOfOracleType = false;

    // Oracle 23ai: a table/view %ROWTYPE parameter is reported by ALL_ARGUMENTS with TYPE_NAME = the
    // base table (12c left TYPE_* null and forced source parsing), so the record classifies as an
    // ORACLE_TYPE_RECORD even though the generated PL/SQL declaration must say "owner.table%ROWTYPE"
    // (a bare "owner.table" is not a type -> PLS-00488). Set when the field synthesis resolved the
    // fields from ALL_TAB_COLUMNS; consulted where the PL/SQL block declares the variable.
    public boolean isTableRowtype = false;

    // The table/view whose columns are this record's fields, when that table cannot be read off
    // realOwner/objectName. Two unrelated cases set it, which is why it is a plain override rather
    // than a flag:
    //   * a STRONG REF CURSOR's row record adopted on a server whose ALL_ARGUMENTS does not expand
    //     the cursor into child rows (19c/21c/23ai — SAAdminWrangler.adoptRefCursorRowType). It
    //     cannot be carried in objectName: that already holds the dotted cursor-type name
    //     ("REFCURSOR_TEST.ora_datatypes_typ") which gives the class its 12c-matching Java name;
    //   * the element record of a package collection of a table %ROWTYPE
    //     ("TYPE t IS TABLE OF tab%ROWTYPE"), where objectName holds the PROCEDURE name the element
    //     was reached through, so the field synthesis would otherwise look for a table by that name.
    // Null on every other record, where the field synthesis keeps using realOwner/objectName exactly
    // as before.
    public String rowtypeTableOwner = null;
    public String rowtypeTableName = null;

    // The base table's columns, for a collection element whose fields cannot be recovered from
    // argRowSet. ExtraTypeWrangler builds the shadow Oracle TYPE from the ELEMENT object, and that is
    // a DIFFERENT instance from the entry the field synthesis populates -- getChildRecord mints a
    // fresh one per collection, so several collections of one %ROWTYPE hold several instances that
    // merely share an identity. Without this the generated Java class comes out correct while
    // extraObjects.sql still emits "CREATE OR REPLACE TYPE x_T AS OBJECT" with no attribute list,
    // which Oracle accepts and holds INVALID. Null everywhere else.
    public ReadOnlyRowSet rowtypeFieldRows = null;

    // TRUE only for a row record built by adoptRefCursorRowType. Distinguishes that case from any
    // other record carrying a rowtypeTable* override, which the two used to be conflated by: 12c
    // builds a cursor's row record from the cursor's own expansion rather than from a table, so an
    // adopted row must NOT be flagged isTableRowtype, while a collection element of tab%ROWTYPE
    // genuinely is one and must be.
    public boolean adoptedCursorRow = false;

    public PlsqlRecordObject(String owner, String objectName, int objectType) {
        super(owner, objectName, objectType);
    }

    // Both comparators are NULL-SAFE on every field (java.util.Objects.equals treats two nulls as equal
    // and never dereferences). This is defensive: these run inside the duplicate-elimination pass, and a
    // single NPE here (historically a null procArgName on a synthesised record header) is swallowed by the
    // engine's outer catch and ABORTS the whole dedup pass, doubling the generated file count. The upstream
    // fix gives such headers a non-null synthetic ARGUMENT_NAME (see SqlStatementDictionary); guarding here
    // as well means a stray null can never silently re-break dedup. "both null" comparing equal matches the
    // pre-existing explicit null handling for procPackageName / procOverload.
    public boolean plsqlEquals(PlsqlRecordObject other) {
        if (other == null) {
            return false;
        }

        return java.util.Objects.equals(this.realOwner, other.realOwner)
                && java.util.Objects.equals(this.fixedJavaName, other.fixedJavaName);
    }

    public boolean plsqlEqualsByArgs(PlsqlRecordObject other) {
        if (other == null) {
            return false;
        }

        return java.util.Objects.equals(this.procOwner, other.procOwner)
                && java.util.Objects.equals(this.procObjectName, other.procObjectName)
                && java.util.Objects.equals(this.procPackageName, other.procPackageName)
                && java.util.Objects.equals(this.procArgName, other.procArgName)
                && java.util.Objects.equals(this.procOverload, other.procOverload);
    }


}


