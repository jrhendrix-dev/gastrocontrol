CREATE TABLE account_tokens (
                                id BIGINT NOT NULL AUTO_INCREMENT,
                                user_id BIGINT NOT NULL,
                                type VARCHAR(32) NOT NULL,
                                token_hash VARCHAR(255) NOT NULL,
                                expires_at TIMESTAMP NOT NULL,
                                used_at TIMESTAMP NULL,
                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                requested_ip VARCHAR(64) NULL,
                                user_agent VARCHAR(255) NULL,
                                PRIMARY KEY (id),
                                CONSTRAINT fk_account_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                CONSTRAINT uq_account_tokens_token_hash UNIQUE (token_hash),
                                INDEX idx_account_tokens_user_type (user_id, type),
                                INDEX idx_account_tokens_expires_at (expires_at),
                                INDEX idx_account_tokens_used_at (used_at)
);
