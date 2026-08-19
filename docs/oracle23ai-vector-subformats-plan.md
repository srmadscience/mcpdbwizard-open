# Oracle 23ai VECTOR sub-formats — scoping & implementation plan

Plan to extend the generator + `com.mcpdbwizard.pub` runtime beyond **dense
FLOAT32/FLOAT64/INT8** vectors to the other `VECTOR` storage formats: **BINARY**
and **SPARSE** (and to make flexible-dimension `VECTOR(*,*)` explicitly covered).
Scoped 2026-07-13 against the FREE26 box (Oracle 26ai Free 23.26.0.0.0).

## Current state

`VECTOR` columns/params are classified once, by `DATA_TYPE`, and read/written/
generated uniformly as `double[]`:

- **Classify** — `SqlUtils.getUnderlyingOracleDatatype` maps `DATA_TYPE == "VECTOR"`
  / `startsWith("VECTOR")` → `ORACLE_VECTOR_DATATYPE` (=30).
- **Read** — `ReadOnlyRowSet.unloadObject` reads `getObject(i, double[].class)`;
  `getVector()` returns `double[]` (with `float[]`/`byte[]` → `double[]` fallbacks);
  `getString` renders via `Arrays.toString(double[])`.
- **Write** — `WriteableRowSet.setVector(double[])`; `StatementParameters2` binds
  `setObject(i, v, OracleType.VECTOR)` for a `double[]`; `CallableStatementParameters`
  reads OUT params as `double[].class`.
- **Generate** — `DatatypeWrangler.getOracletypeCode` → `OracleTypes.VECTOR`;
  `JavaUtils.oracle2JavaDatatype` → `double[]`; `CallableStatementParameterEngine`
  has a VECTOR variable arm + SOAP scalar pass-through; `SqlStatementWrangler`
  carries a `VECTOR` `ASP_DATA_TYPES` token + comment-hint synonym.

This is **correct for dense FLOAT32/FLOAT64/INT8** (and, in practice, flexible-dim
dense — a variable-length `double[]`). It is **wrong for BINARY** (bit-packed → the
natural Java type is `byte[]`, `n` bits = `n/8` bytes) and **for SPARSE** (index/value
pairs, not a dense array).

## Findings that shape the plan (live, FREE26, 2026-07-13)

1. **Classification is feasible but needs a new introspection read.** `DATA_TYPE` is
   just `"VECTOR"` for *every* format (dense/binary/sparse/flexible all identical,
   `DATA_TYPE_MOD` null, `DATA_LENGTH` 8200), so the current classify-by-type-string
   **cannot** tell them apart. **`USER_TAB_COLUMNS.VECTOR_INFO` exposes the full
   format**, e.g.:

   | column DDL | `VECTOR_INFO` |
   |---|---|
   | `VECTOR(3,FLOAT32)` | `VECTOR(3,FLOAT32,DENSE)` |
   | `VECTOR(2,FLOAT64)` | `VECTOR(2,FLOAT64,DENSE)` |
   | `VECTOR(5,INT8)` | `VECTOR(5,INT8,DENSE)` |
   | `VECTOR(*,*)` | `VECTOR(*,*,DENSE)` |
   | `VECTOR(16,BINARY)` | `VECTOR(16,BINARY,DENSE)` |
   | `VECTOR(4,FLOAT32,SPARSE)` | `VECTOR(4,FLOAT32,SPARSE)` |

   So for **table columns** the generator *can* route by format — but only via a new
   step that reads `VECTOR_INFO` (grammar `VECTOR(<dim>,<format>,<storage>)`, `dim`
   may be `*`). It does not read it today.

2. **FLOAT16 is out of scope** — unsupported on 23.26 Free (`ORA-51802: VECTOR column
   type specification has an unsupported dimension format (FLOAT16)`); the column
   cannot even be created.

3. **Flexible-dim dense `VECTOR(*,*)` almost certainly already works** — it is just a
   variable-length `double[]`. Verify, don't build.

4. **The real work is BINARY and SPARSE**, where `double[]` is the wrong
   representation.

5. **Priority caveat (honest).** No current test or real schema uses binary/sparse.
   Today the generator emits *compiling-but-wrong-at-runtime* `double[]` code for such
   columns — a **latent-correctness / forward-looking** gap, not a live break (same
   category as the nested-field-record recursion that was closed as unreachable). Weigh
   accordingly before committing effort.

## Plan

### Investigation gate — RESULTS (completed 2026-07-13, live on FREE26)

