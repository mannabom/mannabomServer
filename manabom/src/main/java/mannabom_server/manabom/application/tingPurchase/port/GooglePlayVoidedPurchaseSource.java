package mannabom_server.manabom.application.tingPurchase.port;

import java.time.Instant;
import java.util.List;

/** Google Play 환불/취소(voided) 구매 목록 조회. */
public interface GooglePlayVoidedPurchaseSource {

    List<VoidedPurchase> listVoidedPurchases(Instant since);

    record VoidedPurchase(
            String purchaseToken,
            String orderId,
            Instant voidedAt,
            String reason
    ) {
    }
}
