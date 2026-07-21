package mannabom_server.manabom.domain.meeting;

import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.entity.MeetingCancellationRequest;
import mannabom_server.manabom.domain.meeting.entity.MeetingCancellationVote;
import mannabom_server.manabom.domain.meeting.entity.MeetingMatch;
import mannabom_server.manabom.domain.meeting.enums.CancellationVoteDecision;
import mannabom_server.manabom.domain.meeting.enums.MeetingCancellationStatus;
import mannabom_server.manabom.domain.user.entity.User;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MeetingCancellationDomainTest {

    @Test
    void initiatorAutomaticallyAgrees() {
        Instant now = Instant.parse("2026-07-21T00:00:00Z");
        User initiator = user(1L);
        MeetingCancellationRequest request = cancellationRequest(initiator, now);

        MeetingCancellationVote vote = MeetingCancellationVote.agreedByInitiator(
                request,
                initiator,
                now
        );

        assertThat(vote.getDecision()).isEqualTo(CancellationVoteDecision.AGREE);
        assertThat(vote.getDecidedAt()).isEqualTo(now);
    }

    @Test
    void pendingMemberCanVoteOnlyOnce() {
        Instant now = Instant.parse("2026-07-21T00:00:00Z");
        User initiator = user(1L);
        MeetingCancellationVote vote = MeetingCancellationVote.pending(
                cancellationRequest(initiator, now),
                user(2L)
        );

        vote.decide(CancellationVoteDecision.REJECT, now);

        assertThat(vote.getDecision()).isEqualTo(CancellationVoteDecision.REJECT);
        assertThatThrownBy(() -> vote.decide(CancellationVoteDecision.AGREE, now))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void requestExpiresAtItsTwentyFourHourDeadline() {
        Instant now = Instant.parse("2026-07-21T00:00:00Z");
        MeetingCancellationRequest request = cancellationRequest(user(1L), now);
        Instant deadline = now.plus(Duration.ofHours(24));

        assertThat(request.isExpiredAt(deadline.minusMillis(1))).isFalse();
        assertThat(request.isExpiredAt(deadline)).isTrue();

        request.expire(deadline);

        assertThat(request.getStatus()).isEqualTo(MeetingCancellationStatus.EXPIRED);
        assertThat(request.getCompletedAt()).isEqualTo(deadline);
    }

    @Test
    void approvedRequestCannotBeCompletedAgain() {
        Instant now = Instant.parse("2026-07-21T00:00:00Z");
        MeetingCancellationRequest request = cancellationRequest(user(1L), now);

        request.approve(now);

        assertThat(request.getStatus()).isEqualTo(MeetingCancellationStatus.APPROVED);
        assertThatThrownBy(() -> request.reject(now))
                .isInstanceOf(IllegalStateException.class);
    }

    private MeetingCancellationRequest cancellationRequest(User initiator, Instant now) {
        return MeetingCancellationRequest.create(
                MeetingMatch.builder()
                        .meeting1(Meeting.builder().id(10L).build())
                        .meeting2(Meeting.builder().id(11L).build())
                        .build(),
                initiator,
                now,
                now.plus(Duration.ofHours(24))
        );
    }

    private User user(Long id) {
        return User.builder()
                .userId(id)
                .kakaoId("mock_" + id)
                .userName("user" + id)
                .build();
    }
}
