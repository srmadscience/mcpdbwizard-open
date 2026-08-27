#!/bin/bash
#
# run-loadtest.sh -- load-test a PUBLISHED MCPDBWizard image, end to end, from a cold start.
#
#   ./run-loadtest.sh                             pull the default image, start it, run the workload
#   ./run-loadtest.sh --image ...:2.0.5            a different image
#   ./run-loadtest.sh --for 5m --rate 200          pass anything through to the load client
#   ./run-loadtest.sh --list                       what does this config publish?
#   ./run-loadtest.sh --proxy                      through /mcp/<config> instead of the direct port
#   ./run-loadtest.sh --stop                       stop and remove the container, then exit
#
# WHAT THIS ADDS OVER Scripts/loadtest/mcp-load.sh, which it calls. That script assumes a
# container is already up with a config built. This one starts from an IMAGE: it pulls, runs,
# waits for the web app, CHECKS THE DATABASE CREDENTIAL, and only then loads. Use it to test what
# was actually published rather than what is in the working tree.
#
# ---------------------------------------------------------------------------------------
# THE CREDENTIAL GATE RUNS BEFORE THE CONTAINER STARTS, AND THAT ORDERING IS THE WHOLE FIX.
#
# A generated MCP server opens a pooled connection per call, and a failing call INVALIDATES its
# pooled connection -- so a load run against a bad password is not a run that reports errors, it
# is a LOGON STORM. A previous incident took a box to 198 of 200 processes.
#
# THE FIRST VERSION OF THIS SCRIPT CHECKED AFTER STARTING THE CONTAINER, AND THAT WAS USELESS.
# It locked the demo account on 2026-08-26 while doing so. The load client never got to run: the
# CONTAINER burns the attempts on its own, because the web app AND every RUN_ON_START MCP server
# connect the moment it comes up and keep retrying. With FAILED_LOGIN_ATTEMPTS=10 -- the DEFAULT
# profile, so assume it -- one `docker run` with a stale DB_PASS crosses the threshold inside a
# minute, and PASSWORD_LOCK_TIME=1 makes that a 24-hour outage. Three careful manual attempts had
# cost nothing; one container start finished the job.
#
# So the gate is now a THROWAWAY container that makes exactly ONE JDBC login and exits: no web
# app, no MCP server, no pool, no retry (DbProbe.java, run by single-file source launch against
# the image's own ojdbc). Nothing long-lived starts until it passes.
#
# It also refuses to keep guessing: a wrong password is reported, never retried with a variation.
# On Oracle 23ai a LOCKED account and a WRONG PASSWORD both report ORA-01017 -- the messages were
# merged to stop user enumeration -- so a retry loop cannot tell "I mistyped it" from "I am the
# reason it is locked". Check DBA_USERS.ACCOUNT_STATUS as a DIFFERENT user instead; the script
# prints that query rather than running it, because it needs DBA rights this does not have.
# ---------------------------------------------------------------------------------------
#
# CREDENTIALS: none are held here. The Oracle settings come from --env-file (default ./.env, the
# same file docker-compose.yml reads), exactly as the documented `docker run` does. This script
# never prints DB_PASS and never writes it anywhere.
#
# Copyright 2003-2026 ATB Consultancy Services Ltd
# (formerly Orinda Software Ltd, Dublin, Ireland)
set -euo pipefail

REPO_ROOT=$(cd "$(dirname "$0")/../../.." && pwd)
IMAGE="${MCPDBWIZARD_IMAGE:-ghcr.io/srmadscience/mcpdbwizard:2.0.8}"
CONTAINER="${CONTAINER:-mcpdbwizard-load}"
ENV_FILE="${ENV_FILE:-$REPO_ROOT/.env}"
CONFIG="${CONFIG:-mcpdemo}"
VOLUME="${VOLUME:-mcpdbwizard-demo}"
WEB_PORT="${WEB_PORT:-8080}"
METRICS_PORT="${METRICS_PORT:-9464}"
WORKLOAD="${WORKLOAD:-}"
KEEP=no
PROXY=no
ARGS=()

