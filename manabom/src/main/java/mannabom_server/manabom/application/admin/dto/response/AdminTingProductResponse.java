package mannabom_server.manabom.application.admin.dto.response;

import mannabom_server.manabom.domain.tingPurchase.entity.TingProduct;

import java.time.Instant;

public record AdminTingProductResponse(
        Long tingProductId,
        String productCode,
        String name,
        int tingAmount,
        int bonusEventTing,
        int displayPriceKrw,
        String appleProductId,
        String googleProductId,
        boolean active,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt
) {
    public static AdminTingProductResponse from(TingProduct product) {
        return new AdminTingProductResponse(
                product.getTingProductId(),
                product.getProductCode(),
                product.getName(),
                product.getTingAmount(),
                product.getBonusEventTing(),
                product.getDisplayPriceKrw(),
                product.getAppleProductId(),
                product.getGoogleProductId(),
                product.isActive(),
                product.getSortOrder(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
