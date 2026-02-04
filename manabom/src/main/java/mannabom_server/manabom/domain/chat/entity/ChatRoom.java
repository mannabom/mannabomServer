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
import mannabom_server.manabom.domain.meeting.entity.MeetingMatch;
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private MeetingMatch match;

    public static ChatRoom createMeetingChatRoom(Meeting meeting){
        return ChatRoom.builder()
                .meeting(meeting)
                .chatStatus(ChatStatus.ENABLED)
                .type(ChatRoomType.MEETING_GROUP)
                .build();
    }

    public static ChatRoom createMatchingChatRoom(MeetingMatch match){
        return ChatRoom.builder()
                .match(match)
                .chatStatus(ChatStatus.ENABLED)
                .type(ChatRoomType.MEETING_MATCH)
                .build();
    }

    public static ChatRoom createProfileChatRoom(){
        return ChatRoom.builder()
                .chatStatus(ChatStatus.ENABLED)
                .type(ChatRoomType.DM_PROFILE)
                .build();
    }

    public static ChatRoom createLoveviewChatRoom(){
        return ChatRoom.builder()
                .chatStatus(ChatStatus.ENABLED)
                .type(ChatRoomType.DM_CODE)
                .build();
    }

    @PrePersist
    @PreUpdate
    public void validate(){
        if (ChatRoomType.MEETING_GROUP.equals(this.type)) {
            if (this.meeting == null) throw new IllegalStateException("미팅 채팅방에는 미팅 정보가 필수입니다.");
            if (this.match != null) throw new IllegalStateException("미팅 채팅방에는 매칭 정보가 들어갈 수 없습니다.");
        }
        else if (ChatRoomType.MEETING_MATCH.equals(this.type)) {
            if (this.match == null) throw new IllegalStateException("매칭 채팅방에는 매칭 정보가 필수입니다.");
            if (this.meeting != null) throw new IllegalStateException("매칭 채팅방에는 미팅 정보가 들어갈 수 없습니다.");
        }
    }

}
