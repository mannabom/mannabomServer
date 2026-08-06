package mannabom_server.manabom.application.gifticon.service;

import mannabom_server.manabom.application.gifticon.dto.request.ConfirmGifticonPaymentRequest;
import mannabom_server.manabom.application.gifticon.dto.request.PrepareGifticonPaymentRequest;
import mannabom_server.manabom.application.gifticon.dto.response.GifticonPaymentPrepareResponse;
import mannabom_server.manabom.application.gifticon.dto.response.GifticonPaymentResponse;
import mannabom_server.manabom.application.gifticon.event.GifticonPaymentRefundRequestedEvent;
import mannabom_server.manabom.application.gifticon.port.GifticonPaymentGateway;
import mannabom_server.manabom.domain.gifticon.entity.GifticonPayment;
import mannabom_server.manabom.domain.gifticon.entity.GifticonProduct;
import mannabom_server.manabom.domain.gifticon.enums.GifticonPaymentStatus;
import mannabom_server.manabom.domain.gifticon.enums.GifticonMessageCreationStatus;
import mannabom_server.manabom.domain.gifticon.repository.GifticonPaymentRepository;
import mannabom_server.manabom.domain.gifticon.repository.GifticonProductRepository;
import mannabom_server.manabom.domain.messageRequest.entity.MessageRequest;
import mannabom_server.manabom.domain.messageRequest.enums.MessageSource;
import mannabom_server.manabom.infrastructure.external.toss.config.TossPaymentsProperties;
import mannabom_server.manabom.application.messageRequest.service.MessageRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.TransientDataAccessResourceException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GifticonPaymentServiceTest {

    @Mock
    private GifticonPaymentRepository paymentRepository;
    @Mock
    private GifticonProductRepository productRepository;
    @Mock
    private GifticonPaymentStateService stateService;
    @Mock
    private GifticonPaymentGateway paymentGateway;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private MessageRequestService messageRequestService;

    private TossPaymentsProperties properties;
    private GifticonPaymentService service;

    @BeforeEach
    void setUp() {
        properties = new TossPaymentsProperties();
        properties.setClientKey("test_ck_test");
        service = new GifticonPaymentService(
                paymentRepository,
                productRepository,
                stateService,
                paymentGateway,
                properties,
                eventPublisher,
                messageRequestService
        );
    }

    @Test
    void preparesTossPaymentWithServerCalculatedWonAmount() {
        GifticonProduct product = orderableProduct(11_100);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(paymentRepository.save(any(GifticonPayment.class))).thenAnswer(invocation -> {
            GifticonPayment payment = invocation.getArgument(0);
            ReflectionTestUtils.setField(payment, "gifticonPaymentId", 20L);
            return payment;
        });

        PrepareGifticonPaymentRequest request = new PrepareGifticonPaymentRequest(
                10L,
                100L,
                "안녕하세요",
                MessageSource.PROFILE_MATCH
        );
        GifticonPaymentPrepareResponse response = service.prepare(1L, request);

        assertThat(response.gifticonPaymentId()).isEqualTo(20L);
        assertThat(response.amount()).isEqualTo(11_100);
        assertThat(response.orderId()).startsWith("GIFTICON_");
        assertThat(response.customerKey()).startsWith("CUSTOMER_");
        assertThat(response.clientKey()).isEqualTo("test_ck_test");
        verify(messageRequestService).validateMessageIntent(
                1L,
                100L,
                "안녕하세요",
                MessageSource.PROFILE_MATCH
        );
    }

    @Test
    void confirmsOnlyWhenTossResultMatchesServerOrder() {
        GifticonPaymentStateService.ConfirmationAttempt attempt =
                new GifticonPaymentStateService.ConfirmationAttempt(
                        20L,
                        "payment-key",
                        "GIFTICON_123456",
                        11_100,
                        false
                );
        ConfirmGifticonPaymentRequest request = new ConfirmGifticonPaymentRequest(
                "payment-key",
                "GIFTICON_123456",
                11_100
        );
        GifticonPayment payment = paidPayment(20L, 11_100);
        when(stateService.startConfirmation(
                1L,
                request.orderId(),
                request.paymentKey(),
                request.amount()
        )).thenReturn(attempt);
        when(paymentGateway.confirm(any())).thenReturn(
                new GifticonPaymentGateway.PaymentResult(
                        "payment-key",
                        "GIFTICON_123456",
                        11_100,
                        "DONE"
                )
        );
        when(stateService.completeConfirmation(20L)).thenReturn(payment);
        when(stateService.startMessageCreation(20L)).thenReturn(true);
        when(messageRequestService.sendPaidGifticonMessage(20L)).thenAnswer(invocation -> {
            MessageRequest messageRequest = new MessageRequest(
                    1L,
                    2L,
                    "안녕하세요",
                    MessageSource.PROFILE_MATCH,
                    payment.getProduct()
            );
            ReflectionTestUtils.setField(messageRequest, "id", 30L);
            payment.attachTo(messageRequest);
            return 30L;
        });
        when(paymentRepository.findByIdWithMessageRequest(20L))
                .thenReturn(Optional.of(payment));

        GifticonPaymentResponse response = service.confirm(1L, request);

        assertThat(response.status()).isEqualTo(GifticonPaymentStatus.PAID);
        assertThat(response.messageCreationStatus())
                .isEqualTo(GifticonMessageCreationStatus.CREATED);
        assertThat(response.messageRequestId()).isEqualTo(30L);
        ArgumentCaptor<GifticonPaymentGateway.ConfirmPaymentCommand> captor =
                ArgumentCaptor.forClass(GifticonPaymentGateway.ConfirmPaymentCommand.class);
        verify(paymentGateway).confirm(captor.capture());
        assertThat(captor.getValue().amount()).isEqualTo(11_100);
        verify(messageRequestService).sendPaidGifticonMessage(20L);
    }

    @Test
    void refundsRejectedRequestThroughTossCancellation() {
        when(stateService.startRefund(20L)).thenReturn(
                new GifticonPaymentStateService.RefundAttempt(
                        20L,
                        "payment-key",
                        "GIFTICON_123456",
                        false
                )
        );
        when(paymentGateway.cancel(any())).thenReturn(
                new GifticonPaymentGateway.PaymentResult(
                        "payment-key",
                        "GIFTICON_123456",
                        11_100,
                        "CANCELED"
                )
        );

        service.refund(20L);

        ArgumentCaptor<GifticonPaymentGateway.CancelPaymentCommand> captor =
                ArgumentCaptor.forClass(GifticonPaymentGateway.CancelPaymentCommand.class);
        verify(paymentGateway).cancel(captor.capture());
        assertThat(captor.getValue().cancelReason()).isEqualTo("메시지 요청 거절");
        verify(stateService).completeRefund(20L);
    }

    @Test
    void requestsRefundWhenUserCancelsUnusedPaidPayment() {
        GifticonPayment payment = paidPayment(20L, 11_100);
        when(paymentRepository.findByIdForUpdate(20L))
                .thenReturn(Optional.of(payment));

        GifticonPaymentResponse response = service.cancelUnusedPayment(1L, 20L);

        assertThat(response.status()).isEqualTo(GifticonPaymentStatus.REFUND_PENDING);
        ArgumentCaptor<GifticonPaymentRefundRequestedEvent> captor =
                ArgumentCaptor.forClass(GifticonPaymentRefundRequestedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().gifticonPaymentId()).isEqualTo(20L);
    }

    @Test
    void requestsRefundForExpiredUnusedPaidPayment() {
        GifticonPayment payment = paidPayment(20L, 11_100);
        when(paymentRepository.findByIdForUpdate(20L))
                .thenReturn(Optional.of(payment));

        service.requestExpiredUnusedPaymentRefund(
                20L,
                payment.getApprovedAt().plusSeconds(1)
        );

        assertThat(payment.getStatus()).isEqualTo(GifticonPaymentStatus.REFUND_PENDING);
        verify(eventPublisher).publishEvent(
                new GifticonPaymentRefundRequestedEvent(20L)
        );
    }

    @Test
    void doesNotRefundExpiredPaymentAlreadyAttachedToMessageRequest() {
        GifticonPayment payment = paidPayment(20L, 11_100);
        MessageRequest messageRequest = new MessageRequest(
                1L,
                2L,
                "안녕하세요",
                MessageSource.PROFILE_MATCH,
                payment.getProduct()
        );
        payment.attachTo(messageRequest);
        when(paymentRepository.findByIdForUpdate(20L))
                .thenReturn(Optional.of(payment));

        service.requestExpiredUnusedPaymentRefund(
                20L,
                payment.getApprovedAt().plusSeconds(1)
        );

        assertThat(payment.getStatus()).isEqualTo(GifticonPaymentStatus.PAID);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void refundsUnusedPaymentWithUnusedPaymentReason() {
        when(stateService.startRefund(20L)).thenReturn(
                new GifticonPaymentStateService.RefundAttempt(
                        20L,
                        "payment-key",
                        "GIFTICON_123456",
                        true
                )
        );
        when(paymentGateway.cancel(any())).thenReturn(
                new GifticonPaymentGateway.PaymentResult(
                        "payment-key",
                        "GIFTICON_123456",
                        11_100,
                        "CANCELED"
                )
        );

        service.refund(20L);

        ArgumentCaptor<GifticonPaymentGateway.CancelPaymentCommand> captor =
                ArgumentCaptor.forClass(GifticonPaymentGateway.CancelPaymentCommand.class);
        verify(paymentGateway).cancel(captor.capture());
        assertThat(captor.getValue().cancelReason())
                .isEqualTo("미사용 기프티콘 결제 취소");
    }

    @Test
    void retriesMessageCreationWhenTemporaryDatabaseFailureOccurs() {
        when(stateService.startMessageCreation(20L)).thenReturn(true);
        when(messageRequestService.sendPaidGifticonMessage(20L))
                .thenThrow(new TransientDataAccessResourceException("temporary"));
        when(stateService.failMessageCreation(eq(20L), anyString(), eq(true)))
                .thenReturn(GifticonMessageCreationStatus.RETRY_PENDING);

        service.createMessageForPaidPayment(20L);

        verify(stateService).failMessageCreation(eq(20L), anyString(), eq(true));
        verify(stateService, never()).startRefund(20L);
    }

    @Test
    void refundsPaymentWhenMessageCreationFailureIsPermanent() {
        when(stateService.startMessageCreation(20L)).thenReturn(true);
        when(messageRequestService.sendPaidGifticonMessage(20L))
                .thenThrow(new IllegalStateException("이미 요청을 보냈습니다."));
        when(stateService.failMessageCreation(eq(20L), anyString(), eq(false)))
                .thenReturn(GifticonMessageCreationStatus.FAILED);

        service.createMessageForPaidPayment(20L);

        verify(stateService).failMessageCreation(eq(20L), anyString(), eq(false));
        verify(stateService).startRefund(20L);
    }

    private GifticonPayment paidPayment(Long paymentId, int salePrice) {
        GifticonPayment payment = new GifticonPayment(
                1L,
                orderableProduct(salePrice),
                "GIFTICON_123456",
                "CUSTOMER_123456",
                100L,
                "안녕하세요",
                MessageSource.PROFILE_MATCH
        );
        ReflectionTestUtils.setField(payment, "gifticonPaymentId", paymentId);
        payment.startConfirmation(
                "payment-key",
                Instant.now(),
                Instant.now().minusSeconds(60)
        );
        payment.markPaid(Instant.now());
        return payment;
    }

    private GifticonProduct orderableProduct(int salePrice) {
        GifticonProduct product = new GifticonProduct(100L);
        ReflectionTestUtils.setField(product, "salePrice", salePrice);
        ReflectionTestUtils.setField(product, "available", true);
        ReflectionTestUtils.setField(product, "productName", "스타벅스 교환권");
        ReflectionTestUtils.setField(product, "startAt", LocalDateTime.now().minusDays(1));
        ReflectionTestUtils.setField(product, "endAt", LocalDateTime.now().plusDays(1));
        product.configureEncryptedTemplateToken("v1:encrypted");
        return product;
    }
}
