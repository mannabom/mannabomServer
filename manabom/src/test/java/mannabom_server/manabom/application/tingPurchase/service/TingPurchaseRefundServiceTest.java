package mannabom_server.manabom.application.tingPurchase.service;

import mannabom_server.manabom.application.currency.service.TingTransactionRecorder;
import mannabom_server.manabom.application.tingPurchase.service.TingPurchaseRefundService.RefundOutcome;
import mannabom_server.manabom.domain.currency.entity.TingWallet;
import mannabom_server.manabom.domain.currency.enums.TingTransactionType;
import mannabom_server.manabom.domain.currency.repository.TingWalletRepository;
import mannabom_server.manabom.domain.payment.entity.Payment;
import mannabom_server.manabom.domain.payment.entity.StorePayment;
import mannabom_server.manabom.domain.payment.enums.PaymentStatus;
import mannabom_server.manabom.domain.payment.enums.PurchaseStore;
import mannabom_server.manabom.domain.payment.enums.StoreEnvironment;
import mannabom_server.manabom.domain.payment.repository.StorePaymentRepository;
import mannabom_server.manabom.domain.tingPurchase.entity.TingPurchase;
import mannabom_server.manabom.domain.tingPurchase.repository.TingPurchaseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static mannabom_server.manabom.application.tingPurchase.service.TingPurchaseFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TingPurchaseRefundServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-16T12:00:00Z");

    @Mock private StorePaymentRepository storePaymentRepository;
    @Mock private TingPurchaseRepository tingPurchaseRepository;
    @Mock private TingWalletRepository tingWalletRepository;
    @Mock private TingTransactionRecorder recorder;
    @InjectMocks private TingPurchaseRefundService service;

    private StorePayment paidStorePayment(PurchaseStore store, String key, String transactionId) {
        Payment payment = verifyingPayment(store);
        StorePayment storePayment = storePayment(payment, store, key);
        storePayment.applyVerification(transactionId, null, 1, NOW, null, store == PurchaseStore.GOOGLE, "{}");
        payment.markPaid(null, null, StoreEnvironment.PRODUCTION, NOW);
        return storePayment;
    }

    @Test
    void refundClawsBackFullAmountWhenBalanceIsSufficient() {
        StorePayment storePayment = paidStorePayment(PurchaseStore.APPLE, "tx-1", "tx-1");
        TingPurchase tingPurchase = tingPurchase(storePayment.getPayment());
        tingPurchase.markCredited(1, NOW);
        TingWallet wallet = new TingWallet(USER_ID);
        wallet.addTing(100);
        wallet.addEventTing(5);
        when(storePaymentRepository.findByStoreAndTransactionIdForUpdate(PurchaseStore.APPLE, "tx-1"))
                .thenReturn(Optional.of(storePayment));
        when(tingPurchaseRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(tingPurchase));
        when(tingWalletRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(wallet));

        RefundOutcome outcome = service.refundByTransactionId(PurchaseStore.APPLE, "tx-1", "REFUND", NOW.plusSeconds(5));

        assertThat(outcome).isEqualTo(RefundOutcome.REFUNDED);
        assertThat(wallet.getTing()).isEqualTo(30);
        assertThat(wallet.getEventTing()).isEqualTo(0);
        assertThat(storePayment.getPayment().getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(storePayment.getPayment().getRefundedAt()).isEqualTo(NOW.plusSeconds(5));
        assertThat(tingPurchase.getClawedBackTing()).isEqualTo(70);
        verify(recorder).recordPaid(eq(wallet), eq(TingTransactionType.TING_PURCHASE_REFUND), eq(-70), any(),
                eq("100"), eq("TING_PURCHASE_REFUND_100"), anyString());
        verify(recorder).recordEvent(eq(wallet), eq(TingTransactionType.TING_PURCHASE_REFUND), eq(-5), any(),
                eq("100"), eq("TING_PURCHASE_REFUND_EVENT_100"), anyString());
    }

    @Test
    void refundClawsBackOnlyAvailableBalanceAndReportsShortfall() {
        StorePayment storePayment = paidStorePayment(PurchaseStore.GOOGLE, "token-1", "GPA.1");
        TingPurchase tingPurchase = tingPurchase(storePayment.getPayment());
        tingPurchase.markCredited(1, NOW);
        TingWallet wallet = new TingWallet(USER_ID);
        wallet.addTing(20);
        when(storePaymentRepository.findByStoreAndPurchaseKeyForUpdate(PurchaseStore.GOOGLE, "token-1"))
                .thenReturn(Optional.of(storePayment));
        when(tingPurchaseRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(tingPurchase));
        when(tingWalletRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(wallet));

        RefundOutcome outcome = service.refundByPurchaseKey(PurchaseStore.GOOGLE, "token-1", "GOOGLE_VOIDED:REMORSE", NOW);

        assertThat(outcome).isEqualTo(RefundOutcome.REFUNDED_WITH_SHORTFALL);
        assertThat(wallet.getTing()).isEqualTo(0);
        assertThat(tingPurchase.getClawedBackTing()).isEqualTo(20);
        assertThat(tingPurchase.getClawedBackEventTing()).isEqualTo(0);
        verify(recorder, never()).recordEvent(any(), any(), anyInt(), any(), any(), any(), any());
    }

    @Test
    void refundIsIdempotentAndHandlesUnknownOrUncreditedPurchases() {
        StorePayment refunded = paidStorePayment(PurchaseStore.APPLE, "tx-1", "tx-1");
        refunded.getPayment().markRefunded("REFUND", NOW);
        when(storePaymentRepository.findByStoreAndTransactionIdForUpdate(PurchaseStore.APPLE, "tx-1"))
                .thenReturn(Optional.of(refunded));
        assertThat(service.refundByTransactionId(PurchaseStore.APPLE, "tx-1", "REFUND", NOW))
                .isEqualTo(RefundOutcome.ALREADY_REFUNDED);

        when(storePaymentRepository.findByStoreAndTransactionIdForUpdate(PurchaseStore.APPLE, "unknown"))
                .thenReturn(Optional.empty());
        assertThat(service.refundByTransactionId(PurchaseStore.APPLE, "unknown", "REFUND", NOW))
                .isEqualTo(RefundOutcome.NOT_FOUND);

        Payment verifying = verifyingPayment(PurchaseStore.GOOGLE);
        StorePayment pending = storePayment(verifying, PurchaseStore.GOOGLE, "token-9");
        when(storePaymentRepository.findByStoreAndPurchaseKeyForUpdate(PurchaseStore.GOOGLE, "token-9"))
                .thenReturn(Optional.of(pending));
        assertThat(service.refundByPurchaseKey(PurchaseStore.GOOGLE, "token-9", "GOOGLE_VOIDED:FRAUD", NOW))
                .isEqualTo(RefundOutcome.FAILED_BEFORE_CREDIT);
        assertThat(verifying.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(tingWalletRepository, never()).findByUserIdForUpdate(any());
    }
}
