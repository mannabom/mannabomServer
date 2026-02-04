package mannabom_server.manabom.domain.chat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.user.entity.User;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "chat_members")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ChatMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id",nullable = false)
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    @CreationTimestamp
    private Instant joinedAt;

    private Integer lastReadMessageId;

    public static ChatMember create(ChatRoom room, User user){
        return ChatMember.builder()
                .room(room)
                .user(user)
                .build();
    }
}
