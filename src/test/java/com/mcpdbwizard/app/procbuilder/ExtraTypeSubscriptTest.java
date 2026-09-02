package com.mcpdbwizard.app.procbuilder;

import com.mcpdbwizard.app.common.PlsqlRecordObject;
import com.mcpdbwizard.pub.JulLog;
import com.mcpdbwizard.pub.LogInterface;
import com.mcpdbwizard.pub.ReadOnlyRowSet;
import com.mcpdbwizard.pub.SqlUtils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The DESTINATION subscript of a record collection's copy-out, pinned without a database.
 * <p>
 * A PL/SQL index-by table is keyed by arbitrary {@code BINARY_INTEGER}s; the shadow SQL nested
 * table the generator copies it into is keyed {@code 1..COUNT}. Indexing the second with the first
 * raised {@code ORA-06532} for a base-0 or negative key and {@code ORA-06533} for a sparse one — a
 * defect that reached a customer and was fixed on 2026-09-02.
 * <p>
 * <b>Why a second guard, when {@code TIndexByRecordKeys} already covers this.</b> That harness needs
 * a live Oracle, a provisioned {@code GENERIC_TESTE}, a regenerated tree and its shadow types; it is
 * skipped by Assumption whenever any of those is missing, and skipping looks exactly like passing in
 * a summary line. This one needs none of them, so it runs on every {@code mvn test} anywhere — and
 * it pins the invariant at the point the bug actually lived, which is the template rather than any
 * particular fixture. The two fail for different reasons and that is the point of having both.
 *
 * @author  devteam@mcpdbwizard.com
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class ExtraTypeSubscriptTest {

    // NOT ConsoleLog: on an error it prints "Press Enter to continue..." and BLOCKS on stdin,
    // which in a Surefire fork hangs the build until the timeout rather than failing it.
    private static final LogInterface LOG = new JulLog("ExtraTypeSubscriptTest");

    /**
     * The three columns {@link ExtraType} reads off a record's argument rows, as an in-memory
     * rowset: one NUMBER, one VARCHAR2 and one DATE, which is enough shape for the template.
     */
    private static ReadOnlyRowSet fieldRows() {
        Object[] rows = new Object[] {
                new Object[] { "ID",    "NUMBER",    new java.math.BigDecimal(22) },
                new Object[] { "LABEL", "VARCHAR2",  new java.math.BigDecimal(32) },
                new Object[] { "WHEN",  "DATE",      new java.math.BigDecimal(7)  },
        };
        return new ReadOnlyRowSet(rows,
                new String[] { "ARGUMENT_NAME", "DATA_TYPE", "DATA_LENGTH" },
                new String[] { "VARCHAR2", "VARCHAR2", "NUMBER" },
                new int[] { SqlUtils.ORACLE_TEXT_DATATYPE, SqlUtils.ORACLE_TEXT_DATATYPE,
                            SqlUtils.ORACLE_NUMBER_DATATYPE },
                new int[] { 0, 0, 0 },
                new long[] { 30, 30, 22 },
                new int[] { 0, 0, 0 },
                LOG, null, false);
    }

    private static ExtraType buildExtraType() {
        PlsqlRecordObject element = new PlsqlRecordObject("GENERIC_TESTE", "ROW_T", 0);
        element.oracleName = "GENERIC_TESTE.IDXBY_KEYS.ROW_T";
        element.argRowSet = fieldRows();

        PlsqlRecordObject collection = new PlsqlRecordObject("GENERIC_TESTE", "ROW_IBY", 0);
        collection.oracleName = "GENERIC_TESTE.IDXBY_KEYS.ROW_IBY";
        collection.typeImplementingClass = element;

        return new ExtraType("OSOFT", collection, LOG, 0, new ArrayList());
    }

    /** The whole unassign block as one string, which is what the emitters splice into the call. */
    private static String unassign() {
        StringBuilder b = new StringBuilder();
        for (String line : buildExtraType().getUnassignStatement()) {
            b.append(line).append('\n');
        }
        return b.toString();
    }

    /**
     * THE regression. The shadow array must be indexed by its own LAST — the row the emitters have
     * just EXTENDed — and never by {@code i}, which is the SOURCE collection's key.
     */
    @Test
    void unassignIndexesTheShadowArrayByItsOwnLast() {
        String sql = unassign();
        assertTrue(sql.contains(ExtraType.PARAM_TARGET_PARAM_ARRAY_NAME + "("
                        + ExtraType.PARAM_TARGET_PARAM_ARRAY_NAME + ".LAST) :="),
                "the shadow array must be assigned at its own LAST, but the template reads:\n" + sql);
        assertFalse(sql.contains(ExtraType.PARAM_TARGET_PARAM_ARRAY_NAME + "(i)"),
                "the shadow array must NEVER be indexed by the source key i -- that is ORA-06532 "
                        + "for a base-0 or negative key and ORA-06533 for a sparse one:\n" + sql);
    }

    /**
     * The SOURCE side keeps {@code (i)}: it is the index-by's own key and the only way to read the
     * element. A fix that renamed both sides would pass the assertion above and read nothing.
     */
    /**
     * The type carries {@link ExtraType#POSITION_ATTRIBUTE} as its LAST attribute, so every existing
     * COL_n keeps its index, and the constructor passes one value per attribute in the same order.
     * These two move together or the arity disagrees at bind time rather than at compile time.
     */
    @Test
    void theTypeAndItsConstructorAgreeOnThePositionAttribute() {
        StringBuilder ddl = new StringBuilder();
        for (String line : buildExtraType().getCreateStatement()) {
            ddl.append(line).append('\n');
        }
        assertTrue(ddl.toString().contains(" ," + ExtraType.POSITION_ATTRIBUTE + " NUMBER);"),
                "the position attribute must close the type declaration:\n" + ddl);

        String[] lines = buildExtraType().getUnassignStatement();
        assertTrue(lines[lines.length - 1].endsWith("," + ExtraType.PARAM_TARGET_SOURCE_INDEX + ");"),
                "the constructor's last argument must be the source subscript, but reads: "
                        + lines[lines.length - 1]);
    }

    /**
     * The position attribute goes in as its own DDL line and never as a newline inside one.
     * createExtraTypeObjects() emits each line as a Java string literal, so an embedded newline
     * makes the generated ServiceImpl fail with "unclosed string literal" -- a compile error in
     * emitted code, far from anything that names the cause.
     */
    @Test
    void theDdlHasNoEmbeddedNewlines() {
        for (String line : buildExtraType().getCreateStatement()) {
            assertFalse(line.contains("\n"), "DDL line carries an embedded newline: " + line);
        }
    }

    @Test
    void unassignStillReadsTheSourceByItsOwnKey() {
        String sql = unassign();
        assertTrue(sql.contains(ExtraType.PARAM_TARGET_PARAM_NAME + "(i).ID"),
                "the source record's fields must still be read at (i):\n" + sql);
        assertTrue(sql.contains(ExtraType.PARAM_TARGET_PARAM_NAME + "(i).LABEL"),
                "the source record's fields must still be read at (i):\n" + sql);
    }

    /**
     * One constructor argument per field, in declaration order. This is what makes the shadow type's
     * arity match its CREATE, and it is the half a future MCPDBWIZARD_POS attribute has to keep
     * true on both sides at once.
     */
    @Test
    void unassignPassesEveryFieldInOrder() {
        String[] lines = buildExtraType().getUnassignStatement();
        assertEquals(4, lines.length,
                "expected one header line plus one per field, got:\n" + String.join("\n", lines));
        assertTrue(lines[1].endsWith(".ID"), lines[1]);
        assertTrue(lines[2].endsWith(".LABEL"), lines[2]);
        assertTrue(lines[3].startsWith(" ,") && lines[3].contains(".WHEN"), lines[3]);
    }

    /**
     * The INBOUND half is deliberately unchanged: there the index-by is the target, and it accepts
     * any subscript, so {@code (i)} on both sides is correct. Pinned so that a later symmetry
     * argument does not "fix" the direction that was never broken.
     */
    @Test
    void assignKeepsTheSourceKeyOnBothSides() {
        StringBuilder b = new StringBuilder();
        for (String line : buildExtraType().getAssignStatement()) {
            b.append(line).append('\n');
        }
        String sql = b.toString();
        // The TARGET subscript is a token, because the two inbound arms answer it differently: the
        // index-by arm substitutes the carried position, the nested-table arm substitutes i. The
        // SOURCE is always read at (i).
        assertTrue(sql.contains(ExtraType.PARAM_TARGET_PARAM_NAME + "("
                        + ExtraType.PARAM_TARGET_TARGET_INDEX + ").ID"), sql);
        assertTrue(sql.contains(ExtraType.PARAM_TARGET_PARAM_ARRAY_NAME + "(i).COL_0"), sql);
        assertFalse(sql.contains(".LAST"),
                "the inbound half writes into a collection the caller chose the subscript for -- it "
                        + "must not acquire a LAST:\n" + sql);
    }
}
