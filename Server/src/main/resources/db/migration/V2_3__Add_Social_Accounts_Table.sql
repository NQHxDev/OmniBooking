-- Create Social Accounts Table
CREATE TABLE IF NOT EXISTS social_accounts (
   id UUID PRIMARY KEY,
   user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
   provider VARCHAR(50) NOT NULL,
   provider_id VARCHAR(255) NOT NULL,
   version BIGINT DEFAULT 0,
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE,
   UNIQUE(provider, provider_id)
);

-- Make users.password nullable for social login users
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;

-- Create index for faster lookups
CREATE INDEX idx_social_accounts_user_id ON social_accounts(user_id);
CREATE INDEX idx_social_accounts_provider_id ON social_accounts(provider, provider_id);
