package mannabom_server.manabom.application.meeting.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Builder;
import lombok.Getter;
import mannabom_server.manabom.application.meeting.dto.common.RegionDto;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.entity.MeetingMatch;
import mannabom_server.manabom.domain.meeting.enums.MatchingStatus;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MatchingResultDataDto {
    Long matchId;
    MatchingStatus matchingStatus;
    boolean requeued;
    Instant decisionDeadline;
    OpponentTeamInfo opponentTeamInfo;

    public static MatchingResultDataDto failed(MeetingMatch match) {
        return MatchingResultDataDto.builder()
                .matchId(match.getId())
                .matchingStatus(MatchingStatus.FAILED)
                .requeued(true)
                .build();
    }

    public static MatchingResultDataDto withOpponent(
            MeetingMatch match,
            Meeting opponent,
            List<TeamMemberProfilesDto.TeamMemberDetailDto> members
    ) {
        return MatchingResultDataDto.builder()
                .matchId(match.getId())
                .matchingStatus(match.getMatchingStatus())
                .requeued(false)
                .decisionDeadline(match.getDecisionDeadLine())
                .opponentTeamInfo(OpponentTeamInfo.of(opponent, members))
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
