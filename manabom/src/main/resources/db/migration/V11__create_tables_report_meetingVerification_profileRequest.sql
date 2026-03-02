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
                         processed_at TIMESTAMP WITH TIME ZONE,
                         created_at TIMESTAMP WITH TIME ZONE NOT NULL,

                         CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES users (id),
                         CONSTRAINT fk_report_target FOREIGN KEY (target_id) REFERENCES users (id)
);

-- 2. meeting_verification 테이블
CREATE TABLE meeting_verification (
                                      id BIGSERIAL PRIMARY KEY,
                                      room_id BIGINT NOT NULL UNIQUE,
                                      is_verified BOOLEAN DEFAULT FALSE,
                                      verified_at TIMESTAMP WITH TIME ZONE,

                                      CONSTRAINT fk_meeting_verification_room FOREIGN KEY (room_id) REFERENCES chat_rooms (id)
);

-- 3. love_view_photo_requests 테이블 (추가됨)
CREATE TABLE love_view_photo_requests (
                                          id BIGSERIAL PRIMARY KEY,
                                          history_id BIGINT,
                                          sender_id BIGINT NOT NULL,
                                          receiver_id BIGINT NOT NULL,
                                          status VARCHAR(50) NOT NULL, -- PhotoRequestStatus (Enum)
                                          created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                          updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                          CONSTRAINT fk_photo_request_history FOREIGN KEY (history_id) REFERENCES love_view_recommend_histories (id),
                                          CONSTRAINT fk_photo_request_sender FOREIGN KEY (sender_id) REFERENCES users (id),
                                          CONSTRAINT fk_photo_request_receiver FOREIGN KEY (receiver_id) REFERENCES users (id)
);