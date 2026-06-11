-- V5: Add Inventory Idempotency, Check Constraints, and Booking Indexes

-- 1. Clean up duplicate records in inventory_operations (if any) before applying the UNIQUE constraint
DELETE FROM inventory_operations io1
USING inventory_operations io2
WHERE io1.id > io2.id
  AND io1.booking_id = io2.booking_id
  AND io1.availability_date = io2.availability_date
  AND io1.operation_type = io2.operation_type;

-- 2. Add a UNIQUE constraint to prevent duplicate room hold/release operations
ALTER TABLE inventory_operations
ADD CONSTRAINT uq_inventory_ops_booking_date_type
UNIQUE (booking_id, availability_date, operation_type);

-- 3. Add a CHECK constraint to ensure room availability count is never negative
ALTER TABLE room_availability
ADD CONSTRAINT chk_room_availability_count CHECK (available_count >= 0);

-- 4. Create indexes to optimize booking search and expiration sweep processes
CREATE INDEX IF NOT EXISTS idx_bookings_check_out_date ON bookings(check_out_date);
CREATE INDEX IF NOT EXISTS idx_bookings_status_expires_at ON bookings(status, expires_at);
