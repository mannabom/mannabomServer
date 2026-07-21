package mannabom_server.manabom.application.chat.dto.response;

import lombok.Builder;
import lombok.Getter;
import mannabom_server.manabom.domain.chat.entity.ChatMessage;

import java.time.Instant;

@Getter
@Builder
public class ChatMessageResponse {
    private Long messageId;
    private String content;
    private String messageType;
    private Instant createdAt;

    private Long senderId;

    public static ChatMessageResponse of(ChatMessage msg){
        return ChatMessageResponse.builder()
                .messageId(msg.getId())
                .createdAt(msg.getCreatedAt())
                .messageType(msg.getType().name())
                .senderId(msg.getUser() != null ? msg.getUser().getUserId() : null)
                .content(msg.getContent())
                .build();
    }
}
