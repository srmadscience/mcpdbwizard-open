package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link JdbcLogin#split}: the placeholder taken out of a config's URL, for the passwords each
 * driver's URL parsing gets wrong. The URLs are assembled from parts so that none appears in the
 * source in a credential's shape, which the open-source export's leak gate refuses.
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class JdbcLoginTest {

    private static final String P = JdbcLogin.PLACEHOLDER;
    /** Every character some driver here misreads in a URL. */
    private static final String HARD = "a-b/c@d&e+f%41g%h#i=j k";

    /** The runtime library cannot see the generator's constant, so it keeps its own copy: they must agree. */
    @Test
    void thePlaceholderIsTheGeneratorsSentinel() {
        assertEquals(com.mcpdbwizard.app.common.DbPasswordSource.SENTINEL, JdbcLogin.PLACEHOLDER);
    }

    @Test
    void theOracleCredentialsLeaveTheUrl() {
        assertArrayEquals(new String[]{"jdbc:oracle:thin:@db.example:1521/SVC", "scott", HARD},
                JdbcLogin.split("jdbc:oracle:thin:scott/" + P + "@db.example:1521/SVC", HARD));
    }

    @Test
    void aPostgresPasswordParameterLeavesTheUrl() {
        assertArrayEquals(new String[]{"jdbc:postgresql://h:5432/db?user=u", null, HARD},
                JdbcLogin.split("jdbc:postgresql://h:5432/db?user=u&password=" + P, HARD));
    }

    @Test
    void theParameterMayBeFirstOrInTheMiddle() {
        assertArrayEquals(new String[]{"jdbc:mariadb://h:3306/db?user=u&useResetConnection=true", null, HARD},
                JdbcLogin.split("jdbc:mariadb://h:3306/db?password=" + P + "&user=u&useResetConnection=true", HARD));
        assertArrayEquals(new String[]{"jdbc:mariadb://h:3306/db?user=u&x=1", null, HARD},
                JdbcLogin.split("jdbc:mariadb://h:3306/db?user=u&password=" + P + "&x=1", HARD));
        assertArrayEquals(new String[]{"jdbc:postgresql://h/db", null, HARD},
                JdbcLogin.split("jdbc:postgresql://h/db?password=" + P, HARD));
    }

    @Test
    void anyOtherPlacementIsReplacedAsBefore() {
        String[] parts = JdbcLogin.split("jdbc:x://h/db;pwd=" + P + ";user=u", "s3cret");
        assertEquals("jdbc:x://h/db;pwd=s3cret;user=u", parts[0]);
        assertEquals(null, parts[2]);
    }

    @Test
    void aNameThatMerelyContainsPasswordIsNotTheParameter() {
        String[] parts = JdbcLogin.split("jdbc:x://h/db?oldpassword=" + P, "s3cret");
        assertEquals("jdbc:x://h/db?oldpassword=s3cret", parts[0]);
        assertEquals(null, parts[2]);
    }
}
