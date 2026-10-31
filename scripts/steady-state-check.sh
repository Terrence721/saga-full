#!/usr/bin/env bash
set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8090}"
HEALTH_URL="$GATEWAY_URL/actuator/health"

echo "Checking gateway health at $HEALTH_URL..."
for i in $(seq 1 10); do
  curl --fail --silent --show-error "$HEALTH_URL" >/dev/null
  sleep 1
done
echo "Gateway steady-state health check passed."
