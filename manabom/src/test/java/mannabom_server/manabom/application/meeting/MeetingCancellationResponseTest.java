package mannabom_server.manabom.application.meeting;

import mannabom_server.manabom.application.meeting.dto.response.MeetingCancellationResponse;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.entity.MeetingCancellationRequest;
import mannabom_server.manabom.domain.meeting.entity.MeetingCancellationVote;
import mannabom_server.manabom.domain.meeting.entity.MeetingMatch;
import mannabom_server.manabom.domain.meeting.enums.CancellationVoteDecision;
import mannabom_server.manabom.domain.user.entity.User;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MeetingCancellationResponseTest {

    @Test
    void countsAgreePendingAndRejectVotes() {
        Instant now = Instant.parse("2026-08-06T00:00:00Z");
        MeetingCancellationRequest request = MeetingCancellationRequest.create(
                MeetingMatch.builder()
                        .meeting1(Meeting.builder().id(10L).build())
                        .meeting2(Meeting.builder().id(11L).build())
                        .build(),
                user(1L),
                now,
                now.plus(Duration.ofHours(24))
        );
        MeetingCancellationVote agreed = MeetingCancellationVote.agreedByInitiator(
                request,
                user(1L),
                now
        );
        MeetingCancellationVote pending = MeetingCancellationVote.pending(request, user(2L));
        MeetingCancellationVote rejected = MeetingCancellationVote.pending(request, user(3L));
        rejected.decide(CancellationVoteDecision.REJECT, now);

        MeetingCancellationResponse response = MeetingCancellationResponse.of(
                request,
                List.of(agreed, pending, rejected)
        );

        assertThat(response.getTotalMemberCount()).isEqualTo(3);
        assertThat(response.getAgreedMemberCount()).isEqualTo(1);
        assertThat(response.getPendingMemberCount()).isEqualTo(1);
        assertThat(response.getRejectedMemberCount()).isEqualTo(1);
        assertThat(response.getAgreedMemberCount()
                + response.getPendingMemberCount()
                + response.getRejectedMemberCount())
                .isEqualTo(response.getTotalMemberCount());
    }

    private User user(Long id) {
        return User.builder()
                .userId(id)
                .kakaoId("mock_" + id)
                .userName("user" + id)
                .build();
    }
}
