-- V7: Alter Outbox Events For Idempotency And Retry

-- 1. Add new columns for status and retry metadata
ALTER TABLE outbox_events ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE outbox_events ADD COLUMN IF NOT EXISTS retry_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE outbox_events ADD COLUMN IF NOT EXISTS next_retry_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE outbox_events ADD COLUMN IF NOT EXISTS last_error TEXT;
ALTER TABLE outbox_events ADD COLUMN IF NOT EXISTS event_version INTEGER NOT NULL DEFAULT 1;

-- 2. Migrate existing processed data to status
UPDATE outbox_events SET status = 'PROCESSED' WHERE processed = TRUE;
UPDATE outbox_events SET status = 'PENDING' WHERE processed = FALSE;

-- 3. Drop deprecated columns
ALTER TABLE outbox_events DROP COLUMN IF EXISTS processed;
ALTER TABLE outbox_events DROP COLUMN IF EXISTS processed_at;
ALTER TABLE outbox_events DROP COLUMN IF EXISTS payload_class;

-- 4. Rebuild indexes for high-performance processing queries
DROP INDEX IF EXISTS idx_outbox_unprocessed;
CREATE INDEX idx_outbox_processing ON outbox_events(status, next_retry_at) WHERE status IN ('PENDING', 'PROCESSING');
