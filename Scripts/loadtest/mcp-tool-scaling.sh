#!/bin/bash
#
# mcp-tool-scaling.sh -- how many tools can one generated MCP server actually carry?
#
#   ./mcp-tool-scaling.sh                          # 1, 2, 4, 8, ... until something breaks
#   ./mcp-tool-scaling.sh --start 16 --max 512
#   ./mcp-tool-scaling.sh --calls 5 --memory 8g
#
# Each PASS takes a real config, clones one of its SQL statements N times, and drives the whole
# product path for that config: generate -> compile -> package -> launch -> call every cloned tool
# several times -> measure. N DOUBLES each pass. The run stops at the first pass that fails and
# reports WHICH PHASE failed, because "it broke" is not a finding -- "it broke in javac at 512
# tools" is.
#
# WHY DOUBLE RATHER THAN STEP. The interesting limits here are not linear: a method that exceeds
# javac's 64KB ceiling, a tools/list payload that outgrows a buffer, a metaspace that fills. Those
# arrive suddenly, and doubling finds the ORDER OF MAGNITUDE in a handful of passes. The last
# PASSED and the first FAILED line together bracket the limit; narrow it with --start if you want
# the exact number.
#
# WHAT COUNTS AS BROKEN. Any of: generation fails or times out, compilation fails, the server does
# not reach its port, the published tool count disagrees with what was asked for, any tool call
# fails or returns the wrong answer, or the container is OOM-killed. Each is reported by name.
#
# EVERY PASS IS A FRESH CONTAINER AND A FRESH VOLUME, both removed afterwards. It does NOT touch
# the repository's configs/ directory or any volume you already have -- the config is built in a
# temporary directory from --base-config, which is only ever read.
#
# WHAT IT NEEDS
#   - docker, curl, python3 on the PATH, and an image to test (--image, default mcpdbwizard-web:local)
#   - a reachable Oracle, named by MCPDBWIZARD_ORACLE_{HOST,PORT,SID,USER} and DB_PASS. An .env
#     beside the repository root is read if present, which is where a normal deployment keeps them.
#
# A NOTE ON THE NUMBERS IT PRINTS. Heap and RSS are read from inside the container with jcmd and
# /proc, per JVM, so they are that process's figures rather than the host's. The container total
# comes from `docker stats`, which is cgroup accounting and therefore only meaningful because this
# script always sets a memory limit. A pass that succeeds while the RSS column climbs steeply is
# worth as much attention as a pass that fails.
#
# Copyright 2003-2026 ATB Consultancy Services Ltd
# (formerly Orinda Software Ltd, Dublin, Ireland)

set -uo pipefail

# ---------------------------------------------------------------------------------------------
# Defaults
# ---------------------------------------------------------------------------------------------
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$HERE/../../.." && pwd)"

IMAGE="${IMAGE:-mcpdbwizard-web:local}"
BASE_CONFIG="${BASE_CONFIG:-$REPO_ROOT/configs/mcpdemo.json}"
CONFIG_NAME="mcpdemo"
CLONE_SOURCE=""              # which statement to clone; default = first parameterless one
START_CLONES=1
MAX_CLONES=4096
CALLS_PER_TOOL=3
MEMORY="4g"
HOST_PORT=18080
START_TIMEOUT=900            # seconds to wait for generate+compile+launch in one pass
ADMIN_PW='ScalingTest12345!'
KEEP=0                       # keep the last container for inspection

# Container/volume names carry the PID so two runs cannot collide.
CTR="mcp-scaling-$$"
VOL="mcp-scaling-vol-$$"
WORK="$(mktemp -d "${TMPDIR:-/tmp}/mcp-tool-scaling.XXXXXX")"

usage() {
    cat <<EOF
Usage: ./mcp-tool-scaling.sh [options]

  --image REF        image under test           (default: $IMAGE)
  --base-config PATH config to clone from       (default: $BASE_CONFIG)
  --clone-source F   .sql filename to clone     (default: first parameterless statement)
  --start N          clones in the first pass   (default: $START_CLONES)
  --max N            stop after this many       (default: $MAX_CLONES)
  --calls N          calls per tool per pass    (default: $CALLS_PER_TOOL)
  --memory SIZE      container memory limit     (default: $MEMORY)
  --port N           host port to publish on    (default: $HOST_PORT)
  --timeout SEC      per-pass start timeout     (default: $START_TIMEOUT)
  --keep             leave the last container running for inspection
  -h, --help         this message
EOF
}

