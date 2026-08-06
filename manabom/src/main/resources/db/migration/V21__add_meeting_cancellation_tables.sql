CREATE TABLE meeting_cancellation_requests
(
    id                BIGSERIAL PRIMARY KEY,
    meeting_id        BIGINT      NOT NULL,
    initiator_user_id BIGINT      NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at        TIMESTAMPTZ NOT NULL,
    completed_at      TIMESTAMPTZ NULL,
    version           BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT fk_cancellation_request_meeting
        FOREIGN KEY (meeting_id)
            REFERENCES meeting (id),

    CONSTRAINT fk_cancellation_request_initiator
        FOREIGN KEY (initiator_user_id)
            REFERENCES users (user_id),

    CONSTRAINT chk_cancellation_request_status
        CHECK (status IN (
                          'PENDING',
                          'APPROVED',
                          'REJECTED',
                          'EXPIRED',
                          'WITHDRAWN'
            )),

    CONSTRAINT chk_cancellation_request_expiration
        CHECK (expires_at > requested_at)
);

-- 한 미팅에서는 진행 중인 전체 취소 요청을 하나만 허용
CREATE UNIQUE INDEX uk_meeting_cancellation_pending
    ON meeting_cancellation_requests (meeting_id)
    WHERE status = 'PENDING';

CREATE INDEX idx_cancellation_request_meeting_status
    ON meeting_cancellation_requests (meeting_id, status);

-- 만료된 요청을 스케줄러에서 조회할 때 사용
CREATE INDEX idx_cancellation_request_status_expires
    ON meeting_cancellation_requests (status, expires_at);


CREATE TABLE meeting_cancellation_votes
(
    id         BIGSERIAL PRIMARY KEY,
    request_id BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    decision   VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    decided_at TIMESTAMPTZ NULL,

    CONSTRAINT fk_cancellation_vote_request
        FOREIGN KEY (request_id)
            REFERENCES meeting_cancellation_requests (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_cancellation_vote_user
        FOREIGN KEY (user_id)
            REFERENCES users (user_id),

    CONSTRAINT uk_cancellation_vote_request_user
        UNIQUE (request_id, user_id),

    CONSTRAINT chk_cancellation_vote_decision
        CHECK (decision IN (
                            'PENDING',
                            'AGREE',
                            'REJECT'
            )),

    CONSTRAINT chk_cancellation_vote_decided_at
        CHECK (
            (decision = 'PENDING' AND decided_at IS NULL)
                OR
            (decision IN ('AGREE', 'REJECT') AND decided_at IS NOT NULL)
            )
);

CREATE INDEX idx_cancellation_vote_request
    ON meeting_cancellation_votes (request_id);

CREATE INDEX idx_cancellation_vote_user
    ON meeting_cancellation_votes (user_id);