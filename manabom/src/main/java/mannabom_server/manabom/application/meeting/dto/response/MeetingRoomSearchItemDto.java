package mannabom_server.manabom.application.meeting.dto.response;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MeetingRoomSearchItemDto{

    @JsonUnwrapped
    private MeetingInfo meetingInfo;
    private List<TeamMemberPreviewDto> membersPreview;
}
