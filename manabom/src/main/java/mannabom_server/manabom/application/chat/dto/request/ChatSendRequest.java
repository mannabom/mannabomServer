package mannabom_server.manabom.application.chat.dto.request;

import lombok.Getter;

import mannabom_server.manabom.domain.chat.enums.ChatMessageType;
import mannabom_server.manabom.domain.chat.enums.ChatRoomType;

@Getter
public class ChatSendRequest {
    private Long roomId;
    private ChatRoomType chatRoomType;
    private ChatMessageType messageType;
    private String content;
    private String clientMeesageId;
}
