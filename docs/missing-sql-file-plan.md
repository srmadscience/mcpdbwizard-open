# A SQL statement whose file is absent vanishes without trace — plan

**Status: option A IMPLEMENTED (2026-08-11). B, the §5 content decisions and the §7 web guard are
open.** Written 2026-08-11 against `03ae3b9`.

A is `ApplicationShell.reportSqlFilesNamedButNotFound` plus the testable
`sqlFilesNamedButNotFound` walk it delegates to. Verified live on ORCL12: a `generic_test1` regen
now warns

    SQL file 'bfilenameFactory.sql' is named by this configuration but is not in
    .../app/Sqlfiles, so no statement was read for it -- and it asked for a class,
    which will NOT be generated.

and emits the machine-readable sibling, so the description editor marks that row too — which closes
the case that started all of this. File count unchanged (2487 on ORCL12), because this is log output
only. Tests: `SqlFilesNamedButNotFoundTest` (6, the walk) and `DanglingSqlFileCensusTest` (2, the
census below). **The census corrected this document:** §2 first said 26 dangling `create=YES`
references; the assertion said 46, and the assertion was right — 26 came from summing the
per-propfile list by eye.

Started as a one-line gap in the MCP "yields no tool" report (`ed71a45`): 28 statements were
deselected, 27 were reported. Chasing the 28th found something wider, and the original framing was
wrong — the missing one is not a `BAD_FILE` wrangler that the report skips. **There is no wrangler
at all.**

---

## 1. What actually happens

`ApplicationShell` builds its statement list by **scanning the SQL directory**
(`aspStatementFileNames`, ~line 3040) and then matching each file it found back to a
`SQL_FILENAME_<i>` key. The config's list is never the driver. So a config naming a file that is not
in that directory produces no `SqlStatementWrangler`, no entry in `aspStatementWranglerArray`, and
nothing downstream can report it — including the `MCP-UNEXPOSED` loop, which iterates that array.

The `fileType` ladder is a red herring for this case. `BAD_FILE = 0` is the field's initial value and
is never assigned; a file that is read but unparseable becomes `GOOD_FILE_BAD_SQL_STATEMENT`, which
*is* surfaced (as `aspStatementErrors[i]`, "SQL Statement not usable"). The absent-file case never
reaches the constructor.

**What is reported today:** an unusable *directory* (`error("Directory … is not usable")`) and an
empty one (`info(… does not contain any usable SQL files)`). **What is not:** any individual named
file that is missing. Confirmed on a live ORCL12 regen — `bfilenameFactory.sql` is named by
`generic_test5_mcp`, is absent from `app/Sqlfiles/`, and appears **zero times** in the generator log,
which ends `rc=0`.

## 2. Scale, measured rather than guessed

Against the directory a regen actually uses (`testrun_current.sh` forces
`SQL_FILE_DIRECTORY=$HOMEDIR/Sqlfiles` for every propfile, overriding what the file says — several
still carry dead Windows paths like `C:\DR\Work\CodeSpooks\Sqlfiles`):

- **84 dangling `SQL_FILENAME_<i>` references across 32 of the 41 propfiles.**
- Of those, **46 references across 10 propfiles carry `SQL_CREATE_CLASS=YES`** — the author asked for
  a class and silently did not get one:

  | Propfile | Dangling, create=YES |
  |---|---|
  | `generic_test1` / `_mcp` | `bfilenameFactory.sql` |
  | `generic_testd` / `_mcp` | 7 `Demo*.sql` |
  | `generic_testf` / `_mcp`, `connector_json{,rpc}` / `_mcp` | 5 (`GetFlights.sql`, `CustomerUpdate.sql`, `CustomerDelete.sql`, `CustcomerAdd.sql`, `ChangeFlightDates.sql`) |

- The remaining 74 are `SQL_CREATE_CLASS=NO`, so they cost nothing today beyond a description that
  could be written against them and never used.

**Most of these files exist — in the wrong directory.** `GetFlights.sql` is in `app/sql/SqlFiles/`,
`bfilenameFactory.sql` in `app/Propfiles/connector_json/` and `…/connector_none/`, `deleteXml.sql` in
`app/Propfiles/generic_test0/`. A few (`DemoAddCust.sql`) are nowhere in the repository. So this is
partly a real content gap and partly a single-directory regen flattening configs that were authored
against several.

**This is baked into the baseline.** `expected-file-counts.txt` was calibrated on output that had
already lost these classes, so the floors cannot see it and never could — the same blind spot the
cross-version floor has for a 12c-only shortfall, one level up.

## 3. Why it matters beyond tidiness

`generic_test1` is the propfile everything else is measured against, and it has been asking for a
statement class it does not get. The failure is **silent, and on the success path**: generation
returns 0, the tree compiles, the suite is green, and the only symptom is a class that was never
there to miss. That is the same shape as the strong-REF-CURSOR degradation (`58a47e2`) and the
`generic_test7` package collision — both invisible for as long as nobody counted the right thing.

## 4. Options

| | Option | Assessment |
|---|---|---|
| **A** | **Warn per dangling reference at generation** — after the directory scan, diff the config's `SQL_FILENAME_<i>` set against what was found and log each miss | **RECOMMENDED.** Small, local, and it reports at the moment the information exists. Says nothing about whether it should have generated, which keeps it honest. |
| B | Fail generation on a dangling reference with `SQL_CREATE_CLASS=YES` | Correct in principle — the author asked for a class. But it turns 10 propfiles red immediately, including `generic_test1`, so it cannot land before the content question in §5 is settled. Do it *after* A, as a follow-up. |
| C | Drive the list from the config instead of the directory scan | The real fix for the inversion, and much the largest: the scan also feeds the Swing selection UI, which needs to show files not yet in the config. Not worth it for this. |
| D | Extend `MCP-UNEXPOSED` to cover it | Where I started, and wrong on its own: it reports only into the description editor, so a `WEB_SERVICES=NO` config or a plain batch run still says nothing. A's log line is the general answer; the editor can read it later. |

