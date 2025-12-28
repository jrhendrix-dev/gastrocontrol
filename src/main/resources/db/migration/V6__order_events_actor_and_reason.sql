-- Adds fields we’ll populate later from auth, but safe to have now.
ALTER TABLE order_events
    ADD COLUMN actor_user_id BIGINT NULL AFTER actor_role,
  ADD COLUMN reason_code VARCHAR(50) NULL AFTER message;

CREATE INDEX idx_order_events_actor_user_id ON order_events(actor_user_id);
CREATE INDEX idx_order_events_reason_code ON order_events(reason_code);
