ALTER TABLE meeting_verification
    ADD COLUMN IF NOT EXISTS failure_notified_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_meeting_verification_failure_notification
    ON meeting_verification (expires_at)
    WHERE is_verified = FALSE AND failure_notified_at IS NULL;
