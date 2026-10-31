#!/usr/bin/env bash
set -euo pipefail

NS="${SAGA_NAMESPACE:-saga-ecosystem}"
export SAGA_NAMESPACE="$NS"

echo "Running local pre-deployment code checks..."
./gradlew test --offline

echo "Running staging configuration checks..."
./scripts/validate-managed-endpoints.sh
./scripts/validate-database-ownership.sh
./scripts/validate-tls-overlay.sh
./scripts/validate-tls-secrets.sh
./scripts/validate-secret-rotation.sh
./scripts/verify-ha-readiness.sh
./scripts/steady-state-check.sh

echo "Production-readiness gate passed."