while [ $# -gt 0 ]; do
    case "$1" in
        --image)        IMAGE="$2"; shift 2 ;;
        --base-config)  BASE_CONFIG="$2"; shift 2 ;;
        --clone-source) CLONE_SOURCE="$2"; shift 2 ;;
        --start)        START_CLONES="$2"; shift 2 ;;
        --max)          MAX_CLONES="$2"; shift 2 ;;
        --calls)        CALLS_PER_TOOL="$2"; shift 2 ;;
        --memory)       MEMORY="$2"; shift 2 ;;
        --port)         HOST_PORT="$2"; shift 2 ;;
        --timeout)      START_TIMEOUT="$2"; shift 2 ;;
        --keep)         KEEP=1; shift ;;
        -h|--help)      usage; exit 0 ;;
        *) echo "unknown option: $1" >&2; usage; exit 2 ;;
    esac
done

# ---------------------------------------------------------------------------------------------
# Housekeeping
# ---------------------------------------------------------------------------------------------
cleanup() {
    if [ "$KEEP" = "1" ] && [ -n "${KEEP_CTR:-}" ]; then
        echo "--keep: leaving container $KEEP_CTR and volume $VOL in place."
    else
        docker rm -f "$CTR" >/dev/null 2>&1
        docker volume rm "$VOL" >/dev/null 2>&1
    fi
    rm -rf "$WORK"
}
trap cleanup EXIT

fail_hard() { echo; echo "ABORTED: $*" >&2; exit 2; }

for tool in docker curl python3; do
    command -v "$tool" >/dev/null 2>&1 || fail_hard "$tool is not on the PATH."
done
docker info >/dev/null 2>&1 || fail_hard "the docker daemon is not reachable."
docker image inspect "$IMAGE" >/dev/null 2>&1 || fail_hard "no such image: $IMAGE (build it first)."
[ -r "$BASE_CONFIG" ] || fail_hard "cannot read base config: $BASE_CONFIG"

# The Oracle coordinates. An .env at the repository root is the normal home for these, and it is
# read WITHOUT exporting it wholesale -- only the keys this script needs, so an unrelated variable
# in that file cannot change the behaviour of the container under test.
ENV_FILE="$REPO_ROOT/.env"
read_env() {
    local key="$1" val=""
    eval "val=\${$key:-}"
    if [ -z "$val" ] && [ -r "$ENV_FILE" ]; then
        val="$(grep -m1 "^${key}=" "$ENV_FILE" 2>/dev/null | cut -d= -f2-)"
    fi
    printf '%s' "$val"
}
ORA_HOST="$(read_env MCPDBWIZARD_ORACLE_HOST)"
ORA_PORT="$(read_env MCPDBWIZARD_ORACLE_PORT)"; ORA_PORT="${ORA_PORT:-1521}"
ORA_SID="$(read_env MCPDBWIZARD_ORACLE_SID)"
ORA_USER="$(read_env MCPDBWIZARD_ORACLE_USER)"
ORA_PASS="$(read_env DB_PASS)"
[ -n "$ORA_HOST" ] || fail_hard "MCPDBWIZARD_ORACLE_HOST is not set and $ENV_FILE does not supply it."
[ -n "$ORA_PASS" ] || fail_hard "DB_PASS is not set and $ENV_FILE does not supply it."

BASE_URL="http://localhost:$HOST_PORT"
COOKIES="$WORK/cookies.txt"

echo "========================================================================================"
echo " MCP tool scaling test"
echo "   image        $IMAGE"
echo "   base config  $BASE_CONFIG"
echo "   oracle       $ORA_USER@$ORA_HOST:$ORA_PORT$ORA_SID"
echo "   passes       $START_CLONES clones, doubling, up to $MAX_CLONES"
echo "   per pass     $CALLS_PER_TOOL calls per cloned tool, ${MEMORY} container limit"
echo "========================================================================================"

