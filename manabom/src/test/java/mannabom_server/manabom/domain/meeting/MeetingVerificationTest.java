package mannabom_server.manabom.domain.meeting;

import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import mannabom_server.manabom.domain.meeting.entity.MeetingVerification;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MeetingVerificationTest {

    @Test
    void persistsSuccessfulClusterSnapshotSeparatelyFromAllSubmissions() {
        MeetingVerification verification = MeetingVerification.builder()
                .room(ChatRoom.builder().id(1L).build())
                .build();
        verification.updateParticipantCount(4);

        verification.verify(3, 37.5665, 126.9780, true, true);

        assertThat(verification.getParticipantCount()).isEqualTo(4);
        assertThat(verification.getVerifiedParticipantCount()).isEqualTo(3);
        assertThat(verification.getFinalLatitude()).isEqualTo(37.5665);
        assertThat(verification.getFinalLongitude()).isEqualTo(126.9780);
        assertThat(verification.isVerifiedHasMale()).isTrue();
        assertThat(verification.isVerifiedHasFemale()).isTrue();
    }

    @Test
    void countsVerifiedLatecomerAsSubmittedAndVerified() {
        MeetingVerification verification = MeetingVerification.builder()
                .room(ChatRoom.builder().id(1L).build())
                .build();
        verification.updateParticipantCount(4);
        verification.verify(3, 37.5665, 126.9780, true, true);

        verification.recordVerifiedLatecomer(true);

        assertThat(verification.getParticipantCount()).isEqualTo(5);
        assertThat(verification.getVerifiedParticipantCount()).isEqualTo(4);
    }

    @Test
    void doesNotCountExistingSubmitterTwiceWhenJoiningAfterSuccess() {
        MeetingVerification verification = MeetingVerification.builder()
                .room(ChatRoom.builder().id(1L).build())
                .build();
        verification.updateParticipantCount(4);
        verification.verify(3, 37.5665, 126.9780, true, true);

        verification.recordVerifiedLatecomer(false);

        assertThat(verification.getParticipantCount()).isEqualTo(4);
        assertThat(verification.getVerifiedParticipantCount()).isEqualTo(4);
    }
}
