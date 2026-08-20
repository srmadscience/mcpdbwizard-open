#!/bin/sh
#
# export-open-source.sh -- stage the app module as a publishable, fresh-history repository.
#
# WHY THE FIRST PUBLISH HAD A FRESH HISTORY, rather than a filter of the existing one:
# Oracle's non-redistributable ojdbc5 jar entered at "Mavenize project", 164 commits back out
# of 169, and live hosts appear across 28 commits. Filtering would have rewritten ~97% of
# history, changed every SHA (breaking the commit ledger the project notes rely on) and still
# required a force-push -- all to reach a state a fresh initial commit reaches by construction,
# with nothing left to miss. The development repository keeps its provenance; the published one
# simply never contained any of it.
#
# THAT ARGUMENT IS ABOUT COMMIT ONE, AND IT IS FINISHED. Re-running `git init` on every export
# would rewrite published history and force a --force push, running over anyone who forked or
# opened a pull request. From the second export onwards this script works inside a CLONE of the
# published repository and adds an ordinary commit. See step 5.
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
#   6. clone (or reuse) the PUBLISHED repo, rsync the staged tree over it with --delete,
#      and add ONE ordinary commit. Fast-forward: no force-push, forks and PRs survive.
#   7. report what changed and what to do next. It does NOT push.
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

# The tree is built in a TEMPORARY directory and copied into the published clone only after
# the leak gate and the build have both passed. Staging straight into the clone would put a
# leak into the working tree of the repository you are about to push, and the gate's "left for
# inspection" would mean "left where it can be committed by accident".
STAGE=$(mktemp -d "${TMPDIR:-/tmp}/mcpdbwizard-export.XXXXXX") || exit 2
trap 'rm -rf "$STAGE"' EXIT INT TERM

# ---- 2. stage --------------------------------------------------------------
if command -v rsync >/dev/null 2>&1; then
    rsync -a --exclude-from="$SCRIPT_DIR/exclude.txt" "$APP_ROOT/" "$STAGE/" || exit 2
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
python3 - "$STAGE/pom.xml" "$PARENT_GROUP" "$PARENT_VERSION" <<'PYEOF'
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
    mkdir -p "$STAGE/examples"
    cp -R "$APP_ROOT/sql/Demo/Src" "$STAGE/examples/generated-output" || exit 2
    find "$STAGE/examples/generated-output" -name '.DS_Store' -delete 2>/dev/null
    cat > "$STAGE/examples/README.md" <<'EOF'
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
    echo "  kept sql/Demo/Src as examples/generated-output ($(find "$STAGE/examples/generated-output" -type f | wc -l | tr -d ' ') files)"
fi

# ---- 3. gate ---------------------------------------------------------------
echo
if ! "$SCRIPT_DIR/check-export-clean.sh" "$STAGE" --staged; then
    echo
    echo "Export aborted. Nothing has touched the published repository."
    echo "Fix the hits in the SOURCE repository, commit, and re-run."
    exit 1
fi

# ---- 4. boilerplate --------------------------------------------------------
# LICENSE and NOTICE are committed in the repository (Apache-2.0), so they arrive with
# the staged copy. Refuse to export without them rather than inventing a placeholder:
# code shipped without a licence is visible, not open source, and nobody may use it.
for required in LICENSE NOTICE; do
    if [ ! -f "$STAGE/$required" ]; then
        echo
        echo "ERROR: $required is missing from the staged tree."
        echo "Publishing without it would leave the code legally unusable."
        exit 2
    fi
done
echo "  LICENSE + NOTICE present ($(wc -l < "$STAGE/LICENSE" | tr -d ' ') lines)"

cat > "$STAGE/CONTRIBUTING.md" <<'EOF'
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
EOF

# ---- 4b. an ignore file, which the export has never had --------------------
# rsync stages from app/, so the repository root's .gitignore was never in scope and the
# published tree shipped without one. A contributor's first `mvn test` then fills `git status`
# with target/, and the first thing they learn about the project is that it does not ignore its
# own build output. The private tree's rules for anything under app/, minus the paths that no
# longer exist here.
cat > "$STAGE/.gitignore" <<'EOF'
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
    if (cd "$STAGE" && mvn -q -B test > "$STAGE/export-build.log" 2>&1); then
        echo "  BUILD OK -- the exported tree compiles and its tests pass without sql/ or OtherDbs/"
        rm -f "$STAGE/export-build.log"
    else
        echo
        echo "ERROR: the staged tree does not build. Last 40 lines:"
        tail -40 "$STAGE/export-build.log"
        echo
        echo "Full log: $STAGE/export-build.log"
        echo "Nothing has touched the published repository. Fix it in the SOURCE repo."
        exit 1
    fi
    # Build output must not become the first commit.
    rm -rf "$STAGE/target"
fi

