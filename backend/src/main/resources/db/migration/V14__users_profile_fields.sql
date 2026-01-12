-- V14__users_profile_fields.sql

ALTER TABLE users
    ADD COLUMN first_name VARCHAR(80) NULL AFTER email,
    ADD COLUMN last_name  VARCHAR(120) NULL AFTER first_name,
    ADD COLUMN phone      VARCHAR(30) NULL AFTER last_name,
    ADD COLUMN updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at,
    ADD COLUMN last_login_at TIMESTAMP NULL AFTER updated_at;

CREATE INDEX idx_users_active ON users(active);
