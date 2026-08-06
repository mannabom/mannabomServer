package mannabom_server.manabom.application.meeting.service;

import mannabom_server.manabom.application.chat.dto.event.ChatSystemMessageEvent;
import mannabom_server.manabom.application.chat.message.SystemMessageType;
import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import mannabom_server.manabom.domain.chat.repository.ChatRoomRepository;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.entity.MeetingCancellationRequest;
import mannabom_server.manabom.domain.meeting.entity.MeetingMatch;
import mannabom_server.manabom.domain.meeting.entity.MeetingMember;
import mannabom_server.manabom.domain.meeting.enums.ChatUserStatus;
import mannabom_server.manabom.domain.meeting.enums.MeetingCancellationStatus;
import mannabom_server.manabom.domain.meeting.repository.MeetingCancellationRequestRepository;
import mannabom_server.manabom.domain.meeting.repository.MeetingMemberRepository;
import mannabom_server.manabom.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingCancellationExpirationServiceTest {

    @Mock
    private MeetingCancellationRequestRepository requestRepository;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private MeetingMemberRepository meetingMemberRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MeetingCancellationExpirationService expirationService;

    @Test
    void expiresPendingRequestInItsOwnProcessingStep() {
        Instant requestedAt = Instant.parse("2026-07-20T00:00:00Z");
        Instant now = requestedAt.plus(Duration.ofHours(25));
        MeetingCancellationRequest request = cancellationRequest(requestedAt);
        ReflectionTestUtils.setField(request, "id", 30L);
        MeetingMatch match = request.getMeetingMatch();
        User user1 = user(1L);
        User user2 = user(2L);
        when(requestRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(request));
        when(chatRoomRepository.findByMatch(match))
                .thenReturn(Optional.of(ChatRoom.builder().id(100L).build()));
        when(meetingMemberRepository.findByMeetingIdAndStatus(10L, ChatUserStatus.ACTIVE))
                .thenReturn(List.of(MeetingMember.addLeader(match.getMeeting1(), user1)));
        when(meetingMemberRepository.findByMeetingIdAndStatus(11L, ChatUserStatus.ACTIVE))
                .thenReturn(List.of(MeetingMember.addLeader(match.getMeeting2(), user2)));

        boolean expired = expirationService.expire(30L, now);

        assertThat(expired).isTrue();
        assertThat(request.getStatus()).isEqualTo(MeetingCancellationStatus.EXPIRED);
        assertThat(request.getCompletedAt()).isEqualTo(now);
        ArgumentCaptor<ChatSystemMessageEvent> eventCaptor =
                ArgumentCaptor.forClass(ChatSystemMessageEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        ChatSystemMessageEvent event = eventCaptor.getValue();
        assertThat(event.roomId()).isEqualTo(100L);
        assertThat(event.type()).isEqualTo(SystemMessageType.MEETING_CANCELLATION_EXPIRED);
        assertThat(event.recipientUserIds()).containsExactly(1L, 2L);
        assertThat(event.data())
                .containsEntry("requestId", 30L)
                .containsEntry("status", MeetingCancellationStatus.EXPIRED.name())
                .containsEntry("expiresAt", request.getExpiresAt().toString());
    }

    @Test
    void skipsRequestThatWasCompletedBeforeLockAcquisition() {
        Instant requestedAt = Instant.parse("2026-07-20T00:00:00Z");
        Instant now = requestedAt.plus(Duration.ofHours(25));
        MeetingCancellationRequest request = cancellationRequest(requestedAt);
        request.reject(now.minusSeconds(1));
        when(requestRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(request));

        boolean expired = expirationService.expire(30L, now);

        assertThat(expired).isFalse();
        assertThat(request.getStatus()).isEqualTo(MeetingCancellationStatus.REJECTED);
    }

    private MeetingCancellationRequest cancellationRequest(Instant requestedAt) {
        MeetingMatch match = MeetingMatch.builder()
                .meeting1(Meeting.builder().id(10L).build())
                .meeting2(Meeting.builder().id(11L).build())
                .build();
        User initiator = User.builder()
                .userId(1L)
                .kakaoId("mock_1")
                .userName("user1")
                .build();
        return MeetingCancellationRequest.create(
                match,
                initiator,
                requestedAt,
                requestedAt.plus(Duration.ofHours(24))
        );
    }

    private User user(Long id) {
        return User.builder()
                .userId(id)
                .kakaoId("mock_" + id)
                .userName("user" + id)
                .build();
    }
}
