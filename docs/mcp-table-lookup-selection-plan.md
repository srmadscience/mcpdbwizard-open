# Selecting a table's index / unique-key / FK lookups — plan

**Status: PARKED 2026-08-10, nothing implemented.** Written against `8f2b553`.

**Why parked.** The question that prompted this — exposing "read by index X" as an MCP tool — turned
out to be already done: `ob_gen_23ai_ix_category` is emitted, drives the DAO's
`getChildByIxIxCategory(...)`, takes the index columns as its input schema and is marked
`readOnlyHint(true)`. What made it look missing is that the table in play, `SYNUSER.EVENT_LOG`,
has exactly one index — `ALOG_PK`, the primary key's own — and no unique or foreign keys, so its
four CRUD tools really are the complete set.

What remains genuinely open is the narrower thing below: you cannot choose WHICH lookups a table
exposes, only all of them or none, via the `R` letter. That is worth doing when a schema in use has
indexes an author wants to withhold. It is not worth doing before then.

Goal, as stated: for tables you must be able to **specify lookup by index** the way the DAO supports
it, and each such lookup needs **description support**.

---

## 1. What is already true (verified, not assumed)

Worth stating first, because two thirds of this already works and the plan is smaller than it sounds.

- **Indexes are always discovered.** `SAAdminWrangler` passes `allIndices = true` to
  `generateTables` (a hard-coded literal, not a config flag), so `getAllConstraintsQuery3` returns
  indexes as well as constraints for every table, on every config.
- **Index lookups are already emitted as MCP tools.** The lookup loop captures `U` (unique key) and
  `INDEX` rows onto `SingleNamespaceObject.mcpTableLookups`, and FK children separately, and each
  becomes its own tool. A unique index returns a single row, a non-unique one an array.
- **Descriptions per lookup already exist**, keyed `UK_<name>` / `IX_<name>` / `FK_<name>`, stored
  in `TABLE_MCP_DESC_<i>_<op>`, emitted through `mcpToolDescriptionLine`, and listed in the web
  editor from `getTableLookupInventory`.

**So "description support" is done.** What is missing is *selection*.

## 2. The actual gap

A lookup is emitted if and only if the table carries the **`R`** letter:

```java
// A secondary lookup is a read of this table, so curation that withholds "R"
// withholds the lookups with it.
if (theTable.mcpTableLookups != null && info.readable) {
```

`R` is one bit covering `get_by_pk` **and every** unique-key, index and FK-child lookup. There is no
way to say "expose the lookup on `IX_ORDER_DATE` but not `IX_INTERNAL_SCRATCH`". A table with a
dozen indexes contributes a dozen tools or none.

That matters for the same reason `TABLE_MCP_CRUD_<i>` exists at all. The project's rule (recorded in
`app/CLAUDE.md`) is that a per-object flag is worth adding **only where one config entry yields
several tools** — one entry, one tool means omitting the entry is already the control. A table entry
yielding a dozen lookups is exactly the case the rule admits, and it is the last place in the MCP
surface where it still applies.

It also has a cost beyond tidiness: every exposed tool's name, description and schema is sent to the
model on **every `tools/list`**. Indexes that exist for the optimiser's benefit rather than as
meaningful access paths are pure token cost, and they invite an agent to query by a column nobody
meant to expose.

## 3. Design

### 3.1 Key

Follow `TABLE_MCP_CRUD_<i>` and the description keys, which already agree on the operation
vocabulary:

```
TABLE_MCP_LOOKUPS_<i> = UK_EMP_CODE,IX_EMP_DEPT,FK_EMP_MGR
```

- **Key absent = every lookup, as today.** This is what keeps existing configs byte-identical and is
  the same contract `TABLE_MCP_CRUD_<i>` uses for its absence.
- **Present-but-empty = no lookups**, while `get_by_pk` still follows `R`. Distinct from absent, as
  everywhere else in this family.
