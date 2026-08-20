# TZ index-by collections lose the time zone — plan

> **OPEN, not started.** Diagnosis re-measured end to end on 2026-08-20 against a 12c server and a
> 23ai-line server. **The 2026-08-15 diagnosis this file used to carry was wrong in its central
> claim** — see "Corrections" below before reading anything else, and before quoting the matching
> paragraph in `CLAUDE.md`.
>
> The fixture schema and the `.sql` files named here are **not part of the published repository**;
> `Scripts/check_provisioning.sh` will name the objects a config expects if you want to build
> equivalents.

## The defect, as measured

A PL/SQL index-by collection whose element is `TIMESTAMP WITH [LOCAL] TIME ZONE`:

```sql
type timestamp_tz_iba is table of timestamp with time zone index by binary_integer;
procedure test_timestamp_tz(p_in in timestamp_tz_iba, p_in_out in out timestamp_tz_iba,
                            p_out out timestamp_tz_iba);
```

crosses JDBC through a **`VARCHAR2` shuttle whose conversion masks carry no time zone**, so the
zone cannot be sent and is not returned. The generated wrapper builds this anonymous block:

```sql
TYPE P_IN_t IS TABLE OF VARCHAR2(30) INDEX BY BINARY_INTEGER;   -- the shuttle
p_in IBA_TEST.TIMESTAMP_TZ_IBA;
...
FOR i IN P_IN_v.FIRST..P_IN_v.LAST LOOP
  p_in(i) := TO_TIMESTAMP(P_IN_v(i),'yyyy-mm-dd hh24:mi:ss.ff8');            -- IN
END LOOP;
IBA_TEST.TEST_TIMESTAMP_TZ(p_in => p_in, p_in_out => p_in_out, p_out => p_out);
FOR i IN p_out.FIRST..p_out.LAST LOOP
  IF p_out.exists(i) THEN P_OUT_v(i) := TO_CHAR(p_out(i),'yyyy-mm-dd hh24:mi:ss.ff8'); END IF;
END LOOP;
```

Three things are wrong in those two lines, and each is independently sufficient:

1. **`TO_TIMESTAMP`, not `TO_TIMESTAMP_TZ`.** The value assigned into a `TIMESTAMP WITH TIME ZONE`
   variable has no zone, so PL/SQL attaches the **session** zone silently.
2. **The mask has no `TZR`/`TZH:TZM`.** So an input carrying a zone does not parse at all, and an
   output carrying a zone is rendered without it.
3. **The shuttle is `VARCHAR2(30)`.** `'2019-03-01 14:25:36.123 +05:30'` is exactly 30 characters;
   a region name (`Asia/Calcutta`) needs far more.

**Measured behaviour, both Oracle lines, through the generated DAO:**

| input | 12c | 23ai line |
|---|---|---|
| `2019-03-01 14:25:36.123 +05:30` | `ORA-01830` | `ORA-01830` |
| `2019-03-01 14:25:36.123` (no zone) | OK → returns `2019-03-01 14:25:36.12300000` | identical |

So the zone is not *corrupted*, it is **unreachable**: there is no value a caller can supply that
carries one, and no value they get back that reveals one. The instant depends on the session time
zone of whichever process happened to make the call.

**The fix shape is proven to work.** Run against both lines, `TO_TIMESTAMP_TZ` with a `TZR` mask
round-trips **both** an offset and a region name through the same procedure:

```
in [2019-03-01 14:25:36.123 +05:30]        out [2019-03-01 14:25:36.123000000 +05:30]
in [2019-03-01 14:25:36.123 Asia/Calcutta] out [2019-03-01 14:25:36.123000000 ASIA/CALCUTTA]
```

`TZR` accepts both forms, so one mask covers both and no per-value branching is needed.

## The second, separable symptom: a shadow-type collision on the truncating line

Under `EXTRA_SQL`, `extraObjects.sql` on 12c declares three collection types where the truncating
line declares one:

| | 12c | 18c / 19c / 21c / 23ai / 26ai |
|---|---|---|
| `TIMESTAMP` element | `OSOFTBT_A AS TABLE OF TIMESTAMP(9)` | same |
| `TIMESTAMP WITH LOCAL TIME ZONE` | `OSOFTBQ_A AS TABLE OF TIMESTAMP(9) WITH LOCAL TIME ZONE` | *absent* |
| `TIMESTAMP WITH TIME ZONE` | `OSOFTBH_A AS TABLE OF TIMESTAMP(9) WITH TIME ZONE` | *absent* |

