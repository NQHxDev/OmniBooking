-- V2.1: Media Management Table
CREATE TABLE IF NOT EXISTS media (
   id UUID PRIMARY KEY,
   url TEXT NOT NULL,
   public_id VARCHAR(255) NOT NULL,
   format VARCHAR(20),
   resource_type VARCHAR(20),
   bytes BIGINT,
   
   -- Relation fields
   entity_id UUID NOT NULL,
   entity_type VARCHAR(50) NOT NULL, -- PROPERTY, ROOM_TYPE, USER_AVATAR
   is_main BOOLEAN DEFAULT FALSE,
   
   version BIGINT DEFAULT 0,
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_media_entity ON media(entity_id, entity_type);
