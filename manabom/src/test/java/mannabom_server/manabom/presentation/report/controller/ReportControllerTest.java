package mannabom_server.manabom.presentation.report.controller;

import mannabom_server.manabom.application.common.dto.ApiResponse;
import mannabom_server.manabom.application.report.dto.request.CreateProfileReportRequest;
import mannabom_server.manabom.application.report.dto.response.CreateReportResponse;
import mannabom_server.manabom.application.report.service.ReportService;
import mannabom_server.manabom.domain.report.entity.ReportReason;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportControllerTest {

    private final ReportService reportService = mock(ReportService.class);
    private final ReportController reportController = new ReportController(reportService);

    @Test
    void returnsCreatedReportIdForProfileReport() {
        CreateProfileReportRequest request = CreateProfileReportRequest.builder()
                .profileId(200L)
                .reason(ReportReason.INAPPROPRIATE_PROFILE)
                .build();
        when(reportService.createProfileReport(1L, request)).thenReturn(300L);

        ResponseEntity<ApiResponse<CreateReportResponse>> response =
                reportController.reportProfile(request, 1L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().reportId()).isEqualTo(300L);
    }
}
