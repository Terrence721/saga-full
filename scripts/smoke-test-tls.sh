#!/usr/bin/env bash
set -euo pipefail

URL="${TLS_URL:-https://localhost:8080/actuator/health}"
CERT="${TLS_CA_CERT:-docker/dev-tls/tls.crt}"
KEY="${TLS_KEY:-docker/dev-tls/tls.key}"

echo "Running TLS smoke test against $URL"
curl --fail --show-error --silent --cacert "$CERT" "$URL" >/dev/null
echo "TLS smoke test passed"
