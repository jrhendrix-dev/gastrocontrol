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
     'ADMIN', 1, NOW(), 'Jonathan', 'Hendrix', '564123567'),
    ('manager@gastro.local',
     '$2b$12$/oIg33gYYwzS02/.GRZF6.5NyYW.Xdqoef5M5laEjC11XdEOeEQ/G',
     'MANAGER', 1, NOW(), 'Juan', 'López', '123456789'),
    ('staff@gastro.local',
     '$2b$12$F1St7LtgqmHMsREXI6oEo.jDu6tcM5HjgcU3fvMNojZ1.o/j5vkTq',
     'STAFF', 1, NOW(), 'Ernesto', 'Gómez', '987654321'),
    ('customer@gastro.local',
     '$2b$12$EIB.hrUar3nFL66VsngcbeAUrMQMvbdyHzj33QJXOYmm6kkVflh4O',
     'CUSTOMER', 1, NOW(), 'Ana', 'Pérez', '600111222');

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
-- CATEGORÍAS
-- =========================
INSERT INTO categories (name)
VALUES
    ('Hamburguesas'),
    ('Entrantes'),
    ('Ensaladas'),
    ('Pizzas'),
    ('Postres'),
    ('Bebidas');

-- =========================
-- PRODUCTOS (15) con imágenes
-- =========================
INSERT INTO products (name, description, price_cents, active, category_id, image_url)
VALUES
    -- Hamburguesas
    ('Hamburguesa Clásica',
     'Hamburguesa de ternera con lechuga, tomate y salsa de la casa.',
     1090, 1,
     (SELECT id FROM categories WHERE name='Hamburguesas' LIMIT 1),
     '/gastrocontrol/uploads/products/seed-hamburguesa-clasica.jpg'),

    ('Hamburguesa con Queso',
     'Hamburguesa de ternera con queso cheddar.',
     1190, 1,
     (SELECT id FROM categories WHERE name='Hamburguesas' LIMIT 1),
     '/gastrocontrol/uploads/products/seed-hamburguesa-queso.jpg'),

    ('Doble Hamburguesa',
     'Doble medallón de ternera con todos los ingredientes.',
     1490, 1,
     (SELECT id FROM categories WHERE name='Hamburguesas' LIMIT 1),
     '/gastrocontrol/uploads/products/seed-doble-hamburguesa.jpg'),

    ('Hamburguesa de Pollo',
     'Pollo crujiente con mayonesa y lechuga.',
     1290, 1,
     (SELECT id FROM categories WHERE name='Hamburguesas' LIMIT 1),
     '/gastrocontrol/uploads/products/seed-hamburguesa-pollo.jpg'),

    -- Entrantes
    ('Patatas Fritas',
     'Patatas crujientes con sal.',
     350, 1,
     (SELECT id FROM categories WHERE name='Entrantes' LIMIT 1),
     '/gastrocontrol/uploads/products/seed-patatas-fritas.jpg'),

    ('Aros de Cebolla',
     'Aros de cebolla dorados con salsa.',
     450, 1,
     (SELECT id FROM categories WHERE name='Entrantes' LIMIT 1),
     '/gastrocontrol/uploads/products/seed-aros-cebolla.jpg'),

    ('Nuggets de Pollo',
     '6 nuggets de pollo con salsa a elegir.',
     590, 1,
     (SELECT id FROM categories WHERE name='Entrantes' LIMIT 1),
     '/gastrocontrol/uploads/products/seed-nuggets-pollo.jpg'),

    -- Ensaladas
    ('Ensalada César',
     'Lechuga romana, parmesano, picatostes y aliño César.',
     890, 1,
     (SELECT id FROM categories WHERE name='Ensaladas' LIMIT 1),
     '/gastrocontrol/uploads/products/seed-ensalada-cesar.jpg'),

    ('Ensalada Griega',
     'Tomate, pepino, feta y aceitunas.',
     850, 1,
     (SELECT id FROM categories WHERE name='Ensaladas' LIMIT 1),
     '/gastrocontrol/uploads/products/seed-ensalada-griega.jpg'),

    -- Pizzas
    ('Pizza Margherita',
     'Tomate, mozzarella y albahaca fresca.',
     1090, 1,
     (SELECT id FROM categories WHERE name='Pizzas' LIMIT 1),
     '/gastrocontrol/uploads/products/seed-pizza-margherita.jpg'),

    ('Pizza Pepperoni',
     'Pepperoni y mozzarella.',
     1290, 1,
     (SELECT id FROM categories WHERE name='Pizzas' LIMIT 1),
     '/gastrocontrol/uploads/products/seed-pizza-pepperoni.jpg'),

    -- Postres
    ('Brownie de Chocolate',
     'Brownie templado con helado de vainilla.',
     490, 1,
     (SELECT id FROM categories WHERE name='Postres' LIMIT 1),
     '/gastrocontrol/uploads/products/seed-brownie-chocolate.jpg'),

    ('Tarta de Queso',
     'Porción de tarta de queso cremosa.',
     520, 1,
     (SELECT id FROM categories WHERE name='Postres' LIMIT 1),
     '/gastrocontrol/uploads/products/seed-tarta-queso.jpg'),

    -- Bebidas
    ('Coca-Cola',
     'Lata 330ml.',
     250, 1,
     (SELECT id FROM categories WHERE name='Bebidas' LIMIT 1),
     '/gastrocontrol/uploads/products/seed-coca-cola.jpg'),

    ('Agua Mineral',
     'Botella 500ml.',
     180, 1,
     (SELECT id FROM categories WHERE name='Bebidas' LIMIT 1),
     '/gastrocontrol/uploads/products/seed-agua-mineral.jpg');

-- =========================
-- PEDIDO DE EJEMPLO (PENDIENTE) EN MESA 1
-- =========================
INSERT INTO orders (type, status, dining_table_id, created_at)
VALUES ('DINE_IN', 'PENDING', 1, NOW());

INSERT INTO order_items (order_id, product_id, quantity, unit_price_cents)
VALUES (
           (SELECT id FROM orders WHERE dining_table_id=1 AND status='PENDING' ORDER BY id DESC LIMIT 1),
           (SELECT id FROM products WHERE name='Hamburguesa Clásica' ORDER BY id ASC LIMIT 1),
           2,
           (SELECT price_cents FROM products WHERE name='Hamburguesa Clásica' ORDER BY id ASC LIMIT 1)
       );

INSERT INTO order_items (order_id, product_id, quantity, unit_price_cents)
VALUES (
           (SELECT id FROM orders WHERE dining_table_id=1 AND status='PENDING' ORDER BY id DESC LIMIT 1),
           (SELECT id FROM products WHERE name='Coca-Cola' ORDER BY id ASC LIMIT 1),
           1,
           (SELECT price_cents FROM products WHERE name='Coca-Cola' ORDER BY id ASC LIMIT 1)
       );

UPDATE orders o
    JOIN (
        SELECT oi.order_id, SUM(oi.quantity * oi.unit_price_cents) AS total
        FROM order_items oi
        GROUP BY oi.order_id
    ) t ON t.order_id = o.id
SET o.total_cents = t.total,
    o.updated_at  = NOW()
WHERE o.dining_table_id = 1 AND o.status = 'PENDING';