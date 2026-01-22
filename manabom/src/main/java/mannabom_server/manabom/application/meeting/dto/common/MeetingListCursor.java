package mannabom_server.manabom.application.meeting.dto.common;

import java.time.Instant;

public record MeetingListCursor(
        Integer score,
        Instant createdAt,
        Long id
) {
}
