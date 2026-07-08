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

CREATE TABLE IF NOT EXISTS catalog_views (
    catalog_id UUID PRIMARY KEY,
    item_name VARCHAR(255) NOT NULL,
    price VARCHAR(50) NOT NULL,
    description TEXT,
    category VARCHAR(255),
    availability VARCHAR(255),
    owner VARCHAR(255),
    updated_at TIMESTAMP NOT NULL
);