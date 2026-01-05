-- src/main/resources/db/migration/V16__orders_pickup_snapshot.sql
-- Pickup snapshot fields on orders.
-- Keep nullable; enforce rules in service layer (required when type='TAKE_AWAY').

ALTER TABLE orders
    ADD COLUMN pickup_name VARCHAR(120) NULL AFTER delivery_notes,
  ADD COLUMN pickup_phone VARCHAR(30) NULL AFTER pickup_name,
  ADD COLUMN pickup_notes VARCHAR(500) NULL AFTER pickup_phone;

-- Optional but practical indexes (search/call-out screens)
CREATE INDEX idx_orders_pickup_phone ON orders(pickup_phone);
