# DATE and TIMESTAMP cross MCP through one lenient, zone-less pattern — plan

> **DONE 2026-08-20, except the estate run (Phase 6).** All five defects fixed and proven on the
> EMITTED code, not just on the pattern: the generated tree was compiled and its private
> `parseIsoDate`/`formatIsoDate` driven reflectively through the six reported cases.
>
> ```
> 1990-01-01                -> 1990-01-01 00:00:00.000 +0000   accepted
> 2003-06-09T22:44:00+05:30 -> 2003-06-09 17:14:00.000 +0000   offset HONOURED
> 2003-06-09T22:44:00Z      -> 2003-06-09 22:44:00.000 +0000   Z HONOURED
> 2003-06-09T22:44:00.500   -> 2003-06-09 22:44:00.500 +0000   millis KEPT
> 2003-13-45T00:00:00       -> REFUSED, message naming the accepted forms
> format(Date) 1970-01-01T00:00:00 · format(Timestamp) 1970-01-01T00:00:00.500
> ```
>
> **The shape of the fix changed one thing the plan did not anticipate, and it closed Phase 5 for
> free.** The logic moved OUT of emitted text and into `com.mcpdbwizard.pub.McpDates`. It could not
> be unit-tested while it existed only as strings inside the emitter; in the library it is covered
> by `McpDatesTest`, whose 13 cases were proven able to fail — reverting `parse` to the original
> one-liner fails **10 of 13**, reproducing every reported defect including the three silent ones.
> And because a LIBRARY constant is not the illegal forward reference that blocked sharing the
> emitted one, the Jackson site now references `McpDates.ISO_PATTERN` instead of repeating the
> literal. There is nothing left to keep in step by hand.
>
> Decisions taken: bare date accepted as midnight; **offset honoured** rather than refused (refusing
> would relocate item 1 onto `Z`, the commonest form a model emits); strict parsing, accepting the
> break; DATE rendering unchanged, fractional seconds added only for `java.sql.Timestamp` via
> `formatAny` — which matters because `Timestamp extends Date`, so every existing call site reaches
> it through `instanceof java.util.Date` and would otherwise keep dropping them.
>
> Two tests pinned the OLD schema wording and were updated: they encoded the defect the report
> found. Db-free suites green — app 845/0/0, web 398/0/0.
>
> **Phase 6 CLOSED 2026-08-28.** The six-box estate ran green on `2.0.12`, which is the gate this
> asked for: ORCL12 961, XE18/ORCL19/ORCL21 955, FREE23/FREE26 968, and web 482 on every box, all
> app failures zero.
>
> **It had said "outstanding" for eight days and five releases, and that is the part worth keeping.**
> The work shipped in 2.0.6; the banner did not move with it, so anyone reading this doc was told
> finished work was pending — and on 2026-08-27 that cost a recommendation to redo it. The estate
> genuinely had not run since 2.0.9, so the claim was half true, which is the hardest kind of stale
> to catch: the gate really was open, just not for the reason written here.
>
> Check a status line against the code and the estate logs, not against the sentence. See
> [[stale-plan-doc-statuses]].

## The defect

The generated server crosses every DATE and TIMESTAMP through one pattern:

```java
private static final String ISO_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";   // SAAdminWrangler:6062
private static String formatIsoDate(java.util.Date d)  { ... .format(d); }        // :6066
private static java.util.Date parseIsoDate(Object v) throws Exception { ... .parse(...); }  // :6072
```

Measured, running that pattern through `SimpleDateFormat` on this machine:

| input | result | |
|---|---|---|
| `1990-01-01` | `ParseException: Unparseable date` | **rejected**, though it is ISO-8601 |
| `1990-01-01T00:00:00` | `1990-01-01 00:00:00.000 +0000` | the one accepted profile |
| `2003-06-09T22:44:00+05:30` | `2003-06-09 22:44:00.000 +0100` | **offset dropped**, silently |
| `2003-06-09T22:44:00Z` | `2003-06-09 22:44:00.000 +0100` | **Z dropped**, silently |
| `2003-06-09T22:44:00.500` | `2003-06-09 22:44:00.000 +0100` | **millis dropped**, silently |
| `2003-13-45T00:00:00` | `2004-02-14 00:00:00.000 +0000` | **rolled over** — `isLenient()` is `true` |

Five distinct problems:

1. **The advertised schema is wrong, and it is the only guidance a caller gets.** The tool says
   `"DATE, ISO-8601 string"` (`mcpCrossingNote` :7079, `mcpSqlParamTypeLabel` :7105). `1990-01-01`
   *is* ISO-8601 and is refused. The error names the input without naming the expectation.
2. **A zone offset is silently discarded.** `SimpleDateFormat.parse(String)` stops at the end of the
   pattern and ignores trailing text. No error, wrong instant, no signal. `Z` — the commonest form a
   model emits — behaves the same way.
3. **Fractional seconds are silently truncated**, by the same mechanism.
4. **Parsing is lenient**, so a malformed date returns confident nonsense rather than an error.
5. **No zone anywhere in the pattern**, so the instant is resolved against the server JVM's default
   zone. The same string means different instants on different servers — and, because DST moves,
   on different *dates* on the same server: the run above gave `+0100` for June and `+0000` for
   January.

