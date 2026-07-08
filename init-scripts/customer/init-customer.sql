CREATE TABLE IF NOT EXISTS customer_views (
    customer_id UUID PRIMARY KEY,
    company_name VARCHAR(255),
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(50),
    street VARCHAR(255) NOT NULL,
    house_number VARCHAR(50) NOT NULL,
    city VARCHAR(255) NOT NULL,
    zip_code VARCHAR(20) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);