# Restricting a generated MCP server to one customer — plan

**Status (2026-10-02): SCHEDULED, NOT STARTED. David has decided the trust model, and it is NOT the
one §2–§9 below argue for — read §0 first. §0 supersedes the rest of this file wherever they
disagree; §6 (the Oracle half), §7 (pooling) and §11 (negative controls) still apply unchanged.**

**Earlier status (2026-09-21): NOT IMPLEMENTED, and deliberately deferred. Feasibility checked, not built.**
Every hook §5 names was verified to exist against MCP SDK 2.0.0 and the emitter on that date; nothing
was changed beyond this file and a cross-reference in
[`mcp-security-review.md`](mcp-security-review.md) §3.1.

The check that cannot go stale: `grep -rn "SET_CONTEXT" app/src/main/java web/src/main/java` returns
nothing while this is unbuilt. **Believe the grep, not this banner.**

**The workaround in §4 needs nothing built and covers tens of tenants today.** Read §4 before
concluding that anything here is required.

---

## 0. Decisions, 2026-10-02 — URL parameters into `SYS_CONTEXT('MCP', …)`

### 0.1 What David asked for

1. The connection URL may carry one or more extra parameters, e.g. a customer id:
   `https://host/mcp/<owner>/<config>?CUSTOMER_ID=42`.
2. During every tool call, `SYS_CONTEXT('MCP','CUSTOMER_ID')` returns that value, so procedures and
   curated SQL can refuse to touch rows belonging to anyone else. The hotel demo is the first user.

### 0.2 The trust model: the URL is trusted, and that is an ACCEPTED risk

**The threat this defends against is the LLM, not the human.** David, 2026-10-02: changing the URL
means editing the client's configuration, i.e. access to the machine the client runs on; that risk is
accepted, and the docs will say so plainly.

This **overrules §2's rule** ("derive it from the credential, do not transmit it") for this feature.
§2 is still right about what it claims — a URL value is chosen by whoever configures the client — but
the design goal is narrower than §2 assumed. The property that matters holds: **the URL never enters
the model's context and no tool can change it**, so the agent cannot choose a different customer. A
`customer_id` *tool parameter* remains forbidden (§13) for exactly that reason.

**Consequences, carried into the design:**

- **No token-to-tenant mapping, and no trusted-proxy header.** §5(a), §5(b), §9.2, §9.3's flag and
  §10.1 fall away. The generated server reads the parameters from **its own request's query string**,
  and the proxy forwards the query string verbatim. One mechanism, identical behind the proxy and
  standalone.
- **Docs must say, in these words or near them:** the parameters scope the *agent*, not the *person*;
  anyone who can edit the client's configuration can change them; never put a secret in one (they land
  in proxy and access logs).

### 0.3 The parameters are declared, 1..n, in the config and the GUI

Only declared names are usable. Proposed shape, following the `SQL_TEXT_<i>` precedent:

    MCP_CONTEXT_PARAM_1=CUSTOMER_ID
    MCP_CONTEXT_PARAM_2=REGION

- **Name rule:** `[A-Z][A-Z0-9_]*`, at most 30 characters — the portable limit for a
  `DBMS_SESSION.SET_CONTEXT` attribute across 12.1–26ai. Matched case-insensitively, stored upper.
- **Value rule:** at most 4000 bytes (the `SET_CONTEXT` value limit). Rejected, not truncated.
- **A URL parameter NOT on the list → the call is refused**, naming the parameter. Not ignored: an
  ignored typo (`CUSTMER_ID=42`) would run with the context unset, and the failure would surface as
  "no rows" somewhere far away.
- JSON config: the same list via `com.mcpdbwizard.schema` and `ConfigConverter` round-trip.
- GUI: a list editor in Service Options. No parameters declared → the feature is entirely absent from
  the emitted code (nothing emitted, no context package needed).

### 0.4 The Oracle side — we ship it, because the server calls it

§9.4 is decided by the mechanism: the server sets the namespace, so the package that owns it is ours.

    CREATE CONTEXT MCP USING <schema>.MCPDBWIZARD_CTX;

`MCPDBWIZARD_CTX` has one procedure: clear the `MCP` namespace, then set each name/value pair passed
in. Shipped as an install script; the DBA runs it once per database (`CREATE ANY CONTEXT`) and grants
`EXECUTE` to each account a generated server connects as. Per §6, nothing else can write the
namespace (`ORA-01031`).

- **The namespace is database-wide.** One `MCP` context per database, bound to one package. Two
  schemas both served by MCP DB Wizard share it, which is fine — values are per session.
- **The generated server checks at startup** that the namespace exists and is bound to a package it
  can execute, and refuses to start otherwise. Without this the first symptom is a tool error in
  Oracle's voice, mid-demo.
