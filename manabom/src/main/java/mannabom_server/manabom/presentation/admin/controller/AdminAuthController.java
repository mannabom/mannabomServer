package mannabom_server.manabom.presentation.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.admin.dto.request.AdminLoginRequest;
import mannabom_server.manabom.application.admin.dto.request.AdminLogoutRequest;
import mannabom_server.manabom.application.admin.dto.request.AdminRefreshTokenRequest;
import mannabom_server.manabom.application.admin.dto.response.AdminAuthResponse;
import mannabom_server.manabom.application.admin.dto.response.AdminMeResponse;
import mannabom_server.manabom.application.admin.service.AdminAuthService;
import mannabom_server.manabom.infrastructure.security.admin.AdminPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    public ResponseEntity<AdminAuthResponse> login(
            @RequestBody @Valid AdminLoginRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(adminAuthService.login(request, clientIp(httpRequest)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AdminAuthResponse> refresh(
            @RequestBody @Valid AdminRefreshTokenRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(adminAuthService.refresh(request, clientIp(httpRequest)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal AdminPrincipal admin,
            @RequestBody @Valid AdminLogoutRequest request,
            HttpServletRequest httpRequest
    ) {
        adminAuthService.logout(admin, request.getRefreshToken(), clientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AdminMeResponse> me(
            @AuthenticationPrincipal AdminPrincipal admin
    ) {
        return ResponseEntity.ok(adminAuthService.me(admin));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
