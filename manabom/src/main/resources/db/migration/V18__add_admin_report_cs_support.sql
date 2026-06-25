ALTER TABLE user_account_restrictions
    ADD COLUMN IF NOT EXISTS suspended_until TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_user_account_restrictions_status_until
    ON user_account_restrictions(status, suspended_until);
