package mannabom_server.manabom.application.location.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.location.service.LocationUsageLogService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class LocationUsageLogPurgeScheduler {

    private final LocationUsageLogService locationUsageLogService;

    @Scheduled(cron = "${location.usage-log.purge-cron:0 30 4 * * *}", zone = "Asia/Seoul")
    public void purgeExpiredLogs() {
        int deleted = locationUsageLogService.purgeExpired(Instant.now());
        if (deleted > 0) {
            log.info("보관 기간이 지난 위치정보 이용 기록 파기: {}건", deleted);
        }
    }
}
