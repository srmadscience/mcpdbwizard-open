# A durable audit sink for the generated MCP server

Options for §3.4 of [`mcp-security-review.md`](mcp-security-review.md). Companion to
[`mcp-rate-limiting-plan.md`](mcp-rate-limiting-plan.md) and
[`mcp-observability.md`](mcp-observability.md).

**Status: BUILT (2026-08-03) — the SPI, the levels, truncation-with-hash, a Kafka reference sink, and
the write-ahead disk spool that §2.3 called the actual work. Delivery is at-least-once; see §7.**

---

## 1. What exists today, and why it is not an audit trail

The emitted `call(...)` funnel — the single point every tool handler passes through — writes one line
per call via `com.mcpdbwizard.pub.McpCallRecord`:

```
MCP-CALL {"tool":"ob_gen_pkg_greet","outcome":"ok","ms":9,"args":["p_name","p_times"]}
```

It is written in a `finally`, so failures are recorded too, and the outcomes distinguish `ok`,
`not-found`, `document-changed`, `pool-exhausted`, `rate-limited`, `database-error` and `error`. The
server's output is teed to stdout, so a platform collector can see it.

That is a good **diagnostic** record and it is not an audit trail:

- the process being audited writes it, to a log it owns;
- no durability guarantee — a container that dies takes unflushed lines with it;
- no ordering or tamper-evidence;
- **argument names only, never values** — deliberately, because a model chose them.

## 2. The three decisions

Most of the work here is deciding these. The code is comparatively easy.

### 2.1 Values, and the consequence of recording them

Recording requests *and* responses inverts the current deliberate choice, and for audit that inversion
is right: "someone called `get_customer`" is not evidence, "someone read customer 4471" is.

The consequence is that the sink becomes **a data store holding production data**. Retention,
encryption at rest, access control and erasure requests now apply to the queue or table as well as to
Oracle. That is the true cost of this feature and it should be accepted knowingly rather than
discovered at the first erasure request.

**Recommendation: three levels, defaulting to the safe one.**

| Level | Records |
|---|---|
| `names` *(default)* | What is recorded today — tool, outcome, duration, argument names |
| `values` | Plus argument values and the response body |
| `off` | No sink |

A default install must not quietly start exporting personal data to a broker.

### 2.2 Truncation versus evidence

A truncated record is weak evidence: "we logged the response, but not the part you are asking about."
A naked byte cap destroys integrity along with size.

**Recommendation:** cap at N bytes, but also record the **full size** and a **hash of the complete
payload**, and set `truncated:true` explicitly. Truncation then costs readability, not integrity, and
nobody can mistake a clipped record for a whole one.

### 2.3 Delivery semantics — the decision that defines the feature

What happens when the sink is unavailable?

| | |
|---|---|
| **Block the call** | Every Oracle operation now depends on the broker being up. Honest, usually unacceptable. |
| **Drop** | The guarantee is false, and worse than none, because it will be trusted. |
| **Bounded local spool, replay on reconnect** | The honest middle — and the actual work in this feature. |

**This must be configurable and stated, not implied.** A deployment that genuinely needs
write-before-act semantics is entitled to block; most will want the spool.

> **A warning about the cheap route.** `Slf4jLog` and `Log4j2Log` already exist as `LogInterface`
> backends, so a Log4j 2 `KafkaAppender` is pure operator configuration and near-zero code. But
> logging frameworks are *designed* to shed load — async appenders discard when their queue fills.
> That is correct for logs and disqualifying for audit. Taking this route gives
> diagnostics-over-Kafka, which is useful, and it must not be described as an audit trail.

## 3. Sink options

| | Strengths | Weaknesses |
|---|---|---|
| **Kafka / broker** | Durable, ordered, replicated; survives the container; feeds a SIEM; retention independent of the database. **Adds no write load to the Oracle instance we are trying to protect** — which matters, since that load is the concern the audit exists to police. | New infrastructure and a client dependency; the delivery decision above becomes unavoidable. |
| **Oracle table** | No new infrastructure; queryable by the DBA in SQL; inherits the database's backup, retention and access controls. The transactional objection is solved — the write can borrow a *second* factory from the pool and commit independently of the work being audited. | Cannot record "the database was unreachable"; write amplification on the instance under protection; invisible to the estate's log pipeline. |
| **File / stdout JSON** | Already effectively present via the tee; zero dependencies. | Same durability and tamper-evidence gaps as today — a staging answer, not an audit one. |

