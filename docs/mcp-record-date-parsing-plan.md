# A DATE inside a record goes through Jackson, not through `McpDates` — plan

> **IMPLEMENTED 2026-09-01, Phases 1-3 and the db-free half of Phase 4. Phases 5 and 6 remain.**
> `McpDateModule` registers `McpDates` on the record mapper; six inputs measured through the REAL
> emitted mapper, out of a `generic_testb_mcp` tree regenerated against ORCL12, all six now matching
> the scalar path — including the reported bare date and the silent `+05:30`:
>
> ```
>   1980-01-01                 1980-01-01 00:00:00.000   scalar same   match
>   1980-01-01T09:30           1980-01-01 09:30:00.000   scalar same   match
>   1980-01-01T09:30:00        1980-01-01 09:30:00.000   scalar same   match
>   1980-01-01T09:30:00+05:30  1980-01-01 04:00:00.000   scalar same   match
>   1980-01-01T09:30:00.500    1980-01-01 09:30:00.500   scalar same   match
>   2003-13-45T00:00:00        REFUSED, McpDates message scalar same   match
> ```
>
> Suites: app **987/0/2**, web **487/0/0**. Negative control run: removing the module from the test
> mapper fails **7 of 11**, covering all four defects plus the outbound `Timestamp` and the message.
>
> **The decisions were taken as recommended.** D1 resolved more cheaply than this plan expected —
> see §4. D2 keep the epoch number, D3 keep `.defaultDateFormat(...)` as a fallback, D4 release the
> outbound `Timestamp` change as a fix.
>
> **STILL OPEN: Phase 4's live half and Phase 6.** Nothing has driven a date through a running MCP
> server into Oracle and read it back, and the estate has not run. What is proven is that the
> emitted mapper binds the value correctly; what is NOT proven is what the DAO then does with it on
> each Oracle line. Do not record this as finished until those run.

**How it was measured, because "reasoned about" is what made this defect survive.** Everything in §1
and §2 came from a throwaway probe against the mapper the emitter actually builds, and the fix was
prototyped before this document was written. The verification then used the technique
`mcp-date-crossing-plan.md` Phase 5 used and credits for its claims holding up: compile a real
generated tree and drive its private `RECORD_MAPPER` reflectively, rather than rebuilding the mapper
in a test and trusting that the emitter emits the same thing.
>
> **The public known-issues page understates this by three defects.** It reports one symptom —
> a bare `1980-01-01` is refused inside a record — which is the only one that fails LOUDLY. The
> same path also drops a zone offset, drops fractional seconds, and rolls a nonsense date into a
> real one, all silently. Those are three of the five defects `McpDates` was written to fix in
> 2.0.0; the record path never got that fix.

**Relationship to what is already written.** This supersedes **Phase 2a** of
[`mcp-sweep-260822-plan.md`](mcp-sweep-260822-plan.md), which identified the inbound half in
2026-08 and inferred the accepted form rather than measuring it. **Phase 2b of that section — the
missing schema description — SHIPPED in 2.0.12** and is not outstanding. The scalar half of the
story is [`mcp-date-crossing-plan.md`](mcp-date-crossing-plan.md), DONE in 2.0.6: this is that same
fix reaching the one path it did not.

## 1. The defect

### 1.1 What was measured

Same eight inputs, same JVM, `Europe/Dublin`, through the real record mapper and through the scalar
path's `McpDates.parse`:

| Input | Inside a record (today) | Scalar parameter | |
|---|---|---|---|
| `1980-01-01` | **REFUSED** | `1980-01-01 00:00:00.000` | the reported issue |
| `1980-01-01T00:00:00` | `1980-01-01 00:00:00.000` | same | agrees |
| `1980-01-01T09:30` | **REFUSED** | `1980-01-01 09:30:00.000` | seconds not optional |
| `1980-01-01T09:30:00Z` | `09:30:00.000 +0000` | `09:30:00.000 +0000` | **agrees BY ACCIDENT — see 1.3** |
| `1980-01-01T09:30:00+05:30` | `09:30:00.000 +0000` | `04:00:00.000 +0000` | **offset DROPPED, 5½ h wrong, silent** |
| `1980-01-01T09:30:00.500` | `09:30:00.000` | `09:30:00.500` | **fraction DROPPED, silent** |
| `2003-13-45T00:00:00` | `2004-02-14 00:00:00` | REFUSED | **rolled into a real date, silent** |
| `rubbish` | REFUSED | REFUSED | agrees |

