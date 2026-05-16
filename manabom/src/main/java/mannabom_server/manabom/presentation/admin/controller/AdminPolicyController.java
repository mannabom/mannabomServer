package mannabom_server.manabom.presentation.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.admin.dto.request.AdminUpdatePolicyRequest;
import mannabom_server.manabom.application.admin.service.AdminPolicyService;
import mannabom_server.manabom.infrastructure.security.admin.AdminPrincipal;
import mannabom_server.manabom.policy.model.RuntimePolicySnapshot;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/policies")
@RequiredArgsConstructor
public class AdminPolicyController {

    private final AdminPolicyService adminPolicyService;

    @GetMapping
    public ResponseEntity<RuntimePolicySnapshot> getPolicy(
            @AuthenticationPrincipal AdminPrincipal admin
    ) {
        return ResponseEntity.ok(adminPolicyService.getPolicy(admin));
    }

    @PatchMapping
    public ResponseEntity<RuntimePolicySnapshot> updatePolicy(
            @AuthenticationPrincipal AdminPrincipal admin,
            @RequestBody @Valid AdminUpdatePolicyRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(adminPolicyService.updatePolicy(admin, request, clientIp(httpRequest)));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
