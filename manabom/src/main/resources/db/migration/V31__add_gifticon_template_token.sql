ALTER TABLE gifticon_product
    ADD COLUMN template_token VARCHAR(512);

ALTER TABLE gifticon_product
    ADD CONSTRAINT uk_gifticon_product_template_token
        UNIQUE (template_token);
