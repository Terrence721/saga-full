# Optional application-level mTLS scaffold

This directory is intentionally disabled by default. Enable only after the
cluster issuer and cert-manager are configured.

## Expected secret shape
- `saga-service-tls` (type: kubernetes.io/tls) containing `tls.crt` and `tls.key`
- A mounted trust bundle for cross-service CA verification

## Rollout notes
- Set `TLS_ENABLED=true` for HTTP services and mount the cert Secret.
- Configure gRPC TLS per the spring-grpc version in use.
- Update downstream WebClient/gRPC clients to present certificates.
- Keep peer traffic plaintext only behind strict network policies until TLS is verified.
