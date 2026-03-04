-- V26__payments_paid_amount_cents.sql
-- Adds a snapshot of the amount that was actually paid at confirmation time.
--
-- WHY this is separate from amount_cents:
--   amount_cents = what we originally requested (can be updated on reopen)
--   paid_amount_cents = what Stripe / manual actually confirmed
--
-- This lets ProcessOrderAdjustmentService compute an accurate delta:
--   delta = newTotalCents - paid_amount_cents
--   delta > 0 → extra charge needed
--   delta < 0 → partial refund needed
--   delta = 0 → no financial action needed
--
-- NULL means payment has not yet been confirmed (status != SUCCEEDED).

ALTER TABLE payments
    ADD COLUMN paid_amount_cents INT NULL
        COMMENT 'Snapshot of the confirmed payment amount in cents. Set when status transitions to SUCCEEDED. NULL until then.'
        AFTER amount_cents;

ALTER TABLE payments
    ADD CONSTRAINT chk_payments_paid_amount_cents_positive
        CHECK (paid_amount_cents IS NULL OR paid_amount_cents > 0);
