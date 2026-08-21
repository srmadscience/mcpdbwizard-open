# Index-by tables crossing MCP: the TZ gate (done) and DATE/RAW (open)

> **TZ: DONE 2026-08-21, six boxes green.** `generic_testd_mcp` went 28 → 30 tools, gaining
> `iba_test_test_timestamp_tz` and `…_ltz`; additions only, tree compiles, db-free suites app
> 850/0/0 and web 398/0/0, estate green on all six boxes with no propfile below its floor.
>
> **DATE and RAW: still gated, and this document is now mostly about them.** See
> "What remains" — the analysis there is the reason they were NOT folded in.
>
> **This file was a forward plan and three of its claims were wrong.** The wrong parts are kept,
> marked, because the first would otherwise be re-derived by whoever picks up DATE and RAW — it is
> the same trap waiting in the same place.

## What was gated, and why the reason retired

`mcpProcUnsupportedReason` skips a routine when an index-by parameter's element is not crossable.
**The gate is per ROUTINE, not per parameter**: one such argument hid every tool for that routine —
no error, no partial exposure, the routine simply absent from `tools/list`. That is why a niche type
mattered more than it looked.

The refusal used to read *"only NUMBER and VARCHAR2 elements cross"*, because `PlsqlIndexByTable2`
is deliberately binary and would **silently stringify** a date — returning something the caller never
sent. That was a sound reason to refuse.

**The TZ binding fix removed it.** The DAO path now converts with `TO_TIMESTAMP_TZ` and a `TZR` mask
in both directions, verified live on 12c and 23ai preserving `+05:30` and `Asia/Calcutta`. The
stringification the gate existed to prevent no longer happens.

**Nothing regressed when that landed** — a zoned element classified as unsupported before the fix
(`java.sql.Timestamp`) and still did after it (`oracle.sql.TIMESTAMPTZ`). The gate had become
*unnecessary*, not newly wrong.

## What TZ actually needed

> **The section that stood here claimed "TZ is nearly free — no new emitted code is needed at all".
> That was WRONG, and it is the correction that matters most to anyone reading this for DATE.**

The claim was that a zoned element could reuse the `string` element kind and cross verbatim, because
the wrapper's own `bindParams` already applies `setDataType(TIMESTAMPTZ)`, the 80-character element
length and `ensureFractionalSeconds()`. **That part is true and is why this was still cheap.**

What it missed: **the emitted mask and the MCP wire format disagree about one character.**

```sql
-- the generated PL/SQL converts a zoned element with:
'yyyy-mm-dd hh24:mi:ss.ff9 TZR'          -- a SPACE between date and time
-- MCP crosses dates as ISO-8601:
'2019-03-01T14:25:36.0+05:30'            -- a T
```

Measured on both Oracle lines:

```
to_timestamp_tz('2019-03-01T14:25:36.0+05:30', 'yyyy-mm-dd hh24:mi:ss.ff9 TZR')
  -> ORA-01858: a non-numeric character was found
```

So TZ needed exactly the mask conversion this document said only DATE needed. It is
`McpDates.toOracleTimestampText` / `fromOracleTimestampText`, a **textual `T`↔space swap** —
deliberately not a parse-and-reformat, because parsing yields an instant and re-rendering it would
put the caller's `+05:30` back in the *server's* zone, which is the defect this whole area was fixed
for. Only the character at index 10 moves, so a `T` inside a region name (`US/Eastern`,
`America/Port_of_Spain`) is left alone.

The separator before the **zone** turns out to be flexible — offset or region, with or without a
space, all accepted — which is why one character is the whole difference.

**What shipped:**

| | |
|---|---|
| `mcpCollectionElementKind` | `oracle.sql.TIMESTAMPTZ` / `TIMESTAMPLTZ` → new kind **`timestamptz`** |
| `mcpIndexByTypeCode` | `timestamptz` → `OracleTypes.VARCHAR` |
| `toScalarObjectArray` | IN: `McpDates.toOracleTimestampText` |
| `collectionToJson` | OUT: `fromOracleTimestampText`; **gained a `theKind` parameter** |
| `mcpCollectionLabel` | the caller-facing description |

Note the last two. The OUT helper had no idea what kind of collection it held, so it could not have
converted anything — "the OUT direction needs nothing" was wrong for the same reason as the rest.

### Why not reuse the `date` element kind

Measured, on the real class:

```
new PlsqlIndexByTable2(OracleTypes.VARCHAR, 0)
  setArray(new Object[]{"2003-06-09T22:44:00Z"})  -> kept verbatim
  setArray(new Object[]{new java.util.Date(0L)})  -> IllegalArgumentException:
                                                     Cannot format given Object as a Number
```

The `date` kind produces `java.util.Date` objects, which is precisely what throws. **Reusing it
would have looked right and failed at run time.** This is the single most important fact for the
DATE work below.

