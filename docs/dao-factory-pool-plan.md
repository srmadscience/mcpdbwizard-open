# DAO factory pooling: implementation plan

Goal set 2026-08-02: *"to make our web application scale we need pooling of some kind. Instead of
JDBC connection pooling I want a pool of DAOFactory objects. The pool should expand as needed (up to
a fixed size, configurable from PB2 / JSON / GUI) and then shrink when inactive."*

## Goal

Give the generated `<Factory>McpServer` a pool of warm `DaoFactory` instances so concurrent callers
stop serializing on one Oracle connection. Grow on demand to a configured maximum, block briefly when
exhausted, evict back down when idle.

(The SOAP service surface comes along for free rather than needing a step of its own: the generated
`<WsImpl>` extends the factory, so a pooled member already *is* a `ServiceImpl` — see step 7.)

This is emission plus a new `com.mcpdbwizard.pub` component. It is **opt-in and off by default**,
because it changes transaction boundaries (see "Behaviour change" below).

## The bottleneck, found in code

`SAAdminWrangler.java:4683-4686` emits the MCP server's shared state:

```java
// One database Connection per factory - tool handlers are serialized on this lock
private static final Object LOCK = new Object();
private static <Factory> theFactory;
```

and `SAAdminWrangler.java:5001-5012` emits the helper that **every** tool handler routes through:

```java
private interface DocOp { String run() throws Exception; }

private static CallToolResult call(DocOp theOperation)
  {
  synchronized (LOCK)
  ...
```

In the `generic_test_23ai` tree that is **90 `call(() -> …)` sites, 82 `theFactory` references and
28 `theServiceImpl` references, all behind one lock on one connection** — under a multi-threaded
Jetty transport (`HttpServletStreamableServerTransportProvider`, emitted around
`SAAdminWrangler.java:4899`). Effective concurrency is 1.

The single `call()` helper is also the reason this is tractable: it is the one place a borrow/return
has to be inserted for all 90 handlers.

## Why pool factories rather than connections

A JDBC pool hands out connections; each borrow would then need a fresh `DaoFactory`, which re-parses
every statement it touches on first use. The generated `DaoFactory` (6,320 lines for
`generic_test_23ai`) caches one instance of each DAO, and each DAO holds its parsed
`CallableStatement`s. Pooling the factory keeps the connection **and** the parsed cursors **and** the
DAO instances warm.

Two consequences fall straight out of that, and both are load-bearing:

- **Returning a factory must not call `releaseResources()`.** That would close the cached statements
  and throw away the entire benefit. Passivation settles the transaction and nothing else.
- **The pool is sized against `open_cursors` and `sessions`, not against CPU.** N factories × M DAOs
  each pin cursors, and an idle-but-not-yet-evicted factory still pins an Oracle session. This has to
  be said in the generated javadoc, not just here.

## One pool per config, and what that means for sizing

There is **one pool per `.pb2`/`.json` config**, not one globally. It could not sensibly be otherwise:
a pool's members have to be interchangeable, and factories from two configs are not. Each config
generates its own tree with its own `DaoFactory` class in its own package, its own DAO set, its own
`DAO_FACTORY_NAME`, and its own connection baked into `theUrl`. `DaoFactoryPool<T>` is typed to one
factory class and `<Factory>Pool` is emitted inside one tree.

In the current runtime shape the question does not even arise: `RuntimeManager` runs one config at a
time and refuses to start a second (`RuntimeManager.java:163`), and each generated server is its own
child JVM — separate processes cannot share a pool in any case. Within a tree, `main()` holds one
pool in a static, the way it holds `theFactory` today. The class is instantiable rather than a
singleton, so an embedded user *could* create several (one per credential set, say), but that
multiplies Oracle sessions and the javadoc should not encourage it.

**The consequence for sizing is that `DAO_POOL_MAX_SIZE` is a per-config ceiling, not a global one.**
Run several generated servers against the same database and the sessions add up: the number to check
against the server's `sessions` and `open_cursors` is the **sum** of the running configs' maxima, not
any single config's setting. Whatever sizing guidance the generated javadoc carries has to say this —
per-pool arithmetic alone will understate the load on a shared database.

## Library

**Apache Commons Pool 2** (`org.apache.commons:commons-pool2`). `GenericObjectPool` provides exactly
the requested shape:

