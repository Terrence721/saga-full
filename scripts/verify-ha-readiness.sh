#!/usr/bin/env bash
set -euo pipefail

NS="${SAGA_NAMESPACE:-saga-ecosystem}"

echo "Checking workload readiness in namespace '$NS'..."
kubectl -n "$NS" get deployments
kubectl -n "$NS" get hpa
kubectl -n "$NS" get pdb

echo
echo "Manual tests still required (see docs/ha-failover.md): rolling updates, node drains, database failover, Kafka broker loss, and backup restore."
