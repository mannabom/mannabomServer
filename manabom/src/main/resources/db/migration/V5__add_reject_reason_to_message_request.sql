ALTER TABLE message_request
ADD COLUMN IF NOT EXISTS reject_reason VARCHAR(255);
