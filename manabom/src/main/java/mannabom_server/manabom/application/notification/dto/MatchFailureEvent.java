package mannabom_server.manabom.application.notification.dto;

import java.time.Instant;

public record MatchFailureEvent(
        Long matchId,
        Long triggerUserId, //거절버튼을 누른 리더
        Boolean isByTimeout
) {
}
