# Audit trail — plan

> ## STATUS: items 1–3, encryption in transit (2026-08-12) and spool encryption at rest (`0a3c17f`) BUILT
>
> **Decided by David:** `MCP_AUDIT_LEVEL=VALUES` is the production posture — arguments and responses
> are recorded for everything (§3.2) — and encryption in transit is an **optional switch on the web
> UI** rather than an environment-only setting.
>
> Shipped: `McpAuditEvent.ofAuth` (§4.1), `AuthenticationAuditListener` for sign-in / failure /
> sign-out, the `MCP_AUDIT_KAFKA_PROP_*` passthrough with override reporting (§4.2), and an
> **Admin → Audit** page carrying the transit switch.
>
> **Two findings from building it.** Spring Security does **not** publish `LogoutSuccessEvent` by
> default — `LogoutConfigurer` never registers the publishing handler — so the logout listener
> compiled, wired and would have silently never fired; `SecurityConfig` now registers it. And the UI
> switch had to be applied as a **system property**, not an environment variable: the sink is built
> inside the web app's own process and a process cannot change its own environment. Saving also
> **reopens the sink**, because a Kafka producer reads its properties once at construction.
>
> **SPOOL ENCRYPTION IS NOW BUILT — `0a3c17f`, "Encrypt the audit spool, and refuse to deliver what
> cannot be read".** This banner said it was not for two weeks after it shipped, which is the sort of
> line that gets quoted: it reads as a live data-protection exposure under the VALUES decision, and
> on 2026-08-27 it was listed as outstanding work on that basis. **`SpoolCipher`** is AES-256-GCM
> with a fresh 12-byte IV per record, keyed from **`MCP_AUDIT_SPOOL_KEY`**, wired in at
> `SpoolingAuditSink`'s construction via `SpoolCipher.fromEnvironment()`.
>
> **It is OFF by default, and "off" is the honest description of an unset key — not "unavailable".**
> Two properties are worth carrying: a named-but-unusable key **throws** rather than falling back to
> plaintext, because an operator who asked for encryption must not silently get none; and each line
> carries an `ENC1:` marker so a spool written before the key was set still drains rather than being
> stranded.
>
> **The Admin → Audit page was wrong in the same way and mattered more** — it told operators
> flatly that "the write-ahead spool is not encrypted", which is false where a key is set and reads
> as "there is no option" where one is not. It now reports the deployment's actual state and names
> the variable. Fixed 2026-08-27 in the same change as this paragraph.
>
> **Still genuinely untouched:** §3.3 (admin-action and config-change auditing). §3.1 (one record or
> two) stayed as recommended — two records, correlated.
>
> The rest of this document is the reasoning as written before implementing.

**Status when written: proposed, not implemented.** Written 2026-08-11. Companion to
[`mcp-audit-sink-plan.md`](mcp-audit-sink-plan.md) (the SPI and spool, BUILT 2026-08-03) and
[`mcp-security-review.md`](mcp-security-review.md) §3.4.

---

## 1. Most of this already exists — read this section before estimating

The request was: optionally encrypted Kafka, a JSON message per login, a JSON message per call
carrying caller / tool / params / results, written asynchronously. Verified against the code today,
**three of those five are already built and shipping**:

| Asked for | State | Where |
|---|---|---|
| JSON message per call | **BUILT** | `McpAuditEvent.toJson()`, written from the emitted `call(...)` funnel's `finally` |
| Caller on the record | **BUILT**, on a *second* record — see §3.1 | `McpAccessAuditor`, from the web `/mcp/{config}` proxy |
| Params + results | **BUILT**, off by default — see §3.2 | `MCP_AUDIT_LEVEL=VALUES` |
| Asynchronous writes | **BUILT** | `KafkaAuditSink.record` calls `send(...)` with a callback and never `get()`s |
| Kafka sink | **BUILT** | `KafkaAuditSink`, `MCP_AUDIT_SINK=com.mcpdbwizard.pub.KafkaAuditSink` |
| Durability past a broker outage | **BUILT** | `SpoolingAuditSink` write-ahead spool, at-least-once, replays verbatim |
| **Login events** | **MISSING** | §2.1 |
| **Encrypted Kafka** | **MISSING** | §2.2 |
| **Encrypted spool** | **MISSING** | §2.3 |

The event already carries more than the request asked for: a UUID, a timestamp, the outcome
taxonomy (`ok` / `not-found` / `document-changed` / `pool-exhausted` / `rate-limited` /
`database-error` / `error`), duration, and — when a response is truncated at `MCP_AUDIT_MAX_BYTES` —
**the full byte count and a SHA-256 of the whole payload**, so a clipped record cannot be mistaken
for a complete one.

