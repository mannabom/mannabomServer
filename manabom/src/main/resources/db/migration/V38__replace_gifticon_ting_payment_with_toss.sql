ALTER TABLE gifticon_product
    ADD COLUMN sale_price INTEGER;

UPDATE gifticon_product
SET sale_price = (
    CEIL((product_price::NUMERIC * 1.10) / 100) * 100
)::INTEGER
WHERE sale_price IS NULL;

ALTER TABLE gifticon_product
    ALTER COLUMN sale_price SET NOT NULL,
    ADD CONSTRAINT ck_gifticon_product_sale_price
        CHECK (sale_price >= 0);

ALTER TABLE gifticon_product
    DROP COLUMN IF EXISTS ting_price,
    DROP COLUMN IF EXISTS ting_price_manually_set;

-- V32 이전의 평문 토큰은 애플리케이션이 시작되기 전에 제거한다.
-- 관리자가 토큰을 다시 등록하면 AES-GCM 암호문으로 저장된다.
UPDATE gifticon_product
SET encrypted_template_token = NULL
WHERE encrypted_template_token IS NOT NULL
  AND encrypted_template_token NOT LIKE 'v1:%';

-- 배포 전에 HELD 상태의 기존 요청은 없으므로 팅 결제 컬럼을 제거한다.
ALTER TABLE message_request
    DROP CONSTRAINT IF EXISTS ck_message_request_held_gift_ting,
    DROP CONSTRAINT IF EXISTS ck_message_request_gift_payment_status,
    DROP COLUMN IF EXISTS held_gift_ting,
    DROP COLUMN IF EXISTS gift_payment_status;

-- 0원 거래는 잔액 변화가 없는 과거 원장이므로 새 제약을 추가하기 전에 제거한다.
DELETE FROM ting_transaction
WHERE amount_delta = 0;

ALTER TABLE ting_transaction
    DROP CONSTRAINT IF EXISTS ck_ting_transaction_amount,
    ADD CONSTRAINT ck_ting_transaction_amount
        CHECK (amount_delta <> 0);

CREATE TABLE gifticon_payment (
    gifticon_payment_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    gifticon_product_id BIGINT NOT NULL,
    message_request_id BIGINT,
    order_id VARCHAR(64) NOT NULL,
    customer_key VARCHAR(64) NOT NULL,
    payment_key VARCHAR(200),
    amount INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    confirmation_started_at TIMESTAMPTZ,
    approved_at TIMESTAMPTZ,
    refund_attempt_count INTEGER NOT NULL DEFAULT 0,
    last_refund_attempt_at TIMESTAMPTZ,
    refunded_at TIMESTAMPTZ,
    failure_reason VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_gifticon_payment_order_id UNIQUE (order_id),
    CONSTRAINT uk_gifticon_payment_payment_key UNIQUE (payment_key),
    CONSTRAINT uk_gifticon_payment_message_request UNIQUE (message_request_id),
    CONSTRAINT fk_gifticon_payment_user
        FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_gifticon_payment_product
        FOREIGN KEY (gifticon_product_id)
            REFERENCES gifticon_product (gifticon_product_id),
    CONSTRAINT fk_gifticon_payment_message_request
        FOREIGN KEY (message_request_id) REFERENCES message_request (id),
    CONSTRAINT ck_gifticon_payment_amount CHECK (amount > 0),
    CONSTRAINT ck_gifticon_payment_refund_attempt_count
        CHECK (refund_attempt_count >= 0),
    CONSTRAINT ck_gifticon_payment_status CHECK (status IN (
        'READY',
        'CONFIRMING',
        'PAID',
        'REFUND_PENDING',
        'REFUND_PROCESSING',
        'REFUNDED',
        'REFUND_FAILED'
    ))
);

CREATE INDEX idx_gifticon_payment_refund_retry
    ON gifticon_payment (status, refund_attempt_count, last_refund_attempt_at);

CREATE INDEX idx_gifticon_payment_unused_paid
    ON gifticon_payment (approved_at, gifticon_payment_id)
    WHERE status = 'PAID' AND message_request_id IS NULL;
