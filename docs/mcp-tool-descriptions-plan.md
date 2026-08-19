# Author-supplied MCP tool descriptions — plan

**Status: IMPLEMENTED — all six phases, plus one thing this plan did not anticipate.** Written
2026-08-10 against `2d6c703`; shipped over 2026-08-10/11 and verified on all six Oracle boxes at
`7f3ed7f`. The status line below used to read "proposed, not implemented" and stayed that way
through the entire build, which is how a finished feature came to look like available work — check
the code before believing a status line in this directory, including this one.

| §9 phase | State |
|---|---|
| 1. Storage + round-trip | DONE — `SEQUENCE_MCP_DESC_<i>`, `PROC_MCP_DESC_<i>`, `SQL_MCP_DESC_<i>`, `TABLE_MCP_DESC_<i>_<OP>`; `.pb2` ↔ `.json` proven identical on all five surfaces |
| 2. Emission funnel | DONE — `SAAdminWrangler.mcpToolDescriptionLine`, **one** `.description(` print site reached by 13 callers, so a new tool surface cannot bypass it |
| 3. Web editor | DONE — `DescriptionController` + `design/descriptions` |
| 4. Lookup inventory (§3 **option A**) | DONE — `getTableLookupInventory` via `DesignSession.listTableLookups`; UK / index / FK rows |
| 5. Duality-view doc operations | DONE — `getDualityViewOperations`; a view gets its `DOC_*` rows *instead of* the four table rows, since it has no row CRUD |
| 6. §7 honest defaults | DONE — `mcpParamTypeLabel`; defaults name ORACLE types, not Java ones |

**§10 open questions, all settled:** (1) inventory source — **A**; (2) duality views — **included**;
(3) length cap — **1024 characters**, `DescriptionController.MAX_LENGTH`, enforced as a hard reject
with newlines and control characters refused too; (4) empty override — **allowed**, it is legal MCP
and a legitimate choice.

**What the plan did not anticipate, added 2026-08-11 in `ed71a45`.** A description written against an
object the crossability gate rejects was accepted, stored, round-tripped — and dropped at emission in
silence. This plan reasons only about tools that exist; nothing here asks what the editor should do
about a config entry that yields none, and the editor duly offered a box for every one. The generator
now emits a machine-readable `MCP-UNEXPOSED` line and the editor marks those rows.

**Note that this partly adopts §3's REJECTED option B, deliberately.** B was rejected as the source
of the *inventory* because it inverts the workflow — you would have to generate before you could
describe. That objection is decisive for "which tools exist" and does not apply to "why this one does
not": the reason is only computable during generation, so there is nothing to invert. The two
coexist — the inventory is live from the dictionary (A), the refusals come from the last generation
(B) — and the editor says plainly when a config has never been generated rather than letting silence
read as approval.

Goal, as stated: a description per MCP tool, stored in the config (`.pb2` / `.json`), editable in
the **web** UI by clicking a hyperlinked *description* field, and used as the emitted tool
description. The Swing UI does not need an editor.

---

## 1. What exists today

Descriptions are **generated from the dictionary at emission time**. There are exactly **13
`.description(...)` sites** in `SAAdminWrangler`, and — the useful part — **every one of them sits
immediately after a `mcpToolNameLine(...)` call**. That funnel already carries
`{toolName, dbObject, objectType}` and records it onto `mcpMetricDescriptions` for the Prometheus
`db_object` label. So a single, already-existing choke point knows the identity of every tool at the
moment its description is written. **This work should ride that funnel rather than touch 13 sites**,
for the same reason the metrics work did: a new tool surface must not be able to arrive
undescribed.

Current quality varies, and it is worth fixing while here (§7): table and duality-view descriptions
name Oracle types (`id (NUMBER)`, `doc (JSON)`), while PL/SQL ones leak **Java** types into a
JSON-facing contract — `p_raw (byte[])`, `p_date (java.util.Date)`,
`p_out (oracle.sql.json.OracleJsonValue)`. A caller is not told that `byte[]` means base64.

## 2. The tool inventory, which is not the object inventory

This is the crux, and it is why the task is non-trivial.

| Config entry | Tools it yields |
|---|---|
| Sequence | 1 (`_nextval`) |
| SQL statement | 1 |
| Procedure/function | 1 (per overload — overloads are distinct entries already) |
| **Table** | **4 fixed** (`get_by_pk`, `insert`, `update`, `delete`) **+ one per unique key, per index, and per FK child** |
| Duality view | 5 (`doc_get_all/_get_by_id/_insert/_update/_delete`, per ALLOW flags) |

Three consequences:

