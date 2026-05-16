package mannabom_server.manabom.presentation.admin.controller;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.admin.dto.response.AdminAuditLogListResponse;
import mannabom_server.manabom.application.admin.service.AdminAuditService;
import mannabom_server.manabom.infrastructure.security.admin.AdminPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audits")
@RequiredArgsConstructor
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    @GetMapping
    public ResponseEntity<AdminAuditLogListResponse> getLogs(
            @AuthenticationPrincipal AdminPrincipal admin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        return ResponseEntity.ok(adminAuditService.getLogs(admin, page, size));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<AdminAuditLogListResponse> getUserOperationLogs(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(adminAuditService.getUserOperationLogs(admin, userId, page, size));
    }
}