**So this is a small piece of work, not a new subsystem.** The value in the next three sections is
mostly in the decisions, not the code.

---

## 2. The actual delta

### 2.1 Login events (the real gap)

Nothing records authentication. There is no `AuthenticationSuccessEvent` /
`AbstractAuthenticationFailureEvent` / `LogoutSuccessEvent` listener anywhere in `mcpdbwizard-web`,
so a trail of MCP calls today can name the account that made each call but cannot say when that
account signed in, from where, or how many times it failed first.

**Failed logins matter more than successful ones** and are the cheaper half to get right: a
successful login is one line in a story the call records already tell, while a run of failures is
often the only trace an attack leaves.

### 2.2 Encrypted Kafka — three different things, and only two are ours

These get conflated, and conflating them produces a plan that cannot be finished:

1. **TLS in transit** — `security.protocol=SSL|SASL_SSL` plus truststore/keystore properties on the
   producer. **Ours, and small:** `KafkaAuditSink.producerProperties` currently hard-codes six
   properties and passes nothing else through. Add a prefixed passthrough
   (`MCP_AUDIT_KAFKA_PROP_<NAME>`) so any producer property — TLS, SASL, compression — can be set
   without a code change per option.
2. **Encryption at rest inside Kafka** — a broker configuration and an operator's job. **Not ours**,
   and the plan should say so rather than implying we deliver it.
3. **Payload encryption before it leaves us** — encrypting the JSON so brokers and their operators
   never see plaintext. **Ours, optional, and only worth it under `VALUES`** (§3.2). It also breaks
   ordinary Kafka consumers, which is a real cost to weigh.

### 2.3 The spool is the weak link, and it is unencrypted

`SpoolingAuditSink` writes plaintext JSON to disk so records survive a broker outage. Under
`MCP_AUDIT_LEVEL=VALUES` that means **production data at rest, unencrypted, on the application
host** — for as long as the outage lasts. Turning on values without encrypting the spool moves the
exposure rather than accepting it knowingly.

---

## 3. Decisions needed before any of this is built

### 3.1 One record or two?

Today one call produces **two** records that correlate by config, tool and time:

- the **generated server's**, which knows the tool, arguments, outcome, duration and response — and
  genuinely cannot know the caller. It connects to Oracle as one shared account and sees a bearer
  token, which is a door key rather than an identity. This is recorded as accepted in
  `mcp-security-review.md`.
- the **proxy's**, which knows the account, the config, the operation and the HTTP status — and does
  not see the tool's arguments or its response.

The request implies one record carrying all of it. Two ways to get there:

| | Approach | Cost |
|---|---|---|
| **A** | Leave two records; document the correlation key | Zero work. A dispute needs a join across two records |
| **B** | Proxy injects the caller into the request `_meta`; the server reads it onto its own event | The server would then record an identity **it cannot verify** — a caller reaching the port directly (§4 of DEPLOYMENT.md) could put anything there. An unverifiable identity in an audit record is worse than an absent one |

**Recommendation: A**, with the correlation documented and a shared request id added to make the join
exact rather than time-based. B trades a real guarantee for a cosmetic one.

### 3.2 Params and results are production data — this is the decision that matters

`MCP_AUDIT_LEVEL=VALUES` already records argument values and the response body. The default is
`NAMES` deliberately, and the reasoning in `McpAuditEvent`'s javadoc is worth re-reading before
flipping it: *"Recording values makes this object carry production data, and the retention,
encryption and erasure obligations that come with it."*

Two asymmetries worth stating plainly:

- **Arguments are model-chosen.** They can carry anything that was in front of the model, including
  data the caller never intended to send to a database.
- **Results are unbounded.** A tool call can return thousands of rows. `MCP_AUDIT_MAX_BYTES` caps
  what is stored, but a cap that clips the interesting part leaves a record that is
  simultaneously large, sensitive, and inconclusive.

**Question for you:** is `VALUES` the intended production posture, or is it for incident
investigation only? The answer changes whether §2.2's payload encryption and §2.3's spool encryption
are optional extras or prerequisites.

### 3.3 What else should be audited?

Login is in the request; these are arguably more security-relevant and are the same shape of work:

- **admin actions** — account created / deleted / promoted, password reset, access-matrix change.
  These change who can reach what, and nothing records them today.
- **config changes** — a saved config decides which database objects are reachable at all, since
  curation is enforced by absence from the generated binary.

Not proposing to build these now; flagging that "audit trail" will be read later as covering them.

---

## 4. Design

Keep the existing shape: one SPI, one sink, one stream, one format. An operator configures the audit
sink once and receives every kind of record through it — that decision is already made and paid for,
and login records should not invent a second mechanism.