Outbound is wrong too, in a way nothing has reported because it is a loss rather than an error:

```
record   {"paramBirthdate":"1970-01-01T01:00:00","paramWhen":"1970-01-01T01:00:00"}
scalar   formatAny(Timestamp) = 1970-01-01T01:00:00.500
```

A `java.sql.Timestamp` FIELD renders through the DATE pattern and loses its fractional seconds. That
is exactly the `formatAny` dispatch `McpDates` documents — *"`Timestamp extends Date`, so every call
site reaches it through `instanceof java.util.Date` and would otherwise keep dropping them"* — and
the record path is the call site that never got it.

### 1.2 Where it comes from

One line, `SAAdminWrangler:5174`:

```java
.defaultDateFormat(new java.text.SimpleDateFormat(com.mcpdbwizard.pub.McpDates.ISO_PATTERN))
```

`ISO_PATTERN` is `yyyy-MM-dd'T'HH:mm:ss`, and a `SimpleDateFormat` on that pattern is a strict
template with two bad habits: `parse(String)` stops at the end of the pattern and IGNORES whatever
follows (so `+05:30` and `.500` are read as trailing junk and discarded), and it is LENIENT by
default (so month 13 day 45 rolls forward). Both are named in `McpDates`'s own class comment as
defects 2, 3 and 4 of the five it exists to fix. **The comment above that line is correct about what
it was for** — it was written to stop Jackson rendering dates as UTC epochs, and it does — but
pointing Jackson at the PATTERN gets none of the parsing that `McpDates` wraps around it.

**So this is not a Jackson bug and not a new defect.** It is the 2.0.0 fix reaching the scalar path
and not the record path, because the record path is Jackson's and nobody re-checked it.

### 1.3 The trap in verifying this

`Z` gives the same answer on both paths — in January, in Dublin. That is a coincidence of the test
zone and date: the record path ignores the `Z` and reads local time, and Dublin's local time in
winter IS UTC. Run the same check in July, or anywhere else, and it diverges. **Do not use a `Z`
case as the regression test for the offset half**, and do not conclude from a passing `Z` that
offsets survive. `+05:30` is the case that cannot be coincidentally right.

## 2. The fix, prototyped and measured

Give the record mapper `McpDates` itself, as a Jackson module, instead of a pattern:

```java
SimpleModule theModule = new SimpleModule("McpDates");
theModule.addDeserializer(java.util.Date.class,      ... McpDates.parse(...));
theModule.addDeserializer(java.sql.Timestamp.class,  ... McpDates.parseTimestamp(...));
theModule.addSerializer(java.util.Date.class,        ... McpDates.formatAny(...));
theModule.addSerializer(java.sql.Timestamp.class,    ... McpDates.formatTimestamp(...));
```

**Those two types are the whole surface, measured rather than assumed.** The emitter maps an Oracle
date column or attribute to `java.util.Date` or `java.sql.Timestamp` and to nothing else — the
checked-in generated tree under `app/sql/Demo/Src` declares only those two, and the type decisions at
`SAAdminWrangler:6564`, `:7000` and `:7505` name only those two. There is no `java.sql.Date` and no
`java.time` field to cover.

Prototyped against the real mapper shape: **all eight inputs above now match the scalar path
exactly**, including the two silent ones, and the outbound record becomes
`{"paramBirthdate":"1970-01-01T01:00:00","paramWhen":"1970-01-01T01:00:00.500"}`.

**A bonus worth noting rather than relying on.** The refusal a caller now sees is `McpDates`'s own
explanatory message *plus the field Jackson was reading*:

```
Cannot read "2003-13-45" as a date. Expected an ISO-8601 date, optionally with a time and a
zone offset: 1990-01-01, ... (through reference chain: Rec["paramBirthdate"])
```

That names the field, which is the thing known issue 6 says our errors do not do. It does **not**
close issue 6 — that is `StatementParameters2`, a different layer, and it is the ORA-17072 overflow
case rather than a parse failure. Do not let this be recorded as closing it.

### 2.1 Where the module lives, and why not in the emitter

