package mannabom_server.manabom.application.tingPurchase.port;

import mannabom_server.manabom.domain.payment.enums.PurchaseStore;
import mannabom_server.manabom.domain.payment.enums.StoreEnvironment;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 스토어(Apple/Google) 인앱결제 검증 게이트웨이.
 * 영구 실패(위조, 미존재, 다른 앱의 거래)는 {@link StorePurchaseInvalidException},
 * 일시 실패(네트워크, 스토어 5xx)는 {@link StoreGatewayException} 으로 구분한다.
 */
public interface InAppPurchaseGateway {

    PurchaseStore store();

    StorePurchase verify(VerifyPurchaseCommand command);

    /** Google: purchases.products.consume. Apple: 클라이언트가 finish() 하므로 no-op. */
    void consume(ConsumePurchaseCommand command);

    record VerifyPurchaseCommand(String storeProductId, String purchaseKey) {
    }

    record ConsumePurchaseCommand(String storeProductId, String purchaseKey) {
    }

    enum StorePurchaseState {
        PURCHASED,
        PENDING,
        CANCELED
    }

    record StorePurchase(
            PurchaseStore store,
            String storeProductId,
            String transactionId,
            String originalTransactionId,
            StorePurchaseState state,
            StoreEnvironment environment,
            int quantity,
            Instant purchasedAt,
            String accountToken,
            BigDecimal amount,
            String currency,
            boolean alreadyConsumed,
            String rawPayload
    ) {
    }
}
