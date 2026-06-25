package mannabom_server.manabom.application.admin.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.admin.enums.UserAccountStatus;
import mannabom_server.manabom.domain.report.entity.ReportStatus;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class AdminProcessReportRequest {
    @NotNull(message = "status는 필수입니다.")
    private ReportStatus status;

    private String adminComment;

    private UserAccountStatus targetAccountStatus;

    private LocalDateTime targetSuspendedUntil;

    private String targetAccountReason;

    @PositiveOrZero(message = "tingGrant는 0 이상이어야 합니다.")
    private Integer tingGrant = 0;

    @PositiveOrZero(message = "eventTingGrant는 0 이상이어야 합니다.")
    private Integer eventTingGrant = 0;

    private String walletReason;
}
