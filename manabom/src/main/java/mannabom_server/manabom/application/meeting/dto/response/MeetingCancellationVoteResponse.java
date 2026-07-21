package mannabom_server.manabom.application.meeting.dto.response;

import lombok.Builder;
import lombok.Getter;
import mannabom_server.manabom.domain.meeting.entity.MeetingCancellationVote;
import mannabom_server.manabom.domain.meeting.enums.CancellationVoteDecision;

import java.time.Instant;

@Getter
@Builder
public class MeetingCancellationVoteResponse {
    private Long userId;
    private CancellationVoteDecision decision;
    private Instant decidedAt;

    public static MeetingCancellationVoteResponse from(MeetingCancellationVote vote) {
        return MeetingCancellationVoteResponse.builder()
                .userId(vote.getUser().getUserId())
                .decision(vote.getDecision())
                .decidedAt(vote.getDecidedAt())
                .build();
    }
}
