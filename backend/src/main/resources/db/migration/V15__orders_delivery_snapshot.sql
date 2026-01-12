-- V15__orders_delivery_snapshot.sql
-- Delivery snapshot fields on orders.
-- Keep nullable; enforce rules in service layer (required when type='DELIVERY').

ALTER TABLE orders
    ADD COLUMN delivery_name VARCHAR(120) NULL AFTER dining_table_id,
    ADD COLUMN delivery_phone VARCHAR(30) NULL AFTER delivery_name,
    ADD COLUMN delivery_address_line1 VARCHAR(190) NULL AFTER delivery_phone,
    ADD COLUMN delivery_address_line2 VARCHAR(190) NULL AFTER delivery_address_line1,
    ADD COLUMN delivery_city VARCHAR(120) NULL AFTER delivery_address_line2,
    ADD COLUMN delivery_postal_code VARCHAR(20) NULL AFTER delivery_city,
    ADD COLUMN delivery_notes VARCHAR(500) NULL AFTER delivery_postal_code;

-- Helpful index for delivery ops screens (optional but practical)
CREATE INDEX idx_orders_delivery_phone ON orders(delivery_phone);
CREATE INDEX idx_orders_delivery_postal_code ON orders(delivery_postal_code);
