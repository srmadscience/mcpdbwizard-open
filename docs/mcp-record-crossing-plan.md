# PL/SQL records cross MCP badly — plan

Two OPEN defects plus one smaller one, found 2026-08-17 by driving the **live `orindademo` MCP
server** as an MCP client rather than by reading code. Neither of the two is Item 7.

Status: **ALL FOUR PHASES DONE** — 1 `fc6f190`, 2+3 `deb22c7`, 4 `144bc29`. Kept as the
record of what was wrong and why each fix took the shape it did; the disproved approaches below
are the part worth re-reading before touching this code again.
Measurements are dated where they appear and were taken against ORINDADEMO on FREE26 —
re-measure before quoting, per the dated-record rule in the private `CLAUDE.md`.

---

## Defect 1 — a record parameter publishes no field schema

`complex_example_add_bookings` emits:

```json
"p_customer":      { "type": "object", "description": "record parameter" },
"p_booking_table": { "type": "array", "items": { "type": "object" } }
```

No `properties`, no field names, no types. **A caller cannot discover what to send.**

**Two independent stages each drop the information**, so fixing either alone changes nothing:

1. `CallableStatementParameterEngine.getMcpParamTuples()` skips every row where `DATA_LEVEL != 0`.
   A record's fields *are* the `DATA_LEVEL 1` rows. The tuple it returns is 8 flat strings with no
   slot for structure, so the shape cannot survive the call even if the rows were kept.
2. `SAAdminWrangler`'s `fieldSchema` ternary in `addPlsqlMethodMcpTool`: the `array` arm emits
   `items` carrying only a type; **`bfile` is the single arm that emits `properties`**; everything
   else — including record, which sets `schemaType="object"` — falls through to a flat
   `{type, description}`.

### Why it survived

`getMcpParamTuples`'s javadoc still says a record/collection parameter "surfaces as a
non-JSON-crossable javaType at level 0, **so the emission skips it**." That was true before records
were supported and is now **stale** — records bind through `RECORD_MAPPER.convertValue(...)`. The
obsolete sentence makes the level-0 filter read as deliberate and harmless. Same failure mode as the
stale plan-doc statuses, but in a code comment, where it is harder to notice.

### Two multipliers, both confirmed

- **Wrong keys bind silently to null.** `RECORD_MAPPER` is built with
  `FAIL_ON_UNKNOWN_PROPERTIES` **disabled**, so unknown keys are ignored: the record binds all-null
  and the procedure runs. A silent wrong answer, not an error.
- **The keys are Java field names, not Oracle column names** — `paramDepartureCity`, not
  `DEPARTURE_CITY`, measured from a live OUT payload. Reading the DDL gets you every key wrong.

### The fix has precedent in-tree

The duality-view path already builds a full per-field property map from `info.docFields`, and the
generated `...Attrs` class holds the exact field list (`FlightsTypeAttrs` = 7 typed public fields).
**The generator knows the shape; the schema emission never asks for it.**

### This is NOT Item 7

`mcp-item7-nested-records-plan.md` is about record-*in*-record classes generating as empty stubs.
This bites **flat, fully-populated** records too. That plan's Phase 2 says to "confirm the tool emits
a nested JSON object schema" — it assumes this already works. It does not. Adjacent family, different
layer: schema emission, not dictionary synthesis.

---

## Defect 2 — `theLog` leaks into every serialized record (3.8x bloat)

`complex_example_get_table_of_flights_from('SFO')` returns 159 rows and **overran the tool-result
token limit**, spilling to a file. Measured: **144,885 chars actual against 38,514 chars of real
data — 3.8x, 669 bytes of noise per row.**

Every element carries two non-data fields, and Jackson recurses into the second:

```json
"recordName": "ORINDADEMO.FLIGHTS_TYPE",
"theLog": { "autoFlush": ..., "logger": { "handlers": [...], "level": {...},
            "parent": { ...the whole parent logger chain, resource bundles... }}}
```

**Cause.** `theLog` is package-private in the generated record class, but `RECORD_MAPPER` sets
`withFieldVisibility(ANY)`, which deliberately overrides access modifiers and sweeps in the
internals.

**`recordName` alone was already known** and judged "harmless, ignored on input". That verdict was
right about `recordName` and missed `theLog`, which is the expensive half. The lean shape already
exists: `FlightsTypeAttrs` has the parameter fields and nothing else; the fat concrete class is what
goes on the wire.

