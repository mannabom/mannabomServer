package mannabom_server.manabom.domain.meeting.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.chat.entity.ChatMember;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_member_id", nullable = false, unique = true)
    private ChatMember chatMember;

    private boolean isVerified = false;

    private Instant rewaredAt;

    @Builder
    public MeetingParticipant(ChatMember chatMember){
        this.chatMember = chatMember;
    }
    public void verify(){
        this.isVerified = true;
        this.rewaredAt = Instant.now();
    }
}
