package mannabom_server.manabom.application.meeting.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.meeting.service.MeetingVerificationExpirationService;
import mannabom_server.manabom.domain.meeting.repository.MeetingVerificationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class MeetingVerificationScheduler {

    private final MeetingVerificationRepository verificationRepository;
    private final MeetingVerificationExpirationService expirationService;

    @Scheduled(fixedDelayString = "${meeting.verification.expiration-check-delay:60000}")
    public void notifyExpiredVerifications() {
        Instant now = Instant.now();
        for (Long verificationId : verificationRepository.findFailureNotificationTargetIds(now)) {
            try {
                expirationService.notifyFailure(verificationId, now);
            } catch (RuntimeException e) {
                log.warn("만남 인증 실패 알림 처리 실패: verificationId={}", verificationId, e);
            }
        }
    }
}
