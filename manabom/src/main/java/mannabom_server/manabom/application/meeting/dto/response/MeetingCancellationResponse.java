package mannabom_server.manabom.application.meeting.dto.response;

import lombok.Builder;
import lombok.Getter;
import mannabom_server.manabom.domain.meeting.entity.MeetingCancellationRequest;
import mannabom_server.manabom.domain.meeting.entity.MeetingCancellationVote;
import mannabom_server.manabom.domain.meeting.enums.CancellationVoteDecision;
import mannabom_server.manabom.domain.meeting.enums.MeetingCancellationStatus;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class MeetingCancellationResponse {
    private Long requestId;
    private Long meetingId;
    private Long initiatorUserId;
    private MeetingCancellationStatus status;
    private Instant requestedAt;
    private Instant expiresAt;
    private Instant completedAt;
    private int totalMemberCount;
    private int agreedMemberCount;
    private int pendingMemberCount;
    private List<MeetingCancellationVoteResponse> votes;

    public static MeetingCancellationResponse of(
            MeetingCancellationRequest request,
            List<MeetingCancellationVote> votes
    ) {
        int agreed = (int) votes.stream()
                .filter(vote -> vote.getDecision() == CancellationVoteDecision.AGREE)
                .count();
        int pending = (int) votes.stream()
                .filter(vote -> vote.getDecision() == CancellationVoteDecision.PENDING)
                .count();

        return MeetingCancellationResponse.builder()
                .requestId(request.getId())
                .meetingId(request.getMeeting().getId())
                .initiatorUserId(request.getInitiator().getUserId())
                .status(request.getStatus())
                .requestedAt(request.getRequestedAt())
                .expiresAt(request.getExpiresAt())
                .completedAt(request.getCompletedAt())
                .totalMemberCount(votes.size())
                .agreedMemberCount(agreed)
                .pendingMemberCount(pending)
                .votes(votes.stream()
                        .map(MeetingCancellationVoteResponse::from)
                        .toList())
                .build();
    }
}