**Items 2–4 are worse than item 1.** Item 1 fails closed. Those three fail silently and produce a
plausible wrong answer. Item 1 is still the one an agent hits first, because `1990-01-01` is exactly
what a model sends for a column called `HIRE_DATE` — and a retry loop on an unparseable date is not
free, see the connection-churn behaviour recorded in `CLAUDE.md`.

## It is wider than DATE, and the OUT direction is lossy too

`mcpIsDateType` (:6450) covers **`java.util.Date` and `java.sql.Timestamp`**, and `mcpArgConversion`
routes a Timestamp param through `new java.sql.Timestamp(parseIsoDate(...).getTime())` (:6740). An
Oracle DATE has no fractional seconds, so item 3 costs it nothing — **a TIMESTAMP does**, and
`formatIsoDate` has no `.SSS` either. So sub-second precision is lost in **both** directions on a
type where it is real data.

## Every site that has to move together

| site | what |
|---|---|
| :6062 | `ISO_DATE_PATTERN` |
| :6066 / :6072 | `formatIsoDate` / `parseIsoDate` |
| **:5170** | **the same literal INLINED** for Jackson's `defaultDateFormat` |
| :6737 / :6740 | `java.util.Date` and `java.sql.Timestamp` argument conversion |
| :6099 / :6204 | row → JSON helpers |
| :6228 / :6632 | collection element kind `date` |
| :7079 / :7105 | the caller-facing schema wording |

**The two literals are kept in step BY HAND today**, and the emitted comment at :5166 says so: the
constant cannot be referenced from the static initializer (illegal forward reference) and is only
emitted when the config has functions, tables or SQL statements. That hand-coupling is itself worth
closing — see Phase 5.

## What this is NOT

- **Not the TZ index-by defect** (`tz-collection-binding-plan.md`). That one is the PL/SQL anonymous
  block's `TO_TIMESTAMP` masks for index-by collections; this is the Java-side MCP crossing. They
  rhyme — zone-less masks, zone unreachable both ways — and **neither fix touches the other.**
- **Not the shared-`SimpleDateFormat` bug** fixed in `GenericLog`. Both helpers construct one per
  call, so there is no thread-safety issue here. Do not "optimise" that into a shared static.

## Decisions wanted before starting

These change what a deployed server accepts and returns, so they are not mine to take.

1. **Accept a bare `1990-01-01`?** Recommended yes, as midnight — it is the form a model reaches for
   and refusing it is the loud failure that started this. But *which* midnight is decision 2.
2. **Honour an offset, or refuse it?** Three options, and they are genuinely different products:
   honour it (convert to the instant — correct, and changes what today's callers get for a string
   that today parses as local); refuse it with a message naming the accepted form (fails closed, no
   silent change); or keep ignoring it (not defensible now it is written down).
3. **Strict parsing is a breaking change for anyone relying on rollover.** `2003-13-45` becoming
   `2004-02-14` is nonsense, but it is nonsense somebody's script may depend on. Recommended: break
   it, and say so in the release note.
4. **Does the OUT format change?** Adding `.SSS` for Timestamp, or an offset, changes what **every
   existing MCP client sees** on every date field. Recommended: leave DATE's rendering exactly as it
   is and add fractional seconds only for `java.sql.Timestamp`, which needs its own helper — a
   smaller blast radius than one shared change.

## Phases

**Phase 0 — a red test, on the emitted behaviour not the pattern.** The measurement above was made
against `SimpleDateFormat` directly; the test has to drive a **generated** server, or it proves
something about the JDK rather than about what we ship. `TGen23aiMcp` already drives one end to end
and its fixture has date columns. Assert each of the six rows above. Expect red on five.

**Phase 1 — replace the parse.** `DateTimeFormatter` with an optional time part and an optional
offset, or at minimum `setLenient(false)` plus a `ParsePosition` check that the whole string was
consumed. **The full-consumption check is the part that closes items 2 and 3** — strictness alone
does not, because trailing text is ignored before leniency ever comes into it.

**Phase 2 — make the schema wording true.** Whatever Phase 1 accepts, `mcpCrossingNote` and
`mcpSqlParamTypeLabel` must say it. "ISO-8601 string" is not a specification; name the accepted
forms. This is the cheapest half of the whole fix and would have prevented the original report.

**Phase 3 — the Timestamp path.** Its own format/parse with fractional seconds, per decision 4.

**Phase 4 — the error message.** An unparseable value should name the expectation, not just echo the
input. This is what stops an agent retrying blind, which is where the connection churn comes from.

**Phase 5 — close the hand-coupling.** Emit the two literals from one place, or add a test that
greps the emitted source for both and asserts they match. A comment saying "keep these in step" is
not a mechanism, and this file exists partly because that comment was the only guard.

**Phase 6 — verify.** This changes emitted output for **every `MCP_SERVER=YES` config**, so
byte-identity with earlier trees is intentionally broken and the full estate is the gate. Check the
`_mcp` file counts as well as the totals.

Copyright 2003-2026 ATB Consultancy Services Ltd
(formerly Orinda Software Ltd, Dublin, Ireland)
