# MCP datatype residuals — plan

Status after the structured-types arc (commits `72850c6`..`66b933a`): an MCP-exposed
PL/SQL routine can already carry all scalars (text, number, DATE, TIMESTAMP, ROWID/UROWID,
RAW, BOOLEAN, JSON, dense/binary VECTOR), OUT ref cursors, CLOB/BLOB, flat records, SQL
object types, and NUMBER/VARCHAR2 collections. This doc plans the remaining uncrossable
types.

## Guiding principle

The whole point is to call *any* PL/SQL (see the `product-purpose-call-any-plsql` memory).
So the target is breadth. But the residuals split by **where the fix lives**, and that — not
value — is what governs risk:

- **MCP-layer only** — changes are confined to `generateMcpServerClass` / the emitted
  `<Factory>McpServer`, gated so non-matching generation stays byte-identical. Low risk;
  verified by the usual fixture → regen → `TGen23aiMcp` → smoke loop.
- **Base-generator** — changes touch the callable-statement wrapper, the SOAP ServiceImpl,
  the record synthesis, or the table-manager generation, i.e. code **every program the wizard
  has ever generated** links against. High risk; needs byte-diff proof on unaffected output
  plus a full multi-box harness run before it can ship.
- **Driver-blocked** — cannot be fixed here at all until ojdbc/server support lands.

## Inventory

| # | Residual | Root cause | Fix location | Effort | Value |
|---|----------|-----------|--------------|--------|-------|
| 1 | Collections of **records / objects** | scoped collections to NUMBER/VARCHAR2 | MCP-layer (compose record+collection) | M | **High** |
| 2 | Collections of **DATE / RAW** elements | element kind gated to number/string | MCP-layer | M | Med |
| 3 | ~~**XMLTYPE / LONG / LONG RAW** params~~ **DONE** (XMLTYPE 7fefaa7; LONG/LONG RAW next commit) | XMLTYPE OUT read via base wrapper's `oracle.xdb.XMLType` (needs `xdb.jar`); LONG OUT-unload String branch NPE'd in useByteArrays mode | Runtime-dep + MCP-layer + a base-gen LONG fix | — | — |
| 4 | **INTERVAL** params (DS / YM) | base SOAP ServiceImpl uses the object getter where a String is expected → won't compile with WEB_SERVICES | Base-generator | M | Med |
| 5 | Bare **TIMESTAMP table column** | base table-manager: row field `String` vs INSERT bind `Timestamp` | Base-generator | M | Med |
| 6 | ~~**TZ timestamps** (TIMESTAMPTZ/LTZ)~~ **DONE** (2026-07-20) | cross as strings via the wrapper's connection-taking `getParam<Cap>String()`; needed an MCP-layer arm + a base-gen `stringValue(conn)` fix | MCP-layer + small base-gen fix | — | — |
| 7 | **Nested records** (record-in-record) | base record synthesis emits an empty class | Base-generator | H | Med |
| 8 | ~~**BFILE** params~~ **DONE** (OUT content base64, IN {directory,filename} locator) | read-only server file: content readable, locator bindable | MCP-layer | — | — |
| 9 | **Sparse / FLOAT16 vectors** | ojdbc11 23.7 has no typed accessor | Driver-blocked | — | Low |

`M`=days, `H`=multi-day / cross-cutting.

## Tiered roadmap

### Tier 1 — MCP-layer, highest value first

**Item 1 — collections of records / objects → JSON array of objects.**
This is the most valuable residual ("return an array of employees"). It composes the two
paths that already exist: `PlsqlArray` (collections) and `RECORD_MAPPER` (records/objects).
- **Verify first** (the lesson from this whole arc — always check the bind path): does the
  generated collection class's `setNewValuesAsObject(Object[])` accept **ORAData record/object
  elements**, or does it require `oracle.sql.Datum` / a `toDatum`? Inspect a generated
  collection-of-records wrapper (e.g. under `generic_test4`/`generic_test1`) and the
  `PlsqlArray` impl's element check. This decides whether IN is a straight compose or needs
  per-element `toDatum(connection)`.
