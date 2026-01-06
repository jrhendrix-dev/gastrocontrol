-- R__dev_reset.sql
SET FOREIGN_KEY_CHECKS=0;

TRUNCATE TABLE order_items;
TRUNCATE TABLE order_events;
TRUNCATE TABLE orders;
TRUNCATE TABLE refresh_tokens;

TRUNCATE TABLE products;
TRUNCATE TABLE categories;

-- If you want to wipe users too, do it last:
-- TRUNCATE TABLE users;

SET FOREIGN_KEY_CHECKS=1;
