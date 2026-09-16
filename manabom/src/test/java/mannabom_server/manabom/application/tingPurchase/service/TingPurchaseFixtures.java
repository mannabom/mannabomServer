package mannabom_server.manabom.application.tingPurchase.service;

import mannabom_server.manabom.application.tingPurchase.port.InAppPurchaseGateway.StorePurchase;
import mannabom_server.manabom.application.tingPurchase.port.InAppPurchaseGateway.StorePurchaseState;
import mannabom_server.manabom.domain.payment.entity.Payment;
import mannabom_server.manabom.domain.payment.entity.StorePayment;
import mannabom_server.manabom.domain.payment.enums.PaymentPurpose;
import mannabom_server.manabom.domain.payment.enums.PurchaseStore;
import mannabom_server.manabom.domain.payment.enums.StoreEnvironment;
import mannabom_server.manabom.domain.tingPurchase.entity.TingProduct;
import mannabom_server.manabom.domain.tingPurchase.entity.TingPurchase;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;

final class TingPurchaseFixtures {

    static final Long USER_ID = 7L;
    static final Long PAYMENT_ID = 100L;
    static final String APPLE_PRODUCT_ID = "com.mannabom.ting.70";
    static final String GOOGLE_PRODUCT_ID = "ting_70";

    private TingPurchaseFixtures() {
    }

    static TingProduct product() {
        TingProduct product = new TingProduct(
                "TING_70", "팅 70개", 70, 5, 9_900, APPLE_PRODUCT_ID, GOOGLE_PRODUCT_ID, true, 1
        );
        ReflectionTestUtils.setField(product, "tingProductId", 3L);
        return product;
    }

    static Payment verifyingPayment(PurchaseStore store) {
        Payment payment = Payment.startVerifying(USER_ID, store.getProvider(), PaymentPurpose.TING_CHARGE);
        ReflectionTestUtils.setField(payment, "paymentId", PAYMENT_ID);
        return payment;
    }

    static StorePayment storePayment(Payment payment, PurchaseStore store, String purchaseKey) {
        StorePayment storePayment = new StorePayment(payment, store, product().storeProductId(store), purchaseKey);
        ReflectionTestUtils.setField(storePayment, "paymentId", payment.getPaymentId());
        return storePayment;
    }

    static TingPurchase tingPurchase(Payment payment) {
        TingPurchase purchase = new TingPurchase(payment, product());
        ReflectionTestUtils.setField(purchase, "paymentId", payment.getPaymentId());
        return purchase;
    }

    static StorePurchase googlePurchased(String orderId, String accountToken) {
        return new StorePurchase(
                PurchaseStore.GOOGLE, GOOGLE_PRODUCT_ID, orderId, null,
                StorePurchaseState.PURCHASED, StoreEnvironment.PRODUCTION, 1,
                Instant.parse("2026-09-16T00:00:00Z"), accountToken,
                null, null, false, "{\"orderId\":\"" + orderId + "\"}"
        );
    }

    static StorePurchase applePurchased(String transactionId, int quantity, StoreEnvironment environment) {
        return new StorePurchase(
                PurchaseStore.APPLE, APPLE_PRODUCT_ID, transactionId, transactionId,
                StorePurchaseState.PURCHASED, environment, quantity,
                Instant.parse("2026-09-16T00:00:00Z"), null,
                new BigDecimal("9900.000"), "KRW", false, "jws"
        );
    }
}
