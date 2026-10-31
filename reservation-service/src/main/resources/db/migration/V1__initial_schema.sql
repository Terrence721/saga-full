CREATE TABLE IF NOT EXISTS reservations (
    id UUID PRIMARY KEY,
    order_id UUID,
    customer_id VARCHAR(255) NOT NULL,
    table_number VARCHAR(255) NOT NULL,
    party_size INTEGER NOT NULL,
    start_time TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS outbox_record (
    id UUID PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    created_time TIMESTAMP NOT NULL
);
