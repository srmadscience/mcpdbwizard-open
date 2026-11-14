package com.mcpdbwizard.app.procbuilder;

import com.mcpdbwizard.pub.DbTestSupport;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * An Oracle config taken through the path a user's takes, inside a test: the propfile loaded and
 * pointed at the test database, the generator run in batch mode in its own JVM ({@code ProcBuilder}
 * ends with {@code System.exit}), and the generated code compiled. The test then launches the
 * generated MCP server itself, with whatever environment and arguments it is testing.
 * <p>
 * The counterpart of the forks' {@code PgGeneratedServer}-style helpers. The regen harnesses
 * ({@code TGen23aiMcp} and the rest) run against a tree {@code testrun_current.sh} generated before
 * Maven started, one config per tree; this builds a config per test, so one test class can compare
 * servers generated with different switches.
 * <p>
 * <b>A clean exit code is not enough:</b> the generator logs a failed step, carries on and exits 0,
 * so {@link #generatorErrors()} reads its log.
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
final class GeneratedOracleServer {

    /** {@code jdbc:oracle:thin:@host:port/service} or {@code ...:port:sid}. */
    private static final Pattern URL = Pattern.compile("^jdbc:oracle:thin:@(?://)?([^:/]+):(\\d+)([/:])(.+)$");

    /** The test classpath: the generated server runs on the same jars the product ships. */
    static final String CLASSPATH = System.getProperty("java.class.path");

    /**
     * Compile-only stand-ins ({@code Scripts/compile-shims}) for an EJB container and the old Log4J
     * logger, added only when the generated code names them, as {@code testrun_current.sh} does.
     */
    private static final String[][] SHIMS = {
        {"javax.ejb.", "Scripts/compile-shims/javax/ejb/EJBException.java", "Scripts/compile-shims/javax/ejb/SessionBean.java",
                "Scripts/compile-shims/javax/ejb/SessionContext.java"},
        {"com.mcpdbwizard.pub.Log4JLog", "Scripts/compile-shims/com/mcpdbwizard/pub/Log4JLog.java"},
    };

    private final String configName;
    private final Map<String, String> env = new HashMap<>();
    private Path work;
    private String generatorLog;
    private String serverClass;

    /** @param configName the propfile under {@code Propfiles/}, without {@code .pb2} */
    GeneratedOracleServer(String configName) {
        this.configName = configName;
    }

    /** Sets an environment variable for the generator (a {@code DB_PASS} the config defers to, say). */
    GeneratedOracleServer env(String name, String value) {
        env.put(name, value);
        return this;
    }

    /** The JDBC URL of the test database, for a test that connects as another user. */
    static String url() {
        return DbTestSupport.URL;
    }

    /**
     * Generates the config, connected as {@code user}/{@code password}, with {@code customise}
     * applied last, and compiles the result. The password may be the {@code FROM_ENV_VARIABLE_DB_PASS}
     * sentinel; then {@link #env} must supply {@code DB_PASS}.
     */
    GeneratedOracleServer build(String user, String password, Consumer<Properties> customise) throws Exception {
        work = Files.createTempDirectory(Files.createDirectories(Paths.get("target").toAbsolutePath()), "ora-generate-");
        Properties config = config(user, password);
        customise.accept(config);
        generatorLog = generate(config);
        compile(work.resolve("Src"), classes());
        serverClass = findServerClass();
        return this;
    }

    /** The classpath a built server runs on: its classes, then the test classpath. */
    String runtimeClasspath() {
        return classes() + File.pathSeparator + CLASSPATH;
    }

    /** The generated MCP server's main class, read from the source the generator wrote. */
    String serverClass() {
        return serverClass;
    }

    /** The working directory of the build. */
    Path work() {
        return work;
    }

    String generatorLog() {
        return generatorLog;
    }

    /** The generator log's error lines: a failed step logs and carries on, and the exit code stays 0. */
    List<String> generatorErrors() {
        List<String> errors = new ArrayList<>();
        for (String line : generatorLog.split("\n")) {
            if (line.contains(":System Error:") || line.contains(":Error:")) {
                errors.add(line);
            }
        }
        return errors;
    }

    /** Every generated source file, concatenated: for a test that greps the output. */
    String allSource() throws IOException {
        StringBuilder b = new StringBuilder();
        try (Stream<Path> s = Files.walk(work.resolve("Src"))) {
            for (Path p : (Iterable<Path>) s.filter(p -> p.toString().endsWith(".java"))::iterator) {
                b.append(Files.readString(p));
            }
        }
        return b.toString();
    }

    private Path classes() {
        return work.resolve("classes");
    }

    // ------------------------------------------------------------------ the pipeline

    private Properties config(String user, String password) throws IOException {
        Matcher m = URL.matcher(DbTestSupport.URL);
        assertTrue(m.matches(), "MCPDBWIZARD_TEST_URL is not jdbc:oracle:thin:@host:port/service: " + DbTestSupport.URL);
        String host = m.group(1);
        String port = m.group(2);
        // ConnectionWrangler takes a leading '/' to mean a service name, as the propfiles write it.
        String sid = "/".equals(m.group(3)) ? "/" + m.group(4) : m.group(4);
        Properties config = new Properties();
        try (InputStream in = new FileInputStream("Propfiles/" + configName + ".pb2")) {
            config.load(in);
        }
        config.setProperty("HOSTNAME", host);
        config.setProperty("PORT", port);
        config.setProperty("ORACLE_SID", sid);
        config.setProperty("USER", user);
        config.setProperty("PASS", password);
        config.setProperty("DAO_CONNECTION_TYPE", "Hard coded connect string");
        config.setProperty("DAO_CONNECTION_NAME", "jdbc:oracle:thin:" + user + "/" + password + "@" + host + ":" + port
                + m.group(3) + m.group(4));
        config.setProperty("CODE_BASE_DIRECTORY", work.resolve("Src").toString());
        config.setProperty("SQL_FILE_DIRECTORY", Files.createDirectories(work.resolve("SqlFiles")).toString());
        return config;
    }

    private String generate(Properties config) throws Exception {
        File configFile = work.resolve(configName + ".pb2").toFile();
        try (OutputStream out = new FileOutputStream(configFile)) {
            config.store(out, "generated by " + getClass().getSimpleName());
        }
        String main = "com.mcpdbwizard.app.procbuilder.gui.ProcBuilder";
        Path log = work.resolve("ProcBuilder.log");
        List<String> cmd = new ArrayList<>(Arrays.asList(Paths.get(System.getProperty("java.home"), "bin", "java").toString(),
                "-Djava.awt.headless=true", "-cp", CLASSPATH, main, work.resolve("log").toString(), "build",
                configFile.toString()));
        // To a file, not a pipe: the generator writes more than a pipe holds.
        ProcessBuilder pb = new ProcessBuilder(cmd).directory(work.toFile()).redirectErrorStream(true)
                .redirectOutput(log.toFile());
        pb.environment().putAll(env);
        Process generator = pb.start();
        assertTrue(generator.waitFor(600, TimeUnit.SECONDS), "generator did not finish in 600 s");
        String text = Files.readString(log);
        assertEquals(0, generator.exitValue(), text);
        return text;
    }

    private void compile(Path src, Path out) throws IOException {
        List<String> sources;
        try (Stream<Path> s = Files.walk(src)) {
            sources = s.filter(p -> p.toString().endsWith(".java")).map(Path::toString).collect(Collectors.toList());
        }
        assertFalse(sources.isEmpty(), "the generator wrote no source into " + src + ":\n" + generatorLog);
        for (String[] shim : SHIMS) {
            boolean named = false;
            for (String f : sources) {
                if (Files.readString(Paths.get(f)).contains(shim[0])) {
                    named = true;
                    break;
                }
            }
            if (named) {
                for (int i = 1; i < shim.length; i++) {
                    sources.add(Paths.get(shim[i]).toAbsolutePath().toString());
                }
            }
        }
        Files.createDirectories(out);
        List<String> args = new ArrayList<>(Arrays.asList("-d", out.toString(), "-cp", CLASSPATH, "-nowarn", "-proc:none"));
        args.addAll(sources);
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        if (javac.run(null, null, err, args.toArray(new String[0])) != 0) {
            fail("the generated code did not compile:\n" + err.toString(StandardCharsets.UTF_8));
        }
    }

    /** {@code <package>.<factory>McpServer}, from the one {@code *McpServer.java} the generator wrote. */
    private String findServerClass() throws IOException {
        Path file;
        try (Stream<Path> s = Files.walk(work.resolve("Src"))) {
            List<Path> found = s.filter(p -> p.getFileName().toString().endsWith("McpServer.java")).collect(Collectors.toList());
            assertEquals(1, found.size(), "expected one generated MCP server, found " + found);
            file = found.get(0);
        }
        Matcher pkg = Pattern.compile("(?m)^package\\s+([\\w.]+)\\s*;").matcher(Files.readString(file));
        assertTrue(pkg.find(), "no package line in " + file);
        String name = file.getFileName().toString();
        return pkg.group(1) + "." + name.substring(0, name.length() - ".java".length());
    }
}
