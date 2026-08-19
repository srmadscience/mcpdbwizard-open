# Security review of the generated MCP server

Review of `<Factory>McpServer` as emitted by `SAAdminWrangler.generateMcpServerClass`, carried out
2026-08-03 against the MCP specification's transport and authorization rules.

**Verdict: the mechanisms are sound; the defaults were not.** Appropriate for the deployment shape
that actually exists — a server on a trusted host driven by one client — but it was not safe to put
on a network. Notably, three of the seven findings were configuration choices rather than missing
features.

Two findings are fixed (§2). One was reviewed and **deliberately accepted** (§3.1). Four remain open
(§3.2–§3.5); each needs a decision about the product, not an edit, which is why none of them was
quietly implemented.

---

## 1. What the review found to be right

Recorded because it is load-bearing, and because a later change that breaks any of it would be a
regression rather than a refactor.

- **Both auth features fail closed.** A missing `MCP_HTTP_TOKEN`, or an unreadable
  `MCP_TLS_KEYSTORE`, logs and `System.exit(2)`s. Neither degrades silently to unauthenticated or to
  plaintext — the failure mode that makes optional security features worthless.
- **Secrets are read from the environment at run time, never baked into generated source.**
- **The bearer comparison is constant-time** (`MessageDigest.isEqual`), so the token cannot be
  recovered by timing.
- **TLS mode builds `new Server()` with only an SSL connector**, so there is no stray plaintext port.
- **Curation happens at generation time.** An unexposed operation has no tool-spec method emitted at
  all: it is absent from the binary rather than merely unregistered at run time. That is a much
  stronger property than a runtime filter, and it should stay that way.
- **The refusal to ship a "read-only mode"** was correct. Nothing in `ALL_ARGUMENTS`/`ALL_OBJECTS`
  says whether a PL/SQL routine writes, and the generator never parses bodies — so such a switch
  could only filter the structurally-knowable surfaces while every exposed procedure stayed callable
  and free to write. A control that reads as a guarantee and is not one is worse than no control.

## 2. Fixed

Both were MCP transport security rules we did not implement, and both are now always-on, configured
from the environment through `com.mcpdbwizard.pub.McpHttpPolicy` rather than by a propfile flag — a
spec MUST should not be something a config can forget.

### 2.1 No `Origin` validation (MUST-level violation)

The spec requires that a server **MUST** validate the `Origin` header and answer **403** when it is
present and invalid. We did not check it at all.

This matters more than it first appears. DNS rebinding works by making the attacker's page look
**same-origin** to the browser — so the same-origin policy never engages, no preflight is sent, and
CORS is not in the path. The server-side `Origin` check is the only thing left. Against a database
MCP server with CRUD tools, the payoff for an attacker is direct read/write of the schema from any
page the operator happens to visit.

Now an always-emitted `jakarta.servlet.Filter` on `/mcp/*`, registered **before** the bearer filter
so a rebinding attempt is refused on the cheaper check. An **absent** `Origin` is allowed — no MCP
client outside a browser sends one, and the requirement concerns a header that is present and wrong.
The literal `null` that sandboxed and `file:` contexts send is refused, as is anything malformed.

Default allowlist is loopback only. `MCP_ALLOWED_ORIGINS` (comma-separated) **replaces** that default
rather than extending it, so the variable always states the whole allowlist; include the loopback
form explicitly if a local browser client still needs it. The single value `*` disables the check.

### 2.2 Bound to every interface

The emitted `http` branch was `new Server(thePort)` — Jetty's default of all interfaces — on a server
whose bearer token and TLS are **both opt-in**. Combined with §2.1 and the web UI's defaults (§3.5),
an out-of-the-box configuration produced an unauthenticated, plaintext MCP server reachable from the
LAN with full CRUD over the selected schema.

Now `127.0.0.1` unless `MCP_HTTP_HOST` says otherwise, per the spec's "when running locally, servers
SHOULD bind only to localhost".

**Consequence worth knowing:** publishing 8090 out of the container now requires
`MCP_HTTP_HOST=0.0.0.0`. That is deliberately not set in the `Dockerfile` — doing so would re-create
an all-interfaces default in the one deployment most likely to be reachable — and is documented at
the `EXPOSE` line instead. `RuntimeManager.waitForPort` probes `127.0.0.1`, so the web Runtime path
needs no change.

