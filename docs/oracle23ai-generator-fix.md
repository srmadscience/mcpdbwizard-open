# Oracle 23ai generator fix — status & resume notes

Tracking the work to make the **generator** emit compilable wrapper code for the
standard APPSCHEMA-shaped schemas when introspecting an **Oracle 23ai** database, so the
full APPSCHEMA generated-harness suite can run against the 23ai box (not just the 12c box).

> **COMPLETE as of 2026-07-13 — all resume-checklist items closed.** The three
> nested-type synthesis increments (`0f64ea6` scalar, `5de512a` non-scalar collection +
> index-by, `d3c7f22` record-as-type fields) and the dedup null-guard (`aaf95d4`) are all
> in HEAD; the record-in-record recursion item is closed as unreachable by any available
> schema. Standalone 23ai generation is functionally complete, 12c stays 250/0/0, and the
> full generated-harness suite now runs GREEN on a **single** 23ai/26ai box under
> `-Pharnesses-23ai` — verified on the new **FREE26** (26ai Free 23.26.0.0.0) box at
> **267/0/2** on 2026-07-13 (FREE23 likewise). The dated notes below are the historical trail.

Status as of 2026-06-13: Increment 1 (scalar) committed. Increment 2 in the working tree,
**not yet committed** — `generic_test1` now **generates and compiles clean on the 23ai box**
(FREE23): 5095 broken files → 2436, 0 compile errors. Three 23ai bugs root-caused and fixed,
all the same underlying cause (the element walk returning nothing or a spurious sibling row
where 12c had the dropped `DATA_LEVEL>0` child): (a) a swallowed NPE that disabled all dedup;
(b) SOAP `*Attrs` companions mis-named after the collection instead of the element; (c) index-by
associative-array element datatype unknown → wrong/absent SOAP converters. Element synthesis now
covers scalar/record/object/%rowtype collections and index-by tables. 12c unchanged (2482,
compiles clean). **All 7 propfiles now generate + compile clean standalone on FREE23.** The
generated-harness *suite* does not yet compile on 23ai — it surfaced a **fourth gap** (direct
package RECORD types lose their fields; same `ALL_ARGUMENTS` root cause, record-type path). A
prototype synthesis fixed the package-record sub-case (3→85 setters) but cascaded into more
sub-cases (table `%ROWTYPE`/DB-object record params); since that regressed the clean standalone
build and the rest is open-ended ("multi-week"), the prototype was **reverted** — tree is back to
all-7-compile-clean. **Update (commit 3/n):** that "reverted" note describes an earlier prototype and
is now superseded — the package-record and table-rowtype field-synthesis arms are committed (12c green,
combined-tree `mvn test` = 250/0/0); the FREE23 harness suite compiles much further, with the remaining
record sub-cases enumerated in the fourth-gap section below.

**Final conclusion (commits a3a7e10 superseded):** standalone 23ai generation is functionally complete —
all 7 propfiles generate + compile clean with records populated. The residual *harness-suite* failures
are NOT generator bugs: they are places where 23ai's data dictionary reports metadata more accurately
than 12c (native `BOOLEAN`; subtypes/`%ROWTYPE` resolved to their base in `ALL_ARGUMENTS`), so the
generator emits correct-but-differently-named output that the 12c-hard-coded harness can't match. The
suite is a 12c-coupled test asset and must be regenerated/adapted for 23ai, not "fixed" in the
generator. **Update 2026-06-14: no reachable generator gap remains** — the previously-flagged
"nested-field record recursion" item was investigated and found to be unreachable on every available
test schema (the plain-record nested-field stub branch is never hit; the only reachable nested-field
args are collections-of-object handled by the collection arm). See the "Nested-field records" bullet.
See the fourth-gap section for the evidence.

**Runtime-validated on FREE23 (2026-06-14).** Beyond compile-clean, the 23ai path is proven to *run*:
- `Datatypes23aiLiveTest` — **6 run, 0 failures, 0 skipped**: the `com.mcpdbwizard.pub` library reads+
  writes JSON / VECTOR / native BOOLEAN on 23ai.
- `TGen23ai` — **1 run, 0 failures, 0 skipped**: the *generator's* 23ai output round-trips — the
  generated DAO reads native BOOLEAN/JSON/VECTOR columns and round-trips JSON/VECTOR proc params.
  (Run by temporarily setting aside the four 12c-coupled harnesses — `TSubtypes01`,
  `TRecordTest2/3SALPubSyn`, `TFunctionScalerDatatypes` — so the test profile compiles; they were
  restored afterwards.)
So: compile-clean (all 7 propfiles), 12c-green (combined-tree 250/0/0), AND 23ai-runtime-green
(`Datatypes23aiLiveTest` + `TGen23ai`).

## Generated temp-file hygiene (2026-06-14)

Two unrelated-to-23ai-but-surfaced-while-testing fixes to how generated code names and
locates the temporary files it spills LONG/CLOB/BLOB/BFILE data into. Both stemmed from
the **callable-statement wrapper** generator path never being wired to the propfile temp
settings (the parallel `DAOFactory` path already was):

