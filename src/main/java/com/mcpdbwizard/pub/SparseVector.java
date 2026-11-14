package com.mcpdbwizard.pub;

/**
 * A sparse Oracle {@code VECTOR} value (23ai): a high-dimensional vector stored as its non-zero
 * entries only. Distinct from a dense {@code VECTOR} (read/written as {@code double[]}) and a binary
 * {@code VECTOR} (read/written as {@code byte[]}) — a sparse vector of a million dimensions with a
 * handful of non-zeros must NOT be densified to a million-element array, so it carries its own
 * {@code {length, indices, values}} shape.
 *
 * <ul>
 *   <li>{@link #length} — the total number of dimensions.</li>
 *   <li>{@link #indices} — the positions (0-based, ascending) of the non-zero entries.</li>
 *   <li>{@link #values} — the non-zero values; {@code values[k]} lives at {@code indices[k]}.</li>
 * </ul>
 *
 * <p>Read from a sparse column/param via {@link ReadOnlyRowSet#getVectorSparse(String)}; written via
 * {@link WriteableRowSet#setVectorSparse(String, SparseVector)} /
 * {@code StatementParameters2.setVectorSparseParam}. The value carries only {@code double} magnitudes
 * — it binds as a FLOAT64 sparse vector and Oracle coerces to the column/param element format
 * (a FLOAT32 column keeps the value; an INT8 column rounds, as it must).
 *
 * <p>Fields are public to match the field-visibility JSON mapping the generated MCP server uses, so a
 * sparse vector round-trips as a {@code {"length":…, "indices":[…], "values":[…]}} object with no
 * extra mapping.
 *
 * <p>Plain data, with no Oracle type in any signature: a PostgreSQL server (mcpdbwizard-pg) crosses a
 * pgvector {@code sparsevec} as one, without ojdbc on its classpath, and Jackson introspects every
 * method of a class it maps, so one {@code oracle.sql.VECTOR} parameter was a NoClassDefFoundError.
 * The conversions to and from {@code oracle.sql.VECTOR} are in {@link OracleSparseVectors}, and need
 * ojdbc 23.26+.
 *
 * @since Oracle 23ai
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class SparseVector {

    /** Total number of dimensions. */
    public int length;

    /** Positions (0-based, ascending) of the non-zero entries. */
    public int[] indices;

    /** The non-zero values; {@code values[k]} lives at {@code indices[k]}. */
    public double[] values;

    /** No-arg constructor (for the JSON mapper). */
    public SparseVector() {
    }

    public SparseVector(int length, int[] indices, double[] values) {
        this.length = length;
        this.indices = indices;
        this.values = values;
    }

    /**
     * The densified form: a {@code double[]} of {@link #length} with {@link #values} placed at
     * {@link #indices} and zeros elsewhere. (Convenience for callers that want the full vector;
     * avoid for very high-dimensional sparse vectors.)
     */
    public double[] toDenseArray() {
        double[] theDense = new double[length];
        if (indices != null) {
            for (int i = 0; i < indices.length; i++) {
                theDense[indices[i]] = values[i];
            }
        }
        return (theDense);
    }

    @Override
    public String toString() {
        return ("SparseVector{length=" + length
                + ", indices=" + java.util.Arrays.toString(indices)
                + ", values=" + java.util.Arrays.toString(values) + "}");
    }
}
