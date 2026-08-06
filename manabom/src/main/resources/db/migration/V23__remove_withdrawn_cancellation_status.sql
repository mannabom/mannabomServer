ALTER TABLE meeting_cancellation_requests
    DROP CONSTRAINT chk_cancellation_request_status;

ALTER TABLE meeting_cancellation_requests
    ADD CONSTRAINT chk_cancellation_request_status
        CHECK (status IN (
                          'PENDING',
                          'APPROVED',
                          'REJECTED',
                          'EXPIRED'
            ));
