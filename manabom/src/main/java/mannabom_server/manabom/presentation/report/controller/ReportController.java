package mannabom_server.manabom.presentation.report.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.common.dto.ApiResponse;
import mannabom_server.manabom.application.report.dto.request.CreateChatReportRequest;
import mannabom_server.manabom.application.report.dto.request.CreateProfileReportRequest;
import mannabom_server.manabom.application.report.dto.response.CreateReportResponse;
import mannabom_server.manabom.application.report.service.ReportService;
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
    public ResponseEntity<ApiResponse<CreateReportResponse>> reportChat(
            @Valid @RequestBody CreateChatReportRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        Long reportId = reportService.createChatReport(userId, request);
        return ResponseEntity.ok(ApiResponse.success(
                CreateReportResponse.of(reportId),
                "채팅 신고를 완료했습니다."
        ));
    }

    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<CreateReportResponse>> reportProfile(
            @Valid @RequestBody CreateProfileReportRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        Long reportId = reportService.createProfileReport(userId, request);
        return ResponseEntity.ok(ApiResponse.success(
                CreateReportResponse.of(reportId),
                "프로필 신고를 완료했습니다."
        ));
    }

}
