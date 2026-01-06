-- R__dev_seed.sql
-- Dev-only seed data. Executed only when SPRING_PROFILES_ACTIVE=dev
-- Assumes R__dev_reset.sql has just truncated dev tables.

-- =========================
-- USERS (ADMIN / MANAGER / STAFF / CUSTOMER)
-- password for all: GastroControl1726!
-- =========================
INSERT INTO users (email, password, role, active, created_at, first_name, last_name, phone)
VALUES
    ('admin@gastro.local',
     '$2b$12$3jae2iKs5BcKJa/m5TkLx.4b6w2ELF1irYFYXbJutSbkCBW5uTTdy',
     'ADMIN', 1, NOW(), 'John', 'Doe', '564123567'),
    ('manager@gastro.local',
     '$2b$12$/oIg33gYYwzS02/.GRZF6.5NyYW.Xdqoef5M5laEjC11XdEOeEQ/G',
     'MANAGER', 1, NOW(), 'Juan', 'Lopez', '123456789'),
    ('staff@gastro.local',
     '$2b$12$F1St7LtgqmHMsREXI6oEo.jDu6tcM5HjgcU3fvMNojZ1.o/j5vkTq',
     'STAFF', 1, NOW(), 'Ernesto', 'Gomez', '987654321'),
    ('customer@gastro.local',
     '$2b$12$EIB.hrUar3nFL66VsngcbeAUrMQMvbdyHzj33QJXOYmm6kkVflh4O',
     'CUSTOMER', 1, NOW(), 'Ana', 'Perez', '600111222');

-- =========================
-- BACKFILL (optional) - keep your existing block if you want
-- =========================

UPDATE users u
    JOIN (
        SELECT
            id,
            ELT(MOD(id, 8) + 1, 'Alex', 'Sam', 'Taylor', 'Chris', 'Jordan', 'Casey', 'Morgan', 'Jamie') AS fn,
            ELT(MOD(id, 8) + 1, 'Smith', 'Johnson', 'Brown', 'Davis', 'Miller', 'Wilson', 'Moore', 'Anderson') AS ln,
            CONCAT('6', LPAD(MOD(id * 7919, 100000000), 8, '0')) AS ph
        FROM users
    ) x ON x.id = u.id
SET
    u.first_name = CASE WHEN u.first_name IS NULL OR u.first_name = '' THEN x.fn ELSE u.first_name END,
    u.last_name  = CASE WHEN u.last_name  IS NULL OR u.last_name  = '' THEN x.ln ELSE u.last_name  END,
    u.phone      = CASE WHEN u.phone      IS NULL OR u.phone      = '' THEN x.ph ELSE u.phone      END
WHERE
    (u.first_name IS NULL OR u.first_name = '')
   OR (u.last_name  IS NULL OR u.last_name  = '')
   OR (u.phone      IS NULL OR u.phone      = '');

-- =========================
-- CATEGORIES
-- =========================
INSERT INTO categories (name)
VALUES
    ('Burgers'),
    ('Starters'),
    ('Salads'),
    ('Pizzas'),
    ('Desserts'),
    ('Drinks');

-- =========================
-- PRODUCTS (15) - ids will become 1..15 (fresh DB / after TRUNCATE)
-- =========================
INSERT INTO products (name, description, price_cents, active, category_id)
VALUES
    ('Hamburger', 'Classic beef burger with lettuce, tomato and house sauce.', 1090, 1,
     (SELECT id FROM categories WHERE name='Burgers' LIMIT 1)),
    ('Cheeseburger', 'Beef burger with cheddar cheese.', 1190, 1,
     (SELECT id FROM categories WHERE name='Burgers' LIMIT 1)),
    ('Double Burger', 'Double beef patty burger.', 1490, 1,
     (SELECT id FROM categories WHERE name='Burgers' LIMIT 1)),
    ('Chicken Burger', 'Crispy chicken burger with mayo.', 1290, 1,
     (SELECT id FROM categories WHERE name='Burgers' LIMIT 1)),

    ('French Fries', 'Crispy fries with salt.', 350, 1,
     (SELECT id FROM categories WHERE name='Starters' LIMIT 1)),
    ('Onion Rings', 'Golden onion rings with dip.', 450, 1,
     (SELECT id FROM categories WHERE name='Starters' LIMIT 1)),
    ('Chicken Nuggets', '6pc nuggets with sauce.', 590, 1,
     (SELECT id FROM categories WHERE name='Starters' LIMIT 1)),

    ('Caesar Salad', 'Romaine, parmesan, croutons, Caesar dressing.', 890, 1,
     (SELECT id FROM categories WHERE name='Salads' LIMIT 1)),
    ('Greek Salad', 'Tomato, cucumber, feta, olives.', 850, 1,
     (SELECT id FROM categories WHERE name='Salads' LIMIT 1)),

    ('Margherita Pizza', 'Tomato, mozzarella, basil.', 1090, 1,
     (SELECT id FROM categories WHERE name='Pizzas' LIMIT 1)),
    ('Pepperoni Pizza', 'Pepperoni and mozzarella.', 1290, 1,
     (SELECT id FROM categories WHERE name='Pizzas' LIMIT 1)),

    ('Chocolate Brownie', 'Warm brownie.', 490, 1,
     (SELECT id FROM categories WHERE name='Desserts' LIMIT 1)),
    ('Cheesecake', 'Creamy cheesecake slice.', 520, 1,
     (SELECT id FROM categories WHERE name='Desserts' LIMIT 1)),

    ('Coke', '330ml can.', 250, 1,
     (SELECT id FROM categories WHERE name='Drinks' LIMIT 1)),
    ('Water', '500ml bottle.', 180, 1,
     (SELECT id FROM categories WHERE name='Drinks' LIMIT 1));

-- =========================
-- SAMPLE ORDER (PENDING) FOR TABLE 1 + ITEMS
-- =========================

INSERT INTO orders (type, status, dining_table_id, created_at)
VALUES ('DINE_IN', 'PENDING', 1, NOW());

-- 2x Hamburger
INSERT INTO order_items (order_id, product_id, quantity, unit_price_cents)
VALUES (
           (SELECT id FROM orders WHERE dining_table_id=1 AND status='PENDING' ORDER BY id DESC LIMIT 1),
           (SELECT id FROM products WHERE name='Hamburger' ORDER BY id ASC LIMIT 1),
           2,
           (SELECT price_cents FROM products WHERE name='Hamburger' ORDER BY id ASC LIMIT 1)
       );

-- 1x Coke
INSERT INTO order_items (order_id, product_id, quantity, unit_price_cents)
VALUES (
           (SELECT id FROM orders WHERE dining_table_id=1 AND status='PENDING' ORDER BY id DESC LIMIT 1),
           (SELECT id FROM products WHERE name='Coke' ORDER BY id ASC LIMIT 1),
           1,
           (SELECT price_cents FROM products WHERE name='Coke' ORDER BY id ASC LIMIT 1)
       );

UPDATE orders o
    JOIN (
        SELECT oi.order_id, SUM(oi.quantity * oi.unit_price_cents) AS total
        FROM order_items oi
        GROUP BY oi.order_id
    ) t ON t.order_id = o.id
SET o.total_cents = t.total,
    o.updated_at = NOW()
WHERE o.dining_table_id = 1 AND o.status = 'PENDING';