If Kafka is already run, Kafka is the better answer. If not, the Oracle table is the cheapest credible
one, and standing up a broker *for this* is a large cost to accept.

## 4. Proposed shape

**Do not put a Kafka client in the generator.** Add a small service-provider interface in
`com.mcpdbwizard.pub`, chosen at run time, exactly as `DAO_LOG_TYPE` selects a `LogInterface` backend:

```java
public interface McpAuditSink {
    void record(McpAuditEvent theEvent);   // never throws into the caller
    void close();
}
```

- Default implementation is a no-op, so nothing changes for anyone who does not configure it.
- Selected by an environment variable naming the class, keeping every client library optional.
- Kafka, an Oracle table and a file become three implementations rather than three forks of the
  emission code.
- **Reuse `McpCallRecord`'s JSON as the payload**, extended with the value/response fields, so the log
  line and the queue message are one versioned format rather than two that drift.

### One concrete implementation note

The response is not currently in scope where the record is written: `String theJson` is declared
*inside* the `try`, while the record is written in the `finally`. Capturing the response means
hoisting that declaration. Small, but it is the one structural change to the emitted funnel.

### Licence split

The **interface belongs in `app/` (open)** — generated code must compile against it, and the promise
that "your generated code is yours forever" depends on the open runtime being sufficient to run it. A
no-op and a file implementation belong there too.

Specific enterprise sinks, and the console that reads them, are the commercial layer. See
[`../../docs/monetization.md`](../../docs/monetization.md): a durable audit trail is one of the four
questions a buyer is paying to answer, so this is product surface rather than a logging tweak.

## 5. Open questions

- **Which sink first?** Decided by whether Kafka is already in the estate. Nothing else should drive it.
- **Blocking or spooling by default?** Spooling, on the argument that a database tool should not be
  taken down by its audit pipeline — but a regulated deployment may disagree, so it must be settable.
- **Record intent before the call as well as outcome after?** Stronger for write operations, and
  doubles the volume. Probably worth it only at the `values` level.
- **Retention and erasure.** Not a code question, but it becomes a real obligation the moment §2.1 is
  set to `values`, and it should be answered before that ships rather than after.

---

## 6. What was built (2026-08-03)

`McpAuditSink` + `McpAuditEvent` + `McpAuditSinks` in `com.mcpdbwizard.pub`, wired into the emitted
`call(...)` funnel. `MCP_AUDIT_SINK` names the class, `MCP_AUDIT_LEVEL` is `names` (default) or
`values`, `MCP_AUDIT_MAX_BYTES` caps the recorded response. A truncated payload keeps its full byte
size and a SHA-256 **of the whole payload**, so a clipped record still verifies. `theJson` was hoisted
out of the `try` as §4 predicted — the one structural change to the funnel.

`KafkaAuditSink` is the reference implementation, on an optional `kafka-clients` dependency.
`acks=all` plus idempotence, keyed by tool name so a partition preserves per-tool ordering, and
`record()` never throws — an audit failure must not replace the caller's real result.

§2.3's two producer policies remain — `block` waits for buffer space, `drop` does not — but they are
no longer the whole answer, because the spool sits in front. See §7.

Verified live against a real server and Oracle: sink selected from the environment, startup logged,
and a successful call recorded end to end —

```json
{"ts":…,"tool":"zeroparam","outcome":"ok","ms":336,"args":{},
 "response":"[{\"dummy\":\"X\"}]","responseBytes":15,
 "responseSha256":"b912e383…","truncated":false}
```


---

## 7. The spool (2026-08-03)

`MCP_AUDIT_SPOOL_DIR` enables `SpoolingAuditSink`, which wraps whatever sink is configured - so
durability is a deployment choice rather than something each implementation solves for itself.

**Write-ahead, not fallback.** A wrapper that spooled only *after* delivery failed cannot work here:
`record()` returns `void`, and an asynchronous sink has not attempted delivery by the time it returns.
So every record is written first, then a drainer delivers whole segments and deletes each only once
`flush()` confirms. That confirmation is why `flush()` was added to the SPI: for Kafka it blocks on
the producer and reports whether any send failed. **Deleting on an unconfirmed delivery is the one way
a spool is worse than no spool** - it converts "we lost records" into "we believe we have them all".

**At-least-once.** A crash between delivering and deleting replays that segment. `McpAuditEvent` now
carries a UUID `id` so a consumer can collapse the duplicate; without one the guarantee would be
unusable rather than merely imperfect.

