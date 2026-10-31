# Apply this policy inside Vault:
# vault policy write saga-ecosystem - <<'HCL'
path "secret/data/saga/ecosystem" {
  capabilities = ["read"]
}
HCL
