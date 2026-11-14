package com.mcpdbwizard.pub;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Connects with a thin URL that carries its credentials, {@code jdbc:oracle:thin:user/password@host...},
 * without handing the credentials to the driver inside the URL.
 * <p>
 * That form is what a config's "Hard coded connect string" holds, and the web console always writes
 * it (with the {@code DB_PASS} sentinel in the password's place). The driver parses the credentials
 * out of the URL itself, and gets it wrong for an ordinary password: one containing {@code -} or
 * {@code /} is "ORA-17067: Invalid Oracle URL specified", and one containing {@code @} is read as part
 * of the host (ORA-17868). Measured against ojdbc11 23.26 on 2026-11-14. The error names the URL, not
 * the password, so a server that will never start reads as a misconfigured address.
 * <p>
 * So the credentials are taken out here and passed as the {@code user} and {@code password}
 * properties, which the driver uses as given. The password runs from the first {@code /} to the
 * LAST {@code @}: a host part never holds an {@code @}, and a password may. Double quotes around it,
 * the driver's own escape for such a password, are removed. A URL with no credentials
 * ({@code jdbc:oracle:thin:@...}, or any other form) is passed to the driver unchanged.
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class OracleThinUrl {

    private static final String PREFIX = "jdbc:oracle:thin:";

    private OracleThinUrl() {
    }

    /**
     * {@code {url without credentials, user, password}}, or null when {@code theUrl} carries no
     * credentials in the {@code user/password@} form.
     */
    public static String[] split(String theUrl) {
        if (theUrl == null || !theUrl.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            return null;
        }
        String theRest = theUrl.substring(PREFIX.length());
        int theAt = theRest.lastIndexOf('@');
        int theSlash = theRest.indexOf('/');
        if (theAt <= 0 || theSlash <= 0 || theSlash > theAt) {
            return null;
        }
        String theUser = theRest.substring(0, theSlash);
        String thePassword = theRest.substring(theSlash + 1, theAt);
        if (thePassword.length() >= 2 && thePassword.startsWith("\"") && thePassword.endsWith("\"")) {
            thePassword = thePassword.substring(1, thePassword.length() - 1);
        }
        return new String[]{theUrl.substring(0, PREFIX.length()) + theRest.substring(theAt), theUser, thePassword};
    }

    /** Opens a connection to {@code theUrl}, its credentials passed as properties when it carries any. */
    public static Connection connect(String theUrl) throws SQLException {
        String[] theParts = split(theUrl);
        if (theParts == null) {
            return DriverManager.getConnection(theUrl);
        }
        Properties theProps = new Properties();
        theProps.setProperty("user", theParts[1]);
        theProps.setProperty("password", theParts[2]);
        return DriverManager.getConnection(theParts[0], theProps);
    }
}
