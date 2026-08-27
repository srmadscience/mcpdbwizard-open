// One JDBC login, then exit. Nothing else -- no pool, no retry, no server.
//
// Run by single-file source launch inside a throwaway container, so it is deliberately
// dependency-free beyond the ojdbc the image already carries. See Scripts/loadtest/run-loadtest.sh.
//
// Copyright 2003-2026 ATB Consultancy Services Ltd
// (formerly Orinda Software Ltd, Dublin, Ireland)
import java.sql.*;
import java.nio.file.*;

public class DbProbe {
    public static void main(String[] a) throws Exception {
        String host = env("MCPDBWIZARD_ORACLE_HOST", "JDBCWIZARD_ORACLE_HOST");
        String port = or(env("MCPDBWIZARD_ORACLE_PORT", "JDBCWIZARD_ORACLE_PORT"), "1521");
        String sid  = env("MCPDBWIZARD_ORACLE_SID",  "JDBCWIZARD_ORACLE_SID");
        String user = env("MCPDBWIZARD_ORACLE_USER", "JDBCWIZARD_ORACLE_USER");
        String pass = System.getenv("DB_PASS");
        String pfile = System.getenv("DB_PASS_FILE");
        if (pfile != null && !pfile.isEmpty()) {
            if (pass != null && !pass.isEmpty()) {
                System.out.println("CONFIG: set exactly ONE of DB_PASS and DB_PASS_FILE, not both");
                System.exit(3);
            }
            pass = Files.readString(Path.of(pfile)).trim();
        }
        if (host == null || host.isEmpty() || user == null || user.isEmpty()) {
            System.out.println("CONFIG: MCPDBWIZARD_ORACLE_HOST and _USER must be set");
            System.exit(3);
        }
        // A SID starting with '/' is a service name, which takes the /service URL form.
        String url = "jdbc:oracle:thin:@" + host + ":" + port
                   + (sid != null && sid.startsWith("/") ? sid : ":" + sid);
        // ONE attempt. No loop, no fallback, no second spelling of the password: on 23ai a
        // locked account and a wrong password are the same ORA-01017, so a retry cannot tell
        // them apart and each one brings a real lock closer.
        try (Connection c = DriverManager.getConnection(url, user, pass);
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery("select user from dual")) {
            r.next();
            System.out.println("OK: connected to " + host + (sid == null ? "" : sid) + " as " + r.getString(1));
        } catch (SQLException e) {
            System.out.println("FAIL: ORA-" + e.getErrorCode() + " " + e.getMessage().split("\n")[0]);
            System.exit(1);
        }
    }
    private static String env(String a, String b) { return or(System.getenv(a), System.getenv(b)); }
    private static String or(String a, String b)  { return (a == null || a.isEmpty()) ? b : a; }
}
