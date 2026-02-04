package mannabom_server.manabom.application.notification.dto;

import java.time.Instant;

public record MatchFoundEvent(
        Long matchId,
        Instant decisionDeadline
) {
}
