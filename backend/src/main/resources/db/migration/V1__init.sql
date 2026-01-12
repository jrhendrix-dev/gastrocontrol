-- Categories
CREATE TABLE categories (
                            id BIGINT NOT NULL,
                            name VARCHAR(120) NOT NULL,
                            PRIMARY KEY (id)
) ENGINE=InnoDB;

-- Dining tables
CREATE TABLE dining_tables (
                               id BIGINT NOT NULL,
                               label VARCHAR(50) NOT NULL,
                               PRIMARY KEY (id)
) ENGINE=InnoDB;

-- Products
CREATE TABLE products (
                          id BIGINT NOT NULL,
                          name VARCHAR(160) NOT NULL,
                          description TEXT NULL,
                          price_cents INT NOT NULL,
                          active BOOLEAN NOT NULL,
                          category_id BIGINT NULL,
                          PRIMARY KEY (id),
                          CONSTRAINT fk_products_category
                              FOREIGN KEY (category_id) REFERENCES categories(id)
) ENGINE=InnoDB;

CREATE INDEX idx_products_category_id ON products(category_id);

-- Orders
CREATE TABLE orders (
                        id BIGINT NOT NULL AUTO_INCREMENT,
                        type VARCHAR(30) NOT NULL,
                        status VARCHAR(30) NOT NULL,
                        dining_table_id BIGINT NULL,
                        created_at TIMESTAMP(6) NOT NULL,
                        PRIMARY KEY (id),
                        CONSTRAINT fk_orders_dining_table
                            FOREIGN KEY (dining_table_id) REFERENCES dining_tables(id)
) ENGINE=InnoDB;

CREATE INDEX idx_orders_type_table_status_created
    ON orders(type, dining_table_id, status, created_at);

-- Order items
CREATE TABLE order_items (
                             id BIGINT NOT NULL AUTO_INCREMENT,
                             order_id BIGINT NOT NULL,
                             product_id BIGINT NOT NULL,
                             quantity INT NOT NULL,
                             unit_price_cents INT NOT NULL,
                             PRIMARY KEY (id),
                             CONSTRAINT fk_order_items_order
                                 FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
                             CONSTRAINT fk_order_items_product
                                 FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB;

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
