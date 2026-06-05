-- V3: Authentication Hardening (Retry metadata, DLT store, DLT auditing)

-- Extend registration_inbox table
ALTER TABLE registration_inbox
ADD COLUMN retry_count INTEGER NOT NULL DEFAULT 0,
ADD COLUMN last_error TEXT,
ADD COLUMN next_retry_at TIMESTAMP WITH TIME ZONE,
ADD COLUMN processed_at TIMESTAMP WITH TIME ZONE;

-- Optimize index for scheduling queries
DROP INDEX IF EXISTS idx_reg_inbox_status;
CREATE INDEX IF NOT EXISTS idx_reg_inbox_status_retry
ON registration_inbox(status, next_retry_at)
WHERE status IN ('PENDING', 'PROCESSING');

-- 2. Create DLT table
CREATE TABLE IF NOT EXISTS registration_dlt (
   request_id UUID PRIMARY KEY,
   email VARCHAR(255) NOT NULL,
   payload JSONB NOT NULL,
   partition_id INTEGER NOT NULL,
   offset_val BIGINT NOT NULL,
   original_error TEXT,
   status VARCHAR(20) NOT NULL, -- PENDING, REPLAYED, FAILED
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
   last_replayed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_reg_dlt_status ON registration_dlt(status);

-- 3. Create DLT Audit table
CREATE TABLE IF NOT EXISTS registration_dlt_audit (
   id UUID PRIMARY KEY,
   request_id UUID NOT NULL,
   replayed_by VARCHAR(255) NOT NULL,
   replayed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
   original_error TEXT,
   replay_result VARCHAR(50) NOT NULL,
   error_message TEXT
);
