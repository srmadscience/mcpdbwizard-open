# Item 7 — Nested records (record-in-record): implementation plan

Item 7 of `docs/mcp-datatype-residuals-plan.md`. The last remaining datatype residual
(Items 8/9 are won't-do / driver-blocked). Items 1–6 are done and shipped.

## Goal

Make the generator emit **populated nested record classes** so a PL/SQL record whose field is
itself a record (or object/collection) generates real, usable Java — and therefore crosses MCP as a
nested JSON object. The MCP layer needs **no changes**: `RECORD_MAPPER` (Jackson, field-based)
already recurses for arbitrary nesting (`SAAdminWrangler.java:4969`), and `mcpIsRecord` already gates
on a non-`Object` class. This is a **base-generator** item; SOAP and table users benefit too, not
just MCP.

## Exact root cause (found in code, not guessed)

`SAAdminWrangler.java:17651-17666`. When 23ai's `ALL_ARGUMENTS` drops a record's field child rows,
the generator synthesizes them from `ALL_PLSQL_TYPE_ATTRS` via `plsqlRecordFieldArgQrySelect`
(`SqlStatementDictionary.java:889`). But that query is **flat / single-level**, and the consuming
code **deliberately discards the whole synthesis** if any field has a non-null `TYPE_OWNER` (a nested
record / object / collection field), leaving the parent record an empty stub. The in-code comment
says: "A record with a nested package-type field … needs the engine to recurse into a structure this
flat ALL_PLSQL_TYPE_ATTRS row can't express … nested record fields are a separate, deeper sub-case."
That sub-case is Item 7.

The engine's nesting machinery already exists and works: `getChildRecord` (`:22864`) recurses on
`procDataLevel`, and the collection-of-record path (Item 1) already synthesizes `DATA_LEVEL+1/+2`
rows (`plsqlCollRecordArgQrySelect`, `SqlStatementDictionary.java:825`) that the engine consumes
successfully. So the gap is **producing the nested field rows**, not teaching the engine to consume
them — *pending Phase 0 confirmation*.

## Phase 0 — resolve the one scope-deciding unknown (DO FIRST)

Does the engine build nested record classes correctly **on 12c**, where `ALL_ARGUMENTS` returns the
nested `DATA_LEVEL` rows natively (so no synthesis runs)?

Ready-made fixture: `sql/multirec.sql` — `order_rec_t` has two `account_address_t` record fields;
`initOrderType` returns `order_rec_t`. Currently **not wired into any propfile**.

- Load it on ORCL12 (APPSCHEMA), generate a wrapper for it, inspect the generated `account_address_t`
  and `order_rec_t` classes.
- **If 12c builds them correctly** → Item 7 = make the 23ai synthesis reproduce 12c's nested rows
  (medium effort, one synthesis site).
- **If 12c is also flat/empty** → the engine's record emitter needs work too (larger).

Code reading suggests the first outcome; this test decides the true scope.

## Approach (assuming Phase 0 confirms the engine recurses)

**Phase 1 — recursive field synthesis.** Replace the "discard if non-scalar" cop-out with recursion.
Preferred: **Java-side recursion** in `SAAdminWrangler` (mirrors `getChildRecord`, reuses the existing
query, avoids fragile hierarchical SQL and the known 19c `ALL_ARGUMENTS`/`ALL_PLSQL_*` bind-loss
quirks):
- When a synthesized field row has a non-null `TYPE_OWNER` resolving to a `PL/SQL RECORD`, recursively
  run `getPlsqlRecordFieldQry` for that sub-record at `DATA_LEVEL+1`, and splice its rows in
  immediately after the field row (depth-first), reproducing the `DATA_LEVEL`-nested shape 12c's
  `ALL_ARGUMENTS` returns.
- Guard with a depth cap / visited-set (defensive; PL/SQL records can't be value-recursive, so depth
  is finite).
- Object / collection fields: route to the existing object/collection synthesis arms, OR — to keep
  Item 7 tightly scoped — recurse only for **record** fields and keep discarding object/collection
  fields (documented), shrinking blast radius. Decide after Phase 0.

**Phase 2 — MCP fixture + crossing.** Add a **procedure** (not a function — function-return records
are still excluded from MCP by design) with nested-record IN/OUT params to the `generic_test_23ai`
fixture, e.g. `echo_order(p_in IN order_rec_t, p_out OUT order_rec_t)`. Confirm the tool emits a
nested JSON object schema and round-trips via `RECORD_MAPPER`. No generator MCP-layer change expected.

## Fixtures & tests

- Reuse `sql/multirec.sql` for the base-generator (compile + SOAP) proof on 12c and 23ai.
- Add a nested-record proc to `sql/datatypes_23ai_gen.sql` + `Propfiles/generic_test_23ai.pb2` +
  `TGen23aiMcp` inline DDL, with a round-trip assertion (nested object in → same nested object out)
  and a matching `prototypes/mcp/smoke_test_generated.py` check.

## Risks

- **Blast radius**: touches record synthesis, which every generated program with records links
  against. Mitigate with the byte-diff discipline used for LONG/TZ (unaffected trees must stay
  byte-identical) and the full three-line regression.
- **19c dictionary quirks** — another reason to prefer Java recursion over a recursive CTE.
- **Deep / cyclic types** — capped defensively.

## Verification bar (same as prior base-gen items)

Byte-identical proof on unaffected canonical trees; `multirec` + nested-proc compile and round-trip;
`TGen23aiMcp` new checks green; full regression **ORCL12 (base) / ORCL21 (`-Pharnesses-longids`) /
FREE23 (`-Pharnesses-23ai`)**.

## Rough size

Larger than LONG/TZ — a genuine base-gen change to the record walk, but bounded to one synthesis site
if Phase 0 confirms the engine recurses. ~1–2 focused sessions including multi-box regression.

## Phase 0 result (run 2026-07-20, ORCL12 12c) — SCOPE CHANGED

**The engine is broken on 12c too — the fix is in the record-class walk, not (primarily) the 23ai
synthesis.** Reproduction: loaded `sql/multirec.sql` into APPSCHEMA on ORCL12 and generated a scratch
propfile introspecting `MULTIREC.INIT_ORDER_TYPE` (function returning the nested record
`order_rec_t`). Result: `generated 59 java files, COMPILE FAILED` with **100 errors, all in the
parent `MultirecSalesOrderT.java`**; the nested `MultirecAccountAddressT.java` built **cleanly**.

12c's `ALL_ARGUMENTS` *does* return the nested rows (verified: lvl=0 `SALES_ORDER_T` → lvl=1
`POSTAL_ADDRESS` = `PL/SQL RECORD` `ACCOUNT_ADDRESS_T` → lvl=2 its fields). The parent record class
is built with **both**:
- the CORRECT typed nested field — `paramPostalAddress` / `getParamPostalAddress()` →
  `MultirecAccountAddressT` (array slot `[1]`), and `paramDefInstallAddress` (slot `[26]`); AND
