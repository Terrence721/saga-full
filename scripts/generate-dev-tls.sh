#!/usr/bin/env bash
set -euo pipefail

OUT_DIR="${1:-docker/dev-tls}"
mkdir -p "$OUT_DIR"

openssl req -x509 -newkey rsa:2048 -sha256 -days 30 -nodes \
  -keyout "$OUT_DIR/tls.key" \
  -out "$OUT_DIR/tls.crt" \
  $'{SUBJ}' \
  -addext "subjectAltName=DNS:localhost,DNS:*.saga-ecosystem.svc.cluster.local,IP:127.0.0.1"

chmod 600 "$OUT_DIR/tls.key"
chmod 644 "$OUT_DIR/tls.crt"

echo "Generated development TLS bundle in $OUT_DIR"
echo "Mount tls.crt and tls.key, then set TLS_ENABLED=true"
