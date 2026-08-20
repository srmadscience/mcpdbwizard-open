#!/bin/sh
#
# testrun_current.sh  --  regenerate + compile the MCPDBWizard test code for the
#                         CURRENT Oracle version only.
#
# Derived from the retired Scripts/testrun2.sh (which ran the generator across a matrix
# of every Oracle version x option combination on a long-gone Solaris host) and
# Scripts/testrun2a1.sh (its per-package javac step). BOTH WERE DELETED 2026-08-17 --
# `git show 385dff7:app/Scripts/testrun2.sh` if the archaeology is ever wanted. They had
# been unrunnable for years: every input they needed was gone (the *.sed files,
# versions.txt, getLatestJava.sh, ~/MCPDBWizard, Solaris JDK paths) and they invoked a
# lowercase `procBuilder` that has never existed. Nothing but this comment referenced
# them, yet three tree-wide renames had faithfully rewritten them in place.
#
# Unlike those, this script is self-contained: it needs none of that, drives the locally
# built shaded jar against the live test database, and writes everything under target/
# (which is gitignored).
#
# For each Propfiles/<name>.pb2 it:
#   1. rewrites the connection + Oracle-version fields to point at the test DB
#      / Oracle 12.1.0, and bumps the package's version label (...ovNNN_ov ->
#      ...ov1210_ov) so the output matches what the migrated 't' harnesses import;
#   2. runs com.mcpdbwizard.app.procbuilder.gui.ProcBuilder in batch mode to
#      generate Java into target/regen/Src;
#   3. compiles that generated Java into target/regen/Built.
#
# The generated tree (e.g.
#   generic_test1/yes_comments/yes_stats/yes_debug/yes_om/icl_jnc/ov1210_ov/
#       {sequence,plsql,table,...})
# is the "generated code" the legacy 't' java files that reference
# 'generic_test?.yes_comments...' link against, i.e. the input to the next
# round of test migration.
#
# Usage:
#   Scripts/testrun_current.sh                       # every Propfiles/generic_test*.pb2
#   Scripts/testrun_current.sh generic_test1 ...     # only the named propfiles
#
# WARNING: this script WIPES the whole regen tree on every run (the rm -rf below), so naming
# individual propfiles leaves ONLY those propfiles built. The T* harnesses import several trees,
# so a subsequent `mvn test` then fails to compile with "package generic_testN... does not exist".
# Name propfiles only when you also intend to run a matching subset of tests; otherwise run with
# no arguments.
#
# NOTE: the default (no-arg) glob now also picks up Propfiles/generic_test_23ai.pb2,
# which targets the Oracle 23ai box (table FIXTURE_TABLE + package FIXTURE_PKG; provision
# with sql/datatypes_23ai_gen.sql) and feeds the TGen23ai harness. On a non-23ai host
# its objects are absent, so generation yields nothing and TGen23ai won't compile under
# the generated-tests profile -- name only the propfiles you can actually generate, or
# point the run at the 23ai server.
#
# Connection defaults match com.mcpdbwizard.pub.DbTestSupport; override via env:
#   ORINDA_TEST_HOST  ORINDA_TEST_PORT  ORINDA_TEST_SID
#   ORINDA_TEST_USER  ORINDA_TEST_PASSWORD
#
# Compile classpath:
#   The shaded jar bundles the com.mcpdbwizard.pub runtime AND the Oracle JDBC
#   driver -- everything the core generated code links against. The generated
#   web-service / XMLType / Spatial code additionally references javax.jws,
#   oracle.xdb and oracle.spatial.geometry, which the legacy build pulled from
#   j2ee.jar / xdb.jar / sdoapi.jar. Those jars are not in this repo, so by
#   default we compile against tiny COMPILE-ONLY shims under Scripts/compile-shims.
#   To compile against the real Oracle libraries instead, put them on OB_EXTRA_CP
#   (it is prepended to the classpath, so it wins over the shims):
#       OB_EXTRA_CP=/path/xdb.jar:/path/sdoapi.jar:/path/jakarta.jws-api.jar \
#           Scripts/testrun_current.sh
#
set -u

