#!/usr/bin/env bash
set -euo pipefail

NS="${SAGA_NAMESPACE:-saga-ecosystem}"
SECRET_NAME="${SAGA_SECRET_NAME:-perimeter-security-spec}"

services=(user order payment reservation restaurant)
for svc in "${services[@]}"; do
  key="$(echo "$svc" | tr '[:lower:]' '[:upper:]')_SERVICE_DATABASE_USER"
  value="$(kubectl -n "$NS" get secret "$SECRET_NAME" -o jsonpath="{.data.$key}" 2>/dev/null || true)"
  if [[ -z "$value" ]]; then
    echo "ERROR: missing secret key $key" >&2
    exit 1
  fi
  echo "OK: $key present"
done
echo "Per-service database credentials are present."
