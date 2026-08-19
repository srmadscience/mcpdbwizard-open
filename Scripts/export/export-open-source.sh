#!/bin/sh
#
# export-open-source.sh -- stage the app module as a publishable, fresh-history repository.
#
# WHY A FRESH HISTORY, not a filter of the existing one: Oracle's non-redistributable
# ojdbc5 jar entered at "Mavenize project", 164 commits back out of 169, and live hosts
# appear across 28 commits. Filtering would rewrite ~97% of history, change every SHA
# (breaking the commit ledger the project notes rely on) and require a force-push -- all
# to reach a state a fresh initial commit reaches by construction, with nothing left to
# miss. The private repository keeps its provenance; the public one simply never
# contained any of it.
#
# Steps:
#   1. refuse to run on a dirty working tree (the export must be reproducible)
#   2. stage app/ into a temp dir, applying Scripts/export/exclude.txt -- which drops the
#      test-schema DDL (sql/, OtherDbs/) and its loader scripts. That removal is NOT
#      negotiable: those trees carry third-party schema layouts, not just our fixtures.
#      See exclude.txt for which, and why the build does not need them.
#   3. run the leak gate over the STAGED tree -- abort on any hit
#   4. require LICENSE + NOTICE, and write CONTRIBUTING + .gitignore
#   5. BUILD AND TEST the staged tree -- abort if it does not pass
#   6. git init + one initial commit
#   7. report what shipped, what did not, and what to do next
#
# Usage:
#   Scripts/export/export-open-source.sh [destination]
#       destination defaults to ../mcpdbwizard-open next to the repository.
#
# Exit codes:  0 = exported     1 = blocked by the leak gate     2 = usage / setup error
#
set -u

# Where the export is published. Recorded here rather than in a person's shell history so the
# closing instructions can print it, and so there is one place to change it. The script NEVER
# pushes -- see the note it prints at the end about the fresh history.
PUBLIC_REPO=${PUBLIC_REPO:-https://github.com/srmadscience/mcpdbwizard-open.git}

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
APP_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)
REPO_ROOT=$(cd "$APP_ROOT/.." && pwd)
DEST=${1:-$REPO_ROOT/../mcpdbwizard-open}

echo "=================================================================="
echo "  open-source export"
echo "  source      : $APP_ROOT"
echo "  destination : $DEST"
echo "=================================================================="

# ---- 1. reproducibility ----------------------------------------------------
if [ -n "$(git -C "$REPO_ROOT" status --porcelain 2>/dev/null)" ]; then
    echo
    echo "ERROR: the working tree has uncommitted changes."
    echo "An export must be reproducible from a known commit -- commit or stash first."
    exit 2
fi
SRC_COMMIT=$(git -C "$REPO_ROOT" rev-parse --short HEAD 2>/dev/null || echo unknown)

if [ -e "$DEST" ]; then
    echo
    echo "ERROR: $DEST already exists. Remove it or name a different destination."
    exit 2
fi

# ---- 2. stage --------------------------------------------------------------
mkdir -p "$DEST" || exit 2
if command -v rsync >/dev/null 2>&1; then
    rsync -a --exclude-from="$SCRIPT_DIR/exclude.txt" "$APP_ROOT/" "$DEST/" || exit 2
else
    echo "ERROR: rsync not found (needed to apply exclude.txt)." >&2
    exit 2
fi

# The exporter's own denylist/exclude belong in the published repo -- a contributor
# needs the gate as much as we do -- but the staging script's paths are repo-relative
# and work unchanged, so nothing to rewrite.

