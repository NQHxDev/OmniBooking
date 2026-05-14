-- V7: Add is_base to currencies table
ALTER TABLE currencies ADD COLUMN IF NOT EXISTS is_base BOOLEAN NOT NULL DEFAULT FALSE;

-- Set USD as default base currency for existing data
UPDATE currencies SET is_base = TRUE WHERE code = 'USD';
