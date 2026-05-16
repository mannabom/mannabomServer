package mannabom_server.manabom.application.admin.service;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.admin.dto.request.AdminAdjustWalletRequest;
import mannabom_server.manabom.application.admin.dto.response.AdminWalletResponse;
import mannabom_server.manabom.domain.admin.enums.AdminAuditActionType;
import mannabom_server.manabom.domain.admin.enums.AdminAuditTargetType;
import mannabom_server.manabom.domain.admin.enums.AdminRole;
import mannabom_server.manabom.domain.currency.entity.TingWallet;
import mannabom_server.manabom.domain.currency.repository.TingWalletRepository;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import mannabom_server.manabom.infrastructure.security.admin.AdminPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminWalletService {

    private final UserRepository userRepository;
    private final TingWalletRepository tingWalletRepository;
    private final AdminAuditService adminAuditService;

    @Transactional(readOnly = true)
    public AdminWalletResponse getWallet(AdminPrincipal admin, Long userId) {
        requireAnyRole(admin, AdminRole.SUPER_ADMIN, AdminRole.FINANCE, AdminRole.SUPPORT);
        TingWallet wallet = tingWalletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("지갑 정보를 찾을 수 없습니다."));
        return toResponse(wallet);
    }

    @Transactional
    public AdminWalletResponse adjustWallet(AdminPrincipal admin,
                                            Long userId,
                                            AdminAdjustWalletRequest request,
                                            String ipAddress) {
        requireAnyRole(admin, AdminRole.SUPER_ADMIN, AdminRole.FINANCE);
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        int tingDelta = request.getTingDelta() == null ? 0 : request.getTingDelta();
        int eventTingDelta = request.getEventTingDelta() == null ? 0 : request.getEventTingDelta();
        if (tingDelta == 0 && eventTingDelta == 0) {
            throw new IllegalArgumentException("변경할 팅 수량이 없습니다.");
        }

        TingWallet wallet = tingWalletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> tingWalletRepository.save(new TingWallet(userId)));
        String before = "ting=" + wallet.getTing() + ", eventTing=" + wallet.getEventTing();

        applyTingDelta(wallet, tingDelta);
        applyEventTingDelta(wallet, eventTingDelta);

        String after = "ting=" + wallet.getTing() + ", eventTing=" + wallet.getEventTing();
        adminAuditService.log(admin.adminId(), AdminAuditActionType.WALLET_ADJUST,
                AdminAuditTargetType.TING_WALLET, userId, before, after, request.getReason(), ipAddress);
        return toResponse(wallet);
    }

    private void applyTingDelta(TingWallet wallet, int delta) {
        if (delta > 0) {
            wallet.addTing(delta);
        } else if (delta < 0) {
            wallet.spendTing(Math.abs(delta));
        }
    }

    private void applyEventTingDelta(TingWallet wallet, int delta) {
        if (delta > 0) {
            wallet.addEventTing(delta);
        } else if (delta < 0) {
            wallet.spendEventTing(Math.abs(delta));
        }
    }

    private AdminWalletResponse toResponse(TingWallet wallet) {
        return AdminWalletResponse.builder()
                .userId(wallet.getUserId())
                .ting(wallet.getTing())
                .eventTing(wallet.getEventTing())
                .build();
    }

    private void requireAnyRole(AdminPrincipal admin, AdminRole... roles) {
        if (admin.hasAnyRole(roles)) {
            return;
        }
        throw new IllegalStateException("해당 관리자 권한으로 수행할 수 없는 작업입니다.");
    }
}
