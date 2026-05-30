-- V10: Add lease_until column to processed_events table to support heartbeat/lease renewals

ALTER TABLE processed_events ADD COLUMN IF NOT EXISTS lease_until TIMESTAMP WITH TIME ZONE;

-- Update existing records: COMPLETED ones have far future expiration, PROCESSING ones have short expiration
UPDATE processed_events SET lease_until = CURRENT_TIMESTAMP + INTERVAL '365 days' WHERE status = 'COMPLETED';
UPDATE processed_events SET lease_until = CURRENT_TIMESTAMP + INTERVAL '5 minutes' WHERE status = 'PROCESSING';
UPDATE processed_events SET lease_until = CURRENT_TIMESTAMP WHERE status = 'FAILED';