# ---- 2b. flatten the parent POM -------------------------------------------
# app/pom.xml inherits from the private reactor's mcpdbwizard-parent via
# <relativePath>../pom.xml</relativePath>. That parent lists app AND web as modules, so
# it cannot ship; and it is an unpublished SNAPSHOT, so a stranger's clone cannot resolve
# it either. It builds on OUR machine only because the artifact is cached in ~/.m2 --
# exactly the kind of defect that would surface as "works for me, broken for everyone".
#
# The parent contributes only groupId, version and two properties, and the child already
# redefines both properties, so inlining groupId/version is a complete substitution.
PARENT_GROUP=$(sed -n 's|.*<groupId>\(.*\)</groupId>.*|\1|p' "$REPO_ROOT/pom.xml" | head -1)
PARENT_VERSION=$(sed -n 's|.*<version>\(.*\)</version>.*|\1|p' "$REPO_ROOT/pom.xml" | head -1)
python3 - "$DEST/pom.xml" "$PARENT_GROUP" "$PARENT_VERSION" <<'PYEOF'
import re, sys
path, group, version = sys.argv[1], sys.argv[2], sys.argv[3]
s = open(path, encoding="utf-8").read()
# Match a real ELEMENT, not the literal string. app/pom.xml carries a comment reading
# "DELIBERATELY HAS NO <parent>", which a substring test reads as a parent being present --
# so this step fired on a POM that had already been flattened, appended a second groupId
# and version, and produced a POM Maven refuses to parse ("Duplicated tag: 'groupId'").
# The export was unbuildable from the day app/ was made standalone until 2026-08-17, and
# nothing caught it because nothing had ever run mvn inside the exported tree.
if not re.search(r"<parent>.*?</parent>", s, re.S):
    print("  no <parent> to flatten -- app/pom.xml already stands alone")
    sys.exit(0)
s = re.sub(r'\n\s*<parent>.*?</parent>\n', '\n', s, count=1, flags=re.S)
s = s.replace("    <artifactId>mcpdbwizard-app</artifactId>",
              f"    <groupId>{group}</groupId>\n"
              f"    <artifactId>mcpdbwizard-app</artifactId>\n"
              f"    <version>{version}</version>", 1)
open(path, "w", encoding="utf-8").write(s)
print(f"  flattened parent POM -> standalone {group}:mcpdbwizard-app:{version}")
PYEOF

# ---- 2c. keep the one thing in sql/ that is not DDL ------------------------
# exclude.txt drops sql/ wholesale, which is right -- but sql/Demo/Src is not DDL. It is 106
# generated .java files plus the extraObjects.sql the generator wrote, a checked-in record of
# what this generator actually emits. For an open-source code generator that is the single most
# useful artefact in the tree: it answers "what do I get?" without an Oracle instance, which is
# the one question a visitor cannot otherwise answer at all.
#
# It moves to examples/generated-output/ rather than staying put, because a surviving sql/
# directory containing no SQL is its own small confusion.
#
# The DDL that produced it does NOT come with it, so this is a read-only illustration and the
# README says so. Do not "restore" mcpdbwizard_demo_ddl.sql to make it runnable -- the demo
# schema is ORINDADEMO, it lives in sql/, and sql/ is what we are not publishing.
if [ -d "$APP_ROOT/sql/Demo/Src" ]; then
    mkdir -p "$DEST/examples"
    cp -R "$APP_ROOT/sql/Demo/Src" "$DEST/examples/generated-output" || exit 2
    find "$DEST/examples/generated-output" -name '.DS_Store' -delete 2>/dev/null
    cat > "$DEST/examples/README.md" <<'EOF'
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
EOF
    echo "  kept sql/Demo/Src as examples/generated-output ($(find "$DEST/examples/generated-output" -type f | wc -l | tr -d ' ') files)"
fi

# ---- 3. gate ---------------------------------------------------------------
echo
if ! "$SCRIPT_DIR/check-export-clean.sh" "$DEST" --staged; then
    echo
    echo "Export aborted; staged tree left at $DEST for inspection."
    echo "Fix the hits in the SOURCE repository, commit, and re-run."
    exit 1
fi

# ---- 4. boilerplate --------------------------------------------------------
# LICENSE and NOTICE are committed in the repository (Apache-2.0), so they arrive with
# the staged copy. Refuse to export without them rather than inventing a placeholder:
# code shipped without a licence is visible, not open source, and nobody may use it.
for required in LICENSE NOTICE; do
    if [ ! -f "$DEST/$required" ]; then
        echo
        echo "ERROR: $required is missing from the staged tree."
        echo "Publishing without it would leave the code legally unusable."
        exit 2
    fi
done
echo "  LICENSE + NOTICE present ($(wc -l < "$DEST/LICENSE" | tr -d ' ') lines)"

cat > "$DEST/CONTRIBUTING.md" <<'EOF'
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
EOF

# ---- 4b. an ignore file, which the export has never had --------------------
# rsync stages from app/, so the repository root's .gitignore was never in scope and the
# published tree shipped without one. A contributor's first `mvn test` then fills `git status`
# with target/, and the first thing they learn about the project is that it does not ignore its
# own build output. The private tree's rules for anything under app/, minus the paths that no
# longer exist here.
cat > "$DEST/.gitignore" <<'EOF'
# --- build output ---
target/
out/
*.class
dependency-reduced-pom.xml

