package mannabom_server.manabom.application.admin.service;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.admin.dto.request.AdminCreateAccountRequest;
import mannabom_server.manabom.application.admin.dto.request.AdminResetPasswordRequest;
import mannabom_server.manabom.application.admin.dto.request.AdminUpdateAccountRequest;
import mannabom_server.manabom.application.admin.dto.response.AdminAccountResponse;
import mannabom_server.manabom.domain.admin.entity.AdminAccount;
import mannabom_server.manabom.domain.admin.enums.AdminAccountStatus;
import mannabom_server.manabom.domain.admin.enums.AdminAuditActionType;
import mannabom_server.manabom.domain.admin.enums.AdminAuditTargetType;
import mannabom_server.manabom.domain.admin.enums.AdminRole;
import mannabom_server.manabom.domain.admin.repository.AdminAccountRepository;
import mannabom_server.manabom.domain.admin.repository.AdminRefreshTokenRepository;
import mannabom_server.manabom.infrastructure.security.admin.AdminPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminAccountService {

    private final AdminAccountRepository adminAccountRepository;
    private final AdminRefreshTokenRepository adminRefreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminAuditService adminAuditService;

    @Transactional(readOnly = true)
    public List<AdminAccountResponse> getAccounts(AdminPrincipal principal) {
        requireSuperAdmin(principal);
        return adminAccountRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminAccountResponse createAccount(AdminPrincipal principal,
                                              AdminCreateAccountRequest request,
                                              String ipAddress) {
        requireSuperAdmin(principal);
        Set<AdminRole> roles = normalizeRoles(request.getRoles());
        if (adminAccountRepository.existsByLoginId(request.getLoginId())) {
            throw new IllegalArgumentException("이미 존재하는 관리자 아이디입니다.");
        }

        AdminAccount account = AdminAccount.builder()
                .loginId(request.getLoginId())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .roles(roles)
                .build();
        AdminAccount saved = adminAccountRepository.save(account);

        adminAuditService.log(principal.adminId(), AdminAuditActionType.ADMIN_ACCOUNT_CREATE,
                AdminAuditTargetType.ADMIN, saved.getAdminId(), null, saved.getRoles().toString(),
                "관리자 계정 생성", ipAddress);
        return toResponse(saved);
    }

    @Transactional
    public AdminAccountResponse updateAccount(AdminPrincipal principal,
                                              Long adminId,
                                              AdminUpdateAccountRequest request,
                                              String ipAddress) {
        requireSuperAdmin(principal);
        AdminAccount account = getAccount(adminId);
        if (principal.adminId().equals(adminId) && request.getStatus() != AdminAccountStatus.ACTIVE) {
            throw new IllegalArgumentException("자기 자신의 관리자 계정은 비활성화할 수 없습니다.");
        }

        Set<AdminRole> roles = normalizeRoles(request.getRoles());
        String before = account.getRoles() + "/" + account.getStatus().name();
        account.update(request.getName(), roles, request.getStatus());
        adminRefreshTokenRepository.deleteByAdminAccount(account);

        adminAuditService.log(principal.adminId(), AdminAuditActionType.ADMIN_ACCOUNT_UPDATE,
                AdminAuditTargetType.ADMIN, adminId, before,
                roles + "/" + request.getStatus().name(),
                "관리자 계정 수정", ipAddress);
        return toResponse(account);
    }

    @Transactional
    public void deleteAccount(AdminPrincipal principal, Long adminId, String ipAddress) {
        requireSuperAdmin(principal);
        if (principal.adminId().equals(adminId)) {
            throw new IllegalArgumentException("자기 자신의 관리자 계정은 삭제할 수 없습니다.");
        }
        AdminAccount account = getAccount(adminId);
        adminRefreshTokenRepository.deleteByAdminAccount(account);
        adminAccountRepository.delete(account);
        adminAuditService.log(principal.adminId(), AdminAuditActionType.ADMIN_ACCOUNT_DELETE,
                AdminAuditTargetType.ADMIN, adminId, account.getLoginId(), null,
                "관리자 계정 삭제", ipAddress);
    }

    @Transactional
    public void resetPassword(AdminPrincipal principal,
                              Long adminId,
                              AdminResetPasswordRequest request,
                              String ipAddress) {
        requireSuperAdmin(principal);
        AdminAccount account = getAccount(adminId);
        account.changePassword(passwordEncoder.encode(request.getPassword()));
        adminRefreshTokenRepository.deleteByAdminAccount(account);

        adminAuditService.log(principal.adminId(), AdminAuditActionType.ADMIN_PASSWORD_RESET,
                AdminAuditTargetType.ADMIN, adminId, null, null,
                "관리자 비밀번호 재설정", ipAddress);
    }

    private AdminAccount getAccount(Long adminId) {
        return adminAccountRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("관리자 계정을 찾을 수 없습니다."));
    }

    private void requireSuperAdmin(AdminPrincipal principal) {
        if (!principal.hasRole(AdminRole.SUPER_ADMIN)) {
            throw new IllegalStateException("총괄 관리자만 수행할 수 있는 작업입니다.");
        }
    }

    private Set<AdminRole> normalizeRoles(Set<AdminRole> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("관리자 권한을 하나 이상 선택해 주세요.");
        }
        return new LinkedHashSet<>(roles);
    }

    private AdminAccountResponse toResponse(AdminAccount account) {
        return AdminAccountResponse.builder()
                .adminId(account.getAdminId())
                .loginId(account.getLoginId())
                .name(account.getName())
                .roles(account.getRoles())
                .status(account.getStatus())
                .lastLoginAt(account.getLastLoginAt())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
