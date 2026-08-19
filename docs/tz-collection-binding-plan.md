# TZ index-by collections bind as VARCHAR on 18c+ — plan

> **OPEN, not started.** Diagnosis measured 2026-08-15; no fix attempted beyond three that were
> tried and reverted (see "Do not repeat"). Estimated one focused session plus an estate run.

## The defect

A PL/SQL index-by collection whose element is `TIMESTAMP WITH [LOCAL] TIME ZONE`:

```sql
type timestamp_tz_iba is table of timestamp with time zone index by binary_integer;
procedure ts_tz_sample(p_in in timestamp_tz_iba, p_in_out in out timestamp_tz_iba, ...);
```
(`app/sql/generic_testd.sql`, package `IBA_TEST`)

generates **different binding strategies on different Oracle lines**:

| | 12c (ORCL12) | 18c / 19c / 21c / 23ai / 26ai |
|---|---|---|
| element resolves as | a typed array | `ORACLE_SCALER_TYPE` |
| bound through | a real SQL array type | `PlsqlIndexByTable2` |
| `extraObjects.sql` | `OSOFTBH_A AS TABLE OF TIMESTAMP(9) WITH TIME ZONE` | *(absent)* |
| emitted wrapper | typed | `new PlsqlIndexByTable2(oracle.jdbc.OracleTypes.VARCHAR, 6)` |

**`PlsqlIndexByTable2` is deliberately binary — VARCHAR or numeric only.** So on five of the six
boxes a timestamp-with-time-zone collection is **bound as strings**.

**Why that is worse than it sounds.** `oracle.sql.TIMESTAMPTZ`'s string constructor SILENTLY
DISCARDS a numeric UTC offset — `"… +05:30"` comes back as `GMT`, wall clock preserved, instant
moved by 5½ hours (recorded in `app/CLAUDE.md` under the driver hazard). A region name survives; an
offset does not. So the failure is not "it throws", it is "the instant is wrong".

The MCP layer already refuses this shape for exactly this reason — `mcpProcUnsupportedReason`
rejects a date or raw index-by because `PlsqlIndexByTable2` cannot carry them. **The DAO wrapper has
no such refusal and generates the VARCHAR bind anyway.**

## Why it survived

`generic_testd` generates 119 files and is green on all six boxes. Nothing drives these two
procedures: `T10GPlsqlIndexBy` covers `Ts` / `Date` / `Raw` / number index-by tables and
`IbaTestTestNumber`, but **not** `IbaTestTestTimestampTz` or `…Ltz`. The divergence is invisible to
the suite and to the file counts.

## Do not repeat — three explanations, all measured and disproved

1. **"The NPEs cause it."** `generic_testd`'s genlog carries 84 `Cannot invoke "String.length()"`
   errors. They are IDENTICAL in count in the passing 119-file run and the failing 115-file one.
   Long-standing noise that generation swallows; unrelated. (Worth its own look one day — 84
   swallowed errors in a green propfile is not nothing.)
2. **"Normalise `elem_type_name` with the existing decode."** Tried. Produces the two missing type
   declarations AND drops the propfile to 115 files, losing `IbaTestTestTimestampTz`, `…TzReturn`,
   `…Ltz`, `…LtzReturn`. `SAAdminWrangler` (~2126) already handles these elements and matches on the
   **RAW** spelling (`"TIMESTAMP WITH LOCAL TZ"`), so normalising breaks that match; the element is
   then hunted as a record and the log shows a bogus `Found
   SYS.TS_LTZ_SAMPLE.TIMESTAMP_WITH_LOCAL_TIME_ZONE`. **The un-normalised name at the
   collection arm is deliberate.**
3. **"Fix the shadow-type DDL in `ExtraType` (~258)."** The type never reaches that path on a
   truncating box, so there is nothing there to fix.

