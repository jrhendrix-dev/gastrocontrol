ALTER TABLE order_items
    ADD UNIQUE KEY uq_order_items_order_product (order_id, product_id);