- **The generator refuses to expose `MCPDBWIZARD_CTX` as a tool**, even if the config names it.
  Otherwise the agent could set its own customer, which is the one thing this exists to prevent.
  (Curated SQL or a customer procedure calling it is author code, and the author's responsibility.)
- ~~§6's VPD recipe and the SE2 view alternative stay as documentation~~ -- SUPERSEDED, see §0.7: no
  VPD. The docs show the developer reading `SYS_CONTEXT` themselves, plus a note that a view using
  it must be `WITH CHECK OPTION`.

### 0.5 Per call, on the borrowed connection (§7, made concrete)

Inside `call(...)` (`SAAdminWrangler.java:5882`), inside the `withFactory` lambda (or the
`synchronized (LOCK)` block when unpooled), **before** `theOperation.run`: one round trip,

    begin <schema>.mcpdbwizard_ctx.set_all(:names, :values); end;

which **clears first, then sets** — so a call with no parameters still leaves the session clean, and
a factory returned dirty by a dead call cannot leak into the next borrower. The values come from
`theExchange.transportContext()`, filled by a `.contextExtractor(...)` on the transport builder
(`:5623`) that reads `HttpServletRequest.getParameter` for the declared names only.

**If the set fails, the call fails** and the factory is invalidated, never returned.

The values also go into the audit record (wanted anyway, and the cheapest proof carriage works).
**Not** into Prometheus labels — unbounded cardinality.

### 0.6 Phases, replacing §8

1. **Config + GUI.** `MCP_CONTEXT_PARAM_<i>` in `.pb2`, JSON, `ConfigConverter`, Service Options.
   *Test:* round-trip, name/limit validation.
   **DONE 2026-10-02.** Rules in `com.mcpdbwizard.pub.McpContextParams` (shared with phase 3's
   emitted server); model `schema.McpContextParam` + `Schema.get/setMcpContextParamNames`; the JSON
   key `mcpContextParams` is written ONLY when non-empty, so no committed `.json` moves;
   `ApplicationShell.readMcpContextParams` reads them and the Swing save preserves them; Service
   Options has a one-per-line box, refused WHOLE on a bad or duplicate name, and an `@InitBinder`
   keeps the list from being bound past that check. **An empty value is refused like a missing
   one** (`?CUSTOMER_ID=`): Oracle stores `''` as NULL, so accepting it would satisfy "required"
   and still leave `SYS_CONTEXT` NULL. Nothing is emitted yet — a declared parameter does nothing
   until phase 3, though the GUI text already describes the finished feature.
2. **Oracle package + install script**, loaded on the six estate boxes. *Test:* set/read/clear
   from a plain JDBC session; a direct `DBMS_SESSION.SET_CONTEXT('MCP',…)` gets `ORA-01031`.
   **DONE 2026-10-02, on ALL SIX estate boxes.** Shipped scripts in `app/db/mcp-context/`
   (`install.sql`, `grant.sql <ACCOUNT>`, `uninstall.sql`) — deliberately NOT under `app/sql/`,
   which never ships. `install.sql` is re-runnable, verifies the package compiled VALID, and
   **refuses if `MCP` is already bound to another package** (proven on FREE23 by rebinding it to
   a package in the test schema: ORA-20900, binding left untouched). The package re-checks name and value
   itself (ORA-20901 / ORA-20902), so a caller skipping the Java checks still cannot set a NULL.
   The per-call round trip is `McpContextParams.applyToSession` in `pub` — written now rather than
   in phase 3 so the live test drives the exact call the emitted server will make — plus
   `notInstalledReason` (PLS-00201 = missing package OR missing grant) for phase 3's startup check.
   `McpContextLiveTest` (9 tests: round trip, replace-whole-set, empty clears, per-session,
   4000-byte value, ORA-01031, both package refusals, clear-survives-a-refused-set) is **9/9 on
   ORCL12 (12.1, locked-password account), XE18, ORCL19, ORCL21, FREE23 and FREE26 (schema-only accounts)**. It
   FAILS rather than skips when the DB is up and the context is missing. `testdata.sh` now runs
   the shipped scripts and grants the test schema.
   ORCL19 and FREE26 were down that morning and were done when they came back the same day.
   FREE26's PDB answered ORA-01109 for a few minutes after boot — the box still opening FREEPDB1,
   not a fault.
   **The MCPDEMO hotel box (`endowment`) is NOT part of the test estate** (David,
   2026-10-02): no suite, `testdata.sh` or `estate.sh` run targets it, and its being down is not
   an estate fault. It gets `install.sql` + `grant.sql MCPDEMO` only when phase 6 (the hotel demo)
   needs it, as a demo step rather than as provisioning.
3. **Emission:** extractor, per-call set in `call(...)`, startup check, unknown-parameter refusal,
   audit field, the refuse-to-expose rule. *Test:* `TGen23aiMcp` gains a procedure returning
   `SYS_CONTEXT('MCP','CUSTOMER_ID')`.
   **DONE 2026-10-02, verified on ORCL12.** Everything is guarded by `!mcpContextParams.isEmpty()`,
   so a config declaring none emits exactly what it did before. ApplicationShell validates the list
   and calls `SAAdminWrangler.setMcpContextParams` before generation (Swing throws, batch returns
   false) rather than threading another positional argument through generateCodeV3. The emitted
   server: `CONTEXT_PARAMS`; an HTTP `.contextExtractor` parsing each request's raw query string
   (`McpContextParams.fromQueryString`); stdio reads `MCP_CONTEXT_<NAME>` once and **exits 2** if
   any is missing; a start-up check that exits 2 with the install/grant instructions when the
   package is missing or not granted (any other start-up failure only warns, like the cursor
   check); in `call(...)` a refusal BEFORE the pool, outcome `context-refused`, naming the
   parameter; `applyContext` (clear-then-set) inside the borrow, on the borrowed session; the audit
   event's new `context` object (recorded at every level). Routines owned by MCPDBWIZARD or in
   MCPDBWIZARD_CTX are never published (MCP-UNEXPOSED reason given).
   **Deviation from §0.5:** a failed set does NOT invalidate the factory. Clear-first makes a dirty
   session harmless to the next borrower, and the usual cause (package not granted) would fail
   identically on a fresh connection, so invalidating would only add a logon per refused call.
   **Test config is `generic_test_ctx`, NOT `TGen23aiMcp`** as planned: a required parameter on
   generic_test_23ai would break every existing caller of that server, and a separate config runs
   on all six boxes rather than only the 23ai line. It needs no DDL (an inline statement reads the
   context back), names SET_VALUE to prove the refusal, and pins DAO_POOL_MAX_SIZE=1 so the
   interleave is guaranteed to share one session. `TMcpContext`: 12 checks, all green on ORCL12.
   **NEGATIVE CONTROL RUN AND SEEN TO FAIL** (§11 item 2): with the per-call set removed from the
   emitted server, Oracle saw `customerId: null` and the four value checks FAILED while the four
   refusal checks still passed (they are upstream of the set). Restored, green again.
   `TMcpServerStartup` now supplies each server's declared `MCP_CONTEXT_<NAME>` (read from the
   emitted source, not by loading the class) -- without it the new server's deliberate refusal to
   start appears there as a 90-second initialize TIMEOUT, not as an exit, which is what a stdio
   client would see too. File-count floor: 64 (ORCL12 only so far).
