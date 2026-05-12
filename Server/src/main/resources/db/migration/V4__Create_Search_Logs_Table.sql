-- Create Search Logs Table
CREATE TABLE search_logs (
   id UUID PRIMARY KEY,
   version BIGINT,
   created_at TIMESTAMP WITH TIME ZONE NOT NULL,
   updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
   deleted_at TIMESTAMP WITH TIME ZONE,
   query_text TEXT NOT NULL,
   country_code VARCHAR(2),
   user_id VARCHAR(255),
   is_boosted BOOLEAN NOT NULL DEFAULT FALSE
);

-- Create Indexes
CREATE INDEX idx_search_logs_query_text ON search_logs(query_text);
CREATE INDEX idx_search_logs_created_at ON search_logs(created_at);
CREATE INDEX idx_search_logs_country_code ON search_logs(country_code);

-- Seed Some Sample Trending Data (Vietnam)
INSERT INTO search_logs (id, version, created_at, updated_at, query_text, country_code, is_boosted)
VALUES
   (gen_random_uuid(), 0, NOW(), NOW(), 'Đà Lạt', 'VN', false),
   (gen_random_uuid(), 0, NOW(), NOW(), 'Hồ Chí Minh', 'VN', false),
   (gen_random_uuid(), 0, NOW(), NOW(), 'Hà Nội', 'VN', false),
   (gen_random_uuid(), 0, NOW(), NOW(), 'Quảng Ninh', 'VN', false),
   (gen_random_uuid(), 0, NOW(), NOW(), 'Phú Quốc', 'VN', false),
   (gen_random_uuid(), 0, NOW(), NOW(), 'Đà Nẵng', 'VN', true);
