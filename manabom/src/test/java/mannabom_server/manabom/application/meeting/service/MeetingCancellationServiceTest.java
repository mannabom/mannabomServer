package mannabom_server.manabom.application.meeting.service;

import mannabom_server.manabom.application.meeting.dto.response.MeetingCancellationResponse;
import mannabom_server.manabom.domain.chat.repository.ChatMemberRepository;
import mannabom_server.manabom.domain.chat.repository.ChatRoomRepository;
import mannabom_server.manabom.domain.meeting.entity.MeetingMatch;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.entity.MeetingCancellationRequest;
import mannabom_server.manabom.domain.meeting.entity.MeetingCancellationVote;
import mannabom_server.manabom.domain.meeting.entity.MeetingMember;
import mannabom_server.manabom.domain.meeting.enums.CancellationVoteDecision;
import mannabom_server.manabom.domain.meeting.enums.ChatUserStatus;
import mannabom_server.manabom.domain.meeting.enums.MatchingStatus;
import mannabom_server.manabom.domain.meeting.enums.MeetingCancellationStatus;
import mannabom_server.manabom.domain.meeting.repository.MeetingCancellationRequestRepository;
import mannabom_server.manabom.domain.meeting.repository.MeetingCancellationVoteRepository;
import mannabom_server.manabom.domain.meeting.repository.MeetingMemberRepository;
import mannabom_server.manabom.domain.meeting.repository.MeetingMatchRepository;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.global.error.MeetingCancellationExpiredException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class MeetingCancellationServiceTest {

    @Mock
    private MeetingMatchRepository meetingMatchRepository;
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
    @Mock
    private ChatMemberRepository chatMemberRepository;
    @Mock
    private MeetingCancellationExpirationService expirationService;

    @InjectMocks
    private MeetingCancellationService meetingCancellationService;

    @Test
    void blocksMembershipChangesWhileCancellationVoteIsPending() {
        MeetingMatch match = mock(MeetingMatch.class);
        when(match.getId()).thenReturn(20L);
        when(meetingMatchRepository.findByMeetingIdAndStatus(
                10L,
                MatchingStatus.SUCCEEDED
        )).thenReturn(Optional.of(match));
        when(requestRepository.existsByMeetingMatch_IdAndStatus(
                20L,
                MeetingCancellationStatus.PENDING
        )).thenReturn(true);

        assertThatThrownBy(() ->
                meetingCancellationService.validateNoPendingCancellation(10L)
        ).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("취소 투표가 진행 중");
    }

    @Test
    void allowsMembershipChangesWithoutPendingCancellationVote() {
        when(meetingMatchRepository.findByMeetingIdAndStatus(
                10L,
                MatchingStatus.SUCCEEDED
        )).thenReturn(Optional.empty());

        assertThatCode(() ->
                meetingCancellationService.validateNoPendingCancellation(10L)
        ).doesNotThrowAnyException();
    }

    @Test
    void createsVotesForMembersOfBothMatchedTeams() {
        Meeting meeting1 = mock(Meeting.class);
        Meeting meeting2 = mock(Meeting.class);
        MeetingMatch match = mock(MeetingMatch.class);
        User initiator = user(1L);

        when(match.getId()).thenReturn(20L);
        when(match.getMeeting1()).thenReturn(meeting1);
        when(match.getMeeting2()).thenReturn(meeting2);
        when(match.getMatchingStatus()).thenReturn(MatchingStatus.SUCCEEDED);
        when(meeting1.getId()).thenReturn(10L);
        when(meeting2.getId()).thenReturn(11L);
        when(meetingMatchRepository.findByIdWithLockAndMeeting(20L))
                .thenReturn(Optional.of(match));
        when(meetingMemberRepository.existsByMeeting_IdAndUser_UserIdAndStatus(
                10L, 1L, ChatUserStatus.ACTIVE
        )).thenReturn(true);
        when(meetingMemberRepository.findByMeetingIdAndStatus(10L, ChatUserStatus.ACTIVE))
                .thenReturn(List.of(member(meeting1, initiator), member(meeting1, user(2L)), member(meeting1, user(3L))));
        when(meetingMemberRepository.findByMeetingIdAndStatus(11L, ChatUserStatus.ACTIVE))
                .thenReturn(List.of(member(meeting2, user(4L)), member(meeting2, user(5L)), member(meeting2, user(6L))));
        when(userRepository.findById(1L)).thenReturn(Optional.of(initiator));
        when(requestRepository.save(any(MeetingCancellationRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = meetingCancellationService.create(20L, 1L);

        assertThatCode(() -> response.getVotes()).doesNotThrowAnyException();
        org.assertj.core.api.Assertions.assertThat(response.getMatchId()).isEqualTo(20L);
        org.assertj.core.api.Assertions.assertThat(response.getTotalMemberCount()).isEqualTo(6);
        org.assertj.core.api.Assertions.assertThat(response.getAgreedMemberCount()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(response.getPendingMemberCount()).isEqualTo(5);
    }

    @Test
    void unanimousApprovalCancelsBothMatchedTeams() {
        Meeting meeting1 = mock(Meeting.class);
        Meeting meeting2 = mock(Meeting.class);
        MeetingMatch match = mock(MeetingMatch.class);
        User voter = user(2L);
        Instant now = Instant.now();
        MeetingCancellationRequest request = MeetingCancellationRequest.create(
                match,
                user(1L),
                now,
                now.plusSeconds(3600)
        );
        ReflectionTestUtils.setField(request, "id", 30L);
        MeetingCancellationVote vote = MeetingCancellationVote.pending(request, voter);

        when(match.getId()).thenReturn(20L);
        when(match.getMeeting1()).thenReturn(meeting1);
        when(match.getMeeting2()).thenReturn(meeting2);
        when(meeting1.getId()).thenReturn(10L);
        when(meeting2.getId()).thenReturn(11L);
        when(requestRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(request));
        when(voteRepository.findByRequest_IdAndUser_UserId(30L, 2L)).thenReturn(Optional.of(vote));
        when(voteRepository.countByRequest_IdAndDecision(
                30L,
                CancellationVoteDecision.PENDING
        )).thenReturn(0L);
        when(voteRepository.countByRequest_IdAndDecision(
                30L,
                CancellationVoteDecision.REJECT
        )).thenReturn(0L);
        when(voteRepository.findAllByRequest_IdOrderById(30L)).thenReturn(List.of(vote));
        when(chatRoomRepository.findByMatch(match)).thenReturn(Optional.empty());
        when(chatRoomRepository.findByMeeting(meeting1)).thenReturn(Optional.empty());
        when(chatRoomRepository.findByMeeting(meeting2)).thenReturn(Optional.empty());
        when(meetingMemberRepository.findByMeetingIdAndStatus(10L, ChatUserStatus.ACTIVE))
                .thenReturn(List.of());
        when(meetingMemberRepository.findByMeetingIdAndStatus(11L, ChatUserStatus.ACTIVE))
                .thenReturn(List.of());

        meetingCancellationService.vote(30L, 2L, CancellationVoteDecision.AGREE);

        verify(meeting1).cancelByAgreement();
        verify(meeting2).cancelByAgreement();
    }

    @Test
    void expiredRequestIsMarkedExpiredWhenVoting() {
        Instant requestedAt = Instant.now().minus(Duration.ofHours(25));
        MeetingCancellationRequest request = MeetingCancellationRequest.create(
                mock(MeetingMatch.class),
                user(1L),
                requestedAt,
                requestedAt.plus(Duration.ofHours(24))
        );
        when(requestRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> meetingCancellationService.vote(
                30L,
                2L,
                CancellationVoteDecision.AGREE
        ))
                .isInstanceOf(MeetingCancellationExpiredException.class)
                .hasMessage("이미 만료된 미팅 취소 요청입니다.");

        assertThat(request.getStatus()).isEqualTo(MeetingCancellationStatus.EXPIRED);
        assertThat(request.getCompletedAt()).isNotNull();
        verifyNoInteractions(voteRepository);
    }

    @Test
    void continuesExpiringOtherRequestsWhenOneRequestFails() {
        when(requestRepository.findExpiredRequestIds(
                any(MeetingCancellationStatus.class),
                any(Instant.class)
        )).thenReturn(List.of(30L, 31L));
        when(expirationService.expire(eq(30L), any(Instant.class)))
                .thenThrow(new RuntimeException("optimistic lock conflict"));
        when(expirationService.expire(eq(31L), any(Instant.class)))
                .thenReturn(true);

        int expiredCount = meetingCancellationService.expirePendingRequests();

        assertThat(expiredCount).isEqualTo(1);
        verify(expirationService).expire(eq(30L), any(Instant.class));
        verify(expirationService).expire(eq(31L), any(Instant.class));
    }

    private MeetingMember member(Meeting meeting, User user) {
        return MeetingMember.addMember(meeting, user);
    }

    private User user(Long id) {
        return User.builder()
                .userId(id)
                .kakaoId("mock_" + id)
                .userName("user" + id)
                .build();
    }
}