| Requirement | Commons Pool 2 |
|---|---|
| expand as needed | lazy `makeObject` up to `maxTotal` |
| fixed maximum | `maxTotal` + `blockWhenExhausted` / `maxWait` |
| shrink when inactive | evictor thread: `timeBetweenEvictionRuns` + `minEvictableIdleDuration` + `minIdle` |
| drop broken factories | `testOnBorrow` / `testWhileIdle` → `validateObject` |

Apache-2.0, so licence-clean against `app/` (see `LICENSE`), and no transitive dependencies.
HikariCP was not considered further — it pools `Connection` specifically and cannot hold a
`DaoFactory`. Hand-rolling the evictor is not worth the maintenance.

**Open decision — dependency reach.** This becomes a new *runtime* dependency of generated code.
Declaring it `<optional>` in `app/pom.xml` (the shape already used for `xdb` / `xmlparserv2`) keeps
it off anyone who leaves pooling off, but `app/target/mcp-classpath.txt` and the Docker image's
`/app/lib` must both carry it, and customers shipping generated code will need the jar.

## Where the code lives

Split along the existing `pub` / generated line: policy and mechanism in the library, typed glue
emitted. Emitting the pool logic into every customer tree would copy several hundred lines per
generation and freeze the policy at generation time.

### New in `com.mcpdbwizard.pub`

- **`PooledResourceUser extends OracleResourceUser`** — the contract the pool needs:
  `confirmConnection()`, `isConnectionUsable()`, `settleTransaction(boolean commit)`,
  `closeFactory()`.
- **`DaoFactoryPool<T extends PooledResourceUser>`** — wraps `GenericObjectPool<T>`, built from a
  `java.util.function.Supplier<T>` plus a config. Public surface: `borrow()` / `release(T)` /
  `invalidate(T)`, `withFactory(task)` doing borrow/try/finally in one call, `close()`, and
  active/idle/borrowed counters for the Runtime page. Its internal `PooledObjectFactory<T>` maps:

  | Pool callback | Factory call |
  |---|---|
  | `makeObject` | supplier + `confirmConnection()` |
  | `activateObject` | `confirmConnection()` — reconnects a dropped session |
  | `validateObject` | `isConnectionUsable()` |
  | `passivateObject` | `settleTransaction(commitOnReturn)` |
  | `destroyObject` | `closeFactory()` |

- **`DaoFactoryPoolConfig`** — the six knobs, with `fromSystemProperties(defaults)` overlaying
  design-time values so a deployment can retune without regenerating. This mirrors the existing
  `MCP_HTTP_TOKEN`-supplied-by-environment pattern (`SAAdminWrangler.java:4863`).

### Emitted, gated on `DAO_POOL=YES`

- **`DaoFactory` gains `implements PooledResourceUser`** plus four small public methods. Genuinely
  needed: in the generated factory `releaseConnection()` is `protected` and there is no public
  `close()` at all — only `confirmConnection()`, `setConnection()`, `hasResources()` and
  `releaseResources()` are public.
- ~~**`<Factory>PooledUnit`** — owns `{DaoFactory, ServiceImpl}` as a pair.~~ **Dropped at step 4,
  and worth recording why the plan called for it.** The reasoning was that `theServiceImpl` is
  constructed from `theFactory.theConnection` (`SAAdminWrangler.java:4758`), so the two would have to
  be pooled together. That describes how the MCP server currently *wires* them, not the type
  hierarchy: the emitted service impl **extends the factory** (`SAAdminWrangler.java:13723`), with
  the same three constructors. One object is therefore already both, so the pool holds a single
  member of the widest type — the service impl when `WEB_SERVICES=YES`, the plain factory otherwise
  — and it inherits `PooledResourceUser` without naming it. This also removes the double object the
  MCP server carries today, where `theFactory` and `theServiceImpl` each hold a full set of DAO
  fields.
- **`<Factory>Pool`** — ~40 lines of typed glue: the supplier, the baked-in defaults, the
  system-property overlay. Deliberately thin, so pool policy stays in the library where a fix reaches
  every generated tree without regenerating.

## The MCP server rewiring

One structural change and one mechanical one.

- `DocOp.run()` → `run(<PooledType> u)`; `call()` drops `synchronized (LOCK)` and becomes
  `thePool.withFactory(u -> theOperation.run(u))`. Because the pooled member is both factory and
  service impl, `theFactory.` and `theServiceImpl.` both become `u.` — the two static fields collapse
  into one borrowed object. The existing catch ladder
  (`CSDocumentChangedException` → etag advice, `CSException` → "Database error", `Exception`)
  carries over verbatim, plus one new arm mapping pool exhaustion to a clear "server busy" tool
  error rather than a stack trace.
