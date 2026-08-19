package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link IOUtils}. These exercise the filesystem helpers using a
 * JUnit-managed temporary directory; no database connection is required.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class IOUtilsTest {

    /** Logging sink required by some IOUtils signatures; silent on the happy path. */
    private final LogInterface log = new ConsoleLog();

    @Test
    void stringRoundTripsThroughAFile(@TempDir Path dir) throws Exception {
        File f = dir.resolve("hello.txt").toFile();
        IOUtils.loadStringIntoFile("Hello, world!", f, log);
        assertEquals("Hello, world!", IOUtils.loadFileIntoString(f));
    }

    @Test
    void byteArrayRoundTripsThroughAFile(@TempDir Path dir) throws Exception {
        File f = dir.resolve("bytes.bin").toFile();
        byte[] data = {0, 1, 2, 3, 64, 127, -1};
        IOUtils.loadByteArrayIntoFile(data, f, log);
        assertArrayEquals(data, IOUtils.loadFileIntoByteArray(f));
    }

    @Test
    void charArrayRoundTripsThroughAFile(@TempDir Path dir) throws Exception {
        File f = dir.resolve("chars.txt").toFile();
        char[] chars = "abcdef".toCharArray();
        IOUtils.loadCharArrayIntoFile(chars, f, log);
        assertArrayEquals(chars, IOUtils.loadFileIntoCharArray(f));
    }

    @Test
    void nullStringProducesAnEmptyFile(@TempDir Path dir) throws Exception {
        File f = dir.resolve("empty.txt").toFile();
        IOUtils.loadStringIntoFile(null, f, log);
        assertTrue(f.exists());
        assertEquals(0, f.length());
        assertEquals("", IOUtils.loadFileIntoString(f));
    }

    @Test
    void loadFileIntoByteArrayReturnsEmptyForMissingNullOrEmptyFiles(@TempDir Path dir) throws Exception {
        assertEquals(0, IOUtils.loadFileIntoByteArray(null).length);
        assertEquals(0, IOUtils.loadFileIntoByteArray(dir.resolve("does-not-exist").toFile()).length);

        File empty = dir.resolve("zero.txt").toFile();
        assertTrue(empty.createNewFile());
        assertEquals(0, IOUtils.loadFileIntoByteArray(empty).length);
    }

    @Test
    void copyFileDuplicatesContent(@TempDir Path dir) throws Exception {
        File src = dir.resolve("src.txt").toFile();
        File dst = dir.resolve("dst.txt").toFile();
        Files.write(src.toPath(), "copy me".getBytes(StandardCharsets.UTF_8));

        IOUtils.copyFile(src, dst);

        assertTrue(dst.exists());
        assertEquals("copy me", IOUtils.loadFileIntoString(dst));
    }

    @Test
    void confirmDirectoryCreatesAMissingDirectory(@TempDir Path dir) throws Exception {
        File target = dir.resolve("a/b/c").toFile();
        assertFalse(target.exists());

        File result = IOUtils.confirmDirectory(target.getAbsolutePath());

        assertTrue(result.exists());
        assertTrue(result.isDirectory());
    }

    @Test
    void getOsTempDirReturnsADirectoryHandle() {
        File tmp = IOUtils.getOsTempDir();
        assertNotNull(tmp);
    }

    // ---- grep, including its documented quirk ----------------------------

    @Test
    void grepFindsAStringThatIsNotAtTheStartOfALine(@TempDir Path dir) throws Exception {
        File f = dir.resolve("log.txt").toFile();
        Files.write(f.toPath(), "first line\nsecond NEEDLE line\nthird line\n".getBytes(StandardCharsets.UTF_8));
        assertTrue(IOUtils.grep("NEEDLE", f));
    }

    @Test
    void grepReturnsFalseWhenStringIsAbsent(@TempDir Path dir) throws Exception {
        File f = dir.resolve("log.txt").toFile();
        Files.write(f.toPath(), "nothing to see here\n".getBytes(StandardCharsets.UTF_8));
        assertFalse(IOUtils.grep("NEEDLE", f));
    }

    @Test
    void grepReturnsFalseForMissingFile(@TempDir Path dir) {
        assertFalse(IOUtils.grep("anything", dir.resolve("nope.txt").toFile()));
    }

    @Test
    void grepQuirk_matchAtColumnZeroIsNotFound(@TempDir Path dir) throws Exception {
        // Characterization test: grep uses indexOf(s) > 0 (not >= 0), so a match
        // at the very start of a line is deliberately NOT reported.
        File f = dir.resolve("log.txt").toFile();
        Files.write(f.toPath(), "NEEDLE at column zero\n".getBytes(StandardCharsets.UTF_8));
        assertFalse(IOUtils.grep("NEEDLE", f),
                "grep's >0 comparison means a column-0 match is not detected");
    }
}
