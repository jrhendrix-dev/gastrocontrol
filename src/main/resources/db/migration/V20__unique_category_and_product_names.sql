-- V20__unique_category_and_product_names.sql
-- Enforce uniqueness for category.name and product.name

ALTER TABLE categories
    ADD CONSTRAINT uq_categories_name UNIQUE (name);

ALTER TABLE products
    ADD CONSTRAINT uq_products_name UNIQUE (name);
