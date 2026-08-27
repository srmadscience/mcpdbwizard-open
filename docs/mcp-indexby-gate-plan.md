# Index-by tables crossing MCP: the whole gate, and how it came down

> **TZ: DONE 2026-08-21, six boxes green.** `generic_testd_mcp` went 28 → 30 tools, gaining
> `iba_test_test_timestamp_tz` and `…_ltz`; additions only, tree compiles, db-free suites app
> 850/0/0 and web 398/0/0, estate green on all six boxes with no propfile below its floor.
>
> **DATE and unzoned TIMESTAMP: DONE 2026-08-26.** `generic_testd_mcp` went 30 → 34 tools —
> `iba_test_test_date`, `iba_test_test_timestamp`, `plsql_indexby_tables_proc_date` and
> `plsql_indexby_tables_proc_ts` — additions only, nothing lost, tree compiles. **Note it was FOUR
> tools, not the two this document anticipated:** `PLSQL_INDEXBY_TABLES` has its own date and
> timestamp procedures, and a gate that hides a whole routine hides them wherever they live.
>
> **Six boxes green on 2026-08-26**, all 41 propfiles each: ORCL12 app 938 (base profile), XE18 /
> ORCL19 / ORCL21 app 932 (`-Pharnesses-longids`), FREE23 / FREE26 app 945 (`-Pharnesses-23ai`),
> web 464 on every one. `TMcpIndexByDateTime` returns **byte-identical values on all six** —
> `.123456789` lands on `.12345700` on the 23ai line exactly as on 12c — so the truncate-then-round
> behaviour is not version-specific.
>
> **RAW: DONE 2026-08-26, the same day, on a separate instruction.** `generic_testd_mcp` went
> 34 → 36 tools — `iba_test_test_raw` and `plsql_indexby_tables_proc_raw` — additions only. **The
> gate is now empty of Oracle scalar types:** NUMBER, VARCHAR2, DATE, TIMESTAMP zoned or not, and
> RAW all cross an index-by table. What still refuses is a **record** element, which
> `PlsqlIndexByTable2` genuinely cannot hold.
>
> **RAW also carried a latent DAO defect out with it**, found by reading rather than by a failure —
> see "The missing guard" below. It is fixed in the same change and, honestly, is not covered by a
> test that fails without it.
>
> **Six boxes green on 2026-08-26 at `d71adf4`**, all 41 propfiles each, none below its floor:
> ORCL12 app 954 (base), XE18 / ORCL19 / ORCL21 app 948 (`-Pharnesses-longids`), FREE23 / FREE26
> app 961 (`-Pharnesses-23ai`), web 464 on every one. The **+16** on every box is
> `McpBinaryTest`'s 15 plus the one `TMcpIndexByRaw` harness — the same delta everywhere, so
> nothing was traded for it. `TMcpIndexByRaw` returns byte-identical values on 12c and 26ai.
>
> **FILE COUNTS DID NOT MOVE, AND CANNOT — do not use them as this change's signal.** RAW crossing
> adds two TOOLS to a server class that already existed, and the `.exists(i)` guard changes the
> text of an emitted PL/SQL string. Neither creates a file. `generic_testd` is 119 on ORCL12 and
> 117 on the truncating boxes both before and after. The evidence is the **tool count** (34 → 36,
> measured on both Oracle lines) and the live round trip; the counts are there to prove nothing was
> LOST.
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

## What DATE and TIMESTAMP needed (done 2026-08-26)

**The prediction in this section was right about the shape and wrong about the cost, and the reason
is worth keeping: the DAO layer had ALREADY done the hard half, for years.** The emitted anonymous
block converts an index-by DATE with `TO_DATE` on the way in (`CallableStatementParameterEngine`
:7341) and `TO_CHAR` on the way out (:7624), and an unzoned TIMESTAMP with `TO_TIMESTAMP` / `TO_CHAR`
(:7343, :7626). The generated wrapper already constructs both with `OracleTypes.VARCHAR` and calls
`setDataType(DATE)` / `(TIMESTAMP)` in `bindParams` — read it in the emitted `IbaTestTestDate`.
**So the MCP layer was the only half refusing**, and unlike TZ — which needed its whole binding
project first — nothing under it had to move. Text conversion, kind, type code, description.

**The `date` kind was NOT reshaped, exactly as this section guessed.** `mcpCollectionElementKind`
now takes the CONTAINER type as a parameter and answers per container: a DATE element of a generated
VARRAY or nested table is still `"date"` and still becomes a `java.util.Date`, which those classes
take happily; only an index-by container gets `"datetext"`. Unzoned TIMESTAMP got `"timestamptext"`
on the index-by arm **and stays `null` everywhere else** — widening it for VARRAYs would have
exposed a path no fixture has ever run, on the way to fixing something else.

