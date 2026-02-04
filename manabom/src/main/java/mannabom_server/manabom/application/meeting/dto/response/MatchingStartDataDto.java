package mannabom_server.manabom.application.meeting.dto.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class MatchingStartDataDto {
    private boolean matchingStarted;
    private Integer estimatedWaitTime;
}
