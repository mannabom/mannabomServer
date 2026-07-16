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
}
