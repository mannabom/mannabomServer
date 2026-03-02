package mannabom_server.manabom.presentation.report.controller;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.common.dto.ApiResponse;
import mannabom_server.manabom.application.report.dto.request.CreateReportRequest;
import mannabom_server.manabom.application.report.service.ReportService;
import mannabom_server.manabom.domain.report.entity.ReportType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/report")
public class ReportController {
    private final ReportService reportService;

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<Void>> reportChat(@RequestBody CreateReportRequest request, @AuthenticationPrincipal Long userId){
        reportService.createReport(userId, request, ReportType.CHAT);
        return ResponseEntity.ok(ApiResponse.success(null, "채팅 신고를 완료했습니다."));
    }

    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<Void>> reportProfile(@RequestBody CreateReportRequest request, @AuthenticationPrincipal Long userId){
        reportService.createReport(userId, request, ReportType.PROFILE);
        return ResponseEntity.ok(ApiResponse.success(null, "프로필 신고를 완료했습니다."));
    }

}
