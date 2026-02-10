package mannabom_server.manabom.application.meeting.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.meeting.entity.Meeting;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingMatchingEvent {
    private Long meetingId;
    private String gender;
    private int memberCount;
    private String sidoCode;
    private String sigunguCode;
    private double avgage;
    private long matchingStartAtMs;

    public static MeetingMatchingEvent from(Meeting meeting){
        return MeetingMatchingEvent.builder()
                .sigunguCode(meeting.getRegion().getSigunguCode())
                .sidoCode(meeting.getRegion().getSidoCode())
                .meetingId(meeting.getId())
                .gender(meeting.getGender().toString())
                .memberCount(meeting.getMaxMembers())
                .avgage(meeting.getAvgAge())
                .matchingStartAtMs(System.currentTimeMillis())
                .build();
    }
}
