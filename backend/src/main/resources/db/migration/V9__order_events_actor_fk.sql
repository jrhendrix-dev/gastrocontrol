ALTER TABLE order_events
    ADD CONSTRAINT fk_order_events_actor_user
        FOREIGN KEY (actor_user_id) REFERENCES users(id);