## 3. Open

### 3.1 Every caller is the same Oracle user — ACCEPTED, not a defect

**Decision (2026-08-03): this is the expected design and is left as is. Do not "fix" it.**

The server authenticates *to* Oracle, not its callers: one connection per factory using the account
fixed at generation time. That is the ordinary three-tier arrangement — the generated server is an
application connecting with a service account, not a proxy for end-user sessions — and it is what
makes connection pooling possible at all, since pooled factories are only interchangeable because
every one of them is the same principal.

What follows from accepting it, recorded so the consequences are not rediscovered as surprises:

- **The Oracle account's privileges are the real security boundary.** With no per-caller identity to
  restrict, what bounds a tool is what the account can do. Grant it least privilege — only the
  objects the config exposes, and only the operations those tools need. Together with curation at
  generation time (§1), that is the access-control model: *the config decides what exists, the Oracle
  grant decides what it can reach.*
- **Per-user attribution cannot come from the database.** Oracle's own auditing sees one account for
  every caller, so an audit trail has to live in the MCP layer instead. This raises the value of
  §3.4 considerably — it is now the only place a "who did what" record can exist.
- **Row-level security keyed to `USER`/`SYS_CONTEXT('USERENV', …)` will not discriminate.** An RLS
  policy driven by an application context that the server sets per call remains possible, but nothing
  in the generated code does that today, and adding it would need a per-call identity to set it from.

### 3.2 Database credentials baked into generated source and config — MECHANISM ADDED

A config may now write **`FROM_ENV_VARIABLE_DB_PASS`** wherever the password would go — as the
`PASS` value, or inside the `DAO_CONNECTION_NAME` JDBC URL — and the real secret is read from the
**`DB_PASS`** environment variable instead. See `com.mcpdbwizard.app.common.DbPasswordSource`.

**Correction to the original finding.** It said the password is emitted as
`protected String thePassword = "…";`. That is true only for **DB2**: the `theUser`/`thePassword`
fields sit behind `oracleVersion.startsWith("DB2")`. On the Oracle path the generated factory
connects with `getConnection(theUrl)` and **no credential properties at all**, so the password
travels *inside the URL* — `SAAdminWrangler.java:11122` — and that, not a password field, was the
real leak. Fixing only `PASS` would have left it entirely intact.

**Why the sentinel survives into the generated code.** One value is used twice: the generator
connects with it to read the dictionary, and it is written into the generated factory for that code
to connect with later. Resolving once at config load would fix only the first — the real password
would still be substituted into the emitted source, which is the copy that gets deployed. So
resolution differs by point of use: `ConnectionWrangler` calls `resolve` when it opens its own
connection and keeps the sentinel form everywhere else, while the emitter calls `toJavaExpression`,
which produces `"…" + passwordFromEnvironment() + "…"` — source that reads the environment at run
time. `mrPassword` and `mrURL` therefore keep the configured value deliberately; resolving them
earlier would silently defeat the whole mechanism.

Both ends are fail-closed. Generation aborts with a message naming `DB_PASS` if it is unset; the
generated factory throws an `IllegalStateException` naming the variable and the class, raised from a
field initialiser so it surfaces immediately rather than as an Oracle login failure that blames the
database.

Verified against FREE23: with `DB_PASS` set, generation succeeds and the generated code connects for
real; with it unset, generation aborts and the generated code throws; with it *wrong*, the value
still reaches Oracle and comes back `ORA-01017`, which is what proves the variable is genuinely
feeding the connection rather than being ignored. Across the generated tree there are **zero**
occurrences of the password and **zero** of the sentinel.

**The secret may come from a file, not just a variable** (added 2026-08-03). `DB_PASS_FILE` names a
file whose contents are the password — the convention the postgres and mysql images use, and the
shape of a Docker Swarm secret or a Kubernetes secret volume. It beats the plain variable on four
counts: absent from `docker inspect`, not inherited by child processes (this app forks the
generator and the MCP server), tmpfs-backed rather than on a disk layer, and rotatable by
replacing the file. Both set at once is an error rather than a precedence rule. A trailing newline
is stripped (`echo secret > file`); a trailing space is not, since it may be the password.
Shared by the generator and the emitted code through `com.mcpdbwizard.pub.EnvironmentSecret`.

