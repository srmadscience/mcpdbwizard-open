#!/bin/sh
#
# check_provisioning.sh -- assert every database object the propfiles name actually exists.
#
# A missing schema fixture does NOT fail the test suite: the generator just emits fewer classes
# and whatever those classes guarded stops being guarded. That is how the ORCL12 box quietly lost
# APPSCHEMA.MULTIREC (generic_test1 dropped from 2487 to 2482 files, the nested-record compile guard
# went inactive, and the suite still reported green). This script turns that into a loud failure.
#
# The expectation comes from the propfiles themselves -- every .pb2 declares the objects it
# introspects -- so there is no separate inventory to keep in step. See the header of
# Scripts/provisioning/CheckProvisioning.java.
#
# Usage:
#   Scripts/check_provisioning.sh                    # EVERY Propfiles/*.pb2
#   Scripts/check_provisioning.sh generic_test1 ...  # only the named propfiles
#
# Connection: the propfile's own USER/PASS against the ORINDA_TEST_* host, exactly as
# Scripts/testrun_current.sh does:
#   ORINDA_TEST_HOST  ORINDA_TEST_PORT  ORINDA_TEST_SID   (SID may be /SERVICE_NAME)
#
# Exit codes: 0 = all present (or DB unreachable, reported as SKIPPED), 1 = something is missing.
#
# Run it before a regen you intend to trust:
#   Scripts/check_provisioning.sh && Scripts/testrun_current.sh
#
set -u

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
HOMEDIR=$(cd "$SCRIPT_DIR/.." && pwd)

JAR=$HOMEDIR/target/mcpdbwizard-app-2.0.0-SNAPSHOT-shaded.jar
SRC=$SCRIPT_DIR/provisioning/CheckProvisioning.java
JAVA=${JAVA:-java}

if [ ! -f "$JAR" ]; then
	echo "ERROR: shaded jar not found (it carries the Oracle driver):"
	echo "       $JAR"
	echo "       build it first:  mvn -DskipTests package"
	exit 2
fi
if [ ! -f "$SRC" ]; then
	echo "ERROR: checker source not found: $SRC"
	exit 2
fi

if [ "$#" -gt 0 ]; then
	PROPFILES=""
	for name in "$@"; do
		PROPFILES="$PROPFILES $HOMEDIR/Propfiles/$(basename "$name" .pb2).pb2"
	done
else
	# EVERY propfile, not the generic_test* glob this used to use. A 41-propfile regen passes its
	# whole list to this script, so the old default checked a SMALLER set than the run it was meant
	# to clear: a hand-run "PASSED" over 770 objects while the regen's own preflight then failed
	# over 1186. A check whose scope is narrower than the thing it certifies is worse than no check,
	# because its answer is believed.
	PROPFILES=$(ls "$HOMEDIR"/Propfiles/*.pb2)
fi

echo "=================================================================="
# Local box inventory (gitignored; see boxes.env.template). Sourced for its DEFAULTS
# only -- an environment variable set by the caller always wins, because every
# assignment in that file is of the ${VAR:-default} form.
BOXES_ENV="$(cd "$(dirname "$0")" && pwd)/boxes.env"
[ -f "$BOXES_ENV" ] && . "$BOXES_ENV"
echo "  provisioning check against ${MCPDBWIZARD_TEST_HOST:-${ORINDA_TEST_HOST:-<unset>}}:${MCPDBWIZARD_TEST_PORT:-${ORINDA_TEST_PORT:-1521}}${MCPDBWIZARD_TEST_SID:-${ORINDA_TEST_SID:-<unset>}}"
echo "=================================================================="

# Single-file source mode (JEP 330): no compile step, the jar supplies the JDBC driver.
"$JAVA" -cp "$JAR" -Dob.knownAbsentFile="$SCRIPT_DIR/provisioning/known-absent.txt" \
	"$SRC" $PROPFILES
exit $?
