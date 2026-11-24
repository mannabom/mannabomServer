package mannabom_server.manabom.application.location.service;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.domain.location.entity.LocationUsageLog;
import mannabom_server.manabom.domain.location.enums.LocationAcquisitionPath;
import mannabom_server.manabom.domain.location.enums.LocationUsagePurpose;
import mannabom_server.manabom.domain.location.repository.LocationUsageLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;

/**
 * 위치정보 이용·제공사실 확인자료를 자동 기록하고 보관 기간이 지나면 파기한다 (위치정보법 제16조 제2항).
 */
@Service
@RequiredArgsConstructor
public class LocationUsageLogService {

    static final Period RETENTION = Period.ofMonths(6);

    private final LocationUsageLogRepository locationUsageLogRepository;

    /**
     * 위치를 받아 이용한 사실은 이후 인증이 실패해 원래 트랜잭션이 롤백돼도 남아야 하므로 별도 트랜잭션으로 기록한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordMeetingVerification(Long userId, Long chatRoomId, Instant usedAt) {
        locationUsageLogRepository.save(new LocationUsageLog(
                userId,
                LocationUsagePurpose.MEETING_VERIFICATION,
                LocationAcquisitionPath.DEVICE_GPS,
                chatRoomId,
                usedAt
        ));
    }

    @Transactional
    public int purgeExpired(Instant now) {
        return locationUsageLogRepository.deleteAllUsedBefore(retentionStart(now));
    }

    static Instant retentionStart(Instant now) {
        return now.atZone(ZoneOffset.UTC).minus(RETENTION).toInstant();
    }
}
