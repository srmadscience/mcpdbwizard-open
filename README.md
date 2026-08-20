# MCPDBWizard

Point it at an Oracle schema and it reads the PL/SQL — packages, procedures, functions,
tables, sequences — and writes the Java that calls it: typed DAO factories, callable‑statement
wrappers, table managers, an optional SOAP layer, and a **Model Context Protocol server** that
exposes the whole surface to an AI agent as typed tools.

Everything is generated ahead of time. The output is ordinary Java with fixed SQL statements
and typed binds, so nothing composes a query at run time — including the MCP server, which
calls the generated wrappers rather than writing SQL for a model to run.

Supports Oracle **12c through 26ai**, and is regression‑tested against six live instances
spanning that range.

---

## The part that is hard

Exposing a database to an agent usually means handing it a `run-sql` tool. That works until the
schema is real: published text‑to‑SQL accuracy falls sharply on enterprise‑scale schemas
compared with tidy benchmarks, and a wrong `UPDATE` is not a wrong answer, it is an incident.

The alternative is curated tools — but writing one per procedure by hand does not scale past a
few, and PL/SQL is unusually hostile to deriving them automatically:

- a procedure can have **any number of OUT and IN OUT parameters**, not a single return value
- parameters can be **records** (including records nested inside records), **collections**,
  **`%ROWTYPE`s**, **REF CURSORs**, package types, and overloads
- Oracle's own data dictionary describes these differently between versions

This project does that derivation. Every generated MCP tool carries a real JSON Schema built
from the procedure's actual signature, so the agent is *told* what the parameters are rather
than inferring them from prose.

```
oracle:  PROCEDURE order_summary(p_customer IN  NUMBER,
                                 p_totals   OUT summary_rec,
                                 p_lines    OUT SYS_REFCURSOR)

tool:    order_summary { "p_customer": <number> }
      -> { "p_totals": { "orderCount": 12, "value": 4210.55 },
           "p_lines":  [ { "sku": "AB-1", "qty": 3 }, ... ] }
```

A record crosses as a JSON object, a REF CURSOR as an array of row objects, a `DATE` as an
ISO‑8601 string, `RAW` and binary vectors as base64, CLOB as text, BLOB as base64.

---

## Quick start

Requires **Java 21** and Maven. The Oracle JDBC driver (`com.oracle.database.jdbc:ojdbc11`)
comes from Maven Central — nothing to install by hand.

```bash
mvn clean package
```

That produces two jars in `target/`: a plain one, and a self‑contained
`mcpdbwizard-app-<version>-shaded.jar` with the driver bundled.

```bash
# Interactive (Swing) -- pick objects and options, save a config
java -jar target/mcpdbwizard-app-*-shaded.jar <log_dir> myconfig.pb2

# Batch -- regenerate from a saved config
java -jar target/mcpdbwizard-app-*-shaded.jar <log_dir> build myconfig.pb2
```

`<log_dir>` is created if missing.

> **Changed in 2026-08:** there used to be a leading `<access_code>` argument. It has been
> **removed**, and a command line that still passes one will be read as the log directory.
> It was validated for shape only — ≥19 characters, not a path, not the literal `build` —
> and then ignored, so it authenticated nothing. Drop it from any script that supplies it.

Configs are `.pb2` (a flat properties file) or `.json`; both are accepted, and convert
losslessly either way:

```bash
java -cp target/mcpdbwizard-app-*-shaded.jar \
     com.mcpdbwizard.schema.ConfigConverter myconfig.pb2 myconfig.json
```

---

## What gets generated

| | |
|---|---|
| **DAO factory** | one entry point per config, wiring connections and logging |
| **PL/SQL wrappers** | a class per procedure/function — `setParamX`, `executeProc`, `getParamY` |
| **Table managers** | row CRUD by primary key, plus unique‑key, index and foreign‑key‑child lookups |
| **SQL statement classes** | your own SQL, with typed bind parameters |
| **SOAP service layer** | optional |
| **JSON / JSON‑RPC connectors** | optional |
| **MCP server** | optional (needs Java 17+ for the MCP SDK) |

Generated code depends only on `com.mcpdbwizard.pub`, the runtime library in this repository.

### The MCP server

A single generated `<Factory>McpServer.java`, speaking **stdio** by default or **Streamable
HTTP** when started with `http [port]`. Optional bearer‑token auth and TLS both read their
secrets from the environment at run time and fail closed if unset, so no secret is baked into
the generated source.

