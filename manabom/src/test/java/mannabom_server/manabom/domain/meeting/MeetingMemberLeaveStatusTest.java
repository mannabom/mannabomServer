package mannabom_server.manabom.domain.meeting;

import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.enums.MeetingStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MeetingMemberLeaveStatusTest {

    @Test
    void matchedMeetingChangesToFastMatchingAfterMemberLeaves() {
        Meeting meeting = meeting(MeetingStatus.MATCHED, 3, 3);

        meeting.deleteMember(25);
        meeting.changeToFastMatchingAfterMemberLeave();

        assertThat(meeting.getCurrentMembers()).isEqualTo(2);
        assertThat(meeting.getMeetingStatus()).isEqualTo(MeetingStatus.FASTMATCHING);
    }

    @Test
    void fullMeetingChangesToRecruitingAfterMemberLeaves() {
        Meeting meeting = meeting(MeetingStatus.FULL, 3, 3);

        meeting.deleteMember(25);
        meeting.changeToRecruitingAfterMemberLeave();

        assertThat(meeting.getCurrentMembers()).isEqualTo(2);
        assertThat(meeting.getMeetingStatus()).isEqualTo(MeetingStatus.RECRUITING);
    }

    @Test
    void recruitingMeetingCannotBeChangedToFastMatching() {
        Meeting meeting = meeting(MeetingStatus.RECRUITING, 2, 3);

        assertThatThrownBy(meeting::changeToFastMatchingAfterMemberLeave)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void fastMatchingMeetingReturnsToMatchedWhenVacancyIsFilled() {
        Meeting meeting = meeting(MeetingStatus.FASTMATCHING, 2, 3);

        meeting.addMember(25);

        assertThat(meeting.getCurrentMembers()).isEqualTo(3);
        assertThat(meeting.getMeetingStatus()).isEqualTo(MeetingStatus.MATCHED);
    }

    private Meeting meeting(
            MeetingStatus status,
            int currentMembers,
            int maxMembers
    ) {
        return Meeting.builder()
                .meetingStatus(status)
                .currentMembers(currentMembers)
                .maxMembers(maxMembers)
                .avgAge(25)
                .build();
    }
}
