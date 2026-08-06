ALTER TABLE chat_messages
    ADD COLUMN system_event_type VARCHAR(64),
    ADD COLUMN system_title VARCHAR(255),
    ADD COLUMN actor_user_id BIGINT,
    ADD COLUMN actor_nickname VARCHAR(255),
    ADD COLUMN system_data JSONB;

ALTER TABLE chat_messages
    ADD CONSTRAINT fk_chat_message_actor
        FOREIGN KEY (actor_user_id) REFERENCES users(user_id);

CREATE INDEX idx_chat_messages_system_event_type
    ON chat_messages(system_event_type)
    WHERE system_event_type IS NOT NULL;

ALTER TABLE meeting_verification
    ADD COLUMN started_by_user_id BIGINT,
    ADD COLUMN participant_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE meeting_verification
    ADD CONSTRAINT fk_meeting_verification_started_by
        FOREIGN KEY (started_by_user_id) REFERENCES users(user_id);