4. **Proxy:** forward the query string to the child (today `McpProxyController` builds the upstream
   URI as a bare `/mcp`). *Test:* `McpProxyEndToEndTest` reads the value back through the proxy.
   **DONE 2026-10-02.** Forwarded VERBATIM (still percent-encoded -- decoding twice would turn %26
   into a separator); the generated server decides which names it accepts. Nothing in the web app
   reads a credential from the query string, so nothing leaks; the comment at the site says to strip
   it if that ever changes. (An unparseable query was a 400, outcome `bad-request`; REMOVED 2026-10-02 -- illegal characters
   and stray % are now encoded so the URI always parses, and the server refuses nonsense per call.) Stub tests
   in `McpProxyControllerTest`; end to end in a NEW `McpProxyContextEndToEndTest` (generic_test_ctx
   through RuntimeManager: exactly one tool, the URL's customer is what Oracle sees, values are
   per REQUEST not per session, a call without it is refused naming it).
5. **Clients:** confirm the query string actually arrives from Claude Code (`.mcp.json`, including
   `${VAR}` expansion), the claude.ai / Desktop custom connector, and Cursor. **If one drops query
   strings, the fallback is a path form**, and that must be known before the docs are written.
   **CLAUDE CODE: DONE 2026-10-02 (2.1.286).** Measured through a logging pass-through proxy in
   front of the generated generic_test_ctx server, with `claude -p --mcp-config` driving it. The
   query string arrives on EVERY request -- `server/discover` (new; sent before `initialize`),
   `initialize`, `notifications/initialized`, the SSE `GET`, `tools/list`, `tools/call` -- and Oracle
   saw the value: (A) a literal `?CUSTOMER_ID=...` works; (B) `${CUST}` in the URL is expanded from
   the client's environment; (C) **with CUST UNSET, Claude Code sends the literal text `${CUST}`,
   unencoded, and the first build RAN WITH IT as the customer** -- matching no rows rather than
   failing. Fixed the same day: `McpContextParams.valueProblem` refuses any value containing `${`
   ("looks like an environment variable the MCP client did not substitute"), and re-measured: the
   connection still succeeds and the CALL is refused with that sentence, which the agent relays.
   The proxy was ALSO wrong for (C): `{`/`}` are illegal in a `java.net.URI`, so `URI.create`
   failed and the proxy answered `initialize` with a 400 -- a client reports that as "cannot
   connect". It now percent-encodes only illegal characters (`McpProxyController.quoteIllegal`,
   hand-written because the multi-argument URI constructors also encode `%`, which would double-
   encode an escape the client already made) and lets the server refuse the call.
   **DOCS POINT from (C):** `${VAR:-}` gives an empty value, which is also refused; either way an
   unset variable is a refusal, never a silent customer.
   **STILL OPEN:** the claude.ai / Desktop custom connector (needs a public HTTPS deployment
   carrying the phase 4 proxy change) and Cursor (not installed here).
