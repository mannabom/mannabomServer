-- 결제 공통 원장(payment) + 스토어 인앱결제 상세(store_payment) + 팅 상품/지급(ting_product, ting_purchase)
-- 기프티콘(토스) 결제는 이번에 이관하지 않는다. provider/purpose 값만 예약한다.

CREATE TABLE payment (
    payment_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    amount NUMERIC(18, 6),
    currency VARCHAR(10),
    environment VARCHAR(20),
    status VARCHAR(30) NOT NULL,
    paid_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    refunded_at TIMESTAMPTZ,
    refund_reason VARCHAR(200),
    failure_reason VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_payment_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT ck_payment_provider CHECK (provider IN ('APPLE_IAP', 'GOOGLE_IAP', 'TOSS')),
    CONSTRAINT ck_payment_purpose CHECK (purpose IN ('TING_CHARGE', 'GIFTICON')),
    CONSTRAINT ck_payment_amount CHECK (amount IS NULL OR amount >= 0),
    CONSTRAINT ck_payment_environment
        CHECK (environment IS NULL OR environment IN ('PRODUCTION', 'SANDBOX')),
    CONSTRAINT ck_payment_status CHECK (status IN (
        'READY',
        'VERIFYING',
        'PAID',
        'FAILED',
        'REFUND_PENDING',
        'REFUND_PROCESSING',
        'REFUND_FAILED',
        'REFUNDED'
    ))
);

CREATE INDEX idx_payment_user_created_at ON payment (user_id, created_at DESC);
CREATE INDEX idx_payment_provider_status ON payment (provider, status);

-- Apple App Store / Google Play 인앱결제 상세. payment 와 1:1 (PK 공유)
CREATE TABLE store_payment (
    payment_id BIGINT PRIMARY KEY,
    store VARCHAR(20) NOT NULL,
    store_product_id VARCHAR(100) NOT NULL,
    -- 클라이언트가 전달한 구매 식별자(멱등 키). Apple: transactionId, Google: purchaseToken
    purchase_key VARCHAR(1000) NOT NULL,
    -- 스토어 검증 후 확정되는 거래 번호. Apple: transactionId, Google: orderId(GPA.xxxx)
    transaction_id VARCHAR(200),
    -- Apple originalTransactionId. Google 은 NULL
    original_transaction_id VARCHAR(200),
    quantity INTEGER NOT NULL DEFAULT 1,
    -- Apple appAccountToken / Google obfuscatedExternalAccountId (구매자 확인용)
    account_token VARCHAR(100),
    purchased_at TIMESTAMPTZ,
    -- Google 소모성 상품 consume 처리 상태. Apple 은 클라이언트가 finish() 하므로 NOT_REQUIRED
    consume_status VARCHAR(30) NOT NULL,
    consume_attempt_count INTEGER NOT NULL DEFAULT 0,
    last_consume_attempt_at TIMESTAMPTZ,
    consumed_at TIMESTAMPTZ,
    payload TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_store_payment_payment FOREIGN KEY (payment_id) REFERENCES payment (payment_id),
    CONSTRAINT uk_store_payment_purchase_key UNIQUE (store, purchase_key),
    CONSTRAINT ck_store_payment_store CHECK (store IN ('APPLE', 'GOOGLE')),
    CONSTRAINT ck_store_payment_quantity CHECK (quantity > 0),
    CONSTRAINT ck_store_payment_consume_attempt_count CHECK (consume_attempt_count >= 0),
    CONSTRAINT ck_store_payment_consume_status CHECK (consume_status IN (
        'NOT_REQUIRED',
        'PENDING',
        'PROCESSING',
        'DONE',
        'FAILED'
    ))
);

CREATE UNIQUE INDEX uk_store_payment_transaction
    ON store_payment (store, transaction_id)
    WHERE transaction_id IS NOT NULL;

CREATE INDEX idx_store_payment_consume_retry
    ON store_payment (consume_status, consume_attempt_count, last_consume_attempt_at)
    WHERE consume_status IN ('PENDING', 'PROCESSING', 'FAILED');