**Opt-in, and inert otherwise.** A config carrying a literal password emits exactly what it emitted
before — a plain quoted literal, with the helper method not emitted at all.

**The web UI no longer asks for an Oracle password at all** (2026-08-03 — the open UX question is
settled). The Design connect form takes host, port, SID and username; `DesignSession` reads the
secret from `DB_PASS`/`DB_PASS_FILE`, and `SchemaDefaults.applyConnection` writes the **sentinel**
into both `PASS` and the `daoConnectionName` URL. So a config saved from the web UI carries no Oracle
secret, and `OracleCredentials` no longer has a password component for one to hide in — the compiler
enforces that rather than a convention.

This follows the §3.1 decision: one Oracle account means one credential, and it belongs in the
environment beside the bearer token and the TLS keystore rather than in a form. It also removes a
doc/behaviour mismatch — `OracleCredentials` claimed the password was "kept only in server-side
session memory", while `DesignController` was in fact routing it into the saved config.

### 3.3 A static shared token is not MCP Authorization — IMPLEMENTED

**`MCP_OAUTH=YES`** makes a generated server an OAuth 2.1 **resource server**
(`com.mcpdbwizard.pub.McpOAuthPolicy`). It validates access tokens issued by an external
authorization server and is deliberately **not** an authorization server itself — the spec puts that
role elsewhere, and building one was never in scope.

The four things the spec requires of the resource-server role:

1. **Protected Resource Metadata (RFC 9728)** at `/.well-known/oauth-protected-resource`, naming the
   authorization server and any required scopes. Served **outside** the auth filter, because a client
   with no token yet must be able to read it to discover where to get one.
2. **Token validation** against the authorization server's JWKS (cached, so keys are not fetched per
   request and a rotation does not need a restart).
3. **Audience binding** — the token must name *this* server. This is the requirement that stops a
   valid token for some other API being replayed here, which matters most when several services share
   one authorization server.
4. **401** with a `WWW-Authenticate` challenge carrying `resource_metadata`, and **403** with
   `error="insufficient_scope"` and the scopes needed, when the token is valid but too weak.

**Why a library verifies the signature.** Nimbus JOSE+JWT (one jar, no transitives, `<optional>`).
Hand-rolled JWT checking fails in well-catalogued ways — `"alg":"none"` accepted, an HMAC token
verified against the public key, expiry unchecked — and each turns the control silently into a no-op.
The accepted algorithms are declared up front and are **asymmetric only**, which is what closes the
confusion cases. Both forgeries have explicit tests.

**Mutually exclusive with `MCP_HTTP_TOKEN`, and the server refuses to start if both are set.** They
read the *same* `Authorization: Bearer` header, so no single request could satisfy both — letting one
silently win would be worse than refusing.

OAuth also satisfies the §3.5a exposure guard, since it is authentication; otherwise an
OAuth-protected server could never be bound off loopback.

Verified live on FREE23 against a stand-in authorization server (real RSA keys, real JWKS endpoint,
real signatures): metadata readable without a token; no token → 401 with the challenge; **valid token
→ 200 on a genuine MCP `initialize`**; wrong audience → 401; expired → 401; valid but missing scope →
403. Reasons are logged server-side and never returned, so a caller is not told which check failed.

**Not covered, deliberately:** Client ID Metadata Documents and Dynamic Client Registration are
*client* and *authorization-server* concerns, not a resource server's.

### 3.3-was A static shared token is not MCP Authorization (original finding)

MCP defines an OAuth 2.1-based authorization scheme (resource server, audience validation, Client ID
Metadata Documents now replacing Dynamic Client Registration). A shared secret gives no per-caller
identity, no scopes, no expiry, and no revocation short of a restart. Adequate for a single trusted
client; not for multi-tenant or enterprise use.

Note this is **not** blocked by §3.1, despite the two sounding alike. Accepting one Oracle account
settles who the *database* sees; it says nothing about who may reach the server. Per-caller identity
at the MCP layer would still buy scoped access, expiry and revocation — and it would supply the
caller identity that an audit record (§3.4) otherwise has no way to name. What it cannot do, given
§3.1, is propagate that identity into Oracle.