- The 90 lambdas change `theFactory.` → `u.theFactory.` and `theServiceImpl.` → `u.theServiceImpl.`.
  In the generator that is a handful of emitted prefix strings: the `daoGetter` assignments at
  `SAAdminWrangler.java:5741`, `:6073`, `:6331`, `:6690`, and the `theFactory.theConnection` uses at
  `:5796`, `:5812`, `:5874`, `:5879`.
- `main()` builds the pool instead of one factory (`SAAdminWrangler.java:4752-4758`) and registers a
  shutdown hook to close it.

Two caveats that belong in the emitted comments:

- **stdio gains nothing.** That transport is single-threaded by nature; pooling only pays over HTTP.
- **Behaviour change: a borrow now defines the transaction boundary.** Today `commitOnRelease`
  governs when work is committed; under pooling that moves to `passivateObject`. This is the main
  reason `DAO_POOL` defaults to `NO`.

## Config surface — PB2 / JSON / GUI / web

Six new `Schema` scalars, threaded exactly the way `MCP_HTTP_TOKEN` is.

| Key | Default | Meaning |
|---|---|---|
| `DAO_POOL` | `NO` | master switch; emits the pool classes |
| `DAO_POOL_MAX_SIZE` | `10` | `maxTotal` — the fixed ceiling |
| `DAO_POOL_MIN_IDLE` | `0` | floor the evictor shrinks to (0 = shrink to empty) |
| `DAO_POOL_MAX_WAIT_MS` | `30000` | block this long before failing a borrow |
| `DAO_POOL_IDLE_TIMEOUT_MS` | `300000` | evict a factory idle longer than this |
| `DAO_POOL_ON_RETURN` | `COMMIT` | `COMMIT` or `ROLLBACK`; `COMMIT` matches today's `commitOnRelease=true` |

Six flags is more than the "only add a flag where one config entry yields several operations"
principle usually allows. The justification is that pool sizing is deployment-specific rather than
derivable from the object selection, and the system-property overlay is what keeps it from being a
regeneration trigger.

Touchpoints per key:

- **`app/src/main/java/com/mcpdbwizard/schema/Schema.java`** — field, the all-args constructor,
  accessors, the `SCALAR` name list (`:1012` region, beside `COMMIT_CONNECTIONS` / `CLOSE_CONNECTIONS`
  at `:1030-1031`), `fromPb2` (`:1097`), `toPb2` (`:1265`), `toMap` (`:1360`), `fromMap` (`:1458`).
  The JSON format comes free — it is the same map.
- **`app/…/gui/ApplicationShell.java`** — read at load (pattern at `:802` and `:914-920`), write at
  save (pattern at `:1458`, `:1857`).
- **`app/…/gui/ThingAdministratorFrame.java`** — a **dedicated pooling tab**, not extra controls
  bolted onto `fileOptionsPanelExtraPanel` (where `closeConnCheckBox` / `commitConnCheckBox` live:
  declared `:247-248`, labelled `:1352-1354`, added `:1782`, enable/disable lists `:2194` and
  `:2481`). Six controls with real operational consequences deserve their own page with room to
  explain the `open_cursors` / `sessions` constraint; folding them into a panel already carrying
  unrelated settings would bury exactly the guidance that stops someone setting the maximum to 200.
  One checkbox, four numeric fields, one combo, with the rest of the tab disabled while pooling is
  off.
- **`web/…/config/OptionCatalog.java`** — a `poolOnReturnActions()` value-set for the enforced
  dropdown.
- **`web/…/config/SchemaDefaults.java`** — new-config defaults, beside the existing
  `setCommitConnections` / `setCloseConnections` block.
- **`web/…/controller/OptionsController.java` + `templates/design/options.html`** — a new `pool`
  group alongside `code` / `service` / `extra`, mirroring the Swing tab. Plain
  `<input type="text">` fields already exist on that page (`daoFactoryName`, `packageName`,
  `daoLogName`, `daoConnectionName`), so no new control type is needed; what is missing is **numeric
  validation**, which nothing there currently does because `Schema` stores every scalar as a String.
  That validation is new work, small but real.

## Sequencing

Steps 1–3 change nothing that regenerates, so each can land and be reviewed on its own.