# ---------------------------------------------------------------------------------------------
# Step 1 -- build a config with N clones of one statement.
#
# The clone source defaults to the first statement with NO bind parameters, so the driver can call
# every tool with an empty argument object. Cloning a parameterised statement would work equally
# well for generation but would make the call phase config-specific, and the call phase is the half
# that proves the server actually works rather than merely starts.
# ---------------------------------------------------------------------------------------------
build_config() {
    local clones="$1" out="$2"
    python3 - "$BASE_CONFIG" "$out" "$clones" "$CLONE_SOURCE" <<'PY'
import copy, json, re, sys
src_path, out_path, clones, wanted = sys.argv[1], sys.argv[2], int(sys.argv[3]), sys.argv[4]
cfg = json.load(open(src_path))
stmts = cfg.get('sqlStatements') or []
if not stmts:
    sys.exit("the base config has no sqlStatements to clone")

def parameterless(s):
    # A '?' outside a comment is a bind. Cheap and good enough: the generator is the authority,
    # and a wrong guess here shows up immediately as a failed call rather than a silent pass.
    return '?' not in re.sub(r'/\*.*?\*/', '', s.get('sql', ''), flags=re.S)

if wanted:
    base = next((s for s in stmts if s['filename'] == wanted), None)
    if base is None:
        sys.exit("no statement named %s in the base config" % wanted)
else:
    base = next((s for s in stmts if parameterless(s)), None)
    if base is None:
        sys.exit("no parameterless statement to clone; pass --clone-source")

nxt = int(max(s['index'] for s in stmts)) + 1
for n in range(1, clones + 1):
    c = copy.deepcopy(base)
    c['index'] = float(nxt + n - 1)
    c['filename'] = 'scaling_clone%05d.sql' % n
    c['mcpDescription'] = 'Scaling clone %05d of %s.' % (n, base['filename'])
    stmts.append(c)
cfg['sqlStatements'] = stmts
json.dump(cfg, open(out_path, 'w'), indent=2)
print(base['filename'])
PY
}

# ---------------------------------------------------------------------------------------------
# Step 2 -- container lifecycle.
#
# ADMIN_INITIAL_PASSWORD is set deliberately: without it the account seeds admin/password and
# FORCES a change on first login, which a script cannot walk through. The staged volume carries
# only the config, so every pass starts from a genuinely clean state rather than inheriting the
# previous pass's workspace -- which would let a stale jar decide the result.
# ---------------------------------------------------------------------------------------------
start_container() {
    local config_file="$1"
    docker rm -f "$CTR" >/dev/null 2>&1
    docker volume rm "$VOL" >/dev/null 2>&1
    docker volume create "$VOL" >/dev/null

    # COPYFILE_DISABLE is load-bearing on macOS: bsdtar otherwise writes an AppleDouble ._file
    # beside every entry, and ConfigStore reads "._mcpdemo.json" as a SECOND config named
    # "._mcpdemo". The symptom is an extra broken row on the Runtime page, not an error.
    COPYFILE_DISABLE=1 tar -cf - -C "$(dirname "$config_file")" "$(basename "$config_file")" \
        | docker run --rm -i -v "$VOL":/data alpine \
            sh -c "cd /data && tar -xf - && mv '$(basename "$config_file")' ${CONFIG_NAME}.json && chown -R 1001:1001 /data" \
        || return 1

    docker run -d --name "$CTR" \
        --memory "$MEMORY" \
        -e SPRING_PROFILES_ACTIVE=docker \
        -e MCPDBWIZARD_ORACLE_HOST="$ORA_HOST" \
        -e MCPDBWIZARD_ORACLE_PORT="$ORA_PORT" \
        -e MCPDBWIZARD_ORACLE_SID="$ORA_SID" \
        -e MCPDBWIZARD_ORACLE_USER="$ORA_USER" \
        -e DB_PASS="$ORA_PASS" \
        -e ADMIN_USERNAME=admin \
        -e ADMIN_INITIAL_PASSWORD="$ADMIN_PW" \
        -p "$HOST_PORT":8080 \
        -v "$VOL":/data \
        "$IMAGE" >/dev/null || return 1

    local waited=0
    until curl -sf -o /dev/null "$BASE_URL/login"; do
        sleep 1; waited=$((waited + 1))
        [ "$waited" -gt 60 ] && return 1
        docker ps -q -f "name=$CTR" | grep -q . || return 1
    done
    return 0
}

