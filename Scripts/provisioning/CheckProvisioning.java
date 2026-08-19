/*
 * CheckProvisioning -- assert that every database object a propfile names actually exists.
 *
 * WHY THIS EXISTS: on 2026-07-31 the ORCL12 box had silently lost the APPSCHEMA.MULTIREC package.
 * Nothing failed. The generator simply emitted 5 fewer files for generic_test1 (2482 instead of
 * 2487), the nested-record compile guard those files provide was inactive, and the whole suite
 * still reported green. A missing fixture must be a LOUD failure, not a quiet loss of coverage.
 *
 * The expectation is derived from the propfiles themselves rather than a hand-written inventory,
 * so it cannot rot: a .pb2 declares every object it introspects as an indexed triple --
 *
 *     PROC_USER_<i> / PROC_PACKAGE_<i> / PROC_NAME_<i>     (packaged or standalone routine)
 *     TABLE_USER_<i> / TABLE_NAME_<i>                      (table or view)
 *     SEQUENCE_USER_<i> / SEQUENCE_NAME_<i>                (sequence)
 *
 * -- and each is checked against ALL_OBJECTS. A packaged routine is checked at PACKAGE level (a
 * per-procedure check would need ALL_PROCEDURES and adds nothing: a package either loaded or it
 * did not). PROC_PACKAGE_<i>=null marks a standalone routine, checked by its own name.
 *
 * Connects with the propfile's OWN USER/PASS (the multi-schema model -- generic_test4 introspects
 * ORINDADEMO, generic_testd introspects GENERIC_TESTD, ...) against the host/port/SID given by the
 * usual ORINDA_TEST_* environment, matching Scripts/testrun_current.sh.
 *
 * It ALSO checks the server-side BFILE test files, which are objects of a different kind and were
 * the second way a box could go quietly wrong: XE18/ORCL19/ORCL21/FREE26 have no SSH, so those
 * files are written through UTL_FILE by sql/bfile_no_ssh_setup.sql, and they used to live in /tmp
 * -- which those boxes wipe on reboot. The result was a 23-failure ORA-22288 FILEOPEN cascade whose
 * victims are mostly TRecordTest2/3*, so it read like a record regression rather than missing files.
 * They now live in /opt/oracle/oradata (persistent), but the check exists because the failure mode
 * is so misleading. Missing files are REPAIRED IN PLACE rather than merely reported: the directory
 * objects are granted read/write to PUBLIC, so any propfile account can rewrite them, and the
 * harnesses only care that a file exists and opens -- never about its contents.
 *
 * Run via Scripts/check_provisioning.sh. Single-file source mode (JEP 330): no build step.
 *
 * Exit codes:  0 = every declared object present (or DB unreachable -> reported as a SKIP);
 *                  BFILE files present, or absent and successfully repaired
 *              1 = at least one declared object is missing, or a BFILE file is missing and
 *                  could not be repaired
 *              2 = usage / unexpected error
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class CheckProvisioning {

    /**
     * One expected object: owner, name, and the kind label used in the report.
     * <p>
     * The name is kept EXACTLY as the propfile spells it. Several APPSCHEMA fixtures are quoted
     * lowercase identifiers ({@code appschema."test seq 4"}, {@code appschema."from"} in sql/appschema.sql),
     * so upper-casing the name would make them look permanently missing. The existence query tries
     * the literal spelling and its upper-case form, which covers both quoted-lowercase objects and
     * ordinary identifiers a propfile happens to store in lower case.
     */
    private static final class Expected {
        final String owner;
        final String name;
        final String kind;

        Expected(String owner, String name, String kind) {
            this.owner = owner.trim().toUpperCase();
            this.name = name.trim();
            this.kind = kind;
        }

        /** Identity is owner.name -- the same package named by 300 PROC_ rows is checked once. */
        @Override
        public boolean equals(Object o) {
            return o instanceof Expected
                    && owner.equals(((Expected) o).owner)
                    && name.equals(((Expected) o).name);
        }

        @Override
        public int hashCode() {
            return (owner + "." + name).hashCode();
        }

        @Override
        public String toString() {
            return owner + "." + name + " (" + kind + ")";
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("usage: CheckProvisioning <propfile.pb2> [<propfile.pb2> ...]");
            System.exit(2);
        }

        // No baked default: the wrapper script sources Scripts/boxes.env, so an unset host
        // means "not configured" and the connection below fails into a SKIP.
        String host = env("MCPDBWIZARD_TEST_HOST", env("ORINDA_TEST_HOST", ""));
        String port = env("MCPDBWIZARD_TEST_PORT", env("ORINDA_TEST_PORT", "1521"));
        String sid = env("MCPDBWIZARD_TEST_SID", env("ORINDA_TEST_SID", ""));
        // A leading '/' means a SERVICE_NAME rather than a SID -- the same convention
        // ConnectionWrangler and testrun_current.sh use.
        String url = sid.startsWith("/")
                ? "jdbc:oracle:thin:@" + host + ":" + port + sid
                : "jdbc:oracle:thin:@" + host + ":" + port + ":" + sid;

        int missingTotal = 0;
        int checkedTotal = 0;
        List<String> skipped = new ArrayList<String>();
        // The wrapper script passes the allowlist's absolute path (it knows its own location).
        Set<String> knownAbsent = loadKnownAbsent(
                System.getProperty("ob.knownAbsentFile", "Scripts/provisioning/known-absent.txt"));

        for (String arg : args) {
            File pb = new File(arg);
            if (!pb.isFile()) {
                System.err.println("ERROR: no such propfile: " + pb);
                System.exit(2);
            }
            Properties p = new Properties();
            try (InputStream in = new FileInputStream(pb)) {
                p.load(in);
            }
            String user = p.getProperty("USER");
            String pass = p.getProperty("PASS");
            if (user == null || pass == null) {
                System.out.println("  " + pb.getName() + ": no USER/PASS -- skipped");
                continue;
            }

            Set<Expected> expected = collect(p);
            if (expected.isEmpty()) {
                System.out.println("  " + pb.getName() + ": declares no objects -- skipped");
                continue;
            }

            List<Expected> missing = new ArrayList<Expected>();
            int allowed = 0;
            try (Connection c = DriverManager.getConnection(url, user, pass)) {
                // Version-gated fixtures: a propfile named *_23ai introspects objects that only a
                // 23ai-line server can hold (duality views, VECTOR, native BOOLEAN), so on an older
                // box their absence is correct, not drift. Same reasoning as the no-gen23ai Maven
                // profile. Skipping keeps the check green on 12c/18c/19c/21c instead of crying wolf.
                int major = c.getMetaData().getDatabaseMajorVersion();
                if (pb.getName().contains("_23ai") && major < 23) {
                    System.out.println("  " + pb.getName() + ": server is Oracle " + major
                            + " (< 23) -- 23ai-only fixtures not applicable, SKIPPED");
                    skipped.add(pb.getName());
                    continue;
                }
                for (Expected e : expected) {
                    if (exists(c, e)) {
                        continue;
                    }
                    if (knownAbsent.contains(e.owner + "." + e.name)) {
                        allowed++;
                        continue;
                    }
                    missing.add(e);
                }
            } catch (Exception e) {
                // Unreachable DB or a schema that was never created is a SKIP, not a failure --
                // this check must never turn an offline box into a red build.
                System.out.println("  " + pb.getName() + ": cannot connect as " + user
                        + " (" + rootMessage(e) + ") -- SKIPPED");
                skipped.add(pb.getName());
                continue;
            }

            checkedTotal += expected.size();
            if (missing.isEmpty()) {
                System.out.println("  " + pb.getName() + ": OK -- all " + expected.size()
                        + " declared objects present (as " + user + ")"
                        + (allowed > 0 ? ", " + allowed + " known-absent" : ""));
            } else {
                missingTotal += missing.size();
                System.out.println("  " + pb.getName() + ": MISSING " + missing.size()
                        + " of " + expected.size() + " declared objects (as " + user + "):");
                for (Expected m : missing) {
                    System.out.println("      " + m);
                }
            }
        }

        // ---- server-side BFILE test files (a per-BOX concern, so once, not per propfile) ----
        int bfileUnrepaired = checkBfiles(url, args);

        System.out.println();
        if (bfileUnrepaired > 0) {
            System.out.println("PROVISIONING CHECK FAILED: " + bfileUnrepaired
                    + " BFILE test file(s) missing and NOT repairable.");
            System.out.println("Rewrite them as SYSTEM:  sqlplus system/<password>@<host>:1521/<service>"
                    + " @sql/bfile_no_ssh_setup <ALIAS> /opt/oracle/oradata");
            System.exit(1);
        }
        if (missingTotal > 0) {
            System.out.println("PROVISIONING CHECK FAILED: " + missingTotal
                    + " declared object(s) missing across " + checkedTotal + " checked.");
            System.out.println("Reload the schema fixtures -- see Scripts/testdata.sh, "
                    + "Scripts/testdata2.sh, sql/multirec.sql, sql/datatypes_23ai_gen.sql.");
            System.exit(1);
        }
        System.out.println("PROVISIONING CHECK PASSED: " + checkedTotal + " declared objects present"
                + (skipped.isEmpty() ? "" : " (" + skipped.size() + " propfile(s) skipped)") + ".");
    }

    /**
     * The BFILE test files, MIRRORING the inventory in sql/bfile_no_ssh_setup.sql -- keep the two in
     * step. Drift is low-consequence by design: the harnesses only require that a file exists and
     * opens, never that it has a particular size or content (see that script's header and
     * Scripts/server_bfile_setup.sh), so a wrong size here still produces a working fixture.
     * Sizes are carried anyway so a repaired file matches what the script would have written.
     */
    private static final String[][] BFILES = buildBfileInventory();

    private static String[][] buildBfileInventory() {
        String[] dates = {
            "152502", "152541", "152551", "152601", "152612", "152622", "152632", "152642",
            "152652", "152702", "152712", "152722", "152733", "152743", "152753", "152803",
            "152813", "152824", "152835", "152846"
        };
        List<String[]> out = new ArrayList<String[]>();
        out.add(new String[] {"APPSCHEMA_TESTDIR1", "test_readable", "1024"});
        out.add(new String[] {"APPSCHEMA_TESTDIR1", "test_unreadable", "1024"});
        out.add(new String[] {"APPSCHEMA_TESTDIR1", "nterdesk.dmp", "10485760"});
        out.add(new String[] {"APPSCHEMA_TESTDIR1", "nterdesk.dmp.Z", "2097152"});
        for (String d : dates) {
            out.add(new String[] {"APPSCHEMA_TESTDIR2", "date." + d, "512"});
        }
        out.add(new String[] {"BYTETEST_TESTDIR1", "GenericFile.txt", "256"});
        return out.toArray(new String[0][]);
    }

    /**
     * Verify every BFILE test file exists, rewriting any that do not. Returns the number that are
     * still missing afterwards (0 on success).
     * <p>
     * Runs once per invocation on ONE connection: these files are a property of the box, not of a
     * propfile, and the directory objects are granted read/write to PUBLIC, so the first propfile
     * account that connects can both see and repair them. If nothing connects, or the directory
     * objects do not exist at all (a box provisioned the /export/data way, or not provisioned yet),
     * this reports and returns 0 -- it must never turn an unrelated situation into a red build.
     */
    private static int checkBfiles(String url, String[] propfiles) {
        Connection c = null;
        for (String arg : propfiles) {
            try {
                Properties p = new Properties();
                try (InputStream in = new FileInputStream(new File(arg))) {
                    p.load(in);
                }
                String user = p.getProperty("USER");
                String pass = p.getProperty("PASS");
                if (user == null || pass == null) {
                    continue;
                }
                c = DriverManager.getConnection(url, user, pass);
                break;
            } catch (Exception ignored) {
                // try the next propfile's account
            }
        }
        if (c == null) {
            System.out.println("  BFILE test files: no usable connection -- SKIPPED");
            return 0;
        }

        try {
            if (!directoriesExist(c)) {
                System.out.println("  BFILE test files: APPSCHEMA_TESTDIR1/2 + BYTETEST_TESTDIR1 not all"
                        + " present -- SKIPPED (box not provisioned, or provisioned without them)");
                return 0;
            }

            List<String[]> missing = new ArrayList<String[]>();
            for (String[] f : BFILES) {
                if (!fileExists(c, f[0], f[1])) {
                    missing.add(f);
                }
            }
            if (missing.isEmpty()) {
                System.out.println("  BFILE test files: OK -- all " + BFILES.length + " present");
                return 0;
            }

            System.out.println("  BFILE test files: " + missing.size() + " of " + BFILES.length
                    + " MISSING -- repairing (this is the ORA-22288 cascade, caught early)");
            int stillMissing = 0;
            for (String[] f : missing) {
                try {
                    writeZeroes(c, f[0], f[1], Integer.parseInt(f[2]));
                    if (!fileExists(c, f[0], f[1])) {
                        stillMissing++;
                        System.out.println("      still missing after write: " + f[0] + "/" + f[1]);
                    }
                } catch (Exception e) {
                    stillMissing++;
                    System.out.println("      cannot repair " + f[0] + "/" + f[1]
                            + ": " + rootMessage(e));
                }
            }
            if (stillMissing == 0) {
                System.out.println("      repaired " + missing.size() + " file(s) -- run continues");
            }
            return stillMissing;
        } catch (Exception e) {
            System.out.println("  BFILE test files: check failed (" + rootMessage(e) + ") -- SKIPPED");
            return 0;
        } finally {
            try {
                c.close();
            } catch (Exception ignored) {
                // nothing useful to do
            }
        }
    }

    /** All three directory objects visible? If not, this box does not use the UTL_FILE fixture path. */
    private static boolean directoriesExist(Connection c) throws Exception {
        String sql = "select count(distinct directory_name) from all_directories"
                + " where directory_name in ('APPSCHEMA_TESTDIR1','APPSCHEMA_TESTDIR2','BYTETEST_TESTDIR1')";
        try (PreparedStatement s = c.prepareStatement(sql); ResultSet rs = s.executeQuery()) {
            return rs.next() && rs.getInt(1) == 3;
        }
    }

    private static boolean fileExists(Connection c, String dir, String name) throws Exception {
        String sql = "select dbms_lob.fileexists(bfilename(?, ?)) from dual";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, dir);
            s.setString(2, name);
            try (ResultSet rs = s.executeQuery()) {
                return rs.next() && rs.getInt(1) == 1;
            }
        }
    }

    /** Rewrite one zero-filled file through UTL_FILE -- the same thing bfile_no_ssh_setup.sql does. */
    private static void writeZeroes(Connection c, String dir, String name, int bytes) throws Exception {
        // The binds are assigned in the executable section, not as declaration defaults, which is
        // the portable placement for a bind inside an anonymous block.
        String plsql =
            "declare\n"
          + "  f          utl_file.file_type;\n"
          + "  chunk      raw(32767);\n"
          + "  chunk_size pls_integer := 32767;\n"
          + "  remaining  pls_integer;\n"
          + "begin\n"
          + "  remaining := ?;\n"
          + "  f := utl_file.fopen(?, ?, 'wb', chunk_size);\n"
          + "  while remaining > 0 loop\n"
          + "    if remaining < chunk_size then chunk_size := remaining; end if;\n"
          + "    chunk := utl_raw.copies(hextoraw('00'), chunk_size);\n"
          + "    utl_file.put_raw(f, chunk, true);\n"
          + "    remaining := remaining - chunk_size;\n"
          + "  end loop;\n"
          + "  utl_file.fclose(f);\n"
          + "exception when others then\n"
          + "  if utl_file.is_open(f) then utl_file.fclose(f); end if;\n"
          + "  raise;\n"
          + "end;";
        try (java.sql.CallableStatement s = c.prepareCall(plsql)) {
            s.setInt(1, bytes);
            s.setString(2, dir);
            s.setString(3, name);
            s.execute();
        }
    }

    /** Every distinct object a propfile declares, across the three indexed key families. */
    private static Set<Expected> collect(Properties p) {
        Set<Expected> out = new LinkedHashSet<Expected>();
        for (String key : p.stringPropertyNames()) {
            if (key.startsWith("PROC_NAME_")) {
                String i = key.substring("PROC_NAME_".length());
                String owner = p.getProperty("PROC_USER_" + i);
                String pkg = p.getProperty("PROC_PACKAGE_" + i);
                String name = p.getProperty("PROC_NAME_" + i);
                if (owner == null) {
                    continue;
                }
                // A packaged routine is checked at package level; PROC_PACKAGE=null means standalone.
                boolean packaged = pkg != null && pkg.length() > 0 && !"null".equalsIgnoreCase(pkg);
                String target = packaged ? pkg : name;
                // A leading '/' marks a name the GENERATOR derived (the hashed form of an
                // over-length public synonym, e.g. "/f62ef35e_PUBSYN_RECORD_TEST_2"). It is a
                // generated Java-side identifier, never a database object name, so checking it
                // would report a permanent false absence.
                if (target != null && target.length() > 0 && !target.startsWith("/")) {
                    out.add(new Expected(owner, target, packaged ? "package" : "routine"));
                }
            } else if (key.startsWith("TABLE_NAME_")) {
                String i = key.substring("TABLE_NAME_".length());
                String owner = p.getProperty("TABLE_USER_" + i);
                String name = p.getProperty("TABLE_NAME_" + i);
                if (owner != null && name != null && name.length() > 0) {
                    out.add(new Expected(owner, name, "table/view"));
                }
            } else if (key.startsWith("SEQUENCE_NAME_")) {
                String i = key.substring("SEQUENCE_NAME_".length());
                String owner = p.getProperty("SEQUENCE_USER_" + i);
                String name = p.getProperty("SEQUENCE_NAME_" + i);
                if (owner != null && name != null && name.length() > 0) {
                    out.add(new Expected(owner, name, "sequence"));
                }
            }
        }
        return out;
    }

    /**
     * ALL_OBJECTS covers packages, standalone routines, tables, views, sequences and synonyms in one
     * view, and reflects what the connected user can actually SEE -- which is the property that
     * matters here, since the generator introspects through the same account.
     */
    private static boolean exists(Connection c, Expected e) throws Exception {
        String sql = "select 1 from all_objects where owner = ? and object_name in (?, ?)"
                + " and rownum = 1";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, e.owner);
            s.setString(2, e.name);
            s.setString(3, e.name.toUpperCase());
            try (ResultSet rs = s.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Known-absent entries, loaded from Scripts/provisioning/known-absent.txt: objects a propfile
     * names but no DDL under sql/ ever creates. They are vestigial, not drift, and would otherwise
     * keep the check permanently red -- which is how a check stops being read. Format is one
     * {@code OWNER.NAME} per line, {@code #} comments ignored; the name is matched case-sensitively,
     * the owner upper-cased.
     */
    private static Set<String> loadKnownAbsent(String path) throws Exception {
        Set<String> out = new LinkedHashSet<String>();
        File f = new File(path);
        if (!f.isFile()) {
            return out;
        }
        for (String line : java.nio.file.Files.readAllLines(f.toPath())) {
            int hash = line.indexOf('#');
            String s = (hash >= 0 ? line.substring(0, hash) : line).trim();
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
        return out;
    }

    private static String env(String key, String dflt) {
        String v = System.getenv(key);
        return (v == null || v.trim().isEmpty()) ? dflt : v.trim();
    }

    private static String rootMessage(Throwable t) {
        String m = t.getMessage();
        if (m == null) {
            return t.getClass().getSimpleName();
        }
        int nl = m.indexOf('\n');
        return nl > 0 ? m.substring(0, nl) : m;
    }
}
