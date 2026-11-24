package mannabom_server.manabom.application.tingPurchase.dto.response;

import mannabom_server.manabom.domain.payment.entity.Payment;
import mannabom_server.manabom.domain.payment.entity.StorePayment;
import mannabom_server.manabom.domain.payment.enums.PaymentStatus;
import mannabom_server.manabom.domain.payment.enums.PurchaseStore;
import mannabom_server.manabom.domain.payment.enums.StoreConsumeStatus;
import mannabom_server.manabom.domain.tingPurchase.entity.TingPurchase;

import java.time.Instant;

public record TingPurchaseResponse(
        Long paymentId,
        PaymentStatus status,
        PurchaseStore store,
        String storeTransactionId,
        String productCode,
        String productName,
        int quantity,
        int creditedTing,
        int creditedEventTing,
        Integer tingBalance,
        Integer eventTingBalance,
        StoreConsumeStatus consumeStatus,
        String failureReason,
        Instant paidAt,
        Instant createdAt
) {
    public static TingPurchaseResponse from(
            Payment payment,
            StorePayment storePayment,
            TingPurchase tingPurchase,
            Integer tingBalance,
            Integer eventTingBalance
    ) {
        return new TingPurchaseResponse(
                payment.getPaymentId(),
                payment.getStatus(),
                storePayment == null ? null : storePayment.getStore(),
                storePayment == null ? null : storePayment.getTransactionId(),
                tingPurchase.getProduct().getProductCode(),
                tingPurchase.getProduct().getName(),
                tingPurchase.getQuantity(),
                tingPurchase.getCreditedTing(),
                tingPurchase.getCreditedEventTing(),
                tingBalance,
                eventTingBalance,
                storePayment == null ? null : storePayment.getConsumeStatus(),
                payment.getFailureReason(),
                payment.getPaidAt(),
                payment.getCreatedAt()
        );
    }
}