- OUT: `getCurrentValuesAsObject(conn)` returns `Object[]` of record instances → serialize
  each through `RECORD_MAPPER` (reuse the record OUT expression per element).
- IN: convert each JSON object → the record class via `RECORD_MAPPER.convertValue`, collect
  into `Object[]`, `setNewValuesAsObject`.
- Element type: already captured via `lookupCollectionElementType` (`ELEM_TYPE_NAME` +
  `ELEM_TYPE_OWNER`); extend `mcpCollectionElementKind` to recognise a record/object element
  (resolve the element type to its synthesized class, like a top-level record param).
- Gating stays on WEB_SERVICES (createExtraTypeObjects), same as scalar collections.
- Fixture: `TYPE person_tab IS TABLE OF person_rec` + `record_coll_echo(IN/OUT)`.

**Item 2 — collections of DATE / RAW elements.**
- Extend `mcpCollectionElementKind`: DATE → `date`, RAW → `raw`.
- OUT: `collectionToJson` currently `writeValueAsString(getCurrentValuesAsObject(...))`;
  a `Date[]` serialises as epoch and a `byte[][]` awkwardly. Add per-element handling
  (Date → ISO via `formatIsoDate`, `byte[]` → base64) — likely a small loop helper rather
  than a raw `writeValueAsString`.
- IN: `toScalarObjectArray` grows `date` (ISO string → `java.util.Date`) and `raw`
  (base64 → `byte[]`) kinds. **Verify** the collection's `setNewValuesAsObject` accepts
  `java.util.Date` / `byte[]` (it may want `oracle.sql.DATE` / `oracle.sql.Datum`).

**Item 3 — XMLTYPE + LONG + LONG RAW. DONE + verified live (XMLTYPE `7fefaa7`; LONG/LONG RAW next commit, 2026-07-20).**

**LONG / LONG RAW** cross as a JSON text string / base64 string. Both bind IN through the wrapper's
`setParam<Cap>(byte[])` overload (LONG's text is UTF-encoded to bytes, LONG RAW's base64 decoded) and read
OUT through `getParam<Cap>ByteArray()` in `useByteArraysForLongsAndLOBS` mode; no extra runtime dep (plain
JDBC — unlike XMLTYPE). `mcpLobKind` gains "long"/"longraw"; new `longToJson`/`longRawToJson` server helpers.
Fixtures: `FIXTURE_PKG.long_echo` / `long_raw_echo`. Uncovered a **base-generator bug** in the process: the
LONG OUT-unload's `instanceof String` branch (a LONG can come back as a String) always unloaded into the
output File, which is null in useByteArrays mode → `NullPointerException` ("null could not be unloaded"). LONG
RAW dodged it (comes back as an InputStream, whose branch already handled useByteArrays). Fix: the String
branch now takes the String's bytes when useByteArrays is on, else the original File-unload — **behavior-
preserving** for existing code (the `else` is the original line; the flag is false in all non-MCP output),
though the generated *source* for a LONG proc changes (e.g. generic_test1). Verified: FREE23 full
`-Pharnesses-23ai` = 281/0/2, `TGen23aiMcp` LONG + LONG RAW round-trips green.

The XMLTYPE story (`7fefaa7`):
The first (2026-07-20) attempt was reverted — the round-trip hung and the root cause looked like "`xdb.jar` absent from
the runtime CP". The re-attempt lands it:
- **Feature.** `xdb` + `xmlparserv2` 23.7 added as `<optional>` pom deps. `SAAdminWrangler` wires the MCP proc path:
  `mcpLobKind`→"xmltype"; IN via `setParam<Cap>(String)` (the wrapper builds `oracle.xdb.XMLType`); OUT read as a
  `char[]` via `getParam<Cap>ByteArray()` → new `xmlToJson` helper, with `setUseByteArraysForLongsAndLOBS(true)` set on
  any proc that has an XMLTYPE param so the base wrapper reads the OUT XML into a char[] instead of returning an
  `oracle.xdb.XMLType` object. So it IS mostly MCP-layer after all — plus the runtime dep.
