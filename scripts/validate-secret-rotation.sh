#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${SAGA_NAMESPACE:-saga-ecosystem}"
SECRET_NAME="${SAGA_SECRET_NAME:-perimeter-security-spec}"
EXPECTED_KEYS="${EXPECTED_KEYS:-JWT_SECRET DATABASE_USER DATABASE_PASSWORD}"

echo "Validating ExternalSecret '$SECRET_NAME' in namespace '$NAMESPACE'..."

kubectl -n "$NAMESPACE" get externalsecret "$SECRET_NAME" >/dev/null
kubectl -n "$NAMESPACE" get secret "$SECRET_NAME" >/dev/null

for key in $EXPECTED_KEYS; do
  value="$(kubectl -n "$NAMESPACE" get secret "$SECRET_NAME" -o jsonpath="{.data.$key}" 2>/dev/null || true)"
  if [[ -z "$value" ]]; then
    echo "ERROR: secret key '$key' is missing or empty" >&2
    exit 1
  fi
  echo "OK: $key present"
done

echo "Rotation hint: update the value in Vault, wait for ExternalSecret refreshInterval, and rerun this validation."
