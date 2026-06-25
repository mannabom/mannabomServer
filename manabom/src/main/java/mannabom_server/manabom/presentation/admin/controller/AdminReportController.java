package mannabom_server.manabom.presentation.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.admin.dto.request.AdminProcessReportRequest;
import mannabom_server.manabom.application.admin.dto.response.AdminReportDetailResponse;
import mannabom_server.manabom.application.admin.dto.response.AdminReportListResponse;
import mannabom_server.manabom.application.admin.service.AdminReportService;
import mannabom_server.manabom.domain.report.entity.ReportStatus;
import mannabom_server.manabom.domain.report.entity.ReportType;
import mannabom_server.manabom.infrastructure.security.admin.AdminPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService adminReportService;

    @GetMapping
    public ResponseEntity<AdminReportListResponse> searchReports(
            @AuthenticationPrincipal AdminPrincipal admin,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportType type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminReportService.searchReports(admin, status, type, keyword, page, size));
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<AdminReportDetailResponse> getReport(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long reportId
    ) {
        return ResponseEntity.ok(adminReportService.getReport(admin, reportId));
    }

    @PatchMapping("/{reportId}")
    public ResponseEntity<AdminReportDetailResponse> processReport(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long reportId,
            @RequestBody @Valid AdminProcessReportRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(adminReportService.processReport(admin, reportId, request, clientIp(httpRequest)));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
