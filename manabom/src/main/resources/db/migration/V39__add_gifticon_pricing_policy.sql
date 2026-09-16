ALTER TABLE policy_config
    ADD COLUMN gifticon_markup_percent NUMERIC(7, 3),
    ADD CONSTRAINT ck_policy_config_gifticon_markup_percent
        CHECK (gifticon_markup_percent IS NULL OR gifticon_markup_percent >= 0);
