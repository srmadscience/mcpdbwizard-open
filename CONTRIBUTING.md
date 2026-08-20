# Contributing

## Running the tests

The database-free suite needs nothing, and is green on a fresh clone:

    mvn test

The live-database tests and the generated-code harnesses need an Oracle instance. Without one
they SKIP rather than fail, which is why the clone is green before you configure anything.

To point them at your own instance, copy the three templates and fill in your connection
details -- the real files are gitignored and never ship:

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
complete and self-contained, and that suite is what `mvn test` runs on a fresh clone.

If you want to exercise the live tier, point the generator at a schema of your own.
Scripts/check_provisioning.sh will tell you exactly which objects a config expects.

examples/generated-output/ shows what the generator emits, without needing a database at all.

## Licence

Contributions are accepted under the Apache License 2.0, the licence of this project.

## Before opening a pull request

    Scripts/export/check-export-clean.sh

This refuses to pass if a private host, a credential or a jar has crept in. It runs against
the whole tree, so it catches a stray connection string in a test fixture as readily as one
in a script.
