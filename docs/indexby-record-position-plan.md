# Index-by collections of records: densify the shadow array, carry `MCPDBWIZARD_POS`

**Status: ALL PHASES DONE (2026-09-02). Five boxes green; ORCL12 red only on a PRE-EXISTING
12c defect this work uncovered, now public known issue 11.** Fixes public known issue 5,
`ORA-06532: subscript outside of limit` when reading back an index-by table of records.

---

## 1. The defect, precisely

An index-by table cannot cross JDBC, so the generator wraps the call in an anonymous block with a
**shadow SQL nested table** and copies element by element. The out-direction copy, from the
checked-in demo output (`app/sql/Demo/Src/.../PackageIdxarrayExampleGetPlsqlArrayOfFlights.java:197`):

```plsql
IF p_flights_from.COUNT > 0 THEN                          -- 12
  P_FLIGHTS_FROM_T.EXTEND(p_flights_from.COUNT);          -- 13
  FOR i IN p_flights_from.FIRST..p_flights_from.LAST LOOP -- 14
   IF p_flights_from.EXISTS(i) THEN                       -- 15
    P_FLIGHTS_FROM_T(i) := OSOFT446NDD6_T                 -- 16   <-- raises
```

`EXTEND(COUNT)` sizes the shadow array `1..COUNT`; the assignment then indexes it with **the
index-by's own key**. An index-by table's keys are arbitrary `BINARY_INTEGER`s. A nested table's
are `1..COUNT`. The code treats them as the same number.

The demo package fills base-0 (`mcpdbwizard_demo_ddl.sql:611`):

```plsql
p_flights_from(p_flights_from.COUNT) := l_flight;   -- first row lands at subscript 0
```

so the first assignment is `T(0)`, outside a nested table's legal range. `ORA-06512: at line 16`
in the field report is that line — the block's own comment numbering.

**Why the write half works.** Inbound, the same pattern runs the other way
(`PackageIdxarrayExampleAddBookingsPlsqlArray.java:287`): the dense subscript goes *into* the
index-by, which accepts any subscript. Fatal in one direction, invisible in the other. That is the
whole of "it is the outbound binding specifically".

**When it bites.** Dense-from-1 works. Base-0 and negative keys give `ORA-06532`; sparse keys give
`ORA-06533` (`EXTEND(3)` then `T(5)`). It cannot answer wrongly in silence: sparse keys always imply
a maximum key above `COUNT`, so it always raises.

**Why the suite never caught it.** The only fixture with an index-by of records is
`iba_rowtype_api_pub`, which fills with `select * bulk collect into z_unallocated_material`
(`app/sql/iba_record.sql:173`) — dense from 1, the one shape the block handles. The estate has been
green for years because the fixture is subscript-friendly by construction and real PL/SQL is not.

**The nested-table path has the same defect, less reachably.** The `TABLE`/`VARRAY` branch
(`CallableStatementParameterEngine.java:7541`) emits the identical `EXTEND(COUNT)` + `FIRST..LAST` +
`_T(i)` shape and has **no `EXISTS` guard at all**. A nested table that has had `DELETE(n)` applied
is sparse, so it fails the same way — and fails on the read side too. Fix both in one pass.

---

## 2. The finding that shapes the design: shadow types are shared by SHAPE, not by parameter

`ExtraType` builds the type name by appending a per-column shape code
(`ExtraTypeSizeWrangler.appendName`, `JavaUtils.mapOracleDatatypeToAlphaChar`), so two collections of
the same record shape get **the same type**. Measured on the demo output:

| Wrapper | Collection kind | Shadow type |
|---|---|---|
| `PackageIdxarrayExampleFlightsPlsqlArray` | index-by | `OSOFT446NDD6_T` |
| `PackageArrayExampleFlightsPlsqlArray` | nested table | `OSOFT446NDD6_T` |
| `PackageIdxarrayExampleBookingsPlsqlArray` | index-by | `OSOFT46ND2_T` |
| `PackageArrayExampleBookingsPlsqlArray` | nested table | `OSOFT46ND2_T` |

and the element class is shared with them: `Flights.java` is used by **both** flights collections and
hardcodes `recordName = "OSOFT446NDD6_T"`.

**Consequence, and it is the sharpest trap in this change.** Adding `MCPDBWIZARD_POS` to only the
index-by's type would change a type the nested-table path also constructs — wrong constructor arity,
`ORA-06550` at run time on a path that is not the one being fixed. Avoiding that by forking the name
is worse: the fork forces a second element class per record shape, because one class names one type.

**So: `MCPDBWIZARD_POS` goes on EVERY record-collection shadow type, index-by and nested table
alike.** One code path, no name fork, no duplicate classes, no collision — and the attribute is
meaningful in both cases, because a nested table that has had elements deleted has a real subscript
worth carrying. The name is left out of the shape code deliberately: every shape gains the attribute
uniformly, so nothing can collide with anything.

