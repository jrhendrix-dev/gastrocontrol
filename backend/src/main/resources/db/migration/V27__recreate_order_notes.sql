-- V27__recreate_order_notes.sql
--
-- Recreates the order_notes table that was dropped in V13.
-- The table is now required by the notes feature on the POS and Kitchen Display.
--
-- Schema is identical to V4 so existing tooling (entity, repository) maps correctly.

CREATE TABLE order_notes (
                             id          BIGINT       NOT NULL AUTO_INCREMENT,
                             order_id    BIGINT       NOT NULL,
                             note        TEXT         NOT NULL,
                             author_role VARCHAR(30)  NULL,
                             created_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                             PRIMARY KEY (id),

                             CONSTRAINT fk_order_notes_order
                                 FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE INDEX idx_order_notes_order_id ON order_notes (order_id);
CREATE INDEX idx_order_notes_created_at ON order_notes (created_at);