In `com.mcpdbwizard.pub`, beside `McpDates`, exposed as something like `McpDates.jacksonModule()`.
The emitted line becomes `.addModule(com.mcpdbwizard.pub.McpDates.jacksonModule())`.

This is the same reasoning that moved the parsing itself out of emitted text in 2.0.0, and it is
worth restating because it is the reason that fix could be trusted: **logic that exists only as
strings inside the emitter cannot be unit-tested**, so it is verified by generating a tree, which
needs an Oracle instance. In the library it is covered by an ordinary db-free test that a
contributor can run.

**It does add a runtime Jackson dependency to `pub`.** Today `pub` does not import Jackson — the
mapper is built in emitted code, and jackson arrives on the generated tree's classpath. Putting a
`JacksonModule` subclass in `pub` means the library references `tools.jackson` types. See **D1**.

## 3. Phases

### Phase 1 — the module, in the library

`McpDates.jacksonModule()` (or a small `McpDatesModule` class) plus `McpDatesModuleTest`.

**Test the module, not the pattern.** The suite already has `McpDatesTest` covering `parse`; this
one must go through a mapper configured exactly as the emitter configures it — field visibility ANY,
getters NONE, `FAIL_ON_UNKNOWN_PROPERTIES` enabled — because the defect is in the interaction, not
in `McpDates`. A fixture record class with a `java.util.Date` and a `java.sql.Timestamp` field is
enough; it needs no generated code.

Cases: the eight in §1.1, both directions, plus the three non-string forms in **D2**.

**Negative control:** build the same mapper WITHOUT the module and confirm the offset, fraction and
month-13 cases fail. Four of the eight pass either way, so a control that only runs the whole set
proves nothing about which half is doing the work. **And check the control took** — this repository
has twice had a control that silently did nothing.

### Phase 2 — wire it into the emitter

One line added at `SAAdminWrangler:5174`, and a decision about the line already there (**D3**).

The emitted comment block above it is long, accurate about why it exists, and now incomplete. It
should say that the module carries the PARSING and the pattern carries only the fallback rendering —
otherwise the next reader concludes the same thing this plan had to disprove: that pointing Jackson
at `ISO_PATTERN` means dates cross the way `McpDates` says they do.

### Phase 3 — the published description becomes WRONG, and a test will say so

`SAAdminWrangler.recordFieldCrossingNote(RECORD_FIELD_DATE)` currently publishes:

> ISO-8601 date-time, pattern `yyyy-MM-dd'T'HH:mm:ss` — the T form is REQUIRED inside a record,
> unlike a scalar date parameter, which also accepts a bare 1980-01-01

Every clause of that is false once Phase 2 lands. It must be rewritten in the SAME change, or the
fix replaces a rejection a caller can see with a schema that lies to them — which is worse, because
the schema is what a model reads instead of trying.

**`RecordFieldCrossingNoteTest` asserts this wording and WILL FAIL.** That is the guard working, not
a problem: it is there precisely so the sentence cannot drift from the behaviour. Update it with the
production change, in the same commit, and do not update it first.

**Recommended new wording:** drop the asymmetry clause entirely and say what both paths now accept —
one sentence, no "unlike a scalar parameter", because there is no longer a difference to warn about.

### Phase 4 — prove it on a generated tree, against a live database

The db-free test proves the module. It cannot prove the EMITTER wired it, which is a separate failure
and the one that would ship silently.

Follow `TMcpIndexByDateTime` and `AbstractMcpHarness`: drive a real generated MCP server over stdio
and call a routine taking a record with a date field, sending a bare `1980-01-01` and an offset form,
asserting what Oracle stored. `generic_testd_mcp` is the natural config; check whether it already
exposes a record-taking routine with a date field before adding a fixture, and prefer an existing one.

**Run it on 12c AND a 23ai-line box.** Not for the parsing, which is pure Java, but because this
plan's sibling — the index-by date work — found the two lines differ in what the DAO does with the
value after it is parsed, and a record path has never been checked across versions for that.

### Phase 5 — the documentation, which is now wrong in three places

1. **`known-issues.md` entry 2** comes out — but only when the fix SHIPS, not when it merges. The
   entry describes released behaviour and is correct until an image carries the change.
2. **The summary-table row** and the frontmatter description both name it; both move together.
3. **Release notes.** Say what the three silent halves were, because a reader who only ever hit the
   bare-date rejection needs to know their existing data may have been read wrong: an offset was
   dropped, so a value sent as `+05:30` was stored 5½ hours out. That is a data-correctness note,
   not a feature note, and it is the reason this is worth a paragraph rather than a line.