The gate ran against FREE26 (ojdbc11 `23.7.0.25.01`). Outcomes materially narrow the
scope — **SPARSE is blocked by the driver, and binary is table-columns-only:**

- **I1 — driver representation.**
  - **Dense FLOAT32 / FLOAT64 / INT8 and flexible `VECTOR(*,*)`** all read back as
    `double[]` correctly (`int8 [1,2,3,4,5]`, `float64 [1.5,2.5]`, `(*,*) [7,8,9]`) —
    the existing path already handles them. ✅ (Increment 0 essentially confirmed.)
  - **BINARY** — `getObject(i, double[].class)` **fails** with
    `ORA-17004: Conversions of VECTOR_BINARY to double[] are not supported`; the current
    generated `double[]` path is therefore a **latent runtime bug** for a binary column.
    `getObject(i, byte[].class)` **works** (`VECTOR(16,BINARY)` value `[3,200]` →
    `byte[]{3,-56}`). Representation = **`byte[]`**; bind via `OracleType.VECTOR_BINARY`
    (present) / `oracle.sql.VECTOR.ofBinaryValues`. **Feasible.**
  - **SPARSE** — **BLOCKED at the driver level.** A `VECTOR(6,FLOAT32,SPARSE)` value
    inserts fine but is **unreadable** by ojdbc11 23.7: `double[]`, `float[]`, **and**
    `oracle.sql.VECTOR` all throw `ORA-17004: Invalid column type`. There is **no
    `OracleType.VECTOR_SPARSE`** field and **no sparse accessor** on `oracle.sql.VECTOR`
    (methods are dense/binary only: `toDoubleArray`/`toFloatArray`/`toByteArray`/
    `toBooleanArray`/`ofBinaryValues`/…). Sparse support needs a newer driver (or a
    server-side densify/serialize workaround) — **deferred, not attempted.**
- **I2 — proc-parameter metadata.** `ALL_ARGUMENTS` has **no** vector-format column
  (nothing matching `%VEC%`), i.e. no `VECTOR_INFO` equivalent for arguments. So a VECTOR
  **proc parameter's** format is invisible to introspection → binary/sparse support is
  **table-columns-only**; proc params stay dense-`double[]` (a documented boundary).
- **I3 — VECTOR_INFO parse contract.** Confirmed grammar `VECTOR(<dim>,<format>,<storage>)`
  with `dim` possibly `*`, `format` ∈ {FLOAT32,FLOAT64,INT8,BINARY}, `storage` ∈
  {DENSE,SPARSE}. (FLOAT16 rejected at DDL time on 23.26 — `ORA-51802`.)

**Net effect on the plan below:** Increment 0 is a verify-only formality; Increment 1
(VECTOR_INFO introspection) proceeds for table columns; **Increment 2 (BINARY) is
table-columns-only**; **Increment 3 (SPARSE) is removed from active scope** pending an
ojdbc upgrade.

### Increment 0 — flexible-dim / INT8 / FLOAT64 dense (verify, ~no code)

Add `VECTOR(*,*)`, `VECTOR(n,INT8)`, `VECTOR(n,FLOAT64)` columns to
`sql/datatypes_23ai.sql` and a `Datatypes23aiLiveTest` case; confirm they round-trip
through the existing `double[]` path. Likely green as-is.

### Increment 1 — introspect `VECTOR_INFO` + sub-classify (the enabler) — DONE (2026-07-13)

The structural core, dense-neutral. Delivered:
- New pub constants `ORACLE_VECTOR_BINARY_DATATYPE` (31) / `ORACLE_VECTOR_SPARSE_DATATYPE`
  (32) plus the pure classifier `SqlUtils.getVectorDatatypeFromInfo(String)` (SPARSE→32,
  BINARY→31, else/null→dense 30), with 10 `SqlUtilsTest` cases.
- `SqlStatementDictionary.getAllTabColsQry(version, withVectorInfo)` overload selects
  `VECTOR_INFO` only when the caller opts in; `SAAdminWrangler.isVectorInfoAvailable()`
  feature-detects the column (cached; robust to the unreliable 23ai version string) so
  the query is byte-identical on pre-23ai (no `ORA-00904`).
- The table-column loop in `generateTables` (~3258) now detects a BINARY/SPARSE VECTOR
  column via `VECTOR_INFO` and skips the table through the existing `broken[i]` path
  (same mechanism as the `Object`/`UNDEFINED` skips) rather than emitting the dense
  `double[]` code that fails at runtime. **Dense/flexible VECTOR is untouched.**

