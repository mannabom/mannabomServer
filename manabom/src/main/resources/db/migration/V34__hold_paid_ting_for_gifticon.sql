ALTER TABLE message_request
    ADD COLUMN held_gift_ting INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN gift_payment_status VARCHAR(30);

ALTER TABLE message_request
    ADD CONSTRAINT ck_message_request_held_gift_ting
        CHECK (held_gift_ting >= 0),
    ADD CONSTRAINT ck_message_request_gift_payment_status
        CHECK (
            (gifticon_product_id IS NULL
                AND held_gift_ting = 0
                AND gift_payment_status IS NULL)
            OR
            (gifticon_product_id IS NOT NULL
                AND held_gift_ting > 0
                AND gift_payment_status IN ('HELD', 'CAPTURED', 'RELEASED'))
        );