### 3.4 No audit trail — RESOLVED: per-call records, a pluggable sink, and a write-ahead spool

**The record now names the caller too** (2026-08-04). Everything below says *what* happened; for a
while nothing said *who* asked for it, which was unavoidable while the generated server was the only
participant — §3.1 accepts that it sees one Oracle account, and a shared bearer token is a door key,
not an identity. The `/mcp/{config}` proxy supplies the missing half, and now writes an
`MCP-ACCESS` line of its own through `com.mcpdbwizard.web.security.McpAccessRecord`:

```
MCP-ACCESS {"user":"sam","config":"payroll","op":"tools/call","tool":"payroll_get_by_pk","outcome":"ok","status":200,"ms":41}
```

The two records are complementary rather than redundant, and it is worth knowing which answers what:
this one answers *who called what, and was it allowed*; the `MCP-CALL` line below answers *what the
tool did, with which argument names, and how it ended*. Correlate by config, tool and time — there is
deliberately no shared request id, because minting one would mean the generated server had to accept
and echo a header from its caller, which is a generator change and a new trust assumption for a line
of logging.

Three details that are decisions rather than omissions:

- **Written in a `finally`, like its sibling**, so `forbidden`, `not-running`, `unknown-config` and
  `unavailable` are recorded as readily as `ok`. A refusal is the entry an audit trail can least
  afford to be missing.
- **A rejected API token is recorded as well**, by `ApiTokenAuthenticationFilter` — that request is
  turned away before the proxy sees it, so this is the only place it can enter the trail, and a run
  of them is the clearest available signal that someone is guessing or that a revoked token is still
  wired into something. Never the token's value: a near miss is as revealing as a hit.
- **`Mcp-Session-Id` is deliberately absent.** It would group a caller's calls neatly, but the spec
  requires it to be unguessable — it is a capability handle, closer to a session cookie than to an
  identifier — so logging one would put a live credential in the audit log.

Argument *values* are still recorded by neither, for the reason set out below.

The `call(...)` funnel now writes one line per tool call
(`com.mcpdbwizard.pub.McpCallRecord`), and `RuntimeManager` tees the generated server's output to
stdout so a collector can see it. Both were prerequisites for any sink at all, and neither commits us
to one:

```
MCP-CALL {"tool":"ob_gen_pkg_greet","outcome":"ok","ms":9,"args":["p_name","p_times"]}
```

Written in a `finally`, so failures are recorded too — the outcomes distinguish `ok`, `not-found`,
`document-changed`, `pool-exhausted`, `database-error` and `error`. Pool saturation in particular was
previously invisible, and reads as a database error unless it has its own outcome.

**Argument names, never values.** This is where an MCP server differs sharply from an ordinary
service: the values were chosen by a model, so they are simultaneously the most interesting thing to
record and the most dangerous — they can carry anything the caller put in front of the model. Names
give the shape of a call without that. Recording values should be a separate explicit opt-in and is
deliberately not built.

**This is a diagnostic record, not an audit trail, and the difference is not cosmetic.** It goes to a
log the audited process itself writes, so it has no durability, ordering or tamper-evidence
guarantee. A real audit trail needs a sink that outlives the container and cannot be rewritten by the
thing being audited. Two candidates, both still open:

- **An Oracle table.** No new infrastructure, queryable by the DBA, and it inherits the database's
  own retention and access controls. The transactional objection — an audit row in the work's own
  transaction is lost when that work rolls back — is now solvable, because the audit write can borrow
  a *second* factory from the pool and commit independently. It still cannot record "the database was
  unreachable".
- **Kafka or another broker.** Durable, ordered, replicated, and it decouples the record from an
  ephemeral container. The decision it forces is what happens when the broker is down: blocking
  couples every Oracle tool call to broker availability, and dropping makes the guarantee false. Note
  this likely needs no product code — `Slf4jLog` and `Log4j2Log` already exist as `LogInterface`
  backends, so a Kafka appender is operator configuration.

