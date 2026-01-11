ALTER TABLE account_tokens
    ADD COLUMN new_email VARCHAR(255) NULL AFTER user_agent;
