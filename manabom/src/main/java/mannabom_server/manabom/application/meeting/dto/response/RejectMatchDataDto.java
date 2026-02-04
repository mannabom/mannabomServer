package mannabom_server.manabom.application.meeting.dto.response;

import mannabom_server.manabom.domain.meeting.enums.MatchingStatus;

public record RejectMatchDataDto(MatchingStatus status, Integer remainingRejectCount, boolean canRematch) {
}
