CREATE TABLE order_notes (
                             id BIGINT NOT NULL AUTO_INCREMENT,
                             order_id BIGINT NOT NULL,
                             note TEXT NOT NULL,
                             created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    -- optional: who wrote it (keep it simple as text for now)
                             author_role VARCHAR(30) NULL,
                             PRIMARY KEY (id),
                             CONSTRAINT fk_order_notes_order
                                 FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_order_notes_order_id ON order_notes(order_id);

-- Why a separate table instead of a column in orders?
-- Notes are typically multiple, time-ordered, and you’ll want to keep a record