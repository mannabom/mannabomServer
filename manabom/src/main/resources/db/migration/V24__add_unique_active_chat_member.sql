-- 과거 DB 기본값 오타 보정
UPDATE chat_members
SET status = 'ACTIVATE'
WHERE status = 'ACTIVE';

-- 변환 후 발생할 수 있는 활성 중복 정리
WITH ranked_active_members AS (
    SELECT id,
        ROW_NUMBER() OVER (
               PARTITION BY room_id, user_id
               ORDER BY id
           ) AS row_number
    FROM chat_members
    WHERE status = 'ACTIVATE'
)
UPDATE chat_members AS cm
SET status = 'DEACTIVATED'
FROM ranked_active_members AS ranked
WHERE cm.id = ranked.id
  AND ranked.row_number > 1;

-- 이후 기본값도 Java enum과 통일
ALTER TABLE chat_members
    ALTER COLUMN status SET DEFAULT 'ACTIVATE';

CREATE UNIQUE INDEX uk_chat_members_active_room_user
    ON chat_members (room_id, user_id)
    WHERE status = 'ACTIVATE';
