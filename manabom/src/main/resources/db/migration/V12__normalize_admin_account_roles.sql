ALTER TABLE admin_accounts
    RENAME COLUMN email TO login_id;

ALTER TABLE admin_accounts
    RENAME CONSTRAINT uk_admin_accounts_email TO uk_admin_accounts_login_id;

ALTER TABLE admin_accounts
    DROP COLUMN role;
