package mannabom_server.manabom.application.meeting.service;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.chat.dto.event.ChatSystemMessageEvent;
import mannabom_server.manabom.application.chat.message.SystemMessageType;
import mannabom_server.manabom.domain.meeting.entity.MeetingVerification;
import mannabom_server.manabom.domain.meeting.repository.MeetingVerificationRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MeetingVerificationExpirationService {

    private final MeetingVerificationRepository verificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean notifyFailure(Long verificationId, Instant now) {
        MeetingVerification verification = verificationRepository.findByIdForUpdate(verificationId)
                .orElse(null);
        if (verification == null || !verification.needsFailureNotification(now)) {
            return false;
        }

        verification.markFailureNotified(now);
        eventPublisher.publishEvent(ChatSystemMessageEvent.of(
                verification.getRoom().getId(),
                SystemMessageType.MEETING_VERIFICATION_FAILED,
                null,
                null,
                Map.of("expiredAt", verification.getExpiresAt().toString())
        ));
        return true;
    }
}
