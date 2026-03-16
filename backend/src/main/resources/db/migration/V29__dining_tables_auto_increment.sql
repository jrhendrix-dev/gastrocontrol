-- Drop FK that references dining_tables.id
ALTER TABLE orders DROP FOREIGN KEY fk_orders_dining_table;

-- Now we can add AUTO_INCREMENT
ALTER TABLE dining_tables MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;

-- Re-add the FK
ALTER TABLE orders
    ADD CONSTRAINT fk_orders_dining_table
        FOREIGN KEY (dining_table_id) REFERENCES dining_tables(id);