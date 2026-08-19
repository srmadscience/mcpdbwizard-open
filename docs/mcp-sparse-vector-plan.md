# Sparse VECTOR support — scope

Item 9 (sparse half) of `docs/mcp-datatype-residuals-plan.md`. **Unblocked** by the ojdbc11
23.7 → 23.26.2.0.0 upgrade (commit `0111bf8`): the driver now reads/writes sparse vectors. This
doc scopes actually implementing them across `com.mcpdbwizard.pub`, the generator, and the MCP layer.
FLOAT16 is **out of scope** — server-blocked (`ORA-51802`), no driver fixes it.

## Goal

A `VECTOR(n, <fmt>, SPARSE)` **table column** or a sparse **PL/SQL VECTOR param** generates working
read+write DAO/wrapper code and crosses MCP as a `{length, indices, values}` JSON object — instead of
being detected and skipped as it is today.

## What the driver now gives us (verified live, FREE23)

- **Read:** `oracle.sql.VECTOR.isSparse()` → true; `.toSparseDoubleArray()` returns a
  `VECTOR.SparseDoubleArray` — parent `SparseArray` has `int length()` + `int[] indices()`; the
  double subtype adds `double[] values()`. (Also `toSparseFloatArray()` / `toSparseByteArray()` /
  `toSparseBooleanArray()`.)
- **Write:** `VECTOR.SparseFloatArray.of(int length, int[] indices, float[] values)` (and
  `SparseDoubleArray.of(...)`, `.fromDenseArray(...)`), then `VECTOR.ofFloat32Values(sparseArray)` /
  `ofFloat64Values(sparseArray)` to build a bindable `VECTOR`.
- **No `VECTOR_SPARSE` OracleType constant** — a sparse value binds through the generic
  `OracleType.VECTOR` carrying a `SparseArray`. (Confirmed by a proc-param round-trip through
  `echo_sparse(p_in IN VECTOR, p_out OUT VECTOR)`.)

## The template: the binary-VECTOR path

Sparse mirrors what Increment 2 did for binary vectors (`ORACLE_VECTOR_BINARY_DATATYPE`, `byte[]`).
The parallel sites, all already present for binary:
- `SqlUtils`: `ORACLE_VECTOR_SPARSE_DATATYPE = 32` **already exists** + `getVectorDatatypeFromInfo`
  classifies `SPARSE`; the table-column introspection already rewrites `DATA_TYPE` to a
  `VECTOR_SPARSE` token (`SqlStatementDictionary` `vectorFormatDataType`). So detection is DONE —
  only the read/write/skip sites need doing.
- `ReadOnlyRowSet.getVectorBinary()` ⟶ add `getVectorSparse()`; the row-unload switch
  (`ORACLE_VECTOR_BINARY_DATATYPE` → `getObject(byte[].class)`) ⟶ add a sparse arm.
- `WriteableRowSet.setVectorBinary()` ⟶ add `setVectorSparse()`; `StatementParameters2`
  `setVectorBinaryParam()` ⟶ add `setVectorSparseParam()`; `CallableStatementParameters`
  binary OUT unload ⟶ add sparse.
- Generator: `JavaUtils.oracle2JavaDatatype` (sparse → the new class, was `"Object"`),
  `DatatypeWrangler.getOracletypeCode` (sparse → `OracleTypes.VECTOR`), the
  `CallableStatementParameterEngine` binary-vector arms (variable-type switch, IN-bind ladder,
  OUT unload, SOAP pass-through) ⟶ add sparse arms, and remove the two skip guards
  (`SAAdminWrangler` table-column loop `~6643`; `JavaUtils` "Object" fallthrough).
- MCP (`SAAdminWrangler.generateMcpServerClass`): the dense-vector `toDoubleArray` / vector-→-array
  arms ⟶ add a sparse `{length, indices, values}` object form.

## Representation

New `com.mcpdbwizard.pub.SparseVector` (small, immutable-ish value class):
```java
public final class SparseVector {
    public int length;        // total dimensions
    public int[] indices;     // non-zero positions (ascending)
    public double[] values;   // the non-zero values, values[k] at indices[k]
}
```
Public fields (matches the `RECORD_MAPPER` field-visibility Jackson style already used for records, so
MCP JSON `{length, indices, values}` round-trips for free). Read builds it from
`SparseDoubleArray`; write turns it into `VECTOR.ofFloat64Values(SparseDoubleArray.of(length, indices,
values))`.

