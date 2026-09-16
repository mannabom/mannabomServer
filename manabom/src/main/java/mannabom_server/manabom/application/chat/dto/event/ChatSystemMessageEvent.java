package mannabom_server.manabom.application.chat.dto.event;

import mannabom_server.manabom.application.chat.message.SystemMessageType;

import java.util.List;
import java.util.Map;

public record ChatSystemMessageEvent(
        Long roomId,
        SystemMessageType type,
        Long actorUserId,
        List<Long> recipientUserIds,
        Map<String, Object> data,
        boolean pushEnabled
) {
    public ChatSystemMessageEvent {
        recipientUserIds = recipientUserIds == null ? null : List.copyOf(recipientUserIds);
        data = data == null ? Map.of() : Map.copyOf(data);
    }

    public static ChatSystemMessageEvent of(
            Long roomId,
            SystemMessageType type,
            Long actorUserId,
            List<Long> recipientUserIds,
            Map<String, Object> data
    ) {
        return new ChatSystemMessageEvent(roomId, type, actorUserId, recipientUserIds, data, true);
    }

    public static ChatSystemMessageEvent withoutPush(
            Long roomId,
            SystemMessageType type,
            Long actorUserId,
            List<Long> recipientUserIds,
            Map<String, Object> data
    ) {
        return new ChatSystemMessageEvent(roomId, type, actorUserId, recipientUserIds, data, false);
    }
}
