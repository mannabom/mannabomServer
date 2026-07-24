ALTER TABLE gifticon_order
    ADD COLUMN sender_nickname VARCHAR(255);

UPDATE gifticon_order gift_order
   SET sender_nickname = profile.nick_name
  FROM message_request message_request
  JOIN profile profile
    ON profile.user_id = message_request.from_user_id
 WHERE message_request.id = gift_order.message_request_id;

UPDATE gifticon_order
   SET sender_nickname = '만나봄 회원'
 WHERE sender_nickname IS NULL
    OR BTRIM(sender_nickname) = '';

ALTER TABLE gifticon_order
    ALTER COLUMN sender_nickname SET NOT NULL;
