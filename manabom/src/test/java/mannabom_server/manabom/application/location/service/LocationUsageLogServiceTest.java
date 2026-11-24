package mannabom_server.manabom.application.location.service;

import mannabom_server.manabom.domain.location.entity.LocationUsageLog;
import mannabom_server.manabom.domain.location.enums.LocationAcquisitionPath;
import mannabom_server.manabom.domain.location.enums.LocationUsagePurpose;
import mannabom_server.manabom.domain.location.repository.LocationUsageLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationUsageLogServiceTest {

    @Mock private LocationUsageLogRepository repository;
    @InjectMocks private LocationUsageLogService service;

    @Test
    void 만남_인증_위치_이용을_기기_GPS_취득으로_기록한다() {
        Instant usedAt = Instant.parse("2026-11-25T03:00:00Z");

        service.recordMeetingVerification(7L, 42L, usedAt);

        ArgumentCaptor<LocationUsageLog> captor = ArgumentCaptor.forClass(LocationUsageLog.class);
        verify(repository).save(captor.capture());
        LocationUsageLog saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(7L);
        assertThat(saved.getChatRoomId()).isEqualTo(42L);
        assertThat(saved.getPurpose()).isEqualTo(LocationUsagePurpose.MEETING_VERIFICATION);
        assertThat(saved.getAcquisitionPath()).isEqualTo(LocationAcquisitionPath.DEVICE_GPS);
        assertThat(saved.getUsedAt()).isEqualTo(usedAt);
    }

    @Test
    void 보관_기간_6개월이_지난_기록만_파기한다() {
        Instant now = Instant.parse("2026-11-25T03:00:00Z");
        when(repository.deleteAllUsedBefore(Instant.parse("2026-05-25T03:00:00Z"))).thenReturn(3);

        int deleted = service.purgeExpired(now);

        assertThat(deleted).isEqualTo(3);
    }

    @Test
    void 필수_값이_없으면_기록을_만들지_않는다() {
        assertThatThrownBy(() -> new LocationUsageLog(
                null, LocationUsagePurpose.MEETING_VERIFICATION, LocationAcquisitionPath.DEVICE_GPS, 1L, Instant.now()
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