**This is a COLLISION, not an absence, and that is worse.** The generated collection classes exist
on both lines, but on the truncating line `IbaTestTimestampTzIba.recordName` and
`IbaTestTimestampLtzIba.recordName` **both read `OSOFTBT_A`** — the zone-less type. The generator
does not know the three elements differ, so it dedupes them into one.

Root cause, one line, in the runtime library:

```java
// SqlUtils.getUnderlyingOracleDatatype
} else if (theColumnDataType.equals("TIMESTAMP WITH TIME ZONE")
        || theColumnDataType.equals("TIMESTAMPTZ")
        || (theColumnDataType.startsWith("TIMESTAMP")
        && theColumnDataType.endsWith("TIME ZONE"))) {
```

`ALL_ARGUMENTS` (12c) spells it `TIMESTAMP WITH TIME ZONE`; `ALL_PLSQL_COLL_TYPES` — the view the
synthesis reads on every truncating version — spells it **`TIMESTAMP WITH TZ`**, which starts with
`TIMESTAMP` and does **not** end with `TIME ZONE`. Measured:

```
TIMESTAMP WITH TIME ZONE        -> 13  (ORACLE_TIMESTAMPTZ_DATATYPE)
TIMESTAMP WITH LOCAL TIME ZONE  -> 14  (ORACLE_TIMESTAMPLTZ_DATATYPE)
TIMESTAMP WITH TZ               -> 12  (ORACLE_TIMESTAMP_DATATYPE)   <-- wrong
TIMESTAMP WITH LOCAL TZ         -> 12  (ORACLE_TIMESTAMP_DATATYPE)   <-- wrong
```

**Two classifiers disagree, and that is the trap.** `SAAdminWrangler` (~2126) already handles the
abbreviated spellings and sets `typeRecordClass = oracle.sql.TIMESTAMPTZ` correctly — which is why
the generated Java class still has an `oracle.sql.TIMESTAMPTZ[]` constructor while the Oracle type
behind it is zone-less. Fixing the classifier is what closes the gap between them.

## Corrections — what this file and `CLAUDE.md` previously got wrong

Each of these was stated as measured fact and repeated. All four are disproved by the runs above.

1. **"12c emits a real SQL array type and binds through it; the truncating line stringifies."**
   **WRONG.** The generated wrapper `IbaTestTestTimestampTz.java` is **byte-identical** on ORCL12
   and FREE26 apart from the build timestamp. Both emit
   `new PlsqlIndexByTable2(oracle.jdbc.OracleTypes.VARCHAR, 6)`,
   `setDataType(oracle.jdbc.OracleTypes.TIMESTAMP)` and `setElementMaxLength(28)`. **Both
   stringify.** There is no per-version binding strategy to converge on.
2. **"The two Oracle lines use DIFFERENT BINDING STRATEGIES."** Follows from 1; also wrong. The
   only cross-version difference is the shadow-type collision above, and the classes that name it
   are **unreferenced** anywhere in the generated tree — so today that difference has no runtime
   consequence at all. It is a latent hazard, not the live defect.
3. **"Expect PASS on ORCL12 and FAIL on the other five."** Wrong: it fails on **all six**. Any
   Phase-0 test written to that expectation would be marked as passing on the one box where it
   should have been reddest.
4. **`Found SYS.TEST_TIMESTAMP_TZ.TIMESTAMP_WITH_TIME_ZONE` in the log was cited as the signature
   of the reverted fix.** It is not. That exact line appears in a **normal, healthy ORCL12 run**.
   It is noise and proves nothing either way.

Also corrected: the `generic_testd` file-count gap (**119 on ORCL12, 117 elsewhere**) is **not** the
TZ classes — both exist on both lines. The two missing files are `LRowCur.java` and
`LRowCurAttrs.java`, a REF CURSOR row type. **A separate gap; do not use the count as this defect's
regression signal.**

## Still standing from the earlier diagnosis — do not repeat