**Verified:** `SqlUtilsTest` 64/0; all 7 propfile file counts byte-identical on FREE26;
full `-Pharnesses-23ai` suite **277/0/2** (267 baseline + 10 new unit tests); and a live
proof — a `VECTOR(16,BINARY)` column on a `generic_test1` table is skipped
("`is a BINARY VECTOR (VECTOR(16,BINARY,DENSE)) - not yet supported, Table being ignored`").

### Increment 2 — BINARY → `byte[]` (table columns only — confirmed by I1/I2)

**Runtime-metadata gate (2026-07-13, live on FREE26).** At *generation* time the format
comes from `VECTOR_INFO` (Increment 1). At *runtime* the `pub` library classifies a column
from JDBC metadata — and the probe showed the format IS recoverable there too:
`ResultSetMetaData.getColumnTypeName()` returns `"VECTOR"` for every format (no help), **but
`getColumnType()` returns format-specific codes** — dense → `OracleTypes.VECTOR_FLOAT32/64/INT8`,
binary → **`OracleTypes.VECTOR_BINARY` (-109)**. `ReadOnlyRowSet` already captures
`getColumnType()` into `columnJavaDatatypes[i]` (right beside the name-based classification), so
a binary column is distinguished with a one-line refinement — no baked type info needed.


Representation is `byte[]`, bound via `OracleType.VECTOR_BINARY`. Full surface (touch
points enumerated in "Current state"): `SqlUtils` classify (from `VECTOR_INFO`) →
`ReadOnlyRowSet.unloadObject` reads `byte[].class` / new `getVectorBinary` / `getString`
→ `WriteableRowSet.setVectorBinary` → `StatementParameters2` bind (`VECTOR_BINARY`) +
`CallableStatementParameters` OUT → generator `JavaUtils` (`byte[]`), `DatatypeWrangler`,
`CallableStatementParameterEngine` arm, `SqlStatementWrangler` token. Plus DDL + live
test. **Scope note (I2) — SUPERSEDED, see the status block below.** As written: only table
columns carry format metadata, so a binary VECTOR *proc parameter* cannot be told from a
dense one, stays on the dense `double[]` path, "and would mis-generate". That limitation
was removed rather than documented: PL/SQL forbids a format-constrained parameter, so the
format travels with the VALUE, and every vector param now gets all three setter overloads
with an `instanceof` dispatch on the OUT side. Nothing mis-generates.

**Status: COMPLETE (2026-07-21). Binary AND sparse generation both shipped.** The line below
said "generator emission (2c) remaining" for three weeks after 2c landed, which made finished
work read as an open task — verify against the code before quoting any status in this directory.
What is genuinely left is **FLOAT16 only**, and it is server-blocked (`ORA-51802` on 26ai Free
23.26 — the release has no FLOAT16 format), so no driver or generator change can reach it.

Beyond the original 2c scope, **sparse** columns generate too: the `VECTOR_SPARSE` token threads
exactly like `VECTOR_BINARY` (→ `JavaUtils` `com.mcpdbwizard.pub.SparseVector`, `DatatypeWrangler`
`OracleTypes.VECTOR`, comment-writer token, INSERT/UPDATE via `setVectorSparseParam`), and both
former skip guards were removed. Over MCP a sparse *table column* crosses as a
`{length, indices, values}` JSON object; a sparse *proc param* does not (I4, scoped out — a VECTOR
proc tool is dense-array only). `app/CLAUDE.md` carries the current account.

- **2a — read (DONE, commit `49437a3`).** `ReadOnlyRowSet` distinguishes binary via
  `getColumnType()==VECTOR_BINARY` and reads `byte[]`; new `getVectorBinary(...)`. Live
  test `readsBinaryVectorColumn`.
- **2b — write (DONE, commit `c0e1f80`).** `StatementParameters2.setVectorBinaryParam`
  (byte[] bound as `OracleType.VECTOR_BINARY`, disambiguated from RAW); `WriteableRowSet.setVectorBinary`;
  `CallableStatementParameters` OUT. Live write-read round-trip test. Full pub suite 211/0/0.
