ALTER TABLE gifticon_payment
    ADD COLUMN purpose VARCHAR(30) NOT NULL DEFAULT 'MESSAGE_REQUEST',
    ADD COLUMN chat_room_id BIGINT,
    ADD COLUMN receiver_user_id BIGINT,
    ADD COLUMN chat_message_id BIGINT;

ALTER TABLE gifticon_payment
    ALTER COLUMN target_profile_id DROP NOT NULL,
    ALTER COLUMN message_source DROP NOT NULL,
    DROP CONSTRAINT ck_gifticon_payment_message_source,
    ADD CONSTRAINT ck_gifticon_payment_message_source
        CHECK (message_source IS NULL OR message_source IN (
            'PROFILE_MATCH',
            'LOVE_VIEW_MATCH'
        )),
    ADD CONSTRAINT ck_gifticon_payment_purpose
        CHECK (purpose IN ('MESSAGE_REQUEST', 'CHAT')),
    ADD CONSTRAINT ck_gifticon_payment_intent
        CHECK (
            (purpose = 'MESSAGE_REQUEST'
                AND target_profile_id IS NOT NULL
                AND message_source IS NOT NULL
                AND chat_room_id IS NULL
                AND receiver_user_id IS NULL)
            OR
            (purpose = 'CHAT'
                AND target_profile_id IS NULL
                AND message_source IS NULL
                AND chat_room_id IS NOT NULL
                AND receiver_user_id IS NOT NULL)
        ),
    ADD CONSTRAINT fk_gifticon_payment_chat_room
        FOREIGN KEY (chat_room_id) REFERENCES chat_rooms (id),
    ADD CONSTRAINT fk_gifticon_payment_receiver_user
        FOREIGN KEY (receiver_user_id) REFERENCES users (user_id),
    ADD CONSTRAINT fk_gifticon_payment_chat_message
        FOREIGN KEY (chat_message_id) REFERENCES chat_messages (id),
    ADD CONSTRAINT uk_gifticon_payment_chat_message UNIQUE (chat_message_id);

CREATE INDEX idx_gifticon_payment_chat_room
    ON gifticon_payment (chat_room_id, created_at DESC)
    WHERE purpose = 'CHAT';

ALTER TABLE gifticon_order
    ADD COLUMN gifticon_payment_id BIGINT;

UPDATE gifticon_order gift_order
SET gifticon_payment_id = payment.gifticon_payment_id
FROM gifticon_payment payment
WHERE payment.message_request_id = gift_order.message_request_id;

ALTER TABLE gifticon_order
    ALTER COLUMN message_request_id DROP NOT NULL,
    ADD CONSTRAINT fk_gifticon_order_payment
        FOREIGN KEY (gifticon_payment_id)
        REFERENCES gifticon_payment (gifticon_payment_id),
    ADD CONSTRAINT uk_gifticon_order_payment UNIQUE (gifticon_payment_id);