---

## 3. Phases

### Phase 0 — reproduce, so "fixed" is measurable — **DONE**
Reproduced on ORCL19 with a self-contained block (no fixture, no DDL), mirroring the emitted pattern:

| Index-by keys | Emitted pattern | Densified pattern |
|---|---|---|
| base-0 (`0..2`), filled `t(t.COUNT)` as the demo does | **ORA-06532** | OK, 3 rows |
| sparse 1,5,9 | **ORA-06533** | OK, 3 rows |
| negative -2,-1 | **ORA-06532** | OK, 2 rows |
| dense 1..3 | OK | OK |

The last row is why the suite has been green: `BULK COLLECT` only ever produces that shape.

*(original phase text)*
Call `package_idxarray_example.get_plsql_array_of_flights` on the demo schema and record the
`ORA-06532`. Call its three siblings (PL/SQL table, object array, nested table) in the same session
for the control. Then `DELETE(n)` on a nested-table fixture and confirm the second, unreported
instance of the same bug.

### Phase 1 — densify (the defect fix; no surface change) — **DONE**
- `ExtraType.java:103` — the unassign template hardcodes the destination subscript:
  `PARAM_TARGET_PARAM_ARRAY_NAME + "(i) := " + ...`. Replace the literal `(i)` with a new
  `PARAM_TARGET_PARAM_ARRAY_INDEX` token so the caller decides the destination subscript.
- `CallableStatementParameterEngine.java:7586-7601` (index-by branch) — declare a per-parameter
  counter beside the existing `_T` declaration at `:7050-7075`, using
  `n.createName(argName + "_I")` so it obeys the same length rules; keep `EXTEND(COUNT)`, increment
  inside the `EXISTS` guard, and substitute the counter for the token.
- `CallableStatementParameterEngine.java:7541-7573` (nested table / varray branch) — same counter,
  **plus** the `EXISTS` guard this branch has never had.

**What was actually done, which is simpler than planned: no new variable at all.** Rather than a
declared counter, the emitters now `EXTEND` one element per copied row and assign at the shadow
array's own `.LAST`. That needs no addition to the block's DECLARE section, so
`PARAM_TARGET_PARAM_ARRAY_INDEX` was never needed either — `ExtraType.java:103` simply names the
array twice, and both occurrences are substituted by the existing `replaceString` call.

Emitted shape now:

```plsql
IF x.COUNT > 0 THEN
  FOR i IN x.FIRST..x.LAST LOOP
   IF x.EXISTS(i) THEN
    X_T.EXTEND;
    X_T(X_T.LAST) := <type>(x(i).FIELD, ...);
   END IF;
  END LOOP;
END IF;
```

**Verified** by regenerating `generic_teste` (the config carrying `iba_rowtype_api_pub`) against a
live box: 60 copy-out sites converted, **zero** of the old `_T(i) :=` shape and zero bulk
`EXTEND(COUNT)` left in the tree, it compiles, and the file count is **204 — exactly the recorded
floor**, confirming this changes text and not structure.

After Phase 1 the `ORA-06532` is gone for base-0, negative and sparse keys, and the dense case is
byte-identical. Keys are still not preserved — that is Phase 2.

### Phase 2 — carry the subscript as `MCPDBWIZARD_POS` — **DONE**
- `ExtraType` — append `,MCPDBWIZARD_POS NUMBER` as the final attribute of every record-collection
  type (mind the terminator: `createStatement` closes with `");"` on the last column, so the close
  moves). Extend the unassign constructor with the source subscript. Leave the shape-derived name
  alone.
- Inbound (`assignStatement`) — key the index-by by the carried position rather than the dense
  index: `NVL(<array>(i).MCPDBWIZARD_POS, i)`. **The `NVL` is load-bearing:** a caller who builds a
  fresh array rather than round-tripping one leaves the attribute null, and `t(NULL)` is
  `ORA-06502`.
- Element class — emit the field, its accessor, and its slot in the generated `getCurrentValues()` /
  `setNewValues()` so the STRUCT arity matches. **Scoped 2026-09-02 and it is the risky half:** that
  emitter (`CallableStatementParameterEngine.java:2595` / `:2720`) builds `Object[theRowSet.size()]`
  from the record's own fields and is used for **every** record class, including records that are
  plain parameters and have no shadow type at all — the demo's `Customers` is decomposed into
  per-field binds and appears in no `extraObjects.sql`. So the field must be added only where the
  class actually backs a shadow type (the classes that carry a `recordName`), or every record class
  gains a meaningless field and the ones with types gain an arity mismatch that shows up at bind
  time, not compile time. Establish which side of that line each emitted class falls on **before**
  writing the change. Oracle attribute `MCPDBWIZARD_POS`; Java accessor
  `getMcpdbwizardPos()`; the MCP/JSON key follows the existing (defective) Java-field-name rule, so
  it will read `mcpdbwizardPos` until known issue "record crossing" is fixed.