while [ $# -gt 0 ]; do
    case "$1" in
        --image)     IMAGE="$2"; shift 2 ;;
        --config)    CONFIG="$2"; shift 2 ;;
        --env-file)  ENV_FILE="$2"; shift 2 ;;
        --workload)  WORKLOAD="$2"; shift 2 ;;
        --proxy)     PROXY=yes; shift ;;
        --keep)      KEEP=yes; shift ;;
        --stop)      docker rm -f "$CONTAINER" >/dev/null 2>&1 && echo "removed $CONTAINER" \
                         || echo "$CONTAINER was not running"
                     exit 0 ;;
        *)           ARGS+=("$1"); shift ;;
    esac
done

# AFTER the argument loop, not before it: derived from whatever --config finally says. Computed
# up with the other defaults it was bound to the DEFAULT config, so `--config mine` silently kept
# looking for workload-mcpdemo.json -- a workload for somebody else's schema, whose tool names
# would then be refused by the pre-run check as if the caller had mistyped them.
WORKLOAD="${WORKLOAD:-$(dirname "$0")/workload-$CONFIG.json}"

# Does this invocation actually need a workload? --list and --tools do not, and --list is the
# FIRST thing anyone runs against a config they have not written one for yet -- so demanding the
# file unconditionally would break the one command that tells you how to write it.
# ${ARGS[*]}, NOT $* -- the argument loop above has already shifted $@ empty, so testing $* here
# matches nothing and refuses --list, the one command this guard exists to point people at.
NEEDS_WORKLOAD=yes
case " ${ARGS[*]} " in
    *" --list "*|*" --tools "*) NEEDS_WORKLOAD=no ;;
esac
if [ "$NEEDS_WORKLOAD" = yes ] && [ ! -f "$WORKLOAD" ]; then
    echo "no workload at $WORKLOAD" >&2
    echo "  Write one for this config, or point at another with --workload." >&2
    echo "  Start with:  $0 --config $CONFIG --list" >&2
    echo "  That prints every published tool WITH a ready-made workload entry to paste." >&2
    exit 2
fi

[ -f "$ENV_FILE" ] || { echo "no env file at $ENV_FILE -- see docker-compose.yml for the settings it needs" >&2; exit 2; }

# ---- 1. the credential gate -- ONE login, and nothing is running yet ---------------------
# A throwaway container: --rm, no name, no ports, no volume, no server. It cannot retry because
# it exits after one attempt, which is what makes a wrong password cost 1 of 10 rather than 10.
echo "=== credential gate (one login, before anything starts) ==="
PROBE_SRC="$(dirname "$0")/DbProbe.java"
[ -f "$PROBE_SRC" ] || { echo "missing $PROBE_SRC" >&2; exit 2; }
docker image inspect "$IMAGE" >/dev/null 2>&1 || { echo "pulling $IMAGE"; docker pull -q "$IMAGE"; }
set +e
PROBE_OUT=$(docker run --rm --env-file "$ENV_FILE" \
    -v "$(cd "$(dirname "$PROBE_SRC")" && pwd)/DbProbe.java:/tmp/DbProbe.java:ro" \
    --entrypoint java "$IMAGE" -cp '/app/lib/*' /tmp/DbProbe.java 2>&1 | grep -v 'Picked up')
PROBE_RC=${PIPESTATUS[0]}
set -e
echo "  $PROBE_OUT"
if [ "$PROBE_RC" -ne 0 ]; then
    cat >&2 <<'MSG'

STOPPING -- and nothing has started, so this cost ONE failed login rather than ten.

DO NOT retry with a different password. On 23ai a locked account and a wrong password
both report ORA-01017 (the messages were merged to stop user enumeration), so guessing
cannot tell the two apart and each guess brings a real lock closer. Ask a DBA -- as a
DIFFERENT user, which costs no attempt:

    select username, account_status, lock_date from dba_users where username = '<USER>';

  OPEN            -> the password in the env file is stale. Fix it there and in the
                     database together: they are one setting in two places.
  LOCKED(TIMED)   -> something is retrying. Find it and STOP IT BEFORE UNLOCKING, or the
                     unlock just restarts the storm. A container left running is the
                     usual culprit -- see --stop.
MSG
    exit 1
fi

# ---- 2. the container --------------------------------------------------------------------
if [ "$(docker inspect -f '{{.State.Running}}' "$CONTAINER" 2>/dev/null)" = true ]; then
    echo "reusing the running container $CONTAINER"
