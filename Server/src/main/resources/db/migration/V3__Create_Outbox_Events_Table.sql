-- Create Outbox Events Table
CREATE TABLE outbox_events (
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

-- Create Index for Unprocessed Events
CREATE INDEX idx_outbox_unprocessed ON outbox_events(processed) WHERE processed = FALSE;
