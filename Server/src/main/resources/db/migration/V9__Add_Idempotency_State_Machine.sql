-- V9: Add Idempotency State Machine columns to processed_events table

ALTER TABLE processed_events ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING';
ALTER TABLE processed_events ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

-- Mark all pre-existing processed events as COMPLETED to avoid recovery scan
UPDATE processed_events SET status = 'COMPLETED';