## The description, and the trap in writing it

The live MCP report that started all this was, at bottom, a **description** defect: the schema said
"ISO-8601 string", the caller sent `1990-01-01`, and it was refused. A zoned timestamp has more
accepted spellings, not fewer, so the description has to carry them. What is emitted:

```
p_in (array of ISO-8601 timestamps with a zone, e.g. 2019-03-01T14:25:36+05:30 or
      2019-03-01T14:25:36 Asia/Calcutta (a LOCAL timestamp is returned in the server's time zone))
```

**The LOCAL clause is not padding.** A `TIMESTAMP WITH LOCAL TIME ZONE` keeps no zone of its own, so
values come back rendered in the *server's*. An agent that sent `+05:30` and reads back `GMT` would
otherwise reasonably conclude the call failed.

> **NO DOUBLE QUOTES IN THAT TEXT, and the first draft had them.** It is printed straight into an
> emitted `.description("…")` literal and **that emission does not escape** — everything else
> reaching it is plain prose. Quoting the examples produced a generated file with bare quotes inside
> a string literal, which does not compile. It was caught by reading the emitted line; the tool
> count was correct throughout, so nothing else would have shown it.

## The decision — TAKEN: expose it

**Should an agent be handed a zoned collection at all?** The mechanism works; that was never the
question. The DAO path is driven by a developer who has read the parameter. MCP hands it to a model
that will emit whatever ISO-ish string it likes, and a zone that *converts* silently is a different
risk from one that fails loudly.

*Decided: expose it.* The product goal is to call any PL/SQL routine whatever its inputs, and a
routine being invisible **in its entirety** because of one parameter is a bigger surprise than a
fiddly type — especially now the conversion is correct and the description says so.

The alternative — leave it gated but fix the message — was done **as well**, for the kinds that are
still gated. Even a gate that stays should not cite a defect that has been fixed.

## What remains: DATE and RAW

Same gate, genuinely different jobs. **Do not fold them in on the grounds that the gate is shared.**

**DATE** carries both problems at once:

- the same mask mismatch — `ORACLE_DATE_TO_CHAR_MASK` is `"yyyy-mm-dd hh24:mi:ss"`, a space, no `T`;
- **and** the `java.util.Date` problem above, because the existing `date` element kind is what
  produces the object `setArray` refuses.

So it needs its own text conversion *and* a decision about whether the `date` kind changes shape or
a new kind appears beside it. Changing `date` affects generated VARRAY and nested-table collections,
which accept `java.util.Date` happily today — so a new kind is likely the smaller change.

**RAW** is an encoding mismatch: MCP crosses binary as base64, `PlsqlIndexByTable2` carries hex
(`getArrayAsRaw` parses hex pairs). Small, but real emitted code both ways.

The refusal now names these rather than the retired reason:

```java
String theObstacle = "raw".equals(elementKind)
        ? "MCP crosses binary as base64 and an index-by table carries it as hex"
        : "the generated conversion uses a space-separated Oracle mask, not the ISO form MCP
           crosses dates in";
```

### Phases for that work

1. **Red test first.** `generic_testd` already has `IBA_TEST.TEST_DATE` and `TEST_RAW`, so its
   `_mcp` sibling gives a before/after with **no fixture change** — assert the tool exists, then
   round-trip a value. (The original plan proposed extending the 23ai fixture; that was
   unnecessary, and it will be unnecessary again.)
2. **A new element kind per type**, not a reshaped `date`.
3. **The conversion**, in `McpDates` (or a sibling for RAW) where it can be unit-tested — the
   emitter cannot test itself, which is why the date logic moved to the runtime library at all.
4. **The description**, naming the accepted form. Plain prose, no quotes.
5. **Verify**: db-free suites, then the six-box estate, because it changes emitted output for every
   `MCP_SERVER=YES` config that has such a routine.

## Traps

- **The gate is per ROUTINE, not per parameter.** One unsupported argument hides every tool for that
  routine.
- **Two branches refuse, not one.** A null element kind refuses at the first check;
  a kind that maps to no `mcpIndexByTypeCode` refuses one branch later, with a different message.
  Adding a kind without adding its type code fails quietly in the second place.
- **Never hand `PlsqlIndexByTable2` a `java.util.Date`** — measured above.
- **`.description(...)` does not escape.** Plain prose only.
- **`TGen23aiMcp` asserts `tools.size() > 18`**, a lower bound — it does not need updating when the
  surface widens, and its fixture has *scalar* TZ params rather than index-by ones. The original
  plan claimed the opposite on both counts.
- **Measure the mask, do not reason about it.** Every wrong claim in this document came from
  reasoning about formats that a single `to_timestamp_tz` call would have settled in seconds.

Copyright 2003-2026 ATB Consultancy Services Ltd
(formerly Orinda Software Ltd, Dublin, Ireland)
