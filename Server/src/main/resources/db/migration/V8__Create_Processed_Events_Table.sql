-- V8: Create Processed Events Table

CREATE TABLE IF NOT EXISTS processed_events (
   event_id UUID NOT NULL,
   consumer_group VARCHAR(100) NOT NULL,
   processed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   PRIMARY KEY (event_id, consumer_group)
);

CREATE INDEX IF NOT EXISTS idx_processed_events_time ON processed_events(processed_at);
