package mannabom_server.manabom.domain.chat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.chat.enums.ChatRoomType;
import mannabom_server.manabom.domain.chat.enums.ChatRoomTypeConverter;
import mannabom_server.manabom.domain.chat.enums.ChatStatus;
import mannabom_server.manabom.domain.common.BaseTimeEntity;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import org.hibernate.annotations.Fetch;

@Entity
@Table(name = "chat_rooms")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ChatRoom extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Convert(converter = ChatRoomTypeConverter.class)
    private ChatRoomType type;

    private ChatStatus chatStatus;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    public static ChatRoom createMeetingChatRoom(Meeting meeting){
        return ChatRoom.builder()
                .meeting(meeting)
                .chatStatus(ChatStatus.ENABLED)
                .type(ChatRoomType.MEETING_GROUP)
                .build();
    }


}
