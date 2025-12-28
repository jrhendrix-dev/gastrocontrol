ALTER TABLE orders
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- For existing rows (MySQL will backfill with default, but this is explicit)
UPDATE orders SET version = 0 WHERE version IS NULL;
