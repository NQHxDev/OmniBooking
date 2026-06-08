-- V2: Add version column to reviews table to resolve schema-drift

-- Step 1: Add column allowing NULL temporarily
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS version BIGINT;

-- Step 2: Set default value for existing rows
UPDATE reviews SET version = 0 WHERE version IS NULL;

-- Step 3: Enforce NOT NULL constraint
ALTER TABLE reviews ALTER COLUMN version SET NOT NULL;

-- Step 4: Set default for future inserts
ALTER TABLE reviews ALTER COLUMN version SET DEFAULT 0;