- Names are the operation keys the descriptions already use, so an author never learns two
  vocabularies and the editor can show selection and description on one row.

**Rejected: a letter per lookup**, as `CRUD` does. Lookups are named, not enumerable, so there is no
fixed alphabet to assign letters from.

**Rejected: `TABLE_MCP_LOOKUP_<i>_<name>=YES/NO` one key per lookup.** It reads more like the rest of
the `.pb2`, but a table with twenty indexes then contributes twenty keys, and a de-selected lookup
becomes indistinguishable from one whose index was dropped.

### 3.2 `R` still governs

`R` stays the master switch: no `R`, no reads at all, list or no list. A lookup list without `R` is
contradictory and should be treated as "no lookups" rather than silently re-enabling reads. Worth an
explicit test, because the two settings are edited on different screens.

### 3.3 What happens to a description for a de-selected lookup

**Keep it.** Same as a de-selected table's description: the text is the author's work and a lookup
may be re-selected next week. It is inert while de-selected, exactly as a description for a lookup
whose key columns are not JSON-crossable already is. Do **not** garbage-collect on save.

### 3.4 The editor

The lookup rows already exist. Each gains a checkbox, defaulting to ticked when the key is absent,
and the description box is disabled (not hidden) while unticked — hiding it would look like the text
had been lost. Same `mcpCrud` precedent: carried keyed on owner/name, never on the config index.

## 4. The part I would not change without a decision

**The emitted tool name for a lookup is `<table>_<constraintName>`** —
`lookupToolName = toolBase + "_" + lookup.toolNameBasis.toLowerCase()...` — giving
`ob_gen_23ai_ix_category` and `ob_gen_23ai_uk_code`. It does not say it is a lookup, and an agent
reading a tool list sees a name whose shape depends on somebody's index-naming convention. Something
like `ob_gen_23ai_get_by_ix_category` would read better.

**But renaming is a breaking change** for any client with a saved tool name, and it is orthogonal to
selection. Left alone here deliberately; raise it separately if it is wanted.

## 5. Phases

1. **Storage** — `Table.mcpLookups` (a `List<String>` or null), `TABLE_MCP_LOOKUPS_<i>`, round-trip
   through `.pb2`/`.json`, `extraProperties` empty. No behaviour.
2. **Emission** — filter the lookup loop by the list when present. Byte-identical when absent.
3. **Swing data model** — `TableTableDataModel` read/write/carry, keyed owner/name, as `mcpCrud` is.
   No Swing editor, but it must not destroy the key.
4. **Web** — checkboxes on the existing lookup rows; preserve across `applyTableSelection`, which
   rebuilds every `Table` (the phase-3a trap).
5. **Live** — a `TGen23aiMcp` leg: with `FIXTURE_TABLE` restricted to `UK_CODE`, `tools/list` must
   contain the unique-key tool and **not** the index one, while `get_by_pk` survives.

## 6. Tests

- db-free: absent vs empty vs listed; an unknown name in the list is ignored rather than fatal (an
  index can be dropped from the database between saves); `extraProperties` empty.
- Emission: byte-identical with the key absent, proven by the worktree diff method.
- The `R`-off-with-a-list case (§3.2).
- Web: selection survives a table-selection save; a de-selected lookup keeps its description.
- Estate: one six-box run, because the lookup loop runs for every table on every propfile.

## 7. Open questions

1. **Does "specify lookup by index" mean selection, or something the DAO does that MCP does not?**
   This plan assumes selection, because indexes are already discovered and already emitted. If the
   real gap is a *shape* the DAO supports and MCP omits — an ORDER BY, a range, a partial-key
   lookup — that is a different piece of work and this plan is the wrong one.
2. Should a **non-unique index** default to off? It returns an array and is the most likely source of
   an accidentally huge result. Defaulting all-on keeps existing configs unchanged, which argues for
   leaving it and letting an author curate.
3. §4 — rename the lookup tools, separately?
