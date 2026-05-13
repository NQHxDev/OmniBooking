-- V5: Finance (Currency and Exchange Rates)

CREATE TABLE IF NOT EXISTS currencies (
   id UUID PRIMARY KEY,
   code VARCHAR(3) UNIQUE NOT NULL,
   name VARCHAR(50) NOT NULL,
   symbol VARCHAR(10),
   is_active BOOLEAN DEFAULT TRUE,

   version BIGINT DEFAULT 0,
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS exchange_rates (
   id UUID PRIMARY KEY,
   base_currency VARCHAR(3) NOT NULL,
   target_currency VARCHAR(3) NOT NULL,
   rate DECIMAL(19, 6) NOT NULL,
   last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

   UNIQUE(base_currency, target_currency)
);

-- Seed Default Currencies
INSERT INTO currencies (id, code, name, symbol)
VALUES
   (gen_random_uuid(), 'USD', 'US Dollar', '$'),
   (gen_random_uuid(), 'VND', 'Vietnamese Dong', '₫')
ON CONFLICT (code) DO NOTHING;
