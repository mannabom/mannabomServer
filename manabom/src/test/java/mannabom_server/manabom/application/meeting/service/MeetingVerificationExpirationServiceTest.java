package mannabom_server.manabom.application.meeting.service;

import mannabom_server.manabom.application.chat.dto.event.ChatSystemMessageEvent;
import mannabom_server.manabom.application.chat.message.SystemMessageType;
import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import mannabom_server.manabom.domain.meeting.entity.MeetingVerification;
import mannabom_server.manabom.domain.meeting.repository.MeetingVerificationRepository;
import mannabom_server.manabom.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingVerificationExpirationServiceTest {

    @Mock private MeetingVerificationRepository verificationRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private MeetingVerificationExpirationService expirationService;

    @Test
    void publishesFailureOnlyOnceAfterExpiration() {
        Instant startedAt = Instant.parse("2026-07-22T00:00:00Z");
        Instant expiredAt = startedAt.plus(Duration.ofHours(9));
        MeetingVerification verification = MeetingVerification.builder()
                .room(ChatRoom.builder().id(77L).build())
                .build();
        verification.startIfNeeded(
                startedAt,
                Duration.ofHours(9),
                User.builder().userId(11L).kakaoId("kakao-11").userName("사용자11").build()
        );
        verification.updateParticipantCount(3);
        when(verificationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(verification));

        assertThat(expirationService.notifyFailure(1L, expiredAt)).isTrue();
        assertThat(expirationService.notifyFailure(1L, expiredAt.plusSeconds(1))).isFalse();

        ArgumentCaptor<ChatSystemMessageEvent> captor =
                ArgumentCaptor.forClass(ChatSystemMessageEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().roomId()).isEqualTo(77L);
        assertThat(captor.getValue().type()).isEqualTo(SystemMessageType.MEETING_VERIFICATION_FAILED);
        assertThat(captor.getValue().actorUserId()).isEqualTo(11L);
        assertThat(captor.getValue().data()).containsEntry("participantCount", 3);
    }
}
