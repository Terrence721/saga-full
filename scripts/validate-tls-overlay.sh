#!/usr/bin/env bash
set -euo pipefail

missing=0
for f in manifests/apps/*.yaml; do
  if ! grep -q 'TLS_ENABLED' "$f" || ! grep -q 'value: "true"' "$f"; then
    echo "TLS_ENABLED not enabled in $f"
    missing=1
  fi
  if ! grep -q 'saga-service-tls' "$f"; then
    echo "TLS certificate mount not present in $f"
    missing=1
  fi
done

if [[ "$missing" -ne 0 ]]; then
  exit 1
fi
echo "TLS production overlays are configured."
