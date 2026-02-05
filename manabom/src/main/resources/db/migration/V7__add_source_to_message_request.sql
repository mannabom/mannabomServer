ALTER TABLE message_request
ADD COLUMN source VARCHAR(30);

UPDATE message_request
SET source = 'PROFILE_MATCH'
WHERE source IS NULL;

ALTER TABLE message_request
ALTER COLUMN source SET NOT NULL;