csrf_of() { grep -o 'name="_csrf" value="[^"]*"' "$1" | head -1 | sed 's/.*value="//;s/"//'; }

console_login() {
    rm -f "$COOKIES"
    local t
    t="$(curl -s -c "$COOKIES" "$BASE_URL/login" | grep -o 'name="_csrf" value="[^"]*"' | sed 's/.*value="//;s/"//')"
    [ -n "$t" ] || return 1
    curl -s -b "$COOKIES" -c "$COOKIES" -o /dev/null \
        -d "username=admin&password=$ADMIN_PW&_csrf=$t" "$BASE_URL/login" || return 1
    curl -s -b "$COOKIES" -c "$COOKIES" -o "$WORK/runtime.html" -L "$BASE_URL/runtime" || return 1
    grep -q "$CONFIG_NAME" "$WORK/runtime.html"
}

press_start() {
    local c; c="$(csrf_of "$WORK/runtime.html")"
    [ -n "$c" ] || return 1
    curl -s -b "$COOKIES" -c "$COOKIES" -o /dev/null \
        -d "name=$CONFIG_NAME&_csrf=$c" "$BASE_URL/runtime/start"
}

# Waits for a TERMINAL state, and distinguishes them. A wait that only looks for success is how a
# crashed pass becomes indistinguishable from a slow one.
await_running() {
    local waited=0
    while [ "$waited" -lt "$START_TIMEOUT" ]; do
        local logs; logs="$(docker logs "$CTR" 2>&1 | tail -400)"
        case "$logs" in
            *"running on port"*)            echo "running";  return 0 ;;
            *"Generation failed"*)          echo "generation-failed"; return 1 ;;
            *"Generation timed out"*)       echo "generation-timeout"; return 1 ;;
            *"Compilation of the generated code failed"*) echo "compile-failed"; return 1 ;;
            *"Could not package"*)          echo "package-failed"; return 1 ;;
            *"Runtime error"*)              echo "runtime-error"; return 1 ;;
        esac
        docker ps -q -f "name=$CTR" | grep -q . || { echo "container-died"; return 1; }
        sleep 3; waited=$((waited + 3))
    done
    echo "timeout-after-${START_TIMEOUT}s"
    return 1
}

# ---------------------------------------------------------------------------------------------
# Step 3 -- an MCP session through the proxy, which is the deployed path. Reaching the generated
# server's own port would skip the half of the product that decides which account may call it.
# ---------------------------------------------------------------------------------------------
open_session() {
    curl -s -b "$COOKIES" -c "$COOKIES" -o "$WORK/users.html" "$BASE_URL/admin/users" || return 1
    local c; c="$(csrf_of "$WORK/users.html")"
    curl -s -b "$COOKIES" -c "$COOKIES" -o /dev/null \
        -d "username=admin&_csrf=$c" "$BASE_URL/admin/users/issue-token" || return 1
    curl -s -b "$COOKIES" -c "$COOKIES" -o "$WORK/issued.html" -L "$BASE_URL/admin/users" || return 1
    TOKEN="$(grep -oE '[A-Za-z0-9_-]{6,}\.[A-Za-z0-9_-]{16,}' "$WORK/issued.html" | head -1)"
    [ -n "$TOKEN" ] || return 1

    curl -s -X POST "$BASE_URL/mcp/$CONFIG_NAME" \
        -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
        -H 'Accept: application/json, text/event-stream' -D "$WORK/h.txt" -o /dev/null \
        -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"mcp-tool-scaling","version":"1"}}}' || return 1
    SESSION="$(grep -i '^mcp-session-id' "$WORK/h.txt" | tr -d '\r' | cut -d' ' -f2)"
    [ -n "$SESSION" ] || return 1
    mcp '{"jsonrpc":"2.0","method":"notifications/initialized"}' >/dev/null
    return 0
}

mcp() {
    curl -s -X POST "$BASE_URL/mcp/$CONFIG_NAME" \
        -H "Authorization: Bearer $TOKEN" -H "mcp-session-id: $SESSION" \
        -H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream' \
        -d "$1"
}

