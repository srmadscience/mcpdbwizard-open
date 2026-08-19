#!/bin/bash
#
# mcp-load.sh -- drive a generated MCP server's tools and check the answers, not just the rate.
#
#   ./mcp-load.sh <calls|Ns> [threads] [tool[,tool...]] [config]
#
#   ./mcp-load.sh 5000                          5,000 calls of job_id_nextval, 1 thread
#   ./mcp-load.sh 300s 8                        five minutes, 8 threads
#   ./mcp-load.sh 300s 8 job_id_nextval,greet   five minutes, alternating two tools
#
# PREFER A DURATION FOR ANYTHING LONG. Throughput climbs steeply for the first minute or so while
# the JVM JITs, the DAO pool grows and Oracle caches cursors, so a call count sized from a short
# sample under-shoots badly: 290,000 calls derived from a 5-second measurement of 958/sec ran at
# 2,403/sec and finished in two minutes. A five-second sample is measuring warm-up, not steady state.
#
# WHAT IT CHECKS BESIDES SPEED. A *_nextval tool is verified for uniqueness and contiguity: every
# call must return a different value, and the values must span exactly as many numbers as there were
# calls. That is what proves each call reached Oracle rather than a cache, a retry or a shared
# result -- a throughput figure alone cannot tell a fast server from a broken one.
#
# WHERE IT RUNS. Inside the mcpdbwizard-web container, because a generated server binds loopback
# there and that is the only place its MCP port is reachable. The client is compiled against the
# container's own /app/lib -- the same jars the server runs on -- so the MCP SDK versions cannot
# drift apart.
#
# It uses a server that is already listening (one started from the Runtime page, say) and leaves it
# alone. If nothing is listening it starts one from the jar the Runtime already built, and says so.
#
# Env: CONTAINER, MCP_PORT (8090), METRICS_PORT (9464).
#
# Copyright 2003-2026 ATB Consultancy Services Ltd
# (formerly Orinda Software Ltd, Dublin, Ireland)
set -euo pipefail

CALLS="${1:-1000}"
THREADS="${2:-1}"
TOOL="${3:-job_id_nextval}"
CONFIG="${4:-dr}"

CONTAINER="${CONTAINER:-mcpdbwizard-app-mcpdbwizard-web-1}"
MCP_PORT="${MCP_PORT:-8090}"
METRICS_PORT="${METRICS_PORT:-9464}"
HERE="$(cd "$(dirname "$0")" && pwd)"

docker exec "$CONTAINER" true 2>/dev/null || { echo "container $CONTAINER is not running"; exit 1; }

# --- the client -------------------------------------------------------------------------------
# /tmp inside the container is reset whenever compose RECREATES it (an image rebuild, an
# environment change), so this re-ships every run rather than assuming last time's copy survived.
docker cp "$HERE/McpLoad.java" "$CONTAINER:/tmp/McpLoad.java" >/dev/null

# Without a logback config on the classpath, logback defaults to DEBUG on the console and buries the
# results under one frame-level line per call.
docker exec "$CONTAINER" sh -c 'cat > /tmp/quiet-logback.xml <<EOF
<configuration>
  <appender name="STDERR" class="ch.qos.logback.core.ConsoleAppender">
    <target>System.err</target><encoder><pattern>%-5level %logger{20} - %msg%n</pattern></encoder>
  </appender>
  <root level="WARN"><appender-ref ref="STDERR"/></root>
</configuration>
EOF'
docker exec "$CONTAINER" sh -c 'cd /tmp && javac -cp "/app/lib/*" McpLoad.java 2>&1 | grep -v "^Note:" || true'

# --- the server -------------------------------------------------------------------------------
STARTED_BY_US=no
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

# --- before -----------------------------------------------------------------------------------
echo
echo "=== /metrics before ==="
curl -sS "http://localhost:$METRICS_PORT/metrics" 2>/dev/null | grep "calls_total{" \
    || echo "(no metrics yet, or PROMETHEUS_SERVER/MCP_METRICS_PORT not set for this config)"

# --- run --------------------------------------------------------------------------------------
echo
docker exec "$CONTAINER" sh -c "cd /tmp && java -Dlogback.configurationFile=/tmp/quiet-logback.xml \
    -cp '/app/lib/*:/tmp' McpLoad http://127.0.0.1:$MCP_PORT $CALLS $TOOL $THREADS" \
    2>&1 | grep -v "Picked up JAVA_TOOL_OPTIONS"

# --- after ------------------------------------------------------------------------------------
echo
echo "=== /metrics after ==="
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
