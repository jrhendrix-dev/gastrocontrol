CREATE TABLE refresh_tokens (
                                id BIGINT PRIMARY KEY AUTO_INCREMENT,

                                user_id BIGINT NOT NULL,
                                token_hash VARCHAR(255) NOT NULL,
                                expires_at TIMESTAMP NOT NULL,

                                revoked_at TIMESTAMP NULL,
                                replaced_by_token_id BIGINT NULL,

                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                created_by_ip VARCHAR(45) NULL,
                                user_agent VARCHAR(255) NULL,

                                CONSTRAINT fk_refresh_tokens_user
                                    FOREIGN KEY (user_id) REFERENCES users(id),

                                CONSTRAINT fk_refresh_tokens_replaced_by
                                    FOREIGN KEY (replaced_by_token_id) REFERENCES refresh_tokens(id),

                                UNIQUE KEY uq_refresh_tokens_token_hash (token_hash),
                                INDEX ix_refresh_tokens_user_id (user_id),
                                INDEX ix_refresh_tokens_expires_at (expires_at)
);