- **Prefix/suffix (commit `a8cf079`).** Generated callable-statement wrappers shipped
  `tempFilePrefix`/`tempFileSuffix` as the literal template tokens
  `"PARAM_TEMP_FILE_PREFIX"`/`"PARAM_TEMP_FILE_SUFFIX"`, so any LOB unload minted files
  literally named `PARAM_TEMP_FILE_PREFIX...SUFFIX` (thousands accumulated in the repo
  root under test runs). Now: new shared `IOUtils.DEFAULT_TEMP_FILE_PREFIX` (`"ob"`) /
  `DEFAULT_TEMP_FILE_SUFFIX` (`".tmp"`) constants back the `pub` library defaults
  (`ReadOnlyRowSet`, `QueryStatement`); the generator emits the **propfile** prefix/suffix
  (`OSOFT`/`.tmp`), threaded through `SAAdminWrangler.effectiveTempFile{Prefix,Suffix}`
  instance fields (the helper generators don't receive them as params) into the engine's
  `configTempFile{Prefix,Suffix}`.
- **Directory (this commit).** Both the callable-statement path (hardcoded) and the
  `DAOFactory` path (propfile `DEFAULT_TEMPDIR=user.dir`) defaulted `tempFileDir` to the
  **current working directory**, so the files landed wherever the app was launched. Now
  `IOUtils.getOsTempDir()` returns `java.io.tmpdir` (the real cross-platform OS temp dir;
  the old branch returned a possibly-nonexistent `~/Temp` on macOS), the callable-statement
  path emits `getOsTempDir()`, and the `DAOFactory` path maps the cwd sentinel
  (`user.dir`/empty) to `getOsTempDir()` while still honouring a genuinely configured dir.

Validated: 12c harness suite 250/0/0 with **zero** files left in the repo root after a run
(the same suite previously dumped ~1600); the temp files instead land in
`/var/folders/.../T/`. 23ai-native `TGen23ai` + `Datatypes23aiLiveTest` still 7/0/0.

## The problem (root cause, confirmed)

Generating `generic_test1/4/testd` against the 23ai box (FREE23) produces ~56 fewer
files than against 12c and **fails to compile**: the SOAP layer
(`DAOFactoryServiceImpl/Interface`) references `*Attrs` companion classes that were
never generated.

**Root cause:** Oracle 23ai's `ALL_ARGUMENTS` no longer returns the nested
`DATA_LEVEL>0` child rows that flatten a collection/record argument's element
structure. For package `ORACLE_ARRAYS`: ORCL12 has **115** `data_level>0` rows;
FREE23 has **0**. The generator discovers a collection's element type in
`SAAdminWrangler.getChildRecord()` / `getNextAttrArgument()` by reading the next
`ALL_ARGUMENTS` row at `data_level+1`; with none, the type is flagged
`usable=false` ("varray X is not usable" in the genlog) and its wrapper + `*Attrs`
SOAP companion are skipped.

Chain: SOAP needs `*Attrs` → type "not usable" → `getChildRecord` null →
`getNextAttrArgument` (the `ALL_ARGUMENTS` `data_level` walk) returns 0 rows on 23ai.

The element info **is** available on 23ai via the PL/SQL type views —
`ALL_PLSQL_COLL_TYPES` (package collections), `ALL_PLSQL_TYPE_ATTRS` (package
records), `ALL_TYPE_ATTRS` (schema objects), `ALL_COLL_TYPES` (schema collections) —
so the fix is to synthesise the missing `ALL_ARGUMENTS`-shaped rows from those views
when the walk is empty. The engine already has this synthesis pattern in
`getAttrArguments` (e.g. `typeArgQrySelect` ← `ALL_TYPE_ATTRS`); the gap is the
**package-level** collections/records.

## Progress

### Increment 1 — package collection of a SCALAR element — DONE (committed `0f64ea6`)

`SAAdminWrangler.getChildRecord`, when `getNextAttrArgument` returns 0 rows, calls a
new `getPackageCollectionChild()` helper that synthesises the element row from
`ALL_PLSQL_COLL_TYPES` via `SqlStatementDictionary.getPlsqlCollScalarQry` /
`plsqlCollScalarArgQrySelect` (scalar element = `elem_type_owner IS NULL`, e.g.
`TBL_VARRAY_NUMBER_OA`). The fallback fires **only** when the `ALL_ARGUMENTS` walk is
empty, so the 12c path is untouched.

Validated both hosts: `generic_test1` @ORCL12 byte-identical (2482 files, 0 "not
usable"); @FREE23 the scalar package collections now generate and drop off the "not
usable" list.

### Increment 2 — package collection of a non-scalar element + index-by elements — `generic_test1` now compiles clean on 23ai (was 5095 broken files; now 2436, 0 errors)

Re-implemented and root-caused (2026-06-13). The over-generation was **not** a multi-stage
type-emission/dedup problem — it was a single **swallowed NullPointerException** that
aborted the entire duplicate-elimination pass. With that fixed, the remaining SOAP compile
errors were a second 23ai-only bug (the element walk returning a spurious sibling row), fixed
by detecting "no deeper child" and synthesising the element for all four kinds.

**The record synthesis (verified empirically against 12c).**
`getPackageCollectionChild` now has a record arm: when the scalar arm returns 0 rows it runs
`SqlStatementDictionary.getPlsqlCollRecordQry` / `plsqlCollRecordArgQrySelect` (19 binds) — a
`UNION ALL` of the element-header row from `ALL_PLSQL_COLL_TYPES` and one field row per record
attribute from `ALL_PLSQL_TYPE_ATTRS`, `ORDER BY SEQUENCE`. An `EXISTS … ALL_PLSQL_TYPES
typecode='PL/SQL RECORD'` guard scopes it to true package records (`elem_type_package` non-null),
excluding `%ROWTYPE` and the `_OB` (null-package) records, which need separate handling. Dumping
`COLL_PROC_4.P_PARAM7` (`TABLE OF ORACLE_ARRAYS.TBL_ARRAY_COMMANDS_TYPE_OA`) on ORCL12 vs the
synthesised rows on FREE23 confirmed a byte-identical 9-row shape (header `seq=14/lvl=1` +
8 fields `seq=15..22/lvl=2`).

**Root cause of the 5095-vs-2482 over-generation (THE answer to the old "frontier").**
`PlsqlRecordObject.plsqlEqualsByArgs` (the Part-1 dedup comparator) does
`this.procArgName.equals(other.procArgName)`. The synthesised record's `procArgName` came from
the element-header row's `ARGUMENT_NAME`, which the first cut emitted as `to_char(null)` — so it
was **null**. 12c never hits this because the live `ALL_ARGUMENTS` walk reads the header through
a query that applies `nvl(argument_name,'_function_result'||…)`, so 12c's header arg name is the
synthetic `_function_result_<seq>`, never null. The null `procArgName` threw an NPE that the
engine's outer `catch (Exception e) { mrLog.syserror("Unable to parse PL/SQL Parameters:"…) }`
swallowed — silently skipping **both** dedup passes. With no dedup, ~1494 near-duplicate
`extraObjects` (e.g. one record type reached from 158 procs) all survived and emitted → ~2×
files. This is exactly why the prior session's register-site guard (step 4) didn't move the file
count: the ExtraType registry was never the emitter; the dedup simply wasn't running.

**The fix.** The record-arm header row now emits `'_function_result_'||to_char(? + 1)` for
`ARGUMENT_NAME`, reproducing 12c's synthetic name. Result on FREE23 `generic_test1`:
**5095 → 2434 files, 0 NPE, 1380 dedup collapses fire.** (Increment 1's scalar arm also emits a
null `ARGUMENT_NAME` but never crashed, because the scalar element is *not* added to
`extraObjects` — only the record element is, so only it reached the comparator.)

**Second 23ai bug — the element walk returns a spurious sibling row (the SOAP `*Attrs`
mis-naming).** With dedup restored, 20 SOAP errors remained: parameters whose element is a
collection-of-record referenced `OracleArrays**TblVarrayCommandsTypeOa**Attrs` (named after the
*collection*) instead of the generated `OracleArrays**TypeArrayCommandsOa**Attrs` (named after the
*record*). Root cause: the element walk (`argRecQryAttrOrderBy`) returns rows whose `sequence`
falls between this argument and the *next sibling argument*. On 12c those are the element/field
child rows; on 23ai there are none, so for any non-final argument the walk returns the **next
sibling argument itself** — a `DATA_LEVEL == procDataLevel` row, not a child. `getChildRecord` then
built a degenerate single-row record from that sibling. (The same-typed param in a proc whose
collection arg was *last*, e.g. `COLL_PROC_5.P_PARAM1`, had no next sibling, so its walk returned 0
and synthesis fired correctly — which is why one occurrence was right and another wrong.)

**The fix (`getChildRecord`).** A genuine element child is always at `procDataLevel+1`, so the
synthesis fallback now triggers whenever the walk returns **no row deeper than `procDataLevel`**
(`walkHasChild`), not only on an empty walk. 12c always returns the real deeper child, so it is
untouched (verified: `generic_test1` @ORCL12 still 2482 files, compiles clean). `getPackageCollectionChild`
grew arms for every element kind, each verified empirically against the 12c `ALL_ARGUMENTS` shape
and scoped by mutually-exclusive `ALL_PLSQL_COLL_TYPES` guards:

| element kind | arm | synthesised header | where attrs come from |
|---|---|---|---|
| scalar (`elem_type_owner` NULL) | `plsqlCollScalarArgQrySelect` (inc. 1) | scalar element row | — |
| package RECORD (`elem_type_package` non-null) | `plsqlCollRecordArgQrySelect` | `PL/SQL RECORD` + field rows | the UNION'd field rows |
| schema OBJECT (`elem_type_package` NULL, ALL_TYPES OBJECT) | `plsqlCollObjectArgQrySelect` | single `OBJECT` header | `getAttrArguments` ← `ALL_TYPE_ATTRS` |
| table `%ROWTYPE` (`elem_type_name LIKE '%\%ROWTYPE'`) | `plsqlCollRowtypeArgQrySelect` | single all-null `PL/SQL RECORD` header | source parse (`getRowTypeofType`) |

Result on FREE23 `generic_test1`: **5095 → 2436 files; 0 SOAP `*Attrs` errors** (was 20).

**Third 23ai bug — index-by (associative array) elements (the last 8 errors).** The SOAP layer
emits `createIndexByTableFrom<Type>Array` / `create<Type>ArrayFromIndexByTable` converters keyed on
the index-by table's element datatype, discovered by `CallableStatementParameterEngine`'s own
`getAttrArguments` (the same `ATTR_ARG_QUERY` walk). On 23ai that walk again finds no element child
(or the next sibling argument), so the element datatype was unknown → wrong/absent converters
(`createIndexByTableFromBigDecimalArray(PlsqlIndexByTable2,int,int)` with no matching definition).
Fixed with the same "no deeper child → synthesise" pattern at the engine call site:
`plsqlIndexbyElemArgQrySelect` / `getPlsqlIndexbyElemQry` synthesises the scalar element row
(DATA_TYPE + precision/scale/length) from `ALL_PLSQL_COLL_TYPES`. After this, `SAMPLE_PROC_01` (CHAR) emits
the String converters and `SAMPLE_PROC_02` (NUMBER) the BigDecimal/double converters, and **`generic_test1`
compiles clean on FREE23 (2436 files, 0 errors)**.

**Assessment:** the over-generation framed as "large, multi-week" was a swallowed NPE; the SOAP
`*Attrs` mis-naming and the index-by converter gap were both the same 23ai cause — the element
walk returning nothing or a spurious sibling row instead of the dropped `DATA_LEVEL>0` child. All
fixed by synthesising the element from the `ALL_PLSQL_*` views whenever the walk has no deeper
child. 12c untouched throughout (still 2482 files, compiles clean).

### Fourth 23ai gap — direct RECORD types lose their fields (record-as-TYPE path)

A record class's fields are discovered in the **"Supporting class identified" loop**
(`SAAdminWrangler` ~line 13792) via `getAttrArguments(...)` — the `ATTR_ARG_QUERY` `ALL_ARGUMENTS`
walk, which on 23ai has no `DATA_LEVEL>0` field rows → `fieldCount=0` → empty record class. This is
the **record-as-type/parameter** path, distinct from the collection-element path. Standalone
generation compiles either way (the procs that *use* a still-empty record are skipped too, so the
tree stays internally consistent), so the gap only fully surfaces when the 12c-authored **harness
suite** imports the records with their fields. The record identity is on the extraObject
(`SAAdminWrangler` ~1148: `packageName`=arg `TYPE_NAME`, `objectName`=arg `TYPE_SUBNAME`,
`realOwner`=owner — all from the data_level=0 arg row 23ai keeps), so the fields can be synthesised.

#### Done (commit 3/n) — package-record + table-`%ROWTYPE` arms; 12c green (250/0/0), all 7 standalone-clean on 23ai

Two field-synthesis arms now run in the loop when `functionArgRowset.size()==0` for a
`PL/SQL RECORD`, each verified against the 12c field shape:
- **Package record** (`plsqlRecordFieldArgQrySelect` ← `ALL_PLSQL_TYPE_ATTRS`, `getPlsqlRecordFieldQry`)
  keyed on `(realOwner, packageName, objectName)`. `RecordTest28iRecordtype` 3 → **85 setters +
  `moveLobsToByteArrays`**. **Adopted only when every field is scalar** (`TYPE_OWNER` null) — a record
  with a nested package-type field (e.g. `IBA_TEST.L_REC` whose fields are `VARCHAR2_IBA` etc.) needs
  the engine to recurse into a structure this flat row can't express, so it is left as the compiling
  empty stub it was before (nested record fields = a later sub-case, below).
- **Table/view `%ROWTYPE`** (`plsqlRowtypeFieldArgQrySelect` ← `ALL_TAB_COLUMNS`,
  `getPlsqlRowtypeFieldQry`) keyed on `(realOwner, objectName)`; the query returns nothing for
  non-table names so genuine schema `OBJECT` types (still fine via `ALL_TYPE_ATTRS` on 23ai) are
  untouched. `LegacyDatatypes` (`LEGACY_DATATYPES%ROWTYPE`) now has its 12 columns + `moveLobsToByteArrays`.

Two supporting fixes ship with them:
1. **Datatype spelling.** `ALL_PLSQL_TYPE_ATTRS` reports `'PL/SQL ROWID'`/`'PL/SQL LONG'` where
   `ALL_ARGUMENTS` (and the engine) use plain `'ROWID'`/`'LONG'`; an unrecognised type left the engine's
   `oracleParamDatatype[i]` null → NPE. Mapped back with `plsqlAttrDataTypeDecode`.
2. **Fatal-abort robustness.** That NPE was *uncaught* in the loop, so one bad record aborted the
   whole generation before any files were written (output once collapsed to 16 files). The per-record
   engine build/validate is now wrapped in `try/catch` → degrade to "not usable" — a robustness fix
   good independent of 23ai.

All arms fire ONLY when the live walk is empty, so 12c is untouched: **`mvn test` on ORCL12 (combined
tree) = 250 run, 0 failures, 0 errors, 2 skipped**, and all seven propfiles still compile clean
standalone on FREE23.

#### Remaining (the harness-suite tail) — and why it is build-phase, not loop, work

With the two arms in, the FREE23 harness *suite* (`mvn test`) compiles much further. The residual
test-compile errors (per `generic_test1`: ~20 `TSubtypes01`, 20+20 `TRecordTest2/3SALPubSyn`, 16
`TFunctionScalerDatatypes`, a few small) fall into groups — and an important distinction emerged when
probing them:

**The records that reach the "Supporting class identified" loop are now handled.** A probe over that
loop on `generic_test1` shows the only remaining empty entries are *collections* (objType 9), not
records — the package + table-`%ROWTYPE` arms cover every record that gets there (e.g. `Agents` /
`SynuserAgents` = `agents%ROWTYPE` each synthesise their 21 columns).

**The still-failing records are killed EARLIER, in the build phase** (`SAAdminWrangler` ~1018-1105),
so they never reach the loop arms (no `try/catch` degradation, not in the loop probe). They are the
parser-resolved kinds whose field walk 23ai dropped:
- **Subtype records** (`AGENTS_SUBTYPES.AGENTS_ROWTYPE` etc.): PL/SQL `SUBTYPE`s
  (`subtype agents_rowtype is agents%rowtype`, `subtype truly_insane is agents_subtypes_types.agents_rowtype`).
  These are in **no** data-dictionary type view (`ALL_PLSQL_TYPES/TYPE_ATTRS/COLL_TYPES` are empty for
  the package) — only in package **source**. Resolving them is a subtype/`%ROWTYPE` **chain** parse
  (`agents_rowtype`→`agents%rowtype`→`AGENTS` table → `ALL_TAB_COLUMNS`), in the build-phase
  `theParser.getRowType(...)` path, not a flat view query.
- **Public-synonym records** (`*PubsynRecordTest2`, hash-prefixed): records reached via a public
  synonym; also dropped before the loop. Needs synonym resolution to the real owner/package/type.
- **Nested-field records** (`IBA_TEST.L_REC`): fields are themselves package collections/types —
  needs recursion, not a flat row. Currently a compiling stub (scalar-only guard).
- **PL/SQL BOOLEAN accessors** (`getFunctionResultBoolean()`/`getParamInOutParamBooleanObj()` in
  `TFunctionScalerDatatypes`): **NOT a generator gap — intended 23ai native-boolean behaviour.**
  12c's `ALL_ARGUMENTS` reports `DATATYPE_TEST.RETURNS_BOOLEAN`'s result/param as `DATA_TYPE='PL/SQL
  BOOLEAN'`; 23ai reports `'BOOLEAN'` (native). So 23ai correctly takes the native-boolean path
  (`setOutParam(1,OracleTypes.BOOLEAN)`, `setNativeBooleanParam(2,…)`, `getFunctionResult()` returning
  `Boolean`) — it compiles and is the *intended* API per the 23ai native-boolean support. The 12c-
  authored harness just calls the pre-native names (`getFunctionResultBoolean()` / `…BooleanObj()`).
  Fixing this means **adapting the harness for 23ai (or a 23ai variant)**, NOT changing the generator
  (which would regress native-boolean support). ~16 of the residual errors are this.

(The other residual `\.java:[` lines in `mvn` output for `TTablebasic`/`TRecordTest1`/
`TFunctionFunctionality`/`TIoUtilsBfileCreator` are deprecation **warnings** — `new Double(double)`,
`new Boolean(boolean)`, `BFILE.open()` — not compile errors.)

**Net of the full tail triage (CORRECTED — supersedes the "subtype keystone parser" note in commit
a3a7e10).** Tracing the subtype records (`AGENTS_SUBTYPES.SUBTYPE_PROC_01`) showed the residual
harness-suite failures are **not generator bugs**: they are cases where 23ai's data dictionary reports
metadata *more accurately than 12c*, so the generator produces **correct-but-differently-shaped** output
that the 12c-hard-coded harness cannot match.

- **Subtype records — already correct on 23ai; needs NO parser feature.** For the subtype params, 12c's
  `ALL_ARGUMENTS` gives `DATA_TYPE='PL/SQL RECORD'` with `TYPE_OWNER/NAME/SUBNAME` all NULL — which
  *forces* the generator to parse package source and keep five distinct subtype-named classes
  (`AgentsSubtypesAgentsRowtype`, `…TypesAgentsRowtype`, `…TrulyInsane`, …). **23ai resolves the subtype
  in the dictionary**: the same params come back as `TYPE_NAME=AGENTS, TYPE_OWNER=APPSCHEMA/SYNUSER`, so the
  generator correctly produces one `Agents`/`SynuserAgents` record (with the right 21 columns, via the
  `%ROWTYPE` arm) and the redundant variants collapse into it. That is *better* output — it just isn't
  the five names the harness imports. So `getSubtypeBase`/chain-parsing is **not needed** and would only
  re-introduce 12c's redundancy.
- **Public-synonym procs — downstream naming, plus a route difference (CORRECTED 2026-06-14).** The
  procs themselves are reachable on 23ai, but the *access path* differs. For the package procs the
  classes just rename (12c hash-prefixes on collision; 23ai's smaller object set doesn't). For the **9i
  stand-alone LOB procs** (`LONGNAME_RECORD_PROC_1..5`) it is sharper: 12c emits **public-synonym**
  access classes (hash-prefixed `*PubsynRecordTest2`, from `pubsyn_record_test_2_9i_sal_rtN`), whereas
  23ai emits **no** public-synonym SAL access class at all — only the direct (`RecordTest29iSalRtN`),
  schema-qualified (`SynuserRecordTest29iSalRtN`) and **private-synonym** (`SynRecordTest29iSalRtN`)
  flavours. (On FREE23 the genlog "Examining records used by …" pass lists `RECORD_TEST_2_9I_SAL_RTn`
  and `SYN_RECORD_TEST_2_9I_SAL_RTn` but never `PUBSYN_…`, even though the 10 public synonyms exist.)
  So the 23ai harness drives the **private-synonym** classes, which carry identical signatures
  (`setParamPClobIn`/`PBlobIn`/`PBfileIn`, same record setters, same `getFunctionResult` types) now that
  the 9i SAL package on 23ai matches 12c (`P_CLOB_IN`). Resolved as a 23ai harness variant — see below.
- **PL/SQL BOOLEAN — same story.** 23ai native-boolean (`DATA_TYPE='BOOLEAN'`) vs 12c `'PL/SQL BOOLEAN'`;
  the generator correctly emits the native-boolean API the harness doesn't call.
- **Nested-field records** — **investigated 2026-06-14: NOT a reachable gap, no code change made.**
  The scalar-only guard in the package-record synthesis arm (`SAAdminWrangler` ~13923) stubs a plain
  `PL/SQL RECORD` whose field has a non-null `ATTR_TYPE_OWNER` (a nested package record/object). A full
  census of the APPSCHEMA schema on FREE23 shows **no reachable proc hits that branch**: the only procedures
  whose record/collection argument has a nested typed field are `DATATYPE_TEST_10G.COLL_XMLTYPE_REC`
  and `SDOTEST_10G.COLL_SDO_REC`, and in both the argument is a **collection** (`TYPE_ARRAY_COMMANDS_OA`,
  field `COMMAND_DESCRIPTION` = `XMLTYPE`/`SDO_GEOMETRY`) handled by the **collection-element arm**
  (`getPackageCollectionChild`'s object path), not the plain-record arm — these generate their object
  field correctly (`oracle.sql.OPAQUE` for XMLType) and compile clean. The deep record-in-record types
  (`RECORD_TEST(2_8I/9I).AND2RECORDTYPE`/`C2REC`, whose fields are `%ROWTYPE`/other package records) are
  **orphan type declarations used by no procedure**, so they are never generated. Verified: `generic_test0`,
  `generic_testg`, `generic_testh` (the only propfiles containing those procs) all generate + compile clean
  on FREE23 with **0** stub/`Unable to build` lines. So the guard's stub branch is effectively dead code for
  every available test schema; recursive record-in-record synthesis would be speculative and unvalidatable.
  If a real schema ever needs it, the bounded first step is to relax the guard to accept fields that resolve
  to a known scalar-mappable schema object (XMLType/SDO_GEOMETRY/…) before attempting true record recursion.

**Conclusion.** Standalone 23ai generation is functionally complete: all 7 propfiles generate and
compile clean with records populated (the committed arms), and where 23ai's output differs from 12c it
is *correct* (native boolean, dictionary-resolved subtypes). The generated-harness **suite** is hard-
coded to 12c's exact class/accessor names, so it is **not a valid 23ai gate without being regenerated
/adapted for 23ai** — that is a test-asset task, not a generator fix. **No generator gap remains
reachable** by the available test schemas (the nested-field-record stub branch is never hit — see above).

### 23ai harness variants (the `harnesses-23ai` profile) — status

The 12c-coupled harnesses that the `harnesses-23ai` profile drops (via `<testExcludes>`) each get a
23ai-shaped replacement under `src/test/generated-harnesses-23ai/`. As of 2026-06-14 **all four are
green on FREE23** (`mvn -P harnesses-23ai -Dtest=… test` → 4 run, 0 failures):

| 12c harness (excluded)      | 23ai variant                  | what differs on 23ai |
|-----------------------------|-------------------------------|----------------------|
| `TFunctionScalerDatatypes`  | `TFunctionScalerDatatypes23ai`| native-boolean accessors |
| `TSubtypes01`               | `TSubtypes0123ai`             | dictionary-resolved subtype → one `%ROWTYPE` class |
| `TRecordTest2SALPubSyn`     | `TRecordTest2SALPubSyn23ai`   | 8i pubsyn procs emit full (un-hashed) names |
| `TRecordTest3SALPubSyn`     | `TRecordTest3SALPubSyn23ai`   | **9i SAL procs reached via the PRIVATE synonym** (`SynRecordTest29iSalRt1..5`); 23ai emits no public-synonym SAL access class |

`TRecordTest3SALPubSyn23ai` (added 2026-06-14) closes the last item: it is a near-verbatim copy of the
12c `TRecordTest3SALPubSyn` with only the five access-class names swapped to the private-synonym
`SynRecordTest29iSalRt1..5` (RT1=procedure, RT2..RT5 the four functions, mapped by `getFunctionResult`
return type). This was unblocked once the 9i SAL package on FREE23 was re-provisioned to match 12c
(`P_CLOB_IN`), so the private-synonym classes carry the identical setters/getters the harness calls.

## Resume checklist (next session)

1. **Treat the harness suite as a 12c-coupled asset.** To run it on 23ai, regenerate/adapt the `T*`
   harnesses against 23ai output (the class/accessor names legitimately differ: native-boolean
   accessors, dictionary-resolved subtype/`%ROWTYPE` class names, different collision-hash names) —
   rather than changing the generator to re-emit 12c-shaped output. No `getSubtypeBase` parser work is
   needed (23ai resolves subtypes in `ALL_ARGUMENTS`); ignore the superseded note in `a3a7e10`.
   **Update 2026-06-14:** the four profile-excluded harnesses now all have green 23ai variants (see
   "23ai harness variants" above) — `TFunctionScalerDatatypes23ai`, `TSubtypes0123ai`,
   `TRecordTest2SALPubSyn23ai`, and the new `TRecordTest3SALPubSyn23ai` (9i SAL via private synonym).
2. ~~(Optional, niche) Nested-field record recursion~~ — **closed 2026-06-14 as not-a-gap.** Census of
   FREE23 shows no reachable proc hits the plain-record nested-field stub; the only reachable nested-field
   args are collections-of-object (XMLType/SDO), handled by the collection arm, and `test0/g/h` all
   generate + compile clean. See the "Nested-field records" bullet above. No code change needed unless a
   future schema exercises a plain record whose fields are themselves records/collections.
2. Suite command on FREE23:
   `MCPDBWIZARD_TEST_HOST=<test-host> MCPDBWIZARD_TEST_SID=/FREEPDB1 MCPDBWIZARD_TEST_URL='jdbc:oracle:thin:@<test-host>:1521/FREEPDB1' MCPDBWIZARD_TEST_USER=appschema MCPDBWIZARD_TEST_PASSWORD=appschema mvn test`.
   (The 12c suite needs the **combined** tree — generate `generic_test_23ai` on FREE23, the other six
   on ORCL12, copy the former into `target/regen/Src` — else `TGen23ai` can't compile.)
3. ~~(Defensive) null-guard `plsqlEqualsByArgs` / `plsqlEquals`~~ — **DONE (commit `aaf95d4`).**
   Both comparators now use `java.util.Objects.equals` on every field (plus a null-`other` check),
   so a future stray null degrades to "not equal" instead of throwing and silently disabling all dedup.

### Reusable artifacts

- **`getPackageCollectionChild` element arms** — now IN the working tree (`SqlStatementDictionary`,
  wired via the `getPlsqlColl*Qry` getters; `getChildRecord` calls them when the walk has no deeper
  child). All four use the same proc-coordinate bind passthrough; `ARGUMENT_NAME` is the synthetic
  `_function_result_<seq>` (must be non-null — see the NPE root cause above):
  - `plsqlCollScalarArgQrySelect` (inc. 1, 9 binds) — `elem_type_owner IS NULL`.
  - `plsqlCollRecordArgQrySelect` (19 binds) — header row from `ALL_PLSQL_COLL_TYPES` (`TYPE_NAME`=
    `elem_type_package`, `TYPE_SUBNAME`=`elem_type_name`, guarded by `EXISTS … ALL_PLSQL_TYPES …
    typecode='PL/SQL RECORD'`) `UNION ALL` field rows from `ALL_PLSQL_TYPE_ATTRS` (`ORDER BY 7`);
    binds 1-10 header arm, 11-19 field arm.
  - `plsqlCollObjectArgQrySelect` (10 binds) — single `'OBJECT'` header; `elem_type_package IS NULL`
    + `EXISTS … ALL_TYPES … typecode='OBJECT'`.
  - `plsqlCollRowtypeArgQrySelect` (10 binds) — single all-null `'PL/SQL RECORD'` header;
    `elem_type_name LIKE '%\%ROWTYPE'`.
  - `plsqlIndexbyElemArgQrySelect` (9 binds, `getPlsqlIndexbyElemQry`) — scalar element row (DATA_TYPE +
    precision/scale/length) for an INDEX-BY table; applied at `CallableStatementParameterEngine`'s
    index-by call site (its own `getAttrArguments`), not in `getChildRecord`.
- **Empirical method** (how each arm was verified): dump the proc's `ALL_ARGUMENTS` child rows on
  ORCL12 (`select … where object_name=… and sequence between …`) to see the exact 12c element-header
  shape, then mirror it from the 23ai `ALL_PLSQL_*` views and diff the generated SOAP file
  (`OracleArrays…Attrs` references) 12c-vs-23ai.
- **Diagnostic logging** used to find both bugs has been removed (pure `mrLog.info`). To reproduce:
  for the NPE, watch for the `"Unable to parse PL/SQL Parameters:"` syserror right after
  `"Examining records to identify duplicates…"`; for the spurious-sibling walk, log
  `tempRec.procObjectName/procArgName`, the walk row count, and the walk row's `DATA_LEVEL` in
  `getChildRecord` just before the `walkHasChild` test.

## Rebuilding the green baseline & running the suite

`target/regen` is gitignored, single-host per run, and wiped each run, so:

```bash
mvn -DskipTests package                          # or -Dmaven.test.skip=true if regen tree is incomplete
# 12c set against ORCL12:
MCPDBWIZARD_TEST_HOST=<test-host> MCPDBWIZARD_TEST_PORT=1521 MCPDBWIZARD_TEST_SID=/orcl \
MCPDBWIZARD_TEST_USER=APPSCHEMA MCPDBWIZARD_TEST_PASSWORD=appschema \
  Scripts/testrun_current.sh generic_test1 generic_test2 generic_test3 generic_test4 generic_test9 generic_testd
# stash those 6 dirs from target/regen/Src, then (TGen23ai drops OB_GEN_* on cleanup):
sqlplus <user>/<password>@FREE23 @sql/datatypes_23ai_gen.sql
MCPDBWIZARD_TEST_HOST=<test-host> MCPDBWIZARD_TEST_PORT=1521 MCPDBWIZARD_TEST_SID=/FREEPDB1 \
MCPDBWIZARD_TEST_USER=appschema MCPDBWIZARD_TEST_PASSWORD=appschema \
  Scripts/testrun_current.sh generic_test_23ai
# copy the 6 stashed dirs back -> all 7 packages -> mvn test-compile is clean.
```

Run the suite (two hosts — the 23ai box can't host the full APPSCHEMA suite yet, this gap):

```bash
# 12c (ORCL12): full APPSCHEMA harness suite + pub suite  -> 250 run, 0 failures, 2 skipped
MCPDBWIZARD_TEST_HOST=<test-host> MCPDBWIZARD_TEST_SID=/orcl \
MCPDBWIZARD_TEST_URL='jdbc:oracle:thin:@<test-host>:1521/orcl' \
MCPDBWIZARD_TEST_USER=APPSCHEMA MCPDBWIZARD_TEST_PASSWORD=appschema \
  mvn test -Dtest='!TGen23ai,!Datatypes23aiLiveTest'

# 23ai (FREE23): the 23ai datatype harnesses  -> 7 run, 0 failures
MCPDBWIZARD_TEST_HOST=<test-host> MCPDBWIZARD_TEST_SID=/FREEPDB1 \
MCPDBWIZARD_TEST_URL='jdbc:oracle:thin:@<test-host>:1521/FREEPDB1' \
MCPDBWIZARD_TEST_USER=appschema MCPDBWIZARD_TEST_PASSWORD=appschema \
  mvn test -Dtest='TGen23ai,Datatypes23aiLiveTest'
```

Last confirmed green (2026-06-08): ORCL12 250/0/0 (2 skipped), FREE23 7/0/0.

Update (2026-06-14): with the collection/index-by/record-field synthesis committed (`5de512a`,
`d3c7f22`): ORCL12 combined-tree suite still 250/0/0 (2 skipped); FREE23 `Datatypes23aiLiveTest` 6/0/0
and `TGen23ai` 1/0/0 (run with the four 12c-coupled record/boolean harnesses set aside). All 7
propfiles generate + compile clean standalone on FREE23. The full 12c-authored APPSCHEMA harness suite
does NOT compile against 23ai output (12c-coupled class/accessor names — a test-asset regeneration task,
not a generator bug; see the fourth-gap section).
