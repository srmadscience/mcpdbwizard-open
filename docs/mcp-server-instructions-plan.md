# A config-level description for the server's MCP instructions — plan

> **COMPLETE, 2026-08-20.** Implemented as `MCP_INSTRUCTIONS`, db-free suites green (app 832/0/0,
> web 398/0/0), an absent key verified BYTE-IDENTICAL against a pre-change tree, and a generated
> tree carrying author text with a quote and a backslash compiles (129 classes, 0 errors).
>
> The plan below is kept as written, with the phases it got wrong marked. Two things it missed:
>
> 1. **`toJson` / the JSON constructor keep their OWN field list**, separate from `SCALAR_KEYS`.
>    Wiring the properties path alone left the value lost crossing JSON, and **the JSON round-trip
>    test is what caught it** — the "is it readable" test the plan did argue for passed, because the
>    properties half was correct. Both halves need wiring and both need asserting.
> 2. **The web control does NOT belong on `descriptions.html`.** That page is per-object
>    (`?type=&key=`), so a server-level field there would render on every object's page. It went on
>    the **Service options** panel instead, beside `MCP_HTTP_TOKEN`, `MCP_HTTPS` and
>    `PROMETHEUS_SERVER` — the settings it is a sibling of. Naming a file because it exists is not
>    the same as reading what is in it.
>
> Decisions taken where the plan left them open: **limit 4096** with its own cost sentence, and
> **empty treated as absent** for emission while still round-tripping.

## What exists today

A generated server already sends `instructions` — it is a top-level field on
`McpServer.SyncSpecification`, returned in `InitializeResult`, and **not** a member of
`ServerCapabilities` (that builder has exactly `completions`, `experimental`, `logging`,
`prompts`, `resources`, `tools`).

`SAAdminWrangler` builds the text at ~5197 from five clauses, each emitted only when that surface
has anything in it, and prints it at **two** sites — the stdio server and the HTTP one:

```java
.instructions("Exposes row CRUD on table(s) AIRCRAFT, AIRLINES, ... ")
```

| clause | says |
|---|---|
| duality views | doc CRUD, the `_id` / `_metadata.etag` contract, and that omitting `_metadata` overwrites |
| tables | which tables, the four operations, rows as JSON keyed by column name |
| PL/SQL routines | which routines, IN params in, JSON out |
| SQL statements | which statements, query → JSON array, DML → `{"executed":true}` |
| sequences | which sequences, `<sequence>_nextval` |

**It is assembled entirely from object names and fixed prose. There is no config key feeding it** —
`SERVER_MCP_DESC`, `MCP_INSTRUCTIONS` and `SERVER_DESC` appear nowhere in the source.

## The gap

Per-**tool** text is already authorable, through four key families that round-trip into `.pb2` and
`.json`:

| key | scope |
|---|---|
| `TABLE_MCP_DESC_<i>_<op>` | per table **per operation** (`_PK`, `_UK_<constraint>`, …) |
| `PROC_MCP_DESC_<i>` | one PL/SQL routine |
| `SQL_MCP_DESC_<i>` | one curated statement |
| `SEQUENCE_MCP_DESC_<i>` | one sequence |

So an author can explain each tool and cannot say a word about the schema as a whole — yet
`instructions` is what a model reads **first**, before any tool. For a schema with names chosen by
someone else years ago, the tools can be described perfectly while the server still opens with
`Exposes row CRUD on table(s) GLELM_AST_INST, WRKBQ`.

What is missing is the part only a human knows: what this schema is *for*, which objects relate to
which, and what an agent should not attempt even though a tool exists for it.

## Design

**Prepend, do not replace.** The generated inventory is always accurate and is derived from what was
actually emitted; the author's text adds meaning it cannot know. Author text first (a model reads
top-down), generated clauses after. **A config with no key set must emit a byte-identical server** —
that is the acceptance test for the whole change, not a nicety.

**Key name: `MCP_INSTRUCTIONS`.** It names the MCP concept it feeds, and it does not read as a
variant of the existing `MCP_SERVER` flag the way `MCP_SERVER_DESC` would. `app/CLAUDE.md` already
records `EXTRA_SQL` being mistaken for the user-SQL-statement flag and the web app then believing
it, so a key whose name invites the wrong reading is a real cost, not a style question.

**Propfile + web GUI, no Swing control** — the settled pattern for every MCP-era scalar
(`MCP_HTTP_TOKEN`, `MCP_HTTPS`, `MCP_OAUTH`, `PROMETHEUS_SERVER`). Do not add a Swing field.

## Phases