### The constraint that decides HOW to fix it

**`withFieldVisibility(ANY)` must stay.** It is not incidental — it is the cure for a separate
defect fixed in `0d3bc1d` (2026-08-17): the SDK's getter-honouring mapper reflected over ~23
narrowing accessors per row class, and `getXxxByteObj()` does `Byte.valueOf(value.toString())`,
which throws `NumberFormatException` for **any NUMBER above 127** and failed the whole tool call.
`getXxxLong()` on a DATE has the same shape with a null check missing.

So: suppress the two fields **by name** (`@JsonIgnore`, a mixin, or serializing the `...Attrs`
superclass view). Do **not** narrow visibility back, and do **not** return to getter-based
serialization — either reintroduces a crash on ordinary data.

**Scope check, measured 2026-08-17:** SQL-statement row classes do **not** carry `theLog` or
`recordName`, so routing that path through `RECORD_MAPPER` in `0d3bc1d` did not widen this. A live
`getflights` returns exactly its 7 column keys. The leak is confined to PL/SQL record classes.

---

## Defect 3 — two date formats from one column

The record path serializes `"2003-06-09T18:38:00.000Z"`; the ref-cursor path gives
`"2003-06-09T22:44:00"`. Same database, same column. The `Z` asserts UTC for what is almost
certainly local time.

Since `0d3bc1d` the SQL-statement path shares `RECORD_MAPPER` and so produces the **`.000Z` form**
too. That makes the split record + statement versus ref cursor — marginally worse to look at, and
easier to fix, because two of the three now share one mapper.

---

## Phases

**Order matters: 1 is smallest and is currently breaking real calls.**

1. **Suppress `theLog` and `recordName`. DONE 2026-08-17, `fc6f190`** — a mixin on
   `RECORD_MAPPER` naming both fields, applied to `Object.class`. Measured 144,885 -> 38,514 chars
   on the 159-row call, six boxes green. Original note kept: by name, per the constraint above. Verify with
   `complex_example_get_table_of_flights_from('SFO')`: the payload should fall from ~144,885 chars
   towards ~38,514, and the call should stop spilling to a file.
2. **Publish the record schema by INTROSPECTING THE RECORD CLASS at server start** — not by
   asking the dictionary. David's suggestion, 2026-08-18, and it is better than what this plan
   originally said; the original approach is recorded below because it was tried and disproved.

   The emitted server **already names the class at the point of use**:

       RECORD_MAPPER.convertValue(req.arguments().get("p_customer"),
                                  com.mcpdbwizard.customer.plsql.Customers.class)

   So the same reference can produce the schema. Build it through `RECORD_MAPPER` itself, so the
   published keys are *by construction* the keys that bind.

   **Three reasons this beats sourcing it from the generator:**

   - **It cannot drift.** One mapper config decides both what is published and what is accepted.
     `theLog` and `recordName` drop out for free, because the phase-1 mixin is part of that config.
     Any generator-side route re-derives the names in a second place, and two places can disagree.
   - **The `ALL_ARGUMENTS` truncation stops mattering.** `Customers.class` carries the same seven
     fields whether the dictionary returned 181 rows or 27. That is exactly what defeated the
     original approach.
   - **Far less code** — no tuple slot, no `recordFieldsAt` row walk, no threading `extraObjects`
     from one emission method to another.

   Verified 2026-08-18 against the real generated classes on ORINDADEMO:

       Customers (7)   paramAddress:String  paramBirthdate:Date  paramCity:String
                       paramName:String  paramPhone:String  paramState:String
                       paramZip:BigDecimal
       Airports (2)    paramAirportCode:String  paramAirportName:String

   **Two things to get right.**

   *Do not assume a constructor.* The probe instantiated a blank via `(LogInterface)`, but only
   **22 of 39** generated `plsql` classes have that constructor. Use an instance-free route: reflect
   the `...Attrs` superclass's public `param*` fields — that is where the data lives — and filter
   through the mapper's configuration rather than a hardcoded exclusion list, or the drift-freedom
   above is lost.

   *This moves the schema from generation time to runtime.* Someone reading
   `DaoFactoryMcpServer.java` will no longer see the field list in the source; it exists only once
   the server starts. That is a genuine debuggability loss traded for the correctness gain, and it
   should be a deliberate choice rather than a side effect. Emitting a comment naming the class the
   schema is derived from would soften it.

   Nested records become tractable rather than a separate question: recursion on a field whose type
   is itself a record is a few lines, where the row-walking route had to decide how deep to flatten.

   ### Disproved: sourcing the fields from the dictionary (2026-08-17)

   The original phase 2 said to keep the `DATA_LEVEL 1` rows in `getMcpParamTuples`, on the reading
   that a record's fields *are* the level-1 rows. **True on 12c, false on every truncating box** —
   five of the six. Instrumented against ORINDADEMO on FREE26, the engine's rowset for
   `COMPLEX_EXAMPLE.ADD_BOOKINGS` holds only three rows, all `DATA_LEVEL 0`:

       lvl=0 P_CUSTOMER        var=paramPCustomer       type=Customers
       lvl=0 P_BOOKING_TABLE   var=paramPBookingTable   type=BookingsTable
       lvl=0 P_STATUS_MESSAGE  var=paramPStatusMessage  type=String

   There are no level-1 rows to keep. The fields are synthesised into the record objects that
   produce the `...Attrs` classes, not back into the rowset. Sourcing them from that registry would
   work, but it needs real plumbing — `extraObjects` is a local in the emission method while
   `addPlsqlMethodMcpTool` is elsewhere — and it still re-derives names in a second place, which is
   the drift the introspection route avoids entirely.

   Whichever route: fix the stale `getMcpParamTuples` javadoc, which still says the emission "skips"
   a record parameter at level 0. It is what made the filter look deliberate and harmless.

