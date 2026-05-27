-- 1. status 컬럼 추가 (기본값은 'ACTIVE')
ALTER TABLE chat_members
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
-- 2. (선택사항) 성능을 위해 status 컬럼에 인덱스 추가
-- 참여 중인 멤버만 자주 조회
CREATE INDEX idx_chat_member_status ON chat_members(status);


-- meeting 테이블에 삭제일 컬럼 추가
-- Instant와 매핑되도록 TIMESTAMP(또는 DATETIME) 타입을 사용합니다.
ALTER TABLE meeting ADD COLUMN deleted_at TIMESTAMP(6) NULL;

-- 삭제된 데이터를 제외하고 조회하는 경우가 많으므로 인덱스를 고려하면 좋습니다.
CREATE INDEX idx_meeting_deleted_at ON meeting(deleted_at);