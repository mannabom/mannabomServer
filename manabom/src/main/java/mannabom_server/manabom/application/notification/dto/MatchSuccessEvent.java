package mannabom_server.manabom.application.notification.dto;

import java.time.Instant;

public record MatchSuccessEvent(
    Long matchId,
    Long chatRoomId,
    boolean isByTimeout,
    Long triggerUserId
) {
}