### Phase 6 — the estate

Six boxes, all 41 propfiles, per the standing rule. This is emitted-code change, so file counts are
the wrong signal — the change adds no file. The signal is the harness from Phase 4 and no propfile
below its floor.

## 4. Decisions wanted before starting

**D1 — does `pub` take a Jackson dependency?**
The module has to reference `tools.jackson` types. Options: (a) put it in `pub` as an optional
dependency, like gson and sdoapi, since only an MCP-generating build needs it; (b) keep it in
emitted code and accept that it cannot be unit-tested — which is what this whole area's history
argues against; (c) put it in `pub` but reach Jackson reflectively, which is untestable in a
different way and worse. **Recommend (a).** The precedent is exactly the optional emission-target
dependency pattern already in `app/pom.xml`, and the alternative is the arrangement that let this
defect survive two years.

> **RESOLVED, and it cost nothing: NO POM CHANGE WAS NEEDED.** Jackson 3 is already on `app`'s
> compile classpath, transitively and already `optional`, through the MCP SDK
> (`io.modelcontextprotocol.sdk:mcp` → `mcp-json-jackson3` → `tools.jackson.core:jackson-databind`).
> So the module inherits exactly the optionality option (a) was going to declare by hand.
>
> **The module is a SEPARATE class, `McpDateModule`, and `McpDates` stays Jackson-free.** That was
> not in the plan and it matters: `McpDates` is reached by every generated DAO, including in
> applications that never touch MCP and have no Jackson on the classpath. Putting Jackson types in
> its signature would put a `NoClassDefFoundError` one reflective call away from a caller who never
> asked for any of this.

**D2 — an epoch NUMBER inside a record is accepted TODAY. Keep it?**
Measured: `{"paramBirthdate":315532800000}` binds `1980-01-01` in a record right now, and the SCALAR
path REFUSES the same value (`McpDates.parse` stringifies it and the ISO parser rejects it). So the
two paths differ here in the OPPOSITE direction from everything else in §1.1, and full parity would
REMOVE a form that works today.

- Keep it (deserializer falls back to the numeric token): backward compatible, one more branch, and
  the two paths still disagree.
- Refuse it: parity, and a caller relying on epoch millis breaks with a clear message.

**Recommend keeping it**, and saying so in the schema description. It is not ambiguous, nothing else
in the product treats a bare integer as a date, and silently removing an accepted spelling is the
failure mode this plan exists to fix. `null` and `""` both bind null on both paths already —
measured — so they need no decision.

**D3 — keep `.defaultDateFormat(...)` once the module is in?**
The module wins for the two types that exist, so the line becomes dead for every field the generator
actually emits. Keeping it costs nothing and covers a date type nobody emits today; removing it
means a future date type crosses as a UTC epoch instant, which is the defect the line was added for.
**Recommend keeping it**, with the comment above it saying it is now a fallback.

**D4 — does the outbound `Timestamp` change need a deprecation note?**
A `java.sql.Timestamp` field inside a record gains `.500` on the way out. Existing clients parsing
`yyyy-MM-dd'T'HH:mm:ss` exactly will now see a longer string. The scalar path made this same change
in 2.0.0 and it was released as a fix rather than a break. **Recommend the same**, called out in the
release notes rather than gated.

## 5. Traps

- **`Z` agrees by accident in this timezone.** §1.3. Use `+05:30`.
- **Four of the eight cases pass without the fix.** A test that runs them as one set and reports
  green tells you nothing about which half works. Assert them individually.
- **`RecordFieldCrossingNoteTest` failing is the guard, not a regression.** Do not "fix" it ahead of
  the production change; that removes the only thing pinning the schema to the behaviour.
- **The record mapper is also used for OUTPUT** — `writeValueAsString` on record returns, on
  SQL-statement row classes, and on collection elements (`SAAdminWrangler:6358`, `:7184`, `:8133`).
  A serializer change reaches all of them, not only record parameters. That is intended, and it is
  wider than the issue's title suggests.
- **Do not use file counts as the signal.** No file is added or removed.
- **Do not record this as closing known issue 6.** The better error message is a side effect in a
  different layer.
