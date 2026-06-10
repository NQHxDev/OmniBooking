-- STEP 1: Investigation report — DO NOT auto-delete
-- Run this query manually and review results before applying constraint:
--
-- SELECT provider_transaction_id, COUNT(*) as cnt
-- FROM transactions
-- WHERE provider_transaction_id IS NOT NULL
-- GROUP BY provider_transaction_id
-- HAVING COUNT(*) > 1;
--
-- If duplicates exist, investigate and resolve manually before proceeding.

-- STEP 2: Apply unique constraint (only after manual verification)
ALTER TABLE transactions
   ADD CONSTRAINT uq_transactions_provider_tx_id
   UNIQUE (provider_transaction_id);
