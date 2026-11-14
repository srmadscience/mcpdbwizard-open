package com.mcpdbwizard.pub;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Connects with a config's URL whose password is the {@code FROM_ENV_VARIABLE_DB_PASS} placeholder,
 * passing the real password as the {@code password} property instead of writing it into the URL.
 * <p>
 * Written into a URL, an ordinary password is misread by every driver here, each in its own way
 * (measured 2026-11-14): Oracle's thin driver refuses one containing {@code -} or {@code /}
 * (ORA-17067) and reads one containing {@code @} as part of the host; pgjdbc cuts it at {@code &},
 * turns {@code +} into a space, decodes {@code %41} to {@code A} and refuses a lone {@code %};
 * MariaDB Connector/J cuts it at {@code &}, and decodes nothing, so no encoding can carry one. The
 * console writes the placeholder into every config it saves, so this is the path its servers take.
 * <p>
 * The placeholder is taken out of the URL, never replaced in it:
 * <ul>
 *   <li>{@code jdbc:oracle:thin:user/<placeholder>@host...}: the credentials leave the URL, and
 *       {@code user} and {@code password} go as properties;</li>
 *   <li>a {@code password=<placeholder>} query parameter (Postgres, MariaDB): the parameter leaves
 *       the URL and the password goes as a property; the user stays in the URL;</li>
 *   <li>anywhere else, the placeholder is replaced in the text, as before this existed.</li>
 * </ul>
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class JdbcLogin {

    /** What a config writes where the password would go; matches {@code DbPasswordSource.SENTINEL}. */
    public static final String PLACEHOLDER = "FROM_ENV_VARIABLE_DB_PASS";

    private static final String ORACLE_THIN = "jdbc:oracle:thin:";

    private JdbcLogin() {
    }

    /**
     * {@code {url, user or null, password}} to connect with: the URL without the placeholder, and the
     * properties to pass. {@code user} is null when the URL still names it.
     */
    public static String[] split(String theTemplate, String thePassword) {
        // Oracle: jdbc:oracle:thin:user/<placeholder>@host...
        String theOracleCredentials = "/" + PLACEHOLDER + "@";
        int theOracleAt = theTemplate.indexOf(theOracleCredentials);
        if (theTemplate.regionMatches(true, 0, ORACLE_THIN, 0, ORACLE_THIN.length()) && theOracleAt > ORACLE_THIN.length()) {
            String theUser = theTemplate.substring(ORACLE_THIN.length(), theOracleAt);
            String theRest = theTemplate.substring(theOracleAt + theOracleCredentials.length() - 1);
            return new String[]{theTemplate.substring(0, ORACLE_THIN.length()) + theRest, theUser, thePassword};
        }
        // A query parameter: ...?a=1&password=<placeholder>&b=2
        String theParameter = "password=" + PLACEHOLDER;
        int theAt = theTemplate.indexOf(theParameter);
        if (theAt > 0 && (theTemplate.charAt(theAt - 1) == '?' || theTemplate.charAt(theAt - 1) == '&')) {
            int theEnd = theAt + theParameter.length();
            if (theEnd == theTemplate.length() || theTemplate.charAt(theEnd) == '&') {
                String theUrl;
                if (theEnd < theTemplate.length()) {
                    theUrl = theTemplate.substring(0, theAt) + theTemplate.substring(theEnd + 1);
                } else {
                    theUrl = theTemplate.substring(0, theAt - 1);
                }
                return new String[]{theUrl, null, thePassword};
            }
        }
        return new String[]{theTemplate.replace(PLACEHOLDER, thePassword), null, null};
    }

    /** Opens a connection to {@code theTemplate}, with {@code thePassword} where its placeholder is. */
    public static Connection connect(String theTemplate, String thePassword) throws SQLException {
        String[] theParts = split(theTemplate, thePassword);
        if (theParts[2] == null) {
            return DriverManager.getConnection(theParts[0]);
        }
        Properties theProps = new Properties();
        if (theParts[1] != null) {
            theProps.setProperty("user", theParts[1]);
        }
        theProps.setProperty("password", theParts[2]);
        return DriverManager.getConnection(theParts[0], theProps);
    }
}
