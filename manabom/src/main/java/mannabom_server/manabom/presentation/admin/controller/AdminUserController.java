package mannabom_server.manabom.presentation.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.admin.dto.request.AdminUpdateUserStatusRequest;
import mannabom_server.manabom.application.admin.dto.response.AdminUserDetailResponse;
import mannabom_server.manabom.application.admin.dto.response.AdminUserListResponse;
import mannabom_server.manabom.application.admin.service.AdminUserService;
import mannabom_server.manabom.infrastructure.security.admin.AdminPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<AdminUserListResponse> searchUsers(
            @AuthenticationPrincipal AdminPrincipal admin,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "ALL") String searchType,
            @RequestParam(defaultValue = "ALL") String accountStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminUserService.searchUsers(admin, keyword, searchType, accountStatus, page, size));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<AdminUserDetailResponse> getUser(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(adminUserService.getUser(admin, userId));
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<AdminUserDetailResponse> updateUserStatus(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long userId,
            @RequestBody @Valid AdminUpdateUserStatusRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(adminUserService.updateUserStatus(admin, userId, request, clientIp(httpRequest)));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
