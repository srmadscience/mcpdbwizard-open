# Fixing what the 2026-08-22 `orindademo` sweep found — plan

> **PROPOSED, not implemented.** Nothing below has been built. The verdicts in §1 were established
> on 2026-08-22 by checking the sweep's claims against source and against the live config, and two
> of them **overturn the sweep's own conclusions** — read §1 before doing any of the work, because
> the highest-severity item in the report is not a product defect at all.
>
> Findings measured against a **26ai Free server (23.26.0.0.0, PDB `FREEPDB1`, user `ORINDADEMO`)**
> — the host `docs/testrun_260822.md` could not identify. Which Oracle line the numbers came from is
> the part that matters and is kept; the box itself is inventory and is deliberately not named here.
> Per the dated-record rule in the private `CLAUDE.md`, re-measure anything numeric before quoting it.

The sweep is `docs/testrun_260822.md`: every tool on the running `orindademo` MCP server invoked at
least once, with exact inputs and outputs. Its evidence is good and its isolation work on Defect 4 is
the best part of it. Its **ranking** is what needs correcting.

---

## 1. What the sweep found, after review

| # | Sweep's claim | Verdict | Anchor |
|---|---|---|---|
| 1 | `paramState` silently dropped — "severity: highest" | **NOT A DEFECT.** Fixture DDL never inserts the column | `app/sql/Demo/mcpdbwizard_demo_ddl.sql:437` |
| — | Instructions advertise CRUD that is not exposed (filed as an aside) | **REAL DEFECT, under-ranked** | `SAAdminWrangler.java:5224` |
| 2 | DATE in a record publishes no format and rejects the documented one | **REAL.** Records bypass the completed date-crossing fix | `SAAdminWrangler.java:5124`, `:6375` |
| 3 | `ORA-17072` names the value, not the column | **REAL**, and the limit is findable | `StatementParameters2.java:1032` |
| 4 | Index-by collection READ fails `ORA-06532` | **REAL, and not covered by any existing plan** | needs investigation |
| — | `paramXxx` Java field names leak outbound | **REAL**, known family | `app/docs/mcp-record-crossing-plan.md` |
| — | Server stopped twice mid-sweep | **EXPLAINED** — two configs, one licence slot | `/data/*.json`, `Licence.java:95` |

### 1.1 Why item 1 is not a defect

The sweep said, correctly, *"Check the package body before assuming this is a generator bug."*
The package body is in the repo and settles it. Every one of the four `INSERT INTO customers`
sites, across three DDL files, reads:

```sql
INSERT INTO customers
(name,address,city,zip,birthdate,phone)     -- STATE is absent
VALUES
(p_customer.name,p_customer.address,p_customer.city,
 p_customer.zip,p_customer.birthdate,p_customer.phone);
```

`CUSTOMERS.STATE VARCHAR2(2)` exists, and the parameter is `p_customer in customers%ROWTYPE` — so
the record **does** carry `state`, the binding delivers it, and the demo PL/SQL never stores it.

**The trap worth keeping:** the sweep's corroborating evidence was
`customers_cst_ix1 {"state":"CA"} -> []`, offered as proof the value never reached the table. It is
proof of exactly that — and it is equally consistent with the fixture never writing the column. A
read-back through a second path confirms *absence*, never *who dropped it*. Distinguishing them
needs the writer's source, which is why the sweep was right to flag the check and wrong to rank the
item before performing it.

### 1.2 The defect the sweep buried

```java
// SAAdminWrangler.java:5224 -- and the same text at :5019
if (haveTables) {
    instructionsText = instructionsText + "Exposes row CRUD on table(s) " + tableNameListing
            + " (get_by_pk / insert / update / delete), rows crossing as JSON objects keyed by column name. ";
}
```

`haveTables` is `mcpTableList.size() > 0` — presence only. **`mcpCrud` is never consulted here**,
though `TableMcpInfo.applyCrudFlags` (`:150`) has already decoded it into
`readable`/`insertable`/`updatable`/`deletable`. All six tables in this config are `mcpCrud: "R"`,
so the server's instructions promise four write tools per table and emit none.

`Schema.java:99` states the generated inventory *"is always accurate and always impersonal"*. That
is now false, and the comment is part of what has to change.

**Why this outranks most of the numbered defects.** Instructions are the first thing a model reads,
before any tool listing. A model told it can `insert` will attempt it and get "unknown tool" —
and, per the `authorization-is-the-config-file` principle, read-only-ness here *is* the
authorization decision. Advertising writes misdescribes the security posture of the config.

