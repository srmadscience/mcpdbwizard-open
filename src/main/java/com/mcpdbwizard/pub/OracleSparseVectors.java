package com.mcpdbwizard.pub;

/**
 * A {@link SparseVector} to and from Oracle's sparse {@code oracle.sql.VECTOR} (23ai). Kept out of
 * {@code SparseVector} itself so that class names no Oracle type: a PostgreSQL server maps it with
 * Jackson and does not have ojdbc. Needs ojdbc 23.26+ (the sparse {@code oracle.sql.VECTOR}
 * accessors).
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class OracleSparseVectors {

    private OracleSparseVectors() {
    }

    /**
     * Build a {@code SparseVector} from a sparse {@code oracle.sql.VECTOR} (its
     * {@code toSparseDoubleArray()} view). Returns {@code null} for a {@code null} input.
     *
     * @throws java.sql.SQLException if the VECTOR cannot be read as a sparse double array
     */
    public static SparseVector fromVector(oracle.sql.VECTOR theVector) throws java.sql.SQLException {
        if (theVector == null) {
            return (null);
        }
        oracle.sql.VECTOR.SparseDoubleArray theSparse = theVector.toSparseDoubleArray();
        return (new SparseVector(theSparse.length(), theSparse.indices(), theSparse.values()));
    }

    /**
     * Turn {@code theVector} into a bindable {@code oracle.sql.VECTOR} (a FLOAT64 sparse vector —
     * Oracle coerces to the target column/param element format).
     *
     * @throws java.sql.SQLException if the sparse vector cannot be constructed
     */
    public static oracle.sql.VECTOR toVector(SparseVector theVector) throws java.sql.SQLException {
        return (oracle.sql.VECTOR.ofFloat64Values(
                oracle.sql.VECTOR.SparseDoubleArray.of(theVector.length, theVector.indices, theVector.values)));
    }
}
