#!/usr/bin/env bash
#
# Regression check that the generator runs off the new .json config format identically to the
# classic .pb2 (com.mcpdbwizard.schema + ApplicationShell.loadConfig).
#
# For one propfile it: rewrites the connection lines to the target DB, derives an equivalent
# .json from that .pb2 with com.mcpdbwizard.schema.ConfigConverter, then runs the generator (batch)
# once off the .pb2 and once off the .json into the same output dir (snapshotting between, so any
# path baked into the output is identical for both). With Javadoc/comment blocks stripped the two
# trees must be byte-identical — the only run-to-run differences are the header timestamp and the
# build stamp, both of which live in comments. PASS = identical generated code.
#
# Usage:  Scripts/json-config-test/run.sh [propfile-basename]     (default: generic_test2)
#   Env (default = ORCL12): ORINDA_TEST_HOST, ORINDA_TEST_PORT, ORINDA_TEST_SID, ORINDA_TEST_VERSION
#     (SID with a leading '/' -> service-name URL form, e.g. /orcl or /FREEPDB1)
#   Requires: the target schema authored by the chosen propfile's USER/PASS reachable, and the
#     shaded jar (mvn -Dmaven.test.skip=true package). Self-skips (exit 0) if the DB is unreachable.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HOMEDIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
JAR="$HOMEDIR/target/mcpdbwizard-app-2.0.0-SNAPSHOT-shaded.jar"
GEN=com.mcpdbwizard.app.procbuilder.gui.ProcBuilder
CONV=com.mcpdbwizard.schema.ConfigConverter

PROPFILE="${1:-generic_test2}"
PB_SRC="$HOMEDIR/Propfiles/$PROPFILE.pb2"

# Local box inventory (gitignored; see boxes.env.template). Sourced for its DEFAULTS
# only -- an environment variable set by the caller always wins, because every
# assignment in that file is of the ${VAR:-default} form.
BOXES_ENV="$(cd "$(dirname "$0")" && pwd)/../boxes.env"
[ -f "$BOXES_ENV" ] && . "$BOXES_ENV"
HOST="${MCPDBWIZARD_TEST_HOST:-${ORINDA_TEST_HOST:-}}"
PORT="${MCPDBWIZARD_TEST_PORT:-${ORINDA_TEST_PORT:-1521}}"
SID="${MCPDBWIZARD_TEST_SID:-${ORINDA_TEST_SID:-/orcl}}"
VERSION="${MCPDBWIZARD_TEST_VERSION:-${ORINDA_TEST_VERSION:-12.1.0}}"

if [ ! -f "$JAR" ]; then
	echo "SKIP: shaded jar not found ($JAR) — run: mvn -Dmaven.test.skip=true package"
	exit 0
fi
if [ ! -f "$PB_SRC" ]; then
	echo "SKIP: no such propfile: $PB_SRC"
	exit 0
fi
if ! nc -z -G 4 "$HOST" "$PORT" >/dev/null 2>&1; then
	echo "SKIP: DB $HOST:$PORT unreachable — this check needs the propfile's live schema."
	exit 0
fi

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
OUT="$WORK/out"; LOG="$WORK/log"
mkdir -p "$OUT" "$LOG"

# 1. rewrite connection/version, leaving the committed .pb2 untouched.
PB="$WORK/$PROPFILE.pb2"
sed -E \
	-e "s#^HOSTNAME=.*#HOSTNAME=$HOST#" \
	-e "s#^PORT=.*#PORT=$PORT#" \
	-e "s#^ORACLE_SID=.*#ORACLE_SID=$SID#" \
	-e "s#^ORACLE_VERSION=.*#ORACLE_VERSION=$VERSION#" \
	"$PB_SRC" > "$PB"

# 2. derive the equivalent .json via the real converter.
JSON="$WORK/$PROPFILE.json"
java -cp "$JAR" "$CONV" "$PB" "$JSON"
echo "== converted $PROPFILE.pb2 -> .json ($(wc -c <"$JSON" | tr -d ' ') bytes) =="

gen() {  # $1 = config file, $2 = snapshot label
	rm -rf "$OUT"; mkdir -p "$OUT"
	java -Djava.awt.headless=true -cp "$JAR" "$GEN" \
		"$LOG" build \
		"CODE_BASE_DIRECTORY=$OUT" "SQL_FILE_DIRECTORY=$WORK/sql" \
		"$1" > "$WORK/$2.genlog" 2>&1 || true
	if ! grep -q "Finished processing file" "$WORK/$2.genlog"; then
		echo "FAIL: generation from $1 did not finish"; tail -20 "$WORK/$2.genlog"; exit 1
	fi
	local n; n=$(find "$OUT" -name '*.java' | wc -l | tr -d ' ')
	echo "  [$2] generated $n java files"
	if [ "$n" = 0 ]; then
		# A connect/login failure (service down, PDB mounted, bad creds) is an environment
		# problem, not a format regression — skip rather than fail so the check stays quiet
		# when the target DB is not fully available.
		if grep -qE "ORA-(12514|12541|12505|12528|01017|12154|17002)" "$WORK/$2.genlog"; then
			echo "SKIP: DB reachable but not usable for $PROPFILE (connect error) — see below:"
			grep -oE "ORA-[0-9]+[^)]*" "$WORK/$2.genlog" | head -1
			exit 0
		fi
		echo "FAIL: no files generated from $2 (and no connect error to explain it)"
		tail -20 "$WORK/$2.genlog"; exit 1
	fi
	rm -rf "$WORK/snap_$2"; cp -R "$OUT" "$WORK/snap_$2"
}

echo "== generating against $HOST:$PORT$SID =="
gen "$PB"   pb2
gen "$JSON" json

# 3. strip all comment blocks (timestamp + rotating tips live only in Javadoc) and diff the code.
strip() {  # $1 = snapshot dir -> $2 = out dir
	rm -rf "$2"
	(cd "$1" && find . -name '*.java' | while read -r f; do
		mkdir -p "$2/$(dirname "$f")"
		sed '/\/\*/,/\*\//d' "$f" > "$2/$f"
	done)
}
strip "$WORK/snap_pb2"  "$WORK/nc_pb2"
strip "$WORK/snap_json" "$WORK/nc_json"

if diff -r "$WORK/nc_pb2" "$WORK/nc_json" > "$WORK/code_diff.txt" 2>&1; then
	echo "PASS: generated code is byte-identical from .pb2 and .json ($PROPFILE)"
	exit 0
else
	echo "FAIL: generated code differs between .pb2 and .json:"
	head -60 "$WORK/code_diff.txt"
	exit 1
fi
