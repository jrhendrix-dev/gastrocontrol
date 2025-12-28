-- V2__seed_reference_data.sql
-- Seed "reference" data so the API can be used immediately in Postman.

-- =========================
-- Categories
-- =========================
INSERT INTO categories (id, name) VALUES
                                      (1, 'Starters'),
                                      (2, 'Mains'),
                                      (3, 'Desserts'),
                                      (4, 'Drinks')
    ON DUPLICATE KEY UPDATE name = VALUES(name);

-- =========================
-- Dining tables
-- =========================
INSERT INTO dining_tables (id, label) VALUES
                                          (1, 'T1'),
                                          (2, 'T2'),
                                          (3, 'T3'),
                                          (4, 'T4'),
                                          (5, 'T5')
    ON DUPLICATE KEY UPDATE label = VALUES(label);

-- =========================
-- Products
-- NOTE: price is in cents
-- =========================
INSERT INTO products (id, name, description, price_cents, active, category_id) VALUES
                                                                                   (101, 'Patatas Bravas', 'Spicy potatoes with sauce', 550, TRUE, 1),
                                                                                   (102, 'Croquetas', 'Classic croquettes', 650, TRUE, 1),

                                                                                   (201, 'Hamburger', 'Beef burger with fries', 1090, TRUE, 2),
                                                                                   (202, 'Chicken Salad', 'Grilled chicken + salad', 990, TRUE, 2),

                                                                                   (301, 'Cheesecake', 'Homemade cheesecake', 590, TRUE, 3),

                                                                                   (401, 'Water', 'Still water', 200, TRUE, 4),
                                                                                   (402, 'Coke', '330ml can', 250, TRUE, 4)

    ON DUPLICATE KEY UPDATE
                         name = VALUES(name),
                         description = VALUES(description),
                         price_cents = VALUES(price_cents),
                         active = VALUES(active),
                         category_id = VALUES(category_id);
