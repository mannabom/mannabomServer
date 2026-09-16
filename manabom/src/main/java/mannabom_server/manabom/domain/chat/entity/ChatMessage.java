package mannabom_server.manabom.domain.chat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.chat.enums.ChatMessageType;
import mannabom_server.manabom.domain.chat.enums.ChatMessageTypeConverter;
import mannabom_server.manabom.domain.common.BaseTimeEntity;
import mannabom_server.manabom.domain.user.entity.User;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

/**
 * 채팅 메시지 엔터티
 */

@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ChatMessage extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_user_id")
    private User user;

    @Convert(converter = ChatMessageTypeConverter.class)
    private ChatMessageType type;

    private String content;

    @Column(name = "system_event_type", length = 64)
    private String systemEventType;

    @Column(name = "system_title")
    private String systemTitle;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_nickname")
    private String actorNickname;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "system_data", columnDefinition = "jsonb")
    private Map<String, Object> systemData;

    public static ChatMessage system(ChatRoom room, String content) {
        return ChatMessage.builder()
                .room(room)
                .user(null)
                .type(ChatMessageType.SYSTEM)
                .content(content)
                .build();
    }

    public static ChatMessage system(
            ChatRoom room,
            String content,
            String systemEventType,
            String systemTitle,
            Long actorUserId,
            String actorNickname,
            Map<String, Object> systemData
    ) {
        return ChatMessage.builder()
                .room(room)
                .user(null)
                .type(ChatMessageType.SYSTEM)
                .content(content)
                .systemEventType(systemEventType)
                .systemTitle(systemTitle)
                .actorUserId(actorUserId)
                .actorNickname(actorNickname)
                .systemData(systemData == null ? Map.of() : new HashMap<>(systemData))
                .build();
    }
}
