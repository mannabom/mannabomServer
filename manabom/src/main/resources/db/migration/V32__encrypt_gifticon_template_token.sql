ALTER TABLE gifticon_product
    DROP CONSTRAINT uk_gifticon_product_template_token;

ALTER TABLE gifticon_product
    RENAME COLUMN template_token TO encrypted_template_token;

ALTER TABLE gifticon_product
    ALTER COLUMN encrypted_template_token TYPE VARCHAR(1024);
