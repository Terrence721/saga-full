#!/usr/bin/env bash
set -euo pipefail

NS="${SAGA_NAMESPACE:-saga-ecosystem}"

echo "Creating namespace and secrets integration prerequisites..."
kubectl apply -f manifests/namespace-setup.yaml
kubectl apply -f manifests/external-secrets/service-account.yaml
./scripts/install-external-secrets.sh
./scripts/bootstrap-vault.sh

echo "Applying Vault HA, network policies, and availability resources..."
kubectl apply -f manifests/vault/vault-ha-statefulset.yaml
kubectl apply -f manifests/network-policies/
kubectl apply -f manifests/high-availability/

echo "Applying application Deployments and cert-manager resources..."
kubectl apply -f manifests/mtls/cert-manager-certificate.yaml
kubectl apply -f manifests/apps/

echo "Validating readiness..."
./scripts/verify-ha-readiness.sh
./scripts/validate-secret-rotation.sh

echo "Staging provision script completed. Inspect Vault init/unseal and managed DB/Kafka endpoints before production use."
