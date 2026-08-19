# Logging when the generated code lives in a container

What is wrong with the logging story now that generated code runs as a child JVM inside an image
nobody can reach into, what is being changed, and what is deliberately being left alone.

---

## 1. The three problems, in order of severity

### 1.1 The durability guarantees are inverted

There are two audit records for one MCP call, and the weaker one carries the more important fact.

| | Written by | Path | Guarantees |
|---|---|---|---|
| `MCP-CALL` — *what the tool did* | the generated server | `McpAuditSinks.fromEnvironment()` → a sink, optionally wrapped in `SpoolingAuditSink` | at-least-once, survives a broker outage, ordered per segment |
| `MCP-ACCESS` — *who asked for it* | the web app's proxy | `LoggerFactory.getLogger(...)` | none |

The record naming a **person** is the one you would need in a dispute, and it had no spool, no
delivery guarantee and no ordering. The record saying a sequence advanced was bulletproof. That is
backwards, and it happened because the access record was added beside a logger and took the shape of
its neighbour rather than the shape of its job.

### 1.2 `DAO_LOG_TYPE` decides a deployment concern at generation time

In 2003 the person generating the code and the person deploying it were the same person, and
deployment meant putting a jar on a server. One config is now run four ways — in a container, over
stdio, through the web Runtime page, or standalone — and the right backend differs for each. The
choice is nevertheless baked in when the code is emitted.

What that produces inside the shipped image, across the seven options:

| `DAO_LOG_TYPE` | In a container |
|---|---|
| `Java 1.4 Logging` (`JulLog`) | stderr. The only unambiguously correct one. |
| `SLF4J` (`Slf4jLog`) | `slf4j-api` is present with **no binding** — silently logs nothing |
| `Apache Log4j 2` (`Log4j2Log`) | `log4j-api` present, no implementation — silently logs nothing |
| `Console Log` | stdout: fatal on the stdio transport, duplicated under the Runtime tee |
| `Text Log` | a file inside an ephemeral container |
| `Apache's Log4J` (1.x) | a compile shim standing in for a jar from another era |
| `User Implemented` | the operator's own class |

So the two most modern-looking options are the two that silently do nothing, and the safe one is
reachable only by picking something labelled "Java 1.4".

The strongest evidence that this is a real mismatch rather than a preference: **the audit path
already routes around it.** `McpCallRecord` is written through a hardcoded `JulLog` rather than the
configured backend, precisely because a console-log config would put audit records on stdout and
corrupt the protocol stream. The most important line in the system cannot trust the configured
logger.

### 1.3 The image ships facades with no bindings

`/app/lib` contains `slf4j-api` and `log4j-api` and no implementation of either. In a library that is
correct — `app/CLAUDE.md` says those are `<optional>` deps and an application supplies its own
binding. In an image it is a trap, because there is no convenient way for an operator to add a jar to
a classpath assembled by the Dockerfile.

---

## 2. What changes

### 2.1 One audit event type, one sink SPI, four more fields

`McpAccessRecord` gets a second destination: the same `McpAuditSink` the generated server uses. It is
**not** a second SPI. `McpAuditEvent` gains four optional fields — `user`, `config`, `op`, `status` —
which the generated server never sets and which appear in the JSON only when present.

Rejected alternative: a separate `AccessSink` interface. An operator configuring durable audit wants
*the audit*, not two of them with two sets of environment variables and two spools to reason about.
The overlap is already most of the event (tool, outcome, duration), so the additive route keeps one
format, one consumer and one set of guarantees.

The log line stays exactly as it is. The sink is additive: with none configured, behaviour is
unchanged.

**The spool needs process isolation.** `SpoolingAuditSink` writes an active file plus numbered
`segment-*` files in one directory, and drains every segment it finds. The web app and each child
server are separate processes, so pointing them all at one `MCP_AUDIT_SPOOL_DIR` would give two
writers one active file and let each drain the others' segments. The web app therefore appends its
own subdirectory. Documented, because an operator inspecting the spool needs to know why there are
two.

### 2.2 A runtime override for the log backend

