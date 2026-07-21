package mannabom_server.manabom.application.meeting.service;

import mannabom_server.manabom.domain.chat.repository.ChatRoomRepository;
import mannabom_server.manabom.domain.meeting.enums.MeetingCancellationStatus;
import mannabom_server.manabom.domain.meeting.repository.MeetingCancellationRequestRepository;
import mannabom_server.manabom.domain.meeting.repository.MeetingCancellationVoteRepository;
import mannabom_server.manabom.domain.meeting.repository.MeetingMemberRepository;
import mannabom_server.manabom.domain.meeting.repository.MeetingRepository;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingCancellationServiceTest {

    @Mock
    private MeetingRepository meetingRepository;
    @Mock
    private MeetingMemberRepository meetingMemberRepository;
    @Mock
    private MeetingCancellationRequestRepository requestRepository;
    @Mock
    private MeetingCancellationVoteRepository voteRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ChatRoomRepository chatRoomRepository;

    @InjectMocks
    private MeetingCancellationService meetingCancellationService;

    @Test
    void blocksMembershipChangesWhileCancellationVoteIsPending() {
        when(requestRepository.existsByMeeting_IdAndStatus(
                10L,
                MeetingCancellationStatus.PENDING
        )).thenReturn(true);

        assertThatThrownBy(() ->
                meetingCancellationService.validateNoPendingCancellation(10L)
        ).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("취소 투표가 진행 중");
    }

    @Test
    void allowsMembershipChangesWithoutPendingCancellationVote() {
        when(requestRepository.existsByMeeting_IdAndStatus(
                10L,
                MeetingCancellationStatus.PENDING
        )).thenReturn(false);

        assertThatCode(() ->
                meetingCancellationService.validateNoPendingCancellation(10L)
        ).doesNotThrowAnyException();
    }
}
