# Let a TZ index-by table cross MCP — plan

> **DONE for TZ, 2026-08-21, except the estate run.** `generic_testd_mcp` went 28 -> 30 tools,
> gaining `iba_test_test_timestamp_tz` and `…_ltz`; the diff is additions only and the tree compiles
> (129 classes). Db-free suites green: app 850/0/0, web 398/0/0.
>
> **THREE CLAIMS IN THIS PLAN WERE WRONG, and the first one mattered.**
>
> 1. **"TZ crosses as text with no new emitted code."** It does not. The emitted mask is
>    `'yyyy-mm-dd hh24:mi:ss.ff9 TZR'` — a SPACE between date and time — while MCP crosses dates as
>    ISO with a `T`. Measured: `to_timestamp_tz('2019-03-01T14:25:36.0+05:30', <that mask>)` raises
>    **ORA-01858**. So TZ needed exactly the mask conversion this plan said only DATE needed. It is
>    `McpDates.toOracleTimestampText` / `fromOracleTimestampText`, a textual `T`↔space swap —
>    deliberately NOT a parse-and-reformat, which would resolve the value to an instant and re-render
>    the caller's `+05:30` in the server's zone. The separator before the ZONE turns out to be
>    flexible, which is why only the one character moves.
> 2. **"`TGen23aiMcp` asserts an exact tool-list count and will need updating."** It asserts
>    `tools.size() > 18`, a lower bound, and its fixture has SCALAR TZ params rather than index-by
>    collections — so it is untouched.
> 3. Phase 0 proposed extending the 23ai fixture. Unnecessary: `generic_testd` already has
>    `IBA_TEST.TEST_TIMESTAMP_TZ` and `…_LTZ`, so its `_mcp` sibling gives a before/after for free.
>
> **A trap this hit, documented in the sibling plan and walked into anyway:** the first version of
> the description quoted its examples, and `.description(...)` does not escape — the generated file
> came out with bare double quotes inside a string literal and would not have compiled. Caught by
> reading the emitted line, not by the tool count, which was correct throughout.
>
> **DATE and RAW remain gated**, but the refusal now names the real obstacle per kind instead of
> blaming `PlsqlIndexByTable2` for a stringification that no longer happens.

## What is gated, and why the reason has retired

`mcpProcUnsupportedReason` skips a whole PL/SQL routine when any index-by parameter's element is not
NUMBER or VARCHAR2:

```java
return "parameter " + argName + " is an index-by table of " + elementKind
        + " (only NUMBER and VARCHAR2 elements cross)";
```

The stated reason is that `PlsqlIndexByTable2` is "deliberately binary", so a date or raw element
"would be silently stringified and come back as something the caller did not put in".

**That was true and no longer is.** The TZ binding fix gave the DAO path `TO_TIMESTAMP_TZ` with a
`TZR` mask in both directions, an 80-character shuttle and `ensureFractionalSeconds`, verified live
on 12c and 23ai preserving both `+05:30` and `Asia/Calcutta`. The stringification the gate exists to
prevent does not happen any more.

**Nothing regressed when that landed** — a zoned element classified as unsupported before the fix
(`java.sql.Timestamp`) and still does after it (`oracle.sql.TIMESTAMPTZ`), so the MCP surface never
moved and the six-box estate stayed green. This is a gate that is now *unnecessary*, not one that is
newly wrong.

## TZ is nearly free, because the wrapper already does the work

**The MCP path drives the generated wrapper directly** — it sets parameters and calls
`executeProc()`, so the wrapper's own `bindParams` runs. That is where `setDataType(TIMESTAMPTZ)`,
`setElementMaxLength(80)` and `ensureFractionalSeconds()` already happen. The MCP layer does not need
to reproduce any of it; it only has to hand over a `PlsqlIndexByTable2` holding strings.

Measured, on the real class:

```
new PlsqlIndexByTable2(OracleTypes.VARCHAR, 0)
  setArray(new Object[]{"2003-06-09T22:44:00Z"})  -> kept verbatim
  setArray(new Object[]{new java.util.Date(0L)})  -> IllegalArgumentException:
                                                     Cannot format given Object as a Number
```

So **crossing as text works and crossing as `java.util.Date` does not** — which is the whole shape of
this change, and the reason DATE is harder than TZ rather than easier.

The OUT direction needs nothing: `collectionToJson` reads `getCurrentValuesAsObject()`, which for a
VARCHAR-backed table is the `String[]` the database put there, already in the `TZR` form.

**So the TZ half is two mappings and a description:**

