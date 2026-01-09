package mannabom_server.manabom.application.chat.dto;

import lombok.Getter;
import lombok.Setter;
import mannabom_server.manabom.domain.chat.ChatMessageType;
import mannabom_server.manabom.domain.chat.ChatRoomType;

@Getter
@Setter
public class ChatSendRequest {

    private Long roomId;
    private ChatRoomType chatRoomType;
    private ChatMessageType messageType;
    private String content;
    private String clientMeesageId;
}
