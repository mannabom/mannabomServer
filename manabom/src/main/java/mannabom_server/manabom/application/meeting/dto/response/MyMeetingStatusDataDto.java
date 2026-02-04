package mannabom_server.manabom.application.meeting.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MyMeetingStatusDataDto{
    private boolean hasActiveRoom;
    
    @JsonUnwrapped
    private MeetingChatRoomInfo meetingChatRoomInfo;
}