1. **`pub` library + db-free unit tests.** No generator change at all.
2. **Config plumbing end to end** (Schema, PB2/JSON, Swing, web). Still no emission change.
3. **`DaoFactory` emission**: `implements PooledResourceUser` + the four public methods, gated.
4. **Emit `<Factory>PooledUnit` and `<Factory>Pool`.**
5. **MCP server rewiring**: `DocOp` signature, `call()`, `main()`, shutdown hook — gated.
6. **Harnesses + fixture flag**, then the six-box run.
7. Surface pool counters on the web Runtime page.

## Step 7 status: done

**Runtime-page stats — done.** A generated MCP server is its own child JVM, so the web app cannot
call the pool's getters; the log is the only channel, and `RuntimeManager` already captures and tails
it (`RuntimeManager.java:297`). A pooled server logs `DaoFactoryPool.statsLine()` every 15s and the
page parses the newest one out of the tail. No new port, endpoint or auth surface.

**A separate SOAP `ServiceImpl` pooling step was dropped, because there is nothing to pool.** Earlier
drafts of this plan carried one, on the assumption that the SOAP layer was a second thing needing the
same treatment as MCP. It is not: the generated `<WsImpl>` **extends** the factory
(`SAAdminWrangler.java:13723`), so the object the pool hands out already *is* a `ServiceImpl`. The
MCP tools that route through SOAP service methods — the `EXTRA_SQL` statement tools and the
collection tools that need `createExtraTypeObjects()` — run on a borrowed, pooled member today. That
is why `theServiceImpl` fell to zero references in the pooled emission.

What would remain is a different deployment entirely: dropping the generated `@WebService` class into
a servlet container to serve SOAP clients, where the container instantiates it and concurrent
requests would serialize on its one connection. Nothing in this repo runs that shape, there is no
container in the test estate to drive it, and no part of the MCP work depends on it. If it is ever
wanted, the shape is a delegating facade implementing the generated service interface, needing
per-method signatures captured across every service-method emitter the way `getMcpParamTuples()`
already does for MCP — a feature in its own right, to be planned then rather than carried here as
permanent unfinished business.

## Fixtures & tests

- **db-free unit tests** (`app/src/test`) against a stub `PooledResourceUser`: grow to `maxTotal`,
  block-then-timeout when exhausted, evict down to `minIdle` after the idle timeout, a failed
  `validateObject` destroys rather than returns, passivate settles the transaction. Fast, no Oracle,
  runs everywhere.
- **`TDaoFactoryPool`** — live harness gated by the usual `DbTestSupport` availability probe. N
  threads × M calls through a pooled generated factory: assert no cross-talk between borrowers,
  `v$session` count never exceeds `maxTotal`, and the count falls back after the idle timeout.
- **Concurrent MCP harness** — extend `TGen23aiMcp` (or a sibling) to fire overlapping tool calls at
  the HTTP transport and assert both correctness and that throughput beats the serialized baseline.
  Note the recorded zombie-session hazard: an orphaned child JVM now holds N sessions instead of 1,
  so teardown discipline matters more than it did.
- **`DAO_POOL=YES` in `app/Propfiles/generic_test_23ai.pb2`**, the way `MCP_HTTP_TOKEN=YES` is set
  there, so every regen compiles the pooled branch.
- **Standing regression bar**: with the flag absent or `NO`, an 18-propfile regen must be
  **byte-identical on ORCL12**, then the full six-box green run.

## Questions raised while planning, and how they were settled

- **Scope — settled: only the generated server.** `mcpdbwizard-web`'s Design pages introspect Oracle
  through `ConnectionWrangler`, not `DaoFactory`, and `RuntimeManager` only spawns the child JVM;
  nothing under `web/src/main/java` references `DaoFactory` at all. Raised with David twice, and
  nothing inside `web/` was ever asked for, so the pooling work stayed on the generated side.
- **`CONNECTION_TYPE_DATASOURCE` / JNDI — settled: allowed, and layered.** A container pool already
  sits underneath, but the warm DAO/cursor state is what is being pooled; the DataSource beneath
  merely supplies connections. The emitted javadoc says closing returns the connection to the
  container, and `DAO_POOL_MAX_SIZE` must not exceed the DataSource maximum.
- **`CONNECTION_TYPE_JBOSERVER` (BC4J `DBTransaction`) — settled: refused.** Generation fails with a
  message pointing at the Pooling tab, because the framework owns the transaction and a pool settling
  it on return would be fighting for it.
- **Sizing guidance.** Whether the generated javadoc should carry a worked `open_cursors` example
  (`maxTotal` × DAOs-per-factory × statements-per-DAO) or just name the constraint.