### Phase 3 — fixtures that would have caught it — **DONE**
`app/sql/idxby_keys.sql` — table, package and **its shadow SQL types** — loaded into
`GENERIC_TESTE` by `Scripts/testdata.sh`, with the 7 routines added to `generic_teste.pb2` and the
`.json` sibling regenerated by `ConfigConverter` (50 pure insertions, no reordering). Generation
goes 204 -> **215** files and `expected-file-counts.txt` is raised to match, measured on a
truncating box. Shapes verified live:

| Procedure | COUNT | FIRST | LAST |
|---|---|---|---|
| `get_base0` | 3 | 0 | 2 |
| `get_sparse` | 3 | 1 | 9 |
| `get_negative` | 3 | -3 | -1 |
| `get_dense` (control) | 3 | 1 | 3 |
| `both_ways` (IN OUT) | 1 | 0 | 0 |
| `get_nt_hole` (nested table) | 2 | 1 | 3, `EXISTS(2)` false |

`TIndexByRecordKeys` passes on ORCL19. It checks ids and labels, not just row counts — a densify
that dropped or duplicated a row would still return three of something — and it records failures
rather than returning at the first, so one run reports every broken shape.

**The fixture creates its own shadow types, and that is DELIBERATE placement.** A
collection-of-records binds through generated SQL types, and the only caller of
`createExtraTypeObjects()` anywhere is the emitted MCP server at startup; `generic_teste` has no
`MCP_SERVER`, so nothing creates them for it. The types on the estate today were made **by hand**
(`CREATED` 2026-08-06 and 2026-08-13, generator work days, not provisioning runs), so a box built
purely from `Scripts/testdata.sh` has never had them. Running that DDL is a **deployment** step, not
the product's job and not a harness's — hence it lives with the rest of the fixture's schema. An
earlier revision had the harness call `createExtraTypeObjects()` itself; that was wrong and was
removed.

**Two traps found doing this.** (1) `extraObjects.sql` cannot simply be fed to SQL*Plus — it buffers
the `CREATE TYPE`s and disconnects without executing them, because the generated file carries no `/`
terminators. (2) `REM` is a SQL*Plus directive, not a PL/SQL comment: inside a package it reaches
the compiler and the package comes out INVALID.

Still to add here when Phase 2 lands: assert `MCPDBWIZARD_POS` round-trips.

### Phase 4 — db-free guard — **DONE**
`ExtraTypeSubscriptTest` builds an `ExtraType` entirely in memory — `ReadOnlyRowSet` has an
`Object[]` constructor, so a three-field record needs no database — and pins four things: the shadow
array is indexed by its own `.LAST` and **never** by `(i)`; the SOURCE side still reads `(i)` (a
rename of both sides would satisfy the first assertion and read nothing); one constructor argument
per field in order, which is what keeps the shadow type's arity matching its `CREATE`; and the
INBOUND half still uses `(i)` on both sides, which is correct there and must not be "fixed" for
symmetry.

Verified non-vacuous by reintroducing the defect in `ExtraType` and re-running: `mvn` exits 1 and
`unassignIndexesTheShadowArrayByItsOwnLast` fails. The emitter was then restored and `git diff`
against HEAD confirmed empty.

**Two faults in the test itself, found by running it, both worth keeping.** (1) It first used
`ConsoleLog`, which on an error prints `Press Enter to continue...` and **blocks on stdin** — in a
Surefire fork that hangs the build to the timeout instead of failing it. `JulLog` is the
non-interactive choice, as `InlineSqlWriteTest` already uses. (2) `ExtraTypeSizeWrangler` reads
`DATA_LENGTH` through `getIntegerObj`, which unwraps a `BigDecimal`; a `Long` in the fixture rowset
gives a `ClassCastException` rather than anything readable.

**And a measurement trap:** the first run of this test reported `exited with code 0` while actually
wedged at that prompt — the status of `head` at the end of a pipe, not Maven's. Capture to a file
and read Maven's own line.

### Phase 5 — estate, docs, release — **DONE**
Six boxes. Strike known issue 5. Release note carrying the migration below.

---

## 4. Migration, and it is not optional

