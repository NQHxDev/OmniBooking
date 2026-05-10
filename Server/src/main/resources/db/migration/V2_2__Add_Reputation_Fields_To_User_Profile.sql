-- Add reputation and verification fields to user_profiles
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS reputation_score DOUBLE PRECISION DEFAULT 100.0;
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS is_verified BOOLEAN DEFAULT FALSE;
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS partner_bio TEXT;