---

## 2. Phases

Ordered by value over risk. Phases 1–3 are independent and can land separately.

### Phase 1 — instructions must describe only what was emitted

**The fix.** Build the operation list per table from the flags already decoded on `TableMcpInfo`,
instead of a hardcoded string. Tables in one config may differ, so the listing cannot be a single
sentence with one parenthesis when the flags are not uniform.

**Decision wanted (D1):** what to print when tables disagree — e.g. `AIRCRAFT` read-only and
`BOOKINGS` full CRUD. Options: (a) group tables by flag set, one clause each; (b) one clause naming
per-table operations inline; (c) list the union and let `tools/list` be the authority. **(a) is
recommended** — it stays one short sentence in the common case where every table matches, which is
what the existing text optimises for.

**Both sites move together** (`:5019` and `:5224`) or the description and the instructions disagree,
which is the same asymmetry that produced this bug.

**Test.** A db-free unit test over the instruction-building function with: all-read-only, all-CRUD,
and a mixed config. Assert the read-only case contains no `insert`. **Negative control:** revert the
production change and confirm the read-only assertion fails — the mixed case will pass either way,
so it cannot be the only test.

**Also update** the `Schema.java:99` javadoc claim.

### Phase 2 — DATE inside a record

Two independent halves; **fixing either alone still leaves the tool unusable**, which is the trap
here.

`app/docs/mcp-date-crossing-plan.md` is marked DONE (2026-08-20) and made `1990-01-01` work. It
fixed the **scalar** path only:

| Path | Emitted code | Accepts `1980-01-01`? |
|---|---|---|
| Scalar DATE param | `parseIsoDate(...)` — `:6796` | yes |
| DATE **inside a record** | `RECORD_MAPPER.convertValue(...)` — `:7523`, `:6282` | **no** |

**2a — inbound.** `RECORD_MAPPER` is built at `:5124`–`:5149` with visibility rules and
`FAIL_ON_UNKNOWN_PROPERTIES` and **no date handling**, so `java.util.Date` fields fall to Jackson's
default deserializer and only `yyyy-MM-dd'T'HH:mm:ss.SSSZ` is accepted. Register a deserializer
module using the **same** parser `parseIsoDate` already uses — shared, not a second copy, or the two
paths drift and a caller gets different answers from a scalar and a field of the same Oracle type.

**2b — schema.** `recordFieldJsonType` (`:6369`) maps a Java class to a bare JSON type string:

```java
if (java.util.Date.class.isAssignableFrom(theType)) { return "string"; }
```

No `format`, no `description`, so the accepted syntax is undiscoverable. Scalar DATE params already
publish `"DATE, ISO-8601 date, e.g. 1990-01-01 ..."`. **The function returns a `String` and must
start returning a schema fragment** (or gain a sibling that supplies the description) — that is the
real shape of this change, and it touches every caller.

**Note `byte[]` returns `"string"` on the next line with the same silence.** RAW/base64 has the
identical discoverability gap; decide whether Phase 2b covers it (**recommended** — same function,
same edit, and leaving it makes the asymmetry harder to explain later).

**Test.** Compile a generated tree and drive the emitted record path reflectively with
`1980-01-01`, `1980-01-01T00:00:00.000+0000` and an offset form — the technique
`mcp-date-crossing-plan.md` Phase 5 already used, and the reason that plan's claims held up.

### Phase 3 — `ORA-17072` should name the field and the limit

```java
// StatementParameters2.java:1032
throw new CSException("StatementParameters2: Error while trying to set parameter " + (i + 1)
        + ": ORA-" + e.getErrorCode() + " " + e.getClass().getName() + ":" + e.getMessage());
```

The `"Inserted value too large for column: \"ZZ_TEST_CLAUDE_1\""` half is **Oracle's** text, which
names the value; we cannot change it. What we control is the prefix, which offers only an ordinal.
Add the parameter/field name where the ordinal is known.

For the record: the constraint in the sweep was **`BOOKINGS.CUSTOMER_NAME VARCHAR2(12)`**, not
`CUSTOMERS.NAME VARCHAR2(80)` — the 16-character name fit the customer row and overflowed the
booking. Worth stating in the sweep doc so the next reader is not guessing.

**Scope check (D2):** `StatementParameters2` is runtime library code on every generated DAO's path,
not MCP-only. Changing an exception message is low risk but wide. Confirm that is wanted rather
than an MCP-layer wrapper.

### Phase 4 — index-by with a RECORD element fails on the way OUT

