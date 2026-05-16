package mannabom_server.manabom.application.admin.service;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.admin.dto.request.AdminLoginRequest;
import mannabom_server.manabom.application.admin.dto.request.AdminRefreshTokenRequest;
import mannabom_server.manabom.application.admin.dto.response.AdminAuthResponse;
import mannabom_server.manabom.application.admin.dto.response.AdminMeResponse;
import mannabom_server.manabom.domain.admin.entity.AdminAccount;
import mannabom_server.manabom.domain.admin.entity.AdminRefreshToken;
import mannabom_server.manabom.domain.admin.enums.AdminAuditActionType;
import mannabom_server.manabom.domain.admin.enums.AdminAuditTargetType;
import mannabom_server.manabom.domain.admin.repository.AdminAccountRepository;
import mannabom_server.manabom.domain.admin.repository.AdminRefreshTokenRepository;
import mannabom_server.manabom.infrastructure.security.admin.AdminJwtUtil;
import mannabom_server.manabom.infrastructure.security.admin.AdminPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminAccountRepository adminAccountRepository;
    private final AdminRefreshTokenRepository adminRefreshTokenRepository;
    private final AdminJwtUtil adminJwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AdminAuditService adminAuditService;

    @Transactional
    public AdminAuthResponse login(AdminLoginRequest request, String ipAddress) {
        AdminAccount admin = adminAccountRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new IllegalArgumentException("관리자 계정 정보를 확인해 주세요."));
        if (!admin.isActive() || !passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            throw new IllegalArgumentException("관리자 계정 정보를 확인해 주세요.");
        }

        admin.recordLogin(LocalDateTime.now());
        adminRefreshTokenRepository.deleteByAdminAccount(admin);
        AdminAuthResponse response = issueTokens(admin);
        adminAuditService.log(admin.getAdminId(), AdminAuditActionType.ADMIN_LOGIN,
                AdminAuditTargetType.ADMIN, admin.getAdminId(), null, null, "관리자 로그인", ipAddress);

        return response;
    }

    @Transactional
    public AdminAuthResponse refresh(AdminRefreshTokenRequest request, String ipAddress) {
        AdminRefreshToken refreshToken = adminRefreshTokenRepository.findByRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 refresh token입니다."));
        if (refreshToken.isExpired()) {
            adminRefreshTokenRepository.delete(refreshToken);
            throw new IllegalArgumentException("만료된 refresh token입니다.");
        }

        AdminAccount admin = refreshToken.getAdminAccount();
        if (!admin.isActive()) {
            throw new IllegalStateException("비활성화된 관리자 계정입니다.");
        }

        adminRefreshTokenRepository.delete(refreshToken);
        AdminAuthResponse response = issueTokens(admin);
        adminAuditService.log(admin.getAdminId(), AdminAuditActionType.ADMIN_REFRESH,
                AdminAuditTargetType.ADMIN, admin.getAdminId(), null, null, "관리자 토큰 갱신", ipAddress);

        return response;
    }

    @Transactional
    public void logout(AdminPrincipal principal, String refreshToken, String ipAddress) {
        adminRefreshTokenRepository.deleteByRefreshToken(refreshToken);
        adminAuditService.log(principal.adminId(), AdminAuditActionType.ADMIN_LOGOUT,
                AdminAuditTargetType.ADMIN, principal.adminId(), null, null, "관리자 로그아웃", ipAddress);
    }

    @Transactional(readOnly = true)
    public AdminMeResponse me(AdminPrincipal principal) {
        AdminAccount admin = adminAccountRepository.findById(principal.adminId())
                .orElseThrow(() -> new IllegalArgumentException("관리자 계정을 찾을 수 없습니다."));
        return AdminMeResponse.builder()
                .adminId(admin.getAdminId())
                .loginId(admin.getLoginId())
                .name(admin.getName())
                .roles(admin.getRoles())
                .status(admin.getStatus())
                .lastLoginAt(admin.getLastLoginAt())
                .build();
    }

    private AdminAuthResponse issueTokens(AdminAccount admin) {
        String accessToken = adminJwtUtil.generateAccessToken(admin.getAdminId(), admin.getLoginId(), admin.getRoles());
        String refreshToken = adminJwtUtil.generateRefreshToken(admin.getAdminId(), admin.getLoginId(), admin.getRoles());
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(adminJwtUtil.getRefreshTokenExpiration() / 1000);

        adminRefreshTokenRepository.save(AdminRefreshToken.builder()
                .adminAccount(admin)
                .refreshToken(refreshToken)
                .expiresAt(expiresAt)
                .build());

        return AdminAuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(adminJwtUtil.getAccessTokenExpiration() / 1000)
                .adminId(admin.getAdminId())
                .loginId(admin.getLoginId())
                .name(admin.getName())
                .roles(admin.getRoles())
                .build();
    }
}
