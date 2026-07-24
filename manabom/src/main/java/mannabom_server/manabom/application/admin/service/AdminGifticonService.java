package mannabom_server.manabom.application.admin.service;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.admin.dto.request.AdminConfigureGifticonTokenRequest;
import mannabom_server.manabom.application.admin.dto.response.AdminGifticonProductResponse;
import mannabom_server.manabom.application.admin.dto.response.AdminGifticonProductSliceResponse;
import mannabom_server.manabom.application.gifticon.port.GifticonTokenCipher;
import mannabom_server.manabom.domain.admin.enums.AdminAuditActionType;
import mannabom_server.manabom.domain.admin.enums.AdminAuditTargetType;
import mannabom_server.manabom.domain.admin.enums.AdminRole;
import mannabom_server.manabom.domain.gifticon.entity.GifticonProduct;
import mannabom_server.manabom.domain.gifticon.repository.GifticonProductRepository;
import mannabom_server.manabom.infrastructure.security.admin.AdminPrincipal;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminGifticonService {

    private static final int MAX_PAGE_SIZE = 100;

    private final GifticonProductRepository gifticonProductRepository;
    private final AdminAuditService adminAuditService;
    private final GifticonTokenCipher gifticonTokenCipher;

    @Transactional(readOnly = true)
    public AdminGifticonProductSliceResponse getProducts(
            AdminPrincipal admin,
            Long cursor,
            int size,
            Boolean tokenConfigured,
            String keyword
    ) {
        requireSuperAdmin(admin);
        if (cursor != null && cursor < 0) {
            throw new IllegalArgumentException("cursor는 0 이상이어야 합니다.");
        }
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size는 1 이상 100 이하여야 합니다.");
        }

        List<GifticonProduct> fetched = gifticonProductRepository.findProductsForAdminAfter(
                cursor == null ? 0L : cursor,
                tokenConfigured,
                StringUtils.hasText(keyword) ? keyword.trim() : "",
                PageRequest.of(0, size + 1)
        );
        boolean hasNext = fetched.size() > size;
        List<GifticonProduct> current = hasNext ? fetched.subList(0, size) : fetched;
        List<AdminGifticonProductResponse> contents = current.stream()
                .map(AdminGifticonProductResponse::from)
                .toList();
        Long nextCursor = hasNext
                ? current.get(current.size() - 1).getGifticonProductId()
                : null;
        return new AdminGifticonProductSliceResponse(contents, nextCursor, hasNext);
    }

    @Transactional
    public AdminGifticonProductResponse configureTemplateToken(
            AdminPrincipal admin,
            Long gifticonProductId,
            AdminConfigureGifticonTokenRequest request,
            String ipAddress
    ) {
        requireSuperAdmin(admin);
        GifticonProduct product = gifticonProductRepository.findById(gifticonProductId)
                .orElseThrow(() -> new IllegalArgumentException("기프티콘 상품을 찾을 수 없습니다."));
        boolean configuredBefore = product.hasTemplateToken();
        product.configureEncryptedTemplateToken(
                gifticonTokenCipher.encrypt(request.getTemplateToken().trim())
        );

        adminAuditService.log(
                admin.adminId(),
                AdminAuditActionType.GIFTICON_TEMPLATE_TOKEN_UPDATE,
                AdminAuditTargetType.GIFTICON_PRODUCT,
                gifticonProductId,
                "templateTokenConfigured=" + configuredBefore,
                "templateTokenConfigured=true",
                request.getReason(),
                ipAddress
        );
        return AdminGifticonProductResponse.from(product);
    }

    private void requireSuperAdmin(AdminPrincipal admin) {
        if (admin == null || !admin.hasRole(AdminRole.SUPER_ADMIN)) {
            throw new IllegalStateException("SUPER_ADMIN 권한이 필요합니다.");
        }
    }
}
