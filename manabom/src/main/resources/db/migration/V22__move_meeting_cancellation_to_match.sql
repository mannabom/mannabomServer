ALTER TABLE meeting_cancellation_requests
    ADD COLUMN meeting_match_id BIGINT;

ALTER TABLE meeting_cancellation_requests
    ALTER COLUMN meeting_id DROP NOT NULL;

ALTER TABLE meeting_cancellation_requests
    ADD CONSTRAINT fk_cancellation_request_meeting_match
        FOREIGN KEY (meeting_match_id)
            REFERENCES meeting_matches (id);

ALTER TABLE meeting_cancellation_requests
    ADD CONSTRAINT chk_cancellation_request_target
        CHECK (
            (meeting_id IS NOT NULL AND meeting_match_id IS NULL)
            OR
            (meeting_id IS NULL AND meeting_match_id IS NOT NULL)
        );

CREATE UNIQUE INDEX uk_meeting_match_cancellation_pending
    ON meeting_cancellation_requests (meeting_match_id)
    WHERE status = 'PENDING' AND meeting_match_id IS NOT NULL;

CREATE INDEX idx_cancellation_request_match_status
    ON meeting_cancellation_requests (meeting_match_id, status);
