package mannabom_server.manabom.application.tingPurchase.dto.response;

import mannabom_server.manabom.domain.tingPurchase.entity.TingProduct;

public record TingProductResponse(
        String productCode,
        String name,
        int tingAmount,
        int bonusEventTing,
        int displayPriceKrw,
        String appleProductId,
        String googleProductId
) {
    public static TingProductResponse from(TingProduct product) {
        return new TingProductResponse(
                product.getProductCode(),
                product.getName(),
                product.getTingAmount(),
                product.getBonusEventTing(),
                product.getDisplayPriceKrw(),
                product.getAppleProductId(),
                product.getGoogleProductId()
        );
    }
}
