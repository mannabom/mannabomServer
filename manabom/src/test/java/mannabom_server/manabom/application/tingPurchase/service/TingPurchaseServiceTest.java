package mannabom_server.manabom.application.tingPurchase.service;

import mannabom_server.manabom.application.tingPurchase.dto.request.VerifyTingPurchaseRequest;
import mannabom_server.manabom.application.tingPurchase.dto.response.TingPurchaseResponse;
import mannabom_server.manabom.application.tingPurchase.port.InAppPurchaseGateway;
import mannabom_server.manabom.application.tingPurchase.port.InAppPurchaseGateway.ConsumePurchaseCommand;
import mannabom_server.manabom.application.tingPurchase.port.InAppPurchaseGateway.VerifyPurchaseCommand;
import mannabom_server.manabom.application.tingPurchase.port.StoreGatewayException;
import mannabom_server.manabom.application.tingPurchase.port.StorePurchaseInvalidException;
import mannabom_server.manabom.application.tingPurchase.service.TingPurchaseStateService.ConsumeAttempt;
import mannabom_server.manabom.application.tingPurchase.service.TingPurchaseStateService.CreditOutcome;
import mannabom_server.manabom.application.tingPurchase.service.TingPurchaseStateService.VerificationAttempt;
import mannabom_server.manabom.domain.currency.entity.TingWallet;
import mannabom_server.manabom.domain.currency.repository.TingWalletRepository;
import mannabom_server.manabom.domain.payment.entity.Payment;
import mannabom_server.manabom.domain.payment.entity.StorePayment;
import mannabom_server.manabom.domain.payment.enums.PaymentStatus;
import mannabom_server.manabom.domain.payment.enums.PurchaseStore;
import mannabom_server.manabom.domain.payment.enums.StoreEnvironment;
import mannabom_server.manabom.domain.payment.repository.StorePaymentRepository;
import mannabom_server.manabom.domain.tingPurchase.entity.TingPurchase;
import mannabom_server.manabom.domain.tingPurchase.repository.TingPurchaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static mannabom_server.manabom.application.tingPurchase.service.TingPurchaseFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TingPurchaseServiceTest {

    @Mock private TingPurchaseStateService stateService;
    @Mock private InAppPurchaseGateway appleGateway;
    @Mock private InAppPurchaseGateway googleGateway;
    @Mock private TingPurchaseRepository tingPurchaseRepository;
    @Mock private StorePaymentRepository storePaymentRepository;
    @Mock private TingWalletRepository tingWalletRepository;

    private TingPurchaseService service;

    @BeforeEach
    void setUp() {
        when(appleGateway.store()).thenReturn(PurchaseStore.APPLE);
        when(googleGateway.store()).thenReturn(PurchaseStore.GOOGLE);
        service = new TingPurchaseService(
                stateService, List.of(appleGateway, googleGateway),
                tingPurchaseRepository, storePaymentRepository, tingWalletRepository
        );
    }

    private VerificationAttempt verifying(PurchaseStore store, String key) {
        return new VerificationAttempt(PAYMENT_ID, PaymentStatus.VERIFYING, store,
                store == PurchaseStore.APPLE ? APPLE_PRODUCT_ID : GOOGLE_PRODUCT_ID, key, null);
    }

    private void stubPaidLookup(PurchaseStore store, String key) {
        Payment payment = verifyingPayment(store);
        payment.markPaid(null, null, StoreEnvironment.PRODUCTION, Instant.now());
        StorePayment storePayment = storePayment(payment, store, key);
        TingPurchase tingPurchase = tingPurchase(payment);
        tingPurchase.markCredited(1, Instant.now());
        TingWallet wallet = new TingWallet(USER_ID);
        wallet.addTing(70);
        when(tingPurchaseRepository.findByIdWithDetails(PAYMENT_ID)).thenReturn(Optional.of(tingPurchase));
        when(storePaymentRepository.findByIdWithPayment(PAYMENT_ID)).thenReturn(Optional.of(storePayment));
        when(tingWalletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
    }

    @Test
    void verifiesCreditsAndConsumesGooglePurchase() {
        VerifyTingPurchaseRequest request = new VerifyTingPurchaseRequest(PurchaseStore.GOOGLE, "TING_70", null, "token-1");
        when(stateService.begin(USER_ID, PurchaseStore.GOOGLE, "TING_70", "token-1"))
                .thenReturn(verifying(PurchaseStore.GOOGLE, "token-1"));
        when(googleGateway.verify(new VerifyPurchaseCommand(GOOGLE_PRODUCT_ID, "token-1")))
                .thenReturn(googlePurchased("GPA.1", null));
        when(stateService.credit(eq(PAYMENT_ID), any()))
                .thenReturn(new CreditOutcome(CreditOutcome.Result.CREDITED, null, 70, 5));
        when(stateService.startConsume(PAYMENT_ID))
                .thenReturn(new ConsumeAttempt(PAYMENT_ID, PurchaseStore.GOOGLE, GOOGLE_PRODUCT_ID, "token-1"));
        stubPaidLookup(PurchaseStore.GOOGLE, "token-1");

        TingPurchaseResponse response = service.verify(USER_ID, request);

        assertThat(response.status()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.creditedTing()).isEqualTo(70);
        assertThat(response.tingBalance()).isEqualTo(70);
        verify(googleGateway).consume(new ConsumePurchaseCommand(GOOGLE_PRODUCT_ID, "token-1"));
        verify(stateService).completeConsume(PAYMENT_ID);
    }

    @Test
    void invalidStorePurchaseMarksFailedAndReturnsBadRequestStyleError() {
        VerifyTingPurchaseRequest request = new VerifyTingPurchaseRequest(PurchaseStore.APPLE, "TING_70", "tx-1", null);
        when(stateService.begin(USER_ID, PurchaseStore.APPLE, "TING_70", "tx-1"))
                .thenReturn(verifying(PurchaseStore.APPLE, "tx-1"));
        when(appleGateway.verify(any())).thenThrow(new StorePurchaseInvalidException("존재하지 않는 거래"));

        assertThatThrownBy(() -> service.verify(USER_ID, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("존재하지 않는 거래");
        verify(stateService).fail(eq(PAYMENT_ID), anyString());
        verify(stateService, never()).credit(any(), any());
    }

    @Test
    void transientGatewayFailureKeepsPurchaseVerifyingAndRethrows() {
        VerifyTingPurchaseRequest request = new VerifyTingPurchaseRequest(PurchaseStore.APPLE, "TING_70", "tx-1", null);
        when(stateService.begin(USER_ID, PurchaseStore.APPLE, "TING_70", "tx-1"))
                .thenReturn(verifying(PurchaseStore.APPLE, "tx-1"));
        when(appleGateway.verify(any())).thenThrow(new StoreGatewayException("timeout"));

        assertThatThrownBy(() -> service.verify(USER_ID, request))
                .isInstanceOf(StoreGatewayException.class);
        verify(stateService).recordTransientFailure(eq(PAYMENT_ID), anyString());
        verify(stateService, never()).fail(any(), any());
    }

    @Test
    void rejectedCreditSurfacesReason() {
        VerifyTingPurchaseRequest request = new VerifyTingPurchaseRequest(PurchaseStore.APPLE, "TING_70", "tx-1", null);
        when(stateService.begin(USER_ID, PurchaseStore.APPLE, "TING_70", "tx-1"))
                .thenReturn(verifying(PurchaseStore.APPLE, "tx-1"));
        when(appleGateway.verify(any())).thenReturn(applePurchased("tx-1", 1, StoreEnvironment.PRODUCTION));
        when(stateService.credit(eq(PAYMENT_ID), any()))
                .thenReturn(new CreditOutcome(CreditOutcome.Result.REJECTED, "상품 불일치", 0, 0));

        assertThatThrownBy(() -> service.verify(USER_ID, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("상품 불일치");
        verify(stateService, never()).startConsume(any());
    }

    @Test
    void alreadyPaidPurchaseReturnsWithoutCallingStore() {
        VerifyTingPurchaseRequest request = new VerifyTingPurchaseRequest(PurchaseStore.GOOGLE, "TING_70", null, "token-1");
        when(stateService.begin(USER_ID, PurchaseStore.GOOGLE, "TING_70", "token-1"))
                .thenReturn(new VerificationAttempt(PAYMENT_ID, PaymentStatus.PAID, PurchaseStore.GOOGLE, GOOGLE_PRODUCT_ID, "token-1", null));
        stubPaidLookup(PurchaseStore.GOOGLE, "token-1");

        TingPurchaseResponse response = service.verify(USER_ID, request);

        assertThat(response.status()).isEqualTo(PaymentStatus.PAID);
        verify(googleGateway, never()).verify(any());
    }

    @Test
    void previouslyFailedPurchaseIsRejectedImmediately() {
        VerifyTingPurchaseRequest request = new VerifyTingPurchaseRequest(PurchaseStore.GOOGLE, "TING_70", null, "token-1");
        when(stateService.begin(USER_ID, PurchaseStore.GOOGLE, "TING_70", "token-1"))
                .thenReturn(new VerificationAttempt(PAYMENT_ID, PaymentStatus.FAILED, PurchaseStore.GOOGLE, GOOGLE_PRODUCT_ID, "token-1", "상품 불일치"));

        assertThatThrownBy(() -> service.verify(USER_ID, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("상품 불일치");
    }

    @Test
    void consumeFailureIsRecordedAndDoesNotPropagate() {
        when(stateService.startConsume(PAYMENT_ID))
                .thenReturn(new ConsumeAttempt(PAYMENT_ID, PurchaseStore.GOOGLE, GOOGLE_PRODUCT_ID, "token-1"));
        doThrow(new StoreGatewayException("503")).when(googleGateway).consume(any());

        service.consume(PAYMENT_ID);

        verify(stateService).failConsume(PAYMENT_ID);
        verify(stateService, never()).completeConsume(any());
    }

    @Test
    void getPurchaseRejectsOtherUsers() {
        Payment payment = verifyingPayment(PurchaseStore.APPLE);
        TingPurchase tingPurchase = tingPurchase(payment);
        ReflectionTestUtils.setField(tingPurchase, "paymentId", PAYMENT_ID);
        when(tingPurchaseRepository.findByIdWithDetails(PAYMENT_ID)).thenReturn(Optional.of(tingPurchase));

        assertThatThrownBy(() -> service.getPurchase(999L, PAYMENT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("본인의 팅 구매");
    }
}
