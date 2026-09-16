package mannabom_server.manabom.domain.tingPurchase.entity;

import mannabom_server.manabom.domain.payment.entity.Payment;
import mannabom_server.manabom.domain.payment.enums.PaymentProvider;
import mannabom_server.manabom.domain.payment.enums.PaymentPurpose;
import mannabom_server.manabom.domain.payment.enums.PurchaseStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TingPurchaseTest {

    private static final Instant NOW = Instant.parse("2026-09-16T12:00:00Z");

    private TingProduct product() {
        return new TingProduct("TING_70", "팅 70개", 70, 5, 9_900, "com.mannabom.ting.70", null, true, 1);
    }

    @Test
    void snapshotsProductAmountsAndCreditsByQuantity() {
        Payment payment = Payment.startVerifying(1L, PaymentProvider.APPLE_IAP, PaymentPurpose.TING_CHARGE);
        TingPurchase purchase = new TingPurchase(payment, product());

        purchase.markCredited(2, NOW);
        purchase.markCredited(5, NOW.plusSeconds(1));

        assertThat(purchase.getQuantity()).isEqualTo(2);
        assertThat(purchase.getCreditedTing()).isEqualTo(140);
        assertThat(purchase.getCreditedEventTing()).isEqualTo(10);
        assertThat(purchase.getCreditedAt()).isEqualTo(NOW);
    }

    @Test
    void clawbackRecordsShortfallAndRejectsOutOfRange() {
        Payment payment = Payment.startVerifying(1L, PaymentProvider.APPLE_IAP, PaymentPurpose.TING_CHARGE);
        TingPurchase purchase = new TingPurchase(payment, product());
        assertThatThrownBy(() -> purchase.markClawedBack(1, 0, NOW))
                .isInstanceOf(IllegalStateException.class);

        purchase.markCredited(1, NOW);
        assertThatThrownBy(() -> purchase.markClawedBack(71, 0, NOW))
                .isInstanceOf(IllegalArgumentException.class);

        purchase.markClawedBack(40, 5, NOW);
        assertThat(purchase.hasClawbackShortfall()).isTrue();
        assertThat(purchase.getClawedBackTing()).isEqualTo(40);
    }

    @Test
    void rejectsNonTingPaymentAndUnavailableStore() {
        Payment gifticon = Payment.startVerifying(1L, PaymentProvider.TOSS, PaymentPurpose.GIFTICON);
        assertThatThrownBy(() -> new TingPurchase(gifticon, product()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("팅 충전 목적");

        TingProduct appleOnly = product();
        assertThat(appleOnly.isPurchasableAt(PurchaseStore.APPLE)).isTrue();
        assertThat(appleOnly.isPurchasableAt(PurchaseStore.GOOGLE)).isFalse();
        assertThatThrownBy(() -> appleOnly.storeProductId(PurchaseStore.GOOGLE))
                .isInstanceOf(IllegalStateException.class);
    }
}