**Both masks were MEASURED on 12c before a line was written**, which is this document's own standing
instruction and which settled two things reasoning would have got wrong:

| against its mask | `...36` | `...36.123` | 9 digits | bare date |
|---|---|---|---|---|
| `TO_DATE 'yyyy-mm-dd hh24:mi:ss'` | OK | **ORA-01830** | — | OK, midnight |
| `TO_TIMESTAMP '...ss.ff8'` | **OK** | OK | **ORA-01830** | OK |

So DATE must have any fraction **stripped** and TIMESTAMP must be **capped at eight digits** — and
TIMESTAMP needs no padding, because the unzoned mask tolerates a missing fraction where the zoned one
does not. That last point is why there is no counterpart to `ensureFractionalSeconds()` here.

**Dropping a DATE's fraction is real precision loss and is deliberate.** Oracle's DATE has nowhere
to put it, the alternative is ORA-01830 rather than fidelity, and the SCALAR date path already drops
it silently by binding a `java.util.Date`. The emitted description says so out loud.

## What RAW needed (done 2026-08-26)

**The prediction in the section this replaces was right about the shape and, unusually for this
document, right about the cost too.** RAW is an encoding mismatch where DATE and TIMESTAMP were mask
mismatches: MCP crosses binary as base64, `PlsqlIndexByTable2` carries hex (`getArrayAsRaw` parses
hex pairs, `setArray(byte[][])` hex-encodes). So it wanted its own conversion in both directions
rather than a fourth text kind — and it got one, `McpBinary`, beside `McpDates` and for the same
reason: the emitter cannot test itself.

**The DAO layer had already done the hard half here as well.** The generated wrapper constructs
`new PlsqlIndexByTable2(oracle.jdbc.OracleTypes.VARCHAR, 0)` and its `bindParams` supplies
`setDataType(OracleTypes.RAW)` and `setElementMaxLength(80)` — twice the `RAW(40)` column, because
hex is two characters a byte — while the emitted anonymous block already converted with `HEXTORAW`
in and `RAWTOHEX` out. Read `IbaTestTestRaw`: every line of it predates this work. **The MCP layer
was again the only half refusing.**

**What shipped:**

| | |
|---|---|
| `mcpCollectionElementKind` | `byte[]` in an index-by container → new kind **`rawhex`**; a generated collection class keeps **`raw`** |
| `mcpIndexByTypeCode` | `rawhex` → `OracleTypes.VARCHAR` |
| `toScalarObjectArray` | IN: `McpBinary.toOracleRawText` (base64 → hex) |
| `collectionToJson` | OUT: `McpBinary.fromOracleRawText` (hex → base64), its own branch — this is the only kind here that is not a date |
| `mcpCollectionLabel` | the caller-facing description |
| `mcpProcUnsupportedReason` | the refusal loses its per-kind ternary: only one obstacle is left |

### The one thing base64 cannot tell you, and why it is documented rather than fixed

**A caller who sends hex is not refused.** `DEADBEEF` is eight characters of the base64 alphabet and
decodes cleanly to four completely different bytes. There is no signal to key on, and sniffing for
hex would break every value that is legitimately both — which, for short values, is most of them.

That is worse here than it would be elsewhere, because **hex is the wrong guess a caller is most
likely to make**: a RAW is shown in hex by SQL\*Plus, by a `DESCRIBE`, and by every other tool the
caller has met it in. So the tool description says it out loud —
`array of binary values as base64, NOT hex, e.g. 3q2+7w== for the four bytes DE AD BE EF` — and
`McpBinaryTest.cannotTellHexFromBase64` pins the behaviour so nobody later reads it as an oversight.

The decode is forgiving in the three ways that carry no ambiguity — whitespace stripped, the
URL-safe alphabet accepted, missing `=` padding supplied — and refuses everything else with a
message naming the accepted form, for the reason `McpDates` gives: a model handed "invalid" retries
blind, and a retry loop against a failing tool churns pooled connections.

### The missing guard — CLOSED 2026-08-27, and RAW was not the only one

