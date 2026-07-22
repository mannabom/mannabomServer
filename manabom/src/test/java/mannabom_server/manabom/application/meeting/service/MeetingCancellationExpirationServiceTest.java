package mannabom_server.manabom.application.meeting.service;

import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import mannabom_server.manabom.domain.chat.repository.ChatMemberRepository;
import mannabom_server.manabom.domain.chat.repository.ChatRoomRepository;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.entity.MeetingCancellationRequest;
import mannabom_server.manabom.domain.meeting.entity.MeetingMatch;
import mannabom_server.manabom.domain.meeting.enums.MeetingCancellationStatus;
import mannabom_server.manabom.domain.meeting.repository.MeetingCancellationRequestRepository;
import mannabom_server.manabom.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingCancellationExpirationServiceTest {

    @Mock
    private MeetingCancellationRequestRepository requestRepository;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatMemberRepository chatMemberRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MeetingCancellationExpirationService expirationService;

    @Test
    void expiresPendingRequestInItsOwnProcessingStep() {
        Instant requestedAt = Instant.parse("2026-07-20T00:00:00Z");
        Instant now = requestedAt.plus(Duration.ofHours(25));
        MeetingCancellationRequest request = cancellationRequest(requestedAt);
        when(requestRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(request));
        when(chatRoomRepository.findByMatch(request.getMeetingMatch()))
                .thenReturn(Optional.of(ChatRoom.builder().id(100L).build()));

        boolean expired = expirationService.expire(30L, now);

        assertThat(expired).isTrue();
        assertThat(request.getStatus()).isEqualTo(MeetingCancellationStatus.EXPIRED);
        assertThat(request.getCompletedAt()).isEqualTo(now);
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
}