## Increments

**I0 — spike the write format. DONE (2026-07-21) — RESOLVED: single FLOAT64 write path, Oracle
coerces.** The unknown was whether a FLOAT64 sparse value binds into non-FLOAT64 sparse columns.
Verified live on FREE23 (ojdbc11 23.26.2.0.0) by inserting `ofFloat64Values(SparseDoubleArray.of(25,
[3,10], [1.5,-2.0]))` into every column format and reading back:
- `VECTOR(25, FLOAT32, SPARSE)` ← FLOAT64 → OK, exact `[1.5, -2.0]`
- `VECTOR(25, FLOAT64, SPARSE)` ← FLOAT64 → OK, exact
- `VECTOR(25, INT8, SPARSE)`    ← FLOAT64 → OK, `[2.0, -2.0]` (1.5 rounds to 2 — inherent to INT8)

**Conclusion:** the writer does NOT need to be format-aware. `setVectorSparse` / `setVectorSparseParam`
can ALWAYS build `ofFloat64Values(SparseDoubleArray.of(length, indices, values))` and let Oracle coerce
to the column/param format (an INT8 column rounds fractional values, as it must). This simplifies I1/I2
— no format token to carry for writing. (Read is likewise uniform: `toSparseDoubleArray()` → `double[]`
for every element format.)

