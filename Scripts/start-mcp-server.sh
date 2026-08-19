#!/bin/sh
#
# start-mcp-server.sh -- generate, compile and launch one config's MCP server in a running
#                        MCPDBWizard container, and wait until it is actually serving.
#
# This drives the Runtime page's own endpoints rather than doing anything behind the app's
# back: the config, the workspace and the child JVM all end up exactly as they would if you
# had clicked Start. Nothing is written into /data directly.
#
# Usage:
#   ./start-mcp-server.sh                       # config 'orindademo' on http://localhost:8080
#   ./start-mcp-server.sh payroll               # a different config
#   ./start-mcp-server.sh orindademo --restart  # stop it first if it is already running
#   ./start-mcp-server.sh --status              # just report, start nothing
#   ./start-mcp-server.sh --stop payroll
#
# Credentials come from the environment, never from this file:
#
#   MCPDBWIZARD_ADMIN_USER      defaults to 'admin'
#   MCPDBWIZARD_ADMIN_PASSWORD  required; prompted for if a terminal is attached
#   MCPDBWIZARD_URL             defaults to http://localhost:8080
#
# WHY IT WAITS RATHER THAN RETURNING. Start is asynchronous: the app generates, compiles and
# then forks a JVM, which takes tens of seconds. A script that posts and exits reports success
# for a server that may still fail to compile, so this polls the Runtime page until the state
# settles on RUNNING or FAILED and exits non-zero on the latter.
#
# Exit codes:  0 = running     1 = start failed or timed out     2 = usage / setup / auth
#
# Copyright 2003-2026 ATB Consultancy Services Ltd
# (formerly Orinda Software Ltd, Dublin, Ireland)

set -u

BASE=${MCPDBWIZARD_URL:-http://localhost:8080}
USERNAME=${MCPDBWIZARD_ADMIN_USER:-admin}
CONFIG=orindademo
ACTION=start
RESTART=no
WAIT_SECONDS=${MCPDBWIZARD_START_TIMEOUT:-180}

while [ $# -gt 0 ]; do
    case "$1" in
        --restart) RESTART=yes; shift ;;
        --status)  ACTION=status; shift ;;
        --stop)    ACTION=stop; shift ;;
        -h|--help) sed -n '2,30p' "$0" | sed 's/^#\{1,2\} \{0,1\}//'; exit 2 ;;
        -*)        echo "ERROR: unknown option: $1" >&2; exit 2 ;;
        *)         CONFIG=$1; shift ;;
    esac
done

command -v curl > /dev/null 2>&1 || { echo "ERROR: curl is required." >&2; exit 2; }

JAR=$(mktemp -t mcpdbwizard-cookies.XXXXXX) || exit 2
cleanup() { rm -f "$JAR" "$JAR.body"; }
trap cleanup EXIT INT TERM

# ---- helpers ---------------------------------------------------------------------------

# Spring Security issues a CSRF token per session and requires it on every POST. It is read
# fresh each time: the token rotates on login, so one captured before signing in is refused.
csrf_from() {
    curl -s -b "$JAR" -c "$JAR" "$BASE$1" \
        | grep -o 'name="_csrf" value="[^"]*"' | head -1 | sed 's/.*value="//;s/"//'
}

fail() { echo "!!! $1" >&2; exit "${2:-1}"; }

# ---- credentials -----------------------------------------------------------------------

PASSWORD=${MCPDBWIZARD_ADMIN_PASSWORD:-}
if [ -z "$PASSWORD" ]; then
    if [ -t 0 ]; then
        printf 'Password for %s at %s: ' "$USERNAME" "$BASE" >&2
        stty -echo 2>/dev/null; read -r PASSWORD; stty echo 2>/dev/null; echo >&2
    else
        fail "MCPDBWIZARD_ADMIN_PASSWORD is not set and there is no terminal to prompt on." 2
    fi
fi
[ -n "$PASSWORD" ] || fail "No password given." 2

# ---- sign in ---------------------------------------------------------------------------