else
    docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
    echo "pulling $IMAGE"
    docker pull -q "$IMAGE"
    echo "starting $CONTAINER"
    # MCP_METRICS_HOST is set here as well as published: a generated server binds 127.0.0.1 by
    # default, so publishing the port alone forwards to a socket not on the external interface
    # and the connection is refused. Both halves are needed; it is easy to get half right.
    docker run -d --name "$CONTAINER" \
        --env-file "$ENV_FILE" \
        -e MCP_METRICS_HOST=0.0.0.0 \
        -p "$WEB_PORT:8080" -p "$METRICS_PORT:9464" \
        -v "$VOLUME:/data" \
        --memory 4g \
        "$IMAGE" > /dev/null
fi

printf 'waiting for the web app'
for _ in $(seq 1 60); do
    if curl -sf -o /dev/null "http://localhost:$WEB_PORT/login" 2>/dev/null; then break; fi
    printf '.'; sleep 2
done
echo
curl -sf -o /dev/null "http://localhost:$WEB_PORT/login" \
    || { echo "web app did not come up. Last log lines:"; docker logs --tail 20 "$CONTAINER"; exit 1; }
docker logs "$CONTAINER" 2>&1 | grep -m1 'Oracle connection configured' || true

# ---- 3. one MCP call, now that the credential is known good ------------------------------
# This is a SMOKE TEST, not the safety gate -- the gate was section 1, before anything started.
# It proves the server and its tools work, which a JDBC login cannot: the login says the database
# will have us, not that the generated server came up or that its tools bind.
echo
echo "=== smoke test (one tool call) ==="
SMOKE_TOOL=
[ -f "$WORKLOAD" ] && SMOKE_TOOL=$(sed -n 's/.*"tool"[ ]*:[ ]*"\([a-z0-9_]*\)".*/\1/p' "$WORKLOAD" | head -1)
if [ -n "$SMOKE_TOOL" ]; then
    SMOKE=$(CONTAINER="$CONTAINER" "$(dirname "$0")/mcp-load.sh" --config "$CONFIG" \
                --tools "$SMOKE_TOOL" --calls 1 --threads 1 2>&1 || true)
else
    # No workload yet: tools/list still proves the server came up and answers.
    SMOKE=$(CONTAINER="$CONTAINER" "$(dirname "$0")/mcp-load.sh" --config "$CONFIG" \
                --list 2>&1 || true)
fi
if echo "$SMOKE" | grep -qi 'tool error\|errors 1'; then
    echo "$SMOKE" | grep -i 'tool error' | head -2
    cat >&2 <<'MSG'

STOPPING. The credential gate passed, so the database will have us -- this is the SERVER
or the TOOL, not the login. A load run would repeat whatever this is tens of thousands of
times, and if it turns out to be connection-related it still becomes a logon storm,
because a failing call invalidates its pooled connection.

If the error DOES mention a login, the account state changed between the gate and now --
almost certainly something else retrying. Stop that first; do not re-run this.

Otherwise look at the server itself:

    docker logs <container> | tail -40
    docker exec <container> sh -c 'tail -40 /tmp/srv.log'
MSG
    exit 1
fi
echo "  ok -- the server answers and its tools bind"

# ---- 4. the load run ---------------------------------------------------------------------
if [ ${#ARGS[@]} -eq 0 ]; then
    ARGS=(--for 60s --threads 8 --warmup 10s)
    echo
    echo "no load options given; using the default: ${ARGS[*]}"
fi
case " ${ARGS[*]} " in
    *" --list "*|*" --tools "*|*" --workload "*) ;;
    *) ARGS+=(--workload "$WORKLOAD") ;;
esac
[ "$PROXY" = yes ] && set -- --proxy "$CONFIG" || set --

echo
CONTAINER="$CONTAINER" "$(dirname "$0")/mcp-load.sh" --config "$CONFIG" "$@" "${ARGS[@]}"
RC=$?

if [ "$KEEP" = no ]; then
    echo
    echo "leaving $CONTAINER running so a second run needs no cold start."
    echo "stop it with:  $0 --stop"
fi
exit $RC
