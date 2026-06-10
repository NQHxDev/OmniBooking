-- Migration: Kafka Reliability and Idempotency Hardening
-- Adds audit columns to registration_inbox and composite UNIQUE constraint to registration_dlt

-- 1. Add audit columns to registration_inbox for stale-processing troubleshooting
ALTER TABLE registration_inbox ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL;
ALTER TABLE registration_inbox ADD COLUMN IF NOT EXISTS processing_started_at TIMESTAMP WITH TIME ZONE;

-- 2. Add composite UNIQUE constraint to registration_dlt to prevent duplicate entries for the same partition & offset
-- Note: Clean up any potential duplicates beforehand if present (normally not needed in clean testdb, but safe)
ALTER TABLE registration_dlt ADD CONSTRAINT uq_reg_dlt_partition_offset UNIQUE (partition_id, offset_val);
