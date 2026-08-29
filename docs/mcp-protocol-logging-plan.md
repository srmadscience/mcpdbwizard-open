# MCP in-protocol logging — plan

> ## STATUS: IMPLEMENTED, 2026-08-11 — option A, as recommended below
>
> Everything from §6 shipped: `com.mcpdbwizard.pub.McpProtocolLog` (SDK-free), the `exchange`
> threaded through the funnel by retargeting the two `mcpCallOpen` strings (no handler body
> changed), the send in the funnel's existing `finally`, and `.logging()` spelled at both
> `.capabilities(...)` sites. Tests as designed in §7: `McpProtocolLogTest` (7, db-free) and a
> `TGen23aiMcp` leg whose **negative** half — no frame once the level is back at `info` — is what
> proves the level is honoured rather than that we send unconditionally.
>
> Verified per §8 on all six boxes, 41 propfiles each, **file counts unchanged everywhere**.
>
> **The §10 upstream issue IS filed:**
> [modelcontextprotocol/java-sdk#1086](https://github.com/modelcontextprotocol/java-sdk/issues/1086)
> (2026-08-11) — that `ServerCapabilities.logging` is added unconditionally, overriding the caller's
> explicit capabilities, so every server built with the SDK advertises a capability SEP-2577
> deprecated. It proposes honouring the caller's capabilities or adding an opt-out, and offers a PR.
> **If it is accepted, option D below becomes viable again — but keep this implementation anyway:**
> it is verified across six boxes and costs nothing at the default level, so removing it would trade
> a working thing for a smaller diff.
>
> **Still open:** §2's second staleness — the spec's `_meta`-based level selection, absent from SDK
> 2.0.0 — remains outside our reach.
>
> The rest of this document is the reasoning as written before implementing, kept because the
> options it rejects are the ones a reader will otherwise re-propose.

**Status when written: proposed, not implemented.** Written 2026-08-07 against SDK `io.modelcontextprotocol.sdk:mcp:2.0.0`.

Not to be confused with [`logging-plan.md`](logging-plan.md), which is about the *backend* a generated
DAO factory logs through (`DAO_LOG_TYPE`, `MCPDBWIZARD_LOG_BACKEND`). This one is about MCP's own
`notifications/message` — messages that cross the protocol to the **client**, not to the operator.

---

## 1. The finding that forces the question

`app/CLAUDE.md` records that our generated server builds
`capabilities(ServerCapabilities.builder().tools(true).build())` — the `tools(true)` half was itself
corrected on 2026-08-28, see §4 — and yet answers `initialize` with
`"capabilities":{"logging":{},"tools":{"listChanged":true}}`, and that **whether the SDK permits
suppressing it was unresolved**. It is now resolved:

> **It cannot be suppressed. The SDK adds `logging` unconditionally.**

`McpAsyncServer` has two constructors — one taking `McpServerTransportProvider` (our stdio path), one
taking `McpStreamableServerTransportProvider` (our HTTP path) — and **both** run:

```java
this.serverCapabilities = features.serverCapabilities().mutate().logging().build();
```

There is no branch and no flag; verified in the bytecode of both constructors
(`javap -c` on `mcp-core-2.0.0.jar`, `.mutate()` → `.logging()` → `.build()` at the same offset in
each). Whatever a caller passes to `.capabilities(...)` is mutated on the way in. So the option
"declare only what we implement" — which is what this project believed it was doing — **does not
exist at the wire level with this SDK**. The real choice is between advertising and implementing, or
advertising and not implementing.

## 2. What is actually true today (the framing matters)

It is tempting to describe this as "advertises a capability it does not implement". That is too
harsh, and the accurate version changes the size of the work:

- **`logging/setLevel` is implemented — by the SDK.** `prepareRequestHandlers` registers a handler
  for it whenever `serverCapabilities.logging() != null`, which is always. A client that calls it
  gets a correct response and the level is stored per session.
- **Level filtering is implemented — by the SDK.** `McpServerSession.minLoggingLevel` defaults to
  **`INFO`**, and `McpAsyncServerExchange.loggingNotification(...)` checks
  `isNotificationForLevelAllowed` before it emits `notifications/message`. Sub-threshold messages are
  dropped inside the SDK, so a caller need not guard.
- **What is missing is only ours: the generated server never calls `loggingNotification`.**

So the promise we make is not a broken method. It is **a stream that is always empty**. That is
still an empty promise a client could act on, but the gap to close is small and entirely on our side.

**Level selection is a second, separate staleness, and it is the SDK's.** The spec moved level
selection off the `logging/setLevel` request onto a per-request `_meta` key
(`io.modelcontextprotocol/logLevel`) *before* the deprecation. SDK 2.0.0 implements only the old
request form — the string `logLevel` does not appear in `mcp-core-2.0.0.jar`. Nothing we can do
about that from here, and it does not block anything below; noted so it is not rediscovered.

## 3. The tension, stated once

MCP's in-protocol logging utility is **deprecated as of protocol revision `2026-07-28`**
([SEP-2577](https://github.com/modelcontextprotocol/modelcontextprotocol/pull/2577)); new
implementations **SHOULD NOT** adopt it, and the earliest removal is the first revision on or after
**2027-07-28**. Implementing a deprecated feature is normally the wrong instinct, and this document
would otherwise recommend against it.

**What overrides that here is §1: we are already advertising it and cannot stop.** Declining to
implement does not put us in the "did not adopt it" camp — it puts us in the "claims it and does not
do it" camp, which is strictly worse than either alternative. Given that, implementing is the
cheapest way to make the wire honest, and it is bounded: at least one full revision cycle of runway,
and the design below confines the blast radius to two files.

## 4. Options considered

| | Option | Verdict |
|---|---|---|
| **A** | **Implement it minimally** — send one `notifications/message` per tool call from the existing funnel | **RECOMMENDED** |
| B | Suppress the capability by rewriting the `initialize` response in a transport wrapper | **Rejected** — fights the SDK from outside, breaks silently on any SDK change, and the thing it hides is one line of work to make true |
| C | Suppress by reflection on the private final `serverCapabilities` field | **Rejected** — same objection, plus it breaks under any future module strictness |
| D | Do nothing, document the mismatch | **Rejected as the final state**, but it is the correct state *until* this plan is accepted, and it is where we are now |
| E | Fork/patch the SDK | **Rejected** — a fork of a fast-moving SDK to remove three bytecodes |

**Do B/C only if A is rejected**, and prefer D to either.

## 5. Design

### 5.1 Unconditional, not a propfile flag

Every other optional MCP feature here is a flag (`MCP_HTTP_TOKEN`, `MCP_HTTPS`,
`PROMETHEUS_SERVER`). This one should **not** be, for the reason `McpHttpPolicy` is not one: *the
capability is advertised unconditionally, so a flag would leave the lie in place for every config
that did not set it* — which is the entire thing being fixed. A config cannot be allowed to forget
it.

Cost of unconditional is nil by default; see 5.2.

### 5.2 Emit at `DEBUG`, so the default is zero frames

The session default is `INFO`. Emitting at **`DEBUG`** means:

- a client that never calls `logging/setLevel` receives **nothing** — no added traffic, no tokens
  spent, no change in observable behaviour;
- a client that asks for debug gets a real, complete implementation.

That is what makes "unconditional" affordable.

**Rejected alternative — mapping the outcome taxonomy onto levels** (`rate-limited`/`pool-exhausted`
→ `WARNING`, `database-error` → `ERROR`, rest `DEBUG`). It looks better than it is: every one of
those outcomes **already goes back to the caller** as a `CallToolResult.isError(true)` carrying the
same text, so the notification would duplicate into the model's context window something it just
received. Do not re-litigate this without a case where the client cannot otherwise learn the fact.

### 5.3 Content: exactly what the funnel already computes

Tool name, outcome, elapsed ms — the same three values the `MCP-CALL` line and `METRICS.record(...)`
already take, in the same `finally`. **No new instrumentation.**

**Argument names only, never values** — the identical rule the `MCP-CALL` record follows, for the
identical reason (the values were chosen by a model). It is weaker here, since the notification goes
back to the caller that supplied them, but keeping one rule for both records is worth more than the
marginal difference, and log frames are not always going to the same place the result is.

### 5.4 Where the SDK types are allowed to appear

**None of the nine `Mcp*` helpers in `com.mcpdbwizard.pub` reference the MCP SDK today** (verified:
zero occurrences of `modelcontextprotocol` under `pub/`). That is a load-bearing property — `pub`
ships with generated code, and a generated DAO layer must not drag an MCP dependency behind it.

So the split is:

- **`com.mcpdbwizard.pub.McpProtocolLog` (new, SDK-free):** builds the message text and answers
  whether a record should be sent at all. Plain `String`/`Map`/`long` in, `String` out — db-free and
  unit-testable exactly like `McpCallRecord`.
- **The emitted funnel:** the *only* place naming `McpSchema.LoggingMessageNotification` /
  `LoggingLevel`. One call site.

That is also the deprecation hedge. When the SDK eventually drops these types, the damage is one
emitted site behind a generator edit, and **no customer's `pub` jar names the removed classes at
all**.

## 6. Implementation

Line numbers are from `SAAdminWrangler.java` at `a90225c`; **grep for the quoted strings rather than
trusting them** — this file's numbers have drifted before.

1. **`app/src/main/java/com/mcpdbwizard/pub/McpProtocolLog.java`** (new, ~60 lines). `logger()`
   returns the logger name (the server class). `line(String toolName, Map args, String outcome, long
   ms)` returns the message text, reusing `McpCallRecord`'s argument-name rule — factor the
   name-extraction out of `McpCallRecord` rather than copying it.
2. **Thread the exchange into the funnel.** All 13 registration sites already have `exchange` in
   scope and every one discards it — they are emitted as
   `.callHandler((exchange, req) -> " + mcpCallOpen`. So this is the same shape as the change that
   added `req`: retarget the two `mcpCallOpen` strings (field init `SAAdminWrangler.java:123`, and
   the pooled/unpooled assignment at `:4535`) from `call(req, …` to `call(exchange, req, …`, and add
   the parameter to the emitted signature at `:5446`. **No handler bodies change.**
3. **Emit the send** in the funnel's existing `finally` (`:5568`–`:5585`), immediately after the
   `theAuditSink.record(...)` call and before the `mcpPrometheus` block, so all four records are
   built from the same values in one place. Wrap in `try { … } catch (Exception e) { }` — **a log
   frame must never turn a successful tool call into a failure**, the same posture `SessionInfo`
   takes with `DBMS_APPLICATION_INFO`.
4. **Declare it.** `.capabilities(ServerCapabilities.builder().tools(true).logging().build())` at
   **both** emission sites (`:5212` stdio, `:5379` http). This changes no wire behaviour — the SDK
   was adding it anyway — but it stops the generated source from *claiming* something different from
   what it serves, which is what made this hard to find.

   > **SUPERSEDED 2026-08-28 in its `tools` argument only** — the `.logging()` half stands. That
   > `tools(true)` is `listChanged=true`, and it was the same defect this document was written
   > about, sitting in the same call: a capability advertised and never implemented. The tool list
   > is a fixed `Arrays.asList` built once at construction, so no `notifications/tools/list_changed`
   > can ever be sent. Both sites now read `tools(false)`.
   >
   > **The two are NOT the same case and the difference is the whole lesson.** `logging` could not
   > be withdrawn — the SDK adds it unconditionally — so the only honest move was to implement it.
   > `tools` is our own argument, so the honest move is to set it correctly. Reaching for
   > "implement it" here would have been pattern-matching on the neighbouring fix and building a
   > notification path the architecture does not want: curation happens at generation time, and an
   > unexposed operation has no code emitted at all, which is what makes the config file the
   > authorization boundary.

**Emitted-output impact:** changes every `MCP_SERVER=YES` tree, so byte-identity with earlier trees
is intentionally broken. **File counts do not move** — no new file is emitted. That arithmetic is
the cheap check on the verification run.

## 7. Tests

- **`McpProtocolLogTest`** (db-free, in `com.mcpdbwizard.pub`): message shape, the argument-names
  rule, null/empty request, and that a null tool name does not throw. Mirrors `McpCallRecordTest`.
- **`TGen23aiMcp`** (live, gated) gains one leg: after `initialize`, assert the response advertises
  `logging`; call `logging/setLevel` with `debug`; call a tool; assert a `notifications/message`
  arrives naming that tool. Then re-set to `info` and assert the next call produces **none** — the
  negative half is the one that proves the level is honoured rather than that we send unconditionally.
- **`TMcpServerStartup`** needs nothing new; it already starts all 21 servers per box and would catch
  a signature mistake at start-up.

## 8. Verification

One full estate run (all six boxes, the three profiles), because step 2 touches the funnel every tool
handler passes through. Expect: **file counts unchanged everywhere**, `app` +N for
`McpProtocolLogTest`, `web` unchanged. Any file-count movement means step 2 broke an emission site
rather than a handler — that is the signal to look for.

## 9. Deliberately out of scope

- **Progress notifications.** `McpSyncServerExchange.progressNotification(...)` exists, is **not**
  deprecated, and is the mechanism actually suited to a slow Oracle call — genuinely new information
  to a caller, unlike a completion record it can infer from its own result. It is the better feature
  and it is a **separate piece of work**; folding it in here would mix a correctness fix with a
  product addition.
- **Sending log frames to the audit sink or Prometheus.** Both already receive these values.
- **`_meta`-based level selection.** Not available in SDK 2.0.0 (§2).

## 10. Upstream

Worth filing against `modelcontextprotocol/java-sdk`: **the SDK forces a deprecated capability onto
every server it builds, overriding the caller's explicit `ServerCapabilities`.** Every Java MCP
server in existence is currently advertising `logging` whether or not it implements it, which is
precisely the confusion SEP-2577 was meant to reduce. If upstream adds an opt-out, option D becomes
viable again and §6 steps 1–3 can be dropped — so **file this before implementing**, in case the
answer is quick.
