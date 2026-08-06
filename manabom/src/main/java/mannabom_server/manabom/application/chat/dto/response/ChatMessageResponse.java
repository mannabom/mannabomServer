package mannabom_server.manabom.application.chat.dto.response;

import lombok.Builder;
import lombok.Getter;
import mannabom_server.manabom.domain.chat.entity.ChatMessage;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
public class ChatMessageResponse {
    private Long messageId;
    private String content;
    private String messageType;
    private String systemEventType;
    private String systemTitle;
    private Long actorUserId;
    private String actorNickname;
    private Map<String, Object> data;
    private Instant createdAt;

    private Long senderId;

    public static ChatMessageResponse of(ChatMessage msg){
        return ChatMessageResponse.builder()
                .messageId(msg.getId())
                .createdAt(msg.getCreatedAt())
                .messageType(msg.getType().name())
                .senderId(msg.getUser() != null ? msg.getUser().getUserId() : null)
                .content(msg.getContent())
                .systemEventType(msg.getSystemEventType())
                .systemTitle(msg.getSystemTitle())
                .actorUserId(msg.getActorUserId())
                .actorNickname(msg.getActorNickname())
                .data(msg.getSystemData())
                .build();
    }
}
