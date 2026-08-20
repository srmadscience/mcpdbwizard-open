#!/bin/sh
#
# check-export-clean.sh -- refuse to publish a tree that still contains anything private.
#
# This is a GATE, not a scrubber. It never edits a file. It reports every hit and exits
# non-zero, so the fix has to happen in the real repository where it stays fixed. A
# scrubber that rewrites on the way out would let the working tree stay dirty forever
# and would turn a missed pattern into a publication that cannot be retracted.
#
# Checks, in order:
#   1. every extended-regex in Scripts/export/denylist.txt
#   2. any *.jar (Oracle's driver is not redistributable, and no jar belongs in source)
#   3. the two files that must exist as templates only
#   4. --staged only, three things the FILTER cannot promise on its own:
#        a. sql/ OtherDbs/ Propfiles/ Sqlfiles/ must all be ABSENT
#        b. nothing may extend the live-database harness base class
#        c. no shipped doc may point at a directory that was excluded
#
# Usage:
#   Scripts/export/check-export-clean.sh [tree] [--staged]
#       tree      defaults to the app module root
#       --staged  the tree is a staged export, so also run check 4. Off by default because
#                 the development repository legitimately CONTAINS the DDL -- asserting its
#                 absence there would fail every pre-commit run.
#
# Exit codes:  0 = clean          1 = at least one hit       2 = usage / setup error
#
set -u

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
APP_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)
TREE=$APP_ROOT
STAGED=0
for arg in "$@"; do
    case "$arg" in
        --staged) STAGED=1 ;;
        *)        TREE=$arg ;;
    esac
done
DENYLIST="$SCRIPT_DIR/denylist.txt"

if [ ! -d "$TREE" ]; then
    echo "ERROR: no such tree: $TREE" >&2
    exit 2
fi
if [ ! -f "$DENYLIST" ]; then
    echo "ERROR: no denylist at $DENYLIST" >&2
    exit 2
fi

echo "=================================================================="
echo "  export leak check"
echo "  tree     : $TREE"
echo "  denylist : $DENYLIST"
echo "=================================================================="

hits=0

# Files to search: everything that would actually ship. Directories named in
# exclude.txt are skipped so a standalone run over the source tree reports the same
# result as a run over the staged tree -- otherwise legacy/ and prototypes/, which are
# never exported, would drown the real hits in noise.
#
# The denylist is skipped because it necessarily CONTAINS every pattern; matching itself
# would make the gate permanently red.
EXCLUDE_DIRS=$(sed -n 's|^\([A-Za-z][A-Za-z0-9_-]*\)/$|\1|p' "$SCRIPT_DIR/exclude.txt" | tr '\n' ' ')
prune="-name target -o -name out -o -name .git -o -name .idea"
for d in $EXCLUDE_DIRS; do
    prune="$prune -o -name $d"
done

# exclude.txt also names individual FILES (the real box inventory). They are not part of
# the export either, so scanning them would report a leak that could never ship -- and
# would train the reader to ignore this output, which is worse than not checking.
EXCLUDE_FILES=$(sed -n 's|^\([A-Za-z][^/]*/.*[^/]\)$|\1|p' "$SCRIPT_DIR/exclude.txt" \
                | grep -v '[*]' || true)

files=$(find "$TREE" \
    -type d \( $prune \) -prune -o \
    -type f -print 2>/dev/null | grep -v '/Scripts/export/denylist.txt$')

for ef in $EXCLUDE_FILES; do
    files=$(echo "$files" | grep -vF "/$ef" || true)
done

# ---- 1. denylist patterns --------------------------------------------------
while IFS= read -r pattern; do
    case "$pattern" in
        ''|\#*) continue ;;
    esac
    # -I skips binary files; -n gives a clickable location.
    found=$(echo "$files" | tr '\n' '\0' | xargs -0 grep -InE -- "$pattern" 2>/dev/null)
    if [ -n "$found" ]; then
        count=$(echo "$found" | wc -l | tr -d ' ')
        echo
        echo "  LEAK  /$pattern/  -- $count hit(s):"
        echo "$found" | head -10 | sed "s|$TREE/|      |"
        if [ "$count" -gt 10 ]; then
            echo "      ... and $((count - 10)) more"
        fi
        hits=$((hits + count))
    fi
done < "$DENYLIST"

# ---- 2. stray jars ---------------------------------------------------------
jars=$(echo "$files" | grep -E '\.jar$' || true)
if [ -n "$jars" ]; then
    echo
    echo "  LEAK  *.jar present (Oracle's driver is not redistributable):"
    echo "$jars" | sed "s|$TREE/|      |"
    hits=$((hits + $(echo "$jars" | wc -l | tr -d ' ')))
fi

