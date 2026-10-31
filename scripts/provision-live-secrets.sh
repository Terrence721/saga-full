#!/usr/bin/env bash
set -euo pipefail

VAULT_ADDR="${VAULT_ADDR:-http://127.0.0.1:8200}"
VAULT_TOKEN="${VAULT_TOKEN:?VAULT_TOKEN must be provided}"
VAULT_KV_PATH="${VAULT_KV_PATH:-secret/data/saga/ecosystem}"

export VAULT_ADDR VAULT_TOKEN

echo "Writing production secrets into Vault..."
vault kv put "$VAULT_KV_PATH" \
  jwt_secret="${JWT_SECRET:?JWT_SECRET must be provided}" \
  jwt_previous_secrets="${JWT_PREVIOUS_SECRETS:-}" \
  user_service_database_user="${USER_SERVICE_DATABASE_USER:?}" \
  user_service_database_password="${USER_SERVICE_DATABASE_PASSWORD:?}" \
  order_service_database_user="${ORDER_SERVICE_DATABASE_USER:?}" \
  order_service_database_password="${ORDER_SERVICE_DATABASE_PASSWORD:?}" \
  payment_service_database_user="${PAYMENT_SERVICE_DATABASE_USER:?}" \
  payment_service_database_password="${PAYMENT_SERVICE_DATABASE_PASSWORD:?}" \
  reservation_service_database_user="${RESERVATION_SERVICE_DATABASE_USER:?}" \
  reservation_service_database_password="${RESERVATION_SERVICE_DATABASE_PASSWORD:?}" \
  restaurant_service_database_user="${RESTAURANT_SERVICE_DATABASE_USER:?}" \
  restaurant_service_database_password="${RESTAURANT_SERVICE_DATABASE_PASSWORD:?}"

echo "Secrets stored. Run scripts/validate-secret-rotation.sh after applying ExternalSecrets."
