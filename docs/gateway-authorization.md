# Gateway authorization

The perimeter filter verifies JWT tokens using current and previous secrets.

## Optional rate limiting
- Local/in-memory: `app.gateway.rate-limit.requests-per-minute`
- Shared mode: set `GATEWAY_RATE_LIMIT_MODE=redis` and configure `spring.data.redis.*`.
- Use a Redis-backed or service-mesh limiter when running more than one gateway replica.

## Optional fine-grained authorization
Use `RoleAuthorization=ROLE_A,ROLE_B` on guarded routes. Required roles are checked against `roles` or `scope` claims.
