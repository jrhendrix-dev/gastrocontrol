CREATE TABLE payments (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          order_id BIGINT NOT NULL,
                          provider VARCHAR(30) NOT NULL,
                          status VARCHAR(30) NOT NULL,
                          amount_cents INT NOT NULL,
                          currency VARCHAR(10) NOT NULL,
                          checkout_session_id VARCHAR(120) NULL,
                          payment_intent_id VARCHAR(120) NULL,
                          created_at TIMESTAMP NOT NULL,
                          updated_at TIMESTAMP NULL,
                          version INT NOT NULL DEFAULT 0,

                          CONSTRAINT fk_payments_order
                              FOREIGN KEY (order_id) REFERENCES orders(id),

                          CONSTRAINT uk_payments_order_id UNIQUE (order_id),
                          CONSTRAINT uk_payments_checkout_session_id UNIQUE (checkout_session_id)
);
