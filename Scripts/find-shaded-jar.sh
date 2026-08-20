#!/bin/sh
#
# find-shaded-jar.sh -- resolve the shaded generator jar WITHOUT hardcoding its version.
#
# Source it, do not run it. $HOMEDIR must already point at the app module root; every caller
# computes that before it needs the jar. On success it sets $JAR. On failure it leaves $JAR
# holding the PATTERN it searched, so the caller's existing "shaded jar not found" message
# still prints something a reader can act on.
#
#   . "$SCRIPT_DIR/find-shaded-jar.sh"      # or ../find-shaded-jar.sh from a subdirectory
#
# WHY THIS EXISTS. The jar filename carries the project version, so four scripts held a literal
# path to mcpdbwizard-app-<version>-shaded.jar. Stamping the version breaks all four at once,
# and not at the point of the edit: the regen and the provisioning preflight simply stop finding
# the generator, and report it as a missing build rather than as a version bump nobody finished.
#
# WHY IT REFUSES MORE THAN ONE MATCH rather than taking the newest. `mvn package` without a
# `clean` leaves the previous version's jar beside the new one, and that is exactly when a bare
# glob quietly picks the wrong file. Everything downstream then runs against a stale generator
# and the difference shows up as unexplained output changes, a long way from the cause. Refusing
# is cheap; picking wrong is expensive and silent.
#
# Copyright 2003-2026 ATB Consultancy Services Ltd
# (formerly Orinda Software Ltd, Dublin, Ireland)

_ob_jar_pattern="$HOMEDIR/target/mcpdbwizard-app-*-shaded.jar"

JAR=""
for _ob_candidate in $_ob_jar_pattern; do
	[ -f "$_ob_candidate" ] || continue
	if [ -n "$JAR" ]; then
		echo "ERROR: more than one shaded jar in $HOMEDIR/target:" >&2
		for _ob_dup in $_ob_jar_pattern; do
			[ -f "$_ob_dup" ] && echo "         $_ob_dup" >&2
		done
		echo "       Which one is current cannot be guessed, and running the wrong one" >&2
		echo "       shows up later as unexplained output changes. Clear it with:" >&2
		echo "         mvn clean package -DskipTests" >&2
		exit 2
	fi
	JAR="$_ob_candidate"
done

# No match: hand the caller the pattern so its own error message names what was looked for.
[ -n "$JAR" ] || JAR="$_ob_jar_pattern"

unset _ob_candidate _ob_dup _ob_jar_pattern
