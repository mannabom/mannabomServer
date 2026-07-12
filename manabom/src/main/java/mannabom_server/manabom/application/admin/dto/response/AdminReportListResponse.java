package mannabom_server.manabom.application.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminReportListResponse {
    private final List<AdminReportSummaryResponse> reports;
    private final long totalCount;
    private final int totalPages;
    private final int page;
    private final int size;
}
