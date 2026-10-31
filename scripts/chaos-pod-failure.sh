#!/usr/bin/env bash
set -euo pipefail

NS="${SAGA_NAMESPACE:-saga-ecosystem}"
APP="${APP:-order-service}"

echo "Deleting one pod for $APP..."
POD="$(kubectl -n "$NS" get pod -l app="$APP" -o jsonpath='{.items[0].metadata.name}')"
kubectl -n "$NS" delete pod "$POD"

echo "Waiting for recovery..."
kubectl -n "$NS" rollout status deployment/"$APP" --timeout=120s

echo "Checking ready pods:"
kubectl -n "$NS" get deployment "$APP"
