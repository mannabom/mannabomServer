-- 위치정보 이용·제공사실 확인자료 (위치정보법 제16조 제2항). 자동 기록하고 6개월 보관 후 파기한다.
-- 탈퇴 후에도 보관 기간 동안 남아야 하므로 users 를 참조하지 않는다.
CREATE TABLE location_usage_log (
    location_usage_log_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    purpose VARCHAR(50) NOT NULL,
    acquisition_path VARCHAR(30) NOT NULL,
    chat_room_id BIGINT,
    used_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_location_usage_log_purpose
        CHECK (purpose IN ('MEETING_VERIFICATION')),
    CONSTRAINT ck_location_usage_log_acquisition_path
        CHECK (acquisition_path IN ('DEVICE_GPS'))
);

CREATE INDEX idx_location_usage_log_user_used_at
    ON location_usage_log (user_id, used_at DESC);

CREATE INDEX idx_location_usage_log_used_at
    ON location_usage_log (used_at);