- the nested record's sub-fields ALSO flattened onto the parent (`setParamPublicId`,
  `setParamAddressType`, `setParamFirstname`, …). Because `order_rec_t` has TWO `account_address_t`
  fields (`postal_address`, `def_install_address`), the flattened sub-field setters **collide** →
  "method setParamPublicId(...) is already defined". The array-slot gap (`[1]` → `[26]`) shows the
  parent even reserves slots for the flattened nested fields.

**Diagnosis:** the parent record-class field walk creates the typed nested field correctly but then
does NOT skip the deeper-`DATA_LEVEL` rows that belong to that nested record — it flattens them into
the parent as well. The nested class is built separately and is fine.

**Revised scope (both paths need work, engine first):**
1. **Engine (the core, fixes 12c):** in the parent record-class field walk, when a field row is itself
   a record (a `DATA_LEVEL` = N `PL/SQL RECORD`/OBJECT row with a type), emit ONLY the typed nested
   field and **skip the following `DATA_LEVEL` > N rows** (they belong to the nested class, which is
   built separately). Find the walk that emits the flattened setters and add the skip. This alone
   should make 12c/21c/18c/19c compile and round-trip nested records.
2. **23ai synthesis (Phase 1 as written):** 23ai drops the lvl>0 rows, so after the engine fix a
   nested record is still an empty stub there — the synthesis must recurse to reproduce the nested
   rows (the flat synthesis is currently discarded when a field is non-scalar). Build on the fixed
   engine.

