-- V11: Add token_version column to users table to support JWT Revocation / Revocation Gap
ALTER TABLE users ADD COLUMN IF NOT EXISTS token_version INT NOT NULL DEFAULT 0;