- **The real blocker was NOT a missing jar — it was classpath ORDER.** `xdb.jar` was present on the harness child CP all
  along; but `target/test-classes` (first on the CP) holds the compile-only `oracle.xdb.XMLType` **shim**
  (`Scripts/compile-shims`, whose `createXML`/`getClobVal` return `null`), which **shadowed** the real jar → the server
  ran the stub → broken/empty frame → the MCP client failed with `java.io.IOException: Failed to read value`. Fix:
  `TGen23aiMcp.mcpServerClasspath()` now puts the resolved runtime deps FIRST so the real `XMLType` wins (both stdio +
  http launch paths). Also fixed a `provision()` ORA-02303 (drop the package + `OB_GEN_ADDR`/`_TAB` types in dependency
  order before recreating).
- **Verified.** `TGen23aiMcp` 82 checks green (incl. XMLTYPE) on FREE23, `TGen23ai` still green.
- BFILE is DONE (OUT content as base64, IN {directory,filename} locator) — see Item 8.

### Tier 2 — base-generator fixes (independent value beyond MCP)

Each of these is a **real pre-existing base bug**, so fixing it helps SOAP / table users too,
not just MCP. Each ships on its own commit with byte-diff proof + a full multi-box run.

**Item 4 — INTERVAL SOAP ServiceImpl bug. DONE (base-gen fix, multi-box verified).**
- The `ServiceImpl` OUT-unload switch (`CallableStatementParameterEngine`) had a TIMESTAMP case
  using `getParam<Cap>String()` but **no INTERVAL case** → INTERVAL fell to the default (the object
  getter `getParam<Cap>()` → `INTERVALDS`/`YM`) assigned into a `String` field → wouldn't compile.
  Added an INTERVAL case (DS + YM) mirroring TIMESTAMP.
