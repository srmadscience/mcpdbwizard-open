#!/bin/bash
#
# mcp-load.sh -- drive a generated MCP server's tools and check the answers, not just the rate.
#
#   ./mcp-load.sh --list                                       what does this server publish?
#   ./mcp-load.sh --tools job_id_nextval --for 60s --threads 8
#   ./mcp-load.sh --workload work.json --for 5m --rate 200 --warmup 30s
#   ./mcp-load.sh --proxy dr --workload work.json --calls 5000   ...through the web proxy
#
# Everything after the wrapper's own flags is passed straight to
# com.mcpdbwizard.loadtest.McpLoad; run `./mcp-load.sh --help` for the full list. What this
# script adds is the CONTAINER: it finds a server to talk to, points the tool at it, and reads
# the server's own /metrics before and after so there is a second, independent measurement.
#
# WRAPPER FLAGS
#   --proxy <config>   aim at the web app's /mcp/<config> instead of the generated server's own
#                      port. Needs a user API token in MCPDBWIZARD_API_TOKEN (or pass --token).
#   --config <name>    which config to start if nothing is listening (default: dr)
#
# THE CLIENT IS NOT COMPILED HERE ANY MORE. It lives in mcpdbwizard-app.jar, which the image
# already carries at /app/lib -- so this no longer `docker cp`s a source file and runs javac on
# every invocation, and the client cannot drift from the MCP SDK the server is running on,
# because it is loading the same jars.
#
# PREFER A DURATION FOR ANYTHING LONG. Throughput climbs steeply for the first minute or so while
# the JVM JITs, the DAO pool grows and Oracle caches cursors, so a call count sized from a short
# sample under-shoots badly: 290,000 calls derived from a 5-second measurement of 958/sec ran at
# 2,403/sec and finished in two minutes. A five-second sample is measuring warm-up, not steady
# state -- which is also what `--warmup 30s` is for.
#
# WHAT IT CHECKS BESIDES SPEED. A workload entry carrying "check": "unique" is verified for
# uniqueness and contiguity: every call must return a different value, and the values must span
# exactly as many numbers as there were calls. That is what proves each call reached Oracle rather
# than a cache, a retry or a shared result -- a throughput figure alone cannot tell a fast server
# from a broken one.
#
# WHERE IT RUNS. Inside the mcpdbwizard-web container, because a generated server binds loopback
# there and that is the only place its MCP port is reachable.
#
# It uses a server that is already listening (one started from the Runtime page, say) and leaves it
# alone. If nothing is listening it starts one from the jar the Runtime already built, and says so.
#
# Env: CONTAINER, MCP_PORT (8090), METRICS_PORT (9464), WEB_PORT (8080), MCPDBWIZARD_API_TOKEN.
#
# Copyright 2003-2026 ATB Consultancy Services Ltd
# (formerly Orinda Software Ltd, Dublin, Ireland)
set -euo pipefail

CONTAINER="${CONTAINER:-mcpdbwizard-app-mcpdbwizard-web-1}"
MCP_PORT="${MCP_PORT:-8090}"
METRICS_PORT="${METRICS_PORT:-9464}"
WEB_PORT="${WEB_PORT:-8080}"
CONFIG=dr
PROXY_CONFIG=
ARGS=()

while [ $# -gt 0 ]; do
    case "$1" in
        --proxy)  PROXY_CONFIG="$2"; shift 2 ;;
        --config) CONFIG="$2"; shift 2 ;;
        *)        ARGS+=("$1"); shift ;;
    esac
done