**So the original plan's Phase-1 "recursive synthesis" is necessary but NOT sufficient — the engine
record-class walk is the first and load-bearing fix.** Re-scope estimate upward slightly; still one
engine site + one synthesis site, but the engine site is the harder/higher-blast-radius one.

Reproduction recipe (recreate when implementing): load `sql/multirec.sql` (append `exit;` — the file
has no terminator and sqlplus `@` hangs without it) into the target schema; make a scratch propfile
by stripping `generic_test_23ai.pb2` of all `PROC_`/`TABLE_` lines (set `WEB_SERVICES=NO`,
`MCP_SERVER=NO`, `EXTRA_SQL=NO`, a scratch `PACKAGE_NAME`) and adding one `PROC_0` =
`MULTIREC.INIT_ORDER_TYPE`; regen. NOTE: don't leave `MULTIREC` in a schema a canonical propfile
introspects, and don't leave the scratch `Propfiles/generic_test_*.pb2` in place — both can pollute a
no-arg / canonical regen.

## Phase 0.5 — implementation attempt (2026-07-20, WIP reverted)

Attempted the 23ai path first (the MCP deliverable). Two engine changes tried, then reverted to keep
the tree clean+green because a THIRD, deeper gap surfaced that needs high-risk surgery on the record-
matching engine.

**What was tried (worked as far as it went):**
1. `SqlStatementDictionary.plsqlRecordFieldArgQrySelect` — promote a record-typed attribute to
   `DATA_TYPE='PL/SQL RECORD'` via a `case … exists(all_plsql_types … typecode='PL/SQL RECORD')`
   (TYPE_OWNER/NAME/SUBNAME already identify the nested record). Without this the flat synthesis
   spelled the field as the record's NAME, which the engine can't classify.
2. `SAAdminWrangler` adopt-guard (~17657) — relaxed "discard if any field non-scalar" to "discard
   only if a field is an OBJECT/COLLECTION (TYPE_OWNER set AND DATA_TYPE≠'PL/SQL RECORD')", so a
   record with nested-record fields is adopted.

**Result:** with fixture `FIXTURE_PKG.contact_rec (label VARCHAR2, person person_rec)` +
`nested_record_echo(p_in IN contact_rec, p_out OUT contact_rec)` on FREE23, the tree **regenerated and
compiled (180 classes)** and the parent `FixturePkgContactRec` gained the nested `paramPerson` field
(slot [1]) — a real improvement over the empty stub.