- INTERVAL then came for free on MCP: added the two INTERVAL constants to `mcpProcUsesStringAccessor`.
- Narrow change: **byte-identical** for the canonical set (only scalar-INTERVAL procs differ, none in
  the canonical fixtures — proven with a stash-baseline diff of `generic_test1` = 0). Verified 12c
  ORCL12 277/0/2, 21c ORCL21 271/0/2, 23ai FREE23 281/0/2; `FIXTURE_PKG.interval_echo` crosses via MCP.
  (Only INTERVAL OUT tested; the IN-side setter overload wasn't exercised — see the fixture.)

**Item 5 — bare TIMESTAMP table column. DONE (base-gen fix, multi-box verified).** It turned out
to be a *cluster* of base-generator bugs, not one — fixed all three sites together:
- `addDateSetMethods` now also emits `setParam<Col>(String)` (parsed via `Timestamp.valueOf`) — the
  compile fix, purely additive (byte-diff: 0 removed).
- `SAAdminWrangler.processMany` read-population now casts `(oracle.sql.TIMESTAMP)theArray[N]`
  directly (was `new oracle.sql.TIMESTAMP((java.sql.Timestamp)theArray[N])` → ClassCastException on
  read); fires only for TIMESTAMP columns.
- `buildTableMcpInfo` types a TIMESTAMP column `String` and reads it through `getRow<Col>String()`.
- (The proc TIMESTAMP OUT path was already fine via `getParam<Cap>String()`.)
- Verified: 12c ORCL12 277/0/2, 21c ORCL21 271/0/2, 23ai FREE23 281/0/2, plus `FIXTURE_DR.a_ts`
  round-tripping through the generated MCP DAO. `TIMESTAMP WITH [LOCAL] TIME ZONE` still unfixed.

**Item 6 — TZ timestamps as strings. DONE + multi-box verified (2026-07-20).** TIMESTAMP WITH
[LOCAL] TIME ZONE params now cross as strings. It turned out to be **mostly MCP-layer**, not the
feared big engine change: the generated wrapper ALREADY exposes a string accessor for TZ —
`getParam<Cap>String(java.sql.Connection)` / `setParam<Cap>(java.sql.Connection, String)` — it just
takes a Connection (converting an `oracle.sql.TIMESTAMPTZ`/`LTZ` to/from text needs the session time
zone). The default byte[]/base64 path miscompiled (the plain `getParam<Cap>()` returns the typed
object, and `mcpValueToJsonExpr` assumed a real byte[]). Fix: `mcpIsTzTimestamp()` + emission arms
that read `theProc.get<Cap>String(theFactory.theConnection)` and bind
`theProc.set<Cap>(theFactory.theConnection, <string>)`. The SOAP ServiceImpl was NOT the blocker
(it uses the byte_array getter and compiled fine — the old "TZ trips the SOAP bug" note was wrong).
- **One base-gen bug found + fixed** (`CallableStatementParameterEngine`): the generated
  `getParam<Cap>String(conn)` took a Connection but called the no-arg `stringValue()`, which ojdbc11
  throws on ("Conversion to String failed") for a TIMESTAMPTZ built from raw bytes. Now calls
  `stringValue(paramConnection)`. Strict fix (the old code always threw at runtime), but it does
  change generated source for any TZ param — `generic_test1`/`generic_test4` have some.
- **Format:** `"yyyy-mm-dd hh:mm:ss.fffffffff <zone>"` (e.g. `2026-07-20 14:30:00.0 America/New_York`).
  WITH TIME ZONE round-trips exactly; WITH LOCAL TIME ZONE normalises to the session zone (same
  instant, different wall-clock) — the harness asserts an exact round-trip for TZ and a valid
  timestamp string for LTZ.
- **Verified:** `TGen23aiMcp` TZ round-trips green; full regression on all three lines — 12c ORCL12
  277/0/2, 21c ORCL21 271/0/2 (`-Pharnesses-longids`), 23ai FREE23 281/0/2 (`-Pharnesses-23ai`).

### Tier 3 — deep / blocked

**Item 7 — nested records. DONE (2026-07-21, merge `6bca2e7` + `7dcac31`).** Turned out to be an
8-site base-generator change (not the "one synthesis site" first assumed) threading recursion +
a recursive scalar count through the whole flatten-record machinery: record-field synthesis marking,
adopt-guard, a type-identity record match, PL/SQL-block generation, IN/OUT bind, OUT read, the
parameter numbering, and a direct-fields-only filter for the 12c-native path. A record-in-record now
crosses MCP on 23ai as a nested JSON object AND compiles on 12c/18c/19c/21c. Byte-identical for flat
records; multi-box green (FREE23 281/0/2, ORCL12 277/0/2, ORCL21 271/0/2). Full write-up:
`docs/mcp-item7-nested-records-plan.md`.

**Item 8 — BFILE. DONE (2026-07-21).** (The earlier "won't-do" here was my own mislabel, not a
decision — a BFILE is a read-only server-side file locator, but its content IS readable and its
locator IS bindable, so both directions cross.) MCP-layer only. A **BFILE OUT** crosses as the
referenced file's **content, base64** (read via `getParam<Cap>ByteArray()` in useByteArrays mode, like
LONG RAW). A **BFILE IN** crosses as a **{directory, filename} locator** object: the generated tool
builds an `oracle.sql.BFILE` with `bfileFromLocator` (`SELECT BFILENAME(?, ?) FROM DUAL`) and binds it
through `setParam<Cap>(oracle.sql.BFILE)` — the file's bytes are never written from the client
(BFILEs are read-only). `mcpLobKind` gains "bfile"; new `bfileToJson` / `bfileFromLocator` server
helpers. Fixture: `OB_BFILE_DIR` + `ob_bfile_test.txt`, `FIXTURE_PKG.bfile_getter` / `bfile_length_func`;
`TGen23aiMcp` + smoke cross-check the OUT content length against the IN locator's reported length
(self-gating if the box's Oracle can't write /tmp). Verified: FREE23 281/0/2 (BFILE tests pass),
ORCL12 277/0/2, ORCL21 271/0/2 (byte-identical — no MCP server on the canonical trees).

