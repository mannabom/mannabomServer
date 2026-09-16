ALTER TABLE gifticon_payment
    ADD COLUMN target_profile_id BIGINT NOT NULL,
    ADD COLUMN message VARCHAR(200),
    ADD COLUMN message_source VARCHAR(30) NOT NULL,
    ADD COLUMN message_creation_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN message_creation_attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN last_message_creation_attempt_at TIMESTAMPTZ,
    ADD COLUMN message_creation_failure_reason VARCHAR(1000),
    ADD CONSTRAINT fk_gifticon_payment_target_profile
        FOREIGN KEY (target_profile_id) REFERENCES profile (profile_id),
    ADD CONSTRAINT ck_gifticon_payment_message_source
        CHECK (message_source IN ('PROFILE_MATCH', 'LOVE_VIEW_MATCH')),
    ADD CONSTRAINT ck_gifticon_payment_message_creation_status
        CHECK (message_creation_status IN (
            'PENDING',
            'PROCESSING',
            'RETRY_PENDING',
            'CREATED',
            'FAILED'
        )),
    ADD CONSTRAINT ck_gifticon_payment_message_creation_attempt_count
        CHECK (message_creation_attempt_count >= 0);

CREATE INDEX idx_gifticon_payment_target_profile
    ON gifticon_payment (target_profile_id);
