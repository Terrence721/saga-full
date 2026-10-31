#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${EXTERNAL_SECRETS_NAMESPACE:-external-secrets}"
RELEASE="${EXTERNAL_SECRETS_RELEASE:-external-secrets}"
CHART_REPO="${EXTERNAL_SECRETS_REPO:-https://charts.external-secrets.io}"
CHART_VERSION="${EXTERNAL_SECRETS_VERSION:-}"

echo "Installing External Secrets Operator into namespace '$NAMESPACE'..."

helm repo add external-secrets "$CHART_REPO" --force-update
helm repo update

helm_args=(upgrade --install "$RELEASE" external-secrets/external-secrets \
  --namespace "$NAMESPACE" --create-namespace \
  --set installCRDs=true)

if [[ -n "$CHART_VERSION" ]]; then
  helm_args+=(--version "$CHART_VERSION")
fi

helm "${helm_args[@]}"

echo "External Secrets Operator installed."
echo "Next: apply manifests/external-secrets/*.yaml after Vault is configured."