Every record-collection shadow type gains an attribute, so **`extraObjects.sql` must be re-run after
regenerating**. Generated Java from before this change constructs the old arity and will fail against
the new type, and vice versa — they must move together. `ORA-02303` ("cannot replace a type with
dependents") is expected where the old types have dependents; the drop statements
(`getDropTypeStatement` / `getDropArrayStatement`) already exist and must be ordered array-then-type.

---

## 5. Risks

1. **The shared-shape trap above** — the reason `MCPDBWIZARD_POS` is unconditional. Any future
   attempt to make it index-by-only reopens it.
2. **`NVL` on the inbound key** — without it, first-time callers get `ORA-06502` instead of the
   `ORA-06532` we just fixed.
3. **Regenerate and reload together** — a half-migrated schema fails at bind time with an arity
   error naming neither cause.
4. **File counts do not move** (no new files), so `expected-file-counts.txt` cannot detect a
   regression here. The Phase 4 emitter test is the signal; the floor is not.


---

## 6. What Phase 2 actually cost, and the finding that nearly sank it

**The gate `generatedGenericTypeName != null` is NOT "which type is this class binding".** It asks
whether a generated type exists for the record; the question that matters is which type the instance
is bound to *right now*, and that is not known until run time. One element class binds **two**
Oracle types: `TypeArrayCommands` defaults to the shadow `OSOFT7990ND99_T` (9 attributes), but a
collection of a real user-declared type reassigns it —

    newValues[i].recordName = childRecordName;   // the owner's own type, 8 attributes

— so a fixed `new Object[9]` satisfies one use and fails the other with **ORA-17049 "Inconsistent
Java and SQL object types"**, which names neither the type nor the arity. Every generated pair
agreed at 9; the mismatch was against the *customer's own* type.

The fix is for the arity to follow `recordName` per call, not per class:

```java
boolean mcpdbwizardWithPos = "OSOFT7990ND99_T".equals(recordName);
Object[] tempObjectArray = new Object[mcpdbwizardWithPos ? 9 : 8];
```

with `setNewValues` accepting either width. A real user type never gains the attribute, which is
right independently of the bug: we have no business altering a type the customer declared.

**Four traps in the migration, each of which cost a run.**
1. **Password from `PASS=`, never `USER=`.** Oracle passwords are case sensitive, so a schema's
   own name used as its password is a *wrong* password; repeated across several propfiles it
   **locked the account**. The logs then show only ORA-28000, never the ORA-01017 behind it.
2. **Probe once, then skip.** A retry loop against a wrong credential is what turns a typo into a
   locked schema.
3. **Drop by the type names the files DECLARE, not by prefix.** `DEFAULT_TEMP_PREFIX` only defaults
   to `OSOFT`; `OB` is in use. A prefix match left those undropped, their re-create hit ORA-02303,
   and the stale type met new Java as ORA-17049.
4. **Dedupe across a schema's trees, and add `/` terminators.** Several propfiles share a schema and
   emit the same shape, so replaying files in turn re-replaces a `_T` whose `_A` already exists
   (ORA-02303 inbound). And `extraObjects.sql` carries no terminators at all, so SQL*Plus buffers
   every `CREATE TYPE` and executes **nothing**, silently.

`Scripts/refresh_extra_types.sh` encodes all four; `estate.sh` runs it between regen and test.


---

## 7. Estate result, 2026-09-02

| Box | app | web | Result |
|---|---|---|---|
| ORCL19 | 995 / 0 | 487 / 0 | green |
| FREE26 | 1008 / 0 | 487 / 0 | green |
| FREE23 | 1008 / 0 | 487 / 0 | green |
| XE18 | 995 / 0 | 487 / 0 | green |
| ORCL21 | 995 / 1 -> **0 on re-run** | 487 / 0 | green (the failure was `ORA-17008`, a dropped connection) |
| ORCL12 | 1001 / 1 | - | red, see below |

`regen exit=0` and `types exit=0` on all six, so the shadow-type refresh is proven on both Oracle
lines rather than only on the box it was written against.

**ORCL12's failure is NOT this work, and that was measured rather than argued.** `TIndexByRecordKeys`
fails there with *"Attempt to set a non-existent parameter number 4; legal range is '1' to '2'"* for
the two procedures that take a record collection IN. Checking out `f957d53`'s three generator files
-- the fixture exists at that commit, Phase 2 does not -- rebuilding and regenerating against the
same ORCL12 produced **identical** numbers (binds=2, index=4), and the same propfile on ORCL19 gives
binds=2, index=2. So it is the 12c dictionary shape: `ALL_ARGUMENTS` expands a record parameter into
child rows and the bind counter walks over them. Filed as public known issue 11, with the workaround
(split the routine so the collection travels one way per call).

**A first estate attempt aborted five boxes in ten seconds each** because `IDXBY_KEYS` had only been
loaded on ORCL19. The preflight caught it and refused to run the tests, which is what stops
`TGen23ai`'s teardown from dropping more fixtures than were missing. Worth keeping as evidence that
the check earns its place.