# ---------------------------------------------------------------------------------------------
# Step 4 -- memory. Anchored on the java binary path so the probe's own shell, whose command line
# necessarily contains the pattern being searched for, cannot match itself and be counted as a
# second server. That false positive has already been hit by hand once.
# ---------------------------------------------------------------------------------------------
measure() {
    docker exec "$CTR" sh -c '
      for p in /proc/[0-9]*; do
        pid=${p#/proc/}
        c=$(tr "\0" " " < $p/cmdline 2>/dev/null)
        case "$c" in
          /opt/java/openjdk/bin/java*DaoFactoryMcpServer*) role=server ;;
          /opt/java/openjdk/bin/java*app.jar*|java*app.jar*) role=web ;;
          *) continue ;;
        esac
        mh=$(/opt/java/openjdk/bin/jcmd $pid VM.flags 2>/dev/null | tr " " "\n" | grep -m1 "^-XX:MaxHeapSize=" | cut -d= -f2)
        hu=$(/opt/java/openjdk/bin/jcmd $pid GC.heap_info 2>/dev/null | grep -m1 -o "used [0-9]*K" | tr -dc 0-9)
        rss=$(awk "/^VmRSS/{print \$2}" $p/status 2>/dev/null)
        [ -n "$mh" ] && [ -n "$hu" ] && echo "$role $((mh/1048576)) $((hu/1024)) $((rss/1024))"
      done' 2>/dev/null
}

container_mem() { docker stats --no-stream --format '{{.MemUsage}}' "$CTR" 2>/dev/null; }
was_oom_killed() { [ "$(docker inspect "$CTR" --format '{{.State.OOMKilled}}' 2>/dev/null)" = "true" ]; }

# ---------------------------------------------------------------------------------------------
# One pass.
# ---------------------------------------------------------------------------------------------
RESULTS="$WORK/results.txt"
: > "$RESULTS"

