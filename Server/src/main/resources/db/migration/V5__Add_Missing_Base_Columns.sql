-- V5: Add missing BaseEntity columns to amenities and booking_status_logs
ALTER TABLE amenities ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE booking_status_logs ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE booking_status_logs ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