The sink decision is worked through in [`mcp-audit-sink-plan.md`](mcp-audit-sink-plan.md) — including
the three choices that define the feature (recording values and the data-protection obligation that
creates, truncation versus evidence, and what happens when the sink is unavailable).

See [`mcp-observability.md`](mcp-observability.md) for the tracing side, which remains separate.

### 3.5a Auth defaults — ADDRESSED by coupling exposure to authentication, not by flipping them

`SchemaDefaults.java:82-83` still sets `mcpHttpToken=NO` and `mcpHttps=NO`, and that is deliberate.
Both features **fail closed**: defaulting them on would make every newly created config refuse to
start until the operator supplied a token or a keystore. Fail-closed plus default-on is a usability
cliff, and the likely outcome is operators turning it back off.

The loopback default (§2.2) had already broken the original compound — an unconfigured server is not
reachable. But the risk moved rather than vanished: **the single act that exposes the server did not
require the two that protect it.** Publishing the container's MCP port needs `MCP_HTTP_HOST`, and
setting it granted network reach with no prompt to authenticate.

So the guard sits where the risk is created. Binding a non-loopback address now **refuses to start**
unless the server was generated with bearer-token auth, naming the fix in the message. Local use is
untouched, and `MCP_ALLOW_UNAUTHENTICATED_EXPOSURE=YES` is the deliberate escape for anyone running
something in front that authenticates — logged as a warning, because disabling a guard should be
visible.

**TLS deliberately does not satisfy it.** That is stricter than the coupling first sketched, and the
reason is in §1: TLS encrypts the wire and restricts nobody. A server with TLS and no token is an
open server that is merely hard to eavesdrop on; the token is what decides who may call.

Verified on FREE23 against a token-off tree: loopback listens on `127.0.0.1`; `0.0.0.0` exits 2 with
the refusal; the override starts it on `*` and logs the warning; and `MCP_ALLOW_UNAUTHENTICATED_
EXPOSURE=no` still refuses, because only explicit consent counts. A token-on tree emits the guard
with `true` and is unaffected.

**The secret half of this is now done** (2026-08-04, `9d4eb53`). The web app generates a fresh
random token per server at start (`RuntimeManager.tokenForServer`) and injects it as
`MCP_HTTP_TOKEN`, so nobody has to invent or store one; `mcpdbwizard.runtime.mcp-token` pins a shared
value instead if an operator wants to reach a server directly. **Still open:** whether
`SchemaDefaults` should therefore flip `mcpHttpToken` to `YES` for new web-created configs, now that
turning it on costs the operator nothing. Note it would still be a usability question rather than a
security one, and for a *different* reason than before — the servers the web app runs are loopback
and fronted by the proxy, so a token-off one is not reachable without an account either way. It
matters for a config later exported and run somewhere else.

### 3.5b Rate limiting and request size — IMPLEMENTED, per-caller quotas included

The original finding was that there was no throttling and no request-body cap. Both now exist; what
remains open is narrower and is stated at the end of this section. **Do not quote the paragraphs
below as if nothing shipped** — the earlier wording of this section survived two releases that
closed it, which is why the status is now spelled out first.

What already existed was real backpressure: the pool bounds concurrency, so calls beyond
`DAO_POOL_MAX_SIZE` get "Server busy: retry" rather than a crash, visible in the logs as
`outcome:"pool-exhausted"`. The database was protected from connection exhaustion.

What was missing was a bound on *rate* — an agent in a loop could keep every pooled connection busy
indefinitely, since concurrency was capped but throughput was not — and a bound on body size.

**Shipped since (all opt-in, all off by default):**

- **Call rate** (`120238f`): `MCP_RATE_LIMIT` calls per second with `MCP_RATE_BURST`, via
  `com.mcpdbwizard.pub.McpRateLimiter`. A refused call is logged as `outcome:"rate-limited"`,
  deliberately distinct from `pool-exhausted` so load shed by *policy* can be told from load shed by
  *saturation*. A mistyped value stops start-up rather than leaving the server silently unlimited.
- **Statement timeout and request-body cap** (`3229c1b`): bounds how long a single call may run and
  how large a body may be. While a cap is configured, a request with no `Content-Length` is refused
  **411** — a chunked body cannot be checked in advance, and letting it through uncapped would be
  the hole the cap exists to close.