**The blocking third gap:** `paramPerson` is typed **`Object`**, not `FixturePkgPersonRec`, so at runtime
the MCP call fails with `Database error: Unsupported data type seen` (the Object field can't be bound).
Root cause: `CallableStatementParameterEngine` (~line 725-836) resolves a nested record field to its
generated class by matching `theRecords[]` on the field's **argument position**
(OWNER/OBJECT_NAME/PACKAGE_NAME/ARGUMENT_NAME/OVERLOAD). On **12c** the nested field is a real walked
`ALL_ARGUMENTS` row, so a `theRecords` entry exists for that position and the match succeeds. On
**23ai** the nested field is SYNTHESISED late (during record-building, AFTER extra-object discovery),
so no positional `theRecords` entry exists → no match → the engine falls back to `Object`. The nested
class `FixturePkgPersonRec` IS built (discovered via `record_echo`), but keyed to a different position.

**To finish, one of (both high-risk, they touch record-matching = every generated program):**
- (a) Match the nested field to its class by **TYPE identity** (TYPE_OWNER/TYPE_NAME/TYPE_SUBNAME →
  a `theRecords` entry of the same PL/SQL type, honouring the existing `replacedByArrayId` dedup),
  as a fallback when the positional match misses. Semantically sound (same type = same class) and
  fairly localized, but changes the resolution used for ALL records.
- (b) Create a `theRecords`/extra-object entry for each synthesised nested-record field during
  discovery so the positional match works uniformly. More faithful to 12c but reorders
  discovery↔synthesis.

**And still separately needed:** the **12c flattening fix** (Phase 0) — on versions where the nested
rows ARE returned, the parent walk must SKIP the deeper `DATA_LEVEL` rows instead of flattening them.

**Verdict:** Item 7 is a genuine multi-part, high-blast-radius engine change (record-matching +
DATA_LEVEL walk + synthesis), not the "one synthesis site" the pre-Phase-0 plan hoped. Recommend
scheduling it as a dedicated effort with byte-diff proof on every canonical tree and full three-line
regression, rather than folding into a quick pass. The two synthesis changes above are correct
building blocks to restore when resuming.

## Phase 1 — fresh implementation run (2026-07-21, WIP on branch item7-nested-records-wip)

Went substantially further; the HARD problem (resolving a synthesized nested field to its generated
class) is SOLVED. Nested records now regenerate and COMPILE (180 classes). Four engine/dict sites
fixed (branch `item7-nested-records-wip`, commit cbc412a):

1. `SqlStatementDictionary.plsqlRecordFieldArgQrySelect` — a record-typed attribute now reports
   `DATA_TYPE='PL/SQL RECORD'` (via `all_plsql_types` typecode) so the engine classifies it.
2. `SAAdminWrangler` adopt-guard — accept a synthesized record whose non-scalar fields are records
   (still discard object/collection fields).
3. `CallableStatementParameterEngine` record matcher (~line 762) — **type-identity fallback**: when
   the positional `theRecords` match misses (which it always does for a synthesized nested field on
   23ai), match by `TYPE_OWNER/TYPE_NAME/TYPE_SUBNAME` against a record of the same PL/SQL type.
   Additive — only rescues cases that previously threw. **Verified:** `FixturePkgContactRec.paramPerson`
   is now typed `FixturePkgPersonRec` with real accessors, not `Object`.
4. `addBindCode` (~line 4052) — recurse with `qualifiedParentVariableName + variableName[i]` as the
   child's parent, so a nested record binds `outer.inner.field`. Byte-identical for a top-level record
   param. **Verified:** IN binds emit `paramPIn.paramPerson.paramName` at the right positions.

**Fixture:** `FIXTURE_PKG.contact_rec(label VARCHAR2, person person_rec)` + `nested_record_echo`.

**What still blocks a working round-trip — the parameter-numbering core (3 interdependent sites):**
- **`getProcCallStatement` PL/SQL block** flattens only ONE level: emits `p_in.person := ?` (binding
  the whole record — impossible for a PL/SQL record) instead of `p_in.person.name := ?;
  p_in.person.age := ?;`. Must recurse into nested records exactly like `addBindCode` now does.
- **`bindParams` OUT numbering** is off for nested: `p_out.label` got position 3, not 4, and the
  nested OUT positions collide (2,3). The `recordInParams/recordOutParams` / `paramInId/paramOutId`
  offset accounting counts a nested record as ONE param, not its recursive scalar total
  (`fieldCount` is the DIRECT field count, e.g. contact_rec=2, not the recursive 3).
- **OUT read/unload** (`getStatementResults`) almost certainly needs the same recursion (untested — the
  run failed earlier, at bind).
- Runtime symptom today: `nested_record_echo` → `Database error: Parameter 4 not set` (block has 4 `?`,
  Java binds 6 with wrong OUT numbers).

**Assessment:** nested records require threading recursion + a recursive scalar count through the
entire flatten-record machinery — record class, type resolution, bind path (all done) PLUS PL/SQL
block generation, IN/OUT parameter numbering, and OUT read. That is the "dedicated effort" this doc
predicted; the numbering rework is the highest-risk part (must stay byte-identical for the flat-record
path every generated program uses). Resume from branch `item7-nested-records-wip`: fix
`getProcCallStatement` to recurse, make the offset accounting use a RECURSIVE scalar count for a
nested record, mirror on the OUT read, then the full byte-diff + 3-box regression.
## Phase 2 — numbering DONE, nested records WORKING on 23ai (2026-07-21, branch item7-nested-records-wip)

All three remaining sites fixed; a nested record round-trips end-to-end via MCP on 23ai.

5. **PL/SQL block** (`getComplexStatementParamString`): the inline IN/OUT record-field loops were
   extracted into recursive emitters `emitRecordInAssigns` / `emitRecordOutAssigns` that flatten a
   nested PL/SQL RECORD field to leaf scalars (`p_in.person.name := ?`). `getComplexStatementRecordVariables`
   (boolean SIGNTYPE decls) recurses too, with path-derived names. Byte-identical for a flat record.
6. **Parameter numbering**: `recordInParams`/`recordOutParams` now use `recursiveLeafCount` (a
   nested record contributes its own leaf count) instead of the direct `fieldCount`. Equal to
   `fieldCount` for a flat record.
7. **Bind + read recursion** (`addBindCode`, `addGetResultsCode`): pass accumulated offsets
   (`inOffSet + paramInId[i] - 1`, `outOffSet + paramOutId[i] - 1`) and the full path
   (`qualifiedParentVariableName + variableName[i]`) into the child engine. Removed the "Multiple
   levels of recursion are not supported" throw on the OUT read (the `weHaveRecursed` flag still
   correctly gates the top-level-only demo / clearParameters code). Byte-identical for a single-level
   record (offsets 0, path no dots).