-- 팅 상품 카탈로그
CREATE TABLE ting_product (
    ting_product_id BIGSERIAL PRIMARY KEY,
    product_code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    ting_amount INTEGER NOT NULL,
    bonus_event_ting INTEGER NOT NULL DEFAULT 0,
    display_price_krw INTEGER NOT NULL,
    apple_product_id VARCHAR(100),
    google_product_id VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_ting_product_code UNIQUE (product_code),
    CONSTRAINT uk_ting_product_apple_product_id UNIQUE (apple_product_id),
    CONSTRAINT uk_ting_product_google_product_id UNIQUE (google_product_id),
    CONSTRAINT ck_ting_product_ting_amount CHECK (ting_amount > 0),
    CONSTRAINT ck_ting_product_bonus_event_ting CHECK (bonus_event_ting >= 0),
    CONSTRAINT ck_ting_product_display_price CHECK (display_price_krw >= 0)
);

-- 팅 지급 상세. payment 와 1:1 (PK 공유)
CREATE TABLE ting_purchase (
    payment_id BIGINT PRIMARY KEY,
    ting_product_id BIGINT NOT NULL,
    -- 구매 당시 상품 스냅샷(1개당)
    ting_amount INTEGER NOT NULL,
    bonus_event_ting INTEGER NOT NULL DEFAULT 0,
    quantity INTEGER NOT NULL DEFAULT 1,
    -- 실제 지갑에 반영된 수량
    credited_ting INTEGER NOT NULL DEFAULT 0,
    credited_event_ting INTEGER NOT NULL DEFAULT 0,
    credited_at TIMESTAMPTZ,
    -- 환불 시 실제 회수한 수량(잔액 부족 시 지급량보다 적을 수 있음)
    clawed_back_ting INTEGER NOT NULL DEFAULT 0,
    clawed_back_event_ting INTEGER NOT NULL DEFAULT 0,
    clawed_back_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ting_purchase_payment FOREIGN KEY (payment_id) REFERENCES payment (payment_id),
    CONSTRAINT fk_ting_purchase_product
        FOREIGN KEY (ting_product_id) REFERENCES ting_product (ting_product_id),
    CONSTRAINT ck_ting_purchase_quantity CHECK (quantity > 0),
    CONSTRAINT ck_ting_purchase_ting_amount CHECK (ting_amount > 0),
    CONSTRAINT ck_ting_purchase_bonus_event_ting CHECK (bonus_event_ting >= 0),
    CONSTRAINT ck_ting_purchase_credited
        CHECK (credited_ting >= 0 AND credited_event_ting >= 0),
    CONSTRAINT ck_ting_purchase_clawed_back
        CHECK (clawed_back_ting >= 0 AND clawed_back_ting <= credited_ting
           AND clawed_back_event_ting >= 0 AND clawed_back_event_ting <= credited_event_ting)
);

CREATE INDEX idx_ting_purchase_product ON ting_purchase (ting_product_id);

-- 초기 상품 카탈로그. 스토어 상품 ID는 App Store Connect / Play Console 에 등록한 값과 일치해야 하며
-- 관리자 API(/api/admin/ting-products)로 수정할 수 있다.
INSERT INTO ting_product
    (product_code, name, ting_amount, bonus_event_ting, display_price_krw, apple_product_id, google_product_id, active, sort_order)
VALUES
    ('TING_30',  '팅 30개',  30,  0,  4400,  'com.mannabom.ting.30',  'ting_30',  TRUE, 1),
    ('TING_70',  '팅 70개',  70,  5,  9900,  'com.mannabom.ting.70',  'ting_70',  TRUE, 2),
    ('TING_150', '팅 150개', 150, 15, 19000, 'com.mannabom.ting.150', 'ting_150', TRUE, 3),
    ('TING_400', '팅 400개', 400, 50, 49000, 'com.mannabom.ting.400', 'ting_400', TRUE, 4);
