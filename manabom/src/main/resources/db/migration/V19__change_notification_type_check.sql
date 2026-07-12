ALTER TABLE notification
DROP CONSTRAINT IF EXISTS notification_type_check;

ALTER TABLE notification
ALTER COLUMN type TYPE VARCHAR(50)
USING CASE type
    WHEN 0 THEN 'CONNECTED'
    WHEN 1 THEN 'MATCHING_STATUS'
    WHEN 2 THEN 'MATCH_FOUND'
    WHEN 3 THEN 'MATCHING_FAILED'
    WHEN 4 THEN 'DECISION_RESULT'
    WHEN 5 THEN 'MATCHING_COMPLETED'
    WHEN 6 THEN 'NEW_CHAT_MESSAGE'
    WHEN 7 THEN 'PHOTO_REQUEST_RECEIVED'
    WHEN 8 THEN 'PHOTO_REQUEST_ACCEPTED'
    WHEN 9 THEN 'PHOTO_REQUEST_REJECTED'
END;

ALTER TABLE notification
    ADD CONSTRAINT notification_type_check
        CHECK (
            type IN (
                     'CONNECTED',
                     'MATCHING_STATUS',
                     'MATCH_FOUND',
                     'MATCHING_FAILED',
                     'DECISION_RESULT',
                     'MATCHING_COMPLETED',
                     'NEW_CHAT_MESSAGE',
                     'PHOTO_REQUEST_RECEIVED',
                     'PHOTO_REQUEST_ACCEPTED',
                     'PHOTO_REQUEST_REJECTED'
                )
            );