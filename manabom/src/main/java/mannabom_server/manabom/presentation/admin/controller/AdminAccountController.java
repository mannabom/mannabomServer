package mannabom_server.manabom.presentation.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.admin.dto.request.AdminCreateAccountRequest;
import mannabom_server.manabom.application.admin.dto.request.AdminResetPasswordRequest;
import mannabom_server.manabom.application.admin.dto.request.AdminUpdateAccountRequest;
import mannabom_server.manabom.application.admin.dto.response.AdminAccountResponse;
import mannabom_server.manabom.application.admin.service.AdminAccountService;
import mannabom_server.manabom.infrastructure.security.admin.AdminPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/accounts")
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    @GetMapping
    public List<AdminAccountResponse> getAccounts(
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return adminAccountService.getAccounts(principal);
    }

    @PostMapping
    public AdminAccountResponse createAccount(
            @AuthenticationPrincipal AdminPrincipal principal,
            @Valid @RequestBody AdminCreateAccountRequest request,
            HttpServletRequest httpRequest
    ) {
        return adminAccountService.createAccount(principal, request, httpRequest.getRemoteAddr());
    }

    @PatchMapping("/{adminId}")
    public AdminAccountResponse updateAccount(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long adminId,
            @Valid @RequestBody AdminUpdateAccountRequest request,
            HttpServletRequest httpRequest
    ) {
        return adminAccountService.updateAccount(principal, adminId, request, httpRequest.getRemoteAddr());
    }

    @PatchMapping("/{adminId}/password")
    public void resetPassword(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long adminId,
            @Valid @RequestBody AdminResetPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        adminAccountService.resetPassword(principal, adminId, request, httpRequest.getRemoteAddr());
    }

    @DeleteMapping("/{adminId}")
    public void deleteAccount(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable Long adminId,
            HttpServletRequest httpRequest
    ) {
        adminAccountService.deleteAccount(principal, adminId, httpRequest.getRemoteAddr());
    }
}
