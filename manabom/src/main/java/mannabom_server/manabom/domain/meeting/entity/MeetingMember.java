package mannabom_server.manabom.domain.meeting.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.meeting.enums.ChatUserStatus;
import mannabom_server.manabom.domain.meeting.enums.Role;
import mannabom_server.manabom.domain.meeting.enums.RoleConverter;
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

    @Convert(converter = RoleConverter.class)
    private Role role;

    @Convert(converter = ChatUserStatusConverter.class)
    private ChatUserStatus status;


    public static MeetingMember addLeader(Meeting meeting, User user){
        return MeetingMember.builder()
                .meeting(meeting)
                .user(user)
                .role(Role.LEADER)
                .status(ChatUserStatus.ACTIVE)
                .build();
    }
    public static MeetingMember addMember(Meeting meeting, User user){
        return MeetingMember.builder()
                .meeting(meeting)
                .user(user)
                .role(Role.MEMBER)
                .status(ChatUserStatus.ACTIVE)
                .build();
    }

}