1. **"Normalise `elem_type_name` in the collection query with the existing decode."** Tried,
   measured, reverted. It rewrites the *string*, which the `SAAdminWrangler` raw-spelling match at
   ~2126 depends on, so the element is then hunted as a record and four wrapper files are lost.
   **The fix below changes the CLASSIFIER, not the string** — that distinction is the whole reason
   it is not the same attempt again, and any patch must be checked against it.
2. **"Fix the shadow-type DDL in `ExtraType` (~258)."** The type never reaches that path on a
   truncating box.
3. **The 84 `Cannot invoke "String.length()"` NPEs in this propfile's genlog are unrelated** —
   identical in count in passing and failing runs. Worth their own look one day.
4. **Check the FILE COUNT as well as `extraObjects.sql`.** Checking the DDL alone is what made the
   reverted attempt look like a success.

## Progress

Phases 0-4 are **DONE** (2026-08-20); Phase 5 is the estate verification and is what remains.

| | | |
|---|---|---|
| 0 | red test | `src/test/generated-harnesses/.../TTzIndexBy.java` — observed failing on both lines first |
| 1 | classifier | `SqlUtils.getUnderlyingOracleDatatype` + `JavaUtils.mapOracleDatatypeToAlphaCharOrLength` |
| 2 | element ladder | TZ/LTZ arms in `CallableStatementParameterEngine`, width 28 → 80 |
| 3 | masks | `TO_TIMESTAMP_TZ` / `TO_CHAR` with `TZR`, both directions; `setDataType` arms |
| 3b | shadow-type DDL | `JavaUtils.oracleSqlTypeName`, used by `ExtraType` |
| 4 | runtime library | `ORACLE_TIMESTAMPTZ_TO_CHAR_MASK`, `ensureFractionalSeconds()`, 3 unit tests |
| 5 | SOAP | `String[]` surface — two dispatch arms, no new emitted code |

**Measured after the fix, generating `generic_testd` against a 12c and a 23ai-line server:**

- the emitted wrapper is **identical on both**;
- `extraObjects.sql` is **identical on both**, with three distinct timestamp collection types;
- file counts **unmoved** (120 / 118 raw), which is where the reverted attempt failed;
- all six live round-trip cases pass on both lines — offset, region name, zone-less with and
  without a fraction, and LTZ instants.

**Two things this turned up that were not in the plan.** (1) Fixing the classifier alone made the
emitted DDL read `TABLE OF TIMESTAMP(9) WITH TZ` — the abbreviated spelling reaching a `CREATE
TYPE`, which is a **syntax error Oracle only raises when the DDL is run**, so it would have
surfaced as a missing type at bind time. Hence Phase 3b, and hence the rule that the normalisation
belongs at the point DDL text is written and nowhere earlier. (2) The `ensureFractionalSeconds`
guard needed a shape check: without one, the second token of any three-token string gets a `.0`,
so a malformed value would reach Oracle altered and be refused with a message describing text the
caller never wrote.

## Phases

**Phase 0 — make it a red test, on all six boxes.**
Add a harness driving `IbaTestTestTimestampTz` / `…Ltz` with a value carrying a **numeric offset**
(`+05:30`) and one carrying a **region name** (`Asia/Calcutta`). Assert the value comes back with
its zone intact. Expect **FAIL everywhere** — today the offset form raises `ORA-01830` and the
zone-less form silently returns a zone-less value. `T10GPlsqlIndexBy` is the natural home; it
already covers `Ts` / `Date` / `Raw` / number index-by tables and stops short of these two.
**Nothing else starts until this is red on a 12c box**, because the box previously believed correct
is the one the old plan would have exempted.

**Phase 1 — classify the abbreviated spellings.**
`SqlUtils.getUnderlyingOracleDatatype`: recognise `TIMESTAMP WITH TZ` → `ORACLE_TIMESTAMPTZ_DATATYPE`
and `TIMESTAMP WITH LOCAL TZ` → `ORACLE_TIMESTAMPLTZ_DATATYPE`. Purely additive to a decode ladder,
but it is in **`com.mcpdbwizard.pub`**, whose signatures are load-bearing for every program this
generator has ever produced — the return value changes for two inputs that previously mapped to
plain `TIMESTAMP`, so it must be treated as a behaviour change and regression-run, not waved
through as a one-liner. **`JavaUtils` carries a second copy of the same ladder (~856); decide
deliberately whether it moves too** — if it does, `oracle2JavaDatatype` starts returning
`oracle.sql.TIMESTAMPTZ` directly and the `SAAdminWrangler` raw-spelling rescue at ~2126 stops
firing. That is arguably the tidier end state and is certainly a different code path; do not change
both halves in one commit without a byte-diff between them.