# --- jars are never committed (Oracle's driver is not redistributable) ---
*.jar
*.war
*.ear
lib/*.jar
repo/

# --- local machine / editor ---
.DS_Store
.idea/
*.iml
*.swp
*.swo
*~
hs_err_pid*
replay_pid*

# --- your own connection details: these are the files CONTRIBUTING tells you to create ---
src/test/resources/test-boxes.properties
Scripts/tns/tnsnames.ora
Scripts/boxes.env

# --- Oracle client noise ---
*.log
*.lst
afiedt.buf
sqlnet.log
*.trc
EOF
echo "  wrote .gitignore"

# ---- 4c. PROVE IT BUILDS ----------------------------------------------------
# The whole point of removing the DDL is that the result still builds, and this is the only
# step that can say so. It is not paranoia: app/ was made standalone on 2026-08-07 and the
# export was UNBUILDABLE from that day until 2026-08-17 -- a duplicated <groupId> from the POM
# flatten -- and nothing noticed, because nothing had ever run Maven inside an exported tree.
# A gate that reads the tree cannot catch a POM that does not parse.
#
# `mvn test`, not `mvn compile`: the database-free suite is what a stranger runs first, and a
# test that hard-fails on a missing fixture directory is exactly the kind of breakage removing
# sql/ could introduce.
if [ "${SKIP_EXPORT_BUILD:-0}" = 1 ]; then
    echo "  SKIPPING the build check (SKIP_EXPORT_BUILD=1) -- the export is NOT verified"
elif ! command -v mvn >/dev/null 2>&1; then
    echo "  WARNING: mvn not found, so the export is NOT verified buildable."
else
    echo
    echo "  building the staged tree (this is the check that the DDL was really unnecessary)"
    if (cd "$DEST" && mvn -q -B test > "$DEST/export-build.log" 2>&1); then
        echo "  BUILD OK -- the exported tree compiles and its tests pass without sql/ or OtherDbs/"
        rm -f "$DEST/export-build.log"
    else
        echo
        echo "ERROR: the staged tree does not build. Last 40 lines:"
        tail -40 "$DEST/export-build.log"
        echo
        echo "Full log: $DEST/export-build.log"
        echo "Staged tree left at $DEST for inspection. Fix it in the SOURCE repository."
        exit 1
    fi
    # Build output must not become the first commit.
    rm -rf "$DEST/target"
fi

# ---- 5. fresh history ------------------------------------------------------
cd "$DEST" || exit 2
git init -q
git add -A
git -c user.name="${GIT_AUTHOR_NAME:-$(git -C "$REPO_ROOT" config user.name)}" \
    -c user.email="${GIT_AUTHOR_EMAIL:-$(git -C "$REPO_ROOT" config user.email)}" \
    commit -q -m "Initial public release

MCPDBWizard: an Oracle PL/SQL introspector and Java code generator
that emits JDBC wrappers, and a Model Context Protocol server exposing them as
typed agent tools.

Exported from the private repository at $SRC_COMMIT with a fresh history. The
history was not filtered: it is new, so the published tree has never contained
Oracle's non-redistributable JDBC driver or any live host or credential."

# ---- 6. report -------------------------------------------------------------
files=$(find "$DEST" -type f -not -path '*/.git/*' | wc -l | tr -d ' ')
size=$(du -sh "$DEST" 2>/dev/null | cut -f1)

echo
echo "=================================================================="
echo "  EXPORTED"
echo "  from   : $SRC_COMMIT (private repo)"
echo "  to     : $DEST"
echo "  content: $files files, $size, 1 commit"
echo "=================================================================="
echo
echo "  Excluded (see Scripts/export/exclude.txt):"
sed -n 's/^\([^#][^ ]*\)$/    \1/p' "$SCRIPT_DIR/exclude.txt" | grep -v '^ *$'
echo
echo "  Before publishing:"
echo "    1. read README.md as a newcomer would"
echo "    2. confirm the DDL really is absent:"
echo "         ls sql OtherDbs            # both should say: No such file or directory"
echo "    3. push:"
echo "         cd $DEST"
echo "         git remote add origin $PUBLIC_REPO"
echo "         git branch -M main && git push -u origin main"
echo
echo "  NOTE: the first push to an EXISTING repository that already has commits will be"
echo "  refused. This export has a fresh history by design and cannot be merged with one --"
echo "  push --force is the only way to reconcile them, and it discards whatever is there."
