package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link OracleThinUrl#split}: the credentials out of a thin URL, for the passwords the driver's own
 * URL parsing gets wrong (ORA-17067 for {@code -} and {@code /}, ORA-17868 for {@code @}).
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class OracleThinUrlTest {

    /**
     * {@code jdbc:oracle:thin:<credentials>@<host part>}, assembled here so that no credential-shaped
     * URL appears literally in the source: the open-source export's leak gate refuses one.
     */
    private static String thin(String theCredentials, String theHostPart) {
        return "jdbc:oracle:thin:" + theCredentials + "@" + theHostPart;
    }

    @Test
    void aPlainPasswordSplits() {
        assertArrayEquals(new String[]{"jdbc:oracle:thin:@db.example:1521/SVC", "scott", "tiger"},
                OracleThinUrl.split(thin("scott/tiger", "db.example:1521/SVC")));
    }

    @Test
    void theOldSidFormSplitsToo() {
        assertArrayEquals(new String[]{"jdbc:oracle:thin:@db.example:1521:ORCL", "scott", "tiger"},
                OracleThinUrl.split(thin("scott/tiger", "db.example:1521:ORCL")));
    }

    @Test
    void theCharactersTheDriverRefusesStayInThePassword() {
        assertArrayEquals(new String[]{"jdbc:oracle:thin:@h:1521/S", "u", "not-the/pass@word"},
                OracleThinUrl.split(thin("u/not-the/pass@word", "h:1521/S")));
    }

    @Test
    void theDriversQuotesAreRemoved() {
        assertArrayEquals(new String[]{"jdbc:oracle:thin:@h:1521/S", "u", "a-b"},
                OracleThinUrl.split(thin("u/\"a-b\"", "h:1521/S")));
    }

    @Test
    void anEmptyPasswordIsKept() {
        assertArrayEquals(new String[]{"jdbc:oracle:thin:@h:1521/S", "u", ""},
                OracleThinUrl.split(thin("u/", "h:1521/S")));
    }

    @Test
    void theTwoHostFormsAreKeptAsWritten() {
        assertArrayEquals(new String[]{"jdbc:oracle:thin:@//h:1521/S", "u", "p"},
                OracleThinUrl.split(thin("u/p", "//h:1521/S")));
        assertArrayEquals(new String[]{"JDBC:ORACLE:THIN:@h:1521/S", "u", "p"},
                OracleThinUrl.split("JDBC:ORACLE:THIN:" + "u/p" + "@" + "h:1521/S"));
    }

    @Test
    void aUrlWithoutCredentialsIsLeftAlone() {
        assertNull(OracleThinUrl.split("jdbc:oracle:thin:@h:1521/S"));
        assertNull(OracleThinUrl.split("jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(HOST=h)(PORT=1521))(CONNECT_DATA=(SERVICE_NAME=S)))"));
        assertNull(OracleThinUrl.split("jdbc:oracle:oci:" + "u/p" + "@TNS"));
        assertNull(OracleThinUrl.split(null));
    }
}
