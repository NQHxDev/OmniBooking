-- V8: Fix Exchange Rates Schema to match BaseEntity

-- Add missing audit columns to exchange_rates table
ALTER TABLE exchange_rates ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE exchange_rates ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE exchange_rates ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE exchange_rates ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- Optional: Add provider column if it's being used in queries but missing in DB
-- (Looking at your logs, it seems to be expected)
ALTER TABLE exchange_rates ADD COLUMN IF NOT EXISTS provider VARCHAR(50);
ALTER TABLE exchange_rates ADD COLUMN IF NOT EXISTS fetched_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
