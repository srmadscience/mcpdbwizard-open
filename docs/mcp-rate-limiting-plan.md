# Rate limiting a generated MCP server

Options for §3.5b of [`mcp-security-review.md`](mcp-security-review.md), the last of the review's
findings still open on the technical side.

**Status: option D (global token bucket) chosen and implemented. C, G and B remain open and are still
recommended** — see §6.

---

## 1. What is actually at risk

Three things, and they fail differently:

- **The Oracle instance.** The generated server holds a fixed number of connections, so it cannot
  swamp the database with sessions — but nothing stops it asking those sessions to do arbitrarily
  expensive work.
- **The pool.** A call that never returns holds a factory, so a handful of slow calls can starve every
  other caller even at trivial request rates.
- **The server JVM.** An unbounded request body is bounded only by heap.

## 2. What already protects it

`DAO_POOL_MAX_SIZE` bounds **concurrency**, and degrades in the right shape: calls beyond the ceiling
get `CSPoolExhaustedException` → "Server busy: retry", now visible in the log as
`outcome:"pool-exhausted"`. That is real backpressure and it is why the database cannot be flooded
with connections.

## 3. The gap, precisely

Unbounded: **statement duration**, **request rate**, **request body size**.

The framing that matters: *rate limiting is not the first tool for the threat people have in mind*.
"An agent loops an expensive query" is bounded by a **statement timeout**, not by requests per second.
One call can hold a connection for an hour and no rate limit prevents it. There is currently **no**
`setQueryTimeout` anywhere in the generated code or the `pub` runtime.

### 3.1 Duration is the query author's problem, not the caller's

Keep this in proportion, because the paragraph above overstates it if read alone. **Nothing exposed
over MCP accepts caller-supplied SQL.** Every statement is authored before it is exposed: the
generator writes the table CRUD, the unique-key/index/foreign-key lookups, the duality-view document
operations and the sequence access; a PL/SQL routine's body belongs to the schema owner; and an
`EXTRA_SQL` statement is a `.sql` file that whoever wrote the config chose to include. A caller
supplies **bind values**, never statement text.

So how long a call runs is a property of *what was exposed*, and it is owned by the person who wrote
the query, at the time they wrote it — exactly as it is for any other application SQL. Expose a query
that scans a billion rows and it will take as long as it takes, whoever calls it and however often.
That is a tuning question for the author, not a hole in the transport.

This reframes a statement timeout from a missing safety net into an ordinary backstop, which is why
it is opt-in rather than defaulted. The residual worth remembering is small and real: a caller does
choose bind values, so a value that hits a bad plan or selects far more rows than the author pictured
can be slower than intended. That is an argument for a timeout as a belt-and-braces measure on a
schema you do not control, not for treating duration as an unbounded attack surface.

## 4. Options

### Group 1 — bound a single call

| | |
|---|---|
| **C. Query timeout** | *(Shipped as **`DAO_QUERY_TIMEOUT_SECONDS`**, not the name proposed here — it applies to every generated DAO, not only an MCP server. See §6.)* A cap applied to statements the DAOs create. Turns a runaway query into a prompt error rather than a held connection. Needs no caller identity, works on every transport and in both auth modes. Touches statement creation broadly, so it needs care: a legitimately slow report must not start failing. |
| **G. Request-size cap** | A few lines on Jetty's `HttpConfiguration`. Trivial. |

### Group 2 — bound the resource

| | |
|---|---|
| **B. Oracle Resource Manager / profile** | `CPU_PER_CALL`, `LOGICAL_READS_PER_CALL`, or a consumer group. The database kills the offending call itself, which protects Oracle even against something that bypasses this server, and needs **no product code**. It is a DBA action rather than product config, and surfaces as `ORA-02393`-style errors. Coherent with §3.1: one shared account means one profile governs the whole server. |

### Group 3 — bound the rate

| | |
|---|---|
| **D. Global token bucket** | In the emitted `call(...)` funnel. Simple, needs no identity. Blunt — one noisy caller can consume the budget — though with a single shared Oracle account and typically a single client, "fairness between callers" may not be a real problem. |
| **E. Per-caller bucket** | Keyed on the OAuth `sub`, possible now §3.3 has landed. Only meaningful under `MCP_OAUTH=YES`; with a static token every caller is the same principal and it collapses into D. Needs an LRU bound on distinct subjects so the map cannot grow without limit. |
| **F. Per-tool-class limits** | Separate budgets for read / write / SQL using the `readOnlyHint`/`destructiveHint` metadata already emitted. Per-*tool* limits would fail the "one config entry, several operations" test from the PB2 note — one entry, one tool — so class-based is the only shape that fits. |
| **A. Reverse proxy / gateway** | nginx, Envoy, an API gateway. Mature, zero product code. **But MCP multiplexes every tool through `POST /mcp`**, so a proxy can only limit requests to the endpoint; it cannot tell `sequence_nextval` from a destructive `EXTRA_SQL` statement. The same structural limit that makes OTel auto-instrumentation weak here, and the reason D/E/F are things only this server can do. |

## 5. Recommended order

1. **C and G.** Smallest change, largest effect, no identity required.
2. **Document B.** Free, enforced at the right layer, belongs beside the least-privilege grant advice.
3. **D, then E only if needed.** Add a global limit when traffic shows it is wanted; go per-caller
   only once there are genuinely several callers.

**F is not recommended**: the most configuration surface for the least protection, given C already
caps the expensive case.

## 6. What was built, and what was not

**D is implemented.** A token bucket in the emitted `call(...)` funnel
(`com.mcpdbwizard.pub.McpRateLimiter`), configured from the environment — `MCP_RATE_LIMIT` requests per
second, `MCP_RATE_BURST` for the bucket depth. **Unset means unlimited**, so existing deployments are
unaffected and there is no new config-file surface: no `Schema` scalar, no GUI control, no JSON
plumbing, matching how `Origin` validation and the bind host are configured. A refused call returns an
error result telling the caller to retry and is recorded as `outcome:"rate-limited"`, distinct from
`pool-exhausted` so load shed by policy can be told apart from load shed by saturation.

**Where the check sits, and what that excludes.** The limiter is inside the emitted `call(...)`
funnel, which the MCP SDK reaches only *after* it has validated the call against the tool's
`inputSchema`. So a request that fails schema validation — a wrong argument type, a missing required
field — is rejected by the SDK and **never consumes a token**. That is defensible, because such a
call never reaches the database, and it is also why a malformed request produces no `MCP-CALL` record
at all. It does mean the limit governs *well-formed* calls rather than all inbound traffic; a flood of
malformed requests is a transport concern, and option A (a proxy) is the right tool for it.

**C and G are now built too** (2026-08-03): `DAO_QUERY_TIMEOUT_SECONDS` applies a per-statement cap at
every emitted prepare site through `com.mcpdbwizard.pub.QueryTimeout`, and `MCP_MAX_REQUEST_BYTES`
refuses an over-large body with 413 — plus 411 for a chunked request with no declared length, which
could otherwise pass uncapped. Both are opt-in for the reason given above: a cap tight enough to catch
abuse is also tight enough to break a legitimate slow report or a large BLOB argument.

**B was not built** and remain the better first answer to "an agent is hammering the
database". D bounds how *often* work starts; it does nothing about how long a single call runs, which
is the case that actually pins a connection.
