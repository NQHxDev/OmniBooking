-- V2: Add Payment Hardening Columns, Constraints, and Audit Table

-- 1. Add Columns to transactions Table
ALTER TABLE transactions ADD COLUMN provider_order_id VARCHAR(255);
ALTER TABLE transactions ADD COLUMN local_amount DECIMAL(19, 4);
ALTER TABLE transactions ADD COLUMN local_currency VARCHAR(3);

-- 2. Drop Old Unique Constraint and Add New Composite Unique Constraints
ALTER TABLE transactions DROP CONSTRAINT IF EXISTS uq_transactions_provider_tx_id;
ALTER TABLE transactions ADD CONSTRAINT uq_transactions_provider_order_id UNIQUE (payment_method, provider_order_id);
ALTER TABLE transactions ADD CONSTRAINT uq_transactions_provider_tx_id UNIQUE (payment_method, provider_transaction_id);

-- 3. Create Performance Indexes for transactions Table
CREATE INDEX IF NOT EXISTS idx_transactions_provider_order_id ON transactions(provider_order_id);
CREATE INDEX IF NOT EXISTS idx_transactions_provider_transaction_id ON transactions(provider_transaction_id);
CREATE INDEX IF NOT EXISTS idx_transactions_booking_id_status ON transactions(booking_id, status);

-- 4. Create payment_events Audit Table
CREATE TABLE IF NOT EXISTS payment_events (
   id UUID PRIMARY KEY,
   transaction_id UUID,
   booking_id UUID,
   event_type VARCHAR(50) NOT NULL,
   event_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
   metadata JSONB,
   version BIGINT DEFAULT 0
);

-- 5. Create Indexes for payment_events Table
CREATE INDEX IF NOT EXISTS idx_payment_events_transaction_id ON payment_events(transaction_id);
CREATE INDEX IF NOT EXISTS idx_payment_events_booking_id ON payment_events(booking_id);
