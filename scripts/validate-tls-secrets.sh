#!/usr/bin/env bash
set -euo pipefail

CERT="${TLS_CERT:-docker/dev-tls/tls.crt}"
KEY="${TLS_KEY:-docker/dev-tls/tls.key}"

openssl x509 -in "$CERT" -noout >/dev/null
openssl pkey -in "$KEY" -noout >/dev/null

cert_pub="$(openssl x509 -in "$CERT" -pubkey -noout | openssl pkey -pubin -outform DER 2>/dev/null | sha256sum | cut -d' ' -f1)"
key_pub="$(openssl pkey -in "$KEY" -pubout -outform DER 2>/dev/null | sha256sum | cut -d' ' -f1)"

if [[ "$cert_pub" != "$key_pub" ]]; then
  echo "Certificate and private key do not match" >&2
  exit 1
fi

echo "TLS certificate and private key are valid and matched."