**Recommendation: A now, B once §5 is resolved.** A is a dozen lines and is a strict improvement
regardless of what is decided about the files themselves.

**Plus one web-only fix that is independent of all four** — guarding the SQL library's delete button
against removing a file a config still names (§7). A catches a dangling reference a generation later;
the guard stops the commonest way of creating one. Do both.

## 5. The content question A will expose, and it needs a decision, not code

**Propfile/regen-side only — none of this touches a web deployment**, for the reasons in §7. A web
config starts with an empty SQL library and can only acquire a dangling reference through the delete
path described there.

Once A logs them, 84 warnings appear on every regen across 32 propfiles, which is loud enough to be
ignored — the failure mode this file elsewhere warns about ("a warning that is always wrong stops
being read"). So each group needs an answer first:

1. **Files that exist elsewhere in the repo** (`GetFlights.sql`, `bfilenameFactory.sql`,
   `deleteXml.sql`, …) — copy into `app/Sqlfiles/`, or teach the regen a per-propfile SQL directory?
   Copying is simpler and matches the single-`SQLDIR` regen; a per-propfile directory is more
   faithful to how the configs were authored.
2. **Files that exist nowhere** (`DemoAddCust.sql` and the rest of the `Demo*` set) — write them,
   or remove the `SQL_FILENAME_<i>` entries from `generic_testd`? Note removing entries **renumbers
   the indices**, which is exactly the index-instability trap `mcp-tool-descriptions-plan.md` §4.3
   documents: any `SQL_MCP_DESC_<i>` in those configs must move with them.
3. **The 74 `create=NO` references** — leave them (they are inert and record intent), or clean them?
   Leaning leave: they cost nothing and deleting them renumbers indices for no gain.

## 6. Implementation sketch for A

- In `ApplicationShell`, after `aspStatementFileNames` is built and the wranglers are constructed,
  walk `SQL_FILENAME_<i>` (the same `SEQ_MISSING_LIMIT` scan the matching loop already uses) and
  collect any value not present in the scanned set.
- One `warning` per miss, naming the file, the directory searched, and whether that entry had
  `SQL_CREATE_CLASS=YES` — the last part is what separates "you will not get the class you asked
  for" from "an inert leftover".
- A machine-readable sibling line, exactly as `mcpUnexposed` does, so the description editor can
  eventually mark those rows too (closing the case that started this).
- Emit nothing when the directory itself was unusable — that is already reported, and 29 further
  lines saying each file is missing would bury it.

## 7. The web module: the same silence, a much smaller hole, and one fix of its own

**Generation is identical.** `RuntimeManager` forks `com.mcpdbwizard.app.procbuilder.gui.ProcBuilder`
in a child JVM (`GENERATOR_CLASS`), so a web-authored config runs through the same `ApplicationShell`
directory scan and loses a dangling statement exactly as a propfile does, with the same silence.
Option A therefore covers both, and it belongs where it is — in the shared generator, not in either UI.

**But the web can barely create one, and that is structural rather than lucky.**

1. **Selection is closed over what exists.** `SqlController.select` hands `applySqlSelection` the
   checked filenames, and those checkboxes are rendered from `SqlLibraryStore.list(scope)` — a
   directory listing. A file that is not there cannot be ticked.
2. **Every config has its OWN SQL directory.** `scope()` is `workingConfig.getSqlScope()`, and
   `applySqlSelection` repoints `SQL_FILE_DIRECTORY` at that scope on each save. This is **option C
   above, already implemented on the web side** — and it is worth noticing that the 84 propfile
   references dangle largely *because* the regen flattens every config onto one `SQLDIR`. The web
   does not have that problem to begin with.

**The one real hole: delete-after-select.** `SqlController.delete` calls
`sqlLibrary.delete(scope(), filename)` and consults nothing — not the working config, not the saved
ones. So: select `foo.sql` with `SQL_CREATE_CLASS=YES`, delete it from the library, generate. The
config still names it, the scan no longer finds it, no class is emitted, `rc=0`, green. Two clicks,
no error. The description editor keeps offering a box for it too, since `sqlDescriptions()` reads the
config rather than the directory.

**Fix (independent of A, and worth doing on its own):** make the delete consult the configs in that
scope. Refusing outright is the simpler rule and probably right — a file a config depends on should
not disappear by accident — with the message naming which configs use it so the author can deselect
first. Warn-and-deselect is the friendlier alternative but has to mutate saved configs, which is a
larger promise than a delete button should make. Either way it stops the hole where it is created
rather than reporting it a generation later, which is the same reasoning A uses one level down.

Note the two fixes are complementary, not alternatives: the web guard prevents the common case, A
catches whatever still arrives — a config edited by hand, restored from a backup, or copied between
scopes.

## 8. Tests

- A db-free test over the committed propfiles asserting the dangling set is **exactly** the 84 known
  today, so the number cannot grow unnoticed and shrinks visibly as §5 is worked through. This is the
  cheap guard and it does not need the fix to land first — write it either way.
- A unit test of the diff itself: a config naming a file the scan did not return produces one
  warning; a config whose files are all present produces none.
- No estate run: this changes generator *log* output only. It becomes an estate matter only if
  option B lands, which by construction changes what generates.