> **`NUMBER` had no guard either, in both its branches**, found by measuring the arms rather than
> re-reading this section. It matters more than RAW did: a NUMBER index-by only reaches this
> conversion when it does not ride the numeric slot, which a high-precision `number(30,15)` does —
> an ordinary type, unlike the RAW that led here. So if the sparse case has ever bitten a real
> caller, it bit there.
>
> **`IBA_TEST.TEST_SPARSE` is the fixture this section said was missing.** One procedure, six OUT
> collections, indexes 1 and 7 only. Proved in both directions on a live 12c box: guard removed →
> `ORA-01403`, guard restored → succeeds. **Six boxes green at `b618ce6`** — ORCL12 app 955, XE18 /
> ORCL19 / ORCL21 949, FREE23 / FREE26 962, web 470 everywhere, `TSparseIndexBy` passing on all six
> with no skips.
>
> **The sparseness SURVIVES, which was not assumed.** Values come back at positions 1 and 7 with
> the gaps empty, not compacted to the front — on both Oracle lines. Had they compacted, the guard
> would have prevented an error while silently corrupting the index mapping, which is worse than
> the exception it replaced.



**`CallableStatementParameterEngine`'s RAW arm of the OUT loop had no `.exists(i)`**, where the
DATE, TIMESTAMP and zoned arms all did:

```sql
-- before
FOR i IN p_out.FIRST..p_out.LAST LOOP
 P_OUT_v(i) := RAWTOHEX(p_out(i));
END LOOP;
```

An index-by table is sparse by nature — PL/SQL lets a routine assign `p_out(1)` and `p_out(7)` and
nothing between — so a missing index raises `NO_DATA_FOUND` from inside the emitted block, naming
neither the parameter nor the gap. Fixed to match its three siblings.

**Be honest about what verifies it: nothing does.** No fixture returns a sparse RAW collection.
`IBA_TEST.TEST_RAW` is `p_out := p_in` on a dense input, and `PLSQL_INDEXBY_TABLES.PROC_RAW` fills
`p3(1..80)` densely and then assigns `p2(3)`, which leaves `p2` dense too. So this is a fix made on
the strength of three sibling arms doing it differently, not on a failing test — **which is exactly
why it survived**: until this change a RAW index-by could not be published as an MCP tool at all, so
its only caller was a hand-written DAO client whose author had read the routine. A fixture with a
deliberately sparse OUT collection would close it properly and is worth adding if anyone touches
this area again.

## Traps

- **The gate is per ROUTINE, not per parameter.** One unsupported argument hides every tool for that
  routine.
- **Two branches refuse, not one.** A null element kind refuses at the first check;
  a kind that maps to no `mcpIndexByTypeCode` refuses one branch later, with a different message.
  Adding a kind without adding its type code fails quietly in the second place.
- **A type code without a CONVERSION is the newer version of that trap, and it fails LOUDER but
  later.** Every text kind rides the same `OracleTypes.VARCHAR` slot, so adding one to
  `mcpIndexByTypeCode` is enough to make the routine cross — and the emitted
  `toScalarObjectArray` then falls through to its `String.valueOf` default and binds the caller's
  text verbatim. Oracle rejects that if you are lucky. If the text happens to parse under the
  mask, it is accepted as something else.
- **Never hand `PlsqlIndexByTable2` a `java.util.Date`** — measured above.
- **`.description(...)` does not escape.** Plain prose only.
- **`TGen23aiMcp` asserts `tools.size() > 18`**, a lower bound — it does not need updating when the
  surface widens, and its fixture has *scalar* TZ params rather than index-by ones. The original
  plan claimed the opposite on both counts.
- **Measure the mask, do not reason about it.** Every wrong claim in this document came from
  reasoning about formats that a single `to_timestamp_tz` call would have settled in seconds.
- **VERIFY THE ARTIFACT THE TOOL CONSUMES, NOT THE INPUT YOU CHANGED — the third variant of this
  repository's oldest trap, hit on 2026-08-26.** Establishing the 30-tool baseline meant stashing
  the change and regenerating. The stash worked; `git stash show --name-only` listed both files;
  the source was provably reverted. The baseline still came back **34 tools with an identical
  list**, i.e. "the change does nothing" — a clean, plausible, completely false result.
  **`mvn package -DskipTests` still COMPILES the test sources**, the generated harnesses import
  every propfile tree, the two-propfile regen had wiped them, so the build failed at test-compile,
  the shaded jar was never re-shaded — and `testrun_current.sh` drives the shaded jar. Source,
  `target/classes` and the jar all disagreed, and only the third one mattered. Use
  `-Dmaven.test.skip=true`, and check the JAR (`javap -p -c` for a string that cannot exist without
  the change) before believing any regen. What exposed it was the baseline tree containing
  `datetext`, a string impossible without the change; without such a tell, "no difference" reads as
  a finding. Compare with the `MCPDBWIZARD_EXTRA_CP=` that `boxes.env` silently refilled, and the
  `git stash` that took nothing: same shape, three different places.

Copyright 2003-2026 ATB Consultancy Services Ltd
(formerly Orinda Software Ltd, Dublin, Ireland)
