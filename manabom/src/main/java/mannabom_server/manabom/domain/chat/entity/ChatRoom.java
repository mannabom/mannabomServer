package mannabom_server.manabom.domain.chat.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.chat.enums.ChatRoomType;
import mannabom_server.manabom.domain.chat.enums.ChatRoomTypeConverter;
import mannabom_server.manabom.domain.chat.enums.ChatStatus;
import mannabom_server.manabom.domain.common.BaseTimeEntity;
import mannabom_server.manabom.domain.matching.entity.LoveViewRecommendHistory;
import mannabom_server.manabom.domain.matching.entity.ProfileRecommendHistory;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.entity.MeetingMatch;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "chat_rooms")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@SQLDelete(sql = "UPDATE chat_rooms SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
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
    @NotFound(action = NotFoundAction.IGNORE)
    private Meeting meeting;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private MeetingMatch match;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_recommend_history_id")
    private ProfileRecommendHistory profile;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "love_view_recommend_history_id")
    private LoveViewRecommendHistory loveView;

    @Column(name = "deleted_at")
    private Instant deletedAt;


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

    public static ChatRoom createProfileChatRoom(ProfileRecommendHistory profile){
        return ChatRoom.builder()
                .chatStatus(ChatStatus.ENABLED)
                .type(ChatRoomType.PROFILE_MATCH)
                .profile(profile)
                .build();
    }

    public static ChatRoom createLoveviewChatRoom(LoveViewRecommendHistory loveView){
        return ChatRoom.builder()
                .chatStatus(ChatStatus.ENABLED)
                .type(ChatRoomType.LOVEVIEW_MATCH)
                .loveView(loveView)
                .build();
    }

    @PrePersist
    @PreUpdate
    public void validate(){
        checkRequiredField();
        int nonNullCount =0;
        if(meeting!=null) nonNullCount++;
        if(profile!=null) nonNullCount++;
        if(match!=null) nonNullCount++;
        if(loveView!=null) nonNullCount++;

        if(nonNullCount!=1){
            throw new IllegalStateException("채팅방은 하나의 정보만 가질 수 있습니다.");
        }
    }
    private void checkRequiredField(){
        switch (this.type){
            case MEETING_MATCH -> {if(this.match==null) throw new IllegalStateException("미팅 매칭 채팅방에는 미팅 매칭 정보가 필수입니다.");}
            case MEETING_GROUP -> {if(this.meeting==null) throw new IllegalStateException("미팅 동성 채팅방에는 미팅 정보가 필수입니다.");}
            case PROFILE_MATCH -> {if(this.profile==null) throw new IllegalStateException("프로필 매칭 채팅방에는 프로필 정보가 필수입니다.");}
            case LOVEVIEW_MATCH -> {if(this.loveView==null) throw new IllegalStateException("연애코드 매칭 채팅방에는 연애코드 정보가 필수입니다.");}

        }
    }
    public void deactivate(){
        this.chatStatus = ChatStatus.DISABLED;
    }

    public void delete(){
        this.deletedAt = Instant.now();
    }
}
