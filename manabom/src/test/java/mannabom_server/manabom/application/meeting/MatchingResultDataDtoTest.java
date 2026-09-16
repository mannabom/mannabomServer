package mannabom_server.manabom.application.meeting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import mannabom_server.manabom.application.meeting.dto.response.MatchingResultDataDto;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.entity.MeetingMatch;
import mannabom_server.manabom.domain.meeting.enums.MatchingStatus;
import mannabom_server.manabom.domain.region.entity.Region;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MatchingResultDataDtoTest {

    @Test
    void exposesFailedAndRequeuedStatusWithoutOpponentAfterTimeout() throws JsonProcessingException {
        Region region = Region.builder()
                .sidoName("서울특별시")
                .sigunguName("강남구")
                .build();
        Meeting meeting1 = Meeting.builder()
                .id(10L)
                .region(region)
                .remainingRejectCount(1)
                .build();
        Meeting meeting2 = Meeting.builder()
                .id(11L)
                .region(region)
                .remainingRejectCount(1)
                .build();
        MeetingMatch match = MeetingMatch.builder()
                .meeting1(meeting1)
                .meeting2(meeting2)
                .build();
        match.processExpiration();

        MatchingResultDataDto response = MatchingResultDataDto.failed(match);

        assertThat(response.getMatchingStatus()).isEqualTo(MatchingStatus.FAILED);
        assertThat(response.isRequeued()).isTrue();
        assertThat(response.getOpponentTeamInfo()).isNull();
        assertThat(new ObjectMapper().findAndRegisterModules().writeValueAsString(response))
                .doesNotContain("opponentTeamInfo");
    }

    @Test
    void doesNotExposeRequeuedWhileDecisionIsPending() {
        Region region = Region.builder()
                .sidoName("서울특별시")
                .sigunguName("강남구")
                .build();
        Meeting meeting1 = Meeting.builder().id(10L).region(region).build();
        Meeting meeting2 = Meeting.builder().id(11L).region(region).build();
        MeetingMatch match = MeetingMatch.builder()
                .meeting1(meeting1)
                .meeting2(meeting2)
                .build();

        MatchingResultDataDto response = MatchingResultDataDto.withOpponent(match, meeting2, List.of());

        assertThat(response.getMatchingStatus()).isEqualTo(MatchingStatus.PENDING);
        assertThat(response.isRequeued()).isFalse();
        assertThat(response.getOpponentTeamInfo()).isNotNull();
    }
}