### 4.1 Login records

A new `McpAuditEvent.ofAuth(...)` alongside the existing `of(...)` and `ofAccess(...)`, carrying
`user`, `operation` (`login` / `login-failed` / `logout`), `status`, source address and a reason for
failures. `toolName` and `config` stay null, as they already may be.

A Spring `ApplicationListener` in `mcpdbwizard-web` for the three Spring Security events, calling a
recorder that mirrors `McpAccessAuditor` — including its posture that a sink failure is logged and
never propagated, so an audit problem cannot stop someone logging in.

**Never record the password, or anything derived from it.** A failed-login record names the account
and the reason (`bad-credentials`, `no-such-account`, `locked`), never the attempt. This needs
saying because `AuthenticationException` subclasses sometimes carry the credentials in the failed
`Authentication` object.

### 4.2 Kafka property passthrough

`MCP_AUDIT_KAFKA_PROP_<NAME>` → producer property `<name>` (underscores to dots, lower-cased), applied
**after** the six properties the sink sets itself, so an operator can override `acks` or
`enable.idempotence` if they must — and a startup log line naming every property that was overridden,
because silently weakening `acks=all` is exactly the change nobody would notice.

TLS then needs no new code: `MCP_AUDIT_KAFKA_PROP_SECURITY_PROTOCOL=SSL` and the truststore
properties.

### 4.3 Optional payload encryption (only if §3.2 says values are a production posture)

AES-GCM with a key from `MCP_AUDIT_ENCRYPTION_KEY` / `_FILE` (`EnvironmentSecret`, same as
`DB_PASS`), applied to the JSON in both the Kafka sink and the spool. The envelope stays plaintext —
id, timestamp, user, tool, outcome — so a consumer can still route, count and alert without the key;
only arguments and the response are enciphered. That split is what keeps ordinary consumers working.

**This is the piece I would build last and only on a clear answer to §3.2**, because it is the only
part that constrains what every downstream consumer can do.

---

## 5. Work breakdown

| | Item | Size |
|---|---|---|
| 1 | `ofAuth` event + JSON shape + unit tests | S |
| 2 | Spring Security event listener + recorder in `mcpdbwizard-web` | S |
| 3 | Kafka property passthrough + override logging | S |
| 4 | Correlation id shared by the proxy and server records (§3.1) | M |
| 5 | Spool encryption (§2.3) | M |
| 6 | Payload encryption (§4.3) | M, and only on a decision |
| 7 | `DEPLOYMENT.md` operator section: what is recorded at each level, what is not, retention | S |

Items 1–3 are the request's genuine gap and are worth doing regardless of the answers above.

---

## 6. Testing

- **db-free:** the new event's JSON shape; that a failed-login record never contains the submitted
  password; the property passthrough, including that an override is logged.
- **web:** the listener fires on success, failure and logout, and a sink that throws does not stop a
  login (mirroring `McpAccessAuditorTest`'s posture).
- **live:** none needed. Nothing here touches the generator, so **no estate run is required** — which
  is unusual for this project and worth stating, since the last two changes both needed six boxes.

---

## 7. What this still is not, after all of the above

Worth writing down so nobody infers more from the phrase "audit trail" than it delivers:

- **At-least-once, not exactly-once.** The spool replays after a crash, so a consumer must tolerate
  duplicates; the event's UUID is what makes that possible.
- **Not tamper-evident.** The audited process writes its own records. Someone who controls the
  application host can suppress or forge them. Tamper-evidence needs signing or a hash chain
  terminating somewhere the application cannot reach — a different piece of work, and the one to
  build if this is ever evidence rather than diagnostics.
- **Ordering is per-partition only.** Records are keyed by tool, so per-tool order holds and global
  order does not.
- **The Oracle session is still a shared account.** The trail can say which MCP account called a
  tool; the database's own auditing cannot, and will show one service user for everything.

---

## 8. Why Kafka, briefly

It was asked whether there is a better idea. Kafka is already implemented here as the reference sink,
so the marginal cost is the passthrough in §4.2 — nothing else comes close on effort. It is also the
right shape for this: many producers, fan-out to a SIEM and to cold storage, and consumers that can
be added later without touching the application.

The alternatives are worth one line each. **A database table** is a poor audit sink for the reason
`mcp-logging-standard-status` already records: a row written in the work's own transaction is lost on
rollback, and it cannot record "database unreachable" — the case most worth having. **A file plus a
shipper** is what the spool already is, minus the fan-out. **OpenTelemetry logs** would fold this
into the observability pipeline, but the GenAI/MCP semantic conventions are still Development
stability, and an audit trail should not be built on attribute names that can still move.
