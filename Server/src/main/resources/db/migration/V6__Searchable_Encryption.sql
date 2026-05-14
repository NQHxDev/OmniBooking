-- V6: Searchable Encryption for Phone Numbers

-- Update user_profiles table
ALTER TABLE user_profiles ADD COLUMN phone_encrypted VARCHAR(255);
ALTER TABLE user_profiles ADD COLUMN phone_search_hash VARCHAR(64);

-- Create index for high-performance searching
CREATE INDEX idx_user_profiles_phone_search_hash ON user_profiles(phone_search_hash);

-- Remove the old plain-text phone number column
ALTER TABLE user_profiles DROP COLUMN phone_number;

-- Update bookings table
ALTER TABLE bookings ADD COLUMN guest_phone_encrypted VARCHAR(255);
ALTER TABLE bookings ADD COLUMN guest_phone_search_hash VARCHAR(64);

-- Create index for high-performance searching
CREATE INDEX idx_bookings_guest_phone_search_hash ON bookings(guest_phone_search_hash);

-- Remove the old plain-text guest phone column
ALTER TABLE bookings DROP COLUMN guest_phone;
