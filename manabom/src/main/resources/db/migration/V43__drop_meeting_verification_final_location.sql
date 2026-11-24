-- 만남 인증 좌표는 목적(인증 판정) 달성 후 보관하지 않는다 (위치정보법 제23조).
-- 인증 가능 시간 동안 필요한 최종 위치는 Redis 에만 두고 만료와 함께 삭제한다.
ALTER TABLE meeting_verification
    DROP COLUMN final_latitude,
    DROP COLUMN final_longitude;
