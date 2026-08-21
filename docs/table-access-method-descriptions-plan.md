# Per-access-method table descriptions

> **THE FEATURE ASKED FOR ALREADY EXISTS, end to end, and has since `095eb6f` (2026-08-10).**
> A table is already not one description: it stores a **map** keyed by operation, the web editor
> already offers one box per access method — `get_by_pk`, `insert`, `update`, `delete`, plus one
> per unique key, index and foreign-key child — and all seven shapes are already consumed at
> emission. This document exists because **checking that turned up a real defect in the same
> area**, which is what the work below is.

## What is already built (verified, not remembered)

| Layer | Where | State |
|---|---|---|
| Storage | `schema/Table.java` — `OP_GET_BY_PK` `PK`, `OP_INSERT` `INS`, `OP_UPDATE` `UPD`, `OP_DELETE` `DEL`, `OP_UNIQUE_KEY_PREFIX` `UK_`, `OP_INDEX_PREFIX` `IX_`, `OP_FOREIGN_KEY_PREFIX` `FK_` | done |
| Config keys | `TABLE_MCP_DESC_<i>_<OP>` in PB2, `mcpDescriptions` map in JSON | done |
| Inventory | `SAAdminWrangler.getTableLookupInventory` → `DesignSession.listTableLookups` | done |
| Web editor | `DescriptionController.rowsFor`, one row per operation | done |
| Emission | four fixed sites at `SAAdminWrangler` 8102/8135/8169/8204; the lookup site at **8283** | done |

**The keys match, which is the thing most likely to have been wrong.** The editor writes
`"UK_" + CONSTRAINT_NAME`; the emitter reads `lookup.kind.toUpperCase() + "_" + lookup.toolNameBasis`,
and `toolNameBasis` is assigned the raw `constraintName` (9949) / `childFKConstraintName` (11757).
A mismatch here would not fail — the override would simply never be used — so it was checked
rather than assumed.

Evidence from real generated output, not from reading the emitter:

```
.description("Look up rows in table FLIGHTS by index FLT_IX3 (airline_name (VARCHAR2), flight_number (NUMBER)). ...")
.description("Look up the row in table GROUPS by unique key GRPS_FK01 (group_name (VARCHAR2), group_type_name (VARCHAR2)). ...")
.description("Look up the child rows referencing table FLIGHTS by foreign key BKG_FLT (...). ...")
```

**So there is nothing to build for "a description per method of access."** If the impression came
from the UI, see gap 3 — that is a fair thing to have concluded from what the page shows.

---

## Gap 1 — an FK child-lookup description never names the table the rows come from

> **DONE 2026-08-21.** `mcpLookupDescription` extracted and unit-tested (`McpLookupDescriptionTest`,
> 7 tests); `TableMcpInfo.Lookup.childTableOracleName` carried from the FK walk. Verified by
> regenerating `generic_test1_mcp` against FREE23: **57 FK descriptions converted, 0 left in the old
> form, 0 reading "table null"**, unique-key and index wording untouched, and the tree still
> **2457** files — exactly its recorded floor, which is what a description-only change should do.
>
> The live proof is the AGENTS table, which is the GROUPS case from this document in another tree:
> five children all keyed `agent_id (NUMBER)`, previously separable only by `R_55`/`R_16`/`R_61`/
> `R_43`/`R_50`, now naming `AGENT_RESOURCES`, `GROUP_AGENT_MEMBERS`, `GROUP_EXPANSION`,
> `JOB_INVOCATIONS` and `OS_DIRECTORY`.
>
> **FREE23 estate green:** all 41 propfiles regenerated (exit 0), app **856/0/0** (2 skipped) and
> web **398/0/0**. The app total is up exactly 7 on the previously recorded 849 — the seven tests
> added here — and web is unchanged, which is the shape a description-only change should have.
>
> **Also pinned on a RUNNING server** (`TGen23aiMcp`): the fixture's `OB_GEN_PARENT` →
> `OB_GEN_CHILD` foreign key is read back off `tools/list` and asserted to name the child. That
> check is a real control, not a tautology — the pre-change tree emitted *"Look up the child rows
> referencing table OB_GEN_PARENT by foreign key FK_CHILD_PARENT"*, which does not contain
> `OB_GEN_CHILD` and would have failed it.
>
> **Why a live assertion was worth adding on top of the unit test.** The existing round-trip check
> proves an author's OVERRIDE reaches the wire — and it passes just as well when the generated
> default underneath is wrong, because an override replaces it. Nothing was looking at the
> defaults. That is how this survived ten days inside an arc recorded as complete.
>
> **Remaining:** the other five boxes. Emitted output changes for every `MCP_SERVER=YES` config
> with an FK child lookup, so the full estate is the honest verification; FREE23 is one box.

