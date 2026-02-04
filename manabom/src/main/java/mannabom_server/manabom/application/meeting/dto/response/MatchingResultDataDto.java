package mannabom_server.manabom.application.meeting.dto.response;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Builder;
import lombok.Getter;
import mannabom_server.manabom.application.meeting.dto.common.RegionDto;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.entity.MeetingMatch;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class MatchingResultDataDto {
    Long matchId;
    Instant decisionDeadline;
    OpponentTeamInfo opponentTeamInfo;


    public static MatchingResultDataDto of(MeetingMatch match, Meeting opponent,List<TeamMemberProfilesDto.TeamMemberDetailDto> members ){
        return MatchingResultDataDto.builder()
                .matchId(match.getId())
                .decisionDeadline(match.getDecisionDeadLine())
                .opponentTeamInfo(
                        OpponentTeamInfo.of(opponent,members)
                )
                .build();
    }

    @Getter
    @Builder
    public static class OpponentTeamInfo{
        Long meetingId;
        String roomName;
        RegionDto region;
        Double avgAge;
        @JsonUnwrapped
        TeamMemberProfilesDto members;

        public static OpponentTeamInfo of(Meeting meeting, List<TeamMemberProfilesDto.TeamMemberDetailDto> members){
            return OpponentTeamInfo.builder()
                    .meetingId(meeting.getId())
                    .roomName(meeting.getRoomName())
                    .region(
                            RegionDto.from(meeting.getRegion())
                    )
                    .avgAge(meeting.getAvgAge())
                    .members(TeamMemberProfilesDto.of(members))
                    .build();
        }
    }


}
