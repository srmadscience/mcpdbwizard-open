package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Assumptions;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Shared helpers for the <b>live-database</b> tests that exercise the SQL-execution
 * wrappers — {@link QueryStatement}, {@link DmlStatement},
 * {@link CallableStatementParameters}, {@link ReadOnlyRowSet} and
 * {@link WriteableRowSet}. Unlike the rest of the {@code com.mcpdbwizard.pub} suite,
 * these classes can only be driven against a real Oracle connection, so the tests
 * that use them are <b>gated</b>: if no database is reachable they are
 * <em>skipped</em> (via JUnit {@link Assumptions}) rather than failed, keeping
 * {@code mvn test} green on machines without the database.
 * <p>
 * Connection details are resolved per key in this order: a {@code -D} system property, then an
 * environment variable, then the gitignored {@code test-boxes.properties} on the test classpath
 * (copy {@code test-boxes.properties.template} to create it). Nothing is hard-coded, so a fresh
 * clone carries no live host or credential and simply skips the live tests.
 * <ul>
 *   <li>{@code -Dmcpdbwizard.test.url}      / {@code MCPDBWIZARD_TEST_URL}</li>
 *   <li>{@code -Dmcpdbwizard.test.user}     / {@code MCPDBWIZARD_TEST_USER}</li>
 *   <li>{@code -Dmcpdbwizard.test.password} / {@code MCPDBWIZARD_TEST_PASSWORD}</li>
 * </ul>
 * The pre-rename {@code orinda.test.*} / {@code ORINDA_TEST_*} spellings are still accepted, with
 * the new name winning. See {@code resolve} for why that is not merely politeness.
 * A single availability probe is performed (with a short login timeout) and cached,
 * so an absent database costs one short connection attempt for the whole run.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class DbTestSupport {

    /**
     * Local box inventory, loaded once from {@code test-boxes.properties} on the test classpath.
     * <p>
     * The connection details used to be hard-coded defaults in this class. They are now supplied by
     * this file, which is <b>gitignored</b>: the repository ships only
     * {@code test-boxes.properties.template}, so a clone carries no live host or credential while a
     * working copy keeps the convenience of a no-environment {@code mvn test}. Absent file means
     * every value falls through to empty, the availability probe fails, and the live tests SKIP —
     * which is the correct behaviour for anyone who has no test database.
     * <p>
     * Resolution order per key: {@code -D} system property, then environment variable, then this
     * file. Keys are the system-property names ({@code orinda.test.url}, {@code .host}, ...).
     */
    private static final java.util.Properties LOCAL_BOXES = loadLocalBoxes();

    private static java.util.Properties loadLocalBoxes() {
        java.util.Properties p = new java.util.Properties();
        try (java.io.InputStream in =
                     DbTestSupport.class.getResourceAsStream("/test-boxes.properties")) {
            if (in != null) {
                p.load(in);
            }
        } catch (java.io.IOException e) {
            // A malformed or unreadable inventory is not worth failing the build over: the values
            // stay empty, the probe fails, and the live tests skip exactly as they would with no
            // database at all.
        }
        return p;
    }

    public static final String URL = resolve("url");
    public static final String USER = resolve("user");
    public static final String PASSWORD = resolve("password");

    // Host / port / SID form, used by the legacy harnesses that build an SAAdminWrangler
    // (which connects by host+sid rather than by JDBC URL).
    public static final String HOST = resolve("host");
    public static final int PORT = parsePort(resolve("port"));
    public static final String SID = resolve("sid");

    /** Cached result of the one-off availability probe. */
    private static Boolean available;

    private DbTestSupport() {
    }

    /**
     * The value of test setting {@code theName} ("host", "sid", …), taking the first non-empty of:
     * the {@code mcpdbwizard.test.*} system property, {@code MCPDBWIZARD_TEST_*}, the legacy
     * {@code orinda.test.*} property, legacy {@code ORINDA_TEST_*}, then the local boxes file
     * under either spelling.
     *
     * <p><b>Both spellings are accepted deliberately, and this one is not a courtesy to strangers.</b>
     * These variables live in developers' shell profiles and in {@code Scripts/boxes.env}, outside
     * the repository, so a clean break could not be fixed by editing the tree — and its failure mode
     * is the bad one. An unrecognised {@code ORINDA_TEST_HOST} does not fail: the value goes empty,
     * the availability probe fails, and every live test <i>skips</i>. A run that should have
     * exercised six Oracle boxes reports green having touched none of them.
     */
    private static String resolve(String theName) {
        String value = firstNonEmpty(
                System.getProperty("mcpdbwizard.test." + theName),
                System.getenv("MCPDBWIZARD_TEST_" + theName.toUpperCase(java.util.Locale.ROOT)),
                System.getProperty("orinda.test." + theName),
                System.getenv("ORINDA_TEST_" + theName.toUpperCase(java.util.Locale.ROOT)),
                LOCAL_BOXES.getProperty("mcpdbwizard.test." + theName),
                LOCAL_BOXES.getProperty("orinda.test." + theName));
        return (value == null) ? "" : value;
    }

    private static String firstNonEmpty(String... theCandidates) {
        for (String theCandidate : theCandidates) {
            if (theCandidate != null && !theCandidate.isEmpty()) {
                return theCandidate;
            }
        }
        return null;
    }

    /** Unset or non-numeric port falls back to 1521 rather than failing class initialisation. */
    private static int parsePort(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (RuntimeException e) {
            return 1521;
        }
    }

    /** Open a brand-new connection. The caller owns it and must close it. */
    static Connection connect() throws SQLException {
        Connection c = DriverManager.getConnection(URL, USER, PASSWORD);
        // The pub library (QueryStatement/DmlStatement/the row sets) and the tests that
        // drive it manage transactions explicitly and call commit()/rollback(). The JDBC
        // default is auto-commit ON, under which an explicit commit() throws ORA-17273
        // ("Could not commit with auto-commit enabled"). Turn it off so the wrappers and
        // their tests own the transaction boundary.
        c.setAutoCommit(false);
        return c;
    }

    /**
     * Skip the calling test (or whole class, when invoked from {@code @BeforeAll}) if
     * no database is reachable; otherwise return a fresh connection.
     */
    static Connection requireConnection() {
        Assumptions.assumeTrue(isAvailable(),
                () -> "No Oracle database reachable at " + URL + " — skipping live wrapper tests");
        try {
            return connect();
        } catch (SQLException e) {
            // Reachable a moment ago but not now: abort (skip) rather than fail.
            Assumptions.abort("Database became unavailable: " + e.getMessage());
            return null; // unreachable — abort() always throws
        }
    }

    /** Whether the configured database is reachable (probed once, then cached). */
    public static boolean databaseAvailable() {
        return isAvailable();
    }

    private static synchronized boolean isAvailable() {
        if (available == null) {
            int previousTimeout = DriverManager.getLoginTimeout();
            DriverManager.setLoginTimeout(5);
            try (Connection probe = connect()) {
                // A successful connect is proof enough; the old ojdbc5 driver does not
                // reliably support Connection.isValid().
                available = (probe != null);
            } catch (Throwable t) {
                available = Boolean.FALSE;
            } finally {
                DriverManager.setLoginTimeout(previousTimeout);
            }
        }
        return available;
    }

    /** Run a statement, ignoring any error. Handy for best-effort {@code DROP}s. */
    static void executeQuietly(Connection connection, String sql) {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException ignored) {
            // best-effort setup / teardown
        }
    }

    /** Run a statement, propagating any error. */
    static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
