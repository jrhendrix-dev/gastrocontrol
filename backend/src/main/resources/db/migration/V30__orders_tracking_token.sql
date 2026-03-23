-- V30__orders_tracking_token.sql
-- Adds a publicly-shareable opaque tracking token to orders.
--
-- Design:
--   - UUID stored as CHAR(36) for readability and portability
--   - Only populated for TAKE_AWAY and DELIVERY orders (NULL for DINE_IN)
--   - Unique constraint prevents token collisions
--   - Customers receive the token in their confirmation URL: /track/{token}
--   - Sequential order IDs are never exposed publicly

ALTER TABLE orders
    ADD COLUMN tracking_token VARCHAR(36) NULL
        COMMENT 'Opaque UUID used in public tracking URLs. NULL for DINE_IN orders.';

CREATE UNIQUE INDEX uq_orders_tracking_token
    ON orders (tracking_token);