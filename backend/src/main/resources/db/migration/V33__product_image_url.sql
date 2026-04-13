-- V33__product_image_url.sql
-- Adds an optional image URL column to the products table.
-- The column stores a server-relative path (e.g. /gastrocontrol/uploads/products/42.webp)
-- written by the backend after storing the file on the uploads volume.

ALTER TABLE products
    ADD COLUMN image_url VARCHAR(500) NULL AFTER description;