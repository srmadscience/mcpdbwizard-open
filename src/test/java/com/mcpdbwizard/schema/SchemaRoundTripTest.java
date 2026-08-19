package com.mcpdbwizard.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Round-trips every committed {@code .pb2} file through {@link Schema}: load the file into a
 * {@link Properties}, build a {@code Schema} from it, and assert {@code schema.toPb2()} reproduces
 * a functionally identical property set (same key/value pairs &mdash; a {@code .pb2} is order- and
 * comment-independent). A second leg proves the JSON constructor: {@code Schema -> toJson() ->
 * new Schema(json) -> toPb2()} must land on the same property set too.
 *
 * <p>This is the acceptance test for the PB2 &harr; Schema model: it exercises the parser against
 * the full variety of real-world propfiles (from the 16-file {@code generic_test} set through the
 * mcpdbwizardconnector wizard files), so any dropped, renamed, or mis-parsed key surfaces immediately.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
@EnabledIf("theConfigCorpusIsPresent")
class SchemaRoundTripTest {

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

    static Stream<Path> pb2Files() throws Exception {
        if (!Files.isDirectory(PROPFILES)) {
            return Stream.empty();
        }
        try (Stream<Path> s = Files.list(PROPFILES)) {
            return s.filter(p -> p.getFileName().toString().endsWith(".pb2"))
                    .sorted()
                    .collect(Collectors.toList())
                    .stream();
        }
    }

    private static Properties load(Path file) throws Exception {
        Properties p = new Properties();
        try (InputStream in = new FileInputStream(file.toFile())) {
            p.load(in);
        }
        return p;
    }

    /** Human-readable diff between two property sets, empty when they are functionally identical. */
    private static List<String> diff(Properties expected, Properties actual) {
        List<String> problems = new ArrayList<>();
        Set<String> allKeys = new TreeSet<>();
        allKeys.addAll(expected.stringPropertyNames());
        allKeys.addAll(actual.stringPropertyNames());
        for (String k : allKeys) {
            String e = expected.getProperty(k);
            String a = actual.getProperty(k);
            if (e == null) {
                problems.add("EXTRA key in output: " + k + " = [" + a + "]");
            } else if (a == null) {
                problems.add("MISSING key in output: " + k + " = [" + e + "]");
            } else if (!e.equals(a)) {
                problems.add("VALUE differs for " + k + ": expected [" + e + "] got [" + a + "]");
            }
        }
        return problems;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pb2Files")
    void pb2RoundTripsThroughSchema(Path file) throws Exception {
        Properties original = load(file);
        Assumptions.assumeTrue(original.size() > 0, "empty propfile");

        // Leg 1: PB2 -> Schema -> PB2
        Schema schema = new Schema(original);
        Properties rebuilt = schema.toPb2();
        List<String> problems = diff(original, rebuilt);
        assertTrue(problems.isEmpty(),
                () -> file + " did not round-trip (" + problems.size() + " differences):\n"
                        + String.join("\n", problems));

        // Leg 2: Schema -> JSON -> Schema -> PB2
        Schema fromJson = new Schema(schema.toJson());
        Properties viaJson = fromJson.toPb2();
        List<String> jsonProblems = diff(original, viaJson);
        assertTrue(jsonProblems.isEmpty(),
                () -> file + " did not round-trip via JSON (" + jsonProblems.size() + " differences):\n"
                        + String.join("\n", jsonProblems));
    }

    /** Sanity: the model must actually parse the collections, not just dump everything into extras. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("pb2Files")
    void extrasNeverSwallowKnownFamilies(Path file) throws Exception {
        Schema schema = new Schema(load(file));
        for (String k : schema.getExtraProperties().keySet()) {
            assertTrue(!k.matches("^(SEQUENCE_|TABLE_|PROC_|SQL_FILENAME|SQL_CREATE_CLASS"
                            + "|SQL_TURN_CURSORS_INTO_RECORDS|SQL_PARAM_).*"),
                    () -> file + ": known-family key leaked into extraProperties: " + k);
        }
    }
}
