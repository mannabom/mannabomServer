package mannabom_server.manabom.application.meeting.dto.response;

import lombok.Builder;
import lombok.Getter;
import mannabom_server.manabom.application.meeting.dto.common.MeetingChatRoomInfo;

@Getter
@Builder
public class MeetingRoomCreateDataDto {
    private Integer creationCost;
    private Integer remainingPoints;
    private MeetingChatRoomInfo meetingChatRoomInfo;



    public static MeetingRoomCreateDataDto of( MeetingChatRoomInfo meetingChatRoomInfo){
        return MeetingRoomCreateDataDto.builder()
                .meetingChatRoomInfo(meetingChatRoomInfo)
                .build();
    }
}
