-- Add timestamps useful for order lifecycle tracking

ALTER TABLE orders
    ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ADD COLUMN closed_at TIMESTAMP(6) NULL;

-- Keep updated_at current
-- MySQL supports ON UPDATE for TIMESTAMP columns.
ALTER TABLE orders
    MODIFY updated_at TIMESTAMP(6) NOT NULL
    DEFAULT CURRENT_TIMESTAMP(6)
    ON UPDATE CURRENT_TIMESTAMP(6);

-- Helpful for filtering active/closed orders
CREATE INDEX idx_orders_status_updated_at ON orders(status, updated_at);
CREATE INDEX idx_orders_closed_at ON orders(closed_at);
