#!/bin/sh
#
# check-rename.sh -- assert the state of the com.orindasoft -> com.mcpdbwizard rename.
#
# Run from anywhere; it locates the repository itself. Exit 0 = every assertion holds.
#
# WHY THIS EXISTS RATHER THAN A GREP IN THE PLAN. Two of the assertions below are not
# "did we finish" checks at all -- they are guards against a specific way of breaking
# the repository that a bulk rename invites:
#
#   A case-insensitive substitution on "orinda" rewrites 393 copyright lines reading
#   "Copyright 2003-2026 ATB Consultancy Services Ltd (formerly Orinda Software Ltd,
#   Dublin, Ireland)", plus the "Portions Copyright (c) 1999 CodeSpooks/SpookyAction.com"
#   lines in five legacy files. Those are a CHAIN OF TITLE, not branding. They are
#   preserved deliberately because the repository holds no record of an assignment from
#   those entities, and rewriting them to match a marketing rename is the one edit in
#   this whole exercise that could do real damage.
#
# So every substitution in the rename is case-sensitive and token-anchored
# (com.orindasoft, OrindaBuild, orindasoft.com) -- never a bare case-insensitive
# "orinda" -- and this script asserts the chain of title is still the length it was.
#
# The phase-completion checks are marked PENDING until their phase lands, so this
# script is useful from phase 1 rather than only at the end.
#
# THIS FILE AND docs/rename-plan.md ARE EXCLUDED FROM THE BULK SUBSTITUTIONS, and must
# stay excluded. They are the two files that legitimately contain the OLD name -- one
# asserts its absence, the other explains the move -- so a tree-wide
# s/com.orindasoft/com.mcpdbwizard/ turns both into nonsense reading
# "com.mcpdbwizard -> com.mcpdbwizard". Phase 2 did exactly that and they were restored
# from git. Note the PATHS below are a different matter and DO track the move.
#
set -u

# THE REPOSITORY ROOT, WHICHEVER TREE THIS IS. Asking git is not fussiness: this script SHIPS,
# and the exported tree has a different shape. Here it sits at app/Scripts/check-rename.sh, so
# "$(dirname $0)/../.." was the root; the exporter flattens app/ to the top, so there it sits at
# Scripts/check-rename.sh and that same expression lands ONE LEVEL ABOVE the repository -- in
# whatever directory happens to contain the checkout. Every count then ran somewhere else and
# came back 0 or empty, and SEVEN assertions failed on a clean public checkout: the one script
# that looks like "prove this tree is consistent" told a contributor it was not.
#
# The fallback keeps it working outside a git checkout (an unpacked tarball), where the old
# expression is still the best guess available.
REPO=$(git -C "$(dirname "$0")" rev-parse --show-toplevel 2>/dev/null)     || REPO=""
[ -n "$REPO" ] || REPO=$(cd "$(dirname "$0")/../.." && pwd)
cd "$REPO" || exit 2

