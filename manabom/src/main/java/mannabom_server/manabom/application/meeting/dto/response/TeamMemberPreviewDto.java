package mannabom_server.manabom.application.meeting.dto.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TeamMemberPreviewDto {
    private Long userId;
    private String profileImage;

}