**Phase 2 — give the index-by element ladder TZ arms.**
`CallableStatementParameterEngine` (~1032) has arms for TEXT / DATE / TIMESTAMP / NUMBER / BINARY
and an `else` that throws `CSUnsupportedDatatypeException`. Add `ORACLE_TIMESTAMPTZ_DATATYPE` and
`ORACLE_TIMESTAMPLTZ_DATATYPE` arms setting `plsqlIndexByDataType` to
`OracleTypes.TIMESTAMPTZ` / `TIMESTAMPLTZ`, `plsqlIndexByRealDataType` to `VARCHAR`,
`plsqlIndexByDataDecPlaces` to 9, and a **`plsqlIndexByDataLength` wide enough for a region name**
— 64 rather than the 28 the plain-TIMESTAMP arm uses. **Note that after Phase 1 this ladder's
`else` becomes reachable on 12c for the first time**, since 12c's `TIMESTAMP WITH TIME ZONE` was
already classifying as 13/14 and only ever reached the TIMESTAMP arm because the walk it fed came
from a different query. Adding the arms is therefore required by Phase 1, not merely enabled by it.

**Phase 3 — fix the four emission sites that write the masks.**
`CallableStatementParameterEngine` lines ~7290, ~7292 (IN) and ~7565, ~7568 (OUT), plus the shuttle
declaration at ~7049 and the `setDataType` ladder at ~4752 / ~4888. Emit
`TO_TIMESTAMP_TZ(v,'yyyy-mm-dd hh24:mi:ss.ff9 TZR')` on the way in and
`TO_CHAR(x,'yyyy-mm-dd hh24:mi:ss.ff9 TZR')` on the way out. **`TZR` covers both an offset and a
region name — verified on both lines — so resist adding a second mask and a fallback.** New masks
belong beside the existing `ORACLE_TIMESTAMP_TO_CHAR_MASK` / `ORACLE_DATE_TO_CHAR_MASK` constants
on `PlsqlIndexByTable2`, since that is where the emitter already reads them from.
**Decide the LTZ semantics explicitly:** an `LTZ` value has no zone of its own — it is normalised to
the session zone — so `TO_CHAR(..., 'TZR')` on one renders the *session's* zone. That may be the
right answer or may be misleading; it is a decision, not a detail.

**Phase 4 — the runtime library's element conversion.**
`PlsqlIndexByTable2` can only carry `String` or `BigDecimal`, and its constructor builds a
`DecimalFormat` whenever precision is non-zero — so on a precision-6 table `theDateFormat` is left
null and `setArray(Object[])` with anything that is not a `String` throws from `DecimalFormat`. A
caller can therefore only use the `String` path today. Two additive methods —
`setArray(oracle.sql.TIMESTAMPTZ[])` and `getArrayAsTimestamptz()` — give the typed route without
touching a signature. **Additive only**: this class ships with every generated DAO layer, and
`getArrayAsTimestamp()` must keep behaving exactly as it does for the existing element kinds.

**Phase 5 — verify.**
`generic_testd` on a 12c box and a 23ai-line box: `extraObjects.sql` identical, **three distinct
timestamp collection types on both**, `IbaTestTimestampTzIba`/`…Ltz` naming their own type rather
than `OSOFTBT_A`, Phase 0 green on all six. Then the full estate — this touches `SqlUtils` and the
engine's shared emission ladders, so byte-identity with earlier trees will break for **every**
config that has a date or timestamp index-by, and that must be inspected rather than assumed
benign.

**Residual — MCP still refuses a TZ index-by, and that refusal is now unnecessary.**
`mcpCollectionElementKind` returns null for a zoned element, so `mcpProcUnsupportedReason` skips the
routine — the same before this fix and after it, so nothing regressed and no MCP surface moved. But
the reason for the refusal has gone: it was that `PlsqlIndexByTable2` "has no third representation"
and would silently stringify, which is exactly what the TZR mask now stops being lossy. Exposing it
means a `string` element kind plus a type code in `mcpIndexByTypeCode`; deliberately **not** done
here, because widening an agent-facing surface is its own decision and belongs in its own change.

