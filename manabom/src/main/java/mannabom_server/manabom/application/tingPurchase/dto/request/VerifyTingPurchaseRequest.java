package mannabom_server.manabom.application.tingPurchase.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mannabom_server.manabom.domain.payment.enums.PurchaseStore;

/**
 * 스토어 결제 완료 후 클라이언트가 보내는 검증 요청.
 * APPLE: transactionId(StoreKit 2 Transaction.id), GOOGLE: purchaseToken(Purchase.purchaseToken)
 */
public record VerifyTingPurchaseRequest(
        @NotNull(message = "store는 필수입니다.")
        PurchaseStore store,
        @NotBlank(message = "productCode는 필수입니다.")
        @Size(max = 50)
        String productCode,
        @Size(max = 200)
        String transactionId,
        @Size(max = 1000)
        String purchaseToken
) {
    public String purchaseKey() {
        return store == PurchaseStore.APPLE ? transactionId : purchaseToken;
    }
}
