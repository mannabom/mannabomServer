ALTER TABLE like_request
ADD COLUMN source VARCHAR(30);

UPDATE like_request
SET source = 'PROFILE_MATCH'
WHERE source IS NULL;

ALTER TABLE like_request
ALTER COLUMN source SET NOT NULL;