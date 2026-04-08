-- V32__enforce_product_name_max_length.sql
--
-- The product name column was originally 160 characters.
-- We enforce a 60-character maximum to match the DTO validation
-- and prevent overly long names from breaking the POS product grid.
--
-- IMPORTANT: Verify no existing rows exceed 60 characters before running:
--   SELECT id, name FROM products WHERE CHAR_LENGTH(name) > 60;

ALTER TABLE products
    MODIFY COLUMN name VARCHAR(60) NOT NULL;