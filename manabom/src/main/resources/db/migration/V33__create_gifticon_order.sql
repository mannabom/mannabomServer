CREATE TABLE gifticon_order (
    gifticon_order_id BIGSERIAL PRIMARY KEY,
    message_request_id BIGINT NOT NULL UNIQUE,
    encrypted_template_token VARCHAR(1024) NOT NULL,
    receiver_phone VARCHAR(30) NOT NULL,
    receiver_name VARCHAR(100) NOT NULL,
    external_key VARCHAR(70) NOT NULL UNIQUE,
    external_order_id VARCHAR(70) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMPTZ,
    requested_at TIMESTAMPTZ,
    failure_reason VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_gifticon_order_message_request
        FOREIGN KEY (message_request_id)
        REFERENCES message_request (id),
    CONSTRAINT ck_gifticon_order_attempt_count
        CHECK (attempt_count >= 0)
);

CREATE INDEX idx_gifticon_order_retry
    ON gifticon_order (status, attempt_count, created_at);