**Investigate before designing.** The sweep isolated this well and its narrowing should be kept:

| Mechanism, identical input `MWO` | Result |
|---|---|
| PL/SQL table / SQL object array / nested table | 2 records each |
| **index-by table** | **`ORA-06532` subscript outside of limit** |

and the index-by **write** on the same package in the same session **succeeded** — so it is the
OUT/return binding, not the IN binding and not the package.

**`app/docs/mcp-indexby-gate-plan.md` does NOT cover this.** That document is about scalar element
kinds (TZ done; DATE and RAW still gated) and never mentions a record element. Do not assume the
gate's analysis transfers.

First questions: what size is the emitted `PlsqlIndexByTable2` given, and where does that number
come from? `ORA-06532` on a 2-row result points at a declared bound rather than volume. `at line 16`
is inside the generated anonymous block, so dump the block for this routine first.

### Phase 5 — outbound `paramXxx` names (decision, not yet a fix)

Table lookups return `departure_city`; record-returning routines return `paramDepartureCity`. Two
vocabularies for the same data in one session.

`app/docs/mcp-record-crossing-plan.md` is marked ALL FOUR PHASES DONE and covers the inbound half.
**Decision wanted (D3):** whether outbound record keys should be renamed to Oracle column names.
This is a **breaking change** to every existing record-returning tool's output shape, so it is a
product call, not a bug fix. Recommend deciding it explicitly and recording the decision either way
rather than leaving it to be rediscovered by the next sweep.

### Phase 6 — the demo fixture (cheap, and it removes a decoy)

Add `state` to the four `INSERT INTO customers` column lists in `app/sql/Demo/`. It costs nothing,
and it stops the next person spending the same hours the sweep did. **This is a fixture change, not
a product fix** — do not let it be recorded as closing Defect 1.

Note the `app/sql/` tree does not ship to the open-source repo, so this change stays private.

### Phase 7 — operational, no code

Both configs carry `runOnStart: YES` **and** `metricsPort: 9464`:

| | `dr.json` | `orindademo.json` |
|---|---|---|
| `runOnStart` | YES | YES |
| `metricsPort` | 9464 | 9464 |

The free tier is one running server (`Licence.java:95`), so two auto-starting configs contend for a
single slot — the likely cause of the sweep's *"server stopped twice"*, and the reason `dr` rather
than `orindademo` holds the metrics port now. Two configs naming the same metrics port is separately
wrong: only the first to start binds it.

Give `orindademo` its own port, and decide which config should auto-start. No product change is
implied — but if this is a common shape, a **warning when two configs name the same metrics port**
belongs on the Service Options tab, beside the existing `PROMETHEUS_SERVER=NO` warning.

### Phase 8 — verification

Phases 1–3 are emitter changes, so the estate is the gate: six boxes, per-propfile counts against
`app/Scripts/provisioning/expected-file-counts.txt`. Expect **no** file-count movement from any of
them — they change emitted *text*, not the set of emitted classes. A count that moves means
something unintended happened.

Re-run the sweep afterwards against the same server to confirm the tools behave, and record which
host it was this time.

---

## 3. Decisions wanted before starting

- **D1** — mixed CRUD flags: how the instructions read when tables differ. (Phase 1; recommend
  grouping by flag set.)
- **D2** — whether Phase 3 edits the shared `StatementParameters2` message or wraps at the MCP layer.
- **D3** — whether outbound record keys become Oracle column names. Breaking; product call.
- **D4** — whether `byte[]`/RAW rides along with Phase 2b. (Recommend yes.)

## 4. Traps

1. **A read-back confirms absence, not authorship.** §1.1. Applies to any "the value did not
   arrive" finding driven through tools alone.
2. **"DONE" on a plan means done for the paths that plan measured.** `mcp-date-crossing-plan.md` is
   genuinely complete for scalars and silent about records. Check which path a fix actually covered
   before treating a defect as regressed.
3. **Two emission sites for one sentence** (`:5019`, `:5224`). Fixing one produces a server whose
   description and instructions contradict each other.
4. **`recordFieldJsonType` returns a bare type string.** Adding a description is a signature change,
   not a line edit — budget for the callers.
5. **A stale MCP tool list looks like a config change.** The tool list in a long-running session is
   captured at connect time; this config's `sqlStatements` is `[]`, so the five statement tools
   visible in an old session are not evidence the config has them. Read `/data/<config>.json`.
6. **The sweep exercised no EXTRA_SQL statement tools** — there were none in this config. Its
   "every tool" framing is accurate for this config and is **not** coverage of the statement surface.
