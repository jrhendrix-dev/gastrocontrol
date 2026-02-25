-- V24__payments_manual_and_amount_constraints.sql
-- Enforce positive amounts + add optional manual reference for dine-in confirmations.

ALTER TABLE payments
    ADD COLUMN manual_reference VARCHAR(120) NULL AFTER payment_intent_id;

-- MySQL 8.0.16+ enforces CHECK constraints
ALTER TABLE payments
    ADD CONSTRAINT chk_payments_amount_cents_positive
        CHECK (amount_cents > 0);

-- Optional: keep currency meaningful (recommended)
ALTER TABLE payments
    ADD CONSTRAINT chk_payments_currency_not_blank
        CHECK (CHAR_LENGTH(TRIM(currency)) > 0);