**This is a defect, not a polish item.** The emitted text (8255) is:

```java
"Look up the child rows referencing table " + info.tableOracleName
        + " by foreign key " + lookup.toolNameBasis + " (" + keySummary + ")."
```

It names the **parent** — the table you are looking up *from* — and never the **child**, which is
where the returned rows actually come from. Measured on a real generated server:

```
tool flights_bkg_flt
  "Look up the child rows referencing table FLIGHTS by foreign key BKG_FLT
   (airline_name (VARCHAR2), flight_number (NUMBER), departure_time (DATE)).
   Returns the child rows as a JSON array."
```

The rows are **BOOKINGS** rows. `BOOKINGS` appears nowhere in the description. An agent is told
what it is looking up *by* and never what it gets *back*.

**Where it stops being cosmetic — GROUPS, from the same tree:**

```
"... referencing table GROUPS by foreign key R_15 (group_id (NUMBER))."
"... referencing table GROUPS by foreign key R_40 (group_id (NUMBER))."
"... referencing table GROUPS by foreign key R_41 (group_id (NUMBER))."
"... referencing table GROUPS by foreign key R_47 (group_id (NUMBER))."
"... referencing table GROUPS by foreign key R_62 (group_id (NUMBER))."
```

**Five tools, byte-identical but for a system-generated constraint name, all keyed on the same
single column.** Nothing in any of them says which child table it returns. A model cannot choose
between these on any principle at all; it can only guess, and four guesses in five are wrong. The
constraint names carry no meaning — Oracle assigned them.

**The web editor already knows the answer and the emitted description does not.** The inventory
row for the same operation reads `foreign-key children in DR.BOOKINGS (BKG_FLT)`. So the two halves
of one feature disagree about how much they can say, which is the tell that this is an oversight
rather than a decision.

### The fix — as built

