package mannabom_server.manabom.application.notification.dto;

import java.time.Instant;

public record MatchSuccessEvent(
    Long matchId,
    Long chatRoomId,
    boolean isByTimeout //타임 아웃에 의한 성사인지
) {
}
