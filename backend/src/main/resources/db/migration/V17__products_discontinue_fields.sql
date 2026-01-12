-- src/main/resources/db/migration/V17__products_discontinue_fields.sql
ALTER TABLE products
    ADD COLUMN discontinued_at DATETIME(6) NULL,
  ADD COLUMN discontinued_reason VARCHAR(255) NULL,
  ADD COLUMN discontinued_by_user_id BIGINT NULL;

CREATE INDEX idx_products_discontinued_by ON products(discontinued_by_user_id);

ALTER TABLE products
    ADD CONSTRAINT fk_products_discontinued_by
        FOREIGN KEY (discontinued_by_user_id) REFERENCES users(id);