run_pass() {
    local clones="$1"
    local t0 t1 gen_ms call_ms
    echo
    echo "---------------------------------------------------------------------------------------"
    echo "PASS: $clones cloned tools"
    echo "---------------------------------------------------------------------------------------"

    local cfg="$WORK/config-$clones.json" cloned_from
    cloned_from="$(build_config "$clones" "$cfg")" || { record "$clones" FAILED build-config - - - -; return 1; }
    printf '  cloning %s x%d ... config is %s KB\n' "$cloned_from" "$clones" "$(( $(wc -c < "$cfg") / 1024 ))"

    t0=$(date +%s)
    start_container "$cfg" || { record "$clones" FAILED container-start - - - -; return 1; }
    console_login      || { record "$clones" FAILED console-login - - - -; return 1; }
    press_start        || { record "$clones" FAILED press-start - - - -; return 1; }

    local state; state="$(await_running)"
    t1=$(date +%s); gen_ms=$(( (t1 - t0) ))
    if [ "$state" != "running" ]; then
        echo "  generate/compile/launch: FAILED ($state) after ${gen_ms}s"
        docker logs "$CTR" 2>&1 | grep -iE "error|exception|failed" | tail -5 | sed 's/^/    | /'
        if was_oom_killed; then echo "    | container was OOM-KILLED"; state="$state+oom"; fi
        record "$clones" FAILED "$state" "$gen_ms" - - -
        return 1
    fi
    echo "  generate/compile/launch: ok in ${gen_ms}s"

    open_session || { record "$clones" FAILED mcp-session "$gen_ms" - - -; return 1; }

    mcp '{"jsonrpc":"2.0","id":2,"method":"tools/list"}' \
        | grep -o '"name":"[^"]*"' | sed 's/"name":"//;s/"//' > "$WORK/tools.txt"
    local total advertised
    total=$(wc -l < "$WORK/tools.txt" | tr -d ' ')
    grep -c '^scaling_clone' "$WORK/tools.txt" > "$WORK/n.txt" 2>/dev/null
    advertised=$(cat "$WORK/n.txt")
    echo "  tools published: $total total, $advertised of them clones (wanted $clones)"
    if [ "$advertised" != "$clones" ]; then
        echo "  tool inventory: FAILED -- the server published $advertised clones, not $clones"
        record "$clones" FAILED tool-count "$gen_ms" "$total" - -
        return 1
    fi

    # Call every clone, CALLS_PER_TOOL times, checking each answer.
    local ok=0 bad=0 id=1000
    : > "$WORK/hits.txt"
    t0=$(date +%s%N)
    local round tool body
    for round in $(seq 1 "$CALLS_PER_TOOL"); do
        while read -r tool; do
            id=$((id + 1))
            body="$(mcp "{\"jsonrpc\":\"2.0\",\"id\":$id,\"method\":\"tools/call\",\"params\":{\"name\":\"$tool\",\"arguments\":{}}}")"
            if printf '%s' "$body" | grep -q "\"id\":$id" \
               && printf '%s' "$body" | grep -q '"result"' \
               && ! printf '%s' "$body" | grep -q '"isError":true'; then
                ok=$((ok + 1)); echo "$tool" >> "$WORK/hits.txt"
            else
                bad=$((bad + 1))
                [ "$bad" -le 3 ] && echo "    | FAIL $tool: $(printf '%s' "$body" | tr -d '\n' | head -c 160)"
            fi
        done < <(grep '^scaling_clone' "$WORK/tools.txt")
    done
    t1=$(date +%s%N); call_ms=$(( (t1 - t0) / 1000000 ))
    local distinct; distinct=$(sort -u "$WORK/hits.txt" | wc -l | tr -d ' ')
    echo "  tool calls: $ok ok, $bad failed, over $distinct distinct tools, in ${call_ms} ms"

    local m; m="$(measure)"
    local web_line srv_line
    web_line="$(printf '%s\n' "$m" | awk '$1=="web"{print; exit}')"
    srv_line="$(printf '%s\n' "$m" | awk '$1=="server"{print; exit}')"
    printf '  web app     max %s MB, heap %s MB, RSS %s MB\n' \
        "$(echo "$web_line" | awk '{print $2}')" "$(echo "$web_line" | awk '{print $3}')" "$(echo "$web_line" | awk '{print $4}')"
    printf '  MCP server  max %s MB, heap %s MB, RSS %s MB\n' \
        "$(echo "$srv_line" | awk '{print $2}')" "$(echo "$srv_line" | awk '{print $3}')" "$(echo "$srv_line" | awk '{print $4}')"
    echo "  container:  $(container_mem)"

    if [ "$bad" -gt 0 ]; then
        record "$clones" FAILED tool-calls "$gen_ms" "$total" "$(echo "$srv_line" | awk '{print $4}')" "$call_ms"
        return 1
    fi
    if was_oom_killed; then
        echo "  container was OOM-KILLED"
        record "$clones" FAILED oom-killed "$gen_ms" "$total" "$(echo "$srv_line" | awk '{print $4}')" "$call_ms"
        return 1
    fi
    record "$clones" PASSED - "$gen_ms" "$total" "$(echo "$srv_line" | awk '{print $4}')" "$call_ms"
    return 0
}

record() { printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$@" >> "$RESULTS"; }

# ---------------------------------------------------------------------------------------------
# The doubling loop.
# ---------------------------------------------------------------------------------------------
CLONES="$START_CLONES"
LAST_GOOD=0
BROKE_AT=""
while [ "$CLONES" -le "$MAX_CLONES" ]; do
    if run_pass "$CLONES"; then
        LAST_GOOD="$CLONES"
    else
        BROKE_AT="$CLONES"
        break
    fi
    KEEP_CTR=""
    docker rm -f "$CTR" >/dev/null 2>&1
    docker volume rm "$VOL" >/dev/null 2>&1
    CLONES=$((CLONES * 2))
done
[ "$KEEP" = "1" ] && KEEP_CTR="$CTR"

echo
echo "========================================================================================"
echo " RESULTS"
echo "========================================================================================"
printf '%10s  %-7s  %-20s %8s %8s %9s %10s\n' CLONES RESULT PHASE BUILD_S TOOLS SRV_RSS_MB CALLS_MS
awk -F'\t' '{printf "%10s  %-7s  %-20s %8s %8s %9s %10s\n", $1,$2,$3,$4,$5,$6,$7}' "$RESULTS"
echo
if [ -n "$BROKE_AT" ]; then
    echo "Broke at $BROKE_AT cloned tools; last good pass was $LAST_GOOD."
    echo "The limit is between $LAST_GOOD and $BROKE_AT -- narrow it with --start."
    exit 1
fi
echo "Reached the --max of $MAX_CLONES without breaking (last good: $LAST_GOOD)."
exit 0