- **2c — generator table-DAO emission (DONE 2026-07-21).** Make a binary-vector *table* generate a
  working DAO instead of being skipped (the Increment-1 skip narrows to sparse-only). Surface:
  narrow the skip; classify binary in the **table-generation** introspection (add `VECTOR_INFO`
  to `getAllTabColsCSEQry`, refine in the engine's `setComplexFlag`); `JavaUtils.oracle2JavaDatatype(31)`
  → `byte[]`; route the INSERT/UPDATE bind to `setVectorBinaryParam` / `OracleType.VECTOR_BINARY`
  (the SELECT read already works via 2a); and audit every table-path switch that handles dense
  `VECTOR` (30) to also handle binary (31). Proc params stay dense (I2). Verify: full FREE26 regen +
  `-Pharnesses-23ai` green + a binary-column table that generates and round-trips.

  **DONE (2026-07-13).** Binary-vector tables now generate a working DAO (read + write) on
  FREE26. The introspection rewrites a binary column's `DATA_TYPE` to a `VECTOR_BINARY` token
  (shared `vectorFormatDataType` `CASE` on `VECTOR_INFO` in both `getAllTabColsQry`/`...CSEQry`);
  `SqlUtils`/`JavaUtils`(→`byte[]`)/`DatatypeWrangler`(→`OracleTypes.VECTOR_BINARY`) recognise it;
  the skip narrows to sparse-only; `SqlStatementWrangler.ASP_DATA_TYPES` += `VECTOR_BINARY`; the
  engine got binary arms in the variable-type switch, the SOAP pass-through, and the IN-bind chain
  (emitting `setVectorBinaryParam`). **The keystone fix:** the generated INSERT/UPDATE SQL *comment*
  (which seeds the ASP token) is written from the Java type, and `byte[]` is ambiguous with RAW — so
  the comment writer (`SAAdminWrangler` ~3715) now emits the `VECTOR_BINARY` token for a binary column
  instead of `byte[]`, which makes the whole ASP→bind chain resolve to `setVectorBinaryParam`.
  **Verified:** full FREE26 regen (dense counts byte-identical) + `-Pharnesses-23ai` green; `TGen23ai`
  reads a `VECTOR(16,BINARY)` column back as `byte[]{3,-56}` through the generated DAO (fixture column
  added to `sql/datatypes_23ai_gen.sql`). The generated write bind (`setVectorBinaryParam`, emitted by
  both Ins and Upd) is covered by `Datatypes23aiLiveTest.writesBinaryVectorColumnViaBoundParam`; it is
  not round-tripped via the generated `rowUpdate` because FIXTURE_TABLE's IDENTITY PK makes any full-row
  update fail `ORA-32796` (pre-existing identity limitation, unrelated to vectors). Sparse stays
  skipped; proc params stay dense (I2).

  **Earlier attempt 2026-07-13 (reverted, superseded by the above).** Got a binary-vector table to
  **generate + compile + read as `byte[]`** on FREE26 via: introspection rewriting a binary
  column's `DATA_TYPE` to a synthetic **`VECTOR_BINARY`** token (a `CASE` on `VECTOR_INFO` in
  both `getAllTabColsQry`/`getAllTabColsCSEQry`, using a shared `vectorFormatDataType` expr);
  `SqlUtils.getUnderlyingOracleDatatype` recognising `VECTOR_BINARY`→31 / `VECTOR_SPARSE`→32;
  narrowing the skip to sparse-only; `JavaUtils`(31)→`byte[]`, (32)→`Object`(skip);
  `DatatypeWrangler` `VECTOR_BINARY`→`OracleTypes.VECTOR_BINARY`; `SqlStatementWrangler`
  `ASP_DATA_TYPES` += `VECTOR_BINARY`; and the engine's **variable-type switch** (~462) +
  **SOAP scalar pass-through** (~9626) getting binary arms (fixes the first crash,
  `PARAM_PROD_NAME does not support data type` / STATEMENT ENGINE INIT NPE).
  **The remaining gap (why it was reverted):** the generated table INSERT/UPDATE manager still
  binds the binary column with plain `theParameters.setParam(idx, byte[])`, which resolves to the
  RAW `setParam(int,byte[])` overload — **not** `setVectorBinaryParam`. The bind is emitted by
  `CallableStatementParameterEngine`'s IN-bind chain (~3898–4444, *many* emission points); a
  `VECTOR_BINARY` branch added at the top of the typed sub-chain (3898) did **not** fire because
  the binary column takes the **scalar-default** emission path, which is elsewhere in that chain.
  Next step: locate the scalar-default `setParam(idx, var)` emission (the one dense VECTOR uses)
  and, when `oracleParamDatatype[i]=="OracleTypes.VECTOR_BINARY"` (already set from `DatatypeWrangler`),
  emit `setVectorBinaryParam` instead; then round-trip a binary-column DAO (write+read) and run the
  full `-Pharnesses-23ai` suite. Reverted because a RAW write to a VECTOR column is wrong at runtime
  and must not ship. (`pub` `setVectorBinaryParam` already exists from 2b, ready for the emitter.)

