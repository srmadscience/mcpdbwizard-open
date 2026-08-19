# Observing a generated MCP server

How to get traces and metrics out of a generated `<Factory>McpServer` **without changing the
generated code**, what that buys, and — just as important — what it does not.

There are two tiers, and they answer different questions.

**Tracing is zero-code**: attach the [OpenTelemetry Java
agent](https://opentelemetry.io/docs/zero-code/java/agent/) at run time and you get Jetty and JDBC
spans without touching the generated code. Most of this document is about that.

**Metrics are emitted**, by the optional `PROMETHEUS_SERVER=YES` config flag — see [Prometheus
metrics](#prometheus-metrics) below. That exists because of the single biggest limitation of the
agent, described in the next section but one: *the agent cannot name the tool that ran*, and no
amount of run-time instrumentation can, because the mapping from a tool name back to an Oracle
object only exists at generation time.

---

## Why the agent, and not MCP's own logging feature

MCP used to carry an in-protocol logging utility (the `logging` server capability plus
`notifications/message`). **It was deprecated in protocol revision `2026-07-28`**
([SEP-2577](https://github.com/modelcontextprotocol/modelcontextprotocol/pull/2577)); new
implementations *should not* adopt it, and the spec names its own replacements: log to `stderr` on
stdio transports, and use OpenTelemetry for structured observability. The generated server does not
declare the `logging` capability, which is now the correct posture rather than a gap.

## What you get, and what you don't

The agent auto-instruments the two layers either side of the server: **Jetty** (one server span per
HTTP request) and **JDBC** (one client span per statement, carrying the SQL), plus JVM runtime
metrics. Spans are correctly parented, so a slow tool call is traceable down to the statement that
made it slow.

**It cannot tell you which MCP tool ran.** MCP multiplexes every tool over a single endpoint, so all
of them arrive as `POST /mcp` and the HTTP spans are indistinguishable. In this codebase the tool
identity is partially recoverable from the JDBC span's SQL text, because each generated tool maps to
distinct statements — but that is a workaround, not tool-level observability.

Per-tool *spans* would need instrumentation inside the emitted `call(...)` helper, which is a
generator change and is **not** implemented. Per-tool **metrics** are — see below — so counts,
latencies and volumes are available per tool and per database object today; what is still missing is
trace context, i.e. a span that parents the JDBC spans underneath it.

## Prometheus metrics

Set **`PROMETHEUS_SERVER=YES`** in the config (propfile key, or the Prometheus metrics control on
the web Design → Options tab; there is deliberately no Swing checkbox, as for `MCP_HTTP_TOKEN` and
`MCP_HTTPS`). It needs `MCP_SERVER=YES` — the measurements are taken in the generated server's
`call(...)` funnel, so without a server there is no tool call to measure, and the generator says so
rather than silently emitting nothing.

Then, at run time, set **`MCP_METRICS_PORT`**. Nothing binds a socket without it:

```sh
MCP_METRICS_PORT=9464 java -cp … <package>.DaoFactoryMcpServer
curl -s localhost:9464/metrics
```

**There is no default port on purpose.** The web application runs up to twenty generated servers at
once and `TMcpServerStartup` forks twenty-one per box; a default would give one of them a listener
and the rest a bind failure on every run. Through the web app, set
`mcpdbwizard.runtime.metrics-port-range` (empty by default, e.g. `9464-9483`) and `RuntimeManager`
allocates one per server and passes it down. `MCP_METRICS_HOST` moves the bind off `127.0.0.1`.

### What is exposed

Every series carries `server`, `tool`, `db_object` and `object_type`.

| Metric | Type | |
|---|---|---|
| `mcpdbwizard_mcp_calls_total` | counter | additionally labelled `outcome` |
| `mcpdbwizard_mcp_call_duration_seconds{quantile="0.5\|0.75\|0.9"}` | summary | plus `_sum` and `_count` |
| `mcpdbwizard_mcp_call_duration_seconds_max` | gauge | |
| `mcpdbwizard_mcp_request_bytes_total`, `..._response_bytes_total` | counter | inbound / outbound JSON |
| `mcpdbwizard_mcp_pool_active`, `_idle`, `_max`, `_borrowed_total`, `_created_total`, `_destroyed_total` | gauge / counter | pooled servers only |

### Why `db_object` needs a generation-time flag

At run time the server holds only the tool name — a lower-cased, punctuation-stripped Oracle name,
with an overload number appended where one was needed — and a single Oracle object routinely yields
several tools (a table yields at least four). `ob_gen_pkg_greet` cannot be turned back into
`APPSCHEMA.OB_GEN_PKG.GREET` by anything running inside the process. The generator therefore writes the
mapping into the server as a `describeTools()` method, one line per tool, built as the tools are
emitted. That is the whole reason this is not a pure runtime feature like the OTel agent.

The label rides on every series rather than sitting in a separate info-metric, so `sum by
(db_object)` needs no PromQL join. It is functionally dependent on `tool`, so it adds no cardinality.

### A call rejected by input-schema validation is not counted

The SDK validates a `tools/call` against the tool's `inputSchema` **before** the handler runs, and
the metrics are taken inside the handler's `call(...)` funnel. So a client sending the wrong type
for an argument gets an `isError` result and leaves **no trace in the metrics at all** — not an
error, not a call.

This matters more than it sounds. During a client misconfiguration a dashboard shows **no traffic**
rather than a spike of failures, which reads as "nobody is using it" instead of "everybody is being
rejected". If you are alerting on this surface, alert on the *absence* of expected calls as well as
on `outcome!="ok"`.

Measured, not assumed: 99 calls where 4 carried a string for a `number` argument produced 95
recorded calls; the 4 appear only as `Tool (...) input validation failed` in the response. A value
that passes the schema and then fails inside the handler — a `string` argument that is not a date —
does record, as `outcome="error"`.

The rate limiter has the same blind spot for the same reason, and `DEPLOYMENT.md` already says so.

### Three choices that look like bugs and are not

- **`outcome` is on the call counter but not on the latency summary.** Splitting the quantiles
  across seven outcomes would compute each from a handful of samples and make p90 meaningless.
- **Quantiles are windowed; the maximum is not.** The quantiles are exact over the last
  `McpMetrics.WINDOW` (2048) calls to that tool — a p90 over all of history stops moving after a day
  and stops answering "is it slow now?". The maximum is since start-up, because a windowed max
  silently discards the worst call the server ever served, which is the one being looked for.
- **An unbindable metrics port is logged, not fatal, but an unparseable one stops start-up.**
  Refusing to serve tools because nothing could be told about them is the wrong trade — this is not
  a fail-closed security control. A port that will not parse is a typo, and the failure worth
  preventing there is an operator who believes they are collecting metrics and is not.

The endpoint is unauthenticated and binds loopback; exposing it warns rather than refusing (unlike
an exposed MCP port, which `McpHttpPolicy` refuses without token auth). It is read-only and carries
no data from the database, but it does publish the schema's object names and the traffic shape, so
put a network policy in front of it.

## Attaching the agent

Get the agent jar once (Maven Central,
`io.opentelemetry.javaagent:opentelemetry-javaagent`) and point a JVM at it. Configuration is
entirely OpenTelemetry's own standard environment variables — the generator deliberately adds no
config surface of its own, so there is nothing to set in the `.pb2`/`.json` config or the GUI.

```sh
export OTEL_SERVICE_NAME=my-oracle-mcp
export OTEL_EXPORTER_OTLP_ENDPOINT=http://collector:4317
export OTEL_METRICS_EXPORTER=none        # traces only, if that is all you want
```

### 1. Running the server directly

```sh
java -javaagent:/opt/otel/opentelemetry-javaagent.jar \
     -cp "$(cat app/target/mcp-classpath.txt):app/target/regen/Classes" \
     com.example.dao.DaoFactoryMcpServer http 8090
```

### 2. Running it from the web Runtime page

`RuntimeManager` builds a fixed command line for the child JVM
(`javaBin(), "-Djava.awt.headless=true", "-cp", …`), so there is **no `-javaagent` hook to pass**.
Use `JAVA_TOOL_OPTIONS` instead, which the JVM honours and which the child inherits — `ProcessBuilder`
passes the parent environment through, and `RuntimeManager.applyMcpEnv` only adds to it:

```sh
export JAVA_TOOL_OPTIONS="-javaagent:/opt/otel/opentelemetry-javaagent.jar"
```

Note what else this catches: `JAVA_TOOL_OPTIONS` set on the web app applies to the **web app itself
and to the forked generator run**, not only the MCP server. That is usually desirable — it is the
only way to see the generation step at all — but it is not opt-in per process, and the agent adds a
second or two to every JVM start.

### 3. In the container

The agent is deliberately **not** baked into the image; it is ~22 MB and most deployments will not
want it. Mount it and set the variable:

```sh
docker run -v /opt/otel:/otel:ro \
  -e JAVA_TOOL_OPTIONS="-javaagent:/otel/opentelemetry-javaagent.jar" \
  -e OTEL_SERVICE_NAME=my-oracle-mcp \
  -e OTEL_EXPORTER_OTLP_ENDPOINT=http://collector:4317 \
  -p 8080:8080 -p 8090:8090 -v mcpdbwizard-demo:/data mcpdbwizard-web
```

## The agent is safe on the stdio transport

Worth stating explicitly, because this project has been bitten by it: on stdio, **stdout carries the
protocol frames**, and anything else written there corrupts the session — which is exactly how
`DAO_LOG_TYPE=Console Log` once broke `TGen23aiMcp`.

The agent does not have this problem. Verified 2026-08-02 with agent 2.14.0 on Temurin 21, capturing
the two streams separately:

- `java -javaagent:… -version` → **stdout 0 bytes**; the version banner and the JVM's
  class-sharing warning both went to stderr.
- A run with `OTEL_TRACES_EXPORTER=logging` that emitted a real span → **stdout 0 bytes**; the
  span was printed to stderr by `LoggingSpanExporter`.

So both the agent's diagnostics and the debug span exporter use stderr, and a stdio MCP session
stays clean. (In practice the deployed transport here is HTTP, so this matters mainly for
`TGen23aiPool`/`TGen23aiMcp` and `prototypes/mcp/smoke_test_generated.py`, which drive the server
over stdio.)

## The server's own output now reaches stdout

Worth knowing, because it changes what a collector can see. The generated server runs as a child JVM
of the web app, and `RuntimeManager` used to redirect its merged output straight into a file
(`/data/runtime/<config>/server.log`). That made the one component doing database work on an agent's
behalf invisible to `docker logs` and to every log driver or sidecar — reachable only through this
app's own UI.

It is now **teed**: the file keeps exactly the bytes the child produced (the Runtime page tails it and
`PoolStats` parses it), and a copy goes to the web app's stdout tagged `[mcp:<config>]`, where the
platform's existing pipeline can collect it. The generation step is teed the same way as `[gen:…]`.

Two consequences. Anything deciding success by *reading* the log file — as the generation step does,
looking for its completion marker — must wait for the tee to drain first, because `Process.waitFor`
returns before the pump has finished writing. And with the agent attached, its span output goes to
stderr, which is merged into this same stream.

## Per-tool records exist even without the agent

The emitted `call(...)` funnel writes one `MCP-CALL` line per tool call — name, outcome, duration and
argument *names* (never values). See `com.mcpdbwizard.pub.McpCallRecord`. That is not a substitute for
`execute_tool` spans, which would carry trace context and parent the JDBC spans underneath; but it
does mean tool identity is recoverable from the logs today, without an agent and without a collector.

## Pool statistics are a separate channel — keep them

A pooled server already logs a `POOL-STATS` line every 15 seconds
(`com.mcpdbwizard.pub.DaoFactoryPool.statsLine()`), and the web Runtime page parses the newest one out
of the child process's log tail. The agent does **not** replace this: the web UI reads a log file, not
a metrics backend, and there is no collector in that path. The Prometheus endpoint does not replace
it either, for the same reason and one more — it is opt-in, so the log line is the only channel a
default install has. The `mcpdbwizard_mcp_pool_*` gauges were added **alongside** it, exactly as this
paragraph asked.

## What this is not

This is the zero-code tier. The two things it does not give you, both requiring generator work:

- **`execute_tool` spans naming the tool.** The insertion point exists and is narrow — all tool
  handlers funnel through one emitted `call(...)`, and the `CallToolRequest` (with its `name()`
  accessor) is already in scope at every registration site — but no *span* is emitted today. The
  Prometheus metrics above use exactly that insertion point, so the remaining gap is trace context
  (a span that parents the JDBC spans beneath it), not tool identity.
- **Semantic-convention conformance.** OpenTelemetry's GenAI/MCP conventions are still at
  *Development* stability (semconv 1.40.0), so attribute names can still change. Deferring is
  deliberate.