# ---- 3. template-only files ------------------------------------------------
for real in "Scripts/tns/tnsnames.ora" "src/test/resources/test-boxes.properties"; do
    # Present is fine in a WORKING copy, where the file is gitignored and excluded from
    # staging -- that is the whole design. It is only a leak when it is present and
    # trackable, i.e. in a staged tree or after someone removed the ignore rule.
    if [ -f "$TREE/$real" ] && ! git -C "$TREE" check-ignore -q "$real" 2>/dev/null; then
        echo
        echo "  LEAK  $real is present and not ignored -- only its .template may ship"
        hits=$((hits + 1))
    fi
    if [ ! -f "$TREE/$real.template" ]; then
        echo
        echo "  MISSING  $real.template -- a clone would have nothing to copy from"
        hits=$((hits + 1))
    fi
done

# ---- 4. the DDL must be absent from a staged tree ---------------------------
# exclude.txt is a FILTER: it drops what it is told to drop. This is an ASSERTION, and the two
# fail differently. A filter goes quiet when someone edits it, renames a directory, or adds a
# new one -- and the result still exports cleanly, because nothing ever checks the outcome.
#
# The DDL is the one exclusion where a silent miss is unrecoverable: it carries third-party
# schema structure, and a push cannot be taken back. So the outcome is checked, not just the
# rule that is supposed to produce it.
if [ "$STAGED" = 1 ]; then
    # A config ENUMERATES the schema it introspects, so the configs and the statement library
    # do not ship at all. Asserted as ABSENCE of the directories, not as absence of a USER field:
    # keying on USER was tried and leaked, because that field says who a config logs in as, not
    # whose objects it names.
    # Structural, not by name: a live-DB harness drives generated DAOs against the private
    # schemas and names their objects. exclude.txt lists the ones in the ordinary test tree by
    # hand, because that package also holds database-free tests that DO ship -- and a hand list
    # goes stale. What a new harness cannot avoid is extending the base class.
    harn=$(grep -rl 'extends AbstractLiveDbHarness' "$TREE/src" 2>/dev/null | wc -l | tr -d ' ')
    if [ "${harn:-0}" != 0 ]; then
        echo
        echo "  LEAK  $harn shipped file(s) extend AbstractLiveDbHarness -- live-DB harnesses name"
        echo "        the private schemas' tables and routines, and cannot run in a clone."
        grep -rl 'extends AbstractLiveDbHarness' "$TREE/src" 2>/dev/null | sed "s|$TREE/|        |"
        hits=$((hits + 1))
    fi

    # Documentation that points at something we removed. Not pedantry: a README describing a
    # directory the reader does not have reads as a BROKEN REPOSITORY rather than as a deliberate
    # omission, and it is the first thing a visitor sees. Caught after the first publish, having
    # been fixed in CLAUDE.md and missed in README.md and CONTRIBUTING.md.
    for doc in README.md CONTRIBUTING.md CLAUDE.md; do
        [ -f "$TREE/$doc" ] || continue
        for gone in sql OtherDbs Propfiles Sqlfiles; do
            [ -e "$TREE/$gone" ] && continue
            # A doc that EXPLAINS the absence is doing the right thing -- app/CLAUDE.md and
            # README.md both name these directories in order to say they are not here, which is
            # exactly the reference a reader needs. Only an UNEXPLAINED mention is stale, so a
            # doc carrying the explanation is exempt. Without this the check punishes the fix.
            if grep -qE 'not (in|part of) th(is|e published) repositor' "$TREE/$doc" 2>/dev/null; then
                continue
            fi
            if grep -q "$gone/" "$TREE/$doc" 2>/dev/null; then
                echo
                echo "  STALE  $doc mentions $gone/ , which this tree does not contain."
                echo "         Reword it -- a reader cannot tell an omission from a broken repo."
                hits=$((hits + 1))
            fi
        done
    done

    for ddl in sql OtherDbs Propfiles Sqlfiles; do
        if [ -e "$TREE/$ddl" ]; then
            echo
            echo "  LEAK  $ddl/ is present in a STAGED tree -- it carries private schema names."
            echo "        exclude.txt should have removed it. Something changed there, or the"
            echo "        directory arrived by another route. Do not delete it by hand and"
            echo "        re-run: find out why the exclusion stopped working."
            hits=$((hits + 1))
        fi
    done
fi

echo
if [ "$hits" -gt 0 ]; then
    echo "=================================================================="
    echo "  EXPORT BLOCKED: $hits problem(s)."
    echo "  Fix them in the repository -- do not loosen the denylist, and do"
    echo "  not add the file to exclude.txt to make this go away."
    echo "=================================================================="
    exit 1
fi
echo "EXPORT CHECK PASSED -- nothing private found."