# ---- 5. publish onto the EXISTING history -----------------------------------
# The fresh `git init` this script used to do was right ONCE and wrong afterwards. The reasoning
# at the top -- do not filter 169 private commits, start clean instead -- is an argument about
# the FIRST publish, and it has already done its job: commit 1 is clean by construction. Doing
# it again on every export would rewrite published history, so every update would need a
# force-push, and anyone who forked or opened a pull request would be run over.
#
# So: work inside a clone of the published repository and add an ordinary commit. Fast-forward,
# no force, forks and PRs keep working.
#
# WHAT WE DELIBERATELY DO NOT DO is replay private commits one by one. That needs every commit
# filtered (the thing rejected above) and it would publish the commit MESSAGES, which name the
# private schemas outright. One public commit per export, naming only the private SHA.
if [ ! -d "$DEST/.git" ]; then
    if [ -e "$DEST" ]; then
        echo "ERROR: $DEST exists but is not a git repository." >&2
        echo "       Move it aside, or name a different destination." >&2
        exit 2
    fi
    echo "  cloning $PUBLIC_REPO"
    # An EMPTY remote clones fine and yields a repo with no commits, so the very first publish
    # and every later one take the same path -- the commit below simply becomes the initial one.
    git clone -q "$PUBLIC_REPO" "$DEST" 2>/dev/null || {
        echo "ERROR: could not clone $PUBLIC_REPO into $DEST." >&2
        echo "       Check network and access. To publish somewhere else, pass a destination" >&2
        echo "       and set PUBLIC_REPO." >&2
        exit 2; }
else
    echo "  reusing the clone at $DEST"
    git -C "$DEST" pull --ff-only -q 2>/dev/null \
        || echo "  (nothing to pull, or no upstream yet)"
fi

# --delete IS THE POINT, not a tidiness flag. Without it a file removed from the private repo
# quietly SURVIVES in the published one: the private tree looks clean, the public tree still
# serves the thing you withdrew, and nothing reports a difference. That is exactly the failure
# this export exists to prevent.
#
# .git is excluded for the obvious reason. export-build.log is excluded because the build step
# writes it into the staging dir on failure and it must never become a published file.
rsync -a --delete \
      --exclude '.git/' \
      --exclude 'export-build.log' \
      "$STAGE/" "$DEST/" || exit 2

cd "$DEST" || exit 2
git add -A

if git diff --cached --quiet 2>/dev/null; then
    echo
    echo "  NO CHANGES -- the published tree already matches this commit. Nothing to do."
    PUBLISHED=unchanged
else
    CHANGED=$(git diff --cached --name-only | wc -l | tr -d ' ')
    if git rev-parse HEAD >/dev/null 2>&1; then
        SUBJECT="Sync from the development repository at $SRC_COMMIT"
        BODY="$CHANGED file(s) changed.

The public history is one commit per export, not a replay of private commits: replaying
would mean filtering every one, and would publish commit messages that name schemas this
repository deliberately excludes."
    else
        SUBJECT="Initial public release"
        BODY="MCPDBWizard: an Oracle PL/SQL introspector and Java code generator that emits
JDBC wrappers, and a Model Context Protocol server exposing them as typed agent tools.

Exported from the development repository at $SRC_COMMIT with a fresh history. The history
was not filtered: it is new, so the published tree has never contained Oracle's
non-redistributable JDBC driver, any live host, or any credential."
    fi
    git -c user.name="${GIT_AUTHOR_NAME:-$(git -C "$REPO_ROOT" config user.name)}" \
        -c user.email="${GIT_AUTHOR_EMAIL:-$(git -C "$REPO_ROOT" config user.email)}" \
        commit -q -m "$SUBJECT" -m "$BODY" || exit 1
    echo "  committed: $SUBJECT ($CHANGED file(s))"
    PUBLISHED=committed
fi

# ---- 6. report -------------------------------------------------------------
files=$(find "$DEST" -type f -not -path '*/.git/*' | wc -l | tr -d ' ')
size=$(du -sh "$DEST" 2>/dev/null | cut -f1)
commits=$(git -C "$DEST" rev-list --count HEAD 2>/dev/null || echo 0)
ahead=$(git -C "$DEST" rev-list --count @{u}..HEAD 2>/dev/null || echo "$commits")

echo
echo "=================================================================="
echo "  STAGED  ($PUBLISHED)"
echo "  from    : $SRC_COMMIT (development repo)"
echo "  to      : $DEST"
echo "  content : $files files, $size, $commits commit(s) total"
echo "  unpushed: $ahead commit(s)"
echo "=================================================================="
echo
echo "  Excluded (see Scripts/export/exclude.txt):"
sed -n 's/^\([^#][^ ]*\)$/    \1/p' "$SCRIPT_DIR/exclude.txt" | grep -v '^ *$'

if [ "$PUBLISHED" = unchanged ]; then
    echo
    echo "  Nothing to push."
    exit 0
fi

echo
echo "  NOT PUSHED. Review, then:"
echo "         cd $DEST"
echo "         git log -1 --stat        # what this export changes"
echo "         git push"
echo
echo "  This is an ordinary fast-forward onto the published history -- no --force, and"
echo "  anyone who forked or opened a pull request is unaffected. If a push is ever"
echo "  REFUSED as non-fast-forward, someone committed to the public repo directly:"
echo "  pull, look at what they did, and re-run this rather than forcing over it."