| | |
|---|---|
| `mcpCollectionElementKind` | map `oracle.sql.TIMESTAMPTZ` / `TIMESTAMPLTZ` to element kind `string` |
| `mcpIndexByTypeCode` | return `OracleTypes.VARCHAR` for it |
| the tool description | say which forms are accepted — see below |

Reusing `string` rather than inventing a kind is deliberate: `toScalarObjectArray`'s `else` branch
already does `String.valueOf`, and `collectionToJson` already passes strings through, so **no new
emitted code is needed at all**. That is the same economy the SOAP side turned out to have.

## The description is the part that must not be skimped

This is the defect the live MCP report found in the DATE crossing, and it applies here with more
force: a zoned timestamp has *more* accepted spellings, not fewer. The parameter's description must
name them, in the way `McpDates`' message now does:

```
p_in (TIMESTAMP WITH TIME ZONE index-by table, array of ISO-8601 strings,
      e.g. "2019-03-01T14:25:36+05:30" or "2019-03-01T14:25:36 Asia/Calcutta")
```

**And a LOCAL timestamp needs a sentence of its own.** It keeps no zone — it is normalised to the
server's — so a value comes back rendered in the *server's* zone, not the caller's. A model given an
array of `+05:30` values and handed back `GMT` ones will otherwise conclude the call failed.

## DATE and RAW are the same gate and NOT the same job

Do not fold them in on the grounds that the gate is shared.

**DATE has a mask mismatch.** The DAO's DATE index-by converts with
`ORACLE_DATE_TO_CHAR_MASK = "yyyy-mm-dd hh24:mi:ss"` — a space, no `T`. MCP crosses dates as
ISO-8601 with a `T`. So a DATE index-by cannot simply cross as text the way TZ can: either the
crossing converts ISO to the Oracle mask on the way in and back on the way out, or the emitted
PL/SQL learns the ISO mask. **And the existing `date` element kind is actively wrong here** — it
produces `java.util.Date` objects, which is precisely what throws.

**RAW has an encoding mismatch.** MCP crosses RAW as base64; `PlsqlIndexByTable2` carries it as hex
(`getArrayAsRaw` parses hex pairs). Crossing needs a base64↔hex conversion, which is small but is
real emitted code.

Both are worth doing and neither is this change.

## The decision this actually needs

**Should an agent be handed a zoned collection at all?** The mechanism now works; that is not the
question. The DAO path is driven by a developer who has read the parameter. MCP hands it to a model
that will emit whatever ISO-ish string it likes, and a zone that *converts* silently is a different
risk from one that fails loudly.

Two honest positions:

- **Expose it.** The product goal is to call any PL/SQL routine whatever its inputs. A routine is
  currently invisible in its entirety because of one parameter, which is a bigger surprise than a
  fiddly type — and the conversion is now correct and documented.
- **Leave it gated, and say so.** Change the refusal to name the real reason rather than the retired
  one, so the next person does not re-derive this whole analysis from a message that is no longer
  true.

**The second is strictly better than today whatever is decided about the first**, and costs one
string. Even if the gate stays, the message should not keep claiming a defect that has been fixed.

## Phases

**Phase 0 — a red test.** `TGen23aiMcp` drives a real server; the fixture package would need a TZ
index-by routine (`sql/datatypes_23ai_gen.sql`). Assert the tool EXISTS, then round-trip `+05:30`
and a region name and require them back intact. Expect red on "tool exists" today.

**Phase 1 — the two mappings**, as above.

**Phase 2 — the description**, including the LTZ sentence.

**Phase 3 — verify.** This adds tools to any config with such a routine, so **file counts move and
tool counts move**: `TGen23aiMcp` asserts an exact tool-list count and will need updating, which is
the signal that the surface really did widen. Then the six-box estate, because it changes emitted
output for every `MCP_SERVER=YES` config that has one.

**Phase 4 — decide DATE and RAW separately**, with the mask and encoding work above priced in.

## Traps

- **The gate is per ROUTINE, not per parameter.** One unsupported parameter hides every tool for that
  routine, which is why this is worth more than it looks.
- **`mcpIndexByTypeCode` returning null is what triggers the refusal**, not the element kind being
  null — a kind that maps to no type code fails the same way, silently, one branch later.
- **Do not give `PlsqlIndexByTable2` a `java.util.Date`.** Measured above. The existing `date`
  element kind does exactly that, so reusing it for TZ would look right and throw at run time.

Copyright 2003-2026 ATB Consultancy Services Ltd
(formerly Orinda Software Ltd, Dublin, Ireland)
