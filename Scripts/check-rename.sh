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

REPO=$(cd "$(dirname "$0")/../.." && pwd)
cd "$REPO" || exit 2

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
        | grep -v -e '^app/Scripts/check-rename\.sh$' -e '^docs/rename-plan\.md$' \
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
# phase 5 added LegacyConfigKeys and its test, each carrying the standard header. Phases 2,
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
# (McpDescriptionKeysTest, WorkingConfigDescriptionsTest, DescriptionController,
# DescriptionControllerTest, DescriptionPageRenderTest, McpParamTypeLabelTest) and the load harness
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
# more than the number. THREE are the "description will never be used" work (McpUnexposedReport,
# McpUnexposedReportTest, UnexposedKeyMatchesEditorKeyTest). The FOURTH is
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
#   audit + spool (10)  SpoolCipher(+Test), AuditSettings(+Test), AuditStats(+Test),
#                       AuditPageRendersTest, McpAccessAuditorFailureTest,
#                       AuthenticationAuditListener(+Test)
#   inline SQL (6)      SqlInliner(+Test), InlineSqlWriteTest, InlineSqlTextTest,
#                       InlineSqlLibraryTest, SqlStatementKeysTest
#   container metrics (3), extra-TYPE check (3), run-on-start (2), protocol log (2)
#   2026-08-17 GUI/release work (7)  OracleRequiredInterceptor,
#                       DesignUnavailableController, NewTableIsReadOnlyByDefaultTest,
#                       PoolingDefaultsTest, OracleUnavailableScreenTest,
#                       check-links.mjs, release.sh
#   remaining (6)       TRecordFieldCensus, AspDatatypeHintTest, RetiredWsRecordTypeTest,
#                       ConfigStoreWriteFailureTest, McpProxyEndToEndTest,
#                       tz-collection-binding-plan.md
#
# THIS SAT FAILING FOR SIX DAYS AND ~39 FILES, which is worse than the two-commit lapse noted
# above and makes the same point louder: the number is only as good as the habit of running the
# script. It is now a phase of ./release.sh at the repository root, so a release run reports it
# rather than relying on someone remembering.
expect "copyright chain-of-title lines" 405 "$(count '(formerly Orinda Software Ltd, Dublin, Ireland)')"
expect "Portions Copyright (c) 1999 lines" 6 "$(count 'Portions Copyright (c) 1999')"
expect "SpookyAction.com attributions" 2 "$(count 'SpookyAction')"

echo
echo "2. Branding placeholders -- phase 1"
# Assert the CONSTANTS are filled in, not that the string SUBST_ is absent from the tree.
# Three comments legitimately quote the old placeholder while explaining why something is
# no longer a literal, and a blanket grep calls those defects. What actually matters is
# that no constant still HOLDS one -- and the stronger check, that none reaches generated
# output, belongs to the regeneration diff rather than to grep.
expect "Namer constants still holding a placeholder" 0 \
    "$(grep -cE '= *"SUBST_' app/src/main/java/com/mcpdbwizard/pub/Namer.java)"

# PARAM_ tokens are NOT all defects. A token the generator writes into a CUSTOMER's file
# must stay a token (PARAM_AUTHOR, PARAM_TARGET_PARAM_*, PARAM_JDBCJAR, ...); one merely
# left unsubstituted in OUR OWN source is an unfinished header. Only the second kind is
# asserted here. Namer.java is excluded: it names the token family in its javadoc on
# purpose, which is the documentation of this very distinction.
expect "branding PARAM_ tokens left in our own sources" 0 \
    "$(git grep -cE '\bPARAM_(PROD_NAME|PRODUCT_NAME|PRODUCT_NAME_LONG|PRODUCT_VERSION|VERSION|PRODUCT_WWW|PRODUCT_URL|SOFTCO|SW_CO_NAME|COPYRIGHT|COPYRIGHT_NOTICE|COPYRIGHT_NOTICE_LONG|STATSINTERFACE_URL|LOGINTERFACE_URL|RORSET_URL|ORACLERESOURCEUSER_URL|LIMIT_URL)\b' \
        -- 'app/src/main/java/com/mcpdbwizard/pub' 'app/src/main/java/com/mcpdbwizard/app' \
           ':!app/src/main/java/com/mcpdbwizard/pub/Namer.java' 2>/dev/null \
        | awk -F: '{s+=$NF} END {print s+0}')"

