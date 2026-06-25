package mannabom_server.manabom.application.admin.dto.response;

import lombok.Builder;
import lombok.Getter;
import mannabom_server.manabom.domain.admin.enums.UserAccountStatus;
import mannabom_server.manabom.domain.report.entity.ReportReason;
import mannabom_server.manabom.domain.report.entity.ReportStatus;
import mannabom_server.manabom.domain.report.entity.ReportType;
import mannabom_server.manabom.domain.user.enums.Gender;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
public class AdminReportDetailResponse {
    private final Long reportId;
    private final ReportType type;
    private final Long contextId;
    private final ReportReason reason;
    private final String additionalDetail;
    private final ReportStatus status;
    private final String adminComment;
    private final Instant processedAt;
    private final Instant createdAt;
    private final UserSnapshot reporter;
    private final UserSnapshot target;
    private final Map<String, Object> referenceContext;

    @Getter
    @Builder
    public static class UserSnapshot {
        private final Long userId;
        private final Long profileId;
        private final String kakaoId;
        private final String userName;
        private final String phoneNum;
        private final String nickName;
        private final Gender gender;
        private final LocalDate birthDate;
        private final String universityName;
        private final String regionSidoName;
        private final String regionSigunguName;
        private final Boolean verified;
        private final UserAccountStatus accountStatus;
        private final String statusReason;
        private final LocalDateTime statusSuspendedUntil;
        private final Integer ting;
        private final Integer eventTing;
        private final LocalDateTime membershipActiveUntil;
    }
}
