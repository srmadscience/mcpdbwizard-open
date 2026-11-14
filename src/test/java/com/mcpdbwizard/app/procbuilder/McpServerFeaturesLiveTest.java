package com.mcpdbwizard.app.procbuilder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcpdbwizard.pub.DbTestSupport;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The generated MCP server's operational features, on Oracle: a port of the MariaDB fork's
 * {@code MdbServerFeaturesLiveTest} (2026-11-14). The pieces are unit-tested here
 * ({@code McpOAuthPolicyTest}, {@code DbPasswordSourceTest}, {@code McpHttpPolicyTest} ...) and
 * {@code TGen23aiMcp} drives the bearer token, TLS and metrics of one regen tree, but nothing ran a
 * generated Oracle server end to end with OAuth, a password from {@code DB_PASS}, the rate limit, the
 * statement timeout, the body cap, the audit trail, the log back-end or the loopback guard. The
 * ORINDADEMO config ({@code generic_test4_mcp}, cut to two tables, the sequence and one packaged
 * procedure) is built three times and each server launched the ways a deployment would:
 * <ul>
 *   <li><b>A</b>: bearer token, TLS, Prometheus metrics, the password from {@code DB_PASS}, a table
 *       curated to read-only, author-supplied descriptions. Run over HTTPS (token, Origin, metrics,
 *       audit trail, statement timeout, body cap, rate limit, log back-end), over stdio (MCP logging
 *       notifications, the password from the environment) and with {@code --list-tools};</li>
 *   <li><b>B</b>: OAuth, against a local JWKS and tokens signed with a generated key;</li>
 *   <li><b>C</b>: no authentication: the non-loopback bind guard, and McpLoad.</li>
 * </ul>
 * Tool names are read from the server's own {@code --list-tools} answer, by suffix, rather than
 * spelled here. The test creates a procedure {@code MCPFEAT_NAP} in ORINDADEMO (and drops it) so a
 * call can outlast the statement timeout: a busy loop, which the driver's cancel interrupts with
 * ORA-01013.
 * <p>
 * Skips unless {@link DbTestSupport} finds a database and ORINDADEMO can log in.
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class McpServerFeaturesLiveTest {
    private static final String OWNER = "ORINDADEMO";
    private static final String DB = "orindademo";
    private static final String TOKEN = "s3cret-token-for-the-test";
    private static final String STOREPASS = "changeit";
    private static final String PROC_DESC = "Book seats. The test's own description of complex_example_add_bookings.";
    private static final String TABLE_DESC = "Read one aircraft. The test's own description of aircraft_get_by_pk.";
    /** A key with a numeric index, for cutting the config down: SQL_TEXT_3, SQL_PARAM_NAME_3_1, TABLE_MCP_DESC_0_PK. */
    private static final java.util.regex.Pattern INDEXED = java.util.regex.Pattern.compile("^([A-Z_]+?)_(\\d+)(_[A-Z0-9_]+)?$");

    private static String NEXTVAL;
    private static String NAP;
    private static GeneratedOracleServer a;
    private static GeneratedOracleServer b;
    private static GeneratedOracleServer c;
    private static Path tmp;
    private static Path keystore;
    private static final List<Process> started = new ArrayList<>();
    /** Each stdio server's stderr file, so a missing reply can report what the server said. */
    private static final Map<Process, Path> stderrOf = new java.util.concurrent.ConcurrentHashMap<>();

    private static Connection owner() throws Exception {
        return DriverManager.getConnection(GeneratedOracleServer.url(), OWNER, DB);
    }

    @BeforeAll
    static void createBuildAndKey() throws Exception {
        assumeTrue(DbTestSupport.databaseAvailable(), "no Oracle database reachable");
        try (Connection conn = owner(); Statement s = conn.createStatement()) {
            // A busy loop, not DBMS_SESSION.SLEEP: it runs on every version from 12.1, and the
            // driver's cancel interrupts it with ORA-01013 as it would a long query.
            s.execute("create or replace procedure MCPFEAT_NAP(r out varchar2) as t date := sysdate; begin"
                    + " while sysdate < t + 30/86400 loop null; end loop; r := 'done'; end;");
        } catch (java.sql.SQLException e) {
            assumeTrue(false, "ORINDADEMO cannot log in or create a procedure here: " + e.getMessage());
        }
        tmp = Files.createTempDirectory(Files.createDirectories(Paths.get("target").toAbsolutePath()), "ora-features-");

        a = new GeneratedOracleServer("generic_test4_mcp").env("DB_PASS", DB);
        a.build(OWNER, "FROM_ENV_VARIABLE_DB_PASS", config -> {
            common(config);
            config.setProperty("MCP_HTTP_TOKEN", "YES");
            config.setProperty("MCP_HTTPS", "YES");
            config.setProperty("PROMETHEUS_SERVER", "YES");
            config.setProperty("TABLE_MCP_CRUD_0", "R");
            config.setProperty("TABLE_MCP_DESC_0_PK", TABLE_DESC);
            config.setProperty("PROC_MCP_DESC_0", PROC_DESC);
        });
        assertEquals(List.of(), a.generatorErrors());
        assertFalse(a.allSource().contains("/" + DB + "@"), "the password reached the source");

        b = new GeneratedOracleServer("generic_test4_mcp");
        b.build(OWNER, DB, config -> {
            common(config);
            config.setProperty("MCP_OAUTH", "YES");
        });
        c = new GeneratedOracleServer("generic_test4_mcp");
        c.build(OWNER, DB, McpServerFeaturesLiveTest::common);

        keystore = tmp.resolve("ks.p12");
        Process keytool = new ProcessBuilder(Paths.get(System.getProperty("java.home"), "bin", "keytool").toString(),
                "-genkeypair", "-alias", "mcp", "-keyalg", "RSA", "-keysize", "2048", "-dname", "CN=localhost",
                "-ext", "SAN=ip:127.0.0.1,dns:localhost", "-validity", "2", "-storetype", "PKCS12",
                "-keystore", keystore.toString(), "-storepass", STOREPASS, "-keypass", STOREPASS)
                .redirectErrorStream(true).start();
        assertTrue(keytool.waitFor(60, TimeUnit.SECONDS) && keytool.exitValue() == 0, "keytool failed");

        JsonArray tools = listTools(c);
        NEXTVAL = nameEnding(tools, "sequential_number_generator_nextval");
        NAP = nameEnding(tools, "mcpfeat_nap");
    }

    /**
     * The config every build shares: generic_test4_mcp cut to AIRCRAFT and AIRLINES, the sequence,
     * COMPLEX_EXAMPLE.ADD_BOOKINGS and the nap procedure. Its SQL statements and other routines are
     * dropped, so a failure here is about the server rather than one of them.
     */
    private static void common(Properties config) {
        for (String key : new ArrayList<>(config.stringPropertyNames())) {
            java.util.regex.Matcher m = INDEXED.matcher(key);
            if (!m.matches()) {
                continue;
            }
            int i = Integer.parseInt(m.group(2));
            String family = m.group(1);
            if (family.startsWith("SQL_") || (family.startsWith("PROC_") && i > 0)
                    || (family.startsWith("TABLE_") && i > 1)) {
                config.remove(key);
            }
        }
        config.setProperty("MCP_SERVER", "YES");
        config.setProperty("DAO_POOL", "YES");
        config.setProperty("PROC_USER_1", OWNER);
        config.setProperty("PROC_NAME_1", "MCPFEAT_NAP");
        config.setProperty("PROC_PACKAGE_1", "null");
        config.setProperty("PROC_OVERLOAD_1", "null");
    }

    @AfterAll
    static void stopAndDrop() throws Exception {
        for (Process p : started) {
            p.destroyForcibly();
        }
        if (DbTestSupport.databaseAvailable()) {
            try (Connection conn = owner(); Statement s = conn.createStatement()) {
                s.execute("drop procedure MCPFEAT_NAP");
            } catch (java.sql.SQLException e) {
                // never created (the class skipped), or already gone
            }
        }
    }

    // ------------------------------------------------------------------ launching

    private static Process launch(GeneratedOracleServer built, Map<String, String> env, Path log, String... args) throws Exception {
        List<String> cmd = new ArrayList<>(List.of(Paths.get(System.getProperty("java.home"), "bin", "java").toString(),
                "-Djava.awt.headless=true", "-cp", built.runtimeClasspath(), built.serverClass()));
        cmd.addAll(List.of(args));
        ProcessBuilder pb = new ProcessBuilder(cmd).directory(tmp.toFile());
        pb.environment().remove("DB_PASS");
        pb.environment().putAll(env);
        if (log != null) {
            pb.redirectErrorStream(true).redirectOutput(log.toFile());
        } else {
            pb.redirectError(tmp.resolve("stdio-" + System.nanoTime() + ".err").toFile());
        }
        Process p = pb.start();
        started.add(p);
        if (log == null) {
            stderrOf.put(p, pb.redirectError().file().toPath());
        }
        return p;
    }

    /** The server's own --list-tools answer: its tools/list payload, with no database. */
    private static JsonArray listTools(GeneratedOracleServer built) throws Exception {
        Path out = tmp.resolve("list-tools-" + System.nanoTime() + ".out");
        Process p = launch(built, Map.of(), out, "--list-tools");
        assertTrue(p.waitFor(60, TimeUnit.SECONDS), "--list-tools did not exit");
        assertEquals(0, p.exitValue(), Files.readString(out));
        String o = Files.readString(out);
        return JsonParser.parseString(o.substring(o.indexOf('{'))).getAsJsonObject().getAsJsonArray("tools");
    }

    /** The name of the one tool ending with {@code suffix}: Oracle's carry the owner in front. */
    private static String nameEnding(JsonArray tools, String suffix) {
        JsonObject t = find(tools, suffix);
        assertTrue(t != null, "no tool ending " + suffix + " in " + tools);
        return t.get("name").getAsString();
    }

    private static int freePort() throws Exception {
        try (ServerSocket s = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            return s.getLocalPort();
        }
    }

    private static Path firstFile(Path dir, String name) throws Exception {
        try (Stream<Path> s = Files.walk(dir)) {
            return s.filter(p -> p.getFileName().toString().equals(name)).findFirst().orElseThrow();
        }
    }

    private static HttpClient tlsClient() throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream in = new FileInputStream(keystore.toFile())) {
            ks.load(in, STOREPASS.toCharArray());
        }
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, tmf.getTrustManagers(), null);
        return HttpClient.newBuilder().sslContext(ctx).connectTimeout(Duration.ofSeconds(5)).build();
    }

    // ------------------------------------------------------------------ MCP over HTTP(S)

    /** One HTTP(S) MCP endpoint: base URL, client, optional bearer token, session. */
    private static final class Endpoint {
        final HttpClient client;
        final String url;
        final String token;
        String session;

        Endpoint(HttpClient client, String url, String token) {
            this.client = client;
            this.url = url;
            this.token = token;
        }

        HttpResponse<String> post(String body, Map<String, String> headers) throws Exception {
            HttpRequest.Builder r = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json").header("Accept", "application/json, text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            if (token != null) {
                r.header("Authorization", "Bearer " + token);
            }
            if (session != null) {
                r.header("Mcp-Session-Id", session);
            }
            headers.forEach(r::header);
            return client.send(r.build(), HttpResponse.BodyHandlers.ofString());
        }

        /** Initializes, waiting up to a minute for the server; returns the initialize response. */
        HttpResponse<String> initialize(Process server, Path log) throws Exception {
            long deadline = System.currentTimeMillis() + 60000;
            while (true) {
                try {
                    HttpResponse<String> r = post("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":"
                            + "{\"protocolVersion\":\"2025-06-18\",\"capabilities\":{},\"clientInfo\":{\"name\":\"t\",\"version\":\"1\"}}}",
                            Map.of());
                    if (r.statusCode() == 200) {
                        session = r.headers().firstValue("Mcp-Session-Id").orElseThrow();
                        post("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}", Map.of());
                    }
                    return r;
                } catch (java.io.IOException e) {
                    if (System.currentTimeMillis() > deadline || !server.isAlive()) {
                        fail("server did not start: " + e + "\n" + (log == null ? "" : Files.readString(log)));
                    }
                    Thread.sleep(250);
                }
            }
        }

        JsonObject message(String body) throws Exception {
            HttpResponse<String> r = post(body, Map.of());
            assertEquals(200, r.statusCode(), r.body());
            return parse(r.body());
        }

        JsonArray tools() throws Exception {
            return message("{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/list\"}").getAsJsonObject("result").getAsJsonArray("tools");
        }

        JsonObject call(String tool, String args) throws Exception {
            return message("{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/call\",\"params\":{\"name\":\"" + tool
                    + "\",\"arguments\":" + args + "}}").getAsJsonObject("result");
        }
    }

    private static JsonObject parse(String body) {
        for (String line : body.split("\n")) {
            if (line.startsWith("data: ")) {
                return JsonParser.parseString(line.substring(6)).getAsJsonObject();
            }
        }
        return JsonParser.parseString(body).getAsJsonObject();
    }

    private static String text(JsonObject result) {
        return result.getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString();
    }

    /** The tool whose name is {@code name} or ends with {@code _name}; null if none, a failure if several. */
    private static JsonObject find(JsonArray tools, String name) {
        JsonObject found = null;
        for (JsonElement t : tools) {
            String n = t.getAsJsonObject().get("name").getAsString();
            if (n.equals(name) || n.endsWith("_" + name)) {
                assertEquals(null, found, "two tools end with " + name);
                found = t.getAsJsonObject();
            }
        }
        return found;
    }

    /** The curation and descriptions build A asked for, in any tools/list answer. */
    private static void assertCuratedAndDescribed(JsonArray tools) {
        assertTrue(find(tools, "aircraft_get_by_pk") != null, "read stays");
        for (String op : List.of("aircraft_insert", "aircraft_update", "aircraft_delete")) {
            assertEquals(null, find(tools, op), op + " is curated out by TABLE_MCP_CRUD_0=R");
        }
        assertTrue(find(tools, "airlines_insert") != null, "an uncurated table keeps its writes");
        assertTrue(find(tools, "aircraft_get_by_pk").get("description").getAsString().contains(TABLE_DESC));
        assertTrue(find(tools, "complex_example_add_bookings").get("description").getAsString().contains(PROC_DESC));
    }

    // ------------------------------------------------------------------ the tests

    /** --list-tools answers from the binary, connecting to nothing: no DB_PASS needed. */
    @Test
    void listToolsNeedsNoDatabase() throws Exception {
        assertCuratedAndDescribed(listTools(a));
    }

    /**
     * Over HTTPS with a bearer token: the token is required, Origin is checked, a call is counted in
     * /metrics with its database object, recorded in the audit trail, cut off by the statement
     * timeout, and an over-large body refused. The password came from DB_PASS.
     */
    @Test
    void overHttpsWithEverythingOn() throws Exception {
        int port = freePort();
        int metrics = freePort();
        Path audit = Files.createDirectories(tmp.resolve("audit"));
        Path log = tmp.resolve("https.log");
        Map<String, String> env = new HashMap<>(Map.of("DB_PASS", DB, "MCP_HTTP_TOKEN", TOKEN,
                "MCP_TLS_KEYSTORE", keystore.toString(), "MCP_TLS_KEYSTORE_PASSWORD", STOREPASS,
                "MCP_METRICS_PORT", String.valueOf(metrics), "MCP_AUDIT_FILE_DIR", audit.toString(),
                "MCP_MAX_REQUEST_BYTES", "8192", "DAO_QUERY_TIMEOUT_SECONDS", "1", "MCPDBWIZARD_LOG_BACKEND", "jul"));
        Process server = launch(a, env, log, "http", String.valueOf(port));
        String url = "https://127.0.0.1:" + port + "/mcp";

        Endpoint ok = new Endpoint(tlsClient(), url, TOKEN);
        assertEquals(200, ok.initialize(server, log).statusCode(), Files.readString(log));
        assertEquals(401, new Endpoint(tlsClient(), url, null).initialize(server, log).statusCode(), "no token");
        assertEquals(401, new Endpoint(tlsClient(), url, "wrong").initialize(server, log).statusCode(), "wrong token");
        // Plaintext to the TLS port gets nothing a client can read as HTTP.
        try {
            HttpResponse<String> plain = HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                    URI.create("http://127.0.0.1:" + port + "/mcp")).timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString("{}")).build(), HttpResponse.BodyHandlers.ofString());
            assertNotEquals(200, plain.statusCode());
        } catch (java.io.IOException expected) {
            // a TLS listener answering plaintext with an alert is the normal outcome
        }
        assertEquals(403, ok.post("{\"jsonrpc\":\"2.0\",\"id\":9,\"method\":\"tools/list\"}",
                Map.of("Origin", "https://evil.example")).statusCode(), "a foreign Origin");
        assertEquals(200, ok.post("{\"jsonrpc\":\"2.0\",\"id\":9,\"method\":\"tools/list\"}",
                Map.of("Origin", "http://localhost:" + port)).statusCode(), "a loopback Origin");

        assertCuratedAndDescribed(ok.tools());
        long first = Long.parseLong(text(ok.call(NEXTVAL, "{}")).replaceAll("\\D", ""));
        long second = Long.parseLong(text(ok.call(NEXTVAL, "{}")).replaceAll("\\D", ""));
        assertTrue(second > first, first + " then " + second);

        long t0 = System.nanoTime();
        JsonObject napped = ok.call(NAP, "{}");
        long ms = (System.nanoTime() - t0) / 1_000_000;
        // ORA-01013: the driver's cancel at DAO_QUERY_TIMEOUT_SECONDS interrupts the loop.
        assertTrue(napped.get("isError").getAsBoolean() && text(napped).contains("ORA-01013"),
                "a long scan under a 1 s timeout must fail: " + napped);
        // Well short of the 30 s the loop runs. Oracle's cancel is a second round trip after the
        // driver's timer fires, so allow more than MariaDB's 4 s (measured 4,007 ms on FREE26).
        assertTrue(ms < 10000, "cut off at the timeout: " + ms + " ms");

        String big = "{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"tools/call\",\"params\":{\"name\":\"" + NEXTVAL
                + "\",\"arguments\":{\"pad\":\"" + "x".repeat(9000) + "\"}}}";
        assertEquals(413, ok.post(big, Map.of()).statusCode(), "MCP_MAX_REQUEST_BYTES");

        String scraped = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + metrics + "/metrics"))
                .build(), HttpResponse.BodyHandlers.ofString()).body();
        String line = scraped.lines().filter(l -> l.startsWith("mcpdbwizard_mcp_calls_total") && l.contains("tool=\"" + NEXTVAL + "\""))
                .findFirst().orElse(null);
        assertTrue(line != null, "no counter for " + NEXTVAL + ":\n" + scraped.lines().limit(20).reduce("", (x, y) -> x + y + "\n"));
        assertTrue(line.contains("db_object=") && line.toUpperCase().contains("SEQUENTIAL_NUMBER_GENERATOR")
                && line.contains("outcome=\"ok\""), line);

        server.destroy();
        assertTrue(server.waitFor(30, TimeUnit.SECONDS));
        StringBuilder trail = new StringBuilder();
        try (Stream<Path> s = Files.walk(audit)) {
            for (Path f : (Iterable<Path>) s.filter(Files::isRegularFile)::iterator) {
                trail.append(Files.readString(f));
            }
        }
        assertTrue(trail.toString().contains(NEXTVAL), "the audit trail names the tool: " + trail);
        assertTrue(trail.toString().contains(NAP), "and the failed call");
    }

    /**
     * MCP_RATE_LIMIT: past the burst, a tool call is refused as an error result naming the limit
     * (the generated server limits tools/call; 429 with Retry-After is the web proxy's per-caller quota).
     */
    @Test
    void theRateLimitRefusesPastTheBurst() throws Exception {
        int port = freePort();
        Path log = tmp.resolve("rate.log");
        Process server = launch(a, Map.of("DB_PASS", DB, "MCP_HTTP_TOKEN", TOKEN, "MCP_TLS_KEYSTORE", keystore.toString(),
                "MCP_TLS_KEYSTORE_PASSWORD", STOREPASS, "MCP_RATE_LIMIT", "1", "MCP_RATE_BURST", "3"), log,
                "http", String.valueOf(port));
        Endpoint ep = new Endpoint(tlsClient(), "https://127.0.0.1:" + port + "/mcp", TOKEN);
        assertEquals(200, ep.initialize(server, log).statusCode());
        JsonObject refused = null;
        int answered = 0;
        for (int i = 0; i < 10 && refused == null; i++) {
            JsonObject r = ep.call(NEXTVAL, "{}");
            if (r.get("isError").getAsBoolean()) {
                refused = r;
            } else {
                answered++;
            }
        }
        assertTrue(refused != null, "ten calls in a burst of 3 at 1/s were never refused");
        assertTrue(text(refused).contains("Rate limit exceeded"), refused.toString());
        assertTrue(answered >= 3 && answered <= 4, "the burst was honoured: " + answered + " answered");
    }

    // ------------------------------------------------------------------ stdio

    /** One stdio MCP session: newline-delimited JSON-RPC on the server's stdin and stdout. */
    private static final class Stdio implements AutoCloseable {
        final Process p;
        final BufferedWriter in;
        /** stdout's lines, read on their own thread so a missing reply fails on a timeout, not a hang. */
        final java.util.concurrent.BlockingQueue<String> lines = new java.util.concurrent.LinkedBlockingQueue<>();

        Stdio(Process p) {
            this.p = p;
            this.in = new BufferedWriter(new OutputStreamWriter(p.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader out = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
            Thread reader = new Thread(() -> {
                try {
                    for (String l; (l = out.readLine()) != null; ) {
                        lines.add(l);
                    }
                } catch (java.io.IOException ignored) {
                    // the process went away
                }
                lines.add("<EOF>");
            });
            reader.setDaemon(true);
            reader.start();
        }

        void send(String json) throws Exception {
            in.write(json);
            in.write("\n");
            in.flush();
        }

        /** Every message up to and including the responses with all these ids, in any order. */
        List<JsonObject> until(int... ids) throws Exception {
            java.util.Set<Integer> waiting = new java.util.HashSet<>();
            for (int id : ids) {
                waiting.add(id);
            }
            List<JsonObject> got = new ArrayList<>();
            while (true) {
                String line = lines.poll(30, TimeUnit.SECONDS);
                if (line == null) {
                    String diagnosis = diagnose();
                    System.out.println(diagnosis);
                    fail("no response " + waiting + " within 30 s; got " + got + "\n" + diagnosis);
                }
                if (line.equals("<EOF>")) {
                    fail("the stdio server closed its output; got " + got);
                }
                if (!line.startsWith("{")) {
                    fail("a non-protocol line on stdout, which corrupts stdio: " + line);
                }
                JsonObject m = JsonParser.parseString(line).getAsJsonObject();
                got.add(m);
                if (m.has("id") && waiting.remove(m.get("id").getAsInt()) && waiting.isEmpty()) {
                    return got;
                }
            }
        }

        /**
         * What the server was doing when a reply did not come: whether it is alive, a thread dump
         * (jcmd), and its stderr. The stderr file lives in the test's temp dir, which a CI runner
         * discards, so without this a failure leaves nothing to read. Diagnosis only: it never throws.
         */
        String diagnose() {
            StringBuilder b = new StringBuilder("---- stdio server diagnosis (pid ").append(p.pid())
                    .append(", alive=").append(p.isAlive()).append(") ----\n");
            try {
                Process j = new ProcessBuilder(Paths.get(System.getProperty("java.home"), "bin", "jcmd").toString(),
                        String.valueOf(p.pid()), "Thread.print", "-l").redirectErrorStream(true).start();
                String dump = new String(j.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                j.waitFor(30, TimeUnit.SECONDS);
                b.append("---- thread dump ----\n").append(dump).append('\n');
            } catch (Exception e) {
                b.append("---- thread dump failed: ").append(e).append('\n');
            }
            try {
                Path err = stderrOf.get(p);
                b.append("---- stderr (").append(err).append(") ----\n")
                        .append(err == null ? "(not captured)" : Files.readString(err)).append('\n');
            } catch (Exception e) {
                b.append("---- stderr unreadable: ").append(e).append('\n');
            }
            return b.append("---- end of diagnosis ----").toString();
        }

        void initialize() throws Exception {
            send("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-06-18\","
                    + "\"capabilities\":{},\"clientInfo\":{\"name\":\"t\",\"version\":\"1\"}}}");
            until(1);
            send("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}");
        }

        @Override
        public void close() {
            p.destroyForcibly();
        }
    }

    private static boolean hasLogFor(List<JsonObject> messages, String tool) {
        return messages.stream().anyMatch(m -> m.has("method") && "notifications/message".equals(m.get("method").getAsString())
                && m.toString().contains(tool));
    }

    /**
     * Over stdio: only protocol frames on stdout; at DEBUG a call sends a notifications/message naming
     * its tool, and at INFO (the default threshold) it sends none -- the negative half is the one that
     * proves the level is honoured.
     */
    @Test
    void overStdioWithMcpLogging() throws Exception {
        try (Stdio s = new Stdio(launch(a, Map.of("DB_PASS", DB), null))) {
            s.initialize();
            s.send("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"logging/setLevel\",\"params\":{\"level\":\"debug\"}}");
            s.until(2);
            s.send("{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"" + NEXTVAL + "\",\"arguments\":{}}}");
            List<JsonObject> debug = s.until(3);
            // The notification may follow its response; read one more message if it did.
            if (!hasLogFor(debug, NEXTVAL)) {
                s.send("{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"ping\"}");
                debug.addAll(s.until(4));
            }
            assertTrue(debug.stream().anyMatch(m -> m.has("id") && m.get("id").getAsInt() == 3
                    && !m.getAsJsonObject("result").get("isError").getAsBoolean()), debug.toString());
            assertTrue(hasLogFor(debug, NEXTVAL), "no notifications/message at debug: " + debug);

            s.send("{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"logging/setLevel\",\"params\":{\"level\":\"info\"}}");
            s.until(5);
            s.send("{\"jsonrpc\":\"2.0\",\"id\":6,\"method\":\"tools/call\",\"params\":{\"name\":\"" + NEXTVAL + "\",\"arguments\":{}}}");
            List<JsonObject> info = s.until(6);
            // The ping only flushes a notification that might trail call 6's reply, so it is sent
            // AFTER that reply. Sent right behind the call (pipelined) it went unanswered once in CI
            // (11.4, 2026-11-11: id 6 answered, id 7 never), the server dropping the second of two
            // requests in flight on stdio. Stdio is test-only here; that is recorded, not chased.
            s.send("{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"ping\"}");
            info.addAll(s.until(7));
            assertFalse(hasLogFor(info, NEXTVAL), "a notification at info: " + info);
        }
    }

    /**
     * FROM_ENV_VARIABLE_DB_PASS: a wrong DB_PASS is the server's ORA-01017, so the variable is what is
     * used. The wrong password holds a '-' on purpose: inside the thin URL the driver refused such a
     * password as ORA-17067 "Invalid Oracle URL", so a correct one never connected either (fixed by
     * OracleThinUrl, 2026-11-14). ORA-01017 here is the proof the password reaches the logon.
     */
    @Test
    void thePasswordComesFromTheEnvironment() throws Exception {
        // On Oracle the pool logs in as the server starts (MariaDB's waits for the first call), so a
        // wrong password stops start-up, and what it says is the evidence.
        Path log = tmp.resolve("wrong-pass.log");
        Process p = launch(a, Map.of("DB_PASS", "not-the-password"), log);
        assertTrue(p.waitFor(60, TimeUnit.SECONDS), "a server with a wrong DB_PASS kept running");
        assertNotEquals(0, p.exitValue());
        String said = Files.readString(log);
        assertFalse(said.contains("ORA-17067"), "the password broke the URL: " + said);
        // The generated factory turns error 1017 into this sentence (and ORA-17067 into nothing like it).
        assertTrue(said.contains("Login Error: Check Username and password are valid"), said);
    }

    /** MCPDBWIZARD_LOG_BACKEND names a back-end; a value that is not one stops start-up rather than being ignored. */
    @Test
    void anUnknownLogBackendStopsStartUp() throws Exception {
        Path log = tmp.resolve("badlog.log");
        Process p = launch(a, Map.of("DB_PASS", DB, "MCPDBWIZARD_LOG_BACKEND", "no_such_backend", "MCP_HTTP_TOKEN", TOKEN,
                "MCP_TLS_KEYSTORE", keystore.toString(), "MCP_TLS_KEYSTORE_PASSWORD", STOREPASS), log, "http", String.valueOf(freePort()));
        assertTrue(p.waitFor(60, TimeUnit.SECONDS), "a server with a bad MCPDBWIZARD_LOG_BACKEND kept running");
        assertNotEquals(0, p.exitValue());
        assertTrue(Files.readString(log).contains("MCPDBWIZARD_LOG_BACKEND"), Files.readString(log));
    }

    // ------------------------------------------------------------------ OAuth

    /** MCP_OAUTH: a token signed by the issuer's key, for this resource, opens a session; anything else is 401. */
    @Test
    void oauthAgainstALocalIssuer() throws Exception {
        RSAKey key = new RSAKeyGenerator(2048).keyID("k1").generate();
        HttpServer jwks = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        byte[] doc = new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
        jwks.createContext("/jwks", ex -> {
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, doc.length);
            ex.getResponseBody().write(doc);
            ex.close();
        });
        jwks.start();
        try {
            String issuer = "http://127.0.0.1:" + jwks.getAddress().getPort();
            int port = freePort();
            String resource = "http://127.0.0.1:" + port + "/mcp";
            Path log = tmp.resolve("oauth.log");
            Process server = launch(b, Map.of("MCP_OAUTH_ISSUER", issuer, "MCP_OAUTH_RESOURCE", resource,
                    "MCP_OAUTH_JWKS_URI", issuer + "/jwks"), log, "http", String.valueOf(port));
            HttpClient http = HttpClient.newHttpClient();

            HttpResponse<String> none = new Endpoint(http, resource, null).initialize(server, log);
            assertEquals(401, none.statusCode(), Files.readString(log));
            assertTrue(none.headers().firstValue("WWW-Authenticate").isPresent(), none.headers().map().toString());
            assertEquals(401, new Endpoint(http, resource, mint(key, issuer, "https://elsewhere/mcp")).initialize(server, log)
                    .statusCode(), "a token for another resource");
            Endpoint ok = new Endpoint(http, resource, mint(key, issuer, resource));
            assertEquals(200, ok.initialize(server, log).statusCode(), Files.readString(log));
            assertFalse(ok.call(NEXTVAL, "{}").get("isError").getAsBoolean());
        } finally {
            jwks.stop(0);
        }
    }

    private static String mint(RSAKey key, String issuer, String audience) throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(),
                new JWTClaimsSet.Builder().issuer(issuer).audience(audience).subject("tester")
                        .expirationTime(new Date(System.currentTimeMillis() + 600000)).issueTime(new Date()).build());
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    // ------------------------------------------------------------------ no authentication

    /**
     * An unauthenticated server will not bind beyond loopback: MCP_HTTP_HOST=0.0.0.0 exits 2, unless
     * MCP_ALLOW_UNAUTHENTICATED_EXPOSURE=YES says something in front authenticates.
     */
    @Test
    void anUnauthenticatedServerStaysOnLoopback() throws Exception {
        Path log = tmp.resolve("bind.log");
        Process refused = launch(c, Map.of("MCP_HTTP_HOST", "0.0.0.0"), log, "http", String.valueOf(freePort()));
        assertTrue(refused.waitFor(60, TimeUnit.SECONDS), "it kept running");
        assertEquals(2, refused.exitValue(), Files.readString(log));

        int port = freePort();
        Path log2 = tmp.resolve("bind-allowed.log");
        Process allowed = launch(c, Map.of("MCP_HTTP_HOST", "0.0.0.0", "MCP_ALLOW_UNAUTHENTICATED_EXPOSURE", "YES"), log2,
                "http", String.valueOf(port));
        assertEquals(200, new Endpoint(HttpClient.newHttpClient(), "http://127.0.0.1:" + port + "/mcp", null)
                .initialize(allowed, log2).statusCode());
    }

    /** McpLoad drives the server for a few seconds; every nextval differs, so every call reached Oracle. */
    @Test
    void mcpLoadAgainstTheServer() throws Exception {
        int port = freePort();
        Path log = tmp.resolve("load-server.log");
        Process server = launch(c, Map.of(), log, "http", String.valueOf(port));
        String url = "http://127.0.0.1:" + port + "/mcp";
        new Endpoint(HttpClient.newHttpClient(), url, null).initialize(server, log);

        Path workload = tmp.resolve("work.json");
        Files.writeString(workload, "[{\"tool\":\"" + NEXTVAL + "\",\"args\":{},\"check\":\"unique\"}]");
        Path results = tmp.resolve("load.json");
        Path out = tmp.resolve("load.out");
        Process load = new ProcessBuilder(Paths.get(System.getProperty("java.home"), "bin", "java").toString(), "-cp",
                GeneratedOracleServer.CLASSPATH, "com.mcpdbwizard.loadtest.McpLoad", "--url", url,
                "--workload", workload.toString(), "--for", "4s", "--threads", "2", "--out", results.toString())
                .redirectErrorStream(true).redirectOutput(out.toFile()).start();
        assertTrue(load.waitFor(120, TimeUnit.SECONDS), "McpLoad did not finish");
        assertEquals(0, load.exitValue(), Files.readString(out));
        JsonObject r = JsonParser.parseString(Files.readString(results)).getAsJsonObject();
        assertTrue(r.get("totalCalls").getAsLong() > 10, r.toString());
        for (JsonElement t : r.getAsJsonArray("tools")) {
            assertEquals(0, t.getAsJsonObject().get("errors").getAsLong(), t.toString());
        }
        assertFalse(Files.readString(out).toLowerCase().contains("check failed"), Files.readString(out));
    }
}
