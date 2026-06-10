-- 1. Add expires_at to bookings
ALTER TABLE bookings ADD COLUMN expires_at TIMESTAMP WITH TIME ZONE;

-- 2. Partial index for expiration worker queries
CREATE INDEX idx_bookings_status_expires
   ON bookings(status, expires_at)
   WHERE expires_at IS NOT NULL AND deleted_at IS NULL;

-- 3. Inventory operations ledger (audit-only)
CREATE TABLE inventory_operations (
   id UUID PRIMARY KEY,
   booking_id UUID NOT NULL REFERENCES bookings(id),
   room_type_id UUID NOT NULL REFERENCES room_types(id),
   availability_date DATE NOT NULL,
   operation_type VARCHAR(10) NOT NULL,
   num_rooms INTEGER NOT NULL,
   created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
   updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
   deleted_at TIMESTAMP WITH TIME ZONE,
   version BIGINT DEFAULT 0
);
CREATE INDEX idx_inv_ops_booking ON inventory_operations(booking_id);
CREATE INDEX idx_inv_ops_type ON inventory_operations(operation_type, booking_id);

-- 4. Lifecycle consistency CHECK constraints
ALTER TABLE bookings ADD CONSTRAINT chk_pending_payment_has_expiry
   CHECK (status <> 'PENDING_PAYMENT' OR expires_at IS NOT NULL);

ALTER TABLE bookings ADD CONSTRAINT chk_confirmed_no_expiry
   CHECK (status <> 'CONFIRMED' OR expires_at IS NULL);

ALTER TABLE bookings ADD CONSTRAINT chk_expired_has_expiry
   CHECK (status <> 'EXPIRED' OR expires_at IS NOT NULL);

-- 5. Extend status column length for new values
ALTER TABLE bookings ALTER COLUMN status TYPE VARCHAR(30);
ALTER TABLE booking_status_logs ALTER COLUMN old_status TYPE VARCHAR(30);
ALTER TABLE booking_status_logs ALTER COLUMN new_status TYPE VARCHAR(30);
