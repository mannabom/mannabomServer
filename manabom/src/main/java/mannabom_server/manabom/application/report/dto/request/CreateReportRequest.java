package mannabom_server.manabom.application.report.dto.request;

import lombok.Getter;
import mannabom_server.manabom.domain.report.entity.ReportReason;

@Getter
public class CreateReportRequest {
    private Long contextId;
    private Long targetId;
    private ReportReason reason;
    private String additionalDetail;
}
