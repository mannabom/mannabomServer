-- 회원 간 차단. 한 방향으로 저장하고 추천·요청·1:1 대화에는 양방향으로 적용한다.
CREATE TABLE user_block (
    user_block_id BIGSERIAL PRIMARY KEY,
    blocker_user_id BIGINT NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    blocked_user_id BIGINT NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    source VARCHAR(20) NOT NULL DEFAULT 'DIRECT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_block UNIQUE (blocker_user_id, blocked_user_id),
    CONSTRAINT ck_user_block_not_self CHECK (blocker_user_id <> blocked_user_id),
    CONSTRAINT ck_user_block_source CHECK (source IN ('DIRECT', 'REPORT'))
);

CREATE INDEX idx_user_block_blocked ON user_block (blocked_user_id);