New `com.mcpdbwizard.pub.LogBackends.create(defaultClassName, logName)`. It returns an instance of the
class named by the **`MCPDBWIZARD_LOG_BACKEND`** environment variable, or of the generated default when that
is unset. The emitted factory calls it instead of constructing a backend directly.

- **`DAO_LOG_TYPE` stays the source of truth.** It supplies the default, so a deployment that sets
  nothing behaves exactly as before. The variable is an override for the case where the person
  deploying knows something the person generating did not.
- **The variable is deliberately not called `DAO_LOG_TYPE`.** `app/CLAUDE.md` already records
  `MCP_HTTP_TOKEN` being both a config flag and an environment variable as "the confusing case" that
  catches people out. Repeating that pattern knowingly would be indefensible.
- **A bad class name fails at startup**, matching `McpAuditSinks` — a logger that silently is not the
  one you asked for is the failure this whole section exists to prevent.

**This changes emitted output for every config**, so byte-identity with earlier trees breaks
deliberately, as it did for `SessionInfo`. It therefore needs a box run, unlike everything else here.

### 2.3 A binding in the image

`logback-classic` goes into `/app/lib`, so `SLF4J` and — via `log4j-to-slf4j` — `Apache Log4j 2`
become working choices instead of silent no-ops. This changes what a generated server logs when
someone picked one of those, which is the point; it changes nothing for the other five.

**A binding with no configuration is its own problem, and Oracle's XML parser blocks the obvious
fix.** Logback with no config defaults its root logger to DEBUG, so adding the binding made Jetty
narrate every selector wakeup — 269 of 275 lines in a freshly started server. The fix is a
`logback.xml`, which the image carries at `/app/conf/logback.xml` (a plain directory entry on the
child classpath, because the `/app/lib/*` glob is expanded to concrete jars and would skip a bare XML
file; and not inside any jar, because a classpath `logback.xml` imposes itself on every program that
finds it).

That configuration could not load. **Oracle's `xmlparserv2` registers itself as the JAXP
`SAXParserFactory`, and its implementation cannot disable external entities** —
`setFeature("http://xml.org/sax/features/external-general-entities", false)` throws
`ParserConfigurationException` rather than returning. Logback sets exactly that feature to parse its
own config safely, so parsing failed and logback finished with **no appenders at all**. The image
therefore forces the JDK parser through `JAVA_TOOL_OPTIONS`; the web application had been hitting the
same failure in its own logging all along, eight times per start.

**The measurement trap, recorded because it nearly shipped.** Counting DEBUG lines said the fix
worked: 269 → 0. It was zero because *nothing was logging* — the same silence the binding was added
to remove, reached by a different route. A count of unwanted output cannot tell "configured
correctly" from "logging dead". The check that distinguishes them is a positive one: emit an INFO
line and confirm it appears.

**Appender target is `System.err`, not `System.out`.** On the stdio transport a generated server's
stdout carries the protocol frames — the reason its own records go through `JulLog` rather than a
console log. A logback `ConsoleAppender` defaults to stdout, so the default would corrupt any
stdio-transport server the moment anything logged through SLF4J. The HTTP transport the Runtime page
uses would never have shown it.

---

## 3. Deliberately not doing

- **OpenTelemetry.** Already assessed in [`mcp-observability.md`](mcp-observability.md) and deferred
  because the GenAI/MCP semantic conventions are still *Development* stability. Nothing here changes
  that judgement.
- **Removing the Runtime tee.** It is doing real work — it is the only reason `docker logs` sees a
  generated server at all — and it has parsing consumers in `POOL-STATS` and the Runtime page.
- **Retiring `Console Log` or `Text Log`.** Both are correct outside a container, and the emitted
  code is a product other people's builds depend on. The container-specific guidance belongs in
  `DEPLOYMENT.md`, not in deleting options.
- **A structured-logging framework for the generated code.** The `PREFIX {json}` convention
  (`MCP-CALL`, `MCP-ACCESS`, `POOL-STATS`) is ad hoc but it is greppable, parseable, dependency-free
  and already consumed. Replacing it would break those consumers to gain tidiness.
- **Unifying the web app's Logback with the generated code's backend.** They are different processes
  with different lifetimes and different jobs; making them share a stack would couple a Spring Boot
  upgrade to what every generated program has ever done.
