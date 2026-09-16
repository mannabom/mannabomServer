package mannabom_server.manabom.application.meeting.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.meeting.enums.CancellationVoteDecision;

@Getter
@NoArgsConstructor
public class MeetingCancellationVoteRequest {

    @NotNull(message = "동의 또는 비동의를 선택해야 합니다.")
    private CancellationVoteDecision decision;
}
