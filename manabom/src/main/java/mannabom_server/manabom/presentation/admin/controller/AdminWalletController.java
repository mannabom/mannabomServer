package mannabom_server.manabom.presentation.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.admin.dto.request.AdminAdjustWalletRequest;
import mannabom_server.manabom.application.admin.dto.response.AdminWalletResponse;
import mannabom_server.manabom.application.admin.service.AdminWalletService;
import mannabom_server.manabom.infrastructure.security.admin.AdminPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users/{userId}/wallet")
@RequiredArgsConstructor
public class AdminWalletController {

    private final AdminWalletService adminWalletService;

    @GetMapping
    public ResponseEntity<AdminWalletResponse> getWallet(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(adminWalletService.getWallet(admin, userId));
    }

    @PatchMapping
    public ResponseEntity<AdminWalletResponse> adjustWallet(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long userId,
            @RequestBody @Valid AdminAdjustWalletRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(adminWalletService.adjustWallet(admin, userId, request, clientIp(httpRequest)));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
