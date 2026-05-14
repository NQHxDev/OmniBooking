-- Create User Passkeys Table
CREATE TABLE user_passkeys (
   id UUID PRIMARY KEY,
   version BIGINT,
   created_at TIMESTAMP WITH TIME ZONE NOT NULL,
   updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
   deleted_at TIMESTAMP WITH TIME ZONE,
   user_id UUID NOT NULL REFERENCES users(id),
   credential_id TEXT NOT NULL UNIQUE,
   public_key TEXT NOT NULL,
   sign_count BIGINT NOT NULL,
   label VARCHAR(255),
   aaguid VARCHAR(255)
);

-- Create Index
CREATE INDEX idx_user_passkeys_user_id ON user_passkeys(user_id);