# --- locate repo root (this script lives in <repo>/Scripts) -----------------
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
HOMEDIR=$(cd "$SCRIPT_DIR/.." && pwd)
cd "$HOMEDIR" || exit 2

# --- the "current Oracle version" -------------------------------------------
OB_VERSION=1210            # package label: ...ov${OB_VERSION}_ov
ORACLE_VERSION=12.1.0      # value written into each .pb2's ORACLE_VERSION field

# --- live test database (same defaults as DbTestSupport) --------------------
# Local box inventory (gitignored; see boxes.env.template). Sourced for its DEFAULTS
# only -- an environment variable set by the caller always wins, because every
# assignment in that file is of the ${VAR:-default} form.
BOXES_ENV="$(cd "$(dirname "$0")" && pwd)/boxes.env"
[ -f "$BOXES_ENV" ] && . "$BOXES_ENV"
HOST=${MCPDBWIZARD_TEST_HOST:-${ORINDA_TEST_HOST:-}}
PORT=${MCPDBWIZARD_TEST_PORT:-${ORINDA_TEST_PORT:-1521}}
SID=${MCPDBWIZARD_TEST_SID:-${ORINDA_TEST_SID:-orcl}}
# No hardcoded schema default: the schema name is private and this script ships. It comes from
# the environment, or from Scripts/boxes.env (gitignored, sourced above) like the host inventory.
DBUSER=${MCPDBWIZARD_TEST_USER:-${ORINDA_TEST_USER:-}}
DBPASS=${MCPDBWIZARD_TEST_PASSWORD:-${ORINDA_TEST_PASSWORD:-}}
if [ -z "$DBUSER" ] || [ -z "$DBPASS" ]; then
	echo "ERROR: no test schema credentials."
	echo "       Set MCPDBWIZARD_TEST_USER and MCPDBWIZARD_TEST_PASSWORD, or put"
	echo "       ORINDA_TEST_USER / ORINDA_TEST_PASSWORD in Scripts/boxes.env"
	echo "       (copy Scripts/boxes.env.template to start)."
	exit 2
fi

# NO ACCESS CODE. ProcBuilder's leading access_code argument was REMOVED in 2026-08: it was
# validated for shape only (>=19 chars, no : . / \ , not "build") and then IGNORED by
# ApplicationShell, so it protected nothing. MCPDBWIZARD_ACCESS_CODE, OB_ACCESS_CODE and
# ORINDA_ACCESS_CODE are now read by nothing and can be dropped from any environment that sets them.
#
# The failure it used to cause is recorded here because it is worth recognising if anyone reverts
# this: with an EMPTY code ProcBuilder printed its usage banner and exited, and this script then
# reported "GENERATION FAILED (rc=0)" for EVERY propfile in about twelve seconds. The message names
# generation, so it read as a total generator collapse rather than a missing variable.

# --- paths ------------------------------------------------------------------
. "$SCRIPT_DIR/find-shaded-jar.sh"   # sets JAR; version-agnostic on purpose
SQLDIR=$HOMEDIR/Sqlfiles
# PER-PROPFILE SQL DIRECTORIES. Every config used to be pointed at the ONE library above, which
# flattened configs that were authored against several directories: a config naming a statement the
# library does not hold silently generated nothing for it (84 such references across 32 of the 41
# propfiles when this was written -- see docs/missing-sql-file-plan.md). sqlDirFor() composes a
# directory per propfile instead: the shared library, plus an optional per-propfile OVERLAY at
# Sqlfiles/<name>/ for statements only that config uses. Composed under target/ rather than
# committed, so a file shared by several configs stays in the library and is not duplicated per
# config -- Sqlfiles/ already holds eight subdirectories that are byte-identical copies of the
# library, which is what that costs.
COMPOSEDSQL=$HOMEDIR/target/regen/work/sqlfiles
SHIMSRC=$SCRIPT_DIR/compile-shims