# No longer needs the com/mcpdbwizard/prod exclusion this assertion used to carry: that
# tree of 2007 generator output was deleted in phase 6 rather than hand-edited, because
# nothing in the repository could regenerate it and rewriting it by hand would have shown
# output no version of the generator ever emitted.
expect "orindasoft.com in main sources" 0 \
    "$(git grep -cF 'orindasoft.com' -- 'app/src/main/java' 'web/src/main/java' \
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
        ':!app/Scripts/check-rename.sh' ':!docs/rename-plan.md' \
        ':!web/src/test/java/com/mcpdbwizard/web/runtime/RunOnStartTest.java' \
        ':!web/src/main/java/com/mcpdbwizard/web/runtime/RuntimeManager.java' 2>/dev/null \
        | awk -F: '{s+=$NF} END {print s+0}')"
# Phase 4 renamed the CLASSES. Bare "OrindaBuild" is a different thing and is still
# expected: 61 "@since OrindaBuild <version>" lines recording when an API appeared, plus
# checked-in 2003-2007 generator output and docs prose. Those are phases 6 and 7.
expect "Orinda* class identifiers" 0 \
    "$(git grep -cE 'Orinda(BuildEvent|Connector|TestModule|TestAction)|JdbcWizardWebApplication' \
        -- . ':!docs/rename-plan.md' ':!app/Scripts/check-rename.sh' 2>/dev/null \
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
expect "deliberate OrindaBuild history notes" 4 "$(count 'OrindaBuild')"

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
        ':!docs/rename-plan.md' ':!app/Scripts/check-rename.sh' \
        ':!docker-compose.yml' ':!DEPLOYMENT.md' 2>/dev/null \
        | awk -F: '{s+=$NF} END {print s+0}')"
expect "Maven artifactIds renamed" 0 "$(count '<artifactId>jdbcwizard-')"

# Phase 5. The property/env families keep a deliberate ALIAS, so a bare count of the old
# spelling would never reach zero and would say nothing. What is asserted instead is that
# nothing still READS the old name as its primary: application.properties defines only
# mcpdbwizard.* keys, and no @Value resolves a jdbcwizard.* one.
expect "primary property keys renamed" 0 \
    "$(grep -cE '^jdbcwizard\.' web/src/main/resources/application.properties \
        web/src/main/resources/application-docker.properties 2>/dev/null \
        | awk -F: '{s+=$NF} END {print s+0}')"
expect "@Value sites reading a legacy key" 0 \
    "$(git grep -cE '\$\{jdbcwizard\.' -- 'web/src/main/java' 2>/dev/null \
        | awk -F: '{s+=$NF} END {print s+0}')"

# Metrics were a CLEAN break -- no alias, so no code may still emit the old series name.
# Scoped to SOURCE, not docs: DEPLOYMENT.md's upgrade section has to name the old series
# to tell an operator which dashboards to edit, and that mention is the fix, not the bug.
expect "legacy jdbcwizard_ metric names in source" 0 \
    "$(git grep -c 'jdbcwizard_' -- 'app/src' 'web/src' 'app/Propfiles' 2>/dev/null \
        | awk -F: '{s+=$NF} END {print s+0}')"

# The alias must stay REACHABLE: these are the fallbacks, and deleting one silently
# strands every deployment still using the old spelling. Counts the mapping entries
# themselves rather than lines mentioning the old prefix, so the class's own javadoc
# can explain itself without moving the number.
# 14, not 15, since 2026-08-17: jdbcwizard.runtime.access-code was dropped because there is no
# new name to point an operator at -- the generator's access-code argument was removed, so the
# setting does not exist under either spelling. This number goes DOWN only when a setting is
# deleted outright; a rename keeps its entry.
expect "legacy property fallbacks still wired" 14 \
    "$(grep -c 'RENAMED.put(' web/src/main/java/com/mcpdbwizard/web/config/LegacyConfigKeys.java)"

# POSITIVE assertion, and the important one in this phase. RuntimeManager locates the
# generator by JAR FILENAME PREFIX, twice. Rename the artifact without these and the
# build stays green while the web Runtime page dies on ClassNotFoundException for
# ProcBuilder -- the exact defect d80ad20 was written to fix. No compiler sees it.
expect "RuntimeManager jar-prefix matches" 2 \
    "$(grep -c 'startsWith("mcpdbwizard-app")' \
        web/src/main/java/com/mcpdbwizard/web/runtime/RuntimeManager.java)"

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
