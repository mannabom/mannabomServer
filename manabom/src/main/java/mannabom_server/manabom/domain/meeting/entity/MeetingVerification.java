package mannabom_server.manabom.domain.meeting.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import mannabom_server.manabom.domain.user.entity.User;

import java.time.Duration;
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

    private Instant startedAt;

    private Instant expiresAt;

    private Instant verifiedAt;

    private Instant failureNotifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "started_by_user_id")
    private User startedBy;

    @Column(name = "participant_count", nullable = false)
    private int participantCount;

    @Builder
    public MeetingVerification(ChatRoom room) {
        this.room = room;
    }

    public void startIfNeeded(Instant now, Duration validDuration, User initiator){
        if(this.startedAt == null){
            this.startedAt = now;
            this.expiresAt = now.plus(validDuration);
            this.startedBy = initiator;
        }
    }

    public void updateParticipantCount(int participantCount) {
        this.participantCount = Math.max(this.participantCount, participantCount);
    }

    public boolean isExpired(Instant now){
        return this.expiresAt != null && !now.isBefore(this.expiresAt);
    }

    public Duration remainingTime(Instant now){
        if(this.expiresAt == null){
            return Duration.ZERO;
        }
        Duration remaining = Duration.between(now, this.expiresAt);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    public void verify(){
        this.isVerified = true;
        this.verifiedAt = Instant.now();
    }

    public boolean needsFailureNotification(Instant now) {
        return !isVerified
                && failureNotifiedAt == null
                && expiresAt != null
                && !now.isBefore(expiresAt);
    }

    public void markFailureNotified(Instant now) {
        if (!needsFailureNotification(now)) {
            throw new IllegalStateException("만남 인증 실패 알림 대상이 아닙니다.");
        }
        this.failureNotifiedAt = now;
    }

}
