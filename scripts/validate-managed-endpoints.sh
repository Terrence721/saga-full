#!/usr/bin/env bash
set -euo pipefail

: "${DATABASE_HOST:?DATABASE_HOST must be set}"
: "${DATABASE_PORT:?DATABASE_PORT must be set}"
: "${KAFKA_BOOTSTRAP_SERVERS:?KAFKA_BOOTSTRAP_SERVERS must be set}"

echo "Checking database endpoint $DATABASE_HOST:$DATABASE_PORT..."
timeout 5 bash -c "until (echo > /dev/tcp/$DATABASE_HOST/$DATABASE_PORT) 2>/dev/null; do sleep 1; done" || {
  echo "Database endpoint is not reachable" >&2
  exit 1
}

IFS=',' read -ra brokers <<< "$KAFKA_BOOTSTRAP_SERVERS"
for broker in "${brokers[@]}"; do
  host="${broker%%:*}"
  port="${broker##*:}"
  echo "Checking Kafka broker $host:$port..."
  timeout 5 bash -c "until (echo > /dev/tcp/$host/$port) 2>/dev/null; do sleep 1; done" || {
    echo "Kafka broker $host:$port is not reachable" >&2
    exit 1
  }
done

echo "Managed data and messaging endpoints are reachable."
