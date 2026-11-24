package mannabom_server.manabom.application.tingPurchase.dto.response;

import java.util.List;

/**
 * @param purchaseAccountToken 클라이언트가 스토어 결제 시 넣어야 하는 구매자 식별 토큰.
 *                             Apple: Product.PurchaseOption.appAccountToken, Google: setObfuscatedAccountId
 */
public record TingProductCatalogResponse(
        String purchaseAccountToken,
        List<TingProductResponse> products
) {
}