**The child name is taken from the child `Table`'s own `oracleName`, not rebuilt from
`childFKOwner` + `childFKTName`.** Those two are in scope and were the obvious source, but joining
them by hand would re-implement the owner-prefixing rule (`JavaUtils.getOracleName` qualifies only
when the object is not the connected user's) — a second copy that would drift the first time a
cross-schema child appeared. The matching `Table` is already found in the loop that decides whether
the child is exposed at all, so its name was there for the taking.

**A three-step order, because the red had to be real.** The wording was extracted *unchanged*
first, so the new test could be watched failing on exactly the two FK assertions while the five
unique-key, index and escaping assertions passed — which is also what proved the extraction had not
altered behaviour. Had the extraction and the reword landed together, a green suite would have
proved only that the test agreed with itself.

`childFKOwner` and `childFKTName` are **already in scope** at the FK walk (~11719, where the child
table is matched against the selected tables), and are already used to decide whether the child is
exposed at all. They are simply not carried.

1. Two fields on `TableMcpInfo.Lookup`: `childTableOracleOwner`, `childTableOracleName`. It already
   carries `childRowClass` and `childTableFixedName`, both **Java** names — which is why the Oracle
   name is absent rather than merely unused, and why this cannot be derived at the emission site.
2. Set them beside `fkLookup.toolNameBasis = childFKConstraintName` (11757).
3. Reword at 8255:
   `"Look up rows in table DR.BOOKINGS that reference table FLIGHTS by foreign key BKG_FLT (…). Returns the child rows as a JSON array."`

Roughly five lines. Note it changes the **generated default** for every FK child tool on every
`MCP_SERVER=YES` config, so it needs the six-box estate — but it cannot change any *override*,
which is the property that makes it safe.

**Trap: `.description(...)` does not escape.** Plain prose only, no double quotes — this project
has already shipped an uncompilable tree that way once. A schema-qualified name is fine.

---

## Gap 2 — the constraint name as the only disambiguator

Not separate work: **gap 1 is the fix.** Recording it so it does not get "solved" twice.

The instinct on seeing `R_15` is to synthesise a friendlier name. Do not — the constraint name is
the one thing that is stable, unique, and matches what a DBA sees in `ALL_CONSTRAINTS`, and it is
also the description **key**, so changing what is displayed risks drifting from what is stored.
Once the child table is named, the constraint name is doing the right job: distinguishing two
foreign keys between the *same* pair of tables, which is exactly the case a table name alone cannot
resolve.

---

## Gap 3 — the editor gives no sign that a table has nine tools

> **DONE 2026-08-21.** Option 2 (the free one) plus the wording half of option 1. Web suite
> **408/0/0**, up exactly 10 on 398 — the ten tests added here.
>
> **Option 1 was NOT built as specified, and should not be.** A real tool count needs the
> dictionary, so it costs two queries per row on a page that can list hundreds of tables. The
> multiplicity is carried by WORDING instead, which is free: a table's link now reads
> **`descriptions`** (plural) against a sequence's `description`, and its tooltip names the whole
> set — *get_by_pk, insert, update and delete, plus one per unique key, index and foreign-key
> child*. That says "there are several behind here" without asking Oracle anything.
>
> The override count rides beside it as `(3 set)`, from the config alone. **A table with nothing
> written carries no badge at all** — a `0 set` reads as a warning about a table that is merely
> untouched. The SQL tab gets the same treatment as `(set)`, since a statement yields one tool and
> a number there would always be 1.
>
> **An empty description still counts.** Empty and absent are different outcomes throughout this
> feature — the editor keeps a separate reset marker precisely so they cannot be collapsed — and a
> badge that ignored an empty one would disagree with the editor that accepted it.

### The trap this cost, which is the documented one biting again

The first tooltip read `Edit this table&apos;s MCP tool descriptions`. **The HTML parser turns
`&apos;` back into an apostrophe before Thymeleaf parses the expression**, so the single-quoted
literal ended early and the *entire object list* threw at render time. javac saw nothing; the
existing `NewTableIsReadOnlyByDefaultTest` caught it. Reworded to avoid the apostrophe entirely
rather than escaped, because the escape that works here (`''`) is not the one anyone reaches for.

**And a correction worth keeping: the object list CAN be rendered without a database.** This
document's first draft of the gap-3 tests assumed it could not, because `/design/tables` calls
`listTables()`. `NewTableIsReadOnlyByDefaultTest` drives the template directly through a
`SpringTemplateEngine` with a hand-built context — no Spring Boot, no Oracle — and the badge tests
now do the same. Assuming otherwise would have left the render half untested, which is exactly the
half that failed.



`objectlist.html` (57) renders one link per object, labelled `description`, tooltip
"Edit the MCP tool description(s) for this object". The parenthesised `(s)` is the only hint. A
table with a PK, two indexes and five FK children hides nine editable descriptions behind a link
that looks exactly like a sequence's one.

Proposal, in order of value:

1. **Show the count** — `descriptions (9)`. It needs the inventory, which needs a connection, so it
   must degrade to the bare label rather than erroring. The four fixed operations are known without
   a connection, so a floor of 4 is always truthful.
2. **Mark that overrides exist** — the config already knows, with no connection needed:
   `workingConfig.tableDescriptions().get(key).size()`. `descriptions (2 of 9 set)`.
3. Leave the page itself alone; it already groups and labels correctly.

**Trap: do not compute the count by opening a connection per row.** The object list can run to
hundreds of tables, and `listTableLookups` is two dictionary queries plus one per P/U constraint.
Cache per config, or take option 2 alone — which is free, needs no database, and is what actually
answers "have I described this table yet?"

---

## Phases

*(1 to 3 are done for gap 1; the six-box estate is the remaining verification.)*

1. **Gap 1, red first.** Assert the current text does *not* contain the child table name, so the
   test is seen failing before it is made to pass. `generic_test1_mcp` already generates FK child
   tools — no fixture change.
2. Carry the two fields; reword; db-free suites.
3. **Estate**, six boxes: the default text changes for every FK child tool.
4. Gap 3, option 2 first (free), then option 1 if the count is wanted.

## Traps

- **The gate is per ROUTINE for PL/SQL but per OPERATION for tables** — a non-crossable key drops
  one lookup tool, not the table.
- **`apply*Selection` rebuilds its lists**, so descriptions must be re-attached; ticking one more
  table would otherwise silently wipe every description in the config.
- **A description stored against an operation that yields no tool is accepted and dropped in
  silence** — that is what `McpUnexposedReport` exists to surface, and it is empty until the config
  has been generated once.
- **`info.lookups` holds the SAME `Lookup` objects `mcpTableLookups` does** (added by reference at
  8013, not copied), which is why a new field carries through to emission. Checked rather than
  assumed: a copy there would have dropped the field silently and emitted the fallback wording,
  which looks exactly like the fix not being applied.
- **A partial regen leaves a PARTIAL jar, not just a stale one.** Regenerating one propfile to
  inspect its output repackages `generated.jar` with only that propfile's classes, so the harnesses
  for every other propfile then fail to compile. Inspect the output, but run the suite only after a
  full regen.
- **Test the round trip on the emitted server, not the config.** `TGen23aiMcp` already reads
  `tools/list` off a running generated server and asserts `TABLE_MCP_DESC_0_INS` comes back
  verbatim; extend that rather than inventing a second mechanism.

Copyright 2003-2026 ATB Consultancy Services Ltd
(formerly Orinda Software Ltd, Dublin, Ireland)
