package mannabom_server.manabom.domain.payment.entity;

import mannabom_server.manabom.domain.payment.enums.PaymentProvider;
import mannabom_server.manabom.domain.payment.enums.PaymentPurpose;
import mannabom_server.manabom.domain.payment.enums.PaymentStatus;
import mannabom_server.manabom.domain.payment.enums.PurchaseStore;
import mannabom_server.manabom.domain.payment.enums.StoreConsumeStatus;
import mannabom_server.manabom.domain.payment.enums.StoreEnvironment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    private static final Instant NOW = Instant.parse("2026-09-16T12:00:00Z");

    @Test
    void verifyingPaymentBecomesPaidOnceAndIgnoresRepeatedMarkPaid() {
        Payment payment = Payment.startVerifying(1L, PaymentProvider.APPLE_IAP, PaymentPurpose.TING_CHARGE);

        payment.markPaid(new BigDecimal("9900"), "KRW", StoreEnvironment.PRODUCTION, NOW);
        payment.markPaid(new BigDecimal("1"), "USD", StoreEnvironment.SANDBOX, NOW.plusSeconds(10));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getAmount()).isEqualByComparingTo("9900");
        assertThat(payment.getCurrency()).isEqualTo("KRW");
        assertThat(payment.getPaidAt()).isEqualTo(NOW);
    }

    @Test
    void failedPaymentCannotBePaidOrRefunded() {
        Payment payment = Payment.startVerifying(1L, PaymentProvider.GOOGLE_IAP, PaymentPurpose.TING_CHARGE);
        payment.markFailed("상품 불일치", NOW);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureReason()).isEqualTo("상품 불일치");
        assertThatThrownBy(() -> payment.markPaid(null, null, null, NOW))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> payment.markRefunded("환불", NOW))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refundIsIdempotent() {
        Payment payment = Payment.startVerifying(1L, PaymentProvider.APPLE_IAP, PaymentPurpose.TING_CHARGE);
        payment.markPaid(null, null, StoreEnvironment.PRODUCTION, NOW);

        assertThat(payment.markRefunded("REFUND", NOW.plusSeconds(60))).isTrue();
        assertThat(payment.markRefunded("REFUND", NOW.plusSeconds(120))).isFalse();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getRefundedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void transientFailureKeepsVerifyingStatus() {
        Payment payment = Payment.startVerifying(1L, PaymentProvider.APPLE_IAP, PaymentPurpose.TING_CHARGE);
        payment.recordTransientFailure("timeout");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.VERIFYING);
        assertThat(payment.getFailureReason()).isEqualTo("timeout");
    }

    @Test
    void storePaymentRejectsProviderMismatchAndTracksConsumeLifecycle() {
        Payment applePayment = Payment.startVerifying(1L, PaymentProvider.APPLE_IAP, PaymentPurpose.TING_CHARGE);
        assertThatThrownBy(() -> new StorePayment(applePayment, PurchaseStore.GOOGLE, "ting_70", "token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("결제 수단과 스토어");

        Payment googlePayment = Payment.startVerifying(1L, PaymentProvider.GOOGLE_IAP, PaymentPurpose.TING_CHARGE);
        StorePayment storePayment = new StorePayment(googlePayment, PurchaseStore.GOOGLE, "ting_70", "token");
        assertThat(storePayment.getConsumeStatus()).isEqualTo(StoreConsumeStatus.NOT_REQUIRED);
        assertThat(storePayment.canStartConsume(10, NOW)).isFalse();

        storePayment.applyVerification("GPA.1", null, 1, NOW, null, true, "{}");
        googlePayment.markPaid(null, null, StoreEnvironment.PRODUCTION, NOW);
        assertThat(storePayment.getConsumeStatus()).isEqualTo(StoreConsumeStatus.PENDING);
        assertThat(storePayment.canStartConsume(10, NOW)).isTrue();

        storePayment.startConsume(NOW);
        assertThat(storePayment.getConsumeStatus()).isEqualTo(StoreConsumeStatus.PROCESSING);
        assertThat(storePayment.canStartConsume(10, NOW.minusSeconds(60))).isFalse();
        assertThat(storePayment.canStartConsume(10, NOW.plusSeconds(60))).isTrue();

        storePayment.markConsumeFailed();
        assertThat(storePayment.getConsumeStatus()).isEqualTo(StoreConsumeStatus.FAILED);
        assertThat(storePayment.canStartConsume(1, NOW)).isFalse();

        storePayment.startConsume(NOW);
        storePayment.markConsumed(NOW);
        assertThat(storePayment.getConsumeStatus()).isEqualTo(StoreConsumeStatus.DONE);
        assertThat(storePayment.getConsumedAt()).isEqualTo(NOW);
    }

    @Test
    void storePaymentRejectsDifferentTransactionIdOnReVerification() {
        Payment payment = Payment.startVerifying(1L, PaymentProvider.APPLE_IAP, PaymentPurpose.TING_CHARGE);
        StorePayment storePayment = new StorePayment(payment, PurchaseStore.APPLE, "com.mannabom.ting.70", "tx-1");
        storePayment.applyVerification("tx-1", "tx-1", 1, NOW, null, false, "jws");

        assertThatThrownBy(() -> storePayment.applyVerification("tx-2", "tx-2", 1, NOW, null, false, "jws"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("다른 거래 번호");
    }
}