Operator-facing setup for all three is in [`DEPLOYMENT.md`](../../DEPLOYMENT.md); the design record,
including what the rate limit deliberately does *not* solve, is in
[`mcp-rate-limiting-plan.md`](mcp-rate-limiting-plan.md).

**Per-caller quotas — CLOSED 2026-08-04.** This section long said quotas were a decision rather than
an edit, because they need a caller identity one shared token cannot supply, and therefore belonged
downstream of §3.3 "or in a proxy in front of the server". That proxy was built (`9d4eb53`) and the
quota now lives in it (`com.mcpdbwizard.web.security.McpCallQuota`): one token bucket per account,
keyed on the username the API token resolved to.

```sh
  -e MCPDBWIZARD_MCP_PER_CALLER_RATE_LIMIT=5   # sustained requests per second, per account
  -e MCPDBWIZARD_MCP_PER_CALLER_BURST=20       # bucket depth; defaults to the rate
```

Unset means unlimited, so nothing changes for an existing deployment. Over-quota is **429** with
`Retry-After`, recorded as `outcome:"rate-limited"` naming the account — the trail says whose quota
it was, which is the whole point of having done §3.4 first.

**The two limits layer; neither replaces the other.** A generated server's own `MCP_RATE_LIMIT`
still guards it against the total load of every caller at once, and against anything reaching it
directly — a published port (§4) or a server run by hand (§8). The proxy's divides that capacity
between accounts. Configuring one is not a reason to drop the other.

Four decisions inside it worth not relitigating:

- **The quota is checked after authorization, not before.** A 403 never reaches a server, so charging
  it would let a misconfigured client exhaust its own allowance on requests that cost the shared
  capacity nothing — and would answer 429 where 403 is both honest and more useful.
- **Every proxied request spends a permit, not only `tools/call`.** Each costs a servlet thread here
  and a request upstream, and `tools/list` against a large schema is not free.
- **The burst therefore has a floor of 5.** An MCP client spends `initialize`,
  `notifications/initialized` and `tools/list` before doing any work, so a bucket that cannot hold a
  handshake would turn a rate limit into an outage rather than a slowdown.
- **Admins are not exempt.** Unlike the access matrix, where exempting them prevents a lock-out only
  an admin could fix, there is nothing self-defeating about limiting an admin's rate — and their
  runaway agent starves everyone else exactly as effectively.

**What it still does not solve** is what `McpRateLimiter` has always said: it bounds how *often*
calls start, not how long one runs, and a single expensive query pins a pooled connection whatever
the request rate. A statement timeout is the tool for that.

**Keep that caveat in proportion, though.** No MCP surface accepts caller-supplied SQL — every
statement is authored before it is exposed, and a caller supplies bind values only — so duration is
owned by whoever wrote the query, in the same way as any other application SQL. A timeout is a
backstop rather than a missing control. See
[`mcp-rate-limiting-plan.md`](mcp-rate-limiting-plan.md) §3.1.

## 4. Notes for whoever changes this code next

- **`startsWith("127.")` is a security hole, not a shortcut.** It also matches
  `127.0.0.1.evil.example.com`, a hostname anyone can register. The loopback test in `McpHttpPolicy`
  is a full IPv4 octet parse for that reason. This bug was written and then caught by
  `McpHttpPolicyTest`, which is why that test is mostly refusals.
- **The plaintext arm of the `http` branch is compiled by nothing else.** `MCP_HTTPS` is a
  generation-time flag, so only one arm is emitted into a given server, and the propfile the live
  harnesses regenerate (`generic_test_23ai`) sets it `YES`. A break in the plaintext arm would
  survive a fully green multi-box run. `GeneratedMcpHttpShapeTest` mirrors that arm so it compiles
  and runs on every build — extend it alongside any change to that branch.
- **Testing the TLS branch by IP** gets `400 Invalid SNI` from Jetty's `SecureRequestCustomizer` when
  the certificate is `CN=localhost`. Connect by name, or build a SAN keystore the way
  `TGen23aiMcp` does.
- The §2 changes alter emitted output for every `MCP_SERVER=YES` config (the `http` branch only).
  Byte-identity with pre-fix trees is intentionally broken.