echo "Signing in to $BASE as $USERNAME"
TOKEN=$(csrf_from /login)
[ -n "$TOKEN" ] || fail "No login form at $BASE/login -- is the container up?" 2

LOCATION=$(curl -s -b "$JAR" -c "$JAR" -o /dev/null -w '%{redirect_url}' -X POST "$BASE/login" \
    --data-urlencode "username=$USERNAME" \
    --data-urlencode "password=$PASSWORD" \
    --data-urlencode "_csrf=$TOKEN")

case "$LOCATION" in
    *error*|"") fail "Sign-in refused. Check MCPDBWIZARD_ADMIN_USER / _PASSWORD." 2 ;;
esac
# A forced password change swallows every other page, and the redirect above still looks
# like success -- so say what is actually wrong rather than timing out later on a status poll.
case "$(curl -s -b "$JAR" -o /dev/null -w '%{redirect_url}' "$BASE/runtime")" in
    *change-password*) fail "This account must set a new password before it can start servers." 2 ;;
esac

# ---- report ----------------------------------------------------------------------------

status_line() {
    curl -s -b "$JAR" "$BASE/runtime" > "$JAR.body"
    # One row per config; pull the block for ours and read its state.
    awk -v cfg="$CONFIG" '
        $0 ~ ">" cfg "<" { found = 1 }
        found && match($0, /STOPPED|STARTING|RUNNING|FAILED/) {
            print substr($0, RSTART, RLENGTH); exit
        }
    ' "$JAR.body"
}

report() {
    S=$(status_line)
    echo "  $CONFIG: ${S:-unknown}"
    [ "${S:-}" = RUNNING ] && echo "  reachable at $BASE/mcp/$CONFIG (needs an API token from the Users page)"
}

if [ "$ACTION" = status ]; then
    report
    exit 0
fi

# ---- stop ------------------------------------------------------------------------------

do_stop() {
    echo "Stopping $CONFIG"
    TOKEN=$(csrf_from /runtime)
    curl -s -b "$JAR" -c "$JAR" -o /dev/null -X POST "$BASE/runtime/stop" \
        --data-urlencode "name=$CONFIG" --data-urlencode "_csrf=$TOKEN"
}

if [ "$ACTION" = stop ]; then
    do_stop; sleep 2; report; exit 0
fi

# ---- start -----------------------------------------------------------------------------

CURRENT=$(status_line)
if [ "$CURRENT" = RUNNING ]; then
    if [ "$RESTART" = yes ]; then
        do_stop; sleep 3
    else
        echo "$CONFIG is already RUNNING -- pass --restart to rebuild and relaunch it."
        report
        exit 0
    fi
fi

echo "Starting $CONFIG (generate, compile, launch -- this takes a while)"
TOKEN=$(csrf_from /runtime)
[ -n "$TOKEN" ] || fail "Could not read a CSRF token from $BASE/runtime." 2
curl -s -b "$JAR" -c "$JAR" -o /dev/null -X POST "$BASE/runtime/start" \
    --data-urlencode "name=$CONFIG" --data-urlencode "_csrf=$TOKEN" \
    || fail "The start request itself failed."

WAITED=0
LAST=""
while [ "$WAITED" -lt "$WAIT_SECONDS" ]; do
    S=$(status_line)
    if [ "$S" != "$LAST" ] && [ -n "$S" ]; then
        echo "  $S"
        LAST=$S
    fi
    case "$S" in
        RUNNING) echo; report; exit 0 ;;
        FAILED)  echo
                 echo "Start FAILED. The Runtime page shows the generation and compile logs:"
                 echo "  $BASE/runtime"
                 exit 1 ;;
    esac
    sleep 3
    WAITED=$((WAITED + 3))
done

echo
echo "Gave up after ${WAIT_SECONDS}s with $CONFIG in state '${LAST:-unknown}'."
echo "It may still be compiling -- check $BASE/runtime, or raise MCPDBWIZARD_START_TIMEOUT."
exit 1
