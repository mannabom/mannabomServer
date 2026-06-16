package mannabom_server.manabom.application.chat.dto.event;

import mannabom_server.manabom.domain.chat.enums.ChatRoomType;

public record ChatRoomLeaveEvent(
        Long userId,
        ChatRoomType roomType,
        Long referenceId
) {
}
