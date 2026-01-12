CREATE TABLE order_events (
                              id BIGINT NOT NULL AUTO_INCREMENT,
                              order_id BIGINT NOT NULL,
                              event_type VARCHAR(50) NOT NULL,
                              from_status VARCHAR(30) NULL,
                              to_status VARCHAR(30) NULL,
                              message TEXT NULL,
                              actor_role VARCHAR(30) NULL,
                              created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                              PRIMARY KEY (id),
                              CONSTRAINT fk_order_events_order
                                  FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_order_events_order_id ON order_events(order_id);
CREATE INDEX idx_order_events_created_at ON order_events(created_at);
