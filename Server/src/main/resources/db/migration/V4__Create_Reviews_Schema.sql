-- V4: Create Reviews Schema (Reviews table, property aggregated ratings, indexing)

CREATE TABLE IF NOT EXISTS reviews (
   id UUID PRIMARY KEY,
   booking_id UUID NOT NULL UNIQUE,
   property_id UUID NOT NULL,
   user_id UUID NOT NULL,
   rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
   comment VARCHAR(1000) CHECK (char_length(comment) >= 10),
   reply TEXT,
   status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
   reply_updated_at TIMESTAMP WITH TIME ZONE,
   deleted_at TIMESTAMP WITH TIME ZONE,
   deleted_by UUID,
   deletion_reason VARCHAR(255),
   moderated_at TIMESTAMP WITH TIME ZONE,
   moderated_by UUID,
   moderation_reason VARCHAR(255),
   created_at TIMESTAMP WITH TIME ZONE NOT NULL,
   updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

   FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
   FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE,
   FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
   FOREIGN KEY (deleted_by) REFERENCES users(id) ON DELETE SET NULL,
   FOREIGN KEY (moderated_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Add aggregated rating fields to properties table
ALTER TABLE properties ADD COLUMN average_rating NUMERIC(4,2) DEFAULT 0.00;
ALTER TABLE properties ADD COLUMN review_count INTEGER DEFAULT 0;
ALTER TABLE properties ADD COLUMN rating_sum BIGINT DEFAULT 0;

-- Optimize index structures for expected query patterns
CREATE INDEX idx_reviews_property_created ON reviews(property_id, created_at DESC) WHERE deleted_at IS NULL AND status = 'PUBLISHED';
CREATE INDEX idx_reviews_user ON reviews(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_booking ON reviews(booking_id);
CREATE INDEX idx_reviews_moderation ON reviews(status, created_at) WHERE deleted_at IS NULL;
