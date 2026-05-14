-- V4: Search and Analytics Logs

CREATE TABLE IF NOT EXISTS search_logs (
   id UUID PRIMARY KEY,
   user_id UUID REFERENCES users(id) ON DELETE SET NULL,
   query_text TEXT NOT NULL,
   country_code VARCHAR(10),
   ip_address VARCHAR(45),
   is_boosted BOOLEAN DEFAULT FALSE,
   
   version BIGINT DEFAULT 0,
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_search_logs_query ON search_logs(query_text);
CREATE INDEX idx_search_logs_created ON search_logs(created_at);
