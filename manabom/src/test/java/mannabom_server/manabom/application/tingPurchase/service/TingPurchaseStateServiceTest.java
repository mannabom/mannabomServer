package mannabom_server.manabom.application.tingPurchase.service;

import mannabom_server.manabom.application.currency.service.TingTransactionRecorder;
import mannabom_server.manabom.application.tingPurchase.port.InAppPurchaseGateway.StorePurchase;
import mannabom_server.manabom.application.tingPurchase.port.InAppPurchaseGateway.StorePurchaseState;
import mannabom_server.manabom.application.tingPurchase.service.TingPurchaseStateService.CreditOutcome;
import mannabom_server.manabom.application.tingPurchase.service.TingPurchaseStateService.VerificationAttempt;
import mannabom_server.manabom.domain.currency.entity.TingWallet;
import mannabom_server.manabom.domain.currency.enums.TingTransactionType;
import mannabom_server.manabom.domain.currency.repository.TingWalletRepository;
import mannabom_server.manabom.domain.payment.entity.Payment;
import mannabom_server.manabom.domain.payment.entity.StorePayment;
import mannabom_server.manabom.domain.payment.enums.PaymentStatus;
import mannabom_server.manabom.domain.payment.enums.PurchaseStore;
import mannabom_server.manabom.domain.payment.enums.StoreConsumeStatus;
import mannabom_server.manabom.domain.payment.enums.StoreEnvironment;
import mannabom_server.manabom.domain.payment.repository.PaymentRepository;
import mannabom_server.manabom.domain.payment.repository.StorePaymentRepository;
import mannabom_server.manabom.domain.tingPurchase.entity.TingPurchase;
import mannabom_server.manabom.domain.tingPurchase.repository.TingProductRepository;
import mannabom_server.manabom.domain.tingPurchase.repository.TingPurchaseRepository;
import mannabom_server.manabom.policy.config.TingPurchaseProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static mannabom_server.manabom.application.tingPurchase.service.TingPurchaseFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TingPurchaseStateServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private StorePaymentRepository storePaymentRepository;
    @Mock private TingPurchaseRepository tingPurchaseRepository;
    @Mock private TingProductRepository productRepository;
    @Mock private TingWalletRepository tingWalletRepository;
    @Mock private TingTransactionRecorder recorder;

    private TingPurchaseProperties properties;
    private TingPurchaseStateService service;

    @BeforeEach
    void setUp() {
        properties = new TingPurchaseProperties();
        service = new TingPurchaseStateService(
                paymentRepository, storePaymentRepository, tingPurchaseRepository,
                productRepository, tingWalletRepository, recorder, properties
        );
    }

    @Test
    void beginReturnsExistingPurchaseForSameUserAndRejectsOtherUser() {
        Payment payment = verifyingPayment(PurchaseStore.GOOGLE);
        StorePayment existing = storePayment(payment, PurchaseStore.GOOGLE, "token-1");
        when(storePaymentRepository.findByStoreAndPurchaseKeyForUpdate(PurchaseStore.GOOGLE, "token-1"))
                .thenReturn(Optional.of(existing));

        VerificationAttempt attempt = service.begin(USER_ID, PurchaseStore.GOOGLE, "TING_70", "token-1");
        assertThat(attempt.paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(attempt.status()).isEqualTo(PaymentStatus.VERIFYING);
        verify(paymentRepository, never()).save(any());

        assertThatThrownBy(() -> service.begin(999L, PurchaseStore.GOOGLE, "TING_70", "token-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("다른 사용자");
    }

    @Test
    void beginCreatesVerifyingPaymentWithStoreAndTingDetails() {
        when(storePaymentRepository.findByStoreAndPurchaseKeyForUpdate(PurchaseStore.APPLE, "tx-1"))
                .thenReturn(Optional.empty());
        when(productRepository.findByProductCode("TING_70")).thenReturn(Optional.of(product()));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(storePaymentRepository.save(any(StorePayment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tingPurchaseRepository.save(any(TingPurchase.class))).thenAnswer(inv -> inv.getArgument(0));

        VerificationAttempt attempt = service.begin(USER_ID, PurchaseStore.APPLE, "TING_70", "tx-1");

        assertThat(attempt.status()).isEqualTo(PaymentStatus.VERIFYING);
        assertThat(attempt.storeProductId()).isEqualTo(APPLE_PRODUCT_ID);
        assertThat(attempt.purchaseKey()).isEqualTo("tx-1");
        verify(tingPurchaseRepository).save(any(TingPurchase.class));
    }

    @Test
    void beginRequiresStoreSpecificIdentifier() {
        assertThatThrownBy(() -> service.begin(USER_ID, PurchaseStore.APPLE, "TING_70", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("transactionId");
        assertThatThrownBy(() -> service.begin(USER_ID, PurchaseStore.GOOGLE, "TING_70", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("purchaseToken");
    }

    @Test
    void creditAddsTingRecordsLedgerAndSchedulesGoogleConsume() {
        Payment payment = verifyingPayment(PurchaseStore.GOOGLE);
        StorePayment storePayment = storePayment(payment, PurchaseStore.GOOGLE, "token-1");
        TingPurchase tingPurchase = tingPurchase(payment);
        TingWallet wallet = new TingWallet(USER_ID);
        wallet.addTing(10);
        when(storePaymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(storePayment));
        when(tingPurchaseRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(tingPurchase));
        when(tingWalletRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(wallet));

        CreditOutcome outcome = service.credit(
                PAYMENT_ID, googlePurchased("GPA.1", PurchaseAccountToken.forUser(USER_ID))
        );

        assertThat(outcome.result()).isEqualTo(CreditOutcome.Result.CREDITED);
        assertThat(outcome.creditedTing()).isEqualTo(70);
        assertThat(outcome.creditedEventTing()).isEqualTo(5);
        assertThat(wallet.getTing()).isEqualTo(80);
        assertThat(wallet.getEventTing()).isEqualTo(5);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(storePayment.getTransactionId()).isEqualTo("GPA.1");
        assertThat(storePayment.getConsumeStatus()).isEqualTo(StoreConsumeStatus.PENDING);
        assertThat(tingPurchase.getCreditedTing()).isEqualTo(70);
        verify(recorder).recordPaid(eq(wallet), eq(TingTransactionType.TING_PURCHASE), eq(70), any(),
                eq("100"), eq("TING_PURCHASE_CREDIT_100"), anyString());
        verify(recorder).recordEvent(eq(wallet), eq(TingTransactionType.TING_PURCHASE), eq(5), any(),
                eq("100"), eq("TING_PURCHASE_CREDIT_EVENT_100"), anyString());
    }

    @Test
    void creditForAppleMultipliesByQuantityAndDoesNotRequireConsume() {
        Payment payment = verifyingPayment(PurchaseStore.APPLE);
        StorePayment storePayment = storePayment(payment, PurchaseStore.APPLE, "tx-1");
        TingPurchase tingPurchase = tingPurchase(payment);
        TingWallet wallet = new TingWallet(USER_ID);
        when(storePaymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(storePayment));
        when(tingPurchaseRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(tingPurchase));
        when(tingWalletRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(wallet));

        CreditOutcome outcome = service.credit(PAYMENT_ID, applePurchased("tx-1", 2, StoreEnvironment.PRODUCTION));

        assertThat(outcome.creditedTing()).isEqualTo(140);
        assertThat(wallet.getTing()).isEqualTo(140);
        assertThat(wallet.getEventTing()).isEqualTo(10);
        assertThat(storePayment.getConsumeStatus()).isEqualTo(StoreConsumeStatus.NOT_REQUIRED);
        assertThat(payment.getAmount()).isEqualByComparingTo("9900.000");
        assertThat(payment.getCurrency()).isEqualTo("KRW");
    }

    @Test
    void creditRejectsProductMismatchAndMarksPaymentFailed() {
        Payment payment = verifyingPayment(PurchaseStore.APPLE);
        StorePayment storePayment = storePayment(payment, PurchaseStore.APPLE, "tx-1");
        when(storePaymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(storePayment));

        StorePurchase other = new StorePurchase(
                PurchaseStore.APPLE, "com.mannabom.ting.30", "tx-1", "tx-1",
                StorePurchaseState.PURCHASED, StoreEnvironment.PRODUCTION, 1, null, null, null, null, false, "jws"
        );
        CreditOutcome outcome = service.credit(PAYMENT_ID, other);

        assertThat(outcome.result()).isEqualTo(CreditOutcome.Result.REJECTED);
        assertThat(outcome.reason()).contains("상품 ID");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(tingWalletRepository, never()).findByUserIdForUpdate(any());
    }

    @Test
    void creditRejectsSandboxWhenDisallowedAndAccountTokenMismatch() {
        properties.setAllowSandboxCredit(false);
        Payment payment = verifyingPayment(PurchaseStore.APPLE);
        StorePayment storePayment = storePayment(payment, PurchaseStore.APPLE, "tx-1");
        when(storePaymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(storePayment));

        CreditOutcome sandbox = service.credit(PAYMENT_ID, applePurchased("tx-1", 1, StoreEnvironment.SANDBOX));
        assertThat(sandbox.result()).isEqualTo(CreditOutcome.Result.REJECTED);
        assertThat(sandbox.reason()).contains("샌드박스");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);

        Payment payment2 = verifyingPayment(PurchaseStore.GOOGLE);
        StorePayment storePayment2 = storePayment(payment2, PurchaseStore.GOOGLE, "token-2");
        when(storePaymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(storePayment2));

        CreditOutcome mismatch = service.credit(PAYMENT_ID, googlePurchased("GPA.2", PurchaseAccountToken.forUser(999L)));
        assertThat(mismatch.result()).isEqualTo(CreditOutcome.Result.REJECTED);
        assertThat(mismatch.reason()).contains("구매자 토큰");
    }

    @Test
    void creditKeepsVerifyingWhenStorePurchaseIsPending() {
        Payment payment = verifyingPayment(PurchaseStore.GOOGLE);
        StorePayment storePayment = storePayment(payment, PurchaseStore.GOOGLE, "token-1");
        when(storePaymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(storePayment));

        StorePurchase pending = new StorePurchase(
                PurchaseStore.GOOGLE, GOOGLE_PRODUCT_ID, null, null,
                StorePurchaseState.PENDING, StoreEnvironment.PRODUCTION, 1, null, null, null, null, false, "{}"
        );
        CreditOutcome outcome = service.credit(PAYMENT_ID, pending);

        assertThat(outcome.result()).isEqualTo(CreditOutcome.Result.PENDING);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.VERIFYING);
        assertThat(payment.getFailureReason()).contains("완료되지 않았습니다");
    }

    @Test
    void creditIsSkippedWhenAlreadyPaid() {
        Payment payment = verifyingPayment(PurchaseStore.GOOGLE);
        payment.markPaid(null, null, StoreEnvironment.PRODUCTION, java.time.Instant.now());
        StorePayment storePayment = storePayment(payment, PurchaseStore.GOOGLE, "token-1");
        when(storePaymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(storePayment));

        CreditOutcome outcome = service.credit(PAYMENT_ID, googlePurchased("GPA.1", null));

        assertThat(outcome.result()).isEqualTo(CreditOutcome.Result.ALREADY_CREDITED);
        verify(tingWalletRepository, never()).findByUserIdForUpdate(any());
        verify(recorder, never()).recordPaid(any(), any(), anyInt(), any(), any(), any(), any());
    }

    @Test
    void startConsumeReturnsNullWhenNotRequired() {
        Payment payment = verifyingPayment(PurchaseStore.APPLE);
        payment.markPaid(null, null, StoreEnvironment.PRODUCTION, java.time.Instant.now());
        StorePayment storePayment = storePayment(payment, PurchaseStore.APPLE, "tx-1");
        when(storePaymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(storePayment));

        assertThat(service.startConsume(PAYMENT_ID)).isNull();
    }
}
