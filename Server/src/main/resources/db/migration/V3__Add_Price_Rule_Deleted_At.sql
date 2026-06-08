-- Migration to add missing deleted_at column to price_rules table
-- This is required because PriceRule entity extends BaseEntity which maps deletedAt property
ALTER TABLE price_rules ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
