package mannabom_server.manabom.application.admin.dto.response;

import lombok.Getter;
import mannabom_server.manabom.domain.report.entity.ReportReason;
import mannabom_server.manabom.domain.report.entity.ReportStatus;
import mannabom_server.manabom.domain.report.entity.ReportType;

import java.time.Instant;

@Getter
public class AdminReportSummaryResponse {
    private final Long reportId;
    private final ReportType type;
    private final Long contextId;
    private final Long reporterId;
    private final String reporterName;
    private final String reporterNickName;
    private final Long targetId;
    private final String targetName;
    private final String targetNickName;
    private final ReportReason reason;
    private final ReportStatus status;
    private final Instant createdAt;
    private final Instant processedAt;

    public AdminReportSummaryResponse(Long reportId,
                                      ReportType type,
                                      Long contextId,
                                      Long reporterId,
                                      String reporterName,
                                      String reporterNickName,
                                      Long targetId,
                                      String targetName,
                                      String targetNickName,
                                      ReportReason reason,
                                      ReportStatus status,
                                      Instant createdAt,
                                      Instant processedAt) {
        this.reportId = reportId;
        this.type = type;
        this.contextId = contextId;
        this.reporterId = reporterId;
        this.reporterName = reporterName;
        this.reporterNickName = reporterNickName;
        this.targetId = targetId;
        this.targetName = targetName;
        this.targetNickName = targetNickName;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
    }
}