**Phase 6 — the sibling shapes, as a separate call.**
`DATE` index-by has the same shape and the same `TO_DATE`/`TO_CHAR` treatment; it loses nothing
today because a `DATE` has no zone, so it is genuinely fine. **A bare `TIMESTAMP` index-by is
fine too.** What is *not* obviously fine is `INTERVAL`, which has no arm at all and hits the
`else`. Decide whether that is in scope.

## Decisions — TAKEN 2026-08-20, with what they were taken on

**1. Which conversion mask.** *Decision: `TZR`, plus client-side normalisation.* No single mask
covers every input shape, which is the fact the decision turns on. Measured identically on 12c and
23ai:

| mask | `+05:30` | `Asia/Calcutta` | `…36.123` | `…36` (no fraction) |
|---|---|---|---|---|
| the old `.ff8`, unzoned | ORA-01830 | ORA-01830 | OK | OK |
| `.ff9 TZR` | OK | OK | OK | **ORA-01843** |
| `.ff9 TZH:TZM` | OK | **ORA-01858** | OK | OK |

`TZH:TZM` would have been strictly additive but cannot express a region name, and a region name is
the only form that stays correct across a daylight-saving transition — so it buys the zone and
loses it again twice a year. `TZR` alone would have broken a caller hand-writing a fraction-less
string, which works today. **So the leniency was put where it can be seen and tested rather than
into the mask**: `PlsqlIndexByTable2.ensureFractionalSeconds()`, emitted into the generated
`bindParams` for zoned elements only.

**2. `JavaUtils`.** *Decision: yes, and the reason found on the way in is the important part.* The
second ladder is not a duplicate of `SqlUtils`' — `oracle2JavaDatatype` delegates, so there was no
duplicate to worry about. The one that mattered is
`JavaUtils.mapOracleDatatypeToAlphaCharOrLength`, whose returned **letter is the discriminator in
the shadow type's NAME** (`Q`, `H`, `T` → `OSOFTBQ_A`, `OSOFTBH_A`, `OSOFTBT_A`). That is the whole
mechanism of the collision: a spelling that misses the `Q`/`H` arms is handed the unzoned type's
name and dedupes into it.

**3. What an LTZ renders as.** *Decision: the same `TZR` mask as TZ.* Measured that `TO_CHAR`
accepts `TZR` for a LOCAL value on both lines — it does not error — and renders the **session**
zone, e.g. `2019-03-01 08:55:36.123000000 GMT`. Emitting it beats omitting it: a bare wall clock
does not say which zone it is a wall clock in. One line serves both types.

**4. Backward compatibility.** *Answered: yes, preserved, and it needed the work in decision 1.*
`TZR` with a zone-less value is accepted and takes the session zone — same answer as today — **but
only if a fractional part is present**, which is precisely why `ensureFractionalSeconds()` exists.

**5. SOAP scope.** *Decision: fix it in this pass — and it turned out cheap.* Priced as "a new WS
type and new generated conversion helpers"; in fact the zoned element crosses as `String[]`, and
`createIndexByTableFromStringArray` / `createStringArrayFromIndexByTable` **already exist**, so it
needed two dispatch arms and no new emitted code. A zone crosses SOAP as text for the same reason a
duality-view document does: `java.sql.Timestamp` has nowhere to put one.

## Reproducer

Needs two servers: one on the 23ai line and one 12c. Substitute your own hosts.

```sh
# generate the same config against each, into separate trees, and diff
#   the wrapper  -> expect IDENTICAL (that is the point)
#   extraObjects -> expect three timestamp collection types on 12c, one elsewhere
#   the two collection classes' recordName -> expect the collision on the truncating box
grep -n "recordName" <tree>/plsql/IbaTestTimestampTzIba.java \
                     <tree>/plsql/IbaTestTimestampLtzIba.java
```

For the live half, drive the generated wrapper directly:

```java
p.paramPIn.setArray(new Object[]{ "2019-03-01 14:25:36.123 +05:30" });
p.executeProc();                        // ORA-01830 today, on every version
```

Copyright 2003-2026 ATB Consultancy Services Ltd
(formerly Orinda Software Ltd, Dublin, Ireland)
