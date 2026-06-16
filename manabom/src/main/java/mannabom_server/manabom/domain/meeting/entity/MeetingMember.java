package mannabom_server.manabom.domain.meeting.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.meeting.enums.ChatUserStatus;
import mannabom_server.manabom.domain.meeting.enums.MeetingRole;
import mannabom_server.manabom.domain.meeting.enums.MeetingRoleConverter;
import mannabom_server.manabom.domain.meeting.enums.ChatUserStatusConverter;
import mannabom_server.manabom.domain.user.entity.User;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "meeting_members")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class MeetingMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Convert(converter = MeetingRoleConverter.class)
    private MeetingRole meetingRole;

    @Convert(converter = ChatUserStatusConverter.class)
    private ChatUserStatus status;


    public static MeetingMember addLeader(Meeting meeting, User user){
        return MeetingMember.builder()
                .meeting(meeting)
                .user(user)
                .meetingRole(MeetingRole.LEADER)
                .status(ChatUserStatus.ACTIVE)
                .build();
    }
    public static MeetingMember addMember(Meeting meeting, User user){
        return MeetingMember.builder()
                .meeting(meeting)
                .user(user)
                .meetingRole(MeetingRole.MEMBER)
                .status(ChatUserStatus.ACTIVE)
                .build();
    }

    public void deactivate(){
        this.status = ChatUserStatus.DEACTIVATED;
    }
    public void appointLeader(){
        this.meetingRole = MeetingRole.LEADER;
    }

}
