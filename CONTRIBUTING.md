# Contributing

## How this repository works, before anything else

**This is a published mirror, and the sync is one-way.** Development happens in a private
repository; this one receives a squashed commit per release. You can see that in the log --
"Sync from the development repository at <sha>" rather than a series of individual changes.

Two consequences, and it is better to know them now than after you have written something:

**A pull request is not merged here.** If it is accepted it gets applied upstream and arrives
in a later sync commit. Your change ships; your commit does not appear in this history. You
will be credited in the upstream commit message, and told when the sync lands.

**Do not build long-lived work on this history.** It only moves forward, and only in whole
releases. Rebasing on the latest sync is fine; expecting to merge a months-old branch is not.

If that is a poor fit for what you had in mind, **open an issue first** and say what you want
to change. It costs you nothing and it may save you an afternoon.

## Running the tests

The database-free suite needs nothing, and is green on a fresh clone:

    mvn test

That is the suite worth caring about here. The live-database tests skip without a database,
which is why the clone is green before you configure anything.

To point them at your own Oracle instance, copy the templates and fill in your own details --
the real files are gitignored and never ship:

    cp src/test/resources/test-boxes.properties.template src/test/resources/test-boxes.properties
    cp Scripts/tns/tnsnames.ora.template                 Scripts/tns/tnsnames.ora
    cp Scripts/boxes.env.template                        Scripts/boxes.env

Per setting, an environment variable (MCPDBWIZARD_TEST_HOST, ...) always wins over the file.

## The test schemas are not in this repository

The generator's own test corpus introspects Oracle schemas whose structure is not ours to
publish -- some of it came from customer work years ago. A generator config ENUMERATES the
schema it points at, so the configs cannot ship either, and the live-database harnesses go with
them: they name those schemas' objects, and they only compile against a regenerated tree that
cannot exist here.

Nothing else is affected. The generator, the runtime library and the database-free suite are
complete and self-contained.

If you want to exercise the live tier, point the generator at a schema of your own.
Scripts/check_provisioning.sh will name the exact objects a config expects, which is the place
to start if you are building a fixture.

examples/generated-output/ shows what the generator emits, without needing a database at all.

## What makes a change easy to accept

- **A test that fails before and passes after.** The database-free suite is where that belongs.
- **Say what you observed**, not only what you changed. This project's own notes are full of
  fixes that were right about the symptom and wrong about the cause.
- **Small and self-contained.** Every change has to be replayed upstream by hand.

## Before you send it

    Scripts/export/check-export-clean.sh

It fails if a hostname, a credential or a jar has crept into the tree. It exists to stop the
maintainers publishing something private, and it is just as good at catching a connection
string you left in a test fixture.

## Licence

Contributions are accepted under the Apache License 2.0, the licence of this project.