1. A table needs a **map** of descriptions, not one string.
2. **The secondary lookups are discovered during generation, not at config time.** They come from
   `SAAdminWrangler`'s constraint loop and are captured onto `SingleNamespaceObject.mcpTableLookups`
   as `TableMcpInfo.Lookup{toolNameBasis, kind, single, childRowClass, …}`.
3. **The web module has no constraint awareness whatsoever** — verified: no reference to
   constraints, `ALL_CONS_*` or foreign keys anywhere in `web/src/main/java`. `DesignSession` lists
   sequences, tables and procedures through `SAAdminWrangler.getSequenceData/getTableData/
   getFunctionData` and nothing else.

**So the requirement "one description per FK" cannot be met by the web UI as it stands.** It can
offer descriptions for everything whose existence it already knows (sequences, statements,
procedures, and a table's four fixed operations) but it cannot enumerate a table's unique keys,
indexes or FK children. That gap needs a decision — §3.

## 3. Decision required: where the lookup inventory comes from

| | Option | Assessment |
|---|---|---|
| **A** | **Web queries the dictionary for constraints/indexes** when opening a table's description editor | **RECOMMENDED.** `DesignSession` already holds a live `SAAdminWrangler`, so this is one more method on the same object against the same connection, and the queries exist in `SqlStatementDictionary`. The inventory is then live and correct before any generation has run. Cost: the lookup-naming rule (`toolNameBasis`) must be reachable from both sides — extract it, do not copy it. |
| B | **Generator writes back a tool manifest** it discovered, which the UI then annotates | Reuses `mcpMetricDescriptions`, which already holds every tool name. But it inverts the workflow: you must generate before you can describe, and a config for a schema you have not generated yet has no editable list. Also puts generator output into the input file. |
| C | **Scope v1 to the fixed operations**, add lookups later | Ships sooner and is honest, but leaves "each FK" — explicitly asked for — undone, and the UI would silently offer fewer tools than the server exposes. |

**Recommendation: A**, with C as the fallback if the constraint queries prove expensive on a wide
schema. Whichever is chosen, the *storage* design below is unaffected — only who populates the list.

## 4. Storage

### 4.1 Keys

Follow the existing indexed families (`TABLE_NAME_<i>`, `PROC_NAME_<i>`, `SEQUENCE_NAME_<i>`,
`SQL_FILENAME_<i>`) and the `TABLE_MCP_CRUD_<i>` precedent:

```
SEQUENCE_MCP_DESC_<i>
PROC_MCP_DESC_<i>
SQL_MCP_DESC_<i>
TABLE_MCP_DESC_<i>_PK | _INS | _UPD | _DEL
TABLE_MCP_DESC_<i>_UK_<CONSTRAINT>
TABLE_MCP_DESC_<i>_IX_<INDEX>
TABLE_MCP_DESC_<i>_FK_<CONSTRAINT>
VIEW_MCP_DESC_<i>_DOC_GET_ALL | _DOC_GET_BY_ID | _DOC_INS | _DOC_UPD | _DOC_DEL
```

**Absent means "use the generated default".** An empty value means an empty description, which is
different and must stay different — the same present-but-empty vs absent contract `Schema` already
keeps for every scalar. Configs written before this exist are byte-identical after a load and save.

**Duality views share the `TABLE_` family today** (a DV is selected as a table), so `VIEW_MCP_DESC_`
would be a new family keyed on the same index space. Simpler alternative: fold them into
`TABLE_MCP_DESC_<i>_DOC_*`. Prefer that — one family, one index space, no ambiguity about which
list an index refers to.

### 4.2 Typed model

`Table` gains `Map<String,String> mcpDescriptions` (operation → text); `Sequence`, `Procedure`,
`SqlStatement` gain a `String mcpDescription`. `Schema.toPb2`/`fromProperties`/`toMap`/`fromMap`
extend as `mcpCrud` already does.

**The `extraProperties` contract is the trap.** `Schema` keeps every unrecognised key verbatim, so a
new key family that the parser does not recognise round-trips *through `extraProperties`* and looks
like it works while nothing in the application can read it. `MetricsPortKeyTest` was written for
exactly this failure mode; the equivalent test here must assert `extraProperties` is **empty** after
loading a config that uses the new keys.

### 4.3 Index instability — the known trap

`app/CLAUDE.md` records that `TableTableDataModel` carries `mcpCrud` **keyed on owner/name, because
the index is reassigned on every save**. Descriptions must do the same in the web session model, or
deleting one table silently re-points another's descriptions.

Worse in Swing, which does **not** need an editor but must not be a destroyer: `ApplicationShell`
keeps the loaded `Properties` in `fileProps` and mutates it, so unknown keys **survive** a Swing
save — good. But if a table is removed in Swing the indices shift while the description keys do not,
so a stale `TABLE_MCP_DESC_3_INS` can reattach to a different table. **This must be tested
explicitly**: load a config with descriptions in Swing, delete a table, save, and assert every
surviving description still belongs to the table it was written for. If it does not, Swing needs a
small re-keying pass on save — which is the one Swing change this work may require.

## 5. Emission

Add a sibling to the existing funnel:

```java
mcpToolDescriptionLine(theJavaCode, theToolName, theGeneratedDefault, theConfiguredOverride);
```

It prints `.description("…")` using the override when present, else the generated default, and — the
point of a funnel — **cannot be bypassed**, so a new tool surface arrives with a description and an
override hook at once. All 13 sites already have the tool identity in scope from the `mcpToolNameLine`
call one line above.

**Escaping is not optional.** The text becomes a Java string literal in emitted source *and* crosses
MCP as JSON. `SAAdminWrangler.javaStringLiteral` already escapes quotes and backslashes (it was added
for `MCPDBWIZARD_LOG_BACKEND`); it does **not** handle newlines or control characters, which a
textarea will happily produce. Either extend it or reject them at the form. A stray newline in an
emitted literal is a compile error across an entire tree — loud, but a poor way to find out.

**Emitted output changes for any config that sets a description**, and only for those; a config
with none is byte-identical. That property is worth asserting, because it is what makes this safe to
land: the byte-identity method in §8 proves it directly.

## 6. Web UI

On the Design object list, each selected object gains a hyperlinked **description** cell showing the
current text (or *"(generated)"* when absent). Clicking opens an editor — a modal or a small
per-object page — with:

- the **generated default shown read-only**, so an author can see what they are replacing;
- a textarea for the override, empty = use the default;
- a **Reset to generated** action, distinct from clearing to empty (§4.1);
- for tables, one row per operation, with the four fixed ones always present and the lookups listed
  per §3.

`ConfigController.save` already carries the working `Schema`; descriptions ride it unchanged. The
`.warning` style added for empty configs is the right vehicle for "this config has descriptions for
objects it no longer selects".

## 7. Fold in: make the generated defaults honest

The PL/SQL defaults should name **Oracle** types and crossing formats, as the table ones do —
`p_raw (RAW, base64)`, `p_date (DATE, ISO-8601)` — not `byte[]` / `java.util.Date` /
`oracle.sql.json.OracleJsonValue`. `getMcpParamTuples()` already carries each parameter's Oracle
datatype constant and (since bug D) its raw type name, so the information is in hand. This changes
emitted output for every `MCP_SERVER=YES` config, so it wants its own commit and its own
byte-identity check, separate from the storage work.

## 8. Tests and verification

- **db-free:** `Schema` round-trip for every new key family, `extraProperties` empty (§4.2), absent
  vs present-but-empty, and the escaping of quotes/backslashes/newlines.
- **Web:** the editor writes what was typed; *Reset* restores the default; the owner/name keying
  survives a table deletion (§4.3).
- **Live:** `TGen23aiMcp` gains one leg — set a description on one tool in `generic_test_23ai`,
  regenerate, and assert `tools/list` returns exactly that text for that tool and the generated
  default for its neighbour.
- **Byte-identity:** generate the canonical set with no descriptions set, before and after the
  change, and diff normalising only the per-run stamp — the method used at `2d6c703`, which proved
  7838 files identical. It is the direct proof of the claim in §5.
- **Estate:** one full six-box run, because the funnel change touches every emitted server.

## 9. Phases

1. Storage + round-trip (`Schema`, `Table`, `Sequence`, `Procedure`, `SqlStatement`) — no behaviour.
2. Emission funnel + override plumbing; byte-identical with no descriptions set.
3. Web editor for the objects the UI already knows (sequences, statements, procedures, the four
   fixed table operations).
4. Lookup inventory per §3 (option A), extending the editor to UK/index/FK.
5. Duality-view doc operations.
6. §7, separately.

Phases 1–3 are useful on their own; 4 is where the "each FK" requirement is actually met.

## 10. Open questions

1. **§3** — which inventory source. Blocks phase 4, not 1–3.
2. **Duality views** were not mentioned in the request. Include them (§9 phase 5) or leave generated?
3. **Length cap?** MCP does not impose one, but a description is sent to a model on every
   `tools/list`. A soft warning above, say, 1024 characters seems right; a hard limit does not.
4. **Should an override be allowed to be empty?** An empty description is legal MCP and a legitimate
   choice, but it is indistinguishable from a mistake. Recommend allowing it with a warning.