6. **Hotel demo:** procedures read `SYS_CONTEXT('MCP','CUSTOMER_ID')`; the demo config declares it.
   **DONE 2026-10-02, as an ADDITION, not a conversion (David's choice).** The existing hotel config
   is left exactly as it was -- it is the STAFF view, its tools take a customer name, and it is
   what `deploy/aws/demo.sh` deploys. Added in the demo repository (the authority):
   `CUSTOMER_PORTAL` in `db/mcp_db_demo_ddl.sql` (my_details, my_bookings, book_rooms,
   cancel_booking, my_complaints, add_complaint -- NONE takes a customer; all read
   `SYS_CONTEXT('MCP','CUSTOMER_NAME')`, refusing ORA-20100 with none and ORA-20101 for an unknown
   customer; cancelling someone else's booking answers exactly as a missing one), and
   `config/mcpdemo_customer.json` declaring **`CUSTOMER_NAME`** (not CUSTOMER_ID: the schema is
   keyed by name) and exposing only the portal, GETROOMLIST, three read-only hotel tables and the
   three customer-neutral lists. The package compiles anywhere, so the AWS demo database simply
   gains an unused package.
   **12.1 caught two compile errors the 23ai line would not have shown first:** a package-private
   function cannot be called inside SQL (PLS-00231 -- copy it to a local), and two separately
   declared REF CURSOR types are not assignable (BookingCursor is a SUBTYPE of ROOM_MANAGER's).
   Loaded VALID on all six estate boxes and checked in SQL*Plus on each; MCPDEMO granted the
   context on each, and `testdata2.sh` now grants it on provisioning. In this repository:
   `Scripts/sync-mcpdemo.sh` now derives `Propfiles/mcpdemo_customer.pb2` the same way as
   `mcpdemo.pb2` (one function, both configs); the config joins the no-arg regen set; harness
   `THotelPortalMcp` (stdio as SUZY BISHOP, HTTP as `?CUSTOMER_NAME=M%20GUSTAVE`).
   The hotel box `endowment` is NOT done: it is outside the estate, and needs `install.sql`,
   `grant.sql MCPDEMO` and the new DDL as a demo step when it is next used.
7. **Docs:** the §0.2 trust statement, the install script, ~~the VPD and SE2 recipes from §6~~ (no VPD, §0.7).
   **DONE 2026-10-02** (not deployed -- the site deploys only via `publish-site.sh`). New page
   `public_website/.../docs/one-customer-per-connection.md` (Curating, order 125): the trust
   statement, install/grant, declaring names, client URL incl. `${VAR}` and stdio, the rules table,
   start-up refusals, clear-first pooling, the PL/SQL pattern (refuse NULL, no customer argument,
   someone else's row answered as missing, no NVL), a VPD recipe (REMOVED 2026-10-02, §0.7) and a
   view note (now: `WITH CHECK OPTION` for any view using SYS_CONTEXT), the audit
   `context` field, the hotel worked example, and which clients are verified. Cross-links from
   `connecting-to-the-mcp-server` and `configs`. `DEPLOYMENT.md`: the proxy forwards the query
   string (section 5), stdio `MCP_CONTEXT_<NAME>` (section 8), four rows in the start-up failure
   table (section 11). `app/CLAUDE.md`: one contributor paragraph. Site builds locally.
8. **Negative controls** — §11 items 1, 2 and 7 still apply as written (pool of size 1, interleaved
   values; delete the set call and watch the test FAIL; a non-VPD box). Add: an undeclared parameter
   is refused; the agent cannot reach `MCPDBWIZARD_CTX`. Items 3–6 are moot under §0.2.
   **DONE 2026-10-02, on ORCL12, with one item that cannot be done here.**
   - Pool of size 1, interleaved customers: `TMcpContext` (phase 3). Green.
   - Delete the per-call set, watch it FAIL: run in phase 3. Oracle saw NULL, value checks failed.
   - **The UNPOOLED branch**, compiled in nobody's tree until now: `generic_test_ctx_nopool`
     (DAO_POOL=NO, 63 files) driven by `TMcpContextUnpooled`, every TMcpContext check. Green.
   - **The agent cannot reach MCPDBWIZARD_CTX**: TMcpContext now CALLS it by the names the generator
     would have used (`mcpdbwizard_ctx_set_value`, `set_value`, `mcpdbwizard_ctx_clear_all`) with a
     `hijacked` value, requires refusal, then checks the configured customer is still in force.
     Green on both twins. (Absent from the list is not the same claim as unreachable.)
   - **The "no tool takes a customer" check can fail**: THotelPortalMcp's checks pointed at the
     STAFF hotel server all three FAILED, as required. **The first attempt at that control passed,
     and proved nothing**: THotelPortalMcp launched its server from the SERVER_CLASS constant, not
     serverClass(), so the override was ignored and it tested the customer server again. Same
     defect was in TMcpContext; both now use serverClass() throughout. A control that passes is a
     reason to look at the control.
   - (VPD material below is HISTORY: VPD was dropped from the docs on 2026-10-02, §0.7.)
   - **The documented VPD recipe, run by hand** on ORCL12 (12.1) and FREE26 (26ai) as written on the
     docs page: no customer -> 0 rows (fails closed), A -> 2, B -> 1, and B's UPDATE touched 1.
     Scratch objects dropped and verified gone.
   - **A box WITHOUT VPD: NOT POSSIBLE on this estate** -- every box is EE, XE or Free, all of
     which have VPD. The docs' SE2 view recipe is therefore untested; it is the ordinary
     grant-the-view pattern, but untested is untested.

### 0.7 Settled 2026-10-02 (David)

- **Every declared parameter is REQUIRED.** One missing from the URL refuses the call, naming it,
  before anything reaches Oracle (§9.3's fail-closed reasoning). No `OPTIONAL` marker for now.
- **stdio reads the same declared names from environment variables**, `MCP_CONTEXT_<NAME>` (e.g.
  `MCP_CONTEXT_CUSTOMER_ID`), fixed for the life of the process. Same required rule. Same trust model
  (§3.1). This is what lets the stdio harnesses (`TGen23aiMcp`) exercise the feature.
- **`MCPDBWIZARD_CTX` lives in its own small account**, not the application schema. Proposed name
  `MCPDBWIZARD`, holding only the package. Locked after install: `NO AUTHENTICATION` (schema-only
  account) on 18c and later; **12.1 has no schema-only accounts**, so there the script uses
  `ACCOUNT LOCK` with an impossible password — one script, branching on version.
- **Free tier.** No licence check; available on the one-running-server free licence.
- **NO dependency on Oracle VPD, and the docs do not recommend it** (David, 2026-10-02): Virtual
  Private Database (`DBMS_RLS`) is an expensive Enterprise Edition feature and the product does not
  need it. The design assumes the developer reads `SYS_CONTEXT('MCP', ...)` in their own PL/SQL
  and curated SQL. The VPD material in §6, §8 (phase 5), §9.4, §10.4 and §11 item 7 below is
  SUPERSEDED by this. Nothing in the product ever used VPD; it was only ever documentation.

### 0.8 Skipped or still open (2026-10-02) -- read this before calling the feature finished

**Clients (phase 5)**
- **Cursor: NOT TESTED.** Not installed on the development machine.
- **claude.ai / Claude Desktop custom connector: NOT TESTED.** Needs a public HTTPS deployment
  carrying the phase 4 proxy change, i.e. a release.
- ~~What our server answers to Claude Code's `server/discover`~~ -- RESOLVED 2026-10-02.
  `server/discover` belongs to MCP revision 2026-07-28 (stateless, no `initialize`); no Java SDK
  release supports it (2.0.1 stops at 2025-11-25), and the spec's backward-compatibility rules say
  how a client detects a legacy server like ours: over stdio any error (ours: a clean JSON-RPC
  `-32601 Method not found`), over HTTP a `400` whose body is not a 2026 error (ours: 400). Both
  land in the fallback-to-`initialize` branch, which is why Claude Code worked. **Do not fake a
  `server/discover` reply** -- claiming a revision we do not speak would break that fallback.
  **Measuring it found a REAL defect**: SDK 2.0.0's HTTP transport writes its own errors (no
  session, unknown session, unparseable message, any method rejected before `initialize`) as the
  whole `McpError` EXCEPTION OBJECT -- stack trace, class and file names, line numbers, Jetty and
  JDK versions -- and the proxy carried it to the caller. Every generated HTTP server ever shipped
  did this. Fixed: `McpErrorBodyFilter` (first on `/mcp/*`, emitted for EVERY MCP server, so all
  generated output changes by one line) and the proxy both rewrite such bodies to
  `{"jsonrpc":"2.0","id":null,"error":{"code","message"}}`, keeping the status so the 2026
  fallback still works. The proxy rewrite covers servers nobody has regenerated.

**Verification breadth**
- **ORCL12-only results:** `TMcpContext`, `TMcpContextUnpooled`, `THotelPortalMcp` and both e2e
  web tests ran on ORCL12 alone, and the `generic_test_ctx` (64), `generic_test_ctx_nopool` (63)
  and `mcpdemo_customer` (106) floors were measured there too. A six-box estate run is owed.
- ~~The unpooled emission path~~ -- CLOSED in phase 8 (`generic_test_ctx_nopool`).
- ~~Phase 8 negative controls~~ -- CLOSED in phase 8.
- ~~The SE2 view recipe untested, and the edition claims unsourced~~ -- MOOT 2026-10-02: VPD is gone
  from the docs (§0.7), and with it every edition claim. What survives is edition-independent and
  MEASURED on ORCL12: through a PLAIN view filtering on SYS_CONTEXT, customer A can insert a row for
  B and move its own rows to B; `WITH CHECK OPTION` refuses both (ORA-01402) and allows A's own.
  The docs say so.
- ~~No check that the customer-argument test can fail~~ -- CLOSED in phase 8; and the first
  attempt at it exposed harnesses that ignored a serverClass() override.

**Shipping**
- **Not released.** No image carries any of this yet; GCP/AWS/Marketplace deployments get it at the
  next `release.sh`. Release notes not written.
- **Open-source export not run.** The docs page links to `db/mcp-context/` in mcpdbwizard-open,
  which 404s until the next export.
- **Docs not deployed** (`publish-site.sh`).
- **The hotel box `endowment` is not set up:** needs `install.sql`, `grant.sql MCPDEMO` and the new
  DDL as a demo step. Outside the estate.

**Product gaps noticed along the way, not fixed**
- ~~Runtime page URL without the `?NAME=` placeholders~~ -- FIXED 2026-10-02: the endpoint shows
  `?CUSTOMER_NAME=<value>` (placeholder, never an example value) and says the values are required.
  Names from the SAVED config (`RuntimeManager.contextParamsFor`). Render-tested.
- ~~The console not checking the database side~~ -- FIXED 2026-10-02: Start now runs
  `RuntimeManager.contextPreflightProblem` BEFORE generating -- connect as the config's account,
  make the clear-context call, and fail in seconds naming install.sql/grant.sql on PLS-00201. Any
  other failure returns null and generation reports it as before (no second source of connection
  errors). Live-tested by `ContextPreflightTest` with CHARGLT, which the estate never grants.
- ~~A stdio refusal looking like a timeout~~ -- FIXED 2026-10-02, by REVERSING phase 3's exit-2:
  a missing `MCP_CONTEXT_<NAME>` and a not-installed/not-granted package are now logged at
  start-up and the server keeps running, refusing each call with the same sentence (the
  not-installed case recovers without a restart, since nothing is cached). An exit reached a stdio
  client only as a connection that never initialised.
- ~~MCP-CALL and MCP-ACCESS lacking the context~~ -- FIXED 2026-10-02: the generated server's
  MCP-CALL line ends with a `context` object (values, unlike argument values; absent when none
  declared, so other servers' lines are unchanged), checked end to end by TMcpContext reading the
  server's own output. The proxy's MCP-ACCESS line and audit event carry the query parameters AS
  SENT (`McpAccessRecord.queryContext`) -- not as accepted; the server's own line says that.
  Full ORCL12 regen + suite was still running when this was committed.
- The **demo repository README** describes `mcpdemo.json` as 2 sequences and 4 SQL statements; the
  file has 1 and 5. Predates this work.
- **`DbTestSupport` gives a login 5 seconds**, so a slow box makes whole live-test classes skip
  (seen on XE18 and ORCL21 on 2026-10-02). Not specific to this feature.
- **FREE23 restarted unexpectedly** on 2026-10-02; cause unknown. `EXTPDB1` was still MOUNTED at
  the last look (saved state says OPEN).
- ~~Scratch processes on ports 18431/18432~~ -- stopped 2026-10-02. A Cursor test needs them
  restarted: the generic_test_ctx server with `http 18431`, and a logging pass-through in front.

---

## 1. The problem

An MCP client is operating on behalf of one customer. Every statement in the system must be scoped to
that customer's rows. The agent must not be able to change which customer that is — and nor, in the
multi-tenant case, may the person running the client.

So: where does the customer number come from, and what enforces it?

This is the note §3.1 of the security review points at when it says an RLS policy driven by a
per-call application context "remains possible, but nothing in the generated code does that today,
and adding it would need a per-call identity to set it from". That identity now exists in the proxy.
The carriage from there to the Oracle session does not.

## 2. Why the obvious answer is wrong, and the rule that replaces it

The obvious answer is a `customer_id` parameter on every tool, with a description saying to pass the
caller's own. That is not a control, it is a prompt.

**Everything in a tool's input schema is agent-controlled.** That is what a schema is *for*. A
description is advisory text handed to a model free to ignore it, and "the model usually complies" is
the same class of claim as "the validator usually catches it" — a filter over an input space nobody
can enumerate. The site makes this argument at length for SQL prompts in
[*Why the safest SQL prompt is the one that does not exist*](https://mcpdbwizard.com/writing/why-the-safest-sql-prompt-does-not-exist/).
It is the same argument, and the same conclusion: the fix is not a better filter, it is not having
the input.

MCP's `_meta` is not a way out — it travels in the JSON-RPC body, no client exposes it as user
configuration, and nothing in the protocol privileges it. **There is no trusted-parameter channel in
MCP, and there should not be one:** a field the client fills and the model cannot is still a field
the client's *user* fills.

> **The rule: do not transmit the customer number. Derive it from the credential.**

The client presents who it is; the server decides what that means, from a mapping the client can
neither see nor edit. Changing customer then requires holding a different credential, and issuing
credentials is already an administrative act with a trail. Everything below applies this one rule.

## 3. What each transport offers

### 3.1 stdio — `env` in the client's server entry

```json
"mcpdbwizard": { "command": "...", "env": { "CUSTOMER_ID": "4711" } }
```

Process-scoped, never enters the model's context window, unreachable from a tool call. It composes
with the existing `FROM_ENV_VARIABLE_DB_PASS` sentinel
(`app/src/main/java/com/mcpdbwizard/app/common/DbPasswordSource.java:37`) — a
`FROM_ENV_VARIABLE_CUSTOMER_ID` would be the same shape.

**Be honest about what it is: a deployment fact, not a security boundary.** Whoever owns the machine
owns that file. It scopes the *agent*; it does not scope the *human*. Legitimate when the process is
one-per-tenant and the tenant is trusted with their own identity. Useless for defending customer A's
rows from customer B.

### 3.2 Streamable HTTP — the bearer token

`Authorization: Bearer <id>.<secret>`, set once in the client's server entry. The model never sees
it, and — unlike the env var — **the value is issued by us**, so a client cannot mint a different
customer, only present a token it was given. That is the difference between a hint and a control, and
it is why this is the shape to build on. HTTP is the deployed shape regardless.

The same reasoning covers an OAuth access token carrying a tenant claim: same rule, more machinery.

## 4. What works today, with nothing built

For a bounded number of customers this is already solved by the model the product rests on.

Configs are owned and addressed `/mcp/<owner>/<config>`; the access matrix decides who may reach
which; `McpProxyController` authorises **before** admitting that a config exists. So:

- one config per customer, generated against that customer's scope — a per-tenant Oracle account, a
  per-tenant view, or the customer number bound into the curated statements;
- an API token granting access to exactly one of them.

The customer number is then fixed at generation time and **absent from the tool surface entirely**.
The agent cannot supply the wrong one for the same reason it cannot reach `PAYROLL.SALARY`: there is
no name to say. This is "authorization is the config file" applied to tenancy rather than to roles.

**Cost, stated plainly:** N customers means N configs, N generations and N running servers, against a
licence that counts running servers. Comfortable at tens. Not viable at thousands.

**This is why the rest of this plan is deferred rather than scheduled.** Nobody has yet asked for the
case §5 serves.

## 5. The mechanism, verified

One running server, many tenants, resolved per call. Four hops, all of whose hooks exist.

**(a) Resolve.** `ApiTokenAuthenticationFilter:110` already reads the token and establishes the
caller. Add a tenant attribute to the token record; resolve token → `customer_id`.

**(b) Carry it to the child.** `McpProxyController` is the natural home — it is already where
per-caller decisions live, and its own class comment says so (the quota went there for exactly this
reason). It sets `X-MCPDBWizard-Customer-Id` on the upstream request.

**This is forgery-proof for one reason worth writing down: `FORWARDED_REQUEST_HEADERS`
(`McpProxyController.java:85`) is a closed allowlist of five headers.** A caller who sets that header
themselves has it dropped, not forwarded, so the child sees only what the proxy put there. **If
anyone ever turns that allowlist into a denylist, this design silently becomes forgeable.** That is
the single line of code the whole scheme depends on, and it should carry a comment saying so.

**(c) Reach the connection.** Expected to be the expensive hop. It is not — checked against SDK 2.0.0
and the emitter, 2026-09-21:

| Need | What already exists |
|---|---|
| Lift an HTTP header into per-call state | `HttpServletStreamableServerTransportProvider.Builder.contextExtractor(McpTransportContextExtractor<HttpServletRequest>)` — and the generator already emits that exact builder at `SAAdminWrangler.java:5623` |
| Read it at handler time | `McpSyncServerExchange.transportContext()` → `McpTransportContext.get(String)` |
| Have it in scope where the factory is borrowed | **`theExchange` is already a parameter of the emitted `call(...)` funnel** (`SAAdminWrangler.java:5882`), which *every* tool passes through |
| A single set-on-borrow point | `call(...)` borrows at one site: `thePool.withFactory(theFactory -> theOperation.run(theFactory))` |
| A way to act on the factory's connection | The factory already carries `theConnection` and already has `setModuleName(String)` emitted onto it (`:13875`), calling `SessionInfo.setModule(theConnection, …)` (`:15248`) |

So the identity arrives in the same method that borrows the factory, and that method already has both
ends. Nothing has to be threaded through the 90 handlers.

**(d) Enforce in the database.** §6 — and note this is the customer's DDL, not ours.

## 6. The Oracle half, so it cannot be overwritten

**`SYS_CONTEXT` only reads.** The write is `DBMS_SESSION.SET_CONTEXT`, and the tamper-resistance is
not in the call but in how the namespace was created:

```sql
CREATE CONTEXT mcp_ctx USING mcp_owner.mcp_ctx_pkg;
```

Only `mcp_owner.mcp_ctx_pkg` may set that namespace. Any other code calling
`DBMS_SESSION.SET_CONTEXT('MCP_CTX', …)` gets `ORA-01031`, **including code running as the connecting
account**. That is what makes it a control rather than a convention.

Then let the *database* apply the predicate, rather than trusting every curated statement to carry
it:

```sql
FUNCTION cust_filter(sch VARCHAR2, obj VARCHAR2) RETURN VARCHAR2 IS
BEGIN
  RETURN 'customer_id = SYS_CONTEXT(''MCP_CTX'',''CUSTOMER_ID'')';
END;
```

registered per table with `DBMS_RLS.ADD_POLICY`. Two properties follow, and they are the whole reason
to do it here rather than in the SQL:

- **It fails closed.** Context unset → `SYS_CONTEXT` returns NULL → `customer_id = NULL` matches no
  rows. **Do not wrap it in `NVL` to anything.** A misconfigured server returning nothing is the
  correct failure.
- **A statement nobody scoped is still scoped.** Curation moves authorship of the SQL from the model
  to your team; it does not audit your team. This layer covers the omission, including a `SELECT`
  added next year by someone who never read this plan.

**Edition caveat, and it is not a footnote.** `DBMS_RLS` is Enterprise Edition (and Oracle Free).
**Standard Edition 2 has no VPD.** The equivalent there is views carrying the predicate with object
grants only on the views — which fits the product well, since the config already chooses which
objects exist. Our own estate spans both lines, so a design assuming VPD is half a design.

**Attribution, alongside.** `DBMS_SESSION.SET_IDENTIFIER(customer_id)` lands in
`V$SESSION.CLIENT_IDENTIFIER` and in Oracle's native audit records, partly repairing §3.1's
consequence that Oracle's own auditing sees one account for every caller. It is **not** a protected
namespace — anyone in the session can change it — so it is attribution, not authorization. In our
architecture nobody in the session can run arbitrary SQL, so the weakness is theoretical here; do not
build the security decision on it regardless.

## 7. The hazard: pooling

**This is the part that will be got wrong, so it is a section and not a bullet.**

An application context lives on the **session**. We pool DAO factories, and roughly 90 MCP handlers
borrow one per call. A factory that served customer 4711, was returned, and is then borrowed for
customer 5822 will run 5822's call **under 4711's predicate** — silently, returning a plausible
number of rows belonging to the wrong customer. There is no error, and the result looks right.

**Set the context on every borrow.** Not on connect: connect happens once and the pool outlives it.
Not *only* on return: a call that times out, throws, or dies with the JVM may never reach the return
path, and a cleanup that runs "almost always" is a cross-tenant leak with a low duty cycle.
Set-on-borrow is idempotent and always runs. Clearing on return as well is cheap and worth having as
a second line — second, not first.

Partitioning the pool per tenant is the alternative and gives away the whole benefit of pooling:
pooled factories are interchangeable *only* because every one is the same principal (§3.1). A
per-tenant pool is N pools.

## 8. Phases

Each phase is independently testable and leaves the tree green. **Phase 0 is the one to do first even
if the rest is never built.**

**Phase 0 — decide the trust model and write it down.** No code. Settle §9.1–§9.4. The deliverable is
this file's Decisions section filled in, because the failure mode in §10.1 is invisible to every test
we could write and can only be prevented by a decision.

**Phase 1 — the proxy hop.** Tenant attribute on the API token record; `McpProxyController` resolves
it and sets `X-MCPDBWizard-Customer-Id` upstream. Add the comment to `FORWARDED_REQUEST_HEADERS`
saying that the allowlist is load-bearing. *Testable alone:* a web-layer test asserting the header is
set from the token and that a caller-supplied copy is dropped. Nothing downstream consumes it yet, so
this phase ships dormant.

**Phase 2 — the trusted-proxy flag.** The config flag from §9.2, defaulting to off, plus the
emitted-server behaviour when it is off (ignore the header) and when it is on but the header is
absent (§9.3). *Testable alone:* two generated servers, one each way.

**Phase 3 — extraction and carriage in the emitted server.** `.contextExtractor(...)` at
`SAAdminWrangler.java:5623`; read `theExchange.transportContext()` in `call(...)` at `:5882`.
*Testable alone:* the value reaches the handler and is visible in the audit record, before anything
touches Oracle. **Put it in the audit record in this phase** — it is the cheapest possible proof that
carriage works, and it is wanted anyway.

**Phase 4 — the session context.** `SessionInfo.setContext`; emit `setCustomerContext(String)` onto
the factory beside `setModuleName`; call it **inside** the `withFactory` lambda, before
`theOperation.run`. *Testable alone:* `SYS_CONTEXT` read back through a tool on a live box.

**Phase 5 — the database side, as documentation.** The `CREATE CONTEXT` / `DBMS_RLS` recipe and the
SE2 view alternative, on the docs site. §9.4 decides whether we also emit a template.

**Phase 6 — the negative controls.** §11. Not optional, and not folded into the phases above: every
phase's own test can pass while the feature does nothing.

## 9. Decisions wanted before anyone starts

**9.1 Which tier are we building for?** §4 covers tens of tenants with no code. §5 is a feature
crossing the proxy, the emitter and the runtime. *Nobody has asked for §5.* If the answer is "we are
not at thousands of tenants", the correct outcome of this plan is that it stays unbuilt and §4 gets a
docs page instead.

**9.2 Where does the customer scope live — the config or the token?** Config is today's answer and
keeps authorization in one place, which is the product's stated model. Token lets one config serve
many tenants but splits the model across two files, and "where is this enforced?" stops having one
answer.

**9.3 What is the trusted-proxy flag called, and what happens when the header is absent?** It must
default to **off**. With it on and the header missing, the server must **refuse the call**, not run
it unscoped — fail-closed at the MCP layer as well as in the predicate, so a misconfiguration is a
visible error and not a query that quietly returns nothing. (Both fail closed; only one is
diagnosable.)

**9.4 Do we ship the Oracle DDL, or document it?** The `CREATE CONTEXT` / `DBMS_RLS` work is the
customer's schema, not ours. We could emit a template or write the docs page and stay out of their
schema. The `oracle-user-privileges` docs page is the precedent for the second.

**9.5 Is stdio in scope?** §10.2. The transport memo says stdio is test-only, which argues for
leaving it unsupported and failing loudly.

## 10. Risks

### 10.1 A trusted header is only trusted because of what sits in front of it

In the deployed shape the child binds loopback and holds the proxy's bearer token, so the proxy is
the only thing that can reach it, and the allowlist drops forged copies. **But the generated server
also ships standalone** — customers run the image themselves, and `TGen23aiMcp` drives it directly. A
standalone server trusting `X-MCPDBWizard-Customer-Id` unconditionally lets anyone who can reach the
port choose their own customer, which is **worse than not having the feature**, because it looks like
a control.

Hence §9.3's flag, off by default, exactly as `X-Forwarded-For` is handled everywhere else. **This
will look like it works in every test either way**, which is what makes it the top risk rather than a
detail.

### 10.2 stdio cannot do this at all

`StdioServerTransportProvider` (`SAAdminWrangler.java:4967`) has no headers and no extractor. That
path needs the §3.1 env var instead, so the emitted code would carry **two** mechanisms chosen by
transport — and a reader would reasonably assume the HTTP one applies. See §9.5.

### 10.3 The silent wrong answer

Every other failure in this system is loud: a missing tool, `ORA-00942`, a pool exhaustion. This one
returns the wrong customer's rows with a 200. §7 is where it comes from, §11 is the only thing that
catches it.

### 10.4 Edition split

A VPD-shaped design silently degrades to nothing on SE2. Whatever is built must be verified on a box
without VPD, or the docs must say plainly that it is EE-only.

## 11. Verification, including the negative controls

**This repository has a documented history of controls that did not control** — a `git stash` that
silently stashed nothing, a bogus-repo test that passed because a different collector answered, an
exclusion list measured against its own default because `${VAR:-default}` substitutes on empty. A
feature whose failure mode is *a plausible wrong answer* cannot be signed off on a passing test.

So, in addition to each phase's own test:

1. **Two tenants, one server, interleaved.** Tokens for 4711 and 5822 calling the same tool through
   the same pool, alternating, enough times to force factory reuse. Each must see only its own rows.
   **Run it with a pool of size 1**, which guarantees the reuse §7 describes rather than hoping for
   it.
2. **The control that must FAIL.** Delete the `setCustomerContext` call and re-run (1). If it still
   passes, the test is proving something else — most likely that the fixture data does not overlap.
   **Do not accept (1) as evidence until this has been seen to fail.**
3. **Forgery.** A client setting `X-MCPDBWizard-Customer-Id` itself must not affect the result. Then
   temporarily widen `FORWARDED_REQUEST_HEADERS` and confirm the same test now *fails* — otherwise it
   is not testing the allowlist.
4. **Flag off.** With the trusted-proxy flag off, the header must be ignored. Assert the *rows*, not
   the log line.
5. **Header absent, flag on.** Must be a refusal with a diagnosable message, not an empty result set
   (§9.3).
6. **Direct-to-child.** Bypass the proxy, hit the child on its port with a chosen header, and confirm
   §10.1 is closed.
7. **On a box without VPD**, confirm the documented behaviour is what actually happens (§10.4).

Measure by rows returned, never by whether a call succeeded: **status is a claim about the call, not
about the state.**

## 12. Alternative: Oracle proxy authentication

If the tenant count is small, skip the context plumbing:

```sql
ALTER USER cust_4711 GRANT CONNECT THROUGH mcp_agent;
```

and connect as `mcp_agent[cust_4711]`. The session then really *is* that user: `USER` returns it,
object grants discriminate, native auditing discriminates, no application context is involved, and
§7's silent failure cannot occur in that form.

The cost is one database user per tenant, plus a pool keyed by tenant for the same reason as §7.
Worth it at tens; unavailable at thousands. **Between this and §4, the small-tenant case has two
working answers that need no new code** — which is the strongest argument for leaving §5 unbuilt.

## 13. What not to do

- **Do not add a `customer_id` tool parameter documented as "pass the caller's own".** §2.
- **Do not accept that parameter and validate it against the token.** If it is in the schema the
  model spends tokens on it, gets it wrong, and the mismatch becomes an error it tries to work
  around. Take it off the surface instead.
- **Do not `NVL` the context lookup.** §6.
- **Do not set the context on connect.** §7.
- **Do not turn `FORWARDED_REQUEST_HEADERS` into a denylist.** §5(b).
- **Do not default the trusted-proxy flag to on**, however convenient the demo. §10.1.
