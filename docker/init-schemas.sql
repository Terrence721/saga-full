-- Runs once, automatically, on the postgres-db container's first startup
-- (docker-entrypoint-initdb.d convention). One schema per service, matching
-- each module's application.yaml postgres profile currentSchema setting.
CREATE SCHEMA IF NOT EXISTS user_service_schema;
CREATE SCHEMA IF NOT EXISTS order_service_schema;
CREATE SCHEMA IF NOT EXISTS payment_service_schema;
