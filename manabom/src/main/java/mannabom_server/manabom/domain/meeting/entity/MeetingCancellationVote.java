package mannabom_server.manabom.domain.meeting.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.meeting.enums.CancellationVoteDecision;
import mannabom_server.manabom.domain.user.entity.User;

import java.time.Instant;

@Entity
@Table(
        name = "meeting_cancellation_votes",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"request_id", "user_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingCancellationVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private MeetingCancellationRequest request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CancellationVoteDecision decision;

    private Instant decidedAt;
}
