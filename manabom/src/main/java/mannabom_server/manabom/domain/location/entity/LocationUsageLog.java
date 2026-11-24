package mannabom_server.manabom.domain.location.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.location.enums.LocationAcquisitionPath;
import mannabom_server.manabom.domain.location.enums.LocationUsagePurpose;

import java.time.Instant;

/**
 * 위치정보 이용·제공사실 확인자료. 생성 후 수정하지 않는다.
 */
@Getter
@Entity
@Table(name = "location_usage_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LocationUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_usage_log_id")
    private Long locationUsageLogId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, updatable = false, length = 50)
    private LocationUsagePurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "acquisition_path", nullable = false, updatable = false, length = 30)
    private LocationAcquisitionPath acquisitionPath;

    @Column(name = "chat_room_id", updatable = false)
    private Long chatRoomId;

    @Column(name = "used_at", nullable = false, updatable = false)
    private Instant usedAt;

    public LocationUsageLog(
            Long userId,
            LocationUsagePurpose purpose,
            LocationAcquisitionPath acquisitionPath,
            Long chatRoomId,
            Instant usedAt
    ) {
        if (userId == null || purpose == null || acquisitionPath == null || usedAt == null) {
            throw new IllegalArgumentException("위치정보 이용 기록의 사용자, 목적, 취득 경로, 이용 일시는 필수입니다.");
        }
        this.userId = userId;
        this.purpose = purpose;
        this.acquisitionPath = acquisitionPath;
        this.chatRoomId = chatRoomId;
        this.usedAt = usedAt;
    }
}
