CREATE TABLE coupon_release_retries (
   id UUID PRIMARY KEY,
   booking_id UUID NOT NULL,
   coupon_id UUID NOT NULL,
   user_id UUID NOT NULL,
   attempt_count INT NOT NULL,
   last_attempt_at TIMESTAMP,
   next_attempt_at TIMESTAMP NOT NULL,
   status VARCHAR(20) NOT NULL,
   created_at TIMESTAMP NOT NULL
);

-- Partial unique index to allow only one active PENDING retry record per booking/coupon combination
CREATE UNIQUE INDEX ux_coupon_retry_pending
ON coupon_release_retries (booking_id, coupon_id)
WHERE status = 'PENDING';

-- Index to optimize the scheduler query selecting pending retries
CREATE INDEX idx_coupon_retry_pending_sched
ON coupon_release_retries (status, next_attempt_at);
