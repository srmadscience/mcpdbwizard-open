# Restricting a generated MCP server to one customer — plan

**Status (2026-09-21): NOT IMPLEMENTED, and deliberately deferred. Feasibility checked, not built.**
Every hook §5 names was verified to exist against MCP SDK 2.0.0 and the emitter on that date; nothing
was changed beyond this file and a cross-reference in
[`mcp-security-review.md`](mcp-security-review.md) §3.1.

The check that cannot go stale: `grep -rn "SET_CONTEXT" app/src/main/java web/src/main/java` returns
nothing while this is unbuilt. **Believe the grep, not this banner.**

**The workaround in §4 needs nothing built and covers tens of tenants today.** Read §4 before
concluding that anything here is required.

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
[*Why the safest SQL prompt is the one that does not exist*](../../public_website/mcpdbwizard-site/src/content/writing/why-the-safest-sql-prompt-does-not-exist.md).
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
