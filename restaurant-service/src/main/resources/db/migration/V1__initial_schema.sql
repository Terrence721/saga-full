CREATE TABLE IF NOT EXISTS restaurant_tickets (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS restaurant_inventory (
    item_code VARCHAR(50) PRIMARY KEY,
    stock_count INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS menu_items (
    item_code VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    available BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS outbox_record (
    id UUID PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    created_time TIMESTAMP NOT NULL
);