**Item 9 — sparse / FLOAT16 vectors.** Split into two very different cases (2026-07-21):
- **SPARSE — NO LONGER DRIVER-BLOCKED.** The driver was upgraded ojdbc11 23.7.0.25.01 →
  **23.26.2.0.0** (matches the 26ai server line), which ships the sparse accessor 23.7 lacked:
  `oracle.sql.VECTOR.isSparse()` / `.toSparseDoubleArray()` (→ `{length, indices[], values[]}`) for
  reading and `VECTOR.ofFloat32Values(VECTOR.SparseFloatArray.of(len, idx, vals))` for binding.
  Verified LIVE (read + proc-param bind round-trip) on FREE23. The driver bump is committed + regressed
  green (12c 277/0/2, 21c 271/0/2, 23ai 281/0/2). **DONE 2026-07-21** (this bullet read "Still TODO
  ... not yet done" long after it shipped): `com.mcpdbwizard.pub.SparseVector` is the
  `{length, indices, values}` value class, carried through `SqlUtils` /`ReadOnlyRowSet` /
  `WriteableRowSet` / the engine as a path distinct from dense `double[]` and binary `byte[]`, and a
  sparse **table column** crosses MCP as that JSON object. A sparse **proc param** deliberately does
  not — see `mcp-sparse-vector-plan.md` I4. **Detection wrinkle worth keeping:** a sparse column
  carries the same JDBC metadata as a dense one (`getColumnType()` is `VECTOR_FLOAT32` for both,
  unlike binary's distinct `VECTOR_BINARY`), so the unload reads the raw `oracle.sql.VECTOR` and
  dispatches on `isSparse()`.
- **FLOAT16 — server-blocked, not driver-blocked.** `VECTOR(n, FLOAT16)` → `ORA-51802` on 26ai Free
  23.26 (the DB version has no FLOAT16 dimension format), so the column/param can't even be created.
  No driver fixes this; wait for an Oracle *server* that adds the format.

## Recommended order

1. **Item 1** (collections of records/objects) — highest value, MCP-layer, composes existing
   code. Start with the bind-path verification.
2. **Item 2** (DATE/RAW collection elements) — small, MCP-layer, rounds out collections.
3. **Item 5** (TIMESTAMP table column) — a genuine base bug worth fixing on its own; unblocks
   a common column type for every table DAO. Do it as the first base-gen change because it's
   self-contained (table-manager only, no SOAP entanglement).
4. **Item 4** (INTERVAL SOAP fix) — second base-gen change; unblocks INTERVAL for SOAP and MCP.
5. ~~**Item 3** (XMLTYPE / LONG / LONG RAW)~~ **DONE (XMLTYPE 7fefaa7; LONG/LONG RAW 2026-07-20)** — MCP-layer +
   `xdb.jar` runtime dep for XMLTYPE, plus a base-gen LONG-unload fix; see the Item 3 detail above.
6. ~~**Item 6** (TZ timestamps)~~ **DONE (2026-07-20)** — MCP-layer arm + a base-gen `stringValue(conn)` fix;
   see the Item 6 detail above. Defer **Item 7** (nested records); **Items 8, 9** are won't-do / blocked.

## What's left

- **Item 7 — nested records** (deep base-gen, H): base synthesis emits an empty class for a record-typed field.
- **Item 9**: sparse+FLOAT16 vectors (driver-blocked). (Item 8 BFILE is DONE.)
- **Beyond datatypes:** MCP-server productization — authz/security (the HTTP transport is open with no auth) and
  per-object curation / read-only mode (every qualifying object, incl. destructive DML/DDL, is currently exposed).

## Verification bar

- MCP-layer items: fixture proc → `Scripts/testrun_current.sh` (full canonical set) →
  `TGen23aiMcp` new check → `smoke_test_generated.py` → full `-Pharnesses-23ai` (must stay
  281/0/2). Non-matching generation must remain byte-identical (the existing gating pattern).
- Base-generator items additionally require: a tree-diff proving unaffected output is
  byte-identical, and a green run on at least one box per line (12c, 19c/21c, 23ai) — the
  `harnesses-longids` / base / `harnesses-23ai` profiles — since the change touches code all
  generated programs link against.
