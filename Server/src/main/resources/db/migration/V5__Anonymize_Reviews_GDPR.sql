-- V5__Anonymize_Reviews_GDPR.sql
-- Drop existing cascade constraints to prevent silent data loss and ratings mismatch
ALTER TABLE reviews DROP CONSTRAINT IF EXISTS reviews_user_id_fkey;
ALTER TABLE reviews DROP CONSTRAINT IF EXISTS reviews_booking_id_fkey;

-- Make user_id nullable to support anonymization (ON DELETE SET NULL)
ALTER TABLE reviews ALTER COLUMN user_id DROP NOT NULL;

-- Add new constraints that handle deletion safely
ALTER TABLE reviews ADD CONSTRAINT reviews_user_id_fkey 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;
    
ALTER TABLE reviews ADD CONSTRAINT reviews_booking_id_fkey 
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE RESTRICT;
