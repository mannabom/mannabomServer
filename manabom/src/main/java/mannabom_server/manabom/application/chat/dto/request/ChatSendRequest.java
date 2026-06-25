package mannabom_server.manabom.application.chat.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

import mannabom_server.manabom.domain.chat.enums.ChatMessageType;

@Getter
@AllArgsConstructor
public class ChatSendRequest {
    private Long roomId;
    private ChatMessageType messageType;
    private String content;
    private String clientMessageId;
}