**I1 — pub read/write. DONE (2026-07-21).** New `com.mcpdbwizard.pub.SparseVector` ({length,
indices, values} + `fromVector`/`toVector`/`toDenseArray`); `ReadOnlyRowSet.getVectorSparse()` +
`WriteableRowSet.setVectorSparse()` + `StatementParameters2.setVectorSparseParam()` + a `SparseVector`
bind arm (converts to `oracle.sql.VECTOR` via `ofFloat64Values`). **Detection wrinkle handled:** a
sparse column carries the SAME JDBC metadata as a dense one (`getColumnType()` = VECTOR_FLOAT32 for
both — unlike binary's VECTOR_BINARY), so the `ORACLE_VECTOR_DATATYPE` row-unload now reads the value
as an `oracle.sql.VECTOR` and dispatches on `isSparse()`: sparse → `SparseVector` (preserved, not
densified), dense → `toDoubleArray()` (equivalent to the old `double[].class` read). `getVector()` on
a sparse column densifies (`toDenseArray`). Verified: `Datatypes23aiLiveTest` sparse read + bound-write
round-trip (`{length:25, indices:[3,10], values:[1.5,-2.0]}`); multi-box green FREE23 283/0/2, ORCL12
277/0/2, ORCL21 271/0/2 (the unload arm is inert pre-23ai — no VECTOR columns). The proc-param OUT
unload (`CallableStatementParameters`) is deferred to I3. Requires ojdbc 23.26+ (shipped in `0111bf8`).

**I2 — generator: sparse table columns. DONE (2026-07-21).** Un-skipped sparse columns (removed
the `Table being ignored` guard) and threaded the `VECTOR_SPARSE` token through
`JavaUtils.oracle2JavaDatatype` (→ `com.mcpdbwizard.pub.SparseVector`, was `"Object"`),
`DatatypeWrangler.getOracletypeCode` (→ `OracleTypes.VECTOR`), `SqlStatementWrangler`'s
`ASP_DATA_TYPES`, the `SAAdminWrangler` comment-writer (emits the `VECTOR_SPARSE` token), and the
`CallableStatementParameterEngine` arms — so a sparse column generates a `SparseVector` DAO field that
reads via `processMany`'s generic `(SparseVector)theArray[n]` cast and binds INSERT/UPDATE via
`setVectorSparseParam`. Verified: `FIXTURE_TABLE.sparse_embed` (`VECTOR(25, FLOAT32, SPARSE)`)
round-trips through the generated DAO; `TGen23ai` reads it as `{25, [3,10], [1.5,-2.0]}`. Non-VECTOR
output is byte-identical (all changes fire only on the 23ai `VECTOR_SPARSE` token).

**I3 — generator: sparse PL/SQL VECTOR params. DONE (2026-07-21).** Extended the format-flexible
`VECTOR` proc-param surface from a DUAL (dense `double[]` + binary `byte[]`) to a TRIPLE surface: a
third `SparseVector` companion field, a `setParam<X>(SparseVector)` overload (clears the other two),
the IN-bind three-way dispatch (`setVectorSparseParam` → `setVectorBinaryParam` → `setParam`), the OUT
`instanceof` dispatch (`SparseVector` → the `*VectorSparse` getter), and a `getParam<X>VectorSparse()`
getter. Backed by `CallableStatementParameters`' `VECTOR` OUT unload now peeking the raw
`oracle.sql.VECTOR` and returning a `SparseVector` when `isSparse()`. So a Java caller can drive any
sparse VECTOR proc param through the generated wrapper. Verified: `TGen23ai` round-trips a sparse
vector through `FIXTURE_PKG.vector_echo(p_in IN VECTOR, p_out OUT VECTOR)`.

**I4 — MCP layer. Table columns DONE (2026-07-21); proc-param format dispatch SCOPED OUT.**
`mcpJavaSchemaType`/`mcpValueToJsonExpr`/`mcpArgConversion` gained a `SparseVector` arm (→ `"object"`,
OUT `RECORD_MAPPER.writeValueAsString`, IN `RECORD_MAPPER.convertValue(arg, SparseVector.class)`), so a
sparse **table column** crosses MCP as a `{length, indices, values}` JSON object and round-trips through
`<table>_insert`/`_get_by_pk`. Verified: `TGen23aiMcp` + `smoke_test_generated.py`.
**Scoped out — MCP proc-param format dispatch:** a `VECTOR` **proc param** still crosses MCP only as a
dense array (its existing behavior — the common case). Accepting the binary (base64) or sparse
(`{indices,…}` object) forms over an MCP proc tool would need a `oneOf` union `inputSchema` plus a
runtime shape-dispatch on both IN and OUT — high-complexity, low-value (sparse vectors are fundamentally
a table-storage feature; a format-flexible proc param has no fixed format, and the dense form already
crosses). The generator **wrapper** (I3) fully supports sparse proc params for Java callers; only the
MCP tool surface is dense-only. Not a regression — binary proc params were never MCP-crossable either.

## Fixtures & tests

- `sql/datatypes_23ai.sql` (pub live test): a sparse column for `Datatypes23aiLiveTest`.
- `sql/datatypes_23ai_gen.sql` + `generic_test_23ai.pb2`: a sparse column on `FIXTURE_TABLE` (or a new
  table) and `FIXTURE_PKG.echo_sparse(p_in IN VECTOR, p_out OUT VECTOR)`; `TGen23ai` (DAO round-trip),
  `TGen23aiMcp` + smoke (MCP `{length,indices,values}` round-trip).

## Risk & scope

- **VECTOR is 23ai-only** — every generator change fires only on 23ai; pre-23ai (12c/18c/19c/21c)
  output is byte-identical (no VECTOR at all), so the multi-box concern is only "don't break existing
  dense/binary VECTOR on 23ai," not a base-gen blast radius.
- **`com.mcpdbwizard.pub` change** (`SparseVector` + new methods) ships with every generated program,
  but it's purely additive (new class, new methods) — no existing signature changes.
- **Effort:** larger than a scalar residual (touches pub + ~5 generator sites + MCP), smaller than
  Item 7. The genuine unknowns are I0 (write-format coercion) and I4's proc-param shape dispatch;
  everything else is a faithful copy of the binary-VECTOR path. Estimate ~1–2 focused sessions.

## Verification bar

Byte-identical for non-sparse output (dense/binary VECTOR round-trips unchanged); `Datatypes23aiLiveTest`
sparse round-trip; `TGen23ai`/`TGen23aiMcp` new sparse checks; full `-Pharnesses-23ai` stays green,
plus a sanity ORCL12/ORCL21 run (must be byte-identical — no VECTOR there).
