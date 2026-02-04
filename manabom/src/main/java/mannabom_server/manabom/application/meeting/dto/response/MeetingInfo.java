package mannabom_server.manabom.application.meeting.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import lombok.Getter;
import mannabom_server.manabom.application.meeting.dto.common.AgeRangeDto;
import mannabom_server.manabom.application.meeting.dto.common.MemberInfo;
import mannabom_server.manabom.application.meeting.dto.common.RegionDto;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.enums.MeetingStatus;
import mannabom_server.manabom.domain.user.enums.Gender;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MeetingInfo {
    private Long meetingId;
    private MeetingStatus meetingStatus;
    private String roomName;
    private String roomCode;
    private Gender gender;
    private RegionDto region;
    private MemberInfo memberInfo;
    private AgeRangeDto ageRangeDto;



    public static MeetingInfo of(Meeting meeting ){
        MemberInfo info = MemberInfo.builder().maxCount(meeting.getMaxMembers()).currentCount(meeting.getCurrentMembers()).build();
        return MeetingInfo.builder()
                .meetingId(meeting.getId())
                .meetingStatus(meeting.getMeetingStatus())
                .roomCode(meeting.getCode())
                .gender(meeting.getGender())
                .ageRangeDto(new AgeRangeDto(meeting.getMinAge(), meeting.getMaxAge()))
                .roomName(meeting.getRoomName())
                .memberInfo(info)
                .region(new RegionDto(meeting.getRegion().getSidoName(), meeting.getRegion().getSigunguName()))
                .build();
    }

    public static MeetingInfo ofSummary(Meeting meeting){
        MemberInfo info = MemberInfo.builder().maxCount(meeting.getMaxMembers()).currentCount(meeting.getCurrentMembers()).build();
        return MeetingInfo.builder()
                .meetingId(meeting.getId())
                .meetingStatus(meeting.getMeetingStatus())
                .ageRangeDto(new AgeRangeDto(meeting.getMinAge(), meeting.getMaxAge()))
                .roomName(meeting.getRoomName())
                .memberInfo(info)
                .region(new RegionDto(meeting.getRegion().getSidoName(), meeting.getRegion().getSigunguName()))
                .build();
    }
}
