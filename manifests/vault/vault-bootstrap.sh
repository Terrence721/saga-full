#!/usr/bin/env bash
set -euo pipefail

VAULT_POD="${VAULT_POD:-vault-0}"
VAULT_NAMESPACE="${VAULT_NAMESPACE:-vault}"

echo "Configuring Vault Kubernetes auth and saga policy in pod '$VAULT_POD'..."

kubectl -n "$VAULT_NAMESPACE" exec "$VAULT_POD" -- sh -c '
set -e
vault secrets enable -path=secret kv-v2 || true
vault policy write saga-ecosystem - <<HCL
path "secret/data/saga/ecosystem" {
  capabilities = ["read"]
}
HCL
vault auth enable kubernetes || true
vault write auth/kubernetes/config \
  kubernetes_host="https://kubernetes.default.svc.cluster.local" \
  kubernetes_ca_cert=@/var/run/secrets/kubernetes.io/serviceaccount/ca.crt
vault write auth/kubernetes/role/saga-ecosystem \
  bound_service_account_names="saga-ecosystem" \
  bound_service_account_namespaces="saga-ecosystem" \
  policies="saga-ecosystem" \
  ttl="1h"
'
echo "Vault bootstrap complete."
