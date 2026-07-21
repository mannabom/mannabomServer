package mannabom_server.manabom.domain.meeting.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.meeting.enums.MeetingCancellationStatus;
import mannabom_server.manabom.domain.user.entity.User;

import java.time.Instant;

@Entity
@Table(name = "meeting_cancellation_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingCancellationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_user_id", nullable = false)
    private User initiator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingCancellationStatus status;

    @Column(nullable = false)
    private Instant requestedAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant completedAt;

    @Version
    private Long version;

    public static MeetingCancellationRequest create(
            Meeting meeting,
            User initiator,
            Instant requestedAt,
            Instant expiresAt
    ) {
        MeetingCancellationRequest request = new MeetingCancellationRequest();
        request.meeting = meeting;
        request.initiator = initiator;
        request.status = MeetingCancellationStatus.PENDING;
        request.requestedAt = requestedAt;
        request.expiresAt = expiresAt;
        return request;
    }

    public boolean isExpiredAt(Instant now) {
        return status == MeetingCancellationStatus.PENDING
                && !now.isBefore(expiresAt);
    }

    public void approve(Instant completedAt) {
        validatePending();
        this.status = MeetingCancellationStatus.APPROVED;
        this.completedAt = completedAt;
    }

    public void reject(Instant completedAt) {
        validatePending();
        this.status = MeetingCancellationStatus.REJECTED;
        this.completedAt = completedAt;
    }

    public void expire(Instant completedAt) {
        validatePending();
        this.status = MeetingCancellationStatus.EXPIRED;
        this.completedAt = completedAt;
    }

    private void validatePending() {
        if (status != MeetingCancellationStatus.PENDING) {
            throw new IllegalStateException("이미 종료된 미팅 취소 요청입니다.");
        }
    }
}
