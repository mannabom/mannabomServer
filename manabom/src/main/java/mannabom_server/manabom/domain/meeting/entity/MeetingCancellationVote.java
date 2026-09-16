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

    public static MeetingCancellationVote pending(
            MeetingCancellationRequest request,
            User user
    ) {
        MeetingCancellationVote vote = new MeetingCancellationVote();
        vote.request = request;
        vote.user = user;
        vote.decision = CancellationVoteDecision.PENDING;
        return vote;
    }

    public static MeetingCancellationVote agreedByInitiator(
            MeetingCancellationRequest request,
            User user,
            Instant decidedAt
    ) {
        MeetingCancellationVote vote = pending(request, user);
        vote.agree(decidedAt);
        return vote;
    }

    public void decide(CancellationVoteDecision decision, Instant decidedAt) {
        if (decision == CancellationVoteDecision.PENDING) {
            throw new IllegalArgumentException("투표 결과는 AGREE 또는 REJECT여야 합니다.");
        }
        if (this.decision != CancellationVoteDecision.PENDING) {
            throw new IllegalStateException("이미 투표를 완료했습니다.");
        }
        this.decision = decision;
        this.decidedAt = decidedAt;
    }

    private void agree(Instant decidedAt) {
        this.decision = CancellationVoteDecision.AGREE;
        this.decidedAt = decidedAt;
    }
}