**Phase 1 — carry it through the config model.**
`Schema`: a `mcpInstructions` field, getter/setter, the full-ctor parameter, the `props.getProperty`
read and the `toPb2` write — the shape `prometheusServer` already has at
`Schema.java:92/202/293/756/1271/1470`.

**And add it to `SCALAR_KEYS` (~1182). This is the trap, and the file already documents it** for
`TABLE_MCP_DESC`: a key that is not claimed by a scalar field falls through to `extraProperties`,
where it **round-trips perfectly while nothing in the application can read it**. `SchemaRoundTripTest`
would stay green throughout. So the round-trip test cannot detect this mistake, and a test that asserts
the value is *readable* is what the phase needs.

**Incomplete as written: `toJson` and the JSON constructor keep a SEPARATE field list** (`m.put(...)`
and `m.get(...)`, ~1608/~1716). `SCALAR_KEYS` governs the properties path only. Wiring one and not
the other gives a value that survives `.pb2` and is silently null after a `.json` round trip.

**Phase 2 — read and preserve it in the generator.**
`ApplicationShell` reads every config scalar on load and writes it back on save; a key it does not
know is **dropped on the next save**, silently. Follow the `prometheusServer` sites
(`ApplicationShell.java:185/870/1618/2470/2598`). Note it is a `String`, not a flag, so it needs the
absent-versus-empty distinction below rather than a boolean.

**Phase 3 — emit it.**
Prepend to `instructionsText` at `SAAdminWrangler` ~5197, ahead of the duality-view clause. **Both
print sites must change** (~5540 stdio, ~5707 HTTP) — they are an existing duplication and a change
to one alone produces a server whose two transports describe themselves differently, which nothing
would catch.

**Escaping is not optional, and the answer is already written down.** The text becomes a Java string
literal and then crosses MCP as JSON. Use `SAAdminWrangler.javaStringLiteral` (~284) for quotes and
backslashes — **note the existing `.instructions(...)` lines do NOT escape at all** today, because
everything reaching them is generator-built with its embedded quotes hand-escaped
(`{\\\"executed\\\":true}`). Adding author text to an unescaped concatenation is the bug this phase
most likely ships.

**Phase 4 — refuse what escaping cannot fix, at the form.**
`javaStringLiteral` handles quotes and backslashes and **not newlines**. A raw newline in an emitted
literal does not fail politely: it fails as a compile error across the whole generated tree, long
after whoever pressed Return has moved on. `DescriptionController.descriptionProblem` (~127) already
rejects newlines, control characters and over-length text with messages written for a person looking
at the form. **Reuse it rather than writing a second validator** — two validators disagreeing is how
one field starts accepting what another refuses.

**Phase 5 — the web UI. ~~WRONG AS WRITTEN~~ — see the banner.**
~~`design/descriptions.html` … so the server field belongs at the top of that page.~~ That page is
**per-object** (`/design/descriptions?type=&key=`), so the field would have rendered on every
object's page. It went on the **Service options** panel instead, beside the other MCP scalars, with
a single-line `input` matching what the per-tool rows use — the newline rule makes a textarea an
invitation to fail.

**Phase 6 — verify.**
`TGen23aiMcp` drives a real generated server over a real `initialize`, so it can assert the
instructions string end to end. **Assert both directions:** that author text appears *and* that the
generated clauses still follow it. Asserting only the first would pass a change that threw the
inventory away. Then a config with the key absent must generate **byte-identically** to before.

## Decisions wanted

1. **Length limit.** `descriptionProblem` enforces a `MAX_LENGTH` tuned for a tool description, and
   its message ("every `tools/list` response carries it to the model") is about a different cost:
   instructions ride one `initialize`, not every call. A separate, larger limit is probably right —
   but it is a decision, and reusing the validator without changing the message would show the wrong
   explanation.
2. **Empty versus absent.** The tool-description path treats null as "no override, use the
   generated default" and empty as "an author deliberately choosing empty". Instructions have no
   per-object default to fall back to, so the same distinction may not carry — decide whether an
   empty value means "generated clauses only" or is refused.
3. **Should it be able to replace rather than prepend?** No control is proposed. If an author wants
   the inventory gone, that is a second flag, and the argument for it is weak: the clauses describe
   what was actually emitted and a model benefits from both.

## Out of scope

The MCP **tool annotations** and the per-tool descriptions are unchanged. This adds one string in one
place; it is not a rework of how any tool describes itself.

Copyright 2003-2026 ATB Consultancy Services Ltd
(formerly Orinda Software Ltd, Dublin, Ireland)
