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

CREATE TABLE IF NOT EXISTS shipment_views (
    shipment_id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    carrier VARCHAR(25),
    shipping_price INT,
    status VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS order_shipment_mapping (
    order_id UUID PRIMARY KEY,
    shipment_id UUID NOT NULL
);