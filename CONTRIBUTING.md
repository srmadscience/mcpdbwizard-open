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

The configs under Propfiles/ introspect Oracle schemas whose DDL is not published: it carried
the table and package layout of third-party systems, which is not ours to hand out. So the
generated-code harnesses -- the tier that regenerates from every config and drives the result
against a live database -- have nothing to point at until you supply schemas of your own.

Nothing else is affected. The generator, the runtime library and the database-free suite are
complete and self-contained, and Scripts/check_provisioning.sh will name the exact objects a
given config expects, which is the starting point if you want to build a fixture.

examples/generated-output/ shows what the generator emits, without needing a database at all.

## Licence

Contributions are accepted under the Apache License 2.0, the licence of this project.

## Before opening a pull request

    Scripts/export/check-export-clean.sh

This refuses to pass if a private host, a credential or a jar has crept in. It runs against
the whole tree, so it catches a stray connection string in a test fixture as readily as one
in a script.
