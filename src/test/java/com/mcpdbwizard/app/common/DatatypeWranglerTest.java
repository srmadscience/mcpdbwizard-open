package com.mcpdbwizard.app.common;

import com.mcpdbwizard.pub.ConsoleLog;
import com.mcpdbwizard.pub.CSException;
import com.mcpdbwizard.pub.LogInterface;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit coverage for {@link DatatypeWrangler#getOracletypeCode}, the classifier that maps an
 * Oracle datatype name to the {@code OracleTypes.*} constant used when registering OUT
 * parameters in generated code. Database-free and deterministic.
 * <p>
 * The national-character cases pin down the KLUGE 001 behaviour: {@code NCHAR}/{@code
 * NVARCHAR2}/{@code NCLOB} are mapped onto their non-national JDBC equivalents rather than
 * being rejected as unknown types.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class DatatypeWranglerTest {

    private final LogInterface theLog = new ConsoleLog();
    private final DatatypeWrangler wrangler = new DatatypeWrangler("12.1.0", theLog);

    private String code(String oracleType) throws CSException {
        return wrangler.getOracletypeCode(oracleType, theLog);
    }

    @Test
    void mapsOrdinaryCharacterTypes() throws CSException {
        assertEquals("OracleTypes.VARCHAR", code("VARCHAR2"));
        assertEquals("OracleTypes.CHAR", code("CHAR"));
        assertEquals("OracleTypes.CLOB", code("CLOB"));
    }

    @Test
    void mapsIeeeBinaryFloatTypesToNumeric() throws CSException {
        // IEEE-754 binary floating-point types ride the NUMBER path.
        assertEquals("OracleTypes.NUMERIC", code("BINARY_FLOAT"));
        assertEquals("OracleTypes.NUMERIC", code("BINARY_DOUBLE"));
    }

    @Test
    void mapsPlsqlIntegerSubtypesAndAnsiNumericAliasesToNumeric() throws CSException {
        // PL/SQL integer subtypes and ANSI numeric aliases ride the NUMBER path, in step with
        // SqlUtils.getUnderlyingOracleDatatype and JavaUtils. Previously these threw as unknown.
        assertEquals("OracleTypes.NUMERIC", code("PLS_INTEGER"));
        assertEquals("OracleTypes.NUMERIC", code("NATURAL"));
        assertEquals("OracleTypes.NUMERIC", code("NATURALN"));
        assertEquals("OracleTypes.NUMERIC", code("POSITIVE"));
        assertEquals("OracleTypes.NUMERIC", code("POSITIVEN"));
        assertEquals("OracleTypes.NUMERIC", code("SIGNTYPE"));
        assertEquals("OracleTypes.NUMERIC", code("DEC"));
        assertEquals("OracleTypes.NUMERIC", code("DECIMAL"));
        assertEquals("OracleTypes.NUMERIC", code("DOUBLE PRECISION"));
        assertEquals("OracleTypes.NUMERIC", code("INTEGER"));
        assertEquals("OracleTypes.NUMERIC", code("INT"));
        assertEquals("OracleTypes.NUMERIC", code("NUMERIC"));
        assertEquals("OracleTypes.NUMERIC", code("REAL"));
        assertEquals("OracleTypes.NUMERIC", code("SMALLINT"));
    }

    @Test
    void mapsNationalCharacterTypesOntoNonNationalEquivalents() throws CSException {
        // KLUGE 001: until form-of-use is applied, the N types reuse the plain JDBC codes.
        assertEquals("OracleTypes.CHAR", code("NCHAR"));
        assertEquals("OracleTypes.CHAR", code("NCHARACTER"));
        assertEquals("OracleTypes.VARCHAR", code("NVARCHAR"));
        assertEquals("OracleTypes.VARCHAR", code("NVARCHAR2"));
        // NCLOB rides the CLOB path (oracle.sql.NCLOB extends oracle.sql.CLOB).
        assertEquals("OracleTypes.CLOB", code("NCLOB"));
    }

    @Test
    void unknownTypeStillThrows() {
        assertThrows(CSException.class, () -> code("SOME_USER_DEFINED_TYPE"));
    }
}
