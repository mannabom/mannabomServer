-- Extend notification.type check constraint for newly added SseEventName values.
-- Current SseEventName ordinal range: 0..9

ALTER TABLE notification
DROP CONSTRAINT IF EXISTS notification_type_check;

ALTER TABLE notification
    ADD CONSTRAINT notification_type_check
        CHECK (type >= 0 AND type <= 9);