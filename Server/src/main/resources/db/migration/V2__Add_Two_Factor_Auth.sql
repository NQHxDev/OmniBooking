-- V2: Add Two Factor Authentication support

CREATE TABLE IF NOT EXISTS user_two_factor (
   id UUID PRIMARY KEY,
   user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
   secret_key VARCHAR(255) NOT NULL,
   is_enabled BOOLEAN DEFAULT FALSE,
   backup_codes TEXT, -- Hashed JSON list of backup codes
   version BIGINT DEFAULT 0,
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_user_two_factor_user_id ON user_two_factor(user_id);
