# Vault + External Secrets setup (production)

Before deploying the ExternalSecret, configure Vault and the Kubernetes auth integration.

## 1. Store the secrets in Vault

```bash
vault kv put secret/saga/ecosystem \
  jwt_secret="$(openssl rand -base64 48)" \
  database_user="postgres" \
  database_password="$(openssl rand -base64 32)"
```

## 2. Enable Kubernetes auth in Vault

```bash
vault auth enable kubernetes
vault write auth/kubernetes/config \
  kubernetes_host="https://kubernetes.default.svc.cluster.local" \
  kubernetes_ca_cert=@/var/run/secrets/kubernetes.io/serviceaccount/ca.crt
```

## 3. Create the Vault policy

```bash
vault policy write saga-ecosystem - <<'HCL'
path "secret/data/saga/ecosystem" {
  capabilities = ["read"]
}
HCL
```

## 4. Bind the Kubernetes service account to the Vault policy

```bash
vault write auth/kubernetes/role/saga-ecosystem \
  bound_service_account_names="saga-ecosystem" \
  bound_service_account_namespaces="saga-ecosystem" \
  policies="saga-ecosystem" \
  ttl="1h"
```

## 5. Deploy the ESO resources

```bash
kubectl apply -f manifests/external-secrets/service-account.yaml
kubectl apply -f manifests/external-secrets/vault-secret-store.yaml
kubectl apply -f manifests/external-secrets/external-secret-perimeter.yaml
```

## 6. Network and security notes

- Vault should be reachable only from the `external-secrets` namespace/controllers (or from a private network), not from application pods.
- Restrict direct access to `perimeter-security-spec` via Kubernetes RBAC; only ESO and cluster operators should read it.
- Rotate `jwt_secret` using Vault versioning; External Secrets Operator refreshes the Kubernetes Secret every hour by default.
- Use TLS for Vault, enforce audit logging, and disable root token access in production.
