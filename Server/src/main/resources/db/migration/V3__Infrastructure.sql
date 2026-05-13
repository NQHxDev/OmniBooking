-- V3: Infrastructure (Outbox, Social Accounts)

-- Create Outbox Events Table
CREATE TABLE IF NOT EXISTS outbox_events (
   id UUID PRIMARY KEY,
   aggregate_id UUID NOT NULL,
   aggregate_type VARCHAR(50) NOT NULL,
   event_type VARCHAR(100) NOT NULL,
   payload TEXT NOT NULL,
   payload_class VARCHAR(255) NOT NULL,
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE,
   version BIGINT DEFAULT 0,
   processed BOOLEAN DEFAULT FALSE,
   processed_at TIMESTAMP WITH TIME ZONE
);

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

-- Create Indexes
CREATE INDEX idx_outbox_unprocessed ON outbox_events(processed) WHERE processed = FALSE;
CREATE INDEX idx_social_accounts_user_id ON social_accounts(user_id);
CREATE INDEX idx_social_accounts_provider_id ON social_accounts(provider, provider_id);
