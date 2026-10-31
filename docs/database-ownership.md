# Per-service database ownership

Production should not use the shared `postgres` superuser for application runtime.

## Required changes
- Create one database role per service using `manifests/database/per-service-roles.sql`.
- Store each role/password in the secret store.
- Update each Deployment environment to point to its own `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`.
- Ensure Flyway runs as a privileged migration role, while runtime pods use least-privilege roles.
- Keep each service limited to its own schema.
