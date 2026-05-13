-- V5: Add Currency and Exchange Rate Tables

-- Create Currencies Table
CREATE TABLE currencies (
   id UUID PRIMARY KEY,
   version BIGINT,
   created_at TIMESTAMP WITH TIME ZONE NOT NULL,
   updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
   deleted_at TIMESTAMP WITH TIME ZONE,
   code VARCHAR(3) NOT NULL UNIQUE,
   name VARCHAR(50) NOT NULL,
   symbol VARCHAR(10) NOT NULL,
   is_active BOOLEAN NOT NULL DEFAULT TRUE,
   is_base BOOLEAN NOT NULL DEFAULT FALSE
);

-- Create Exchange Rates Table
CREATE TABLE exchange_rates (
   id UUID PRIMARY KEY,
   version BIGINT,
   created_at TIMESTAMP WITH TIME ZONE NOT NULL,
   updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
   deleted_at TIMESTAMP WITH TIME ZONE,
   from_currency VARCHAR(3) NOT NULL,
   to_currency VARCHAR(3) NOT NULL,
   rate DECIMAL(18, 6) NOT NULL,
   provider VARCHAR(50),
   fetched_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Seed Initial Currencies
INSERT INTO currencies (id, version, created_at, updated_at, code, name, symbol, is_active, is_base)
VALUES
('018f6f5d-7a00-7000-8000-000000000001', 0, NOW(), NOW(), 'USD', 'US Dollar', '$', TRUE, TRUE),
('018f6f5d-7a00-7000-8000-000000000002', 0, NOW(), NOW(), 'VND', 'Việt Nam Đồng', '₫', TRUE, FALSE);

-- Create Indexes
CREATE INDEX idx_currencies_code ON currencies(code);
CREATE INDEX idx_exchange_rates_to_currency ON exchange_rates(to_currency);
