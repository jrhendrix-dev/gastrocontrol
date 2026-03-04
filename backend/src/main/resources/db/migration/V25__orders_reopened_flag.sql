-- V25__orders_reopened_flag.sql
-- Adds a boolean flag to track whether an order has been reopened after payment.
--
-- When an order is reopened:
--   - This flag is set to TRUE, which unlocks item editing even for paid orders.
--   - It must be cleared (set back to FALSE) only after a financial adjustment
--     has been processed (ProcessOrderAdjustmentService).
--   - ChangeOrderStatusService blocks FINISHED while this flag is TRUE.
--
-- This avoids hacking the status pipeline for a transient "edit window" concern.

ALTER TABLE orders
    ADD COLUMN reopened TINYINT(1) NOT NULL DEFAULT 0
        COMMENT 'TRUE while the order is in an edit-unlocked state post-reopen. Must be cleared by ProcessOrderAdjustmentService.'
        AFTER closed_at;

-- Index is not strictly required but useful if you query "all reopened orders" in ops dashboards.
CREATE INDEX idx_orders_reopened ON orders (reopened);
