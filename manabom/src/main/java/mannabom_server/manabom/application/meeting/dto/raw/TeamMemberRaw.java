package mannabom_server.manabom.application.meeting.dto.raw;

import mannabom_server.manabom.domain.meeting.enums.MeetingRole;

public record TeamMemberRaw(Long userId, String nickName, String mainImageUrl, MeetingRole role) {
}
