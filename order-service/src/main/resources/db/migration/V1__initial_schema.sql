CREATE TABLE IF NOT EXISTS customer_orders (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    total_amount DECIMAL(19,2) NOT NULL,
    item_code VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    table_number VARCHAR(255),
    customer_name VARCHAR(255),
    payment_method VARCHAR(50) NOT NULL,
    card_brand VARCHAR(255),
    card_last4 VARCHAR(255),
    status VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS outbox_record (
    id UUID PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    created_time TIMESTAMP NOT NULL
);
