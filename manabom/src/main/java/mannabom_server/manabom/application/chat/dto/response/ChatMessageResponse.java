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
    private int unreadCount;

    public static ChatMessageResponse of(ChatMessage msg, int unreadCount){
        return ChatMessageResponse.builder()
                .messageId(msg.getId())
                .createdAt(msg.getCreatedAt())
                .messageType(msg.getType().name())
                .unreadCount(unreadCount)
                .senderId(msg.getUser().getUserId())
                .content(msg.getContent())
                .build();
    }
}
