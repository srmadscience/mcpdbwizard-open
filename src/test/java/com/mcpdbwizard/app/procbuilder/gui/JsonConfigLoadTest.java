package com.mcpdbwizard.app.procbuilder.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Verifies the application loads its new {@code .json} config format identically to the classic
 * {@code .pb2} format. {@link ApplicationShell#loadConfig(String)} is the single entry point both
 * {@code getIniFile()} (interactive + batch) use, so proving it returns an identical
 * {@link Properties} set for a {@code .json} file and its {@code .pb2} sibling proves the whole
 * application &mdash; every field, table, procedure and SQL statement it later reads out of
 * {@code fileProps} &mdash; behaves the same whichever format was supplied.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
@EnabledIf("theConfigCorpusIsPresent")
class JsonConfigLoadTest {

    /**
     * The open-source export ships no configs -- a config enumerates the schema it introspects --
     * so this whole class has nothing to examine there. Disabled at class level rather than
     * skipped per test: two of these are {@code @ParameterizedTest}s whose source would be empty,
     * and JUnit 5.10 fails a parameterised test that receives zero arguments rather than skipping
     * it, so an {@code Assumptions} call inside the test body never runs.
     */
    static boolean theConfigCorpusIsPresent() {
        return new java.io.File("Propfiles").isDirectory();
    }

    private static final Path PROPFILES = Paths.get("Propfiles");

    static Stream<Path> pb2WithJsonSibling() throws Exception {
        if (!Files.isDirectory(PROPFILES)) {
            return Stream.empty();
        }
        try (Stream<Path> s = Files.list(PROPFILES)) {
            return s.filter(p -> p.getFileName().toString().endsWith(".pb2"))
                    .filter(p -> Files.exists(siblingJson(p)))
                    .sorted()
                    .collect(Collectors.toList())
                    .stream();
        }
    }

    private static Path siblingJson(Path pb2) {
        String name = pb2.getFileName().toString();
        return pb2.resolveSibling(name.substring(0, name.length() - 4) + ".json");
    }

    @Test
    void isJsonConfigRoutesOnExtension() {
        assertTrue(ApplicationShell.isJsonConfig("foo.json"));
        assertTrue(ApplicationShell.isJsonConfig("/a/b/Foo.JSON"));
        assertFalse(ApplicationShell.isJsonConfig("foo.pb2"));
        assertFalse(ApplicationShell.isJsonConfig(null));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pb2WithJsonSibling")
    void jsonConfigLoadsIdenticallyToPb2(Path pb2) throws Exception {
        Properties fromPb2 = ApplicationShell.loadConfig(pb2.toString());
        Properties fromJson = ApplicationShell.loadConfig(siblingJson(pb2).toString());
        assertEquals(fromPb2, fromJson,
                pb2.getFileName() + ": JSON config did not load to the same properties as the .pb2");
    }

    /** There must actually be JSON fixtures to exercise (guards against a silent zero-file run). */
    @Test
    void jsonFixturesExist() throws Exception {
        long n = pb2WithJsonSibling().count();
        Assumptions.assumeTrue(Files.isDirectory(PROPFILES), "no Propfiles dir");
        assertTrue(n > 0, "expected at least one .json config fixture alongside the .pb2 files");
    }
}
