package mannabom_server.manabom.application.admin.service;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.admin.dto.request.AdminTingProductRequest;
import mannabom_server.manabom.application.admin.dto.response.AdminTingProductResponse;
import mannabom_server.manabom.domain.admin.enums.AdminAuditActionType;
import mannabom_server.manabom.domain.admin.enums.AdminAuditTargetType;
import mannabom_server.manabom.domain.admin.enums.AdminRole;
import mannabom_server.manabom.domain.tingPurchase.entity.TingProduct;
import mannabom_server.manabom.domain.tingPurchase.repository.TingProductRepository;
import mannabom_server.manabom.infrastructure.security.admin.AdminPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminTingProductService {

    private final TingProductRepository productRepository;
    private final AdminAuditService adminAuditService;

    @Transactional(readOnly = true)
    public List<AdminTingProductResponse> getProducts(AdminPrincipal admin) {
        requireAnyRole(admin, AdminRole.SUPER_ADMIN, AdminRole.FINANCE, AdminRole.SUPPORT);
        return productRepository.findAllByOrderBySortOrderAscTingProductIdAsc()
                .stream()
                .map(AdminTingProductResponse::from)
                .toList();
    }

    @Transactional
    public AdminTingProductResponse create(AdminPrincipal admin, AdminTingProductRequest request, String ipAddress) {
        requireAnyRole(admin, AdminRole.SUPER_ADMIN, AdminRole.FINANCE);
        if (request.productCode() == null || request.productCode().isBlank()) {
            throw new IllegalArgumentException("productCode는 필수입니다.");
        }
        productRepository.findByProductCode(request.productCode()).ifPresent(existing -> {
            throw new IllegalArgumentException("이미 존재하는 상품 코드입니다: " + request.productCode());
        });
        TingProduct product = productRepository.save(new TingProduct(
                request.productCode(),
                request.name(),
                request.tingAmount(),
                request.bonusEventTing(),
                request.displayPriceKrw(),
                request.appleProductId(),
                request.googleProductId(),
                request.active(),
                request.sortOrder()
        ));
        adminAuditService.log(admin.adminId(), AdminAuditActionType.TING_PRODUCT_CREATE,
                AdminAuditTargetType.TING_PRODUCT, product.getTingProductId(),
                null, snapshot(product), request.reason(), ipAddress);
        return AdminTingProductResponse.from(product);
    }

    @Transactional
    public AdminTingProductResponse update(
            AdminPrincipal admin,
            Long tingProductId,
            AdminTingProductRequest request,
            String ipAddress
    ) {
        requireAnyRole(admin, AdminRole.SUPER_ADMIN, AdminRole.FINANCE);
        TingProduct product = productRepository.findById(tingProductId)
                .orElseThrow(() -> new IllegalArgumentException("팅 상품을 찾을 수 없습니다."));
        String before = snapshot(product);
        product.update(
                request.name(),
                request.tingAmount(),
                request.bonusEventTing(),
                request.displayPriceKrw(),
                request.appleProductId(),
                request.googleProductId(),
                request.active(),
                request.sortOrder()
        );
        adminAuditService.log(admin.adminId(), AdminAuditActionType.TING_PRODUCT_UPDATE,
                AdminAuditTargetType.TING_PRODUCT, product.getTingProductId(),
                before, snapshot(product), request.reason(), ipAddress);
        return AdminTingProductResponse.from(product);
    }

    private String snapshot(TingProduct p) {
        return "code=" + p.getProductCode()
                + ", name=" + p.getName()
                + ", ting=" + p.getTingAmount()
                + ", bonusEventTing=" + p.getBonusEventTing()
                + ", priceKrw=" + p.getDisplayPriceKrw()
                + ", apple=" + p.getAppleProductId()
                + ", google=" + p.getGoogleProductId()
                + ", active=" + p.isActive()
                + ", sort=" + p.getSortOrder();
    }

    private void requireAnyRole(AdminPrincipal admin, AdminRole... roles) {
        if (admin.hasAnyRole(roles)) {
            return;
        }
        throw new IllegalStateException("해당 관리자 권한으로 수행할 수 없는 작업입니다.");
    }
}
