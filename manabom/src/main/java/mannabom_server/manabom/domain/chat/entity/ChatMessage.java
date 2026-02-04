package mannabom_server.manabom.domain.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.chat.enums.ChatMessageType;
import mannabom_server.manabom.domain.chat.enums.ChatMessageTypeConverter;
import mannabom_server.manabom.domain.common.BaseTimeEntity;
import mannabom_server.manabom.domain.question.enums.QuestionType;
import mannabom_server.manabom.domain.user.entity.User;

/**
 * 채팅 메시지 엔터티
 */

@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor
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


}