**What survives what.** `fsync=never` (default) survives the process - crash, OOM kill, container
restart - because the bytes are in the OS cache. It does not survive power loss. `always` does, at a
disk round trip per tool call.

**A full spool refuses new records rather than discarding old ones.** Dropping the oldest is not
offered: discarding what has already been accepted for audit is the worst of the three options.

Verified live rather than only in unit tests: three calls were made with the delegate broken, the
server was **SIGKILLed**, and a fresh process - making no calls of its own - read the spool back and
delivered all three, each with its original id and timestamp.

**Still not built:** encryption of the spool at rest, which matters once the level is `values`, since
the spool then holds production data sitting on the volume.

---

## 8. The local trail and three more sinks (2026-08-19)

§4 above said "a no-op and a file implementation belong there too" and only the no-op was built. The
file one is now here, and the reason it finally mattered is commercial rather than technical: the
paid line moved from *whether you get auditing* to *whether records leave the machine and how long
they are kept*, so every installation keeps a local trail and the free tier stops recording nothing.

**`FileAuditSink` is a sibling of the spool, not a subclass.** Both write rolling JSONL segments and
the resemblance is deliberate, but a spool is a **delivery queue** — a segment exists until a
delegate confirms it and is then deleted as soon as possible — while this is a **retention store**,
where a segment exists until it is old enough to delete and there is no delegate to confirm
anything. They disagree about the one thing that decides when a file may be removed.

**Ageing out is not a loss; eviction for space is.** `getDroppedCount()` counts records thrown away
early because the byte cap was hit, and write failures. Counting scheduled deletions would make
every healthy trail read as permanently broken.

**The window comes from the segment NAME, not the file's mtime**, so a restored or copied backup is
not treated as fresh — with a fallback to mtime so a hand-renamed file is not read as 1970 and
deleted on the next prune.

### `MCP_AUDIT_SINK` names one class, and that stopped being enough

A licensed installation keeps a local trail **and** streams. `FanOutAuditSink` composes them, and
`MCP_AUDIT_SINK` keeps its original meaning: where records go when they leave this machine. Naming
`FileAuditSink` there is refused with a message pointing at `MCP_AUDIT_FILE_DIR` — it would
otherwise be spooled, which is two copies of the same bytes, and would ignore the retention window.

**`FanOutAuditSink.flush()` must not short-circuit.** A `&&` chain reads correctly and is wrong: the
second sink is never flushed once the first refuses, so a spool in front is told "not delivered"
about a sink it never asked and replays records that had arrived.

### Syslog, Splunk, S3

Syslog and Splunk add **no dependency**; S3 adds an optional `software.amazon.awssdk:s3`, taken for
its credentials chain rather than for signing — in a container the credentials come from an ECS task
role or an EKS service account, and reimplementing that is the kind of code that works in testing
and fails in the deployment that matters.

Three things each cost a debugging cycle and are worth not rediscovering:

**A TCP write to a closed peer SUCCEEDS.** The bytes go into the local send buffer; only the *next*
write fails. Retry-on-exception — which is what most syslog appenders do — therefore never fires,
and exactly one record is lost, counted as delivered, every time a collector restarts.
`SyslogAuditSink` probes for EOF with a 1 ms read before each write; syslog over TCP is one-way, so
the only thing a read can return is EOF. The residual race needs an acknowledgement syslog does not
have, which is what the spool in front is for.

**Framing is octet-counted (RFC 6587), not newline-terminated.** A record's JSON can carry an
escaped newline, and newline framing turns that one record into two malformed ones at the collector.

**A duplicate S3 key OVERWRITES rather than failing.** Two servers on one config rolling in the same
millisecond would destroy one object's records with no error anywhere. The key carries a UUID for
that reason alone.

All four streaming sinks follow `KafkaAuditSink`'s rule that `flush()` compares against the drop
count at the **end of the previous flush**, never the start of this one — §7 above explains why, and
it is the property every one of these was negative-controlled against.

### Closed from §5's open questions

**Retention and erasure** was listed there as "not a code question, but it becomes a real obligation
the moment §2.1 is set to `values`". It is now both. `MCP_AUDIT_RETENTION_DAYS` is the
storage-limitation control, **erasure is by the window expiring** — the trail is append-only, and
removing one record would destroy the tamper-evidence that makes it worth citing — and `0` is a
supported setting meaning nothing is kept. A purge tool could be added later without changing the
format; pseudonymisation at write time could not, which is why declining it now is the reversible
choice.