3. **Revisit `FAIL_ON_UNKNOWN_PROPERTIES`.** Silently swallowing unknown keys is defensible while
   callers cannot discover field names. Once a schema is published it stops being protective and
   starts hiding caller errors. This is a behaviour change for existing callers, so it is a decision
   rather than a follow-on.

   **Decided 2026-08-17: ship it WITH phase 2, not before.** Enabling it while the shape is still
   undiscoverable would be strictness with nothing to be strict against — it would reject keys the
   caller had no way to get right. The one-line change is ready (drop the `.disable(...)` from the
   emitted `RECORD_MAPPER`); it is held back only because phase 2 is.
4. **Reconcile the date formats. DONE 2026-08-18, `144bc29`** — and it was not a formatting
   bug. The record path rendered in UTC, so it reported `17:38` for a row Oracle holds as `18:38`:
   a WRONG WALL-CLOCK TIME, not a differently-spelled one. Dropping the `Z` alone would have left
   the hour wrong with nothing marking it, so `defaultTimeZone(getDefault())` was needed alongside
   the pattern. Both paths now return the identical string for the same flight.

Use the byte-diff discipline in `app/CLAUDE.md`: non-MCP trees must stay byte-identical, and
`generic_test_23ai` is the propfile whose MCP surface is expected to move.

---

## Decisions wanted before starting

- **Published field names stay Java-shaped — DECIDED 2026-08-17.** `paramDepartureCity`,
  not `DEPARTURE_CITY`: they are the names the generated record class carries and therefore the
  only names `RECORD_MAPPER` binds. The Oracle spelling would read better and not work. Original
  framing kept below.
- **Do the published field names stay Java-shaped?** `paramDepartureCity` is what the code produces;
  `DEPARTURE_CITY` is what someone reading the DDL will send. Publishing the schema makes the
  mismatch discoverable rather than silent, but renaming would break any existing caller.
- **Phase 3 is a compatibility break** in the direction of strictness. Worth pairing with phase 2 so
  the schema and the strictness arrive together, rather than tightening against callers who still
  cannot see the shape.

---

## Reproducer — needs no build

Point an MCP client at the running `orindademo` config (`app/Scripts/start-mcp-server.sh`), then:

```
tools/list                                   -> read complex_example_add_bookings' inputSchema
complex_example_get_table_of_flights_from    -> p_city = SFO
```

Two traps in the fixture data: that tool takes an **airport code**, not a city name; and the
ORINDADEMO rows are all **March–June 2003**, so any date-filtered tool returns nothing unless given
2003 dates.

Copyright 2003-2026 ATB Consultancy Services Ltd
(formerly Orinda Software Ltd, Dublin, Ireland)
