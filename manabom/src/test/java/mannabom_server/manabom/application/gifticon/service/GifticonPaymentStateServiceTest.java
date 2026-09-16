package mannabom_server.manabom.application.gifticon.service;

import mannabom_server.manabom.domain.gifticon.entity.GifticonPayment;
import mannabom_server.manabom.domain.gifticon.entity.GifticonProduct;
import mannabom_server.manabom.domain.gifticon.repository.GifticonPaymentRepository;
import mannabom_server.manabom.domain.gifticon.enums.GifticonMessageCreationStatus;
import mannabom_server.manabom.domain.gifticon.enums.GifticonPaymentStatus;
import mannabom_server.manabom.infrastructure.external.toss.config.TossPaymentsProperties;
import mannabom_server.manabom.domain.messageRequest.enums.MessageSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GifticonPaymentStateServiceTest {

    @Mock
    private GifticonPaymentRepository paymentRepository;

    @Test
    void rejectsClientAmountThatDiffersFromStoredOrderAmount() {
        GifticonPayment payment = payment(1L, 11_100);
        when(paymentRepository.findByOrderIdForUpdate("GIFTICON_123456"))
                .thenReturn(Optional.of(payment));
        GifticonPaymentStateService service = new GifticonPaymentStateService(
                paymentRepository,
                new TossPaymentsProperties()
        );

        assertThatThrownBy(() -> service.startConfirmation(
                1L,
                "GIFTICON_123456",
                "payment-key",
                100
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("서버 주문 금액");
    }

    @Test
    void rejectsAnotherUsersPaymentConfirmation() {
        GifticonPayment payment = payment(1L, 11_100);
        when(paymentRepository.findByOrderIdForUpdate("GIFTICON_123456"))
                .thenReturn(Optional.of(payment));
        GifticonPaymentStateService service = new GifticonPaymentStateService(
                paymentRepository,
                new TossPaymentsProperties()
        );

        assertThatThrownBy(() -> service.startConfirmation(
                2L,
                "GIFTICON_123456",
                "payment-key",
                11_100
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("본인의 기프티콘 결제");
    }

    @Test
    void temporaryMessageCreationFailureMovesToRetryPending() {
        GifticonPayment payment = paidPayment();
        when(paymentRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(payment));
        GifticonPaymentStateService service = new GifticonPaymentStateService(
                paymentRepository,
                new TossPaymentsProperties()
        );

        assertThat(service.startMessageCreation(1L)).isTrue();
        GifticonMessageCreationStatus result = service.failMessageCreation(
                1L,
                "temporary",
                true
        );

        assertThat(result).isEqualTo(GifticonMessageCreationStatus.RETRY_PENDING);
        assertThat(payment.getStatus()).isEqualTo(GifticonPaymentStatus.PAID);
        assertThat(payment.getMessageCreationAttemptCount()).isEqualTo(1);
    }

    @Test
    void permanentMessageCreationFailureRequestsRefund() {
        GifticonPayment payment = paidPayment();
        when(paymentRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(payment));
        GifticonPaymentStateService service = new GifticonPaymentStateService(
                paymentRepository,
                new TossPaymentsProperties()
        );

        assertThat(service.startMessageCreation(1L)).isTrue();
        GifticonMessageCreationStatus result = service.failMessageCreation(
                1L,
                "permanent",
                false
        );

        assertThat(result).isEqualTo(GifticonMessageCreationStatus.FAILED);
        assertThat(payment.getStatus()).isEqualTo(GifticonPaymentStatus.REFUND_PENDING);
    }

    @Test
    void exhaustedTemporaryMessageCreationRetriesRequestRefund() {
        GifticonPayment payment = paidPayment();
        when(paymentRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(payment));
        TossPaymentsProperties properties = new TossPaymentsProperties();
        properties.getMessageCreationRetry().setMaxAttempts(1);
        GifticonPaymentStateService service = new GifticonPaymentStateService(
                paymentRepository,
                properties
        );

        assertThat(service.startMessageCreation(1L)).isTrue();
        GifticonMessageCreationStatus result = service.failMessageCreation(
                1L,
                "temporary",
                true
        );

        assertThat(result).isEqualTo(GifticonMessageCreationStatus.FAILED);
        assertThat(payment.getStatus()).isEqualTo(GifticonPaymentStatus.REFUND_PENDING);
    }

    @Test
    void adminCanRetryRefundAfterAutomaticAttemptsAreExhausted() {
        GifticonPayment payment = paidPayment();
        payment.requestRefund();
        payment.startRefund(java.time.Instant.now().minusSeconds(120));
        payment.markRefundFailed("첫 번째 환불 실패");
        when(paymentRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(payment));
        TossPaymentsProperties properties = new TossPaymentsProperties();
        properties.getRefundRetry().setMaxAttempts(1);
        GifticonPaymentStateService service = new GifticonPaymentStateService(
                paymentRepository,
                properties
        );

        assertThat(service.startRefund(1L)).isNull();
        GifticonPaymentStateService.RefundAttempt attempt =
                service.startRefundForAdmin(1L);

        assertThat(attempt.paymentId()).isEqualTo(1L);
        assertThat(payment.getRefundAttemptCount()).isEqualTo(2);
        assertThat(payment.getStatus()).isEqualTo(GifticonPaymentStatus.REFUND_PROCESSING);
    }

    @Test
    void adminCanRetryMessageAfterAutomaticAttemptsAreExhausted() {
        GifticonPayment payment = paidPayment();
        ReflectionTestUtils.setField(
                payment,
                "messageCreationStatus",
                GifticonMessageCreationStatus.FAILED
        );
        ReflectionTestUtils.setField(payment, "messageCreationAttemptCount", 10);
        when(paymentRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(payment));
        TossPaymentsProperties properties = new TossPaymentsProperties();
        properties.getMessageCreationRetry().setMaxAttempts(10);
        GifticonPaymentStateService service = new GifticonPaymentStateService(
                paymentRepository,
                properties
        );

        service.startMessageCreationForAdmin(1L);

        assertThat(payment.getMessageCreationAttemptCount()).isEqualTo(11);
        assertThat(payment.getMessageCreationStatus())
                .isEqualTo(GifticonMessageCreationStatus.PROCESSING);
    }

    private GifticonPayment paidPayment() {
        GifticonPayment payment = payment(1L, 11_100);
        ReflectionTestUtils.setField(payment, "gifticonPaymentId", 1L);
        java.time.Instant now = java.time.Instant.now();
        payment.startConfirmation("payment-key", now, now.minusSeconds(60));
        payment.markPaid(now);
        return payment;
    }

    private GifticonPayment payment(Long userId, int salePrice) {
        GifticonProduct product = new GifticonProduct(100L);
        ReflectionTestUtils.setField(product, "salePrice", salePrice);
        return new GifticonPayment(
                userId,
                product,
                "GIFTICON_123456",
                "CUSTOMER_123456",
                100L,
                "안녕하세요",
                MessageSource.PROFILE_MATCH
        );
    }
}
