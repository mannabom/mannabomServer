package mannabom_server.manabom.domain.meeting;

import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.entity.MeetingMatch;
import mannabom_server.manabom.domain.meeting.enums.MatchingStatus;
import mannabom_server.manabom.domain.meeting.enums.MeetingDecision;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MeetingMatchExpirationTest {

    @Test
    void processesExpirationForBothWaitingTeams() {
        Meeting meeting1 = Meeting.builder()
                .id(10L)
                .remainingRejectCount(1)
                .build();
        Meeting meeting2 = Meeting.builder()
                .id(11L)
                .remainingRejectCount(1)
                .build();
        MeetingMatch match = MeetingMatch.builder()
                .meeting1(meeting1)
                .meeting2(meeting2)
                .build();

        match.processExpiration();

        assertThat(match.getMeeting1Decision()).isEqualTo(MeetingDecision.AUTO_REJECTED);
        assertThat(match.getMeeting2Decision()).isEqualTo(MeetingDecision.AUTO_REJECTED);
        assertThat(match.getMatchingStatus()).isEqualTo(MatchingStatus.FAILED);
        assertThat(meeting1.getRemainingRejectCount()).isZero();
        assertThat(meeting2.getRemainingRejectCount()).isZero();
    }
}
