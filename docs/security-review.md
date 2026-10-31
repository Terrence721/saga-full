# Security review

## Scope
This review covers dependency hygiene, container image build expectations, and API gateway authorization.

## Findings and required improvements

### Dependencies
- Keep Spring Boot 3.5.x and gRPC/protobuf versions aligned; avoid mixing incompatible protobuf-java versions.
- Run OWASP Dependency-Check or OSV scanning in CI before releases.
- Review `spring-security-crypto`, auth0, and Kafka client transitive vulnerabilities quarterly.
- Pin and verify all GitHub Action versions (already pinned mostly at v4/v5).

### Image builds
- Use non-root runtime users in Dockerfiles.
- Build on minimal/distroless base images and scan images with Trivy or Grype.
- Sign images with cosign and verify signatures in deployment policy.
- Do not store secrets in image layers or build args.
- Use read-only root filesystem where supported.

### Gateway authorization
- JWT verification now supports graceful rotation via previous secrets.
- Expired tokens map to 403; malformed/invalid tokens map to 401.
- Authorization is perimeter-based only; add fine-grained role/permission enforcement if business APIs require it.
- Rate limiting, request size limits, CORS restrictions, and security headers should be configured before production.

### Kubernetes runtime
- Network policies are present, but production should add mTLS or service mesh authorization.
- Secrets must come from Vault/external secret stores (not committed placeholders).
- Container images should be pulled from a trusted registry with image pull secrets.

## Recommended next actions
1. Add dependency and container vulnerability scans to CI.
2. Add gateway rate limiting and security headers.
3. Define RBAC/authorization claims for protected routes.
4. Require signed images and enforce admission policy.