**And the meta-trap:** checking `extraObjects.sql` alone made hypothesis 2 look like a success. Only
the FILE COUNT exposed it. Any attempt must check both.

## Phases

**Phase 0 — make it a failing test.** Extend `T10GPlsqlIndexBy` (or add a sibling) to round-trip a
TZ and an LTZ index-by through `IbaTestTestTimestampTz` / `…Ltz` with a value carrying a **numeric
offset** (`+05:30`), not a region name — the offset is what the string path loses. Expect PASS on
ORCL12 and FAIL on the other five. **Nothing else starts until the defect is a red test**, because
every hypothesis so far looked right until measured.

**Phase 1 — understand 12c's route, don't guess it.** Establish, from a real ORCL12 run, how the
element is classified, what `getChildRecord` returns, which `objectType` it carries, and where
`OSOFTBH_A` is created. The truncating path must arrive at the same place; today nobody has written
down what that place is. Output: a short note in this file, not code.

**Phase 2 — classify the element correctly on the truncating versions.** The element must resolve to
the same typed shape 12c produces, WITHOUT normalising `elem_type_name` (hypothesis 2). Likely means
teaching the synthesis/classification arm that `TIMESTAMP WITH TZ` / `TIMESTAMP WITH LOCAL TZ` are
TZ scalars, in the vocabulary the existing `SAAdminWrangler` match already speaks.

**Phase 3 — verify on both lines.** `generic_testd` on FREE26 and ORCL12: `extraObjects.sql`
identical, **file count 119 on both**, Phase 0's test green everywhere. Then the full estate.

**Phase 4 — consider the sibling shapes.** `DATE` and `RAW` index-by are rejected by MCP for the
same reason but still generate VARCHAR/numeric binds in the DAO layer. Decide whether they are the
same defect (probably) and whether they are in scope (separate call).

## Decisions wanted before starting

1. **Is a wrong instant acceptable until fixed?** If not, an interim guard is cheap: make the
   generator REFUSE a TZ index-by param on the truncating versions — the wrapper stops generating,
   which is loud, rather than binding strings, which is silent. That trades four generated files for
   honesty and would need the floors adjusted.
2. **Scope: TZ only, or `DATE`/`RAW` index-by too?** Same root cause, wider blast radius.
3. **Does any customer rely on the current VARCHAR behaviour?** If a deployment feeds region-name
   strings it works today and would keep working; an offset silently does not.

## Reproducer

Needs two servers: one on the 23ai line (which truncates `ALL_ARGUMENTS`) and one 12c
(which does not). Substitute your own hosts below — `Scripts/boxes.env` names them if you
have one.

```sh
# on a TRUNCATING box (23ai line)
MCPDBWIZARD_TEST_HOST=$TRUNCATING_HOST MCPDBWIZARD_TEST_SID=/$TRUNCATING_SID \
MCPDBWIZARD_TEST_URL="jdbc:oracle:thin:@$TRUNCATING_HOST:1521/$TRUNCATING_SID" \
  app/Scripts/testrun_current.sh generic_testd
grep -n "PlsqlIndexByTable2" app/target/regen/Src/generic_testd/*/*/*/*/*/*/plsql/IbaTestTestTimestampTz.java
# -> new PlsqlIndexByTable2(oracle.jdbc.OracleTypes.VARCHAR, 6)

# then on a 12c box (MCPDBWIZARD_TEST_HOST=$ORACLE12_HOST MCPDBWIZARD_TEST_SID=/$ORACLE12_SID), diff:
#   app/target/regen/Src/generic_testd/*/*/*/*/*/*/extraObjects.sql
```

**Check the FILE COUNT too**, not just `extraObjects.sql` — floor 119 for `generic_testd`.
The wrong fix produces the two missing type declarations while dropping four wrapper
files, so the diff alone makes it look right.

Copyright 2003-2026 ATB Consultancy Services Ltd
(formerly Orinda Software Ltd, Dublin, Ireland)
