CREATE TABLE ting_transaction (
    ting_transaction_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    balance_type VARCHAR(20) NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    amount_delta INTEGER NOT NULL,
    balance_after INTEGER NOT NULL,
    reference_type VARCHAR(50),
    reference_id VARCHAR(100),
    idempotency_key VARCHAR(150),
    description VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_ting_transaction_balance_type
        CHECK (balance_type IN ('PAID', 'EVENT')),
    CONSTRAINT ck_ting_transaction_balance_after
        CHECK (balance_after >= 0),
    CONSTRAINT ck_ting_transaction_amount
        CHECK (
            amount_delta <> 0
            OR transaction_type = 'GIFTICON_CAPTURE'
        )
);

CREATE INDEX idx_ting_transaction_user_created_at
    ON ting_transaction (user_id, created_at DESC);

CREATE INDEX idx_ting_transaction_reference
    ON ting_transaction (reference_type, reference_id);

CREATE UNIQUE INDEX uk_ting_transaction_idempotency_key
    ON ting_transaction (idempotency_key)
    WHERE idempotency_key IS NOT NULL;