# Compose (and echo the path of) the SQL directory for one propfile: the shared library plus an
# optional per-propfile OVERLAY at Sqlfiles/<name>/ for statements only that config uses.
#
# THE LIBRARY IS COPIED WHOLE, AND THAT IS DELIBERATE -- do not "improve" this to copy only the
# files the propfile names. A .sql file the config does NOT name is still generated:
# ApplicationShell leaves tempPropNumber at -1 for it, SQL_CREATE_CLASS_-1 does not exist, and that
# key defaults to YES. So the directory means "generate the statements in here" and the config's
# list exists to DESELECT. That is load-bearing, not an accident: generic_test_23ai names ZERO SQL
# files yet TGen23aiMcp drives user-SQL-statement tools from oneparam.sql, which it gets this way
# alone. Copying only named files was tried and collapsed SEVENTEEN propfiles by 40-55 files each.
#
# The corollary is the rule for CONTENT: a file wanted by some configs but not all belongs in an
# OVERLAY, never in the shared library, because everything in the library is emitted into every
# tree. Adding six shared files to it put six unrelated classes into all 35.
#
# The overlay key drops any _mcp suffix, so a variant shares its original's statements -- the two
# differ only in MCP_SERVER/TARGET_JVM/PACKAGE_NAME, never in SQL. Composed under target/ rather
# than committed, so the library is not duplicated per config.
sqlDirFor() {
	_ob_name="${1%_mcp}"
	_ob_dir="$COMPOSEDSQL/$1"
	rm -rf "$_ob_dir"
	mkdir -p "$_ob_dir"
	cp "$SQLDIR"/*.sql "$_ob_dir"/ 2>/dev/null
	if [ -d "$SQLDIR/$_ob_name" ]; then
		cp "$SQLDIR/$_ob_name"/*.sql "$_ob_dir"/ 2>/dev/null
	fi
	echo "$_ob_dir"
}
# com.mcpdbwizard.pub.JavaLog -- the optional java.util.logging backend (historically the
# separate OBJavaLog.jar). Generated code with DAO_LOG_TYPE=Java 1.4 Logging links against it.
JAVALOGSRC=$HOMEDIR/JavaLoggingSrc
OUTDIR=$HOMEDIR/target/regen
SRCDIR=$OUTDIR/Src
BUILTDIR=$OUTDIR/Built
SHIMDIR=$OUTDIR/shim-classes
LOGDIR=$OUTDIR/Log
WORKDIR=$OUTDIR/work

JAVA=${JAVA:-java}
JAVAC=${JAVAC:-javac}
GENERATOR_CLASS=com.mcpdbwizard.app.procbuilder.gui.ProcBuilder
OB_EXTRA_CP=${MCPDBWIZARD_EXTRA_CP:-${OB_EXTRA_CP:-}}

PASS_OR_FAIL=0

# --- preflight --------------------------------------------------------------
if [ ! -f "$JAR" ]; then
	echo "ERROR: shaded jar not found:"
	echo "       $JAR"
	echo "       build it first:  mvn -DskipTests package"
	exit 2
fi
if [ ! -d "$SQLDIR" ]; then
	echo "ERROR: Sqlfiles directory not found: $SQLDIR"
	exit 2
fi

# --- provisioning preflight -------------------------------------------------
# A missing schema fixture does not fail anything downstream: the generator emits fewer classes,
# whatever those classes guarded silently stops being guarded, and the suite still reports green.
# That is exactly how ORCL12 lost APPSCHEMA.MULTIREC (generic_test1: 2487 -> 2482 files, nested-record
# compile guard inactive, nobody noticed). So verify every object the selected propfiles declare
# BEFORE generating. Set OB_SKIP_PROVISIONING_CHECK=1 to bypass (e.g. deliberately generating
# against a partially provisioned box); an unreachable DB is reported as SKIPPED, never a failure.
if [ "${MCPDBWIZARD_SKIP_PROVISIONING_CHECK:-${OB_SKIP_PROVISIONING_CHECK:-0}}" != 1 ]; then
	if ! "$SCRIPT_DIR/check_provisioning.sh" "$@"; then
		echo
		echo "=================================================================="
		echo "  ABORTING: the database is missing objects the propfiles declare."
		echo "  Generating now would silently produce a short tree and a weaker"
		echo "  test suite. Reload the fixtures listed above, or re-run with"
		echo "  OB_SKIP_PROVISIONING_CHECK=1 to proceed anyway."
		echo "=================================================================="
		exit 2
	fi
	echo
fi

rm -rf "$SRCDIR" "$BUILTDIR" "$SHIMDIR" "$LOGDIR" "$WORKDIR"
mkdir -p "$SRCDIR" "$BUILTDIR" "$SHIMDIR" "$LOGDIR" "$WORKDIR"

# --- compile the shims used to satisfy javax.jws / oracle.xdb / oracle.spatial
echo "Building compile shims from $SHIMSRC"
if ! "$JAVAC" -cp "$JAR" -d "$SHIMDIR" $(find "$SHIMSRC" -name '*.java') 2>"$WORKDIR/shim.log"; then
	echo "ERROR: failed to compile shims:"
	cat "$WORKDIR/shim.log"
	exit 2
fi

# --- compile the optional Java 1.4 logging backend (com.mcpdbwizard.pub.JavaLog) -------------
# It links only against the pub runtime in $JAR; output lands in $SHIMDIR, which is already on
# the generate/compile classpath, so generated DAO factories that select Java 1.4 Logging find it.
if [ -d "$JAVALOGSRC" ]; then
	echo "Building Java 1.4 logging backend from $JAVALOGSRC"
	if ! "$JAVAC" -cp "$JAR" -d "$SHIMDIR" $(find "$JAVALOGSRC" -name '*.java') 2>"$WORKDIR/javalog.log"; then
		echo "ERROR: failed to compile JavaLog:"
		cat "$WORKDIR/javalog.log"
		exit 2
	fi
fi

# --- MCP SDK classpath (for propfiles with MCP_SERVER=YES) ------------------
# A generated <Factory>McpServer compiles against the real MCP Java SDK (an
# <optional> dependency of the main pom, so `mvn -DskipTests package` has
# already pulled it into the local repository). Resolve the project's full
# dependency classpath once and cache it.
#
# -P '!generated-tests' IS LOAD-BEARING, and without it this NEVER succeeds on a
# clean run. That profile is file-activated on target/regen/Src, which exists by
# the time we get here, and it declares a SYSTEM-scoped dependency on
# target/regen/generated.jar -- a file this script deletes at the start and only
# writes at the end. Maven resolves every declared dependency before running any
# goal, so it fails on the missing jar and never gets as far as printing a
# classpath. Disabling the profile drops that dependency; nothing else in it
# affects what we want here, which is the main pom's own dependencies.
#
# THIS FILE IS NOT ONLY A COMPILE CLASSPATH, and that is why the bug above cost a
# real failure rather than a warning. AbstractMcpHarness.mcpServerClasspath() reads
# the SAME file at RUN time and puts it FIRST when launching a generated MCP server,
# precisely so the real xdb.jar shadows the oracle.xdb STUB in Scripts/compile-shims.
# With the file missing the stub wins, and TGen23aiMcp fails at the XMLTYPE step with
# "java.io.IOException: Failed to read value" -- a client-side JSON error that says
# nothing about classpaths and reads like a generator regression. (Compilation is the
# forgiving half: $JAR bundles io/modelcontextprotocol/** and oracle/xdb, so the
# MCP_SERVER=YES propfiles compile from the shaded jar whatever happens here. That is
# what made the old warning look cosmetic.)
#
# The failure needed BOTH the bug and a `mvn clean`: this file lives outside
# target/regen, so the ordinary `rm -rf target/regen` leaves it alone and a stale copy
# from an older run kept everything working. `mvn clean` removes it, and then nothing
# could rebuild it. So it survived for as long as nobody cleaned.
#
# The old message claimed MCP_SERVER=YES propfiles "will not compile", which was false
# -- every run warned, every run compiled, and a warning that is always wrong stops
# being read.
MCPCP_FILE=$HOMEDIR/target/regen-mcp-classpath.txt
if [ ! -s "$MCPCP_FILE" ]; then
	echo "Resolving MCP SDK compile classpath (cached in $MCPCP_FILE)"
	(cd "$HOMEDIR" && mvn -q -P '!generated-tests' dependency:build-classpath \
		-Dmdep.outputFile="$MCPCP_FILE" >/dev/null 2>&1) \
		|| echo "WARNING: could not resolve the MCP SDK classpath; falling back to the SDK bundled in the shaded jar"
fi

# Classpath used to run the generator AND to compile its output. OB_EXTRA_CP
# (real Oracle/EE jars, if supplied) is prepended so it overrides the shims.
CP="$JAR:$SHIMDIR:$SRCDIR:$BUILTDIR"
[ -s "$MCPCP_FILE" ] && CP="$CP:$(cat "$MCPCP_FILE")"
[ -n "$OB_EXTRA_CP" ] && CP="$OB_EXTRA_CP:$CP"

# --- which propfiles? -------------------------------------------------------
if [ "$#" -gt 0 ]; then
	PROPFILES=""
	for name in "$@"; do
		PROPFILES="$PROPFILES $HOMEDIR/Propfiles/$(basename "$name" .pb2).pb2"
	done
	echo "NOTE: named propfiles only -- the regen tree was wiped, so it now contains just these."
	echo "      A full 'mvn test' needs every tree; re-run with no arguments for that."
	echo
else
	PROPFILES=$(ls "$HOMEDIR"/Propfiles/generic_test*.pb2)
fi

EXPECTED_COUNTS="$SCRIPT_DIR/provisioning/expected-file-counts.txt"

for PB_SRC in $PROPFILES; do
	PROPFILE=$(basename "$PB_SRC" .pb2)
	echo "=================================================================="
	echo "  $PROPFILE"
	echo "=================================================================="

	if [ ! -f "$PB_SRC" ]; then
		echo "  SKIP: no such propfile: $PB_SRC"
		PASS_OR_FAIL=1
		continue
	fi

	# 1. rewrite connection / version / package-version fields ----------------
	#    NB: USER/PASS are deliberately NOT rewritten. Each propfile introspects its OWN
	#    authored schema (test1/2/3/9=APPSCHEMA, generic_test4=ORINDADEMO, generic_testd=
	#    GENERIC_TESTD, ...), provisioned per OtherDbs/testdata.sh. Flattening every propfile
	#    to one user made the generator look in the wrong schema and emit no PL/SQL for the
	#    objects that live elsewhere. Only host/port/SID (which DB) and the version label move.
	# The generated DAO factories bake a RUNTIME connection (DAO_CONNECTION_TYPE / _NAME). The
	# committed value points at a long-gone host (hard-coded string) or a JNDI datasource
	# ('jdbc/orindabuild'); neither resolves under JUnit. Repoint it at the test DB as a direct
	# hard-coded connect string using THIS propfile's own schema creds, so the generated factory
	# self-connects to the right schema (e.g. anotherDaoFactory()/DaoFactory() no-arg ctors).
	PB_USER=$(grep -m1 '^USER=' "$PB_SRC" | cut -d= -f2 | tr -d '\r')
	PB_PASS=$(grep -m1 '^PASS=' "$PB_SRC" | cut -d= -f2 | tr -d '\r')
	# Baked DAO connect string. A SID starting with '/' (e.g. /FREEPDB1, a PDB
	# service name) takes the service-name URL form @host:port/service; a bare SID
	# takes the classic @host:port:sid form.
	case "$SID" in
		/*) DAO_URL="jdbc:oracle:thin:${PB_USER}/${PB_PASS}@${HOST}:${PORT}${SID}" ;;
		*)  DAO_URL="jdbc:oracle:thin:${PB_USER}/${PB_PASS}@${HOST}:${PORT}:${SID}" ;;
	esac
	PB=$WORKDIR/$PROPFILE.pb2
	sed -E \
		-e "s/^HOSTNAME=.*/HOSTNAME=$HOST/" \
		-e "s/^PORT=.*/PORT=$PORT/" \
		-e "s|^ORACLE_SID=.*|ORACLE_SID=$SID|" \
		-e "s/^ORACLE_VERSION=.*/ORACLE_VERSION=$ORACLE_VERSION/" \
		-e "s/^(PACKAGE_NAME=.*)\.ov[0-9]+_ov/\1.ov${OB_VERSION}_ov/" \
		-e "s|^DAO_CONNECTION_TYPE=.*|DAO_CONNECTION_TYPE=Hard coded connect string|" \
		-e "s|^DAO_CONNECTION_NAME=.*|DAO_CONNECTION_NAME=${DAO_URL}|" \
		"$PB_SRC" > "$PB"

	PKG=$(grep -m1 '^PACKAGE_NAME=' "$PB" | cut -d= -f2 | tr -d '\r')
	PKG_DIR=$(echo "$PKG" | tr '.' '/')
	echo "  package : $PKG"

	# 2. generate -------------------------------------------------------------
	#    The CODE_BASE_DIRECTORY / SQL_FILE_DIRECTORY overrides MUST precede the
	#    config argument: ProcBuilder generates the instant it sees a non-override
	#    argument, so anything after the config file is ignored for that file.
	#    OB_GEN_FROM_JSON=1 drives the whole run off the new .json config format
	#    instead of .pb2: the rewritten $PB is converted to .json via ConfigConverter
	#    and generation runs from that. The generated tree is then compiled and (under
	#    the generated-tests profile) exercised by the T* harnesses exactly as usual,
	#    proving the runtime works off a JSON-sourced tree. (Mutually exclusive with
	#    OB_JSON_PARITY, which already regenerates from .json separately.)
	GEN_INPUT="$PB"
	if [ "${MCPDBWIZARD_GEN_FROM_JSON:-${OB_GEN_FROM_JSON:-0}}" = 1 ]; then
		GEN_INPUT=$WORKDIR/$PROPFILE.json
		"$JAVA" -cp "$JAR" com.mcpdbwizard.schema.ConfigConverter "$PB" "$GEN_INPUT" \
			> "$WORKDIR/$PROPFILE.convert.log" 2>&1
		echo "  input   : $GEN_INPUT (converted from .pb2)"
	fi
	GENLOG=$WORKDIR/$PROPFILE.genlog
	PROPSQLDIR=$(sqlDirFor "$PROPFILE")
	echo "  running : generator (batch) -> $SRCDIR"
	"$JAVA" -Djava.awt.headless=true -cp "$JAR" "$GENERATOR_CLASS" \
		"$LOGDIR" build \
		"CODE_BASE_DIRECTORY=$SRCDIR" "SQL_FILE_DIRECTORY=$PROPSQLDIR" \
		"$GEN_INPUT" > "$GENLOG" 2>&1
	GEN_RC=$?

	# The generator exits 0 and logs "Finished processing file" on success; the
	# many ":Error:" lines it prints for objects it deliberately skips (NCLOB
	# params, object-type columns, procedures over the parameter limit, ...) are
	# expected and NOT failures -- so we key off the exit code + the completion
	# marker + actually-produced files, exactly as testrun2.sh did (rc==0).
	if [ "$GEN_RC" != 0 ] || ! grep -q "Finished processing file" "$GENLOG"; then
		echo "  GENERATION FAILED (rc=$GEN_RC) -- see $GENLOG"
		tail -15 "$GENLOG"
		PASS_OR_FAIL=1
		continue
	fi

	NFILES=$(find "$SRCDIR/$PKG_DIR" -name '*.java' 2>/dev/null | wc -l | tr -d ' ')
	echo "  generated $NFILES java files"
	if [ "$NFILES" = 0 ]; then
		echo "  GENERATION produced no .java files -- see $GENLOG"
		PASS_OR_FAIL=1
		continue
	fi

	# Guard against a SILENT COLLAPSE. Generation can lose its connection part-way
	# (ORA-17008) and still exit 0 with "Finished processing file" in the log, leaving
	# a handful of files instead of hundreds. The suite will not catch it -- no harness
	# imports every propfile -- so compare against the floor recorded per propfile.
	# See Scripts/provisioning/expected-file-counts.txt for why these are minimums.
	MINFILES=$(sed -n "s/^$PROPFILE[[:space:]][[:space:]]*\([0-9][0-9]*\).*/\1/p" \
		"$EXPECTED_COUNTS" 2>/dev/null | head -1)
	if [ -n "$MINFILES" ] && [ "$NFILES" -lt "$MINFILES" ]; then
		echo
		echo "  =============================================================="
		echo "  FILE-COUNT COLLAPSE: $PROPFILE generated $NFILES files,"
		echo "  but at least $MINFILES are expected."
		echo
		echo "  Generation exited 0, so this would otherwise pass silently."
		echo "  Look for ORA- errors in $GENLOG -- a connection dropped mid-run"
		echo "  is the usual cause, and usually succeeds on a re-run."
		echo "  If the reduction is intentional, update the floor in"
		echo "  Scripts/provisioning/expected-file-counts.txt."
		echo "  =============================================================="
		echo
		grep -oE "ORA-[0-9]+" "$GENLOG" 2>/dev/null | sort | uniq -c | sed 's/^/    /'
		PASS_OR_FAIL=1
		continue
	fi

	# 2b. JSON config parity (opt-in: OB_JSON_PARITY=1) -----------------------
	#    Prove the generator produces identical code from the new .json config
	#    format as from the .pb2. Convert the (already-rewritten) $PB to .json via
	#    com.mcpdbwizard.schema.ConfigConverter, regenerate from the .json into the
	#    SAME $SRCDIR (so any path baked into the output is identical), and diff the
	#    two trees with comment blocks stripped -- the only run-to-run differences
	#    (generation timestamp + build stamp) live in Javadoc. The .pb2
	#    output is snapshotted first and restored afterwards so the compile step
	#    below still runs against it. Off by default (adds one extra generation per
	#    propfile). See also the standalone Scripts/json-config-test/run.sh.
	if [ "${MCPDBWIZARD_JSON_PARITY:-${OB_JSON_PARITY:-0}}" = 1 ]; then
		echo "  json-parity: regenerating from .json and diffing"
		PB_JSON=$WORKDIR/$PROPFILE.json
		PSNAP_PB2=$WORKDIR/$PROPFILE.parity_pb2
		PSNAP_JSON=$WORKDIR/$PROPFILE.parity_json
		rm -rf "$PSNAP_PB2" "$PSNAP_JSON"
		cp -R "$SRCDIR/$PKG_DIR" "$PSNAP_PB2"
		"$JAVA" -cp "$JAR" com.mcpdbwizard.schema.ConfigConverter "$PB" "$PB_JSON" \
			> "$WORKDIR/$PROPFILE.convert.log" 2>&1
		rm -rf "$SRCDIR/$PKG_DIR"
		"$JAVA" -Djava.awt.headless=true -cp "$JAR" "$GENERATOR_CLASS" \
			"$LOGDIR" build \
			"CODE_BASE_DIRECTORY=$SRCDIR" "SQL_FILE_DIRECTORY=$PROPSQLDIR" \
			"$PB_JSON" > "$WORKDIR/$PROPFILE.jsongen.log" 2>&1
		cp -R "$SRCDIR/$PKG_DIR" "$PSNAP_JSON"
		# restore the .pb2 output so the compile step uses it
		rm -rf "$SRCDIR/$PKG_DIR"
		cp -R "$PSNAP_PB2" "$SRCDIR/$PKG_DIR"
		# strip all comment blocks (timestamp + tips live only in Javadoc), then diff
		for src in "$PSNAP_PB2" "$PSNAP_JSON"; do
			(cd "$src" && find . -name '*.java' | while read -r jf; do
				mkdir -p "$src.nc/$(dirname "$jf")"
				sed '/\/\*/,/\*\//d' "$jf" > "$src.nc/$jf"
			done)
		done
		if diff -r "$PSNAP_PB2.nc" "$PSNAP_JSON.nc" > "$WORKDIR/$PROPFILE.parity.diff" 2>&1; then
			echo "  json-parity: OK (code byte-identical from .pb2 and .json)"
		else
			echo "  json-parity: FAILED -- .pb2 and .json produced different code"
			head -30 "$WORKDIR/$PROPFILE.parity.diff"
			PASS_OR_FAIL=1
			continue
		fi
	fi

	# 3. compile --------------------------------------------------------------
	#    Modernized equivalent of testrun2a1.sh: one javac over the whole
	#    generated package, output to BUILTDIR, deprecation/Note noise ignored.
	echo "  compiling $PKG_DIR"
	FILELIST=$WORKDIR/$PROPFILE.filelist
	find "$SRCDIR/$PKG_DIR" -name '*.java' > "$FILELIST"
	COMPLOG=$WORKDIR/$PROPFILE.compile
	"$JAVAC" -J-Xmx1500m -d "$BUILTDIR" -cp "$CP" -sourcepath "$SRCDIR" \
		"@$FILELIST" > "$COMPLOG" 2>&1
	COMP_RC=$?

	if [ "$COMP_RC" != 0 ]; then
		echo "  COMPILE FAILED -- see $COMPLOG"
		echo "  ----------------------------------------------------------------"
		grep 'error:' "$COMPLOG" | head -40
		echo "  ----------------------------------------------------------------"
		PASS_OR_FAIL=1
		continue
	fi

	NCLASS=$(find "$BUILTDIR/$PKG_DIR" -name '*.class' 2>/dev/null | wc -l | tr -d ' ')
	echo "  OK -- $NCLASS classes"
done

# --- package the compiled tree so Maven does not recompile it ---------------
# The generated-tests profile used to add $SRCDIR as a TEST-SOURCE root, so `mvn test`
# compiled all ~11,700 generated files a SECOND time -- everything above had already
# compiled them into $BUILTDIR. That duplicate javac is the single largest allocation in
# a run and was being SIGKILLed by the OS on a memory-tight machine, which reads as a
# mysterious mid-run death rather than an out-of-memory error (no OutOfMemoryError is
# ever printed; the log just stops).
#
# So publish the classes as a jar and let the profile put THAT on the test classpath. The
# T* harnesses still compile against the generated classes exactly as before -- they just
# link to them instead of rebuilding them.
if [ "$PASS_OR_FAIL" = 0 ]; then
	GENJAR=$OUTDIR/generated.jar
	echo
	echo "  packaging $(find "$BUILTDIR" -name '*.class' | wc -l | tr -d ' ') classes -> $GENJAR"
	if ! (cd "$BUILTDIR" && jar cf "$GENJAR" .); then
		echo "  ERROR: could not package $GENJAR"
		PASS_OR_FAIL=1
	fi
fi

echo
echo "=================================================================="
if [ "$PASS_OR_FAIL" = 0 ]; then
	echo "  ALL PROPFILES GENERATED + COMPILED OK"
	echo "  sources: $SRCDIR"
	echo "  classes: $BUILTDIR"
	echo "  jar    : $OUTDIR/generated.jar (what mvn test compiles against)"
else
	echo "  ONE OR MORE PROPFILES FAILED (see logs under $WORKDIR)"
fi
echo "=================================================================="
exit $PASS_OR_FAIL