Helpers: `findRecordIdByType` (type-identity record lookup, honours `replacedByArrayId`),
`recursiveLeafCount`.

**Verified:** `FIXTURE_PKG.contact_rec { label VARCHAR2, person person_rec }` + `nested_record_echo`
round-trips `{paramLabel, paramPerson:{paramName, paramAge}}` via MCP. `TGen23aiMcp` nested-record
test PASS. **Multi-box regression, all green:** FREE23 23ai 281/0/2 (`-Pharnesses-23ai`), ORCL12 12c
277/0/2, ORCL21 21c 271/0/2 (`-Pharnesses-longids`). (`theLog`/`recordName` appear in the JSON but
that is a pre-existing RECORD_MAPPER quirk shared with `record_echo`, not new.)

## Phase 3 — 12c-native path DONE (2026-07-21)

The **12c-native path** — where `ALL_ARGUMENTS` returns the nested `DATA_LEVEL` rows (12c/18c/19c/21c)
— over-flattened the parent record CLASS (the `multirec` compile-fail from Phase 0), because the
parent's `getAttrArguments` result carries the whole subtree while the 23ai synthesis feeds a
direct-fields-only rowset. **Fix:** `SAAdminWrangler` now calls a new
`ReadOnlyRowSet.retainShallowestByIntColumn("DATA_LEVEL")` on a PL/SQL RECORD's argument rowset before
building its class — reducing it to the DIRECT fields (shallowest level); the nested-record field then
resolves to its own separately-built class and the engine's recursion (added in Phases 1–2) handles
block/bind/read. A no-op for a flat record (single level → byte-identical) and on 23ai (synthesis is
already one level).

**Verified:** `multirec` (`order_rec_t` with two `account_address_t` record fields) now
regenerates + COMPILES on ORCL12 (59 classes; `setParamPostalAddress(MultirecAccountAddressT)`
present, zero flattened `setParamPublicId`). Full multi-box regression still green: FREE23 281/0/2,
ORCL12 277/0/2, ORCL21 271/0/2 — the filter is byte-identical for the canonical (flat-record) trees.

Nested records now work on BOTH lines: 23ai via MCP (`nested_record_echo`) and 12c/18c/19c/21c-native
(record classes compile + the flatten path is gone). Item 7 complete. (A permanent `multirec`
canonical fixture would guard the 12c-native path against future regressions — not yet added; the
23ai `nested_record_echo` guards the synthesis path.)
