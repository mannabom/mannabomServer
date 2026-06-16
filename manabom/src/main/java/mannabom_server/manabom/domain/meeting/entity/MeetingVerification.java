package mannabom_server.manabom.domain.meeting.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import mannabom_server.manabom.domain.user.entity.User;

import java.time.Instant;

@Entity
@Table
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false, unique = true)
    private ChatRoom room;

    private boolean isVerified = false;

    private Instant verifiedAt;

    @Builder
    public MeetingVerification(ChatRoom room) {
        this.room = room;
    }


    public void verify(){
        this.isVerified = true;
        this.verifiedAt = Instant.now();
    }

}
