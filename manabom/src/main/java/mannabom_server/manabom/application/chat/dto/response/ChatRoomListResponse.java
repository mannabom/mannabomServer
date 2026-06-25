package mannabom_server.manabom.application.chat.dto.response;

import lombok.Builder;
import lombok.Getter;
import mannabom_server.manabom.domain.chat.entity.ChatMessage;
import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import mannabom_server.manabom.domain.chat.enums.ChatRoomType;

import java.time.Instant;

@Builder
@Getter
public class ChatRoomListResponse {
     private Long roomId;
     private String roomName;
     private String lastMessage;
     private Instant lastmessageAt;
     private int unreadCount;

    public static ChatRoomListResponse of(ChatRoom room,String roomName, ChatMessage lastMsg, int unreadCount){
        return ChatRoomListResponse.builder()
                .roomId(room.getId())
                .roomName(roomName)
                .lastMessage(lastMsg!=null ? lastMsg.getType().getDisplayMessage(lastMsg.getContent()): null )
                .unreadCount(unreadCount)
                .build();
    }

}
