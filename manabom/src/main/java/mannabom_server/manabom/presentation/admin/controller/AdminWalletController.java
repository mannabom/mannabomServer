package mannabom_server.manabom.presentation.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.admin.dto.request.AdminActivateMembershipRequest;
import mannabom_server.manabom.application.admin.dto.request.AdminAdjustWalletRequest;
import mannabom_server.manabom.application.admin.dto.response.AdminWalletResponse;
import mannabom_server.manabom.application.admin.service.AdminWalletService;
import mannabom_server.manabom.infrastructure.security.admin.AdminPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/admin/users/{userId}/wallet")
@RequiredArgsConstructor
public class AdminWalletController {

    private final AdminWalletService adminWalletService;

    @Value("${app.admin.audit.trust-proxy:false}")
    private boolean trustProxy;

    @Value("${app.admin.audit.trusted-proxies:}")
    private String trustedProxies;

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

    @PostMapping("/membership")
    public ResponseEntity<AdminWalletResponse> activateMembership(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long userId,
            @RequestBody @Valid AdminActivateMembershipRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(adminWalletService.activateMembership(admin, userId, request, clientIp(httpRequest)));
    }

    private String clientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!trustProxy || !isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }

        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return Arrays.stream(forwarded.split(","))
                    .map(String::trim)
                    .filter(ip -> !ip.isBlank())
                    .filter(ip -> !"unknown".equalsIgnoreCase(ip))
                    .findFirst()
                    .orElse(remoteAddr);
        }
        return remoteAddr;
    }

    private boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null || remoteAddr.isBlank() || trustedProxies == null || trustedProxies.isBlank()) {
            return false;
        }
        return Arrays.stream(trustedProxies.split(","))
                .map(String::trim)
                .anyMatch(remoteAddr::equals);
    }
}
