package mannabom_server.manabom.application.meeting.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.meeting.service.MeetingCancellationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class MeetingCancellationScheduler {

    private final MeetingCancellationService meetingCancellationService;

    @Scheduled(fixedDelayString = "${meeting.cancellation.expiration-check-delay:60000}")
    public void expireCancellationRequests() {
        int expiredCount = meetingCancellationService.expirePendingRequests();
        if (expiredCount > 0) {
            log.info("만료된 미팅 전체 취소 요청 {}건을 처리했습니다.", expiredCount);
        }
    }
}
