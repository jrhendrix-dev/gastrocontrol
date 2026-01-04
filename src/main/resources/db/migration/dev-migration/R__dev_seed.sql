-- R__dev_seed.sql
-- Dev-only seed data. Executed only when SPRING_PROFILES_ACTIVE=dev
-- Depends on V2__seed_reference_data.sql for categories/products/tables.

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
     'CUSTOMER', 1, NOW(), 'Ana', 'Perez', '600111222')
ON DUPLICATE KEY UPDATE
                     -- keep these in sync in dev (optional)
                     role = VALUES(role),
                     active = VALUES(active),

                     -- backfill only if missing
                     first_name = CASE
                                      WHEN users.first_name IS NULL OR users.first_name = '' THEN VALUES(first_name)
                                      ELSE users.first_name
                         END,
                     last_name = CASE
                                     WHEN users.last_name IS NULL OR users.last_name = '' THEN VALUES(last_name)
                                     ELSE users.last_name
                         END,
                     phone = CASE
                                 WHEN users.phone IS NULL OR users.phone = '' THEN VALUES(phone)
                                 ELSE users.phone
                         END;

-- =========================
-- BACKFILL: any other users created via API
-- fill missing first_name/last_name/phone with "random-ish" deterministic values
-- =========================

UPDATE users u
    JOIN (
        SELECT
            id,
            -- pick from a small list using MOD on id (stable across runs)
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
-- SAMPLE ORDER (PENDING) FOR TABLE 1 + ITEMS
-- Uses products from V2: 201 Hamburger, 402 Coke
-- =========================

-- Create a pending order for table 1 if none exists
INSERT INTO orders (type, status, dining_table_id, total_cents, created_at, updated_at, closed_at, version)
SELECT
    'DINE_IN',
    'PENDING',
    1,
    0,
    NOW(),
    NOW(),
    NULL,
    0
WHERE NOT EXISTS (
    SELECT 1
    FROM orders o
    WHERE o.dining_table_id = 1 AND o.status = 'PENDING'
);

-- Add items to the latest pending order for table 1
-- 2x Hamburger (201) + 1x Coke (402)
INSERT INTO order_items (order_id, product_id, quantity, unit_price_cents)
SELECT o.id, 201, 2, 1090
FROM orders o
WHERE o.dining_table_id = 1 AND o.status = 'PENDING'
ORDER BY o.id DESC
LIMIT 1
ON DUPLICATE KEY UPDATE
                     quantity = VALUES(quantity),
                     unit_price_cents = VALUES(unit_price_cents);

INSERT INTO order_items (order_id, product_id, quantity, unit_price_cents)
SELECT o.id, 402, 1, 250
FROM orders o
WHERE o.dining_table_id = 1 AND o.status = 'PENDING'
ORDER BY o.id DESC
LIMIT 1
ON DUPLICATE KEY UPDATE
                     quantity = VALUES(quantity),
                     unit_price_cents = VALUES(unit_price_cents);


-- Recompute total_cents for that order
UPDATE orders o
    JOIN (
        SELECT oi.order_id, SUM(oi.quantity * oi.unit_price_cents) AS total
        FROM order_items oi
        GROUP BY oi.order_id
    ) t ON t.order_id = o.id
SET o.total_cents = t.total,
    o.updated_at = NOW()
WHERE o.dining_table_id = 1 AND o.status = 'PENDING';
