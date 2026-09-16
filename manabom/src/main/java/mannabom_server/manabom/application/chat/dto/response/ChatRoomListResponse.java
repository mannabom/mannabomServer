package mannabom_server.manabom.application.chat.dto.response;

import lombok.Builder;
import lombok.Getter;
import mannabom_server.manabom.domain.chat.entity.ChatMessage;
import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import mannabom_server.manabom.domain.chat.enums.ChatMessageType;

import java.time.Instant;

@Builder
@Getter
public class ChatRoomListResponse {
     private Long roomId;
     private String roomName;
     private String lastMessage;
     private Instant lastmessageAt;
     private boolean hasUnreadMessages;

    public static ChatRoomListResponse of(ChatRoom room,String roomName, ChatMessage lastMsg, boolean hasUnreadMessages){
        return ChatRoomListResponse.builder()
                .roomId(room.getId())
                .roomName(roomName)
                .lastMessage(lastMsg != null ? lastMessagePreview(lastMsg) : null)
                .lastmessageAt(lastMsg != null ? lastMsg.getCreatedAt() : null)
                .hasUnreadMessages(hasUnreadMessages)
                .build();
    }

    private static String lastMessagePreview(ChatMessage message) {
        if (message.getType() == ChatMessageType.SYSTEM && message.getSystemTitle() != null) {
            return message.getSystemTitle();
        }
        return message.getType().getDisplayMessage(message.getContent());
    }

}