if [ ${#ARGS[@]} -eq 0 ]; then
    ARGS=(--help)
fi

docker exec "$CONTAINER" true 2>/dev/null || { echo "container $CONTAINER is not running"; exit 1; }

# --- the server -------------------------------------------------------------------------------
# The proxy route needs the web app (always up if the container is) AND a server running behind it,
# which the Runtime page starts. The direct route can start one itself.
STARTED_BY_US=no
if [ -n "$PROXY_CONFIG" ]; then
    URL="http://127.0.0.1:$WEB_PORT/mcp/$PROXY_CONFIG"
    # Only when the caller did not name one: an explicit --token must beat the environment, or a
    # deliberate "try this other account's token" silently tests the same account as last time.
    if [ -n "${MCPDBWIZARD_API_TOKEN:-}" ] && [[ " ${ARGS[*]} " != *" --token "* ]]; then
        ARGS+=(--token "$MCPDBWIZARD_API_TOKEN")
    fi
    echo "aiming at the web proxy: $URL"
    echo "  (the proxy requires the config to be RUNNING -- start it from the Runtime page)"
else
    URL="http://127.0.0.1:$MCP_PORT/mcp"
    if docker exec "$CONTAINER" sh -c "ps -ef | grep -q '[D]aoFactoryMcpServer'"; then
        echo "using the MCP server already running (started from the Runtime page, or by an earlier run)"
    else
        echo "no MCP server running; starting $CONFIG from its jar on port $MCP_PORT"
        docker exec -d "$CONTAINER" sh -c "cd /data/runtime/$CONFIG \
            && MCP_METRICS_PORT=$METRICS_PORT java -Djava.awt.headless=true \
                 -cp '/data/runtime/$CONFIG/$CONFIG-mcp.jar:/app/lib/*:/app/conf' \
                 com.mcpdbwizard.customer.DaoFactoryMcpServer http $MCP_PORT > /tmp/srv.log 2>&1"
        STARTED_BY_US=yes
        for _ in $(seq 1 60); do
            docker exec "$CONTAINER" sh -c "grep -q 'Logged in' /tmp/srv.log 2>/dev/null" && break
            sleep 2
        done
        docker exec "$CONTAINER" sh -c 'grep -vE "Picked up|^$" /tmp/srv.log | head -4' || true
    fi
fi

# A workload file lives on THIS machine; ship it in so the container can read it.
for i in "${!ARGS[@]}"; do
    if [ "${ARGS[$i]}" = "--workload" ] && [ -f "${ARGS[$((i+1))]:-}" ]; then
        docker cp "${ARGS[$((i+1))]}" "$CONTAINER:/tmp/workload.json" >/dev/null
        ARGS[$((i+1))]=/tmp/workload.json
    fi
done

# --- before -----------------------------------------------------------------------------------
echo
echo "=== /metrics before ==="
curl -sS "http://localhost:$METRICS_PORT/metrics" 2>/dev/null | grep "calls_total{" \
    || echo "(no metrics yet, or PROMETHEUS_SERVER/MCP_METRICS_PORT not set for this config)"

# --- run --------------------------------------------------------------------------------------
# Without a logback config on the classpath, logback defaults to DEBUG on the console and buries
# the results under one frame-level line per call. /tmp inside the container is reset whenever
# compose RECREATES it, so this is written every run rather than assumed to have survived.
docker exec "$CONTAINER" sh -c 'cat > /tmp/quiet-logback.xml <<EOF
<configuration>
  <appender name="STDERR" class="ch.qos.logback.core.ConsoleAppender">
    <target>System.err</target><encoder><pattern>%-5level %logger{20} - %msg%n</pattern></encoder>
  </appender>
  <root level="WARN"><appender-ref ref="STDERR"/></root>
</configuration>
EOF'

echo
set +e
docker exec "$CONTAINER" java -Dlogback.configurationFile=/tmp/quiet-logback.xml \
    -cp '/app/lib/*' com.mcpdbwizard.loadtest.McpLoad --url "$URL" "${ARGS[@]}" \
    2>&1 | grep -v "Picked up JAVA_TOOL_OPTIONS"
RC=${PIPESTATUS[0]}
set -e

# --- after ------------------------------------------------------------------------------------
echo
echo "=== /metrics after ==="
echo "(the server's own count. It should agree with the tool's; two independent measurements"
echo " disagreeing is worth more than either one on its own.)"
curl -sS "http://localhost:$METRICS_PORT/metrics" 2>/dev/null \
    | grep -E "calls_total\{|duration_seconds_(count|sum|max)\{|_pool_" \
    || echo "(metrics not reachable on localhost:$METRICS_PORT)"

if [ "$STARTED_BY_US" = yes ]; then
    echo
    echo "NOTE: this script started the MCP server and is leaving it running on port $MCP_PORT."
    echo "      Stop it before using the Runtime page for '$CONFIG', or the page's own child"
    echo "      cannot bind that port:"
    echo "        docker exec $CONTAINER pkill -f DaoFactoryMcpServer"
fi

exit "$RC"
