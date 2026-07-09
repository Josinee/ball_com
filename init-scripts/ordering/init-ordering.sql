CREATE TABLE IF NOT EXISTS event_store (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    sequence_number INT NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    payload JSONB NOT NULL,
    UNIQUE (aggregate_id, sequence_number)
);

CREATE TABLE IF NOT EXISTS processed_events (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS order_views (
    order_id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    total_amount NUMERIC(10, 2) NOT NULL,
    order_status VARCHAR(50) NOT NULL,
    payment_status VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- voor de custoemrs en catalogs

CREATE TABLE IF NOT EXISTS ordering_customers (
    customer_id UUID PRIMARY KEY,
    company_name VARCHAR(255),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    updated_at TIMESTAMP NOT NULL
    
);

CREATE TABLE IF NOT EXISTS ordering_catalog_items (
    product_id UUID PRIMARY KEY,
    item_name VARCHAR(255) NOT NULL,
    price NUMERIC(10, 2) NOT NULL,
    category VARCHAR(255),
    availability VARCHAR(255) NOT NULL,
    owner VARCHAR(255),
    updated_at TIMESTAMP NOT NULL
);