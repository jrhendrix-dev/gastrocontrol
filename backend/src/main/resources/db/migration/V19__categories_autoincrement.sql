-- V19__categories_autoincrement.sql
-- Make categories.id AUTO_INCREMENT (MySQL)

ALTER TABLE products
    DROP FOREIGN KEY fk_products_category;

ALTER TABLE categories
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE products
    ADD CONSTRAINT fk_products_category
        FOREIGN KEY (category_id) REFERENCES categories(id);