### Increment 3 — SPARSE — UNBLOCKED (ojdbc11 23.26.2.0.0), implementation still TODO

**Update 2026-07-21: the driver is no longer the blocker.** The project upgraded ojdbc11
23.7.0.25.01 → **23.26.2.0.0**, which DOES read and write sparse vectors as typed values:
`oracle.sql.VECTOR.isSparse()` / `.toSparseDoubleArray()` (parent `SparseArray` gives
`length()` + `int[] indices()`; `SparseDoubleArray.values()` gives the `double[]`), and
`VECTOR.ofFloat32Values(VECTOR.SparseFloatArray.of(len, int[] idx, float[] vals))` to construct
one for binding. Verified LIVE on FREE23 — read a `VECTOR(25, FLOAT32, SPARSE)` back as
`{length:25, indices:[3,10], values:[1.5,-2.0]}` and round-tripped a constructed sparse vector
through `echo_sparse(p_in IN VECTOR, p_out OUT VECTOR)`. (There is still no `VECTOR_SPARSE`
`OracleType` constant — sparse binds through the generic VECTOR type carrying a `SparseArray`
value.)

**Original I1 finding (ojdbc11 23.7):** a `SPARSE` vector was unreadable (`ORA-17004` for every
Java type; no `VECTOR_SPARSE` OracleType; no sparse accessor). That is fixed by the driver bump.

**Still TODO to support it here:** design the representation — a small `SparseVector` class in
`pub` (dims + `int[]` indices + `double[]` values), `ORACLE_VECTOR_SPARSE_DATATYPE` read/write in
`ReadOnlyRowSet`/`WriteableRowSet`/`StatementParameters2`, and mirror Increment 2's generator
surface — plus a `{length, indices, values}` JSON shape for the MCP layer. Feasible now; not yet
done. Until then a sparse column/param is still detected and **skipped**, not crossed.

## Regression safety & verification

Every new route is gated on `VECTOR_INFO` reporting a non-dense-float format, so the
dense path and all non-23ai servers are untouched. Gates at each increment:

- `generic_test1` file counts unchanged on 12c (2482), compiles clean.
- `-Pharnesses-23ai` stays **267/0/2** on FREE26/FREE23.
- New BINARY/SPARSE `Datatypes23aiLiveTest` cases pass on the live 23ai/26ai box
  (23.26 supports both DDL forms; the test is self-provisioning).

## Effort / sequencing

Investigation gate: **done** (2026-07-13). Inc 0 trivial (verify-only). Inc 1 medium
(introspection plumbing — feasible thanks to `VECTOR_INFO`). Inc 2 medium (binary,
table-columns-only). Inc 3 (sparse) removed until an ojdbc upgrade. Natural stop points
after each: **Inc 0+1 alone make the generator format-aware** (correct classification, no
silent mis-read) even before binary read/write lands — and, notably, would let it detect
a sparse column and skip/flag it rather than emit the currently-broken `double[]` path.

## Touch-point index (as of 2026-07-13)

- `com/mcpdbwizard/pub/SqlUtils.java` — `ORACLE_VECTOR_DATATYPE` (~396); classify (~671).
- `com/mcpdbwizard/pub/ReadOnlyRowSet.java` — `unloadObject` VECTOR (~722); `getVector`
  (~3713); `getString` VECTOR (~2490).
- `com/mcpdbwizard/pub/WriteableRowSet.java` — `setVector` (~209/220).
- `com/mcpdbwizard/pub/StatementParameters2.java` — VECTOR convenience setter (~648);
  bind (~1018).
- `com/mcpdbwizard/pub/CallableStatementParameters.java` — VECTOR OUT (~273).
- `com/mcpdbwizard/app/common/DatatypeWrangler.java` — `OracleTypes.VECTOR` (~167).
- `com/mcpdbwizard/app/common/JavaUtils.java` — `double[]` mapping (~554).
- `com/mcpdbwizard/app/procbuilder/CallableStatementParameterEngine.java` — VECTOR arm
  (~462); SOAP scalar pass-through (~9620).
- `com/mcpdbwizard/app/procbuilder/SqlStatementWrangler.java` — `VECTOR` token (~36);
  comment hint (~499).
- Tests/DDL — `sql/datatypes_23ai.sql`, `src/test/java/com/mcpdbwizard/pub/Datatypes23aiLiveTest.java`.
