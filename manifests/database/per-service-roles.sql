-- Create least-privilege database users for each saga service.
-- Run as a DBA before deploying or during a migration window.
-- Replace the example passwords with values from the secret store.

CREATE USER user_service WITH PASSWORD 'REPLACE_WITH_SECURE_VALUE';
CREATE USER order_service WITH PASSWORD 'REPLACE_WITH_SECURE_VALUE';
CREATE USER payment_service WITH PASSWORD 'REPLACE_WITH_SECURE_VALUE';
CREATE USER reservation_service WITH PASSWORD 'REPLACE_WITH_SECURE_VALUE';
CREATE USER restaurant_service WITH PASSWORD 'REPLACE_WITH_SECURE_VALUE';

CREATE SCHEMA IF NOT EXISTS user_service_schema;
CREATE SCHEMA IF NOT EXISTS order_service_schema;
CREATE SCHEMA IF NOT EXISTS payment_service_schema;
CREATE SCHEMA IF NOT EXISTS reservation_service_schema;
CREATE SCHEMA IF NOT EXISTS restaurant_service_schema;

GRANT USAGE, CREATE ON SCHEMA user_service_schema TO user_service;
GRANT USAGE, CREATE ON SCHEMA order_service_schema TO order_service;
GRANT USAGE, CREATE ON SCHEMA payment_service_schema TO payment_service;
GRANT USAGE, CREATE ON SCHEMA reservation_service_schema TO reservation_service;
GRANT USAGE, CREATE ON SCHEMA restaurant_service_schema TO restaurant_service;

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA user_service_schema TO user_service;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA order_service_schema TO order_service;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA payment_service_schema TO payment_service;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA reservation_service_schema TO reservation_service;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA restaurant_service_schema TO restaurant_service;

ALTER DEFAULT PRIVILEGES IN SCHEMA user_service_schema GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO user_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA order_service_schema GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO order_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA payment_service_schema GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO payment_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA reservation_service_schema GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO reservation_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA restaurant_service_schema GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO restaurant_service;
