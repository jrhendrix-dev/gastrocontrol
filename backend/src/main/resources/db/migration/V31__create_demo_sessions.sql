-- V31__create_demo_sessions.sql
-- Tracking table for active demo sessions in the default schema.
-- The cleanup job uses this to find and drop expired schemas.

CREATE TABLE IF NOT EXISTS demo_sessions (
                                             id         BIGINT       NOT NULL AUTO_INCREMENT,
                                             session_id VARCHAR(32)  NOT NULL UNIQUE,
    schema_name VARCHAR(64) NOT NULL UNIQUE,
    created_at DATETIME     NOT NULL,
    expires_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_demo_sessions_expires_at (expires_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;