# Development Vault deployment

`manifests/vault/vault-dev-statefulset.yaml` provides a single-node Vault dev server for bootstrap testing only.

Do not use dev mode or the sample root token in production. Use a highly available Vault cluster and production authentication for real deployments.
