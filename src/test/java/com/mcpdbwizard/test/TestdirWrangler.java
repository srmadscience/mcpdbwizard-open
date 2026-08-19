package com.mcpdbwizard.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

/**
 * Test-tree replacement for the legacy {@code TestdirWrangler}. The original hard-coded
 * machine-specific paths (e.g. {@code /export/data/...}, {@code Y:\MCPDBWizard\...}); this
 * version roots everything under the JVM temp directory so the migrated harnesses run on
 * any machine. Only the directory accessors the active harnesses actually use are kept
 * meaningful; the rest are retained for source compatibility.
 * <p>
 * The BLOB/CLOB round-trip harnesses (e.g. {@code TFileBLOB}, {@code TTablesLOB},
 * {@code TSqlDatatypes}) read whatever files are present in the blob/clob directories and
 * stream them into LOB columns, so the directories are auto-populated with small synthetic
 * fixture files on first access (including the {@code vmstat.exe} name one harness hard-codes).
 * This is done from the client side so the tests have real data to round-trip on any machine.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class TestdirWrangler {

    public TestdirWrangler() {
    }

    private static String ensure(String name) {
        File dir = new File(System.getProperty("java.io.tmpdir"), name);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir.getAbsolutePath();
    }

    /**
     * Populate {@code dir} with a few small fixture files if it has none yet. Binary fixtures
     * use deterministic, varied byte content so LOB round-trips have something to compare.
     */
    private static String ensureFixtures(String name, boolean binary) {
        File dir = new File(ensure(name));
        try {
            if (binary) {
                writeBinaryFixture(new File(dir, "fixture0.bin"), 1024);
                writeBinaryFixture(new File(dir, "fixture1.bin"), 4096);
                // One harness (TSqlDatatypes) hard-codes this filename.
                writeBinaryFixture(new File(dir, "vmstat.exe"), 8192);
            } else {
                writeTextFixture(new File(dir, "fixture0.txt"),
                        "A Quick Brown Fox Jumped over the Lazy Dog.\n");
                // Kept comfortably under PL/SQL's 32767-byte LONG/VARCHAR2 limit: some
                // harnesses stream these files through LONG / LONG RAW columns, which fail
                // with ORA-06502 if the content is larger. Pure ASCII, so the byte length
                // and character length match (BLOB and CLOB round-trips both compare by
                // length).
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 100; i++) {
                    sb.append("line ").append(i).append(": the quick brown fox\n");
                }
                writeTextFixture(new File(dir, "fixture1.txt"), sb.toString());
                // TSqlDatatypes / TSqlDatatypes9i hard-code this CLOB filename.
                writeTextFixture(new File(dir, "index-all.html"),
                        "<html><body><h1>Index</h1>\n" + sb + "</body></html>\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to write test fixtures to " + dir, e);
        }
        return dir.getAbsolutePath();
    }

    /** Idempotent: (re)create the fixture only if it is missing or empty. */
    private static void writeBinaryFixture(File f, int size) throws IOException {
        if (f.isFile() && f.length() == size) {
            return;
        }
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            data[i] = (byte) ((i * 31 + 7) & 0xFF);
        }
        Files.write(f.toPath(), data);
    }

    private static void writeTextFixture(File f, String content) throws IOException {
        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        if (f.isFile() && f.length() == data.length) {
            return;
        }
        Files.write(f.toPath(), data);
    }

    /** A writable scratch directory. */
    public static String getTempDir() {
        return ensure("orinda-test-temp");
    }

    /**
     * A writable directory for generated test files, pre-seeded with ASCII-text fixtures.
     * This directory is shared by harnesses that round-trip its files both as BLOBs (byte
     * length) and as CLOBs / strings (char length), so the fixtures are plain ASCII: their
     * byte length and character length are identical and survive both paths unchanged.
     */
    public static String getTestDir() {
        return ensureFixtures("orinda-test-files", false);
    }

    /**
     * A directory that cannot be written to, used by negative tests. The filesystem root is
     * not writable to a normal user, so file creation under it fails as the harnesses expect.
     */
    public static String getBadTempDir() {
        return File.separator;
    }

    public static String getBigBlobTestDir() {
        return ensureFixtures("orinda-test-bigblob", true);
    }

    public static String getBlobTestDir() {
        return ensureFixtures("orinda-test-blob", true);
    }

    public static String getClobTestDir() {
        return ensureFixtures("orinda-test-clob", false);
    }
}
