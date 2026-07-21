CREATE UNIQUE INDEX uk_chat_members_active_room_user
    ON chat_members (room_id, user_id)
    WHERE status = 'ACTIVATE';
