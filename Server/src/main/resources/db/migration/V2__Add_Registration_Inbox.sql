-- V2: Add Registration Inbox Table for Asynchronous Registration Pipeline

CREATE TABLE IF NOT EXISTS registration_inbox (
    request_id UUID PRIMARY KEY,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL, -- PENDING, SENT, PROCESSING, SUCCESS, FAILED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE
);

-- Index for scanning pending/processing records quickly (used by recovery workers)
CREATE INDEX IF NOT EXISTS idx_reg_inbox_status 
ON registration_inbox(status) 
WHERE status IN ('PENDING', 'PROCESSING');
