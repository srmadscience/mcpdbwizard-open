# Example generator output

`generated-output/` is a checked-in copy of what MCPDBWizard emitted from a small
flights/airports schema (`ORINDADEMO`) — 106 Java files plus the `extraObjects.sql`
the generator wrote alongside them. It was regenerated on 2026-08-07 against a live
schema, so it shows what this generator produces today rather than what it once did.

Read it to see the shape of the output before installing anything: the DAO factory,
the per-table managers under `generated/table/`, the callable-statement wrappers under
`generated/plsql/`, and the SOAP service layer.

**It is an illustration, not a runnable sample.** The DDL for the schema it was
generated from is not part of this repository, so there is nothing here to point it
at. `demo.java` is a hand-written driver kept for the same reason — to show how
generated code is called, not to be run as-is.

Nothing here is on the build path. `pom.xml` compiles `src/main/java` only.
