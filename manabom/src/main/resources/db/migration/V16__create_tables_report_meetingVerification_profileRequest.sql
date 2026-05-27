-- 1. reports 테이블
CREATE TABLE reports (
                         id BIGSERIAL PRIMARY KEY,
                         type VARCHAR(50) NOT NULL,
                         context_id BIGINT,
                         reporter_id BIGINT NOT NULL,
                         target_id BIGINT NOT NULL,
                         reason VARCHAR(50) NOT NULL,
                         additional_detail TEXT,
                         status VARCHAR(50) NOT NULL DEFAULT 'RECEIVED',
                         admin_comment TEXT,
                         processed_at TIMESTAMPTZ,
                         created_at TIMESTAMPTZ NOT NULL,
                         CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES users(user_id),
                         CONSTRAINT fk_report_target FOREIGN KEY (target_id) REFERENCES users(user_id)
);

-- 2. meeting_verification 테이블
CREATE TABLE meeting_verification (
                                      id BIGSERIAL PRIMARY KEY,
                                      room_id BIGINT NOT NULL UNIQUE,
                                      is_verified BOOLEAN DEFAULT FALSE,
                                      verified_at TIMESTAMPTZ,
                                      CONSTRAINT fk_meeting_verification_room FOREIGN KEY (room_id) REFERENCES chat_rooms(id)
);

-- 3. meeting_participant 테이블 (추가됨)
CREATE TABLE meeting_participant (
                                     id BIGSERIAL PRIMARY KEY,
                                     chat_member_id BIGINT NOT NULL UNIQUE, -- OneToOne 관계
                                     is_verified BOOLEAN DEFAULT FALSE,
                                     rewared_at TIMESTAMPTZ,                -- Instant 매핑
                                     CONSTRAINT fk_meeting_participant_chat_member FOREIGN KEY (chat_member_id) REFERENCES chat_members(id)
);

-- 4. love_view_photo_requests 테이블
CREATE TABLE love_view_photo_requests (
                                          id BIGSERIAL PRIMARY KEY,
                                          history_id BIGINT,
                                          sender_id BIGINT NOT NULL,
                                          receiver_id BIGINT NOT NULL,
                                          status VARCHAR(50) NOT NULL,           -- @Enumerated(EnumType.STRING) 필수!
                                          created_at TIMESTAMPTZ NOT NULL,
                                          updated_at TIMESTAMPTZ NOT NULL,
                                          CONSTRAINT fk_photo_request_history FOREIGN KEY (history_id) REFERENCES love_view_recommend_history(id),
                                          CONSTRAINT fk_photo_request_sender FOREIGN KEY (sender_id) REFERENCES users(user_id),
                                          CONSTRAINT fk_photo_request_receiver FOREIGN KEY (receiver_id) REFERENCES users(user_id)
);