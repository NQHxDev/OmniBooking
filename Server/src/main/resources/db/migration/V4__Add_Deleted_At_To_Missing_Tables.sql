-- V4: Add deleted_at column to missing tables that extend BaseEntity
ALTER TABLE amenities ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE room_availability ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE booking_status_logs ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
