CREATE TABLE gifticon_product (
    gifticon_product_id BIGSERIAL PRIMARY KEY,
    template_trace_id BIGINT NOT NULL UNIQUE,
    template_name VARCHAR(200) NOT NULL,
    start_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    end_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    order_template_status VARCHAR(30) NOT NULL,
    budget_type VARCHAR(30),
    gift_sent_count BIGINT NOT NULL DEFAULT 0,
    bm_sender_name VARCHAR(100),
    mc_image_url VARCHAR(2048),
    mc_text TEXT,
    item_type VARCHAR(30) NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    brand_name VARCHAR(100) NOT NULL,
    product_image_url VARCHAR(2048),
    product_thumb_image_url VARCHAR(2048),
    brand_image_url VARCHAR(2048),
    product_price INTEGER NOT NULL CHECK (product_price >= 0),
    ting_price INTEGER NOT NULL CHECK (ting_price >= 0),
    ting_price_manually_set BOOLEAN NOT NULL DEFAULT FALSE,
    available BOOLEAN NOT NULL DEFAULT FALSE,
    last_synced_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_gifticon_product_available_price
    ON gifticon_product (available, product_price, gifticon_product_id);

CREATE INDEX idx_gifticon_product_available_period
    ON gifticon_product (available, start_at, end_at);

CREATE INDEX idx_gifticon_product_available_brand_cursor
    ON gifticon_product (available, brand_name, gifticon_product_id);
