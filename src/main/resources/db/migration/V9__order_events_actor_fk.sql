ALTER TABLE order_events
    ADD CONSTRAINT fk_order_events_actor_user
        FOREIGN KEY (actor_user_id) REFERENCES users(id);

CREATE INDEX idx_order_events_actor_user_id ON order_events(actor_user_id);
