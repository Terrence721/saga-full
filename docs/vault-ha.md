# Production Vault (HA) deployment

`manifests/vault/vault-ha-statefulset.yaml` is a production-oriented scaffold, not a one-click install.

## Operator responsibilities
1. Provision persistent volumes and backups.
2. Create and mount a real TLS secret named `vault-tls`.
3. Initialize the cluster and securely store unseal keys.
4. Prefer auto-unseal (cloud KMS) over manual unsealing.
5. Apply the Vault policy and Kubernetes auth configuration in `vault-bootstrap.sh`.
6. Enable audit logging and monitoring.
7. Create regular Raft snapshots and test restore.
8. Replace dev-mode root tokens with least-privilege auth.

## Validation
- `vault status` shows initialized, sealed false, and HA enabled.
- External Secrets Operator can read `secret/data/saga/ecosystem`.
- `scripts/validate-secret-rotation.sh` succeeds.