# Where this script lives RELATIVE TO THAT ROOT, for the self-exclusions below. It quotes the old
# package name and the copyright lines verbatim while explaining them, so counting it measures this
# script's own prose rather than the repository -- and an exclusion anchored on the development
# tree's path silently stops excluding anything in the exported one, which inflates every count by
# however many times this file says the thing it is counting.
SELF=${0#"$REPO"/}
case "$SELF" in
    /*|"$0") SELF=$(cd "$(dirname "$0")" && pwd)/$(basename "$0"); SELF=${SELF#"$REPO"/} ;;
esac

# WHICH TREE THIS IS, and the two things that follow from it.
#
# APP is the prefix the app module's sources sit behind: "app/" here, nothing in the exported
# tree, which the exporter flattens to the top. Without it every app/... path below simply does
# not exist there, grep says so on stderr, and the assertion reports an EMPTY count -- which reads
# as "0 of something" rather than as "this did not run".
#
# PARTIAL says the web module is absent, which is the whole difference between this repository and
# the published one. It decides whether a TOTAL can be asserted at all: see skip_on_partial.
if [ -d app/src/main/java ]; then APP=app/; else APP=""; fi
if [ -d web/src/main/java ]; then PARTIAL=no; else PARTIAL=yes; fi

# A COUNT OF THE WHOLE REPOSITORY CANNOT BE ASSERTED AGAINST A SUBSET OF IT, and pretending
# otherwise is worse than not checking. The published tree carries the app module and neither web/
# nor the private directories, so every total below -- 493 copyright lines, 6 portions notices, 8
# history notes -- is arithmetic about a tree the reader does not have. Reported as skipped, with
# the reason, rather than failed: a contributor running this on a clean checkout should not be told
# their tree is broken by a number that was never about it.
#
# ZERO assertions are NOT skipped, and the distinction is the point. "This string appears nowhere"
# stays true and stays meaningful on any subset -- so everything that must be absent is still
# checked in the published tree, which is where absence actually matters.
skip_on_partial() {
    printf '  skipped %-46s %s\n' "$1" "$2"
}

FAILED=0

# Count occurrences (not files) of a fixed string across everything a commit would
# contain -- tracked files AND new untracked ones -- excluding the two documents that
# describe the rename. Both quote the old package name and the copyright lines verbatim
# while explaining them, so counting those measures this script's own prose rather than
# the repository.
#
# WHY NOT PLAIN `git grep`: it ignores untracked files, so a new source file is invisible
# until it is committed and then appears all at once. That turned a correct baseline into
# a phantom failure TWICE during this rename -- once in phase 2 and again in phase 6, both
# times reading exactly like the damage these assertions exist to catch. Counting
# ls-files --cached --others removes the blind spot: what the script sees is what the
# next commit will contain.
count() {
    git ls-files --cached --others --exclude-standard \
        | grep -v -e "^$SELF$" -e '^docs/rename-plan\.md$' \
        | tr '\n' '\0' \
        | xargs -0 grep -cF "$1" 2>/dev/null \
        | awk -F: '{s+=$NF} END {print s+0}'
}

count_re() {
    git grep -cE "$1" -- . 2>/dev/null | awk -F: '{s+=$NF} END {print s+0}'
}

expect() {
    label=$1; want=$2; got=$3
    if [ "$want" = "$got" ]; then
        printf '  ok      %-46s %s\n' "$label" "$got"
    else
        printf '  FAILED  %-46s want %s, got %s\n' "$label" "$want" "$got"
        FAILED=$((FAILED + 1))
    fi
}

pending() {
    printf '  pending %-46s %s (phase %s)\n' "$1" "$2" "$3"
}

echo "=================================================================="
echo "  rename check -- $(git rev-parse --short HEAD 2>/dev/null || echo '?')"
echo "=================================================================="
echo
echo "1. Chain of title -- MUST NOT CHANGE (see the header above)"

# This counts the copyright LINE, not the bare company name, and the difference matters.
# The bare name legitimately went DOWN in phase 1: two emission sites used to write
# "<product> is made by Orinda Software Ltd, Dublin, Ireland" into customers' files, and
# that is vendor attribution rather than copyright -- it correctly became the current
# vendor. Counting the bare name would have called that a regression.
#
# Baseline 346 at 231d11f; 348 after phase 1, which added the line to package.html (whose
# proprietary notice was replaced) and to Namer's param_vendor_name javadoc; 350 after
# phase 5 added the web module's legacy-key mapper and its test, each carrying the standard
# header. Phases 2,
# 3, 4 and 6 left it untouched, which is what they should do -- moving a package, renaming
# an artifact or regenerating a demo tree must not so much as brush a copyright header.
#
# Phase 6 is worth noting: it replaced 11 checked-in generated files with 105, and this
# number did not move. Generated output carries no copyright header at all, exactly as
# NOTICE says it should.
#
# EXPECT TO UPDATE THIS NUMBER as later phases add or remove files -- but only ever with
# a diff in front of you showing which file moved and why. A number that drifts without
# explanation is the failure this assertion exists to catch.
# 350 -> 352 on 2026-08-08: two new db-free test classes, SchemaSelectsNothingTest and
# MetricsPortKeyTest, one header line each.
#
# 352 -> 360 on 2026-08-10: EIGHT new files, one header line each -- the MCP tool description arc
# (McpDescriptionKeysTest, McpParamTypeLabelTest, plus four in the WEB module: a controller, its
# test, a page-render test and a working-config test) and the load harness
# (Scripts/loadtest/McpLoad.java, Scripts/loadtest/mcp-load.sh). Verified two ways before moving the
# number: every one is a NEW file, and the three files that a -G diff also flagged
# (Function/Sequence/TableTableDataModel) were confirmed to hold exactly 1 line before and after --
# they appear only because the edits rewrote them wholesale, not because a header changed.
#
# NOTE the load harness carries the notice even though it lives under Scripts/. It is OUR code, so
# it does; Scripts/compile-shims deliberately does NOT, because an ATB copyright on a stand-in for
# another party's API would misstate authorship. Do not "tidy" the two into agreement.
#
# 360 -> 364 on 2026-08-11: FOUR new files, one header line each, and the reconciliation matters
# more than the number. THREE are the "description will never be used" work, all in the WEB module
# (a report class, its test, and a key-agreement test). The FOURTH is
# QuoteQualifiedNameTest, added back in c251d66 -- so this assertion had ALREADY been failing for
# two commits before anyone ran it, both of them pushed. That is the lesson worth keeping: the
# check is not part of `mvn test` and a green six-box estate says nothing about it, so it has to be
# run deliberately after any commit that adds a file. Verified with
# `git diff 7f3ed7f -G'formerly Orinda Software Ltd' --name-only` plus a per-file count showing
# exactly one notice line in each.
#
# 364 -> 366 on 2026-08-11: two new db-free test classes, one header line each --
# SqlFilesNamedButNotFoundTest and DanglingSqlFileCensusTest, both from the missing-SQL-file
# work (docs/missing-sql-file-plan.md). Verified by listing the untracked files and counting
# one notice in each; no existing file's header moved.
#
# 366 -> 405 on 2026-08-17: THIRTY-NINE new files, one header line each. Reconciled with
# `git diff f588202 HEAD -G'formerly Orinda Software Ltd' --name-only` and a per-file
# before/after count: 42 files flagged, 39 notice lines GAINED, 0 lost, and 0 existing file
# whose header count moved -- so every one is a new file, which is the only shape of change
# this assertion should ever accept without a closer look. By arc:
#   audit + spool (10)  SpoolCipher(+Test); in the WEB module, two settings/stats pairs, two
#                       page and failure tests, and an authentication listener(+Test)
#   inline SQL (6)      SqlInliner(+Test), InlineSqlWriteTest, InlineSqlTextTest,
#                       SqlStatementKeysTest, and one library test in the WEB module
#   container metrics (3), extra-TYPE check (3), run-on-start (2), protocol log (2)
#   2026-08-17 GUI/release work (7)  check-links.mjs, release.sh, and five in the WEB module:
#                       an interceptor, an unavailable-screen controller and three tests
#   remaining (6)       TRecordFieldCensus, AspDatatypeHintTest, RetiredWsRecordTypeTest,
#                       tz-collection-binding-plan.md, and two WEB module tests (a config-store
#                       write failure and the proxy end-to-end)
#
# THIS SAT FAILING FOR SIX DAYS AND ~39 FILES, which is worse than the two-commit lapse noted
# above and makes the same point louder: the number is only as good as the habit of running the
# script. It is now a phase of ./release.sh at the repository root, so a release run reports it
# rather than relying on someone remembering.
# 2026-08-21: 405 -> 448. Growth only, from files added since -- McpDates and its test, the TZ
# and date plan documents, bump-version.sh, the index-by work. Every one carries the notice
# because it is ours. A RISE is the benign direction here and is ordinary maintenance; a FALL
# would mean a file lost its notice, which is the thing this number exists to catch.
# 2026-08-21 (later the same day): 448 -> 451. Three files, one notice each -- the FK child-lookup
# description work: McpLookupDescriptionTest, a badge test in the WEB module, and its plan
# document. Checked
# to be purely ADDITIVE rather than trusting the direction: the set of files carrying the notice
# before and after differs by exactly those three, with none dropping out. That is the check worth
# doing, because four gained and one lost also reads as +3 and is the failure this number exists
# to catch.
# 2026-08-25: 451 -> 463. TWELVE new files, one notice each, from the 2.0.3 arcs -- the
# Runtime tool-listing work (GeneratedMcpToolListingShapeTest here, plus a listing class(+Test),
# a stub and two render tests in the WEB module), Prometheus service discovery (four more WEB
# files: a controller(+Test), a targets class, a scrape entry point and a security test), and one
# WEB start-up report test.
# Checked the way the 2026-08-21 entry says to, not by the direction: a per-file count at
# 33f460f and at HEAD gives 12 files GAINED, 0 LOST, and 0 file present in both whose count
# moved. A set difference of exactly the new files is the only shape that means "additive";
# +13 and -1 also reads as +12 and is the failure this number exists to catch.
# 2026-08-26: 463 -> 479. SEVENTEEN new files MINUS ONE deleted -- the RPC-level load harness
# moving out of Scripts/ and into the app module as com.mcpdbwizard.loadtest (eleven main
# classes, six db-free test classes), replacing Scripts/loadtest/McpLoad.java, which carried one
# notice and is gone.
# THE NET IS THE PART TO CHECK HERE, because this is the first entry where the number moved for
# two reasons at once and +16 could equally be "sixteen files added" or "seventeen added and one
# that quietly lost its header". Verified as the 2026-08-21 entry says: the set of files carrying
# the notice differs by exactly those eighteen, each new file holds exactly ONE notice line, and
# `git diff -G'formerly Orinda Software Ltd' --name-only` flags Scripts/loadtest/McpLoad.java and
# nothing else -- so no surviving file's header moved.
# 2026-08-26 (later the same day): 479 -> 480. ONE new file, one notice -- the live harness
# TMcpIndexByDateTime, which round-trips a DATE and an unzoned TIMESTAMP index-by table through
# the MCP text path that stopped being gated on the same day. Nothing was deleted and nothing was
# renamed, so unlike the entry above this is a plain +1 with no netting to see through. Verified
# the same way regardless: the new file holds exactly ONE notice line, and
# `git diff -G'formerly Orinda Software Ltd' --name-only` lists nothing at all -- so no surviving
# file's header moved to make up the number.
# 2026-08-26 (third move the same day): 480 -> 483. THREE new files, one notice each -- McpBinary
# and McpBinaryTest (the base64/hex conversion an index-by RAW needs to cross MCP, and its db-free
# tests) and TMcpIndexByRaw, the live harness that round-trips one. Nothing deleted, nothing
# renamed, so this is a plain +3 with no netting to see through. Verified as the entries above say
# to: each new file holds exactly ONE notice line, and `git diff -G'formerly Orinda Software Ltd'
# --name-only` lists nothing at all, so no surviving file's header moved to make up the number.
# 2026-08-27: 483 -> 485. TWO new files, one notice each -- Scripts/loadtest/run-loadtest.sh (the
# cold-start load-test driver) and Scripts/loadtest/DbProbe.java (its one-shot credential probe).
# THE SECOND ONE IS WHY THIS ENTRY IS WORTH READING. The release stopped here at 484, not 485:
# DbProbe.java had been written with NO notice at all, so the count was short by one and the
# shortfall looked like an ordinary +1 for the shell script. A TOTAL cannot see a missing header --
# it only sees a sum that moved -- so the delta has to be checked FILE BY FILE, which is what the
# 2026-08-21 entry means by verifying a set difference rather than a direction. Doing that found
# the omission; re-pinning to 484 would have locked it in and called it correct.
# 2026-08-27 (later): 485 -> 487. TWO new files in the WEB module, one notice each -- a controller
# advice and its test, for the version shown on the login screen and in the banner. A plain +2 with
# nothing netting out, and unlike the entry above the arithmetic was worked out BEFORE the gate ran
# and matched what it reported. That is the check: predicting the number and then verifying it file
# by file are different acts, and only the second one catches a file that arrived with no notice at
# all.
# 2026-08-27 (third move today): 487 -> 488. ONE new file, one notice -- TSparseIndexBy, the live
# harness for a SPARSE index-by OUT collection. A plain +1, expected before the gate ran and
# verified file by file after, which is the rule the DbProbe.java entry above exists to enforce.
# 2026-08-27 (fourth move today): 488 -> 489. ONE new file in the WEB module, one notice -- a test
# pinning the bind address a child MCP server is given for its Prometheus scrape port. A plain +1,
# predicted before the gate ran and then verified as a set difference:
# `git diff -G'formerly Orinda Software Ltd' --name-only` since the last passing commit lists that
# one file and nothing else, so no surviving header moved to make up the number.
# 2026-08-27 (fifth move today): 489 -> 493. FOUR new files in the WEB module, one notice each --
# one start-up component and three tests, across config upload, boot-time config adoption and the
# security chain. A plain +4, predicted before the gate ran and verified as a set difference after:
# `git diff -G'formerly Orinda Software Ltd' --name-only` since the last passing commit lists those
# four and nothing else, so no surviving header moved to make up the number.
# 2026-08-28: 493 -> 491. TWO of those four DELETED -- the boot-time config adoption arc was
# withdrawn. A FALL is normally the direction this number exists to catch, so it is worth saying
# why this one is not: the notice lines went with whole files, not out of surviving ones. Verified
# that way rather than by the arithmetic -- `git diff -G'formerly Orinda Software Ltd' --name-only`
# lists exactly the two deletions and nothing else, so no file that remains lost its header.
#
# ---------------------------------------------------------------------------------------
# WHEN YOU ADD AN ENTRY: NAME APP-MODULE FILES, NEVER WEB-MODULE ONES.
#
# This script SHIPS -- app/Scripts/ is the public half, and every line above is readable in
# mcpdbwizard-open. app/ is Apache-2.0 and its filenames are already published, so naming
# them costs nothing and makes an entry checkable. web/ is proprietary and does not export,
# so a class named here is the only place its name appears publicly: the running record of
# a closed module's internals, published by the one file whose job is bookkeeping.
#
# Say "N new files in the WEB module" and what kind they are -- a component, a test, which
# area. That is enough to verify the arithmetic later, which is all these entries are for.
# Same reasoning as Scripts/export/exclude.txt deliberately not enumerating what it drops:
# an inventory of what you are withholding publishes the index you meant to withhold.
#
# Three entries above were rewritten on David's instruction (2026-08-27) after the first two
# had already been pushed public. The names are innocuous, which is exactly why this needed
# a rule rather than a judgement call each time -- nobody stops for an innocuous one.
# ---------------------------------------------------------------------------------------
if [ "$PARTIAL" = yes ]; then
    skip_on_partial "copyright chain-of-title lines" "a whole-repository total; this tree is the published subset"
else
    # 2026-08-28 (later): 491 -> 493. TWO new files, one notice each -- a database-free test for the
# record-field crossing notes in the app module, and the WEB module's per-run config-directory
# helper. A plain +2, verified as a set difference after the gate reported it.
#
# WORTH READING FOR THE MISS RATHER THAN THE NUMBER. The web file arrived one commit earlier and
# the count was NOT moved with it, so this assertion had already been failing for a commit before
# anything ran it -- the same lapse the 2026-08-11 entry records, for the same reason: a full suite
# was run and this was not. It is not part of `mvn test`, and a green estate says nothing about it.
# The release caught it in its BUILD phase, which is exactly why that phase exists, but the cost
# was a stopped release rather than a one-line edit at the time.
# 2026-08-31: 493 -> 495, and NOT because of the release that found it. TWO new tests in the WEB
# module -- the audit-trail size budget and server log rotation -- landed before 2.0.14 shipped and
# the count was not moved with them, so this assertion had been failing since 2026-08-28 and 2.0.14
# went out over it. Measured rather than reasoned: the count at 532a63f (Release 2.0.14) is 495,
# identical to the count now, and `git diff -G'formerly Orinda Software Ltd' --name-status` across
# that span lists exactly those two additions.
#
# THE RELEASE THAT TRIPPED IT ADDED NOTHING TO THE NUMBER, which is worth saying because the
# obvious reading of a +2 at release time is "the release did it". 2.0.15 adds three notice-bearing
# files in the WEB module (two components for the tool-description editor and one test) and deletes
# three (the per-object description page's row model and two of its tests). Net zero, verified as a
# set difference over 532a63f..HEAD rather than by the arithmetic.
#
# So this is the SECOND consecutive time the entry above's warning has come true -- the gate is not
# in `mvn test`, a green six-box estate says nothing about it, and it is therefore only ever run by
# a release, which is the most expensive place to learn. Worth wiring into the ordinary suite.
# 2026-08-31 (later the same day): 495 -> 496. ONE new file in the APP module, one notice --
# BrandingInvariantsTest, which runs THIS SCRIPT as part of `mvn test` so that a broken invariant
# stops costing a release to discover. A plain +1, predicted before the gate ran and then confirmed
# by it: the first run of the new test reported 496 against 495 and named nothing else.
#
# That is the check working on its first day, and it is worth leaving on the record: a guard whose
# only trigger was a release had gone wrong twice unnoticed, and the very act of wiring it into the
# suite moved the number it exists to protect.
# 2026-09-01: 496 -> 501. FIVE files, one notice each, and NONE of them from the work that found
# it -- the deploy/ targets of PRs #2 and #3: aws/ecs-ec2.yaml, backup-volume.sh,
# macos/install.sh, macos/mcpdbwizard-start.sh, ubuntu/install.sh. The count was not moved with
# them, so this assertion had been failing since #2 and every commit after it was red.
#
# THE ENTRY ABOVE PREDICTED THIS AND NAMED THE WRONG REMEDY, which is the part worth keeping. It
# closes by saying a guard only a release runs is the most expensive place to learn, and that
# wiring it into `mvn test` fixes that. The wiring worked -- this was caught by an ordinary
# `mvn -pl app test`, not by a stopped release. It still went unnoticed for two commits, because
# those commits were deploy/ documentation and nobody ran the APP suite over them.
#
# So the lesson has moved on. It is not "wire it into the suite" any more, that is done. It is that
# a REPOSITORY-WIDE total is moved by files nowhere near the module whose suite asserts it, and the
# app suite is the only place this runs. Worth running it from the web module too, or from a hook.
# 2026-09-01 (later the same day): 501 -> 504. THREE new files, one notice each, all from the
# open-cursor work: the check itself in pub, its test, and Scripts/loadtest/mcp-tool-scaling.sh.
# A plain +3, predicted before the gate ran and confirmed by it -- and confirmed the other way too,
# by stashing exactly those three and watching the count fall back to 501.
#
# THAT STASH IS THE ENTRY WORTH KEEPING. `git stash show --name-only` printed NOTHING for it, which
# is this repository's known trap and reads exactly like a control that silently took nothing. It
# had taken them: the run that followed reported 501 rather than 504, which is the proof. Check a
# control by its EFFECT on the measurement, never by the stash listing.
# 2026-09-01 (third move the same day): 504 -> 505. ONE new file, one notice --
# McpServerInstructionsTest, for the duality-view clause that discarded the author's instructions.
# Moved WITH the commit that adds the file this time rather than after a red suite, which is what
# the two entries above were both asking for.
# 2026-09-01 (fourth move the same day, and the reason there were four): 505 -> 507. TWO new files,
# one notice each -- McpDateModule in pub and its test -- for the record-date parsing fix. Four
# moves in a day is not churn, it is what a repository-wide total does when the work is spread
# across modules; the entry above asks for a hook, and this is the fourth piece of evidence for it.
# 2026-09-02: 507 -> 508. ONE new file, one notice -- TIndexByRecordKeys, the harness for the
# index-by subscript defect (ORA-06532). Moved with the commit that adds the file, as above.
# 2026-09-02 (second move): 508 -> 509. ONE new file -- ExtraTypeSubscriptTest, the database-free
# guard for the same defect, which the harness above cannot be (it skips by Assumption without a
# live box, and a skip reads like a pass).
# 2026-09-05: 509 -> 510. ONE new file, one notice -- Ec2Metadata, the IMDSv2 probe behind the
# instance-id initial password. Added because the AWS Marketplace listing audit rejected the product
# for "static/default passwords"; the probe is what scopes the new behaviour to EC2 so that
# standalone and other-cloud deployments are untouched.
# 2026-09-05 (second move): 510 -> 511. ONE new file -- Scripts/aws-demo/aws-demo.sh, which
# stands the whole product up in AWS for an hour and then deletes it. A shell script rather than
# a Java source, which is the only thing new here: the notice is a repository-wide count, not a
# Java-file one, so an ops script carrying the header moves it exactly like a class does.
# 2026-09-05 (third move): 511 -> 512. ONE new file -- EcsMetadata, the ECS/Fargate half of the
# AWS probe. Added after the listing audit rejected the product a SECOND time with the same
# static-password finding: keying on the EC2 instance id alone left Fargate tasks and
# IMDS-blocked EKS pods -- both AWS -- falling through to the hard-coded default.
expect "copyright chain-of-title lines" 512 "$(count '(formerly Orinda Software Ltd, Dublin, Ireland)')"
fi
if [ "$PARTIAL" = yes ]; then
    skip_on_partial "Portions Copyright (c) 1999 lines" "a whole-repository total; this tree is the published subset"
else
    expect "Portions Copyright (c) 1999 lines" 6 "$(count 'Portions Copyright (c) 1999')"
fi
if [ "$PARTIAL" = yes ]; then
    skip_on_partial "SpookyAction.com attributions" "a whole-repository total; this tree is the published subset"
else
    expect "SpookyAction.com attributions" 2 "$(count 'SpookyAction')"
fi

echo
echo "2. Branding placeholders -- phase 1"
# Assert the CONSTANTS are filled in, not that the string SUBST_ is absent from the tree.
# Three comments legitimately quote the old placeholder while explaining why something is
# no longer a literal, and a blanket grep calls those defects. What actually matters is
# that no constant still HOLDS one -- and the stronger check, that none reaches generated
# output, belongs to the regeneration diff rather than to grep.
expect "Namer constants still holding a placeholder" 0 \
    "$(grep -cE '= *"SUBST_' "${APP}src/main/java/com/mcpdbwizard/pub/Namer.java")"

# PARAM_ tokens are NOT all defects. A token the generator writes into a CUSTOMER's file
# must stay a token (PARAM_AUTHOR, PARAM_TARGET_PARAM_*, PARAM_JDBCJAR, ...); one merely
# left unsubstituted in OUR OWN source is an unfinished header. Only the second kind is
# asserted here. Namer.java is excluded: it names the token family in its javadoc on
# purpose, which is the documentation of this very distinction.
expect "branding PARAM_ tokens left in our own sources" 0 \
    "$(git grep -cE '\bPARAM_(PROD_NAME|PRODUCT_NAME|PRODUCT_NAME_LONG|PRODUCT_VERSION|VERSION|PRODUCT_WWW|PRODUCT_URL|SOFTCO|SW_CO_NAME|COPYRIGHT|COPYRIGHT_NOTICE|COPYRIGHT_NOTICE_LONG|STATSINTERFACE_URL|LOGINTERFACE_URL|RORSET_URL|ORACLERESOURCEUSER_URL|LIMIT_URL)\b' \
        -- "${APP}src/main/java/com/mcpdbwizard/pub" "${APP}src/main/java/com/mcpdbwizard/app" \
           ":!${APP}src/main/java/com/mcpdbwizard/pub/Namer.java" 2>/dev/null \
        | awk -F: '{s+=$NF} END {print s+0}')"

# No longer needs the com/mcpdbwizard/prod exclusion this assertion used to carry: that
# tree of 2007 generator output was deleted in phase 6 rather than hand-edited, because
# nothing in the repository could regenerate it and rewriting it by hand would have shown
# output no version of the generator ever emitted.
expect "orindasoft.com in main sources" 0 \
    "$(git grep -cF 'orindasoft.com' -- "${APP}src/main/java" 'web/src/main/java' \
        2>/dev/null | awk -F: '{s+=$NF} END {print s+0}')"

echo
echo "3. Package and product names"

# Counts BOTH spellings: the dotted form in Java and the slashed form in poms, scripts,
# Dockerfiles and docs. Missing the slashed one leaves a build that compiles and a
# Dockerfile that copies from a directory that no longer exists.
#
# This file is excluded because it names the old package in its own header and in this
# very comment; docs/rename-plan.md likewise. See the note at the top.
#
# TWO MORE EXCLUSIONS, added 2026-08-17, and the reason matters because it is NOT "the
# assertion was inconvenient". In both, the old package name is the SUBJECT rather than a
# missed rename, and removing it would break the thing it documents or tests:
#
#   RunOnStartTest        writeFingerprint(ws, "com.orindasoft") IS the test input. It
#                         asserts the runtime refuses to launch a workspace built against
#                         the pre-rename package -- the real defect, where both sides read
#                         2.0.0-SNAPSHOT and the server died seconds after a launch that
#                         looked fine. Rename the literal and the test asserts nothing.
#   RuntimeManager        the javadoc on that check, quoting the NoClassDefFoundError it
#                         exists to prevent.
#
# So this stays an exact 0 over everything else. If you find yourself adding a third
# exclusion, check first that the hit is genuinely about the rename rather than a file that
# was missed -- widening the exclusion list is how an assertion like this stops meaning
# anything. It sat FAILING at 6 for six days because these two were never reconciled.
expect "com.orindasoft / com-orindasoft references" 0 \
    "$(git grep -c -e 'com\.orindasoft' -e 'com/orindasoft' -- . \
        ":!$SELF" ':!docs/rename-plan.md' \
        ':!web/src/test/java/com/mcpdbwizard/web/runtime/RunOnStartTest.java' \
        ':!web/src/main/java/com/mcpdbwizard/web/runtime/RuntimeManager.java' 2>/dev/null \
        | awk -F: '{s+=$NF} END {print s+0}')"
# Phase 4 renamed the CLASSES. Bare "OrindaBuild" is a different thing and is still
# expected: 61 "@since OrindaBuild <version>" lines recording when an API appeared, plus
# checked-in 2003-2007 generator output and docs prose. Those are phases 6 and 7.
expect "Orinda* class identifiers" 0 \
    "$(git grep -cE 'Orinda(BuildEvent|Connector|TestModule|TestAction)|JdbcWizardWebApplication' \
        -- . ':!docs/rename-plan.md' ":!$SELF" 2>/dev/null \
        | awk -F: '{s+=$NF} END {print s+0}')"
# Four DELIBERATE historical statements survive, and they should. Each says what the
# product used to be called, in a place where that is the point:
#
#   app/NOTICE                 provenance -- the rename changed the name, not the copyright
#   app/sql/Demo/README.txt x2 why that tree was regenerated rather than edited
#   pub/LogBackends.java       why the legacy env var is OB_ (it stood for OrindaBuild)
#   LogBackendsTest.java       the same, in the test that pins the alias
#
# The number is asserted rather than left pending so a FIFTH cannot creep in unnoticed --
# and so removing one of these is a decision rather than an accident.
# 2026-08-21: 4 -> 7. THREE MORE, ALL IN THE SAME NEW PLACE and all deliberate: the FAQ page's
# "backstory" answer, which tells the reader this began as OrindaBuild in 2003. That is a page
# whose entire purpose is to say what the product used to be called, so it is the fifth, sixth
# and seventh of exactly the kind this assertion permits rather than a rename that was missed.
#
# Note app/NOTICE is NOT among them despite the list above: it names "Orinda Software Ltd", the
# company, not "OrindaBuild", the product. The four were README.txt x2, LogBackends.java and
# LogBackendsTest.java.
# 2026-08-25: 7 -> 8. ONE more, and the same kind again: the "LLMs for fun and profit" post
# under public_website/.../writing/, which tells the reader "this turned into a product called
# OrindaBuild". Prose about what the product used to be called is precisely what this assertion
# permits -- the FAQ's three are the same sentence in a different page. Verified by listing the
# carrying files before and after rather than by the total: the set differs by that one file
# only, and none of the other four moved.
if [ "$PARTIAL" = yes ]; then
    skip_on_partial "deliberate OrindaBuild history notes" "a whole-repository total; this tree is the published subset"
else
    expect "deliberate OrindaBuild history notes" 8 "$(count 'OrindaBuild')"
fi

# Phase 3: artifact and jar names. NOT the same as the property keys (jdbcwizard.*,
# JDBCWIZARD_*, mcpdbwizard_mcp_*), which are phase 5 and keep a compatibility alias --
# hence the narrow pattern rather than a bare "jdbcwizard".
#
# docker-compose.yml and DEPLOYMENT.md are excluded because both legitimately name the
# OLD volume: a volume rename migrates nothing, so the upgrade instructions have to tell
# an operator to copy jdbcwizard-demo across. Compose also derives its project name from
# the checkout DIRECTORY, still jdbcwizard-app, which a comment there explains.
expect "stale jdbcwizard-* artifact names" 0 \
    "$(git grep -cE 'jdbcwizard-(app|web|parent|open)' -- . \
        ':!docs/rename-plan.md' ":!$SELF" \
        ':!docker-compose.yml' ':!DEPLOYMENT.md' 2>/dev/null \
        | awk -F: '{s+=$NF} END {print s+0}')"
expect "Maven artifactIds renamed" 0 "$(count '<artifactId>jdbcwizard-')"

# Phase 5. The property/env families keep a deliberate ALIAS, so a bare count of the old
# spelling would never reach zero and would say nothing. What is asserted instead is that
# nothing still READS the old name as its primary: application.properties defines only
# mcpdbwizard.* keys, and no @Value resolves a jdbcwizard.* one.
if [ "$PARTIAL" = yes ]; then
    skip_on_partial "primary property keys renamed" "its subject is the web module, which does not ship"
else
    expect "primary property keys renamed" 0 \
        "$(grep -cE '^jdbcwizard\.' web/src/main/resources/application.properties \
            web/src/main/resources/application-docker.properties 2>/dev/null \
            | awk -F: '{s+=$NF} END {print s+0}')"
fi
if [ "$PARTIAL" = yes ]; then
    skip_on_partial "@Value sites reading a legacy key" "its subject is the web module, which does not ship"
else
    expect "@Value sites reading a legacy key" 0 \
        "$(git grep -cE '\$\{jdbcwizard\.' -- 'web/src/main/java' 2>/dev/null \
            | awk -F: '{s+=$NF} END {print s+0}')"
fi

# Metrics were a CLEAN break -- no alias, so no code may still emit the old series name.
# Scoped to SOURCE, not docs: DEPLOYMENT.md's upgrade section has to name the old series
# to tell an operator which dashboards to edit, and that mention is the fix, not the bug.
expect "legacy jdbcwizard_ metric names in source" 0 \
    "$(git grep -c 'jdbcwizard_' -- "${APP}src" 'web/src' "${APP}Propfiles" 2>/dev/null \
        | awk -F: '{s+=$NF} END {print s+0}')"

# The alias must stay REACHABLE: these are the fallbacks, and deleting one silently
# strands every deployment still using the old spelling. Counts the mapping entries
# themselves rather than lines mentioning the old prefix, so the class's own javadoc
# can explain itself without moving the number.
# 14, not 15, since 2026-08-17: jdbcwizard.runtime.access-code was dropped because there is no
# new name to point an operator at -- the generator's access-code argument was removed, so the
# setting does not exist under either spelling. This number goes DOWN only when a setting is
# deleted outright; a rename keeps its entry.
if [ "$PARTIAL" = yes ]; then
    skip_on_partial "legacy property fallbacks still wired" "its subject is the web module, which does not ship"
else
    expect "legacy property fallbacks still wired" 14 \
        "$(grep -c 'RENAMED.put(' web/src/main/java/com/mcpdbwizard/web/config/LegacyConfigKeys.java)"
fi

# POSITIVE assertion, and the important one in this phase. RuntimeManager locates the
# generator by JAR FILENAME PREFIX, twice. Rename the artifact without these and the
# build stays green while the web Runtime page dies on ClassNotFoundException for
# ProcBuilder -- the exact defect d80ad20 was written to fix. No compiler sees it.
if [ "$PARTIAL" = yes ]; then
    skip_on_partial "RuntimeManager jar-prefix matches" "its subject is the web module, which does not ship"
else
    expect "RuntimeManager jar-prefix matches" 2 \
        "$(grep -c 'startsWith("mcpdbwizard-app")' \
            web/src/main/java/com/mcpdbwizard/web/runtime/RuntimeManager.java)"
fi

echo
echo "4. Casing discipline (see docs/rename-plan.md 3.2)"
echo "     MCPDBWizard = prose and emitted text"
echo "     McpDbWizard = Java identifiers,  com.mcpdbwizard = packages"
# The two forms are not interchangeable and mixing them is this phase's likeliest defect.
# MCPDBWizard belongs in prose, javadoc and emitted strings; McpDbWizard is the identifier
# form. This catches the wrong one being used as a type: `new MCPDBWizardEvent(...)`,
# `extends MCPDBWizard...`, an import of one, or a static call on one.
expect "MCPDBWizard used as a Java type" 0 \
    "$(git grep -cE '(new |extends |implements |import [a-z.]*)MCPDBWizard' -- '*.java' 2>/dev/null \
        | awk -F: '{s+=$NF} END {print s+0}')"

echo
if [ "$FAILED" -eq 0 ]; then
    echo "All active assertions hold."
    exit 0
fi
echo "$FAILED assertion(s) FAILED."
exit 1
