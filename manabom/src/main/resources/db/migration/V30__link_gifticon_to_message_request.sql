ALTER TABLE message_request
    ADD COLUMN gifticon_product_id BIGINT;

ALTER TABLE message_request
    ADD CONSTRAINT fk_message_request_gifticon_product
        FOREIGN KEY (gifticon_product_id)
        REFERENCES gifticon_product (gifticon_product_id);

CREATE INDEX idx_message_request_gifticon_product
    ON message_request (gifticon_product_id);