It exposes PL/SQL routines, table row CRUD and secondary lookups, user SQL statements,
sequences, and — on 23ai — JSON‑relational duality views with document CRUD and etag optimistic
locking.

**What is exposed is decided when you generate, not at run time.** An object you did not select
has no code generated for it at all, and `TABLE_MCP_CRUD_<i>` narrows a table to any subset of
create/read/update/delete. An operation that is not exposed has no tool method emitted — it is
absent from the binary, not merely unregistered.

---

## Oracle datatype support

Beyond the ordinary scalars and LOBs: 12c identity columns and extended `VARCHAR2`/`RAW`; 21c
native `JSON`; 23ai native `BOOLEAN`, `VECTOR` (dense, binary and sparse), and JSON‑relational
duality views.

Known gaps: `TIMESTAMP WITH [LOCAL] TIME ZONE` and BFILE cross as procedure parameters but not
yet as table columns; `SDO_GEOMETRY` has no JSON mapping, so a routine using one is skipped;
FLOAT16 vectors are blocked server‑side.

---

## Logging

Generated factories pick a `LogInterface` implementation from the config: console, text file,
`java.util.logging`, Log4j 1.x, **SLF4J**, or **Log4j 2**. The SLF4J and Log4j 2 backends live
in `com.mcpdbwizard.pub` and depend only on the facade jar, which is an optional dependency —
supply the api plus a binding yourself if you use them.

---

## Tests

The database‑free suite needs nothing and is green on a fresh clone:

```bash
mvn test
```

Tests that need Oracle are **gated**: with no database reachable they *skip* rather than fail.
To point them at your own instance, copy the templates — the real files are gitignored and
never leave your machine:

```bash
cp src/test/resources/test-boxes.properties.template src/test/resources/test-boxes.properties
cp Scripts/tns/tnsnames.ora.template                 Scripts/tns/tnsnames.ora
cp Scripts/boxes.env.template                        Scripts/boxes.env
```

Per setting, an environment variable (`MCPDBWIZARD_TEST_HOST`, …) always wins over the file, which
is how a run selects one server over another.

A third tier links against **generator output**: `Scripts/testrun_current.sh` regenerates code
from a set of configs and compiles it, and a family of harnesses then drives that code against a
live database.

**That tier is not part of this repository, and neither are the schemas it needs.** The configs
introspect Oracle schemas whose structure is not ours to publish — some of it came from customer
work years ago — and a config *enumerates* the schema it points at, so the configs cannot ship
either. The harnesses go with them: they name those schemas' tables and routines, and they only
compile against a regenerated tree that cannot exist here.

What that costs you: nothing to run the generator, and nothing to run the suite. The
database-free tests are complete and green on a fresh clone; the gated live tests skip. What you
do not get is a ready-made corpus to regenerate against. `Scripts/check_provisioning.sh` stays,
and will name the exact objects a config expects, which is the place to start if you build your
own.

`examples/generated-output/` shows what the generator emits, with no database at all.

---

## Repository layout

| Path | What |
|---|---|
| `src/main/java/com/mcpdbwizard/pub` | runtime library the generated code links against |
| `src/main/java/com/mcpdbwizard/app` | the generator — engine, Swing UI, shared helpers |
| `src/main/java/com/mcpdbwizard/schema` | typed model of a config; `.pb2` ↔ `.json` |
| `src/main/java/com/mcpdbwizard/mcpdbwizardconnector` | JSON / JSON‑RPC connector generator |
| `examples/generated-output` | a checked‑in example of generator output, regenerated 2026‑08‑07 |
| `Scripts/` | regeneration, provisioning checks, and the export gate |

Contributor notes — architecture, conventions and accumulated gotchas — are in
[`CLAUDE.md`](CLAUDE.md).

---

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md). One thing to run before opening a pull request:

```bash
Scripts/export/check-export-clean.sh
```

It fails if a private hostname, a credential or a jar has crept into the tree.

## Licence

**Apache License 2.0** — see [`LICENSE`](LICENSE) and [`NOTICE`](NOTICE).

Code this generator *emits* is your own work and carries no licence obligation from
this project. It links at run time against `com.mcpdbwizard.pub`, which is in this
repository and likewise Apache‑2.0, so you can ship generated applications under
whatever terms you like.
