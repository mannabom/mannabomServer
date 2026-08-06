package mannabom_server.manabom.application.report.dto.response;

public record CreateReportResponse(Long reportId) {

    public static CreateReportResponse of(Long reportId) {
        return new CreateReportResponse(reportId);
    }
}
