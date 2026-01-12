-- Make products.id AUTO_INCREMENT (MySQL) while preserving FK integrity

-- 1) Drop FK(s) that reference products(id)
ALTER TABLE order_items
    DROP FOREIGN KEY fk_order_items_product;

-- 2) Ensure products.id is compatible with AUTO_INCREMENT
-- (If id is already PK, this is fine. If not, you MUST add a PK elsewhere.)
ALTER TABLE products
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;

-- 3) Recreate FK(s)
ALTER TABLE order_items
    ADD CONSTRAINT fk_order_items_product
        FOREIGN KEY (product_id) REFERENCES products(id);
