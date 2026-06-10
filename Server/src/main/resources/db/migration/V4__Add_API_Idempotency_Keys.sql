CREATE TABLE idempotency_keys (
   id UUID PRIMARY KEY,
   idempotency_key VARCHAR(255) NOT NULL,
   endpoint VARCHAR(255) NOT NULL,
   request_hash VARCHAR(255) NOT NULL,
   response_payload JSONB,
   response_status INTEGER,
   processing_status VARCHAR(50) NOT NULL,
   response_cached BOOLEAN NOT NULL DEFAULT TRUE,
   created_at TIMESTAMP WITH TIME ZONE NOT NULL,
   expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
   processing_started_at TIMESTAMP WITH TIME ZONE NOT NULL,
   CONSTRAINT uq_endpoint_idempotency_key UNIQUE (endpoint, idempotency_key)
);

CREATE INDEX idx_idempotency_keys_expires_at ON idempotency_keys (expires_at);
