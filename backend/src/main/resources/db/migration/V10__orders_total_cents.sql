-- Add persisted total cents to orders
ALTER TABLE orders
    ADD COLUMN total_cents INT NOT NULL DEFAULT 0;

-- Backfill existing orders based on order_items
UPDATE orders o
    LEFT JOIN (
    SELECT
    order_id,
    COALESCE(SUM(quantity * unit_price_cents), 0) AS total_cents
    FROM order_items
    GROUP BY order_id
    ) x ON x.order_id = o.id
    SET o.total_cents = COALESCE(x.total_cents, 0);
